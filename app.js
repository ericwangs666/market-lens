const data = window.MARKET_DATA;
let currentMarket = "cn";
let currentFilter = "all";

const marketView = document.getElementById("marketView");
const historyView = document.getElementById("historyView");
const watchlistView = document.getElementById("watchlistView");
const researchView = document.getElementById("researchView");
const deployView = document.getElementById("deployView");
const historyList = document.getElementById("historyList");
const historyDetail = document.getElementById("historyDetail");
const searchInput = document.getElementById("searchInput");
const memoForm = document.getElementById("memoForm");
const memoList = document.getElementById("memoList");
const alertList = document.getElementById("alertList");
const manualUpdateButton = document.getElementById("manualUpdateButton");
const clearUpdateTokenButton = document.getElementById("clearUpdateTokenButton");
const manualUpdateStatus = document.getElementById("manualUpdateStatus");
const providerStatus = document.getElementById("providerStatus");
const MEMO_KEY = "market-lens-memos";
const UPDATE_TOKEN_KEY = "market-lens-github-token";
const WORKFLOW_DISPATCH_URL = "https://api.github.com/repos/ericwangs666/market-lens/actions/workflows/daily-data.yml/dispatches";
let historyIndex = null;
let selectedHistoryDate = null;

document.getElementById("runTime").textContent = data.lastRun;
renderProviderStatus();

manualUpdateButton?.addEventListener("click", triggerManualUpdate);
clearUpdateTokenButton?.addEventListener("click", () => {
  localStorage.removeItem(UPDATE_TOKEN_KEY);
  setManualUpdateStatus("已清除本机授权，下次更新会重新询问 token。");
});

document.querySelectorAll(".tab").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    currentMarket = button.dataset.market;
    render();
  });
});

document.querySelectorAll("[data-filter]").forEach((button) => {
  button.addEventListener("click", () => {
    currentFilter = button.dataset.filter;
    document.querySelectorAll("[data-filter]").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    renderMarket();
  });
});

searchInput.addEventListener("input", () => renderMarket());

function fmtPct(value) {
  if (value === null || value === undefined) return "--";
  return `${value > 0 ? "+" : ""}${value.toFixed(2)}%`;
}

function setManualUpdateStatus(message, isError = false) {
  if (!manualUpdateStatus) return;
  manualUpdateStatus.textContent = message;
  manualUpdateStatus.classList.toggle("error", isError);
}

function renderProviderStatus() {
  if (!providerStatus) return;
  const status = data.providerStatus || {};
  const messages = [];
  const cnStatus = String(status.cn || "");
  const usStatus = String(status.us || "");
  const sectorStatus = String(status.cnSectors || "");
  messages.push(cnStatus.includes("seed") || cnStatus.includes("no usable") ? "A股：备用数据" : "A股：已更新");
  messages.push(
    sectorStatus.includes("KPL")
      ? "板块：开盘啦"
      : sectorStatus.includes("Public")
        ? "板块：公开排行"
        : "板块：未获取"
  );
  messages.push(usStatus.includes("missing") || usStatus.includes("seed") ? "美股：备用数据" : "美股：已更新");
  providerStatus.innerHTML = messages.map((message) => `<span>${message}</span>`).join("");
}

