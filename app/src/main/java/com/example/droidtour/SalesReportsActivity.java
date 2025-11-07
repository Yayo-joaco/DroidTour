package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class SalesReportsActivity extends AppCompatActivity {
    
    private ChipGroup chipGroupPeriod;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private SalesReportsPagerAdapter pagerAdapter;
    private com.example.droidtour.utils.PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("ADMIN")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_sales_reports);
        
        setupToolbar();
        initializeViews();
        setupPeriodFilters();
        setupViewPager();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        chipGroupPeriod = findViewById(R.id.chip_group_period);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
    }
    
    private void setupPeriodFilters() {
        Chip chipDaily = findViewById(R.id.chip_daily);
        Chip chipMonthly = findViewById(R.id.chip_monthly);
        Chip chipAnnual = findViewById(R.id.chip_annual);
        
        chipGroupPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedPeriod;
            if (checkedId == R.id.chip_daily) {
                selectedPeriod = "Diario";
            } else if (checkedId == R.id.chip_monthly) {
                selectedPeriod = "Mensual";
            } else if (checkedId == R.id.chip_annual) {
                selectedPeriod = "Anual";
            } else {
                selectedPeriod = "Diario";
            }
            
            // TODO: Actualizar datos según el período seleccionado
            loadReportsForPeriod(selectedPeriod);
        });
    }
    
    private void setupViewPager() {
        pagerAdapter = new SalesReportsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Por Servicio");
                    break;
                case 1:
                    tab.setText("Por Tour");
                    break;
                case 2:
                    tab.setText("Resumen");
                    break;
            }
        }).attach();
    }
    
    private void loadReportsForPeriod(String period) {
        // TODO: Cargar datos de reportes según el período seleccionado
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Adapter para ViewPager2
    private static class SalesReportsPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        
        public SalesReportsPagerAdapter(androidx.fragment.app.FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new SalesByServiceFragment();
                case 1:
                    return new SalesByTourFragment();
                case 2:
                    return new SalesSummaryFragment();
                default:
                    return new SalesByServiceFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 3;
        }
    }
    
    private void redirectToLogin() {
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
