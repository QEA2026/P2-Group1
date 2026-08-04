package com.rev.manager.cucumber.hooks;

import java.io.ByteArrayInputStream;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import com.rev.manager.cucumber.utils.DatabaseTestHelper;
import com.rev.manager.cucumber.utils.DownloadHelper;
import com.rev.manager.cucumber.utils.DriverFactory;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;

public class Hooks {

    @BeforeAll
    public static void beforeAll() {
        DatabaseTestHelper.initializeDatabase();
    }

    @Before(order = 0)
    public void resetDatabase() {
        DatabaseTestHelper.resetDatabase();
    }

    @Before(order = 1)
    public void clearDownloads() {
        DownloadHelper.clearDownloadDirectory();
    }
    @Before(order = 2)
    public void startBrowser() {
        DriverFactory.getDriver();
    }

    @After(order = 1)
    public void takeScreenShotOnFailure(Scenario scenario) {
        if (scenario.isFailed()) {
            byte[] screenshot = ((TakesScreenshot) DriverFactory.getDriver())
                    .getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", scenario.getName());

            Allure.addAttachment("Failure Screenshot", new ByteArrayInputStream(screenshot));
            
        }
    }

    @After(order = 0)
    public void quitDriver() { 
        DriverFactory.quitDriver();
    }
}
