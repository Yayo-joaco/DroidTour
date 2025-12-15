package com.example.droidtour.superadmin.managers;

import android.util.Log;
import android.widget.TextView;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.superadmin.helpers.DashboardDateHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Manager para manejo de KPIs del dashboard
 */
public class DashboardKPIManager {
    
    private static final String TAG = "DashboardKPIManager";
    private final FirebaseFirestore db;
    
    public DashboardKPIManager(FirebaseFirestore db) {
        this.db = db;
    }
    
    /**
     * Carga el total de usuarios (excluyendo SUPERADMIN)
     */
    public void loadTotalUsers(TextView tvTotalUsers, KPICallback callback) {
        if (tvTotalUsers == null || db == null) {
            Log.w(TAG, "tvTotalUsers o db es null");
            return;
        }
        
        tvTotalUsers.setText("Cargando...");
        
        db.collection(FirestoreManager.COLLECTION_USERS)
                .whereNotEqualTo("userType", "SUPERADMIN")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            int totalUsers = 0;
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String userType = doc.getString("userType");
                                if (userType == null || !userType.equals("SUPERADMIN")) {
                                    totalUsers++;
                                }
                            }
                            
                            String formattedCount = formatNumber(totalUsers);
                            tvTotalUsers.setText(formattedCount);
                            
