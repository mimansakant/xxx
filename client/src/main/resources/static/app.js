const tokenKey = "harbor.session";
let sessionToken = sessionStorage.getItem(tokenKey) || "";
let username = "";
let authMode = "login";
let accounts = [];
let budgets = [];
let currentBudgetId = "";
let cachedReports = new Map();

const byId = (id) => document.getElementById(id);
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
})[char]);
const money = (value) => new Intl.NumberFormat(undefined, {
  style: "currency", currency: "USD"
}).format(Number(value || 0));

async function request(path, options = {}) {
  const headers = { Accept: "application/json", ...(options.headers || {}) };
  if (sessionToken) headers.Authorization = `Bearer ${sessionToken}`;
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(path, { ...options, headers });
  if (response.status === 401 && sessionToken) {
    sessionStorage.removeItem(tokenKey);
    sessionToken = "";
    showAuth();
  }
  if (response.status === 204) return null;
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.reason || body?.message || `Request failed (${response.status})`);
  }
  return body;
}

function showNotice(message, type = "error", auth = false) {
  const element = byId(auth ? "authNotice" : "appNotice");
  element.textContent = message;
  element.className = `notice ${type}`;
  element.hidden = false;
}

function clearNotice(auth = false) {
  const element = byId(auth ? "authNotice" : "appNotice");
  element.hidden = true;
  element.textContent = "";
}

function showAuth() {
  byId("authView").hidden = false;
  byId("appView").hidden = true;
  clearNotice();
}

function showApp() {
  byId("authView").hidden = true;
  byId("appView").hidden = false;
  byId("usernameLabel").textContent = username;
}

function setAuthMode(mode) {
  authMode = mode;
  const signup = mode === "signup";
  byId("authTitle").textContent = signup ? "Create your account" : "Welcome back";
  byId("authDescription").textContent = signup
    ? "Start with a private space to organize your money."
    : "Sign in to pick up where you left off.";
  byId("authSubmit").textContent = signup ? "Create account" : "Sign in";
  byId("authPassword").autocomplete = signup ? "new-password" : "current-password";
  byId("loginTab").classList.toggle("active", !signup);
  byId("signupTab").classList.toggle("active", signup);
  clearNotice(true);
}

function navigate(page) {
  const pages = ["overview", "accounts", "budgets", "purchases"];
  pages.forEach((name) => {
    byId(`${name}Page`).hidden = name !== page;
    document.querySelectorAll(`[data-nav="${name}"]`).forEach((item) => {
      item.classList.toggle("active", name === page && item.classList.contains("nav-link"));
    });
  });
  byId("pageTitle").textContent = page[0].toUpperCase() + page.slice(1);
  clearNotice();
  if (page === "overview") loadOverview().catch(reportError);
  if (page === "purchases") loadHistory().catch(reportError);
  if (page === "budgets") renderBudgets();
}

function dateQuery(from, to, accountId) {
  const params = new URLSearchParams();
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  if (accountId) params.set("accountId", accountId);
  return params.toString();
}

function accountOptions(select, includeAll, selected = "") {
  const first = includeAll
    ? '<option value="">All accounts</option>'
    : '<option value="">Personal budget</option>';
  select.innerHTML = first + accounts.map((account) =>
    `<option value="${escapeHtml(account.id)}">${escapeHtml(account.name)} · ${escapeHtml(account.type)}</option>`
  ).join("");
  select.value = selected;
}

function budgetOptions(select, includeAll, selected = "") {
  const first = includeAll ? '<option value="">All budgets</option>' : "";
  select.innerHTML = first + budgets.map((budget) =>
    `<option value="${escapeHtml(budget.budgetId)}">${escapeHtml(budget.budgetName)}${budget.accountName ? ` · ${escapeHtml(budget.accountName)}` : ""}</option>`
  ).join("");
  if (selected && budgets.some((budget) => budget.budgetId === selected)) {
    select.value = selected;
  } else if (budgets.length && !includeAll) {
    select.value = budgets[0].budgetId;
  } else {
    select.value = "";
  }
}

