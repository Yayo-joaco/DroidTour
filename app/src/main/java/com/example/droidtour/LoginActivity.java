package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.managers.FileManager;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister;
    private MaterialTextView tvForgotPassword;

    // ==================== LOCAL STORAGE ====================
    private PreferencesManager prefsManager;
    private FileManager fileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeLocalStorage();
        initializeViews();
        setupClickListeners();

        // Verificar si ya hay una sesión activa (solo si no viene de un registro)
        boolean skipSessionCheck = getIntent().getBooleanExtra("skip_session_check", false);
        if (!skipSessionCheck) {
            checkExistingSession();
        }
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
        // 1. Primero verificar usuarios registrados en Local Storage
        JSONObject userData = fileManager.leerDatosUsuario();
        if (!userData.toString().equals("{}")) {
            try {
                String storedEmail = userData.getString("email");
                String userType = userData.getString("userType");

                if (email.equals(storedEmail)) {
                    // Usuario encontrado en Local Storage
                    String userId = userData.getString("id");
                    String fullName = userData.getString("fullName");

                    // Actualizar sesión
                    String phone = userData.optString("phone", "");
                    prefsManager.saveUserData(userId, fullName, email, phone, userType);

                    // Verificar estado del usuario
                    String status = userData.optString("status", "ACTIVE");
                    boolean approved = userData.optBoolean("approved", true);

                    if ("PENDING_APPROVAL".equals(status) || !approved) {
                        Toast.makeText(this, "Tu cuenta está pendiente de aprobación.\nRecibirás una notificación cuando sea activada.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Redirigir según tipo de usuario
                    Intent intent = getIntentForUserType(userType, fullName);
                    if (intent != null) {
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
            } catch (Exception e) {
                // Si hay error leyendo datos, continuar con autenticación mock
            }
        }

        // 2. Autenticación mock para usuarios predefinidos
        Intent intent = null;
        String userName = "";
        String userType = "";

        if (email.equals("superadmin@droidtour.com") && password.equals("admin123")) {
            intent = new Intent(this, SuperadminMainActivity.class);
            userName = "Superadministrador";
            userType = "SUPERADMIN";
        } else if (email.equals("admin@tours.com") && password.equals("admin123")) {
            intent = new Intent(this, TourAdminMainActivity.class);
            userName = "Administrador de Empresa";
            userType = "ADMIN";
        } else if (email.equals("guia@tours.com") && password.equals("guia123")) {
            intent = new Intent(this, TourGuideMainActivity.class);
            userName = "Guía de Turismo";
            userType = "GUIDE";
        } else if (email.equals("cliente@email.com") && password.equals("cliente123")) {
            intent = new Intent(this, ClientMainActivity.class);
            userName = "Cliente";
            userType = "CLIENT";
        } else {
            Toast.makeText(this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
            return;
        }

        if (intent != null) {
            // Guardar sesión para usuarios mock
            String mockId = userType + "_MOCK_" + System.currentTimeMillis();
            prefsManager.saveUserData(mockId, userName, email, "000000000", userType);

            Toast.makeText(this, "Bienvenido " + userName, Toast.LENGTH_SHORT).show();
            startActivity(intent);
            finish();
        }
    }

    // ==================== MÉTODOS DE LOCAL STORAGE ====================

    /**
     * Inicializar managers de local storage
     */
    private void initializeLocalStorage() {
        prefsManager = new PreferencesManager(this);
        fileManager = new FileManager(this);
    }

    /**
     * Verificar si ya existe una sesión activa
     */
    private void checkExistingSession() {
        if (prefsManager.isLoggedIn()) {
            String userType = prefsManager.getUserType();
            String userName = prefsManager.getUserName();

            Intent intent = getIntentForUserType(userType, userName);
            if (intent != null) {
                Toast.makeText(this, "Sesión activa detectada. Bienvenido de vuelta, " + userName, Toast.LENGTH_SHORT).show();
                startActivity(intent);
                finish();
            }
        }
    }

    /**
     * Obtener Intent según tipo de usuario
     */
    private Intent getIntentForUserType(String userType, String userName) {
        switch (userType) {
            case "SUPERADMIN":
                return new Intent(this, SuperadminMainActivity.class);
            case "ADMIN":
                return new Intent(this, TourAdminMainActivity.class);
            case "GUIDE":
                return new Intent(this, TourGuideMainActivity.class);
            case "CLIENT":
                return new Intent(this, ClientMainActivity.class);
            default:
                return null;
        }
    }
}
