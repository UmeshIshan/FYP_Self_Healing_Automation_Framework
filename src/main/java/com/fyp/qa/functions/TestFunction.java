package com.fyp.qa.functions;

import com.fyp.qa.pages.TestPage;

public class TestFunction {
    static TestPage testPage = new TestPage();

    public static void navigateToWidgets(){
        testPage.clickOnWidgets();
    }

    public static void clickOnElements(){
        testPage.clickOnElements();
    }

    public static void userNameInput(String userName){
        testPage.userNameInput(userName);
    }

    public static void passwordInput(String password){
        testPage.passwordInput(password);
    }

    public static void clickLoginBtn(){
        testPage.clickLoginBtn();
    }



}
