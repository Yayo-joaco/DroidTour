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
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.managers.FileManager;

public class SuperadminMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private MaterialCardView cardUserManagement, cardReports, cardLogs;

    // Storage
    private PreferencesManager prefsManager;
    private FileManager fileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_superadmin_main);

        // Inicializar storage
        prefsManager = new PreferencesManager(this);
        fileManager = new FileManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupDrawer();
        setupClickListeners();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        cardUserManagement = findViewById(R.id.card_user_management);
        cardReports = findViewById(R.id.card_reports);
        cardLogs = findViewById(R.id.card_logs);
    }

    private void setupDrawer() {
        setSupportActionBar(toolbar);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupClickListeners() {
        cardUserManagement.setOnClickListener(v -> {
            Toast.makeText(this, "Abriendo Gestión de Usuarios", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SuperadminUsersActivity.class));
        });

        cardReports.setOnClickListener(v -> {
            Toast.makeText(this, "Abriendo Reportes de Tours", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SuperadminReportsActivity.class));
        });

        cardLogs.setOnClickListener(v -> {
            Toast.makeText(this, "Abriendo Logs del Sistema", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SuperadminLogsActivity.class));
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            // Ya estamos en home
            Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_user_management) {
            startActivity(new Intent(this, SuperadminUsersActivity.class));
        } else if (id == R.id.nav_reports) {
            startActivity(new Intent(this, SuperadminReportsActivity.class));
        } else if (id == R.id.nav_logs) {
            startActivity(new Intent(this, SuperadminLogsActivity.class));
        } else if (id == R.id.nav_profile) {
            Toast.makeText(this, "Perfil - En desarrollo", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            performLogout();
        }
        
        drawerLayout.closeDrawers();
        return true;
    }

    /**
     * Realizar logout completo
     */
    private void performLogout() {
        // 1. Limpiar SharedPreferences
        prefsManager.logout();

        // 2. Limpiar archivos de datos de usuario
        if (fileManager != null) {
            fileManager.limpiarDatosUsuario();
        }

        // 3. Mostrar mensaje de confirmación
        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

        // 4. Redirigir al LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}
