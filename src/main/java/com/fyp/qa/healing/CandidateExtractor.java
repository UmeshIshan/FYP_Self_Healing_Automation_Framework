package com.fyp.qa.healing;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CandidateExtractor {

    private final WebDriver driver;

    public CandidateExtractor(WebDriver driver) {
        this.driver = driver;
    }

    public List<HealDTO.Candidate> extract(int maxCandidates) {
        return extract(maxCandidates, "");
    }

    @SuppressWarnings("unchecked")
    public List<HealDTO.Candidate> extract(int maxCandidates, String cssSelector) {

        String js =
                "function safeStr(v){ return (v==null? '': (''+v)).trim(); }\n" +
                        "function isVisible(e){\n" +
                        "  if(!e) return false;\n" +
                        "  const st = window.getComputedStyle(e);\n" +
                        "  if(!st) return false;\n" +
                        "  if(st.display==='none' || st.visibility==='hidden' || parseFloat(st.opacity||'1')===0) return false;\n" +
                        "  const r = e.getBoundingClientRect();\n" +
                        "  return !!r && r.width>=2 && r.height>=2;\n" +
                        "}\n" +
                        "function attr(e,n){ return safeStr(e.getAttribute(n)); }\n" +
                        "function xpathLiteral(s){\n" +
                        "  s = safeStr(s);\n" +
                        "  if (s.indexOf(\"'\")===-1) return \"'\"+s+\"'\";\n" +
                        "  if (s.indexOf('\"')===-1) return '\"'+s+'\"';\n" +
                        "  const parts=s.split(\"'\");\n" +
                        "  const out=[];\n" +
                        "  for(let i=0;i<parts.length;i++){ if(parts[i].length) out.push(\"'\"+parts[i]+\"'\"); if(i!==parts.length-1) out.push('\"\\\\\\''+'\"'); }\n" +
                        "  return 'concat('+out.join(',')+')';\n" +
                        "}\n" +
                        "function stableXPath(e){\n" +
                        "  const tag=(e.tagName||'').toLowerCase();\n" +
                        "  const id=safeStr(e.id);\n" +
                        "  if(id) return '//*[@id='+xpathLiteral(id)+']';\n" +
                        "  const dt=attr(e,'data-testid')||attr(e,'data-test')||attr(e,'data-qa');\n" +
                        "  if(dt) return '//*[(@data-testid='+xpathLiteral(dt)+' or @data-test='+xpathLiteral(dt)+' or @data-qa='+xpathLiteral(dt)+')]';\n" +
                        "  const name=attr(e,'name');\n" +
                        "  if(name) return '//'+tag+'[@name='+xpathLiteral(name)+']';\n" +
                        "  const ph=attr(e,'placeholder');\n" +
                        "  if(ph) return '//'+tag+'[@placeholder='+xpathLiteral(ph)+']';\n" +
                        "  const parts=[];\n" +
                        "  let el=e;\n" +
                        "  while(el && el.nodeType===1){\n" +
                        "    let ix=1; let sib=el.previousSibling;\n" +
                        "    while(sib){ if(sib.nodeType===1 && sib.tagName===el.tagName) ix++; sib=sib.previousSibling; }\n" +
                        "    parts.unshift(el.tagName.toLowerCase()+'['+ix+']');\n" +
                        "    el=el.parentNode;\n" +
                        "  }\n" +
                        "  return '//'+parts.join('/');\n" +
                        "}\n" +
                        "const sel = arguments[0] || 'input,textarea,button,a[href],[role=\"button\"],[role=\"textbox\"],[contenteditable=\"true\"],[aria-label],[data-testid],[data-test],[data-qa]';\n" +
                        "const cap = arguments[1] || 200;\n" +
                        "const els = Array.from(document.querySelectorAll(sel)).filter(isVisible).slice(0, cap);\n" +
                        "return els.map((e,i)=>({\n" +
                        "  xpath: stableXPath(e),\n" +
                        "  text: safeStr(e.innerText || e.value || ''),\n" +
                        "  tag: (e.tagName||'').toLowerCase(),\n" +
                        "  idx: i,\n" +
                        "  ariaLabel: attr(e,'aria-label'),\n" +
                        "  id: safeStr(e.id),\n" +
                        "  name: attr(e,'name'),\n" +
                        "  className: safeStr(e.className),\n" +
                        "  placeholder: attr(e,'placeholder'),\n" +
                        "  type: attr(e,'type'),\n" +
                        "  dataTestId: (attr(e,'data-testid')||attr(e,'data-test')||attr(e,'data-qa'))\n" +
                        "}));";


        Object rawObj = ((JavascriptExecutor) driver).executeScript(js, cssSelector, maxCandidates);
        List<Map<String, Object>> raw = (List<Map<String, Object>>) rawObj;

        List<HealDTO.Candidate> out = new ArrayList<>();
        for (Map<String, Object> r : raw) {
            String xpath = (String) r.get("xpath");
            String text = (String) r.get("text");
            String tag = (String) r.get("tag");
            int idx = ((Number) r.get("idx")).intValue();
            String aria = (String) r.get("ariaLabel");

            // new fields
            String id = (String) r.get("id");
            String name = (String) r.get("name");
            String className = (String) r.get("className");
            String placeholder = (String) r.get("placeholder");
            String type = (String) r.get("type");
            String dataTestId = (String) r.get("dataTestId");

            if (xpath != null && !xpath.isBlank()) {
                // ✅ Use the NEW overloaded constructor (keep your old constructor too)
                out.add(new HealDTO.Candidate(
                        xpath, text, tag, idx, aria,
                        id, name, className, placeholder, type, dataTestId
                ));
            }
        }
        return out;
    }
}
