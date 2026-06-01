package com.tessera.ui;

import com.tessera.ui.components.MonacoEditorPane;
import com.tessera.ui.components.SuiKit;
import com.tessera.ui.components.ThemeManager;
import com.tessera.ui.components.UIFactory;
import com.tessera.util.DocumentConverter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Document Converter Studio
 * Converts Word, Excel, and PDF to Markdown for LLM consumption.
 * Supports multi-selection via checkboxes and bulk conversion with progress tracking.
 */
public class DocumentConverterStudioWindow {

    private final Stage stage;
    private File workspaceRoot;
    private File outputRoot;
    
    private TreeView<File> treeView;
    private CheckBoxTreeItem<File> rootItem;
    
    private TabPane mainTabPane;
    private WebView mdPreview;
    private MonacoEditorPane mdCodeEditor;
    private TextArea txtFileInfo;
    
    private ProgressBar progressBar;
    private Label lblProgressStatus;
    private Button btnConvert;
    
    private final com.tessera.ui.components.ConsolePane consoleArea;

    public DocumentConverterStudioWindow() {
        this(null);
    }

    public DocumentConverterStudioWindow(File baseWorkspace) {
        this.stage = new Stage();
        this.consoleArea = new com.tessera.ui.components.ConsolePane();
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DocumentConverterStudioWindow.class);
        
