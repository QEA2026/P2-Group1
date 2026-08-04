"use strict";

/*
    View elements
*/

const loadingView = document.getElementById("loading-view");
const loginView = document.getElementById("login-view");
const dashboardView = document.getElementById(
    "dashboard-view"
);

/*
    Forms
*/

const loginForm = document.getElementById("login-form");
const expenseForm = document.getElementById(
    "expense-form"
);
const editExpenseForm = document.getElementById(
    "edit-expense-form"
);

/*
    Buttons
*/

const loginButton = document.getElementById(
    "login-button"
);
const logoutButton = document.getElementById(
    "logout-button"
);
const refreshButton = document.getElementById(
    "refresh-button"
);
const submitExpenseButton = document.getElementById(
    "submit-expense-button"
);
const closeEditModalButton = document.getElementById(
    "close-edit-modal"
);
const cancelEditButton = document.getElementById(
    "cancel-edit-button"
);
const saveEditButton = document.getElementById(
    "save-edit-button"
);

/*
    Login inputs
*/

const usernameInput = document.getElementById(
    "username"
);
const passwordInput = document.getElementById(
    "password"
);

/*
    Submit expense inputs
*/

const amountInput = document.getElementById(
    "expense-amount"
);
const descriptionInput = document.getElementById(
    "expense-description"
);
const categoryInput = document.getElementById(
    "expense-category"
);

/*
    Edit expense modal inputs
*/

const editModal = document.getElementById(
    "edit-modal"
);
const editExpenseIdInput = document.getElementById(
    "edit-expense-id"
);
const editExpenseAmountInput = document.getElementById(
    "edit-expense-amount"
);
const editExpenseDescriptionInput =
    document.getElementById(
        "edit-expense-description"
    );
const editExpenseCategoryInput = document.getElementById(
    "edit-expense-category"
);

/*
    Message elements
*/

const loginMessage = document.getElementById(
    "login-message"
);
const expenseMessage = document.getElementById(
    "expense-message"
);
const editExpenseMessage = document.getElementById(
    "edit-expense-message"
);
const tableMessage = document.getElementById(
    "table-message"
);

/*
    Dashboard elements
*/

const welcomeMessage = document.getElementById(
    "welcome-message"
);
const dashboardTitle = document.getElementById(
    "dashboard-title"
);

/*
    Summary card elements
*/

const totalExpensesElement = document.getElementById(
    "total-expenses"
);
const pendingExpensesElement = document.getElementById(
    "pending-expenses"
);
const approvedExpensesElement = document.getElementById(
    "approved-expenses"
);
const deniedExpensesElement = document.getElementById(
    "denied-expenses"
);

/*
    Table and filter elements
*/

const statusFilter = document.getElementById(
    "status-filter"
);
const pendingOnlyFilter = document.getElementById(
    "pending-only-filter"
);
const expensesTableBody = document.getElementById(
    "expenses-table-body"
);
const expensesTableContainer = document.querySelector(
    ".table-container"
);
const emptyExpensesElement = document.getElementById(
    "empty-expenses"
);

/*
    Application state
*/

let currentEmployee = null;
let allExpenses = [];


/*
    View helpers
*/

function showOnly(view) {
    loadingView.classList.add("hidden");
    loginView.classList.add("hidden");
    dashboardView.classList.add("hidden");

    view.classList.remove("hidden");
}


function showLoginView() {
    currentEmployee = null;
    allExpenses = [];

    closeEditModal();
    showOnly(loginView);

    passwordInput.value = "";

    updateSummaryCards();
    renderExpenses([]);

    clearMessage(loginMessage);
    clearMessage(expenseMessage);
    clearMessage(tableMessage);
}


function showDashboardView(employee) {
    currentEmployee = employee;

    const username = employee?.username || "Employee";

    welcomeMessage.textContent = `Welcome, ${username}`;
    dashboardTitle.textContent = `Welcome, ${username}`;

    showOnly(dashboardView);
}


/*
    Message helpers
*/

function setMessage(element, message, type = "error") {
    element.textContent = message;
    element.className = `message ${type}`;
}


