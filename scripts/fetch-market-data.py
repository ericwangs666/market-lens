import json
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


def number_from_keys(row, keys):
    for key in keys:
        value = row.get(key)
        if value not in (None, "", "-"):
            try:
                return round(float(value), 2)
            except (TypeError, ValueError):
                continue
    return 0


def text_from_keys(row, keys):
    for key in keys:
        value = row.get(key)
        if value not in (None, "", "-"):
            return str(value)
    return ""


def normalize_sector_stock(name, code, pct=None, type_name="领涨股"):
    if not name:
        return None
    stock = {
        "name": str(name),
        "code": str(code or ""),
        "type": type_name,
    }
    if pct not in (None, "", "-"):
        try:
            stock["pct"] = round(float(pct), 2)
        except (TypeError, ValueError):
            pass
    return stock


def fetch_kpl_sectors(status):
    url = os.environ.get("KPL_SECTOR_STRENGTH_URL", "").strip()
    if not url:
        status["cnSectors"] = "KPL sector source not configured; using public fallback"
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
        pct = number_from_keys(row, ("pct", "Pct", "change", "Change", "change_pct", "ChangePct", "zf", "ZhangFu", "strength", "Strength"))
        leader = normalize_sector_stock(
            text_from_keys(row, ("leader", "Leader", "stock_name", "StockName", "lead_stock_name")),
            text_from_keys(row, ("leader_code", "LeaderCode", "stock_code", "StockCode", "lead_stock_code")),
            number_from_keys(row, ("leader_pct", "LeaderPct", "stock_pct", "StockPct")),
        )
        sector = {
            "name": name,
            "pct": pct,
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


def build_stock_sector_strength(market_data, status):
    groups = {}
    for stock in market_data["markets"]["cn"].get("stocks", []):
        sector = stock.get("sector") or "观察池"
        groups.setdefault(sector, []).append(stock)

    sectors = []
    for name, stocks in groups.items():
        ranked = sorted(
            stocks,
            key=lambda item: item.get("pct") if item.get("pct") is not None else -999,
            reverse=True,
        )
        pct_values = [stock.get("pct") for stock in ranked if stock.get("pct") is not None]
        strength = round(sum(pct_values) / len(pct_values), 2) if pct_values else 0
        sectors.append({
            "name": name,
            "pct": strength,
            "status": "观察池涨幅计算",
            "source": "本地观察池",
            "stocks": [
                {
                    "name": stock.get("name"),
                    "code": stock.get("code"),
                    "pct": stock.get("pct"),
                    "type": stock.get("type"),
                }
                for stock in ranked[:4]
            ],
        })

    sectors.sort(key=lambda item: item.get("pct", 0), reverse=True)
    if sectors:
        status["cnSectors"] = "Local watchlist sector strength fallback"
    return sectors[:8]


def apply_cn_sector_strength(market_data, status):
    cn = market_data["markets"]["cn"]
    sectors = fetch_kpl_sectors(status)
    if not sectors:
        sectors = fetch_public_sector_strength(status)
    if not sectors:
        sectors = build_stock_sector_strength(market_data, status)
    if sectors:
        cn["sectors"] = sectors


def update_quote_from_closes(target, closes, latest_date, provider_name):
    if len(closes) >= 2 and closes[-2]:
        target["pct"] = round((closes[-1] / closes[-2] - 1) * 100, 2)
    target["prices"] = [round(value, 2) for value in closes[-6:]]
    target["d5"] = pct_change(closes, 5)
    target["d20"] = pct_change(closes, 20)
    target["d60"] = pct_change(closes, 60)
    target["reason"] = f"已接入{provider_name}延迟日线数据，最新交易日 {latest_date}。"


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
        "source": "public-delayed-web-data",
        "sourceNote": "Generated by scripts/fetch-market-data.py from Sina Finance public delayed daily data. Missing or failed sources fall back to data/market-seed.json.",
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
    apply_sina_cn(market_data, provider_status)
    apply_cn_sector_strength(market_data, provider_status)
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
