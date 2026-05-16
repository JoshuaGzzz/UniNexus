package com.studentmarketplace.service;

import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.model.Post.PostStatus;
import com.studentmarketplace.model.Rental;
import com.studentmarketplace.model.Rental.RentalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RentalService handles all rental-related database operations.
 * Implements the Service/DAO pattern to keep controllers lean.
 * Single Responsibility: manages all rental CRUD operations.
 */
public class RentalService {
    private static final Logger logger = LoggerFactory.getLogger(RentalService.class);
    private final DatabaseManager dbManager;

    public RentalService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Create a new rental listing
     */
    public Optional<Integer> createRental(Rental rental) {
        if (!rental.validatePostData()) {
            logger.warn("Invalid rental data: {}", rental);
            return Optional.empty();
        }

        try {
            dbManager.beginTransaction();

            // Insert into posts table
            String postSql = "INSERT INTO posts (seller_id, post_type, title, description, status) " +
                           "VALUES (?, ?, ?, ?, ?)";
            Optional<Long> postId = dbManager.executeInsert(postSql,
                    rental.getSellerId(),
                    rental.getPostType().toString(),
                    rental.getTitle(),
                    rental.getDescription(),
                    rental.getStatus().toString());

            if (!postId.isPresent()) {
                dbManager.rollback();
                logger.error("Failed to insert rental post");
                return Optional.empty();
            }

            // Insert into rentals table
            String rentalSql = "INSERT INTO rentals " +
                    "(post_id, rental_type, location, latitude, longitude, price_per_month, " +
                    "bedrooms, bathrooms, area_sqm, furnished, amenities, available_from, " +
                    "main_image_path, thumbnail_image_path) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            String amenitiesJson = String.join(",", rental.getAmenities());
            dbManager.executeUpdate(rentalSql,
                    postId.get().intValue(),
                    rental.getRentalType().toString(),
                    rental.getLocation(),
                    rental.getLatitude(),
                    rental.getLongitude(),
                    rental.getPricePerMonth(),
                    rental.getBedrooms(),
                    rental.getBathrooms(),
                    rental.getAreaSqm(),
                    rental.isFurnished() ? 1 : 0,
                    amenitiesJson,
                    rental.getAvailableFrom(),
                    rental.getMainImagePath(),
                    rental.getThumbnailImagePath());

            dbManager.commit();
            logger.info("Rental created successfully with post ID: {}", postId.get());
            return Optional.of(postId.get().intValue());

        } catch (SQLException e) {
            logger.error("Failed to create rental", e);
            try {
                dbManager.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Rollback failed", rollbackEx);
            }
            return Optional.empty();
        }
    }

