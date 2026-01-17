package com.fyp.qa.pages;

import com.fyp.qa.base.TestBase;
import org.openqa.selenium.By;

public class TestPage extends TestBase {
    By widgetOption = By.xpath("//div[contains(., 'Widgets')]");
    By widgetOption2 = By.xpath("//div[contains(text(),'Widg')]");
    By elements = By.xpath("//div[contains(text(),'Elements')]");
    By userNameTxt = By.xpath("//div[@placeholder='Username']");
    By passwordTxt = By.xpath("//span[@id='password']");
    By loginBtn = By.xpath("//input[@data-test='login']");



    public void clickOnWidgets(){
//        webUI.scrollToElementTillFound(widgetOption);

        //webUI.openURL("https://calendar.google.com/calendar/u/1/r/week");
        webUI.click(widgetOption);
        //webUI.sendKeys(By.xpath("//h5[contains(text(),'Widgets')]"),"test");
    }

    public void clickOnElements(){
        webUI.click(elements);
    }

    public void userNameInput(String userName){
        webUI.sendKeys(userNameTxt,userName);
    }

    public void passwordInput(String userName){
        webUI.sendKeys(passwordTxt,userName);
    }

    public void clickLoginBtn(){
        webUI.click(loginBtn);
    }

}
