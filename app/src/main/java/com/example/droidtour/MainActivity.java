package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.droidtour.client.ClientMainActivity;
import com.example.droidtour.superadmin.SuperadminMainActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.managers.FileManager;

public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardSuperadmin, cardTourAdmin, cardTourGuide, cardClient;
    private MaterialButton btnLogin;
    
    // ==================== LOCAL STORAGE ====================
    private PreferencesManager prefsManager;
    private FileManager fileManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ==================== INICIALIZAR LOCAL STORAGE ====================
        initLocalStorage();
        
        // ==================== VERIFICAR SESIÓN ====================
        checkUserSession();

        initViews();
        setupClickListeners();
    }



    private void initViews() {
        cardSuperadmin = findViewById(R.id.card_superadmin);
        cardTourAdmin = findViewById(R.id.card_tour_admin);
        cardTourGuide = findViewById(R.id.card_tour_guide);
        cardClient = findViewById(R.id.card_client);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void setupClickListeners() {
        cardSuperadmin.setOnClickListener(v -> {
            Toast.makeText(this, "Accediendo como Superadministrador", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, SuperadminMainActivity.class));
        });

        cardTourAdmin.setOnClickListener(v -> {
            Toast.makeText(this, "Accediendo como Administrador de Empresa", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, TourAdminMainActivity.class));
        });

        cardTourGuide.setOnClickListener(v -> {
            Toast.makeText(this, "Accediendo como Guía de Turismo", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, TourGuideMainActivity.class));
        });

        cardClient.setOnClickListener(v -> {
            Toast.makeText(this, "Accediendo como Cliente", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, ClientMainActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    // ==================== MÉTODOS DE LOCAL STORAGE ====================
    
    /**
     * Inicializar managers de local storage
     */
    private void initLocalStorage() {
        prefsManager = new PreferencesManager(this);
        fileManager = new FileManager(this);

        // Ejemplo de uso básico - simplificado para PreferencesManager
        Toast.makeText(this, "Sistema de almacenamiento inicializado", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Verificar si hay sesión activa
     */
    private void checkUserSession() {
        if (prefsManager.isLoggedIn()) {
            String usuario = prefsManager.getUserName();
            String tipoUsuario = prefsManager.getUserType();

            Toast.makeText(this, "Bienvenido de vuelta, " + usuario, Toast.LENGTH_SHORT).show();

            // Redirigir según tipo de usuario
            switch (tipoUsuario) {
                case "SUPERADMIN":
                    startActivity(new Intent(this, SuperadminMainActivity.class));
                    finish();
                    break;
                case "ADMIN":
                    startActivity(new Intent(this, TourAdminMainActivity.class));
                    finish();
                    break;
                case "GUIDE":
                    startActivity(new Intent(this, TourGuideMainActivity.class));
                    finish();
                    break;
                case "CLIENT":
                    startActivity(new Intent(this, ClientMainActivity.class));
                    finish();
                    break;
            }
        }
    }
    
    /**
     * Ejemplo de uso de SharedPreferences
     */
    private void ejemploSharedPreferences() {
        // Guardar datos de usuario
        prefsManager.saveUserData("user123", "Juan Pérez", "juan@email.com", "123456789", "CLIENT");

        // Guardar configuraciones
        prefsManager.setNotificationsEnabled(true);

        // Leer datos
        String usuario = prefsManager.getUserName();
        boolean notificaciones = prefsManager.areNotificationsEnabled();

        // Cerrar sesión
        prefsManager.logout();
    }
    
    /**
     * Ejemplo de uso de FileManager
     */
    private void ejemploFileManager() {
        try {
            // Crear JSON con datos de usuario
            org.json.JSONObject datosUsuario = new org.json.JSONObject();
            datosUsuario.put("nombre", "María García");
            datosUsuario.put("email", "maria@email.com");
            datosUsuario.put("telefono", "999888777");
            datosUsuario.put("fecha_registro", System.currentTimeMillis());
            
            // Guardar datos de usuario
            fileManager.guardarDatosUsuario(datosUsuario);
            
            // Guardar cache de API
            fileManager.guardarCache("tours_populares", "{\"tours\": [\"Machu Picchu\", \"Lima Centro\"]}");
            
            // Leer datos
            org.json.JSONObject usuario = fileManager.leerDatosUsuario();
            String cache = fileManager.leerCache("tours_populares");
            
            // Verificar si cache es válido (1 hora)
            boolean cacheValido = fileManager.cacheValido("tours_populares", 3600000);
            
        } catch (org.json.JSONException e) {
            Toast.makeText(this, "Error con JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Limpiar datos de local storage (para testing)
     */
    private void limpiarLocalStorage() {
        prefsManager.logout();
        fileManager.limpiarTodosLosArchivos();
        Toast.makeText(this, "Local storage limpiado", Toast.LENGTH_SHORT).show();
    }


}