async function loadAccounts() {
  accounts = await request("/api/accounts");
  const currentOverviewAccount = byId("overviewAccount").value;
  const currentHistoryAccount = byId("historyAccount").value;
  accountOptions(byId("overviewAccount"), true, currentOverviewAccount);
  accountOptions(byId("historyAccount"), true, currentHistoryAccount);
  accountOptions(byId("budgetsAccount"), true, byId("budgetsAccount").value);
  accountOptions(byId("budgetAccount"), false, byId("budgetAccount").value);
  renderAccounts();
}

async function loadBudgets(selectedId = "") {
  cachedReports.clear();
  budgets = await request("/api/budgets");
  currentBudgetId = selectedId || currentBudgetId;
  budgetOptions(byId("overviewBudget"), true, currentBudgetId);
  budgetOptions(byId("historyBudget"), true);
  budgetOptions(byId("purchaseBudget"), false, currentBudgetId);
  await loadBudgetCards();
  await loadPurchaseCategoryOptions();
}

function renderAccounts() {
  const host = byId("accountsList");
  if (!accounts.length) {
    host.className = "card-list empty-state";
    host.textContent = "No accounts yet. Add one to connect it to a budget.";
    return;
  }
  host.className = "card-list";
  host.innerHTML = accounts.map((account) => `
    <article class="account-card">
      <div><strong>${escapeHtml(account.name)}</strong><p>${escapeHtml(account.type)}${account.institution ? ` · ${escapeHtml(account.institution)}` : ""}</p></div>
      <span class="tag">Tracking</span>
    </article>`).join("");
}

function renderBudgets(items = budgets) {
  const host = byId("budgetsList");
  if (!items.length) {
    host.className = "card-list empty-state";
    host.textContent = "Create a budget to start tracking your spending.";
    return;
  }
  host.className = "card-list";
  host.innerHTML = items.map((budget) => `
    <button class="budget-card" type="button" data-open-budget="${escapeHtml(budget.budgetId)}">
      <div><strong>${escapeHtml(budget.budgetName)}</strong><p>${escapeHtml(budget.accountName || "Personal budget")} · ${money(budget.totalSpent)} spent</p></div>
      <div class="budget-amount"><strong>${money(budget.totalRemaining)}</strong><p>remaining of ${money(budget.totalLimit)}</p></div>
    </button>`).join("");
}

async function loadBudgetCards() {
  const params = dateQuery(
    byId("budgetsFrom").value,
    byId("budgetsTo").value,
    byId("budgetsAccount").value
  );
  const suffix = params ? `?${params}` : "";
  const reports = await Promise.all(budgets.map((budget) =>
    request(`/api/budgets/${encodeURIComponent(budget.budgetId)}/report${suffix}`)
  ));
  const cards = budgets.map((budget, index) => ({
    ...budget,
    totalLimit: reports[index].totalLimit,
    totalSpent: reports[index].totalSpent,
    totalRemaining: reports[index].totalRemaining,
    accountName: reports[index].accountName
  })).filter((budget) =>
    !byId("budgetsAccount").value
      || budget.accountId === byId("budgetsAccount").value);
  renderBudgets(cards);
}

function renderCategoryRows(categories) {
  const host = byId("categoryBreakdown");
  if (!categories.length) {
    host.className = "breakdown-list empty-state";
    host.textContent = "Add categories to this budget to start tracking.";
    return;
  }
  host.className = "breakdown-list";
  host.innerHTML = categories.map((category) => {
    const ratio = Number(category.limit) > 0
      ? Math.min(100, Number(category.spent) / Number(category.limit) * 100) : 0;
    const over = Number(category.spent) > Number(category.limit);
    return `<div class="breakdown-row">
      <div class="breakdown-head"><strong>${escapeHtml(category.name)}</strong><span>${money(category.spent)} of ${money(category.limit)} · ${money(category.remaining)} left</span></div>
      <div class="progress-track"><div class="progress-fill ${over ? "over" : ""}" style="width:${ratio}%"></div></div>
    </div>`;
  }).join("");
}

