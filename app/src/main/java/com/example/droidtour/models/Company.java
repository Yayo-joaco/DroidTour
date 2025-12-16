package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Company {
    @DocumentId
    private String companyId;

    private String businessName;    // Razón social
    private String commercialName;  // Nombre comercial
    private String ruc;
    private String businessType;    // S.A.C., E.I.R.L., etc.

    private String adminUserId;     // ID del usuario administrador (referencia)
    private String email;           // Email corporativo
    private String phone;           // Teléfono corporativo
    private String address;
    private String description;     // Descripción de la empresa
    private String logoUrl;
    private List<String> coverImageUrls;
    private List<String> serviceIds; // IDs de los servicios de la empresa

    // Campos de calificación
    private Double averageRating; // Promedio de calificaciones (0.0 a 5.0)
    private Integer totalReviews; // Total de reseñas recibidas

    private String status; // active, inactive
    @ServerTimestamp private Date createdAt;
    @ServerTimestamp private Date updatedAt;

    // Constructor vacío
    public Company() {}

    // Constructor para crear empresa
    public Company(String businessName, String commercialName, String ruc,
                   String businessType, String adminUserId, String email, String phone) {
        this.businessName = businessName;
        this.commercialName = commercialName;
        this.ruc = ruc;
        this.businessType = businessType;
        this.adminUserId = adminUserId;
        this.email = email;
        this.phone = phone;
        this.status = "active";
        this.averageRating = 0.0;
        this.totalReviews = 0;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public void setCommercialName(String commercialName) {
        this.commercialName = commercialName;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(String adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public List<String> getCoverImageUrls() {
        return coverImageUrls;
    }

    public void setCoverImageUrls(List<String> coverImageUrls) {
        this.coverImageUrls = coverImageUrls;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<String> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<String> serviceIds) {
        this.serviceIds = serviceIds;
    }

    // Getters y Setters para campos de rating
    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }
}