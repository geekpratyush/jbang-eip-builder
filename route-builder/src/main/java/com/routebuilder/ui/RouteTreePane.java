package com.routebuilder.ui;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
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

    private final java.util.Set<File> checkedFiles = new java.util.HashSet<>();
    private final java.util.Set<File> conflictingFiles = new java.util.HashSet<>();
    private Runnable onCheckedFilesChanged;

    public java.util.Set<File> getCheckedFiles() {
        return checkedFiles;
    }

    public void setOnCheckedFilesChanged(Runnable onCheckedFilesChanged) {
        this.onCheckedFilesChanged = onCheckedFilesChanged;
    }

    public void setOnRunSelected(java.util.function.BiConsumer<File, String> onRunSelected) {
        this.onRunSelected = onRunSelected;
    }

    private static java.util.List<File> clipboardFiles = new java.util.ArrayList<>();
    private static boolean isCutAction = false;
    private java.util.function.Consumer<java.util.List<File>> onFilesSelected;

    public void setOnFilesSelected(java.util.function.Consumer<java.util.List<File>> onFilesSelected) {
        this.onFilesSelected = onFilesSelected;
    }

    private boolean programmaticEdit = false;

    private boolean isRefreshing = false;
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

        Button btnExpandAll = new Button();
        btnExpandAll.setGraphic(new FontIcon("fas-expand-arrows-alt"));
        btnExpandAll.setTooltip(new Tooltip("Expand All"));
        btnExpandAll.getStyleClass().addAll("editor-btn");
        btnExpandAll.setOnAction(e -> toggleAllNodes(rootItem, true));

        Button btnCollapseAll = new Button();
        btnCollapseAll.setGraphic(new FontIcon("fas-compress-arrows-alt"));
        btnCollapseAll.setTooltip(new Tooltip("Collapse All"));
        btnCollapseAll.getStyleClass().addAll("editor-btn");
        btnCollapseAll.setOnAction(e -> toggleAllNodes(rootItem, false));

        toolbar.getChildren().addAll(btnExpandAll, btnCollapseAll);

        File camelDir = new File(System.getProperty("user.dir"), "camel");
        if (camelDir.exists()) {
            baseDirectory = camelDir;
        } else {
            baseDirectory = new File(System.getProperty("user.dir"), "routes");
        }
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs();
        }

        rootItem = new TreeItem<>(baseDirectory);
        rootItem.setExpanded(true);
        
        treeView = new TreeView<File>(rootItem) {
            @Override
            public void edit(TreeItem<File> item) {
                if (programmaticEdit && item != null && !item.getValue().equals(baseDirectory)) {
                    super.edit(item);
                }
            }
        };
        treeView.getStyleClass().add("custom-tree-view");
        this.setMinWidth(50);
        this.setPrefWidth(260);
        
        treeView.setEditable(true);
        treeView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        // Reset programmaticEdit when edit ends
        treeView.setOnEditCommit(e -> programmaticEdit = false);
        treeView.setOnEditCancel(e -> programmaticEdit = false);

        // Ensure clicking a selected item doesn't start edit automatically
        treeView.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2 && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                // Handle open file on double click
                TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue().isFile()) {
                    if (this.onFileSelected != null) {
                        this.onFileSelected.accept(selected.getValue());
                    }
                }
                event.consume(); // Prevent default edit behavior on double click
            }
        });

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
                File item = getItem();
                if (item != null) {
                    setText(item.getName());
                    updateGraphic(item);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }

            @Override
            public void commitEdit(File newFile) {
                super.commitEdit(newFile);
                if (newFile != null) {
                    setText(newFile.getName());
                    updateGraphic(newFile);
                }
                refresh(); 
            }

            private void createTextField() {
                textField = new TextField();
                textField.setOnKeyReleased(t -> {
                    if (t.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        String newName = textField.getText();
                        File item = getItem();
                        if (item != null) {
                            if (!item.isDirectory() && !newName.endsWith(".yaml") && !newName.endsWith(".yml")) {
                                newName += ".yaml";
                            }
                            File newFile = new File(item.getParentFile(), newName);
                            if (item.renameTo(newFile)) {
                                commitEdit(newFile);
                            } else {
                                cancelEdit();
                            }
                        }
                    } else if (t.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });
            }

            private void updateGraphic(File item) {
                if (item == null) return;
                CheckBox cb = new CheckBox();
                if (item.isDirectory()) {
                    boolean hasNestedConflict = false;
                    for (File f : conflictingFiles) {
                        if (f.getAbsolutePath().startsWith(item.getAbsolutePath() + java.io.File.separator)) {
                            hasNestedConflict = true;
                            break;
                        }
                    }
                    if (hasNestedConflict) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }

                    cb.setAllowIndeterminate(true);
                    CheckState state = getFolderCheckState(item);
                    if (state == CheckState.CHECKED) {
                        cb.setSelected(true);
                        cb.setIndeterminate(false);
                    } else if (state == CheckState.UNCHECKED) {
                        cb.setSelected(false);
                        cb.setIndeterminate(false);
                    } else {
                        cb.setSelected(false);
                        cb.setIndeterminate(true);
                    }
                    cb.setOnAction(e -> {
                        CheckState currentState = getFolderCheckState(item);
                        boolean targetChecked = (currentState != CheckState.CHECKED);
                        setCheckedRecursive(item, targetChecked);
                        updateConflicts();
                        if (onCheckedFilesChanged != null) {
                            onCheckedFilesChanged.run();
                        }
                        javafx.application.Platform.runLater(() -> treeView.refresh());
                    });
                    
                    FontIcon icon = RouteBuilderApp.getFileIcon(item);
                    HBox box = new HBox(5, cb, icon);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(box);
                } else {
                    if (conflictingFiles.contains(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }

                    cb.setSelected(checkedFiles.contains(item));
                    cb.setOnAction(e -> {
                        if (cb.isSelected()) {
                            checkedFiles.add(item);
                        } else {
                            checkedFiles.remove(item);
                        }
                        updateConflicts();
                        if (onCheckedFilesChanged != null) {
                            onCheckedFilesChanged.run();
                        }
                        // Refresh parent folders to update their check/indeterminate state
                        javafx.application.Platform.runLater(() -> treeView.refresh());
                    });
                    
                    FontIcon icon = RouteBuilderApp.getFileIcon(item);
                    HBox box = new HBox(5, cb, icon);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }

            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
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
                            boolean hasNestedConflict = false;
                            for (File f : conflictingFiles) {
                                if (f.getAbsolutePath().startsWith(item.getAbsolutePath() + java.io.File.separator)) {
                                    hasNestedConflict = true;
                                    break;
                                }
                            }
                            if (hasNestedConflict) {
                                setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            } else {
                                setStyle("");
                            }

                            CheckBox cb = new CheckBox();
                            cb.setAllowIndeterminate(true);
                            CheckState state = getFolderCheckState(item);
                            if (state == CheckState.CHECKED) {
                                cb.setSelected(true);
                                cb.setIndeterminate(false);
                            } else if (state == CheckState.UNCHECKED) {
                                cb.setSelected(false);
                                cb.setIndeterminate(false);
                            } else {
                                cb.setSelected(false);
                                cb.setIndeterminate(true);
                            }
                            cb.setOnAction(e -> {
                                CheckState currentState = getFolderCheckState(item);
                                boolean targetChecked = (currentState != CheckState.CHECKED);
                                setCheckedRecursive(item, targetChecked);
                                updateConflicts();
                                if (onCheckedFilesChanged != null) {
                                    onCheckedFilesChanged.run();
                                }
                                javafx.application.Platform.runLater(() -> treeView.refresh());
                            });
                            FontIcon icon = new FontIcon("fas-project-diagram");
                            HBox box = new HBox(5, cb, icon);
                            box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            setGraphic(box);
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
            KeyCode code = event.getCode();
            if (code == KeyCode.F2) {
                TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.getValue().equals(baseDirectory)) {
                    programmaticEdit = true;
                    treeView.edit(selected);
                }
                event.consume();
            } else if (code == KeyCode.ENTER) {
                TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (selected.getValue().isFile()) {
                        if (this.onFileSelected != null) this.onFileSelected.accept(selected.getValue());
                    } else {
                        selected.setExpanded(!selected.isExpanded());
                    }
                }
                event.consume();
            } else if (code == KeyCode.DELETE || (code == KeyCode.BACK_SPACE && event.isShortcutDown())) {
                TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    deleteItem(selected.getValue());
                }
                event.consume();
            } else if (code == KeyCode.F5) {
                refresh();
                event.consume();
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isRefreshing) return;
            java.util.List<File> selectedFiles = new java.util.ArrayList<>();
            for (TreeItem<File> item : treeView.getSelectionModel().getSelectedItems()) {
                if (item != null && item.getValue() != null && item.getValue().isFile()) {
                    selectedFiles.add(item.getValue());
                }
            }
            if (this.onFilesSelected != null) {
                this.onFilesSelected.accept(selectedFiles);
            }
            if (selectedFiles.size() == 1) {
                if (this.onFileSelected != null) {
                    this.onFileSelected.accept(selectedFiles.get(0));
                }
            } else if (selectedFiles.isEmpty() && newVal != null && newVal.getValue() != null && newVal.getValue().isDirectory()) {
                 // Clear single selection if folder is clicked
                 if (this.onFileSelected != null) {
                     this.onFileSelected.accept(null);
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

        MenuItem copyUriMenu = new MenuItem("Copy Camel URI", new FontIcon("fas-link"));
        copyUriMenu.setOnAction(e -> {
            String uri = getCamelUri(item);
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(uri);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        });

        if (item.isDirectory()) {
            contextMenu.getItems().addAll(newMenu, runRouteMenu, new SeparatorMenuItem(), pasteMenu, new SeparatorMenuItem(), renameMenu, deleteMenu);
        } else {
            String name = item.getName().toLowerCase();
            if (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".groovy")) {
                contextMenu.getItems().addAll(openMenu, runRouteMenu, copyUriMenu, new SeparatorMenuItem(), cutMenu, copyMenu, pasteMenu, new SeparatorMenuItem(), renameMenu, deleteMenu);
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

    private String getCamelUri(File item) {
        if (item == null) return "";
        String fileName = item.getName();
        if (fileName.endsWith(".kamelet.yaml")) {
            // For kamelets, the URI is kamelet:name
            String name = fileName.substring(0, fileName.indexOf(".kamelet.yaml"));
            return "kamelet:" + name;
        }
        
        try {
            // Get path relative to base directory
            java.nio.file.Path base = baseDirectory.toPath().toAbsolutePath();
            java.nio.file.Path file = item.toPath().toAbsolutePath();
            java.nio.file.Path relative = base.relativize(file);
            String path = relative.toString().replace("\\", "/");
            return "file:" + path;
        } catch (Exception e) {
            return "file:" + item.getName();
        }
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
            programmaticEdit = true;
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

    private void toggleAllNodes(TreeItem<?> item, boolean expanded) {
        if (item != null) {
            item.setExpanded(expanded);
            for (TreeItem<?> child : item.getChildren()) {
                toggleAllNodes(child, expanded);
            }
        }
    }

    private void deleteItem(File fileToDelete) {
        if (fileToDelete == null || fileToDelete.equals(baseDirectory)) return;

        java.util.List<File> itemsToDelete = getSelectedFilesOrItem(fileToDelete);
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
            for (File f : itemsToDelete) {
                deleteRecursively(f);
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

    public TreeView<File> getTreeView() {
        return treeView;
    }

    public void setBaseDirectory(File newBaseDir) {
        File camelDir = new File(newBaseDir, "camel");
        File routesDir = new File(newBaseDir, "routes");
        if (camelDir.exists() && camelDir.isDirectory()) {
            this.baseDirectory = camelDir;
            if (title != null) {
                title.setText("EXPLORER: CAMEL");
            }
        } else if (routesDir.exists() && routesDir.isDirectory()) {
            this.baseDirectory = routesDir;
            if (title != null) {
                title.setText("EXPLORER: ROUTES");
            }
        } else {
            this.baseDirectory = newBaseDir;
            if (title != null) {
                title.setText("EXPLORER: " + newBaseDir.getName().toUpperCase());
            }
        }
        this.rootItem.setValue(this.baseDirectory);
        checkedFiles.clear();
        updateConflicts();
        if (onCheckedFilesChanged != null) {
            onCheckedFilesChanged.run();
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
        // Capture current state
        File selectedFile = getSelectedFile();
        java.util.List<File> expandedFiles = new java.util.ArrayList<>();
        collectExpandedFiles(rootItem, expandedFiles);

        isRefreshing = true;
        try {
            checkedFiles.removeIf(file -> !file.exists());
            updateConflicts();
            if (onCheckedFilesChanged != null) {
                onCheckedFilesChanged.run();
            }
            rootItem.getChildren().clear();
            populateTree(baseDirectory, rootItem);
            rootItem.setExpanded(true);

            // Restore expansion
            restoreExpansion(rootItem, expandedFiles);
        } finally {
            isRefreshing = false;
        }

        // Restore selection - this will trigger events normally but after population is complete
        if (selectedFile != null) {
            TreeItem<File> item = findTreeItem(rootItem, selectedFile);
            if (item != null) {
                treeView.getSelectionModel().clearSelection();
                treeView.getSelectionModel().select(item);
            }
        }
    }

    private void collectExpandedFiles(TreeItem<File> item, java.util.List<File> expanded) {
        if (item == null) return;
        if (item.isExpanded() && item.getValue() != null) {
            expanded.add(item.getValue());
        }
        for (TreeItem<File> child : item.getChildren()) {
            collectExpandedFiles(child, expanded);
        }
    }

    private void restoreExpansion(TreeItem<File> item, java.util.List<File> expanded) {
        if (item == null || item.getValue() == null) return;
        if (expanded.contains(item.getValue())) {
            item.setExpanded(true);
        }
        for (TreeItem<File> child : item.getChildren()) {
            restoreExpansion(child, expanded);
        }
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
        createTemplateFileInDir(targetDir, name, content, false);
    }

    public void createTemplateFileInDir(File targetDir, String name, String content, boolean overwrite) {
        try {
            File newFile = new File(targetDir, name);
            if (!overwrite) {
                int counter = 1;
                while (newFile.exists()) {
                    String baseName = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
                    String ext = name.contains(".") ? name.substring(name.indexOf('.')) : "";
                    newFile = new File(targetDir, baseName + counter + ext);
                    counter++;
                }
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

    private enum CheckState {
        CHECKED, UNCHECKED, INDETERMINATE
    }

    private boolean isRouteFile(File file) {
        if (!file.isFile()) return false;
        String name = file.getName().toLowerCase();
        if (name.startsWith(".")) return false;
        return name.endsWith(".java") || name.endsWith(".yaml") || name.endsWith(".yml") || 
               name.endsWith(".xml") || name.endsWith(".txt") || name.endsWith(".template");
    }

    private CheckState getFolderCheckState(File folder) {
        java.util.List<File> files = new java.util.ArrayList<>();
        collectFilesRecursive(folder, files);
        if (files.isEmpty()) {
            return CheckState.UNCHECKED;
        }
        int checkedCount = 0;
        for (File f : files) {
            if (checkedFiles.contains(f)) {
                checkedCount++;
            }
        }
        if (checkedCount == 0) {
            return CheckState.UNCHECKED;
        } else if (checkedCount == files.size()) {
            return CheckState.CHECKED;
        } else {
            return CheckState.INDETERMINATE;
        }
    }

    private void collectFilesRecursive(File file, java.util.List<File> list) {
        if (isRouteFile(file)) {
            list.add(file);
        } else if (file.isDirectory()) {
            if (!file.getName().startsWith(".")) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        collectFilesRecursive(child, list);
                    }
                }
            }
        }
    }

    private void setCheckedRecursive(File file, boolean checked) {
        if (isRouteFile(file)) {
            if (checked) {
                checkedFiles.add(file);
            } else {
                checkedFiles.remove(file);
            }
        } else if (file.isDirectory()) {
            if (!file.getName().startsWith(".")) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        setCheckedRecursive(child, checked);
                    }
                }
            }
        }
    }

    public void updateConflicts() {
        conflictingFiles.clear();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        java.util.Map<String, java.util.List<File>> idMap = new java.util.HashMap<>();
        
        for (File f : checkedFiles) {
            if (f == null || !f.isFile()) continue;
            String name = f.getName().toLowerCase();
            if (name.endsWith(".yaml") || name.endsWith(".yml")) {
                try {
                    String content = java.nio.file.Files.readString(f.toPath());
                    com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(content);
                    if (node != null) {
                        java.util.List<com.fasterxml.jackson.databind.JsonNode> items = new java.util.ArrayList<>();
                        if (node.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode item : node) {
                                items.add(item);
                            }
                        } else {
                            items.add(node);
                        }
                        
                        for (com.fasterxml.jackson.databind.JsonNode item : items) {
                            String id = null;
                            if (item.has("route")) {
                                com.fasterxml.jackson.databind.JsonNode r = item.get("route");
                                if (r.has("id")) id = r.get("id").asText();
                            } else if (item.has("from")) {
                                com.fasterxml.jackson.databind.JsonNode fr = item.get("from");
                                if (fr.has("id")) id = fr.get("id").asText();
                            } else if (item.has("id")) {
                                id = item.get("id").asText();
                            }
                            if (id != null && !id.trim().isEmpty()) {
                                idMap.computeIfAbsent(id, k -> new java.util.ArrayList<>()).add(f);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            } else if (name.endsWith(".java")) {
                try {
                    String content = java.nio.file.Files.readString(f.toPath());
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\.routeId\\s*\\(\\s*\"([^\"]+)\"\\s*\\)").matcher(content);
                    while (m.find()) {
                        String id = m.group(1);
                        if (id != null && !id.trim().isEmpty()) {
                            idMap.computeIfAbsent(id, k -> new java.util.ArrayList<>()).add(f);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        
        for (java.util.List<File> list : idMap.values()) {
            if (list.size() > 1) {
                conflictingFiles.addAll(list);
            }
        }
    }
}