function renderAccountBreakdown(items) {
  const host = byId("accountBreakdown");
  if (!items.length) {
    host.className = "breakdown-list empty-state";
    host.textContent = "No purchases in this date range.";
    return;
  }
  host.className = "breakdown-list";
  host.innerHTML = items.map((item) => `
    <div class="account-line"><span class="account-name"><span class="account-symbol">↗</span>${escapeHtml(item.accountName)}</span><strong>${money(item.spent)}</strong></div>`
  ).join("");
}

function renderPurchaseTable(host, purchases, compact = false) {
  if (!purchases.length) {
    host.className = "table-wrap empty-state";
    host.textContent = "No purchases in this view yet.";
    return;
  }
  host.className = "table-wrap";
  const rows = purchases.slice(0, compact ? 6 : purchases.length).map((purchase) => `
    <tr>
      <td><div class="purchase-title"><strong>${escapeHtml(purchase.name)}</strong><span>${escapeHtml(purchase.date)}</span></div></td>
      <td>${escapeHtml(purchase.categoryName)}${purchase.categorySuggested ? ' <span class="tag">Suggested</span>' : ""}</td>
      ${compact ? "" : `<td>${escapeHtml(purchase.accountName || "Personal")}</td><td>${escapeHtml(purchase.budgetName)}</td>`}
      <td><strong>${money(purchase.amount)}</strong></td>
      ${compact ? "" : `<td><div class="table-actions"><button class="table-action" type="button" data-edit-purchase="${escapeHtml(purchase.purchaseId)}">Edit</button><button class="table-action danger" type="button" data-delete-purchase="${escapeHtml(purchase.purchaseId)}">Delete</button></div></td>`}
    </tr>`).join("");
  host.innerHTML = `<table><thead><tr><th>Purchase</th><th>Category</th>${compact ? "" : "<th>Account</th><th>Budget</th>"}<th>Amount</th>${compact ? "" : "<th>Actions</th>"}</tr></thead><tbody>${rows}</tbody></table>`;
}

async function loadOverview() {
  await ensureBudgetOptions();
  if (!budgets.length) {
    byId("metricLimit").textContent = money(0);
    byId("metricSpent").textContent = money(0);
    byId("metricRemaining").textContent = money(0);
    byId("metricCount").textContent = "0";
    renderCategoryRows([]);
    renderAccountBreakdown([]);
    renderPurchaseTable(byId("recentPurchases"), []);
    return;
  }
  const selected = byId("overviewBudget").value;
  const query = dateQuery(
    byId("overviewFrom").value,
    byId("overviewTo").value,
    byId("overviewAccount").value
  );
  const selectedBudgets = selected
    ? budgets.filter((budget) => budget.budgetId === selected)
    : budgets;
  const reports = await Promise.all(selectedBudgets.map((budget) => {
    const suffix = query ? `?${query}` : "";
    return request(`/api/budgets/${encodeURIComponent(budget.budgetId)}/report${suffix}`);
  }));
  reports.forEach((report) => cachedReports.set(report.budgetId, report));
  const limit = reports.reduce((sum, report) => sum + Number(report.totalLimit || 0), 0);
  const spent = reports.reduce((sum, report) => sum + Number(report.totalSpent || 0), 0);
  const purchases = reports.flatMap((report) => report.purchases);
  const categoryMap = new Map();
  const accountMap = new Map();
  reports.forEach((report) => {
    report.categories.forEach((category) => {
      const current = categoryMap.get(category.name) || { ...category, limit: 0, spent: 0 };
      current.limit += Number(category.limit || 0);
      current.spent += Number(category.spent || 0);
      current.remaining = current.limit - current.spent;
      categoryMap.set(category.name, current);
    });
    report.accountBreakdown.forEach((account) => {
      const current = accountMap.get(account.accountName) || { ...account, spent: 0 };
      current.spent += Number(account.spent || 0);
      accountMap.set(account.accountName, current);
    });
  });
  byId("metricLimit").textContent = money(limit);
  byId("metricSpent").textContent = money(spent);
  byId("metricRemaining").textContent = money(limit - spent);
  byId("metricCount").textContent = String(purchases.length);
  renderCategoryRows([...categoryMap.values()]);
  renderAccountBreakdown([...accountMap.values()]);
  renderPurchaseTable(byId("recentPurchases"),
    [...purchases].sort((a, b) => b.date.localeCompare(a.date)), true);
}

