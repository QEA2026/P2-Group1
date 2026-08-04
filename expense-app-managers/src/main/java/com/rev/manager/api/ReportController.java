package com.rev.manager.api;

import com.rev.manager.DAO.JDBCManagerDAO;
import com.rev.manager.model.Category;
import com.rev.manager.model.Expense;
import com.rev.manager.repository.ExpenseWithUser;
import com.rev.manager.service.ExpenseService;
import io.javalin.http.Context;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.InternalServerErrorResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * REST controller for expense reporting operations.
 * Handles CSV report generation by various criteria.
 */
public class ReportController {
    private final ExpenseService expenseService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private JDBCManagerDAO jdbcManager = null;
    
    public ReportController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public ReportController(ExpenseService expenseService, JDBCManagerDAO jdbcManager) {
        this.expenseService = expenseService;
        this.jdbcManager = jdbcManager;
    }
    
    /**
     * Generate CSV report of all expenses.
     * GET /api/reports/expenses/csv
     */
    public void generateAllExpensesReport(Context ctx) {
        try {
            List<Expense> expenses = jdbcManager.view_all_expenses();
            //List<ExpenseWithUser> expenses = expenseService.getAllExpenses();
            String csvContent = expenseService.generateCsvReport(expenses);
            
            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"all_expenses_report.csv\"");
            ctx.result(csvContent);
            
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to generate expenses report: " + e.getMessage());
        }
    }
    
    /**
     * Generate CSV report of expenses by employee.
     * GET /api/reports/expenses/employee/{empUsername}/csv // Replace with {empUsername}
     */
    public void generateEmployeeExpensesReport(Context ctx) {
        try {
            String empUsername = ctx.pathParamAsClass("empUsername", String.class).get();
            List<Expense> expenses = jdbcManager.gen_report_emp(empUsername);
            //List<ExpenseWithUser> expenses = expenseService.getExpensesByEmployee(employeeId); //Replace with jdbc gen_report_emp(empUsername)
            String csvContent = expenseService.generateCsvReport(expenses); // Change so CsvReport take in List<Expense> instead of List<ExpenseWithUser>
            
            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"employee_" + empUsername + "_expenses_report.csv\"");
            ctx.result(csvContent);
            
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("Invalid employee ID format");
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to generate employee expenses report: " + e.getMessage());
        }
    }
    
    /**
     * Generate CSV report of expenses by category.
     * GET /api/reports/expenses/category/{category}/csv
     */
    public void generateCategoryExpensesReport(Context ctx) {
        try {
            String category = ctx.pathParam("category");
            
            if (category == null || category.trim().isEmpty()) {
                throw new BadRequestResponse("Category parameter is required");
            }
            //Add in additional check to make sure Category is part of the enumeration
            
            //List<ExpenseWithUser> expenses = expenseService.getExpensesByCategory(category); // Replace with jdbc gen_report_cat
            Category c = Category.Other;
            try {c = Category.valueOf(category);}
            catch (IllegalArgumentException e){

            }
            List<Expense> expenses = jdbcManager.gen_report_cat(c);
            String csvContent = expenseService.generateCsvReport(expenses);
            
            String safeCategory = category.replaceAll("[^a-zA-Z0-9_-]", "_");
            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"category_" + safeCategory + "_expenses_report.csv\"");
            ctx.result(csvContent);
            
        } catch (Exception e) {
            if (e instanceof BadRequestResponse) {
                throw e;
            }
            throw new InternalServerErrorResponse("Failed to generate category expenses report: " + e.getMessage());
        }
    }
    
    /**
     * Generate CSV report of expenses by date range.
     * GET /api/reports/expenses/daterange/csv?startDate=YYYY/MM/DD&endDate=YYYY/MM/DD
     */
    public void generateDateRangeExpensesReport(Context ctx) {
        try {
            String startDateStr = ctx.queryParam("startDate");
            String endDateStr = ctx.queryParam("endDate");
            startDateStr = startDateStr.replace('-','/');
            endDateStr = endDateStr.replace('-','/');
            
            if (startDateStr == null || endDateStr == null) {
                throw new BadRequestResponse("Both startDate and endDate query parameters are required (format: YYYY/MM/DD)");
            }
            
            // Validate date format
            try {
                LocalDate.parse(startDateStr, DATE_FORMATTER);
                LocalDate.parse(endDateStr, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new BadRequestResponse("Invalid date format. Use YYYY/MM/DD format");
            }
            
            //List<ExpenseWithUser> expenses = expenseService.getExpensesByDateRange(startDateStr, endDateStr); // Replace with jdbc gen_report_date()
            System.out.println("Start date is : " + startDateStr);
            System.out.println("End date is   : " + endDateStr);
            List<Expense> expenses = jdbcManager.gen_report_date(startDateStr, endDateStr);
            String csvContent = expenseService.generateCsvReport(expenses);
            
            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"expenses_" + startDateStr + "_to_" + endDateStr + "_report.csv\"");
            ctx.result(csvContent);
            
        } catch (Exception e) {
            if (e instanceof BadRequestResponse) {
                throw e;
            }
            throw new InternalServerErrorResponse("Failed to generate date range expenses report: " + e.getMessage());
        }
    }
    
    /**
     * Generate CSV report of pending expenses only.
     * GET /api/reports/expenses/pending/csv
     */
    public void generatePendingExpensesReport(Context ctx) {
        try {
            //List<ExpenseWithUser> expenses = expenseService.getPendingExpenses(); // Change to jdbc.view_expenses()
            List<Expense> expenses = jdbcManager.view_expenses();
            String csvContent = expenseService.generateCsvReport(expenses); // Change to take in List<Expense>
            
            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"pending_expenses_report.csv\"");
            ctx.result(csvContent);
            
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to generate pending expenses report: " + e.getMessage());
        }
    }
}