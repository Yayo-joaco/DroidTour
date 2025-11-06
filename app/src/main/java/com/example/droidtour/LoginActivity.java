package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.client.ClientMainActivity;
import com.example.droidtour.managers.PrefsManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

public class LoginActivity extends AppCompatActivity {
    
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister;
    private TextView tvForgotPassword;
    private PrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Cambia el color de la barra de notificaciones al azul definido
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        prefsManager = new PrefsManager(this);

        //Redirección en caso se encuentre sesión activa
        if (prefsManager.sesionActiva() && !prefsManager.obtenerTipoUsuario().isEmpty()) {
            redirigirSegunRol();
            finish();
            return;
        }

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
            prefsManager.guardarUsuario("SUPERADMIN001", "Gabrielle Ivonne", email, "SUPERADMIN");
            Toast.makeText(this, "Bienvenido Superadministrador", Toast.LENGTH_SHORT).show();
            intent = new Intent(this, SuperadminMainActivity.class);

        } else if (email.equals("admin@tours.com") && password.equals("admin123")) {

            prefsManager.guardarUsuario("ADMIN001", "Laura Campos", email, "ADMIN");
            Toast.makeText(this, "Bienvenido Administrador de Empresa", Toast.LENGTH_SHORT).show();
            intent = new Intent(this, TourAdminMainActivity.class);

        } else if (email.equals("guia@tours.com") && password.equals("guia123")) {

            prefsManager.guardarUsuario("GUIDE001", "Carlos Mendoza", email, "GUIDE");
            Toast.makeText(this, "Bienvenido Guía de Turismo", Toast.LENGTH_SHORT).show();
            intent = new Intent(this, TourGuideMainActivity.class);

        } else if (email.equals("cliente@email.com") && password.equals("cliente123")) {

            prefsManager.guardarUsuario("CLIENT001", "Gabrielle Ivonne", email, "CLIENT");
            Toast.makeText(this, "Bienvenido Cliente", Toast.LENGTH_SHORT).show();
            intent = new Intent(this, ClientMainActivity.class);

        } else {
            Toast.makeText(this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
            return;
        }

        //Guardar hora del login
        prefsManager.guardarUltimoLogin(System.currentTimeMillis());

        //Actualizar que no es la primera vez
        prefsManager.marcarPrimeraVezCompletada();

        if (intent != null) {
            startActivity(intent);
            finish(); // Cerrar login para que no se pueda volver con back
        }
    }

    private void redirigirSegunRol(){
        String tipoUsuario = prefsManager.obtenerTipoUsuario();
        Intent intent = null;

        switch (tipoUsuario){
            case "SUPERADMIN":
                intent = new Intent(this, SuperadminMainActivity.class);
                break;
            case "ADMIN":
                intent = new Intent(this, TourAdminMainActivity.class);
                break;
            case "GUIDE":
                intent = new Intent(this, TourGuideMainActivity.class);
                break;
            case "CLIENT":
                intent = new Intent(this, ClientMainActivity.class);
                break;
            default:
                intent = new Intent(this, LoginActivity.class);
                break;
        }

        startActivity(intent);
    }
}
