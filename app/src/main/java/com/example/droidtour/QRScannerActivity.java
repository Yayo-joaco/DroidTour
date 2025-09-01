package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class QRScannerActivity extends AppCompatActivity {
    
    // private MaterialCardView cardScannerPreview;
    // private Button btnStartScan;
    private Button btnManualEntry;
    private TextView tvScanResult;
    private TextView tvScanResultMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Escáner QR");
    }
    
    private void initializeViews() {
        // Usar los IDs que realmente existen en el layout
        // cardScannerPreview = findViewById(R.id.card_scanner_preview);
        // btnStartScan = findViewById(R.id.btn_start_scan);
        btnManualEntry = findViewById(R.id.btn_manual_entry);
        tvScanResult = findViewById(R.id.tv_scan_result_title); // Usar título como resultado
        tvScanResultMessage = findViewById(R.id.tv_scan_result_message);
    }
    
    private void setupClickListeners() {
        // btnStartScan.setOnClickListener(v -> {
        //     startQRScan();
        // });
        
        btnManualEntry.setOnClickListener(v -> {
            // TODO: Mostrar dialog para entrada manual de código
            Toast.makeText(this, "Entrada manual de código", Toast.LENGTH_SHORT).show();
        });
        
        // cardScannerPreview.setOnClickListener(v -> {
        //     startQRScan();
        // });
        
        // Simular escaneo automático para demo
        startQRScan();
    }
    
    private void startQRScan() {
        // TODO: Implementar escáner QR real
        Toast.makeText(this, "Iniciando escáner QR...", Toast.LENGTH_SHORT).show();
        
        // Simulación de escaneo exitoso
        simulateSuccessfulScan();
    }
    
    private void simulateSuccessfulScan() {
        // Simular resultado de escaneo para demostración
        tvScanResult.setText("QR12345678");
        tvScanResultMessage.setText("Cliente registrado correctamente");
        
        // TODO: Procesar el código QR escaneado
        // - Verificar validez del código
        // - Registrar check-in/check-out
        // - Actualizar estado del tour
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
