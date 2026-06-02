package com.example.datamapper;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.stage.Modality;

/**
 * Controller for the mapping expression dialog.
 * Currently provides simple OK/Cancel handling and displays the entered
 * expression in an informational alert when OK is pressed.
 */
public class MappingDialogController {

    @FXML private TextArea expressionArea;

    /** Called when the user clicks the OK button. */
    @FXML
    private void handleOk() {
        String expr = expressionArea != null ? expressionArea.getText() : "";
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Mapping expression:\n" + expr, ButtonType.OK);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
        closeWindow();
    }

    /** Called when the user clicks the Cancel button. */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /** Utility to close the dialog window. */
    private void closeWindow() {
        // The TextArea is part of the scene; get its window and close it.
        if (expressionArea != null) {
            Stage stage = (Stage) expressionArea.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
    // Getter for expression used by the mapper
    public String getExpression() {
        return expressionArea != null ? expressionArea.getText() : null;
    }
}
