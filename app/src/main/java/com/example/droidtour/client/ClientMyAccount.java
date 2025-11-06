package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.User;
import com.google.android.material.appbar.MaterialToolbar;

public class ClientMyAccount extends AppCompatActivity {
    
    // ✅ Firebase Managers
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private String currentUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myaccount);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // ✅ Inicializar Firebase
        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        
        currentUserId = authManager.getCurrentUserId();
        
        // 🔥 TEMPORAL: Para testing sin login
        if (currentUserId == null) {
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            android.widget.Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", android.widget.Toast.LENGTH_SHORT).show();
        }

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

        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                Intent i = new Intent(ClientMyAccount.this, ClientProfileActivity.class);
                startActivity(i);
            });
        }

        if (cardPayment != null) {
            cardPayment.setOnClickListener(v -> {
                Intent i = new Intent(ClientMyAccount.this, PaymentMethodsActivity.class);
                startActivity(i);
            });
        }

        if (cardSettings != null) {
            cardSettings.setOnClickListener(v -> {
                Intent i = new Intent(ClientMyAccount.this, ClientSettingsActivity.class);
                startActivity(i);
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    /**
     * ✅ CARGAR DATOS DEL USUARIO DESDE FIREBASE
     */
    private void loadUserData() {
        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                
                // Actualizar los TextView del header
                TextView tvUserName = findViewById(R.id.tv_user_name);
                TextView tvUserEmail = findViewById(R.id.tv_user_email);
                
                if (tvUserName != null) {
                    tvUserName.setText(user.getFullName());
                }
                
                if (tvUserEmail != null) {
                    tvUserEmail.setText(user.getEmail());
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Usar valores por defecto en caso de error
                TextView tvUserName = findViewById(R.id.tv_user_name);
                TextView tvUserEmail = findViewById(R.id.tv_user_email);
                
                if (tvUserName != null) {
                    tvUserName.setText("Usuario");
                }
                
                if (tvUserEmail != null) {
                    tvUserEmail.setText("");
                }
            }
        });
    }
}