                            if (callback != null) callback.onComplete();
                        } else {
                            tvTotalUsers.setText("0");
                            if (callback != null) callback.onComplete();
                        }
                    } else {
                        Log.e(TAG, "Error cargando usuarios", task.getException());
                        tvTotalUsers.setText("--");
                        if (callback != null) callback.onComplete();
                    }
                });
    }
    
    /**
     * Carga tours activos
     */
    public void loadActiveTours(TextView tvActiveTours, KPICallback callback) {
        if (tvActiveTours == null || db == null) {
            Log.w(TAG, "tvActiveTours o db es null");
            return;
        }
        
        tvActiveTours.setText("Cargando...");
        
        db.collection(FirestoreManager.COLLECTION_TOURS)
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            int activeTours = querySnapshot.size();
                            tvActiveTours.setText(String.valueOf(activeTours));
                            
                            if (callback != null) callback.onComplete();
                        } else {
                            tvActiveTours.setText("0");
                            if (callback != null) callback.onComplete();
                        }
                    } else {
                        Log.e(TAG, "Error cargando tours activos", task.getException());
                        tvActiveTours.setText("--");
                        if (callback != null) callback.onComplete();
                    }
                });
    }
    
    /**
     * Carga total de reservas para un período
     */
    public void loadTotalBookings(TextView tvBookings, int periodType, Date selectedDate, KPICallback callback) {
        if (tvBookings == null || db == null) {
            Log.w(TAG, "tvBookings o db es null");
            return;
        }
        
        tvBookings.setText("Cargando...");
        
        DashboardDateHelper.DateRange dateRange = DashboardDateHelper.calculateDateRange(periodType, selectedDate);
        if (dateRange == null || dateRange.startDate == null || dateRange.endDate == null) {
            tvBookings.setText("0");
            if (callback != null) callback.onComplete();
            return;
        }
        
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            int totalBookings = 0;
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        Date tourDateObj = DashboardDateHelper.parseDateFlexible(tourDate);
                                        if (tourDateObj != null && isDateInRange(tourDateObj, dateRange)) {
                                            totalBookings++;
                                        }
                                    }
                                }
                            }
                            
                            tvBookings.setText(String.valueOf(totalBookings));
                            if (callback != null) callback.onComplete();
                        } else {
                            tvBookings.setText("0");
                            if (callback != null) callback.onComplete();
                        }
                    } else {
                        Log.w(TAG, "Error con whereIn, intentando sin filtro", task.getException());
                        loadTotalBookingsWithoutFilter(tvBookings, periodType, selectedDate, callback);
                    }
                });
    }
    
    private void loadTotalBookingsWithoutFilter(TextView tvBookings, int periodType, Date selectedDate, KPICallback callback) {
        DashboardDateHelper.DateRange dateRange = DashboardDateHelper.calculateDateRange(periodType, selectedDate);
        if (dateRange == null || dateRange.startDate == null || dateRange.endDate == null) {
            tvBookings.setText("0");
            if (callback != null) callback.onComplete();
            return;
        }
        
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            int totalBookings = 0;
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String status = doc.getString("status");
                                String paymentStatus = doc.getString("paymentStatus");
                                
                                if (status != null && 
                                    (status.equals("CONFIRMADA") || 
                                     status.equals("EN_CURSO") || 
                                     status.equals("COMPLETADA"))) {
                                    
                                    if (paymentStatus == null || 
                                        paymentStatus.equals("CONFIRMADO") || 
                                        paymentStatus.equals("COBRADO")) {
                                        
                                        String tourDate = doc.getString("tourDate");
                                        if (tourDate != null && !tourDate.isEmpty()) {
                                            Date tourDateObj = DashboardDateHelper.parseDateFlexible(tourDate);
                                            if (tourDateObj != null && isDateInRange(tourDateObj, dateRange)) {
                                                totalBookings++;
                                            }
                                        }
                                    }
                                }
                            }
                            
                            tvBookings.setText(String.valueOf(totalBookings));
                            if (callback != null) callback.onComplete();
                        } else {
                            tvBookings.setText("0");
                            if (callback != null) callback.onComplete();
                        }
                    } else {
                        Log.e(TAG, "Error cargando reservas", task.getException());
                        tvBookings.setText("--");
                        if (callback != null) callback.onComplete();
                    }
                });
    }
    
    /**
     * Carga total de ingresos para un período
     */
    public void loadTotalRevenue(TextView tvRevenue, int periodType, Date selectedDate, KPICallback callback) {
        if (tvRevenue == null || db == null) {
            Log.w(TAG, "tvRevenue o db es null");
            return;
        }
        
        tvRevenue.setText("Cargando...");
        
        DashboardDateHelper.DateRange dateRange = DashboardDateHelper.calculateDateRange(periodType, selectedDate);
        if (dateRange == null || dateRange.startDate == null || dateRange.endDate == null) {
            tvRevenue.setText("S/ 0");
            if (callback != null) callback.onComplete();
            return;
        }
        
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            double totalRevenue = 0.0;
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        Date tourDateObj = DashboardDateHelper.parseDateFlexible(tourDate);
                                        if (tourDateObj != null && isDateInRange(tourDateObj, dateRange)) {
                                            Object priceObj = doc.get("totalPrice");
                                            if (priceObj != null) {
                                                totalRevenue += parsePrice(priceObj);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            String formattedRevenue = formatCurrency(totalRevenue);
                            tvRevenue.setText(formattedRevenue);
                            if (callback != null) callback.onComplete();
                        } else {
                            tvRevenue.setText("S/ 0");
                            if (callback != null) callback.onComplete();
                        }
                    } else {
                        Log.w(TAG, "Error con whereIn, intentando sin filtro", task.getException());
                        loadTotalRevenueWithoutFilter(tvRevenue, periodType, selectedDate, callback);
                    }
                });
    }
    
    private void loadTotalRevenueWithoutFilter(TextView tvRevenue, int periodType, Date selectedDate, KPICallback callback) {
        DashboardDateHelper.DateRange dateRange = DashboardDateHelper.calculateDateRange(periodType, selectedDate);
        if (dateRange == null || dateRange.startDate == null || dateRange.endDate == null) {
            tvRevenue.setText("S/ 0");
            if (callback != null) callback.onComplete();
            return;
        }
        
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            double totalRevenue = 0.0;
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String status = doc.getString("status");
                                String paymentStatus = doc.getString("paymentStatus");
                                
                                if (status != null && 
                                    (status.equals("CONFIRMADA") || 
                                     status.equals("EN_CURSO") || 
                                     status.equals("COMPLETADA"))) {
                                    
                                    if (paymentStatus == null || 
                                        paymentStatus.equals("CONFIRMADO") || 
                                        paymentStatus.equals("COBRADO")) {
                                        
                                        String tourDate = doc.getString("tourDate");
                                        if (tourDate != null && !tourDate.isEmpty()) {
                                            Date tourDateObj = DashboardDateHelper.parseDateFlexible(tourDate);
                                            if (tourDateObj != null && isDateInRange(tourDateObj, dateRange)) {
                                                Object priceObj = doc.get("totalPrice");
                                                if (priceObj != null) {
                                                    totalRevenue += parsePrice(priceObj);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            String formattedRevenue = formatCurrency(totalRevenue);
                            tvRevenue.setText(formattedRevenue);
                            if (callback != null) callback.onComplete();
                        } else {
                            tvRevenue.setText("S/ 0");
                            if (callback != null) callback.onComplete();
                        }
                    } else {
                        Log.e(TAG, "Error cargando ingresos", task.getException());
                        tvRevenue.setText("--");
                        if (callback != null) callback.onComplete();
                    }
                });
    }
    
    // Helper methods
    
    private boolean isDateInRange(Date date, DashboardDateHelper.DateRange dateRange) {
        Calendar tourCal = Calendar.getInstance();
        tourCal.setTime(date);
        tourCal.set(Calendar.HOUR_OF_DAY, 0);
        tourCal.set(Calendar.MINUTE, 0);
        tourCal.set(Calendar.SECOND, 0);
        tourCal.set(Calendar.MILLISECOND, 0);
        
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(dateRange.startDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(dateRange.endDate);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        
        return tourCal.compareTo(startCal) >= 0 && tourCal.compareTo(endCal) <= 0;
    }
    
    private double parsePrice(Object priceObj) {
        if (priceObj instanceof Double) {
            return (Double) priceObj;
        } else if (priceObj instanceof Long) {
            return ((Long) priceObj).doubleValue();
        } else if (priceObj instanceof Number) {
            return ((Number) priceObj).doubleValue();
        } else if (priceObj instanceof String) {
            try {
                return Double.parseDouble((String) priceObj);
            } catch (NumberFormatException e) {
                Log.w(TAG, "No se pudo parsear totalPrice: " + priceObj);
            }
        }
        return 0.0;
    }
    
    private String formatNumber(int number) {
        return String.format(Locale.getDefault(), "%,d", number);
    }
    
    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "S/ %,.0f", amount);
    }
    
    public interface KPICallback {
        void onComplete();
    }
}
