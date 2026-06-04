package com.crd.rebalancing.tests;

import com.crd.rebalancing.models.Security;
import com.crd.rebalancing.pages.RebalancingPage;
import com.crd.rebalancing.utils.DriverManager;
import com.crd.rebalancing.utils.RebalancingCalculator;
import com.crd.rebalancing.utils.RebalancingCalculator.Action;
import io.qameta.allure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * =====================================================================
 *  Portfolio Rebalancing Calculator – TestNG Test Suite
 *  CRD / Alpha Platform Engineering – Technical Assessment
 * =====================================================================
 *
 *  ASSUMPTION LOG
 *  --------------
 *  A1. Fractional shares are not supported; floor(dollarAmount / unitPrice) is used.
 *  A2. "target variance" in the spec = currentPct - targetPct.
 *      Negative variance → BUY, positive variance → SELL, zero → HOLD.
 *  A3. All percentages refer to % of total portfolio value (not individual).
 *  A4. Total assets of $100,000 is the baseline for the standard scenario.
 *  A5. Unit prices and allocations are point-in-time snapshots (no drift simulation).
 *  A6. The application validates unit price > 0 and total assets > 0.
 *
 *  MANUAL TEST CASES (documented here; automated below)
 *  ─────────────────────────────────────────────────────
 *  TC-M01  Standard scenario: IBM needs BUY 66, ORCL needs SELL 45, rest HOLD 0
 *  TC-M02  Verify "Calculate" button is present and clickable
 *  TC-M03  Verify results table is hidden before calculation
 *  TC-M04  Verify results table appears after clicking Calculate
 *  TC-M05  Verify Reset clears results
 *  TC-M06  Enter unit price of 0 → error message shown, no results
 *  TC-M07  Enter total assets of 0 → error message shown, no results
 *  TC-M08  Enter negative total assets → error message shown
 *  TC-M09  All securities HOLD (all variances = 0) → 0 shares for all
 *  TC-M10  All securities need BUY → all show BUY with correct share counts
 *  TC-M11  All securities need SELL → all show SELL with correct share counts
 *  TC-M12  Verify BUY action for security with negative variance
 *  TC-M13  Verify SELL action for security with positive variance
 *  TC-M14  Verify dollar amount = |variance%| / 100 * totalAssets
 *  TC-M15  Data-driven: different total asset values produce proportionally correct shares
 *  TC-M16  Large portfolio ($10 million) – shares scale linearly
 *  TC-M17  Low unit price ($1) – large share count output
 *  TC-M18  Add a 6th security row and verify it appears in results
 *  TC-M19  Verify page title is "Portfolio Rebalancing Calculator"
 *  TC-M20  Verify each result row is tagged with the correct security name
 * =====================================================================
 */
@Epic("Portfolio Rebalancing Calculator")
@Feature("Rebalancing Engine")
public class RebalancingTest {

    private static final Logger log = LoggerFactory.getLogger(RebalancingTest.class);
    private RebalancingPage page;

    // Standard test data matching the assessment scenario
    private static final double TOTAL_ASSETS = 100_000.0;

    private static final Security IBM  = new Security("IBM",  20, 10, 150);
    private static final Security MSFT = new Security("MSFT", 20, 20, 90);
    private static final Security ORCL = new Security("ORCL", 20, 30, 220);
    private static final Security AAPL = new Security("AAPL", 20, 20, 450);
    private static final Security HD   = new Security("HD",   20, 20, 70);

    private static final List<Security> STANDARD_PORTFOLIO =
            Arrays.asList(IBM, MSFT, ORCL, AAPL, HD);

    private static String APP_URL;

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        File appFile = new File("src/test/resources/index.html").getAbsoluteFile();
        APP_URL = appFile.toURI().toString();
        log.info("App URL resolved: {}", APP_URL);

