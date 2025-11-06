package com.example.droidtour;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.droidtour.client.ClientMainActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.managers.FileManager;
import org.json.JSONObject;
import org.json.JSONException;
import android.util.Log;
import java.util.Calendar;

public class ClientRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etFirstName, etLastName, etDocumentNumber, etBirthDate,
            etEmail, etPhone, etAddress, etPassword, etConfirmPassword;
    private AutoCompleteTextView spinnerDocumentType;
    private MaterialButton btnRegister, btnSelectPhoto;
    private MaterialToolbar toolbar;

    // ==================== LOCAL STORAGE ====================
    private PreferencesManager prefsManager;
    private FileManager fileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_registration);

        initializeLocalStorage();
        initializeViews();
        setupToolbar();
        setupDocumentTypeSpinner();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        spinnerDocumentType = findViewById(R.id.spinner_document_type);
        etDocumentNumber = findViewById(R.id.et_document_number);
        etBirthDate = findViewById(R.id.et_birth_date);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Registro de Cliente");
    }

    private void setupDocumentTypeSpinner() {
        String[] documentTypes = {"DNI", "Pasaporte", "Carnet de Extranjería"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, documentTypes);
        spinnerDocumentType.setAdapter(adapter);
    }

    private void setupClickListeners() {
        etBirthDate.setOnClickListener(v -> showDatePicker());

        btnSelectPhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad de selección de foto - En desarrollo", Toast.LENGTH_SHORT).show();
        });

        btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                registerClient();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                    etBirthDate.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private boolean validateForm() {
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("Campo obligatorio");
            return false;
        }
        if (etLastName.getText().toString().trim().isEmpty()) {
            etLastName.setError("Campo obligatorio");
            return false;
        }
        if (spinnerDocumentType.getText().toString().trim().isEmpty()) {
            spinnerDocumentType.setError("Seleccione tipo de documento");
            return false;
        }
        if (etDocumentNumber.getText().toString().trim().isEmpty()) {
            etDocumentNumber.setError("Campo obligatorio");
            return false;
        }
        if (etBirthDate.getText().toString().trim().isEmpty()) {
            etBirthDate.setError("Campo obligatorio");
            return false;
        }
        if (etEmail.getText().toString().trim().isEmpty()) {
            etEmail.setError("Campo obligatorio");
            return false;
        }
        if (etPhone.getText().toString().trim().isEmpty()) {
            etPhone.setError("Campo obligatorio");
            return false;
        }
        if (etAddress.getText().toString().trim().isEmpty()) {
            etAddress.setError("Campo obligatorio");
            return false;
        }
        if (etPassword.getText().toString().trim().isEmpty()) {
            etPassword.setError("Campo obligatorio");
            return false;
        }
        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            return false;
        }
        return true;
    }

    private void registerClient() {
        Log.d("ClientRegistration", "Iniciando registro de cliente...");

        try {
            // Obtener datos del formulario
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String documentType = spinnerDocumentType.getText().toString().trim();
            String documentNumber = etDocumentNumber.getText().toString().trim();
            String birthDate = etBirthDate.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            Log.d("ClientRegistration", "Datos obtenidos: " + firstName + " " + lastName + ", " + email);

            // Generar ID único para el cliente
            String clientId = "CLIENT_" + System.currentTimeMillis();
            String fullName = firstName + " " + lastName;

            Log.d("ClientRegistration", "ID generado: " + clientId);

            // 1. Guardar datos básicos en SharedPreferences
            prefsManager.saveUserData(clientId, fullName, email, phone, "CLIENT");

            Log.d("ClientRegistration", "Datos guardados en SharedPreferences");

            // 2. Guardar datos completos en archivo JSON
            JSONObject clientData = new JSONObject();
            clientData.put("id", clientId);
            clientData.put("firstName", firstName);
            clientData.put("lastName", lastName);
            clientData.put("fullName", fullName);
            clientData.put("documentType", documentType);
            clientData.put("documentNumber", documentNumber);
            clientData.put("birthDate", birthDate);
            clientData.put("email", email);
            clientData.put("phone", phone);
            clientData.put("address", address);
            clientData.put("userType", "CLIENT");
            clientData.put("registrationDate", System.currentTimeMillis());
            clientData.put("status", "ACTIVE");
            clientData.put("profileComplete", true);

            Log.d("ClientRegistration", "JSON creado: " + clientData.toString());

            // Guardar perfil completo
            boolean saved = fileManager.guardarDatosUsuario(clientData);

            Log.d("ClientRegistration", "Archivo guardado: " + saved);

            if (saved) {
                // 3. Crear backup del registro
                boolean backupCreated = fileManager.crearBackup("client_registration_" + clientId, clientData);
                Log.d("ClientRegistration", "Backup creado: " + backupCreated);

                // 4. Guardar configuraciones por defecto
                prefsManager.setNotificationsEnabled(true);

                Log.d("ClientRegistration", "Configuraciones guardadas");

                // Verificar que la sesión esté activa
                boolean sessionActive = prefsManager.isLoggedIn();
                Log.d("ClientRegistration", "Sesión activa: " + sessionActive);

                Toast.makeText(this, "Cliente registrado exitosamente\nBienvenido " + fullName, Toast.LENGTH_LONG).show();

                // 5. Redirigir directamente a la actividad principal del cliente
                Log.d("ClientRegistration", "Iniciando redirección a ClientMainActivity");

                Intent intent = new Intent(this, ClientMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

                Log.d("ClientRegistration", "Redirección completada");

            } else {
                Log.e("ClientRegistration", "Error al guardar archivo");
                Toast.makeText(this, "Error al guardar los datos. Intente nuevamente.", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            Log.e("ClientRegistration", "Error JSON: " + e.getMessage());
            Toast.makeText(this, "Error al procesar los datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("ClientRegistration", "Error general: " + e.getMessage());
            Toast.makeText(this, "Error inesperado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
