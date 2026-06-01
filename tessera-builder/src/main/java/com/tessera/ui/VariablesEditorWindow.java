package com.tessera.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariablesEditorWindow {

    public static class PropertyRow {
        private final javafx.beans.property.SimpleStringProperty key;
        private final javafx.beans.property.SimpleStringProperty value;
        private final javafx.beans.property.SimpleStringProperty description;

        public PropertyRow(String key, String value, String description) {
            this.key = new javafx.beans.property.SimpleStringProperty(key);
            this.value = new javafx.beans.property.SimpleStringProperty(value);
            this.description = new javafx.beans.property.SimpleStringProperty(description);
        }

        public String getKey() { return key.get(); }
        public void setKey(String k) { key.set(k); }
        public javafx.beans.property.SimpleStringProperty keyProperty() { return key; }

        public String getValue() { return value.get(); }
        public void setValue(String v) { value.set(v); }
        public javafx.beans.property.SimpleStringProperty valueProperty() { return value; }

        public String getDescription() { return description.get(); }
        public void setDescription(String d) { description.set(d); }
        public javafx.beans.property.SimpleStringProperty descriptionProperty() { return description; }
    }

    public static void show(File workspaceDir, Runnable onSaveCallback) {
        Stage stage = new Stage();
        stage.setTitle("Workspace Properties / Variables");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        
        // Apply active theme class to layout so css selects it (e.g. .root.theme-intellij-light)
        layout.getStyleClass().add(RouteBuilderApp.currentThemeClass);

        Label titleLbl = new Label("Placeholder Properties");
        titleLbl.getStyleClass().add("variables-title");

        Label descLbl = new Label("Define key-value property placeholders and descriptions below. Descriptions are saved as comments in application.properties. Double-click empty space below to add a blank row.");
        descLbl.setWrapText(true);
        descLbl.getStyleClass().add("variables-desc");

        File propsFile = new File(workspaceDir, "application.properties");

        // Table
        TableView<PropertyRow> table = new TableView<>();
        table.setEditable(true);

        TableColumn<PropertyRow, String> keyCol = new TableColumn<>("Property Key");
        keyCol.setCellValueFactory(cellData -> cellData.getValue().keyProperty());
        keyCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        keyCol.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setKey(t.getNewValue()));
        keyCol.setPrefWidth(160);

        TableColumn<PropertyRow, String> valCol = new TableColumn<>("Property Value");
        valCol.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        valCol.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setValue(t.getNewValue()));
        valCol.setPrefWidth(160);

        TableColumn<PropertyRow, String> descCol = new TableColumn<>("Description (Comment)");
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setDescription(t.getNewValue()));
        descCol.setPrefWidth(180);

        table.getColumns().addAll(keyCol, valCol, descCol);

        // Add double click listener to create empty row in background/empty cell space
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                javafx.scene.Node node = event.getPickResult().getIntersectedNode();
                boolean clickedOnEmpty = true;
                while (node != null) {
                    if (node instanceof TableRow) {
                        TableRow<?> row = (TableRow<?>) node;
                        if (!row.isEmpty()) {
                            clickedOnEmpty = false;
                            break;
                        }
                    }
                    node = node.getParent();
                }
                if (clickedOnEmpty) {
                    PropertyRow newRow = new PropertyRow("", "", "");
                    table.getItems().add(newRow);
                    table.getSelectionModel().select(newRow);
                    table.layout();
                    int rowIndex = table.getItems().size() - 1;
                    table.edit(rowIndex, keyCol);
                }
            }
        });

        // Load existing using custom parser to preserve comments as descriptions
        List<String> keyOrder = new ArrayList<>();
        Map<String, String> values = new HashMap<>();
        Map<String, String> descriptions = new HashMap<>();
        
        if (propsFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(propsFile))) {
                String line;
                List<String> currentComments = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (trimmed.startsWith("#") || trimmed.startsWith("!")) {
                        String commentContent = trimmed.substring(1).trim();
                        if (!commentContent.startsWith("Managed by") && !commentContent.startsWith("Default JBang")) {
                            currentComments.add(commentContent);
                        }
                    } else {
                        int eqIdx = trimmed.indexOf('=');
                        if (eqIdx > 0) {
                            String key = trimmed.substring(0, eqIdx).trim();
                            String val = trimmed.substring(eqIdx + 1).trim();
                            keyOrder.add(key);
                            values.put(key, val);
                            if (!currentComments.isEmpty()) {
                                descriptions.put(key, String.join(" ", currentComments));
                                currentComments.clear();
                            }
                        } else {
                            currentComments.clear();
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Standard properties required by this app
        Map<String, String> defaultValues = new java.util.LinkedHashMap<>();
        defaultValues.put("API_URL", "http://localhost:8080");
        defaultValues.put("kafka.orders.endpoint", "stub:kafka:topic:orders");
        defaultValues.put("ibmmq-endpoint", "stub:jms:queue:IBMMQ.Q");
        defaultValues.put("ibmmq.request.queue", "stub:jms:queue:REQUEST.Q");
        defaultValues.put("ibmmq.reply.queue", "stub:jms:queue:REPLY.Q");
        defaultValues.put("ibmmq.dlq.queue", "stub:jms:queue:DLQ");
        defaultValues.put("ibmmq.xa.request", "stub:jms:queue:XA.REQUEST");
        defaultValues.put("ibmmq.audit.trail", "stub:jms:queue:audit.trail");
        defaultValues.put("notify.email.queue", "stub:jms:queue:NOTIFY.EMAIL");
        defaultValues.put("notify.sms.queue", "stub:jms:queue:NOTIFY.SMS");
        defaultValues.put("mongodb.orders.endpoint", "stub:mongodb:cameldb?operation=insert");
        defaultValues.put("mongodb.audit.transactions", "stub:mongodb:myDb?consumerType=changeStream&database=audit&collection=transactions");
        defaultValues.put("http.orders.endpoint", "stub:http:api.example.com/orders");
        defaultValues.put("http.downstream.endpoint", "stub:http:downstream-service/api");
        defaultValues.put("enricher.customer.service", "stub:direct:fetch-customer");
        defaultValues.put("camel.jbang.platform-http.port", "9999");

        Map<String, String> defaultDescs = new HashMap<>();
        defaultDescs.put("API_URL", "Remote target container host API URL for Deploy & Run Remotely");
        defaultDescs.put("kafka.orders.endpoint", "Kafka Orders endpoint");
        defaultDescs.put("ibmmq-endpoint", "IBM MQ default endpoint");
        defaultDescs.put("ibmmq.request.queue", "IBM MQ Request queue");
        defaultDescs.put("ibmmq.reply.queue", "IBM MQ Reply queue");
        defaultDescs.put("ibmmq.dlq.queue", "IBM MQ Dead Letter Queue");
        defaultDescs.put("ibmmq.xa.request", "IBM MQ XA Request queue");
        defaultDescs.put("ibmmq.audit.trail", "IBM MQ Audit Trail queue");
        defaultDescs.put("notify.email.queue", "Email notification queue");
        defaultDescs.put("notify.sms.queue", "SMS notification queue");
        defaultDescs.put("mongodb.orders.endpoint", "MongoDB orders endpoint");
        defaultDescs.put("mongodb.audit.transactions", "MongoDB transaction audit endpoint");
        defaultDescs.put("http.orders.endpoint", "HTTP Orders endpoint");
        defaultDescs.put("http.downstream.endpoint", "HTTP Downstream API endpoint");
        defaultDescs.put("enricher.customer.service", "Customer Service direct stub");
        defaultDescs.put("camel.jbang.platform-http.port", "Camel JBang HTTP server port (controls Vert.x platform-http server port)");

        // Merge missing default values into the loaded list
        for (Map.Entry<String, String> entry : defaultValues.entrySet()) {
            String k = entry.getKey();
            if (!values.containsKey(k)) {
                keyOrder.add(k);
                values.put(k, entry.getValue());
                descriptions.put(k, defaultDescs.get(k));
            }
        }

        for (String k : keyOrder) {
            table.getItems().add(new PropertyRow(k, values.get(k), descriptions.getOrDefault(k, "")));
        }

        // Add form
        HBox inputBox = new HBox(8);
        inputBox.setPadding(new Insets(5, 0, 5, 0));

        TextField newKeyField = new TextField();
        newKeyField.setPromptText("New Key...");
        newKeyField.setPrefWidth(140);
        newKeyField.getStyleClass().add("variables-field");

        TextField newValField = new TextField();
        newValField.setPromptText("New Value...");
        newValField.setPrefWidth(140);
        newValField.getStyleClass().add("variables-field");

        TextField newDescField = new TextField();
        newDescField.setPromptText("Description...");
        newDescField.setPrefWidth(150);
        newDescField.getStyleClass().add("variables-field");

        Button btnAdd = new Button("Add");
        btnAdd.getStyleClass().add("variables-btn-add");
        btnAdd.setOnAction(e -> {
            String k = newKeyField.getText().trim();
            String v = newValField.getText().trim();
            String d = newDescField.getText().trim();
            if (!k.isEmpty()) {
                table.getItems().add(new PropertyRow(k, v, d));
                newKeyField.clear();
                newValField.clear();
                newDescField.clear();
            }
        });

        inputBox.getChildren().addAll(newKeyField, newValField, newDescField, btnAdd);

        // Action Buttons
        HBox actionBox = new HBox(10);
        actionBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnDelete = new Button("Delete Selected");
        btnDelete.getStyleClass().add("variables-btn-delete");
        btnDelete.setOnAction(e -> {
            PropertyRow selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                table.getItems().remove(selected);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSave = new Button("Save");
        btnSave.getStyleClass().add("variables-btn-save");
        btnSave.setPrefWidth(100);
        btnSave.setOnAction(e -> {
            try (PrintWriter writer = new PrintWriter(new java.io.FileWriter(propsFile))) {
                writer.println("# Managed by Camel Route Builder Studio");
                writer.println();
                for (PropertyRow row : table.getItems()) {
                    String k = row.getKey().trim();
                    String v = row.getValue().trim();
                    String desc = row.getDescription().trim();
                    if (k.isEmpty()) continue;
                    
                    if (!desc.isEmpty()) {
                        writer.println("# " + desc);
                    }
                    writer.println(k + "=" + v);
                    writer.println();
                }
                System.out.println("Saved variables to properties file: " + propsFile.getAbsolutePath());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                RouteBuilderApp.themeDialog(alert);
                alert.setTitle("Variables Saved");
                alert.setHeaderText("Properties saved successfully");
                alert.setContentText("Variables have been written to:\n" + propsFile.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                RouteBuilderApp.themeDialog(alert);
                alert.setTitle("Error");
                alert.setHeaderText("Could not save properties");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            stage.close();
        });

        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("variables-btn-cancel");
        btnCancel.setOnAction(e -> stage.close());

        Button btnHelp = new Button("Help");
        btnHelp.getStyleClass().add("variables-btn-cancel");
        btnHelp.setOnAction(e -> new RouteBuilderHelpWindow("Studio Configuration", "Variables").show());

        actionBox.getChildren().addAll(btnDelete, btnHelp, spacer, btnSave, btnCancel);

        layout.getChildren().addAll(titleLbl, descLbl, table, inputBox, actionBox);
        VBox.setVgrow(table, Priority.ALWAYS);

        com.tessera.ui.components.ThemeManager.registerRoot(layout);
        Scene scene = new Scene(layout, 540, 480);
        try {
            String css = VariablesEditorWindow.class.getResource("/styles/main.css").toExternalForm();
            scene.getStylesheets().add(css);
            if (RouteBuilderApp.currentDynamicCssUri != null) {
                scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
            }
        } catch (Exception ignored) {}
        
        // Register layout in the active themed roots registry
        RouteBuilderApp.themedRoots.add(layout);
        stage.setOnHidden(e -> RouteBuilderApp.themedRoots.remove(layout));

        stage.setScene(scene);
        stage.show();
    }
}
