package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import java.util.List;

public class Guide {
    @DocumentId
    private String guideId; // Mismo que userId

    private List<String> languages;
    private Boolean approved; // completamente necesario para validar si ingresa o no
    private Float rating; // Promedio de calificaciones recibidas (0.0 a 5.0)
    private Integer totalReviews; // Total de reseñas recibidas
    private String specialties; //no hay campso en formulario y para esta version no usaremos :p
    private Integer yearsOfExperience; //no hay campso en formulario y para esta version no usaremos :p
    private String biography; //no hay campso en formulario y para esta version no usaremos :p

    // Constructor
    public Guide(String guideId, List<String> languages) {
        this.guideId = guideId;
        this.languages = languages;
        this.approved = false;
        this.rating = 0.0f;
        this.totalReviews = 0;
    }

    public Guide() {}


    public String getGuideId() {
        return guideId;
    }

    public void setGuideId(String guideId) {
        this.guideId = guideId;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public Float getRating() {
        return rating;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }

    public String getSpecialties() {
        return specialties;
    }

    public void setSpecialties(String specialties) {
        this.specialties = specialties;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }
}