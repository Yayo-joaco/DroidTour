package com.example.droidtour.client;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
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

    private TextInputEditText etPhoneNumber; // Este es solo para mostrar el número local
    private TextInputEditText etEmail, etFirstName, etLastName;
    private TextInputEditText etBirthDate, etDocumentType, etDocumentNumber;
    private CountryCodePicker countryCodePicker;
    private ExtendedFloatingActionButton fabSave;
    private ImageButton btnEditPhoto;
    private ImageView profileImage;

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
        etPhoneNumber = findViewById(R.id.et_phone); // Este es el TextInputEditText
        countryCodePicker = findViewById(R.id.ccp); // Este es el CountryCodePicker separado

        // IMPORTANTE: Asignar el EditText al CountryCodePicker
        // Esto es lo que falta y causa el NullPointerException
        countryCodePicker.registerCarrierNumberEditText(etPhoneNumber);

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

        // Imagen de perfil
        profileImage = findViewById(R.id.profile_image);
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

        // Cargar imagen de perfil si existe
        String photoUrl = null;
        if (user.getPersonalData() != null) {
            photoUrl = user.getPersonalData().getProfileImageUrl();
        }

        if (profileImage != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_avatar_24)
                    .error(R.drawable.ic_avatar_24)
                    .circleCrop()
                    .into(profileImage);
        }

        // Campos editables - Teléfono
        if (user.getPersonalData() != null) {
            String phone = user.getPersonalData().getPhoneNumber();
            if (phone != null && !phone.isEmpty()) {
                try {
                    // Configurar el CountryCodePicker con el número completo
                    countryCodePicker.setFullNumber(phone);

                    // Extraer solo el número local para el campo de texto
                    String localNumber = extractLocalNumber(phone);
                    etPhoneNumber.setText(formatPhoneNumber(localNumber));
                } catch (Exception e) {
                    Log.e(TAG, "Error configurando número de teléfono: " + e.getMessage());
                    // Si falla, mostrar el número tal cual
                    etPhoneNumber.setText(formatPhoneNumber(phone));
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
     * Extrae solo la parte local del número telefónico
     */
    private String extractLocalNumber(String fullNumber) {
        if (fullNumber == null || fullNumber.isEmpty()) {
            return "";
        }

        // Remover el código de país si existe
        if (fullNumber.contains(" ")) {
            return fullNumber.substring(fullNumber.indexOf(" ") + 1);
        }

        return fullNumber;
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
            try {
                countryCodePicker.setFullNumber(phone);
                String localNumber = extractLocalNumber(phone);
                etPhoneNumber.setText(formatPhoneNumber(localNumber));
            } catch (Exception e) {
                Log.e(TAG, "Error configurando teléfono de respaldo: " + e.getMessage());
            }
        }

        if (profileImage != null) {
            profileImage.setImageResource(R.drawable.ic_avatar_24);
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

        setupPhoneFormatter();
    }

    /**
     * Formatea un número de teléfono local agregando espacios cada 3 dígitos
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < digitsOnly.length(); i++) {
            if (i > 0 && i % 3 == 0) {
                formatted.append(" ");
            }
            formatted.append(digitsOnly.charAt(i));
        }
        return formatted.toString();
    }

    private void setupPhoneFormatter() {
        etPhoneNumber.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) return;

                isFormatting = true;
                String text = s.toString().replaceAll("\\s+", ""); // Remover espacios

                // Solo permitir dígitos
                text = text.replaceAll("[^0-9]", "");

                // Formatear con espacios cada 3 dígitos
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < text.length(); i++) {
                    if (i > 0 && i % 3 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(text.charAt(i));
                }

                // Actualizar el texto
                int cursorPosition = etPhoneNumber.getSelectionStart();
                int lengthBefore = s.length();
                s.clear();
                s.append(formatted.toString());

                // Ajustar la posición del cursor
                int lengthAfter = formatted.length();
                int cursorOffset = lengthAfter - lengthBefore;
                int newCursorPosition = Math.max(0, Math.min(formatted.length(), cursorPosition + cursorOffset));
                etPhoneNumber.setSelection(newCursorPosition);

                isFormatting = false;
            }
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

            // Previsualizar la imagen seleccionada
            if (profileImage != null && selectedImageUri != null) {
                Glide.with(this)
                        .load(selectedImageUri)
                        .placeholder(R.drawable.ic_avatar_24)
                        .error(R.drawable.ic_avatar_24)
                        .circleCrop()
                        .into(profileImage);
            }
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
            if (selectedImageUri != null) {
                // Si se seleccionó nueva imagen, subirla y guardar todo junto
                saveProfileWithImage(userId, updatedUser, selectedImageUri);
            } else {
                saveProfileToFirestore(userId, updatedUser);
            }
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
        if (countryCodePicker == null || etPhoneNumber == null) {
            return null;
        }

        try {
            String localNumber = etPhoneNumber.getText().toString().trim().replaceAll("\\s+", "");

            if (localNumber.isEmpty()) {
                return null;
            }

            // Construir manualmente el número completo
            String countryCode = countryCodePicker.getSelectedCountryCode();
            if (countryCode != null && !countryCode.isEmpty()) {
                return "+" + countryCode + " " + localNumber;
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error obteniendo número de teléfono: " + e.getMessage());
            return null;
        }
    }

    /**
     * 🔥 Validar campos del formulario
     */
    private boolean validateFields() {
        String localNumber = etPhoneNumber.getText().toString().trim().replaceAll("\\s+", "");

        // Validar teléfono (opcional)
        if (!localNumber.isEmpty()) {
            // Validar que el número tenga exactamente 9 dígitos
            if (localNumber.length() != 9) {
                etPhoneNumber.setError("El número de teléfono debe tener 9 dígitos");
                return false;
            }

            // Validar que solo contenga dígitos
            if (!localNumber.matches("\\d{9}")) {
                etPhoneNumber.setError("El número de teléfono solo debe contener dígitos");
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

    /**
     * 🔥 Guardar cambios incluyendo imagen (sube imagen y guarda user)
     */
    private void saveProfileWithImage(String userId, User updatedUser, Uri imageUri) {
        // registerClient maneja la subida de la imagen (desde Uri) y guarda la URL en el documento
        firestoreManager.registerClient(updatedUser, imageUri, null, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "✅ Perfil con imagen actualizado exitosamente");
                Toast.makeText(ClientEditProfileActivity.this, "Perfil actualizado", Toast.LENGTH_SHORT).show();

                // Actualizar PreferencesManager si el teléfono cambió
                String fullPhoneNumber = getFullPhoneNumber();
                if (fullPhoneNumber != null && !fullPhoneNumber.isEmpty()) {
                    prefsManager.saveUserPhone(fullPhoneNumber);
                }

                // Actualizar referencia local
                currentUser = (User) result;

                // Devolver resultado a ClientProfileActivity
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error actualizando perfil con imagen: " + e.getMessage(), e);
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
            String currentPhone = getFullPhoneNumber();
            String originalPhone = currentUser.getPersonalData().getPhoneNumber();

            if (currentPhone != null && originalPhone != null) {
                // Normalizar números para comparación (quitar espacios)
                String normalizedCurrent = currentPhone.replaceAll("\\s+", "");
                String normalizedOriginal = originalPhone.replaceAll("\\s+", "");

                if (!normalizedCurrent.equals(normalizedOriginal)) {
                    return true;
                }
            } else if (currentPhone != null || originalPhone != null) {
                // Uno es null y el otro no
                return true;
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