package com.crd.rebalancing.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages WebDriver lifecycle.
 * Runs headless Chrome locally and inside Docker (amd64 via --platform flag).
 */
public class DriverManager {

    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    public static WebDriver getDriver() {
        if (driverHolder.get() == null) {
            log.info("Initialising ChromeDriver...");
            WebDriverManager.chromedriver().setup();
            log.debug("ChromeDriver binary resolved by WebDriverManager");

            ChromeOptions options = buildChromeOptions();
            driverHolder.set(new ChromeDriver(options));
            log.info("ChromeDriver started successfully");
        }
        return driverHolder.get();
    }

    public static void quitDriver() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            log.info("Quitting ChromeDriver");
            driver.quit();
            driverHolder.remove();
            log.debug("ChromeDriver session closed and removed from ThreadLocal");
        }
    }

    private static ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1440,900");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--allow-file-access-from-files");
        log.debug("ChromeOptions configured: headless, no-sandbox, window=1440x900");
        return options;
    }
}
