package com.onrender.tutrnav;

import java.util.List;

public class TuitionModel {

    // --- Unique Identifiers ---
    private String tuitionId;
    private String teacherId;

    // --- Core Class Details ---
    private String title;
    private String subject;      // Kept for legacy support, main categorization is now via Tags
    private String time;         // NEW: Field for class timings (e.g., "04:00 PM - 06:00 PM")
    private String fee;
    private int maxStudents;     // NEW: Field for maximum capacity
    private String description;
    private String bannerUrl;

    // --- Location Data ---
    private double latitude;
    private double longitude;

    // --- Teacher Info (Denormalized for faster reads) ---
    private String teacherName;
    private String teacherPhoto;

    // --- Metadata ---
    private List<String> tags;

    // ==========================================
    //       CONSTRUCTORS
    // ==========================================

    /**
     * Required Empty Constructor for Firebase/Firestore Deserialization.
     * Do NOT remove this, or Firestore will crash when fetching data!
     */
    public TuitionModel() {}

    /**
     * Full Parameter Constructor for manual instantiations.
     */
    public TuitionModel(String tuitionId, String teacherId, String title, String subject,
                        String time, String fee, int maxStudents, String description,
                        String bannerUrl, double latitude, double longitude,
                        String teacherName, String teacherPhoto, List<String> tags) {
        this.tuitionId = tuitionId;
        this.teacherId = teacherId;
        this.title = title;
        this.subject = subject;
        this.time = time; // Initialized newly added timings
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

    // Note: Setters are required for Firebase to dynamically map fields
    // to variables if the empty constructor is used.

    public String getTuitionId() { return tuitionId; }
    public void setTuitionId(String tuitionId) { this.tuitionId = tuitionId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    // --- NEW: Timing Getter/Setter ---
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

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getTeacherPhoto() { return teacherPhoto; }
    public void setTeacherPhoto(String teacherPhoto) { this.teacherPhoto = teacherPhoto; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // ==========================================
    //       DEBUGGING / UTILITY
    // ==========================================

    /**
     * Clean toString() representation for easy Logcat debugging.
     */
    @Override
    public String toString() {
        return "TuitionModel{" +
                "tuitionId='" + tuitionId + '\'' +
                ", title='" + title + '\'' +
                ", time='" + time + '\'' +
                ", fee='" + fee + '\'' +
                ", maxStudents=" + maxStudents +
                ", teacherName='" + teacherName + '\'' +
                '}';
    }
}