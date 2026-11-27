import { authApi, pantryApi } from "./api.js";

const appShell = document.querySelector("#app-shell");
const elements = {
  list: document.querySelector("#pantry-list"),
  total: document.querySelector("#total-count"),
  low: document.querySelector("#low-count"),
  soon: document.querySelector("#soon-count"),
  visibleCount: document.querySelector("#visible-count"),
  notice: document.querySelector("#notice"),
  search: document.querySelector("#search-input"),
  category: document.querySelector("#category-filter"),
  status: document.querySelector("#status-filter"),
  form: document.querySelector("#item-form"),
  name: document.querySelector("#item-name"),
  itemCategory: document.querySelector("#item-category"),
  quantity: document.querySelector("#item-quantity"),
  unit: document.querySelector("#item-unit"),
  expiry: document.querySelector("#item-expiry"),
  title: document.querySelector("#form-title"),
  eyebrow: document.querySelector("#form-eyebrow"),
  submitLabel: document.querySelector("#submit-label"),
  submitButton: document.querySelector("#submit-button"),
  cancelEdit: document.querySelector("#cancel-edit")
};

const state = { editingId: null, loading: false, filters: {}, actionIds: new Set(), debounce: null };
const categoryIcons = {
  Dairy: "◌", Grains: "▦", Vegetables: "❋", Spices: "✳",
  Beverages: "◉", Snacks: "◇", Other: "✦"
};
const statusNames = {
  AVAILABLE: "In stock", LOW_STOCK: "Low stock", EXPIRED: "Expired", USED_UP: "Used up"
};

document.querySelector("#today-label").textContent = new Intl.DateTimeFormat("en", {
  weekday: "long", month: "short", day: "numeric"
}).format(new Date());

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, character => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[character]);
}

function formatDate(value) {
  if (!value) return "No expiry date";
  return new Intl.DateTimeFormat("en", { month: "short", day: "numeric", year: "numeric" })
    .format(new Date(`${value}T00:00:00`));
}

function dateTone(item) {
  if (!item.expiryDate || item.status === "USED_UP") return "";
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const expiry = new Date(`${item.expiryDate}T00:00:00`);
  if (expiry < today) return "date-expired";
  const soon = new Date(today);
  soon.setDate(soon.getDate() + 3);
  return expiry <= soon ? "date-soon" : "";
}

function statusClass(status) {
  return `status-${status.toLowerCase().replaceAll("_", "-")}`;
}

function showNotice(message, type = "error") {
  elements.notice.textContent = message;
  elements.notice.className = `notice notice-${type}`;
  elements.notice.hidden = false;
}

function clearNotice() {
  elements.notice.hidden = true;
  elements.notice.textContent = "";
}

function handleUnauthorized(error) {
  if (error.status !== 401) return false;
  window.location.replace("./login.html");
  return true;
}

async function initializeApp() {
  try {
    const user = await authApi.me();
    document.querySelector("#account-name").textContent = user.email;
    document.querySelector("#account-avatar").textContent = user.email.slice(0, 1).toUpperCase();
    appShell.hidden = false;
    await refresh();
  } catch (error) {
    if (error.status === 401) {
      window.location.replace("./login.html");
    } else {
      appShell.hidden = false;
      showNotice("Unable to verify your sign-in. Check that the pantry API is running and try again.");
    }
  }
}

function renderSummary(summary) {
  elements.total.textContent = summary.totalItems;
  elements.low.textContent = summary.lowStockItems;
  elements.soon.textContent = summary.expiringSoonItems;
}

