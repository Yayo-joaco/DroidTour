package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de Reserva para Firebase Firestore
 * Representa las reservas/bookings de tours realizadas por clientes
 */
public class Reservation {
    @DocumentId
    private String reservationId;
    
    private String userId;
    private String userName;
    private String userEmail;
    private String tourId;
    private String tourName;
    private String companyId;
    private String companyName;
    private String guideId;
    private String guideName;
    
    // Detalles de la reserva
    private String tourDate;
    private String tourTime;
    private Integer numberOfPeople;
    private Double pricePerPerson;
    private Double totalPrice;
    
    // QR Codes para check-in y check-out
    private String qrCodeCheckIn; // QR-Inicio: Mostrar al guía al iniciar el tour
    private String qrCodeCheckOut; // QR-Fin: Mostrar al guía al finalizar el tour
    private Boolean hasCheckedIn; // Si el cliente ya hizo check-in
    private Boolean hasCheckedOut; // Si el cliente ya hizo check-out
    private Date checkInTime;
    private Date checkOutTime;
    
    // Estado de la reserva
    private String status; // PENDIENTE, CONFIRMADA, EN_CURSO, COMPLETADA, CANCELADA
    private String paymentStatus; // PENDIENTE, CONFIRMADO, COBRADO, RECHAZADO
    private String paymentMethod; // Últimos 4 dígitos de la tarjeta (ej: "****1234")
    private String paymentMethodId; // ID del método de pago usado
    private String paymentTransactionId;
    
    // Campos adicionales del cliente
    private String specialRequests; // Solicitudes especiales del cliente
    private Boolean hasReview; // Si el cliente ya dejó una reseña
    private Boolean paymentNotificationSent; // Si se envió notificación de cobro
    
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    // Constructor vacío requerido por Firestore
    public Reservation() {}

    // Constructor completo
    public Reservation(String userId, String userName, String userEmail, String tourId, 
                      String tourName, String companyId, String companyName, String tourDate, 
                      String tourTime, Integer numberOfPeople, Double pricePerPerson) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.tourId = tourId;
        this.tourName = tourName;
        this.companyId = companyId;
        this.companyName = companyName;
        this.tourDate = tourDate;
        this.tourTime = tourTime;
        this.numberOfPeople = numberOfPeople;
        this.pricePerPerson = pricePerPerson;
        this.totalPrice = pricePerPerson * numberOfPeople;
        this.status = "PENDIENTE";
        this.paymentStatus = "PENDIENTE";
        this.hasReview = false;
        this.hasCheckedIn = false;
        this.hasCheckedOut = false;
        this.paymentNotificationSent = false;
        
        // Generar QR codes únicos
        generateQRCodes();
    }
    
    /**
     * Generar códigos QR únicos para check-in y check-out
     */
    private void generateQRCodes() {
        long timestamp = System.currentTimeMillis();
        this.qrCodeCheckIn = "CHECKIN-" + userId + "-" + timestamp;
        this.qrCodeCheckOut = "CHECKOUT-" + userId + "-" + (timestamp + 1);
    }

    // Convertir a Map para guardar en Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("userEmail", userEmail);
        map.put("tourId", tourId);
        map.put("tourName", tourName);
        map.put("companyId", companyId);
        map.put("companyName", companyName);
        map.put("guideId", guideId);
        map.put("guideName", guideName);
        map.put("tourDate", tourDate);
        map.put("tourTime", tourTime);
        map.put("numberOfPeople", numberOfPeople);
        map.put("pricePerPerson", pricePerPerson);
        map.put("totalPrice", totalPrice);
        map.put("qrCodeCheckIn", qrCodeCheckIn);
        map.put("qrCodeCheckOut", qrCodeCheckOut);
        map.put("hasCheckedIn", hasCheckedIn);
        map.put("hasCheckedOut", hasCheckedOut);
        map.put("checkInTime", checkInTime);
        map.put("checkOutTime", checkOutTime);
        map.put("status", status);
        map.put("paymentStatus", paymentStatus);
        map.put("paymentMethod", paymentMethod);
        map.put("paymentMethodId", paymentMethodId);
        map.put("paymentTransactionId", paymentTransactionId);
        map.put("specialRequests", specialRequests);
        map.put("hasReview", hasReview);
        map.put("paymentNotificationSent", paymentNotificationSent);
        return map;
    }

    // Getters y Setters
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }

    public String getTourName() { return tourName; }
    public void setTourName(String tourName) { this.tourName = tourName; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getGuideId() { return guideId; }
    public void setGuideId(String guideId) { this.guideId = guideId; }

    public String getGuideName() { return guideName; }
    public void setGuideName(String guideName) { this.guideName = guideName; }

    public String getTourDate() { return tourDate; }
    public void setTourDate(String tourDate) { this.tourDate = tourDate; }

    public String getTourTime() { return tourTime; }
    public void setTourTime(String tourTime) { this.tourTime = tourTime; }

    public Integer getNumberOfPeople() { return numberOfPeople; }
    public void setNumberOfPeople(Integer numberOfPeople) { this.numberOfPeople = numberOfPeople; }

    public Double getPricePerPerson() { return pricePerPerson; }
    public void setPricePerPerson(Double pricePerPerson) { this.pricePerPerson = pricePerPerson; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public String getQrCodeCheckIn() { return qrCodeCheckIn; }
    public void setQrCodeCheckIn(String qrCodeCheckIn) { this.qrCodeCheckIn = qrCodeCheckIn; }

    public String getQrCodeCheckOut() { return qrCodeCheckOut; }
    public void setQrCodeCheckOut(String qrCodeCheckOut) { this.qrCodeCheckOut = qrCodeCheckOut; }

    public Boolean getHasCheckedIn() { return hasCheckedIn; }
    public void setHasCheckedIn(Boolean hasCheckedIn) { this.hasCheckedIn = hasCheckedIn; }

    public Boolean getHasCheckedOut() { return hasCheckedOut; }
    public void setHasCheckedOut(Boolean hasCheckedOut) { this.hasCheckedOut = hasCheckedOut; }

    public Date getCheckInTime() { return checkInTime; }
    public void setCheckInTime(Date checkInTime) { this.checkInTime = checkInTime; }

    public Date getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(Date checkOutTime) { this.checkOutTime = checkOutTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public String getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(String paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public Boolean getHasReview() { return hasReview; }
    public void setHasReview(Boolean hasReview) { this.hasReview = hasReview; }

    public Boolean getPaymentNotificationSent() { return paymentNotificationSent; }
    public void setPaymentNotificationSent(Boolean paymentNotificationSent) { this.paymentNotificationSent = paymentNotificationSent; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

