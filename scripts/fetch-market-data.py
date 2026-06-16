import json
import os
import sys
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

TUSHARE_URL = "https://api.tushare.pro"
ALPHA_URL = "https://www.alphavantage.co/query"

CN_INDEX_CODES = {
    "上证指数": "000001.SH",
    "深证成指": "399001.SZ",
    "创业板指": "399006.SZ",
    "沪深300": "000300.SH",
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


def rounded(value):
    if value is None:
        return None
    return round(float(value), 2)


def request_json(url, payload=None):
    data = None
    headers = {"User-Agent": "market-lens/1.0"}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=data, headers=headers)
    with urllib.request.urlopen(request, timeout=25) as response:
        return json.loads(response.read().decode("utf-8"))


def tushare_call(token, api_name, params, fields):
    payload = {
        "api_name": api_name,
        "token": token,
        "params": params,
        "fields": fields,
    }
    result = request_json(TUSHARE_URL, payload)
    if result.get("code") != 0:
        raise RuntimeError(result.get("msg") or "Tushare request failed")
    data = result.get("data") or {}
    field_names = data.get("fields") or []
    rows = data.get("items") or []
    return [dict(zip(field_names, row)) for row in rows]


def is_permission_error(exc):
    message = str(exc)
    return "权限" in message or "permission" in message.lower() or "没有接口" in message


def compact_warning(status, key, message):
    warnings = status.setdefault(key, [])
    if message not in warnings:
        warnings.append(message)


def fetch_tushare_basics(token, status, end_date):
    basics = {}
    calendar_rows = []

    try:
        start_date = (china_now() - timedelta(days=14)).strftime("%Y%m%d")
        calendar_rows = tushare_call(
            token,
            "trade_cal",
            {"exchange": "SSE", "start_date": start_date, "end_date": end_date},
            "cal_date,is_open,pretrade_date",
        )
        open_days = [row["cal_date"] for row in calendar_rows if str(row.get("is_open")) == "1"]
        if open_days:
            status["cnTradeCalendar"] = {
                "latestOpenDate": open_days[-1],
                "recentOpenDays": open_days[-5:],
            }
    except Exception as exc:
        compact_warning(status, "cnWarnings", f"trade_cal unavailable: {exc}")

    try:
        stock_rows = tushare_call(
            token,
            "stock_basic",
            {"list_status": "L"},
            "ts_code,symbol,name,area,industry,list_date",
        )
        for row in stock_rows:
            basics[row.get("symbol")] = row
            basics[row.get("ts_code")] = row
        status["cnStockBasic"] = {
            "count": len(stock_rows),
            "matched": 0,
        }
    except Exception as exc:
        compact_warning(status, "cnWarnings", f"stock_basic unavailable: {exc}")

    return basics, calendar_rows


def stock_to_ts_code(code):
    code = str(code).strip()
    if "." in code:
        return code
    if code.startswith(("6", "9")):
        return f"{code}.SH"
    if code.startswith(("0", "2", "3")):
        return f"{code}.SZ"
    if code.startswith(("4", "8")):
        return f"{code}.BJ"
    return code


def fetch_tushare_series(token, api_name, ts_code, start_date, end_date):
    rows = tushare_call(
        token,
        api_name,
        {"ts_code": ts_code, "start_date": start_date, "end_date": end_date},
        "ts_code,trade_date,close,pct_chg",
    )
    rows = [row for row in rows if row.get("close") not in (None, "")]
    return sorted(rows, key=lambda row: row["trade_date"])


