package com.studentmarketplace.model;

import java.time.LocalDateTime;

/**
 * User class representing a student/seller on the marketplace.
 * Handles authentication and user profile information.
 */
public class User {
    private int userId;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String studentId;
    private String university;
    private String phone;
    private String profileImagePath;
    private String bio;
    private double rating;
    private int totalRatings;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum AccountStatus {
        ACTIVE, SUSPENDED, BANNED
    }

    /**
     * Constructor for new user registration
     */
    public User(String username, String email, String passwordHash,
                String fullName, String studentId, String university) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.studentId = studentId;
        this.university = university;
        this.rating = 5.0;
        this.totalRatings = 0;
        this.accountStatus = AccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructor for loading existing users from database
     */
    public User(int userId, String username, String email, String passwordHash,
                String fullName, String studentId, String university, String phone,
                String profileImagePath, String bio, double rating, int totalRatings,
                AccountStatus accountStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.studentId = studentId;
        this.university = university;
        this.phone = phone;
        this.profileImagePath = profileImagePath;
        this.bio = bio;
        this.rating = rating;
        this.totalRatings = totalRatings;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Verify user password against stored hash (implement with bcrypt/Argon2 in production)
     */
    public boolean verifyPassword(String plainPassword) {
        // TODO: Implement proper password hashing verification with bcrypt
        // For now, this is a placeholder
        return passwordHash.equals(hashPassword(plainPassword));
    }

    /**
     * Simple password hasher (REPLACE with bcrypt in production)
     */
    private static String hashPassword(String password) {
        // TODO: Use BCrypt.hashpw() in production
        return Integer.toHexString(password.hashCode());
    }

    /**
     * Add a rating to this user's profile
     */
    public void addRating(double newRating) {
        if (newRating < 1.0 || newRating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1.0 and 5.0");
        }
        // Calculate weighted average
        this.rating = (this.rating * this.totalRatings + newRating) / (this.totalRatings + 1);
        this.totalRatings++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if user account is active
     */
    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.updatedAt = LocalDateTime.now();
    }

    public String getStudentId() {
        return studentId;
    }

    public String getUniversity() {
        return university;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
        this.updatedAt = LocalDateTime.now();
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
        this.updatedAt = LocalDateTime.now();
    }

    public double getRating() {
        return Math.round(rating * 10.0) / 10.0; // Round to 1 decimal place
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return String.format("User[ID=%d, Username=%s, Name=%s, University=%s, Rating=%.1f★ (%d reviews)]",
                userId, username, fullName, university, getRating(), totalRatings);
    }
}