function renderItems(items) {
  elements.visibleCount.textContent = `(${items.length})`;
  if (state.loading) {
    elements.list.innerHTML = '<div class="loading-state"><span class="spinner"></span> Gathering your pantry...</div>';
    return;
  }
  if (!items.length) {
    elements.list.innerHTML = `<div class="empty-state">
      <span class="empty-illustration" aria-hidden="true">⌂</span>
      <h3>Your shelves are looking peaceful.</h3>
      <p>No items match this view. Try another filter or add something you love.</p>
      <button class="button button-primary" type="button" data-action="add-first">＋ Add your first item</button>
    </div>`;
    return;
  }

  elements.list.innerHTML = items.map(item => {
    const busy = state.actionIds.has(item.id);
    const icon = categoryIcons[item.category] || categoryIcons.Other;
    return `<article class="pantry-item">
      <div class="item-symbol" aria-hidden="true">${escapeHtml(icon)}</div>
      <div class="item-main">
        <div class="item-title-row">
          <h3>${escapeHtml(item.name)}</h3>
          <span class="status-badge ${statusClass(item.status)}"><i></i>${escapeHtml(statusNames[item.status] || item.status)}</span>
        </div>
        <p class="item-category">${escapeHtml(item.category)}</p>
        <p class="item-expiry ${dateTone(item)}">${item.expiryDate ? `Best before ${escapeHtml(formatDate(item.expiryDate))}` : "No expiry date"}</p>
      </div>
      <div class="item-quantity"><strong>${escapeHtml(item.quantity)}</strong><span>${escapeHtml(item.unit)}</span></div>
      <div class="item-actions">
        <button class="icon-button consume-button" type="button" data-action="consume" data-id="${item.id}" aria-label="Consume ${escapeHtml(item.name)}" title="Consume one unit" ${busy || item.status === "USED_UP" ? "disabled" : ""}>${busy ? '<span class="tiny-spinner"></span>' : "−"}</button>
        <button class="icon-button" type="button" data-action="edit" data-id="${item.id}" aria-label="Edit ${escapeHtml(item.name)}" title="Edit item">↗</button>
        <button class="icon-button delete-button" type="button" data-action="delete" data-id="${item.id}" aria-label="Delete ${escapeHtml(item.name)}" title="Delete item">×</button>
      </div>
    </article>`;
  }).join("");
}

async function refresh(showLoading = true) {
  state.loading = showLoading;
  if (showLoading) renderItems([]);
  clearNotice();
  try {
    const [items, summary] = await Promise.all([
      pantryApi.list(state.filters),
      pantryApi.summary()
    ]);
    state.loading = false;
    renderSummary(summary);
    renderItems(items);
  } catch (error) {
    state.loading = false;
    renderItems([]);
    if (handleUnauthorized(error)) return;
    showNotice(`${error.message} Check that the pantry API is running and try again.`);
  }
}

function setEditMode(item) {
  state.editingId = item.id;
  elements.name.value = item.name;
  elements.itemCategory.value = item.category;
  elements.quantity.value = item.quantity;
  elements.unit.value = item.unit;
  elements.expiry.value = item.expiryDate || "";
  elements.title.textContent = "Edit item";
  elements.eyebrow.textContent = "KEEP THINGS UP TO DATE";
  elements.submitLabel.textContent = "Save changes";
  elements.cancelEdit.hidden = false;
  clearFieldErrors();
  document.querySelector("#form-panel").scrollIntoView({ behavior: "smooth", block: "start" });
  elements.name.focus({ preventScroll: true });
}

function resetForm() {
  state.editingId = null;
  elements.form.reset();
  elements.title.textContent = "Add an item";
  elements.eyebrow.textContent = "MAKE ROOM FOR MORE";
  elements.submitLabel.textContent = "Add to pantry";
  elements.cancelEdit.hidden = true;
  elements.submitButton.disabled = false;
  clearFieldErrors();
}

function clearFieldErrors() {
  document.querySelectorAll(".field-error").forEach(field => { field.textContent = ""; });
  document.querySelectorAll(".field-invalid").forEach(field => field.classList.remove("field-invalid"));
}

function validateForm() {
  clearFieldErrors();
  const values = {
    name: elements.name.value.trim(),
    category: elements.itemCategory.value,
    quantity: Number(elements.quantity.value),
    unit: elements.unit.value,
    expiryDate: elements.expiry.value
  };
  const errors = {};
  if (!values.name) errors.name = "Add a name for this item.";
  if (!values.category) errors.category = "Choose a category.";
  if (!Number.isFinite(values.quantity) || values.quantity <= 0) errors.quantity = "Enter a quantity greater than zero.";
  if (!values.unit) errors.unit = "Choose a unit.";
  if (values.expiryDate && Number.isNaN(new Date(`${values.expiryDate}T00:00:00`).getTime())) {
    errors.expiryDate = "Enter a valid date.";
  }
  Object.entries(errors).forEach(([field, message]) => {
    const input = document.querySelector(`#item-${field === "expiryDate" ? "expiry" : field}`);
    const output = document.querySelector(`#${field}-error`);
    input.classList.add("field-invalid");
    output.textContent = message;
  });
  return Object.keys(errors).length ? null : {
    name: values.name,
    category: values.category,
    quantity: values.quantity,
    unit: values.unit,
    expiryDate: values.expiryDate || null
  };
}

