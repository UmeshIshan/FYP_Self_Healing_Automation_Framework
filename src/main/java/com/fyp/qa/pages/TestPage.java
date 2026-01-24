package com.fyp.qa.pages;

import com.fyp.qa.base.TestBase;
import org.openqa.selenium.By;

public class TestPage extends TestBase {
    By widgetOption = By.xpath("//div[contains(., 'Widgets')]");
    By widgetOption2 = By.xpath("//div[contains(text(),'Widg')]");
    By elements = By.xpath("//div[contains(text(),'Elements')]");
    By userNameTxt = By.xpath("//div[@placeholder='Username']");
    By passwordTxt = By.xpath("//input[@id='passcode']");
    By loginBtn = By.xpath("//input[@data-test='login']");



    public void clickOnWidgets(){
        webUI.click(widgetOption);
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
