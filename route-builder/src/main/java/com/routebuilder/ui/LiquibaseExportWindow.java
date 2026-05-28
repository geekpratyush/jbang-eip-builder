package com.routebuilder.ui;

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
        public final StringProperty description;
        public final StringProperty filename;
        public final StringProperty path;
        public final BooleanProperty enabled;
        public final StringProperty details;
        public final String rawContent;

        public ExportItem(String id, String description, String filename, String path, boolean enabled, String details, String rawContent) {
            this.id = new SimpleStringProperty(id);
            this.description = new SimpleStringProperty(description);
            this.filename = new SimpleStringProperty(filename);
            this.path = new SimpleStringProperty(path);
            this.enabled = new SimpleBooleanProperty(enabled);
            this.details = new SimpleStringProperty(details);
            this.rawContent = rawContent;
        }

        public String getId() { return id.get(); }
        public String getDescription() { return description.get(); }
        public String getFilename() { return filename.get(); }
        public String getPath() { return path.get(); }
        public boolean isEnabled() { return enabled.get(); }
        public String getDetails() { return details.get(); }
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

        // --- Top Controls ---
        HBox topBox = new HBox(10);
        topBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblExportType = new Label("Export As:");
        lblExportType.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ComboBox<String> cmbExportType = new ComboBox<>();
        cmbExportType.getItems().addAll("SQL", "MONGODB", "File");
        cmbExportType.setValue("SQL");
        topBox.getChildren().addAll(lblExportType, cmbExportType);

        // --- Table View (Excel Grid) ---
        TableView<ExportItem> table = new TableView<>();
        table.setEditable(true);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ExportItem, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> data.getValue().id);
        colId.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colId.setPrefWidth(150);

        TableColumn<ExportItem, String> colDesc = new TableColumn<>("DESCRIPTION");
        colDesc.setCellValueFactory(data -> data.getValue().description);
        colDesc.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colDesc.setPrefWidth(200);

        TableColumn<ExportItem, String> colFile = new TableColumn<>("FILENAME");
        colFile.setCellValueFactory(data -> data.getValue().filename);
        colFile.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colFile.setPrefWidth(150);

        TableColumn<ExportItem, String> colPath = new TableColumn<>("PATH");
        colPath.setCellValueFactory(data -> data.getValue().path);
        colPath.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colPath.setPrefWidth(200);

        TableColumn<ExportItem, Boolean> colEnabled = new TableColumn<>("ENABLED");
        colEnabled.setCellValueFactory(data -> data.getValue().enabled);
        colEnabled.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(colEnabled));
        colEnabled.setPrefWidth(80);

        TableColumn<ExportItem, String> colDetails = new TableColumn<>("DETAILS");
        colDetails.setCellValueFactory(data -> data.getValue().details);
        colDetails.setPrefWidth(120);

        table.getColumns().addAll(colId, colDesc, colPath, colFile, colEnabled, colDetails);

        ObservableList<ExportItem> items = FXCollections.observableArrayList(item -> new javafx.beans.Observable[]{
            item.id, item.description, item.filename, item.path, item.enabled
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

        items.addListener((javafx.collections.ListChangeListener.Change<? extends ExportItem> c) -> {
            updateEditorContent(engine, cmbExportType.getValue(), isTransformer, items);
        });

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

        engine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == javafx.concurrent.Worker.State.SUCCEEDED) {
                updateEditorContent(engine, cmbExportType.getValue(), isTransformer, items);
            }
        });
        engine.loadContent(html);

        // Update editor on combo box change
        cmbExportType.setOnAction(e -> {
            String type = cmbExportType.getValue();
            if ("File".equals(type)) {
                webView.setVisible(false);
                webView.setManaged(false);
            } else {
                webView.setVisible(true);
                webView.setManaged(true);
                updateEditorContent(engine, type, isTransformer, items);
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
            if ("File".equals(type)) {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Select Folder to Export Files");
                File dir = chooser.showDialog(stage);
                if (dir != null) {
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
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Save Liquibase Changelog");
                if ("SQL".equals(type)) {
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                    chooser.setInitialFileName("changelog-export-sql.xml");
                } else {
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                    chooser.setInitialFileName("changelog-export-mongodb.xml");
                }
                
                File file = chooser.showSaveDialog(stage);
                if (file != null) {
                    try {
                        String currentContent = (String) engine.executeScript("window.getValue()");
                        Files.writeString(file.toPath(), currentContent);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Changelog saved successfully!");
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

        // Hide validate if not XML
        cmbExportType.valueProperty().addListener((obs, oldV, newV) -> {
            btnValidate.setVisible(!"File".equals(newV));
            btnValidate.setManaged(!"File".equals(newV));
        });

        btnBox.getChildren().addAll(btnValidate, btnSave);
        layout.getChildren().addAll(topBox, table, webView, btnBox);

        Scene scene = new Scene(layout, 1000, 700);
        try {
            scene.getStylesheets().add(LiquibaseExportWindow.class.getResource("/styles/main.css").toExternalForm());
            if (RouteBuilderApp.currentDynamicCssUri != null) {
                scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
            }
        } catch (Exception ignored) {}
        
        stage.setScene(scene);
        stage.show();
    }

    private static void updateEditorContent(WebEngine engine, String exportType, boolean isTransformer, java.util.List<ExportItem> items) {
        String xml = "";
        String tableName = isTransformer ? "TRANSFORMER_LOGIC" : "ROUTE_CONTENT";
        
        if ("SQL".equals(exportType)) {
            xml = buildSqlLiquibase(items, tableName);
        } else if ("MONGODB".equals(exportType)) {
            xml = buildMongoLiquibase(items, tableName);
        }
        
        try {
            String encoded = java.net.URLEncoder.encode(xml, "UTF-8").replace("+", "%20");
            engine.executeScript("if(window.setValue) window.setValue(decodeURIComponent('" + encoded + "')); else window.editorValue = decodeURIComponent('" + encoded + "');");
            engine.executeScript("if(window.editor && window.editor.getModel()) { monaco.editor.setModelLanguage(window.editor.getModel(), 'xml'); }");
        } catch (Exception ignored) {}
    }

    private static String buildSqlLiquibase(java.util.List<ExportItem> items, String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<databaseChangeLog\n");
        sb.append("    xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n");
        sb.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("    xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog\n");
        sb.append("        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd\">\n\n");
        sb.append("    <changeSet id=\"export-").append(tableName.toLowerCase()).append("-").append(System.currentTimeMillis()).append("\" author=\"studio\">\n");

        for (ExportItem item : items) {
            if (!item.isEnabled()) continue;
            sb.append("        <insert tableName=\"").append(tableName).append("\">\n");
            sb.append("            <column name=\"id\" value=\"").append(escapeXml(item.getId())).append("\"/>\n");
            sb.append("            <column name=\"description\" value=\"").append(escapeXml(item.getDescription())).append("\"/>\n");
            sb.append("            <column name=\"filename\" value=\"").append(escapeXml(item.getFilename())).append("\"/>\n");
            sb.append("            <column name=\"path\" value=\"").append(escapeXml(item.getPath())).append("\"/>\n");
            sb.append("            <column name=\"enabled\" valueBoolean=\"").append(item.isEnabled()).append("\"/>\n");
            sb.append("            <column name=\"details\" value=\"").append(escapeXml(item.getDetails())).append("\"/>\n");
            sb.append("            <column name=\"content\">\n");
            sb.append("                <![CDATA[\n").append(item.getRawContent()).append("\n                ]]>\n");
            sb.append("            </column>\n");
            sb.append("        </insert>\n");
        }

        sb.append("    </changeSet>\n");
        sb.append("</databaseChangeLog>\n");
        return sb.toString();
    }

    private static String buildMongoLiquibase(java.util.List<ExportItem> items, String collectionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<databaseChangeLog\n");
        sb.append("    xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n");
        sb.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("    xmlns:ext=\"http://www.liquibase.org/xml/ns/dbchangelog-ext\"\n");
        sb.append("    xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog\n");
        sb.append("        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd\n");
        sb.append("        http://www.liquibase.org/xml/ns/dbchangelog-ext\n");
        sb.append("        http://www.liquibase.org/xml/ns/dbchangelog-ext.xsd\">\n\n");
        sb.append("    <changeSet id=\"export-").append(collectionName.toLowerCase()).append("-").append(System.currentTimeMillis()).append("\" author=\"studio\">\n");

        for (ExportItem item : items) {
            if (!item.isEnabled()) continue;
            sb.append("        <ext:insertMany collectionName=\"").append(collectionName).append("\">\n");
            sb.append("            <ext:document>\n");
            sb.append("                <![CDATA[\n");
            
            JSONObject doc = new JSONObject();
            doc.put("_id", item.getId());
            doc.put("description", item.getDescription());
            doc.put("filename", item.getFilename());
            doc.put("path", item.getPath());
            doc.put("enabled", item.isEnabled());
            doc.put("details", item.getDetails());
            doc.put("content", item.getRawContent());
            
            sb.append(doc.toString(4)).append("\n");
            sb.append("                ]]>\n");
            sb.append("            </ext:document>\n");
            sb.append("        </ext:insertMany>\n");
        }

        sb.append("    </changeSet>\n");
        sb.append("</databaseChangeLog>\n");
        return sb.toString();
    }

    private static java.util.List<ExportItem> parseRoutes(File baseDir, Set<File> files) {
        java.util.List<ExportItem> results = new java.util.ArrayList<>();
        Pattern idPattern = Pattern.compile("(?i)^#\\s*ID\\s*:\\s*(.*)$", Pattern.MULTILINE);
        Pattern descPattern = Pattern.compile("(?i)^#\\s*Description\\s*:\\s*(.*)$", Pattern.MULTILINE);
        Pattern enabledPattern = Pattern.compile("(?i)^#\\s*Enabled\\s*:\\s*(true|false)$", Pattern.MULTILINE);
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

                boolean enabled = true;
                Matcher mEnabled = enabledPattern.matcher(content);
                if (mEnabled.find()) enabled = Boolean.parseBoolean(mEnabled.group(1).trim());

                String relPath = baseDir.toPath().toAbsolutePath().relativize(f.toPath().toAbsolutePath()).toString().replace("\\", "/");

                results.add(new ExportItem(id, description, f.getName(), relPath, enabled, "YAML Route", content));
                } catch (Exception e) {
                results.add(new ExportItem("Error", "Failed to parse", f.getName(), "", false, e.getMessage(), ""));
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
                results.add(new ExportItem(id, description, logicFileName, relPath, enabled, "Type: " + type, logicContent));
            } catch (Exception e) {
                results.add(new ExportItem("Error", "Failed to parse config", folder.getName(), "", false, e.getMessage(), ""));
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
}
