# Market Lens

Market Lens is a static market dashboard MVP for tracking A-share and US market sectors, leading stocks, recent stock trends, and public institutional research links.

## Local Preview

Open:

```text
market-site/index.html
```

## Daily Data Generation

Daily market data is generated from AKShare and public delayed data sources, with seed data as a fallback:

```text
data/market-seed.json
```

GitHub Actions installs AKShare automatically before generating data:

```text
python -m pip install -U pip akshare
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

If AKShare or a public data source is rate-limited, the script keeps the current seed data so the site can still publish.

## Manual Update Backend

The site can trigger the daily update directly from the page. For security, do not put a GitHub token in frontend code.

This repo includes a Cloudflare Worker backend at:

```text
backend/cloudflare-worker/
```

Deploy it, set the Worker secret:

```text
GITHUB_TOKEN
```

Then put the Worker update endpoint in both `config.js` and `market-site/config.js`:

```js
window.MARKET_LENS_UPDATE_ENDPOINT = "https://your-worker.workers.dev/api/update";
```

Once configured, the page's "手动更新数据" button calls the backend directly and no longer asks for a browser token.

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
