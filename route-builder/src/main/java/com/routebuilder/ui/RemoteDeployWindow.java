package com.routebuilder.ui;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoteDeployWindow {

    public static class DeployItem {
        public final StringProperty id;
        public final StringProperty filename;
        public final StringProperty typeOrFormat;
        public final DoubleProperty version;
        public final StringProperty status;
        public final String rawContent;
        public final boolean isTransformer;
        public final BooleanProperty selected;
        public final List<String> allRouteIds = new ArrayList<>();

        public DeployItem(String id, String filename, String typeOrFormat, double version, String rawContent, boolean isTransformer, List<String> allRouteIds) {
            this.id = new SimpleStringProperty(id);
            this.filename = new SimpleStringProperty(filename);
            this.typeOrFormat = new SimpleStringProperty(typeOrFormat);
            this.version = new SimpleDoubleProperty(version);
            this.status = new SimpleStringProperty("Pending");
            this.rawContent = rawContent;
            this.isTransformer = isTransformer;
            this.selected = new SimpleBooleanProperty(true);
            if (allRouteIds != null) {
                this.allRouteIds.addAll(allRouteIds);
            }
        }
        
        public List<String> getAllRouteIds() { return allRouteIds; }

        public String getId() { return id.get(); }
        public String getFilename() { return filename.get(); }
        public String getTypeOrFormat() { return typeOrFormat.get(); }
        public double getVersion() { return version.get(); }
        public String getStatus() { return status.get(); }
        public String getRawContent() { return rawContent; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { selected.set(val); }
        public BooleanProperty selectedProperty() { return selected; }
    }

    public static void showForRoutes(File baseDir, Set<File> files) {
        showDialog("Remote Deploy & Run Test - Routes", false, baseDir, files, null);
    }

    public static void showForTransformations(File baseDir, Set<File> folders) {
        showDialog("Copy to Remote - Transformations", true, baseDir, folders, null);
    }

    public static void showForFiles(String title, File baseDir, Set<DeployItem> customItems) {
        showDialog(title, true, baseDir, null, customItems);
    }

    private static void showDialog(String title, boolean isCopyOnly, File baseDir, Set<File> targets, Set<DeployItem> customItems) {
        Stage stage = new Stage();
        stage.setTitle(title);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getStyleClass().add("app-root");
        if (RouteBuilderApp.currentThemeClass != null) {
            layout.getStyleClass().add(RouteBuilderApp.currentThemeClass);
        }

        String defaultApiUrl = loadApiUrl(baseDir);

        // --- Target Connection Setup ---
        GridPane topGrid = new GridPane();
        topGrid.setHgap(15);
        topGrid.setVgap(8);
        topGrid.setPadding(new Insets(0, 0, 10, 0));

        Label lblRemoteUrl = new Label("Remote Container Host URL:");
        lblRemoteUrl.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        TextField txtRemoteUrl = new TextField(defaultApiUrl);
        txtRemoteUrl.setPrefWidth(280);

        topGrid.add(lblRemoteUrl, 0, 0);
        topGrid.add(txtRemoteUrl, 1, 0);

        TextField txtServerPath = new TextField("/opt/camel/resources");
        txtServerPath.setPrefWidth(240);

        if (isCopyOnly) {
            Label lblServerPath = new Label("Server Target Path:");
            lblServerPath.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
            topGrid.add(lblServerPath, 2, 0);
            topGrid.add(txtServerPath, 3, 0);
        } else {
            Label lblRemoteMode = new Label("Deployment Strategy:");
            lblRemoteMode.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
            ComboBox<String> cmbRemoteMode = new ComboBox<>();
            cmbRemoteMode.getItems().addAll("Persistent (DB + Hot Reload)", "Temporary (Memory/Disk Only)");
            cmbRemoteMode.setValue("Temporary (Memory/Disk Only)");
            topGrid.add(lblRemoteMode, 2, 0);
            topGrid.add(cmbRemoteMode, 3, 0);
        }

        // --- Table View of Selected Assets ---
        TableView<DeployItem> table = new TableView<>();
        VBox.setVgrow(table, Priority.ALWAYS);

        ObservableList<DeployItem> items = FXCollections.observableArrayList();
        if (customItems != null) {
            items.addAll(customItems);
        } else if (isCopyOnly) {
            items.addAll(parseTransformations(baseDir, targets));
        } else {
            items.addAll(parseRoutes(baseDir, targets));
        }
        table.setItems(items);

        // --- Checkbox Column (Select All / Row Select) ---
        TableColumn<DeployItem, Boolean> colSelect = new TableColumn<>("");
        colSelect.setPrefWidth(40);
        
        CheckBox cbSelectAll = new CheckBox();
        cbSelectAll.setSelected(true);
        colSelect.setGraphic(cbSelectAll);
        
        cbSelectAll.setOnAction(e -> {
            boolean sel = cbSelectAll.isSelected();
            for (DeployItem item : items) {
                item.setSelected(sel);
            }
            table.refresh();
        });

        colSelect.setCellValueFactory(data -> data.getValue().selected);
        colSelect.setCellFactory(col -> new TableCell<DeployItem, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(evt -> {
                    DeployItem item = getTableView().getItems().get(getIndex());
                    item.setSelected(checkBox.isSelected());
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item);
                    setGraphic(checkBox);
                }
            }
        });

        TableColumn<DeployItem, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> data.getValue().id);
        colId.setPrefWidth(110);

        TableColumn<DeployItem, String> colFile = new TableColumn<>("FILE NAME");
        colFile.setCellValueFactory(data -> data.getValue().filename);
        colFile.setPrefWidth(130);

        TableColumn<DeployItem, String> colType = new TableColumn<>(isCopyOnly ? "TRANSFORMER TYPE" : "ROUTE FORMAT");
        colType.setCellValueFactory(data -> data.getValue().typeOrFormat);
        colType.setPrefWidth(100);

        TableColumn<DeployItem, Double> colVer = new TableColumn<>("VERSION");
        colVer.setCellValueFactory(data -> data.getValue().version.asObject());
        colVer.setPrefWidth(70);

        TableColumn<DeployItem, String> colStatus = new TableColumn<>(isCopyOnly ? "COPY STATUS" : "DEPLOYMENT STATUS");
        colStatus.setCellValueFactory(data -> data.getValue().status);
        colStatus.setPrefWidth(130);

        // --- Console / Live Log Area with TabPane ---
        TabPane tabPane = new TabPane();
        tabPane.setPrefHeight(200);
        VBox.setVgrow(tabPane, Priority.NEVER);

        Tab tabDeploy = new Tab("Activity Log");
        tabDeploy.setClosable(false);
        TextArea txtConsole = new TextArea();
        txtConsole.setEditable(false);
        txtConsole.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-control-inner-background: #1e1e1e; -fx-text-fill: #00ff00;");
        tabDeploy.setContent(txtConsole);

        Tab tabLogs = new Tab("Remote Live Logs");
        tabLogs.setClosable(false);
        TextArea txtLogs = new TextArea();
        txtLogs.setEditable(false);
        txtLogs.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-control-inner-background: #1e1e1e; -fx-text-fill: #00e5ff;");
        tabLogs.setContent(txtLogs);

        tabPane.getTabs().addAll(tabDeploy, tabLogs);

        // --- Action Buttons in Row ---
        TableColumn<DeployItem, Void> colActions = new TableColumn<>("ACTIONS");
        colActions.setPrefWidth(180);
        colActions.setCellFactory(col -> new TableCell<DeployItem, Void>() {
            private final Button btnRowPlay = new Button("", new org.kordamp.ikonli.javafx.FontIcon(isCopyOnly ? "fas-upload" : "fas-play"));
            private final Button btnRowStop = new Button("", new org.kordamp.ikonli.javafx.FontIcon("fas-stop"));
            private final HBox rowActions = new HBox(8, btnRowPlay, btnRowStop);

            {
                rowActions.setAlignment(javafx.geometry.Pos.CENTER);
                btnRowPlay.getStyleClass().addAll("toolbar-btn", "btn-run");
                btnRowStop.getStyleClass().addAll("toolbar-btn", "btn-stop");
                
                if (isCopyOnly) {
                    btnRowStop.setVisible(false);
                    btnRowStop.setManaged(false);
                    btnRowPlay.setTooltip(new Tooltip("Copy File to Remote Server Path"));
                }

                btnRowPlay.setOnAction(evt -> {
                    DeployItem item = getTableView().getItems().get(getIndex());
                    if (isCopyOnly) {
                        copySingleFile(item, txtRemoteUrl.getText().trim(), txtServerPath.getText().trim(), txtConsole);
                    } else {
                        deploySingleItem(item, txtRemoteUrl.getText().trim(), true, txtConsole);
                    }
                });

                btnRowStop.setOnAction(evt -> {
                    DeployItem item = getTableView().getItems().get(getIndex());
                    stopSingleItem(item, txtRemoteUrl.getText().trim(), txtConsole);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(rowActions);
                }
            }
        });

        table.getColumns().addAll(colSelect, colId, colFile, colType, colVer, colStatus, colActions);

        // --- Bottom Action Toolbar ---
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnClose = new Button("Close", new org.kordamp.ikonli.javafx.FontIcon("fas-times"));
        btnClose.getStyleClass().add("toolbar-btn");
        btnClose.setOnAction(e -> stage.close());

        Button btnStop = new Button("Stop Selected", new org.kordamp.ikonli.javafx.FontIcon("fas-stop"));
        btnStop.getStyleClass().addAll("toolbar-btn", "btn-stop");
        if (isCopyOnly) {
            btnStop.setVisible(false);
            btnStop.setManaged(false);
        }

        btnStop.setOnAction(e -> {
            String targetUrl = txtRemoteUrl.getText().trim();
            if (targetUrl.isEmpty()) return;

            long selectedCount = items.stream().filter(DeployItem::isSelected).filter(item -> !item.isTransformer).count();
            if (selectedCount == 0) {
                txtConsole.appendText("[WARNING] No active route items selected for stopping.\n");
                return;
            }

            txtConsole.appendText("[BATCH STOP STARTED] Stopping " + selectedCount + " selected routes...\n");
            for (DeployItem item : items) {
                if (item.isSelected() && !item.isTransformer) {
                    stopSingleItem(item, targetUrl, txtConsole);
                }
            }
        });

        Button btnDeploy = new Button(isCopyOnly ? "Copy Selected" : "Deploy Selected", new org.kordamp.ikonli.javafx.FontIcon(isCopyOnly ? "fas-upload" : "fas-server"));
        btnDeploy.getStyleClass().addAll("toolbar-btn", "btn-deploy");
        btnDeploy.setOnAction(e -> {
            String targetUrl = txtRemoteUrl.getText().trim();
            if (targetUrl.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a valid remote target host URL.");
                RouteBuilderApp.themeDialog(alert);
                alert.showAndWait();
                return;
            }
            
            long selectedCount = items.stream().filter(DeployItem::isSelected).count();
            if (selectedCount == 0) {
                txtConsole.appendText("[WARNING] No items selected.\n");
                return;
            }

            if (isCopyOnly) {
                String serverPath = txtServerPath.getText().trim();
                if (serverPath.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a valid server target path.");
                    RouteBuilderApp.themeDialog(alert);
                    alert.showAndWait();
                    return;
                }
                txtConsole.appendText("[BATCH COPY STARTED] Copying " + selectedCount + " selected files to " + serverPath + "...\n");
                for (DeployItem item : items) {
                    if (item.isSelected()) {
                        copySingleFile(item, targetUrl, serverPath, txtConsole);
                    }
                }
            } else {
                boolean tempOnly = true;
                txtConsole.appendText("[BATCH DEPLOYMENT STARTED] Deploying " + selectedCount + " selected targets to " + targetUrl + "\n");
                for (DeployItem item : items) {
                    if (item.isSelected()) {
                        deploySingleItem(item, targetUrl, tempOnly, txtConsole);
                    }
                }
            }
        });

        // --- Polling Timeline for Live Logs & Running Status ---
        Timeline pollTimeline = new Timeline(
            new KeyFrame(Duration.seconds(2), event -> {
                String targetUrl = txtRemoteUrl.getText().trim();
                if (targetUrl.isEmpty()) return;

                String baseUrl = targetUrl.endsWith("/") ? targetUrl.substring(0, targetUrl.length() - 1) : targetUrl;
                HttpClient client = HttpClient.newBuilder().build();

                // 1. Fetch live logs
                HttpRequest logRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/admin/logs"))
                        .GET()
                        .build();

                client.sendAsync(logRequest, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(res -> {
                            if (res.statusCode() == 200) {
                                try {
                                    org.json.JSONArray logArray = new org.json.JSONArray(res.body());
                                    List<String> logList = new ArrayList<>();
                                    for (int i = 0; i < logArray.length(); i++) {
                                        logList.add(logArray.getString(i));
                                    }
                                    String allLogs = String.join("\n", logList);
                                    javafx.application.Platform.runLater(() -> {
                                        txtLogs.setText(allLogs);
                                        txtLogs.positionCaret(allLogs.length());
                                    });
                                } catch (Exception ignored) {}
                            }
                        });

                // 2. Fetch running routes to synchronize UI status (only in Deploy/Run mode)
                if (!isCopyOnly) {
                    HttpRequest hbRequest = HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + "/admin/heartbeat"))
                            .GET()
                            .build();

                    client.sendAsync(hbRequest, HttpResponse.BodyHandlers.ofString())
                            .thenAccept(res -> {
                                if (res.statusCode() == 200) {
                                    try {
                                        JSONObject hb = new JSONObject(res.body());
                                        org.json.JSONArray running = hb.optJSONArray("runningRoutes");
                                        if (running != null) {
                                            Set<String> runningSet = new HashSet<>();
                                            for (int i = 0; i < running.length(); i++) {
                                                runningSet.add(running.getString(i));
                                            }
                                            javafx.application.Platform.runLater(() -> {
                                                for (DeployItem item : items) {
                                                    if (item.isTransformer) continue;
                                                    
                                                    if (runningSet.contains(item.getId())) {
                                                        item.status.set("Running");
                                                    } else {
                                                        String current = item.status.get();
                                                        if ("Running".equals(current) || "Deploying...".equals(current)) {
                                                            item.status.set("Inactive");
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    } catch (Exception ignored) {}
                                }
                            });
                }
            })
        );
        pollTimeline.setCycleCount(Timeline.INDEFINITE);
        pollTimeline.play();

        stage.setOnCloseRequest(e -> {
            pollTimeline.stop();
        });

        btnBox.getChildren().addAll(btnStop, btnDeploy, btnClose);
        layout.getChildren().addAll(topGrid, table, tabPane, btnBox);

        com.routebuilder.ui.components.ThemeManager.registerRoot(layout);
        Scene scene = new Scene(layout, 800, 600);
        try {
            scene.getStylesheets().add(RemoteDeployWindow.class.getResource("/styles/main.css").toExternalForm());
            if (RouteBuilderApp.currentDynamicCssUri != null) {
                scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
            }
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.show();
    }

    private static void deploySingleItem(DeployItem item, String targetUrl, boolean tempOnly, TextArea txtConsole) {
        if (targetUrl == null || targetUrl.isEmpty()) return;
        item.status.set("Deploying...");
        String endpoint = tempOnly ? "/admin/routes/hotload-temp" : "/admin/routes/upload";

        String baseUrl = targetUrl.endsWith("/") ? targetUrl.substring(0, targetUrl.length() - 1) : targetUrl;
        String fullUrl = baseUrl + endpoint;
        HttpClient client = HttpClient.newBuilder().build();

        try {
            JSONObject json = new JSONObject();
            json.put("routeId", item.getId());
            json.put("version", item.getVersion());
            json.put("content", item.getRawContent());
            json.put("format", item.getTypeOrFormat());
            json.put("enabled", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        javafx.application.Platform.runLater(() -> {
                            if (res.statusCode() == 200 || res.statusCode() == 201) {
                                item.status.set("Running");
                                txtConsole.appendText("[SUCCESS] " + item.getId() + " - Remote response: " + res.body() + "\n");
                            } else {
                                item.status.set("Failed (" + res.statusCode() + ")");
                                txtConsole.appendText("[ERROR] " + item.getId() + " - Status " + res.statusCode() + " - Body: " + res.body() + "\n");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        javafx.application.Platform.runLater(() -> {
                            item.status.set("Failed (Connection Error)");
                            txtConsole.appendText("[ERROR] " + item.getId() + " - Connection Exception: " + ex.getMessage() + "\n");
                        });
                        return null;
                    });

        } catch (Exception ex) {
            item.status.set("Failed (Internal Error)");
            txtConsole.appendText("[ERROR] " + item.getId() + " - Exception: " + ex.getMessage() + "\n");
        }
    }

    private static void stopSingleItem(DeployItem item, String targetUrl, TextArea txtConsole) {
        if (targetUrl == null || targetUrl.isEmpty() || item.isTransformer) return;

        String baseUrl = targetUrl.endsWith("/") ? targetUrl.substring(0, targetUrl.length() - 1) : targetUrl;
        String stopUrl = baseUrl + "/admin/routes/stop";

        txtConsole.appendText("[STOP REQUESTED] Route: " + item.getId() + (item.getAllRouteIds().isEmpty() ? "" : " (" + String.join(", ", item.getAllRouteIds()) + ")") + "\n");
        HttpClient client = HttpClient.newBuilder().build();

        try {
            JSONObject json = new JSONObject();
            if (item.getAllRouteIds().isEmpty()) {
                json.put("routeId", item.getId());
            } else {
                // Send the list of IDs
                json.put("routeId", item.getAllRouteIds());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(stopUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        javafx.application.Platform.runLater(() -> {
                            if (res.statusCode() == 200) {
                                item.status.set("Stopped");
                                txtConsole.appendText("[SUCCESS] Route " + item.getId() + " stopped remotely.\n");
                            } else {
                                txtConsole.appendText("[ERROR] Stop failed: " + res.body() + "\n");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        javafx.application.Platform.runLater(() -> {
                            txtConsole.appendText("[ERROR] Stop connection error: " + ex.getMessage() + "\n");
                        });
                        return null;
                    });
        } catch (Exception ex) {
            txtConsole.appendText("[ERROR] Exception: " + ex.getMessage() + "\n");
        }
    }

    private static void copySingleFile(DeployItem item, String targetUrl, String serverPath, TextArea txtConsole) {
        if (targetUrl == null || targetUrl.isEmpty() || serverPath == null || serverPath.isEmpty()) return;
        item.status.set("Copying...");
        
        String baseUrl = targetUrl.endsWith("/") ? targetUrl.substring(0, targetUrl.length() - 1) : targetUrl;
        String fullUrl = baseUrl + "/admin/files/upload";
        HttpClient client = HttpClient.newBuilder().build();

        try {
            String finalTargetDir = serverPath;
            if (item.isTransformer) {
                if (!finalTargetDir.endsWith("/")) {
                    finalTargetDir += "/";
                }
                finalTargetDir += item.getTypeOrFormat() + "/" + item.getId();
            }

            JSONObject json = new JSONObject();
            json.put("fileName", item.getFilename());
            json.put("content", item.getRawContent());
            json.put("targetDir", finalTargetDir);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        javafx.application.Platform.runLater(() -> {
                            if (res.statusCode() == 200 || res.statusCode() == 201) {
                                item.status.set("Copied");
                                try {
                                    JSONObject resp = new JSONObject(res.body());
                                    String copiedPath = resp.optString("copiedPath", serverPath + "/" + item.getFilename());
                                    txtConsole.appendText("[SUCCESS] File " + item.getId() + " copied to: " + copiedPath + "\n");
                                } catch (Exception ex) {
                                    txtConsole.appendText("[SUCCESS] File " + item.getId() + " copied to: " + serverPath + "/" + item.getFilename() + "\n");
                                }
                            } else {
                                item.status.set("Failed (" + res.statusCode() + ")");
                                txtConsole.appendText("[ERROR] Copy failed for " + item.getId() + " - Status " + res.statusCode() + " - Body: " + res.body() + "\n");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        javafx.application.Platform.runLater(() -> {
                            item.status.set("Failed (Connection Error)");
                            txtConsole.appendText("[ERROR] " + item.getId() + " - Connection Exception: " + ex.getMessage() + "\n");
                        });
                        return null;
                    });

        } catch (Exception ex) {
            item.status.set("Failed (Internal Error)");
            txtConsole.appendText("[ERROR] " + item.getId() + " - Exception: " + ex.getMessage() + "\n");
        }
    }

    private static List<DeployItem> parseRoutes(File baseDir, Set<File> files) {
        List<DeployItem> results = new ArrayList<>();
        Pattern headerIdPattern = Pattern.compile("(?i)^#\\s*ID\\s*:\\s*(.*)$", Pattern.MULTILINE);
        Pattern versionPattern = Pattern.compile("(?i)^#\\s*Version\\s*:\\s*([0-9.]+)", Pattern.MULTILINE);
        
        // Robust regex to find all route IDs in YAML, Java, and XML
        Pattern yamlIdPattern = Pattern.compile("(?m)^\\s*id:\\s*[\"']?([^\"'\\n\\s]+)[\"']?");
        Pattern javaIdPattern = Pattern.compile("\\.routeId\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");
        Pattern xmlIdPattern = Pattern.compile("<route\\s+id=\"([^\"]+)\"");

        for (File f : files) {
            if (!f.isFile()) continue;
            try {
                String content = Files.readString(f.toPath());
                String logicalId = f.getName().replace(".camel.yaml", "").replace(".yaml", "");
                
                // 1. Get Logical ID from Header (if exists)
                Matcher mHeadId = headerIdPattern.matcher(content);
                if (mHeadId.find()) logicalId = mHeadId.group(1).trim();

                // 2. Extract ALL internal route IDs
                List<String> allRouteIds = new ArrayList<>();
                String nameLower = f.getName().toLowerCase();
                
                if (nameLower.endsWith(".yaml") || nameLower.endsWith(".yml")) {
                    Matcher m = yamlIdPattern.matcher(content);
                    while (m.find()) {
                        String rid = m.group(1).trim();
                        if (!allRouteIds.contains(rid)) allRouteIds.add(rid);
                    }
                } else if (nameLower.endsWith(".java")) {
                    Matcher m = javaIdPattern.matcher(content);
                    while (m.find()) {
                        String rid = m.group(1).trim();
                        if (!allRouteIds.contains(rid)) allRouteIds.add(rid);
                    }
                } else if (nameLower.endsWith(".xml")) {
                    Matcher m = xmlIdPattern.matcher(content);
                    while (m.find()) {
                        String rid = m.group(1).trim();
                        if (!allRouteIds.contains(rid)) allRouteIds.add(rid);
                    }
                }

                double version = 1.0;
                Matcher mVer = versionPattern.matcher(content);
                if (mVer.find()) {
                    try {
                        version = Double.parseDouble(mVer.group(1).trim());
                    } catch (Exception ignored) {}
                }

                String format = "yaml";
                if (nameLower.endsWith(".xml")) format = "xml";
                else if (nameLower.endsWith(".java")) format = "java";

                results.add(new DeployItem(logicalId, f.getName(), format, version, content, false, allRouteIds));
            } catch (Exception e) {
                results.add(new DeployItem("Error", f.getName(), "yaml", 1.0, "", false, null));
            }
        }
        return results;
    }

    private static List<DeployItem> parseTransformations(File baseDir, Set<File> folders) {
        List<DeployItem> results = new ArrayList<>();

        for (File folder : folders) {
            if (!folder.isDirectory()) continue;
            File configFile = new File(folder, "transformation.json");
            if (!configFile.exists()) continue;

            try {
                String configContent = Files.readString(configFile.toPath());
                JSONObject config = new JSONObject(configContent);

                String defaultId = folder.getName();
                String id = config.optString("id", defaultId);
                String type = config.optString("type", "unknown");
                double version = config.optDouble("version", 1.0);

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

                results.add(new DeployItem(id, logicFileName, type, version, logicContent, true, null));
            } catch (Exception e) {
                results.add(new DeployItem("Error", folder.getName(), "unknown", 1.0, "", true, null));
            }
        }
        return results;
    }

    private static String loadApiUrl(File baseDir) {
        if (baseDir != null) {
            File propsFile = new File(baseDir, "application.properties");
            if (propsFile.exists()) {
                try {
                    java.util.Properties props = new java.util.Properties();
                    try (java.io.FileReader reader = new java.io.FileReader(propsFile)) {
                        props.load(reader);
                    }
                    String val = props.getProperty("API_URL");
                    if (val != null && !val.trim().isEmpty()) {
                        return val.trim();
                    }
                } catch (Exception ignored) {}
            }
        }
        return "http://localhost:8080";
    }
}
