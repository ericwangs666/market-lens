# Market Lens 部署说明

这是一个静态网站 MVP，可以直接部署到 Cloudflare Pages、Vercel、Netlify 或 GitHub Pages。

## 推荐架构

1. 前端：`market-site/` 静态页面。
2. 数据：每天生成一个 JSON 文件，例如 `daily/2026-06-12.json`。
3. 定时：GitHub Actions、Cloudflare Workers Cron、Vercel Cron 或 Codex Automation。
4. 数据源：生产环境建议使用授权 API，避免直接复制东方财富、同花顺页面数据做公开服务。

## 可接入数据源

- A股：Tushare Pro、聚宽、Ricequant、Wind/Choice（商业）、交易所公开数据。
- 美股：Polygon.io、IEX Cloud、Alpha Vantage、Nasdaq Data Link、官方 Nasdaq API（需确认授权范围）。
- 研报：高盛、摩根士丹利公开 Insights 页面；正式研究门户通常需要登录授权。

## 云端发布步骤

1. 把 `market-site/` 推到 GitHub 仓库。
2. 在 Cloudflare Pages 或 Vercel 选择该目录作为静态站点。
3. 设置每日定时任务，收盘后生成最新数据文件。
4. 页面读取最新 JSON，展示“今日已更新 / 更新失败 / 数据延迟”。

## 下一步开发

- 接入真实行情 API。
- 增加自选股和历史搜索。
- 增加用户登录和收藏。
- 增加每日自动通知。
- 增加研报标题抓取和产业标签分类。
