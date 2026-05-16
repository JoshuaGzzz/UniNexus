package com.studentmarketplace.controller;

import com.studentmarketplace.MainApplication;
import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.service.AfterSaleService;
import com.studentmarketplace.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class RefundRequestController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(RefundRequestController.class);

    @FXML
    private ComboBox<String> orderDropdown;
    @FXML
    private TextArea reasonArea;
    @FXML
    private Button submitButton;
    @FXML
    private Label messageLabel;

    private final AfterSaleService afterSaleService = new AfterSaleService();
    private final Map<String, Integer> orderMap = new LinkedHashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadBuyerTransactions();
    }

    private void loadBuyerTransactions() {
        int buyerId = SessionManager.getInstance().getCurrentUserId();
        System.out.println("[REFUND] loadBuyerTransactions called. buyerId=" + buyerId);

        if (buyerId <= 0) {
            showMessage("You must be logged in to request a refund.", "-fx-text-fill: #e74c3c;");
            submitButton.setDisable(true);
            return;
        }

        String sql = "SELECT t.transaction_id, t.amount, t.transaction_date, p.title " +
                "FROM transactions t " +
                "JOIN posts p ON p.post_id = t.post_id " +
                "WHERE t.buyer_id = ? AND t.status = 'COMPLETED' " +
                "ORDER BY t.transaction_date DESC";

        System.out.println("[REFUND] Running query for buyer_id=" + buyerId);
        try (PreparedStatement stmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setInt(1, buyerId);
            try (ResultSet rs = stmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    int transactionId = rs.getInt("transaction_id");
                    double amount = rs.getDouble("amount");
                    String date = rs.getString("transaction_date");
                    String title = rs.getString("title");
                    System.out.println("[REFUND] Found transaction: id=" + transactionId
                            + " title=" + title + " amount=" + amount + " date=" + date);

                    String display = "Order #" + transactionId + " — " + title
                            + " (Php " + String.format("%.2f", amount) + ")";
                    orderMap.put(display, transactionId);
                    orderDropdown.getItems().add(display);
                }
                System.out.println("[REFUND] Total transactions found: " + count);
            }
        } catch (Exception e) {
            System.out.println("[REFUND] EXCEPTION in query: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            logger.error("Failed to load buyer transactions for dropdown", e);
        }

        if (orderDropdown.getItems().isEmpty()) {
            System.out.println("[REFUND] Dropdown is empty - no completed orders for buyer " + buyerId);
            orderDropdown.setPromptText("No completed orders found");
            submitButton.setDisable(true);
        }
    }

    private void showMessage(String text, String style) {
        messageLabel.setText(text);
        messageLabel.setStyle(style);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.setStyle("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    @FXML
    private void handleSubmit() {
        clearMessage();

        String selected = orderDropdown.getValue();
        if (selected == null) {
            showMessage("Please select an order.", "-fx-text-fill: #e74c3c;");
            return;
        }

        String reason = reasonArea.getText() == null ? "" : reasonArea.getText().trim();
        if (reason.isEmpty()) {
            showMessage("Please provide a reason for the refund.", "-fx-text-fill: #e74c3c;");
            return;
        }

        int buyerId = SessionManager.getInstance().getCurrentUserId();
        Integer transactionId = orderMap.get(selected);
        if (transactionId == null || buyerId <= 0) {
            showMessage("Invalid selection or session expired.", "-fx-text-fill: #e74c3c;");
            return;
        }

        boolean success = afterSaleService.submitRefund(buyerId, transactionId, reason);
        if (success) {
            showMessage("Refund request submitted successfully!", "-fx-text-fill: #27ae60;");
            orderDropdown.setValue(null);
            reasonArea.clear();
        } else {
            showMessage("Failed to submit refund request. Please try again.", "-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void handleClear() {
        orderDropdown.setValue(null);
        reasonArea.clear();
        clearMessage();
    }

    @FXML
    private void handleViewStatus() {
        MainApplication.showRefundStatusView();
    }

    @FXML
    private void handleBack() {
        MainApplication.showClientDashboard(SessionManager.getInstance().getCurrentSession());
    }
}
