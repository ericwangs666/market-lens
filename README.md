# Market Lens

Market Lens is a static market dashboard MVP for tracking A-share and US market sectors, leading stocks, recent stock trends, and public institutional research links.

## Local Preview

Open:

```text
market-site/index.html
```

## Daily Data Generation

Daily market data is generated from licensed API secrets when available, with seed data as a fallback:

```text
data/market-seed.json
```

GitHub Actions reads these repository secrets:

```text
TUSHARE_TOKEN
ALPHA_VANTAGE_API_KEY
```

Run the API generator:

```text
python scripts/fetch-market-data.py
```

The script updates:

```text
data.js
daily/latest.json
daily/YYYY-MM-DD.json
realtime/latest.json
market-site/data.js
market-site/daily/latest.json
market-site/daily/YYYY-MM-DD.json
market-site/realtime/latest.json
```

If an API token is missing or a provider is rate-limited, the script keeps the current seed data so the site can still publish.

## GitHub Pages Deployment

This repository includes a GitHub Actions workflow at:

```text
.github/workflows/pages.yml
```

When the repository is pushed to the `main` branch, GitHub Pages deploys the contents of `market-site/`.

The scheduled workflow at:

```text
.github/workflows/daily-data.yml
```

runs on weekdays, regenerates the daily data files, commits them to `main`, and then the Pages workflow publishes the updated site.

In GitHub, enable Pages with:

```text
Settings -> Pages -> Build and deployment -> Source: GitHub Actions
```

## Next Steps

- Connect licensed A-share and US stock data APIs.
- Generate daily JSON files after market close.
- Add self-selected stocks and historical views.
- Add automated daily status notifications.
- Add stock notes and threshold alerts.

## Architecture

See:

```text
market-site/ARCHITECTURE.md
```
