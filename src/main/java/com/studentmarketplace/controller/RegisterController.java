package com.studentmarketplace.controller;

import com.studentmarketplace.MainApplication;
import com.studentmarketplace.model.UserSession;
import com.studentmarketplace.service.AuthService;
import com.studentmarketplace.service.AuthService.AuthPayload;
import com.studentmarketplace.service.AuthService.RegistrationResult;
import com.studentmarketplace.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Handles account registration for real user creation.
 */
public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField fullNameField;
    @FXML private TextField studentIdField;
    @FXML private TextField universityField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleRegister() {
        String username = safe(usernameField.getText());
        String email = safe(emailField.getText());
        String fullName = safe(fullNameField.getText());
        String studentId = safe(studentIdField.getText());
        String university = safe(universityField.getText());
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (!password.equals(confirm)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        if (password.length() < 8) {
            messageLabel.setText("Password must be at least 8 characters.");
            return;
        }

        RegistrationResult result = authService.registerDetailed(
                username, email, fullName, studentId, university, password
        );
        if (!result.success() || result.session().isEmpty()) {
            messageLabel.setText(result.message());
            return;
        }

        AuthPayload payload = result.session().get();
        UserSession session = payload.session();
        SessionManager.getInstance().setSession(payload.user(), session);
        UserSession.setCurrent(payload.user(), session);
        MainApplication.showClientDashboard(session);
    }

    @FXML
    public void handleBackToLogin() {
        MainApplication.showLoginView();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
