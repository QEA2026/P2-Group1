package com.rev.manager.cucumber.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public class DriverFactory {

    private static WebDriver driver;
    /**
     * Directory where Chrome will automatically download files.
     */
    private static final Path DOWNLOAD_DIRECTORY = Paths.get(
            System.getProperty("user.dir"),
            "target",
            "downloads"
    );
    
    private DriverFactory() {
        // Prevent instantiation
    }

    public static WebDriver getDriver() {

        if (driver == null) {

            WebDriverManager.chromedriver().setup();

            try {
                Files.createDirectories(DOWNLOAD_DIRECTORY);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to create download directory.",
                        e
                );
            }

            Map<String, Object> prefs = new HashMap<>();

            prefs.put(
                    "download.default_directory",
                    DOWNLOAD_DIRECTORY.toAbsolutePath().toString()
            );
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.directory_upgrade", true);
            prefs.put("safebrowsing.enabled", true);

            ChromeOptions options = new ChromeOptions();

            options.setExperimentalOption("prefs", prefs);

            options.addArguments(
                    "--start-maximized"
            );

            driver = new ChromeDriver(options);
        }

        return driver;
    }

    public static void quitDriver() {

        if (driver != null) {

            driver.quit();

            driver = null;
        }
    }

    public static Path getDownloadDirectory() {
        return DOWNLOAD_DIRECTORY;
    }
}
