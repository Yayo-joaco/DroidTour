package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

public class CheckoutAlertsActivity extends AppCompatActivity {
    
    private RecyclerView rvPendingCheckouts, rvProcessedCheckouts;
    private TextView tvPendingCount, tvProcessedCount;
    private TextView tvTotalProcessed, tvToursCompleted, tvPlatformCommission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_alerts);
        
        setupToolbar();
        initializeViews();
        setupRecyclerViews();
        loadCheckoutData();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        rvPendingCheckouts = findViewById(R.id.rv_pending_checkouts);
        rvProcessedCheckouts = findViewById(R.id.rv_processed_checkouts);
        
        tvPendingCount = findViewById(R.id.tv_pending_count);
        tvProcessedCount = findViewById(R.id.tv_processed_count);
        
        tvTotalProcessed = findViewById(R.id.tv_total_processed);
        tvToursCompleted = findViewById(R.id.tv_tours_completed);
        tvPlatformCommission = findViewById(R.id.tv_platform_commission);
    }
    
    private void setupRecyclerViews() {
        rvPendingCheckouts.setLayoutManager(new LinearLayoutManager(this));
        rvProcessedCheckouts.setLayoutManager(new LinearLayoutManager(this));
        
        // TODO: Configurar adapters para las listas de checkout
    }
    
    private void loadCheckoutData() {
        // TODO: Cargar datos reales desde base de datos
        // Por ahora mostrar datos de prueba
        tvPendingCount.setText("3");
        tvProcessedCount.setText("8");
        tvTotalProcessed.setText("S/. 1,250.00");
        tvToursCompleted.setText("8");
        tvPlatformCommission.setText("S/. 125.00");
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
