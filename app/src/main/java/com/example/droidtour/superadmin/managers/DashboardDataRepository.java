package com.example.droidtour.superadmin.managers;

import android.util.Log;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.superadmin.helpers.DashboardDateHelper;
import com.example.droidtour.superadmin.helpers.DashboardDataProcessor;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Repository para consultas a Firestore del dashboard
 */
public class DashboardDataRepository {
    
    private static final String TAG = "DashboardDataRepo";
    private final FirebaseFirestore db;
    
    public interface DashboardDataCallback {
        void onSuccess(Map<String, Map<String, ?>> processedData);
        void onError(Exception error);
    }
    
    public DashboardDataRepository(FirebaseFirestore db) {
        this.db = db;
    }
    
    /**
     * Carga todos los datos del dashboard para un período específico
     */
    public void loadAllDashboardDataForPeriod(int periodType, Date selectedDate, DashboardDataCallback callback) {
        if (db == null) {
            Log.w(TAG, "db es null");
            if (callback != null) callback.onError(new IllegalStateException("FirebaseFirestore no inicializado"));
            return;
        }
        
        // Calcular rango de fechas
        DashboardDateHelper.DateRange dateRange = DashboardDateHelper.calculateDateRange(periodType, selectedDate);
        if (dateRange.startDate == null || dateRange.endDate == null) {
            Log.w(TAG, "Rango de fechas inválido");
            if (callback != null) callback.onError(new IllegalArgumentException("Rango de fechas inválido"));
            return;
        }
        
        Log.d(TAG, "Cargando datos consolidados para período: " + periodType);
        
        // Consultar todas las reservas con status válido (una sola consulta)
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            // Procesar datos usando DashboardDataProcessor
                            Map<String, Map<String, ?>> processedData = DashboardDataProcessor.processReservationsData(
                                    querySnapshot, dateRange, periodType);
                            
                            if (callback != null) {
                                callback.onSuccess(processedData);
                            }
                            
                            Log.d(TAG, "Datos consolidados cargados exitosamente");
                        } else {
                            Log.w(TAG, "QuerySnapshot es null");
                            if (callback != null) {
                                callback.onError(new IllegalStateException("QuerySnapshot es null"));
                            }
                        }
                    } else {
                        // Si falla con whereIn, intentar sin filtro
                        Log.w(TAG, "Error con whereIn, intentando sin filtro", task.getException());
                        loadAllDashboardDataForPeriodWithoutFilter(periodType, selectedDate, callback);
                    }
                });
    }
    
    /**
     * Fallback sin filtro whereIn
     */
    public void loadAllDashboardDataForPeriodWithoutFilter(int periodType, Date selectedDate, DashboardDataCallback callback) {
        if (db == null) {
            if (callback != null) callback.onError(new IllegalStateException("FirebaseFirestore no inicializado"));
            return;
        }
        
        DashboardDateHelper.DateRange dateRange = DashboardDateHelper.calculateDateRange(periodType, selectedDate);
        if (dateRange.startDate == null || dateRange.endDate == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Rango de fechas inválido"));
            return;
        }
        
        // Consultar todas las reservas sin filtro
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            // Procesar datos usando DashboardDataProcessor
                            Map<String, Map<String, ?>> processedData = DashboardDataProcessor.processReservationsData(
                                    querySnapshot, dateRange, periodType);
                            
                            if (callback != null) {
                                callback.onSuccess(processedData);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError(new IllegalStateException("QuerySnapshot es null"));
                            }
                        }
                    } else {
                        Log.e(TAG, "Error cargando datos sin filtro", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
    }
    
    /**
     * Carga tours por categoría
     */
    public void loadToursByCategory(ToursByCategoryCallback callback) {
        if (db == null) {
            if (callback != null) callback.onError(new IllegalStateException("FirebaseFirestore no inicializado"));
            return;
        }
        
        db.collection(FirestoreManager.COLLECTION_TOURS)
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            java.util.Map<String, Integer> toursByCategory = new java.util.HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String category = doc.getString("category");
                                
                                if (category == null || category.isEmpty()) {
                                    category = "Sin categoría";
                                }
                                
                                toursByCategory.put(category, 
                                    toursByCategory.getOrDefault(category, 0) + 1);
                            }
                            
                            if (callback != null) {
                                callback.onSuccess(toursByCategory);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError(new IllegalStateException("QuerySnapshot es null"));
                            }
                        }
                    } else {
                        // Si falla con whereEqualTo, intentar sin filtro
                        loadToursByCategoryWithoutFilter(callback);
                    }
                });
    }
    
    /**
     * Fallback sin filtro whereEqualTo para tours
     */
    public void loadToursByCategoryWithoutFilter(ToursByCategoryCallback callback) {
        if (db == null) {
            if (callback != null) callback.onError(new IllegalStateException("FirebaseFirestore no inicializado"));
            return;
        }
        
        db.collection(FirestoreManager.COLLECTION_TOURS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            java.util.Map<String, Integer> toursByCategory = new java.util.HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                Boolean isActive = doc.getBoolean("isActive");
                                if (isActive == null || isActive) {
                                    String category = doc.getString("category");
                                    
                                    if (category == null || category.isEmpty()) {
                                        category = "Sin categoría";
                                    }
                                    
                                    toursByCategory.put(category, 
                                        toursByCategory.getOrDefault(category, 0) + 1);
                                }
                            }
                            
                            if (callback != null) {
                                callback.onSuccess(toursByCategory);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError(new IllegalStateException("QuerySnapshot es null"));
                            }
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
    }
    
    public interface ToursByCategoryCallback {
        void onSuccess(java.util.Map<String, Integer> toursByCategory);
        void onError(Exception error);
    }
}
