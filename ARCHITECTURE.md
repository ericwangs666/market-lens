# Market Lens API 与提醒架构

## 目标

把当前静态 MVP 升级成可长期使用的网站：

- A股与美股分开展示。
- 查看个股近期走势、热门板块、龙头个股和机构公开观点。
- 记录个股备忘录。
- 当自选股涨幅、跌幅、成交额或板块热度达到阈值时提醒。
- 每天自动生成市场复盘并部署到云端。

## 推荐数据源

### A股

- Tushare Pro：日线、指数、板块、财务、公告，适合每日复盘。
- 聚宽 / Ricequant：研究、分钟线、因子和策略数据。
- Wind / Choice：机构级商业数据，适合正式商业化。

### 美股

- Polygon.io：实时或延迟行情、聚合 K 线、公司事件，适合实时提醒。
- IEX Cloud：报价、基本面、新闻，适合美股个股页。
- Alpha Vantage：低成本 MVP，频率有限。
- Nasdaq Data Link：研究型数据和宏观数据。

## 功能模块

### 1. 数据采集

定时任务：

- A股盘后：北京时间 15:15-17:30 生成日终数据。
- 美股盘后：美东收盘后生成日终数据。
- 盘中提醒：每 1-5 分钟拉取自选股报价。

输出文件：

```text
daily/2026-06-12.json
watchlist/latest-quotes.json
research/latest.json
```

### 2. 用户备忘录

MVP 可用浏览器 `localStorage`。

生产环境建议：

- Supabase / Firebase / Cloudflare D1 存用户、自选股、备注和提醒规则。
- 用户表：`users`
- 备忘录表：`stock_notes`
- 规则表：`alert_rules`
- 触发历史表：`alert_events`

### 3. 提醒规则

规则示例：

- 当 `601958` 今日涨幅 >= 5% 提醒。
- 当 `NVDA` 盘中涨幅 >= 3% 且成交额放大提醒。
- 当某个板块进入涨幅榜前 5 且自选股在该板块提醒。
- 当高盛/大摩公开文章出现 AI、semiconductor、copper、biotech 关键词提醒。

防打扰策略：

- 同一股票同一规则每天只提醒一次。
- 如果突破更高阈值，例如 5%、8%、10%，可再次提醒。
- 记录提醒时间、触发价、触发涨幅和行情源。

### 4. 推送渠道

优先级建议：

1. 邮件：实现最简单，适合每日复盘。
2. Telegram Bot：适合个人实时提醒。
3. 企业微信 / 飞书机器人：适合桌面通知。
4. 浏览器 Push：体验好，但需要后端和 HTTPS。
5. 短信：成本高，作为重要提醒备用。

### 5. GitHub Pages 限制

GitHub Pages 只能托管静态页面，不能安全保存 API Key，也不能跑实时后端。

推荐组合：

- 前端：GitHub Pages。
- 定时生成数据：GitHub Actions 或 Cloudflare Workers Cron。
- 密钥：GitHub Actions Secrets 或 Cloudflare Secrets。
- 用户备忘录与提醒：Supabase / Firebase / Cloudflare D1。

## MVP 到生产的路线

1. 当前版本：静态网站 + 本地备忘录 + 本地阈值判断。
2. 第一步云端：GitHub Pages + GitHub Actions 每天生成 `data.js`。
3. 第二步数据：接入授权行情 API。
4. 第三步账户：Supabase 登录、自选股、备注云同步。
5. 第四步提醒：Cloudflare Worker 定时拉行情并推送通知。
6. 第五步研报：抓取公开机构文章标题和摘要，按产业标签归类。
