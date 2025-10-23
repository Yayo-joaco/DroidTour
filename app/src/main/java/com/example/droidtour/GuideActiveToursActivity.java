package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;

public class GuideActiveToursActivity extends AppCompatActivity {

    private RecyclerView rvMyTours;
    private Chip chipTodas, chipEnProgreso, chipProgramados, chipCompletados;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_active_tours);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFilters();
    }

    private void initializeViews() {
        rvMyTours = findViewById(R.id.rv_my_tours);
        chipTodas = findViewById(R.id.chip_todas);
        chipEnProgreso = findViewById(R.id.chip_en_progreso);
        chipProgramados = findViewById(R.id.chip_programados);
        chipCompletados = findViewById(R.id.chip_completados);
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvMyTours.setLayoutManager(layoutManager);
        rvMyTours.setAdapter(new MyToursAdapter(currentFilter));
    }

    private void setupFilters() {
        chipTodas.setChecked(true);
        
        chipTodas.setOnClickListener(v -> {
            currentFilter = "ALL";
            refreshList();
        });
        
        chipEnProgreso.setOnClickListener(v -> {
            currentFilter = "IN_PROGRESS";
            refreshList();
        });
        
        chipProgramados.setOnClickListener(v -> {
            currentFilter = "SCHEDULED";
            refreshList();
        });
        
        chipCompletados.setOnClickListener(v -> {
            currentFilter = "COMPLETED";
            refreshList();
        });
    }

    private void refreshList() {
        rvMyTours.setAdapter(new MyToursAdapter(currentFilter));
    }

    // Adapter for My Tours
    private class MyToursAdapter extends RecyclerView.Adapter<MyToursAdapter.ViewHolder> {
        
        private final String[] allTourNames = {
                "City Tour Lima Centro Histórico",
                "Cusco Mágico Full Day",
                "Paracas y Huacachina"
        };
        private final String[] allDates = {
                "Hoy, 15 Dic",
                "Mañana, 16 Dic",
                "20 Dic, 2024"
        };
        private final String[] allTimes = {
                "09:00 - 13:00",
                "08:00 - 16:00",
                "07:00 - 19:00"
        };
        private final String[] allStatuses = {
                "EN PROGRESO",
                "PROGRAMADO",
                "PROGRAMADO"
        };
        private final String[] allProgress = {
                "Punto 2 de 4",
                "Próximo",
                "Próximo"
        };
        private final String[] allCurrentLocations = {
                "Plaza de Armas",
                "Por iniciar",
                "Por iniciar"
        };
        private final double[] allPayments = {180.0, 250.0, 450.0};
        private final int[] allParticipants = {6, 12, 6};
        
        private java.util.List<Integer> filteredIndices;
        
        MyToursAdapter(String filter) {
            filteredIndices = new java.util.ArrayList<>();
            for (int i = 0; i < allTourNames.length; i++) {
                if (filter.equals("ALL")) {
                    filteredIndices.add(i);
                } else if (filter.equals("IN_PROGRESS") && allStatuses[i].equals("EN PROGRESO")) {
                    filteredIndices.add(i);
                } else if (filter.equals("SCHEDULED") && allStatuses[i].equals("PROGRAMADO")) {
                    filteredIndices.add(i);
                } else if (filter.equals("COMPLETED") && allStatuses[i].equals("COMPLETADO")) {
                    filteredIndices.add(i);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_guide_active_tour, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
            int actualIndex = filteredIndices.get(position);
            
            holder.tvTourName.setText(allTourNames[actualIndex]);
            holder.tvTourDate.setText(allDates[actualIndex]);
            holder.tvTourTime.setText(allTimes[actualIndex]);
            holder.tvTourStatus.setText(allStatuses[actualIndex]);
            holder.tvTourProgress.setText(allProgress[actualIndex]);
            holder.tvCurrentLocation.setText(allCurrentLocations[actualIndex]);
            holder.tvPaymentAmount.setText(String.format("S/. %.0f", allPayments[actualIndex]));
            holder.tvParticipantsCount.setText(allParticipants[actualIndex] + " personas");

            // Show/hide buttons based on tour status
            boolean isInProgress = allStatuses[actualIndex].equals("EN PROGRESO");
            holder.fabScanQR.setVisibility(isInProgress ? android.view.View.VISIBLE : android.view.View.GONE);
            holder.btnRegisterLocation.setVisibility(isInProgress ? android.view.View.VISIBLE : android.view.View.GONE);

            // Handle button clicks - Ver Mapa abre vista simplificada
            holder.btnViewMap.setOnClickListener(v -> {
                android.widget.Toast.makeText(GuideActiveToursActivity.this, 
                    "Ver Mapa: " + allTourNames[actualIndex], 
                    android.widget.Toast.LENGTH_SHORT).show();
                // TODO: Abrir vista de mapa simplificada
            });

            holder.btnRegisterLocation.setOnClickListener(v -> {
                Intent intent = new Intent(GuideActiveToursActivity.this, LocationTrackingActivity.class);
                intent.putExtra("tour_name", allTourNames[actualIndex]);
                intent.putExtra("register_mode", true);
                startActivity(intent);
            });

            holder.fabScanQR.setOnClickListener(v -> {
                Intent intent = new Intent(GuideActiveToursActivity.this, QRScannerActivity.class);
                intent.putExtra("tour_name", allTourNames[actualIndex]);
                startActivity(intent);
            });

            // Make the whole item clickable for details
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(GuideActiveToursActivity.this, GuideActiveTourDetailActivity.class);
                intent.putExtra("tour_name", allTourNames[actualIndex]);
                intent.putExtra("payment", allPayments[actualIndex]);
                startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
            return filteredIndices.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTourName, tvTourDate, tvTourTime, tvTourStatus, tvTourProgress,
                    tvCurrentLocation, tvPaymentAmount, tvParticipantsCount;
            com.google.android.material.button.MaterialButton btnViewMap, btnRegisterLocation;
            com.google.android.material.floatingactionbutton.FloatingActionButton fabScanQR;

            ViewHolder(View itemView) {
                super(itemView);
                tvTourName = itemView.findViewById(R.id.tv_tour_name);
                tvTourDate = itemView.findViewById(R.id.tv_tour_date);
                tvTourTime = itemView.findViewById(R.id.tv_tour_time);
                tvTourStatus = itemView.findViewById(R.id.tv_tour_status);
                tvTourProgress = itemView.findViewById(R.id.tv_tour_progress);
                tvCurrentLocation = itemView.findViewById(R.id.tv_current_location);
                tvPaymentAmount = itemView.findViewById(R.id.tv_payment_amount);
                tvParticipantsCount = itemView.findViewById(R.id.tv_participants_count);
                btnViewMap = itemView.findViewById(R.id.btn_view_map);
                btnRegisterLocation = itemView.findViewById(R.id.btn_register_location);
                fabScanQR = itemView.findViewById(R.id.fab_scan_qr);
            }
        }
    }
}
