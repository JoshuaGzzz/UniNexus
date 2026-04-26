package org.marketplace;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("It finally works.");
        Scene scene = new Scene(new StackPane(label), 400, 300);
        stage.setScene(scene);
        stage.setTitle("Success");
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}