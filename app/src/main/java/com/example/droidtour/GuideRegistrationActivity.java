package com.example.droidtour;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.droidtour.client.ClientRegistrationPhotoActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.managers.FileManager;
import com.example.droidtour.utils.NavigationUtils;
import org.json.JSONObject;
import org.json.JSONException;
import com.google.android.material.button.MaterialButton;
import com.hbb20.CountryCodePicker;

import java.util.Calendar;

public class GuideRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etNombres, etApellidos, etNumeroDocumento,
            etFechaNacimiento, etCorreo;
    private AutoCompleteTextView etDocumento;
    private TextInputEditText etTelefono;
    private MaterialButton btnSiguiente;
    private CountryCodePicker ccp;
    private boolean isGoogleUserFlow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_registration);
        //getWindow().setStatusBarColor(ContextCompat.getColor("#FF6200EE"));
        initializeViews();

        // Manejar datos de Google si vienen en el intent
        handleGoogleUserData();

        setupDocumentTypeSpinner();
        setupCountryCodePicker();
        setupNameFilters();
        setupClickListeners();
    }
    
    /**
     * Configura filtros de entrada para limitar nombres y apellidos a 40 caracteres
     */
    private void setupNameFilters() {
        android.text.InputFilter[] nameFilters = new android.text.InputFilter[]{
            new android.text.InputFilter.LengthFilter(40)
        };
        etNombres.setFilters(nameFilters);
        etApellidos.setFilters(nameFilters);
    }

    // Nuevo: rellenar campos si venimos desde Google Sign-In
    private void handleGoogleUserData() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("googleUser", false)) {
            isGoogleUserFlow = true;
            String userEmail = extras.getString("userEmail", "");
            String userName = extras.getString("userName", "");

            if (userEmail != null && !userEmail.isEmpty()) {
                etCorreo.setText(userEmail);
                etCorreo.setEnabled(false); // hacerlo de solo lectura
            }

            if (userName != null && !userName.isEmpty()) {
                String[] parts = userName.split(" ", 2);
                if (parts.length >= 1) etNombres.setText(parts[0]);
                if (parts.length == 2) etApellidos.setText(parts[1]);
            }
        }
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
        findViewById(R.id.tvRegresar).setOnClickListener(v -> handleBackNavigation());
    }


    private void setupDocumentTypeSpinner() {
        String[] documentTypes = {"DNI", "Carnet de Extranjería"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                documentTypes
        );
        etDocumento.setAdapter(adapter);
        
        // Escuchar cambios en el tipo de documento para aplicar filtro dinámicamente
        etDocumento.setOnItemClickListener((parent, view, position, id) -> {
            updateDocumentNumberFilter();
        });
        
        // Configurar filtro de entrada para DNI
        setupDocumentNumberFilter();

        // Opcional: establecer valor por defecto
        // etDocumento.setText("DNI", false);
    }
    
    /**
     * Actualiza el filtro del campo número de documento según el tipo seleccionado
     */
    private void updateDocumentNumberFilter() {
        String tipoDocumento = etDocumento.getText().toString().trim();
        if ("DNI".equals(tipoDocumento)) {
            // Aplicar filtro: solo números, máximo 8 dígitos
            etNumeroDocumento.setFilters(new android.text.InputFilter[]{
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
            etNumeroDocumento.setFilters(new android.text.InputFilter[0]);
        }
    }
    
    private void setupDocumentNumberFilter() {
        // Aplicar filtro inicial si el tipo de documento es DNI
        updateDocumentNumberFilter();
        
        // TextWatcher adicional para limpiar caracteres no numéricos en tiempo real
        etNumeroDocumento.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String tipoDocumento = etDocumento.getText().toString().trim();
                if ("DNI".equals(tipoDocumento)) {
                    // Solo permitir números y máximo 8 dígitos para DNI
                    String text = s.toString().replaceAll("[^0-9]", "");
                    if (text.length() > 8) {
                        text = text.substring(0, 8);
                    }
                    if (!s.toString().equals(text)) {
                        int cursorPosition = etNumeroDocumento.getSelectionStart();
                        s.clear();
                        s.append(text);
                        int newPosition = Math.min(cursorPosition, text.length());
                        etNumeroDocumento.setSelection(newPosition);
                    }
                }
            }
        });
    }

    private void setupCountryCodePicker() {
        ccp.registerCarrierNumberEditText(etTelefono);
        ccp.setDefaultCountryUsingNameCode("PE");
        ccp.resetToDefaultCountry();
        setupPhoneFormatter();
    }

    private void setupPhoneFormatter() {
        etTelefono.addTextChangedListener(new android.text.TextWatcher() {
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
                int cursorPosition = etTelefono.getSelectionStart();
                int lengthBefore = s.length();
                s.clear();
                s.append(formatted.toString());
                
                // Ajustar la posición del cursor
                int lengthAfter = formatted.length();
                int cursorOffset = lengthAfter - lengthBefore;
                int newCursorPosition = Math.max(0, Math.min(formatted.length(), cursorPosition + cursorOffset));
                etTelefono.setSelection(newCursorPosition);
                
                isFormatting = false;
            }
        });
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
                    // Validar que la fecha no sea futura
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);
                    
                    if (selectedDate.after(today)) {
                        Toast.makeText(this, "La fecha de nacimiento no puede ser futura", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String date = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
                    etFechaNacimiento.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        // Limitar la fecha máxima a hoy
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private boolean validateForm() {
        String nombres = etNombres.getText().toString().trim();
        if (nombres.isEmpty()) {
            etNombres.setError("Campo obligatorio");
            etNombres.requestFocus();
            return false;
        }
        if (nombres.length() > 40) {
            etNombres.setError("Los nombres no pueden exceder 40 caracteres");
            etNombres.requestFocus();
            return false;
        }

        String apellidos = etApellidos.getText().toString().trim();
        if (apellidos.isEmpty()) {
            etApellidos.setError("Campo obligatorio");
            etApellidos.requestFocus();
            return false;
        }
        if (apellidos.length() > 40) {
            etApellidos.setError("Los apellidos no pueden exceder 40 caracteres");
            etApellidos.requestFocus();
            return false;
        }

        if (etDocumento.getText().toString().trim().isEmpty()) {
            etDocumento.setError("Seleccione tipo de documento");
            etDocumento.requestFocus();
            return false;
        }

        String tipoDocumento = etDocumento.getText().toString().trim();
        String numeroDocumento = etNumeroDocumento.getText().toString().trim();
        
        if (numeroDocumento.isEmpty()) {
            etNumeroDocumento.setError("Campo obligatorio");
            etNumeroDocumento.requestFocus();
            return false;
        }
        
        // Validar DNI: solo números, exactamente 8 cifras
        if ("DNI".equals(tipoDocumento)) {
            String numeroSinEspacios = numeroDocumento.replaceAll("\\s+", "");
            if (!numeroSinEspacios.matches("\\d{8}")) {
                etNumeroDocumento.setError("El DNI debe tener exactamente 8 dígitos numéricos");
                etNumeroDocumento.requestFocus();
                return false;
            }
        }

        if (etFechaNacimiento.getText().toString().trim().isEmpty()) {
            etFechaNacimiento.setError("Campo obligatorio");
            etFechaNacimiento.requestFocus();
            return false;
        }
        
        // Validar que la fecha de nacimiento no sea futura
        String fechaNacimiento = etFechaNacimiento.getText().toString().trim();
        if (!isValidBirthDate(fechaNacimiento)) {
            etFechaNacimiento.setError("La fecha de nacimiento no puede ser futura");
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

        String telefono = etTelefono.getText().toString().trim();
        if (telefono.isEmpty()) {
            etTelefono.setError("Campo obligatorio");
            etTelefono.requestFocus();
            return false;
        }

        // Validar que el número tenga exactamente 9 dígitos (solo el número local, sin código de país)
        String telefonoSinEspacios = telefono.replaceAll("\\s+", "");
        if (telefonoSinEspacios.length() != 9) {
            etTelefono.setError("El número de teléfono debe tener 9 dígitos");
            etTelefono.requestFocus();
            return false;
        }

        // Validar que solo contenga dígitos
        if (!telefonoSinEspacios.matches("\\d{9}")) {
            etTelefono.setError("El número de teléfono solo debe contener dígitos");
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
    
    /**
     * Valida que la fecha de nacimiento no sea futura
     */
    private boolean isValidBirthDate(String dateString) {
        try {
            // Formato esperado: DD/MM/YYYY
            String[] parts = dateString.split("/");
            if (parts.length != 3) return false;
            
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1; // Calendar usa meses 0-11
            int year = Integer.parseInt(parts[2]);
            
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, day, 0, 0, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);
            
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            return !selectedDate.after(today);
        } catch (Exception e) {
            return false;
        }
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
        Intent intent = new Intent(this, GuideRegistrationPhotoActivity.class);
        intent.putExtra("nombres", nombres);
        intent.putExtra("apellidos", apellidos);
        intent.putExtra("correo", correo);
        intent.putExtra("tipoDocumento", tipoDocumento);
        intent.putExtra("numeroDocumento", numeroDocumento);
        intent.putExtra("fechaNacimiento", fechaNacimiento);
        intent.putExtra("telefono", fullPhoneNumber);

        // NUEVO: Pasar también los datos de Google si existen
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("googleUser", false)) {
            intent.putExtra("googleUser", true);
            intent.putExtra("userType", extras.getString("userType", ""));
            intent.putExtra("userEmail", extras.getString("userEmail", ""));
            intent.putExtra("userName", extras.getString("userName", ""));
            intent.putExtra("userPhoto", extras.getString("userPhoto", "")); // ← ESTA ES LA CLAVE
        }

        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackNavigation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void handleBackNavigation() {
        if (isGoogleUserFlow) {
            NavigationUtils.navigateBackToLogin(this, true);
        } else {
            finish();
        }
    }
}
