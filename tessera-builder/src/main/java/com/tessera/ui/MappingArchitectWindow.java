package com.tessera.ui;

import com.tessera.ui.components.SuiKit;
import com.tessera.ui.components.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Sovereign Resilience Engine - Mapping Architect
 * High-fidelity visual mapping leveraging D3.js via an embedded WebView.
 */
public class MappingArchitectWindow {

    private Stage stage;
    private HttpServer localServer;
    private WebEngine engine;

    public void show(String titleStr, String logicContent, String sourceContent) {
        Platform.runLater(() -> {
            try {
                this.stage = new Stage();
                stage.setTitle("Sovereign Mapping Architect - " + titleStr);

                BorderPane root = new BorderPane();
                root.getStyleClass().addAll("app-root", RouteBuilderApp.currentThemeClass != null ? RouteBuilderApp.currentThemeClass : "theme-vscode-dark");
                ThemeManager.registerRoot(root);

                // --- Header ---
                HBox header = SuiKit.createStudioHeader("Mapping Architect : " + titleStr, "fas-brain");
                header.setPadding(new Insets(10, 20, 10, 20));
                Label lblTitle = new Label("[ SOVEREIGN ARCHITECT : " + titleStr.toUpperCase() + " ]");
                lblTitle.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 18px; -fx-text-fill: #00FF41; -fx-font-weight: bold;");
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

                Button btnClose = new Button("TERMINATE", new FontIcon("fas-power-off"));
                btnClose.getStyleClass().add("editor-btn"); 
                btnClose.setStyle("-fx-text-fill: #F44336; -fx-border-color: #F44336;");
                btnClose.setOnAction(e -> stage.close());

                FontIcon brainIcon = new FontIcon("fas-brain");
                brainIcon.setIconColor(javafx.scene.paint.Color.web("#00FF41"));
                header.getChildren().addAll(brainIcon, lblTitle, spacer, btnClose);
                root.setTop(header);

                // --- WebView Content ---
                WebView webView = new WebView();
                engine = webView.getEngine();
                root.setCenter(webView);

                // Start Local Server for D3.js and HTML
                startLocalServer();

                // Prepare Graph Data
                String graphJson = MappingParser.parseXsltToGraph(logicContent, sourceContent);
                String encodedJson = java.net.URLEncoder.encode(graphJson, StandardCharsets.UTF_8).replace("+", "%20");

                engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        engine.executeScript("renderGraph(decodeURIComponent('" + encodedJson + "'));");
                    }
                });

                engine.load("http://127.0.0.1:" + localServer.getAddress().getPort() + "/visualizer/mapper.html");

                com.tessera.ui.components.ThemeManager.registerRoot(root);
                Scene scene = new Scene(root, 1400, 900);
                if (getClass().getResource("/styles/main.css") != null) {
                    scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
                }
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.setOnHidden(e -> { if(localServer != null) localServer.stop(0); });
                stage.show();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void startLocalServer() {
        try {
            localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            localServer.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                InputStream is = getClass().getResourceAsStream(path);
                
                if (is == null) {
                    exchange.sendResponseHeaders(404, -1);
                } else {
                    byte[] data = is.readAllBytes();
                    String mime = "text/plain";
                    if (path.endsWith(".html")) mime = "text/html; charset=utf-8";
                    else if (path.endsWith(".js")) mime = "application/javascript";
                    else if (path.endsWith(".css")) mime = "text/css";
                    
                    exchange.getResponseHeaders().add("Content-Type", mime);
                    exchange.sendResponseHeaders(200, data.length);
                    exchange.getResponseBody().write(data);
                }
                exchange.close();
            });
            localServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            localServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
