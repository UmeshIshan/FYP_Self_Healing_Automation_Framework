package com.fyp.qa.healing;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SelfHealingEngine {

    private static final Logger logger = LoggerFactory.getLogger(SelfHealingEngine.class);

    private final WebDriver driver;
    private final HealingConfig config;
    private final CandidateExtractor extractor;
    private final HealerClient client;

    public SelfHealingEngine(WebDriver driver, HealingConfig config) {
        this.driver = driver;
        this.config = config;
        this.extractor = new CandidateExtractor(driver);
        this.client = new HealerClient(config.apiUrl);
    }

    public boolean isXPathLocator(By locator) {
        String s = locator.toString();
        return s != null && s.startsWith("By.xpath:");
    }

    public String extractXPath(By locator) {
        return locator.toString().replaceFirst("^By\\.xpath:\\s*", "");
    }

    /**
     * Returns HealResult (with healed xpath & confidence & decision) if healer suggests something,
     * otherwise returns null.
     */
    public HealResult healXPathResult(String oldXpath, String expectedText, String expectedTag, List<HealDTO.Candidate> candidates) throws Exception {
        // candidates are provided by caller (avoid re-extraction)
        HealDTO.OldElement old = new HealDTO.OldElement(
                safe(expectedText),
                safe(expectedTag),
                safe(oldXpath),
                normalizeIntent(extractIntentToken(oldXpath)),
                0
        );

        if (old.text.isBlank()) {
            old.text = "";
        }

        HealDTO.HealRequest req = new HealDTO.HealRequest(old, candidates);

        long start = System.currentTimeMillis();
        logger.info("HEAL: Calling API url={} oldXpathHintText='{}' expectedTag='{}' candidates={}",
                config.apiUrl, expectedText, expectedTag,
                (candidates == null ? 0 : candidates.size()));

        HealDTO.HealResponse resp = client.heal(req);

        // 🔍 DEBUG: API payload candidates preview
        if (candidates != null) {
            int limit = Math.min(candidates.size(), 15); // prevent huge logs
            logger.info("HEAL DEBUG: sending {} candidates to API (showing first {})", candidates.size(), limit);

            for (int i = 0; i < limit; i++) {
                HealDTO.Candidate c = candidates.get(i);
                logger.info("  API_CAND[{}] xpath={} | tag={} | text='{}' | id={} | dataTestId={} | idx={}",
                        i, safe(c.xpath), safe(c.tag), safe(c.text), safe(c.id), safe(c.dataTestId), c.idx);
            }
        }


        long ms = System.currentTimeMillis() - start;
        logger.info("HEAL: API returned in {} ms decision={} confidence={} healed={}",
                ms,
                (resp == null ? "null" : resp.decision),
                (resp == null ? -1.0 : resp.confidence),
                (resp == null ? "null" : resp.healed_xpath));

        if (resp == null) {
            HealResult r = new HealResult(null, null, 0.0d, "MANUAL_REVIEW_API_NULL");
            r.reason = "API returned null response";
            return r;
        }
        if (resp.healed_xpath == null || resp.healed_xpath.isBlank()) {
            HealResult r = new HealResult(null, null, resp.confidence, "MANUAL_REVIEW_NO_XPATH");
            r.reason = "API returned empty healed_xpath decision=" + resp.decision;
            return r;
        }

        logger.warn("Healer response: decision={} confidence={} healed_xpath={}",
                resp.decision, resp.confidence, resp.healed_xpath);

        // return structured result regardless of decision — let caller decide based on confidence/decision
        By healedBy = resp.healed_xpath == null || resp.healed_xpath.isBlank() ? null : By.xpath(resp.healed_xpath);
        return new HealResult(healedBy, resp.healed_xpath, resp.confidence, resp.decision);
    }

    /**
     * Single entry: try heal from By locator.
     * Returns HealResult or null.
     */
    public HealResult heal(By originalLocator) {
        if (!config.enabled) return null;
        if (!isXPathLocator(originalLocator)) return null;

        try {
            String oldXpath = extractXPath(originalLocator);

            // Always work from main document (avoid iframe context pollution)
            driver.switchTo().defaultContent();

            if (config.enableDomFallback) {
                HealResult fb = domFallback(oldXpath);
                if (fb != null) return fb;
            }

            // STEP 1: ML heal (existing)
            String expectedTag = inferTagFromXpath(oldXpath);

            // If action is SEND_KEYS, expectedTag should not restrict to wrong tags like span/div
            if (safe(config.actionName).toLowerCase().contains("sendkeys") || safe(config.actionName).toLowerCase().contains("type")) {
                expectedTag = "input";
            }

            String expectedText = normalizeHint(extractBestHintFromXpath(oldXpath));

            // reinforce with canonical intent token (password/username/login/etc.)
            String intentTok = normalizeIntent(extractIntentToken(oldXpath));
            if (!intentTok.isBlank() && !expectedText.toLowerCase().contains(intentTok)) {
                expectedText = (expectedText + " " + intentTok).trim();
            }


            // Extract candidates ONCE (tag-change resistant selector)
            String selector = actionSelector(config.actionName, expectedTag);
            List<HealDTO.Candidate> candidates = extractor.extract(config.maxCandidates, selector);

            // 🔍 DEBUG: Print candidates sent to the API (ranker input)
            if (candidates != null) {
                logger.info("HEAL DEBUG: candidates dump (count={} selector='{}' expectedTag='{}' expectedText='{}')",
                        candidates.size(), selector, expectedTag, expectedText);

                for (int i = 0; i < candidates.size(); i++) {
                    HealDTO.Candidate c = candidates.get(i);

                    logger.info("  CAND[{}] xpath={} | tag={} | text='{}' | id={} | name={} | placeholder='{}' | ariaLabel='{}' | dataTestId={} | idx={}",
                            i,
                            safe(c.xpath),
                            safe(c.tag),
                            safe(c.text),
                            safe(c.id),
                            safe(c.name),
                            safe(c.placeholder),
                            safe(c.ariaLabel),
                            safe(c.dataTestId),
                            c.idx
                    );
                }
            } else {
                logger.info("HEAL DEBUG: candidates is NULL");
            }


            // ML heal using the same candidates (no repeated DOM work)
            HealResult result = healXPathResult(oldXpath, expectedText, expectedTag, candidates);

            if (result == null) return null;


            if (config.enableIntentGate) {
                String oldTok = normalizeIntent(extractIntentToken(oldXpath));
                if (!oldTok.isBlank()) {
                    // Prefer DOM-attribute check; only fallback to string heuristic if DOM lookup fails
                    boolean ok = healedElementContainsToken(result.healedXpath, oldTok) || intentMatches(oldXpath, result.healedXpath);
                    if (!ok) {
                        result.decision = "REJECT_INTENT_MISMATCH";
                        result.reason = "Intent mismatch: old=" + oldTok + " healed=" + result.healedXpath;
                        return result;
                    }
                }
            }

            // STEP 2: hard reject ad/iframe-like heals
            if (isAdLikeXpath(result.healedXpath)) {
                result.decision = "REJECT_AD_IFRAME";
                return result;
            }

            int matches = countMatches(result.healedXpath);
            result.matchCount = matches;   // <-- store for audit/logging

            // VERIFIED override should NOT bypass intent/action correctness
            if (config.allowVerifiedOverride
                    && matches == 1
                    && !"manual_review".equalsIgnoreCase(result.decision)
                    && result.confidence >= config.confidenceThreshold) {

                boolean sane;

                // 1) Action sanity (DOM-based, tag-change resistant)
                try {
                    WebElement el = driver.findElement(By.xpath(result.healedXpath));
                    sane = isAllowedForAction(config.actionName, el);
                } catch (Exception e) {
                    sane = false;
                }

                // 2) Intent sanity (prevents Email -> Username)
                if (sane) {
                    String intent = normalizeIntent(extractIntentToken(oldXpath));

                    // If old xpath contains strong intent but it's not present anywhere on the page, don't auto-heal
                    if (!intent.isBlank() && !anyCandidateContainsToken(candidates, intent)) {
                        result.decision = "MANUAL_REVIEW_NO_INTENT_ON_PAGE";
                        return result;
                    }

                    // If intent exists, healed element must contain it (fuzzy OK)
                    if (!intent.isBlank() && !healedElementContainsToken(result.healedXpath, intent)) {
                        result.decision = "REJECT_INTENT_MISMATCH";
                        return result;
                    }
                }

                if (sane && result.confidence >= config.confidenceThreshold) {
                    result.decision = "AUTO_HEAL_VERIFIED_UNIQUE";
                    return result;
                }
            }


            boolean apiAuto = result.decision != null && result.decision.equalsIgnoreCase("auto_heal");

            // If API said manual_review, do NOT force auto-heal here.
            // Only auto-heal if API is auto_heal and confidence passes threshold.
            if (apiAuto && result.confidence >= config.confidenceThreshold) {
                result.decision = "AUTO_HEAL_CONFIDENT";
            } else {
                result.decision = "MANUAL_REVIEW";
            }
            return result;

        } catch (Exception e) {
            String oldXpathSafe = "";
            try { oldXpathSafe = extractXPath(originalLocator); } catch (Exception ignore) {}

            logger.error("HEAL: Exception during healing. locator={} oldXpath={} apiUrl={} action={} msg={}",
                    originalLocator, oldXpathSafe, config.apiUrl, config.actionName, e.toString(), e);

            HealResult r = new HealResult(null, null, 0.0d, "MANUAL_REVIEW_API_ERROR");
            r.reason = e.getMessage(); // ✅ this will surface the root cause to UIActionBase if you log it
            return r;
        }
    }


    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean isAdLikeXpath(String xp) {
        if (xp == null) return true;
        String s = xp.toLowerCase();
        String[] bad = {
                "google_ads", "googleads", "doubleclick", "googlesyndication",
                "adsbygoogle", "aswift", "adplus", "ad.plus", "gpt",
                "safeframe", "criteo", "openx", "taboola", "outbrain",
                "fixedban"
        };
        for (String b : bad) if (s.contains(b)) return true;
        return false;
    }

    private int countMatches(String xp) {
        try {
            return driver.findElements(By.xpath(xp)).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Very small DOM fallback: tries to recover common “Username/Widgets” style intent. */
    private HealResult domFallback(String oldXpath) {
        if (oldXpath == null || oldXpath.isBlank()) return null;

        // Extract a quoted hint (works for @placeholder='Username', contains(text(),'Widgets'), normalize-space()='X', etc.)
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("'([^']{2,60})'").matcher(oldXpath);
        if (!m.find()) return null;

        String hint = m.group(1).trim();
        if (hint.isBlank()) return null;

        String[] tries = new String[] {
                "//*[@placeholder='" + hint + "']",
                "//*[normalize-space()='" + hint + "']"
        };

        for (String xp : tries) {
            if (isAdLikeXpath(xp)) continue;
            if (countMatches(xp) == 1) {
                return new HealResult(By.xpath(xp), xp, 1.0d, "AUTO_HEAL_DOM_FALLBACK");
            }
        }
        return null;
    }

    private String inferTagFromXpath(String xpath) {
        // Simple heuristic: //input..., //button..., //span..., etc.
        if (xpath == null) return "";
        String x = xpath.trim();
        if (x.startsWith("//")) {
            String after = x.substring(2);
            int end = after.indexOf('[');
            String tag = (end > 0 ? after.substring(0, end) : after).trim();
            // ignore wildcard
            return "*".equals(tag) ? "" : tag;
        }
        return "";
    }

    private String extractBestHintFromXpath(String xpath) {
        // Grab first quoted literal: placeholder/text/id/name/class etc.
        if (xpath == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("'([^']+)'|\"([^\"]+)\"").matcher(xpath);
        if (m.find()) return (m.group(1) != null ? m.group(1) : m.group(2));
        return "";
    }

    private String extractIntentToken(String xpath) {
        if (xpath == null) return "";

        List<String> strong = List.of(
                "password", "passcode", "pwd",
                "username", "user", "userid",
                "email", "mail",
                "login", "signin", "sign-in",
                "submit", "confirm",
                "search", "find",
                "qty", "quantity",
                "cart", "basket"
        );

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("'([^']+)'|\"([^\"]+)\"").matcher(xpath);
        while (m.find()) {
            String lit = (m.group(1) != null ? m.group(1) : m.group(2));
            if (lit == null) continue;
            String t = lit.trim().toLowerCase();
            for (String k : strong) {
                if (t.contains(k)) return k;
            }
        }
        return "";
    }

    private String normalizeIntent(String token) {
        if (token == null) return "";
        token = token.trim().toLowerCase();

        // collapse synonyms to a canonical intent
        return switch (token) {
            // password synonyms
            case "passcode", "pwd", "passwd", "pass", "pin" -> "password";

            // username synonyms
            case "user", "userid", "uname", "loginid" -> "username";

            // login synonyms
            case "signin", "sign-in", "signon", "logon" -> "login";

            // email synonyms
            case "mail", "e-mail", "emailaddress" -> "email";

            // quantity synonyms
            case "qty", "quant", "amount" -> "quantity";

            default -> token;
        };
    }

    private boolean intentMatches(String oldXpath, String healedXpath) {
        String oldTok = normalizeIntent(extractIntentToken(oldXpath));
        if (oldTok.isBlank()) return true; // no intent extracted -> don’t block

        // if healed xpath contains any synonym/canonical form, allow
        String hx = (healedXpath == null) ? "" : healedXpath.toLowerCase();
        if (hx.contains(oldTok)) return true;

        // also accept if healed xpath contains one of known synonyms for that canonical intent
        if ("password".equals(oldTok) && (hx.contains("passcode") || hx.contains("pwd"))) return true;
        if ("username".equals(oldTok) && (hx.contains("user") || hx.contains("userid"))) return true;
        if ("login".equals(oldTok) && (hx.contains("signin") || hx.contains("sign-in"))) return true;

        return false;
    }



    private boolean healedElementContainsToken(String healedXpath, String token) {
        try {
            WebElement e = driver.findElement(By.xpath(healedXpath));
            String blob =
                    (safe(e.getText()) + " " +
                            safe(e.getAttribute("placeholder")) + " " +
                            safe(e.getAttribute("aria-label")) + " " +
                            safe(e.getAttribute("name")) + " " +
                            safe(e.getAttribute("id")) + " " +
                            safe(e.getAttribute("data-testid")) + " " +
                            safe(e.getAttribute("data-test")) + " " +
                            safe(e.getAttribute("data-qa"))).toLowerCase();
            String canon = normalizeIntent(token);

            // allow canonical and known synonyms
            if (fuzzyTokenMatch(blob, canon)) return true;

            if ("password".equals(canon)) {
                return fuzzyTokenMatch(blob, "passcode") || fuzzyTokenMatch(blob, "pwd");
            }
            if ("username".equals(canon)) {
                return fuzzyTokenMatch(blob, "user") || fuzzyTokenMatch(blob, "userid");
            }
            if ("login".equals(canon)) {
                return fuzzyTokenMatch(blob, "signin") || fuzzyTokenMatch(blob, "sign-in");
            }
            if ("email".equals(canon)) {
                return fuzzyTokenMatch(blob, "mail");
            }

            return false;

        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isAllowedForAction(String action, WebElement e) {
        String tag = safe(e.getTagName()).toLowerCase();
        String role = safe(e.getAttribute("role")).toLowerCase();
        String ce = safe(e.getAttribute("contenteditable")).toLowerCase();
        String type = safe(e.getAttribute("type")).toLowerCase();

        action = (action == null ? "" : action.toLowerCase());

        if (action.contains("sendkeys") || action.contains("type")) {
            if (tag.equals("input") || tag.equals("textarea")) return true;
            if (role.equals("textbox")) return true;
            if (ce.equals("true")) return true;
            return false;
        }

        if (action.contains("click") || action.contains("tap")) {
            if (tag.equals("button") || tag.equals("a")) return true;
            if (role.equals("button")) return true;
            // Optional: allow input[type=submit/button]
            if (tag.equals("input") && (type.equals("submit") || type.equals("button"))) return true;
            return false;
        }

        // Default: be strict
        return true;
    }

    private boolean fuzzyTokenMatch(String haystack, String needle) {
        haystack = normalizeTokens(haystack);
        needle = normalizeTokens(needle);

        if (needle.isBlank()) return true;
        if (haystack.contains(needle)) return true;

        java.util.Set<String> h = new java.util.HashSet<>(java.util.Arrays.asList(haystack.split(" ")));
        java.util.Set<String> n = new java.util.HashSet<>(java.util.Arrays.asList(needle.split(" ")));

        n.removeIf(t -> t.length() < 3);
        if (n.isEmpty()) return true;

        int hit = 0;
        for (String t : n) if (h.contains(t)) hit++;

        double overlap = (double) hit / (double) n.size();
        return overlap >= 0.6; // tune 0.5–0.7
    }

    private String normalizeTokens(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String actionSelector(String actionName, String expectedTag) {
        String a = safe(actionName).toLowerCase();

        if (a.contains("sendkeys") || a.contains("type")) {
            return "input,textarea,[role=\"textbox\"],[contenteditable=\"true\"]";
        }

        if (a.contains("click") || a.contains("tap")) {
            return "button,a[href],[role=\"button\"],input[type=\"submit\"],input[type=\"button\"]";
        }

        // fallback
        return safe(expectedTag).isBlank()
                ? "input,textarea,button,a[href],[role],[aria-label],[data-testid],[data-test],[data-qa]"
                : safe(expectedTag);
    }


    private boolean anyCandidateContainsToken(List<HealDTO.Candidate> candidates, String token) {
        if (candidates == null || candidates.isEmpty()) return false;
        String canon = normalizeIntent(token);
        String needle = normalizeTokens(canon);

        for (HealDTO.Candidate c : candidates) {
            String blob = (
                    safe(c.text) + " " +
                            safe(c.ariaLabel) + " " +
                            safe(c.placeholder) + " " +
                            safe(c.name) + " " +
                            safe(c.id) + " " +
                            safe(c.dataTestId) + " " +
                            safe(c.xpath)
            ).toLowerCase();

            if (fuzzyTokenMatch(blob, needle)) return true;

            // synonyms
            if ("password".equals(canon)) {
                if (fuzzyTokenMatch(blob, "passcode") || fuzzyTokenMatch(blob, "pwd")) return true;
            }
            if ("username".equals(canon)) {
                if (fuzzyTokenMatch(blob, "user") || fuzzyTokenMatch(blob, "userid")) return true;
            }
            if ("login".equals(canon)) {
                if (fuzzyTokenMatch(blob, "signin") || fuzzyTokenMatch(blob, "sign-in")) return true;
            }
            if ("email".equals(canon)) {
                if (fuzzyTokenMatch(blob, "mail")) return true;
            }
        }
        return false;
    }


    private String normalizeHint(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase();

        // convert separators into spaces so "sign-in" becomes "sign in"
        s = s.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();

        // canonicalize each token using the same synonym collapsing
        String[] parts = s.split(" ");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            String canon = normalizeIntent(p);
            if (!canon.isBlank()) out.append(canon).append(" ");
        }
        return out.toString().trim();
    }







}
