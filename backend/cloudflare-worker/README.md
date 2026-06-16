# Market Lens Update Backend

This Cloudflare Worker stores the GitHub token as a backend secret and lets the page trigger the daily data workflow without asking the browser user for a token.

Do not put the GitHub token in frontend code. Store it as the Worker secret named `GITHUB_TOKEN`.

## Deploy

1. Install Wrangler:

```powershell
npm install -g wrangler
```

2. Log in to Cloudflare:

```powershell
wrangler login
```

3. Deploy from this folder:

```powershell
cd C:\Users\15224\projects\99\99\backend\cloudflare-worker
wrangler deploy
```

4. Add the GitHub token as a secret:

```powershell
wrangler secret put GITHUB_TOKEN
```

The token needs `repo` and `workflow` permissions.

5. Copy the deployed Worker URL, for example:

```text
https://market-lens-update.your-name.workers.dev
```

6. Set the update endpoint in both `config.js` and `market-site/config.js`:

```js
window.MARKET_LENS_UPDATE_ENDPOINT = "https://market-lens-update.your-name.workers.dev/api/update";
```

After this is deployed, the page button calls the backend directly and no longer asks for a GitHub token.

## Endpoints

```text
POST /api/update
```

Returns:

```json
{ "ok": true, "message": "Workflow dispatch accepted" }
```

```text
GET /api/health
```

Checks whether the Worker is online.
