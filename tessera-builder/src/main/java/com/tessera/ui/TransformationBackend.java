package com.tessera.ui;

import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.smooks.Smooks;
import org.smooks.io.sink.StringSink;
import org.json.JSONObject;
import groovy.lang.GroovyShell;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import net.sf.flatpack.ParserFactory;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransformationBackend {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> joorInstanceCache = new java.util.concurrent.ConcurrentHashMap<>();

    public static String transform(String input, String logic, String type, File currentFolder) throws Exception {
        String targetType = "json"; // Default
        if (currentFolder != null) {
            File configFile = new File(currentFolder, "transformation.json");
            if (configFile.exists()) {
                try {
                    String content = Files.readString(configFile.toPath());
                    JSONObject currentConfig = new JSONObject(content);
                    JSONObject targetCfg = currentConfig.optJSONObject("target");
                    if (targetCfg != null) {
                        targetType = targetCfg.optString("type", "json");
                    }
                } catch (Exception ignored) { }
            }
        }

        if ("xslt".equalsIgnoreCase(type) || "xslt3".equalsIgnoreCase(type)) {
            return transformXslt(input, logic);
        } else if ("jslt".equalsIgnoreCase(type)) {
            return transformJslt(input, logic, targetType);
        } else if ("smooks".equalsIgnoreCase(type)) {
            return transformSmooks(input, logic, currentFolder);
        } else if ("joor".equalsIgnoreCase(type)) {
            return transformJoor(input, logic);
        } else if ("groovy".equalsIgnoreCase(type)) {
            return transformGroovy(input, logic);
        } else if ("flatpack".equalsIgnoreCase(type)) {
            return transformFlatpack(input, logic);
        } else if ("freemarker".equalsIgnoreCase(type) || "ftl".equalsIgnoreCase(type)) {
            return transformFreemarker(input, logic);
        }
        return "Unsupported transformation type: " + type;
    }

    public static String unmarshal(String rawContent, JSONObject sourceCfg) {
        if (sourceCfg == null) return rawContent;
        String type = sourceCfg.optString("type", "xml");
        if ("mt".equalsIgnoreCase(type) || "mt103".equalsIgnoreCase(type) || "swift".equalsIgnoreCase(type)) {
            try {
                AbstractMT mt = AbstractMT.parse(rawContent);
                if (mt != null) return mt.xml().trim();
            } catch (Exception e) {
                System.err.println("Prowide parsing failed: " + e.getMessage());
            }
        }
        return rawContent;
    }

    public static String combineEnrichmentXml(String originalXml, String truncatedXml) {
        String cleanOriginal = originalXml.replaceAll("<\\?xml.*?\\?>", "").trim();
        String cleanTruncated = truncatedXml.replaceAll("<\\?xml.*?\\?>", "").trim();
        return "<envelope>\n  <original>\n" + cleanOriginal + "\n  </original>\n" +
                "  <truncated>\n" + cleanTruncated + "\n  </truncated>\n</envelope>";
    }

    public static String transformEnrichment(String originalXml, String truncatedXml, String logic) throws Exception {
        return transformEnrichment(originalXml, truncatedXml, logic, "xslt", null);
    }

    public static String transformEnrichment(String originalXml, String truncatedXml, String logic, String logicType,
            File currentFolder) throws Exception {
        String combinedXml = combineEnrichmentXml(originalXml, truncatedXml);
        return transform(combinedXml, logic, logicType, currentFolder);
    }

    private static String toFileUriString(File file) {
        String uri = file.toURI().toString();
        if (uri.startsWith("file:/") && !uri.startsWith("file:///")) {
            uri = "file:///" + uri.substring(6);
        }
        return uri;
    }

    private static String transformSmooks(String input, String configXml, File currentFolder) throws Exception {
        if (currentFolder != null) {
            currentFolder = currentFolder.getAbsoluteFile().getCanonicalFile();
        }
        if (configXml != null) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(schemaUri|schemaURI)\\s*=\\s*['\"]([^'\"]+)['\"]");
            java.util.regex.Matcher matcher = pattern.matcher(configXml);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String attrName = matcher.group(1);
                String rawUri = matcher.group(2);
                if (!rawUri.contains(":/") && !rawUri.startsWith("file:")) {
                    File schemaFile = new File(currentFolder, rawUri);
                    String absoluteUri = toFileUriString(schemaFile);
                    matcher.appendReplacement(sb, attrName + "=\"" + absoluteUri + "\"");
                } else {
                    matcher.appendReplacement(sb, matcher.group(0));
                }
            }
            matcher.appendTail(sb);
            configXml = sb.toString();
        }

        Smooks smooks = null;
        try {
            smooks = new Smooks();
            byte[] configBytes = configXml.getBytes(StandardCharsets.UTF_8);
            try (InputStream stream = new ByteArrayInputStream(configBytes)) {
                if (currentFolder != null) {
                    String baseUri = toFileUriString(currentFolder);
                    if (!baseUri.endsWith("/")) {
                        baseUri += "/";
                    }
                    smooks.addResourceConfigs(baseUri, stream);
                } else {
                    smooks.addResourceConfigs(stream);
                }
            }
            
            StringSink sink = new StringSink();
            smooks.filterSource(new org.smooks.io.source.StringSource(input), sink);
            String result = sink.toString();
            if (result == null || result.trim().isEmpty() || result.trim().equals("[]")) {
                 return "Smooks filtered successfully but returned no data. Check your <exports> or reader configuration.";
            }
            try {
                return prettyPrintXml(result);
            } catch (Exception e) {
                return result; 
            }
        } finally {
            if (smooks != null) smooks.close();
        }
    }

    private static String transformJslt(String input, String jslt, String targetType) throws Exception {
        String jsonInput = input;
        if (input != null && input.trim().startsWith("<")) {
            try {
                // If it looks like XML, convert to JSON for JSLT
                jsonInput = org.json.XML.toJSONObject(input).toString();
            } catch (Exception e) {
                // Not valid XML, proceed as JSON
            }
        }
        JsonNode inputNode = mapper.readTree(jsonInput);

        // Add custom functions to guarantee availability and XSLT compatibility
        List<com.schibsted.spt.data.jslt.Function> customFunctions = new ArrayList<>();
        
        // substring(string, start, [end])
        customFunctions.add(new com.schibsted.spt.data.jslt.Function() {
            @Override public String getName() { return "substring"; }
            @Override public int getMinArguments() { return 2; }
            @Override public int getMaxArguments() { return 3; }
            @Override public JsonNode call(JsonNode inputNode, JsonNode[] arguments) {
                String str = arguments[0].asText();
                if (str == null || str.equals("null")) return com.fasterxml.jackson.databind.node.TextNode.valueOf("");
                int start = arguments[1].asInt();
                int end = (arguments.length > 2) ? arguments[2].asInt() : str.length();
                if (start < 0) start = 0;
                if (start > str.length()) start = str.length();
                if (end < start) end = start;
                if (end > str.length()) end = str.length();
                return com.fasterxml.jackson.databind.node.TextNode.valueOf(str.substring(start, end));
            }
        });

        // format-number(number, pattern)
        customFunctions.add(new com.schibsted.spt.data.jslt.Function() {
            @Override public String getName() { return "format-number"; }
            @Override public int getMinArguments() { return 2; }
            @Override public int getMaxArguments() { return 2; }
            @Override public JsonNode call(JsonNode inputNode, JsonNode[] arguments) {
                double val = arguments[0].asDouble();
                String pattern = arguments[1].asText();
                try {
                    java.text.DecimalFormat df = new java.text.DecimalFormat(pattern);
                    return com.fasterxml.jackson.databind.node.TextNode.valueOf(df.format(val));
                } catch (Exception e) {
                    return com.fasterxml.jackson.databind.node.TextNode.valueOf(String.valueOf(val));
                }
            }
        });

        // replace(string, search, replacement)
        customFunctions.add(new com.schibsted.spt.data.jslt.Function() {
            @Override public String getName() { return "replace"; }
            @Override public int getMinArguments() { return 3; }
            @Override public int getMaxArguments() { return 3; }
            @Override public JsonNode call(JsonNode inputNode, JsonNode[] arguments) {
                String str = arguments[0].asText();
                String search = arguments[1].asText();
                String replacement = arguments[2].asText();
                if (str == null) return com.fasterxml.jackson.databind.node.NullNode.getInstance();
                return com.fasterxml.jackson.databind.node.TextNode.valueOf(str.replace(search, replacement));
            }
        });

        // array-zip(arr1, arr2)
        customFunctions.add(new com.schibsted.spt.data.jslt.Function() {
            @Override public String getName() { return "array-zip"; }
            @Override public int getMinArguments() { return 2; }
            @Override public int getMaxArguments() { return 2; }
            @Override public JsonNode call(JsonNode inputNode, JsonNode[] arguments) {
                if (!(arguments[0] instanceof com.fasterxml.jackson.databind.node.ArrayNode)) {
                    return com.fasterxml.jackson.databind.node.NullNode.getInstance();
                }
                com.fasterxml.jackson.databind.node.ArrayNode arr1 = (com.fasterxml.jackson.databind.node.ArrayNode) arguments[0];
                com.fasterxml.jackson.databind.node.ArrayNode arr2 = arguments[1] instanceof com.fasterxml.jackson.databind.node.ArrayNode ? (com.fasterxml.jackson.databind.node.ArrayNode) arguments[1] : null;
                com.fasterxml.jackson.databind.node.ArrayNode result = mapper.createArrayNode();
                for (int i = 0; i < arr1.size(); i++) {
                    com.fasterxml.jackson.databind.node.ObjectNode obj = mapper.createObjectNode();
                    obj.set("orig", arr1.get(i));
                    obj.set("trunc", (arr2 != null && i < arr2.size()) ? arr2.get(i) : com.fasterxml.jackson.databind.node.NullNode.getInstance());
                    result.add(obj);
                }
                return result;
            }
        });

        Expression expr = Parser.compileString(jslt, customFunctions);
        JsonNode outputNode = expr.apply(inputNode);
        
        // If the result is a simple text node (often the case for MT output), return it as-is
        if (outputNode.isTextual()) {
            return outputNode.asText();
        }

        String jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(outputNode);

        if ("xml".equalsIgnoreCase(targetType)) {
            String xmlOut = org.json.XML.toString(new JSONObject(jsonResult), "Document");
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                   xmlOut.replace("<Document>", "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14\">");
        }
        return jsonResult;
    }

    private static String transformJoor(String input, String javaCode) throws Exception {
        Object mapperInstance = joorInstanceCache.computeIfAbsent(javaCode, code -> {
            try {
                if (code.contains("class ") || code.contains("interface ")) {
                    String pkg = "com.tessera.dynamic";
                    java.util.regex.Matcher pkgMatcher = java.util.regex.Pattern.compile("package\\s+([a-zA-Z0-9_\\.]+)\\s*;").matcher(code);
                    if (pkgMatcher.find()) {
                        pkg = pkgMatcher.group(1);
                    }
                    String className = "Mapper";
                    java.util.regex.Matcher classMatcher = java.util.regex.Pattern.compile("(?:class|interface|enum)\\s+(\\w+)").matcher(code);
                    if (classMatcher.find()) {
                        className = classMatcher.group(1);
                    }
                    String fullClassName = pkg + "." + className;
                    org.joor.Reflect compiled = org.joor.Reflect.compile(fullClassName, code);
                    try {
                        return compiled.create().get();
                    } catch (Exception e) {
                        return compiled.get();
                    }
                } else {
                    return org.joor.Reflect.compile("com.tessera.dynamic.PreviewMapper", 
                        "package com.tessera.dynamic; public class PreviewMapper { " +
                        "public String map(Object body) { " + code + " } }")
                        .create().get();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return (String) org.joor.Reflect.on(mapperInstance).call("map", input).get();
    }

    private static String transformGroovy(String input, String groovyCode) throws Exception {
        try {
            GroovyShell shell = new GroovyShell();
            shell.setProperty("body", input);
            Object result = shell.evaluate(groovyCode);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            return "Groovy Error: " + e.getMessage();
        }
    }

    private static String transformFlatpack(String input, String definitionXml) throws Exception {
        try {
            ParserFactory factory = DefaultParserFactory.getInstance();
            net.sf.flatpack.Parser parser = factory.newFixedLengthParser(new StringReader(definitionXml), new StringReader(input));
            DataSet ds = parser.parse();
            
            if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
                StringBuilder errs = new StringBuilder("Flatpack Parsing Errors:\n");
                for (Object err : ds.getErrors()) errs.append(err.toString()).append("\n");
                return errs.toString();
            }

            StringBuilder xml = new StringBuilder("<dataset>\n");
            while (ds.next()) {
                xml.append("  <record>\n");
                String[] columns = ds.getColumns();
                for (String col : columns) {
                    xml.append("    <").append(col).append(">")
                       .append(ds.getString(col).trim())
                       .append("</").append(col).append(">\n");
                }
                xml.append("  </record>\n");
            }
            xml.append("</dataset>");
            return xml.toString();
        } catch (Exception e) {
            return "Flatpack Error: " + e.getMessage();
        }
    }

    private static String transformXslt(String xml, String xslt) throws Exception {
        TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
        Transformer transformer = factory.newTransformer(new StreamSource(new StringReader(xslt)));
        Source xmlSource;
        if (xml.trim().startsWith("[HEADER_START]")) {
            transformer.setParameter("payload", xml);
            xmlSource = new StreamSource(new StringReader("<dummy/>"));
        } else {
            xmlSource = new StreamSource(new StringReader(xml));
        }
        StringWriter writer = new StringWriter();
        transformer.transform(xmlSource, new StreamResult(writer));
        return writer.toString();
    }

    public static String validateXml(String xml, File xsdFile) {
        try {
            javax.xml.validation.SchemaFactory factory = javax.xml.validation.SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
            javax.xml.validation.Schema schema = factory.newSchema(xsdFile);
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private static String prettyPrintXml(String xml) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        Source xmlSource = new StreamSource(new StringReader(xml));
        StringWriter writer = new StringWriter();
        transformer.transform(xmlSource, new StreamResult(writer));
        return writer.toString();
    }

    private static String transformFreemarker(String input, String templateContent) throws Exception {
        java.util.Map<String, Object> dataModel = new java.util.HashMap<>();
        dataModel.put("body", input);
        
        try {
            if (input.trim().startsWith("{") || input.trim().startsWith("[")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Object jsonParsed = mapper.readValue(input, Object.class);
                dataModel.put("body", jsonParsed);
            }
        } catch (Exception ignored) {}
        
        java.util.Map<String, Object> headers = new java.util.HashMap<>();
        headers.put("name", "Camel Developer");
        headers.put("date", java.time.LocalDate.now().toString());
        dataModel.put("headers", headers);
        
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        
        freemarker.cache.StringTemplateLoader stringLoader = new freemarker.cache.StringTemplateLoader();
        stringLoader.putTemplate("myTemplate", templateContent);
        cfg.setTemplateLoader(stringLoader);
        
        freemarker.template.Template temp = cfg.getTemplate("myTemplate");
        try (StringWriter out = new StringWriter()) {
            temp.process(dataModel, out);
            return out.toString();
        }
    }
}
