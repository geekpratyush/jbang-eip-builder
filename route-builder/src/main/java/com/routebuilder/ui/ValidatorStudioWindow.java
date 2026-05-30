package com.routebuilder.ui;

import javafx.application.Platform;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.json.JSONObject;
import org.json.JSONArray;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

public class ValidatorStudioWindow {
    public static final List<ValidatorStudioWindow> activeInstances = new ArrayList<>();

    private final Stage stage;
    private File workspaceRoot;

    // Sidebar Components
    private TreeView<String> treeView;
    private TextArea consoleArea;

    // Toolbar Components
    private ComboBox<String> cmbValidatorType;
    private ComboBox<String> studioThemeBox;
    private RadioButton radStandard;
    private RadioButton radEnhanced;

    // Monaco Editor components
    private WebView webViewSource;
    private WebView webViewSchema;
    private WebView webViewResult;

    private WebEngine engineSource;
    private WebEngine engineSchema;
    private WebEngine engineResult;

    private Label lblSourceTitle;
    private Label lblSchemaTitle;

    // State Variables
    private String currentThemeName = RouteBuilderApp.currentThemeName;
    private ValidationMapping activeMapping = null;
    private final Map<TreeItem<String>, ValidationMapping> treeItemMappingMap = new HashMap<>();
    private final List<ValidationMapping> mappingsList = new ArrayList<>();

    public static class ValidationMapping {
        public String name;
        public String messagePath;
        public String schemaPath;
        public String type;

        public ValidationMapping(String name, String messagePath, String schemaPath, String type) {
            this.name = name;
            this.messagePath = messagePath;
            this.schemaPath = schemaPath;
            this.type = type;
        }
    }

    public ValidatorStudioWindow() {
        activeInstances.add(this);
        this.stage = new Stage();
        this.workspaceRoot = new File(System.getProperty("user.dir"), "validator-workspace");
        initializeWorkspace();
    }

    public ValidatorStudioWindow(boolean forTestOnly) {
        this.stage = null;
        this.workspaceRoot = null;
    }

    private void initializeWorkspace() {
        initializeWorkspace(this.workspaceRoot);
    }

    public static void initializeWorkspace(File workspaceRoot) {
        if (!workspaceRoot.exists()) {
            workspaceRoot.mkdirs();
        }
        createSampleFiles(workspaceRoot);
    }

