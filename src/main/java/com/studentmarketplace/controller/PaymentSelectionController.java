package com.studentmarketplace.controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;

/**
 * Controller for the payment selection bridge during checkout.
 */
public class PaymentSelectionController {
    @FXML private VBox paymentRoot;
    @FXML private RadioButton codRadio;
    @FXML private RadioButton ewalletRadio;
    @FXML private RadioButton cardRadio;
    @FXML private ImageView paymentIconView;
    @FXML private Label selectionLabel;
    @FXML private Button placeOrderButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator verificationIndicator;

    private final ToggleGroup paymentGroup = new ToggleGroup();
    private Stage dialogStage;
    private PaymentChoice confirmedChoice;

    @FXML
    public void initialize() {
        codRadio.setToggleGroup(paymentGroup);
        ewalletRadio.setToggleGroup(paymentGroup);
        cardRadio.setToggleGroup(paymentGroup);

        codRadio.setUserData(PaymentChoice.COD);
        ewalletRadio.setUserData(PaymentChoice.EWALLET);
        cardRadio.setUserData(PaymentChoice.CARD);

        placeOrderButton.setDisable(true);
        verificationIndicator.setVisible(false);

        paymentGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            placeOrderButton.setDisable(newToggle == null);
            updateSelectedVisual(newToggle);
        });
    }

    @FXML
    private void handlePlaceOrder() {
        Toggle selectedToggle = paymentGroup.getSelectedToggle();
        if (selectedToggle == null) {
            return;
        }

        confirmedChoice = (PaymentChoice) selectedToggle.getUserData();

        placeOrderButton.setDisable(true);
        cancelButton.setDisable(true);
        codRadio.setDisable(true);
        ewalletRadio.setDisable(true);
        cardRadio.setDisable(true);
        verificationIndicator.setVisible(true);

        PauseTransition verificationDelay = new PauseTransition(Duration.seconds(2));
        verificationDelay.setOnFinished(evt -> closeDialog());
        verificationDelay.play();
    }

    @FXML
    private void handleCancel() {
        confirmedChoice = null;
        closeDialog();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public PaymentChoice getConfirmedChoice() {
        return confirmedChoice;
    }

    private void updateSelectedVisual(Toggle selectedToggle) {
        if (selectedToggle == null) {
            selectionLabel.setText("No payment method selected");
            paymentIconView.setImage(null);
            return;
        }

        PaymentChoice choice = (PaymentChoice) selectedToggle.getUserData();
        selectionLabel.setText("Selected: " + choice.displayLabel());

        Image icon = loadIcon(choice.iconResourcePath());
        paymentIconView.setImage(icon);
    }

    private Image loadIcon(String iconResourcePath) {
        try (InputStream stream = getClass().getResourceAsStream(iconResourcePath)) {
            if (stream == null) {
                return null;
            }
            return new Image(stream);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void closeDialog() {
        Stage stage = dialogStage != null
                ? dialogStage
                : (Stage) paymentRoot.getScene().getWindow();
        stage.close();
    }

    public enum PaymentChoice {
        COD("Cash on Delivery (COD)", "CASH", "/images/payment/COD.png"),
        EWALLET("QR Ph / Maya / GCash", "ONLINE_TRANSFER", "/images/payment/Maya.png"),
        CARD("Credit / Debit Card (Visa/Mastercard)", "CREDIT_CARD", "/images/payment/visa.png");

        private final String displayLabel;
        private final String databaseValue;
        private final String iconResourcePath;

        PaymentChoice(String displayLabel, String databaseValue, String iconResourcePath) {
            this.displayLabel = displayLabel;
            this.databaseValue = databaseValue;
            this.iconResourcePath = iconResourcePath;
        }

        public String displayLabel() {
            return displayLabel;
        }

        public String databaseValue() {
            return databaseValue;
        }

        public String iconResourcePath() {
            return iconResourcePath;
        }
    }
}
