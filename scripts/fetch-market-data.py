import json
import math
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from copy import deepcopy
from datetime import datetime, timedelta, timezone
from pathlib import Path

try:
    from zoneinfo import ZoneInfo
except ImportError:  # pragma: no cover
    ZoneInfo = None


ROOT = Path(__file__).resolve().parents[1]
SEED_PATH = ROOT / "data" / "market-seed.json"
SITE_DIR = ROOT / "market-site"
ROOT_DAILY_DIR = ROOT / "daily"
SITE_DAILY_DIR = SITE_DIR / "daily"
ROOT_REALTIME_DIR = ROOT / "realtime"
SITE_REALTIME_DIR = SITE_DIR / "realtime"

SINA_CN_DAILY_URL = "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData"
SINA_US_DAILY_URL = "https://stock.finance.sina.com.cn/usstock/api/jsonp.php/var%20_{symbol}=/US_MinKService.getDailyK"
EASTMONEY_BOARD_URL = "https://push2.eastmoney.com/api/qt/clist/get"

CN_INDEX_CODES = {
    "上证指数": "sh000001",
    "深证成指": "sz399001",
    "创业板指": "sz399006",
    "沪深300": "sh000300",
}

CN_INDEX_AK_CODES = {
    "上证指数": "000001",
    "深证成指": "399001",
    "创业板指": "399006",
    "沪深300": "000300",
}


def china_now():
    if ZoneInfo:
        return datetime.now(ZoneInfo("Asia/Shanghai"))
    return datetime.now(timezone.utc) + timedelta(hours=8)


def pct_change(values, sessions):
    if len(values) <= sessions:
        return None
    base = values[-sessions - 1]
    latest = values[-1]
    if not base:
        return None
    return round((latest / base - 1) * 100, 2)


def is_blank(value):
    if value is None:
        return True
    if isinstance(value, float) and math.isnan(value):
        return True
    return str(value).strip() in {"", "-", "nan", "None"}


def as_text(value):
    return "" if is_blank(value) else str(value).strip()


def as_number(value, default=None):
    if is_blank(value):
        return default
    try:
        return round(float(str(value).replace(",", "").replace("%", "")), 2)
    except (TypeError, ValueError):
        return default


def compact_warning(status, key, message):
    warnings = status.setdefault(key, [])
    if message not in warnings:
        warnings.append(message)


def request_text(url, referer, retries=3, extra_headers=None):
    headers = {
        "User-Agent": "Mozilla/5.0 market-lens/1.0",
        "Referer": referer,
    }
    if extra_headers:
        headers.update(extra_headers)
    last_error = None
    for attempt in range(retries):
        try:
            request = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(request, timeout=25) as response:
                return response.read().decode("utf-8", errors="ignore")
        except Exception as exc:
            last_error = exc
            if attempt < retries - 1:
                time.sleep(1.2 * (attempt + 1))
    raise last_error


def request_json(url, referer, retries=3, extra_headers=None):
    return json.loads(request_text(url, referer, retries, extra_headers))


def df_records(df):
    return df.fillna("").to_dict("records")


def load_akshare(status):
    try:
        import akshare as ak  # type: ignore
        return ak
    except Exception as exc:
        compact_warning(status, "akshareWarnings", f"AKShare unavailable: {exc}")
        return None


def call_akshare(fetcher, label, status, attempts=3):
    last_error = None
    for attempt in range(attempts):
        try:
            return fetcher()
        except Exception as exc:
            last_error = exc
            if attempt < attempts - 1:
                time.sleep(1.5 * (attempt + 1))
    compact_warning(status, "akshareWarnings", f"{label} unavailable: {last_error}")
    return None


def cn_symbol(code):
    code = str(code).strip()
    if code.startswith(("6", "9")):
        return f"sh{code}"
    return f"sz{code}"


def parse_cn_rows(rows):
    parsed = []
    for row in rows:
        close = row.get("close")
        if close in (None, "", "0"):
            continue
        parsed.append({
            "date": row.get("day"),
            "close": float(close),
        })
    return parsed


def fetch_sina_cn_daily(symbol, sessions=140):
    url = f"{SINA_CN_DAILY_URL}?{urllib.parse.urlencode({
        'symbol': symbol,
        'scale': '240',
        'ma': 'no',
        'datalen': str(sessions),
    })}"
    text = request_text(url, "https://finance.sina.com.cn/")
    return parse_cn_rows(json.loads(text))


