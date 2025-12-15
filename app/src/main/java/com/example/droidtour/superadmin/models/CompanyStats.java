package com.example.droidtour.superadmin.models;

import com.example.droidtour.models.Company;

/**
 * Modelo para estadísticas de empresa en reportes
 */
public class CompanyStats {
    private Company company;
    private int totalReservations;
    private int confirmedReservations;
    private int completedReservations;
    private int inProgressReservations;
    private int cancelledReservations;
    private double totalRevenue;
    private int totalPeopleServed;
    private double averagePricePerReservation;
    private int activeTours;
    private int totalTours;
    
    public CompanyStats(Company company) {
        this.company = company;
        this.totalReservations = 0;
        this.confirmedReservations = 0;
        this.completedReservations = 0;
        this.inProgressReservations = 0;
        this.cancelledReservations = 0;
        this.totalRevenue = 0.0;
        this.totalPeopleServed = 0;
        this.averagePricePerReservation = 0.0;
        this.activeTours = 0;
        this.totalTours = 0;
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public int getTotalReservations() {
        return totalReservations;
    }
    
    public void setTotalReservations(int totalReservations) {
        this.totalReservations = totalReservations;
    }
    
    public int getConfirmedReservations() {
        return confirmedReservations;
    }
    
    public void setConfirmedReservations(int confirmedReservations) {
        this.confirmedReservations = confirmedReservations;
    }
    
    public int getCompletedReservations() {
        return completedReservations;
    }
    
    public void setCompletedReservations(int completedReservations) {
        this.completedReservations = completedReservations;
    }
    
    public double getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    public int getInProgressReservations() {
        return inProgressReservations;
    }
    
    public void setInProgressReservations(int inProgressReservations) {
        this.inProgressReservations = inProgressReservations;
    }
    
    public int getCancelledReservations() {
        return cancelledReservations;
    }
    
    public void setCancelledReservations(int cancelledReservations) {
        this.cancelledReservations = cancelledReservations;
    }
    
    public int getTotalPeopleServed() {
        return totalPeopleServed;
    }
    
    public void setTotalPeopleServed(int totalPeopleServed) {
        this.totalPeopleServed = totalPeopleServed;
    }
    
    public double getAveragePricePerReservation() {
        return averagePricePerReservation;
    }
    
    public void setAveragePricePerReservation(double averagePricePerReservation) {
        this.averagePricePerReservation = averagePricePerReservation;
    }
    
    public int getActiveTours() {
        return activeTours;
    }
    
    public void setActiveTours(int activeTours) {
        this.activeTours = activeTours;
    }
    
    public int getTotalTours() {
        return totalTours;
    }
    
    public void setTotalTours(int totalTours) {
        this.totalTours = totalTours;
    }
    
    /**
     * Calcula el promedio de precio por reserva
     */
    public void calculateAveragePrice() {
        if (totalReservations > 0) {
            this.averagePricePerReservation = totalRevenue / totalReservations;
        }
    }
}

