// Manager Expense Dashboard JavaScript

class ManagerDashboard {
    constructor() {
        this.currentUser = null;
        this.currentExpenseId = null;
        this.init();
    }

    init() {
        // Check authentication first
        this.checkAuthStatus();
    }

    async checkAuthStatus() {
        try {
            console.log('=== Manager Auth Check Started ===');
            
            // Verify auth status via cookies (httpOnly cookie is automatically sent)
            console.log('Making request to /api/auth/status');
            const response = await fetch('/api/auth/status', {
                credentials: 'same-origin' // Include cookies in request
            });
            console.log('Auth status response received:', response.status);
            const data = await response.json();
            console.log('Auth status data:', data);

            if (data.authenticated && data.user.role.toLowerCase() === 'manager') {
                this.currentUser = data.user;
                this.initializeApp();
            } else {
                // Not authenticated or not a manager, redirect to login
                console.log('Not authenticated as manager, redirecting to login');
                window.location.href = '/login.html';
            }
        } catch (error) {
            console.error('Auth check failed:', error);
            window.location.href = 'http://localhost:5000/login';
        }
    }

    getAuthHeaders() {
        // With HTTP-only cookies, authentication is handled automatically
        // No need to manually add Authorization header
        return {
            'Content-Type': 'application/json'
        };
    }
    
    // Get fetch options that include credentials for cookie-based auth
    getFetchOptions(options = {}) {
        return {
            ...options,
            credentials: 'include', // Include cookies in request
            headers: {
                ...this.getAuthHeaders(),
                ...options.headers
            }
        };
    }

    initializeApp() {
        // Hide loading section and show main content
        document.getElementById('loading-section').style.display = 'none';
        document.getElementById('header').style.display = 'block';
        document.getElementById('navigation').style.display = 'block';
        
        // Display username
        document.getElementById('username-display').textContent = this.currentUser.username;
        
        // Set up event listeners
        this.setupEventListeners();
        
        // Show pending expenses by default
        this.showPendingExpensesSection();
    }

    setupEventListeners() {
        // Logout button
        document.getElementById('logout-btn').addEventListener('click', () => {
            this.logout();
        });

        // Navigation buttons
        document.getElementById('show-pending').addEventListener('click', () => {
            this.showPendingExpensesSection();
        });

        document.getElementById('show-all-expenses').addEventListener('click', () => {
            this.showAllExpensesSection();
        });

        document.getElementById('show-reports').addEventListener('click', () => {
            this.showSection('reports-section');
        });

        // Refresh buttons
        document.getElementById('refresh-pending').addEventListener('click', () => {
            this.loadPendingExpenses();
        });

        document.getElementById('refresh-all-expenses').addEventListener('click', () => {
            this.loadAllExpenses();
        });

        // Filter buttons
        document.getElementById('filter-by-employee').addEventListener('click', () => {
            const employeeId = document.getElementById('employee-filter').value;
            if (employeeId) {
                this.loadExpensesByEmployee(employeeId);
            }
        });

        document.getElementById('clear-employee-filter').addEventListener('click', () => {
            document.getElementById('employee-filter').value = '';
            this.loadAllExpenses();
        });

        // Report generation buttons
        document.getElementById('generate-all-expenses-report').addEventListener('click', () => {
            this.generateReport('/api/reports/expenses/csv', 'all_expenses_report.csv');
        });

        document.getElementById('generate-pending-report').addEventListener('click', () => {
            this.generateReport('/api/reports/expenses/pending/csv', 'pending_expenses_report.csv');
        });

        document.getElementById('generate-employee-report').addEventListener('click', () => {
            const empUsername = document.getElementById('employee-report-name').value;
            if (empUsername) {
                this.generateReport(`/api/reports/expenses/employee/${empUsername}/csv`, `employee_${empUsername}_report.csv`);
            } else {
                this.showReportMessage('Please enter an employee Name', 'error');
            }
        });

        document.getElementById('generate-category-report').addEventListener('click', () => {
            const category = document.getElementById('category-report').value.trim();
            if (category) {
                this.generateReport(`/api/reports/expenses/category/${encodeURIComponent(category)}/csv`, `category_${category}_report.csv`);
            } else {
                this.showReportMessage('Please enter a category', 'error');
            }
        });

        document.getElementById('generate-date-range-report').addEventListener('click', () => {
            const startDate = document.getElementById('start-date').value;
            const endDate = document.getElementById('end-date').value;
            if (startDate && endDate) {
                this.generateReport(`/api/reports/expenses/daterange/csv?startDate=${startDate}&endDate=${endDate}`, `expenses_${startDate}_to_${endDate}_report.csv`);
            } else {
                this.showReportMessage('Please select both start and end dates', 'error');
            }
        });

        // Review modal buttons
        document.getElementById('approve-expense').addEventListener('click', () => {
            this.approveExpense();
        });

        document.getElementById('deny-expense').addEventListener('click', () => {
            this.denyExpense();
        });

        document.getElementById('cancel-review').addEventListener('click', () => {
            this.closeReviewModal();
        });

        // Close modal when clicking outside
        document.getElementById('review-modal').addEventListener('click', (e) => {
            if (e.target.id === 'review-modal') {
                this.closeReviewModal();
            }
        });
    }

