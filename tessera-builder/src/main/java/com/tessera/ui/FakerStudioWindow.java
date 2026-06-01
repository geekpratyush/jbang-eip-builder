package com.tessera.ui;

import com.tessera.faker.UniversalFaker;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FakerStudioWindow {
    public static final java.util.List<FakerStudioWindow> activeInstances = new java.util.ArrayList<>();

    private final Stage stage;
    private File workspaceRoot;
    private TreeView<File> treeView;
    
    private com.tessera.ui.components.MonacoEditorPane editorMonaco;
    private com.tessera.ui.components.MonacoEditorPane previewMonaco;
    
    private File currentFile;

    private UniversalFaker faker;
    private ComboBox<String> dbComboBox;
    private Timeline fakingTimeline;

    private Object strongEditorBridge;
    private Object strongPreviewBridge;

    private File clipboardFile = null;
    private boolean isCutOperation = false;
    private StringBuilder prefixBuffer = new StringBuilder();
    private long lastKeyTime = 0;

    public FakerStudioWindow(File projectBase) {
        activeInstances.add(this);
        this.stage = new Stage();
        
        String wsRoot = System.getProperty("WORKSPACE_ROOT_DIR");
        File base = (projectBase != null) ? projectBase : (wsRoot != null ? new File(wsRoot) : new File(System.getProperty("user.dir")));
        System.out.println("FakerStudio: Searching for workspace in " + base.getAbsolutePath());

        // Search for templates folder: prioritize Environment Standard first
        String templatesEnv = System.getProperty("FAKER_TEMPLATES_DIR");
        File templatesDir = (templatesEnv != null) ? new File(templatesEnv) : new File(base, "FAKER/templates");
        
        if (!templatesDir.exists()) {
            templatesDir = new File(base, "templates");
        }
        
        // Secondary fallback if still not found (handles cases where app is run from subfolders)
        if (!templatesDir.exists()) {
            File root = wsRoot != null ? new File(wsRoot) : new File(System.getProperty("user.dir"));
            templatesDir = new File(root, "FAKER/templates");
            if (!templatesDir.exists()) {
                templatesDir = new File(root, "templates");
            }
        }
        
        if (!templatesDir.exists()) {
            templatesDir.mkdirs();
        }
        
        this.workspaceRoot = templatesDir.getAbsoluteFile();
        System.out.println("FakerStudio: Workspace Root set to " + workspaceRoot.getAbsolutePath());

        // Discover database folders inside faker-db
        String dbEnv = System.getProperty("FAKER_DB_DIR");
        File dbBaseDir = (dbEnv != null) ? new File(dbEnv).getParentFile() : new File(base, "FAKER/faker-db");
        
        if (!dbBaseDir.exists()) {
            dbBaseDir = new File(base, "faker-db");
        }
        if (!dbBaseDir.exists()) {
            File root = wsRoot != null ? new File(wsRoot) : new File(System.getProperty("user.dir"));
            dbBaseDir = new File(root, "FAKER/faker-db");
            if (!dbBaseDir.exists()) {
                dbBaseDir = new File(root, "faker-db");
            }
        }

        System.out.println("FakerStudio: DB Base Dir set to " + dbBaseDir.getAbsolutePath());
        
        // Scan for DB subdirectories
        java.util.List<String> dbNames = new java.util.ArrayList<>();
        if (dbBaseDir.exists() && dbBaseDir.isDirectory()) {
            File[] dbDirs = dbBaseDir.listFiles(File::isDirectory);
            if (dbDirs != null) {
                for (File d : dbDirs) {
                    if (!d.getName().startsWith(".")) {
                        dbNames.add(d.getName());
                    }
                }
            }
        }
        java.util.Collections.sort(dbNames);
        System.out.println("FakerStudio: Found " + dbNames.size() + " databases: " + dbNames);

        dbComboBox = new ComboBox<>();
        dbComboBox.setPromptText("Select DB");
        dbComboBox.getItems().addAll(dbNames);

        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(FakerStudioWindow.class);
        String cachedDb = prefs.get("selected_faker_db", null);

        if (dbNames.size() == 1) {
            dbComboBox.setValue(dbNames.get(0));
            prefs.put("selected_faker_db", dbNames.get(0));
            this.faker = new UniversalFaker(workspaceRoot.toPath(), dbBaseDir.toPath().resolve(dbNames.get(0)));
        } else if (dbNames.size() > 1) {
            if (cachedDb != null && dbNames.contains(cachedDb)) {
                dbComboBox.setValue(cachedDb);
                this.faker = new UniversalFaker(workspaceRoot.toPath(), dbBaseDir.toPath().resolve(cachedDb));
            } else {
                dbComboBox.setValue(dbNames.get(0));
                prefs.put("selected_faker_db", dbNames.get(0));
                this.faker = new UniversalFaker(workspaceRoot.toPath(), dbBaseDir.toPath().resolve(dbNames.get(0)));
            }
        } else {
            this.faker = new UniversalFaker(workspaceRoot.toPath());
        }

        final File finalDbBaseDir = dbBaseDir;
        dbComboBox.setOnAction(evt -> {
            String selected = dbComboBox.getValue();
            if (selected != null) {
                prefs.put("selected_faker_db", selected);
                faker.setDatabase(finalDbBaseDir.toPath().resolve(selected));
                generatePreview();
            }
        });
    }

    public void show() {
        stage.setTitle("Tessera - Faker Studio");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.getStyleClass().add(RouteBuilderApp.currentThemeClass);

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("editor-toolbar");

        Button btnNew = new Button("New", new FontIcon("fas-plus"));
        btnNew.getStyleClass().addAll("toolbar-btn", "btn-new");
        btnNew.setOnAction(e -> createNewTemplate());

        Button btnSave = new Button("Save", new FontIcon("fas-save"));
        btnSave.getStyleClass().addAll("toolbar-btn", "btn-save");
        btnSave.setOnAction(e -> saveCurrentFile());

        Button btnRefresh = new Button("Refresh", new FontIcon("fas-sync"));
        btnRefresh.getStyleClass().addAll("toolbar-btn");
        btnRefresh.setOnAction(e -> refreshTree());

        Button btnGenerate = new Button("Fake It!", new FontIcon("fas-magic"));
        btnGenerate.getStyleClass().addAll("toolbar-btn", "btn-validate");
        btnGenerate.setOnAction(e -> generatePreview());

        Button btnStartFaking = new Button("Start Faking", new FontIcon("fas-play"));
        btnStartFaking.getStyleClass().addAll("toolbar-btn", "btn-play");

        Button btnStopFaking = new Button("Stop Faking", new FontIcon("fas-stop"));
        btnStopFaking.getStyleClass().addAll("toolbar-btn", "btn-stop");
        btnStopFaking.setDisable(true);

        Label lblDelay = new Label("Delay (ms):");
        lblDelay.setStyle("-fx-text-fill: -fx-text-background-color;");

        Spinner<Integer> delaySpinner = new Spinner<>(100, 10000, 1000, 100);
        delaySpinner.setEditable(true);
        delaySpinner.setPrefWidth(90);

        btnStartFaking.setOnAction(e -> {
            btnStartFaking.setDisable(true);
            btnStopFaking.setDisable(false);
            delaySpinner.setDisable(true);
            
            fakingTimeline = new Timeline(new KeyFrame(
                Duration.millis(delaySpinner.getValue()),
                evt -> generatePreview()
            ));
            fakingTimeline.setCycleCount(Timeline.INDEFINITE);
            fakingTimeline.play();
        });

        btnStopFaking.setOnAction(e -> {
            btnStartFaking.setDisable(false);
            btnStopFaking.setDisable(true);
            delaySpinner.setDisable(false);
            if (fakingTimeline != null) {
                fakingTimeline.stop();
                fakingTimeline = null;
            }
        });

        Label lblDb = new Label("Database:");
        lblDb.setStyle("-fx-text-fill: -fx-text-background-color;");

        Button btnHelp = new Button("Help Guide", new FontIcon("fas-question-circle"));
        btnHelp.getStyleClass().add("toolbar-btn");
        btnHelp.setOnAction(e -> new RouteBuilderHelpWindow("Advanced Tools", "Faker").show());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(
            btnNew, btnSave, btnRefresh, new Separator(),
            btnGenerate, btnStartFaking, btnStopFaking, lblDelay, delaySpinner,
            new Separator(), lblDb, dbComboBox,
            spacer, btnHelp
        );
        root.setTop(toolBar);

        // --- Sidebar ---
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(10));
        sidebar.setSpacing(5);
        sidebar.getStyleClass().add("studio-sidebar");

        Label lblExplorer = new Label("TEMPLATE EXPLORER");
        lblExplorer.getStyleClass().add("studio-explorer-label");

        TextField filterField = new TextField();
        filterField.setPromptText("Filter templates...");
        filterField.getStyleClass().add("studio-search-field");
        filterField.textProperty().addListener((obs, oldText, newText) -> {
            filterTree(newText);
        });

        treeView = new TreeView<>();
        treeView.getStyleClass().add("sidebar-tree-view");
        VBox.setVgrow(treeView, Priority.ALWAYS);
        
        treeView.setCellFactory(tv -> {
            TreeCell<File> cell = new TreeCell<>() {
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
            };

            ContextMenu contextMenu = new ContextMenu();
            
            MenuItem miCopy = new MenuItem("Copy", new FontIcon("fas-copy"));
            miCopy.setOnAction(evt -> {
                File file = cell.getItem();
                if (file != null) {
                    clipboardFile = file;
                    isCutOperation = false;
                }
            });

            MenuItem miCut = new MenuItem("Cut", new FontIcon("fas-cut"));
            miCut.setOnAction(evt -> {
                File file = cell.getItem();
                if (file != null) {
                    clipboardFile = file;
                    isCutOperation = true;
                }
            });

            MenuItem miPaste = new MenuItem("Paste", new FontIcon("fas-paste"));
            miPaste.setOnAction(evt -> {
                File targetDir = cell.getItem();
                if (targetDir == null) {
                    targetDir = workspaceRoot;
                } else if (targetDir.isFile()) {
                    targetDir = targetDir.getParentFile();
                }
                
                if (clipboardFile != null && clipboardFile.exists()) {
                    try {
                        File dest = new File(targetDir, clipboardFile.getName());
                        if (dest.exists()) {
                            String baseName = getFileBaseName(clipboardFile.getName());
                            String ext = getFileExtension(clipboardFile);
                            dest = new File(targetDir, baseName + "_copy." + ext);
                        }
                        if (clipboardFile.isDirectory()) {
                            copyDirectory(clipboardFile, dest);
                        } else {
                            Files.copy(clipboardFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                        if (isCutOperation) {
                            deleteFileRecursively(clipboardFile);
                            clipboardFile = null;
                        }
                        refreshTree();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            MenuItem miDelete = new MenuItem("Delete", new FontIcon("fas-trash"));
            miDelete.setOnAction(evt -> {
                File file = cell.getItem();
                if (file != null) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirm Delete");
                    alert.setHeaderText("Delete " + file.getName() + "?");
                    alert.setContentText("Are you sure you want to delete this " + (file.isDirectory() ? "directory" : "file") + "? This action cannot be undone.");
                    alert.getDialogPane().getStyleClass().add(RouteBuilderApp.currentThemeClass);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        deleteFileRecursively(file);
                        if (file.equals(currentFile)) {
                            currentFile = null;
                            editorMonaco.setText("");
                            previewMonaco.setText("");
                        }
                        refreshTree();
                    }
                }
            });

            contextMenu.getItems().addAll(miCopy, miCut, miPaste, new SeparatorMenuItem(), miDelete);

            cell.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });

            return cell;
        });

        // Context menu for empty space/root level in treeview
        ContextMenu treeContextMenu = new ContextMenu();
        MenuItem rootPaste = new MenuItem("Paste", new FontIcon("fas-paste"));
        rootPaste.setOnAction(evt -> {
            if (clipboardFile != null && clipboardFile.exists()) {
                try {
                    File targetDir = workspaceRoot;
                    File dest = new File(targetDir, clipboardFile.getName());
                    if (dest.exists()) {
                        String baseName = getFileBaseName(clipboardFile.getName());
                        String ext = getFileExtension(clipboardFile);
                        dest = new File(targetDir, baseName + "_copy." + ext);
                    }
                    if (clipboardFile.isDirectory()) {
                        copyDirectory(clipboardFile, dest);
                    } else {
                        Files.copy(clipboardFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (isCutOperation) {
                        deleteFileRecursively(clipboardFile);
                        clipboardFile = null;
                    }
                    refreshTree();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        treeContextMenu.getItems().add(rootPaste);
        treeView.setContextMenu(treeContextMenu);

        // Incremental keyboard prefix search navigation
        treeView.setOnKeyTyped(event -> {
            String character = event.getCharacter();
            if (character == null || character.isEmpty()) return;
            char ch = character.charAt(0);
            if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '-' || ch == '_') {
                long now = System.currentTimeMillis();
                if (now - lastKeyTime > 1000) {
                    prefixBuffer.setLength(0);
                }
                prefixBuffer.append(ch);
                lastKeyTime = now;

                String searchPrefix = prefixBuffer.toString().toLowerCase();
                selectFirstNodeStartingWith(searchPrefix);
                event.consume();
            }
        });

        // Listen to selection changes to update editor instantly
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue().isFile()) {
                openFile(newVal.getValue());
            }
        });

        sidebar.getChildren().addAll(lblExplorer, filterField, treeView);

        // --- Single Editor/Preview Panel ---
        SplitPane editorSplit = new SplitPane();
        editorSplit.setOrientation(Orientation.HORIZONTAL);

        editorMonaco = new com.tessera.ui.components.MonacoEditorPane("xml");
        editorMonaco.setOnContentChanged(text -> generatePreview());
        previewMonaco = new com.tessera.ui.components.MonacoEditorPane("xml");

        VBox editorPanel = new VBox();
        javafx.scene.Node editorHeader = createHeader("TEMPLATE (EDITABLE)", editorMonaco);
        editorPanel.getChildren().addAll(editorHeader, editorMonaco);
        VBox.setVgrow(editorMonaco, Priority.ALWAYS);

        VBox previewPanel = new VBox();
        javafx.scene.Node previewHeader = createHeader("FAKE PREVIEW", previewMonaco);
        previewPanel.getChildren().addAll(previewHeader, previewMonaco);
        VBox.setVgrow(previewMonaco, Priority.ALWAYS);

        editorSplit.getItems().addAll(editorPanel, previewPanel);
        editorSplit.setDividerPositions(0.5);

        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.getItems().addAll(sidebar, editorSplit);
        horizontalSplit.setDividerPositions(0.2);

        root.setCenter(horizontalSplit);

        com.tessera.ui.components.ThemeManager.registerRoot(root);
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        if (RouteBuilderApp.currentDynamicCssUri != null) {
            scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
        }
        stage.setScene(scene);

        RouteBuilderApp.themedRoots.add(root);
        stage.setOnHidden(e -> {
            if (fakingTimeline != null) {
                fakingTimeline.stop();
            }
            activeInstances.remove(this);
            RouteBuilderApp.themedRoots.remove(root);
        });

        stage.setMaximized(true);
        stage.show();

        refreshTree();
    }

    private void refreshTree() {
        File rootDir = workspaceRoot;
        System.out.println("FakerStudio: Refreshing tree for " + rootDir.getAbsolutePath());
        if (!rootDir.exists()) {
            System.err.println("FakerStudio: Root directory does not exist!");
            return;
        }
        TreeItem<File> rootItem = new TreeItem<>(rootDir);
        rootItem.setExpanded(true);
        buildTree(rootDir, rootItem);
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        
        System.out.println("FakerStudio: Found " + rootItem.getChildren().size() + " templates at root level.");
        faker.reloadTemplates();
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
                if (f.isDirectory()) {
                    buildTree(f, item);
                }
            }
        }
    }

    private void openFile(File file) {
        this.currentFile = file;
        try {
            String content = Files.readString(file.toPath());
            String lang = getLanguageForFile(file);
            
            editorMonaco.setLanguage(lang);
            editorMonaco.setText(content);

            previewMonaco.setLanguage(lang);
            previewMonaco.setText("");
        } catch (IOException ignored) {}
    }





    private void generatePreview() {
        if (editorMonaco == null || previewMonaco == null) return;
        String template = editorMonaco.getText();
        String result = faker.generateDirect(template);
        previewMonaco.setText(result);
    }


    private void saveCurrentFile() {
        if (currentFile != null && editorMonaco != null) {
            try {
                Files.writeString(currentFile.toPath(), editorMonaco.getText());
                faker.reloadTemplates();
            } catch (IOException ignored) {}
        }
    }

    private void createNewTemplate() {
        TextInputDialog dialog = new TextInputDialog("new-template.xml");
        dialog.setTitle("New Template");
        dialog.setHeaderText("Create a new message template");
        dialog.setContentText("Filename:");
        dialog.showAndWait().ifPresent(name -> {
            File f = new File(workspaceRoot, name);
            try {
                if (f.createNewFile()) {
                    refreshTree();
                    selectFileInTree(f);
                }
            } catch (IOException ignored) {}
        });
    }

    private void selectFileInTree(File f) {
        if (treeView.getRoot() == null) return;
        selectFileInTreeRecursive(treeView.getRoot(), f);
    }

    private boolean selectFileInTreeRecursive(TreeItem<File> item, File f) {
        if (f.equals(item.getValue())) {
            treeView.getSelectionModel().select(item);
            return true;
        }
        for (TreeItem<File> child : item.getChildren()) {
            if (selectFileInTreeRecursive(child, f)) {
                return true;
            }
        }
        return false;
    }

    private javafx.scene.Node createHeader(String title, com.tessera.ui.components.MonacoEditorPane editor) {
        HBox header = new HBox(3);
        header.setPadding(new Insets(2, 5, 2, 8));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("editor-panel-header");
        
        Label lbl = new Label(title);
        lbl.getStyleClass().add("editor-panel-header-label");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnCopy = createSmallButton("fas-copy", "Copy All");
        btnCopy.getStyleClass().add("btn-copy-text");
        btnCopy.setOnAction(e -> {
            String text = editor.getText();
            if (text != null && !text.isEmpty()) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(text);
                clipboard.setContent(content);
            }
        });
        
        header.getChildren().addAll(lbl, spacer, btnCopy);
        return header;
    }

    private Button createSmallButton(String iconClass, String tooltip) {
        Button btn = new Button("", new FontIcon(iconClass));
        btn.getStyleClass().addAll("icon-only-btn", "small-btn");
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    private void filterTree(String query) {
        if (query == null || query.trim().isEmpty()) {
            refreshTree();
            return;
        }
        String lowerQuery = query.toLowerCase();
        File rootDir = workspaceRoot;
        TreeItem<File> rootItem = new TreeItem<>(rootDir);
        rootItem.setExpanded(true);
        buildFilteredTree(rootDir, rootItem, lowerQuery);
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
    }

    private boolean buildFilteredTree(File dir, TreeItem<File> parent, String query) {
        File[] files = dir.listFiles();
        boolean anyMatches = false;
        if (files != null) {
            java.util.Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });
            for (File f : files) {
                if (f.getName().startsWith(".")) continue;
                TreeItem<File> item = new TreeItem<>(f);
                if (f.isDirectory()) {
                    boolean dirMatches = f.getName().toLowerCase().contains(query);
                    item.setExpanded(true);
                    boolean childrenMatch = buildFilteredTree(f, item, query);
                    if (dirMatches || childrenMatch) {
                        parent.getChildren().add(item);
                        anyMatches = true;
                    }
                } else {
                    if (f.getName().toLowerCase().contains(query)) {
                        parent.getChildren().add(item);
                        anyMatches = true;
                    }
                }
            }
        }
        return anyMatches;
    }

    private void selectFirstNodeStartingWith(String prefix) {
        TreeItem<File> root = treeView.getRoot();
        if (root == null) return;
        TreeItem<File> match = findNodeStartingWith(root, prefix);
        if (match != null) {
            treeView.getSelectionModel().select(match);
            int index = treeView.getRow(match);
            if (index >= 0) {
                treeView.scrollTo(index);
            }
        }
    }

    private TreeItem<File> findNodeStartingWith(TreeItem<File> item, String prefix) {
        if (item != treeView.getRoot() && item.getValue() != null) {
            String name = item.getValue().getName().toLowerCase();
            if (name.startsWith(prefix)) {
                return item;
            }
        }
        for (TreeItem<File> child : item.getChildren()) {
            TreeItem<File> match = findNodeStartingWith(child, prefix);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private void deleteFileRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFileRecursively(child);
                }
            }
        }
        file.delete();
    }

    private void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String getFileBaseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? filename : filename.substring(0, dot);
    }

    private String getLanguageForFile(File f) {
        String ext = getFileExtension(f);
        if ("xml".equalsIgnoreCase(ext) || "xsd".equalsIgnoreCase(ext)) return "xml";
        if ("json".equalsIgnoreCase(ext)) return "json";
        return "text";
    }

    private String getFileExtension(File f) {
        String n = f.getName();
        int i = n.lastIndexOf('.');
        return i > 0 ? n.substring(i + 1).toLowerCase() : "";
    }
}
