package com.example.droidtour.superadmin.helpers;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Helper class para manejo de fechas en el dashboard
 */
public class DashboardDateHelper {
    
    private static final String TAG = "DashboardDateHelper";
    
    /**
     * Clase para almacenar rango de fechas
     */
    public static class DateRange {
        public Date startDate;
        public Date endDate;
        
        public DateRange(Date start, Date end) {
            this.startDate = start;
            this.endDate = end;
        }
    }
    
    /**
     * Extrae año-mes de una fecha: "YYYY-MM-DD" -> "YYYY-MM"
     */
    public static String extractYearMonth(String tourDate) {
        if (tourDate != null && tourDate.length() >= 7) {
            return tourDate.substring(0, 7); // "YYYY-MM"
        }
        return "";
    }
    
    /**
     * Intenta parsear una fecha en múltiples formatos posibles
     * @param dateString String con la fecha en cualquier formato
     * @return Date parseada o null si no se puede parsear
     */
    public static Date parseDateFlexible(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        
        // Lista de formatos a intentar
        String[] dateFormats = {
            "yyyy-MM-dd",                    // Formato estándar: 2024-12-15
            "dd MMMM, yyyy",                 // Español: 15 Diciembre, 2024
            "dd MMMM yyyy",                  // Español sin coma: 15 Diciembre 2024
            "dd/MM/yyyy",                    // Formato corto: 15/12/2024
            "dd-MM-yyyy",                    // Con guiones: 15-12-2024
            "yyyy/MM/dd",                    // Formato inverso: 2024/12/15
            "MMMM dd, yyyy",                 // Inglés: December 15, 2024
            "dd MMM yyyy",                   // Formato corto mes: 15 Dec 2024
            "yyyy-MM-dd HH:mm:ss",           // Con hora: 2024-12-15 10:30:00
            "yyyy-MM-dd'T'HH:mm:ss",         // ISO con T: 2024-12-15T10:30:00
        };
        
        // Intentar cada formato
        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                sdf.setLenient(false);
                Date date = sdf.parse(dateString);
                if (date != null) {
                    return date;
                }
            } catch (Exception e) {
                // Continuar con el siguiente formato
            }
        }
        
        // Si es "Mañana", calcular la fecha de mañana
        if (dateString.equalsIgnoreCase("Mañana")) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return cal.getTime();
        }
        
        // Si es "Día" o "Hoy", usar la fecha actual
        if (dateString.equalsIgnoreCase("Día") || dateString.equalsIgnoreCase("Hoy") || 
            dateString.equalsIgnoreCase("Today") || dateString.equalsIgnoreCase("Day")) {
            return Calendar.getInstance().getTime();
        }
        
        // Si es "Ayer", usar la fecha de ayer
        if (dateString.equalsIgnoreCase("Ayer") || dateString.equalsIgnoreCase("Yesterday")) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            return cal.getTime();
        }
        
        // Si no se pudo parsear, intentar con formato flexible de español
        try {
            // Intentar parsear formato español común: "15 Diciembre, 2024"
            // Mapeo de meses en español
            String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
                            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
            
            String lowerDate = dateString.toLowerCase().trim();
            for (int i = 0; i < meses.length; i++) {
                if (lowerDate.contains(meses[i])) {
                    // Extraer día y año
                    String[] parts = dateString.split(" ");
                    if (parts.length >= 3) {
                        try {
                            int day = Integer.parseInt(parts[0]);
                            int year = Integer.parseInt(parts[parts.length - 1].replace(",", ""));
                            Calendar cal = Calendar.getInstance();
                            cal.set(Calendar.YEAR, year);
                            cal.set(Calendar.MONTH, i);
                            cal.set(Calendar.DAY_OF_MONTH, day);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            cal.set(Calendar.SECOND, 0);
                            cal.set(Calendar.MILLISECOND, 0);
                            return cal.getTime();
                        } catch (NumberFormatException e) {
                            // Continuar
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorar
        }
        
        return null;
    }
    
    /**
     * Calcula el rango de fechas según el tipo de período
     * @param periodType 0=Día, 1=Semana, 2=Mes, 3=Año
     * @param selectedDate Fecha seleccionada por el usuario (puede ser null)
     * @return DateRange con startDate y endDate
     */
    public static DateRange calculateDateRange(int periodType, Date selectedDate) {
        Calendar cal = Calendar.getInstance();
        Date endDate;
        Date startDate;
        
        if (selectedDate != null) {
            // Usar fecha seleccionada
            cal.setTime(selectedDate);
        }
        
        switch (periodType) {
            case 0: // Día
                if (selectedDate != null) {
                    // Día específico seleccionado
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    startDate = cal.getTime();
                    
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTime();
                } else {
                    // Día actual por defecto
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    startDate = cal.getTime();
                    
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTime();
                }
                break;
                
            case 1: // Semana
                if (selectedDate != null) {
                    // Semana que comienza en la fecha seleccionada
                    cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    startDate = cal.getTime();
                    
                    // Último día de la semana
                    cal.add(Calendar.DAY_OF_WEEK, 6);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTime();
                } else {
                    // Últimos 7 días desde hoy
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    cal.add(Calendar.DAY_OF_YEAR, -6); // Incluir hoy, así que -6 días
                    startDate = cal.getTime();
                    
                    cal.add(Calendar.DAY_OF_YEAR, 6);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTime();
                }
                break;
                
            case 2: // Mes
                if (selectedDate != null) {
                    // Mes de la fecha seleccionada
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    startDate = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTime();
                } else {
                    // Últimos 30 días desde hoy
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    cal.add(Calendar.DAY_OF_YEAR, -29); // Incluir hoy, así que -29 días
                    startDate = cal.getTime();
                    
                    cal.add(Calendar.DAY_OF_YEAR, 29);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTime();
                }
                break;
                
            case 3: // Año
                if (selectedDate != null) {
                    // Año de la fecha seleccionada
                    cal.set(Calendar.MONTH, Calendar.JANUARY);
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    startDate = cal.getTime();
                    
                    cal.set(Calendar.MONTH, Calendar.DECEMBER);
                    cal.set(Calendar.DAY_OF_MONTH, 31);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTime();
                } else {
                    // Último año desde hoy
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    cal.add(Calendar.YEAR, -1);
                    cal.add(Calendar.DAY_OF_YEAR, 1); // Incluir hoy del año pasado
                    startDate = cal.getTime();
                    
                    cal.add(Calendar.YEAR, 1);
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTime();
                }
                break;
                
            default:
                // Por defecto, día actual
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startDate = cal.getTime();
                
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                endDate = cal.getTime();
                break;
        }
        
        return new DateRange(startDate, endDate);
    }
    
    /**
     * Genera lista de todos los días en el rango de fechas
     */
    public static List<String> generateDaysInRange(DateRange dateRange) {
        List<String> days = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateRange.startDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(dateRange.endDate);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        
        while (!cal.after(endCal)) {
            days.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        return days;
    }
    
    /**
     * Genera lista de todos los meses en el rango de fechas
     */
    public static List<String> generateMonthsInRange(DateRange dateRange) {
        List<String> months = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateRange.startDate);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(dateRange.endDate);
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        
        while (!cal.after(endCal)) {
            months.add(sdf.format(cal.getTime()));
            cal.add(Calendar.MONTH, 1);
        }
        
        return months;
    }
    
    /**
     * Formatea etiqueta de día: "YYYY-MM-DD" -> "DD/MM"
     */
    public static String formatDayLabel(String dayKey) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dayKey);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error formateando día: " + dayKey, e);
        }
        return dayKey;
    }
    
    /**
     * Formatea etiqueta de mes: "YYYY-MM" -> "MMM yyyy"
     */
    public static String formatMonthLabel(String yearMonth) {
        try {
            String[] parts = yearMonth.split("-");
            if (parts.length == 2) {
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[0]);
                
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month - 1); // Calendar.MONTH es 0-based
                
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                return sdf.format(cal.getTime());
            }
        } catch (Exception e) {
            Log.w(TAG, "Error formateando mes: " + yearMonth, e);
        }
        return yearMonth;
    }
}
