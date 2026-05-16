package com.studentmarketplace.service;

import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.model.AcademicResource;
import com.studentmarketplace.model.Post;
import com.studentmarketplace.model.PostSummary;
import com.studentmarketplace.model.Product;
import com.studentmarketplace.model.Rental;
import com.studentmarketplace.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Buyer-side marketplace data access and checkout workflow.
 */
public class MarketplaceService {
    private static final Logger logger = LoggerFactory.getLogger(MarketplaceService.class);

    private final DatabaseManager dbManager;
    private final ReportService reportService;
    private final TransactionService transactionService;

    public MarketplaceService() {
        this.dbManager = DatabaseManager.getInstance();
        this.reportService = new ReportService();
        this.transactionService = new TransactionService();
    }

    public List<Post> getAvailablePosts() {
        String sql = "SELECT p.post_id, p.seller_id, p.post_type, p.title, p.description, p.image_path AS post_image_path, p.status, p.created_at, p.updated_at, " +
                "r.rental_id, r.rental_type, r.location AS rental_location, r.latitude, r.longitude, r.price_per_month, r.bedrooms, r.bathrooms, r.area_sqm, r.furnished, r.amenities, r.available_from, r.main_image_path AS rental_main_image, r.thumbnail_image_path AS rental_thumb, " +
                "pr.product_id, pr.category, pr.price AS product_price, pr.condition, pr.quantity, pr.location AS product_location, pr.main_image_path AS product_main_image, pr.thumbnail_image_path AS product_thumb, " +
                "ar.resource_id, ar.resource_type, ar.file_path, ar.file_size_mb, ar.price AS resource_price, ar.download_count, ar.subject_area, ar.course_code, ar.university " +
                "FROM posts p " +
                "LEFT JOIN rentals r ON p.post_id = r.post_id " +
                "LEFT JOIN products pr ON p.post_id = pr.post_id " +
                "LEFT JOIN academic_resources ar ON p.post_id = ar.post_id " +
                "WHERE p.status = 'ACTIVE' " +
                "ORDER BY p.created_at DESC";

        List<Post> results = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                mapRowToPost(rs).ifPresent(results::add);
            }
        } catch (SQLException e) {
            logger.error("Failed to load available posts", e);
        }
        return results;
    }

    public Optional<Post> getPostDetail(int postId) {
        String sql = "SELECT p.post_id, p.seller_id, p.post_type, p.title, p.description, p.image_path AS post_image_path, p.status, p.created_at, p.updated_at, " +
                "r.rental_id, r.rental_type, r.location AS rental_location, r.latitude, r.longitude, r.price_per_month, r.bedrooms, r.bathrooms, r.area_sqm, r.furnished, r.amenities, r.available_from, r.main_image_path AS rental_main_image, r.thumbnail_image_path AS rental_thumb, " +
                "pr.product_id, pr.category, pr.price AS product_price, pr.condition, pr.quantity, pr.location AS product_location, pr.main_image_path AS product_main_image, pr.thumbnail_image_path AS product_thumb, " +
                "ar.resource_id, ar.resource_type, ar.file_path, ar.file_size_mb, ar.price AS resource_price, ar.download_count, ar.subject_area, ar.course_code, ar.university " +
                "FROM posts p " +
                "LEFT JOIN rentals r ON p.post_id = r.post_id " +
                "LEFT JOIN products pr ON p.post_id = pr.post_id " +
                "LEFT JOIN academic_resources ar ON p.post_id = ar.post_id " +
                "WHERE p.post_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPost(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load post details: {}", postId, e);
        }
        return Optional.empty();
    }

    public CheckoutResult checkout(int buyerId, List<Post> selectedPosts) {
        return checkout(buyerId, selectedPosts, "CASH");
    }

    public CheckoutResult checkout(int buyerId, List<Post> selectedPosts, String paymentMethod) {
        return transactionService.processCheckout(buyerId, selectedPosts, paymentMethod, this::getPostDetail);
    }

    public boolean reportPost(int reporterId, int postId, String reason) {
        return reportService.reportPost(reporterId, postId, reason);
    }

    public boolean flagPost(int postId) {
        return dbManager.executeUpdate("UPDATE posts SET flagged = 1, updated_at = CURRENT_TIMESTAMP WHERE post_id = ?", postId) > 0;
    }

    public List<PostSummary> getFlaggedPosts() {
        List<PostSummary> rows = new ArrayList<>();
        String sql = "SELECT p.post_id, p.seller_id, u.username, p.title, p.description, p.status, p.created_at, " +
                "COALESCE(r.location, pr.location, ar.university, 'N/A') AS location, " +
                "COALESCE(r.price_per_month, pr.price, ar.price, 0) AS amount " +
                "FROM posts p " +
                "JOIN users u ON u.user_id = p.seller_id " +
                "LEFT JOIN rentals r ON r.post_id = p.post_id " +
                "LEFT JOIN products pr ON pr.post_id = p.post_id " +
                "LEFT JOIN academic_resources ar ON ar.post_id = p.post_id " +
                "WHERE COALESCE(p.flagged, 0) = 1 " +
                "ORDER BY p.updated_at DESC";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(new PostSummary(
                        rs.getInt("post_id"),
                        rs.getInt("seller_id"),
                        rs.getString("username"),
                        rs.getString("title"),
                        rs.getString("description") == null ? "" : rs.getString("description"),
                        rs.getString("location"),
                        rs.getDouble("amount"),
                        rs.getString("status"),
                        parseSqlDateTime(rs.getString("created_at"))
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to load flagged posts", e);
        }

        return rows;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, email, password_hash, full_name, student_id, university, phone, profile_image_path, bio, rating, total_ratings, account_status, created_at, updated_at FROM users ORDER BY created_at DESC";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("student_id"),
                        rs.getString("university"),
                        rs.getString("phone"),
                        rs.getString("profile_image_path"),
                        rs.getString("bio"),
                        rs.getDouble("rating"),
                        rs.getInt("total_ratings"),
                        User.AccountStatus.valueOf(rs.getString("account_status")),
                        parseSqlDateTime(rs.getString("created_at")),
                        parseSqlDateTime(rs.getString("updated_at"))
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to load users", e);
        }

        return users;
    }

    public boolean updateUserStatus(int userId, User.AccountStatus status) {
        return dbManager.executeUpdate(
                "UPDATE users SET account_status = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                status.toString(),
                userId
        ) > 0;
    }

    public boolean archivePost(int postId) {
        return dbManager.executeUpdate(
                "UPDATE posts SET status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP WHERE post_id = ?",
                postId
        ) > 0;
    }

    public boolean clearFlag(int postId) {
        return dbManager.executeUpdate(
                "UPDATE posts SET flagged = 0, updated_at = CURRENT_TIMESTAMP WHERE post_id = ?",
                postId
        ) > 0;
    }

    private Optional<Post> mapRowToPost(ResultSet rs) throws SQLException {
        int postId = rs.getInt("post_id");
        int sellerId = rs.getInt("seller_id");
        String title = rs.getString("title");
        String description = rs.getString("description") == null ? "" : rs.getString("description");
        Post.PostStatus status = Post.PostStatus.valueOf(rs.getString("status"));
        LocalDateTime createdAt = parseSqlDateTime(rs.getString("created_at"));
        LocalDateTime updatedAt = parseSqlDateTime(rs.getString("updated_at"));
        String postType = rs.getString("post_type");

        if ("RENTAL".equalsIgnoreCase(postType)) {
            String amenitiesRaw = rs.getString("amenities");
            List<String> amenities = new ArrayList<>();
            if (amenitiesRaw != null && !amenitiesRaw.isBlank()) {
                for (String raw : amenitiesRaw.split(",")) {
                    amenities.add(raw.trim());
                }
            }

            LocalDate availableFrom = rs.getDate("available_from") != null
                    ? rs.getDate("available_from").toLocalDate()
                    : null;

                Rental rental = new Rental(
                    postId,
                    sellerId,
                    title,
                    description,
                    status,
                    createdAt,
                    updatedAt,
                    rs.getInt("rental_id"),
                    Rental.RentalType.valueOf(rs.getString("rental_type")),
                    rs.getString("rental_location"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getDouble("price_per_month"),
                    rs.getInt("bedrooms"),
                    rs.getInt("bathrooms"),
                    rs.getDouble("area_sqm"),
                    rs.getBoolean("furnished"),
                    amenities,
                    availableFrom,
                    rs.getString("rental_main_image"),
                    rs.getString("rental_thumb")
            );

            if (rental.getMainImagePath() == null || rental.getMainImagePath().isBlank()) {
                rental.setMainImagePath(rs.getString("post_image_path"));
                rental.setThumbnailImagePath(rs.getString("post_image_path"));
            }
            return Optional.of(rental);
        }

        if ("RESOURCE".equalsIgnoreCase(postType)) {
            AcademicResource resource = new AcademicResource(
                    postId,
                    sellerId,
                    title,
                    description,
                    status,
                    createdAt,
                    updatedAt,
                    rs.getInt("resource_id"),
                    AcademicResource.ResourceType.valueOf(rs.getString("resource_type")),
                    rs.getString("file_path"),
                    rs.getDouble("file_size_mb"),
                    rs.getDouble("resource_price"),
                    rs.getInt("download_count"),
                    rs.getString("subject_area"),
                    rs.getString("course_code"),
                    rs.getString("university")
            );
            resource.setPreviewImagePath(rs.getString("post_image_path"));
            return Optional.of(resource);
        }

        Product product = new Product(
                postId,
                sellerId,
                title,
                description,
                status,
                createdAt,
                updatedAt,
                rs.getInt("product_id"),
                Product.ProductCategory.valueOf(rs.getString("category")),
                rs.getDouble("product_price"),
                Product.ProductCondition.valueOf(rs.getString("condition")),
                rs.getInt("quantity"),
                rs.getString("product_location"),
                rs.getString("product_main_image"),
                rs.getString("product_thumb")
        );

        if (product.getMainImagePath() == null || product.getMainImagePath().isBlank()) {
            product.setMainImagePath(rs.getString("post_image_path"));
            product.setThumbnailImagePath(rs.getString("post_image_path"));
        }
        return Optional.of(product);
    }

    private LocalDateTime parseSqlDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(value.replace(" ", "T"));
        } catch (Exception ignored) {
            return LocalDateTime.now();
        }
    }

    public record CheckoutResult(boolean success, String message) {
        public static CheckoutResult success(String message) {
            return new CheckoutResult(true, message);
        }

        public static CheckoutResult failure(String message) {
            return new CheckoutResult(false, message);
        }
    }
}
