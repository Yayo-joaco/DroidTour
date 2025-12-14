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

    // Para almacenar datos originales
    private User currentUser;

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
                currentUser = (User) result;
                if (currentUser != null) {
                    updateUIWithUserData(currentUser);
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
     * 🔥 Actualizar UI con datos del usuario CORREGIDO
     */
    private void updateUIWithUserData(User user) {
        // Guardar referencia al usuario
        this.currentUser = user;

        // Campos editables - Teléfono
        if (user.getPersonalData() != null) {
            String phone = user.getPersonalData().getPhoneNumber();
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
        }

        // Campos de solo lectura
        etEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");

        // Usar métodos compatibles que acceden a personalData
        etFirstName.setText(user.getFirstName() != null ? user.getFirstName() : "N/A");
        etLastName.setText(user.getLastName() != null ? user.getLastName() : "N/A");

        // Fecha de nacimiento - Acceder a través de personalData
        if (user.getPersonalData() != null) {
            String dateOfBirth = user.getPersonalData().getDateOfBirth();
            if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                etBirthDate.setText(dateOfBirth);
            } else {
                etBirthDate.setText("N/A");
            }

            etDocumentType.setText(user.getPersonalData().getDocumentType() != null ?
                    user.getPersonalData().getDocumentType() : "DNI");
            etDocumentNumber.setText(user.getPersonalData().getDocumentNumber() != null ?
                    user.getPersonalData().getDocumentNumber() : "N/A");
        } else {
            etBirthDate.setText("N/A");
            etDocumentType.setText("DNI");
            etDocumentNumber.setText("N/A");
        }
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
            Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show();
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

        // Crear nuevo objeto User con los cambios
        User updatedUser = createUpdatedUser();

        if (updatedUser != null) {
            saveProfileToFirestore(userId, updatedUser);
        }
    }

    /**
     * 🔥 Crear objeto User actualizado
     */
    private User createUpdatedUser() {
        if (currentUser == null) {
            return null;
        }

        // Crear copia del usuario actual
        User updatedUser = new User();
        updatedUser.setUserId(currentUser.getUserId());
        updatedUser.setEmail(currentUser.getEmail());
        updatedUser.setUserType(currentUser.getUserType());
        updatedUser.setStatus(currentUser.getStatus());
        updatedUser.setCreatedAt(currentUser.getCreatedAt());
        updatedUser.setCompanyId(currentUser.getCompanyId());

        // Actualizar personalData
        User.PersonalData personalData = new User.PersonalData();

        // Mantener datos existentes o usar nuevos
        personalData.setFirstName(currentUser.getPersonalData() != null ?
                currentUser.getPersonalData().getFirstName() : null);
        personalData.setLastName(currentUser.getPersonalData() != null ?
                currentUser.getPersonalData().getLastName() : null);

        // Actualizar teléfono si cambió
        String fullPhoneNumber = getFullPhoneNumber();
        if (fullPhoneNumber != null && !fullPhoneNumber.isEmpty()) {
            personalData.setPhoneNumber(fullPhoneNumber);
        } else if (currentUser.getPersonalData() != null) {
            personalData.setPhoneNumber(currentUser.getPersonalData().getPhoneNumber());
        }

        // Mantener otros datos
        if (currentUser.getPersonalData() != null) {
            personalData.setDocumentType(currentUser.getPersonalData().getDocumentType());
            personalData.setDocumentNumber(currentUser.getPersonalData().getDocumentNumber());
            personalData.setDateOfBirth(currentUser.getPersonalData().getDateOfBirth());
            personalData.setProfileImageUrl(currentUser.getPersonalData().getProfileImageUrl());
        }

        // Actualizar fullName
        if (personalData.getFirstName() != null && personalData.getLastName() != null) {
            personalData.setFullName(personalData.getFirstName() + " " + personalData.getLastName());
        }

        updatedUser.setPersonalData(personalData);

        return updatedUser;
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
     * 🔥 Guardar cambios en Firestore CORREGIDO
     */
    private void saveProfileToFirestore(String userId, User updatedUser) {
        firestoreManager.upsertUser(updatedUser, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "✅ Perfil actualizado exitosamente");
                Toast.makeText(ClientEditProfileActivity.this, "Perfil actualizado", Toast.LENGTH_SHORT).show();

                // Actualizar PreferencesManager si el teléfono cambió
                String fullPhoneNumber = getFullPhoneNumber();
                if (fullPhoneNumber != null && !fullPhoneNumber.isEmpty()) {
                    prefsManager.saveUserPhone(fullPhoneNumber);
                }

                // Actualizar referencia local
                currentUser = updatedUser;

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
        // Verificar si el teléfono cambió
        if (currentUser != null && currentUser.getPersonalData() != null) {
            String currentPhone = etPhone.getText().toString().trim();
            String originalPhone = currentUser.getPersonalData().getPhoneNumber();

            // Normalizar números para comparación
            if (originalPhone != null && originalPhone.startsWith("+")) {
                try {
                    // Extraer solo el número local del original
                    String originalLocal = originalPhone.substring(originalPhone.indexOf(" ") + 1);
                    if (!originalLocal.equals(currentPhone)) {
                        return true;
                    }
                } catch (Exception e) {
                    // Si hay error en el parsing, comparar directamente
                    if (!originalPhone.equals(getFullPhoneNumber())) {
                        return true;
                    }
                }
            }
        }

        return selectedImageUri != null;
    }

    /**
     * 🔥 Mostrar diálogo de cambios sin guardar
     */
    private void showUnsavedChangesDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cambios sin guardar")
                .setMessage("Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?")
                .setPositiveButton("Salir", (dialog, which) -> {
                    ClientEditProfileActivity.super.onBackPressed();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}