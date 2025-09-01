package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

public class LoginActivity extends AppCompatActivity {
    
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister;
    private MaterialTextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simulación de autenticación con datos mock
            authenticateUser(email, password);
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RoleSelectionActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void authenticateUser(String email, String password) {
        // Mock authentication - En producción esto se haría contra la base de datos
        Intent intent = null;
        
        if (email.equals("superadmin@droidtour.com") && password.equals("admin123")) {
            intent = new Intent(this, SuperadminMainActivity.class);
            Toast.makeText(this, "Bienvenido Superadministrador", Toast.LENGTH_SHORT).show();
        } else if (email.equals("admin@tours.com") && password.equals("admin123")) {
            intent = new Intent(this, TourAdminMainActivity.class);
            Toast.makeText(this, "Bienvenido Administrador de Empresa", Toast.LENGTH_SHORT).show();
        } else if (email.equals("guia@tours.com") && password.equals("guia123")) {
            intent = new Intent(this, TourGuideMainActivity.class);
            Toast.makeText(this, "Bienvenido Guía de Turismo", Toast.LENGTH_SHORT).show();
        } else if (email.equals("cliente@email.com") && password.equals("cliente123")) {
            intent = new Intent(this, ClientMainActivity.class);
            Toast.makeText(this, "Bienvenido Cliente", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
            return;
        }

        if (intent != null) {
            startActivity(intent);
            finish(); // Cerrar login para que no se pueda volver con back
        }
    }
}
