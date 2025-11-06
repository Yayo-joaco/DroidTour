package com.example.droidtour.client;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.UserPreferences;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

public class ClientSettingsActivity extends AppCompatActivity {

    private MaterialSwitch switchPushNotifications, switchEmailNotifications;
    private View settingChangePassword, settingDeleteAccount;
    private View settingPrivacyPolicy, settingTermsConditions;
    
    // ✅ Firebase Managers
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private String currentUserId;
    private UserPreferences userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_settings);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // ✅ Inicializar Firebase
        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        
        currentUserId = authManager.getCurrentUserId();
        
        // 🔥 TEMPORAL: Para testing sin login
        if (currentUserId == null) {
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            android.widget.Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", android.widget.Toast.LENGTH_SHORT).show();
        }

        setupToolbar();
        initializeViews();
        loadSettingsFromFirebase();
        setupClickListeners();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Configuración");
        }
    }

    private void initializeViews() {
        switchPushNotifications = findViewById(R.id.switch_push_notifications);
        switchEmailNotifications = findViewById(R.id.switch_email_notifications);
        
        settingChangePassword = findViewById(R.id.setting_change_password);
        settingDeleteAccount = findViewById(R.id.setting_delete_account);
        settingPrivacyPolicy = findViewById(R.id.setting_privacy_policy);
        settingTermsConditions = findViewById(R.id.setting_terms_conditions);
    }

    /**
     * ✅ CARGAR CONFIGURACIONES DESDE FIREBASE
     */
    private void loadSettingsFromFirebase() {
        firestoreManager.getUserPreferences(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                userPreferences = (UserPreferences) result;
                displaySettings();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClientSettingsActivity.this, 
                    "Error cargando configuración", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ✅ MOSTRAR CONFIGURACIONES EN UI
     */
    private void displaySettings() {
        switchPushNotifications.setChecked(userPreferences.getPushNotificationsEnabled());
        switchEmailNotifications.setChecked(userPreferences.getEmailNotificationsEnabled());
    }

    private void setupClickListeners() {
        // Account settings
        settingChangePassword.setOnClickListener(v -> {
            Toast.makeText(this, "Cambio de contraseña próximamente", Toast.LENGTH_SHORT).show();
        });

        settingDeleteAccount.setOnClickListener(v -> {
            Toast.makeText(this, "Eliminación de cuenta próximamente", Toast.LENGTH_SHORT).show();
        });

        // Privacy settings
        settingPrivacyPolicy.setOnClickListener(v -> {
            Toast.makeText(this, "Política de privacidad próximamente", Toast.LENGTH_SHORT).show();
        });

        settingTermsConditions.setOnClickListener(v -> {
            Toast.makeText(this, "Términos y condiciones próximamente", Toast.LENGTH_SHORT).show();
        });

        // ✅ Notification switches - Guardar en Firebase
        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userPreferences != null) {
                updatePreference("pushNotificationsEnabled", isChecked);
                Toast.makeText(this, "Notificaciones push " + 
                    (isChecked ? "activadas" : "desactivadas"), Toast.LENGTH_SHORT).show();
            }
        });

        switchEmailNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userPreferences != null) {
                updatePreference("emailNotificationsEnabled", isChecked);
                Toast.makeText(this, "Notificaciones por email " + 
                    (isChecked ? "activadas" : "desactivadas"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ✅ ACTUALIZAR PREFERENCIA EN FIREBASE
     */
    private void updatePreference(String key, Object value) {
        firestoreManager.updateUserPreferenceByUserId(currentUserId, key, value,
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    // Preferencia actualizada exitosamente
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(ClientSettingsActivity.this, 
                        "Error guardando configuración", Toast.LENGTH_SHORT).show();
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
}

