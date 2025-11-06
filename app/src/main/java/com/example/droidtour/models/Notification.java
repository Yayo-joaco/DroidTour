package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de Notificación para Firebase Firestore
 * Representa notificaciones enviadas a los usuarios (principalmente clientes)
 */
public class Notification {
    @DocumentId
    private String notificationId;
    
    private String userId; // Usuario destinatario
    private String userType; // CLIENT, GUIDE, ADMIN, SUPERADMIN
    private String type; // Tipo de notificación
    private String title;
    private String message;
    private String imageUrl; // Imagen opcional
    
    // Datos relacionados
    private String relatedId; // ID de la reserva, tour, etc.
    private String relatedType; // "reservation", "tour", "payment", etc.
    
    // Estado
    private Boolean isRead;
    private Boolean isPushSent; // Si se envió notificación push
    @ServerTimestamp
    private Date createdAt;
    private Date readAt;
    
    // Tipos de notificación para CLIENTE
    public static final String TYPE_RESERVATION_CONFIRMED = "RESERVATION_CONFIRMED"; // Reserva confirmada
    public static final String TYPE_QR_SENT = "QR_SENT"; // QR enviado
    public static final String TYPE_PAYMENT_CHARGED = "PAYMENT_CHARGED"; // Cobro realizado
    public static final String TYPE_TOUR_REMINDER = "TOUR_REMINDER"; // Recordatorio de tour
    public static final String TYPE_TOUR_COMPLETED = "TOUR_COMPLETED"; // Tour completado
    public static final String TYPE_REVIEW_REQUEST = "REVIEW_REQUEST"; // Solicitud de reseña
    public static final String TYPE_PAYMENT_FAILED = "PAYMENT_FAILED"; // Pago fallido

    // Constructor vacío requerido por Firestore
    public Notification() {}

    // Constructor completo
    public Notification(String userId, String userType, String type, String title, String message) {
        this.userId = userId;
        this.userType = userType;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.isPushSent = false;
    }

    // Constructor con relación
    public Notification(String userId, String userType, String type, String title, String message,
                       String relatedId, String relatedType) {
        this(userId, userType, type, title, message);
        this.relatedId = relatedId;
        this.relatedType = relatedType;
    }

    // Convertir a Map para guardar en Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userType", userType);
        map.put("type", type);
        map.put("title", title);
        map.put("message", message);
        map.put("imageUrl", imageUrl);
        map.put("relatedId", relatedId);
        map.put("relatedType", relatedType);
        map.put("isRead", isRead);
        map.put("isPushSent", isPushSent);
        map.put("readAt", readAt);
        return map;
    }

    // Getters y Setters
    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }

    public String getRelatedType() {
        return relatedType;
    }

    public void setRelatedType(String relatedType) {
        this.relatedType = relatedType;
    }

    public Boolean isRead() {
        return isRead != null && isRead;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    // Alias para compatibilidad
    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Boolean getPushSent() {
        return isPushSent;
    }

    public void setPushSent(Boolean pushSent) {
        isPushSent = pushSent;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getReadAt() {
        return readAt;
    }

    public void setReadAt(Date readAt) {
        this.readAt = readAt;
    }
}

