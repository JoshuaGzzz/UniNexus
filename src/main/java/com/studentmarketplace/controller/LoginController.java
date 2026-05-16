package com.studentmarketplace.controller;

import com.studentmarketplace.MainApplication;
import com.studentmarketplace.model.User;
import com.studentmarketplace.model.UserSession;
import com.studentmarketplace.service.AuthService;
import com.studentmarketplace.service.AuthService.AuthPayload;
import com.studentmarketplace.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

/**
 * Handles login screen actions and routes users by role.
 */
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        visiblePasswordField.managedProperty().bind(showPasswordCheckBox.selectedProperty());
        visiblePasswordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());

        passwordField.managedProperty().bind(showPasswordCheckBox.selectedProperty().not());
        passwordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());

        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password.");
            return;
        }

        Optional<AuthPayload> payload = authService.loginWithUser(username, password);
        if (payload.isEmpty()) {
            messageLabel.setText("Invalid credentials. Try demo accounts below.");
            return;
        }

        User user = payload.get().user();
        UserSession session = payload.get().session();
        SessionManager.getInstance().setSession(user, session);
        UserSession.setCurrent(user, session);

        if (session.getRole() == UserSession.Role.ADMIN) {
            MainApplication.showAdminDashboard(session);
        } else {
            MainApplication.showClientDashboard(session);
        }
    }

    @FXML
    public void handleOpenMarketplace() {
        MainApplication.showDormitoryListing();
    }

    @FXML
    public void handleOpenRegister() {
        MainApplication.showRegisterView();
    }
}
