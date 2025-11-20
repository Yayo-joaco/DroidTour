package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.models.Tour;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuideActiveToursActivity extends AppCompatActivity {

    private static final String TAG = "GuideActiveTours";
    private RecyclerView rvMyTours;
    private Chip chipTodas, chipEnProgreso, chipProgramados, chipCompletados;
    private String currentFilter = "ALL";
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private String currentUserId;
    private List<Reservation> allReservations = new ArrayList<>();
    private MyToursAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);
        
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
        
        // Obtener ID del usuario actual
        currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = prefsManager.getUserId();
        }
        
        setContentView(R.layout.activity_guide_active_tours);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFilters();
        
        // Cargar tours del guía desde Firebase
        loadGuideTours();
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
        adapter = new MyToursAdapter(new ArrayList<>());
        rvMyTours.setAdapter(adapter);
    }
    
    private void loadGuideTours() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Error: currentUserId es null o vacío");
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "🔄 Cargando tours para guía: " + currentUserId);
        
        firestoreManager.getReservationsByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                allReservations = (List<Reservation>) result;
                Log.d(TAG, "✅ Tours cargados: " + allReservations.size());
                filterAndUpdateList();
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando tours del guía", e);
                Toast.makeText(GuideActiveToursActivity.this, 
                    "Error al cargar tours: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void filterAndUpdateList() {
        List<Reservation> filteredReservations = new ArrayList<>();
        
        for (Reservation reservation : allReservations) {
            String status = reservation.getStatus();
            if (currentFilter.equals("ALL")) {
                filteredReservations.add(reservation);
            } else if (currentFilter.equals("IN_PROGRESS") && "EN_PROGRESO".equals(status)) {
                filteredReservations.add(reservation);
            } else if (currentFilter.equals("SCHEDULED") && ("CONFIRMADA".equals(status) || "PROGRAMADA".equals(status))) {
                filteredReservations.add(reservation);
            } else if (currentFilter.equals("COMPLETED") && "COMPLETADA".equals(status)) {
                filteredReservations.add(reservation);
            }
        }
        
        adapter.updateData(filteredReservations);
        Log.d(TAG, "📊 Tours filtrados (" + currentFilter + "): " + filteredReservations.size());
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
        filterAndUpdateList();
    }

    // Adapter for My Tours
    private class MyToursAdapter extends RecyclerView.Adapter<MyToursAdapter.ViewHolder> {
        
        private List<Reservation> reservations;
        
        MyToursAdapter(List<Reservation> reservations) {
            this.reservations = reservations != null ? reservations : new ArrayList<>();
        }
        
        public void updateData(List<Reservation> newReservations) {
            this.reservations = newReservations != null ? newReservations : new ArrayList<>();
            notifyDataSetChanged();
        }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_guide_active_tour, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
            Reservation reservation = reservations.get(position);
            
            // Datos básicos del tour
            holder.tvTourName.setText(reservation.getTourName() != null ? reservation.getTourName() : "Tour sin nombre");
            holder.tvTourDate.setText(reservation.getTourDate() != null ? reservation.getTourDate() : "Fecha no disponible");
            holder.tvTourTime.setText(reservation.getTourTime() != null ? reservation.getTourTime() : "Hora no disponible");
            
            // Estado del tour
            String status = reservation.getStatus() != null ? reservation.getStatus() : "PENDIENTE";
            holder.tvTourStatus.setText(status);
            
            // Progreso (por ahora simplificado)
            if ("EN_PROGRESO".equals(status)) {
                holder.tvTourProgress.setText("En progreso");
                holder.tvCurrentLocation.setText("Ubicación actual");
            } else if ("CONFIRMADA".equals(status) || "PROGRAMADA".equals(status)) {
                holder.tvTourProgress.setText("Programado");
                holder.tvCurrentLocation.setText("Programado");
            } else if ("COMPLETADA".equals(status)) {
                holder.tvTourProgress.setText("Completado");
                holder.tvCurrentLocation.setText("Finalizado");
            } else {
                holder.tvTourProgress.setText("Pendiente");
                holder.tvCurrentLocation.setText("N/A");
            }
            
            // Pago y participantes
            double totalPrice = reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0.0;
            int numberOfPeople = reservation.getNumberOfPeople() != null ? reservation.getNumberOfPeople() : 0;
            holder.tvPaymentAmount.setText(String.format("S/. %.2f", totalPrice));
            holder.tvParticipantsCount.setText(numberOfPeople + " persona" + (numberOfPeople != 1 ? "s" : ""));

            // Show/hide buttons based on tour status
            boolean isInProgress = "EN_PROGRESO".equals(status);
            holder.fabScanQR.setVisibility(isInProgress ? android.view.View.VISIBLE : android.view.View.GONE);
            holder.btnRegisterLocation.setVisibility(isInProgress ? android.view.View.VISIBLE : android.view.View.GONE);

            // Handle button clicks
            holder.btnViewMap.setOnClickListener(v -> {
                android.widget.Toast.makeText(GuideActiveToursActivity.this, 
                    "Ver Mapa: " + reservation.getTourName(), 
                    android.widget.Toast.LENGTH_SHORT).show();
                // TODO: Abrir vista de mapa simplificada
            });

            holder.btnRegisterLocation.setOnClickListener(v -> {
                Intent intent = new Intent(GuideActiveToursActivity.this, LocationTrackingActivity.class);
                intent.putExtra("tour_name", reservation.getTourName());
                intent.putExtra("reservation_id", reservation.getReservationId());
                intent.putExtra("register_mode", true);
                startActivity(intent);
            });

            holder.fabScanQR.setOnClickListener(v -> {
                Intent intent = new Intent(GuideActiveToursActivity.this, QRScannerActivity.class);
                intent.putExtra("tour_name", reservation.getTourName());
                intent.putExtra("reservation_id", reservation.getReservationId());
                startActivity(intent);
            });

            // Make the whole item clickable for details
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(GuideActiveToursActivity.this, GuideActiveTourDetailActivity.class);
                intent.putExtra("tour_name", reservation.getTourName());
                intent.putExtra("reservation_id", reservation.getReservationId());
                intent.putExtra("payment", totalPrice);
                startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
            return reservations.size();
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
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
