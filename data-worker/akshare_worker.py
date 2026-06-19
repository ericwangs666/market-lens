from __future__ import annotations

import contextlib
import io
from datetime import date, timedelta
from decimal import Decimal
from typing import Any

import akshare as ak
import pandas as pd


def _value(row: pd.Series, column: str) -> Decimal | None:
    value = row.get(column)
    if value is None or pd.isna(value):
        return None
    return Decimal(str(value))


def _number(value: Decimal | None) -> float | int | None:
    if value is None:
        return None
    if value == value.to_integral_value():
        return int(value)
    return float(value)


def fetch_a_daily_quote(symbol: str, trade_date: date) -> dict[str, Any]:
    try:
        return _fetch_eastmoney_quote(symbol, trade_date)
    except Exception as eastmoney_error:
        try:
            return _fetch_sina_quote(symbol, trade_date)
        except Exception as sina_error:
            raise RuntimeError(
                f"AKShare Eastmoney failed for {symbol}: {eastmoney_error}; "
                f"Sina fallback failed: {sina_error}"
            ) from sina_error


def _fetch_eastmoney_quote(symbol: str, trade_date: date) -> dict[str, Any]:
    compact_date = trade_date.strftime("%Y%m%d")
    captured_output = io.StringIO()
    with contextlib.redirect_stdout(captured_output):
        frame = ak.stock_zh_a_hist(
            symbol=symbol,
            period="daily",
            start_date=compact_date,
            end_date=compact_date,
            adjust="",
        )

    if frame is None or frame.empty:
        raise RuntimeError(f"No AKShare daily quote for {symbol} on {trade_date}")

    row = frame.iloc[-1]
    close_price = _value(row, "收盘")
    change_amount = _value(row, "涨跌额")
    pre_close_price = (
        close_price - change_amount
        if close_price is not None and change_amount is not None
        else None
    )

    return {
        "symbol": symbol,
        "market": "A",
        "tradeDate": trade_date.isoformat(),
        "openPrice": _number(_value(row, "开盘")),
        "highPrice": _number(_value(row, "最高")),
        "lowPrice": _number(_value(row, "最低")),
        "closePrice": _number(close_price),
        "preClosePrice": _number(pre_close_price),
        "changeAmount": _number(change_amount),
        "pctChange": _number(_value(row, "涨跌幅")),
        "volume": _number(_value(row, "成交量")),
        "amount": _number(_value(row, "成交额")),
        "dataSource": "AKSHARE",
    }


def _fetch_sina_quote(symbol: str, trade_date: date) -> dict[str, Any]:
    start_date = (trade_date - timedelta(days=14)).strftime("%Y%m%d")
    end_date = trade_date.strftime("%Y%m%d")
    captured_output = io.StringIO()
    with contextlib.redirect_stdout(captured_output):
        frame = ak.stock_zh_a_daily(
            symbol=_sina_symbol(symbol),
            start_date=start_date,
            end_date=end_date,
            adjust="",
        )

    if frame is None or frame.empty:
        raise RuntimeError(f"No AKShare Sina daily quote for {symbol} on {trade_date}")

    frame = frame.copy()
    frame["date"] = pd.to_datetime(frame["date"]).dt.date
    matching_indexes = frame.index[frame["date"] == trade_date].tolist()
    if not matching_indexes:
        raise RuntimeError(f"No AKShare Sina daily quote for {symbol} on {trade_date}")

    index = matching_indexes[-1]
    row = frame.loc[index]
    row_position = frame.index.get_loc(index)
    previous_close = None
    if isinstance(row_position, int) and row_position > 0:
        previous_close = _value(frame.iloc[row_position - 1], "close")

    close_price = _value(row, "close")
    change_amount = (
        close_price - previous_close
        if close_price is not None and previous_close is not None
        else None
    )
    pct_change = (
        change_amount / previous_close * Decimal("100")
        if change_amount is not None and previous_close not in (None, Decimal("0"))
        else None
    )

    return {
        "symbol": symbol,
        "market": "A",
        "tradeDate": trade_date.isoformat(),
        "openPrice": _number(_value(row, "open")),
        "highPrice": _number(_value(row, "high")),
        "lowPrice": _number(_value(row, "low")),
        "closePrice": _number(close_price),
        "preClosePrice": _number(previous_close),
        "changeAmount": _number(change_amount),
        "pctChange": _number(pct_change),
        "volume": _number(_value(row, "volume")),
        "amount": _number(_value(row, "amount")),
        "dataSource": "AKSHARE",
    }


def _sina_symbol(symbol: str) -> str:
    if symbol.startswith(("6", "9")):
        return f"sh{symbol}"
    if symbol.startswith(("0", "3")):
        return f"sz{symbol}"
    return f"bj{symbol}"
