package com.example.datamapper;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class VisualDataMapperApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/datamapper/ui/visual-datamapper.fxml"));
        primaryStage.setTitle("Visual DataMapper");
        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
