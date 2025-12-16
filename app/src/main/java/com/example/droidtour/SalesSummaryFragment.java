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
    private TextView tvTotalRevenue, tvTotalTours, tvTotalBookings, tvAvgTicket, tvAvgRating;
    private TextView tvGrossRevenue, tvPlatformFee, tvGuidePayments, tvGuidePaymentsKpi, tvNetRevenue;
    private RecyclerView rvTopTours;
    private LineChart lineChartTrend;
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;
    private com.example.droidtour.adapters.TopToursAdapter topToursAdapter;
    private android.view.View layoutEmptyTopTours;
    private android.view.View layoutEmptyChart;
    
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
                Log.d(TAG, "Período inicializado desde arguments - Tipo: " + currentPeriodType + 
                      ", Fecha: " + (currentPeriodDate != null ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentPeriodDate) : "null"));
            } else {
                Log.d(TAG, "No hay fecha de período en arguments, usando null");
            }
        } else {
            Log.d(TAG, "No hay arguments, usando valores por defecto");
        }
        
        initializeViews(view);
        setupRecyclerView();
        loadCompanyAndData();
        
        return view;
    }
    
    public void updatePeriod(int periodType, Date periodDate) {
        this.currentPeriodType = periodType;
        this.currentPeriodDate = periodDate;
        Log.d(TAG, "updatePeriod llamado - Tipo: " + periodType + 
              ", Fecha: " + (periodDate != null ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(periodDate) : "null") +
              ", CompanyId: " + currentCompanyId);
        if (currentCompanyId != null) {
            loadSummaryData();
        } else {
            Log.w(TAG, "updatePeriod: currentCompanyId es null, no se puede cargar datos");
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
        if (tvTotalBookings != null) tvTotalBookings.setText("0");
        if (tvAvgTicket != null) tvAvgTicket.setText("S/. 0");
        if (tvAvgRating != null) tvAvgRating.setText("0.0");
        
        // Mostrar estados vacíos
        if (layoutEmptyTopTours != null) {
            layoutEmptyTopTours.setVisibility(android.view.View.VISIBLE);
        }
        if (rvTopTours != null) {
            rvTopTours.setVisibility(android.view.View.GONE);
        }
        if (layoutEmptyChart != null) {
            layoutEmptyChart.setVisibility(android.view.View.VISIBLE);
        }
        if (lineChartTrend != null) {
            lineChartTrend.setVisibility(android.view.View.GONE);
        }
        
        // Limpiar adapter
        if (topToursAdapter != null) {
            topToursAdapter.updateData(new ArrayList<>());
        }
    }
    
    private void initializeViews(View view) {
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue);
        tvTotalTours = view.findViewById(R.id.tv_total_tours);
        tvTotalBookings = view.findViewById(R.id.tv_total_bookings);
        tvAvgTicket = view.findViewById(R.id.tv_avg_ticket);
        tvAvgRating = view.findViewById(R.id.tv_avg_rating);
        
        tvGrossRevenue = view.findViewById(R.id.tv_gross_revenue);
        tvPlatformFee = view.findViewById(R.id.tv_platform_fee);
        tvGuidePayments = view.findViewById(R.id.tv_guide_payments);
        tvGuidePaymentsKpi = view.findViewById(R.id.tv_guide_payments_kpi);
        tvNetRevenue = view.findViewById(R.id.tv_net_revenue);
        
        rvTopTours = view.findViewById(R.id.rv_top_tours);
        lineChartTrend = view.findViewById(R.id.line_chart_trend);
        layoutEmptyTopTours = view.findViewById(R.id.layout_empty_top_tours);
        layoutEmptyChart = view.findViewById(R.id.layout_empty_chart);
    }
    
    private void setupRecyclerView() {
        rvTopTours.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTopTours.setNestedScrollingEnabled(false);
        topToursAdapter = new com.example.droidtour.adapters.TopToursAdapter(new ArrayList<>());
        rvTopTours.setAdapter(topToursAdapter);
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
                if (tvTotalBookings != null) tvTotalBookings.setText(String.valueOf(validReservations));
                if (tvAvgTicket != null) tvAvgTicket.setText(String.format(Locale.getDefault(), "S/. %.0f", avgTicket));
                
                if (tvGrossRevenue != null) tvGrossRevenue.setText(String.format(Locale.US, "S/. %.2f", totalRevenue));
                if (tvPlatformFee != null) tvPlatformFee.setText(String.format(Locale.US, "- S/. %.2f", platformFee));
                if (tvGuidePayments != null) tvGuidePayments.setText(String.format(Locale.US, "- S/. %.2f", guidePayments));
                if (tvGuidePaymentsKpi != null) tvGuidePaymentsKpi.setText(String.format(Locale.US, "S/. %.2f", guidePayments));
                if (tvNetRevenue != null) tvNetRevenue.setText(String.format(Locale.US, "S/. %.2f", netRevenue));
                
                // Gráfico de tendencias con datos reales
                Log.d(TAG, "Preparando gráfico - Reservas filtradas: " + filteredReservations.size() + 
                      ", Reservas válidas: " + validReservations + 
                      ", Período tipo: " + currentPeriodType + 
                      ", Fecha período: " + (currentPeriodDate != null ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentPeriodDate) : "null"));
                setupTrendChart(filteredReservations);
                
                // Tours más vendidos
                setupTopTours(filteredReservations);
                
                // Cargar rating promedio desde reseñas
                loadAverageRating(filteredReservations);
                
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
        
        Log.d(TAG, "setupTrendChart - Total reservas recibidas: " + reservations.size() + 
              ", Reservas válidas: " + validReservations.size() + 
              ", Período tipo: " + currentPeriodType);
        
        if (validReservations.isEmpty()) {
            // Mostrar estado vacío
            if (layoutEmptyChart != null) {
                layoutEmptyChart.setVisibility(android.view.View.VISIBLE);
            }
            if (lineChartTrend != null) {
                lineChartTrend.setVisibility(android.view.View.GONE);
                lineChartTrend.setData(null);
                lineChartTrend.invalidate();
            }
            return;
        }
        
        // Ocultar estado vacío
        if (layoutEmptyChart != null) {
            layoutEmptyChart.setVisibility(android.view.View.GONE);
        }
        if (lineChartTrend != null) {
            lineChartTrend.setVisibility(android.view.View.VISIBLE);
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
                // Usar formato español para los meses
                labelFormat = new SimpleDateFormat("MMM", new Locale("es", "ES"));
                // Verificar que haya un año seleccionado
                if (currentPeriodDate != null) {
                    Calendar yearCal = Calendar.getInstance();
                    yearCal.setTime(currentPeriodDate);
                    int selectedYear = yearCal.get(Calendar.YEAR);
                    Log.d(TAG, "Gráfico anual - Año seleccionado: " + selectedYear);
                    
                    for (Reservation r : validReservations) {
                        Date date = getReservationDateForFiltering(r);
                        if (date == null) {
                            Log.w(TAG, "Reserva sin fecha válida para gráfico: " + r.getReservationId());
                            continue;
                        }
                        cal.setTime(date);
                        // Verificar que la reserva pertenezca al año seleccionado
                        if (cal.get(Calendar.YEAR) != selectedYear) {
                            Log.d(TAG, "Reserva fuera del año seleccionado - Año reserva: " + cal.get(Calendar.YEAR) + ", Año seleccionado: " + selectedYear);
                            continue;
                        }
                        // Obtener el mes como número (0-11) y mapearlo a nombre en español
                        int monthIndex = cal.get(Calendar.MONTH);
                        String[] monthNames = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
                        String monthKey = monthNames[monthIndex];
                        double revenue = r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                        dateRevenueMap.put(monthKey, dateRevenueMap.getOrDefault(monthKey, 0.0) + revenue);
                        Log.d(TAG, "Agregando al gráfico anual - Mes: " + monthKey + ", Ingreso: " + revenue + ", Año: " + cal.get(Calendar.YEAR));
                    }
                } else {
                    // Si no hay fecha de período, usar todas las reservas válidas
                    Log.w(TAG, "currentPeriodDate es null para gráfico anual, usando todas las reservas");
                    String[] monthNames = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
                    for (Reservation r : validReservations) {
                        Date date = getReservationDateForFiltering(r);
                        if (date == null) continue;
                        cal.setTime(date);
                        int monthIndex = cal.get(Calendar.MONTH);
                        String monthKey = monthNames[monthIndex];
                        double revenue = r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                        dateRevenueMap.put(monthKey, dateRevenueMap.getOrDefault(monthKey, 0.0) + revenue);
                    }
                }
                // Ordenar meses en el orden correcto
                String[] months = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
                for (String month : months) {
                    if (dateRevenueMap.containsKey(month)) {
                        dateLabels.add(month);
                    }
                }
                Log.d(TAG, "Gráfico anual - Meses con datos: " + dateLabels.size() + ", Total ingresos: " + dateRevenueMap);
                break;
                
            default: // General - agrupar por mes de los últimos 6 meses
                labelFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                Calendar sixMonthsAgo = Calendar.getInstance();
                sixMonthsAgo.add(Calendar.MONTH, -6);
                
                for (Reservation r : validReservations) {
                    Date date = getReservationDateForFiltering(r);
                    if (date == null) {
                        Log.w(TAG, "Reserva sin fecha válida para gráfico (General): " + r.getReservationId());
                        continue;
                    }
                    cal.setTime(date);
                    if (cal.before(sixMonthsAgo)) {
                        Log.d(TAG, "Reserva fuera del rango de 6 meses: " + date);
                        continue;
                    }
                    
                    String monthKey = labelFormat.format(date);
                    double revenue = r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                    dateRevenueMap.put(monthKey, dateRevenueMap.getOrDefault(monthKey, 0.0) + revenue);
                    Log.d(TAG, "Agregando al gráfico (General) - Mes: " + monthKey + ", Ingreso: " + revenue);
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
            // Mostrar estado vacío
            if (layoutEmptyChart != null) {
                layoutEmptyChart.setVisibility(android.view.View.VISIBLE);
            }
            if (lineChartTrend != null) {
                lineChartTrend.setVisibility(android.view.View.GONE);
                lineChartTrend.setData(null);
                lineChartTrend.invalidate();
            }
            return;
        }
        
        // Ocultar estado vacío
        if (layoutEmptyChart != null) {
            layoutEmptyChart.setVisibility(android.view.View.GONE);
        }
        if (lineChartTrend != null) {
            lineChartTrend.setVisibility(android.view.View.VISIBLE);
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
            // Mostrar estado vacío
            if (layoutEmptyChart != null) {
                layoutEmptyChart.setVisibility(android.view.View.VISIBLE);
            }
            if (lineChartTrend != null) {
                lineChartTrend.setVisibility(android.view.View.GONE);
                lineChartTrend.setData(null);
                lineChartTrend.invalidate();
            }
            return;
        }
        
        // Ocultar estado vacío
        if (layoutEmptyChart != null) {
            layoutEmptyChart.setVisibility(android.view.View.GONE);
        }
        if (lineChartTrend != null) {
            lineChartTrend.setVisibility(android.view.View.VISIBLE);
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
        
        Log.d(TAG, "Gráfico configurado - Entradas: " + entries.size() + ", Labels: " + dateLabels.size());
    }
    
    /**
     * Carga el rating promedio desde las reseñas de las reservas
     */
    private void loadAverageRating(List<Reservation> reservations) {
        if (currentCompanyId == null || tvAvgRating == null) {
            if (tvAvgRating != null) tvAvgRating.setText("0.0");
            return;
        }
        
        // Obtener IDs de reservas válidas
        List<String> reservationIds = new ArrayList<>();
        for (Reservation r : reservations) {
            if (isValidReservationForReports(r) && r.getReservationId() != null) {
                reservationIds.add(r.getReservationId());
            }
        }
        
        if (reservationIds.isEmpty()) {
            tvAvgRating.setText("0.0");
            return;
        }
        
        // Cargar reseñas de la empresa
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("reviews")
                .whereEqualTo("companyId", currentCompanyId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        tvAvgRating.setText("0.0");
                        return;
                    }
                    
                    double totalRating = 0;
                    int reviewCount = 0;
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        com.example.droidtour.models.Review review = doc.toObject(com.example.droidtour.models.Review.class);
                        if (review != null && review.getRating() != null) {
                            // Filtrar solo reseñas de reservas válidas del período
                            String reservationId = review.getReservationId();
                            if (reservationId != null && reservationIds.contains(reservationId)) {
                                totalRating += review.getRating();
                                reviewCount++;
                            }
                        }
                    }
                    
                    if (reviewCount > 0) {
                        double avgRating = totalRating / reviewCount;
                        tvAvgRating.setText(String.format(Locale.getDefault(), "%.1f", avgRating));
                        Log.d(TAG, "Rating promedio calculado: " + avgRating + " de " + reviewCount + " reseñas");
                    } else {
                        tvAvgRating.setText("0.0");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cargando reseñas para rating promedio", e);
                    tvAvgRating.setText("0.0");
                });
    }
    
    /**
     * Calcula y muestra los tours más vendidos
     */
    private void setupTopTours(List<Reservation> reservations) {
        if (rvTopTours == null || topToursAdapter == null) return;
        
        // Filtrar solo reservaciones válidas para reportes
        List<Reservation> validReservations = new ArrayList<>();
        for (Reservation r : reservations) {
            if (isValidReservationForReports(r)) {
                validReservations.add(r);
            }
        }
        
        if (validReservations.isEmpty()) {
            // Mostrar estado vacío
            if (layoutEmptyTopTours != null) {
                layoutEmptyTopTours.setVisibility(android.view.View.VISIBLE);
            }
            if (rvTopTours != null) {
                rvTopTours.setVisibility(android.view.View.GONE);
            }
            topToursAdapter.updateData(new ArrayList<>());
            return;
        }
        
        // Ocultar estado vacío
        if (layoutEmptyTopTours != null) {
            layoutEmptyTopTours.setVisibility(android.view.View.GONE);
        }
        if (rvTopTours != null) {
            rvTopTours.setVisibility(android.view.View.VISIBLE);
        }
        
        // Agrupar por tourId
        Map<String, TourStats> tourStatsMap = new HashMap<>();
        
        for (Reservation r : validReservations) {
            String tourId = r.getTourId();
            String tourName = r.getTourName();
            
            if (tourId == null || tourId.isEmpty()) {
                continue;
            }
            
            TourStats stats = tourStatsMap.get(tourId);
            if (stats == null) {
                stats = new TourStats(tourId, tourName != null ? tourName : "Sin nombre");
                tourStatsMap.put(tourId, stats);
            }
            
            stats.salesCount++;
            stats.totalRevenue += r.getTotalPrice() != null ? r.getTotalPrice() : 0;
        }
        
        // Convertir a lista y ordenar por cantidad de ventas (descendente)
        List<com.example.droidtour.adapters.TopToursAdapter.TopTour> topToursList = new ArrayList<>();
        for (TourStats stats : tourStatsMap.values()) {
            topToursList.add(new com.example.droidtour.adapters.TopToursAdapter.TopTour(
                stats.tourId,
                stats.tourName,
                stats.salesCount,
                stats.totalRevenue
            ));
        }
        
        // Ordenar por cantidad de ventas (descendente), luego por ingresos (descendente)
        Collections.sort(topToursList, (t1, t2) -> {
            int salesCompare = Integer.compare(t2.getSalesCount(), t1.getSalesCount());
            if (salesCompare != 0) {
                return salesCompare;
            }
            return Double.compare(t2.getTotalRevenue(), t1.getTotalRevenue());
        });
        
        // Limitar a top 10
        if (topToursList.size() > 10) {
            topToursList = topToursList.subList(0, 10);
        }
        
        // Actualizar adapter
        topToursAdapter.updateData(topToursList);
        
        Log.d(TAG, "Tours más vendidos cargados: " + topToursList.size());
    }
    
    /**
     * Clase auxiliar para estadísticas de tours
     */
    private static class TourStats {
        String tourId;
        String tourName;
        int salesCount;
        double totalRevenue;
        
        TourStats(String tourId, String tourName) {
            this.tourId = tourId;
            this.tourName = tourName;
            this.salesCount = 0;
            this.totalRevenue = 0;
        }
    }
}
