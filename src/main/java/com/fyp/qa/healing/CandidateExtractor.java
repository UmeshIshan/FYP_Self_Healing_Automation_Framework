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

                        // ---------- LABEL EXTRACTION ----------
                        "function labelText(e){\n" +
                        "  try {\n" +
                        "    if(!e) return '';\n" +
                        "    if (e.closest) {\n" +
                        "      const lab = e.closest('label');\n" +
                        "      if (lab) return safeStr(lab.innerText);\n" +
                        "    }\n" +
                        "    const id = safeStr(e.id);\n" +
                        "    if(!id) return '';\n" +
                        "    const lab2 = document.querySelector('label[for=\"'+id.replace(/\"/g,'\\\\\"')+'\"]');\n" +
                        "    return safeStr(lab2 ? lab2.innerText : '');\n" +
                        "  } catch(ex) { return ''; }\n" +
                        "}\n" +

                        // ---------- HEADING CONTEXT ----------
                        "function headingContext(e){\n" +
                        "  try {\n" +
                        "    var cur = e.parentElement;\n" +
                        "    var depth = 0;\n" +
                        "    while(cur && depth < 6){\n" +
                        "      var t = (cur.tagName||'').toLowerCase();\n" +
                        "      if(t==='h1'||t==='h2'||t==='h3'||t==='h4'||t==='h5'||t==='h6') return safeStr(cur.innerText);\n" +
                        "      var kids = cur.querySelectorAll ? Array.from(cur.querySelectorAll('h1,h2,h3,h4,h5,h6')) : [];\n" +
                        "      if(kids.length > 0) return safeStr(kids[0].innerText);\n" +
                        "      cur = cur.parentElement; depth++;\n" +
                        "    }\n" +
                        "    return '';\n" +
                        "  } catch(ex){ return ''; }\n" +
                        "}\n" +

                        // ---------- ANCESTOR CHAIN ----------
                        "function ancestorChain(e){\n" +
                        "  try {\n" +
                        "    var parts = []; var cur = e.parentElement; var d = 0;\n" +
                        "    while(cur && d < 5){\n" +
                        "      var t = (cur.tagName||'').toLowerCase();\n" +
                        "      var cls = safeStr(cur.className).split(' ')[0];\n" +
                        "      parts.push(cls ? t+'.'+cls : t);\n" +
                        "      cur = cur.parentElement; d++;\n" +
                        "    }\n" +
                        "    return parts.join(' > ');\n" +
                        "  } catch(ex){ return ''; }\n" +
                        "}\n" +

                        // ---------- DOM DEPTH ----------
                        "function domDepth(e){\n" +
                        "  try {\n" +
                        "    var d = 0; var cur = e.parentElement;\n" +
                        "    while(cur && cur.nodeType===1){ d++; cur = cur.parentElement; }\n" +
                        "    return d;\n" +
                        "  } catch(ex){ return 0; }\n" +
                        "}\n" +

                        // ---------- SIBLING TEXTS ----------
                        "function siblingText(e, direction){\n" +
                        "  try {\n" +
                        "    var texts = []; var sib = direction==='before' ? e.previousElementSibling : e.nextElementSibling;\n" +
                        "    var limit = 2;\n" +
                        "    while(sib && limit-->0){\n" +
                        "      var t = safeStr(sib.innerText||sib.textContent||'');\n" +
                        "      if(t) texts.push(t);\n" +
                        "      sib = direction==='before' ? sib.previousElementSibling : sib.nextElementSibling;\n" +
                        "    }\n" +
                        "    return texts.join(' | ');\n" +
                        "  } catch(ex){ return ''; }\n" +
                        "}\n" +

                        // ---------- NEARBY TEXT ----------
                        "function nearbyText(e){\n" +
                        "  try {\n" +
                        "    var p = e.parentElement;\n" +
                        "    if(!p) return '';\n" +
                        "    var t = safeStr(p.innerText||p.textContent||'');\n" +
                        "    if(t.length > 120) t = t.substring(0,120);\n" +
                        "    return t;\n" +
                        "  } catch(ex){ return ''; }\n" +
                        "}\n" +

                        // ---------- XPATH HELPERS ----------
                        "function xpathLiteral(s){\n" +
                        "  s = safeStr(s);\n" +
                        "  if (s.indexOf(\"'\")===-1) return \"'\"+s+\"'\";\n" +
                        "  if (s.indexOf('\"')===-1) return '\"'+s+'\"';\n" +
                        "  const parts=s.split(\"'\");\n" +
                        "  const out=[];\n" +
                        "  for(let i=0;i<parts.length;i++){ if(parts[i].length) out.push(\"'\"+parts[i]+\"'\"); if(i!==parts.length-1) out.push('\"\\\\\\''+'\"'); }\n" +
                        "  return 'concat('+out.join(',')+')';\n" +
                        "}\n" +

                        // ---------- STABLE XPATH ----------
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

                        // ---------- COLLECT CANDIDATES ----------
                        "const sel = arguments[0] || 'input,textarea,button,a[href],[role=\"button\"],[role=\"textbox\"],[contenteditable=\"true\"],[aria-label],[data-testid],[data-test],[data-qa]';\n" +
                        "const cap = arguments[1] || 200;\n" +
                        "const els = Array.from(document.querySelectorAll(sel)).filter(isVisible).slice(0, cap);\n" +
                        "return els.map((e,i)=>({\n" +
                        "  xpath:          stableXPath(e),\n" +

                        // enriched text blob (synonym-resistant, safe)
                        "  text: (\n" +
                        "    safeStr(e.innerText || e.value || '') + ' ' +\n" +
                        "    labelText(e) + ' ' +\n" +
                        "    attr(e,'placeholder') + ' ' +\n" +
                        "    attr(e,'aria-label') + ' ' +\n" +
                        "    attr(e,'name') + ' ' +\n" +
                        "    safeStr(e.id) + ' ' +\n" +
                        "    (attr(e,'data-testid')||attr(e,'data-test')||attr(e,'data-qa'))\n" +
                        "  ).trim(),\n" +

                        // existing fields
                        "  tag:            (e.tagName||'').toLowerCase(),\n" +
                        "  idx:            i,\n" +
                        "  id:             safeStr(e.id),\n" +
                        "  name:           attr(e,'name'),\n" +
                        "  className:      safeStr(e.className),\n" +
                        "  placeholder:    attr(e,'placeholder'),\n" +
                        "  ariaLabel:      attr(e,'aria-label'),\n" +
                        "  type:           attr(e,'type'),\n" +
                        "  value:          safeStr(e.value),\n" +
                        "  dataTestId:     (attr(e,'data-testid')||attr(e,'data-test')||attr(e,'data-qa')),\n" +

                        // NEW: richer context fields
                        "  role:           attr(e,'role'),\n" +
                        "  title:          attr(e,'title'),\n" +
                        "  labelText:      labelText(e),\n" +
                        "  parentText:     safeStr(e.parentElement ? (e.parentElement.innerText||'') : '').substring(0,200),\n" +
                        "  headingContext: headingContext(e),\n" +
                        "  ancestorChain:  ancestorChain(e),\n" +
                        "  domDepth:       domDepth(e),\n" +
                        "  siblingBefore:  siblingText(e,'before'),\n" +
                        "  siblingAfter:   siblingText(e,'after'),\n" +
                        "  nearbyText:     nearbyText(e),\n" +
                        "  isVisible:      isVisible(e),\n" +
                        "  isEnabled:      !e.disabled,\n" +
                        "  bboxX:          (e.getBoundingClientRect()||{}).left||0,\n" +
                        "  bboxY:          (e.getBoundingClientRect()||{}).top||0,\n" +
                        "  bboxW:          (e.getBoundingClientRect()||{}).width||0,\n" +
                        "  bboxH:          (e.getBoundingClientRect()||{}).height||0\n" +
                        "}));";



        Object rawObj = ((JavascriptExecutor) driver).executeScript(js, cssSelector, maxCandidates);
        List<Map<String, Object>> raw = (List<Map<String, Object>>) rawObj;

        List<HealDTO.Candidate> out = new ArrayList<>();
        for (Map<String, Object> r : raw) {
            String xpath       = (String) r.get("xpath");
            String text        = (String) r.get("text");
            String tag         = (String) r.get("tag");
            int    idx         = ((Number) r.get("idx")).intValue();
            String aria        = safeStr(r.get("ariaLabel"));

            // existing attribute fields
            String id          = safeStr(r.get("id"));
            String name        = safeStr(r.get("name"));
            String className   = safeStr(r.get("className"));
            String placeholder = safeStr(r.get("placeholder"));
            String type        = safeStr(r.get("type"));
            String dataTestId  = safeStr(r.get("dataTestId"));
            String value       = safeStr(r.get("value"));

            // NEW: richer context fields
            String role          = safeStr(r.get("role"));
            String title         = safeStr(r.get("title"));
            String labelTxt      = safeStr(r.get("labelText"));
            String parentText    = safeStr(r.get("parentText"));
            String headingCtx    = safeStr(r.get("headingContext"));
            String ancestorCh    = safeStr(r.get("ancestorChain"));
            double domDepthVal   = r.get("domDepth") != null ? ((Number) r.get("domDepth")).doubleValue() : 0.0;
            String sibBefore     = safeStr(r.get("siblingBefore"));
            String sibAfter      = safeStr(r.get("siblingAfter"));
            String nearby        = safeStr(r.get("nearbyText"));
            boolean visibleVal   = r.get("isVisible") != null && Boolean.TRUE.equals(r.get("isVisible"));
            boolean enabledVal   = r.get("isEnabled") == null || Boolean.TRUE.equals(r.get("isEnabled"));
            double bboxX         = r.get("bboxX") != null ? ((Number) r.get("bboxX")).doubleValue() : 0.0;
            double bboxY         = r.get("bboxY") != null ? ((Number) r.get("bboxY")).doubleValue() : 0.0;
            double bboxW         = r.get("bboxW") != null ? ((Number) r.get("bboxW")).doubleValue() : 0.0;
            double bboxH         = r.get("bboxH") != null ? ((Number) r.get("bboxH")).doubleValue() : 0.0;

            if (xpath != null && !xpath.isBlank()) {
                out.add(new HealDTO.Candidate(
                        xpath, text, tag, idx, aria,
                        id, name, className, placeholder, type, value, dataTestId,
                        role, title, labelTxt, parentText, headingCtx, ancestorCh,
                        domDepthVal, sibBefore, sibAfter, nearby,
                        visibleVal, enabledVal,
                        bboxX, bboxY, bboxW, bboxH
                ));
            }
        }
        return out;
    }

    private static String safeStr(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
