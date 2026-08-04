package com.rev.manager.cucumber.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.openqa.selenium.WebDriver;

import com.rev.manager.cucumber.pages.LoginPage;
import com.rev.manager.cucumber.pages.ManagerPage;

import io.qameta.allure.Step;

public class AuthenticationHelper {

    private final LoginPage loginPage;
    private final ManagerPage managerPage;

    public AuthenticationHelper() {
        WebDriver driver = DriverFactory.getDriver();
        loginPage = new LoginPage(driver);
        managerPage = new ManagerPage(driver);
    }

    @Step("Authenticate as Manager")
    public void loginAsManager() {

        loginPage.open();

        loginPage.login(
            TestData.MANAGER_USERNAME,
            TestData.MANAGER_PASSWORD
        );

        assertTrue(managerPage.isDashboardLoaded());
    }
}
