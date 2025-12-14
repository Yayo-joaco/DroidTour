package com.example.droidtour.client;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.PaymentMethod;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddPaymentMethodActivity extends AppCompatActivity {

    private static final String TAG = "AddPaymentMethod";
    
    private TextInputLayout tilCardHolder, tilCardNumber, tilExpiryMonth, tilExpiryYear, tilCvv;
    private TextInputEditText etCardHolder, etCardNumber, etExpiryMonth, etExpiryYear, etCvv;
    private MaterialButton btnSaveCard;
    
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_payment_method);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        prefsManager = new PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = prefsManager.getUserId();

        setupToolbar();
        initializeViews();
        setupListeners();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Agregar Tarjeta");
        }
    }

    private void initializeViews() {
        tilCardHolder = findViewById(R.id.til_card_holder);
        tilCardNumber = findViewById(R.id.til_card_number);
        tilExpiryMonth = findViewById(R.id.til_expiry_month);
        tilExpiryYear = findViewById(R.id.til_expiry_year);
        tilCvv = findViewById(R.id.til_cvv);

        etCardHolder = findViewById(R.id.et_card_holder);
        etCardNumber = findViewById(R.id.et_card_number);
        etExpiryMonth = findViewById(R.id.et_expiry_month);
        etExpiryYear = findViewById(R.id.et_expiry_year);
        etCvv = findViewById(R.id.et_cvv);

        btnSaveCard = findViewById(R.id.btn_save_card);
    }

    private void setupListeners() {
        // Auto-format card number (add spaces every 4 digits)
        etCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                String input = s.toString().replaceAll("\\s", "");
                StringBuilder formatted = new StringBuilder();
                
                for (int i = 0; i < input.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(input.charAt(i));
                }

                etCardNumber.removeTextChangedListener(this);
                etCardNumber.setText(formatted.toString());
                etCardNumber.setSelection(formatted.length());
                etCardNumber.addTextChangedListener(this);

                isFormatting = false;
            }
        });

        // Auto-complete year (2025 → 25)
        etExpiryYear.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (input.length() == 4) {
                    // Si escribe "2025", convertir a "25"
                    if (input.startsWith("20")) {
                        etExpiryYear.removeTextChangedListener(this);
                        etExpiryYear.setText(input.substring(2));
                        etExpiryYear.setSelection(2);
                        etExpiryYear.addTextChangedListener(this);
                    }
                }
            }
        });

        btnSaveCard.setOnClickListener(v -> validateAndSaveCard());
    }

    private void validateAndSaveCard() {
        // Clear previous errors
        tilCardHolder.setError(null);
        tilCardNumber.setError(null);
        tilExpiryMonth.setError(null);
        tilExpiryYear.setError(null);
        tilCvv.setError(null);

        String cardHolder = etCardHolder.getText().toString().trim();
        String cardNumber = etCardNumber.getText().toString().replaceAll("\\s", "");
        String expiryMonth = etExpiryMonth.getText().toString().trim();
        String expiryYear = etExpiryYear.getText().toString().trim();
        String cvv = etCvv.getText().toString().trim();

        boolean isValid = true;

        // Validate card holder
        if (cardHolder.isEmpty()) {
            tilCardHolder.setError("Ingresa el nombre del titular");
            isValid = false;
        }

        // Validate card number
        if (cardNumber.isEmpty()) {
            tilCardNumber.setError("Ingresa el número de tarjeta");
            isValid = false;
        } else if (cardNumber.length() < 13 || cardNumber.length() > 19) {
            tilCardNumber.setError("Número de tarjeta inválido");
            isValid = false;
        } else if (!isValidLuhn(cardNumber)) {
            tilCardNumber.setError("Número de tarjeta inválido (Luhn)");
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

        // Validate CVV
        if (cvv.isEmpty()) {
            tilCvv.setError("Ingresa el CVV");
            isValid = false;
        } else if (cvv.length() < 3 || cvv.length() > 4) {
            tilCvv.setError("CVV inválido");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Pad month with leading zero if needed
        if (expiryMonth.length() == 1) {
            expiryMonth = "0" + expiryMonth;
        }

        // Convert year to full format for storage
        String fullYear = "20" + expiryYear;

        // Detect card type
        String cardType = detectCardType(cardNumber);

        // Create PaymentMethod
        PaymentMethod paymentMethod = new PaymentMethod(
            currentUserId,
            cardHolder.toUpperCase(),
            cardNumber,
            cardType,
            expiryMonth,
            fullYear
        );
        paymentMethod.setCvv(cvv); // Solo para demo, NO almacenar en producción

        // Save to Firestore
        btnSaveCard.setEnabled(false);
        btnSaveCard.setText("Guardando...");

        firestoreManager.addPaymentMethod(paymentMethod, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                android.widget.Toast.makeText(AddPaymentMethodActivity.this, 
                    "✅ Tarjeta agregada exitosamente", android.widget.Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                android.widget.Toast.makeText(AddPaymentMethodActivity.this, 
                    "❌ Error al agregar tarjeta: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                btnSaveCard.setEnabled(true);
                btnSaveCard.setText("Guardar Tarjeta");
            }
        });
    }

    /**
     * Detectar tipo de tarjeta por el primer dígito
     */
    private String detectCardType(String cardNumber) {
        if (cardNumber.startsWith("4")) {
            return "VISA";
        } else if (cardNumber.startsWith("5")) {
            return "MASTERCARD";
        } else if (cardNumber.startsWith("3")) {
            return "AMERICAN EXPRESS";
        } else {
            return "OTRO";
        }
    }

    /**
     * Validar número de tarjeta con algoritmo de Luhn
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
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

