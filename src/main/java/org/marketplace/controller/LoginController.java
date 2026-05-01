package org.marketplace.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button; // later use for login, register, & browse listing button
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 Controller class for the login screen (login.fxml).
 
 This controller handles:
    UI element initialization
    Password visibility toggle (checkbox)
    Authentication logic against demo credentials
    Action handlers for Login, Register, and Browse Listings buttons
 */
public class LoginController implements Initializable {

    // FXML Injected Fields
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordHiddenField;
    
    @FXML
    private TextField passwordVisibleField;
    
    @FXML
    private CheckBox showPasswordCheckBox;
    
    @FXML
    private Label messageLabel;
    
    // Constants
    
    // Demo credentials
    private static final String DEMO_CLIENT_USERNAME = "client";
    private static final String DEMO_CLIENT_PASSWORD = "password123";
    private static final String DEMO_ADMIN_USERNAME = "admin";
    private static final String DEMO_ADMIN_PASSWORD = "password123";
    
    // Initialization 
    
    /**
     Initializes the controller and sets up the password visibility toggle logic.
     Called automatically after the FXML file is loaded.
      
     @param url            The location used to resolve relative paths for the root object.
     @param resourceBundle The resources used to localize the root object.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initially hide the password visible field
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);
        
        // Clear any initial message
        messageLabel.setText("");
        
        // Setup the checkbox listener for password visibility toggle
        setupPasswordVisibilityToggle();
    }
    
    /**
     Sets up the bidirectional binding and visibility logic for the password fields.
     When the "Show Password" checkbox is checked:
        The password text is displayed in passwordVisibleField
        The passwordHiddenField becomes invisible/unmanaged
     When unchecked:
        The passwordHiddenField is shown again
        The passwordVisibleField becomes invisible/unmanaged
        Both fields are kept synchronized during input.
     */

    private void setupPasswordVisibilityToggle() {
        showPasswordCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Show password: copy text from hidden field to visible field
                passwordVisibleField.setText(passwordHiddenField.getText());
                passwordHiddenField.setManaged(false);
                passwordHiddenField.setVisible(false);
                passwordVisibleField.setManaged(true);
                passwordVisibleField.setVisible(true);
            } else {
                // Hide password: copy text from visible field to hidden field
                passwordHiddenField.setText(passwordVisibleField.getText());
                passwordVisibleField.setManaged(false);
                passwordVisibleField.setVisible(false);
                passwordHiddenField.setManaged(true);
                passwordHiddenField.setVisible(true);
            }
        });
        
        // Keep the password fields synchronized when user types
        passwordHiddenField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (showPasswordCheckBox.isSelected()) {
                passwordVisibleField.setText(newValue);
            }
        });
        
        passwordVisibleField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (showPasswordCheckBox.isSelected()) {
                passwordHiddenField.setText(newValue);
            }
        });
    }
    
    // Action Handlers
    
    /**
     * Handles the Login button click event.
     * 
     * Validates the username and password against hardcoded demo credentials:
     * - Client: username="client", password="password123"
     * - Admin: username="admin", password="password123"
     * 
     * Updates the messageLabel with success or error feedback.
     * 
     * @param event The ActionEvent triggered by the Login button.
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        // Get username from the username field
        String username = usernameField.getText().trim();
        
        // Get password from the active password field (hidden or visible)
        String password = showPasswordCheckBox.isSelected() 
            ? passwordVisibleField.getText() 
            : passwordHiddenField.getText();
        
        // Validate credentials
        if (authenticateUser(username, password)) {
            // Determine the user's role based on credentials
            String role = username.equals(DEMO_ADMIN_USERNAME) ? "Admin" : "Client";
            
            // Display success message in green
            messageLabel.setStyle("-fx-text-fill: #00AA00; -fx-font-size: 12;");
            messageLabel.setText("Login Successful! Role: " + role);
            
            System.out.println("✓ Login successful for user: " + username + " (Role: " + role + ")");
            
            // Navigate to the appropriate dashboard or main screen based on role
            // For now, just display the message
        } else {
            // Display error message in red
            messageLabel.setStyle("-fx-text-fill: #CC0000; -fx-font-size: 12;");
            messageLabel.setText("Invalid credentials.");
            
            System.out.println("✗ Login failed for user: " + username);
            
            // Clear the password fields for security
            passwordHiddenField.clear();
            passwordVisibleField.clear();
        }
    }
    
    /**
     Handles the Register button click event.
     Currently displays a navigation message to the register screen.
     
     @param event The ActionEvent triggered by the Register button.
     */

    @FXML
    private void handleRegister(ActionEvent event) {
        messageLabel.setStyle("-fx-text-fill: #0066CC; -fx-font-size: 12;");
        messageLabel.setText("Navigating to Register Screen...");
        
        System.out.println("→ Register button clicked. Navigating to registration screen...");
        
        // Load and display the registration screen
    }
    
    /**
     Handles the Browse Listings button click event.
     Allows users to continue as a guest and browse available listings.
     
     @param event The ActionEvent triggered by the Browse Listings button.
     */
    @FXML
    private void handleBrowseListings(ActionEvent event) {
        messageLabel.setStyle("-fx-text-fill: #0066CC; -fx-font-size: 12;");
        messageLabel.setText("Continuing as Guest / Browsing Listings...");
        
        System.out.println("→ Browse Listings button clicked. Loading listings as guest...");
        
        // Load and display the listings screen
    }
    
    // Authentication Logic
    
    /**
     Authenticates a user based on their username and password.
     Checks against the hardcoded demo credentials:
     Client: username="client", password="password123"
     Admin: username="admin", password="password123"
     
     @param username The username entered by the user.
     @param password The password entered by the user.
     @return true if the credentials match a valid user, false otherwise.
     */
    private boolean authenticateUser(String username, String password) {
        // Check for client credentials
        if (username.equals(DEMO_CLIENT_USERNAME) && password.equals(DEMO_CLIENT_PASSWORD)) {
            return true;
        }
        
        // Check for admin credentials
        if (username.equals(DEMO_ADMIN_USERNAME) && password.equals(DEMO_ADMIN_PASSWORD)) {
            return true;
        }
        
        // No match found
        return false;
    }
}
