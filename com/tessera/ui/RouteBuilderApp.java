package com.tessera.ui;

import com.tessera.ui.components.ThemeManager;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class RouteBuilderApp extends Application {

    public static FontIcon getFileIcon(java.io.File file) {
        if (file == null) return new FontIcon("fas-file");
        if (file.isDirectory()) return new FontIcon("fas-folder");
        
        String name = file.getName().toLowerCase();
        if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            try {
                // Peek first 4KB for performance
                String content = "";
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                    long len = Math.min(raf.length(), 4096);
                    byte[] bytes = new byte[(int)len];
                    raf.readFully(bytes);
                    content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).toLowerCase();
                }
                
                if (content.contains("camel") || content.contains("route:") || content.contains("from:")) {
                    FontIcon icon = new FontIcon("fas-route");
                    icon.setIconColor(javafx.scene.paint.Color.web("#FF9800"));
                    return icon;
                }
                if (content.contains("kafka")) {
                    FontIcon icon = new FontIcon("fas-server");
                    icon.setIconColor(javafx.scene.paint.Color.web("#E91E63"));
                    return icon;
                }
                if (content.contains("mongodb")) {
                    FontIcon icon = new FontIcon("fas-leaf");
                    icon.setIconColor(javafx.scene.paint.Color.web("#4CAF50"));
                    return icon;
                }
                if (content.contains("kamelet")) {
                    FontIcon icon = new FontIcon("fas-plug");
                    icon.setIconColor(javafx.scene.paint.Color.web("#2196F3"));
                    return icon;
                }
                if (content.contains("{{type:")) {
                    FontIcon icon = new FontIcon("fas-magic");
                    icon.setIconColor(javafx.scene.paint.Color.web("#E040FB"));
                    return icon;
                }
            } catch (Exception ignored) {}
            return new FontIcon("fas-file-signature");
        }
        
        if (name.endsWith(".xml") || name.endsWith(".template") || name.endsWith(".txt")) {
            try {
                String content = "";
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                    long len = Math.min(raf.length(), 2048);
                    byte[] bytes = new byte[(int)len];
                    raf.readFully(bytes);
                    content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).toLowerCase();
                }
                if (content.contains("{{type:")) {
                    FontIcon icon = new FontIcon("fas-magic");
                    icon.setIconColor(javafx.scene.paint.Color.web("#E040FB"));
                    return icon;
                }
            } catch (Exception ignored) {}
        }
        
        if (name.endsWith(".xml")) {
            FontIcon icon = new FontIcon("fas-file-code");
            icon.setIconColor(javafx.scene.paint.Color.web("#569cd6"));
            return icon;
        }
        if (name.endsWith(".json")) {
            FontIcon icon = new FontIcon("fas-file-alt");
            icon.setIconColor(javafx.scene.paint.Color.web("#ce9178"));
            return icon;
        }
        if (name.endsWith(".java")) {
            FontIcon icon = new FontIcon("fas-coffee");
            icon.setIconColor(javafx.scene.paint.Color.web("#f89820"));
            return icon;
        }
        if (name.endsWith(".mmd") || name.endsWith(".mermaid")) {
            FontIcon icon = new FontIcon("fas-project-diagram");
            icon.setIconColor(javafx.scene.paint.Color.web("#E040FB"));
            return icon;
        }
        if (name.endsWith(".puml") || name.endsWith(".plantuml")) {
            FontIcon icon = new FontIcon("fas-sitemap");
            icon.setIconColor(javafx.scene.paint.Color.web("#00BCD4"));
            return icon;
        }
        if (name.endsWith(".dot") || name.endsWith(".gv")) {
            FontIcon icon = new FontIcon("fas-network-wired");
            icon.setIconColor(javafx.scene.paint.Color.web("#9C27B0"));
            return icon;
        }
        if (name.endsWith(".csv")) return new FontIcon("fas-file-csv");
        if (name.endsWith(".bpmn")) return new FontIcon("fas-project-diagram");
        
        return new FontIcon("fas-file");
    }

    public YamlEditorPane editorPane;
    private DiagramPane diagramPane;
    private RouteTreePane treePane;
    private com.tessera.lsp.LspManager lspManager;
    private com.tessera.ui.components.ConsolePane consolePane;
    private HelpPortalPane helpPortalPane;
    private javafx.scene.control.Button btnPlay;
    private javafx.scene.control.Button btnStop;
    private final Process[] runnerProcess = {null};

    public static String currentThemeClass = ThemeManager.getCurrentThemeClass();
    public static String currentThemeName = ThemeManager.getCurrentThemeName();
    public static String currentDynamicCssUri = ThemeManager.getCurrentDynamicCssUri();
    public static final java.util.Set<javafx.scene.Parent> themedRoots = new java.util.HashSet<>();
    public static RouteBuilderApp instance;

    public static RouteBuilderApp getInstance() {
        return instance;
    }
    public static javafx.scene.layout.BorderPane rootNode;
    public static javafx.scene.control.ComboBox<String> globalThemeBox;

    public static void setGlobalTheme(String theme) {
        ThemeManager.applyTheme(theme);
        currentThemeName = ThemeManager.getCurrentThemeName();
        currentThemeClass = ThemeManager.getCurrentThemeClass();
        currentDynamicCssUri = null; 
        if (instance != null) instance.updateInternalThemes(theme);
    }

    private void updateInternalThemes(String theme) {
        if (diagramPane != null) diagramPane.setTheme(theme);
        if (helpPortalPane != null) helpPortalPane.setTheme(theme);
        // editorPane and others update via ThemeManager listeners or registered roots
    }

    public static void themeDialog(javafx.scene.control.Dialog<?> dialog) {
        com.tessera.ui.components.ThemeManager.registerRoot(dialog.getDialogPane());
    }

    public void showConsole(Process process, String title) {
        javafx.application.Platform.runLater(() -> {
            if (consolePane != null) {
                consolePane.clear();
                consolePane.log("\033[1;36m╔══ " + title + " ══╗\033[0m\n");
            }
        });

        // Pipe combined stdout/stderr — read raw bytes so \r and mid-line ANSI sequences are preserved
        new Thread(() -> pipeStream(process, process.getInputStream()), "console-combined-stream").start();
    }

    private void pipeStream(Process process, java.io.InputStream stream) {
        byte[] buf = new byte[2048];
        int n;
        try {
            while ((n = stream.read(buf)) != -1) {
                // Decode using the platform charset (UTF-8 for Camel Main / JBang)
                final String chunk = new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8);
                if (consolePane != null) consolePane.log(chunk);
            }
        } catch (Exception ignored) {
        } finally {
            if (process != null && !process.isAlive()) {
                try {
                    int exitCode = process.exitValue();
                    if (consolePane != null) {
                        if (exitCode == 143 || exitCode == 130) {
                            consolePane.log("\n\033[1;32m[Route Builder Studio] Process stopped by user.\033[0m\n");
                        } else {
                            consolePane.log("\n\033[1;31m[Route Builder Studio] Process exited with code " + exitCode + "\033[0m\n");
                        }
                    }
                } catch (Exception ignored) {}
                javafx.application.Platform.runLater(() -> {
                    if (btnStop != null) btnStop.setDisable(true);
                    if (editorPane != null && editorPane.getBtnStopFile() != null) {
                        editorPane.getBtnStopFile().setDisable(true);
                    }
                    if (btnPlay != null && treePane != null) {
                        boolean hasChecked = !treePane.getCheckedFiles().isEmpty();
                        boolean hasSelected = treePane.getTreeView().getSelectionModel().getSelectedItem() != null;
                        btnPlay.setDisable(!hasChecked && !hasSelected);
                    }
                });
            }
        }
    }

    public void stopCurrentProcess() {
        if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
            runnerProcess[0].destroy();
            runnerProcess[0].descendants().forEach(ProcessHandle::destroyForcibly);
            runnerProcess[0] = null;
        }
        if (btnStop != null) {
            btnStop.setDisable(true);
        }
    }

    public void setRunnerProcess(Process p) {
        this.runnerProcess[0] = p;
        if (btnStop != null) {
            btnStop.setDisable(false);
        }
    }

    private void showManual() {
        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        
        try {
            java.io.File manualFile = new java.io.File(System.getProperty("user.dir"), "User Manual.md");
            String mdContent = "";
            if (manualFile.exists()) {
                mdContent = java.nio.file.Files.readString(manualFile.toPath());
            } else {
                mdContent = "# Error\nManual not found at: " + manualFile.getAbsolutePath();
            }
            
            org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder().build();
            org.commonmark.node.Node document = parser.parse(mdContent);
            org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder().build();
            String htmlContent = renderer.render(document);

            String fullHtml = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <style>\n" +
                "    body { font-family: 'Segoe UI', Helvetica, Arial, sans-serif; padding: 20px; background-color: #1e1e1e; color: #cccccc; }\n" +
                "    a { color: #569cd6; text-decoration: none; }\n" +
                "    a:hover { text-decoration: underline; }\n" +
                "    pre { background-color: #2d2d2d; padding: 10px; border-radius: 5px; overflow-x: auto; border: 1px solid #444; }\n" +
                "    code { font-family: Consolas, 'Courier New', monospace; background-color: #2d2d2d; padding: 2px 4px; border-radius: 3px; }\n" +
                "    blockquote { border-left: 4px solid #007acc; margin: 0; padding-left: 10px; color: #aaa; }\n" +
                "    h1, h2, h3 { border-bottom: 1px solid #444; padding-bottom: 5px; color: #fff; }\n" +
                "    table { border-collapse: collapse; width: 100%; }\n" +
                "    th, td { border: 1px solid #444; padding: 8px; text-align: left; }\n" +
                "    th { background-color: #2d2d2d; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                htmlContent +
                "</body>\n" +
                "</html>";
                
            webView.getEngine().loadContent(fullHtml);
        } catch (Exception e) {
            webView.getEngine().loadContent("<html><body style='color:red'>Error reading manual: " + e.getMessage() + "</body></html>");
        }

        javafx.scene.Scene scene = new javafx.scene.Scene(webView, 800, 600);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("User Manual");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void start(Stage primaryStage) {
        loadWorkspaceProperties();
        lspManager = new com.tessera.lsp.LspManager();
        lspManager.start();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        instance = this;
        rootNode = root;

        // Top Menu Bar
        javafx.scene.control.MenuBar menuBar = new javafx.scene.control.MenuBar();
        javafx.scene.control.Menu fileMenu = new javafx.scene.control.Menu("_File");

        javafx.scene.control.MenuItem newSampleProjectItem = new javafx.scene.control.MenuItem("Sample Camel Project");
        
        javafx.scene.control.Menu newMenu = new javafx.scene.control.Menu("New...");
        javafx.scene.control.MenuItem newProjectItem = new javafx.scene.control.MenuItem("Project (Workspace)");
        javafx.scene.control.MenuItem newFileItem = new javafx.scene.control.MenuItem("Empty YAML File");
        javafx.scene.control.MenuItem newKameletItem = new javafx.scene.control.MenuItem("Kamelet Definition");
        javafx.scene.control.MenuItem newComponentItem = new javafx.scene.control.MenuItem("Camel Component (Java)");
        javafx.scene.control.MenuItem newProcessorItem = new javafx.scene.control.MenuItem("Processor (Java)");
        javafx.scene.control.MenuItem newJavaDslItem = new javafx.scene.control.MenuItem("Java DSL Route");
        javafx.scene.control.MenuItem newXmlDslItem = new javafx.scene.control.MenuItem("XML DSL Route");
        javafx.scene.control.MenuItem newYamlDslItem = new javafx.scene.control.MenuItem("YAML DSL Route");
        javafx.scene.control.MenuItem newGroovyDslItem = new javafx.scene.control.MenuItem("Groovy DSL Route");
        javafx.scene.control.MenuItem newKotlinDslItem = new javafx.scene.control.MenuItem("Kotlin DSL Route");
        
        javafx.scene.control.Menu newTransformMenu = new javafx.scene.control.Menu("Transformations");
        javafx.scene.control.MenuItem newXsltItem = new javafx.scene.control.MenuItem("XSLT Template");
        javafx.scene.control.MenuItem newJsltItem = new javafx.scene.control.MenuItem("JSLT Template");
        javafx.scene.control.MenuItem newFtlItem = new javafx.scene.control.MenuItem("FreeMarker (FTL)");
        newTransformMenu.getItems().addAll(newXsltItem, newJsltItem, newFtlItem);

        newMenu.getItems().addAll(newProjectItem, newSampleProjectItem, newFileItem, new javafx.scene.control.SeparatorMenuItem(), 
                                  newKameletItem, newComponentItem, newProcessorItem, new javafx.scene.control.SeparatorMenuItem(), 
                                  newYamlDslItem, newJavaDslItem, newXmlDslItem, newGroovyDslItem, newKotlinDslItem, newTransformMenu);

        javafx.scene.control.MenuItem openItem = new javafx.scene.control.MenuItem("Open Folder...");
        javafx.scene.control.Menu recentProjectsMenu = new javafx.scene.control.Menu("Recent Projects");
        javafx.scene.control.MenuItem exitItem = new javafx.scene.control.MenuItem("Exit");
        exitItem.setOnAction(e -> javafx.application.Platform.exit());
        fileMenu.getItems().addAll(newMenu, openItem, recentProjectsMenu, new javafx.scene.control.SeparatorMenuItem(), exitItem);
        
        javafx.scene.control.Menu editMenu = new javafx.scene.control.Menu("_Edit");
        javafx.scene.control.MenuItem undoItem = new javafx.scene.control.MenuItem("Undo");
        javafx.scene.control.MenuItem redoItem = new javafx.scene.control.MenuItem("Redo");
        javafx.scene.control.MenuItem cutItem = new javafx.scene.control.MenuItem("Cut");
        javafx.scene.control.MenuItem copyItem = new javafx.scene.control.MenuItem("Copy");
        javafx.scene.control.MenuItem pasteItem = new javafx.scene.control.MenuItem("Paste");
        javafx.scene.control.MenuItem selectAllItem = new javafx.scene.control.MenuItem("Select All");

        undoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Z, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        redoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Z, javafx.scene.input.KeyCombination.CONTROL_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        cutItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.X, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        copyItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.C, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        pasteItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.V, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        selectAllItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.A, javafx.scene.input.KeyCombination.CONTROL_DOWN));

        javafx.scene.control.MenuItem cryptoItem = new javafx.scene.control.MenuItem("Crypto Studio...");
        cryptoItem.setOnAction(e -> CryptoStudioWindow.show());

        editMenu.getItems().addAll(undoItem, redoItem, new javafx.scene.control.SeparatorMenuItem(), cutItem, copyItem, pasteItem, new javafx.scene.control.SeparatorMenuItem(), cryptoItem, selectAllItem);

        javafx.scene.control.Menu viewMenu = new javafx.scene.control.Menu("_View");
        javafx.scene.control.CheckMenuItem viewExplorerItem = new javafx.scene.control.CheckMenuItem("Project Explorer");
        viewExplorerItem.setSelected(true);
        javafx.scene.control.CheckMenuItem viewCodeItem = new javafx.scene.control.CheckMenuItem("Code Editor");
        viewCodeItem.setSelected(true);
        javafx.scene.control.CheckMenuItem viewDiagramItem = new javafx.scene.control.CheckMenuItem("Diagram Canvas");
        viewDiagramItem.setSelected(true);
        javafx.scene.control.CheckMenuItem viewHelpItem = new javafx.scene.control.CheckMenuItem("Interactive Help Portal");
        viewHelpItem.setSelected(false);
        javafx.scene.control.MenuItem resetLayoutItem = new javafx.scene.control.MenuItem("Reset Layout");
        javafx.scene.control.MenuItem swapLayoutItem = new javafx.scene.control.MenuItem("Swap Code and Diagram");
        viewMenu.getItems().addAll(viewExplorerItem, viewCodeItem, viewDiagramItem, viewHelpItem, new javafx.scene.control.SeparatorMenuItem(), swapLayoutItem, resetLayoutItem);
        
        javafx.scene.control.Menu helpMenu = new javafx.scene.control.Menu("_Help");
        javafx.scene.control.MenuItem maxItem = new javafx.scene.control.MenuItem("Maximize Window");
        maxItem.setOnAction(e -> primaryStage.setMaximized(true));
        javafx.scene.control.MenuItem restoreItem = new javafx.scene.control.MenuItem("Restore Window");
        restoreItem.setOnAction(e -> primaryStage.setMaximized(false));
        
        javafx.scene.control.TextField searchBox = new javafx.scene.control.TextField();
        searchBox.setPromptText("Search Manual...");
        javafx.scene.control.CustomMenuItem searchItem = new javafx.scene.control.CustomMenuItem(searchBox, false);
        
        javafx.scene.control.MenuItem helpGuideItem = new javafx.scene.control.MenuItem("Open Help Guide...", new org.kordamp.ikonli.javafx.FontIcon("fas-question-circle"));
        helpGuideItem.setOnAction(e -> new RouteBuilderHelpWindow().show());

        javafx.scene.control.MenuItem manualItem = new javafx.scene.control.MenuItem("Open User Manual");
        manualItem.setOnAction(e -> showManual());

        javafx.scene.control.MenuItem interactiveHelpItem = new javafx.scene.control.MenuItem("Open Help Portal");
        interactiveHelpItem.setOnAction(e -> viewHelpItem.setSelected(true));

        javafx.scene.control.MenuItem aboutItem = new javafx.scene.control.MenuItem("About Tessera...", new org.kordamp.ikonli.javafx.FontIcon("fas-info-circle"));
        aboutItem.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().addAll(maxItem, restoreItem, new javafx.scene.control.SeparatorMenuItem(), searchItem, helpGuideItem, manualItem, interactiveHelpItem, new javafx.scene.control.SeparatorMenuItem(), aboutItem);
        
        javafx.scene.control.Menu toolsMenu = new javafx.scene.control.Menu("_Tools");
        
        javafx.scene.control.MenuItem variablesItem = new javafx.scene.control.MenuItem("Variables Editor...");
        variablesItem.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            if (baseDir != null) {
                VariablesEditorWindow.show(baseDir, null);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        javafx.scene.control.MenuItem toolsCryptoItem = new javafx.scene.control.MenuItem("Crypto Studio...");
        toolsCryptoItem.setOnAction(e -> CryptoStudioWindow.show());

        javafx.scene.control.MenuItem transformItem = new javafx.scene.control.MenuItem("Data Transformation Studio...");
        transformItem.setOnAction(e -> {
            TransformationStudioWindow studio = new TransformationStudioWindow();
            studio.show();
        });

        javafx.scene.control.MenuItem validateItem = new javafx.scene.control.MenuItem("Universal Validator Studio...");
        validateItem.setOnAction(e -> {
            ValidatorStudioWindow validatorStudio = new ValidatorStudioWindow();
            validatorStudio.show();
        });

        javafx.scene.control.MenuItem diagramItem = new javafx.scene.control.MenuItem("Universal Diagram Studio...");
        diagramItem.setOnAction(e -> {
            java.io.File workspaceRoot = getWorkspaceRoot();
            DiagramStudioWindow diagramStudio = new DiagramStudioWindow(workspaceRoot);
            diagramStudio.show();
        });

        javafx.scene.control.MenuItem fakerItem = new javafx.scene.control.MenuItem("Faker & Template Studio...");
        fakerItem.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            FakerStudioWindow fakerStudio = new FakerStudioWindow(baseDir);
            fakerStudio.show();
        });

        javafx.scene.control.MenuItem kameletBuilderItem = new javafx.scene.control.MenuItem("Kamelet Builder...");
        kameletBuilderItem.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            if (baseDir != null) {
                KameletStudioWindow.show(baseDir);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        javafx.scene.control.MenuItem dependencyCatalogItem = new javafx.scene.control.MenuItem("Dependency Catalog...");
        dependencyCatalogItem.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            if (baseDir != null) {
                DependencyCatalogWindow.show(baseDir);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        javafx.scene.control.MenuItem exportItem = new javafx.scene.control.MenuItem("Export to Liquibase...");
        exportItem.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (checked.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please select one or more routes using the checkboxes in the Explorer.");
                themeDialog(alert);
                alert.showAndWait();
                return;
            }
            LiquibaseExportWindow.showForRoutes(getWorkspaceRoot(), checked);
        });

        javafx.scene.control.MenuItem remoteDeployItem = new javafx.scene.control.MenuItem("Deploy Remotely...");
        remoteDeployItem.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (checked.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please select one or more routes using the checkboxes in the Explorer.");
                themeDialog(alert);
                alert.showAndWait();
                return;
            }
            RemoteDeployWindow.showForRoutes(getWorkspaceRoot(), checked);
        });

        toolsMenu.getItems().addAll(
            variablesItem,
            toolsCryptoItem,
            transformItem,
            validateItem,
            diagramItem,
            fakerItem,
            new javafx.scene.control.SeparatorMenuItem(),
            kameletBuilderItem,
            dependencyCatalogItem,
            new javafx.scene.control.SeparatorMenuItem(),
            remoteDeployItem,
            exportItem
        );

        javafx.scene.control.Menu themeMenu = new javafx.scene.control.Menu("T_heme");
        javafx.scene.control.ToggleGroup themeGroup = new javafx.scene.control.ToggleGroup();
        String savedTheme = java.util.prefs.Preferences.userNodeForPackage(RouteBuilderApp.class).get("themeName", "VSCode Dark");
        for (String themeName : com.tessera.ui.components.ThemeManager.getAvailableThemes().keySet()) {
            javafx.scene.control.RadioMenuItem themeItem = new javafx.scene.control.RadioMenuItem(themeName);
            themeItem.setToggleGroup(themeGroup);
            if (themeName.equals(savedTheme)) {
                themeItem.setSelected(true);
            }
            themeItem.setOnAction(e -> setGlobalTheme(themeName));
            themeMenu.getItems().add(themeItem);
        }
        
        com.tessera.ui.components.ThemeManager.addListener(newTheme -> {
            for (javafx.scene.control.MenuItem item : themeMenu.getItems()) {
                if (item instanceof javafx.scene.control.RadioMenuItem radioItem) {
                    if (radioItem.getText().equals(newTheme)) {
                        radioItem.setSelected(true);
                    }
                }
            }
        });

        menuBar.getMenus().addAll(fileMenu, editMenu, toolsMenu, viewMenu, themeMenu, helpMenu);
        
        javafx.scene.control.ToolBar toolBar = new javafx.scene.control.ToolBar();
        
        // Brand Logo Button
        javafx.scene.control.Button btnLogo = new javafx.scene.control.Button();
        btnLogo.getStyleClass().add("toolbar-logo-btn");
        btnLogo.setPickOnBounds(true);
        javafx.scene.web.WebView logoIcon = new javafx.scene.web.WebView();
        logoIcon.setPrefSize(28, 28);
        logoIcon.setMinSize(28, 28);
        logoIcon.setMaxSize(28, 28);
        String iconSvg = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600">
              <g transform="translate(400, 300) scale(2.2)">
                <g transform="rotate(45)" stroke-linejoin="round">
                  <path d="M 100 100 L 4 100 L 4 62 L -2 62 C -6 72, -20 68, -20 50 C -20 32, -6 28, -2 38 L 4 38 L 4 4 L 38 4 L 38 -2 C 28 -6, 32 -20, 50 -20 C 68 -20, 72 -6, 62 -2 L 62 4 L 100 4 Z" fill="#0F4A56" />
                  <path d="M -100 100 L -100 4 L -62 4 L -62 -2 C -72 -6, -68 -20, -50 -20 C -32 -20, -28 -6, -38 -2 L -38 4 L -4 4 L -4 38 L -10 38 C -14 28, -28 32, -28 50 C -28 68, -14 72, -10 62 L -4 62 L -4 100 Z" fill="#156574" />
                  <path d="M 100 -100 L 100 -4 L 62 -4 L 62 -10 C 72 -14, 68 -28, 50 -28 C 32 -28, 28 -14, 38 -10 L 38 -4 L 4 -4 L 4 -38 L -2 -38 C -6 -28, -20 -32, -20 -50 C -20 -68, -6 -72, -2 -62 L 4 -62 L 4 -100 Z" fill="#F3A869" />
                  <path d="M -100 -100 L -4 -100 L -4 -62 L -10 -62 C -14 -72, -28 -68, -28 -50 C -28 -32, -14 -28, -10 -38 L -4 -38 L -4 -4 L -38 -4 L -38 -10 C -28 -14, -32 -28, -50 -28 C -68 -28, -72 -14, -62 -10 L -62 -4 L -100 -4 Z" fill="#428EB8" />
                </g>
              </g>
            </svg>
            """;
        logoIcon.getEngine().loadContent("<html><body style='margin:0;padding:0;overflow:hidden;background:transparent;'>" + iconSvg + "</body></html>");
        logoIcon.setMouseTransparent(true);
        btnLogo.setGraphic(logoIcon);
        btnLogo.setTooltip(new javafx.scene.control.Tooltip("About Tessera"));
        btnLogo.setOnAction(e -> showAboutDialog());

        javafx.scene.control.ToggleButton btnViewExplorer = new javafx.scene.control.ToggleButton("Explorer", new org.kordamp.ikonli.javafx.FontIcon("fas-folder"));
        btnViewExplorer.setSelected(true);
        btnViewExplorer.getStyleClass().add("toolbar-btn");
        
        javafx.scene.control.ToggleButton btnViewCode = new javafx.scene.control.ToggleButton("Code", new org.kordamp.ikonli.javafx.FontIcon("fas-code"));
        btnViewCode.setSelected(true);
        btnViewCode.getStyleClass().add("toolbar-btn");
        
        javafx.scene.control.ToggleButton btnViewDiagram = new javafx.scene.control.ToggleButton("Diagram", new org.kordamp.ikonli.javafx.FontIcon("fas-project-diagram"));
        btnViewDiagram.setSelected(true);
        btnViewDiagram.getStyleClass().add("toolbar-btn");
        
        javafx.scene.control.Button btnSwapPanels = new javafx.scene.control.Button("Swap Panels", new org.kordamp.ikonli.javafx.FontIcon("fas-exchange-alt"));
        btnSwapPanels.getStyleClass().add("toolbar-btn");
        
        btnPlay = new javafx.scene.control.Button("Play");
        btnPlay.setGraphic(new org.kordamp.ikonli.javafx.FontIcon("fas-play"));
        btnPlay.getStyleClass().addAll("toolbar-btn", "btn-play");
        btnPlay.setDisable(true); // Disabled by default until a file/folder is selected or checked

        btnStop = new javafx.scene.control.Button("Stop", new org.kordamp.ikonli.javafx.FontIcon("fas-stop"));
        btnStop.getStyleClass().addAll("toolbar-btn", "btn-stop");
        btnStop.setDisable(true);

        javafx.scene.control.Button btnExport = new javafx.scene.control.Button("Export", new org.kordamp.ikonli.javafx.FontIcon("fas-download"));
        btnExport.getStyleClass().addAll("toolbar-btn", "btn-export");
        btnExport.setTooltip(new javafx.scene.control.Tooltip("Export Selected Routes to Liquibase Changelog"));
        btnExport.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (checked.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please select one or more routes using the checkboxes in the Explorer.");
                themeDialog(alert);
                alert.showAndWait();
                return;
            }
            LiquibaseExportWindow.showForRoutes(getWorkspaceRoot(), checked);
        });

        javafx.scene.control.Button btnRemoteDeploy = new javafx.scene.control.Button("Run Remotely", new org.kordamp.ikonli.javafx.FontIcon("fas-server"));
        btnRemoteDeploy.getStyleClass().addAll("toolbar-btn", "btn-deploy");
        btnRemoteDeploy.setTooltip(new javafx.scene.control.Tooltip("Deploy & Test Selected Routes on Remote Container"));
        btnRemoteDeploy.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (checked.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please select one or more routes using the checkboxes in the Explorer.");
                themeDialog(alert);
                alert.showAndWait();
                return;
            }
            RemoteDeployWindow.showForRoutes(getWorkspaceRoot(), checked);
        });
        
        javafx.scene.control.Button btnManual = new javafx.scene.control.Button("Help Guide", new org.kordamp.ikonli.javafx.FontIcon("fas-question-circle"));
        btnManual.getStyleClass().addAll("toolbar-btn", "btn-manual");
        btnManual.setOnAction(e -> new RouteBuilderHelpWindow().show());

        javafx.scene.control.Button btnVariables = new javafx.scene.control.Button("Variables", new org.kordamp.ikonli.javafx.FontIcon("fas-cube"));
        btnVariables.getStyleClass().addAll("toolbar-btn", "btn-variables");
        btnVariables.setTooltip(new javafx.scene.control.Tooltip("Open Workspace Properties / Variables"));
        btnVariables.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            if (baseDir != null) {
                VariablesEditorWindow.show(baseDir, null);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("No Active Workspace");
                alert.setContentText("Please open a project or folder first.");
                alert.showAndWait();
            }
        });

        javafx.scene.control.Button btnCrypto = new javafx.scene.control.Button("Crypto", new org.kordamp.ikonli.javafx.FontIcon("fas-shield-alt"));
        btnCrypto.getStyleClass().addAll("toolbar-btn", "btn-decrypt");
        btnCrypto.setTooltip(new javafx.scene.control.Tooltip("Open Universal Crypto Studio (AES, Base64, URL)"));
        btnCrypto.setOnAction(e -> CryptoStudioWindow.show());

        javafx.scene.control.Button btnTransform = new javafx.scene.control.Button("Transform", new org.kordamp.ikonli.javafx.FontIcon("fas-random"));
        btnTransform.getStyleClass().addAll("toolbar-btn", "btn-transform");
        btnTransform.setTooltip(new javafx.scene.control.Tooltip("Open Data Transformation Studio"));
        btnTransform.setOnAction(e -> {
            TransformationStudioWindow studio = new TransformationStudioWindow();
            studio.show();
        });

        javafx.scene.control.Button btnValidateStudio = new javafx.scene.control.Button("Validate", new org.kordamp.ikonli.javafx.FontIcon("fas-check-double"));
        btnValidateStudio.getStyleClass().addAll("toolbar-btn", "btn-validate-studio");
        btnValidateStudio.setTooltip(new javafx.scene.control.Tooltip("Open Universal Validator Studio"));
        btnValidateStudio.setOnAction(e -> {
            ValidatorStudioWindow validatorStudio = new ValidatorStudioWindow();
            validatorStudio.show();
        });

        javafx.scene.control.Button btnDiagramStudio = new javafx.scene.control.Button("Diagrams", new org.kordamp.ikonli.javafx.FontIcon("fas-paint-brush"));
        btnDiagramStudio.getStyleClass().addAll("toolbar-btn", "btn-diagram-studio");
        btnDiagramStudio.setTooltip(new javafx.scene.control.Tooltip("Open Universal Diagram Studio"));
        btnDiagramStudio.setOnAction(e -> {
            java.io.File workspaceRoot = getWorkspaceRoot();
            DiagramStudioWindow diagramStudio = new DiagramStudioWindow(workspaceRoot);
            diagramStudio.show();
        });

        javafx.scene.control.Button btnDocConverter = new javafx.scene.control.Button("Doc Converter", new org.kordamp.ikonli.javafx.FontIcon("fas-file-alt"));
        btnDocConverter.getStyleClass().addAll("toolbar-btn", "btn-deps");
        btnDocConverter.setTooltip(new javafx.scene.control.Tooltip("Open Document Converter Studio (PDF/Word/Excel to MD)"));
        btnDocConverter.setOnAction(e -> {
            java.io.File workspaceRoot = getWorkspaceRoot();
            DocumentConverterStudioWindow studio = new DocumentConverterStudioWindow(workspaceRoot);
            studio.show();
        });

        javafx.scene.control.Button btnFakerStudio = new javafx.scene.control.Button("Faker", new org.kordamp.ikonli.javafx.FontIcon("fas-magic"));
        btnFakerStudio.getStyleClass().addAll("toolbar-btn", "btn-faker-studio");
        btnFakerStudio.setTooltip(new javafx.scene.control.Tooltip("Open Universal Faker & Template Studio"));
        btnFakerStudio.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            FakerStudioWindow fakerStudio = new FakerStudioWindow(baseDir);
            fakerStudio.show();
        });

        javafx.scene.control.Button btnKamelets = new javafx.scene.control.Button("Kamelets", new org.kordamp.ikonli.javafx.FontIcon("fas-puzzle-piece"));
        btnKamelets.getStyleClass().addAll("toolbar-btn", "btn-kamelets");
        btnKamelets.setTooltip(new javafx.scene.control.Tooltip("Open Kamelet Studio Builder"));
        btnKamelets.setOnAction(e -> {
            java.io.File base = getWorkspaceRoot();
            if (base != null) {
                KameletStudioWindow.show(base);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        javafx.scene.control.Button btnDeps = new javafx.scene.control.Button("Dependencies", new org.kordamp.ikonli.javafx.FontIcon("fas-list"));
        btnDeps.getStyleClass().addAll("toolbar-btn", "btn-dependencies");
        btnDeps.setTooltip(new javafx.scene.control.Tooltip("Open Dependency Catalog Manager"));
        btnDeps.setOnAction(e -> {
            java.io.File base = getWorkspaceRoot();
            if (base != null) {
                DependencyCatalogWindow.show(base);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        toolBar.getItems().addAll(btnLogo, new javafx.scene.control.Separator(), btnViewExplorer, btnViewCode, btnViewDiagram, new javafx.scene.control.Separator(), btnSwapPanels, new javafx.scene.control.Separator(), btnPlay, btnStop, new javafx.scene.control.Separator(), btnVariables, btnCrypto, btnTransform, btnValidateStudio, btnDiagramStudio, btnDocConverter, btnFakerStudio, btnKamelets, btnDeps, btnRemoteDeploy, btnExport, btnManual);


        boolean[] swapCodeDiagram = {false};

        viewExplorerItem.selectedProperty().bindBidirectional(btnViewExplorer.selectedProperty());
        viewCodeItem.selectedProperty().bindBidirectional(btnViewCode.selectedProperty());
        viewDiagramItem.selectedProperty().bindBidirectional(btnViewDiagram.selectedProperty());

        javafx.scene.layout.VBox topContainer = new javafx.scene.layout.VBox(menuBar, toolBar);
        root.setTop(topContainer);

        // Split pane to hold the three main areas
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setOrientation(Orientation.HORIZONTAL);
        mainSplitPane.getStyleClass().add("main-split-pane");

        // Helper to refresh layout based on selection/checks
        Runnable refreshGlobalLayout = () -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            java.util.List<java.io.File> checkedList = new java.util.ArrayList<>(checked);
            javafx.scene.control.TreeItem<java.io.File> selectedItem = treePane.getTreeView().getSelectionModel().getSelectedItem();
            java.io.File selectedFile = (selectedItem != null && selectedItem.getValue().isFile()) ? selectedItem.getValue() : null;

            if (checkedList.size() > 1) {
                // Multi-route mode: Hide code, show all diagrams
                viewCodeItem.setSelected(false);
                java.util.List<String> contents = new java.util.ArrayList<>();
                for (java.io.File f : checkedList) {
                    try { contents.add(java.nio.file.Files.readString(f.toPath())); } catch (Exception ignored) {}
                }
                diagramPane.setCurrentFile(null);
                diagramPane.renderDiagrams(contents);
            } else {
                // Single route mode: Show code panel
                viewCodeItem.setSelected(true);
                java.io.File target = (checkedList.size() == 1) ? checkedList.get(0) : selectedFile;
                
                if (target != null) {
                    editorPane.loadFile(target);
                    try {
                        String content = java.nio.file.Files.readString(target.toPath());
                        diagramPane.setCurrentFile(target);
                        diagramPane.renderDiagram(content);
                    } catch (Exception ignored) {}
                } else {
                    editorPane.closeFile();
                    diagramPane.renderDiagram("");
                }
            }
            
            boolean hasChecked = !checked.isEmpty();
            boolean hasSelected = selectedFile != null;
            boolean isRunning = runnerProcess[0] != null && runnerProcess[0].isAlive();
            btnPlay.setDisable(isRunning || (!hasChecked && !hasSelected));
        };

        // 1. Left Panel: Route Tree
        treePane = new RouteTreePane(file -> {
            // This is called on single-click or double-click selection
            refreshGlobalLayout.run();
        });
        
        treePane.setOnCheckedFilesChanged(refreshGlobalLayout);
        
        treePane.getTreeView().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            refreshGlobalLayout.run();
        });

        ThemeManager.registerRoot(
treePane);

        helpPortalPane = new HelpPortalPane(() -> {
            viewHelpItem.setSelected(false);
        });
        ThemeManager.registerRoot(
helpPortalPane);

        searchBox.setOnAction(e -> {
            String query = searchBox.getText();
            viewHelpItem.setSelected(true);
            helpPortalPane.search(query);
        });



        java.util.function.BiConsumer<java.io.File, String> playProject = (target, mode) -> {
            boolean offline = "offline".equals(mode);
            btnPlay.setDisable(true);
            btnStop.setDisable(false);
            System.out.println("Starting Routes with JBang... (mode=" + mode + ", target=" + (target == null ? "all" : target.getName()) + ")");
            try {
                java.io.File baseDir = treePane.getBaseDirectory();
                java.io.File workspaceRoot = getWorkspaceRoot();
                
                String executablePath = getJbangExecutable();
                
                java.util.List<String> command = new java.util.ArrayList<>();
                command.add(executablePath);
                command.add("--main=main.CamelJBang");
                String catalogPath = getJbangCatalog();
                if (catalogPath != null) {
                    command.add("--catalog=" + catalogPath);
                }
                if (offline) command.add("--offline");
                command.add("camel");
                command.add("run");
                command.add("--port=0");
                command.add("--console"); // Enable interactive-style output
                command.add("--logging-level=info");
                
                java.io.File propsFile = new java.io.File(baseDir, "application.properties");
                if (!propsFile.exists() && workspaceRoot != null) {
                    java.io.File wsProps = new java.io.File(workspaceRoot, "application.properties");
                    if (wsProps.exists()) {
                        command.add("--properties=../application.properties");
                    }
                } else if (propsFile.exists()) {
                    command.add("--properties=application.properties");
                }
                
                java.util.Set<String> addedPaths = new java.util.HashSet<>();
                java.util.Set<String> dependencies = new java.util.HashSet<>();

                if (target == null) {
                    java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
                    if (!checked.isEmpty()) {
                        for (java.io.File f : checked) {
                            if (f.isFile()) {
                                String relative = baseDir.toPath().toAbsolutePath().relativize(f.toPath().toAbsolutePath()).toString().replace("\\", "/");
                                if (addedPaths.add(relative)) {
                                    command.add(relative);
                                }
                                for (java.io.File srcFile : findCamelKSources(f)) {
                                    String relSrc = baseDir.toPath().toAbsolutePath().relativize(srcFile.toPath().toAbsolutePath()).toString().replace("\\", "/");
                                    if (addedPaths.add(relSrc)) {
                                        command.add(relSrc);
                                    }
                                }
                                dependencies.addAll(findCamelKDependencies(f));
                            }
                        }
                    } else {
                        command.add(".");
                    }
                } else if (target.isFile()) {
                    try {
                        String relative = baseDir.toPath().toAbsolutePath().relativize(target.toPath().toAbsolutePath()).toString().replace("\\", "/");
                        String val = relative.isEmpty() ? target.getName() : relative;
                        if (addedPaths.add(val)) {
                            command.add(val);
                        }
                    } catch (Exception ex) {
                        String val = target.getAbsolutePath().replace("\\", "/");
                        if (addedPaths.add(val)) {
                            command.add(val);
                        }
                    }
                    for (java.io.File srcFile : findCamelKSources(target)) {
                        try {
                            String relSrc = baseDir.toPath().toAbsolutePath().relativize(srcFile.toPath().toAbsolutePath()).toString().replace("\\", "/");
                            if (addedPaths.add(relSrc)) {
                                command.add(relSrc);
                            }
                        } catch (Exception ex) {
                            String val = srcFile.getAbsolutePath().replace("\\", "/");
                            if (addedPaths.add(val)) {
                                command.add(val);
                            }
                        }
                    }
                    dependencies.addAll(findCamelKDependencies(target));
                } else { // Directory
                    java.util.List<java.io.File> collected = new java.util.ArrayList<>();
                    collectAllRouteFiles(target, collected);
                    if (!collected.isEmpty()) {
                        for (java.io.File f : collected) {
                            String relative = baseDir.toPath().toAbsolutePath().relativize(f.toPath().toAbsolutePath()).toString().replace("\\", "/");
                            if (addedPaths.add(relative)) {
                                command.add(relative);
                            }
                            for (java.io.File srcFile : findCamelKSources(f)) {
                                String relSrc = baseDir.toPath().toAbsolutePath().relativize(srcFile.toPath().toAbsolutePath()).toString().replace("\\", "/");
                                if (addedPaths.add(relSrc)) {
                                    command.add(relSrc);
                                }
                            }
                            dependencies.addAll(findCamelKDependencies(f));
                        }
                    } else {
                        command.add(target.getName() + "/*");
                    }
                }

                for (String dep : dependencies) {
                    command.add("--dependency=" + dep);
                }
                for (String dep : DependencyCatalogWindow.getEnabledDependencies(workspaceRoot)) {
                    command.add("--dependency=" + dep);
                }
                
                boolean dev = "dev".equals(mode);
                command.add("--runtime=main");
                if (dev) command.add("--dev");
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.environment().put("TERM", "xterm-256color");
                pb.directory(baseDir);
                pb.redirectErrorStream(true);
                runnerProcess[0] = pb.start();
                showConsole(runnerProcess[0], "Camel Route Runtime (JBang)");
                
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
                        runnerProcess[0].destroyForcibly();
                    }
                }));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        treePane.setOnRunSelected((target, mode) -> {
            playProject.accept(target, mode);
        });

        btnPlay.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (!checked.isEmpty()) {
                playProject.accept(null, "offline"); // playProject already handles checkedFiles internally
            } else {
                javafx.scene.control.TreeItem<java.io.File> selectedItem = treePane.getTreeView().getSelectionModel().getSelectedItem();
                java.io.File target = (selectedItem != null) ? selectedItem.getValue() : null;
                playProject.accept(target, "offline");
            }
        });

        btnStop.setOnAction(e -> {
            boolean hasChecked = !treePane.getCheckedFiles().isEmpty();
            boolean hasSelected = treePane.getTreeView().getSelectionModel().getSelectedItem() != null;
            btnPlay.setDisable(!hasChecked && !hasSelected);
            btnStop.setDisable(true);
            System.out.println("Stopping Routes...");
            if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
                runnerProcess[0].destroy();
                runnerProcess[0].descendants().forEach(ProcessHandle::destroyForcibly);
                runnerProcess[0] = null;
            }
            try {
                String executablePath = getJbangExecutable();
                java.util.List<String> stopCmd = new java.util.ArrayList<>();
                stopCmd.add(executablePath);
                stopCmd.add("--main=main.CamelJBang");
                String catalogPath = getJbangCatalog();
                if (catalogPath != null) {
                    stopCmd.add("--catalog=" + catalogPath);
                }
                stopCmd.add("camel");
                stopCmd.add("stop");
                new ProcessBuilder(stopCmd).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnExport.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (checked.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Please select one or more routes using the checkboxes in the Explorer.");
                themeDialog(alert);
                alert.showAndWait();
                return;
            }
            LiquibaseExportWindow.showForRoutes(treePane.getBaseDirectory(), checked);
        });

        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(RouteBuilderApp.class);

        openItem.setOnAction(e -> {
            javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle("Open Workspace Folder");
            java.io.File selectedDirectory = chooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                treePane.setBaseDirectory(selectedDirectory);
                saveRecentProject(selectedDirectory.getAbsolutePath(), prefs, recentProjectsMenu, treePane);
            }
        });

        newProjectItem.setOnAction(e -> {
            javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle("Select New Project Location");
            java.io.File selectedDirectory = chooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                treePane.setBaseDirectory(selectedDirectory);
                saveRecentProject(selectedDirectory.getAbsolutePath(), prefs, recentProjectsMenu, treePane);
            }
        });
        
        newSampleProjectItem.setOnAction(e -> {
            javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle("Select Directory for Sample Project");
            java.io.File selectedDirectory = chooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                // The route builder file tree should point to workspace/camel
                java.io.File camelDir = new java.io.File(selectedDirectory, "camel");
                if (!camelDir.exists()) camelDir.mkdirs();

                generateChapterSamples(treePane, selectedDirectory);
                
                // setBaseDirectory will pick up the 'camel' folder
                treePane.setBaseDirectory(selectedDirectory);
                saveRecentProject(selectedDirectory.getAbsolutePath(), prefs, recentProjectsMenu, treePane);

                // Update preferences for other studios to point to the new workspace folders
                java.util.prefs.Preferences transPrefs = java.util.prefs.Preferences.userNodeForPackage(TransformationStudioWindow.class);
                transPrefs.put("mappingsPath", new java.io.File(selectedDirectory, "mappings").getAbsolutePath());

                java.util.prefs.Preferences diagPrefs = java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class);
                diagPrefs.put("workspaceRoot", new java.io.File(selectedDirectory, "diagrams").getAbsolutePath());

                java.util.prefs.Preferences docPrefs = java.util.prefs.Preferences.userNodeForPackage(DocumentConverterStudioWindow.class);
                docPrefs.put("workspaceRoot", new java.io.File(selectedDirectory, "docs").getAbsolutePath());
                docPrefs.put("outputRoot", new java.io.File(selectedDirectory, "docs/output").getAbsolutePath());

                java.util.prefs.Preferences valPrefs = java.util.prefs.Preferences.userNodeForPackage(ValidatorStudioWindow.class);
                valPrefs.put("workspaceRoot", new java.io.File(selectedDirectory, "validator").getAbsolutePath());
            }
        });
        
        newFileItem.setOnAction(e -> treePane.createTemplateFile("new-file.yaml", ""));
        newKameletItem.setOnAction(e -> treePane.createTemplateFile("my-kamelet.kamelet.yaml", "apiVersion: camel.apache.org/v1alpha1\nkind: Kamelet\nmetadata:\n  name: my-kamelet\nspec:\n  definition:\n    title: \"My Kamelet\"\n    description: \"Does something\"\n    properties:\n      foo:\n        type: string\n  template:\n    from:\n      uri: \"timer:tick\"\n      steps:\n        - log: \"${body}\"\n"));
        newComponentItem.setOnAction(e -> treePane.createTemplateFile("MyComponent.java", "package com.example;\n\nimport org.apache.camel.Endpoint;\nimport org.apache.camel.support.DefaultComponent;\nimport java.util.Map;\n\npublic class MyComponent extends DefaultComponent {\n    @Override\n    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {\n        return null;\n    }\n}\n"));
        newProcessorItem.setOnAction(e -> treePane.createTemplateFile("MyProcessor.java", "package com.example;\n\nimport org.apache.camel.Exchange;\nimport org.apache.camel.Processor;\n\npublic class MyProcessor implements Processor {\n    @Override\n    public void process(Exchange exchange) throws Exception {\n        String body = exchange.getIn().getBody(String.class);\n        exchange.getIn().setBody(body + \" processed\");\n    }\n}\n"));
        newJavaDslItem.setOnAction(e -> treePane.createTemplateFile("MyRoute.java", "package com.example;\n\nimport org.apache.camel.builder.RouteBuilder;\n\npublic class MyRoute extends RouteBuilder {\n    @Override\n    public void configure() throws Exception {\n        from(\"timer:java?period=1000\")\n            .log(\"Java DSL Route Triggered\")\n            .to(\"mock:result\");\n    }\n}\n"));
        newXmlDslItem.setOnAction(e -> treePane.createTemplateFile("xml-route.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<routes xmlns=\"http://camel.apache.org/schema/spring\">\n    <route id=\"xml-route\">\n        <from uri=\"timer:xml?period=1000\"/>\n        <log message=\"XML DSL Route Triggered\"/>\n        <to uri=\"mock:result\"/>\n    </route>\n</routes>\n"));
        newYamlDslItem.setOnAction(e -> treePane.createTemplateFile("yaml-route.yaml", "- route:\n    id: \"yaml-route\"\n    from:\n      uri: \"timer:yaml?period=1000\"\n      steps:\n        - log: \"YAML DSL Route Triggered\"\n"));
        newGroovyDslItem.setOnAction(e -> treePane.createTemplateFile("groovy-route.groovy", "import org.apache.camel.builder.RouteBuilder\n\nclass MyGroovyRoute extends RouteBuilder {\n    void configure() {\n        from(\"timer:groovy?period=1000\")\n            .log(\"Groovy DSL Route Triggered\")\n            .to(\"mock:result\")\n    }\n}\n"));
        newKotlinDslItem.setOnAction(e -> treePane.createTemplateFile("kotlin-route.kts", "import org.apache.camel.builder.RouteBuilder\n\nclass MyKotlinRoute : RouteBuilder() {\n    override fun configure() {\n        from(\"timer:kotlin?period=1000\")\n            .log(\"Kotlin DSL Route Triggered\")\n            .to(\"mock:result\")\n    }\n}\n"));
        newXsltItem.setOnAction(e -> treePane.createTemplateFile("transform.xslt", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n  <xsl:template match=\"/\">\n    <result>\n      <message>Transformed by XSLT</message>\n    </result>\n  </xsl:template>\n</xsl:stylesheet>\n"));
        newJsltItem.setOnAction(e -> treePane.createTemplateFile("transform.jslt", "{\n  \"transformed\": .body,\n  \"status\": \"success\"\n}\n"));
        newFtlItem.setOnAction(e -> treePane.createTemplateFile("template.ftl", "Hello ${headers.name!'World'}!\nYour message is: ${body}\n"));

        // 2. Middle Panel: VSCode-like Yaml Editor (Refreshes tree on save)
        editorPane = new YamlEditorPane(this::updateDiagram, () -> {
            treePane.refresh();
        });
        editorPane.setLspManager(lspManager);
        ThemeManager.registerRoot(
editorPane);

        editorPane.setOnPlayFile((file, mode) -> {
            boolean offline = "offline".equals(mode);
            System.out.println("Starting Single File with JBang... (mode=" + mode + ")");
            try {
                java.io.File baseDir = file.getParentFile();
                String executablePath = getJbangExecutable();
                
                java.util.List<String> command = new java.util.ArrayList<>();
                command.add(executablePath);
                command.add("--main=main.CamelJBang");
                String catalogPath = getJbangCatalog();
                if (catalogPath != null) {
                    command.add("--catalog=" + catalogPath);
                }
                if (offline) command.add("--offline");
                command.add("camel");
                command.add("run");
                command.add("--port=0");
                command.add("--console"); // Enable interactive-style output
                command.add("--logging-level=info");
                
                java.io.File workspaceDir = treePane != null ? treePane.getBaseDirectory() : null;
                java.io.File workspaceRoot = getWorkspaceRoot();

                java.io.File propsFile = new java.io.File(baseDir, "application.properties");
                if (propsFile.exists()) {
                    command.add("--properties=application.properties");
                } else {
                    java.io.File targetProps = null;
                    if (workspaceDir != null && new java.io.File(workspaceDir, "application.properties").exists()) {
                        targetProps = new java.io.File(workspaceDir, "application.properties");
                    } else if (workspaceRoot != null && new java.io.File(workspaceRoot, "application.properties").exists()) {
                        targetProps = new java.io.File(workspaceRoot, "application.properties");
                    }
                    
                    if (targetProps != null) {
                        try {
                            String relProps = baseDir.toPath().toAbsolutePath().relativize(targetProps.toPath().toAbsolutePath()).toString().replace("\\", "/");
                            command.add("--properties=" + relProps);
                        } catch (Exception ex) {
                            command.add("--properties=" + targetProps.getAbsolutePath().replace("\\", "/"));
                        }
                    }
                }
                
                java.util.Set<String> addedPaths = new java.util.HashSet<>();
                if (addedPaths.add(file.getName())) {
                    command.add(file.getName());
                }
                for (java.io.File srcFile : findCamelKSources(file)) {
                    try {
                        String relSrc = baseDir.toPath().toAbsolutePath().relativize(srcFile.toPath().toAbsolutePath()).toString().replace("\\", "/");
                        if (addedPaths.add(relSrc)) {
                            command.add(relSrc);
                        }
                    } catch (Exception ex) {
                        if (addedPaths.add(srcFile.getName())) {
                            command.add(srcFile.getName());
                        }
                    }
                }
                for (String dep : findCamelKDependencies(file)) {
                    command.add("--dependency=" + dep);
                }
                for (String dep : DependencyCatalogWindow.getEnabledDependencies(workspaceRoot)) {
                    command.add("--dependency=" + dep);
                }
                boolean dev = "dev".equals(mode);
                command.add("--runtime=main");
                if (dev) command.add("--dev");
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.environment().put("TERM", "xterm-256color");
                pb.directory(baseDir);
                pb.redirectErrorStream(true);
                Process singleProcess = pb.start();
                
                if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
                    runnerProcess[0].destroy();
                    runnerProcess[0].descendants().forEach(ProcessHandle::destroyForcibly);
                }
                runnerProcess[0] = singleProcess;
                showConsole(runnerProcess[0], "Single Route: " + file.getName());
                
                javafx.application.Platform.runLater(() -> {
                    btnPlay.setDisable(true);
                    btnStop.setDisable(false);
                    if (editorPane != null && editorPane.getBtnStopFile() != null) {
                        editorPane.getBtnStopFile().setDisable(false);
                    }
                });
                
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (singleProcess.isAlive()) {
                        singleProcess.destroyForcibly();
                    }
                }));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        editorPane.setOnStopFile(() -> {
            System.out.println("Stopping Single Route...");
            if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
                runnerProcess[0].destroy();
                runnerProcess[0].descendants().forEach(ProcessHandle::destroyForcibly);
                runnerProcess[0] = null;
            }
            javafx.application.Platform.runLater(() -> {
                if (editorPane != null && editorPane.getBtnStopFile() != null) {
                    editorPane.getBtnStopFile().setDisable(true);
                }
                if (btnStop != null) btnStop.setDisable(true);
            });
        });

        // 3. Right Panel: Visual Diagram
        diagramPane = new DiagramPane(theme -> setGlobalTheme(theme), updatedYaml -> {
            editorPane.setText(updatedYaml);
        });
        ThemeManager.registerRoot(
diagramPane);

        Runnable updateLayout = () -> {
            mainSplitPane.getItems().clear();
            if (viewExplorerItem.isSelected()) mainSplitPane.getItems().add(treePane);
            
            if (swapCodeDiagram[0]) {
                if (viewDiagramItem.isSelected()) mainSplitPane.getItems().add(diagramPane);
                if (viewCodeItem.isSelected()) mainSplitPane.getItems().add(editorPane);
            } else {
                if (viewCodeItem.isSelected()) mainSplitPane.getItems().add(editorPane);
                if (viewDiagramItem.isSelected()) mainSplitPane.getItems().add(diagramPane);
            }

            if (viewHelpItem.isSelected()) {
                mainSplitPane.getItems().add(helpPortalPane);
            }
            
            int count = mainSplitPane.getItems().size();
            if (count == 4) {
                mainSplitPane.setDividerPositions(0.15, 0.45, 0.75);
            } else if (count == 3) {
                mainSplitPane.setDividerPositions(0.18, 0.58);
            } else if (count == 2) {
                mainSplitPane.setDividerPositions(0.3);
            }
        };

        swapLayoutItem.setOnAction(e -> {
            swapCodeDiagram[0] = !swapCodeDiagram[0];
            updateLayout.run();
        });
        btnSwapPanels.setOnAction(e -> {
            swapCodeDiagram[0] = !swapCodeDiagram[0];
            updateLayout.run();
        });

        viewExplorerItem.selectedProperty().addListener((obs, oldVal, newVal) -> updateLayout.run());
        viewCodeItem.selectedProperty().addListener((obs, oldVal, newVal) -> updateLayout.run());
        viewDiagramItem.selectedProperty().addListener((obs, oldVal, newVal) -> updateLayout.run());
        viewHelpItem.selectedProperty().addListener((obs, oldVal, newVal) -> updateLayout.run());

        editorPane.setOnToggleDiagram(() -> {
            viewDiagramItem.setSelected(!viewDiagramItem.isSelected());
        });

        editorPane.setOnClose(() -> viewCodeItem.setSelected(false));
        diagramPane.setOnClose(() -> viewDiagramItem.setSelected(false));

        diagramPane.setOnMaximize(() -> {
            boolean onlyDiagram = !viewExplorerItem.isSelected() && !viewCodeItem.isSelected() && viewDiagramItem.isSelected();
            if (onlyDiagram) {
                viewExplorerItem.setSelected(true);
                viewCodeItem.setSelected(true);
            } else {
                viewExplorerItem.setSelected(false);
                viewCodeItem.setSelected(false);
                viewDiagramItem.setSelected(true);
            }
        });

        // Initial setup
        updateLayout.run();

        undoItem.setOnAction(e -> editorPane.undo());
        redoItem.setOnAction(e -> editorPane.redo());
        cutItem.setOnAction(e -> editorPane.cut());
        copyItem.setOnAction(e -> editorPane.copy());
        pasteItem.setOnAction(e -> editorPane.paste());
        selectAllItem.setOnAction(e -> editorPane.selectAll());
        
        resetLayoutItem.setOnAction(e -> {
            swapCodeDiagram[0] = false;
            viewExplorerItem.setSelected(true);
            viewCodeItem.setSelected(true);
            viewDiagramItem.setSelected(true);
            updateLayout.run();
        });

        // 4. Bottom Panel: Console
        consolePane = new com.tessera.ui.components.ConsolePane();
        ThemeManager.registerRoot(
consolePane);

        // Wrap in a vertical SplitPane
        SplitPane verticalSplitPane = new SplitPane();
        verticalSplitPane.setOrientation(Orientation.VERTICAL);
        verticalSplitPane.getStyleClass().add("main-split-pane");
        verticalSplitPane.getItems().addAll(mainSplitPane, consolePane);
        verticalSplitPane.setDividerPositions(0.75); // 75% for top editors, 25% for console

        root.setCenter(verticalSplitPane);

        // Bottom Status Bar
        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox();
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new javafx.geometry.Insets(3, 10, 3, 10));
        javafx.scene.control.Label statusLabel = new javafx.scene.control.Label("Ready");
        statusBar.getChildren().add(statusLabel);
        root.setBottom(statusBar);

        com.tessera.ui.components.ThemeManager.registerRoot(root);
        Scene scene = new Scene(root, 1400, 800);
        
        // Load CSS
        String css = getClass().getResource("/styles/main.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        // Set a nice dark theme background
        scene.setFill(javafx.scene.paint.Color.web("#1e1e1e"));

        primaryStage.setTitle("Tessera - Enterprise Integration Studio");
        primaryStage.setScene(scene);
        
        primaryStage.setOnCloseRequest(e -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });
        
        String lastOpenedDir = prefs.get("lastOpenedDir", null);
        if (lastOpenedDir != null) {
            java.io.File dir = new java.io.File(lastOpenedDir);
            if (dir.exists() && dir.isDirectory()) {
                treePane.setBaseDirectory(dir);
            }
        }
        updateRecentProjectsMenu(prefs, recentProjectsMenu, treePane);

        primaryStage.setMaximized(true);
        primaryStage.show();
        
        // Apply initial theme
        javafx.application.Platform.runLater(() -> setGlobalTheme(com.tessera.ui.components.ThemeManager.getCurrentThemeName()));
        
        // Setup default dummy route and write it to disk so the tree sees it
        String defaultYaml = "- route:\n" +
            "    id: \"complex-financial-transaction\"\n" +
            "    from:\n" +
            "      uri: \"timer:trigger\"\n" +
            "      steps:\n" +
            "        - log:\n" +
            "            message: \"Starting complex transaction...\"\n" +
            "        - setBody:\n" +
            "            constant: \"{ 'transactionId': 'TXN-9988' }\"\n" +
            "        - doTry:\n" +
            "            steps:\n" +
            "              - log:\n" +
            "                  message: \"Validating payload...\"\n" +
            "              - choice:\n" +
            "                  when:\n" +
            "                    - simple: \"${body} contains 'TXN'\"\n" +
            "                      steps:\n" +
            "                        - setHeader:\n" +
            "                            name: \"Validation\"\n" +
            "                            constant: \"PASSED\"\n" +
            "                  otherwise:\n" +
            "                    steps:\n" +
            "                      - to: \"mock:dead-letter\"\n" +
            "              - log:\n" +
            "                  message: \"Validation successful, broadcasting parallelly...\"\n" +
            "              - multicast:\n" +
            "                  steps:\n" +
            "                    - to: \"kafka:transactions-topic\"\n" +
            "                    - to: \"mongodb:myDb?database=financial\"\n" +
            "                    - to: \"ibmmq:queue:TXN.PROCESSING.QUEUE\"\n" +
            "            doCatch:\n" +
            "              - exception:\n" +
            "                  - \"java.lang.Exception\"\n" +
            "                steps:\n" +
            "                  - log:\n" +
            "                      message: \"Error processing transaction: ${exception.message}\"\n" +
            "                  - to: \"mock:error-handler\"\n" +
            "            doFinally:\n" +
            "              steps:\n" +
            "                - log:\n" +
            "                    message: \"Transaction processing finished.\"\n";

        if (editorPane.getCurrentFile() == null) {
            try {
                java.io.File dir = new java.io.File(System.getProperty("user.dir"), "camel");
                if (!dir.exists()) {
                    java.io.File routesDir = new java.io.File(System.getProperty("user.dir"), "routes");
                    if (routesDir.exists()) {
                        dir = routesDir;
                    }
                }
                dir.mkdirs();
                java.io.File defaultFile = new java.io.File(dir, "complex-financial-transaction.yaml");
                if (!defaultFile.exists()) {
                    java.nio.file.Files.writeString(defaultFile.toPath(), defaultYaml);
                    treePane.refresh();
                }
                editorPane.loadFile(defaultFile);
            } catch (Exception e) {
                editorPane.setText(defaultYaml);
            }
        }
    }

    private void updateDiagram(String yamlContent) {
        if (editorPane != null) {
            diagramPane.setCurrentFile(editorPane.getCurrentFile());
        }
        diagramPane.renderDiagram(yamlContent);
    }

    private void saveRecentProject(String path, java.util.prefs.Preferences prefs, javafx.scene.control.Menu recentProjectsMenu, RouteTreePane treePane) {
        prefs.put("lastOpenedDir", path);
        String history = prefs.get("recentProjects", "");
        java.util.List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(history.split(";")));
        list.remove(path);
        list.add(0, path);
        if (list.size() > 10) list = list.subList(0, 10);
        prefs.put("recentProjects", String.join(";", list));
        updateRecentProjectsMenu(prefs, recentProjectsMenu, treePane);
    }

    private void updateRecentProjectsMenu(java.util.prefs.Preferences prefs, javafx.scene.control.Menu recentProjectsMenu, RouteTreePane treePane) {
        recentProjectsMenu.getItems().clear();
        String history = prefs.get("recentProjects", "");
        if (history.isEmpty()) return;
        for (String path : history.split(";")) {
            if (path.isEmpty()) continue;
            javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(path);
            item.setOnAction(e -> {
                java.io.File dir = new java.io.File(path);
                if (dir.exists() && dir.isDirectory()) {
                    treePane.setBaseDirectory(dir);
                    saveRecentProject(path, prefs, recentProjectsMenu, treePane);
                }
            });
            recentProjectsMenu.getItems().add(item);
        }
    }

    private void generateChapterSamples(RouteTreePane treePane, java.io.File base) {
        try {
            java.io.File docsDir = new java.io.File(base, "docs");
            java.io.File docsInputDir = new java.io.File(docsDir, "input");
            java.io.File docsOutputDir = new java.io.File(docsDir, "output");

            if (!docsInputDir.exists()) docsInputDir.mkdirs();
            if (!docsOutputDir.exists()) docsOutputDir.mkdirs();

            generateFromIndex(base, "/samples/");

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (treePane != null) {
            treePane.refresh();
        }
    }

    private void generateFromIndex(java.io.File base, String resourcePrefix) throws java.io.IOException {
        byte[] indexBytes = readResourceBytes(resourcePrefix + "files.txt");
        if (indexBytes.length == 0) return;
        
        String filesIndex = new String(indexBytes, java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = filesIndex.split("\\r?\\n");
        for (String relativePath : lines) {
            relativePath = relativePath.trim();
            if (relativePath.isEmpty() || relativePath.endsWith("files.txt")) continue;

            java.io.File targetFile = new java.io.File(base, relativePath);
            if (targetFile.exists()) continue;

            byte[] content = readResourceBytes(resourcePrefix + relativePath);
            if (content.length == 0) continue;

            java.io.File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            java.nio.file.Files.write(targetFile.toPath(), content);
        }
    }

    public void dumpSamplesToResources() {
        generateChapterSamples(null, null);
    }

    private byte[] readResourceBytes(String path) {
        try (java.io.InputStream is = RouteBuilderApp.class.getResourceAsStream(path)) {
            if (is == null) {
                return new byte[0];
            }
            return is.readAllBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }


    @Override
    public void stop() {
        if (lspManager != null) {
            lspManager.stop();
        }
    }

    public static java.util.Set<String> detectDependenciesFromProperties(java.util.List<String> propertyPaths) {
        java.util.Set<String> deps = new java.util.HashSet<>();
        if (propertyPaths == null) return deps;
        for (String pathStr : propertyPaths) {
            java.io.File file = new java.io.File(pathStr);
            if (!file.exists()) continue;
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("#") || line.isEmpty()) continue;
                    int eq = line.indexOf('=');
                    if (eq == -1) continue;
                    String value = line.substring(eq + 1).trim();
                    if (value.contains(":")) {
                        String[] parts = value.split(":");
                        if (parts.length > 0) {
                            String first = parts[0].trim();
                            if ("stub".equals(first)) {
                                deps.add("stub");
                                if (parts.length > 1) {
                                    deps.add(parts[1].trim());
                                }
                            } else {
                                deps.add(first);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return deps;
    }

    public java.io.File getWorkspaceRoot() {
        if (treePane == null) return null;
        java.io.File base = treePane.getBaseDirectory();
        if (base != null && (base.getName().equals("routes") || base.getName().equals("camel"))) {
            return base.getParentFile();
        }
        return base;
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void installClipboardShortcuts(javafx.scene.web.WebView webView) {
        webView.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case C:
                        copyWebViewSelection(webView);
                        event.consume();
                        break;
                    case X:
                        cutWebViewSelection(webView);
                        event.consume();
                        break;
                    case V:
                        pasteToWebView(webView);
                        event.consume();
                        break;
                    case A:
                        selectAllWebView(webView);
                        event.consume();
                        break;
                    case Z:
                        if (event.isShiftDown()) {
                            redoWebView(webView);
                        } else {
                            undoWebView(webView);
                        }
                        event.consume();
                        break;
                    case Y:
                        redoWebView(webView);
                        event.consume();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private static void copyWebViewSelection(javafx.scene.web.WebView webView) {
        try {
            String selection = (String) webView.getEngine().executeScript(
                "if(window.editor) { " +
                "  var sel = window.editor.getSelection(); " +
                "  window.editor.getModel().getValueInRange(sel); " +
                "} else { " +
                "  window.getSelection ? window.getSelection().toString() : ''; " +
                "}"
            );
            if (selection != null && !selection.isEmpty()) {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(selection);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cutWebViewSelection(javafx.scene.web.WebView webView) {
        copyWebViewSelection(webView);
        try {
            webView.getEngine().executeScript(
                "if(window.editor) { " +
                "  window.editor.executeEdits('clipboard', [{range: window.editor.getSelection(), text: ''}]); " +
                "}"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void pasteToWebView(javafx.scene.web.WebView webView) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String content = clipboard.getString();
            try {
                String encoded = java.net.URLEncoder.encode(content, "UTF-8").replace("+", "%20");
                webView.getEngine().executeScript(
                    "if(window.editor) { " +
                    "  window.editor.executeEdits('clipboard', [{range: window.editor.getSelection(), text: decodeURIComponent('" + encoded + "')}]); " +
                    "}"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void selectAllWebView(javafx.scene.web.WebView webView) {
        try {
            webView.getEngine().executeScript(
                "if(window.editor) { " +
                "  window.editor.setSelection(window.editor.getModel().getFullModelRange()); " +
                "}"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void undoWebView(javafx.scene.web.WebView webView) {
        try {
            webView.getEngine().executeScript(
                "if(window.editor) { " +
                "  window.editor.trigger('keyboard', 'undo', null); " +
                "}"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void redoWebView(javafx.scene.web.WebView webView) {
        try {
            webView.getEngine().executeScript(
                "if(window.editor) { " +
                "  window.editor.trigger('keyboard', 'redo', null); " +
                "}"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean hasStdbuf() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("linux")) {
            return false;
        }
        return new java.io.File("/usr/bin/stdbuf").exists() ||
               new java.io.File("/bin/stdbuf").exists() ||
               new java.io.File("/usr/sbin/stdbuf").exists();
    }

    public static String getJbangExecutable() {
        String os = System.getProperty("os.name").toLowerCase();
        String jbangScript = os.contains("win") ? "jbang.cmd" : "jbang";
        java.io.File jbangExe = null;
        try {
            java.io.File jarFile = new java.io.File(RouteBuilderApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            java.io.File installDir = jarFile.getParentFile().getParentFile();
            jbangExe = new java.io.File(installDir, jbangScript);
        } catch (Exception ignored) {}
        
        if (jbangExe == null || !jbangExe.exists()) {
            jbangExe = new java.io.File(System.getProperty("user.dir"), jbangScript);
        }
        if (!jbangExe.exists()) {
            jbangExe = new java.io.File(new java.io.File(System.getProperty("user.dir"), "route-builder"), jbangScript);
        }
        return jbangExe.exists() ? jbangExe.getAbsolutePath() : jbangScript;
    }

    public static String getJbangCatalog() {
        java.io.File catalogFile = null;
        try {
            java.io.File jarFile = new java.io.File(RouteBuilderApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            java.io.File installDir = jarFile.getParentFile().getParentFile();
            catalogFile = new java.io.File(installDir, "jbang-catalog.json");
        } catch (Exception ignored) {}
        
        if (catalogFile == null || !catalogFile.exists()) {
            catalogFile = new java.io.File(System.getProperty("user.dir"), "jbang-catalog.json");
        }
        if (!catalogFile.exists()) {
            catalogFile = new java.io.File(new java.io.File(System.getProperty("user.dir"), "route-builder"), "jbang-catalog.json");
        }
        return catalogFile.exists() ? catalogFile.getAbsolutePath().replace("\\", "/") : null;
    }

    public static String getCamelVersion() {
        String catalogPath = getJbangCatalog();
        if (catalogPath != null) {
            try {
                String content = java.nio.file.Files.readString(java.nio.file.Paths.get(catalogPath));
                int idx = content.indexOf("org.apache.camel:camel-jbang-main:");
                if (idx != -1) {
                    int start = idx + "org.apache.camel:camel-jbang-main:".length();
                    int end = content.indexOf('"', start);
                    if (end != -1) {
                        return content.substring(start, end).trim();
                    }
                }
            } catch (Exception ignored) {}
        }
        return "4.18.0"; // default fallback
    }

    private static void collectAllRouteFiles(java.io.File dir, java.util.List<java.io.File> collected) {
        java.io.File[] files = dir.listFiles();
        if (files == null) return;
        for (java.io.File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".")) {
                    collectAllRouteFiles(f, collected);
                }
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".groovy")) {
                    collected.add(f);
                }
            }
        }
    }

    private static java.util.List<java.io.File> findCamelKSources(java.io.File file) {
        java.util.List<java.io.File> sources = new java.util.ArrayList<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        findCamelKSourcesRecursive(file, sources, visited);
        return sources;
    }

    private static void findCamelKSourcesRecursive(java.io.File file, java.util.List<java.io.File> sources, java.util.Set<String> visited) {
        if (file == null || !file.exists() || !file.isFile()) return;
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (Exception ex) {
            canonicalPath = file.getAbsolutePath();
        }
        if (!visited.add(canonicalPath)) return;

        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
            java.io.File parent = file.getParentFile();
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("#") && line.contains("camel-k:") && line.contains("source=")) {
                    String src = line.substring(line.indexOf("source=") + 7).trim();
                    if (!src.isEmpty()) {
                        java.io.File srcFile = new java.io.File(parent, src);
                        if (srcFile.exists()) {
                            sources.add(srcFile);
                            findCamelKSourcesRecursive(srcFile, sources, visited);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static java.util.List<String> findCamelKDependencies(java.io.File file) {
        java.util.List<String> deps = new java.util.ArrayList<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        findCamelKDependenciesRecursive(file, deps, visited);
        return deps;
    }

    private static void findCamelKDependenciesRecursive(java.io.File file, java.util.List<String> deps, java.util.Set<String> visited) {
        if (file == null || !file.exists() || !file.isFile()) return;
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (Exception ex) {
            canonicalPath = file.getAbsolutePath();
        }
        if (!visited.add(canonicalPath)) return;

        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
            java.io.File parent = file.getParentFile();
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("#") && line.contains("camel-k:")) {
                    if (line.contains("dependency=")) {
                        String dep = line.substring(line.indexOf("dependency=") + 11).trim();
                        if (!dep.isEmpty()) {
                            if (dep.startsWith("mvn:")) {
                                dep = dep.substring(4);
                            }
                            if (!deps.contains(dep)) {
                                deps.add(dep);
                            }
                        }
                    } else if (line.contains("source=")) {
                        String src = line.substring(line.indexOf("source=") + 7).trim();
                        if (!src.isEmpty()) {
                            java.io.File srcFile = new java.io.File(parent, src);
                            if (srcFile.exists()) {
                                findCamelKDependenciesRecursive(srcFile, deps, visited);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public void loadWorkspaceProperties() {
        java.io.File wsRoot = getWorkspaceRoot();
        if (wsRoot == null) {
            // Fallback to user.dir if treePane is not yet initialized
            wsRoot = new java.io.File(System.getProperty("user.dir"));
            // Climb up to find application.properties if we are deep in a project
            while (wsRoot != null && !new java.io.File(wsRoot, "application.properties").exists()) {
                wsRoot = wsRoot.getParentFile();
            }
        }

        if (wsRoot != null && new java.io.File(wsRoot, "application.properties").exists()) {
            java.io.File propsFile = new java.io.File(wsRoot, "application.properties");
            try (java.io.InputStream input = new java.io.FileInputStream(propsFile)) {
                java.util.Properties props = new java.util.Properties();
                props.load(input);
                
                String absoluteWsPath = wsRoot.getAbsolutePath().replace("\\", "/");
                System.setProperty("WORKSPACE_ROOT_DIR", absoluteWsPath);
                System.out.println("[Tessera] Workspace Root: " + absoluteWsPath);

                boolean needsUpdate = false;
                String[] vars = {"FAKER_TEMPLATES_DIR", "FAKER_DB_DIR", "MAPPING_DIR", "DRAWINGS_DIR", "VALIDATOR_DIR"};
                for (String var : vars) {
                    String value = props.getProperty(var);
                    if (value != null && !value.trim().isEmpty()) {
                        java.io.File resolvedFile = new java.io.File(value);
                        if (!resolvedFile.isAbsolute()) {
                            resolvedFile = new java.io.File(wsRoot, value);
                        }
                        
                        String absPath = resolvedFile.getAbsolutePath().replace("\\", "/");
                        System.setProperty(var, absPath);
                        
                        // Check if we should update the file to absolute path
                        if (!value.equals(absPath)) {
                            props.setProperty(var, absPath);
                            needsUpdate = true;
                        }
                        System.out.println("[Tessera] Loaded environment: " + var + "=" + absPath);
                    }
                }

                if (!absoluteWsPath.equals(props.getProperty("WORKSPACE_ROOT_DIR"))) {
                    props.setProperty("WORKSPACE_ROOT_DIR", absoluteWsPath);
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    try (java.io.OutputStream output = new java.io.FileOutputStream(propsFile)) {
                        props.store(output, "Standardized Tessera Workspace Configuration - Automatically Updated to Absolute Paths");
                        System.out.println("[Tessera] Updated application.properties with absolute paths for better portability across subdirectories.");
                    }
                }
            } catch (Exception e) {
                System.err.println("[Tessera] Error loading workspace properties: " + e.getMessage());
            }
        }
    }

    private void showAboutDialog() {
        javafx.scene.control.Dialog<Void> aboutDialog = new javafx.scene.control.Dialog<>();
        aboutDialog.setTitle("About Tessera");
        aboutDialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        
        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        webView.setPrefSize(800, 500); // Increased size to fit the full logo + tagline SVG
        
        String logoUrl = getClass().getResource("/tessera-logo-tagline.svg").toExternalForm();
        
        String content = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', sans-serif; background: #f6f9fa; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; overflow: hidden; }
                    img { width: 100%; height: auto; max-width: 800px; }
                </style>
            </head>
            <body>
                <img src="LOGO_URL" />
            </body>
            </html>
            """.replace("LOGO_URL", logoUrl);
            
        webView.getEngine().loadContent(content);
        aboutDialog.getDialogPane().setContent(webView);
        themeDialog(aboutDialog);
        aboutDialog.showAndWait();
    }
}
