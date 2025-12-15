package com.example.droidtour.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.superadmin.adapters.CompaniesStatsAdapter;
import com.example.droidtour.superadmin.managers.CompanyStatsRepository;
import com.example.droidtour.superadmin.models.CompanyStats;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CompaniesReportsActivity extends AppCompatActivity {
    
    private static final String TAG = "CompaniesReports";
    
    private PreferencesManager prefsManager;
    private RecyclerView rvCompaniesStats;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmptyState;
    private CircularProgressIndicator loadingIndicator;
    private CompaniesStatsAdapter adapter;
    private CompanyStatsRepository repository;
    private List<CompanyStats> companiesStats;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefsManager = new PreferencesManager(this);
        
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("SUPERADMIN") && !userType.equals("ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_companies_reports);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
        loadCompaniesStats();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void initializeViews() {
        rvCompaniesStats = findViewById(R.id.rv_companies_stats);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        loadingIndicator = findViewById(R.id.loading_indicator);
        
        companiesStats = new ArrayList<>();
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        repository = new CompanyStatsRepository(db);
        
        swipeRefresh.setOnRefreshListener(() -> {
            loadCompaniesStats();
        });
    }
    
    private void setupRecyclerView() {
        adapter = new CompaniesStatsAdapter(companiesStats);
        rvCompaniesStats.setLayoutManager(new LinearLayoutManager(this));
        rvCompaniesStats.setAdapter(adapter);
    }
    
    private void loadCompaniesStats() {
        showLoading(true);
        
        repository.loadCompaniesWithStats(new CompanyStatsRepository.CompanyStatsCallback() {
            @Override
            public void onSuccess(List<CompanyStats> stats) {
                Log.d(TAG, "Estadísticas cargadas: " + stats.size() + " empresas");
                
                companiesStats.clear();
                companiesStats.addAll(stats);
                adapter.notifyDataSetChanged();
                
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                
                if (companiesStats.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error cargando estadísticas", error);
                Toast.makeText(CompaniesReportsActivity.this,
                    "Error al cargar estadísticas: " + error.getMessage(),
                    Toast.LENGTH_SHORT).show();
                
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                showEmptyState(true);
            }
        });
    }
    
    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (rvCompaniesStats != null) {
            rvCompaniesStats.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
    
    private void showEmptyState(boolean show) {
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (rvCompaniesStats != null) {
            rvCompaniesStats.setVisibility(show ? View.GONE : View.VISIBLE);
        }
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
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

