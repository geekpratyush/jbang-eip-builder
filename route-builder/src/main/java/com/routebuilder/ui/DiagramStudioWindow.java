package com.routebuilder.ui;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.json.JSONObject;
import netscape.javascript.JSObject;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class DiagramStudioWindow {
    public static final List<DiagramStudioWindow> activeInstances = new ArrayList<>();

    private final Stage stage;
    private File workspaceRoot;
    private TreeView<File> treeView;
    private TabPane tabPane;
    private final Map<File, Tab> openTabs = new HashMap<>();
    private final Map<Tab, WebEngine> tabEngines = new HashMap<>();
    private final Map<Tab, WebEngine> tabCanvasEngines = new HashMap<>();
    private final Map<Tab, File> tabFiles = new HashMap<>();
    private final Map<Tab, WebView> tabEditorViews = new HashMap<>();
    private final Map<Tab, WebView> tabCanvasViews = new HashMap<>();
    private final Map<Tab, Parent> tabEditorContainers = new HashMap<>();
    
    private ComboBox<String> studioThemeBox;
    private ComboBox<String> diagramThemeBox;
    private String currentThemeName = RouteBuilderApp.currentThemeName;
    private String currentDiagramTheme = "default";

    private static final String[] DIAGRAM_THEMES = {
        "default", "base", "forest", "dark", "neutral", "vibrant", "corporate", "ocean", "sunset", "midnight"
    };

    private ComboBox<String> boardThemeBox;
    private ComboBox<String> layoutModeBox;
    private String currentBoardTheme = "Dark Grid";
    private String currentOrientation = "T-D";
    private String currentLayoutMode = "Code Left";
    private static final String[] BOARD_THEMES = { 
        "Plain White", "Grey Paper", "Soft Grey", "Dotted Grey", "Light Grid", 
        "Dark Grid", "Dark Dotted", "Clean Dark", "Navy Blue", "Hacker Black", "Blueprint" 
    };
    private static final String[] LAYOUT_MODES = {
        "Code Left", "Code Right", "Code Top", "Code Bottom"
    };

    public DiagramStudioWindow() {
        activeInstances.add(this);
        this.stage = new Stage();
        
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
        String savedRoot = prefs.get("workspaceRoot", null);
        File defaultRoot = new File(System.getProperty("user.dir"), "diagram-workspace");
        
        if (savedRoot != null) {
            File f = new File(savedRoot);
            if (f.exists() && f.isDirectory()) {
                this.workspaceRoot = f;
            }
        }
        
        if (this.workspaceRoot == null) {
            this.workspaceRoot = defaultRoot;
        }

        this.currentDiagramTheme = prefs.get("diagramTheme", "default");
        this.currentBoardTheme = prefs.get("boardTheme", "Dark Grid");
        this.currentOrientation = prefs.get("orientation", "T-D");
        this.currentLayoutMode = prefs.get("layoutMode", "Code Left");

        if (!workspaceRoot.exists()) {
            workspaceRoot.mkdirs();
        }
    }

    public void show() {
        stage.setTitle("Universal Diagram Studio IDE");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.getStyleClass().add(RouteBuilderApp.currentThemeClass);

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("editor-toolbar");

        Button btnNew = new Button("New", new FontIcon("fas-plus"));
        btnNew.getStyleClass().addAll("toolbar-btn", "btn-new");
        btnNew.setOnAction(e -> createNewDiagram());

        Button btnOpenWorkspace = new Button("Workspace", new FontIcon("fas-folder-open"));
        btnOpenWorkspace.getStyleClass().addAll("toolbar-btn", "btn-workspace");
        btnOpenWorkspace.setTooltip(new Tooltip("Change Drawing Workspace Location"));
        btnOpenWorkspace.setOnAction(e -> changeWorkspace());

        Button btnSave = new Button("Save", new FontIcon("fas-save"));
        btnSave.getStyleClass().addAll("toolbar-btn", "btn-save");
        btnSave.setOnAction(e -> saveCurrentTab());

        Button btnRefreshWorkspace = new Button(null, new FontIcon("fas-sync-alt"));
        btnRefreshWorkspace.getStyleClass().add("toolbar-btn");
        btnRefreshWorkspace.setTooltip(new Tooltip("Refresh File Tree"));
        btnRefreshWorkspace.setOnAction(e -> refreshTree());

        Button btnSwap = new Button(null, new FontIcon("fas-exchange-alt"));
        btnSwap.getStyleClass().add("toolbar-btn");
        btnSwap.setTooltip(new Tooltip("Swap Editor and Canvas"));
        btnSwap.setOnAction(e -> swapPanels());

        Button btnExport = new Button("Export SVG", new FontIcon("fas-download"));
        btnExport.getStyleClass().addAll("toolbar-btn", "btn-export");
        btnExport.setOnAction(e -> exportCurrentDiagramToSVG());

        Button btnZoomIn = new Button(null, new FontIcon("fas-search-plus"));
        btnZoomIn.getStyleClass().add("toolbar-btn");
        btnZoomIn.setOnAction(e -> applyZoom(1.2));

        Button btnZoomOut = new Button(null, new FontIcon("fas-search-minus"));
        btnZoomOut.getStyleClass().add("toolbar-btn");
        btnZoomOut.setOnAction(e -> applyZoom(0.8));

        Button btnZoomReset = new Button("100%", new FontIcon("fas-expand"));
        btnZoomReset.getStyleClass().add("toolbar-btn");
        btnZoomReset.setOnAction(e -> resetZoom());

        diagramThemeBox = new ComboBox<>();
        diagramThemeBox.getItems().addAll(DIAGRAM_THEMES);
        diagramThemeBox.setValue(currentDiagramTheme);
        diagramThemeBox.setTooltip(new Tooltip("Diagram Drawing Theme"));
        diagramThemeBox.setOnAction(e -> {
            currentDiagramTheme = diagramThemeBox.getValue();
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
            prefs.put("diagramTheme", currentDiagramTheme);
            refreshAllTabs();
        });

        boardThemeBox = new ComboBox<>();
        boardThemeBox.getItems().addAll(BOARD_THEMES);
        boardThemeBox.setValue(currentBoardTheme);
        boardThemeBox.setTooltip(new Tooltip("Drawing Board Background Theme"));
        boardThemeBox.setOnAction(e -> {
            currentBoardTheme = boardThemeBox.getValue();
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
            prefs.put("boardTheme", currentBoardTheme);
            refreshAllTabs();
        });

        Button btnRender = new Button("Render", new FontIcon("fas-play"));
        btnRender.getStyleClass().addAll("toolbar-btn", "btn-render");
        btnRender.setTooltip(new Tooltip("Force render / refresh active diagram"));
        btnRender.setOnAction(e -> forceRenderActiveTab());

        layoutModeBox = new ComboBox<>();
        layoutModeBox.getItems().addAll(LAYOUT_MODES);
        layoutModeBox.setValue(currentLayoutMode);
        layoutModeBox.setTooltip(new Tooltip("Split Pane Layout Position"));
        layoutModeBox.setOnAction(e -> {
            currentLayoutMode = layoutModeBox.getValue();
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
            prefs.put("layoutMode", currentLayoutMode);
            updateAllTabsLayout();
        });

        Button btnOrientation = new Button();
        btnOrientation.setGraphic(new FontIcon("L-R".equals(currentOrientation) ? "fas-long-arrow-alt-right" : "fas-long-arrow-alt-down"));
        btnOrientation.setTooltip(new Tooltip("Toggle Layout Orientation (T-D / L-R)"));
        btnOrientation.getStyleClass().add("toolbar-btn");
        btnOrientation.setOnAction(e -> {
            currentOrientation = "L-R".equals(currentOrientation) ? "T-D" : "L-R";
            btnOrientation.setGraphic(new FontIcon("L-R".equals(currentOrientation) ? "fas-long-arrow-alt-right" : "fas-long-arrow-alt-down"));
            java.util.prefs.Preferences p = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
            p.put("orientation", currentOrientation);
            refreshAllTabs();
        });

        studioThemeBox = new ComboBox<>();
        studioThemeBox.getItems().addAll("VSCode Dark", "IntelliJ Light", "Dracula", "Monokai", "Hacker");
        studioThemeBox.setValue(RouteBuilderApp.currentThemeName);
        studioThemeBox.setOnAction(e -> RouteBuilderApp.setGlobalTheme(studioThemeBox.getValue()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(
            btnNew, btnOpenWorkspace, new Separator(),
            btnSave, btnRefreshWorkspace, btnSwap, btnRender, new Separator(),
            btnExport, new Separator(),
            btnZoomIn, btnZoomOut, btnZoomReset, new Separator(),
            new Label("Theme:"), diagramThemeBox, 
            new Label("Board:"), boardThemeBox,
            new Label("Layout:"), layoutModeBox,
            btnOrientation, new Separator(),
            spacer, studioThemeBox
        );
        root.setTop(toolBar);

        // --- Sidebar ---
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(50);
        sidebar.setPadding(new Insets(10));
        sidebar.setSpacing(5);
        sidebar.getStyleClass().add("studio-sidebar");

        HBox sidebarHeader = new HBox(5);
        sidebarHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label lblExplorer = new Label("EXPLORER");
        lblExplorer.getStyleClass().add("studio-explorer-label");
        HBox.setHgrow(lblExplorer, Priority.ALWAYS);
        lblExplorer.setMaxWidth(Double.MAX_VALUE);

        Button btnExpandAll = new Button(null, new FontIcon("fas-expand-arrows-alt"));
        btnExpandAll.getStyleClass().add("small-action-btn");
        btnExpandAll.setTooltip(new Tooltip("Expand All"));
        btnExpandAll.setOnAction(e -> toggleAllNodes(treeView.getRoot(), true));

        Button btnCollapseAll = new Button(null, new FontIcon("fas-compress-arrows-alt"));
        btnCollapseAll.getStyleClass().add("small-action-btn");
        btnCollapseAll.setTooltip(new Tooltip("Collapse All"));
        btnCollapseAll.setOnAction(e -> toggleAllNodes(treeView.getRoot(), false));

        sidebarHeader.getChildren().addAll(lblExplorer, btnExpandAll, btnCollapseAll);

        treeView = new TreeView<>();
        treeView.getStyleClass().add("sidebar-tree-view");
        VBox.setVgrow(treeView, Priority.ALWAYS);
        
        // --- Keyboard Shortcuts & Auto-Selection ---
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue().isFile()) {
                // Auto-switch/open on selection
                openFile(newVal.getValue());
            }
        });

        treeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<File> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null && item.getValue().isFile()) {
                    openFile(item.getValue());
                }
            }
        });

        treeView.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.ENTER) {
                TreeItem<File> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null) {
                    if (item.getValue().isFile()) openFile(item.getValue());
                    else item.setExpanded(!item.isExpanded());
                }
                event.consume();
            } else if (code == KeyCode.DELETE || (code == KeyCode.BACK_SPACE && event.isShortcutDown())) {
                deleteSelectedFile();
                event.consume();
            } else if (code == KeyCode.F5) {
                refreshTree();
                event.consume();
            }
        });
        
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    setGraphic(RouteBuilderApp.getFileIcon(item));
                }
            }
        });

        sidebar.getChildren().addAll(lblExplorer, treeView);
        
        setupTreeContextMenu();

        // --- Tabs ---
        tabPane = new TabPane();
        tabPane.getStyleClass().add("editor-tab-pane");

        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.getItems().addAll(sidebar, tabPane);
        horizontalSplit.setDividerPositions(0.2);

        root.setCenter(horizontalSplit);

        Scene scene = new Scene(root, 1400, 900);
        
        // --- Global Accelerators ---
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), this::saveCurrentTab);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN), () -> {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected != null) tabPane.getTabs().remove(selected);
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), () -> applyZoom(1.2));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), () -> applyZoom(0.8));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), this::resetZoom);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), this::refreshTree);

        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        if (RouteBuilderApp.currentDynamicCssUri != null) {
            scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
        }
        stage.setScene(scene);

        RouteBuilderApp.themedRoots.add(root);
        stage.setOnHidden(e -> {
            activeInstances.remove(this);
            RouteBuilderApp.themedRoots.remove(root);
        });

        stage.setMaximized(true);
        stage.show();

        refreshTree();
    }

    private void changeWorkspace() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Diagram Workspace Directory");
        chooser.setInitialDirectory(workspaceRoot.exists() ? workspaceRoot : new File(System.getProperty("user.dir")));
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            this.workspaceRoot = selected;
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
            prefs.put("workspaceRoot", selected.getAbsolutePath());
            refreshTree();
        }
    }

    private void refreshTree() {
        TreeItem<File> rootItem = new TreeItem<>(workspaceRoot);
        rootItem.setExpanded(true);
        buildFileTree(workspaceRoot, rootItem);
        treeView.setRoot(rootItem);
    }

    private void buildFileTree(File dir, TreeItem<File> parent) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    TreeItem<File> item = new TreeItem<>(f);
                    parent.getChildren().add(item);
                    buildFileTree(f, item);
                    // Remove empty directories
                    if (item.getChildren().isEmpty()) {
                        parent.getChildren().remove(item);
                    }
                } else {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".mmd") || name.endsWith(".mermaid") ||
                        name.endsWith(".puml") || name.endsWith(".plantuml") ||
                        name.endsWith(".dot") || name.endsWith(".gv")) {
                        
                        TreeItem<File> item = new TreeItem<>(f);
                        parent.getChildren().add(item);
                    }
                }
            }
        }
    }

    private void openFile(File file) {
        if (openTabs.containsKey(file)) {
            tabPane.getSelectionModel().select(openTabs.get(file));
            return;
        }

        tabPane.getTabs().clear();
        openTabs.clear();
        tabEngines.clear();
        tabCanvasEngines.clear();
        tabFiles.clear();

        Tab tab = new Tab(file.getName());
        openTabs.put(file, tab);
        tabFiles.put(tab, file);
        
        SplitPane tabSplit = new SplitPane();

        WebView canvasView = new WebView();
        WebView editorView = new WebView();
        RouteBuilderApp.installClipboardShortcuts(editorView);
        
        tabEditorViews.put(tab, editorView);
        tabCanvasViews.put(tab, canvasView);
        
        VBox editorBox = new VBox();
        editorBox.getStyleClass().add("editor-view-container");
        
        HBox editorToolbar = new HBox();
        editorToolbar.getStyleClass().add("editor-header-bar");
        editorToolbar.setPadding(new Insets(5));
        editorToolbar.setSpacing(10);
        editorToolbar.setAlignment(Pos.CENTER_LEFT);
        
        Label codeLabel = new Label("Editor Tools:");
        codeLabel.getStyleClass().add("editor-tools-label");
        
        ColorPicker colorPicker = new ColorPicker(Color.web("#4CAF50"));
        colorPicker.setStyle("-fx-color-label-visible: false;");
        colorPicker.setTooltip(new Tooltip("Pick Color - inserts hex code at cursor"));
        colorPicker.setOnAction(evt -> {
            insertTextInEditor(tab, toHexString(colorPicker.getValue()));
        });
        
        ComboBox<String> snippetBox = new ComboBox<>();
        snippetBox.setPromptText("Insert Snippet/Theme");
        updateSnippetDropdown(tab, snippetBox);
        
        snippetBox.setOnAction(evt -> {
            String selected = snippetBox.getValue();
            if (selected != null) {
                handleSnippetSelection(tab, selected, colorPicker);
                Platform.runLater(() -> snippetBox.setValue(null));
            }
        });
        
        editorToolbar.getChildren().addAll(codeLabel, new Label("Color:"), colorPicker, new Label("Snippets:"), snippetBox);
        
        VBox.setVgrow(editorView, Priority.ALWAYS);
        editorBox.getChildren().addAll(editorToolbar, editorView);
        
        tabEditorContainers.put(tab, editorBox);
        
        applyLayoutToSplitPane(tabSplit, editorBox, canvasView);

        tab.setContent(tabSplit);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        tabEngines.put(tab, editorView.getEngine());
        tabCanvasEngines.put(tab, canvasView.getEngine());

        try {
            String content = Files.readString(file.toPath());
            loadMonacoForTab(tab, content, getLanguage(file));
            loadCanvasForTab(tab, content, getDiagramType(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        tab.setOnClosed(e -> {
            openTabs.remove(file);
            tabEngines.remove(tab);
            tabCanvasEngines.remove(tab);
            tabFiles.remove(tab);
            tabEditorViews.remove(tab);
            tabCanvasViews.remove(tab);
            tabEditorContainers.remove(tab);
        });
    }

    private void applyLayoutToSplitPane(SplitPane tabSplit, Parent editorNode, WebView canvasView) {
        tabSplit.getItems().clear();
        switch (currentLayoutMode) {
            case "Code Right":
                tabSplit.setOrientation(Orientation.HORIZONTAL);
                tabSplit.getItems().addAll(canvasView, editorNode);
                break;
            case "Code Top":
                tabSplit.setOrientation(Orientation.VERTICAL);
                tabSplit.getItems().addAll(editorNode, canvasView);
                break;
            case "Code Bottom":
                tabSplit.setOrientation(Orientation.VERTICAL);
                tabSplit.getItems().addAll(canvasView, editorNode);
                break;
            case "Code Left":
            default:
                tabSplit.setOrientation(Orientation.HORIZONTAL);
                tabSplit.getItems().addAll(editorNode, canvasView);
                break;
        }
        tabSplit.setDividerPositions(0.5);
    }

    private void updateSnippetDropdown(Tab tab, ComboBox<String> box) {
        box.getItems().clear();
        File file = tabFiles.get(tab);
        if (file == null) return;
        String type = getDiagramType(file);
        
        if ("plantuml".equalsIgnoreCase(type)) {
            box.getItems().addAll(
                "Theme: Metal",
                "Theme: Superhero",
                "Theme: Black Knight",
                "Theme: Cerulean",
                "Skinparam Class Background",
                "Skinparam Actor Background",
                "Color: Element Inline Color"
            );
        } else if ("mermaid".equalsIgnoreCase(type)) {
            box.getItems().addAll(
                "Theme: Dark",
                "Theme: Forest",
                "Theme: Neutral",
                "Theme: Base (Custom Primary)",
                "Style: Node Styling Template",
                "Style: Stroke Dasharray",
                "Link Style: Direct Link Color"
            );
        } else if ("graphviz".equalsIgnoreCase(type)) {
            box.getItems().addAll(
                "Style: Filled Node (Yellow)",
                "Style: Filled Node (Custom)",
                "Style: Colored Edge",
                "Graph: Background Color"
            );
        } else {
            box.getItems().addAll(
                "Insert Color Hex"
            );
        }
    }

    private void handleSnippetSelection(Tab tab, String selection, ColorPicker colorPicker) {
        if (selection == null || selection.isEmpty()) return;
        
        String hex = toHexString(colorPicker.getValue());
        String snippet = "";
        
        switch (selection) {
            // PlantUML
            case "Theme: Metal": snippet = "!theme metal\n"; break;
            case "Theme: Superhero": snippet = "!theme superhero\n"; break;
            case "Theme: Black Knight": snippet = "!theme black-knight\n"; break;
            case "Theme: Cerulean": snippet = "!theme cerulean\n"; break;
            case "Skinparam Class Background": snippet = "skinparam classBackgroundColor " + hex + "\n"; break;
            case "Skinparam Actor Background": snippet = "skinparam actorBackgroundColor " + hex + "\n"; break;
            case "Color: Element Inline Color": snippet = " " + hex; break;
            
            // Mermaid
            case "Theme: Dark": snippet = "%%{init: {'theme': 'dark'}}%%\n"; break;
            case "Theme: Forest": snippet = "%%{init: {'theme': 'forest'}}%%\n"; break;
            case "Theme: Neutral": snippet = "%%{init: {'theme': 'neutral'}}%%\n"; break;
            case "Theme: Base (Custom Primary)": snippet = "%%{init: {'theme': 'base', 'themeVariables': { 'primaryColor': '" + hex + "' }}}%%\n"; break;
            case "Style: Node Styling Template": snippet = "style NODE_ID fill:" + hex + ",stroke:#333,stroke-width:2px;\n"; break;
            case "Style: Stroke Dasharray": snippet = "style NODE_ID stroke-dasharray: 5 5;\n"; break;
            case "Link Style: Direct Link Color": snippet = "linkStyle 0 stroke:" + hex + ",stroke-width:2px;\n"; break;
            
            // Graphviz
            case "Style: Filled Node (Yellow)": snippet = " [fillcolor=\"yellow\", style=filled]"; break;
            case "Style: Filled Node (Custom)": snippet = " [fillcolor=\"" + hex + "\", style=filled]"; break;
            case "Style: Colored Edge": snippet = " [color=\"" + hex + "\"]"; break;
            case "Graph: Background Color": snippet = "bgcolor=\"" + hex + "\""; break;
            
            default:
                snippet = hex;
                break;
        }
        
        insertTextInEditor(tab, snippet);
    }

    private String toHexString(Color val) {
        return String.format("#%02X%02X%02X",
            (int)(val.getRed() * 255),
            (int)(val.getGreen() * 255),
            (int)(val.getBlue() * 255)
        );
    }

    private void insertTextInEditor(Tab tab, String text) {
        WebEngine engine = tabEngines.get(tab);
        if (engine != null) {
            try {
                String escapedText = text.replace("\\", "\\\\")
                                         .replace("'", "\\'")
                                         .replace("\"", "\\\"")
                                         .replace("\n", "\\n")
                                         .replace("\r", "");
                engine.executeScript("if(window.editor) { window.editor.trigger('keyboard', 'type', { text: '" + escapedText + "' }); window.editor.focus(); }");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadMonacoForTab(Tab tab, String content, String language) {
        WebEngine engine = tabEngines.get(tab);
        String activeTheme = RouteBuilderApp.currentThemeClass;
        
        String monacoBase = getClass().getResource("/monaco/vs/loader.js").toExternalForm();
        monacoBase = monacoBase.substring(0, monacoBase.lastIndexOf("/vs/loader.js"));

        String html = getMonacoHtml(content, language);
        
        engine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new DiagramBridge(tab));
                
                // Ensure theme is set after load
                String mTheme = activeTheme.contains("light") ? "vs" : "vs-dark";
                engine.executeScript("if(window.setTheme) window.setTheme('" + mTheme + "');");
            }
        });
        
        engine.loadContent(html);
    }

    private void loadCanvasForTab(Tab tab, String content, String type) {
        WebEngine engine = tabCanvasEngines.get(tab);
        String html = getCanvasHtml(content, type);
        engine.loadContent(html);
    }

    public class DiagramBridge {
        private final Tab tab;
        public DiagramBridge(Tab tab) { this.tab = tab; }
        public void onContentChanged(String newCode) {
            Platform.runLater(() -> {
                File file = tabFiles.get(tab);
                if (file == null) return;
                
                String type = getDiagramType(file);
                if ("plantuml".equalsIgnoreCase(type)) {
                    requestPlantUmlRender(newCode);
                } else {
                    WebEngine canvasEngine = tabCanvasEngines.get(tab);
                    if (canvasEngine != null) {
                        try {
                            String base64 = java.util.Base64.getEncoder().encodeToString(newCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            canvasEngine.executeScript("if(window.updateDiagramBase64) window.updateDiagramBase64(\"" + base64 + "\");");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        public void requestPlantUmlRender(String code) {
            String svg = renderPlantUmlToSvg(code);
            Platform.runLater(() -> {
                WebEngine canvasEngine = tabCanvasEngines.get(tab);
                if (canvasEngine != null) {
                    try {
                        String base64 = java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        canvasEngine.executeScript("if(window.updateSvgBase64) window.updateSvgBase64(\"" + base64 + "\");");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private String getMonacoHtml(String content, String language) {
        String activeTheme = RouteBuilderApp.currentThemeClass;
        String editorBg = "#1e1e1e";
        if ("theme-intellij-light".equals(activeTheme)) editorBg = "#ffffff";
        else if ("theme-dracula".equals(activeTheme)) editorBg = "#282a36";
        else if ("theme-monokai".equals(activeTheme)) editorBg = "#272822";
        else if ("theme-hacker".equals(activeTheme)) editorBg = "#050505";

        String encodedContent = "";
        try {
            encodedContent = java.net.URLEncoder.encode(content, "UTF-8").replace("+", "%20");
        } catch (Exception ignored) {}
        
        String monacoBase = getClass().getResource("/monaco/vs/loader.js").toExternalForm();
        monacoBase = monacoBase.substring(0, monacoBase.lastIndexOf("/vs/loader.js"));

        return "<!DOCTYPE html><html><head><base href=\"" + monacoBase + "/\"/><meta charset=\"UTF-8\">" +
               "<style>body { margin: 0; padding: 0; overflow: hidden; background-color: " + editorBg + "; } #editor { width: 100vw; height: 100vh; }</style>" +
               "</head><body><div id=\"editor\"></div>" +
               "<script src=\"" + monacoBase + "/vs/loader.js\"></script>" +
               "<script>" +
               "require.config({ paths: { vs: '" + monacoBase + "/vs' }});" +
               "require(['vs/editor/editor.main'], function() {" +
               "  try {" +
               "    // Register Mermaid language\n" +
               "    monaco.languages.register({ id: 'mermaid' });\n" +
               "    monaco.languages.setMonarchTokensProvider('mermaid', {\n" +
               "      tokenizer: {\n" +
               "        root: [\n" +
               "          [/[a-z_$][\\w$]*/, { cases: { '@keywords': 'keyword', '@default': 'identifier' } }],\n" +
               "          { include: '@whitespace' },\n" +
               "          [/\\[\\[.*?\\]\\]/, 'annotation'],\n" +
               "          [/\\d+/, 'number'],\n" +
               "          [/\"/, { token: 'string.quote', bracket: '@open', next: '@string' }],\n" +
               "        ],\n" +
               "        string: [\n" +
               "          [/[^\"]+/, 'string'],\n" +
               "          [/\"/, { token: 'string.quote', bracket: '@close', next: '@pop' }],\n" +
               "        ],\n" +
               "        whitespace: [[/[ \\t\\r\\n]+/, 'white'], [/%%.*$/, 'comment']],\n" +
               "      },\n" +
               "      keywords: ['graph', 'TD', 'LR', 'subgraph', 'end', 'sequenceDiagram', 'participant', 'as', 'note', 'loop', 'alt', 'else', 'opt', 'classDiagram', 'stateDiagram-v2', 'erDiagram', 'pie'],\n" +
               "    });\n" +
               "\n" +
               "    // Register PlantUML language\n" +
               "    monaco.languages.register({ id: 'plantuml' });\n" +
               "    monaco.languages.setMonarchTokensProvider('plantuml', {\n" +
               "      tokenizer: {\n" +
               "        root: [\n" +
               "          [/^\\s*@startuml/, 'keyword'], [/^\\s*@enduml/, 'keyword'],\n" +
               "          [/![a-z]*/, 'keyword'],\n" +
               "          [/[a-z_$][\\w$]*/, { cases: { '@keywords': 'keyword', '@default': 'identifier' } }],\n" +
               "          { include: '@whitespace' },\n" +
               "          [/\"/, { token: 'string.quote', bracket: '@open', next: '@string' }],\n" +
               "        ],\n" +
               "        string: [[/[^\"]+/, 'string'], [/\"/, { token: 'string.quote', bracket: '@close', next: '@pop' }]],\n" +
               "        whitespace: [[/[ \\t\\r\\n]+/, 'white'], [/' .*$/, 'comment']],\n" +
               "      },\n" +
               "      keywords: ['actor', 'boundary', 'control', 'entity', 'participant', 'as', 'order', 'box', 'newpage', 'title', 'header', 'footer', 'caption', 'legend', 'package', 'node', 'cloud', 'database', 'storage', 'agent', 'artifact', 'component', 'interface', 'class', 'enum', 'struct'],\n" +
               "    });\n" +
               "\n" +
               "    // Register Graphviz (dot) language\n" +
               "    monaco.languages.register({ id: 'dot' });\n" +
               "    monaco.languages.setMonarchTokensProvider('dot', {\n" +
               "      tokenizer: {\n" +
               "        root: [\n" +
               "          [/[a-z_$][\\w$]*/, { cases: { '@keywords': 'keyword', '@default': 'identifier' } }],\n" +
               "          { include: '@whitespace' },\n" +
               "          [/\"/, { token: 'string.quote', bracket: '@open', next: '@string' }],\n" +
               "        ],\n" +
               "        string: [[/[^\"]+/, 'string'], [/\"/, { token: 'string.quote', bracket: '@close', next: '@pop' }]],\n" +
               "        whitespace: [[/[ \\t\\r\\n]+/, 'white'], [/\\/\\/.*$/, 'comment'], [/\\/\\*/, 'comment', '@comment']],\n" +
               "      },\n" +
               "      comment: [[/[^\\/*]+/, 'comment'], [/[\\/*]/, 'comment'], [/\\*\\//, 'comment', '@pop']],\n" +
               "      keywords: ['graph', 'digraph', 'subgraph', 'node', 'edge', 'strict'],\n" +
               "    });\n" +
               "\n" +
               "    monaco.editor.defineTheme('theme-vscode-dark', { base: 'vs-dark', inherit: true, rules: [ { token: 'keyword', foreground: '569cd6', fontStyle: 'bold' }, { token: 'comment', foreground: '6a9955', fontStyle: 'italic' }, { token: 'string', foreground: 'ce9178' } ], colors: { 'editor.background': '#1e1e1e' } });\n" +
               "    monaco.editor.defineTheme('theme-intellij-light', { base: 'vs', inherit: true, rules: [ { token: 'keyword', foreground: '0000ff', fontStyle: 'bold' }, { token: 'comment', foreground: '808080', fontStyle: 'italic' }, { token: 'string', foreground: '008000' } ], colors: { 'editor.background': '#ffffff' } });\n" +
               "    monaco.editor.defineTheme('theme-dracula', { base: 'vs-dark', inherit: true, rules: [ { token: 'keyword', foreground: 'ff79c6', fontStyle: 'bold' }, { token: 'comment', foreground: '6272a4', fontStyle: 'italic' }, { token: 'string', foreground: 'f1fa8c' } ], colors: { 'editor.background': '#282a36' } });\n" +
               "    monaco.editor.defineTheme('theme-monokai', { base: 'vs-dark', inherit: true, rules: [ { token: 'keyword', foreground: 'f92672', fontStyle: 'bold' }, { token: 'comment', foreground: '75715e', fontStyle: 'italic' }, { token: 'string', foreground: 'e6db74' } ], colors: { 'editor.background': '#272822' } });\n" +
               "    monaco.editor.defineTheme('theme-hacker', { base: 'hc-black', inherit: true, rules: [ { token: 'keyword', foreground: '00ff00', fontStyle: 'bold' }, { token: 'comment', foreground: '00cc00', fontStyle: 'italic' }, { token: 'string', foreground: '00ff00' } ], colors: { 'editor.background': '#050505' } });\n" +
               "  } catch(e) { console.error('Registration error', e); }\n" +
               "\n" +
               "  window.editor = monaco.editor.create(document.getElementById('editor'), {" +
               "    value: decodeURIComponent('" + encodedContent + "')," +
               "    language: '" + language + "'," +
               "    theme: '" + activeTheme + "'," +
               "    automaticLayout: true," +
               "    fontSize: 14," +
               "    minimap: { enabled: false }" +
               "  });" +
               "  window.editor.onDidChangeModelContent(function() {" +
               "    if(window.javaBridge) window.javaBridge.onContentChanged(window.editor.getValue());" +
               "  });" +
               "  window.setTheme = function(t) { monaco.editor.setTheme(t); };" +
               "});" +
               "</script></body></html>";
    }

    private String getCanvasHtml(String content, String type) {
        if ("mermaid".equalsIgnoreCase(type)) {
            return getMermaidHtml(content);
        } else if ("plantuml".equalsIgnoreCase(type)) {
            return getPlantUmlHtml(content);
        } else if ("graphviz".equalsIgnoreCase(type)) {
            return getGraphvizHtml(content);
        }
        
        String boardCss = getBoardThemeCss();
        String textColor = currentThemeName.equalsIgnoreCase("IntelliJ Light") ? "#333333" : "#aaaaaa";
        return "<html><body style='margin: 0; padding: 0; overflow: hidden; font-family: sans-serif; " + boardCss + " color: " + textColor + "; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0;'>" +
               "<div style='text-align: center;'><h2>" + type.toUpperCase() + " Rendering</h2><p>Please edit the code on the right.<br/>Native rendering for " + type + " coming soon.</p></div></body></html>";
    }

    private String getGraphvizHtml(String content) {
        String boardCss = getBoardThemeCss();
        String processedCode = content.trim();
        if (processedCode.startsWith("digraph") || processedCode.startsWith("graph")) {
            String dir = currentOrientation.equals("L-R") ? "LR" : "TB";
            if (!processedCode.contains("rankdir")) {
                int braceIdx = processedCode.indexOf("{");
                if (braceIdx != -1) {
                    processedCode = processedCode.substring(0, braceIdx + 1) + "\n  rankdir=" + dir + ";\n" + processedCode.substring(braceIdx + 1);
                }
            }
        }

        String base64Code = java.util.Base64.getEncoder().encodeToString(processedCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
               "<script src=\"" + getClass().getResource("/styles/viz.js").toExternalForm() + "\"></script>" +
               "<script src=\"" + getClass().getResource("/styles/lite.render.js").toExternalForm() + "\"></script>" +
               "<style>" +
               "  * { -webkit-user-select: none; user-select: none; }" +
               "  body { margin: 0; padding: 0; overflow: hidden; font-family: 'Segoe UI', sans-serif; " + boardCss + " }" +
               "  #container { width: 100vw; height: 100vh; cursor: grab; overflow: hidden; display: flex; justify-content: center; align-items: center; }" +
               "  #container:active { cursor: grabbing; }" +
               "  #viewport { transform-origin: 0 0; transition: transform 0.1s ease-out; }" +
               "  .diagram-block { padding: 30px; background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); display: inline-block; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }" +
               "  .theme-intellij-light .diagram-block { background: rgba(0,0,0,0.02); border: 1px solid rgba(0,0,0,0.1); box-shadow: 0 5px 15px rgba(0,0,0,0.1); }" +
               "  svg { max-width: 100%; height: auto; }" +
               "</style></head><body>" +
               "<div id=\"container\"><div id=\"viewport\"></div></div>" +
               "<script>" +
               "var viz = new Viz(); var scale = 1.0, tx = 0, ty = 0, isDragging = false, lastX, lastY;" +
               "var container = document.getElementById('container'), viewport = document.getElementById('viewport');" +
               "window.addEventListener('dragstart', function(e) { e.preventDefault(); });" +
               "function render(code) {" +
               "  if (!code || code.trim().length === 0) return;" +
               "  viz.renderSVGElement(code).then(function(element) {" +
               "    viewport.innerHTML = ''; var div = document.createElement('div'); div.className='diagram-block'; div.appendChild(element); viewport.appendChild(div);" +
               "  }).catch(error => { viewport.innerHTML = '<div style=\"color:red\">Graphviz Error: ' + error + '</div>'; });" +
               "}" +
               "container.onwheel = function(e) { e.preventDefault(); var delta = e.deltaY > 0 ? 0.9 : 1.1; scale *= delta; updateTransform(); };" +
               "container.onmousedown = function(e) { if(e.button === 0) { isDragging = true; lastX = e.clientX; lastY = e.clientY; e.preventDefault(); } };" +
               "window.onmousemove = function(e) { if (!isDragging) return; tx += (e.clientX - lastX); ty += (e.clientY - lastY); lastX = e.clientX; lastY = e.clientY; updateTransform(); };" +
               "window.onmouseup = function() { isDragging = false; };" +
               "function updateTransform() { viewport.style.transform = 'translate(' + tx + 'px,' + ty + 'px) scale(' + scale + ')'; }" +
               "window.updateDiagram = function(code) { render(code); };" +
               "window.updateDiagramBase64 = function(base64) {" +
               "  var code = decodeURIComponent(escape(window.atob(base64)));" +
               "  window.updateDiagram(code);" +
               "};" +
               "window.onload = function() {" +
               "  var initialCode = decodeURIComponent(escape(window.atob('" + base64Code + "')));" +
               "  render(initialCode);" +
               "};" +
               "</script></body></html>";
    }

    private String getPlantUmlHtml(String content) {
        String boardCss = getBoardThemeCss();
        String processed = content.trim();
        if (currentOrientation.equals("L-R") && !processed.contains("left to right direction")) {
            processed = processed.replace("@startuml", "@startuml\nleft to right direction");
        }
        String svg = renderPlantUmlToSvg(processed);
        String base64Svg = java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
               "<style>" +
               "  * { -webkit-user-select: none; user-select: none; }" +
               "  body { margin: 0; padding: 0; overflow: hidden; font-family: 'Segoe UI', sans-serif; " + boardCss + " }" +
               "  #container { width: 100vw; height: 100vh; cursor: grab; overflow: hidden; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; }" +
               "  #container:active { cursor: grabbing; }" +
               "  #viewport { transform-origin: 0 0; padding: 100px; display: inline-block; min-width: 100%; text-align: center; }" +
               "  .diagram-block { margin-bottom: 50px; padding: 30px; background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); display: inline-block; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }" +
               "  .theme-intellij-light .diagram-block { background: rgba(0,0,0,0.02); border: 1px solid rgba(0,0,0,0.1); box-shadow: 0 5px 15px rgba(0,0,0,0.1); }" +
               "  svg { max-width: 100%; height: auto; }" +
               "</style></head><body class=\"" + (currentThemeName.equalsIgnoreCase("IntelliJ Light") ? "theme-intellij-light" : "") + "\">" +
               "<div id=\"container\"><div id=\"viewport\">" +
               "</div></div>" +
               "<script>" +
               "var scale = 1.0, tx = 0, ty = 0, isDragging = false, lastX, lastY;" +
               "var container = document.getElementById('container'), viewport = document.getElementById('viewport');" +
               "window.addEventListener('dragstart', function(e) { e.preventDefault(); });" +
               "container.onwheel = function(e) {" +
               "  e.preventDefault();" +
               "  var delta = e.deltaY > 0 ? 0.9 : 1.1;" +
               "  scale *= delta;" +
               "  updateTransform();" +
               "};" +
               "container.onmousedown = function(e) { if(e.button === 0) { isDragging = true; lastX = e.clientX; lastY = e.clientY; e.preventDefault(); } };" +
               "window.onmousemove = function(e) {" +
               "  if (!isDragging) return;" +
               "  tx += (e.clientX - lastX); ty += (e.clientY - lastY);" +
               "  lastX = e.clientX; lastY = e.clientY;" +
               "  updateTransform();" +
               "};" +
               "window.onmouseup = function() { isDragging = false; };" +
               "function updateTransform() {" +
               "  viewport.style.transform = 'translate(' + tx + 'px,' + ty + 'px) scale(' + scale + ')';" +
               "}" +
               "window.updateDiagram = function(code) {" +
               "  if(window.javaBridge) window.javaBridge.requestPlantUmlRender(code);" +
               "};" +
               "window.updateSvg = function(svg) {" +
               "  viewport.innerHTML = '<div class=\"diagram-block\">' + svg + '</div>';" +
               "};" +
               "window.updateSvgBase64 = function(base64) {" +
               "  var svg = decodeURIComponent(escape(window.atob(base64)));" +
               "  window.updateSvg(svg);" +
               "};" +
               "window.onload = function() {" +
               "  window.updateSvgBase64('" + base64Svg + "');" +
               "};" +
               "</script></body></html>";
    }

    private String renderPlantUmlToSvg(String content) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            String processed = content.trim();
            if (!processed.startsWith("@startuml")) {
                processed = "@startuml\n" + processed + "\n@enduml";
            }
            
            // Force Smetana (Java-based Graphviz) to avoid dot executable dependency
            if (!processed.contains("!pragma layout smetana")) {
                processed = processed.replace("@startuml", "@startuml\n!pragma layout smetana");
            }
            
            // Support built-in offline C4 library inside PlantUML jar by using standard include path syntax <C4/...>
            processed = processed.replaceAll("https?://raw\\.githubusercontent\\.com/plantuml-stdlib/C4-PlantUML/master/([A-Za-z0-9_]+)(\\.puml)?", "<C4/$1>");

            SourceStringReader reader = new SourceStringReader(processed);
            reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
            return os.toString("UTF-8");
        } catch (Exception e) {
            return "<div style='color:red;'>PlantUML Error: " + e.getMessage() + "</div>";
        }
    }

    private String getMermaidHtml(String content) {
        String theme = currentDiagramTheme;
        String boardCss = getBoardThemeCss();
        String orientation = currentOrientation.equals("L-R") ? "LR" : "TD";
        
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
               "<script src=\"" + getClass().getResource("/styles/mermaid.min.js").toExternalForm() + "\"></script>" +
               "<style>" +
               "  * { -webkit-user-select: none; user-select: none; }" +
               "  body { margin: 0; padding: 0; overflow: hidden; font-family: 'Segoe UI', sans-serif; " + boardCss + " }" +
               "  #container { width: 100vw; height: 100vh; cursor: grab; overflow: hidden; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; }" +
               "  #container:active { cursor: grabbing; }" +
               "  #viewport { transform-origin: 0 0; padding: 100px; display: inline-block; min-width: 100%; text-align: center; }" +
               "  .diagram-block { margin-bottom: 50px; padding: 30px; background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); display: inline-block; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }" +
               "  .theme-intellij-light .diagram-block { background: rgba(0,0,0,0.02); border: 1px solid rgba(0,0,0,0.1); box-shadow: 0 5px 15px rgba(0,0,0,0.1); }" +
               "</style></head><body class=\"" + (currentThemeName.equalsIgnoreCase("IntelliJ Light") ? "theme-intellij-light" : "") + "\">" +
               "<div id=\"container\"><div id=\"viewport\">" +
               renderAllMermaidBlocks(content, orientation) +
               "</div></div>" +
               "<script>" +
               "mermaid.initialize({ startOnLoad: true, theme: '" + theme + "', securityLevel: 'loose', flowchart: { useMaxWidth: false } });" +
               "var scale = 1.0, tx = 0, ty = 0, isDragging = false, lastX, lastY;" +
               "var container = document.getElementById('container'), viewport = document.getElementById('viewport');" +
               "window.addEventListener('dragstart', function(e) { e.preventDefault(); });" +
               "container.onwheel = function(e) {" +
               "  e.preventDefault();" +
               "  var rect = viewport.getBoundingClientRect();" +
               "  var mouseX = e.clientX - tx;" +
               "  var mouseY = e.clientY - ty;" +
               "  var delta = e.deltaY > 0 ? 0.9 : 1.1;" +
               "  var newScale = scale * delta;" +
               "  if (newScale > 0.1 && newScale < 10) {" +
               "    scale = newScale;" +
               "    updateTransform();" +
               "  }" +
               "};" +
               "container.onmousedown = function(e) { if(e.button === 0) { isDragging = true; lastX = e.clientX; lastY = e.clientY; e.preventDefault(); } };" +
               "window.onmousemove = function(e) {" +
               "  if (!isDragging) return;" +
               "  tx += (e.clientX - lastX); ty += (e.clientY - lastY);" +
               "  lastX = e.clientX; lastY = e.clientY;" +
               "  updateTransform();" +
               "};" +
               "window.onmouseup = function() { isDragging = false; };" +
               "function updateTransform() {" +
               "  viewport.style.transform = 'translate(' + tx + 'px,' + ty + 'px) scale(' + scale + ')';" +
               "}" +
               "window.updateDiagram = function(code) {" +
               "  viewport.innerHTML = '';" +
               "  var blocks = code.split(/```mermaid|```/);" +
               "  if (blocks.length <= 1 && code.trim().length > 0) {" +
               "    var div = document.createElement('div'); div.className = 'mermaid diagram-block'; div.textContent = code;" +
               "    viewport.appendChild(div);" +
               "  } else {" +
               "    for (var i = 1; i < blocks.length; i += 2) {" +
               "      var div = document.createElement('div'); div.className = 'mermaid diagram-block'; div.textContent = blocks[i];" +
               "      viewport.appendChild(div);" +
               "    }" +
               "  }" +
               "  try { mermaid.init(undefined, '.mermaid'); } catch(e) { console.error(e); }" +
               "};" +
               "window.updateDiagramBase64 = function(base64) {" +
               "  var code = decodeURIComponent(escape(window.atob(base64)));" +
               "  window.updateDiagram(code);" +
               "};" +
               "</script></body></html>";
    }

    private String getBoardThemeCss() {
        switch (currentBoardTheme) {
            case "Plain White":
                return "background-color: #ffffff; color: #333333;";
            case "Grey Paper":
                return "background-color: #f5f5f5; color: #333333; background-image: linear-gradient(#e0e0e0 1px, transparent 1px), linear-gradient(90deg, #e0e0e0 1px, transparent 1px); background-size: 40px 40px;";
            case "Soft Grey":
                return "background-color: #e0e0e0; color: #333333;";
            case "Dotted Grey":
                return "background-color: #f0f0f0; color: #333333; background-image: radial-gradient(#cccccc 1px, transparent 1px); background-size: 20px 20px;";
            case "Dark Dotted":
                return "background-color: #1e1e1e; color: #d4d4d4; background-image: radial-gradient(#333333 1.5px, transparent 1.5px); background-size: 30px 30px;";
            case "Clean Dark":
                return "background-color: #1e1e1e; color: #d4d4d4;";
            case "Light Grid":
                return "background-color: #fdfdfd; background-image: linear-gradient(#f0f0f0 1px, transparent 1px), linear-gradient(90deg, #f0f0f0 1px, transparent 1px); background-size: 20px 20px;";
            case "Navy Blue":
                return "background-color: #0a192f; color: #ccd6f6;";
            case "Hacker Black":
                return "background-color: #050505; color: #00ff00; background-image: linear-gradient(0deg, transparent 24%, rgba(0, 255, 0, .05) 25%, rgba(0, 255, 0, .05) 26%, transparent 27%, transparent 74%, rgba(0, 255, 0, .05) 75%, rgba(0, 255, 0, .05) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(0, 255, 0, .05) 25%, rgba(0, 255, 0, .05) 26%, transparent 27%, transparent 74%, rgba(0, 255, 0, .05) 75%, rgba(0, 255, 0, .05) 76%, transparent 77%, transparent); background-size: 50px 50px;";
            case "Blueprint":
                return "background-color: #1a4a91; color: white; background-image: linear-gradient(rgba(255,255,255,.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.1) 1px, transparent 1px); background-size: 20px 20px;";
            case "Dark Grid":
            default:
                return "background-color: #121212; background-image: radial-gradient(#333333 1px, transparent 1px); background-size: 30px 30px;";
        }
    }

    private String renderAllMermaidBlocks(String content, String orientation) {
        if (content == null || content.trim().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        // Check if content has markdown blocks
        if (content.contains("```mermaid")) {
            String[] parts = content.split("```mermaid");
            for (int i = 1; i < parts.length; i++) {
                String block = parts[i].split("```")[0];
                String processed = injectMermaidOrientation(block, orientation);
                sb.append("<div class=\"mermaid diagram-block\">").append(escapeHtml(processed)).append("</div>");
            }
        } else {
            // Assume the whole file is a single mermaid diagram
            String processed = injectMermaidOrientation(content, orientation);
            sb.append("<div class=\"mermaid diagram-block\">").append(escapeHtml(processed)).append("</div>");
        }
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String injectMermaidOrientation(String code, String orientation) {
        String trimmed = code.trim();
        if (trimmed.startsWith("graph ") || trimmed.startsWith("flowchart ")) {
            // Replace existing orientation if found
            String updated = trimmed.replaceAll("(graph|flowchart)\\s+(TD|LR|TB|BT|RL)", "$1 " + orientation);
            if (!updated.equals(trimmed)) return updated;
            
            // Or inject after first word if not found
            int firstSpace = trimmed.indexOf(" ");
            if (firstSpace != -1) {
                return trimmed.substring(0, firstSpace) + " " + orientation + " " + trimmed.substring(firstSpace + 1);
            }
        }
        return code;
    }

    private String getLanguage(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".mmd") || name.endsWith(".mermaid")) return "mermaid";
        if (name.endsWith(".puml") || name.endsWith(".plantuml")) return "plantuml";
        if (name.endsWith(".dot") || name.endsWith(".gv")) return "dot";
        if (name.endsWith(".d2")) return "d2";
        if (name.endsWith(".bpmn")) return "xml";
        return "text";
    }

    private String getDiagramType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".mmd") || name.endsWith(".mermaid")) return "mermaid";
        if (name.endsWith(".puml") || name.endsWith(".plantuml")) return "plantuml";
        if (name.endsWith(".dot") || name.endsWith(".gv")) return "graphviz";
        if (name.endsWith(".d2")) return "d2";
        if (name.endsWith(".bpmn")) return "bpmn";
        return "unknown";
    }

    private void refreshAllTabs() {
        for (Tab tab : tabPane.getTabs()) {
            File file = tabFiles.get(tab);
            if (file != null) {
                try {
                    String content = Files.readString(file.toPath());
                    loadCanvasForTab(tab, content, getDiagramType(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setupTreeContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem mnuNewFile = new MenuItem("New Diagram", new FontIcon("fas-plus-circle"));
        mnuNewFile.setOnAction(e -> createNewDiagram());

        MenuItem mnuDelete = new MenuItem("Delete", new FontIcon("fas-trash"));
        mnuDelete.setOnAction(e -> deleteSelectedFile());

        MenuItem mnuRefresh = new MenuItem("Refresh", new FontIcon("fas-sync"));
        mnuRefresh.setOnAction(e -> refreshTree());

        contextMenu.getItems().addAll(mnuNewFile, new SeparatorMenuItem(), mnuDelete, new SeparatorMenuItem(), mnuRefresh);
        treeView.setContextMenu(contextMenu);
    }

    private void deleteSelectedFile() {
        TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue().equals(workspaceRoot)) return;
        File target = selected.getValue();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + target.getName() + "?", ButtonType.YES, ButtonType.NO);
        RouteBuilderApp.themeDialog(alert);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                deleteRecursively(target);
                refreshTree();
            }
        });
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) deleteRecursively(f);
            }
        }
        file.delete();
    }

    private void createNewDiagram() {
        TextInputDialog dialog = new TextInputDialog("new-diagram.mmd");
        dialog.setTitle("New Diagram");
        dialog.setHeaderText("Enter name for new Mermaid diagram:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.endsWith(".mmd")) name += ".mmd";
            File f = new File(new File(workspaceRoot, "diagrams/mermaid"), name);
            try {
                f.getParentFile().mkdirs();
                Files.writeString(f.toPath(), "graph TD\n    A[Start] --> B[End]");
                refreshTree();
                openFile(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveCurrentTab() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab != null && tabFiles.containsKey(tab)) {
            WebEngine engine = tabEngines.get(tab);
            if (engine != null) {
                String code = (String) engine.executeScript("window.editor.getValue()");
                try {
                    Files.writeString(tabFiles.get(tab).toPath(), code);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void exportCurrentDiagramToSVG() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab != null) {
            WebEngine engine = tabCanvasEngines.get(tab);
            if (engine != null) {
                // Collect all SVGs from the viewport
                String js = "(function() {" +
                            "  var viewport = document.getElementById('viewport');" +
                            "  if (!viewport) return null;" +
                            "  var svgs = viewport.getElementsByTagName('svg');" +
                            "  if (svgs.length === 0) return null;" +
                            "  if (svgs.length === 1) return svgs[0].outerHTML;" +
                            "  var combined = '<svg xmlns=\"http://www.w3.org/2000/svg\">';" +
                            "  for (var i=0; i<svgs.length; i++) combined += svgs[i].outerHTML;" +
                            "  combined += '</svg>';" +
                            "  return combined;" +
                            "})()";
                Object result = engine.executeScript(js);
                if (result instanceof String && ((String) result).contains("<svg")) {
                    String svg = (String) result;
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Save SVG");
                    chooser.setInitialFileName(tab.getText().replaceAll("\\.[^.]+$", "") + ".svg");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SVG Files", "*.svg"));
                    File file = chooser.showSaveDialog(stage);
                    if (file != null) {
                        try {
                            Files.writeString(file.toPath(), svg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "No rendered diagram found to export.");
                    alert.showAndWait();
                }
            }
        }
    }

    private void applyZoom(double factor) {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab != null) {
            WebEngine engine = tabCanvasEngines.get(tab);
            if (engine != null) {
                engine.executeScript("if(typeof scale !== 'undefined') scale *= " + factor + "; if(window.updateTransform) updateTransform();");
            }
        }
    }

    private void resetZoom() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab != null) {
            WebEngine engine = tabCanvasEngines.get(tab);
            if (engine != null) {
                engine.executeScript("if(typeof scale !== 'undefined') scale = 1.0; if(typeof tx !== 'undefined') tx = 0; if(typeof ty !== 'undefined') ty = 0; if(window.updateTransform) updateTransform();");
            }
        }
    }

    private void swapPanels() {
        if ("Code Left".equals(currentLayoutMode)) {
            currentLayoutMode = "Code Right";
        } else if ("Code Right".equals(currentLayoutMode)) {
            currentLayoutMode = "Code Left";
        } else if ("Code Top".equals(currentLayoutMode)) {
            currentLayoutMode = "Code Bottom";
        } else if ("Code Bottom".equals(currentLayoutMode)) {
            currentLayoutMode = "Code Top";
        }
        
        if (layoutModeBox != null) {
            layoutModeBox.setValue(currentLayoutMode);
        }
        
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
        prefs.put("layoutMode", currentLayoutMode);
        updateAllTabsLayout();
    }

    private void updateAllTabsLayout() {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getContent() instanceof SplitPane) {
                SplitPane sp = (SplitPane) tab.getContent();
                WebView editor = tabEditorViews.get(tab);
                WebView canvas = tabCanvasViews.get(tab);
                if (editor != null && canvas != null) {
                    applyLayoutToSplitPane(sp, editor, canvas);
                }
            }
        }
    }

    private void forceRenderActiveTab() {
        Tab activeTab = tabPane.getSelectionModel().getSelectedItem();
        if (activeTab == null) return;
        
        WebEngine editorEngine = tabEngines.get(activeTab);
        if (editorEngine == null) return;
        
        try {
            String currentCode = (String) editorEngine.executeScript("window.editor.getValue();");
            if (currentCode != null) {
                File file = tabFiles.get(activeTab);
                if (file != null) {
                    String type = getDiagramType(file);
                    if ("plantuml".equalsIgnoreCase(type)) {
                        String svg = renderPlantUmlToSvg(currentCode);
                        WebEngine canvasEngine = tabCanvasEngines.get(activeTab);
                        if (canvasEngine != null) {
                            String base64 = java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            canvasEngine.executeScript("if(window.updateSvgBase64) window.updateSvgBase64(\"" + base64 + "\");");
                        }
                    } else {
                        WebEngine canvasEngine = tabCanvasEngines.get(activeTab);
                        if (canvasEngine != null) {
                            String base64 = java.util.Base64.getEncoder().encodeToString(currentCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            canvasEngine.executeScript("if(window.updateDiagramBase64) window.updateDiagramBase64(\"" + base64 + "\");");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public void setTheme(String themeName) {
        this.currentThemeName = themeName;
        String themeClass = "theme-" + themeName.toLowerCase().replace(" ", "-");
        String monacoTheme = themeName.equalsIgnoreCase("IntelliJ Light") ? "vs" : "vs-dark";
        Platform.runLater(() -> {
            if (studioThemeBox != null) studioThemeBox.setValue(themeName);
            for (WebEngine engine : tabEngines.values()) {
                engine.executeScript("if(window.setTheme) window.setTheme('" + monacoTheme + "');");
            }
            // Update Mermaid theme as well if needed
        });
    }
}
