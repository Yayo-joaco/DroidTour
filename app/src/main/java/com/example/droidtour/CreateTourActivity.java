package com.example.droidtour;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CreateTourActivity extends AppCompatActivity {
    
    private TextInputEditText etTourName, etTourDescription, etTourPrice, etTourDuration;
    private TextInputEditText etStartDate, etEndDate;
    private MaterialButton btnAddLocation, btnCancel, btnSave;
    private RecyclerView rvLocations;
    private ChipGroup chipGroupLanguages;
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
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupLanguageChips();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        etTourName = findViewById(R.id.et_tour_name);
        etTourDescription = findViewById(R.id.et_tour_description);
        etTourPrice = findViewById(R.id.et_tour_price);
        etTourDuration = findViewById(R.id.et_tour_duration);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        
        btnAddLocation = findViewById(R.id.btn_add_location);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save_tour);
        
        rvLocations = findViewById(R.id.rv_locations);
        chipGroupLanguages = findViewById(R.id.chip_group_languages);
        
        cbBreakfast = findViewById(R.id.cb_breakfast);
        cbLunch = findViewById(R.id.cb_lunch);
        cbDinner = findViewById(R.id.cb_dinner);
        cbTransport = findViewById(R.id.cb_transport);
    }
    
    private void setupClickListeners() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        
        btnAddLocation.setOnClickListener(v -> {
            Toast.makeText(this, "Abrir mapa para agregar ubicación", Toast.LENGTH_SHORT).show();
            // TODO: Implementar selección de ubicación en mapa
        });
        
        btnCancel.setOnClickListener(v -> finish());
        
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveTour();
            }
        });
    }
    
    private void setupRecyclerView() {
        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        // TODO: Configurar adapter para lista de ubicaciones
    }
    
    private void setupLanguageChips() {
        Chip chipSpanish = findViewById(R.id.chip_spanish);
        Chip chipEnglish = findViewById(R.id.chip_english);
        Chip chipFrench = findViewById(R.id.chip_french);
        Chip chipGerman = findViewById(R.id.chip_german);
        
        chipSpanish.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateLanguageSelection("Español", isChecked);
        });
        
        chipEnglish.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateLanguageSelection("Inglés", isChecked);
        });
        
        chipFrench.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateLanguageSelection("Francés", isChecked);
        });
        
        chipGerman.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateLanguageSelection("Alemán", isChecked);
        });
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
                editText.setText(date);
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }
    
    private boolean validateInputs() {
        if (etTourName.getText().toString().trim().isEmpty()) {
            etTourName.setError("Ingrese el nombre del tour");
            return false;
        }
        
        if (etTourDescription.getText().toString().trim().isEmpty()) {
            etTourDescription.setError("Ingrese la descripción");
            return false;
        }
        
        if (etTourPrice.getText().toString().trim().isEmpty()) {
            etTourPrice.setError("Ingrese el precio");
            return false;
        }
        
        if (etTourDuration.getText().toString().trim().isEmpty()) {
            etTourDuration.setError("Ingrese la duración");
            return false;
        }
        
        if (etStartDate.getText().toString().trim().isEmpty()) {
            etStartDate.setError("Seleccione fecha de inicio");
            return false;
        }
        
        if (etEndDate.getText().toString().trim().isEmpty()) {
            etEndDate.setError("Seleccione fecha de fin");
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
