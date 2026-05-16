package com.studentmarketplace.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rental class for RENTAL type posts.
 * Represents rental properties (dormitories, apartments, houses, rooms).
 */
public class Rental extends Post {
    private int rentalId;
    private RentalType rentalType;
    private String location;
    private double latitude;
    private double longitude;
    private double pricePerMonth;
    private int bedrooms;
    private int bathrooms;
    private double areaSqm;
    private boolean furnished;
    private List<String> amenities;
    private LocalDate availableFrom;
    private String mainImagePath;
    private String thumbnailImagePath;
    private List<String> additionalImages;

    public enum RentalType {
        DORMITORY, APARTMENT, ROOM, HOUSE
    }

    /**
     * Constructor for new rental posts
     */
    public Rental(int sellerId, String title, String description,
                  RentalType rentalType, String location, double pricePerMonth) {
        super(sellerId, title, description, PostType.RENTAL);
        this.rentalType = rentalType;
        this.location = location;
        this.pricePerMonth = pricePerMonth;
        this.bedrooms = 1;
        this.bathrooms = 1;
        this.furnished = false;
        this.amenities = new ArrayList<>();
        this.additionalImages = new ArrayList<>();
    }

    /**
     * Constructor for loading existing rentals from database
     */
    public Rental(int postId, int sellerId, String title, String description,
                  PostStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                  int rentalId, RentalType rentalType, String location,
                  double latitude, double longitude, double pricePerMonth,
                  int bedrooms, int bathrooms, double areaSqm, boolean furnished,
                  List<String> amenities, LocalDate availableFrom,
                  String mainImagePath, String thumbnailImagePath) {
        super(postId, sellerId, title, description, PostType.RENTAL, status, createdAt, updatedAt);
        this.rentalId = rentalId;
        this.rentalType = rentalType;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pricePerMonth = pricePerMonth;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.areaSqm = areaSqm;
        this.furnished = furnished;
        this.amenities = amenities != null ? new ArrayList<>(amenities) : new ArrayList<>();
        this.availableFrom = availableFrom;
        this.mainImagePath = mainImagePath;
        this.thumbnailImagePath = thumbnailImagePath;
        this.additionalImages = new ArrayList<>();
    }

    @Override
    public double getPrice() {
        return pricePerMonth;
    }

    @Override
    public String getDetailedDescription() {
        String amenitiesStr = amenities.isEmpty() ? "None" : String.join(", ", amenities);
        return String.format("%s\n\nType: %s | Location: %s\nBedrooms: %d | Bathrooms: %d | Area: %.0f sqm\n" +
                "Price: ₱%.2f/month | Furnished: %s\nAmenities: %s",
                description, rentalType, location, bedrooms, bathrooms, areaSqm,
                pricePerMonth, furnished ? "Yes" : "No", amenitiesStr);
    }

    @Override
    public boolean validatePostData() {
        return title != null && !title.trim().isEmpty() &&
               pricePerMonth > 0 &&
               location != null && !location.trim().isEmpty() &&
               bedrooms > 0 &&
               bathrooms > 0;
    }

    @Override
    public String getMainImagePath() {
        return mainImagePath;
    }

    // Getters and Setters
    public int getRentalId() {
        return rentalId;
    }

    public void setRentalId(int rentalId) {
        this.rentalId = rentalId;
    }

    public RentalType getRentalType() {
        return rentalType;
    }

    public void setRentalType(RentalType rentalType) {
        this.rentalType = rentalType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getPricePerMonth() {
        return pricePerMonth;
    }

    public void setPricePerMonth(double pricePerMonth) {
        if (pricePerMonth > 0) {
            this.pricePerMonth = pricePerMonth;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(int bedrooms) {
        if (bedrooms > 0) {
            this.bedrooms = bedrooms;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public int getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(int bathrooms) {
        if (bathrooms > 0) {
            this.bathrooms = bathrooms;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public double getAreaSqm() {
        return areaSqm;
    }

    public void setAreaSqm(double areaSqm) {
        this.areaSqm = areaSqm;
    }

    public boolean isFurnished() {
        return furnished;
    }

    public void setFurnished(boolean furnished) {
        this.furnished = furnished;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getAmenities() {
        return new ArrayList<>(amenities);
    }

    public void addAmenity(String amenity) {
        if (!amenities.contains(amenity)) {
            amenities.add(amenity);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeAmenity(String amenity) {
        amenities.remove(amenity);
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDate availableFrom) {
        this.availableFrom = availableFrom;
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

    @Override
    public String toString() {
        return String.format("Rental[ID=%d, Type=%s, Location=%s, Price=₱%.2f/mo, Beds=%d, Baths=%d]",
                rentalId, rentalType, location, pricePerMonth, bedrooms, bathrooms);
    }
}
