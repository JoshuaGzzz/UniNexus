package com.studentmarketplace.model;

import java.time.LocalDateTime;

/**
 * AcademicResource class for RESOURCE type posts.
 * Represents digital resources like software, PDFs, research papers, and notes.
 */
public class AcademicResource extends Post {
    private int resourceId;
    private ResourceType resourceType;
    private String filePath;
    private double fileSizeMb;
    private double price;
    private int downloadCount;
    private String subjectArea;
    private String courseCode;
    private String university;
    private String previewImagePath;

    public enum ResourceType {
        SOFTWARE, PDF, RESEARCH_PAPER, NOTES, OTHER
    }

    /**
     * Constructor for new resource posts
     */
    public AcademicResource(int sellerId, String title, String description,
                           ResourceType resourceType, String filePath) {
        super(sellerId, title, description, PostType.RESOURCE);
        this.resourceType = resourceType;
        this.filePath = filePath;
        this.price = 0.0; // Free by default
        this.downloadCount = 0;
    }

    /**
     * Constructor for loading existing resources from database
     */
    public AcademicResource(int postId, int sellerId, String title, String description,
                           PostStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                           int resourceId, ResourceType resourceType, String filePath,
                           double fileSizeMb, double price, int downloadCount,
                           String subjectArea, String courseCode, String university) {
        super(postId, sellerId, title, description, PostType.RESOURCE, status, createdAt, updatedAt);
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.filePath = filePath;
        this.fileSizeMb = fileSizeMb;
        this.price = price;
        this.downloadCount = downloadCount;
        this.subjectArea = subjectArea;
        this.courseCode = courseCode;
        this.university = university;
    }

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public String getDetailedDescription() {
        String courseInfo = courseCode != null ? "Course: " + courseCode : "";
        String subjectInfo = subjectArea != null ? "Subject: " + subjectArea : "";
        return String.format("%s\n\nType: %s | Size: %.2f MB\nPrice: ₱%.2f | Downloads: %d\n%s %s",
                description, resourceType, fileSizeMb, price, downloadCount,
                courseInfo, subjectInfo);
    }

    @Override
    public boolean validatePostData() {
        return title != null && !title.trim().isEmpty() &&
               filePath != null && !filePath.trim().isEmpty() &&
               price >= 0;
    }

    @Override
    public String getMainImagePath() {
        return previewImagePath;
    }

    public void setPreviewImagePath(String previewImagePath) {
        this.previewImagePath = previewImagePath;
    }

    // Getters and Setters
    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        this.updatedAt = LocalDateTime.now();
    }

    public double getFileSizeMb() {
        return fileSizeMb;
    }

    public void setFileSizeMb(double fileSizeMb) {
        this.fileSizeMb = fileSizeMb;
    }

    public void setPrice(double price) {
        if (price >= 0) {
            this.price = price;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public String getSubjectArea() {
        return subjectArea;
    }

    public void setSubjectArea(String subjectArea) {
        this.subjectArea = subjectArea;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
        this.updatedAt = LocalDateTime.now();
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("AcademicResource[ID=%d, Type=%s, Title=%s, Size=%.2f MB, Price=₱%.2f, Downloads=%d]",
                resourceId, resourceType, title, fileSizeMb, price, downloadCount);
    }
}
