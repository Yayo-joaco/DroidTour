package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.droidtour.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class ClientQRCodesActivity extends AppCompatActivity {
    
    private TextView tvTourName, tvTourDate, tvReservationCode;
    private TextView tvCheckinStatus, tvCheckoutStatus;
    private MaterialButton btnShareCheckin, btnSaveCheckin;
    private MaterialButton btnShareCheckout, btnSaveCheckout;
    
    private int reservationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_qr_codes);

        getIntentData();
        setupToolbar();
        initializeViews();
        setupTourData();
        setupClickListeners();
    }

    private void getIntentData() {
        reservationId = getIntent().getIntExtra("reservation_id", 0);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Códigos QR");
        }
    }

    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvTourDate = findViewById(R.id.tv_tour_date);
        tvReservationCode = findViewById(R.id.tv_reservation_code);
        tvCheckinStatus = findViewById(R.id.tv_checkin_status);
        tvCheckoutStatus = findViewById(R.id.tv_checkout_status);
        btnShareCheckin = findViewById(R.id.btn_share_checkin);
        btnSaveCheckin = findViewById(R.id.btn_save_checkin);
        btnShareCheckout = findViewById(R.id.btn_share_checkout);
        btnSaveCheckout = findViewById(R.id.btn_save_checkout);
    }

    private void setupTourData() {
        String[] tourNames = {
            "City Tour Lima Centro",
            "Machu Picchu Full Day",
            "Islas Ballestas",
            "Cañón del Colca"
        };
        
        String[] dates = {
            "15 Dic, 2024 • 09:00 AM",
            "18 Dic, 2024 • 06:00 AM",
            "20 Dic, 2024 • 08:00 AM",
            "22 Dic, 2024 • 07:30 AM"
        };
        
        String[] codes = {"#DT2024001", "#DT2024002", "#DT2024003", "#DT2024004"};

        int index = reservationId % tourNames.length;
        
        tvTourName.setText(tourNames[index]);
        tvTourDate.setText(dates[index]);
        tvReservationCode.setText(codes[index]);
        
        // Set QR status based on reservation
        if (reservationId < 2) {
            tvCheckinStatus.setText("LISTO");
            tvCheckoutStatus.setText("PENDIENTE");
        } else {
            tvCheckinStatus.setText("USADO");
            tvCheckoutStatus.setText("USADO");
        }
    }

    private void setupClickListeners() {
        btnShareCheckin.setOnClickListener(v -> {
            shareQRCode("Check-in");
        });

        btnSaveCheckin.setOnClickListener(v -> {
            saveQRCode("Check-in");
        });

        btnShareCheckout.setOnClickListener(v -> {
            shareQRCode("Check-out");
        });

        btnSaveCheckout.setOnClickListener(v -> {
            saveQRCode("Check-out");
        });
    }

    private void shareQRCode(String type) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Código QR " + type + " para tour: " + tvTourName.getText());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Código QR " + type);
        startActivity(Intent.createChooser(shareIntent, "Compartir QR " + type));
    }

    private void saveQRCode(String type) {
        Toast.makeText(this, "QR " + type + " guardado en galería", Toast.LENGTH_SHORT).show();
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

