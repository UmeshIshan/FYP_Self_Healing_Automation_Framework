package com.fyp.qa.common;

public class Constants {
    public static String MAIN_URL = "https://www.saucedemo.com/";
    public static boolean RUN_HEADLESS = Boolean.parseBoolean(System.getProperty("run_headless","false"));
    public static String BROWSER_NAME = System.getProperty("browser_name","chrome");
}
