package com.example.droidtour.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CreateTourActivity extends AppCompatActivity {
    
    private static final String TAG = "CreateTourActivity";

    private TextInputEditText etTourName, etTourDescription, etTourPrice, etTourDuration;
    private TextInputEditText etStartDate, etEndDate;
    private MaterialButton btnAddLocation;
    private ExtendedFloatingActionButton btnSave;
    private RecyclerView rvLocations;
    private CheckBox cbBreakfast, cbLunch, cbDinner, cbTransport;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    
    private List<String> selectedLanguages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("ADMIN")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_create_tour);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupLanguageChips();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void initializeViews() {
        etTourName = findViewById(R.id.et_tour_name);
        etTourDescription = findViewById(R.id.et_tour_description);
        etTourPrice = findViewById(R.id.et_tour_price);
        etTourDuration = findViewById(R.id.et_tour_duration);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        
        btnAddLocation = findViewById(R.id.btn_add_location);
        btnSave = findViewById(R.id.btn_save_tour);
        
        rvLocations = findViewById(R.id.rv_locations);

        cbBreakfast = findViewById(R.id.cb_breakfast);
        cbLunch = findViewById(R.id.cb_lunch);
        cbDinner = findViewById(R.id.cb_dinner);
        cbTransport = findViewById(R.id.cb_transport);
    }
    
    private void setupClickListeners() {
        if (etStartDate != null) etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        else Log.w(TAG, "etStartDate es null");

        if (etEndDate != null) etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        else Log.w(TAG, "etEndDate es null");

        if (btnAddLocation != null) {
            btnAddLocation.setOnClickListener(v -> {
                Toast.makeText(this, "Abrir mapa para agregar ubicación", Toast.LENGTH_SHORT).show();
                // TODO: Implementar selección de ubicación en mapa
            });
        } else {
            Log.w(TAG, "btnAddLocation es null");
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (validateInputs()) {
                    saveTour();
                }
            });
        } else {
            Log.w(TAG, "btnSave es null");
        }
    }
    
    private void setupRecyclerView() {
        if (rvLocations != null) {
            rvLocations.setLayoutManager(new LinearLayoutManager(this));
        } else {
            Log.w(TAG, "rvLocations es null");
        }
        // TODO: Configurar adapter para lista de ubicaciones
    }
    
    private void setupLanguageChips() {
        // El layout actual usa MaterialCheckBox con ids cb_spanish, cb_english, cb_french, cb_portuguese
        CompoundButton cbSpanish = findViewById(R.id.cb_spanish);
        CompoundButton cbEnglish = findViewById(R.id.cb_english);
        CompoundButton cbFrench = findViewById(R.id.cb_french);
        CompoundButton cbPortuguese = findViewById(R.id.cb_portuguese);

        if (cbSpanish != null) {
            cbSpanish.setOnCheckedChangeListener((buttonView, isChecked) -> updateLanguageSelection("Español", isChecked));
            // Si está marcado por defecto, agregar a la lista
            if (cbSpanish.isChecked() && !selectedLanguages.contains("Español")) selectedLanguages.add("Español");
        }

        if (cbEnglish != null) {
            cbEnglish.setOnCheckedChangeListener((buttonView, isChecked) -> updateLanguageSelection("Inglés", isChecked));
        }

        if (cbFrench != null) {
            cbFrench.setOnCheckedChangeListener((buttonView, isChecked) -> updateLanguageSelection("Francés", isChecked));
        }

        if (cbPortuguese != null) {
            cbPortuguese.setOnCheckedChangeListener((buttonView, isChecked) -> updateLanguageSelection("Portugués", isChecked));
        }
    }
    
    private void updateLanguageSelection(String language, boolean isSelected) {
        if (isSelected) {
            if (!selectedLanguages.contains(language)) {
                selectedLanguages.add(language);
            }
        } else {
            selectedLanguages.remove(language);
        }
    }
    
    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                if (editText != null) {
                    editText.setText(date);
                } else {
                    Log.w(TAG, "editText pasado a showDatePicker es null");
                }
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }
    
    private boolean validateInputs() {
        if (etTourName == null || etTourName.getText() == null || etTourName.getText().toString().trim().isEmpty()) {
            if (etTourName != null) etTourName.setError("Ingrese el nombre del tour");
            return false;
        }
        
        if (etTourDescription == null || etTourDescription.getText() == null || etTourDescription.getText().toString().trim().isEmpty()) {
            if (etTourDescription != null) etTourDescription.setError("Ingrese la descripción");
            return false;
        }
        
        if (etTourPrice == null || etTourPrice.getText() == null || etTourPrice.getText().toString().trim().isEmpty()) {
            if (etTourPrice != null) etTourPrice.setError("Ingrese el precio");
            return false;
        }
        
        if (etTourDuration == null || etTourDuration.getText() == null || etTourDuration.getText().toString().trim().isEmpty()) {
            if (etTourDuration != null) etTourDuration.setError("Ingrese la duración");
            return false;
        }
        
        if (etStartDate == null || etStartDate.getText() == null || etStartDate.getText().toString().trim().isEmpty()) {
            if (etStartDate != null) etStartDate.setError("Seleccione fecha de inicio");
            return false;
        }
        
        if (etEndDate == null || etEndDate.getText() == null || etEndDate.getText().toString().trim().isEmpty()) {
            if (etEndDate != null) etEndDate.setError("Seleccione fecha de fin");
            return false;
        }
        
        if (selectedLanguages.isEmpty()) {
            Toast.makeText(this, "Seleccione al menos un idioma", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void saveTour() {
        // TODO: Implementar guardado en base de datos
        Toast.makeText(this, "Tour creado exitosamente", Toast.LENGTH_SHORT).show();
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
    
    private void redirectToLogin() {
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
