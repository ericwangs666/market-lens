# Market Lens

Market Lens is a static market dashboard MVP for tracking A-share and US market sectors, leading stocks, recent stock trends, and public institutional research links.

## Local Preview

Open:

```text
market-site/index.html
```

## Daily Data Generation

Daily market data is generated from:

```text
data/market-seed.json
```

Run it locally:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\generate-daily-data.ps1
```

The script updates:

```text
data.js
market-site/data.js
market-site/daily/latest.json
market-site/daily/YYYY-MM-DD.json
```

For production, replace or enrich `data/market-seed.json` in the script with licensed market APIs such as Tushare Pro, Polygon, IEX Cloud, Alpha Vantage, or Nasdaq Data Link.

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
