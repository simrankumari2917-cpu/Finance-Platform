// ========================================================
// CampusFin - Personal Finance Platform Frontend Logic
// ========================================================

let authToken = localStorage.getItem("campusfin_token") || null;
let currentUser = null;
let currentCurrency = "$";
let currentView = "overview";

// Cached Data
let cachedExpenses = [];
let cachedBudgets = [];
let cachedGoals = [];
let cachedAdminUsers = [];

// Initialize on page load
document.addEventListener("DOMContentLoaded", () => {
    loadPublicConfig();
    if (authToken) {
        verifySession();
    } else {
        showAuth();
    }
});

// ========================================================
// 1. PUBLIC CONFIG & UTILS
// ========================================================

async function loadPublicConfig() {
    try {
        const res = await apiRequest("/api/public/config", "GET");
        if (res.success && res.data) {
            document.getElementById("brandName").textContent = res.data.platformName || "CampusFin";
            if (res.data.defaultCurrency) {
                // Extract symbol if present e.g. "USD ($)" -> "$"
                const match = res.data.defaultCurrency.match(/\((.*?)\)/);
                currentCurrency = match ? match[1] : "$";
                document.querySelectorAll(".currency-label").forEach(el => el.textContent = currentCurrency);
            }
        }
    } catch (e) {
        console.warn("Public config not loaded:", e);
    }
}

function formatCurrency(amount) {
    const val = Number(amount) || 0;
    return `${currentCurrency}${val.toFixed(2)}`;
}

function showAlert(message, type = "success") {
    const container = document.getElementById("alertContainer");
    const box = document.getElementById("alertBox");
    const msg = document.getElementById("alertMessage");

    box.className = `alert alert-${type}`;
    msg.textContent = message;
    container.style.display = "block";

    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        hideAlert();
    }, 5000);
}

function hideAlert() {
    document.getElementById("alertContainer").style.display = "none";
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = "none";
}

function openModal(modalId) {
    document.getElementById(modalId).style.display = "flex";
}

// ========================================================
// 2. AUTHENTICATION & SESSION
// ========================================================

function toggleAuthMode(mode) {
    if (mode === "register") {
        document.getElementById("loginFormContainer").style.display = "none";
        document.getElementById("registerFormContainer").style.display = "block";
    } else {
        document.getElementById("loginFormContainer").style.display = "block";
        document.getElementById("registerFormContainer").style.display = "none";
    }
}

function fillQuickLogin(email, password) {
    document.getElementById("loginEmail").value = email;
    document.getElementById("loginPassword").value = password;
    handleLoginSubmit(new Event("submit"));
}

async function handleLoginSubmit(event) {
    event.preventDefault();
    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value.trim();

    try {
        const res = await apiRequest("/api/auth/login", "POST", { email, password });
        if (res.success) {
            authToken = res.data.token;
            localStorage.setItem("campusfin_token", authToken);
            currentUser = res.data;
            showAlert(res.message, "success");
            setupUserInterface();
        } else {
            showAlert(res.error || "Login failed", "danger");
        }
    } catch (err) {
        showAlert(err.message, "danger");
    }
}

async function handleRegisterSubmit(event) {
    event.preventDefault();
    const fullName = document.getElementById("regFullName").value.trim();
    const email = document.getElementById("regEmail").value.trim();
    const password = document.getElementById("regPassword").value.trim();

    try {
        const res = await apiRequest("/api/auth/register", "POST", { fullName, email, password });
        if (res.success) {
            authToken = res.data.token;
            localStorage.setItem("campusfin_token", authToken);
            currentUser = res.data;
            showAlert("Account created! Welcome, " + fullName, "success");
            setupUserInterface();
        } else {
            showAlert(res.error || "Registration failed", "danger");
        }
    } catch (err) {
        showAlert(err.message, "danger");
    }
}

async function verifySession() {
    try {
        const res = await apiRequest("/api/auth/me", "GET");
        if (res.success) {
            currentUser = res.data;
            setupUserInterface();
        } else {
            handleLogout();
        }
    } catch (e) {
        handleLogout();
    }
}

