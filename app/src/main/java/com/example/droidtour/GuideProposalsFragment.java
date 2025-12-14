package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.TourOffer;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuideProposalsFragment extends Fragment {
    
    private static final String TAG = "GuideProposalsFragment";
    private RecyclerView rvPendingProposals, rvAcceptedProposals, rvRejectedProposals;
    private TextView tvPendingCount, tvAcceptedCount, tvRejectedCount;
    private FloatingActionButton fabNewProposal;
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;
    
    private List<TourOffer> pendingOffers = new ArrayList<>();
    private List<TourOffer> acceptedOffers = new ArrayList<>();
    private List<TourOffer> rejectedOffers = new ArrayList<>();
    
    private ProposalsAdapter pendingAdapter, acceptedAdapter, rejectedAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guide_proposals, container, false);
        
        firestoreManager = FirestoreManager.getInstance();
        prefsManager = new PreferencesManager(requireContext());
        
        initializeViews(view);
        setupClickListeners();
        setupRecyclerViews();
        loadCompanyAndData();
        
        return view;
    }
    
    private void loadCompanyAndData() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadProposalsData();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
            }
        });
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
        
        pendingAdapter = new ProposalsAdapter(pendingOffers);
        acceptedAdapter = new ProposalsAdapter(acceptedOffers);
        rejectedAdapter = new ProposalsAdapter(rejectedOffers);
        
        rvPendingProposals.setAdapter(pendingAdapter);
        rvAcceptedProposals.setAdapter(acceptedAdapter);
        rvRejectedProposals.setAdapter(rejectedAdapter);
    }
    
    private void loadProposalsData() {
        if (currentCompanyId == null) return;
        
        // Cargar ofertas de la empresa
        firestoreManager.getOffersByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<TourOffer> allOffers = (List<TourOffer>) result;
                
                pendingOffers.clear();
                acceptedOffers.clear();
                rejectedOffers.clear();
                
                for (TourOffer offer : allOffers) {
                    String status = offer.getStatus();
                    if ("PENDING".equalsIgnoreCase(status)) {
                        pendingOffers.add(offer);
                    } else if ("ACCEPTED".equalsIgnoreCase(status)) {
                        acceptedOffers.add(offer);
                    } else if ("REJECTED".equalsIgnoreCase(status)) {
                        rejectedOffers.add(offer);
                    }
                }
                
                if (tvPendingCount != null) tvPendingCount.setText(String.valueOf(pendingOffers.size()));
                if (tvAcceptedCount != null) tvAcceptedCount.setText(String.valueOf(acceptedOffers.size()));
                if (tvRejectedCount != null) tvRejectedCount.setText(String.valueOf(rejectedOffers.size()));
                
                pendingAdapter.updateData(pendingOffers);
                acceptedAdapter.updateData(acceptedOffers);
                rejectedAdapter.updateData(rejectedOffers);
                
                Log.d(TAG, "Propuestas cargadas - Pending: " + pendingOffers.size() + 
                      ", Accepted: " + acceptedOffers.size() + ", Rejected: " + rejectedOffers.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando propuestas", e);
                if (tvPendingCount != null) tvPendingCount.setText("0");
                if (tvAcceptedCount != null) tvAcceptedCount.setText("0");
                if (tvRejectedCount != null) tvRejectedCount.setText("0");
            }
        });
    }
    
    // Adapter para propuestas
    private static class ProposalsAdapter extends RecyclerView.Adapter<ProposalsAdapter.ViewHolder> {
        private List<TourOffer> offers;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        ProposalsAdapter(List<TourOffer> offers) {
            this.offers = offers != null ? offers : new ArrayList<>();
        }
        
        void updateData(List<TourOffer> newData) {
            this.offers = newData != null ? newData : new ArrayList<>();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TourOffer offer = offers.get(position);
            holder.text1.setText(offer.getTourName() != null ? offer.getTourName() : "Oferta");
            String info = offer.getStatus() + " - S/. " + (offer.getPaymentAmount() != null ? offer.getPaymentAmount() : 0);
            holder.text2.setText(info);
        }
        
        @Override
        public int getItemCount() {
            return offers != null ? offers.size() : 0;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View view) {
                super(view);
                text1 = view.findViewById(android.R.id.text1);
                text2 = view.findViewById(android.R.id.text2);
            }
        }
    }
}
