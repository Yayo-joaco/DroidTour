package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputLayout tilEmail;
    private MaterialButton btnSendLink, btnCancel;
    private MaterialCardView cardSuccess;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configurar toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Recuperar Contraseña");
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        tilEmail = findViewById(R.id.til_email);
        btnSendLink = findViewById(R.id.btn_send_link);
        btnCancel = findViewById(R.id.btn_cancel);
        cardSuccess = findViewById(R.id.card_success);
    }

    private void setupClickListeners() {
        btnSendLink.setOnClickListener(v -> sendPasswordResetEmail());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void sendPasswordResetEmail() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();

        // Validaciones
        if (email.isEmpty()) {
            tilEmail.setError("Ingresa tu correo electrónico");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingresa un correo electrónico válido");
            return;
        }

        // Deshabilitar botones y mostrar progreso
        setButtonsEnabled(false);
        btnSendLink.setText("Enviando...");

        // DEBUG: Verificar email
        android.util.Log.d("PasswordReset", "Enviando reset para: " + email);

        // Firebase: Enviar email de recuperación
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // ÉXITO
                        android.util.Log.d("PasswordReset", "Email enviado exitosamente");
                        showSuccessMessage(email);
                    } else {
                        // ERROR - Restaurar botones
                        android.util.Log.e("PasswordReset", "Error: " + task.getException());
                        handleSendError(task.getException());
                    }
                });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnSendLink.setEnabled(enabled);
        btnCancel.setEnabled(enabled);

        // Cambiar apariencia visual del botón cancelar cuando está deshabilitado
        if (enabled) {
            btnCancel.setAlpha(1.0f);
            btnCancel.setText("Cancelar");
        } else {
            btnCancel.setAlpha(0.6f);
            btnCancel.setText("Espere...");
        }
    }

    private void showSuccessMessage(String email) {
        // Mostrar tarjeta de éxito y ocultar botón de enviar
        cardSuccess.setVisibility(android.view.View.VISIBLE);
        btnSendLink.setVisibility(android.view.View.GONE);

        // Restaurar botón cancelar pero cambiar su función
        setButtonsEnabled(true);
        btnCancel.setText("Volver al Login");
        btnCancel.setOnClickListener(v -> finish());

        // Personalizar mensaje de éxito
        android.widget.TextView tvSuccessMessage = findViewById(R.id.tv_success_message);
        tvSuccessMessage.setText(String.format(
                "Hemos enviado un enlace de recuperación a:\n%s",
                email
        ));

        Toast.makeText(this, "¡Enlace enviado! Revisa tu email.", Toast.LENGTH_LONG).show();

    }



    private void openEmailClient() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // Si no hay cliente de email, abrir Gmail
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://gmail.com"));
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Abre tu aplicación de email manualmente", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleSendError(Exception exception) {
        // Restaurar botones a estado normal
        setButtonsEnabled(true);
        btnSendLink.setText("Enviar Enlace de Recuperación");
        btnCancel.setText("Cancelar");
        btnCancel.setOnClickListener(v -> finish());

        String errorMessage = "Error al enviar el enlace de recuperación";

        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            android.util.Log.e("PasswordReset", "Error detallado: " + exceptionMessage);

            if (exceptionMessage.contains("user-not-found")) {
                errorMessage = "No existe una cuenta con este correo electrónico";
            } else if (exceptionMessage.contains("invalid-email")) {
                errorMessage = "El formato del correo electrónico es inválido";
            } else if (exceptionMessage.contains("network") || exceptionMessage.contains("INTERNET")) {
                errorMessage = "Error de conexión. Verifica tu internet";
            } else if (exceptionMessage.contains("too-many-requests")) {
                errorMessage = "Demasiados intentos. Espera unos minutos";
            } else {
                errorMessage = "Error: " + exceptionMessage;
            }
        }

        tilEmail.setError(errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
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