package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SalesByTourFragment extends Fragment {
    
    private RecyclerView rvToursSales;
    private View layoutEmptyTours;

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
    }
    
    private void setupRecyclerView() {
        rvToursSales.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Configurar adapter para reportes por tour
    }
    
    private void loadSalesData() {
        // TODO: Cargar datos reales desde base de datos
        // Por ahora mostrar estado vacío
        showEmptyState(true);
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            rvToursSales.setVisibility(View.GONE);
            layoutEmptyTours.setVisibility(View.VISIBLE);
        } else {
            rvToursSales.setVisibility(View.VISIBLE);
            layoutEmptyTours.setVisibility(View.GONE);
        }
    }
}
