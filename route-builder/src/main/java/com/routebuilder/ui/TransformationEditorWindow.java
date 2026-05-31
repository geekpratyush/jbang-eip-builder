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
    
    private com.routebuilder.ui.components.MonacoEditorPane editorSourceRaw;
    private com.routebuilder.ui.components.MonacoEditorPane editorSourceXml;
    private com.routebuilder.ui.components.MonacoEditorPane editorLogic;
    private com.routebuilder.ui.components.MonacoEditorPane editorTarget;
    private TextArea consoleArea;

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
        // MonacoEditorPane instances automatically respond to theme changes via ThemeManager
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
            
            VBox originalBox = new VBox(new Label("Original Source"), editorSourceRaw = new com.routebuilder.ui.components.MonacoEditorPane("xml"));
            VBox.setVgrow(editorSourceRaw, Priority.ALWAYS);
            
            VBox truncatedBox = new VBox(new Label("Truncated Source (to be enriched)"), editorSourceXml = new com.routebuilder.ui.components.MonacoEditorPane("xml"));
            VBox.setVgrow(editorSourceXml, Priority.ALWAYS);
            
            enrichSplit.getItems().addAll(originalBox, truncatedBox);
            sourcePanel.getChildren().add(enrichSplit);
            VBox.setVgrow(enrichSplit, Priority.ALWAYS);
        } else if (isNonXmlSource) {
            SplitPane sourceSplit = new SplitPane();
            sourceSplit.setOrientation(Orientation.VERTICAL);
            
            VBox rawBox = new VBox(new Label("Raw Source"), editorSourceRaw = new com.routebuilder.ui.components.MonacoEditorPane("text"));
            VBox.setVgrow(editorSourceRaw, Priority.ALWAYS);
            
            VBox xmlBox = new VBox(new Label("Converted XML"), editorSourceXml = new com.routebuilder.ui.components.MonacoEditorPane("xml"));
            VBox.setVgrow(editorSourceXml, Priority.ALWAYS);
            
            sourceSplit.getItems().addAll(rawBox, xmlBox);
            sourcePanel.getChildren().add(sourceSplit);
            VBox.setVgrow(sourceSplit, Priority.ALWAYS);
        } else {
            editorSourceXml = new com.routebuilder.ui.components.MonacoEditorPane("xml");
            sourcePanel.getChildren().add(editorSourceXml);
            VBox.setVgrow(editorSourceXml, Priority.ALWAYS);
        }

        // --- Panel 2: Logic ---
        VBox logicPanel = new VBox();
        Label lblLogic = new Label("LOGIC (" + transformationType.toUpperCase() + ")");
        lblLogic.getStyleClass().add("pane-title");
        editorLogic = new com.routebuilder.ui.components.MonacoEditorPane("xml");
        logicPanel.getChildren().addAll(lblLogic, editorLogic);
        VBox.setVgrow(editorLogic, Priority.ALWAYS);

        // --- Panel 3: Target ---
        VBox targetPanel = new VBox();
        Label lblTarget = new Label("TARGET");
        lblTarget.getStyleClass().add("pane-title");
        editorTarget = new com.routebuilder.ui.components.MonacoEditorPane("xml");
        targetPanel.getChildren().addAll(lblTarget, editorTarget);
        VBox.setVgrow(editorTarget, Priority.ALWAYS);

        horizontalSplit.getItems().addAll(sourcePanel, logicPanel, targetPanel);
        horizontalSplit.setDividerPositions(0.33, 0.66);

        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.getItems().addAll(horizontalSplit, consoleArea);
        mainSplit.setDividerPositions(0.8);
        
        root.setCenter(mainSplit);

        com.routebuilder.ui.components.ThemeManager.registerRoot(root);
        Scene scene = new Scene(root, 1200, 800);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            if (RouteBuilderApp.currentDynamicCssUri != null) {
                scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
            }
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.show();

        // Determine source languages from config
        String sourceRawLang = "text";
        String sourceXmlLang = "xml";
        
        JSONObject srcCfg = config.optJSONObject("source");
        if (srcCfg != null) {
            String type = srcCfg.optString("type", "xml").toLowerCase();
            if ("json".equals(type)) sourceRawLang = "json";
            else if ("xml".equals(type)) sourceRawLang = "xml";
            else if ("mt".equals(type) || "swift".equals(type)) sourceRawLang = "text";
        }

        if (editorSourceRaw != null) editorSourceRaw.setLanguage(sourceRawLang);
        if (editorSourceXml != null) editorSourceXml.setLanguage(sourceXmlLang);

        loadSourceData();
        loadLogicData();

        String logicLang = "xml";
        if ("jslt".equals(transformationType)) logicLang = "json";
        else if ("groovy".equals(transformationType)) logicLang = "java";
        editorLogic.setLanguage(logicLang);

        String targetLang = "xml";
        if (targetCfg != null) {
            String type = targetCfg.optString("type", "xml");
            if ("mt".equalsIgnoreCase(type) || "swift".equalsIgnoreCase(type)) {
                targetLang = "text"; 
            } else if ("json".equalsIgnoreCase(type)) {
                targetLang = "json";
            }
        }
        editorTarget.setLanguage(targetLang);
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            consoleArea.appendText("[" + time + "] " + msg + "\n");
        });
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
                    
                    if (f1.exists()) editorSourceRaw.setText(Files.readString(f1.toPath()));
                    if (f2.exists()) editorSourceXml.setText(Files.readString(f2.toPath()));
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
                    editorSourceRaw.setText(content);
                    unmarshalToXml(content);
                } else {
                    editorSourceXml.setText(content);
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
                editorLogic.setText(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unmarshalToXml(String rawContent) {
        CompletableFuture.runAsync(() -> {
            try {
                String xml = TransformationBackend.unmarshal(rawContent, config.optJSONObject("source"));
                editorSourceXml.setText(xml);
            } catch (Exception e) {
                editorSourceXml.setText("Error unmarshalling: " + e.getMessage());
            }
        });
    }

    private void runValidation() {
        String targetXml = editorTarget.getText();
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
        String sourceXml = editorSourceXml.getText();
        String logic = editorLogic.getText();
        log("Running " + transformationType.toUpperCase() + " transformation...");
        
        CompletableFuture.runAsync(() -> {
            try {
                String result;
                if (isEnrichment) {
                    String originalXml = editorSourceRaw.getText();
                    result = TransformationBackend.transformEnrichment(originalXml, sourceXml, logic);
                } else {
                    result = TransformationBackend.transform(sourceXml, logic, transformationType, null);
                }
                editorTarget.setText(result);
                log("Transformation completed successfully.");
            } catch (Exception e) {
                editorTarget.setText("Transformation Error:\n" + e.getMessage());
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
            String content = editorLogic.getText();
            Files.writeString(logicFile.toPath(), content);
            log("Saved logic to " + logicFile.getName());
        } catch (Exception e) {
            log("Error saving logic: " + e.getMessage());
        }
    }
}
