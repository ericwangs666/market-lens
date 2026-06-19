# Market Lens

Market Lens is a market dashboard project with:

- a static frontend in the repository root and `market-site/`
- a Java Spring Boot backend in `backend/spring-boot/`
- PostgreSQL persistence managed by Flyway
- Redis-based alert dedupe
- a Python AKShare worker in `data-worker/` for free A-share daily quotes
- development-only admin test endpoints for mock data, alert checks, and daily review generation

## Project Structure

- Frontend entry: `index.html`
- Published static copy: `market-site/index.html`
- Frontend API wrapper: `src/api.js`
- Backend entry class: `backend/spring-boot/src/main/java/com/marketlens/backend/MarketLensBackendApplication.java`
- Backend config: `backend/spring-boot/src/main/resources/application.yml`
- Flyway SQL: `backend/spring-boot/src/main/resources/db/migration/`
- AKShare worker: `data-worker/fetch_a_daily_quotes.py`

The existing static pages, mock market data, and local fallback behavior are still retained.

## Frontend

The root frontend is a static page and can be previewed directly or served locally:

```powershell
python -m http.server 5173
```

Open:

```text
http://localhost:5173/index.html
```

The frontend uses:

```text
src/api.js
config.js
app.js
```

By default the frontend points to:

```js
window.MARKET_LENS_API_BASE = window.MARKET_LENS_API_BASE || "http://localhost:8080/api";
```

The root project also includes a minimal build command for acceptance verification:

```powershell
npm install
npm run build
```

The build output goes to:

```text
dist/
```

## Spring Boot Backend

The backend lives in:

```text
backend/spring-boot/
```

Requirements:

```text
Java 17
PostgreSQL
Redis
```

Database and Redis settings come from environment variables. No real API key, token, or database password should be committed.

Important variables:

```text
SERVER_PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/market_lens
DB_USERNAME=market_lens
DB_PASSWORD=<your-local-password>
FLYWAY_ENABLED=true
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
SPRING_PROFILES_ACTIVE=dev
MARKET_DATA_USE_MOCK=false
MARKET_DATA_FALLBACK_TO_MOCK=true
MARKET_DATA_PROVIDER_A=AKSHARE
AKSHARE_PYTHON_COMMAND=python
AKSHARE_WORKER_PATH=../../data-worker/fetch_a_daily_quotes.py
```

Start the backend:

```powershell
cd backend/spring-boot
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_URL="jdbc:postgresql://localhost:5432/market_lens"
$env:DB_USERNAME="market_lens"
$env:DB_PASSWORD="<your-local-password>"
$env:FLYWAY_ENABLED="true"
$env:ALERT_SCHEDULER_ENABLED="false"
.\mvnw.cmd spring-boot:run
```

Health check:

```text
GET http://localhost:8080/api/health
```

Expected response:

```json
{
  "code": 0,
  "message": "success",
  "data": "ok"
}
```

## AKShare A-Share Data

Market Lens currently uses AKShare as the preferred A-share provider because it
is free, does not require a Tushare token, and is suitable for personal
research and an MVP. The Java backend remains responsible for persistence,
analysis, reviews, alerts, and APIs. Python only fetches quotes and prints JSON.

Tushare remains a future optional provider design. It is not required for the
current startup path. US quotes can continue to use mock data or a future Alpha
Vantage provider.

Install the Python dependencies:

```powershell
cd data-worker
pip install -r requirements.txt
```

Test the worker independently:

```powershell
python fetch_a_daily_quotes.py `
  --market A `
  --tradeDate 2026-06-19 `
  --symbols "600000,000001"
```

The worker prints a JSON array to standard output. It never connects to the
database and contains no API key or database password.

Provider configuration:

```yaml
market-data:
  use-mock: false
  fallback-to-mock: true
  provider-a: AKSHARE
  akshare:
    python-command: ${AKSHARE_PYTHON_COMMAND:python}
    worker-path: ${AKSHARE_WORKER_PATH:../data-worker/fetch_a_daily_quotes.py}
```

Use `MARKET_DATA_USE_MOCK=true` to force mock quotes. With mock disabled,
`provider-a=AKSHARE` invokes the Python worker. If AKShare fails and
`MARKET_DATA_FALLBACK_TO_MOCK=true`, the ingest continues with mock data and
writes a `WARNING` row to `job_run_logs`. If fallback is disabled, the task
fails and writes `FAILED`.

AKShare public data is mainly appropriate for personal research and MVP
validation. A commercial deployment should move to a formally licensed market
data provider.

## Local PostgreSQL

This repository does not currently include a `docker-compose.yml`, so the quickest local startup path is a standalone container:

```powershell
$env:POSTGRES_PASSWORD="<your-local-password>"
docker run --name market-lens-postgres `
  -e POSTGRES_DB=market_lens `
  -e POSTGRES_USER=market_lens `
  -e POSTGRES_PASSWORD=$env:POSTGRES_PASSWORD `
  -p 5432:5432 `
  -d postgres:16
```

