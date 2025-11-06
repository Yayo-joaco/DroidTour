package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.droidtour.database.DatabaseHelper;
import com.example.droidtour.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class TourBookingActivity extends AppCompatActivity {

    private TextView tvTourName, tvCompanyName, tvPrice;
    private TextView tvParticipantsCount, tvTotalPrice;
    private TextInputEditText etTourDate, etParticipants, etComments;
    private MaterialButton btnConfirmBooking;
    
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private String currentUserId;
    private String tourId, tourName, companyId, companyName;
    private double pricePerPerson;
    
    // Storage Local (deprecated)
    private DatabaseHelper dbHelper;
    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        
        etTourDate = findViewById(R.id.et_tour_date);
        etParticipants = findViewById(R.id.et_participants);
        etComments = findViewById(R.id.et_comments);
        
        btnConfirmBooking = findViewById(R.id.btn_confirm_booking);
    }

    private void setupClickListeners() {
        btnConfirmBooking.setOnClickListener(v -> confirmBooking());
        
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
        
        // dateText es final, no se puede reasignar
        
        int participants = Integer.parseInt(participantsText);
        
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
                reservation.setPaymentStatus("PENDIENTE");
                reservation.setSpecialRequests(comments);
                
                firestoreManager.createReservation(reservation, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(TourBookingActivity.this, "¡Reserva confirmada!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(TourBookingActivity.this, MyReservationsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(TourBookingActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(TourBookingActivity.this, "Error obteniendo datos", Toast.LENGTH_SHORT).show();
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
}