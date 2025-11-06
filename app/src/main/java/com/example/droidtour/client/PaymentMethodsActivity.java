package com.example.droidtour.client;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.PaymentMethod;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class PaymentMethodsActivity extends AppCompatActivity {

    private RecyclerView rvPaymentMethods;
    private PaymentMethodsAdapter paymentMethodsAdapter;
    private MaterialCardView cardAddNew;
    private TextView tvCardsCount;
    
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private String currentUserId;
    private List<PaymentMethod> paymentMethodsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = authManager.getCurrentUserId();
        
        // 🔥 TEMPORAL: Para testing sin login
        if (currentUserId == null) {
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", Toast.LENGTH_SHORT).show();
        }

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        loadPaymentMethodsFromFirebase();
        setupClickListeners();
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

    private void loadPaymentMethodsFromFirebase() {
        firestoreManager.getPaymentMethodsByUser(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                paymentMethodsList.clear();
                paymentMethodsList.addAll((List<PaymentMethod>) result);
                paymentMethodsAdapter.notifyDataSetChanged();
                tvCardsCount.setText(String.valueOf(paymentMethodsList.size()));
                
                if (paymentMethodsList.isEmpty()) {
                    Toast.makeText(PaymentMethodsActivity.this, "No hay tarjetas registradas", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PaymentMethodsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e("PaymentMethods", "Error cargando tarjetas", e);
            }
        });
    }

    private void setupClickListeners() {
        cardAddNew.setOnClickListener(v -> {
            Toast.makeText(this, "Agregar nueva tarjeta próximamente", Toast.LENGTH_SHORT).show();
        });
    }

    private void onPaymentMethodAction(PaymentMethod card, String action) {
        switch (action) {
            case "set_default":
                firestoreManager.setDefaultPaymentMethod(currentUserId, card.getPaymentMethodId(), new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(PaymentMethodsActivity.this, "Tarjeta establecida como principal", Toast.LENGTH_SHORT).show();
                        loadPaymentMethodsFromFirebase();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(PaymentMethodsActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case "edit":
                Toast.makeText(this, "Editar tarjeta próximamente", Toast.LENGTH_SHORT).show();
                break;
            case "delete":
                firestoreManager.deletePaymentMethod(card.getPaymentMethodId(), new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(PaymentMethodsActivity.this, "Tarjeta eliminada", Toast.LENGTH_SHORT).show();
                        loadPaymentMethodsFromFirebase();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(PaymentMethodsActivity.this, "Error eliminando", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

// Adaptador para métodos de pago
class PaymentMethodsAdapter extends RecyclerView.Adapter<PaymentMethodsAdapter.ViewHolder> {
    interface OnPaymentMethodAction { void onAction(PaymentMethod card, String action); }
    private final List<PaymentMethod> paymentMethods;
    private final OnPaymentMethodAction onPaymentMethodAction;

    PaymentMethodsAdapter(List<PaymentMethod> paymentMethods, OnPaymentMethodAction listener) {
        this.paymentMethods = paymentMethods;
        this.onPaymentMethodAction = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_method, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PaymentMethod card = paymentMethods.get(position);
        
        android.widget.ImageView ivCardType = holder.itemView.findViewById(R.id.iv_card_type);
        TextView cardNumber = holder.itemView.findViewById(R.id.tv_card_number);
        TextView cardHolder = holder.itemView.findViewById(R.id.tv_card_holder);
        TextView expiryDate = holder.itemView.findViewById(R.id.tv_expiry_date);
        TextView lastUsed = holder.itemView.findViewById(R.id.tv_last_used);
        TextView totalSpent = holder.itemView.findViewById(R.id.tv_total_spent);
        TextView defaultBadge = holder.itemView.findViewById(R.id.tv_default_badge);
        MaterialButton btnSetDefault = holder.itemView.findViewById(R.id.btn_set_default);
        MaterialButton btnEditCard = holder.itemView.findViewById(R.id.btn_edit_card);
        MaterialButton btnDeleteCard = holder.itemView.findViewById(R.id.btn_delete_card);

        cardNumber.setText(card.getCardNumber());
        cardHolder.setText(card.getCardHolderName());
        expiryDate.setText(card.getExpiryMonth() + "/" + card.getExpiryYear());
        lastUsed.setText("Última vez: " + (card.getCreatedAt() != null ? new java.text.SimpleDateFormat("dd MMM, yyyy").format(card.getCreatedAt()) : "N/A"));
        totalSpent.setText("S/. 0");

        int brandRes = R.drawable.ic_visa_logo;
        if (card.getCardType().contains("MASTERCARD")) {
            brandRes = R.drawable.ic_mastercard_logo;
        } else if (card.getCardType().contains("AMEX")) {
            brandRes = R.drawable.ic_amex_logo;
        }
        if (ivCardType != null) ivCardType.setImageResource(brandRes);

        if (card.getDefault()) {
            defaultBadge.setVisibility(android.view.View.VISIBLE);
            btnSetDefault.setVisibility(android.view.View.GONE);
        } else {
            defaultBadge.setVisibility(android.view.View.GONE);
            btnSetDefault.setVisibility(android.view.View.VISIBLE);
        }

        btnSetDefault.setOnClickListener(v -> onPaymentMethodAction.onAction(card, "set_default"));
        btnEditCard.setOnClickListener(v -> onPaymentMethodAction.onAction(card, "edit"));
        btnDeleteCard.setOnClickListener(v -> onPaymentMethodAction.onAction(card, "delete"));
    }

    @Override
    public int getItemCount() { return paymentMethods.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View v) { super(v); }
    }
}
