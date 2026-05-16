package com.studentmarketplace.controller;

import com.studentmarketplace.MainApplication;
import com.studentmarketplace.model.AcademicResource;
import com.studentmarketplace.model.Post;
import com.studentmarketplace.model.Product;
import com.studentmarketplace.model.Rental;
import com.studentmarketplace.model.User;
import com.studentmarketplace.model.UserSession;
import com.studentmarketplace.service.AdminService;
import com.studentmarketplace.service.AdminService.AdminSummary;
import com.studentmarketplace.service.ReportService;
import com.studentmarketplace.service.UserService;
import com.studentmarketplace.util.ImageUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.List;

/**
 * Admin dashboard for user management and global post oversight.
 */
public class AdminDashboardController {
    private static final int FLAG_SUSPENSION_THRESHOLD = 3;

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;

    @FXML
    private Label activePostsLabel;
    @FXML
    private Label productPostsLabel;
    @FXML
    private Label rentalPostsLabel;
    @FXML
    private Label resourcePostsLabel;
    @FXML
    private Label flaggedItemsLabel;

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> emailColumn;
    @FXML
    private TableColumn<User, String> fullNameColumn;
    @FXML
    private TableColumn<User, String> universityColumn;
    @FXML
    private TableColumn<User, String> statusColumn;

    @FXML
    private TableView<Post> masterPostsTable;
    @FXML
    private TableColumn<Post, String> postIdColumn;
    @FXML
    private TableColumn<Post, String> postTitleColumn;
    @FXML
    private TableColumn<Post, String> postTypeColumn;
    @FXML
    private TableColumn<Post, String> postOwnerColumn;
    @FXML
    private TableColumn<Post, String> postPriceColumn;
    @FXML
    private TableColumn<Post, String> postStatusColumn;

    @FXML
    private TableView<com.studentmarketplace.model.PostSummary> flaggedPostsTable;
    @FXML
    private TableColumn<com.studentmarketplace.model.PostSummary, String> flaggedTitleColumn;
    @FXML
    private TableColumn<com.studentmarketplace.model.PostSummary, String> flaggedSellerColumn;
    @FXML
    private TableColumn<com.studentmarketplace.model.PostSummary, String> flaggedReasonColumn;
    @FXML
    private TableColumn<com.studentmarketplace.model.PostSummary, String> flaggedStatusColumn;

    @FXML
    private Label postDetailTitleLabel;
    @FXML
    private Label postDetailMetaLabel;
    @FXML
    private Label postDetailExtraLabel;
    @FXML
    private TextArea postDetailDescriptionArea;
    @FXML
    private ImageView postDetailImageView;

    private final AdminService adminService = new AdminService();
    private final UserService userService = new UserService();
    private final ReportService reportService = new ReportService();

    private List<User> allUsers = new java.util.ArrayList<>();
    private List<com.studentmarketplace.model.PostSummary> allFlaggedPosts = new java.util.ArrayList<>();

    private final ScheduledService<AdminData> refreshService = new ScheduledService<>() {
        @Override
        protected Task<AdminData> createTask() {
            String query = searchField.getText();
            return new Task<>() {
                @Override
                protected AdminData call() {
                    List<User> users = userService.getAllUsers();
                    List<Post> masterPosts = adminService.getMasterPosts(query);
                    List<com.studentmarketplace.model.PostSummary> flaggedPosts = reportService.getFlaggedPosts();
                    AdminSummary summary = adminService.getSummary();
                    return new AdminData(users, masterPosts, flaggedPosts, summary);
                }
            };
        }
    };

