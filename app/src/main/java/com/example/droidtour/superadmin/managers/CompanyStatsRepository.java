package com.example.droidtour.superadmin.managers;

import android.util.Log;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.superadmin.models.CompanyStats;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository para calcular estadísticas de empresas
 */
public class CompanyStatsRepository {
    
    private static final String TAG = "CompanyStatsRepo";
    private final FirebaseFirestore db;
    
    public interface CompanyStatsCallback {
        void onSuccess(List<CompanyStats> companiesStats);
        void onError(Exception error);
    }
    
    public CompanyStatsRepository(FirebaseFirestore db) {
        this.db = db;
    }
    
    /**
     * Carga todas las empresas con sus estadísticas de reservas
     */
    public void loadCompaniesWithStats(CompanyStatsCallback callback) {
        if (db == null) {
            Log.w(TAG, "db es null");
            if (callback != null) {
                callback.onError(new IllegalStateException("FirebaseFirestore no inicializado"));
            }
            return;
        }
        
        Log.d(TAG, "Cargando empresas con estadísticas");
        
        // Primero cargar todas las empresas (sin filtro de status para incluir todas)
        db.collection(FirestoreManager.COLLECTION_COMPANIES)
                .get()
                .addOnCompleteListener(companiesTask -> {
                    if (companiesTask.isSuccessful()) {
                        QuerySnapshot companiesSnapshot = companiesTask.getResult();
                        if (companiesSnapshot != null && !companiesSnapshot.isEmpty()) {
                            Log.d(TAG, "Empresas encontradas: " + companiesSnapshot.size());
                            // Cargar reservas para calcular estadísticas
                            loadReservationsAndCalculateStats(companiesSnapshot, callback);
                        } else {
                            Log.w(TAG, "No hay empresas en la base de datos");
                            if (callback != null) {
                                callback.onSuccess(new ArrayList<>());
                            }
                        }
                    } else {
                        Log.e(TAG, "Error cargando empresas", companiesTask.getException());
                        if (callback != null) {
                            callback.onError(companiesTask.getException());
                        }
                    }
                });
    }
    
    
    private void loadReservationsAndCalculateStats(QuerySnapshot companiesSnapshot, CompanyStatsCallback callback) {
        // Cargar todas las reservas con status válido
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(reservationsTask -> {
                    if (reservationsTask.isSuccessful()) {
                        QuerySnapshot reservationsSnapshot = reservationsTask.getResult();
                        Map<String, CompanyStats> statsMap = new HashMap<>();
                        
                        // Inicializar stats para cada empresa
                        for (QueryDocumentSnapshot companyDoc : companiesSnapshot) {
                            Company company = companyDoc.toObject(Company.class);
                            if (company != null && company.getCompanyId() == null) {
                                company.setCompanyId(companyDoc.getId());
                            }
                            if (company != null) {
                                statsMap.put(company.getCompanyId(), new CompanyStats(company));
                            }
                        }
                        
                        // Calcular estadísticas desde reservas
                        if (reservationsSnapshot != null) {
                            Log.d(TAG, "Procesando " + reservationsSnapshot.size() + " reservas");
                            for (QueryDocumentSnapshot reservationDoc : reservationsSnapshot) {
                                Reservation reservation = reservationDoc.toObject(Reservation.class);
                                if (reservation != null && reservation.getCompanyId() != null) {
                                    CompanyStats stats = statsMap.get(reservation.getCompanyId());
                                    if (stats != null) {
                                        String status = reservation.getStatus();
                                        if (status != null && 
                                            (status.equals("CONFIRMADA") || status.equals("EN_CURSO") || 
                                             status.equals("COMPLETADA") || status.equals("CANCELADA"))) {
                                            stats.setTotalReservations(stats.getTotalReservations() + 1);
                                            
                                            if ("CONFIRMADA".equals(status)) {
                                                stats.setConfirmedReservations(stats.getConfirmedReservations() + 1);
                                            } else if ("EN_CURSO".equals(status)) {
                                                stats.setInProgressReservations(stats.getInProgressReservations() + 1);
                                            }
                                            
                                            // Completadas: considerar solo hasCheckedOut == true
                                            Boolean hasCheckedOut = reservation.getHasCheckedOut();
                                            if (hasCheckedOut != null && hasCheckedOut) {
                                                stats.setCompletedReservations(stats.getCompletedReservations() + 1);
                                            }
                                            
                                            if ("CANCELADA".equals(status)) {
                                                stats.setCancelledReservations(stats.getCancelledReservations() + 1);
                                            }
                                            
                                            // Sumar revenue si existe
                                            if (reservation.getTotalPrice() != null) {
                                                stats.setTotalRevenue(stats.getTotalRevenue() + reservation.getTotalPrice());
                                            }
                                            
                                            // Sumar personas atendidas
                                            if (reservation.getNumberOfPeople() != null) {
                                                stats.setTotalPeopleServed(stats.getTotalPeopleServed() + reservation.getNumberOfPeople());
                                            }
                                        }
                                    } else {
                                        Log.w(TAG, "Empresa no encontrada en statsMap para companyId: " + reservation.getCompanyId());
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "Reservas snapshot es null");
                        }
                        
                        // Calcular promedios y cargar tours
                        loadToursAndCalculateAverages(statsMap, callback);
                    } else {
                        // Fallback sin filtro de reservas
                        Log.w(TAG, "Error con whereIn, intentando sin filtro");
                        loadReservationsWithoutFilter(companiesSnapshot, callback);
                    }
                });
    }
    
    private void loadReservationsWithoutFilter(QuerySnapshot companiesSnapshot, CompanyStatsCallback callback) {
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(reservationsTask -> {
                    if (reservationsTask.isSuccessful()) {
                        QuerySnapshot reservationsSnapshot = reservationsTask.getResult();
                        Map<String, CompanyStats> statsMap = new HashMap<>();
                        
                        // Inicializar stats para cada empresa
                        for (QueryDocumentSnapshot companyDoc : companiesSnapshot) {
                            Company company = companyDoc.toObject(Company.class);
                            if (company != null && company.getCompanyId() == null) {
                                company.setCompanyId(companyDoc.getId());
                            }
                            if (company != null) {
                                statsMap.put(company.getCompanyId(), new CompanyStats(company));
                            }
                        }
                        
                        // Calcular estadísticas desde reservas
                        if (reservationsSnapshot != null) {
                            for (QueryDocumentSnapshot reservationDoc : reservationsSnapshot) {
                                Reservation reservation = reservationDoc.toObject(Reservation.class);
                                if (reservation != null && reservation.getCompanyId() != null) {
                                    CompanyStats stats = statsMap.get(reservation.getCompanyId());
                                    if (stats != null) {
                                        String status = reservation.getStatus();
                                        if (status != null && 
                                            (status.equals("CONFIRMADA") || status.equals("EN_CURSO") || 
                                             status.equals("COMPLETADA") || status.equals("CANCELADA"))) {
                                            stats.setTotalReservations(stats.getTotalReservations() + 1);
                                            
                                            if ("CONFIRMADA".equals(status)) {
                                                stats.setConfirmedReservations(stats.getConfirmedReservations() + 1);
                                            } else if ("EN_CURSO".equals(status)) {
                                                stats.setInProgressReservations(stats.getInProgressReservations() + 1);
                                            }
                                            
                                            // Completadas: considerar solo hasCheckedOut == true
                                            Boolean hasCheckedOut = reservation.getHasCheckedOut();
                                            if (hasCheckedOut != null && hasCheckedOut) {
                                                stats.setCompletedReservations(stats.getCompletedReservations() + 1);
                                            }
                                            
                                            if ("CANCELADA".equals(status)) {
                                                stats.setCancelledReservations(stats.getCancelledReservations() + 1);
                                            }
                                            
                                            if (reservation.getTotalPrice() != null) {
                                                stats.setTotalRevenue(stats.getTotalRevenue() + reservation.getTotalPrice());
                                            }
                                            
                                            if (reservation.getNumberOfPeople() != null) {
                                                stats.setTotalPeopleServed(stats.getTotalPeopleServed() + reservation.getNumberOfPeople());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Calcular promedios y cargar tours
                        loadToursAndCalculateAverages(statsMap, callback);
                    } else {
                        Log.e(TAG, "Error cargando reservas", reservationsTask.getException());
                        if (callback != null) {
                            callback.onError(reservationsTask.getException());
                        }
                    }
                });
    }
    
    /**
     * Carga tours y calcula promedios para todas las empresas
     */
    private void loadToursAndCalculateAverages(Map<String, CompanyStats> statsMap, CompanyStatsCallback callback) {
        // Cargar todos los tours
        db.collection(FirestoreManager.COLLECTION_TOURS)
                .get()
                .addOnCompleteListener(toursTask -> {
                    if (toursTask.isSuccessful() && toursTask.getResult() != null) {
                        QuerySnapshot toursSnapshot = toursTask.getResult();
                        
                        // Contar tours por empresa
                        for (QueryDocumentSnapshot tourDoc : toursSnapshot) {
                            String companyId = tourDoc.getString("companyId");
                            if (companyId != null && statsMap.containsKey(companyId)) {
                                CompanyStats stats = statsMap.get(companyId);
                                if (stats != null) {
                                    stats.setTotalTours(stats.getTotalTours() + 1);
                                    
                                    // Contar tours activos
                                    Boolean isActive = tourDoc.getBoolean("isActive");
                                    if (isActive != null && isActive) {
                                        stats.setActiveTours(stats.getActiveTours() + 1);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Calcular promedios para todas las empresas
                    for (CompanyStats stats : statsMap.values()) {
                        stats.calculateAveragePrice();
                    }
                    
                    // Convertir map a lista
                    List<CompanyStats> companiesStats = new ArrayList<>(statsMap.values());
                    Log.d(TAG, "Estadísticas completas calculadas para " + companiesStats.size() + " empresas");
                    
                    if (callback != null) {
                        callback.onSuccess(companiesStats);
                    }
                });
    }
}

