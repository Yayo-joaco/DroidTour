package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.managers.FileManager;

public class TourAdminMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private TextView tvPendingAlertsCount, tvActiveChatCount;

    // Storage
    private PreferencesManager prefsManager;
    private FileManager fileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new PreferencesManager(this);
        
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
        
        setContentView(R.layout.activity_tour_admin_main);

        // Inicializar storage
        fileManager = new FileManager(this);

        setupToolbar();
        setupDrawer();
        setupCardClickListeners();
        setupFab();
        initializeNotificationCounters();
        loadDashboardData();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        navigationView.setNavigationItemSelectedListener(this);
    }
    
    private void setupCardClickListeners() {
        // Company Info Card
        MaterialCardView companyInfoCard = findViewById(R.id.card_company_info);
        companyInfoCard.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, CompanyInfoActivity.class);
            startActivity(intent);
        });
        
        // Service Management Card
        MaterialCardView serviceManagementCard = findViewById(R.id.card_service_management);
        serviceManagementCard.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, CreateServiceActivity.class);
            startActivity(intent);
        });
        
        // Tour Management Card
        MaterialCardView tourManagementCard = findViewById(R.id.card_tour_management);
        tourManagementCard.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, CreateTourActivity.class);
            startActivity(intent);
        });
        
        // Guide Management Card
        MaterialCardView guideManagementCard = findViewById(R.id.card_guide_management);
        guideManagementCard.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, GuideManagementActivity.class);
            startActivity(intent);
        });
        
        // Guide Tracking Card
        MaterialCardView guideTrackingCard = findViewById(R.id.card_guide_tracking);
        guideTrackingCard.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, GuideTrackingActivity.class);
            startActivity(intent);
        });
        
        // Checkout Alerts Card
        MaterialCardView checkoutAlertsCard = findViewById(R.id.card_checkout_alerts);
        checkoutAlertsCard.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, CheckoutAlertsActivity.class);
            startActivity(intent);
        });
        
        // Sales Reports Card
        MaterialCardView salesReportsCard = findViewById(R.id.card_sales_reports);
        salesReportsCard.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, SalesReportsActivity.class);
            startActivity(intent);
        });
        
        // Customer Chat Card
        MaterialCardView customerChatCard = findViewById(R.id.card_customer_chat);
        customerChatCard.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, AdminChatListActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupFab() {
        FloatingActionButton fabCreateTour = findViewById(R.id.fab_create_tour);
        fabCreateTour.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, CreateTourActivity.class);
            startActivity(intent);
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_company_info) {
            startActivity(new Intent(this, CompanyInfoActivity.class));
        } else if (id == R.id.nav_guide_management) {
            startActivity(new Intent(this, GuideManagementActivity.class));
        } else if (id == R.id.nav_guide_tracking) {
            startActivity(new Intent(this, GuideTrackingActivity.class));
        } else if (id == R.id.nav_checkout_alerts) {
            startActivity(new Intent(this, CheckoutAlertsActivity.class));
        } else if (id == R.id.nav_sales_reports) {
            startActivity(new Intent(this, SalesReportsActivity.class));
        } else if (id == R.id.nav_customer_chat) {
            startActivity(new Intent(this, AdminChatListActivity.class));
        } else if (id == R.id.nav_logout) {
            performLogout();
        }
        
        drawerLayout.closeDrawers();
        return true;
    }
    
    private void initializeNotificationCounters() {
        tvPendingAlertsCount = findViewById(R.id.tv_pending_alerts_count);
        tvActiveChatCount = findViewById(R.id.tv_active_chats_count);
    }
    
    private void loadDashboardData() {
        // TODO: Cargar datos reales desde base de datos
        // Por ahora mostrar datos de prueba
        updatePendingAlertsCount(3);
        updateActiveChatCount(2);
    }
    
    private void updatePendingAlertsCount(int count) {
        if (tvPendingAlertsCount != null) {
            tvPendingAlertsCount.setText(String.valueOf(count));
            tvPendingAlertsCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }
    
    private void updateActiveChatCount(int count) {
        if (tvActiveChatCount != null) {
            tvActiveChatCount.setText(String.valueOf(count));
            tvActiveChatCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * Realizar logout completo
     */
    private void performLogout() {
        // 1. Limpiar SharedPreferences
        prefsManager.logout();

        // 2. Limpiar archivos de datos de usuario
        if (fileManager != null) {
            fileManager.limpiarDatosUsuario();
        }

        // 3. Mostrar mensaje de confirmación
        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

        // 4. Redirigir al LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar contadores cuando se regrese a la pantalla principal
        loadDashboardData();
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
