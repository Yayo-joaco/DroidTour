package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.droidtour.client.ClientRegistrationActivity;
import com.example.droidtour.client.ClientRegistrationPhotoActivity;
import com.example.droidtour.utils.NavigationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class RoleSelectionActivity extends AppCompatActivity {

    private MaterialCardView cardRegisterClient, cardRegisterGuide, cardRegisterAdmin;
    private MaterialToolbar toolbar;

    private boolean isGoogleUser = false;
    private String userEmail, userName, userPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        // Obtener datos del usuario de Google si existe
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isGoogleUser = extras.getBoolean("googleUser", false);
            userEmail = extras.getString("userEmail", "");
            userName = extras.getString("userName", "");
            userPhoto = extras.getString("userPhoto", "");
        }

        initializeViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        cardRegisterClient = findViewById(R.id.card_register_client);
        cardRegisterGuide = findViewById(R.id.card_register_guide);
        cardRegisterAdmin = findViewById(R.id.card_register_admin);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Seleccionar Tipo de Usuario");
        }

        // Configurar el listener de navegación CORRECTAMENTE
        toolbar.setNavigationOnClickListener(v -> handleBackNavigation());
    }

    private void setupClickListeners() {
        cardRegisterClient.setOnClickListener(v -> {
            if (isGoogleUser) {
                // Registro rápido con Google como cliente
                startGoogleRegistration("CLIENT");
            } else {
                startActivity(new Intent(this, ClientRegistrationActivity.class));
            }
        });

        cardRegisterGuide.setOnClickListener(v -> {
            if (isGoogleUser) {
                // Registro con Google como guía
                startGoogleRegistration("GUIDE");
            } else {
                startActivity(new Intent(this, GuideRegistrationActivity.class));
            }
        });

        // ELIMINA ESTA PARTE - ya está configurado en setupToolbar()
        // findViewById(R.id.toolbar).setNavigationOnClickListener(v -> {
        //    if (isGoogleUser) {
        //        FirebaseAuth.getInstance().signOut();
        //    }
        //    finish();
        // });
    }

    private void startGoogleRegistration(String userType) {
        Intent intent;
        if ("CLIENT".equals(userType)) {
            // Cambiar: ir a ClientRegistrationActivity en lugar de ClientRegistrationPhotoActivity
            intent = new Intent(this, ClientRegistrationActivity.class);
        } else {
            intent = new Intent(this, GuideRegistrationActivity.class);
        }

        intent.putExtra("googleUser", true);
        intent.putExtra("userType", userType);
        intent.putExtra("userEmail", userEmail);
        intent.putExtra("userName", userName);
        intent.putExtra("userPhoto", userPhoto);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        handleBackNavigation();
        return true;
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void handleBackNavigation() {
        if (isGoogleUser) {
            Toast.makeText(this, "Sesión de Google cerrada", Toast.LENGTH_SHORT).show();
            NavigationUtils.navigateBackToLogin(this, true);
        } else {
            finish();
        }
    }
}