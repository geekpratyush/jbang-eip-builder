package com.tessera.ui;

import com.tessera.ui.components.MonacoEditorPane;
import com.tessera.ui.components.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * Enhanced YAML Editor Pane.
 * Now leverages the shared MonacoEditorPane component for perfect theme sync and high contrast.
 */
public class YamlEditorPane extends VBox {

    private MonacoEditorPane monacoPane;
    private javafx.scene.control.TabPane tabPane;
    private java.util.Map<File, String> fileCache = new java.util.HashMap<>();
    private java.util.List<File> openFiles = new java.util.ArrayList<>();
    private File currentFile = null;
    private com.tessera.lsp.LspManager lspManager;
    private Consumer<String> onTextChanged;
    private Runnable onFileSaved;

    private Button btnPlayFile;
    private Button btnStopFile;
    
    public Button getBtnPlayFile() { return btnPlayFile; }
    public Button getBtnStopFile() { return btnStopFile; }

    private Runnable onToggleDiagram;
    private java.util.function.BiConsumer<File, String> onPlayFile;
    private Runnable onStopFile;
    private Runnable onClose;
    private Consumer<File> onTabClosed;
    private HBox toolbar;

    public HBox getToolbar() {
        return toolbar;
    }

    public void setOnTabClosed(Consumer<File> onTabClosed) {
        this.onTabClosed = onTabClosed;
    }

