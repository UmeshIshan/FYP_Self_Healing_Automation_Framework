package com.fyp.qa.pages;

import com.fyp.qa.base.TestBase;
import org.openqa.selenium.By;

public class TestPage extends TestBase {
    By widgetOption = By.xpath("//h5[contains(text(),'Widgets')]");


    public void clickOnWidgets(){
        webUI.click(widgetOption);
    }

}
