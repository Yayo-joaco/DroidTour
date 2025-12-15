package com.example.droidtour.superadmin.helpers;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import com.example.droidtour.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class para configuración y actualización de gráficos del dashboard
 */
public class DashboardChartHelper {
    
    private static final String TAG = "DashboardChartHelper";
    private final Context context;
    
    public DashboardChartHelper(Context context) {
        this.context = context;
    }
    
    // ========== LINE CHART - REVENUE ==========
    
    /**
     * Actualiza el gráfico de líneas de ingresos con datos diarios
     */
    public void updateLineChartWithDailyData(LineChart lineChart, Map<String, Double> dailyRevenue, 
                                           DashboardDateHelper.DateRange dateRange) {
        if (lineChart == null) return;
        
        List<String> allDays = DashboardDateHelper.generateDaysInRange(dateRange);
        TreeMap<String, Double> sortedRevenue = new TreeMap<>(dailyRevenue);
        
        List<Entry> entries = new ArrayList<>();
        List<String> dayLabels = new ArrayList<>();
        
        for (int i = 0; i < allDays.size(); i++) {
            String dayKey = allDays.get(i);
            Double revenue = sortedRevenue.get(dayKey);
            entries.add(new Entry(i, revenue != null ? revenue.floatValue() : 0f));
            dayLabels.add(DashboardDateHelper.formatDayLabel(dayKey));
        }
        
        if (entries.isEmpty()) {
            updateLineChartWithEmptyData(lineChart);
            return;
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Ingresos (S/)");
        dataSet.setColor(context.getResources().getColor(R.color.green));
        dataSet.setCircleColor(context.getResources().getColor(R.color.green));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(context.getResources().getColor(R.color.green));
        dataSet.setFillAlpha(30);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(context.getResources().getColor(R.color.black));
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        xAxis.setLabelRotationAngle(-45f);
        
        lineChart.invalidate();
    }
    
    /**
     * Actualiza el gráfico de líneas de ingresos con datos mensuales
     */
    public void updateLineChartWithMonthlyData(LineChart lineChart, Map<String, Double> monthlyRevenue,
                                             DashboardDateHelper.DateRange dateRange) {
        if (lineChart == null) return;
        
        List<String> allMonths = DashboardDateHelper.generateMonthsInRange(dateRange);
        TreeMap<String, Double> sortedRevenue = new TreeMap<>(monthlyRevenue);
        
        List<Entry> entries = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        
        for (int i = 0; i < allMonths.size(); i++) {
            String monthKey = allMonths.get(i);
            Double revenue = sortedRevenue.get(monthKey);
            float revenueValue = revenue != null ? revenue.floatValue() : 0f;
            entries.add(new Entry(i, revenueValue));
            monthLabels.add(DashboardDateHelper.formatMonthLabel(monthKey));
        }
        
        if (entries.isEmpty()) {
            updateLineChartWithEmptyData(lineChart);
            return;
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Ingresos (S/)");
        dataSet.setColor(context.getResources().getColor(R.color.green));
        dataSet.setCircleColor(context.getResources().getColor(R.color.green));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(context.getResources().getColor(R.color.green));
        dataSet.setFillAlpha(30);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(context.getResources().getColor(R.color.black));
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xAxis.setLabelRotationAngle(-45f);
        
        lineChart.invalidate();
    }
    
    public void updateLineChartWithEmptyData(LineChart lineChart) {
        if (lineChart == null) return;
        
        List<Entry> entries = new ArrayList<>();
        LineDataSet dataSet = new LineDataSet(entries, "Ingresos (S/)");
        dataSet.setColor(context.getResources().getColor(R.color.green));
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Sin datos")));
        
        lineChart.invalidate();
    }
    
    // ========== BAR CHART - BOOKINGS ==========
    
    public void updateBarChartWithDailyData(BarChart barChart, Map<String, Integer> dailyBookings,
                                          DashboardDateHelper.DateRange dateRange) {
        if (barChart == null) return;
        
        List<String> allDays = DashboardDateHelper.generateDaysInRange(dateRange);
        TreeMap<String, Integer> sortedBookings = new TreeMap<>(dailyBookings);
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> dayLabels = new ArrayList<>();
        
        for (int i = 0; i < allDays.size(); i++) {
            String dayKey = allDays.get(i);
            Integer bookings = sortedBookings.get(dayKey);
            entries.add(new BarEntry(i, bookings != null ? bookings.floatValue() : 0f));
            dayLabels.add(DashboardDateHelper.formatDayLabel(dayKey));
        }
        
        if (entries.isEmpty()) {
            updateBarChartWithEmptyData(barChart);
            return;
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Reservas");
        dataSet.setColor(context.getResources().getColor(R.color.green));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(context.getResources().getColor(R.color.black));
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        xAxis.setLabelRotationAngle(-45f);
        
        barChart.invalidate();
    }
    
    public void updateBarChartWithMonthlyData(BarChart barChart, Map<String, Integer> monthlyBookings,
                                             DashboardDateHelper.DateRange dateRange) {
        if (barChart == null) return;
        
        List<String> allMonths = DashboardDateHelper.generateMonthsInRange(dateRange);
        TreeMap<String, Integer> sortedBookings = new TreeMap<>(monthlyBookings);
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        
        for (int i = 0; i < allMonths.size(); i++) {
            String monthKey = allMonths.get(i);
            Integer bookings = sortedBookings.get(monthKey);
            entries.add(new BarEntry(i, bookings != null ? bookings.floatValue() : 0f));
            monthLabels.add(DashboardDateHelper.formatMonthLabel(monthKey));
        }
        
        if (entries.isEmpty()) {
            updateBarChartWithEmptyData(barChart);
            return;
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Reservas");
        dataSet.setColor(context.getResources().getColor(R.color.green));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(context.getResources().getColor(R.color.black));
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xAxis.setLabelRotationAngle(-45f);
        
        barChart.invalidate();
    }
    
    public void updateBarChartWithEmptyData(BarChart barChart) {
        if (barChart == null) return;
        
        List<BarEntry> entries = new ArrayList<>();
        BarDataSet dataSet = new BarDataSet(entries, "Reservas");
        dataSet.setColor(context.getResources().getColor(R.color.green));
        
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Sin datos")));
        
        barChart.invalidate();
    }
    
    // ========== BAR CHART - PEOPLE ==========
    
    public void updateBarChartPeopleWithDailyData(BarChart barChart, Map<String, Integer> dailyPeople,
                                                 DashboardDateHelper.DateRange dateRange) {
        if (barChart == null) return;
        
        List<String> allDays = DashboardDateHelper.generateDaysInRange(dateRange);
        TreeMap<String, Integer> sortedPeople = new TreeMap<>(dailyPeople);
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> dayLabels = new ArrayList<>();
        
        for (int i = 0; i < allDays.size(); i++) {
            String dayKey = allDays.get(i);
            Integer people = sortedPeople.get(dayKey);
            entries.add(new BarEntry(i, people != null ? people.floatValue() : 0f));
            dayLabels.add(DashboardDateHelper.formatDayLabel(dayKey));
        }
        
        if (entries.isEmpty()) {
            updateBarChartPeopleWithEmptyData(barChart);
            return;
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Personas");
        dataSet.setColor(context.getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(context.getResources().getColor(R.color.black));
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        xAxis.setLabelRotationAngle(-45f);
        
        barChart.invalidate();
    }
    
    public void updateBarChartPeopleWithMonthlyData(BarChart barChart, Map<String, Integer> monthlyPeople,
                                                   DashboardDateHelper.DateRange dateRange) {
        if (barChart == null) return;
        
        List<String> allMonths = DashboardDateHelper.generateMonthsInRange(dateRange);
        TreeMap<String, Integer> sortedPeople = new TreeMap<>(monthlyPeople);
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        
        for (int i = 0; i < allMonths.size(); i++) {
            String monthKey = allMonths.get(i);
            Integer people = sortedPeople.get(monthKey);
            entries.add(new BarEntry(i, people != null ? people.floatValue() : 0f));
            monthLabels.add(DashboardDateHelper.formatMonthLabel(monthKey));
        }
        
        if (entries.isEmpty()) {
            updateBarChartPeopleWithEmptyData(barChart);
            return;
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Personas");
        dataSet.setColor(context.getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(context.getResources().getColor(R.color.black));
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xAxis.setLabelRotationAngle(-45f);
        
        barChart.invalidate();
    }
    
    public void updateBarChartPeopleWithEmptyData(BarChart barChart) {
        if (barChart == null) return;
        
        List<BarEntry> entries = new ArrayList<>();
        BarDataSet dataSet = new BarDataSet(entries, "Personas");
        dataSet.setColor(context.getResources().getColor(R.color.primary));
        
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Sin datos")));
        
        barChart.invalidate();
    }
    
    // ========== LINE CHART - AVERAGE PRICE ==========
    
    public void updateLineChartAveragePriceWithDailyData(LineChart lineChart, Map<String, Double> dailyAveragePrice,
                                                        DashboardDateHelper.DateRange dateRange) {
        if (lineChart == null) return;
        
        List<String> allDays = DashboardDateHelper.generateDaysInRange(dateRange);
        TreeMap<String, Double> sortedAveragePrice = new TreeMap<>(dailyAveragePrice);
        
        List<Entry> entries = new ArrayList<>();
        List<String> dayLabels = new ArrayList<>();
        
        for (int i = 0; i < allDays.size(); i++) {
            String dayKey = allDays.get(i);
            Double averagePrice = sortedAveragePrice.get(dayKey);
            entries.add(new Entry(i, averagePrice != null ? averagePrice.floatValue() : 0f));
            dayLabels.add(DashboardDateHelper.formatDayLabel(dayKey));
        }
        
        if (entries.isEmpty()) {
            updateLineChartAveragePriceWithEmptyData(lineChart);
            return;
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Precio Promedio (S/)");
        dataSet.setColor(context.getResources().getColor(R.color.primary));
        dataSet.setCircleColor(context.getResources().getColor(R.color.primary));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(context.getResources().getColor(R.color.primary));
        dataSet.setFillAlpha(30);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(context.getResources().getColor(R.color.black));
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        xAxis.setLabelRotationAngle(-45f);
        
        lineChart.invalidate();
    }
    
    public void updateLineChartAveragePriceWithMonthlyData(LineChart lineChart, Map<String, Double> monthlyAveragePrice,
                                                          DashboardDateHelper.DateRange dateRange) {
        if (lineChart == null) return;
        
        List<String> allMonths = DashboardDateHelper.generateMonthsInRange(dateRange);
        TreeMap<String, Double> sortedAveragePrice = new TreeMap<>(monthlyAveragePrice);
        
        List<Entry> entries = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        
        for (int i = 0; i < allMonths.size(); i++) {
            String monthKey = allMonths.get(i);
            Double averagePrice = sortedAveragePrice.get(monthKey);
            entries.add(new Entry(i, averagePrice != null ? averagePrice.floatValue() : 0f));
            monthLabels.add(DashboardDateHelper.formatMonthLabel(monthKey));
        }
        
        if (entries.isEmpty()) {
            updateLineChartAveragePriceWithEmptyData(lineChart);
            return;
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Precio Promedio (S/)");
        dataSet.setColor(context.getResources().getColor(R.color.primary));
        dataSet.setCircleColor(context.getResources().getColor(R.color.primary));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(context.getResources().getColor(R.color.primary));
        dataSet.setFillAlpha(30);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(context.getResources().getColor(R.color.black));
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xAxis.setLabelRotationAngle(-45f);
        
        lineChart.invalidate();
    }
    
    public void updateLineChartAveragePriceWithEmptyData(LineChart lineChart) {
        if (lineChart == null) return;
        
        List<Entry> entries = new ArrayList<>();
        LineDataSet dataSet = new LineDataSet(entries, "Precio Promedio (S/)");
        dataSet.setColor(context.getResources().getColor(R.color.primary));
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Sin datos")));
        
        lineChart.invalidate();
    }
    
    // ========== PIE CHART - TOURS BY CATEGORY ==========
    
    public void updatePieChartWithData(PieChart pieChart, Map<String, Integer> toursByCategory) {
        if (pieChart == null) return;
        
        if (toursByCategory.isEmpty()) {
            updatePieChartWithEmptyData(pieChart);
            return;
        }
        
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : toursByCategory.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }
        
        entries.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        
        int[] colors = new int[]{
            context.getResources().getColor(R.color.primary),
            context.getResources().getColor(R.color.green),
            context.getResources().getColor(R.color.orange),
            context.getResources().getColor(R.color.gray),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#795548"),
            Color.parseColor("#607D8B"),
            Color.parseColor("#E91E63")
        };
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        
        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter());
        
        pieChart.setData(pieData);
        pieChart.invalidate();
    }
    
    public void updatePieChartWithEmptyData(PieChart pieChart) {
        if (pieChart == null) return;
        
        List<PieEntry> entries = new ArrayList<>();
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{context.getResources().getColor(R.color.gray)});
        
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }
}
