package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.droidtour.R;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.models.User;
import com.example.droidtour.models.UserSession;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.os.Build;
import android.provider.Settings;

public class ClientCreatePasswordActivity extends AppCompatActivity {
    private static final String TAG = "ClientCreatePassword";

    private TextInputEditText etPassword, etRepeatPassword;
    private TextView tvPasswordError;
    private MaterialButton btnSiguiente;

    // Variables para datos del usuario
    private String nombres, apellidos, correo, tipoDocumento, numeroDocumento, fechaNacimiento, telefono;
    private String photoUri;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_create_password);

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
        btnSiguiente.setText("Creando cuenta...");

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

    private void handleRegistrationError(String errorMessage) {
        btnSiguiente.setEnabled(true);
        btnSiguiente.setText("Siguiente");

        tvPasswordError.setText(errorMessage);
        tvPasswordError.setVisibility(TextView.VISIBLE);

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void saveUserToFirestore(FirebaseUser user) {
        String userId = user.getUid();

        // 3. Crear objeto User usando el modelo CORREGIDO
        User newUser = new User();
        newUser.setUserId(userId);
        newUser.setEmail(correo);
        newUser.setUserType("CLIENT");
        newUser.setStatus("active");

        // Crear PersonalData
        User.PersonalData personalData = new User.PersonalData(
                nombres,
                apellidos,
                tipoDocumento,
                numeroDocumento,
                fechaNacimiento,
                telefono
        );

        // Agregar foto si existe
        if (photoUri != null && !photoUri.isEmpty()) {
            personalData.setProfileImageUrl(photoUri);
        }

        newUser.setPersonalData(personalData);

        // 4. GUARDAR EN FIRESTORE usando FirestoreManager (DESCOMENTADO)
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.upsertUser(newUser, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Usuario guardado exitosamente en Firestore");
                // 5. GUARDAR EN PREFERENCES
                saveToPreferencesManager(userId);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al guardar usuario en Firestore: " + e.getMessage());
                handleRegistrationError("Error al guardar datos: " + e.getMessage());
            }
        });
    }

    private void saveToPreferencesManager(String userId) {
        PreferencesManager prefsManager = new PreferencesManager(this);

        String fullName = nombres + " " + apellidos;
        prefsManager.saveUserData(userId, fullName, correo, telefono, "CLIENT");
        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
        prefsManager.marcarPrimeraVezCompletada();

        // Guardar sesión en Firestore
        saveSessionToFirestore(userId, correo, fullName);

        // Redirigir al dashboard
        Toast.makeText(this, "¡Registro completado exitosamente!", Toast.LENGTH_SHORT).show();
        redirectToMainActivity();
    }

    private void saveSessionToFirestore(String userId, String email, String name) {
        // Obtener información del dispositivo
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        String deviceOS = "Android " + Build.VERSION.RELEASE;
        String appVersion = "1.0.0";

        // Crear sesión
        UserSession session = new UserSession(userId, email, name, "CLIENT",
                deviceId, deviceModel, deviceOS, appVersion);

        // Guardar en Firestore
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.createUserSession(session, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                String sessionId = result instanceof UserSession ?
                        ((UserSession) result).getSessionId() : "unknown";

                PreferencesManager prefs = new PreferencesManager(ClientCreatePasswordActivity.this);
                prefs.guardar("session_id", sessionId);
                Log.d(TAG, "Sesión guardada en Firestore: " + sessionId);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error guardando sesión en Firestore: " + e.getMessage());
                // No bloquear el flujo si falla
            }
        });
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(this, ClientMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}