# Portfolio Rebalancing Calculator – QA Test Suite
**CRD / Alpha Platform Engineering – Technical Assessment**

[![CI Pipeline](https://github.com/bbvines/portfolio-rebalancing-tests/actions/workflows/ci.yml/badge.svg)](https://github.com/bbvines/portfolio-rebalancing-tests/actions/workflows/ci.yml)

📊 **[View Live Allure Report](https://bbvines.github.io/portfolio-rebalancing-tests/allure-report)**

---

## Reports

### 1. Live Allure Report (GitHub Pages)
```
https://bbvines.github.io/portfolio-rebalancing-tests/allure-report
```

### 2. Artifacts (from any CI run)
1. Go to `https://github.com/bbvines/portfolio-rebalancing-tests/actions`
2. Click on any run
3. Scroll down to **Artifacts** and download:

| Artifact | Contents |
|----------|---------|
| `allure-report` | TestNG run HTML report |
| `docker-allure-report` | Docker run HTML report |
| `pmd-report` | PMD code analysis XML |

### 3. PMD Report (local)
```bash
mvn site
open target/site/pmd.html
```

---

## Tech Stack

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Java | 11+ | Test implementation language |
| **Build Tool** | Maven | 3.9.6 | Dependency management and build lifecycle |
| **Test Framework** | TestNG | 7.9.0 | Test execution, data-driven support, lifecycle annotations |
| **UI Automation** | Selenium WebDriver | 4.18.1 | Browser automation via Page Object Model |
| **Driver Management** | WebDriverManager | 5.7.0 | Auto-downloads matching ChromeDriver |
| **Browser** | Google Chrome | Latest | Headless browser for test execution |
| **Reporting** | Allure | 2.27.0 | HTML test reports with Epic/Feature/Story/Severity |
| **Logging** | SLF4J + Logback | 2.0.13 / 1.5.6 | Structured logging to console and file |
| **Code Quality** | PMD | 7.7.0 | Static code analysis, enforces coding standards |
| **Containerisation** | Docker | Latest | Runs tests in isolated amd64 container |
| **CI/CD** | GitHub Actions | - | Automated pipeline: PMD → tests → Docker → Allure |
| **Test Design Pattern** | Page Object Model (POM) | - | Separates page interactions from test logic |

---

## Problem Summary

Account ABC holds $100,000 in total assets across 5 securities. The application calculates how many shares of each security to **buy** or **sell** to reach the target allocation.

| Security | Target % | Current % | Target Variance | Unit Price | Expected Output |
|----------|----------|-----------|-----------------|------------|-----------------|
| IBM      | 20       | 10        | -10 (BUY)       | $150       | **BUY 66 shares**  |
| MSFT     | 20       | 20        | 0 (HOLD)        | $90        | **HOLD 0 shares**  |
| ORCL     | 20       | 30        | +10 (SELL)      | $220       | **SELL 45 shares** |
| AAPL     | 20       | 20        | 0 (HOLD)        | $450       | **HOLD 0 shares**  |
| HD       | 20       | 20        | 0 (HOLD)        | $70        | **HOLD 0 shares**  |

### Calculation Formula
```
variance (%) = currentPct - targetPct
dollarAmount = |variance%| / 100 × totalAssets
shares       = floor(dollarAmount / unitPrice)
action       = BUY  if variance < 0
               SELL if variance > 0
               HOLD if variance = 0
```

### Assumptions
| ID  | Assumption |
|-----|------------|
| A1  | Fractional shares are not allowed — `floor()` is applied to share counts |
| A2  | `targetVariance = currentPct - targetPct`; negative → BUY, positive → SELL |
| A3  | All percentages are of the total portfolio value |
| A4  | Baseline total assets = $100,000 |
| A5  | Prices are point-in-time snapshots; no drift simulation |
| A6  | Application validates: unit price > 0, total assets > 0 |

---

## Manual Test Cases

| TC ID  | Category         | Description                                              | Steps                                                                 | Expected Result                        |
|--------|------------------|----------------------------------------------------------|-----------------------------------------------------------------------|----------------------------------------|
| TC-M01 | Standard         | Standard rebalancing with given assessment data          | Load page → click Calculate with default data                        | IBM=BUY 66, ORCL=SELL 45, rest=HOLD 0 |
| TC-M02 | UI Sanity        | Calculate button is present and clickable                | Load page → verify Calculate button exists and is enabled            | Button is visible and enabled          |
| TC-M03 | UI Sanity        | Results table hidden before calculation                  | Load page → check results section                                     | Results section not displayed          |
| TC-M04 | UI Sanity        | Results table appears after clicking Calculate           | Click Calculate → check results section                              | Results section becomes visible        |
| TC-M05 | UI Sanity        | Reset clears results                                     | Click Calculate → click Reset                                        | Results section hidden again           |
| TC-M06 | Validation       | Unit price = 0 shows error, no results                   | Set IBM unit price to 0 → click Calculate                            | Error message shown, no results table  |
| TC-M07 | Validation       | Total assets = 0 shows error                             | Set total assets to 0 → click Calculate                              | Error message shown                    |
| TC-M08 | Validation       | Negative total assets shows error                        | Set total assets to -5000 → click Calculate                          | Error message shown                    |
| TC-M09 | Boundary         | All zero variance → all HOLD with 0 shares               | Set all current% = target% → Calculate                               | All rows show HOLD and 0 shares        |
| TC-M10 | Boundary         | All securities under target → all BUY                    | Set all current% < target% → Calculate                               | All rows show BUY                      |
| TC-M11 | Boundary         | All securities over target → all SELL                    | Set all current% > target% → Calculate                               | All rows show SELL                     |
| TC-M12 | Calculation      | Negative variance maps to BUY action                     | Set IBM current=10, target=20 → Calculate                            | IBM action = BUY                       |
| TC-M13 | Calculation      | Positive variance maps to SELL action                    | Set ORCL current=30, target=20 → Calculate                           | ORCL action = SELL                     |
| TC-M14 | Calculation      | Dollar amount = \|variance%\| / 100 × totalAssets        | IBM: \|10-20\|/100 × 100000 = $10,000                                | Dollar amount = $10,000.00             |
| TC-M15 | Data-Driven      | Different total asset values produce correct share counts | Vary totalAssets: 50K, 100K, 200K → verify IBM shares                | Proportional share counts              |
| TC-M16 | Boundary         | Large portfolio ($10M) shares scale linearly             | Set totalAssets to 10,000,000 → Calculate                            | IBM BUY 6666 shares                    |
| TC-M17 | Boundary         | Low unit price ($1) yields large share count             | Set IBM price to $1 → Calculate                                      | IBM BUY 10,000 shares                  |
| TC-M18 | UI               | Add 6th security row appears in results                  | Click "+ Add Security" → fill data → Calculate                       | 6 rows in results table                |
| TC-M19 | UI Sanity        | Page title is correct                                    | Load page → read browser title                                       | "Portfolio Rebalancing Calculator"     |
| TC-M20 | UI               | Each result row is tagged with correct security name     | Click Calculate → check data-security attributes                     | Row tags match security names          |
| TC-M21 | Standard         | IBM share count is exactly 66                            | Load page → click Calculate → check IBM shares                       | IBM = 66 shares                        |
| TC-M22 | Standard         | MSFT share count is exactly 0 (HOLD)                     | Load page → click Calculate → check MSFT shares                      | MSFT = 0 shares                        |
| TC-M23 | Standard         | ORCL share count is exactly 45                           | Load page → click Calculate → check ORCL shares                      | ORCL = 45 shares                       |
| TC-M24 | Standard         | AAPL share count is exactly 0 (HOLD)                     | Load page → click Calculate → check AAPL shares                      | AAPL = 0 shares                        |
| TC-M25 | Standard         | HD share count is exactly 0 (HOLD)                       | Load page → click Calculate → check HD shares                        | HD = 0 shares                          |
| TC-M26 | Calculation      | IBM dollar amount to trade = $10,000                     | Load page → click Calculate → check IBM dollar amount                | IBM dollar = $10,000.00                |
| TC-M27 | Calculation      | ORCL dollar amount to trade = $10,000                    | Load page → click Calculate → check ORCL dollar amount               | ORCL dollar = $10,000.00               |
| TC-M28 | Calculation      | MSFT dollar amount = $0 for zero variance                | Load page → click Calculate → check MSFT dollar amount               | MSFT dollar = $0.00                    |
| TC-M29 | Calculation      | IBM variance displays as +10                             | Load page → click Calculate → check IBM variance column              | IBM variance = +10                     |
| TC-M30 | Calculation      | ORCL variance displays as -10                            | Load page → click Calculate → check ORCL variance column             | ORCL variance = -10                    |
| TC-M31 | Calculation      | Fractional shares are floored not rounded                | Set IBM price=$150, variance=10% on $100K → Calculate                | 66 shares (not 67 — floor of 66.67)    |
| TC-M32 | Calculation      | Dollar formula = \|variance%\| / 100 × totalAssets       | Set security with 10% variance on $100K → Calculate                  | Dollar amount = $10,000.00             |

---

## Automated Test Cases

> **Total: 34 test methods → 42 test executions**
> TC-02b and TC-19b were added to complete manual test coverage — hence non-sequential numbering.
> TC-25 (×5) and TC-26 (×5) are data-driven, producing 10 extra executions.

| TC ID  | Manual TC | Method                                          | Assertion |
|--------|-----------|-------------------------------------------------|-----------|
| TC-01  | TC-M03    | `tc01_resultsSectionHiddenOnLoad`               | Results hidden on load |
| TC-02  | TC-M19    | `tc02_pageTitle`                                | Title = "Portfolio Rebalancing Calculator" |
| TC-02b | TC-M02    | `tc02b_calculateButtonVisibleAndEnabled`        | Calculate button visible and enabled on load |
| TC-03  | TC-M04    | `tc03_resultsSectionVisibleAfterCalculate`      | Results visible after Calculate |
| TC-04  | TC-M05    | `tc04_resetHidesResults`                        | Results hidden after Reset |
| TC-05  | TC-M12    | `tc05_ibmActionIsBuy`                           | IBM action = BUY |
| TC-06  | TC-M21    | `tc06_ibmSharesToBuy`                           | IBM shares = 66 |
| TC-07  | TC-M22    | `tc07_msftActionIsHold`                         | MSFT action = HOLD, shares = 0 |
| TC-08  | TC-M13    | `tc08_orclActionIsSell`                         | ORCL action = SELL |
| TC-09  | TC-M23    | `tc09_orclSharesToSell`                         | ORCL shares = 45 |
| TC-10  | TC-M24    | `tc10_aaplHold`                                 | AAPL HOLD, 0 shares |
| TC-11  | TC-M25    | `tc11_hdHold`                                   | HD HOLD, 0 shares |
| TC-12  | TC-M20    | `tc12_resultRowCount`                           | 5 result rows |
| TC-13  | TC-M26    | `tc13_ibmDollarAmount`                          | IBM dollar = $10,000 |
| TC-14  | TC-M27    | `tc14_orclDollarAmount`                         | ORCL dollar = $10,000 |
| TC-15  | TC-M28    | `tc15_msftDollarAmountZero`                     | MSFT dollar = $0 |
| TC-16  | TC-M29    | `tc16_ibmVarianceDisplay`                       | IBM variance = +10 |
| TC-17  | TC-M30    | `tc17_orclVarianceDisplay`                      | ORCL variance = -10 |
| TC-18  | TC-M06    | `tc18_zeroPriceShowsError`                      | Error when unit price = 0 |
| TC-19  | TC-M07    | `tc19_zeroTotalAssetsShowsError`                | Error when total assets = 0 |
| TC-19b | TC-M08    | `tc19b_negativeTotalAssetsShowsError`           | Error when total assets is negative |
| TC-20  | TC-M16    | `tc20_largePortfolioScalesCorrectly`            | IBM = 6666 shares at $10M |
| TC-21  | TC-M17    | `tc21_lowUnitPriceLargeShares`                  | IBM = 10000 shares at $1/share |
| TC-22  | TC-M09    | `tc22_allZeroVarianceAllHold`                   | All HOLD when balanced |
| TC-23  | TC-M10    | `tc23_allSecuritiesNeedBuy`                     | All BUY when underweight |
| TC-24  | TC-M11    | `tc24_allSecuritiesNeedSell`                    | All SELL when overweight |
| TC-25  | TC-M15    | `tc25_ibmSharesForDifferentPortfolioSizes` (×5) | Parameterized: 50K/100K/200K/1.5M/$1K |
| TC-26  | TC-M01    | `tc26_correctSharesPerSecurity` (×5)            | All 5 securities verified in one pass |
| TC-27  | TC-M18    | `tc27_addedSecurityAppearsInResults`            | 6 rows when 6th security added |
| TC-28  | TC-M12    | `tc28_calculatorActionBuy`                      | Calculator: negative variance → BUY |
| TC-29  | TC-M13    | `tc29_calculatorActionSell`                     | Calculator: positive variance → SELL |
| TC-30  | TC-M09    | `tc30_calculatorActionHold`                     | Calculator: zero variance → HOLD |
| TC-31  | TC-M31    | `tc31_shareCountUseFloor`                       | 66.67 floors to 66, not rounded to 67 |
| TC-32  | TC-M32    | `tc32_dollarAmountFormula`                      | Dollar = \|variance%\|/100 × assets |

---

## Project Structure

```
portfolio-rebalancing-tests/
├── Dockerfile                           # Docker image definition (amd64, works on Apple Silicon)
├── .dockerignore                        # Files excluded from Docker build
├── pom.xml                              # Maven build + dependencies
├── testng.xml                           # TestNG suite definition
├── README.md
├── .github/
│   └── workflows/
│       └── ci.yml                       # GitHub Actions CI pipeline
└── src/
    └── test/
        ├── resources/
        │   ├── index.html               # The application under test
        │   └── logback-test.xml         # Logback logging configuration
        └── java/com/crd/rebalancing/
            ├── models/
            │   └── Security.java        # Data model
            ├── pages/
            │   └── RebalancingPage.java # Page Object Model
            ├── tests/
            │   └── RebalancingTest.java # 34 test methods → 42 test executions
            └── utils/
                ├── DriverManager.java           # WebDriver lifecycle (headless Chrome)
                └── RebalancingCalculator.java   # Business logic helper
```

> **Note:** `RebalancingTest.java` contains **34 test methods** producing **42 test executions** (tc25 and tc26 are data-driven with 5 rows each). Covers all 32 manual test cases.

---

## Running the Tests

### Prerequisites
- Java 11+ (project compiled with Java 25)
- Maven 3.6+
- Google Chrome installed

### Run all tests
```bash
mvn test
```

### Run a specific test by name
```bash
mvn test -Dtest=RebalancingTest#tc05_ibmActionIsBuy
mvn test -Dtest=RebalancingTest#tc06_ibmSharesToBuy
```

---

## Logging

The project uses **SLF4J + Logback** for structured logging across all test classes.

### Log output locations

| Output | Location | When |
|--------|----------|------|
| Console | Terminal / CI logs | Every run |
| Log file | `target/logs/test.log` | Every run |
| Rolled logs | `target/logs/test.YYYY-MM-DD.log` | Daily rotation, kept 7 days |

### Log levels

| Logger | Level | Why |
|--------|-------|-----|
| `com.crd.rebalancing` | DEBUG | Full visibility into test setup and teardown |
| Selenium, TestNG, Allure | WARN | Suppresses noisy third-party output |
| Root | INFO | Default for everything else |

### What gets logged

| Class | Log messages |
|-------|-------------|
| `DriverManager` | ChromeDriver initialisation, options configured, session started/quit |
| `RebalancingTest` | App URL resolved, page loaded, class setup/teardown lifecycle |

### Sample console output
```
09:04:46.123 [main] INFO  c.c.r.tests.RebalancingTest - App URL resolved: file:///Users/.../index.html
09:04:46.456 [main] INFO  c.c.r.utils.DriverManager - Initialising ChromeDriver...
09:04:46.789 [main] DEBUG c.c.r.utils.DriverManager - ChromeOptions configured: headless, no-sandbox, window=1440x900
09:04:47.012 [main] INFO  c.c.r.utils.DriverManager - ChromeDriver started successfully
09:04:47.234 [main] INFO  c.c.r.tests.RebalancingTest - ChromeDriver initialised for test class
09:04:47.456 [main] DEBUG c.c.r.tests.RebalancingTest - Loading application page: file:///Users/.../index.html
09:04:47.678 [main] DEBUG c.c.r.tests.RebalancingTest - Page loaded successfully
09:04:58.123 [main] INFO  c.c.r.tests.RebalancingTest - Test class complete — quitting ChromeDriver
09:04:58.234 [main] INFO  c.c.r.utils.DriverManager - Quitting ChromeDriver
```

### View the log file
```bash
# View full log
cat target/logs/test.log

# Follow log in real time while tests run
tail -f target/logs/test.log

# View only errors and warnings
grep -E "WARN|ERROR" target/logs/test.log
```

### Configuration file
Logging is configured in [`src/test/resources/logback-test.xml`](src/test/resources/logback-test.xml).

---

## Docker

Runs the full test suite inside a container — no local Java, Maven, or Chrome needed.

> **Note:** `--platform linux/amd64` is required on Apple Silicon (arm64) because Google Chrome only publishes amd64 Linux packages. Docker Desktop handles the emulation via Rosetta automatically.

### Build the image
```bash
docker build --platform linux/amd64 -t portfolio-rebalancing-tests .
```

### Run all tests
```bash
docker run --platform linux/amd64 --rm \
  -v $(pwd)/target:/app/target \
  portfolio-rebalancing-tests
```
Allure report is written to `target/allure-report/index.html` on your machine.

### Run PMD analysis only
```bash
docker run --platform linux/amd64 --rm portfolio-rebalancing-tests \
  mvn pmd:check --no-transfer-progress
```

### Run a specific test by name
```bash
docker run --platform linux/amd64 --rm portfolio-rebalancing-tests \
  mvn test -Dtest=RebalancingTest#tc05_ibmActionIsBuy --no-transfer-progress
```

### Run clean (clears old Allure results)
```bash
docker run --platform linux/amd64 --rm \
  -v $(pwd)/target:/app/target \
  portfolio-rebalancing-tests mvn clean test allure:report --no-transfer-progress
```

---

## Allure Report

### Generate and open the report
```bash
# Step 1: Run tests (produces target/allure-results/)
mvn test

# Step 2: Generate HTML report
mvn allure:report

# Step 3: Open in browser
mvn allure:open
```

The report is saved at `target/allure-report/index.html`.

### Allure annotations used

| Annotation | Used for |
|------------|----------|
| `@Epic`    | Top-level grouping: "Portfolio Rebalancing Calculator" |
| `@Feature` | Feature area: "Rebalancing Engine" |
| `@Story`   | User story per test: BUY Action, SELL Action, Input Validation, etc. |
| `@Severity`| BLOCKER / CRITICAL / NORMAL / MINOR — reflects business impact |

### Severity breakdown

| Severity | Tests |
|----------|-------|
| BLOCKER  | Share count for IBM (66) and ORCL (45), floor rounding, all BUY/SELL actions for standard scenario |
| CRITICAL | HOLD action, dollar amounts, validation errors, all/none BUY/SELL scenarios |
| NORMAL   | Variance display, row count, portfolio scaling, data-driven sizes |
| MINOR    | Page title |

---

## CI/CD Pipeline

The pipeline is defined in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) and runs on every push or pull request to `main`/`master`.

### Pipeline Flow

```
push / pull_request
        │
        ▼
┌───────────────┐  fails  ┌────────────────────────────────┐
│  Job 1: pmd   │────────►│  Build blocked — nothing runs  │
│ (code quality)│         └────────────────────────────────┘
└───────┬───────┘
        │ passes
        ├──────────────────────────┐
        ▼                          ▼
┌────────────────────┐   ┌─────────────────────────┐
│  Job 2: test       │   │  Job 3: docker           │
│  - Install Chrome  │   │  - Build Docker image    │
│  - mvn test        │   │  - Run tests in Docker   │
│  - allure:report   │   │  - Upload Allure report  │
│  - GitHub Pages    │   └─────────────────────────┘
└────────────────────┘
```

Jobs 2 and 3 run **in parallel** after PMD passes — saving time.

### Job 1 — PMD Code Analysis

| Step | Command | Output |
|------|---------|--------|
| Run PMD | `mvn pmd:check` | Fails build on Priority 1–2 violations |
| Upload report | `actions/upload-artifact@v4` | `pmd.xml` saved for 7 days |

PMD runs **before** tests. If a code quality violation is found, the pipeline stops and tests never execute — enforcing a quality gate.

### Job 2 — TestNG Tests & Allure Report

| Step | Details |
|------|---------|
| Chrome install | Latest stable via `browser-actions/setup-chrome` |
| Run tests | `mvn test` — 34 test methods, 42 total executions |
| Allure report | `mvn allure:report` — generates HTML report |
| Artifacts | `allure-results` and `allure-report` saved for 7 days |
| GitHub Pages | On merge to `main`/`master`, report published to `https://<user>.github.io/<repo>/allure-report` |

### Job 3 — Docker Build & Test

| Step | Details |
|------|---------|
| Docker Buildx setup | `docker/setup-buildx-action@v3` — enables multi-platform builds |
| Build image | `docker build --platform linux/amd64` — builds using `Dockerfile` |
| Run tests | `docker run --platform linux/amd64` — runs all 42 tests inside container |
| Upload report | `docker-allure-report` artifact saved for 7 days |

Validates that the `Dockerfile` builds correctly and all tests pass inside the containerised environment.

### Triggers

| Event | Behaviour |
|-------|-----------|
| Push to `main`/`master` | Full pipeline (all 3 jobs) + GitHub Pages publish |
| Pull request to `main`/`master` | Full pipeline (all 3 jobs), no Pages publish |
| Manual (`workflow_dispatch`) | Full pipeline on demand |

### Artifacts produced per run

| Artifact | Source | Retained |
|----------|--------|---------|
| `pmd-report` | Job 1 | 7 days |
| `allure-results` | Job 2 | 7 days |
| `allure-report` | Job 2 | 7 days |
| `docker-allure-report` | Job 3 | 7 days |

### One-time GitHub Setup

1. Go to repo **Settings → Pages → Source** → select `gh-pages` branch
2. No secrets needed — `GITHUB_TOKEN` is built-in to every repository