async function ensureBudgetOptions() {
  if (!budgets.length) {
    await loadBudgets();
  }
}

async function loadHistory() {
  await ensureBudgetOptions();
  const params = new URLSearchParams();
  if (byId("historyFrom").value) params.set("from", byId("historyFrom").value);
  if (byId("historyTo").value) params.set("to", byId("historyTo").value);
  if (byId("historyAccount").value) params.set("accountId", byId("historyAccount").value);
  if (byId("historyBudget").value) params.set("budgetId", byId("historyBudget").value);
  const suffix = params.toString() ? `?${params}` : "";
  const purchases = await request(`/api/purchases${suffix}`);
  renderPurchaseTable(byId("purchaseHistory"), purchases);
}

async function loadPurchaseCategoryOptions() {
  const select = byId("purchaseCategory");
  const budgetId = byId("purchaseBudget").value;
  select.innerHTML = '<option value="">Suggest for me</option>';
  if (!budgetId) return;
  try {
    const report = cachedReports.get(budgetId)
      || await request(`/api/budgets/${encodeURIComponent(budgetId)}/report`);
    cachedReports.set(budgetId, report);
    select.innerHTML += report.categories.map((category) =>
      `<option value="${escapeHtml(category.categoryId)}">${escapeHtml(category.name)}</option>`
    ).join("");
  } catch (error) {
    showNotice(error.message);
  }
}

async function openPurchaseEditor(purchaseId) {
  const purchase = await request("/api/purchases");
  const item = purchase.find((row) => row.purchaseId === purchaseId);
  if (!item) throw new Error("Purchase is no longer available.");
  const report = cachedReports.get(item.budgetId)
    || await request(`/api/budgets/${encodeURIComponent(item.budgetId)}/report`);
  cachedReports.set(item.budgetId, report);
  const form = byId("editPurchaseForm");
  form.elements.purchaseId.value = item.purchaseId;
  form.elements.name.value = item.name;
  form.elements.amount.value = item.amount;
  form.elements.date.value = item.date;
  const accountLinked = Boolean(item.accountId);
  form.elements.name.readOnly = accountLinked;
  form.elements.amount.readOnly = accountLinked;
  form.elements.date.readOnly = accountLinked;
  form.dataset.accountLinked = String(accountLinked);
  form.querySelector(".edit-note").textContent = accountLinked
    ? "Account-linked purchase details are fixed; you can still change its category."
    : "Changes are saved in your client account and reflected in future budget views.";
  form.elements.categoryId.innerHTML = report.categories.map((category) =>
    `<option value="${escapeHtml(category.categoryId)}">${escapeHtml(category.name)}</option>`
  ).join("");
  form.elements.categoryId.value = item.categoryId;
  byId("editPurchaseDialog").showModal();
}

async function refreshAfterChange(selectedBudget = currentBudgetId) {
  await loadAccounts();
  await loadBudgets(selectedBudget);
  await loadOverview();
  await loadHistory();
}

function reportError(error) {
  showNotice(error.message || "Something went wrong. Please try again.");
}

byId("loginTab").addEventListener("click", () => setAuthMode("login"));
byId("signupTab").addEventListener("click", () => setAuthMode("signup"));
byId("authForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  clearNotice(true);
  const credentials = Object.fromEntries(new FormData(event.currentTarget));
  let authenticated = false;
  try {
    const result = await request(
      authMode === "signup" ? "/api/auth/signup" : "/api/auth/login",
      { method: "POST", body: JSON.stringify(credentials) }
    );
    sessionToken = result.token;
    username = result.username;
    sessionStorage.setItem(tokenKey, sessionToken);
    showApp();
    authenticated = true;
    await loadAccounts();
    await loadBudgets();
    await loadOverview();
  } catch (error) {
    if (authenticated) {
      reportError(error);
    } else {
      showNotice(error.message, "error", true);
    }
  }
});