def parse_sina_us_jsonp(text):
    match = re.search(r"=\((\[.*\])\);?\s*$", text, re.S)
    if not match:
        return []
    rows = json.loads(match.group(1))
    parsed = []
    for row in rows:
        close = row.get("c")
        if close in (None, "", "0"):
            continue
        parsed.append({
            "date": row.get("d"),
            "close": float(close),
        })
    return parsed


def fetch_sina_us_daily(symbol):
    symbol = str(symbol).upper()
    url = SINA_US_DAILY_URL.format(symbol=symbol) + "?" + urllib.parse.urlencode({"symbol": symbol})
    text = request_text(url, "https://finance.sina.com.cn/")
    return parse_sina_us_jsonp(text)


def update_quote_from_closes(target, closes, latest_date, provider_name):
    if len(closes) >= 2 and closes[-2]:
        target["pct"] = round((closes[-1] / closes[-2] - 1) * 100, 2)
    target["prices"] = [round(value, 2) for value in closes[-6:]]
    target["d5"] = pct_change(closes, 5)
    target["d20"] = pct_change(closes, 20)
    target["d60"] = pct_change(closes, 60)
    target["reason"] = f"已接入{provider_name}延迟日线数据，最新交易日 {latest_date}。"


def find_first_list(value):
    if isinstance(value, list):
        return value
    if isinstance(value, dict):
        for key in ("data", "list", "List", "items", "Items", "diff", "plates", "sectors"):
            found = find_first_list(value.get(key))
            if found:
                return found
        for child in value.values():
            found = find_first_list(child)
            if found:
                return found
    return []


def number_from_keys(row, keys, default=0):
    for key in keys:
        value = row.get(key)
        parsed = as_number(value)
        if parsed is not None:
            return parsed
    return default


def text_from_keys(row, keys):
    for key in keys:
        value = as_text(row.get(key))
        if value:
            return value
    return ""


def normalize_sector_stock(name, code, pct=None, type_name="领涨股"):
    if not name:
        return None
    stock = {
        "name": str(name),
        "code": str(code or ""),
        "type": type_name,
    }
    parsed_pct = as_number(pct)
    if parsed_pct is not None:
        stock["pct"] = parsed_pct
    return stock


def fetch_kpl_sectors(status):
    url = os.environ.get("KPL_SECTOR_STRENGTH_URL", "").strip()
    if not url:
        status["cnSectors"] = "KPL sector source not configured; using AKShare"
        return []

    headers = {}
    cookie = os.environ.get("KPL_COOKIE", "").strip()
    if cookie:
        headers["Cookie"] = cookie

    try:
        payload = request_json(url, "https://www.kaipanla.com/", extra_headers=headers)
        rows = find_first_list(payload)
    except Exception as exc:
        compact_warning(status, "cnSectorWarnings", f"KPL sector source unavailable: {exc}")
        return []

    sectors = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        name = text_from_keys(row, ("name", "Name", "plate_name", "PlateName", "sector_name", "SectorName", "title"))
        if not name:
            continue
        leader = normalize_sector_stock(
            text_from_keys(row, ("leader", "Leader", "stock_name", "StockName", "lead_stock_name")),
            text_from_keys(row, ("leader_code", "LeaderCode", "stock_code", "StockCode", "lead_stock_code")),
            number_from_keys(row, ("leader_pct", "LeaderPct", "stock_pct", "StockPct")),
        )
        sector = {
            "name": name,
            "pct": number_from_keys(row, ("pct", "Pct", "change", "Change", "change_pct", "ChangePct", "zf", "ZhangFu", "strength", "Strength")),
            "status": "开盘啦板块强度",
            "source": "开盘啦",
        }
        if leader:
            sector["stocks"] = [leader]
        sectors.append(sector)

    sectors.sort(key=lambda item: item.get("pct", 0), reverse=True)
    if sectors:
        status["cnSectors"] = f"KPL sector strength updated {len(sectors[:8])} sectors"
    return sectors[:8]


def fetch_akshare_sector_stocks(ak, sector_name, board_type, status):
    if board_type == "industry":
        fetcher = lambda: ak.stock_board_industry_cons_em(symbol=sector_name)
    else:
        fetcher = lambda: ak.stock_board_concept_cons_em(symbol=sector_name)
    df = call_akshare(fetcher, f"{sector_name} constituents", status, attempts=2)
    if df is None:
        return []

    stocks = []
    for row in df_records(df):
        name = text_from_keys(row, ("名称", "股票名称", "证券名称"))
        code = text_from_keys(row, ("代码", "股票代码", "证券代码"))
        pct = number_from_keys(row, ("涨跌幅", "涨幅", "change_pct"), None)
        if name:
            stocks.append({
                "name": name,
                "code": code,
                "pct": pct,
                "type": "板块成分股",
            })
    stocks.sort(key=lambda item: item.get("pct") if item.get("pct") is not None else -999, reverse=True)
    return stocks[:4]


