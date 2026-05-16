package com.studentmarketplace.controller;

import com.studentmarketplace.MainApplication;
import com.studentmarketplace.model.PostSummary;
import com.studentmarketplace.model.UserSession;
import com.studentmarketplace.service.PostManagementService;
import com.studentmarketplace.util.ImageUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

/**
 * Seller dashboard for creating and managing own posts.
 */
public class ClientDashboardController {
    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> postTypeCombo;
    @FXML private TextField titleField;
    @FXML private TextField locationField;
    @FXML private TextField priceField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField imagePathField;
    @FXML private ImageView imagePreview;

    @FXML private VBox rentalFieldsBox;
    @FXML private TextField bedroomsField;
    @FXML private CheckBox furnishedCheck;

    @FXML private VBox saleFieldsBox;
    @FXML private ComboBox<String> conditionCombo;
    @FXML private ComboBox<String> categoryCombo;

    @FXML private VBox resourceFieldsBox;
    @FXML private ComboBox<String> resourceTypeCombo;
    @FXML private TextField downloadLinkField;

    @FXML private ListView<PostSummary> myPostsListView;
    @FXML private Label statusLabel;

    private final PostManagementService postService = new PostManagementService();
    private UserSession session;

    @FXML
    public void initialize() {
        postTypeCombo.setItems(FXCollections.observableArrayList("RENTAL", "SALE", "RESOURCE"));
        postTypeCombo.setValue("RENTAL");

        conditionCombo.setItems(FXCollections.observableArrayList("LIKE_NEW", "GOOD", "FAIR", "POOR"));
        conditionCombo.setValue("GOOD");

        categoryCombo.setItems(FXCollections.observableArrayList("ELECTRONICS", "TEXTBOOKS", "FURNITURE", "CLOTHING", "ACCESSORIES", "OTHER"));
        categoryCombo.setValue("OTHER");

        resourceTypeCombo.setItems(FXCollections.observableArrayList("SOFTWARE", "PDF", "RESEARCH_PAPER", "NOTES", "OTHER"));
        resourceTypeCombo.setValue("NOTES");

        postTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyPostTypeTemplate());
        applyPostTypeTemplate();

        myPostsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(PostSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label title = new Label(item.getTitle());
                title.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #f8fafc;");

                Label meta = new Label(String.format("%s | Php %.0f | %s", item.getLocation(), item.getPricePerMonth(), item.getStatus()));
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

                VBox card = new VBox(2, title, meta);
                card.setStyle("-fx-padding: 8 10 8 10; -fx-background-color: transparent;");
                setGraphic(card);
                setText(null);
            }
        });
    }

    public void setSession(UserSession session) {
        this.session = session;
        welcomeLabel.setText("Welcome, " + session.getFullName() + " (Seller)");
        refreshPosts();
    }

    private void applyPostTypeTemplate() {
        String type = postTypeCombo.getValue();

        boolean rental = "RENTAL".equalsIgnoreCase(type);
        boolean sale = "SALE".equalsIgnoreCase(type);
        boolean resource = "RESOURCE".equalsIgnoreCase(type);

        rentalFieldsBox.setVisible(rental);
        rentalFieldsBox.setManaged(rental);

        saleFieldsBox.setVisible(sale);
        saleFieldsBox.setManaged(sale);

        resourceFieldsBox.setVisible(resource);
        resourceFieldsBox.setManaged(resource);
    }

    @FXML
    public void handleCreatePost() {
        if (session == null) {
            statusLabel.setText("Session expired. Please login again.");
            return;
        }

        String title = safe(titleField.getText());
        String location = safe(locationField.getText());
        String description = safe(descriptionArea.getText());
        String priceRaw = safe(priceField.getText());
        String imagePath = safe(imagePathField.getText());

        if (title.isEmpty() || priceRaw.isEmpty()) {
            statusLabel.setText("Title and price are required.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceRaw.replace("₱", "").replace(",", ""));
        } catch (NumberFormatException e) {
            statusLabel.setText("Price must be a valid number.");
            return;
        }

        int bedrooms = 1;
        try {
            if (!safe(bedroomsField.getText()).isEmpty()) {
                bedrooms = Integer.parseInt(safe(bedroomsField.getText()));
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Bedrooms must be a valid number.");
            return;
        }

        String postType = postTypeCombo.getValue();
        if (!"RESOURCE".equals(postType) && location.isEmpty()) {
            statusLabel.setText("Location is required for rental and sale posts.");
            return;
        }

        PostManagementService.CreatePostRequest request = new PostManagementService.CreatePostRequest(
                session.getUserId(),
                postType,
                title,
                description,
                location,
                price,
                imagePath,
                bedrooms,
                furnishedCheck.isSelected(),
                conditionCombo.getValue(),
                categoryCombo.getValue(),
                resourceTypeCombo.getValue(),
                safe(downloadLinkField.getText()),
                "UniNexus"
        );

        Task<PostManagementService.OperationResult> task = new Task<>() {
            @Override
            protected PostManagementService.OperationResult call() {
                return postService.createPost(request);
            }
        };

        task.setOnSucceeded(evt -> {
            PostManagementService.OperationResult result = task.getValue();
            statusLabel.setText(result.message());
            if (result.success()) {
                clearForm();
                refreshPosts();
            }
        });
        task.setOnFailed(evt -> statusLabel.setText("Failed to create post."));

        Thread thread = new Thread(task, "seller-post-create-worker");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleChooseImage() {
        Window window = titleField != null && titleField.getScene() != null ? titleField.getScene().getWindow() : null;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File selected = fileChooser.showOpenDialog(window);
        if (selected != null) {
            try {
                String relativePath = ImageUtil.copyToLocalAssets(selected);
                imagePathField.setText(relativePath);
                imagePreview.setImage(ImageUtil.loadCached(relativePath, 200, 120));
            } catch (Exception e) {
                statusLabel.setText("Failed to save selected image.");
            }
        }
    }

    @FXML
    public void handleArchiveSelected() {
        PostSummary selected = myPostsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a post first.");
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return postService.updatePostStatus(selected.getPostId(), "ARCHIVED");
            }
        };

        task.setOnSucceeded(evt -> {
            if (task.getValue()) {
                statusLabel.setText("Post archived.");
                refreshPosts();
            } else {
                statusLabel.setText("Failed to archive post.");
            }
        });
        task.setOnFailed(evt -> statusLabel.setText("Failed to archive post."));

        Thread thread = new Thread(task, "seller-post-archive-worker");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleRefresh() {
        refreshPosts();
        statusLabel.setText("Posts refreshed.");
    }

    @FXML
    public void handleBrowseMarketplace() {
        MainApplication.showDormitoryListing();
    }

    @FXML
    public void handleOpenMyMarketplace() {
        if (session == null) {
            statusLabel.setText("Session expired. Please login again.");
            return;
        }
        MainApplication.showClientMarketplace(session);
    }

    @FXML
    public void handleOpenProfile() {
        MainApplication.showProfileView();
    }

    @FXML
    public void handleLogout() {
        MainApplication.showLoginView();
    }

    @FXML
    public void handleRequestRefund() {
        MainApplication.showRefundRequestView();
    }

    @FXML
    public void handleMyRefunds() {
        MainApplication.showRefundStatusView();
    }

    private void refreshPosts() {
        if (session == null) {
            return;
        }

        Task<List<PostSummary>> task = new Task<>() {
            @Override
            protected List<PostSummary> call() {
                return postService.getPostsBySeller(session.getUserId());
            }
        };

        task.setOnSucceeded(evt -> myPostsListView.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(evt -> statusLabel.setText("Failed to load posts."));

        Thread thread = new Thread(task, "seller-post-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void clearForm() {
        titleField.clear();
        locationField.clear();
        priceField.clear();
        descriptionArea.clear();
        imagePathField.clear();
        bedroomsField.setText("1");
        furnishedCheck.setSelected(false);
        downloadLinkField.clear();
        imagePreview.setImage(null);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
