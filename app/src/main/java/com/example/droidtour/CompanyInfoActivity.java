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
    
    private TextInputEditText etCompanyName, etCompanyEmail, etCompanyPhone, etCompanyAddress;
    private MaterialCardView cardImage1, cardImage2, cardMapPreview;
    private MaterialButton btnSelectLocation, btnCancel, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_info);
        
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
        // TODO: Implementar guardado en base de datos
        Toast.makeText(this, "Información de empresa guardada exitosamente", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
