package com.onrender.tutrnav.models;

import java.util.List;

public class TuitionModel {

    // --- Unique Identifiers ---
    private String tuitionId;
    private String teacherId;

    // --- Core Class Details ---
    private String title;
    private String subject;      // Legacy support
    private String time;         // Class Timings (e.g. "04:00 PM")
    private String fee;
    private int maxStudents;
    private String description;
    private String bannerUrl;

    // --- Location Data ---
    private double latitude;
    private double longitude;
    private String address;      // NEW: Human readable address (e.g. "123 Main St")

    // --- Teacher Info (Saved explicitly to prevent showing Student's own info) ---
    private String teacherName;
    private String teacherPhoto;
    private String teacherPhone;      // NEW: For the Dial intent
    private String teacherExperience; // NEW: For the Bottom Sheet profile

    // --- Metadata ---
    private List<String> tags;
    private double rating; // Default rating for UI

    // ==========================================
    //       CONSTRUCTORS
    // ==========================================

    /**
     * Required Empty Constructor for Firebase/Firestore Deserialization.
     */
    public TuitionModel() {}

    /**
     * Full Parameter Constructor
     */
    public TuitionModel(String tuitionId, String teacherId, String title, String subject,
                        String time, String fee, int maxStudents, String description,
                        String bannerUrl, double latitude, double longitude, String address,
                        String teacherName, String teacherPhoto, String teacherPhone,
                        String teacherExperience, List<String> tags) {
        this.tuitionId = tuitionId;
        this.teacherId = teacherId;
        this.title = title;
        this.subject = subject;
        this.time = time;
        this.fee = fee;
        this.maxStudents = maxStudents;
        this.description = description;
        this.bannerUrl = bannerUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.teacherName = teacherName;
        this.teacherPhoto = teacherPhoto;
        this.teacherPhone = teacherPhone;
        this.teacherExperience = teacherExperience;
        this.tags = tags;
        this.rating = 4.8; // Default placeholder rating
    }

    // ==========================================
    //       GETTERS AND SETTERS
    // ==========================================

    public String getTuitionId() { return tuitionId; }
    public void setTuitionId(String tuitionId) { this.tuitionId = tuitionId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

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

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    // --- Teacher Details ---
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getTeacherPhoto() { return teacherPhoto; }
    public void setTeacherPhoto(String teacherPhoto) { this.teacherPhoto = teacherPhoto; }

    public String getTeacherPhone() { return teacherPhone; }
    public void setTeacherPhone(String teacherPhone) { this.teacherPhone = teacherPhone; }

    public String getTeacherExperience() { return teacherExperience; }
    public void setTeacherExperience(String teacherExperience) { this.teacherExperience = teacherExperience; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
}