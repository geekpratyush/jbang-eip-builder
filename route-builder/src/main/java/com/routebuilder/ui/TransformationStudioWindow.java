package com.routebuilder.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.json.JSONObject;
import netscape.javascript.JSObject;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

public class TransformationStudioWindow {

    private final Stage stage;
    private final Preferences prefs;
    private File currentMappingsPath;
    
    private TreeView<File> mappingTree;
    private BorderPane mainContentArea;
    private Label lblStudioTitle;
    
    private Button btnRun, btnValidate, btnSave, btnBrowseXsd, btnConfig, btnSnippet, btnClose;
    
    private File currentFolder;
    private JSONObject currentConfig;
    private String transformationType = "xslt";
    private boolean isNonXmlSource = false;
    private boolean isEnrichment = false;
    private boolean isMtToMx = false;

    private WebView sourceRawWebView, sourceXmlWebView, logicWebView, logicSecondaryWebView, targetWebView;
    private WebEngine sourceRawEngine, sourceXmlEngine, logicEngine, logicSecondaryEngine, targetEngine;
    
    private File sourceRawFile, sourceXmlFile, logicFile, logicSecondaryFile;

    private TextArea consoleArea;
    private Process snippetProcess;
    private final List<Object> persistentBridges = new ArrayList<>();

    private boolean sourceRawInitialized, sourceXmlInitialized, logicInitialized, targetInitialized;

    public TransformationStudioWindow() {
        this.stage = new Stage();
        this.prefs = Preferences.userNodeForPackage(TransformationStudioWindow.class);
        String defaultPath = new File(System.getProperty("user.dir"), "test-mapping").exists() ?
            new File(System.getProperty("user.dir"), "test-mapping").getAbsolutePath() :
            new File(System.getProperty("user.dir"), "transformation-samples").getAbsolutePath();
        String savedPath = prefs.get("mappingsPath", defaultPath);
        this.currentMappingsPath = new File(savedPath);
        if (!this.currentMappingsPath.exists()) {
            this.currentMappingsPath.mkdirs();
        }
    }

    public void show() {
        stage.setTitle("Data Transformation Studio");

        BorderPane root = new BorderPane();
        root.getStyleClass().add(RouteBuilderApp.currentThemeClass);
        root.setStyle("-fx-background-color: #1e1e1e;");

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("editor-toolbar");
        btnRun = new Button("Run", new FontIcon("fas-play"));
        btnRun.getStyleClass().addAll("editor-btn", "btn-run");
        btnRun.setOnAction(e -> runTransformation());
        btnRun.setDisable(true);

        btnValidate = new Button("Validate", new FontIcon("fas-check-double"));
        btnValidate.getStyleClass().addAll("editor-btn", "btn-validate");
        btnValidate.setOnAction(e -> runValidation());
        btnValidate.setDisable(true);

        btnBrowseXsd = new Button("Browse XSD", new FontIcon("fas-file-medical"));
        btnBrowseXsd.getStyleClass().addAll("editor-btn", "btn-browse-xsd");
        btnBrowseXsd.setOnAction(e -> browseXsd());
        btnBrowseXsd.setDisable(true);

        btnSave = new Button("Save Config", new FontIcon("fas-save"));
        btnSave.getStyleClass().addAll("editor-btn", "btn-save-config");
        btnSave.setOnAction(e -> saveGlobalConfig());
        btnSave.setDisable(true);

        Button btnConfig = new Button("Set Mappings Path", new FontIcon("fas-cog"));
        btnConfig.getStyleClass().addAll("editor-btn", "btn-set-mappings");
        btnConfig.setOnAction(e -> chooseMappingsPath());

        lblStudioTitle = new Label("Select a mapping to begin");
        lblStudioTitle.setStyle("-fx-font-weight: bold; -fx-padding: 0 20; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(btnRun, btnValidate, btnBrowseXsd, btnSave, new Separator(), btnConfig, new Separator(), lblStudioTitle, spacer);
        root.setTop(toolBar);

        // --- Left Sidebar ---
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(10));
        sidebar.setSpacing(5);
        sidebar.setStyle("-fx-background-color: #252526; -fx-border-color: #333333; -fx-border-width: 0 1 0 0;");

        Label lblExplorer = new Label("MAPPING EXPLORER");
        lblExplorer.setStyle("-fx-font-size: 10; -fx-text-fill: #858585; -fx-font-weight: bold;");
        
        mappingTree = new TreeView<>();
        mappingTree.setShowRoot(false);
        mappingTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue().isDirectory()) {
                loadMapping(newVal.getValue());
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem newItem = new MenuItem("New Transformation...", new FontIcon("fas-plus-circle"));
        newItem.setOnAction(e -> showNewTransformationDialog());
        MenuItem newFolderItem = new MenuItem("New Folder...", new FontIcon("fas-folder-plus"));
        newFolderItem.setOnAction(e -> showNewFolderDialog());
        MenuItem deleteItem = new MenuItem("Delete Selected", new FontIcon("fas-trash-alt"));
        deleteItem.setOnAction(e -> deleteSelectedEntity());
        MenuItem refreshItem = new MenuItem("Refresh", new FontIcon("fas-sync"));
        refreshItem.setOnAction(e -> refreshMappingTree());
        contextMenu.getItems().addAll(newItem, newFolderItem, deleteItem, new SeparatorMenuItem(), refreshItem);
        mappingTree.setContextMenu(contextMenu);

        sidebar.getChildren().addAll(lblExplorer, mappingTree);

        // --- Main Content Area ---
        mainContentArea = new BorderPane();
        mainContentArea.setStyle("-fx-background-color: #1e1e1e;");
        
        Label lblPlaceholder = new Label("Select a transformation folder from the explorer");
        lblPlaceholder.setStyle("-fx-text-fill: #555; -fx-font-size: 18;");
        mainContentArea.setCenter(lblPlaceholder);

        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefHeight(120);
        consoleArea.setStyle("-fx-control-inner-background: #000; -fx-text-fill: #0f0; -fx-font-family: 'Consolas', monospace;");

        SplitPane verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().addAll(mainContentArea, consoleArea);
        verticalSplit.setDividerPositions(0.8);

        SplitPane mainSplit = new SplitPane();
        mainSplit.getItems().addAll(sidebar, verticalSplit);
        mainSplit.setDividerPositions(0.2);
        
        root.setCenter(mainSplit);

        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(TransformationStudioWindow.class.getResource("/styles/main.css").toExternalForm());
        if (RouteBuilderApp.currentDynamicCssUri != null) {
            scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
        }
        stage.setScene(scene);
        
        RouteBuilderApp.themedRoots.add(root);
        stage.setOnHidden(e -> {
            RouteBuilderApp.themedRoots.remove(root);
            if (snippetProcess != null && snippetProcess.isAlive()) {
                try {
                    snippetProcess.destroy();
                    snippetProcess.descendants().forEach(ProcessHandle::destroyForcibly);
                } catch (Exception ignored) {}
            }
        });
        
        stage.setMaximized(true);
        stage.show();

        refreshMappingTree();
    }