    /**
     * Get rental by ID
     */
    public Optional<Rental> getRentalById(int rentalId) {
        String sql = "SELECT p.post_id, p.seller_id, p.title, p.description, p.status, " +
                    "p.created_at, p.updated_at, r.rental_id, r.rental_type, r.location, " +
                    "r.latitude, r.longitude, r.price_per_month, r.bedrooms, r.bathrooms, " +
                    "r.area_sqm, r.furnished, r.amenities, r.available_from, " +
                    "r.main_image_path, r.thumbnail_image_path " +
                    "FROM posts p JOIN rentals r ON p.post_id = r.post_id " +
                    "WHERE r.rental_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, rentalId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRental(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get rental by ID: {}", rentalId, e);
        }
        return Optional.empty();
    }

    /**
     * Get all active rentals in a specific location
     */
    public List<Rental> getRentalsByLocation(String location) {
        List<Rental> rentals = new ArrayList<>();
        String sql = "SELECT p.post_id, p.seller_id, p.title, p.description, p.status, " +
                    "p.created_at, p.updated_at, r.rental_id, r.rental_type, r.location, " +
                    "r.latitude, r.longitude, r.price_per_month, r.bedrooms, r.bathrooms, " +
                    "r.area_sqm, r.furnished, r.amenities, r.available_from, " +
                    "r.main_image_path, r.thumbnail_image_path " +
                    "FROM posts p JOIN rentals r ON p.post_id = r.post_id " +
                    "WHERE r.location LIKE ? AND p.status = ? " +
                    "ORDER BY p.created_at DESC";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, "%" + location + "%");
            stmt.setString(2, PostStatus.ACTIVE.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rentals.add(mapResultSetToRental(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get rentals by location: {}", location, e);
        }
        return rentals;
    }

    /**
     * Get rentals by type (DORMITORY, APARTMENT, etc.)
     */
    public List<Rental> getRentalsByType(RentalType rentalType) {
        List<Rental> rentals = new ArrayList<>();
        String sql = "SELECT p.post_id, p.seller_id, p.title, p.description, p.status, " +
                    "p.created_at, p.updated_at, r.rental_id, r.rental_type, r.location, " +
                    "r.latitude, r.longitude, r.price_per_month, r.bedrooms, r.bathrooms, " +
                    "r.area_sqm, r.furnished, r.amenities, r.available_from, " +
                    "r.main_image_path, r.thumbnail_image_path " +
                    "FROM posts p JOIN rentals r ON p.post_id = r.post_id " +
                    "WHERE r.rental_type = ? AND p.status = ? " +
                    "ORDER BY r.price_per_month ASC";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, rentalType.toString());
            stmt.setString(2, PostStatus.ACTIVE.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rentals.add(mapResultSetToRental(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get rentals by type: {}", rentalType, e);
        }
        return rentals;
    }

    /**
     * Get rentals within a price range
     */
    public List<Rental> getRentalsByPriceRange(double minPrice, double maxPrice) {
        List<Rental> rentals = new ArrayList<>();
        String sql = "SELECT p.post_id, p.seller_id, p.title, p.description, p.status, " +
                    "p.created_at, p.updated_at, r.rental_id, r.rental_type, r.location, " +
                    "r.latitude, r.longitude, r.price_per_month, r.bedrooms, r.bathrooms, " +
                    "r.area_sqm, r.furnished, r.amenities, r.available_from, " +
                    "r.main_image_path, r.thumbnail_image_path " +
                    "FROM posts p JOIN rentals r ON p.post_id = r.post_id " +
                    "WHERE r.price_per_month BETWEEN ? AND ? AND p.status = ? " +
                    "ORDER BY r.price_per_month ASC";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setDouble(1, minPrice);
            stmt.setDouble(2, maxPrice);
            stmt.setString(3, PostStatus.ACTIVE.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rentals.add(mapResultSetToRental(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get rentals by price range: {} - {}", minPrice, maxPrice, e);
        }
        return rentals;
    }

    /**
     * Update rental listing
     */
    public boolean updateRental(Rental rental) {
        if (!rental.validatePostData()) {
            logger.warn("Invalid rental data for update: {}", rental);
            return false;
        }

        String sql = "UPDATE rentals SET rental_type = ?, location = ?, latitude = ?, " +
                    "longitude = ?, price_per_month = ?, bedrooms = ?, bathrooms = ?, " +
                    "area_sqm = ?, furnished = ?, amenities = ?, available_from = ?, " +
                    "main_image_path = ?, thumbnail_image_path = ? " +
                    "WHERE rental_id = ?";

        try {
            String amenitiesJson = String.join(",", rental.getAmenities());
            int rowsAffected = dbManager.executeUpdate(sql,
                    rental.getRentalType().toString(),
                    rental.getLocation(),
                    rental.getLatitude(),
                    rental.getLongitude(),
                    rental.getPricePerMonth(),
                    rental.getBedrooms(),
                    rental.getBathrooms(),
                    rental.getAreaSqm(),
                    rental.isFurnished() ? 1 : 0,
                    amenitiesJson,
                    rental.getAvailableFrom(),
                    rental.getMainImagePath(),
                    rental.getThumbnailImagePath(),
                    rental.getRentalId());

            if (rowsAffected > 0) {
                logger.info("Rental updated successfully: {}", rental.getRentalId());
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to update rental: {}", rental.getRentalId(), e);
        }
        return false;
    }

    /**
     * Delete (archive) a rental
     */
    public boolean deleteRental(int rentalId) {
        String sql = "UPDATE posts SET status = ? WHERE post_id IN " +
                    "(SELECT post_id FROM rentals WHERE rental_id = ?)";

        try {
            int rowsAffected = dbManager.executeUpdate(sql, PostStatus.DELETED.toString(), rentalId);
            if (rowsAffected > 0) {
                logger.info("Rental deleted: {}", rentalId);
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to delete rental: {}", rentalId, e);
        }
        return false;
    }

    /**
     * Map ResultSet to Rental object
     */
    private Rental mapResultSetToRental(ResultSet rs) throws SQLException {
        List<String> amenities = new ArrayList<>();
        String amenitiesStr = rs.getString("amenities");
        if (amenitiesStr != null && !amenitiesStr.isEmpty()) {
            for (String amenity : amenitiesStr.split(",")) {
                amenities.add(amenity.trim());
            }
        }

        return new Rental(
                rs.getInt("post_id"),
                rs.getInt("seller_id"),
                rs.getString("title"),
                rs.getString("description"),
                PostStatus.valueOf(rs.getString("status")),
            parseSqlDateTime(rs.getString("created_at")),
            parseSqlDateTime(rs.getString("updated_at")),
                rs.getInt("rental_id"),
                RentalType.valueOf(rs.getString("rental_type")),
                rs.getString("location"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                rs.getDouble("price_per_month"),
                rs.getInt("bedrooms"),
                rs.getInt("bathrooms"),
                rs.getDouble("area_sqm"),
                rs.getBoolean("furnished"),
                amenities,
                rs.getDate("available_from") != null ? rs.getDate("available_from").toLocalDate() : null,
                rs.getString("main_image_path"),
                rs.getString("thumbnail_image_path")
        );
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
}
