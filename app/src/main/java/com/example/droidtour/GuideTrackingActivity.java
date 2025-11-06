package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GuideTrackingActivity extends AppCompatActivity {
    
    private MaterialCardView cardMap;
    private RecyclerView rvActiveGuides;
    private TextView tvActiveCount;
    private FloatingActionButton fabFilter;
    private com.example.droidtour.utils.PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea ADMIN o GUIDE
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("GUIDE"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_guide_tracking);
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadActiveGuides();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        cardMap = findViewById(R.id.card_map);
        rvActiveGuides = findViewById(R.id.rv_active_guides);
        tvActiveCount = findViewById(R.id.tv_active_count);
        fabFilter = findViewById(R.id.fab_filter);
    }
    
    private void setupClickListeners() {
        cardMap.setOnClickListener(v -> {
            Toast.makeText(this, "Abrir mapa completo", Toast.LENGTH_SHORT).show();
            // TODO: Implementar mapa completo con ubicaciones de guías
        });
        
        fabFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filtros de seguimiento", Toast.LENGTH_SHORT).show();
            // TODO: Mostrar dialog de filtros
        });
    }
    
    private void setupRecyclerView() {
        rvActiveGuides.setLayoutManager(new LinearLayoutManager(this));
        // TODO: Configurar adapter para lista de guías activos
    }
    
    private void loadActiveGuides() {
        // TODO: Cargar datos de guías activos desde base de datos
        // Por ahora mostrar datos de prueba
        tvActiveCount.setText("3 activos");
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
