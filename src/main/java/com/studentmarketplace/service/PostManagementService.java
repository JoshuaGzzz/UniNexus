package com.studentmarketplace.service;

import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.model.PostSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for seller-side CRUD and post creation by category.
 */
public class PostManagementService {
    private static final Logger logger = LoggerFactory.getLogger(PostManagementService.class);

    private final DatabaseManager dbManager;

    public PostManagementService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public OperationResult createPost(CreatePostRequest request) {
        if (request == null || request.sellerId() <= 0) {
            return OperationResult.failure("Invalid session. Please login again.");
        }

        if (!isUserExisting(request.sellerId())) {
            return OperationResult.failure("Your account was not found. Please login again.");
        }

        if (safe(request.title()).isEmpty() || safe(request.description()).isEmpty()) {
            return OperationResult.failure("Title and description are required.");
        }

        if (request.price() <= 0) {
            return OperationResult.failure("Price must be greater than zero.");
        }

        try {
            dbManager.beginTransaction();

            Optional<Long> insertedPostId = dbManager.executeInsert(
                    "INSERT INTO posts (seller_id, post_type, title, description, image_path, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')",
                    request.sellerId(),
                    request.postType(),
                    safe(request.title()),
                    safe(request.description()),
                    safe(request.imagePath())
            );

            if (insertedPostId.isEmpty()) {
                dbManager.rollback();
                return OperationResult.failure("Failed to create post.");
            }

            int postId = insertedPostId.get().intValue();
            String type = request.postType().toUpperCase();

            if ("RENTAL".equals(type)) {
                int rows = dbManager.executeUpdate(
                        "INSERT INTO rentals (post_id, rental_type, location, price_per_month, bedrooms, bathrooms, furnished, amenities, main_image_path, thumbnail_image_path) VALUES (?, 'DORMITORY', ?, ?, ?, 1, ?, ?, ?, ?)",
                        postId,
                        safe(request.location()),
                        request.price(),
                        request.bedrooms(),
                        request.furnished() ? 1 : 0,
                        "wifi,study-area",
                        safe(request.imagePath()),
                        safe(request.imagePath())
                );
                if (rows <= 0) {
                    dbManager.rollback();
                    return OperationResult.failure("Failed to create rental post.");
                }
            } else if ("SALE".equals(type)) {
                int rows = dbManager.executeUpdate(
                        "INSERT INTO products (post_id, category, price, condition, quantity, location, main_image_path, thumbnail_image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        postId,
                        safe(request.productCategory()).isEmpty() ? "OTHER" : safe(request.productCategory()),
                        request.price(),
                        safe(request.productCondition()).isEmpty() ? "GOOD" : safe(request.productCondition()),
                        1,
                        safe(request.location()),
                        safe(request.imagePath()),
                        safe(request.imagePath())
                );
                if (rows <= 0) {
                    dbManager.rollback();
                    return OperationResult.failure("Failed to create sale post.");
                }
            } else if ("RESOURCE".equals(type)) {
                int rows = dbManager.executeUpdate(
                        "INSERT INTO academic_resources (post_id, resource_type, file_path, file_size_mb, price, download_count, subject_area, course_code, university) VALUES (?, ?, ?, 0, ?, 0, ?, ?, ?)",
                        postId,
                        safe(request.resourceType()).isEmpty() ? "NOTES" : safe(request.resourceType()),
                        safe(request.downloadLink()).isEmpty() ? "N/A" : safe(request.downloadLink()),
                        request.price(),
                        "General",
                        "N/A",
                        safe(request.university()).isEmpty() ? "UniNexus" : safe(request.university())
                );
                if (rows <= 0) {
                    dbManager.rollback();
                    return OperationResult.failure("Failed to create resource post.");
                }
            } else {
                dbManager.rollback();
                return OperationResult.failure("Unsupported post type.");
            }

            dbManager.commit();
            return OperationResult.success("Post created successfully.");
        } catch (Exception e) {
            logger.error("Failed creating post for seller {}", request.sellerId(), e);
            try {
                dbManager.rollback();
            } catch (Exception ignored) {
                // no-op
            }
            return OperationResult.failure("Failed to create post due to a database error.");
        }
    }

    public List<PostSummary> getPostsBySeller(int sellerId) {
        String sql = "SELECT p.post_id, p.seller_id, u.username, p.title, p.description, p.status, p.created_at, " +
                "COALESCE(r.location, pr.location, ar.university, 'N/A') AS location, " +
                "COALESCE(r.price_per_month, pr.price, ar.price, 0) AS amount " +
                "FROM posts p " +
                "JOIN users u ON p.seller_id = u.user_id " +
                "LEFT JOIN rentals r ON p.post_id = r.post_id " +
                "LEFT JOIN products pr ON p.post_id = pr.post_id " +
                "LEFT JOIN academic_resources ar ON p.post_id = ar.post_id " +
                "WHERE p.seller_id = ? " +
                "ORDER BY p.created_at DESC";
        return querySummaries(sql, sellerId);
    }

