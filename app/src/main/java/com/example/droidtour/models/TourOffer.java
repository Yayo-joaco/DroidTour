package com.example.droidtour.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo para ofertas de tours enviadas a guías por empresas
 */
public class TourOffer {
    
    private String offerId;          // También accesible como id
    private String guideId;          // ID del guía al que se envió la oferta
    private String guideName;
    private String companyId;        // Ahora usamos agencyId para consistencia
    private String agencyId;         // ID de la empresa que envió la oferta
    private String companyName;
    private String tourId;           // ID del tour ofrecido
    private String tourName;
    private String tourDate;
    private String tourTime;
    private String tourDuration;
    private Double paymentAmount;
    private Integer numberOfParticipants;
    private String status;           // PENDIENTE, ACEPTADA, RECHAZADA
    private Date createdAt;
    private Date respondedAt;
    private String additionalNotes;
    private String notes;            // Alias para additionalNotes
    
    // Constructor vacío requerido por Firestore
    public TourOffer() {}
    
    // Constructor completo
    public TourOffer(String guideId, String guideName, String companyId, String companyName,
                     String tourId, String tourName, String tourDate, String tourTime,
                     String tourDuration, Double paymentAmount, Integer numberOfParticipants) {
        this.guideId = guideId;
        this.guideName = guideName;
        this.companyId = companyId;
        this.agencyId = companyId;  // Sincronizar agencyId con companyId
        this.companyName = companyName;
        this.tourId = tourId;
        this.tourName = tourName;
        this.tourDate = tourDate;
        this.tourTime = tourTime;
        this.tourDuration = tourDuration;
        this.paymentAmount = paymentAmount;
        this.numberOfParticipants = numberOfParticipants;
        this.status = "PENDIENTE";
        this.createdAt = new Date();
    }
    
    // Convertir a Map para Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("offerId", offerId);
        map.put("guideId", guideId);
        map.put("guideName", guideName);
        map.put("companyId", companyId != null ? companyId : agencyId); // Compatibilidad
        map.put("agencyId", agencyId != null ? agencyId : companyId);   // Compatibilidad
        map.put("companyName", companyName);
        map.put("tourId", tourId);
        map.put("tourName", tourName);
        map.put("tourDate", tourDate);
        map.put("tourTime", tourTime);
        map.put("tourDuration", tourDuration);
        map.put("paymentAmount", paymentAmount);
        map.put("numberOfParticipants", numberOfParticipants);
        map.put("status", status);
        map.put("createdAt", createdAt);
        map.put("respondedAt", respondedAt);
        map.put("additionalNotes", additionalNotes != null ? additionalNotes : notes);
        map.put("notes", notes != null ? notes : additionalNotes);
        return map;
    }
    
    // Getters y Setters
    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }
    
    // Alias para compatibilidad con código que use "id"
    public String getId() { return offerId; }
    public void setId(String id) { this.offerId = id; }
    
    public String getGuideId() { return guideId; }
    public void setGuideId(String guideId) { this.guideId = guideId; }
    
    public String getGuideName() { return guideName; }
    public void setGuideName(String guideName) { this.guideName = guideName; }
    
    public String getCompanyId() { return companyId != null ? companyId : agencyId; }
    public void setCompanyId(String companyId) { 
        this.companyId = companyId;
        if (this.agencyId == null) this.agencyId = companyId;
    }
    
    // Alias para agencyId
    public String getAgencyId() { return agencyId != null ? agencyId : companyId; }
    public void setAgencyId(String agencyId) { 
        this.agencyId = agencyId;
        if (this.companyId == null) this.companyId = agencyId;
    }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }
    
    public String getTourName() { return tourName; }
    public void setTourName(String tourName) { this.tourName = tourName; }
    
    public String getTourDate() { return tourDate; }
    public void setTourDate(String tourDate) { this.tourDate = tourDate; }
    
    public String getTourTime() { return tourTime; }
    public void setTourTime(String tourTime) { this.tourTime = tourTime; }
    
    public String getTourDuration() { return tourDuration; }
    public void setTourDuration(String tourDuration) { this.tourDuration = tourDuration; }
    
    public Double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Double paymentAmount) { this.paymentAmount = paymentAmount; }
    
    public Integer getNumberOfParticipants() { return numberOfParticipants; }
    public void setNumberOfParticipants(Integer numberOfParticipants) { 
        this.numberOfParticipants = numberOfParticipants; 
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Date respondedAt) { this.respondedAt = respondedAt; }
    
    public String getAdditionalNotes() { return additionalNotes != null ? additionalNotes : notes; }
    public void setAdditionalNotes(String additionalNotes) { 
        this.additionalNotes = additionalNotes;
        if (this.notes == null) this.notes = additionalNotes;
    }
    
    // Alias para notes
    public String getNotes() { return notes != null ? notes : additionalNotes; }
    public void setNotes(String notes) { 
        this.notes = notes;
        if (this.additionalNotes == null) this.additionalNotes = notes;
    }
}

