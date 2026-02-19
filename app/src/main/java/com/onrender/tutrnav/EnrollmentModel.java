package com.onrender.tutrnav;

import com.google.firebase.Timestamp;

public class EnrollmentModel {
    private String enrollmentId;
    private String studentId;
    private String studentName; // Cached for display
    private String studentPhoto;
    private String teacherId;
    private String tuitionId;
    private String tuitionTitle; // Cached for display
    private String status; // "pending", "approved", "rejected"
    private Timestamp timestamp;

    public EnrollmentModel() {} // Required for Firestore

    public EnrollmentModel(String studentId, String studentName, String studentPhoto, String teacherId, String tuitionId, String tuitionTitle) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentPhoto = studentPhoto;
        this.teacherId = teacherId;
        this.tuitionId = tuitionId;
        this.tuitionTitle = tuitionTitle;
        this.status = "pending";
        this.timestamp = Timestamp.now();
    }

    // Getters & Setters
    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentPhoto() { return studentPhoto; }
    public String getTeacherId() { return teacherId; }
    public String getTuitionId() { return tuitionId; }
    public String getTuitionTitle() { return tuitionTitle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getTimestamp() { return timestamp; }
}