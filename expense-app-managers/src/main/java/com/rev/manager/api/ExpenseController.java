package com.rev.manager.api;

import com.rev.manager.DAO.JDBCManagerDAO;
import com.rev.manager.DAO.ManagerException.ExpenseNotFoundException;
import com.rev.manager.DAO.ManagerException.ExpenseNotPendingException;
import com.rev.manager.DAO.ManagerException.UserNotFoundException;
import com.rev.manager.model.Expense;
import com.rev.manager.repository.ExpenseWithUser;
import com.rev.manager.repository.User;
import com.rev.manager.service.ExpenseService;
import io.javalin.http.Context;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.InternalServerErrorResponse;

import java.util.List;
import java.util.Map;

/**
 * REST controller for expense management operations.
 * Handles expense approval, denial, and viewing operations for managers.
 */
public class ExpenseController {
    private final ExpenseService expenseService;
    private JDBCManagerDAO jdbcManager;
    
    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public ExpenseController(ExpenseService expenseService, JDBCManagerDAO jdbcManager){
        this.expenseService = expenseService;
        this.jdbcManager = jdbcManager;
    }
    
    /**
     * Get all pending expenses for manager review.
     * GET /api/expenses/pending
     */
    public void getPendingExpenses(Context ctx) {
        try {
            //List<ExpenseWithUser> pendingExpenses = expenseService.getPendingExpenses(); // Replace with jdbc.view_expenses()
            List<Expense> pendingExpenses = jdbcManager.view_expenses();
            ctx.json(Map.of(
                "success", true,
                "data", pendingExpenses, //Data changes from ExpenseWithUser to Expense
                "count", pendingExpenses.size()
            ));
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to retrieve pending expenses: " + e.getMessage());
        }
    }
    
    /**
     * Approve an expense.
     * POST /api/expenses/{expenseId}/approve
     * Request body: { "comment": "optional comment" }
     */
    public void approveExpense(Context ctx) {
        try {
            int expenseId = ctx.pathParamAsClass("expenseId", Integer.class).get();
            User manager = AuthenticationMiddleware.getAuthenticatedManager(ctx);
            
            // Get optional comment from request body
            String comment = null;
            try {
                Map<String, Object> requestBody = ctx.bodyAsClass(Map.class);
                comment = (String) requestBody.get("comment");
            } catch (Exception e) {
                // Ignore - comment is optional
            }
            boolean success = false;
            try{
                jdbcManager.approve_exp(manager.getId(), expenseId, comment);
                success = true;
            } catch (ExpenseNotFoundException | ExpenseNotPendingException e){
                System.out.println("Expense can not be approved");
            }
            
            if (success) {
                ctx.json(Map.of(
                    "success", true,
                    "message", "Expense approved successfully"
                ));
            } else {
                throw new NotFoundResponse("Expense not found or could not be approved");
            }
            
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("Invalid expense ID format");
        } catch (Exception e) {
            if (e instanceof NotFoundResponse) {
                throw e;
            }
            throw new InternalServerErrorResponse("Failed to approve expense: " + e.getMessage());
        }
    }
    
    /**
     * Deny an expense.
     * POST /api/expenses/{expenseId}/deny
     * Request body: { "comment": "optional comment" }
     */
    public void denyExpense(Context ctx) {
        try {
            int expenseId = ctx.pathParamAsClass("expenseId", Integer.class).get();
            User manager = AuthenticationMiddleware.getAuthenticatedManager(ctx);
            
            // Get optional comment from request body
            String comment = null;
            try {
                Map<String, Object> requestBody = ctx.bodyAsClass(Map.class);
                comment = (String) requestBody.get("comment");
            } catch (Exception e) {
                // Ignore - comment is optional
            }
            
            boolean success = false;
            try{
                jdbcManager.deny_exp(manager.getId(), expenseId, comment);
                success = true;
            } catch (ExpenseNotFoundException | ExpenseNotPendingException e){
                System.out.println("Expense can not be denied");
            }

            if (success) {
                ctx.json(Map.of(
                    "success", true,
                    "message", "Expense denied successfully"
                ));
            } else {
                throw new NotFoundResponse("Expense not found or could not be denied");
            }
            
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("Invalid expense ID format");
        } catch (Exception e) {
            if (e instanceof NotFoundResponse) {
                throw e;
            }
            throw new InternalServerErrorResponse("Failed to deny expense: " + e.getMessage());
        }
    }
    
    /**
     * Get all expenses (for general viewing).
     * GET /api/expenses
     */
    public void getAllExpenses(Context ctx) {
        try {
            //List<ExpenseWithUser> allExpenses = expenseService.getAllExpenses();
            List<Expense> allExpenses = jdbcManager.view_all_expenses();
            ctx.json(Map.of(
                "success", true,
                "data", allExpenses,
                "count", allExpenses.size()
            ));
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to retrieve expenses: " + e.getMessage());
        }
    }
    
    /**
     * Get expenses for a specific employee.
     * GET /api/expenses/employee/{empUsername} //Switch to {empUsername}
     */
    public void getExpensesByEmployee(Context ctx) {
        try {
            String empUsername = ctx.pathParamAsClass("empUsername", String.class).get(); // Switch to String empUsername
            List<Expense> expenses = jdbcManager.gen_report_emp(empUsername); // Replace with gen_report_emp(empUsername), try catch for UserNotFoundException
            
            ctx.json(Map.of(
                "success", true,
                "data", expenses, // Data changes from List of ExpenseWithUser to List of Expense
                "count", expenses.size(),
                "empUsername", empUsername // Switch to employee name
            ));
            
        } catch (UserNotFoundException e) {
            System.out.println("User not found"); //FIXME: Should display error "User not found in database", but instead shows "Network error. Please try again""
            throw new BadRequestResponse("Invalid employee name, check spelling");
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to retrieve expenses for employee: " + e.getMessage());
        }
    }
}