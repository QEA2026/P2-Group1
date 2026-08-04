package com.rev.manager.cucumber.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rev.manager.cucumber.pages.LoginPage;
import com.rev.manager.cucumber.pages.ManagerPage;
import com.rev.manager.cucumber.utils.DriverFactory;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LogoutSteps {

    private final LoginPage loginPage;
    private final ManagerPage managerPage;

     public LogoutSteps() {
        loginPage = new LoginPage(DriverFactory.getDriver());
        managerPage = new ManagerPage(DriverFactory.getDriver());
    }

    @When("the manager logs out")
    public void managerLogsOut() {
        managerPage.logout();
    }

    @Then("the manager should be redirected to the login page")
    public void redirectedToLoginPage() {
        assertTrue(DriverFactory.getDriver().getCurrentUrl().contains("login"),
        "Manager was not redirected to the login page.");
    }

    @And("the login form should be displayed")
    public void loginFormShouldBeDisplayed() {
        assertTrue(loginPage.isLoginFormDisplayed());
    }
}
