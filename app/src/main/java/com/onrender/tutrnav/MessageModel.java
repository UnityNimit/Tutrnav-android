package com.onrender.tutrnav;

import com.google.firebase.Timestamp;

public class MessageModel {
    private String messageId;
    private String text;
    private String senderId;
    private String senderName;
    private String teacherPhoto;
    private String tuitionId;
    private String tuitionTitle;
    private String type; // "NORMAL", "IMPORTANT", "FEE"
    private boolean isBroadcast; // Added this back to match your previous logic
    private Timestamp timestamp;

    public MessageModel() {}

    public MessageModel(String text, String senderId, String senderName, String teacherPhoto, String tuitionId, String tuitionTitle, String type) {
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.teacherPhoto = teacherPhoto;
        this.tuitionId = tuitionId;
        this.tuitionTitle = tuitionTitle;
        this.type = type;
        this.isBroadcast = true;
        this.timestamp = Timestamp.now();
    }

    // --- GETTERS ---
    public String getMessageId() { return messageId; }
    public String getText() { return text; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getTeacherPhoto() { return teacherPhoto; }
    public String getTuitionId() { return tuitionId; }
    public String getTuitionTitle() { return tuitionTitle; }
    public String getType() { return type; }
    public boolean isBroadcast() { return isBroadcast; }
    public Timestamp getTimestamp() { return timestamp; }

    // --- CRITICAL: SETTERS FOR FIRESTORE ---
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setText(String text) { this.text = text; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setTeacherPhoto(String teacherPhoto) { this.teacherPhoto = teacherPhoto; }
    public void setTuitionId(String tuitionId) { this.tuitionId = tuitionId; }
    public void setTuitionTitle(String tuitionTitle) { this.tuitionTitle = tuitionTitle; }
    public void setType(String type) { this.type = type; }
    public void setBroadcast(boolean broadcast) { isBroadcast = broadcast; } // Note: Firestore maps "isBroadcast" to "broadcast" usually
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}