package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class QRScannerActivity extends AppCompatActivity {

    private Button btnManualEntry, btnToggleFlash, btnContinueScanning;
    private TextView tvScanResult, tvScanResultMessage, tvScansCount;
    private RecyclerView rvScanHistory;
    private ScanHistoryAdapter scanHistoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Escáner QR");
    }
    
    private void initializeViews() {
        btnManualEntry = findViewById(R.id.btn_manual_entry);
        btnToggleFlash = findViewById(R.id.btn_toggle_flash);
        btnContinueScanning = findViewById(R.id.btn_continue_scanning);
        tvScanResult = findViewById(R.id.tv_scan_result_title);
        tvScanResultMessage = findViewById(R.id.tv_scan_result_message);
        tvScansCount = findViewById(R.id.tv_scans_count);
        rvScanHistory = findViewById(R.id.rv_scan_history);
    }

    private void setupRecyclerView() {
        rvScanHistory.setLayoutManager(new LinearLayoutManager(this));
        scanHistoryAdapter = new ScanHistoryAdapter();
        rvScanHistory.setAdapter(scanHistoryAdapter);
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

// Adaptador para historial de escaneos
class ScanHistoryAdapter extends RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        android.view.View item = holder.itemView;
        TextView clientName = item.findViewById(R.id.tv_client_name);
        TextView scanType = item.findViewById(R.id.tv_scan_type_text);
        TextView scanTime = item.findViewById(R.id.tv_scan_time);
        TextView status = item.findViewById(R.id.tv_status);
        android.widget.ImageView scanTypeIcon = item.findViewById(R.id.iv_scan_type);
        android.view.View statusIndicator = item.findViewById(R.id.view_status_indicator);

        switch (position) {
            case 0:
                clientName.setText("Ana García Pérez");
                scanType.setText("Check-in");
                scanTime.setText("• 10:15 AM");
                status.setText("✓");
                scanTypeIcon.setImageResource(android.R.drawable.ic_menu_camera);
                statusIndicator.setBackgroundResource(R.drawable.circle_green);
                break;
            case 1:
                clientName.setText("Carlos Mendoza");
                scanType.setText("Check-in");
                scanTime.setText("• 10:18 AM");
                status.setText("✓");
                scanTypeIcon.setImageResource(android.R.drawable.ic_menu_camera);
                statusIndicator.setBackgroundResource(R.drawable.circle_green);
                break;
            case 2:
                clientName.setText("María López");
                scanType.setText("Check-out");
                scanTime.setText("• 12:30 PM");
                status.setText("✓");
                scanTypeIcon.setImageResource(android.R.drawable.ic_menu_camera);
                statusIndicator.setBackgroundResource(R.drawable.circle_orange);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View itemView) { super(itemView); }
    }
}
