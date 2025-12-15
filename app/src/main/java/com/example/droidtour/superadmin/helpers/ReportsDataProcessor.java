package com.example.droidtour.superadmin.helpers;

import android.util.Log;
import com.example.droidtour.models.Reservation;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Procesador de datos para reportes de reservas
 * Agrupa reservas por día, mes o año según el período seleccionado
 */
public class ReportsDataProcessor {
    
    private static final String TAG = "ReportsDataProcessor";
    
    /**
     * Tipo de período: 0=Diario, 1=Mensual, 2=Anual
     */
    public static final int PERIOD_DAILY = 0;
    public static final int PERIOD_MONTHLY = 1;
    public static final int PERIOD_YEARLY = 2;
    
    /**
     * Procesa reservas y las agrupa según el período seleccionado
     * @param reservationsSnapshot Snapshot de reservas desde Firestore
     * @param periodType Tipo de período (0=Diario, 1=Mensual, 2=Anual)
     * @return Map con clave=período (día/mes/año), valor=total de reservas
     */
    public static Map<String, Integer> processReservationsByPeriod(
            QuerySnapshot reservationsSnapshot, int periodType) {
        
        Map<String, Integer> periodData = new HashMap<>();
        
        if (reservationsSnapshot == null || reservationsSnapshot.isEmpty()) {
            return periodData;
        }
        
        SimpleDateFormat dateFormat;
        switch (periodType) {
            case PERIOD_DAILY:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                break;
            case PERIOD_MONTHLY:
                dateFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                break;
            case PERIOD_YEARLY:
                dateFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                break;
            default:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        }
        
        for (QueryDocumentSnapshot doc : reservationsSnapshot) {
            try {
                Reservation reservation = doc.toObject(Reservation.class);
                if (reservation == null) continue;
                
                // Obtener fecha de la reserva
                Date reservationDate = getReservationDate(reservation);
                if (reservationDate == null) continue;
                
                // Formatear según período
                String periodKey = dateFormat.format(reservationDate);
                
                // Contar reservas por período
                periodData.put(periodKey, periodData.getOrDefault(periodKey, 0) + 1);
                
            } catch (Exception e) {
                Log.w(TAG, "Error procesando reserva: " + doc.getId(), e);
            }
        }
        
        return periodData;
    }
    
    /**
     * Procesa reservas agrupadas por empresa y período
     * @param reservationsSnapshot Snapshot de reservas
     * @param periodType Tipo de período
     * @return Map con clave=companyId, valor=Map<período, cantidad>
     */
    public static Map<String, Map<String, Integer>> processReservationsByCompanyAndPeriod(
            QuerySnapshot reservationsSnapshot, int periodType) {
        
        Map<String, Map<String, Integer>> companyData = new HashMap<>();
        
        if (reservationsSnapshot == null || reservationsSnapshot.isEmpty()) {
            return companyData;
        }
        
        SimpleDateFormat dateFormat;
        switch (periodType) {
            case PERIOD_DAILY:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                break;
            case PERIOD_MONTHLY:
                dateFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                break;
            case PERIOD_YEARLY:
                dateFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                break;
            default:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        }
        
        for (QueryDocumentSnapshot doc : reservationsSnapshot) {
            try {
                Reservation reservation = doc.toObject(Reservation.class);
                if (reservation == null || reservation.getCompanyId() == null) continue;
                
                String companyId = reservation.getCompanyId();
                Date reservationDate = getReservationDate(reservation);
                if (reservationDate == null) continue;
                
                String periodKey = dateFormat.format(reservationDate);
                
                // Inicializar map de empresa si no existe
                if (!companyData.containsKey(companyId)) {
                    companyData.put(companyId, new HashMap<>());
                }
                
                Map<String, Integer> companyPeriods = companyData.get(companyId);
                companyPeriods.put(periodKey, companyPeriods.getOrDefault(periodKey, 0) + 1);
                
            } catch (Exception e) {
                Log.w(TAG, "Error procesando reserva por empresa: " + doc.getId(), e);
            }
        }
        
        return companyData;
    }
    
    /**
     * Obtiene la fecha de una reserva desde diferentes campos posibles
     */
    private static Date getReservationDate(Reservation reservation) {
        // Intentar obtener fecha desde createdAt si existe
        if (reservation.getCreatedAt() != null) {
            return reservation.getCreatedAt();
        }
        
        // Intentar parsear tourDate si existe
        if (reservation.getTourDate() != null && !reservation.getTourDate().isEmpty()) {
            Date parsedDate = DashboardDateHelper.parseDateFlexible(reservation.getTourDate());
            if (parsedDate != null) {
                return parsedDate;
            }
        }
        
        // Si no hay fecha disponible, usar fecha actual como fallback
        return new Date();
    }
    
    /**
     * Genera lista de períodos ordenados según el tipo
     * @param periodType Tipo de período
     * @param dateRange Rango de fechas
     * @return Lista de períodos ordenados
     */
    public static List<String> generatePeriodsList(int periodType, DashboardDateHelper.DateRange dateRange) {
        List<String> periods = new ArrayList<>();
        
        if (dateRange == null || dateRange.startDate == null || dateRange.endDate == null) {
            return periods;
        }
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateRange.startDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        SimpleDateFormat dateFormat;
        switch (periodType) {
            case PERIOD_DAILY:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                while (!cal.getTime().after(dateRange.endDate)) {
                    periods.add(dateFormat.format(cal.getTime()));
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                break;
            case PERIOD_MONTHLY:
                dateFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                while (!cal.getTime().after(dateRange.endDate)) {
                    periods.add(dateFormat.format(cal.getTime()));
                    cal.add(Calendar.MONTH, 1);
                }
                break;
            case PERIOD_YEARLY:
                dateFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                while (!cal.getTime().after(dateRange.endDate)) {
                    periods.add(dateFormat.format(cal.getTime()));
                    cal.add(Calendar.YEAR, 1);
                }
                break;
        }
        
        return periods;
    }
    
    /**
     * Formatea una clave de período para mostrar en el gráfico
     */
    public static String formatPeriodLabel(String periodKey, int periodType) {
        try {
            SimpleDateFormat inputFormat;
            SimpleDateFormat outputFormat;
            
            switch (periodType) {
                case PERIOD_DAILY:
                    inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    outputFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                    break;
                case PERIOD_MONTHLY:
                    inputFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                    outputFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                    break;
                case PERIOD_YEARLY:
                    return periodKey; // Ya está en formato año
                default:
                    return periodKey;
            }
            
            Date date = inputFormat.parse(periodKey);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error formateando período: " + periodKey, e);
        }
        
        return periodKey;
    }
}

