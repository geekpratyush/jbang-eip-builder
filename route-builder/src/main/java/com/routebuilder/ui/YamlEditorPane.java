package com.routebuilder.ui;

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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

public class YamlEditorPane extends VBox {

    private WebView webView;
    private WebEngine engine;
    private boolean initialized = false;
    private String pendingText = "";
    private boolean isUpdatingFromBridge = false;
    private JavaBridge bridge = new JavaBridge();
    private String currentTheme = com.routebuilder.ui.components.ThemeManager.getCurrentThemeClass().equals("theme-intellij-light") || com.routebuilder.ui.components.ThemeManager.getCurrentThemeClass().equals("theme-github-light") ? "vs" : "vs-dark";

    private Consumer<String> onTextChanged;
    private Runnable onFileSaved;
    private Label title;
    private File currentFile = null;
    private com.routebuilder.lsp.LspManager lspManager;

    public void setTheme(String themeName) {
        String themeClass = com.routebuilder.ui.components.ThemeManager.getCurrentThemeClass();
        String theme = "vs-dark";
        String bgColor = "#1e1e1e";
        if (themeClass.equals("theme-intellij-light") || themeClass.equals("theme-github-light")) {
            theme = "vs"; bgColor = "#ffffff";
        } else if (themeClass.equals("theme-hacker") || themeClass.equals("theme-cyberpunk")) {
            theme = "hc-black"; bgColor = "#000000";
        }
        
        currentTheme = theme;
        if (initialized && engine != null) {
            try {
                engine.executeScript("window.setTheme('" + theme + "'); document.body.style.backgroundColor = '" + bgColor + "';");
            } catch (Exception e) {}
        }
    }

