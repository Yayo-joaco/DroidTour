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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class ClientMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private RecyclerView rvCompanies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        setupToolbarAndDrawer();
        setupCompaniesList();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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

    private void setupCompaniesList() {
        rvCompanies = findViewById(R.id.rv_companies);
        rvCompanies.setLayoutManager(new LinearLayoutManager(this));
        rvCompanies.setAdapter(new ExampleCompaniesAdapter(v -> {
            // Al hacer click en una empresa, abrir reserva con un tour ejemplo
            Intent i = new Intent(this, TourBookingActivity.class);
            i.putExtra("tourName", "Tour Machu Picchu Completo");
            i.putExtra("companyName", "Tours Cusco Adventures");
            i.putExtra("price", 250.0);
            startActivity(i);
        }));

        // Configurar botones de acceso rápido
        findViewById(R.id.btn_my_reservations).setOnClickListener(v -> {
            startActivity(new Intent(this, ClientReservationsActivity.class));
        });

        findViewById(R.id.btn_tour_history).setOnClickListener(v -> {
            startActivity(new Intent(this, ClientHistoryActivity.class));
        });

        findViewById(R.id.btn_chat).setOnClickListener(v -> {
            startActivity(new Intent(this, ClientChatActivity.class));
        });

        findViewById(R.id.btn_real_time_tracking).setOnClickListener(v -> {
            startActivity(new Intent(this, RealTimeTrackingActivity.class));
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home || id == R.id.nav_available_companies) {
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_my_reservations) {
            startActivity(new Intent(this, ClientReservationsActivity.class));
        } else if (id == R.id.nav_tour_history) {
            startActivity(new Intent(this, ClientHistoryActivity.class));
        } else if (id == R.id.nav_profile) {
            Toast.makeText(this, "Perfil próximamente", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
        return true;
    }
}

// Adaptador de empresas con ejemplos
class ExampleCompaniesAdapter extends RecyclerView.Adapter<ExampleCompaniesAdapter.ViewHolder> {
    interface OnCompanyClick { void onClick(View v); }
    private final OnCompanyClick onCompanyClick;
    ExampleCompaniesAdapter(OnCompanyClick listener) { this.onCompanyClick = listener; }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        android.view.View item = holder.itemView;
        TextView name = item.findViewById(R.id.tv_company_name);
        TextView location = item.findViewById(R.id.tv_company_location);
        TextView toursCount = item.findViewById(R.id.tv_tours_count);
        TextView priceRange = item.findViewById(R.id.tv_price_range);
        android.view.View button = item.findViewById(R.id.btn_view_tours);

        switch (position % 3) {
            case 0:
                name.setText("Tours Cusco Adventures");
                location.setText("Cusco, Perú");
                toursCount.setText("12 tours disponibles");
                priceRange.setText("Desde S/. 120");
                break;
            case 1:
                name.setText("Lima City Travel");
                location.setText("Lima, Perú");
                toursCount.setText("8 tours disponibles");
                priceRange.setText("Desde S/. 90");
                break;
            default:
                name.setText("Arequipa Andes Tours");
                location.setText("Arequipa, Perú");
                toursCount.setText("5 tours disponibles");
                priceRange.setText("Desde S/. 110");
        }

        item.setOnClickListener(v -> onCompanyClick.onClick(v));
        button.setOnClickListener(v -> onCompanyClick.onClick(v));
    }

    @Override
    public int getItemCount() { return 9; }

    static class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(android.view.View v) { super(v);} }
}