If it already exists:

```powershell
docker start market-lens-postgres
```

Redis example:

```powershell
docker run --name market-lens-redis -p 6379:6379 -d redis:7
```

## API Surface

Core backend endpoints:

```text
GET    /api/health
GET    /api/stocks/search?keyword=NVDA
GET    /api/stocks/NVDA
GET    /api/watchlist
POST   /api/watchlist
DELETE /api/watchlist/{id}
GET    /api/notes
GET    /api/notes/{symbol}
POST   /api/notes
PUT    /api/notes/{id}
DELETE /api/notes/{id}
GET    /api/alerts/rules
POST   /api/alerts/rules
PUT    /api/alerts/rules/{id}
DELETE /api/alerts/rules/{id}
GET    /api/alerts/events
GET    /api/market/daily-review
GET    /api/market/daily-quotes?market=A&tradeDate=2026-06-19
```

Development-only admin endpoints:

```text
POST /api/admin/alerts/check
POST /api/admin/market-review/generate
GET  /api/admin/market-data/providers
POST /api/admin/market-data/test?market=A
POST /api/admin/market-data/ingest?market=A&tradeDate=2026-06-19
POST /api/admin/market-daily-job/run?market=A&tradeDate=2026-06-19
```

Legacy compatibility endpoints such as `/api/memos`, `/api/alert-rules`, and `/api/alert-history` are still present so older frontend behavior does not break.

## Acceptance And Test Verification

### 1. Verify Project Structure

Check these paths:

```text
README.md
index.html
src/api.js
backend/spring-boot/pom.xml
backend/spring-boot/src/main/java
backend/spring-boot/src/main/resources/application.yml
backend/spring-boot/src/main/resources/db/migration
```

### 2. Check Git Status

```powershell
git status
```

Make sure these are not committed:

```text
.env
.idea/
node_modules/
dist/
target/
application-local.yml
```

### 3. Run Backend Tests And Package

```powershell
cd backend/spring-boot
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

Expected:

- tests pass
- packaging succeeds
- `target/market-lens-backend-0.1.0-SNAPSHOT.jar` exists

### 4. Verify Flyway

After starting the backend with `FLYWAY_ENABLED=true`:

```powershell
docker exec -it market-lens-postgres psql -U market_lens -d market_lens -c "\dt"
docker exec -it market-lens-postgres psql -U market_lens -d market_lens -c "select version, success from flyway_schema_history order by installed_rank;"
```

Expected core tables:

```text
users
stocks
watchlists
stock_notes
daily_quotes
alert_rules
alert_events
market_reviews
research_items
job_run_logs
flyway_schema_history
```

### 5. Test APIs With curl

Health:

```powershell
curl http://localhost:8080/api/health
```

Search stock:

```powershell
curl "http://localhost:8080/api/stocks/search?keyword=NVDA"
curl "http://localhost:8080/api/stocks/NVDA"
```

Add watchlist:

```powershell
curl -X POST http://localhost:8080/api/watchlist `
  -H "Content-Type: application/json" `
  -d "{\"symbol\":\"NVDA\",\"market\":\"US\"}"
```

List watchlist:

```powershell
curl http://localhost:8080/api/watchlist
```

Add note:

```powershell
curl -X POST http://localhost:8080/api/notes `
  -H "Content-Type: application/json" `
  -d "{\"symbol\":\"NVDA\",\"note\":\"AI leader, watch data center revenue and margin.\"}"
```

Query note by symbol:

```powershell
curl http://localhost:8080/api/notes/NVDA
```

Update note:

```powershell
curl -X PUT http://localhost:8080/api/notes/1 `
  -H "Content-Type: application/json" `
  -d "{\"symbol\":\"NVDA\",\"note\":\"Updated thesis after earnings.\"}"
```

Add alert rule:

```powershell
curl -X POST http://localhost:8080/api/alerts/rules `
  -H "Content-Type: application/json" `
  -d "{\"symbol\":\"NVDA\",\"ruleType\":\"PCT_CHANGE\",\"operator\":\">=\",\"thresholdValue\":3,\"enabled\":true,\"oncePerDay\":true}"
```

