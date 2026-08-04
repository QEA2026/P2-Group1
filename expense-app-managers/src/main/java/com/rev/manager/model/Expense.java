package com.rev.manager.model;


/**
 * Class to represent all the data relevant to a manager stored about a reported expense.
 * From the users table, it will store the username of the employee making the expense.
 * From the expenses table, it will store the expense id (id), amount, description, date, and category.
 * From the approvals table, it will store the status, reviewer id, comment, and review_date.
 * If an expense has status pending, it will have null values for reviewer_id, comment, and review_date.
 */
public final class Expense{
    private final long expense_id;
    private final String emp_name;
    private final double amount;
    private final String description;
    private final String exp_date;
    private final Category category;
    private final Status status;
    private final String reviewer_name;
    private final String comment;
    private final String review_date;

    /**
     * Constructor for an expense with status = Status.pending, where all of the values in the approval table are null (excluding status)
     * Will set reviewer_id to 0, which is not a valid reviewer_id
     * @param expense_id
     * @param emp_name
     * @param amount
     * @param description
     * @param exp_date String in format 'YYYY-MM-DD'
     * @param category
     */
    public Expense(long expense_id, String emp_name, double amount, String description, String exp_date,
            Category category) {
        this.expense_id = expense_id;
        this.emp_name = emp_name;
        this.amount = amount;
        this.description = description;
        this.exp_date = exp_date;
        this.category = category;
        this.status = Status.pending;
        this.reviewer_name = null;
        this.comment = null;
        this.review_date = null;
    }

    /**
     * Constructor for an expense that has either been approved or denied.
     * @param expense_id
     * @param emp_name
     * @param amount
     * @param description
     * @param exp_date
     * @param category
     * @param status Status.approved or Status.denied
     * @param reviewer_name
     * @param comment
     * @param review_date
     */
    public Expense(long expense_id, String emp_name, double amount, String description, String exp_date,
            Category category, Status status, String reviewer_name, String comment, String review_date) {
        this.expense_id = expense_id;
        this.emp_name = emp_name;
        this.amount = amount;
        this.description = description;
        this.exp_date = exp_date;
        this.category = category;
        this.status = status;
        this.reviewer_name = reviewer_name;
        this.comment = comment;
        this.review_date = review_date;
    }

    public long getExpense_id() {
        return expense_id;
    }

    public String getEmp_name() {
        return emp_name;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getExp_date() {
        return exp_date;
    }

    public Category getCategory() {
        return category;
    }

    public Status getStatus() {
        return status;
    }

    public String getReviewer_name() {
        return reviewer_name;
    }

    public String getComment() {
        return comment;
    }

    public String getReview_date() {
        return review_date;
    }

    @Override
    public String toString() {
        return String.format("│ %-3d │ %-15s │ %-10.2f │ %-40s │ %-10s │ %-15s │ %-10s │ %-20s │ %-40s │ %-10s │",
        expense_id, emp_name, amount, description, exp_date, category.toString(), status.toString(), reviewer_name, comment, review_date);
    }

    public static void printTableHeader(){
        System.out.println("╭─────┬─────────────────┬────────────┬──────────────────────────────────────────┬────────────┬─────────────────┬────────────┬──────────────────────┬──────────────────────────────────────────┬────────────╮");
        System.out.println("│EXPID│  EMPLOYEE NAME  │   AMOUNT   │               DESCRIPTION                │EXPENSE_DATE│     CATEGORY    │   STATUS   │     REVIEWER_NAME    │                 COMMENT                  │ REVIEW_DATE│");
        System.out.println("╞═════╪═════════════════╪════════════╪══════════════════════════════════════════╪════════════╪═════════════════╪════════════╪══════════════════════╪══════════════════════════════════════════╪════════════╡");
    }

    public static void printTableCloser(){
        System.out.println("╰─────┴─────────────────┴────────────┴──────────────────────────────────────────┴────────────┴─────────────────┴────────────┴──────────────────────┴──────────────────────────────────────────┴────────────╯");
    }
}
