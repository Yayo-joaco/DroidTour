package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

public class GuideMyAccount extends AppCompatActivity {
    
    private static final String TAG = "GuideMyAccount";
    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private String currentUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar helpers
        prefsManager = new PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);
        
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
        
        // Obtener ID del usuario actual
        currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = prefsManager.getUserId();
        }
        
        setContentView(R.layout.activity_myaccount);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.mi_cuenta));
        }
        toolbar.setTitle(getString(R.string.mi_cuenta));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Cargar datos desde Firebase
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
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Error: currentUserId es null o vacío");
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "🔄 Cargando datos del usuario: " + currentUserId);
        
        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                Log.d(TAG, "✅ Datos del usuario cargados");
                displayUserData(user);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando datos del usuario", e);
                Toast.makeText(GuideMyAccount.this, 
                    "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Usar datos de PreferencesManager como fallback
                displayFallbackData();
            }
        });
    }
    
    private void displayUserData(User user) {
        TextView tvUserName = findViewById(R.id.tv_user_name);
        TextView tvUserEmail = findViewById(R.id.tv_user_email);
        
        String fullName = user.getFullName() != null ? user.getFullName() : 
                         (user.getFirstName() + " " + user.getLastName());
        String email = user.getEmail();
        
        if (tvUserName != null) {
            tvUserName.setText(fullName);
        }
        
        if (tvUserEmail != null) {
            tvUserEmail.setText(email != null ? email : "No especificado");
        }
        
        Log.d(TAG, "✅ UI actualizada con datos de Firebase");
    }
    
    private void displayFallbackData() {
        TextView tvUserName = findViewById(R.id.tv_user_name);
        TextView tvUserEmail = findViewById(R.id.tv_user_email);
        
        if (tvUserName != null) {
            tvUserName.setText(prefsManager.getUserName());
        }
        
        if (tvUserEmail != null) {
            tvUserEmail.setText(prefsManager.getUserEmail());
        }
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

