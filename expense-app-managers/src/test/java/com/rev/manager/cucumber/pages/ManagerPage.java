package com.rev.manager.cucumber.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ManagerPage extends BasePage {
    
    private static final String URL =
        System.getenv().getOrDefault(
                "MANAGER_APP_URL",
                "http://localhost:5001"
        ) + "/manager.html";

    // ---------- Locators ----------

    // Header
    private final By usernameDisplay = By.id("username-display");
    private final By logoutBtn = By.id("logout-btn");

    // Navigation
    private final By showPendingTab = By.id("show-pending");
    private final By showAllExpensesTab = By.id("show-all-expenses");
    private final By showReportsTab = By.id("show-reports");

    // Pending Expenses
    private final By pendingExpensesSection = By.id("pending-expenses-section");
    private final By refreshPendingBtn = By.id("refresh-pending");
    private final By pendingExpensesList = By.id("pending-expenses-list");
    // header row excluded; each remaining <tr> is one expense
    private final By pendingExpenseRows = By.cssSelector("#pending-expenses-list table tr:not(:first-child)");

    // All Expenses
    private final By allExpensesSection = By.id("all-expenses-section");
    private final By employeeFilterInput = By.id("employee-filter");
    private final By filterByEmployeeBtn = By.id("filter-by-employee");
    private final By clearEmployeeFilterBtn = By.id("clear-employee-filter");
    private final By refreshAllExpensesBtn = By.id("refresh-all-expenses");
    private final By allExpensesList = By.id("all-expenses-list");
    private final By allExpenseRows = By.cssSelector("#all-expenses-list table tr:not(:first-child)");

    // Reports
    private final By reportsSection = By.id("reports-section");
    private final By generateAllExpensesReportBtn = By.id("generate-all-expenses-report");
    private final By generatePendingReportBtn = By.id("generate-pending-report");
    private final By employeeReportNameInput = By.id("employee-report-name");
    private final By generateEmployeeReportBtn = By.id("generate-employee-report");
    private final By categoryReportInput = By.id("category-report");
    private final By generateCategoryReportBtn = By.id("generate-category-report");
    private final By startDateInput = By.id("start-date");
    private final By endDateInput = By.id("end-date");
    private final By generateDateRangeReportBtn = By.id("generate-date-range-report");
    private final By reportMessage = By.id("report-message");

    // Review Modal
    private final By reviewModal = By.id("review-modal");
    private final By expenseDetails = By.id("expense-details");
    private final By reviewCommentBox = By.id("review-comment");
    private final By approveExpenseBtn = By.id("approve-expense");
    private final By denyExpenseBtn = By.id("deny-expense");
    private final By cancelReviewBtn = By.id("cancel-review");
    private final By reviewMessage = By.id("review-message");

    // Loading
    private final By loadingSection = By.id("loading-section");

    // Column indices within a pending-row's <td> list
    private static final int PENDING_COL_ID = 0;
    private static final int PENDING_COL_EMPLOYEE = 1;
    private static final int PENDING_COL_DESCRIPTION = 2;
    private static final int PENDING_COL_DATE = 3;
    private static final int PENDING_COL_AMOUNT = 4;
    private static final int PENDING_COL_CATEGORY = 5;
    private static final int PENDING_COL_STATUS = 6;
    private static final int PENDING_COL_ACTIONS = 7;

    // Column indices within an all-expenses row's <td> list
    private static final int ALL_COL_ID = 0;
    private static final int ALL_COL_EMPLOYEE = 1;
    private static final int ALL_COL_STATUS = 6;
    private static final int ALL_COL_REVIEWER = 7;
    private static final int ALL_COL_COMMENT = 8;
    private static final int ALL_COL_REVIEW_DATE = 9;


    public ManagerPage(WebDriver driver) {
        super(driver);
    }

    // ---------- Page State ----------
    
    public void open() {
        driver.get(URL);
    }

    public String getWelcomeUsername() {
        return find(usernameDisplay).getText();
    }

    public boolean isDashboardLoaded() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loadingSection));
        return find(showPendingTab).isDisplayed();
    }

    // ---------- Navigation ----------

    public void goToPendingExpenses() {
        click(showPendingTab);
        find(pendingExpensesSection);
    }

    public void goToAllExpenses() {
        click(showAllExpensesTab);
        find(allExpensesSection);
    }

    public void goToReports() {
        click(showReportsTab);
        find(reportsSection);
    }

    // ---------- Pending Expenses ----------

    public void refreshPendingExpenses() {
        click(refreshPendingBtn);
    }

    public int getPendingExpenseCount() {
        return findAll(pendingExpenseRows).size();
    }

    public String getPendingExpenseId(int row) {
        return cellText(pendingExpenseRows, row, PENDING_COL_ID);
    }

    public String getPendingExpenseEmployee(int row) {
        return cellText(pendingExpenseRows, row, PENDING_COL_EMPLOYEE);
    }

    public String getPendingExpenseStatus(int row) {
        return cellText(pendingExpenseRows, row, PENDING_COL_STATUS);
    }

    /** True if a row with the given expense id currently appears in the Pending list. */
    public boolean isExpensePending(String expenseId) {
        return findRowIndexById(pendingExpenseRows, PENDING_COL_ID, expenseId) >= 0;
    }

    // ---------- All Expenses ----------

    public void filterByEmployee(String employeeName) {
        type(employeeFilterInput, employeeName);
        click(filterByEmployeeBtn);
    }

    public void clearEmployeeFilter() {
        click(clearEmployeeFilterBtn);
    }

    public void refreshAllExpenses() {
        click(refreshAllExpensesBtn);
    }

    public int getAllExpensesCount() {
        return findAll(allExpenseRows).size();
    }

    public String getAllExpenseStatus(String expenseId) {
        // int idx = findRowIndexById(allExpenseRows, ALL_COL_ID, expenseId);
        // if (idx < 0) return null;
        // return cellText(allExpenseRows, idx, ALL_COL_STATUS);
        By statusCell = By.xpath(
            "//div[@id='all-expenses-list']"
            + "//table//tr[td[normalize-space()='" + expenseId + "']]"
            + "/td[" + (ALL_COL_STATUS + 1) + "]"
        );

        return wait.until(
            ExpectedConditions.visibilityOfElementLocated(statusCell)
        ).getText().trim();
    }

    public String getAllExpenseReviewer(String expenseId) {
        int idx = findRowIndexById(allExpenseRows, ALL_COL_ID, expenseId);
        if (idx < 0) return null;
        return cellText(allExpenseRows, idx, ALL_COL_REVIEWER);
    }

    public String getAllExpenseComment(String expenseId) {
        int idx = findRowIndexById(allExpenseRows, ALL_COL_ID, expenseId);
        if (idx < 0) return null;
        return cellText(allExpenseRows, idx, ALL_COL_COMMENT);
    }

    /** True if an expense with the given id appears anywhere in the All Expenses list. */
    public boolean isExpenseDisplayed(String expenseId) {
        return findRowIndexById(allExpenseRows, ALL_COL_ID, expenseId) >= 0;
    }

    // ---------- Expense Review Actions ----------
    // NOTE: "row" is the index (0-based) into the PENDING table's data rows,
    // i.e. row 0 is the first expense listed, not the <th> header row.

    private void openReviewModal(int row) {
        
        List<WebElement> rows = findAll(pendingExpenseRows);
        if(row >= rows.size())
            throw new IllegalArgumentException("No pending expenses returned");

        WebElement actionCell = rows.get(row).findElements(By.tagName("td")).get(PENDING_COL_ACTIONS);
        actionCell.findElement(By.tagName("button")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(reviewModal));
    }

    private void openReviewModalByExpenseId(String expenseId) {
        int row = findRowIndexById(pendingExpenseRows, PENDING_COL_ID, expenseId);
        if (row < 0) {
            throw new IllegalStateException("Expense id " + expenseId + " not found in Pending Expenses list");
        }
        openReviewModal(row);
    }

    public void approveExpense(int row) {
        openReviewModal(row);
        click(approveExpenseBtn);
    }

    public void approveExpense(int row, String comment) {
        openReviewModal(row);
        driver.findElement(reviewCommentBox).sendKeys(comment);
        click(approveExpenseBtn);
    }

    public void approveExpenseById(String expenseId, String comment) {
        openReviewModalByExpenseId(expenseId);
        if (comment != null && !comment.isEmpty()) {
            driver.findElement(reviewCommentBox).sendKeys(comment);
        }
        click(approveExpenseBtn);
    }

    public void rejectExpense(int row) {
        openReviewModal(row);
        click(denyExpenseBtn);
    }

    public void rejectExpense(int row, String comment) {
        openReviewModal(row);
        driver.findElement(reviewCommentBox).sendKeys(comment);
        click(denyExpenseBtn);
    }

    public void rejectExpenseById(String expenseId, String comment) {
        openReviewModalByExpenseId(expenseId);
        if (comment != null && !comment.isEmpty()) {
            driver.findElement(reviewCommentBox).sendKeys(comment);
        }
        click(denyExpenseBtn);
    }

    public void cancelReview() {
        click(cancelReviewBtn);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(reviewModal));
    }

    public boolean isReviewModalDisplayed() {
        return driver.findElement(reviewModal).isDisplayed();
    }

    public String getExpenseDetailsText() {
        return driver.findElement(expenseDetails).getText();
    }

    public String getReviewMessage() {
        return driver.findElement(reviewMessage).getText();
    }

    // ---------- Reports ----------

    public void generateAllExpensesReport() {
        click(generateAllExpensesReportBtn);
    }

    public void generatePendingReport() {
        click(generatePendingReportBtn);
    }

    public void generateEmployeeReport(String employeeName) {
        type(employeeReportNameInput, employeeName);
        click(generateEmployeeReportBtn);
    }

    public void generateCategoryReport(String category) {
        type(categoryReportInput, category);
        click(generateCategoryReportBtn);
    }

    public void generateDateRangeReport(String startDate, String endDate) {
        driver.findElement(startDateInput).sendKeys(startDate);
        driver.findElement(endDateInput).sendKeys(endDate);
        click(generateDateRangeReportBtn);
    }

    public String getReportMessage() {
        return driver.findElement(reportMessage).getText();
    }

    // ---------- Logout ----------

    public void logout() {
        click(logoutBtn);
        wait.until(ExpectedConditions.urlContains("login"));
    }

    // ---------- Helpers ----------

    private String cellText(By rowsLocator, int rowIndex, int colIndex) {
        List<WebElement> rows = findAll(rowsLocator);
        return rows.get(rowIndex).findElements(By.tagName("td")).get(colIndex).getText();
    }

    /** Returns the 0-based row index whose given column matches expectedValue, or -1 if not found. */
    private int findRowIndexById(By rowsLocator, int idColIndex, String expectedValue) {
        List<WebElement> rows = findAll(rowsLocator);
        for (int i = 0; i < rows.size(); i++) {
            String cell = rows.get(i).findElements(By.tagName("td")).get(idColIndex).getText();
            if (cell.equals(expectedValue)) {
                return i;
            }
        }
        return -1;
    }
}
