package com.onrender.tutrnav;

import java.util.List;

public class TuitionModel {

    // --- Unique Identifiers ---
    private String tuitionId;
    private String teacherId;

    // --- Core Class Details ---
    private String title;
    private String subject; // Kept for legacy support, main categorization is now via Tags
    private String fee;
    private int maxStudents; // NEW: Field for maximum capacity
    private String description;
    private String bannerUrl;

    // --- Location Data ---
    private double latitude;
    private double longitude;

    // --- Teacher Info (Denormalized) ---
    private String teacherName;
    private String teacherPhoto;

    // --- Metadata ---
    private List<String> tags;

    // ==========================================
    //       CONSTRUCTORS
    // ==========================================

    // 1. Required Empty Constructor for Firebase/Firestore
    public TuitionModel() {}

    // 2. Full Parameter Constructor
    public TuitionModel(String tuitionId, String teacherId, String title, String subject,
                        String fee, int maxStudents, String description, String bannerUrl,
                        double latitude, double longitude, String teacherName,
                        String teacherPhoto, List<String> tags) {
        this.tuitionId = tuitionId;
        this.teacherId = teacherId;
        this.title = title;
        this.subject = subject;
        this.fee = fee;
        this.maxStudents = maxStudents;
        this.description = description;
        this.bannerUrl = bannerUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.teacherName = teacherName;
        this.teacherPhoto = teacherPhoto;
        this.tags = tags;
    }

    // ==========================================
    //       GETTERS AND SETTERS
    // ==========================================

    // Note: Setters are required for Firebase to populate fields correctly
    // if the constructor isn't used directly during deserialization.

    public String getTuitionId() { return tuitionId; }
    public void setTuitionId(String tuitionId) { this.tuitionId = tuitionId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getFee() { return fee; }
    public void setFee(String fee) { this.fee = fee; }

    public int getMaxStudents() { return maxStudents; }
    public void setMaxStudents(int maxStudents) { this.maxStudents = maxStudents; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getTeacherPhoto() { return teacherPhoto; }
    public void setTeacherPhoto(String teacherPhoto) { this.teacherPhoto = teacherPhoto; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}