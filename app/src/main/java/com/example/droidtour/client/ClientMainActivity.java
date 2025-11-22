package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.droidtour.CompaniesListActivity;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.MainActivity;
import com.example.droidtour.MyReservationsActivity;
import com.example.droidtour.R;
import com.example.droidtour.TourDetailActivity;
import com.example.droidtour.ToursCatalogActivity;
import com.example.droidtour.database.DatabaseHelper;
import com.example.droidtour.utils.NotificationHelper;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import java.util.List;

public class ClientMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private RecyclerView rvFeaturedTours, rvPopularCompanies;
    private MaterialCardView cardExploreTours, cardMyReservations, cardChats;
    private TextView tvWelcomeMessage, tvActiveReservations;
    
    // Firebase
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private String currentUserId;
    
    // Storage Local (deprecated - migrar a Firebase)
    private DatabaseHelper dbHelper;
    private PreferencesManager prefsManager;
    private NotificationHelper notificationHelper;
    
    // Datos Firebase
    private List<com.example.droidtour.models.Tour> featuredTours = new java.util.ArrayList<>();
    private List<com.example.droidtour.models.Company> popularCompanies = new java.util.ArrayList<>();
    
    // Toolbar menu elements
    private FrameLayout notificationActionLayout, avatarActionLayout;
    private TextView tvNotificationBadge;
    private ImageView ivAvatarAction;
    private int notificationCount = 0;

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
        
        // Validar que el usuario sea CLIENT
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_client_main);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar Firebase
        authManager = com.example.droidtour.firebase.FirebaseAuthManager.getInstance(this);
        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        currentUserId = authManager.getCurrentUserId();
        
        // 🔥 TEMPORAL: Para testing sin login
        if (currentUserId == null) {
            // Usar UID real de Firebase Authentication: prueba@droidtour.com
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            android.widget.Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", android.widget.Toast.LENGTH_LONG).show();
        }
        
        // Inicializar helpers locales (deprecated)
        dbHelper = new DatabaseHelper(this);
        notificationHelper = new NotificationHelper(this);

        initializeViews();
        // Corregir datos del usuario PRIMERO (sin actualizar vistas aún)
        correctUserData();
        setupToolbarAndDrawer();
        setupDashboardData();
        setupClickListeners();
        setupRecyclerViews();
        
        // Cargar datos del usuario y actualizar vistas
        loadUserData();
        
        // Cargar reservas de ejemplo
        loadSampleReservations();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        // Dashboard elements
        tvWelcomeMessage = findViewById(R.id.tv_welcome_message);
        tvActiveReservations = findViewById(R.id.tv_active_reservations);
        cardExploreTours = findViewById(R.id.card_explore_tours);
        cardMyReservations = findViewById(R.id.card_my_reservations);
        cardChats = findViewById(R.id.card_chats);
        
        // RecyclerViews
        rvFeaturedTours = findViewById(R.id.rv_featured_tours);
        rvPopularCompanies = findViewById(R.id.rv_popular_companies);
    }

    private void setupDashboardData() {
        // Set welcome message (dynamic based on user)
        if (prefsManager.sesionActiva()) {
            String userName = prefsManager.obtenerUsuario();
            if (userName != null && !userName.isEmpty()) {
                // Extraer solo el primer nombre para el saludo
                String firstName = userName.split(" ")[0];
                tvWelcomeMessage.setText("¡Hola, " + firstName + "!");
            } else {
                tvWelcomeMessage.setText("¡Hola, Gabrielle!");
            }
        } else {
            tvWelcomeMessage.setText("¡Hola, Gabrielle!");
        }
        
        // Set active reservations count
        tvActiveReservations.setText("2");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_general, menu);
        setupVisualMenuElements(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_notifications) {
            // Abrir la pantalla de notificaciones desde el toolbar
            Intent intent = new Intent(this, ClientNotificationsActivity.class);
            startActivityForResult(intent, 100);
            return true;
        } else if (id == R.id.action_profile) {
            // Abrir pantalla de "Mi cuenta" al seleccionar la opción de perfil
            Intent intentProfileMenu = new Intent(this, ClientMyAccount.class);
            startActivity(intentProfileMenu);
            return true;
        }
        // Handle drawer toggle
        if (drawerToggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    private void setupVisualMenuElements(Menu menu) {
        MenuItem notificationItem = menu.findItem(R.id.action_notifications);
        if (notificationItem != null) {
            notificationActionLayout = (FrameLayout) notificationItem.getActionView();
            if (notificationActionLayout != null) {
                tvNotificationBadge = notificationActionLayout.findViewById(R.id.tv_notification_badge);
                updateNotificationBadge();
                notificationActionLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(ClientMainActivity.this, ClientNotificationsActivity.class);
                    startActivityForResult(intent, 100);
                });
            }
        }

        MenuItem avatarItem = menu.findItem(R.id.action_profile);
        if (avatarItem != null) {
            avatarActionLayout = (FrameLayout) avatarItem.getActionView();
            if (avatarActionLayout != null) {
                ivAvatarAction = avatarActionLayout.findViewById(R.id.iv_avatar_action);
                // Al hacer click en el avatar del toolbar, abrir ClientMyAccount
                avatarActionLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(ClientMainActivity.this, ClientMyAccount.class);
                    startActivity(intent);
                });
            }
        }
    }

    private void updateNotificationBadge() {
        if (tvNotificationBadge != null && dbHelper != null) {
            int unreadCount = dbHelper.getUnreadNotificationsCount();
            if (unreadCount > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(String.valueOf(Math.min(unreadCount, 99)));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            // Actualizar badge cuando se regresa de notificaciones
            updateNotificationBadge();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Asegurar que "Inicio" esté seleccionado cuando la activity sea visible
        try {
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.nav_dashboard);
                MenuItem dashboardItem = navigationView.getMenu().findItem(R.id.nav_dashboard);
                if (dashboardItem != null) dashboardItem.setChecked(true);
            }
        } catch (Exception ignored) {
        }
    }

    private void setupToolbarAndDrawer() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        
        // Marcar "Inicio" (dashboard) como seleccionado para esta activity
        try {
            navigationView.setCheckedItem(R.id.nav_dashboard);
            MenuItem dashboardItem = navigationView.getMenu().findItem(R.id.nav_dashboard);
            if (dashboardItem != null) dashboardItem.setChecked(true);
        } catch (Exception ignored) {
            // Si por alguna razón el menú aún no está inflado o el id no existe, ignorar sin romper la app
        }

        // Actualizar nombre de usuario en el header del drawer
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
            if (tvUserNameHeader != null) {
                String userName = prefsManager.obtenerUsuario();
                if (userName != null && !userName.isEmpty()) {
                    // Mostrar nombre + apellido (dos primeros tokens) si están disponibles
                    String displayName = userName.trim();
                    String[] parts = displayName.split("\\s+");
                    if (parts.length >= 2) {
                        displayName = parts[0] + " " + parts[1];
                    } else if (parts.length == 1) {
                        displayName = parts[0];
                    }
                    tvUserNameHeader.setText(displayName);
                } else {
                    tvUserNameHeader.setText("Usuario");
                }
            }
        }

        // Asegurar que el icono de hamburguesa sea blanco (tint)
        int whiteColor = ContextCompat.getColor(this, R.color.white);
        try {
            // Tint al navigation icon del toolbar si existe
            if (toolbar.getNavigationIcon() != null) {
                android.graphics.drawable.Drawable nav = toolbar.getNavigationIcon();
                nav = DrawableCompat.wrap(nav);
                DrawableCompat.setTint(nav, whiteColor);
                toolbar.setNavigationIcon(nav);
            }
        } catch (Exception ignored) { }

        try {
            // Si el ActionBarDrawerToggle creó un DrawerArrowDrawable, forzar su color
            if (drawerToggle != null && drawerToggle.getDrawerArrowDrawable() != null) {
                drawerToggle.getDrawerArrowDrawable().setColor(whiteColor);
                // sincronizar estado por si acaso
                drawerToggle.syncState();
            }
        } catch (Exception ignored) { }
    }

    private void setupClickListeners() {
        // Quick action cards
        cardExploreTours.setOnClickListener(v -> {
            Intent intent = new Intent(ClientMainActivity.this, CompaniesListActivity.class);
            startActivity(intent);
        });

        cardMyReservations.setOnClickListener(v -> {
            Intent intent = new Intent(ClientMainActivity.this, MyReservationsActivity.class);
            startActivity(intent);
        });

        cardChats.setOnClickListener(v -> {
            Intent intent = new Intent(ClientMainActivity.this, ClientChatActivity.class);
            startActivity(intent);
        });
    }
    
    /**
     * 🔥 Abrir activity para inicializar datos de prueba
     */
    private void openInitializeDataActivity() {
        Intent intent = new Intent(this, com.example.droidtour.firebase.InitializeTestDataActivity.class);
        startActivity(intent);
    }

    private void setupRecyclerViews() {
        // Featured Tours (horizontal)
        rvFeaturedTours.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedTours.setAdapter(new FeaturedToursAdapter(featuredTours, this::onFeaturedTourClick));

        // Popular Companies (vertical)
        rvPopularCompanies.setLayoutManager(new LinearLayoutManager(this));
        rvPopularCompanies.setAdapter(new PopularCompaniesAdapter(popularCompanies, this::onCompanyClick));
        
        // Cargar datos desde Firebase
        loadFeaturedToursFromFirebase();
        loadPopularCompaniesFromFirebase();
    }
    
    private void loadFeaturedToursFromFirebase() {
        firestoreManager.getTours(new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                featuredTours.clear();
                featuredTours.addAll((List<com.example.droidtour.models.Tour>) result);
                rvFeaturedTours.getAdapter().notifyDataSetChanged();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClientMainActivity.this, "Error cargando tours", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadPopularCompaniesFromFirebase() {
        firestoreManager.getCompanies(new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                popularCompanies.clear();
                popularCompanies.addAll((List<com.example.droidtour.models.Company>) result);
                rvPopularCompanies.getAdapter().notifyDataSetChanged();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClientMainActivity.this, "Error cargando empresas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onFeaturedTourClick(com.example.droidtour.models.Tour tour) {
        Intent intent = new Intent(this, TourDetailActivity.class);
        intent.putExtra("tour_id", tour.getTourId());
        intent.putExtra("tour_name", tour.getName());
        intent.putExtra("company_name", tour.getCompanyName());
        intent.putExtra("company_id", tour.getCompanyId());
        intent.putExtra("price", tour.getPricePerPerson());
        startActivity(intent);
    }

    private void onCompanyClick(com.example.droidtour.models.Company company) {
        Intent intent = new Intent(this, ToursCatalogActivity.class);
        intent.putExtra("company_id", company.getCompanyId());
        intent.putExtra("company_name", company.getName());
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_dashboard) {
            // Already on dashboard
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_explore_tours) {
            startActivity(new Intent(this, CompaniesListActivity.class));
        } else if (id == R.id.nav_companies) {
            startActivity(new Intent(this, CompaniesListActivity.class));
        } else if (id == R.id.nav_my_reservations) {
            startActivity(new Intent(this, MyReservationsActivity.class));
        } else if (id == R.id.nav_qr_codes) {
            startActivity(new Intent(this, ClientQRCodesActivity.class));
        } else if (id == R.id.nav_payment_methods) {
            startActivity(new Intent(this, PaymentMethodsActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ClientProfileActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, ClientSettingsActivity.class));
        } else if (id == R.id.nav_init_test_data) {
            // 🔥 Inicializar datos de prueba
            openInitializeDataActivity();
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
    
    // ==================== STORAGE LOCAL ====================
    
    private void correctUserData() {
        // Si no hay sesión activa, no corregir nada
        if (!prefsManager.sesionActiva()) return;

        // Verificar y corregir datos del cliente (sin actualizar vistas)
        String userType = prefsManager.obtenerTipoUsuario();
        String userName = prefsManager.obtenerUsuario();
        // Ya no necesitamos corregir datos - LoginActivity ya guarda todo correctamente
    }
    
    private void loadUserData() {
        // Cargar datos del usuario y actualizar vistas
        String userName = prefsManager.obtenerUsuario();
        
        // Actualizar mensaje de bienvenida
        if (userName != null && !userName.isEmpty()) {
            // Extraer solo el primer nombre para el saludo
            String firstName = userName.split(" ")[0];
            tvWelcomeMessage.setText("¡Hola, " + firstName + "!");
        } else {
            tvWelcomeMessage.setText("¡Hola, Gabrielle!");
        }
        
        // Actualizar nombre en el header del drawer
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
                if (tvUserNameHeader != null && userName != null && !userName.isEmpty()) {
                    // Mostrar nombre + apellido (dos primeros tokens) en el header
                    String displayName = userName.trim();
                    String[] parts = displayName.split("\\s+");
                    if (parts.length >= 2) {
                        displayName = parts[0] + " " + parts[1];
                    } else if (parts.length == 1) {
                        displayName = parts[0];
                    }
                    tvUserNameHeader.setText(displayName);
                }
            }
        }
    }
    
    private void loadSampleReservations() {
        // Cargar reservas de ejemplo solo si la BD está vacía
        List<DatabaseHelper.Reservation> existingReservations = dbHelper.getAllReservations();
        
        if (existingReservations.isEmpty()) {
            // Agregar reservas de ejemplo
            dbHelper.addReservation(
                "City Tour Lima Centro", 
                "Lima Adventure Tours", 
                "28 Oct", 
                "09:00 AM",
                "CONFIRMADA", 
                150.0, 
                2, 
                "QR-2024-001"
            );
            
            dbHelper.addReservation(
                "Tour Machu Picchu", 
                "Cusco Explorer", 
                "02 Nov", 
                "06:00 AM",
                "CONFIRMADA", 
                450.0, 
                2, 
                "QR-2024-002"
            );
            
            Toast.makeText(this, "Reservas cargadas", Toast.LENGTH_SHORT).show();
            
            // Enviar notificación de confirmación
            notificationHelper.sendReservationConfirmedNotification(
                "City Tour Lima Centro", 
                "28 Oct", 
                "QR-2024-001"
            );
            
            // Actualizar contador de reservas activas
            updateActiveReservationsCount();
        }
    }
    
    private void updateActiveReservationsCount() {
        List<DatabaseHelper.Reservation> allReservations = dbHelper.getAllReservations();
        int activeCount = 0;
        
        for (DatabaseHelper.Reservation res : allReservations) {
            if (res.getStatus().equals("CONFIRMADA")) {
                activeCount++;
            }
        }
        
        tvActiveReservations.setText(activeCount + " reservas activas");
    }
    
    // Método para crear una nueva reserva
    public void createReservation(String tourName, String company, String date, String time,
                                  double price, int people) {
        // Generar código QR único
        String qrCode = "QR-" + System.currentTimeMillis();
        
        // Guardar en BD
        dbHelper.addReservation(tourName, company, date, time, "CONFIRMADA", price, people, qrCode);
        
        // Enviar notificación
        notificationHelper.sendReservationConfirmedNotification(tourName, date, qrCode);
        
        // Enviar notificación de pago
        notificationHelper.sendPaymentConfirmedNotification(tourName, price);
        
        Toast.makeText(this, "¡Reserva confirmada! Código: " + qrCode, Toast.LENGTH_LONG).show();
        
        // Actualizar contador
        updateActiveReservationsCount();
    }
    
    // ==================== NOTIFICACIONES ====================
    
    private void testClientNotifications() {
        // Método de prueba para enviar notificaciones de ejemplo
        notificationHelper.sendTourReminderForClient(
            "City Tour Lima Centro", 
            "28 Oct", 
            "09:00 AM"
        );
    }
    
    // ==================== VALIDACIÓN DE SESIÓN ====================
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

// Adaptador para tours destacados (horizontal)
class FeaturedToursAdapter extends RecyclerView.Adapter<FeaturedToursAdapter.ViewHolder> {
    interface OnTourClick { void onClick(com.example.droidtour.models.Tour tour); }
    private final List<com.example.droidtour.models.Tour> tours;
    private final OnTourClick onTourClick;
    
    FeaturedToursAdapter(List<com.example.droidtour.models.Tour> tours, OnTourClick listener) { 
        this.tours = tours;
        this.onTourClick = listener; 
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_featured_tour, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        com.example.droidtour.models.Tour tour = tours.get(position);
        
        android.widget.ImageView tourImage = holder.itemView.findViewById(R.id.iv_featured_image);
        TextView tourName = holder.itemView.findViewById(R.id.tv_tour_name);
        TextView companyName = holder.itemView.findViewById(R.id.tv_company_name);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView price = holder.itemView.findViewById(R.id.tv_price);

        tourName.setText(tour.getName());
        companyName.setText(tour.getCompanyName());
        rating.setText("⭐ " + tour.getAverageRating());
        price.setText("S/. " + String.format("%.0f", tour.getPricePerPerson()));

        if (tourImage != null) {
            String imageUrl = tour.getImageUrl() != null ? tour.getImageUrl() : "https://www.dicasdeviagem.com/wp-content/uploads/2020/03/lima-costa-mar-2048x1364.jpg";
            Glide.with(tourImage.getContext())
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(tourImage);
        }

        holder.itemView.setOnClickListener(v -> onTourClick.onClick(tour));
    }

    @Override
    public int getItemCount() { return tours.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

// Adaptador para empresas populares (vertical)
class PopularCompaniesAdapter extends RecyclerView.Adapter<PopularCompaniesAdapter.ViewHolder> {
    interface OnCompanyClick { void onClick(com.example.droidtour.models.Company company); }
    private final List<com.example.droidtour.models.Company> companies;
    private final OnCompanyClick onCompanyClick;
    
    PopularCompaniesAdapter(List<com.example.droidtour.models.Company> companies, OnCompanyClick listener) { 
        this.companies = companies;
        this.onCompanyClick = listener; 
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_popular_company, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        com.example.droidtour.models.Company company = companies.get(position);
        
        TextView companyName = holder.itemView.findViewById(R.id.tv_company_name);
        TextView location = holder.itemView.findViewById(R.id.tv_company_location);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView toursCount = holder.itemView.findViewById(R.id.tv_tours_count);
        TextView reviewsCount = holder.itemView.findViewById(R.id.tv_reviews_count);
        android.view.View btnViewTours = holder.itemView.findViewById(R.id.btn_view_tours);

        companyName.setText(company.getName());
        location.setText("📍 " + company.getCity() + ", " + company.getCountry());
        rating.setText("⭐ " + company.getAverageRating());
        toursCount.setText("• " + company.getTotalTours() + " tours");
        reviewsCount.setText("• " + company.getTotalReviews() + " reseñas");

        holder.itemView.setOnClickListener(v -> onCompanyClick.onClick(company));
        btnViewTours.setOnClickListener(v -> onCompanyClick.onClick(company));
    }

    @Override
    public int getItemCount() { return companies.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}
