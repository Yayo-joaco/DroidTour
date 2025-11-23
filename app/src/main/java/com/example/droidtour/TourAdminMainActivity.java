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

import com.example.droidtour.admin.CompanyInfoActivity;
import com.example.droidtour.admin.CreateServiceActivity;
import com.example.droidtour.admin.CreateTourActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.droidtour.managers.PrefsManager;
import com.example.droidtour.managers.FileManager;

public class TourAdminMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private TextView tvWelcomeAdmin, tvCompanyName, tvPendingAlertsCount, tvActiveChatCount;
    private TextView tvAdminNameHeader, tvCompanyNameHeader;
    private MaterialCardView cardAlerts, cardCustomerChat, cardReports;
    private MaterialCardView cardCompanyInfo, cardCreateTour, cardCreateService;
    private MaterialCardView cardGuideManagement, cardGuideTracking;

    // Storage
    private PrefsManager prefsManager;
    private FileManager fileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_admin_main);

        // Inicializar storage
        prefsManager = new PrefsManager(this);
        fileManager = new FileManager(this);

        initViews();
        setupToolbar();
        setupDrawer();
        setupCardClickListeners();
        loadUserData();
        loadNavHeaderData();
        initializeNotificationCounters();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        tvWelcomeAdmin = findViewById(R.id.tv_welcome_admin);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvPendingAlertsCount = findViewById(R.id.tv_pending_alerts_count);

        // Primera fila - Quick Actions
        cardAlerts = findViewById(R.id.card_alerts);
        cardCustomerChat = findViewById(R.id.card_customer_chat);
        cardReports = findViewById(R.id.card_reports);

        // Gestión de Empresa
        cardCompanyInfo = findViewById(R.id.card_company_info);

        // Gestión de Tours
        cardCreateTour = findViewById(R.id.card_create_tour);
        cardCreateService = findViewById(R.id.card_create_service);

        // Gestión de Guías
        cardGuideManagement = findViewById(R.id.card_guide_management);
        cardGuideTracking = findViewById(R.id.card_guide_tracking);
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
        // Primera fila - Quick Actions
        cardAlerts.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, CheckoutAlertsActivity.class);
            startActivity(intent);
        });

        cardCustomerChat.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, AdminChatListActivity.class);
            startActivity(intent);
        });

        cardReports.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, SalesReportsActivity.class);
            startActivity(intent);
        });

        // Gestión de Empresa
        cardCompanyInfo.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, CompanyInfoActivity.class);
            startActivity(intent);
        });

        // Gestión de Tours
        cardCreateTour.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, CreateTourActivity.class);
            startActivity(intent);
        });

        cardCreateService.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, CreateServiceActivity.class);
            startActivity(intent);
        });

        // Gestión de Guías
        cardGuideManagement.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, GuideManagementActivity.class);
            startActivity(intent);
        });

        cardGuideTracking.setOnClickListener(v -> {
            Intent intent = new Intent(TourAdminMainActivity.this, GuideTrackingActivity.class);
            startActivity(intent);
        });
    }



    private void loadUserData() {
        // Obtener datos del usuario desde PrefsManager
        String userName = prefsManager.obtenerUsuario();
        String userType = prefsManager.obtenerTipoUsuario();

        if (userName != null && !userName.isEmpty()) {
            // Extraer solo el primer nombre si hay varios nombres
            String firstName = userName.split(" ")[0];
            tvWelcomeAdmin.setText("¡Hola, " + firstName + "!");
        } else {
            tvWelcomeAdmin.setText("¡Hola, Admin!");
        }

        // Mostrar nombre de empresa (por ahora genérico)
        tvCompanyName.setText("Empresa de Tours");
    }

    private void loadNavHeaderData() {
        // Obtener el header del NavigationView
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        if (headerView != null) {
            tvAdminNameHeader = headerView.findViewById(R.id.tv_admin_name_header);
            tvCompanyNameHeader = headerView.findViewById(R.id.tv_company_name_header);

            // Cargar datos del usuario
            String userName = prefsManager.obtenerUsuario();
            if (userName != null && !userName.isEmpty()) {
                tvAdminNameHeader.setText(userName);
            } else {
                tvAdminNameHeader.setText("Admin Usuario");
            }

            // Mostrar nombre de empresa
            tvCompanyNameHeader.setText("Empresa de Tours");
        }
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
            //Se limpian los datos de seión
            prefsManager.cerrarSesion();

            //Limpiar el stack de activities de Login
            Intent intent= new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            finish();

            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
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
        prefsManager.cerrarSesion();

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
}
