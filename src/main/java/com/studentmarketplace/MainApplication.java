package com.studentmarketplace;

import com.studentmarketplace.controller.AdminDashboardController;
import com.studentmarketplace.controller.ClientDashboardController;
import com.studentmarketplace.controller.ClientMarketplaceController;
import com.studentmarketplace.controller.ProfileController;
import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.model.UserSession;
import com.studentmarketplace.util.SessionManager;
import com.studentmarketplace.util.StageManager;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the UniNexus JavaFX application.
 * Initializes the database, loads the primary scene, and manages the
 * application lifecycle.
 */
public class MainApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
    private static final String APP_TITLE = "UniNexus";
    private static final String APP_VERSION = "1.0.0";
    private static UserSession currentSession;

    @Override
    public void start(Stage primaryStage) {
        try {
            StageManager.getInstance().setPrimaryStage(primaryStage);
            logger.info("Starting {} v{}", APP_TITLE, APP_VERSION);

            // Initialize database
            DatabaseManager.getInstance();
            logger.info("Database initialized");

            // Set app icon
            try {
                java.net.URL iconUrl = getClass().getResource("/images/app-icon.png");
                if (iconUrl != null) {
                    primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
                } else {
                    logger.warn("Application icon not found: images/app-icon.png");
                }
            } catch (Exception e) {
                logger.warn("Could not load application icon", e);
            }

            // Set window properties
            primaryStage.setWidth(1200);
            primaryStage.setHeight(700);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            showLoginView();
            logger.info("Application started successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorAndExit("Application startup failed", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Closing application");
        try {
            DatabaseManager.getInstance().closeConnection();
            logger.info("Database connection closed");
        } catch (Exception e) {
            logger.error("Error closing database connection", e);
        }
    }

    public static void showLoginView() {
        currentSession = null;
        SessionManager.getInstance().clear();
        UserSession.clearCurrent();
        loadScene("fxml/login.fxml", APP_TITLE + " - Login", 1000, 700, null);
    }

    public static void showDormitoryListing() {
        loadScene("fxml/dormitory-listing.fxml", APP_TITLE + " - Marketplace", 1200, 700, null);
    }

    public static void showRegisterView() {
        loadScene("fxml/register.fxml", APP_TITLE + " - Register", 1000, 760, null);
    }

    public static void showClientDashboard(UserSession session) {
        currentSession = session;
        SessionManager.getInstance().setSession(SessionManager.getInstance().getCurrentUser(), session);
        UserSession.setCurrent(SessionManager.getInstance().getCurrentUser(), session);
        loadScene("fxml/client-dashboard.fxml", APP_TITLE + " - Client", 1200, 700, controller -> {
            if (controller instanceof ClientDashboardController clientController) {
                clientController.setSession(session);
            }
        });
    }

    public static void showAdminDashboard(UserSession session) {
        currentSession = session;
        SessionManager.getInstance().setSession(SessionManager.getInstance().getCurrentUser(), session);
        UserSession.setCurrent(SessionManager.getInstance().getCurrentUser(), session);
        loadScene("fxml/admin-dashboard.fxml", APP_TITLE + " - Admin", 1200, 700, controller -> {
            if (controller instanceof AdminDashboardController adminController) {
                adminController.setSession(session);
            }
        });
    }

    public static void showClientMarketplace(UserSession session) {
        currentSession = session;
        SessionManager.getInstance().setSession(SessionManager.getInstance().getCurrentUser(), session);
        UserSession.setCurrent(SessionManager.getInstance().getCurrentUser(), session);
        loadScene("fxml/client-marketplace.fxml", APP_TITLE + " - My Marketplace", 1250, 760, controller -> {
            if (controller instanceof ClientMarketplaceController marketplaceController) {
                marketplaceController.setSession(session);
            }
        });
    }

    public static void showProfileView() {
        loadScene("fxml/profile.fxml", APP_TITLE + " - Profile", 980, 700, controller -> {
            if (controller instanceof ProfileController) {
                // no-op; controller initializes from session singleton
            }
        });
    }

    public static void showRefundRequestView() {
        loadScene("fxml/refund-request.fxml", APP_TITLE + " - Refund Request", 800, 600, null);
    }

    public static void showRefundStatusView() {
        loadScene("fxml/refund-status.fxml", APP_TITLE + " - My Refunds", 900, 600, null);
    }

    public static void showRefundAdminView() {
        loadScene("fxml/refund-admin.fxml", APP_TITLE + " - Refund Management", 1100, 650, null);
    }

    public static UserSession getCurrentSession() {
        return currentSession;
    }

    public static void navigateBackFromListing() {
        if (currentSession == null) {
            showLoginView();
            return;
        }

        if (currentSession.getRole() == UserSession.Role.ADMIN) {
            showAdminDashboard(currentSession);
        } else {
            showClientDashboard(currentSession);
        }
    }

    private static void loadScene(String fxmlPath, String title, int width, int height,
            java.util.function.Consumer<Object> controllerInitializer) {
        StageManager.getInstance().switchScene(fxmlPath, title, width, height, controllerInitializer);
    }

    /**
     * Show error dialog and exit
     */
    private void showErrorAndExit(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();
        System.exit(1);
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        launch(args);
    }
}
