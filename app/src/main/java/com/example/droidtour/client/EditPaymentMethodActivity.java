package com.example.droidtour.client;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

public class EditPaymentMethodActivity extends AppCompatActivity {

    private static final String TAG = "EditPaymentMethod";
    
    private TextInputLayout tilCardHolder, tilExpiryMonth, tilExpiryYear;
    private TextInputEditText etCardHolder, etExpiryMonth, etExpiryYear;
    private MaterialButton btnUpdateCard;
    
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    
    private String paymentMethodId;
    private String cardNumber;
    private String cardType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_payment_method);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        prefsManager = new PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();

        // Get payment method data from intent
        paymentMethodId = getIntent().getStringExtra("paymentMethodId");
        String cardHolder = getIntent().getStringExtra("cardHolderName");
        cardNumber = getIntent().getStringExtra("cardNumber");
        cardType = getIntent().getStringExtra("cardType");
        String expiryMonth = getIntent().getStringExtra("expiryMonth");
        String expiryYear = getIntent().getStringExtra("expiryYear");

        if (paymentMethodId == null) {
            Toast.makeText(this, "Error: No se encontró la tarjeta", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        populateFields(cardHolder, expiryMonth, expiryYear);
        setupListeners();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Editar Tarjeta");
        }
    }

    private void initializeViews() {
        tilCardHolder = findViewById(R.id.til_card_holder);
        tilExpiryMonth = findViewById(R.id.til_expiry_month);
        tilExpiryYear = findViewById(R.id.til_expiry_year);

        etCardHolder = findViewById(R.id.et_card_holder);
        etExpiryMonth = findViewById(R.id.et_expiry_month);
        etExpiryYear = findViewById(R.id.et_expiry_year);

        btnUpdateCard = findViewById(R.id.btn_update_card);
    }

    private void populateFields(String cardHolder, String expiryMonth, String expiryYear) {
        etCardHolder.setText(cardHolder);
        etExpiryMonth.setText(expiryMonth);
        
        // Convert full year to YY format
        if (expiryYear != null && expiryYear.length() == 4) {
            etExpiryYear.setText(expiryYear.substring(2));
        } else {
            etExpiryYear.setText(expiryYear);
        }
    }

    private void setupListeners() {
        btnUpdateCard.setOnClickListener(v -> validateAndUpdateCard());
    }

    private void validateAndUpdateCard() {
        // Clear previous errors
        tilCardHolder.setError(null);
        tilExpiryMonth.setError(null);
        tilExpiryYear.setError(null);

        String cardHolder = etCardHolder.getText().toString().trim();
        String expiryMonth = etExpiryMonth.getText().toString().trim();
        String expiryYear = etExpiryYear.getText().toString().trim();

        boolean isValid = true;

        // Validate card holder
        if (cardHolder.isEmpty()) {
            tilCardHolder.setError("Ingresa el nombre del titular");
            isValid = false;
        }

        // Validate expiry month
        if (expiryMonth.isEmpty()) {
            tilExpiryMonth.setError("Ingresa el mes");
            isValid = false;
        } else {
            int month = Integer.parseInt(expiryMonth);
            if (month < 1 || month > 12) {
                tilExpiryMonth.setError("Mes inválido (1-12)");
                isValid = false;
            }
        }

        // Validate expiry year
        if (expiryYear.isEmpty()) {
            tilExpiryYear.setError("Ingresa el año");
            isValid = false;
        } else if (expiryYear.length() != 2) {
            tilExpiryYear.setError("Formato: YY");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Pad month with leading zero if needed
        if (expiryMonth.length() == 1) {
            expiryMonth = "0" + expiryMonth;
        }

        // Convert year to full format
        String fullYear = "20" + expiryYear;

        // Update payment method
        Map<String, Object> updates = new HashMap<>();
        updates.put("cardHolderName", cardHolder.toUpperCase());
        updates.put("expiryMonth", expiryMonth);
        updates.put("expiryYear", fullYear);

        btnUpdateCard.setEnabled(false);
        btnUpdateCard.setText("Actualizando...");

        /*firestoreManager.updatePaymentMethod(paymentMethodId, updates, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(EditPaymentMethodActivity.this, 
                    "✅ Tarjeta actualizada exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditPaymentMethodActivity.this, 
                    "❌ Error al actualizar tarjeta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                btnUpdateCard.setEnabled(true);
                btnUpdateCard.setText("Actualizar Tarjeta");
            }
        });

         */
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

