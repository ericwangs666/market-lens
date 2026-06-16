import json
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


def request_text(url, referer, retries=3):
    headers = {
        "User-Agent": "Mozilla/5.0 market-lens/1.0",
        "Referer": referer,
    }
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