    public YamlEditorPane(Consumer<String> onTextChanged, Runnable onFileSaved) {
        this.onTextChanged = onTextChanged;
        this.onFileSaved = onFileSaved;
        
        getStyleClass().add("editor-pane");
        ThemeManager.registerRoot(this);
        
        tabPane = new javafx.scene.control.TabPane();
        tabPane.getStyleClass().add("editor-tab-pane");
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (oldTab != null) {
                oldTab.setContent(null);
            }
            if (newTab != null && newTab.getUserData() instanceof File) {
                newTab.setContent(monacoPane);
                switchToFile((File) newTab.getUserData());
            } else if (newTab == null) {
                closeFileContent();
            }
        });

        this.toolbar = new HBox(10);
        toolbar.setPadding(new Insets(5));
        toolbar.getStyleClass().add("editor-toolbar");

        Button btnSave = new Button("", new FontIcon("fas-save"));
        btnSave.setTooltip(new Tooltip("Save"));
        btnSave.getStyleClass().addAll("editor-btn", "btn-save");
        btnSave.setOnAction(e -> saveFile());

        javafx.scene.layout.StackPane saveAllGraphic = new javafx.scene.layout.StackPane();
        FontIcon backSave = new FontIcon("fas-save");
        backSave.setOpacity(0.5);
        backSave.setTranslateX(-3);
        backSave.setTranslateY(-3);
        FontIcon frontSave = new FontIcon("fas-save");
        frontSave.setTranslateX(2);
        frontSave.setTranslateY(2);
        saveAllGraphic.getChildren().addAll(backSave, frontSave);

        Button btnSaveAll = new Button("", saveAllGraphic);
        btnSaveAll.setTooltip(new Tooltip("Save All"));
        btnSaveAll.getStyleClass().addAll("editor-btn", "btn-save-all");
        btnSaveAll.setOnAction(e -> saveAllFiles());

        Button btnSaveAs = new Button("", new FontIcon("fas-file-alt"));
        btnSaveAs.setTooltip(new Tooltip("Save As..."));
        btnSaveAs.getStyleClass().addAll("editor-btn", "btn-save-as");
        btnSaveAs.setOnAction(e -> saveFileAs());

        Button btnCopy = new Button("", new FontIcon("fas-copy"));
        btnCopy.setTooltip(new Tooltip("Copy Selection"));
        btnCopy.getStyleClass().addAll("editor-btn", "btn-copy-text");
        btnCopy.setOnAction(e -> copy());

        Button btnCopyAll = new Button("", new FontIcon("fas-clipboard-list"));
        btnCopyAll.setTooltip(new Tooltip("Copy All Content"));
        btnCopyAll.getStyleClass().addAll("editor-btn", "btn-copy-all-text");
        btnCopyAll.setOnAction(e -> {
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        });

        Button btnToggleDiagram = new Button("", new FontIcon("fas-columns"));
        btnToggleDiagram.setTooltip(new Tooltip("Toggle Diagram Panel"));
        btnToggleDiagram.getStyleClass().addAll("editor-btn");
        btnToggleDiagram.setOnAction(e -> {
            if (onToggleDiagram != null) onToggleDiagram.run();
        });



        btnPlayFile = new Button("", new FontIcon("fas-play"));
        btnPlayFile.setTooltip(new Tooltip("Play Current File"));
        btnPlayFile.getStyleClass().addAll("editor-btn", "btn-play-file");
        btnPlayFile.setOnAction(e -> {
            if (onPlayFile != null && currentFile != null) onPlayFile.accept(currentFile, "dev");
        });

        btnStopFile = new Button("", new FontIcon("fas-stop"));
        btnStopFile.setTooltip(new Tooltip("Stop Current File"));
        btnStopFile.getStyleClass().addAll("editor-btn", "btn-stop-file");
        btnStopFile.setDisable(true);
        btnStopFile.setOnAction(e -> {
            if (onStopFile != null) onStopFile.run();
        });

        toolbar.getChildren().addAll(btnSave, btnSaveAll, btnSaveAs, btnCopy, btnCopyAll, btnToggleDiagram);

        // Core Monaco Editor Component
        monacoPane = new MonacoEditorPane("yaml");
        monacoPane.setOnSave(this::saveFile);
        monacoPane.setOnContentChanged(text -> {
            if (currentFile != null) {
                fileCache.put(currentFile, text);
            }
            if (onTextChanged != null) onTextChanged.accept(text);
            if (lspManager != null) lspManager.updateDocument(text);
        });

        getChildren().addAll(tabPane);
    }

    public void setLspManager(com.tessera.lsp.LspManager lspManager) {
        this.lspManager = lspManager;
        if (this.lspManager != null) {
            this.lspManager.setDiagnosticsConsumer(diagnostics -> {
                Platform.runLater(() -> {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        String json = mapper.writeValueAsString(diagnostics.getDiagnostics());
                        monacoPane.showDiagnostics(json);
                    } catch(Exception e) {}
                });
            });
        }
    }

    public void setTheme(String themeName) {
        // MonacoEditorPane automatically responds to ThemeManager notifications.
    }

    public void setOnToggleDiagram(Runnable onToggleDiagram) { this.onToggleDiagram = onToggleDiagram; }
    public void setOnPlayFile(java.util.function.BiConsumer<File, String> onPlayFile) { this.onPlayFile = onPlayFile; }
    public void setOnStopFile(Runnable onStopFile) { this.onStopFile = onStopFile; }
    public void setOnClose(Runnable onClose) { this.onClose = onClose; }
    public File getCurrentFile() { return this.currentFile; }

    public void copy() { /* Monaco handles native context menu copy */ }
    public void cut() { /* Monaco handles native cut */ }
    public void paste() { /* Monaco handles native paste */ }
    public void selectAll() { /* Monaco handles native select all */ }
    public void undo() { /* Monaco handles native undo */ }
    public void redo() { /* Monaco handles native redo */ }

    public void setText(String text) {
        monacoPane.setText(text);
    }

    public String getText() {
        return monacoPane.getText();
    }

    public void loadFiles(java.util.List<File> files) {
        java.util.List<File> toRemove = new java.util.ArrayList<>(openFiles);
        toRemove.removeAll(files);
        
        java.util.List<File> toAdd = new java.util.ArrayList<>(files);
        toAdd.removeAll(openFiles);
        
        for (File f : toRemove) {
            fileCache.remove(f);
            openFiles.remove(f);
            tabPane.getTabs().removeIf(t -> f.equals(t.getUserData()));
        }
        
        for (File f : toAdd) {
            try {
                String content = Files.readString(f.toPath());
                fileCache.put(f, content);
                openFiles.add(f);
                javafx.scene.control.Tab tab = new javafx.scene.control.Tab(f.getName());
                tab.setUserData(f);
                tab.setClosable(true);
                tab.setOnClosed(e -> {
                    openFiles.remove(f);
                    fileCache.remove(f);
                    if (onTabClosed != null) onTabClosed.accept(f);
                });
                tabPane.getTabs().add(tab);
            } catch (IOException e) {}
        }
        
        if (!files.isEmpty()) {
            if (!files.contains(currentFile)) {
                for (javafx.scene.control.Tab t : tabPane.getTabs()) {
                    if (files.get(0).equals(t.getUserData())) {
                        tabPane.getSelectionModel().select(t);
                        break;
                    }
                }
            } else {
                switchToFile(currentFile);
            }
        } else {
            closeFileContent();
        }
    }

    private void switchToFile(File file) {
        if (file == null) return;
        currentFile = file;
        String content = fileCache.getOrDefault(file, "");
        
        String ext = "";
        int lastDot = file.getName().lastIndexOf('.');
        if (lastDot > 0) ext = file.getName().substring(lastDot + 1).toLowerCase();
        
        String lang = "plaintext";
        if (ext.equals("yaml") || ext.equals("yml")) lang = "yaml";
        else if (ext.equals("java")) lang = "java";
        else if (ext.equals("groovy")) lang = "groovy";
        
        monacoPane.setLanguage(lang);
        if (lspManager != null && lang.equals("yaml")) lspManager.setDocumentUri(file.toURI().toString());
        
        monacoPane.setText(content);
    }

    public void loadFile(File file) {
        if (file == null) {
            loadFiles(java.util.Collections.emptyList());
        } else {
            loadFiles(java.util.Collections.singletonList(file));
        }
    }

    public void closeFileContent() {
        currentFile = null;
        monacoPane.setText("");
        if (lspManager != null) lspManager.setDocumentUri("");
    }

    public void closeFile() {
        loadFiles(java.util.Collections.emptyList());
    }

    public void saveAllFiles() {
        if (currentFile != null) {
            fileCache.put(currentFile, monacoPane.getText());
        }
        for (File f : openFiles) {
            String content = fileCache.get(f);
            if (content != null) {
                try {
                    Files.writeString(f.toPath(), content);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (onFileSaved != null) onFileSaved.run();
    }

    public void saveFile() {
        if (currentFile == null) saveFileAs(); 
        else {
            fileCache.put(currentFile, monacoPane.getText());
            writeToFile(currentFile);
        }
    }

    private void saveFileAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save YAML Route");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML Files", "*.yaml", "*.yml"));
        File dir = new File(System.getProperty("user.dir"), "routes");
        if (!dir.exists()) dir.mkdirs();
        fileChooser.setInitialDirectory(dir);
        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (file != null) {
            currentFile = file;
            for (javafx.scene.control.Tab t : tabPane.getTabs()) {
                if (t.isSelected()) {
                    t.setText(file.getName());
                    t.setUserData(file);
                    break;
                }
            }
            writeToFile(file);
        }
    }

    private void writeToFile(File file) {
        try {
            Files.writeString(file.toPath(), getText());
            if (onFileSaved != null) onFileSaved.run();
        } catch (IOException ex) { ex.printStackTrace(); }
    }
}
