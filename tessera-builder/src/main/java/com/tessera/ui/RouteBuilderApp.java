package com.tessera.ui;

// import com.tessera.ui.VisualDataMapperWindow; // removed
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
        if (file == null)
            return new FontIcon("fas-file");
        if (file.isDirectory())
            return new FontIcon("fas-folder");

        String name = file.getName().toLowerCase();
        if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            try {
                // Peek first 4KB for performance
                String content = "";
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                    long len = Math.min(raf.length(), 4096);
                    byte[] bytes = new byte[(int) len];
                    raf.readFully(bytes);
                    content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).toLowerCase();
                    content = content.replaceAll("#\\s+type:", "#type:");
                }

                if (content.contains("#type: db") || content.contains("#type: database")) {
                    FontIcon icon = new FontIcon("fas-database");
                    icon.setIconColor(javafx.scene.paint.Color.web("#3498db"));
                    return icon;
                }
                if (content.contains("#type: oracle")) {
                    FontIcon icon = new FontIcon("fas-database");
                    icon.setIconColor(javafx.scene.paint.Color.web("#e74c3c"));
                    return icon;
                }
                if (content.contains("#type: bean")) {
                    FontIcon icon = new FontIcon("fas-seedling");
                    icon.setIconColor(javafx.scene.paint.Color.web("#8bc34a"));
                    return icon;
                }
                if (content.contains("#type: sql")) {
                    FontIcon icon = new FontIcon("fas-table");
                    icon.setIconColor(javafx.scene.paint.Color.web("#9b59b6"));
                    return icon;
                }
                if (content.contains("#type: activemq") || content.contains("#type: jms")
                        || content.contains("#type: mq")) {
                    FontIcon icon = new FontIcon("fas-envelope");
                    icon.setIconColor(javafx.scene.paint.Color.web("#e67e22"));
                    return icon;
                }
                if (content.contains("#type: kafka")) {
                    FontIcon icon = new FontIcon("fas-bolt");
                    icon.setIconColor(javafx.scene.paint.Color.web("#E91E63"));
                    return icon;
                }
                if (content.contains("#type: solace")) {
                    FontIcon icon = new FontIcon("fas-broadcast-tower");
                    icon.setIconColor(javafx.scene.paint.Color.web("#00c895"));
                    return icon;
                }
                if (content.contains("#type: rabbitmq") || content.contains("#type: rabbit")) {
                    FontIcon icon = new FontIcon("fas-paw");
                    icon.setIconColor(javafx.scene.paint.Color.web("#ff6600"));
                    return icon;
                }
                if (content.contains("#type: redis") || content.contains("#type: cache")) {
                    FontIcon icon = new FontIcon("fas-memory");
                    icon.setIconColor(javafx.scene.paint.Color.web("#d32f2f"));
                    return icon;
                }
                if (content.contains("#type: aws") || content.contains("#type: cloud")) {
                    FontIcon icon = new FontIcon("fas-cloud");
                    icon.setIconColor(javafx.scene.paint.Color.web("#ff9900"));
                    return icon;
                }
                if (content.contains("#type: rest") || content.contains("#type: api")
                        || content.contains("#type: http")) {
                    FontIcon icon = new FontIcon("fas-globe");
                    icon.setIconColor(javafx.scene.paint.Color.web("#2ecc71"));
                    return icon;
                }

                if (content.contains("kafka")) {
                    FontIcon icon = new FontIcon("fas-kafka");
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

                if (content.contains("camel") || content.contains("route:") || content.contains("from:")) {
                    FontIcon icon = new FontIcon("fas-route");
                    icon.setIconColor(javafx.scene.paint.Color.web("#FF9800"));
                    return icon;
                }
            } catch (Exception ignored) {
            }
            return new FontIcon("fas-file-signature");
        }

        if (name.endsWith(".xml") || name.endsWith(".template") || name.endsWith(".txt")) {
            try {
                String content = "";
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                    long len = Math.min(raf.length(), 2048);
                    byte[] bytes = new byte[(int) len];
                    raf.readFully(bytes);
                    content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).toLowerCase();
                }
                if (content.contains("{{type:")) {
                    FontIcon icon = new FontIcon("fas-magic");
                    icon.setIconColor(javafx.scene.paint.Color.web("#E040FB"));
                    return icon;
                }
            } catch (Exception ignored) {
            }
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
        if (name.endsWith(".csv"))
            return new FontIcon("fas-file-csv");
        if (name.endsWith(".bpmn"))
            return new FontIcon("fas-project-diagram");

        return new FontIcon("fas-file");
    }

    public YamlEditorPane editorPane;
    private DiagramPane diagramPane;
    private RouteTreePane treePane;
    private com.tessera.lsp.LspManager lspManager;
    private com.tessera.ui.components.ConsolePane consolePane;
    private HelpPortalPane helpPortalPane;
    private javafx.stage.Stage helpPortalStage;
    private javafx.scene.control.Button btnPlay;
    private javafx.scene.control.Button btnStop;
    private javafx.scene.control.CheckMenuItem mongoSimItem;
    private javafx.scene.control.CheckMenuItem oracleSimItem;
    private final Process[] runnerProcess = { null };

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
        if (instance != null)
            instance.updateInternalThemes(theme);
    }

    private void updateInternalThemes(String theme) {
        if (diagramPane != null)
            diagramPane.setTheme(theme);
        if (helpPortalPane != null)
            helpPortalPane.setTheme(theme);
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

        // Pipe combined stdout/stderr — read raw bytes so \r and mid-line ANSI
        // sequences are preserved
        new Thread(() -> pipeStream(process, process.getInputStream()), "console-combined-stream").start();
    }

    private void pipeStream(Process process, java.io.InputStream stream) {
        byte[] buf = new byte[2048];
        int n;
        try {
            while ((n = stream.read(buf)) != -1) {
                // Decode using the platform charset (UTF-8 for Camel Main / JBang)
                final String chunk = new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8);
                if (consolePane != null)
                    consolePane.log(chunk);
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
                            consolePane.log("\n\033[1;31m[Route Builder Studio] Process exited with code " + exitCode
                                    + "\033[0m\n");
                        }
                    }
                } catch (Exception ignored) {
                }
                javafx.application.Platform.runLater(() -> {
                    if (btnStop != null)
                        btnStop.setDisable(true);
                    if (editorPane != null && editorPane.getBtnStopFile() != null) {
                        editorPane.getBtnStopFile().setDisable(true);
                    }
                    if (btnPlay != null && treePane != null) {
                        btnPlay.setStyle(""); // Clear running opacity override
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
        if (btnPlay != null && treePane != null) {
            btnPlay.setStyle(""); // Remove running state opacity override
            boolean hasChecked = !treePane.getCheckedFiles().isEmpty();
            boolean hasSelected = treePane.getTreeView().getSelectionModel().getSelectedItem() != null;
            btnPlay.setDisable(!hasChecked && !hasSelected);
        }
    }

    public void setRunnerProcess(Process p) {
        this.runnerProcess[0] = p;
        if (btnStop != null) {
            btnStop.setDisable(false);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            javafx.scene.image.Image appIcon = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/logo.png"));
            primaryStage.getIcons().add(appIcon);
        } catch (Exception ignored) {
        }

        // --- Splash Screen Phase ---
        Stage splashStage = new Stage(javafx.stage.StageStyle.UNDECORATED);
        try {
            splashStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/logo.png")));
        } catch (Exception ignored) {
        }

        javafx.scene.web.WebView splashWebView = new javafx.scene.web.WebView();
        splashWebView.setPrefSize(800, 600);

        String splashContent = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Tessera Animated Logo</title>
                  <style>
                    body {
                      margin: 0;
                      min-height: 100vh;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      background-color: #f4f7f9;
                      overflow: hidden;
                    }
                    .logo-container {
                      width: 100%;
                      max-width: 500px;
                      padding: 2rem;
                    }
                    /* Core Setup & Performance Optimization */
                    .animated-element {
                      backface-visibility: hidden;
                      will-change: transform, opacity;
                    }
                    /* Base states (Hidden and positioned outward) */
                    .piece-tl { transform: translate(-350px, -350px) rotate(-60deg); opacity: 0; }
                    .piece-tr { transform: translate(350px, -350px) rotate(60deg); opacity: 0; }
                    .piece-bl { transform: translate(-350px, 350px) rotate(-60deg); opacity: 0; }
                    .piece-br { transform: translate(350px, 350px) rotate(60deg); opacity: 0; }
                    .tri-left { transform: translate(-180px, 20px); opacity: 0; }
                    .tri-right { transform: translate(180px, 20px); opacity: 0; }
                    .text-title { transform: translateY(35px); opacity: 0; }
                    .text-sub { transform: translateY(35px); opacity: 0; }
                    /* Trigger classes */
                    .play-animation .piece-tl { animation: assembleTL 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
                    .play-animation .piece-tr { animation: assembleTR 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
                    .play-animation .piece-bl { animation: assembleBL 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
                    .play-animation .piece-br { animation: assembleBR 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
                    .play-animation .tri-left { animation: slideTriLeft 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.2s forwards; }
                    .play-animation .tri-right { animation: slideTriRight 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.2s forwards; }
                    .play-animation .text-title { animation: textFadeUp 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.5s forwards; }
                    .play-animation .text-sub { animation: textFadeUp 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.75s forwards; }
                    /* Keyframes */
                    @keyframes assembleTL { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }
                    @keyframes assembleTR { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }
                    @keyframes assembleBL { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }
                    @keyframes assembleBR { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }
                    @keyframes slideTriLeft { 100% { transform: translate(0, 0); opacity: 1; } }
                    @keyframes slideTriRight { 100% { transform: translate(0, 0); opacity: 1; } }
                    @keyframes textFadeUp { 100% { transform: translateY(0); opacity: 1; } }
                  </style>
                </head>
                <body>
                  <div class="logo-container">
                    <svg id="tessera-logo" xmlns="http://www.w3.org/2000/svg" viewBox="190 30 620 560" width="100%" height="100%">
                      <defs>
                        <linearGradient id="topBlueGrad" x1="-50" y1="50" x2="50" y2="-50" gradientUnits="userSpaceOnUse">
                          <stop offset="49.8%" stop-color="#3A8DB5" />
                          <stop offset="50.2%" stop-color="#64ACD0" />
                        </linearGradient>
                        <linearGradient id="bottomDarkGrad" x1="-50" y1="50" x2="50" y2="-50" gradientUnits="userSpaceOnUse">
                          <stop offset="49.8%" stop-color="#0E505E" />
                          <stop offset="50.2%" stop-color="#176E7D" />
                        </linearGradient>
                        <filter id="pieceShadow" x="-30%" y="-30%" width="160%" height="160%">
                          <feDropShadow dx="3" dy="6" stdDeviation="5" flood-color="#002233" flood-opacity="0.18" />
                        </filter>
                      </defs>
                      <g transform="translate(500, 215) scale(1.1)">
                        <g stroke-width="12" stroke-linejoin="round">
                          <path class="animated-element tri-left" d="M -120 45 L -55 110 L -185 110 Z" fill="#136070" stroke="#136070" />
                          <path class="animated-element tri-right" d="M 120 45 L 185 110 L 55 110 Z" fill="#207886" stroke="#207886" />
                        </g>
                        <g transform="rotate(45)" stroke-linejoin="round">
                          <path class="animated-element piece-br" d="M 100 100 L 4 100 L 4 62 L -2 62 C -6 72, -20 68, -20 50 C -20 32, -6 28, -2 38 L 4 38 L 4 4 L 38 4 L 38 -2 C 28 -6, 32 -20, 50 -20 C 68 -20, 72 -6, 62 -2 L 62 4 L 100 4 Z" fill="url(#bottomDarkGrad)" filter="url(#pieceShadow)" />
                          <path class="animated-element piece-bl" d="M -100 100 L -100 4 L -62 4 L -62 -2 C -72 -6, -68 -20, -50 -20 C -32 -20, -28 -6, -38 -2 L -38 4 L -4 4 L -4 38 L -10 38 C -14 28, -28 32, -28 50 C -28 68, -14 72, -10 62 L -4 62 L -4 100 Z" fill="#136070" filter="url(#pieceShadow)" />
                          <path class="animated-element piece-tr" d="M 100 -100 L 100 -4 L 62 -4 L 62 -10 C 72 -14, 68 -28, 50 -28 C 32 -28, 28 -14, 38 -10 L 38 -4 L 4 -4 L 4 -38 L -2 -38 C -6 -28, -20 -32, -20 -50 C -20 -68, -6 -72, -2 -62 L 4 -62 L 4 -100 Z" fill="#F1A463" filter="url(#pieceShadow)" />
                          <path class="animated-element piece-tl" d="M -100 -100 L -4 -100 L -4 -62 L -10 -62 C -14 -72, -28 -68, -28 -50 C -28 -32, -14 -28, -10 -38 L -4 -38 L -4 -4 L -38 -4 L -38 -10 C -28 -14, -32 -28, -50 -28 C -68 -28, -72 -14, -62 -10 L -62 -4 L -100 -4 Z" fill="url(#topBlueGrad)" filter="url(#pieceShadow)" />
                        </g>
                      </g>
                      <text class="animated-element text-title" x="500" y="470" font-family="'Segoe UI', 'Montserrat', sans-serif" font-weight="900" font-size="82" fill="#333333" text-anchor="middle" letter-spacing="6">TESSERA</text>
                      <text class="animated-element text-sub" x="500" y="525" font-family="'Segoe UI', 'Montserrat', sans-serif" font-weight="400" font-size="26" fill="#4B555A" text-anchor="middle" letter-spacing="0.5">The foundational tiles of enterprise architecture</text>
                    </svg>
                  </div>
                  <script>
                    document.addEventListener("DOMContentLoaded", function() {
                      setTimeout(function() {
                        document.getElementById('tessera-logo').classList.add('play-animation');
                      }, 100);
                    });
                  </script>
                </body>
                </html>
                """;

        splashWebView.getEngine().loadContent(splashContent);
        splashStage.setScene(new javafx.scene.Scene(splashWebView, 800, 600));
        splashStage.centerOnScreen();
        splashStage.show();

        instance = this;

        // --- Initialization ---
        loadWorkspaceProperties();
        lspManager = new com.tessera.lsp.LspManager();
        lspManager.start();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        rootNode = root;

        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(1.5));
        delay.setOnFinished(event -> {
            splashStage.close();
            primaryStage.show();
        });

        setupMainWindow(primaryStage, root);
        delay.play();
    }

    private void setupMainWindow(Stage primaryStage, BorderPane root) {
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

        newMenu.getItems().addAll(newProjectItem, newSampleProjectItem, newFileItem,
                new javafx.scene.control.SeparatorMenuItem(),
                newKameletItem, newComponentItem, newProcessorItem, new javafx.scene.control.SeparatorMenuItem(),
                newYamlDslItem, newJavaDslItem, newXmlDslItem, newGroovyDslItem, newKotlinDslItem, newTransformMenu);

        javafx.scene.control.MenuItem openItem = new javafx.scene.control.MenuItem("Open Folder...");
        javafx.scene.control.Menu recentProjectsMenu = new javafx.scene.control.Menu("Recent Projects");
        javafx.scene.control.MenuItem exitItem = new javafx.scene.control.MenuItem("Exit");
        exitItem.setOnAction(e -> javafx.application.Platform.exit());

        javafx.scene.control.MenuItem saveItem = new javafx.scene.control.MenuItem("Save");
        saveItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(e -> {
            if (editorPane != null)
                editorPane.saveFile();
        });

        javafx.scene.control.MenuItem saveAllItem = new javafx.scene.control.MenuItem("Save All");
        saveAllItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S,
                javafx.scene.input.KeyCombination.CONTROL_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        saveAllItem.setOnAction(e -> {
            if (editorPane != null)
                editorPane.saveAllFiles();
        });

        fileMenu.getItems().addAll(newMenu, openItem, saveItem, saveAllItem, recentProjectsMenu,
                new javafx.scene.control.SeparatorMenuItem(), exitItem);

        javafx.scene.control.Menu editMenu = new javafx.scene.control.Menu("_Edit");
        javafx.scene.control.MenuItem undoItem = new javafx.scene.control.MenuItem("Undo");
        javafx.scene.control.MenuItem redoItem = new javafx.scene.control.MenuItem("Redo");
        javafx.scene.control.MenuItem cutItem = new javafx.scene.control.MenuItem("Cut");
        javafx.scene.control.MenuItem copyItem = new javafx.scene.control.MenuItem("Copy");
        javafx.scene.control.MenuItem pasteItem = new javafx.scene.control.MenuItem("Paste");
        javafx.scene.control.MenuItem selectAllItem = new javafx.scene.control.MenuItem("Select All");

        undoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Z,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        redoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Z,
                javafx.scene.input.KeyCombination.CONTROL_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        cutItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.X,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        copyItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.C,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        pasteItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.V,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        selectAllItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.A,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));

        javafx.scene.control.MenuItem cryptoItem = new javafx.scene.control.MenuItem("Crypto Studio...");
        cryptoItem.setOnAction(e -> CryptoStudioWindow.show());

        editMenu.getItems().addAll(undoItem, redoItem, new javafx.scene.control.SeparatorMenuItem(), cutItem, copyItem,
                pasteItem, new javafx.scene.control.SeparatorMenuItem(), cryptoItem, selectAllItem);

        javafx.scene.control.Menu viewMenu = new javafx.scene.control.Menu("_View");
        javafx.scene.control.CheckMenuItem viewExplorerItem = new javafx.scene.control.CheckMenuItem(
                "Project Explorer");
        viewExplorerItem.setSelected(true);
        javafx.scene.control.CheckMenuItem viewCodeItem = new javafx.scene.control.CheckMenuItem("Code Editor");
        viewCodeItem.setSelected(true);
        javafx.scene.control.CheckMenuItem viewDiagramItem = new javafx.scene.control.CheckMenuItem("Diagram Canvas");
        viewDiagramItem.setSelected(true);
        javafx.scene.control.MenuItem resetLayoutItem = new javafx.scene.control.MenuItem("Reset Layout");
        javafx.scene.control.MenuItem swapLayoutItem = new javafx.scene.control.MenuItem("Swap Code and Diagram");
        viewMenu.getItems().addAll(viewExplorerItem, viewCodeItem, viewDiagramItem,
                new javafx.scene.control.SeparatorMenuItem(), swapLayoutItem, resetLayoutItem);

        javafx.scene.control.Menu helpMenu = new javafx.scene.control.Menu("_Help");
        javafx.scene.control.MenuItem maxItem = new javafx.scene.control.MenuItem("Maximize Window");
        maxItem.setOnAction(e -> primaryStage.setMaximized(true));
        javafx.scene.control.MenuItem restoreItem = new javafx.scene.control.MenuItem("Restore Window");
        restoreItem.setOnAction(e -> primaryStage.setMaximized(false));

        javafx.scene.control.MenuItem helpGuideItem = new javafx.scene.control.MenuItem("Open Help Guide...",
                new org.kordamp.ikonli.javafx.FontIcon("fas-question-circle"));
        helpGuideItem.setOnAction(e -> new RouteBuilderHelpWindow().show());

        javafx.scene.control.MenuItem interactiveHelpItem = new javafx.scene.control.MenuItem("Interactive Help Portal...",
                new org.kordamp.ikonli.javafx.FontIcon("fas-book"));
        interactiveHelpItem.setOnAction(e -> {
            if (helpPortalStage == null) {
                helpPortalStage = new javafx.stage.Stage();
                helpPortalStage.setTitle("Tessera Interactive Help Portal");
                javafx.scene.Scene scene = new javafx.scene.Scene(helpPortalPane, 900, 650);
                scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
                helpPortalStage.setScene(scene);
                com.tessera.ui.components.ThemeManager.registerRoot(helpPortalPane);
                
                // Set app-root and current theme class so CSS applies correctly to the new stage
                helpPortalPane.getStyleClass().addAll("app-root", currentThemeClass);
            }
            helpPortalStage.show();
            helpPortalStage.toFront();
        });

        javafx.scene.control.MenuItem aboutItem = new javafx.scene.control.MenuItem("About Tessera...",
                new org.kordamp.ikonli.javafx.FontIcon("fas-info-circle"));
        aboutItem.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().addAll(maxItem, restoreItem, new javafx.scene.control.SeparatorMenuItem(), helpGuideItem, interactiveHelpItem,
                new javafx.scene.control.SeparatorMenuItem(), aboutItem);

        javafx.scene.control.Menu toolsMenu = new javafx.scene.control.Menu("_Tools");

        mongoSimItem = new javafx.scene.control.CheckMenuItem("Embedded MongoDB (Disabled)");
        oracleSimItem = new javafx.scene.control.CheckMenuItem("Embedded H2 DB (Disabled)");

        mongoSimItem.selectedProperty().addListener((obs, oldV, newV) -> {
            mongoSimItem.setText(newV ? "Embedded MongoDB (Active)" : "Embedded MongoDB (Disabled)");
        });
        oracleSimItem.selectedProperty().addListener((obs, oldV, newV) -> {
            oracleSimItem.setText(newV ? "Embedded H2 DB (Active)" : "Embedded H2 DB (Disabled)");
        });

        javafx.scene.control.MenuItem variablesItem = new javafx.scene.control.MenuItem("Variables Editor...");
        variablesItem.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            if (baseDir != null) {
                VariablesEditorWindow.show(baseDir, null);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        javafx.scene.control.MenuItem toolsCryptoItem = new javafx.scene.control.MenuItem("Crypto Studio...");
        toolsCryptoItem.setOnAction(e -> CryptoStudioWindow.show());

        javafx.scene.control.MenuItem transformItem = new javafx.scene.control.MenuItem(
                "Data Transformation Studio...");
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

        // Visual Data Mapper menu item removed
        // javafx.scene.control.MenuItem visualDataMapperItem = new
        // javafx.scene.control.MenuItem("Visual Data Mapper...");
        // visualDataMapperItem.setOnAction(e -> VisualDataMapperWindow.show());

        javafx.scene.control.MenuItem kameletBuilderItem = new javafx.scene.control.MenuItem("Kamelet Builder...");
        kameletBuilderItem.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            if (baseDir != null) {
                KameletStudioWindow.show(baseDir);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        javafx.scene.control.MenuItem dependencyCatalogItem = new javafx.scene.control.MenuItem(
                "Dependency Catalog...");
        dependencyCatalogItem.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            if (baseDir != null) {
                DependencyCatalogWindow.show(baseDir);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        javafx.scene.control.MenuItem exportItem = new javafx.scene.control.MenuItem("Export to Liquibase...");
        exportItem.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (checked.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING,
                        "Please select one or more routes using the checkboxes in the Explorer.");
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
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING,
                        "Please select one or more routes using the checkboxes in the Explorer.");
                themeDialog(alert);
                alert.showAndWait();
                return;
            }
            RemoteDeployWindow.showForRoutes(getWorkspaceRoot(), checked);
        });

        toolsMenu.getItems().addAll(
                variablesItem,
                validateItem,
                diagramItem,
                fakerItem,
                new javafx.scene.control.SeparatorMenuItem(),
                kameletBuilderItem,
                dependencyCatalogItem,
                new javafx.scene.control.SeparatorMenuItem(),
                remoteDeployItem,
                exportItem,
                new javafx.scene.control.SeparatorMenuItem(),
                mongoSimItem,
                oracleSimItem);

        javafx.scene.control.Menu themeMenu = new javafx.scene.control.Menu("T_heme");
        javafx.scene.control.ToggleGroup themeGroup = new javafx.scene.control.ToggleGroup();
        String savedTheme = java.util.prefs.Preferences.userNodeForPackage(RouteBuilderApp.class).get("themeName",
                "VSCode Dark");
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
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="-252 -200 504 400">
                  <defs>
                    <linearGradient id="topBlueGrad" x1="-50" y1="50" x2="50" y2="-50" gradientUnits="userSpaceOnUse">
                      <stop offset="49.8%" stop-color="#428EB8" />
                      <stop offset="50.2%" stop-color="#7CC0E3" />
                    </linearGradient>
                    <linearGradient id="bottomDarkGrad" x1="-50" y1="50" x2="50" y2="-50" gradientUnits="userSpaceOnUse">
                      <stop offset="49.8%" stop-color="#0F4A56" />
                      <stop offset="50.2%" stop-color="#186975" />
                    </linearGradient>
                    <clipPath id="clipTL">
                      <path d="M -100 -100 L -4 -100 L -4 -62 L -10 -62 C -14 -72, -28 -68, -28 -50 C -28 -32, -14 -28, -10 -38 L -4 -38 L -4 -4 L -38 -4 L -38 -10 C -28 -14, -32 -28, -50 -28 C -68 -28, -72 -14, -62 -10 L -62 -4 L -100 -4 Z" />
                    </clipPath>
                    <clipPath id="clipTR">
                      <path d="M 100 -100 L 100 -4 L 62 -4 L 62 -10 C 72 -14, 68 -28, 50 -28 C 32 -28, 28 -14, 38 -10 L 38 -4 L 4 -4 L 4 -38 L -2 -38 C -6 -28, -20 -32, -20 -50 C -20 -68, -6 -72, -2 -62 L 4 -62 L 4 -100 Z" />
                    </clipPath>
                    <clipPath id="clipBL">
                      <path d="M -100 100 L -100 4 L -62 4 L -62 -2 C -72 -6, -68 -20, -50 -20 C -32 -20, -28 -6, -38 -2 L -38 4 L -4 4 L -4 38 L -10 38 C -14 28, -28 32, -28 50 C -28 68, -14 72, -10 62 L -4 62 L -4 100 Z" />
                    </clipPath>
                    <clipPath id="clipBR">
                      <path d="M 100 100 L 4 100 L 4 62 L -2 62 C -6 72, -20 68, -20 50 C -20 32, -6 28, -2 38 L 4 38 L 4 4 L 38 4 L 38 -2 C 28 -6, 32 -20, 50 -20 C 68 -20, 72 -6, 62 -2 L 62 4 L 100 4 Z" />
                    </clipPath>
                  </defs>
                  <g transform="scale(1.4)">
                    <g stroke-width="8" stroke-linejoin="round">
                      <path d="M -115 40 L -55 100 L -175 100 Z" fill="#155F6E" stroke="#155F6E" />
                      <path d="M 115 40 L 175 100 L 55 100 Z" fill="#26828E" stroke="#26828E" />
                    </g>
                    <g transform="rotate(45)" stroke-linejoin="round">
                      <g clip-path="url(#clipTL)">
                        <path d="M -100 -100 L -4 -100 L -4 -62 L -10 -62 C -14 -72, -28 -68, -28 -50 C -28 -32, -14 -28, -10 -38 L -4 -38 L -4 -4 L -38 -4 L -38 -10 C -28 -14, -32 -28, -50 -28 C -68 -28, -72 -14, -62 -10 L -62 -4 L -100 -4 Z" fill="url(#topBlueGrad)" />
                      </g>
                      <g clip-path="url(#clipTR)">
                        <path d="M 100 -100 L 100 -4 L 62 -4 L 62 -10 C 72 -14, 68 -28, 50 -28 C 32 -28, 28 -14, 38 -10 L 38 -4 L 4 -4 L 4 -38 L -2 -38 C -6 -28, -20 -32, -20 -50 C -20 -68, -6 -72, -2 -62 L 4 -62 L 4 -100 Z" fill="#F3A869" />
                      </g>
                      <g clip-path="url(#clipBL)">
                        <path d="M -100 100 L -100 4 L -62 4 L -62 -2 C -72 -6, -68 -20, -50 -20 C -32 -20, -28 -6, -38 -2 L -38 4 L -4 4 L -4 38 L -10 38 C -14 28, -28 32, -28 50 C -28 68, -14 72, -10 62 L -4 62 L -4 100 Z" fill="#156574" />
                      </g>
                      <g clip-path="url(#clipBR)">
                        <path d="M 100 100 L 4 100 L 4 62 L -2 62 C -6 72, -20 68, -20 50 C -20 32, -6 28, -2 38 L 4 38 L 4 4 L 38 4 L 38 -2 C 28 -6, 32 -20, 50 -20 C 68 -20, 72 -6, 62 -2 L 62 4 L 100 4 Z" fill="url(#bottomDarkGrad)" />
                      </g>
                    </g>
                  </g>
                </svg>
                """;
        logoIcon.getEngine()
                .loadContent("<html><body style='margin:0;padding:0;overflow:hidden;background:transparent;'>" + iconSvg
                        + "</body></html>");
        logoIcon.setMouseTransparent(true);
        btnLogo.setGraphic(logoIcon);
        btnLogo.setTooltip(new javafx.scene.control.Tooltip("About Tessera"));
        btnLogo.setOnAction(e -> showAboutDialog());

        javafx.scene.control.ToggleButton btnViewExplorer = new javafx.scene.control.ToggleButton("Explorer",
                new org.kordamp.ikonli.javafx.FontIcon("fas-folder"));
        btnViewExplorer.setSelected(true);
        btnViewExplorer.getStyleClass().add("toolbar-btn");

        javafx.scene.control.ToggleButton btnViewCode = new javafx.scene.control.ToggleButton("Code",
                new org.kordamp.ikonli.javafx.FontIcon("fas-code"));
        btnViewCode.setSelected(true);
        btnViewCode.getStyleClass().add("toolbar-btn");

        javafx.scene.control.ToggleButton btnViewDiagram = new javafx.scene.control.ToggleButton("Diagram",
                new org.kordamp.ikonli.javafx.FontIcon("fas-project-diagram"));
        btnViewDiagram.setSelected(true);
        btnViewDiagram.getStyleClass().add("toolbar-btn");

        javafx.scene.control.Button btnSwapPanels = new javafx.scene.control.Button("Swap Panels",
                new org.kordamp.ikonli.javafx.FontIcon("fas-exchange-alt"));
        btnSwapPanels.getStyleClass().add("toolbar-btn");

        btnPlay = new javafx.scene.control.Button("Play");
        org.kordamp.ikonli.javafx.FontIcon playIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-play");
        playIcon.setIconColor(javafx.scene.paint.Color.web("#4CAF50"));
        btnPlay.setGraphic(playIcon);
        btnPlay.getStyleClass().addAll("toolbar-btn", "btn-play");
        btnPlay.setDisable(true); // Disabled by default until a file/folder is selected or checked

        org.kordamp.ikonli.javafx.FontIcon stopIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-stop");
        stopIcon.setIconColor(javafx.scene.paint.Color.web("#999999"));
        btnStop = new javafx.scene.control.Button("Stop", stopIcon);
        btnStop.getStyleClass().addAll("toolbar-btn", "btn-stop");
        btnStop.setDisable(true);

        btnStop.disabledProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                stopIcon.setIconColor(javafx.scene.paint.Color.web("#F44336")); // Vivid Red when enabled
            } else {
                stopIcon.setIconColor(javafx.scene.paint.Color.web("#999999")); // Greyed out when disabled
            }
        });

        javafx.scene.control.Button btnExport = new javafx.scene.control.Button("Export",
                new org.kordamp.ikonli.javafx.FontIcon("fas-download"));
        btnExport.getStyleClass().addAll("toolbar-btn", "btn-export");
        btnExport.setTooltip(new javafx.scene.control.Tooltip("Export Selected Routes to Liquibase Changelog"));
        btnExport.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (checked.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING,
                        "Please select one or more routes using the checkboxes in the Explorer.");
                themeDialog(alert);
                alert.showAndWait();
                return;
            }
            LiquibaseExportWindow.showForRoutes(getWorkspaceRoot(), checked);
        });

        javafx.scene.control.Button btnRemoteDeploy = new javafx.scene.control.Button("Run Remotely",
                new org.kordamp.ikonli.javafx.FontIcon("fas-server"));
        btnRemoteDeploy.getStyleClass().addAll("toolbar-btn", "btn-deploy");
        btnRemoteDeploy
                .setTooltip(new javafx.scene.control.Tooltip("Deploy & Test Selected Routes on Remote Container"));
        btnRemoteDeploy.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (checked.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING,
                        "Please select one or more routes using the checkboxes in the Explorer.");
                themeDialog(alert);
                alert.showAndWait();
                return;
            }
            RemoteDeployWindow.showForRoutes(getWorkspaceRoot(), checked);
        });

        javafx.scene.control.Button btnManual = new javafx.scene.control.Button("Help Guide",
                new org.kordamp.ikonli.javafx.FontIcon("fas-question-circle"));
        btnManual.getStyleClass().addAll("toolbar-btn", "btn-manual");
        btnManual.setOnAction(e -> new RouteBuilderHelpWindow().show());

        javafx.scene.control.Button btnVariables = new javafx.scene.control.Button("Variables",
                new org.kordamp.ikonli.javafx.FontIcon("fas-cube"));
        btnVariables.getStyleClass().addAll("toolbar-btn", "btn-variables");
        btnVariables.setTooltip(new javafx.scene.control.Tooltip("Open Workspace Properties / Variables"));
        btnVariables.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            if (baseDir != null) {
                VariablesEditorWindow.show(baseDir, null);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("No Active Workspace");
                alert.setContentText("Please open a project or folder first.");
                alert.showAndWait();
            }
        });

        javafx.scene.control.Button btnCrypto = new javafx.scene.control.Button("Crypto",
                new org.kordamp.ikonli.javafx.FontIcon("fas-shield-alt"));
        btnCrypto.getStyleClass().addAll("toolbar-btn", "btn-decrypt");
        btnCrypto.setTooltip(new javafx.scene.control.Tooltip("Open Universal Crypto Studio (AES, Base64, URL)"));
        btnCrypto.setOnAction(e -> CryptoStudioWindow.show());

        javafx.scene.control.Button btnTransform = new javafx.scene.control.Button("Transform",
                new org.kordamp.ikonli.javafx.FontIcon("fas-random"));
        btnTransform.getStyleClass().addAll("toolbar-btn", "btn-transform");
        btnTransform.setTooltip(new javafx.scene.control.Tooltip("Open Data Transformation Studio"));
        btnTransform.setOnAction(e -> {
            TransformationStudioWindow studio = new TransformationStudioWindow();
            studio.show();
        });

        javafx.scene.control.Button btnValidateStudio = new javafx.scene.control.Button("Validate",
                new org.kordamp.ikonli.javafx.FontIcon("fas-check-double"));
        btnValidateStudio.getStyleClass().addAll("toolbar-btn", "btn-validate-studio");
        btnValidateStudio.setTooltip(new javafx.scene.control.Tooltip("Open Universal Validator Studio"));
        btnValidateStudio.setOnAction(e -> {
            ValidatorStudioWindow validatorStudio = new ValidatorStudioWindow();
            validatorStudio.show();
        });

        javafx.scene.control.Button btnDiagramStudio = new javafx.scene.control.Button("Diagrams",
                new org.kordamp.ikonli.javafx.FontIcon("fas-paint-brush"));
        btnDiagramStudio.getStyleClass().addAll("toolbar-btn", "btn-diagram-studio");
        btnDiagramStudio.setTooltip(new javafx.scene.control.Tooltip("Open Universal Diagram Studio"));
        btnDiagramStudio.setOnAction(e -> {
            java.io.File workspaceRoot = getWorkspaceRoot();
            DiagramStudioWindow diagramStudio = new DiagramStudioWindow(workspaceRoot);
            diagramStudio.show();
        });

        javafx.scene.control.Button btnDocConverter = new javafx.scene.control.Button("Doc Converter",
                new org.kordamp.ikonli.javafx.FontIcon("fas-file-alt"));
        btnDocConverter.getStyleClass().addAll("toolbar-btn", "btn-deps");
        btnDocConverter
                .setTooltip(new javafx.scene.control.Tooltip("Open Document Converter Studio (PDF/Word/Excel to MD)"));
        btnDocConverter.setOnAction(e -> {
            java.io.File workspaceRoot = getWorkspaceRoot();
            DocumentConverterStudioWindow studio = new DocumentConverterStudioWindow(workspaceRoot);
            studio.show();
        });

        javafx.scene.control.Button btnFakerStudio = new javafx.scene.control.Button("Faker",
                new org.kordamp.ikonli.javafx.FontIcon("fas-magic"));
        btnFakerStudio.getStyleClass().addAll("toolbar-btn", "btn-faker-studio");
        btnFakerStudio.setTooltip(new javafx.scene.control.Tooltip("Open Universal Faker & Template Studio"));
        btnFakerStudio.setOnAction(e -> {
            java.io.File baseDir = getWorkspaceRoot();
            FakerStudioWindow fakerStudio = new FakerStudioWindow(baseDir);
            fakerStudio.show();
        });

        javafx.scene.control.Button btnKamelets = new javafx.scene.control.Button("Kamelets",
                new org.kordamp.ikonli.javafx.FontIcon("fas-puzzle-piece"));
        btnKamelets.getStyleClass().addAll("toolbar-btn", "btn-kamelets");
        btnKamelets.setTooltip(new javafx.scene.control.Tooltip("Open Kamelet Studio Builder"));
        btnKamelets.setOnAction(e -> {
            java.io.File base = getWorkspaceRoot();
            if (base != null) {
                KameletStudioWindow.show(base);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        javafx.scene.control.Button btnDeps = new javafx.scene.control.Button("Dependencies",
                new org.kordamp.ikonli.javafx.FontIcon("fas-list"));
        btnDeps.getStyleClass().addAll("toolbar-btn", "btn-dependencies");
        btnDeps.setTooltip(new javafx.scene.control.Tooltip("Open Dependency Catalog Manager"));
        btnDeps.setOnAction(e -> {
            java.io.File base = getWorkspaceRoot();
            if (base != null) {
                DependencyCatalogWindow.show(base);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING, "Please open a folder or workspace first.");
                themeDialog(alert);
                alert.showAndWait();
            }
        });

        javafx.scene.control.ToggleButton btnMongoSim = new javafx.scene.control.ToggleButton("Mongo DB (Off)",
                new org.kordamp.ikonli.javafx.FontIcon("fas-leaf"));
        btnMongoSim.getStyleClass().addAll("toolbar-btn", "btn-mongo-sim");
        btnMongoSim.setTooltip(new javafx.scene.control.Tooltip(
                "Turn ON to automatically start the background Native Embedded MongoDB (Flapdoodle) with your route."));
        btnMongoSim.setOnMouseEntered(e -> {
            if (!btnMongoSim.isSelected())
                btnMongoSim.setStyle("-fx-background-color: rgba(76, 175, 80, 0.2);");
        });
        btnMongoSim.setOnMouseExited(e -> {
            if (!btnMongoSim.isSelected())
                btnMongoSim.setStyle("");
        });
        btnMongoSim.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                if (consolePane != null)
                    consolePane.log(
                            "\n\033[1;32m[Tessera Studio] Embedded MongoDB injection ARMED for next Camel run.\033[0m\n");
                btnMongoSim.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                btnMongoSim.setText("Mongo DB (ON)");
            } else {
                if (consolePane != null)
                    consolePane.log("\n\033[1;31m[Tessera Studio] Embedded MongoDB injection DISABLED.\033[0m\n");
                btnMongoSim.setStyle("");
                btnMongoSim.setText("Mongo DB (Off)");
            }
        });

        javafx.scene.control.ToggleButton btnOracleSim = new javafx.scene.control.ToggleButton("SQL DB (Off)",
                new org.kordamp.ikonli.javafx.FontIcon("fas-database"));
        btnOracleSim.getStyleClass().addAll("toolbar-btn", "btn-oracle-sim");
        btnOracleSim.setTooltip(new javafx.scene.control.Tooltip(
                "Turn ON to automatically start the background Native Embedded H2 SQL Database with your route."));
        btnOracleSim.setOnMouseEntered(e -> {
            if (!btnOracleSim.isSelected())
                btnOracleSim.setStyle("-fx-background-color: rgba(33, 150, 243, 0.2);");
        });
        btnOracleSim.setOnMouseExited(e -> {
            if (!btnOracleSim.isSelected())
                btnOracleSim.setStyle("");
        });
        btnOracleSim.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                if (consolePane != null)
                    consolePane.log(
                            "\n\033[1;36m[Tessera Studio] Embedded H2 SQL injection ARMED for next Camel run.\033[0m\n");
                btnOracleSim.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
                btnOracleSim.setText("SQL DB (ON)");
            } else {
                if (consolePane != null)
                    consolePane.log("\n\033[1;31m[Tessera Studio] Embedded H2 SQL injection DISABLED.\033[0m\n");
                btnOracleSim.setStyle("");
                btnOracleSim.setText("SQL DB (Off)");
            }
        });

        toolBar.getItems().addAll(btnLogo, new javafx.scene.control.Separator(), btnViewExplorer,
                new javafx.scene.control.Separator(), btnPlay, btnStop, new javafx.scene.control.Separator(),
                btnMongoSim, btnOracleSim, new javafx.scene.control.Separator(), btnVariables, btnCrypto, btnTransform,
                btnValidateStudio, btnDiagramStudio, btnDocConverter, btnFakerStudio, btnKamelets, btnDeps,
                btnRemoteDeploy, btnExport, btnManual);

        for (javafx.scene.Node node : toolBar.getItems()) {
            if (node instanceof javafx.scene.control.Button && node != btnLogo) {
                node.setOnMouseEntered(
                        e -> node.setStyle("-fx-background-color: rgba(128, 128, 128, 0.2); -fx-cursor: hand;"));
                node.setOnMouseExited(e -> node.setStyle(""));
            }
        }

        boolean[] swapCodeDiagram = { false };

        viewExplorerItem.selectedProperty().bindBidirectional(btnViewExplorer.selectedProperty());
        viewCodeItem.selectedProperty().bindBidirectional(btnViewCode.selectedProperty());
        viewDiagramItem.selectedProperty().bindBidirectional(btnViewDiagram.selectedProperty());
        mongoSimItem.selectedProperty().bindBidirectional(btnMongoSim.selectedProperty());
        oracleSimItem.selectedProperty().bindBidirectional(btnOracleSim.selectedProperty());

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
            javafx.scene.control.TreeItem<java.io.File> selectedItem = treePane.getTreeView().getSelectionModel()
                    .getSelectedItem();
            java.io.File selectedFile = (selectedItem != null && selectedItem.getValue().isFile())
                    ? selectedItem.getValue()
                    : null;

            if (checkedList.size() > 1) {
                // Multi-route mode: Show code, show all diagrams
                viewCodeItem.setSelected(true);
                editorPane.loadFiles(checkedList);

                java.util.List<String> contents = new java.util.ArrayList<>();
                for (java.io.File f : checkedList) {
                    try {
                        contents.add(java.nio.file.Files.readString(f.toPath()));
                    } catch (Exception ignored) {
                    }
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
                    } catch (Exception ignored) {
                    }
                } else {
                    editorPane.closeFile();
                    diagramPane.renderDiagram("");
                }
            }

            boolean hasChecked = !checked.isEmpty();
            boolean hasSelected = selectedFile != null;
            boolean isRunning = runnerProcess[0] != null && runnerProcess[0].isAlive();
            if (btnPlay != null)
                btnPlay.setDisable(isRunning || (!hasChecked && !hasSelected));
            if (btnStop != null)
                btnStop.setDisable(!isRunning);
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

        helpPortalPane = new HelpPortalPane(() -> {
            if (helpPortalStage != null) {
                helpPortalStage.hide();
            }
        });

        java.util.function.BiConsumer<java.io.File, String> playProject = (target, mode) -> {
            boolean offline = "offline".equals(mode);
            btnPlay.setDisable(true);
            btnStop.setDisable(false);
            System.out.println("Starting Routes with JBang... (mode=" + mode + ", target="
                    + (target == null ? "all" : target.getName()) + ")");
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
                if (offline)
                    command.add("--offline");
                command.add("camel");
                command.add("run");
                String portArg = System.getenv("CAMEL_SERVER_PORT");
                command.add("--port=" + (portArg != null ? portArg : "9090"));
                command.add("--logging-level=info");

                java.io.File propsFile = new java.io.File(baseDir, "application.properties");
                if (propsFile.exists()) {
                    command.add("--properties=application.properties");
                } else if (workspaceRoot != null) {
                    java.io.File wsProps = new java.io.File(workspaceRoot, "application.properties");
                    if (wsProps.exists()) {
                        try {
                            String relProps = baseDir.toPath().toAbsolutePath()
                                    .relativize(wsProps.toPath().toAbsolutePath()).toString().replace("\\", "/");
                            command.add("--properties=" + relProps);
                        } catch (Exception ex) {
                            command.add("--properties=" + wsProps.getAbsolutePath().replace("\\", "/"));
                        }
                    }
                }

                java.util.Set<String> addedPaths = new java.util.HashSet<>();
                java.util.Set<String> dependencies = new java.util.HashSet<>();

                if (target == null) {
                    java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
                    if (!checked.isEmpty()) {
                        for (java.io.File f : checked) {
                            if (f.isFile()) {
                                String abs = f.getAbsolutePath().replace("\\", "/");
                                if (addedPaths.add(abs)) {
                                    command.add(abs);
                                }
                                for (java.io.File srcFile : findCamelKSources(f)) {
                                    processCamelSource(srcFile, baseDir, addedPaths, command);
                                }
                                dependencies.addAll(findCamelKDependencies(f));
                            }
                        }
                    } else {
                        command.add(".");
                    }
                } else if (target.isFile()) {
                    String val = target.getAbsolutePath().replace("\\", "/");
                    if (addedPaths.add(val)) {
                        command.add(val);
                    }
                    for (java.io.File srcFile : findCamelKSources(target)) {
                        processCamelSource(srcFile, baseDir, addedPaths, command);
                    }
                    dependencies.addAll(findCamelKDependencies(target));
                } else { // Directory
                    java.util.List<java.io.File> collected = new java.util.ArrayList<>();
                    collectAllRouteFiles(target, collected);
                    if (!collected.isEmpty()) {
                        for (java.io.File f : collected) {
                            String abs = f.getAbsolutePath().replace("\\", "/");
                            if (addedPaths.add(abs)) {
                                command.add(abs);
                            }
                            for (java.io.File srcFile : findCamelKSources(f)) {
                                processCamelSource(srcFile, baseDir, addedPaths, command);
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

                boolean needsMongo = false;
                boolean needsSql = false;
                java.util.List<java.io.File> scanFiles = new java.util.ArrayList<>();
                if (target == null) {
                    if (treePane != null)
                        scanFiles.addAll(treePane.getCheckedFiles());
                } else if (target.isFile()) {
                    scanFiles.add(target);
                } else if (target.isDirectory()) {
                    collectAllRouteFiles(target, scanFiles);
                }

                for (java.io.File scanFile : scanFiles) {
                    if (scanFile != null && scanFile.isFile()) {
                        try {
                            String content = new String(java.nio.file.Files.readAllBytes(scanFile.toPath()),
                                    java.nio.charset.StandardCharsets.UTF_8).toLowerCase();
                            if (content.contains("mongodb:") || content.contains("mongodb-driver"))
                                needsMongo = true;
                            if (content.contains("sql:") || content.contains("jdbc:"))
                                needsSql = true;
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (mongoSimItem != null && oracleSimItem != null) {
                    if (mongoSimItem.isSelected() || needsMongo) {
                        command.add("--dependency=mvn:org.mongodb:mongodb-driver-sync:4.11.1");
                        command.add("--dependency=mvn:de.flapdoodle.embed:de.flapdoodle.embed.mongo:4.24.0");
                        command.add("--dependency=mvn:org.apache.camel:camel-mongodb:4.18.0");
                        java.io.File mongoFile = new java.io.File(workspaceRoot != null ? workspaceRoot : baseDir,
                                ".tessera/EmbeddedMongo.java");
                        if (mongoFile.exists()) {
                            String mongoPath = mongoFile.getAbsolutePath().replace("\\", "/");
                            if (addedPaths.add(mongoPath)) {
                                command.add(mongoPath);
                            }
                        }
                    }
                    if (oracleSimItem.isSelected() || needsSql) {
                        command.add("--dependency=mvn:com.h2database:h2:2.2.224");
                        command.add("--dependency=mvn:org.apache.camel:camel-sql:4.18.0");
                        java.io.File h2File = new java.io.File(workspaceRoot != null ? workspaceRoot : baseDir,
                                ".tessera/H2DataSource.java");
                        if (h2File.exists()) {
                            String h2Path = h2File.getAbsolutePath().replace("\\", "/");
                            if (addedPaths.add(h2Path)) {
                                command.add(h2Path);
                            }
                        }
                    }

                }

                boolean dev = "dev".equals(mode);
                command.add("--runtime=main");
                if (dev)
                    command.add("--dev");

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.environment().put("TERM", "xterm-256color");
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    pb.environment().put("JAVA_TOOL_OPTIONS", "-Dde.flapdoodle.os.override=Windows|X86_64");
                }
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
                javafx.scene.control.TreeItem<java.io.File> selectedItem = treePane.getTreeView().getSelectionModel()
                        .getSelectedItem();
                java.io.File target = (selectedItem != null) ? selectedItem.getValue() : null;
                playProject.accept(target, "offline");
            }
        });

        btnStop.setOnAction(e -> {
            btnStop.setDisable(true);
            btnPlay.setDisable(true);
            System.out.println("Stopping Routes...");
            new Thread(() -> {
                if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
                    runnerProcess[0].descendants().forEach(ProcessHandle::destroyForcibly);
                    runnerProcess[0].destroyForcibly();
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
                    Process stopProcess = new ProcessBuilder(stopCmd).start();
                    stopProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                javafx.application.Platform.runLater(() -> {
                    boolean hasChecked = !treePane.getCheckedFiles().isEmpty();
                    boolean hasSelected = treePane.getTreeView().getSelectionModel().getSelectedItem() != null;
                    btnPlay.setDisable(!hasChecked && !hasSelected);
                });
            }).start();
        });

        btnExport.setOnAction(e -> {
            java.util.Set<java.io.File> checked = treePane.getCheckedFiles();
            if (checked.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING,
                        "Please select one or more routes using the checkboxes in the Explorer.");
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
                if (!camelDir.exists())
                    camelDir.mkdirs();

                generateChapterSamples(treePane, selectedDirectory);

                // setBaseDirectory will pick up the 'camel' folder
                treePane.setBaseDirectory(selectedDirectory);
                saveRecentProject(selectedDirectory.getAbsolutePath(), prefs, recentProjectsMenu, treePane);

                // Update preferences for other studios to point to the new workspace folders
                java.util.prefs.Preferences transPrefs = java.util.prefs.Preferences
                        .userNodeForPackage(TransformationStudioWindow.class);
                transPrefs.put("mappingsPath", new java.io.File(selectedDirectory, "mappings").getAbsolutePath());

                java.util.prefs.Preferences diagPrefs = java.util.prefs.Preferences
                        .userNodeForPackage(DiagramStudioWindow.class);
                diagPrefs.put("workspaceRoot", new java.io.File(selectedDirectory, "diagrams").getAbsolutePath());

                java.util.prefs.Preferences docPrefs = java.util.prefs.Preferences
                        .userNodeForPackage(DocumentConverterStudioWindow.class);
                docPrefs.put("workspaceRoot", new java.io.File(selectedDirectory, "docs").getAbsolutePath());
                docPrefs.put("outputRoot", new java.io.File(selectedDirectory, "docs/output").getAbsolutePath());

                java.util.prefs.Preferences valPrefs = java.util.prefs.Preferences
                        .userNodeForPackage(ValidatorStudioWindow.class);
                valPrefs.put("workspaceRoot", new java.io.File(selectedDirectory, "validator").getAbsolutePath());
            }
        });

        newFileItem.setOnAction(e -> treePane.createTemplateFile("new-file.yaml", ""));
        newKameletItem.setOnAction(e -> treePane.createTemplateFile("my-kamelet.kamelet.yaml",
                "apiVersion: camel.apache.org/v1alpha1\nkind: Kamelet\nmetadata:\n  name: my-kamelet\nspec:\n  definition:\n    title: \"My Kamelet\"\n    description: \"Does something\"\n    properties:\n      foo:\n        type: string\n  template:\n    from:\n      uri: \"timer:tick\"\n      steps:\n        - log: \"${body}\"\n"));
        newComponentItem.setOnAction(e -> treePane.createTemplateFile("MyComponent.java",
                "package com.example;\n\nimport org.apache.camel.Endpoint;\nimport org.apache.camel.support.DefaultComponent;\nimport java.util.Map;\n\npublic class MyComponent extends DefaultComponent {\n    @Override\n    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {\n        return null;\n    }\n}\n"));
        newProcessorItem.setOnAction(e -> treePane.createTemplateFile("MyProcessor.java",
                "package com.example;\n\nimport org.apache.camel.Exchange;\nimport org.apache.camel.Processor;\n\npublic class MyProcessor implements Processor {\n    @Override\n    public void process(Exchange exchange) throws Exception {\n        String body = exchange.getIn().getBody(String.class);\n        exchange.getIn().setBody(body + \" processed\");\n    }\n}\n"));
        newJavaDslItem.setOnAction(e -> treePane.createTemplateFile("MyRoute.java",
                "package com.example;\n\nimport org.apache.camel.builder.RouteBuilder;\n\npublic class MyRoute extends RouteBuilder {\n    @Override\n    public void configure() throws Exception {\n        from(\"timer:java?period=1000\")\n            .log(\"Java DSL Route Triggered\")\n            .to(\"mock:result\");\n    }\n}\n"));
        newXmlDslItem.setOnAction(e -> treePane.createTemplateFile("xml-route.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<routes xmlns=\"http://camel.apache.org/schema/spring\">\n    <route id=\"xml-route\">\n        <from uri=\"timer:xml?period=1000\"/>\n        <log message=\"XML DSL Route Triggered\"/>\n        <to uri=\"mock:result\"/>\n    </route>\n</routes>\n"));
        newYamlDslItem.setOnAction(e -> treePane.createTemplateFile("yaml-route.yaml",
                "- route:\n    id: \"yaml-route\"\n    from:\n      uri: \"timer:yaml?period=1000\"\n      steps:\n        - log: \"YAML DSL Route Triggered\"\n"));
        newGroovyDslItem.setOnAction(e -> treePane.createTemplateFile("groovy-route.groovy",
                "import org.apache.camel.builder.RouteBuilder\n\nclass MyGroovyRoute extends RouteBuilder {\n    void configure() {\n        from(\"timer:groovy?period=1000\")\n            .log(\"Groovy DSL Route Triggered\")\n            .to(\"mock:result\")\n    }\n}\n"));
        newKotlinDslItem.setOnAction(e -> treePane.createTemplateFile("kotlin-route.kts",
                "import org.apache.camel.builder.RouteBuilder\n\nclass MyKotlinRoute : RouteBuilder() {\n    override fun configure() {\n        from(\"timer:kotlin?period=1000\")\n            .log(\"Kotlin DSL Route Triggered\")\n            .to(\"mock:result\")\n    }\n}\n"));
        newXsltItem.setOnAction(e -> treePane.createTemplateFile("transform.xslt",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n  <xsl:template match=\"/\">\n    <result>\n      <message>Transformed by XSLT</message>\n    </result>\n  </xsl:template>\n</xsl:stylesheet>\n"));
        newJsltItem.setOnAction(e -> treePane.createTemplateFile("transform.jslt",
                "{\n  \"transformed\": .body,\n  \"status\": \"success\"\n}\n"));
        newFtlItem.setOnAction(e -> treePane.createTemplateFile("template.ftl",
                "Hello ${headers.name!'World'}!\nYour message is: ${body}\n"));

        // 2. Middle Panel: VSCode-like Yaml Editor (Refreshes tree on save)
        editorPane = new YamlEditorPane(this::updateDiagram, () -> {
            treePane.refresh();
        });
        editorPane.setLspManager(lspManager);
        ThemeManager.registerRoot(editorPane);

        topContainer.getChildren().add(editorPane.getToolbar());

        editorPane.setOnTabClosed(file -> {
            treePane.getCheckedFiles().remove(file);
            treePane.refresh();
            refreshGlobalLayout.run();
        });

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
                if (offline)
                    command.add("--offline");
                command.add("camel");
                command.add("run");
                String portArg = System.getenv("CAMEL_SERVER_PORT");
                command.add("--port=" + (portArg != null ? portArg : "9090"));
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
                    } else if (workspaceRoot != null
                            && new java.io.File(workspaceRoot, "application.properties").exists()) {
                        targetProps = new java.io.File(workspaceRoot, "application.properties");
                    }

                    if (targetProps != null) {
                        try {
                            String relProps = baseDir.toPath().toAbsolutePath()
                                    .relativize(targetProps.toPath().toAbsolutePath()).toString().replace("\\", "/");
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
                        String relSrc = baseDir.toPath().toAbsolutePath().relativize(srcFile.toPath().toAbsolutePath())
                                .toString().replace("\\", "/");
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
                if (dev)
                    command.add("--dev");

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.environment().put("TERM", "xterm-256color");
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    pb.environment().put("JAVA_TOOL_OPTIONS", "-Dde.flapdoodle.os.override=Windows|X86_64");
                }
                pb.directory(baseDir);
                pb.redirectErrorStream(true);
                Process singleProcess = pb.start();

                if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
                    runnerProcess[0].descendants().forEach(ProcessHandle::destroyForcibly);
                    runnerProcess[0].destroyForcibly();
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
            javafx.application.Platform.runLater(() -> {
                if (editorPane != null && editorPane.getBtnStopFile() != null) {
                    editorPane.getBtnStopFile().setDisable(true);
                }
                if (btnStop != null)
                    btnStop.setDisable(true);
            });
            new Thread(() -> {
                if (runnerProcess[0] != null && runnerProcess[0].isAlive()) {
                    runnerProcess[0].descendants().forEach(ProcessHandle::destroyForcibly);
                    runnerProcess[0].destroyForcibly();
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
                    Process stopProcess = new ProcessBuilder(stopCmd).start();
                    stopProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        // 3. Right Panel: Visual Diagram
        diagramPane = new DiagramPane(theme -> setGlobalTheme(theme), updatedYaml -> {
            editorPane.setText(updatedYaml);
        });
        ThemeManager.registerRoot(
                diagramPane);

        Runnable updateLayout = () -> {
            mainSplitPane.getItems().clear();
            if (viewExplorerItem.isSelected())
                mainSplitPane.getItems().add(treePane);

            if (swapCodeDiagram[0]) {
                if (viewDiagramItem.isSelected())
                    mainSplitPane.getItems().add(diagramPane);
                if (viewCodeItem.isSelected())
                    mainSplitPane.getItems().add(editorPane);
            } else {
                if (viewCodeItem.isSelected())
                    mainSplitPane.getItems().add(editorPane);
                if (viewDiagramItem.isSelected())
                    mainSplitPane.getItems().add(diagramPane);
            }

            int count = mainSplitPane.getItems().size();
            if (count == 3) {
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

        editorPane.setOnToggleDiagram(() -> {
            viewDiagramItem.setSelected(!viewDiagramItem.isSelected());
        });

        editorPane.setOnClose(() -> viewCodeItem.setSelected(false));
        diagramPane.setOnClose(() -> viewDiagramItem.setSelected(false));

        diagramPane.setOnMaximize(() -> {
            boolean onlyDiagram = !viewExplorerItem.isSelected() && !viewCodeItem.isSelected()
                    && viewDiagramItem.isSelected();
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

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.F5) {
                if (btnPlay != null && !btnPlay.isDisabled()) {
                    btnPlay.fire();
                    e.consume();
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.F6) {
                if (btnStop != null && !btnStop.isDisabled()) {
                    btnStop.fire();
                    e.consume();
                }
            }
        });

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

        // Apply initial theme
        javafx.application.Platform
                .runLater(() -> setGlobalTheme(com.tessera.ui.components.ThemeManager.getCurrentThemeName()));

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

    private void saveRecentProject(String path, java.util.prefs.Preferences prefs,
            javafx.scene.control.Menu recentProjectsMenu, RouteTreePane treePane) {
        if (path == null || path.isEmpty())
            return;
        prefs.put("lastOpenedDir", path);
        loadWorkspaceProperties();

        String history = prefs.get("recentProjects", "");
        java.util.List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(history.split(";")));
        list.remove(path);
        list.add(0, path);
        if (list.size() > 10)
            list = list.subList(0, 10);
        prefs.put("recentProjects", String.join(";", list));
        updateRecentProjectsMenu(prefs, recentProjectsMenu, treePane);
    }

    private void updateRecentProjectsMenu(java.util.prefs.Preferences prefs,
            javafx.scene.control.Menu recentProjectsMenu, RouteTreePane treePane) {
        recentProjectsMenu.getItems().clear();
        String history = prefs.get("recentProjects", "");
        if (history.isEmpty())
            return;
        for (String path : history.split(";")) {
            if (path.isEmpty())
                continue;
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

            if (!docsInputDir.exists())
                docsInputDir.mkdirs();
            if (!docsOutputDir.exists())
                docsOutputDir.mkdirs();

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
        if (indexBytes.length == 0)
            return;

        String filesIndex = new String(indexBytes, java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = filesIndex.split("\\r?\\n");
        for (String relativePath : lines) {
            relativePath = relativePath.trim();
            if (relativePath.isEmpty() || relativePath.endsWith("files.txt"))
                continue;

            java.io.File targetFile = new java.io.File(base, relativePath);
            if (targetFile.exists())
                continue;

            byte[] content = readResourceBytes(resourcePrefix + relativePath);
            if (content.length == 0)
                continue;

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
        if (propertyPaths == null)
            return deps;
        for (String pathStr : propertyPaths) {
            java.io.File file = new java.io.File(pathStr);
            if (!file.exists())
                continue;
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("#") || line.isEmpty())
                        continue;
                    int eq = line.indexOf('=');
                    if (eq == -1)
                        continue;
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
            } catch (Exception ignored) {
            }
        }
        return deps;
    }

    public java.io.File getWorkspaceRoot() {
        if (treePane == null)
            return null;
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
                            "}");
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
                            "  window.editor.executeEdits('clipboard', [{range: window.editor.getSelection(), text: ''}]); "
                            +
                            "}");
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
                                "  window.editor.executeEdits('clipboard', [{range: window.editor.getSelection(), text: decodeURIComponent('"
                                + encoded + "')}]); " +
                                "}");
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
                            "}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void undoWebView(javafx.scene.web.WebView webView) {
        try {
            webView.getEngine().executeScript(
                    "if(window.editor) { " +
                            "  window.editor.trigger('keyboard', 'undo', null); " +
                            "}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void redoWebView(javafx.scene.web.WebView webView) {
        try {
            webView.getEngine().executeScript(
                    "if(window.editor) { " +
                            "  window.editor.trigger('keyboard', 'redo', null); " +
                            "}");
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
            java.io.File jarFile = new java.io.File(
                    RouteBuilderApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            java.io.File installDir = jarFile.getParentFile().getParentFile();
            jbangExe = new java.io.File(installDir, jbangScript);
        } catch (Exception ignored) {
        }

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
            java.io.File jarFile = new java.io.File(
                    RouteBuilderApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            java.io.File installDir = jarFile.getParentFile().getParentFile();
            catalogFile = new java.io.File(installDir, "jbang-catalog.json");
        } catch (Exception ignored) {
        }

        if (catalogFile == null || !catalogFile.exists()) {
            catalogFile = new java.io.File(System.getProperty("user.dir"), "jbang-catalog.json");
        }
        if (!catalogFile.exists()) {
            catalogFile = new java.io.File(new java.io.File(System.getProperty("user.dir"), "route-builder"),
                    "jbang-catalog.json");
        }
        return catalogFile.exists() ? catalogFile.getAbsolutePath().replace("\\", "/") : null;
    }

    private static void processCamelSource(java.io.File srcFile, java.io.File baseDir, java.util.Set<String> addedPaths,
            java.util.List<String> command) {
        String val = srcFile.getAbsolutePath().replace("\\", "/");
        if (addedPaths.add(val)) {
            command.add(val);
        }
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
            } catch (Exception ignored) {
            }
        }
        return "4.18.0"; // default fallback
    }

    private static void collectAllRouteFiles(java.io.File dir, java.util.List<java.io.File> collected) {
        java.io.File[] files = dir.listFiles();
        if (files == null)
            return;
        for (java.io.File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".")) {
                    collectAllRouteFiles(f, collected);
                }
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".java") || name.endsWith(".xml")
                        || name.endsWith(".groovy") || name.endsWith(".xslt") || name.endsWith(".xsl")) {
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

    private static void findCamelKSourcesRecursive(java.io.File file, java.util.List<java.io.File> sources,
            java.util.Set<String> visited) {
        if (file == null || !file.exists() || !file.isFile())
            return;
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (Exception ex) {
            canonicalPath = file.getAbsolutePath();
        }
        if (!visited.add(canonicalPath))
            return;

        java.io.File parent = file.getParentFile();
        if (parent != null && parent.exists()) {
            java.io.File[] siblings = parent.listFiles((d, n) -> {
                String name = n.toLowerCase();
                return name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".json")
                        || name.endsWith(".csv") || name.endsWith(".txt") || name.endsWith(".properties")
                        || name.endsWith(".xslt") || name.endsWith(".xsl");
            });
            if (siblings != null) {
                for (java.io.File sibling : siblings) {
                    if (!sibling.equals(file)) {
                        String sibCanonical;
                        try {
                            sibCanonical = sibling.getCanonicalPath();
                        } catch (Exception ex) {
                            sibCanonical = sibling.getAbsolutePath();
                        }
                        if (visited.add(sibCanonical)) {
                            sources.add(sibling);
                        }
                    }
                }
            }
        }

        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
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
        } catch (Exception ignored) {
        }
    }

    private static java.util.List<String> findCamelKDependencies(java.io.File file) {
        java.util.List<String> deps = new java.util.ArrayList<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        findCamelKDependenciesRecursive(file, deps, visited);
        return deps;
    }

    private static void findCamelKDependenciesRecursive(java.io.File file, java.util.List<String> deps,
            java.util.Set<String> visited) {
        if (file == null || !file.exists() || !file.isFile())
            return;
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (Exception ex) {
            canonicalPath = file.getAbsolutePath();
        }
        if (!visited.add(canonicalPath))
            return;

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
        } catch (Exception ignored) {
        }
    }

    public void loadWorkspaceProperties() {
        java.io.File wsRoot = getWorkspaceRoot();
        java.io.File cacheFile = new java.io.File(System.getProperty("user.home"), ".tessera_workspace_cache");

        if (wsRoot == null) {
            // Check cache file first (most reliable)
            if (cacheFile.exists()) {
                try {
                    String cachedPath = java.nio.file.Files.readString(cacheFile.toPath()).trim();
                    if (!cachedPath.isEmpty()) {
                        java.io.File f = new java.io.File(cachedPath);
                        if (f.exists() && f.isDirectory())
                            wsRoot = f;
                    }
                } catch (Exception ignored) {
                }
            }

            if (wsRoot == null) {
                // Check preference
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences
                        .userNodeForPackage(RouteBuilderApp.class);
                String lastDir = prefs.get("lastOpenedDir", null);
                if (lastDir != null) {
                    wsRoot = new java.io.File(lastDir);
                }
            }

            // Climb up to find application.properties if needed
            while (wsRoot != null && wsRoot.exists() && !new java.io.File(wsRoot, "application.properties").exists()) {
                wsRoot = wsRoot.getParentFile();
            }
        }

        if (wsRoot == null || !new java.io.File(wsRoot, "application.properties").exists()) {
            // Fallback to user.dir
            wsRoot = new java.io.File(System.getProperty("user.dir"));
            while (wsRoot != null && !new java.io.File(wsRoot, "application.properties").exists()) {
                wsRoot = wsRoot.getParentFile();
            }
        }

        if (wsRoot != null && new java.io.File(wsRoot, "application.properties").exists()) {
            // Update cache
            try {
                java.nio.file.Files.writeString(cacheFile.toPath(), wsRoot.getAbsolutePath());
            } catch (Exception ignored) {
            }

            java.io.File propsFile = new java.io.File(wsRoot, "application.properties");
            try (java.io.InputStream input = new java.io.FileInputStream(propsFile)) {
                java.util.Properties props = new java.util.Properties();
                props.load(input);

                String absoluteWsPath = wsRoot.getAbsolutePath().replace("\\", "/");
                System.setProperty("WORKSPACE_ROOT_DIR", absoluteWsPath);
                System.out.println("[Tessera] Workspace Root: " + absoluteWsPath);

                final java.io.File finalWsRoot = wsRoot;
                // Proactively scope the tree pane to camel/routes if they exist
                if (treePane != null) {
                    javafx.application.Platform.runLater(() -> {
                        java.io.File currentBase = treePane.getBaseDirectory();
                        System.out.println("[Tessera] Checking Explorer scoping. Current base: "
                                + (currentBase != null ? currentBase.getAbsolutePath() : "null"));
                        // If we aren't already in camel or routes, try to find them from the root
                        if (currentBase == null || (!currentBase.getName().equals("camel")
                                && !currentBase.getName().equals("routes"))) {
                            System.out.println("[Tessera] Attempting to redirect Explorer to subfolder of: "
                                    + finalWsRoot.getAbsolutePath());
                            treePane.setBaseDirectory(finalWsRoot);
                        } else {
                            System.out.println(
                                    "[Tessera] Explorer already correctly scoped to: " + currentBase.getName());
                        }
                    });
                }

                boolean needsUpdate = false;
                String[] vars = { "FAKER_TEMPLATES_DIR", "FAKER_DB_DIR", "MAPPING_DIR", "DRAWINGS_DIR",
                        "VALIDATOR_DIR" };
                for (String var : vars) {
                    String value = props.getProperty(var);
                    if (value != null && !value.trim().isEmpty()) {
                        // Normalize and cleanup already corrupted paths (recursive prefixes)
                        if (value.contains("/home/") || value.contains(":/")) {
                            String[] parts = value.split("(?=/home/)|(?=[A-Z]:/)");
                            if (parts.length > 1) {
                                // Take the last part which is likely the most "nested" and hopefully correct
                                // one
                                value = parts[parts.length - 1];
                            }
                        }

                        java.io.File resolvedFile = new java.io.File(value);
                        // More robust absolute check: isAbsolute() + starts with / or drive letter
                        boolean isAbs = resolvedFile.isAbsolute() || value.startsWith("/")
                                || value.matches("^[A-Z]:/.*");

                        if (!isAbs) {
                            resolvedFile = new java.io.File(wsRoot, value);
                        }

                        String absPath = resolvedFile.getAbsolutePath().replace("\\", "/");
                        System.setProperty(var, absPath);

                        // Sync preferences for other studios to ensure they load from these paths
                        if (var.equals("MAPPING_DIR")) {
                            java.util.prefs.Preferences.userNodeForPackage(TransformationStudioWindow.class)
                                    .put("mappingsPath", absPath);
                        } else if (var.equals("DRAWINGS_DIR")) {
                            java.util.prefs.Preferences.userNodeForPackage(DiagramStudioWindow.class)
                                    .put("workspaceRoot", absPath);
                        }

                        // Only update if it actually changed and wasn't already absolute
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
                        props.store(output,
                                "Standardized Tessera Workspace Configuration - Automatically Updated to Absolute Paths");
                        System.out.println(
                                "[Tessera] Updated application.properties with absolute paths for better portability across subdirectories.");
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
        webView.setPrefSize(800, 600);

        // Get theme colors from current state
        String bgHex = "#f4f7f9";
        String titleHex = "#333333";
        String subHex = "#4B555A";

        if (currentThemeClass.contains("dark") || currentThemeClass.contains("cyberpunk")
                || currentThemeClass.contains("midnight") || currentThemeClass.contains("hacker")
                || currentThemeClass.contains("nordic") || currentThemeClass.contains("dracula")
                || currentThemeClass.contains("monokai")) {
            bgHex = "#0a0a1a";
            titleHex = "#00ffff";
            subHex = "#ff00aa";
            if (currentThemeClass.equals("theme-cyberpunk")) {
                bgHex = "#000000";
                titleHex = "#f3f315";
                subHex = "#00ff41";
            } else if (currentThemeClass.equals("theme-midnight")) {
                bgHex = "#05070a";
                titleHex = "#ffffff";
                subHex = "#aab1ff";
            }
        }

        final String finalBg = bgHex;
        final String finalTitle = titleHex;
        final String finalSub = subHex;

        String content = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Tessera Animated Logo</title>
                  <style>
                    body {
                      margin: 0;
                      min-height: 100vh;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      background-color: BG_COLOR;
                      overflow: hidden;
                    }
                    .logo-container {
                      width: 100%;
                      max-width: 500px;
                      padding: 2rem;
                    }
                    /* Core Setup & Performance Optimization */
                    .animated-element {
                      backface-visibility: hidden;
                      will-change: transform, opacity;
                    }
                    /* Base states (Hidden and positioned outward) */
                    .piece-tl { transform: translate(-350px, -350px) rotate(-60deg); opacity: 0; }
                    .piece-tr { transform: translate(350px, -350px) rotate(60deg); opacity: 0; }
                    .piece-bl { transform: translate(-350px, 350px) rotate(-60deg); opacity: 0; }
                    .piece-br { transform: translate(350px, 350px) rotate(60deg); opacity: 0; }
                    .tri-left { transform: translate(-180px, 20px); opacity: 0; }
                    .tri-right { transform: translate(180px, 20px); opacity: 0; }
                    .text-title { transform: translateY(35px); opacity: 0; }
                    .text-sub { transform: translateY(35px); opacity: 0; }
                    /* Trigger classes */
                    .play-animation .piece-tl { animation: assembleTL 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
                    .play-animation .piece-tr { animation: assembleTR 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
                    .play-animation .piece-bl { animation: assembleBL 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
                    .play-animation .piece-br { animation: assembleBR 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
                    .play-animation .tri-left { animation: slideTriLeft 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.2s forwards; }
                    .play-animation .tri-right { animation: slideTriRight 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.2s forwards; }
                    .play-animation .text-title { animation: textFadeUp 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.5s forwards; }
                    .play-animation .text-sub { animation: textFadeUp 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.75s forwards; }
                    /* Keyframes */
                    @keyframes assembleTL { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }
                    @keyframes assembleTR { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }
                    @keyframes assembleBL { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }
                    @keyframes assembleBR { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }
                    @keyframes slideTriLeft { 100% { transform: translate(0, 0); opacity: 1; } }
                    @keyframes slideTriRight { 100% { transform: translate(0, 0); opacity: 1; } }
                    @keyframes textFadeUp { 100% { transform: translateY(0); opacity: 1; } }
                  </style>
                </head>
                <body>
                  <div class="logo-container">
                    <svg id="tessera-logo" xmlns="http://www.w3.org/2000/svg" viewBox="190 30 620 560" width="100%" height="100%">
                      <defs>
                        <linearGradient id="topBlueGrad" x1="-50" y1="50" x2="50" y2="-50" gradientUnits="userSpaceOnUse">
                          <stop offset="49.8%" stop-color="#3A8DB5" />
                          <stop offset="50.2%" stop-color="#64ACD0" />
                        </linearGradient>
                        <linearGradient id="bottomDarkGrad" x1="-50" y1="50" x2="50" y2="-50" gradientUnits="userSpaceOnUse">
                          <stop offset="49.8%" stop-color="#0E505E" />
                          <stop offset="50.2%" stop-color="#176E7D" />
                        </linearGradient>
                        <filter id="pieceShadow" x="-30%" y="-30%" width="160%" height="160%">
                          <feDropShadow dx="3" dy="6" stdDeviation="5" flood-color="#002233" flood-opacity="0.18" />
                        </filter>
                      </defs>
                      <g transform="translate(500, 215) scale(1.1)">
                        <g stroke-width="12" stroke-linejoin="round">
                          <path class="animated-element tri-left" d="M -120 45 L -55 110 L -185 110 Z" fill="#136070" stroke="#136070" />
                          <path class="animated-element tri-right" d="M 120 45 L 185 110 L 55 110 Z" fill="#207886" stroke="#207886" />
                        </g>
                        <g transform="rotate(45)" stroke-linejoin="round">
                          <path class="animated-element piece-br" d="M 100 100 L 4 100 L 4 62 L -2 62 C -6 72, -20 68, -20 50 C -20 32, -6 28, -2 38 L 4 38 L 4 4 L 38 4 L 38 -2 C 28 -6, 32 -20, 50 -20 C 68 -20, 72 -6, 62 -2 L 62 4 L 100 4 Z" fill="url(#bottomDarkGrad)" filter="url(#pieceShadow)" />
                          <path class="animated-element piece-bl" d="M -100 100 L -100 4 L -62 4 L -62 -2 C -72 -6, -68 -20, -50 -20 C -32 -20, -28 -6, -38 -2 L -38 4 L -4 4 L -4 38 L -10 38 C -14 28, -28 32, -28 50 C -28 68, -14 72, -10 62 L -4 62 L -4 100 Z" fill="#136070" filter="url(#pieceShadow)" />
                          <path class="animated-element piece-tr" d="M 100 -100 L 100 -4 L 62 -4 L 62 -10 C 72 -14, 68 -28, 50 -28 C 32 -28, 28 -14, 38 -10 L 38 -4 L 4 -4 L 4 -38 L -2 -38 C -6 -28, -20 -32, -20 -50 C -20 -68, -6 -72, -2 -62 L 4 -62 L 4 -100 Z" fill="#F1A463" filter="url(#pieceShadow)" />
                          <path class="animated-element piece-tl" d="M -100 -100 L -4 -100 L -4 -62 L -10 -62 C -14 -72, -28 -68, -28 -50 C -28 -32, -14 -28, -10 -38 L -4 -38 L -4 -4 L -38 -4 L -38 -10 C -28 -14, -32 -28, -50 -28 C -68 -28, -72 -14, -62 -10 L -62 -4 L -100 -4 Z" fill="url(#topBlueGrad)" filter="url(#pieceShadow)" />
                        </g>
                      </g>
                      <text class="animated-element text-title" x="500" y="470" font-family="'Segoe UI', 'Montserrat', sans-serif" font-weight="900" font-size="82" fill="TITLE_COLOR" text-anchor="middle" letter-spacing="6">TESSERA</text>
                      <text class="animated-element text-sub" x="500" y="525" font-family="'Segoe UI', 'Montserrat', sans-serif" font-weight="400" font-size="26" fill="SUB_COLOR" text-anchor="middle" letter-spacing="0.5">The foundational tiles of enterprise architecture</text>
                    </svg>
                  </div>
                  <script>
                    document.addEventListener("DOMContentLoaded", function() {
                      setTimeout(function() {
                        document.getElementById('tessera-logo').classList.add('play-animation');
                      }, 100);
                    });
                  </script>
                </body>
                </html>
                """
                .replace("BG_COLOR", finalBg).replace("TITLE_COLOR", finalTitle).replace("SUB_COLOR", finalSub);

        webView.getEngine().loadContent(content);
        aboutDialog.getDialogPane().setContent(webView);
        themeDialog(aboutDialog);
        aboutDialog.showAndWait();
    }
}
