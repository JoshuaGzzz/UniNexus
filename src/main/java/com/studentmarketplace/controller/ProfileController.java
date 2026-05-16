package com.studentmarketplace.controller;

import com.studentmarketplace.MainApplication;
import com.studentmarketplace.model.User;
import com.studentmarketplace.model.UserSession;
import com.studentmarketplace.service.UserService;
import com.studentmarketplace.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Profile management screen for updating personal info.
 */
public class ProfileController {
    @FXML private Label usernameLabel;
    @FXML private Label fullNameLabel;
    @FXML private TextField universityField;
    @FXML private TextField contactField;
    @FXML private TextArea bioArea;
    @FXML private Label statusLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        User current = SessionManager.getInstance().getCurrentUser();
        UserSession session = SessionManager.getInstance().getCurrentSession();

        if (current != null) {
            usernameLabel.setText(current.getUsername());
            fullNameLabel.setText(current.getFullName());
            universityField.setText(current.getUniversity());
            contactField.setText(current.getPhone() == null ? "" : current.getPhone());
            bioArea.setText(current.getBio() == null ? "" : current.getBio());
        } else if (session != null) {
            usernameLabel.setText(session.getUsername());
            fullNameLabel.setText(session.getFullName());
        }
    }

    @FXML
    public void handleSaveProfile() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        if (userId <= 0) {
            statusLabel.setText("Session expired. Please login again.");
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return userService.updateProfile(
                        userId,
                        universityField.getText(),
                        contactField.getText(),
                        bioArea.getText()
                );
            }
        };

        task.setOnSucceeded(evt -> {
            if (task.getValue()) {
                userService.getUserById(userId).ifPresent(user -> SessionManager.getInstance().setSession(user, SessionManager.getInstance().getCurrentSession()));
                statusLabel.setText("Profile updated successfully.");
            } else {
                statusLabel.setText("Failed to update profile.");
            }
        });
        task.setOnFailed(evt -> statusLabel.setText("Failed to update profile."));

        Thread thread = new Thread(task, "profile-update-worker");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleBack() {
        UserSession session = SessionManager.getInstance().getCurrentSession();
        if (session != null) {
            MainApplication.showClientDashboard(session);
        } else {
            MainApplication.showLoginView();
        }
    }
}
