package com.fyp.qa.healing;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CandidateExtractor {

    // WebDriver used to execute JavaScript inside the browser
    private final WebDriver driver;

    // Selector for all interactive elements (things users can click/type/select)
    public static final String SELECTOR_INTERACTIVE =
            "input,textarea,select,button,a[href]," +
                    "[role=\"button\"],[role=\"link\"],[role=\"menuitem\"],[role=\"tab\"]," +
                    "[role=\"checkbox\"],[role=\"radio\"],[role=\"textbox\"],[role=\"combobox\"]," +
                    "[role=\"switch\"],[role=\"option\"]," +
                    "[aria-label]," +
                    "[onclick],[contenteditable=\"true\"]";

    // Selector for elements that mainly contain text (used for assertions)
    public static final String SELECTOR_TEXT =
            "input,textarea,select,button,a," +
                    "h1,h2,h3,h4,h5,h6,p,span,label,div,li,td,th,caption," +
                    "[aria-label],[data-testid],[data-test],[data-qa]";

    // Constructor to initialize WebDriver
    public CandidateExtractor(WebDriver driver) {
        this.driver = driver;
    }

    public List<HealDTO.Candidate> extract(int maxCandidates) {
        return extract(maxCandidates, "");
    }

    @SuppressWarnings("unchecked")
    public List<HealDTO.Candidate> extract(int maxCandidates, String cssSelector) {
        // JavaScript that runs inside the browser to extract elements
        String js =
                "function safeStr(v){ return (v==null? '': (''+v)).trim(); }\n" +

                        // Checks if element is visible on screen
                        "function isVisible(e){\n" +
                        "  if(!e) return false;\n" +
                        "  const st = window.getComputedStyle(e);\n" +
                        "  if(!st) return false;\n" +
                        "  if(st.display==='none'||st.visibility==='hidden'||parseFloat(st.opacity||'1')===0) return false;\n" +
                        "  const r = e.getBoundingClientRect();\n" +
                        "  return !!r && r.width>=2 && r.height>=2;\n" +
                        "}\n" +

                        "function attr(e,n){ return safeStr(e.getAttribute(n)); }\n" +

                        // Finds label text for inputs
                        "function labelText(e){\n" +
                        "  try {\n" +
                        "    if(!e) return '';\n" +
                        "    if(e.closest){\n" +
                        "      const lab = e.closest('label');\n" +
                        "      if(lab){\n" +
                        // strip sr-only spans from label text
                        "        const clone = lab.cloneNode(true);\n" +
                        "        clone.querySelectorAll('.sr-only,[aria-hidden=\"true\"]').forEach(x=>x.remove());\n" +
                        "        const t = safeStr(clone.innerText||clone.textContent||'');\n" +
                        "        if(t) return t;\n" +
                        "      }\n" +
                        "    }\n" +
                        "    const id = safeStr(e.id);\n" +
                        "    if(!id) return '';\n" +
                        "    const lab2 = document.querySelector('label[for=\"'+id.replace(/\"/g,'\\\\\"')+'\"]');\n" +
                        "    if(lab2){\n" +
                        "      const clone2 = lab2.cloneNode(true);\n" +
                        "      clone2.querySelectorAll('.sr-only,[aria-hidden=\"true\"]').forEach(x=>x.remove());\n" +
                        "      return safeStr(clone2.innerText||clone2.textContent||'');\n" +
                        "    }\n" +
                        "    return '';\n" +
                        "  } catch(ex){ return ''; }\n" +
                        "}\n" +

                        // Extracts clean visible text
                        "function cleanInnerText(e){\n" +
                        "  try {\n" +
                        "    if(!e) return '';\n" +
                        "    const clone = e.cloneNode(true);\n" +
                        "    clone.querySelectorAll('.sr-only,[aria-hidden=\"true\"],script,style').forEach(x=>x.remove());\n" +
                        "    return safeStr(clone.innerText||clone.textContent||e.value||'');\n" +
                        "  } catch(ex){ return safeStr(e.innerText||e.value||''); }\n" +
                        "}\n" +

                        // Handles icon-only elements (extracts image info)
                        "function childImgFilename(e){\n" +
                        "  try {\n" +
                        "    if(!e.querySelector) return '';\n" +
                        "    const img = e.querySelector('img');\n" +
                        "    if(!img) return '';\n" +
                        "    const src = attr(img,'src') || attr(img,'data-src') || '';\n" +
                        "    const alt = attr(img,'alt');\n" +
                        // prefer alt text, fall back to filename without extension
                        "    if(alt) return alt;\n" +
                        "    const filename = src.split('/').pop().split('?')[0];\n" +
                        "    return filename.replace(/\\.[a-z]{2,4}$/i,'');\n" +
                        "  } catch(ex){ return ''; }\n" +
                        "}\n" +

                        // Finds the nearest heading (h1–h6) related to the element
                        "function headingContext(e){\n" +
                        "  try {\n" +
                        "    var cur = e.parentElement; var depth = 0;\n" +
                        "    while(cur && depth < 8){\n" +
                        "      var t = (cur.tagName||'').toLowerCase();\n" +
                        "      if(/^h[1-6]$/.test(t)) return safeStr(cur.innerText);\n" +
                        "      var kids = cur.querySelectorAll?Array.from(cur.querySelectorAll('h1,h2,h3,h4,h5,h6')):[];\n" +
                        "      if(kids.length>0) return safeStr(kids[0].innerText);\n" +
                        "      cur = cur.parentElement; depth++;\n" +
                        "    }\n" +
                        "    return '';\n" +
                        "  } catch(ex){ return ''; }\n" +
                        "}\n" +

                        // Builds a short path of parent elements (like a simplified CSS path)
                        "function ancestorChain(e){\n" +
                        "  try {\n" +
                        "    var parts=[]; var cur=e.parentElement; var d=0;\n" +
                        "    while(cur && d<5){\n" +
                        "      var t=(cur.tagName||'').toLowerCase();\n" +
                        "      var cls=safeStr(cur.className).split(' ')[0];\n" +
                        "      parts.push(cls?t+'.'+cls:t);\n" +
                        "      cur=cur.parentElement; d++;\n" +
                        "    }\n" +
                        "    return parts.join(' > ');\n" +
                        "  } catch(ex){ return ''; }\n" +
                        "}\n" +

                        // Calculates how deep element is in DOM
                        "function domDepth(e){\n" +
                        "  try{\n" +
                        "    var d=0; var cur=e.parentElement;\n" +
                        "    while(cur&&cur.nodeType===1){d++;cur=cur.parentElement;}\n" +
                        "    return d;\n" +
                        "  }catch(ex){return 0;}\n" +
                        "}\n" +

                        // Gets text from nearby sibling elements (before or after the element)
                        "function siblingText(e,direction){\n" +
                        "  try{\n" +
                        "    var texts=[]; var sib=direction==='before'?e.previousElementSibling:e.nextElementSibling;\n" +
                        "    var limit=2;\n" +
                        "    while(sib&&limit-->0){\n" +
                        "      var t=safeStr(sib.innerText||sib.textContent||'');\n" +
                        "      if(t) texts.push(t);\n" +
                        "      sib=direction==='before'?sib.previousElementSibling:sib.nextElementSibling;\n" +
                        "    }\n" +
                        "    return texts.join(' | ');\n" +
                        "  }catch(ex){return '';}\n" +
                        "}\n" +

                        // Gets text from the parent container of the element
                        "function nearbyText(e){\n" +
                        "  try{\n" +
                        "    var p=e.parentElement;\n" +
                        "    if(!p) return '';\n" +
                        "    var t=safeStr(p.innerText||p.textContent||'');\n" +
                        "    if(t.length>120) t=t.substring(0,120);\n" +
                        "    return t;\n" +
                        "  }catch(ex){return '';}\n" +
                        "}\n" +

                        // Safely converts a string into a valid XPath literal
                        "function xpathLiteral(s){\n" +
                        "  s=safeStr(s);\n" +
                        "  if(s.indexOf(\"'\")===-1) return \"'\"+s+\"'\";\n" +
                        "  if(s.indexOf('\"')===-1) return '\"'+s+'\"';\n" +
                        "  const parts=s.split(\"'\"); const out=[];\n" +
                        "  for(let i=0;i<parts.length;i++){if(parts[i].length)out.push(\"'\"+parts[i]+\"'\");if(i!==parts.length-1)out.push('\"\\\\\\''+'\"');}\n" +
                        "  return 'concat('+out.join(',')+')';\n" +
                        "}\n" +

                        // ── stableXPath ───────────────────────────────────────────────────
                        // Priority: id > dataTestId > name > placeholder > aria-label > href > positional
                        "function stableXPath(e){\n" +
                        "  const tag=(e.tagName||'').toLowerCase();\n" +
                        "  const id=safeStr(e.id);\n" +
                        "  if(id) return '//*[@id='+xpathLiteral(id)+']';\n" +
                        "  const dt=attr(e,'data-testid')||attr(e,'data-test')||attr(e,'data-qa');\n" +
                        "  if(dt) return '//*[(@data-testid='+xpathLiteral(dt)+' or @data-test='+xpathLiteral(dt)+' or @data-qa='+xpathLiteral(dt)+')]';\n" +
                        "  const nm=attr(e,'name');\n" +
                        "  if(nm) return '//'+tag+'[@name='+xpathLiteral(nm)+']';\n" +
                        "  const ph=attr(e,'placeholder');\n" +
                        "  if(ph) return '//'+tag+'[@placeholder='+xpathLiteral(ph)+']';\n" +
                        "  const al=attr(e,'aria-label');\n" +
                        "  if(al) return '//'+tag+'[@aria-label='+xpathLiteral(al)+']';\n" +
                        // href fallback for links — gives cleaner xpath than positional
                        "  const href=attr(e,'href');\n" +
                        "  if(href && href!=='#' && !href.startsWith('javascript') && href.length<150)\n" +
                        "    return '//'+tag+'[@href='+xpathLiteral(href)+']';\n" +
                        // positional fallback — last resort
                        "  const parts=[]; let el=e;\n" +
                        "  while(el&&el.nodeType===1){\n" +
                        "    let ix=1; let sib=el.previousSibling;\n" +
                        "    while(sib){if(sib.nodeType===1&&sib.tagName===el.tagName)ix++;sib=sib.previousSibling;}\n" +
                        "    parts.unshift(el.tagName.toLowerCase()+'['+ix+']');\n" +
                        "    el=el.parentNode;\n" +
                        "  }\n" +
                        "  return '//'+parts.join('/');\n" +
                        "}\n" +

                        // ── COLLECT CANDIDATES ────────────────────────────────────────────
                        // arguments[0] = cssSelector (empty string = use default)
                        // arguments[1] = maxCandidates cap
                        "const sel = arguments[0] || '" + SELECTOR_INTERACTIVE + "';\n" +
                        "const cap = arguments[1] || 200;\n" +
                        // collect interactive elements
                        "const interactiveEls = Array.from(document.querySelectorAll(sel)).filter(isVisible);\n" +

                        // collect text-bearing divs/spans/p — only ones with DIRECT text node children
                        // this captures product names, labels, titles etc. without capturing container divs
                        "const textEls = Array.from(document.querySelectorAll('div,span,p,h1,h2,h3,h4,h5,h6,li,td,th,label'))\n" +
                        "  .filter(e => {\n" +
                        "    if (!isVisible(e)) return false;\n" +
                        // must have direct text node
                        "    const directText = Array.from(e.childNodes)\n" +
                        "      .filter(n => n.nodeType === 3)\n" +
                        "      .map(n => n.textContent.trim())\n" +
                        "      .join(' ').trim();\n" +
                        "    if (directText.length < 2) return false;\n" +
                        // skip containers — too many children means it is a wrapper not a label
                        "    if (e.children.length > 0) return false;\n" +
                        // skip long text — content blocks not labels
                        "    if (directText.length > 80) return false;\n" +
                        "    if (interactiveEls.includes(e)) return false;\n" +
                        "    return true;\n" +
                        "  });\n" +

                        // merge — interactive first, then text elements, deduplicated, capped
                        "const seen = new Set();\n" +
                        "const els = [...interactiveEls, ...textEls]\n" +
                        "  .filter(e => { if (seen.has(e)) return false; seen.add(e); return true; })\n" +
                        "  .slice(0, cap);\n" +

                        "return els.map((e,i)=>({\n" +
                        "  xpath:       stableXPath(e),\n" +

                        // Build a rich feature set for each element — clean innerText + label + placeholder + aria +
                        //                  name + id + dataTestId + child-img filename
                        "  text: [\n" +
                        "    cleanInnerText(e),\n" +
                        "    labelText(e),\n" +
                        "    attr(e,'placeholder'),\n" +
                        "    attr(e,'aria-label'),\n" +
                        "    attr(e,'name'),\n" +
                        "    safeStr(e.id),\n" +
                        "    (attr(e,'data-testid')||attr(e,'data-test')||attr(e,'data-qa')),\n" +
                        "    childImgFilename(e)\n" +  // ← NEW: handles icon-only links
                        "  ].filter(Boolean).join(' ').trim(),\n" +

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
                        "  role:           attr(e,'role'),\n" +
                        "  title:          attr(e,'title'),\n" +
                        "  labelText:      labelText(e),\n" +
                        "  parentText:     safeStr(e.parentElement?(e.parentElement.innerText||''):'').substring(0,200),\n" +
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
        // This returns a raw list of elements with their extracted data
        Object rawObj = ((JavascriptExecutor) driver).executeScript(js, cssSelector, maxCandidates);
        List<Map<String, Object>> raw = (List<Map<String, Object>>) rawObj;

        List<HealDTO.Candidate> out = new ArrayList<>();
        // Loop through each extracted element
        for (Map<String, Object> r : raw) {
            // Basic element details
            String xpath       = (String) r.get("xpath");
            String text        = (String) r.get("text");
            String tag         = (String) r.get("tag");
            int    idx         = ((Number) r.get("idx")).intValue();
            String aria        = safeStr(r.get("ariaLabel"));

            // Standard attributes
            String id          = safeStr(r.get("id"));
            String name        = safeStr(r.get("name"));
            String className   = safeStr(r.get("className"));
            String placeholder = safeStr(r.get("placeholder"));
            String type        = safeStr(r.get("type"));
            String dataTestId  = safeStr(r.get("dataTestId"));
            String value       = safeStr(r.get("value"));

            // Additional semantic/context attributes
            String role        = safeStr(r.get("role"));
            String title       = safeStr(r.get("title"));
            String labelTxt    = safeStr(r.get("labelText"));
            String parentText  = safeStr(r.get("parentText"));
            String headingCtx  = safeStr(r.get("headingContext"));
            String ancestorCh  = safeStr(r.get("ancestorChain"));
            double domDepthVal = r.get("domDepth") != null ? ((Number) r.get("domDepth")).doubleValue() : 0.0;
            String sibBefore   = safeStr(r.get("siblingBefore"));
            String sibAfter    = safeStr(r.get("siblingAfter"));
            String nearby      = safeStr(r.get("nearbyText"));
            boolean visibleVal = r.get("isVisible") != null && Boolean.TRUE.equals(r.get("isVisible"));
            boolean enabledVal = r.get("isEnabled") == null || Boolean.TRUE.equals(r.get("isEnabled"));
            double bboxX       = r.get("bboxX") != null ? ((Number) r.get("bboxX")).doubleValue() : 0.0;
            double bboxY       = r.get("bboxY") != null ? ((Number) r.get("bboxY")).doubleValue() : 0.0;
            double bboxW       = r.get("bboxW") != null ? ((Number) r.get("bboxW")).doubleValue() : 0.0;
            double bboxH       = r.get("bboxH") != null ? ((Number) r.get("bboxH")).doubleValue() : 0.0;

            if (xpath != null && !xpath.isBlank()) {
                // Create a Candidate object with all extracted features
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
