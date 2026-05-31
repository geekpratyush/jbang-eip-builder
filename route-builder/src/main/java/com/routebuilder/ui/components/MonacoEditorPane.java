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
 * Synchronized with the Sovereign UI Design System.
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

        // Register for design tokens and theme updates
        ThemeManager.registerRoot(this);
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

        engine.load("http://127.0.0.1:" + sharedPort + "/");
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
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    private String getBaseHtml() {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>body{margin:0;padding:0;overflow:hidden;background-color:#1e1e1e;}#editor{width:100vw;height:100vh;}</style></head><body><div id=\"editor\"></div><script src=\"/vs/loader.js\"></script><script>\n" +
            "require.config({ paths: { 'vs': '/vs' } });\n" +
            "require(['vs/editor/editor.main'], function() {\n" +
            "    monaco.languages.register({ id: 'swift-mt' });\n" +
            "    monaco.languages.setMonarchTokensProvider('swift-mt', { tokenizer: { root: [[/{[1-5]:/, 'metatag'], [/^:[0-9A-Z]{2,3}:/, 'keyword'], [/\\n:[0-9A-Z]{2,3}:/, 'keyword'], [/-}/, 'metatag']] } });\n" +
            "    window.editor = monaco.editor.create(document.getElementById('editor'), { value: '', language: 'plaintext', theme: 'vs-dark', automaticLayout: true, minimap: { enabled: false }, fontSize: 14, fontFamily: 'monospace', scrollBeyondLastLine: false });\n" +
            "    window.editor.onDidChangeModelContent(function() { if (window.javaBridge) window.javaBridge.onContentChanged(window.editor.getValue()); });\n" +
            "    window.setValue = function(v) { window.editor.setValue(v); };\n" +
            "    window.getValue = function() { return window.editor.getValue(); };\n" +
            "    window.setTheme = function(t) { monaco.editor.setTheme(t); };\n" +
            "    window.setLanguage = function(l) { var lang = (l === 'text') ? 'swift-mt' : l; monaco.editor.setModelLanguage(window.editor.getModel(), lang); };\n" +
            "    window.showDiagnostics = function(json) { try { var markers = JSON.parse(json).map(function(d) { return { severity: monaco.MarkerSeverity.Error, startLineNumber: (d.range?d.range.start.line+1:1), startColumn: (d.range?d.range.start.character+1:1), endLineNumber: (d.range?d.range.end.line+1:1), endColumn: (d.range?d.range.end.character+1:1), message: d.message }; }); monaco.editor.setModelMarkers(window.editor.getModel(), 'studio', markers); } catch(e) {} };\n" +
            "    var checkBridge = setInterval(function() { if (window.javaBridge) { clearInterval(checkBridge); window.javaBridge.onEditorReady(); } }, 50);\n" +
            "});</script></body></html>";
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
                if (result instanceof String) this.currentContent = (String) result;
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
        String theme = "vs-dark"; String bgColor = "#1e1e1e";
        if (themeClass.contains("light")) { theme = "vs"; bgColor = "#ffffff"; }
        else if (themeClass.equals("theme-cyberpunk") || themeClass.equals("theme-hacker")) { theme = "hc-black"; bgColor = "#000000"; }
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
            if (onContentChanged != null) Platform.runLater(() -> onContentChanged.accept(newText));
        }
    }

    public static void stopSharedServer() {
        synchronized (serverLock) { if (sharedServer != null) { sharedServer.stop(0); sharedServer = null; } }
    }
}
