package com.onrender.tutrnav.models;

public class EnrollmentModel {
    private String enrollmentId;
    private String studentId;
    private String studentName;
    private String studentPhoto;
    private String teacherId;
    private String tuitionId;
    private String tuitionTitle;
    private String status; // "pending", "approved", "rejected"

    // THE FIX 1: Use Long instead of Timestamp to prevent deserialization crashes
    private Long timestamp;

    // Required Empty Constructor for Firestore
    public EnrollmentModel() {}

    // Constructor for manual instantiation
    public EnrollmentModel(String studentId, String studentName, String studentPhoto, String teacherId, String tuitionId, String tuitionTitle) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentPhoto = studentPhoto;
        this.teacherId = teacherId;
        this.tuitionId = tuitionId;
        this.tuitionTitle = tuitionTitle;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    // ==========================================
    //       GETTERS AND SETTERS
    // ==========================================

    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentPhoto() { return studentPhoto; }
    public void setStudentPhoto(String studentPhoto) { this.studentPhoto = studentPhoto; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTuitionId() { return tuitionId; }
    public void setTuitionId(String tuitionId) { this.tuitionId = tuitionId; }

    public String getTuitionTitle() { return tuitionTitle; }
    public void setTuitionTitle(String tuitionTitle) { this.tuitionTitle = tuitionTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}