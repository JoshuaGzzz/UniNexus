package com.studentmarketplace.service;

import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service dedicated to user profile and admin user management operations.
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final DatabaseManager dbManager;

    public UserService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Optional<User> getUserById(int userId) {
        String sql = "SELECT user_id, username, email, password_hash, full_name, student_id, university, phone, profile_image_path, bio, rating, total_ratings, account_status, created_at, updated_at FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
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
                            java.time.LocalDateTime.parse(rs.getString("created_at").replace(" ", "T")),
                            java.time.LocalDateTime.parse(rs.getString("updated_at").replace(" ", "T"))
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Failed loading user profile: {}", userId, e);
        }
        return Optional.empty();
    }

    public boolean updateProfile(int userId, String university, String phone, String bio) {
        String sql = "UPDATE users SET university = ?, phone = ?, bio = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        return dbManager.executeUpdate(sql,
                university == null ? "" : university.trim(),
                phone == null ? "" : phone.trim(),
                bio == null ? "" : bio.trim(),
                userId
        ) > 0;
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
                        java.time.LocalDateTime.parse(rs.getString("created_at").replace(" ", "T")),
                        java.time.LocalDateTime.parse(rs.getString("updated_at").replace(" ", "T"))
                ));
            }
        } catch (Exception e) {
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
}
