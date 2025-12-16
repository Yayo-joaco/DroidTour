package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.droidtour.models.PaymentMethod;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class TourBookingActivity extends AppCompatActivity {
    private static final String TAG = "TourBookingActivity";

    private TextView tvTourName, tvCompanyName, tvPrice;
    private TextView tvServicePrice, tvTotalPrice, tvTourDate;
    private TextView tvPaymentMethodName, tvPaymentMethodInfo;
    private ImageView ivCardIcon;
    private LinearLayout layoutPaymentMethod;
    private MaterialButton btnConfirmBooking;
    
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private String currentUserId;
    private String tourId, tourName, companyId, companyName;
    private String tourDate, tourTime;
    private double pricePerPerson;
    private double servicePrice;
    
    // Payment method
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private PaymentMethod selectedPaymentMethod = null;
    
    // Helpers
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

        // Inicializar helpers
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
        tourDate = getIntent().getStringExtra("tour_date");
        tourTime = getIntent().getStringExtra("tour_time");
        pricePerPerson = getIntent().getDoubleExtra("price", 55.0);
        servicePrice = getIntent().getDoubleExtra("service_price", 0.0);
        
        if (tourName == null) tourName = "Tour Increíble";
        if (companyName == null) companyName = "Empresa de Tours";
        if (tourId == null) tourId = "TOUR001";
        if (companyId == null) companyId = "COMP001";
        if (tourDate == null) tourDate = "Por confirmar";
        if (tourTime == null) tourTime = "09:00";
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
        tvTourDate = findViewById(R.id.tv_tour_date_value);
        tvServicePrice = findViewById(R.id.tv_service_price);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        
        tvPaymentMethodName = findViewById(R.id.tv_payment_method_name);
        tvPaymentMethodInfo = findViewById(R.id.tv_payment_method_info);
        ivCardIcon = findViewById(R.id.iv_card_icon);
        layoutPaymentMethod = findViewById(R.id.layout_payment_method);
        
        btnConfirmBooking = findViewById(R.id.btn_confirm_booking);
        
        // Cargar métodos de pago
        loadPaymentMethods();
    }

    private void setupClickListeners() {
        btnConfirmBooking.setOnClickListener(v -> confirmBooking());
        
        layoutPaymentMethod.setOnClickListener(v -> showPaymentMethodDialog());
    }

    private void loadTourData() {
        tvTourName.setText(tourName);
        tvCompanyName.setText(companyName);
        tvPrice.setText("S/. " + String.format("%.2f", pricePerPerson));
        tvTourDate.setText(tourDate + " - " + tourTime);
        tvServicePrice.setText("S/. " + String.format("%.2f", servicePrice));
        calculateTotal();
    }

    private void calculateTotal() {
        double total = pricePerPerson + servicePrice;
        tvTotalPrice.setText("S/. " + String.format("%.2f", total));
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
        if (selectedPaymentMethod == null) {
            Toast.makeText(this, "Por favor selecciona un método de pago", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Deshabilitar botón para evitar doble clic
        btnConfirmBooking.setEnabled(false);
        btnConfirmBooking.setText("Procesando...");
        
        // Primero validar que el tour tenga guía asignado
        firestoreManager.getTourById(tourId, new com.example.droidtour.firebase.FirestoreManager.TourCallback() {
            @Override
            public void onSuccess(com.example.droidtour.models.Tour tour) {
                if (tour == null) {
                    btnConfirmBooking.setEnabled(true);
                    btnConfirmBooking.setText("Confirmar Reserva");
                    Toast.makeText(TourBookingActivity.this, "❌ Error: Tour no encontrado", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                
                // Validar que el tour sea público y tenga guía asignado
                Boolean isPublic = tour.getPublic();
                String assignedGuideId = tour.getAssignedGuideId();
                String assignedGuideName = tour.getAssignedGuideName();
                
                if (isPublic == null || !isPublic || assignedGuideId == null || assignedGuideId.trim().isEmpty()) {
                    btnConfirmBooking.setEnabled(true);
                    btnConfirmBooking.setText("Confirmar Reserva");
                    Toast.makeText(TourBookingActivity.this, 
                        "❌ Este tour no está disponible actualmente. No tiene un guía asignado.", 
                        Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                
                // Si pasó las validaciones, proceder a crear la reserva
                firestoreManager.getUser(currentUserId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                        
                        Reservation reservation = new Reservation(
                            currentUserId,
                            user.getFirstName() + " " + user.getLastName(),
                            user.getEmail(),
                            tourId,
                            tourName,
                            companyId,
                            companyName,
                            tourDate,
                            tourTime,
                            1, // Siempre 1 persona por reserva
                            pricePerPerson,
                            servicePrice
                        );
                        
                        // Establecer datos del guía en la reserva
                        reservation.setGuideId(assignedGuideId);
                        reservation.setGuideName(assignedGuideName != null ? assignedGuideName : "Guía asignado");
                        
                        reservation.setStatus("CONFIRMADA");
                        reservation.setPaymentStatus("CONFIRMADO");
                        
                        // Agregar información del método de pago
                        String last4 = selectedPaymentMethod.getCardNumber().substring(
                            selectedPaymentMethod.getCardNumber().length() - 4);
                        reservation.setPaymentMethod(selectedPaymentMethod.getCardType() + " ****" + last4);
                        reservation.setPaymentMethodId(selectedPaymentMethod.getPaymentMethodId());
                        
                        firestoreManager.createReservation(reservation, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                Reservation savedReservation = (Reservation) result;
                                
                                // Regenerar QR codes con el reservationId real
                                savedReservation.regenerateQRCodes();
                                firestoreManager.updateReservation(savedReservation, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object updateResult) {
                                        Log.d(TAG, "QR codes regenerados correctamente");
                                    }
                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "Error al regenerar QR codes", e);
                                    }
                                });
                                
                                // ✅ Incrementar expectedParticipants del tour
                                incrementTourParticipants();
                                
                                Toast.makeText(TourBookingActivity.this, "✅ ¡Reserva confirmada!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(TourBookingActivity.this, MyReservationsActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Error creando reserva", e);
                                btnConfirmBooking.setEnabled(true);
                                btnConfirmBooking.setText("Confirmar Reserva");
                                Toast.makeText(TourBookingActivity.this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error obteniendo datos del usuario", e);
                        btnConfirmBooking.setEnabled(true);
                        btnConfirmBooking.setText("Confirmar Reserva");
                        Toast.makeText(TourBookingActivity.this, "❌ Error obteniendo datos", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error obteniendo tour: " + error);
                btnConfirmBooking.setEnabled(true);
                btnConfirmBooking.setText("Confirmar Reserva");
                Toast.makeText(TourBookingActivity.this, "❌ Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Incrementa el contador expectedParticipants del tour cuando se crea una reserva confirmada
     */
    private void incrementTourParticipants() {
        Log.d(TAG, "🔄 Intentando incrementar expectedParticipants para tourId: " + tourId);
        
        firestoreManager.getTourById(tourId, new com.example.droidtour.firebase.FirestoreManager.TourCallback() {
            @Override
            public void onSuccess(com.example.droidtour.models.Tour tour) {
                if (tour == null) {
                    Log.e(TAG, "❌ Tour es null, no se puede incrementar");
                    return;
                }
                
                int currentCount = tour.getExpectedParticipants() != null ? tour.getExpectedParticipants() : 0;
                int newCount = currentCount + 1;
                
                Log.d(TAG, "📊 expectedParticipants actual: " + currentCount + ", nuevo: " + newCount);
                
                tour.setExpectedParticipants(newCount);
                
                firestoreManager.updateTour(tour, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "✅ expectedParticipants incrementado exitosamente a " + newCount);
                        Toast.makeText(TourBookingActivity.this, "Participantes actualizados: " + newCount, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "❌ Error actualizando expectedParticipants en Firebase", e);
                        Toast.makeText(TourBookingActivity.this, "Error actualizando contador de participantes", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "❌ Error obteniendo tour para incrementar participantes: " + error);
                Toast.makeText(TourBookingActivity.this, "Error al obtener tour: " + error, Toast.LENGTH_SHORT).show();
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