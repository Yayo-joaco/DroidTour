package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Color;

public class SalesSummaryFragment extends Fragment {
    
    private TextView tvTotalRevenue, tvTotalTours, tvAvgTicket, tvAvgRating;
    private TextView tvGrossRevenue, tvPlatformFee, tvGuidePayments, tvNetRevenue;
    private RecyclerView rvTopTours;
    private LineChart lineChartTrend;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales_summary, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        loadSummaryData();
        
        return view;
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
        // TODO: Cargar datos reales desde base de datos
        // Por ahora mostrar datos de prueba
        tvTotalRevenue.setText("S/. 12,450");
        tvTotalTours.setText("47");
        tvAvgTicket.setText("S/. 265");
        tvAvgRating.setText("4.6");
        
        tvGrossRevenue.setText("S/. 12,450.00");
        tvPlatformFee.setText("S/. 1,245.00");
        tvGuidePayments.setText("S/. 3,500.00");
        tvNetRevenue.setText("S/. 7,705.00");

        // Datos de ejemplo para la tendencia
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0f, 800f));
        entries.add(new Entry(1f, 950f));
        entries.add(new Entry(2f, 1200f));
        entries.add(new Entry(3f, 1000f));
        entries.add(new Entry(4f, 1400f));

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
