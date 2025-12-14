package com.example.droidtour;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Reservation;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutAlertsActivity extends AppCompatActivity {
    
    private static final String TAG = "CheckoutAlertsActivity";
    private RecyclerView rvPendingCheckouts, rvProcessedCheckouts;
    private TextView tvPendingCount, tvProcessedCount;
    private TextView tvTotalProcessed, tvToursCompleted, tvPlatformCommission;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private String currentCompanyId;
    
    private List<Reservation> pendingCheckouts = new ArrayList<>();
    private List<Reservation> processedCheckouts = new ArrayList<>();
    private CheckoutAdapter pendingAdapter, processedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("COMPANY_ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_checkout_alerts);
        
        firestoreManager = FirestoreManager.getInstance();
        
        setupToolbar();
        initializeViews();
        setupRecyclerViews();
        loadCompanyIdAndData();
    }
    
    private void loadCompanyIdAndData() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadCheckoutData();
                } else {
                    Toast.makeText(CheckoutAlertsActivity.this, "No se encontró empresa asociada", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar datos de usuario", e);
                Toast.makeText(CheckoutAlertsActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        rvPendingCheckouts = findViewById(R.id.rv_pending_checkouts);
        rvProcessedCheckouts = findViewById(R.id.rv_processed_checkouts);
        
        tvPendingCount = findViewById(R.id.tv_pending_count);
        tvProcessedCount = findViewById(R.id.tv_processed_count);
        
        tvTotalProcessed = findViewById(R.id.tv_total_processed);
        tvToursCompleted = findViewById(R.id.tv_tours_completed);
        tvPlatformCommission = findViewById(R.id.tv_platform_commission);
    }
    
    private void setupRecyclerViews() {
        rvPendingCheckouts.setLayoutManager(new LinearLayoutManager(this));
        rvProcessedCheckouts.setLayoutManager(new LinearLayoutManager(this));
        
        pendingAdapter = new CheckoutAdapter(pendingCheckouts, true, this::processCheckout);
        processedAdapter = new CheckoutAdapter(processedCheckouts, false, null);
        
        rvPendingCheckouts.setAdapter(pendingAdapter);
        rvProcessedCheckouts.setAdapter(processedAdapter);
    }
    
    private void loadCheckoutData() {
        if (currentCompanyId == null) return;
        
        Log.d(TAG, "Cargando reservaciones para companyId: " + currentCompanyId);
        
        // Cargar reservaciones de la empresa
        firestoreManager.getReservationsByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Reservation> allReservations = (List<Reservation>) result;
                
                pendingCheckouts.clear();
                processedCheckouts.clear();
                
                double totalProcessed = 0;
                
                for (Reservation reservation : allReservations) {
                    String status = reservation.getStatus();
                    if ("EN_PROGRESO".equals(status) || "CONFIRMADA".equals(status)) {
                        pendingCheckouts.add(reservation);
                    } else if ("COMPLETADA".equals(status)) {
                        processedCheckouts.add(reservation);
                        totalProcessed += reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0;
                    }
                }
                
                // Actualizar contadores
                tvPendingCount.setText(String.valueOf(pendingCheckouts.size()));
                tvProcessedCount.setText(String.valueOf(processedCheckouts.size()));
                tvTotalProcessed.setText(String.format(Locale.getDefault(), "S/. %.2f", totalProcessed));
                tvToursCompleted.setText(String.valueOf(processedCheckouts.size()));
                tvPlatformCommission.setText(String.format(Locale.getDefault(), "S/. %.2f", totalProcessed * 0.10)); // 10% comisión
                
                // Actualizar adapters
                pendingAdapter.notifyDataSetChanged();
                processedAdapter.notifyDataSetChanged();
                
                Log.d(TAG, "Pendientes: " + pendingCheckouts.size() + ", Procesados: " + processedCheckouts.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar reservaciones", e);
                Toast.makeText(CheckoutAlertsActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void processCheckout(Reservation reservation) {
        // Marcar como completada
        firestoreManager.updateReservationStatus(reservation.getReservationId(), "COMPLETADA", 
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Toast.makeText(CheckoutAlertsActivity.this, "✅ Checkout procesado", Toast.LENGTH_SHORT).show();
                    loadCheckoutData(); // Recargar datos
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(CheckoutAlertsActivity.this, "Error al procesar checkout", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    // Adapter interno para checkouts
    private static class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.ViewHolder> {
        private List<Reservation> reservations;
        private boolean showActions;
        private OnCheckoutAction actionListener;
        
        interface OnCheckoutAction {
            void onProcess(Reservation reservation);
        }
        
        CheckoutAdapter(List<Reservation> reservations, boolean showActions, OnCheckoutAction listener) {
            this.reservations = reservations;
            this.showActions = showActions;
            this.actionListener = listener;
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
            Reservation reservation = reservations.get(position);
            
            holder.text1.setText(reservation.getTourName() != null ? reservation.getTourName() : "Tour");
            
            String details = String.format(Locale.getDefault(), 
                "S/. %.2f - %s", 
                reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0.0,
                reservation.getStatus() != null ? reservation.getStatus() : "");
            holder.text2.setText(details);
            
            if (showActions && actionListener != null) {
                holder.itemView.setOnClickListener(v -> actionListener.onProcess(reservation));
            }
        }
        
        @Override
        public int getItemCount() {
            return reservations != null ? reservations.size() : 0;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void redirectToLogin() {
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
