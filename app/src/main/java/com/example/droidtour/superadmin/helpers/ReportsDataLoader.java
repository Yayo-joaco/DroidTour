package com.example.droidtour.superadmin.helpers;

import android.util.Log;
import com.example.droidtour.superadmin.managers.ReportsRepository;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper para cargar datos de reportes
 */
public class ReportsDataLoader {
    
    private static final String TAG = "ReportsDataLoader";
    private final ReportsRepository repository;
    
    public interface DataLoadCallback {
        void onTotalReservationsLoaded(int total);
        void onError(Exception error);
    }
    
    public ReportsDataLoader(FirebaseFirestore db) {
        this.repository = new ReportsRepository(db);
    }
    
    /**
     * Carga el total de reservas
     */
    public void loadTotalReservations(DataLoadCallback callback) {
        Log.d(TAG, "Iniciando carga de total de reservas");
        
        repository.getTotalReservations(new ReportsRepository.ReportsCallback() {
            @Override
            public void onSuccess(int totalReservations) {
                Log.d(TAG, "Total de reservas cargado: " + totalReservations);
                if (callback != null) {
                    callback.onTotalReservationsLoaded(totalReservations);
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error cargando total de reservas", error);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
}

