package com.rev.manager.service;

import com.rev.manager.model.Expense;
import com.rev.manager.repository.ApprovalRepository;
import com.rev.manager.repository.ExpenseRepository;
import com.rev.manager.repository.ExpenseWithUser;
import com.rev.manager.repository.User;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

/**
 * Service for expense management business logic.
 * Handles expense approvals, reporting, and related operations.
 */
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final ApprovalRepository approvalRepository;
    
    public ExpenseService(ExpenseRepository expenseRepository, ApprovalRepository approvalRepository) {
        this.expenseRepository = expenseRepository;
        this.approvalRepository = approvalRepository;
    }
    
    /**
     * Get all pending expenses for manager review.
     * @return List of pending expenses with user information
     */
    public List<ExpenseWithUser> getPendingExpenses() {
        return expenseRepository.findPendingExpensesWithUsers();
    }
    
    /**
     * Approve an expense.
     * @param expenseId the expense ID to approve
     * @param managerId the manager's user ID
     * @param comment optional comment from manager
     * @return true if approval was successful
     */
    public boolean approveExpense(int expenseId, int managerId, String comment) {
        return approvalRepository.updateApprovalStatus(expenseId, "approved", managerId, comment);
    }
    
    /**
     * Deny an expense.
     * @param expenseId the expense ID to deny
     * @param managerId the manager's user ID
     * @param comment optional comment from manager
     * @return true if denial was successful
     */
    public boolean denyExpense(int expenseId, int managerId, String comment) {
        return approvalRepository.updateApprovalStatus(expenseId, "denied", managerId, comment);
    }
    
    /**
     * Get expenses for a specific employee.
     * @param employeeId the employee's user ID
     * @return List of expenses for the employee
     */
    public List<ExpenseWithUser> getExpensesByEmployee(int employeeId) {
        return expenseRepository.findExpensesByUser(employeeId);
    }
    
    /**
     * Get expenses by category (description contains the category text).
     * @param category the category to filter by
     * @return List of expenses matching the category
     */
    public List<ExpenseWithUser> getExpensesByCategory(String category) {
        return expenseRepository.findExpensesByCategory(category);
    }
    
    /**
     * Get expenses within a date range.
     * @param startDate start date (YYYY-MM-DD format)
     * @param endDate end date (YYYY-MM-DD format)
     * @return List of expenses within the date range
     */
    public List<ExpenseWithUser> getExpensesByDateRange(String startDate, String endDate) {
        return expenseRepository.findExpensesByDateRange(startDate, endDate);
    }
    
    /**
     * Get all expenses.
     * @return List of all expenses with user information
     */
    public List<ExpenseWithUser> getAllExpenses() {
        return expenseRepository.findAllExpensesWithUsers();
    }
    
    /**
     * Generate a CSV report of expenses.
     * @param expenses the list of expenses to include in the report
     * @return CSV string representation of the expenses
     */
    public String generateCsvReport(List<Expense> expenses) { // Replace to take in a List<Expense>
        StringWriter csvWriter = new StringWriter();
        //Replace body so that it works with the Expense datatype instead of ExpenseWithUser


        // CSV Header
        //csvWriter.append("Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date\n");
        csvWriter.append("Expense ID,Employee,Amount,Description,Date,Category,Status,Reviewer,Comment,Review Date\n");
        
        // CSV Data
        for (Expense expenseWithUser : expenses) {
            csvWriter.append(String.valueOf(expenseWithUser.getExpense_id())).append(",");
            csvWriter.append(escapeCsvValue(expenseWithUser.getEmp_name())).append(",");
            csvWriter.append(String.valueOf(expenseWithUser.getAmount())).append(",");
            csvWriter.append(escapeCsvValue(expenseWithUser.getDescription())).append(",");
            csvWriter.append(expenseWithUser.getExp_date()).append(",");
            csvWriter.append(expenseWithUser.getCategory().name()).append(",");
            csvWriter.append(expenseWithUser.getStatus().name()).append(",");
            
            // Reviewer (might be null for pending expenses)
            String reviewer = expenseWithUser.getReviewer_name();
            if (reviewer != null) {
                csvWriter.append(reviewer);
            }
            csvWriter.append(",");
            
            // Comment (might be null)
            String comment = expenseWithUser.getComment();
            if (comment != null) {
                csvWriter.append(escapeCsvValue(comment));
            }
            csvWriter.append(",");
            
            // Review Date (might be null for pending expenses)
            String reviewDate = expenseWithUser.getReview_date();
            if (reviewDate != null) {
                csvWriter.append(reviewDate);
            }
            
            csvWriter.append("\n");
        }
        
        return csvWriter.toString();
    }
    
    /**
     * Escape CSV values to handle commas, quotes, and newlines.
     * @param value the value to escape
     * @return escaped CSV value
     */
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}