    public List<PostSummary> getAllPosts() {
        String sql = "SELECT p.post_id, p.seller_id, u.username, p.title, p.description, p.status, p.created_at, " +
                "COALESCE(r.location, pr.location, ar.university, 'N/A') AS location, " +
                "COALESCE(r.price_per_month, pr.price, ar.price, 0) AS amount " +
                "FROM posts p " +
                "JOIN users u ON p.seller_id = u.user_id " +
                "LEFT JOIN rentals r ON p.post_id = r.post_id " +
                "LEFT JOIN products pr ON p.post_id = pr.post_id " +
                "LEFT JOIN academic_resources ar ON p.post_id = ar.post_id " +
                "ORDER BY p.created_at DESC";
        return querySummaries(sql);
    }

    public boolean updatePostStatus(int postId, String status) {
        String sql = "UPDATE posts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE post_id = ?";
        return dbManager.executeUpdate(sql, status, postId) > 0;
    }

    public Optional<PostSummary> getPostByIdForSeller(int sellerId, int postId) {
        String sql = "SELECT p.post_id, p.seller_id, u.username, p.title, p.description, p.status, p.created_at, " +
                "COALESCE(r.location, pr.location, ar.university, 'N/A') AS location, " +
                "COALESCE(r.price_per_month, pr.price, ar.price, 0) AS amount " +
                "FROM posts p " +
                "JOIN users u ON p.seller_id = u.user_id " +
                "LEFT JOIN rentals r ON p.post_id = r.post_id " +
                "LEFT JOIN products pr ON p.post_id = pr.post_id " +
                "LEFT JOIN academic_resources ar ON p.post_id = ar.post_id " +
                "WHERE p.seller_id = ? AND p.post_id = ?";

        List<PostSummary> rows = querySummaries(sql, sellerId, postId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public boolean updatePostForSeller(int sellerId, int postId, String title, String description,
                                       String location, double pricePerMonth) {
        if (safe(title).isEmpty() || safe(location).isEmpty() || pricePerMonth <= 0) {
            return false;
        }

        try {
            dbManager.beginTransaction();

            int postRows = dbManager.executeUpdate(
                    "UPDATE posts SET title = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE post_id = ? AND seller_id = ?",
                    safe(title),
                    safe(description),
                    postId,
                    sellerId
            );

            if (postRows == 0) {
                dbManager.rollback();
                return false;
            }

            dbManager.executeUpdate("UPDATE rentals SET location = ?, price_per_month = ? WHERE post_id = ?", safe(location), pricePerMonth, postId);
            dbManager.executeUpdate("UPDATE products SET location = ?, price = ? WHERE post_id = ?", safe(location), pricePerMonth, postId);
            dbManager.executeUpdate("UPDATE academic_resources SET price = ? WHERE post_id = ?", pricePerMonth, postId);

            dbManager.commit();
            return true;
        } catch (Exception e) {
            logger.error("Failed updating post {} for seller {}", postId, sellerId, e);
            try {
                dbManager.rollback();
            } catch (Exception ignored) {
                // no-op
            }
            return false;
        }
    }

    public boolean deletePostForSeller(int sellerId, int postId) {
        String sql = "UPDATE posts SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP WHERE post_id = ? AND seller_id = ?";
        return dbManager.executeUpdate(sql, postId, sellerId) > 0;
    }

    private List<PostSummary> querySummaries(String sql, Object... params) {
        List<PostSummary> summaries = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String createdAtRaw = rs.getString("created_at");
                    LocalDateTime createdAt;
                    try {
                        createdAt = LocalDateTime.parse(createdAtRaw.replace(" ", "T"));
                    } catch (Exception ignored) {
                        createdAt = LocalDateTime.now();
                    }

                    summaries.add(new PostSummary(
                            rs.getInt("post_id"),
                            rs.getInt("seller_id"),
                            rs.getString("username"),
                            rs.getString("title"),
                            rs.getString("description") != null ? rs.getString("description") : "",
                            rs.getString("location") != null ? rs.getString("location") : "N/A",
                            rs.getDouble("amount"),
                            rs.getString("status"),
                            createdAt
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Failed querying post summaries", e);
        }
        return summaries;
    }

    private boolean isUserExisting(int userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            logger.error("Failed to validate user existence: {}", userId, e);
            return false;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record CreatePostRequest(int sellerId,
                                    String postType,
                                    String title,
                                    String description,
                                    String location,
                                    double price,
                                    String imagePath,
                                    int bedrooms,
                                    boolean furnished,
                                    String productCondition,
                                    String productCategory,
                                    String resourceType,
                                    String downloadLink,
                                    String university) {
    }

    public record OperationResult(boolean success, String message) {
        public static OperationResult success(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult failure(String message) {
            return new OperationResult(false, message);
        }
    }
}
