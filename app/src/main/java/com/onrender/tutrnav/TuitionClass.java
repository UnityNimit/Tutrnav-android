package com.onrender.tutrnav;

public class TuitionClass {
    String subject;
    String topic;
    String tutorName;
    String startTime;
    String duration;
    String location;
    String status; // "UPCOMING", "LIVE", "CANCELLED"
    int colorCode; // Color integer

    public TuitionClass(String subject, String topic, String tutorName, String startTime, String duration, String location, String status, int colorCode) {
        this.subject = subject;
        this.topic = topic;
        this.tutorName = tutorName;
        this.startTime = startTime;
        this.duration = duration;
        this.location = location;
        this.status = status;
        this.colorCode = colorCode;
    }

    // Getters
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public String getTutorName() { return tutorName; }
    public String getStartTime() { return startTime; }
    public String getDuration() { return duration; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public int getColorCode() { return colorCode; }
}