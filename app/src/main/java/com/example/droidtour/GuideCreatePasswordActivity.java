package com.example.droidtour;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.droidtour.models.User;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.utils.ImageUploadManager;
import com.google.firebase.storage.UploadTask;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class GuideCreatePasswordActivity extends AppCompatActivity {
    private static final String TAG = "GuideCreatePassword";

    private TextInputEditText etPassword, etRepeatPassword;
    private TextView tvPasswordError;
    private MaterialButton btnSiguiente;

    // Variables para datos del usuario
    private String nombres, apellidos, correo, tipoDocumento, numeroDocumento, fechaNacimiento, telefono;
    private List<String> idiomasSeleccionados;
    private String photoUri;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_create_password);

        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        loadUserData();
        setupTextWatchers();
        setupClickListeners();
    }

    private void initializeViews() {
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        findViewById(R.id.tvRegresar).setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        Intent intent = getIntent();
        nombres = intent.getStringExtra("nombres");
        apellidos = intent.getStringExtra("apellidos");
        correo = intent.getStringExtra("correo");
        tipoDocumento = intent.getStringExtra("tipoDocumento");
        numeroDocumento = intent.getStringExtra("numeroDocumento");
        fechaNacimiento = intent.getStringExtra("fechaNacimiento");
        telefono = intent.getStringExtra("telefono");
        photoUri = intent.getStringExtra("photoUri");

        // Obtener idiomas si se pasaron
        idiomasSeleccionados = intent.getStringArrayListExtra("idiomas");
        if (idiomasSeleccionados == null) {
            idiomasSeleccionados = new ArrayList<>();
        }
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswords();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etPassword.addTextChangedListener(watcher);
        etRepeatPassword.addTextChangedListener(watcher);
    }

    private void setupClickListeners() {
        btnSiguiente.setOnClickListener(v -> {
            if (validatePasswords()) {
                registerUser();
            }
        });
    }

    private boolean validatePasswords() {
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String repeat = etRepeatPassword.getText() != null ? etRepeatPassword.getText().toString() : "";
        String error = getPasswordError(password, repeat);

        if (error == null) {
            tvPasswordError.setVisibility(TextView.GONE);
            btnSiguiente.setEnabled(true);
            return true;
        } else {
            tvPasswordError.setText(error);
            tvPasswordError.setVisibility(TextView.VISIBLE);
            btnSiguiente.setEnabled(false);
            return false;
        }
    }

    private String getPasswordError(String password, String repeat) {
        if (password.isEmpty() || repeat.isEmpty()) {
            return "Debes ingresar ambas contraseñas.";
        }
        if (!password.equals(repeat)) {
            return "Las contraseñas no coinciden.";
        }
        if (password.length() < 8) {
            return "La contraseña debe tener al menos 8 caracteres.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Debe contener al menos una letra minúscula.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Debe contener al menos una letra mayúscula.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Debe contener al menos un número.";
        }
        if (!password.matches(".*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/|`~].*")) {
            return "Debe contener al menos un caracter especial.";
        }
        return null;
    }

    private void registerUser() {
        btnSiguiente.setEnabled(false);
        btnSiguiente.setText("Creando cuenta..."); // Feedback visual

        String password = etPassword.getText().toString().trim();

        // 1. CREAR USUARIO EN FIREBASE AUTH
        mAuth.createUserWithEmailAndPassword(correo, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 2. USUARIO CREADO EN AUTH - GUARDAR EN FIRESTORE
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user);
                        } else {
                            handleRegistrationError("Error al obtener usuario");
                        }
                    } else {
                        // MEJOR MANEJO DE ERRORES ESPECÍFICOS
                        String errorMessage = "Error en el registro";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage.contains("email address is already")) {
                                errorMessage = "Este correo electrónico ya está registrado";
                            } else if (exceptionMessage.contains("network error") || exceptionMessage.contains("INTERNET")) {
                                errorMessage = "Error de conexión. Verifica tu internet";
                            } else {
                                errorMessage = exceptionMessage;
                            }
                        }
                        handleRegistrationError(errorMessage);
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user) {
        String userId = user.getUid();
        String fullName = (nombres + " " + apellidos).trim();

        // 3. Crear objeto User usando el modelo con PersonalData
        User newUser = new User();
        newUser.setUserId(userId);
        newUser.setEmail(correo);
        newUser.setUserType("GUIDE");
        newUser.setStatus("pending"); // Estado pendiente de aprobación

        // Crear y configurar PersonalData
        User.PersonalData personalData = new User.PersonalData();
        personalData.setFirstName(nombres);
        personalData.setLastName(apellidos);
        personalData.setFullName(fullName);
        personalData.setDocumentType(tipoDocumento);
        personalData.setDocumentNumber(numeroDocumento);
        personalData.setDateOfBirth(fechaNacimiento);
        personalData.setPhoneNumber(telefono);

        newUser.setPersonalData(personalData);
        
        Log.d(TAG, "📸 Preparando guardado de guía. photoUri: " + (photoUri != null ? photoUri : "null"));

        // 4. SUBIR IMAGEN SI EXISTE Y LUEGO GUARDAR EN FIRESTORE
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        
        if (photoUri != null && !photoUri.isEmpty()) {
            // Convertir String a Uri
            Uri imageUri = Uri.parse(photoUri);
            
            // Subir imagen a Firebase Storage
            UploadTask uploadTask = ImageUploadManager.uploadFromUri(imageUri, "profile_images", userId);
            if (uploadTask == null) {
                handleRegistrationError("Error al iniciar la subida de imagen");
                return;
            }
            
            // Obtener URL de descarga después de subir
            ImageUploadManager.getDownloadUrl(uploadTask, new ImageUploadManager.ImageUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    Log.d(TAG, "✅ Imagen subida exitosamente. URL: " + downloadUrl);
                    
                    // Asegurar que personalData existe
                    if (newUser.getPersonalData() == null) {
                        Log.e(TAG, "❌ Error: personalData es null");
                        handleRegistrationError("Error: datos personales no inicializados");
                        return;
                    }
                    
                    // Actualizar PersonalData con la URL de descarga
                    newUser.getPersonalData().setProfileImageUrl(downloadUrl);
                    Log.d(TAG, "✅ URL de imagen asignada a personalData.profileImageUrl: " + newUser.getPersonalData().getProfileImageUrl());
                    
                    // Guardar usuario en Firestore
                    firestoreManager.createUser(newUser, new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            User savedUser = (User) result;
                            String savedImageUrl = savedUser.getPersonalData() != null ? 
                                    savedUser.getPersonalData().getProfileImageUrl() : null;
                            Log.d(TAG, "✅ Guía con imagen guardado exitosamente en Firestore");
                            Log.d(TAG, "✅ URL de imagen guardada en Firestore: " + savedImageUrl);
                            // 5. GUARDAR ROL DEL USUARIO con idiomas
                            saveUserRoleWithLanguages(userId);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "❌ Error al guardar guía en Firestore para userId: " + userId, e);
                            handleRegistrationError("Error al guardar datos: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e(TAG, "❌ Error al subir imagen: " + exception.getMessage(), exception);
                    handleRegistrationError("Error al subir imagen: " + exception.getMessage());
                }
            });
        } else {
            // No hay imagen, guardar directamente
            firestoreManager.createUser(newUser, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "Guía guardado exitosamente en Firestore");
                    // 5. GUARDAR ROL DEL USUARIO con idiomas
                    saveUserRoleWithLanguages(userId);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error al guardar guía en Firestore para userId: " + userId, e);
                    handleRegistrationError("Error al guardar datos: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Guardar el rol de guía con idiomas en user_roles
     */
    private void saveUserRoleWithLanguages(String userId) {
        FirestoreManager firestoreManager = FirestoreManager.getInstance();

        // Crear campos extra para el rol de guía
        Map<String, Object> extraFields = new HashMap<>();

        // Agregar idiomas si existen
        if (idiomasSeleccionados != null && !idiomasSeleccionados.isEmpty()) {
            extraFields.put("languages", idiomasSeleccionados);
        }

        // Agregar campos adicionales para guías
        extraFields.put("approved", false); // Requiere aprobación
        extraFields.put("rating", 0.0f); // Calificación inicial

        firestoreManager.saveUserRole(userId, "GUIDE", "pending", extraFields, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Rol de guía guardado exitosamente");
                // 6. CREAR DOCUMENTO EN COLECCIÓN GUIDES (opcional)
                saveGuideDocument(userId);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al guardar rol de guía para userId: " + userId, e);
                handleRegistrationError("Error al guardar rol: " + e.getMessage());
            }
        });
    }

    /**
     * Guardar documento en colección guides (opcional, para información adicional)
     */
    private void saveGuideDocument(String userId) {
        // Crear documento Guide usando el modelo Guide
        com.example.droidtour.models.Guide guide = new com.example.droidtour.models.Guide();
        guide.setGuideId(userId);
        guide.setLanguages(idiomasSeleccionados);
        guide.setApproved(false); // Pendiente de aprobación
        guide.setRating(0.0f); // Sin calificación aún

        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.upsertGuide(guide, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Documento de guía creado exitosamente");
                // 7. REGISTRO COMPLETADO - GUARDAR EN PREFERENCES
                saveToPreferencesManager(userId);

                Toast.makeText(GuideCreatePasswordActivity.this,
                        "¡Registro completado exitosamente!", Toast.LENGTH_SHORT).show();

                // 8. REDIRIGIR A PANTALLA DE ESPERA
                redirectToApprovalPending();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al crear documento de guía para userId: " + userId, e);
                // Continuar de todas formas, el documento Guide es opcional
                saveToPreferencesManager(userId);
                Toast.makeText(GuideCreatePasswordActivity.this,
                        "¡Registro completado exitosamente!", Toast.LENGTH_SHORT).show();
                redirectToApprovalPending();
            }
        });
    }

    private void saveToPreferencesManager(String userId) {
        com.example.droidtour.utils.PreferencesManager prefsManager =
                new com.example.droidtour.utils.PreferencesManager(this);

        String fullName = (nombres + " " + apellidos).trim();
        prefsManager.saveUserData(userId, fullName, correo, telefono, "GUIDE");
        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
        prefsManager.marcarPrimeraVezCompletada();
    }

    private void redirectToApprovalPending() {
        Intent intent = new Intent(this, GuideApprovalPendingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleRegistrationError(String errorMessage) {
        Log.e(TAG, "Error en registro: " + errorMessage);
        btnSiguiente.setEnabled(true);
        btnSiguiente.setText("Siguiente"); // Restaurar texto

        // Mostrar error en el TextView en lugar de solo Toast
        tvPasswordError.setText(errorMessage);
        tvPasswordError.setVisibility(TextView.VISIBLE);

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
}