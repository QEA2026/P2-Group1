package com.rev.manager.cucumber.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rev.manager.cucumber.pages.ManagerPage;
import com.rev.manager.cucumber.utils.AuthenticationHelper;
import com.rev.manager.cucumber.utils.DatabaseTestHelper;
import com.rev.manager.cucumber.utils.DriverFactory;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class PendingExpenseSteps {
    private final AuthenticationHelper authHelper;
    private final ManagerPage managerPage;

    public PendingExpenseSteps() {
        authHelper = new AuthenticationHelper();
        managerPage = new ManagerPage(DriverFactory.getDriver());
    }

    @Given("the manager is logged in")
    public void theManagerIsLoggedIn() {
        authHelper.loginAsManager();
    }


    @When("the manager refreshes pending expenses")
    public void theManagerRefreshesPage() {
        managerPage.refreshPendingExpenses();
    }

    @Then("the pending expense table should contain pending expenses only")
    public void pendingExpensesShouldBeDisplayed() {
        assertTrue(managerPage.getPendingExpenseCount() > 0);
    }

    @Given("there are no pending expenses")
    public void noPendingExpenses() {
        DatabaseTestHelper.executeSqlFile("remove-pending-expenses.sql");
    }

    @Then("the manager should see no pending expenses")
    public void verifyNoPendingExpenses() {
        assertEquals(0, managerPage.getPendingExpenseCount());
    }
}
