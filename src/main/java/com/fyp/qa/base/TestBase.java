package com.fyp.qa.base;
import com.fyp.qa.common.Constants;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestBase {
    protected static final Logger LOGGER = Logger.getLogger(TestBase.class.getName());
    protected static WebDriver driver;
    protected static WebDriverWait wait;
    protected static JavascriptExecutor js;
    protected static UIActionBase webUI;


    public static void initialization(){
        if(driver == null){
            if(Constants.BROWSER_NAME.equalsIgnoreCase("chrome")){
                try{
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("--start-maximized");
                    if(Constants.RUN_HEADLESS){
                        chromeOptions.addArguments("--headless", "--window-size=1920,1080");
                    }
                    driver = new ChromeDriver(chromeOptions);
                    js = (JavascriptExecutor) driver;
                    wait = new WebDriverWait(driver, Duration.ofSeconds(30));
                    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
                    driver.get(Constants.MAIN_URL);

                    // Always reset context
                    driver.switchTo().defaultContent();

                    webUI = new UIActionBase(driver,wait);

                    LOGGER.info("WebDriver initialized and navigated to the URL: " + Constants.MAIN_URL);
                }catch(Exception e){
                    LOGGER.log(Level.SEVERE, "Failed to initialize WebDriver", e);
                }
            }else {
                LOGGER.warning("Unsupported browser or WebDriver is already initialized.");
            }
        }
    }

    // Method to close the browser and clean up resources
    public static void closeAllBrowsers() {
        if (driver != null) {
//            driver.close();
            driver.quit();
            driver = null;  // Reset the driver to allow re-initialization in future tests
            LOGGER.info("All browsers are closed.");
        }
    }



}
