package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SuperadminProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvDocumentType, tvDocumentNumber, tvPhone;
    private CardView cardLanguages, cardStatistics;
    private FloatingActionButton fabEdit;
    private ImageButton btnEditPhoto;
    
    private PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myprofile);

        // Inicializar helpers
        prefsManager = new PreferencesManager(this);

        setupToolbar();
        initializeViews();
        loadUserData();
        setupClickListeners();
        
        // Ocultar secciones no necesarias para superadministrador
        hideLanguagesSection();
        hideStatisticsSection();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mi Perfil");
        }
    }

    private void initializeViews() {
        // Header
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvUserRole = findViewById(R.id.tv_user_role);
        btnEditPhoto = findViewById(R.id.btn_edit_photo_small);
        
        // Información personal
        tvDocumentType = findViewById(R.id.tv_document_type);
        tvDocumentNumber = findViewById(R.id.tv_document_number);
        tvPhone = findViewById(R.id.tv_phone);
        
        // Secciones para ocultar
        cardLanguages = findViewById(R.id.card_languages);
        cardStatistics = findViewById(R.id.card_statistics);
        
        // FAB
        fabEdit = findViewById(R.id.fab_edit);
    }

    private void loadUserData() {
        if (!prefsManager.isLoggedIn()) {
            Toast.makeText(this, "Sesión expirada. Por favor, inicia sesión nuevamente", Toast.LENGTH_SHORT).show();
            // Redirigir al login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Verificar que el usuario sea un superadministrador
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("SUPERADMIN") && !userType.equals("ADMIN"))) {
            // Si no es superadministrador, inicializar datos del superadministrador
            prefsManager.saveUserData(
                "SUPERADMIN001", 
                "Gabrielle Ivonne", 
                "superadmin@droidtour.com", 
                "999888777", 
                "SUPERADMIN"
            );
            userType = "SUPERADMIN";
        }

        // Cargar datos del usuario (superadministrador)
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();
        String userPhone = prefsManager.getUserPhone();

        // Si los datos son de otro rol, reemplazarlos con datos del superadministrador
        if (!userName.equals("Gabrielle Ivonne") && (userName.equals("Carlos Mendoza") || 
            userName.equals("María López") || userName.equals("Ana García Rodríguez"))) {
            prefsManager.saveUserData(
                "SUPERADMIN001", 
                "Gabrielle Ivonne", 
                "superadmin@droidtour.com", 
                "999888777", 
                "SUPERADMIN"
            );
            userName = "Gabrielle Ivonne";
            userEmail = "superadmin@droidtour.com";
            userPhone = "999888777";
        }
        
        // Si el email no es el correcto del superadministrador, corregirlo
        if (!userEmail.equals("superadmin@droidtour.com") && userType.equals("SUPERADMIN")) {
            prefsManager.saveUserData(
                "SUPERADMIN001", 
                userName, 
                "superadmin@droidtour.com", 
                userPhone, 
                "SUPERADMIN"
            );
            userEmail = "superadmin@droidtour.com";
        }

        // Actualizar header
        tvUserName.setText(userName);
        tvUserEmail.setText(userEmail);
        
        // Actualizar rol - siempre mostrar "SUPER ADMINISTRADOR"
        tvUserRole.setText("SUPER ADMINISTRADOR");

        // Actualizar información personal
        tvDocumentType.setText("DNI");
        tvDocumentNumber.setText("98765432"); // Diferente a otros roles
        tvPhone.setText(userPhone != null && !userPhone.isEmpty() ? userPhone : "+51 999 888 777");
    }

    private void hideLanguagesSection() {
        // Ocultar sección de idiomas para superadministrador
        if (cardLanguages != null) {
            cardLanguages.setVisibility(View.GONE);
        }
    }

    private void hideStatisticsSection() {
        // Ocultar sección de estadísticas para superadministrador
        if (cardStatistics != null) {
            cardStatistics.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // Botón editar foto
        if (btnEditPhoto != null) {
            btnEditPhoto.setOnClickListener(v -> {
                Toast.makeText(this, "Editar foto próximamente", Toast.LENGTH_SHORT).show();
            });
        }

        // FAB editar
        if (fabEdit != null) {
            fabEdit.setOnClickListener(v -> {
                Toast.makeText(this, "Edición de perfil próximamente", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

