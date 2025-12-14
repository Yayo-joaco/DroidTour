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
    
    // ========== CAMPOS DE METADATOS Y REGISTRO ==========
    private String provider; // "email", "google" - método de autenticación
    private Boolean customPhoto; // Si el usuario subió su propia foto
    private Boolean profileCompleted; // Si completó el perfil
    private Date profileCompletedAt; // Fecha de completado del perfil
    private String registeredBy; // userId del usuario que registró (para admins creados por superadmin)
    
    // ========== CAMPOS ESPECÍFICOS PARA GUÍA ==========
    private Boolean isGuideApproved; // Aprobado por superadmin
    private Float guideRating; // Calificación del guía
    private List<String> guideLanguages; // ["ES", "EN", "FR", "DE", "IT", "ZH", "JA"]
    private String guideSpecialties; // Especialidades del guía
    
    // ========== CAMPOS ESPECÍFICOS PARA ADMIN DE EMPRESA ==========
    private String companyId; // ID de la empresa que administra
    private String companyBusinessName; // Razón social de la empresa
    private String companyRuc; // RUC de la empresa
    private String companyCommercialName; // Nombre comercial de la empresa
    private String companyType; // Tipo de empresa
    
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

    /**
     * Convertir a Map para guardar en Firestore
     * 
     * NOTA: Solo guarda los campos nuevos (estándar). Los campos legacy (phone, birthDate, 
     * photoURL, displayName, languages) NO se guardan para evitar duplicación de datos.
     * 
     * Los campos legacy se mantienen solo para LECTURA desde documentos existentes en Firestore.
     * La lectura de campos legacy se maneja en FirestoreManager.mapDocumentToUser().
     * 
     * Estrategia de migración:
     * - Nuevos documentos: solo usan campos estándar
     * - Documentos legacy: se leen correctamente gracias a mapDocumentToUser()
     * - Al actualizar documentos legacy, se migran automáticamente a campos estándar
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("firstName", firstName);
        map.put("lastName", lastName);
        map.put("fullName", fullName);
        // Solo guardar phoneNumber (campo estándar), NO duplicar como "phone"
        map.put("phoneNumber", phoneNumber);
        map.put("countryCode", countryCode);
        map.put("documentType", documentType);
        map.put("documentNumber", documentNumber);
        // Solo guardar dateOfBirth (campo estándar), NO duplicar como "birthDate"
        map.put("dateOfBirth", dateOfBirth);
        map.put("address", address);
        map.put("userType", userType);
        // Solo guardar profileImageUrl (campo estándar), NO duplicar como "photoURL" o "photoUrl"
        map.put("profileImageUrl", profileImageUrl);
        map.put("isActive", isActive);
        map.put("isEmailVerified", isEmailVerified);
        if (status != null) map.put("status", status);
        
        // Campos de metadatos y registro
        if (provider != null) map.put("provider", provider);
        if (customPhoto != null) map.put("customPhoto", customPhoto);
        if (profileCompleted != null) map.put("profileCompleted", profileCompleted);
        if (profileCompletedAt != null) map.put("profileCompletedAt", profileCompletedAt);
        if (registeredBy != null) map.put("registeredBy", registeredBy);
        
        // Campos específicos para guías
        if ("GUIDE".equals(userType)) {
            map.put("isGuideApproved", isGuideApproved != null ? isGuideApproved : false);
            map.put("guideRating", guideRating != null ? guideRating : 0.0f);
            // Solo guardar guideLanguages (campo estándar), NO duplicar como "languages"
            map.put("guideLanguages", guideLanguages);
            map.put("guideSpecialties", guideSpecialties);
        }
        
        // Campos específicos para admin de empresa
        if ("ADMIN".equals(userType)) {
            if (companyId != null) map.put("companyId", companyId);
            if (companyBusinessName != null) map.put("companyBusinessName", companyBusinessName);
            if (companyRuc != null) map.put("companyRuc", companyRuc);
            if (companyCommercialName != null) map.put("companyCommercialName", companyCommercialName);
            if (companyType != null) map.put("companyType", companyType);
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

    /**
     * Nombre completo del usuario (campo estándar)
     * Para documentos legacy con "displayName", se lee en FirestoreManager.mapDocumentToUser()
     */
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    /**
     * Getter alternativo para compatibilidad con documentos legacy
     * Solo para lectura, NO se usa para escritura
     */
    public String getDisplayName() {
        return fullName;
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

    /**
     * Fecha de nacimiento (campo estándar)
     * @PropertyName("birthDate") permite leer documentos legacy que usan "birthDate"
     * pero en toMap() se guarda como "dateOfBirth" (campo estándar)
     */
    @PropertyName("birthDate")
    public String getDateOfBirth() {
        return dateOfBirth;
    }
    @PropertyName("birthDate")
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

    // Usar profileImageUrl como nombre estándar
    // Para documentos legacy con "photoURL", se manejará en el código de lectura
    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    @PropertyName("isActive")
    public Boolean getActive() {
        return isActive;
    }

    @PropertyName("isActive")
    public void setActive(Boolean active) {
        isActive = active;
    }
    
    // Getter alternativo usando el nombre del campo directamente
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @PropertyName("isEmailVerified")
    public Boolean getEmailVerified() {
        return isEmailVerified;
    }

    @PropertyName("isEmailVerified")
    public void setEmailVerified(Boolean emailVerified) {
        isEmailVerified = emailVerified;
    }
    
    // Getter alternativo usando el nombre del campo directamente
    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }
    
    public void setIsEmailVerified(Boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
    }

    @PropertyName("isGuideApproved")
    public Boolean getGuideApproved() {
        return isGuideApproved;
    }

    @PropertyName("isGuideApproved")
    public void setGuideApproved(Boolean guideApproved) {
        isGuideApproved = guideApproved;
    }
    
    // Getter alternativo usando el nombre del campo directamente
    public Boolean getIsGuideApproved() {
        return isGuideApproved;
    }
    
    public void setIsGuideApproved(Boolean isGuideApproved) {
        this.isGuideApproved = isGuideApproved;
    }

    public Float getGuideRating() {
        return guideRating;
    }

    public void setGuideRating(Float guideRating) {
        this.guideRating = guideRating;
    }

    /**
     * Idiomas que habla el guía (campo estándar)
     */
    @com.google.firebase.firestore.PropertyName("guideLanguages")
    public List<String> getGuideLanguages() {
        return guideLanguages;
    }

    @com.google.firebase.firestore.PropertyName("guideLanguages")
    public void setGuideLanguages(List<String> guideLanguages) {
        this.guideLanguages = guideLanguages;
    }
    
    /**
     * Método alternativo para compatibilidad con campo "languages" (legacy)
     * Solo para lectura desde documentos existentes, NO se usa para escritura
     */
    @com.google.firebase.firestore.PropertyName("languages")
    public List<String> getLanguages() {
        return guideLanguages;
    }
    
    @com.google.firebase.firestore.PropertyName("languages")
    public void setLanguages(List<String> languages) {
        this.guideLanguages = languages;
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

    public String getCompanyBusinessName() {
        return companyBusinessName;
    }

    public void setCompanyBusinessName(String companyBusinessName) {
        this.companyBusinessName = companyBusinessName;
    }

    public String getCompanyRuc() {
        return companyRuc;
    }

    public void setCompanyRuc(String companyRuc) {
        this.companyRuc = companyRuc;
    }

    public String getCompanyCommercialName() {
        return companyCommercialName;
    }

    public void setCompanyCommercialName(String companyCommercialName) {
        this.companyCommercialName = companyCommercialName;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Boolean getCustomPhoto() {
        return customPhoto;
    }

    public void setCustomPhoto(Boolean customPhoto) {
        this.customPhoto = customPhoto;
    }

    public Boolean getProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(Boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public Date getProfileCompletedAt() {
        return profileCompletedAt;
    }

    public void setProfileCompletedAt(Date profileCompletedAt) {
        this.profileCompletedAt = profileCompletedAt;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
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
