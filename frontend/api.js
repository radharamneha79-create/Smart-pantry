const API_BASE_URL = (window.PANTRY_API_BASE_URL || "http://localhost:8080/api/v1").replace(/\/+$/, "");
const ITEMS_URL = `${API_BASE_URL}/pantry-items`;

async function request(path, options = {}) {
  const response = await fetch(`${ITEMS_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers
    }
  });

  if (response.status === 204) {
    return null;
  }

  const body = await response.json().catch(() => null);
  if (!response.ok) {
    const fieldMessages = body?.fieldErrors
      ? Object.values(body.fieldErrors).join(" ")
      : "";
    throw new Error([body?.message || `Request failed (${response.status}).`, fieldMessages]
      .filter(Boolean).join(" "));
  }
  return body;
}

export const pantryApi = {
  list(filters = {}) {
    const query = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        query.set(key, value);
      }
    });
    const suffix = query.size ? `?${query.toString()}` : "";
    return request(suffix);
  },
  summary() {
    return request("/summary");
  },
  get(id) {
    return request(`/${encodeURIComponent(id)}`);
  },
  create(item) {
    return request("", { method: "POST", body: JSON.stringify(item) });
  },
  update(id, item) {
    return request(`/${encodeURIComponent(id)}`, { method: "PUT", body: JSON.stringify(item) });
  },
  consume(id, quantity) {
    return request(`/${encodeURIComponent(id)}/consume`, {
      method: "PATCH",
      body: JSON.stringify({ quantity })
    });
  },
  remove(id) {
    return request(`/${encodeURIComponent(id)}`, { method: "DELETE" });
  }
};
