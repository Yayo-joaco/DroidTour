package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class LocationTrackingActivity extends AppCompatActivity {

    private RecyclerView rvLocationPoints;
    private TextView tvTourName, tvTourProgress, tvPointsCompleted;
    private MaterialButton btnCenterLocation;
    private com.example.droidtour.utils.PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea un guía
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_location_tracking);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        loadTourData();
    }

    private void initializeViews() {
        rvLocationPoints = findViewById(R.id.rv_location_points);
        tvTourName = findViewById(R.id.tv_tour_name);
        tvTourProgress = findViewById(R.id.tv_tour_progress);
        tvPointsCompleted = findViewById(R.id.tv_points_completed);
        btnCenterLocation = findViewById(R.id.btn_center_location);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvLocationPoints.setLayoutManager(layoutManager);
        rvLocationPoints.setAdapter(new LocationPointsAdapter());
    }

    private void loadTourData() {
        String tourName = getIntent().getStringExtra("tour_name");
        if (tourName != null) {
            tvTourName.setText(tourName);
        } else {
            tvTourName.setText("City Tour Lima Centro");
        }
        
        updateProgressCounter();

        btnCenterLocation.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, 
                "Centrando en tu ubicación actual", 
                android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void updateProgressCounter() {
        RecyclerView.Adapter adapter = rvLocationPoints.getAdapter();
        if (adapter instanceof LocationPointsAdapter) {
            LocationPointsAdapter locationAdapter = (LocationPointsAdapter) adapter;
            int completed = locationAdapter.getCompletedCount();
            int total = locationAdapter.getItemCount();
            
            tvTourProgress.setText("Punto " + completed + " de " + total + " • 6 participantes");
            tvPointsCompleted.setText(completed + "/" + total + " completados");
        }
    }

    // Adapter for Location Points
    private class LocationPointsAdapter extends RecyclerView.Adapter<LocationPointsAdapter.ViewHolder> {
        
        private final String[] pointNames = {
                "Punto de Encuentro",
                "Plaza de Armas",
                "Catedral de Lima",
                "Palacio de Gobierno"
        };
        private boolean[] pointCompleted = {
                true,
                true,
                false,
                false
        };
        private final String[] arrivalTimes = {
                "09:00 AM",
                "10:15 AM",
                "--:--",
                "--:--"
        };

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_location_point, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.tvPointNumber.setText(String.valueOf(position + 1));
            holder.tvLocationName.setText(pointNames[position]);
            holder.tvArrivalTime.setText("⏰ " + arrivalTimes[position]);

            // Set status and button visibility
            if (pointCompleted[position]) {
                holder.tvLocationDescription.setText("COMPLETADO");
                holder.tvLocationDescription.setTextColor(getColor(R.color.green));
                holder.btnRegisterArrival.setVisibility(android.view.View.GONE);
                holder.tvStatusCompleted.setVisibility(android.view.View.VISIBLE);
            } else {
                holder.tvLocationDescription.setText("PENDIENTE");
                holder.tvLocationDescription.setTextColor(getColor(R.color.gray));
                holder.btnRegisterArrival.setVisibility(android.view.View.VISIBLE);
                holder.tvStatusCompleted.setVisibility(android.view.View.GONE);
            }

            // Register button click
            holder.btnRegisterArrival.setOnClickListener(v -> {
                pointCompleted[position] = true;
                arrivalTimes[position] = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        .format(new java.util.Date());
                notifyItemChanged(position);
                
                // Update progress counter
                updateProgressCounter();
                
                android.widget.Toast.makeText(LocationTrackingActivity.this, 
                    "Llegada registrada: " + pointNames[position], 
                    android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return pointNames.length;
        }

        public int getCompletedCount() {
            int count = 0;
            for (boolean completed : pointCompleted) {
                if (completed) count++;
            }
            return count;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPointNumber, tvLocationName, tvLocationDescription, tvArrivalTime, tvStatusCompleted;
            MaterialButton btnRegisterArrival;

            ViewHolder(View itemView) {
                super(itemView);
                tvPointNumber = itemView.findViewById(R.id.tv_point_number);
                tvLocationName = itemView.findViewById(R.id.tv_location_name);
                tvLocationDescription = itemView.findViewById(R.id.tv_location_description);
                tvArrivalTime = itemView.findViewById(R.id.tv_arrival_time);
                tvStatusCompleted = itemView.findViewById(R.id.tv_status_completed);
                btnRegisterArrival = itemView.findViewById(R.id.btn_register_arrival);
            }
        }
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