def apply_tushare(seed, market_data, status):
    token = os.environ.get("TUSHARE_TOKEN", "").strip()
    if not token:
        status["cn"] = "missing TUSHARE_TOKEN; using seed data"
        return

    end_date = china_now().strftime("%Y%m%d")
    start_date = (china_now() - timedelta(days=120)).strftime("%Y%m%d")
    cn = market_data["markets"]["cn"]
    updates = 0
    basics, _calendar_rows = fetch_tushare_basics(token, status, end_date)
    matched_basics = 0
    index_daily_blocked = False
    daily_blocked = False

    for metric in cn.get("metrics", []):
        if index_daily_blocked:
            break
        ts_code = CN_INDEX_CODES.get(metric.get("name"))
        if not ts_code:
            continue
        try:
            rows = fetch_tushare_series(token, "index_daily", ts_code, start_date, end_date)
            if rows:
                latest = rows[-1]
                metric["value"] = f"{float(latest['close']):.2f}"
                metric["pct"] = rounded(latest.get("pct_chg"))
                updates += 1
        except Exception as exc:
            if is_permission_error(exc):
                status["cnIndexDaily"] = "no access to index_daily; keeping seed index data"
                index_daily_blocked = True
            else:
                compact_warning(status, "cnWarnings", f"{metric.get('name')}: {exc}")

    for stock in cn.get("stocks", []):
        ts_code = stock_to_ts_code(stock["code"])
        basic = basics.get(str(stock.get("code"))) or basics.get(ts_code)
        if basic:
            matched_basics += 1
            stock["profile"] = {
                "tsCode": basic.get("ts_code"),
                "area": basic.get("area"),
                "industry": basic.get("industry"),
                "listDate": basic.get("list_date"),
            }
        if daily_blocked:
            continue
        try:
            rows = fetch_tushare_series(token, "daily", ts_code, start_date, end_date)
            closes = [float(row["close"]) for row in rows]
            if closes:
                latest = rows[-1]
                stock["pct"] = rounded(latest.get("pct_chg"))
                stock["prices"] = [round(value, 2) for value in closes[-6:]]
                stock["d5"] = pct_change(closes, 5)
                stock["d20"] = pct_change(closes, 20)
                stock["d60"] = pct_change(closes, 60)
                stock["reason"] = f"已接入 Tushare 日线数据，最新交易日 {latest['trade_date']}。"
                updates += 1
        except Exception as exc:
            if is_permission_error(exc):
                status["cnDaily"] = "no access to daily; keeping seed stock quotes"
                daily_blocked = True
            else:
                compact_warning(status, "cnWarnings", f"{stock.get('code')}: {exc}")

    if "cnStockBasic" in status:
        status["cnStockBasic"]["matched"] = matched_basics
    if updates:
        status["cn"] = f"Tushare updated {updates} CN quote items"
    elif matched_basics or status.get("cnTradeCalendar"):
        status["cn"] = "Tushare token works for low-permission metadata; quote data is using seed fallback"
    else:
        status["cn"] = "Tushare returned no usable CN data"


def alpha_query(params):
    url = f"{ALPHA_URL}?{urllib.parse.urlencode(params)}"
    result = request_json(url)
    if "Error Message" in result:
        raise RuntimeError(result["Error Message"])
    if "Note" in result:
        raise RuntimeError(result["Note"])
    if "Information" in result:
        raise RuntimeError(result["Information"])
    return result


def fetch_alpha_daily(api_key, symbol):
    result = alpha_query({
        "function": "TIME_SERIES_DAILY",
        "symbol": symbol,
        "outputsize": "compact",
        "apikey": api_key,
    })
    series = result.get("Time Series (Daily)")
    if not series:
        raise RuntimeError("Alpha Vantage daily series missing")
    rows = []
    for date_value, values in series.items():
        rows.append({
            "date": date_value,
            "close": float(values["4. close"]),
        })
    return sorted(rows, key=lambda row: row["date"])


def update_quote_from_closes(target, closes, latest_date):
    if len(closes) >= 2 and closes[-2]:
        target["pct"] = round((closes[-1] / closes[-2] - 1) * 100, 2)
    target["prices"] = [round(value, 2) for value in closes[-6:]]
    target["d5"] = pct_change(closes, 5)
    target["d20"] = pct_change(closes, 20)
    target["d60"] = pct_change(closes, 60)
    target["reason"] = f"已接入 Alpha Vantage 日线数据，最新交易日 {latest_date}。"


def apply_alpha_vantage(market_data, status):
    api_key = os.environ.get("ALPHA_VANTAGE_API_KEY", "").strip()
    if not api_key:
        status["us"] = "missing ALPHA_VANTAGE_API_KEY; using seed data"
        return

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
            rows = fetch_alpha_daily(api_key, symbol)
            closes = [row["close"] for row in rows]
            cache[symbol] = (rows, closes)
        except Exception as exc:
            status.setdefault("usWarnings", []).append(f"{symbol}: {exc}")

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
            update_quote_from_closes(stock, closes, rows[-1]["date"])
            updates += 1

    status["us"] = f"Alpha Vantage updated {updates} US items" if updates else "Alpha Vantage returned no usable US data"


def build_market_data(seed, date_value, last_run):
    market_data = {
        "lastRun": last_run,
        "generatedDate": date_value,
        "source": "market-api",
        "sourceNote": "Generated by scripts/fetch-market-data.py. Missing or failed API sources fall back to data/market-seed.json.",
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
    apply_tushare(seed, market_data, provider_status)
    apply_alpha_vantage(market_data, provider_status)
    write_outputs(market_data)

    print(f"Generated market data for {date_value}")
    print(json.dumps(provider_status, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"fetch-market-data failed: {exc}", file=sys.stderr)
        raise