function clearMessage(element) {
    element.textContent = "";
    element.className = "message";
}


/*
    Button helper
*/

function setButtonLoading(
    button,
    isLoading,
    normalText,
    loadingText
) {
    button.disabled = isLoading;

    button.textContent = isLoading
        ? loadingText
        : normalText;
}


/*
    Response helper
*/

async function readJsonResponse(response) {
    try {
        return await response.json();
    } catch {
        return {
            status: "error",
            message: "The server returned an invalid response."
        };
    }
}


/*
    Session
*/

async function checkCurrentSession() {
    try {
        const response = await fetch("/api/session", {
            method: "GET",
            credentials: "include"
        });

        const data = await readJsonResponse(response);

        if (!response.ok) {
            showLoginView();
            return;
        }

        showDashboardView(data.employee);
        await loadExpenses();

    } catch (error) {
        console.error("Session check failed:", error);

        showLoginView();

        setMessage(
            loginMessage,
            "Unable to connect to the server."
        );
    }
}


/*
    Login
*/

async function handleLogin(event) {
    event.preventDefault();
    clearMessage(loginMessage);

    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    if (!username) {
        setMessage(
            loginMessage,
            "Username is required."
        );

        usernameInput.focus();
        return;
    }

    if (!password) {
        setMessage(
            loginMessage,
            "Password is required."
        );

        passwordInput.focus();
        return;
    }

    setButtonLoading(
        loginButton,
        true,
        "Sign In",
        "Signing In..."
    );

    try {
        const response = await fetch("/api/login", {
            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            credentials: "include",

            body: JSON.stringify({
                username,
                password
            })
        });

        const data = await readJsonResponse(response);

        if (!response.ok) {
            setMessage(
                loginMessage,
                data.message || "Login failed."
            );

            return;
        }

        loginForm.reset();

        showDashboardView(data.employee);
        await loadExpenses();

    } catch (error) {
        console.error("Login failed:", error);

        setMessage(
            loginMessage,
            "Unable to connect to the server."
        );

    } finally {
        setButtonLoading(
            loginButton,
            false,
            "Sign In",
            "Signing In..."
        );
    }
}


/*
    Logout
*/

async function handleLogout() {
    setButtonLoading(
        logoutButton,
        true,
        "Logout",
        "Logging Out..."
    );

    try {
        const response = await fetch("/api/logout", {
            method: "POST",
            credentials: "include"
        });

        const data = await readJsonResponse(response);

        if (!response.ok && response.status !== 401) {
            window.alert(
                data.message || "Logout failed."
            );

            return;
        }

        expenseForm.reset();
        showLoginView();

    } catch (error) {
        console.error("Logout failed:", error);

        window.alert(
            "Unable to connect to the server."
        );

    } finally {
        setButtonLoading(
            logoutButton,
            false,
            "Logout",
            "Logging Out..."
        );
    }
}


/*
    Submit a new expense
*/

async function handleExpenseSubmit(event) {
    event.preventDefault();
    clearMessage(expenseMessage);

    const amount = Number(amountInput.value);
    const description =
        descriptionInput.value.trim();
    const category = categoryInput.value;

    if (!Number.isFinite(amount)) {
        setMessage(
            expenseMessage,
            "Enter a valid expense amount."
        );

        amountInput.focus();
        return;
    }

    if (amount < 1 || amount > 10000) {
        setMessage(
            expenseMessage,
            "Amount must be between $1.00 and $10,000.00."
        );

        amountInput.focus();
        return;
    }

    if (!description) {
        setMessage(
            expenseMessage,
            "Description is required."
        );

        descriptionInput.focus();
        return;
    }

    if (!category) {
        setMessage(
            expenseMessage,
            "Select an expense category."
        );

        categoryInput.focus();
        return;
    }

    setButtonLoading(
        submitExpenseButton,
        true,
        "Submit Expense",
        "Submitting..."
    );

    try {
        const response = await fetch("/api/expenses", {
            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            credentials: "include",

            body: JSON.stringify({
                amount,
                description,
                category
            })
        });

        const data = await readJsonResponse(response);

        if (response.status === 401) {
            showLoginView();

            setMessage(
                loginMessage,
                "Your session expired. Please sign in again."
            );

            return;
        }

        if (!response.ok) {
            setMessage(
                expenseMessage,
                data.message ||
                    "Expense submission failed."
            );

            return;
        }

        expenseForm.reset();

        setMessage(
            expenseMessage,
            data.message ||
                "Expense submitted successfully.",
            "success"
        );

        await loadExpenses();

    } catch (error) {
        console.error(
            "Expense submission failed:",
            error
        );

        setMessage(
            expenseMessage,
            "Unable to connect to the server."
        );

    } finally {
        setButtonLoading(
            submitExpenseButton,
            false,
            "Submit Expense",
            "Submitting..."
        );
    }
}


