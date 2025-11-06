package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

public class SuperadminMyAccount extends AppCompatActivity {
    
    private PreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myaccount);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar PreferencesManager
        prefsManager = new PreferencesManager(this);

        // Toolbar: permitir botón de retroceso y mostrar título de la app
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Mostrar título (tomado de strings.xml) en la ActionBar
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.mi_cuenta));
        }
        // Asegurar que el título también se establezca directamente en la toolbar (para temas donde ActionBar no lo muestre)
        toolbar.setTitle(getString(R.string.mi_cuenta));
        // Asegurar color de título (si el tema no lo aplica)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Cargar y mostrar datos del usuario
        loadUserData();

        // Encontrar las tarjetas y asignar listeners para redirecciones
        CardView cardProfile = findViewById(R.id.card_my_profile);
        CardView cardPayment = findViewById(R.id.card_payment_methods);
        CardView cardSettings = findViewById(R.id.card_settings);
        CardView cardLogout = findViewById(R.id.card_logout);

        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                Intent i = new Intent(SuperadminMyAccount.this, SuperadminProfileActivity.class);
                startActivity(i);
            });
        }

        // Los superadministradores no tienen métodos de pago, así que lo ocultamos
        if (cardPayment != null) {
            cardPayment.setVisibility(android.view.View.GONE);
        }

        // Los superadministradores no tienen configuración separada, así que ocultamos esta opción también
        if (cardSettings != null) {
            cardSettings.setVisibility(android.view.View.GONE);
        }
        
        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                // Cerrar sesión
                prefsManager.logout();
                Intent i = new Intent(SuperadminMyAccount.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void loadUserData() {
        // Verificar y corregir datos del superadministrador
        String userType = prefsManager.getUserType();
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();
        
        // Si no está logueado o el tipo no es SUPERADMIN, inicializar como superadministrador
        if (!prefsManager.isLoggedIn() || (userType != null && !userType.equals("SUPERADMIN") && !userType.equals("ADMIN"))) {
            prefsManager.saveUserData(
                "SUPERADMIN001", 
                "Gabrielle Ivonne", 
                "superadmin@droidtour.com", 
                "999888777", 
                "SUPERADMIN"
            );
            userName = "Gabrielle Ivonne";
            userEmail = "superadmin@droidtour.com";
        } else {
            // Si está logueado pero el nombre no es correcto, corregirlo
            if (!userName.equals("Gabrielle Ivonne") && (userName.equals("Carlos Mendoza") || 
                userName.equals("María López") || userName.equals("Ana García Rodríguez") || 
                userName.equals("María González"))) {
                prefsManager.saveUserData(
                    "SUPERADMIN001", 
                    "Gabrielle Ivonne", 
                    "superadmin@droidtour.com", 
                    "999888777", 
                    "SUPERADMIN"
                );
                userName = "Gabrielle Ivonne";
                userEmail = "superadmin@droidtour.com";
            }
        }
        
        // Asegurar que el email sea el correcto del superadministrador
        if (!userEmail.equals("superadmin@droidtour.com") && userType != null && 
            (userType.equals("SUPERADMIN") || userType.equals("ADMIN"))) {
            prefsManager.saveUserData(
                "SUPERADMIN001", 
                userName, 
                "superadmin@droidtour.com", 
                "999888777", 
                "SUPERADMIN"
            );
            userEmail = "superadmin@droidtour.com";
        }
        
        // Actualizar los TextView del header
        TextView tvUserName = findViewById(R.id.tv_user_name);
        TextView tvUserEmail = findViewById(R.id.tv_user_email);
        
        if (tvUserName != null) {
            tvUserName.setText(userName != null && !userName.isEmpty() ? userName : "Gabrielle Ivonne");
        }
        
        if (tvUserEmail != null) {
            tvUserEmail.setText(userEmail != null && !userEmail.isEmpty() ? userEmail : "superadmin@droidtour.com");
        }
    }
}

