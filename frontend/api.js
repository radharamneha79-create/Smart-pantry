const API_BASE_URL = (window.PANTRY_API_BASE_URL || "http://localhost:8080/api/v1").replace(/\/+$/, "");
const ITEMS_URL = `${API_BASE_URL}/pantry-items`;
const AUTH_URL = `${API_BASE_URL}/auth`;
let csrfToken = null;

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function request(url, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  if (!["GET", "HEAD", "OPTIONS"].includes(method) && !url.endsWith("/csrf") && !csrfToken) {
    await loadCsrfToken();
  }
  const headers = {
    Accept: "application/json",
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...options.headers
  };
  if (!["GET", "HEAD", "OPTIONS"].includes(method) && csrfToken) {
    headers[csrfToken.headerName] = csrfToken.token;
  }

  const response = await fetch(url, {
    ...options,
    credentials: "include",
    headers
  });

  if (response.status === 204) {
    return null;
  }

  const body = await response.json().catch(() => null);
  if (!response.ok) {
    const fieldMessages = body?.fieldErrors
      ? Object.values(body.fieldErrors).join(" ")
      : "";
    throw new ApiError([body?.message || `Request failed (${response.status}).`, fieldMessages]
      .filter(Boolean).join(" "), response.status);
  }
  return body;
}

async function loadCsrfToken() {
  csrfToken = await request(`${AUTH_URL}/csrf`);
  return csrfToken;
}

export const authApi = {
  csrf: loadCsrfToken,
  register(credentials) {
    return request(`${AUTH_URL}/register`, {
      method: "POST",
      body: JSON.stringify(credentials)
    });
  },
  login(credentials) {
    return request(`${AUTH_URL}/login`, {
      method: "POST",
      body: JSON.stringify(credentials)
    });
  },
  me() {
    return request(`${AUTH_URL}/me`);
  },
  logout() {
    return request(`${AUTH_URL}/logout`, { method: "POST" });
  }
};

export const pantryApi = {
  list(filters = {}) {
    const query = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        query.set(key, value);
      }
    });
    const suffix = query.size ? `?${query.toString()}` : "";
    return request(`${ITEMS_URL}${suffix}`);
  },
  summary() {
    return request(`${ITEMS_URL}/summary`);
  },
  get(id) {
    return request(`${ITEMS_URL}/${encodeURIComponent(id)}`);
  },
  create(item) {
    return request(ITEMS_URL, { method: "POST", body: JSON.stringify(item) });
  },
  update(id, item) {
    return request(`${ITEMS_URL}/${encodeURIComponent(id)}`, {
      method: "PUT",
      body: JSON.stringify(item)
    });
  },
  consume(id, quantity) {
    return request(`${ITEMS_URL}/${encodeURIComponent(id)}/consume`, {
      method: "PATCH",
      body: JSON.stringify({ quantity })
    });
  },
  remove(id) {
    return request(`${ITEMS_URL}/${encodeURIComponent(id)}`, { method: "DELETE" });
  }
};
