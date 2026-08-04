package com.rev.manager.cucumber.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    // Login URL
    private static final String URL = "http://localhost:5001";

    // Locators
    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By LOGIN_BUTTON = By.id("login-btn");
    private static final By LOGIN_MESSAGE = By.id("login-message");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Opens the login page
     */
    public void open() {
        driver.get(URL);
    }

    /**
     * Enters username
     * @param username user's username for the account
     */
    public void enterUsername(String username) {
        type(USERNAME, username);
    }

    /**
     * Enters password
     * @param password user's password for the account
     */
    public void enterPassword(String password) {
        type(PASSWORD, password);
    }

    /**
     * Clicks the login button
     */
    public void clickLogin() {
        click(LOGIN_BUTTON);
    }

    /**
     * Logs in to the application.
     * @param username user's username for the account
     * @param password user's password for the account
     */
    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }

    /**
     * Returns the login response message
     * @return String  login message
     */
    public String getLoginMessage() {
        return getText(LOGIN_MESSAGE);
    }

    /**
     * Returns true if the login form is visible
     * @return True if the login form is visible, false otherwise
     */
    public boolean isLoginFormDisplayed() {
        return isDisplayed(USERNAME) && isDisplayed(PASSWORD) && isDisplayed(LOGIN_BUTTON);
    }

}
