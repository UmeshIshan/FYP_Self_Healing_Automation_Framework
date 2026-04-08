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
    public double minModelScore = 0.0;
    public boolean requireSanityCheck = true;
    public boolean enableIntentGate = true;
    public boolean intentGateStrict = true;


    // Now reads from env var so it can be tuned without recompiling.
    private static double defaultThreshold() {
        return 0.50d;
    }

    public HealingConfig() {
        this.confidenceThreshold = defaultThreshold();
    }

    public HealingConfig(boolean enabled, String apiUrl, int maxCandidates, int waitSeconds) {
        this.enabled = enabled;
        this.apiUrl = apiUrl;
        this.maxCandidates = maxCandidates;
        this.waitSeconds = waitSeconds;
        this.confidenceThreshold = defaultThreshold();
    }

    public HealingConfig(boolean enabled, String apiUrl, int maxCandidates, int waitSeconds, double confidenceThreshold) {
        this.enabled = enabled;
        this.apiUrl = apiUrl;
        this.maxCandidates = maxCandidates;
        this.waitSeconds = waitSeconds;
        this.confidenceThreshold = confidenceThreshold;
    }
}
