package com.crd.rebalancing.pages;

import com.crd.rebalancing.models.Security;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object Model for the Portfolio Rebalancing Calculator (index.html).
 * All element interactions are encapsulated here; tests never touch the driver directly.
 */
public class RebalancingPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ── Locators ─────────────────────────────────────────────────────────────
    private final By totalAssetsInput  = By.id("totalAssets");
    private final By calculateBtn      = By.id("calculateBtn");
    private final By resetBtn          = By.id("resetBtn");
    private final By addRowBtn         = By.id("addRowBtn");
    private final By errorDiv          = By.id("error");
    private final By resultsSection    = By.id("resultsSection");
    private final By resultsTableRows  = By.cssSelector("#resultsBody tr");

    public RebalancingPage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public void load(String url) {
        driver.get(url);
        wait.until(ExpectedConditions.visibilityOfElementLocated(calculateBtn));
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void setTotalAssets(double amount) {
        WebElement input = driver.findElement(totalAssetsInput);
        input.clear();
        input.sendKeys(String.valueOf((long) amount));
    }

    /**
     * Clears all existing input rows and fills fresh rows from the given list.
     */
    public void enterSecurities(List<Security> securities) {
        // Clear existing rows via JS (faster than clicking)
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('inputBody').innerHTML = '';");

        for (int i = 0; i < securities.size(); i++) {
            driver.findElement(addRowBtn).click();
        }

        List<WebElement> rows = driver.findElements(By.cssSelector("#inputBody tr"));
        for (int i = 0; i < securities.size(); i++) {
            Security sec = securities.get(i);
            WebElement row = rows.get(i);
            setCell(row, ".security",   sec.getName());
            setCell(row, ".targetPct",  String.valueOf(sec.getTargetPct()));
            setCell(row, ".currentPct", String.valueOf(sec.getCurrentPct()));
            setCell(row, ".unitPrice",  String.valueOf(sec.getUnitPrice()));
        }
    }

    /** Updates a single pre-existing row (0-indexed) in the input table. */
    public void updateSecurityRow(int rowIndex, Security sec) {
        List<WebElement> rows = driver.findElements(By.cssSelector("#inputBody tr"));
        WebElement row = rows.get(rowIndex);
        setCell(row, ".security",   sec.getName());
        setCell(row, ".targetPct",  String.valueOf(sec.getTargetPct()));
        setCell(row, ".currentPct", String.valueOf(sec.getCurrentPct()));
        setCell(row, ".unitPrice",  String.valueOf(sec.getUnitPrice()));
    }

    public boolean isCalculateButtonDisplayed() {
        return driver.findElement(calculateBtn).isDisplayed();
    }

    public boolean isCalculateButtonEnabled() {
        return driver.findElement(calculateBtn).isEnabled();
    }

    public void clickCalculate() {
        driver.findElement(calculateBtn).click();
    }

    public void clickReset() {
        driver.findElement(resetBtn).click();
    }

    public void clickAddRow() {
        driver.findElement(addRowBtn).click();
    }

    // ── Result Readers ────────────────────────────────────────────────────────

    public boolean isResultsTableVisible() {
        try {
            WebElement section = driver.findElement(resultsSection);
            return section.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getErrorMessage() {
        return driver.findElement(errorDiv).getText();
    }

    public int getResultRowCount() {
        return driver.findElements(resultsTableRows).size();
    }

    /**
     * Returns the text of a specific cell in the results table.
     *
     * @param securityName the data-security attribute value
     * @param cssClass     one of: "action", "shares", "variance", "dollarAmount"
     */
    public String getResultCell(String securityName, String cssClass) {
        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("#resultsBody tr[data-security='" + securityName + "']")));
        return row.findElement(By.cssSelector("." + cssClass)).getText().trim();
    }

    public String getAction(String securityName) {
        return getResultCell(securityName, "action");
    }

    public int getShares(String securityName) {
        return Integer.parseInt(getResultCell(securityName, "shares"));
    }

    public double getVariance(String securityName) {
        return Double.parseDouble(getResultCell(securityName, "variance"));
    }

    public double getDollarAmount(String securityName) {
        return Double.parseDouble(getResultCell(securityName, "dollarAmount"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setCell(WebElement row, String cssClass, String value) {
        WebElement cell = row.findElement(By.cssSelector(cssClass));
        cell.clear();
        cell.sendKeys(value);
    }
}