document.querySelectorAll("[data-nav]").forEach((button) => {
  button.addEventListener("click", () => navigate(button.dataset.nav));
});

byId("logoutButton").addEventListener("click", async () => {
  try {
    await request("/api/auth/logout", { method: "POST" });
  } catch (error) {
    sessionStorage.removeItem(tokenKey);
    sessionToken = "";
    username = "";
    showAuth();
    showNotice(`${error.message}. Your local session was cleared.`, "error", true);
    return;
  }
  sessionStorage.removeItem(tokenKey);
  sessionToken = "";
  username = "";
  showAuth();
});

byId("accountForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const targetForm = event.currentTarget;
  try {
    const payload = Object.fromEntries(new FormData(targetForm));
    await request("/api/accounts", { method: "POST", body: JSON.stringify(payload) });
    targetForm.reset();
    await refreshAfterChange();
    showNotice("Account added.", "success");
  } catch (error) { reportError(error); }
});

byId("budgetForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const targetForm = event.currentTarget;
  try {
    const form = new FormData(targetForm);
    const result = await request("/api/budgets", {
      method: "POST",
      body: JSON.stringify({
        name: form.get("name"),
        accountId: form.get("accountId") || null,
        firstCategory: form.get("firstCategory"),
        firstCategoryLimit: Number(form.get("firstCategoryLimit"))
      })
    });
    currentBudgetId = result.budgetId;
    targetForm.reset();
    await refreshAfterChange(currentBudgetId);
    byId("overviewBudget").value = currentBudgetId;
    showNotice("Budget created and connected.", "success");
  } catch (error) { reportError(error); }
});

byId("budgetForm").elements.accountId.addEventListener("focus", () => {
  accountOptions(byId("budgetAccount"), false, byId("budgetAccount").value);
});

byId("budgetForm").elements.accountId.addEventListener("change", (event) => {
  if (event.target.value) {
    const account = accounts.find((item) => item.id === event.target.value);
    if (account && !["checking", "savings", "credit", "debit", "other"].includes(account.type)) {
      showNotice("This account type cannot be connected to a budget.");
    }
  }
});

byId("categoryForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!currentBudgetId) return;
  const targetForm = event.currentTarget;
  try {
    const payload = Object.fromEntries(new FormData(targetForm));
    payload.limit = Number(payload.limit);
    const report = await request(`/api/budgets/${encodeURIComponent(currentBudgetId)}/categories`, {
      method: "POST", body: JSON.stringify(payload)
    });
    cachedReports.set(currentBudgetId, report);
    targetForm.reset();
    renderManagedCategories(report);
    await refreshAfterChange(currentBudgetId);
    showNotice("Category added.", "success");
  } catch (error) { reportError(error); }
});

byId("purchaseBudget").addEventListener("change", loadPurchaseCategoryOptions);
byId("purchaseForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const targetForm = event.currentTarget;
  try {
    const form = new FormData(targetForm);
    const result = await request(`/api/budgets/${encodeURIComponent(form.get("budgetId"))}/purchases`, {
      method: "POST",
      body: JSON.stringify({
        name: form.get("name"),
        amount: Number(form.get("amount")),
        date: form.get("date") || null,
        categoryId: form.get("categoryId") || null
      })
    });
    targetForm.reset();
    await refreshAfterChange(result.budgetId);
    showNotice(`Purchase added to ${result.categoryName}${result.categorySuggested ? " (suggested category)" : ""}.`, "success");
  } catch (error) { reportError(error); }
});

byId("overviewApply").addEventListener("click", () => loadOverview().catch(reportError));
byId("historyApply").addEventListener("click", () => loadHistory().catch(reportError));
byId("overviewBudget").addEventListener("change", (event) => {
  currentBudgetId = event.target.value;
  loadOverview().catch(reportError);
});

