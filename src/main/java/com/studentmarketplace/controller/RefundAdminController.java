package com.studentmarketplace.controller;

import com.studentmarketplace.model.RefundRequest;
import com.studentmarketplace.service.AfterSaleService;
import com.studentmarketplace.MainApplication;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Admin controller for reviewing and acting on all refund requests.
 */
public class RefundAdminController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(RefundAdminController.class);

    @FXML
    private TableView<RefundRequest> refundTable;
    @FXML
    private TableColumn<RefundRequest, String> colRefundId;
    @FXML
    private TableColumn<RefundRequest, String> colBuyer;
    @FXML
    private TableColumn<RefundRequest, String> colTransactionId;
    @FXML
    private TableColumn<RefundRequest, String> colReason;
    @FXML
    private TableColumn<RefundRequest, String> colStatus;
    @FXML
    private TableColumn<RefundRequest, String> colDate;
    @FXML
    private Button approveButton;
    @FXML
    private Button rejectButton;
    @FXML
    private Label messageLabel;

    private final AfterSaleService afterSaleService = new AfterSaleService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupColumns();
        loadRefunds();
    }

    private void setupColumns() {
        colRefundId
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getRefundId())));

        colBuyer.setCellValueFactory(data -> new SimpleStringProperty("User #" + data.getValue().getBuyerId()));

        colTransactionId.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getTransactionId())));

        colReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason()));

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                switch (status.toUpperCase()) {
                    case "APPROVED" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "REJECTED" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    case "PENDING" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    default -> setStyle("-fx-font-weight: bold;");
                }
            }
        });

        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt()));
    }

    private void loadRefunds() {
        List<RefundRequest> refunds = afterSaleService.getAllRefunds();
        refundTable.setItems(FXCollections.observableArrayList(refunds));
        messageLabel.setText("");
    }

    @FXML
    private void handleApprove() {
        updateSelectedStatus("APPROVED");
    }

    @FXML
    private void handleReject() {
        updateSelectedStatus("REJECTED");
    }

    @FXML
    private void handleRefresh() {
        loadRefunds();
    }

    private void updateSelectedStatus(String newStatus) {
        RefundRequest selected = refundTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Please select a refund request first.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        boolean success = afterSaleService.updateRefundStatus(selected.getRefundId(), newStatus);
        if (success) {
            messageLabel.setText("Refund #" + selected.getRefundId() + " " + newStatus.toLowerCase() + ".");
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            loadRefunds();
        } else {
            messageLabel.setText("Failed to update refund status. Please try again.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void handleBackToDashboard() {
        MainApplication.navigateBackFromListing();
    }
}