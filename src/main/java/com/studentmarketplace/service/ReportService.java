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

/**
 * Service for item reports and flagged moderation data.
 */
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private final DatabaseManager dbManager;

    public ReportService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public boolean reportPost(int reporterId, int postId, String reason) {
        try {
            dbManager.beginTransaction();

            int reportInserted = dbManager.executeUpdate(
                    "INSERT INTO reports (reporter_id, post_id, reason, status) VALUES (?, ?, ?, 'OPEN')",
                    reporterId,
                    postId,
                    reason == null || reason.isBlank() ? "Reported by user" : reason.trim()
            );

            int flagged = dbManager.executeUpdate(
                    "UPDATE posts SET flagged = 1, updated_at = CURRENT_TIMESTAMP WHERE post_id = ?",
                    postId
            );

            if (reportInserted <= 0 || flagged <= 0) {
                dbManager.rollback();
                return false;
            }

            dbManager.commit();
            return true;
        } catch (Exception e) {
            logger.error("Failed reporting post {}", postId, e);
            try {
                dbManager.rollback();
            } catch (Exception ignored) {
                // no-op
            }
            return false;
        }
    }

    /**
     * Fetch all posts that have at least one open report.
     * Aggregates the most recent report reason via a correlated subquery.
     */
    public List<PostSummary> getFlaggedPosts() {
        List<PostSummary> rows = new ArrayList<>();
        String sql = "SELECT p.post_id, p.seller_id, u.username, p.title, p.description, p.status, p.created_at, " +
                "COALESCE(r.location, pr.location, ar.university, 'N/A') AS location, " +
                "COALESCE(r.price_per_month, pr.price, ar.price, 0) AS amount, " +
                "(" +
                "  SELECT GROUP_CONCAT(rp2.reason, '; ') " +
                "  FROM reports rp2 " +
                "  WHERE rp2.post_id = p.post_id AND rp2.status = 'OPEN'" +
                ") AS report_reason " +
                "FROM posts p " +
                "JOIN users u ON u.user_id = p.seller_id " +
                "LEFT JOIN rentals r ON r.post_id = p.post_id " +
                "LEFT JOIN products pr ON pr.post_id = p.post_id " +
                "LEFT JOIN academic_resources ar ON ar.post_id = p.post_id " +
                "WHERE EXISTS (SELECT 1 FROM reports rp WHERE rp.post_id = p.post_id AND rp.status = 'OPEN') " +
                "ORDER BY p.updated_at DESC";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String createdAtRaw = rs.getString("created_at");
                LocalDateTime createdAt;
                try {
                    createdAt = LocalDateTime.parse(createdAtRaw.replace(" ", "T"));
                } catch (Exception ignored) {
                    createdAt = LocalDateTime.now();
                }

                String reason = rs.getString("report_reason");
                if (reason == null || reason.isBlank()) {
                    reason = "No reason provided";
                }

                rows.add(new PostSummary(
                        rs.getInt("post_id"),
                        rs.getInt("seller_id"),
                        rs.getString("username"),
                        rs.getString("title"),
                        rs.getString("description") == null ? "" : rs.getString("description"),
                        rs.getString("location"),
                        rs.getDouble("amount"),
                        rs.getString("status"),
                        createdAt,
                        reason
                ));
            }
        } catch (Exception e) {
            logger.error("Failed loading flagged posts", e);
        }
        return rows;
    }

    public boolean clearReportsAndFlag(int postId) {
        try {
            dbManager.beginTransaction();
            dbManager.executeUpdate("UPDATE reports SET status = 'RESOLVED', resolved_at = CURRENT_TIMESTAMP WHERE post_id = ? AND status = 'OPEN'", postId);
            int updated = dbManager.executeUpdate("UPDATE posts SET flagged = 0, updated_at = CURRENT_TIMESTAMP WHERE post_id = ?", postId);
            if (updated <= 0) {
                dbManager.rollback();
                return false;
            }
            dbManager.commit();
            return true;
        } catch (Exception e) {
            logger.error("Failed clearing report/flag for post {}", postId, e);
            try {
                dbManager.rollback();
            } catch (Exception ignored) {
                // no-op
            }
            return false;
        }
    }
}