byId("budgetsList").addEventListener("click", async (event) => {
  const button = event.target.closest("[data-open-budget]");
  if (!button) return;
  currentBudgetId = button.dataset.openBudget;
  try {
    const report = await request(`/api/budgets/${encodeURIComponent(currentBudgetId)}/report`);
    cachedReports.set(currentBudgetId, report);
    byId("categoryManager").hidden = false;
    byId("categoryManagerTitle").textContent = report.budgetName;
    renderManagedCategories(report);
    byId("categoryManager").scrollIntoView({ behavior: "smooth" });
  } catch (error) { reportError(error); }
});

byId("budgetsApply").addEventListener("click", () => loadBudgetCards().catch(reportError));

function renderManagedCategories(report) {
  const host = byId("managedCategories");
  host.innerHTML = report.categories.map((category) => `
    <button class="category-chip" type="button" data-edit-category="${escapeHtml(category.categoryId)}">
      <strong>${escapeHtml(category.name)}</strong>${money(category.limit)} limit
    </button>`).join("");
}

byId("managedCategories").addEventListener("click", (event) => {
  const button = event.target.closest("[data-edit-category]");
  if (!button) return;
  const report = cachedReports.get(currentBudgetId);
  const category = report?.categories.find((item) => item.categoryId === button.dataset.editCategory);
  if (!category) return;
  const form = byId("editCategoryForm");
  form.elements.categoryId.value = category.categoryId;
  form.elements.name.value = category.name;
  form.elements.limit.value = category.limit;
  byId("editCategoryDialog").showModal();
});

byId("editCategoryForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const form = new FormData(event.currentTarget);
    const report = await request(`/api/budgets/${encodeURIComponent(currentBudgetId)}/categories/${encodeURIComponent(form.get("categoryId"))}`, {
      method: "PATCH",
      body: JSON.stringify({ name: form.get("name"), limit: Number(form.get("limit")) })
    });
    cachedReports.set(currentBudgetId, report);
    byId("editCategoryDialog").close();
    renderManagedCategories(report);
    await refreshAfterChange(currentBudgetId);
    showNotice("Category updated.", "success");
  } catch (error) { reportError(error); }
});

byId("purchaseHistory").addEventListener("click", async (event) => {
  const edit = event.target.closest("[data-edit-purchase]");
  const remove = event.target.closest("[data-delete-purchase]");
  try {
    if (edit) await openPurchaseEditor(edit.dataset.editPurchase);
    if (remove && window.confirm("Delete this purchase?")) {
      await request(`/api/purchases/${encodeURIComponent(remove.dataset.deletePurchase)}`, { method: "DELETE" });
      await refreshAfterChange();
      showNotice("Purchase deleted.", "success");
    }
  } catch (error) { reportError(error); }
});

byId("editPurchaseForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const values = Object.fromEntries(new FormData(event.currentTarget));
    await request(`/api/purchases/${encodeURIComponent(values.purchaseId)}`, {
      method: "PATCH",
      body: JSON.stringify(event.currentTarget.dataset.accountLinked === "true"
        ? { categoryId: values.categoryId }
        : {
          name: values.name,
          amount: Number(values.amount),
          date: values.date,
          categoryId: values.categoryId
        })
    });
    byId("editPurchaseDialog").close();
    await refreshAfterChange();
    showNotice("Purchase updated.", "success");
  } catch (error) { reportError(error); }
});

byId("closeEditDialog").addEventListener("click", () => byId("editPurchaseDialog").close());
byId("closeCategoryDialog").addEventListener("click", () => byId("editCategoryDialog").close());

async function initialize() {
  if (!sessionToken) {
    showAuth();
    return;
  }
  showApp();
  try {
    const profile = await request("/api/auth/me");
    username = profile.username;
    byId("usernameLabel").textContent = username;
    await loadAccounts();
    await loadBudgets();
    await loadOverview();
  } catch (error) {
    showNotice(error.message);
  }
}

initialize();
