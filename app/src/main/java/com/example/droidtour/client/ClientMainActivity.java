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
import com.example.droidtour.MyReservationsActivity;
import com.example.droidtour.R;
import com.example.droidtour.TourDetailActivity;
import com.example.droidtour.ToursCatalogActivity;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.Company;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.utils.NotificationHelper;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import java.util.List;
import java.util.ArrayList;

public class ClientMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "ClientMainActivity";

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

    // Helpers
    private PreferencesManager prefsManager;
    private NotificationHelper notificationHelper;

    // Datos Firebase
    private final List<Tour> featuredTours = new ArrayList<>();
    private final List<Company> popularCompanies = new ArrayList<>();

    // Toolbar menu elements
    private TextView tvNotificationBadge;
    private ImageView ivAvatarAction;

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
        currentUserId = prefsManager.getUserId(); // Usar el ID de PreferencesManager

        // Inicializar helpers
        notificationHelper = new NotificationHelper(this);

        initializeViews();
        correctUserData();
        setupToolbarAndDrawer();
        setupDashboardData();
        setupClickListeners();
        setupRecyclerViews();

        // Cargar datos del usuario y actualizar vistas
        loadUserData();

        // Cargar reservas activas desde Firestore
        updateActiveReservationsCount();

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
        if (prefsManager.isLoggedIn()) {
            String userName = prefsManager.getUserName();
            if (userName != null && !userName.isEmpty()) {
                // Extraer solo el primer nombre para el saludo
                String firstName = userName.split(" ")[0];
                tvWelcomeMessage.setText("¡Hola, " + firstName + "!");
            } else {
                tvWelcomeMessage.setText("¡Hola!");
            }
        } else {
            tvWelcomeMessage.setText("¡Hola!");
        }

        // Set active reservations count (temporal)
        tvActiveReservations.setText("0 reservas activas");
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
            startActivity(intent);
            return true;
        } else if (id == R.id.action_profile) {
            // Abrir pantalla de "Mi cuenta" al seleccionar la opción de perfil
            Intent intentProfileMenu = new Intent(this, ClientMyAccount.class);
            startActivity(intentProfileMenu);
            return true;
        }
        // Handle drawer toggle
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    private void setupVisualMenuElements(Menu menu) {
        MenuItem notificationItem = menu.findItem(R.id.action_notifications);
        if (notificationItem != null && notificationItem.getActionView() != null) {
            FrameLayout notificationActionLayout = (FrameLayout) notificationItem.getActionView();
            tvNotificationBadge = notificationActionLayout.findViewById(R.id.tv_notification_badge);
            updateNotificationBadge();
            notificationActionLayout.setOnClickListener(v -> {
                Intent intent = new Intent(ClientMainActivity.this, ClientNotificationsActivity.class);
                startActivity(intent);
            });
        }

        MenuItem avatarItem = menu.findItem(R.id.action_profile);
        if (avatarItem != null && avatarItem.getActionView() != null) {
            FrameLayout avatarActionLayout = (FrameLayout) avatarItem.getActionView();
            ivAvatarAction = avatarActionLayout.findViewById(R.id.iv_avatar_action);
            // Cargar imagen de perfil si existe
            loadUserProfileImage();
            // Al hacer click en el avatar del toolbar, abrir ClientMyAccount
            avatarActionLayout.setOnClickListener(v -> {
                Intent intent = new Intent(ClientMainActivity.this, ClientMyAccount.class);
                startActivity(intent);
            });
        }
    }

    private void loadUserProfileImage() {
        if (ivAvatarAction != null && currentUserId != null) {
            // Cargar datos del usuario para obtener la foto
            firestoreManager.getUserById(currentUserId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    if (result instanceof com.example.droidtour.models.User) {
                        com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                        String photoUrl = user.getPhotoUrl();
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(ClientMainActivity.this)
                                    .load(photoUrl)
                                    .placeholder(android.R.drawable.sym_def_app_icon)
                                    .circleCrop()
                                    .into(ivAvatarAction);
                        } else {
                            // Mostrar icono por defecto si no hay foto
                            ivAvatarAction.setImageResource(android.R.drawable.sym_def_app_icon);
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e(TAG, "Error cargando foto de perfil", e);
                }
            });
        }
    }

    private void updateNotificationBadge() {
        if (tvNotificationBadge != null && currentUserId != null) {
            firestoreManager.getUnreadNotifications(currentUserId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    if (result instanceof List) {
                        List<?> notifications = (List<?>) result;
                        int unreadCount = notifications.size();
                        if (unreadCount > 0) {
                            tvNotificationBadge.setVisibility(View.VISIBLE);
                            tvNotificationBadge.setText(String.valueOf(Math.min(unreadCount, 99)));
                        } else {
                            tvNotificationBadge.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e(TAG, "Error cargando contador de notificaciones", e);
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            });
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
        // Actualizar datos al regresar a la actividad
        updateNotificationBadge();
        updateActiveReservationsCount();

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Marcar "Inicio" (dashboard) como seleccionado para esta activity
        try {
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.nav_dashboard);
                MenuItem dashboardItem = navigationView.getMenu().findItem(R.id.nav_dashboard);
                if (dashboardItem != null) dashboardItem.setChecked(true);
            }
        } catch (Exception ignored) {
            // Si por alguna razón el menú aún no está inflado o el id no existe, ignorar sin romper la app
        }

        // Actualizar nombre de usuario en el header del drawer
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
                if (tvUserNameHeader != null) {
                    String userName = prefsManager.getUserName();
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
            if (drawerToggle != null) {
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
        // TODO: Implementar la carga real desde FirestoreManager
        featuredTours.clear();
        // Dejar vacío para evitar datos de testing hardcodeados
        if (rvFeaturedTours.getAdapter() != null) rvFeaturedTours.getAdapter().notifyDataSetChanged();
    }

    private void loadPopularCompaniesFromFirebase() {
        // TODO: Implementar la carga real desde FirestoreManager
        popularCompanies.clear();
        // Dejar vacío para evitar datos de testing hardcodeados
        if (rvPopularCompanies.getAdapter() != null) rvPopularCompanies.getAdapter().notifyDataSetChanged();
    }

    private void onFeaturedTourClick(Tour tour) {
        Intent intent = new Intent(this, TourDetailActivity.class);
        intent.putExtra("tour_id", tour.getTourId());
        intent.putExtra("tour_name", tour.getTourName());
        intent.putExtra("company_name", tour.getCompanyName());
        intent.putExtra("company_id", tour.getCompanyId());
        Double price = tour.getPricePerPerson();
        if (price != null) intent.putExtra("price", price);
        startActivity(intent);
    }

    private void onCompanyClick(Company company) {
        Intent intent = new Intent(this, ToursCatalogActivity.class);
        intent.putExtra("company_id", company.getCompanyId());
        intent.putExtra("company_name", company.getCommercialName());
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already on dashboard
            if (drawerLayout != null) drawerLayout.closeDrawers();
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
        } else if (id == R.id.nav_logout) {
            // Se limpian los datos de sesión
            prefsManager.logout();

            // Limpiar el stack de activities de Login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            if (drawerLayout != null) drawerLayout.closeDrawers();
            finish();

            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
        }

        if (drawerLayout != null) drawerLayout.closeDrawers();
        return true;
    }

    // ==================== STORAGE LOCAL ====================

    private void correctUserData() {
        // Si no hay sesión activa, no corregir nada
        if (!prefsManager.isLoggedIn()) return;

        // Verificar y corregir datos del cliente (sin actualizar vistas)
        // No-op por ahora
    }

    private void loadUserData() {
        // Cargar datos del usuario y actualizar vistas
        String userName = prefsManager.getUserName();

        // Actualizar mensaje de bienvenida
        if (userName != null && !userName.isEmpty()) {
            // Extraer solo el primer nombre para el saludo
            String firstName = userName.split(" ")[0];
            tvWelcomeMessage.setText("¡Hola, " + firstName + "!");
        } else {
            tvWelcomeMessage.setText("¡Hola!");
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

    private void updateActiveReservationsCount() {
        if (currentUserId == null) {
            tvActiveReservations.setText("0 reservas activas");
            return;
        }

        firestoreManager.getReservationsByUser(currentUserId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<Reservation> reservations = (List<Reservation>) result;
                    int activeCount = 0;

                    for (Reservation res : reservations) {
                        if (res.getStatus() != null && res.getStatus().equals("CONFIRMADA")) {
                            activeCount++;
                        }
                    }

                    tvActiveReservations.setText(activeCount + " reservas activas");
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e(TAG, "Error cargando contador de reservas activas", e);
                tvActiveReservations.setText("0 reservas activas");
            }
        });
    }

    // ==================== NOTIFICACIONES ====================

    private void testClientNotifications() {
        // Método de prueba para enviar notificaciones de ejemplo
        if (currentUserId != null) {
            notificationHelper.sendTourReminderForClient(
                    currentUserId,
                    "City Tour Lima Centro",
                    "28 Oct",
                    "09:00 AM"
            );
        }
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
    interface OnTourClick { void onClick(Tour tour); }
    private final List<Tour> tours;
    private final OnTourClick onTourClick;

    FeaturedToursAdapter(List<Tour> tours, OnTourClick listener) {
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
        Tour tour = tours.get(position);

        android.widget.ImageView tourImage = holder.itemView.findViewById(R.id.iv_featured_image);
        TextView tourName = holder.itemView.findViewById(R.id.tv_tour_name);
        TextView companyName = holder.itemView.findViewById(R.id.tv_company_name);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView price = holder.itemView.findViewById(R.id.tv_price);

        tourName.setText(tour.getTourName() != null ? tour.getTourName() : "Tour sin nombre");
        companyName.setText(tour.getCompanyName() != null ? tour.getCompanyName() : "Compañía");
        // Mostrar rating si existe
        Double avg = tour.getAverageRating();
        rating.setText(avg != null ? String.format("%.1f", avg) : "4.5");
        Double priceVal = tour.getPricePerPerson();
        if (priceVal != null && priceVal > 0) {
            price.setText("S/. " + String.format("%.0f", priceVal));
        } else {
            price.setText("Consultar precio");
        }

        if (tourImage != null) {
            String imageUrl = tour.getMainImageUrl() != null ? tour.getMainImageUrl() :
                    "https://www.dicasdeviagem.com/wp-content/uploads/2020/03/lima-costa-mar-2048x1364.jpg";
            Glide.with(tourImage.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(tourImage);
        }

        holder.itemView.setOnClickListener(v -> onTourClick.onClick(tour));
    }

    @Override
    public int getItemCount() {
        return tours != null ? tours.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View v) { super(v); }
    }
}

// Adaptador para empresas populares (vertical)
class PopularCompaniesAdapter extends RecyclerView.Adapter<PopularCompaniesAdapter.ViewHolder> {
    interface OnCompanyClick { void onClick(Company company); }
    private final List<Company> companies;
    private final OnCompanyClick onCompanyClick;

    PopularCompaniesAdapter(List<Company> companies, OnCompanyClick listener) {
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
        Company company = companies.get(position);

        TextView companyName = holder.itemView.findViewById(R.id.tv_company_name);
        TextView location = holder.itemView.findViewById(R.id.tv_company_location);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView toursCount = holder.itemView.findViewById(R.id.tv_tours_count);
        TextView reviewsCount = holder.itemView.findViewById(R.id.tv_reviews_count);
        android.view.View btnViewTours = holder.itemView.findViewById(R.id.btn_view_tours);

        // Usar nombre comercial o razón social
        String displayName = company.getCommercialName() != null ?
                company.getCommercialName() : company.getBusinessName();
        companyName.setText(displayName != null ? displayName : "Empresa");

        location.setText("📍 " + (company.getAddress() != null ? company.getAddress() : "Ubicación no disponible"));

        // Datos temporales - mostrar campos si existen
        rating.setText("⭐ " + (company.getStatus() != null ? company.getStatus() : "4.5"));
        toursCount.setText("• -- tours");
        reviewsCount.setText("• -- reseñas");

        holder.itemView.setOnClickListener(v -> onCompanyClick.onClick(company));
        if (btnViewTours != null) {
            btnViewTours.setOnClickListener(v -> onCompanyClick.onClick(company));
        }
    }

    @Override
    public int getItemCount() {
        return companies != null ? companies.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View v) { super(v); }
    }
}