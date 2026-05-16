package com.studentmarketplace.model;

import java.time.LocalDateTime;

/**
 * Lightweight list row model for client/admin post management screens.
 */
public class PostSummary {
    private final int postId;
    private final int sellerId;
    private final String sellerUsername;
    private final String title;
    private final String description;
    private final String location;
    private final double pricePerMonth;
    private final String status;
    private final LocalDateTime createdAt;
    private final String reportReason;

    /**
     * Full constructor including report reason (for flagged posts view).
     */
    public PostSummary(int postId,
                       int sellerId,
                       String sellerUsername,
                       String title,
                       String description,
                       String location,
                       double pricePerMonth,
                       String status,
                       LocalDateTime createdAt,
                       String reportReason) {
        this.postId = postId;
        this.sellerId = sellerId;
        this.sellerUsername = sellerUsername;
        this.title = title;
        this.description = description;
        this.location = location;
        this.pricePerMonth = pricePerMonth;
        this.status = status;
        this.createdAt = createdAt;
        this.reportReason = reportReason;
    }

    /**
     * Backward-compatible constructor without report reason.
     */
    public PostSummary(int postId,
                       int sellerId,
                       String sellerUsername,
                       String title,
                       String description,
                       String location,
                       double pricePerMonth,
                       String status,
                       LocalDateTime createdAt) {
        this(postId, sellerId, sellerUsername, title, description, location,
             pricePerMonth, status, createdAt, "");
    }

    public int getPostId() {
        return postId;
    }

    public int getSellerId() {
        return sellerId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public double getPricePerMonth() {
        return pricePerMonth;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getReportReason() {
        return reportReason;
    }

    @Override
    public String toString() {
        return String.format("#%d | %s | %s | Php %.0f/mo | %s", postId, title, location, pricePerMonth, status);
    }
}
