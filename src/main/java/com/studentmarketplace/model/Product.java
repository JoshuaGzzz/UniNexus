package com.studentmarketplace.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product class for SALE type posts.
 * Represents physical items being sold on the marketplace.
 */
public class Product extends Post {
    private int productId;
    private ProductCategory category;
    private double price;
    private ProductCondition condition;
    private int quantity;
    private String location;
    private String mainImagePath;
    private String thumbnailImagePath;
    private List<String> additionalImages;

    public enum ProductCategory {
        ELECTRONICS, TEXTBOOKS, FURNITURE, CLOTHING, ACCESSORIES, OTHER
    }

    public enum ProductCondition {
        LIKE_NEW, GOOD, FAIR, POOR
    }

    /**
     * Constructor for new product posts
     */
    public Product(int sellerId, String title, String description,
                   ProductCategory category, double price, String location) {
        super(sellerId, title, description, PostType.SALE);
        this.category = category;
        this.price = price;
        this.location = location;
        this.condition = ProductCondition.GOOD;
        this.quantity = 1;
        this.additionalImages = new ArrayList<>();
    }

    /**
     * Constructor for loading existing products from database
     */
    public Product(int postId, int sellerId, String title, String description,
                   PostStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                   int productId, ProductCategory category, double price,
                   ProductCondition condition, int quantity, String location,
                   String mainImagePath, String thumbnailImagePath) {
        super(postId, sellerId, title, description, PostType.SALE, status, createdAt, updatedAt);
        this.productId = productId;
        this.category = category;
        this.price = price;
        this.condition = condition;
        this.quantity = quantity;
        this.location = location;
        this.mainImagePath = mainImagePath;
        this.thumbnailImagePath = thumbnailImagePath;
        this.additionalImages = new ArrayList<>();
    }

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public String getDetailedDescription() {
        return String.format("%s\nCategory: %s | Condition: %s | Location: %s | Qty: %d | Price: ₱%.2f",
                description, category, condition, location, quantity, price);
    }

    @Override
    public boolean validatePostData() {
        return title != null && !title.trim().isEmpty() &&
               price > 0 &&
               location != null && !location.trim().isEmpty() &&
               quantity > 0;
    }

    @Override
    public String getMainImagePath() {
        return mainImagePath;
    }

    // Getters and Setters
    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public void setPrice(double price) {
        if (price > 0) {
            this.price = price;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public ProductCondition getCondition() {
        return condition;
    }

    public void setCondition(ProductCondition condition) {
        this.condition = condition;
        this.updatedAt = LocalDateTime.now();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity > 0) {
            this.quantity = quantity;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }

    public void setMainImagePath(String mainImagePath) {
        this.mainImagePath = mainImagePath;
    }

    public String getThumbnailImagePath() {
        return thumbnailImagePath;
    }

    public void setThumbnailImagePath(String thumbnailImagePath) {
        this.thumbnailImagePath = thumbnailImagePath;
    }

    public List<String> getAdditionalImages() {
        return new ArrayList<>(additionalImages);
    }

    public void addAdditionalImage(String imagePath) {
        this.additionalImages.add(imagePath);
    }

    public void removeAdditionalImage(String imagePath) {
        this.additionalImages.remove(imagePath);
    }

    @Override
    public String toString() {
        return String.format("Product[ID=%d, Title=%s, Category=%s, Price=₱%.2f, Location=%s, Qty=%d]",
                productId, title, category, price, location, quantity);
    }
}
