package com.fyp.qa.base;

import com.fyp.qa.healing.HealResult;
import com.fyp.qa.healing.HealingConfig;
import com.fyp.qa.healing.SelfHealingEngine;
import com.fyp.qa.base.RunLogContext;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UIActionBase {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Actions actions;
    private static final Logger logger = LoggerFactory.getLogger(UIActionBase.class);

    // Healing engine
    private final HealingConfig healingConfig;
    private final SelfHealingEngine healingEngine;

    public UIActionBase(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.actions = new Actions(driver);

        // You can set HEAL_API_URL in docker-compose for runner service.
        String apiUrl = System.getenv().getOrDefault("HEAL_API_URL", "http://127.0.0.1:8000");

        // You can later load these from config/properties
        this.healingConfig = new HealingConfig(true, apiUrl, 200, 5);
        this.healingEngine = new SelfHealingEngine(driver, healingConfig);

        // Useful once per session (shows in UI + IntelliJ)
        uiInfo("🧩 UIActionBase initialized | healApi=" + apiUrl + " | threshold=" + healingConfig.confidenceThreshold);
    }

    private void uiInfo(String msg) {
        // Website logs
        RunLogContext.log(msg);
        // IntelliJ / backend logs
        logger.info(msg);
    }

    private void uiWarn(String msg) {
        RunLogContext.log(msg);
        logger.warn(msg);
    }

    private void uiError(String msg, Throwable t) {
        RunLogContext.log(msg);
        logger.error(msg, t);
    }

    private String exBrief(Throwable t) {
        if (t == null) return "";
        String m = t.getMessage();
        if (m == null) m = "";
        // keep UI logs readable
        if (m.length() > 180) m = m.substring(0, 180) + "...";
        return t.getClass().getSimpleName() + (m.isBlank() ? "" : (": " + m));
    }

    public UIActionBase openURL(String url) {
        try {
            driver.get(url);
            uiInfo("🌐 OPEN: " + url);
        } catch (Exception e) {
            uiError("🛑 OPEN failed: " + url + " | " + exBrief(e), e);
            throw new RuntimeException("Failed to open URL: " + url, e);
        }
        return this;
    }

    // ----------------------
    // CLICK (with healing)
    // ----------------------
    public UIActionBase click(By by) {
        try {
            uiInfo("➡️ CLICK: " + by);

            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(by));
            element.click();

            uiInfo("✅ CLICK success: " + by);
            return this;

        } catch (TimeoutException | NoSuchElementException | ElementClickInterceptedException e) {
            uiWarn("❌ CLICK failed: " + by + " | " + exBrief(e));

            // Try healing
            uiWarn("🩹 HEAL(click) start | original=" + by);
            HealResult result = healingEngine.heal(by);

            if (result == null) {
                uiError("🛑 HEAL(click) null result | original=" + by, e);
                throw new RuntimeException("Click failed and healing not possible for locator: " + by, e);
            }

            uiInfo("🧠 HEAL(click) response | original=" + by
                    + " | healedXpath=" + result.healedXpath
                    + " | confidence=" + result.confidence
                    + " | decision=" + result.decision);

            if (shouldAutoHeal(result)) {
                uiInfo("✅ HEAL(click) accepted | threshold=" + healingConfig.confidenceThreshold
                        + " | original=" + by
                        + " | healed=" + result.healedLocator
                        + " | confidence=" + result.confidence);

                try {
                    WebElement healedEl = wait.until(ExpectedConditions.elementToBeClickable(result.healedLocator));
                    healedEl.click();
                    uiInfo("✅ HEAL(click) retry success | healed=" + result.healedLocator);
                    return this;

                } catch (Exception healEx) {
                    uiError("🛑 HEAL(click) retry failed | original=" + by
                            + " | healed=" + result.healedLocator
                            + " | " + exBrief(healEx), healEx);
                    throw new RuntimeException("Healed click failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                uiWarn("⛔ HEAL(click) rejected | threshold=" + healingConfig.confidenceThreshold
                        + " | original=" + by
                        + " | healedXpath=" + result.healedXpath
                        + " | confidence=" + result.confidence
                        + " | decision=" + result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            uiError("🛑 CLICK unexpected error: " + by + " | " + exBrief(e), e);
            throw new RuntimeException("Unexpected click failure for locator: " + by, e);
        }
    }

    // ----------------------
    // SEND KEYS (with healing)
    // ----------------------
    public UIActionBase sendKeys(By by, String data) {
        try {
            uiInfo("➡️ SENDKEYS: " + by + " | dataLen=" + (data == null ? 0 : data.length()));

            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            element.clear();
            element.sendKeys(data);

            uiInfo("✅ SENDKEYS success: " + by);

        } catch (TimeoutException | NoSuchElementException e) {
            uiWarn("❌ SENDKEYS failed: " + by + " | " + exBrief(e));

            String healId = "H" + System.currentTimeMillis();
            uiWarn("🩹 HEAL(sendKeys)[" + healId + "] start | original=" + by + " | apiUrl=" + healingConfig.apiUrl);

            long t0 = System.currentTimeMillis();
            HealResult result = healingEngine.heal(by);
            long ms = System.currentTimeMillis() - t0;

            uiInfo("🕒 HEAL(sendKeys)[" + healId + "] returned in " + ms + "ms | resultNull=" + (result == null));

            if (result == null) {
                uiError("🛑 HEAL(sendKeys)[" + healId + "] null result | original=" + by, e);
                throw new RuntimeException("sendKeys failed and healing not possible for locator: " + by, e);
            }

            uiInfo("🧠 HEAL(sendKeys)[" + healId + "] response | original=" + by
                    + " | healedXpath=" + result.healedXpath
                    + " | confidence=" + result.confidence
                    + " | decision=" + result.decision);

            if (shouldAutoHeal(result)) {
                uiInfo("✅ HEAL(sendKeys)[" + healId + "] accepted | threshold=" + healingConfig.confidenceThreshold
                        + " | healed=" + result.healedLocator
                        + " | confidence=" + result.confidence);

                try {
                    WebElement healedEl = wait.until(ExpectedConditions.visibilityOfElementLocated(result.healedLocator));
                    healedEl.clear();
                    healedEl.sendKeys(data);
                    uiInfo("✅ HEAL(sendKeys)[" + healId + "] retry success | healed=" + result.healedLocator);

                } catch (Exception healEx) {
                    uiError("🛑 HEAL(sendKeys)[" + healId + "] retry failed | original=" + by
                            + " | healed=" + result.healedLocator
                            + " | " + exBrief(healEx), healEx);
                    throw new RuntimeException("Healed sendKeys failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                uiWarn("⛔ HEAL(sendKeys)[" + healId + "] rejected | threshold=" + healingConfig.confidenceThreshold
                        + " | healedXpath=" + result.healedXpath
                        + " | confidence=" + result.confidence
                        + " | decision=" + result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            uiError("🛑 SENDKEYS unexpected error: " + by + " | " + exBrief(e), e);
            throw new RuntimeException("Unexpected sendKeys failure for locator: " + by, e);
        }

        return this;
    }

    // ----------------------
    // CLEAR (with healing)
    // ----------------------
    public UIActionBase clear(By by) {
        try {
            uiInfo("➡️ CLEAR: " + by);

            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            element.clear();

            uiInfo("✅ CLEAR success: " + by);

        } catch (TimeoutException | NoSuchElementException e) {
            uiWarn("❌ CLEAR failed: " + by + " | " + exBrief(e));

            uiWarn("🩹 HEAL(clear) start | original=" + by);
            HealResult result = healingEngine.heal(by);

            if (result == null) {
                uiError("🛑 HEAL(clear) null result | original=" + by, e);
                throw new RuntimeException("clear failed and healing not possible for locator: " + by, e);
            }

            uiInfo("🧠 HEAL(clear) response | original=" + by
                    + " | healedXpath=" + result.healedXpath
                    + " | confidence=" + result.confidence
                    + " | decision=" + result.decision);

            if (shouldAutoHeal(result)) {
                uiInfo("✅ HEAL(clear) accepted | threshold=" + healingConfig.confidenceThreshold
                        + " | healed=" + result.healedLocator
                        + " | confidence=" + result.confidence);

                try {
                    WebElement healedEl = wait.until(ExpectedConditions.visibilityOfElementLocated(result.healedLocator));
                    healedEl.clear();
                    uiInfo("✅ HEAL(clear) retry success | healed=" + result.healedLocator);

                } catch (Exception healEx) {
                    uiError("🛑 HEAL(clear) retry failed | original=" + by
                            + " | healed=" + result.healedLocator
                            + " | " + exBrief(healEx), healEx);
                    throw new RuntimeException("Healed clear failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                uiWarn("⛔ HEAL(clear) rejected | threshold=" + healingConfig.confidenceThreshold
                        + " | healedXpath=" + result.healedXpath
                        + " | confidence=" + result.confidence
                        + " | decision=" + result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            uiError("🛑 CLEAR unexpected error: " + by + " | " + exBrief(e), e);
            throw new RuntimeException("Unexpected clear failure for locator: " + by, e);
        }

        return this;
    }

    // ----------------------
    // OPTIONAL: HOVER (with healing)
    // ----------------------
    public UIActionBase hover(By by) {
        try {
            uiInfo("➡️ HOVER: " + by);

            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            actions.moveToElement(element).perform();

            uiInfo("✅ HOVER success: " + by);

        } catch (TimeoutException | NoSuchElementException e) {
            uiWarn("❌ HOVER failed: " + by + " | " + exBrief(e));

            uiWarn("🩹 HEAL(hover) start | original=" + by);
            HealResult result = healingEngine.heal(by);

            if (result == null) {
                uiError("🛑 HEAL(hover) null result | original=" + by, e);
                throw new RuntimeException("hover failed and healing not possible for locator: " + by, e);
            }

            uiInfo("🧠 HEAL(hover) response | original=" + by
                    + " | healedXpath=" + result.healedXpath
                    + " | confidence=" + result.confidence
                    + " | decision=" + result.decision);

            if (shouldAutoHeal(result)) {
                uiInfo("✅ HEAL(hover) accepted | threshold=" + healingConfig.confidenceThreshold
                        + " | healed=" + result.healedLocator
                        + " | confidence=" + result.confidence);

                try {
                    WebElement healedEl = wait.until(ExpectedConditions.visibilityOfElementLocated(result.healedLocator));
                    actions.moveToElement(healedEl).perform();
                    uiInfo("✅ HEAL(hover) retry success | healed=" + result.healedLocator);

                } catch (Exception healEx) {
                    uiError("🛑 HEAL(hover) retry failed | original=" + by
                            + " | healed=" + result.healedLocator
                            + " | " + exBrief(healEx), healEx);
                    throw new RuntimeException("Healed hover failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                uiWarn("⛔ HEAL(hover) rejected | threshold=" + healingConfig.confidenceThreshold
                        + " | healedXpath=" + result.healedXpath
                        + " | confidence=" + result.confidence
                        + " | decision=" + result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            uiError("🛑 HOVER unexpected error: " + by + " | " + exBrief(e), e);
            throw new RuntimeException("Unexpected hover failure for locator: " + by, e);
        }

        return this;
    }

    // ----------------------
    // OPTIONAL: GET TEXT (with healing)
    // ----------------------
    public String getText(By by) {
        try {
            uiInfo("➡️ GETTEXT: " + by);

            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            String txt = element.getText();

            uiInfo("✅ GETTEXT success: " + by + " | text=" + txt);
            return txt;

        } catch (TimeoutException | NoSuchElementException e) {
            uiWarn("❌ GETTEXT failed: " + by + " | " + exBrief(e));

            uiWarn("🩹 HEAL(getText) start | original=" + by);
            HealResult result = healingEngine.heal(by);

            if (result == null) {
                uiError("🛑 HEAL(getText) null result | original=" + by, e);
                throw new RuntimeException("getText failed and healing not possible for locator: " + by, e);
            }

            uiInfo("🧠 HEAL(getText) response | original=" + by
                    + " | healedXpath=" + result.healedXpath
                    + " | confidence=" + result.confidence
                    + " | decision=" + result.decision);

            if (shouldAutoHeal(result)) {
                uiInfo("✅ HEAL(getText) accepted | threshold=" + healingConfig.confidenceThreshold
                        + " | healed=" + result.healedLocator
                        + " | confidence=" + result.confidence);

                try {
                    WebElement healedEl = wait.until(ExpectedConditions.visibilityOfElementLocated(result.healedLocator));
                    String txt = healedEl.getText();
                    uiInfo("✅ HEAL(getText) retry success | healed=" + result.healedLocator + " | text=" + txt);
                    return txt;

                } catch (Exception healEx) {
                    uiError("🛑 HEAL(getText) retry failed | original=" + by
                            + " | healed=" + result.healedLocator
                            + " | " + exBrief(healEx), healEx);
                    throw new RuntimeException("Healed getText failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                uiWarn("⛔ HEAL(getText) rejected | threshold=" + healingConfig.confidenceThreshold
                        + " | healedXpath=" + result.healedXpath
                        + " | confidence=" + result.confidence
                        + " | decision=" + result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            uiError("🛑 GETTEXT unexpected error: " + by + " | " + exBrief(e), e);
            throw new RuntimeException("Unexpected getText failure for locator: " + by, e);
        }
    }

    public UIActionBase scrollToElementTillFound(By by) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            List<WebElement> els = driver.findElements(by);

            if (!els.isEmpty()) {
                js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", els.get(0));
                uiInfo("🧭 SCROLL: element found, scrolled to " + by);
            } else {
                js.executeScript("window.scrollTo(0, (document.documentElement.scrollHeight || document.body.scrollHeight));");
                uiInfo("🧭 SCROLL: element not found, scrolled to page end | locator=" + by);
            }
        } catch (Exception e) {
            uiError("🛑 SCROLL failed | locator=" + by + " | " + exBrief(e), e);
        }
        return this;
    }

    private boolean shouldAutoHeal(HealResult r) {
        return r != null
                && r.decision != null
                && r.decision.startsWith("AUTO_HEAL");
    }
}