async function triggerManualUpdate() {
  let token = localStorage.getItem(UPDATE_TOKEN_KEY);
  if (!token) {
    token = window.prompt("粘贴 GitHub token（需要 Actions 写入权限，只保存在当前浏览器）：");
    if (!token) {
      setManualUpdateStatus("已取消更新。");
      return;
    }
    localStorage.setItem(UPDATE_TOKEN_KEY, token.trim());
  }

  manualUpdateButton.disabled = true;
  setManualUpdateStatus("正在提交更新任务...");
  try {
    const response = await fetch(WORKFLOW_DISPATCH_URL, {
      method: "POST",
      headers: {
        "Accept": "application/vnd.github+json",
        "Authorization": `Bearer ${token.trim()}`,
        "Content-Type": "application/json",
        "X-GitHub-Api-Version": "2022-11-28",
      },
      body: JSON.stringify({ ref: "main" }),
    });
    if (response.status === 204) {
      setManualUpdateStatus("更新任务已提交，约 1 分钟后刷新页面查看。");
      return;
    }
    if (response.status === 401 || response.status === 403) {
      localStorage.removeItem(UPDATE_TOKEN_KEY);
      setManualUpdateStatus("授权失败，请重新填写有 Actions 写入权限的 GitHub token。", true);
      return;
    }
    setManualUpdateStatus(`提交失败：GitHub 返回 ${response.status}。`, true);
  } catch (error) {
    setManualUpdateStatus("提交失败：网络连接异常。", true);
  } finally {
    manualUpdateButton.disabled = false;
  }
}

function sectorStocks(market, sector) {
  if (Array.isArray(sector.stocks) && sector.stocks.length) {
    return sector.stocks;
  }
  const name = String(sector.name || "").toLowerCase();
  return market.stocks
    .filter((stock) => {
      const sectorName = String(stock.sector || "").toLowerCase();
      const text = `${stock.name || ""} ${stock.code || ""} ${stock.sector || ""} ${stock.type || ""} ${stock.reason || ""}`.toLowerCase();
      return sectorName === name || sectorName.includes(name) || name.includes(sectorName) || text.includes(name);
    })
    .sort((a, b) => (b.pct ?? -999) - (a.pct ?? -999));
}

function sectorCard(market, sector) {
  const stocks = sectorStocks(market, sector).slice(0, 4);
  return `
    <article class="sector-card">
      <div class="bar-row">
        <strong title="${sector.name}">${sector.name}</strong>
        <div class="track"><div class="fill" style="width:${Math.max(6, Math.min(100, Math.abs(sector.pct) * 10))}%"></div></div>
        <span class="${sector.pct >= 0 ? "up" : "down"}">${fmtPct(sector.pct)}</span>
      </div>
      <div class="sector-stocks">
        ${stocks.map((stock) => `
          <div class="sector-stock">
            <span>
              <strong title="${stock.name}">${stock.name}</strong>
              <small>${stock.code}${stock.type ? ` · ${stock.type}` : ""}</small>
            </span>
            <em class="${stock.pct >= 0 ? "up" : "down"}">${fmtPct(stock.pct)}</em>
          </div>
        `).join("") || "<div class=\"sector-empty\">暂无匹配个股</div>"}
      </div>
    </article>
  `;
}

function render() {
  marketView.classList.toggle("hidden", currentMarket === "research" || currentMarket === "deploy" || currentMarket === "watchlist" || currentMarket === "history");
  historyView.classList.toggle("hidden", currentMarket !== "history");
  watchlistView.classList.toggle("hidden", currentMarket !== "watchlist");
  researchView.classList.toggle("hidden", currentMarket !== "research");
  deployView.classList.toggle("hidden", currentMarket !== "deploy");

  if (currentMarket === "history") renderHistory();
  if (currentMarket === "research") renderResearch();
  if (currentMarket === "watchlist") renderWatchlist();
  if (currentMarket === "cn" || currentMarket === "us") renderMarket();
}

