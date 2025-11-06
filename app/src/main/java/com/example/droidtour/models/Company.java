package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de Empresa Turística para Firebase Firestore
 * Representa empresas que ofrecen tours y servicios turísticos
 */
public class Company {
    @DocumentId
    private String companyId;
    
    private String companyName;
    private String description;
    private String adminUserId; // ID del usuario administrador de la empresa
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String country;
    private String logoUrl;
    private List<String> coverImageUrls;
    
    // Información legal
    private String ruc; // Registro Único de Contribuyentes (Perú)
    private String businessType; // S.A.C., E.I.R.L., etc.
    
    // Estadísticas
    private Double averageRating;
    private Integer totalReviews;
    private Integer totalTours;
    private Integer totalClients;
    private Double priceFrom; // Precio desde
    
    // Estado
    private Boolean isActive;
    private Boolean isVerified;
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    // Constructor vacío requerido por Firestore
    public Company() {}

    // Constructor básico
    public Company(String companyName, String adminUserId, String email, String phoneNumber, 
                   String city, String country) {
        this.companyName = companyName;
        this.adminUserId = adminUserId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.city = city;
        this.country = country;
        this.isActive = true;
        this.isVerified = false;
        this.averageRating = 0.0;
        this.totalReviews = 0;
        this.totalTours = 0;
        this.totalClients = 0;
        this.priceFrom = 0.0;
    }

    // Convertir a Map para guardar en Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("companyName", companyName);
        map.put("description", description);
        map.put("adminUserId", adminUserId);
        map.put("email", email);
        map.put("phoneNumber", phoneNumber);
        map.put("address", address);
        map.put("city", city);
        map.put("country", country);
        map.put("logoUrl", logoUrl);
        map.put("coverImageUrls", coverImageUrls);
        map.put("ruc", ruc);
        map.put("businessType", businessType);
        map.put("averageRating", averageRating);
        map.put("totalReviews", totalReviews);
        map.put("totalTours", totalTours);
        map.put("totalClients", totalClients);
        map.put("priceFrom", priceFrom);
        map.put("isActive", isActive);
        map.put("isVerified", isVerified);
        return map;
    }

    // Getters y Setters
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    // Método alias para compatibilidad
    public String getName() { return companyName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAdminUserId() { return adminUserId; }
    public void setAdminUserId(String adminUserId) { this.adminUserId = adminUserId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public List<String> getCoverImageUrls() { return coverImageUrls; }
    public void setCoverImageUrls(List<String> coverImageUrls) { this.coverImageUrls = coverImageUrls; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public Integer getTotalTours() { return totalTours; }
    public void setTotalTours(Integer totalTours) { this.totalTours = totalTours; }

    public Integer getTotalClients() { return totalClients; }
    public void setTotalClients(Integer totalClients) { this.totalClients = totalClients; }

    public Double getPriceFrom() { return priceFrom; }
    public void setPriceFrom(Double priceFrom) { this.priceFrom = priceFrom; }

    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }

    public Boolean getVerified() { return isVerified; }
    public void setVerified(Boolean verified) { isVerified = verified; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

