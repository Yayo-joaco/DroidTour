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
            String userId = prefsManager.getUserId();

            Toast.makeText(this, "Bienvenido de vuelta, " + usuario, Toast.LENGTH_SHORT).show();

            // Redirigir según tipo de usuario
            switch (tipoUsuario) {
                case "SUPERADMIN":
                    startActivity(new Intent(this, SuperadminMainActivity.class));
                    finish();
                    break;
                case "ADMIN":
                case "COMPANY_ADMIN":
                    startActivity(new Intent(this, TourAdminMainActivity.class));
                    finish();
                    break;
                case "GUIDE":
                    // Validar estado de aprobación del guía antes de redirigir
                    if (userId != null && !userId.isEmpty()) {
                        checkGuideApprovalStatus(userId);
                    } else {
                        // Si no hay userId, redirigir a login
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                    break;
                case "CLIENT":
                    startActivity(new Intent(this, ClientMainActivity.class));
                    finish();
                    break;
            }
        }
    }

    /**
     * Validar estado de aprobación del guía
     */
    private void checkGuideApprovalStatus(String userId) {
        com.example.droidtour.firebase.FirestoreManager firestoreManager = 
            com.example.droidtour.firebase.FirestoreManager.getInstance();
        
        firestoreManager.getUserById(userId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User userObj = (com.example.droidtour.models.User) result;
                String statusField = userObj.getStatus();

                if (statusField != null && ("inactive".equalsIgnoreCase(statusField) ||
                        "suspended".equalsIgnoreCase(statusField))) {
                    redirectToUserDisabled(userId);
                    return;
                }

                // Revisar user_roles para verificar estado de aprobación
                firestoreManager.getUserRoles(userId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> rolesData = (java.util.Map<String, Object>) result;

                        String guideStatus = extractGuideStatus(rolesData);

                        if ("active".equals(guideStatus)) {
                            // Guía aprobado - redirigir al dashboard
                            startActivity(new Intent(MainActivity.this, TourGuideMainActivity.class));
                            finish();
                        } else {
                            // Guía no aprobado - redirigir a pantalla de espera
                            redirectToApprovalPending();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        android.util.Log.e("MainActivity", "Error al obtener user_roles", e);
                        // Por seguridad, redirigir a pantalla de espera si no se puede verificar
                        redirectToApprovalPending();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("MainActivity", "Error al obtener usuario", e);
                // Por seguridad, redirigir a pantalla de espera si no se puede verificar
                redirectToApprovalPending();
            }
        });
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
        Intent intent = new Intent(this, com.example.droidtour.GuideApprovalPendingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToUserDisabled(String userId) {
        Intent intent = new Intent(this, com.example.droidtour.UserDisabledActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("reason", "Tu cuenta ha sido desactivada. Contacta con soporte.");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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