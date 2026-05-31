package com.routebuilder.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
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
    public static final java.util.List<TransformationStudioWindow> activeInstances = new java.util.ArrayList<>();

    private Stage stage;
    private final Preferences prefs;
    private File currentMappingsPath;

    private TreeView<File> mappingTree;
    private BorderPane mainContentArea;
    private Label lblStudioTitle;

    private Button btnRun, btnValidate, btnSave, btnBrowseXsd, btnConfig, btnSnippet, btnClose;

    private File currentFolder;
    private final java.util.Set<File> checkedFiles = new java.util.HashSet<>();

    public java.util.Set<File> getCheckedFiles() {
        return checkedFiles;
    }

    private JSONObject currentConfig;
    private String transformationType = "xslt";
    private boolean isNonXmlSource = false;
    private boolean isEnrichment = false;
    private boolean isMtToMx = false;

    private com.routebuilder.ui.components.MonacoEditorPane sourceRawEditor, sourceXmlEditor, logicEditor,
            logicSecondaryEditor, targetEditor;
    private File sourceRawFile, sourceXmlFile, logicFile, logicSecondaryFile;

    private com.routebuilder.ui.components.ConsolePane consolePane;
    private Process snippetProcess;
    private final List<Object> persistentBridges = new ArrayList<>();

    private boolean sourceRawInitialized, sourceXmlInitialized, logicInitialized, targetInitialized;

    public TransformationStudioWindow() {
        activeInstances.add(this);
        this.prefs = Preferences.userNodeForPackage(TransformationStudioWindow.class);
        File userDir = new File(System.getProperty("user.dir"));
        String defaultPath;
        if (new File(userDir, "mappings").exists()) {
            defaultPath = new File(userDir, "mappings").getAbsolutePath();
        } else if (new File(userDir, "test-mapping").exists()) {
            defaultPath = new File(userDir, "test-mapping").getAbsolutePath();
        } else if (new File(userDir, "transformation-samples").exists()) {
            defaultPath = new File(userDir, "transformation-samples").getAbsolutePath();
        } else {
            defaultPath = new File(userDir, "mappings").getAbsolutePath();
        }
        String savedPath = prefs.get("mappingsPath", defaultPath);
        this.currentMappingsPath = new File(savedPath);
        if (!this.currentMappingsPath.exists()) {
            this.currentMappingsPath.mkdirs();
        }
    }

    public void show() {
        if (stage == null) {
            stage = new Stage();
        }
        stage.setTitle("Data Transformation Studio");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", RouteBuilderApp.currentThemeClass);
        com.routebuilder.ui.components.ThemeManager.registerRoot(root);

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("editor-toolbar");
        btnRun = new Button("Transform", new FontIcon("fas-play"));
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

        Button btnSampleData = new Button("Create Sample Mappings", new FontIcon("fas-magic"));
        btnSampleData.getStyleClass().addAll("editor-btn", "btn-sample-data");
        btnSampleData.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Directory for Sample Mappings");
            chooser.setInitialDirectory(
                    currentMappingsPath.exists() ? currentMappingsPath : new File(System.getProperty("user.dir")));
            File selected = chooser.showDialog(stage);
            if (selected != null) {
                currentMappingsPath = selected;
                prefs.put("mappingsPath", selected.getAbsolutePath());
                generateSampleMappings(selected);
                refreshMappingTree();
            }
        });

        lblStudioTitle = new Label("Select a mapping to begin");
        lblStudioTitle.getStyleClass().add("studio-title-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnExport = new Button("Export", new FontIcon("fas-download"));
        btnExport.getStyleClass().addAll("editor-btn", "btn-export");
        btnExport.setTooltip(new Tooltip("Export Selected Transformations to Liquibase Changelog"));
        btnExport.setOnAction(e -> {
            if (checkedFiles.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Please select one or more transformations using the checkboxes in the Explorer.");
                RouteBuilderApp.themeDialog(alert);
                alert.showAndWait();
                return;
            }
            LiquibaseExportWindow.showForTransformations(currentMappingsPath, checkedFiles);
        });

        Button btnDeployRemote = new Button("Copy to Remote", new FontIcon("fas-share-square"));
        btnDeployRemote.getStyleClass().addAll("editor-btn", "btn-deploy");
        btnDeployRemote.setTooltip(new Tooltip("Copy Selected Transformations to Remote Container Path"));
        btnDeployRemote.setOnAction(e -> {
            if (checkedFiles.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Please select one or more transformations using the checkboxes in the Explorer.");
                RouteBuilderApp.themeDialog(alert);
                alert.showAndWait();
                return;
            }
            RemoteDeployWindow.showForTransformations(currentMappingsPath, checkedFiles);
        });

        Button btnHelp = new Button("Help Guide", new FontIcon("fas-question-circle"));
        btnHelp.getStyleClass().addAll("editor-btn", "btn-help");
        btnHelp.setOnAction(e -> new RouteBuilderHelpWindow("Advanced Tools", "Transformation").show());

        toolBar.getItems().addAll(btnValidate, btnBrowseXsd, btnSave, new Separator(), btnConfig, btnSampleData,
                btnDeployRemote, btnExport, btnRun, new Separator(), lblStudioTitle, spacer, btnHelp);
        root.setTop(toolBar);

        // --- Left Sidebar ---
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(50);
        sidebar.setPadding(new Insets(10));
        sidebar.setSpacing(5);
        sidebar.getStyleClass().add("studio-sidebar");

        HBox sidebarHeader = new HBox(5);
        sidebarHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblExplorer = new Label("MAPPING EXPLORER");
        lblExplorer.getStyleClass().add("studio-explorer-label");
        HBox.setHgrow(lblExplorer, Priority.ALWAYS);
        lblExplorer.setMaxWidth(Double.MAX_VALUE);

        Button btnExpandAll = new Button(null, new FontIcon("fas-expand-arrows-alt"));
        btnExpandAll.getStyleClass().add("small-action-btn");
        btnExpandAll.setTooltip(new Tooltip("Expand All"));
        btnExpandAll.setOnAction(e -> {
            TreeItem<?> treeRoot = mappingTree.getRoot();
            if (treeRoot != null) {
                treeRoot.setExpanded(true); // keep virtual root open
                for (TreeItem<?> child : treeRoot.getChildren())
                    toggleAllNodes(child, true);
            }
        });

        Button btnCollapseAll = new Button(null, new FontIcon("fas-compress-arrows-alt"));
        btnCollapseAll.getStyleClass().add("small-action-btn");
        btnCollapseAll.setTooltip(new Tooltip("Collapse All"));
        btnCollapseAll.setOnAction(e -> {
            TreeItem<?> treeRoot = mappingTree.getRoot();
            if (treeRoot != null) {
                // Never collapse the virtual (hidden) root — only its children
                for (TreeItem<?> child : treeRoot.getChildren())
                    toggleAllNodes(child, false);
            }
        });

        sidebarHeader.getChildren().addAll(lblExplorer, btnExpandAll, btnCollapseAll);

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

        sidebar.getChildren().addAll(sidebarHeader, mappingTree);
        VBox.setVgrow(mappingTree, Priority.ALWAYS);

        // --- Main Content Area ---
        mainContentArea = new BorderPane();
        mainContentArea.getStyleClass().addAll("studio-content", "app-root");

        Label lblPlaceholder = new Label("Select a transformation folder from the explorer");
        lblPlaceholder.getStyleClass().add("studio-placeholder-label");
        mainContentArea.setCenter(lblPlaceholder);

        consolePane = new com.routebuilder.ui.components.ConsolePane();
        consolePane.setPrefHeight(150);
        com.routebuilder.ui.components.ThemeManager.registerRoot(consolePane);

        SplitPane verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().addAll(mainContentArea, consolePane);
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
            activeInstances.remove(this);
            RouteBuilderApp.themedRoots.remove(root);
            if (snippetProcess != null && snippetProcess.isAlive()) {
                try {
                    snippetProcess.destroy();
                    snippetProcess.descendants().forEach(ProcessHandle::destroyForcibly);
                } catch (Exception ignored) {
                }
            }
        });

        stage.setMaximized(true);
        stage.show();

        refreshMappingTree();
    }

    private void chooseMappingsPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Mappings Base Directory");
        chooser.setInitialDirectory(
                currentMappingsPath.exists() ? currentMappingsPath : new File(System.getProperty("user.dir")));
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
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("mapping-tree-item", "folder-tree-item");
                } else {
                    setText(item.getName());
                    setStyle(null);
                    File configFile = new File(item, "transformation.json");

                    CheckBox cb = new CheckBox();
                    cb.setAllowIndeterminate(true);

                    CheckState state = getFolderCheckState(item);
                    if (state == CheckState.CHECKED) {
                        cb.setSelected(true);
                        cb.setIndeterminate(false);
                    } else if (state == CheckState.UNCHECKED) {
                        cb.setSelected(false);
                        cb.setIndeterminate(false);
                    } else {
                        cb.setSelected(false);
                        cb.setIndeterminate(true);
                    }

                    cb.setOnAction(e -> {
                        CheckState currentState = getFolderCheckState(item);
                        boolean targetChecked = (currentState != CheckState.CHECKED);
                        setCheckedRecursive(item, targetChecked);
                        mappingTree.refresh();
                    });

                    FontIcon icon;
                    if (configFile.exists()) {
                        icon = new FontIcon("fas-exchange-alt");
                        getStyleClass().add("mapping-tree-item");
                        getStyleClass().remove("folder-tree-item");
                    } else {
                        icon = RouteBuilderApp.getFileIcon(item);
                        getStyleClass().add("folder-tree-item");
                        getStyleClass().remove("mapping-tree-item");
                    }

                    HBox box = new HBox(5, cb, icon);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
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
                    if (!configFile.exists())
                        buildTree(f, item);
                }
            }
        }
    }

    private void loadMapping(File folder) {
        File configFile = new File(folder, "transformation.json");
        if (!configFile.exists())
            return;
        this.currentFolder = folder;
        try {
            String content = Files.readString(configFile.toPath());
            this.currentConfig = new JSONObject(content);
            String type = currentConfig.optString("type", "xslt");
            this.transformationType = type;
            this.isEnrichment = "enrichment".equalsIgnoreCase(type);

            JSONObject source = currentConfig.optJSONObject("source");
            String sourceType = source != null ? source.optString("type", "xml") : "xml";

            this.isMtToMx = "mt".equalsIgnoreCase(type) || "mt-to-mx".equalsIgnoreCase(type)
                    || ("mt".equalsIgnoreCase(sourceType) && "xslt".equalsIgnoreCase(type));
            this.isNonXmlSource = !"xml".equalsIgnoreCase(sourceType) && !isEnrichment && !isMtToMx;

            lblStudioTitle.setText(currentConfig.optString("name", folder.getName()));
            log("Loading mapping: " + lblStudioTitle.getText());
            btnRun.setDisable(false);
            btnSave.setDisable(false);
            btnBrowseXsd.setDisable(false);
            updateValidationState();
            buildTransformationUI();
        } catch (Exception e) {
            log("Error loading config: " + e.getMessage());
        }
    }

    private void updateValidationState() {
        JSONObject target = currentConfig.optJSONObject("target");
        if (target != null && "xml".equalsIgnoreCase(target.optString("type"))) {
            String xsd = target.optString("xsd");
            if (xsd != null && !xsd.isEmpty() && new File(currentFolder, xsd).exists()) {
                btnValidate.setDisable(false);
            } else
                btnValidate.setDisable(true);
        } else
            btnValidate.setDisable(true);
    }

    private void browseXsd() {
        if (currentFolder == null)
            return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select XSD Schema");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XSD Files", "*.xsd"));
        File selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            try {
                File targetFile = new File(currentFolder, selected.getName());
                Files.copy(selected.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                JSONObject target = currentConfig.optJSONObject("target");
                if (target == null) {
                    target = new JSONObject();
                    target.put("type", "xml");
                    currentConfig.put("target", target);
                }
                target.put("xsd", selected.getName());
                File configFile = new File(currentFolder, "transformation.json");
                Files.writeString(configFile.toPath(), currentConfig.toString(2));
                log("XSD imported: " + selected.getName());
                updateValidationState();
            } catch (Exception e) {
                log("Error importing XSD: " + e.getMessage());
            }
        }
    }

    private void buildTransformationUI() {
        Platform.runLater(() -> {
            mainContentArea.setCenter(null);
            SplitPane horizontalSplit = new SplitPane();
            horizontalSplit.setOrientation(Orientation.HORIZONTAL);

            sourceRawEditor = new com.routebuilder.ui.components.MonacoEditorPane();
            sourceXmlEditor = new com.routebuilder.ui.components.MonacoEditorPane();

            VBox sourcePanel = new VBox();
            if (isEnrichment || isMtToMx) {
                SplitPane sourceSplit = new SplitPane();
                sourceSplit.setOrientation(Orientation.VERTICAL);
                VBox topBox = new VBox(createHeader(isEnrichment ? "Original Source" : "Raw Source", sourceRawEditor,
                        true, f -> sourceRawFile = f), sourceRawEditor);
                VBox.setVgrow(sourceRawEditor, Priority.ALWAYS);
                VBox bottomBox = new VBox(createHeader(isEnrichment ? "Truncated Source" : "Converted XML",
                        sourceXmlEditor, isEnrichment, f -> sourceXmlFile = f), sourceXmlEditor);
                VBox.setVgrow(sourceXmlEditor, Priority.ALWAYS);
                sourceSplit.getItems().addAll(topBox, bottomBox);
                sourcePanel.getChildren().add(sourceSplit);
                VBox.setVgrow(sourceSplit, Priority.ALWAYS);
            } else {
                sourcePanel.getChildren().addAll(createHeader("SOURCE", sourceXmlEditor, true, f -> sourceXmlFile = f),
                        sourceXmlEditor);
                VBox.setVgrow(sourceXmlEditor, Priority.ALWAYS);
            }

            VBox logicPanel = new VBox();
            logicEditor = new com.routebuilder.ui.components.MonacoEditorPane();

            org.json.JSONArray logicArr = currentConfig.optJSONArray("logic");
            boolean isSmooks = "smooks".equalsIgnoreCase(transformationType);
            if (logicArr != null && logicArr.length() > 1 && !isSmooks) {
                logicSecondaryEditor = new com.routebuilder.ui.components.MonacoEditorPane();
                SplitPane logicSplit = new SplitPane();
                logicSplit.setOrientation(Orientation.VERTICAL);
                VBox topBox = new VBox(createHeader("CONFIG (" + transformationType.toUpperCase() + ")", logicEditor,
                        true, f -> logicFile = f), logicEditor);
                VBox.setVgrow(logicEditor, Priority.ALWAYS);
                VBox bottomBox = new VBox(
                        createHeader("SCHEMA / MODEL", logicSecondaryEditor, true, f -> logicSecondaryFile = f),
                        logicSecondaryEditor);
                VBox.setVgrow(logicSecondaryEditor, Priority.ALWAYS);
                logicSplit.getItems().addAll(topBox, bottomBox);
                logicPanel.getChildren().add(logicSplit);
                VBox.setVgrow(logicSplit, Priority.ALWAYS);
            } else {
                logicSecondaryEditor = null;
                logicPanel.getChildren().addAll(createHeader("LOGIC (" + transformationType.toUpperCase() + ")",
                        logicEditor, true, f -> logicFile = f), logicEditor);
                VBox.setVgrow(logicEditor, Priority.ALWAYS);
            }

            VBox targetPanel = new VBox();
            targetEditor = new com.routebuilder.ui.components.MonacoEditorPane();
            targetPanel.getChildren().addAll(createHeader("TARGET", targetEditor, false, null), targetEditor);
            VBox.setVgrow(targetEditor, Priority.ALWAYS);

            horizontalSplit.getItems().addAll(sourcePanel, logicPanel, targetPanel);
            horizontalSplit.setDividerPositions(0.33, 0.66);
            mainContentArea.setCenter(horizontalSplit);
            initEditors();
        });
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

    private javafx.scene.Node createHeader(String title, com.routebuilder.ui.components.MonacoEditorPane editor,
            boolean isFileBased, java.util.function.Consumer<File> onFileRefUpdated) {
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
            btnOpen.setOnAction(e -> openFileIntoEditor(editor, onFileRefUpdated));
            Button btnSave = createSmallButton("fas-save", "Save File");
            btnSave.getStyleClass().add("btn-save-file");
            btnSave.setOnAction(e -> saveEditorToFile(editor, false, onFileRefUpdated));
            header.getChildren().addAll(btnOpen, btnSave);
            if (title.contains("LOGIC") || title.contains("CONFIG")) {
                Button btnVisualMap = createSmallButton("fas-map-marked-alt", "Sovereign Mapping Architect");
                btnVisualMap.getStyleClass().add("btn-map-cyan");
                boolean isMapApplicable = transformationType != null
                        && (transformationType.equalsIgnoreCase("xslt") || transformationType.equalsIgnoreCase("jslt")
                                || transformationType.toLowerCase().contains("enrichment")
                                || transformationType.toLowerCase().contains("mt"));
                btnVisualMap.setDisable(!isMapApplicable);
                if (isMapApplicable) {
                    btnVisualMap.setStyle(
                            "-fx-text-fill: #00e5ff; -fx-border-color: #00e5ff; -fx-border-radius: 4; -fx-border-width: 1px; -fx-padding: 1 5;");
                } else {
                    btnVisualMap.setStyle(
                            "-fx-text-fill: #555; -fx-border-color: #444; -fx-border-radius: 4; -fx-border-width: 1px; -fx-padding: 1 5;");
                }
                btnVisualMap.setOnAction(e -> showMappingArchitect());
                header.getChildren().add(btnVisualMap);

                Button btnSnippet = createSmallButton("fas-code", "Camel Route Snippet Info");
                btnSnippet.getStyleClass().add("btn-snippet-info");
                btnSnippet.setOnAction(e -> showSnippetWindow());
                header.getChildren().add(btnSnippet);
            }
        }
        Button btnSaveAs = createSmallButton("fas-file-download", "Save As...");
        btnSaveAs.getStyleClass().add("btn-save-as-file");
        btnSaveAs.setOnAction(e -> saveEditorToFile(editor, true, onFileRefUpdated));
        Button btnCopy = createSmallButton("fas-copy", "Copy All");
        btnCopy.getStyleClass().add("btn-copy-text");
        btnCopy.setOnAction(e -> {
            String val = editor.getText();
            if (val != null) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(val);
                clipboard.setContent(content);
                log("Copied " + val.length() + " chars to clipboard.");
            }
        });
        header.getChildren().addAll(btnSaveAs, btnCopy);
        return header;
    }

    private void showMappingArchitect() {
        if (currentFolder == null)
            return;
        try {
            String logicContent = logicEditor.getText();
            String sourceContent = sourceXmlEditor != null ? sourceXmlEditor.getText() : "";

            MappingArchitectWindow architect = new MappingArchitectWindow();
            architect.show("Visual Mapper - " + transformationType.toUpperCase(), logicContent, sourceContent);
        } catch (Exception e) {
            log("Error launching visual mapper: " + e.getMessage());
        }
    }

    private Button createSmallButton(String icon, String tooltip) {
        Button b = new Button();
        FontIcon fi = new FontIcon(icon);
        b.setGraphic(fi);
        b.setTooltip(new Tooltip(tooltip));
        b.getStyleClass().add("small-action-btn");
        return b;
    }

    private void openFileIntoEditor(com.routebuilder.ui.components.MonacoEditorPane editor,
            java.util.function.Consumer<File> onFileRefUpdated) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(currentFolder);
        File selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            try {
                editor.setText(Files.readString(selected.toPath()));
                if (onFileRefUpdated != null)
                    onFileRefUpdated.accept(selected);
                log("Opened: " + selected.getName());
            } catch (Exception e) {
                log("Open Error: " + e.getMessage());
            }
        }
    }

    private void saveEditorToFile(com.routebuilder.ui.components.MonacoEditorPane editor, boolean isSaveAs,
            java.util.function.Consumer<File> onFileRefUpdated) {
        String content = editor.getText();
        File target = null;
        if (!isSaveAs) {
            if (editor == sourceRawEditor)
                target = sourceRawFile;
            else if (editor == sourceXmlEditor)
                target = sourceXmlFile;
            else if (editor == logicEditor)
                target = logicFile;
            else if (editor == logicSecondaryEditor)
                target = logicSecondaryFile;
        }
        if (target == null) {
            FileChooser chooser = new FileChooser();
            chooser.setInitialDirectory(currentFolder);
            target = chooser.showSaveDialog(stage);
        }
        if (target != null) {
            try {
                Files.writeString(target.toPath(), content);
                if (onFileRefUpdated != null)
                    onFileRefUpdated.accept(target);
                log("Saved: " + target.getName());
            } catch (Exception e) {
                log("Save Error: " + e.getMessage());
            }
        }
    }

    private void initEditors() {
        if (isMtToMx) {
            sourceRawEditor.setLanguage("text");
        }

        String sourceLang = "xml";
        JSONObject srcCfg = currentConfig.optJSONObject("source");
        if (srcCfg != null) {
            String type = srcCfg.optString("type", "xml");
            if ("json".equalsIgnoreCase(type))
                sourceLang = "json";
            else if ("text".equalsIgnoreCase(type) || "csv".equalsIgnoreCase(type) || "edi".equalsIgnoreCase(type))
                sourceLang = "text";
        }
        sourceXmlEditor.setLanguage(sourceLang);

        if (isEnrichment) {
            sourceRawEditor.setLanguage(sourceLang);
        }

        String logicLang = "jslt".equals(transformationType) ? "json"
                : ("groovy".equals(transformationType) || "joor".equals(transformationType) ? "java" : "xml");
        logicEditor.setLanguage(logicLang);

        if (logicSecondaryEditor != null) {
            logicSecondaryEditor.setLanguage("xml");
        }

        String targetLang = "xml";
        JSONObject targetCfg = currentConfig.optJSONObject("target");
        if (targetCfg != null) {
            String type = targetCfg.optString("type", "xml");
            if ("mt".equalsIgnoreCase(type) || "swift".equalsIgnoreCase(type))
                targetLang = "text";
            else if ("json".equalsIgnoreCase(type))
                targetLang = "json";
        }
        targetEditor.setLanguage(targetLang);

        // MonacoEditorPane handles queuing setText if not initialized yet
        loadSourceData();
        loadLogicData();
    }

    private void loadSourceData() {
        try {
            if (isEnrichment) {
                org.json.JSONArray sources = currentConfig.optJSONArray("sources");
                if (sources != null && sources.length() >= 2) {
                    JSONObject s1 = sources.getJSONObject(0);
                    JSONObject s2 = sources.getJSONObject(1);
                    sourceRawFile = new File(currentFolder, s1.optString("file"));
                    sourceXmlFile = new File(currentFolder, s2.optString("file"));
                    if (sourceRawFile.exists())
                        sourceRawEditor.setText(Files.readString(sourceRawFile.toPath()));
                    if (sourceXmlFile.exists())
                        sourceXmlEditor.setText(Files.readString(sourceXmlFile.toPath()));
                }
                return;
            }
            JSONObject srcCfg = currentConfig.optJSONObject("source");
            if (srcCfg != null) {
                File f = new File(currentFolder, srcCfg.optString("file"));
                if (f.exists()) {
                    String content = Files.readString(f.toPath());
                    if (isMtToMx) {
                        sourceRawFile = f;
                        sourceRawEditor.setText(content);
                        unmarshalToXml(content);
                    } else {
                        sourceXmlFile = f;
                        sourceXmlEditor.setText(content);
                    }
                }
            }
        } catch (Exception e) {
            log("Error loading source: " + e.getMessage());
        }
    }

    private void loadLogicData() {
        try {
            org.json.JSONArray logicArr = currentConfig.optJSONArray("logic");
            if (logicArr != null && logicArr.length() > 0) {
                JSONObject primary = logicArr.getJSONObject(0);
                logicFile = new File(currentFolder, primary.optString("file"));
                if (logicFile.exists())
                    logicEditor.setText(Files.readString(logicFile.toPath()));

                if (logicArr.length() > 1 && logicSecondaryEditor != null) {
                    JSONObject secondary = logicArr.getJSONObject(1);
                    logicSecondaryFile = new File(currentFolder, secondary.optString("file"));
                    if (logicSecondaryFile.exists())
                        logicSecondaryEditor.setText(Files.readString(logicSecondaryFile.toPath()));
                }
                return;
            }

            JSONObject logicCfg = currentConfig.optJSONObject("logic");
            if (logicCfg != null) {
                logicFile = new File(currentFolder, logicCfg.optString("file"));
                if (logicFile.exists())
                    logicEditor.setText(Files.readString(logicFile.toPath()));
            }
        } catch (Exception e) {
            log("Error loading logic: " + e.getMessage());
        }
    }

    private void unmarshalToXml(String rawContent) {
        CompletableFuture.runAsync(() -> {
            try {
                String xml = TransformationBackend.unmarshal(rawContent, currentConfig.optJSONObject("source"));
                Platform.runLater(() -> sourceXmlEditor.setText(xml));
            } catch (Exception e) {
                Platform.runLater(() -> sourceXmlEditor.setText("Error: " + e.getMessage()));
            }
        });
    }

    private void runTransformation() {
        if (currentFolder == null)
            return;
        String sourceText = sourceXmlEditor != null ? sourceXmlEditor.getText() : "";
        String logic = logicEditor != null ? logicEditor.getText() : "";

        // Save secondary logic file if present (e.g. DFDL schema)
        if (logicSecondaryEditor != null && logicSecondaryFile != null) {
            try {
                Files.writeString(logicSecondaryFile.toPath(), logicSecondaryEditor.getText());
            } catch (Exception ignored) {
            }
        }

        String rawSource = isEnrichment ? (sourceRawEditor != null ? sourceRawEditor.getText() : "") : "";

        String execType = currentConfig.optString("type", "xslt");
        if (execType.contains("-to-"))
            execType = "xslt";
        final String finalExecType = execType;
        log("Running " + finalExecType.toUpperCase() + "...");

        CompletableFuture.runAsync(() -> {
            try {
                String result = isEnrichment ? TransformationBackend.transformEnrichment(rawSource, sourceText, logic)
                        : TransformationBackend.transform(sourceText, logic, finalExecType, currentFolder);
                Platform.runLater(() -> {
                    targetEditor.setText(result);
                    log("Transformation Success.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    log("Transformation Failed: " + e.getMessage());
                    targetEditor.setText("Error: " + e.getMessage());
                });
            }
        });
    }

    private void runValidation() {
        if (currentFolder == null)
            return;
        String xml = targetEditor != null ? targetEditor.getText() : "";
        JSONObject targetCfg = currentConfig.optJSONObject("target");
        String xsdName = targetCfg != null ? targetCfg.optString("xsd") : null;
        if (xsdName == null) {
            log("No XSD defined.");
            return;
        }
        File xsd = new File(currentFolder, xsdName);
        if (!xsd.exists()) {
            log("XSD not found: " + xsdName);
            return;
        }
        log("Validating against " + xsdName + "...");
        CompletableFuture.runAsync(() -> {
            String err = TransformationBackend.validateXml(xml, xsd);
            Platform.runLater(() -> log(err == null ? "VALIDATION SUCCESS." : "VALIDATION FAILED:\n" + err));
        });
    }

    private void saveGlobalConfig() {
        if (currentFolder == null)
            return;
        try {
            File configFile = new File(currentFolder, "transformation.json");
            Files.writeString(configFile.toPath(), currentConfig.toString(2));
            log("Saved global config.");
        } catch (Exception e) {
            log("Save Config Error: " + e.getMessage());
        }
    }

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
        alert.setContentText(
                "Are you sure you want to delete this entity and all its contents?\n\n" + file.getAbsolutePath());

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
            if (p.equals(parent))
                return true;
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

        boolean[] userModifiedFolder = { false };
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
                "Java Joor Mapper");
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
            String sourceFile = "source.xml", logicFile = "transform.xslt", sourceType = "xml", logicType = "xslt",
                    targetType = "xml";
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
                        "      <dfdl:format alignment=\"1\" alignmentUnits=\"bytes\" encoding=\"UTF-8\" byteOrder=\"bigEndian\" ignoreCase=\"yes\" utf16Width=\"fixed\" textNumberRep=\"standard\" lengthKind=\"delimited\" initiator=\"\" terminator=\"\" separator=\"\" separatorPosition=\"infix\" occursCountKind=\"implicit\" representation=\"text\" truncateSpecifiedLengthString=\"no\" textBidi=\"no\" floating=\"no\" sequenceKind=\"ordered\" escapeSchemeRef=\"\" leadingSkip=\"0\" trailingSkip=\"0\" encodingErrorPolicy=\"replace\" nilValueDelimiterPolicy=\"none\" emptyValueDelimiterPolicy=\"none\" documentFinalTerminatorCanBeMissing=\"yes\" textOutputMinLength=\"0\" textPadKind=\"none\" textTrimKind=\"none\" initiatedContent=\"no\" outputNewLine=\"%LF;\" fillByte=\"%#r20;\" separatorSuppressionPolicy=\"anyEmpty\" />\n"
                        +
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
        if (input == null || input.isEmpty())
            return "";
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

        String path = logicFile.getAbsolutePath().replace("\\", "/");
        String fileUri = toFileUriString(logicFile);
        String groupId = "org.apache.camel";
        String artifactId = "camel-" + type;
        String version = RouteBuilderApp.getCamelVersion();

        boolean isFullClassVal = false;
        String fullClassNameVal = "";
        String classSourceVal = "";
        if ("joor".equals(type) && logicFile != null && logicFile.exists()) {
            try {
                String content = Files.readString(logicFile.toPath());
                if (content.contains("class ") || content.contains("interface ")) {
                    isFullClassVal = true;
                    classSourceVal = content;
                    String pkg = "com.routebuilder.dynamic";
                    java.util.regex.Matcher pkgMatcher = java.util.regex.Pattern
                            .compile("package\\s+([a-zA-Z0-9_\\.]+)\\s*;").matcher(content);
                    if (pkgMatcher.find()) {
                        pkg = pkgMatcher.group(1);
                    }
                    String className = "Mapper";
                    java.util.regex.Matcher classMatcher = java.util.regex.Pattern
                            .compile("(?:class|interface|enum)\\s+(\\w+)").matcher(content);
                    if (classMatcher.find()) {
                        className = classMatcher.group(1);
                    }
                    fullClassNameVal = pkg + "." + className;
                }
            } catch (Exception ignored) {
            }
        }
        final boolean isFullClass = isFullClassVal;
        final String fullClassName = fullClassNameVal;
        final String classSource = classSourceVal;

        // Retrieve source content from editor or file
        String sourceMsg = "";
        String originalMsg = "";
        String truncatedMsg = "";
        if (isEnrichment) {
            originalMsg = sourceRawEditor != null ? sourceRawEditor.getText() : "";
            truncatedMsg = sourceXmlEditor != null ? sourceXmlEditor.getText() : "";
            if (originalMsg == null || originalMsg.trim().isEmpty()) {
                if (sourceRawFile != null && sourceRawFile.exists()) {
                    try {
                        originalMsg = Files.readString(sourceRawFile.toPath());
                    } catch (Exception ignored) {
                    }
                }
            }
            if (truncatedMsg == null || truncatedMsg.trim().isEmpty()) {
                if (sourceXmlFile != null && sourceXmlFile.exists()) {
                    try {
                        truncatedMsg = Files.readString(sourceXmlFile.toPath());
                    } catch (Exception ignored) {
                    }
                }
            }
            if (originalMsg == null)
                originalMsg = "";
            if (truncatedMsg == null)
                truncatedMsg = "";
            sourceMsg = TransformationBackend.combineEnrichmentXml(originalMsg, truncatedMsg);
        } else {
            if (isMtToMx) {
                sourceMsg = sourceRawEditor != null ? sourceRawEditor.getText() : "";
            } else {
                sourceMsg = sourceXmlEditor != null ? sourceXmlEditor.getText() : "";
            }
            if (sourceMsg == null || sourceMsg.trim().isEmpty()) {
                if (sourceRawFile != null && sourceRawFile.exists()) {
                    try {
                        sourceMsg = Files.readString(sourceRawFile.toPath());
                    } catch (Exception ignored) {
                    }
                }
                if ((sourceMsg == null || sourceMsg.trim().isEmpty()) && sourceXmlFile != null
                        && sourceXmlFile.exists()) {
                    try {
                        sourceMsg = Files.readString(sourceXmlFile.toPath());
                    } catch (Exception ignored) {
                    }
                }
            }
            if (sourceMsg == null)
                sourceMsg = "";
        }

        // Determine all necessary dependencies (cartridges + Camel components)
        java.util.List<String> deps = new java.util.ArrayList<>();
        if (isMtSource) {
            deps.add("org.apache.camel:camel-swift:4.18.2");
            deps.add("org.apache.camel:camel-xslt:" + version);
        } else if ("smooks".equals(type)) {
            deps.add("org.apache.camel:camel-smooks:" + version);
            try {
                String xmlContent = Files.readString(logicFile.toPath());
                if (xmlContent.contains("csv") || xmlContent.contains("<csv:") || xmlContent.contains("xmlns:csv")) {
                    deps.add("org.smooks.cartridges:smooks-csv-cartridge:2.0.3");
                } else if (xmlContent.contains("fixed-length") || xmlContent.contains("<fl:")
                        || xmlContent.contains("fixed-length-1.4.xsd")) {
                    deps.add("org.smooks.cartridges:smooks-fixed-length-cartridge:2.0.3");
                } else if (xmlContent.contains("json") || xmlContent.contains("<json:")
                        || xmlContent.contains("xmlns:json")) {
                    deps.add("org.smooks.cartridges:smooks-json-cartridge:2.0.3");
                } else if (xmlContent.contains("yaml") || xmlContent.contains("<yaml:")
                        || xmlContent.contains("xmlns:yaml")) {
                    deps.add("org.smooks.cartridges:smooks-yaml-cartridge:2.0.3");
                } else if (xmlContent.contains("edi") || xmlContent.contains("<edi:")
                        || xmlContent.contains("xmlns:edi")) {
                    deps.add("org.smooks.cartridges:smooks-edifact-cartridge:2.0.3");
                }
                if (xmlContent.contains("javabean") || xmlContent.contains("xmlns:jb")
                        || xmlContent.contains("/javabean-")) {
                    deps.add("org.smooks.cartridges:smooks-javabean-cartridge:2.0.3");
                }
            } catch (Exception ignored) {
            }
        } else if ("groovy".equals(type)) {
            deps.add("org.apache.camel:camel-groovy:" + version);
            if (version.startsWith("4.20") || version.startsWith("4.21") || version.startsWith("4.22")
                    || version.compareTo("4.20.0") >= 0) {
                deps.add("org.apache.groovy:groovy-xml:5.0.5");
                deps.add("org.apache.groovy:groovy-json:5.0.5");
            } else {
                deps.add("org.apache.groovy:groovy-xml:4.0.30");
                deps.add("org.apache.groovy:groovy-json:4.0.30");
            }
        } else if ("joor".equals(type)) {
            deps.add("org.apache.camel:camel-joor:" + version);
            deps.add("org.json:json:20231013");
        } else if ("mt".equalsIgnoreCase(type)) {
            deps.add("org.apache.camel:camel-swift:4.18.2");
            deps.add("org.apache.camel:camel-xslt:" + version);
        } else if ("xslt".equalsIgnoreCase(type) || "mt-to-mx".equalsIgnoreCase(type)
                || "mx-to-mt".equalsIgnoreCase(type)) {
            deps.add("org.apache.camel:camel-xslt:" + version);
        } else {
            deps.add(groupId + ":" + artifactId + ":" + version);
        }

        if ("freemarker".equals(type) && (sourceMsg.trim().startsWith("{") || sourceMsg.trim().startsWith("["))) {
            deps.add("org.apache.camel:camel-jackson:" + version);
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
            } catch (Exception ignored) {
            }
        }

        if ("xslt".equalsIgnoreCase(type) || isMtSource || "mt-to-mx".equalsIgnoreCase(type)
                || "mx-to-mt".equalsIgnoreCase(type)) {
            if (isMtSource || "mt-to-mx".equalsIgnoreCase(type)) {
                yamlStep = "unmarshal:\n            swiftMt: {}\n        - setBody:\n            simple: \"${body.xml}\"\n        - to:\n            uri: \"xslt-saxon:"
                        + fileUri + "\"";
                javaStep = ".unmarshal().swiftMt()\n            .setBody().simple(\"${body.xml}\")\n            .to(\"xslt-saxon:"
                        + fileUri + "\")";
                xmlStep = "<unmarshal>\n            <swiftMt/>\n        </unmarshal>\n        <setBody>\n            <simple>${body.xml}</simple>\n        </setBody>\n        <to uri=\"xslt-saxon:"
                        + fileUri + "\"/>";
            } else if ("mx-to-mt".equalsIgnoreCase(type)) {
                yamlStep = "to:\n            uri: \"xslt-saxon:" + fileUri
                        + "\"\n        - marshal:\n            swiftMt: {}";
                javaStep = ".to(\"xslt-saxon:" + fileUri + "\")\n            .marshal().swiftMt()";
                xmlStep = "<to uri=\"xslt-saxon:" + fileUri
                        + "\"/>\n        <marshal>\n            <swiftMt/>\n        </marshal>";
            } else {
                yamlStep = "to:\n            uri: \"xslt-saxon:" + fileUri + "\"";
                javaStep = ".to(\"xslt-saxon:" + fileUri + "\")";
                xmlStep = "<to uri=\"xslt-saxon:" + fileUri + "\"/>";
            }
        } else if ("jslt".equals(type)) {
            yamlStep = "to:\n            uri: \"jslt:" + fileUri + "\"";
            javaStep = ".to(\"jslt:" + fileUri + "\")";
            xmlStep = "<to uri=\"jslt:" + fileUri + "\"/>";
        } else if ("smooks".equals(type)) {
            yamlStep = "unmarshal:\n            smooks:\n              smooksConfig: \"" + fileUri + "\"";
            javaStep = ".unmarshal().smooks(\"" + fileUri + "\")";
            xmlStep = "<unmarshal>\n            <smooks smooksConfig=\"" + fileUri + "\"/>\n        </unmarshal>";
        } else if ("flatpack".equals(type)) {
            yamlStep = "unmarshal:\n" +
                    "            flatpack:\n" +
                    "              fixed: " + isFlatpackFixed + "\n" +
                    "              definition: \"" + fileUri + "\"\n" +
                    "        - split:\n" +
                    "            simple: \"${body}\"\n" +
                    "            steps:\n" +
                    "              - log: \"Row: ${body}\"";
            javaStep = ".unmarshal().flatpack(\"" + fileUri + "\", " + isFlatpackFixed + ")\n" +
                    "            .split().simple(\"${body}\")\n" +
                    "                .log(\"Row: ${body}\")\n" +
                    "            .end()";
            xmlStep = "<unmarshal>\n" +
                    "            <flatpack id=\"flatpack\" definition=\"" + fileUri + "\" fixed=\"" + isFlatpackFixed
                    + "\"/>\n" +
                    "        </unmarshal>\n" +
                    "        <split>\n" +
                    "            <simple>${body}</simple>\n" +
                    "            <log message=\"Row: ${body}\"/>\n" +
                    "        </split>";
        } else if ("groovy".equals(type)) {
            yamlStep = "transform:\n            groovy: \"resource:" + fileUri + "\"";
            javaStep = ".transform().groovy(\"resource:" + fileUri + "\")";
            xmlStep = "<transform>\n            <groovy>resource:" + fileUri + "</groovy>\n        </transform>";
        } else if ("joor".equals(type)) {
            if (isFullClass) {
                String escapedSource = escapeJavaString(classSource);
                String joorCode = "Class<?> clazz;\n" +
                        "try {\n" +
                        "    clazz = Class.forName(\"" + fullClassName + "\");\n" +
                        "} catch (Exception e) {\n" +
                        "    clazz = org.joor.Reflect.compile(\n" +
                        "        \"" + fullClassName + "\",\n" +
                        "        \"" + escapedSource + "\"\n" +
                        "    ).type();\n" +
                        "}\n" +
                        "return org.joor.Reflect.onClass(clazz).call(\"map\", body).get();";

                yamlStep = "transform:\n            joor: |\n" + indentString(joorCode, 14);
                javaStep = ".transform().joor(\"" + joorCode.replace("\n", "\\n").replace("\"", "\\\"") + "\")";
                xmlStep = "<transform>\n            <joor>\n"
                        + indentString(joorCode.replace("<", "&lt;").replace(">", "&gt;"), 16)
                        + "\n            </joor>\n        </transform>";
            } else {
                yamlStep = "transform:\n            joor: \"resource:" + fileUri + "\"";
                javaStep = ".transform().joor(\"" + fileUri + "\")";
                xmlStep = "<transform>\n            <joor>resource:" + fileUri + "</joor>\n        </transform>";
            }
        } else if ("freemarker".equals(type)
                && (sourceMsg.trim().startsWith("{") || sourceMsg.trim().startsWith("["))) {
            yamlStep = "unmarshal:\n            json:\n              library: Jackson\n        - to:\n            uri: \"freemarker:"
                    + fileUri + "\"";
            javaStep = ".unmarshal().json(org.apache.camel.model.dataformat.JsonLibrary.Jackson)\n            .to(\"freemarker:"
                    + fileUri + "\")";
            xmlStep = "<unmarshal>\n            <json library=\"Jackson\"/>\n        </unmarshal>\n        <to uri=\"freemarker:"
                    + fileUri + "\"/>";
        } else {
            yamlStep = "to:\n            uri: \"" + type + ":" + fileUri + "\"";
            javaStep = ".to(\"" + type + ":" + fileUri + "\")";
            xmlStep = "<to uri=\"" + type + ":" + fileUri + "\"/>";
        }

        // Generate full runnable route DSLs
        String yamlDsl = "";
        String javaDsl = "";
        String xmlDsl = "";

        String yamlHeaderSteps = "";
        String javaHeaderSteps = "";
        String xmlHeaderSteps = "";
        if ("freemarker".equals(type)) {
            yamlHeaderSteps = "        - setHeader:\n" +
                    "            name: \"name\"\n" +
                    "            constant: \"Camel Developer\"\n" +
                    "        - setHeader:\n" +
                    "            name: \"date\"\n" +
                    "            simple: \"${date:now:yyyy-MM-dd}\"\n";
            javaHeaderSteps = "            .setHeader(\"name\").constant(\"Camel Developer\")\n" +
                    "            .setHeader(\"date\").simple(\"${date:now:yyyy-MM-dd}\")\n";
            xmlHeaderSteps = "        <setHeader name=\"name\">\n" +
                    "            <constant>Camel Developer</constant>\n" +
                    "        </setHeader>\n" +
                    "        <setHeader name=\"date\">\n" +
                    "            <simple>${date:now:yyyy-MM-dd}</simple>\n" +
                    "        </setHeader>\n";
        }

        yamlDsl = "- route:\n" +
                "    id: transform-test-route\n" +
                "    from:\n" +
                "      uri: \"timer:trigger?repeatCount=1&delay=0\"\n" +
                "      steps:\n" +
                "        - setBody:\n" +
                "            constant: |\n" +
                (sourceMsg.isEmpty() ? "              [source message]\n" : indentString(sourceMsg, 14) + "\n") +
                yamlHeaderSteps +
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
                javaHeaderSteps +
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
                (sourceMsg.isEmpty() ? "                [source message]\n" : indentString(xmlSourceContent, 16) + "\n")
                +
                "            </constant>\n" +
                "        </setBody>\n" +
                xmlHeaderSteps +
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
        txtJbang.getStyleClass().add("snippet-code-area");
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
        txtGradle.getStyleClass().add("snippet-code-area");
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
        txtMaven.getStyleClass().add("snippet-code-area");
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
            yamlHeader.append("# camel-k: dependency=mvn:").append(dep).append("\n");
            javaHeader.append("//DEPS ").append(dep).append("\n");
            xmlHeader.append("<!-- camel-k: dependency=mvn:").append(dep).append(" -->\n");
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
        txtYaml.getStyleClass().addAll("snippet-code-area", "snippet-dsl");
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
        txtJava.getStyleClass().addAll("snippet-code-area", "snippet-dsl");
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
        txtXml.getStyleClass().addAll("snippet-code-area", "snippet-dsl");
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
                } catch (Exception ignored) {
                }
                snippetProcess = null;
            }
            if (consolePane != null) {
                consolePane.clear();
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

            String executablePath = RouteBuilderApp.getJbangExecutable();

            List<String> command = new ArrayList<>();
            if (hasStdbuf()) {
                command.add("stdbuf");
                command.add("-oL");
                command.add("-eL");
            }
            command.add(executablePath);
            command.add("--main=main.CamelJBang");
            String catalogPath = RouteBuilderApp.getJbangCatalog();
            if (catalogPath != null) {
                command.add("--catalog=" + catalogPath);
            }
            command.add("camel");
            command.add("run");
            command.add("--port=0");
            command.add(tempFile.getAbsolutePath());
            for (String dep : deps) {
                command.add("--dependency=" + dep);
            }
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

                pipeSnippetStream(snippetProcess, snippetProcess.getInputStream(), btnRunSnippet, btnStopSnippet,
                        tempFile);
                pipeSnippetStream(snippetProcess, snippetProcess.getErrorStream(), btnRunSnippet, btnStopSnippet,
                        tempFile);

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
                } catch (Exception ignored) {
                }
                snippetProcess = null;
            }
            try {
                String executablePath = RouteBuilderApp.getJbangExecutable();
                java.util.List<String> stopCmd = new java.util.ArrayList<>();
                stopCmd.add(executablePath);
                stopCmd.add("--main=main.CamelJBang");
                String catalogPath = RouteBuilderApp.getJbangCatalog();
                if (catalogPath != null) {
                    stopCmd.add("--catalog=" + catalogPath);
                }
                stopCmd.add("camel");
                stopCmd.add("stop");
                new ProcessBuilder(stopCmd).start();
            } catch (Exception ex) {
                ex.printStackTrace();
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
        if (!os.contains("linux"))
            return false;
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "which", "stdbuf" });
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String toFileUriString(File file) {
        String uri = file.toURI().toString();
        if (uri.startsWith("file:/") && !uri.startsWith("file:///")) {
            uri = "file:///" + uri.substring(6);
        }
        return uri;
    }

    private void pipeSnippetStream(Process process, java.io.InputStream stream, Button btnRun, Button btnStop,
            File tempFile) {
        new Thread(() -> {
            byte[] buf = new byte[2048];
            int n;
            try {
                while ((n = stream.read(buf)) != -1) {
                    final String chunk = new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8);
                    if (consolePane != null)
                        consolePane.log(chunk);
                }
            } catch (Exception ignored) {
            } finally {
                Platform.runLater(() -> {
                    if (process != null && !process.isAlive()) {
                        btnRun.setDisable(false);
                        btnStop.setDisable(true);
                        try {
                            int code = process.exitValue();
                            if (code == 143 || code == 130) {
                                log("Process stopped by user.");
                            } else {
                                log("Process exited with code: " + code);
                            }
                        } catch (Exception ignored2) {
                        }
                        if (tempFile.exists()) {
                            tempFile.delete();
                        }
                    }
                });
            }
        }, "snippet-stream-pipe").start();
    }

    private void log(String msg) {
        if (consolePane != null) {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            consolePane.log("[" + time + "] " + msg + "\n");
        }
    }

    private void generateSampleMappings(File base) {
        try {
            String filesIndex = readResource("/samplemapping/files.txt");
            if (filesIndex == null || filesIndex.trim().isEmpty()) {
                log("Error: samplemapping/files.txt not found or empty.");
                return;
            }
            String[] lines = filesIndex.split("\\r?\\n");
            for (String relativePath : lines) {
                relativePath = relativePath.trim();
                if (relativePath.isEmpty())
                    continue;

                // Load content of the resource
                String content = readResource("/samplemapping/" + relativePath);

                // Recreate directory structure under base directory
                File targetFile = new File(base, relativePath);
                File parentDir = targetFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // Write the file content
                Files.writeString(targetFile.toPath(), content);
            }
            log("Successfully generated sample mappings in: " + base.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            log("Error generating sample mappings: " + e.getMessage());
        }
    }

    private String readResource(String path) {
        try (java.io.InputStream is = TransformationStudioWindow.class.getResourceAsStream(path)) {
            if (is == null) {
                return "";
            }
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void toggleAllNodes(TreeItem<?> item, boolean expanded) {
        if (item != null) {
            item.setExpanded(expanded);
            for (TreeItem<?> child : item.getChildren()) {
                toggleAllNodes(child, expanded);
            }
        }
    }

    private enum CheckState {
        CHECKED, UNCHECKED, INDETERMINATE
    }

    private boolean isMappingFolder(File file) {
        if (!file.isDirectory())
            return false;
        return new File(file, "transformation.json").exists();
    }

    private CheckState getFolderCheckState(File folder) {
        java.util.List<File> mappings = new java.util.ArrayList<>();
        collectMappingsRecursive(folder, mappings);
        if (mappings.isEmpty())
            return CheckState.UNCHECKED;

        int checkedCount = 0;
        for (File f : mappings) {
            if (checkedFiles.contains(f))
                checkedCount++;
        }

        if (checkedCount == 0)
            return CheckState.UNCHECKED;
        if (checkedCount == mappings.size())
            return CheckState.CHECKED;
        return CheckState.INDETERMINATE;
    }

    private void collectMappingsRecursive(File file, java.util.List<File> list) {
        if (isMappingFolder(file)) {
            list.add(file);
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && !child.getName().startsWith(".")) {
                    collectMappingsRecursive(child, list);
                }
            }
        }
    }

    private void setCheckedRecursive(File file, boolean checked) {
        if (isMappingFolder(file)) {
            if (checked)
                checkedFiles.add(file);
            else
                checkedFiles.remove(file);
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && !child.getName().startsWith(".")) {
                    setCheckedRecursive(child, checked);
                }
            }
        }
    }

    private static String escapeJavaString(String source) {
        if (source == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                // skip
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
