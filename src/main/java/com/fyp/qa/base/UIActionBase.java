package com.fyp.qa.base;

import com.fyp.qa.healing.HealResult;
import com.fyp.qa.healing.HealingConfig;
import com.fyp.qa.healing.SelfHealingEngine;

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

    // ✅ Healing engine lives outside; UIActionBase only calls it on exception
    private final HealingConfig healingConfig;
    private final SelfHealingEngine healingEngine;

    public UIActionBase(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.actions = new Actions(driver);

        // ✅ You can later load from config/properties
        this.healingConfig = new HealingConfig(true, "http://127.0.0.1:8000", 200, 5);
        this.healingEngine = new SelfHealingEngine(driver, healingConfig);
    }

    public UIActionBase openURL(String url) {
        try {
            driver.get(url);
            logger.info("Opened URL: {}", url);
        } catch (Exception e) {
            logger.error("Failed to open URL: {}", url, e);
        }
        return this;
    }

    // ----------------------
    // CLICK (with healing)
    // ----------------------
    public UIActionBase click(By by) {
        try {
            logger.info("CLICK: Attempting locator={}", by);

            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(by));
            element.click();

            logger.info("CLICK: Success locator={}", by);
            return this;

        } catch (TimeoutException | NoSuchElementException | ElementClickInterceptedException e) {
            logger.warn("CLICK: Failed locator={} | exception={}", by, e.getClass().getSimpleName(), e);

            // Try healing
            logger.warn("HEAL: Invoking healer — originalLocator={}", by);
            HealResult result = healingEngine.heal(by);

            if (result == null) {
                logger.error("HEAL: Healing not possible (null result). original={}", by, e);
                throw new RuntimeException("Click failed and healing not possible for locator: " + by, e);
            }

            logger.info("HEALER RESPONSE: original={} healed={} confidence={} decision={}", by, result.healedXpath, result.confidence, result.decision);

            if (shouldAutoHeal(result)) {
                logger.info("HEAL: Accepted healed locator (threshold={}): original={} healed={} confidence={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence);

                try {
                    WebElement healedEl = wait.until(ExpectedConditions.elementToBeClickable(result.healedLocator));
                    healedEl.click();
                    logger.warn("HEAL: Retry success. original={} healed={}", by, result.healedLocator);
                    return this;
                } catch (Exception healEx) {
                    logger.error("HEAL: Retry FAILED. original={} healed={}", by, result.healedLocator, healEx);
                    throw new RuntimeException("Healed click failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                logger.error("HEAL: Rejected healed locator (threshold={}): original={} healed={} confidence={} decision={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence, result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            logger.error("CLICK: Unexpected failure locator={} | exception={}", by, e.getClass().getSimpleName(), e);
            throw new RuntimeException("Unexpected click failure for locator: " + by, e);
        }
    }


    // ----------------------
    // SEND KEYS (with healing)
    // ----------------------
    public UIActionBase sendKeys(By by, String data) {
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            element.clear();
            element.sendKeys(data);
            logger.info("Sent keys to element: {} data={}", by, data);

        } catch (TimeoutException | NoSuchElementException e) {
            logger.info("SENDKEYS: Failed locator={} | exception={}", by, e.getClass().getSimpleName(), e);

            String healId = "H" + System.currentTimeMillis();
            logger.info("HEAL[{}]: Invoking healer — originalLocator={} apiUrl={}", healId, by, healingConfig.apiUrl);

            long t0 = System.currentTimeMillis();
            HealResult result = healingEngine.heal(by);
            long ms = System.currentTimeMillis() - t0;

            logger.info("HEAL[{}]: Returned in {} ms. resultNull={}", healId, ms, (result == null));

            if (result == null) {
                logger.error("HEAL: Healing not possible (null result). original={}", by, e);
                throw new RuntimeException("sendKeys failed and healing not possible for locator: " + by, e);
            }

            logger.info("HEALER RESPONSE: original={} healed={} confidence={} decision={}", by, result.healedXpath, result.confidence, result.decision);

            if (shouldAutoHeal(result)) {
                logger.info("HEAL: Accepted healed locator (threshold={}): original={} healed={} confidence={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence);
                try {
                    WebElement healedEl = wait.until(ExpectedConditions.visibilityOfElementLocated(result.healedLocator));
                    healedEl.clear();
                    healedEl.sendKeys(data);
                    logger.warn("HEAL: sendKeys success on healed locator. Old={} | Healed={}", by, result.healedLocator);
                } catch (Exception healEx) {
                    logger.error("HEAL: sendKeys on healed FAILED. Old={} | Healed={}", by, result.healedLocator, healEx);
                    throw new RuntimeException("Healed sendKeys failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                logger.error("HEAL: Rejected healed locator (threshold={}): original={} healed={} confidence={} decision={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence, result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            logger.error("SENDKEYS: Unexpected error while sending keys. Locator={} data={}", by, data, e);
            throw new RuntimeException("Unexpected sendKeys failure for locator: " + by, e);
        }

        return this;
    }

    // ----------------------
    // CLEAR (with healing)
    // ----------------------
    public UIActionBase clear(By by) {
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            element.clear();
            logger.info("Cleared element: {}", by);

        } catch (TimeoutException | NoSuchElementException e) {
            logger.warn("CLEAR: Failed locator={} | exception={}", by, e.getClass().getSimpleName(), e);

            logger.warn("HEAL: Invoking healer — originalLocator={}", by);
            HealResult result = healingEngine.heal(by);

            if (result == null) {
                logger.error("HEAL: Healing not possible (null result). original={}", by, e);
                throw new RuntimeException("clear failed and healing not possible for locator: " + by, e);
            }

            logger.info("HEALER RESPONSE: original={} healed={} confidence={} decision={}", by, result.healedXpath, result.confidence, result.decision);

            if (shouldAutoHeal(result)) {
                logger.info("HEAL: Accepted healed locator (threshold={}): original={} healed={} confidence={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence);
                try {
                    WebElement healedEl = wait.until(ExpectedConditions.visibilityOfElementLocated(result.healedLocator));
                    healedEl.clear();
                    logger.warn("HEAL: Cleared healed element. Old={} | Healed={}", by, result.healedLocator);
                } catch (Exception healEx) {
                    logger.error("HEAL: clear on healed FAILED. Old={} | Healed={}", by, result.healedLocator, healEx);
                    throw new RuntimeException("Healed clear failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                logger.error("HEAL: Rejected healed locator (threshold={}): original={} healed={} confidence={} decision={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence, result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            logger.error("CLEAR: Unexpected error while clearing element: {}", by, e);
            throw new RuntimeException("Unexpected clear failure for locator: " + by, e);
        }

        return this;
    }

    // ----------------------
    // OPTIONAL: HOVER (with healing)
    // ----------------------
    public UIActionBase hover(By by) {
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            actions.moveToElement(element).perform();
            logger.info("Hovered on element: {}", by);

        } catch (TimeoutException | NoSuchElementException e) {
            logger.warn("HOVER: Failed locator={} | exception={}", by, e.getClass().getSimpleName(), e);

            logger.warn("HEAL: Invoking healer — originalLocator={}", by);
            HealResult result = healingEngine.heal(by);

            if (result == null) {
                logger.error("HEAL: Healing not possible (null result). original={}", by, e);
                throw new RuntimeException("hover failed and healing not possible for locator: " + by, e);
            }

            logger.info("HEALER RESPONSE: original={} healed={} confidence={} decision={}", by, result.healedXpath, result.confidence, result.decision);

            if (shouldAutoHeal(result)) {
                logger.info("HEAL: Accepted healed locator (threshold={}): original={} healed={} confidence={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence);
                try {
                    WebElement healedEl = wait.until(ExpectedConditions.visibilityOfElementLocated(result.healedLocator));
                    actions.moveToElement(healedEl).perform();
                    logger.warn("HEAL: Hovered healed element. Old={} | Healed={}", by, result.healedLocator);
                } catch (Exception healEx) {
                    logger.error("HEAL: hover on healed FAILED. Old={} | Healed={}", by, result.healedLocator, healEx);
                    throw new RuntimeException("Healed hover failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                logger.error("HEAL: Rejected healed locator (threshold={}): original={} healed={} confidence={} decision={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence, result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            logger.error("HOVER: Unexpected error while hovering element: {}", by, e);
            throw new RuntimeException("Unexpected hover failure for locator: " + by, e);
        }

        return this;
    }

    // ----------------------
    // OPTIONAL: GET TEXT (with healing)
    // ----------------------
    public String getText(By by) {
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            String txt = element.getText();
            logger.info("Got text from element: {} text={}", by, txt);
            return txt;

        } catch (TimeoutException | NoSuchElementException e) {
            logger.warn("GETTEXT: Failed locator={} | exception={}", by, e.getClass().getSimpleName(), e);

            logger.warn("HEAL: Invoking healer — originalLocator={}", by);
            HealResult result = healingEngine.heal(by);

            if (result == null) {
                logger.error("HEAL: Healing not possible (null result). original={}", by, e);
                throw new RuntimeException("getText failed and healing not possible for locator: " + by, e);
            }

            logger.info("HEALER RESPONSE: original={} healed={} confidence={} decision={}", by, result.healedXpath, result.confidence, result.decision);

            if (shouldAutoHeal(result)) {
                logger.info("HEAL: Accepted healed locator (threshold={}): original={} healed={} confidence={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence);
                try {
                    WebElement healedEl = wait.until(ExpectedConditions.visibilityOfElementLocated(result.healedLocator));
                    String txt = healedEl.getText();
                    logger.warn("HEAL: Got text from healed element. Old={} | Healed={} text={}", by, result.healedLocator, txt);
                    return txt;
                } catch (Exception healEx) {
                    logger.error("HEAL: getText on healed FAILED. Old={} | Healed={}", by, result.healedLocator, healEx);
                    throw new RuntimeException("Healed getText failed. Old=" + by + " Healed=" + result.healedLocator, healEx);
                }

            } else {
                logger.error("HEAL: Rejected healed locator (threshold={}): original={} healed={} confidence={} decision={}", healingConfig.confidenceThreshold, by, result.healedXpath, result.confidence, result.decision);
                throw new RuntimeException("Healed locator rejected (low confidence). Old=" + by + " healed=" + result.healedXpath);
            }

        } catch (Exception e) {
            logger.error("GETTEXT: Unexpected error while getting text: {}", by, e);
            throw new RuntimeException("Unexpected getText failure for locator: " + by, e);
        }
    }

    public UIActionBase scrollToElementTillFound(By by) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            List<WebElement> els = driver.findElements(by);

            if (!els.isEmpty()) {
                js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", els.get(0));
                logger.info("Scrolled to element: {}", by);
            } else {
                js.executeScript("window.scrollTo(0, (document.documentElement.scrollHeight || document.body.scrollHeight));");
                logger.info("Scrolled to page end (element not found): {}", by);
            }
        } catch (Exception e) {
            logger.error("Failed to scroll to element: {}", by, e);
        }
        return this;
    }

    private boolean shouldAutoHeal(HealResult r) {
        return r != null
                && r.decision != null
                && r.decision.startsWith("AUTO_HEAL");
    }

}
