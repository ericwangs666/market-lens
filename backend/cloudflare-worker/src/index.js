const DEFAULT_HEADERS = {
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Content-Type": "application/json; charset=utf-8",
};

function corsHeaders(request, env) {
  const origin = request.headers.get("Origin") || "";
  const allowedOrigin = env.ALLOWED_ORIGIN || "*";
  const allowOrigin = allowedOrigin === "*" || origin === allowedOrigin
    ? (origin || allowedOrigin)
    : allowedOrigin;
  return {
    ...DEFAULT_HEADERS,
    "Access-Control-Allow-Origin": allowOrigin,
  };
}

function jsonResponse(request, env, body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: corsHeaders(request, env),
  });
}

async function dispatchWorkflow(env) {
  if (!env.GITHUB_TOKEN) {
    return {
      ok: false,
      status: 500,
      message: "Missing GITHUB_TOKEN secret",
    };
  }

  const owner = env.GITHUB_OWNER || "ericwangs666";
  const repo = env.GITHUB_REPO || "market-lens";
  const workflow = env.GITHUB_WORKFLOW || "daily-data.yml";
  const ref = env.GITHUB_REF || "main";
  const url = `https://api.github.com/repos/${owner}/${repo}/actions/workflows/${workflow}/dispatches`;

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Accept": "application/vnd.github+json",
      "Authorization": `Bearer ${env.GITHUB_TOKEN}`,
      "Content-Type": "application/json",
      "User-Agent": "market-lens-update-worker",
      "X-GitHub-Api-Version": "2022-11-28",
    },
    body: JSON.stringify({ ref }),
  });

  if (response.status === 204) {
    return {
      ok: true,
      status: 202,
      message: "Workflow dispatch accepted",
    };
  }

  const details = await response.text();
  return {
    ok: false,
    status: response.status,
    message: "GitHub workflow dispatch failed",
    details,
  };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: corsHeaders(request, env),
      });
    }

    if (url.pathname === "/api/health" && request.method === "GET") {
      return jsonResponse(request, env, {
        ok: true,
        service: "market-lens-update-worker",
      });
    }

    if (url.pathname === "/api/update" && request.method === "POST") {
      const result = await dispatchWorkflow(env);
      return jsonResponse(request, env, result, result.ok ? 202 : result.status || 500);
    }

    return jsonResponse(request, env, {
      ok: false,
      message: "Not found",
    }, 404);
  },
};
