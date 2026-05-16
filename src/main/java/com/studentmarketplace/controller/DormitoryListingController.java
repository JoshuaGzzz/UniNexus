package com.studentmarketplace.controller;

import com.studentmarketplace.MainApplication;
import com.studentmarketplace.model.AcademicResource;
import com.studentmarketplace.model.Post;
import com.studentmarketplace.model.Product;
import com.studentmarketplace.model.Rental;
import com.studentmarketplace.service.MarketplaceService;
import com.studentmarketplace.util.CartManager;
import com.studentmarketplace.util.ImageUtil;
import com.studentmarketplace.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Buyer marketplace with dynamic cards, details, cart, and checkout.
 */
public class DormitoryListingController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DormitoryListingController.class);

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> locationFilter;
    @FXML
    private ComboBox<String> priceRangeFilter;
    @FXML
    private TilePane postTilePane;
    @FXML
    private Label noResultsLabel;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Button backButton;

    @FXML
    private Label detailTitleLabel;
    @FXML
    private Label detailTypeLabel;
    @FXML
    private Label detailPriceLabel;
    @FXML
    private Label detailStatusLabel;
    @FXML
    private TextArea detailDescriptionArea;
    @FXML
    private ImageView detailImageView;

    @FXML
    private ListView<Post> cartListView;
    @FXML
    private Label cartTotalLabel;

    private final MarketplaceService marketplaceService = new MarketplaceService();
    private final CartManager cartManager = CartManager.getInstance();

    private final ObservableList<Post> loadedPosts = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupNavigationButton();
        setupFilters();
        setupCartList();
        setupListeners();
        loadMarketplacePostsAsync();
    }

    private void setupNavigationButton() {
        if (backButton == null) {
            return;
        }

        if (SessionManager.getInstance().getCurrentSession() == null) {
            backButton.setText("Back to Login");
            return;
        }

        switch (SessionManager.getInstance().getCurrentSession().getRole()) {
            case ADMIN -> backButton.setText("Back to Admin");
            case CLIENT -> backButton.setText("Back to Dashboard");
        }
    }

    private void setupFilters() {
        locationFilter.setItems(FXCollections.observableArrayList(
                "All Locations",
                "Quiapo",
                "Sampaloc",
                "Diliman",
                "Makati",
                "BGC",
                "Pasig"));
        locationFilter.setValue("All Locations");

        priceRangeFilter.setItems(FXCollections.observableArrayList(
                "All Prices",
                "0-5000",
                "5000-10000",
                "10000-15000",
                "15000+"));
        priceRangeFilter.setValue("All Prices");
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderCards());
        locationFilter.valueProperty().addListener((obs, oldVal, newVal) -> renderCards());
        priceRangeFilter.valueProperty().addListener((obs, oldVal, newVal) -> renderCards());
        cartManager.getCartItems()
                .addListener((javafx.collections.ListChangeListener<Post>) change -> updateCartTotal());
    }

    private void setupCartList() {
        cartListView.setItems(cartManager.getCartItems());
        cartListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Post item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label line = new Label(item.getTitle() + " - Php " + String.format("%.2f", item.getPrice()));
                Button remove = new Button("Remove");
                remove.setOnAction(evt -> cartManager.remove(item));

                VBox box = new VBox(6, line, remove);
                box.setPadding(new Insets(6));
                setGraphic(box);
                setText(null);
            }
        });

        updateCartTotal();
    }

    private void loadMarketplacePostsAsync() {
        Service<List<Post>> loadService = new Service<>() {
            @Override
            protected Task<List<Post>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<Post> call() {
                        return marketplaceService.getAvailablePosts();
                    }
                };
            }
        };

        loadingIndicator.visibleProperty().bind(loadService.runningProperty());
        loadService.setOnSucceeded(evt -> {
            loadedPosts.setAll(loadService.getValue());
            renderCards();
        });
        loadService.setOnFailed(evt -> {
            logger.error("Failed to load marketplace posts", loadService.getException());
            noResultsLabel.setText("Failed to load posts.");
            noResultsLabel.setVisible(true);
        });
        loadService.start();
    }

    private void renderCards() {
        postTilePane.getChildren().clear();

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String location = locationFilter.getValue() == null ? "All Locations" : locationFilter.getValue();
        String priceRange = priceRangeFilter.getValue() == null ? "All Prices" : priceRangeFilter.getValue();

        int shown = 0;
        for (Post post : loadedPosts) {
            if (!matchesSearch(post, search) || !matchesLocation(post, location) || !matchesPrice(post, priceRange)) {
                continue;
            }

            postTilePane.getChildren().add(buildCard(post));
            shown++;
        }

        noResultsLabel.setVisible(shown == 0);
    }

    private boolean matchesSearch(Post post, String search) {
        if (search.isBlank()) {
            return true;
        }

        return post.getTitle().toLowerCase().contains(search)
                || post.getDescription().toLowerCase().contains(search);
    }

    private boolean matchesLocation(Post post, String location) {
        if ("All Locations".equals(location)) {
            return true;
        }

        if (post instanceof Rental rental) {
            return location.equalsIgnoreCase(rental.getLocation());
        }
        if (post instanceof Product product) {
            return location.equalsIgnoreCase(product.getLocation());
        }
        return true;
    }

    private boolean matchesPrice(Post post, String priceRange) {
        double price = post.getPrice();
        return switch (priceRange) {
            case "0-5000" -> price >= 0 && price <= 5000;
            case "5000-10000" -> price > 5000 && price <= 10000;
            case "10000-15000" -> price > 10000 && price <= 15000;
            case "15000+" -> price > 15000;
            default -> true;
        };
    }

    private VBox buildCard(Post post) {
        ImageView thumb = new ImageView();
        thumb.setFitWidth(200);
        thumb.setFitHeight(110);
        thumb.setPreserveRatio(true);
        Image thumbImage = ImageUtil.loadCached(post.getMainImagePath(), 200, 110);
        if (thumbImage != null) {
            thumb.setImage(thumbImage);
        }

        Label title = new Label(post.getTitle());
        title.getStyleClass().add("market-card-title");

        Label type = new Label(post.getPostType().name());
        Label price = new Label("Php " + String.format("%.2f", post.getPrice()));

        Button detailsButton = new Button("View Details");
        detailsButton.setOnAction(evt -> loadDetailAsync(post.getPostId()));

        Button cartButton = new Button("Add to Cart");
        cartButton.setOnAction(evt -> {
            if (!cartManager.add(post)) {
                showInfo("Item already exists in cart.");
            }
        });

        Button reportButton = new Button("Report");
        reportButton.setOnAction(evt -> {
            TextInputDialog dialog = new TextInputDialog("Suspicious listing");
            dialog.setHeaderText("Report this item");
            dialog.setContentText("Reason:");
            String reason = dialog.showAndWait().orElse("Reported by user");

            int reporterId = SessionManager.getInstance().getCurrentUserId();
            if (reporterId <= 0) {
                showInfo("Please login to report posts.");
                return;
            }

            boolean ok = marketplaceService.reportPost(reporterId, post.getPostId(), reason);
            showInfo(ok ? "Post reported successfully." : "Unable to report post.");
        });

        VBox card = new VBox(8, thumb, title, type, price, detailsButton, cartButton, reportButton);
        card.getStyleClass().add("market-card");
        card.setPadding(new Insets(10));
        card.setPrefWidth(220);
        card.setAlignment(Pos.TOP_LEFT);
        return card;
    }

    private void loadDetailAsync(int postId) {
        Task<Post> detailTask = new Task<>() {
            @Override
            protected Post call() {
                return marketplaceService.getPostDetail(postId).orElse(null);
            }
        };

        detailTask.setOnSucceeded(evt -> renderDetail(detailTask.getValue()));
        detailTask.setOnFailed(evt -> showInfo("Failed to load item details."));

        Thread thread = new Thread(detailTask, "marketplace-detail-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderDetail(Post post) {
        if (post == null) {
            detailTitleLabel.setText("No item selected");
            detailTypeLabel.setText("-");
            detailPriceLabel.setText("-");
            detailStatusLabel.setText("-");
            detailDescriptionArea.clear();
            detailImageView.setImage(null);
            return;
        }

        detailTitleLabel.setText(post.getTitle());
        detailTypeLabel.setText(post.getPostType().name());
        detailPriceLabel.setText("Php " + String.format("%.2f", post.getPrice()));
        detailStatusLabel.setText(post.getStatus().name());
        detailImageView.setImage(ImageUtil.loadCached(post.getMainImagePath(), 320, 180));

        StringBuilder details = new StringBuilder(post.getDescription()).append("\n\n");

        if (post instanceof Rental rental) {
            details.append("Bedrooms: ").append(rental.getBedrooms()).append("\n")
                    .append("Bathrooms: ").append(rental.getBathrooms()).append("\n")
                    .append("Location: ").append(rental.getLocation()).append("\n");
        } else if (post instanceof Product product) {
            details.append("Category: ").append(product.getCategory()).append("\n")
                    .append("Condition: ").append(product.getCondition()).append("\n")
                    .append("Quantity: ").append(product.getQuantity()).append("\n")
                    .append("Location: ").append(product.getLocation()).append("\n");
        } else if (post instanceof AcademicResource resource) {
            details.append("Format: ").append(resource.getResourceType()).append("\n")
                    .append("File: ").append(resource.getFilePath()).append("\n")
                    .append("Downloads: ").append(resource.getDownloadCount()).append("\n");
        }

        detailDescriptionArea.setText(details.toString());
    }

    private void updateCartTotal() {
        cartTotalLabel.setText("Total: Php " + String.format("%.2f", cartManager.getTotalAmount()));
    }

    @FXML
    private void handleCheckout() {
        int buyerId = SessionManager.getInstance().getCurrentUserId();
        if (buyerId <= 0) {
            showInfo("Please login before checkout.");
            return;
        }
        if (cartManager.getCartItems().isEmpty()) {
            showInfo("Cart is empty.");
            return;
        }

        PaymentSelectionController.PaymentChoice paymentChoice = showPaymentSelectionDialog();
        if (paymentChoice == null) {
            return;
        }

        List<Post> snapshot = new ArrayList<>(cartManager.getCartItems());
        MarketplaceService.CheckoutResult result =
                marketplaceService.checkout(buyerId, snapshot, paymentChoice.databaseValue());

        if (result.success()) {
            cartManager.clear();
            showSuccessSummary(snapshot, paymentChoice.displayLabel());
            MainApplication.navigateBackFromListing();
        } else {
            showInfo(result.message());
        }
    }

    private PaymentSelectionController.PaymentChoice showPaymentSelectionDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("/fxml/payment-selection.fxml"));
            Parent root = loader.load();

            PaymentSelectionController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            if (backButton != null && backButton.getScene() != null) {
                dialogStage.initOwner(backButton.getScene().getWindow());
            }
            dialogStage.setTitle("Payment Selection");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();
            return controller.getConfirmedChoice();
        } catch (IOException e) {
            logger.error("Failed to open payment selection dialog", e);
            showInfo("Unable to open payment selection.");
            return null;
        }
    }

    private void showSuccessSummary(List<Post> purchasedPosts, String paymentMethodLabel) {
        if (purchasedPosts == null || purchasedPosts.isEmpty()) {
            showInfo("Success! Checkout completed.");
            return;
        }

        String itemName;
        double totalPrice = 0;
        for (Post post : purchasedPosts) {
            totalPrice += post.getPrice();
        }

        if (purchasedPosts.size() == 1) {
            itemName = purchasedPosts.get(0).getTitle();
        } else {
            itemName = purchasedPosts.size() + " items";
        }

        String message = "Success! You bought " + itemName + " for Php "
                + String.format("%.2f", totalPrice)
                + " via " + paymentMethodLabel + ".";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Checkout Completed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleClearCart() {
        cartManager.clear();
    }

    @FXML
    private void handleRefresh() {
        loadMarketplacePostsAsync();
    }

    @FXML
    private void handleBackNavigation() {
        MainApplication.navigateBackFromListing();
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
