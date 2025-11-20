package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.database.DatabaseHelper;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.managers.PrefsManager;
import com.example.droidtour.models.Notification;
import com.example.droidtour.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import java.util.List;

public class TourGuideMainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView rvPendingOffers, rvUpcomingTours;
    private MaterialCardView cardActiveTour, cardQRScanner, cardLocationTracking;
    private TextView tvViewAllOffers, tvViewAllTours;
    
    // Dashboard Stats TextViews
    private TextView tvGuideStatus, tvGuideRating, tvMonthlyEarnings, tvCompletedTours;
    private TextView tvActiveTourName, tvActiveTourProgress;
    private MaterialButton btnContinueTour;
    
    // Storage y Notificaciones
    private DatabaseHelper dbHelper;
    private PrefsManager prefsManager;
    private NotificationHelper notificationHelper;
    
    // Firebase
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private String currentUserId;
    private com.example.droidtour.models.Reservation activeTourReservation;
    
    // Toolbar menu elements
    private FrameLayout notificationActionLayout, avatarActionLayout;
    private TextView tvNotificationBadge;
    private ImageView ivAvatarAction;
    private int notificationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_guide_main);
        
        // Inicializar helpers
        dbHelper = new DatabaseHelper(this);
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
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // ✅ RECARGAR DASHBOARD CADA VEZ QUE REGRESAS
        setupRecyclerViews();
        // Recargar contador de notificaciones
        loadNotificationsCount();
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
                    tvUserNameHeader.setText(userName);
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
                // Abrir actividad de inicialización de datos
                startActivity(new Intent(this, com.example.droidtour.firebase.InitializeTestDataActivity.class));
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
            return;
        }
        
        firestoreManager.getOffersByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<com.example.droidtour.models.TourOffer> allOffers = 
                    (List<com.example.droidtour.models.TourOffer>) result;
                
                // Filtrar solo ofertas pendientes
                List<com.example.droidtour.models.TourOffer> pendingOffers = new java.util.ArrayList<>();
                for (com.example.droidtour.models.TourOffer offer : allOffers) {
                    if ("PENDIENTE".equals(offer.getStatus())) {
                        pendingOffers.add(offer);
                    }
                }
                
                // Actualizar adapter
                rvPendingOffers.setAdapter(new PendingOffersAdapterFirebase(pendingOffers, TourGuideMainActivity.this::onOfferClick));
                android.util.Log.d("TourGuideMain", "✅ Ofertas pendientes cargadas: " + pendingOffers.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando ofertas", e);
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
            return;
        }
        
        firestoreManager.getReservationsByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<com.example.droidtour.models.Reservation> allReservations = 
                    (List<com.example.droidtour.models.Reservation>) result;
                
                // Filtrar solo reservas programadas/confirmadas
                List<com.example.droidtour.models.Reservation> upcomingTours = new java.util.ArrayList<>();
                for (com.example.droidtour.models.Reservation reservation : allReservations) {
                    if ("CONFIRMADA".equals(reservation.getStatus()) || "PROGRAMADA".equals(reservation.getStatus())) {
                        upcomingTours.add(reservation);
                    }
                }
                
                // Actualizar adapter
                rvUpcomingTours.setAdapter(new UpcomingToursAdapterFirebase(upcomingTours, TourGuideMainActivity.this::onTourClick));
                android.util.Log.d("TourGuideMain", "✅ Tours programados cargados: " + upcomingTours.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando tours", e);
                Toast.makeText(TourGuideMainActivity.this, 
                    "Error al cargar tours programados", Toast.LENGTH_SHORT).show();
            }
        });
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

    // Adapter for Pending Offers (Horizontal)
    private class PendingOffersAdapter extends RecyclerView.Adapter<PendingOffersAdapter.ViewHolder> {
        
        private final OnOfferClickListener listener;
        private final List<DatabaseHelper.Offer> offers;

        PendingOffersAdapter(List<DatabaseHelper.Offer> offers, OnOfferClickListener listener) {
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
            // ✅ USAR DATOS DE LA BD
            DatabaseHelper.Offer offer = offers.get(position);
            
            holder.tvTourName.setText(offer.getTourName());
            holder.tvCompanyName.setText(offer.getCompany());
            holder.tvTourDate.setText(offer.getDate());
            holder.tvTourTime.setText(offer.getTime());
            holder.tvPaymentAmount.setText(String.format("S/. %.0f", offer.getPayment()));
            holder.tvParticipants.setText(offer.getParticipants() + " personas");

            holder.btnAccept.setOnClickListener(v -> {
                // ✅ ACEPTAR OFERTA Y GUARDAR EN BD
                acceptOffer(offer.getId(), offer.getTourName(), offer.getCompany(), 
                    offer.getDate(), offer.getTime(), offer.getPayment(), offer.getParticipants());
                
                // Recargar dashboard
                setupRecyclerViews();
            });

            holder.btnReject.setOnClickListener(v -> {
                // Handle reject offer
                android.widget.Toast.makeText(TourGuideMainActivity.this, 
                    "Oferta rechazada", 
                    android.widget.Toast.LENGTH_SHORT).show();
            });

            holder.itemView.setOnClickListener(v -> listener.onClick(position));
        }

        @Override
        public int getItemCount() {
            // ✅ RETORNAR NÚMERO REAL DE OFERTAS
            return offers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTourName, tvCompanyName, tvTourDate, tvTourTime, 
                     tvPaymentAmount, tvParticipants;
            com.google.android.material.button.MaterialButton btnAccept, btnReject;

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

    // Adapter for Upcoming Tours (Vertical)
    private class UpcomingToursAdapter extends RecyclerView.Adapter<UpcomingToursAdapter.ViewHolder> {
        
        private final OnOfferClickListener listener;
        private final List<DatabaseHelper.Tour> tours;

        UpcomingToursAdapter(List<DatabaseHelper.Tour> tours, OnOfferClickListener listener) {
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
            // ✅ USAR DATOS DE LA BD
            DatabaseHelper.Tour tour = tours.get(position);
            
            holder.tvTourName.setText(tour.getName());
            holder.tvCompanyName.setText(tour.getCompany());
            holder.tvTourDate.setText(tour.getDate());
            holder.tvTourTime.setText(tour.getTime());
            holder.tvPaymentAmount.setText(String.format("S/. %.0f", tour.getPayment()));
            holder.tvParticipants.setText(tour.getParticipants() + " personas");

            holder.itemView.setOnClickListener(v -> listener.onClick(position));
    }
    
    @Override
        public int getItemCount() {
            // ✅ RETORNAR NÚMERO REAL DE TOURS
            return tours.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTourName, tvCompanyName, tvTourDate, tvTourTime, 
                     tvPaymentAmount, tvParticipants, tvStatus;
            View viewStatusIndicator;

            ViewHolder(View itemView) {
                super(itemView);
                tvTourName = itemView.findViewById(R.id.tv_tour_name);
                tvCompanyName = itemView.findViewById(R.id.tv_company_name);
                tvTourDate = itemView.findViewById(R.id.tv_tour_date);
                tvTourTime = itemView.findViewById(R.id.tv_tour_time);
                tvPaymentAmount = itemView.findViewById(R.id.tv_payment_amount);
                tvParticipants = itemView.findViewById(R.id.tv_participants);
                tvStatus = itemView.findViewById(R.id.tv_status);
                viewStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
            }
        }
    }

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
        
        // Cargar datos reales del usuario desde Firebase
        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                
                // Actualizar nombre en el header del drawer
                NavigationView navigationView = findViewById(R.id.nav_view);
                if (navigationView != null) {
                    View headerView = navigationView.getHeaderView(0);
                    if (headerView != null) {
                        TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
                        if (tvUserNameHeader != null) {
                            String fullName = user.getFullName() != null ? user.getFullName() : 
                                            (user.getFirstName() + " " + user.getLastName());
                            tvUserNameHeader.setText(fullName);
                        }
                    }
                }
                
                android.util.Log.d("TourGuideMain", "✅ Nombre del drawer actualizado desde Firebase");
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando usuario para drawer", e);
                // Fallback a PreferencesManager
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
        });
    }
    
    private void loadSampleDataIfNeeded() {
        // Cargar datos de ejemplo solo si la BD está vacía
        List<DatabaseHelper.Offer> existingOffers = dbHelper.getAllOffers();
        
        if (existingOffers.isEmpty()) {
            // Agregar ofertas de ejemplo
            dbHelper.addOffer("City Tour Lima Centro", "Perú Grand Travel", 
                "25 Oct", "09:00 AM", 180.0, "PENDIENTE", 15);
            dbHelper.addOffer("Tour Pachacamac", "Lima Explorer", 
                "26 Oct", "08:00 AM", 200.0, "PENDIENTE", 12);
            
            Toast.makeText(this, "Datos de ejemplo cargados", Toast.LENGTH_SHORT).show();
            
            // Enviar notificación de nueva oferta
            notificationHelper.sendNewOfferNotification(
                "City Tour Lima Centro", 
                "Perú Grand Travel", 
                180.0
            );
        }
        
        // Cargar tours aceptados de ejemplo
        List<DatabaseHelper.Tour> existingTours = dbHelper.getAllTours();
        if (existingTours.isEmpty()) {
            dbHelper.addTour("Tour Islas Palomino", "Oceanic Adventures", 
                "27 Oct", "07:00 AM", "PROGRAMADO", 250.0, 20);
        }
    }
    
    // Método para aceptar una oferta
    public void acceptOffer(int offerId, String tourName, String company, String date, 
                           String time, double payment, int participants) {
        // Actualizar oferta a aceptada (se mueve a "Mis Tours")
        int rowsUpdated = dbHelper.updateOfferStatus(offerId, "ACEPTADA");
        
        // Agregar tour a "Mis Tours"
        long tourId = dbHelper.addTour(tourName, company, date, time, "PROGRAMADO", payment, participants);
        
        // Enviar notificación de confirmación
        notificationHelper.sendTourReminderNotification(tourName, time);
        
        // Mostrar mensaje con detalles
        Toast.makeText(this, "✅ Oferta aceptada: " + tourName + 
            "\nOferta actualizada: " + rowsUpdated + 
            "\nTour creado con ID: " + tourId, 
            Toast.LENGTH_LONG).show();
        
        // ✅ RECARGAR DASHBOARD INMEDIATAMENTE
        setupRecyclerViews();
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
    
    // ==================== ESTADÍSTICAS DEL GUÍA ====================
    
    /**
     * Cargar estadísticas del guía desde Firebase
     */
    private void loadGuideStats() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            android.util.Log.e("TourGuideMain", "❌ Error: currentUserId es null");
            return;
        }
        
        // 1. Cargar rating del guía
        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                Float rating = user.getGuideRating();
                
                if (rating != null && rating > 0) {
                    tvGuideStatus.setText("Estado: APROBADO");
                    // El número de tours se cargará desde las reservas
                } else {
                    tvGuideStatus.setText("Estado: APROBADO");
                    tvGuideRating.setText("Calificación: ⭐ 0.0 (0 tours)");
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando usuario", e);
            }
        });
        
        // 2. Cargar reservas para calcular tours completados y ganancias
        firestoreManager.getReservationsByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<com.example.droidtour.models.Reservation> allReservations = 
                    (List<com.example.droidtour.models.Reservation>) result;
                
                final int[] completedCount = {0};
                double totalEarnings = 0.0;
                
                for (com.example.droidtour.models.Reservation reservation : allReservations) {
                    if ("COMPLETADA".equals(reservation.getStatus())) {
                        completedCount[0]++;
                        Double price = reservation.getTotalPrice();
                        if (price != null) {
                            totalEarnings += price;
                        }
                    }
                }
                
                // Actualizar UI
                tvCompletedTours.setText(completedCount[0] + " Tours");
                tvMonthlyEarnings.setText(String.format("S/. %.0f", totalEarnings));
                
                // Actualizar rating con el número real de tours
                firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                        Float rating = user.getGuideRating();
                        if (rating != null && rating > 0) {
                            tvGuideRating.setText(String.format("Calificación: ⭐ %.1f (%d tours)", rating, completedCount[0]));
                        } else {
                            tvGuideRating.setText(String.format("Calificación: ⭐ 0.0 (%d tours)", completedCount[0]));
                        }
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        tvGuideRating.setText(String.format("Calificación: ⭐ 0.0 (%d tours)", completedCount[0]));
                    }
                });
                
                android.util.Log.d("TourGuideMain", "✅ Stats: " + completedCount[0] + " tours, S/. " + totalEarnings);
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourGuideMain", "❌ Error cargando reservas", e);
                tvCompletedTours.setText("0 Tours");
                tvMonthlyEarnings.setText("S/. 0");
            }
        });
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
                List<com.example.droidtour.models.Reservation> allReservations = 
                    (List<com.example.droidtour.models.Reservation>) result;
                
                // Buscar tour EN_PROGRESO
                com.example.droidtour.models.Reservation activeTour = null;
                for (com.example.droidtour.models.Reservation reservation : allReservations) {
                    if ("EN_PROGRESO".equals(reservation.getStatus())) {
                        activeTour = reservation;
                        break;
                    }
                }
                
                final com.example.droidtour.models.Reservation finalActiveTour = activeTour;
                
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
        private final List<com.example.droidtour.models.TourOffer> offers;

        PendingOffersAdapterFirebase(List<com.example.droidtour.models.TourOffer> offers, OnOfferClickListener listener) {
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
            com.example.droidtour.models.TourOffer offer = offers.get(position);
            
            holder.tvTourName.setText(offer.getTourName() != null ? offer.getTourName() : "Tour");
            holder.tvCompanyName.setText(offer.getCompanyName() != null ? offer.getCompanyName() : "Empresa");
            holder.tvTourDate.setText(offer.getTourDate() != null ? offer.getTourDate() : "Fecha");
            holder.tvTourTime.setText(offer.getTourTime() != null ? offer.getTourTime() : "Hora");
            holder.tvPaymentAmount.setText(String.format("S/. %.0f", offer.getPaymentAmount() != null ? offer.getPaymentAmount() : 0.0));
            holder.tvParticipants.setText((offer.getNumberOfParticipants() != null ? offer.getNumberOfParticipants() : 0) + " personas");

            holder.btnAccept.setOnClickListener(v -> {
                // Aceptar oferta directamente desde el dashboard
                if (offer.getOfferId() != null) {
                    firestoreManager.updateOfferStatus(offer.getOfferId(), "ACEPTADA", 
                        new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                Toast.makeText(TourGuideMainActivity.this, 
                                    "✅ Oferta aceptada: " + offer.getTourName(), 
                                    Toast.LENGTH_LONG).show();
                                // Recargar ofertas
                                loadPendingOffersFromFirebase();
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
        private final List<com.example.droidtour.models.Reservation> tours;

        UpcomingToursAdapterFirebase(List<com.example.droidtour.models.Reservation> tours, OnTourClickListener listener) {
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
            com.example.droidtour.models.Reservation tour = tours.get(position);
            
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
