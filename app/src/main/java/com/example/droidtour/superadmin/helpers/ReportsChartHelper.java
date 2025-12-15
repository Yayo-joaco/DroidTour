package com.example.droidtour.superadmin.helpers;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import com.example.droidtour.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper para configurar gráficos de reportes de reservas
 */
public class ReportsChartHelper {
    
    private static final String TAG = "ReportsChartHelper";
    private final Context context;
    
    public ReportsChartHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Actualiza el gráfico de barras con datos de reservas por período
     * @param barChart Gráfico de barras
     * @param periodData Map con período como clave y cantidad como valor
     * @param periodType Tipo de período (0=Diario, 1=Mensual, 2=Anual)
     */
    public void updateBarChartWithPeriodData(BarChart barChart, Map<String, Integer> periodData, int periodType) {
        if (barChart == null) {
            Log.w(TAG, "barChart es null");
            return;
        }
        
        if (periodData == null || periodData.isEmpty()) {
            updateBarChartWithEmptyData(barChart);
            return;
        }
        
        // Ordenar períodos
        TreeMap<String, Integer> sortedData = new TreeMap<>(periodData);
        List<String> periods = new ArrayList<>(sortedData.keySet());
        
        // Crear entradas para el gráfico
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < periods.size(); i++) {
            String periodKey = periods.get(i);
            Integer count = sortedData.get(periodKey);
            entries.add(new BarEntry(i, count != null ? count.floatValue() : 0f));
            labels.add(ReportsDataProcessor.formatPeriodLabel(periodKey, periodType));
        }
        
        if (entries.isEmpty()) {
            updateBarChartWithEmptyData(barChart);
            return;
        }
        
        // Configurar dataset
        BarDataSet dataSet = new BarDataSet(entries, "Reservas");
        dataSet.setColor(context.getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawValues(true);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);
        
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setFitBars(true);
        
        // Configurar eje X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setLabelCount(Math.min(labels.size(), 10)); // Limitar etiquetas visibles
        
        // Configurar eje Y
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);
        
        barChart.animateY(800);
        barChart.invalidate();
    }
    
    /**
     * Actualiza el gráfico con datos vacíos
     */
    private void updateBarChartWithEmptyData(BarChart barChart) {
        barChart.clear();
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.invalidate();
    }
    
    /**
     * Verifica si el gráfico tiene datos
     */
    public boolean hasChartData(BarChart barChart) {
        if (barChart == null) return false;
        com.github.mikephil.charting.data.BarData data = barChart.getData();
        return data != null && data.getEntryCount() > 0;
    }
}