    async logout() {
        try {
            // Call logout endpoint to clear HTTP-only cookie
            const response = await fetch('/api/auth/logout', this.getFetchOptions({
                method: 'POST'
            }));
            
            if (response.ok) {
                console.log('Logout successful');
            }
        } catch (error) {
            console.error('Logout request failed:', error);
        } finally {
            // Always redirect to login page regardless of logout API success
            window.location.href = '/login.html';
        }
    }

    async loadPendingExpenses() {
        try {
            const response = await fetch('/api/expenses/pending', this.getFetchOptions());
            console.log('Pending expenses response received:', response);
            const data = await response.json();
            console.log('Pending expenses data:', data);

            if (data && data.success) {
                // Map server shape { expense, user, approval } -> flat shape expected by displayPendingExpenses
                const expenses = Array.isArray(data.data) ? data.data.map(item => {
                    const e = item || {};
                    //const u = item.user || {};
                    //const a = item.approval || {};
                    return {
                        id: e.expense_id,
                        empName: e.emp_name,
                        amount: e.amount ?? 0,
                        description: e.description ?? '',
                        date: e.exp_date ?? '',
                        category: e.category,
                        // include approval fields if you want to reuse displayAllExpenses later
                        status: e.status,
                    };
                }) : [];

                this.displayPendingExpenses(expenses);
            } else {
                this.showMessage('pending-expenses-list', data.error || 'Failed to load pending expenses', 'error');
            }
        } catch (error) {
            this.showMessage('pending-expenses-list', error.name, 'error');
        }
    }

    async loadAllExpenses() {
        try {
            const response = await fetch('/api/expenses', this.getFetchOptions());
            const data = await response.json();

            if (data && data.success) {
                // Map server shape { expense, user, approval } -> flat shape expected by displayAllExpenses
                /*const expenses = Array.isArray(data.data) ? data.data.map(item => {
                    const e = item.expense || {};
                    const u = item.user || {};
                    const a = item.approval || {};
                    return {
                        id: e.id,
                        userId: e.userId ?? u.id,
                        username: u.username ?? 'Unknown',
                        amount: e.amount ?? 0,
                        description: e.description ?? '',
                        date: e.date ?? '',
                        status: a.status,
                        reviewerUsername: a.reviewer,
                        comment: a.comment
                    };
                }) : [];*/

                const expenses = Array.isArray(data.data) ? data.data.map(item => {
                    const e = item || {};
                    return {
                        id: e.expense_id,
                        username: e.emp_name ?? 'Unknown',
                        amount: e.amount ?? 0,
                        description: e.description ?? '',
                        date: e.exp_date ?? '',
                        category: e.category,//May need to cast to string
                        status: e.status,
                        reviewerUsername: e.reviewer_name,
                        comment: e.comment,
                        review_date: e.review_date
                    };
                }) : [];

                this.displayAllExpenses(expenses);
            } else {
                this.showMessage('all-expenses-list', data.error || 'Failed to load expenses', 'error');
            }
        } catch (error) {
            this.showMessage('all-expenses-list', 'Network error. Please try again.', 'error');
        }
    }

