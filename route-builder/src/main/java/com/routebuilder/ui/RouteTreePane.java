package com.routebuilder.ui;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Consumer;

public class RouteTreePane extends VBox {

    private TreeView<File> treeView;
    private TreeItem<File> rootItem;
    private File baseDirectory;
    private Consumer<File> onFileSelected;
    private java.util.function.BiConsumer<File, String> onRunSelected;
    private boolean showHidden = false;
    private Label title;

    public void setOnRunSelected(java.util.function.BiConsumer<File, String> onRunSelected) {
        this.onRunSelected = onRunSelected;
    }

    private static java.util.List<File> clipboardFiles = new java.util.ArrayList<>();
    private static boolean isCutAction = false;

    public RouteTreePane(Consumer<File> onFileSelected) {
        this.onFileSelected = onFileSelected;
        
        getStyleClass().add("route-tree-pane");
        
        title = new Label("EXPLORER: ROUTES");
        title.getStyleClass().add("pane-title");

        HBox toolbar = new HBox(5);
        toolbar.getStyleClass().add("editor-toolbar");

        Button btnNewFile = new Button();
        btnNewFile.setGraphic(new FontIcon("fas-file-medical"));
        btnNewFile.setTooltip(new Tooltip("New File"));
        btnNewFile.getStyleClass().addAll("editor-btn", "btn-new-file");
        btnNewFile.setOnAction(e -> createNewItem(false));

        Button btnNewFolder = new Button();
        btnNewFolder.setGraphic(new FontIcon("fas-folder-plus"));
        btnNewFolder.setTooltip(new Tooltip("New Folder"));
        btnNewFolder.getStyleClass().addAll("editor-btn", "btn-new-folder");
        btnNewFolder.setOnAction(e -> createNewItem(true));

        Button btnRefresh = new Button();
        btnRefresh.setGraphic(new FontIcon("fas-sync-alt"));
        btnRefresh.setTooltip(new Tooltip("Refresh"));
        btnRefresh.getStyleClass().addAll("editor-btn", "btn-refresh");
        btnRefresh.setOnAction(e -> refresh());

        Button btnToggleHidden = new Button();
        btnToggleHidden.setGraphic(new FontIcon("fas-eye-slash"));
        btnToggleHidden.setTooltip(new Tooltip("Show Hidden Items"));
        btnToggleHidden.getStyleClass().addAll("editor-btn");
        btnToggleHidden.setOnAction(e -> {
            showHidden = !showHidden;
            if (showHidden) {
                btnToggleHidden.setGraphic(new FontIcon("fas-eye"));
                btnToggleHidden.setTooltip(new Tooltip("Hide Hidden Items"));
            } else {
                btnToggleHidden.setGraphic(new FontIcon("fas-eye-slash"));
                btnToggleHidden.setTooltip(new Tooltip("Show Hidden Items"));
            }
            refresh();
        });

        toolbar.getChildren().addAll(btnNewFile, btnNewFolder, btnRefresh, btnToggleHidden);

        baseDirectory = new File(System.getProperty("user.dir"), "routes");
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs();
        }

        rootItem = new TreeItem<>(baseDirectory);
        rootItem.setExpanded(true);
        
        treeView = new TreeView<>(rootItem);
        treeView.getStyleClass().add("custom-tree-view");
        
