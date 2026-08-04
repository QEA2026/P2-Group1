To run manager app:

mvn clean package (from expense-app-managers directory)
mvn exec:java -D exec.mainClass="com.rev.manager.Main"

All functions work with the JDBCManagerDAO querying and updating database.

API CALLS
- All API calls (except for login services) require auth token
- Errors will return a json with error code 500 in format
- For test, feel free to assert different error codes and messages where appropriate.
{
    "success", false,
    "error", "Internal server error",
    "message", e.getMessage()
}

----------LOGIN/LOGOUT----------
POST /api/auth/login
Request Body:
{"username": "username","password": "password"}
On missing (null) username or password: 400 {"success": false, "error": "Username and password required"}
On invalid username or password: 401 {"success": false, "error": "Invalid credentials or user is not a manager"}
On success, creation of jwtToken (jwt set to jwtToken) as a cookie: 200 {"success": true, "message": "Login successful",
"user":{"id": id, "username": "username", "role": "role"}
}
POST /api/auth/logout
Removes jwt cookie
{"success": true, "message": "Logged out successfully"}


----------EXPENSE MANAGEMENT----------

GET /api/expenses (view all expenses)
{
    "success": true,
    "data": [List of expenses],
    "count": allExpenses.size()
}

GET /api/expenses/pending (view pending expenses)
{
    "success": true,
    "data": [List of pending expenses],
    "count": pendingExpenses.size()
}

GET /api/expenses/employee/{empUsername} (view expenses from an employee)
{
    "success": true,
    "data": [List of expenses from employee]
    "count": expenses.size(),
    "empUsername": empUsername
}

POST /api/expenses/{expenseId}/approve (approve an expense)
Request body: { "comment": "optional comment" }
{
    "success": true,
    "message": "expense approved successfully"
}

POST /api/expenses/{expenseId}/deny (deny an expense)
Request body: { "comment": "optional comment" }
{
    "success": true,
    "message": "expense approved successfully"
}

----------REPORT GENERATION----------

contentType is text/csv
Result is set to a string, which is csv text data,
"Expense ID,Employee,Amount,Description,Date,Category,Status,Reviewer,Comment,Review Date\n"
followed by a line of data from each expense (for pending expenses, nothing will be put in Reviewer, Comment, or Review Date)

Ex:
Expense ID,Employee,Amount,Description,Date,Category,Status,Reviewer,Comment,Review Date\n
28,Bob,29.99,AWS Certification,7/25/2026,Certifications,approved,Andrew,"One certificate write off",7/26/2026\n
42,Tommy,44.55,Lunch in Paris,7/26/2026,Meals,pending,,,\n


GET /api/reports/expenses/csv (Report all expenses)
GET /api/reports/expenses/pending/csv (Report only pending expenses)
GET /api/reports/expenses/employee/{empUsername}/csv (Report expenses from an employee, or error if emp not found)
GET /api/reports/expenses/category/{category}/csv (Report expenses from a particular category, or other on invalid category)
GET /api/reports/expenses/daterange/csv?startDate=YYYY/MM/DD&endDate=YYYY/MM/DD (Report expenses between start and end dates, inclusive. Empty csv is returned if no reports are found in the date range)