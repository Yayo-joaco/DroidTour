package com.example.droidtour;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.TourOffer;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento que muestra los guías que han aceptado propuestas y están activos
 */
public class ActiveGuidesFragment extends Fragment {
    
    private static final String TAG = "ActiveGuidesFragment";
    private RecyclerView rvActiveGuides;
    private View layoutEmptyState;
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;
    private List<TourOffer> activeOffers = new ArrayList<>();
    private ActiveGuidesAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_guides, container, false);
        
        firestoreManager = FirestoreManager.getInstance();
        prefsManager = new PreferencesManager(requireContext());
        
        initializeViews(view);
        setupRecyclerView();
        loadCompanyAndData();
        
        return view;
    }
    
    private void loadCompanyAndData() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadActiveGuides();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
                showEmptyState(true);
            }
        });
    }
    
    private void initializeViews(View view) {
        rvActiveGuides = view.findViewById(R.id.rv_active_guides);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
    }
    
    private void setupRecyclerView() {
        rvActiveGuides.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ActiveGuidesAdapter(activeOffers);
        rvActiveGuides.setAdapter(adapter);
    }
    
    private void loadActiveGuides() {
        if (currentCompanyId == null) {
            showEmptyState(true);
            return;
        }
        
        // Cargar ofertas aceptadas de la empresa
        firestoreManager.getOffersByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<TourOffer> allOffers = (List<TourOffer>) result;
                activeOffers.clear();
                
                // Filtrar solo las ofertas aceptadas
                for (TourOffer offer : allOffers) {
                    if ("ACEPTADA".equals(offer.getStatus())) {
                        activeOffers.add(offer);
                    }
                }
                
                if (activeOffers.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    adapter.updateData(activeOffers);
                }
                
                Log.d(TAG, "Guías activos cargados: " + activeOffers.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando guías activos", e);
                showEmptyState(true);
            }
        });
    }
    
    private void showEmptyState(boolean show) {
        if (layoutEmptyState == null || rvActiveGuides == null) return;
        
        if (show) {
            rvActiveGuides.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvActiveGuides.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
    
    // Adapter para guías activos
    private static class ActiveGuidesAdapter extends RecyclerView.Adapter<ActiveGuidesAdapter.ViewHolder> {
        private List<TourOffer> offers;
        
        ActiveGuidesAdapter(List<TourOffer> offers) {
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
                    .inflate(R.layout.item_active_guide, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TourOffer offer = offers.get(position);
            
            // Nombre del guía
            holder.tvGuideName.setText(offer.getGuideName() != null ? offer.getGuideName() : "Guía");
            
            // Nombre del tour con fecha
            String tourInfo = offer.getTourName() != null ? offer.getTourName() : "Tour";
            if (offer.getTourDate() != null) {
                tourInfo += " - " + offer.getTourDate();
            }
            holder.tvTourName.setText(tourInfo);
            
            // Ubicación actual (TODO: integrar con ubicación en tiempo real)
            holder.tvCurrentLocation.setText("Ubicación no disponible");
            
            // Última actualización
            holder.tvLastUpdate.setText("En línea");
            
            // Foto del guía (TODO: cargar desde perfil)
            // holder.ivGuidePhoto - usar Glide cuando tengamos la URL
            
            // Listeners
            holder.ivLocateGuide.setOnClickListener(v -> {
                // TODO: Abrir mapa con ubicación del guía
            });
            
            holder.ivContactGuide.setOnClickListener(v -> {
                // TODO: Abrir chat o llamada con el guía
            });
        }
        
        @Override
        public int getItemCount() {
            return offers != null ? offers.size() : 0;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvGuideName, tvTourName, tvCurrentLocation, tvLastUpdate;
            ImageView ivGuidePhoto, ivLocateGuide, ivContactGuide;
            View viewStatusIndicator;
            
            ViewHolder(View view) {
                super(view);
                tvGuideName = view.findViewById(R.id.tv_guide_name);
                tvTourName = view.findViewById(R.id.tv_tour_name);
                tvCurrentLocation = view.findViewById(R.id.tv_current_location);
                tvLastUpdate = view.findViewById(R.id.tv_last_update);
                ivGuidePhoto = view.findViewById(R.id.iv_guide_photo);
                ivLocateGuide = view.findViewById(R.id.iv_locate_guide);
                ivContactGuide = view.findViewById(R.id.iv_contact_guide);
                viewStatusIndicator = view.findViewById(R.id.view_status_indicator);
            }
        }
    }
}
