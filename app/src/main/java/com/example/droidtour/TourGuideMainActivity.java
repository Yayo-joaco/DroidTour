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
import com.example.droidtour.utils.NotificationHelper;
import com.example.droidtour.utils.PreferencesManager;
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
    
    // Storage y Notificaciones
    private DatabaseHelper dbHelper;
    private PreferencesManager prefsManager;
    private NotificationHelper notificationHelper;
    
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
        prefsManager = new PreferencesManager(this);
        notificationHelper = new NotificationHelper(this);
        
        initializeViews();
        // Corregir datos del usuario PRIMERO (sin actualizar vistas aún)
        correctUserData();
        setupToolbar();
        setupNavigationDrawer();
        setupRecyclerViews();
        setupClickListeners();
        
        // Cargar datos del usuario y actualizar vistas
        loadUserData();
        
        // Simular carga de datos de ejemplo (solo primera vez)
        loadSampleDataIfNeeded();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // ✅ RECARGAR DASHBOARD CADA VEZ QUE REGRESAS
        setupRecyclerViews();
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
            Toast.makeText(this, "Notificaciones - Por implementar", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_profile) {
            Toast.makeText(this, "Perfil - Por implementar", Toast.LENGTH_SHORT).show();
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
                notificationActionLayout.setOnClickListener(v ->
                        Toast.makeText(this, "Notificaciones - Por implementar", Toast.LENGTH_SHORT).show());
            }
        }

        MenuItem avatarItem = menu.findItem(R.id.action_profile);
        if (avatarItem != null) {
            avatarActionLayout = (FrameLayout) avatarItem.getActionView();
            if (avatarActionLayout != null) {
                ivAvatarAction = avatarActionLayout.findViewById(R.id.iv_avatar_action);
                avatarActionLayout.setOnClickListener(v ->
                        Toast.makeText(this, "Perfil - Por implementar", Toast.LENGTH_SHORT).show());
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
                String userType = prefsManager.getUserType();
                String userName = prefsManager.getUserName();
                
                // Asegurar que el guía tenga el nombre correcto
                if (userType != null && userType.equals("GUIDE")) {
                    if (!userName.equals("Carlos Mendoza") && (userName.equals("Gabrielle Ivonne") || 
                        userName.equals("María López") || userName.equals("Ana García Rodríguez"))) {
                        prefsManager.saveUserData(
                            "GUIDE001", 
                            "Carlos Mendoza", 
                            "guia@tours.com", 
                            "987654321", 
                            "GUIDE"
                        );
                        prefsManager.setGuideApproved(true);
                        prefsManager.setGuideRating(4.8f);
                        userName = "Carlos Mendoza";
                    }
                }
                
                if (userName != null && !userName.isEmpty()) {
                    tvUserNameHeader.setText(userName);
                } else {
                    tvUserNameHeader.setText("Carlos Mendoza");
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
            } else if (id == R.id.nav_logout) {
                // Handle logout
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupRecyclerViews() {
        // ✅ CARGAR OFERTAS PENDIENTES DE LA BD
        List<DatabaseHelper.Offer> pendingOffers = new java.util.ArrayList<>();
        for (DatabaseHelper.Offer offer : dbHelper.getAllOffers()) {
            if (offer.getStatus().equals("PENDIENTE")) {
                pendingOffers.add(offer);
            }
        }
        
        // ✅ CARGAR TOURS PROGRAMADOS DE LA BD
        List<DatabaseHelper.Tour> upcomingTours = new java.util.ArrayList<>();
        for (DatabaseHelper.Tour tour : dbHelper.getAllTours()) {
            if (tour.getStatus().equals("PROGRAMADO")) {
                upcomingTours.add(tour);
            }
        }
        
        // Pending Offers RecyclerView (Horizontal)
        LinearLayoutManager offersLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvPendingOffers.setLayoutManager(offersLayoutManager);
        rvPendingOffers.setAdapter(new PendingOffersAdapter(pendingOffers, this::onOfferClick));

        // Upcoming Tours RecyclerView (Vertical)
        LinearLayoutManager toursLayoutManager = new LinearLayoutManager(this);
        rvUpcomingTours.setLayoutManager(toursLayoutManager);
        rvUpcomingTours.setAdapter(new UpcomingToursAdapter(upcomingTours, this::onTourClick));
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
    
    // ==================== STORAGE LOCAL ====================
    
    private void correctUserData() {
        // Verificar y corregir datos del guía (sin actualizar vistas)
        String userType = prefsManager.getUserType();
        String userName = prefsManager.getUserName();
        
        // Si no está logueado o el tipo no es GUIDE, inicializar como guía
        if (!prefsManager.isLoggedIn() || (userType != null && !userType.equals("GUIDE"))) {
            prefsManager.saveUserData(
                "GUIDE001", 
                "Carlos Mendoza", 
                "guia@tours.com", 
                "987654321", 
                "GUIDE"
            );
            prefsManager.setGuideApproved(true);
            prefsManager.setGuideRating(4.8f);
        } else {
            // Si está logueado pero el nombre no es correcto, corregirlo
            if (!userName.equals("Carlos Mendoza") && (userName.equals("Gabrielle Ivonne") || 
                userName.equals("María López") || userName.equals("Ana García Rodríguez"))) {
                prefsManager.saveUserData(
                    "GUIDE001", 
                    "Carlos Mendoza", 
                    "guia@tours.com", 
                    "987654321", 
                    "GUIDE"
                );
                prefsManager.setGuideApproved(true);
                prefsManager.setGuideRating(4.8f);
            }
        }
    }
    
    private void loadUserData() {
        // Actualizar nombre en el header del drawer
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
                if (tvUserNameHeader != null) {
                    String userName = prefsManager.getUserName();
                    if (userName != null && !userName.isEmpty()) {
                        tvUserNameHeader.setText(userName);
                    } else {
                        tvUserNameHeader.setText("Carlos Mendoza");
                    }
                }
            }
        }
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
    
    private void testNotifications() {
        // Método de prueba para enviar notificaciones de ejemplo
        notificationHelper.sendNewOfferNotification(
            "Tour Paracas Full Day", 
            "Coastal Adventures", 
            350.0
        );
    }
}
