package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

public class TourGuideMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    
    // Cards del dashboard
    private MaterialCardView cardTourOffers;
    private MaterialCardView cardMyTours;
    private MaterialCardView cardQrScanner;
    private MaterialCardView cardLocationTracking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_guide_main);
        
        setupToolbar();
        setupNavigationDrawer();
        initializeViews();
        setupCardClickListeners();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
    
    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        
        drawerToggle = new ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        
        navigationView.setNavigationItemSelectedListener(this);
    }
    
    private void initializeViews() {
        cardTourOffers = findViewById(R.id.card_tour_offers);
        cardMyTours = findViewById(R.id.card_my_tours);
        cardQrScanner = findViewById(R.id.card_qr_scanner);
        cardLocationTracking = findViewById(R.id.card_location_tracking);
    }
    
    private void setupCardClickListeners() {
        // Ofertas de Tours
        cardTourOffers.setOnClickListener(v -> {
            Intent intent = new Intent(this, TourOffersActivity.class);
            startActivity(intent);
        });
        
        // Mis Tours
        cardMyTours.setOnClickListener(v -> {
            Intent intent = new Intent(this, GuideActiveToursActivity.class);
            startActivity(intent);
        });
        
        // Escáner QR
        cardQrScanner.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScannerActivity.class);
            startActivity(intent);
        });
        
        // Registro de Ubicación
        cardLocationTracking.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationTrackingActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_home) {
            // Ya estamos en home, cerrar drawer
            drawerLayout.closeDrawers();
            return true;
            
        } else if (itemId == R.id.nav_available_tours) {
            Intent intent = new Intent(this, TourOffersActivity.class);
            startActivity(intent);
            
        } else if (itemId == R.id.nav_my_tours) {
            Intent intent = new Intent(this, GuideActiveToursActivity.class);
            startActivity(intent);
            
        } else if (itemId == R.id.nav_tour_location) {
            Intent intent = new Intent(this, LocationTrackingActivity.class);
            startActivity(intent);
            
        } else if (itemId == R.id.nav_profile) {
            Intent intent = new Intent(this, GuideRegistrationActivity.class);
            intent.putExtra("mode", "edit_profile");
            startActivity(intent);
            
        } else if (itemId == R.id.nav_logout) {
            // Implementar logout
            Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        
        drawerLayout.closeDrawers();
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }
}