    async loadExpensesByEmployee(empUsername) { // Change to empName
        try {
            const response = await fetch(`/api/expenses/employee/${empUsername}`, this.getFetchOptions()); // Change to empName
            const data = await response.json();

            if (data && data.success) {
                const expenses = Array.isArray(data.data) ? data.data.map(item => {
                    const e = item || {};
                    return {
                        id: e.expense_id,
                        username: e.emp_name ?? 'Unknown',
                        amount: e.amount ?? 0,
                        description: e.description ?? '',
                        date: e.exp_date ?? '',
                        category: e.category,
                        status: e.status,
                        reviewerUsername: e.reviewer_name,
                        comment: e.comment,
                        review_date: e.review_date
                    };
                }) : [];

                this.displayAllExpenses(expenses, `Employee '${empUsername}' Expenses`);
            } else {
                this.showMessage('all-expenses-list', data.error || 'Failed to load employee expenses', 'error');
            }
        } catch (error) {
            this.showMessage('all-expenses-list', 'Network error. Please try again.', 'error');
        }
    }

    displayPendingExpenses(expenses) {
        const container = document.getElementById('pending-expenses-list');
        
        if (expenses.length === 0) {
            container.innerHTML = '<p>No pending expenses found.</p>';
            return;
        }

        let html = '<table border="1" cellpadding="8" cellspacing="0" width="100%">';
        html += '<tr style="background-color: #f0f0f0;"><th>Expense Id</th><th>Employee</th><th>Description</th><th>Date</th><th>Amount</th><th>Category</th><th>Status</th><th>Actions</th></tr>';

        expenses.forEach(expense => {
            html += `<tr>
                <td>${expense.id}</td>
                <td>${expense.empName}</td>
                <td>${expense.description}</td>
                <td>${expense.date}</td>
                <td>$${expense.amount.toFixed(2)}</td>
                <td>${expense.category}</td>
                <td style="color: orange; font-weight: bold;">${expense.status}</td>
                <td>
                    <button onclick="managerDashboard.reviewExpense(${expense.id}, '${expense.empName}', '${expense.date}', ${expense.amount}, '${expense.category}', '${expense.description}')" 
                            style="background-color: #007bff; color: white;">Review</button>
                </td>
            </tr>`;
        });

        html += '</table>';
        container.innerHTML = html;
    }

    displayAllExpenses(expenses, title = 'All Expenses') {
        const container = document.getElementById('all-expenses-list');
        
        if (expenses.length === 0) {
            container.innerHTML = '<p>No expenses found.</p>';
            return;
        }

        let html = `<h4>${title}</h4>`;
        html += '<table border="1" cellpadding="8" cellspacing="0" width="100%">';
        html += '<tr style="background-color: #f0f0f0;"><th>Expense Id</th><th>Employee</th><th>Description</th><th>Date</th><th>Amount</th><th>Category</th><th>Status</th><th>Reviewer</th><th>Comment</th><th>Review Date</th></tr>';

        expenses.forEach(expense => {
            const statusColor = expense.status === 'approved' ? 'green' : 
                              expense.status === 'denied' ? 'red' : 'orange';
            
            html += `<tr>
                <td>${expense.id}</td>
                <td>${expense.username}</td>
                <td>${expense.description}</td>
                <td>${expense.date}</td>
                <td>$${expense.amount.toFixed(2)}</td>
                <td>${expense.category}</td>
                <td style="color: ${statusColor}; font-weight: bold;">${expense.status.toUpperCase()}</td>
                <td>${expense.reviewerUsername || '-'}</td>
                <td>${expense.comment || '-'}</td>
                <td>${expense.review_date}</td>
            </tr>`;
        });

        html += '</table>';
        container.innerHTML = html;
    }

