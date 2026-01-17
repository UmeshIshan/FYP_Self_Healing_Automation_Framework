package com.fyp.qa.healing;

public class HealingConfig {
    public boolean enabled;
    public String apiUrl;
    public int maxCandidates;
    public int waitSeconds;
    public double confidenceThreshold;
    public String actionName = "";
    public boolean enableDomFallback = true;
    public boolean allowVerifiedOverride = true;
    public double minModelScore = 0.0;          // raw score gate (temporary)
    public boolean requireSanityCheck = true;   // safer acceptance


    public HealingConfig() {
        // default threshold for accepting healed locators
        this.confidenceThreshold = 0.05d;
    }

    public HealingConfig(boolean enabled, String apiUrl, int maxCandidates, int waitSeconds) {
        this.enabled = enabled;
        this.apiUrl = apiUrl;
        this.maxCandidates = maxCandidates;
        this.waitSeconds = waitSeconds;
        this.confidenceThreshold = 0.05d;
    }

    public HealingConfig(boolean enabled, String apiUrl, int maxCandidates, int waitSeconds, double confidenceThreshold) {
        this.enabled = enabled;
        this.apiUrl = apiUrl;
        this.maxCandidates = maxCandidates;
        this.waitSeconds = waitSeconds;
        this.confidenceThreshold = confidenceThreshold;
    }
}