        DriverManager.getDriver();
        log.info("ChromeDriver initialised for test class");
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        log.debug("Loading application page: {}", APP_URL);
        page = new RebalancingPage(DriverManager.getDriver());
        page.load(APP_URL);
        log.debug("Page loaded successfully");
    }

    @AfterClass(alwaysRun = true)
    public void classTearDown() {
        log.info("Test class complete — quitting ChromeDriver");
        DriverManager.quitDriver();
    }

    // =========================================================================
    //  Page Load & UI Sanity
    // =========================================================================

    /**
     * TC-M03 / TC-01: Results table must be hidden on initial page load.
     */
    @Story("Page Load")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-01: Results section is hidden before calculation")
    public void tc01_resultsSectionHiddenOnLoad() {
        Assert.assertFalse(page.isResultsTableVisible(),
                "Results section should be hidden before Calculate is clicked");
    }

    /**
     * TC-M19 / TC-02: Page title verification.
     */
    @Story("Page Load")
    @Severity(SeverityLevel.MINOR)
    @Test(description = "TC-02: Page title is correct")
    public void tc02_pageTitle() {
        String title = DriverManager.getDriver().getTitle();
        Assert.assertEquals(title, "Portfolio Rebalancing Calculator",
                "Page title mismatch");
    }

    /**
     * TC-M02 / TC-02b: Calculate button is visible and enabled on page load.
     */
    @Story("Calculate Button")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-02b: Calculate button is present and clickable")
    public void tc02b_calculateButtonVisibleAndEnabled() {
        Assert.assertTrue(page.isCalculateButtonDisplayed(),
                "Calculate button should be visible on page load");
        Assert.assertTrue(page.isCalculateButtonEnabled(),
                "Calculate button should be enabled on page load");
    }

    /**
     * TC-M08 / TC-19b: Negative total assets shows error, no results.
     */
    @Story("Input Validation")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-19b: Negative total assets shows error")
    public void tc19b_negativeTotalAssetsShowsError() {
        page.setTotalAssets(-5000);
        page.clickCalculate();
        String error = page.getErrorMessage();
        Assert.assertFalse(error.isEmpty(),
                "An error message should appear when total assets is negative");
        Assert.assertFalse(page.isResultsTableVisible(),
                "Results table should not appear when total assets is negative");
    }

    /**
     * TC-M04 / TC-03: Results table becomes visible after Calculate is clicked.
     */
    @Story("Calculate Button")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-03: Results section appears after Calculate")
    public void tc03_resultsSectionVisibleAfterCalculate() {
        page.clickCalculate(); // default pre-filled data
        Assert.assertTrue(page.isResultsTableVisible(),
                "Results section should be visible after Calculate is clicked");
    }

    /**
     * TC-M05 / TC-04: Reset hides the results table.
     */
    @Story("Reset Button")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-04: Reset clears results table")
    public void tc04_resetHidesResults() {
        page.clickCalculate();
        Assert.assertTrue(page.isResultsTableVisible(), "Pre-condition: results must be visible");
        page.clickReset();
        Assert.assertFalse(page.isResultsTableVisible(),
                "Results section should be hidden after Reset");
    }

    // =========================================================================
    //  Standard Scenario (Assessment Data)
    // =========================================================================

    /**
     * TC-M01 / TC-05: IBM has negative variance → action must be BUY.
     */
    @Story("BUY Action")
    @Severity(SeverityLevel.BLOCKER)
    @Test(description = "TC-05: IBM action is BUY (negative variance)")
    public void tc05_ibmActionIsBuy() {
        page.clickCalculate();
        Assert.assertEquals(page.getAction("IBM"), "BUY",
                "IBM has current 10% < target 20%, action should be BUY");
    }

    /**
     * TC-06: IBM share count = floor(10000 / 150) = 66.
     */
    @Story("Share Count Calculation")
    @Severity(SeverityLevel.BLOCKER)
    @Test(description = "TC-06: IBM shares to buy = 66")
    public void tc06_ibmSharesToBuy() {
        int expected = RebalancingCalculator.calculateShares(IBM, TOTAL_ASSETS);
        page.clickCalculate();
        Assert.assertEquals(page.getShares("IBM"), expected,
                "IBM: floor($10000 / $150) should be 66 shares");
    }

    /**
     * TC-07: MSFT has zero variance → action must be HOLD with 0 shares.
     */
    @Story("HOLD Action")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-07: MSFT action is HOLD with 0 shares")
    public void tc07_msftActionIsHold() {
        page.clickCalculate();
        Assert.assertEquals(page.getAction("MSFT"), "HOLD",
                "MSFT current == target, action should be HOLD");
        Assert.assertEquals(page.getShares("MSFT"), 0,
                "MSFT shares should be 0 when action is HOLD");
    }

    /**
     * TC-M13 / TC-08: ORCL has positive variance → action must be SELL.
     */
    @Story("SELL Action")
    @Severity(SeverityLevel.BLOCKER)
    @Test(description = "TC-08: ORCL action is SELL (positive variance)")
    public void tc08_orclActionIsSell() {
        page.clickCalculate();
        Assert.assertEquals(page.getAction("ORCL"), "SELL",
                "ORCL has current 30% > target 20%, action should be SELL");
    }

    /**
     * TC-09: ORCL share count = floor(10000 / 220) = 45.
     */
    @Story("Share Count Calculation")
    @Severity(SeverityLevel.BLOCKER)
    @Test(description = "TC-09: ORCL shares to sell = 45")
    public void tc09_orclSharesToSell() {
        int expected = RebalancingCalculator.calculateShares(ORCL, TOTAL_ASSETS);
        page.clickCalculate();
        Assert.assertEquals(page.getShares("ORCL"), expected,
                "ORCL: floor($10000 / $220) should be 45 shares");
    }

    /**
     * TC-10: AAPL has zero variance → HOLD with 0 shares.
     */
    @Story("HOLD Action")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-10: AAPL action is HOLD with 0 shares")
    public void tc10_aaplHold() {
        page.clickCalculate();
        Assert.assertEquals(page.getAction("AAPL"), "HOLD");
        Assert.assertEquals(page.getShares("AAPL"), 0);
    }

    /**
     * TC-11: HD has zero variance → HOLD with 0 shares.
     */
    @Story("HOLD Action")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-11: HD action is HOLD with 0 shares")
    public void tc11_hdHold() {
        page.clickCalculate();
        Assert.assertEquals(page.getAction("HD"), "HOLD");
        Assert.assertEquals(page.getShares("HD"), 0);
    }

    /**
     * TC-12: Standard scenario produces exactly 5 result rows.
     */
    @Story("Results Table")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-12: Results table has 5 rows for 5 securities")
    public void tc12_resultRowCount() {
        page.clickCalculate();
        Assert.assertEquals(page.getResultRowCount(), 5,
                "Standard portfolio has 5 securities; results should have 5 rows");
    }

    // =========================================================================
    //  Dollar Amount Validation
    // =========================================================================

    /**
     * TC-M14 / TC-13: IBM dollar amount = |10-20|/100 * 100000 = $10,000.
     */
    @Story("Dollar Amount Calculation")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-13: IBM dollar amount to trade = $10,000")
    public void tc13_ibmDollarAmount() {
        double expected = RebalancingCalculator.calculateDollarAmount(IBM, TOTAL_ASSETS);
        page.clickCalculate();
        Assert.assertEquals(page.getDollarAmount("IBM"), expected, 0.01,
                "IBM dollar amount should be $10,000.00");
    }

    /**
     * TC-14: ORCL dollar amount = |30-20|/100 * 100000 = $10,000.
     */
    @Story("Dollar Amount Calculation")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-14: ORCL dollar amount to trade = $10,000")
    public void tc14_orclDollarAmount() {
        double expected = RebalancingCalculator.calculateDollarAmount(ORCL, TOTAL_ASSETS);
        page.clickCalculate();
        Assert.assertEquals(page.getDollarAmount("ORCL"), expected, 0.01,
                "ORCL dollar amount should be $10,000.00");
    }

    /**
     * TC-15: MSFT dollar amount = 0 when variance is 0.
     */
    @Story("Dollar Amount Calculation")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-15: MSFT dollar amount = $0 for zero variance")
    public void tc15_msftDollarAmountZero() {
        page.clickCalculate();
        Assert.assertEquals(page.getDollarAmount("MSFT"), 0.0, 0.01,
                "MSFT dollar amount should be $0.00 when no rebalancing needed");
    }

    // =========================================================================
    //  Variance Display
    // =========================================================================

    /**
     * TC-16: IBM displayed variance = -(current - target) = -(10-20) = +10 (need to buy 10%).
     */
    @Story("Variance Display")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-16: IBM variance displayed as +10")
    public void tc16_ibmVarianceDisplay() {
        page.clickCalculate();
        // Page shows -(variance) so a negative variance (need to buy) shows as positive
        Assert.assertEquals(page.getVariance("IBM"), 10.0, 0.01,
                "IBM variance should display as 10 (10% short of target)");
    }

    /**
     * TC-17: ORCL displayed variance = -(30-20) = -10 (10% over target).
     */
    @Story("Variance Display")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-17: ORCL variance displayed as -10")
    public void tc17_orclVarianceDisplay() {
        page.clickCalculate();
        Assert.assertEquals(page.getVariance("ORCL"), -10.0, 0.01,
                "ORCL variance should display as -10 (10% over target)");
    }

    // =========================================================================
    //  Boundary & Edge Cases
    // =========================================================================

    /**
     * TC-M06 / TC-18: Unit price of 0 triggers validation error.
     */
    @Story("Input Validation")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-18: Unit price = 0 shows error, no results")
    public void tc18_zeroPriceShowsError() {
        page.updateSecurityRow(0, new Security("IBM", 20, 10, 0));
        page.clickCalculate();
        String error = page.getErrorMessage();
        Assert.assertFalse(error.isEmpty(),
                "An error message should appear when unit price is 0");
        Assert.assertFalse(page.isResultsTableVisible(),
                "Results table should not appear when input is invalid");
    }

    /**
     * TC-M07 / TC-19: Total assets = 0 triggers validation error.
     */
    @Story("Input Validation")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-19: Total assets = 0 shows error")
    public void tc19_zeroTotalAssetsShowsError() {
        page.setTotalAssets(0);
        page.clickCalculate();
        String error = page.getErrorMessage();
        Assert.assertFalse(error.isEmpty(),
                "An error message should appear when total assets = 0");
    }

    /**
     * TC-M16 / TC-20: Large portfolio ($10M) – IBM shares = floor(1,000,000 / 150) = 6,666.
     */
    @Story("Portfolio Scaling")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-20: Large portfolio $10M – IBM BUY 6666 shares")
    public void tc20_largePortfolioScalesCorrectly() {
        double largeAssets = 10_000_000.0;
        page.setTotalAssets(largeAssets);
        page.clickCalculate();

        int expected = RebalancingCalculator.calculateShares(IBM, largeAssets);
        Assert.assertEquals(page.getShares("IBM"), expected,
                "IBM shares should scale linearly with total assets");
    }

    /**
     * TC-M17 / TC-21: Low unit price ($1) results in large share count.
     * Security with 10% variance on $100K = $10,000 / $1 = 10,000 shares.
     */
    @Story("Portfolio Scaling")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-21: Low unit price ($1) yields large share count")
    public void tc21_lowUnitPriceLargeShares() {
        Security cheapStock = new Security("IBM", 20, 10, 1);
        page.updateSecurityRow(0, cheapStock);
        page.clickCalculate();

        int expected = RebalancingCalculator.calculateShares(cheapStock, TOTAL_ASSETS);
        Assert.assertEquals(page.getShares("IBM"), expected,
                "Should produce 10,000 shares for a $1 stock with 10% variance on $100K");
    }

    /**
     * TC-M09 / TC-22: All securities at target (zero variance) → all HOLD with 0 shares.
     */
    @Story("HOLD Action")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-22: All zero variance → all HOLD 0 shares")
    public void tc22_allZeroVarianceAllHold() {
        // Set all securities to exactly their target (no deviation)
        List<Security> balanced = Arrays.asList(
                new Security("IBM",  20, 20, 150),
                new Security("MSFT", 20, 20, 90),
                new Security("ORCL", 20, 20, 220),
                new Security("AAPL", 20, 20, 450),
                new Security("HD",   20, 20, 70)
        );
        page.enterSecurities(balanced);
        page.clickCalculate();

        for (Security sec : balanced) {
            Assert.assertEquals(page.getAction(sec.getName()), "HOLD",
                    sec.getName() + " should be HOLD");
            Assert.assertEquals(page.getShares(sec.getName()), 0,
                    sec.getName() + " shares should be 0");
        }
    }

    /**
     * TC-M10 / TC-23: All securities under target → all BUY.
     */
    @Story("BUY Action")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-23: All securities under target → all BUY")
    public void tc23_allSecuritiesNeedBuy() {
        List<Security> allBuy = Arrays.asList(
                new Security("IBM",  25, 15, 150),
                new Security("MSFT", 25, 15, 90),
                new Security("ORCL", 25, 15, 220),
                new Security("AAPL", 25, 15, 450)
        );
        page.enterSecurities(allBuy);
        page.clickCalculate();

        for (Security sec : allBuy) {
            Assert.assertEquals(page.getAction(sec.getName()), "BUY",
                    sec.getName() + " should be BUY when current < target");
        }
    }

    /**
     * TC-M11 / TC-24: All securities over target → all SELL.
     */
    @Story("SELL Action")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-24: All securities over target → all SELL")
    public void tc24_allSecuritiesNeedSell() {
        List<Security> allSell = Arrays.asList(
                new Security("IBM",  15, 25, 150),
                new Security("MSFT", 15, 25, 90),
                new Security("ORCL", 15, 25, 220),
                new Security("AAPL", 15, 25, 450)
        );
        page.enterSecurities(allSell);
        page.clickCalculate();

        for (Security sec : allSell) {
            Assert.assertEquals(page.getAction(sec.getName()), "SELL",
                    sec.getName() + " should be SELL when current > target");
        }
    }

    // =========================================================================
    //  Data-Driven Tests
    // =========================================================================

    /**
     * TC-M15 / TC-25 (data-driven): Verify correct share counts across different total asset values.
     *
     * Security: IBM, target=20%, current=10%, price=$150
     * Expected: floor(|variance%| / 100 * totalAssets / unitPrice)
     */
    @Story("Share Count Calculation")
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "totalAssetsProvider",
          description = "TC-25: IBM share count is correct across different portfolio sizes")
    public void tc25_ibmSharesForDifferentPortfolioSizes(double totalAssets, int expectedShares) {
        page.setTotalAssets(totalAssets);
        page.clickCalculate();
        Assert.assertEquals(page.getShares("IBM"), expectedShares,
                "IBM shares incorrect for total assets = $" + totalAssets);
    }

    @DataProvider(name = "totalAssetsProvider")
    public Object[][] totalAssetsProvider() {
        // { totalAssets, expectedIbmShares }
        // IBM: 10% variance, $150 unit price
        // expected = floor(totalAssets * 0.10 / 150)
        return new Object[][] {
                { 100_000,   66   },   // floor(10000 / 150)
                { 200_000,  133   },   // floor(20000 / 150)
                { 50_000,    33   },   // floor(5000  / 150)
                { 1_500_000, 1000 },   // floor(150000 / 150)
                { 1_000,       0  },   // floor(100 / 150) = 0 (less than 1 share)
        };
    }

    /**
     * TC-26 (data-driven): Verify share counts for different securities at fixed $100K.
     */
    @Story("Share Count Calculation")
    @Severity(SeverityLevel.BLOCKER)
    @Test(dataProvider = "securitySharesProvider",
          description = "TC-26: Correct shares for each security in standard scenario")
    public void tc26_correctSharesPerSecurity(String securityName, int expectedShares, String expectedAction) {
        page.clickCalculate();
        Assert.assertEquals(page.getShares(securityName), expectedShares,
                securityName + " share count mismatch");
        Assert.assertEquals(page.getAction(securityName), expectedAction,
                securityName + " action mismatch");
    }

    @DataProvider(name = "securitySharesProvider")
    public Object[][] securitySharesProvider() {
        return new Object[][] {
                // { securityName, expectedShares, expectedAction }
                { "IBM",  66, "BUY"  },   // floor(10000/150)
                { "MSFT",  0, "HOLD" },
                { "ORCL", 45, "SELL" },   // floor(10000/220)
                { "AAPL",  0, "HOLD" },
                { "HD",    0, "HOLD" },
        };
    }

    // =========================================================================
    //  Adding New Securities
    // =========================================================================

    /**
     * TC-M18 / TC-27: Adding a 6th security produces 6 result rows.
     */
    @Story("Add Security")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "TC-27: Add new security row appears in results")
    public void tc27_addedSecurityAppearsInResults() {
        List<Security> sixSecurities = Arrays.asList(
                new Security("IBM",  20, 10, 150),
                new Security("MSFT", 20, 20, 90),
                new Security("ORCL", 20, 30, 220),
                new Security("AAPL", 20, 20, 450),
                new Security("HD",   20, 20, 70),
                new Security("GOOG", 0,  0,  180)
        );
        page.enterSecurities(sixSecurities);
        page.clickCalculate();

        Assert.assertEquals(page.getResultRowCount(), 6,
                "After adding 6th security, results should have 6 rows");
    }

    // =========================================================================
    //  Calculator Logic Unit Verification
    // =========================================================================

    /**
     * TC-28: RebalancingCalculator correctly identifies BUY for negative variance.
     */
    @Story("BUY Action")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-28: Calculator identifies BUY for negative variance")
    public void tc28_calculatorActionBuy() {
        Security underweight = new Security("TEST", 30, 20, 100);
        Assert.assertEquals(RebalancingCalculator.calculateAction(underweight), Action.BUY);
    }

    /**
     * TC-29: RebalancingCalculator correctly identifies SELL for positive variance.
     */
    @Story("SELL Action")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-29: Calculator identifies SELL for positive variance")
    public void tc29_calculatorActionSell() {
        Security overweight = new Security("TEST", 20, 30, 100);
        Assert.assertEquals(RebalancingCalculator.calculateAction(overweight), Action.SELL);
    }

    /**
     * TC-30: RebalancingCalculator correctly identifies HOLD for zero variance.
     */
    @Story("HOLD Action")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-30: Calculator identifies HOLD for zero variance")
    public void tc30_calculatorActionHold() {
        Security balanced = new Security("TEST", 20, 20, 100);
        Assert.assertEquals(RebalancingCalculator.calculateAction(balanced), Action.HOLD);
    }

    /**
     * TC-31: Floor rounding — partial shares are discarded (not rounded up).
     * 10% variance on $100K = $10,000; $10,000 / $150 = 66.67 → floors to 66, not 67.
     */
    @Story("Share Count Calculation")
    @Severity(SeverityLevel.BLOCKER)
    @Test(description = "TC-31: Share count uses floor, not round")
    public void tc31_shareCountUseFloor() {
        // $10,000 / $150 = 66.666... → should be 66
        int shares = RebalancingCalculator.calculateShares(IBM, TOTAL_ASSETS);
        Assert.assertEquals(shares, 66,
                "Fractional shares must be discarded (floor), not rounded");
    }

    /**
     * TC-32: Dollar amount calculation is exactly |variance%| / 100 * totalAssets.
     */
    @Story("Dollar Amount Calculation")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "TC-32: Dollar amount is proportional to variance and total assets")
    public void tc32_dollarAmountFormula() {
        Security sec = new Security("X", 20, 30, 100); // 10% over target
        double dollarAmount = RebalancingCalculator.calculateDollarAmount(sec, 100_000);
        Assert.assertEquals(dollarAmount, 10_000.0, 0.001,
                "Dollar amount should be 10% of $100K = $10,000");
    }
}