def akshare_sector_from_row(row, board_type):
    name = text_from_keys(row, ("板块名称", "名称", "行业名称", "概念名称"))
    if not name:
        return None
    if name.startswith("昨日"):
        return None

    pct = number_from_keys(row, ("涨跌幅", "涨幅", "change_pct"), None)
    if pct is None:
        return None

    leader = normalize_sector_stock(
        text_from_keys(row, ("领涨股票", "领涨股", "领涨名称")),
        text_from_keys(row, ("领涨股票代码", "领涨代码", "代码")),
        number_from_keys(row, ("领涨股票-涨跌幅", "领涨涨跌幅", "领涨股涨跌幅"), None),
    )
    source_label = "AKShare 行业板块" if board_type == "industry" else "AKShare 概念板块"
    sector = {
        "name": name,
        "pct": pct,
        "status": source_label,
        "source": source_label,
        "boardType": board_type,
        "code": text_from_keys(row, ("板块代码", "代码")),
    }
    if leader:
        sector["stocks"] = [leader]
    return sector


def fetch_akshare_sectors(ak, status):
    sectors = []
    for board_type, fetcher_name in (
        ("industry", "stock_board_industry_name_em"),
        ("concept", "stock_board_concept_name_em"),
    ):
        fetcher = getattr(ak, fetcher_name)
        df = call_akshare(fetcher, f"AKShare {board_type} sectors", status)
        if df is None:
            continue
        rows = df_records(df)
        for row in rows:
            sector = akshare_sector_from_row(row, board_type)
            if sector:
                sectors.append(sector)

    seen = set()
    deduped = []
    for sector in sorted(sectors, key=lambda item: item.get("pct", 0), reverse=True):
        if sector["name"] in seen:
            continue
        seen.add(sector["name"])
        deduped.append(sector)

    top_sectors = deduped[:8]
    for sector in top_sectors:
        board_type = sector.pop("boardType", "concept")
        stocks = fetch_akshare_sector_stocks(ak, sector["name"], board_type, status)
        if stocks:
            sector["stocks"] = stocks

    if top_sectors:
        status["cnSectors"] = f"AKShare board strength updated {len(top_sectors)} sectors"
    return top_sectors


def fetch_eastmoney_board_rows(fs, limit=20):
    url = f"{EASTMONEY_BOARD_URL}?{urllib.parse.urlencode({
        'pn': '1',
        'pz': str(limit),
        'po': '1',
        'np': '1',
        'fltt': '2',
        'invt': '2',
        'fid': 'f3',
        'fs': fs,
        'fields': 'f3,f12,f14,f62,f128,f136,f140,f207,f208',
    })}"
    payload = request_json(url, "https://quote.eastmoney.com/")
    return ((payload.get("data") or {}).get("diff") or [])


def fetch_public_sector_strength(status):
    sectors = []
    try:
        rows = fetch_eastmoney_board_rows("m:90+t:2", 12) + fetch_eastmoney_board_rows("m:90+t:3", 12)
    except Exception as exc:
        compact_warning(status, "cnSectorWarnings", f"Public sector fallback unavailable: {exc}")
        return []

    seen = set()
    for row in rows:
        name = row.get("f14")
        if not name or name in seen:
            continue
        seen.add(name)
        stocks = []
        first = normalize_sector_stock(row.get("f128"), row.get("f140"), row.get("f136"), "领涨股")
        second = normalize_sector_stock(row.get("f207"), row.get("f208"), None, "活跃股")
        for item in (first, second):
            if item and item.get("code") not in {stock.get("code") for stock in stocks}:
                stocks.append(item)
        sectors.append({
            "name": name,
            "pct": number_from_keys(row, ("f3",)),
            "status": "公开板块涨幅排行",
            "source": "东方财富板块排行",
            "code": row.get("f12"),
            "moneyFlow": row.get("f62"),
            "stocks": stocks,
        })

    sectors.sort(key=lambda item: item.get("pct", 0), reverse=True)
    if sectors:
        status["cnSectors"] = f"Public sector strength updated {len(sectors[:8])} sectors"
    return sectors[:8]


