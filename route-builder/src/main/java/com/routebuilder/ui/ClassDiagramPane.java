package com.routebuilder.ui;

import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.layout.Priority;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ClassDiagramPane extends VBox {
    private WebView webView;
    private WebEngine engine;
    private ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private boolean isInitialized = false;
    private String lastMermaidCode = "";

    public ClassDiagramPane() {
        webView = new WebView();
        engine = webView.getEngine();
        VBox.setVgrow(webView, Priority.ALWAYS);
        getChildren().add(webView);
        
        String mermaidJs = "";
        try (java.io.InputStream is = ClassDiagramPane.class.getResourceAsStream("/styles/mermaid.min.js")) {
            if (is != null) {
                mermaidJs = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } else {
                System.err.println("Could not find /styles/mermaid.min.js in resources!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String scriptTag;
        if (!mermaidJs.isEmpty()) {
            scriptTag = "<script>" + mermaidJs + "</script>";
        } else {
            scriptTag = "<script src=\"https://cdn.jsdelivr.net/npm/mermaid@10.9.0/dist/mermaid.min.js\"></script>";
        }

        // Load the base HTML once
        String baseHtml = "<html>" +
                "<head>" +
                scriptTag +
                "<style>" +
                "  body { background-color: #1e1e1e; color: white; margin: 0; padding: 20px; overflow: auto; font-family: 'Segoe UI', sans-serif; }" +
                "  .mermaid { display: flex; justify-content: center; transition: opacity 0.3s; }" +
                "  .status { position: fixed; top: 10px; right: 10px; font-size: 10px; color: #444; }" +
                "  #error-box { color: #f44336; padding: 10px; border: 1px solid #f44336; border-radius: 4px; display: none; font-size: 12px; margin-top: 20px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div id=\"status\" class=\"status\">Ready</div>" +
                "<div id=\"error-box\"></div>" +
                "<div id=\"diagram-container\" class=\"mermaid\"></div>" +
                "<script>" +
                "  window.updateDiagram = function(code) {" +
                "    const container = document.getElementById('diagram-container');" +
                "    const errorBox = document.getElementById('error-box');" +
                "    const status = document.getElementById('status');" +
                "    errorBox.style.display = 'none';" +
                "    status.innerHTML = 'Rendering...';" +
                "    if (typeof mermaid === 'undefined') {" +
                "       setTimeout(() => window.updateDiagram(code), 100);" +
                "       return;" +
                "    }" +
                "    try {" +
                "      mermaid.initialize({" +
                "        startOnLoad: false," +
                "        theme: 'dark'," +
                "        securityLevel: 'loose'," +
                "        class: { useMaxWidth: false }," +
                "        themeVariables: {" +
                "          background: '#1e1e1e'," +
                "          primaryColor: '#1e1e1e'," +
                "          primaryTextColor: '#d4d4d4'," +
                "          primaryBorderColor: '#3f3f46'," +
                "          lineColor: '#52525b'," +
                "          secondaryColor: '#27272a'," +
                "          tertiaryColor: '#27272a'" +
                "        }" +
                "      });" +
                "      mermaid.render('graphDiv', code).then(({svg}) => {" +
                "        container.innerHTML = svg;" +
                "        status.innerHTML = 'Done';" +
                "      }).catch(err => {" +
                "        errorBox.innerHTML = 'Mermaid Error: ' + err.message;" +
                "        errorBox.style.display = 'block';" +
                "        status.innerHTML = 'Error';" +
                "      });" +
                "    } catch(e) {" +
                "      errorBox.innerHTML = 'Critical Error: ' + e.message;" +
                "      errorBox.style.display = 'block';" +
                "    }" +
                "  };" +
                "</script>" +
                "</body>" +
                "</html>";
        
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                isInitialized = true;
                if (!lastMermaidCode.isEmpty()) {
                    renderCode(lastMermaidCode);
                }
            }
        });
        engine.loadContent(baseHtml);
    }

    public void renderClassDiagram(String javaCode) {
        String mermaidCode = parseJavaToMermaid(javaCode);
        renderCode(mermaidCode);
    }

    public void renderYamlBeansDiagram(String yamlText, java.io.File currentFile) {
        String mermaidCode = parseYamlToMermaid(yamlText, currentFile);
        renderCode(mermaidCode);
    }

    private void renderCode(String mermaidCode) {
        lastMermaidCode = mermaidCode;
        if (isInitialized) {
            try {
                // Escape backticks and backslashes for JS string
                String escaped = mermaidCode.replace("\\", "\\\\").replace("`", "\\`").replace("\n", "\\n").replace("'", "\\'");
                engine.executeScript("window.updateDiagram('" + escaped + "')");
            } catch (Exception e) {
                // Fallback if script fails
                System.err.println("JS Execution failed: " + e.getMessage());
            }
        }
    }

    private String sanitize(String text) {
        if (text == null) return "";
        // Replace Java generics < > with Mermaid-safe ~ ~
        String result = text.replace("<", "~").replace(">", "~");
        // Aggressively remove non-alphanumeric except safe ones
        return result.replaceAll("[^a-zA-Z0-9_ ~]", "_");
    }

    private boolean isKeyword(String s) {
        String[] keywords = {"return", "package", "import", "throw", "new", "class", "interface", "enum", "extends", "implements", "static", "final", "transient", "volatile", "public", "private", "protected", "void"};
        for (String kw : keywords) {
            if (kw.equals(s)) return true;
        }
        return false;
    }

    private void resolveJavaClassLocally(String simpleClassName, java.io.File currentFile,
                                         java.util.Map<String, java.util.List<String>> classMembers,
                                         java.util.List<String> relations,
                                         java.util.Set<String> externalClasses,
                                         java.util.Set<String> parsedJavaClasses) {
        if (parsedJavaClasses.contains(simpleClassName)) {
            return;
        }
        parsedJavaClasses.add(simpleClassName);

        java.io.File dir = currentFile != null ? currentFile.getParentFile() : null;
        if (dir == null || !dir.exists()) {
            externalClasses.add(simpleClassName);
            return;
        }

        java.io.File javaFile = new java.io.File(dir, simpleClassName + ".java");
        if (!javaFile.exists()) {
            externalClasses.add(simpleClassName);
            return;
        }

        try {
            String javaCode = java.nio.file.Files.readString(javaFile.toPath());
            
            // Extract class definition
            Pattern classPattern = Pattern.compile("(class|interface|enum)\\s+(\\w+)");
            Matcher classMatcher = classPattern.matcher(javaCode);
            String className = simpleClassName;
            if (classMatcher.find()) {
                className = classMatcher.group(2);
            }
            
            java.util.List<String> members = classMembers.computeIfAbsent(className, k -> new java.util.ArrayList<>());
            members.add("<<Java Class>>");

            // Parse Fields
            Pattern fieldPattern = Pattern.compile("(?m)^\\s*(?:private|protected|public|static|final|\\s)*\\s*([a-zA-Z0-9_<>\\[\\]~\\.]+)\\s+(\\w+)\\s*(?:=|;)");
            Matcher fieldMatcher = fieldPattern.matcher(javaCode);
            while (fieldMatcher.find()) {
                String fullMatch = fieldMatcher.group(0);
                String type = fieldMatcher.group(1);
                String name = fieldMatcher.group(2);
                
                // Skip Java keywords/control statements
                if (isKeyword(type) || isKeyword(name)) continue;

                String visSymbol = "+";
                if (fullMatch.contains("private")) visSymbol = "-";
                else if (fullMatch.contains("protected")) visSymbol = "#";

                members.add(visSymbol + " " + sanitize(type) + " " + sanitize(name));

                String rawType = type.replaceAll("<.*>", "");
                if (Character.isUpperCase(rawType.charAt(0)) && !rawType.equals("String") && !rawType.equals("Integer")) {
                    String cleanRawType = sanitize(rawType);
                    relations.add(className + " --> " + cleanRawType + " : refers");
                    // Recursively parse the referenced class if it exists locally
                    resolveJavaClassLocally(cleanRawType, currentFile, classMembers, relations, externalClasses, parsedJavaClasses);
                }
            }

            // Parse Methods
            Pattern methodPattern = Pattern.compile("(?:public|protected|private|static|final|\\s)*\\s*([a-zA-Z0-9_<>\\[\\]~\\.]+)\\s+(\\w+)\\((.*?)\\)\\s*(?:\\{|;)");
            Matcher methodMatcher = methodPattern.matcher(javaCode);
            while (methodMatcher.find()) {
                String returnType = methodMatcher.group(1);
                String methodName = methodMatcher.group(2);
                String args = methodMatcher.group(3);

                if (isKeyword(methodName)) continue;

                members.add(sanitize(methodName) + "(" + sanitize(args) + ") " + sanitize(returnType));
            }

            // Inheritance
            if (javaCode.contains("extends")) {
                Pattern exP = Pattern.compile("extends\\s+(\\w+)");
                Matcher exM = exP.matcher(javaCode);
                if (exM.find()) {
                    String parent = sanitize(exM.group(1));
                    relations.add(parent + " <|-- " + className);
                    resolveJavaClassLocally(parent, currentFile, classMembers, relations, externalClasses, parsedJavaClasses);
                }
            }
            if (javaCode.contains("implements")) {
                Pattern imP = Pattern.compile("implements\\s+([\\w\\s,]+)");
                Matcher imM = imP.matcher(javaCode);
                if (imM.find()) {
                    for (String imp : imM.group(1).split(",")) {
                        String clean = sanitize(imp.trim());
                        if (!clean.isEmpty()) {
                            relations.add(clean + " <|.. " + className);
                            resolveJavaClassLocally(clean, currentFile, classMembers, relations, externalClasses, parsedJavaClasses);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String parseYamlToMermaid(String yamlText, java.io.File currentFile) {
        StringBuilder sb = new StringBuilder("classDiagram\n");
        java.util.Map<String, java.util.List<String>> classMembers = new java.util.LinkedHashMap<>();
        java.util.List<String> relations = new java.util.ArrayList<>();
        java.util.Set<String> externalClasses = new java.util.HashSet<>();
        java.util.Map<String, String> instanceTypes = new java.util.HashMap<>();
        java.util.Set<String> parsedJavaClasses = new java.util.HashSet<>();

        try {
            JsonNode root = yamlMapper.readTree(yamlText);
            if (root != null && root.isArray()) {
                for (JsonNode item : root) {
                    if (item.has("beans") && item.get("beans").isArray()) {
                        for (JsonNode bean : item.get("beans")) {
                            String name = bean.has("name") ? bean.get("name").asText() : "Unnamed";
                            String type = bean.has("type") ? bean.get("type").asText() : "Object";
                            String simpleType = type.contains(".") ? type.substring(type.lastIndexOf('.') + 1) : type;

                            String safeName = sanitize(name);
                            String safeSimpleType = sanitize(simpleType);
                            instanceTypes.put(safeName, safeSimpleType);
                            
                            java.util.List<String> members = classMembers.computeIfAbsent(safeName, k -> new java.util.ArrayList<>());

                            if (bean.has("properties") && bean.get("properties").isObject()) {
                                bean.get("properties").fields().forEachRemaining(entry -> {
                                    String key = entry.getKey();
                                    String val = entry.getValue().asText();
                                    
                                    if (val.startsWith("#")) {
                                        String targetBean = sanitize(val.substring(1));
                                        relations.add(safeName + " --> " + targetBean + " : depends");
                                        members.add(sanitize(key) + " : " + targetBean);
                                    } else {
                                        members.add(sanitize(key) + " : " + sanitize(val));
                                    }
                                });
                            }

                            // We connect the YAML bean instance to the Java class representation
                            relations.add(safeName + " ..> " + safeSimpleType + " : instantiates");

                            // Resolve local Java class files
                            resolveJavaClassLocally(safeSimpleType, currentFile, classMembers, relations, externalClasses, parsedJavaClasses);
                        }
                    }
                }
            }

            for (String cls : classMembers.keySet()) {
                sb.append("class ").append(cls).append(" {\n");
                String typeTag = instanceTypes.get(cls);
                if (typeTag != null) {
                    sb.append("  <<").append(typeTag).append(">>\n");
                }
                if (externalClasses.contains(cls)) {
                    sb.append("  <<External>>\n");
                }
                for (String m : classMembers.get(cls)) {
                    sb.append("  ").append(m).append("\n");
                }
                sb.append("}\n");
            }

            for (String ext : externalClasses) {
                if (!classMembers.containsKey(ext)) {
                    sb.append("class ").append(ext).append(" {\n  <<External>>\n}\n");
                }
                sb.append("style ").append(ext).append(" fill:#1a3a5a,stroke:#3a5a7a,stroke-width:2px,color:#fff\n");
            }

            for (String rel : relations) {
                sb.append(rel).append("\n");
            }

        } catch (Exception e) {
            return "classDiagram\nclass Error {\n  msg: " + sanitize(e.getMessage()) + "\n}";
        }
        return sb.toString();
    }

    private String parseJavaToMermaid(String javaCode) {
        StringBuilder sb = new StringBuilder("classDiagram\n");
        java.util.Map<String, java.util.List<String>> classMembers = new java.util.LinkedHashMap<>();
        java.util.List<String> relations = new java.util.ArrayList<>();
        java.util.Set<String> externalClasses = new java.util.HashSet<>();

        Pattern classPattern = Pattern.compile("(class|interface|enum)\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(javaCode);
        
        String className = "Unknown";
        if (classMatcher.find()) {
            className = classMatcher.group(2);
            classMembers.computeIfAbsent(className, k -> new java.util.ArrayList<>());
        }

        // Hardened Field Regex
        Pattern fieldPattern = Pattern.compile("(?:private|protected|public|static|final|\\s)*\\s*([\\w<>\\[\\]~\\.]+)\\s+(\\w+)\\s*(?:=|;)");
        Matcher fieldMatcher = fieldPattern.matcher(javaCode);
        while (fieldMatcher.find()) {
            String fullMatch = fieldMatcher.group(0);
            String type = fieldMatcher.group(1);
            String name = fieldMatcher.group(2);
            
            if (classMembers.containsKey(className)) {
                String visSymbol = "+";
                if (fullMatch.contains("private")) visSymbol = "-";
                else if (fullMatch.contains("protected")) visSymbol = "#";
                
                classMembers.get(className).add(visSymbol + " " + sanitize(type) + " " + sanitize(name));
                
                String rawType = type.replaceAll("<.*>", "");
                if (Character.isUpperCase(rawType.charAt(0)) && !rawType.equals("String") && !rawType.equals("Integer")) {
                    if (javaCode.contains("import") && javaCode.contains(rawType)) {
                        externalClasses.add(sanitize(rawType));
                        relations.add(className + " --> " + sanitize(rawType) + " : dependency");
                    } else if (!rawType.equals(className)) {
                        relations.add(className + " --> " + sanitize(rawType) + " : refers");
                    }
                }
            }
        }

        // Hardened Method Regex
        Pattern methodPattern = Pattern.compile("(?:public|protected|private|static|final|\\s)*\\s*([\\w<>\\[\\]~\\.]+)\\s+(\\w+)\\((.*?)\\)\\s*(?:\\{|;)");
        Matcher methodMatcher = methodPattern.matcher(javaCode);
        while (methodMatcher.find()) {
            String returnType = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            String args = methodMatcher.group(3);
            
            if (!methodName.equals("if") && !methodName.equals("for") && !methodName.equals("while") && !methodName.equals("switch") && !methodName.equals("return")) {
                if (classMembers.containsKey(className)) {
                    classMembers.get(className).add(sanitize(methodName) + "(" + sanitize(args) + ") " + sanitize(returnType));
                }
            }
        }

        for (String cls : classMembers.keySet()) {
            sb.append("class ").append(cls).append(" {\n");
            for (String m : classMembers.get(cls)) {
                sb.append("  ").append(m).append("\n");
            }
            sb.append("}\n");
        }

        for (String ext : externalClasses) {
            if (!classMembers.containsKey(ext)) {
                sb.append("class ").append(ext).append(" {\n  <<External>>\n}\n");
                sb.append("style ").append(ext).append(" fill:#1a3a5a,stroke:#3a5a7a,stroke-width:2px,color:#fff\n");
            }
        }

        for (String rel : relations) {
            sb.append(rel).append("\n");
        }

        if (javaCode.contains("extends")) {
            Pattern exP = Pattern.compile("extends\\s+(\\w+)");
            Matcher exM = exP.matcher(javaCode);
            if (exM.find()) sb.append(exM.group(1)).append(" <|-- ").append(className).append("\n");
        }
        if (javaCode.contains("implements")) {
            Pattern imP = Pattern.compile("implements\\s+([\\w\\s,]+)");
            Matcher imM = imP.matcher(javaCode);
            if (imM.find()) {
                for (String imp : imM.group(1).split(",")) {
                    String clean = sanitize(imp.trim());
                    if (!clean.isEmpty()) sb.append(clean).append(" <|.. ").append(className).append("\n");
                }
            }
        }

        return sb.toString();
    }
}
