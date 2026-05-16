package com.studentmarketplace.model;

import java.time.LocalDateTime;

/**
 * Abstract base class for all post types in the marketplace.
 * Implements the Template Method pattern for common post operations.
 */
public abstract class Post {
    protected int postId;
    protected int sellerId;
    protected String title;
    protected String description;
    protected String ownerUsername;
    protected PostStatus status;
    protected PostType postType;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    public enum PostType {
        SALE, RENTAL, RESOURCE
    }

    public enum PostStatus {
        ACTIVE, SOLD, RENTED, FLAGGED, ARCHIVED, DELETED
    }

    /**
     * Constructor for new posts (before database insertion)
     */
    public Post(int sellerId, String title, String description, PostType postType) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.postType = postType;
        this.status = PostStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructor for existing posts from database
     */
    public Post(int postId, int sellerId, String title, String description,
                PostType postType, PostStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(sellerId, title, description, postType);
        this.postId = postId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Abstract methods that subclasses must implement
    /**
     * Get the display price for this post
     */
    public abstract double getPrice();

    /**
     * Get a detailed description for display
     */
    public abstract String getDetailedDescription();

    /**
     * Validate post-specific data
     */
    public abstract boolean validatePostData();

    /**
     * Get the primary image path for display
     */
    public abstract String getMainImagePath();

    // Common getters/setters
    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getSellerId() {
        return sellerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public PostType getPostType() {
        return postType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return String.format("%s[ID=%d, Title=%s, Status=%s, Seller=%d]",
                postType, postId, title, status, sellerId);
    }
}
