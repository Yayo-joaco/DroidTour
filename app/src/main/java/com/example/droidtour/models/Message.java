package com.example.droidtour.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Message {
    private String messageId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName;
    private String senderType; // "CLIENT", "COMPANY", "GUIDE", "ADMIN"
    private String messageText;
    private Date timestamp;
    private boolean isRead;
    private String conversationId; // Para agrupar mensajes

    public Message() {}

    public Message(String messageId, String senderId, String senderName, String receiverId, 
                   String receiverName, String senderType, String messageText, 
                   Date timestamp, boolean isRead, String conversationId) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.senderType = senderType;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.conversationId = conversationId;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId);
        map.put("senderId", senderId);
        map.put("senderName", senderName);
        map.put("receiverId", receiverId);
        map.put("receiverName", receiverName);
        map.put("senderType", senderType);
        map.put("messageText", messageText);
        map.put("timestamp", timestamp);
        map.put("isRead", isRead);
        map.put("conversationId", conversationId);
        return map;
    }

    // Getters
    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getReceiverId() { return receiverId; }
    public String getReceiverName() { return receiverName; }
    public String getSenderType() { return senderType; }
    public String getMessageText() { return messageText; }
    public Date getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public String getConversationId() { return conversationId; }

    // Setters
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public void setSenderType(String senderType) { this.senderType = senderType; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { isRead = read; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
}

