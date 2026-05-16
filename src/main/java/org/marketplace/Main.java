package org.marketplace;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("UniNexus - Spartan Student Marketplace");
        goToFeed();
        stage.show();
    }

    // ─── Routing Methods ─────────────────────────────────────────────────────

    public static void goToLogin() throws IOException {
        loadScene("/view/login.fxml");
    }

    public static void goToRegistration() throws IOException {
        loadScene("/view/registration.fxml");
    }

    public static void goToFeed() throws IOException {
        loadScene("/view/buyer_feed.fxml");
    }

    public static void goToSellForm() throws IOException {
        loadScene("/view/sell_form.fxml");
    }

    public static void goToAdmin() throws IOException {
        loadScene("/view/admin_dashboard.fxml");
    }

    public static void goToCart() throws IOException {
        loadScene("/view/cart.fxml");
    }

    public static void goToCheckout() throws IOException {
        loadScene("/view/checkout.fxml");
    }

    // ─── Internal helper ─────────────────────────────────────────────────────

    private static void loadScene(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(Main.class.getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}