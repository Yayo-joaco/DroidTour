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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class PaymentMethodsActivity extends AppCompatActivity {

    private RecyclerView rvPaymentMethods;
    private PaymentMethodsAdapter paymentMethodsAdapter;
    private MaterialCardView cardAddNew;
    private TextView tvCardsCount;
    private com.example.droidtour.utils.PreferencesManager prefsManager;

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

        setupToolbar();
        initializeViews();
        setupHeaderData();
        setupRecyclerView();
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

    private void setupHeaderData() {
        tvCardsCount.setText("2");
    }

    private void setupRecyclerView() {
        rvPaymentMethods.setLayoutManager(new LinearLayoutManager(this));
        paymentMethodsAdapter = new PaymentMethodsAdapter(this::onPaymentMethodAction);
        rvPaymentMethods.setAdapter(paymentMethodsAdapter);
    }

    private void setupClickListeners() {
        cardAddNew.setOnClickListener(v -> {
            Toast.makeText(this, "Agregar nueva tarjeta", Toast.LENGTH_SHORT).show();
            // In real app: startActivity(new Intent(this, AddPaymentMethodActivity.class));
        });
    }

    private void onPaymentMethodAction(int position, String action) {
        switch (action) {
            case "set_default":
                Toast.makeText(this, "Tarjeta establecida como principal", Toast.LENGTH_SHORT).show();
                break;
            case "edit":
                Toast.makeText(this, "Editar tarjeta", Toast.LENGTH_SHORT).show();
                break;
            case "delete":
                Toast.makeText(this, "Tarjeta eliminada", Toast.LENGTH_SHORT).show();
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
    
    private void redirectToLogin() {
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

// Adaptador para métodos de pago
class PaymentMethodsAdapter extends RecyclerView.Adapter<PaymentMethodsAdapter.ViewHolder> {
    interface OnPaymentMethodAction { void onAction(int position, String action); }
    private final OnPaymentMethodAction onPaymentMethodAction;

    PaymentMethodsAdapter(OnPaymentMethodAction listener) {
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

        String[] cardTypes = {"Visa", "Mastercard", "American Express"};
        String[] cardNumbers = {"**** **** **** 1234", "**** **** **** 5678", "**** **** **** 9012"};
        String[] expiryDates = {"12/26", "08/25", "03/27"};
        String[] lastUsedDates = {"10 Dic, 2024", "05 Dic, 2024", "28 Nov, 2024"};
        String[] totalAmounts = {"S/. 425", "S/. 255", "S/. 180"};

        int index = position % cardTypes.length;

        cardNumber.setText(cardNumbers[index]);
        cardHolder.setText("ANA GARCIA PEREZ");
        expiryDate.setText(expiryDates[index]);
        lastUsed.setText("Última vez: " + lastUsedDates[index]);
        totalSpent.setText(totalAmounts[index]);

        // Asignar icono de marca según el tipo de tarjeta
        // La variable `cardTypes[index]` contiene el nombre del tipo (p.ej. "Visa", "Mastercard", "American Express").
        // Aquí mapeamos explícitamente esos nombres a los drawables existentes (más seguro y eficiente que getIdentifier).
        String type = cardTypes[index].toLowerCase();
        int brandRes = R.drawable.ic_visa_logo; // fallback
        if (type.contains("visa")) {
            brandRes = R.drawable.ic_visa_logo;
        } else if (type.contains("master")) {
            // Usar el drawable vectorial que tenemos: ic_mastercard_logo.xml
            brandRes = R.drawable.ic_mastercard_logo;
        } else if (type.contains("american") || type.contains("amex")) {
            // American Express -> ic_amex_logo.xml
            brandRes = R.drawable.ic_amex_logo;
        }
        if (ivCardType != null) ivCardType.setImageResource(brandRes);

        // Show default badge only for first card
        if (position == 0) {
            defaultBadge.setVisibility(android.view.View.VISIBLE);
            btnSetDefault.setVisibility(android.view.View.GONE);
        } else {
            defaultBadge.setVisibility(android.view.View.GONE);
            btnSetDefault.setVisibility(android.view.View.VISIBLE);
        }

        btnSetDefault.setOnClickListener(v -> onPaymentMethodAction.onAction(position, "set_default"));
        btnEditCard.setOnClickListener(v -> onPaymentMethodAction.onAction(position, "edit"));
        btnDeleteCard.setOnClickListener(v -> onPaymentMethodAction.onAction(position, "delete"));
    }

    @Override
    public int getItemCount() { return 3; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View v) { super(v); }
    }
}
