from __future__ import annotations

import argparse
import json
import sys
from datetime import date

from akshare_worker import fetch_a_daily_quote


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fetch A-share daily quotes from AKShare and output JSON."
    )
    parser.add_argument("--market", default="A", choices=["A"])
    parser.add_argument("--tradeDate", required=True)
    parser.add_argument("--symbols", required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        trade_date = date.fromisoformat(args.tradeDate)
        symbols = [
            value.strip()
            for value in args.symbols.split(",")
            if value.strip()
        ]
        if not symbols:
            raise ValueError("At least one symbol is required")

        quotes = [
            fetch_a_daily_quote(symbol, trade_date)
            for symbol in symbols
        ]
        print(json.dumps(quotes, ensure_ascii=False))
        return 0
    except Exception as exception:
        print(
            json.dumps(
                {
                    "error": "AKShare fetch failed",
                    "message": str(exception),
                    "market": args.market,
                    "tradeDate": args.tradeDate,
                },
                ensure_ascii=False,
            ),
            file=sys.stderr,
        )
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
