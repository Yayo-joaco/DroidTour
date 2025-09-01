package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SalesByServiceFragment extends Fragment {
    
    private RecyclerView rvServicesSales;
    private View layoutEmptyServices;

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
    }
    
    private void setupRecyclerView() {
        rvServicesSales.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Configurar adapter para reportes por servicio
    }
    
    private void loadSalesData() {
        // TODO: Cargar datos reales desde base de datos
        // Por ahora mostrar estado vacío
        showEmptyState(true);
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            rvServicesSales.setVisibility(View.GONE);
            layoutEmptyServices.setVisibility(View.VISIBLE);
        } else {
            rvServicesSales.setVisibility(View.VISIBLE);
            layoutEmptyServices.setVisibility(View.GONE);
        }
    }
}
