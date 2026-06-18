(function () {
  const DEFAULT_BASE_URL = "http://localhost:8080/api";
  const baseUrl = (window.MARKET_LENS_API_BASE || DEFAULT_BASE_URL).replace(/\/+$/, "");
  const UNAVAILABLE_MESSAGE = "Backend service is temporarily unavailable";

  async function request(path, options = {}) {
    let response;
    try {
      response = await fetch(`${baseUrl}${path}`, {
        cache: "no-store",
        ...options,
        headers: {
          Accept: "application/json",
          ...(options.body ? { "Content-Type": "application/json" } : {}),
          ...(options.headers || {}),
        },
      });
    } catch (error) {
      throw new Error(UNAVAILABLE_MESSAGE);
    }

    if (!response.ok) {
      throw new Error(UNAVAILABLE_MESSAGE);
    }

    if (response.status === 204) {
      return null;
    }

    const payload = await response.json();
    if (payload && typeof payload.code === "number" && payload.code !== 0) {
      throw new Error(payload.message || UNAVAILABLE_MESSAGE);
    }
    return payload;
  }

  function normalizeCollection(value) {
    if (Array.isArray(value)) return value;
    if (Array.isArray(value?.data)) return value.data;
    if (Array.isArray(value?.items)) return value.items;
    return [];
  }

  function sameStock(left, right) {
    return String(left?.code || left?.stockCode || "").toUpperCase() === String(right?.code || right?.stockCode || "").toUpperCase();
  }

  function marketCode(value) {
    const text = String(value || "").toUpperCase();
    if (text.includes("US")) return "US";
    if (text.includes("HK")) return "HK";
    if (text.includes("ETF")) return "ETF";
    return "CN";
  }

  function normalizeMemo(item) {
    return {
      id: item.id,
      code: item.code || item.stockCode,
      name: item.name || item.title || item.stockCode,
      note: item.note || item.content || "",
      createdAt: item.createdAt,
      updatedAt: item.updatedAt,
    };
  }

  function normalizeAlertRule(item) {
    return {
      id: item.id,
      code: item.code || item.stockCode,
      name: item.name || item.stockCode,
      metric: item.metric || item.ruleType || "PCT_CHANGE",
      operator: item.operator || ">=",
      threshold: Number(item.threshold ?? item.thresholdValue),
      enabled: item.enabled !== false,
      oncePerDay: item.oncePerDay !== false,
      createdAt: item.createdAt,
      updatedAt: item.updatedAt,
    };
  }

  function toWatchlistRequest(item) {
    return {
      symbol: item.code,
      name: item.name || item.code,
      market: marketCode(item.market),
    };
  }

  function toMemoRequest(item) {
    const symbol = item.code || item.stockCode;
    const note = item.note || item.content || "No note yet.";
    return {
      symbol,
      stockCode: symbol,
      title: item.name || item.title || symbol,
      note,
      content: note,
    };
  }

  function toAlertRuleRequest(item) {
    const symbol = item.code || item.stockCode;
    const threshold = Math.max(0.01, Math.abs(Number(item.threshold) || 0.01));
    return {
      symbol,
      stockCode: symbol,
      ruleType: "PCT_CHANGE",
      operator: ">=",
      threshold,
      thresholdValue: threshold,
      enabled: item.enabled !== false,
      oncePerDay: item.oncePerDay !== false,
    };
  }

  function getCollection(path) {
    return request(path).then(normalizeCollection);
  }

  async function reconcileCollection(path, items, toRequest) {
    const nextItems = Array.isArray(items) ? items : [];
    const currentItems = await getCollection(path);
    const keepIds = new Set(nextItems.map((item) => item.id).filter(Boolean));

    await Promise.all(currentItems
      .filter((current) => current.id && !keepIds.has(current.id) && !nextItems.some((item) => !item.id && sameStock(item, current)))
      .map((current) => request(`${path}/${current.id}`, { method: "DELETE" })));

    await Promise.all(nextItems.map((item) => {
      const existing = item.id ? item : currentItems.find((current) => sameStock(item, current));
      const method = existing?.id ? "PUT" : "POST";
      const targetPath = existing?.id ? `${path}/${existing.id}` : path;
      return request(targetPath, {
        method,
        body: JSON.stringify(toRequest(item)),
      });
    }));

    return getCollection(path);
  }

  async function reconcileWatchlist(items) {
    const nextItems = Array.isArray(items) ? items : [];
    const currentItems = await getCollection("/watchlist");
    const keepIds = new Set(nextItems.map((item) => item.id).filter(Boolean));

    await Promise.all(currentItems
      .filter((current) => current.id && !keepIds.has(current.id) && !nextItems.some((item) => sameStock(item, current)))
      .map((current) => request(`/watchlist/${current.id}`, { method: "DELETE" })));

    await Promise.all(nextItems
      .filter((item) => !item.id && !currentItems.some((current) => sameStock(item, current)))
      .map((item) => request("/watchlist", {
        method: "POST",
        body: JSON.stringify(toWatchlistRequest(item)),
      })));

    return getCollection("/watchlist");
  }

  window.MarketLensApi = {
    unavailableMessage: UNAVAILABLE_MESSAGE,
    getWatchlist: () => getCollection("/watchlist"),
    saveWatchlist: reconcileWatchlist,
    getMemos: () => getCollection("/notes").then((items) => items.map(normalizeMemo)),
    saveMemos: (items) => reconcileCollection("/notes", items, toMemoRequest).then((saved) => saved.map(normalizeMemo)),
    getAlertRules: () => getCollection("/alerts/rules").then((items) => items.map(normalizeAlertRule)),
    saveAlertRules: (items) => reconcileCollection("/alerts/rules", items, toAlertRuleRequest).then((saved) => saved.map(normalizeAlertRule)),
    getDailyReview: () => request("/market/daily-review").then((payload) => payload?.data ?? payload ?? null),
  };
})();
