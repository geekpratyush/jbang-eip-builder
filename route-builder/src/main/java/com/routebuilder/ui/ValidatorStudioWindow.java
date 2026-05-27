package com.routebuilder.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
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
import netscape.javascript.JSObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

public class ValidatorStudioWindow {
    public static final java.util.List<ValidatorStudioWindow> activeInstances = new java.util.ArrayList<>();

    private final Stage stage;
    private File workspaceRoot;
    private TreeView<File> treeView;
    private TextField txtSearch;
    private TabPane tabPane;
    private final Map<File, Tab> openTabs = new HashMap<>();
    private final Map<Tab, WebEngine> tabEngines = new HashMap<>();
    private final Map<Tab, File> tabFiles = new HashMap<>();

    private ComboBox<String> studioThemeBox;
    private ComboBox<String> cmbValidatorType;
    private RadioButton radStandard;
    private RadioButton radEnhanced;

    // Results panel components
    private Label lblStatus;
    private Label lblStats;
    private ListView<String> lstResults;

    // Mapping fields
    private Label lblMappedSchema;
    private Button btnLinkSchema;
    private Button btnMappingStudio;
    private String currentThemeName = RouteBuilderApp.currentThemeName;
    private String mermaidScriptTag = "";
    private WebView webViewMappingMap;
    private TreeView<File> treeMapMessage;
    private TreeView<File> treeMapSchema;
    private ComboBox<String> cmbMapFormat;