        if (baseWorkspace != null) {
            this.workspaceRoot = new File(baseWorkspace, "docs");
            this.outputRoot = new File(this.workspaceRoot, "output");
        } else {
            String savedRoot = prefs.get("workspaceRoot", System.getProperty("user.dir"));
            this.workspaceRoot = new File(savedRoot);
            String savedOutput = prefs.get("outputRoot", new File(workspaceRoot, "converted-markdown").getAbsolutePath());
            this.outputRoot = new File(savedOutput);
        }
    }

    public void show() {
        stage.setTitle("Sovereign Document Converter Studio - [ LLM FEED GENERATOR ]");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        ThemeManager.registerRoot(root);

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("editor-toolbar");

        Button btnWorkspace = SuiKit.createActionButton("Set Source", "fas-folder-open", "btn-workspace", this::selectWorkspace);
        Button btnOutput = SuiKit.createActionButton("Set Output", "fas-file-export", "btn-save", this::selectOutput);
        
        btnConvert = SuiKit.createActionButton("START CONVERSION", "fas-play", "btn-validate-studio", this::startConversion);
        btnConvert.setDisable(true);
        
        Button btnRefresh = UIFactory.createIconButton("fas-sync-alt", "Refresh Explorer");
        btnRefresh.setOnAction(e -> refreshTree());
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnHelp = SuiKit.createActionButton("Help", "fas-question-circle", "btn-manual", this::showHelp);

        toolBar.getItems().addAll(btnWorkspace, btnOutput, new Separator(), btnConvert, btnRefresh, spacer, btnHelp);
        root.setTop(toolBar);

        // --- Center Split ---
        SplitPane mainSplit = new SplitPane();
        mainSplit.setDividerPositions(0.3);

        // 1. Sidebar (Tree with Checkboxes)
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(10));
        sidebar.getStyleClass().add("studio-sidebar");
        
        Label lblExplorer = new Label("DOCUMENTS EXPLORER");
        lblExplorer.getStyleClass().add("studio-explorer-label");
        
        treeView = new TreeView<>();
        treeView.getStyleClass().add("sidebar-tree-view");
        treeView.setShowRoot(false);
        treeView.setCellFactory(tv -> new CheckBoxTreeCell<File>() {
            @Override
            public void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue().isFile()) {
                handleFilePreview(newVal.getValue());
            }
        });
        
        sidebar.getChildren().addAll(lblExplorer, treeView, new Label("CONSOLE"), consoleArea);
        consoleArea.setPrefHeight(250);

        // 2. Right Panel (Progress + Info + Markdown)
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        
        // Progress Section
        VBox progressBox = new VBox(5);
        progressBox.setPadding(new Insets(10));
        progressBox.setStyle("-fx-background-color: -sui-bg-secondary; -fx-background-radius: 8; -fx-border-color: -sui-border-color; -fx-border-radius: 8;");
        
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        lblProgressStatus = new Label("Ready. Select documents and click Start Conversion.");
        lblProgressStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: -sui-text-dim;");
        
        progressBox.getChildren().addAll(new Label("CONVERSION PROGRESS"), progressBar, lblProgressStatus);
        
        // Tabs
        mainTabPane = new TabPane();
        VBox.setVgrow(mainTabPane, Priority.ALWAYS);
        
        Tab tabPreview = new Tab("Markdown Preview", new FontIcon("fas-eye"));
        tabPreview.setClosable(false);
        mdPreview = new WebView();
        tabPreview.setContent(mdPreview);
        
        Tab tabCode = new Tab("Markdown Code", new FontIcon("fas-code"));
        tabCode.setClosable(false);
        mdCodeEditor = new MonacoEditorPane("markdown");
        tabCode.setContent(mdCodeEditor);
        
        Tab tabInfo = new Tab("File Details", new FontIcon("fas-info-circle"));
        tabInfo.setClosable(false);
        txtFileInfo = new TextArea();
        txtFileInfo.setEditable(false);
        txtFileInfo.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 13px; -fx-control-inner-background: -sui-bg-primary; -fx-text-fill: -sui-text-main;");
        tabInfo.setContent(txtFileInfo);
        
        mainTabPane.getTabs().addAll(tabPreview, tabCode, tabInfo);
        
        rightPanel.getChildren().addAll(progressBox, mainTabPane);

        mainSplit.getItems().addAll(sidebar, rightPanel);
        root.setCenter(mainSplit);

        Scene scene = new Scene(root, 1500, 950);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        
        refreshTree();
        log("INFO", "Document Converter Studio active. Select documents to convert.");
    }

    private void handleFilePreview(File file) {
        try {
            showMetadata(file);
            String md = convertToMarkdown(file);
            mdCodeEditor.setText(md);
            updatePreview(md);
        } catch (Exception e) {
            log("ERROR", "Preview failed: " + e.getMessage());
        }
    }

    private void showMetadata(File file) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String text = String.format(
            "FILE INFORMATION\n" +
            "================\n" +
            "Name:      %s\n" +
            "Type:      %s\n" +
            "Size:      %.2f KB\n" +
            "Modified:  %s\n" +
            "Path:      %s\n\n" +
            "Source Workspace: %s\n" +
            "Target Output:    %s",
            file.getName(),
            getFileType(file),
            file.length() / 1024.0,
            sdf.format(new Date(file.lastModified())),
            file.getAbsolutePath(),
            workspaceRoot.getAbsolutePath(),
            outputRoot.getAbsolutePath()
        );
        txtFileInfo.setText(text);
    }

    private String getFileType(File f) {
        String n = f.getName().toLowerCase();
        if (n.endsWith(".pdf")) return "PDF Document";
        if (n.endsWith(".docx")) return "Word Document (OOXML)";
        if (n.endsWith(".xlsx")) return "Excel Spreadsheet (OOXML)";
        return "Unknown";
    }

    private String convertToMarkdown(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".docx")) return DocumentConverter.convertDocx(file);
        if (name.endsWith(".xlsx")) return DocumentConverter.convertXlsx(file);
        if (name.endsWith(".pdf")) return DocumentConverter.convertPdf(file);
        return "# Unsupported Format\nOnly .docx, .xlsx, and .pdf are supported.";
    }

    private void updatePreview(String md) {
        org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder().build();
        org.commonmark.node.Node document = parser.parse(md);
        org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder().build();
        String body = renderer.render(document);
        
        String textColor = ThemeManager.getCurrentThemeClass().contains("light") ? "#1a1a1a" : "#d4d4d4";
        String bgColor = ThemeManager.getCurrentThemeClass().contains("light") ? "#ffffff" : "#1e1e1e";
        if (ThemeManager.getCurrentThemeClass().equals("theme-nordic")) {
            textColor = "#eceff4"; bgColor = "#2e3440";
        }

        String html = "<html><body style='background-color: " + bgColor + "; color: " + textColor + "; font-family: sans-serif; padding: 20px;'>" + body + "</body></html>";
        mdPreview.getEngine().loadContent(html);
    }

    private void selectWorkspace() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Source Workspace");
        dc.setInitialDirectory(workspaceRoot);
        File selected = dc.showDialog(stage);
        if (selected != null) {
            this.workspaceRoot = selected;
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DocumentConverterStudioWindow.class);
            prefs.put("workspaceRoot", selected.getAbsolutePath());
            refreshTree();
            log("INFO", "Source workspace updated: " + workspaceRoot.getAbsolutePath());
        }
    }

    private void selectOutput() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Output Directory");
        dc.setInitialDirectory(workspaceRoot);
        File selected = dc.showDialog(stage);
        if (selected != null) {
            this.outputRoot = selected;
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DocumentConverterStudioWindow.class);
            prefs.put("outputRoot", selected.getAbsolutePath());
            log("INFO", "Output directory set: " + outputRoot.getAbsolutePath());
        }
    }

    private void startConversion() {
        List<File> toConvert = new ArrayList<>();
        collectSelectedFiles(rootItem, toConvert);
        
        if (toConvert.isEmpty()) {
            log("WARN", "No files selected for conversion.");
            return;
        }

        btnConvert.setDisable(true);
        log("INFO", "Starting conversion of " + toConvert.size() + " files...");
        
        new Thread(() -> {
            int total = toConvert.size();
            for (int i = 0; i < total; i++) {
                File f = toConvert.get(i);
                final int index = i + 1;
                Platform.runLater(() -> {
                    progressBar.setProgress((double) index / total);
                    lblProgressStatus.setText("Converting (" + index + "/" + total + "): " + f.getName());
                });
                
                processFile(f);
            }
            Platform.runLater(() -> {
                lblProgressStatus.setText("Conversion Complete. " + total + " files processed.");
                log("INFO", "Bulk conversion finished.");
                btnConvert.setDisable(false);
            });
        }).start();
    }

    private void collectSelectedFiles(CheckBoxTreeItem<File> item, List<File> list) {
        if (item.isSelected() || item.isIndeterminate()) {
            File f = item.getValue();
            if (f.isFile()) {
                list.add(f);
            }
            for (TreeItem<File> child : item.getChildren()) {
                collectSelectedFiles((CheckBoxTreeItem<File>) child, list);
            }
        }
    }

    private void processFile(File f) {
        String name = f.getName().toLowerCase();
        if (!name.endsWith(".pdf") && !name.endsWith(".docx") && !name.endsWith(".xlsx")) return;
        
        try {
            String md = convertToMarkdown(f);
            String relativePath = workspaceRoot.toPath().relativize(f.toPath()).toString();
            String targetPath = relativePath.substring(0, relativePath.lastIndexOf(".")) + ".md";
            File outFile = new File(outputRoot, targetPath);
            outFile.getParentFile().mkdirs();
            Files.writeString(outFile.toPath(), md);
            log("INFO", "Generated: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            log("ERROR", "Failed to convert " + f.getName() + ": " + e.getMessage());
        }
    }

    private void refreshTree() {
        rootItem = new CheckBoxTreeItem<>(workspaceRoot);
        rootItem.setExpanded(true);
        rootItem.selectedProperty().addListener((obs, oldVal, newVal) -> updateButtonState());
        
        buildTree(workspaceRoot, rootItem);
        treeView.setRoot(rootItem);
        updateButtonState();
    }

    private void buildTree(File folder, CheckBoxTreeItem<File> parent) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory() || f.getName().endsWith(".pdf") || f.getName().endsWith(".docx") || f.getName().endsWith(".xlsx")) {
                CheckBoxTreeItem<File> item = new CheckBoxTreeItem<>(f);
                item.selectedProperty().addListener((obs, oldVal, newVal) -> updateButtonState());
                parent.getChildren().add(item);
                if (f.isDirectory()) buildTree(f, item);
            }
        }
    }

    private void updateButtonState() {
        List<File> selected = new ArrayList<>();
        if (rootItem != null) collectSelectedFiles(rootItem, selected);
        btnConvert.setDisable(selected.isEmpty());
    }

    private void log(String level, String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String color = level.equals("ERROR") ? "\033[31m" : (level.equals("WARN") ? "\033[33m" : "\033[32m");
        consoleArea.log(String.format("[%s] %s[%s]\033[0m %s\n", ts, color, level, msg));
    }

    private void showHelp() {
        new RouteBuilderHelpWindow("Document Converter Studio", null).show();
    }
}
