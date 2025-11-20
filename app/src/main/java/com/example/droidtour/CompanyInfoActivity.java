package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class CompanyInfoActivity extends AppCompatActivity {
    
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private String currentUserId;
    
    private TextInputEditText etCompanyName, etCompanyEmail, etCompanyPhone, etCompanyAddress;
    private MaterialCardView cardImage1, cardImage2, cardMapPreview;
    private MaterialButton btnSelectLocation, btnCancel, btnSave;
    private com.example.droidtour.utils.PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("ADMIN")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_company_info);
        
        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        authManager = com.example.droidtour.firebase.FirebaseAuthManager.getInstance(this);
        currentUserId = authManager.getCurrentUserId();
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        etCompanyName = findViewById(R.id.et_company_name);
        etCompanyEmail = findViewById(R.id.et_company_email);
        etCompanyPhone = findViewById(R.id.et_company_phone);
        etCompanyAddress = findViewById(R.id.et_company_address);
        
        cardImage1 = findViewById(R.id.card_image1);
        cardImage2 = findViewById(R.id.card_image2);
        cardMapPreview = findViewById(R.id.card_map_preview);
        
        btnSelectLocation = findViewById(R.id.btn_select_location);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save_company);
    }
    
    private void setupClickListeners() {
        cardImage1.setOnClickListener(v -> {
            Toast.makeText(this, "Seleccionar imagen 1", Toast.LENGTH_SHORT).show();
            // TODO: Implementar selección de imagen
        });
        
        cardImage2.setOnClickListener(v -> {
            Toast.makeText(this, "Seleccionar imagen 2", Toast.LENGTH_SHORT).show();
            // TODO: Implementar selección de imagen
        });
        
        btnSelectLocation.setOnClickListener(v -> {
            Toast.makeText(this, "Abrir mapa para seleccionar ubicación", Toast.LENGTH_SHORT).show();
            // TODO: Implementar selección de ubicación en mapa
        });
        
        cardMapPreview.setOnClickListener(v -> {
            Toast.makeText(this, "Ver ubicación en mapa", Toast.LENGTH_SHORT).show();
            // TODO: Mostrar mapa completo
        });
        
        btnCancel.setOnClickListener(v -> finish());
        
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveCompanyInfo();
            }
        });
    }
    
    private boolean validateInputs() {
        if (etCompanyName.getText().toString().trim().isEmpty()) {
            etCompanyName.setError("Ingrese el nombre de la empresa");
            return false;
        }
        
        if (etCompanyEmail.getText().toString().trim().isEmpty()) {
            etCompanyEmail.setError("Ingrese el correo electrónico");
            return false;
        }
        
        if (etCompanyPhone.getText().toString().trim().isEmpty()) {
            etCompanyPhone.setError("Ingrese el teléfono");
            return false;
        }
        
        if (etCompanyAddress.getText().toString().trim().isEmpty()) {
            etCompanyAddress.setError("Ingrese la dirección");
            return false;
        }
        
        return true;
    }
    
    private void saveCompanyInfo() {
        String name = etCompanyName.getText().toString().trim();
        String email = etCompanyEmail.getText().toString().trim();
        String phone = etCompanyPhone.getText().toString().trim();
        String address = etCompanyAddress.getText().toString().trim();
        
        // Generar ID único para la empresa
        String companyId = "COMP_" + System.currentTimeMillis();
        
        com.example.droidtour.models.Company company = new com.example.droidtour.models.Company(name, currentUserId, email, phone, "Lima", "Perú");
        company.setAddress(address);
        company.setActive(true);
        
        firestoreManager.createCompany(company, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(CompanyInfoActivity.this, "Empresa guardada exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CompanyInfoActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