    @FXML
    public void initialize() {
        // ── User Management columns ──
        usernameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        emailColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        fullNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        universityColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUniversity()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAccountStatus().name()));

        // Color-coded status cells for the User table
        statusColumn.setCellFactory(column -> new TableCell<>() {
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
                    case "ACTIVE" -> setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    case "SUSPENDED" -> setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    case "BANNED" -> setStyle("-fx-text-fill: #7c2d12; -fx-font-weight: bold;");
                    default -> setStyle("-fx-font-weight: bold;");
                }
            }
        });

        // ── Master Posts columns ──
        postIdColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getPostId())));
        postTitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        postTypeColumn.setCellValueFactory(cell -> new SimpleStringProperty(displayType(cell.getValue())));
        postOwnerColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getOwnerUsername() == null ? "-" : cell.getValue().getOwnerUsername()));
        postPriceColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(String.format("Php %.2f", cell.getValue().getPrice())));
        postStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(displayStatus(cell.getValue())));

        // Color-coded status cells for the Master Posts table
        postStatusColumn.setCellFactory(column -> new TableCell<>() {
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
                    case "ACTIVE" -> setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    case "FLAGGED" -> setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    case "ARCHIVED" -> setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
                    case "SOLD", "RENTED" -> setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                    case "DELETED" -> setStyle("-fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-strikethrough: true;");
                    default -> setStyle("-fx-font-weight: bold;");
                }
            }
        });

        // ── Flagged Posts columns ──
        flaggedTitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        flaggedSellerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSellerUsername()));
        flaggedReasonColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReportReason()));
        flaggedStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));

        // Color-coded status cells for the Flagged Posts table
        flaggedStatusColumn.setCellFactory(column -> new TableCell<>() {
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
                    case "ACTIVE" -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    case "ARCHIVED" -> setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
                    case "DELETED" -> setStyle("-fx-text-fill: #991b1b; -fx-font-weight: bold;");
                    default -> setStyle("-fx-font-weight: bold;");
                }
            }
        });

        masterPostsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> renderSelectedPost(newVal));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            applySearch(newVal);
            refreshNow();
        });

        refreshService.setPeriod(Duration.seconds(30));
        refreshService.setOnSucceeded(evt -> applySnapshot(refreshService.getValue()));
        refreshService.setOnFailed(evt -> statusLabel.setText("Failed to refresh admin data."));
    }

    public void setSession(UserSession session) {
        welcomeLabel.setText("Welcome, " + session.getFullName() + " (Admin)");
        refreshNow();
    }

    @FXML
    public void handleRefresh() {
        refreshNow();
    }

    @FXML
    public void handleSuspendSelectedUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a user first.");
            return;
        }

        if (selected.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            statusLabel.setText("User \"" + selected.getUsername() + "\" is already suspended.");
            return;
        }

        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suspend User");
        confirm.setHeaderText("Suspend user \"" + selected.getUsername() + "\"?");

        int flaggedCount = adminService.getOpenFlagCountForUser(selected.getUserId());
        String detail = "This user currently has " + flaggedCount + " open flagged post(s).\n"
                + "Suspending will prevent them from logging in.";
        confirm.setContentText(detail);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean ok = userService.updateUserStatus(selected.getUserId(), User.AccountStatus.SUSPENDED);
        statusLabel.setText(ok ? "User \"" + selected.getUsername() + "\" suspended." : "Failed to suspend user.");
        if (ok) {
            refreshNow();
        }
    }

    @FXML
    public void handleActivateSelectedUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a user first.");
            return;
        }

        if (selected.getAccountStatus() == User.AccountStatus.ACTIVE) {
            statusLabel.setText("User \"" + selected.getUsername() + "\" is already active.");
            return;
        }

        boolean ok = userService.updateUserStatus(selected.getUserId(), User.AccountStatus.ACTIVE);
        statusLabel.setText(ok ? "User \"" + selected.getUsername() + "\" activated." : "Failed to activate user.");
        if (ok) {
            refreshNow();
        }
    }

    @FXML
    public void handleArchiveSelectedPost() {
        Post selected = masterPostsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a post first.");
            return;
        }

        boolean ok = adminService.archivePost(selected.getPostId());
        statusLabel.setText(ok ? "Post archived." : "Failed to archive post.");
        if (ok) {
            refreshNow();
        }
    }

    @FXML
    public void handleDeleteSelectedPost() {
        Post selected = masterPostsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a post first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete selected post?");
        confirm.setContentText("This will mark the post as deleted and resolve related reports.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean ok = adminService.deletePost(selected.getPostId());
        statusLabel.setText(ok ? "Post deleted." : "Failed to delete post.");
        if (ok) {
            refreshNow();
        }
    }

    @FXML
    public void handleArchiveFlaggedPost() {
        com.studentmarketplace.model.PostSummary selected = flaggedPostsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a flagged post first.");
            return;
        }

        boolean ok = adminService.archivePost(selected.getPostId());
        statusLabel.setText(ok ? "Flagged post archived." : "Failed to archive flagged post.");
        if (ok) {
            refreshNow();
        }
    }

    @FXML
    public void handleClearFlag() {
        com.studentmarketplace.model.PostSummary selected = flaggedPostsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a flagged post first.");
            return;
        }

        boolean ok = reportService.clearReportsAndFlag(selected.getPostId());
        statusLabel.setText(ok ? "Flag cleared." : "Failed to clear flag.");
        if (ok) {
            refreshNow();
        }
    }

    @FXML
    public void handleOpenRefundAdmin() {
        MainApplication.showRefundAdminView();
    }

    @FXML
    public void handleLogout() {
        refreshService.cancel();
        MainApplication.showLoginView();
    }

    private void refreshNow() {
        if (refreshService.getState() == javafx.concurrent.Worker.State.READY) {
            refreshService.start();
        } else {
            refreshService.restart();
        }
    }

    private void applySearch(String query) {
        String q = query == null ? "" : query.toLowerCase().trim();
        if (q.isEmpty()) {
            usersTable.setItems(FXCollections.observableArrayList(allUsers));
            flaggedPostsTable.setItems(FXCollections.observableArrayList(allFlaggedPosts));
        } else {
            List<User> filteredUsers = allUsers.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(q)
                            || u.getEmail().toLowerCase().contains(q)
                            || u.getFullName().toLowerCase().contains(q)
                            || u.getUniversity().toLowerCase().contains(q))
                    .toList();
            List<com.studentmarketplace.model.PostSummary> filteredFlagged = allFlaggedPosts.stream()
                    .filter(p -> p.getTitle().toLowerCase().contains(q)
                            || p.getSellerUsername().toLowerCase().contains(q)
                            || (p.getReportReason() != null && p.getReportReason().toLowerCase().contains(q)))
                    .toList();
            usersTable.setItems(FXCollections.observableArrayList(filteredUsers));
            flaggedPostsTable.setItems(FXCollections.observableArrayList(filteredFlagged));
        }
    }

    private void applySnapshot(AdminData data) {
        allUsers = data.users();
        allFlaggedPosts = data.flaggedPosts();
        applySearch(searchField.getText());
        masterPostsTable.setItems(FXCollections.observableArrayList(data.masterPosts()));

        activePostsLabel.setText("Active: " + data.summary().activePosts());
        productPostsLabel.setText("Products: " + data.summary().productPosts());
        rentalPostsLabel.setText("Rentals: " + data.summary().rentalPosts());
        resourcePostsLabel.setText("Resources: " + data.summary().resourcePosts());
        flaggedItemsLabel.setText("Flagged: " + data.summary().flaggedItems());

        if (masterPostsTable.getSelectionModel().getSelectedItem() != null) {
            renderSelectedPost(masterPostsTable.getSelectionModel().getSelectedItem());
        } else if (!data.masterPosts().isEmpty()) {
            masterPostsTable.getSelectionModel().selectFirst();
        }

        statusLabel.setText("Admin data refreshed.");
    }

    private void renderSelectedPost(Post selected) {
        if (selected == null) {
            postDetailTitleLabel.setText("No post selected");
            postDetailMetaLabel.setText("-");
            postDetailExtraLabel.setText("-");
            postDetailDescriptionArea.clear();
            postDetailImageView.setImage(null);
            return;
        }

        postDetailTitleLabel.setText(selected.getTitle());
        postDetailMetaLabel
                .setText(displayType(selected) + " | " + selected.getOwnerUsername() + " | " + displayStatus(selected));
        postDetailImageView.setImage(ImageUtil.loadCached(selected.getMainImagePath(), 320, 180));

        String extra;
        if (selected instanceof Rental rental) {
            extra = "Location: " + rental.getLocation() + "\nBedrooms: " + rental.getBedrooms() + "\nBathrooms: "
                    + rental.getBathrooms();
        } else if (selected instanceof Product product) {
            extra = "Category: " + product.getCategory() + "\nCondition: " + product.getCondition() + "\nQuantity: "
                    + product.getQuantity() + "\nLocation: " + product.getLocation();
        } else if (selected instanceof AcademicResource resource) {
            extra = "Resource Type: " + resource.getResourceType() + "\nDownload Link: " + resource.getFilePath()
                    + "\nDownloads: " + resource.getDownloadCount();
        } else {
            extra = "No additional details.";
        }

        postDetailExtraLabel.setText(extra);
        postDetailDescriptionArea.setText(selected.getDescription() == null ? "" : selected.getDescription());
    }

    private String displayType(Post post) {
        return switch (post.getPostType()) {
            case SALE -> "Product";
            case RENTAL -> "Rental";
            case RESOURCE -> "Resource";
        };
    }

    private String displayStatus(Post post) {
        if (post.getStatus() == Post.PostStatus.FLAGGED) {
            return "FLAGGED";
        }
        return post.getStatus().name();
    }

    private record AdminData(List<User> users,
            List<Post> masterPosts,
            List<com.studentmarketplace.model.PostSummary> flaggedPosts,
            AdminSummary summary) {
    }
}