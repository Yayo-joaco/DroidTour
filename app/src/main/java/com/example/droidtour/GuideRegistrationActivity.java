package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class GuideRegistrationActivity extends AppCompatActivity {
    
    private TextInputEditText etFirstName;
    private TextInputEditText etLastName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etDocumentNumber;
    // private TextInputEditText etExperience;
    // private TextInputEditText etLanguages;
    // private TextInputEditText etSpecialties;
    private Button btnRegister;
    private Button btnCancel;
    
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_registration);
        
        // Verificar si es modo edición
        String mode = getIntent().getStringExtra("mode");
        isEditMode = "edit_profile".equals(mode);

        initializeViews();
        setupToolbar();
        setupClickListeners();

        if (isEditMode) {
            loadExistingData();
        }
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (isEditMode) {
            getSupportActionBar().setTitle("Editar Perfil");
            btnRegister.setText("Guardar Cambios");
        } else {
            getSupportActionBar().setTitle("Registro de Guía");
            btnRegister.setText("Registrarse");
        }
    }
    
    private void initializeViews() {
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etDocumentNumber = findViewById(R.id.et_document_number);
        // Los campos de experiencia, idiomas y especialidades no existen en el layout actual
        // etExperience = findViewById(R.id.et_experience);
        // etLanguages = findViewById(R.id.et_languages);
        // etSpecialties = findViewById(R.id.et_specialties);
        btnRegister = findViewById(R.id.btn_register);
        btnCancel = findViewById(R.id.btn_cancel);
    }
    
    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                saveGuideData();
            }
        });
        
        btnCancel.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void loadExistingData() {
        // TODO: Cargar datos existentes del guía desde base de datos
        // Por ahora cargar datos de ejemplo
        etFirstName.setText("Carlos");
        etLastName.setText("Mendoza");
        etEmail.setText("carlos.mendoza@email.com");
        etPhone.setText("987654321");
        etDocumentNumber.setText("12345678");
        // Los campos de experiencia, idiomas y especialidades no están disponibles en el layout actual
        // etExperience.setText("5 años de experiencia en turismo cultural");
        // etLanguages.setText("Español, Inglés, Quechua");
        // etSpecialties.setText("Historia, Arqueología, Gastronomía");
    }
    
    private boolean validateForm() {
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("Campo requerido");
            return false;
        }
        
        if (etLastName.getText().toString().trim().isEmpty()) {
            etLastName.setError("Campo requerido");
            return false;
        }
        
        if (etEmail.getText().toString().trim().isEmpty()) {
            etEmail.setError("Campo requerido");
            return false;
        }
        
        if (etPhone.getText().toString().trim().isEmpty()) {
            etPhone.setError("Campo requerido");
            return false;
        }
        
        if (etDocumentNumber.getText().toString().trim().isEmpty()) {
            etDocumentNumber.setError("Campo requerido");
            return false;
        }
        
        return true;
    }
    
    private void saveGuideData() {
        // TODO: Guardar datos del guía en base de datos
        
        String message = isEditMode ? "Perfil actualizado correctamente" : "Registro enviado para aprobación";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        
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
