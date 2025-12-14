package com.example.droidtour;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActiveToursFragment extends Fragment {
    
    private static final String TAG = "ActiveToursFragment";
    private TextInputEditText etSearchTours;
    private RecyclerView rvActiveTours;
    private View layoutEmptyState;
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;
    private List<Reservation> activeReservations = new ArrayList<>();
    private ActiveToursAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_tours, container, false);
        
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
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadActiveTours();
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
        etSearchTours = view.findViewById(R.id.et_search_tours);
        rvActiveTours = view.findViewById(R.id.rv_active_tours);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
    }
    
    private void setupRecyclerView() {
        rvActiveTours.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ActiveToursAdapter(activeReservations);
        rvActiveTours.setAdapter(adapter);
    }
    
    private void loadActiveTours() {
        if (currentCompanyId == null) {
            showEmptyState(true);
            return;
        }
        
        // Cargar reservaciones activas (EN_PROGRESO o CONFIRMADA)
        firestoreManager.getReservationsByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Reservation> allReservations = (List<Reservation>) result;
                activeReservations.clear();
                
                for (Reservation r : allReservations) {
                    String status = r.getStatus();
                    if ("EN_PROGRESO".equals(status) || "CONFIRMADA".equals(status)) {
                        activeReservations.add(r);
                    }
                }
                
                if (activeReservations.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    adapter.updateData(activeReservations);
                }
                
                Log.d(TAG, "Tours activos cargados: " + activeReservations.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando tours activos", e);
                showEmptyState(true);
            }
        });
    }
    
    private void showEmptyState(boolean show) {
        if (layoutEmptyState == null || rvActiveTours == null) return;
        
        if (show) {
            rvActiveTours.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvActiveTours.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
    
    // Adapter para tours activos
    private static class ActiveToursAdapter extends RecyclerView.Adapter<ActiveToursAdapter.ViewHolder> {
        private List<Reservation> tours;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        
        ActiveToursAdapter(List<Reservation> tours) {
            this.tours = tours != null ? tours : new ArrayList<>();
        }
        
        void updateData(List<Reservation> newData) {
            this.tours = newData != null ? newData : new ArrayList<>();
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
            Reservation r = tours.get(position);
            holder.text1.setText(r.getTourName() != null ? r.getTourName() : "Tour");
            String info = r.getStatus() + " - " + (r.getTourDate() != null ? r.getTourDate() + " " + (r.getTourTime() != null ? r.getTourTime() : "") : "");
            holder.text2.setText(info);
        }
        
        @Override
        public int getItemCount() {
            return tours != null ? tours.size() : 0;
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
