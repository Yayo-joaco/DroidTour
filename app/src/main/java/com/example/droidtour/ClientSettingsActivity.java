package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

public class ClientSettingsActivity extends AppCompatActivity {

    private MaterialSwitch switchPushNotifications, switchEmailNotifications;
    private View settingChangePassword, settingDeleteAccount;
    private View settingPrivacyPolicy, settingTermsConditions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_settings);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        setupToolbar();
        initializeViews();
        setupClickListeners();
        loadSettings();
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

        // Notification switches
        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Guardar configuración de notificaciones push
            Toast.makeText(this, "Notificaciones push " + (isChecked ? "activadas" : "desactivadas"), Toast.LENGTH_SHORT).show();
        });

        switchEmailNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Guardar configuración de notificaciones por email
            Toast.makeText(this, "Notificaciones por email " + (isChecked ? "activadas" : "desactivadas"), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSettings() {
        // TODO: Cargar configuraciones guardadas del usuario
        switchPushNotifications.setChecked(true);
        switchEmailNotifications.setChecked(false);
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

