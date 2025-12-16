package com.example.droidtour.superadmin.helpers;

import android.util.Log;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class para procesamiento y agregación de datos del dashboard
 */
public class DashboardDataProcessor {
    
    private static final String TAG = "DashboardDataProcessor";
    
    /**
     * Procesa documentos de reservas y agrupa datos por día o mes
     * @param documents Documentos de Firestore
     * @param dateRange Rango de fechas
     * @param periodType 0=Día, 1=Semana, 2=Mes, 3=Año
     * @return Map con claves: "revenue", "bookings", "people", "averagePrice"
     */
    public static Map<String, Map<String, ?>> processReservationsData(
            Iterable<QueryDocumentSnapshot> documents,
            DashboardDateHelper.DateRange dateRange,
            int periodType) {
        
        Map<String, Double> dailyRevenue = new HashMap<>();
        Map<String, Integer> dailyBookings = new HashMap<>();
        Map<String, Integer> dailyPeople = new HashMap<>();
        
        int processedDocs = 0;
        int inRangeDocs = 0;
        
        // Procesar todos los documentos
        for (QueryDocumentSnapshot doc : documents) {
            processedDocs++;
            
            // Verificar paymentStatus
            String paymentStatus = doc.getString("paymentStatus");
            if (paymentStatus == null || 
                paymentStatus.equals("CONFIRMADO") || 
                paymentStatus.equals("COBRADO")) {
                
                // Verificar hasCheckedOut
                Boolean hasCheckedOut = doc.getBoolean("hasCheckedOut");
                if (hasCheckedOut == null || !hasCheckedOut) {
                    continue; // Saltar reservas que no han sido procesadas (check-out)
                }
                
                // Obtener tourDate (puede estar en varios formatos)
                String tourDate = doc.getString("tourDate");
                if (tourDate != null && !tourDate.isEmpty()) {
                    // Intentar parsear la fecha con formato flexible
                    Date tourDateObj = DashboardDateHelper.parseDateFlexible(tourDate);
                    if (tourDateObj != null) {
                        // Comparar fechas (solo fecha, sin hora)
                        Calendar tourCal = Calendar.getInstance();
                        tourCal.setTime(tourDateObj);
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
                        
                        if (tourCal.compareTo(startCal) >= 0 && tourCal.compareTo(endCal) <= 0) {
                            inRangeDocs++;
                            
                            // Para año, agrupar por mes; para otros períodos, por día
                            String dayKey;
                            if (periodType == 3) {
                                // Agrupar por mes: "YYYY-MM"
                                SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                                dayKey = monthFormat.format(tourDateObj);
                            } else {
                                // Agrupar por día: "YYYY-MM-DD"
                                SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                dayKey = dayFormat.format(tourDateObj);
                            }
                            
                            // Procesar totalPrice (ingresos)
                            Object priceObj = doc.get("totalPrice");
                            if (priceObj != null) {
                                double price = parsePrice(priceObj);
                                dailyRevenue.put(dayKey, dailyRevenue.getOrDefault(dayKey, 0.0) + price);
                            }
                            
                            // Contar reservas
                            dailyBookings.put(dayKey, dailyBookings.getOrDefault(dayKey, 0) + 1);
                            
                            // Procesar numberOfPeople
                            Object peopleObj = doc.get("numberOfPeople");
                            if (peopleObj != null) {
                                int people = parsePeople(peopleObj);
                                dailyPeople.put(dayKey, dailyPeople.getOrDefault(dayKey, 0) + people);
                            }
                        }
                    } else {
                        // Si no se pudo parsear la fecha, loguear pero continuar
                        Log.w(TAG, "No se pudo parsear fecha: " + tourDate);
                    }
                }
            }
        }
        
        // Calcular precio promedio diario
        Map<String, Double> dailyAveragePrice = new HashMap<>();
        for (String dayKey : dailyRevenue.keySet()) {
            Double revenue = dailyRevenue.get(dayKey);
            Integer people = dailyPeople.get(dayKey);
            if (revenue != null && people != null && people > 0) {
                double averagePrice = revenue / people;
                dailyAveragePrice.put(dayKey, averagePrice);
            }
        }
        
        Log.d(TAG, "Documentos procesados: " + processedDocs + ", en rango: " + inRangeDocs);
        
        // Retornar map con todos los datos
        Map<String, Map<String, ?>> result = new HashMap<>();
        result.put("revenue", dailyRevenue);
        result.put("bookings", dailyBookings);
        result.put("people", dailyPeople);
        result.put("averagePrice", dailyAveragePrice);
        
        return result;
    }
    
    /**
     * Parsea el precio de un objeto (puede ser Double, Long, String, etc.)
     */
    private static double parsePrice(Object priceObj) {
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
    
    /**
     * Parsea el número de personas de un objeto
     */
    private static int parsePeople(Object peopleObj) {
        if (peopleObj instanceof Integer) {
            return (Integer) peopleObj;
        } else if (peopleObj instanceof Long) {
            return ((Long) peopleObj).intValue();
        } else if (peopleObj instanceof Number) {
            return ((Number) peopleObj).intValue();
        }
        return 0;
    }
}

