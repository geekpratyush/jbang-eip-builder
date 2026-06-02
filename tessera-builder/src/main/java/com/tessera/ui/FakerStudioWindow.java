package com.tessera.ui;

import com.tessera.faker.UniversalFaker;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

public class FakerStudioWindow {
    public static final java.util.List<FakerStudioWindow> activeInstances = new java.util.ArrayList<>();

    private final Stage stage;
    private TreeView<File> templateTree;
    private TreeView<File> dbTree;
    private TabPane sidebarTabs;
    private File templatesRoot;
    private File dbBaseDir;
    private File workspaceRoot;
    
    private com.tessera.ui.components.MonacoEditorPane editorMonaco;
    private com.tessera.ui.components.MonacoEditorPane previewMonaco;
    
    private File currentFile;
    private List<File> clipboardFiles = new java.util.ArrayList<>();
    private boolean isCutOperation = false;

    private UniversalFaker faker;
    private ComboBox<String> dbComboBox;
    private Timeline fakingTimeline;

    public FakerStudioWindow(File projectBase) {
        activeInstances.add(this);
        this.stage = new Stage();
        
        String wsRoot = System.getProperty("WORKSPACE_ROOT_DIR");
        File base = (projectBase != null) ? projectBase : (wsRoot != null ? new File(wsRoot) : new File(System.getProperty("user.dir")));

        // Search for templates folder
        String templatesEnv = System.getProperty("FAKER_TEMPLATES_DIR");
        this.templatesRoot = (templatesEnv != null) ? new File(templatesEnv) : new File(base, "FAKER/templates");
        if (!templatesRoot.exists()) templatesRoot = new File(base, "templates");
        if (!templatesRoot.exists()) {
            File root = wsRoot != null ? new File(wsRoot) : new File(System.getProperty("user.dir"));
            templatesRoot = new File(root, "FAKER/templates");
            if (!templatesRoot.exists()) templatesRoot = new File(root, "templates");
        }
        if (!templatesRoot.exists()) templatesRoot.mkdirs();
        
        // Discover database folders inside faker-db
        String dbEnv = System.getProperty("FAKER_DB_DIR");
        this.dbBaseDir = (dbEnv != null) ? new File(dbEnv).getParentFile() : new File(base, "FAKER/faker-db");
        if (!dbBaseDir.exists()) dbBaseDir = new File(base, "faker-db");
        if (!dbBaseDir.exists()) {
            File root = wsRoot != null ? new File(wsRoot) : new File(System.getProperty("user.dir"));
            dbBaseDir = new File(root, "FAKER/faker-db");
        }
        if (!dbBaseDir.exists()) dbBaseDir.mkdirs();

        this.workspaceRoot = (templatesRoot.getParentFile() != null) ? templatesRoot.getParentFile() : base;
        
        java.util.List<String> dbNames = new java.util.ArrayList<>();
        if (dbBaseDir.exists() && dbBaseDir.isDirectory()) {
            File[] dbDirs = dbBaseDir.listFiles(File::isDirectory);
            if (dbDirs != null) {
                for (File d : dbDirs) if (!d.getName().startsWith(".")) dbNames.add(d.getName());
            }
        }
        java.util.Collections.sort(dbNames);

        dbComboBox = new ComboBox<>();
        dbComboBox.setPromptText("Select DB");
        dbComboBox.getItems().addAll(dbNames);

        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(FakerStudioWindow.class);
        String cachedDb = prefs.get("selected_faker_db", null);

        if (dbNames.contains(cachedDb)) {
            dbComboBox.setValue(cachedDb);
            this.faker = new UniversalFaker(templatesRoot.toPath(), dbBaseDir.toPath().resolve(cachedDb));
        } else if (!dbNames.isEmpty()) {
            dbComboBox.setValue(dbNames.get(0));
            this.faker = new UniversalFaker(templatesRoot.toPath(), dbBaseDir.toPath().resolve(dbNames.get(0)));
        } else {
            this.faker = new UniversalFaker(templatesRoot.toPath());
        }

        dbComboBox.setOnAction(evt -> {
            String selected = dbComboBox.getValue();
            if (selected != null) {
                prefs.put("selected_faker_db", selected);
                faker.setDatabase(dbBaseDir.toPath().resolve(selected));
                generatePreview();
            }
        });
    }