function renderMarket() {
  const market = data.markets[currentMarket];
  if (!market) return;

  const keyword = searchInput.value.trim().toLowerCase();
  const stocks = market.stocks.filter((stock) => {
    const text = `${stock.name} ${stock.code} ${stock.sector} ${stock.type}`.toLowerCase();
    if (keyword && !text.includes(keyword)) return false;
    if (currentFilter === "leaders" && !stock.type.includes("龙头") && !stock.type.includes("权重")) return false;
    if (currentFilter === "hot" && !["钼", "铜", "半导体", "券商概念", "AI基础设施"].some((tag) => stock.sector.includes(tag))) return false;
    return true;
  });

  marketView.innerHTML = `
    <section class="panel">
      <h2>${market.label}概览</h2>
      <p>${market.summary}</p>
      <div class="metric-grid">
        ${market.metrics.map((item) => `
          <div class="metric">
            <span>${item.name}</span>
            <strong>${item.value}</strong>
            <em class="${item.pct >= 0 ? "up" : "down"}">${fmtPct(item.pct)}</em>
          </div>
        `).join("")}
      </div>
      <h2 style="margin-top:22px;">热门板块</h2>
      <div class="bars">
        ${market.sectors.length ? market.sectors.map((item) => sectorCard(market, item)).join("") : "<div class=\"sector-empty sector-unavailable\">未获取到全市场板块强度，暂不展示热门板块。</div>"}
      </div>
    </section>
    <section class="panel">
      <h2>个股近期走势</h2>
      <div class="stock-grid">
        ${stocks.map((stock, index) => stockCard(stock, index)).join("") || "<p>没有匹配的个股。</p>"}
      </div>
    </section>
  `;

  requestAnimationFrame(() => {
    stocks.forEach((stock, index) => drawSparkline(`chart-${index}`, stock.prices));
  });
}

function stockCard(stock, index) {
  return `
    <article class="stock-card">
      <header>
        <div>
          <h3>${stock.name}</h3>
          <small>${stock.code} · ${stock.sector}</small>
        </div>
        <div class="pct ${stock.pct >= 0 ? "up" : "down"}">${fmtPct(stock.pct)}</div>
      </header>
      <span class="tag">${stock.type}</span>
      <canvas id="chart-${index}" width="420" height="90" aria-label="${stock.name}走势"></canvas>
      <p>${stock.reason}</p>
      <div class="trend-row">
        <span>5日 ${fmtPct(stock.d5)}</span>
        <span>20日 ${fmtPct(stock.d20)}</span>
        <span>60日 ${fmtPct(stock.d60)}</span>
      </div>
    </article>
  `;
}