/*
    Load expenses
*/

async function loadExpenses() {
    clearMessage(tableMessage);

    refreshButton.disabled = true;
    refreshButton.textContent = "Refreshing...";

    try {
        const response = await fetch("/api/expenses", {
            method: "GET",
            credentials: "include"
        });

        const data = await readJsonResponse(response);

        if (response.status === 401) {
            showLoginView();

            setMessage(
                loginMessage,
                "Your session expired. Please sign in again."
            );

            return;
        }

        if (!response.ok) {
            setMessage(
                tableMessage,
                data.message ||
                    "Unable to load expenses."
            );

            return;
        }

        allExpenses = Array.isArray(data.expenses)
            ? data.expenses
            : [];

        updateSummaryCards();
        renderFilteredExpenses();

    } catch (error) {
        console.error(
            "Expense loading failed:",
            error
        );

        setMessage(
            tableMessage,
            "Unable to connect to the server."
        );

    } finally {
        refreshButton.disabled = false;
        refreshButton.textContent = "Refresh Expenses";
    }
}


/*
    Summary cards
*/

function updateSummaryCards() {
    const pendingCount = allExpenses.filter(
        expense =>
            normalizeStatus(expense.status) === "pending"
    ).length;

    const approvedCount = allExpenses.filter(
        expense =>
            normalizeStatus(expense.status) === "approved"
    ).length;

    const deniedCount = allExpenses.filter(
        expense =>
            normalizeStatus(expense.status) === "denied"
    ).length;

    totalExpensesElement.textContent =
        allExpenses.length;

    pendingExpensesElement.textContent =
        pendingCount;

    approvedExpensesElement.textContent =
        approvedCount;

    deniedExpensesElement.textContent =
        deniedCount;
}


/*
    Filtering
*/

function renderFilteredExpenses() {
    const selectedStatus = statusFilter.value;
    const pendingOnly = pendingOnlyFilter.checked;

    let filteredExpenses = [...allExpenses];

    if (pendingOnly) {
        filteredExpenses = filteredExpenses.filter(
            expense =>
                normalizeStatus(expense.status) ===
                "pending"
        );

    } else if (selectedStatus !== "all") {
        filteredExpenses = filteredExpenses.filter(
            expense =>
                normalizeStatus(expense.status) ===
                selectedStatus
        );
    }

    renderExpenses(filteredExpenses);
}


function handlePendingOnlyChange() {
    statusFilter.disabled =
        pendingOnlyFilter.checked;

    if (pendingOnlyFilter.checked) {
        statusFilter.value = "all";
    }

    renderFilteredExpenses();
}


/*
    Render expense table
*/

function renderExpenses(expenses) {
    expensesTableBody.replaceChildren();

    if (expenses.length === 0) {
        expensesTableContainer.classList.add(
            "hidden"
        );

        emptyExpensesElement.classList.remove(
            "hidden"
        );

        return;
    }

    expensesTableContainer.classList.remove(
        "hidden"
    );

    emptyExpensesElement.classList.add(
        "hidden"
    );

    for (const expense of expenses) {
        expensesTableBody.appendChild(
            createExpenseRow(expense)
        );
    }
}


