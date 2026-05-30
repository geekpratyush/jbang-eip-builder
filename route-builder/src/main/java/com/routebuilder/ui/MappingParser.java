package com.routebuilder.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingParser {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static int idCounter = 0;
    
    // Fallback store to capture source fields implicitly declared in XSLT
    private static final Set<String> implicitSourceFields = new LinkedHashSet<>();

    public static String parseXsltToGraph(String xslt, String sourceData) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode nodes = root.putArray("nodes");
        ArrayNode links = root.putArray("links");
        Set<String> processedNodes = new HashSet<>();
        idCounter = 0;
        implicitSourceFields.clear();

        try {
            // 1. Build JSLT/XSLT Logic Map (Do this FIRST to extract implicit sources)
            if (xslt.trim().startsWith("<")) {
                parseXsltDom(xslt, nodes, links, processedNodes);
            } else if (xslt.trim().startsWith("{")) {
                parseJslt(xslt, nodes, links, processedNodes);
            }

            // 2. Build Source Schema Tree
            if (sourceData != null && !sourceData.isBlank()) {
                JSONObject json = sourceData.trim().startsWith("<") ? XML.toJSONObject(sourceData) : new JSONObject(sourceData);
                buildSourceGraph(json, "root", nodes, links, processedNodes, "");
            } else if (!implicitSourceFields.isEmpty()) {
                // Heuristically construct source nodes from XPath expressions
                addNode(nodes, "src_root", "Inferred Source", "SOURCE", processedNodes);
                for (String field : implicitSourceFields) {
                    String id = "src_" + field.replaceAll("[^a-zA-Z0-9_]", "_");
                    addNode(nodes, id, field, "SOURCE", processedNodes);
                    addLink(links, "src_root", id);
                }
            } else {
                addNode(nodes, "src_root", "Source Payload", "SOURCE", processedNodes);
            }

            // 3. Post-Process Links (Link Implicit Sources to Logic)
            postProcessImplicitLinks(links, processedNodes);

        } catch (Exception e) {
            addNode(nodes, "error", "Metadata Error: " + e.getMessage(), "ERROR", processedNodes);
        }

        return root.toString();
    }

    private static void parseXsltDom(String xslt, ArrayNode nodes, ArrayNode links, Set<String> processed) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xslt.getBytes(StandardCharsets.UTF_8)));

        NodeList templates = doc.getElementsByTagNameNS("*", "template");
        for (int i = 0; i < templates.getLength(); i++) {
            Element template = (Element) templates.item(i);
            String match = template.getAttribute("match");
            String name = template.getAttribute("name");
            
            // Intelligent identification of Source Root (e.g. envelope, message)
            if (!match.isEmpty() && !match.equals("/") && !match.equals("*")) {
                extractImplicitSources(match);
            }

            String label = !match.isEmpty() ? "MATCH: " + cleanXPath(match) : "CALL: " + name;
            String templateId = "template_" + idCounter++;

            addNode(nodes, templateId, label, "TEMPLATE", processed);
            
            // Temporary link string to be resolved in post-processing
            if (!match.isEmpty()) {
                addLink(links, "LINK_MATCH_" + cleanXPath(match), templateId);
            }

            traverseLogicNodes(template, templateId, nodes, links, processed, false);
        }
    }

    private static void traverseLogicNodes(Node parentNode, String parentId, ArrayNode nodes, ArrayNode links, Set<String> processed, boolean inTextBlock) {
        NodeList children = parentNode.getChildNodes();
        StringBuilder textBuffer = new StringBuilder();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) child;
                String ns = el.getNamespaceURI();
                String localName = el.getLocalName();
                
                if ("http://www.w3.org/1999/XSL/Transform".equals(ns)) {
                    processXslElement(el, localName, parentId, nodes, links, processed);
                } else {
                    // Target element creation (Standard XML Output)
                    String targetId = "target_" + idCounter++;
                    addNode(nodes, targetId, "<" + localName + ">", "TARGET", processed);
                    addLink(links, parentId, targetId);
                    traverseLogicNodes(el, targetId, nodes, links, processed, false);
                }
            } else if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getTextContent().trim();
                if (!text.isEmpty()) {
                    // Intelligent SWIFT MT / Plain Text block detection
                    if (text.contains("{1:") || text.contains(":20:") || text.matches(".*:\\d{2}[A-Z]?:.*")) {
                        String targetId = "target_mt_" + idCounter++;
                        addNode(nodes, targetId, text.length() > 15 ? text.substring(0, 15) + "..." : text, "TARGET", processed);
                        addLink(links, parentId, targetId);
                    }
                }
            }
        }
    }

    private static void processXslElement(Element el, String xslType, String parentId, ArrayNode nodes, ArrayNode links, Set<String> processed) {
        String currentId = "logic_" + idCounter++;
        String label = xslType;

        switch (xslType) {
            case "value-of":
                String select = el.getAttribute("select");
                label = "VALUE: " + cleanXPath(select);
                extractImplicitSources(select);
                addNode(nodes, currentId, label, "LOGIC", processed);
                addLink(links, parentId, currentId);
                addLink(links, "LINK_SELECT_" + cleanXPath(select), currentId);
                break;
            case "variable":
            case "param":
                String name = el.getAttribute("name");
                String vSel = el.getAttribute("select");
                label = xslType.toUpperCase() + ": $" + name;
                if (!vSel.isEmpty()) extractImplicitSources(vSel);
                addNode(nodes, currentId, label, "VARIABLE", processed);
                addLink(links, parentId, currentId);
                if (!vSel.isEmpty()) addLink(links, "LINK_SELECT_" + cleanXPath(vSel), currentId);
                traverseLogicNodes(el, currentId, nodes, links, processed, false);
                break;
            case "choose":
            case "if":
            case "for-each":
                String test = el.hasAttribute("test") ? el.getAttribute("test") : el.getAttribute("select");
                label = xslType.toUpperCase() + (test.isEmpty() ? "" : ": " + cleanXPath(test));
                extractImplicitSources(test);
                addNode(nodes, currentId, label, "CONDITION", processed);
                addLink(links, parentId, currentId);
                addLink(links, "LINK_SELECT_" + cleanXPath(test), currentId);
                traverseLogicNodes(el, currentId, nodes, links, processed, false);
                break;
            case "when":
            case "otherwise":
                String cond = el.getAttribute("test");
                label = xslType.toUpperCase() + (cond.isEmpty() ? "" : ": " + cleanXPath(cond));
                extractImplicitSources(cond);
                addNode(nodes, currentId, label, "BRANCH", processed);
                addLink(links, parentId, currentId);
                addLink(links, "LINK_SELECT_" + cleanXPath(cond), currentId);
                traverseLogicNodes(el, currentId, nodes, links, processed, false);
                break;
            case "copy-of":
                String sel = el.getAttribute("select");
                label = "COPY: " + cleanXPath(sel);
                extractImplicitSources(sel);
                addNode(nodes, currentId, label, "LOGIC", processed);
                addLink(links, parentId, currentId);
                addLink(links, "LINK_SELECT_" + cleanXPath(sel), currentId);
                break;
            case "apply-templates":
                String mode = el.getAttribute("mode");
                String atSel = el.getAttribute("select");
                label = "APPLY" + (mode.isEmpty() ? "" : " [" + mode + "]") + ": " + cleanXPath(atSel);
                extractImplicitSources(atSel);
                addNode(nodes, currentId, label, "TEMPLATE", processed);
                addLink(links, parentId, currentId);
                addLink(links, "LINK_SELECT_" + cleanXPath(atSel), currentId);
                traverseLogicNodes(el, currentId, nodes, links, processed, false);
                break;
            case "with-param":
                String pName = el.getAttribute("name");
                String pSel = el.getAttribute("select");
                label = "PARAM: $" + pName;
                extractImplicitSources(pSel);
                addNode(nodes, currentId, label, "VARIABLE", processed);
                addLink(links, parentId, currentId);
                addLink(links, "LINK_SELECT_" + cleanXPath(pSel), currentId);
                break;
            case "element":
                String eName = el.getAttribute("name");
                label = "ELEM: " + eName;
                addNode(nodes, currentId, label, "TARGET", processed);
                addLink(links, parentId, currentId);
                traverseLogicNodes(el, currentId, nodes, links, processed, false);
                break;
            case "text":
                String txt = el.getTextContent().trim();
                label = "TEXT: " + (txt.length() > 10 ? txt.substring(0, 10) + "..." : txt);
                addNode(nodes, currentId, label, "TARGET", processed);
                addLink(links, parentId, currentId);
                break;
            default:
                traverseLogicNodes(el, parentId, nodes, links, processed, false);
                break;
        }
    }

    private static void extractImplicitSources(String xpath) {
        if (xpath == null || xpath.isEmpty() || xpath.equals("/")) return;
        // Strip out functions like substring(), concat(), local-name()
        String cleanPath = xpath.replaceAll("[a-zA-Z\\-]+\\([^)]*\\)", "");
        // Extract block names like block4/field[name='20'] -> block4, field, name
        Matcher m = Pattern.compile("([a-zA-Z0-9_\\-]+)").matcher(cleanPath);
        while (m.find()) {
            String token = m.group(1);
            if (!token.equals("and") && !token.equals("or") && !token.equals("not") && !token.matches("\\d+")) {
                implicitSourceFields.add(token);
            }
        }
    }

    private static String cleanXPath(String xpath) {
        if (xpath == null) return "";
        String clean = xpath.replace("pacs:", "").replace("urn:", "");
        if (clean.length() > 25) return clean.substring(0, 22) + "...";
        return clean;
    }

    private static void postProcessImplicitLinks(ArrayNode links, Set<String> processedNodes) {
        for (int i = 0; i < links.size(); i++) {
            ObjectNode link = (ObjectNode) links.get(i);
            String source = link.get("source").asText();
            if (source.startsWith("LINK_MATCH_") || source.startsWith("LINK_SELECT_")) {
                String expression = source.substring(11);
                boolean linked = false;
                for (String pId : processedNodes) {
                    if (pId.startsWith("src_")) {
                        String fieldName = pId.substring(4);
                        if (expression.toLowerCase().contains(fieldName.toLowerCase())) {
                            link.put("source", pId);
                            linked = true;
                            break;
                        }
                    }
                }
                if (!linked) {
                    // fallback to root if can't resolve heuristic
                    link.put("source", "src_root");
                }
            }
        }
    }

    private static void parseJslt(String jslt, ArrayNode nodes, ArrayNode links, Set<String> processed) {
        addNode(nodes, "jslt_root", "JSLT Object", "TEMPLATE", processed);
        
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(.+)");
        Matcher m = p.matcher(jslt);
        
        while (m.find()) {
            String targetKey = m.group(1);
            String expr = m.group(2).trim();
            if (expr.endsWith(",")) expr = expr.substring(0, expr.length() - 1);

            String lId = "logic_" + idCounter++;
            String tId = "target_" + idCounter++;

            addNode(nodes, lId, "EVAL: " + expr, "LOGIC", processed);
            addNode(nodes, tId, "\"" + targetKey + "\"", "TARGET", processed);

            addLink(links, "jslt_root", lId);
            addLink(links, lId, tId);

            String srcField = expr.replace(".", "").replaceAll("[^a-zA-Z0-9_]", "");
            if (processed.contains("src_" + srcField)) {
                addLink(links, "src_" + srcField, lId);
            }
        }
    }

    private static void buildSourceGraph(JSONObject json, String parentId, ArrayNode nodes, ArrayNode links, Set<String> processed, String currentPath) {
        for (String key : json.keySet()) {
            String id = "src_" + key.replaceAll("[^a-zA-Z0-9_]", "_");
            addNode(nodes, id, key, "SOURCE", processed);
            if (!parentId.equals("root")) addLink(links, parentId, id);
            
            Object val = json.get(key);
            if (val instanceof JSONObject) {
                buildSourceGraph((JSONObject) val, id, nodes, links, processed, currentPath + "/" + key);
            } else if (val instanceof org.json.JSONArray) {
                org.json.JSONArray arr = (org.json.JSONArray) val;
                if (arr.length() > 0 && arr.get(0) instanceof JSONObject) {
                    buildSourceGraph(arr.getJSONObject(0), id, nodes, links, processed, currentPath + "/" + key);
                }
            }
        }
    }

    private static void addNode(ArrayNode nodes, String id, String name, String type, Set<String> processed) {
        if (processed.contains(id)) return;
        ObjectNode node = nodes.addObject();
        node.put("id", id);
        node.put("name", name.length() > 25 ? name.substring(0, 22) + "..." : name);
        node.put("type", type);
        processed.add(id);
    }

    private static void addLink(ArrayNode links, String source, String target) {
        ObjectNode link = links.addObject();
        link.put("source", source);
        link.put("target", target);
    }
}
