package com.routebuilder.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class KameletStudioWindow {

    private final Stage stage;
    private final File baseDir;
    private final File workspaceRoot;

    private TreeView<File> treeView;
    private YamlEditorPane editorPane;
    private DiagramPane diagramPane;

    private File currentFile = null;
    private boolean isUpdatingFromDiagram = false;

    public KameletStudioWindow(File baseDir) {
        this.baseDir = baseDir;
        this.workspaceRoot = new File(baseDir, "kamelets");
        if (!workspaceRoot.exists()) {
            workspaceRoot.mkdirs();
        }
        
        initializeTutorialsAndExamples();

        this.stage = new Stage();
        initUI();
    }

    public static void show(File baseDir) {
        KameletStudioWindow window = new KameletStudioWindow(baseDir);
        window.stage.show();
    }

    private void initializeTutorialsAndExamples() {
        // Create chapter folders
        File ch1 = new File(workspaceRoot, "tutorial/chapter-1-basics");
        File ch2 = new File(workspaceRoot, "tutorial/chapter-2-databases");
        File ch3 = new File(workspaceRoot, "tutorial/chapter-3-messaging");
        File ch4 = new File(workspaceRoot, "tutorial/chapter-4-transactions");
        File examples = new File(workspaceRoot, "examples");

        ch1.mkdirs();
        ch2.mkdirs();
        ch3.mkdirs();
        ch4.mkdirs();
        examples.mkdirs();

        try {
            // Write Chapter 1 files
            File timerSource = new File(ch1, "kamelet-studio-timer-source.kamelet.yaml");
            if (!timerSource.exists()) {
                Files.writeString(timerSource.toPath(), getTimerSourceYaml());
            }

            File logSink = new File(ch1, "kamelet-studio-log-sink.kamelet.yaml");
            if (!logSink.exists()) {
                Files.writeString(logSink.toPath(), getLogSinkYaml());
            }

            // Copy other standard kamelets from classpath resources into corresponding folders
            copyKameletFromClasspath("kamelet-studio-mongodb-source.kamelet.yaml", ch2);
            copyKameletFromClasspath("kamelet-studio-mongodb-sink.kamelet.yaml", ch2);
            copyKameletFromClasspath("kamelet-studio-mongodb-action.kamelet.yaml", ch2);
            copyKameletFromClasspath("kamelet-studio-sql-action.kamelet.yaml", ch2);

            copyKameletFromClasspath("kamelet-studio-solace-source.kamelet.yaml", ch3);

            copyKameletFromClasspath("kamelet-studio-solace-xa-source.kamelet.yaml", ch4);

            // Write examples
            File ex1 = new File(examples, "timer-to-log.yaml");
            if (!ex1.exists()) {
                Files.writeString(ex1.toPath(), getTimerToLogExampleYaml());
            }

            File ex2 = new File(examples, "mongodb-to-mq.yaml");
            if (!ex2.exists()) {
                Files.writeString(ex2.toPath(), getMongodbToMqExampleYaml());
            }

            File ex3 = new File(examples, "solace-to-sql.yaml");
            if (!ex3.exists()) {
                Files.writeString(ex3.toPath(), getSolaceToSqlExampleYaml());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyKameletFromClasspath(String filename, File destDir) {
        File destFile = new File(destDir, filename);
        if (destFile.exists()) {
            return;
        }
        try (java.io.InputStream in = getClass().getResourceAsStream("/kamelets/" + filename)) {
            if (in != null) {
                Files.copy(in, destFile.toPath());
            } else {
                System.err.println("Kamelet template resource not found on classpath: /kamelets/" + filename);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        stage.setTitle("Custom Kamelet Designer Studio");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.getStyleClass().add(RouteBuilderApp.currentThemeClass);

        // Toolbar
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("editor-toolbar");

        Button btnNew = new Button("New", new FontIcon("fas-plus"));
        btnNew.getStyleClass().addAll("toolbar-btn", "btn-new");
        btnNew.setOnAction(e -> createNewKamelet());

        Button btnSave = new Button("Save", new FontIcon("fas-save"));
        btnSave.getStyleClass().addAll("toolbar-btn", "btn-save");
        btnSave.setOnAction(e -> saveCurrentFile());

        Button btnRefresh = new Button("Refresh", new FontIcon("fas-sync"));
        btnRefresh.getStyleClass().add("toolbar-btn");
        btnRefresh.setOnAction(e -> refreshTree());

        Button btnTest = new Button("Test", new FontIcon("fas-play"));
        btnTest.getStyleClass().addAll("toolbar-btn", "btn-play");
        btnTest.setOnAction(e -> testKamelet());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnHelp = new Button("Help Guide", new FontIcon("fas-question-circle"));
        btnHelp.getStyleClass().add("toolbar-btn");
        btnHelp.setOnAction(e -> new RouteBuilderHelpWindow("Kamelet Studio", "Kamelet").show());

        Button btnClose = new Button("Close", new FontIcon("fas-times"));
        btnClose.getStyleClass().add("toolbar-btn");
        btnClose.setOnAction(e -> stage.close());

        toolBar.getItems().addAll(btnNew, btnSave, btnRefresh, new Separator(), btnTest, spacer, btnHelp, btnClose);
        root.setTop(toolBar);

        // Sidebar (Explorer)
        VBox sidebar = new VBox(5);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(10));
        sidebar.getStyleClass().add("studio-sidebar");

        Label lblExplorer = new Label("TUTORIALS & KAMELETS");
        lblExplorer.getStyleClass().add("studio-explorer-label");

        TextField filterField = new TextField();
        filterField.setPromptText("Filter...");
        filterField.getStyleClass().add("studio-search-field");
        filterField.textProperty().addListener((obs, oldText, newText) -> filterTree(newText));

        treeView = new TreeView<>();
        treeView.getStyleClass().add("sidebar-tree-view");
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue().isFile()) {
                openFile(newVal.getValue());
            }
        });

        // Cell Factory for custom icons
        treeView.setCellFactory(tv -> {
            TreeCell<File> cell = new TreeCell<>() {
                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (item.isDirectory()) {
                            setText(item.getName());
                            setGraphic(new FontIcon("fas-folder"));
                        } else {
                            String name = item.getName();
                            if (name.endsWith(".kamelet.yaml")) {
                                setText(name.replace(".kamelet.yaml", ""));
                                setGraphic(new FontIcon("fas-puzzle-piece"));
                            } else {
                                setText(name);
                                setGraphic(new FontIcon("fas-file-alt"));
                            }
                        }
                    }
                }
            };

            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete", new FontIcon("fas-trash"));
            deleteItem.setOnAction(e -> {
                File f = cell.getItem();
                if (f != null) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete " + f.getName() + "?", ButtonType.YES, ButtonType.NO);
                    RouteBuilderApp.themeDialog(alert);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            f.delete();
                            if (f.equals(currentFile)) {
                                currentFile = null;
                                editorPane.setText("");
                                diagramPane.renderDiagram("");
                            }
                            refreshTree();
                        }
                    });
                }
            });
            contextMenu.getItems().add(deleteItem);

            cell.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty || (cell.getItem() != null && cell.getItem().isDirectory())) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell;
        });

        sidebar.getChildren().addAll(lblExplorer, filterField, treeView);

        // Center split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);

        editorPane = new YamlEditorPane(text -> {
            if (isUpdatingFromDiagram) return;
            Platform.runLater(() -> {
                try {
                    String diagramYaml = convertKameletToDiagramYaml(text);
                    diagramPane.renderDiagram(diagramYaml);
                } catch (Exception ignored) {}
            });
        }, () -> {
            refreshTree();
        });
        editorPane.setPrefWidth(550);

        diagramPane = new DiagramPane(theme -> {}, updatedDiagramYaml -> {
            if (currentFile == null) return;
            isUpdatingFromDiagram = true;
            try {
                String currentContent = editorPane.getText();
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                JsonNode rootNode = mapper.readTree(currentContent);
                if (rootNode != null && rootNode.isArray()) {
                    // If it is a route, set the diagram YAML directly
                    editorPane.setText(updatedDiagramYaml);
                } else {
                    // Merge diagram YAML back into Kamelet spec.template
                    String newKameletYaml = mergeDiagramIntoKameletYaml(currentContent, updatedDiagramYaml);
                    editorPane.setText(newKameletYaml);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isUpdatingFromDiagram = false;
            }
        });

        splitPane.getItems().addAll(editorPane, diagramPane);
        splitPane.setDividerPositions(0.45);

        SplitPane mainSplit = new SplitPane();
        mainSplit.getItems().addAll(sidebar, splitPane);
        mainSplit.setDividerPositions(0.20);
        root.setCenter(mainSplit);

        Scene scene = new Scene(root, 1400, 850);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        if (RouteBuilderApp.currentDynamicCssUri != null) {
            scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
        }
        stage.setScene(scene);

        RouteBuilderApp.themedRoots.add(root);
        stage.setOnHidden(e -> RouteBuilderApp.themedRoots.remove(root));

        refreshTree();
    }

    private void refreshTree() {
        TreeItem<File> rootItem = new TreeItem<>(workspaceRoot);
        rootItem.setExpanded(true);

        // Add root-level files
        File[] files = workspaceRoot.listFiles((dir, name) -> name.endsWith(".kamelet.yaml"));
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File f : files) {
                rootItem.getChildren().add(new TreeItem<>(f));
            }
        }

        // Add subdirectories recursively
        File[] dirs = workspaceRoot.listFiles(File::isDirectory);
        if (dirs != null) {
            Arrays.sort(dirs, Comparator.comparing(File::getName));
            for (File d : dirs) {
                buildTreeRecursively(d, rootItem);
            }
        }

        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
    }

    private void buildTreeRecursively(File file, TreeItem<File> parentItem) {
        TreeItem<File> dirItem = new TreeItem<>(file);
        dirItem.setExpanded(true);

        File[] children = file.listFiles();
        if (children != null) {
            Arrays.sort(children, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareTo(f2.getName());
            });
            for (File child : children) {
                if (child.isDirectory()) {
                    buildTreeRecursively(child, dirItem);
                } else if (child.getName().endsWith(".kamelet.yaml") || child.getName().endsWith(".yaml") || child.getName().endsWith(".yml")) {
                    dirItem.getChildren().add(new TreeItem<>(child));
                }
            }
        }
        if (!dirItem.getChildren().isEmpty()) {
            parentItem.getChildren().add(dirItem);
        }
    }

    private void filterTree(String query) {
        if (query == null || query.trim().isEmpty()) {
            refreshTree();
            return;
        }
        String lower = query.toLowerCase();
        TreeItem<File> rootItem = new TreeItem<>(workspaceRoot);
        filterTreeRecursively(workspaceRoot, rootItem, lower);
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
    }

    private void filterTreeRecursively(File file, TreeItem<File> parentItem, String query) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    TreeItem<File> dirItem = new TreeItem<>(child);
                    dirItem.setExpanded(true);
                    filterTreeRecursively(child, dirItem, query);
                    if (!dirItem.getChildren().isEmpty()) {
                        parentItem.getChildren().add(dirItem);
                    }
                } else if (child.getName().toLowerCase().contains(query)) {
                    if (child.getName().endsWith(".kamelet.yaml") || child.getName().endsWith(".yaml") || child.getName().endsWith(".yml")) {
                        parentItem.getChildren().add(new TreeItem<>(child));
                    }
                }
            }
        }
    }

    private void openFile(File f) {
        this.currentFile = f;
        diagramPane.setCurrentFile(f);
        editorPane.loadFile(f);
        try {
            String yamlContent = Files.readString(f.toPath());
            String diagramYaml = convertKameletToDiagramYaml(yamlContent);
            diagramPane.renderDiagram(diagramYaml);
        } catch (IOException ignored) {}
    }

    private void saveCurrentFile() {
        if (currentFile != null) {
            editorPane.saveFile();
        }
    }

    private void createNewKamelet() {
        TextInputDialog dialog = new TextInputDialog("my-custom-kamelet");
        RouteBuilderApp.themeDialog(dialog);
        dialog.setTitle("New Kamelet");
        dialog.setHeaderText("Create a new Custom Kamelet Definition");
        dialog.setContentText("Enter Kamelet Name (lowercase, kebab-case):");
        dialog.showAndWait().ifPresent(name -> {
            String cleanName = name.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
            File f = new File(workspaceRoot, cleanName + ".kamelet.yaml");
            if (f.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "A Kamelet named " + cleanName + " already exists!");
                RouteBuilderApp.themeDialog(alert);
                alert.showAndWait();
                return;
            }
            try {
                String template = "apiVersion: camel.apache.org/v1\n" +
                        "kind: Kamelet\n" +
                        "metadata:\n" +
                        "  name: " + cleanName + "\n" +
                        "  labels:\n" +
                        "    camel.apache.org/kamelet.type: \"action\"\n" +
                        "  annotations:\n" +
                        "    camel.apache.org/kamelet.title: \"" + toTitleCase(cleanName) + "\"\n" +
                        "    camel.apache.org/kamelet.version: \"1.0.0\"\n" +
                        "spec:\n" +
                        "  definition:\n" +
                        "    title: \"" + toTitleCase(cleanName) + "\"\n" +
                        "    description: \"Custom kamelet action\"\n" +
                        "    properties:\n" +
                        "      message:\n" +
                        "        title: Message\n" +
                        "        type: string\n" +
                        "        default: \"Hello from Custom Kamelet\"\n" +
                        "  template:\n" +
                        "    from:\n" +
                        "      uri: \"kamelet:source\"\n" +
                        "      steps:\n" +
                        "        - setBody:\n" +
                        "            constant: \"{{message}}\"\n" +
                        "        - to: \"kamelet:sink\"\n";
                Files.writeString(f.toPath(), template);
                refreshTree();
                openFile(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private String toTitleCase(String kebab) {
        String[] parts = kebab.split("-");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.length() > 0) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String convertKameletToDiagramYaml(String yamlContent) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JsonNode root = mapper.readTree(yamlContent);
            if (root == null) {
                return "";
            }
            if (root.isArray()) {
                // It is already a route! Render it directly
                return yamlContent;
            }
            if (!root.has("spec")) {
                return "";
            }
            JsonNode spec = root.get("spec");
            if (!spec.has("template")) {
                return "";
            }
            JsonNode template = spec.get("template");

            ArrayNode diagramArray = mapper.createArrayNode();

            // Extract beans
            if (template.has("beans")) {
                ObjectNode beansNode = mapper.createObjectNode();
                beansNode.set("beans", template.get("beans"));
                diagramArray.add(beansNode);
            }

            // Extract route
            if (template.has("from")) {
                ObjectNode routeNode = mapper.createObjectNode();
                ObjectNode innerRoute = mapper.createObjectNode();
                innerRoute.set("from", template.get("from"));
                routeNode.set("route", innerRoute);
                diagramArray.add(routeNode);
            }

            return mapper.writeValueAsString(diagramArray);
        } catch (Exception e) {
            return "";
        }
    }

    private String mergeDiagramIntoKameletYaml(String currentKameletYaml, String diagramYaml) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JsonNode kameletRoot = mapper.readTree(currentKameletYaml);
            if (kameletRoot == null) {
                return currentKameletYaml;
            }

            ObjectNode specNode = (ObjectNode) kameletRoot.get("spec");
            if (specNode == null) {
                specNode = mapper.createObjectNode();
                ((ObjectNode) kameletRoot).set("spec", specNode);
            }

            ObjectNode templateNode = (ObjectNode) specNode.get("template");
            if (templateNode == null) {
                templateNode = mapper.createObjectNode();
                specNode.set("template", templateNode);
            }

            JsonNode diagramRoot = mapper.readTree(diagramYaml);
            if (diagramRoot != null && diagramRoot.isArray()) {
                templateNode.remove("beans");
                templateNode.remove("from");

                for (JsonNode item : diagramRoot) {
                    if (item.has("beans")) {
                        templateNode.set("beans", item.get("beans"));
                    } else if (item.has("route")) {
                        JsonNode innerRoute = item.get("route");
                        if (innerRoute.has("from")) {
                            templateNode.set("from", innerRoute.get("from"));
                        }
                    }
                }
            }

            return mapper.writeValueAsString(kameletRoot);
        } catch (Exception e) {
            e.printStackTrace();
            return currentKameletYaml;
        }
    }

    private void testKamelet() {
        if (currentFile == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No Kamelet open for testing.");
            RouteBuilderApp.themeDialog(alert);
            alert.showAndWait();
            return;
        }

        saveCurrentFile();

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JsonNode root = mapper.readTree(currentFile);
            if (root == null) {
                return;
            }

            if (root.isArray()) {
                // It is a route! Test it as a normal route.
                launchJbangRoute(currentFile);
                return;
            }

            String name = root.has("metadata") && root.get("metadata").has("name") 
                    ? root.get("metadata").get("name").asText() : "custom-kamelet";
            String type = "action";
            if (root.has("metadata") && root.get("metadata").has("labels")) {
                JsonNode labels = root.get("metadata").get("labels");
                if (labels.has("camel.apache.org/kamelet.type")) {
                    type = labels.get("camel.apache.org/kamelet.type").asText();
                }
            }

            JsonNode spec = root.get("spec");
            JsonNode properties = spec.has("definition") && spec.get("definition").has("properties")
                    ? spec.get("definition").get("properties") : null;
            JsonNode requiredNode = spec.has("definition") && spec.get("definition").has("required")
                    ? spec.get("definition").get("required") : null;

            Set<String> requiredSet = new HashSet<>();
            if (requiredNode != null && requiredNode.isArray()) {
                for (JsonNode req : requiredNode) {
                    requiredSet.add(req.asText());
                }
            }

            // Create testing configuration dialog
            Stage testStage = new Stage();
            testStage.initModality(Modality.APPLICATION_MODAL);
            testStage.setTitle("Configure Test - " + name);

            VBox testRoot = new VBox(15);
            testRoot.setPadding(new Insets(15));
            testRoot.getStyleClass().add("app-root");
            testRoot.getStyleClass().add(RouteBuilderApp.currentThemeClass);

            Label title = new Label("Kamelet Test Configuration");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-text-background-color;");
            testRoot.getChildren().add(title);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            testRoot.getChildren().add(grid);

            Map<String, TextField> textFields = new HashMap<>();

            int rowIndex = 0;
            if (properties != null && properties.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String propKey = entry.getKey();
                    JsonNode propVal = entry.getValue();

                    Label lblProp = new Label(propKey + (requiredSet.contains(propKey) ? " *" : ""));
                    if (requiredSet.contains(propKey)) {
                        lblProp.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-text-background-color;");
                    } else {
                        lblProp.setStyle("-fx-text-fill: -fx-text-background-color;");
                    }
                    grid.add(lblProp, 0, rowIndex);

                    TextField tf = new TextField();
                    if (propVal.has("default")) {
                        tf.setText(propVal.get("default").asText());
                    }
                    tf.setPrefWidth(250);
                    grid.add(tf, 1, rowIndex);
                    textFields.put(propKey, tf);

                    String desc = propVal.has("description") ? propVal.get("description").asText() : "";
                    if (!desc.isEmpty()) {
                        Label lblDesc = new Label(desc);
                        lblDesc.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
                        grid.add(lblDesc, 2, rowIndex);
                    }

                    rowIndex++;
                }
            }

            if (rowIndex == 0) {
                testRoot.getChildren().add(new Label("No parameters to configure."));
            }

            HBox btnBox = new HBox(10);
            btnBox.setAlignment(Pos.CENTER_RIGHT);

            Button btnRun = new Button("Run Test", new FontIcon("fas-play-circle"));
            btnRun.getStyleClass().addAll("toolbar-btn", "btn-play");

            final String finalType = type;
            final String finalName = name;

            btnRun.setOnAction(evt -> {
                // Collect params
                StringBuilder query = new StringBuilder();
                boolean missingRequired = false;
                for (Map.Entry<String, TextField> entry : textFields.entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue().getText().trim();
                    if (val.isEmpty() && requiredSet.contains(key)) {
                        missingRequired = true;
                    }
                    if (!val.isEmpty()) {
                        if (query.length() > 0) query.append("&");
                        query.append(key).append("=").append(val);
                    }
                }

                if (missingRequired) {
                    Alert warn = new Alert(Alert.AlertType.WARNING, "Please fill in all required fields (*)");
                    RouteBuilderApp.themeDialog(warn);
                    warn.showAndWait();
                    return;
                }

                testStage.close();
                launchJbangTest(finalName, finalType, query.toString());
            });

            Button btnCancel = new Button("Cancel", new FontIcon("fas-times"));
            btnCancel.getStyleClass().add("toolbar-btn");
            btnCancel.setOnAction(evt -> testStage.close());

            btnBox.getChildren().addAll(btnRun, btnCancel);
            testRoot.getChildren().add(btnBox);

            Scene testScene = new Scene(testRoot, 600, Math.min(500, 200 + rowIndex * 40));
            testScene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            if (RouteBuilderApp.currentDynamicCssUri != null) {
                testScene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
            }
            testStage.setScene(testScene);
            testStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchJbangRoute(File routeFile) {
        try {
            List<String> command = new ArrayList<>();
            command.add(RouteBuilderApp.getJbangExecutable());
            command.add("--main=main.CamelJBang");
            String catalogPath = RouteBuilderApp.getJbangCatalog();
            if (catalogPath != null) {
                command.add("--catalog=" + catalogPath);
            }
            command.add("camel");
            command.add("run");
            command.add("--port=0");
            
            // JBang needs relative path to the route file
            String relRoute = baseDir.toPath().toAbsolutePath().relativize(routeFile.toPath().toAbsolutePath()).toString().replace("\\", "/");
            command.add(relRoute);

            // Add enabled catalog dependencies
            for (String dep : DependencyCatalogWindow.getEnabledDependencies(baseDir)) {
                command.add("--dependency=" + dep);
            }

            command.add("--runtime=main");

            System.out.println("Running route command: " + String.join(" ", command));

            RouteBuilderApp.instance.stopCurrentProcess();

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("TERM", "xterm-256color");
            pb.directory(baseDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            RouteBuilderApp.instance.setRunnerProcess(p);
            RouteBuilderApp.instance.showConsole(p, "Running Route: " + routeFile.getName());

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to run example route: " + e.getMessage());
            RouteBuilderApp.themeDialog(alert);
            alert.showAndWait();
        }
    }

    private void launchJbangTest(String kameletName, String type, String queryParams) {
        // Construct the test yaml route in baseDir
        File testFile = new File(baseDir, "test-" + kameletName + "-route.yaml");
        try {
            String uri = "kamelet:" + kameletName + (queryParams.isEmpty() ? "" : "?" + queryParams);
            String testRouteYaml;
            if ("source".equalsIgnoreCase(type)) {
                testRouteYaml = "- route:\n" +
                        "    id: \"test-kamelet-source\"\n" +
                        "    from:\n" +
                        "      uri: \"" + uri + "\"\n" +
                        "      steps:\n" +
                        "        - log:\n" +
                        "            message: \"KAMELET-TEST: Received message body: ${body}\"\n";
            } else {
                testRouteYaml = "- route:\n" +
                        "    id: \"test-kamelet-sink-action\"\n" +
                        "    from:\n" +
                        "      uri: \"timer:test-timer?period=5000\"\n" +
                        "      steps:\n" +
                        "        - setBody:\n" +
                        "            constant: \"Test message from Kamelet Designer\"\n" +
                        "        - to:\n" +
                        "            uri: \"" + uri + "\"\n" +
                        "        - log:\n" +
                        "            message: \"KAMELET-TEST: Message sent to Kamelet successfully. Response body: ${body}\"\n";
            }

            Files.writeString(testFile.toPath(), testRouteYaml);

            // Command
            List<String> command = new ArrayList<>();
            command.add(RouteBuilderApp.getJbangExecutable());
            command.add("--main=main.CamelJBang");
            String catalogPath = RouteBuilderApp.getJbangCatalog();
            if (catalogPath != null) {
                command.add("--catalog=" + catalogPath);
            }
            command.add("camel");
            command.add("run");
            command.add("--port=0");
            command.add(testFile.getName());

            // Add auto-inject dependencies from DependencyCatalog
            for (String dep : DependencyCatalogWindow.getEnabledDependencies(baseDir)) {
                command.add("--dependency=" + dep);
            }

            command.add("--runtime=main");

            System.out.println("Running test command: " + String.join(" ", command));

            RouteBuilderApp.instance.stopCurrentProcess();

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("TERM", "xterm-256color");
            pb.directory(baseDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            RouteBuilderApp.instance.setRunnerProcess(p);
            RouteBuilderApp.instance.showConsole(p, "Testing Kamelet: " + kameletName);

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to run Kamelet test: " + e.getMessage());
            RouteBuilderApp.themeDialog(alert);
            alert.showAndWait();
        }
    }

    private String getTimerSourceYaml() {
        return "apiVersion: camel.apache.org/v1\n" +
                "kind: Kamelet\n" +
                "metadata:\n" +
                "  name: kamelet-studio-timer-source\n" +
                "  labels:\n" +
                "    camel.apache.org/kamelet.type: \"source\"\n" +
                "  annotations:\n" +
                "    camel.apache.org/kamelet.title: \"Kamelet Studio Timer Source\"\n" +
                "    camel.apache.org/kamelet.version: \"1.0.0\"\n" +
                "spec:\n" +
                "  definition:\n" +
                "    title: \"Timer Source\"\n" +
                "    description: \"Produces periodic messages at a set interval.\"\n" +
                "    properties:\n" +
                "      period:\n" +
                "        type: integer\n" +
                "        default: 1000\n" +
                "        description: \"The period in milliseconds between ticks.\"\n" +
                "  template:\n" +
                "    from:\n" +
                "      uri: \"timer:tick?period={{period}}\"\n" +
                "      steps:\n" +
                "        - setBody:\n" +
                "            constant: \"Hello from Timer Source!\"\n" +
                "        - to: \"kamelet:sink\"\n";
    }

    private String getLogSinkYaml() {
        return "apiVersion: camel.apache.org/v1\n" +
                "kind: Kamelet\n" +
                "metadata:\n" +
                "  name: kamelet-studio-log-sink\n" +
                "  labels:\n" +
                "    camel.apache.org/kamelet.type: \"sink\"\n" +
                "  annotations:\n" +
                "    camel.apache.org/kamelet.title: \"Kamelet Studio Log Sink\"\n" +
                "    camel.apache.org/kamelet.version: \"1.0.0\"\n" +
                "spec:\n" +
                "  definition:\n" +
                "    title: \"Log Sink\"\n" +
                "    description: \"Logs the incoming message body and headers.\"\n" +
                "    properties:\n" +
                "      showHeaders:\n" +
                "        type: boolean\n" +
                "        default: false\n" +
                "        description: \"Show headers in output.\"\n" +
                "  template:\n" +
                "    from:\n" +
                "      uri: \"kamelet:source\"\n" +
                "      steps:\n" +
                "        - log:\n" +
                "            message: \"Incoming Body: ${body}\"\n" +
                "            showHeaders: \"{{showHeaders}}\"\n";
    }

    private String getTimerToLogExampleYaml() {
        return "# camel-k: dependency=mvn:org.apache.camel.kamelet:camel-kamelet:3.20.1\n" +
                "- route:\n" +
                "    id: timer-to-log-example\n" +
                "    from:\n" +
                "      uri: \"kamelet:kamelet-studio-timer-source?period=2000\"\n" +
                "      steps:\n" +
                "        - to: \"kamelet:kamelet-studio-log-sink?showHeaders=true\"\n";
    }

    private String getMongodbToMqExampleYaml() {
        return "# camel-k: dependency=mvn:org.apache.camel.kamelet:camel-kamelet:3.20.1\n" +
                "- route:\n" +
                "    id: mongodb-to-mq-example\n" +
                "    from:\n" +
                "      uri: \"kamelet:kamelet-studio-mongodb-source?database=finance&collection=transactions&consumerType=changeStreams\"\n" +
                "      steps:\n" +
                "        - to: \"kamelet:kamelet-studio-ibmmq-sink?queuename=DEV.QUEUE.1\"\n";
    }

    private String getSolaceToSqlExampleYaml() {
        return "# camel-k: dependency=mvn:org.apache.camel.kamelet:camel-kamelet:3.20.1\n" +
                "- route:\n" +
                "    id: solace-to-sql-example\n" +
                "    from:\n" +
                "      uri: \"kamelet:kamelet-studio-solace-source?queuename=orders\"\n" +
                "      steps:\n" +
                "        - to: \"kamelet:kamelet-studio-sql-action?query=INSERT INTO orders (id, body) VALUES (#, #)\"\n" +
                "        - to: \"kamelet:kamelet-studio-log-sink?showHeaders=false\"\n";
    }
}
