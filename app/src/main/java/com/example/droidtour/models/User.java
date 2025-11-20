package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de Usuario para Firebase Firestore
 * Representa a todos los tipos de usuarios: Cliente, Guía, Admin de Empresa, Superadmin
 */
public class User {
    @DocumentId
    private String userId;
    
    // ========== DATOS COMUNES A TODOS LOS ROLES ==========
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private String countryCode;
    private String documentType; // DNI, Pasaporte, Carnet de Extranjería
    private String documentNumber;
    private String dateOfBirth;
    private String address; // Domicilio (requerido para Cliente y Guía)
    private String userType; // CLIENT, GUIDE, ADMIN, SUPERADMIN
    private String profileImageUrl;
    
    // ========== CAMPOS ESPECÍFICOS PARA GUÍA ==========
    private Boolean isGuideApproved; // Aprobado por superadmin
    private Float guideRating; // Calificación del guía
    private List<String> guideLanguages; // ["ES", "EN", "FR", "DE", "IT", "ZH", "JA"]
    private String guideSpecialties; // Especialidades del guía
    
    // ========== CAMPOS ESPECÍFICOS PARA ADMIN DE EMPRESA ==========
    private String companyId; // ID de la empresa que administra
    
    // ========== CAMPOS COMUNES DE ESTADO ==========
    private Boolean isActive; // Activado/Desactivado por superadmin
    private Boolean isEmailVerified;

    // En tu clase User, agrega:
    private String status; // "active", "inactive", "pending", "suspended"

    // Agrega estos getters y setters:
    public String getStatus() {
        return status != null ? status : "active";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhotoUrl() {
        return profileImageUrl;
    }

    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    // Constructor vacío requerido por Firestore
    public User() {}

    // Constructor completo
    public User(String email, String firstName, String lastName, String phoneNumber, 
                String documentType, String documentNumber, String dateOfBirth, 
                String address, String userType) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.phoneNumber = phoneNumber;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.userType = userType;
        this.isActive = true;
        this.isEmailVerified = false;
        
        // Si es guía, inicializar campos específicos
        if ("GUIDE".equals(userType)) {
            this.isGuideApproved = false; // Requiere aprobación
            this.guideRating = 0.0f;
        }
    }

    // Convertir a Map para guardar en Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("firstName", firstName);
        map.put("lastName", lastName);
        map.put("fullName", fullName);
        map.put("phoneNumber", phoneNumber);
        map.put("countryCode", countryCode);
        map.put("documentType", documentType);
        map.put("documentNumber", documentNumber);
        map.put("dateOfBirth", dateOfBirth);
        map.put("address", address);
        map.put("userType", userType);
        map.put("profileImageUrl", profileImageUrl);
        map.put("isActive", isActive);
        map.put("isEmailVerified", isEmailVerified);
        
        // Campos específicos para guías
        if ("GUIDE".equals(userType)) {
            map.put("isGuideApproved", isGuideApproved != null ? isGuideApproved : false);
            map.put("guideRating", guideRating != null ? guideRating : 0.0f);
            map.put("guideLanguages", guideLanguages);
            map.put("guideSpecialties", guideSpecialties);
        }
        
        // Campos específicos para admin de empresa
        if ("ADMIN".equals(userType)) {
            map.put("companyId", companyId);
        }
        
        return map;
    }

    // Getters y Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @PropertyName("phoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @PropertyName("phoneNumber")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    // Getter alternativo para leer el campo "phone" de Firestore (legacy)
    @PropertyName("phone")
    public String getPhone() {
        return phoneNumber;
    }

    @PropertyName("phone")
    public void setPhone(String phone) {
        this.phoneNumber = phone;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Boolean getEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public Boolean getGuideApproved() {
        return isGuideApproved;
    }

    public void setGuideApproved(Boolean guideApproved) {
        isGuideApproved = guideApproved;
    }

    public Float getGuideRating() {
        return guideRating;
    }

    public void setGuideRating(Float guideRating) {
        this.guideRating = guideRating;
    }

    public List<String> getGuideLanguages() {
        return guideLanguages;
    }

    public void setGuideLanguages(List<String> guideLanguages) {
        this.guideLanguages = guideLanguages;
    }

    public String getGuideSpecialties() {
        return guideSpecialties;
    }

    public void setGuideSpecialties(String guideSpecialties) {
        this.guideSpecialties = guideSpecialties;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
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
