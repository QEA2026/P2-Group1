package com.rev.manager.cucumber.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

public class DriverFactory {

    private static WebDriver driver;
    /**
     * Directory where Chrome will automatically download files.
     */
    private static final Path DOWNLOAD_DIRECTORY = Paths.get(
        System.getenv().getOrDefault(
            "DOWNLOAD_DIR",
            Paths.get(System.getProperty("user.dir"), "downloads").toString()
        )
    );
    
    private DriverFactory() {
        // Prevent instantiation
    }

    public static WebDriver getDriver() {

        if (driver == null) {

            // WebDriverManager.chromedriver().setup();

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
            // prefs.put("download.default_directory", "/downloads");
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.directory_upgrade", true);
            prefs.put("safebrowsing.enabled", true);

            ChromeOptions options = new ChromeOptions();

            options.setExperimentalOption("prefs", prefs);

            options.addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage"
            );

            /*
             * DOCKER=true
             *     Use Selenium container.
             *
             * Otherwise
             *     Use Chrome installed on Windows.
             */
            boolean docker = Boolean.parseBoolean(
                System.getenv().getOrDefault(
                    "DOCKER",
                    "false"
                )
            );
            
            try {

                if (docker) {

                    String seleniumUrl = System.getenv().getOrDefault(
                        "SELENIUM_URL",
                        "http://selenium:4444"
                    );

                    System.out.println(
                        "Running E2E tests using Docker Selenium: "
                            + seleniumUrl
                    );

                    driver = new RemoteWebDriver(
                        URI.create(seleniumUrl).toURL(),
                        options
                    );

                } else {

                    System.out.println(
                        "Running E2E tests using local ChromeDriver"
                    );

                    driver = new ChromeDriver(options);
                }

            } catch (MalformedURLException e) {

                throw new RuntimeException(
                    "Unable to connect to Selenium",
                    e
                );
            }
        }


        //     /*
        //      * Local Windows:
        //      *     http://localhost:4444
        //      *
        //      * Docker/Jenkins:
        //      *     http://selenium:4444
        //      */
        //     String seleniumUrl = System.getenv().getOrDefault(
        //         "SELENIUM_URL",
        //         "http://localhost:4444"
        //     );

        //     try {
        //         driver = new RemoteWebDriver(
        //             URI.create(seleniumUrl).toURL(),
        //             options
        //         );
        //     } catch (MalformedURLException e) {
        //         throw new RuntimeException(
        //             "Unable to connect to Selenium",
        //             e
        //         );
        //     }
        // }

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
