package com.example.droidtour.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.firebase.FirebaseStorageManager;
import com.example.droidtour.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

public class ClientCreatePasswordActivity extends AppCompatActivity {
    
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private FirebaseStorageManager storageManager;
    
    private TextInputEditText etPassword, etRepeatPassword;
    private TextView tvPasswordError;
    private MaterialButton btnSiguiente;
    
    private String nombres, apellidos, tipoDocumento, numeroDocumento, fechaNacimiento, correo, telefono;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_create_password);
        
        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        storageManager = FirebaseStorageManager.getInstance();
        
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        // progressBar = findViewById(R.id.progressBar); // No existe en el layout
        findViewById(R.id.tvRegresar).setOnClickListener(v -> finish());
        
        loadUserDataFromIntent();

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

        btnSiguiente.setOnClickListener(v -> registerUser());
    }
    
    private void loadUserDataFromIntent() {
        Intent intent = getIntent();
        nombres = intent.getStringExtra("nombres");
        apellidos = intent.getStringExtra("apellidos");
        tipoDocumento = intent.getStringExtra("tipoDocumento");
        numeroDocumento = intent.getStringExtra("numeroDocumento");
        fechaNacimiento = intent.getStringExtra("fechaNacimiento");
        correo = intent.getStringExtra("correo");
        telefono = intent.getStringExtra("telefono");
        
        String photoUriString = intent.getStringExtra("photoUri");
        if (photoUriString != null) {
            photoUri = Uri.parse(photoUriString);
        }
    }
    
    private void registerUser() {
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        
        if (getPasswordError(password, password) != null) {
            Toast.makeText(this, "Por favor corrige los errores en la contraseña", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar progreso
        btnSiguiente.setText("Registrando...");
        btnSiguiente.setEnabled(false);
        
        // Crear objeto User primero
        User newUser = new User(
            "", // userId - se asignará después
            correo,
            nombres,
            apellidos,
            tipoDocumento,
            numeroDocumento,
            fechaNacimiento,
            telefono,
            "" // dirección
        );
        newUser.setUserType("CLIENT");
        newUser.setActive(true);
        
        // Registrar en Firebase Auth Y Firestore automáticamente
        authManager.registerWithEmail(correo, password, newUser, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser firebaseUser) {
                // Usuario registrado exitosamente
                String userId = firebaseUser.getUid();
                
                if (photoUri != null) {
                    uploadProfilePhoto(userId);
                } else {
                    completeRegistration();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                btnSiguiente.setText("Siguiente");
                btnSiguiente.setEnabled(true);
                Toast.makeText(ClientCreatePasswordActivity.this, 
                    "Error al registrar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void uploadProfilePhoto(String userId) {
        storageManager.uploadProfileImage(userId, photoUri, new FirebaseStorageManager.StorageCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                // Actualizar URL de foto en Firestore
                firestoreManager.updateUserPhotoUrl(userId, downloadUrl, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        completeRegistration();
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        // Foto no se pudo actualizar, pero usuario ya está registrado
                        completeRegistration();
                    }
                });
            }
            
            @Override
            public void onProgress(int progress) {
                // Progreso de subida
            }
            
            @Override
            public void onFailure(Exception e) {
                // Foto no se pudo subir, pero usuario ya está registrado
                completeRegistration();
            }
        });
    }
    
    private void completeRegistration() {
        Toast.makeText(this, "¡Registro exitoso! Bienvenido", Toast.LENGTH_SHORT).show();
        
        // Redirigir a ClientMainActivity
        Intent intent = new Intent(this, ClientMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void validatePasswords() {
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String repeat = etRepeatPassword.getText() != null ? etRepeatPassword.getText().toString() : "";
        String error = getPasswordError(password, repeat);
        if (error == null) {
            tvPasswordError.setVisibility(TextView.GONE);
            btnSiguiente.setEnabled(true);
        } else {
            tvPasswordError.setText(error);
            tvPasswordError.setVisibility(TextView.VISIBLE);
            btnSiguiente.setEnabled(false);
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
}
