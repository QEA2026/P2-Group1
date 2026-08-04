package com.rev.manager.cucumber.steps;

import com.rev.manager.cucumber.pages.ManagerPage;
import com.rev.manager.cucumber.utils.AuthenticationHelper;
import com.rev.manager.cucumber.utils.DriverFactory;

import io.cucumber.java.en.When;

public class DenyExpenseSteps {

    private final AuthenticationHelper authHelper;
    private final ManagerPage managerPage;

    public DenyExpenseSteps() {
        authHelper = new AuthenticationHelper();
        managerPage = new ManagerPage(DriverFactory.getDriver());
    }

    @When("the manager denies expense {string}")
    public void approveReview(String expenseId) {
        managerPage.rejectExpenseById(expenseId, "Not Nice");
    }

}
