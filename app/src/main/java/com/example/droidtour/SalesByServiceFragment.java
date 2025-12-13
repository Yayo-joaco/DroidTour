package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class SalesByServiceFragment extends Fragment {
    
    private RecyclerView rvServicesSales;
    private View layoutEmptyServices;
    private BarChart barChartServices;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales_by_service, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        loadSalesData();
        
        return view;
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
    
    private void loadSalesData() {
        // TODO: Cargar datos reales desde base de datos
        // Por ahora generamos datos de ejemplo para el gráfico
        List<String> labels = new ArrayList<>();
        labels.add("Guía");
        labels.add("Transporte");
        labels.add("Entrada");
        labels.add("Comida");
        labels.add("Extras");

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, 1200f));
        entries.add(new BarEntry(1f, 900f));
        entries.add(new BarEntry(2f, 600f));
        entries.add(new BarEntry(3f, 300f));
        entries.add(new BarEntry(4f, 150f));

        if (entries.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            BarDataSet set = new BarDataSet(entries, "Ventas (S/)");
            set.setColors(new int[]{Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"), Color.parseColor("#FFC107"), Color.parseColor("#9C27B0"), Color.parseColor("#FF5722")});
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
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            rvServicesSales.setVisibility(View.GONE);
            layoutEmptyServices.setVisibility(View.VISIBLE);
            if (barChartServices != null) barChartServices.setVisibility(View.GONE);
        } else {
            rvServicesSales.setVisibility(View.VISIBLE);
            layoutEmptyServices.setVisibility(View.GONE);
            if (barChartServices != null) barChartServices.setVisibility(View.VISIBLE);
        }
    }
}