elements.form.addEventListener("submit", async event => {
  event.preventDefault();
  const item = validateForm();
  if (!item) return;
  elements.submitButton.disabled = true;
  elements.submitLabel.textContent = state.editingId ? "Saving..." : "Adding...";
  try {
    if (state.editingId) {
      await pantryApi.update(state.editingId, item);
    } else {
      await pantryApi.create(item);
    }
    const successMessage = state.editingId
      ? "Your pantry item has been updated."
      : "Added to your pantry. Nice one!";
    resetForm();
    await refresh(false);
    showNotice(successMessage, "success");
  } catch (error) {
    elements.submitButton.disabled = false;
    elements.submitLabel.textContent = state.editingId ? "Save changes" : "Add to pantry";
    if (handleUnauthorized(error)) return;
    showNotice(error.message);
  }
});

document.querySelector("#logout-button").addEventListener("click", async () => {
  try {
    await authApi.logout();
    window.location.replace("./login.html");
  } catch (error) {
    if (!handleUnauthorized(error)) showNotice(error.message);
  }
});

elements.cancelEdit.addEventListener("click", resetForm);
document.querySelector("#mobile-add").addEventListener("click", () => {
  document.querySelector("#form-panel").scrollIntoView({ behavior: "smooth", block: "start" });
  elements.name.focus({ preventScroll: true });
});

function updateFilters() {
  clearTimeout(state.debounce);
  state.debounce = setTimeout(() => {
    state.filters = {
      name: elements.search.value.trim(),
      category: elements.category.value,
      status: elements.status.value
    };
    refresh();
  }, 220);
}

elements.search.addEventListener("input", updateFilters);
elements.category.addEventListener("change", updateFilters);
elements.status.addEventListener("change", updateFilters);

document.addEventListener("keydown", event => {
  if (event.key === "/" && !["INPUT", "SELECT", "TEXTAREA"].includes(document.activeElement.tagName)) {
    event.preventDefault();
    elements.search.focus();
  }
  if (event.key === "Escape" && state.editingId) resetForm();
});

elements.list.addEventListener("click", async event => {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  if (button.dataset.action === "add-first") {
    document.querySelector("#form-panel").scrollIntoView({ behavior: "smooth", block: "start" });
    elements.name.focus({ preventScroll: true });
    return;
  }
  const { action, id } = button.dataset;
  const itemId = Number(id);
  if (action === "edit") {
    try {
      setEditMode(await pantryApi.get(itemId));
    } catch (error) {
      if (handleUnauthorized(error)) return;
      showNotice(error.message);
    }
    return;
  }
  if (action === "delete") {
    if (!window.confirm("Remove this item from your pantry?")) return;
    button.disabled = true;
    try {
      await pantryApi.remove(itemId);
      if (state.editingId === itemId) resetForm();
      await refresh(false);
      showNotice("Item removed from your pantry.", "success");
    } catch (error) {
      button.disabled = false;
      if (handleUnauthorized(error)) return;
      showNotice(error.message);
    }
    return;
  }
  if (action === "consume") {
    const item = await pantryApi.get(itemId).catch(error => {
      if (handleUnauthorized(error)) return null;
      showNotice(error.message);
      return null;
    });
    if (!item) return;
    const consumedQuantity = Math.min(1, item.quantity);
    state.actionIds.add(itemId);
    button.disabled = true;
    button.innerHTML = '<span class="tiny-spinner"></span>';
    try {
      await pantryApi.consume(itemId, consumedQuantity);
      await refresh(false);
      showNotice(`${consumedQuantity} ${item.unit} of ${item.name} marked as used.`, "success");
    } catch (error) {
      state.actionIds.delete(itemId);
      if (handleUnauthorized(error)) return;
      showNotice(error.message);
      await refresh(false);
    } finally {
      state.actionIds.delete(itemId);
    }
  }
});

initializeApp();
