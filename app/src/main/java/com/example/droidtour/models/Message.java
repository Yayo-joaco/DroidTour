package com.example.droidtour.models;

import com.google.firebase.Timestamp;
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
    private String content; // Alias de messageText
    private Timestamp timestamp;
    private Timestamp createdAt; // Alias de timestamp
    private boolean isRead;
    private String conversationId; // Para agrupar mensajes
    private String companyId;
    
    // Campos para archivos adjuntos
    private boolean hasAttachment;
    private String attachmentUrl;
    private String attachmentType; // "IMAGE" o "PDF"
    private String attachmentName;
    private Long attachmentSize; // Tamaño en bytes

    public Message() {}

    public Message(String messageId, String senderId, String senderName, String receiverId,
                   String receiverName, String senderType, String messageText,
                   Timestamp timestamp, boolean isRead, String conversationId) {
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
        map.put("hasAttachment", hasAttachment);
        if (attachmentUrl != null) map.put("attachmentUrl", attachmentUrl);
        if (attachmentType != null) map.put("attachmentType", attachmentType);
        if (attachmentName != null) map.put("attachmentName", attachmentName);
        if (attachmentSize != null) map.put("attachmentSize", attachmentSize);
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
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public boolean getIsRead() { return isRead; }
    public String getConversationId() { return conversationId; }
    public String getCompanyId() { return companyId; }
    
    // Getters para archivos adjuntos
    public boolean hasAttachment() { return hasAttachment; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public String getAttachmentType() { return attachmentType; }
    public String getAttachmentName() { return attachmentName; }
    public Long getAttachmentSize() { return attachmentSize; }
    
    // Alias getters
    public String getContent() { 
        return content != null ? content : messageText; 
    }
    public Date getCreatedAt() { 
        if (createdAt != null) return createdAt.toDate();
        if (timestamp != null) return timestamp.toDate();
        return null; 
    }

    // Setters
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public void setSenderType(String senderType) { this.senderType = senderType; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { isRead = read; }
    public void setIsRead(boolean isRead) { this.isRead = isRead; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public void setContent(String content) { this.content = content; this.messageText = content; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; this.timestamp = createdAt; }
    
    // Setters para archivos adjuntos
    public void setHasAttachment(boolean hasAttachment) { this.hasAttachment = hasAttachment; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
    public void setAttachmentSize(Long attachmentSize) { this.attachmentSize = attachmentSize; }
}
