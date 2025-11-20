package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.example.droidtour.models.PaymentMethod;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentMethodsActivity extends AppCompatActivity {

    private static final String TAG = "PaymentMethodsActivity";
    private RecyclerView rvPaymentMethods;
    private PaymentMethodsAdapter paymentMethodsAdapter;
    private MaterialCardView cardAddNew;
    private TextView tvCardsCount;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private String currentUserId;
    private List<PaymentMethod> paymentMethodsList = new ArrayList<>();
    private List<Reservation> userReservations = new ArrayList<>();
    private Map<String, PaymentMethodStats> paymentMethodStatsMap = new HashMap<>();

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
        
        // Validar que el usuario sea CLIENT
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_payment_methods);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar Firebase
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);
        currentUserId = authManager.getCurrentUserId();
        
        // Si no hay usuario autenticado, usar del PreferencesManager
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = prefsManager.getUserId();
        }
        
        Log.d(TAG, "📱 PaymentMethodsActivity iniciada para userId: " + currentUserId);

        setupToolbar();
        initializeViews();
        setupRecyclerView(); // IMPORTANTE: Configurar RecyclerView ANTES de cargar datos
        setupClickListeners();
        
        // 🔥 Cargar datos desde Firestore
        loadUserReservations(); // Primero cargar reservas
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Métodos de Pago");
        }
    }

    private void initializeViews() {
        rvPaymentMethods = findViewById(R.id.rv_payment_methods);
        cardAddNew = findViewById(R.id.card_add_new);
        tvCardsCount = findViewById(R.id.tv_cards_count);
    }

    private void setupRecyclerView() {
        rvPaymentMethods.setLayoutManager(new LinearLayoutManager(this));
        paymentMethodsAdapter = new PaymentMethodsAdapter(paymentMethodsList, this::onPaymentMethodAction);
        rvPaymentMethods.setAdapter(paymentMethodsAdapter);
    }
    
    /**
     * 🔥 Cargar reservas del usuario para calcular estadísticas de uso
     */
    private void loadUserReservations() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Error: currentUserId es null o vacío");
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "🔄 Cargando reservas para userId: " + currentUserId);
        
        firestoreManager.getReservationsByUser(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                userReservations = (List<Reservation>) result;
                Log.d(TAG, "✅ Reservas cargadas: " + userReservations.size());
                
                // Calcular estadísticas por método de pago
                calculatePaymentMethodStats();
                
                // Ahora cargar los métodos de pago
                loadPaymentMethodsFromFirestore();
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando reservas", e);
                // Continuar sin estadísticas
                loadPaymentMethodsFromFirestore();
            }
        });
    }
    
    /**
     * 🔥 Calcular estadísticas de uso por método de pago
     */
    private void calculatePaymentMethodStats() {
        paymentMethodStatsMap.clear();
        
        for (Reservation reservation : userReservations) {
            String paymentMethodId = reservation.getPaymentMethodId();
            if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
                PaymentMethodStats stats = paymentMethodStatsMap.get(paymentMethodId);
                if (stats == null) {
                    stats = new PaymentMethodStats();
                    paymentMethodStatsMap.put(paymentMethodId, stats);
                }
                
                stats.totalSpent += reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0.0;
                stats.usageCount++;
                
                // Actualizar última fecha de uso
                if (reservation.getCreatedAt() != null) {
                    if (stats.lastUsedDate == null || reservation.getCreatedAt().after(stats.lastUsedDate)) {
                        stats.lastUsedDate = reservation.getCreatedAt();
                    }
                }
            }
        }
        
        Log.d(TAG, "📊 Estadísticas calculadas para " + paymentMethodStatsMap.size() + " métodos de pago");
    }
    
    /**
     * 🔥 Cargar métodos de pago desde Firestore
     */
    private void loadPaymentMethodsFromFirestore() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Error: currentUserId es null o vacío");
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "🔄 Cargando métodos de pago para userId: " + currentUserId);
        
        firestoreManager.getPaymentMethodsByUser(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                paymentMethodsList = (List<PaymentMethod>) result;
                
                Log.d(TAG, "✅ Métodos de pago cargados: " + paymentMethodsList.size());
                
                // Actualizar contador (null-safe y hacer visible)
                if (tvCardsCount != null) {
                    tvCardsCount.setText(String.valueOf(paymentMethodsList.size()));
                    tvCardsCount.setVisibility(android.view.View.VISIBLE);
                }
                
                // Actualizar RecyclerView con estadísticas
                if (paymentMethodsAdapter != null) {
                    paymentMethodsAdapter.setPaymentMethodStats(paymentMethodStatsMap);
                    paymentMethodsAdapter.updateData(paymentMethodsList);
                }
                
                if (paymentMethodsList.isEmpty()) {
                    Log.d(TAG, "⚠️ No hay métodos de pago registrados para este usuario");
                    Toast.makeText(PaymentMethodsActivity.this, 
                        "No tienes métodos de pago registrados", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error al cargar métodos de pago", e);
                Toast.makeText(PaymentMethodsActivity.this, 
                    "Error al cargar métodos de pago: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        cardAddNew.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPaymentMethodActivity.class);
            startActivity(intent);
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recargar métodos de pago cuando volvemos de agregar/editar
        loadUserReservations(); // Recargar todo desde el principio
    }

    private void onPaymentMethodAction(int position, String action) {
        if (position < 0 || position >= paymentMethodsList.size()) return;
        
        PaymentMethod paymentMethod = paymentMethodsList.get(position);
        
        switch (action) {
            case "set_default":
                setAsDefaultPaymentMethod(paymentMethod);
                break;
            case "edit":
                openEditPaymentMethod(paymentMethod);
                break;
            case "delete":
                deletePaymentMethod(paymentMethod, position);
                break;
        }
    }
    
    /**
     * 🔥 Abrir pantalla de edición
     */
    private void openEditPaymentMethod(PaymentMethod paymentMethod) {
        Intent intent = new Intent(this, EditPaymentMethodActivity.class);
        intent.putExtra("paymentMethodId", paymentMethod.getPaymentMethodId());
        intent.putExtra("cardHolderName", paymentMethod.getCardHolderName());
        intent.putExtra("cardNumber", paymentMethod.getCardNumber());
        intent.putExtra("cardType", paymentMethod.getCardType());
        intent.putExtra("expiryMonth", paymentMethod.getExpiryMonth());
        intent.putExtra("expiryYear", paymentMethod.getExpiryYear());
        startActivity(intent);
    }
    
    /**
     * 🔥 Establecer tarjeta como predeterminada
     */
    private void setAsDefaultPaymentMethod(PaymentMethod paymentMethod) {
        firestoreManager.setDefaultPaymentMethod(currentUserId, paymentMethod.getPaymentMethodId(), 
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Toast.makeText(PaymentMethodsActivity.this, 
                        "Tarjeta establecida como principal", Toast.LENGTH_SHORT).show();
                    loadPaymentMethodsFromFirestore(); // Recargar lista
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(PaymentMethodsActivity.this, 
                        "Error al establecer tarjeta principal", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    /**
     * 🔥 Eliminar método de pago
     */
    private void deletePaymentMethod(PaymentMethod paymentMethod, int position) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Tarjeta")
            .setMessage("¿Estás seguro de que deseas eliminar esta tarjeta?")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                firestoreManager.deletePaymentMethod(paymentMethod.getPaymentMethodId(), 
                    new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            Toast.makeText(PaymentMethodsActivity.this, 
                                "Tarjeta eliminada", Toast.LENGTH_SHORT).show();
                            paymentMethodsList.remove(position);
                            paymentMethodsAdapter.notifyItemRemoved(position);
                            tvCardsCount.setText(String.valueOf(paymentMethodsList.size()));
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(PaymentMethodsActivity.this, 
                                "Error al eliminar tarjeta", Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
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

/**
 * Clase helper para almacenar estadísticas de uso de métodos de pago
 */
class PaymentMethodStats {
    double totalSpent = 0.0;
    int usageCount = 0;
    java.util.Date lastUsedDate = null;
}

// Adaptador para métodos de pago
class PaymentMethodsAdapter extends RecyclerView.Adapter<PaymentMethodsAdapter.ViewHolder> {
    interface OnPaymentMethodAction { void onAction(int position, String action); }
    private final OnPaymentMethodAction onPaymentMethodAction;
    private List<PaymentMethod> paymentMethods;
    private Map<String, PaymentMethodStats> statsMap = new HashMap<>();

    PaymentMethodsAdapter(List<PaymentMethod> paymentMethods, OnPaymentMethodAction listener) {
        this.paymentMethods = paymentMethods;
        this.onPaymentMethodAction = listener;
    }
    
    public void updateData(List<PaymentMethod> newData) {
        this.paymentMethods = newData;
        notifyDataSetChanged();
    }
    
    public void setPaymentMethodStats(Map<String, PaymentMethodStats> stats) {
        this.statsMap = stats != null ? stats : new HashMap<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_method, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (paymentMethods == null || paymentMethods.isEmpty()) return;
        
        PaymentMethod paymentMethod = paymentMethods.get(position);
        
        // 🔥 Usar vistas cacheadas del ViewHolder (¡MUCHO MÁS EFICIENTE!)
        // Validar que no sean null antes de usar
        if (holder.tvCardNumber != null) {
            holder.tvCardNumber.setText(formatCardNumber(paymentMethod.getCardNumber()));
        }
        
        if (holder.tvCardHolder != null) {
            holder.tvCardHolder.setText(paymentMethod.getCardHolderName() != null ? 
                paymentMethod.getCardHolderName() : "N/A");
        }
        
        if (holder.tvExpiryDate != null) {
            String expiry = (paymentMethod.getExpiryMonth() != null ? paymentMethod.getExpiryMonth() : "??") + 
                            "/" + 
                            (paymentMethod.getExpiryYear() != null ? paymentMethod.getExpiryYear() : "????");
            holder.tvExpiryDate.setText(expiry);
        }
        
        // Calcular uso de la tarjeta dinámicamente desde estadísticas reales
        PaymentMethodStats stats = statsMap.get(paymentMethod.getPaymentMethodId());
        
        if (holder.tvUsageInfo != null) {
            if (stats != null && stats.usageCount > 0) {
                String usageText = stats.usageCount == 1 ? "Usada en 1 reserva" : "Usada en " + stats.usageCount + " reservas";
                holder.tvUsageInfo.setText(usageText);
            } else {
                holder.tvUsageInfo.setText("Sin uso registrado");
            }
        }
        
        if (holder.tvLastUsed != null) {
            if (stats != null && stats.lastUsedDate != null) {
                holder.tvLastUsed.setText("Última vez: " + formatDate(stats.lastUsedDate));
            } else {
                holder.tvLastUsed.setText("Última vez: N/A");
            }
        }
        
        if (holder.tvTotalSpent != null) {
            if (stats != null && stats.totalSpent > 0) {
                holder.tvTotalSpent.setText("S/. " + String.format("%.2f", stats.totalSpent));
            } else {
                holder.tvTotalSpent.setText("S/. 0.00");
            }
        }

        // Asignar icono de marca según el tipo de tarjeta
        if (holder.ivCardType != null) {
            String type = paymentMethod.getCardType() != null ? paymentMethod.getCardType().toLowerCase() : "";
            int brandRes = R.drawable.ic_visa_logo; // fallback
            if (type.contains("visa")) {
                brandRes = R.drawable.ic_visa_logo;
            } else if (type.contains("master")) {
                brandRes = R.drawable.ic_mastercard_logo;
            } else if (type.contains("american") || type.contains("amex")) {
                brandRes = R.drawable.ic_amex_logo;
            }
            holder.ivCardType.setImageResource(brandRes);
        }

        // Mostrar badge de default solo para tarjeta principal
        Boolean isDefault = paymentMethod.getIsDefault();
        if (holder.tvDefaultBadge != null && holder.btnSetDefault != null) {
            if (isDefault != null && isDefault) {
                holder.tvDefaultBadge.setVisibility(android.view.View.VISIBLE);
                holder.btnSetDefault.setVisibility(android.view.View.GONE);
            } else {
                holder.tvDefaultBadge.setVisibility(android.view.View.GONE);
                holder.btnSetDefault.setVisibility(android.view.View.VISIBLE);
            }
        }

        // Configurar listeners (null-safe)
        if (holder.btnSetDefault != null) {
            holder.btnSetDefault.setOnClickListener(v -> onPaymentMethodAction.onAction(holder.getAdapterPosition(), "set_default"));
        }
        if (holder.btnEditCard != null) {
            holder.btnEditCard.setOnClickListener(v -> onPaymentMethodAction.onAction(holder.getAdapterPosition(), "edit"));
        }
        if (holder.btnDeleteCard != null) {
            holder.btnDeleteCard.setOnClickListener(v -> onPaymentMethodAction.onAction(holder.getAdapterPosition(), "delete"));
        }
    }
    
    private String formatCardNumber(String cardNumber) {
        if (cardNumber == null) return "****";
        // Ya viene enmascarado de Firestore (ej: "****1234")
        // Agregar espacios para mejor legibilidad
        if (cardNumber.length() >= 8) {
            return cardNumber.substring(0, 4) + " **** **** " + cardNumber.substring(4);
        }
        return cardNumber;
    }
    
    private String formatDate(java.util.Date date) {
        if (date == null) return "N/A";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public int getItemCount() { 
        return paymentMethods != null ? paymentMethods.size() : 0; 
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // 🔥 Cachear todas las vistas para evitar findViewById repetidos
        android.widget.ImageView ivCardType;
        TextView tvCardNumber;
        TextView tvCardHolder;
        TextView tvExpiryDate;
        TextView tvUsageInfo;  // ← NUEVO
        TextView tvLastUsed;
        TextView tvTotalSpent;
        TextView tvDefaultBadge;
        MaterialButton btnSetDefault;
        MaterialButton btnEditCard;
        MaterialButton btnDeleteCard;
        
        ViewHolder(android.view.View v) {
            super(v);
            // Inicializar todas las vistas UNA SOLA VEZ
            ivCardType = v.findViewById(R.id.iv_card_type);
            tvCardNumber = v.findViewById(R.id.tv_card_number);
            tvCardHolder = v.findViewById(R.id.tv_card_holder);
            tvExpiryDate = v.findViewById(R.id.tv_expiry_date);
            tvUsageInfo = v.findViewById(R.id.tv_usage_info);  // ← NUEVO
            tvLastUsed = v.findViewById(R.id.tv_last_used);
            tvTotalSpent = v.findViewById(R.id.tv_total_spent);
            tvDefaultBadge = v.findViewById(R.id.tv_default_badge);
            btnSetDefault = v.findViewById(R.id.btn_set_default);
            btnEditCard = v.findViewById(R.id.btn_edit_card);
            btnDeleteCard = v.findViewById(R.id.btn_delete_card);
        }
    }
}
