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
import com.example.droidtour.models.Reservation;
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
    private Reservation activeTourReservation;
    
    // Toolbar menu elements
    private FrameLayout notificationActionLayout, avatarActionLayout;
    private TextView tvNotificationBadge;
    private ImageView ivAvatarAction;
    private int notificationCount = 0;
    
    // Flag para verificar si la actividad está completamente inicializada
    private boolean isActivityInitialized = false;

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
                startActivity(new Intent(this, LocationTrackingActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, GuideProfileActivity.class));
            } else if (id == R.id.nav_init_test_data) {
                // Intent por nombre de clase para evitar referencia a clase inexistente en tiempo de compilación
                try {
                    Intent intent = new Intent();
                    intent.setClassName(getPackageName(), "com.example.droidtour.firebase.InitializeTestDataActivity");
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Inicializador de datos no disponible", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.nav_logout) {
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
        
        firestoreManager.getReservationsByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Reservation> allReservations = (List<Reservation>) result;

                // Filtrar solo reservas programadas/confirmadas
                List<Reservation> upcomingTours = new java.util.ArrayList<>();
                for (Reservation reservation : allReservations) {
                    if ("CONFIRMADA".equals(reservation.getStatus()) || "PROGRAMADA".equals(reservation.getStatus())) {
                        upcomingTours.add(reservation);
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
        // Active Tour Card and Continue Button
        MaterialButton btnContinueTour = findViewById(R.id.btn_continue_tour);
        if (btnContinueTour != null) {
            btnContinueTour.setOnClickListener(v -> {
                Intent intent = new Intent(this, LocationTrackingActivity.class);
                intent.putExtra("tour_id", 1);
                intent.putExtra("tour_name", "City Tour Lima - Centro Histórico");
                startActivity(intent);
            });
        }

        cardActiveTour.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationTrackingActivity.class);
            intent.putExtra("tour_id", 1);
            intent.putExtra("tour_name", "City Tour Lima - Centro Histórico");
            startActivity(intent);
        });

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
        
        // QR Scanner
        cardQRScanner.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScannerActivity.class);
            intent.putExtra("tour_name", "City Tour Lima - Centro Histórico");
            startActivity(intent);
        });
        
        // Location Tracking
        cardLocationTracking.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationTrackingActivity.class);
            intent.putExtra("tour_name", "City Tour Lima - Centro Histórico");
            startActivity(intent);
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

        // 2. Cargar reservas para calcular tours completados y ganancias
        firestoreManager.getReservationsByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Reservation> allReservations = (List<Reservation>) result;

                int completedCount = 0;
                double totalEarnings = 0.0;

                for (Reservation reservation : allReservations) {
                    if ("COMPLETADA".equals(reservation.getStatus())) {
                        completedCount++;
                        Double price = reservation.getTotalPrice();
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

    // NUEVO MÉTODO PARA CARGAR RATING:
    private void loadGuideRatingFromFirebase() {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // Intentar cargar desde user_roles primero
        db.collection(FirestoreManager.COLLECTION_USER_ROLES)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Float rating = null;
                        Integer toursCount = 0;

                        // Extraer rating de diferentes estructuras posibles
                        if (doc.contains("rating")) {
                            Object ratingObj = doc.get("rating");
                            if (ratingObj instanceof Number) {
                                rating = ((Number) ratingObj).floatValue();
                            }
                        } else if (doc.contains("guide")) {
                            Object guideObj = doc.get("guide");
                            if (guideObj instanceof java.util.Map) {
                                java.util.Map<String, Object> guideMap = (java.util.Map<String, Object>) guideObj;
                                Object ratingObj = guideMap.get("rating");
                                if (ratingObj instanceof Number) {
                                    rating = ((Number) ratingObj).floatValue();
                                }
                            }
                        }

                        // Actualizar UI con el rating
                        if (rating != null && rating > 0) {
                            tvGuideRating.setText(String.format("Calificación: ⭐ %.1f", rating));
                        } else {
                            tvGuideRating.setText("Calificación: ⭐ 0.0");
                        }
                    } else {
                        // Si no existe en user_roles, intentar desde guides
                        loadRatingFromGuides();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.w("TourGuideMain", "Error cargando rating desde user_roles: " + e.getMessage());
                    loadRatingFromGuides();
                });
    }

    // MÉTODO FALLBACK PARA RATING DESDE GUIDES:
    private void loadRatingFromGuides() {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection(FirestoreManager.COLLECTION_GUIDES)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Object ratingObj = doc.get("rating");
                        if (ratingObj instanceof Number) {
                            float rating = ((Number) ratingObj).floatValue();
                            tvGuideRating.setText(String.format("Calificación: ⭐ %.1f", rating));
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
        
        firestoreManager.getReservationsByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Reservation> allReservations =
                    (List<Reservation>) result;
                
                // Buscar tour EN_PROGRESO
                Reservation activeTour = null;
                for (Reservation reservation : allReservations) {
                    if ("EN_PROGRESO".equals(reservation.getStatus())) {
                        activeTour = reservation;
                        break;
                    }
                }
                
                final Reservation finalActiveTour = activeTour;
                
                if (finalActiveTour != null) {
                    activeTourReservation = finalActiveTour;
                    cardActiveTour.setVisibility(View.VISIBLE);
                    tvActiveTourName.setText(finalActiveTour.getTourName());
                    tvActiveTourProgress.setText("📍 Punto 2 de 4 • Plaza de Armas"); // TODO: Dinámico
                    
                    btnContinueTour.setOnClickListener(v -> {
                        // Ir a LocationTrackingActivity con el tour activo
                        Intent intent = new Intent(TourGuideMainActivity.this, LocationTrackingActivity.class);
                        intent.putExtra("reservation_id", finalActiveTour.getReservationId());
                        intent.putExtra("tour_name", finalActiveTour.getTourName());
                        startActivity(intent);
                    });
                    
                    // Configurar botones de acciones rápidas
                    cardQRScanner.setOnClickListener(v -> {
                        Intent intent = new Intent(TourGuideMainActivity.this, QRScannerActivity.class);
                        intent.putExtra("reservation_id", finalActiveTour.getReservationId());
                        startActivity(intent);
                    });
                    
                    cardLocationTracking.setOnClickListener(v -> {
                        Intent intent = new Intent(TourGuideMainActivity.this, LocationTrackingActivity.class);
                        intent.putExtra("reservation_id", finalActiveTour.getReservationId());
                        intent.putExtra("tour_name", finalActiveTour.getTourName());
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
                // Aceptar oferta directamente desde el dashboard
                if (offer.getOfferId() != null && offer.getTourId() != null) {
                    // 1. Actualizar estado de la oferta
                    firestoreManager.updateOfferStatus(offer.getOfferId(), "ACEPTADA", 
                        new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                // 2. Hacer el tour público y asignar el guía
                                Map<String, Object> tourUpdates = new HashMap<>();
                                tourUpdates.put("isPublic", true);
                                tourUpdates.put("assignedGuideId", currentUserId);
                                tourUpdates.put("isActive", true);
                                
                                firestoreManager.updateTour(offer.getTourId(), tourUpdates, 
                                    new FirestoreManager.FirestoreCallback() {
                                        @Override
                                        public void onSuccess(Object result) {
                                            Toast.makeText(TourGuideMainActivity.this, 
                                                "✅ Oferta aceptada. Tour ahora público para clientes", 
                                                Toast.LENGTH_LONG).show();
                                            loadPendingOffersFromFirebase();
                                        }
                                        
                                        @Override
                                        public void onFailure(Exception e) {
                                            Toast.makeText(TourGuideMainActivity.this, 
                                                "⚠️ Oferta aceptada pero error al publicar tour", 
                                                Toast.LENGTH_SHORT).show();
                                            loadPendingOffersFromFirebase();
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
        private final List<Reservation> tours;

        UpcomingToursAdapterFirebase(List<Reservation> tours, OnTourClickListener listener) {
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
            Reservation tour = tours.get(position);
            
            holder.tvTourName.setText(tour.getTourName() != null ? tour.getTourName() : "Tour");
            holder.tvCompanyName.setText(tour.getCompanyName() != null ? tour.getCompanyName() : "Empresa");
            holder.tvTourDate.setText(tour.getTourDate() != null ? tour.getTourDate() : "Fecha");
            holder.tvTourTime.setText(tour.getTourTime() != null ? tour.getTourTime() : "Hora");
            holder.tvGroupSize.setText((tour.getNumberOfPeople() != null ? tour.getNumberOfPeople() : 0) + " personas");
            holder.tvStatus.setText(tour.getStatus() != null ? tour.getStatus() : "PENDIENTE");

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
}
