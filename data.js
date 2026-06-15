window.MARKET_DATA = {
  lastRun: "2026-06-12 22:19 CST",
  markets: {
    cn: {
      label: "A股",
      summary: "2026-06-12 收盘：指数普涨，资源金属、军工航空、券商和医疗分支领涨。",
      metrics: [
        { name: "上证指数", value: "4031.51", pct: 1.12 },
        { name: "深证成指", value: "14963.41", pct: 0.75 },
        { name: "创业板指", value: "3830.35", pct: 0.50 },
        { name: "沪深300", value: "4777.32", pct: 1.16 }
      ],
      sectors: [
        { name: "钼", pct: 10.00, status: "强趋势延续" },
        { name: "铅锌", pct: 9.14, status: "超跌反弹" },
        { name: "铜", pct: 7.03, status: "资源线扩散" },
        { name: "诊断服务", pct: 7.03, status: "医疗分支修复" },
        { name: "航天航空", pct: 4.42, status: "低位补涨" },
        { name: "券商概念", pct: 3.99, status: "情绪修复" },
        { name: "AI制药医疗", pct: 2.87, status: "新兴产业分支" }
      ],
      stocks: [
        { name: "迪安诊断", code: "300244", pct: 18.01, type: "涨幅/成交龙头", sector: "诊断服务", d5: 17.66, d20: 5.66, d60: -1.25, reason: "医疗分支共振，成交额放大。", prices: [15.9, 16.2, 16.0, 16.8, 17.0, 19.79] },
        { name: "金钼股份", code: "601958", pct: 10.00, type: "资源权重龙头", sector: "钼", d5: 16.21, d20: 21.29, d60: 18.20, reason: "钼板块涨停，连续两日强势。", prices: [20.8, 21.0, 20.95, 21.09, 23.2, 25.52] },
        { name: "海亮股份", code: "002203", pct: 10.00, type: "铜链成交龙头", sector: "铜", d5: 23.11, d20: 10.62, d60: 63.69, reason: "铜板块大涨，成交活跃。", prices: [18.6, 18.9, 19.4, 20.0, 21.31, 23.44] },
        { name: "中航西飞", code: "000768", pct: 10.01, type: "军工权重龙头", sector: "航天航空", d5: 8.70, d20: -3.42, d60: -15.08, reason: "航天航空与通用航空同步走强。", prices: [20.6, 20.9, 20.8, 20.7, 20.78, 22.86] },
        { name: "中银证券", code: "601696", pct: 10.01, type: "券商情绪龙头", sector: "券商概念", d5: 6.56, d20: -3.27, d60: -13.69, reason: "券商涨停带动风险偏好。", prices: [10.6, 10.7, 10.8, 10.7, 10.49, 11.54] },
        { name: "麦迪科技", code: "603990", pct: 9.98, type: "AI医疗龙头", sector: "AI制药医疗", d5: -2.74, d20: 33.09, d60: 45.67, reason: "AI制药医疗分支活跃。", prices: [22.6, 22.1, 21.6, 20.3, 20.04, 22.04] }
      ]
    },
    us: {
      label: "美股",
      summary: "美股模块已预留结构：可接入 Nasdaq、Polygon、IEX Cloud、Alpha Vantage 等授权数据源。",
      metrics: [
        { name: "QQQ", value: "待接入", pct: 0 },
        { name: "XLK", value: "待接入", pct: 0 },
        { name: "SMH", value: "待接入", pct: 0 },
        { name: "SOXX", value: "待接入", pct: 0 }
      ],
      sectors: [
        { name: "AI基础设施", pct: 0, status: "待接入实时/延迟数据" },
        { name: "半导体", pct: 0, status: "适合用 SMH/SOXX 跟踪" },
        { name: "大型科技", pct: 0, status: "适合用 QQQ/XLK 跟踪" },
        { name: "软件", pct: 0, status: "适合用 IGV 跟踪" }
      ],
      stocks: [
        { name: "NVIDIA", code: "NVDA", pct: 0, type: "AI芯片权重", sector: "半导体", d5: null, d20: null, d60: null, reason: "生产环境接入后显示实时走势。", prices: [201, 205, 209, 204, 200, 203] },
        { name: "Broadcom", code: "AVGO", pct: 0, type: "AI网络/ASIC", sector: "半导体", d5: null, d20: null, d60: null, reason: "适合跟踪 AI 基础设施链。", prices: [360, 366, 372, 368, 372, 377] },
        { name: "Microsoft", code: "MSFT", pct: 0, type: "云与AI平台", sector: "大型科技", d5: null, d20: null, d60: null, reason: "云资本开支与 AI 应用落地核心观察。", prices: [397, 392, 388, 390, 397, 391] }
      ]
    }
  },
  research: [
    { bank: "Goldman Sachs", title: "Insights: Artificial Intelligence / Markets", url: "https://www.goldmansachs.com/insights/", theme: "AI资本开支、市场风险、宏观与行业观点", note: "公开洞察入口，部分正式研报需机构权限。" },
    { bank: "Goldman Sachs", title: "The AI Investment Boom: When Will It Pay Off?", url: "https://www.goldmansachs.com/insights/", theme: "AI投资回报、半导体、数据中心、算力链", note: "官网 Insights 页面列出该播客/文章入口。" },
    { bank: "Morgan Stanley", title: "Insights: Technology & Disruption", url: "https://www.morganstanley.com/insights", theme: "科技颠覆、市场趋势、新兴产业", note: "公开洞察入口，Research Portal/Matrix 通常需要登录。" },
    { bank: "Morgan Stanley", title: "2026 Midyear Outlooks", url: "https://www.morganstanley.com/insights", theme: "中期展望、资产配置、行业线索", note: "官网 Insights 页面可访问公开摘要。" }
  ],
  apiPlan: {
    cn: [
      { name: "Tushare Pro", use: "A股日线、指数、板块、财务、公告，适合每日生成报告。", note: "需要 token，部分高频数据需要积分或权限。" },
      { name: "聚宽 / Ricequant", use: "A股历史、分钟线、因子和回测，适合研究型功能。", note: "通常需要账号授权，云端部署要配置密钥。" },
      { name: "Wind / Choice", use: "专业终端和机构级数据，适合严肃商业场景。", note: "成本较高，但版权和稳定性最好。" }
    ],
    us: [
      { name: "Polygon.io", use: "美股实时/延迟行情、聚合 K 线、公司事件。", note: "适合实时提醒，需付费计划。" },
      { name: "IEX Cloud", use: "美股报价、基本面、新闻。", note: "API 形态友好，按量计费。" },
      { name: "Alpha Vantage", use: "美股日线、技术指标、部分免费额度。", note: "适合 MVP，实时性和频率有限。" },
      { name: "Nasdaq Data Link", use: "宏观、行业、替代数据。", note: "更偏研究数据，不一定适合实时提醒。" }
    ],
    alerts: [
      "盘中每 1-5 分钟拉取自选股报价。",
      "对比用户设置的涨跌幅阈值、成交额、板块热度或研报关键词。",
      "触发后写入 alerts 表，并通过邮件、Telegram、企业微信、Server酱或浏览器 Push 推送。",
      "避免重复轰炸：同一股票同一规则每天只提醒一次，除非再次突破更高阈值。"
    ]
  }
};
