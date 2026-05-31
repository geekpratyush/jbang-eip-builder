package com.routebuilder.ui;

import com.routebuilder.ui.components.MonacoEditorPane;
import com.routebuilder.ui.components.ThemeManager;
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
    private Label title;
    private File currentFile = null;
    private com.routebuilder.lsp.LspManager lspManager;
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

    public YamlEditorPane(Consumer<String> onTextChanged, Runnable onFileSaved) {
        this.onTextChanged = onTextChanged;
        this.onFileSaved = onFileSaved;
        
        getStyleClass().add("editor-pane");
        ThemeManager.registerRoot(this);
        
        title = new Label("EDITOR: Untitled.yaml");
        title.getStyleClass().add("pane-title");

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(5));
        toolbar.getStyleClass().add("editor-toolbar");

        Button btnSave = new Button("", new FontIcon("fas-save"));
        btnSave.setTooltip(new Tooltip("Save"));
        btnSave.getStyleClass().addAll("editor-btn", "btn-save");
        btnSave.setOnAction(e -> saveFile());

        Button btnSaveAs = new Button("", new FontIcon("fas-file-alt"));
        btnSaveAs.setTooltip(new Tooltip("Save As..."));
        btnSaveAs.getStyleClass().addAll("editor-btn", "btn-save-as");
        btnSaveAs.setOnAction(e -> saveFileAs());

        Button btnDeploy = new Button("", new FontIcon("fas-cloud-upload-alt"));
        btnDeploy.setTooltip(new Tooltip("Deploy to REST API"));
        btnDeploy.getStyleClass().addAll("editor-btn", "btn-deploy");
        btnDeploy.setOnAction(e -> deployYaml());

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

        Button btnClose = new Button("", new FontIcon("fas-times"));
        btnClose.setTooltip(new Tooltip("Close Editor"));
        btnClose.getStyleClass().addAll("editor-btn");
        btnClose.setOnAction(e -> {
            if (onClose != null) onClose.run();
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

        toolbar.getChildren().addAll(btnSave, btnSaveAs, btnCopy, btnCopyAll, btnToggleDiagram, btnDeploy, new javafx.scene.control.Separator(), btnPlayFile, btnStopFile, new javafx.scene.control.Separator(), btnClose);

        // Core Monaco Editor Component
        monacoPane = new MonacoEditorPane("yaml");
        VBox.setVgrow(monacoPane, Priority.ALWAYS);
        monacoPane.setOnContentChanged(text -> {
            if (onTextChanged != null) onTextChanged.accept(text);
            if (lspManager != null) lspManager.updateDocument(text);
        });

        getChildren().addAll(title, toolbar, monacoPane);
    }

    public void setLspManager(com.routebuilder.lsp.LspManager lspManager) {
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

    public void loadFile(File file) {
        if (file == null) return;
        try {
            String content = Files.readString(file.toPath());
            currentFile = file;
            title.setText("EDITOR: " + file.getName());
            
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
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    public void closeFile() {
        currentFile = null;
        title.setText("EDITOR: Untitled.yaml");
        monacoPane.setText("");
        if (lspManager != null) lspManager.setDocumentUri("");
    }

    public void saveFile() { if (currentFile == null) saveFileAs(); else writeToFile(currentFile); }

    private void saveFileAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save YAML Route");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML Files", "*.yaml", "*.yml"));
        File dir = new File(System.getProperty("user.dir"), "routes");
        if (!dir.exists()) dir.mkdirs();
        fileChooser.setInitialDirectory(dir);
        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (file != null) { currentFile = file; title.setText("EDITOR: " + file.getName()); writeToFile(file); }
    }

    private void writeToFile(File file) {
        try {
            Files.writeString(file.toPath(), getText());
            if (onFileSaved != null) onFileSaved.run();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void deployYaml() {
        TextInputDialog dialog = new TextInputDialog("https://httpbin.org/post");
        RouteBuilderApp.themeDialog(dialog);
        dialog.setTitle("Deploy Route");
        dialog.setHeaderText("Deploy YAML Route via REST API");
        dialog.setContentText("Endpoint URL:");

        dialog.showAndWait().ifPresent(url -> {
            String yamlContent = getText();
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(url))
                            .header("Content-Type", "application/yaml")
                            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(yamlContent))
                            .build();
                    java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Response Code: " + response.statusCode(), javafx.scene.control.ButtonType.OK);
                        RouteBuilderApp.themeDialog(alert);
                        alert.setTitle("Deployment Status");
                        alert.showAndWait();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), javafx.scene.control.ButtonType.OK);
                        RouteBuilderApp.themeDialog(alert);
                        alert.setTitle("Deployment Failed");
                        alert.showAndWait();
                    });
                }
            });
        });
    }
}
