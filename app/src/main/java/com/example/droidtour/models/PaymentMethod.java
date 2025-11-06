package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de Método de Pago para Firebase Firestore
 * Representa tarjetas de crédito/débito registradas por el cliente
 */
public class PaymentMethod {
    @DocumentId
    private String paymentMethodId;
    
    private String userId; // ID del cliente propietario
    private String cardHolderName; // Nombre en la tarjeta
    private String cardNumber; // Número de tarjeta (solo últimos 4 dígitos almacenados)
    private String cardType; // VISA, MASTERCARD, AMEX, etc.
    private String expiryMonth; // MM
    private String expiryYear; // YYYY
    private String cvv; // NO almacenar en producción - solo para demo
    private Boolean isDefault; // Tarjeta predeterminada
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    // Constructor vacío requerido por Firestore
    public PaymentMethod() {}

    // Constructor completo
    public PaymentMethod(String userId, String cardHolderName, String cardNumber, 
                        String cardType, String expiryMonth, String expiryYear) {
        this.userId = userId;
        this.cardHolderName = cardHolderName;
        // Solo guardar últimos 4 dígitos por seguridad
        this.cardNumber = maskCardNumber(cardNumber);
        this.cardType = cardType;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.isDefault = false;
    }

    /**
     * Enmascarar número de tarjeta - solo mostrar últimos 4 dígitos
     * Ej: "1234567812345678" -> "****5678"
     */
    private String maskCardNumber(String fullCardNumber) {
        if (fullCardNumber == null || fullCardNumber.length() < 4) {
            return "****";
        }
        return "****" + fullCardNumber.substring(fullCardNumber.length() - 4);
    }

    // Convertir a Map para guardar en Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("cardHolderName", cardHolderName);
        map.put("cardNumber", cardNumber); // Solo últimos 4 dígitos
        map.put("cardType", cardType);
        map.put("expiryMonth", expiryMonth);
        map.put("expiryYear", expiryYear);
        map.put("isDefault", isDefault);
        // NO incluir CVV por seguridad
        return map;
    }

    // Getters y Setters
    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(String expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(String expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public Boolean isDefault() {
        return isDefault != null && isDefault;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    // Alias para compatibilidad
    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}