    private void chooseMappingsPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Mappings Base Directory");
        chooser.setInitialDirectory(currentMappingsPath.exists() ? currentMappingsPath : new File(System.getProperty("user.dir")));
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            currentMappingsPath = selected;
            prefs.put("mappingsPath", selected.getAbsolutePath());
            refreshMappingTree();
            log("Mappings path updated to: " + selected.getAbsolutePath());
        }
    }

    private void refreshMappingTree() {
        TreeItem<File> rootItem = new TreeItem<>(currentMappingsPath);
        buildTree(currentMappingsPath, rootItem);
        mappingTree.setRoot(rootItem);
        mappingTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    setText(item.getName());
                    File configFile = new File(item, "transformation.json");
                    if (configFile.exists()) {
                        setGraphic(new FontIcon("fas-exchange-alt"));
                        setStyle("-fx-text-fill: #4fc1ff;");
                    } else {
                        setGraphic(new FontIcon("fas-folder"));
                        setStyle("-fx-text-fill: #cccccc;");
                    }
                }
            }
        });
    }

    private void buildTree(File dir, TreeItem<File> parent) {
        File[] files = dir.listFiles();
        if (files != null) {
            java.util.Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            for (File f : files) {
                if (f.isDirectory() && !f.getName().startsWith(".")) {
                    TreeItem<File> item = new TreeItem<>(f);
                    parent.getChildren().add(item);
                    File configFile = new File(f, "transformation.json");
                    if (!configFile.exists()) buildTree(f, item);
                }
            }
        }
    }

    private void loadMapping(File folder) {
        File configFile = new File(folder, "transformation.json");
        if (!configFile.exists()) return;
        this.currentFolder = folder;
        try {
            String content = Files.readString(configFile.toPath());
            this.currentConfig = new JSONObject(content);
            String type = currentConfig.optString("type", "xslt");
            this.transformationType = type;
            this.isEnrichment = "enrichment".equalsIgnoreCase(type);
            
            JSONObject source = currentConfig.optJSONObject("source");
            String sourceType = source != null ? source.optString("type", "xml") : "xml";
            
            this.isMtToMx = "mt".equalsIgnoreCase(type) || "mt-to-mx".equalsIgnoreCase(type) || ("mt".equalsIgnoreCase(sourceType) && "xslt".equalsIgnoreCase(type));
            this.isNonXmlSource = !"xml".equalsIgnoreCase(sourceType) && !isEnrichment && !isMtToMx;

            lblStudioTitle.setText(currentConfig.optString("name", folder.getName()));
            log("Loading mapping: " + lblStudioTitle.getText());
            btnRun.setDisable(false); btnSave.setDisable(false); btnBrowseXsd.setDisable(false);
            updateValidationState();
            buildTransformationUI();
        } catch (Exception e) { log("Error loading config: " + e.getMessage()); }
    }

    private void updateValidationState() {
        JSONObject target = currentConfig.optJSONObject("target");
        if (target != null && "xml".equalsIgnoreCase(target.optString("type"))) {
            String xsd = target.optString("xsd");
            if (xsd != null && !xsd.isEmpty() && new File(currentFolder, xsd).exists()) {
                btnValidate.setDisable(false);
            } else btnValidate.setDisable(true);
        } else btnValidate.setDisable(true);
    }

    private void browseXsd() {
        if (currentFolder == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select XSD Schema");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XSD Files", "*.xsd"));
        File selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            try {
                File targetFile = new File(currentFolder, selected.getName());
                Files.copy(selected.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                JSONObject target = currentConfig.optJSONObject("target");
                if (target == null) { target = new JSONObject(); target.put("type", "xml"); currentConfig.put("target", target); }
                target.put("xsd", selected.getName());
                File configFile = new File(currentFolder, "transformation.json");
                Files.writeString(configFile.toPath(), currentConfig.toString(2));
                log("XSD imported: " + selected.getName());
                updateValidationState();
            } catch (Exception e) { log("Error importing XSD: " + e.getMessage()); }
        }
    }

    private void buildTransformationUI() {
        mainContentArea.setCenter(null);
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.setOrientation(Orientation.HORIZONTAL);

        sourceRawWebView = createWebView();
        sourceXmlWebView = createWebView();
        sourceRawEngine = sourceRawWebView.getEngine();
        sourceXmlEngine = sourceXmlWebView.getEngine();

        VBox sourcePanel = new VBox();
        if (isEnrichment || isMtToMx) {
            SplitPane sourceSplit = new SplitPane();
            sourceSplit.setOrientation(Orientation.VERTICAL);
            VBox topBox = new VBox(createHeader(isEnrichment ? "Original Source" : "Raw Source", sourceRawEngine, true, f -> sourceRawFile = f), sourceRawWebView);
            VBox.setVgrow(sourceRawWebView, Priority.ALWAYS);
            VBox bottomBox = new VBox(createHeader(isEnrichment ? "Truncated Source" : "Converted XML", sourceXmlEngine, isEnrichment, f -> sourceXmlFile = f), sourceXmlWebView);
            VBox.setVgrow(sourceXmlWebView, Priority.ALWAYS);
            sourceSplit.getItems().addAll(topBox, bottomBox);
            sourcePanel.getChildren().add(sourceSplit);
            VBox.setVgrow(sourceSplit, Priority.ALWAYS);
        } else {
            sourcePanel.getChildren().addAll(createHeader("SOURCE", sourceXmlEngine, true, f -> sourceXmlFile = f), sourceXmlWebView);
            VBox.setVgrow(sourceXmlWebView, Priority.ALWAYS);
        }

        VBox logicPanel = new VBox();
        logicWebView = createWebView();
        logicEngine = logicWebView.getEngine();
        
        org.json.JSONArray logicArr = currentConfig.optJSONArray("logic");
        boolean isSmooks = "smooks".equalsIgnoreCase(transformationType);
        if (logicArr != null && logicArr.length() > 1 && !isSmooks) {
            logicSecondaryWebView = createWebView();
            logicSecondaryEngine = logicSecondaryWebView.getEngine();
            SplitPane logicSplit = new SplitPane();
            logicSplit.setOrientation(Orientation.VERTICAL);
            VBox topBox = new VBox(createHeader("CONFIG (" + transformationType.toUpperCase() + ")", logicEngine, true, f -> logicFile = f), logicWebView);
            VBox.setVgrow(logicWebView, Priority.ALWAYS);
            VBox bottomBox = new VBox(createHeader("SCHEMA / MODEL", logicSecondaryEngine, true, f -> logicSecondaryFile = f), logicSecondaryWebView);
            VBox.setVgrow(logicSecondaryWebView, Priority.ALWAYS);
            logicSplit.getItems().addAll(topBox, bottomBox);
            logicPanel.getChildren().add(logicSplit);
            VBox.setVgrow(logicSplit, Priority.ALWAYS);
        } else {
            logicSecondaryEngine = null;
            logicPanel.getChildren().addAll(createHeader("LOGIC (" + transformationType.toUpperCase() + ")", logicEngine, true, f -> logicFile = f), logicWebView);
            VBox.setVgrow(logicWebView, Priority.ALWAYS);
        }

        VBox targetPanel = new VBox();
        targetWebView = createWebView();
        targetEngine = targetWebView.getEngine();
        targetPanel.getChildren().addAll(createHeader("TARGET", targetEngine, false, null), targetWebView);
        VBox.setVgrow(targetWebView, Priority.ALWAYS);

        horizontalSplit.getItems().addAll(sourcePanel, logicPanel, targetPanel);
        horizontalSplit.setDividerPositions(0.33, 0.66);
        mainContentArea.setCenter(horizontalSplit);
        initEditors();
    }

    private WebView createWebView() {
        WebView wv = new WebView();
        RouteBuilderApp.installClipboardShortcuts(wv);
        wv.setContextMenuEnabled(true);
        WebEngine engine = wv.getEngine();
        ClipboardBridge bridge = new ClipboardBridge();
        persistentBridges.add(bridge);
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
            }
        });
        return wv;
    }

    public class ClipboardBridge {
        public void copy(String text) {
            if (text != null && !text.isEmpty()) {
                Platform.runLater(() -> {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(text);
                    clipboard.setContent(content);
                    log("Copied " + text.length() + " chars to system clipboard.");
                });
            }
        }
    }

    private javafx.scene.Node createHeader(String title, WebEngine engine, boolean isFileBased, java.util.function.Consumer<File> onFileRefUpdated) {
        HBox header = new HBox(3);
        header.setPadding(new Insets(2, 5, 2, 8));
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getStyleClass().add("editor-panel-header");
        Label lbl = new Label(title);
        lbl.getStyleClass().add("editor-panel-header-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(lbl, spacer);
        if (isFileBased) {
            Button btnOpen = createSmallButton("fas-folder-open", "Open File");
            btnOpen.getStyleClass().add("btn-open-file");
            btnOpen.setOnAction(e -> openFileIntoEditor(engine, onFileRefUpdated));
            Button btnSave = createSmallButton("fas-save", "Save File");
            btnSave.getStyleClass().add("btn-save-file");
            btnSave.setOnAction(e -> saveEditorToFile(engine, false, onFileRefUpdated));
            header.getChildren().addAll(btnOpen, btnSave);
            if (title.contains("LOGIC") || title.contains("CONFIG")) {
                Button btnSnippet = createSmallButton("fas-code", "Camel Route Snippet Info");
                btnSnippet.getStyleClass().add("btn-snippet-info");
                btnSnippet.setOnAction(e -> showSnippetWindow());
                header.getChildren().add(btnSnippet);
            }
        }
        Button btnSaveAs = createSmallButton("fas-file-download", "Save As...");
        btnSaveAs.getStyleClass().add("btn-save-as-file");
        btnSaveAs.setOnAction(e -> saveEditorToFile(engine, true, onFileRefUpdated));
        Button btnCopy = createSmallButton("fas-copy", "Copy All");
        btnCopy.getStyleClass().add("btn-copy-text");
        btnCopy.setOnAction(e -> {
            Object val = engine.executeScript("window.getValue()");
            if (val instanceof String) {
                 Clipboard clipboard = Clipboard.getSystemClipboard();
                 ClipboardContent content = new ClipboardContent();
                 content.putString((String)val);
                 clipboard.setContent(content);
                 log("Copied " + ((String)val).length() + " chars to clipboard.");
            }
        });
        header.getChildren().addAll(btnSaveAs, btnCopy);
        return header;
    }

    private Button createSmallButton(String icon, String tooltip) {
        Button b = new Button();
        FontIcon fi = new FontIcon(icon);
        b.setGraphic(fi);
        b.setTooltip(new Tooltip(tooltip));
        b.getStyleClass().add("small-action-btn");
        return b;
    }

    private void openFileIntoEditor(WebEngine engine, java.util.function.Consumer<File> onFileRefUpdated) {
        FileChooser chooser = new FileChooser(); chooser.setInitialDirectory(currentFolder);
        File selected = chooser.showOpenDialog(stage);
        if (selected != null) { try { setEditorText(engine, Files.readString(selected.toPath())); if (onFileRefUpdated != null) onFileRefUpdated.accept(selected); log("Opened: " + selected.getName()); } catch (Exception e) { log("Open Error: " + e.getMessage()); } }
    }

    private void saveEditorToFile(WebEngine engine, boolean isSaveAs, java.util.function.Consumer<File> onFileRefUpdated) {
        String content = getEditorText(engine); File target = null;
        if (!isSaveAs) { 
            if (engine == sourceRawEngine) target = sourceRawFile; 
            else if (engine == sourceXmlEngine) target = sourceXmlFile; 
            else if (engine == logicEngine) target = logicFile; 
            else if (engine == logicSecondaryEngine) target = logicSecondaryFile;
        }
        if (target == null) { FileChooser chooser = new FileChooser(); chooser.setInitialDirectory(currentFolder); target = chooser.showSaveDialog(stage); }
        if (target != null) { try { Files.writeString(target.toPath(), content); if (onFileRefUpdated != null) onFileRefUpdated.accept(target); log("Saved: " + target.getName()); } catch (Exception e) { log("Save Error: " + e.getMessage()); } }
    }

    private void initEditors() {
        sourceRawInitialized = sourceXmlInitialized = logicInitialized = targetInitialized = false;
        if (isMtToMx || isEnrichment) setupMonaco(sourceRawEngine, "text", (obs, oldVal, newVal) -> { sourceRawInitialized = true; checkAndLoadData(); });
        
        String sourceLang = "xml";
        JSONObject srcCfg = currentConfig.optJSONObject("source");
        if (srcCfg != null) {
            String type = srcCfg.optString("type", "xml");
            if ("json".equalsIgnoreCase(type)) sourceLang = "json";
            else if ("text".equalsIgnoreCase(type) || "csv".equalsIgnoreCase(type) || "edi".equalsIgnoreCase(type)) sourceLang = "text";
        }
        setupMonaco(sourceXmlEngine, sourceLang, (obs, oldVal, newVal) -> { sourceXmlInitialized = true; if (!isMtToMx && !isEnrichment) checkAndLoadData(); });
        
        String logicLang = "jslt".equals(transformationType) ? "json" : ("groovy".equals(transformationType) || "joor".equals(transformationType) ? "java" : "xml");
        setupMonaco(logicEngine, logicLang, (obs, oldVal, newVal) -> { logicInitialized = true; checkAndLoadData(); });
        
        if (logicSecondaryEngine != null) {
            setupMonaco(logicSecondaryEngine, "xml", (obs, oldVal, newVal) -> {});
        }
        
        String targetLang = "xml"; JSONObject targetCfg = currentConfig.optJSONObject("target");
        if (targetCfg != null) { String type = targetCfg.optString("type", "xml"); if ("mt".equalsIgnoreCase(type) || "swift".equalsIgnoreCase(type)) targetLang = "text"; else if ("json".equalsIgnoreCase(type)) targetLang = "json"; }
        setupMonaco(targetEngine, targetLang, (obs, oldVal, newVal) -> { targetInitialized = true; });
    }

    private void checkAndLoadData() { if (logicInitialized && (sourceXmlInitialized || sourceRawInitialized)) { loadSourceData(); loadLogicData(); } }

    private void setupMonaco(WebEngine engine, String language, javafx.beans.value.ChangeListener<javafx.concurrent.Worker.State> onSucceeded) {
        String monacoBase = getClass().getResource("/monaco/vs/loader.js").toExternalForm();
        monacoBase = monacoBase.substring(0, monacoBase.lastIndexOf("/vs/loader.js"));
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>body{margin:0;padding:0;overflow:hidden;background-color:#1e1e1e;}#editor{width:100vw;height:100vh;}</style></head><body><div id='editor'></div><script src='" + monacoBase + "/vs/loader.js'></script><script>\n" +
            "window.editorValue = ''; window.setValue = function(val) { window.editorValue = val; if(window.editor) window.editor.setValue(val); };\n" +
            "window.getValue = function() { return window.editor ? window.editor.getValue() : window.editorValue; };\n" +
            "window.getSelection = function() { if(!window.editor) return ''; var sel = window.editor.getSelection(); return window.editor.getModel().getValueInRange(sel); };\n" +
            "require.config({ paths: { vs: '" + monacoBase + "/vs' }});\n" +
            "require(['vs/editor/editor.main'], function() {\n" +
            "  monaco.languages.register({ id: 'swift-mt' });\n" +
            "  monaco.languages.setMonarchTokensProvider('swift-mt', { tokenizer: { root: [ [/{[1-5]:/, 'metatag'], [/}/, 'metatag'], [/^:[0-9A-Z]{2,3}:/, 'keyword'], [/-}/, 'metatag'], [/\\n:[0-9A-Z]{2,3}:/, 'keyword'] ] } });\n" +
            "  monaco.editor.defineTheme('swift-dark', { base: 'vs-dark', inherit: true, rules: [ { token: 'keyword', foreground: '569cd6', fontStyle: 'bold' }, { token: 'metatag', foreground: 'ce9178' } ], colors: { 'editor.background': '#1e1e1e' } });\n" +
            "  window.editor = monaco.editor.create(document.getElementById('editor'), { value: window.editorValue, language: '" + ("text".equals(language) ? "swift-mt" : language) + "', theme: 'swift-dark', automaticLayout: true, minimap: { enabled: false }, fontSize: 12 });\n" +
            "  window.editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyC, function() {\n" +
            "     var text = window.getSelection(); if(!text) text = window.getValue();\n" +
            "     if(window.javaBridge) window.javaBridge.copy(text);\n" +
            "  });\n" +
            "});\n</script></body></html>";
        if (onSucceeded != null) {
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> { if (newState == javafx.concurrent.Worker.State.SUCCEEDED) onSucceeded.changed(null, oldState, newState); });
        }
        engine.loadContent(html);
    }

    private void setEditorText(WebEngine engine, String text) {
        if (text == null) text = ""; final String finalT = text;
        Platform.runLater(() -> { try { String encoded = java.net.URLEncoder.encode(finalT, "UTF-8").replace("+", "%20"); engine.executeScript("if(window.setValue) window.setValue(decodeURIComponent('" + encoded + "')); else window.editorValue = decodeURIComponent('" + encoded + "');"); } catch (Exception ignored) { } });
    }

    private String getEditorText(WebEngine engine) { try { Object result = engine.executeScript("window.getValue()"); return (result instanceof String) ? (String) result : ""; } catch (Exception e) { return ""; } }

    private void loadSourceData() {
        try {
            if (isEnrichment) {
                org.json.JSONArray sources = currentConfig.optJSONArray("sources");
                if (sources != null && sources.length() >= 2) {
                    JSONObject s1 = sources.getJSONObject(0); JSONObject s2 = sources.getJSONObject(1);
                    sourceRawFile = new File(currentFolder, s1.optString("file")); sourceXmlFile = new File(currentFolder, s2.optString("file"));
                    if (sourceRawFile.exists()) setEditorText(sourceRawEngine, Files.readString(sourceRawFile.toPath()));
                    if (sourceXmlFile.exists()) setEditorText(sourceXmlEngine, Files.readString(sourceXmlFile.toPath()));
                }
                return;
            }
            JSONObject srcCfg = currentConfig.optJSONObject("source");
            if (srcCfg != null) {
                File f = new File(currentFolder, srcCfg.optString("file"));
                if (f.exists()) {
                    String content = Files.readString(f.toPath());
                    if (isMtToMx) { sourceRawFile = f; setEditorText(sourceRawEngine, content); unmarshalToXml(content); } 
                    else { sourceXmlFile = f; setEditorText(sourceXmlEngine, content); }
                }
            }
        } catch (Exception e) { log("Error loading source: " + e.getMessage()); }
    }

    private void loadLogicData() { 
        try { 
            org.json.JSONArray logicArr = currentConfig.optJSONArray("logic");
            if (logicArr != null && logicArr.length() > 0) {
                JSONObject primary = logicArr.getJSONObject(0);
                logicFile = new File(currentFolder, primary.optString("file")); 
                if (logicFile.exists()) setEditorText(logicEngine, Files.readString(logicFile.toPath()));
                
                if (logicArr.length() > 1 && logicSecondaryEngine != null) {
                    JSONObject secondary = logicArr.getJSONObject(1);
                    logicSecondaryFile = new File(currentFolder, secondary.optString("file"));
                    if (logicSecondaryFile.exists()) setEditorText(logicSecondaryEngine, Files.readString(logicSecondaryFile.toPath()));
                }
                return;
            }
            
            JSONObject logicCfg = currentConfig.optJSONObject("logic"); 
            if (logicCfg != null) { 
                logicFile = new File(currentFolder, logicCfg.optString("file")); 
                if (logicFile.exists()) setEditorText(logicEngine, Files.readString(logicFile.toPath())); 
            } 
        } catch (Exception e) { log("Error loading logic: " + e.getMessage()); } 
    }

    private void unmarshalToXml(String rawContent) { CompletableFuture.runAsync(() -> { try { String xml = TransformationBackend.unmarshal(rawContent, currentConfig.optJSONObject("source")); setEditorText(sourceXmlEngine, xml); } catch (Exception e) { setEditorText(sourceXmlEngine, "Error: " + e.getMessage()); } }); }

    private void runTransformation() {
        if (currentFolder == null) return;
        String sourceText = getEditorText(sourceXmlEngine);
        String logic = getEditorText(logicEngine); 
        
        // Save secondary logic file if present (e.g. DFDL schema)
        if (logicSecondaryEngine != null && logicSecondaryFile != null) {
            try {
                Files.writeString(logicSecondaryFile.toPath(), getEditorText(logicSecondaryEngine));
            } catch (Exception ignored) {}
        }
        
        String rawSource = isEnrichment ? getEditorText(sourceRawEngine) : "";
        
        String execType = currentConfig.optString("type", "xslt"); if (execType.contains("-to-")) execType = "xslt"; final String finalExecType = execType;
        log("Running " + finalExecType.toUpperCase() + "...");
        
        CompletableFuture.runAsync(() -> { 
            try { 
                String result = isEnrichment ? TransformationBackend.transformEnrichment(rawSource, sourceText, logic) : TransformationBackend.transform(sourceText, logic, finalExecType, currentFolder); 
                setEditorText(targetEngine, result); 
                log("Transformation Success."); 
            } catch (Exception e) { 
                log("Transformation Failed: " + e.getMessage()); 
                setEditorText(targetEngine, "Error: " + e.getMessage()); 
            } 
        });
    }

    private void runValidation() {
        if (currentFolder == null) return;
        String xml = getEditorText(targetEngine); JSONObject targetCfg = currentConfig.optJSONObject("target"); String xsdName = targetCfg != null ? targetCfg.optString("xsd") : null;
        if (xsdName == null) { log("No XSD defined."); return; }
        File xsd = new File(currentFolder, xsdName); if (!xsd.exists()) { log("XSD not found: " + xsdName); return; }
        log("Validating against " + xsdName + "..."); CompletableFuture.runAsync(() -> { String err = TransformationBackend.validateXml(xml, xsd); log(err == null ? "VALIDATION SUCCESS." : "VALIDATION FAILED:\n" + err); });
    }

    private void saveGlobalConfig() { if (currentFolder == null) return; try { File configFile = new File(currentFolder, "transformation.json"); Files.writeString(configFile.toPath(), currentConfig.toString(2)); log("Saved global config."); } catch (Exception e) { log("Save Config Error: " + e.getMessage()); } }

    private File getSelectedParentFolder() {
        TreeItem<File> selectedItem = mappingTree.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getValue() == null) {
            return currentMappingsPath;
        }
        File selected = selectedItem.getValue();
        if (selected.isDirectory()) {
            File configFile = new File(selected, "transformation.json");
            if (configFile.exists()) {
                return selected.getParentFile();
            }
            return selected;
        }
        return currentMappingsPath;
    }

    private void showNewFolderDialog() {
        File parentDir = getSelectedParentFolder();
        TextInputDialog dialog = new TextInputDialog("new-folder");
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a new folder under: " + parentDir.getName());
        dialog.setContentText("Folder Name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                File newFolder = new File(parentDir, name.trim());
                if (newFolder.exists()) {
                    log("Error: Folder already exists: " + name);
                } else {
                    newFolder.mkdirs();
                    log("Created folder: " + newFolder.getAbsolutePath());
                    refreshMappingTree();
                }
            }
        });
    }

    private void deleteSelectedEntity() {
        TreeItem<File> selectedItem = mappingTree.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getValue() == null) {
            return;
        }
        File file = selectedItem.getValue();
        if (file.equals(currentMappingsPath)) {
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        RouteBuilderApp.themeDialog(alert);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete " + (file.isDirectory() ? "Folder" : "File"));
        alert.setContentText("Are you sure you want to delete this entity and all its contents?\n\n" + file.getAbsolutePath());
        
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                try {
                    deleteFolderRecursively(file);
                    log("Deleted: " + file.getAbsolutePath());
                    if (currentFolder != null && (file.equals(currentFolder) || isChildOf(currentFolder, file))) {
                        currentFolder = null;
                        currentConfig = null;
                        mainContentArea.setCenter(new Label("Select a transformation folder from the explorer"));
                        btnRun.setDisable(true);
                        btnValidate.setDisable(true);
                        btnBrowseXsd.setDisable(true);
                        btnSave.setDisable(true);
                    }
                    refreshMappingTree();
                } catch (Exception ex) {
                    log("Error deleting: " + ex.getMessage());
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    RouteBuilderApp.themeDialog(err);
                    err.setTitle("Delete Error");
                    err.setHeaderText("Failed to delete entity");
                    err.setContentText(ex.getMessage());
                    err.showAndWait();
                }
            }
        });
    }

    private boolean isChildOf(File child, File parent) {
        File p = child.getParentFile();
        while (p != null) {
            if (p.equals(parent)) return true;
            p = p.getParentFile();
        }
        return false;
    }

    private void deleteFolderRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFolderRecursively(child);
                }
            }
        }
        file.delete();
    }

    private void showNewTransformationDialog() {
        File parentDir = getSelectedParentFolder();
        Dialog<JSONObject> dialog = new Dialog<>();
        RouteBuilderApp.themeDialog(dialog);
        dialog.setTitle("New Transformation Wizard");
        dialog.setHeaderText("Create a new mapping project under: " + parentDir.getName());
        
        ButtonType btnCreateType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCreateType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField txtTitle = new TextField();
        txtTitle.setText("Transformation Type Name");
        txtTitle.setPrefWidth(250);
        
        TextField txtName = new TextField();
        txtName.setText("transformation-type-name");
        txtName.setPrefWidth(250);
        
        boolean[] userModifiedFolder = {false};
        txtName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (txtName.isFocused()) {
                userModifiedFolder[0] = true;
            }
        });
        
        txtTitle.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!userModifiedFolder[0]) {
                if (newVal == null || newVal.isEmpty()) {
                    txtName.setText("");
                } else {
                    String folderSafe = newVal.trim().toLowerCase()
                                             .replaceAll("[^a-z0-9\\s-]", "")
                                             .replaceAll("\\s+", "-");
                    txtName.setText(folderSafe);
                }
            }
        });
        
        ComboBox<String> comboScenario = new ComboBox<>();
        comboScenario.getItems().addAll(
            "MT to MX (SWIFT to ISO)",
            "MX to MT (ISO to SWIFT)",
            "JSON to JSON (JSLT)",
            "CSV to XML (Smooks)",
            "EDI to XML (Smooks)",
            "Fixed Format (Flatpack)",
            "Custom Groovy Script",
            "Java Joor Mapper"
        );
        comboScenario.setValue("MT to MX (SWIFT to ISO)");
        
        grid.add(new Label("Title/Description:"), 0, 0);
        grid.add(txtTitle, 1, 0);
        grid.add(new Label("Folder Name:"), 0, 1);
        grid.add(txtName, 1, 1);
        grid.add(new Label("Mapping Scenario:"), 0, 2);
        grid.add(comboScenario, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnCreateType) {
                JSONObject res = new JSONObject();
                res.put("title", txtTitle.getText());
                res.put("name", txtName.getText());
                res.put("scenario", comboScenario.getValue());
                return res;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(res -> {
            String title = res.optString("title");
            String folderName = res.optString("name");
            String scenario = res.optString("scenario");
            if (folderName != null && !folderName.isEmpty()) {
                if (title == null || title.trim().isEmpty()) {
                    title = folderName.replace("-", " ").toUpperCase();
                }
                createTransformationTemplate(parentDir, title.trim(), folderName.trim(), scenario);
            }
        });
    }

    private void createTransformationTemplate(File parentDir, String title, String folderName, String scenario) {
        File newFolder = new File(parentDir, folderName);
        if (newFolder.exists()) {
            log("Error: Folder already exists: " + folderName);
            return;
        }
        newFolder.mkdirs();
        try {
            JSONObject config = new JSONObject();
            config.put("name", title);
            String sourceFile = "source.xml", logicFile = "transform.xslt", sourceType = "xml", logicType = "xslt", targetType = "xml";
            String sourceContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n  <data>Sample</data>\n</root>";
            String logicContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n  <xsl:template match=\"/\">\n    <output>\n       <val><xsl:value-of select=\"//data\"/></val>\n    </output>\n  </xsl:template>\n</xsl:stylesheet>";
            if (scenario.startsWith("MT to MX")) {
                config.put("type", "mt-to-mx");
                sourceFile = "source.txt";
                sourceType = "mt";
                sourceContent = "{1:F01SENDERBKAXXX0000000000}{2:I103RECEIVERBKAXXXN}{4:\n:20:REF-123\n:32A:260524USD1000,\n-}";
                logicContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n  <xsl:template match=\"/message\">\n    <ISO20022>\n       <MsgId><xsl:value-of select=\"//tag[name='20']/value\"/></MsgId>\n    </ISO20022>\n  </xsl:template>\n</xsl:stylesheet>";
            } else if (scenario.startsWith("MX to MT")) {
                config.put("type", "mx-to-mt");
                targetType = "mt";
            } else if (scenario.contains("JSLT")) {
                config.put("type", "jslt");
                sourceFile = "source.json";
                sourceType = "json";
                targetType = "json";
                logicFile = "transform.jslt";
                logicType = "jslt";
                sourceContent = "{\n  \"id\": \"123\",\n  \"user\": \"Admin\"\n}";
                logicContent = "{\n  \"result\": .id,\n  \"name\": .user\n}";
            } else if (scenario.contains("CSV")) {
                config.put("type", "smooks");
                sourceFile = "source.csv";
                sourceType = "text";
                logicFile = "smooks-config.xml";
                logicType = "xml";
                sourceContent = "id,name\n1,Pratyush";
                logicContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<smooks-resource-list xmlns=\"https://www.smooks.org/xsd/smooks-2.0.xsd\" xmlns:csv=\"https://www.smooks.org/xsd/smooks/csv-1.7.xsd\">\n  <csv:reader fields=\"id,name\" skipLines=\"1\" />\n</smooks-resource-list>";
            } else if (scenario.contains("EDI")) {
                config.put("type", "smooks");
                sourceFile = "source.edi";
                sourceType = "text";
                logicFile = "smooks-config.xml";
                logicType = "xml";
                sourceContent = "HDR*123*A'";
                logicContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<smooks-resource-list xmlns=\"https://www.smooks.org/xsd/smooks-2.0.xsd\" xmlns:dfdl=\"https://www.smooks.org/xsd/smooks/dfdl-1.0.xsd\">\n  <dfdl:parser schemaUri=\"mapping.dfdl.xsd\" />\n</smooks-resource-list>";
            } else if (scenario.contains("Flatpack")) {
                config.put("type", "flatpack");
                sourceFile = "source.txt";
                sourceType = "text";
                logicFile = "definition.xml";
                logicType = "xml";
                sourceContent = "001PRATYUSH  LONDON";
                logicContent = "<flatpack>\n  <column name=\"ID\" length=\"3\" />\n  <column name=\"NAME\" length=\"10\" />\n</flatpack>";
            } else if (scenario.contains("Groovy")) {
                config.put("type", "groovy");
                logicFile = "transform.groovy";
                logicType = "java";
                logicContent = "// Groovy Transform: 'body' is the input\nreturn \"Result: \" + body";
            } else if (scenario.contains("Joor")) {
                config.put("type", "joor");
                logicFile = "Transform.java";
                logicType = "java";
                logicContent = "// Java Snippet (jOOR): 'body' is the input\nreturn ((String)body).replace(\"<order>\", \"<processed_order>\");";
            }
            
            JSONObject source = new JSONObject();
            source.put("type", sourceType);
            source.put("file", sourceFile);
            config.put("source", source);
            
            if (scenario.contains("EDI")) {
                org.json.JSONArray logicArr = new org.json.JSONArray();
                JSONObject l1 = new JSONObject();
                l1.put("type", "xml");
                l1.put("file", "smooks-config.xml");
                logicArr.put(l1);
                JSONObject l2 = new JSONObject();
                l2.put("type", "xml");
                l2.put("file", "mapping.dfdl.xsd");
                logicArr.put(l2);
                config.put("logic", logicArr);
                String dfdl = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
                    "           xmlns:dfdl=\"http://www.ogf.org/dfdl/dfdl-1.0/\"\n" +
                    "           targetNamespace=\"http://example.com\"\n" +
                    "           elementFormDefault=\"unqualified\">\n" +
                    "  <xs:annotation>\n" +
                    "    <xs:appinfo source=\"http://www.ogf.org/dfdl/\">\n" +
                    "      <dfdl:format alignment=\"1\" alignmentUnits=\"bytes\" encoding=\"UTF-8\" byteOrder=\"bigEndian\" ignoreCase=\"yes\" utf16Width=\"fixed\" textNumberRep=\"standard\" lengthKind=\"delimited\" initiator=\"\" terminator=\"\" separator=\"\" separatorPosition=\"infix\" occursCountKind=\"implicit\" representation=\"text\" truncateSpecifiedLengthString=\"no\" textBidi=\"no\" floating=\"no\" sequenceKind=\"ordered\" escapeSchemeRef=\"\" leadingSkip=\"0\" trailingSkip=\"0\" encodingErrorPolicy=\"replace\" nilValueDelimiterPolicy=\"none\" emptyValueDelimiterPolicy=\"none\" documentFinalTerminatorCanBeMissing=\"yes\" textOutputMinLength=\"0\" textPadKind=\"none\" textTrimKind=\"none\" initiatedContent=\"no\" outputNewLine=\"%LF;\" fillByte=\"%#r20;\" separatorSuppressionPolicy=\"anyEmpty\" />\n" +
                    "    </xs:appinfo>\n" +
                    "  </xs:annotation>\n" +
                    "  <xs:element name=\"Interchange\">\n" +
                    "    <xs:complexType>\n" +
                    "      <xs:sequence dfdl:separator=\"*\" dfdl:terminator=\"'\">\n" +
                    "        <xs:element name=\"Segment\" type=\"xs:string\" />\n" +
                    "        <xs:element name=\"ID\" type=\"xs:string\" />\n" +
                    "        <xs:element name=\"Status\" type=\"xs:string\" />\n" +
                    "      </xs:sequence>\n" +
                    "    </xs:complexType>\n" +
                    "  </xs:element>\n" +
                    "</xs:schema>";
                Files.writeString(new File(newFolder, "mapping.dfdl.xsd").toPath(), dfdl);
            } else {
                JSONObject logic = new JSONObject();
                logic.put("type", logicType);
                logic.put("file", logicFile);
                config.put("logic", logic);
            }
            
            JSONObject target = new JSONObject();
            target.put("type", targetType);
            config.put("target", target);
            
            Files.writeString(new File(newFolder, "transformation.json").toPath(), config.toString(2));
            Files.writeString(new File(newFolder, sourceFile).toPath(), sourceContent);
            Files.writeString(new File(newFolder, logicFile).toPath(), logicContent);
            log("Project created: " + folderName);
            refreshMappingTree();
        } catch (Exception e) {
            log("Error creating project: " + e.getMessage());
        }
    }

    private static String indentString(String input, int spaces) {
        if (input == null || input.isEmpty()) return "";
        String normalized = input.replace("\r\n", "\n").replace("\r", "\n");
        String indent = " ".repeat(spaces);
        return indent + normalized.replace("\n", "\n" + indent);
    }

    private void showSnippetWindow() {
        if (logicFile == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            RouteBuilderApp.themeDialog(alert);
            alert.setTitle("Code Snippet Info");
            alert.setHeaderText("No active logic file loaded");
            alert.setContentText("Please select or create a mapping transformation project first.");
            alert.showAndWait();
            return;
        }

        String type = transformationType != null ? transformationType.toLowerCase() : "xslt";
        if ("enrichment".equals(type)) {
            type = "xslt";
        }
        if ("ftl".equals(type)) {
            type = "freemarker";
        }
        
        JSONObject sourceCfg = currentConfig.optJSONObject("source");
        String sourceType = sourceCfg != null ? sourceCfg.optString("type", "xml") : "xml";
        boolean isMtSource = "mt".equalsIgnoreCase(sourceType) || "mt".equalsIgnoreCase(type);

        String path = logicFile.getAbsolutePath();
        String groupId = "org.apache.camel";
        String artifactId = "camel-" + type;
        String version = "4.20.0";

        // Determine all necessary dependencies (cartridges + Camel components)
        java.util.List<String> deps = new java.util.ArrayList<>();
        if (isMtSource) {
            deps.add("org.apache.camel:camel-swift:4.18.2");
            deps.add("org.apache.camel:camel-xslt:4.20.0");
        } else if ("smooks".equals(type)) {
            deps.add("org.apache.camel:camel-smooks:4.20.0");
            try {
                String xmlContent = Files.readString(logicFile.toPath());
                if (xmlContent.contains("csv") || xmlContent.contains("<csv:") || xmlContent.contains("xmlns:csv")) {
                    deps.add("org.smooks.cartridges:smooks-csv-cartridge:2.0.3");
                } else if (xmlContent.contains("fixed-length") || xmlContent.contains("<fl:") || xmlContent.contains("fixed-length-1.4.xsd")) {
                    deps.add("org.smooks.cartridges:smooks-fixed-length-cartridge:2.0.3");
                } else if (xmlContent.contains("json") || xmlContent.contains("<json:") || xmlContent.contains("xmlns:json")) {
                    deps.add("org.smooks.cartridges:smooks-json-cartridge:2.0.3");
                } else if (xmlContent.contains("yaml") || xmlContent.contains("<yaml:") || xmlContent.contains("xmlns:yaml")) {
                    deps.add("org.smooks.cartridges:smooks-yaml-cartridge:2.0.3");
                } else if (xmlContent.contains("edi") || xmlContent.contains("<edi:") || xmlContent.contains("xmlns:edi")) {
                    deps.add("org.smooks.cartridges:smooks-edifact-cartridge:2.0.3");
                }
            } catch (Exception ignored) {}
        } else if ("groovy".equals(type)) {
            deps.add("org.apache.camel:camel-groovy:4.20.0");
        } else if ("joor".equals(type)) {
            deps.add("org.apache.camel:camel-joor:4.18.2");
        } else if ("mt".equalsIgnoreCase(type)) {
            deps.add("org.apache.camel:camel-swift:4.18.2");
            deps.add("org.apache.camel:camel-xslt:4.20.0");
        } else if ("xslt".equalsIgnoreCase(type) || "mt-to-mx".equalsIgnoreCase(type) || "mx-to-mt".equalsIgnoreCase(type)) {
            deps.add("org.apache.camel:camel-xslt:4.20.0");
        } else {
            deps.add(groupId + ":" + artifactId + ":" + version);
        }

        // Format dependencies for the dependency tabs
        StringBuilder jbangDepsBuilder = new StringBuilder();
        StringBuilder gradleDepsBuilder = new StringBuilder();
        StringBuilder mavenDepsBuilder = new StringBuilder();
        for (String dep : deps) {
            jbangDepsBuilder.append("//DEPS ").append(dep).append("\n");
            gradleDepsBuilder.append("implementation(\"").append(dep).append("\")\n");
            String[] parts = dep.split(":");
            mavenDepsBuilder.append("<dependency>\n")
                            .append("    <groupId>").append(parts[0]).append("</groupId>\n")
                            .append("    <artifactId>").append(parts[1]).append("</artifactId>\n")
                            .append("    <version>").append(parts[2]).append("</version>\n")
                            .append("</dependency>\n");
        }
        String jbangDepsStr = jbangDepsBuilder.toString().trim();
        String gradleDepsStr = gradleDepsBuilder.toString().trim();
        String mavenDepsStr = mavenDepsBuilder.toString().trim();

        // Retrieve source content from editor or file
        String sourceMsg = "";
        String originalMsg = "";
        String truncatedMsg = "";
        if (isEnrichment) {
            originalMsg = getEditorText(sourceRawEngine);
            truncatedMsg = getEditorText(sourceXmlEngine);
            if (originalMsg == null || originalMsg.trim().isEmpty()) {
                if (sourceRawFile != null && sourceRawFile.exists()) {
                    try { originalMsg = Files.readString(sourceRawFile.toPath()); } catch (Exception ignored) {}
                }
            }
            if (truncatedMsg == null || truncatedMsg.trim().isEmpty()) {
                if (sourceXmlFile != null && sourceXmlFile.exists()) {
                    try { truncatedMsg = Files.readString(sourceXmlFile.toPath()); } catch (Exception ignored) {}
                }
            }
            if (originalMsg == null) originalMsg = "";
            if (truncatedMsg == null) truncatedMsg = "";
            sourceMsg = TransformationBackend.combineEnrichmentXml(originalMsg, truncatedMsg);
        } else {
            if (isMtToMx) {
                sourceMsg = getEditorText(sourceRawEngine);
            } else {
                sourceMsg = getEditorText(sourceXmlEngine);
            }
            if (sourceMsg == null || sourceMsg.trim().isEmpty()) {
                if (sourceRawFile != null && sourceRawFile.exists()) {
                    try { sourceMsg = Files.readString(sourceRawFile.toPath()); } catch (Exception ignored) {}
                }
                if ((sourceMsg == null || sourceMsg.trim().isEmpty()) && sourceXmlFile != null && sourceXmlFile.exists()) {
                    try { sourceMsg = Files.readString(sourceXmlFile.toPath()); } catch (Exception ignored) {}
                }
            }
            if (sourceMsg == null) sourceMsg = "";
        }

        // Build route steps based on type
        String yamlStep = "";
        String javaStep = "";
        String xmlStep = "";

        boolean isFlatpackFixed = false;
        if ("flatpack".equals(type)) {
            try {
                String defContent = Files.readString(logicFile.toPath());
                if (defContent.contains("length=")) {
                    isFlatpackFixed = true;
                }
            } catch (Exception ignored) {}
        }

        if ("xslt".equalsIgnoreCase(type) || isMtSource || "mt-to-mx".equalsIgnoreCase(type) || "mx-to-mt".equalsIgnoreCase(type)) {
            if (isMtSource || "mt-to-mx".equalsIgnoreCase(type)) {
                yamlStep = "unmarshal:\n            swiftMt: {}\n        - setBody:\n            simple: \"${body.xml}\"\n        - to:\n            uri: \"xslt-saxon:file:" + path + "\"";
                javaStep = ".unmarshal().swiftMt()\n            .setBody().simple(\"${body.xml}\")\n            .to(\"xslt-saxon:file:" + path + "\")";
                xmlStep = "<unmarshal>\n            <swiftMt/>\n        </unmarshal>\n        <setBody>\n            <simple>${body.xml}</simple>\n        </setBody>\n        <to uri=\"xslt-saxon:file:" + path + "\"/>";
            } else if ("mx-to-mt".equalsIgnoreCase(type)) {
                yamlStep = "to:\n            uri: \"xslt-saxon:file:" + path + "\"\n        - marshal:\n            swiftMt: {}";
                javaStep = ".to(\"xslt-saxon:file:" + path + "\")\n            .marshal().swiftMt()";
                xmlStep = "<to uri=\"xslt-saxon:file:" + path + "\"/>\n        <marshal>\n            <swiftMt/>\n        </marshal>";
            } else {
                yamlStep = "to:\n            uri: \"xslt-saxon:file:" + path + "\"";
                javaStep = ".to(\"xslt-saxon:file:" + path + "\")";
                xmlStep = "<to uri=\"xslt-saxon:file:" + path + "\"/>";
            }
        } else if ("jslt".equals(type)) {

            yamlStep = "to:\n            uri: \"jslt:file:" + path + "\"";
            javaStep = ".to(\"jslt:file:" + path + "\")";
            xmlStep = "<to uri=\"jslt:file:" + path + "\"/>";
        } else if ("smooks".equals(type)) {
            yamlStep = "unmarshal:\n            smooks:\n              smooksConfig: \"file:" + path + "\"";
            javaStep = ".unmarshal().smooks(\"file:" + path + "\")";
            xmlStep = "<unmarshal>\n            <smooks smooksConfig=\"file:" + path + "\"/>\n        </unmarshal>";
        } else if ("flatpack".equals(type)) {
            yamlStep = "unmarshal:\n" +
                       "            flatpack:\n" +
                       "              fixed: " + isFlatpackFixed + "\n" +
                       "              definition: \"file:" + path + "\"\n" +
                       "        - split:\n" +
                       "            simple: \"${body}\"\n" +
                       "            steps:\n" +
                       "              - log: \"Row: ${body}\"";
            javaStep = ".unmarshal().flatpack(\"file:" + path + "\", " + isFlatpackFixed + ")\n" +
                       "            .split().simple(\"${body}\")\n" +
                       "                .log(\"Row: ${body}\")\n" +
                       "            .end()";
            xmlStep = "<unmarshal>\n" +
                      "            <flatpack id=\"flatpack\" definition=\"file:" + path + "\" fixed=\"" + isFlatpackFixed + "\"/>\n" +
                      "        </unmarshal>\n" +
                      "        <split>\n" +
                      "            <simple>${body}</simple>\n" +
                      "            <log message=\"Row: ${body}\"/>\n" +
                      "        </split>";
        } else if ("groovy".equals(type)) {
            yamlStep = "transform:\n            groovy: \"resource:file:" + path + "\"";
            javaStep = ".transform().groovy(\"resource:file:" + path + "\")";
            xmlStep = "<transform>\n            <groovy>resource:file:" + path + "</groovy>\n        </transform>";
        } else if ("joor".equals(type)) {
            yamlStep = "transform:\n            joor: \"resource:file:" + path + "\"";
            javaStep = ".transform().joor(\"resource:file:" + path + "\")";
            xmlStep = "<transform>\n            <joor>resource:file:" + path + "</joor>\n        </transform>";
        } else {
            yamlStep = "to:\n            uri: \"" + type + ":file:" + path + "\"";
            javaStep = ".to(\"" + type + ":file:" + path + "\")";
            xmlStep = "<to uri=\"" + type + ":file:" + path + "\"/>";
        }

        // Generate full runnable route DSLs
        String yamlDsl = "";
        String javaDsl = "";
        String xmlDsl = "";

        yamlDsl = "- route:\n" +
                         "    id: transform-test-route\n" +
                         "    from:\n" +
                         "      uri: \"timer:trigger?repeatCount=1&delay=0\"\n" +
                         "      steps:\n" +
                         "        - setBody:\n" +
                         "            constant: |\n" +
                         (sourceMsg.isEmpty() ? "              [source message]\n" : indentString(sourceMsg, 14) + "\n") +
                         "        - " + yamlStep + "\n" +
                         "        - log: \"Parsed Output: ${body}\"";

            javaDsl = "import org.apache.camel.builder.RouteBuilder;\n\n" +
                             "public class TransformRoute extends RouteBuilder {\n" +
                             "    @Override\n" +
                             "    public void configure() throws Exception {\n" +
                             "        from(\"timer:trigger?repeatCount=1&delay=0\")\n" +
                             "            .setBody().constant(\"\"\"\n" +
                             (sourceMsg.isEmpty() ? "                [source message]\n" : indentString(sourceMsg, 16) + "\n") +
                             "                \"\"\")\n" +
                             "            " + javaStep + "\n" +
                             "            .log(\"Parsed Output: ${body}\");\n" +
                             "    }\n" +
                             "}";

            String xmlSourceContent = sourceMsg;
            if (xmlSourceContent.contains("<") || xmlSourceContent.contains("&") || xmlSourceContent.contains(">")) {
                xmlSourceContent = "<![CDATA[\n" + xmlSourceContent + "\n]]>";
            }
            xmlDsl = "<routes xmlns=\"http://camel.apache.org/schema/spring\">\n" +
                            "    <route id=\"transform-test-route\">\n" +
                            "        <from uri=\"timer:trigger?repeatCount=1&delay=0\"/>\n" +
                            "        <setBody>\n" +
                            "            <constant>\n" +
                            (sourceMsg.isEmpty() ? "                [source message]\n" : indentString(xmlSourceContent, 16) + "\n") +
                            "            </constant>\n" +
                            "        </setBody>\n" +
                            "        " + xmlStep + "\n" +
                            "        <log message=\"Parsed Output: ${body}\"/>\n" +
                            "    </route>\n" +
                            "</routes>";

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Camel Route Snippet & Dependency");
        dialog.setHeaderText("Configuration for: " + logicFile.getName());
        
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setPrefWidth(600);

        Label lblDep = new Label("Required Dependency:");
        lblDep.setStyle("-fx-font-weight: bold;");

        TabPane depTabPane = new TabPane();
        depTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        java.util.function.Consumer<String> copier = txt -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(txt);
            clipboard.setContent(content);
            log("Copied snippet to clipboard.");
        };

        Tab jbangTab = new Tab("JBang");
        HBox jbangBox = new HBox(5);
        TextArea txtJbang = new TextArea(jbangDepsStr);
        txtJbang.setEditable(false);
        txtJbang.setPrefHeight(60);
        txtJbang.setStyle("-fx-font-family: monospace; -fx-control-inner-background: #252526; -fx-text-fill: #9cdcfe;");
        HBox.setHgrow(txtJbang, Priority.ALWAYS);
        Button btnCopyJbang = new Button("", new FontIcon("fas-copy"));
        btnCopyJbang.getStyleClass().addAll("small-action-btn", "btn-copy-text");
        btnCopyJbang.setOnAction(e -> copier.accept(txtJbang.getText()));
        jbangBox.getChildren().addAll(txtJbang, btnCopyJbang);
        jbangTab.setContent(jbangBox);

        Tab gradleTab = new Tab("Gradle");
        HBox gradleBox = new HBox(5);
        TextArea txtGradle = new TextArea(gradleDepsStr);
        txtGradle.setEditable(false);
        txtGradle.setPrefHeight(60);
        txtGradle.setStyle("-fx-font-family: monospace; -fx-control-inner-background: #252526; -fx-text-fill: #9cdcfe;");
        HBox.setHgrow(txtGradle, Priority.ALWAYS);
        Button btnCopyGradle = new Button("", new FontIcon("fas-copy"));
        btnCopyGradle.getStyleClass().addAll("small-action-btn", "btn-copy-text");
        btnCopyGradle.setOnAction(e -> copier.accept(txtGradle.getText()));
        gradleBox.getChildren().addAll(txtGradle, btnCopyGradle);
        gradleTab.setContent(gradleBox);

        Tab mavenTab = new Tab("Maven");
        HBox mavenBox = new HBox(5);
        TextArea txtMaven = new TextArea(mavenDepsStr);
        txtMaven.setEditable(false);
        txtMaven.setPrefHeight(90);
        txtMaven.setStyle("-fx-font-family: monospace; -fx-control-inner-background: #252526; -fx-text-fill: #9cdcfe;");
        HBox.setHgrow(txtMaven, Priority.ALWAYS);
        Button btnCopyMaven = new Button("", new FontIcon("fas-copy"));
        btnCopyMaven.getStyleClass().addAll("small-action-btn", "btn-copy-text");
        btnCopyMaven.setOnAction(e -> copier.accept(txtMaven.getText()));
        mavenBox.getChildren().addAll(txtMaven, btnCopyMaven);
        mavenTab.setContent(mavenBox);

        depTabPane.getTabs().addAll(jbangTab, gradleTab, mavenTab);

        Label lblDsl = new Label("Route DSL Usage (with DEPS):");
        lblDsl.setStyle("-fx-font-weight: bold;");

        // Prepend JBang dependency headers directly to the DSL tabs
        StringBuilder yamlHeader = new StringBuilder();
        StringBuilder javaHeader = new StringBuilder();
        StringBuilder xmlHeader = new StringBuilder();
        for (String dep : deps) {
            yamlHeader.append("#DEPS ").append(dep).append("\n");
            javaHeader.append("//DEPS ").append(dep).append("\n");
            xmlHeader.append("<!--DEPS ").append(dep).append(" -->\n");
        }
        String yamlWithDep = yamlHeader.toString() + "\n" + yamlDsl;
        String javaWithDep = javaHeader.toString() + "\n" + javaDsl;
        String xmlWithDep = xmlHeader.toString() + "\n" + xmlDsl;

        TabPane dslTabPane = new TabPane();
        dslTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab yamlTab = new Tab("YAML");
        HBox yamlBox = new HBox(5);
        TextArea txtYaml = new TextArea(yamlWithDep);
        txtYaml.setEditable(false);
        txtYaml.setPrefHeight(150);
        txtYaml.setStyle("-fx-font-family: monospace; -fx-control-inner-background: #252526; -fx-text-fill: #ce9178;");
        HBox.setHgrow(txtYaml, Priority.ALWAYS);
        Button btnCopyYaml = new Button("", new FontIcon("fas-copy"));
        btnCopyYaml.getStyleClass().addAll("small-action-btn", "btn-copy-text");
        btnCopyYaml.setOnAction(e -> copier.accept(txtYaml.getText()));
        yamlBox.getChildren().addAll(txtYaml, btnCopyYaml);
        yamlTab.setContent(yamlBox);

        Tab javaTab = new Tab("Java");
        HBox javaBox = new HBox(5);
        TextArea txtJava = new TextArea(javaWithDep);
        txtJava.setEditable(false);
        txtJava.setPrefHeight(150);
        txtJava.setStyle("-fx-font-family: monospace; -fx-control-inner-background: #252526; -fx-text-fill: #ce9178;");
        HBox.setHgrow(txtJava, Priority.ALWAYS);
        Button btnCopyJava = new Button("", new FontIcon("fas-copy"));
        btnCopyJava.getStyleClass().addAll("small-action-btn", "btn-copy-text");
        btnCopyJava.setOnAction(e -> copier.accept(txtJava.getText()));
        javaBox.getChildren().addAll(txtJava, btnCopyJava);
        javaTab.setContent(javaBox);

        Tab xmlTab = new Tab("XML");
        HBox xmlBox = new HBox(5);
        TextArea txtXml = new TextArea(xmlWithDep);
        txtXml.setEditable(false);
        txtXml.setPrefHeight(150);
        txtXml.setStyle("-fx-font-family: monospace; -fx-control-inner-background: #252526; -fx-text-fill: #ce9178;");
        HBox.setHgrow(txtXml, Priority.ALWAYS);
        Button btnCopyXml = new Button("", new FontIcon("fas-copy"));
        btnCopyXml.getStyleClass().addAll("small-action-btn", "btn-copy-text");
        btnCopyXml.setOnAction(e -> copier.accept(txtXml.getText()));
        xmlBox.getChildren().addAll(txtXml, btnCopyXml);
        xmlTab.setContent(xmlBox);

        dslTabPane.getTabs().addAll(yamlTab, javaTab, xmlTab);

        Button btnCopy = new Button("Copy All Info", new FontIcon("fas-copy"));
        btnCopy.getStyleClass().addAll("btn-copy-all");
        btnCopy.setOnAction(e -> {
            copier.accept("JBang Dependencies:\n" + txtJbang.getText() + "\n\n" +
                          "Gradle Dependencies:\n" + txtGradle.getText() + "\n\n" +
                          "Maven Dependencies:\n" + txtMaven.getText() + "\n\n" +
                          "Java DSL:\n" + txtJava.getText() + "\n\n" +
                          "YAML DSL:\n" + txtYaml.getText() + "\n\n" +
                          "XML DSL:\n" + txtXml.getText());
        });

        Button btnRunSnippet = new Button("Run Route", new FontIcon("fas-play"));
        btnRunSnippet.getStyleClass().addAll("editor-btn", "btn-run");

        Button btnStopSnippet = new Button("Stop Route", new FontIcon("fas-stop"));
        btnStopSnippet.getStyleClass().addAll("editor-btn", "btn-stop");
        btnStopSnippet.setDisable(true);

        if (snippetProcess != null && snippetProcess.isAlive()) {
            btnRunSnippet.setDisable(true);
            btnStopSnippet.setDisable(false);
        }

        btnRunSnippet.setOnAction(e -> {
            if (snippetProcess != null && snippetProcess.isAlive()) {
                try {
                    snippetProcess.destroy();
                    snippetProcess.descendants().forEach(ProcessHandle::destroyForcibly);
                } catch (Exception ignored) {}
                snippetProcess = null;
            }

            Tab selectedTab = dslTabPane.getSelectionModel().getSelectedItem();
            String tabText = selectedTab != null ? selectedTab.getText() : "YAML";

            String snippetContent = "";
            String fileName = "";
            if ("YAML".equals(tabText)) {
                snippetContent = yamlWithDep;
                fileName = "temp-transform-test-route.camel.yaml";
            } else if ("Java".equals(tabText)) {
                snippetContent = javaWithDep;
                fileName = "TransformRoute.java";
            } else if ("XML".equals(tabText)) {
                snippetContent = xmlWithDep;
                fileName = "temp-transform-test-route.camel.xml";
            }

            File runDir = currentFolder;
            if (runDir == null) {
                runDir = currentMappingsPath;
            }
            if (runDir == null || !runDir.exists()) {
                runDir = new File(System.getProperty("user.dir"));
            }

            File tempFile = new File(runDir, fileName);
            try {
                Files.writeString(tempFile.toPath(), snippetContent);
                tempFile.deleteOnExit();
            } catch (Exception ex) {
                log("Failed to write temporary route file: " + ex.getMessage());
                return;
            }

            String os = System.getProperty("os.name").toLowerCase();
            String jbangScript = os.contains("win") ? "jbang.cmd" : "jbang";
            File jbangExe = new File(System.getProperty("user.dir"), jbangScript);
            if (!jbangExe.exists()) {
                jbangExe = new File(new File(System.getProperty("user.dir"), "route-builder"), jbangScript);
            }
            String executablePath = jbangExe.exists() ? jbangExe.getAbsolutePath() : "jbang";

            List<String> command = new ArrayList<>();
            if (hasStdbuf()) {
                command.add("stdbuf");
                command.add("-oL");
                command.add("-eL");
            }
            command.add(executablePath);
            command.add("camel@apache/camel");
            command.add("run");
            command.add(tempFile.getAbsolutePath());
            command.add("--runtime=main");
            command.add("--dev");

            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.environment().put("TERM", "xterm-256color");
                pb.directory(runDir);
                snippetProcess = pb.start();

                btnRunSnippet.setDisable(true);
                btnStopSnippet.setDisable(false);

                log("╔══ Running Camel Route from Snippet ══╗");
                log("Command: " + String.join(" ", command));

                pipeSnippetStream(snippetProcess, snippetProcess.getInputStream(), btnRunSnippet, btnStopSnippet, tempFile);
                pipeSnippetStream(snippetProcess, snippetProcess.getErrorStream(), btnRunSnippet, btnStopSnippet, tempFile);

            } catch (Exception ex) {
                log("Failed to start route process: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        btnStopSnippet.setOnAction(e -> {
            btnRunSnippet.setDisable(false);
            btnStopSnippet.setDisable(true);
            log("Stopping route process...");
            if (snippetProcess != null && snippetProcess.isAlive()) {
                try {
                    snippetProcess.destroy();
                    snippetProcess.descendants().forEach(ProcessHandle::destroyForcibly);
                } catch (Exception ignored) {}
                snippetProcess = null;
            }
        });

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actionBox.getChildren().addAll(btnCopy, btnRunSnippet, btnStopSnippet);

        vbox.getChildren().addAll(lblDep, depTabPane, lblDsl, dslTabPane, actionBox);
        dialog.getDialogPane().setContent(vbox);
        RouteBuilderApp.themeDialog(dialog);
        dialog.showAndWait();
    }

    private boolean hasStdbuf() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("linux")) return false;
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"which", "stdbuf"});
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String stripAnsi(String text) {
        if (text == null) return "";
        return text.replaceAll("\\u001B\\[[;\\d]*[ -/]*[@-~]", "");
    }

    private void pipeSnippetStream(Process process, java.io.InputStream stream, Button btnRun, Button btnStop, File tempFile) {
        new Thread(() -> {
            byte[] buf = new byte[2048];
            int n;
            try {
                while ((n = stream.read(buf)) != -1) {
                    final String chunk = new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8);
                    String cleanChunk = stripAnsi(chunk);
                    Platform.runLater(() -> consoleArea.appendText(cleanChunk));
                }
            } catch (Exception ignored) {
            } finally {
                Platform.runLater(() -> {
                    if (process != null && !process.isAlive()) {
                        btnRun.setDisable(false);
                        btnStop.setDisable(true);
                        try {
                            int code = process.exitValue();
                            log("Process exited with code: " + code);
                        } catch (Exception ignored2) {}
                        if (tempFile.exists()) {
                            tempFile.delete();
                        }
                    }
                });
            }
        }, "snippet-stream-pipe").start();
    }

    private void log(String msg) { Platform.runLater(() -> { String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")); consoleArea.appendText("[" + time + "] " + msg + "\n"); }); }
}
