package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
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
    
    // Storage y Notificaciones
    private DatabaseHelper dbHelper;
    private PreferencesManager prefsManager;
    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        // Inicializar helpers
        dbHelper = new DatabaseHelper(this);
        prefsManager = new PreferencesManager(this);
        notificationHelper = new NotificationHelper(this);

        initializeViews();
        setupToolbarAndDrawer();
        setupDashboardData();
        setupClickListeners();
        setupRecyclerViews();
        
        // Cargar datos del usuario
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
        // Set welcome message (could be dynamic based on user)
        tvWelcomeMessage.setText("¡Hola, Ana!");
        
        // Set active reservations count
        tvActiveReservations.setText("2");
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
        rvFeaturedTours.setAdapter(new FeaturedToursAdapter(this::onFeaturedTourClick));

        // Popular Companies (vertical)
        rvPopularCompanies.setLayoutManager(new LinearLayoutManager(this));
        rvPopularCompanies.setAdapter(new PopularCompaniesAdapter(this::onCompanyClick));
    }

    private void onFeaturedTourClick(int position) {
        // Datos consistentes con catálogo
        String name, company, imageUrl; double price;
        switch (position % 4) {
            case 0:
                name = "City Tour Lima Centro Histórico"; company = "Lima Adventure Tours"; price = 85.0;
                imageUrl = "https://www.dicasdeviagem.com/wp-content/uploads/2020/03/lima-costa-mar-2048x1364.jpg"; break;
            case 1:
                name = "Machu Picchu Full Day"; company = "Cusco Explorer"; price = 180.0;
                imageUrl = "https://res.klook.com/image/upload/c_fill,w_627,h_470/q_80/w_80,x_15,y_15,g_south_west,l_Klook_water_br_trans_yhcmh3/activities/jdnneadpdsxcsnghocbu.jpg"; break;
            case 2:
                name = "Islas Ballestas y Paracas"; company = "Paracas Tours"; price = 65.0;
                imageUrl = "https://image.jimcdn.com/app/cms/image/transf/none/path/s336fd9bc7dca3ebc/image/ida0ff171f4a6d885/version/1391479285/image.jpg"; break;
            default:
                name = "Cañón del Colca 2D/1N"; company = "Arequipa Adventures"; price = 120.0;
                imageUrl = "https://thriveandwander.com/wp-content/uploads/2023/12/barranco-lima-768x514.jpg"; break;
        }

        Intent intent = new Intent(this, TourDetailActivity.class);
        intent.putExtra("tour_id", position);
        intent.putExtra("tour_name", name);
        intent.putExtra("company_name", company);
        intent.putExtra("price", price);
        intent.putExtra("image_url", imageUrl);
        startActivity(intent);
    }

    private void onCompanyClick(int position) {
        // Navigate to company's tours catalog
        Intent intent = new Intent(this, ToursCatalogActivity.class);
        intent.putExtra("company_id", position);
        intent.putExtra("company_name", "Lima Adventure Tours");
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
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
        } else if (id == R.id.nav_logout) {
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
        
        drawerLayout.closeDrawers();
        return true;
    }
    
    // ==================== STORAGE LOCAL ====================
    
    private void loadUserData() {
        // Simular login de cliente (en producción vendría del servidor)
        if (!prefsManager.isLoggedIn()) {
            prefsManager.saveUserData(
                "CLIENT001", 
                "María López", 
                "maria.lopez@example.com", 
                "912345678", 
                "CLIENT"
            );
        }
        
        // Actualizar mensaje de bienvenida
        String userName = prefsManager.getUserName();
        tvWelcomeMessage.setText("¡Hola, " + userName + "!");
        Toast.makeText(this, "Bienvenida " + userName, Toast.LENGTH_SHORT).show();
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
}

// Adaptador para tours destacados (horizontal)
class FeaturedToursAdapter extends RecyclerView.Adapter<FeaturedToursAdapter.ViewHolder> {
    interface OnTourClick { void onClick(int position); }
    private final OnTourClick onTourClick;
    
