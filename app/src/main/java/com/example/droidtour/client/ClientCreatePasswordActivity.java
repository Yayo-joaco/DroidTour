package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.droidtour.R;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ClientCreatePasswordActivity extends AppCompatActivity {
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

    private void handleRegistrationError(String errorMessage) {
        btnSiguiente.setEnabled(true);
        btnSiguiente.setText("Siguiente"); // Restaurar texto

        // Mostrar error en el TextView en lugar de solo Toast
        tvPasswordError.setText(errorMessage);
        tvPasswordError.setVisibility(TextView.VISIBLE);

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void saveUserToFirestore(FirebaseUser user) {
        String userId = user.getUid();
        String fullName = nombres + " " + apellidos;

        // 3. PREPARAR DATOS PARA FIRESTORE
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userId);
        userData.put("email", correo);
        userData.put("firstName", nombres);
        userData.put("lastName", apellidos);
        userData.put("fullName", fullName);
        userData.put("documentType", tipoDocumento);
        userData.put("documentNumber", numeroDocumento);
        userData.put("birthDate", fechaNacimiento);
        userData.put("phone", telefono);
        userData.put("userType", "CLIENT");
        userData.put("provider", "email");
        userData.put("status", "active");
        userData.put("createdAt", new java.util.Date());
        userData.put("profileCompleted", true);
        userData.put("profileCompletedAt", new java.util.Date());

        // Agregar foto si existe
        if (photoUri != null && !photoUri.isEmpty()) {
            userData.put("photoURL", photoUri);
            userData.put("customPhoto", true);
        }

        // 4. GUARDAR EN COLECCIÓN "users"
        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // 5. GUARDAR ROL DEL USUARIO (CLIENTE ACTIVO DIRECTAMENTE)
                    saveUserRole(userId);
                })
                .addOnFailureListener(e -> {
                    handleRegistrationError("Error al guardar datos: " + e.getMessage());
                });
    }

    private void saveUserRole(String userId) {
        Map<String, Object> roleData = new HashMap<>();
        roleData.put("status", "active");
        roleData.put("activatedAt", new java.util.Date());
        roleData.put("updatedAt", new java.util.Date());

        Map<String, Object> roleUpdate = new HashMap<>();
        roleUpdate.put("client", roleData);

        FirebaseFirestore.getInstance().collection("user_roles")
                .document(userId)
                .set(roleUpdate)
                .addOnSuccessListener(aVoid -> {
                    // 6. REGISTRO COMPLETADO - GUARDAR EN PREFERENCES
                    saveToPreferencesManager(userId);

                    Toast.makeText(this, "¡Registro completado exitosamente!", Toast.LENGTH_SHORT).show();

                    // 7. REDIRIGIR AL DASHBOARD DEL CLIENTE
                    redirectToMainActivity();
                })
                .addOnFailureListener(e -> {
                    handleRegistrationError("Error al guardar rol: " + e.getMessage());
                });
    }

    private void saveToPreferencesManager(String userId) {
        PreferencesManager prefsManager = new PreferencesManager(this);

        String fullName = nombres + " " + apellidos;
        prefsManager.saveUserData(userId, fullName, correo, telefono, "CLIENT");
        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
        prefsManager.marcarPrimeraVezCompletada();
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(this, ClientMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}