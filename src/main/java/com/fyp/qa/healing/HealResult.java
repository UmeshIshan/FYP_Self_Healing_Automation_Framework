package com.fyp.qa.healing;

import org.openqa.selenium.By;

/**
 * Simple POJO representing the healer's response mapped into a usable form.
 */
public class HealResult {
    public By healedLocator;
    public String healedXpath;
    public double confidence;
    public String decision;
    public int matchCount;
    public boolean sanityPassed;
    public String reason;

    public HealResult() {}

    public HealResult(By healedLocator, String healedXpath, double confidence, String decision) {
        this.healedLocator = healedLocator;
        this.healedXpath = healedXpath;
        this.confidence = confidence;
        this.decision = decision;
    }
}

