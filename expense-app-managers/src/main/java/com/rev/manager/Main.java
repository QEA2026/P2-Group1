package com.rev.manager;

import com.rev.manager.api.AuthenticationMiddleware;
import com.rev.manager.api.ExpenseController;
import com.rev.manager.api.ReportController;
import com.rev.manager.repository.DatabaseConnection;
import com.rev.manager.repository.UserRepository;
import com.rev.manager.repository.ExpenseRepository;
import com.rev.manager.repository.User;
import com.rev.manager.repository.ApprovalRepository;
import com.rev.manager.service.AuthenticationService;
import com.rev.manager.service.ExpenseService;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.sql.SQLException;
//import java.sql.SQLException;
import java.util.Map;

import com.rev.manager.DAO.JDBCManagerDAO;

/**
 * Main application class for the Revature Expense Manager (Manager App).
 * Sets up dependency injection and configures Javalin web server with REST endpoints.
 */
public class Main {
    private static final int PORT = 5001;
    
    public static void main(String[] args) {
        // Initialize dependencies using constructor dependency injection
        DatabaseConnection databaseConnection;
        if(args.length == 0)
            databaseConnection = new DatabaseConnection();
        else
            databaseConnection = new DatabaseConnection(args[0]);
        
        // Repository layer
        UserRepository userRepository = new UserRepository(databaseConnection);
        ExpenseRepository expenseRepository = new ExpenseRepository(databaseConnection);
        ApprovalRepository approvalRepository = new ApprovalRepository(databaseConnection);
        
        // Service layer
        JDBCManagerDAO jdbcManager = null;
        try {jdbcManager = new JDBCManagerDAO(databaseConnection.getConnection());}
        catch (SQLException e) {
            System.err.println("Error with your database, please check connection.");
            e.printStackTrace();
            System.exit(1);
        }
        AuthenticationService authenticationService = new AuthenticationService(userRepository);
        ExpenseService expenseService = new ExpenseService(expenseRepository, approvalRepository);
        
        // API layer
        AuthenticationMiddleware authMiddleware = new AuthenticationMiddleware(authenticationService);
        ExpenseController expenseController = new ExpenseController(expenseService, jdbcManager);
        ReportController reportController = new ReportController(expenseService, jdbcManager);

        
        // Configure and start Javalin application
        Javalin app = Javalin.create(config -> {
            // Enable CORS for cross-origin requests from frontend
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowHost("http://127.0.0.1:5000");
                    it.allowHost("http://localhost:5000");
                    it.allowCredentials = true;
                });
            });
            
            // Enable static file serving from resources (serve from classpath:/public)
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
            
            // Enable request logging
            config.bundledPlugins.enableDevLogging();
        });
        
        // Global exception handling
        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.json(java.util.Map.of(
                "success", false,
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        });
        
        // Root redirect to manager dashboard
        app.get("/", ctx -> ctx.redirect("/manager.html"));
        
        // Authentication status endpoint (no auth required)
        app.get("/api/auth/status", ctx -> {
            String jwtToken = ctx.cookie("jwt");
            
            java.util.Optional<com.rev.manager.repository.User> managerOpt = authenticationService.validateManagerAuthentication(jwtToken);
            
            if (managerOpt.isPresent()) {
                com.rev.manager.repository.User manager = managerOpt.get();
                ctx.json(java.util.Map.of(
                    "authenticated", true,
                    "user", java.util.Map.of(
                        "id", manager.getId(),
                        "username", manager.getUsername(),
                        "role", manager.getRole()
                    )
                ));
            } else {
                ctx.json(java.util.Map.of("authenticated", false));
            }
        });
        
        // Manager login endpoint (no auth required)
        app.post("/api/auth/login", ctx -> {
            try {
                // Parse login request
                // @SuppressWarnings("unchecked")
                User loginData = ctx.bodyAsClass(User.class);
                System.out.println("Login attempt for user: " + loginData.getUsername());
                String username = loginData.getUsername();
                String password = loginData.getPassword();

                if (username == null || password == null) {
                    ctx.status(400);
                    ctx.json(Map.of(
                        "success", false,
                        "error", "Username and password are required"
                    ));
                    return;
                }
                
                // Authenticate manager
                System.out.println("Attempting authentication for user: " + username);
                java.util.Optional<com.rev.manager.repository.User> managerOpt = authenticationService.authenticateManager(username, password); // Replace with jdbc login (maybe)
                
                if (managerOpt.isPresent()) {
                    System.out.println("Authentication successful for user: " + username);
                    com.rev.manager.repository.User manager = managerOpt.get();
                    
                    // Create JWT token
                    String jwtToken = authenticationService.createJwtToken(manager);
                    
                    // Set JWT as HTTP-only cookie
                    ctx.cookie("jwt", jwtToken, 24 * 60 * 60); // 24 hours expiry
                    
                    ctx.status(200);
                    ctx.json(Map.of(
                        "success", true,
                        "message", "Login successful",
                        "user", Map.of(
                            "id", manager.getId(),
                            "username", manager.getUsername(),
                            "role", manager.getRole()
                        )
                    ));
                } else {
                    ctx.status(401);
                    ctx.json(Map.of(
                        "success", false,
                        "error", "Invalid credentials or user is not a manager"
                    ));
                }
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "error", "Invalid request format"
                ));
                e.printStackTrace();
            }
        });
        
        // Manager logout endpoint (no auth required)
        app.post("/api/auth/logout", ctx -> {
            // Clear the JWT cookie
            ctx.removeCookie("jwt");
            ctx.json(Map.of(
                "success", true,
                "message", "Logged out successfully"
            ));
        });
        
        // Protected routes - require manager authentication
        app.before("/api/expenses/*", authMiddleware.validateManager());
        app.before("/api/reports/*", authMiddleware.validateManager());
        
        // Expense management endpoints
        // All functions here now use the jdbcManagerDAO under the controller functions.
        app.get("/api/expenses", expenseController::getAllExpenses); // IMPLEMENTED :) (with view_all_expenses())
        app.get("/api/expenses/pending", expenseController::getPendingExpenses); // IMPLEMENTED :) (with view_expenses())
        app.get("/api/expenses/employee/{empUsername}", expenseController::getExpensesByEmployee); // IMPLEMENTED (with gen_report_emp())
        app.post("/api/expenses/{expenseId}/approve", expenseController::approveExpense); // IMPLEMENTED (with approved_exp()) (small bugs with showing "Network error" instead of "Expense already approved" when trying to submit an expense that has already been submitted)
        app.post("/api/expenses/{expenseId}/deny", expenseController::denyExpense); // IMPLEMENTED (with deny_exp()) (same bug as above)
        
        // Report generation endpoints
        // All functions here now use the jdbcManagerDAO under the controller functions.
        app.get("/api/reports/expenses/csv", reportController::generateAllExpensesReport); // IMPLEMENTED :) (with view_all_expenses())
        app.get("/api/reports/expenses/pending/csv", reportController::generatePendingExpensesReport); // IMPLEMENTED :) (with view_expenses())
        app.get("/api/reports/expenses/employee/{empUsername}/csv", reportController::generateEmployeeExpensesReport); //IMPLEMENTED :) (with gen_report_emp(empUsername)), switched url to /api/expenses/employee/{empUsername}
        app.get("/api/reports/expenses/category/{category}/csv", reportController::generateCategoryExpensesReport); //IMPLEMENTED :) (with gen_report_category(Category)) If a category is not an exact match to a category, defaults to Category other.
        app.get("/api/reports/expenses/daterange/csv", reportController::generateDateRangeExpensesReport); // IMPLEMENTED :) (with gen_report_date(startDate, endDate))
        
        // Root route - serve manager dashboard
        
        // Health check endpoint
        app.get("/health", ctx -> ctx.json(java.util.Map.of(
            "status", "healthy",
            "service", "expense-manager-api",
            "version", "1.0.0"
        )));
        
        // Start the server
        app.start(PORT);
        
        System.out.println("   Expense Manager API (Manager App) started successfully!");
        System.out.println("   Server running on: http://localhost:" + PORT);
        System.out.println("   Health check: http://localhost:" + PORT + "/health");
        System.out.println("   API Documentation:");
        System.out.println("   Authentication Status: GET /api/auth/status");
        System.out.println("   Pending Expenses: GET /api/expenses/pending");
        System.out.println("   All Expenses: GET /api/expenses");
        System.out.println("   Employee Expenses: GET /api/expenses/employee/{empUsername}"); // Switch to /api/expenses/employee/{empUsername}
        System.out.println("   Approve Expense: POST /api/expenses/{expenseId}/approve");
        System.out.println("   Deny Expense: POST /api/expenses/{expenseId}/deny");
        System.out.println("   CSV Reports: GET /api/reports/expenses/csv");
        System.out.println("   More reports available at /api/reports/expenses/...");
    }
}