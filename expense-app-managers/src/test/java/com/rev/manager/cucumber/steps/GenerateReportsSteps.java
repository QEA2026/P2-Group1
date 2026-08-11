package com.rev.manager.cucumber.steps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rev.manager.cucumber.pages.ManagerPage;
import com.rev.manager.cucumber.utils.DownloadHelper;
import com.rev.manager.cucumber.utils.DriverFactory;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class GenerateReportsSteps {

    private final ManagerPage managerPage;
    private String expectedReportFilename;
    private File downloadedReport;
    private String category;

    public GenerateReportsSteps() {
        managerPage = new ManagerPage(DriverFactory.getDriver());
    }

    @When("the manager generates an employee report for {string}")
    public void generateReportByEmployee(String employee) {
        expectedReportFilename = "employee_" + employee + "_report.csv";
        managerPage.goToReports();
        managerPage.generateEmployeeReport(employee);
    }

    @When("the manager generates a category report for {string}")
    public void generateReportByCategory(String category) {
        expectedReportFilename = "category_" + category + "_report.csv";
        managerPage.goToReports();
        managerPage.generateCategoryReport(category);
    }

    @When("the manager generates a date range report from {string} to {string}")
    public void generateReportByDateRange(String startDate, String endDate) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMddyyyy");

        DateTimeFormatter outputFormatter =  DateTimeFormatter.ISO_LOCAL_DATE;

        LocalDate start = LocalDate.parse(startDate, inputFormatter);

        LocalDate end = LocalDate.parse(endDate, inputFormatter);

        String formattedStart = start.format(outputFormatter);
        String formattedEnd = end.format(outputFormatter);

        expectedReportFilename = "expenses_" + formattedStart + "_to_" + formattedEnd + "_report.csv";
        managerPage.goToReports();
        managerPage.generateDateRangeReport(startDate, endDate);
    }

    @Then("the manager should see {string}")
    public void verifySuccessMessage(String successMessage) {
        assertEquals(successMessage, managerPage.getReportMessage());
    }

    @And("a CSV report should be downloaded")
    public void csvReportShouldBeDownloaded() {

        downloadedReport  = DownloadHelper.waitForCsvDownload(expectedReportFilename);

        assertTrue(downloadedReport.exists());
        assertTrue(downloadedReport.length() > 0);
    }

    @And("the CSV report should contain expenses for {string}")
    public void cvsReportShouldContainExpenses(String employee) throws IOException {
        List<String> lines = Files.readAllLines(downloadedReport.toPath());
        assertTrue(lines.stream().anyMatch(line -> line.contains(employee)));
    }

    @And("every expense in the CSV report should be between {string} and {string}")
    public void verifyDateRange(String startDate, String endDate) throws IOException {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMddyyyy");
        DateTimeFormatter csvFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        LocalDate start = LocalDate.parse(startDate, inputFormatter);
        LocalDate end = LocalDate.parse(endDate, inputFormatter);

        List<String> lines = Files.readAllLines(downloadedReport.toPath());

        String[] headers = lines.getFirst().split(",");

        int dateColumn = -1;

        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase("Date")) {
                dateColumn = i;
                break;
            }
        }

        assertTrue(dateColumn >= 0, "Date column not found in CSV.");

        // Skip header
        for (int i = 1; i < lines.size(); i++) {

            String[] columns = lines.get(i).split(",");

            // Replace with the correct column index
            String dateString = columns[dateColumn].trim();

            LocalDate expenseDate = LocalDate.parse(dateString, csvFormatter);

            assertFalse(expenseDate.isBefore(start), "Expense date " + expenseDate + " is before " + start);

            assertFalse(expenseDate.isAfter(end), "Expense date " + expenseDate + " is after " + end);
        }
    }
}