function createExpenseRow(expense) {
    const row = document.createElement("tr");

    row.appendChild(
        createTableCell(expense.id ?? "—")
    );

    row.appendChild(
        createTableCell(expense.date || "—")
    );

    const descriptionCell = createTableCell(
        expense.description || "—"
    );

    descriptionCell.classList.add(
        "description-cell"
    );

    row.appendChild(descriptionCell);

    row.appendChild(
        createTableCell(
            expense.category || "Other"
        )
    );

    const amountCell = createTableCell(
        formatCurrency(expense.amount)
    );

    amountCell.classList.add("amount-cell");

    row.appendChild(amountCell);

    const status = normalizeStatus(
        expense.status
    );

    const statusCell =
        document.createElement("td");

    const statusBadge =
        document.createElement("span");

    statusBadge.textContent =
        status || "unknown";

    statusBadge.className =
        `status-badge status-${status || "unknown"}`;

    statusCell.appendChild(statusBadge);
    row.appendChild(statusCell);

    const actionsCell =
        document.createElement("td");

    actionsCell.classList.add(
        "actions-cell"
    );

    if (status === "pending") {
        const actionsContainer =
            document.createElement("div");

        actionsContainer.classList.add(
            "row-actions"
        );

        const editButton =
            document.createElement("button");

        editButton.type = "button";
        editButton.textContent = "Edit";
        editButton.className = "edit-button";

        editButton.addEventListener(
            "click",
            () => {
                openEditModal(expense);
            }
        );

        const deleteButton =
            document.createElement("button");

        deleteButton.type = "button";
        deleteButton.textContent = "Delete";
        deleteButton.className = "delete-button";

        deleteButton.addEventListener(
            "click",
            () => {
                deleteExpense(expense);
            }
        );

        actionsContainer.append(
            editButton,
            deleteButton
        );

        actionsCell.appendChild(
            actionsContainer
        );

    } else {
        const readOnlyText =
            document.createElement("span");

        readOnlyText.textContent = "Read only";
        readOnlyText.className =
            "read-only-text";

        actionsCell.appendChild(readOnlyText);
    }

    row.appendChild(actionsCell);

    return row;
}


function createTableCell(value) {
    const cell = document.createElement("td");

    cell.textContent = String(value);

    return cell;
}


/*
    Edit expense modal
*/

function openEditModal(expense) {
    clearMessage(editExpenseMessage);

    editExpenseIdInput.value =
        expense.id;

    editExpenseAmountInput.value =
        expense.amount;

    editExpenseDescriptionInput.value =
        expense.description || "";

    editExpenseCategoryInput.value =
        expense.category || "";

    editModal.classList.remove("hidden");

    document.body.classList.add(
        "modal-open"
    );

    editExpenseAmountInput.focus();
}


function closeEditModal() {
    editModal.classList.add("hidden");

    document.body.classList.remove(
        "modal-open"
    );

    editExpenseForm.reset();
    editExpenseIdInput.value = "";

    clearMessage(editExpenseMessage);
}


/*
    Update expense
*/

