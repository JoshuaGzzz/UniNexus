package com.studentmarketplace.controller;

import com.studentmarketplace.MainApplication;
import com.studentmarketplace.model.PostSummary;
import com.studentmarketplace.model.UserSession;
import com.studentmarketplace.service.PostManagementService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Dedicated client marketplace page with post details and edit/delete actions.
 */
public class ClientMarketplaceController {
    @FXML private Label welcomeLabel;
    @FXML private ListView<PostSummary> postsListView;

    @FXML private Label postIdLabel;
    @FXML private Label statusLabel;
    @FXML private Label createdAtLabel;

    @FXML private TextField titleField;
    @FXML private TextField locationField;
    @FXML private TextField priceField;
    @FXML private TextArea descriptionArea;

    @FXML private Label messageLabel;

    private final PostManagementService postService = new PostManagementService();
    private UserSession session;
    private PostSummary selectedPost;

    @FXML
    public void initialize() {
        String readableInputStyle = "-fx-text-fill: #111111; -fx-control-inner-background: #ffffff; -fx-background-color: #ffffff; -fx-background-insets: 0; -fx-background-radius: 10; -fx-prompt-text-fill: #777777;";
        titleField.setStyle(readableInputStyle);
        locationField.setStyle(readableInputStyle);
        priceField.setStyle(readableInputStyle);
        descriptionArea.setStyle(readableInputStyle + " -fx-highlight-fill: #00a86b; -fx-highlight-text-fill: #ffffff; -fx-wrap-text: true;");

        postsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(PostSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                Label title = new Label(item.getTitle());
                title.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #f8fafc;");

                Label meta = new Label(String.format("%s | Php %.0f/mo | %s", item.getLocation(), item.getPricePerMonth(), item.getStatus()));
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

                VBox card = new VBox(2, title, meta);
                card.setStyle("-fx-padding: 8 10 8 10; -fx-background-color: transparent;");
                setGraphic(card);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            }
        });

        postsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedPost = newVal;
            renderDetails();
        });
    }

    public void setSession(UserSession session) {
        this.session = session;
        welcomeLabel.setText("My Marketplace - " + session.getFullName());
        refreshList();
    }

    @FXML
    public void handleRefresh() {
        refreshList();
        messageLabel.setText("Refreshed.");
    }

    @FXML
    public void handleSaveChanges() {
        if (session == null || selectedPost == null) {
            messageLabel.setText("Select a post first.");
            return;
        }

        String title = safe(titleField.getText());
        String location = safe(locationField.getText());
        String description = safe(descriptionArea.getText());

        double price;
        try {
            price = Double.parseDouble(safe(priceField.getText()));
        } catch (NumberFormatException e) {
            messageLabel.setText("Price must be a valid number.");
            return;
        }

        boolean ok = postService.updatePostForSeller(session.getUserId(), selectedPost.getPostId(), title, description, location, price);
        if (!ok) {
            messageLabel.setText("Failed to save changes.");
            return;
        }

        messageLabel.setText("Post updated.");
        refreshList();
        reselectPost(selectedPost.getPostId());
    }

    @FXML
    public void handleDeletePost() {
        if (session == null || selectedPost == null) {
            messageLabel.setText("Select a post first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Post");
        confirm.setHeaderText("Delete selected post?");
        confirm.setContentText("This will mark the post as DELETED.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean ok = postService.deletePostForSeller(session.getUserId(), selectedPost.getPostId());
        if (ok) {
            messageLabel.setText("Post deleted.");
            selectedPost = null;
            refreshList();
            clearDetails();
        } else {
            messageLabel.setText("Failed to delete post.");
        }
    }

    @FXML
    public void handleBackToDashboard() {
        if (session != null) {
            MainApplication.showClientDashboard(session);
        } else {
            MainApplication.showLoginView();
        }
    }

    @FXML
    public void handleBrowsePublicListings() {
        MainApplication.showDormitoryListing();
    }

    private void refreshList() {
        if (session == null) {
            return;
        }

        Task<List<PostSummary>> task = new Task<>() {
            @Override
            protected List<PostSummary> call() {
                return postService.getPostsBySeller(session.getUserId());
            }
        };

        task.setOnSucceeded(evt -> postsListView.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(evt -> messageLabel.setText("Failed to load your posts."));

        Thread thread = new Thread(task, "my-marketplace-list-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderDetails() {
        if (session == null || selectedPost == null) {
            clearDetails();
            return;
        }

        Task<Optional<PostSummary>> task = new Task<>() {
            @Override
            protected Optional<PostSummary> call() {
                return postService.getPostByIdForSeller(session.getUserId(), selectedPost.getPostId());
            }
        };

        task.setOnSucceeded(evt -> {
            Optional<PostSummary> detailOpt = task.getValue();
            if (detailOpt.isEmpty()) {
                clearDetails();
                return;
            }

            PostSummary detail = detailOpt.get();
            postIdLabel.setText(String.valueOf(detail.getPostId()));
            statusLabel.setText(detail.getStatus());
            createdAtLabel.setText(detail.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            titleField.setText(detail.getTitle());
            locationField.setText(detail.getLocation());
            priceField.setText(String.valueOf((int) detail.getPricePerMonth()));
            descriptionArea.setText(detail.getDescription());
        });

        task.setOnFailed(evt -> messageLabel.setText("Failed to load post details."));

        Thread thread = new Thread(task, "my-marketplace-detail-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void clearDetails() {
        postIdLabel.setText("-");
        statusLabel.setText("-");
        createdAtLabel.setText("-");

        titleField.clear();
        locationField.clear();
        priceField.clear();
        descriptionArea.clear();
    }

    private void reselectPost(int postId) {
        for (PostSummary row : postsListView.getItems()) {
            if (row.getPostId() == postId) {
                postsListView.getSelectionModel().select(row);
                selectedPost = row;
                renderDetails();
                return;
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