def apply_cn_sector_strength(market_data, status, ak=None):
    cn = market_data["markets"]["cn"]
    sectors = fetch_kpl_sectors(status)
    if not sectors and ak:
        sectors = fetch_akshare_sectors(ak, status)
    if not sectors:
        sectors = fetch_public_sector_strength(status)
    cn["sectors"] = sectors
    if not sectors:
        status["cnSectors"] = "No full-market sector strength source available"


def apply_akshare_cn_quotes(market_data, status, ak):
    cn = market_data["markets"]["cn"]
    updates = 0

    try:
        spot_df = call_akshare(ak.stock_zh_a_spot_em, "stock_zh_a_spot_em", status)
        spot_rows = df_records(spot_df) if spot_df is not None else []
        spot_by_code = {as_text(row.get("代码")).zfill(6): row for row in spot_rows if as_text(row.get("代码"))}
    except Exception as exc:
        compact_warning(status, "akshareWarnings", f"stock_zh_a_spot_em unavailable: {exc}")
        spot_by_code = {}

    for stock in cn.get("stocks", []):
        code = as_text(stock.get("code")).zfill(6)
        row = spot_by_code.get(code)
        if not row:
            continue
        latest = as_number(row.get("最新价"))
        pct = as_number(row.get("涨跌幅"))
        if pct is not None:
            stock["pct"] = pct
        if latest is not None:
            stock["latestPrice"] = latest
        stock["turnover"] = row.get("成交额")
        stock["turnoverRate"] = row.get("换手率")
        stock["reason"] = "已接入 AKShare 全 A 行情数据，涨跌幅与成交额来自公开行情源。"
        updates += 1

    status["cnAkshareQuotes"] = f"AKShare spot updated {updates} CN stocks" if updates else "AKShare spot returned no watched stocks"

    try:
        index_df = call_akshare(ak.stock_zh_index_spot_em, "stock_zh_index_spot_em", status)
        index_rows = df_records(index_df) if index_df is not None else []
    except Exception as exc:
        compact_warning(status, "akshareWarnings", f"stock_zh_index_spot_em unavailable: {exc}")
        index_rows = []

    for metric in cn.get("metrics", []):
        expected_code = CN_INDEX_AK_CODES.get(metric.get("name"))
        for row in index_rows:
            code = as_text(row.get("代码")).zfill(6)
            name = as_text(row.get("名称"))
            if expected_code and code != expected_code and metric.get("name") != name:
                continue
            value = as_number(row.get("最新价"))
            pct = as_number(row.get("涨跌幅"))
            if value is not None:
                metric["value"] = f"{value:.2f}"
            if pct is not None:
                metric["pct"] = pct
            break


def apply_sina_cn(market_data, status):
    cn = market_data["markets"]["cn"]
    updates = 0

    for metric in cn.get("metrics", []):
        symbol = CN_INDEX_CODES.get(metric.get("name"))
        if not symbol:
            continue
        try:
            rows = fetch_sina_cn_daily(symbol)
            closes = [row["close"] for row in rows]
            if not closes:
                continue
            metric["value"] = f"{closes[-1]:.2f}"
            if len(closes) >= 2 and closes[-2]:
                metric["pct"] = round((closes[-1] / closes[-2] - 1) * 100, 2)
            updates += 1
        except Exception as exc:
            compact_warning(status, "cnWarnings", f"{metric.get('name')}: {exc}")

    for stock in cn.get("stocks", []):
        try:
            rows = fetch_sina_cn_daily(cn_symbol(stock.get("code")))
            closes = [row["close"] for row in rows]
            if not closes:
                continue
            update_quote_from_closes(stock, closes, rows[-1]["date"], "新浪财经")
            updates += 1
        except Exception as exc:
            compact_warning(status, "cnWarnings", f"{stock.get('code')}: {exc}")

    status["cn"] = (
        f"Sina Finance delayed data updated {updates} CN items"
        if updates
        else "Sina Finance returned no usable CN data; using seed data"
    )


