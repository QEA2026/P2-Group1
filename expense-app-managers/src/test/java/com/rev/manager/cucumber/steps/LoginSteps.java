package com.rev.manager.cucumber.steps;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rev.manager.cucumber.pages.LoginPage;
import com.rev.manager.cucumber.utils.DriverFactory;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;

@Epic("Expense Management System")
@Feature("Manager Authentication")
public class LoginSteps {

    private final LoginPage loginPage;

    public LoginSteps() {
        loginPage = new LoginPage(DriverFactory.getDriver());
    }

    @Given("the manager is on the login page")
    public void theManagerIsOnTheLoginPage() {

        loginPage.open();

        assertTrue(loginPage.isLoginFormDisplayed());
    }

    @When("the manager enters username {string}")
    public void theManagerEntersUsername(String username) {

        loginPage.enterUsername(username);
    }
    @And("enters password {string}")
    public void theManagerEntersPassword(String password) {

        loginPage.enterPassword(password);
    }
    @And("clicks login button")
    public void clicksLoginButton() {
        loginPage.clickLogin();
    }

    @Then("the login message should be {string}")
    public void verifyMessage(String expected) {

        assertEquals(expected, loginPage.getLoginMessage());
    }

    // @Then("the login result should be {string}")
    // public void theLoginResultShouldBe(String result) {

    //     switch (result.toUpperCase()) {

    //         case "SUCCESS":

    //             assertTrue(managerPage.isDashboardLoaded());

    //             // Adjust this to whatever your ManagerPage returns
    //             assertEquals("Andrew", managerPage.getWelcomeUsername());
    //             break;

    //         case "FAILURE":

    //             assertTrue(loginPage.isLoginFormDisplayed());
    //             break;

    //         default:
    //             fail("Unknown login result: " + result);
    //     }
    // }
}
