package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;

public class ActiveToursFragment extends Fragment {
    
    private TextInputEditText etSearchTours;
    private RecyclerView rvActiveTours;
    private View layoutEmptyState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_tours, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        loadActiveTours();
        
        return view;
    }
    
    private void initializeViews(View view) {
        etSearchTours = view.findViewById(R.id.et_search_tours);
        rvActiveTours = view.findViewById(R.id.rv_active_tours);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
    }
    
    private void setupRecyclerView() {
        rvActiveTours.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Configurar adapter para tours activos
    }
    
    private void loadActiveTours() {
        // TODO: Cargar tours activos desde base de datos
        // Por ahora mostrar estado vacío
        showEmptyState(true);
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            rvActiveTours.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvActiveTours.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}
