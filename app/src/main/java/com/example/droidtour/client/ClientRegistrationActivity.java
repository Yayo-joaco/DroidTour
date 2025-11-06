package com.example.droidtour.client;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.droidtour.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hbb20.CountryCodePicker;
import java.util.Calendar;

public class ClientRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etNombres, etApellidos, etNumeroDocumento,
            etFechaNacimiento, etCorreo;
    private AutoCompleteTextView etDocumento;  // Cambiado a AutoCompleteTextView
    private TextInputEditText etTelefono;
    private MaterialButton btnSiguiente;
    private CountryCodePicker ccp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_registration);
        //getWindow().setStatusBarColor(ContextCompat.getColor("#FF6200EE"));
        initializeViews();
        setupDocumentTypeSpinner();
        setupCountryCodePicker();
        setupClickListeners();
    }

    private void initializeViews() {
        etNombres = findViewById(R.id.etNombres);
        etApellidos = findViewById(R.id.etApellidos);
        etDocumento = findViewById(R.id.etDocumento);
        etNumeroDocumento = findViewById(R.id.etNumeroDocumento);
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento);
        etCorreo = findViewById(R.id.etCorreo);
        etTelefono = findViewById(R.id.etTelefono);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        ccp = findViewById(R.id.ccp);
        findViewById(R.id.tvRegresar).setOnClickListener(v -> finish());
    }


    private void setupDocumentTypeSpinner() {
        String[] documentTypes = {"DNI", "Carnet de Extranjería"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                documentTypes
        );
        etDocumento.setAdapter(adapter);

        // Opcional: establecer valor por defecto
        // etDocumento.setText("DNI", false);
    }

    private void setupCountryCodePicker() {
        ccp.registerCarrierNumberEditText(etTelefono);
        ccp.setDefaultCountryUsingNameCode("PE");
        ccp.resetToDefaultCountry();
    }

    private void setupClickListeners() {
        etFechaNacimiento.setOnClickListener(v -> showDatePicker());

        btnSiguiente.setOnClickListener(v -> {
            if (validateForm()) {
                proceedToNext();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
                    etFechaNacimiento.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private boolean validateForm() {
        if (etNombres.getText().toString().trim().isEmpty()) {
            etNombres.setError("Campo obligatorio");
            etNombres.requestFocus();
            return false;
        }

        if (etApellidos.getText().toString().trim().isEmpty()) {
            etApellidos.setError("Campo obligatorio");
            etApellidos.requestFocus();
            return false;
        }

        if (etDocumento.getText().toString().trim().isEmpty()) {
            etDocumento.setError("Seleccione tipo de documento");
            etDocumento.requestFocus();
            return false;
        }

        if (etNumeroDocumento.getText().toString().trim().isEmpty()) {
            etNumeroDocumento.setError("Campo obligatorio");
            etNumeroDocumento.requestFocus();
            return false;
        }

        if (etFechaNacimiento.getText().toString().trim().isEmpty()) {
            etFechaNacimiento.setError("Campo obligatorio");
            etFechaNacimiento.requestFocus();
            return false;
        }

        if (etCorreo.getText().toString().trim().isEmpty()) {
            etCorreo.setError("Campo obligatorio");
            etCorreo.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(etCorreo.getText().toString()).matches()) {
            etCorreo.setError("Correo electrónico inválido");
            etCorreo.requestFocus();
            return false;
        }

        if (etTelefono.getText().toString().trim().isEmpty()) {
            etTelefono.setError("Campo obligatorio");
            etTelefono.requestFocus();
            return false;
        }

        if (!ccp.isValidFullNumber()) {
            etTelefono.setError("Número de teléfono inválido");
            etTelefono.requestFocus();
            return false;
        }

        return true;
    }

    private void proceedToNext() {
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String tipoDocumento = etDocumento.getText().toString().trim();
        String numeroDocumento = etNumeroDocumento.getText().toString().trim();
        String fechaNacimiento = etFechaNacimiento.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String fullPhoneNumber = ccp.getFullNumberWithPlus();

        // Navegar a la actividad de foto y pasar los datos necesarios
        Intent intent = new Intent(this, ClientRegistrationPhotoActivity.class);
        intent.putExtra("nombres", nombres);
        intent.putExtra("apellidos", apellidos);
        intent.putExtra("correo", correo);
        intent.putExtra("tipoDocumento", tipoDocumento);
        intent.putExtra("numeroDocumento", numeroDocumento);
        intent.putExtra("fechaNacimiento", fechaNacimiento);
        intent.putExtra("telefono", fullPhoneNumber);
        startActivity(intent);
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