    reviewExpense(expenseId, username, date, amount, category, description) {
        this.currentExpenseId = expenseId;
        
        // Populate expense details in modal
        const detailsHtml = `
            <p><strong>Employee:</strong> ${username}</p>
            <p><strong>Date:</strong> ${date}</p>
            <p><strong>Amount:</strong> $${amount.toFixed(2)}</p>
            <p><strong>Category:</strong> ${category}</p>
            <p><strong>Description:</strong> ${description}</p>
        `;
        
        document.getElementById('expense-details').innerHTML = detailsHtml;
        document.getElementById('review-comment').value = '';
        document.getElementById('review-message').innerHTML = '';
        
        // Show modal
        document.getElementById('review-modal').style.display = 'block';
    }

    async approveExpense() {
        const comment = document.getElementById('review-comment').value.trim();
        
        try {
            const response = await fetch(`/api/expenses/${this.currentExpenseId}/approve`, this.getFetchOptions({
                method: 'POST',
                body: JSON.stringify({ comment: comment || null }),
            }));

            const data = await response.json();

            if (data.success) {
                this.showReviewMessage('Expense approved successfully!', 'success');
                setTimeout(() => {
                    this.closeReviewModal();
                    this.loadPendingExpenses();
                }, 1500);
            } else {
                this.showReviewMessage(data.error || 'Failed to approve expense', 'error');
            }
        } catch (error) {
            this.showReviewMessage('Network error. Please try again.', 'error');
        }
    }

    async denyExpense() {
        const comment = document.getElementById('review-comment').value.trim();
        
        try {
            const response = await fetch(`/api/expenses/${this.currentExpenseId}/deny`, this.getFetchOptions({
                method: 'POST',
                body: JSON.stringify({ comment: comment || null }),
            }));

            const data = await response.json();

            if (data.success) {
                this.showReviewMessage('Expense denied successfully!', 'success');
                setTimeout(() => {
                    this.closeReviewModal();
                    this.loadPendingExpenses();
                }, 1500);
            } else {
                this.showReviewMessage(data.error || 'Failed to deny expense', 'error');
            }
        } catch (error) {
            this.showReviewMessage('Network error. Please try again.', 'error');
        }
    }

    closeReviewModal() {
        document.getElementById('review-modal').style.display = 'none';
        this.currentExpenseId = null;
    }

    async generateReport(url, filename) {
        try {
            const response = await fetch(url, this.getFetchOptions());
            
            console.log("=== REPORT DEBUG ===");
            console.log("URL:", url);
            console.log("Status:", response.status);
            console.log("OK:", response.ok);
            console.log("Content-Type:", response.headers.get("content-type"));

            if (response.ok) {
                const blob = await response.blob();
                
                console.log("Blob size:", blob.size);
                console.log("Blob type:", blob.type);

                // Create download link
                const downloadUrl = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = downloadUrl;
                a.download = filename;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(downloadUrl);
                
                this.showReportMessage('Report generated successfully!', 'success');
            } else {
                const data = await response.json();

                console.error("REPORT REQUEST FAILED");
                console.error("Status:", response.status);
                console.error("Response:", data);

                this.showReportMessage(data.error || 'Failed to generate report', 'error');
            }
        } catch (error) {
            console.error(error);
            this.showReportMessage('Network error. Please try again.', 'error');
        }
    }

    showPendingExpensesSection() {
        this.showSection('pending-expenses-section');
        this.loadPendingExpenses();
    }

    showAllExpensesSection() {
        this.showSection('all-expenses-section');
        this.loadAllExpenses();
    }

    showSection(sectionId) {
        this.hideAllSections();
        document.getElementById(sectionId).style.display = 'block';
    }

    hideAllSections() {
        const sections = [
            'pending-expenses-section',
            'all-expenses-section', 
            'reports-section'
        ];
        
        sections.forEach(id => {
            document.getElementById(id).style.display = 'none';
        });
    }

    showMessage(elementId, message, type) {
        const element = document.getElementById(elementId);
        element.innerHTML = `<p style="color: ${type === 'error' ? 'red' : 'green'}; font-weight: bold;">${message}</p>`;
        
        // Clear message after 5 seconds
        setTimeout(() => {
            element.innerHTML = '';
        }, 5000);
    }

    showReportMessage(message, type) {
        this.showMessage('report-message', message, type);
    }

    showReviewMessage(message, type) {
        this.showMessage('review-message', message, type);
    }
}

// Initialize the app when page loads
const managerDashboard = new ManagerDashboard();