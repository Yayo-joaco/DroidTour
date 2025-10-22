package com.example.droidtour;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class QRScannerActivity extends AppCompatActivity {

    private TextView tvScanStatus, tvTourName;
    private MaterialButton btnManualEntry, btnContinueScanning, btnToggleFlash;
    private com.google.android.material.card.MaterialCardView cardScanResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        initializeViews();
        setupToolbar();
        setupClickListeners();
        simulateScan();
    }

    private void initializeViews() {
        tvScanStatus = findViewById(R.id.tv_scan_status);
        tvTourName = findViewById(R.id.tv_tour_name);
        btnManualEntry = findViewById(R.id.btn_manual_entry);
        btnContinueScanning = findViewById(R.id.btn_continue_scanning);
        btnToggleFlash = findViewById(R.id.btn_toggle_flash);
        cardScanResult = findViewById(R.id.card_scan_result);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnManualEntry.setOnClickListener(v -> {
            // Show manual entry dialog
            android.widget.Toast.makeText(this, 
                "Entrada manual de código", 
                android.widget.Toast.LENGTH_SHORT).show();
        });

        btnToggleFlash.setOnClickListener(v -> {
            // Toggle flash
            android.widget.Toast.makeText(this, 
                "Flash activado/desactivado", 
                android.widget.Toast.LENGTH_SHORT).show();
        });

        btnContinueScanning.setOnClickListener(v -> {
            // Continue scanning more QRs
            cardScanResult.setVisibility(android.view.View.GONE);
            tvScanStatus.setText("Esperando QR de Check-in");
            android.widget.Toast.makeText(this, 
                "Listo para escanear siguiente QR", 
                android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void simulateScan() {
        // Simulate QR scan after 3 seconds
        new android.os.Handler().postDelayed(() -> {
            // Update status
            tvScanStatus.setText("✓ Check-in Completado");
            tvScanStatus.setTextColor(getColor(R.color.green));
            
            // Show success overlay
            cardScanResult.setVisibility(android.view.View.VISIBLE);
            
            // Update history count
            TextView tvScansCount = findViewById(R.id.tv_scans_count);
            if (tvScansCount != null) {
                tvScansCount.setText("4 escaneos"); // 3 + 1 nuevo
            }
            
            android.widget.Toast.makeText(this, 
                "¡Check-in exitoso!", 
                android.widget.Toast.LENGTH_SHORT).show();
        }, 3000);
    }
}