function setupUserInterface() {
    document.getElementById("authSection").style.display = "none";
    document.getElementById("mainNav").style.display = "flex";

    document.getElementById("userName").textContent = currentUser.fullName;
    const badge = document.getElementById("userBadge");
    badge.textContent = currentUser.role;
    badge.className = `role-badge ${currentUser.role === 'ADMIN' ? 'role-admin' : 'role-user'}`;

    // Admin nav links
    const adminItems = document.querySelectorAll(".admin-nav-item");
    const userItems = document.querySelectorAll(".user-nav-item");

    if (currentUser.role === "ADMIN") {
        adminItems.forEach(el => el.style.display = "block");
        userItems.forEach(el => el.style.display = "block");
        navigateTo("overview");
    } else {
        adminItems.forEach(el => el.style.display = "none");
        userItems.forEach(el => el.style.display = "block");
        navigateTo("overview");
    }
}

async function handleLogout() {
    if (authToken) {
        try {
            await apiRequest("/api/auth/logout", "POST");
        } catch (e) {}
    }
    authToken = null;
    currentUser = null;
    localStorage.removeItem("campusfin_token");
    showAuth();
}

function showAuth() {
    document.getElementById("mainNav").style.display = "none";
    document.querySelectorAll(".app-view").forEach(el => el.style.display = "none");
    document.getElementById("authSection").style.display = "block";
    toggleAuthMode("login");
}

// ========================================================
// 3. NAVIGATION & VIEW ROUTING
// ========================================================

function navigateTo(view) {
    currentView = view;

    // Hide all views
    document.querySelectorAll(".app-view").forEach(el => el.style.display = "none");

    // Remove active class from nav buttons
    document.querySelectorAll(".nav-links button").forEach(b => b.classList.remove("active"));

    // Activate selected view
    switch (view) {
        case "overview":
            document.getElementById("viewOverview").style.display = "block";
            document.getElementById("navOverview").classList.add("active");
            loadOverview();
            break;
        case "expenses":
            document.getElementById("viewExpenses").style.display = "block";
            document.getElementById("navExpenses").classList.add("active");
            loadExpenses();
            break;
        case "budgets":
            document.getElementById("viewBudgets").style.display = "block";
            document.getElementById("navBudgets").classList.add("active");
            loadBudgets();
            break;
        case "goals":
            document.getElementById("viewGoals").style.display = "block";
            document.getElementById("navGoals").classList.add("active");
            loadGoals();
            break;
        case "admin-users":
            if (currentUser && currentUser.role === "ADMIN") {
                document.getElementById("viewAdminUsers").style.display = "block";
                document.getElementById("navAdminUsers").classList.add("active");
                loadAdminUsers();
            }
            break;
        case "admin-security":
            if (currentUser && currentUser.role === "ADMIN") {
                document.getElementById("viewAdminSecurity").style.display = "block";
                document.getElementById("navAdminSecurity").classList.add("active");
                loadAdminSecurity();
            }
            break;
        case "admin-system":
            if (currentUser && currentUser.role === "ADMIN") {
                document.getElementById("viewAdminSystem").style.display = "block";
                document.getElementById("navAdminSystem").classList.add("active");
                loadAdminSystem();
            }
            break;
    }
}

// ========================================================
// 4. OVERVIEW DASHBOARD
// ========================================================

async function loadOverview() {
    try {
        const [sumRes, expRes, budRes, goalRes] = await Promise.all([
            apiRequest("/api/expenses/summary", "GET"),
            apiRequest("/api/expenses", "GET"),
            apiRequest("/api/budgets", "GET"),
            apiRequest("/api/goals", "GET")
        ]);

        // Stat cards
        if (sumRes.success) {
            document.getElementById("statTotalExpense").textContent = formatCurrency(sumRes.data.totalSpending);
            document.getElementById("statMonthlyExpense").textContent = formatCurrency(sumRes.data.monthlySpending);

            // Category breakdown
            renderCategoryBreakdown(sumRes.data.categoryDistribution || {}, sumRes.data.totalSpending);
        }

        if (budRes.success) {
            document.getElementById("statActiveBudgets").textContent = budRes.data.length;
        }

        if (goalRes.success) {
            const activeGoals = goalRes.data.filter(g => g.status === "IN_PROGRESS").length;
            document.getElementById("statActiveGoals").textContent = activeGoals;
        }

        // Recent expenses (up to 5)
        if (expRes.success) {
            renderRecentExpenses(expRes.data.slice(0, 5));
        }

    } catch (err) {
        console.error("Error loading overview:", err);
    }
}

