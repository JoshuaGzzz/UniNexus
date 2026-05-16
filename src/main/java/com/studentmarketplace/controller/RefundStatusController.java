package com.studentmarketplace.controller;

import com.studentmarketplace.MainApplication;
import com.studentmarketplace.model.RefundRequest;
import com.studentmarketplace.service.AfterSaleService;
import com.studentmarketplace.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
 * Controller for the buyer's "My Refund Requests" view.
 */
public class RefundStatusController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(RefundStatusController.class);

    @FXML private TableView<RefundRequest> refundTable;
    @FXML private TableColumn<RefundRequest, String> colRefundId;
    @FXML private TableColumn<RefundRequest, String> colTransactionId;
    @FXML private TableColumn<RefundRequest, String> colReason;
    @FXML private TableColumn<RefundRequest, String> colStatus;
    @FXML private TableColumn<RefundRequest, String> colDate;
    @FXML private Label messageLabel;

    private final AfterSaleService afterSaleService = new AfterSaleService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupColumns();
        loadRefunds();
    }

    private void setupColumns() {
        colRefundId.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getRefundId())));

        colTransactionId.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getTransactionId())));

        colReason.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getReason()));

        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));

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
                    case "PENDING"  -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    default         -> setStyle("-fx-font-weight: bold;");
                }
            }
        });

        colDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCreatedAt()));
    }

    private void loadRefunds() {
        int buyerId = SessionManager.getInstance().getCurrentUserId();
        if (buyerId <= 0) {
            messageLabel.setText("You must be logged in to view refund history.");
            return;
        }

        List<RefundRequest> refunds = afterSaleService.getBuyerRefunds(buyerId);
        refundTable.setItems(FXCollections.observableArrayList(refunds));

        if (refunds.isEmpty()) {
            messageLabel.setText("You have not submitted any refund requests yet.");
        } else {
            messageLabel.setText("");
        }
    }

    @FXML
    private void handleSubmitNew() {
        MainApplication.showRefundRequestView();
    }

    @FXML
    private void handleBack() {
        MainApplication.showClientDashboard(SessionManager.getInstance().getCurrentSession());
    }
}
