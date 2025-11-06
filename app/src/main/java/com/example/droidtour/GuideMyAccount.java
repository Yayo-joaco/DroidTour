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

import com.example.droidtour.LoginActivity;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

public class GuideMyAccount extends AppCompatActivity {
    
    private PreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager
        prefsManager = new PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea un guía
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_myaccount);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

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
                Intent i = new Intent(GuideMyAccount.this, GuideProfileActivity.class);
                startActivity(i);
            });
        }

        // Los guías no tienen métodos de pago, así que lo ocultamos
        if (cardPayment != null) {
            cardPayment.setVisibility(android.view.View.GONE);
        }

        // Los guías no tienen configuración separada, así que ocultamos esta opción también
        // O podemos redirigir a Mi Perfil si se prefiere mantenerla
        if (cardSettings != null) {
            cardSettings.setVisibility(android.view.View.GONE);
        }
        
        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                // Se limpian los datos de sesión
                prefsManager.logout();
                
                // Limpiar el stack de activities y redirigir a Login
                Intent intent = new Intent(GuideMyAccount.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                
                android.widget.Toast.makeText(this, "Sesión cerrada correctamente", 
                    android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void loadUserData() {
        // Verificar y corregir datos del guía
        String userType = prefsManager.getUserType();
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();
        
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
            userName = "Carlos Mendoza";
            userEmail = "guia@tours.com";
        } else {
            // Si está logueado pero el nombre no es correcto, corregirlo
            if (!userName.equals("Carlos Mendoza") && (userName.equals("Gabrielle Ivonne") || 
                userName.equals("María López") || userName.equals("Ana García Rodríguez") || 
                userName.equals("María González"))) {
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
                userEmail = "guia@tours.com";
            }
        }
        
        // Asegurar que el email sea el correcto del guía
        if (!userEmail.equals("guia@tours.com") && userType != null && userType.equals("GUIDE")) {
            prefsManager.saveUserData(
                "GUIDE001", 
                userName, 
                "guia@tours.com", 
                "987654321", 
                "GUIDE"
            );
            prefsManager.setGuideApproved(true);
            prefsManager.setGuideRating(4.8f);
            userEmail = "guia@tours.com";
        }
        
        // Actualizar los TextView del header
        TextView tvUserName = findViewById(R.id.tv_user_name);
        TextView tvUserEmail = findViewById(R.id.tv_user_email);
        
        if (tvUserName != null) {
            tvUserName.setText(userName != null && !userName.isEmpty() ? userName : "Carlos Mendoza");
        }
        
        if (tvUserEmail != null) {
            tvUserEmail.setText(userEmail != null && !userEmail.isEmpty() ? userEmail : "guia@tours.com");
        }
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

