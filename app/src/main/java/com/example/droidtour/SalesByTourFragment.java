package com.example.droidtour;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.utils.PreferencesManager;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.graphics.Color;

public class SalesByTourFragment extends Fragment {
    
    private static final String TAG = "SalesByTourFragment";
    
    private RecyclerView rvToursSales;
    private View layoutEmptyTours;
    private PieChart pieChartTours;
    
    // Period state
    private int currentPeriodType = 3; // 0=Diario, 1=Mensual, 2=Anual, 3=General
    private Date currentPeriodDate = null;
    
    // Data
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales_by_tour, container, false);
        
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
            loadSalesData();
        }
    }
    
    private void initializeViews(View view) {
        rvToursSales = view.findViewById(R.id.rv_tours_sales);
        layoutEmptyTours = view.findViewById(R.id.layout_empty_tours);
        pieChartTours = view.findViewById(R.id.pie_chart_tours);
    }
    
    private void setupRecyclerView() {
        rvToursSales.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Configurar adapter para reportes por tour
    }
    
    private void loadCompanyAndData() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadSalesData();
                } else {
                    showEmptyState(true);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
                showEmptyState(true);
            }
        });
    }
    
    private void loadSalesData() {
        if (currentCompanyId == null) {
            showEmptyState(true);
            return;
        }
        
        // Cargar reservaciones de la empresa
        firestoreManager.getReservationsByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Reservation> reservations = (List<Reservation>) result;
                
                // Filtrar reservaciones según el período
                List<Reservation> filteredReservations = filterReservationsByPeriod(reservations);
                
                // Filtrar solo reservaciones válidas para reportes (hasCheckedOut = true)
                List<Reservation> validReservations = new ArrayList<>();
                for (Reservation r : filteredReservations) {
                    if (isValidReservationForReports(r)) {
                        validReservations.add(r);
                    }
                }
                
                if (validReservations.isEmpty()) {
                    showEmptyState(true);
                    return;
                }
                
                // Agrupar ventas por tour
                processTourSales(validReservations);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando reservaciones", e);
                showEmptyState(true);
            }
        });
    }
    
    private void processTourSales(List<Reservation> reservations) {
        // Mapa para acumular ventas por tour
        Map<String, TourSalesData> tourSales = new HashMap<>();
        
        for (Reservation reservation : reservations) {
            String tourId = reservation.getTourId();
            String tourName = reservation.getTourName();
            
            if (tourId == null && tourName == null) {
                continue;
            }
            
            // Usar tourId como clave, o tourName si no hay tourId
            String key = tourId != null ? tourId : tourName;
            
            double totalPrice = reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0;
            
            TourSalesData data = tourSales.get(key);
            if (data == null) {
                data = new TourSalesData();
                data.tourId = tourId;
                data.tourName = tourName != null ? tourName : "Tour sin nombre";
                data.totalRevenue = 0;
                data.reservationCount = 0;
                tourSales.put(key, data);
            }
            
            data.totalRevenue += totalPrice;
            data.reservationCount++;
        }
        
        // Convertir a lista y ordenar por revenue (mayor a menor)
        List<TourSalesData> sortedTours = new ArrayList<>(tourSales.values());
        Collections.sort(sortedTours, new Comparator<TourSalesData>() {
            @Override
            public int compare(TourSalesData t1, TourSalesData t2) {
                return Double.compare(t2.totalRevenue, t1.totalRevenue);
            }
        });
        
        // Calcular total para porcentajes
        double totalRevenue = 0;
        for (TourSalesData data : sortedTours) {
            totalRevenue += data.totalRevenue;
        }
        
        if (totalRevenue == 0) {
            showEmptyState(true);
            return;
        }
        
        // Crear datos para el gráfico de pastel
        List<PieEntry> entries = new ArrayList<>();
        for (TourSalesData data : sortedTours) {
            float percentage = (float) ((data.totalRevenue / totalRevenue) * 100);
            entries.add(new PieEntry(percentage, data.tourName));
        }
        
        // Limitar a los primeros 8 tours para que el gráfico sea legible
        if (entries.size() > 8) {
            entries = entries.subList(0, 8);
        }
        
        displayTourChart(entries);
    }
    
    private void displayTourChart(List<PieEntry> entries) {
        if (entries.isEmpty()) {
            showEmptyState(true);
            return;
        }
        
        showEmptyState(false);
        
        PieDataSet set = new PieDataSet(entries, "Ventas por Tour");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);

        PieData data = new PieData(set);
        data.setValueFormatter(new PercentFormatter());
        pieChartTours.setData(data);
        pieChartTours.getDescription().setEnabled(false);
        pieChartTours.setUsePercentValues(true);
        pieChartTours.animateY(800);
        pieChartTours.invalidate();
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
    
    private void showEmptyState(boolean show) {
        if (show) {
            layoutEmptyTours.setVisibility(View.VISIBLE);
            if (pieChartTours != null) pieChartTours.setVisibility(View.GONE);
        } else {
            layoutEmptyTours.setVisibility(View.GONE);
            if (pieChartTours != null) pieChartTours.setVisibility(View.VISIBLE);
        }
    }
    
    // Clase auxiliar para datos de ventas por tour
    private static class TourSalesData {
        String tourId;
        String tourName;
        double totalRevenue;
        int reservationCount;
    }
}
