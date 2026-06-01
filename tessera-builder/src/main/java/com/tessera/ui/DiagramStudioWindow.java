package com.tessera.ui;

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
    private BorderPane root;
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
    private Button btnLayoutMode;
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
        this(null);
    }

    public DiagramStudioWindow(File baseWorkspace) {
        activeInstances.add(this);
        this.stage = new Stage();
        
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
        
        if (baseWorkspace != null) {
            this.workspaceRoot = new File(baseWorkspace, "diagrams");
        } else {
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

        this.root = new BorderPane();
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

        btnLayoutMode = new Button();
        btnLayoutMode.getStyleClass().add("toolbar-btn");
        updateLayoutModeIcon(btnLayoutMode);
        btnLayoutMode.setOnAction(e -> {
            if ("Code Left".equals(currentLayoutMode)) {
                currentLayoutMode = "Code Top";
            } else if ("Code Top".equals(currentLayoutMode)) {
                currentLayoutMode = "Code Right";
            } else if ("Code Right".equals(currentLayoutMode)) {
                currentLayoutMode = "Code Bottom";
            } else {
                currentLayoutMode = "Code Left";
            }
            updateLayoutModeIcon(btnLayoutMode);
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

        Button btnHelp = new Button("Help Guide", new FontIcon("fas-question-circle"));
        btnHelp.getStyleClass().add("toolbar-btn");
        btnHelp.setOnAction(e -> new RouteBuilderHelpWindow("Route Designer & Sync", "Diagram").show());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(
            btnNew, btnOpenWorkspace, new Separator(),
            btnSave, btnRefreshWorkspace, btnSwap, btnRender, new Separator(),
            btnExport, new Separator(),
            btnZoomIn, btnZoomOut, btnZoomReset, new Separator(),
            new Label("Theme:"), diagramThemeBox, 
            new Label("Board:"), boardThemeBox,
            new Label("Layout:"), btnLayoutMode,
            btnOrientation, new Separator(),
            spacer, btnHelp, new Separator(), studioThemeBox
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
            } else if (code == KeyCode.C && event.isShortcutDown()) {
                copySelectedFile();
                event.consume();
            } else if (code == KeyCode.X && event.isShortcutDown()) {
                cutSelectedFile();
                event.consume();
            } else if (code == KeyCode.V && event.isShortcutDown()) {
                pasteCopiedFile();
                event.consume();
            }
        });
        
        treeView.setCellFactory(tv -> {
            TreeCell<File> cell = new TreeCell<>() {
                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setTooltip(null);
                    } else {
                        setText(item.getName());
                        setGraphic(RouteBuilderApp.getFileIcon(item));
                        setTooltip(new Tooltip(item.getAbsolutePath()));
                    }
                }
            };

            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty() && cell.getItem() != null && cell.getItem().isFile()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putFiles(Collections.singletonList(cell.getItem()));
                    db.setContent(content);
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasFiles()) {
                    File target = cell.getItem();
                    if (target == null || target.isDirectory() || (target.isFile() && target.getParentFile() != null)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }
                event.consume();
            });

            cell.setOnDragEntered(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasFiles()) {
                    File target = cell.getItem();
                    if (target != null && target.isDirectory()) {
                        cell.setStyle("-fx-background-color: -sui-selection; -fx-text-fill: white;");
                    } else if (target != null && target.isFile()) {
                        cell.setStyle("-fx-border-color: -sui-accent-primary; -fx-border-width: 0 0 2px 0;");
                    }
                }
                event.consume();
            });

            cell.setOnDragExited(event -> {
                cell.setStyle("");
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    File sourceFile = db.getFiles().get(0);
                    File target = cell.getItem();
                    File destDir = workspaceRoot;
                    if (target != null) {
                        if (target.isDirectory()) {
                            destDir = target;
                        } else {
                            destDir = target.getParentFile();
                        }
                    }
                    if (sourceFile.exists() && destDir.isDirectory()) {
                        File destFile = new File(destDir, sourceFile.getName());
                        try {
                            if (!destFile.getAbsolutePath().equals(sourceFile.getAbsolutePath())) {
                                Files.move(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                
                                Tab openTab = openTabs.remove(sourceFile);
                                if (openTab != null) {
                                    openTabs.put(destFile, openTab);
                                    tabFiles.put(openTab, destFile);
                                    final File finalDest = destFile;
                                    Platform.runLater(() -> {
                                        openTab.setText(finalDest.getName());
                                        openTab.setTooltip(new Tooltip(finalDest.getAbsolutePath()));
                                    });
                                }
                                success = true;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                event.setDropCompleted(success);
                event.consume();
                if (success) {
                    Platform.runLater(this::refreshTree);
                }
            });

            return cell;
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

        root.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        root.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                for (File file : db.getFiles()) {
                    if (file.isFile() && (file.getName().endsWith(".mermaid") || file.getName().endsWith(".mmd") || 
                                          file.getName().endsWith(".puml") || file.getName().endsWith(".plantuml") || 
                                          file.getName().endsWith(".dot") || file.getName().endsWith(".gv"))) {
                        Platform.runLater(() -> openFile(file));
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        com.tessera.ui.components.ThemeManager.registerRoot(root);
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
        Set<String> expandedPaths = new HashSet<>();
        if (treeView.getRoot() != null) {
            collectExpandedPaths(treeView.getRoot(), expandedPaths);
        }

        TreeItem<File> rootItem = new TreeItem<>(workspaceRoot);
        rootItem.setExpanded(true);
        buildFileTree(workspaceRoot, rootItem);
        
        restoreExpandedStates(rootItem, expandedPaths);
        treeView.setRoot(rootItem);
    }

    private void collectExpandedPaths(TreeItem<File> item, Set<String> expandedPaths) {
        if (item != null && item.isExpanded() && item.getValue() != null) {
            expandedPaths.add(item.getValue().getAbsolutePath());
            for (TreeItem<File> child : item.getChildren()) {
                collectExpandedPaths(child, expandedPaths);
            }
        }
    }

    private void restoreExpandedStates(TreeItem<File> item, Set<String> expandedPaths) {
        if (item != null && item.getValue() != null) {
            if (expandedPaths.contains(item.getValue().getAbsolutePath())) {
                item.setExpanded(true);
            }
            for (TreeItem<File> child : item.getChildren()) {
                restoreExpandedStates(child, expandedPaths);
            }
        }
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
        tab.setTooltip(new Tooltip(file.getAbsolutePath()));
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
        
        VBox.setVgrow(editorView, Priority.ALWAYS);
        editorBox.getChildren().addAll(editorView);
        
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
        String activeTheme = RouteBuilderApp.currentThemeName;
        
        String monacoBase = getClass().getResource("/monaco/vs/loader.js").toExternalForm();
        monacoBase = monacoBase.substring(0, monacoBase.lastIndexOf("/vs/loader.js"));

        String html = getMonacoHtml(content, language);
        
        engine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new DiagramBridge(tab));
                
                String mTheme = getMonacoThemeName(activeTheme);
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
               "<script src=\"" + getClass().getResource("/styles/mermaid.min.js").toExternalForm() + "\"></script>" +
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
               "  function validateCode(code, lang) {\n" +
               "    var markers = [];\n" +
               "    if (lang === 'mermaid' && window.mermaid) {\n" +
               "      try {\n" +
               "        window.mermaid.parse(code);\n" +
               "      } catch(e) {\n" +
               "        var line = 1;\n" +
               "        var msg = e.message || 'Syntax Error';\n" +
               "        var m = msg.match(/line\\\\s+(\\\\d+)/i) || msg.match(/on line\\\\s+(\\\\d+)/i);\n" +
               "        if (m) line = parseInt(m[1]);\n" +
               "        markers.push({\n" +
               "          severity: monaco.MarkerSeverity.Error,\n" +
               "          message: msg,\n" +
               "          startLineNumber: line,\n" +
               "          startColumn: 1,\n" +
               "          endLineNumber: line,\n" +
               "          endColumn: 100\n" +
               "        });\n" +
               "      }\n" +
               "    } else {\n" +
               "      var lines = code.split('\\\\n');\n" +
               "      var openBraces = [];\n" +
               "      var openBrackets = [];\n" +
               "      var inQuotes = false;\n" +
               "      for (var idx = 0; idx < lines.length; idx++) {\n" +
               "        var lineText = lines[idx];\n" +
               "        var lineNum = idx + 1;\n" +
               "        var quoteCount = (lineText.match(/\\\"/g) || []).length;\n" +
               "        if (quoteCount % 2 !== 0) {\n" +
               "          markers.push({\n" +
               "            severity: monaco.MarkerSeverity.Warning,\n" +
               "            message: 'Unmatched double quotes on this line',\n" +
               "            startLineNumber: lineNum,\n" +
               "            startColumn: 1,\n" +
               "            endLineNumber: lineNum,\n" +
               "            endColumn: lineText.length + 1\n" +
               "          });\n" +
               "        }\n" +
               "        for (var charIdx = 0; charIdx < lineText.length; charIdx++) {\n" +
               "          var c = lineText.charAt(charIdx);\n" +
               "          if (c === '\\\"') {\n" +
               "            inQuotes = !inQuotes;\n" +
               "          }\n" +
               "          if (inQuotes) continue;\n" +
               "          if (c === '{') {\n" +
               "            openBraces.push({ line: lineNum, col: charIdx + 1 });\n" +
               "          } else if (c === '}') {\n" +
               "            if (openBraces.length === 0) {\n" +
               "              markers.push({\n" +
               "                severity: monaco.MarkerSeverity.Error,\n" +
               "                message: 'Unmatched closing brace }',\n" +
               "                startLineNumber: lineNum,\n" +
               "                startColumn: charIdx + 1,\n" +
               "                endLineNumber: lineNum,\n" +
               "                endColumn: charIdx + 2\n" +
               "              });\n" +
               "            } else {\n" +
               "              openBraces.pop();\n" +
               "            }\n" +
               "          } else if (c === '[') {\n" +
               "            openBrackets.push({ line: lineNum, col: charIdx + 1 });\n" +
               "          } else if (c === ']') {\n" +
               "            if (openBrackets.length === 0) {\n" +
               "              markers.push({\n" +
               "                severity: monaco.MarkerSeverity.Error,\n" +
               "                message: 'Unmatched closing bracket ]',\n" +
               "                startLineNumber: lineNum,\n" +
               "                startColumn: charIdx + 1,\n" +
               "                endLineNumber: lineNum,\n" +
               "                endColumn: charIdx + 2\n" +
               "              });\n" +
               "            } else {\n" +
               "              openBrackets.pop();\n" +
               "            }\n" +
               "          }\n" +
               "        }\n" +
               "      }\n" +
               "      while (openBraces.length > 0) {\n" +
               "        var b = openBraces.pop();\n" +
               "        markers.push({\n" +
               "          severity: monaco.MarkerSeverity.Error,\n" +
               "          message: 'Unclosed opening brace {',\n" +
               "          startLineNumber: b.line,\n" +
               "          startColumn: b.col,\n" +
               "          endLineNumber: b.line,\n" +
               "          endColumn: b.col + 1\n" +
               "        });\n" +
               "      }\n" +
               "      while (openBrackets.length > 0) {\n" +
               "        var br = openBrackets.pop();\n" +
               "        markers.push({\n" +
               "          severity: monaco.MarkerSeverity.Error,\n" +
               "          message: 'Unclosed opening bracket [',\n" +
               "          startLineNumber: br.line,\n" +
               "          startColumn: br.col,\n" +
               "          endLineNumber: br.line,\n" +
               "          endColumn: br.col + 1\n" +
               "        });\n" +
               "      }\n" +
               "    }\n" +
               "    monaco.editor.setModelMarkers(window.editor.getModel(), 'diagram', markers);\n" +
               "  }\n" +
               "  validateCode(window.editor.getValue(), '" + language + "');\n" +
               "  window.editor.onDidChangeModelContent(function() {" +
               "    var code = window.editor.getValue();" +
               "    validateCode(code, '" + language + "');" +
               "    if(window.javaBridge) window.javaBridge.onContentChanged(code);" +
               "  });" +
               "  window.setTheme = function(t) { monaco.editor.setTheme(t); };" +
               "});" +
               "</script></body></html>";
    }

    private String getMonacoThemeName(String themeName) {
        if (themeName == null) return "theme-vscode-dark";
        switch (themeName) {
            case "IntelliJ Light": return "theme-intellij-light";
            case "Dracula": return "theme-dracula";
            case "Monokai": return "theme-monokai";
            case "Hacker": return "theme-hacker";
            case "VSCode Dark":
            default:
                return "theme-vscode-dark";
        }
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
        boolean isLightBoard = currentBoardTheme.contains("Light") || currentBoardTheme.contains("White") || currentBoardTheme.contains("Grey");
        String textColor = isLightBoard ? "#333333" : "#aaaaaa";
        return "<html><body style='margin: 0; padding: 0; overflow: hidden; font-family: sans-serif; " + boardCss + " color: " + textColor + "; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0;'>" +
               "<div style='text-align: center;'><h2>" + type.toUpperCase() + " Rendering</h2><p>Please edit the code on the right.<br/>Native rendering for " + type + " coming soon.</p></div></body></html>";
    }

    private String getGraphvizHtml(String content) {
        String boardCss = getBoardThemeCss();
        String processedCode = injectGraphvizOrientation(content, currentOrientation);

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
        String processed = injectPlantUmlOrientation(content, currentOrientation);
        String svg = renderPlantUmlToSvg(processed);
        String base64Svg = java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        boolean isLightBoard = currentBoardTheme.contains("Light") || currentBoardTheme.contains("White") || currentBoardTheme.contains("Grey");
        
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
               "</style></head><body class=\"" + (isLightBoard ? "theme-intellij-light" : "") + "\">" +
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
        
        boolean isLightBoard = currentBoardTheme.contains("Light") || currentBoardTheme.contains("White") || currentBoardTheme.contains("Grey");
        
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
               "</style></head><body class=\"" + (isLightBoard ? "theme-intellij-light" : "") + "\">" +
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
        String[] lines = code.split("\\r?\\n");
        boolean modified = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("graph ") || line.startsWith("flowchart ")) {
                String updated = line.replaceAll("(graph|flowchart)\\s+(TD|LR|TB|BT|RL)", "$1 " + orientation);
                if (updated.equals(line)) {
                    int spaceIdx = line.indexOf(" ");
                    lines[i] = line.substring(0, spaceIdx) + " " + orientation + " " + line.substring(spaceIdx + 1);
                } else {
                    lines[i] = updated;
                }
                modified = true;
                break;
            }
        }
        if (modified) {
            return String.join("\n", lines);
        }
        return code;
    }

    private String injectPlantUmlOrientation(String code, String orientation) {
        String processed = code;
        if ("L-R".equals(orientation)) {
            processed = processed.replace("top to bottom direction", "");
            if (!processed.contains("left to right direction")) {
                processed = processed.replace("@startuml", "@startuml\nleft to right direction");
            }
        } else {
            processed = processed.replace("left to right direction", "");
            if (!processed.contains("top to bottom direction")) {
                processed = processed.replace("@startuml", "@startuml\ntop to bottom direction");
            }
        }
        return processed;
    }

    private String injectGraphvizOrientation(String code, String orientation) {
        String dir = "L-R".equals(orientation) ? "LR" : "TB";
        String processed = code;
        if (processed.contains("rankdir")) {
            processed = processed.replaceAll("rankdir\\s*=\\s*\"?[A-Z]{2}\"?", "rankdir=" + dir);
        } else {
            int braceIdx = processed.indexOf("{");
            if (braceIdx != -1) {
                processed = processed.substring(0, braceIdx + 1) + "\n  rankdir=" + dir + ";\n" + processed.substring(braceIdx + 1);
            }
        }
        return processed;
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

    private File clipboardFile = null;
    private boolean isCutOperation = false;

    private void copySelectedFile() {
        TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue().isFile()) {
            clipboardFile = selected.getValue();
            isCutOperation = false;
        }
    }

    private void cutSelectedFile() {
        TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue().isFile()) {
            clipboardFile = selected.getValue();
            isCutOperation = true;
        }
    }

    private void pasteCopiedFile() {
        if (clipboardFile == null || !clipboardFile.exists()) return;
        
        File targetDir = workspaceRoot;
        TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            File selectedFile = selected.getValue();
            if (selectedFile.isDirectory()) {
                targetDir = selectedFile;
            } else {
                targetDir = selectedFile.getParentFile();
            }
        }

        File destFile = new File(targetDir, clipboardFile.getName());
        if (destFile.getAbsolutePath().equals(clipboardFile.getAbsolutePath())) {
            String baseName = clipboardFile.getName();
            String nameWithoutExt = baseName;
            String ext = "";
            int dotIdx = baseName.lastIndexOf('.');
            if (dotIdx > 0) {
                nameWithoutExt = baseName.substring(0, dotIdx);
                ext = baseName.substring(dotIdx);
            }
            int count = 1;
            do {
                destFile = new File(targetDir, nameWithoutExt + " - Copy (" + count + ")" + ext);
                count++;
            } while (destFile.exists());
        }

        try {
            if (isCutOperation) {
                Files.move(clipboardFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                Tab openTab = openTabs.remove(clipboardFile);
                if (openTab != null) {
                    openTabs.put(destFile, openTab);
                    tabFiles.put(openTab, destFile);
                    final File finalDest = destFile;
                    Platform.runLater(() -> {
                        openTab.setText(finalDest.getName());
                        openTab.setTooltip(new Tooltip(finalDest.getAbsolutePath()));
                    });
                }
                
                clipboardFile = null;
                isCutOperation = false;
            } else {
                Files.copy(clipboardFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            refreshTree();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to copy/move file: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void setupTreeContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem mnuNewFile = new MenuItem("New Diagram", new FontIcon("fas-plus-circle"));
        mnuNewFile.setOnAction(e -> createNewDiagram());

        MenuItem mnuCopy = new MenuItem("Copy", new FontIcon("fas-copy"));
        mnuCopy.setOnAction(e -> copySelectedFile());

        MenuItem mnuCut = new MenuItem("Cut", new FontIcon("fas-cut"));
        mnuCut.setOnAction(e -> cutSelectedFile());

        MenuItem mnuPaste = new MenuItem("Paste", new FontIcon("fas-paste"));
        mnuPaste.setOnAction(e -> pasteCopiedFile());

        MenuItem mnuDelete = new MenuItem("Delete", new FontIcon("fas-trash"));
        mnuDelete.setOnAction(e -> deleteSelectedFile());

        MenuItem mnuRefresh = new MenuItem("Refresh", new FontIcon("fas-sync"));
        mnuRefresh.setOnAction(e -> refreshTree());

        MenuItem mnuProperties = new MenuItem("Properties", new FontIcon("fas-info-circle"));
        mnuProperties.setOnAction(e -> showFileProperties());

        contextMenu.getItems().addAll(
            mnuNewFile, new SeparatorMenuItem(),
            mnuCopy, mnuCut, mnuPaste, new SeparatorMenuItem(),
            mnuDelete, new SeparatorMenuItem(),
            mnuRefresh, new SeparatorMenuItem(),
            mnuProperties
        );
        treeView.setContextMenu(contextMenu);
    }

    private void showFileProperties() {
        TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        File file = selected.getValue();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File Properties - " + file.getName());
        alert.setHeaderText("Diagram File Information");
        
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        alert.getDialogPane().getStyleClass().addAll("custom-dialog-pane", RouteBuilderApp.currentThemeClass);
        
        String type = getDiagramType(file);
        String readableType = type;
        if ("mermaid".equalsIgnoreCase(type)) readableType = "Mermaid Flowchart (.mermaid)";
        else if ("plantuml".equalsIgnoreCase(type)) readableType = "PlantUML Diagram (.puml)";
        else if ("graphviz".equalsIgnoreCase(type)) readableType = "Graphviz Graph (.dot)";
        
        String sizeStr = file.length() + " bytes";
        String lastModified = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified()));
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 20, 10, 10));
        
        grid.add(new Label("File Name:"), 0, 0);
        Label lblName = new Label(file.getName());
        lblName.setStyle("-fx-font-weight: bold;");
        grid.add(lblName, 1, 0);
        
        grid.add(new Label("Diagram Type:"), 0, 1);
        grid.add(new Label(readableType), 1, 1);
        
        grid.add(new Label("File Size:"), 0, 2);
        grid.add(new Label(sizeStr), 1, 2);
        
        grid.add(new Label("Last Modified:"), 0, 3);
        grid.add(new Label(lastModified), 1, 3);
        
        grid.add(new Label("Saved Location:"), 0, 4);
        TextField pathField = new TextField(file.getAbsolutePath());
        pathField.setEditable(false);
        pathField.setPrefWidth(350);
        pathField.getStyleClass().add("properties-path-field");
        grid.add(pathField, 1, 4);
        
        alert.getDialogPane().setContent(grid);
        alert.showAndWait();
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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New Diagram");
        dialog.setHeaderText("Specify details for the new diagram:");
        
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("custom-dialog-pane");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField("my-new-diagram.mermaid");
        nameField.setPromptText("Filename (e.g. billing-flow)");
        nameField.setPrefWidth(200);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Mermaid Flowchart", "PlantUML Container", "Graphviz Directed Graph");
        typeBox.setValue("Mermaid Flowchart");

        typeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            String currentName = nameField.getText().trim();
            String baseName = currentName.replaceAll("(?i)\\.(mermaid|mmd|puml|plantuml|dot|gv)$", "");
            if (baseName.isEmpty()) baseName = "my-new-diagram";
            if ("PlantUML Container".equals(newVal)) {
                nameField.setText(baseName + ".puml");
            } else if ("Graphviz Directed Graph".equals(newVal)) {
                nameField.setText(baseName + ".dot");
            } else {
                nameField.setText(baseName + ".mermaid");
            }
        });

        grid.add(new Label("Diagram Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Diagram Type:"), 0, 1);
        grid.add(typeBox, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("variables-btn-save");
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("variables-btn-cancel");

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = "unnamed";
                String selectedType = typeBox.getValue();
                
                String ext = ".mermaid";
                String folder = "diagrams/mermaid";
                
                if ("PlantUML Container".equals(selectedType) || name.toLowerCase().endsWith(".puml") || name.toLowerCase().endsWith(".plantuml")) {
                    ext = ".puml";
                    folder = "diagrams/plantuml";
                } else if ("Graphviz Directed Graph".equals(selectedType) || name.toLowerCase().endsWith(".dot") || name.toLowerCase().endsWith(".gv")) {
                    ext = ".dot";
                    folder = "diagrams/graphviz";
                }

                if (!name.toLowerCase().endsWith(ext)) {
                    name += ext;
                }

                String sampleCode = "";
                if (".puml".equals(ext)) {
                    sampleCode = "@startuml\n" +
                                 "!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml\n\n" +
                                 "title System Container Diagram for Online Banking System\n\n" +
                                 "Person(customer, \"Banking Customer\", \"A customer of the bank, with personal bank accounts.\")\n\n" +
                                 "System_Boundary(c1, \"Internet Banking System\") {\n" +
                                 "    Container(web_app, \"Web Application\", \"Java, Spring Boot\", \"Delivers the static content and the internet banking single page application.\")\n" +
                                 "    Container(spa, \"Single-Page Application\", \"JavaScript, Angular\", \"Provides all of the internet banking functionality to the customer via their web browser.\")\n" +
                                 "    Container(mobile_app, \"Mobile App\", \"C#, Xamarin\", \"Provides a limited subset of the internet banking functionality to customers via their mobile device.\")\n" +
                                 "    Container(api_app, \"API Application\", \"Java, Spring Boot\", \"Provides internet banking functionality via a JSON/HTTPS API.\")\n" +
                                 "    ContainerDb(database, \"Database\", \"Relational Database Schema\", \"Stores user registration information, hashed authentication credentials, access logs, etc.\")\n" +
                                 "}\n\n" +
                                 "System_Ext(mail_system, \"E-mail System\", \"The internal Microsoft Exchange e-mail system.\")\n" +
                                 "System_Ext(mainframe, \"Mainframe Banking System\", \"Stores all of the core banking information about customers, accounts, transactions, etc.\")\n\n" +
                                 "Rel(customer, web_app, \"Uses\", \"HTTPS\")\n" +
                                 "Rel(customer, spa, \"Uses\", \"HTTPS\")\n" +
                                 "Rel(customer, mobile_app, \"Uses\", \"HTTPS\")\n\n" +
                                 "Rel_Neighbor(web_app, spa, \"Delivers\", \"HTTPS\")\n" +
                                 "Rel(spa, api_app, \"Uses\", \"JSON/HTTPS\")\n" +
                                 "Rel(mobile_app, api_app, \"Uses\", \"JSON/HTTPS\")\n\n" +
                                 "Rel(api_app, database, \"Reads from and writes to\", \"JDBC\")\n" +
                                 "Rel_Back(mail_system, api_app, \"Sends e-mails using\", \"SMTP\")\n" +
                                 "Rel(api_app, mainframe, \"Uses\", \"XML/HTTPS\")\n" +
                                 "@enduml\n";
                } else if (".dot".equals(ext)) {
                    sampleCode = "digraph G {\n" +
                                 "    // Graph Styling\n" +
                                 "    fontname=\"Helvetica,Arial,sans-serif\"\n" +
                                 "    node [fontname=\"Helvetica,Arial,sans-serif\", style=filled, shape=box, penwidth=1.5]\n" +
                                 "    edge [fontname=\"Helvetica,Arial,sans-serif\", penwidth=1.5]\n" +
                                 "    bgcolor=\"#1e1e1e\"\n\n" +
                                 "    // Subgraphs for clusters\n" +
                                 "    subgraph cluster_frontend {\n" +
                                 "        label = \"Frontend Layer\"\n" +
                                 "        color = \"#2196F3\"\n" +
                                 "        fontcolor = \"#2196F3\"\n" +
                                 "        style = dashed\n" +
                                 "        \n" +
                                 "        WebApp [label=\"Web Application\\n(React)\", fillcolor=\"#1565C0\", fontcolor=\"#ffffff\"]\n" +
                                 "        MobileApp [label=\"Mobile App\\n(Flutter)\", fillcolor=\"#1976D2\", fontcolor=\"#ffffff\"]\n" +
                                 "    }\n\n" +
                                 "    subgraph cluster_backend {\n" +
                                 "        label = \"Microservices Layer\"\n" +
                                 "        color = \"#4CAF50\"\n" +
                                 "        fontcolor = \"#4CAF50\"\n" +
                                 "        style = dashed\n" +
                                 "        \n" +
                                 "        APIGateway [label=\"API Gateway\\n(Spring Cloud)\", fillcolor=\"#2E7D32\", fontcolor=\"#ffffff\", shape=hexagon]\n" +
                                 "        AuthService [label=\"Auth Service\\n(Go)\", fillcolor=\"#388E3C\", fontcolor=\"#ffffff\"]\n" +
                                 "        OrderService [label=\"Order Service\\n(Java)\", fillcolor=\"#43A047\", fontcolor=\"#ffffff\"]\n" +
                                 "    }\n\n" +
                                 "    subgraph cluster_data {\n" +
                                 "        label = \"Data Stores\"\n" +
                                 "        color = \"#FF9800\"\n" +
                                 "        fontcolor = \"#FF9800\"\n" +
                                 "        style = dashed\n" +
                                 "        \n" +
                                 "        UserDB [label=\"User Database\\n(PostgreSQL)\", fillcolor=\"#E65100\", fontcolor=\"#ffffff\", shape=cylinder]\n" +
                                 "        OrderDB [label=\"Order Database\\n(MongoDB)\", fillcolor=\"#F57C00\", fontcolor=\"#ffffff\", shape=cylinder]\n" +
                                 "    }\n\n" +
                                 "    // Connections\n" +
                                 "    WebApp -> APIGateway [label=\"HTTPS/REST\", color=\"#ffffff\", fontcolor=\"#ffffff\"]\n" +
                                 "    MobileApp -> APIGateway [label=\"HTTPS/REST\", color=\"#ffffff\", fontcolor=\"#ffffff\"]\n" +
                                 "    \n" +
                                 "    APIGateway -> AuthService [label=\"gRPC\", color=\"#81C784\", fontcolor=\"#81C784\"]\n" +
                                 "    APIGateway -> OrderService [label=\"gRPC\", color=\"#81C784\", fontcolor=\"#81C784\"]\n" +
                                 "    \n" +
                                 "    AuthService -> UserDB [label=\"SQL\", color=\"#FFB74D\", fontcolor=\"#FFB74D\"]\n" +
                                 "    OrderService -> OrderDB [label=\"BSON\", color=\"#FFB74D\", fontcolor=\"#FFB74D\"]\n" +
                                 "}\n";
                } else {
                    sampleCode = "%%{init: {'theme': 'dark'}}%%\n" +
                                 "graph TD\n" +
                                 "    %% Node Definitions %%\n" +
                                 "    Start([Start Process]) --> Auth{Is User Authorized?}\n" +
                                 "    \n" +
                                 "    %% Branching Paths %%\n" +
                                 "    Auth -->|Yes| Fetch[Fetch Customer Account Data]\n" +
                                 "    Auth -->|No| Reject[Access Denied]\n" +
                                 "    \n" +
                                 "    %% Nested Container Subgraph %%\n" +
                                 "    subgraph ProcessingEngine [Data Processing Engine]\n" +
                                 "        direction LR\n" +
                                 "        Fetch --> Validation{Check Balance}\n" +
                                 "        Validation -->|Sufficient| Approve[Approve Transaction]\n" +
                                 "        Validation -->|Insufficient| Hold[Place Hold on Account]\n" +
                                 "    end\n" +
                                 "    \n" +
                                 "    %% Node Styling %%\n" +
                                 "    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:2px,color:#fff\n" +
                                 "    style Reject fill:#F44336,stroke:#B71C1C,stroke-width:2px,color:#fff\n" +
                                 "    style Auth fill:#FF9800,stroke:#E65100,stroke-width:2px,color:#fff\n" +
                                 "    style Validation fill:#2196F3,stroke:#0D47A1,stroke-width:2px,color:#fff\n" +
                                 "    \n" +
                                 "    %% Connector styles %%\n" +
                                 "    linkStyle 0 stroke:#4CAF50,stroke-width:2px;\n" +
                                 "    linkStyle 1 stroke:#F44336,stroke-width:2px;\n";
                }

                File f = new File(new File(workspaceRoot, folder), name);
                try {
                    f.getParentFile().mkdirs();
                    Files.writeString(f.toPath(), sampleCode);
                    refreshTree();
                    openFile(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        
        if (btnLayoutMode != null) {
            updateLayoutModeIcon(btnLayoutMode);
        }
        
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
        prefs.put("layoutMode", currentLayoutMode);
        updateAllTabsLayout();
    }

    private void updateLayoutModeIcon(Button btn) {
        if ("Code Left".equals(currentLayoutMode)) {
            btn.setGraphic(new FontIcon("fas-columns"));
            btn.setTooltip(new Tooltip("Layout: Code Left (Click to cycle)"));
        } else if ("Code Top".equals(currentLayoutMode)) {
            btn.setGraphic(new FontIcon("fas-th-list"));
            btn.setTooltip(new Tooltip("Layout: Code Top (Click to cycle)"));
        } else if ("Code Right".equals(currentLayoutMode)) {
            btn.setGraphic(new FontIcon("fas-columns"));
            btn.setTooltip(new Tooltip("Layout: Code Right (Click to cycle)"));
        } else {
            btn.setGraphic(new FontIcon("fas-th-list"));
            btn.setTooltip(new Tooltip("Layout: Code Bottom (Click to cycle)"));
        }
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
        String monacoTheme = getMonacoThemeName(themeName);
        Platform.runLater(() -> {
            if (studioThemeBox != null) studioThemeBox.setValue(themeName);
            for (WebEngine engine : tabEngines.values()) {
                engine.executeScript("if(window.setTheme) window.setTheme('" + monacoTheme + "');");
            }
            if (root != null) {
                root.getStyleClass().removeAll("theme-vscode-dark", "theme-intellij-light", "theme-dracula", "theme-monokai", "theme-hacker");
                root.getStyleClass().add(themeClass);
            }
        });
    }
}