function renderCategoryBreakdown(distribution, total) {
    const container = document.getElementById("categoryBreakdownContainer");
    const categories = Object.keys(distribution);

    if (categories.length === 0 || total === 0) {
        container.innerHTML = '<p style="color: var(--text-muted); font-size: 0.9rem;">No expense categories recorded yet.</p>';
        return;
    }

    let html = "";
    for (const cat of categories) {
        const amt = distribution[cat];
        const pct = Math.round((amt / total) * 100);
        html += `
            <div style="margin-bottom: 0.85rem;">
                <div style="display: flex; justify-content: space-between; font-size: 0.85rem; font-weight: 500;">
                    <span>${cat}</span>
                    <span>${formatCurrency(amt)} (${pct}%)</span>
                </div>
                <div class="progress-container">
                    <div class="progress-bar progress-bar-success" style="width: ${pct}%;"></div>
                </div>
            </div>
        `;
    }
    container.innerHTML = html;
}

function renderRecentExpenses(expenses) {
    const container = document.getElementById("recentExpensesContainer");
    if (!expenses || expenses.length === 0) {
        container.innerHTML = '<p style="color: var(--text-muted); font-size: 0.9rem;">No transactions yet.</p>';
        return;
    }

    let html = '<div class="table-responsive"><table class="data-table"><tbody>';
    for (const exp of expenses) {
        html += `
            <tr>
                <td style="font-size: 0.85rem;">${exp.expenseDate}</td>
                <td><span class="badge badge-info">${exp.category}</span></td>
                <td style="font-weight: 500;">${exp.description || "-"}</td>
                <td style="font-weight: 600; text-align: right; color: var(--danger);">${formatCurrency(exp.amount)}</td>
            </tr>
        `;
    }
    html += '</tbody></table></div>';
    container.innerHTML = html;
}

// ========================================================
// 5. EXPENSES MANAGEMENT
// ========================================================

async function loadExpenses() {
    const category = document.getElementById("expenseFilterCategory").value;
    const startDate = document.getElementById("expenseFilterStartDate").value;
    const endDate = document.getElementById("expenseFilterEndDate").value;

    let url = `/api/expenses?category=${encodeURIComponent(category)}`;
    if (startDate) url += `&startDate=${encodeURIComponent(startDate)}`;
    if (endDate) url += `&endDate=${encodeURIComponent(endDate)}`;

    try {
        const res = await apiRequest(url, "GET");
        if (res.success) {
            cachedExpenses = res.data;
            renderExpensesTable(res.data);
            document.getElementById("expenseCountBadge").textContent = `${res.data.length} records`;
        }
    } catch (e) {
        showAlert("Failed to load expenses: " + e.message, "danger");
    }
}

function clearExpenseFilters() {
    document.getElementById("expenseFilterCategory").value = "ALL";
    document.getElementById("expenseFilterStartDate").value = "";
    document.getElementById("expenseFilterEndDate").value = "";
    loadExpenses();
}

function renderExpensesTable(expenses) {
    const tbody = document.getElementById("expensesTableBody");
    if (!expenses || expenses.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 2rem;">No matching expense records found.</td></tr>';
        return;
    }

    let html = "";
    for (const exp of expenses) {
        html += `
            <tr>
                <td>${exp.expenseDate}</td>
                <td><span class="badge badge-info">${exp.category}</span></td>
                <td>${exp.description ? escapeHtml(exp.description) : '<span style="color:var(--text-muted); font-style:italic;">No description</span>'}</td>
                <td style="font-weight: 600; color: var(--danger);">${formatCurrency(exp.amount)}</td>
                <td style="text-align: right;">
                    <button class="btn btn-secondary btn-sm" onclick="openExpenseModal(${exp.id})">Edit</button>
                    <button class="btn btn-outline-danger btn-sm" onclick="deleteExpense(${exp.id})">Delete</button>
                </td>
            </tr>
        `;
    }
    tbody.innerHTML = html;
}

function openExpenseModal(expenseId = null) {
    const modalTitle = document.getElementById("modalExpenseTitle");
    const formId = document.getElementById("expenseFormId");
    const amount = document.getElementById("expenseFormAmount");
    const category = document.getElementById("expenseFormCategory");
    const date = document.getElementById("expenseFormDate");
    const desc = document.getElementById("expenseFormDescription");

    if (expenseId) {
        const exp = cachedExpenses.find(e => e.id === expenseId);
        if (!exp) return;
        modalTitle.textContent = "Edit Expense";
        formId.value = exp.id;
        amount.value = exp.amount;
        category.value = exp.category;
        date.value = exp.expenseDate;
        desc.value = exp.description || "";
    } else {
        modalTitle.textContent = "Add New Expense";
        formId.value = "";
        amount.value = "";
        category.value = "Food & Dining";
        date.value = new Date().toISOString().split("T")[0];
        desc.value = "";
    }
    openModal("modalExpense");
}