        treeView.setEditable(true);
        treeView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        treeView.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY_OR_MOVE);
                e.consume();
            }
        });

        treeView.setOnDragDropped(e -> processDrop(e, baseDirectory));

        treeView.setCellFactory(tv -> new TreeCell<File>() {
            private TextField textField;

            @Override
            public void startEdit() {
                super.startEdit();
                if (textField == null) {
                    createTextField();
                }
                textField.setText(getItem().getName());
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem().getName());
                updateGraphic(getItem());
            }

            @Override
            public void commitEdit(File newFile) {
                super.commitEdit(newFile);
                setText(newFile.getName());
                updateGraphic(newFile);
                refresh(); 
            }

            private void createTextField() {
                textField = new TextField();
                textField.setOnKeyReleased(t -> {
                    if (t.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        String newName = textField.getText();
                        File item = getItem();
                        if (!item.isDirectory() && !newName.endsWith(".yaml") && !newName.endsWith(".yml")) {
                            newName += ".yaml";
                        }
                        File newFile = new File(item.getParentFile(), newName);
                        if (item.renameTo(newFile)) {
                            commitEdit(newFile);
                        } else {
                            cancelEdit();
                        }
                    } else if (t.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });
            }

            private void updateGraphic(File item) {
                if (item.isDirectory()) {
                    setGraphic(new FontIcon("fas-folder"));
                } else if (item.getName().endsWith(".yaml") || item.getName().endsWith(".yml")) {
                    setGraphic(new FontIcon("fas-file-code"));
                } else {
                    setGraphic(new FontIcon("fas-file"));
                }
            }

            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(createContextMenu(baseDirectory));
                    setOnDragDetected(null);
                    setupDropTarget(this, baseDirectory);
                } else {
                    if (isEditing()) {
                        if (textField != null) textField.setText(item.getName());
                        setText(null);
                        setGraphic(textField);
                    } else {
                        if (item.equals(baseDirectory)) {
                            setText("Routes");
                            setGraphic(new FontIcon("fas-project-diagram"));
                        } else {
                            setText(item.getName());
                            updateGraphic(item);
                        }

                        setOnDragDetected(e -> {
                            if (item != null && !item.equals(baseDirectory)) {
                                javafx.scene.input.Dragboard db = startDragAndDrop(javafx.scene.input.TransferMode.COPY_OR_MOVE);
                                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                                
                                java.util.List<File> filesToDrag = new java.util.ArrayList<>();
                                for (TreeItem<File> selectedItem : treeView.getSelectionModel().getSelectedItems()) {
                                    if (selectedItem != null && selectedItem.getValue() != null && !selectedItem.getValue().equals(baseDirectory)) {
                                        filesToDrag.add(selectedItem.getValue());
                                    }
                                }
                                if (!filesToDrag.contains(item)) {
                                    filesToDrag.clear();
                                    filesToDrag.add(item);
                                }
                                
                                content.putFiles(filesToDrag);
                                db.setContent(content);
                                e.consume();
                            }
                        });

                        setupDropTarget(this, item.isDirectory() ? item : item.getParentFile());

                        setContextMenu(createContextMenu(item));
                    }
                }
            }

            private void setupDropTarget(TreeCell<File> cell, File targetDir) {
                cell.setOnDragOver(e -> {
                    if (e.getDragboard().hasFiles() && targetDir != null) {
                        e.acceptTransferModes(javafx.scene.input.TransferMode.COPY_OR_MOVE);
                        e.consume();
                    }
                });

                cell.setOnDragDropped(e -> processDrop(e, targetDir));
            }
        });

        treeView.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.F2) {
                TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.getValue().equals(baseDirectory)) {
                    treeView.edit(selected);
                }
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null && newVal.getValue().isFile()) {
                if (this.onFileSelected != null) {
                    this.onFileSelected.accept(newVal.getValue());
                }
            }
        });

        treeView.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2 && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                event.consume();
                TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue().isFile()) {
                    if (this.onFileSelected != null) {
                        this.onFileSelected.accept(selected.getValue());
                    }
                }
            }
        });

        VBox.setVgrow(treeView, Priority.ALWAYS);

        getChildren().addAll(title, toolbar, treeView);
        
        refresh();
    }

    private ContextMenu createContextMenu(File item) {
        if (item == null) return null;
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem openMenu = new MenuItem("Open", new FontIcon("fas-folder-open"));
        openMenu.setOnAction(e -> {
            if (item.isFile() && onFileSelected != null) {
                onFileSelected.accept(item);
            }
        });
        
        Menu newMenu = new Menu("New...", new FontIcon("fas-plus-circle"));
        
        MenuItem newFileMenu = new MenuItem("Empty File", new FontIcon("fas-file"));
        newFileMenu.setOnAction(e -> createNewItem(item, false));
        
        MenuItem newFolderMenu = new MenuItem("Folder", new FontIcon("fas-folder-plus"));
        newFolderMenu.setOnAction(e -> createNewItem(item, true));

        MenuItem newYamlDslItem = new MenuItem("YAML DSL Route", new FontIcon("fas-route"));
        newYamlDslItem.setOnAction(e -> createTemplateFileInDir(item, "yaml-route.yaml", "- route:\n    id: \"yaml-route\"\n    from:\n      uri: \"timer:yaml?period=1000\"\n      steps:\n        - log: \"YAML DSL Route Triggered\"\n"));

        MenuItem newRestItem = new MenuItem("YAML REST Config", new FontIcon("fas-server"));
        newRestItem.setOnAction(e -> createTemplateFileInDir(item, "rest-config.yaml", "- rest:\n    path: /api\n    get:\n      - path: /hello\n        to: direct:hello\n"));

        MenuItem newExcItem = new MenuItem("YAML Global Exception", new FontIcon("fas-exclamation-triangle"));
        newExcItem.setOnAction(e -> createTemplateFileInDir(item, "global-exception.yaml", "- onException:\n    exception: [\"java.lang.Exception\"]\n    steps:\n        - log: Caught Exception\n"));

        MenuItem newKameletItem = new MenuItem("Kamelet Definition", new FontIcon("fas-cube"));
        newKameletItem.setOnAction(e -> createTemplateFileInDir(item, "my-kamelet.kamelet.yaml", "apiVersion: camel.apache.org/v1alpha1\nkind: Kamelet\nmetadata:\n  name: my-kamelet\nspec:\n  definition:\n    title: \"My Kamelet\"\n    description: \"Does something\"\n    properties:\n      foo:\n        type: string\n  template:\n    from:\n      uri: \"timer:tick\"\n      steps:\n        - log: \"${body}\"\n"));

        MenuItem newComponentItem = new MenuItem("Camel Component (Java)", new FontIcon("fas-coffee"));
        newComponentItem.setOnAction(e -> createTemplateFileInDir(item, "MyComponent.java", "package com.example;\n\nimport org.apache.camel.Endpoint;\nimport org.apache.camel.support.DefaultComponent;\nimport java.util.Map;\n\npublic class MyComponent extends DefaultComponent {\n    @Override\n    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {\n        return null;\n    }\n}\n"));

        MenuItem newProcessorItem = new MenuItem("Processor (Java)", new FontIcon("fas-coffee"));
        newProcessorItem.setOnAction(e -> createTemplateFileInDir(item, "MyProcessor.java", "package com.example;\n\nimport org.apache.camel.Exchange;\nimport org.apache.camel.Processor;\n\npublic class MyProcessor implements Processor {\n    @Override\n    public void process(Exchange exchange) throws Exception {\n        String body = exchange.getIn().getBody(String.class);\n        exchange.getIn().setBody(body + \" processed\");\n    }\n}\n"));

        MenuItem newJavaDslItem = new MenuItem("Java DSL Route", new FontIcon("fas-coffee"));
        newJavaDslItem.setOnAction(e -> createTemplateFileInDir(item, "MyRoute.java", "package com.example;\n\nimport org.apache.camel.builder.RouteBuilder;\n\npublic class MyRoute extends RouteBuilder {\n    @Override\n    public void configure() throws Exception {\n        from(\"timer:java?period=1000\")\n            .log(\"Java DSL Route Triggered\")\n            .to(\"mock:result\");\n    }\n}\n"));

        MenuItem newXmlDslItem = new MenuItem("XML DSL Route", new FontIcon("fas-code"));
        newXmlDslItem.setOnAction(e -> createTemplateFileInDir(item, "xml-route.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<routes xmlns=\"http://camel.apache.org/schema/spring\">\n    <route id=\"xml-route\">\n        <from uri=\"timer:xml?period=1000\"/>\n        <log message=\"XML DSL Route Triggered\"/>\n        <to uri=\"mock:result\"/>\n    </route>\n</routes>\n"));
        
        MenuItem newGroovyDslItem = new MenuItem("Groovy DSL Route", new FontIcon("fas-code"));
        newGroovyDslItem.setOnAction(e -> createTemplateFileInDir(item, "groovy-route.groovy", "import org.apache.camel.builder.RouteBuilder\n\nclass MyGroovyRoute extends RouteBuilder {\n    void configure() {\n        from(\"timer:groovy?period=1000\")\n            .log(\"Groovy DSL Route Triggered\")\n            .to(\"mock:result\")\n    }\n}\n"));

        MenuItem newKotlinDslItem = new MenuItem("Kotlin DSL Route", new FontIcon("fas-code"));
        newKotlinDslItem.setOnAction(e -> createTemplateFileInDir(item, "kotlin-route.kts", "import org.apache.camel.builder.RouteBuilder\n\nclass MyKotlinRoute : RouteBuilder() {\n    override fun configure() {\n        from(\"timer:kotlin?period=1000\")\n            .log(\"Kotlin DSL Route Triggered\")\n            .to(\"mock:result\")\n    }\n}\n"));

        Menu templatesMenu = new Menu("Transformations", new FontIcon("fas-exchange-alt"));
        
        MenuItem newXsltItem = new MenuItem("XSLT Template", new FontIcon("fas-file-code"));
        newXsltItem.setOnAction(e -> createTemplateFileInDir(item, "transform.xslt", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n  <xsl:template match=\"/\">\n    <result>\n      <message>Transformed by XSLT</message>\n    </result>\n  </xsl:template>\n</xsl:stylesheet>\n"));

        MenuItem newJsltItem = new MenuItem("JSLT Template", new FontIcon("fas-file-code"));
        newJsltItem.setOnAction(e -> createTemplateFileInDir(item, "transform.jslt", "{\n  \"transformed\": .body,\n  \"status\": \"success\"\n}\n"));

        MenuItem newFtlItem = new MenuItem("FreeMarker (FTL) Template", new FontIcon("fas-file-alt"));
        newFtlItem.setOnAction(e -> createTemplateFileInDir(item, "template.ftl", "Hello ${headers.name!'World'}!\nYour message is: ${body}\n"));

        templatesMenu.getItems().addAll(newXsltItem, newJsltItem, newFtlItem);

        newMenu.getItems().addAll(newFileMenu, newFolderMenu, new SeparatorMenuItem(), 
                                  newYamlDslItem, newRestItem, newExcItem, newKameletItem, new SeparatorMenuItem(), 
                                  newJavaDslItem, newXmlDslItem, newGroovyDslItem, newKotlinDslItem, new SeparatorMenuItem(),
                                  newComponentItem, newProcessorItem, templatesMenu);

        MenuItem cutMenu = new MenuItem("Cut", new FontIcon("fas-cut"));
        cutMenu.setOnAction(e -> {
            clipboardFiles = new java.util.ArrayList<>(getSelectedFilesOrItem(item));
            isCutAction = true;
        });

        MenuItem copyMenu = new MenuItem("Copy", new FontIcon("fas-copy"));
        copyMenu.setOnAction(e -> {
            clipboardFiles = new java.util.ArrayList<>(getSelectedFilesOrItem(item));
            isCutAction = false;
        });

        MenuItem pasteMenu = new MenuItem("Paste", new FontIcon("fas-paste"));
        pasteMenu.setDisable(clipboardFiles == null || clipboardFiles.isEmpty() || !clipboardFiles.get(0).exists());
        pasteMenu.setOnAction(e -> pasteItem(item));
        
        MenuItem renameMenu = new MenuItem("Rename", new FontIcon("fas-pen"));
        renameMenu.setOnAction(e -> renameItem(item));
        
        MenuItem deleteMenu = new MenuItem("Delete", new FontIcon("fas-trash-alt"));
        deleteMenu.setOnAction(e -> deleteItem(item));

        MenuItem runRouteMenu = new MenuItem("Run Route(s)", new FontIcon("fas-play-circle"));
        runRouteMenu.setOnAction(e -> {
            if (onRunSelected != null) {
                onRunSelected.accept(item, "offline");
            }
        });

        if (item.isDirectory()) {
            contextMenu.getItems().addAll(newMenu, runRouteMenu, new SeparatorMenuItem(), pasteMenu, new SeparatorMenuItem(), renameMenu, deleteMenu);
        } else {
            String name = item.getName().toLowerCase();
            if (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".groovy")) {
                contextMenu.getItems().addAll(openMenu, runRouteMenu, new SeparatorMenuItem(), cutMenu, copyMenu, pasteMenu, new SeparatorMenuItem(), renameMenu, deleteMenu);
            } else {
                contextMenu.getItems().addAll(openMenu, new SeparatorMenuItem(), cutMenu, copyMenu, pasteMenu, new SeparatorMenuItem(), renameMenu, deleteMenu);
            }
        }

        if (item.equals(baseDirectory)) {
            contextMenu.getItems().removeAll(renameMenu, deleteMenu);
        }

        // Auto-show/hide paste menu if we request to show the menu
        contextMenu.setOnShowing(e -> {
            pasteMenu.setDisable(clipboardFiles == null || clipboardFiles.isEmpty() || !clipboardFiles.get(0).exists());
        });

        return contextMenu;
    }

    private void createNewItem(boolean isFolder) {
        TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
        File targetDir = baseDirectory;
        if (selected != null) {
            targetDir = selected.getValue().isDirectory() ? selected.getValue() : selected.getValue().getParentFile();
        }
        createNewItem(targetDir, isFolder);
    }

    private java.util.List<File> getSelectedFilesOrItem(File item) {
        java.util.List<File> files = new java.util.ArrayList<>();
        for (TreeItem<File> selectedItem : treeView.getSelectionModel().getSelectedItems()) {
            if (selectedItem != null && selectedItem.getValue() != null && !selectedItem.getValue().equals(baseDirectory)) {
                files.add(selectedItem.getValue());
            }
        }
        if (!files.contains(item)) {
            files.clear();
            files.add(item);
        }
        return files;
    }

    private void pasteItem(File targetNode) {
        if (clipboardFiles == null || clipboardFiles.isEmpty() || !clipboardFiles.get(0).exists()) return;
        
        File targetDir = targetNode.isDirectory() ? targetNode : targetNode.getParentFile();
        
        try {
            for (File clipFile : clipboardFiles) {
                if (!clipFile.exists()) continue;
                File newFile = new File(targetDir, clipFile.getName());
                
                if (newFile.exists() && !newFile.equals(clipFile)) {
                    int counter = 1;
                    String name = clipFile.getName();
                    String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
                    String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                    while (newFile.exists()) {
                        newFile = new File(targetDir, base + "_" + counter + ext);
                        counter++;
                    }
                }
                
                if (isCutAction) {
                    if (!newFile.equals(clipFile)) {
                        Files.move(clipFile.toPath(), newFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    if (clipFile.isDirectory()) {
                        copyDirectory(clipFile, newFile);
                    } else {
                        Files.copy(clipFile.toPath(), newFile.toPath());
                    }
                }
            }
            if (isCutAction) clipboardFiles.clear();
            refresh();
            TreeItem<File> targetTreeItem = findTreeItem(rootItem, targetDir);
            if (targetTreeItem != null) targetTreeItem.setExpanded(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        Files.walk(sourceDir.toPath()).forEach(source -> {
            java.nio.file.Path destination = targetDir.toPath().resolve(sourceDir.toPath().relativize(source));
            try {
                Files.copy(source, destination);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void createNewItem(File targetDir, boolean isFolder) {
        if (targetDir == null || !targetDir.isDirectory()) return;

        TextInputDialog dialog = new TextInputDialog();
        RouteBuilderApp.themeDialog(dialog);
        dialog.setTitle(isFolder ? "New Folder" : "New File");
        dialog.setHeaderText("Create in: " + targetDir.getName());
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                if (!isFolder && !name.endsWith(".yaml") && !name.endsWith(".yml")) {
                    name += ".yaml";
                }
                
                File newFile = new File(targetDir, name);
                if (isFolder) {
                    newFile.mkdir();
                } else {
                    newFile.createNewFile();
                    Files.writeString(newFile.toPath(), "- route:\n    id: \"new-route\"\n    from:\n      uri: \"timer:tick\"\n      steps:\n        - log: \"Hello\"\n");
                }
                refresh();
                
                // Expand parent to show the new item
                expandToPath(newFile, rootItem);
                
                if (!isFolder) {
                    TreeItem<File> item = findTreeItem(rootItem, newFile);
                    if (item != null) {
                        treeView.getSelectionModel().clearSelection();
                        treeView.getSelectionModel().select(item);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void renameItem(File item) {
        if (item == null || item.equals(baseDirectory)) return;
        TreeItem<File> treeItem = findTreeItem(rootItem, item);
        if (treeItem != null) {
            treeView.edit(treeItem);
        }
    }

    private TreeItem<File> findTreeItem(TreeItem<File> root, File file) {
        if (root.getValue().equals(file)) return root;
        for (TreeItem<File> child : root.getChildren()) {
            TreeItem<File> found = findTreeItem(child, file);
            if (found != null) return found;
        }
        return null;
    }

    private void deleteItem(File item) {
        if (item == null || item.equals(baseDirectory)) return;

        java.util.List<File> itemsToDelete = getSelectedFilesOrItem(item);
        if (itemsToDelete.isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        RouteBuilderApp.themeDialog(alert);
        alert.setTitle("Delete");
        if (itemsToDelete.size() == 1) {
            alert.setHeaderText("Are you sure you want to delete '" + itemsToDelete.get(0).getName() + "'?");
        } else {
            alert.setHeaderText("Are you sure you want to delete " + itemsToDelete.size() + " items?");
        }
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            for (File file : itemsToDelete) {
                deleteRecursively(file);
            }
            refresh();
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) deleteRecursively(f);
            }
        }
        file.delete();
    }

    private boolean expandToPath(File targetFile, TreeItem<File> current) {
        if (current.getValue().equals(targetFile)) {
            if (current.getParent() != null) {
                current.getParent().setExpanded(true);
            }
            return true;
        }
        for (TreeItem<File> child : current.getChildren()) {
            if (expandToPath(targetFile, child)) {
                current.setExpanded(true);
                return true;
            }
        }
        return false;
    }

    private TreeItem<File> findFirstFile(TreeItem<File> item) {
        if (item == null) return null;
        if (item.getValue() != null && item.getValue().isFile()) return item;
        for (TreeItem<File> child : item.getChildren()) {
            TreeItem<File> found = findFirstFile(child);
            if (found != null) return found;
        }
        return null;
    }

    public void setBaseDirectory(File newBaseDir) {
        this.baseDirectory = newBaseDir;
        this.rootItem.setValue(newBaseDir);
        if (title != null) {
            title.setText("EXPLORER: " + newBaseDir.getName().toUpperCase());
        }
        refresh();
        
        TreeItem<File> firstFileItem = findFirstFile(rootItem);
        if (firstFileItem != null) {
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().select(firstFileItem);
        } else {
            treeView.getSelectionModel().clearSelection();
            if (this.onFileSelected != null) {
                this.onFileSelected.accept(null);
            }
        }
    }

    public void refresh() {
        rootItem.getChildren().clear();
        populateTree(baseDirectory, rootItem);
        rootItem.setExpanded(true);
    }

    private void populateTree(File dir, TreeItem<File> parentItem) {
        File[] files = dir.listFiles();
        if (files != null) {
            java.util.Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });
            for (File file : files) {
                if (!showHidden) {
                    if (file.getName().startsWith(".")) {
                        continue;
                    }
                    if (!file.isDirectory()) {
                        String name = file.getName().toLowerCase();
                        if (!name.endsWith(".java") && !name.endsWith(".yaml") && !name.endsWith(".yml")) {
                            continue;
                        }
                    }
                }
                TreeItem<File> item = new TreeItem<>(file);
                parentItem.getChildren().add(item);
                if (file.isDirectory()) {
                    populateTree(file, item);
                }
            }
        }
    }

    public File getBaseDirectory() {
        return this.baseDirectory;
    }

    public File getSelectedDirectory() {
        TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null) {
            File f = selected.getValue();
            return f.isDirectory() ? f : f.getParentFile();
        }
        return baseDirectory;
    }

    public void createTemplateFile(String name, String content) {
        createTemplateFileInDir(baseDirectory, name, content);
    }

    public void createTemplateFileInDir(File targetDir, String name, String content) {
        try {
            File newFile = new File(targetDir, name);
            int counter = 1;
            while (newFile.exists()) {
                String baseName = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
                String ext = name.contains(".") ? name.substring(name.indexOf('.')) : "";
                newFile = new File(targetDir, baseName + counter + ext);
                counter++;
            }
            Files.writeString(newFile.toPath(), content);
            refresh();
            expandToPath(newFile, rootItem);
            
            // Select the new file so it opens in the editor
            TreeItem<File> item = findTreeItem(rootItem, newFile);
            if (item != null) {
                treeView.getSelectionModel().select(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void processDrop(javafx.scene.input.DragEvent e, File targetDir) {
        javafx.scene.input.Dragboard db = e.getDragboard();
        boolean success = false;
        if (db.hasFiles() && targetDir != null) {
            for (File droppedFile : db.getFiles()) {
                if (droppedFile.equals(targetDir)) continue;
                
                boolean isMove = e.getTransferMode() == javafx.scene.input.TransferMode.MOVE;
                if (isMove && droppedFile.getParentFile().equals(targetDir)) {
                    success = true; // Visually successful even if no-op
                    continue;
                }
                
                File destFile = new File(targetDir, droppedFile.getName());
                
                if (!isMove && destFile.exists()) {
                    String name = droppedFile.getName();
                    String baseName = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
                    String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                    int counter = 1;
                    while (destFile.exists()) {
                        destFile = new File(targetDir, baseName + " (" + counter + ")" + ext);
                        counter++;
                    }
                }
                
                try {
                    if (isMove) {
                        Files.move(droppedFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        if (droppedFile.isDirectory()) {
                            copyDirectory(droppedFile, destFile);
                        } else {
                            Files.copy(droppedFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            success = true;
            refresh();
            expandToPath(targetDir, rootItem);
        }
        e.setDropCompleted(success);
        e.consume();
    }

    public File getSelectedFile() {
        TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null && selected.getValue().isFile()) {
            return selected.getValue();
        }
        return null;
    }

    public TreeItem<File> getRootItem() {
        return rootItem;
    }
}
