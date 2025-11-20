package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.droidtour.database.DatabaseHelper;
import com.example.droidtour.models.PaymentMethod;
import com.example.droidtour.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class TourBookingActivity extends AppCompatActivity {

    private TextView tvTourName, tvCompanyName, tvPrice;
    private TextView tvParticipantsCount, tvTotalPrice;
    private TextView tvPaymentMethodName, tvPaymentMethodInfo;
    private ImageView ivCardIcon;
    private LinearLayout layoutPaymentMethod;
    private TextInputEditText etTourDate, etParticipants, etComments;
    private MaterialButton btnConfirmBooking;
    
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private String currentUserId;
    private String tourId, tourName, companyId, companyName;
    private double pricePerPerson;
    
    // Payment method
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private PaymentMethod selectedPaymentMethod = null;
    
    // Storage Local (deprecated)
    private DatabaseHelper dbHelper;
    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        com.example.droidtour.utils.PreferencesManager prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
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
        
        setContentView(R.layout.activity_tour_booking);

        // Inicializar Firebase
        authManager = com.example.droidtour.firebase.FirebaseAuthManager.getInstance(this);
        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        currentUserId = authManager.getCurrentUserId();
        
        // 🔥 TEMPORAL: Para testing sin login
        if (currentUserId == null) {
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", Toast.LENGTH_SHORT).show();
        }

        // Inicializar Storage Local (deprecated)
        dbHelper = new DatabaseHelper(this);
        notificationHelper = new NotificationHelper(this);

        getIntentData();
        setupToolbar();
        initializeViews();
        setupClickListeners();
        loadTourData();
        calculateTotal();
    }

    private void getIntentData() {
        tourId = getIntent().getStringExtra("tour_id");
        tourName = getIntent().getStringExtra("tour_name");
        companyId = getIntent().getStringExtra("company_id");
        companyName = getIntent().getStringExtra("company_name");
        pricePerPerson = getIntent().getDoubleExtra("price", 85.0);
        
        if (tourName == null) tourName = "Tour Increíble";
        if (companyName == null) companyName = "Empresa de Tours";
        if (tourId == null) tourId = "TOUR001";
        if (companyId == null) companyId = "COMP001";
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reservar Tour");
        }
    }

    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvPrice = findViewById(R.id.tv_price);
        tvParticipantsCount = findViewById(R.id.tv_participants_count);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        
        tvPaymentMethodName = findViewById(R.id.tv_payment_method_name);
        tvPaymentMethodInfo = findViewById(R.id.tv_payment_method_info);
        ivCardIcon = findViewById(R.id.iv_card_icon);
        layoutPaymentMethod = findViewById(R.id.layout_payment_method);
        
        etTourDate = findViewById(R.id.et_tour_date);
        etParticipants = findViewById(R.id.et_participants);
        etComments = findViewById(R.id.et_comments);
        
        btnConfirmBooking = findViewById(R.id.btn_confirm_booking);
        
        // Cargar métodos de pago
        loadPaymentMethods();
    }

    private void setupClickListeners() {
        btnConfirmBooking.setOnClickListener(v -> confirmBooking());
        
        layoutPaymentMethod.setOnClickListener(v -> showPaymentMethodDialog());
        
        etParticipants.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                calculateTotal();
            }
        });
        
        etTourDate.setOnClickListener(v -> {
            // TODO: Abrir DatePicker
            Toast.makeText(this, "Selector de fecha próximamente", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadTourData() {
        tvTourName.setText(tourName);
        tvCompanyName.setText(companyName);
        tvPrice.setText("S/. " + String.format("%.2f", pricePerPerson) + " por persona");
    }

    private void calculateTotal() {
        try {
            String participantsText = etParticipants.getText().toString().trim();
            if (!participantsText.isEmpty()) {
                int participants = Integer.parseInt(participantsText);
                double total = pricePerPerson * participants;
                
                tvParticipantsCount.setText(String.valueOf(participants));
                tvTotalPrice.setText("S/. " + String.format("%.2f", total));
            }
        } catch (NumberFormatException e) {
            tvParticipantsCount.setText("1");
            tvTotalPrice.setText("S/. " + String.format("%.2f", pricePerPerson));
        }
    }

    private void loadPaymentMethods() {
        firestoreManager.getPaymentMethodsByUser(currentUserId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                paymentMethods = (List<PaymentMethod>) result;
                
                if (paymentMethods.isEmpty()) {
                    tvPaymentMethodName.setText("No tienes tarjetas");
                    tvPaymentMethodInfo.setText("Agrega una en Métodos de Pago");
                } else {
                    // Seleccionar tarjeta por defecto automáticamente
                    for (PaymentMethod pm : paymentMethods) {
                        if (pm.getIsDefault() != null && pm.getIsDefault()) {
                            selectedPaymentMethod = pm;
                            updatePaymentMethodUI();
                            break;
                        }
                    }
                    // Si no hay tarjeta por defecto, seleccionar la primera
                    if (selectedPaymentMethod == null && !paymentMethods.isEmpty()) {
                        selectedPaymentMethod = paymentMethods.get(0);
                        updatePaymentMethodUI();
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(TourBookingActivity.this, "Error cargando métodos de pago", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showPaymentMethodDialog() {
        if (paymentMethods.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("Sin métodos de pago")
                .setMessage("Necesitas agregar al menos una tarjeta para continuar")
                .setPositiveButton("Ir a Métodos de Pago", (dialog, which) -> {
                    Intent intent = new Intent(this, com.example.droidtour.client.PaymentMethodsActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
            return;
        }
        
        String[] options = new String[paymentMethods.size()];
        for (int i = 0; i < paymentMethods.size(); i++) {
            PaymentMethod pm = paymentMethods.get(i);
            String last4 = pm.getCardNumber().substring(pm.getCardNumber().length() - 4);
            options[i] = pm.getCardType() + " •••• " + last4;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Seleccionar método de pago")
            .setItems(options, (dialog, which) -> {
                selectedPaymentMethod = paymentMethods.get(which);
                updatePaymentMethodUI();
            })
            .show();
    }
    
    private void updatePaymentMethodUI() {
        if (selectedPaymentMethod == null) return;
        
        String last4 = selectedPaymentMethod.getCardNumber().substring(
            selectedPaymentMethod.getCardNumber().length() - 4);
        
        tvPaymentMethodName.setText(selectedPaymentMethod.getCardType());
        tvPaymentMethodInfo.setText("•••• " + last4);
        
        // Actualizar icono según tipo de tarjeta
        String cardType = selectedPaymentMethod.getCardType().toLowerCase();
        if (cardType.contains("visa")) {
            ivCardIcon.setColorFilter(getColor(R.color.primary));
        } else if (cardType.contains("mastercard")) {
            ivCardIcon.setColorFilter(getColor(R.color.orange));
        } else {
            ivCardIcon.setColorFilter(getColor(R.color.gray));
        }
    }
    
    private void confirmBooking() {
        String participantsText = etParticipants.getText().toString().trim();
        String dateTemp = etTourDate.getText().toString().trim();
        if (dateTemp.isEmpty()) {
            dateTemp = "2024-12-15";
        }
        final String dateText = dateTemp;
        final String comments = etComments.getText().toString().trim();
        
        if (participantsText.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el número de personas", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedPaymentMethod == null) {
            Toast.makeText(this, "Por favor selecciona un método de pago", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int participants = Integer.parseInt(participantsText);
        
        // Deshabilitar botón para evitar doble clic
        btnConfirmBooking.setEnabled(false);
        btnConfirmBooking.setText("Procesando...");
        
        firestoreManager.getUser(currentUserId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                
                com.example.droidtour.models.Reservation reservation = new com.example.droidtour.models.Reservation(
                    currentUserId,
                    user.getFirstName() + " " + user.getLastName(),
                    user.getEmail(),
                    tourId,
                    tourName,
                    companyId,
                    companyName,
                    dateText,
                    "09:00",
                    participants,
                    pricePerPerson
                );
                reservation.setStatus("CONFIRMADA");
                reservation.setPaymentStatus("CONFIRMADO");
                reservation.setSpecialRequests(comments);
                
                // Agregar información del método de pago
                String last4 = selectedPaymentMethod.getCardNumber().substring(
                    selectedPaymentMethod.getCardNumber().length() - 4);
                reservation.setPaymentMethod(selectedPaymentMethod.getCardType() + " ****" + last4);
                reservation.setPaymentMethodId(selectedPaymentMethod.getPaymentMethodId());
                
                firestoreManager.createReservation(reservation, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(TourBookingActivity.this, "✅ ¡Reserva confirmada!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(TourBookingActivity.this, MyReservationsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        btnConfirmBooking.setEnabled(true);
                        btnConfirmBooking.setText("Confirmar Reserva");
                        Toast.makeText(TourBookingActivity.this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                btnConfirmBooking.setEnabled(true);
                btnConfirmBooking.setText("Confirmar Reserva");
                Toast.makeText(TourBookingActivity.this, "❌ Error obteniendo datos", Toast.LENGTH_SHORT).show();
            }
        });
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
        Intent intent = new Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}