package com.routebuilder.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TransformationEditorWindow {
    public static final java.util.List<TransformationEditorWindow> activeInstances = new java.util.ArrayList<>();

    private final File folder;
    private JSONObject config;
    private final Stage stage;
    
    private WebView sourceRawWebView;
    private WebView sourceXmlWebView;
    private WebView logicWebView;
    private WebView targetWebView;
    private TextArea consoleArea;
    
    private WebEngine sourceRawEngine;
    private WebEngine sourceXmlEngine;
    private WebEngine logicEngine;
    private WebEngine targetEngine;

    private boolean sourceRawInitialized = false;
    private boolean sourceXmlInitialized = false;
    private boolean logicInitialized = false;
    private boolean targetInitialized = false;

    private String transformationType = "xslt";
    private boolean isNonXmlSource = false;
    private boolean isEnrichment = false;

    public TransformationEditorWindow(File folder) {
        activeInstances.add(this);
        this.folder = folder;
        this.stage = new Stage();
        loadConfig();
    }

    public void setTheme(String themeName) {
        String monacoTheme = themeName.equalsIgnoreCase("IntelliJ Light") ? "vs" : "vs-dark";
        Platform.runLater(() -> {
            if (sourceRawInitialized) sourceRawEngine.executeScript("if(window.setTheme) window.setTheme('" + monacoTheme + "');");
            if (sourceXmlInitialized) sourceXmlEngine.executeScript("if(window.setTheme) window.setTheme('" + monacoTheme + "');");
            if (logicInitialized) logicEngine.executeScript("if(window.setTheme) window.setTheme('" + monacoTheme + "');");
            if (targetInitialized) targetEngine.executeScript("if(window.setTheme) window.setTheme('" + monacoTheme + "');");
        });
    }

    private void loadConfig() {
        try {
            File configFile = new File(folder, "transformation.json");
            if (configFile.exists()) {
                String content = Files.readString(configFile.toPath());
                this.config = new JSONObject(content);
                this.transformationType = config.optString("type", "xslt");
                this.isEnrichment = "enrichment".equals(transformationType);
                
                JSONObject source = config.optJSONObject("source");
                if (source != null) {
                    String sourceType = source.optString("type", "xml");
                    this.isNonXmlSource = !"xml".equalsIgnoreCase(sourceType);
                }
            } else {
                this.config = new JSONObject();
                this.config.put("type", "xslt");
                this.config.put("name", "New Transformation");
                
                JSONObject source = new JSONObject();
                source.put("type", "xml");
                source.put("file", "source.xml");
                this.config.put("source", source);

                JSONObject target = new JSONObject();
                target.put("type", "xml");
                target.put("xsd", "schema.xsd");
                this.config.put("target", target);
                
                JSONObject logic = new JSONObject();
                logic.put("type", "xslt");
                logic.put("file", "transform.xslt");
                this.config.put("logic", logic);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.config = new JSONObject();
        }
    }

    public void show() {
        stage.setTitle("Data Transformation Studio - " + config.optString("name", folder.getName()));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.getStyleClass().add(RouteBuilderApp.currentThemeClass);

        // Toolbar
        ToolBar toolBar = new ToolBar();
        Button btnRun = new Button("Run", new FontIcon("fas-play"));
        btnRun.getStyleClass().addAll("editor-btn", "btn-play-file");
        btnRun.setOnAction(e -> runTransformation());

        Button btnValidate = new Button("Validate", new FontIcon("fas-check-double"));
        btnValidate.getStyleClass().addAll("editor-btn", "btn-validate");
        btnValidate.setOnAction(e -> runValidation());
        
        JSONObject targetCfg = config.optJSONObject("target");
        if (targetCfg == null || !"xml".equalsIgnoreCase(targetCfg.optString("type"))) {
            btnValidate.setDisable(true);
        }

        Button btnSave = new Button("Save", new FontIcon("fas-save"));
        btnSave.getStyleClass().add("editor-btn");
        btnSave.setOnAction(e -> saveLogic());

        Label lblName = new Label(config.optString("name", "Transformation"));
        lblName.getStyleClass().add("transformation-name-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(btnRun, btnValidate, btnSave, new Separator(), lblName, spacer);
        root.setTop(toolBar);

        RouteBuilderApp.themedRoots.add(root);
        stage.setOnHidden(e -> {
            activeInstances.remove(this);
            RouteBuilderApp.themedRoots.remove(root);
        });

        // Console at bottom
        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefHeight(120);
        consoleArea.getStyleClass().add("studio-console-area");
        log("Studio Initialized for " + config.optString("name"));

        // Main Horizontal SplitPane
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.setOrientation(Orientation.HORIZONTAL);

        // --- Panel 1: Source ---
        VBox sourcePanel = new VBox();
        Label lblSource = new Label(isEnrichment ? "SOURCES (ORIGINAL & TRUNCATED)" : "SOURCE");
        lblSource.getStyleClass().add("pane-title");
        sourcePanel.getChildren().add(lblSource);

        if (isEnrichment) {
            SplitPane enrichSplit = new SplitPane();
            enrichSplit.setOrientation(Orientation.VERTICAL);
            
            VBox originalBox = new VBox(new Label("Original Source"), sourceRawWebView = new WebView());
            RouteBuilderApp.installClipboardShortcuts(sourceRawWebView);
            VBox.setVgrow(sourceRawWebView, Priority.ALWAYS);
            
            VBox truncatedBox = new VBox(new Label("Truncated Source (to be enriched)"), sourceXmlWebView = new WebView());
            RouteBuilderApp.installClipboardShortcuts(sourceXmlWebView);
            VBox.setVgrow(sourceXmlWebView, Priority.ALWAYS);
            
            enrichSplit.getItems().addAll(originalBox, truncatedBox);
            sourcePanel.getChildren().add(enrichSplit);
            VBox.setVgrow(enrichSplit, Priority.ALWAYS);
            
            sourceRawEngine = sourceRawWebView.getEngine();
            sourceXmlEngine = sourceXmlWebView.getEngine();
        } else if (isNonXmlSource) {
            SplitPane sourceSplit = new SplitPane();
            sourceSplit.setOrientation(Orientation.VERTICAL);
            
            VBox rawBox = new VBox(new Label("Raw Source"), sourceRawWebView = new WebView());
            RouteBuilderApp.installClipboardShortcuts(sourceRawWebView);
            VBox.setVgrow(sourceRawWebView, Priority.ALWAYS);
            
            VBox xmlBox = new VBox(new Label("Converted XML"), sourceXmlWebView = new WebView());
            RouteBuilderApp.installClipboardShortcuts(sourceXmlWebView);
            VBox.setVgrow(sourceXmlWebView, Priority.ALWAYS);
            
            sourceSplit.getItems().addAll(rawBox, xmlBox);
            sourcePanel.getChildren().add(sourceSplit);
            VBox.setVgrow(sourceSplit, Priority.ALWAYS);
            
            sourceRawEngine = sourceRawWebView.getEngine();
            sourceXmlEngine = sourceXmlWebView.getEngine();
        } else {
            sourceXmlWebView = new WebView();
            RouteBuilderApp.installClipboardShortcuts(sourceXmlWebView);
            sourcePanel.getChildren().add(sourceXmlWebView);
            VBox.setVgrow(sourceXmlWebView, Priority.ALWAYS);
            sourceXmlEngine = sourceXmlWebView.getEngine();
        }

        // --- Panel 2: Logic ---
        VBox logicPanel = new VBox();
        Label lblLogic = new Label("LOGIC (" + transformationType.toUpperCase() + ")");
        lblLogic.getStyleClass().add("pane-title");
        logicWebView = new WebView();
        RouteBuilderApp.installClipboardShortcuts(logicWebView);
        logicPanel.getChildren().addAll(lblLogic, logicWebView);
        VBox.setVgrow(logicWebView, Priority.ALWAYS);
        logicEngine = logicWebView.getEngine();

        // --- Panel 3: Target ---
        VBox targetPanel = new VBox();
        Label lblTarget = new Label("TARGET");
        lblTarget.getStyleClass().add("pane-title");
        targetWebView = new WebView();
        RouteBuilderApp.installClipboardShortcuts(targetWebView);
        targetPanel.getChildren().addAll(lblTarget, targetWebView);
        VBox.setVgrow(targetWebView, Priority.ALWAYS);
        targetEngine = targetWebView.getEngine();

        horizontalSplit.getItems().addAll(sourcePanel, logicPanel, targetPanel);
        horizontalSplit.setDividerPositions(0.33, 0.66);

        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.getItems().addAll(horizontalSplit, consoleArea);
        mainSplit.setDividerPositions(0.8);
        
        root.setCenter(mainSplit);

        Scene scene = new Scene(root, 1200, 800);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            if (RouteBuilderApp.currentDynamicCssUri != null) {
                scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
            }
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.show();

        initEditors();
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            consoleArea.appendText("[" + time + "] " + msg + "\n");
        });
    }

    private void initEditors() {
        if (isNonXmlSource || isEnrichment) {
            setupMonaco(sourceRawEngine, "text", (obs, oldVal, newVal) -> { sourceRawInitialized = true; loadSourceData(); });
        }
        setupMonaco(sourceXmlEngine, "xml", (obs, oldVal, newVal) -> { sourceXmlInitialized = true; if (!isNonXmlSource && !isEnrichment) loadSourceData(); });
        
        String logicLang = "xml";
        if ("jslt".equals(transformationType)) logicLang = "json";
        else if ("groovy".equals(transformationType)) logicLang = "java";
        
        setupMonaco(logicEngine, logicLang, (obs, oldVal, newVal) -> { logicInitialized = true; loadLogicData(); });

        String targetLang = "xml";
        JSONObject targetCfg = config.optJSONObject("target");
        if (targetCfg != null) {
            String type = targetCfg.optString("type", "xml");
            if ("mt".equalsIgnoreCase(type) || "swift".equalsIgnoreCase(type)) {
                targetLang = "text"; // Will trigger swift-mt highlighting
            } else if ("json".equalsIgnoreCase(type)) {
                targetLang = "json";
            }
        }
        setupMonaco(targetEngine, targetLang, (obs, oldVal, newVal) -> { targetInitialized = true; });
    }

    private void setupMonaco(WebEngine engine, String language, javafx.beans.value.ChangeListener<javafx.concurrent.Worker.State> onSucceeded) {
        try {
            String html = getMonacoHtml(language);
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    onSucceeded.changed(null, oldState, newState);
                }
            });
            engine.loadContent(html);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMonacoHtml(String language) {
        String monacoBase = getClass().getResource("/monaco/vs/loader.js").toExternalForm();
        monacoBase = monacoBase.substring(0, monacoBase.lastIndexOf("/vs/loader.js"));
        
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <base href=\"" + monacoBase + "/\"/>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <style>\n" +
            "        body { margin: 0; padding: 0; overflow: hidden; background-color: #1e1e1e; }\n" +
            "        #editor { width: 100vw; height: 100vh; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"editor\"></div>\n" +
            "    <script src=\"" + monacoBase + "/vs/loader.js\"></script>\n" +
            "    <script>\n" +
            "        window.editorValue = window.editorValue || '';\n" +
            "        window.setValue = function(val) {\n" +
            "            window.editorValue = val;\n" +
            "            if(window.editor) {\n" +
            "                window.editor.setValue(val);\n" +
            "            }\n" +
            "        };\n" +
            "        window.getValue = function() {\n" +
            "            return window.editor ? window.editor.getValue() : window.editorValue;\n" +
            "        };\n" +
            "        require.config({ paths: { vs: '" + monacoBase + "/vs' }});\n" +
            "        require(['vs/editor/editor.main'], function() {\n" +
            "            // Define SWIFT MT language\n" +
            "            monaco.languages.register({ id: 'swift-mt' });\n" +
            "            monaco.languages.setMonarchTokensProvider('swift-mt', {\n" +
            "                tokenizer: {\n" +
            "                    root: [\n" +
            "                        [/{[1-5]:/, 'metatag'],\n" +
            "                        [/}/, 'metatag'],\n" +
            "                        [/^:[0-9A-Z]{2,3}:/, 'keyword'],\n" +
            "                        [/-}/, 'metatag'],\n" +
            "                        [/\\n:[0-9A-Z]{2,3}:/, 'keyword'],\n" +
            "                    ]\n" +
            "                }\n" +
            "            });\n" +
            "            monaco.editor.defineTheme('swift-dark', {\n" +
            "                base: 'vs-dark',\n" +
            "                inherit: true,\n" +
            "                rules: [\n" +
            "                    { token: 'keyword', foreground: '569cd6', fontStyle: 'bold' },\n" +
            "                    { token: 'metatag', foreground: 'ce9178' }\n" +
            "                ],\n" +
            "                colors: { 'editor.background': '#1e1e1e' }\n" +
            "            });\n" +
            "            window.editor = monaco.editor.create(document.getElementById('editor'), {\n" +
            "                value: window.editorValue,\n" +
            "                language: '" + ("text".equals(language) ? "swift-mt" : language) + "',\n" +
            "                theme: 'swift-dark',\n" +
            "                automaticLayout: true,\n" +
            "                minimap: { enabled: false },\n" +
            "                scrollBeyondLastLine: false,\n" +
            "                fontSize: 14\n" +
            "            });\n" +
            "        });\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    private void setEditorText(WebEngine engine, String text) {
        if (text == null) text = "";
        final String finalT = text;
        Platform.runLater(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(finalT, "UTF-8").replace("+", "%20");
                String script = 
                    "if (typeof window.setValue === 'function') {" +
                    "  window.setValue(decodeURIComponent('" + encoded + "'));" +
                    "} else {" +
                    "  window.editorValue = decodeURIComponent('" + encoded + "');" +
                    "}";
                engine.executeScript(script);
            } catch (Exception e) { }
        });
    }

    private String getEditorText(WebEngine engine) {
        try {
            return (String) engine.executeScript("window.getValue();");
        } catch (Exception e) {
            return "";
        }
    }

    private void loadSourceData() {
        try {
            if (isEnrichment) {
                org.json.JSONArray sources = config.optJSONArray("sources");
                if (sources != null && sources.length() >= 2) {
                    JSONObject s1 = sources.getJSONObject(0);
                    JSONObject s2 = sources.getJSONObject(1);
                    
                    File f1 = new File(folder, s1.optString("file"));
                    File f2 = new File(folder, s2.optString("file"));
                    
                    if (f1.exists()) setEditorText(sourceRawEngine, Files.readString(f1.toPath()));
                    if (f2.exists()) setEditorText(sourceXmlEngine, Files.readString(f2.toPath()));
                }
                return;
            }

            JSONObject sourceConfig = config.optJSONObject("source");
            if (sourceConfig == null) return;
            
            String fileName = sourceConfig.optString("file");
            if (fileName == null || fileName.isEmpty()) return;
            
            File sourceFile = new File(folder, fileName);
            if (sourceFile.exists()) {
                String content = Files.readString(sourceFile.toPath());
                if (isNonXmlSource) {
                    setEditorText(sourceRawEngine, content);
                    unmarshalToXml(content);
                } else {
                    setEditorText(sourceXmlEngine, content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadLogicData() {
        try {
            JSONObject logicConfig = config.optJSONObject("logic");
            if (logicConfig == null) return;
            
            String fileName = logicConfig.optString("file");
            if (fileName == null || fileName.isEmpty()) return;
            
            File logicFile = new File(folder, fileName);
            if (logicFile.exists()) {
                String content = Files.readString(logicFile.toPath());
                setEditorText(logicEngine, content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unmarshalToXml(String rawContent) {
        CompletableFuture.runAsync(() -> {
            try {
                String xml = TransformationBackend.unmarshal(rawContent, config.optJSONObject("source"));
                setEditorText(sourceXmlEngine, xml);
            } catch (Exception e) {
                setEditorText(sourceXmlEngine, "Error unmarshalling: " + e.getMessage());
            }
        });
    }

    private void runValidation() {
        String targetXml = getEditorText(targetEngine);
        if (targetXml == null || targetXml.isEmpty()) {
            log("Error: Target XML is empty. Run transformation first.");
            return;
        }

        JSONObject targetCfg = config.optJSONObject("target");
        String xsdFile = targetCfg != null ? targetCfg.optString("xsd") : null;
        if (xsdFile == null || xsdFile.isEmpty()) {
            log("Error: No XSD schema defined in transformation.json");
            return;
        }

        File schemaFile = new File(folder, xsdFile);
        if (!schemaFile.exists()) {
            log("Error: XSD file not found: " + schemaFile.getAbsolutePath());
            return;
        }

        log("Validating target XML against " + xsdFile + "...");
        CompletableFuture.runAsync(() -> {
            try {
                String result = TransformationBackend.validateXml(targetXml, schemaFile);
                if (result == null) {
                    log("SUCCESS: XML is valid according to schema.");
                } else {
                    log("VALIDATION FAILED:\n" + result);
                }
            } catch (Exception e) {
                log("Validation Error: " + e.getMessage());
            }
        });
    }

    private void runTransformation() {
        String sourceXml = getEditorText(sourceXmlEngine);
        String logic = getEditorText(logicEngine);
        log("Running " + transformationType.toUpperCase() + " transformation...");
        
        CompletableFuture.runAsync(() -> {
            try {
                String result;
                if (isEnrichment) {
                    String originalXml = getEditorText(sourceRawEngine);
                    result = TransformationBackend.transformEnrichment(originalXml, sourceXml, logic);
                } else {
                    result = TransformationBackend.transform(sourceXml, logic, transformationType, null);
                }
                setEditorText(targetEngine, result);
                log("Transformation completed successfully.");
            } catch (Exception e) {
                setEditorText(targetEngine, "Transformation Error:\n" + e.getMessage());
                log("Error: Transformation failed. Check output panel.");
            }
        });
    }

    private void saveLogic() {
        try {
            JSONObject logicConfig = config.optJSONObject("logic");
            if (logicConfig == null) return;
            
            String fileName = logicConfig.optString("file");
            if (fileName == null || fileName.isEmpty()) return;
            
            File logicFile = new File(folder, fileName);
            String content = getEditorText(logicEngine);
            Files.writeString(logicFile.toPath(), content);
            log("Saved logic to " + logicFile.getName());
        } catch (Exception e) {
            log("Error saving logic: " + e.getMessage());
        }
    }
}