    public ValidatorStudioWindow() {
        activeInstances.add(this);
        this.stage = new Stage();
        this.workspaceRoot = new File(System.getProperty("user.dir"), "validator-workspace");
        initializeWorkspace();
        loadMermaidJs();
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
                "    { \"messagePath\": \"messages/xml/invoice-valid.xml\", \"schemaPath\": \"schemas/xsd/invoice-schema.xsd\", \"type\": \"XML + XSD\" },\n" +
                "    { \"messagePath\": \"messages/xml/invoice-invalid.xml\", \"schemaPath\": \"schemas/xsd/invoice-schema.xsd\", \"type\": \"XML + XSD\" },\n" +
                "    { \"messagePath\": \"messages/json/customer-valid.json\", \"schemaPath\": \"schemas/json-schema/customer-schema.json\", \"type\": \"JSON + Schema\" },\n" +
                "    { \"messagePath\": \"messages/json/customer-invalid.json\", \"schemaPath\": \"schemas/json-schema/customer-schema.json\", \"type\": \"JSON + Schema\" },\n" +
                "    { \"messagePath\": \"messages/yaml/config-valid.yaml\", \"schemaPath\": \"schemas/json-schema/config-schema.json\", \"type\": \"YAML + Schema\" },\n" +
                "    { \"messagePath\": \"messages/yaml/config-invalid.yaml\", \"schemaPath\": \"schemas/json-schema/config-schema.json\", \"type\": \"YAML + Schema\" },\n" +
                "    { \"messagePath\": \"messages/csv/transactions-valid.csv\", \"schemaPath\": \"schemas/csv/transactions-metadata.json\", \"type\": \"CSV + CSVW\" },\n" +
                "    { \"messagePath\": \"messages/csv/transactions-invalid.csv\", \"schemaPath\": \"schemas/csv/transactions-metadata.json\", \"type\": \"CSV + CSVW\" },\n" +
                "    { \"messagePath\": \"messages/flatfile/fixedwidth-valid.txt\", \"schemaPath\": \"schemas/flatfile/fixedwidth-schema.json\", \"type\": \"Flat File\" },\n" +
                "    { \"messagePath\": \"messages/iso20022/pacs008-valid.xml\", \"schemaPath\": \"schemas/iso20022/pacs008-schema.xsd\", \"type\": \"ISO 20022 MX\" },\n" +
                "    { \"messagePath\": \"messages/iso20022/pacs008-invalid.xml\", \"schemaPath\": \"schemas/iso20022/pacs008-schema.xsd\", \"type\": \"ISO 20022 MX\" }\n" +
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
        stage.setTitle("Universal Message & Schema Validator IDE");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.getStyleClass().add(RouteBuilderApp.currentThemeClass);

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("editor-toolbar");

        Button btnValidate = new Button("Validate", new FontIcon("fas-check-double"));
        btnValidate.getStyleClass().addAll("toolbar-btn", "btn-validate");
        btnValidate.setOnAction(e -> runValidation());

        Button btnSave = new Button("Save", new FontIcon("fas-save"));
        btnSave.getStyleClass().addAll("toolbar-btn", "btn-save");
        btnSave.setOnAction(e -> saveCurrentTab());

        cmbValidatorType = new ComboBox<>();
        cmbValidatorType.getItems().addAll("Auto-Detect", "XML + XSD", "JSON + Schema", "YAML + Schema", "SWIFT MT Message", "ISO 20022 MX", "CSV + CSVW", "Flat File");
        cmbValidatorType.setValue("Auto-Detect");
        cmbValidatorType.setTooltip(new Tooltip("Select validator mode or let it auto-detect based on file extension"));

        ToggleGroup modeGroup = new ToggleGroup();
        radStandard = new RadioButton("Standard");
        radStandard.setToggleGroup(modeGroup);
        radStandard.setSelected(true);
        radStandard.getStyleClass().add("radio-theme-standard");
        radEnhanced = new RadioButton("Enhanced (+Rules)");
        radEnhanced.setToggleGroup(modeGroup);
        radEnhanced.getStyleClass().add("radio-theme-enhanced");

        studioThemeBox = new ComboBox<>();
        studioThemeBox.getItems().addAll("VSCode Dark", "IntelliJ Light", "Dracula", "Monokai", "Hacker");
        studioThemeBox.setValue(RouteBuilderApp.currentThemeName);
        studioThemeBox.setTooltip(new Tooltip("Change Theme"));
        studioThemeBox.setOnAction(e -> RouteBuilderApp.setGlobalTheme(studioThemeBox.getValue()));

        lblMappedSchema = new Label("Schema: None");
        lblMappedSchema.getStyleClass().add("mapped-schema-label");
        lblMappedSchema.setMaxWidth(200);

        btnLinkSchema = new Button("Link Schema", new FontIcon("fas-link"));
        btnLinkSchema.getStyleClass().addAll("toolbar-btn", "btn-link-schema");
        btnLinkSchema.setOnAction(e -> openLinkSchemaDialogForActiveFile());

        btnMappingStudio = new Button("Mapping Studio", new FontIcon("fas-project-diagram"));
        btnMappingStudio.getStyleClass().addAll("toolbar-btn", "btn-mapping-studio");
        btnMappingStudio.setOnAction(e -> openMappingStudioTab());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(
            btnValidate, btnSave, new Separator(),
            new Label("Format:"), cmbValidatorType, new Separator(),
            lblMappedSchema, btnLinkSchema, btnMappingStudio, new Separator(),
            new Label("SWIFT Mode:"), radStandard, radEnhanced, new Separator(),
            spacer, studioThemeBox
        );
        root.setTop(toolBar);

        // --- Sidebar (Explorer) ---
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(260);
        sidebar.setPadding(new Insets(8));
        sidebar.setSpacing(5);
        sidebar.getStyleClass().add("studio-sidebar");

        Label lblExplorer = new Label("VALIDATOR EXPLORER");
        lblExplorer.getStyleClass().add("studio-explorer-label");

        txtSearch = new TextField();
        txtSearch.setPromptText("Filter files...");
        txtSearch.getStyleClass().add("sidebar-search-box");
        txtSearch.textProperty().addListener((obs, old, newVal) -> refreshTree());

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

        setupTreeContextMenu();

        sidebar.getChildren().addAll(lblExplorer, txtSearch, treeView);

        // --- Editors Area ---
        tabPane = new TabPane();
        tabPane.getStyleClass().add("editor-tab-pane");
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            updateToolbarForTab(newTab);
        });

        // --- Bottom Results Panel ---
        BorderPane resultsPane = new BorderPane();
        resultsPane.getStyleClass().add("results-pane");
        resultsPane.setPrefHeight(250);

        HBox resultsHeader = new HBox(10);
        resultsHeader.setPadding(new Insets(5, 10, 5, 10));
        resultsHeader.getStyleClass().add("results-header");
        resultsHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        lblStatus = new Label("Ready");
        lblStatus.getStyleClass().add("results-status-label");

        lblStats = new Label("");
        lblStats.getStyleClass().add("results-stats-label");

        Region resultsSpacer = new Region();
        HBox.setHgrow(resultsSpacer, Priority.ALWAYS);

        Button btnClear = new Button("Clear", new FontIcon("fas-eraser"));
        btnClear.getStyleClass().add("small-action-btn");
        btnClear.setOnAction(e -> {
            lstResults.getItems().clear();
            lblStatus.setText("Ready");
            lblStats.setText("");
        });

        resultsHeader.getChildren().addAll(lblStatus, lblStats, resultsSpacer, btnClear);
        resultsPane.setTop(resultsHeader);

        lstResults = new ListView<>();
        lstResults.getStyleClass().add("results-list-view");
        lstResults.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                String selected = lstResults.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    navigateEditorToErrorLine(selected);
                }
            }
        });
        resultsPane.setCenter(lstResults);

        // Split Editor & Results
        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.getItems().addAll(tabPane, resultsPane);
        mainSplit.setDividerPositions(0.7);

        // Left sidebar & right workspace split
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.getItems().addAll(sidebar, mainSplit);
        horizontalSplit.setDividerPositions(0.18);

        root.setCenter(horizontalSplit);

        Scene scene = new Scene(root, 1400, 900);
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

        // Add keyboard shortcuts
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
            () -> saveCurrentTab()
        );
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.F5),
            () -> runValidation()
        );
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN),
            () -> {
                if (horizontalSplit.getDividerPositions()[0] > 0.05) {
                    horizontalSplit.setDividerPositions(0.0);
                } else {
                    horizontalSplit.setDividerPositions(0.18);
                }
            }
        );

        stage.setMaximized(true);
        stage.show();

        refreshTree();
    }

    public void setTheme(String themeName) {
        String themeClass = "theme-" + themeName.toLowerCase().replace(" ", "-");
        Platform.runLater(() -> {
            if (studioThemeBox != null && !themeName.equals(studioThemeBox.getValue())) {
                studioThemeBox.setValue(themeName);
            }
            for (WebEngine engine : tabEngines.values()) {
                applyMonacoTheme(engine, themeClass);
            }
            this.currentThemeName = themeName;
            if (webViewMappingMap != null) {
                webViewMappingMap.getEngine().loadContent(generateMappingBaseHtml(mermaidScriptTag, themeName));
            }
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

    private void refreshTree() {
        File rootDir = workspaceRoot;
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        TreeItem<File> rootItem = new TreeItem<>(rootDir);
        rootItem.setExpanded(true);
        buildFileTree(rootDir, rootItem, txtSearch.getText().trim().toLowerCase());
        treeView.setRoot(rootItem);
    }

    private void buildFileTree(File dir, TreeItem<File> parentItem, String filter) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (File file : files) {
            if (file.isDirectory()) {
                TreeItem<File> dirItem = new TreeItem<>(file);
                // Simple representation check
                if (filter.isEmpty() || matchesFilterRecursive(file, filter)) {
                    parentItem.getChildren().add(dirItem);
                    dirItem.setExpanded(true);
                    buildFileTree(file, dirItem, filter);
                }
            } else {
                if (filter.isEmpty() || file.getName().toLowerCase().contains(filter)) {
                    TreeItem<File> fileItem = new TreeItem<>(file);
                    parentItem.getChildren().add(fileItem);
                }
            }
        }
    }

    private boolean matchesFilterRecursive(File dir, String filter) {
        if (dir.getName().toLowerCase().contains(filter)) return true;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    if (matchesFilterRecursive(child, filter)) return true;
                } else {
                    if (child.getName().toLowerCase().contains(filter)) return true;
                }
            }
        }
        return false;
    }

    private void setupTreeContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem mnuNewFile = new MenuItem("New File", new FontIcon("fas-file-medical"));
        mnuNewFile.setOnAction(e -> {
            TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
            final File parentDir = (selected != null) ? 
                (selected.getValue().isDirectory() ? selected.getValue() : selected.getValue().getParentFile()) : 
                workspaceRoot;
            TextInputDialog dialog = new TextInputDialog("new_file.txt");
            dialog.setTitle("New File");
            dialog.setHeaderText("Create a new file in: " + parentDir.getName());
            dialog.setContentText("File name:");
            dialog.showAndWait().ifPresent(name -> {
                File f = new File(parentDir, name);
                try {
                    if (f.createNewFile()) {
                        refreshTree();
                        openFile(f);
                    }
                } catch (Exception ex) {
                    showError("Create File Failed", ex.getMessage());
                }
            });
        });

        MenuItem mnuNewFolder = new MenuItem("New Folder", new FontIcon("fas-folder-plus"));
        mnuNewFolder.setOnAction(e -> {
            TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
            final File parentDir = (selected != null) ? 
                (selected.getValue().isDirectory() ? selected.getValue() : selected.getValue().getParentFile()) : 
                workspaceRoot;
            TextInputDialog dialog = new TextInputDialog("NewFolder");
            dialog.setTitle("New Folder");
            dialog.setHeaderText("Create a new folder in: " + parentDir.getName());
            dialog.setContentText("Folder name:");
            dialog.showAndWait().ifPresent(name -> {
                File f = new File(parentDir, name);
                if (f.mkdirs()) {
                    refreshTree();
                }
            });
        });

        MenuItem mnuRename = new MenuItem("Rename", new FontIcon("fas-edit"));
        mnuRename.setOnAction(e -> {
            TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue().equals(workspaceRoot)) return;
            File current = selected.getValue();
            TextInputDialog dialog = new TextInputDialog(current.getName());
            dialog.setTitle("Rename File/Folder");
            dialog.setHeaderText("Rename: " + current.getName());
            dialog.setContentText("New name:");
            dialog.showAndWait().ifPresent(name -> {
                File dest = new File(current.getParentFile(), name);
                if (current.renameTo(dest)) {
                    refreshTree();
                    // Update active tab if open
                    Tab tab = openTabs.remove(current);
                    if (tab != null) {
                        tab.setText(name);
                        openTabs.put(dest, tab);
                        tabFiles.put(tab, dest);
                    }
                }
            });
        });

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
                    Tab tab = openTabs.remove(target);
                    if (tab != null) {
                        tabPane.getTabs().remove(tab);
                        tabEngines.remove(tab);
                        tabFiles.remove(tab);
                    }
                }
            });
        });

        MenuItem mnuDuplicate = new MenuItem("Duplicate", new FontIcon("fas-copy"));
        mnuDuplicate.setOnAction(e -> {
            TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || !selected.getValue().isFile()) return;
            File src = selected.getValue();
            File dest = new File(src.getParentFile(), "copy_" + src.getName());
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                refreshTree();
            } catch (Exception ex) {
                showError("Duplicate Failed", ex.getMessage());
            }
        });

        contextMenu.getItems().addAll(mnuNewFile, mnuNewFolder, new SeparatorMenuItem(), mnuRename, mnuDelete, mnuDuplicate);

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
                        if (item.isDirectory()) {
                            setGraphic(new FontIcon("fas-folder"));
                        } else {
                            String ext = getFileExtension(item);
                            switch (ext) {
                                case "xml":
                                case "xsd":
                                    setGraphic(new FontIcon("fas-file-code"));
                                    break;
                                case "json":
                                    setGraphic(new FontIcon("fas-file-alt"));
                                    break;
                                case "yaml":
                                case "yml":
                                    setGraphic(new FontIcon("fas-file-signature"));
                                    break;
                                case "csv":
                                    setGraphic(new FontIcon("fas-file-excel"));
                                    break;
                                default:
                                    setGraphic(new FontIcon("fas-file"));
                                    break;
                            }
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

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        f.delete();
    }

    private void openFile(File file) {
        if (openTabs.containsKey(file)) {
            tabPane.getSelectionModel().select(openTabs.get(file));
            return;
        }

        Tab tab = new Tab(file.getName());
        WebView wv = new WebView();
        RouteBuilderApp.installClipboardShortcuts(wv);
        wv.setContextMenuEnabled(true);
        WebEngine engine = wv.getEngine();

        tab.setContent(wv);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        openTabs.put(file, tab);
        tabEngines.put(tab, engine);
        tabFiles.put(tab, file);

        tab.setOnClosed(e -> {
            openTabs.remove(file);
            tabEngines.remove(tab);
            tabFiles.remove(tab);
        });

        String ext = getFileExtension(file);
        String lang = "plaintext";
        if ("xml".equals(ext) || "xsd".equals(ext)) lang = "xml";
        else if ("json".equals(ext)) lang = "json";
        else if ("yaml".equals(ext) || "yml".equals(ext)) lang = "yaml";
        else if ("csv".equals(ext)) lang = "csv";

        final String targetLang = lang;
        setupMonaco(engine, lang, (obs, oldVal, newVal) -> {
            if (newVal == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    String content = Files.readString(file.toPath());
                    setEditorText(engine, content);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void saveCurrentTab() {
        Tab active = tabPane.getSelectionModel().getSelectedItem();
        if (active == null) return;
        File f = tabFiles.get(active);
        WebEngine engine = tabEngines.get(active);
        if (f != null && engine != null) {
            String content = getEditorText(engine);
            try {
                Files.writeString(f.toPath(), content);
                lblStatus.setText("Saved " + f.getName());
            } catch (Exception ex) {
                showError("Save Failed", ex.getMessage());
            }
        }
    }

    private void setupMonaco(WebEngine engine, String language, javafx.beans.value.ChangeListener<javafx.concurrent.Worker.State> onSucceeded) {
        String monacoBase = getClass().getResource("/monaco/vs/loader.js").toExternalForm();
        monacoBase = monacoBase.substring(0, monacoBase.lastIndexOf("/vs/loader.js"));
        
        String activeTheme = RouteBuilderApp.currentThemeClass;
        String editorBg = "#1e1e1e";
        if ("theme-intellij-light".equals(activeTheme)) editorBg = "#ffffff";
        else if ("theme-dracula".equals(activeTheme)) editorBg = "#282a36";
        else if ("theme-monokai".equals(activeTheme)) editorBg = "#272822";
        else if ("theme-hacker".equals(activeTheme)) editorBg = "#050505";

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>body{margin:0;padding:0;overflow:hidden;background-color:" + editorBg + ";}#editor{width:100vw;height:100vh;}</style></head><body><div id='editor'></div><script src='" + monacoBase + "/vs/loader.js'></script><script>\n" +
            "window.editorValue = ''; window.setValue = function(val) { window.editorValue = val; if(window.editor) window.editor.setValue(val); };\n" +
            "window.getValue = function() { return window.editor ? window.editor.getValue() : window.editorValue; };\n" +
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
            "  window.editor = monaco.editor.create(document.getElementById('editor'), { value: window.editorValue, language: '" + ("text".equals(language) ? "swift-mt" : language) + "', theme: '" + activeTheme + "', automaticLayout: true, minimap: { enabled: true }, fontSize: 12 });\n" +
            "});\n</script></body></html>";

        engine.getLoadWorker().stateProperty().addListener(onSucceeded);
        engine.loadContent(html);
    }

    private void setEditorText(WebEngine engine, String text) {
        if (text == null) text = ""; final String finalT = text;
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

    private void navigateEditorToErrorLine(String logLine) {
        Tab active = tabPane.getSelectionModel().getSelectedItem();
        if (active == null) return;
        WebEngine engine = tabEngines.get(active);
        if (engine == null) return;

        Pattern p = Pattern.compile("Line (\\d+)");
        Matcher m = p.matcher(logLine);
        if (m.find()) {
            int line = Integer.parseInt(m.group(1));
            engine.executeScript("if(window.editor) { window.editor.revealLine(" + line + "); window.editor.setPosition({lineNumber: " + line + ", column: 1}); window.editor.focus(); }");
        }
    }

    private void runValidation() {
        Tab active = tabPane.getSelectionModel().getSelectedItem();
        if (active == null) {
            showError("No Active File", "Please open a file to validate.");
            return;
        }

        File file = tabFiles.get(active);
        WebEngine engine = tabEngines.get(active);
        if (file == null || engine == null) return;

        String content = getEditorText(engine);
        if (content.isEmpty()) {
            lstResults.getItems().clear();
            lstResults.getItems().add("Error: File is empty.");
            updateStatusLabel(false, 0);
            return;
        }

        String mappedType = getMappedType(file);
        String valType = cmbValidatorType.getValue();
        if ("Auto-Detect".equals(valType) && mappedType != null) {
            valType = mappedType;
        } else if ("Auto-Detect".equals(valType)) {
            valType = autoDetectType(file, content);
        }

        String mappedSchemaPath = getMappedSchema(file);
        File schemaFile = null;
        if (mappedSchemaPath != null && !mappedSchemaPath.isEmpty()) {
            schemaFile = new File(workspaceRoot, mappedSchemaPath);
        }

        lstResults.getItems().clear();
        long start = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();

        try {
            switch (valType) {
                case "XML + XSD":
                    validateXmlAndXsd(content, workspaceRoot, schemaFile, errors);
                    break;
                case "JSON + Schema":
                    validateJsonWithSchema(content, workspaceRoot, schemaFile, errors);
                    break;
                case "YAML + Schema":
                    validateYamlWithSchema(content, workspaceRoot, schemaFile, errors);
                    break;
                case "SWIFT MT Message":
                    validateSwiftMt(content, radEnhanced.isSelected(), errors);
                    break;
                case "ISO 20022 MX":
                    validateIso20022Mx(content, workspaceRoot, schemaFile, errors);
                    break;
                case "CSV + CSVW":
                    validateCsvW(content, workspaceRoot, schemaFile, errors);
                    break;
                case "Flat File":
                    validateFlatFile(content, workspaceRoot, schemaFile, errors);
                    break;
                default:
                    errors.add("Unsupported validation type: " + valType);
                    break;
            }
        } catch (Exception ex) {
            errors.add("Validation processing error: " + ex.getMessage());
        }

        long duration = System.currentTimeMillis() - start;

        if (errors.isEmpty()) {
            lstResults.getItems().add("✅ Validation Successful! The file is clean and conforms to its rules.");
            updateStatusLabel(true, duration);
        } else {
            for (String err : errors) {
                lstResults.getItems().add("❌ " + err);
            }
            updateStatusLabel(false, duration);
        }
    }

    private String autoDetectType(File file, String content) {
        String ext = getFileExtension(file);
        if ("xsd".equals(ext)) return "XML + XSD";
        if ("xml".equals(ext)) {
            if (content.contains("urn:iso:std:iso:20022")) return "ISO 20022 MX";
            return "XML + XSD";
        }
        if ("json".equals(ext)) return "JSON + Schema";
        if ("yaml".equals(ext) || "yml".equals(ext)) return "YAML + Schema";
        if ("csv".equals(ext)) return "CSV + CSVW";
        if (content.contains("{1:") && content.contains("{4:")) return "SWIFT MT Message";
        return "Flat File";
    }

    // --- Validation Engines Implementation ---

    public static void validateXmlAndXsd(String content, File workspaceRoot, List<String> errors) {
        validateXmlAndXsd(content, workspaceRoot, null, errors);
    }

    public static void validateXmlAndXsd(String content, File workspaceRoot, File schemaFile, List<String> errors) {
        if (schemaFile == null) {
            schemaFile = new File(workspaceRoot, "schemas/xsd/invoice-schema.xsd");
        }
        if (!schemaFile.exists()) {
            errors.add("Schema schema file missing at: " + schemaFile.getAbsolutePath());
            return;
        }
        try {
            String xsdContent = Files.readString(schemaFile.toPath());
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
            validator.validate(new StreamSource(new StringReader(content)));
        } catch (Exception ex) {
            errors.add("XML Validation failed: " + ex.getMessage());
        }
    }

    public static void validateJsonWithSchema(String content, File workspaceRoot, List<String> errors) {
        validateJsonWithSchema(content, workspaceRoot, null, errors);
    }

    public static void validateJsonWithSchema(String content, File workspaceRoot, File schemaFile, List<String> errors) {
        if (schemaFile == null) {
            schemaFile = new File(workspaceRoot, "schemas/json-schema/customer-schema.json");
        }
        if (!schemaFile.exists()) {
            errors.add("Schema file missing.");
            return;
        }
        try {
            JSONObject schemaJson = new JSONObject(Files.readString(schemaFile.toPath()));
            JSONObject dataJson = new JSONObject(content);
            validateJsonNode(dataJson, schemaJson, "", errors);
        } catch (Exception ex) {
            errors.add("JSON Syntax/Parsing Error: " + ex.getMessage());
        }
    }

    public static void validateYamlWithSchema(String content, File workspaceRoot, List<String> errors) {
        validateYamlWithSchema(content, workspaceRoot, null, errors);
    }

    public static void validateYamlWithSchema(String content, File workspaceRoot, File schemaFile, List<String> errors) {
        if (schemaFile == null) {
            schemaFile = new File(workspaceRoot, "schemas/json-schema/config-schema.json");
        }
        if (!schemaFile.exists()) {
            errors.add("Schema file missing.");
            return;
        }
        try {
            com.fasterxml.jackson.dataformat.yaml.YAMLFactory yamlFactory = new com.fasterxml.jackson.dataformat.yaml.YAMLFactory();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper(yamlFactory);
            Object obj = mapper.readValue(content, Object.class);

            com.fasterxml.jackson.databind.ObjectMapper jsonMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonString = jsonMapper.writeValueAsString(obj);

            JSONObject schemaJson = new JSONObject(Files.readString(schemaFile.toPath()));
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

    public static void validateSwiftMt(String content, boolean enhanced, List<String> errors) {
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
                    if (enhanced && !ref.startsWith("EXB-")) {
                        errors.add("Line " + humanLine + " - Field 20: Reference format must start with EXB- (CUSTOM-004)");
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
                    // Match date(6) + currency(3) + amount
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
                            if (enhanced && valAmount > 10000000.0) {
                                errors.add("Line " + humanLine + " - Field 32A: Amount exceeds 10M limit (CUSTOM-001)");
                            }
                            if (enhanced && "RUB".equals(valCcy)) {
                                errors.add("Line " + humanLine + " - Field 32A: Restricted currency RUB (CUSTOM-002)");
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
                    if (enhanced && (rest.contains("Iran") || rest.contains("Tehran"))) {
                        errors.add("Line " + humanLine + " - Field 59: High-risk jurisdiction Iran detected (CUSTOM-003)");
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
                    if (enhanced && (line.contains("Iran") || line.contains("Tehran"))) {
                        errors.add("Line " + humanLine + " - Field 59: High-risk jurisdiction Iran detected (CUSTOM-003)");
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

    public static void validateIso20022Mx(String content, File workspaceRoot, List<String> errors) {
        validateIso20022Mx(content, workspaceRoot, null, errors);
    }

    public static void validateIso20022Mx(String content, File workspaceRoot, File schemaFile, List<String> errors) {
        if (schemaFile == null) {
            schemaFile = new File(workspaceRoot, "schemas/iso20022/pacs008-schema.xsd");
        }
        if (!schemaFile.exists()) {
            errors.add("ISO 20022 pacs.008 schema file missing.");
            return;
        }
        try {
            String xsdContent = Files.readString(schemaFile.toPath());
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
            validator.validate(new StreamSource(new StringReader(content)));
        } catch (Exception ex) {
            errors.add("ISO XML Validation failed: " + ex.getMessage());
        }
    }

    public static void validateCsvW(String content, File workspaceRoot, List<String> errors) {
        validateCsvW(content, workspaceRoot, null, errors);
    }

    public static void validateCsvW(String content, File workspaceRoot, File schemaFile, List<String> errors) {
        if (schemaFile == null) {
            schemaFile = new File(workspaceRoot, "schemas/csv/transactions-metadata.json");
        }
        if (!schemaFile.exists()) {
            errors.add("CSV Schema metadata file missing.");
            return;
        }
        try {
            JSONObject metadata = new JSONObject(Files.readString(schemaFile.toPath()));
            JSONObject tableSchema = metadata.getJSONObject("tableSchema");
            JSONArray columns = tableSchema.getJSONArray("columns");

            String[] rows = content.split("\\r?\\n");
            if (rows.length == 0) {
                errors.add("CSV file is empty.");
                return;
            }

            // Header line
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

                    // datatype constraints
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

                    // Primary key uniqueness
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

    public static void validateFlatFile(String content, File workspaceRoot, List<String> errors) {
        validateFlatFile(content, workspaceRoot, null, errors);
    }

    public static void validateFlatFile(String content, File workspaceRoot, File schemaFile, List<String> errors) {
        if (schemaFile == null) {
            schemaFile = new File(workspaceRoot, "schemas/flatfile/fixedwidth-schema.json");
        }
        if (!schemaFile.exists()) {
            errors.add("Flat file fixed-width schema missing.");
            return;
        }
        try {
            JSONObject schema = new JSONObject(Files.readString(schemaFile.toPath()));
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

    private void updateStatusLabel(boolean success, long duration) {
        if (success) {
            lblStatus.setText("SUCCESS");
            lblStatus.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
        } else {
            lblStatus.setText("INVALID");
            lblStatus.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
        }
        lblStats.setText("Validated in " + duration + " ms");
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
            "  \"custom_rules\": [\n" +
            "    {\n" +
            "      \"id\": \"CUSTOM-001\",\n" +
            "      \"description\": \"Amount limit check > 10M\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"CUSTOM-002\",\n" +
            "      \"description\": \"Restricted currency RUB\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"CUSTOM-003\",\n" +
            "      \"description\": \"High-risk jurisdiction Iran\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"CUSTOM-004\",\n" +
            "      \"description\": \"Reference format starts with EXB-\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private void loadMermaidJs() {
        String mermaidJs = "";
        try (java.io.InputStream is = ValidatorStudioWindow.class.getResourceAsStream("/styles/mermaid.min.js")) {
            if (is != null) {
                mermaidJs = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!mermaidJs.isEmpty()) {
            this.mermaidScriptTag = "<script>" + mermaidJs + "</script>";
        } else {
            this.mermaidScriptTag = "<script src=\"https://cdn.jsdelivr.net/npm/mermaid@10.9.0/dist/mermaid.min.js\"></script>";
        }
    }

    public String getMappedSchema(File messageFile) {
        if (messageFile == null || workspaceRoot == null) return null;
        File mappingFile = new File(workspaceRoot, "validation-mapping.json");
        if (!mappingFile.exists()) return null;
        try {
            String content = Files.readString(mappingFile.toPath());
            JSONObject json = new JSONObject(content);
            JSONArray arr = json.optJSONArray("mappings");
            if (arr != null) {
                String relativePath = workspaceRoot.toURI().relativize(messageFile.toURI()).getPath();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String mPath = obj.optString("messagePath");
                    if (relativePath.equals(mPath) || messageFile.getName().equals(mPath)) {
                        return obj.optString("schemaPath");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getMappedType(File messageFile) {
        if (messageFile == null || workspaceRoot == null) return null;
        File mappingFile = new File(workspaceRoot, "validation-mapping.json");
        if (!mappingFile.exists()) return null;
        try {
            String content = Files.readString(mappingFile.toPath());
            JSONObject json = new JSONObject(content);
            JSONArray arr = json.optJSONArray("mappings");
            if (arr != null) {
                String relativePath = workspaceRoot.toURI().relativize(messageFile.toURI()).getPath();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String mPath = obj.optString("messagePath");
                    if (relativePath.equals(mPath) || messageFile.getName().equals(mPath)) {
                        return obj.optString("type");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveMapping(File messageFile, File schemaFile, String type) {
        if (messageFile == null || workspaceRoot == null) return;
        File mappingFile = new File(workspaceRoot, "validation-mapping.json");
        JSONObject json;
        JSONArray arr;
        try {
            if (mappingFile.exists()) {
                String content = Files.readString(mappingFile.toPath());
                json = new JSONObject(content);
                arr = json.optJSONArray("mappings");
                if (arr == null) {
                    arr = new JSONArray();
                    json.put("mappings", arr);
                }
            } else {
                json = new JSONObject();
                arr = new JSONArray();
                json.put("mappings", arr);
            }
            
            String relMsgPath = workspaceRoot.toURI().relativize(messageFile.toURI()).getPath();
            String relSchemaPath = schemaFile != null ? workspaceRoot.toURI().relativize(schemaFile.toURI()).getPath() : "";
            
            // Check if already exists, then update/replace
            boolean found = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (relMsgPath.equals(obj.optString("messagePath"))) {
                    if (schemaFile == null) {
                        arr.remove(i);
                    } else {
                        obj.put("schemaPath", relSchemaPath);
                        obj.put("type", type);
                    }
                    found = true;
                    break;
                }
            }
            
            if (!found && schemaFile != null) {
                JSONObject obj = new JSONObject();
                obj.put("messagePath", relMsgPath);
                obj.put("schemaPath", relSchemaPath);
                obj.put("type", type);
                arr.put(obj);
            }
            
            Files.writeString(mappingFile.toPath(), json.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateToolbarForTab(Tab tab) {
        if (tab == null) {
            if (lblMappedSchema != null) lblMappedSchema.setText("Schema: None");
            if (cmbValidatorType != null) cmbValidatorType.setValue("Auto-Detect");
            return;
        }
        File file = tabFiles.get(tab);
        if (file == null) {
            if (lblMappedSchema != null) lblMappedSchema.setText("Schema: N/A");
            return;
        }
        
        String schemaPath = getMappedSchema(file);
        if (schemaPath != null && !schemaPath.isEmpty()) {
            if (lblMappedSchema != null) lblMappedSchema.setText("Schema: " + new File(schemaPath).getName());
        } else {
            if (lblMappedSchema != null) lblMappedSchema.setText("Schema: None");
        }
        
        String mappedType = getMappedType(file);
        if (mappedType != null && !mappedType.isEmpty()) {
            if (cmbValidatorType != null) cmbValidatorType.setValue(mappedType);
        } else {
            if (cmbValidatorType != null) cmbValidatorType.setValue("Auto-Detect");
        }
    }

    private void openLinkSchemaDialogForActiveFile() {
        Tab active = tabPane.getSelectionModel().getSelectedItem();
        if (active == null) {
            showError("No Active File", "Please open a file to map it to a schema.");
            return;
        }
        File activeFile = tabFiles.get(active);
        if (activeFile == null) {
            showError("Invalid File", "Active tab is not a file editor.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Link Schema");
        dialog.setHeaderText("Link a schema file to: " + activeFile.getName());

        ButtonType btnTypeLink = new ButtonType("Link", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTypeLink, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> cmbSchemaList = new ComboBox<>();
        List<File> schemas = findAllSchemaFiles(workspaceRoot);
        for (File s : schemas) {
            String relPath = workspaceRoot.toURI().relativize(s.toURI()).getPath();
            cmbSchemaList.getItems().add(relPath);
        }
        cmbSchemaList.setPromptText("Select schema file...");

        ComboBox<String> cmbTypes = new ComboBox<>();
        cmbTypes.getItems().addAll("XML + XSD", "JSON + Schema", "YAML + Schema", "ISO 20022 MX", "CSV + CSVW", "Flat File");
        cmbTypes.setValue("XML + XSD");

        cmbSchemaList.setOnAction(e -> {
            String selected = cmbSchemaList.getValue();
            if (selected != null) {
                if (selected.endsWith(".xsd")) {
                    if (selected.contains("iso20022")) {
                        cmbTypes.setValue("ISO 20022 MX");
                    } else {
                        cmbTypes.setValue("XML + XSD");
                    }
                } else if (selected.endsWith(".json") || selected.endsWith(".csvw")) {
                    if (selected.contains("csv")) {
                        cmbTypes.setValue("CSV + CSVW");
                    } else if (selected.contains("flatfile")) {
                        cmbTypes.setValue("Flat File");
                    } else {
                        String actExt = getFileExtension(activeFile);
                        if ("yaml".equals(actExt) || "yml".equals(actExt)) {
                            cmbTypes.setValue("YAML + Schema");
                        } else {
                            cmbTypes.setValue("JSON + Schema");
                        }
                    }
                }
            }
        });

        String currentSchema = getMappedSchema(activeFile);
        if (currentSchema != null && !currentSchema.isEmpty()) {
            cmbSchemaList.setValue(currentSchema);
            String currentType = getMappedType(activeFile);
            if (currentType != null) {
                cmbTypes.setValue(currentType);
            }
        }

        grid.add(new Label("Schema File:"), 0, 0);
        grid.add(cmbSchemaList, 1, 0);
        grid.add(new Label("Validation Type:"), 0, 1);
        grid.add(cmbTypes, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == btnTypeLink) {
                String schemaRelPath = cmbSchemaList.getValue();
                if (schemaRelPath == null || schemaRelPath.isEmpty()) {
                    showError("No Schema Selected", "Please select a schema file.");
                    return;
                }
                File schemaFile = new File(workspaceRoot, schemaRelPath);
                String valType = cmbTypes.getValue();
                saveMapping(activeFile, schemaFile, valType);
                updateToolbarForTab(active);
                
                if (webViewMappingMap != null) {
                    refreshMappingDiagram();
                }
            }
        });
    }

    private List<File> findAllSchemaFiles(File dir) {
        List<File> list = new ArrayList<>();
        findAllSchemaFilesRec(dir, list);
        return list;
    }

    private void findAllSchemaFilesRec(File file, List<File> list) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    findAllSchemaFilesRec(child, list);
                }
            }
        } else {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".xsd") || name.endsWith(".json") || name.endsWith(".csvw")) {
                list.add(file);
            }
        }
    }

    private void openMappingStudioTab() {
        for (Tab tab : tabPane.getTabs()) {
            if ("Schema Mapping Studio".equals(tab.getText())) {
                tabPane.getSelectionModel().select(tab);
                refreshMappingDiagram();
                return;
            }
        }

        Tab tab = new Tab("Schema Mapping Studio", new FontIcon("fas-project-diagram"));
        
        SplitPane mainSplit = new SplitPane();
        mainSplit.setDividerPositions(0.6);

        BorderPane editorPane = new BorderPane();
        editorPane.getStyleClass().add("mapping-editor-pane");

        HBox mappingToolbar = new HBox(10);
        mappingToolbar.setPadding(new Insets(10));
        mappingToolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        mappingToolbar.getStyleClass().add("mapping-toolbar");

        cmbMapFormat = new ComboBox<>();
        cmbMapFormat.getItems().addAll("XML + XSD", "JSON + Schema", "YAML + Schema", "ISO 20022 MX", "CSV + CSVW", "Flat File");
        cmbMapFormat.setValue("XML + XSD");

        Button btnMapLink = new Button("Link Selected", new FontIcon("fas-link"));
        btnMapLink.getStyleClass().addAll("editor-btn", "btn-link-schema");

        Button btnMapUnlink = new Button("Unlink Message", new FontIcon("fas-unlink"));
        btnMapUnlink.getStyleClass().addAll("editor-btn", "btn-delete");

        mappingToolbar.getChildren().addAll(
            new Label("Validation Format:"), cmbMapFormat, btnMapLink, btnMapUnlink
        );
        editorPane.setTop(mappingToolbar);

        SplitPane treesSplit = new SplitPane();
        treesSplit.setDividerPositions(0.5);

        VBox msgBox = new VBox(5);
        msgBox.setPadding(new Insets(10));
        Label lblMsg = new Label("Message Files (Sources)");
        lblMsg.setStyle("-fx-font-weight: bold;");
        treeMapMessage = new TreeView<>();
        VBox.setVgrow(treeMapMessage, Priority.ALWAYS);
        msgBox.getChildren().addAll(lblMsg, treeMapMessage);

        VBox schemaBox = new VBox(5);
        schemaBox.setPadding(new Insets(10));
        Label lblSchema = new Label("Schema Files (Targets)");
        lblSchema.setStyle("-fx-font-weight: bold;");
        treeMapSchema = new TreeView<>();
        VBox.setVgrow(treeMapSchema, Priority.ALWAYS);
        schemaBox.getChildren().addAll(lblSchema, treeMapSchema);

        treesSplit.getItems().addAll(msgBox, schemaBox);
        editorPane.setCenter(treesSplit);

        VBox mapBox = new VBox(10);
        mapBox.setPadding(new Insets(10));
        Label lblMapTitle = new Label("Live Relationships Map");
        lblMapTitle.setStyle("-fx-font-weight: bold;");
        
        webViewMappingMap = new WebView();
        VBox.setVgrow(webViewMappingMap, Priority.ALWAYS);
        
        mapBox.getChildren().addAll(lblMapTitle, webViewMappingMap);

        mainSplit.getItems().addAll(editorPane, mapBox);
        tab.setContent(mainSplit);

        setupDragAndDropMapping();

        btnMapLink.setOnAction(e -> {
            TreeItem<File> msgItem = treeMapMessage.getSelectionModel().getSelectedItem();
            TreeItem<File> schemaItem = treeMapSchema.getSelectionModel().getSelectedItem();
            if (msgItem == null || msgItem.getValue().isDirectory()) {
                showError("Selection Required", "Please select a Message file in the left explorer.");
                return;
            }
            if (schemaItem == null || schemaItem.getValue().isDirectory()) {
                showError("Selection Required", "Please select a Schema file in the right explorer.");
                return;
            }
            saveMapping(msgItem.getValue(), schemaItem.getValue(), cmbMapFormat.getValue());
            refreshMappingDiagram();
            
            Tab active = tabPane.getSelectionModel().getSelectedItem();
            if (active != null) {
                updateToolbarForTab(active);
            }
        });

        btnMapUnlink.setOnAction(e -> {
            TreeItem<File> msgItem = treeMapMessage.getSelectionModel().getSelectedItem();
            if (msgItem == null || msgItem.getValue().isDirectory()) {
                showError("Selection Required", "Please select a Message file in the left explorer to unlink.");
                return;
            }
            saveMapping(msgItem.getValue(), null, null);
            refreshMappingDiagram();
            
            Tab active = tabPane.getSelectionModel().getSelectedItem();
            if (active != null) {
                updateToolbarForTab(active);
            }
        });

        refreshMappingTrees();

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        tab.setOnClosed(e -> {
            webViewMappingMap = null;
            treeMapMessage = null;
            treeMapSchema = null;
            cmbMapFormat = null;
        });

        webViewMappingMap.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                refreshMappingDiagram();
            }
        });
        webViewMappingMap.getEngine().loadContent(generateMappingBaseHtml(mermaidScriptTag, currentThemeName));
    }

    private void refreshMappingTrees() {
        if (treeMapMessage == null || treeMapSchema == null) return;

        TreeItem<File> rootMsg = new TreeItem<>(workspaceRoot);
        populateTreeFiltered(workspaceRoot, rootMsg, false, null);
        treeMapMessage.setRoot(rootMsg);
        treeMapMessage.setShowRoot(false);

        TreeItem<File> rootSchema = new TreeItem<>(workspaceRoot);
        populateTreeFiltered(workspaceRoot, rootSchema, true, null);
        treeMapSchema.setRoot(rootSchema);
        treeMapSchema.setShowRoot(false);
    }

    private void populateTreeFiltered(File file, TreeItem<File> parent, boolean isSchemaFilter, String filterText) {
        if (!file.exists()) return;
        boolean matchesFilterText = filterText == null || filterText.isEmpty() || 
                                    file.getName().toLowerCase().contains(filterText.toLowerCase());
        
        if (file.isDirectory()) {
            String path = file.getAbsolutePath().replace('\\', '/');
            if (isSchemaFilter && path.contains("/messages")) return;
            if (!isSchemaFilter && path.contains("/schemas")) return;
            
            TreeItem<File> dirItem = new TreeItem<>(file);
            dirItem.setExpanded(true);
            
            File[] children = file.listFiles();
            if (children != null) {
                Arrays.sort(children, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });
                for (File child : children) {
                    populateTreeFiltered(child, dirItem, isSchemaFilter, filterText);
                }
            }
            if (!dirItem.getChildren().isEmpty() || matchesFilterText) {
                parent.getChildren().add(dirItem);
            }
        } else {
            String name = file.getName().toLowerCase();
            boolean isSchemaFile = name.endsWith(".xsd") || name.endsWith(".json") || name.endsWith(".csvw");
            boolean isMessageFile = name.endsWith(".xml") || name.endsWith(".json") || name.endsWith(".yaml") || 
                                    name.endsWith(".csv") || name.endsWith(".txt");
            
            if (isSchemaFilter && !isSchemaFile) return;
            if (!isSchemaFilter && !isMessageFile) return;
            
            if (matchesFilterText) {
                parent.getChildren().add(new TreeItem<>(file));
            }
        }
    }

    private void setupDragAndDropMapping() {
        if (treeMapMessage == null || treeMapSchema == null) return;

        treeMapMessage.setCellFactory(tv -> {
            TreeCell<File> cell = new TreeCell<>() {
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
                            String ext = getFileExtension(item);
                            if ("xml".equals(ext)) setGraphic(new FontIcon("fas-file-code"));
                            else if ("json".equals(ext)) setGraphic(new FontIcon("fas-file-alt"));
                            else if ("yaml".equals(ext) || "yml".equals(ext)) setGraphic(new FontIcon("fas-file-signature"));
                            else setGraphic(new FontIcon("fas-file"));
                        }
                    }
                }
            };

            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty() && cell.getItem().isFile()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(cell.getItem().getAbsolutePath());
                    db.setContent(content);
                    event.consume();
                }
            });

            return cell;
        });

        treeMapSchema.setCellFactory(tv -> {
            TreeCell<File> cell = new TreeCell<>() {
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
                            setGraphic(new FontIcon("fas-file-signature"));
                        }
                    }
                }
            };

            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    File targetFile = cell.getItem();
                    if (targetFile != null && targetFile.isFile()) {
                        event.acceptTransferModes(TransferMode.COPY);
                    }
                }
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    File sourceFile = new File(db.getString());
                    File targetFile = cell.getItem();
                    if (targetFile != null && targetFile.isFile()) {
                        String type = "XML + XSD";
                        String targetPath = targetFile.getAbsolutePath().replace('\\', '/');
                        if (targetPath.endsWith(".xsd")) {
                            if (targetPath.contains("iso20022")) {
                                type = "ISO 20022 MX";
                            } else {
                                type = "XML + XSD";
                            }
                        } else if (targetPath.endsWith(".json") || targetPath.endsWith(".csvw")) {
                            if (targetPath.contains("csv")) {
                                type = "CSV + CSVW";
                            } else if (targetPath.contains("flatfile")) {
                                type = "Flat File";
                            } else {
                                String srcExt = getFileExtension(sourceFile);
                                if ("yaml".equals(srcExt) || "yml".equals(srcExt)) {
                                    type = "YAML + Schema";
                                } else {
                                    type = "JSON + Schema";
                                }
                            }
                        }

                        saveMapping(sourceFile, targetFile, type);
                        refreshMappingDiagram();

                        Tab active = tabPane.getSelectionModel().getSelectedItem();
                        if (active != null) {
                            updateToolbarForTab(active);
                        }

                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return cell;
        });
    }

    private void refreshMappingDiagram() {
        if (webViewMappingMap == null) return;
        String code = generateMermaidMappingCode();
        try {
            String escaped = code.replace("\\", "\\\\").replace("`", "\\`").replace("\n", "\\n").replace("'", "\\'");
            webViewMappingMap.getEngine().executeScript("window.updateDiagram('" + escaped + "')");
        } catch (Exception e) {
            System.err.println("Failed to execute updateDiagram script: " + e.getMessage());
        }
    }

    private String generateMermaidMappingCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("graph LR\n");
        
        File mappingFile = new File(workspaceRoot, "validation-mapping.json");
        if (!mappingFile.exists()) {
            sb.append("  subgraph No Mappings\n  hint[\"No mappings defined yet.\"]\n  end\n");
            return sb.toString();
        }
        
        try {
            String content = Files.readString(mappingFile.toPath());
            JSONObject json = new JSONObject(content);
            JSONArray arr = json.optJSONArray("mappings");
            if (arr == null || arr.length() == 0) {
                sb.append("  subgraph No Mappings\n  hint[\"No mappings defined yet.\"]\n  end\n");
                return sb.toString();
            }
            
            sb.append("  classDef msg fill:#3a5a7c,stroke:#5c8dbf,stroke-width:2px,color:#fff;\n");
            sb.append("  classDef schema fill:#2d6a4f,stroke:#52b788,stroke-width:2px,color:#fff;\n");
            
            Map<String, String> msgIds = new HashMap<>();
            Map<String, String> schemaIds = new HashMap<>();
            
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String msgPath = obj.optString("messagePath");
                String schemaPath = obj.optString("schemaPath");
                String type = obj.optString("type");
                
                String mId = msgIds.computeIfAbsent(msgPath, k -> "M" + (msgIds.size()));
                String sId = schemaIds.computeIfAbsent(schemaPath, k -> "S" + (schemaIds.size()));
                
                sb.append("  ").append(mId).append("[\"").append(msgPath).append("\"]:::msg\n");
                sb.append("  ").append(sId).append("[\"").append(schemaPath).append("\"]:::schema\n");
                sb.append("  ").append(mId).append(" -->|").append(type).append("| ").append(sId).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sb.append("  error[\"Error reading mappings: ").append(e.getMessage()).append("\"]\n");
        }
        
        return sb.toString();
    }

    private String generateMappingBaseHtml(String scriptTag, String currentTheme) {
        String bgColor = "#1e1e1e";
        String fgColor = "white";
        String mermaidTheme = "dark";
        String background = "#1e1e1e";
        String primaryColor = "#1e1e1e";
        String primaryTextColor = "#d4d4d4";
        String primaryBorderColor = "#3f3f46";
        String lineColor = "#52525b";
        String secondaryColor = "#27272a";
        String tertiaryColor = "#27272a";

        if ("IntelliJ Light".equalsIgnoreCase(currentTheme)) {
            bgColor = "#ffffff";
            fgColor = "#333333";
            mermaidTheme = "default";
            background = "#ffffff";
            primaryColor = "#e2e8f0";
            primaryTextColor = "#1e293b";
            primaryBorderColor = "#cbd5e1";
            lineColor = "#94a3b8";
            secondaryColor = "#f1f5f9";
            tertiaryColor = "#f1f5f9";
        } else if ("Dracula".equalsIgnoreCase(currentTheme)) {
            bgColor = "#282a36";
            fgColor = "#f8f8f2";
            mermaidTheme = "dark";
            background = "#282a36";
            primaryColor = "#44475a";
            primaryTextColor = "#f8f8f2";
            primaryBorderColor = "#6272a4";
            lineColor = "#6272a4";
            secondaryColor = "#282a36";
            tertiaryColor = "#282a36";
        } else if ("Monokai".equalsIgnoreCase(currentTheme)) {
            bgColor = "#272822";
            fgColor = "#f8f8f2";
            mermaidTheme = "dark";
            background = "#272822";
            primaryColor = "#3e3d32";
            primaryTextColor = "#f8f8f2";
            primaryBorderColor = "#75715e";
            lineColor = "#75715e";
            secondaryColor = "#272822";
            tertiaryColor = "#272822";
        } else if ("Hacker".equalsIgnoreCase(currentTheme)) {
            bgColor = "#050505";
            fgColor = "#00ff00";
            mermaidTheme = "dark";
            background = "#050505";
            primaryColor = "#001d00";
            primaryTextColor = "#00ff00";
            primaryBorderColor = "#00ff00";
            lineColor = "#00ff00";
            secondaryColor = "#000000";
            tertiaryColor = "#000000";
        }

        return "<html>" +
                "<head>" +
                scriptTag +
                "<style>" +
                "  body { background-color: " + bgColor + "; color: " + fgColor + "; margin: 0; padding: 20px; overflow: auto; font-family: 'Segoe UI', sans-serif; }" +
                "  .mermaid { display: flex; justify-content: center; transition: opacity 0.3s; }" +
                "  .status { position: fixed; top: 10px; right: 10px; font-size: 10px; color: #888; }" +
                "  #error-box { color: #f44336; padding: 10px; border: 1px solid #f44336; border-radius: 4px; display: none; font-size: 12px; margin-top: 20px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div id=\"status\" class=\"status\">Ready</div>" +
                "<div id=\"error-box\"></div>" +
                "<div id=\"diagram-container\" class=\"mermaid\"></div>" +
                "<script>" +
                "  window.updateDiagram = function(code) {" +
                "    const container = document.getElementById('diagram-container');" +
                "    const errorBox = document.getElementById('error-box');" +
                "    const status = document.getElementById('status');" +
                "    errorBox.style.display = 'none';" +
                "    status.innerHTML = 'Rendering...';" +
                "    if (typeof mermaid === 'undefined') {" +
                "       setTimeout(() => window.updateDiagram(code), 100);" +
                "       return;" +
                "    }" +
                "    try {" +
                "      mermaid.initialize({" +
                "        startOnLoad: false," +
                "        theme: '" + mermaidTheme + "'," +
                "        securityLevel: 'loose'," +
                "        flowchart: { useMaxWidth: false }," +
                "        themeVariables: {" +
                "          background: '" + background + "'," +
                "          primaryColor: '" + primaryColor + "'," +
                "          primaryTextColor: '" + primaryTextColor + "'," +
                "          primaryBorderColor: '" + primaryBorderColor + "'," +
                "          lineColor: '" + lineColor + "'," +
                "          secondaryColor: '" + secondaryColor + "'," +
                "          tertiaryColor: '" + tertiaryColor + "'" +
                "        }" +
                "      });" +
                "      mermaid.render('graphDiv', code).then(({svg}) => {" +
                "        container.innerHTML = svg;" +
                "        status.innerHTML = 'Done';" +
                "      }).catch(err => {" +
                "        errorBox.innerHTML = 'Mermaid Error: ' + err.message;" +
                "        errorBox.style.display = 'block';" +
                "        status.innerHTML = 'Error';" +
                "      });" +
                "    } catch(e) {" +
                "      errorBox.innerHTML = 'Critical Error: ' + e.message;" +
                "      errorBox.style.display = 'block';" +
                "    }" +
                "  };" +
                "</script>" +
                "</body>" +
                "</html>";
    }
}
