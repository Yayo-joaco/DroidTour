package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.managers.PrefsManager;
import com.example.droidtour.models.Notification;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.TourOffer;
import com.example.droidtour.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TourGuideMainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView rvPendingOffers, rvUpcomingTours;

    private LinearLayout emptyPendingOffers;
    private LinearLayout emptyUpcomingTours;
    private MaterialCardView cardActiveTour, cardQRScanner, cardLocationTracking;
    private TextView tvViewAllOffers, tvViewAllTours;
    
    // Dashboard Stats TextViews
    private TextView tvGuideStatus, tvGuideRating, tvMonthlyEarnings, tvCompletedTours;
    private TextView tvActiveTourName, tvActiveTourProgress;
    private MaterialButton btnContinueTour;
    
    // Storage y Notificaciones
    private PrefsManager prefsManager;
    private NotificationHelper notificationHelper;
    
    // Firebase
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private String currentUserId;
    private Tour activeTour;
    
    // Toolbar menu elements
    private FrameLayout notificationActionLayout, avatarActionLayout;
    private TextView tvNotificationBadge;
    private ImageView ivAvatarAction;
    private int notificationCount = 0;
    
    // Flag para verificar si la actividad está completamente inicializada
    private boolean isActivityInitialized = false;

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar helpers
        prefsManager = new PrefsManager(this);
        notificationHelper = new NotificationHelper(this);
        
        // Inicializar Firebase
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);
        currentUserId = authManager.getCurrentUserId();
        
        // Fallback a PreferencesManager
        if (currentUserId == null || currentUserId.isEmpty()) {
            // Usar el userId guardado en PreferencesManager si existe
            com.example.droidtour.utils.PreferencesManager utilsPrefs = 
                new com.example.droidtour.utils.PreferencesManager(this);
            currentUserId = utilsPrefs.getUserId();
        }
        
        // Validar estado de aprobación del guía antes de permitir acceso
        if (currentUserId != null && !currentUserId.isEmpty()) {
            checkGuideApprovalStatus();
        } else {
            // Si no hay userId, redirigir a login
            redirectToLogin();
            finish();
            return;
        }
    }

    /**
     * Validar estado de aprobación del guía antes de permitir acceso
     */
    private void checkGuideApprovalStatus() {
        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User userObj = (com.example.droidtour.models.User) result;
                String statusField = userObj.getStatus();

                if (statusField != null && ("inactive".equalsIgnoreCase(statusField) ||
                        "suspended".equalsIgnoreCase(statusField))) {
                    redirectToUserDisabled();
                    return;
                }

                // Revisar user_roles para verificar estado de aprobación
                firestoreManager.getUserRoles(currentUserId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> rolesData = (java.util.Map<String, Object>) result;

                        String guideStatus = extractGuideStatus(rolesData);

                        if ("active".equals(guideStatus)) {
                            // Guía aprobado - continuar con onCreate
                            continueOnCreate();
                        } else {
                            // Guía no aprobado - redirigir a pantalla de espera
                            redirectToApprovalPending();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        android.util.Log.e("TourGuideMain", "Error al obtener user_roles", e);
                        // Por seguridad, redirigir a pantalla de espera si no se puede verificar
                        redirectToApprovalPending();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "Error al obtener usuario", e);
                // Por seguridad, redirigir a pantalla de espera si no se puede verificar
                redirectToApprovalPending();
            }
        });
    }

    /**
     * Continuar con la inicialización normal de la actividad
     */
    private void continueOnCreate() {
        setContentView(R.layout.activity_tour_guide_main);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupRecyclerViews();
        setupClickListeners();
        setupSwipeRefresh();
        
        // Cargar datos del usuario y actualizar vistas
        loadUserData();
        
        // Cargar notificaciones desde Firebase
        loadNotificationsCount();
        
        // Cargar estadísticas del guía desde Firebase
        loadGuideStats();
        
        // Cargar tour activo
        loadActiveTour();
        
        // Marcar la actividad como completamente inicializada
        isActivityInitialized = true;
    }

    /**
     * Extraer estado de guía desde diferentes estructuras posibles
     */
    private String extractGuideStatus(java.util.Map<String, Object> rolesData) {
        // Estructura 1: directa
        if (rolesData.containsKey("status")) {
            return (String) rolesData.get("status");
        }

        // Estructura 2: bajo "guide"
        if (rolesData.containsKey("guide")) {
            Object guideObj = rolesData.get("guide");
            if (guideObj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> guideMap = (java.util.Map<String, Object>) guideObj;
                if (guideMap.containsKey("status")) {
                    return (String) guideMap.get("status");
                }
            }
        }

        // Estructura 3: bajo "roles.guide"
        if (rolesData.containsKey("roles")) {
            Object rolesObj = rolesData.get("roles");
            if (rolesObj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> rolesMap = (java.util.Map<String, Object>) rolesObj;
                Object guideRole = rolesMap.get("guide");
                if (guideRole instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> guideMap = (java.util.Map<String, Object>) guideRole;
                    return (String) guideMap.get("status");
                }
            }
        }

        return null;
    }

    private void redirectToApprovalPending() {
        Intent intent = new Intent(this, GuideApprovalPendingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToUserDisabled() {
        Intent intent = new Intent(this, UserDisabledActivity.class);
        intent.putExtra("userId", currentUserId);
        intent.putExtra("reason", "Tu cuenta ha sido desactivada. Contacta con soporte.");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void openLocationTracking() {
        // Si ya tenemos el tour activo cargado, usarlo directamente
        if (activeTour != null && "EN_PROGRESO".equals(activeTour.getTourStatus())) {
            Intent intent = new Intent(this, LocationTrackingActivity.class);
            intent.putExtra("tour_id", activeTour.getTourId());
            startActivity(intent);
            return;
        }
        
        // Si no, buscar el tour activo primero
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "No se pudo obtener tu ID de usuario", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar mensaje de carga
        Toast.makeText(this, "Cargando tour activo...", Toast.LENGTH_SHORT).show();
        
        firestoreManager.getToursByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> allTours = (List<Tour>) result;
                
                // Buscar tour EN_PROGRESO
                Tour foundActiveTour = null;
                for (Tour tour : allTours) {
                    if ("EN_PROGRESO".equals(tour.getTourStatus())) {
                        foundActiveTour = tour;
                        break;
                    }
                }
                
                if (foundActiveTour != null) {
                    Intent intent = new Intent(TourGuideMainActivity.this, LocationTrackingActivity.class);
                    intent.putExtra("tour_id", foundActiveTour.getTourId());
                    startActivity(intent);
                } else {
                    Toast.makeText(TourGuideMainActivity.this, 
                        "No tienes ningún tour en progreso", 
                        Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(TourGuideMainActivity.this, 
                    "Error al cargar tour: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Solo ejecutar si la actividad está completamente inicializada
        if (isActivityInitialized) {
            // ✅ RECARGAR DASHBOARD CADA VEZ QUE REGRESAS
            setupRecyclerViews();
            // Recargar contador de notificaciones
            loadNotificationsCount();
        }
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        rvPendingOffers = findViewById(R.id.rv_pending_offers);
        rvUpcomingTours = findViewById(R.id.rv_upcoming_tours);
        cardActiveTour = findViewById(R.id.card_active_tour);
        cardQRScanner = findViewById(R.id.card_qr_scanner);
        cardLocationTracking = findViewById(R.id.card_location_tracking);
        tvViewAllOffers = findViewById(R.id.tv_view_all_offers);
        tvViewAllTours = findViewById(R.id.tv_view_all_tours);


        emptyPendingOffers = findViewById(R.id.empty_pending_offers);
        emptyUpcomingTours = findViewById(R.id.empty_upcoming_tours);
        
        // Dashboard Stats
        tvGuideStatus = findViewById(R.id.tv_guide_status);
        tvGuideRating = findViewById(R.id.tv_guide_rating);
        tvMonthlyEarnings = findViewById(R.id.tv_monthly_earnings);
        tvCompletedTours = findViewById(R.id.tv_completed_tours);
        
        // Active Tour elements
        tvActiveTourName = findViewById(R.id.tv_active_tour_name);
        tvActiveTourProgress = findViewById(R.id.tv_active_tour_progress);
        btnContinueTour = findViewById(R.id.btn_continue_tour);


        swipeRefresh = findViewById(R.id.swipe_refresh);



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
            Intent intent = new Intent(this, GuideNotificationsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_profile) {
            // Abrir pantalla de "Mi cuenta" al seleccionar la opción de perfil
            Intent intentProfileMenu = new Intent(this, GuideMyAccount.class);
            startActivity(intentProfileMenu);
            return true;
        }
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
                    Intent intent = new Intent(this, GuideNotificationsActivity.class);
                    startActivity(intent);
                });
            }
        }

        MenuItem avatarItem = menu.findItem(R.id.action_profile);
        if (avatarItem != null) {
            avatarActionLayout = (FrameLayout) avatarItem.getActionView();
            if (avatarActionLayout != null) {
                ivAvatarAction = avatarActionLayout.findViewById(R.id.iv_avatar_action);
                avatarActionLayout.setOnClickListener(v -> {
                    Intent intentProfileMenu = new Intent(this, GuideMyAccount.class);
                    startActivity(intentProfileMenu);
                });
            }
        }
    }

    private void updateNotificationBadge() {
        if (tvNotificationBadge != null) {
            if (notificationCount > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(String.valueOf(Math.min(notificationCount, 9)));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
    
    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, findViewById(R.id.toolbar),
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Actualizar nombre de usuario en el header del drawer
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
            if (tvUserNameHeader != null) {
                String userType = prefsManager.obtenerTipoUsuario();
                // Cargar nombre del usuario actual desde PreferencesManager
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

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_home) {
                // Already in home
            } else if (id == R.id.nav_available_tours) {
                startActivity(new Intent(this, TourOffersActivity.class));
            } else if (id == R.id.nav_my_tours) {
                startActivity(new Intent(this, GuideActiveToursActivity.class));
            } else if (id == R.id.nav_tour_location) {
                // Obtener el tour activo del guía antes de abrir LocationTrackingActivity
                openLocationTracking();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, GuideProfileActivity.class));
            }  else if (id == R.id.nav_logout) {
                // Handle logout - limpiar sesión correctamente
                prefsManager.cerrarSesion();

                // Redirigir a LoginActivity con flags para limpiar stack
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

                Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
            }
            
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupRecyclerViews() {
        // Configurar layout managers
        LinearLayoutManager offersLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvPendingOffers.setLayoutManager(offersLayoutManager);
        
        LinearLayoutManager toursLayoutManager = new LinearLayoutManager(this);
        rvUpcomingTours.setLayoutManager(toursLayoutManager);
        
        // Cargar datos desde Firebase
        loadPendingOffersFromFirebase();
        loadUpcomingToursFromFirebase();
    }
    
    /**
     * Cargar ofertas pendientes desde Firebase
     */
    private void loadPendingOffersFromFirebase() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            android.util.Log.e("TourGuideMain", "❌ Error: currentUserId es null");
            showEmptyPendingOffers(true);
            return;
        }
        
        firestoreManager.getOffersByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<TourOffer> allOffers = (List<TourOffer>) result;

                // Filtrar solo ofertas pendientes
                List<TourOffer> pendingOffers = new java.util.ArrayList<>();
                for (TourOffer offer : allOffers) {
                    if ("PENDIENTE".equals(offer.getStatus())) {
                        pendingOffers.add(offer);
                    }
                }

                if (pendingOffers.isEmpty()) {
                    showEmptyPendingOffers(true);
                    android.util.Log.d("TourGuideMain", "ℹNo hay ofertas pendientes");
                } else {
                    showEmptyPendingOffers(false);

                    // Actualizar adapter
                    rvPendingOffers.setAdapter(new PendingOffersAdapterFirebase(pendingOffers, TourGuideMainActivity.this::onOfferClick));
                    android.util.Log.d("TourGuideMain", "Ofertas pendientes cargadas: " + pendingOffers.size());
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando ofertas", e);

                // ✅ MOSTRAR ESTADO VACÍO EN CASO DE ERROR
                showEmptyPendingOffers(true);

                // Opcional: mostrar mensaje de error
                Toast.makeText(TourGuideMainActivity.this,
                        "Error al cargar ofertas", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Cargar tours programados desde Firebase
     */
    private void loadUpcomingToursFromFirebase() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            android.util.Log.e("TourGuideMain", "❌ Error: currentUserId es null");
            showEmptyUpcomingTours(true);
            return;
        }
        
        firestoreManager.getToursByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Tour> allTours = (List<Tour>) result;

                // Filtrar solo tours confirmados
                List<Tour> upcomingTours = new java.util.ArrayList<>();
                for (Tour tour : allTours) {
                    if ("CONFIRMADA".equals(tour.getTourStatus())) {
                        upcomingTours.add(tour);
                    }
                }

                if (upcomingTours.isEmpty()) {
                    // ✅ MOSTRAR ESTADO VACÍO
                    showEmptyUpcomingTours(true);
                    android.util.Log.d("TourGuideMain", "ℹ️ No hay próximos tours");
                } else {
                    // ✅ OCULTAR ESTADO VACÍO Y MOSTRAR LISTA
                    showEmptyUpcomingTours(false);

                    // Actualizar adapter
                    rvUpcomingTours.setAdapter(new UpcomingToursAdapterFirebase(upcomingTours, TourGuideMainActivity.this::onTourClick));
                    android.util.Log.d("TourGuideMain", "✅ Tours programados cargados: " + upcomingTours.size());
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando tours", e);
                Toast.makeText(TourGuideMainActivity.this, 
                    "Error al cargar tours programados", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Muestra u oculta el estado vacío para ofertas pendientes
     */
    private void showEmptyPendingOffers(boolean showEmpty) {
        if (emptyPendingOffers != null && rvPendingOffers != null) {
            if (showEmpty) {
                emptyPendingOffers.setVisibility(View.VISIBLE);
                rvPendingOffers.setVisibility(View.GONE);
            } else {
                emptyPendingOffers.setVisibility(View.GONE);
                rvPendingOffers.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Muestra u oculta el estado vacío para próximos tours
     */
    private void showEmptyUpcomingTours(boolean showEmpty) {
        if (emptyUpcomingTours != null && rvUpcomingTours != null) {
            if (showEmpty) {
                emptyUpcomingTours.setVisibility(View.VISIBLE);
                rvUpcomingTours.setVisibility(View.GONE);
            } else {
                emptyUpcomingTours.setVisibility(View.GONE);
                rvUpcomingTours.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupClickListeners() {
        // View All Offers
        tvViewAllOffers.setOnClickListener(v -> {
            Intent intent = new Intent(this, TourOffersActivity.class);
            startActivity(intent);
        });
        
        // View All Tours
        tvViewAllTours.setOnClickListener(v -> {
            Intent intent = new Intent(this, GuideActiveToursActivity.class);
            startActivity(intent);
        });
        
        // Note: Active tour card, QR Scanner, and Location Tracking clicks 
        // are configured dynamically in checkActiveTour() when a tour is active
    }

    private void setupSwipeRefresh() {
        // Configurar colores del refresh indicator
        swipeRefresh.setColorSchemeResources(
                R.color.primary,
                R.color.accent,
                R.color.green
        );

        // Configurar el listener para el refresh
        swipeRefresh.setOnRefreshListener(() -> {
            // Mostrar mensaje opcional
            Toast.makeText(TourGuideMainActivity.this,
                    "Actualizando datos...",
                    Toast.LENGTH_SHORT).show();

            // Recargar todos los datos
            refreshAllData();
        });
    }

    private void refreshAllData() {
        // Contador para saber cuándo terminar el refresh
        final int[] pendingTasks = {5}; // 5 tareas en total

        // Metodo helper para verificar si todas las tareas han terminado
        Runnable checkComplete = () -> {
            pendingTasks[0]--;
            if (pendingTasks[0] <= 0) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(TourGuideMainActivity.this,
                        "Datos actualizados",
                        Toast.LENGTH_SHORT).show();
            }
        };

        // 1. Recargar datos del usuario
        loadUserData();
        checkComplete.run();

        // 2. Recargar notificaciones
        loadNotificationsCount();
        checkComplete.run();

        // 3. Recargar estadísticas
        loadGuideStats();
        checkComplete.run();

        // 4. Recargar ofertas pendientes
        firestoreManager.getOffersByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                loadPendingOffersFromFirebase();
                checkComplete.run();
            }

            @Override
            public void onFailure(Exception e) {
                loadPendingOffersFromFirebase();
                checkComplete.run();
            }
        });

        // 5. Recargar tours programados y tour activo
        firestoreManager.getToursByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                loadUpcomingToursFromFirebase();
                loadActiveTour();
                checkComplete.run();
            }

            @Override
            public void onFailure(Exception e) {
                loadUpcomingToursFromFirebase();
                loadActiveTour();
                checkComplete.run();
            }
        });
    }


    private void onOfferClick(int position) {
        Intent intent = new Intent(this, TourOfferDetailActivity.class);
        intent.putExtra("offer_id", position);
        startActivity(intent);
    }

    private void onTourClick(int position) {
        Intent intent = new Intent(this, GuideActiveTourDetailActivity.class);
        intent.putExtra("tour_id", position);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ============================= ADAPTERS =============================

    // Adapters antiguos eliminados - ahora se usan PendingOffersAdapterFirebase y UpcomingToursAdapterFirebase

    // Click listener interface
    private interface OnOfferClickListener {
        void onClick(int position);
    }
    
    private interface OnTourClickListener {
        void onClick(int position);
    }
    
    // ==================== STORAGE LOCAL ====================

    private void loadUserData() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            android.util.Log.e("TourGuideMain", "❌ Error: currentUserId es null");
            return;
        }

        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;

                // Extraer nombre desde personalData
                String fullName = extractFullName(user);

                // Obtener URL de la imagen de perfil
                String photoUrl = null;
                if (user.getPersonalData() != null) {
                    photoUrl = user.getPersonalData().getProfileImageUrl();
                }
                
                // También intentar obtener desde getPhotoUrl() (método legacy)
                if (photoUrl == null || photoUrl.isEmpty()) {
                    photoUrl = user.getPhotoUrl();
                }
                
                // 📸 CARGAR FOTO DE PERFIL EN EL AVATAR DEL TOOLBAR
                if (ivAvatarAction != null) {
                    if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http")) {
                        android.util.Log.d("TourGuideMain", "📸 Cargando avatar en toolbar desde URL: " + photoUrl);
                        Glide.with(TourGuideMainActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar_24)
                                .error(R.drawable.ic_avatar_24)
                                .transform(new CircleCrop())
                                .into(ivAvatarAction);
                    } else {
                        ivAvatarAction.setImageResource(R.drawable.ic_avatar_24);
                    }
                }
                
                // Actualizar nombre en el header del drawer
                NavigationView navigationView = findViewById(R.id.nav_view);
                if (navigationView != null) {
                    View headerView = navigationView.getHeaderView(0);
                    if (headerView != null) {
                        TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
                        if (tvUserNameHeader != null) {
                            String displayName = getDisplayName(fullName);
                            tvUserNameHeader.setText(displayName);
                        }
                        
                        // 📸 CARGAR FOTO DE PERFIL EN EL DRAWER
                        android.widget.ImageView ivAvatarHeader = headerView.findViewById(R.id.iv_avatar_header);
                        if (ivAvatarHeader != null) {
                            if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http")) {
                                android.util.Log.d("TourGuideMain", "📸 Cargando avatar en drawer desde URL: " + photoUrl);
                                Glide.with(TourGuideMainActivity.this)
                                        .load(photoUrl)
                                        .placeholder(R.drawable.ic_avatar_24)
                                        .error(R.drawable.ic_avatar_24)
                                        .transform(new CircleCrop())
                                        .into(ivAvatarHeader);
                            } else {
                                ivAvatarHeader.setImageResource(R.drawable.ic_avatar_24);
                            }
                        }
                    }
                }

                android.util.Log.d("TourGuideMain", "✅ Nombre del drawer actualizado desde Firebase");
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando usuario para drawer", e);
                // Fallback a PreferencesManager
                updateHeaderFromPrefs();
            }
        });
    }
    private String extractFullName(com.example.droidtour.models.User user) {
        if (user.getPersonalData() != null) {
            String fullName = user.getPersonalData().getFullName();
            if (fullName != null && !fullName.isEmpty()) {
                return fullName;
            }

            String firstName = user.getPersonalData().getFirstName();
            String lastName = user.getPersonalData().getLastName();
            if (firstName != null || lastName != null) {
                return ((firstName != null ? firstName : "") + " " +
                        (lastName != null ? lastName : "")).trim();
            }
        }
        return "Usuario";
    }

    private String getDisplayName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "Usuario";
        }

        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1]; // Nombre + Apellido
        } else if (parts.length == 1) {
            return parts[0];
        }
        return "Usuario";
    }

    // MÉTODO HELPER PARA FALLBACK:
    private void updateHeaderFromPrefs() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
                if (tvUserNameHeader != null) {
                    String userName = prefsManager.obtenerUsuario();
                    tvUserNameHeader.setText(userName != null && !userName.isEmpty() ? userName : "Usuario");
                }
            }
        }
    }

    // MÉTODO loadGuideStats() CORREGIDO:
    private void loadGuideStats() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            android.util.Log.e("TourGuideMain", "❌ Error: currentUserId es null");
            return;
        }

        // 1. Cargar rating desde user_roles o guides (ya que User no tiene guideRating)
        loadGuideRatingFromFirebase();

        // 2. Cargar tours para calcular tours completados y ganancias
        firestoreManager.getToursByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Tour> allTours = (List<Tour>) result;

                int completedCount = 0;
                double totalEarnings = 0.0;

                for (Tour tour : allTours) {
                    if ("COMPLETADA".equals(tour.getTourStatus())) {
                        completedCount++;
                        Double price = tour.getPricePerPerson();
                        if (price != null) {
                            totalEarnings += price;
                        }
                    }
                }

                // Actualizar UI
                tvCompletedTours.setText(completedCount + " Tours");
                tvMonthlyEarnings.setText(String.format("S/. %.0f", totalEarnings));
                tvGuideStatus.setText("Estado: APROBADO");

                android.util.Log.d("TourGuideMain", "✅ Stats: " + completedCount + " tours, S/. " + totalEarnings);
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando reservas", e);
                tvCompletedTours.setText("0 Tours");
                tvMonthlyEarnings.setText("S/. 0");
                tvGuideStatus.setText("Estado: APROBADO");
            }
        });
    }

    // Cargar rating desde colección guides (modelo actualizado con reviews)
    private void loadGuideRatingFromFirebase() {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection(FirestoreManager.COLLECTION_GUIDES)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        com.example.droidtour.models.Guide guide = doc.toObject(com.example.droidtour.models.Guide.class);
                        if (guide != null) {
                            Float rating = guide.getRating();
                            Integer totalReviews = guide.getTotalReviews();
                            
                            // Actualizar UI con el rating
                            if (rating != null && rating > 0) {
                                String ratingText = String.format("Calificación: ⭐ %.1f", rating);
                                if (totalReviews != null && totalReviews > 0) {
                                    ratingText += String.format(" (%d reseñas)", totalReviews);
                                }
                                tvGuideRating.setText(ratingText);
                            } else {
                                tvGuideRating.setText("Calificación: ⭐ 0.0");
                            }
                        } else {
                            tvGuideRating.setText("Calificación: ⭐ 0.0");
                        }
                    } else {
                        tvGuideRating.setText("Calificación: ⭐ 0.0");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.w("TourGuideMain", "Error cargando rating desde guides: " + e.getMessage());
                    tvGuideRating.setText("Calificación: ⭐ 0.0");
                });
    }

    
    // ==================== NOTIFICACIONES ====================
    
    /**
     * Cargar contador de notificaciones no leídas desde Firebase
     */
    private void loadNotificationsCount() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        
        firestoreManager.getUnreadNotifications(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Notification> unreadNotifications = (List<Notification>) result;
                notificationCount = unreadNotifications.size();
                updateNotificationBadge();
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "Error cargando notificaciones", e);
                // No mostrar error al usuario, solo log
            }
        });
    }
    
    private void testNotifications() {
        // Método de prueba para enviar notificaciones de ejemplo
        notificationHelper.sendNewOfferNotification(
            "Tour Paracas Full Day", 
            "Coastal Adventures", 
            350.0
        );
    }
    
    /**
     * Cargar tour activo (EN_PROGRESO) desde Firebase
     */
    private void loadActiveTour() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            cardActiveTour.setVisibility(View.GONE);
            return;
        }
        
        firestoreManager.getToursByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> allTours = (List<Tour>) result;
                
                // Buscar tour EN_PROGRESO
                Tour foundActiveTour = null;
                for (Tour tour : allTours) {
                    if ("EN_PROGRESO".equals(tour.getTourStatus())) {
                        foundActiveTour = tour;
                        break;
                    }
                }
                
                final Tour finalActiveTour = foundActiveTour;
                
                if (finalActiveTour != null) {
                    activeTour = finalActiveTour;
                    cardActiveTour.setVisibility(View.VISIBLE);
                    tvActiveTourName.setText(finalActiveTour.getTourName());
                    
                    // Calcular progreso dinámicamente
                    String progressText = calculateTourProgress(finalActiveTour);
                    tvActiveTourProgress.setText(progressText);
                    
                    btnContinueTour.setOnClickListener(v -> {
                        // Ir a LocationTrackingActivity con el tour activo
                        Intent intent = new Intent(TourGuideMainActivity.this, LocationTrackingActivity.class);
                        intent.putExtra("tour_id", finalActiveTour.getTourId());
                        startActivity(intent);
                    });
                    
                    // Configurar botones de acciones rápidas
                    cardQRScanner.setOnClickListener(v -> {
                        Intent intent = new Intent(TourGuideMainActivity.this, QRScannerActivity.class);
                        intent.putExtra("tour_name", finalActiveTour.getTourName());
                        intent.putExtra("tour_id", finalActiveTour.getTourId());
                        startActivity(intent);
                    });
                    
                    cardLocationTracking.setOnClickListener(v -> {
                        Intent intent = new Intent(TourGuideMainActivity.this, LocationTrackingActivity.class);
                        intent.putExtra("tour_id", finalActiveTour.getTourId());
                        startActivity(intent);
                    });
                    
                    android.util.Log.d("TourGuideMain", "✅ Tour activo encontrado: " + finalActiveTour.getTourName());
                } else {
                    // No hay tour activo, ocultar card
                    cardActiveTour.setVisibility(View.GONE);
                    android.util.Log.d("TourGuideMain", "ℹ️ No hay tour activo");
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando tour activo", e);
                cardActiveTour.setVisibility(View.GONE);
            }
        });
    }
    
    // ==================== ADAPTERS FIREBASE ====================
    
    /**
     * Adapter para ofertas pendientes usando Firebase
     */
    private class PendingOffersAdapterFirebase extends RecyclerView.Adapter<PendingOffersAdapterFirebase.ViewHolder> {
        
        private final OnOfferClickListener listener;
        private final List<TourOffer> offers;

        PendingOffersAdapterFirebase(List<TourOffer> offers, OnOfferClickListener listener) {
            this.offers = offers;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_pending_offer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TourOffer offer = offers.get(position);
            
            holder.tvTourName.setText(offer.getTourName() != null ? offer.getTourName() : "Tour");
            holder.tvCompanyName.setText(offer.getCompanyName() != null ? offer.getCompanyName() : "Empresa");
            holder.tvTourDate.setText(offer.getTourDate() != null ? offer.getTourDate() : "Fecha");
            holder.tvTourTime.setText(offer.getTourTime() != null ? offer.getTourTime() : "Hora");
            holder.tvPaymentAmount.setText(String.format("S/. %.0f", offer.getPaymentAmount() != null ? offer.getPaymentAmount() : 0.0));
            holder.tvParticipants.setText((offer.getNumberOfParticipants() != null ? offer.getNumberOfParticipants() : 0) + " personas");

            holder.btnAccept.setOnClickListener(v -> {
                // Aceptar oferta desde el dashboard con validación completa
                if (offer.getOfferId() != null && offer.getTourId() != null) {
                    String offerDate = offer.getTourDate();
                    String guideId = currentUserId;
                    
                    // 1. Verificar que no tenga otro tour en la misma fecha
                    firestoreManager.getToursByGuide(guideId, new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            List<Tour> existingTours = (List<Tour>) result;
                            
                            // Validar conflicto de fechas
                            for (Tour tour : existingTours) {
                                if (offerDate != null && offerDate.equals(tour.getTourDate())) {
                                    String status = tour.getTourStatus();
                                    if ("EN_PROGRESO".equals(status) || "CONFIRMADA".equals(status)) {
                                        Toast.makeText(TourGuideMainActivity.this, 
                                            "❌ Ya tienes un tour aceptado para el " + offerDate, 
                                            Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                }
                            }
                            
                            // 2. No hay conflicto, obtener el tour completo para determinar status preciso
                            firestoreManager.getTourById(offer.getTourId(), new FirestoreManager.TourCallback() {
                                @Override
                                public void onSuccess(Tour newTour) {
                                    if (newTour == null) {
                                        Toast.makeText(TourGuideMainActivity.this, "Error: Tour no encontrado", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    
                                    // Actualizar estado de la oferta
                                    firestoreManager.updateOfferStatus(offer.getOfferId(), "ACEPTADA", 
                                        new FirestoreManager.FirestoreCallback() {
                                            @Override
                                            public void onSuccess(Object result) {
                                                // Determinar el status del tour usando fecha, hora inicio y fin
                                                String tourStatus = determineTourStatusForDashboard(existingTours, newTour);
                                        
                                                // Actualizar el tour con assignedGuideId, nombre y status
                                                Map<String, Object> tourUpdates = new HashMap<>();
                                                tourUpdates.put("assignedGuideId", guideId);
                                                tourUpdates.put("assignedGuideName", offer.getGuideName());
                                                tourUpdates.put("tourStatus", tourStatus);
                                                tourUpdates.put("guidePayment", offer.getPaymentAmount());
                                                tourUpdates.put("isPublic", true);
                                                tourUpdates.put("isActive", true);
                                                
                                                android.util.Log.d("TourGuideMain", "🎯 Actualizando tour " + offer.getTourId() + " con status: " + tourStatus);
                                                
                                                firestoreManager.updateTour(offer.getTourId(), tourUpdates, 
                                                    new FirestoreManager.FirestoreCallback() {
                                                        @Override
                                                        public void onSuccess(Object result) {
                                                            android.util.Log.d("TourGuideMain", "✅ Tour actualizado exitosamente");
                                                            
                                                            // Si este tour es EN_PROGRESO, actualizar otros a CONFIRMADA
                                                            if ("EN_PROGRESO".equals(tourStatus)) {
                                                                updateOtherToursToConfirmedFromDashboard(guideId, offer.getTourId());
                                                            }
                                                            
                                                            Toast.makeText(TourGuideMainActivity.this, 
                                                                "✅ Oferta aceptada - Tour agregado a Mis Tours", 
                                                                Toast.LENGTH_LONG).show();
                                                            
                                                            // Recargar ofertas y dashboard
                                                            loadPendingOffersFromFirebase();
                                                            loadActiveTour();
                                                        }
                                                        
                                                        @Override
                                                        public void onFailure(Exception e) {
                                                            android.util.Log.e("TourGuideMain", "❌ Error actualizando tour", e);
                                                            Toast.makeText(TourGuideMainActivity.this, 
                                                                "Error al actualizar tour: " + e.getMessage(), 
                                                                Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                            }
                                            
                                            @Override
                                            public void onFailure(Exception e) {
                                                Toast.makeText(TourGuideMainActivity.this, 
                                                    "Error al aceptar oferta: " + e.getMessage(), 
                                                    Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                }
                                
                                @Override
                                public void onFailure(String error) {
                                    android.util.Log.e("TourGuideMain", "Error obteniendo tour: " + error);
                                    Toast.makeText(TourGuideMainActivity.this, 
                                        "Error obteniendo datos del tour: " + error, 
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(TourGuideMainActivity.this, 
                                "Error verificando disponibilidad: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            holder.btnReject.setOnClickListener(v -> {
                // Rechazar oferta directamente desde el dashboard
                if (offer.getOfferId() != null) {
                    firestoreManager.updateOfferStatus(offer.getOfferId(), "RECHAZADA", 
                        new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                Toast.makeText(TourGuideMainActivity.this, 
                                    "Oferta rechazada", 
                                    Toast.LENGTH_SHORT).show();
                                // Recargar ofertas
                                loadPendingOffersFromFirebase();
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(TourGuideMainActivity.this, 
                                    "Error al rechazar oferta: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            });

            holder.itemView.setOnClickListener(v -> listener.onClick(position));
        }

        @Override
        public int getItemCount() {
            return offers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTourName, tvCompanyName, tvTourDate, tvTourTime, tvPaymentAmount, tvParticipants;
            MaterialButton btnAccept, btnReject;

            ViewHolder(View itemView) {
                super(itemView);
                tvTourName = itemView.findViewById(R.id.tv_tour_name);
                tvCompanyName = itemView.findViewById(R.id.tv_company_name);
                tvTourDate = itemView.findViewById(R.id.tv_tour_date);
                tvTourTime = itemView.findViewById(R.id.tv_tour_time);
                tvPaymentAmount = itemView.findViewById(R.id.tv_payment_amount);
                tvParticipants = itemView.findViewById(R.id.tv_participants);
                btnAccept = itemView.findViewById(R.id.btn_accept);
                btnReject = itemView.findViewById(R.id.btn_reject);
            }
        }
    }
    
    /**
     * Adapter para tours programados usando Firebase
     */
    private class UpcomingToursAdapterFirebase extends RecyclerView.Adapter<UpcomingToursAdapterFirebase.ViewHolder> {
        
        private final OnTourClickListener listener;
        private final List<Tour> tours;

        UpcomingToursAdapterFirebase(List<Tour> tours, OnTourClickListener listener) {
            this.tours = tours;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_upcoming_tour, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Tour tour = tours.get(position);
            
            holder.tvTourName.setText(tour.getTourName() != null ? tour.getTourName() : "Tour");
            holder.tvCompanyName.setText(tour.getCompanyName() != null ? tour.getCompanyName() : "Empresa");
            holder.tvTourDate.setText(tour.getTourDate() != null ? tour.getTourDate() : "Fecha");
            holder.tvTourTime.setText(tour.getStartTime() != null ? tour.getStartTime() : "Hora");
            holder.tvGroupSize.setText((tour.getMaxGroupSize() != null ? tour.getMaxGroupSize() : 0) + " personas");
            holder.tvStatus.setText(tour.getTourStatus() != null ? tour.getTourStatus() : "PENDIENTE");

            holder.itemView.setOnClickListener(v -> listener.onClick(position));
        }

        @Override
        public int getItemCount() {
            return tours.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTourName, tvCompanyName, tvTourDate, tvTourTime, tvGroupSize, tvStatus;

            ViewHolder(View itemView) {
                super(itemView);
                tvTourName = itemView.findViewById(R.id.tv_tour_name);
                tvCompanyName = itemView.findViewById(R.id.tv_company_name);
                tvTourDate = itemView.findViewById(R.id.tv_tour_date);
                tvTourTime = itemView.findViewById(R.id.tv_tour_time);
                tvGroupSize = itemView.findViewById(R.id.tv_participants); // ✅ ID correcto del layout
                tvStatus = itemView.findViewById(R.id.tv_status);
            }
        }
    }
    
    // ==================== MÉTODOS HELPER PARA ACEPTAR OFERTAS ====================
    
    /**
     * Determina el status de un tour usando fecha, hora de inicio y hora de fin para comparaciones precisas
     * La fecha más cercana es EN_PROGRESO, las demás son CONFIRMADA
     */
    private String determineTourStatusForDashboard(List<Tour> existingTours, Tour newTour) {
        if (newTour == null || newTour.getTourDate() == null) {
            return "CONFIRMADA";
        }
        
        // Si no hay tours existentes, verificar si está completado antes de asignar EN_PROGRESO
        if (existingTours == null || existingTours.isEmpty()) {
            if (isTourCompleted(newTour.getTourDate(), newTour.getStartTime(), newTour.getEndTime())) {
                return "COMPLETADA";
            }
            return "EN_PROGRESO";
        }
        
        try {
            // Verificar si el nuevo tour ya está completado
            if (isTourCompleted(newTour.getTourDate(), newTour.getStartTime(), newTour.getEndTime())) {
                return "COMPLETADA";
            }
            
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            
            java.util.Date newDate = dateFormat.parse(newTour.getTourDate());
            java.util.Calendar todayCal = java.util.Calendar.getInstance();
            java.util.Date today = todayCal.getTime();
            
            // Normalizar fechas a medianoche para comparación de fechas
            java.util.Calendar newDateCal = java.util.Calendar.getInstance();
            newDateCal.setTime(newDate);
            newDateCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            newDateCal.set(java.util.Calendar.MINUTE, 0);
            newDateCal.set(java.util.Calendar.SECOND, 0);
            newDateCal.set(java.util.Calendar.MILLISECOND, 0);
            
            java.util.Calendar todayCalNormalized = java.util.Calendar.getInstance();
            todayCalNormalized.setTime(today);
            todayCalNormalized.set(java.util.Calendar.HOUR_OF_DAY, 0);
            todayCalNormalized.set(java.util.Calendar.MINUTE, 0);
            todayCalNormalized.set(java.util.Calendar.SECOND, 0);
            todayCalNormalized.set(java.util.Calendar.MILLISECOND, 0);
            
            // Si la fecha ya pasó completamente (antes de hoy), es COMPLETADA
            if (newDateCal.before(todayCalNormalized)) {
                return "COMPLETADA";
            }
            
            // Buscar el tour más cercano entre todos los tours activos
            Tour closestTour = null;
            java.util.Date closestDate = null;
            
            for (Tour tour : existingTours) {
                String tourStatus = tour.getTourStatus();
                if (!"EN_PROGRESO".equals(tourStatus) && !"CONFIRMADA".equals(tourStatus)) {
                    continue; // Ignorar tours completados o rechazados
                }
                
                String tourDate = tour.getTourDate();
                if (tourDate != null) {
                    java.util.Date existingDate = dateFormat.parse(tourDate);
                    
                    // Normalizar a medianoche
                    java.util.Calendar existingCal = java.util.Calendar.getInstance();
                    existingCal.setTime(existingDate);
                    existingCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    existingCal.set(java.util.Calendar.MINUTE, 0);
                    existingCal.set(java.util.Calendar.SECOND, 0);
                    existingCal.set(java.util.Calendar.MILLISECOND, 0);
                    existingDate = existingCal.getTime();
                    
                    // Solo considerar tours futuros o de hoy
                    if (existingDate.after(todayCalNormalized.getTime()) || 
                        existingDate.equals(todayCalNormalized.getTime())) {
                        
                        if (closestDate == null || existingDate.before(closestDate)) {
                            closestDate = existingDate;
                            closestTour = tour;
                        }
                    }
                }
            }
            
            // Comparar el nuevo tour con el más cercano
            if (closestDate != null) {
                // Si hay un tour más cercano, este nuevo tour es CONFIRMADA
                if (closestDate.before(newDateCal.getTime())) {
                    return "CONFIRMADA";
                }
                // Si tienen la misma fecha, comparar por hora de inicio
                if (closestDate.equals(newDateCal.getTime())) {
                    String newStartTime = newTour.getStartTime();
                    String closestStartTime = closestTour != null ? closestTour.getStartTime() : null;
                    
                    if (newStartTime != null && closestStartTime != null) {
                        try {
                            java.util.Date newStartTimeObj = timeFormat.parse(newStartTime);
                            java.util.Date closestStartTimeObj = timeFormat.parse(closestStartTime);
                            
                            // Si el tour existente empieza antes, el nuevo es CONFIRMADA
                            if (closestStartTimeObj.before(newStartTimeObj)) {
                                return "CONFIRMADA";
                            }
                        } catch (Exception e) {
                            android.util.Log.e("TourGuideMain", "Error parseando hora de inicio", e);
                        }
                    }
                }
            }
            
            // Esta es la fecha más cercana, verificar si debe ser EN_PROGRESO
            // Un tour está EN_PROGRESO si es hoy y ya pasó la hora de inicio pero no la de fin
            if (newDateCal.equals(todayCalNormalized)) {
                if (isTourInProgress(newTour.getStartTime(), newTour.getEndTime())) {
                    return "EN_PROGRESO";
                }
            }
            
            // Si es futuro, puede ser EN_PROGRESO si es el más cercano
            return "EN_PROGRESO";
            
        } catch (Exception e) {
            android.util.Log.e("TourGuideMain", "Error determinando status", e);
            return "CONFIRMADA";
        }
    }
    
    /**
     * Verifica si un tour ya está completado (fecha pasó Y hora de fin pasó)
     */
    private boolean isTourCompleted(String tourDate, String startTime, String endTime) {
        if (tourDate == null) return false;
        
        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.util.Date tourDateObj = dateFormat.parse(tourDate);
            
            java.util.Calendar todayCal = java.util.Calendar.getInstance();
            java.util.Calendar tourCal = java.util.Calendar.getInstance();
            tourCal.setTime(tourDateObj);
            
            // Normalizar ambas fechas a medianoche
            todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            todayCal.set(java.util.Calendar.MINUTE, 0);
            todayCal.set(java.util.Calendar.SECOND, 0);
            todayCal.set(java.util.Calendar.MILLISECOND, 0);
            
            tourCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            tourCal.set(java.util.Calendar.MINUTE, 0);
            tourCal.set(java.util.Calendar.SECOND, 0);
            tourCal.set(java.util.Calendar.MILLISECOND, 0);
            
            // Si la fecha ya pasó completamente, está completado
            if (tourCal.before(todayCal)) {
                return true;
            }
            
            // Si es hoy, verificar si ya pasó la hora de fin
            if (tourCal.equals(todayCal) && endTime != null && !endTime.isEmpty()) {
                try {
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                    java.util.Date endTimeObj = timeFormat.parse(endTime);
                    
                    java.util.Calendar endCal = java.util.Calendar.getInstance();
                    endCal.setTime(tourDateObj);
                    endCal.set(java.util.Calendar.HOUR_OF_DAY, endTimeObj.getHours());
                    endCal.set(java.util.Calendar.MINUTE, endTimeObj.getMinutes());
                    endCal.set(java.util.Calendar.SECOND, 0);
                    endCal.set(java.util.Calendar.MILLISECOND, 0);
                    
                    java.util.Calendar nowCal = java.util.Calendar.getInstance();
                    
                    // Si ya pasó la hora de fin, está completado
                    return nowCal.after(endCal);
                } catch (Exception e) {
                    android.util.Log.e("TourGuideMain", "Error parseando hora de fin: " + endTime, e);
                }
            }
            
            return false;
        } catch (Exception e) {
            android.util.Log.e("TourGuideMain", "Error verificando si tour está completado", e);
            return false;
        }
    }
    
    /**
     * Verifica si un tour está actualmente en progreso (hoy Y hora actual >= startTime Y hora actual < endTime)
     */
    private boolean isTourInProgress(String startTime, String endTime) {
        if (startTime == null || endTime == null) return false;
        
        try {
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            java.util.Date startTimeObj = timeFormat.parse(startTime);
            java.util.Date endTimeObj = timeFormat.parse(endTime);
            
            java.util.Calendar nowCal = java.util.Calendar.getInstance();
            java.util.Calendar startCal = java.util.Calendar.getInstance();
            java.util.Calendar endCal = java.util.Calendar.getInstance();
            
            startCal.set(java.util.Calendar.HOUR_OF_DAY, startTimeObj.getHours());
            startCal.set(java.util.Calendar.MINUTE, startTimeObj.getMinutes());
            startCal.set(java.util.Calendar.SECOND, 0);
            startCal.set(java.util.Calendar.MILLISECOND, 0);
            
            endCal.set(java.util.Calendar.HOUR_OF_DAY, endTimeObj.getHours());
            endCal.set(java.util.Calendar.MINUTE, endTimeObj.getMinutes());
            endCal.set(java.util.Calendar.SECOND, 0);
            endCal.set(java.util.Calendar.MILLISECOND, 0);
            
            // Está en progreso si: hora actual >= hora inicio Y hora actual < hora fin
            return (nowCal.after(startCal) || nowCal.equals(startCal)) && nowCal.before(endCal);
            
        } catch (Exception e) {
            android.util.Log.e("TourGuideMain", "Error verificando si tour está en progreso", e);
            return false;
        }
    }
    
    /**
     * Si el tour recién aceptado es EN_PROGRESO, cambiar otros EN_PROGRESO a CONFIRMADA
     */
    private void updateOtherToursToConfirmedFromDashboard(String guideId, String newTourId) {
        firestoreManager.getToursByGuide(guideId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> allTours = (List<Tour>) result;
                
                for (Tour tour : allTours) {
                    String tourId = tour.getTourId();
                    String status = tour.getTourStatus();
                    
                    // Si no es el nuevo tour y está EN_PROGRESO, cambiar a CONFIRMADA
                    if (!newTourId.equals(tourId) && "EN_PROGRESO".equals(status)) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("tourStatus", "CONFIRMADA");
                        
                        firestoreManager.updateTour(tourId, updates, new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                android.util.Log.d("TourGuideMain", "✅ Tour " + tourId + " cambiado a CONFIRMADA");
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                android.util.Log.e("TourGuideMain", "❌ Error actualizando tour a CONFIRMADA", e);
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error obteniendo tours para actualizar", e);
            }
        });
    }
    
    /**
     * Calcula el texto de progreso del tour basado en las paradas confirmadas
     * Formato: "📍 Parada X de Y • Nombre del lugar"
     */
    private String calculateTourProgress(Tour tour) {
        if (tour == null) return "📍 Sin información";
        
        List<Tour.TourStop> stops = tour.getStops();
        int totalStops = (stops != null) ? stops.size() : 0;
        
        // Si no hay paradas, mostrar punto de encuentro
        if (totalStops == 0) {
            String meetingPoint = (tour.getMeetingPoint() != null) ? tour.getMeetingPoint() : "Sin punto de encuentro";
            return "📍 Parada 0 de 0 • " + meetingPoint;
        }
        
        // Contar paradas confirmadas
        int confirmedStops = 0;
        Tour.TourStop lastConfirmedStop = null;
        
        if (stops != null) {
            for (Tour.TourStop stop : stops) {
                if (stop.getCompleted() != null && stop.getCompleted()) {
                    confirmedStops++;
                    lastConfirmedStop = stop;
                }
            }
        }
        
        // Si no hay ninguna confirmada, mostrar punto de encuentro
        if (confirmedStops == 0) {
            String meetingPoint = (tour.getMeetingPoint() != null) ? tour.getMeetingPoint() : "Punto de encuentro";
            return "📍 Parada 0 de " + totalStops + " • " + meetingPoint;
        }
        
        // Si hay paradas confirmadas, mostrar la última
        String locationName = (lastConfirmedStop != null) ? lastConfirmedStop.getName() : "Parada actual";
        return "📍 Parada " + confirmedStops + " de " + totalStops + " • " + locationName;
    }
}
