package com.routebuilder.ui;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class RouteBuilderApp extends Application {

    private YamlEditorPane editorPane;
    private DiagramPane diagramPane;
    private RouteTreePane treePane;
    private com.routebuilder.lsp.LspManager lspManager;
    private ConsolePane consolePane;

    public static String currentThemeClass = "theme-vscode-dark";
    public static String currentDynamicCssUri = null;
    public static final java.util.List<javafx.scene.Parent> themedRoots = new java.util.ArrayList<>();

    public static void themeDialog(javafx.scene.control.Dialog<?> dialog) {
        dialog.getDialogPane().getStyleClass().add(currentThemeClass);
        try {
            dialog.getDialogPane().getStylesheets().add(RouteBuilderApp.class.getResource("/styles/main.css").toExternalForm());
            if (currentDynamicCssUri != null) {
                dialog.getDialogPane().getStylesheets().add(currentDynamicCssUri);
            }
        } catch (Exception ignored) {}
        themedRoots.add(dialog.getDialogPane());
        dialog.setOnCloseRequest(e -> themedRoots.remove(dialog.getDialogPane()));
    }

    private void showConsole(Process process, String title) {
        javafx.application.Platform.runLater(() -> {
            if (consolePane != null) {
                consolePane.clear();
                consolePane.log("\033[1;36m╔══ " + title + " ══╗\033[0m\n");
            }
        });

        // Pipe stdout — read raw bytes so \r and mid-line ANSI sequences are preserved
        new Thread(() -> pipeStream(process.getInputStream()), "console-stdout").start();
        new Thread(() -> pipeStream(process.getErrorStream()),  "console-stderr").start();
    }

    private void pipeStream(java.io.InputStream stream) {
        byte[] buf = new byte[2048];
        int n;
        try {
            while ((n = stream.read(buf)) != -1) {
                // Decode using the platform charset (UTF-8 for Quarkus / JBang)
                final String chunk = new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8);
                if (consolePane != null) consolePane.log(chunk);
            }
        } catch (Exception ignored) {}
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
        lspManager = new com.routebuilder.lsp.LspManager();
        lspManager.start();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

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

        javafx.scene.control.MenuItem decryptItem = new javafx.scene.control.MenuItem("Decrypt Ciphertext...");
        decryptItem.setOnAction(e -> DecryptToolWindow.show());

        editMenu.getItems().addAll(undoItem, redoItem, new javafx.scene.control.SeparatorMenuItem(), cutItem, copyItem, pasteItem, new javafx.scene.control.SeparatorMenuItem(), decryptItem, selectAllItem);

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
        
        javafx.scene.control.MenuItem manualItem = new javafx.scene.control.MenuItem("Open User Manual");
        manualItem.setOnAction(e -> showManual());

        javafx.scene.control.MenuItem interactiveHelpItem = new javafx.scene.control.MenuItem("Open Help Portal");
        interactiveHelpItem.setOnAction(e -> viewHelpItem.setSelected(true));

        helpMenu.getItems().addAll(maxItem, restoreItem, new javafx.scene.control.SeparatorMenuItem(), searchItem, manualItem, interactiveHelpItem);
        
        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);
        
        javafx.scene.control.ToolBar toolBar = new javafx.scene.control.ToolBar();
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
        
        javafx.scene.control.SplitMenuButton btnPlay = new javafx.scene.control.SplitMenuButton();
        btnPlay.setText("Play Offline (Stubs)");
        btnPlay.setGraphic(new org.kordamp.ikonli.javafx.FontIcon("fas-plug"));
        btnPlay.getStyleClass().addAll("toolbar-btn", "btn-play");
        javafx.scene.control.MenuItem playDevItem = new javafx.scene.control.MenuItem("Play (Dev Services)");
        playDevItem.setGraphic(new org.kordamp.ikonli.javafx.FontIcon("fas-play"));
        javafx.scene.control.MenuItem playInfraItem = new javafx.scene.control.MenuItem("Play (My Own Infra)");
        playInfraItem.setGraphic(new org.kordamp.ikonli.javafx.FontIcon("fas-server"));
        btnPlay.getItems().addAll(playDevItem, playInfraItem);

        javafx.scene.control.Button btnStop = new javafx.scene.control.Button("Stop", new org.kordamp.ikonli.javafx.FontIcon("fas-stop"));
        btnStop.getStyleClass().addAll("toolbar-btn", "btn-stop");
        btnStop.setDisable(true);

        javafx.scene.control.Button btnExport = new javafx.scene.control.Button("Export", new org.kordamp.ikonli.javafx.FontIcon("fas-download"));
        btnExport.getStyleClass().addAll("toolbar-btn", "btn-export");
        
        javafx.scene.control.Button btnManual = new javafx.scene.control.Button("Manual", new org.kordamp.ikonli.javafx.FontIcon("fas-book"));
        btnManual.getStyleClass().addAll("toolbar-btn", "btn-manual");
        btnManual.setOnAction(e -> showManual());

        javafx.scene.control.Button btnInfraConfig = new javafx.scene.control.Button("Infra", new org.kordamp.ikonli.javafx.FontIcon("fas-cog"));
        btnInfraConfig.getStyleClass().addAll("toolbar-btn", "btn-config");
        btnInfraConfig.setOnAction(e -> {
            javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Infrastructure Configuration");
            dialog.setHeaderText("Define connection properties (SSL, mTLS, Kerberos, etc.)");
            javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);
            javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea();
            textArea.setPromptText("camel.component.kafka.brokers=my-cluster:9092\ncamel.ssl.trustStore=...");
            textArea.setPrefSize(500, 300);
            textArea.setStyle("-fx-font-family: monospace;");
            java.io.File infraFile = new java.io.File(System.getProperty("user.dir"), "infra.properties");
            if (infraFile.exists()) {
                try {
                    textArea.setText(java.nio.file.Files.readString(infraFile.toPath()));
                } catch (Exception ex) {}
            }
            dialog.getDialogPane().setContent(textArea);
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) return textArea.getText();
                return null;
            });
            dialog.showAndWait().ifPresent(configText -> {
                try {
                    java.nio.file.Files.writeString(infraFile.toPath(), configText);
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        });

        javafx.scene.control.Button btnVariables = new javafx.scene.control.Button("Variables", new org.kordamp.ikonli.javafx.FontIcon("fas-cube"));
        btnVariables.getStyleClass().addAll("toolbar-btn", "btn-variables");
        btnVariables.setTooltip(new javafx.scene.control.Tooltip("Open Workspace Properties / Variables"));
        btnVariables.setOnAction(e -> {
            java.io.File baseDir = treePane.getBaseDirectory();
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

        javafx.scene.control.Button btnDecrypt = new javafx.scene.control.Button("Decrypt", new org.kordamp.ikonli.javafx.FontIcon("fas-unlock-alt"));
        btnDecrypt.getStyleClass().addAll("toolbar-btn", "btn-decrypt");
        btnDecrypt.setOnAction(e -> DecryptToolWindow.show());

        javafx.scene.control.Button btnXsltMapper = new javafx.scene.control.Button("Map", new org.kordamp.ikonli.javafx.FontIcon("fas-map-marked-alt"));
        btnXsltMapper.getStyleClass().addAll("toolbar-btn", "btn-xslt");
        btnXsltMapper.setTooltip(new javafx.scene.control.Tooltip("Open Visual XSLT Data Mapper"));
        btnXsltMapper.setOnAction(e -> XsltMapperWindow.show());

        javafx.scene.control.Button btnTransform = new javafx.scene.control.Button("Transform", new org.kordamp.ikonli.javafx.FontIcon("fas-random"));
        btnTransform.getStyleClass().addAll("toolbar-btn", "btn-transform");
        btnTransform.setTooltip(new javafx.scene.control.Tooltip("Open Data Transformation Studio"));
        btnTransform.setOnAction(e -> {
            TransformationStudioWindow studio = new TransformationStudioWindow();
            studio.show();
        });

        javafx.scene.control.ComboBox<String> themeBox = new javafx.scene.control.ComboBox<>();
        themeBox.getItems().addAll("VSCode Dark", "IntelliJ Light", "Dracula", "Monokai", "Hacker");
        themeBox.setValue("VSCode Dark");
        themeBox.setTooltip(new javafx.scene.control.Tooltip("Change IDE Theme"));
        themeBox.setOnAction(e -> applyTheme(themeBox.getValue(), root));

        toolBar.getItems().addAll(btnViewExplorer, btnViewCode, btnViewDiagram, new javafx.scene.control.Separator(), btnSwapPanels, new javafx.scene.control.Separator(), btnPlay, btnStop, new javafx.scene.control.Separator(), btnInfraConfig, btnVariables, btnDecrypt, btnXsltMapper, btnTransform, btnExport, btnManual, new javafx.scene.control.Separator(), themeBox);

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

        // 1. Left Panel: Route Tree (Passes file to editor when clicked)
        treePane = new RouteTreePane(file -> {
            editorPane.loadFile(file);
        });

        HelpPortalPane helpPortalPane = new HelpPortalPane(() -> {
            viewHelpItem.setSelected(false);
        });

        searchBox.setOnAction(e -> {
            String query = searchBox.getText();
            viewHelpItem.setSelected(true);
            helpPortalPane.search(query);
        });

        Process[] runnerProcess = {null};

        java.util.function.BiConsumer<java.io.File, String> playProject = (target, mode) -> {
            boolean offline = "offline".equals(mode);
            boolean infra = "infra".equals(mode);
            btnPlay.setDisable(true);
            btnStop.setDisable(false);
            System.out.println("Starting Routes with JBang... (mode=" + mode + ", target=" + (target == null ? "all" : target.getName()) + ")");
            try {
                java.io.File baseDir = treePane.getBaseDirectory();
                
                String os = System.getProperty("os.name").toLowerCase();
                String jbangScript = os.contains("win") ? "jbang.cmd" : "jbang";
                java.io.File jbangExe = new java.io.File(System.getProperty("user.dir"), jbangScript);
                
                String executablePath = jbangExe.exists() ? jbangExe.getAbsolutePath() : "jbang";
                
                java.util.List<String> command = new java.util.ArrayList<>();
                command.add(executablePath);
                if (offline) command.add("--offline");
                command.add("camel@apache/camel");
                command.add("run");
                
                if (target == null) {
                    java.io.File[] files = baseDir.listFiles((d, name) -> name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".groovy"));
                    if (files != null && files.length > 0) {
                        for (java.io.File f : files) command.add(f.getName());
                    } else {
                        command.add("*");
                    }
                } else if (target.isFile()) {
                    try {
                        String relative = baseDir.toURI().relativize(target.toURI()).getPath();
                        command.add(relative.isEmpty() ? target.getName() : relative);
                    } catch (Exception ex) {
                        command.add(target.getAbsolutePath());
                    }
                } else { // Directory
                    java.util.List<String> collected = new java.util.ArrayList<>();
                    class FileCollector {
                        void collect(java.io.File folder, java.util.List<String> paths, java.io.File base) {
                            java.io.File[] files = folder.listFiles();
                            if (files != null) {
                                for (java.io.File f : files) {
                                    if (f.isDirectory()) {
                                        collect(f, paths, base);
                                    } else {
                                        String name = f.getName().toLowerCase();
                                        if (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".groovy")) {
                                            try {
                                                String relative = base.toURI().relativize(f.toURI()).getPath();
                                                paths.add(relative.isEmpty() ? f.getName() : relative);
                                            } catch (Exception ex) {
                                                paths.add(f.getAbsolutePath());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    new FileCollector().collect(target, collected, baseDir);
                    if (!collected.isEmpty()) {
                        command.addAll(collected);
                    } else {
                        command.add(target.getName() + "/*");
                    }
                }
                
                boolean dev = "dev".equals(mode);
                command.add("--runtime=quarkus");
                if (offline) command.add("--stub=all");
                if (dev) command.add("--profile=devservices");
                if (infra) {
                    command.add("--profile=dev");
                    java.io.File infraFile = new java.io.File(System.getProperty("user.dir"), "infra.properties");
                    if (infraFile.exists()) {
                        command.add("--properties=" + infraFile.getAbsolutePath());
                    }
                }
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.environment().put("QUARKUS_CONSOLE_COLOR", "true");
                pb.environment().put("TERM", "xterm-256color");
                pb.directory(baseDir);
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

        btnPlay.setOnAction(e -> playProject.accept(null, "offline"));
        playDevItem.setOnAction(e -> playProject.accept(null, "dev"));
        playInfraItem.setOnAction(e -> playProject.accept(null, "infra"));

        btnStop.setOnAction(e -> {
            btnPlay.setDisable(false);
            btnStop.setDisable(true);
            System.out.println("Stopping Routes...");
            if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
                runnerProcess[0].destroy();
                runnerProcess[0].descendants().forEach(ProcessHandle::destroyForcibly);
                runnerProcess[0] = null;
            }
        });

        btnExport.setOnAction(e -> {
            System.out.println("Exporting to Quarkus Maven Project...");
            try {
                java.io.File baseDir = treePane.getBaseDirectory();
                String os = System.getProperty("os.name").toLowerCase();
                String jbangScript = os.contains("win") ? "jbang.cmd" : "jbang";
                java.io.File jbangExe = new java.io.File(System.getProperty("user.dir"), jbangScript);
                String executablePath = jbangExe.exists() ? jbangExe.getAbsolutePath() : "jbang";
                
                java.util.List<String> command = new java.util.ArrayList<>();
                command.add(executablePath);
                command.add("camel@apache/camel");
                command.add("export");
                java.io.File[] files = baseDir.listFiles((d, name) -> name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".groovy"));
                if (files != null && files.length > 0) {
                    for (java.io.File f : files) command.add(f.getName());
                } else {
                    command.add("*");
                }
                command.add("--runtime=quarkus");
                command.add("--dir=export-dir");

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(baseDir);
                Process exportProcess = pb.start();
                showConsole(exportProcess, "Maven Export Output");
                System.out.println("Export triggered. Check the 'export-dir' in your project directory.");

                exportProcess.onExit().thenRun(() -> {
                    System.out.println("Export completed. Generating database changelogs...");
                    RouteChangelogGenerator.generate(baseDir, baseDir);
                    java.io.File exportDir = new java.io.File(baseDir, "export-dir");
                    if (exportDir.exists()) {
                        RouteChangelogGenerator.generate(baseDir, exportDir);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
                treePane.setBaseDirectory(selectedDirectory);
                generateChapterSamples(treePane, selectedDirectory);
                saveRecentProject(selectedDirectory.getAbsolutePath(), prefs, recentProjectsMenu, treePane);
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

        editorPane.setOnPlayFile((file, mode) -> {
            boolean offline = "offline".equals(mode);
            boolean infra = "infra".equals(mode);
            System.out.println("Starting Single File with JBang... (mode=" + mode + ")");
            try {
                java.io.File baseDir = file.getParentFile();
                String os = System.getProperty("os.name").toLowerCase();
                String jbangScript = os.contains("win") ? "jbang.cmd" : "jbang";
                java.io.File jbangExe = new java.io.File(System.getProperty("user.dir"), jbangScript);
                String executablePath = jbangExe.exists() ? jbangExe.getAbsolutePath() : "jbang";
                
                java.util.List<String> command = new java.util.ArrayList<>();
                command.add(executablePath);
                if (offline) command.add("--offline");
                command.add("camel@apache/camel");
                command.add("run");
                command.add(file.getName());
                boolean dev = "dev".equals(mode);
                command.add("--runtime=quarkus");
                if (offline) command.add("--stub=all");
                if (dev) command.add("--profile=devservices");
                if (infra) {
                    command.add("--profile=dev");
                    java.io.File infraFile = new java.io.File(System.getProperty("user.dir"), "infra.properties");
                    if (infraFile.exists()) {
                        command.add("--properties=" + infraFile.getAbsolutePath());
                    }
                }
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.environment().put("QUARKUS_CONSOLE_COLOR", "true");
                pb.environment().put("TERM", "xterm-256color");
                pb.directory(baseDir);
                Process singleProcess = pb.start();
                
                if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
                    runnerProcess[0].destroy();
                    runnerProcess[0].descendants().forEach(ProcessHandle::destroyForcibly);
                }
                runnerProcess[0] = singleProcess;
                showConsole(runnerProcess[0], "Single Route: " + file.getName());
                
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
        });

        // 3. Right Panel: Visual Diagram
        diagramPane = new DiagramPane(theme -> applyTheme(theme, root), updatedYaml -> {
            editorPane.setText(updatedYaml);
        });

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
        consolePane = new ConsolePane();

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

        Scene scene = new Scene(root, 1400, 800);
        
        // Load CSS
        String css = getClass().getResource("/styles/main.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        // Set a nice dark theme background
        scene.setFill(javafx.scene.paint.Color.web("#1e1e1e"));

        primaryStage.setTitle("Route Builder Studio");
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
        javafx.application.Platform.runLater(() -> applyTheme(themeBox.getValue(), root));
        
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

        try {
            java.io.File dir = new java.io.File(System.getProperty("user.dir"), "routes");
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

    private void updateDiagram(String yamlContent) {
        if (editorPane != null) {
            diagramPane.setCurrentFile(editorPane.getCurrentFile());
        }
        diagramPane.renderDiagram(yamlContent);
    }

    private void applyTheme(String theme, javafx.scene.layout.BorderPane root) {
        root.getStyleClass().removeAll("theme-vscode-dark", "theme-intellij-light", "theme-dracula", "theme-monokai", "theme-hacker");
        String cssClass = "theme-" + theme.toLowerCase().replace(" ", "-");
        currentThemeClass = cssClass;
        if (!cssClass.equals("theme-vscode-dark")) {
            root.getStyleClass().add(cssClass);
        }
        if (cssClass.equals("theme-intellij-light")) editorPane.setTheme("vs");
        else if (cssClass.equals("theme-hacker")) editorPane.setTheme("hc-black");
        else editorPane.setTheme("vs-dark");

        String baseColor = "#1e1e1e";
        String textColor = "#cccccc";
        if (cssClass.equals("theme-intellij-light")) { baseColor = "#ffffff"; textColor = "#333333"; }
        else if (cssClass.equals("theme-dracula")) { baseColor = "#282a36"; textColor = "#f8f8f2"; }
        else if (cssClass.equals("theme-monokai")) { baseColor = "#272822"; textColor = "#f8f8f2"; }
        else if (cssClass.equals("theme-hacker")) { baseColor = "#050505"; textColor = "#00ff00"; }
        
        try {
            javafx.scene.Scene currentScene = root.getScene();
            if (currentScene != null) {
                root.setStyle("");

                currentScene.getStylesheets().removeIf(s -> s.startsWith("data:text/css"));
                String keyCol = cssClass.equals("theme-intellij-light") ? "#0000ff" : "#9cdcfe";
                String strCol = cssClass.equals("theme-intellij-light") ? "#a31515" : "#ce9178";
                String numCol = cssClass.equals("theme-intellij-light") ? "#098658" : "#b5cea8";
                String tagCol = cssClass.equals("theme-intellij-light") ? "#800000" : "#569cd6";
                String editorBg = cssClass.equals("theme-intellij-light") ? "#ffffff" : "#1e1e1e";
                String editorFg = cssClass.equals("theme-intellij-light") ? "#333333" : "#d4d4d4";
                String borderCol = cssClass.equals("theme-intellij-light") ? "#cccccc" : "#444444";

                String menuBgCol = cssClass.equals("theme-intellij-light") ? "#f3f3f3" : "#252526";
                String menuBorderCol = cssClass.equals("theme-intellij-light") ? "#cccccc" : "#454545";
                String menuHoverCol = cssClass.equals("theme-intellij-light") ? "#e5e5e5" : "#094771";
                String menuTextCol = cssClass.equals("theme-intellij-light") ? "#333333" : "#cccccc";
                String menuTextHoverCol = cssClass.equals("theme-intellij-light") ? "#000000" : "#ffffff";

                String dynamicCss = 
                    ".root { -fx-base: " + baseColor + "; -fx-control-inner-background: " + baseColor + "; -fx-background-color: " + baseColor + "; -fx-text-base-color: " + textColor + "; -fx-text-background-color: " + textColor + "; -fx-text-fill: " + textColor + "; }\n" +
                    ".context-menu { -fx-background-color: " + menuBgCol + "; -fx-border-color: " + menuBorderCol + "; }\n" +
                    ".context-menu .menu-item:focused { -fx-background-color: " + menuHoverCol + "; }\n" +
                    ".context-menu .menu-item .label { -fx-text-fill: " + menuTextCol + "; }\n" +
                    ".context-menu .menu-item:focused .label { -fx-text-fill: " + menuTextHoverCol + "; }\n" +
                    ".context-menu .ikonli-font-icon { -fx-icon-color: " + menuTextCol + "; }\n" +
                    ".context-menu .menu-item:focused .ikonli-font-icon { -fx-icon-color: " + menuTextHoverCol + "; }\n" +
                    ".syntax-editor .text.syntax-key { -fx-fill: " + keyCol + "; }\n" +
                    ".syntax-editor .text.syntax-string { -fx-fill: " + strCol + "; }\n" +
                    ".syntax-editor .text.syntax-number { -fx-fill: " + numCol + "; }\n" +
                    ".syntax-editor .text.syntax-keyword { -fx-fill: " + keyCol + "; -fx-font-weight: bold; }\n" +
                    ".syntax-editor .text.syntax-tag { -fx-fill: " + tagCol + "; }\n" +
                    ".syntax-editor .text.syntax-attr { -fx-fill: " + keyCol + "; }\n" +
                    ".syntax-editor { -fx-font-family: 'Monospaced'; -fx-font-size: 13px; -fx-background-color: " + editorBg + "; -fx-border-color: " + borderCol + "; -fx-border-radius: 3px; }\n" +
                    ".syntax-editor .text { -fx-fill: " + editorFg + "; }";
                String dataUri = "data:text/css;charset=utf-8," + java.net.URLEncoder.encode(dynamicCss, "UTF-8").replace("+", "%20");
                currentDynamicCssUri = dataUri;
                currentScene.getStylesheets().add(dataUri);

                for (javafx.scene.Parent themedRoot : themedRoots) {
                    themedRoot.getStyleClass().removeAll("theme-vscode-dark", "theme-intellij-light", "theme-dracula", "theme-monokai", "theme-hacker");
                    if (!cssClass.equals("theme-vscode-dark")) {
                        themedRoot.getStyleClass().add(cssClass);
                    }
                    javafx.scene.Scene scene = themedRoot.getScene();
                    if (scene != null) {
                        scene.getStylesheets().removeIf(s -> s.startsWith("data:text/css"));
                        scene.getStylesheets().add(currentDynamicCssUri);
                    }
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
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
        // helper to create a subfolder then write a file into it
        java.util.function.BiConsumer<String[], String> write = (path, content) -> {
            java.io.File dir = base;
            for (int i = 0; i < path.length - 1; i++) dir = new java.io.File(dir, path[i]);
            dir.mkdirs();
            treePane.createTemplateFileInDir(dir, path[path.length - 1], content);
        };

        // Write application.properties to root of sample project
        String propsContent = 
            "# Decoupled URIs for Chapter Samples\n" +
            "wiretap.audit.uri=stub:jms:queue:audit.trail\n" +
            "kafka.orders.uri=stub:kafka:topic:orders\n" +
            "ibmmq.request.uri=stub:jms:queue:REQUEST.Q\n" +
            "mongodb.orders.uri=stub:mongodb:cameldb?operation=insert\n" +
            "dlq.uri=stub:jms:queue:DLQ\n";
        treePane.createTemplateFileInDir(base, "application.properties", propsContent);

        // ── Chapter 1: Basics ────────────────────────────────────────────────
        write.accept(new String[]{"chapter-01-basics", "01-hello-timer.camel.yaml"},
            "# ID: hello-timer\n# ENABLED: true\n# DESCRIPTION: Simplest possible route — timer fires every 3s and logs a message\n# AUTHOR: Camel Studio\n# STUB: none\n\n" +
            "- route:\n    id: \"hello-timer\"\n    from:\n      uri: \"timer:hello?period=3000\"\n      steps:\n        - log:\n            message: \"[Chapter 1] Hello from Apache Camel! Timestamp: ${date:now:HH:mm:ss}\"\n");

        write.accept(new String[]{"chapter-01-basics", "02-set-body-header.camel.yaml"},
            "# ID: set-body-header\n# ENABLED: true\n# DESCRIPTION: Demonstrates setBody and setHeader EIPs\n# AUTHOR: Camel Studio\n# STUB: none\n\n" +
            "- route:\n    id: \"set-body-header\"\n    from:\n      uri: \"timer:trigger?period=5000&repeatCount=3\"\n      steps:\n        - setHeader:\n            name: \"MessageType\"\n            constant: \"ORDER\"\n        - setBody:\n            constant: '{\"orderId\":\"ORD-001\",\"amount\":199.99,\"currency\":\"USD\"}'\n        - log:\n            message: \"[Chapter 1] Header: ${header.MessageType} | Body: ${body}\"\n        - to: \"mock:output\"\n");

        write.accept(new String[]{"chapter-01-basics", "03-simple-expression.camel.yaml"},
            "# ID: simple-expression\n# ENABLED: true\n# DESCRIPTION: Uses Simple language to conditionally transform the message body\n# AUTHOR: Camel Studio\n# STUB: none\n\n" +
            "- route:\n    id: \"simple-expression\"\n    from:\n      uri: \"timer:tick?period=4000\"\n      steps:\n        - setHeader:\n            name: \"counter\"\n            groovy: \"(exchange.getProperty('CamelTimerCounter', Integer.class) ?: 0)\"\n        - setBody:\n            simple: \"Request #${exchangeProperty.CamelTimerCounter} processed at ${date:now:HH:mm:ss}\"\n        - log:\n            message: \"[Chapter 1] ${body}\"\n        - to: \"mock:sink\"\n");

        // ── Chapter 2: Content-Based Routing ─────────────────────────────────
        write.accept(new String[]{"chapter-02-routing", "01-choice-routing.camel.yaml"},
            "# ID: choice-routing\n# ENABLED: true\n# DESCRIPTION: Content-Based Router pattern using Choice/When/Otherwise EIPs\n# AUTHOR: Camel Studio\n# STUB: none\n\n" +
            "- route:\n    id: \"cbr-injector\"\n    from:\n      uri: \"timer:cbr?period=2000&repeatCount=6\"\n      steps:\n        - setBody:\n            simple: \"${random(1,4)}\"\n        - log: \"[Chapter 2] Routing message with type: ${body}\"\n        - to: \"direct:cbr-router\"\n\n" +
            "- route:\n    id: \"cbr-router\"\n    from:\n      uri: \"direct:cbr-router\"\n      steps:\n        - choice:\n            when:\n              - simple: \"${body} == '1'\"\n                steps:\n                  - log: \"[Chapter 2] >> Priority GOLD order\"\n                  - to: \"mock:gold\"\n              - simple: \"${body} == '2'\"\n                steps:\n                  - log: \"[Chapter 2] >> Priority SILVER order\"\n                  - to: \"mock:silver\"\n              - simple: \"${body} == '3'\"\n                steps:\n                  - log: \"[Chapter 2] >> Priority BRONZE order\"\n                  - to: \"mock:bronze\"\n            otherwise:\n              steps:\n                - log: \"[Chapter 2] >> Unknown priority, rejecting\"\n                - to: \"mock:rejected\"\n");

        write.accept(new String[]{"chapter-02-routing", "02-wiretap-audit.camel.yaml"},
            "# ID: wiretap-audit\n# ENABLED: true\n# DESCRIPTION: WireTap pattern — async audit copy without slowing main flow\n# AUTHOR: Camel Studio\n# STUB: mock (stub replaces JMS audit queue)\n\n" +
            "- route:\n    id: \"main-payment-flow\"\n    from:\n      uri: \"timer:payment?period=3000&repeatCount=4\"\n      steps:\n        - setBody:\n            constant: '{\"txId\":\"TX-${random(1000,9999)}\",\"amount\":500}'\n        - log: \"[Chapter 2] Processing payment: ${body}\"\n        - wireTap:\n            uri: \"{{wiretap.audit.uri}}\"\n        - to: \"mock:payment-processed\"\n\n" +
            "- route:\n    id: \"audit-consumer\"\n    from:\n      uri: \"{{wiretap.audit.uri}}\"\n      steps:\n        - log: \"[Chapter 2][AUDIT] Captured audit event: ${body}\"\n        - to: \"mock:audit-log\"\n");

        write.accept(new String[]{"chapter-02-routing", "03-filter-eip.camel.yaml"},
            "# ID: filter-eip\n# ENABLED: true\n# DESCRIPTION: Message Filter EIP — only passes messages matching the predicate\n# AUTHOR: Camel Studio\n# STUB: none\n\n" +
            "- route:\n    id: \"filter-high-value\"\n    from:\n      uri: \"timer:orders?period=2500\"\n      steps:\n        - setBody:\n            simple: \"${random(100,1000)}\"\n        - log: \"[Chapter 2] Order amount: ${body}\"\n        - filter:\n            simple: \"${body} > 500\"\n            steps:\n              - log: \"[Chapter 2][FILTER] High-value order detected: ${body}\"\n              - to: \"mock:high-value\"\n");

        // ── Chapter 3: Messaging Systems (Stubs) ─────────────────────────────
        write.accept(new String[]{"chapter-03-messaging", "01-kafka-consumer.camel.yaml"},
            "# ID: kafka-consumer-stub\n# ENABLED: true\n# DESCRIPTION: Consumes messages from a Kafka topic. Uses stub: in offline mode.\n# STUB: stub:kafka:topic:orders (replaces kafka:orders?brokers=localhost:9092)\n# REAL_URI: kafka:orders?brokers=localhost:9092&groupId=camel-studio\n\n" +
            "- route:\n    id: \"kafka-order-consumer\"\n    from:\n      uri: \"{{kafka.orders.uri}}\"\n      steps:\n        - log: \"[Chapter 3][KAFKA] Received order from Kafka: ${body}\"\n        - setHeader:\n            name: \"ProcessedAt\"\n            simple: \"${date:now:yyyy-MM-dd HH:mm:ss}\"\n        - to: \"mock:order-processor\"\n\n" +
            "- route:\n    id: \"kafka-order-publisher\"\n    from:\n      uri: \"timer:kafka-test?period=4000&repeatCount=5\"\n      steps:\n        - setBody:\n            simple: '{\"orderId\":\"ORD-${random(100,999)}\",\"product\":\"Widget\",\"qty\":${random(1,10)}}'\n        - log: \"[Chapter 3][KAFKA] Sending to Kafka: ${body}\"\n        - to: \"{{kafka.orders.uri}}\"\n");

        write.accept(new String[]{"chapter-03-messaging", "02-jms-ibmmq-stub.camel.yaml"},
            "# ID: jms-ibmmq-stub\n# ENABLED: true\n# DESCRIPTION: IBM MQ / JMS request-reply pattern using stubs for offline testing\n# STUB: stub:jms:queue:REQUEST.Q and stub:jms:queue:REPLY.Q\n# REAL_URI: jms:queue:REQUEST.Q?connectionFactory=#ibmMQFactory\n\n" +
            "- route:\n    id: \"ibmmq-request-sender\"\n    from:\n      uri: \"timer:mq-test?period=5000&repeatCount=4\"\n      steps:\n        - setBody:\n            constant: '<TransactionRequest><TxId>TXN-001</TxId><Amount>1000.00</Amount></TransactionRequest>'\n        - setHeader:\n            name: \"JMSCorrelationID\"\n            simple: \"CID-${random(10000,99999)}\"\n        - log: \"[Chapter 3][IBM-MQ] Sending to request queue: ${header.JMSCorrelationID}\"\n        - to: \"{{ibmmq.request.uri}}\"\n\n" +
            "- route:\n    id: \"ibmmq-request-handler\"\n    from:\n      uri: \"{{ibmmq.request.uri}}\"\n      steps:\n        - log: \"[Chapter 3][IBM-MQ] Processing request: ${body}\"\n        - setBody:\n            constant: '<TransactionResponse><Status>APPROVED</Status><Auth>AUTH-789</Auth></TransactionResponse>'\n        - to: \"stub:jms:queue:REPLY.Q\"\n\n" +
            "- route:\n    id: \"ibmmq-reply-consumer\"\n    from:\n      uri: \"stub:jms:queue:REPLY.Q\"\n      steps:\n        - log: \"[Chapter 3][IBM-MQ] Got reply: ${body}\"\n        - to: \"mock:mq-completed\"\n");

        write.accept(new String[]{"chapter-03-messaging", "03-mongodb-stub.camel.yaml"},
            "# ID: mongodb-stub\n# ENABLED: true\n# DESCRIPTION: Saves and retrieves documents from MongoDB using stubs offline\n# STUB: stub:mongodb:cameldb (replaces mongodb:cameldb?operation=insert&collection=orders)\n# REAL_URI: mongodb:myMongoBean?database=cameldb&collection=orders&operation=insert\n\n" +
            "- route:\n    id: \"mongodb-insert\"\n    from:\n      uri: \"timer:mongo?period=4000&repeatCount=4\"\n      steps:\n        - setBody:\n            simple: '{\"_id\":\"${random(1000,9999)}\",\"customer\":\"ACME Corp\",\"amount\":${random(100,5000)},\"ts\":\"${date:now:yyyy-MM-dd}\"}'\n        - log: \"[Chapter 3][MONGO] Inserting document: ${body}\"\n        - to: \"{{mongodb.orders.uri}}\"\n        - log: \"[Chapter 3][MONGO] Insert complete, result: ${body}\"\n        - to: \"mock:mongo-inserted\"\n");

        // ── Chapter 4: Error Handling ─────────────────────────────────────────
        write.accept(new String[]{"chapter-04-error-handling", "01-global-exception.camel.yaml"},
            "# ID: global-exception-handler\n# ENABLED: true\n# DESCRIPTION: Global onException with retry, exponential backoff, and dead-letter channel\n# AUTHOR: Camel Studio\n# STUB: stub:jms:queue:DLQ (dead letter queue)\n\n" +
            "- onException:\n    exception:\n      - \"java.io.IOException\"\n      - \"java.net.ConnectException\"\n    redeliveryPolicy:\n      maximumRedeliveries: 3\n      redeliveryDelay: 1000\n      backOffMultiplier: 2.0\n      useExponentialBackOff: true\n    handled:\n      constant: true\n    steps:\n      - log:\n          loggingLevel: ERROR\n          message: \"[Chapter 4][DLQ] Routing to dead-letter after ${header.CamelRedeliveryCounter} retries: ${exception.message}\"\n      - to: \"{{dlq.uri}}\"\n\n" +
            "- route:\n    id: \"flaky-downstream\"\n    from:\n      uri: \"timer:errors?period=3000&repeatCount=5\"\n      steps:\n        - setBody:\n            simple: \"Payload-${random(1,100)}\"\n        - log: \"[Chapter 4] Attempting to send: ${body}\"\n        - to: \"{{dlq.uri}}\"\n        - log: \"[Chapter 4] Sent successfully\"\n");

        write.accept(new String[]{"chapter-04-error-handling", "02-dotry-docatch.camel.yaml"},
            "# ID: dotry-docatch\n# ENABLED: true\n# DESCRIPTION: Inline doTry/doCatch/doFinally for local exception scoping\n# AUTHOR: Camel Studio\n# STUB: none\n\n" +
            "- route:\n    id: \"dotry-example\"\n    from:\n      uri: \"timer:try?period=4000&repeatCount=4\"\n      steps:\n        - setBody:\n            simple: \"${random(1,10)}\"\n        - doTry:\n            steps:\n              - log: \"[Chapter 4][TRY] Processing value: ${body}\"\n              - filter:\n                  simple: \"${body} > 7\"\n                  steps:\n                    - throwException:\n                        exceptionType: \"java.lang.RuntimeException\"\n                        message: \"Value too large: ${body}\"\n              - log: \"[Chapter 4][TRY] Success: ${body}\"\n              - to: \"mock:success\"\n            doCatch:\n              - exception:\n                  - \"java.lang.RuntimeException\"\n                steps:\n                  - log: \"[Chapter 4][CATCH] Caught exception: ${exception.message}\"\n                  - to: \"mock:caught\"\n            doFinally:\n              steps:\n                - log: \"[Chapter 4][FINALLY] Cleanup complete\"\n");

        write.accept(new String[]{"chapter-04-error-handling", "03-circuit-breaker.camel.yaml"},
            "# ID: circuit-breaker\n# ENABLED: true\n# DESCRIPTION: Circuit Breaker pattern protecting against downstream failures with fallback\n# AUTHOR: Camel Studio\n# STUB: stub:http:api.example.com/orders (replaces live HTTP call)\n\n" +
            "- route:\n    id: \"circuit-breaker-demo\"\n    from:\n      uri: \"timer:cb-test?period=3000&repeatCount=6\"\n      steps:\n        - log: \"[Chapter 4][CB] Calling downstream service...\"\n        - circuitBreaker:\n            steps:\n              - setHeader:\n                  name: \"CamelHttpMethod\"\n                  constant: \"GET\"\n              - to: \"stub:http:api.example.com/orders\"\n              - log: \"[Chapter 4][CB] Success: ${body}\"\n              - to: \"mock:cb-success\"\n            onFallback:\n              steps:\n                - log: \"[Chapter 4][CB] Circuit OPEN — returning cached fallback\"\n                - setBody:\n                    constant: '{\"orders\":[],\"source\":\"cache\",\"status\":\"degraded\"}'\n                - to: \"mock:cb-fallback\"\n");

        // ── Chapter 5: Transformation ─────────────────────────────────────────
        write.accept(new String[]{"chapter-05-transformation", "01-enrich-pattern.camel.yaml"},
            "# ID: enrich-pattern\n# ENABLED: true\n# DESCRIPTION: Content Enricher pattern — fetch extra data and merge into original message\n# AUTHOR: Camel Studio\n# STUB: stub:direct:fetch-customer (replaces real DB/API call)\n\n" +
            "- route:\n    id: \"order-enricher\"\n    from:\n      uri: \"timer:enrich?period=4000&repeatCount=4\"\n      steps:\n        - setBody:\n            constant: '{\"orderId\":\"ORD-123\",\"customerId\":\"CUST-42\"}'\n        - log: \"[Chapter 5][ENRICH] Original order: ${body}\"\n        - enrich:\n            expression:\n              constant: \"stub:direct:fetch-customer\"\n            aggregationStrategy: \"#class:org.apache.camel.processor.aggregate.GroupedBodyAggregationStrategy\"\n        - log: \"[Chapter 5][ENRICH] Enriched result: ${body}\"\n        - to: \"mock:enriched\"\n\n" +
            "- route:\n    id: \"customer-service-stub\"\n    from:\n      uri: \"stub:direct:fetch-customer\"\n      steps:\n        - log: \"[Chapter 5][ENRICH] Fetching customer for order: ${body}\"\n        - setBody:\n            constant: '{\"customerId\":\"CUST-42\",\"name\":\"ACME Corp\",\"tier\":\"GOLD\",\"creditLimit\":50000}'\n");

        write.accept(new String[]{"chapter-05-transformation", "02-split-aggregate.camel.yaml"},
            "# ID: split-aggregate\n# ENABLED: true\n# DESCRIPTION: Splitter + Aggregator pattern — split batch, process items, recombine\n# AUTHOR: Camel Studio\n# STUB: none\n\n" +
            "- route:\n    id: \"batch-splitter\"\n    from:\n      uri: \"timer:batch?period=6000&repeatCount=3\"\n      steps:\n        - setBody:\n            constant: \"item1,item2,item3,item4,item5\"\n        - log: \"[Chapter 5][SPLIT] Splitting batch: ${body}\"\n        - split:\n            simple: \"${body}\"\n            delimiter: \",\"\n            steps:\n              - log: \"[Chapter 5][SPLIT] Processing item: ${body}\"\n              - setBody:\n                  simple: \"PROCESSED:${body}:${date:now:HH:mm:ss}\"\n              - to: \"mock:split-item\"\n");

        write.accept(new String[]{"chapter-05-transformation", "03-multicast.camel.yaml"},
            "# ID: multicast\n# ENABLED: true\n# DESCRIPTION: Multicast EIP — broadcast same message to multiple endpoints in parallel\n# AUTHOR: Camel Studio\n# STUB: stub:jms:queue:NOTIFY.EMAIL, stub:jms:queue:NOTIFY.SMS\n\n" +
            "- route:\n    id: \"multicast-notifier\"\n    from:\n      uri: \"timer:notify?period=5000&repeatCount=3\"\n      steps:\n        - setBody:\n            constant: '{\"event\":\"PAYMENT_COMPLETE\",\"txId\":\"TX-12345\",\"amount\":499.99}'\n        - log: \"[Chapter 5][MULTICAST] Broadcasting notification: ${body}\"\n        - multicast:\n            parallelProcessing: true\n            steps:\n              - to: \"stub:jms:queue:NOTIFY.EMAIL\"\n              - to: \"stub:jms:queue:NOTIFY.SMS\"\n              - to: \"mock:audit-trail\"\n");

        // ── Chapter 6: REST APIs ──────────────────────────────────────────────
        write.accept(new String[]{"chapter-06-rest-api", "01-rest-producer.camel.yaml"},
            "# ID: rest-producer\n# ENABLED: true\n# DESCRIPTION: Calls an external REST API using the http component (stubbed for offline)\n# AUTHOR: Camel Studio\n# STUB: stub:http:api.example.com (replaces real HTTP)\n\n" +
            "- route:\n    id: \"rest-api-caller\"\n    from:\n      uri: \"timer:http?period=5000&repeatCount=4\"\n      steps:\n        - setHeader:\n            name: \"CamelHttpMethod\"\n            constant: \"GET\"\n        - setHeader:\n            name: \"Accept\"\n            constant: \"application/json\"\n        - log: \"[Chapter 6][REST] Calling external API...\"\n        - to: \"stub:http:api.example.com/api/v1/products\"\n        - log: \"[Chapter 6][REST] API Response: ${body}\"\n        - to: \"mock:api-result\"\n");

        write.accept(new String[]{"chapter-06-rest-api", "02-rest-consumer.camel.yaml"},
            "# ID: rest-consumer\n# ENABLED: true\n# DESCRIPTION: Exposes REST endpoints using Camel REST DSL (Quarkus platform)\n# AUTHOR: Camel Studio\n# STUB: none (self-hosted REST server)\n\n" +
            "- rest:\n    path: \"/api/v1\"\n    post:\n      - path: \"/orders\"\n        consumes: \"application/json\"\n        produces: \"application/json\"\n        to: \"direct:create-order\"\n    get:\n      - path: \"/orders/{id}\"\n        produces: \"application/json\"\n        to: \"direct:get-order\"\n    delete:\n      - path: \"/orders/{id}\"\n        to: \"direct:delete-order\"\n\n" +
            "- route:\n    id: \"create-order\"\n    from:\n      uri: \"direct:create-order\"\n      steps:\n        - log: \"[Chapter 6][REST] POST /orders - Body: ${body}\"\n        - setHeader:\n            name: \"orderId\"\n            simple: \"ORD-${random(1000,9999)}\"\n        - setBody:\n            simple: '{\"orderId\":\"${header.orderId}\",\"status\":\"CREATED\",\"timestamp\":\"${date:now:yyyy-MM-dd HH:mm:ss}\"}'\n\n" +
            "- route:\n    id: \"get-order\"\n    from:\n      uri: \"direct:get-order\"\n      steps:\n        - log: \"[Chapter 6][REST] GET /orders/${header.id}\"\n        - setBody:\n            simple: '{\"orderId\":\"${header.id}\",\"status\":\"ACTIVE\",\"amount\":299.99}'\n\n" +
            "- route:\n    id: \"delete-order\"\n    from:\n      uri: \"direct:delete-order\"\n      steps:\n        - log: \"[Chapter 6][REST] DELETE /orders/${header.id}\"\n        - setBody:\n            simple: '{\"orderId\":\"${header.id}\",\"status\":\"DELETED\"}'\n");

        // ── Chapter 7: Enterprise Patterns ────────────────────────────────────
        write.accept(new String[]{"chapter-07-enterprise", "01-saga-pattern.camel.yaml"},
            "# ID: saga-pattern\n# ENABLED: true\n# DESCRIPTION: Saga EIP for distributed transaction coordination with compensation\n# AUTHOR: Camel Studio\n# STUB: stub:direct:book-hotel, stub:direct:book-flight, stub:direct:charge-payment\n\n" +
            "- route:\n    id: \"saga-travel-booking\"\n    from:\n      uri: \"timer:saga?period=6000&repeatCount=3\"\n      steps:\n        - setBody:\n            constant: '{\"tripId\":\"TRIP-001\",\"customer\":\"John Doe\",\"destination\":\"Paris\"}'\n        - log: \"[Chapter 7][SAGA] Starting travel booking saga: ${body}\"\n        - saga:\n            compensation: \"direct:cancel-booking\"\n            completion: \"direct:confirm-booking\"\n            steps:\n              - to: \"stub:direct:book-hotel\"\n              - log: \"[Chapter 7][SAGA] Hotel booked\"\n              - to: \"stub:direct:book-flight\"\n              - log: \"[Chapter 7][SAGA] Flight booked\"\n              - to: \"stub:direct:charge-payment\"\n              - log: \"[Chapter 7][SAGA] Payment charged — saga complete\"\n\n" +
            "- route:\n    id: \"cancel-booking\"\n    from:\n      uri: \"direct:cancel-booking\"\n      steps:\n        - log: \"[Chapter 7][SAGA][COMPENSATE] Cancelling booking: ${body}\"\n        - to: \"mock:booking-cancelled\"\n\n" +
            "- route:\n    id: \"confirm-booking\"\n    from:\n      uri: \"direct:confirm-booking\"\n      steps:\n        - log: \"[Chapter 7][SAGA][COMPLETE] Booking confirmed: ${body}\"\n        - to: \"mock:booking-confirmed\"\n");

        write.accept(new String[]{"chapter-07-enterprise", "02-transactional-outbox.camel.yaml"},
            "# ID: transactional-outbox\n# ENABLED: true\n# DESCRIPTION: Transactional Outbox pattern — poll DB outbox table and publish to Kafka\n# AUTHOR: Camel Studio\n# STUB: stub:kafka:topic:OUTBOX.EVENTS (replaces real Kafka)\n\n" +
            "- route:\n    id: \"outbox-poller\"\n    from:\n      uri: \"timer:outbox-poll?period=5000\"\n      steps:\n        - log: \"[Chapter 7][OUTBOX] Polling outbox table for unpublished events...\"\n        - setBody:\n            constant: '[{\"eventId\":\"EVT-001\",\"type\":\"ORDER_CREATED\",\"payload\":\"{}\",\"status\":\"PENDING\"}]'\n        - split:\n            jsonpath: \"$[*]\"\n            steps:\n              - log: \"[Chapter 7][OUTBOX] Publishing event: ${body}\"\n              - to: \"stub:kafka:topic:OUTBOX.EVENTS\"\n              - log: \"[Chapter 7][OUTBOX] Event published, marking as SENT\"\n              - to: \"mock:outbox-sent\"\n");

        write.accept(new String[]{"chapter-07-enterprise", "03-dead-letter-channel.camel.yaml"},
            "# ID: dead-letter-channel\n# ENABLED: true\n# DESCRIPTION: Dead Letter Channel pattern — capture poison messages after max retries\n# AUTHOR: Camel Studio\n# STUB: stub:jms:queue:DLQ, stub:jms:queue:MAIN.QUEUE\n\n" +
            "- onException:\n    exception:\n      - \"java.lang.Exception\"\n    redeliveryPolicy:\n      maximumRedeliveries: 3\n      redeliveryDelay: 500\n    handled:\n      constant: true\n    steps:\n      - log:\n          loggingLevel: ERROR\n          message: \"[Chapter 7][DLC] Dead-lettering message after retries. Cause: ${exception.message}\"\n      - setHeader:\n          name: \"X-DLQ-Reason\"\n          simple: \"${exception.message}\"\n      - setHeader:\n          name: \"X-DLQ-Timestamp\"\n          simple: \"${date:now:yyyy-MM-dd HH:mm:ss}\"\n      - to: \"stub:jms:queue:DLQ\"\n\n" +
            "- route:\n    id: \"main-processor\"\n    from:\n      uri: \"stub:jms:queue:MAIN.QUEUE\"\n      steps:\n        - log: \"[Chapter 7][DLC] Processing message: ${body}\"\n        - filter:\n            simple: \"${body} contains 'POISON'\"\n            steps:\n              - throwException:\n                  exceptionType: \"java.lang.RuntimeException\"\n                  message: \"Poison message detected\"\n        - to: \"mock:processed\"\n\n" +
            "- route:\n    id: \"dlq-monitor\"\n    from:\n      uri: \"stub:jms:queue:DLQ\"\n      steps:\n        - log: \"[Chapter 7][DLC][DLQ] Received dead-letter: ${body} | Reason: ${header.X-DLQ-Reason}\"\n        - to: \"mock:dlq-received\"\n");


        // ── Chapter 8: HTTP REST Server (Actually Runnable) ───────────────────
        // All files in this chapter start a real Quarkus HTTP server on :8080
        // when run with JBang. Use: curl http://localhost:8080/...

        write.accept(new String[]{"chapter-08-rest-server", "01-platform-http-hello.camel.yaml"},
            "# ID: platform-http-hello\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: Simplest live HTTP endpoint using platform-http component.\n" +
            "#   Starts a real server on port 8080. Run this file then call:\n" +
            "#     curl http://localhost:8080/hello\n" +
            "#     curl \"http://localhost:8080/hello?name=Pratyush\"\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: none — live HTTP server\n\n" +
            "- route:\n" +
            "    id: \"http-hello\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/hello\"\n" +
            "      parameters:\n" +
            "        httpMethodRestrict: \"GET\"\n" +
            "      steps:\n" +
            "        - setBody:\n" +
            "            simple: \"Hello, ${header.name}! Welcome to Camel REST on Quarkus.\"\n" +
            "        - log: \"[Chapter 8][HELLO] Served: ${body}\"\n" +
            "        - setHeader:\n" +
            "            name: \"Content-Type\"\n" +
            "            constant: \"text/plain\"\n");

        write.accept(new String[]{"chapter-08-rest-server", "02-rest-dsl-crud.camel.yaml"},
            "# ID: rest-dsl-crud\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: Full CRUD REST API using the Camel REST DSL.\n" +
            "#   Starts a live HTTP server on port 8080.\n" +
            "#\n" +
            "#   CREATE:  curl -X POST http://localhost:8080/api/orders \\\n" +
            "#              -H 'Content-Type: application/json' \\\n" +
            "#              -d '{\"product\":\"Widget\",\"qty\":5}'\n" +
            "#\n" +
            "#   READ:    curl http://localhost:8080/api/orders/ORD-001\n" +
            "#\n" +
            "#   UPDATE:  curl -X PUT http://localhost:8080/api/orders/ORD-001 \\\n" +
            "#              -H 'Content-Type: application/json' \\\n" +
            "#              -d '{\"qty\":10}'\n" +
            "#\n" +
            "#   DELETE:  curl -X DELETE http://localhost:8080/api/orders/ORD-001\n" +
            "#\n" +
            "#   LIST:    curl http://localhost:8080/api/orders\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: none — live HTTP server\n\n" +
            "- rest:\n" +
            "    path: \"/api/orders\"\n" +
            "    consumes: \"application/json\"\n" +
            "    produces: \"application/json\"\n" +
            "    get:\n" +
            "      - path: \"/\"\n" +
            "        description: \"List all orders\"\n" +
            "        to: \"direct:list-orders\"\n" +
            "      - path: \"/{id}\"\n" +
            "        description: \"Get order by ID\"\n" +
            "        to: \"direct:get-order\"\n" +
            "    post:\n" +
            "      - path: \"/\"\n" +
            "        description: \"Create a new order\"\n" +
            "        to: \"direct:create-order\"\n" +
            "    put:\n" +
            "      - path: \"/{id}\"\n" +
            "        description: \"Update an order\"\n" +
            "        to: \"direct:update-order\"\n" +
            "    delete:\n" +
            "      - path: \"/{id}\"\n" +
            "        description: \"Delete an order\"\n" +
            "        to: \"direct:delete-order\"\n\n" +
            "- route:\n" +
            "    id: \"list-orders\"\n" +
            "    from:\n" +
            "      uri: \"direct:list-orders\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 8][CRUD] GET /api/orders\"\n" +
            "        - setBody:\n" +
            "            constant: '[{\"id\":\"ORD-001\",\"product\":\"Widget\",\"qty\":5,\"status\":\"ACTIVE\"},{\"id\":\"ORD-002\",\"product\":\"Gadget\",\"qty\":2,\"status\":\"PENDING\"}]'\n\n" +
            "- route:\n" +
            "    id: \"get-order\"\n" +
            "    from:\n" +
            "      uri: \"direct:get-order\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 8][CRUD] GET /api/orders/${header.id}\"\n" +
            "        - setBody:\n" +
            "            simple: '{\"id\":\"${header.id}\",\"product\":\"Widget\",\"qty\":5,\"status\":\"ACTIVE\",\"createdAt\":\"${date:now:yyyy-MM-dd HH:mm:ss\"}'\n\n" +
            "- route:\n" +
            "    id: \"create-order\"\n" +
            "    from:\n" +
            "      uri: \"direct:create-order\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 8][CRUD] POST /api/orders body=${body}\"\n" +
            "        - setHeader:\n" +
            "            name: \"newOrderId\"\n" +
            "            simple: \"ORD-${random(1000,9999)}\"\n" +
            "        - setBody:\n" +
            "            simple: '{\"id\":\"${header.newOrderId}\",\"status\":\"CREATED\",\"createdAt\":\"${date:now:yyyy-MM-dd HH:mm:ss\"}'\n" +
            "        - setHeader:\n" +
            "            name: \"CamelHttpResponseCode\"\n" +
            "            constant: \"201\"\n\n" +
            "- route:\n" +
            "    id: \"update-order\"\n" +
            "    from:\n" +
            "      uri: \"direct:update-order\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 8][CRUD] PUT /api/orders/${header.id} body=${body}\"\n" +
            "        - setBody:\n" +
            "            simple: '{\"id\":\"${header.id}\",\"status\":\"UPDATED\",\"updatedAt\":\"${date:now:yyyy-MM-dd HH:mm:ss\"}'\n\n" +
            "- route:\n" +
            "    id: \"delete-order\"\n" +
            "    from:\n" +
            "      uri: \"direct:delete-order\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 8][CRUD] DELETE /api/orders/${header.id}\"\n" +
            "        - setBody:\n" +
            "            simple: '{\"id\":\"${header.id}\",\"status\":\"DELETED\"}'\n" +
            "        - setHeader:\n" +
            "            name: \"CamelHttpResponseCode\"\n" +
            "            constant: \"200\"\n");

        write.accept(new String[]{"chapter-08-rest-server", "03-rest-auth-header.camel.yaml"},
            "# ID: rest-auth-header\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: REST endpoint that validates an API-Key header before serving.\n" +
            "#   curl -H 'X-API-Key: secret123' http://localhost:8080/secure/data\n" +
            "#   curl http://localhost:8080/secure/data   <- returns 401\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: none — live HTTP server\n\n" +
            "- route:\n" +
            "    id: \"secure-endpoint\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/secure/data\"\n" +
            "      parameters:\n" +
            "        httpMethodRestrict: \"GET\"\n" +
            "      steps:\n" +
            "        - choice:\n" +
            "            when:\n" +
            "              - simple: \"${header.X-API-Key} == 'secret123'\"\n" +
            "                steps:\n" +
            "                  - log: \"[Chapter 8][AUTH] Authorized request\"\n" +
            "                  - setBody:\n" +
            "                      constant: '{\"data\":\"Sensitive payload\",\"clearance\":\"TOP_SECRET\"}'\n" +
            "                  - setHeader:\n" +
            "                      name: \"Content-Type\"\n" +
            "                      constant: \"application/json\"\n" +
            "            otherwise:\n" +
            "              steps:\n" +
            "                - log: \"[Chapter 8][AUTH] Unauthorized — missing or wrong API key\"\n" +
            "                - setHeader:\n" +
            "                    name: \"CamelHttpResponseCode\"\n" +
            "                    constant: \"401\"\n" +
            "                - setBody:\n" +
            "                    constant: '{\"error\":\"Unauthorized\",\"message\":\"Valid X-API-Key header required\"}'\n");

        write.accept(new String[]{"chapter-08-rest-server", "04-rest-json-transform.camel.yaml"},
            "# ID: rest-json-transform\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: REST endpoint that accepts a raw payment object and transforms it\n" +
            "#   to a canonical format using setBody + Simple expressions.\n" +
            "#\n" +
            "#   curl -X POST http://localhost:8080/api/payments \\\n" +
            "#     -H 'Content-Type: application/json' \\\n" +
            "#     -d '{\"sender\":\"Alice\",\"receiver\":\"Bob\",\"amount\":250.00,\"currency\":\"USD\"}'\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: none — live HTTP server\n\n" +
            "- route:\n" +
            "    id: \"payment-transformer\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/api/payments\"\n" +
            "      parameters:\n" +
            "        httpMethodRestrict: \"POST\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 8][PAY] Received payment: ${body}\"\n" +
            "        - unmarshal:\n" +
            "            json:\n" +
            "              library: Jackson\n" +
            "        - setHeader:\n" +
            "            name: \"txRef\"\n" +
            "            simple: \"TXN-${random(100000,999999)}\"\n" +
            "        - setHeader:\n" +
            "            name: \"Content-Type\"\n" +
            "            constant: \"application/json\"\n" +
            "        - marshal:\n" +
            "            json:\n" +
            "              library: Jackson\n" +
            "        - log: \"[Chapter 8][PAY] Canonical payment dispatched: ref=${header.txRef}\"\n" +
            "        - setBody:\n" +
            "            simple: '{\"txRef\":\"${header.txRef}\",\"status\":\"ACCEPTED\",\"timestamp\":\"${date:now:yyyy-MM-dd HH:mm:ss\"}'\n" +
            "        - setHeader:\n" +
            "            name: \"CamelHttpResponseCode\"\n" +
            "            constant: \"202\"\n");

        write.accept(new String[]{"chapter-08-rest-server", "05-rest-proxy-gateway.camel.yaml"},
            "# ID: rest-proxy-gateway\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: API Gateway pattern — receives HTTP requests and proxies them to a\n" +
            "#   downstream service (stubbed here). Adds correlation ID, auth header, logging.\n" +
            "#\n" +
            "#   curl http://localhost:8080/gateway/products\n" +
            "#   curl http://localhost:8080/gateway/products/42\n" +
            "# STUB: stub:http:downstream-service (replaces real downstream)\n" +
            "# AUTHOR: Camel Studio\n\n" +
            "- route:\n" +
            "    id: \"api-gateway\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/gateway/{resource}\"\n" +
            "      steps:\n" +
            "        - setHeader:\n" +
            "            name: \"X-Correlation-ID\"\n" +
            "            simple: \"COR-${random(100000,999999)}\"\n" +
            "        - setHeader:\n" +
            "            name: \"X-Gateway-Timestamp\"\n" +
            "            simple: \"${date:now:yyyy-MM-dd HH:mm:ss}\"\n" +
            "        - log: \"[Chapter 8][GW] Routing ${header.CamelHttpMethod} /${header.resource} — CorID: ${header.X-Correlation-ID}\"\n" +
            "        - circuitBreaker:\n" +
            "            steps:\n" +
            "              - setHeader:\n" +
            "                  name: \"Authorization\"\n" +
            "                  constant: \"Bearer internal-service-token\"\n" +
            "              - to: \"stub:http:downstream-service/api/${header.resource}\"\n" +
            "              - log: \"[Chapter 8][GW] Downstream responded: ${body}\"\n" +
            "            onFallback:\n" +
            "              steps:\n" +
            "                - log: \"[Chapter 8][GW] Downstream unreachable — returning 503\"\n" +
            "                - setHeader:\n" +
            "                    name: \"CamelHttpResponseCode\"\n" +
            "                    constant: \"503\"\n" +
            "                - setBody:\n" +
            "                    constant: '{\"error\":\"Service Unavailable\",\"retryAfter\":30}'\n" +
            "        - setHeader:\n" +
            "            name: \"Content-Type\"\n" +
            "            constant: \"application/json\"\n");

        write.accept(new String[]{"chapter-08-rest-server", "06-rest-streaming-sse.camel.yaml"},
            "# ID: rest-streaming-sse\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: Server-Sent Events (SSE) style streaming endpoint.\n" +
            "#   Pushes a stream of price-tick events to the caller every second.\n" +
            "#\n" +
            "#   curl -N http://localhost:8080/stream/prices\n" +
            "#\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: none — live HTTP server\n\n" +
            "- route:\n" +
            "    id: \"price-feed-producer\"\n" +
            "    from:\n" +
            "      uri: \"timer:price-tick?period=1000\"\n" +
            "      steps:\n" +
            "        - setBody:\n" +
            "            simple: \"data: {\\\"symbol\\\":\\\"EUR/USD\\\",\\\"price\\\":\\\"${random(10800,10900)}\\\",\\\"ts\\\":\\\"${date:now:HH:mm:ss}\\\"}\\n\\n\"\n" +
            "        - to: \"direct:price-broadcaster\"\n\n" +
            "- route:\n" +
            "    id: \"sse-endpoint\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/stream/prices\"\n" +
            "      parameters:\n" +
            "        httpMethodRestrict: \"GET\"\n" +
            "      steps:\n" +
            "        - setHeader:\n" +
            "            name: \"Content-Type\"\n" +
            "            constant: \"text/event-stream\"\n" +
            "        - setHeader:\n" +
            "            name: \"Cache-Control\"\n" +
            "            constant: \"no-cache\"\n" +
            "        - setBody:\n" +
            "            constant: \"data: {\\\"message\\\":\\\"Connected to Camel price stream\\\"}\\n\\n\"\n" +
            "        - log: \"[Chapter 8][SSE] Client connected to price stream\"\n");

        write.accept(new String[]{"chapter-08-rest-server", "07-rest-file-upload.camel.yaml"},
            "# ID: rest-file-upload\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: Multipart file upload endpoint — receives a file and saves it.\n" +
            "#\n" +
            "#   curl -X POST http://localhost:8080/upload \\\n" +
            "#     -F 'file=@/path/to/yourfile.xml'\n" +
            "#\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: none — live HTTP server\n\n" +
            "- route:\n" +
            "    id: \"file-upload-handler\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/upload\"\n" +
            "      parameters:\n" +
            "        httpMethodRestrict: \"POST\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 8][UPLOAD] Received upload — Content-Type: ${header.Content-Type}\"\n" +
            "        - choice:\n" +
            "            when:\n" +
            "              - simple: \"${body} != null\"\n" +
            "                steps:\n" +
            "                  - setHeader:\n" +
            "                      name: \"CamelFileName\"\n" +
            "                      simple: \"upload-${date:now:yyyyMMdd-HHmmss}.bin\"\n" +
            "                  - to: \"file:uploads\"\n" +
            "                  - log: \"[Chapter 8][UPLOAD] Saved file: ${header.CamelFileName}\"\n" +
            "                  - setBody:\n" +
            "                      simple: '{\"status\":\"uploaded\",\"file\":\"${header.CamelFileName}\",\"size\":\"${body.length}\"}'\n" +
            "            otherwise:\n" +
            "              steps:\n" +
            "                - setHeader:\n" +
            "                    name: \"CamelHttpResponseCode\"\n" +
            "                    constant: \"400\"\n" +
            "                - setBody:\n" +
            "                    constant: '{\"error\":\"No file received\"}'\n" +
            "        - setHeader:\n" +
            "            name: \"Content-Type\"\n" +
            "            constant: \"application/json\"\n");

        write.accept(new String[]{"chapter-08-rest-server", "08-rest-health-metrics.camel.yaml"},
            "# ID: rest-health-metrics\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: Health-check and metrics endpoints (standard in microservices).\n" +
            "#\n" +
            "#   curl http://localhost:8080/health\n" +
            "#   curl http://localhost:8080/health/live\n" +
            "#   curl http://localhost:8080/health/ready\n" +
            "#   curl http://localhost:8080/metrics\n" +
            "#\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: none — live HTTP server\n\n" +
            "- route:\n" +
            "    id: \"health-overall\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/health\"\n" +
            "      parameters:\n" +
            "        httpMethodRestrict: \"GET\"\n" +
            "      steps:\n" +
            "        - setHeader:\n" +
            "            name: \"Content-Type\"\n" +
            "            constant: \"application/json\"\n" +
            "        - setBody:\n" +
            "            simple: '{\"status\":\"UP\",\"timestamp\":\"${date:now:yyyy-MM-dd HH:mm:ss}\",\"components\":{\"camel\":{\"status\":\"UP\"},\"db\":{\"status\":\"UP\"}}}'\n\n" +
            "- route:\n" +
            "    id: \"health-live\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/health/live\"\n" +
            "      parameters:\n" +
            "        httpMethodRestrict: \"GET\"\n" +
            "      steps:\n" +
            "        - setHeader:\n" +
            "            name: \"Content-Type\"\n" +
            "            constant: \"application/json\"\n" +
            "        - setBody:\n" +
            "            constant: '{\"status\":\"UP\"}'\n\n" +
            "- route:\n" +
            "    id: \"health-ready\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/health/ready\"\n" +
            "      parameters:\n" +
            "        httpMethodRestrict: \"GET\"\n" +
            "      steps:\n" +
            "        - setHeader:\n" +
            "            name: \"Content-Type\"\n" +
            "            constant: \"application/json\"\n" +
            "        - setBody:\n" +
            "            constant: '{\"status\":\"UP\",\"checks\":[{\"name\":\"camel-context\",\"status\":\"UP\"}]}'\n\n" +
            "- route:\n" +
            "    id: \"metrics-endpoint\"\n" +
            "    from:\n" +
            "      uri: \"platform-http:/metrics\"\n" +
            "      parameters:\n" +
            "        httpMethodRestrict: \"GET\"\n" +
            "      steps:\n" +
            "        - setHeader:\n" +
            "            name: \"Content-Type\"\n" +
            "            constant: \"text/plain\"\n" +
            "        - setBody:\n" +
            "            simple: \"# HELP camel_routes_running_routes Number of running routes\\n# TYPE camel_routes_running_routes gauge\\ncamel_routes_running_routes 4\\n# HELP camel_exchanges_total Total exchanges processed\\n# TYPE camel_exchanges_total counter\\ncamel_exchanges_total ${exchangeProperty.CamelTimerCounter}\\n\"\n");

        // ── Chapter 9: Cryptography & Auditing ──────────────────────────────
        write.accept(new String[]{"chapter-09-crypto-audit", "01-audit-stream-exclude.camel.yaml"},
            "# ID: mongo-audit-exclude\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: MongoDB Change Stream audit consumer demonstrating exclusion rules and host metadata resolution.\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: stub:mongodb:audit (replaces real database connection)\n\n" +
            "- route:\n" +
            "    id: \"mongo-change-stream-audit\"\n" +
            "    from:\n" +
            "      uri: \"stub:mongodb:myDb?consumerType=changeStream&database=audit&collection=transactions\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 9][AUDIT] Captured mutation payload: ${body}\"\n" +
            "        - to: \"mock:filtered-audit\"\n");

        write.accept(new String[]{"chapter-09-crypto-audit", "02-aes-gcm-encrypt-config.camel.yaml"},
            "# ID: aes-gcm-properties\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: Demonstrates decryption of credentials in properties files using PBKDF2 GCM key derivation.\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: none\n\n" +
            "- route:\n" +
            "    id: \"secure-properties-loader\"\n" +
            "    from:\n" +
            "      uri: \"timer:secure-props?period=5000&repeatCount=2\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 9] Database Username: ${properties:db.username:admin}\"\n" +
            "        - log: \"[Chapter 9] Encrypted Password resolved: ${properties:db.password:ENC(g21A/98s/8da12k=)}\"\n");

        write.accept(new String[]{"chapter-09-crypto-audit", "03-ibmmq-xa-narayana.camel.yaml"},
            "# ID: ibmmq-xa-narayana\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: Demonstrates IBM MQ Jakarta 9.4.5.1 Client configuration with JmsPoolXAConnectionFactory.\n" +
            "# AUTHOR: Camel Studio\n" +
            "# STUB: stub:jms:queue:XA.REQUEST\n\n" +
            "- route:\n" +
            "    id: \"ibmmq-xa-consumer\"\n" +
            "    from:\n" +
            "      uri: \"stub:jms:queue:XA.REQUEST?cacheLevelName=CACHE_NONE&transacted=false\"\n" +
            "      steps:\n" +
            "        - log: \"[Chapter 9][XA] Coordinated transaction message: ${body}\"\n" +
            "        - to: \"mock:xa-processed\"\n");

        // ── Chapter 10: Beans & Java DSL ─────────────────────────────────────
        write.accept(new String[]{"chapter-10-beans-java", "MyServiceBean.java"},
            "package com.example;\n\n" +
            "public class MyServiceBean {\n" +
            "    private String prefix = \"DEFAULT\";\n\n" +
            "    public void setPrefix(String prefix) {\n" +
            "        this.prefix = prefix;\n" +
            "    }\n\n" +
            "    public String formatMessage(String body) {\n" +
            "        return \"[\" + prefix + \"] >> \" + body;\n" +
            "    }\n" +
            "}\n");

        write.accept(new String[]{"chapter-10-beans-java", "01-yaml-beans.camel.yaml"},
            "# ID: yaml-beans-example\n" +
            "# ENABLED: true\n" +
            "# DESCRIPTION: Declares local beans in YAML and invokes them in a Camel route step\n" +
            "# AUTHOR: Camel Studio\n\n" +
            "- beans:\n" +
            "    - name: myBean\n" +
            "      type: com.example.MyServiceBean\n" +
            "      properties:\n" +
            "        prefix: \"YAML-BEAN-LOG\"\n\n" +
            "- route:\n" +
            "    id: \"yaml-bean-route\"\n" +
            "    from:\n" +
            "      uri: \"timer:yaml-bean?period=3000&repeatCount=3\"\n" +
            "      steps:\n" +
            "        - setBody:\n" +
            "            constant: \"Message from YAML DSL\"\n" +
            "        - bean:\n" +
            "            ref: \"myBean\"\n" +
            "            method: \"formatMessage\"\n" +
            "        - log:\n" +
            "            message: \"Result: ${body}\"\n");

        write.accept(new String[]{"chapter-10-beans-java", "02-java-route.java"},
            "// camel-k: language=java\n" +
            "//DEPS org.apache.camel:camel-timer:4.20.0\n\n" +
            "import org.apache.camel.builder.RouteBuilder;\n" +
            "import org.apache.camel.BindToRegistry;\n\n" +
            "public class MyBeanRoute extends RouteBuilder {\n\n" +
            "    @Override\n" +
            "    public void configure() throws Exception {\n" +
            "        from(\"timer:java-bean?period=4000&repeatCount=3\")\n" +
            "            .setBody().constant(\"Message from Java DSL\")\n" +
            "            .bean(\"myJavaFormatter\", \"format\")\n" +
            "            .log(\"Result: ${body}\");\n" +
            "    }\n\n" +
            "    @BindToRegistry(\"myJavaFormatter\")\n" +
            "    public static class JavaFormatter {\n" +
            "        public String format(String input) {\n" +
            "            return \"[JAVA-BEAN-LOG] \" + input;\n" +
            "        }\n" +
            "    }\n" +
            "}\n");

        treePane.refresh();
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
}