def apply_sina_us(market_data, status):
    us = market_data["markets"]["us"]
    updates = 0
    metric_symbols = [metric.get("name") for metric in us.get("metrics", []) if metric.get("name")]
    stock_symbols = [stock.get("code") for stock in us.get("stocks", []) if stock.get("code")]
    symbols = []
    for symbol in metric_symbols + stock_symbols:
        if symbol and symbol not in symbols:
            symbols.append(symbol)

    cache = {}
    for symbol in symbols:
        try:
            rows = fetch_sina_us_daily(symbol)
            closes = [row["close"] for row in rows]
            if closes:
                cache[symbol] = (rows, closes)
        except Exception as exc:
            compact_warning(status, "usWarnings", f"{symbol}: {exc}")

    for metric in us.get("metrics", []):
        symbol = metric.get("name")
        if symbol in cache:
            rows, closes = cache[symbol]
            metric["value"] = f"{closes[-1]:.2f}"
            if len(closes) >= 2 and closes[-2]:
                metric["pct"] = round((closes[-1] / closes[-2] - 1) * 100, 2)
            updates += 1

    for stock in us.get("stocks", []):
        symbol = stock.get("code")
        if symbol in cache:
            rows, closes = cache[symbol]
            update_quote_from_closes(stock, closes, rows[-1]["date"], "新浪财经")
            updates += 1

    status["us"] = (
        f"Sina Finance delayed data updated {updates} US items"
        if updates
        else "Sina Finance returned no usable US data; using seed data"
    )


def build_market_data(seed, date_value, last_run):
    market_data = {
        "lastRun": last_run,
        "generatedDate": date_value,
        "source": "akshare-public-delayed-data",
        "sourceNote": "Generated by scripts/fetch-market-data.py from AKShare, KPL when configured, and public delayed web data. Missing or failed sources fall back to data/market-seed.json.",
        "markets": {},
        "research": deepcopy(seed.get("research", [])),
        "apiPlan": deepcopy(seed.get("apiPlan", {})),
        "providerStatus": {},
    }
    for name, market in seed["markets"].items():
        summary = market.get("summaryTemplate", "").replace("{date}", date_value)
        market_data["markets"][name] = {
            "label": market.get("label", name),
            "summary": summary,
            "metrics": deepcopy(market.get("metrics", [])),
            "sectors": deepcopy(market.get("sectors", [])),
            "stocks": deepcopy(market.get("stocks", [])),
        }
    return market_data


def history_reports(output_dir, current_date, current_last_run):
    reports = {}
    if output_dir.exists():
        for path in output_dir.glob("*.json"):
            if path.name in {"latest.json", "index.json"}:
                continue
            date_value = path.stem
            last_run = date_value
            try:
                report = json.loads(path.read_text(encoding="utf-8-sig"))
                last_run = report.get("lastRun", last_run)
            except Exception:
                pass
            reports[date_value] = {
                "date": date_value,
                "file": path.name,
                "lastRun": last_run,
            }
    reports[current_date] = {
        "date": current_date,
        "file": f"{current_date}.json",
        "lastRun": current_last_run,
    }
    return sorted(reports.values(), key=lambda item: item["date"], reverse=True)


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_outputs(market_data):
    date_value = market_data["generatedDate"]
    histories = history_reports(ROOT_DAILY_DIR, date_value, market_data["lastRun"])
    market_data["historyReports"] = histories

    for daily_dir in (ROOT_DAILY_DIR, SITE_DAILY_DIR):
        write_json(daily_dir / f"{date_value}.json", market_data)
        write_json(daily_dir / "latest.json", market_data)
        write_json(daily_dir / "index.json", histories)

    for realtime_dir in (ROOT_REALTIME_DIR, SITE_REALTIME_DIR):
        write_json(realtime_dir / "latest.json", market_data)

    data_js = "window.MARKET_DATA = " + json.dumps(market_data, ensure_ascii=False, indent=2) + ";\n"
    (ROOT / "data.js").write_text(data_js, encoding="utf-8")
    (SITE_DIR / "data.js").write_text(data_js, encoding="utf-8")


def main():
    if not SEED_PATH.exists():
        raise SystemExit(f"Seed file not found: {SEED_PATH}")

    now = china_now()
    date_value = now.strftime("%Y-%m-%d")
    last_run = now.strftime("%Y-%m-%d %H:%M CST")
    seed = json.loads(SEED_PATH.read_text(encoding="utf-8-sig"))
    market_data = build_market_data(seed, date_value, last_run)

    provider_status = market_data["providerStatus"]
    ak = load_akshare(provider_status)
    apply_sina_cn(market_data, provider_status)
    if ak:
        apply_akshare_cn_quotes(market_data, provider_status, ak)
    apply_cn_sector_strength(market_data, provider_status, ak)
    apply_sina_us(market_data, provider_status)
    write_outputs(market_data)

    print(f"Generated market data for {date_value}")
    print(json.dumps(provider_status, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"fetch-market-data failed: {exc}", file=sys.stderr)
        raise
