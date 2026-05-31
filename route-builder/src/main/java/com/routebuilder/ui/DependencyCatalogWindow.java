package com.routebuilder.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DependencyCatalogWindow {

    public static class DependencyItem {
        private boolean enabled;
        private String name;
        private String coordinate;
        private String description;

        public DependencyItem() {}

        public DependencyItem(boolean enabled, String name, String coordinate, String description) {
            this.enabled = enabled;
            this.name = name;
            this.coordinate = coordinate;
            this.description = description;
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCoordinate() { return coordinate; }
        public void setCoordinate(String coordinate) { this.coordinate = coordinate; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    private final File workspaceRoot;
    private final Stage stage;
    private TableView<DependencyItem> tableView;
    private ObservableList<DependencyItem> tableData;

    public DependencyCatalogWindow(File workspaceRoot, Stage stage) {
        this.workspaceRoot = workspaceRoot;
        this.stage = stage;
        initUI();
    }

    public static void show(File baseDir) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Dependency Catalog Manager");

        DependencyCatalogWindow window = new DependencyCatalogWindow(baseDir, stage);
        stage.show();
    }

    public static List<String> getEnabledDependencies(File baseDir) {
        List<String> list = new ArrayList<>();
        if (baseDir == null || !baseDir.exists()) {
            return list;
        }
        File catalogFile = new File(baseDir, "dependency-catalog.json");
        List<DependencyItem> items;
        if (!catalogFile.exists()) {
            items = getDefaultCatalog();
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                items = mapper.readValue(catalogFile, new TypeReference<List<DependencyItem>>() {});
            } catch (IOException e) {
                items = getDefaultCatalog();
            }
        }
        for (DependencyItem item : items) {
            if (item.isEnabled() && item.getCoordinate() != null && !item.getCoordinate().trim().isEmpty()) {
                list.add(item.getCoordinate().trim());
            }
        }
        return list;
    }

    private static List<DependencyItem> getDefaultCatalog() {
        List<DependencyItem> list = new ArrayList<>();
        list.add(new DependencyItem(true, "IBM MQ Jakarta Client", "com.ibm.mq:com.ibm.mq.jakarta.client:9.4.5.1", "Required for connecting to IBM MQ 9.x+ queues using Jakarta Messaging."));
        list.add(new DependencyItem(true, "Pooled JMS", "org.messaginghub:pooled-jms:3.1.2", "Connection pool for JMS providers like IBM MQ and Solace."));
        list.add(new DependencyItem(true, "Camel YAML DSL", "org.apache.camel:camel-yaml-dsl:4.18.0", "Enables loading Camel routes from YAML files."));
        list.add(new DependencyItem(true, "Camel JMS Component", "org.apache.camel:camel-jms:4.18.0", "Enables JMS message endpoints in routes."));
        list.add(new DependencyItem(true, "Camel MongoDB Component", "org.apache.camel:camel-mongodb:4.18.0", "Enables MongoDB endpoints in routes."));
        list.add(new DependencyItem(true, "Camel Jackson Component", "org.apache.camel:camel-jackson:4.18.0", "Enables JSON serialization and deserialization."));
        list.add(new DependencyItem(true, "Camel JTA / Narayana", "org.apache.camel:camel-jta:4.18.0", "Required for XA distributed transaction managers."));
        list.add(new DependencyItem(true, "Solace Java API", "com.solacesystems:sol-jms:10.23.0", "Required for connecting to Solace message brokers."));
        return list;
    }

    private void initUI() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.getStyleClass().add("app-root");
        root.getStyleClass().add(RouteBuilderApp.currentThemeClass);

        Label lblTitle = new Label("Dependency Catalog");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-text-background-color;");

        Label lblSubtitle = new Label("Double-click any cell to edit properties inline. Check 'Enabled' to automatically include dependencies in JBang runs.");
        lblSubtitle.setWrapText(true);
        lblSubtitle.setStyle("-fx-text-fill: gray;");

        tableView = new TableView<>();
        tableView.setEditable(true);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Columns
        TableColumn<DependencyItem, Boolean> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setPrefWidth(80);
        enabledCol.setCellValueFactory(data -> {
            javafx.beans.property.SimpleBooleanProperty prop = new javafx.beans.property.SimpleBooleanProperty(data.getValue().isEnabled());
            prop.addListener((obs, oldVal, newVal) -> data.getValue().setEnabled(newVal));
            return prop;
        });
        enabledCol.setCellFactory(CheckBoxTableCell.forTableColumn(enabledCol));

        TableColumn<DependencyItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> e.getRowValue().setName(e.getNewValue()));

        TableColumn<DependencyItem, String> coordCol = new TableColumn<>("Maven Coordinate");
        coordCol.setPrefWidth(300);
        coordCol.setCellValueFactory(new PropertyValueFactory<>("coordinate"));
        coordCol.setCellFactory(TextFieldTableCell.forTableColumn());
        coordCol.setOnEditCommit(e -> e.getRowValue().setCoordinate(e.getNewValue()));

        TableColumn<DependencyItem, String> descCol = new TableColumn<>("Description");
        descCol.setPrefWidth(300);
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(e -> e.getRowValue().setDescription(e.getNewValue()));

        tableView.getColumns().addAll(enabledCol, nameCol, coordCol, descCol);

        // Load data
        loadCatalogData();

        // Buttons
        Button btnAdd = new Button("Add", new FontIcon("fas-plus"));
        btnAdd.getStyleClass().add("toolbar-btn");
        btnAdd.setOnAction(e -> {
            DependencyItem item = new DependencyItem(true, "New Dependency", "group:artifact:version", "Description of dependency");
            tableData.add(item);
            tableView.scrollTo(item);
            tableView.getSelectionModel().select(item);
        });

        Button btnDelete = new Button("Delete", new FontIcon("fas-trash"));
        btnDelete.getStyleClass().add("toolbar-btn");
        btnDelete.setOnAction(e -> {
            DependencyItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                tableData.remove(selected);
            }
        });

        Button btnInject = new Button("Inject Headers", new FontIcon("fas-file-import"));
        btnInject.getStyleClass().add("toolbar-btn");
        btnInject.setTooltip(new Tooltip("Inject current enabled dependencies into active editor file as header comments"));
        btnInject.setOnAction(e -> injectHeadersToActiveFile());

        Button btnSave = new Button("Save", new FontIcon("fas-save"));
        btnSave.getStyleClass().addAll("toolbar-btn", "btn-save");
        btnSave.setOnAction(e -> {
            saveCatalog();
            stage.close();
        });

        Button btnCancel = new Button("Close", new FontIcon("fas-times"));
        btnCancel.getStyleClass().add("toolbar-btn");
        btnCancel.setOnAction(e -> stage.close());

        Button btnHelp = new Button("Help", new FontIcon("fas-question-circle"));
        btnHelp.getStyleClass().add("toolbar-btn");
        btnHelp.setOnAction(e -> new RouteBuilderHelpWindow("Dependency Catalog", "Dependency").show());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        btnBox.getChildren().addAll(btnAdd, btnDelete, btnInject, btnHelp, spacer, btnSave, btnCancel);

        root.getChildren().addAll(lblTitle, lblSubtitle, tableView, btnBox);

        com.routebuilder.ui.components.ThemeManager.registerRoot(root);
        Scene scene = new Scene(root, 900, 500);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        if (RouteBuilderApp.currentDynamicCssUri != null) {
            scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
        }
        stage.setScene(scene);
    }

    private void loadCatalogData() {
        List<DependencyItem> items = new ArrayList<>();
        File catalogFile = new File(workspaceRoot, "dependency-catalog.json");
        if (!catalogFile.exists()) {
            items = getDefaultCatalog();
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                items = mapper.readValue(catalogFile, new TypeReference<List<DependencyItem>>() {});
            } catch (IOException e) {
                items = getDefaultCatalog();
            }
        }
        tableData = FXCollections.observableArrayList(items);
        tableView.setItems(tableData);
    }

    private void saveCatalog() {
        File catalogFile = new File(workspaceRoot, "dependency-catalog.json");
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(catalogFile, new ArrayList<>(tableData));
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save dependency catalog: " + e.getMessage());
            RouteBuilderApp.themeDialog(alert);
            alert.showAndWait();
        }
    }

    private void injectHeadersToActiveFile() {
        if (RouteBuilderApp.instance == null || RouteBuilderApp.instance.editorPane == null || RouteBuilderApp.instance.editorPane.getCurrentFile() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No active file opened in the editor.");
            RouteBuilderApp.themeDialog(alert);
            alert.showAndWait();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (DependencyItem item : tableData) {
            if (item.isEnabled() && item.getCoordinate() != null && !item.getCoordinate().trim().isEmpty()) {
                sb.append("# camel-k: dependency=mvn:").append(item.getCoordinate().trim()).append("\n");
            }
        }

        if (sb.length() == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No dependencies are currently enabled.");
            RouteBuilderApp.themeDialog(alert);
            alert.showAndWait();
            return;
        }

        String currentText = RouteBuilderApp.instance.editorPane.getText();
        String[] lines = currentText.split("\n", -1);
        StringBuilder newText = new StringBuilder(sb.toString());
        for (String line : lines) {
            if (line.trim().startsWith("#") && line.contains("camel-k:") && line.contains("dependency=")) {
                continue; // Skip old dependency comments
            }
            newText.append(line).append("\n");
        }

        String resultText = newText.toString();
        if (resultText.endsWith("\n")) {
            resultText = resultText.substring(0, resultText.length() - 1);
        }

        RouteBuilderApp.instance.editorPane.setText(resultText);
    }
}
