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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
                
                // Obtener fecha de hoy para filtrar procesados recientes
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                Date todayStart = today.getTime();
                
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.set(Calendar.HOUR_OF_DAY, 0);
                tomorrow.set(Calendar.MINUTE, 0);
                tomorrow.set(Calendar.SECOND, 0);
                tomorrow.set(Calendar.MILLISECOND, 0);
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                Date tomorrowStart = tomorrow.getTime();
                
                double totalProcessedToday = 0;
                int toursCompletedToday = 0;
                
                for (Reservation reservation : allReservations) {
                    Boolean hasCheckedOut = reservation.getHasCheckedOut();
                    Boolean hasCheckedIn = reservation.getHasCheckedIn();
                    String status = reservation.getStatus();
                    Date checkOutTime = reservation.getCheckOutTime();
                    
                    // Check-outs Pendientes: 
                    // - hasCheckedIn == true
                    // - hasCheckedOut == false (o null)
                    // - status CONFIRMADA o EN_CURSO
                    if (hasCheckedIn != null && hasCheckedIn && 
                        (hasCheckedOut == null || !hasCheckedOut) &&
                        (status != null && ("CONFIRMADA".equals(status) || "EN_CURSO".equals(status)))) {
                        pendingCheckouts.add(reservation);
                    }
                    
                    // Procesados Recientemente:
                    // - hasCheckedIn == true
                    // - hasCheckedOut == true
                    // (SIN filtro de fecha ni status, porque hasCheckedOut es la señal principal)
                    if (hasCheckedIn != null && hasCheckedIn &&
                        hasCheckedOut != null && hasCheckedOut) {
                        processedCheckouts.add(reservation);
                    }
                    
                    // Resumen de Pagos Hoy:
                    // - hasCheckedIn == true
                    // - hasCheckedOut == true
                    // - checkOutTime de HOY (o updatedAt si checkOutTime es null)
                    // (SIN verificar status, porque hasCheckedOut es la señal principal)
                    Date dateToCheck = checkOutTime != null ? checkOutTime : reservation.getUpdatedAt();
                    if (hasCheckedIn != null && hasCheckedIn &&
                        hasCheckedOut != null && hasCheckedOut &&
                        dateToCheck != null &&
                        dateToCheck.compareTo(todayStart) >= 0 && 
                        dateToCheck.compareTo(tomorrowStart) < 0) {
                        // Calcular total procesado hoy
                        totalProcessedToday += reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0;
                        toursCompletedToday++;
                    }
                }
                
                // Ordenar procesados por fecha más reciente (checkOutTime, o updatedAt si checkOutTime es null)
                Collections.sort(processedCheckouts, new Comparator<Reservation>() {
                    @Override
                    public int compare(Reservation r1, Reservation r2) {
                        Date d1 = r1.getCheckOutTime();
                        Date d2 = r2.getCheckOutTime();
                        
                        // Si checkOutTime es null, usar updatedAt como alternativa
                        if (d1 == null) {
                            d1 = r1.getUpdatedAt();
                        }
                        if (d2 == null) {
                            d2 = r2.getUpdatedAt();
                        }
                        
                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d2.compareTo(d1); // Más reciente primero
                    }
                });
                
                // Actualizar contadores
                tvPendingCount.setText(String.valueOf(pendingCheckouts.size()));
                tvProcessedCount.setText(String.valueOf(processedCheckouts.size()));
                tvTotalProcessed.setText(String.format(Locale.US, "S/. %.2f", totalProcessedToday));
                tvToursCompleted.setText(String.valueOf(toursCompletedToday));
                tvPlatformCommission.setText(String.format(Locale.US, "S/. %.2f", totalProcessedToday * 0.10)); // 10% comisión
                
                // Actualizar adapters
                pendingAdapter.notifyDataSetChanged();
                processedAdapter.notifyDataSetChanged();
                
                Log.d(TAG, "Pendientes: " + pendingCheckouts.size() + ", Procesados recientemente: " + processedCheckouts.size() + 
                    ", Total procesado hoy: S/. " + totalProcessedToday + ", Tours completados hoy: " + toursCompletedToday);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar reservaciones", e);
                Toast.makeText(CheckoutAlertsActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void processCheckout(Reservation reservation) {
        // Verificar que ya haya hecho check-in
        if (reservation.getHasCheckedIn() == null || !reservation.getHasCheckedIn()) {
            Toast.makeText(CheckoutAlertsActivity.this, "El cliente debe hacer check-in primero", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Verificar que no haya hecho check-out ya
        if (reservation.getHasCheckedOut() != null && reservation.getHasCheckedOut()) {
            Toast.makeText(CheckoutAlertsActivity.this, "Este checkout ya fue procesado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Actualizar reserva: marcar hasCheckedOut = true, checkOutTime = ahora, y status = COMPLETADA
        reservation.setHasCheckedOut(true);
        reservation.setCheckOutTime(new Date());
        reservation.setStatus("COMPLETADA");
        
        firestoreManager.updateReservation(reservation, 
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Toast.makeText(CheckoutAlertsActivity.this, "✅ Checkout procesado", Toast.LENGTH_SHORT).show();
                    loadCheckoutData(); // Recargar datos
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error al procesar checkout", e);
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
            
            String details;
            if (showActions) {
                // Para pendientes: mostrar precio y status
                details = String.format(Locale.US, 
                    "S/. %.2f - %s", 
                    reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0.0,
                    reservation.getStatus() != null ? reservation.getStatus() : "");
            } else {
                // Para procesados: mostrar precio, status y hora de checkout (o updatedAt si checkOutTime es null)
                String checkoutTimeStr = "";
                Date timeToShow = reservation.getCheckOutTime();
                if (timeToShow == null) {
                    timeToShow = reservation.getUpdatedAt();
                }
                if (timeToShow != null) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    checkoutTimeStr = " • " + timeFormat.format(timeToShow);
                }
                details = String.format(Locale.US, 
                    "S/. %.2f - %s%s", 
                    reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0.0,
                    reservation.getStatus() != null ? reservation.getStatus() : "",
                    checkoutTimeStr);
            }
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

