package com.example.droidtour;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.utils.PreferencesManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.graphics.Color;

public class SalesSummaryFragment extends Fragment {
    
    private static final String TAG = "SalesSummaryFragment";
    private TextView tvTotalRevenue, tvTotalTours, tvAvgTicket, tvAvgRating;
    private TextView tvGrossRevenue, tvPlatformFee, tvGuidePayments, tvNetRevenue;
    private RecyclerView rvTopTours;
    private LineChart lineChartTrend;
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;
    
    // Period state
    private int currentPeriodType = 3; // 0=Diario, 1=Mensual, 2=Anual, 3=General
    private Date currentPeriodDate = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales_summary, container, false);
        
        firestoreManager = FirestoreManager.getInstance();
        prefsManager = new PreferencesManager(requireContext());
        
        // Obtener período de los arguments si existen
        if (getArguments() != null) {
            currentPeriodType = getArguments().getInt("periodType", 3);
            long periodDateLong = getArguments().getLong("periodDate", -1);
            if (periodDateLong > 0) {
                currentPeriodDate = new Date(periodDateLong);
            }
        }
        
        initializeViews(view);
        setupRecyclerView();
        loadCompanyAndData();
        
        return view;
    }
    
    public void updatePeriod(int periodType, Date periodDate) {
        this.currentPeriodType = periodType;
        this.currentPeriodDate = periodDate;
        if (currentCompanyId != null) {
            loadSummaryData();
        }
    }
    
    private void loadCompanyAndData() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadSummaryData();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
                // Mostrar datos vacíos
                showEmptyData();
            }
        });
    }
    
    private void showEmptyData() {
        if (tvTotalRevenue != null) tvTotalRevenue.setText("S/. 0");
        if (tvTotalTours != null) tvTotalTours.setText("0");
        if (tvAvgTicket != null) tvAvgTicket.setText("S/. 0");
        if (tvAvgRating != null) tvAvgRating.setText("0.0");
    }
    
    private void initializeViews(View view) {
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue);
        tvTotalTours = view.findViewById(R.id.tv_total_tours);
        tvAvgTicket = view.findViewById(R.id.tv_avg_ticket);
        tvAvgRating = view.findViewById(R.id.tv_avg_rating);
        
        tvGrossRevenue = view.findViewById(R.id.tv_gross_revenue);
        tvPlatformFee = view.findViewById(R.id.tv_platform_fee);
        tvGuidePayments = view.findViewById(R.id.tv_guide_payments);
        tvNetRevenue = view.findViewById(R.id.tv_net_revenue);
        
        rvTopTours = view.findViewById(R.id.rv_top_tours);
        lineChartTrend = view.findViewById(R.id.line_chart_trend);
    }
    
    private void setupRecyclerView() {
        rvTopTours.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTopTours.setNestedScrollingEnabled(false);
        // TODO: Configurar adapter para tours más vendidos
    }
    
    private void loadSummaryData() {
        if (currentCompanyId == null) {
            showEmptyData();
            return;
        }
        
        // Cargar reservaciones de la empresa desde Firebase
        firestoreManager.getReservationsByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Reservation> reservations = (List<Reservation>) result;
                
                // Filtrar reservaciones según el período seleccionado
                List<Reservation> filteredReservations = filterReservationsByPeriod(reservations);
                
                double totalRevenue = 0;
                int validReservations = 0;
                
                for (Reservation r : filteredReservations) {
                    if (isValidReservationForReports(r)) {
                        totalRevenue += r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                        validReservations++;
                    }
                }
                
                double avgTicket = validReservations > 0 ? totalRevenue / validReservations : 0;
                double platformFee = totalRevenue * 0.10; // 10% comisión
                double guidePayments = totalRevenue * 0.30; // 30% para guías
                double netRevenue = totalRevenue - platformFee - guidePayments;
                
                // Actualizar UI
                if (tvTotalRevenue != null) tvTotalRevenue.setText(String.format(Locale.getDefault(), "S/. %.0f", totalRevenue));
                if (tvTotalTours != null) tvTotalTours.setText(String.valueOf(validReservations));
                if (tvAvgTicket != null) tvAvgTicket.setText(String.format(Locale.getDefault(), "S/. %.0f", avgTicket));
                if (tvAvgRating != null) tvAvgRating.setText("4.5"); // TODO: Calcular rating promedio
                
                if (tvGrossRevenue != null) tvGrossRevenue.setText(String.format(Locale.getDefault(), "S/. %.2f", totalRevenue));
                if (tvPlatformFee != null) tvPlatformFee.setText(String.format(Locale.getDefault(), "S/. %.2f", platformFee));
                if (tvGuidePayments != null) tvGuidePayments.setText(String.format(Locale.getDefault(), "S/. %.2f", guidePayments));
                if (tvNetRevenue != null) tvNetRevenue.setText(String.format(Locale.getDefault(), "S/. %.2f", netRevenue));
                
                // Gráfico de tendencias con datos reales
                setupTrendChart(filteredReservations);
                
                Log.d(TAG, "Resumen cargado: " + validReservations + " reservas, S/. " + totalRevenue);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando reservaciones", e);
                showEmptyData();
            }
        });
    }
    
    /**
     * Verifica si la reserva es válida para reportes de ventas
     * Requiere: hasCheckedOut = true (ya se cobró) y status válido (CONFIRMADA, EN_CURSO, COMPLETADA)
     */
    private boolean isValidReservationForReports(Reservation reservation) {
        if (reservation == null) return false;
        
        // Verificar que hasCheckedOut sea true (ya se realizó el pago)
        Boolean hasCheckedOut = reservation.getHasCheckedOut();
        if (hasCheckedOut == null || !hasCheckedOut) {
            return false;
        }
        
        // Verificar que el status sea válido
        String status = reservation.getStatus();
        if (status == null) return false;
        return "CONFIRMADA".equals(status) || 
               "EN_CURSO".equals(status) || 
               "COMPLETADA".equals(status);
    }
    
    /**
     * Obtiene la fecha relevante de la reserva para filtrado por período
     * Prioriza tourDate (fecha del servicio) sobre createdAt (fecha de reserva)
     */
    private Date getReservationDateForFiltering(Reservation reservation) {
        // Priorizar tourDate (fecha del servicio) sobre createdAt
        if (reservation.getTourDate() != null) {
            try {
                // Formato DD/MM/YYYY
                String[] parts = reservation.getTourDate().split("/");
                if (parts.length == 3) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[0])
                    );
                    return cal.getTime();
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parseando tourDate: " + reservation.getTourDate(), e);
            }
        }
        
        // Fallback a createdAt si no hay tourDate
        return reservation.getCreatedAt();
    }
    
    private List<Reservation> filterReservationsByPeriod(List<Reservation> reservations) {
        if (currentPeriodType == 3) {
            // General: sin filtro
            return reservations;
        }
        
        if (currentPeriodDate == null) {
            Log.w(TAG, "currentPeriodDate es null, retornando todas las reservaciones");
            return reservations;
        }
        
        Calendar periodCal = Calendar.getInstance();
        periodCal.setTime(currentPeriodDate);
        // Normalizar hora a medianoche para comparación precisa
        periodCal.set(Calendar.HOUR_OF_DAY, 0);
        periodCal.set(Calendar.MINUTE, 0);
        periodCal.set(Calendar.SECOND, 0);
        periodCal.set(Calendar.MILLISECOND, 0);
        
        Calendar reservationCal = Calendar.getInstance();
        
        List<Reservation> filtered = new ArrayList<>();
        
        Log.d(TAG, "Filtrando reservaciones. Período tipo: " + currentPeriodType + 
              ", Fecha período: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentPeriodDate) +
              ", Total reservaciones: " + reservations.size());
        
        for (Reservation r : reservations) {
            Date reservationDate = getReservationDateForFiltering(r);
            if (reservationDate == null) {
                continue;
            }
            
            reservationCal.setTime(reservationDate);
            // Normalizar hora a medianoche para comparación precisa
            reservationCal.set(Calendar.HOUR_OF_DAY, 0);
            reservationCal.set(Calendar.MINUTE, 0);
            reservationCal.set(Calendar.SECOND, 0);
            reservationCal.set(Calendar.MILLISECOND, 0);
            
            boolean matches = false;
            switch (currentPeriodType) {
                case 0: // Diario
                    matches = reservationCal.get(Calendar.YEAR) == periodCal.get(Calendar.YEAR) &&
                             reservationCal.get(Calendar.MONTH) == periodCal.get(Calendar.MONTH) &&
                             reservationCal.get(Calendar.DAY_OF_MONTH) == periodCal.get(Calendar.DAY_OF_MONTH);
                    break;
                case 1: // Mensual
                    matches = reservationCal.get(Calendar.YEAR) == periodCal.get(Calendar.YEAR) &&
                             reservationCal.get(Calendar.MONTH) == periodCal.get(Calendar.MONTH);
                    break;
                case 2: // Anual
                    matches = reservationCal.get(Calendar.YEAR) == periodCal.get(Calendar.YEAR);
                    break;
            }
            
            if (matches) {
                filtered.add(r);
            }
        }
        
        Log.d(TAG, "Reservaciones filtradas: " + filtered.size());
        return filtered;
    }
    
    private void setupTrendChart(List<Reservation> reservations) {
        if (lineChartTrend == null) return;
        
        // Filtrar solo reservaciones válidas para reportes (hasCheckedOut = true)
        List<Reservation> validReservations = new ArrayList<>();
        for (Reservation r : reservations) {
            if (isValidReservationForReports(r)) {
                validReservations.add(r);
            }
        }
        
        if (validReservations.isEmpty()) {
            // Mostrar gráfico vacío
            lineChartTrend.setData(null);
            lineChartTrend.invalidate();
            return;
        }
        
        // Agrupar reservaciones por fecha según el período
        Map<String, Double> dateRevenueMap = new HashMap<>();
        List<String> dateLabels = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat labelFormat;
        
        switch (currentPeriodType) {
            case 0: // Diario - agrupar por día de la semana
                labelFormat = new SimpleDateFormat("EEE", Locale.getDefault());
                for (Reservation r : validReservations) {
                    Date date = getReservationDateForFiltering(r);
                    if (date == null) continue;
                    cal.setTime(date);
                    String dayKey = labelFormat.format(date);
                    double revenue = r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                    dateRevenueMap.put(dayKey, dateRevenueMap.getOrDefault(dayKey, 0.0) + revenue);
                }
                // Ordenar días de la semana
                String[] weekDays = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
                for (String day : weekDays) {
                    if (dateRevenueMap.containsKey(day)) {
                        dateLabels.add(day);
                    }
                }
                break;
                
            case 1: // Mensual - agrupar por día del mes
                labelFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                for (Reservation r : validReservations) {
                    Date date = getReservationDateForFiltering(r);
                    if (date == null) continue;
                    cal.setTime(date);
                    String dayKey = labelFormat.format(date);
                    double revenue = r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                    dateRevenueMap.put(dayKey, dateRevenueMap.getOrDefault(dayKey, 0.0) + revenue);
                }
                // Ordenar por fecha
                List<String> sortedDays = new ArrayList<>(dateRevenueMap.keySet());
                Collections.sort(sortedDays, (s1, s2) -> {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                        return sdf.parse(s1).compareTo(sdf.parse(s2));
                    } catch (Exception e) {
                        return s1.compareTo(s2);
                    }
                });
                dateLabels = sortedDays;
                // Limitar a últimos 15 días para legibilidad
                if (dateLabels.size() > 15) {
                    dateLabels = dateLabels.subList(dateLabels.size() - 15, dateLabels.size());
                }
                break;
                
            case 2: // Anual - agrupar por mes
                labelFormat = new SimpleDateFormat("MMM", Locale.getDefault());
                for (Reservation r : validReservations) {
                    Date date = getReservationDateForFiltering(r);
                    if (date == null) continue;
                    cal.setTime(date);
                    String monthKey = labelFormat.format(date);
                    double revenue = r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                    dateRevenueMap.put(monthKey, dateRevenueMap.getOrDefault(monthKey, 0.0) + revenue);
                }
                // Ordenar meses
                String[] months = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
                for (String month : months) {
                    if (dateRevenueMap.containsKey(month)) {
                        dateLabels.add(month);
                    }
                }
                break;
                
            default: // General - agrupar por mes de los últimos 6 meses
                labelFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                Calendar sixMonthsAgo = Calendar.getInstance();
                sixMonthsAgo.add(Calendar.MONTH, -6);
                
                for (Reservation r : validReservations) {
                    Date date = getReservationDateForFiltering(r);
                    if (date == null) continue;
                    cal.setTime(date);
                    if (cal.before(sixMonthsAgo)) continue;
                    
                    String monthKey = labelFormat.format(date);
                    double revenue = r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                    dateRevenueMap.put(monthKey, dateRevenueMap.getOrDefault(monthKey, 0.0) + revenue);
                }
                // Ordenar por fecha
                List<String> sortedMonths = new ArrayList<>(dateRevenueMap.keySet());
                Collections.sort(sortedMonths, (s1, s2) -> {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                        return sdf.parse(s1).compareTo(sdf.parse(s2));
                    } catch (Exception e) {
                        return s1.compareTo(s2);
                    }
                });
                dateLabels = sortedMonths;
                break;
        }
        
        if (dateLabels.isEmpty()) {
            lineChartTrend.setData(null);
            lineChartTrend.invalidate();
            return;
        }
        
        // Crear entradas para el gráfico
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dateLabels.size(); i++) {
            String label = dateLabels.get(i);
            Double revenue = dateRevenueMap.get(label);
            if (revenue != null) {
                entries.add(new Entry(i, revenue.floatValue()));
            }
        }
        
        if (entries.isEmpty()) {
            lineChartTrend.setData(null);
            lineChartTrend.invalidate();
            return;
        }

        LineDataSet set = new LineDataSet(entries, "Ingresos S/");
        set.setColor(Color.parseColor("#2196F3"));
        set.setCircleColor(Color.parseColor("#2196F3"));
        set.setLineWidth(2f);
        set.setValueTextSize(10f);
        set.setDrawValues(true);

        LineData data = new LineData(set);
        lineChartTrend.setData(data);
        lineChartTrend.getDescription().setEnabled(false);
        lineChartTrend.getLegend().setEnabled(false);

        // Configurar labels del eje X
        String[] labelsArray = dateLabels.toArray(new String[0]);
        XAxis xAxis = lineChartTrend.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labelsArray));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45);

        lineChartTrend.animateY(800);
        lineChartTrend.invalidate();
    }
}
