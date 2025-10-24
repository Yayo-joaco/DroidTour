package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.managers.FileManager;
import org.json.JSONObject;
import org.json.JSONException;

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

    // ==================== LOCAL STORAGE ====================
    private PreferencesManager prefsManager;
    private FileManager fileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_registration);

        // Verificar si es modo edición
        String mode = getIntent().getStringExtra("mode");
        isEditMode = "edit_profile".equals(mode);

        initializeLocalStorage();
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
        try {
            // Obtener datos del formulario
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String documentNumber = etDocumentNumber.getText().toString().trim();

            // Generar ID único para el guía
            String guideId = isEditMode ? prefsManager.getUserId() : "GUIDE_" + System.currentTimeMillis();
            String fullName = firstName + " " + lastName;

            if (!isEditMode) {
                // 1. Guardar datos básicos en SharedPreferences (solo para registro nuevo)
                prefsManager.saveUserData(guideId, fullName, email, phone, "GUIDE");
                prefsManager.setGuideApproved(true); // Para demo, aprobamos automáticamente
            }

            // 2. Guardar datos completos en archivo JSON
            JSONObject guideData = new JSONObject();
            guideData.put("id", guideId);
            guideData.put("firstName", firstName);
            guideData.put("lastName", lastName);
            guideData.put("fullName", fullName);
            guideData.put("email", email);
            guideData.put("phone", phone);
            guideData.put("documentNumber", documentNumber);
            guideData.put("userType", "GUIDE");
            guideData.put("registrationDate", System.currentTimeMillis());
            guideData.put("lastUpdateDate", System.currentTimeMillis());
            guideData.put("status", "ACTIVE"); // Para demo, aprobamos automáticamente
            guideData.put("approved", true);
            guideData.put("rating", isEditMode ? 4.5 : 0.0);
            guideData.put("toursCompleted", isEditMode ? 15 : 0);

            // Datos específicos del guía
            JSONObject guideSpecifics = new JSONObject();
            guideSpecifics.put("experience", isEditMode ? "5 años de experiencia en turismo cultural" : "Nuevo guía");
            guideSpecifics.put("languages", "Español, Inglés");
            guideSpecifics.put("specialties", "Historia, Arqueología, Gastronomía");
            guideSpecifics.put("certifications", new JSONObject()
                .put("tourism_license", true)
                .put("first_aid", false)
                .put("language_certificates", true)
            );
            guideData.put("guide_info", guideSpecifics);

            // Guardar perfil completo
            boolean saved = fileManager.guardarDatosUsuario(guideData);

            if (saved) {
                // 3. Crear backup del registro
                fileManager.crearBackup("guide_" + (isEditMode ? "update_" : "registration_") + guideId, guideData);

                if (!isEditMode) {
                    // 4. Guardar configuraciones por defecto (solo para registro nuevo)
                    prefsManager.setNotificationsEnabled(true);
                }

                String message = isEditMode ?
                    "Perfil actualizado correctamente" :
                    "¡Registro exitoso!\nBienvenido como Guía de Turismo";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                if (!isEditMode) {
                    // 5. Redirigir según el estado del guía
                    // Para este demo, vamos a aprobar automáticamente al guía
                    // En producción, esto sería manejado por un admin
                    prefsManager.setGuideApproved(true);

                    // Redirigir directamente a TourGuideMainActivity
                    Intent intent = new Intent(this, TourGuideMainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }

                finish();
            } else {
                Toast.makeText(this, "Error al guardar los datos. Intente nuevamente.", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            Toast.makeText(this, "Error al procesar los datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== MÉTODOS DE LOCAL STORAGE ====================

    /**
     * Inicializar managers de local storage
     */
    private void initializeLocalStorage() {
        prefsManager = new PreferencesManager(this);
        fileManager = new FileManager(this);
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
