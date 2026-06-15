# Market Lens

Market Lens is a static market dashboard MVP for tracking A-share and US market sectors, leading stocks, recent stock trends, and public institutional research links.

## Local Preview

Open:

```text
market-site/index.html
```

## GitHub Pages Deployment

This repository includes a GitHub Actions workflow at:

```text
.github/workflows/pages.yml
```

When the repository is pushed to the `main` branch, GitHub Pages deploys the contents of `market-site/`.

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
