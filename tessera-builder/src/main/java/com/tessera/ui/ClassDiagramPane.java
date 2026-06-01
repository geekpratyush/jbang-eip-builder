package com.tessera.ui;

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

    private String currentTheme = "VSCode Dark";
    private String scriptTag = "";

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

        if (!mermaidJs.isEmpty()) {
            scriptTag = "<script>" + mermaidJs + "</script>";
        } else {
            scriptTag = "<script src=\"https://cdn.jsdelivr.net/npm/mermaid@10.9.0/dist/mermaid.min.js\"></script>";
        }

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                isInitialized = true;
                if (!lastMermaidCode.isEmpty()) {
                    renderCode(lastMermaidCode);
                }
            }
        });
        engine.loadContent(generateBaseHtml());
    }

    public void setTheme(String theme) {
        this.currentTheme = theme;
        isInitialized = false;
        engine.loadContent(generateBaseHtml());
    }

    private String generateBaseHtml() {
        String bgColor = "#1e1e1e";
        String fgColor = "white";
        String mermaidTheme = "dark";
        String background = "#1e1e1e";
        String primaryColor = "#1e1e1e";
        String primaryTextColor = "#d4d4d4";
        String primaryBorderColor = "#3f3f46";
        String lineColor = "#52525b";
        String secondaryColor = "#27272a";
        String tertiaryColor = "#27272a";

        if ("IntelliJ Light".equalsIgnoreCase(currentTheme)) {
            bgColor = "#ffffff";
            fgColor = "#333333";
            mermaidTheme = "default";
            background = "#ffffff";
            primaryColor = "#e2e8f0";
            primaryTextColor = "#1e293b";
            primaryBorderColor = "#cbd5e1";
            lineColor = "#94a3b8";
            secondaryColor = "#f1f5f9";
            tertiaryColor = "#f1f5f9";
        } else if ("Dracula".equalsIgnoreCase(currentTheme)) {
            bgColor = "#282a36";
            fgColor = "#f8f8f2";
            mermaidTheme = "dark";
            background = "#282a36";
            primaryColor = "#44475a";
            primaryTextColor = "#f8f8f2";
            primaryBorderColor = "#6272a4";
            lineColor = "#6272a4";
            secondaryColor = "#282a36";
            tertiaryColor = "#282a36";
        } else if ("Monokai".equalsIgnoreCase(currentTheme)) {
            bgColor = "#272822";
            fgColor = "#f8f8f2";
            mermaidTheme = "dark";
            background = "#272822";
            primaryColor = "#3e3d32";
            primaryTextColor = "#f8f8f2";
            primaryBorderColor = "#75715e";
            lineColor = "#75715e";
            secondaryColor = "#272822";
            tertiaryColor = "#272822";
        } else if ("Hacker".equalsIgnoreCase(currentTheme)) {
            bgColor = "#050505";
            fgColor = "#00ff00";
            mermaidTheme = "dark";
            background = "#050505";
            primaryColor = "#001d00";
            primaryTextColor = "#00ff00";
            primaryBorderColor = "#00ff00";
            lineColor = "#00ff00";
            secondaryColor = "#000000";
            tertiaryColor = "#000000";
        }

        return "<html>" +
                "<head>" +
                scriptTag +
                "<style>" +
                "  body { background-color: " + bgColor + "; color: " + fgColor + "; margin: 0; padding: 20px; overflow: auto; font-family: 'Segoe UI', sans-serif; }" +
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
                "        theme: '" + mermaidTheme + "'," +
                "        securityLevel: 'loose'," +
                "        class: { useMaxWidth: false }," +
                "        themeVariables: {" +
                "          background: '" + background + "'," +
                "          primaryColor: '" + primaryColor + "'," +
                "          primaryTextColor: '" + primaryTextColor + "'," +
                "          primaryBorderColor: '" + primaryBorderColor + "'," +
                "          lineColor: '" + lineColor + "'," +
                "          secondaryColor: '" + secondaryColor + "'," +
                "          tertiaryColor: '" + tertiaryColor + "'" +
                "        }" +
                "      });" +
                "      const uniqueId = 'graphDiv_' + Math.floor(Math.random() * 1000000);" +
                "      mermaid.render(uniqueId, code).then(({svg}) => {" +
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
        if (engine.getLoadWorker().getState() == javafx.concurrent.Worker.State.RUNNING) {
            return;
        }
        try {
            Object res = engine.executeScript("typeof window.updateDiagram !== 'undefined'");
            if (Boolean.TRUE.equals(res)) {
                // Escape backticks and backslashes for JS string
                String escaped = mermaidCode.replace("\\", "\\\\").replace("`", "\\`").replace("\n", "\\n").replace("'", "\\'");
                engine.executeScript("window.updateDiagram('" + escaped + "')");
            } else {
                engine.loadContent(generateBaseHtml());
            }
        } catch (Exception e) {
            try {
                engine.loadContent(generateBaseHtml());
            } catch (Exception ex) {
                System.err.println("Failed to reload WebView content: " + ex.getMessage());
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

        java.util.List<String> routeSources = new java.util.ArrayList<>();
        java.util.List<String> routeSteps = new java.util.ArrayList<>();
        java.util.List<String> beanInstances = new java.util.ArrayList<>();

        try {
            JsonNode root = yamlMapper.readTree(yamlText);
            if (root != null && root.isArray()) {
                for (JsonNode item : root) {
                    if (item.has("beans") && item.get("beans").isArray()) {
                        for (JsonNode bean : item.get("beans")) {
                            String name = bean.has("name") ? bean.get("name").asText() : "Unnamed";
                            String type = bean.has("type") ? bean.get("type").asText() : "Object";
                            if (type.startsWith("#class:")) {
                                type = type.substring(7);
                            }
                            String simpleType = type.contains(".") ? type.substring(type.lastIndexOf('.') + 1) : type;

                            String safeName = sanitize(name);
                            String safeSimpleType = sanitize(simpleType);
                            instanceTypes.put(safeName, safeSimpleType);
                            beanInstances.add(safeName);
                            
                            java.util.List<String> members = classMembers.computeIfAbsent(safeName, k -> new java.util.ArrayList<>());

                            if (bean.has("properties") && bean.get("properties").isObject()) {
                                bean.get("properties").fields().forEachRemaining(entry -> {
                                    String key = entry.getKey();
                                    String val = entry.getValue().asText();
                                    
                                    if (val.startsWith("#")) {
                                        String cleanVal = val.substring(1);
                                        if (cleanVal.startsWith("bean:")) {
                                            cleanVal = cleanVal.substring(5);
                                        } else if (cleanVal.startsWith("class:")) {
                                            cleanVal = cleanVal.substring(6);
                                        }
                                        if (cleanVal.startsWith("{{") && cleanVal.endsWith("}}")) {
                                            cleanVal = cleanVal.substring(2, cleanVal.length() - 2);
                                        }
                                        String targetBean = sanitize(cleanVal);
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

                    // Extract route steps and relations to beans
                    if (item.has("route") || item.has("from")) {
                        JsonNode fromNode = item.has("from") ? item.get("from") : (item.has("route") ? item.get("route").get("from") : null);
                        if (fromNode != null) {
                            String fromUri = fromNode.has("uri") ? fromNode.get("uri").asText() : "direct:start";
                            String sourceName = "Source_" + sanitize(fromUri);
                            classMembers.computeIfAbsent(sourceName, k -> new java.util.ArrayList<>()).add("<<Camel Route Source>>");
                            instanceTypes.put(sourceName, "Endpoint");
                            routeSources.add(sourceName);

                            String lastStepNode = sourceName;
                            if (fromNode.has("steps") && fromNode.get("steps").isArray()) {
                                int stepIdx = 1;
                                for (JsonNode step : fromNode.get("steps")) {
                                    String stepType = step.fieldNames().hasNext() ? step.fieldNames().next() : "step";
                                    JsonNode stepDetails = step.get(stepType);

                                    String stepLabel = stepType;
                                    java.util.List<String> stepRefs = new java.util.ArrayList<>();

                                    if (stepDetails != null) {
                                        if (stepDetails.isTextual()) {
                                            stepLabel = stepType + ": " + stepDetails.asText();
                                        } else if (stepDetails.has("uri")) {
                                            stepLabel = stepType + ": " + stepDetails.get("uri").asText();
                                            String uriVal = stepDetails.get("uri").asText();
                                            if (uriVal.startsWith("bean:")) {
                                                String r = uriVal.substring(5);
                                                if (r.contains("?")) {
                                                    r = r.substring(0, r.indexOf('?'));
                                                }
                                                stepRefs.add(r);
                                            }
                                        } else if (stepDetails.has("ref")) {
                                            stepRefs.add(stepDetails.get("ref").asText());
                                            stepLabel = stepType + " ref: " + stepDetails.get("ref").asText();
                                        }
                                        
                                        // Also search parameters/properties for bean references
                                        if (stepDetails.has("parameters") && stepDetails.get("parameters").isObject()) {
                                            scanForBeanReferences(stepDetails.get("parameters"), stepRefs);
                                        }
                                        scanForBeanReferences(stepDetails, stepRefs);
                                    } else if (step.isTextual()) {
                                        stepLabel = stepType + ": " + step.asText();
                                    }

                                    String stepNodeName = "Step_" + stepIdx + "_" + sanitize(stepType);
                                    java.util.List<String> stepMembers = classMembers.computeIfAbsent(stepNodeName, k -> new java.util.ArrayList<>());
                                    stepMembers.add("<<Route Step>>");
                                    stepMembers.add(sanitize(stepLabel));
                                    instanceTypes.put(stepNodeName, stepType);
                                    routeSteps.add(stepNodeName);

                                    relations.add(lastStepNode + " --> " + stepNodeName + " : flow");

                                    for (String ref : stepRefs) {
                                        if (ref.startsWith("{{") && ref.endsWith("}}")) {
                                            ref = ref.substring(2, ref.length() - 2);
                                        }
                                        String safeBeanRef = sanitize(ref);
                                        relations.add(stepNodeName + " ..> " + safeBeanRef + " : references");
                                    }

                                    lastStepNode = stepNodeName;
                                    stepIdx++;
                                }
                            }
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
                sb.append(getExternalStyleString(ext));
            }

            for (String rel : relations) {
                sb.append(rel).append("\n");
            }

            // Apply stunning themed styles
            for (String src : routeSources) {
                sb.append(getEndpointStyleString(src));
            }
            for (String step : routeSteps) {
                sb.append(getStepStyleString(step));
            }
            for (String bn : beanInstances) {
                sb.append(getBeanStyleString(bn));
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
                sb.append(getExternalStyleString(ext));
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

    private String getExternalStyleString(String ext) {
        String fill = "#1a3a5a";
        String stroke = "#3a5a7a";
        String color = "#fff";
        
        if ("IntelliJ Light".equalsIgnoreCase(currentTheme)) {
            fill = "#e2e8f0";
            stroke = "#94a3b8";
            color = "#1e293b";
        } else if ("Dracula".equalsIgnoreCase(currentTheme)) {
            fill = "#44475a";
            stroke = "#6272a4";
            color = "#f8f8f2";
        } else if ("Monokai".equalsIgnoreCase(currentTheme)) {
            fill = "#3e3d32";
            stroke = "#75715e";
            color = "#f8f8f2";
        } else if ("Hacker".equalsIgnoreCase(currentTheme)) {
            fill = "#001a00";
            stroke = "#00ff00";
            color = "#00ff00";
        }
        
        return "style " + ext + " fill:" + fill + ",stroke:" + stroke + ",stroke-width:2px,color:" + color + "\n";
    }

    private String getEndpointStyleString(String ext) {
        String fill = "#1b4d3e"; 
        String stroke = "#2e7d32";
        String color = "#fff";
        
        if ("IntelliJ Light".equalsIgnoreCase(currentTheme)) {
            fill = "#d1fae5"; 
            stroke = "#10b981";
            color = "#065f46";
        } else if ("Dracula".equalsIgnoreCase(currentTheme)) {
            fill = "#1b4d3e";
            stroke = "#50fa7b";
            color = "#f8f8f2";
        } else if ("Monokai".equalsIgnoreCase(currentTheme)) {
            fill = "#272822";
            stroke = "#a6e22e";
            color = "#f8f8f2";
        } else if ("Hacker".equalsIgnoreCase(currentTheme)) {
            fill = "#001a00";
            stroke = "#00ff00";
            color = "#00ff00";
        }
        return "style " + ext + " fill:" + fill + ",stroke:" + stroke + ",stroke-width:2px,color:" + color + "\n";
    }

    private String getStepStyleString(String ext) {
        String fill = "#1a365d"; 
        String stroke = "#3182ce";
        String color = "#fff";
        
        if ("IntelliJ Light".equalsIgnoreCase(currentTheme)) {
            fill = "#dbeafe"; 
            stroke = "#3b82f6";
            color = "#1e3a8a";
        } else if ("Dracula".equalsIgnoreCase(currentTheme)) {
            fill = "#2d3748";
            stroke = "#8be9fd";
            color = "#f8f8f2";
        } else if ("Monokai".equalsIgnoreCase(currentTheme)) {
            fill = "#272822";
            stroke = "#66d9ef";
            color = "#f8f8f2";
        } else if ("Hacker".equalsIgnoreCase(currentTheme)) {
            fill = "#000033";
            stroke = "#00ffff";
            color = "#00ffff";
        }
        return "style " + ext + " fill:" + fill + ",stroke:" + stroke + ",stroke-width:2px,color:" + color + "\n";
    }

    private String getBeanStyleString(String ext) {
        String fill = "#4c1d95"; 
        String stroke = "#8b5cf6";
        String color = "#fff";
        
        if ("IntelliJ Light".equalsIgnoreCase(currentTheme)) {
            fill = "#f3e8ff"; 
            stroke = "#a855f7";
            color = "#5b21b6";
        } else if ("Dracula".equalsIgnoreCase(currentTheme)) {
            fill = "#4c1d95";
            stroke = "#bd93f9";
            color = "#f8f8f2";
        } else if ("Monokai".equalsIgnoreCase(currentTheme)) {
            fill = "#272822";
            stroke = "#ae81ff";
            color = "#f8f8f2";
        } else if ("Hacker".equalsIgnoreCase(currentTheme)) {
            fill = "#1a001a";
            stroke = "#ff00ff";
            color = "#ff00ff";
        }
        return "style " + ext + " fill:" + fill + ",stroke:" + stroke + ",stroke-width:2px,color:" + color + "\n";
    }

    private void scanForBeanReferences(JsonNode node, java.util.List<String> refsList) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode valNode = entry.getValue();
                if (valNode.isTextual()) {
                    String val = valNode.asText();
                    if (val.startsWith("#")) {
                        String clean = val.substring(1);
                        if (clean.startsWith("bean:")) {
                            clean = clean.substring(5);
                        }
                        if (clean.startsWith("class:")) {
                            clean = clean.substring(6);
                        }
                        refsList.add(clean);
                    }
                }
            });
        }
    }
}
