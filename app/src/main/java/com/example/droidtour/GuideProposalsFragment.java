package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GuideProposalsFragment extends Fragment {
    
    private RecyclerView rvPendingProposals, rvAcceptedProposals, rvRejectedProposals;
    private TextView tvPendingCount, tvAcceptedCount, tvRejectedCount;
    private FloatingActionButton fabNewProposal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guide_proposals, container, false);
        
        initializeViews(view);
        setupClickListeners();
        setupRecyclerViews();
        loadProposalsData();
        
        return view;
    }
    
    private void initializeViews(View view) {
        rvPendingProposals = view.findViewById(R.id.rv_pending_proposals);
        rvAcceptedProposals = view.findViewById(R.id.rv_accepted_proposals);
        rvRejectedProposals = view.findViewById(R.id.rv_rejected_proposals);
        
        tvPendingCount = view.findViewById(R.id.tv_pending_count);
        tvAcceptedCount = view.findViewById(R.id.tv_accepted_count);
        tvRejectedCount = view.findViewById(R.id.tv_rejected_count);
        
        fabNewProposal = view.findViewById(R.id.fab_new_proposal);
    }
    
    private void setupClickListeners() {
        fabNewProposal.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Crear nueva propuesta", Toast.LENGTH_SHORT).show();
            // TODO: Abrir dialog o activity para crear nueva propuesta
        });
    }
    
    private void setupRecyclerViews() {
        rvPendingProposals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAcceptedProposals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRejectedProposals.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // TODO: Configurar adapters para cada RecyclerView
    }
    
    private void loadProposalsData() {
        // TODO: Cargar datos reales desde base de datos
        // Por ahora mostrar datos de prueba
        tvPendingCount.setText("3");
        tvAcceptedCount.setText("5");
        tvRejectedCount.setText("2");
    }
}