List alert rules:

```powershell
curl http://localhost:8080/api/alerts/rules
```

List alert events:

```powershell
curl http://localhost:8080/api/alerts/events
```

Get latest daily review:

```powershell
curl http://localhost:8080/api/market/daily-review
```

Check the configured provider:

```powershell
curl http://localhost:8080/api/admin/market-data/providers
```

Test AKShare without writing to PostgreSQL:

```powershell
curl -X POST "http://localhost:8080/api/admin/market-data/test?market=A"
```

The test endpoint checks today first and then searches backward up to seven
days for the most recent available trading day.

Ingest A-share quotes:

```powershell
curl -X POST "http://localhost:8080/api/admin/market-data/ingest?market=A&tradeDate=2026-06-19"
```

Run the complete A-share daily job:

```powershell
curl -X POST "http://localhost:8080/api/admin/market-daily-job/run?market=A&tradeDate=2026-06-19"
```

Query the stored quotes:

```powershell
curl "http://localhost:8080/api/market/daily-quotes?market=A&tradeDate=2026-06-19"
```

The complete job reads A-share watchlist symbols, fetches quotes, performs an
idempotent upsert using `(market, symbol, trade_date)`, generates the market
review, and records the outcome in `job_run_logs`.

### 6. Verify Alert Triggering

Generate mock data first:

```powershell
curl -X POST http://localhost:8080/api/admin/market-review/generate
```

Create a rule matching the mock quote:

```powershell
curl -X POST http://localhost:8080/api/alerts/rules `
  -H "Content-Type: application/json" `
  -d "{\"symbol\":\"NVDA\",\"ruleType\":\"PCT_CHANGE\",\"operator\":\">=\",\"thresholdValue\":3,\"enabled\":true,\"oncePerDay\":true}"
```

Run the alert engine:

```powershell
curl -X POST http://localhost:8080/api/admin/alerts/check
```

Then verify:

```powershell
curl http://localhost:8080/api/alerts/events
```

Expected event fields include:

```text
stockCode
triggerPrice
triggerPct
triggeredAt
message
channel
```

Run the alert check twice on the same day to verify `oncePerDay=true` prevents duplicate events.

### 7. Verify Daily Review Generation

```powershell
curl -X POST http://localhost:8080/api/admin/market-review/generate
curl http://localhost:8080/api/market/daily-review
```

This should:

- create or update the latest daily review
- persist it in `market_reviews`
- make it available to the frontend research view

### 8. Verify Frontend Uses The Backend

Serve the frontend:

```powershell
python -m http.server 5173
```

Open:

```text
http://localhost:5173/index.html
```

Then verify in the browser network panel that the page calls:

```text
/api/watchlist
/api/notes
/api/alerts/rules
/api/market/daily-review
```

If the backend is unavailable, the page should fall back to local mock/static data and show a backend unavailable state instead of crashing.

### 9. Verify Frontend Build

From the repository root:

```powershell
npm install
npm run build
```

Expected:

- no syntax errors
- no missing path errors
- `dist/` is generated

### 10. Confirm No Secrets Leak To Frontend

Search likely secret names:

```powershell
Select-String -Path index.html,app.js,config.js,src\api.js,market-site\index.html,market-site\app.js,market-site\config.js,market-site\src\api.js `
  -Pattern "API_KEY|TOKEN|SECRET|DB_PASSWORD|TELEGRAM_BOT_TOKEN|GITHUB_TOKEN"
```

Expected result:

- no real API key in frontend files
- no real database password in committed files
- only public API base URLs or placeholder names

## Cloudflare Worker Update Backend

The repository includes a Cloudflare Worker helper in:

```text
backend/cloudflare-worker/
```

It is used to trigger the GitHub Actions daily update without hardcoding a GitHub token into the frontend.

Store the GitHub token only as a backend secret:

```text
GITHUB_TOKEN
```

## GitHub Pages

The repository includes a Pages workflow at:

```text
.github/workflows/pages.yml
```

The published static copy lives under:

```text
market-site/
```

## Automated Test Coverage

Current backend tests cover:

```text
StockServiceTest
WatchlistServiceTest
StockNoteServiceTest
AlertRuleServiceTest
AlertEngineTest
MarketLensApiIntegrationTest
AkshareMarketDataProviderTest
MarketDataProviderFactoryTest
MarketDataIngestServiceTest
MarketDailyJobServiceTest
MarketDataApiTest
```