    public void setLspManager(com.routebuilder.lsp.LspManager lspManager) {
        this.lspManager = lspManager;
        if (this.lspManager != null) {
            this.lspManager.setDiagnosticsConsumer(diagnostics -> {
                Platform.runLater(() -> {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        String json = mapper.writeValueAsString(diagnostics.getDiagnostics());
                        String encoded = java.net.URLEncoder.encode(json, "UTF-8").replace("+", "%20");
                        if (engine != null) {
                            engine.executeScript("if(window.showDiagnostics) window.showDiagnostics(decodeURIComponent('" + encoded + "'));");
                        }
                    } catch(Exception e) { e.printStackTrace(); }
                });
            });
        }
    }

    private Button btnPlayFile;
    private Button btnStopFile;
    
    public Button getBtnPlayFile() { return btnPlayFile; }
    public Button getBtnStopFile() { return btnStopFile; }

    public YamlEditorPane(Consumer<String> onTextChanged, Runnable onFileSaved) {
        this.onTextChanged = onTextChanged;
        this.onFileSaved = onFileSaved;
        
        getStyleClass().add("editor-pane");
        
        title = new Label("EDITOR: Untitled.yaml");
        title.getStyleClass().add("pane-title");

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(5));
        toolbar.getStyleClass().add("editor-toolbar");

        Button btnSave = new Button();
        btnSave.setGraphic(new FontIcon("fas-save"));
        btnSave.setTooltip(new Tooltip("Save"));
        btnSave.getStyleClass().addAll("editor-btn", "btn-save");
        btnSave.setOnAction(e -> saveFile());

        Button btnSaveAs = new Button();
        btnSaveAs.setGraphic(new FontIcon("fas-file-alt"));
        btnSaveAs.setTooltip(new Tooltip("Save As..."));
        btnSaveAs.getStyleClass().addAll("editor-btn", "btn-save-as");
        btnSaveAs.setOnAction(e -> saveFileAs());

        Button btnDeploy = new Button();
        btnDeploy.setGraphic(new FontIcon("fas-cloud-upload-alt"));
        btnDeploy.setTooltip(new Tooltip("Deploy to REST API"));
        btnDeploy.getStyleClass().addAll("editor-btn", "btn-deploy");
        btnDeploy.setOnAction(e -> deployYaml());

        Button btnCopy = new Button();
        btnCopy.setGraphic(new FontIcon("fas-copy"));
        btnCopy.setTooltip(new Tooltip("Copy Selection"));
        btnCopy.getStyleClass().addAll("editor-btn", "btn-copy-text");
        btnCopy.setOnAction(e -> copy());

        Button btnCopyAll = new Button();
        btnCopyAll.setGraphic(new FontIcon("fas-clipboard-list"));
        btnCopyAll.setTooltip(new Tooltip("Copy All Content"));
        btnCopyAll.getStyleClass().addAll("editor-btn", "btn-copy-all-text");
        btnCopyAll.setOnAction(e -> {
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        });

        Button btnToggleDiagram = new Button();
        btnToggleDiagram.setGraphic(new FontIcon("fas-columns"));
        btnToggleDiagram.setTooltip(new Tooltip("Toggle Diagram Panel"));
        btnToggleDiagram.getStyleClass().addAll("editor-btn");
        btnToggleDiagram.setOnAction(e -> {
            if (onToggleDiagram != null) onToggleDiagram.run();
        });

        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("fas-times"));
        btnClose.setTooltip(new Tooltip("Close Editor"));
        btnClose.getStyleClass().addAll("editor-btn");
        btnClose.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        btnPlayFile = new Button();
        btnPlayFile.setGraphic(new FontIcon("fas-play"));
        btnPlayFile.setTooltip(new Tooltip("Play Current File"));
        btnPlayFile.getStyleClass().addAll("editor-btn", "btn-play-file");
        btnPlayFile.setOnAction(e -> {
            if (onPlayFile != null && currentFile != null) onPlayFile.accept(currentFile, "dev");
        });

        btnStopFile = new Button();
        btnStopFile.setGraphic(new FontIcon("fas-stop"));
        btnStopFile.setTooltip(new Tooltip("Stop Current File"));
        btnStopFile.getStyleClass().addAll("editor-btn", "btn-stop-file");
        btnStopFile.setDisable(true);
        btnStopFile.setOnAction(e -> {
            if (onStopFile != null) onStopFile.run();
        });

        toolbar.getChildren().addAll(btnSave, btnSaveAs, btnCopy, btnCopyAll, btnToggleDiagram, btnDeploy, new javafx.scene.control.Separator(), btnPlayFile, btnStopFile, new javafx.scene.control.Separator(), btnClose);

        webView = new WebView();
        engine = webView.getEngine();
        VBox.setVgrow(webView, Priority.ALWAYS);

        webView.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.S) {
                saveFile();
                event.consume();
            }
        });
        RouteBuilderApp.installClipboardShortcuts(webView);

        loadMonaco();

        getChildren().addAll(title, toolbar, webView);
    }

    private void loadMonaco() {
        try {
            com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try {
                    String path = exchange.getRequestURI().getPath();
                    System.out.println("HTTP GET: " + path);
                    if ("/".equals(path) || "/index.html".equals(path)) {
                        String html = getMonacoHtml();
                        byte[] response = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                        exchange.sendResponseHeaders(200, response.length);
                        exchange.getResponseBody().write(response);
                    } else {
                        java.io.InputStream is = getClass().getResourceAsStream("/monaco" + path);
                        if (is == null) {
                            exchange.sendResponseHeaders(404, -1);
                        } else {
                            byte[] data = is.readAllBytes();
                            if (path.endsWith(".js")) exchange.getResponseHeaders().add("Content-Type", "application/javascript");
                            else if (path.endsWith(".css")) exchange.getResponseHeaders().add("Content-Type", "text/css");
                            else if (path.endsWith(".ttf")) exchange.getResponseHeaders().add("Content-Type", "font/ttf");
                            
                            exchange.sendResponseHeaders(200, data.length);
                            exchange.getResponseBody().write(data);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                } finally {
                    exchange.close();
                }
            });
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            int port = server.getAddress().getPort();

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    netscape.javascript.JSObject window = (netscape.javascript.JSObject) engine.executeScript("window");
                    window.setMember("javaBridge", bridge);
                }
            });

            engine.load("http://127.0.0.1:" + port + "/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMonacoHtml() {
        String bgColor = currentTheme.equals("vs") ? "#ffffff" : "#1e1e1e";
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <style>\n" +
            "        body { margin: 0; padding: 0; overflow: hidden; background-color: " + bgColor + "; }\n" +
            "        #editor { width: 100vw; height: 100vh; }\n" +
            "    </style>\n" +
            "    <script>\n" +
            "        window.onerror = function(msg, url, line) {\n" +
            "            if (window.javaBridge) window.javaBridge.logError('Error: ' + msg + ' at ' + line);\n" +
            "        };\n" +
            "        document.execCommand = function(command, ui, val) {\n" +
            "            if (command === 'copy' && window.javaBridge) { window.javaBridge.copyToSystem(); return true; }\n" +
            "            if (command === 'cut' && window.javaBridge) { window.javaBridge.cutToSystem(); return true; }\n" +
            "            return false;\n" +
            "        };\n" +
            "    </script>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"editor\"></div>\n" +
            "    <script src=\"/vs/loader.js\"></script>\n" +
            "    <script>\n" +
            "        require.config({ paths: { vs: '/vs' }});\n" +
            "        require(['vs/editor/editor.main'], function() {\n" +
            "            window.editor = monaco.editor.create(document.getElementById('editor'), {\n" +
            "                value: '',\n" +
            "                language: 'yaml',\n" +
            "                theme: '" + currentTheme + "',\n" +
            "                automaticLayout: true,\n" +
            "                minimap: { enabled: true },\n" +
            "                scrollBeyondLastLine: false,\n" +
            "                fontSize: 14,\n" +
            "                tabSize: 2\n" +
            "            });\n" +
            "            let isUpdating = false;\n" +
            "            window.editor.onDidChangeModelContent(function(e) {\n" +
            "                if (isUpdating) return;\n" +
            "                if (window.javaBridge) {\n" +
            "                    window.javaBridge.onContentChanged(window.editor.getValue());\n" +
            "                }\n" +
            "            });\n" +
            "            window.updateValue = function(val) {\n" +
            "                isUpdating = true;\n" +
            "                window.editor.setValue(val);\n" +
            "                isUpdating = false;\n" +
            "            };\n" +
            "            window.setLanguage = function(lang) {\n" +
            "                var model = window.editor.getModel();\n" +
            "                monaco.editor.setModelLanguage(model, lang);\n" +
            "            };\n" +
            "            window.showDiagnostics = function(json) {\n" +
            "                try {\n" +
            "                    var diags = JSON.parse(json);\n" +
            "                    console.log('Diagnostics:', diags);\n" +
            "                    var markers = diags.map(function(d) {\n" +
            "                        var sev = monaco.MarkerSeverity.Error;\n" +
            "                        if (d.severity === 2 || d.severity === 'Warning') sev = monaco.MarkerSeverity.Warning;\n" +
            "                        else if (d.severity === 3 || d.severity === 4 || d.severity === 'Information' || d.severity === 'Hint') sev = monaco.MarkerSeverity.Info;\n" +
            "                        return {\n" +
            "                            severity: sev,\n" +
            "                            startLineNumber: d.range.start.line + 1,\n" +
            "                            startColumn: d.range.start.character + 1,\n" +
            "                            endLineNumber: d.range.end.line + 1,\n" +
            "                            endColumn: d.range.end.character + 1,\n" +
            "                            message: d.message\n" +
            "                        };\n" +
            "                    });\n" +
            "                    monaco.editor.setModelMarkers(window.editor.getModel(), 'camel-lsp', markers);\n" +
            "                } catch(e) {}\n" +
            "            };\n" +
            "            window.completionCallbacks = {};\n" +
            "            window.completionId = 0;\n" +
            "            window.acceptCompletions = function(id, json) {\n" +
            "                if (window.completionCallbacks[id]) {\n" +
            "                    try { window.completionCallbacks[id](JSON.parse(json)); } catch(e) { window.completionCallbacks[id]([]); }\n" +
            "                    delete window.completionCallbacks[id];\n" +
            "                }\n" +
            "            };\n" +
            "            monaco.languages.registerCompletionItemProvider('yaml', {\n" +
            "                provideCompletionItems: function(model, position) {\n" +
            "                    return new Promise(function(resolve, reject) {\n" +
            "                        if (!window.javaBridge) { resolve({suggestions: []}); return; }\n" +
            "                        var id = ++window.completionId;\n" +
            "                        window.completionCallbacks[id] = resolve;\n" +
            "                        window.javaBridge.requestCompletions(id, position.lineNumber, position.column);\n" +
            "                    }).then(function(items) {\n" +
            "                        return { suggestions: items };\n" +
            "                    });\n" +
            "                }\n" +
            "            });\n" +
            "            window.editor.onKeyDown(function(e) {\n" +
            "                if (e.ctrlKey || e.metaKey) {\n" +
            "                    var key = e.browserEvent.key.toLowerCase();\n" +
            "                    if (key === 'c') {\n" +
            "                        if (window.javaBridge) {\n" +
            "                            window.javaBridge.copyToSystem();\n" +
            "                            e.preventDefault();\n" +
            "                            e.stopPropagation();\n" +
            "                        }\n" +
            "                    } else if (key === 'v') {\n" +
            "                        if (window.javaBridge) {\n" +
            "                            window.javaBridge.pasteToEditor();\n" +
            "                            e.preventDefault();\n" +
            "                            e.stopPropagation();\n" +
            "                        }\n" +
            "                    } else if (key === 'x') {\n" +
            "                        if (window.javaBridge) {\n" +
            "                            window.javaBridge.cutToSystem();\n" +
            "                            e.preventDefault();\n" +
            "                            e.stopPropagation();\n" +
            "                        }\n" +
            "                    }\n" +
            "                }\n" +
            "            });\n" +
            "            window.editor.addAction({\n" +
            "                id: 'java-paste',\n" +
            "                label: 'Paste',\n" +
            "                contextMenuGroupId: '9_cutcopypaste',\n" +
            "                contextMenuOrder: 3,\n" +
            "                run: function(ed) {\n" +
            "                    if (window.javaBridge) window.javaBridge.pasteToEditor();\n" +
            "                }\n" +
            "            });\n" +
            "            window.setTheme = function(themeName) {\n" +
            "                monaco.editor.setTheme(themeName);\n" +
            "                document.body.style.backgroundColor = themeName === 'vs' ? '#ffffff' : '#1e1e1e';\n" +
            "            };\n" +
            "            var checkReady = setInterval(function() {\n" +
            "                if (window.javaBridge) {\n" +
            "                    clearInterval(checkReady);\n" +
            "                    window.javaBridge.onEditorReady();\n" +
            "                }\n" +
            "            }, 50);\n" +
            "        });\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    private Runnable onToggleDiagram;
    public void setOnToggleDiagram(Runnable onToggleDiagram) {
        this.onToggleDiagram = onToggleDiagram;
    }

    private java.util.function.BiConsumer<File, String> onPlayFile;
    public void setOnPlayFile(java.util.function.BiConsumer<File, String> onPlayFile) {
        this.onPlayFile = onPlayFile;
    }

    private Runnable onStopFile;
    public void setOnStopFile(Runnable onStopFile) {
        this.onStopFile = onStopFile;
    }

    private Runnable onClose;
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public File getCurrentFile() {
        return this.currentFile;
    }

    private java.util.Timer debounceTimer;

    public class JavaBridge {
        public void logError(String msg) {
            System.err.println("WebView JS Error: " + msg);
        }

        public void onEditorReady() {
            Platform.runLater(() -> {
                initialized = true;
                if (!pendingText.isEmpty()) {
                    setTextInternal(pendingText);
                    if (lspManager != null) lspManager.openDocument(pendingText);
                    pendingText = "";
                }
            });
        }
        
        public void onContentChanged(String content) {
            if (isUpdatingFromBridge) return;
            if (lspManager != null) lspManager.updateDocument(content);
            
            // Live Update Debouncing
            if (debounceTimer != null) debounceTimer.cancel();
            debounceTimer = new java.util.Timer();
            debounceTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        if (onTextChanged != null) {
                            onTextChanged.accept(content);
                        }
                    });
                }
            }, 500); // 500ms delay
        }

        public void requestCompletions(int callbackId, int line, int col) {
            if (lspManager != null) {
                lspManager.getCompletions(line - 1, col - 1).thenAccept(json -> {
                    Platform.runLater(() -> {
                        try {
                            String encoded = java.net.URLEncoder.encode(json, "UTF-8").replace("+", "%20");
                            engine.executeScript("if (window.acceptCompletions) window.acceptCompletions(" + callbackId + ", decodeURIComponent('" + encoded + "'));");
                        } catch(Exception e) {}
                    });
                });
            } else {
                Platform.runLater(() -> {
                    engine.executeScript("if (window.acceptCompletions) window.acceptCompletions(" + callbackId + ", '[]');");
                });
            }
        }

        public void pasteToEditor() {
            Platform.runLater(() -> paste());
        }

        public void copyToSystem() {
            Platform.runLater(() -> copy());
        }

        public void cutToSystem() {
            Platform.runLater(() -> cut());
        }
    }

    public void copy() {
        try {
            String selection = (String) engine.executeScript("window.editor.getModel().getValueInRange(window.editor.getSelection());");
            if (selection != null && !selection.isEmpty()) {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(selection);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
            }
        } catch (Exception e) {}
    }

    public void cut() {
        copy();
        try {
            engine.executeScript("window.editor.executeEdits('clipboard', [{range: window.editor.getSelection(), text: ''}]);");
        } catch (Exception e) {}
    }

    public void paste() {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String content = clipboard.getString();
            try {
                String encoded = java.net.URLEncoder.encode(content, "UTF-8").replace("+", "%20");
                engine.executeScript("window.editor.executeEdits('clipboard', [{range: window.editor.getSelection(), text: decodeURIComponent('" + encoded + "')}]);");
            } catch (Exception e) {}
        }
    }

    public void selectAll() {
        try {
            engine.executeScript("window.editor.setSelection(window.editor.getModel().getFullModelRange());");
        } catch (Exception e) {}
    }

    public void setText(String text) {
        if (!initialized) {
            pendingText = text;
        } else {
            setTextInternal(text);
            if (lspManager != null) lspManager.updateDocument(text);
        }
    }

    private void setTextInternal(String text) {
        isUpdatingFromBridge = true;
        try {
            String encoded = java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20");
            String script = "var text = decodeURIComponent('" + encoded + "'); " +
                            "if (window.updateValue) window.updateValue(text); else window.editor.setValue(text);";
            engine.executeScript(script);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isUpdatingFromBridge = false;
        }
    }

    public String getText() {
        if (!initialized) return pendingText;
        return (String) engine.executeScript("window.editor.getValue();");
    }

    public void loadFile(File file) {
        if (file == null) return;
        try {
            String content = Files.readString(file.toPath());
            
            // Avoid redundant reloads if content is the same and it's the same file
            if (file.equals(currentFile) && content.equals(getText())) {
                return;
            }

            currentFile = file;
            title.setText("EDITOR: " + file.getName());
            
            String extension = "";
            int lastDot = file.getName().lastIndexOf('.');
            if (lastDot > 0) {
                extension = file.getName().substring(lastDot + 1).toLowerCase();
            }
            
            String determinedLanguage = "plaintext";
            if (extension.equals("yaml") || extension.equals("yml")) {
                determinedLanguage = "yaml";
            } else if (extension.equals("java")) {
                determinedLanguage = "java";
            } else if (extension.equals("groovy")) {
                determinedLanguage = "groovy";
            }
            
            final String lang = determinedLanguage;
            if (initialized) {
                engine.executeScript("if(window.setLanguage) window.setLanguage('" + lang + "');");
            }
            
            if (lspManager != null) {
                if (lang.equals("yaml")) {
                    lspManager.setDocumentUri(file.toURI().toString());
                } else {
                    engine.executeScript("if(window.showDiagnostics) window.showDiagnostics('[]');");
                }
            }
            
            setText(content);
            if (onTextChanged != null) {
                onTextChanged.accept(content);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void closeFile() {
        currentFile = null;
        title.setText("EDITOR: Untitled.yaml");
        setText("");
        if (lspManager != null) {
            lspManager.setDocumentUri("");
            try {
                if (initialized) {
                    engine.executeScript("if(window.showDiagnostics) window.showDiagnostics('[]');");
                }
            } catch (Exception e) {}
        }
    }

    public void saveFile() {
        if (currentFile == null) {
            saveFileAs();
        } else {
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
            title.setText("EDITOR: " + file.getName());
            writeToFile(file);
        }
    }

    private void writeToFile(File file) {
        try {
            Files.writeString(file.toPath(), getText());
            if (onFileSaved != null) {
                onFileSaved.run();
            }
            System.out.println("Saved file: " + file.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        RouteBuilderApp.themeDialog(alert);
                        alert.setTitle("Deployment Status");
                        alert.setHeaderText("Response Code: " + response.statusCode());
                        alert.setContentText(response.body().length() > 200 ? response.body().substring(0, 200) + "..." : response.body());
                        alert.showAndWait();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        RouteBuilderApp.themeDialog(alert);
                        alert.setTitle("Deployment Failed");
                        alert.setHeaderText("Error connecting to REST API");
                        alert.setContentText(ex.getMessage());
                        alert.showAndWait();
                    });
                }
            });
        });
    }

    public void undo() {
        if (initialized) engine.executeScript("window.editor.trigger('keyboard', 'undo', null);");
    }

    public void redo() {
        if (initialized) engine.executeScript("window.editor.trigger('keyboard', 'redo', null);");
    }
}