async function handleExpenseFormSubmit(event) {
    event.preventDefault();
    const id = document.getElementById("expenseFormId").value;
    const amount = parseFloat(document.getElementById("expenseFormAmount").value);
    const category = document.getElementById("expenseFormCategory").value;
    const expenseDate = document.getElementById("expenseFormDate").value;
    const description = document.getElementById("expenseFormDescription").value.trim();

    try {
        let res;
        if (id) {
            res = await apiRequest("/api/expenses/update", "POST", { id: parseInt(id), amount, category, expenseDate, description });
        } else {
            res = await apiRequest("/api/expenses", "POST", { amount, category, expenseDate, description });
        }

        if (res.success) {
            closeModal("modalExpense");
            showAlert(res.message, "success");
            loadExpenses();
        } else {
            showAlert(res.error || "Failed to save expense", "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function deleteExpense(id) {
    if (!confirm("Are you sure you want to delete this expense record?")) return;
    try {
        const res = await apiRequest("/api/expenses/delete", "POST", { id });
        if (res.success) {
            showAlert(res.message, "success");
            loadExpenses();
        } else {
            showAlert(res.error, "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ========================================================
// 6. BUDGETS MANAGEMENT
// ========================================================

async function loadBudgets() {
    try {
        const res = await apiRequest("/api/budgets", "GET");
        if (res.success) {
            cachedBudgets = res.data;
            renderBudgetsGrid(res.data);
        }
    } catch (e) {
        showAlert("Failed to load budgets: " + e.message, "danger");
    }
}

function renderBudgetsGrid(budgets) {
    const grid = document.getElementById("budgetsGrid");
    if (!budgets || budgets.length === 0) {
        grid.innerHTML = '<p style="color: var(--text-muted); font-size: 0.95rem; grid-column: 1/-1; padding: 2rem; text-align: center; background:#fff; border-radius:10px;">No budgets created yet. Click "+ Create Budget" above to set spending limits.</p>';
        return;
    }

    let html = "";
    for (const b of budgets) {
        const pct = b.utilizationPercentage;
        let progressClass = "progress-bar-success";
        let statusBadge = `<span class="badge badge-success">Safe</span>`;

        if (pct >= 100) {
            progressClass = "progress-bar-danger";
            statusBadge = `<span class="badge badge-danger">Exceeded!</span>`;
        } else if (pct >= 80) {
            progressClass = "progress-bar-warning";
            statusBadge = `<span class="badge badge-warning">Near Limit</span>`;
        }

        const barWidth = Math.min(100, pct);

        html += `
            <div class="item-card">
                <div class="item-header">
                    <div>
                        <div class="item-title">${escapeHtml(b.category)}</div>
                        <div class="item-meta">${b.duration} Budget</div>
                    </div>
                    ${statusBadge}
                </div>

                <div class="item-details">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 0.25rem;">
                        <span style="color: var(--text-muted); font-size: 0.85rem;">Spent / Limit</span>
                        <span style="font-weight: 600;">${formatCurrency(b.spentAmount)} / ${formatCurrency(b.amountLimit)}</span>
                    </div>

                    <div class="progress-container">
                        <div class="progress-bar ${progressClass}" style="width: ${barWidth}%;"></div>
                    </div>

                    <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-top: 0.35rem;">
                        <span>Utilization: <strong>${pct}%</strong></span>
                        <span style="color: ${b.remainingAmount < 0 ? 'var(--danger)' : 'var(--text-muted)'}; font-weight: 500;">
                            Remaining: ${formatCurrency(b.remainingAmount)}
                        </span>
                    </div>
                </div>

                <div class="item-actions">
                    <button class="btn btn-secondary btn-sm" style="flex: 1;" onclick="openBudgetModal(${b.id})">Edit</button>
                    <button class="btn btn-outline-danger btn-sm" onclick="deleteBudget(${b.id})">Delete</button>
                </div>
            </div>
        `;
    }
    grid.innerHTML = html;
}

function openBudgetModal(budgetId = null) {
    const title = document.getElementById("modalBudgetTitle");
    const formId = document.getElementById("budgetFormId");
    const cat = document.getElementById("budgetFormCategory");
    const limit = document.getElementById("budgetFormLimit");
    const duration = document.getElementById("budgetFormDuration");

    if (budgetId) {
        const b = cachedBudgets.find(item => item.id === budgetId);
        if (!b) return;
        title.textContent = "Edit Budget";
        formId.value = b.id;
        cat.value = b.category;
        limit.value = b.amountLimit;
        duration.value = b.duration;
    } else {
        title.textContent = "Create Budget";
        formId.value = "";
        cat.value = "Food & Dining";
        limit.value = "";
        duration.value = "Monthly";
    }
    openModal("modalBudget");
}

async function handleBudgetFormSubmit(event) {
    event.preventDefault();
    const id = document.getElementById("budgetFormId").value;
    const category = document.getElementById("budgetFormCategory").value;
    const amountLimit = parseFloat(document.getElementById("budgetFormLimit").value);
    const duration = document.getElementById("budgetFormDuration").value;

    try {
        let res;
        if (id) {
            res = await apiRequest("/api/budgets/update", "POST", { id: parseInt(id), category, amountLimit, duration });
        } else {
            res = await apiRequest("/api/budgets", "POST", { category, amountLimit, duration });
        }

        if (res.success) {
            closeModal("modalBudget");
            showAlert(res.message, "success");
            loadBudgets();
        } else {
            showAlert(res.error || "Failed to save budget", "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function deleteBudget(id) {
    if (!confirm("Are you sure you want to delete this budget?")) return;
    try {
        const res = await apiRequest("/api/budgets/delete", "POST", { id });
        if (res.success) {
            showAlert(res.message, "success");
            loadBudgets();
        } else {
            showAlert(res.error, "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ========================================================
// 7. FINANCIAL GOALS MANAGEMENT
// ========================================================

async function loadGoals() {
    try {
        const res = await apiRequest("/api/goals", "GET");
        if (res.success) {
            cachedGoals = res.data;
            renderGoalsGrid(res.data);
        }
    } catch (e) {
        showAlert("Failed to load goals: " + e.message, "danger");
    }
}

function renderGoalsGrid(goals) {
    const grid = document.getElementById("goalsGrid");
    if (!goals || goals.length === 0) {
        grid.innerHTML = '<p style="color: var(--text-muted); font-size: 0.95rem; grid-column: 1/-1; padding: 2rem; text-align: center; background:#fff; border-radius:10px;">No goals established yet. Click "+ Create Goal" above to start savings tracking.</p>';
        return;
    }

    let html = "";
    for (const g of goals) {
        const isCompleted = g.status === "COMPLETED";
        const badge = isCompleted ?
            `<span class="badge badge-success">Completed 🎉</span>` :
            `<span class="badge badge-info">In Progress</span>`;

        html += `
            <div class="item-card">
                <div class="item-header">
                    <div>
                        <div class="item-title">${escapeHtml(g.description)}</div>
                        <div class="item-meta">Target Deadline: <strong>${g.deadline}</strong></div>
                    </div>
                    ${badge}
                </div>

                <div class="item-details">
                    <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 0.25rem;">
                        <span style="color: var(--text-muted);">Saved / Target</span>
                        <span style="font-weight: 600;">${formatCurrency(g.currentAmount)} / ${formatCurrency(g.targetAmount)}</span>
                    </div>

                    <div class="progress-container">
                        <div class="progress-bar ${isCompleted ? 'progress-bar-success' : 'progress-bar-warning'}" style="width: ${Math.min(100, g.progressPercentage)}%;"></div>
                    </div>

                    <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-top: 0.35rem;">
                        <span>Progress: <strong>${g.progressPercentage}%</strong></span>
                        <span style="color: var(--text-muted);">
                            ${isCompleted ? 'Goal reached!' : 'Need ' + formatCurrency(g.remainingAmount)}
                        </span>
                    </div>
                </div>

                <div class="item-actions">
                    <button class="btn btn-success btn-sm" onclick="openDepositModal(${g.id}, '${escapeHtml(g.description)}')">+ Add Funds</button>
                    <button class="btn btn-secondary btn-sm" onclick="openGoalModal(${g.id})">Edit</button>
                    <button class="btn btn-outline-danger btn-sm" onclick="deleteGoal(${g.id})">Delete</button>
                </div>
            </div>
        `;
    }
    grid.innerHTML = html;
}

function openGoalModal(goalId = null) {
    const title = document.getElementById("modalGoalTitle");
    const formId = document.getElementById("goalFormId");
    const desc = document.getElementById("goalFormDescription");
    const target = document.getElementById("goalFormTarget");
    const current = document.getElementById("goalFormCurrent");
    const deadline = document.getElementById("goalFormDeadline");

    if (goalId) {
        const g = cachedGoals.find(item => item.id === goalId);
        if (!g) return;
        title.textContent = "Edit Financial Goal";
        formId.value = g.id;
        desc.value = g.description;
        target.value = g.targetAmount;
        current.value = g.currentAmount;
        deadline.value = g.deadline;
    } else {
        title.textContent = "Create Financial Goal";
        formId.value = "";
        desc.value = "";
        target.value = "";
        current.value = "0";
        // Default deadline: 3 months from now
        const d = new Date();
        d.setMonth(d.getMonth() + 3);
        deadline.value = d.toISOString().split("T")[0];
    }
    openModal("modalGoal");
}

async function handleGoalFormSubmit(event) {
    event.preventDefault();
    const id = document.getElementById("goalFormId").value;
    const description = document.getElementById("goalFormDescription").value.trim();
    const targetAmount = parseFloat(document.getElementById("goalFormTarget").value);
    const currentAmount = parseFloat(document.getElementById("goalFormCurrent").value);
    const deadline = document.getElementById("goalFormDeadline").value;

    try {
        let res;
        if (id) {
            res = await apiRequest("/api/goals/update", "POST", { id: parseInt(id), description, targetAmount, currentAmount, deadline });
        } else {
            res = await apiRequest("/api/goals", "POST", { description, targetAmount, currentAmount, deadline });
        }

        if (res.success) {
            closeModal("modalGoal");
            showAlert(res.message, "success");
            loadGoals();
        } else {
            showAlert(res.error || "Failed to save goal", "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

function openDepositModal(goalId, goalName) {
    document.getElementById("depositGoalId").value = goalId;
    document.getElementById("depositGoalName").textContent = `Goal: "${goalName}"`;
    document.getElementById("depositAmount").value = "";
    openModal("modalDeposit");
}

async function handleDepositSubmit(event) {
    event.preventDefault();
    const id = parseInt(document.getElementById("depositGoalId").value);
    const amount = parseFloat(document.getElementById("depositAmount").value);

    try {
        const res = await apiRequest("/api/goals/deposit", "POST", { id, amount });
        if (res.success) {
            closeModal("modalDeposit");
            showAlert(res.message, "success");
            loadGoals();
        } else {
            showAlert(res.error, "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function deleteGoal(id) {
    if (!confirm("Are you sure you want to delete this financial goal?")) return;
    try {
        const res = await apiRequest("/api/goals/delete", "POST", { id });
        if (res.success) {
            showAlert(res.message, "success");
            loadGoals();
        } else {
            showAlert(res.error, "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ========================================================
// 8. ADMIN: USER MANAGEMENT
// ========================================================

async function loadAdminUsers() {
    try {
        // Fetch stats
        const statsRes = await apiRequest("/api/admin/stats", "GET");
        if (statsRes.success) {
            document.getElementById("adminTotalUsers").textContent = statsRes.data.totalUsers;
            document.getElementById("adminActiveUsers").textContent = statsRes.data.activeUsers;
            document.getElementById("adminSuspendedUsers").textContent = statsRes.data.suspendedUsers;
            document.getElementById("adminDbEngine").textContent = statsRes.data.databaseEngine;
        }

        // Fetch user list
        const q = document.getElementById("adminUserSearchInput").value.trim();
        const role = document.getElementById("adminRoleFilter").value;
        const status = document.getElementById("adminStatusFilter").value;

        const url = `/api/admin/users?q=${encodeURIComponent(q)}&role=${encodeURIComponent(role)}&status=${encodeURIComponent(status)}`;
        const usersRes = await apiRequest(url, "GET");

        if (usersRes.success) {
            cachedAdminUsers = usersRes.data;
            renderAdminUsersTable(usersRes.data);
            document.getElementById("adminUserCount").textContent = `${usersRes.data.length} accounts`;
        }
    } catch (e) {
        showAlert("Failed to load admin users: " + e.message, "danger");
    }
}

function clearAdminUserFilters() {
    document.getElementById("adminUserSearchInput").value = "";
    document.getElementById("adminRoleFilter").value = "ALL";
    document.getElementById("adminStatusFilter").value = "ALL";
    loadAdminUsers();
}

function renderAdminUsersTable(users) {
    const tbody = document.getElementById("adminUsersTableBody");
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 2rem;">No users found matching query.</td></tr>';
        return;
    }

    let html = "";
    for (const u of users) {
        const roleBadge = u.role === "ADMIN" ?
            `<span class="role-badge role-admin">ADMIN</span>` :
            `<span class="role-badge role-user">USER</span>`;

        const statusBadge = u.status === "ACTIVE" ?
            `<span class="badge badge-success">ACTIVE</span>` :
            `<span class="badge badge-danger">SUSPENDED</span>`;

        const isSelf = currentUser && currentUser.id === u.id;

        html += `
            <tr>
                <td><strong>#${u.id}</strong></td>
                <td style="font-weight: 500;">${escapeHtml(u.fullName)} ${isSelf ? '<span class="badge badge-secondary">You</span>' : ''}</td>
                <td>${escapeHtml(u.email)}</td>
                <td>${roleBadge}</td>
                <td>${statusBadge}</td>
                <td style="text-align: right;">
                    <button class="btn btn-secondary btn-sm" onclick="openAdminUserModal(${u.id})">Edit</button>
                    ${!isSelf ? `<button class="btn btn-outline-danger btn-sm" onclick="deleteAdminUser(${u.id}, '${escapeHtml(u.email)}')">Delete</button>` : ''}
                </td>
            </tr>
        `;
    }
    tbody.innerHTML = html;
}

function openAdminUserModal(userId = null) {
    const title = document.getElementById("modalAdminUserTitle");
    const formId = document.getElementById("adminUserFormId");
    const name = document.getElementById("adminUserFormName");
    const email = document.getElementById("adminUserFormEmail");
    const pass = document.getElementById("adminUserFormPassword");
    const role = document.getElementById("adminUserFormRole");
    const status = document.getElementById("adminUserFormStatus");

    if (userId) {
        const u = cachedAdminUsers.find(item => item.id === userId);
        if (!u) return;
        title.textContent = "Edit User Account";
        formId.value = u.id;
        name.value = u.fullName;
        email.value = u.email;
        pass.value = "";
        pass.placeholder = "Leave empty to keep existing password";
        pass.required = false;
        role.value = u.role;
        status.value = u.status;
    } else {
        title.textContent = "Create New User Account";
        formId.value = "";
        name.value = "";
        email.value = "";
        pass.value = "";
        pass.placeholder = "Enter initial password (min 6 chars)";
        pass.required = true;
        role.value = "USER";
        status.value = "ACTIVE";
    }
    openModal("modalAdminUser");
}

async function handleAdminUserSubmit(event) {
    event.preventDefault();
    const id = document.getElementById("adminUserFormId").value;
    const fullName = document.getElementById("adminUserFormName").value.trim();
    const email = document.getElementById("adminUserFormEmail").value.trim();
    const password = document.getElementById("adminUserFormPassword").value;
    const role = document.getElementById("adminUserFormRole").value;
    const status = document.getElementById("adminUserFormStatus").value;

    try {
        let res;
        if (id) {
            res = await apiRequest("/api/admin/users/update", "POST", { id: parseInt(id), fullName, email, password, role, status });
        } else {
            res = await apiRequest("/api/admin/users", "POST", { fullName, email, password, role, status });
        }

        if (res.success) {
            closeModal("modalAdminUser");
            showAlert(res.message, "success");
            loadAdminUsers();
        } else {
            showAlert(res.error || "Failed to save user account", "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

async function deleteAdminUser(id, email) {
    if (!confirm(`Are you sure you want to permanently delete account ${email}? All associated expenses, budgets, and goals will be removed.`)) return;

    try {
        const res = await apiRequest("/api/admin/users/delete", "POST", { id });
        if (res.success) {
            showAlert(res.message, "success");
            loadAdminUsers();
        } else {
            showAlert(res.error, "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ========================================================
// 9. ADMIN: SECURITY SETTINGS
// ========================================================

async function loadAdminSecurity() {
    try {
        const [secRes, logsRes] = await Promise.all([
            apiRequest("/api/admin/security", "GET"),
            apiRequest("/api/admin/security/logs", "GET")
        ]);

        if (secRes.success && secRes.data) {
            const s = secRes.data;
            document.getElementById("secEncryption").value = s.encryptionStandard;
            document.getElementById("secTlsRequired").checked = s.tlsRequired;
            document.getElementById("secSessionTimeout").value = s.sessionTimeoutMinutes;
            document.getElementById("secMfa").value = s.mfaEnforcement;
            document.getElementById("secLockout").value = s.lockoutThreshold;
        }

        if (logsRes.success) {
            renderSecurityLogs(logsRes.data);
        }
    } catch (e) {
        showAlert("Failed to load security settings: " + e.message, "danger");
    }
}

function renderSecurityLogs(logs) {
    const tbody = document.getElementById("securityLogsTableBody");
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No security activity logged yet.</td></tr>';
        return;
    }

    let html = "";
    for (const log of logs) {
        html += `
            <tr>
                <td><span class="badge badge-info">${escapeHtml(log.action)}</span></td>
                <td><strong>${escapeHtml(log.performedBy)}</strong></td>
                <td>${escapeHtml(log.details)}</td>
                <td style="font-family: monospace; font-size: 0.8rem;">${log.ipAddress}</td>
            </tr>
        `;
    }
    tbody.innerHTML = html;
}

async function handleSecuritySettingsSubmit(event) {
    event.preventDefault();
    const encryptionStandard = document.getElementById("secEncryption").value;
    const tlsRequired = document.getElementById("secTlsRequired").checked;
    const sessionTimeoutMinutes = parseInt(document.getElementById("secSessionTimeout").value);
    const mfaEnforcement = document.getElementById("secMfa").value;
    const lockoutThreshold = parseInt(document.getElementById("secLockout").value);

    try {
        const res = await apiRequest("/api/admin/security", "POST", {
            encryptionStandard,
            tlsRequired,
            sessionTimeoutMinutes,
            mfaEnforcement,
            lockoutThreshold
        });

        if (res.success) {
            showAlert(res.message, "success");
            loadAdminSecurity();
        } else {
            showAlert(res.error, "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ========================================================
// 10. ADMIN: SYSTEM SETTINGS
// ========================================================

async function loadAdminSystem() {
    try {
        const [sysRes, logsRes] = await Promise.all([
            apiRequest("/api/admin/system", "GET"),
            apiRequest("/api/admin/system/logs", "GET")
        ]);

        if (sysRes.success && sysRes.data) {
            const s = sysRes.data;
            document.getElementById("sysPlatformName").value = s.platformName;
            document.getElementById("sysCurrency").value = s.defaultCurrency;
            document.getElementById("sysBudgetThreshold").value = s.budgetAlertThreshold;
            document.getElementById("sysMaintenance").checked = s.maintenanceMode;
        }

        if (logsRes.success) {
            renderSystemLogs(logsRes.data);
        }
    } catch (e) {
        showAlert("Failed to load system settings: " + e.message, "danger");
    }
}

function renderSystemLogs(logs) {
    const tbody = document.getElementById("systemLogsTableBody");
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No configuration changes recorded yet.</td></tr>';
        return;
    }

    let html = "";
    for (const log of logs) {
        html += `
            <tr>
                <td><span class="badge badge-warning">${escapeHtml(log.action)}</span></td>
                <td><strong>${escapeHtml(log.performedBy)}</strong></td>
                <td>${escapeHtml(log.details)}</td>
                <td style="font-family: monospace; font-size: 0.8rem;">${log.ipAddress}</td>
            </tr>
        `;
    }
    tbody.innerHTML = html;
}

async function handleSystemSettingsSubmit(event) {
    event.preventDefault();
    const platformName = document.getElementById("sysPlatformName").value.trim();
    const defaultCurrency = document.getElementById("sysCurrency").value;
    const budgetAlertThreshold = parseInt(document.getElementById("sysBudgetThreshold").value);
    const maintenanceMode = document.getElementById("sysMaintenance").checked;

    try {
        const res = await apiRequest("/api/admin/system", "POST", {
            platformName,
            defaultCurrency,
            budgetAlertThreshold,
            maintenanceMode
        });

        if (res.success) {
            showAlert(res.message, "success");
            loadPublicConfig();
            loadAdminSystem();
        } else {
            showAlert(res.error, "danger");
        }
    } catch (e) {
        showAlert(e.message, "danger");
    }
}

// ========================================================
// 11. FETCH API HELPER
// ========================================================

async function apiRequest(endpoint, method = "GET", body = null) {
    const headers = {
        "Content-Type": "application/json"
    };

    if (authToken) {
        headers["Authorization"] = `Bearer ${authToken}`;
    }

    const options = { method, headers };
    if (body && (method === "POST" || method === "PUT")) {
        options.body = JSON.stringify(body);
    }

    const response = await fetch(endpoint, options);
    let data;
    try {
        data = await response.json();
    } catch (e) {
        data = { success: false, error: "Invalid JSON response from server" };
    }

    if (!response.ok && data.success !== false) {
        data.success = false;
        data.error = data.error || `HTTP error ${response.status}`;
    }

    return data;
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
