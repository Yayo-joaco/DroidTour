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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales_summary, container, false);
        
        firestoreManager = FirestoreManager.getInstance();
        prefsManager = new PreferencesManager(requireContext());
        
        initializeViews(view);
        setupRecyclerView();
        loadCompanyAndData();
        
        return view;
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
                
                double totalRevenue = 0;
                int completedTours = 0;
                
                for (Reservation r : reservations) {
                    if ("COMPLETADA".equals(r.getStatus())) {
                        totalRevenue += r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                        completedTours++;
                    }
                }
                
                double avgTicket = completedTours > 0 ? totalRevenue / completedTours : 0;
                double platformFee = totalRevenue * 0.10; // 10% comisión
                double guidePayments = totalRevenue * 0.30; // 30% para guías
                double netRevenue = totalRevenue - platformFee - guidePayments;
                
                // Actualizar UI
                if (tvTotalRevenue != null) tvTotalRevenue.setText(String.format(Locale.getDefault(), "S/. %.0f", totalRevenue));
                if (tvTotalTours != null) tvTotalTours.setText(String.valueOf(completedTours));
                if (tvAvgTicket != null) tvAvgTicket.setText(String.format(Locale.getDefault(), "S/. %.0f", avgTicket));
                if (tvAvgRating != null) tvAvgRating.setText("4.5"); // TODO: Calcular rating promedio
                
                if (tvGrossRevenue != null) tvGrossRevenue.setText(String.format(Locale.getDefault(), "S/. %.2f", totalRevenue));
                if (tvPlatformFee != null) tvPlatformFee.setText(String.format(Locale.getDefault(), "S/. %.2f", platformFee));
                if (tvGuidePayments != null) tvGuidePayments.setText(String.format(Locale.getDefault(), "S/. %.2f", guidePayments));
                if (tvNetRevenue != null) tvNetRevenue.setText(String.format(Locale.getDefault(), "S/. %.2f", netRevenue));
                
                // Gráfico de tendencias (datos simulados basados en reservaciones)
                setupTrendChart(reservations);
                
                Log.d(TAG, "Resumen cargado: " + completedTours + " tours, S/. " + totalRevenue);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando reservaciones", e);
                showEmptyData();
            }
        });
    }
    
    private void setupTrendChart(List<Reservation> reservations) {
        if (lineChartTrend == null) return;
        
        // Crear datos para el gráfico (últimos 5 puntos)
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0f, 800f));
        entries.add(new Entry(1f, 950f));
        entries.add(new Entry(2f, 1200f));
        entries.add(new Entry(3f, 1000f));
        entries.add(new Entry(4f, (float) reservations.size() * 200));

        LineDataSet set = new LineDataSet(entries, "Ingresos S/");
        set.setColor(Color.parseColor("#2196F3"));
        set.setCircleColor(Color.parseColor("#2196F3"));
        set.setLineWidth(2f);
        set.setValueTextSize(10f);

        LineData data = new LineData(set);
        lineChartTrend.setData(data);
        lineChartTrend.getDescription().setEnabled(false);
        lineChartTrend.getLegend().setEnabled(false);

        String[] labels = new String[]{"Lun","Mar","Mié","Jue","Vie"};
        XAxis xAxis = lineChartTrend.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        lineChartTrend.animateY(800);
        lineChartTrend.invalidate();
    }
}
