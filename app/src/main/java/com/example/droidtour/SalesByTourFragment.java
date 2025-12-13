package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Color;

public class SalesByTourFragment extends Fragment {
    
    private RecyclerView rvToursSales;
    private View layoutEmptyTours;
    private PieChart pieChartTours;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales_by_tour, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        loadSalesData();
        
        return view;
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
    
    private void loadSalesData() {
        // TODO: Cargar datos reales desde base de datos
        // Datos de ejemplo para el pie chart
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(45f, "Cusco Tour"));
        entries.add(new PieEntry(30f, "City Tour"));
        entries.add(new PieEntry(15f, "Nazca"));
        entries.add(new PieEntry(10f, "Paracas"));

        if (entries.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            PieDataSet set = new PieDataSet(entries, "Ventas por Tour");
            set.setColors(ColorTemplate.MATERIAL_COLORS);
            set.setValueTextColor(Color.WHITE);
            set.setValueTextSize(12f);

            PieData data = new PieData(set);
            pieChartTours.setData(data);
            pieChartTours.getDescription().setEnabled(false);
            pieChartTours.setUsePercentValues(false);
            pieChartTours.animateY(800);
            pieChartTours.invalidate();
        }
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            rvToursSales.setVisibility(View.GONE);
            layoutEmptyTours.setVisibility(View.VISIBLE);
            if (pieChartTours != null) pieChartTours.setVisibility(View.GONE);
        } else {
            rvToursSales.setVisibility(View.VISIBLE);
            layoutEmptyTours.setVisibility(View.GONE);
            if (pieChartTours != null) pieChartTours.setVisibility(View.VISIBLE);
        }
    }
}
