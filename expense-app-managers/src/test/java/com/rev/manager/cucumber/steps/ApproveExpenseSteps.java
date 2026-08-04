package com.rev.manager.cucumber.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rev.manager.cucumber.pages.ManagerPage;
import com.rev.manager.cucumber.utils.AuthenticationHelper;
import com.rev.manager.cucumber.utils.DriverFactory;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ApproveExpenseSteps {

    private final AuthenticationHelper authHelper;
    private final ManagerPage managerPage;

    public ApproveExpenseSteps() {
        authHelper = new AuthenticationHelper();
        managerPage = new ManagerPage(DriverFactory.getDriver());
    }

    @And("pending expenses exist")
    public void verifiesPendingExpensesExist() {
        assertTrue(managerPage.getPendingExpenseCount() > 0);
    }

    @When("the manager approves expense {string}")
    public void approveReview(String expenseId) {
        managerPage.approveExpenseById(expenseId, "Nice");
    }

    @Then("the expense {string} status should be {string}")
    public void verifyExpenseStatus(String expenseId, String status) {
        managerPage.cancelReview();
        managerPage.goToAllExpenses();
        managerPage.refreshAllExpenses();
        assertEquals(status, managerPage.getAllExpenseStatus(expenseId));
    }

}
