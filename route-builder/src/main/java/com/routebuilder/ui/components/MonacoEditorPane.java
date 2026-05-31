package com.routebuilder.ui.components;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpServer;

/**
 * Universal Monaco Editor Component for the Studio.
 * Optimized with a Shared Static Server to ensure reliable loading across multiple instances.
 * Includes Live Content Change tracking.
 */
public class MonacoEditorPane extends VBox {

    private static HttpServer sharedServer;
    private static int sharedPort;
    private static final Object serverLock = new Object();

    private WebView webView;
    private WebEngine engine;
    private boolean editorReady = false;
    private String currentLanguage = "yaml";
    private String currentContent = "";
    private Consumer<String> onContentChanged;
    
    private final JavaBridge bridge = new JavaBridge();

    public MonacoEditorPane() {
        this("yaml");
    }

    public MonacoEditorPane(String language) {
        this.currentLanguage = language;
        webView = new WebView();
        VBox.setVgrow(webView, Priority.ALWAYS);
        getChildren().add(webView);
        
        engine = webView.getEngine();
        com.routebuilder.ui.RouteBuilderApp.installClipboardShortcuts(webView);

        ThemeManager.addListener(this::onThemeChanged);

        ensureSharedServer();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
            } else if (newState == Worker.State.FAILED) {
                System.err.println("[MonacoEditor] Failed to load editor page: " + engine.getLoadWorker().getException());
            }
        });

        String url = "http://127.0.0.1:" + sharedPort + "/";
        engine.load(url);
    }

    public void setOnContentChanged(Consumer<String> onContentChanged) {
        this.onContentChanged = onContentChanged;
    }

    private void ensureSharedServer() {
        synchronized (serverLock) {
            if (sharedServer == null) {
                try {
                    sharedServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                    sharedServer.createContext("/", exchange -> {
                        try {
                            String path = exchange.getRequestURI().getPath();
                            if ("/".equals(path) || "/index.html".equals(path)) {
                                byte[] response = getBaseHtml().getBytes(StandardCharsets.UTF_8);
                                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                                exchange.sendResponseHeaders(200, response.length);
                                exchange.getResponseBody().write(response);
                            } else {
                                java.io.InputStream is = MonacoEditorPane.class.getResourceAsStream("/monaco" + path);
                                if (is == null) {
                                    System.err.println("[MonacoServer] Resource not found: /monaco" + path);
                                    exchange.sendResponseHeaders(404, -1);
                                } else {
                                    byte[] data = is.readAllBytes();
                                    String mime = "text/plain";
                                    if (path.endsWith(".js")) mime = "application/javascript";
                                    else if (path.endsWith(".css")) mime = "text/css";
                                    else if (path.endsWith(".ttf")) mime = "font/ttf";
                                    
                                    exchange.getResponseHeaders().add("Content-Type", mime);
                                    exchange.sendResponseHeaders(200, data.length);
                                    exchange.getResponseBody().write(data);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            exchange.close();
                        }
                    });
                    sharedServer.setExecutor(Executors.newCachedThreadPool());
                    sharedServer.start();
                    sharedPort = sharedServer.getAddress().getPort();
                    System.out.println("[MonacoServer] Shared server started on port " + sharedPort);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getBaseHtml() {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <style>\n" +
            "        body { margin: 0; padding: 0; overflow: hidden; background-color: #1e1e1e; }\n" +
            "        #editor { width: 100vw; height: 100vh; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"editor\"></div>\n" +
            "    <script src=\"/vs/loader.js\"></script>\n" +
            "    <script>\n" +
            "        require.config({ paths: { 'vs': '/vs' } });\n" +
            "        require(['vs/editor/editor.main'], function() {\n" +
            "            monaco.languages.register({ id: 'swift-mt' });\n" +
            "            monaco.languages.setMonarchTokensProvider('swift-mt', {\n" +
            "                tokenizer: { root: [\n" +
            "                    [/{[1-5]:/, 'metatag'],\n" +
            "                    [/^:[0-9A-Z]{2,3}:/, 'keyword'], [/\\n:[0-9A-Z]{2,3}:/, 'keyword'],\n" +
            "                    [/-}/, 'metatag']\n" +
            "                ] }\n" +
            "            });\n" +
            "\n" +
            "            monaco.editor.defineTheme('studio-dark', {\n" +
            "                base: 'vs-dark', inherit: true,\n" +
            "                rules: [ \n" +
            "                    { token: 'metatag', foreground: 'ce9178', fontStyle: 'bold' }, \n" +
            "                    { token: 'keyword', foreground: '569cd6', fontStyle: 'bold' },\n" +
            "                    { token: 'tag', foreground: '569cd6' },\n" +
            "                    { token: 'attribute.name', foreground: '9cdcfe' },\n" +
            "                    { token: 'attribute.value', foreground: 'ce9178' },\n" +
            "                    { token: 'string', foreground: 'ce9178' },\n" +
            "                    { token: 'number', foreground: 'b5cea8' }\n" +
            "                ],\n" +
            "                colors: { 'editor.background': '#1e1e1e' }\n" +
            "            });\n" +
            "            monaco.editor.defineTheme('studio-hacker', {\n" +
            "                base: 'hc-black', inherit: true,\n" +
            "                rules: [ \n" +
            "                    { token: '', foreground: '00ff00' }, \n" +
            "                    { token: 'keyword', foreground: '00ff00', fontStyle: 'bold' },\n" +
            "                    { token: 'metatag', foreground: '00ff00', fontStyle: 'bold' },\n" +
            "                    { token: 'string', foreground: '00ff00' }\n" +
            "                ],\n" +
            "                colors: { 'editor.background': '#000000' }\n" +
            "            });\n" +
            "            \n" +
            "            window.editor = monaco.editor.create(document.getElementById('editor'), {\n" +
            "                value: '',\n" +
            "                language: 'plaintext',\n" +
            "                theme: 'vs-dark',\n" +
            "                automaticLayout: true,\n" +
            "                minimap: { enabled: false },\n" +
            "                fontSize: 14,\n" +
            "                fontFamily: 'JetBrains Mono, Consolas, monospace',\n" +
            "                renderWhitespace: 'none',\n" +
            "                scrollBeyondLastLine: false\n" +
            "            });\n" +
            "\n" +
            "            window.editor.onDidChangeModelContent(function() {\n" +
            "                if (window.javaBridge) window.javaBridge.onContentChanged(window.editor.getValue());\n" +
            "            });\n" +
            "            \n" +
            "            window.setValue = function(v) { window.editor.setValue(v); };\n" +
            "            window.getValue = function() { return window.editor.getValue(); };\n" +
            "            window.setTheme = function(t) {\n" +
            "                var mt = (t === 'vs-dark') ? 'studio-dark' : (t === 'hc-black' ? 'studio-hacker' : t);\n" +
            "                monaco.editor.setTheme(mt);\n" +
            "            };\n" +
            "            window.setLanguage = function(l) {\n" +
            "                var lang = (l === 'text') ? 'swift-mt' : l;\n" +
            "                monaco.editor.setModelLanguage(window.editor.getModel(), lang);\n" +
            "            };\n" +
            "            window.showDiagnostics = function(json) {\n" +
            "                try {\n" +
            "                    var diags = JSON.parse(json);\n" +
            "                    var markers = diags.map(function(d) {\n" +
            "                        return {\n" +
            "                            severity: monaco.MarkerSeverity.Error,\n" +
            "                            startLineNumber: (d.range ? d.range.start.line + 1 : 1),\n" +
            "                            startColumn: (d.range ? d.range.start.character + 1 : 1),\n" +
            "                            endLineNumber: (d.range ? d.range.end.line + 1 : 1),\n" +
            "                            endColumn: (d.range ? d.range.end.character + 1 : 1),\n" +
            "                            message: d.message\n" +
            "                        };\n" +
            "                    });\n" +
            "                    monaco.editor.setModelMarkers(window.editor.getModel(), 'studio', markers);\n" +
            "                } catch(e) {}\n" +
            "            };\n" +
            "            \n" +
            "            var checkBridge = setInterval(function() {\n" +
            "                if (window.javaBridge) {\n" +
            "                    clearInterval(checkBridge);\n" +
            "                    window.javaBridge.onEditorReady();\n" +
            "                }\n" +
            "            }, 50);\n" +
            "        });\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    public void setText(String text) {
        this.currentContent = text != null ? text : "";
        if (editorReady) {
            Platform.runLater(() -> {
                try {
                    String encoded = java.net.URLEncoder.encode(this.currentContent, "UTF-8").replace("+", "%20");
                    engine.executeScript("window.setValue(decodeURIComponent('" + encoded + "'));");
                } catch (Exception e) {}
            });
        }
    }

    public String getText() {
        if (editorReady) {
            try {
                Object result = engine.executeScript("window.getValue()");
                if (result instanceof String) {
                    this.currentContent = (String) result;
                }
            } catch (Exception e) {}
        }
        return this.currentContent;
    }

    public void setLanguage(String language) {
        this.currentLanguage = language;
        if (editorReady) {
            Platform.runLater(() -> {
                try {
                    engine.executeScript("window.setLanguage('" + language + "');");
                } catch (Exception e) {}
            });
        }
    }

    public void showDiagnostics(String json) {
        if (editorReady) {
            Platform.runLater(() -> {
                try {
                    String encoded = java.net.URLEncoder.encode(json, "UTF-8").replace("+", "%20");
                    engine.executeScript("window.showDiagnostics(decodeURIComponent('" + encoded + "'));");
                } catch (Exception e) {}
            });
        }
    }

    private void onThemeChanged(String themeName) {
        if (!editorReady) return;
        
        String themeClass = ThemeManager.getCurrentThemeClass();
        String theme = "vs-dark";
        String bgColor = "#1e1e1e";
        
        if (themeClass.equals("theme-intellij-light") || themeClass.equals("theme-github-light")) {
            theme = "vs"; bgColor = "#ffffff";
        } else if (themeClass.equals("theme-hacker") || themeClass.equals("theme-cyberpunk")) {
            theme = "hc-black"; bgColor = "#000000";
        }
        
        try {
            engine.executeScript("window.setTheme('" + theme + "');");
            engine.executeScript("document.body.style.backgroundColor = '" + bgColor + "';");
        } catch (Exception e) {}
    }

    public class JavaBridge {
        public void onEditorReady() {
            Platform.runLater(() -> {
                editorReady = true;
                setText(currentContent);
                setLanguage(currentLanguage);
                onThemeChanged(ThemeManager.getCurrentThemeName());
            });
        }
        
        public void onContentChanged(String newText) {
            currentContent = newText;
            if (onContentChanged != null) {
                Platform.runLater(() -> onContentChanged.accept(newText));
            }
        }
    }

    public static void stopSharedServer() {
        synchronized (serverLock) {
            if (sharedServer != null) {
                sharedServer.stop(0);
                sharedServer = null;
            }
        }
    }
}
