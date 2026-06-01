package com.tessera.ui;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestYamlEditor extends Application {
    @Override
    public void start(Stage primaryStage) {
        YamlEditorPane pane = new YamlEditorPane(text -> {}, () -> {});
        pane.setText("hello world");
        primaryStage.setScene(new Scene(pane, 800, 600));
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
