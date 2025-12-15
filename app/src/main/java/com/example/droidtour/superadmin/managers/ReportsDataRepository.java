package com.example.droidtour.superadmin.managers;

import android.util.Log;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.superadmin.helpers.DashboardDateHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.Date;

/**
 * Repository para cargar datos de reservas para reportes
 */
public class ReportsDataRepository {
    
    private static final String TAG = "ReportsDataRepo";
    private final FirebaseFirestore db;
    
    public interface ReportsDataCallback {
        void onSuccess(QuerySnapshot reservationsSnapshot);
        void onError(Exception error);
    }
    
    public ReportsDataRepository(FirebaseFirestore db) {
        this.db = db;
    }
    
    /**
     * Carga reservas para un período específico
     * @param periodType 0=Diario, 1=Mensual, 2=Anual
     * @param selectedDate Fecha seleccionada (null para usar fecha actual)
     */
    public void loadReservationsForPeriod(int periodType, Date selectedDate, ReportsDataCallback callback) {
        if (db == null) {
            Log.w(TAG, "db es null");
            if (callback != null) {
                callback.onError(new IllegalStateException("FirebaseFirestore no inicializado"));
            }
            return;
        }
        
        // Calcular rango de fechas según período
        DashboardDateHelper.DateRange dateRange = calculateDateRangeForPeriod(periodType, selectedDate);
        
        if (dateRange.startDate == null || dateRange.endDate == null) {
            Log.w(TAG, "Rango de fechas inválido");
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Rango de fechas inválido"));
            }
            return;
        }
        
        Log.d(TAG, "Cargando reservas para período: " + periodType + 
              " desde " + dateRange.startDate + " hasta " + dateRange.endDate);
        
        // Consultar reservas con status válido en el rango de fechas
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Log.d(TAG, "Reservas cargadas: " + querySnapshot.size());
                            if (callback != null) {
                                callback.onSuccess(querySnapshot);
                            }
                        } else {
                            Log.w(TAG, "QuerySnapshot es null");
                            if (callback != null) {
                                callback.onError(new IllegalStateException("QuerySnapshot es null"));
                            }
                        }
                    } else {
                        // Fallback sin filtro
                        Log.w(TAG, "Error con whereIn, intentando sin filtro", task.getException());
                        loadReservationsWithoutFilter(callback);
                    }
                });
    }
    
    /**
     * Fallback sin filtro whereIn
     */
    private void loadReservationsWithoutFilter(ReportsDataCallback callback) {
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            if (callback != null) {
                                callback.onSuccess(querySnapshot);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError(new IllegalStateException("QuerySnapshot es null"));
                            }
                        }
                    } else {
                        Log.e(TAG, "Error cargando reservas sin filtro", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
    }
    
    /**
     * Calcula el rango de fechas según el tipo de período
     */
    private DashboardDateHelper.DateRange calculateDateRangeForPeriod(int periodType, Date selectedDate) {
        // Mapear tipos: 0=Diario (0), 1=Mensual (2), 2=Anual (3)
        int dashboardPeriodType;
        switch (periodType) {
            case 0: // Diario
                dashboardPeriodType = 0;
                break;
            case 1: // Mensual
                dashboardPeriodType = 2;
                break;
            case 2: // Anual
                dashboardPeriodType = 3;
                break;
            default:
                dashboardPeriodType = 0;
        }
        
        return DashboardDateHelper.calculateDateRange(dashboardPeriodType, selectedDate);
    }
}

