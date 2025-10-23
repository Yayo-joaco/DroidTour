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
    
    private String tourName, companyName;
    private double pricePerPerson;
    
    // Storage Local
    private DatabaseHelper dbHelper;
    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_booking);

        // Inicializar Storage Local
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
        tourName = getIntent().getStringExtra("tour_name");
        companyName = getIntent().getStringExtra("company_name");
        pricePerPerson = getIntent().getDoubleExtra("price", 85.0);
        
        if (tourName == null) tourName = "Tour Increíble";
        if (companyName == null) companyName = "Empresa de Tours";
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
        String dateText = etTourDate.getText().toString().trim();
        String comments = etComments.getText().toString().trim();
        
        // Validaciones
        if (participantsText.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el número de personas", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (dateText.isEmpty()) {
            dateText = "28 Oct"; // Fecha por defecto
        }
        
        // Calcular datos de la reserva
        int participants = Integer.parseInt(participantsText);
        double totalPrice = pricePerPerson * participants;
        String qrCode = "QR-" + System.currentTimeMillis(); // Código QR único
        
        // ✅ GUARDAR EN BASE DE DATOS
        long reservationId = dbHelper.addReservation(
            tourName,           // nombre del tour
            companyName,        // empresa
            dateText,           // fecha
            "09:00 AM",         // hora por defecto
            "CONFIRMADA",       // estado
            totalPrice,         // precio total
            participants,       // número de personas
            qrCode              // código QR
        );
        
        // ✅ ENVIAR NOTIFICACIONES
        notificationHelper.sendReservationConfirmedNotification(tourName, dateText, qrCode);
        notificationHelper.sendPaymentConfirmedNotification(tourName, totalPrice);
        
        // Mostrar mensaje de éxito
        Toast.makeText(this, "¡Reserva confirmada! Código: " + qrCode, Toast.LENGTH_LONG).show();
        
        // Navegar a Mis Reservas
        Intent intent = new Intent(this, MyReservationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
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