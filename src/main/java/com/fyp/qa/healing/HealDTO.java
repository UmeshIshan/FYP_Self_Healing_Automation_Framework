package com.fyp.qa.healing;

import java.util.List;

public class HealDTO {

    public static class HealRequest {
        public OldElement old;
        public List<Candidate> candidates;

        public HealRequest() {}

        public HealRequest(OldElement old, List<Candidate> candidates) {
            this.old = old;
            this.candidates = candidates;
        }
    }

    public static class OldElement {
        public String text = "";
        public String tag = "";

        // ADD THESE
        public String xpath = "";
        public String intent = "";

        public int idx = 0;

        public OldElement() {}

        public OldElement(String text, String tag, String xpath, String intent, int idx) {
            this.text = text;
            this.tag = tag;
            this.xpath = xpath;
            this.intent = intent;
            this.idx = idx;
        }
    }

    public static class Candidate {
        public String xpath = "";
        public String text = "";
        public String tag = "";
        public Integer idx = 0;
        public String ariaLabel = "";

        public String id = "";
        public String name = "";
        public String className = "";
        public String placeholder = "";
        public String type = "";
        public String value = "";
        public String dataTestId = "";


        public Candidate() {}

        public Candidate(String xpath, String text, String tag, Integer idx, String ariaLabel) {
            this.xpath = xpath;
            this.text = text;
            this.tag = tag;
            this.idx = idx;
            this.ariaLabel = ariaLabel;

            // default values for new fields
            this.id = "";
            this.name = "";
            this.className = "";
            this.placeholder = "";
            this.type = "";
            this.dataTestId = "";
            this.value = "";
        }

        // ✅ NEW OVERLOADED CONSTRUCTOR (use this from CandidateExtractor)
        public Candidate(
                String xpath,
                String text,
                String tag,
                Integer idx,
                String ariaLabel,
                String id,
                String name,
                String className,
                String placeholder,
                String type,
                String value,
                String dataTestId
        ) {
            this.xpath = xpath;
            this.text = text;
            this.tag = tag;
            this.idx = idx;
            this.ariaLabel = safe(ariaLabel);

            this.id = safe(id);
            this.name = safe(name);
            this.className = safe(className);
            this.placeholder = safe(placeholder);
            this.type = safe(type);
            this.value = safe(value);
            this.dataTestId = safe(dataTestId);
        }

        private static String safe(String v) {
            return v == null ? "" : v;
        }
    }


    public static class HealResponse {
        public String healed_xpath;
        public double confidence;
        public String decision;

        public HealResponse() {}
    }
}
