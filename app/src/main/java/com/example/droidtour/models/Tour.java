package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de Tour para Firebase Firestore
 * Representa tours ofrecidos por empresas turísticas
 */
public class Tour {
    @DocumentId
    private String tourId;
    
    private String tourName;
    private String companyId;
    private String companyName;
    private String description;
    private Double pricePerPerson;
    private String duration; // "4 horas", "Full Day", "2D/1N"
    private String category; // "Cultural", "Aventura", "Naturaleza", etc.
    private Integer maxGroupSize;
    private List<String> languages; // ["ES", "EN", "FR"]
    private List<String> includedServices; // ["Transporte", "Guía", "Almuerzo"]
    private List<String> includedServiceIds; // IDs de servicios de la empresa
    private String meetingPoint;
    private String departureTime;
    private List<String> imageUrls;
    private String mainImageUrl;
    
    // Campos para el itinerario
    private List<ItineraryPoint> itinerary;
    
    // Paradas del tour (ubicaciones en el mapa)
    private List<TourStop> stops;
    
    // Campos para estadísticas
    private Double averageRating;
    private Integer totalReviews;
    private Integer totalBookings;
    
    // Campos de estado
    private Boolean isActive;
    private Boolean isFeatured;
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    // Constructor vacío requerido por Firestore
    public Tour() {}

    // Constructor básico
    public Tour(String tourName, String companyId, String companyName, String description, 
                Double pricePerPerson, String duration, Integer maxGroupSize) {
        this.tourName = tourName;
        this.companyId = companyId;
        this.companyName = companyName;
        this.description = description;
        this.pricePerPerson = pricePerPerson;
        this.duration = duration;
        this.maxGroupSize = maxGroupSize;
        this.isActive = true;
        this.isFeatured = false;
        this.averageRating = 0.0;
        this.totalReviews = 0;
        this.totalBookings = 0;
    }

    // Clase interna para puntos del itinerario
    public static class ItineraryPoint {
        private String time;
        private String locationName;
        private String activityDescription;
        private String duration;

        public ItineraryPoint() {}

        public ItineraryPoint(String time, String locationName, String activityDescription, String duration) {
            this.time = time;
            this.locationName = locationName;
            this.activityDescription = activityDescription;
            this.duration = duration;
        }

        // Getters y Setters
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getLocationName() { return locationName; }
        public void setLocationName(String locationName) { this.locationName = locationName; }
        public String getActivityDescription() { return activityDescription; }
        public void setActivityDescription(String activityDescription) { this.activityDescription = activityDescription; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
    }

    // Clase interna para paradas del tour
    public static class TourStop {
        private double latitude;
        private double longitude;
        private String name;
        private int order;

        public TourStop() {}

        public TourStop(double latitude, double longitude, String name, int order) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
            this.order = order;
        }

        // Getters y Setters
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
    }

    // Convertir a Map para guardar en Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tourName", tourName);
        map.put("companyId", companyId);
        map.put("companyName", companyName);
        map.put("description", description);
        map.put("pricePerPerson", pricePerPerson);
        map.put("duration", duration);
        map.put("category", category);
        map.put("maxGroupSize", maxGroupSize);
        map.put("languages", languages);
        map.put("includedServices", includedServices);
        map.put("includedServiceIds", includedServiceIds);
        map.put("meetingPoint", meetingPoint);
        map.put("stops", stops);
        map.put("departureTime", departureTime);
        map.put("imageUrls", imageUrls);
        map.put("mainImageUrl", mainImageUrl);
        map.put("itinerary", itinerary);
        map.put("averageRating", averageRating);
        map.put("totalReviews", totalReviews);
        map.put("totalBookings", totalBookings);
        map.put("isActive", isActive);
        map.put("isFeatured", isFeatured);
        return map;
    }

    // Getters y Setters
    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }

    public String getTourName() { return tourName; }
    public void setTourName(String tourName) { this.tourName = tourName; }
    
    // Métodos alias para compatibilidad
    public String getName() { return tourName; }
    public String getImageUrl() { return mainImageUrl; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPricePerPerson() { return pricePerPerson; }
    public void setPricePerPerson(Double pricePerPerson) { this.pricePerPerson = pricePerPerson; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getMaxGroupSize() { return maxGroupSize; }
    public void setMaxGroupSize(Integer maxGroupSize) { this.maxGroupSize = maxGroupSize; }

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }

    public List<String> getIncludedServices() { return includedServices; }
    public void setIncludedServices(List<String> includedServices) { this.includedServices = includedServices; }

    public List<String> getIncludedServiceIds() { return includedServiceIds; }
    public void setIncludedServiceIds(List<String> includedServiceIds) { this.includedServiceIds = includedServiceIds; }

    public String getMeetingPoint() { return meetingPoint; }
    public void setMeetingPoint(String meetingPoint) { this.meetingPoint = meetingPoint; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }

    public List<ItineraryPoint> getItinerary() { return itinerary; }
    public void setItinerary(List<ItineraryPoint> itinerary) { this.itinerary = itinerary; }

    public List<TourStop> getStops() { return stops; }
    public void setStops(List<TourStop> stops) { this.stops = stops; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public Integer getTotalBookings() { return totalBookings; }
    public void setTotalBookings(Integer totalBookings) { this.totalBookings = totalBookings; }

    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }

    public Boolean getFeatured() { return isFeatured; }
    public void setFeatured(Boolean featured) { isFeatured = featured; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

