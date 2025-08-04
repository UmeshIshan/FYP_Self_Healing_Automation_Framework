package com.fyp.qa.functions;

import com.fyp.qa.pages.TestPage;

public class TestFunction {
    static TestPage testPage = new TestPage();

    public static void navigateToWidgets(){
        testPage.clickOnWidgets();
    }



}
