package com.fyp.qa.base;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UIActionBase {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Actions actions;
    private static final Logger logger = LoggerFactory.getLogger(UIActionBase.class);

    public UIActionBase(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.actions = new Actions(driver);
    }

    public UIActionBase openURL(String Url){
        try{
            driver.get(Url);
            logger.info("Opened URL: {}", Url);
        }catch(Exception e) {
            logger.error("Failed to open URL: {}", Url, e);
        }
        return this;
    }

    public UIActionBase click(By by){
        try{
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(by));
            element.click();
            logger.info("Clicked on element: {}", by);
        }catch(Exception e){
            logger.error("Failed to click on element: {}", by, e);
        }
        return this;
    }

    public UIActionBase sendKeys(By by,String data){
        try{
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            element.clear();
            element.sendKeys(data);
        }catch(Exception e){
            logger.error("Failed to send keys to element: {} with data: {}", by, data, e);
        }
        return this;
    }

    public UIActionBase clear(By by){
        try{
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            element.clear();
            logger.info("Cleared element: {}", by);
        }catch(Exception e){
            logger.error("Failed to clear element: {}", by, e);
        }
        return this;
    }



}
