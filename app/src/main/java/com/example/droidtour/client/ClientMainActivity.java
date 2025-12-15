package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.User;
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
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
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

    // Header (navigation) elements
    private ImageView ivProfileHeader;
    private TextView tvUserNameHeader;


    private LinearLayout emptyFeaturedTours;
    private LinearLayout emptyPopularCompanies;

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
        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = prefsManager.getUserId();

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

        // Cargar imagen de perfil (toolbar + header)
        refreshUserImagesFromFirestore();

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


        emptyFeaturedTours = findViewById(R.id.empty_featured_tours);
        emptyPopularCompanies = findViewById(R.id.empty_popular_companies);
    }

    private void setupDashboardData() {
        if (prefsManager.isLoggedIn()) {
            String userName = prefsManager.getUserName();
            if (userName != null && !userName.isEmpty()) {
                String firstName = userName.split(" ")[0];
                tvWelcomeMessage.setText("¡Hola, " + firstName + "!");
            } else {
                tvWelcomeMessage.setText("¡Hola!");
            }
        } else {
            tvWelcomeMessage.setText("¡Hola!");
        }

        tvActiveReservations.setText("0 reservas activas");
    }


    /**
     * Muestra u oculta el estado vacío para tours destacados
     */
    private void showEmptyFeaturedTours(boolean showEmpty) {
        if (emptyFeaturedTours != null && rvFeaturedTours != null) {
            if (showEmpty) {
                emptyFeaturedTours.setVisibility(View.VISIBLE);
                rvFeaturedTours.setVisibility(View.GONE);
            } else {
                emptyFeaturedTours.setVisibility(View.GONE);
                rvFeaturedTours.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Muestra u oculta el estado vacío para empresas populares
     */
    private void showEmptyPopularCompanies(boolean showEmpty) {
        if (emptyPopularCompanies != null && rvPopularCompanies != null) {
            if (showEmpty) {
                emptyPopularCompanies.setVisibility(View.VISIBLE);
                rvPopularCompanies.setVisibility(View.GONE);
            } else {
                emptyPopularCompanies.setVisibility(View.GONE);
                rvPopularCompanies.setVisibility(View.VISIBLE);
            }
        }
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
            Intent intent = new Intent(this, ClientNotificationsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_profile) {
            Intent intentProfileMenu = new Intent(this, ClientMyAccount.class);
            startActivity(intentProfileMenu);
            return true;
        }

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

            // Placeholder inicial
            if (ivAvatarAction != null) {
                ivAvatarAction.setImageResource(R.drawable.ic_avatar_24);
            }

            // Cargar imagen real (Firestore)
            refreshUserImagesFromFirestore();

            avatarActionLayout.setOnClickListener(v -> {
                Intent intent = new Intent(ClientMainActivity.this, ClientMyAccount.class);
                startActivity(intent);
            });
        }
    }

    private void updateNotificationBadge() {
        if (tvNotificationBadge != null && currentUserId != null) {
            firestoreManager.getUnreadNotifications(currentUserId, new FirestoreManager.FirestoreCallback() {
                @Override
                @SuppressWarnings("unchecked")
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
                    Log.e(TAG, "Error cargando contador de notificaciones", e);
                    if (tvNotificationBadge != null) tvNotificationBadge.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
        updateActiveReservationsCount();

        // refrescar foto por si la cambiaron en otra pantalla
        refreshUserImagesFromFirestore();

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

        // Marcar "Inicio"
        try {
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.nav_dashboard);
                MenuItem dashboardItem = navigationView.getMenu().findItem(R.id.nav_dashboard);
                if (dashboardItem != null) dashboardItem.setChecked(true);
            }
        } catch (Exception ignored) {}

        // Header refs (nombre + foto)
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                ivProfileHeader = headerView.findViewById(R.id.iv_profile_picture_header);
                tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);

                // placeholder inicial para la foto del header
                if (ivProfileHeader != null) {
                    ivProfileHeader.setImageResource(R.drawable.ic_avatar_24);
                }

                // nombre desde prefs (rápido)
                if (tvUserNameHeader != null) {
                    String userName = prefsManager.getUserName();
                    tvUserNameHeader.setText(buildShortDisplayName(userName));
                }
            }
        }

        // Tint hamburguesa
        int whiteColor = ContextCompat.getColor(this, R.color.white);
        try {
            if (toolbar.getNavigationIcon() != null) {
                android.graphics.drawable.Drawable nav = toolbar.getNavigationIcon();
                nav = DrawableCompat.wrap(nav);
                DrawableCompat.setTint(nav, whiteColor);
                toolbar.setNavigationIcon(nav);
            }
        } catch (Exception ignored) { }

        try {
            if (drawerToggle != null) {
                drawerToggle.getDrawerArrowDrawable().setColor(whiteColor);
                drawerToggle.syncState();
            }
        } catch (Exception ignored) { }

        // Finalmente: cargar foto real desde Firestore
        refreshUserImagesFromFirestore();
    }

    private String buildShortDisplayName(String userName) {
        if (userName == null) return "Usuario";
        String displayName = userName.trim();
        if (displayName.isEmpty()) return "Usuario";

        String[] parts = displayName.split("\\s+");
        if (parts.length >= 2) return parts[0] + " " + parts[1];
        return parts[0];
    }

    /**
     * Carga URL de foto desde Firestore y actualiza:
     * - ivAvatarAction (toolbar)
     * - ivProfileHeader (navigation header)
     */
    private void refreshUserImagesFromFirestore() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) return;

        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (!(result instanceof User)) return;

                User user = (User) result;

                // Nombre (si quieres que también se refresque desde Firestore)
                if (tvUserNameHeader != null) {
                    String fullName = (user.getFullName() != null && !user.getFullName().trim().isEmpty())
                            ? user.getFullName()
                            : prefsManager.getUserName();
                    tvUserNameHeader.setText(buildShortDisplayName(fullName));
                }

                String photoUrl = null;
                if (user.getPersonalData() != null) {
                    photoUrl = user.getPersonalData().getProfileImageUrl();
                }

                // Toolbar avatar
                if (ivAvatarAction != null) {
                    Glide.with(ClientMainActivity.this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_avatar_24)
                            .error(R.drawable.ic_avatar_24)
                            .circleCrop()
                            .into(ivAvatarAction);
                }

                // Navigation header avatar
                if (ivProfileHeader != null) {
                    Glide.with(ClientMainActivity.this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_avatar_24)
                            .error(R.drawable.ic_avatar_24)
                            .circleCrop()
                            .into(ivProfileHeader);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando foto de perfil (Firestore)", e);

                if (ivAvatarAction != null) ivAvatarAction.setImageResource(R.drawable.ic_avatar_24);
                if (ivProfileHeader != null) ivProfileHeader.setImageResource(R.drawable.ic_avatar_24);
            }
        });
    }

    private void setupClickListeners() {
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
        rvFeaturedTours.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedTours.setAdapter(new FeaturedToursAdapter(featuredTours, this::onFeaturedTourClick));

        rvPopularCompanies.setLayoutManager(new LinearLayoutManager(this));
        rvPopularCompanies.setAdapter(new PopularCompaniesAdapter(popularCompanies, this::onCompanyClick));

        loadFeaturedToursFromFirebase();
        loadPopularCompaniesFromFirebase();
    }

    private void loadFeaturedToursFromFirebase() {
        showEmptyFeaturedTours(true);
        featuredTours.clear();
        if (rvFeaturedTours.getAdapter() != null) rvFeaturedTours.getAdapter().notifyDataSetChanged();
    }

    private void loadPopularCompaniesFromFirebase() {
        // Mostrar estado vacío inicialmente (opcional)
        showEmptyPopularCompanies(true);

        // Limpiar lista actual
        popularCompanies.clear();

        // Cargar empresas desde Firebase
        firestoreManager.getAllCompaniesNoFilter(new FirestoreManager.FirestoreCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<Company> allCompanies = (List<Company>) result;

                    // Agregar todas las empresas o filtrar por algún criterio
                    // Ejemplo: empresas que tienen tours activos
                    for (Company company : allCompanies) {
                        // Aquí puedes agregar lógica de filtrado si es necesario
                        popularCompanies.add(company);

                        // Para mostrar solo las primeras 5 o 10
                        if (popularCompanies.size() >= 10) {
                            break;
                        }
                    }

                    // Actualizar UI según si hay datos o no
                    if (popularCompanies.isEmpty()) {
                        showEmptyPopularCompanies(true);
                        Log.d(TAG, "No hay empresas populares disponibles");
                    } else {
                        showEmptyPopularCompanies(false);

                        // Actualizar adapter
                        if (rvPopularCompanies.getAdapter() != null) {
                            rvPopularCompanies.getAdapter().notifyDataSetChanged();
                        }

                        Log.d(TAG, "Empresas populares cargadas: " + popularCompanies.size());
                    }
                } else {
                    showEmptyPopularCompanies(true);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando empresas populares", e);
                showEmptyPopularCompanies(true);
                Toast.makeText(ClientMainActivity.this,
                        "Error al cargar empresas populares", Toast.LENGTH_SHORT).show();
            }
        });
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
            prefsManager.logout();

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

    private void correctUserData() {
        if (!prefsManager.isLoggedIn()) return;
        // No-op por ahora
    }

    private void loadUserData() {
        String userName = prefsManager.getUserName();

        if (userName != null && !userName.isEmpty()) {
            String firstName = userName.split(" ")[0];
            tvWelcomeMessage.setText("¡Hola, " + firstName + "!");
        } else {
            tvWelcomeMessage.setText("¡Hola!");
        }

        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvName = headerView.findViewById(R.id.tv_user_name_header);
                if (tvName != null) {
                    tvName.setText(buildShortDisplayName(userName));
                }
            }
        }
    }

    private void updateActiveReservationsCount() {
        if (currentUserId == null) {
            tvActiveReservations.setText("0 reservas activas");
            return;
        }

        firestoreManager.getReservationsByUser(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            @SuppressWarnings("unchecked")
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
                Log.e(TAG, "Error cargando contador de reservas activas", e);
                tvActiveReservations.setText("0 reservas activas");
            }
        });
    }

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

        String displayName = company.getCommercialName() != null ?
                company.getCommercialName() : company.getBusinessName();
        companyName.setText(displayName != null ? displayName : "Empresa");

        location.setText("📍 " + (company.getAddress() != null ? company.getAddress() : "Ubicación no disponible"));

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
