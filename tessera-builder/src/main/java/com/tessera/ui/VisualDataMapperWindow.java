package com.tessera.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Window wrapper for the Visual DataMapper tool.
 * This mirrors the pattern used for other studio windows (e.g., CryptoStudioWindow).
 */
public class VisualDataMapperWindow {
    private static final String TITLE = "Visual DataMapper";
    private static final String FXML_PATH = "/datamapper/ui/visual-datamapper.fxml";

    public static void show() {
        try {
            FXMLLoader loader = new FXMLLoader(VisualDataMapperWindow.class.getResource(FXML_PATH));
            Stage stage = new Stage();
            stage.setTitle(TITLE);
            stage.initModality(Modality.NONE);
            stage.setScene(new Scene(loader.load()));
            stage.show();
        } catch (IOException e) {
            // For simplicity, print the stacktrace. In production we would log appropriately.
            e.printStackTrace();
        }
    }
}