    public static void createSampleFiles(File workspaceRoot) {
        try {
            ValidatorStudioWindow dummy = new ValidatorStudioWindow(true);

            // Schemas
            File xsdDir = new File(workspaceRoot, "schemas/xsd");
            xsdDir.mkdirs();
            writeString(new File(xsdDir, "invoice-schema.xsd"), dummy.getInvoiceXsd());

            File jsonSchemaDir = new File(workspaceRoot, "schemas/json-schema");
            jsonSchemaDir.mkdirs();
            writeString(new File(jsonSchemaDir, "customer-schema.json"), dummy.getCustomerJsonSchema());
            writeString(new File(jsonSchemaDir, "config-schema.json"), dummy.getConfigJsonSchema());

            File isoDir = new File(workspaceRoot, "schemas/iso20022");
            isoDir.mkdirs();
            writeString(new File(isoDir, "pacs008-schema.xsd"), dummy.getPacs008Xsd());

            File flatfileDir = new File(workspaceRoot, "schemas/flatfile");
            flatfileDir.mkdirs();
            writeString(new File(flatfileDir, "fixedwidth-schema.json"), dummy.getFixedWidthSchema());

            File csvDir = new File(workspaceRoot, "schemas/csv");
            csvDir.mkdirs();
            writeString(new File(csvDir, "transactions-metadata.json"), dummy.getCsvwMetadata());

            // Messages
            File xmlMsgDir = new File(workspaceRoot, "messages/xml");
            xmlMsgDir.mkdirs();
            writeString(new File(xmlMsgDir, "invoice-valid.xml"), dummy.getInvoiceValidXml());
            writeString(new File(xmlMsgDir, "invoice-invalid.xml"), dummy.getInvoiceInvalidXml());

            File jsonMsgDir = new File(workspaceRoot, "messages/json");
            jsonMsgDir.mkdirs();
            writeString(new File(jsonMsgDir, "customer-valid.json"), dummy.getCustomerValidJson());
            writeString(new File(jsonMsgDir, "customer-invalid.json"), dummy.getCustomerInvalidJson());

            File yamlMsgDir = new File(workspaceRoot, "messages/yaml");
            yamlMsgDir.mkdirs();
            writeString(new File(yamlMsgDir, "config-valid.yaml"), dummy.getConfigValidYaml());
            writeString(new File(yamlMsgDir, "config-invalid.yaml"), dummy.getConfigInvalidYaml());

            File csvMsgDir = new File(workspaceRoot, "messages/csv");
            csvMsgDir.mkdirs();
            writeString(new File(csvMsgDir, "transactions-valid.csv"), dummy.getTransactionsValidCsv());
            writeString(new File(csvMsgDir, "transactions-invalid.csv"), dummy.getTransactionsInvalidCsv());

            File flatMsgDir = new File(workspaceRoot, "messages/flatfile");
            flatMsgDir.mkdirs();
            writeString(new File(flatMsgDir, "fixedwidth-valid.txt"), dummy.getFixedWidthValidTxt());

            File mtStdDir = new File(workspaceRoot, "messages/mt/standard");
            mtStdDir.mkdirs();
            writeString(new File(mtStdDir, "mt103-valid.txt"), dummy.getMt103ValidTxt());
            writeString(new File(mtStdDir, "mt103-invalid.txt"), dummy.getMt103InvalidTxt());
            writeString(new File(mtStdDir, "mt202-valid.txt"), dummy.getMt202ValidTxt());
            writeString(new File(mtStdDir, "mt940-valid.txt"), dummy.getMt940ValidTxt());

            File mtEnhDir = new File(workspaceRoot, "messages/mt/enhanced");
            mtEnhDir.mkdirs();
            writeString(new File(mtEnhDir, "mt103-valid-enhanced.txt"), dummy.getMt103ValidEnhancedTxt());
            writeString(new File(mtEnhDir, "mt103-invalid-enhanced.txt"), dummy.getMt103InvalidEnhancedTxt());

            File isoMsgDir = new File(workspaceRoot, "messages/iso20022");
            isoMsgDir.mkdirs();
            writeString(new File(isoMsgDir, "pacs008-valid.xml"), dummy.getPacs008ValidXml());
            writeString(new File(isoMsgDir, "pacs008-invalid.xml"), dummy.getPacs008InvalidXml());

            File valDir = new File(workspaceRoot, "validators");
            valDir.mkdirs();
            writeString(new File(valDir, "custom-mt-rules.json"), dummy.getCustomMtRulesJson());

            String defaultMappings = "{\n" +
                "  \"mappings\": [\n" +
                "    { \"name\": \"Invoice XML (Valid)\", \"messagePath\": \"messages/xml/invoice-valid.xml\", \"schemaPath\": \"schemas/xsd/invoice-schema.xsd\", \"type\": \"XML + XSD\" },\n" +
                "    { \"name\": \"Invoice XML (Invalid)\", \"messagePath\": \"messages/xml/invoice-invalid.xml\", \"schemaPath\": \"schemas/xsd/invoice-schema.xsd\", \"type\": \"XML + XSD\" },\n" +
                "    { \"name\": \"Customer JSON (Valid)\", \"messagePath\": \"messages/json/customer-valid.json\", \"schemaPath\": \"schemas/json-schema/customer-schema.json\", \"type\": \"JSON + Schema\" },\n" +
                "    { \"name\": \"Customer JSON (Invalid)\", \"messagePath\": \"messages/json/customer-invalid.json\", \"schemaPath\": \"schemas/json-schema/customer-schema.json\", \"type\": \"JSON + Schema\" },\n" +
                "    { \"name\": \"App Config YAML (Valid)\", \"messagePath\": \"messages/yaml/config-valid.yaml\", \"schemaPath\": \"schemas/json-schema/config-schema.json\", \"type\": \"YAML + Schema\" },\n" +
                "    { \"name\": \"App Config YAML (Invalid)\", \"messagePath\": \"messages/yaml/config-invalid.yaml\", \"schemaPath\": \"schemas/json-schema/config-schema.json\", \"type\": \"YAML + Schema\" },\n" +
                "    { \"name\": \"Swift MT103 (Valid)\", \"messagePath\": \"messages/mt/standard/mt103-valid.txt\", \"schemaPath\": \"\", \"type\": \"SWIFT MT Message\" },\n" +
                "    { \"name\": \"Swift MT103 (Invalid)\", \"messagePath\": \"messages/mt/standard/mt103-invalid.txt\", \"schemaPath\": \"\", \"type\": \"SWIFT MT Message\" },\n" +
                "    { \"name\": \"Swift MT103 Enhanced (Valid)\", \"messagePath\": \"messages/mt/enhanced/mt103-valid-enhanced.txt\", \"schemaPath\": \"validators/custom-mt-rules.json\", \"type\": \"SWIFT MT Message\" },\n" +
                "    { \"name\": \"Swift MT103 Enhanced (Invalid)\", \"messagePath\": \"messages/mt/enhanced/mt103-invalid-enhanced.txt\", \"schemaPath\": \"validators/custom-mt-rules.json\", \"type\": \"SWIFT MT Message\" },\n" +
                "    { \"name\": \"Transactions CSV (Valid)\", \"messagePath\": \"messages/csv/transactions-valid.csv\", \"schemaPath\": \"schemas/csv/transactions-metadata.json\", \"type\": \"CSV + CSVW\" },\n" +
                "    { \"name\": \"Transactions CSV (Invalid)\", \"messagePath\": \"messages/csv/transactions-invalid.csv\", \"schemaPath\": \"schemas/csv/transactions-metadata.json\", \"type\": \"CSV + CSVW\" },\n" +
                "    { \"name\": \"FixedWidth Flat File (Valid)\", \"messagePath\": \"messages/flatfile/fixedwidth-valid.txt\", \"schemaPath\": \"schemas/flatfile/fixedwidth-schema.json\", \"type\": \"Flat File\" },\n" +
                "    { \"name\": \"ISO20022 Pacs008 (Valid)\", \"messagePath\": \"messages/iso20022/pacs008-valid.xml\", \"schemaPath\": \"schemas/iso20022/pacs008-schema.xsd\", \"type\": \"ISO 20022 MX\" },\n" +
                "    { \"name\": \"ISO20022 Pacs008 (Invalid)\", \"messagePath\": \"messages/iso20022/pacs008-invalid.xml\", \"schemaPath\": \"schemas/iso20022/pacs008-schema.xsd\", \"type\": \"ISO 20022 MX\" }\n" +
                "  ]\n" +
                "}";
            writeString(new File(workspaceRoot, "validation-mapping.json"), defaultMappings);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeString(File f, String s) throws IOException {
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            Files.writeString(f.toPath(), s);
        }
    }

    public void show() {
        stage.setTitle("Validation Studio - Rules, Schemas & Messages Validator");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.getStyleClass().add(RouteBuilderApp.currentThemeClass);

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("editor-toolbar");

        Button btnValidate = new Button("Validate", new FontIcon("fas-check-double"));
        btnValidate.getStyleClass().addAll("toolbar-btn", "btn-validate");
        btnValidate.setOnAction(e -> runValidation());

        Button btnSave = new Button("Save Scenario Content", new FontIcon("fas-save"));
        btnSave.getStyleClass().addAll("toolbar-btn", "btn-save");
        btnSave.setOnAction(e -> saveCurrentContent());

        cmbValidatorType = new ComboBox<>();
        cmbValidatorType.getItems().addAll("XML + XSD", "JSON + Schema", "YAML + Schema", "SWIFT MT Message", "ISO 20022 MX", "CSV + CSVW", "Flat File");
        cmbValidatorType.setValue("XML + XSD");
        cmbValidatorType.setOnAction(e -> updateEditorLanguages(cmbValidatorType.getValue()));

        ToggleGroup modeGroup = new ToggleGroup();
        radStandard = new RadioButton("Standard");
        radStandard.setToggleGroup(modeGroup);
        radStandard.setSelected(true);
        radStandard.getStyleClass().add("radio-theme-standard");
        radStandard.setOnAction(e -> log("INFO", "SWIFT mode set to Standard."));

        radEnhanced = new RadioButton("Enhanced (+Rules)");
        radEnhanced.setToggleGroup(modeGroup);
        radEnhanced.getStyleClass().add("radio-theme-enhanced");
        radEnhanced.setOnAction(e -> log("INFO", "SWIFT mode set to Enhanced (incorporating custom JSON rules)."));

        studioThemeBox = new ComboBox<>();
        studioThemeBox.getItems().addAll("VSCode Dark", "IntelliJ Light", "Dracula", "Monokai", "Hacker");
        studioThemeBox.setValue(RouteBuilderApp.currentThemeName);
        studioThemeBox.setOnAction(e -> RouteBuilderApp.setGlobalTheme(studioThemeBox.getValue()));

        Button btnHelp = new Button("Help Guide", new FontIcon("fas-question-circle"));
        btnHelp.getStyleClass().addAll("toolbar-btn", "btn-manual");
        btnHelp.setOnAction(e -> new RouteBuilderHelpWindow("Validation Studio", null).show());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(
            btnValidate, btnSave, new Separator(),
            new Label("Validation Type:"), cmbValidatorType, new Separator(),
            new Label("SWIFT Mode:"), radStandard, radEnhanced, new Separator(),
            spacer, btnHelp, new Separator(), studioThemeBox
        );
        root.setTop(toolBar);

        // --- Center Split Layout ---
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.setDividerPositions(0.20);

        // --- Sidebar (Left Split) ---
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(8));
        sidebar.getStyleClass().add("studio-sidebar");

        Label lblExplorer = new Label("VALIDATION HISTORY / SCENARIOS");
        lblExplorer.getStyleClass().add("studio-explorer-label");
        lblExplorer.setMaxWidth(Double.MAX_VALUE);

        treeView = new TreeView<>();
        treeView.getStyleClass().add("sidebar-tree-view");
        treeView.setShowRoot(false);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        treeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null && treeItemMappingMap.containsKey(item)) {
                    loadValidationMapping(treeItemMappingMap.get(item));
                }
            }
        });

        setupTreeContextMenu();

        Label lblConsole = new Label("CONSOLE");
        lblConsole.getStyleClass().add("studio-explorer-label");

        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setWrapText(true);
        consoleArea.setPrefHeight(220);
        consoleArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px; -fx-control-inner-background: #0f0f11; -fx-text-fill: #e1e1e6;");

        sidebar.getChildren().addAll(lblExplorer, treeView, new Separator(), lblConsole, consoleArea);

        // --- Editors Panel (Right Split) ---
        SplitPane editorsSplit = new SplitPane();
        editorsSplit.setDividerPositions(0.33, 0.66);

        // Left Section: Source Message (Data)
        VBox paneSource = new VBox(4);
        VBox.setVgrow(paneSource, Priority.ALWAYS);
        HBox headerSource = new HBox(8);
        headerSource.setPadding(new Insets(6));
        headerSource.getStyleClass().add("editor-header");
        headerSource.setAlignment(Pos.CENTER_LEFT);
        headerSource.setStyle("-fx-background-color: #252526;");
        Label lblSourceIcon = new Label("", new FontIcon("fas-file-code"));
        lblSourceTitle = new Label("Source Message (Data) - None");
        lblSourceTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        headerSource.getChildren().addAll(lblSourceIcon, lblSourceTitle);

        webViewSource = new WebView();
        RouteBuilderApp.installClipboardShortcuts(webViewSource);
        VBox.setVgrow(webViewSource, Priority.ALWAYS);
        engineSource = webViewSource.getEngine();
        paneSource.getChildren().addAll(headerSource, webViewSource);

        // Middle Section: Schema / Rules
        VBox paneSchema = new VBox(4);
        VBox.setVgrow(paneSchema, Priority.ALWAYS);
        HBox headerSchema = new HBox(8);
        headerSchema.setPadding(new Insets(6));
        headerSchema.getStyleClass().add("editor-header");
        headerSchema.setAlignment(Pos.CENTER_LEFT);
        headerSchema.setStyle("-fx-background-color: #252526;");
        Label lblSchemaIcon = new Label("", new FontIcon("fas-project-diagram"));
        lblSchemaTitle = new Label("Schema / Rules / Context - None");
        lblSchemaTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        
        Region schemaHeaderSpacer = new Region();
        HBox.setHgrow(schemaHeaderSpacer, Priority.ALWAYS);
        
        Button btnUpdateSchema = new Button("Overwrite Schema...", new FontIcon("fas-upload"));
        btnUpdateSchema.getStyleClass().addAll("toolbar-btn", "btn-save");
        btnUpdateSchema.setTooltip(new Tooltip("Overwrite active schema file with a local file"));
        btnUpdateSchema.setOnAction(e -> {
            if (activeMapping == null) {
                showError("No Scenario Active", "Please select a scenario from the explorer first.");
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Schema/Rules File to Overwrite");
            File selectedFile = chooser.showOpenDialog(stage);
            if (selectedFile != null) {
                try {
                    String content = Files.readString(selectedFile.toPath());
                    setEditorText(engineSchema, content);
                    
                    if (activeMapping.schemaPath.isEmpty()) {
                        String sub = activeMapping.type.toLowerCase().replace(" + ", "_").replace(" ", "_");
                        File schemaDir = new File(workspaceRoot, "schemas/" + sub);
                        schemaDir.mkdirs();
                        File schemaFile = new File(schemaDir, selectedFile.getName());
                        Files.writeString(schemaFile.toPath(), content);
                        activeMapping.schemaPath = workspaceRoot.toURI().relativize(schemaFile.toURI()).getPath();
                        
                        updateMappingInJson(activeMapping);
                        refreshTree();
                    } else {
                        File schemaFile = new File(workspaceRoot, activeMapping.schemaPath);
                        schemaFile.getParentFile().mkdirs();
                        Files.writeString(schemaFile.toPath(), content);
                    }
                    lblSchemaTitle.setText("Schema / Rules / Context - " + new File(activeMapping.schemaPath).getName());
                    log("INFO", "Schema overwritten from " + selectedFile.getName());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Schema file updated and overwritten successfully!", ButtonType.OK);
                    alert.setTitle("Schema Overwritten");
                    alert.setHeaderText(null);
                    alert.showAndWait();
                } catch (Exception ex) {
                    showError("Upload Error", ex.getMessage());
                }
            }
        });
        
        headerSchema.getChildren().addAll(lblSchemaIcon, lblSchemaTitle, schemaHeaderSpacer, btnUpdateSchema);

        webViewSchema = new WebView();
        RouteBuilderApp.installClipboardShortcuts(webViewSchema);
        VBox.setVgrow(webViewSchema, Priority.ALWAYS);
        engineSchema = webViewSchema.getEngine();
        paneSchema.getChildren().addAll(headerSchema, webViewSchema);

        // Right Section: Results / Errors
        VBox paneResult = new VBox(4);
        VBox.setVgrow(paneResult, Priority.ALWAYS);
        HBox headerResult = new HBox(8);
        headerResult.setPadding(new Insets(6));
        headerResult.getStyleClass().add("editor-header");
        headerResult.setAlignment(Pos.CENTER_LEFT);
        headerResult.setStyle("-fx-background-color: #252526;");
        Label lblResultIcon = new Label("", new FontIcon("fas-poll-h"));
        Label lblResultTitle = new Label("Validation Results / Report");
        lblResultTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        headerResult.getChildren().addAll(lblResultIcon, lblResultTitle);

        webViewResult = new WebView();
        RouteBuilderApp.installClipboardShortcuts(webViewResult);
        VBox.setVgrow(webViewResult, Priority.ALWAYS);
        engineResult = webViewResult.getEngine();
        paneResult.getChildren().addAll(headerResult, webViewResult);

        editorsSplit.getItems().addAll(paneSource, paneSchema, paneResult);
        horizontalSplit.getItems().addAll(sidebar, editorsSplit);

        root.setCenter(horizontalSplit);

        Scene scene = new Scene(root, 1500, 950);
        scene.getStylesheets().add(ValidatorStudioWindow.class.getResource("/styles/main.css").toExternalForm());
        if (RouteBuilderApp.currentDynamicCssUri != null) {
            scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
        }
        stage.setScene(scene);

        RouteBuilderApp.themedRoots.add(root);
        stage.setOnHidden(e -> {
            activeInstances.remove(this);
            RouteBuilderApp.themedRoots.remove(root);
        });

        // Shortcuts
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
            () -> saveCurrentContent()
        );
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.F5),
            () -> runValidation()
        );

        stage.setMaximized(true);
        stage.show();

        // Monaco Initializers
        setupMonaco(webViewSource, engineSource, "xml", "<!-- Choose a validation scenario from the left tree panel, or right-click to add a new validation pair -->");
        setupMonaco(webViewSchema, engineSchema, "xml", "<!-- Schema definition / Rules definition will load here -->");
        setupMonaco(webViewResult, engineResult, "markdown", "# Validation Studio\n" +
            "Double-click a scenario in the left history tree to load it.\n" +
            "Modify contents dynamically, and click **Validate** to view standard/enhanced output.");

        log("INFO", "Validation Studio workspace loaded.");
        refreshTree();
    }

    private void log(String level, String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String out = String.format("[%s] [%s] %s\n", ts, level, msg);
        Platform.runLater(() -> {
            consoleArea.appendText(out);
            consoleArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void setTheme(String themeName) {
        this.currentThemeName = themeName;
        String themeClass = "theme-" + themeName.toLowerCase().replace(" ", "-");
        Platform.runLater(() -> {
            if (studioThemeBox != null && !themeName.equals(studioThemeBox.getValue())) {
                studioThemeBox.setValue(themeName);
            }
            applyMonacoTheme(engineSource, themeClass);
            applyMonacoTheme(engineSchema, themeClass);
            applyMonacoTheme(engineResult, themeClass);
        });
    }

    private void applyMonacoTheme(WebEngine engine, String themeClass) {
        if (engine != null) {
            try {
                String bg = "#1e1e1e";
                if ("theme-intellij-light".equals(themeClass)) bg = "#ffffff";
                else if ("theme-dracula".equals(themeClass)) bg = "#282a36";
                else if ("theme-monokai".equals(themeClass)) bg = "#272822";
                else if ("theme-hacker".equals(themeClass)) bg = "#050505";

                engine.executeScript("if(window.editor) { monaco.editor.setTheme('" + themeClass + "'); document.body.style.backgroundColor = '" + bg + "'; }");
            } catch (Exception ignored) {}
        }
    }

    private void setupMonaco(WebView wv, WebEngine engine, String language, String initialValue) {
        String monacoBase = getClass().getResource("/monaco/vs/loader.js").toExternalForm();
        monacoBase = monacoBase.substring(0, monacoBase.lastIndexOf("/vs/loader.js"));

        String activeTheme = RouteBuilderApp.currentThemeClass;
        String editorBg = "#1e1e1e";
        if ("theme-intellij-light".equals(activeTheme)) editorBg = "#ffffff";
        else if ("theme-dracula".equals(activeTheme)) editorBg = "#282a36";
        else if ("theme-monokai".equals(activeTheme)) editorBg = "#272822";
        else if ("theme-hacker".equals(activeTheme)) editorBg = "#050505";

        String html = "<!DOCTYPE html><html><head><base href='" + monacoBase + "/'/><meta charset='UTF-8'><style>body{margin:0;padding:0;overflow:hidden;background-color:" + editorBg + ";}#editor{width:100vw;height:100vh;}</style></head><body><div id='editor'></div><script src='" + monacoBase + "/vs/loader.js'></script><script>\n" +
            "window.editorValue = ''; window.setValue = function(val) { window.editorValue = val; if(window.editor) window.editor.setValue(val); };\n" +
            "window.getValue = function() { return window.editor ? window.editor.getValue() : window.editorValue; };\n" +
            "window.setLanguage = function(lang) {\n" +
            "  if (window.editor) {\n" +
            "    var model = window.editor.getModel();\n" +
            "    monaco.editor.setModelLanguage(model, lang);\n" +
            "  }\n" +
            "};\n" +
            "window.getSelection = function() { if(!window.editor) return ''; var sel = window.editor.getSelection(); return window.editor.getModel().getValueInRange(sel); };\n" +
            "require.config({ paths: { vs: '" + monacoBase + "/vs' }});\n" +
            "require(['vs/editor/editor.main'], function() {\n" +
            "  monaco.languages.register({ id: 'swift-mt' });\n" +
            "  monaco.languages.setMonarchTokensProvider('swift-mt', { tokenizer: { root: [ [/{[1-5]:/, 'metatag'], [/}/, 'metatag'], [/^:[0-9A-Z]{2,3}:/, 'keyword'], [/-}/, 'metatag'], [/\\n:[0-9A-Z]{2,3}:/, 'keyword'] ] } });\n" +
            "  monaco.editor.defineTheme('theme-vscode-dark', { base: 'vs-dark', inherit: true, rules: [ { token: 'keyword', foreground: '569cd6', fontStyle: 'bold' }, { token: 'metatag', foreground: 'ce9178' } ], colors: { 'editor.background': '#1e1e1e' } });\n" +
            "  monaco.editor.defineTheme('theme-intellij-light', { base: 'vs', inherit: true, rules: [ { token: 'keyword', foreground: '0000ff', fontStyle: 'bold' }, { token: 'metatag', foreground: 'a31515' } ], colors: { 'editor.background': '#ffffff' } });\n" +
            "  monaco.editor.defineTheme('theme-dracula', { base: 'vs-dark', inherit: true, rules: [ { token: 'keyword', foreground: 'ff79c6', fontStyle: 'bold' }, { token: 'metatag', foreground: 'bd93f9' } ], colors: { 'editor.background': '#282a36' } });\n" +
            "  monaco.editor.defineTheme('theme-monokai', { base: 'vs-dark', inherit: true, rules: [ { token: 'keyword', foreground: 'f92672', fontStyle: 'bold' }, { token: 'metatag', foreground: 'ae81ff' } ], colors: { 'editor.background': '#272822' } });\n" +
            "  monaco.editor.defineTheme('theme-hacker', { base: 'hc-black', inherit: true, rules: [ { token: 'keyword', foreground: '00ff00', fontStyle: 'bold' }, { token: 'metatag', foreground: '00ff00' } ], colors: { 'editor.background': '#050505' } });\n" +
            "  window.editor = monaco.editor.create(document.getElementById('editor'), { value: window.editorValue, language: '" + ("text".equals(language) ? "swift-mt" : language) + "', theme: '" + activeTheme + "', automaticLayout: true, minimap: { enabled: false }, fontSize: 13 });\n" +
            "  if (window.editorValue) window.editor.setValue(window.editorValue);\n" +
            "});\n</script></body></html>";

        engine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == javafx.concurrent.Worker.State.SUCCEEDED) {
                setEditorText(engine, initialValue);
            }
        });
        engine.loadContent(html);
    }

    private void setEditorText(WebEngine engine, String text) {
        if (text == null) text = "";
        final String finalT = text;
        Platform.runLater(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(finalT, "UTF-8").replace("+", "%20");
                engine.executeScript("if(window.setValue) window.setValue(decodeURIComponent('" + encoded + "')); else window.editorValue = decodeURIComponent('" + encoded + "');");
            } catch (Exception ignored) {}
        });
    }

    private String getEditorText(WebEngine engine) {
        try {
            Object result = engine.executeScript("window.getValue()");
            return (result instanceof String) ? (String) result : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void updateEditorLanguages(String type) {
        String sourceLang = "plaintext";
        String schemaLang = "json";

        switch (type) {
            case "XML + XSD":
            case "ISO 20022 MX":
                sourceLang = "xml";
                schemaLang = "xml";
                break;
            case "JSON + Schema":
                sourceLang = "json";
                schemaLang = "json";
                break;
            case "YAML + Schema":
                sourceLang = "yaml";
                schemaLang = "json";
                break;
            case "SWIFT MT Message":
                sourceLang = "swift-mt";
                schemaLang = "json";
                break;
            case "CSV + CSVW":
                sourceLang = "csv";
                schemaLang = "json";
                break;
            case "Flat File":
                sourceLang = "plaintext";
                schemaLang = "json";
                break;
        }

        final String srcL = sourceLang;
        final String schL = schemaLang;

        Platform.runLater(() -> {
            try {
                engineSource.executeScript("window.setLanguage('" + srcL + "')");
                engineSchema.executeScript("window.setLanguage('" + schL + "')");
            } catch (Exception ignored) {}
        });
    }

    private void refreshTree() {
        treeView.setRoot(new TreeItem<>("Root"));
        treeItemMappingMap.clear();
        mappingsList.clear();

        File mappingFile = new File(workspaceRoot, "validation-mapping.json");
        if (!mappingFile.exists()) {
            log("WARN", "validation-mapping.json missing, recreating default workspace files.");
            initializeWorkspace();
        }

        try {
            String content = Files.readString(mappingFile.toPath());
            JSONObject json = new JSONObject(content);
            JSONArray arr = json.optJSONArray("mappings");
            if (arr != null) {
                Map<String, List<ValidationMapping>> grouped = new LinkedHashMap<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name = obj.optString("name", "");
                    String messagePath = obj.optString("messagePath", "");
                    String schemaPath = obj.optString("schemaPath", "");
                    String type = obj.optString("type", "XML + XSD");

                    if (name.isEmpty()) {
                        name = new File(messagePath).getName();
                    }

                    ValidationMapping mapping = new ValidationMapping(name, messagePath, schemaPath, type);
                    mappingsList.add(mapping);
                    grouped.computeIfAbsent(type, k -> new ArrayList<>()).add(mapping);
                }

                TreeItem<String> rootItem = new TreeItem<>("Root");
                for (Map.Entry<String, List<ValidationMapping>> entry : grouped.entrySet()) {
                    TreeItem<String> categoryItem = new TreeItem<>(entry.getKey());
                    categoryItem.setExpanded(true);
                    categoryItem.setGraphic(new FontIcon("fas-folder-open"));
                    for (ValidationMapping mapping : entry.getValue()) {
                        TreeItem<String> item = new TreeItem<>(mapping.name);
                        item.setGraphic(new FontIcon("fas-link"));
                        categoryItem.getChildren().add(item);
                        treeItemMappingMap.put(item, mapping);
                    }
                    rootItem.getChildren().add(categoryItem);
                }
                treeView.setRoot(rootItem);
            }
        } catch (Exception e) {
            log("ERROR", "Failed to reload Tree Scenarios: " + e.getMessage());
        }
    }

    private void loadValidationMapping(ValidationMapping mapping) {
        this.activeMapping = mapping;
        cmbValidatorType.setValue(mapping.type);

        File messageFile = new File(workspaceRoot, mapping.messagePath);
        File schemaFile = mapping.schemaPath.isEmpty() ? null : new File(workspaceRoot, mapping.schemaPath);

        lblSourceTitle.setText("Source Message (Data) - " + messageFile.getName());
        lblSchemaTitle.setText("Schema / Rules / Context - " + (schemaFile == null ? "None" : schemaFile.getName()));

        String msgContent = "";
        String schemaContent = "";

        try {
            if (messageFile.exists()) {
                msgContent = Files.readString(messageFile.toPath());
            } else {
                msgContent = "<!-- ERROR: Message file " + messageFile.getName() + " does not exist -->";
            }
        } catch (Exception ex) {
            msgContent = "<!-- Error reading message file: " + ex.getMessage() + " -->";
        }

        try {
            if (schemaFile != null && schemaFile.exists()) {
                schemaContent = Files.readString(schemaFile.toPath());
            } else {
                schemaContent = schemaFile == null ? "" : "<!-- ERROR: Schema file " + schemaFile.getName() + " does not exist -->";
            }
        } catch (Exception ex) {
            schemaContent = "<!-- Error reading schema file: " + ex.getMessage() + " -->";
        }

        setEditorText(engineSource, msgContent);
        setEditorText(engineSchema, schemaContent);

        String welcomeMarkdown = "# Ready to Validate\n" +
            "**Scenario:** " + mapping.name + "\n" +
            "**Type:** " + mapping.type + "\n\n" +
            "Click **Validate** in the toolbar to check document constraints.";
        setEditorText(engineResult, welcomeMarkdown);

        updateEditorLanguages(mapping.type);
        log("INFO", "Loaded scenario '" + mapping.name + "' into workspace editors.");
    }

    private void saveCurrentContent() {
        if (activeMapping == null) {
            showError("No Scenario Active", "Select a scenario from the explorer before saving.");
            return;
        }

        File messageFile = new File(workspaceRoot, activeMapping.messagePath);
        File schemaFile = activeMapping.schemaPath.isEmpty() ? null : new File(workspaceRoot, activeMapping.schemaPath);

        String msgText = getEditorText(engineSource);
        String schemaText = getEditorText(engineSchema);

        try {
            if (messageFile != null) {
                Files.writeString(messageFile.toPath(), msgText);
            }
            if (schemaFile != null) {
                Files.writeString(schemaFile.toPath(), schemaText);
            }
            log("INFO", "Saved edits for scenario '" + activeMapping.name + "' successfully.");
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Scenario files updated successfully!", ButtonType.OK);
            alert.setTitle("Saved Successfully");
            alert.setHeaderText(null);
            alert.showAndWait();
        } catch (Exception ex) {
            log("ERROR", "Save failed: " + ex.getMessage());
            showError("Save Error", ex.getMessage());
        }
    }

    private void runValidation() {
        String type = cmbValidatorType.getValue();
        if (type == null) {
            showError("Select Validator Type", "No validation format has been chosen.");
            return;
        }

        String sourceContent = getEditorText(engineSource);
        String schemaContent = getEditorText(engineSchema);

        if (sourceContent.trim().isEmpty()) {
            setEditorText(engineResult, "# ❌ Validation Error\nSource content cannot be empty.");
            return;
        }

        log("INFO", "Executing validation suite: " + type + "...");
        long start = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();

        try {
            switch (type) {
                case "XML + XSD":
                    if (schemaContent.trim().isEmpty()) {
                        errors.add("XSD schema data is empty.");
                    } else {
                        validateXmlAndXsd(sourceContent, schemaContent, errors);
                    }
                    break;
                case "JSON + Schema":
                    if (schemaContent.trim().isEmpty()) {
                        errors.add("JSON Schema definition is empty.");
                    } else {
                        validateJsonWithSchema(sourceContent, schemaContent, errors);
                    }
                    break;
                case "YAML + Schema":
                    if (schemaContent.trim().isEmpty()) {
                        errors.add("JSON Schema definition is empty.");
                    } else {
                        validateYamlWithSchema(sourceContent, schemaContent, errors);
                    }
                    break;
                case "SWIFT MT Message":
                    validateSwiftMtWithRules(sourceContent, schemaContent, errors);
                    break;
                case "ISO 20022 MX":
                    if (schemaContent.trim().isEmpty()) {
                        errors.add("ISO 20022 Schema XSD definition is empty.");
                    } else {
                        validateIso20022Mx(sourceContent, schemaContent, errors);
                    }
                    break;
                case "CSV + CSVW":
                    if (schemaContent.trim().isEmpty()) {
                        errors.add("CSVW JSON metadata layout is empty.");
                    } else {
                        validateCsvW(sourceContent, schemaContent, errors);
                    }
                    break;
                case "Flat File":
                    if (schemaContent.trim().isEmpty()) {
                        errors.add("Flat file JSON constraints definition is empty.");
                    } else {
                        validateFlatFile(sourceContent, schemaContent, errors);
                    }
                    break;
                default:
                    errors.add("Unsupported validation format selected: " + type);
                    break;
            }
        } catch (Exception ex) {
            errors.add("Validator Engine Critical Crash: " + ex.getMessage());
        }

        long duration = System.currentTimeMillis() - start;
        StringBuilder mdReport = new StringBuilder();

        if (errors.isEmpty()) {
            mdReport.append("# ✅ Validation Successful!\n\n");
            mdReport.append("- **Format Schema Type:** ").append(type).append("\n");
            mdReport.append("- **Time Spent:** ").append(duration).append(" ms\n\n");
            mdReport.append("---\n\n");
            mdReport.append("🎉 The document has been checked and matches all structurally specified rules. No discrepancies found.");
            log("INFO", "Validation completed successfully in " + duration + " ms.");
        } else {
            mdReport.append("# ❌ Validation Failed\n\n");
            mdReport.append("- **Format Schema Type:** ").append(type).append("\n");
            mdReport.append("- **Time Spent:** ").append(duration).append(" ms\n");
            mdReport.append("- **Errors Encountered:** ").append(errors.size()).append(" anomalies\n\n");
            mdReport.append("---\n\n");
            mdReport.append("### Diagnostic Errors Listing:\n\n");
            for (String err : errors) {
                mdReport.append("- ❌ ").append(err).append("\n");
            }
            log("WARN", "Validation completed with " + errors.size() + " anomalies.");
        }

        setEditorText(engineResult, mdReport.toString());
    }

    private void setupTreeContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem mnuAdd = new MenuItem("Add Validation Pair...", new FontIcon("fas-plus-circle"));
        mnuAdd.setOnAction(e -> openAddPairDialog());

        MenuItem mnuUpdate = new MenuItem("Update Validation Pair...", new FontIcon("fas-edit"));
        mnuUpdate.setOnAction(e -> {
            TreeItem<String> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || !treeItemMappingMap.containsKey(selected)) {
                showError("Selection Error", "Please select a mapping item from the tree list.");
                return;
            }
            ValidationMapping mapping = treeItemMappingMap.get(selected);
            openUpdatePairDialog(mapping);
        });

        MenuItem mnuRemove = new MenuItem("Remove Validation Pair", new FontIcon("fas-minus-circle"));
        mnuRemove.setOnAction(e -> {
            TreeItem<String> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || !treeItemMappingMap.containsKey(selected)) {
                showError("Selection Error", "Please select a mapping item from the tree list.");
                return;
            }
            ValidationMapping mapping = treeItemMappingMap.get(selected);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete mapping: '" + mapping.name + "'?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    removeMapping(mapping);
                }
            });
        });

        contextMenu.getItems().addAll(mnuAdd, mnuUpdate, mnuRemove);

        treeView.setCellFactory(tv -> {
            TreeCell<String> cell = new TreeCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        TreeItem<String> treeItem = getTreeItem();
                        if (treeItem != null) {
                            setGraphic(treeItem.getGraphic());
                        }
                    }
                }
            };
            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell;
        });
    }

    private void updateMappingInJson(ValidationMapping mapping) {
        File mappingFile = new File(workspaceRoot, "validation-mapping.json");
        try {
            if (!mappingFile.exists()) return;
            String content = Files.readString(mappingFile.toPath());
            JSONObject json = new JSONObject(content);
            JSONArray arr = json.optJSONArray("mappings");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String mPath = obj.optString("messagePath");
                    String type = obj.optString("type");
                    if (mapping.messagePath.equals(mPath) && mapping.type.equals(type)) {
                        obj.put("schemaPath", mapping.schemaPath);
                        obj.put("name", mapping.name);
                        break;
                    }
                }
                Files.writeString(mappingFile.toPath(), json.toString(2));
            }
        } catch (Exception ex) {
            log("ERROR", "Failed to update mapping in JSON: " + ex.getMessage());
        }
    }

    private void openUpdatePairDialog(ValidationMapping mapping) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Validation Pair Scenario");
        dialog.setHeaderText("Update name, type, or files for '" + mapping.name + "'");

        ButtonType btnTypeSave = new ButtonType("Update Pair", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTypeSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        TextField txtName = new TextField(mapping.name);

        ComboBox<String> cmbTypes = new ComboBox<>();
        cmbTypes.getItems().addAll("XML + XSD", "JSON + Schema", "YAML + Schema", "SWIFT MT Message", "ISO 20022 MX", "CSV + CSVW", "Flat File");
        cmbTypes.setValue(mapping.type);

        File oldMsgFile = new File(workspaceRoot, mapping.messagePath);
        TextField txtMsgPath = new TextField(oldMsgFile.getAbsolutePath());
        Button btnBrowseMsg = new Button("Browse...");

        File oldSchemaFile = mapping.schemaPath.isEmpty() ? null : new File(workspaceRoot, mapping.schemaPath);
        TextField txtSchemaPath = new TextField(oldSchemaFile == null ? "" : oldSchemaFile.getAbsolutePath());
        Button btnBrowseSchema = new Button("Browse...");

        btnBrowseMsg.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Message Payload");
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                txtMsgPath.setText(file.getAbsolutePath());
            }
        });

        btnBrowseSchema.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Schema definition");
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                txtSchemaPath.setText(file.getAbsolutePath());
            }
        });

        grid.add(new Label("Name:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Format Type:"), 0, 1);
        grid.add(cmbTypes, 1, 1);
        grid.add(new Label("Message File:"), 0, 2);
        grid.add(txtMsgPath, 1, 2);
        grid.add(btnBrowseMsg, 2, 2);
        grid.add(new Label("Schema/Rules File:"), 0, 3);
        grid.add(txtSchemaPath, 1, 3);
        grid.add(btnBrowseSchema, 2, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == btnTypeSave) {
                String newName = txtName.getText().trim();
                String newType = cmbTypes.getValue();
                String msgPathStr = txtMsgPath.getText().trim();
                String schemaPathStr = txtSchemaPath.getText().trim();

                if (newName.isEmpty() || msgPathStr.isEmpty()) {
                    showError("Missing Parameters", "Name and message files must be provided.");
                    return;
                }

                try {
                    File msgSrc = new File(msgPathStr);
                    File schemaSrc = schemaPathStr.isEmpty() ? null : new File(schemaPathStr);

                    String sub = newType.toLowerCase().replace(" + ", "_").replace(" ", "_");
                    File msgDest = copyFileToWorkspace(msgSrc, "messages/" + sub);
                    File schemaDest = copyFileToWorkspace(schemaSrc, "schemas/" + sub);

                    String msgRel = workspaceRoot.toURI().relativize(msgDest.toURI()).getPath();
                    String schemaRel = schemaDest == null ? "" : workspaceRoot.toURI().relativize(schemaDest.toURI()).getPath();

                    updateMappingDetails(mapping, newName, msgRel, schemaRel, newType);
                } catch (Exception ex) {
                    log("ERROR", "Failed to update mapping: " + ex.getMessage());
                    showError("Update Scenario Failed", ex.getMessage());
                }
            }
        });
    }

    private void updateMappingDetails(ValidationMapping oldMapping, String newName, String newMsgRel, String newSchemaRel, String newType) {
        File mappingFile = new File(workspaceRoot, "validation-mapping.json");
        try {
            if (!mappingFile.exists()) return;
            String content = Files.readString(mappingFile.toPath());
            JSONObject json = new JSONObject(content);
            JSONArray arr = json.optJSONArray("mappings");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String mPath = obj.optString("messagePath");
                    String type = obj.optString("type");
                    if (oldMapping.messagePath.equals(mPath) && oldMapping.type.equals(type)) {
                        obj.put("name", newName);
                        obj.put("messagePath", newMsgRel);
                        obj.put("schemaPath", newSchemaRel);
                        obj.put("type", newType);
                        break;
                    }
                }
                Files.writeString(mappingFile.toPath(), json.toString(2));
                log("INFO", "Updated scenario '" + oldMapping.name + "' to '" + newName + "'.");
                refreshTree();
                
                if (activeMapping != null && activeMapping.messagePath.equals(oldMapping.messagePath) && activeMapping.type.equals(oldMapping.type)) {
                    ValidationMapping updatedMapping = new ValidationMapping(newName, newMsgRel, newSchemaRel, newType);
                    loadValidationMapping(updatedMapping);
                }
            }
        } catch (Exception ex) {
            log("ERROR", "Failed to update mapping details in JSON: " + ex.getMessage());
        }
    }

    private void openAddPairDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Validation Pair Scenario");
        dialog.setHeaderText("Create and link a message file with a schema format");

        ButtonType btnTypeSave = new ButtonType("Add Pair", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTypeSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        TextField txtName = new TextField();
        txtName.setPromptText("e.g. pacs008 customer query");

        ComboBox<String> cmbTypes = new ComboBox<>();
        cmbTypes.getItems().addAll("XML + XSD", "JSON + Schema", "YAML + Schema", "SWIFT MT Message", "ISO 20022 MX", "CSV + CSVW", "Flat File");
        cmbTypes.setValue("XML + XSD");

        TextField txtMsgPath = new TextField();
        txtMsgPath.setPromptText("Click Browse to choose message payload...");
        Button btnBrowseMsg = new Button("Browse...");

        TextField txtSchemaPath = new TextField();
        txtSchemaPath.setPromptText("Click Browse to choose schema file (optional for SWIFT)...");
        Button btnBrowseSchema = new Button("Browse...");

        btnBrowseMsg.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Message Payload");
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                txtMsgPath.setText(file.getAbsolutePath());
                if (txtName.getText().trim().isEmpty()) {
                    String baseName = file.getName();
                    int dot = baseName.lastIndexOf('.');
                    txtName.setText((dot == -1 ? baseName : baseName.substring(0, dot)) + " Scenario");
                }
            }
        });

        btnBrowseSchema.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Schema definition");
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                txtSchemaPath.setText(file.getAbsolutePath());
            }
        });

        grid.add(new Label("Name:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Format Type:"), 0, 1);
        grid.add(cmbTypes, 1, 1);
        grid.add(new Label("Message File:"), 0, 2);
        grid.add(txtMsgPath, 1, 2);
        grid.add(btnBrowseMsg, 2, 2);
        grid.add(new Label("Schema/Rules File:"), 0, 3);
        grid.add(txtSchemaPath, 1, 3);
        grid.add(btnBrowseSchema, 2, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == btnTypeSave) {
                String name = txtName.getText().trim();
                String type = cmbTypes.getValue();
                String msgPathStr = txtMsgPath.getText().trim();
                String schemaPathStr = txtSchemaPath.getText().trim();

                if (name.isEmpty() || msgPathStr.isEmpty()) {
                    showError("Missing Parameters", "Name and message files must be provided.");
                    return;
                }

                try {
                    File msgSrc = new File(msgPathStr);
                    File schemaSrc = schemaPathStr.isEmpty() ? null : new File(schemaPathStr);

                    String sub = type.toLowerCase().replace(" + ", "_").replace(" ", "_");
                    File msgDest = copyFileToWorkspace(msgSrc, "messages/" + sub);
                    File schemaDest = copyFileToWorkspace(schemaSrc, "schemas/" + sub);

                    String msgRel = workspaceRoot.toURI().relativize(msgDest.toURI()).getPath();
                    String schemaRel = schemaDest == null ? "" : workspaceRoot.toURI().relativize(schemaDest.toURI()).getPath();

                    addMapping(name, msgRel, schemaRel, type);
                } catch (Exception ex) {
                    log("ERROR", "Failed to add mapping: " + ex.getMessage());
                    showError("Add Scenario Failed", ex.getMessage());
                }
            }
        });
    }

    private File copyFileToWorkspace(File sourceFile, String targetSubdir) throws IOException {
        if (sourceFile == null) return null;
        if (sourceFile.getAbsolutePath().startsWith(workspaceRoot.getAbsolutePath())) {
            return sourceFile;
        }
        File targetDir = new File(workspaceRoot, targetSubdir);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        File targetFile = new File(targetDir, sourceFile.getName());
        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return targetFile;
    }

    private void addMapping(String name, String msgRel, String schemaRel, String type) {
        File mappingFile = new File(workspaceRoot, "validation-mapping.json");
        try {
            JSONObject json = new JSONObject();
            JSONArray arr = new JSONArray();

            if (mappingFile.exists()) {
                String content = Files.readString(mappingFile.toPath());
                json = new JSONObject(content);
                arr = json.optJSONArray("mappings");
                if (arr == null) {
                    arr = new JSONArray();
                    json.put("mappings", arr);
                }
            } else {
                json.put("mappings", arr);
            }

            JSONObject newMapping = new JSONObject();
            newMapping.put("name", name);
            newMapping.put("messagePath", msgRel);
            newMapping.put("schemaPath", schemaRel);
            newMapping.put("type", type);
            arr.put(newMapping);

            Files.writeString(mappingFile.toPath(), json.toString(2));
            log("INFO", "Added validation scenario '" + name + "'.");
            refreshTree();
        } catch (Exception ex) {
            log("ERROR", "Failed to write validation-mapping.json: " + ex.getMessage());
        }
    }

    private void removeMapping(ValidationMapping mapping) {
        File mappingFile = new File(workspaceRoot, "validation-mapping.json");
        try {
            if (!mappingFile.exists()) return;

            String content = Files.readString(mappingFile.toPath());
            JSONObject json = new JSONObject(content);
            JSONArray arr = json.optJSONArray("mappings");
            if (arr != null) {
                JSONArray newArr = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String mPath = obj.optString("messagePath");
                    String sPath = obj.optString("schemaPath");
                    String type = obj.optString("type");

                    if (mapping.messagePath.equals(mPath) && mapping.schemaPath.equals(sPath) && mapping.type.equals(type)) {
                        continue;
                    }
                    newArr.put(obj);
                }
                json.put("mappings", newArr);
                Files.writeString(mappingFile.toPath(), json.toString(2));
                log("INFO", "Removed validation scenario '" + mapping.name + "'.");
                refreshTree();

                setEditorText(engineSource, "");
                setEditorText(engineSchema, "");
                setEditorText(engineResult, "# Scenario Deleted");
                lblSourceTitle.setText("Source Message (Data) - None");
                lblSchemaTitle.setText("Schema / Rules / Context - None");
                activeMapping = null;
            }
        } catch (Exception ex) {
            log("ERROR", "Failed to remove mapping: " + ex.getMessage());
        }
    }

    // --- Validation Engines In-Memory String Implementation ---

    public static void validateXmlAndXsd(String xmlContent, String xsdContent, List<String> errors) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(new StringReader(xsdContent)));
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException ex) {
                    errors.add("Line " + ex.getLineNumber() + ":" + ex.getColumnNumber() + " - Warning: " + ex.getMessage());
                }
                @Override
                public void error(org.xml.sax.SAXParseException ex) {
                    errors.add("Line " + ex.getLineNumber() + ":" + ex.getColumnNumber() + " - Error: " + ex.getMessage());
                }
                @Override
                public void fatalError(org.xml.sax.SAXParseException ex) {
                    errors.add("Line " + ex.getLineNumber() + ":" + ex.getColumnNumber() + " - Fatal Error: " + ex.getMessage());
                }
            });
            validator.validate(new StreamSource(new StringReader(xmlContent)));
        } catch (Exception ex) {
            errors.add("XML Validation failed: " + ex.getMessage());
        }
    }

    public static void validateJsonWithSchema(String jsonContent, String schemaContent, List<String> errors) {
        try {
            JSONObject schemaJson = new JSONObject(schemaContent);
            JSONObject dataJson = new JSONObject(jsonContent);
            validateJsonNode(dataJson, schemaJson, "", errors);
        } catch (Exception ex) {
            errors.add("JSON Syntax/Parsing Error: " + ex.getMessage());
        }
    }

    public static void validateYamlWithSchema(String yamlContent, String schemaContent, List<String> errors) {
        try {
            YAMLFactory yamlFactory = new YAMLFactory();
            ObjectMapper mapper = new ObjectMapper(yamlFactory);
            Object obj = mapper.readValue(yamlContent, Object.class);

            ObjectMapper jsonMapper = new ObjectMapper();
            String jsonString = jsonMapper.writeValueAsString(obj);

            JSONObject schemaJson = new JSONObject(schemaContent);
            JSONObject dataJson = new JSONObject(jsonString);

            validateJsonNode(dataJson, schemaJson, "", errors);
        } catch (Exception ex) {
            errors.add("YAML / JSON Schema validation error: " + ex.getMessage());
        }
    }

    public static void validateJsonNode(Object node, Object schema, String path, List<String> errors) {
        if (schema instanceof JSONObject) {
            JSONObject sObj = (JSONObject) schema;
            String type = sObj.optString("type", "");

            if ("object".equals(type) && !(node instanceof JSONObject)) {
                errors.add(path + ": Must be an object");
                return;
            }
            if ("array".equals(type) && !(node instanceof JSONArray)) {
                errors.add(path + ": Must be an array");
                return;
            }
            if ("integer".equals(type) && !(node instanceof Integer || node instanceof Long)) {
                errors.add(path + ": Must be an integer");
                return;
            }
            if ("number".equals(type) && !(node instanceof Number)) {
                errors.add(path + ": Must be a number");
                return;
            }
            if ("string".equals(type) && !(node instanceof String)) {
                errors.add(path + ": Must be a string");
                return;
            }
            if ("boolean".equals(type) && !(node instanceof Boolean)) {
                errors.add(path + ": Must be a boolean");
                return;
            }

            if (node instanceof String) {
                String val = (String) node;
                if (sObj.has("minLength") && val.length() < sObj.getInt("minLength")) {
                    errors.add(path + ": String length (" + val.length() + ") is less than minLength (" + sObj.getInt("minLength") + ")");
                }
                if (sObj.has("maxLength") && val.length() > sObj.getInt("maxLength")) {
                    errors.add(path + ": String length (" + val.length() + ") is greater than maxLength (" + sObj.getInt("maxLength") + ")");
                }
                if (sObj.has("pattern")) {
                    String pat = sObj.getString("pattern");
                    if (!val.matches(pat)) {
                        errors.add(path + ": Value '" + val + "' does not match pattern '" + pat + "'");
                    }
                }
                if (sObj.has("format")) {
                    String fmt = sObj.getString("format");
                    if ("email".equals(fmt) && (!val.contains("@") || !val.contains("."))) {
                        errors.add(path + ": Invalid email format '" + val + "'");
                    }
                }
            }

            if (node instanceof Number) {
                double val = ((Number) node).doubleValue();
                if (sObj.has("minimum") && val < sObj.getDouble("minimum")) {
                    errors.add(path + ": Value " + val + " is less than minimum (" + sObj.getDouble("minimum") + ")");
                }
                if (sObj.has("maximum") && val > sObj.getDouble("maximum")) {
                    errors.add(path + ": Value " + val + " is greater than maximum (" + sObj.getDouble("maximum") + ")");
                }
            }

            if (node instanceof JSONObject) {
                JSONObject nObj = (JSONObject) node;
                if (sObj.has("required")) {
                    JSONArray req = sObj.getJSONArray("required");
                    for (int i = 0; i < req.length(); i++) {
                        String prop = req.getString(i);
                        if (!nObj.has(prop)) {
                            errors.add(path.isEmpty() ? "Missing required property '" + prop + "'" : path + ": Missing required property '" + prop + "'");
                        }
                    }
                }
                if (sObj.has("properties")) {
                    JSONObject props = sObj.getJSONObject("properties");
                    for (String key : props.keySet()) {
                        if (nObj.has(key)) {
                            validateJsonNode(nObj.get(key), props.get(key), path.isEmpty() ? key : path + "." + key, errors);
                        }
                    }
                }
            }

            if (node instanceof JSONArray) {
                JSONArray nArr = (JSONArray) node;
                if (sObj.has("items")) {
                    Object itemsSchema = sObj.get("items");
                    for (int i = 0; i < nArr.length(); i++) {
                        validateJsonNode(nArr.get(i), itemsSchema, path + "[" + i + "]", errors);
                    }
                }
            }

            if (sObj.has("enum")) {
                JSONArray enums = sObj.getJSONArray("enum");
                boolean found = false;
                for (int i = 0; i < enums.length(); i++) {
                    if (enums.get(i).toString().equals(node.toString())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    errors.add(path + ": Value '" + node + "' is not in allowed enum list");
                }
            }
        }
    }

    public void validateSwiftMtWithRules(String content, String rulesJson, List<String> errors) {
        validateSwiftMt(content, radEnhanced.isSelected(), rulesJson, errors);
    }

    public static void validateSwiftMt(String content, boolean enhanced, String rulesJson, List<String> errors) {
        String refPrefix = "EXB-";
        double amountLimit = 10000000.0;
        List<String> restrictedCurrencies = new ArrayList<>(Arrays.asList("RUB"));
        List<String> highRiskJurisdictions = new ArrayList<>(Arrays.asList("Iran", "Tehran"));

        if (enhanced && rulesJson != null && !rulesJson.trim().isEmpty()) {
            try {
                JSONObject rulesObj = new JSONObject(rulesJson);
                JSONObject custom = rulesObj.optJSONObject("custom_rules");
                if (custom == null && rulesObj.has("custom_rules")) {
                    // Try to see if it's an array or object
                    JSONArray customArr = rulesObj.optJSONArray("custom_rules");
                    if (customArr != null) {
                        // Old rules format, convert array of id/description
                    }
                }

                if (rulesObj.has("refPrefix")) refPrefix = rulesObj.getString("refPrefix");
                if (rulesObj.has("amountLimit")) amountLimit = rulesObj.getDouble("amountLimit");

                JSONArray ccyArr = rulesObj.optJSONArray("restrictedCurrencies");
                if (ccyArr != null) {
                    restrictedCurrencies.clear();
                    for (int i = 0; i < ccyArr.length(); i++) {
                        restrictedCurrencies.add(ccyArr.getString(i));
                    }
                }

                JSONArray jurArr = rulesObj.optJSONArray("highRiskJurisdictions");
                if (jurArr != null) {
                    highRiskJurisdictions.clear();
                    for (int i = 0; i < jurArr.length(); i++) {
                        highRiskJurisdictions.add(jurArr.getString(i));
                    }
                }
            } catch (Exception ex) {
                errors.add("Warning: Failed to parse rules JSON, using default rules. Details: " + ex.getMessage());
            }
        }

        String[] lines = content.split("\\r?\\n");
        boolean hasBlock1 = false;
        boolean hasBlock2 = false;
        boolean hasBlock4 = false;
        boolean endsBlock4 = false;

        String bic = null;
        String opCode = null;
        String valDateStr = null;
        String valCcy = null;
        double valAmount = -1.0;
        int field20Line = -1;
        int field23bLine = -1;
        int field32aLine = -1;
        int field50kLine = -1;
        int field59Line = -1;
        int field71aLine = -1;

        int field50kLineCount = 0;
        int field59LineCount = 0;

        String activeField = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int humanLine = i + 1;

            if (line.startsWith("{1:")) {
                hasBlock1 = true;
                int idx = line.indexOf("}");
                if (idx != -1) {
                    String blockVal = line.substring(3, idx);
                    if (blockVal.length() < 12) {
                        errors.add("Line " + humanLine + " - Block 1 must contain a valid BIC (min 12 chars: Application/BIC/Session)");
                    } else {
                        bic = blockVal.substring(3, 11);
                    }
                }
            } else if (line.startsWith("{2:")) {
                hasBlock2 = true;
            } else if (line.startsWith("{4:")) {
                hasBlock4 = true;
            } else if (line.startsWith("-}")) {
                endsBlock4 = true;
            } else if (line.startsWith(":")) {
                activeField = null;
                if (line.startsWith(":20:")) {
                    field20Line = humanLine;
                    String ref = line.substring(4);
                    if (ref.isEmpty()) {
                        errors.add("Line " + humanLine + " - Field 20: Transaction Reference Number is empty.");
                    } else if (ref.length() > 16) {
                        errors.add("Line " + humanLine + " - Field 20: Reference exceeds 16 chars limit.");
                    }
                    if (enhanced && !ref.startsWith(refPrefix)) {
                        errors.add("Line " + humanLine + " - Field 20: Reference format must start with " + refPrefix);
                    }
                } else if (line.startsWith(":23B:")) {
                    field23bLine = humanLine;
                    opCode = line.substring(5);
                    List<String> validOps = Arrays.asList("CRED", "SPAY", "SSTD", "SPRI");
                    if (!validOps.contains(opCode)) {
                        errors.add("Line " + humanLine + " - Field 23B: Bank Operation Code must be CRED, SPAY, SSTD, or SPRI.");
                    }
                } else if (line.startsWith(":32A:")) {
                    field32aLine = humanLine;
                    String val = line.substring(5);
                    Pattern p = Pattern.compile("^(\\d{6})([A-Z]{3})([0-9,.]+)$");
                    Matcher m = p.matcher(val);
                    if (m.matches()) {
                        valDateStr = m.group(1);
                        valCcy = m.group(2);
                        String amtStr = m.group(3).replace(",", ".");
                        try {
                            valAmount = Double.parseDouble(amtStr);
                            if (valAmount < 0) {
                                errors.add("Line " + humanLine + " - Field 32A: Amount cannot be negative.");
                            }
                            if (enhanced && valAmount > amountLimit) {
                                errors.add("Line " + humanLine + " - Field 32A: Amount exceeds limit " + amountLimit);
                            }
                            if (enhanced && restrictedCurrencies.contains(valCcy)) {
                                errors.add("Line " + humanLine + " - Field 32A: Restricted currency " + valCcy);
                            }
                        } catch (Exception ex) {
                            errors.add("Line " + humanLine + " - Field 32A: Invalid amount formatting.");
                        }
                    } else {
                        errors.add("Line " + humanLine + " - Field 32A: Must match format YYMMDDCCYAmount");
                    }
                } else if (line.startsWith(":50K:")) {
                    field50kLine = humanLine;
                    activeField = "50K";
                    field50kLineCount = 1;
                } else if (line.startsWith(":59:")) {
                    field59Line = humanLine;
                    activeField = "59";
                    field59LineCount = 1;
                    String rest = line.substring(4);
                    if (enhanced) {
                        for (String risk : highRiskJurisdictions) {
                            if (rest.contains(risk)) {
                                errors.add("Line " + humanLine + " - Field 59: High-risk jurisdiction " + risk + " detected");
                            }
                        }
                    }
                } else if (line.startsWith(":71A:")) {
                    field71aLine = humanLine;
                    String fee = line.substring(5);
                    List<String> validFees = Arrays.asList("BEN", "SHA", "OUR");
                    if (!validFees.contains(fee)) {
                        errors.add("Line " + humanLine + " - Field 71A: Details of Charges must be BEN, SHA, or OUR.");
                    }
                    if ("CRED".equals(opCode) && !"SHA".equals(fee)) {
                        errors.add("Line " + humanLine + " - CFR001 Cross-field rule: If 23B is CRED, 71A must be SHA (was: " + fee + ")");
                    }
                }
            } else if (!line.isEmpty() && activeField != null) {
                if ("50K".equals(activeField)) {
                    field50kLineCount++;
                } else if ("59".equals(activeField)) {
                    field59LineCount++;
                    if (enhanced) {
                        for (String risk : highRiskJurisdictions) {
                            if (line.contains(risk)) {
                                errors.add("Line " + humanLine + " - Field 59: High-risk jurisdiction " + risk + " detected");
                            }
                        }
                    }
                }
            }
        }

        if (!hasBlock1) errors.add("Missing Block 1 starting with {1:");
        if (!hasBlock2) errors.add("Missing Block 2 starting with {2:");
        if (!hasBlock4) errors.add("Missing Block 4 starting with {4:");
        if (!endsBlock4) errors.add("Block 4 is missing the terminator -}");

        if (field50kLine != -1 && field50kLineCount < 2) {
            errors.add("Line " + field50kLine + " - Field 50K: Ordering Customer requires minimum 2 lines.");
        }
        if (field59Line != -1 && field59LineCount < 2) {
            errors.add("Line " + field59Line + " - Field 59: Beneficiary Customer requires minimum 2 lines.");
        }
    }

    public static void validateIso20022Mx(String content, String xsdContent, List<String> errors) {
        validateXmlAndXsd(content, xsdContent, errors);
    }

    public static void validateCsvW(String content, String schemaContent, List<String> errors) {
        try {
            JSONObject metadata = new JSONObject(schemaContent);
            JSONObject tableSchema = metadata.getJSONObject("tableSchema");
            JSONArray columns = tableSchema.getJSONArray("columns");

            String[] rows = content.split("\\r?\\n");
            if (rows.length == 0) {
                errors.add("CSV file is empty.");
                return;
            }

            String[] headers = parseCsvRow(rows[0]);
            Set<String> keys = new HashSet<>();

            for (int i = 1; i < rows.length; i++) {
                if (rows[i].trim().isEmpty()) continue;
                String[] cells = parseCsvRow(rows[i]);
                int humanLine = i + 1;

                if (cells.length < columns.length()) {
                    errors.add("Line " + humanLine + " - Insufficient columns. Expected " + columns.length() + ", got " + cells.length);
                    continue;
                }

                for (int c = 0; c < columns.length(); c++) {
                    JSONObject col = columns.getJSONObject(c);
                    String val = cells[c].trim();
                    String colName = col.getString("name");

                    if (col.optBoolean("required", false) && val.isEmpty()) {
                        errors.add("Line " + humanLine + " - Column '" + colName + "' is required but empty.");
                        continue;
                    }

                    if (val.isEmpty()) continue;

                    String type = col.optString("datatype", "string");
                    if ("number".equals(type)) {
                        try {
                            double num = Double.parseDouble(val);
                            if (col.has("minimum") && num < col.getDouble("minimum")) {
                                errors.add("Line " + humanLine + " - Column '" + colName + "' value " + num + " is less than minimum (" + col.getDouble("minimum") + ")");
                            }
                        } catch (Exception ex) {
                            errors.add("Line " + humanLine + " - Column '" + colName + "' has invalid numeric value '" + val + "'");
                        }
                    } else if ("date".equals(type)) {
                        if (!val.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                            errors.add("Line " + humanLine + " - Column '" + colName + "' has invalid date format '" + val + "', expected yyyy-MM-dd");
                        }
                    }

                    if (col.has("minLength") && val.length() < col.getInt("minLength")) {
                        errors.add("Line " + humanLine + " - Column '" + colName + "' length " + val.length() + " is shorter than minLength (" + col.getInt("minLength") + ")");
                    }
                    if (col.has("maxLength") && val.length() > col.getInt("maxLength")) {
                        errors.add("Line " + humanLine + " - Column '" + colName + "' length " + val.length() + " is longer than maxLength (" + col.getInt("maxLength") + ")");
                    }
                    if (col.has("format")) {
                        String format = col.getString("format");
                        if (!val.matches(format) && !"date".equals(type)) {
                            errors.add("Line " + humanLine + " - Column '" + colName + "' value '" + val + "' does not match format regex '" + format + "'");
                        }
                    }

                    if (col.has("constraints")) {
                        JSONObject constraints = col.getJSONObject("constraints");
                        if (constraints.has("enum")) {
                            JSONArray allowed = constraints.getJSONArray("enum");
                            boolean found = false;
                            for (int a = 0; a < allowed.length(); a++) {
                                if (allowed.getString(a).equals(val)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                errors.add("Line " + humanLine + " - Column '" + colName + "' value '" + val + "' is not in allowed enum list.");
                            }
                        }
                    }

                    if (colName.equals(tableSchema.optString("primaryKey", ""))) {
                        if (!keys.add(val)) {
                            errors.add("Line " + humanLine + " - Primary key constraint violated: Duplicate key '" + val + "' found.");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            errors.add("CSV Validation error: " + ex.getMessage());
        }
    }

    public static String[] parseCsvRow(String row) {
        List<String> cells = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                cells.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        cells.add(sb.toString());
        return cells.toArray(new String[0]);
    }

    public static void validateFlatFile(String content, String schemaContent, List<String> errors) {
        try {
            JSONObject schema = new JSONObject(schemaContent);
            int expectedLength = schema.optInt("recordLength", 80);
            JSONArray fields = schema.getJSONArray("fields");

            String[] lines = content.split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int humanLine = i + 1;

                if (line.isEmpty()) continue;

                if (line.length() < expectedLength) {
                    errors.add("Line " + humanLine + " - Record length is too short (" + line.length() + " chars), expected " + expectedLength);
                    continue;
                }

                for (int f = 0; f < fields.length(); f++) {
                    JSONObject field = fields.getJSONObject(f);
                    String fieldName = field.getString("name");
                    int start = field.getInt("start") - 1;
                    int length = field.getInt("length");

                    if (start + length > line.length()) {
                        errors.add("Line " + humanLine + " - Field '" + fieldName + "' bounds out of range.");
                        continue;
                    }

                    String val = line.substring(start, start + length);
                    if (field.optBoolean("required", false) && val.trim().isEmpty()) {
                        errors.add("Line " + humanLine + " - Required field '" + fieldName + "' is empty.");
                        continue;
                    }

                    String type = field.optString("type", "string");
                    if ("decimal".equals(type)) {
                        try {
                            Double.parseDouble(val.trim());
                        } catch (Exception ex) {
                            errors.add("Line " + humanLine + " - Field '" + fieldName + "' must be decimal, got '" + val + "'");
                        }
                    } else if ("date".equals(type)) {
                        if (!val.trim().matches("^\\d{8}$")) {
                            errors.add("Line " + humanLine + " - Field '" + fieldName + "' must be Date in format yyyyMMdd, got '" + val + "'");
                        }
                    }

                    if (field.has("pattern")) {
                        String pattern = field.getString("pattern");
                        if (!val.trim().matches(pattern)) {
                            errors.add("Line " + humanLine + " - Field '" + fieldName + "' value '" + val + "' does not match pattern '" + pattern + "'");
                        }
                    }

                    if (field.has("enum")) {
                        JSONArray allowed = field.getJSONArray("enum");
                        boolean found = false;
                        for (int a = 0; a < allowed.length(); a++) {
                            if (allowed.getString(a).equals(val.trim())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            errors.add("Line " + humanLine + " - Field '" + fieldName + "' value '" + val.trim() + "' is not in allowed list.");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            errors.add("Flat file validation error: " + ex.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIdx = name.lastIndexOf('.');
        if (lastIdx == -1) return "";
        return name.substring(lastIdx + 1).toLowerCase();
    }

    // --- Preloaded Sample Content Providers ---

    private String getInvoiceXsd() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "           targetNamespace=\"http://example.com/invoice\"\n" +
            "           xmlns=\"http://example.com/invoice\"\n" +
            "           elementFormDefault=\"qualified\">\n" +
            "    <xs:element name=\"Invoice\">\n" +
            "        <xs:complexType>\n" +
            "            <xs:sequence>\n" +
            "                <xs:element name=\"InvoiceNumber\" type=\"xs:string\"/>\n" +
            "                <xs:element name=\"IssueDate\" type=\"xs:date\"/>\n" +
            "                <xs:element name=\"Customer\">\n" +
            "                    <xs:complexType>\n" +
            "                        <xs:sequence>\n" +
            "                            <xs:element name=\"Name\" type=\"xs:string\"/>\n" +
            "                            <xs:element name=\"Email\" type=\"xs:string\"/>\n" +
            "                        </xs:sequence>\n" +
            "                    </xs:complexType>\n" +
            "                </xs:element>\n" +
            "                <xs:element name=\"LineItems\" minOccurs=\"1\" maxOccurs=\"unbounded\">\n" +
            "                    <xs:complexType>\n" +
            "                        <xs:sequence>\n" +
            "                            <xs:element name=\"Item\">\n" +
            "                                <xs:complexType>\n" +
            "                                    <xs:sequence>\n" +
            "                                        <xs:element name=\"Description\" type=\"xs:string\"/>\n" +
            "                                        <xs:element name=\"Quantity\" type=\"xs:positiveInteger\"/>\n" +
            "                                        <xs:element name=\"UnitPrice\" type=\"xs:decimal\"/>\n" +
            "                                    </xs:sequence>\n" +
            "                                </xs:complexType>\n" +
            "                            </xs:element>\n" +
            "                        </xs:sequence>\n" +
            "                    </xs:complexType>\n" +
            "                </xs:element>\n" +
            "                <xs:element name=\"TotalAmount\" type=\"xs:decimal\"/>\n" +
            "            </xs:sequence>\n" +
            "        </xs:complexType>\n" +
            "    </xs:element>\n" +
            "</xs:schema>";
    }

    private String getInvoiceValidXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Invoice xmlns=\"http://example.com/invoice\">\n" +
            "    <InvoiceNumber>INV-2024-001</InvoiceNumber>\n" +
            "    <IssueDate>2024-01-15</IssueDate>\n" +
            "    <Customer>\n" +
            "        <Name>Acme Corporation</Name>\n" +
            "        <Email>billing@acme.com</Email>\n" +
            "    </Customer>\n" +
            "    <LineItems>\n" +
            "        <Item>\n" +
            "            <Description>Professional Services</Description>\n" +
            "            <Quantity>40</Quantity>\n" +
            "            <UnitPrice>150.00</UnitPrice>\n" +
            "        </Item>\n" +
            "    </LineItems>\n" +
            "    <TotalAmount>6000.00</TotalAmount>\n" +
            "</Invoice>";
    }

    private String getInvoiceInvalidXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Invoice xmlns=\"http://example.com/invoice\">\n" +
            "    <InvoiceNumber>INV-2024-002</InvoiceNumber>\n" +
            "    <IssueDate>not-a-date</IssueDate>\n" +
            "    <Customer>\n" +
            "        <Name>Global Tech Ltd</Name>\n" +
            "        <Email>invalid-email</Email>\n" +
            "    </Customer>\n" +
            "    <LineItems>\n" +
            "        <Item>\n" +
            "            <Description>Consulting</Description>\n" +
            "            <Quantity>-5</Quantity>\n" +
            "            <UnitPrice>200.00</UnitPrice>\n" +
            "        </Item>\n" +
            "    </LineItems>\n" +
            "</Invoice>";
    }

    private String getCustomerJsonSchema() {
        return "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
            "  \"title\": \"Customer\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"required\": [\"id\", \"name\", \"email\", \"status\"],\n" +
            "  \"properties\": {\n" +
            "    \"id\": {\n" +
            "      \"type\": \"integer\",\n" +
            "      \"minimum\": 1\n" +
            "    },\n" +
            "    \"name\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"minLength\": 2,\n" +
            "      \"maxLength\": 100\n" +
            "    },\n" +
            "    \"email\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"format\": \"email\"\n" +
            "    },\n" +
            "    \"phone\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"pattern\": \"^\\\\+?[1-9]\\\\d{1,14}$\"\n" +
            "    },\n" +
            "    \"status\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\"active\", \"inactive\", \"pending\"]\n" +
            "    }\n" +
            "  }\n" +
            "}";
    }

    private String getCustomerValidJson() {
        return "{\n" +
            "  \"id\": 1001,\n" +
            "  \"name\": \"John Smith\",\n" +
            "  \"email\": \"john.smith@example.com\",\n" +
            "  \"phone\": \"+14155552671\",\n" +
            "  \"status\": \"active\"\n" +
            "}";
    }

    private String getCustomerInvalidJson() {
        return "{\n" +
            "  \"id\": -5,\n" +
            "  \"name\": \"A\",\n" +
            "  \"email\": \"not-an-email\",\n" +
            "  \"status\": \"deleted\"\n" +
            "}";
    }

    private String getConfigJsonSchema() {
        return "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
            "  \"title\": \"ApplicationConfig\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"required\": [\"appName\", \"version\", \"database\", \"features\"],\n" +
            "  \"properties\": {\n" +
            "    \"appName\": { \"type\": \"string\", \"minLength\": 1 },\n" +
            "    \"version\": { \"type\": \"string\", \"pattern\": \"^\\\\d+\\\\.\\\\d+\\\\.\\\\d+$\" },\n" +
            "    \"debug\": { \"type\": \"boolean\" },\n" +
            "    \"database\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"required\": [\"host\", \"port\", \"name\"],\n" +
            "      \"properties\": {\n" +
            "        \"host\": { \"type\": \"string\" },\n" +
            "        \"port\": { \"type\": \"integer\", \"minimum\": 1, \"maximum\": 65535 },\n" +
            "        \"name\": { \"type\": \"string\", \"pattern\": \"^[a-zA-Z_][a-zA-Z0-9_]*$\" }\n" +
            "      }\n" +
            "    },\n" +
            "    \"features\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"minItems\": 1,\n" +
            "      \"items\": {\n" +
            "        \"type\": \"string\",\n" +
            "        \"enum\": [\"auth\", \"logging\", \"cache\", \"api\", \"webhook\"]\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    }

    private String getConfigValidYaml() {
        return "appName: MyApplication\n" +
            "version: \"2.1.0\"\n" +
            "debug: false\n" +
            "database:\n" +
            "  host: db.example.com\n" +
            "  port: 5432\n" +
            "  name: myapp_prod\n" +
            "features:\n" +
            "  - auth\n" +
            "  - logging\n" +
            "  - api";
    }

    private String getConfigInvalidYaml() {
        return "appName: \"\"\n" +
            "version: \"2.1\"\n" +
            "database:\n" +
            "  host: 192.168.1.1\n" +
            "  port: 70000\n" +
            "  name: \"123-invalid\"\n" +
            "features:\n" +
            "  - auth\n" +
            "  - unknown_feature";
    }

    private String getPacs008Xsd() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "           targetNamespace=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\"\n" +
            "           xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\"\n" +
            "           elementFormDefault=\"qualified\">\n" +
            "    <xs:element name=\"Document\">\n" +
            "        <xs:complexType>\n" +
            "            <xs:sequence>\n" +
            "                <xs:element name=\"FIToFICstmrCdtTrf\">\n" +
            "                    <xs:complexType>\n" +
            "                        <xs:sequence>\n" +
            "                            <xs:element name=\"GrpHdr\">\n" +
            "                                <xs:complexType>\n" +
            "                                    <xs:sequence>\n" +
            "                                        <xs:element name=\"MsgId\" type=\"Max35Text\"/>\n" +
            "                                        <xs:element name=\"CreDtTm\" type=\"ISODateTime\"/>\n" +
            "                                        <xs:element name=\"NbOfTxs\" type=\"Max15NumericText\"/>\n" +
            "                                        <xs:element name=\"CtrlSum\" type=\"DecimalNumber\"/>\n" +
            "                                    </xs:sequence>\n" +
            "                                </xs:complexType>\n" +
            "                            </xs:element>\n" +
            "                            <xs:element name=\"CdtTrfTxInf\" maxOccurs=\"unbounded\">\n" +
            "                                <xs:complexType>\n" +
            "                                    <xs:sequence>\n" +
            "                                        <xs:element name=\"PmtId\">\n" +
            "                                            <xs:complexType>\n" +
            "                                                <xs:sequence>\n" +
            "                                                    <xs:element name=\"InstrId\" type=\"Max35Text\" minOccurs=\"0\"/>\n" +
            "                                                    <xs:element name=\"EndToEndId\" type=\"Max35Text\"/>\n" +
            "                                                </xs:sequence>\n" +
            "                                            </xs:complexType>\n" +
            "                                        </xs:element>\n" +
            "                                        <xs:element name=\"Amt\">\n" +
            "                                            <xs:complexType>\n" +
            "                                                <xs:sequence>\n" +
            "                                                    <xs:element name=\"InstdAmt\">\n" +
            "                                                        <xs:complexType>\n" +
            "                                                            <xs:simpleContent>\n" +
            "                                                                <xs:extension base=\"DecimalNumber\">\n" +
            "                                                                    <xs:attribute name=\"Ccy\" type=\"CurrencyCode\" use=\"required\"/>\n" +
            "                                                                </xs:extension>\n" +
            "                                                            </xs:simpleContent>\n" +
            "                                                        </xs:complexType>\n" +
            "                                                    </xs:element>\n" +
            "                                                </xs:sequence>\n" +
            "                                            </xs:complexType>\n" +
            "                                        </xs:element>\n" +
            "                                        <xs:element name=\"Cdtr\">\n" +
            "                                            <xs:complexType>\n" +
            "                                                <xs:sequence>\n" +
            "                                                    <xs:element name=\"Nm\" type=\"Max140Text\"/>\n" +
            "                                                </xs:sequence>\n" +
            "                                            </xs:complexType>\n" +
            "                                        </xs:element>\n" +
            "                                    </xs:sequence>\n" +
            "                                </xs:complexType>\n" +
            "                            </xs:element>\n" +
            "                        </xs:sequence>\n" +
            "                    </xs:complexType>\n" +
            "                </xs:element>\n" +
            "            </xs:sequence>\n" +
            "        </xs:complexType>\n" +
            "    </xs:element>\n" +
            "    <xs:simpleType name=\"Max35Text\">\n" +
            "        <xs:restriction base=\"xs:string\">\n" +
            "            <xs:maxLength value=\"35\"/>\n" +
            "            <xs:minLength value=\"1\"/>\n" +
            "        </xs:restriction>\n" +
            "    </xs:simpleType>\n" +
            "    <xs:simpleType name=\"Max15NumericText\">\n" +
            "        <xs:restriction base=\"xs:string\">\n" +
            "            <xs:pattern value=\"[0-9]{1,15}\"/>\n" +
            "        </xs:restriction>\n" +
            "    </xs:simpleType>\n" +
            "    <xs:simpleType name=\"Max140Text\">\n" +
            "        <xs:restriction base=\"xs:string\">\n" +
            "            <xs:maxLength value=\"140\"/>\n" +
            "        </xs:restriction>\n" +
            "    </xs:simpleType>\n" +
            "    <xs:simpleType name=\"ISODateTime\">\n" +
            "        <xs:restriction base=\"xs:dateTime\"/>\n" +
            "    </xs:simpleType>\n" +
            "    <xs:simpleType name=\"DecimalNumber\">\n" +
            "        <xs:restriction base=\"xs:decimal\">\n" +
            "            <xs:fractionDigits value=\"5\"/>\n" +
            "            <xs:totalDigits value=\"18\"/>\n" +
            "        </xs:restriction>\n" +
            "    </xs:simpleType>\n" +
            "    <xs:simpleType name=\"CurrencyCode\">\n" +
            "        <xs:restriction base=\"xs:string\">\n" +
            "            <xs:pattern value=\"[A-Z]{3}\"/>\n" +
            "        </xs:restriction>\n" +
            "    </xs:simpleType>\n" +
            "</xs:schema>";
    }

    private String getPacs008ValidXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">\n" +
            "    <FIToFICstmrCdtTrf>\n" +
            "        <GrpHdr>\n" +
            "            <MsgId>MSG-2024-001</MsgId>\n" +
            "            <CreDtTm>2024-01-15T10:30:00Z</CreDtTm>\n" +
            "            <NbOfTxs>1</NbOfTxs>\n" +
            "            <CtrlSum>100000.00</CtrlSum>\n" +
            "        </GrpHdr>\n" +
            "        <CdtTrfTxInf>\n" +
            "            <PmtId>\n" +
            "                <InstrId>INST-001</InstrId>\n" +
            "                <EndToEndId>E2E-001</EndToEndId>\n" +
            "            </PmtId>\n" +
            "            <Amt>\n" +
            "                <InstdAmt Ccy=\"EUR\">100000.00</InstdAmt>\n" +
            "            </Amt>\n" +
            "            <Cdtr>\n" +
            "                <Nm>Global Trading Ltd</Nm>\n" +
            "            </Cdtr>\n" +
            "        </CdtTrfTxInf>\n" +
            "    </FIToFICstmrCdtTrf>\n" +
            "</Document>";
    }

    private String getPacs008InvalidXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">\n" +
            "    <FIToFICstmrCdtTrf>\n" +
            "        <GrpHdr>\n" +
            "            <MsgId></MsgId>\n" +
            "            <CreDtTm>2024-01-15</CreDtTm>\n" +
            "            <NbOfTxs>ABC</NbOfTxs>\n" +
            "            <CtrlSum>-1000.00</CtrlSum>\n" +
            "        </GrpHdr>\n" +
            "        <CdtTrfTxInf>\n" +
            "            <PmtId>\n" +
            "                <EndToEndId>This is a very long end to end identifier that exceeds thirty five characters limit</EndToEndId>\n" +
            "            </PmtId>\n" +
            "            <Amt>\n" +
            "                <InstdAmt Ccy=\"EURO\">100000.00</InstdAmt>\n" +
            "            </Amt>\n" +
            "            <Cdtr>\n" +
            "                <Nm>Global Trading Ltd</Nm>\n" +
            "            </Cdtr>\n" +
            "        </CdtTrfTxInf>\n" +
            "    </FIToFICstmrCdtTrf>\n" +
            "</Document>";
    }

    private String getFixedWidthSchema() {
        return "{\n" +
            "  \"recordLength\": 80,\n" +
            "  \"fields\": [\n" +
            "    { \"name\": \"recordType\", \"start\": 1, \"length\": 2, \"type\": \"string\", \"required\": true },\n" +
            "    { \"name\": \"accountNumber\", \"start\": 3, \"length\": 10, \"type\": \"string\", \"pattern\": \"^[0-9]{10}$\" },\n" +
            "    { \"name\": \"customerName\", \"start\": 13, \"length\": 30, \"type\": \"string\" },\n" +
            "    { \"name\": \"balance\", \"start\": 43, \"length\": 15, \"type\": \"decimal\" },\n" +
            "    { \"name\": \"currency\", \"start\": 58, \"length\": 3, \"type\": \"string\", \"enum\": [\"USD\", \"EUR\", \"GBP\"] },\n" +
            "    { \"name\": \"status\", \"start\": 61, \"length\": 1, \"type\": \"string\", \"enum\": [\"A\", \"I\", \"C\"] },\n" +
            "    { \"name\": \"lastUpdated\", \"start\": 62, \"length\": 8, \"type\": \"date\" }\n" +
            "  ]\n" +
            "}";
    }

    private String getFixedWidthValidTxt() {
        return "011234567890ACME CORPORATION              000001000000.00USDA20240115           \n" +
            "021234567891GLOBAL TRADING LTD            000005000000.50EURI20240116           ";
    }

    private String getCsvwMetadata() {
        return "{\n" +
            "  \"@context\": \"http://www.w3.org/ns/csvw\",\n" +
            "  \"url\": \"transactions.csv\",\n" +
            "  \"tableSchema\": {\n" +
            "    \"columns\": [\n" +
            "      {\n" +
            "        \"name\": \"transactionId\",\n" +
            "        \"titles\": \"Transaction ID\",\n" +
            "        \"datatype\": \"string\",\n" +
            "        \"required\": true,\n" +
            "        \"minLength\": 5,\n" +
            "        \"maxLength\": 20\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"date\",\n" +
            "        \"titles\": \"Date\",\n" +
            "        \"datatype\": \"date\",\n" +
            "        \"required\": true\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"amount\",\n" +
            "        \"titles\": \"Amount\",\n" +
            "        \"datatype\": \"number\",\n" +
            "        \"required\": true,\n" +
            "        \"minimum\": 0\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"currency\",\n" +
            "        \"titles\": \"Currency\",\n" +
            "        \"datatype\": \"string\",\n" +
            "        \"required\": true,\n" +
            "        \"format\": \"^[A-Z]{3}$\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"status\",\n" +
            "        \"titles\": \"Status\",\n" +
            "        \"datatype\": \"string\",\n" +
            "        \"required\": true,\n" +
            "        \"constraints\": {\n" +
            "          \"enum\": [\"PENDING\", \"COMPLETED\", \"FAILED\", \"CANCELLED\"]\n" +
            "        }\n" +
            "      }\n" +
            "    ],\n" +
            "    \"primaryKey\": \"transactionId\"\n" +
            "  }\n" +
            "}";
    }

    private String getTransactionsValidCsv() {
        return "Transaction ID,Date,Amount,Currency,Status\n" +
            "TXN-001,2024-01-15,1500.00,USD,COMPLETED\n" +
            "TXN-002,2024-01-16,2500.50,EUR,PENDING\n" +
            "TXN-003,2024-01-17,999.99,GBP,COMPLETED";
    }

    private String getTransactionsInvalidCsv() {
        return "Transaction ID,Date,Amount,Currency,Status\n" +
            "TXN-001,2024-01-15,1500.00,USD,COMPLETED\n" +
            "TXN-002,not-a-date,-500.00,EUR,PENDING\n" +
            "TXN-003,2024-01-17,999.99,EURO,UNKNOWN\n" +
            "TXN-001,2024-01-18,2000.00,GBP,COMPLETED";
    }

    private String getMt103ValidTxt() {
        return "{1:F01BANKBEBBAXXX1234567890}\n" +
            "{2:I103BANKUS33XXXXN}\n" +
            "{4:\n" +
            ":20:REFERENCE123456\n" +
            ":23B:CRED\n" +
            ":32A:240115USD100000,\n" +
            ":50K:/123456789\n" +
            "ACME CORPORATION\n" +
            "123 BUSINESS STREET\n" +
            "NEW YORK NY 10001\n" +
            ":59:/987654321\n" +
            "GLOBAL TRADING LTD\n" +
            "456 COMMERCE AVE\n" +
            "LONDON EC2A 4DP\n" +
            ":71A:SHA\n" +
            "-}\n" +
            "{5:{CHK:1234567890ABC}}";
    }

    private String getMt103InvalidTxt() {
        return "{1:F01BANKBEBBAXXX1234567890}\n" +
            "{2:I103BANKUS33XXXXN}\n" +
            "{4:\n" +
            ":20:REF\n" +
            ":23B:INVALID_CODE\n" +
            ":32A:240115USD-50000,\n" +
            ":50K:/123\n" +
            "ACME\n" +
            ":59:/987\n" +
            "GLOBAL\n" +
            "-}";
    }

    private String getMt202ValidTxt() {
        return "{1:F01BANKBEBBAXXX1234567890}\n" +
            "{2:I202BANKUS33XXXXN}\n" +
            "{4:\n" +
            ":20:REF2024001\n" +
            ":21:RELATEDREF001\n" +
            ":32A:240115EUR500000,\n" +
            ":58A:BANKFRPPXXX\n" +
            "-}\n" +
            "{5:{CHK:ABCDEF123456}}";
    }

    private String getMt940ValidTxt() {
        return "{1:F01BANKBEBBAXXX1234567890}\n" +
            "{2:I940BANKUS33XXXXN}\n" +
            "{4:\n" +
            ":20:STATEMENT001\n" +
            ":25:1234567890\n" +
            ":28C:001/01\n" +
            ":60F:C240115EUR1000000,\n" +
            ":61:2401150115D50000,NTRFNONREF\n" +
            ":86:INVOICE PAYMENT\n" +
            ":62F:C240115EUR950000,\n" +
            "-}\n" +
            "{5:{CHK:1234567890ABC}}";
    }

    private String getMt103ValidEnhancedTxt() {
        return "{1:F01BANKBEBBAXXX1234567890}\n" +
            "{2:I103BANKUS33XXXXN}\n" +
            "{4:\n" +
            ":20:EXB-2024-000001\n" +
            ":23B:CRED\n" +
            ":32A:240115USD500000,\n" +
            ":50K:/123456789\n" +
            "ACME CORPORATION\n" +
            "123 BUSINESS STREET\n" +
            "NEW YORK NY 10001\n" +
            ":59:/987654321\n" +
            "GLOBAL TRADING LTD\n" +
            "456 COMMERCE AVE\n" +
            "LONDON EC2A 4DP\n" +
            ":71A:SHA\n" +
            "-}\n" +
            "{5:{CHK:1234567890ABC}}";
    }

    private String getMt103InvalidEnhancedTxt() {
        return "{1:F01BANKBEBBAXXX1234567890}\n" +
            "{2:I103BANKUS33XXXXN}\n" +
            "{4:\n" +
            ":20:REF-123456\n" +
            ":23B:CRED\n" +
            ":32A:240115RUB15000000,\n" +
            ":50K:/123456789\n" +
            "ACME CORPORATION\n" +
            "123 BUSINESS STREET\n" +
            "NEW YORK NY 10001\n" +
            ":59:/987654321\n" +
            "GLOBAL TRADING LTD\n" +
            "456 COMMERCE AVE\n" +
            "TEHRAN IRAN\n" +
            ":71A:SHA\n" +
            "-}\n" +
            "{5:{CHK:1234567890ABC}}";
    }

    private String getCustomMtRulesJson() {
        return "{\n" +
            "  \"enhanced\": true,\n" +
            "  \"refPrefix\": \"EXB-\",\n" +
            "  \"amountLimit\": 10000000.0,\n" +
            "  \"restrictedCurrencies\": [\"RUB\"],\n" +
            "  \"highRiskJurisdictions\": [\"Iran\", \"Tehran\"]\n" +
            "}";
    }
}
