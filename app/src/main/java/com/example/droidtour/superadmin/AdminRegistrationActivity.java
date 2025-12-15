package com.example.droidtour.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.hbb20.CountryCodePicker;
import android.widget.AutoCompleteTextView;
import com.example.droidtour.R;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.firebase.auth.UserProfileChangeRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AdminRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etBusinessName, etRuc, etCommercialName;
    private TextInputEditText etAdminFirstName, etAdminLastName, etAdminEmail;
    private TextInputEditText etAdminPhone, etAdminDocNumber;
    private TextInputEditText etAdminPassword, etAdminConfirmPassword;
    private AutoCompleteTextView actBusinessType, actAdminDocType;
    private CountryCodePicker ccpAdmin;
    private ExtendedFloatingActionButton btnRegisterAdmin;

    private FirebaseAuth mAuth;
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;

    // Patterns para validación
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_registration);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Configurar compatibilidad con ventana
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Validar permisos del usuario actual
        prefsManager = new PreferencesManager(this);
        if (!prefsManager.isLoggedIn() || !"SUPERADMIN".equals(prefsManager.getUserType())) {
            Toast.makeText(this, "Acceso denegado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        firestoreManager = FirestoreManager.getInstance();

        // Configurar Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Inicializar vistas
        initViews();

        // Configurar dropdowns
        setupDropdowns();

        // Configurar filtros de entrada
        setupNameFilters();
        setupDocumentNumberFilter();

        // Configurar listener del botón de registro
        setupPhoneFormatter();
        setupRegisterButton();
    }
    
    /**
     * Configura filtros de entrada para limitar nombres y apellidos a 40 caracteres
     */
    private void setupNameFilters() {
        android.text.InputFilter[] nameFilters = new android.text.InputFilter[]{
            new android.text.InputFilter.LengthFilter(40)
        };
        etAdminFirstName.setFilters(nameFilters);
        etAdminLastName.setFilters(nameFilters);
    }
    
    /**
     * Configura filtro de entrada para DNI (máximo 8 dígitos numéricos)
     */
    private void setupDocumentNumberFilter() {
        // Escuchar cambios en el tipo de documento para aplicar filtro dinámicamente
        actAdminDocType.setOnItemClickListener((parent, view, position, id) -> {
            updateDocumentNumberFilter();
        });
        
        // Aplicar filtro inicial
        updateDocumentNumberFilter();
        
        // TextWatcher adicional para limpiar caracteres no numéricos en tiempo real
        etAdminDocNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String tipoDocumento = actAdminDocType.getText().toString().trim();
                if ("DNI".equals(tipoDocumento)) {
                    // Solo permitir números y máximo 8 dígitos para DNI
                    String text = s.toString().replaceAll("[^0-9]", "");
                    if (text.length() > 8) {
                        text = text.substring(0, 8);
                    }
                    if (!s.toString().equals(text)) {
                        int cursorPosition = etAdminDocNumber.getSelectionStart();
                        s.clear();
                        s.append(text);
                        int newPosition = Math.min(cursorPosition, text.length());
                        etAdminDocNumber.setSelection(newPosition);
                    }
                }
            }
        });
    }
    
    /**
     * Actualiza el filtro del campo número de documento según el tipo seleccionado
     */
    private void updateDocumentNumberFilter() {
        String tipoDocumento = actAdminDocType.getText().toString().trim();
        if ("DNI".equals(tipoDocumento)) {
            // Aplicar filtro: solo números, máximo 8 dígitos
            etAdminDocNumber.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(8),
                (source, start, end, dest, dstart, dend) -> {
                    // Solo permitir dígitos
                    for (int i = start; i < end; i++) {
                        if (!Character.isDigit(source.charAt(i))) {
                            return "";
                        }
                    }
                    return null; // Aceptar el texto
                }
            });
        } else {
            // Remover restricciones para otros tipos de documento
            etAdminDocNumber.setFilters(new android.text.InputFilter[0]);
        }
    }

    private void initViews() {
        // Campos de empresa
        etBusinessName = findViewById(R.id.et_business_name);
        etRuc = findViewById(R.id.et_ruc);
        etCommercialName = findViewById(R.id.et_commercial_name);
        actBusinessType = findViewById(R.id.act_business_type);

        // Campos del administrador
        etAdminFirstName = findViewById(R.id.et_admin_first_name);
        etAdminLastName = findViewById(R.id.et_admin_last_name);
        etAdminEmail = findViewById(R.id.et_admin_email);
        etAdminPhone = findViewById(R.id.et_admin_phone);
        etAdminDocNumber = findViewById(R.id.et_admin_doc_number);
        actAdminDocType = findViewById(R.id.act_admin_doc_type);
        ccpAdmin = findViewById(R.id.ccp_admin);

        // Campos de contraseña
        etAdminPassword = findViewById(R.id.et_admin_password);
        etAdminConfirmPassword = findViewById(R.id.et_admin_confirm_password);

        // Botón de registro
        btnRegisterAdmin = findViewById(R.id.btn_register_admin);
    }

    private void setupDropdowns() {
        // Tipos de persona jurídica
        String[] businessTypes = {"S.A.C.", "S.A.", "E.I.R.L.", "S.R.L.", "S.C.", "Asociación", "Fundación"};
        ArrayAdapter<String> businessAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, businessTypes
        );
        actBusinessType.setAdapter(businessAdapter);
        actBusinessType.setText(businessTypes[0], false); // Valor por defecto

        // Tipos de documento
        String[] docTypes = {"DNI", "Carné de Extranjería", "Pasaporte", "RUC"};
        ArrayAdapter<String> docAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, docTypes
        );
        actAdminDocType.setAdapter(docAdapter);
        actAdminDocType.setText(docTypes[0], false); // Valor por defecto DNI
    }

    private void setupRegisterButton() {
        btnRegisterAdmin.setOnClickListener(v -> {
            if (validateForm()) {
                registerAdmin();
            }
        });
    }

    private void setupPhoneFormatter() {
        ccpAdmin.registerCarrierNumberEditText(etAdminPhone);
        etAdminPhone.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) return;

                isFormatting = true;
                String text = s.toString().replaceAll("\\s+", ""); // Remover espacios
                
                // Solo permitir dígitos
                text = text.replaceAll("[^0-9]", "");
                
                // Formatear con espacios cada 3 dígitos
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < text.length(); i++) {
                    if (i > 0 && i % 3 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(text.charAt(i));
                }
                
                // Actualizar el texto
                int cursorPosition = etAdminPhone.getSelectionStart();
                int lengthBefore = s.length();
                s.clear();
                s.append(formatted.toString());
                
                // Ajustar la posición del cursor
                int lengthAfter = formatted.length();
                int cursorOffset = lengthAfter - lengthBefore;
                int newCursorPosition = Math.max(0, Math.min(formatted.length(), cursorPosition + cursorOffset));
                etAdminPhone.setSelection(newCursorPosition);
                
                isFormatting = false;
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validar empresa
        if (TextUtils.isEmpty(etBusinessName.getText())) {
            etBusinessName.setError("La razón social es obligatoria");
            isValid = false;
        }

        if (TextUtils.isEmpty(etRuc.getText()) || etRuc.getText().length() != 11) {
            etRuc.setError("RUC debe tener 11 dígitos");
            isValid = false;
        }

        // Validar datos del administrador
        String firstName = etAdminFirstName.getText().toString().trim();
        if (TextUtils.isEmpty(firstName)) {
            etAdminFirstName.setError("Nombres son obligatorios");
            isValid = false;
        } else if (firstName.length() > 40) {
            etAdminFirstName.setError("Los nombres no pueden exceder 40 caracteres");
            isValid = false;
        }

        String lastName = etAdminLastName.getText().toString().trim();
        if (TextUtils.isEmpty(lastName)) {
            etAdminLastName.setError("Apellidos son obligatorios");
            isValid = false;
        } else if (lastName.length() > 40) {
            etAdminLastName.setError("Los apellidos no pueden exceder 40 caracteres");
            isValid = false;
        }

        String email = etAdminEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            etAdminEmail.setError("Correo corporativo inválido");
            isValid = false;
        }

        String tipoDocumento = actAdminDocType.getText().toString().trim();
        String numeroDocumento = etAdminDocNumber.getText().toString().trim();
        
        if (TextUtils.isEmpty(numeroDocumento)) {
            etAdminDocNumber.setError("Número de documento es obligatorio");
            isValid = false;
        } else if ("DNI".equals(tipoDocumento)) {
            // Validar DNI: solo números, exactamente 8 cifras
            String numeroSinEspacios = numeroDocumento.replaceAll("\\s+", "");
            if (!numeroSinEspacios.matches("\\d{8}")) {
                etAdminDocNumber.setError("El DNI debe tener exactamente 8 dígitos numéricos");
                isValid = false;
            }
        }

        // Validar teléfono (si está presente)
        String phone = etAdminPhone.getText().toString().trim();
        if (!TextUtils.isEmpty(phone)) {
            String telefonoSinEspacios = phone.replaceAll("\\s+", "");
            if (telefonoSinEspacios.length() != 9) {
                etAdminPhone.setError("El número de teléfono debe tener 9 dígitos");
                isValid = false;
            } else if (!telefonoSinEspacios.matches("\\d{9}")) {
                etAdminPhone.setError("El número de teléfono solo debe contener dígitos");
                isValid = false;
            }
        }

        // Validar contraseñas
        String password = etAdminPassword.getText().toString();
        String confirmPassword = etAdminConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(password) || !PASSWORD_PATTERN.matcher(password).matches()) {
            etAdminPassword.setError("La contraseña debe tener al menos 8 caracteres, incluir mayúsculas, minúsculas, números y un carácter especial");
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            etAdminConfirmPassword.setError("Las contraseñas no coinciden");
            isValid = false;
        }

        return isValid;
    }

    private void registerAdmin() {
        btnRegisterAdmin.setEnabled(false);
        btnRegisterAdmin.setText("Registrando...");

        String email = etAdminEmail.getText().toString().trim();
        String password = etAdminPassword.getText().toString();
        String firstName = etAdminFirstName.getText().toString().trim();
        String lastName = etAdminLastName.getText().toString().trim();
        String fullName = firstName + " " + lastName;
        String phone = ccpAdmin.getSelectedCountryCodeWithPlus() + etAdminPhone.getText().toString();
        String docType = actAdminDocType.getText().toString();
        String docNumber = etAdminDocNumber.getText().toString();

        // Datos de empresa
        String businessName = etBusinessName.getText().toString().trim();
        String ruc = etRuc.getText().toString().trim();
        String commercialName = etCommercialName.getText().toString().trim();
        String businessType = actBusinessType.getText().toString();

        // 1) Crear usuario en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(authTask -> {
                    if (!authTask.isSuccessful()) {
                        handleRegistrationError("Error al crear usuario: " + (authTask.getException() != null ? authTask.getException().getMessage() : "desconocido"));
                        return;
                    }

                    String userId = authTask.getResult().getUser().getUid();

                    // Actualizar displayName en Firebase Auth (opcional)
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();
                    authTask.getResult().getUser().updateProfile(profileUpdates)
                            .addOnFailureListener(e -> Log.w("AdminRegistration", "No se pudo actualizar displayName: " + e.getMessage()));

                    // 2) Crear Company en Firestore y asociarla al admin
                    // NOTA: Los campos email, phone, address y description de la EMPRESA
                    // se dejan como null. El admin los llenará después en CompanyInfoActivity.
                    // Los datos del admin (email, phone) son distintos a los de la empresa.
                    com.example.droidtour.models.Company company = new com.example.droidtour.models.Company();
                    company.setBusinessName(businessName);
                    company.setRuc(ruc);
                    company.setCommercialName(commercialName);
                    company.setBusinessType(businessType);
                    company.setAdminUserId(userId);
                    company.setEmail(null);         // Email de la empresa (diferente al del admin)
                    company.setPhone(null);         // Teléfono de la empresa (diferente al del admin)
                    company.setAddress(null);       // Dirección de la empresa
                    company.setDescription(null);   // Descripción de la empresa

                    firestoreManager.createCompany(company, new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            com.example.droidtour.models.Company createdCompany = (com.example.droidtour.models.Company) result;
                            String companyId = createdCompany != null ? createdCompany.getCompanyId() : null;

                            // 3) Crear User (modelo nuevo) con personalData y companyId
                            User newUser = new User();
                            newUser.setUserId(userId);
                            newUser.setEmail(email);
                            newUser.setUserType("COMPANY_ADMIN");
                            newUser.setStatus("active");
                            if (companyId != null) newUser.setCompanyId(companyId);

                            User.PersonalData pd = new User.PersonalData();
                            pd.setFirstName(firstName);
                            pd.setLastName(lastName);
                            pd.setFullName(fullName);
                            pd.setDocumentType(docType);
                            pd.setDocumentNumber(docNumber);
                            pd.setPhoneNumber(phone);
                            newUser.setPersonalData(pd);

                            // 4) Guardar User en Firestore
                            firestoreManager.upsertUser(newUser, new FirestoreManager.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    Log.d("AdminRegistration", "Administrador guardado exitosamente en Firestore");

                                    // 5) Guardar rol en user_roles
                                    Map<String, Object> adminAdditionalFields = new HashMap<>();
                                    adminAdditionalFields.put("assignedAt", new java.util.Date());
                                    adminAdditionalFields.put("assignedBy", prefsManager.getUserId());
                                    adminAdditionalFields.put("company", businessName);
                                    adminAdditionalFields.put("companyRuc", ruc);

                                    firestoreManager.saveUserRole(userId, "COMPANY_ADMIN", "active", adminAdditionalFields, new FirestoreManager.FirestoreCallback() {
                                        @Override
                                        public void onSuccess(Object result) {
                                            Log.d("AdminRegistration", "Rol de administrador guardado exitosamente");
                                            Toast.makeText(AdminRegistrationActivity.this,
                                                    "Administrador registrado exitosamente", Toast.LENGTH_LONG).show();

                                            // Enviar email de bienvenida (opcional)
                                            sendWelcomeEmail(email, fullName, password);

                                            // Regresar a la lista de usuarios
                                            Intent intent = new Intent();
                                            intent.putExtra("newAdminRegistered", true);
                                            setResult(RESULT_OK, intent);
                                            finish();
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e("AdminRegistration", "Error al guardar rol de administrador para userId: " + userId, e);
                                            handleRegistrationError("Error al asignar rol: " + e.getMessage());
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e("AdminRegistration", "Error al guardar administrador en Firestore para userId: " + userId, e);
                                    handleRegistrationError("Error al guardar datos: " + e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("AdminRegistration", "Error al crear company en Firestore: ", e);
                            handleRegistrationError("Error al crear empresa: " + e.getMessage());
                        }
                    });
                });
    }

    private void handleRegistrationError(String errorMessage) {
        Log.e("AdminRegistration", errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        btnRegisterAdmin.setEnabled(true);
        btnRegisterAdmin.setText("Registrar");
    }

    private void sendWelcomeEmail(String email, String fullName, String tempPassword) {
        // Aquí puedes implementar el envío de email usando Firebase Functions o un servicio
        // Por ahora solo mostraremos un mensaje
        String message = String.format(
                "Credenciales de acceso:\n\nEmail: %s\nContraseña temporal: %s\n\nPor favor cambie su contraseña al primer inicio de sesión.",
                email, tempPassword
        );

        // Esto es solo para demostración. En producción, usa Firebase Functions
        Log.i("AdminRegistration", "Email de bienvenida para " + email + ": " + message);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}