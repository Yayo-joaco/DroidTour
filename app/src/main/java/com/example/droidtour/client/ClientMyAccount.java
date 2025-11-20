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

import com.example.droidtour.LoginActivity;
import com.example.droidtour.MainActivity;
import com.example.droidtour.R;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

public class ClientMyAccount extends AppCompatActivity {

    private PreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea CLIENT
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
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

        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                // Cerrar sesión
                prefsManager.cerrarSesion();
                Intent i = new Intent(ClientMyAccount.this, LoginActivity.class);
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
        // Obtener datos del usuario desde PreferencesManager (ya guardados correctamente por LoginActivity)
        String userName = prefsManager.obtenerUsuario();
        String userEmail = prefsManager.obtenerEmail();

        // Actualizar los TextView del header
        TextView tvUserName = findViewById(R.id.tv_user_name);
        TextView tvUserEmail = findViewById(R.id.tv_user_email);

        if (tvUserName != null) {
            tvUserName.setText(userName != null && !userName.isEmpty() ? userName : "Gabrielle Ivonne");
        }

        if (tvUserEmail != null) {
            tvUserEmail.setText(userEmail != null && !userEmail.isEmpty() ? userEmail : "cliente@email.com");
        }
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
