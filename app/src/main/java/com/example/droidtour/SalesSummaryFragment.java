package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SalesSummaryFragment extends Fragment {
    
    private TextView tvTotalRevenue, tvTotalTours, tvAvgTicket, tvAvgRating;
    private TextView tvGrossRevenue, tvPlatformFee, tvGuidePayments, tvNetRevenue;
    private RecyclerView rvTopTours;

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
    }
}