    FeaturedToursAdapter(OnTourClick listener) { 
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
        android.widget.ImageView tourImage = holder.itemView.findViewById(R.id.iv_featured_image);
        TextView tourName = holder.itemView.findViewById(R.id.tv_tour_name);
        TextView companyName = holder.itemView.findViewById(R.id.tv_company_name);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView price = holder.itemView.findViewById(R.id.tv_price);

        String imageUrl;
        switch (position % 4) {
            case 0:
                tourName.setText("City Tour Lima Centro Histórico");
                companyName.setText("Lima Adventure Tours");
                rating.setText("⭐ 4.8");
                price.setText("S/. 85");
                imageUrl = "https://www.dicasdeviagem.com/wp-content/uploads/2020/03/lima-costa-mar-2048x1364.jpg";
                break;
            case 1:
                tourName.setText("Machu Picchu Full Day");
                companyName.setText("Cusco Explorer");
                rating.setText("⭐ 4.9");
                price.setText("S/. 180");
                imageUrl = "https://res.klook.com/image/upload/c_fill,w_627,h_470/q_80/w_80,x_15,y_15,g_south_west,l_Klook_water_br_trans_yhcmh3/activities/jdnneadpdsxcsnghocbu.jpg";
                break;
            case 2:
                tourName.setText("Islas Ballestas y Paracas");
                companyName.setText("Paracas Tours");
                rating.setText("⭐ 4.7");
                price.setText("S/. 65");
                imageUrl = "https://image.jimcdn.com/app/cms/image/transf/none/path/s336fd9bc7dca3ebc/image/ida0ff171f4a6d885/version/1391479285/image.jpg";
                break;
            default:
                tourName.setText("Cañón del Colca 2D/1N");
                companyName.setText("Arequipa Adventures");
                rating.setText("⭐ 4.6");
                price.setText("S/. 120");
                imageUrl = "https://thriveandwander.com/wp-content/uploads/2023/12/barranco-lima-768x514.jpg";
        }

        if (tourImage != null) {
            Glide.with(tourImage.getContext())
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(tourImage);
        }

        holder.itemView.setOnClickListener(v -> onTourClick.onClick(position));
    }

    @Override
    public int getItemCount() { return 4; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

// Adaptador para empresas populares (vertical)
class PopularCompaniesAdapter extends RecyclerView.Adapter<PopularCompaniesAdapter.ViewHolder> {
    interface OnCompanyClick { void onClick(int position); }
    private final OnCompanyClick onCompanyClick;
    
    PopularCompaniesAdapter(OnCompanyClick listener) { 
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
        TextView companyName = holder.itemView.findViewById(R.id.tv_company_name);
        TextView location = holder.itemView.findViewById(R.id.tv_company_location);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView toursCount = holder.itemView.findViewById(R.id.tv_tours_count);
        TextView reviewsCount = holder.itemView.findViewById(R.id.tv_reviews_count);
        android.view.View btnViewTours = holder.itemView.findViewById(R.id.btn_view_tours);

        switch (position % 3) {
            case 0:
                companyName.setText("Lima Adventure Tours");
                location.setText("📍 Lima, Perú");
                rating.setText("⭐ 4.8");
                toursCount.setText("• 12 tours");
                reviewsCount.setText("• 245 reseñas");
                break;
            case 1:
                companyName.setText("Cusco Explorer");
                location.setText("📍 Cusco, Perú");
                rating.setText("⭐ 4.9");
                toursCount.setText("• 8 tours");
                reviewsCount.setText("• 189 reseñas");
                break;
            default:
                companyName.setText("Arequipa Adventures");
                location.setText("📍 Arequipa, Perú");
                rating.setText("⭐ 4.7");
                toursCount.setText("• 6 tours");
                reviewsCount.setText("• 156 reseñas");
        }

        holder.itemView.setOnClickListener(v -> onCompanyClick.onClick(position));
        btnViewTours.setOnClickListener(v -> onCompanyClick.onClick(position));
    }

    @Override
    public int getItemCount() { return 3; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}
