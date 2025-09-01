package com.example.droidtour;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;

public class AdminRegistrationActivity extends AppCompatActivity {
    
    private TextInputEditText etFirstName, etLastName, etDocumentNumber, etBirthDate,
            etEmail, etPhone, etAddress, etPassword, etConfirmPassword,
            etCompanyName, etCompanyEmail, etCompanyPhone, etCompanyAddress;
    private AutoCompleteTextView spinnerDocumentType;
    private MaterialButton btnRegister, btnSelectPhoto, btnSelectCompanyPhotos;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_registration);

        initializeViews();
        setupToolbar();
        setupDocumentTypeSpinner();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        spinnerDocumentType = findViewById(R.id.spinner_document_type);
        etDocumentNumber = findViewById(R.id.et_document_number);
        etBirthDate = findViewById(R.id.et_birth_date);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etCompanyName = findViewById(R.id.et_company_name);
        etCompanyEmail = findViewById(R.id.et_company_email);
        etCompanyPhone = findViewById(R.id.et_company_phone);
        etCompanyAddress = findViewById(R.id.et_company_address);
        btnRegister = findViewById(R.id.btn_register);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);
        btnSelectCompanyPhotos = findViewById(R.id.btn_select_company_photos);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Registro de Administrador");
    }

    private void setupDocumentTypeSpinner() {
        String[] documentTypes = {"DNI", "Pasaporte", "Carnet de Extranjería"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, documentTypes);
        spinnerDocumentType.setAdapter(adapter);
    }

    private void setupClickListeners() {
        etBirthDate.setOnClickListener(v -> showDatePicker());
        
        btnSelectPhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad de selección de foto personal - En desarrollo", Toast.LENGTH_SHORT).show();
        });

        btnSelectCompanyPhotos.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad de selección de fotos promocionales (mín. 2) - En desarrollo", Toast.LENGTH_SHORT).show();
        });

        btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                registerAdmin();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                    etBirthDate.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private boolean validateForm() {
        // Validación datos personales
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("Campo obligatorio");
            return false;
        }
        if (etLastName.getText().toString().trim().isEmpty()) {
            etLastName.setError("Campo obligatorio");
            return false;
        }
        if (spinnerDocumentType.getText().toString().trim().isEmpty()) {
            spinnerDocumentType.setError("Seleccione tipo de documento");
            return false;
        }
        if (etDocumentNumber.getText().toString().trim().isEmpty()) {
            etDocumentNumber.setError("Campo obligatorio");
            return false;
        }
        if (etBirthDate.getText().toString().trim().isEmpty()) {
            etBirthDate.setError("Campo obligatorio");
            return false;
        }
        if (etEmail.getText().toString().trim().isEmpty()) {
            etEmail.setError("Campo obligatorio");
            return false;
        }
        if (etPhone.getText().toString().trim().isEmpty()) {
            etPhone.setError("Campo obligatorio");
            return false;
        }
        if (etAddress.getText().toString().trim().isEmpty()) {
            etAddress.setError("Campo obligatorio");
            return false;
        }
        if (etPassword.getText().toString().trim().isEmpty()) {
            etPassword.setError("Campo obligatorio");
            return false;
        }
        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            return false;
        }

        // Validación datos empresa
        if (etCompanyName.getText().toString().trim().isEmpty()) {
            etCompanyName.setError("Campo obligatorio");
            return false;
        }
        if (etCompanyEmail.getText().toString().trim().isEmpty()) {
            etCompanyEmail.setError("Campo obligatorio");
            return false;
        }
        if (etCompanyPhone.getText().toString().trim().isEmpty()) {
            etCompanyPhone.setError("Campo obligatorio");
            return false;
        }
        if (etCompanyAddress.getText().toString().trim().isEmpty()) {
            etCompanyAddress.setError("Campo obligatorio");
            return false;
        }

        return true;
    }

    private void registerAdmin() {
        // Mock registration - En producción esto se guardaría en la base de datos
        Toast.makeText(this, "Administrador y empresa registrados exitosamente.\nPendiente de aprobación por Superadmin.", Toast.LENGTH_LONG).show();
        
        // Redirigir al login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
