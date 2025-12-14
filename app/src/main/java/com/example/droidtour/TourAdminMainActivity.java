package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
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
import com.example.droidtour.admin.TourManagementActivity;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import com.example.droidtour.models.Message;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.droidtour.managers.PrefsManager;
import com.example.droidtour.managers.FileManager;

import java.util.List;

public class TourAdminMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "TourAdminMainActivity";
    
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
    
    // Firebase
    private FirestoreManager firestoreManager;
    private PreferencesManager preferencesManager;
    private String currentCompanyId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_admin_main);

        // Inicializar storage
        prefsManager = new PrefsManager(this);
        fileManager = new FileManager(this);
        
        // Inicializar Firebase
        firestoreManager = FirestoreManager.getInstance();
        preferencesManager = new PreferencesManager(this);

        initViews();
        setupToolbar();
        setupDrawer();
        setupCardClickListeners();
        initializeNotificationCounters();
        setupFab();
        
        // Cargar datos desde Firebase
        loadUserDataFromFirebase();

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



    private void loadUserDataFromFirebase() {
        String userId = preferencesManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            // Fallback a datos locales
            loadUserDataFromLocal();
            return;
        }
        
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null) {
                    currentUserName = user.getFullName();
                    currentCompanyId = user.getCompanyId();
                    
                    // Actualizar UI con nombre del usuario
                    if (currentUserName != null && !currentUserName.isEmpty()) {
                        String firstName = currentUserName.split(" ")[0];
                        tvWelcomeAdmin.setText("¡Hola, " + firstName + "!");
                    } else {
                        tvWelcomeAdmin.setText("¡Hola, Admin!");
                    }
                    
                    // Cargar nombre de la empresa
                    if (currentCompanyId != null && !currentCompanyId.isEmpty()) {
                        loadCompanyName();
                        loadDashboardDataFromFirebase();
                    } else {
                        tvCompanyName.setText("Sin empresa asignada");
                    }
                    
                    // Actualizar header del drawer
                    loadNavHeaderData();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario desde Firebase", e);
                loadUserDataFromLocal();
            }
        });
    }
    
    private void loadCompanyName() {
        firestoreManager.getCompanyById(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Company company = (Company) result;
                if (company != null) {
                    String companyName = company.getCommercialName();
                    if (companyName == null || companyName.isEmpty()) {
                        companyName = company.getBusinessName();
                    }
                    tvCompanyName.setText(companyName != null ? companyName : "Empresa de Tours");
                    
                    // Actualizar también en el header
                    if (tvCompanyNameHeader != null) {
                        tvCompanyNameHeader.setText(companyName != null ? companyName : "Empresa de Tours");
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando empresa", e);
                tvCompanyName.setText("Empresa de Tours");
            }
        });
    }
    
    private void loadUserDataFromLocal() {
        // Fallback a PrefsManager
        String userName = prefsManager.obtenerUsuario();

        if (userName != null && !userName.isEmpty()) {
            String firstName = userName.split(" ")[0];
            tvWelcomeAdmin.setText("¡Hola, " + firstName + "!");
            currentUserName = userName;
        } else {
            tvWelcomeAdmin.setText("¡Hola, Admin!");
        }

        tvCompanyName.setText("Empresa de Tours");
        loadNavHeaderData();
    }

    private void loadNavHeaderData() {
        // Obtener el header del NavigationView
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        if (headerView != null) {
            tvAdminNameHeader = headerView.findViewById(R.id.tv_admin_name_header);
            tvCompanyNameHeader = headerView.findViewById(R.id.tv_company_name_header);

            // Cargar datos del usuario
            if (currentUserName != null && !currentUserName.isEmpty()) {
                if (tvAdminNameHeader != null) tvAdminNameHeader.setText(currentUserName);
            } else {
                String userName = prefsManager.obtenerUsuario();
                if (tvAdminNameHeader != null) {
                    tvAdminNameHeader.setText(userName != null && !userName.isEmpty() ? userName : "Admin Usuario");
                }
            }

            // El nombre de empresa se actualiza en loadCompanyName
            if (tvCompanyNameHeader != null && tvCompanyNameHeader.getText().toString().isEmpty()) {
                tvCompanyNameHeader.setText("Empresa de Tours");
            }
        }
    }
    
    private void loadDashboardDataFromFirebase() {
        if (currentCompanyId == null) return;
        
        // Cargar conteo de alertas pendientes (reservaciones confirmadas)
        firestoreManager.getReservationsByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Reservation> reservations = (List<Reservation>) result;
                int pendingCount = 0;
                if (reservations != null) {
                    for (Reservation r : reservations) {
                        if ("CONFIRMADA".equals(r.getStatus())) {
                            pendingCount++;
                        }
                    }
                }
                updatePendingAlertsCount(pendingCount);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando alertas", e);
                updatePendingAlertsCount(0);
            }
        });
        
        // Cargar conteo de chats activos
        firestoreManager.getMessagesByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Message> messages = (List<Message>) result;
                // Contar mensajes no leídos
                int unreadCount = 0;
                if (messages != null) {
                    for (Message msg : messages) {
                        if (!msg.getIsRead()) {
                            unreadCount++;
                        }
                    }
                }
                updateActiveChatCount(unreadCount);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando chats", e);
                updateActiveChatCount(0);
            }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_general, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            // Abrir pantalla de Mi Cuenta del Admin
            Intent intent = new Intent(this, AdminMyAccountActivity.class);
            startActivity(intent);
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
        } else if (id == R.id.nav_tour_management) {
            // Nuevo: abrir actividad de gestión de tours
            startActivity(new Intent(this, TourManagementActivity.class));
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
        // Cargar datos desde Firebase
        if (currentCompanyId != null) {
            loadDashboardDataFromFirebase();
        } else {
            // Si no hay companyId, mostrar 0
            updatePendingAlertsCount(0);
            updateActiveChatCount(0);
        }
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