function drawSparkline(id, values) {
  const canvas = document.getElementById(id);
  if (!canvas || !values || values.length < 2) return;
  const ctx = canvas.getContext("2d");
  const width = canvas.width;
  const height = canvas.height;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  ctx.clearRect(0, 0, width, height);
  ctx.lineWidth = 4;
  ctx.strokeStyle = values.at(-1) >= values[0] ? "#c63d32" : "#18735d";
  ctx.beginPath();
  values.forEach((value, index) => {
    const x = (index / (values.length - 1)) * (width - 24) + 12;
    const y = height - 14 - ((value - min) / range) * (height - 28);
    if (index === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });
  ctx.stroke();
}

async function loadHistoryIndex() {
  if (historyIndex) return historyIndex;
  try {
    const response = await fetch("./daily/index.json", { cache: "no-store" });
    if (!response.ok) throw new Error("history index unavailable");
    historyIndex = await response.json();
  } catch {
    historyIndex = data.historyReports || [{
      date: data.generatedDate || data.lastRun.slice(0, 10),
      file: "latest.json",
      lastRun: data.lastRun
    }];
  }
  if (!Array.isArray(historyIndex)) historyIndex = [historyIndex];
  return historyIndex;
}

async function loadHistoryReport(item) {
  if (!item || item.file === "latest.json") return data;
  try {
    const response = await fetch(`./daily/${item.file}`, { cache: "no-store" });
    if (!response.ok) throw new Error("history report unavailable");
    return await response.json();
  } catch {
    return null;
  }
}

async function renderHistory() {
  historyList.innerHTML = `<div class="empty-state">正在读取历史报告...</div>`;
  historyDetail.innerHTML = "";
  const reports = await loadHistoryIndex();
  if (!reports.length) {
    historyList.innerHTML = `<div class="empty-state">还没有历史报告。每日生成后会自动出现在这里。</div>`;
    return;
  }

  if (!selectedHistoryDate) selectedHistoryDate = reports[0].date;
  historyList.innerHTML = reports.map((item) => `
    <button class="history-date ${item.date === selectedHistoryDate ? "active" : ""}" data-history-date="${item.date}">
      <strong>${item.date}</strong>
      <span>${item.lastRun || item.file}</span>
    </button>
  `).join("");

  document.querySelectorAll("[data-history-date]").forEach((button) => {
    button.addEventListener("click", () => {
      selectedHistoryDate = button.dataset.historyDate;
      renderHistory();
    });
  });

  const selected = reports.find((item) => item.date === selectedHistoryDate) || reports[0];
  const report = await loadHistoryReport(selected);
  if (!report) {
    historyDetail.innerHTML = `<div class="empty-state">这一天的报告暂时读取失败。</div>`;
    return;
  }
  historyDetail.innerHTML = `
    <header class="history-header">
      <div>
        <h2>${selected.date} 历史报告</h2>
        <p>${report.lastRun || "历史数据"}</p>
      </div>
      <span>${selected.date === (data.generatedDate || "") ? "当前最新" : "历史归档"}</span>
    </header>
    <div class="history-market-grid">
      ${["cn", "us"].map((marketName) => historyMarketPanel(report, marketName)).join("")}
    </div>
    <section class="compare-panel">
      <h2>与当前最新对比</h2>
      <div class="compare-grid">
        ${["cn", "us"].map((marketName) => compareMarket(report, marketName)).join("")}
      </div>
    </section>
  `;
}

function historyMarketPanel(report, marketName) {
  const market = report.markets?.[marketName];
  if (!market) return "";
  const topSectors = [...market.sectors].sort((a, b) => b.pct - a.pct).slice(0, 4);
  const topStocks = [...market.stocks].sort((a, b) => b.pct - a.pct).slice(0, 4);
  return `
    <section class="history-market">
      <h3>${market.label}</h3>
      <p>${market.summary}</p>
      <h4>热门板块</h4>
      ${topSectors.map((item) => `
        <div class="history-row">
          <span>${item.name}</span>
          <strong class="${item.pct >= 0 ? "up" : "down"}">${fmtPct(item.pct)}</strong>
        </div>
      `).join("")}
      <h4>龙头个股</h4>
      ${topStocks.map((item) => `
        <div class="history-row">
          <span>${item.name} <small>${item.code}</small></span>
          <strong class="${item.pct >= 0 ? "up" : "down"}">${fmtPct(item.pct)}</strong>
        </div>
      `).join("")}
    </section>
  `;
}

function compareMarket(report, marketName) {
  const historical = report.markets?.[marketName];
  const latest = data.markets?.[marketName];
  if (!historical || !latest) return "";
  const historicalSector = [...historical.sectors].sort((a, b) => b.pct - a.pct)[0];
  const latestSector = [...latest.sectors].sort((a, b) => b.pct - a.pct)[0];
  const historicalStock = [...historical.stocks].sort((a, b) => b.pct - a.pct)[0];
  const latestStock = [...latest.stocks].sort((a, b) => b.pct - a.pct)[0];
  return `
    <article class="compare-card">
      <h3>${latest.label}</h3>
      <div>
        <span>热门板块</span>
        <p>${historicalSector?.name || "--"} ${historicalSector ? fmtPct(historicalSector.pct) : ""}</p>
        <strong>当前：${latestSector?.name || "--"} ${latestSector ? fmtPct(latestSector.pct) : ""}</strong>
      </div>
      <div>
        <span>龙头个股</span>
        <p>${historicalStock?.name || "--"} ${historicalStock ? fmtPct(historicalStock.pct) : ""}</p>
        <strong>当前：${latestStock?.name || "--"} ${latestStock ? fmtPct(latestStock.pct) : ""}</strong>
      </div>
    </article>
  `;
}

function renderResearch() {
  researchView.innerHTML = `
    <section class="panel">
      <h2>机构公开观点入口</h2>
      <p>高盛、大摩的完整研究报告多数需要机构账号；这里先接入公开 Insights 页面和可公开查看的专题入口。</p>
      ${data.research.map((item) => `
        <article class="research-card">
          <a href="${item.url}" target="_blank" rel="noreferrer">${item.bank} · ${item.title}</a>
          <p><strong>${item.theme}</strong></p>
          <p>${item.note}</p>
        </article>
      `).join("")}
    </section>
    <section class="panel">
      <h2>新兴产业看板</h2>
      <p>建议纳入：AI基础设施、半导体材料、先进封装、数据中心电力、机器人、低空经济、创新药、商业航天、铜/金属资源。</p>
      <p>下一步可以做成“机构观点热度”：每天抓取公开标题和摘要，按产业标签归类，再和当天板块表现交叉验证。</p>
    </section>
  `;
}

function allStocks() {
  return Object.values(data.markets).flatMap((market) => market.stocks.map((stock) => ({
    ...stock,
    market: market.label
  })));
}

function loadMemos() {
  try {
    return JSON.parse(localStorage.getItem(MEMO_KEY) || "[]");
  } catch {
    return [];
  }
}

function saveMemos(memos) {
  localStorage.setItem(MEMO_KEY, JSON.stringify(memos));
}

function findQuote(code) {
  const normalized = code.trim().toLowerCase();
  return allStocks().find((stock) => stock.code.toLowerCase() === normalized || stock.name.toLowerCase() === normalized);
}

function renderWatchlist() {
  const memos = loadMemos();
  const triggered = memos.map((memo) => ({ memo, quote: findQuote(memo.code) }))
    .filter((item) => item.quote && item.quote.pct >= item.memo.threshold);

  alertList.innerHTML = triggered.length ? triggered.map(({ memo, quote }) => `
    <article class="alert-card">
      <header>
        <h3>${memo.name} <small>${memo.code}</small></h3>
        <strong class="up">${fmtPct(quote.pct)}</strong>
      </header>
      <p>已超过你设置的 ${fmtPct(memo.threshold)} 提醒阈值。</p>
      <p>${memo.note || "暂无备忘录。"}</p>
    </article>
  `).join("") : `<div class="empty-state">当前没有触发提醒。添加备忘录后，页面会按最新行情数据判断是否触发。</div>`;

  memoList.innerHTML = memos.length ? memos.map((memo, index) => {
    const quote = findQuote(memo.code);
    return `
      <article class="memo-card">
        <header>
          <div>
            <h3>${memo.name} <small>${memo.code}</small></h3>
            <p>${memo.note || "暂无备忘录。"}</p>
          </div>
          <button data-delete-memo="${index}">删除</button>
        </header>
        <div class="trend-row">
          <span>阈值 ${fmtPct(memo.threshold)}</span>
          <span>今日 ${quote ? fmtPct(quote.pct) : "无行情"}</span>
          <span>${quote ? quote.market : "待接入"}</span>
        </div>
      </article>
    `;
  }).join("") : `<div class="empty-state">还没有备忘录。可以先添加一只股票和提醒阈值。</div>`;

  document.querySelectorAll("[data-delete-memo]").forEach((button) => {
    button.addEventListener("click", () => {
      const next = loadMemos();
      next.splice(Number(button.dataset.deleteMemo), 1);
      saveMemos(next);
      renderWatchlist();
    });
  });
}

memoForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const memo = {
    code: document.getElementById("memoCode").value.trim(),
    name: document.getElementById("memoName").value.trim(),
    threshold: Number(document.getElementById("memoThreshold").value),
    note: document.getElementById("memoNote").value.trim(),
    createdAt: new Date().toISOString()
  };
  const memos = loadMemos();
  memos.unshift(memo);
  saveMemos(memos);
  memoForm.reset();
  document.getElementById("memoThreshold").value = "5";
  renderWatchlist();
});

render();
