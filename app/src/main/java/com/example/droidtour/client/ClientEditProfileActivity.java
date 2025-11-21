package com.example.droidtour.client;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hbb20.CountryCodePicker;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClientEditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    private TextInputEditText etPhone, etEmail, etFirstName, etLastName;
    private TextInputEditText etBirthDate, etDocumentType, etDocumentNumber;
    private CountryCodePicker countryCodePicker;
    private ExtendedFloatingActionButton fabSave;
    private ImageButton btnEditPhoto;

    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private Uri selectedImageUri;
    private Calendar birthDateCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar helpers
        prefsManager = new PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();

        setupToolbar();
        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Editar Perfil");
        }
    }

    private void initializeViews() {
        // Campos editables
        etPhone = findViewById(R.id.et_phone);
        countryCodePicker = findViewById(R.id.ccp);

        // Campos de solo lectura
        etEmail = findViewById(R.id.et_email);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etBirthDate = findViewById(R.id.et_birth_date);
        etDocumentType = findViewById(R.id.et_document_type);
        etDocumentNumber = findViewById(R.id.et_document_number);

        // Botones
        fabSave = findViewById(R.id.fab_save);
        btnEditPhoto = findViewById(R.id.btn_edit_photo_small);
    }

    /**
     * 🔥 Cargar datos del usuario
     */
    private void loadUserData() {
        if (!prefsManager.isLoggedIn()) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = prefsManager.getUserId();
        Log.d(TAG, "Cargando datos para userId: " + userId);

        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null) {
                    updateUIWithUserData(user);
                } else {
                    Log.e(TAG, "Usuario no encontrado en Firestore");
                    showFallbackData();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario: " + e.getMessage(), e);
                Toast.makeText(ClientEditProfileActivity.this, "Error cargando datos", Toast.LENGTH_SHORT).show();
                showFallbackData();
            }
        });
    }

    /**
     * 🔥 Actualizar UI con datos del usuario
     */
    private void updateUIWithUserData(User user) {
        // Campos editables
        String phone = user.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            // Separar código de país y número si es necesario
            if (phone.startsWith("+")) {
                try {
                    // El CountryCodePicker puede manejar el formato internacional
                    countryCodePicker.setFullNumber(phone);
                    // Extraer solo el número local para el campo de texto
                    String localNumber = phone.substring(phone.indexOf(" ") + 1);
                    etPhone.setText(localNumber);
                } catch (Exception e) {
                    etPhone.setText(phone);
                }
            } else {
                etPhone.setText(phone);
            }
        }

        // Campos de solo lectura
        etEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        etFirstName.setText(user.getFirstName() != null ? user.getFirstName() : "N/A");
        etLastName.setText(user.getLastName() != null ? user.getLastName() : "N/A");

        // Fecha de nacimiento
        if (user.getDateOfBirth() != null && !user.getDateOfBirth().isEmpty()) {
            etBirthDate.setText(user.getDateOfBirth());
        } else {
            etBirthDate.setText("N/A");
        }

        etDocumentType.setText(user.getDocumentType() != null ? user.getDocumentType() : "DNI");
        etDocumentNumber.setText(user.getDocumentNumber() != null ? user.getDocumentNumber() : "N/A");
    }

    /**
     * 🔥 Datos de respaldo desde PreferencesManager
     */
    private void showFallbackData() {
        etEmail.setText(prefsManager.getUserEmail());
        etFirstName.setText("N/A");
        etLastName.setText("N/A");
        etBirthDate.setText("N/A");
        etDocumentType.setText("DNI");
        etDocumentNumber.setText("N/A");

        String phone = prefsManager.getUserPhone();
        if (phone != null && !phone.isEmpty()) {
            etPhone.setText(phone);
        }
    }

    private void setupClickListeners() {
        // Botón editar foto
        btnEditPhoto.setOnClickListener(v -> {
            openImagePicker();
        });

        // Botón guardar cambios
        fabSave.setOnClickListener(v -> {
            saveProfileChanges();
        });

        // Configurar CountryCodePicker
        countryCodePicker.setOnCountryChangeListener(() -> {
            // El código de país se actualiza automáticamente
            Log.d(TAG, "País seleccionado: " + countryCodePicker.getSelectedCountryName() +
                    " Código: " + countryCodePicker.getSelectedCountryCodeWithPlus());
        });
    }

    /**
     * 🔥 Abrir selector de imágenes
     */
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            // Aquí puedes mostrar una preview de la imagen seleccionada
            Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show();
            // TODO: Implementar upload de imagen a Firebase Storage
        }
    }

    /**
     * 🔥 Guardar cambios del perfil
     */
    private void saveProfileChanges() {
        if (!prefsManager.isLoggedIn()) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Validar campos
        if (!validateFields()) {
            return;
        }

        String userId = prefsManager.getUserId();
        Map<String, Object> updates = new HashMap<>();

        // Obtener número de teléfono completo con código de país
        String fullPhoneNumber = getFullPhoneNumber();
        if (fullPhoneNumber != null && !fullPhoneNumber.isEmpty()) {
            updates.put("phoneNumber", fullPhoneNumber);
            // También mantener compatibilidad con el campo legacy "phone"
            updates.put("phone", fullPhoneNumber);
        }

        // Aquí podrías agregar más campos editables en el futuro

        // Si hay imagen seleccionada, subirla primero
        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(userId, updates);
        } else {
            saveProfileToFirestore(userId, updates);
        }
    }

    /**
     * 🔥 Obtener número de teléfono completo con código de país
     */
    private String getFullPhoneNumber() {
        String localNumber = etPhone.getText().toString().trim();
        if (localNumber.isEmpty()) {
            return null;
        }

        // CountryCodePicker ya incluye el código del país
        return countryCodePicker.getFullNumberWithPlus();
    }

    /**
     * 🔥 Validar campos del formulario
     */
    private boolean validateFields() {
        String phone = etPhone.getText().toString().trim();

        // Validar teléfono (opcional)
        if (!phone.isEmpty()) {
            if (phone.length() < 6) {
                etPhone.setError("Número de teléfono muy corto");
                return false;
            }

            // Validar que solo contenga números y espacios
            if (!phone.matches("[0-9\\s]+")) {
                etPhone.setError("Solo se permiten números y espacios");
                return false;
            }
        }

        return true;
    }

    /**
     * 🔥 Subir imagen y luego guardar perfil
     */
    private void uploadImageAndSaveProfile(String userId, Map<String, Object> updates) {
        // TODO: Implementar upload a Firebase Storage
        // Por ahora, guardamos solo los datos sin imagen
        Toast.makeText(this, "Subida de imagen próximamente", Toast.LENGTH_SHORT).show();
        saveProfileToFirestore(userId, updates);
    }

    /**
     * 🔥 Guardar cambios en Firestore
     */
    private void saveProfileToFirestore(String userId, Map<String, Object> updates) {
        if (updates.isEmpty()) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Agregar timestamp de actualización
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        firestoreManager.updateUser(userId, updates, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "✅ Perfil actualizado exitosamente");
                Toast.makeText(ClientEditProfileActivity.this, "Perfil actualizado", Toast.LENGTH_SHORT).show();

                // Actualizar PreferencesManager si el teléfono cambió
                String fullPhoneNumber = getFullPhoneNumber();
                if (fullPhoneNumber != null && !fullPhoneNumber.isEmpty()) {
                    prefsManager.saveUserPhone(fullPhoneNumber);
                }

                // Devolver resultado a ClientProfileActivity
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error actualizando perfil: " + e.getMessage(), e);
                Toast.makeText(ClientEditProfileActivity.this, "Error guardando cambios: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Preguntar si hay cambios sin guardar
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 🔥 Verificar si hay cambios sin guardar
     */
    private boolean hasUnsavedChanges() {
        // Comparar con datos originales o verificar si hay texto en los campos editables
        String currentPhone = etPhone.getText().toString().trim();
        return !currentPhone.isEmpty() || selectedImageUri != null;
    }

    /**
     * 🔥 Mostrar diálogo de cambios sin guardar
     */
    private void showUnsavedChangesDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cambios sin guardar")
                .setMessage("Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?")
                .setPositiveButton("Salir", (dialog, which) -> {
                    // Forzar salida
                    ClientEditProfileActivity.super.onBackPressed();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}