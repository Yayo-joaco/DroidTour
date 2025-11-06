package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de Preferencias de Usuario para Firebase Firestore
 * Almacena configuraciones personalizadas del cliente
 */
public class UserPreferences {
    @DocumentId
    private String preferencesId;
    
    private String userId; // ID del usuario propietario
    
    // Configuración de Notificaciones
    private Boolean pushNotificationsEnabled; // Notificaciones Push
    private Boolean emailNotificationsEnabled; // Notificaciones Email
    private Boolean smsNotificationsEnabled; // Notificaciones SMS
    
    // Preferencias de Idioma
    private String preferredLanguage; // "ES", "EN", "FR", etc.
    
    // Preferencias de Visualización
    private String theme; // "LIGHT", "DARK", "AUTO"
    private String currency; // "PEN", "USD", "EUR"
    
    // Preferencias de Privacidad
    private Boolean shareLocationEnabled; // Compartir ubicación
    private Boolean showProfilePublicly; // Perfil público
    
    // Preferencias de Comunicación
    private Boolean allowMarketingEmails; // Recibir ofertas
    private Boolean allowNewsletters; // Recibir newsletters
    
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    // Constructor vacío requerido por Firestore
    public UserPreferences() {
        // Valores por defecto
        this.pushNotificationsEnabled = true;
        this.emailNotificationsEnabled = false;
        this.smsNotificationsEnabled = false;
        this.preferredLanguage = "ES";
        this.theme = "AUTO";
        this.currency = "PEN";
        this.shareLocationEnabled = true;
        this.showProfilePublicly = false;
        this.allowMarketingEmails = false;
        this.allowNewsletters = false;
    }

    // Constructor con userId
    public UserPreferences(String userId) {
        this();
        this.userId = userId;
    }

    // Convertir a Map para guardar en Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("pushNotificationsEnabled", pushNotificationsEnabled);
        map.put("emailNotificationsEnabled", emailNotificationsEnabled);
        map.put("smsNotificationsEnabled", smsNotificationsEnabled);
        map.put("preferredLanguage", preferredLanguage);
        map.put("theme", theme);
        map.put("currency", currency);
        map.put("shareLocationEnabled", shareLocationEnabled);
        map.put("showProfilePublicly", showProfilePublicly);
        map.put("allowMarketingEmails", allowMarketingEmails);
        map.put("allowNewsletters", allowNewsletters);
        return map;
    }

    // Getters y Setters
    public String getPreferencesId() {
        return preferencesId;
    }

    public void setPreferencesId(String preferencesId) {
        this.preferencesId = preferencesId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getPushNotificationsEnabled() {
        return pushNotificationsEnabled;
    }

    public void setPushNotificationsEnabled(Boolean pushNotificationsEnabled) {
        this.pushNotificationsEnabled = pushNotificationsEnabled;
    }

    public Boolean getEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public Boolean getSmsNotificationsEnabled() {
        return smsNotificationsEnabled;
    }

    public void setSmsNotificationsEnabled(Boolean smsNotificationsEnabled) {
        this.smsNotificationsEnabled = smsNotificationsEnabled;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getShareLocationEnabled() {
        return shareLocationEnabled;
    }

    public void setShareLocationEnabled(Boolean shareLocationEnabled) {
        this.shareLocationEnabled = shareLocationEnabled;
    }

    public Boolean getShowProfilePublicly() {
        return showProfilePublicly;
    }

    public void setShowProfilePublicly(Boolean showProfilePublicly) {
        this.showProfilePublicly = showProfilePublicly;
    }

    public Boolean getAllowMarketingEmails() {
        return allowMarketingEmails;
    }

    public void setAllowMarketingEmails(Boolean allowMarketingEmails) {
        this.allowMarketingEmails = allowMarketingEmails;
    }

    public Boolean getAllowNewsletters() {
        return allowNewsletters;
    }

    public void setAllowNewsletters(Boolean allowNewsletters) {
        this.allowNewsletters = allowNewsletters;
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

