package com.tessera.ui;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiquibaseExportWindow {

    public static class ExportItem {
        public final StringProperty id;
        public final DoubleProperty version;
        public final StringProperty description;
        public final StringProperty filename;
        public final StringProperty path;
        public final BooleanProperty enabled;
        public final StringProperty details;
        public final StringProperty formatOrType;
        public final String rawContent;

        public ExportItem(String id, double version, String description, String filename, String path, 
                          boolean enabled, String details, String formatOrType, String rawContent) {
            this.id = new SimpleStringProperty(id);
            this.version = new SimpleDoubleProperty(version);
            this.description = new SimpleStringProperty(description);
            this.filename = new SimpleStringProperty(filename);
            this.path = new SimpleStringProperty(path);
            this.enabled = new SimpleBooleanProperty(enabled);
            this.details = new SimpleStringProperty(details);
            this.formatOrType = new SimpleStringProperty(formatOrType);
            this.rawContent = rawContent;
        }

        public String getId() { return id.get(); }
        public double getVersion() { return version.get(); }
        public String getDescription() { return description.get(); }
        public String getFilename() { return filename.get(); }
        public String getPath() { return path.get(); }
        public boolean isEnabled() { return enabled.get(); }
        public String getDetails() { return details.get(); }
        public String getFormatOrType() { return formatOrType.get(); }
        public String getRawContent() { return rawContent; }
    }

    public static void showForRoutes(File baseDir, Set<File> files) {
        showDialog("Liquibase Export - Routes", false, baseDir, files);
    }

    public static void showForTransformations(File baseDir, Set<File> folders) {
        showDialog("Liquibase Export - Transformations", true, baseDir, folders);
    }

    private static void showDialog(String title, boolean isTransformer, File baseDir, Set<File> targets) {
        Stage stage = new Stage();
        stage.setTitle(title);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getStyleClass().add("app-root");
        if (RouteBuilderApp.currentThemeClass != null) {
            layout.getStyleClass().add(RouteBuilderApp.currentThemeClass);
        }

        // --- Top Grid Controls ---
        GridPane topGrid = new GridPane();
        topGrid.setHgap(15);
        topGrid.setVgap(8);
        topGrid.setPadding(new Insets(0, 0, 10, 0));

        Label lblExportType = new Label("Export Target:");
        lblExportType.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ComboBox<String> cmbExportType = new ComboBox<>();
        cmbExportType.getItems().addAll(
            "SQL (Liquibase XML)", 
            "SQL (Liquibase YAML)", 
            "MONGODB (Liquibase XML)", 
            "MONGODB (Liquibase YAML)", 
            "Raw Files Only"
        );
        cmbExportType.setValue("SQL (Liquibase XML)");

        Label lblStyle = new Label("Liquibase Seeding Style:");
        lblStyle.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ComboBox<String> cmbStyle = new ComboBox<>();
        cmbStyle.getItems().addAll("Option A: Inline Content (CDATA)", "Option B: External Files (valueClobFile)");
        cmbStyle.setValue("Option A: Inline Content (CDATA)");

        Label lblTableName = new Label("Table/Collection Name:");
        lblTableName.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        TextField txtTableName = new TextField(isTransformer ? "camel_transformers" : "camel_routes");

        Label lblClobPrefix = new Label("Classpath Prefix:");
        lblClobPrefix.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        TextField txtClobPrefix = new TextField(isTransformer ? "db/changelog/seeds/transformers/" : "db/changelog/seeds/routes/");

        Label lblExportDir = new Label("Physical Export Dir:");
        lblExportDir.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        TextField txtExportDir = new TextField();
        txtExportDir.setPrefWidth(250);
        Button btnBrowseDir = new Button("Browse...");

        topGrid.add(lblExportType, 0, 0);
        topGrid.add(cmbExportType, 1, 0);
        topGrid.add(lblStyle, 2, 0);
        topGrid.add(cmbStyle, 3, 0);

        topGrid.add(lblTableName, 0, 1);
        topGrid.add(txtTableName, 1, 1);
        topGrid.add(lblClobPrefix, 2, 1);
        topGrid.add(txtClobPrefix, 3, 1);

        topGrid.add(lblExportDir, 0, 2);
        topGrid.add(txtExportDir, 1, 2, 2, 1);
        topGrid.add(btnBrowseDir, 3, 2);

        // Visibility settings initially
        lblClobPrefix.setVisible(false);
        txtClobPrefix.setVisible(false);
        lblExportDir.setVisible(false);
        txtExportDir.setVisible(false);
        btnBrowseDir.setVisible(false);

        // --- Table View (Excel Grid) ---
        TableView<ExportItem> table = new TableView<>();
        table.setEditable(true);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ExportItem, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> data.getValue().id);
        colId.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colId.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).id.set(t.getNewValue()));
        colId.setPrefWidth(120);

        TableColumn<ExportItem, Double> colVer = new TableColumn<>("VERSION");
        colVer.setCellValueFactory(data -> data.getValue().version.asObject());
        colVer.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colVer.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).version.set(t.getNewValue()));
        colVer.setPrefWidth(80);

        TableColumn<ExportItem, String> colDesc = new TableColumn<>("DESCRIPTION");
        colDesc.setCellValueFactory(data -> data.getValue().description);
        colDesc.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colDesc.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).description.set(t.getNewValue()));
        colDesc.setPrefWidth(150);

        TableColumn<ExportItem, String> colFile = new TableColumn<>("FILENAME");
        colFile.setCellValueFactory(data -> data.getValue().filename);
        colFile.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colFile.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).filename.set(t.getNewValue()));
        colFile.setPrefWidth(140);

        TableColumn<ExportItem, String> colFormatOrType = new TableColumn<>(isTransformer ? "TYPE" : "FORMAT");
        colFormatOrType.setCellValueFactory(data -> data.getValue().formatOrType);
        colFormatOrType.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colFormatOrType.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).formatOrType.set(t.getNewValue()));
        colFormatOrType.setPrefWidth(90);

        TableColumn<ExportItem, Boolean> colEnabled = new TableColumn<>("ENABLED");
        colEnabled.setCellValueFactory(data -> data.getValue().enabled);
        colEnabled.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(colEnabled));
        colEnabled.setPrefWidth(70);

        TableColumn<ExportItem, String> colDetails = new TableColumn<>("DETAILS");
        colDetails.setCellValueFactory(data -> data.getValue().details);
        colDetails.setPrefWidth(100);

        table.getColumns().addAll(colId, colVer, colDesc, colFile, colFormatOrType, colEnabled, colDetails);

        ObservableList<ExportItem> items = FXCollections.observableArrayList(item -> new javafx.beans.Observable[]{
            item.id, item.version, item.description, item.filename, item.path, item.enabled, item.formatOrType
        });
        if (isTransformer) {
            items.addAll(parseTransformations(baseDir, targets));
        } else {
            items.addAll(parseRoutes(baseDir, targets));
        }
        table.setItems(items);

        // --- Monaco Editor ---
        WebView webView = new WebView();
        RouteBuilderApp.installClipboardShortcuts(webView);
        WebEngine engine = webView.getEngine();
        VBox.setVgrow(webView, Priority.ALWAYS);

        String activeTheme = RouteBuilderApp.currentThemeClass != null ? RouteBuilderApp.currentThemeClass : "theme-vscode-dark";
        String editorBg = "#1e1e1e";
        if ("theme-intellij-light".equals(activeTheme)) editorBg = "#ffffff";
        else if ("theme-dracula".equals(activeTheme)) editorBg = "#282a36";
        else if ("theme-monokai".equals(activeTheme)) editorBg = "#272822";
        else if ("theme-hacker".equals(activeTheme)) editorBg = "#050505";

        String monacoBase = LiquibaseExportWindow.class.getResource("/monaco/vs/loader.js").toExternalForm();
        monacoBase = monacoBase.substring(0, monacoBase.lastIndexOf("/vs/loader.js"));

        String html = "<!DOCTYPE html><html><head><base href='" + monacoBase + "/'/><meta charset='UTF-8'><style>body{margin:0;padding:0;overflow:hidden;background-color:" + editorBg + ";}#editor{width:100vw;height:100vh;}</style></head><body><div id='editor'></div><script src='" + monacoBase + "/vs/loader.js'></script><script>\n" +
            "window.editorValue = ''; window.setValue = function(val) { window.editorValue = val; if(window.editor) window.editor.setValue(val); };\n" +
            "window.getValue = function() { return window.editor ? window.editor.getValue() : window.editorValue; };\n" +
            "require.config({ paths: { vs: '" + monacoBase + "/vs' }});\n" +
            "require(['vs/editor/editor.main'], function() {\n" +
            "  window.editor = monaco.editor.create(document.getElementById('editor'), { value: window.editorValue, language: 'xml', theme: '" + (activeTheme.contains("light") ? "vs" : "vs-dark") + "', automaticLayout: true, minimap: { enabled: false }, fontSize: 14, formatOnPaste: true, formatOnType: true });\n" +
            "});\n</script></body></html>";

        Runnable updateEditor = () -> {
            updateEditorContent(engine, cmbExportType.getValue(), cmbStyle.getValue(), 
                                txtTableName.getText(), txtClobPrefix.getText(), isTransformer, items);
        };

        engine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == javafx.concurrent.Worker.State.SUCCEEDED) {
                updateEditor.run();
            }
        });
        engine.loadContent(html);

        // Update editor on field changes
        cmbExportType.setOnAction(e -> {
            String type = cmbExportType.getValue();
            boolean isRaw = "Raw Files Only".equals(type);
            boolean isSql = type != null && type.startsWith("SQL (Liquibase");
            boolean isMongo = type != null && type.startsWith("MONGODB (Liquibase");

            lblStyle.setVisible(isSql);
            cmbStyle.setVisible(isSql);
            lblTableName.setVisible(isSql || isMongo);
            txtTableName.setVisible(isSql || isMongo);

            boolean showDir = isRaw || (isSql && "Option B: External Files (valueClobFile)".equals(cmbStyle.getValue()));
            lblClobPrefix.setVisible(isSql && "Option B: External Files (valueClobFile)".equals(cmbStyle.getValue()));
            txtClobPrefix.setVisible(isSql && "Option B: External Files (valueClobFile)".equals(cmbStyle.getValue()));
            lblExportDir.setVisible(showDir);
            txtExportDir.setVisible(showDir);
            btnBrowseDir.setVisible(showDir);

            if (isRaw) {
                webView.setVisible(false);
                webView.setManaged(false);
            } else {
                webView.setVisible(true);
                webView.setManaged(true);
                updateEditor.run();
            }
        });

        cmbStyle.setOnAction(e -> {
            String targetType = cmbExportType.getValue();
            boolean isSql = targetType != null && targetType.startsWith("SQL (Liquibase");
            boolean isOptB = "Option B: External Files (valueClobFile)".equals(cmbStyle.getValue());
            boolean showDir = isSql && isOptB;
            lblClobPrefix.setVisible(showDir);
            txtClobPrefix.setVisible(showDir);
            lblExportDir.setVisible(showDir);
            txtExportDir.setVisible(showDir);
            btnBrowseDir.setVisible(showDir);
            updateEditor.run();
        });

        txtTableName.textProperty().addListener((obs, oldVal, newVal) -> updateEditor.run());
        txtClobPrefix.textProperty().addListener((obs, oldVal, newVal) -> updateEditor.run());
        
        items.addListener((javafx.collections.ListChangeListener.Change<? extends ExportItem> c) -> {
            updateEditor.run();
        });

        btnBrowseDir.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Physical Export Directory for Seed Files");
            File dir = chooser.showDialog(stage);
            if (dir != null) {
                txtExportDir.setText(dir.getAbsolutePath());
            }
        });

        // --- Bottom Buttons ---
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnValidate = new Button("Validate XML", new org.kordamp.ikonli.javafx.FontIcon("fas-check-double"));
        btnValidate.getStyleClass().addAll("toolbar-btn", "btn-validate");
        btnValidate.setOnAction(e -> {
            try {
                String currentContent = (String) engine.executeScript("window.getValue()");
                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                builder.parse(new java.io.ByteArrayInputStream(currentContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "XML syntax is valid and well-formed!");
                alert.setHeaderText("Validation Successful");
                RouteBuilderApp.themeDialog(alert);
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Syntax Error:\n" + ex.getMessage());
                alert.setHeaderText("Validation Failed");
                RouteBuilderApp.themeDialog(alert);
                alert.showAndWait();
            }
        });

        Button btnSave = new Button("Export...", new org.kordamp.ikonli.javafx.FontIcon("fas-save"));
        btnSave.getStyleClass().addAll("toolbar-btn", "btn-save");
        btnSave.setOnAction(e -> {
            String type = cmbExportType.getValue();
            if ("Raw Files Only".equals(type)) {
                String dirPath = txtExportDir.getText().trim();
                File dir = null;
                if (dirPath.isEmpty()) {
                    DirectoryChooser chooser = new DirectoryChooser();
                    chooser.setTitle("Select Folder to Export Files");
                    dir = chooser.showDialog(stage);
                } else {
                    dir = new File(dirPath);
                }
                
                if (dir != null) {
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    try {
                        for (ExportItem item : items) {
                            if (!item.isEnabled()) continue;
                            File outFile = new File(dir, item.getFilename());
                            Files.writeString(outFile.toPath(), item.getRawContent());
                        }
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Files exported successfully to " + dir.getAbsolutePath());
                        RouteBuilderApp.themeDialog(alert);
                        alert.showAndWait();
                        stage.close();
                    } catch (Exception ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export: " + ex.getMessage());
                        RouteBuilderApp.themeDialog(alert);
                        alert.showAndWait();
                    }
                }
            } else {
                // Liquibase Export
                boolean isSqlXml = "SQL (Liquibase XML)".equals(type);
                boolean isSqlYaml = "SQL (Liquibase YAML)".equals(type);
                boolean isMongoYaml = "MONGODB (Liquibase YAML)".equals(type);
                boolean isSql = isSqlXml || isSqlYaml;
                boolean isOptB = isSql && "Option B: External Files (valueClobFile)".equals(cmbStyle.getValue());
                
                File exportDir = null;
                if (isOptB) {
                    String dirPath = txtExportDir.getText().trim();
                    if (dirPath.isEmpty()) {
                        DirectoryChooser dirChooser = new DirectoryChooser();
                        dirChooser.setTitle("Select Physical Export Directory for Seed Files");
                        exportDir = dirChooser.showDialog(stage);
                        if (exportDir != null) {
                            txtExportDir.setText(exportDir.getAbsolutePath());
                        } else {
                            return; // User cancelled
                        }
                    } else {
                        exportDir = new File(dirPath);
                    }
                }
                
                String timestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
                String typeTag = isTransformer ? "transformer" : "routebuilder";

                FileChooser chooser = new FileChooser();
                chooser.setTitle("Save Liquibase Changelog");
                if (isSqlYaml || isMongoYaml) {
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML Files", "*.yaml", "*.yml"));
                    String suffix = isSqlYaml ? "changelog-export-sql.yaml" : "changelog-export-mongodb.yaml";
                    chooser.setInitialFileName(timestamp + "-" + typeTag + "-" + suffix);
                } else {
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                    String suffix = isSqlXml ? "changelog-export-sql.xml" : "changelog-export-mongodb.xml";
                    chooser.setInitialFileName(timestamp + "-" + typeTag + "-" + suffix);
                }
                
                File changelogFile = chooser.showSaveDialog(stage);
                if (changelogFile != null) {
                    try {
                        // 1. Save Changelog
                        String currentContent = (String) engine.executeScript("window.getValue()");
                        Files.writeString(changelogFile.toPath(), currentContent);
                        
                        // 2. Save seed files if Option B
                        if (isOptB && exportDir != null) {
                            if (!exportDir.exists()) {
                                exportDir.mkdirs();
                            }
                            for (ExportItem item : items) {
                                if (!item.isEnabled()) continue;
                                File seedFile = new File(exportDir, item.getFilename());
                                Files.writeString(seedFile.toPath(), item.getRawContent());
                            }
                        }
                        
                        String msg = "Changelog saved successfully!";
                        if (isOptB && exportDir != null) {
                            msg += "\nSeed files also exported to: " + exportDir.getAbsolutePath();
                        }
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
                        RouteBuilderApp.themeDialog(alert);
                        alert.showAndWait();
                        stage.close();
                    } catch (Exception ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save: " + ex.getMessage());
                        RouteBuilderApp.themeDialog(alert);
                        alert.showAndWait();
                    }
                }
            }
        });

        cmbExportType.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isXml = newV != null && newV.contains("XML");
            btnValidate.setVisible(isXml);
            btnValidate.setManaged(isXml);
        });

        Button btnHelp = new Button("Help Guide", new org.kordamp.ikonli.javafx.FontIcon("fas-question-circle"));
        btnHelp.getStyleClass().add("toolbar-btn");
        btnHelp.setOnAction(e -> new RouteBuilderHelpWindow("Advanced Tools", "Export").show());

        Button btnCopyToRemote = new Button("Copy to Remote...", new org.kordamp.ikonli.javafx.FontIcon("fas-share-square"));
        btnCopyToRemote.getStyleClass().addAll("toolbar-btn", "btn-deploy");
        btnCopyToRemote.setOnAction(evt -> {
            String type = cmbExportType.getValue();
            boolean isRaw = "Raw Files Only".equals(type);
            String changelogContent = isRaw ? "" : (String) engine.executeScript("window.getValue()");

            java.util.Set<RemoteDeployWindow.DeployItem> deployItems = new java.util.HashSet<>();

            // 1. Add the changelog file if not raw
            if (!isRaw) {
                String timestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
                String typeTag = isTransformer ? "transformer" : "routebuilder";
                String filename = timestamp + "-" + typeTag + "-changelog-export.xml";
                if (type.contains("YAML")) {
                    filename = timestamp + "-" + typeTag + "-changelog-export.yaml";
                }
                deployItems.add(new RemoteDeployWindow.DeployItem("changelog", filename, type.contains("YAML") ? "yaml" : "xml", 1.0, changelogContent, false, null));
            }

            // 2. Add the seed files
            for (ExportItem item : items) {
                if (item.isEnabled()) {
                    deployItems.add(new RemoteDeployWindow.DeployItem(item.getId(), item.getFilename(), item.getFormatOrType(), item.getVersion(), item.getRawContent(), false, null));
                }
            }

            if (deployItems.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "No files available to copy. Make sure at least one item is enabled.");
                RouteBuilderApp.themeDialog(alert);
                alert.showAndWait();
                return;
            }

            RemoteDeployWindow.showForFiles("Copy Liquibase Artifacts to Remote Server", baseDir, deployItems);
        });

        btnBox.getChildren().addAll(btnHelp, btnValidate, btnCopyToRemote, btnSave);
        layout.getChildren().addAll(topGrid, table, webView, btnBox);

        com.tessera.ui.components.ThemeManager.registerRoot(layout);
        Scene scene = new Scene(layout, 1050, 750);
        try {
            scene.getStylesheets().add(LiquibaseExportWindow.class.getResource("/styles/main.css").toExternalForm());
            if (RouteBuilderApp.currentDynamicCssUri != null) {
                scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
            }
        } catch (Exception ignored) {}
        
        stage.setScene(scene);
        stage.show();
    }

    private static void updateEditorContent(WebEngine engine, String exportType, String style, 
                                            String tableName, String clobPrefix, boolean isTransformer, 
                                            java.util.List<ExportItem> items) {
        String content = "";
        String language = "xml";
        
        if ("SQL (Liquibase XML)".equals(exportType)) {
            content = buildSqlLiquibase(items, tableName, style, clobPrefix, isTransformer);
            language = "xml";
        } else if ("SQL (Liquibase YAML)".equals(exportType)) {
            content = buildSqlYamlLiquibase(items, tableName, style, clobPrefix, isTransformer);
            language = "yaml";
        } else if ("MONGODB (Liquibase XML)".equals(exportType)) {
            content = buildMongoLiquibase(items, tableName, isTransformer);
            language = "xml";
        } else if ("MONGODB (Liquibase YAML)".equals(exportType)) {
            content = buildMongoYamlLiquibase(items, tableName, isTransformer);
            language = "yaml";
        }
        
        try {
            String encoded = java.net.URLEncoder.encode(content, "UTF-8").replace("+", "%20");
            engine.executeScript("if(window.setValue) window.setValue(decodeURIComponent('" + encoded + "')); else window.editorValue = decodeURIComponent('" + encoded + "');");
            engine.executeScript("if(window.editor && window.editor.getModel()) { monaco.editor.setModelLanguage(window.editor.getModel(), '" + language + "'); }");
        } catch (Exception ignored) {}
    }

    private static String buildSqlLiquibase(java.util.List<ExportItem> items, String tableName, 
                                            String style, String clobPrefix, boolean isTransformer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<databaseChangeLog\n");
        sb.append("    xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n");
        sb.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("    xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog\n");
        sb.append("        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd\">\n\n");
        sb.append("    <changeSet id=\"export-").append(tableName.toLowerCase()).append("-").append(System.currentTimeMillis()).append("\" author=\"studio\">\n");

        boolean isOptB = "Option B: External Files (valueClobFile)".equals(style);
        String prefix = clobPrefix != null ? clobPrefix : "";
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }

        for (ExportItem item : items) {
            if (!item.isEnabled()) continue;
            sb.append("        <insert tableName=\"").append(tableName).append("\">\n");
            
            if (isTransformer) {
                sb.append("            <column name=\"id\" value=\"").append(escapeXml(item.getId())).append("\"/>\n");
                sb.append("            <column name=\"type\" value=\"").append(escapeXml(item.getFormatOrType())).append("\"/>\n");
                sb.append("            <column name=\"version\" valueNumeric=\"").append(String.format("%.2f", item.getVersion())).append("\"/>\n");
                sb.append("            <column name=\"file_name\" value=\"").append(escapeXml(item.getFilename())).append("\"/>\n");
                if (isOptB) {
                    sb.append("            <column name=\"file_content\" valueClobFile=\"").append(prefix).append(escapeXml(item.getFilename())).append("\"/>\n");
                } else {
                    sb.append("            <column name=\"file_content\">\n");
                    sb.append("                <![CDATA[\n").append(item.getRawContent()).append("\n                ]]>\n");
                    sb.append("            </column>\n");
                }
                sb.append("            <column name=\"enabled\" valueBoolean=\"").append(item.isEnabled()).append("\"/>\n");
            } else {
                sb.append("            <column name=\"route_id\" value=\"").append(escapeXml(item.getId())).append("\"/>\n");
                sb.append("            <column name=\"version\" valueNumeric=\"").append(String.format("%.2f", item.getVersion())).append("\"/>\n");
                sb.append("            <column name=\"route_format\" value=\"").append(escapeXml(item.getFormatOrType())).append("\"/>\n");
                if (isOptB) {
                    sb.append("            <column name=\"route_content\" valueClobFile=\"").append(prefix).append(escapeXml(item.getFilename())).append("\"/>\n");
                } else {
                    sb.append("            <column name=\"route_content\">\n");
                    sb.append("                <![CDATA[\n").append(item.getRawContent()).append("\n                ]]>\n");
                    sb.append("            </column>\n");
                }
                sb.append("            <column name=\"enabled\" valueBoolean=\"").append(item.isEnabled()).append("\"/>\n");
            }
            
            sb.append("        </insert>\n");
        }

        sb.append("    </changeSet>\n");
        sb.append("</databaseChangeLog>\n");
        return sb.toString();
    }

    private static String buildSqlYamlLiquibase(java.util.List<ExportItem> items, String tableName, 
                                                String style, String clobPrefix, boolean isTransformer) {
        StringBuilder sb = new StringBuilder();
        sb.append("databaseChangeLog:\n");
        sb.append("  - changeSet:\n");
        sb.append("      id: export-").append(tableName.toLowerCase()).append("-").append(System.currentTimeMillis()).append("\n");
        sb.append("      author: studio\n");
        sb.append("      changes:\n");

        boolean isOptB = "Option B: External Files (valueClobFile)".equals(style);
        String prefix = clobPrefix != null ? clobPrefix : "";
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }

        for (ExportItem item : items) {
            if (!item.isEnabled()) continue;
            sb.append("        - insert:\n");
            sb.append("            tableName: ").append(tableName).append("\n");
            sb.append("            columns:\n");
            
            if (isTransformer) {
                sb.append("              - column:\n");
                sb.append("                  name: id\n");
                sb.append("                  value: \"").append(escapeYaml(item.getId())).append("\"\n");
                sb.append("              - column:\n");
                sb.append("                  name: type\n");
                sb.append("                  value: \"").append(escapeYaml(item.getFormatOrType())).append("\"\n");
                sb.append("              - column:\n");
                sb.append("                  name: version\n");
                sb.append("                  valueNumeric: ").append(String.format("%.2f", item.getVersion())).append("\n");
                sb.append("              - column:\n");
                sb.append("                  name: file_name\n");
                sb.append("                  value: \"").append(escapeYaml(item.getFilename())).append("\"\n");
                sb.append("              - column:\n");
                sb.append("                  name: file_content\n");
                if (isOptB) {
                    sb.append("                  valueClobFile: \"").append(prefix).append(escapeYaml(item.getFilename())).append("\"\n");
                    sb.append("                  encoding: \"UTF-8\"\n");
                } else {
                    sb.append("                  value: |\n");
                    sb.append(indentText(item.getRawContent(), 20)).append("\n");
                }
                sb.append("              - column:\n");
                sb.append("                  name: enabled\n");
                sb.append("                  valueBoolean: ").append(item.isEnabled()).append("\n");
            } else {
                sb.append("              - column:\n");
                sb.append("                  name: route_id\n");
                sb.append("                  value: \"").append(escapeYaml(item.getId())).append("\"\n");
                sb.append("              - column:\n");
                sb.append("                  name: version\n");
                sb.append("                  valueNumeric: ").append(String.format("%.2f", item.getVersion())).append("\n");
                sb.append("              - column:\n");
                sb.append("                  name: route_format\n");
                sb.append("                  value: \"").append(escapeYaml(item.getFormatOrType())).append("\"\n");
                sb.append("              - column:\n");
                sb.append("                  name: route_content\n");
                if (isOptB) {
                    sb.append("                  valueClobFile: \"").append(prefix).append(escapeYaml(item.getFilename())).append("\"\n");
                    sb.append("                  encoding: \"UTF-8\"\n");
                } else {
                    sb.append("                  value: |\n");
                    sb.append(indentText(item.getRawContent(), 20)).append("\n");
                }
                sb.append("              - column:\n");
                sb.append("                  name: enabled\n");
                sb.append("                  valueBoolean: ").append(item.isEnabled()).append("\n");
            }
        }
        return sb.toString();
    }

    private static String buildMongoLiquibase(java.util.List<ExportItem> items, String collectionName, boolean isTransformer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<databaseChangeLog\n");
        sb.append("    xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n");
        sb.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("    xmlns:ext=\"http://www.liquibase.org/xml/ns/mongodb\"\n");
        sb.append("    xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog\n");
        sb.append("        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd\n");
        sb.append("        http://www.liquibase.org/xml/ns/mongodb\n");
        sb.append("        http://www.liquibase.org/xml/ns/mongodb/liquibase-mongodb-latest.xsd\">\n\n");
        sb.append("    <changeSet id=\"export-").append(collectionName.toLowerCase()).append("-").append(System.currentTimeMillis()).append("\" author=\"studio\">\n");

        for (ExportItem item : items) {
            if (!item.isEnabled()) continue;
            sb.append("        <ext:insertMany collectionName=\"").append(collectionName).append("\">\n");
            sb.append("            <ext:documents><![CDATA[[\n");
            
            JSONObject doc = new JSONObject();
            JSONObject compositeId = new JSONObject();
            
            if (isTransformer) {
                compositeId.put("id", item.getId());
                compositeId.put("version", item.getVersion());
                doc.put("_id", compositeId);
                doc.put("id", item.getId());
                doc.put("type", item.getFormatOrType());
                doc.put("version", item.getVersion());
                doc.put("file_name", item.getFilename());
                doc.put("content", item.getRawContent());
                doc.put("enabled", item.isEnabled());
            } else {
                compositeId.put("route_id", item.getId());
                compositeId.put("version", item.getVersion());
                doc.put("_id", compositeId);
                doc.put("route_id", item.getId());
                doc.put("version", item.getVersion());
                doc.put("content", item.getRawContent());
                doc.put("route_format", item.getFormatOrType());
                doc.put("enabled", item.isEnabled());
            }
            
            String docJson = doc.toString(4);
            String indentedDocJson = indentText(docJson, 16).trim();
            sb.append("                ").append(indentedDocJson).append("\n");
            sb.append("            ]]]></ext:documents>\n");
            sb.append("        </ext:insertMany>\n");
        }

        sb.append("    </changeSet>\n");
        sb.append("</databaseChangeLog>\n");
        return sb.toString();
    }

    private static String buildMongoYamlLiquibase(java.util.List<ExportItem> items, String collectionName, boolean isTransformer) {
        StringBuilder sb = new StringBuilder();
        sb.append("databaseChangeLog:\n");
        sb.append("  - changeSet:\n");
        sb.append("      id: export-").append(collectionName.toLowerCase()).append("-").append(System.currentTimeMillis()).append("\n");
        sb.append("      author: studio\n");
        sb.append("      changes:\n");

        for (ExportItem item : items) {
            if (!item.isEnabled()) continue;
            sb.append("        - insertMany:\n");
            sb.append("            collectionName: ").append(collectionName).append("\n");
            sb.append("            documents:\n");
            sb.append("              - _id:\n");
            if (isTransformer) {
                sb.append("                  id: \"").append(escapeYaml(item.getId())).append("\"\n");
                sb.append("                  version: ").append(String.format("%.2f", item.getVersion())).append("\n");
                sb.append("                id: \"").append(escapeYaml(item.getId())).append("\"\n");
                sb.append("                type: \"").append(escapeYaml(item.getFormatOrType())).append("\"\n");
                sb.append("                version: ").append(String.format("%.2f", item.getVersion())).append("\n");
                sb.append("                file_name: \"").append(escapeYaml(item.getFilename())).append("\"\n");
                sb.append("                content: |\n");
                sb.append(indentText(item.getRawContent(), 18)).append("\n");
                sb.append("                enabled: ").append(item.isEnabled()).append("\n");
            } else {
                sb.append("                  route_id: \"").append(escapeYaml(item.getId())).append("\"\n");
                sb.append("                  version: ").append(String.format("%.2f", item.getVersion())).append("\n");
                sb.append("                route_id: \"").append(escapeYaml(item.getId())).append("\"\n");
                sb.append("                version: ").append(String.format("%.2f", item.getVersion())).append("\n");
                sb.append("                content: |\n");
                sb.append(indentText(item.getRawContent(), 18)).append("\n");
                sb.append("                route_format: \"").append(escapeYaml(item.getFormatOrType())).append("\"\n");
                sb.append("                enabled: ").append(item.isEnabled()).append("\n");
            }
        }
        return sb.toString();
    }

    private static java.util.List<ExportItem> parseRoutes(File baseDir, Set<File> files) {
        java.util.List<ExportItem> results = new java.util.ArrayList<>();
        Pattern idPattern = Pattern.compile("(?i)^#\\s*ID\\s*:\\s*(.*)$", Pattern.MULTILINE);
        Pattern descPattern = Pattern.compile("(?i)^#\\s*Description\\s*:\\s*(.*)$", Pattern.MULTILINE);
        Pattern enabledPattern = Pattern.compile("(?i)^#\\s*Enabled\\s*:\\s*(true|false)$", Pattern.MULTILINE);
        Pattern versionPattern = Pattern.compile("(?i)^#\\s*Version\\s*:\\s*([0-9.]+)", Pattern.MULTILINE);
        Pattern fallbackIdPattern = Pattern.compile("id:\\s*[\"']?([^\"'\\n]+)[\"']?");

        for (File f : files) {
            if (!f.isFile()) continue;
            try {
                String content = Files.readString(f.toPath());
                String id = f.getName().replace(".camel.yaml", "").replace(".yaml", "");
                Matcher mId = idPattern.matcher(content);
                if (mId.find()) id = mId.group(1).trim();
                else {
                    Matcher fbId = fallbackIdPattern.matcher(content);
                    if (fbId.find()) id = fbId.group(1).trim();
                }

                String description = "";
                Matcher mDesc = descPattern.matcher(content);
                if (mDesc.find()) description = mDesc.group(1).trim();
                if (description.isEmpty()) description = id;

                double version = 1.0;
                Matcher mVer = versionPattern.matcher(content);
                if (mVer.find()) {
                    try {
                        version = Double.parseDouble(mVer.group(1).trim());
                    } catch (Exception ignored) {}
                }

                boolean enabled = true;
                Matcher mEnabled = enabledPattern.matcher(content);
                if (mEnabled.find()) enabled = Boolean.parseBoolean(mEnabled.group(1).trim());

                String format = "yaml";
                String nameLower = f.getName().toLowerCase();
                if (nameLower.endsWith(".xml")) {
                    format = "xml";
                } else if (nameLower.endsWith(".java")) {
                    format = "java";
                }

                String relPath = baseDir.toPath().toAbsolutePath().relativize(f.toPath().toAbsolutePath()).toString().replace("\\", "/");

                results.add(new ExportItem(id, version, description, f.getName(), relPath, enabled, "YAML Route", format, content));
            } catch (Exception e) {
                results.add(new ExportItem("Error", 1.0, "Failed to parse", f.getName(), "", false, e.getMessage(), "yaml", ""));
            }
        }
        return results;
    }

    private static java.util.List<ExportItem> parseTransformations(File baseDir, Set<File> folders) {
        java.util.List<ExportItem> results = new java.util.ArrayList<>();

        for (File folder : folders) {
            if (!folder.isDirectory()) continue;
            File configFile = new File(folder, "transformation.json");
            if (!configFile.exists()) continue;

            try {
                String configContent = Files.readString(configFile.toPath());
                JSONObject config = new JSONObject(configContent);
                
                String defaultId = "";
                if (folder.getParentFile() != null) {
                    defaultId = folder.getParentFile().getName() + "-" + folder.getName();
                } else {
                    defaultId = folder.getName();
                }
                String id = config.optString("id", defaultId);
                String type = config.optString("type", "unknown");
                String description = config.optString("description", "");
                if (description.isEmpty()) description = id;
                double version = config.optDouble("version", 1.0);
                boolean enabled = config.optBoolean("enabled", true);
                
                String logicFileName = "";
                org.json.JSONArray logicArr = config.optJSONArray("logic");
                if (logicArr != null && logicArr.length() > 0) {
                    logicFileName = logicArr.getJSONObject(0).optString("file", "");
                } else {
                    JSONObject logicCfg = config.optJSONObject("logic");
                    if (logicCfg != null) {
                        logicFileName = logicCfg.optString("file", "");
                    }
                }

                String logicContent = "";
                if (!logicFileName.isEmpty()) {
                    File logicFile = new File(folder, logicFileName);
                    if (logicFile.exists()) {
                        logicContent = Files.readString(logicFile.toPath());
                    }
                }

                String relPath = baseDir.toPath().toAbsolutePath().relativize(folder.toPath().toAbsolutePath()).toString().replace("\\", "/");
                results.add(new ExportItem(id, version, description, logicFileName, relPath, enabled, "Type: " + type, type, logicContent));
            } catch (Exception e) {
                results.add(new ExportItem("Error", 1.0, "Failed to parse config", folder.getName(), "", false, e.getMessage(), "unknown", ""));
            }
        }
        return results;
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String escapeYaml(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static String indentText(String text, int spaces) {
        if (text == null) return "";
        String indent = " ".repeat(spaces);
        String[] lines = text.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(indent).append(lines[i]);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
