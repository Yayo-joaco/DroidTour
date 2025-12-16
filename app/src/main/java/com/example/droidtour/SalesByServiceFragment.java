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
import com.example.droidtour.models.Service;
import com.example.droidtour.models.Tour;
import com.example.droidtour.utils.PreferencesManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import android.graphics.Color;
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

public class SalesByServiceFragment extends Fragment {
    
    private static final String TAG = "SalesByServiceFragment";
    
    private RecyclerView rvServicesSales;
    private View layoutEmptyServices;
    private BarChart barChartServices;
    
    // Period state
    private int currentPeriodType = 3; // 0=Diario, 1=Mensual, 2=Anual, 3=General
    private Date currentPeriodDate = null;
    
    // Data
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales_by_service, container, false);
        
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
        rvServicesSales = view.findViewById(R.id.rv_services_sales);
        layoutEmptyServices = view.findViewById(R.id.layout_empty_services);
        barChartServices = view.findViewById(R.id.bar_chart_services);
    }
    
    private void setupRecyclerView() {
        rvServicesSales.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Configurar adapter para reportes por servicio
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
                
                // Cargar todos los servicios de la empresa
                firestoreManager.getServicesByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        List<Service> services = (List<Service>) result;
                        Map<String, Service> serviceMap = new HashMap<>();
                        for (Service s : services) {
                            if (s.getServiceId() != null) {
                                serviceMap.put(s.getServiceId(), s);
                            }
                        }
                        
                        // Procesar ventas por servicio
                        processServiceSales(validReservations, serviceMap);
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error cargando servicios", e);
                        // Procesar sin servicios (usar nombres de includedServices)
                        processServiceSales(validReservations, new HashMap<>());
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando reservaciones", e);
                showEmptyState(true);
            }
        });
    }
    
    private void processServiceSales(List<Reservation> reservations, Map<String, Service> serviceMap) {
        // Clase auxiliar para manejar el estado compartido
        class ServiceSalesProcessor {
            final Map<String, Double> serviceSales = new HashMap<>();
            final Map<String, String> serviceNames = new HashMap<>();
            int processedCount = 0;
            final int totalReservations = reservations.size();
            
            void processReservation(Reservation reservation) {
                if (reservation.getTourId() == null) {
                    processedCount++;
                    checkIfComplete();
                    return;
                }
                
                firestoreManager.getTourById(reservation.getTourId(), new FirestoreManager.TourCallback() {
                    @Override
                    public void onSuccess(Tour tour) {
                        if (tour == null) {
                            processedCount++;
                            checkIfComplete();
                            return;
                        }
                        
                    double reservationTotal = reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0;
                    double servicePrice = reservation.getServicePrice() != null ? reservation.getServicePrice() : 0;
                    double tourBasePrice = reservation.getPricePerPerson() != null ? reservation.getPricePerPerson() : 0;
                    
                    // Obtener servicios incluidos del tour
                    List<String> includedServices = tour.getIncludedServices();
                    List<String> includedServiceIds = tour.getIncludedServiceIds();
                    
                    // Si hay serviceIds, usar esos para obtener precios reales
                    if (includedServiceIds != null && !includedServiceIds.isEmpty()) {
                        double totalServicePrice = 0;
                        Map<String, Double> servicePrices = new HashMap<>();
                        
                        for (String serviceId : includedServiceIds) {
                            Service service = serviceMap.get(serviceId);
                            if (service != null) {
                                double price = service.getPrice();
                                totalServicePrice += price;
                                servicePrices.put(serviceId, price);
                                serviceNames.put(serviceId, service.getName());
                            }
                        }
                        
                        // Distribuir el precio base del tour proporcionalmente entre servicios incluidos
                        if (totalServicePrice > 0) {
                            for (String serviceId : includedServiceIds) {
                                Double price = servicePrices.get(serviceId);
                                if (price != null) {
                                    double proportion = price / totalServicePrice;
                                    // Distribuir el precio base del tour proporcionalmente
                                    double serviceRevenue = tourBasePrice * proportion;
                                    
                                    String serviceName = serviceNames.get(serviceId);
                                    if (serviceName == null) {
                                        serviceName = "Servicio " + serviceId;
                                    }
                                    
                                    synchronized (serviceSales) {
                                        serviceSales.put(serviceId, 
                                            serviceSales.getOrDefault(serviceId, 0.0) + serviceRevenue);
                                    }
                                    serviceNames.put(serviceId, serviceName);
                                }
                            }
                        }
                        
                        // Si hay servicePrice adicional, distribuirlo también proporcionalmente
                        if (servicePrice > 0 && totalServicePrice > 0) {
                            for (String serviceId : includedServiceIds) {
                                Double price = servicePrices.get(serviceId);
                                if (price != null) {
                                    double proportion = price / totalServicePrice;
                                    double additionalRevenue = servicePrice * proportion;
                                    
                                    synchronized (serviceSales) {
                                        serviceSales.put(serviceId, 
                                            serviceSales.getOrDefault(serviceId, 0.0) + additionalRevenue);
                                    }
                                }
                            }
                        }
                    } else if (includedServices != null && !includedServices.isEmpty()) {
                        // Si solo hay nombres de servicios, distribuir equitativamente
                        int serviceCount = includedServices.size();
                        double revenuePerService = tourBasePrice / serviceCount;
                        
                        for (String serviceName : includedServices) {
                            String key = serviceName.toLowerCase();
                            synchronized (serviceSales) {
                                serviceSales.put(key, 
                                    serviceSales.getOrDefault(key, 0.0) + revenuePerService);
                            }
                            serviceNames.put(key, serviceName);
                        }
                        
                        // Distribuir servicePrice adicional equitativamente
                        if (servicePrice > 0) {
                            double additionalPerService = servicePrice / serviceCount;
                            for (String serviceName : includedServices) {
                                String key = serviceName.toLowerCase();
                                synchronized (serviceSales) {
                                    serviceSales.put(key, 
                                        serviceSales.getOrDefault(key, 0.0) + additionalPerService);
                                }
                            }
                        }
                    }
                        
                        synchronized (this) {
                            processedCount++;
                            checkIfComplete();
                        }
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error cargando tour: " + error);
                        synchronized (this) {
                            processedCount++;
                            checkIfComplete();
                        }
                    }
                });
            }
            
            synchronized void checkIfComplete() {
                if (processedCount >= totalReservations) {
                    // Todos los tours procesados, mostrar gráfico
                    displayServiceChart(serviceSales, serviceNames);
                }
            }
        }
        
        ServiceSalesProcessor processor = new ServiceSalesProcessor();
        
        // Procesar todas las reservaciones
        for (Reservation reservation : reservations) {
            processor.processReservation(reservation);
        }
    }
    
    private void displayServiceChart(Map<String, Double> serviceSales, Map<String, String> serviceNames) {
        if (serviceSales.isEmpty()) {
            showEmptyState(true);
            return;
        }
        
        // Convertir a lista y ordenar por valor (menor a mayor)
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(serviceSales.entrySet());
        Collections.sort(sortedEntries, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                return Double.compare(e1.getValue(), e2.getValue());
            }
        });
        
        // Crear datos para el gráfico
        List<String> labels = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();
        int[] colors = new int[]{
            Color.parseColor("#4CAF50"), // Verde
            Color.parseColor("#2196F3"), // Azul
            Color.parseColor("#FFC107"), // Amarillo
            Color.parseColor("#9C27B0"), // Morado
            Color.parseColor("#FF5722"), // Naranja
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#FF9800"), // Naranja oscuro
            Color.parseColor("#795548")  // Marrón
        };
        
        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<String, Double> entry = sortedEntries.get(i);
            String serviceName = serviceNames.get(entry.getKey());
            if (serviceName == null) {
                serviceName = entry.getKey();
            }
            labels.add(serviceName);
            entries.add(new BarEntry(i, entry.getValue().floatValue()));
        }
        
        showEmptyState(false);
        
        BarDataSet set = new BarDataSet(entries, "Ventas (S/)");
        if (entries.size() <= colors.length) {
            set.setColors(colors);
        } else {
            set.setColor(Color.parseColor("#2196F3"));
        }
        set.setValueTextColor(Color.BLACK);
        set.setValueTextSize(10f);

        BarData data = new BarData(set);
        data.setBarWidth(0.9f);

        barChartServices.setData(data);
        barChartServices.setFitBars(true);
        barChartServices.getDescription().setEnabled(false);
        barChartServices.getLegend().setEnabled(false);
        barChartServices.animateY(800);

        XAxis xAxis = barChartServices.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45);

        barChartServices.invalidate();
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
            layoutEmptyServices.setVisibility(View.VISIBLE);
            if (barChartServices != null) barChartServices.setVisibility(View.GONE);
        } else {
            layoutEmptyServices.setVisibility(View.GONE);
            if (barChartServices != null) barChartServices.setVisibility(View.VISIBLE);
        }
    }
}
