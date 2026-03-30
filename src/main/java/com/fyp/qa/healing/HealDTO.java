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
        // original fields
        public String text   = "";
        public String tag    = "";
        public String xpath  = "";
        public String intent = "";
        public int    idx    = 0;

        // add all fields the Python API expects for OldElement.
        // role_match, label_seqsim, ancestor_jaccard are always 0.
        public String id             = "";
        public String name           = "";
        public String className      = "";
        public String placeholder    = "";
        public String type           = "";
        public String value          = "";
        public String ariaLabel      = "";
        public String dataTestId     = "";
        public String role           = "";
        public String title          = "";
        public String labelText      = "";
        public String headingContext = "";
        public String parentText     = "";
        public String siblingText    = "";
        public String ancestorChain  = "";
        public double domDepth       = 0.0;

        public OldElement() {}

        // original constructor kept for compatibility
        public OldElement(String text, String tag, String xpath, String intent, int idx) {
            this.text   = text;
            this.tag    = tag;
            this.xpath  = xpath;
            this.intent = intent;
            this.idx    = idx;
        }
    }

    public static class Candidate {
        public String  xpath       = "";
        public String  text        = "";
        public String  tag         = "";
        public Integer idx         = 0;
        public String  ariaLabel   = "";
        public String  id          = "";
        public String  name        = "";
        public String  className   = "";
        public String  placeholder = "";
        public String  type        = "";
        public String  value       = "";
        public String  dataTestId  = "";
        public String  role           = "";
        public String  title          = "";
        public String  labelText      = "";
        public String  parentText     = "";
        public String  headingContext = "";
        public String  ancestorChain  = "";
        public double  domDepth       = 0.0;
        public String  siblingBefore  = "";
        public String  siblingAfter   = "";
        public String  nearbyText     = "";
        public boolean isVisible      = true;
        public boolean isEnabled      = true;
        public double  bboxX          = 0.0;
        public double  bboxY          = 0.0;
        public double  bboxW          = 0.0;
        public double  bboxH          = 0.0;

        public Candidate() {}

        public Candidate(String xpath, String text, String tag, Integer idx, String ariaLabel) {
            this.xpath = xpath; this.text = text; this.tag = tag;
            this.idx = idx; this.ariaLabel = safe(ariaLabel);
        }

        public Candidate(
                String xpath, String text, String tag, Integer idx, String ariaLabel,
                String id, String name, String className, String placeholder,
                String type, String value, String dataTestId) {
            this.xpath=xpath; this.text=text; this.tag=tag; this.idx=idx;
            this.ariaLabel=safe(ariaLabel); this.id=safe(id); this.name=safe(name);
            this.className=safe(className); this.placeholder=safe(placeholder);
            this.type=safe(type); this.value=safe(value); this.dataTestId=safe(dataTestId);
        }

        public Candidate(
                String xpath, String text, String tag, Integer idx, String ariaLabel,
                String id, String name, String className, String placeholder,
                String type, String value, String dataTestId,
                String role, String title, String labelText,
                String parentText, String headingContext, String ancestorChain,
                double domDepth, String siblingBefore, String siblingAfter,
                String nearbyText, boolean isVisible, boolean isEnabled,
                double bboxX, double bboxY, double bboxW, double bboxH) {
            this.xpath=xpath; this.text=text; this.tag=tag; this.idx=idx;
            this.ariaLabel=safe(ariaLabel); this.id=safe(id); this.name=safe(name);
            this.className=safe(className); this.placeholder=safe(placeholder);
            this.type=safe(type); this.value=safe(value); this.dataTestId=safe(dataTestId);
            this.role=safe(role); this.title=safe(title); this.labelText=safe(labelText);
            this.parentText=safe(parentText); this.headingContext=safe(headingContext);
            this.ancestorChain=safe(ancestorChain); this.domDepth=domDepth;
            this.siblingBefore=safe(siblingBefore); this.siblingAfter=safe(siblingAfter);
            this.nearbyText=safe(nearbyText); this.isVisible=isVisible;
            this.isEnabled=isEnabled; this.bboxX=bboxX; this.bboxY=bboxY;
            this.bboxW=bboxW; this.bboxH=bboxH;
        }

        private static String safe(String v) { return v == null ? "" : v; }
    }

    public static class HealResponse {
        public String healed_xpath;
        public double confidence;
        public String decision;
        public HealResponse() {}
    }
}
