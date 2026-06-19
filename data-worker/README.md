# Market Lens AKShare Data Worker

This module fetches A-share daily quotes with AKShare and prints normalized
JSON to standard output. It does not connect to PostgreSQL.

## Install

```bash
pip install -r requirements.txt
```

## Run

```bash
python fetch_a_daily_quotes.py \
  --market A \
  --tradeDate 2026-06-19 \
  --symbols "600000,000001"
```

Successful output is a JSON array. Errors are written to standard error and
the process exits with a non-zero status.
