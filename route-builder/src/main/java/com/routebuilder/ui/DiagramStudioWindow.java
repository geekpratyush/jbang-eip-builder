package com.routebuilder.ui;

import javafx.application.Platform;
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
    
    private ComboBox<String> studioThemeBox;
    private ComboBox<String> diagramThemeBox;
    private String currentThemeName = RouteBuilderApp.currentThemeName;
    private String currentDiagramTheme = "default";

    private static final String[] DIAGRAM_THEMES = {
        "default", "base", "forest", "dark", "neutral", "vibrant", "corporate", "ocean", "sunset", "midnight"
    };

    private ComboBox<String> boardThemeBox;
    private String currentBoardTheme = "Dark Grid";
    private static final String[] BOARD_THEMES = { 
        "Plain White", "Grey Paper", "Soft Grey", "Dotted Grey", "Light Grid", 
        "Dark Grid", "Dark Dotted", "Clean Dark", "Navy Blue", "Hacker Black", "Blueprint" 
    };

    public DiagramStudioWindow() {
        activeInstances.add(this);
        this.stage = new Stage();
        
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
        String savedRoot = prefs.get("workspaceRoot", new File(System.getProperty("user.dir"), "diagram-workspace").getAbsolutePath());
        this.workspaceRoot = new File(savedRoot);
        
        this.currentDiagramTheme = prefs.get("diagramTheme", "default");
        this.currentBoardTheme = prefs.get("boardTheme", "Dark Grid");

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

        studioThemeBox = new ComboBox<>();
        studioThemeBox.getItems().addAll("VSCode Dark", "IntelliJ Light", "Dracula", "Monokai", "Hacker");
        studioThemeBox.setValue(RouteBuilderApp.currentThemeName);
        studioThemeBox.setOnAction(e -> RouteBuilderApp.setGlobalTheme(studioThemeBox.getValue()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(btnNew, btnOpenWorkspace, btnSave, btnExport, new Separator(), btnZoomIn, btnZoomOut, btnZoomReset, new Separator(), 
            new Label("Board:"), boardThemeBox, new Label("Theme:"), diagramThemeBox, new Separator(), spacer, studioThemeBox);
        root.setTop(toolBar);

        // --- Sidebar ---
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(8));
        sidebar.setSpacing(5);
        sidebar.getStyleClass().add("studio-sidebar");

        Label lblExplorer = new Label("DIAGRAM EXPLORER");
        lblExplorer.getStyleClass().add("studio-explorer-label");

        treeView = new TreeView<>();
        treeView.getStyleClass().add("sidebar-tree-view");
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<File> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null && item.getValue().isFile()) {
                    openFile(item.getValue());
                }
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
                    if (item.isDirectory()) {
                        setGraphic(new FontIcon("fas-folder"));
                    } else {
                        String name = item.getName().toLowerCase();
                        if (name.endsWith(".mmd") || name.endsWith(".mermaid")) {
                            setGraphic(new FontIcon("fas-project-diagram")); // Mermaid
                        } else if (name.endsWith(".puml") || name.endsWith(".plantuml")) {
                            setGraphic(new FontIcon("fas-sitemap")); // PlantUML
                        } else if (name.endsWith(".dot") || name.endsWith(".gv")) {
                            setGraphic(new FontIcon("fas-network-wired")); // Graphviz
                        } else {
                            setGraphic(new FontIcon("fas-file-image"));
                        }
                    }
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

        Tab tab = new Tab(file.getName());
        openTabs.put(file, tab);
        tabFiles.put(tab, file);
        
        SplitPane tabSplit = new SplitPane();
        tabSplit.setOrientation(Orientation.HORIZONTAL);

        WebView canvasView = new WebView();
        WebView editorView = new WebView();
        
        tabSplit.getItems().addAll(canvasView, editorView);
        tabSplit.setDividerPositions(0.5);

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
        });
    }

    private void loadMonacoForTab(Tab tab, String content, String language) {
        WebEngine engine = tabEngines.get(tab);
        engine.loadContent(getMonacoHtml(content, language));
        
        engine.getLoadWorker().stateProperty().addListener((obs, old, newVal) -> {
            if (newVal == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new DiagramBridge(tab));
            }
        });
    }

    private void loadCanvasForTab(Tab tab, String content, String type) {
        WebEngine engine = tabCanvasEngines.get(tab);
        engine.loadContent(getCanvasHtml(content, type));
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
                        canvasEngine.executeScript("if(window.updateDiagram) window.updateDiagram(\"" + newCode.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\");");
                    }
                }
            });
        }

        public void requestPlantUmlRender(String code) {
            String svg = renderPlantUmlToSvg(code);
            Platform.runLater(() -> {
                WebEngine canvasEngine = tabCanvasEngines.get(tab);
                if (canvasEngine != null) {
                    canvasEngine.executeScript("if(window.updateSvg) window.updateSvg(\"" + svg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\");");
                }
            });
        }
    }

    private String getMonacoHtml(String content, String language) {
        String monacoTheme = currentThemeName.equalsIgnoreCase("IntelliJ Light") ? "vs" : "vs-dark";
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
               "<style>body { margin: 0; padding: 0; overflow: hidden; background-color: #1e1e1e; } #editor { width: 100vw; height: 100vh; }</style>" +
               "</head><body><div id=\"editor\"></div>" +
               "<script src=\"" + getClass().getResource("/monaco/vs/loader.js").toExternalForm() + "\"></script>" +
               "<script>" +
               "require.config({ paths: { vs: '" + getClass().getResource("/monaco/vs").toExternalForm() + "' }});" +
               "require(['vs/editor/editor.main'], function() {" +
               "  window.editor = monaco.editor.create(document.getElementById('editor'), {" +
               "    value: `" + content + "`," +
               "    language: '" + language + "'," +
               "    theme: '" + monacoTheme + "'," +
               "    automaticLayout: true" +
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
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
               "<script src=\"" + getClass().getResource("/styles/viz.js").toExternalForm() + "\"></script>" +
               "<script src=\"" + getClass().getResource("/styles/lite.render.js").toExternalForm() + "\"></script>" +
               "<style>" +
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
               "function render(code) {" +
               "  if (!code || code.trim().length === 0) return;" +
               "  viz.renderSVGElement(code).then(function(element) {" +
               "    viewport.innerHTML = ''; var div = document.createElement('div'); div.className='diagram-block'; div.appendChild(element); viewport.appendChild(div);" +
               "  }).catch(error => { viewport.innerHTML = '<div style=\"color:red\">Graphviz Error: ' + error + '</div>'; });" +
               "}" +
               "container.onwheel = function(e) { e.preventDefault(); var delta = e.deltaY > 0 ? 0.9 : 1.1; scale *= delta; updateTransform(); };" +
               "container.onmousedown = function(e) { if(e.button === 0) { isDragging = true; lastX = e.clientX; lastY = e.clientY; } };" +
               "window.onmousemove = function(e) { if (!isDragging) return; tx += (e.clientX - lastX); ty += (e.clientY - lastY); lastX = e.clientX; lastY = e.clientY; updateTransform(); };" +
               "window.onmouseup = function() { isDragging = false; };" +
               "function updateTransform() { viewport.style.transform = 'translate(' + tx + 'px,' + ty + 'px) scale(' + scale + ')'; }" +
               "window.updateDiagram = function(code) { render(code); };" +
               "render(`" + content.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$") + "`);" +
               "</script></body></html>";
    }

    private String getPlantUmlHtml(String content) {
        String boardCss = getBoardThemeCss();
        String svg = renderPlantUmlToSvg(content);
        
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
               "<style>" +
               "  body { margin: 0; padding: 0; overflow: hidden; font-family: 'Segoe UI', sans-serif; " + boardCss + " }" +
               "  #container { width: 100vw; height: 100vh; cursor: grab; overflow: hidden; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; }" +
               "  #container:active { cursor: grabbing; }" +
               "  #viewport { transform-origin: 0 0; padding: 100px; display: inline-block; min-width: 100%; text-align: center; }" +
               "  .diagram-block { margin-bottom: 50px; padding: 30px; background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); display: inline-block; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }" +
               "  .theme-intellij-light .diagram-block { background: rgba(0,0,0,0.02); border: 1px solid rgba(0,0,0,0.1); box-shadow: 0 5px 15px rgba(0,0,0,0.1); }" +
               "  svg { max-width: 100%; height: auto; }" +
               "</style></head><body class=\"" + (currentThemeName.equalsIgnoreCase("IntelliJ Light") ? "theme-intellij-light" : "") + "\">" +
               "<div id=\"container\"><div id=\"viewport\">" +
               "<div class=\"diagram-block\">" + svg + "</div>" +
               "</div></div>" +
               "<script>" +
               "var scale = 1.0, tx = 0, ty = 0, isDragging = false, lastX, lastY;" +
               "var container = document.getElementById('container'), viewport = document.getElementById('viewport');" +
               "container.onwheel = function(e) {" +
               "  e.preventDefault();" +
               "  var delta = e.deltaY > 0 ? 0.9 : 1.1;" +
               "  scale *= delta;" +
               "  updateTransform();" +
               "};" +
               "container.onmousedown = function(e) { if(e.button === 0) { isDragging = true; lastX = e.clientX; lastY = e.clientY; } };" +
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
               "  // For PlantUML, we need to call back to Java to re-render SVG" +
               "  if(window.javaBridge) window.javaBridge.requestPlantUmlRender(code);" +
               "};" +
               "window.updateSvg = function(svg) {" +
               "  viewport.innerHTML = '<div class=\"diagram-block\">' + svg + '</div>';" +
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
            
            // Fix C4 includes to look locally if they are remote
            processed = processed.replaceAll("!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/", "!include " + new File(workspaceRoot, "libraries/c4/").getAbsolutePath().replace("\\", "/") + "/");

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
        
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
               "<script src=\"" + getClass().getResource("/styles/mermaid.min.js").toExternalForm() + "\"></script>" +
               "<style>" +
               "  body { margin: 0; padding: 0; overflow: hidden; font-family: 'Segoe UI', sans-serif; " + boardCss + " }" +
               "  #container { width: 100vw; height: 100vh; cursor: grab; overflow: hidden; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; }" +
               "  #container:active { cursor: grabbing; }" +
               "  #viewport { transform-origin: 0 0; padding: 100px; display: inline-block; min-width: 100%; text-align: center; }" +
               "  .diagram-block { margin-bottom: 50px; padding: 30px; background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); display: inline-block; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }" +
               "  .theme-intellij-light .diagram-block { background: rgba(0,0,0,0.02); border: 1px solid rgba(0,0,0,0.1); box-shadow: 0 5px 15px rgba(0,0,0,0.1); }" +
               "</style></head><body class=\"" + (currentThemeName.equalsIgnoreCase("IntelliJ Light") ? "theme-intellij-light" : "") + "\">" +
               "<div id=\"container\"><div id=\"viewport\">" +
               renderAllMermaidBlocks(content) +
               "</div></div>" +
               "<script>" +
               "mermaid.initialize({ startOnLoad: true, theme: '" + theme + "', securityLevel: 'loose', flowchart: { useMaxWidth: false } });" +
               "var scale = 1.0, tx = 0, ty = 0, isDragging = false, lastX, lastY;" +
               "var container = document.getElementById('container'), viewport = document.getElementById('viewport');" +
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
               "container.onmousedown = function(e) { if(e.button === 0) { isDragging = true; lastX = e.clientX; lastY = e.clientY; } };" +
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
               "    var div = document.createElement('div'); div.className = 'mermaid diagram-block'; div.innerHTML = code;" +
               "    viewport.appendChild(div);" +
               "  } else {" +
               "    for (var i = 1; i < blocks.length; i += 2) {" +
               "      var div = document.createElement('div'); div.className = 'mermaid diagram-block'; div.innerHTML = blocks[i];" +
               "      viewport.appendChild(div);" +
               "    }" +
               "  }" +
               "  try { mermaid.init(undefined, '.mermaid'); } catch(e) { console.error(e); }" +
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

    private String renderAllMermaidBlocks(String content) {
        if (content == null || content.trim().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        // Check if content has markdown blocks
        if (content.contains("```mermaid")) {
            String[] parts = content.split("```mermaid");
            for (int i = 1; i < parts.length; i++) {
                String block = parts[i].split("```")[0];
                sb.append("<div class=\"mermaid diagram-block\">").append(block).append("</div>");
            }
        } else {
            // Assume the whole file is a single mermaid diagram
            sb.append("<div class=\"mermaid diagram-block\">").append(content).append("</div>");
        }
        return sb.toString();
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
        mnuDelete.setOnAction(e -> {
            TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue().equals(workspaceRoot)) return;
            File target = selected.getValue();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + target.getName() + "?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    deleteRecursively(target);
                    refreshTree();
                }
            });
        });

        MenuItem mnuRefresh = new MenuItem("Refresh", new FontIcon("fas-sync"));
        mnuRefresh.setOnAction(e -> refreshTree());

        contextMenu.getItems().addAll(mnuNewFile, new SeparatorMenuItem(), mnuDelete, new SeparatorMenuItem(), mnuRefresh);
        treeView.setContextMenu(contextMenu);
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
                engine.executeScript("scale *= " + factor + "; updateTransform();");
            }
        }
    }

    private void resetZoom() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab != null) {
            WebEngine engine = tabCanvasEngines.get(tab);
            if (engine != null) {
                engine.executeScript("scale = 1.0; tx = 0; ty = 0; updateTransform();");
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
