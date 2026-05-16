package com.studentmarketplace.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Centralized scene navigation to avoid duplicated stage handling logic.
 */
public final class StageManager {
    private static StageManager instance;

    private Stage primaryStage;

    private StageManager() {
    }

    public static synchronized StageManager getInstance() {
        if (instance == null) {
            instance = new StageManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void switchScene(String fxmlPath,
                            String title,
                            int width,
                            int height,
                            Consumer<Object> controllerInitializer) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage not configured.");
        }

        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    StageManager.class.getResource("/" + fxmlPath),
                    "Resource not found: " + fxmlPath
            ));

            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(Objects.requireNonNull(
                    StageManager.class.getResource("/css/global.css"),
                    "Resource not found: css/global.css"
            ).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(
                    StageManager.class.getResource("/css/dark-theme.css"),
                    "Resource not found: css/dark-theme.css"
            ).toExternalForm());

            if (controllerInitializer != null) {
                controllerInitializer.accept(loader.getController());
            }

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }
}
