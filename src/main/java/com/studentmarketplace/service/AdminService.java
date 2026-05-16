package com.studentmarketplace.service;

import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.model.AcademicResource;
import com.studentmarketplace.model.Post;
import com.studentmarketplace.model.Product;
import com.studentmarketplace.model.Rental;
import com.studentmarketplace.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Admin-only service for master post management, counts, moderation and suspension rules.
 */
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private static final int FLAG_SUSPENSION_THRESHOLD = 3;

    private final DatabaseManager dbManager;

    public AdminService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public List<Post> getMasterPosts(String searchText) {
        String cleanSearch = searchText == null ? "" : searchText.trim().toLowerCase();
        String sql = "SELECT p.post_id, p.seller_id, u.username, p.post_type, p.title, p.description, p.image_path, p.status, p.created_at, p.updated_at, " +
                "r.rental_id, r.rental_type, r.location AS rental_location, r.latitude, r.longitude, r.price_per_month, r.bedrooms, r.bathrooms, r.area_sqm, r.furnished, r.amenities, r.available_from, r.main_image_path AS rental_main_image, r.thumbnail_image_path AS rental_thumb, " +
                "pr.product_id, pr.category, pr.price AS product_price, pr.condition, pr.quantity, pr.location AS product_location, pr.main_image_path AS product_main_image, pr.thumbnail_image_path AS product_thumb, " +
                "ar.resource_id, ar.resource_type, ar.file_path, ar.file_size_mb, ar.price AS resource_price, ar.download_count, ar.subject_area, ar.course_code, ar.university " +
                "FROM posts p " +
                "JOIN users u ON u.user_id = p.seller_id " +
                "LEFT JOIN rentals r ON p.post_id = r.post_id " +
                "LEFT JOIN products pr ON p.post_id = pr.post_id " +
                "LEFT JOIN academic_resources ar ON p.post_id = ar.post_id " +
                "WHERE (? = '' OR lower(u.username) LIKE ? OR lower(p.title) LIKE ?) " +
                "ORDER BY p.updated_at DESC";

        List<Post> posts = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, cleanSearch);
            stmt.setString(2, "%" + cleanSearch + "%");
            stmt.setString(3, "%" + cleanSearch + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mapRowToPost(rs).ifPresent(posts::add);
                }
            }
        } catch (Exception e) {
            logger.error("Failed loading master posts", e);
        }
        return posts;
    }

    public AdminSummary getSummary() {
        long activePosts = queryLong("SELECT COUNT(*) FROM posts WHERE status = 'ACTIVE'");
        long productPosts = queryLong("SELECT COUNT(*) FROM posts WHERE post_type = 'SALE'");
        long rentalPosts = queryLong("SELECT COUNT(*) FROM posts WHERE post_type = 'RENTAL'");
        long resourcePosts = queryLong("SELECT COUNT(*) FROM posts WHERE post_type = 'RESOURCE'");
        long flaggedItems = queryLong("SELECT COUNT(*) FROM reports WHERE status = 'OPEN'");
        return new AdminSummary(activePosts, productPosts, rentalPosts, resourcePosts, flaggedItems);
    }

    public boolean archivePost(int postId) {
        try {
            dbManager.beginTransaction();
            int updated = dbManager.executeUpdate("UPDATE posts SET status = 'ARCHIVED', flagged = 0, updated_at = CURRENT_TIMESTAMP WHERE post_id = ?", postId);
            dbManager.executeUpdate("UPDATE reports SET status = 'RESOLVED', resolved_at = CURRENT_TIMESTAMP WHERE post_id = ? AND status = 'OPEN'", postId);
            if (updated <= 0) {
                dbManager.rollback();
                return false;
            }
            dbManager.commit();
            return true;
        } catch (Exception e) {
            logger.error("Failed archiving post {}", postId, e);
            try {
                dbManager.rollback();
            } catch (Exception ignored) {
                // no-op
            }
            return false;
        }
    }

    public boolean deletePost(int postId) {
        try {
            dbManager.beginTransaction();
            int updated = dbManager.executeUpdate("UPDATE posts SET status = 'DELETED', flagged = 0, updated_at = CURRENT_TIMESTAMP WHERE post_id = ?", postId);
            dbManager.executeUpdate("UPDATE reports SET status = 'RESOLVED', resolved_at = CURRENT_TIMESTAMP WHERE post_id = ? AND status = 'OPEN'", postId);
            if (updated <= 0) {
                dbManager.rollback();
                return false;
            }
            dbManager.commit();
            return true;
        } catch (Exception e) {
            logger.error("Failed deleting post {}", postId, e);
            try {
                dbManager.rollback();
            } catch (Exception ignored) {
                // no-op
            }
            return false;
        }
    }

    public int getOpenFlagCountForUser(int userId) {
        return (int) queryLong(
                "SELECT COUNT(*) FROM reports r JOIN posts p ON p.post_id = r.post_id WHERE p.seller_id = ? AND r.status = 'OPEN'",
                userId
        );
    }

    public boolean suspendUserIfFlagged(int userId) {
        if (getOpenFlagCountForUser(userId) < FLAG_SUSPENSION_THRESHOLD) {
            return false;
        }
        return dbManager.executeUpdate("UPDATE users SET account_status = 'SUSPENDED', updated_at = CURRENT_TIMESTAMP WHERE user_id = ?", userId) > 0;
    }

    public List<UserFlagSummary> getUserFlagSummaries() {
        List<UserFlagSummary> rows = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.full_name, u.account_status, COUNT(r.report_id) AS open_flags " +
                "FROM users u " +
                "LEFT JOIN posts p ON p.seller_id = u.user_id " +
                "LEFT JOIN reports r ON r.post_id = p.post_id AND r.status = 'OPEN' " +
                "GROUP BY u.user_id, u.username, u.full_name, u.account_status " +
                "ORDER BY open_flags DESC, u.username ASC";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(new UserFlagSummary(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("account_status"),
                        rs.getInt("open_flags")
                ));
            }
        } catch (Exception e) {
            logger.error("Failed loading user flag summaries", e);
        }
        return rows;
    }

    private Optional<Post> mapRowToPost(ResultSet rs) throws Exception {
        int postId = rs.getInt("post_id");
        int sellerId = rs.getInt("seller_id");
        String title = rs.getString("title");
        String description = rs.getString("description") == null ? "" : rs.getString("description");
        String ownerUsername = rs.getString("username");
        Post.PostStatus status = deriveStatus(rs.getString("status"), rs.getInt("post_id"));
        LocalDateTime createdAt = parseSqlDateTime(rs.getString("created_at"));
        LocalDateTime updatedAt = parseSqlDateTime(rs.getString("updated_at"));
        String postType = rs.getString("post_type");

        if ("RENTAL".equalsIgnoreCase(postType)) {
            List<String> amenities = new ArrayList<>();
            String amenitiesRaw = rs.getString("amenities");
            if (amenitiesRaw != null && !amenitiesRaw.isBlank()) {
                for (String raw : amenitiesRaw.split(",")) {
                    amenities.add(raw.trim());
                }
            }

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
                    rs.getDate("available_from") != null ? rs.getDate("available_from").toLocalDate() : null,
                    rs.getString("rental_main_image"),
                    rs.getString("rental_thumb")
            );
            rental.setOwnerUsername(ownerUsername);
            if (rental.getMainImagePath() == null || rental.getMainImagePath().isBlank()) {
                rental.setMainImagePath(rs.getString("image_path"));
                rental.setThumbnailImagePath(rs.getString("image_path"));
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
            resource.setOwnerUsername(ownerUsername);
            resource.setPreviewImagePath(rs.getString("image_path"));
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
        product.setOwnerUsername(ownerUsername);
        if (product.getMainImagePath() == null || product.getMainImagePath().isBlank()) {
            product.setMainImagePath(rs.getString("image_path"));
            product.setThumbnailImagePath(rs.getString("image_path"));
        }
        return Optional.of(product);
    }

    private Post.PostStatus deriveStatus(String dbStatus, int postId) {
        if (isFlagged(postId)) {
            return Post.PostStatus.FLAGGED;
        }
        try {
            return Post.PostStatus.valueOf(dbStatus);
        } catch (Exception ignored) {
            return Post.PostStatus.ACTIVE;
        }
    }

    private boolean isFlagged(int postId) {
        return queryLong("SELECT COUNT(*) FROM reports WHERE post_id = ? AND status = 'OPEN'", postId) > 0;
    }

    private long queryLong(String sql, Object... params) {
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            logger.error("Failed running count query", e);
        }
        return 0;
    }

    private long queryLong(String sql) {
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.error("Failed running count query", e);
        }
        return 0;
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

    public record AdminSummary(long activePosts,
                               long productPosts,
                               long rentalPosts,
                               long resourcePosts,
                               long flaggedItems) {
    }

    public record UserFlagSummary(int userId,
                                  String username,
                                  String fullName,
                                  String accountStatus,
                                  int openFlags) {
    }
}