    public void show() {
        stage.setTitle("Tessera - Universal Faker Studio");
        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", RouteBuilderApp.currentThemeClass);
        com.tessera.ui.components.ThemeManager.registerRoot(root);

        sidebarTabs = new TabPane();
        sidebarTabs.setSide(Side.TOP); sidebarTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        sidebarTabs.setPrefWidth(280);

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("studio-toolbar");

        MenuButton btnNew = new MenuButton("New", new FontIcon("fas-plus"));
        btnNew.getStyleClass().add("toolbar-btn");
        
        MenuItem miNewFile = new MenuItem("New File", new FontIcon("fas-plus"));
        miNewFile.setOnAction(e -> {
            Tab active = sidebarTabs.getSelectionModel().getSelectedItem();
            File rootDir = "Faker DB".equals(active.getText()) ? dbBaseDir : templatesRoot;
            createNewFileIn(rootDir);
        });
        
        MenuItem miNewFolder = new MenuItem("New Folder", new FontIcon("fas-folder"));
        miNewFolder.setOnAction(e -> {
            Tab active = sidebarTabs.getSelectionModel().getSelectedItem();
            File rootDir = "Faker DB".equals(active.getText()) ? dbBaseDir : templatesRoot;
            createNewFolderIn(rootDir);
        });
        
        btnNew.getItems().addAll(miNewFile, miNewFolder);

        Button btnSave = new Button("Save", new FontIcon("fas-save"));
        btnSave.getStyleClass().add("toolbar-btn");
        btnSave.setOnAction(e -> saveCurrentFile());

        Button btnRefresh = new Button("Refresh", new FontIcon("fas-sync"));
        btnRefresh.getStyleClass().add("toolbar-btn");
        btnRefresh.setOnAction(e -> refreshTrees());

        Button btnGenerate = new Button("Fake Once", new FontIcon("fas-magic"));
        btnGenerate.getStyleClass().addAll("toolbar-btn", "btn-run");
        btnGenerate.setOnAction(e -> generatePreview());

        Button btnStartFaking = new Button("Start Auto", new FontIcon("fas-play"));
        btnStartFaking.getStyleClass().addAll("toolbar-btn", "btn-run");
        Button btnStopFaking = new Button("Stop", new FontIcon("fas-stop"));
        btnStopFaking.getStyleClass().addAll("toolbar-btn", "btn-stop");
        btnStopFaking.setDisable(true);

        Label lblDelay = new Label(" Delay:");
        Spinner<Integer> delaySpinner = new Spinner<>(100, 10000, 1000, 100);
        delaySpinner.setEditable(true); delaySpinner.setPrefWidth(90);

        btnStartFaking.setOnAction(e -> {
            btnStartFaking.setDisable(true); btnStopFaking.setDisable(false); delaySpinner.setDisable(true);
            fakingTimeline = new Timeline(new KeyFrame(Duration.millis(delaySpinner.getValue()), evt -> generatePreview()));
            fakingTimeline.setCycleCount(Timeline.INDEFINITE); fakingTimeline.play();
        });
        btnStopFaking.setOnAction(e -> {
            btnStartFaking.setDisable(false); btnStopFaking.setDisable(true); delaySpinner.setDisable(false);
            if (fakingTimeline != null) { fakingTimeline.stop(); fakingTimeline = null; }
        });

        Label lblDb = new Label("DB:");
        Separator sep1 = new Separator();
        Separator sep2 = new Separator();

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        toolBar.getItems().addAll(btnNew, btnSave, btnRefresh, sep1, btnGenerate, btnStartFaking, btnStopFaking, lblDelay, delaySpinner, sep2, lblDb, dbComboBox, spacer);
        root.setTop(toolBar);

        Tab tTemplates = new Tab("Templates", new FontIcon("fas-file-code"));
        TextField templateFilter = new TextField();
        templateFilter.setPromptText("Search templates...");
        templateFilter.getStyleClass().add("studio-search-field");
        // Ensure tree view fills available height
        templateTree = createTreeView(templatesRoot);
        VBox.setVgrow(templateTree, javafx.scene.layout.Priority.ALWAYS);
        templateFilter.textProperty().addListener((obs, oldVal, newVal) -> filterTree(templatesRoot, templateTree, newVal));
        tTemplates.setContent(new VBox(5, templateFilter, templateTree));

        Tab tDb = new Tab("Faker DB", new FontIcon("fas-database"));
        TextField dbFilter = new TextField();
        dbFilter.setPromptText("Search database...");
        dbFilter.getStyleClass().add("studio-search-field");
        dbTree = createTreeView(dbBaseDir);
        VBox.setVgrow(dbTree, javafx.scene.layout.Priority.ALWAYS);
        dbFilter.textProperty().addListener((obs, oldVal, newVal) -> filterTree(dbBaseDir, dbTree, newVal));
        tDb.setContent(new VBox(5, dbFilter, dbTree));

        sidebarTabs.getTabs().addAll(tTemplates, tDb);
        root.setLeft(sidebarTabs);

        // --- Editor & Preview ---
        VBox editorPanel = new VBox();
        editorMonaco = new com.tessera.ui.components.MonacoEditorPane(c -> {}, "xml");
        editorMonaco.setOnSave(this::saveCurrentFile);
        
        javafx.scene.Node editorHeader = createEditorHeader("EDITOR", editorMonaco);
        editorPanel.getChildren().addAll(editorHeader, editorMonaco);
        VBox.setVgrow(editorMonaco, Priority.ALWAYS);

        previewMonaco = new com.tessera.ui.components.MonacoEditorPane(c -> {}, "text");
        previewMonaco.setEditable(false);
        VBox previewBox = new VBox(new Label("  Live Preview Output"), previewMonaco);
        previewBox.getStyleClass().add("preview-box"); VBox.setVgrow(previewMonaco, Priority.ALWAYS);

        SplitPane mainSplit = new SplitPane(editorPanel, previewBox);
        mainSplit.setOrientation(Orientation.HORIZONTAL); mainSplit.setDividerPositions(0.6);
        root.setCenter(mainSplit);

        // Dynamic visibility logic
        sidebarTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            boolean isTemplates = newTab != null && "Templates".equals(newTab.getText());
            
            // Toolbar visibility
            btnGenerate.setVisible(isTemplates); btnGenerate.setManaged(isTemplates);
            btnStartFaking.setVisible(isTemplates); btnStartFaking.setManaged(isTemplates);
            btnStopFaking.setVisible(isTemplates); btnStopFaking.setManaged(isTemplates);
            lblDelay.setVisible(isTemplates); lblDelay.setManaged(isTemplates);
            delaySpinner.setVisible(isTemplates); delaySpinner.setManaged(isTemplates);
            sep1.setVisible(isTemplates); sep1.setManaged(isTemplates);
            sep2.setVisible(isTemplates); sep2.setManaged(isTemplates);
            lblDb.setVisible(isTemplates); lblDb.setManaged(isTemplates);
            dbComboBox.setVisible(isTemplates); dbComboBox.setManaged(isTemplates);
            
            // Preview panel visibility
            previewBox.setVisible(isTemplates);
            previewBox.setManaged(isTemplates);
            if (isTemplates) {
                mainSplit.setDividerPositions(0.6);
            } else {
                mainSplit.setDividerPositions(1.0);
            }
        });

        Scene scene = new Scene(root, 1300, 900);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene);
        stage.setOnHidden(e -> { if (fakingTimeline != null) fakingTimeline.stop(); activeInstances.remove(this); });
        stage.show();
        refreshTrees();
        
        // Trigger initial visibility
        Platform.runLater(() -> sidebarTabs.getSelectionModel().select(tTemplates));
    }

    private TreeView<File> createTreeView(File rootDir) {
        TreeView<File> tree = new TreeView<>();
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tree.getStyleClass().add("sidebar-tree-view");
        tree.setCellFactory(tv -> new TreeCell<File>() {
            @Override protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(item.getName()); setGraphic(RouteBuilderApp.getFileIcon(item)); }
            }
        });

        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue().isFile()) openFile(newVal.getValue());
        });

        tree.setContextMenu(createContextMenu(tree, rootDir));
        return tree;
    }

    private ContextMenu createContextMenu(TreeView<File> tree, File rootDir) {
        ContextMenu menu = new ContextMenu();
        
        MenuItem miNewFile = new MenuItem("New File", new FontIcon("fas-plus"));
        miNewFile.setOnAction(e -> {
            File selected = getSelectedOrRoot(tree, rootDir);
            createNewFileIn(selected.isDirectory() ? selected : selected.getParentFile());
        });

        MenuItem miNewFolder = new MenuItem("New Folder", new FontIcon("fas-folder"));
        miNewFolder.setOnAction(e -> {
            File selected = getSelectedOrRoot(tree, rootDir);
            createNewFolderIn(selected.isDirectory() ? selected : selected.getParentFile());
        });

        MenuItem miCut = new MenuItem("Cut", new FontIcon("fas-cut"));
        miCut.setOnAction(e -> { 
            clipboardFiles = tree.getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.toList());
            isCutOperation = true; 
        });

        MenuItem miCopy = new MenuItem("Copy", new FontIcon("fas-copy"));
        miCopy.setOnAction(e -> { 
            clipboardFiles = tree.getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.toList());
            isCutOperation = false; 
        });

        MenuItem miPaste = new MenuItem("Paste", new FontIcon("fas-paste"));
        miPaste.setOnAction(e -> {
            File target = getSelectedOrRoot(tree, rootDir);
            pasteFiles(target.isDirectory() ? target : target.getParentFile());
        });

        MenuItem miRename = new MenuItem("Rename", new FontIcon("fas-edit"));
        miRename.setOnAction(e -> renameFile(tree.getSelectionModel().getSelectedItem().getValue()));

        MenuItem miDelete = new MenuItem("Delete Selected", new FontIcon("fas-trash"));
        miDelete.setOnAction(e -> deleteSelectedFiles(tree));

        menu.getItems().addAll(miNewFile, miNewFolder, new SeparatorMenuItem(), miCut, miCopy, miPaste, new SeparatorMenuItem(), miRename, miDelete);
        return menu;
    }

    private File getSelectedOrRoot(TreeView<File> tree, File rootDir) {
        TreeItem<File> item = tree.getSelectionModel().getSelectedItem();
        return (item != null) ? item.getValue() : rootDir;
    }

    private void refreshTrees() {
        populateTree(templatesRoot, templateTree);
        populateTree(dbBaseDir, dbTree);
        faker.reloadTemplates();
    }

    private void populateTree(File rootDir, TreeView<File> tree) {
        if (rootDir == null || !rootDir.exists()) return;
        TreeItem<File> rootItem = new TreeItem<>(rootDir);
        rootItem.setExpanded(true); buildTree(rootDir, rootItem);
        tree.setRoot(rootItem); tree.setShowRoot(false);
    }

    private void filterTree(File rootDir, TreeView<File> tree, String query) {
        if (query == null || query.trim().isEmpty()) {
            populateTree(rootDir, tree);
            return;
        }
        String q = query.toLowerCase();
        TreeItem<File> rootItem = new TreeItem<>(rootDir);
        rootItem.setExpanded(true);
        buildFilteredTree(rootDir, rootItem, q);
        tree.setRoot(rootItem);
        tree.setShowRoot(false);
    }

    private boolean buildFilteredTree(File dir, TreeItem<File> parent, String query) {
        File[] files = dir.listFiles();
        boolean hasMatch = false;
        if (files != null) {
            java.util.Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });
            for (File f : files) {
                if (f.getName().startsWith(".")) continue;
                if (f.isDirectory()) {
                    TreeItem<File> subItem = new TreeItem<>(f);
                    if (buildFilteredTree(f, subItem, query)) {
                        parent.getChildren().add(subItem);
                        subItem.setExpanded(true);
                        hasMatch = true;
                    }
                } else if (f.getName().toLowerCase().contains(query)) {
                    parent.getChildren().add(new TreeItem<>(f));
                    hasMatch = true;
                }
            }
        }
        return hasMatch || dir.getName().toLowerCase().contains(query);
    }

    private void buildTree(File dir, TreeItem<File> parent) {
        File[] files = dir.listFiles();
        if (files != null) {
            java.util.Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });
            for (File f : files) {
                if (f.getName().startsWith(".")) continue;
                TreeItem<File> item = new TreeItem<>(f);
                parent.getChildren().add(item);
                if (f.isDirectory()) buildTree(f, item);
            }
        }
    }

    private void openFile(File file) {
        this.currentFile = file;
        try {
            String content = Files.readString(file.toPath());
            String lang = file.getName().endsWith(".json") ? "json" : "xml";
            editorMonaco.setLanguage(lang); editorMonaco.setText(content);
            previewMonaco.setText("");
        } catch (IOException ignored) {}
    }

    private void generatePreview() {
        if (editorMonaco == null) return;
        previewMonaco.setText(faker.generateDirect(editorMonaco.getText()));
    }

    private void saveCurrentFile() {
        if (currentFile != null) {
            try {
                Files.writeString(currentFile.toPath(), editorMonaco.getText());
                faker.reloadTemplates();
                System.out.println("[FakerStudio] Saved: " + currentFile.getName());
            } catch (IOException ignored) {}
        }
    }

    private void createNewFileIn(File dir) {
        TextInputDialog dialog = new TextInputDialog("new-file.json");
        dialog.setTitle("New File"); dialog.showAndWait().ifPresent(name -> {
            File f = new File(dir, name);
            try { if (f.createNewFile()) { refreshTrees(); openFile(f); } } catch (IOException ignored) {}
        });
    }

    private void createNewFolderIn(File dir) {
        TextInputDialog dialog = new TextInputDialog("new-folder");
        dialog.setTitle("New Folder"); dialog.showAndWait().ifPresent(name -> {
            File f = new File(dir, name);
            if (f.mkdirs()) refreshTrees();
        });
    }

    private void renameFile(File f) {
        TextInputDialog dialog = new TextInputDialog(f.getName());
        dialog.setTitle("Rename"); dialog.showAndWait().ifPresent(name -> {
            File dest = new File(f.getParentFile(), name);
            if (f.renameTo(dest)) refreshTrees();
        });
    }

    private void deleteSelectedFiles(TreeView<File> tree) {
        List<File> files = tree.getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.toList());
        if (files.isEmpty()) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + files.size() + " selected items?", ButtonType.YES, ButtonType.NO);
        RouteBuilderApp.themeDialog(alert);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                for (File f : files) deleteFileRecursively(f);
                refreshTrees();
            }
        });
    }

    private void deleteFileRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteFileRecursively(child);
            }
        }
        file.delete();
    }

    private void pasteFiles(File targetDir) {
        if (clipboardFiles.isEmpty()) return;
        try {
            for (File f : clipboardFiles) {
                Path dest = targetDir.toPath().resolve(f.getName());
                if (isCutOperation) Files.move(f.toPath(), dest);
                else copyFileOrFolder(f.toPath(), dest);
            }
            if (isCutOperation) clipboardFiles.clear();
            refreshTrees();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void copyFileOrFolder(Path src, Path dest) throws IOException {
        if (Files.isDirectory(src)) {
            Files.createDirectories(dest);
            try (var stream = Files.list(src)) {
                for (Path p : stream.collect(Collectors.toList())) copyFileOrFolder(p, dest.resolve(p.getFileName()));
            }
        } else {
            Files.copy(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private javafx.scene.Node createEditorHeader(String title, com.tessera.ui.components.MonacoEditorPane editor) {
        HBox header = new HBox(5);
        header.setPadding(new Insets(2, 5, 2, 8));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("editor-panel-header");
        
        Label lbl = new Label(title);
        lbl.getStyleClass().add("editor-panel-header-label");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnSave = createSmallButton("fas-save", "Save (Ctrl+S)");
        btnSave.setOnAction(e -> saveCurrentFile());
        
        Button btnCopy = createSmallButton("fas-copy", "Copy All");
        btnCopy.setOnAction(e -> {
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(editor.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        });
        
        header.getChildren().addAll(lbl, spacer, btnSave, btnCopy);
        return header;
    }

    private Button createSmallButton(String icon, String tip) {
        Button b = new Button("", new FontIcon(icon));
        b.getStyleClass().addAll("icon-only-btn", "small-btn");
        b.setTooltip(new Tooltip(tip));
        return b;
    }
}
