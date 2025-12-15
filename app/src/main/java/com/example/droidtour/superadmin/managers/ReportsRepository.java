package com.example.droidtour.superadmin.managers;

import android.util.Log;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;

/**
 * Repository para consultas de reportes de tours
 */
public class ReportsRepository {
    
    private static final String TAG = "ReportsRepository";
    private final FirebaseFirestore db;
    
    public interface ReportsCallback {
        void onSuccess(int totalReservations);
        void onError(Exception error);
    }
    
    public ReportsRepository(FirebaseFirestore db) {
        this.db = db;
    }
    
    /**
     * Obtiene el total de reservas con status válido
     */
    public void getTotalReservations(ReportsCallback callback) {
        if (db == null) {
            Log.w(TAG, "db es null");
            if (callback != null) {
                callback.onError(new IllegalStateException("FirebaseFirestore no inicializado"));
            }
            return;
        }
        
        Log.d(TAG, "Consultando total de reservas");
        
        // Consultar reservas con status válido
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            int total = querySnapshot.size();
                            Log.d(TAG, "Total de reservas: " + total);
                            if (callback != null) {
                                callback.onSuccess(total);
                            }
                        } else {
                            Log.w(TAG, "QuerySnapshot es null");
                            if (callback != null) {
                                callback.onError(new IllegalStateException("QuerySnapshot es null"));
                            }
                        }
                    } else {
                        // Si falla con whereIn, intentar sin filtro
                        Log.w(TAG, "Error con whereIn, intentando sin filtro", task.getException());
                        getTotalReservationsWithoutFilter(callback);
                    }
                });
    }
    
    /**
     * Fallback sin filtro whereIn
     */
    private void getTotalReservationsWithoutFilter(ReportsCallback callback) {
        if (db == null) {
            if (callback != null) {
                callback.onError(new IllegalStateException("FirebaseFirestore no inicializado"));
            }
            return;
        }
        
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            int total = querySnapshot.size();
                            Log.d(TAG, "Total de reservas (sin filtro): " + total);
                            if (callback != null) {
                                callback.onSuccess(total);
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
}

