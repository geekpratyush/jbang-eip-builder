package com.example.datamapper;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Controller for the Visual Data Mapper UI.
 * Handles opening a scenario (selecting a folder under sample/datamapper) and
 * loading its source/target XSD files into the respective TreeViews.
 */
public class VisualDataMapperController {
    @FXML private TreeView<String> scenarioTree;
    @FXML private TreeView<String> sourceTree;
    @FXML private TreeView<String> targetTree;
    @FXML private TableView<Mapping> mappingTable;
    @FXML private TableColumn<Mapping, String> srcColumn;
    @FXML private TableColumn<Mapping, String> tgtColumn;

    @FXML
    private void initialize() {
        loadScenarioTree();
    }

    /** Loads the list of scenario folders into the left‑hand TreeView. */
    private void loadScenarioTree() {
        try {
            URL baseUrl = getClass().getResource("/sample/datamapper");
            if (baseUrl == null) {
                showError("Scenario directory not found.");
                return;
            }
            Path base = Path.of(baseUrl.toURI());
            TreeItem<String> root = new TreeItem<>("Scenarios");
            root.setExpanded(true);
            try (Stream<Path> dirs = Files.list(base)) {
                dirs.filter(Files::isDirectory).forEach(dir -> {
                    TreeItem<String> item = new TreeItem<>(dir.getFileName().toString());
                    root.getChildren().add(item);
                });
            }
            scenarioTree.setRoot(root);
            scenarioTree.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && !newV.getValue().equals("Scenarios")) {
                    loadScenario(newV.getValue());
                }
            });
        } catch (IOException | URISyntaxException e) {
            showError("Failed to load scenarios: " + e.getMessage());
        }
    }

    /** Opens a dialog to let the user pick a scenario and then loads it. */
    @FXML
    private void handleOpenScenario() {
        List<String> scenarios = getScenarioNames();
        if (scenarios.isEmpty()) {
            showInfo("No scenarios available.");
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(scenarios.get(0), scenarios);
        dialog.setTitle("Open Scenario");
        dialog.setHeaderText("Select a scenario to load");
        dialog.initModality(Modality.APPLICATION_MODAL);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(this::loadScenario);
    }

    /** Returns the names of scenario folders under sample/datamapper. */
    private List<String> getScenarioNames() {
        List<String> names = new ArrayList<>();
        try {
            URL baseUrl = getClass().getResource("/sample/datamapper");
            if (baseUrl == null) return names;
            Path base = Path.of(baseUrl.toURI());
            try (Stream<Path> dirs = Files.list(base)) {
                dirs.filter(Files::isDirectory)
                    .forEach(p -> names.add(p.getFileName().toString()));
            }
        } catch (IOException | URISyntaxException ignored) {}
        return names;
    }

    /** Loads the selected scenario's source.xsd and target.xsd into the TreeViews. */
    private void loadScenario(String scenario) {
        try {
            URL baseUrl = getClass().getResource("/sample/datamapper/" + scenario);
            if (baseUrl == null) {
                showError("Scenario not found: " + scenario);
                return;
            }
            Path scenarioPath = Path.of(baseUrl.toURI());
            sourceTree.setRoot(buildTreeFromFile(scenarioPath.resolve("source.xsd")));
            targetTree.setRoot(buildTreeFromFile(scenarioPath.resolve("target.xsd")));
            // Load sample mappings into TableView
            ObservableList<Mapping> data = FXCollections.observableArrayList();
            Path mappingFile = scenarioPath.resolve("mapping.txt");
            if (Files.exists(mappingFile)) {
                Files.lines(mappingFile).forEach(line -> {
                    if (!line.trim().isEmpty()) {
                        String[] parts = line.split("->");
                        if (parts.length == 2) {
                            data.add(new Mapping(parts[0].trim(), parts[1].trim()));
                        } else {
                            data.add(new Mapping(line.trim(), ""));
                        }
                    }
                });
            }
            mappingTable.setItems(data);
            // Configure columns
            srcColumn.setCellValueFactory(cellData -> cellData.getValue().sourceProperty());
            tgtColumn.setCellValueFactory(cellData -> cellData.getValue().targetProperty());
        } catch (IOException | URISyntaxException e) {
            showError("Failed to load scenario " + scenario + ": " + e.getMessage());
        }
    }

    /** Simple line‑by‑line conversion of an XSD file into a TreeItem hierarchy. */
    private TreeItem<String> buildTreeFromFile(Path file) throws IOException {
        TreeItem<String> root = new TreeItem<>(file.getFileName().toString());
        Files.lines(file).forEach(line -> root.getChildren().add(new TreeItem<>(line.trim())));
        root.setExpanded(true);
        return root;
    }

    /** Helper to show an informational dialog. */
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.initModality(Modality.APPLICATION_MODAL);
        a.showAndWait();
    }

    /** Helper to show an error dialog. */
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.initModality(Modality.APPLICATION_MODAL);
        a.showAndWait();
    }

    @FXML
    private void handleGenerateMapping() {
        // For now just show a simple info that mappings are ready
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Mapping generation not implemented yet.", ButtonType.OK);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }

    @FXML
    private void handleAddMapping() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/datamapper/ui/mapping-dialog.fxml"));
            Stage dialog = new Stage();
            dialog.setTitle("Define Mapping Expression");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new javafx.scene.Scene(loader.load()));
            Object ctrl = loader.getController();
            dialog.showAndWait();
            if (ctrl instanceof com.example.datamapper.MappingDialogController) {
                String expr = ((com.example.datamapper.MappingDialogController) ctrl).getExpression();
                if (expr != null && !expr.isBlank()) {
                    String[] parts = expr.split("->");
                    if (parts.length == 2) {
                        Mapping m = new Mapping(parts[0].trim(), parts[1].trim());
                        mappingTable.getItems().add(m);
                    } else {
                        Mapping m = new Mapping(expr.trim(), "");
                        mappingTable.getItems().add(m);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