async function handleEditExpense(event) {
    event.preventDefault();

    clearMessage(editExpenseMessage);

    const expenseId =
        editExpenseIdInput.value;

    const amount = Number(
        editExpenseAmountInput.value
    );

    const description =
        editExpenseDescriptionInput.value.trim();

    const category =
        editExpenseCategoryInput.value;

    if (!expenseId) {
        setMessage(
            editExpenseMessage,
            "The expense ID is missing."
        );

        return;
    }

    if (!Number.isFinite(amount)) {
        setMessage(
            editExpenseMessage,
            "Enter a valid expense amount."
        );

        editExpenseAmountInput.focus();
        return;
    }

    if (amount < 1 || amount > 10000) {
        setMessage(
            editExpenseMessage,
            "Amount must be between $1.00 and $10,000.00."
        );

        editExpenseAmountInput.focus();
        return;
    }

    if (!description) {
        setMessage(
            editExpenseMessage,
            "Description is required."
        );

        editExpenseDescriptionInput.focus();
        return;
    }

    if (!category) {
        setMessage(
            editExpenseMessage,
            "Select an expense category."
        );

        editExpenseCategoryInput.focus();
        return;
    }

    setButtonLoading(
        saveEditButton,
        true,
        "Save Changes",
        "Saving..."
    );

    try {
        const response = await fetch(
            `/api/expenses/${expenseId}`,
            {
                method: "PUT",

                headers: {
                    "Content-Type":
                        "application/json"
                },

                credentials: "include",

                body: JSON.stringify({
                    amount,
                    description,
                    category
                })
            }
        );

        const data =
            await readJsonResponse(response);

        if (response.status === 401) {
            closeEditModal();
            showLoginView();

            setMessage(
                loginMessage,
                "Your session expired. Please sign in again."
            );

            return;
        }

        if (!response.ok) {
            setMessage(
                editExpenseMessage,
                data.message ||
                    "Unable to update the expense."
            );

            return;
        }

        closeEditModal();

        await loadExpenses();

        setMessage(
            tableMessage,
            data.message ||
                "Expense updated successfully.",
            "success"
        );

    } catch (error) {
        console.error(
            "Expense update failed:",
            error
        );

        setMessage(
            editExpenseMessage,
            "Unable to connect to the server."
        );

    } finally {
        setButtonLoading(
            saveEditButton,
            false,
            "Save Changes",
            "Saving..."
        );
    }
}


/*
    Delete expense
*/

async function deleteExpense(expense) {
    const description =
        expense.description || "this expense";

    const confirmed = window.confirm(
        `Delete "${description}"?\n\n` +
        "This action cannot be undone."
    );

    if (!confirmed) {
        return;
    }

    clearMessage(tableMessage);

    try {
        const response = await fetch(
            `/api/expenses/${expense.id}`,
            {
                method: "DELETE",
                credentials: "include"
            }
        );

        const data =
            await readJsonResponse(response);

        if (response.status === 401) {
            showLoginView();

            setMessage(
                loginMessage,
                "Your session expired. Please sign in again."
            );

            return;
        }

        if (!response.ok) {
            setMessage(
                tableMessage,
                data.message ||
                    "Unable to delete the expense."
            );

            return;
        }

        await loadExpenses();

        setMessage(
            tableMessage,
            data.message ||
                "Expense deleted successfully.",
            "success"
        );

    } catch (error) {
        console.error(
            "Expense deletion failed:",
            error
        );

        setMessage(
            tableMessage,
            "Unable to connect to the server."
        );
    }
}


/*
    Utility functions
*/

function normalizeStatus(status) {
    return String(status || "")
        .trim()
        .toLowerCase();
}


function formatCurrency(value) {
    const amount = Number(value);

    if (!Number.isFinite(amount)) {
        return "$0.00";
    }

    return new Intl.NumberFormat(
        "en-US",
        {
            style: "currency",
            currency: "USD"
        }
    ).format(amount);
}


/*
    Event listeners
*/

loginForm.addEventListener(
    "submit",
    handleLogin
);

expenseForm.addEventListener(
    "submit",
    handleExpenseSubmit
);

editExpenseForm.addEventListener(
    "submit",
    handleEditExpense
);

logoutButton.addEventListener(
    "click",
    handleLogout
);

refreshButton.addEventListener(
    "click",
    loadExpenses
);

statusFilter.addEventListener(
    "change",
    renderFilteredExpenses
);

pendingOnlyFilter.addEventListener(
    "change",
    handlePendingOnlyChange
);

closeEditModalButton.addEventListener(
    "click",
    closeEditModal
);

cancelEditButton.addEventListener(
    "click",
    closeEditModal
);

editModal.addEventListener(
    "click",
    event => {
        if (event.target === editModal) {
            closeEditModal();
        }
    }
);

document.addEventListener(
    "keydown",
    event => {
        if (
            event.key === "Escape" &&
            !editModal.classList.contains("hidden")
        ) {
            closeEditModal();
        }
    }
);

document.addEventListener(
    "DOMContentLoaded",
    checkCurrentSession
);