package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de Reseña para Firebase Firestore
 * Representa las reseñas/valoraciones de tours realizadas por clientes
 */
public class Review {
    @DocumentId
    private String reviewId;
    
    private String userId;
    private String userName;
    private String userInitial;
    private String userProfileImageUrl;
    
    private String tourId;
    private String tourName;
    private String companyId;
    private String companyName;
    private String reservationId;
    
    // Calificaciones separadas
    private Float rating; // 1.0 a 5.0 - Rating del tour (general)
    private String reviewText; // Comentario general del tour
    private String reviewTitle;
    
    // Calificación del guía
    private String guideId;
    private String guideName;
    private Float guideRating; // 1.0 a 5.0 - Rating específico del guía
    private String guideReviewText; // Comentario específico del guía (opcional)
    
    // Calificación de la empresa
    private Float companyRating; // 1.0 a 5.0 - Rating específico de la empresa
    private String companyReviewText; // Comentario específico de la empresa (opcional)
    
    // Respuesta de la empresa (opcional)
    private String companyResponse;
    private Date companyResponseDate;
    
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    // Constructor vacío requerido por Firestore
    public Review() {}

    // Constructor completo
    public Review(String userId, String userName, String userInitial, String tourId, 
                 String tourName, String companyId, String companyName, String reservationId,
                 Float rating, String reviewText, String reviewTitle) {
        this.userId = userId;
        this.userName = userName;
        this.userInitial = userInitial;
        this.tourId = tourId;
        this.tourName = tourName;
        this.companyId = companyId;
        this.companyName = companyName;
        this.reservationId = reservationId;
        this.rating = rating;
        this.reviewText = reviewText;
        this.reviewTitle = reviewTitle;
    }

    // Convertir a Map para guardar en Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("userInitial", userInitial);
        map.put("userProfileImageUrl", userProfileImageUrl);
        map.put("tourId", tourId);
        map.put("tourName", tourName);
        map.put("companyId", companyId);
        map.put("companyName", companyName);
        map.put("reservationId", reservationId);
        map.put("rating", rating);
        map.put("reviewText", reviewText);
        map.put("reviewTitle", reviewTitle);
        map.put("guideId", guideId);
        map.put("guideName", guideName);
        map.put("guideRating", guideRating);
        map.put("guideReviewText", guideReviewText);
        map.put("companyRating", companyRating);
        map.put("companyReviewText", companyReviewText);
        map.put("companyResponse", companyResponse);
        map.put("companyResponseDate", companyResponseDate);
        return map;
    }

    // Getters y Setters
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserInitial() { return userInitial; }
    public void setUserInitial(String userInitial) { this.userInitial = userInitial; }

    public String getUserProfileImageUrl() { return userProfileImageUrl; }
    public void setUserProfileImageUrl(String userProfileImageUrl) { this.userProfileImageUrl = userProfileImageUrl; }

    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }

    public String getTourName() { return tourName; }
    public void setTourName(String tourName) { this.tourName = tourName; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public Float getRating() { return rating; }
    public void setRating(Float rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    
    // Método alias para compatibilidad
    public String getComment() { return reviewText; }
    public void setComment(String comment) { this.reviewText = comment; }

    public String getReviewTitle() { return reviewTitle; }
    public void setReviewTitle(String reviewTitle) { this.reviewTitle = reviewTitle; }

    public String getCompanyResponse() { return companyResponse; }
    public void setCompanyResponse(String companyResponse) { this.companyResponse = companyResponse; }

    public Date getCompanyResponseDate() { return companyResponseDate; }
    public void setCompanyResponseDate(Date companyResponseDate) { this.companyResponseDate = companyResponseDate; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // Getters y Setters para campos de guía
    public String getGuideId() { return guideId; }
    public void setGuideId(String guideId) { this.guideId = guideId; }

    public String getGuideName() { return guideName; }
    public void setGuideName(String guideName) { this.guideName = guideName; }

    public Float getGuideRating() { return guideRating; }
    public void setGuideRating(Float guideRating) { this.guideRating = guideRating; }

    public String getGuideReviewText() { return guideReviewText; }
    public void setGuideReviewText(String guideReviewText) { this.guideReviewText = guideReviewText; }

    // Getters y Setters para campos de empresa
    public Float getCompanyRating() { return companyRating; }
    public void setCompanyRating(Float companyRating) { this.companyRating = companyRating; }

    public String getCompanyReviewText() { return companyReviewText; }
    public void setCompanyReviewText(String companyReviewText) { this.companyReviewText = companyReviewText; }
}

