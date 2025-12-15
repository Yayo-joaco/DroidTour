package com.example.droidtour.admin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Tour;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import com.example.droidtour.models.Service;

public class CreateTourActivity extends AppCompatActivity {
    
    private static final String TAG = "CreateTourActivity";
    private static final int REQ_LOCATIONS = 101;
    private static final int REQ_MEETING_POINT = 102;
    
    // Mapeo de nombres completos a códigos ISO de idiomas
    private static final java.util.Map<String, String> LANGUAGE_CODES = new java.util.HashMap<String, String>() {{
        put("Español", "es");
        put("Inglés", "en");
        put("Portugués", "pt");
        put("Francés", "fr");
        put("Quechua", "qu");
        put("Alemán", "de");
        put("Italiano", "it");
        put("Chino", "zh");
        put("Japonés", "ja");
        put("Coreano", "ko");
        put("Ruso", "ru");
        put("Árabe", "ar");
    }};

    private TextInputEditText etTourName, etTourDescription, etTourPrice, etTourDuration;
    private TextInputEditText etTourDate, etStartTime, etEndTime, etMeetingTime, etMaxCapacity;
    private android.widget.AutoCompleteTextView actvTourCategory;
    private MaterialButton btnAddLocation, btnAddImages, btnSelectMeetingPoint;
    private android.widget.TextView tvMeetingPointSelected, tvLocationsCount;
    
    // Datos del punto de encuentro
    private String meetingPointName = "";
    private Double meetingPointLat = null;
    private Double meetingPointLng = null;
    private ExtendedFloatingActionButton btnSave;
    private RecyclerView rvLocations, rvTourImages, rvServices;
    private android.widget.LinearLayout tvNoServices;
    private android.widget.TextView tvImagesCount;
    private android.widget.LinearLayout placeholderImages;
    private TourImagesAdapter tourImagesAdapter;
    private static final int MAX_IMAGES = 5;
    
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private com.example.droidtour.firebase.FirebaseStorageManager storageManager;
    
    private List<String> selectedLanguages = new ArrayList<>();
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();
    private String mainImageUrl = null;
    private String currentUserId;
    private String currentCompanyId;
    private String currentCompanyName;
    
    // Servicios de la empresa
    private List<Service> companyServices = new ArrayList<>();
    private List<String> selectedServiceIds = new ArrayList<>();
    private List<String> selectedServiceNames = new ArrayList<>();
    private ServiceCheckboxAdapter serviceAdapter;
    
    // Paradas del tour
    private List<TourLocation> tourLocations = new ArrayList<>();
    
    // Modo edición
    private boolean isEditMode = false;
    private String editingTourId = null;
    private Tour currentTour = null;
    
    // Launcher para seleccionar imágenes
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("COMPANY_ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_create_tour);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        // Inicializar Firebase
        firestoreManager = FirestoreManager.getInstance();
        storageManager = com.example.droidtour.firebase.FirebaseStorageManager.getInstance();
        currentUserId = prefsManager.getUserId();
        
        // Obtener companyId del usuario (si es COMPANY_ADMIN)
        loadCompanyId();
        
        setupToolbar();
        initializeViews();
        setupCategoryDropdown();
        setupImagePicker();
        setupClickListeners();
        setupRecyclerView();
        setupLanguageChips();
        
        // Verificar si es modo edición
        checkEditMode();
    }
    
    private void loadCompanyId() {
        // Obtener el companyId del usuario actual desde Firestore
        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    Log.d(TAG, "CompanyId cargado: " + currentCompanyId);
                    
                    // Cargar nombre de la empresa
                    loadCompanyName();
                    
                    // Cargar servicios de la empresa
                    loadCompanyServices();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar companyId", e);
            }
        });
    }
    
    private void loadCompanyName() {
        firestoreManager.getCompanyById(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.Company company = (com.example.droidtour.models.Company) result;
                if (company != null) {
                    currentCompanyName = company.getCommercialName();
                    if (currentCompanyName == null || currentCompanyName.isEmpty()) {
                        currentCompanyName = company.getBusinessName();
                    }
                    Log.d(TAG, "CompanyName cargado: " + currentCompanyName);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar nombre de empresa", e);
            }
        });
    }
    
    private void loadCompanyServices() {
        firestoreManager.getServicesByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Service> services = (List<Service>) result;
                companyServices.clear();
                if (services != null && !services.isEmpty()) {
                    companyServices.addAll(services);
                    setupServicesRecyclerView();
                    if (tvNoServices != null) {
                        tvNoServices.setVisibility(View.GONE);
                    }
                    if (rvServices != null) {
                        rvServices.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvNoServices != null) {
                        tvNoServices.setVisibility(View.VISIBLE);
                    }
                    if (rvServices != null) {
                        rvServices.setVisibility(View.GONE);
                    }
                }
                Log.d(TAG, "Servicios cargados: " + companyServices.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar servicios", e);
                if (tvNoServices != null) {
                    tvNoServices.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    
    private void setupServicesRecyclerView() {
        if (rvServices != null) {
            rvServices.setLayoutManager(new LinearLayoutManager(this));
            serviceAdapter = new ServiceCheckboxAdapter(companyServices, (serviceId, serviceName, isChecked) -> {
                if (isChecked) {
                    if (!selectedServiceIds.contains(serviceId)) {
                        selectedServiceIds.add(serviceId);
                    }
                    if (!selectedServiceNames.contains(serviceName)) {
                        selectedServiceNames.add(serviceName);
                    }
                } else {
                    selectedServiceIds.remove(serviceId);
                    selectedServiceNames.remove(serviceName);
                }
            });
            rvServices.setAdapter(serviceAdapter);
        }
    }
    
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    
                    int addedCount = 0;
                    
                    // Verificar si se seleccionaron múltiples imágenes
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count && selectedImageUris.size() < MAX_IMAGES; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            if (!selectedImageUris.contains(imageUri)) {
                                selectedImageUris.add(imageUri);
                                addedCount++;
                            }
                        }
                    } else if (data.getData() != null && selectedImageUris.size() < MAX_IMAGES) {
                        // Una sola imagen
                        Uri imageUri = data.getData();
                        if (!selectedImageUris.contains(imageUri)) {
                            selectedImageUris.add(imageUri);
                            addedCount++;
                        }
                    }
                    
                    if (addedCount > 0) {
                        if (tourImagesAdapter != null) {
                            tourImagesAdapter.notifyDataSetChanged();
                        }
                        updateImagesUI();
                        Toast.makeText(this, "✅ " + addedCount + " imagen(es) agregada(s)", Toast.LENGTH_SHORT).show();
                    } else if (selectedImageUris.size() >= MAX_IMAGES) {
                        Toast.makeText(this, "⚠️ Límite de 5 imágenes alcanzado", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
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
        etTourDate = findViewById(R.id.et_tour_date);
        etStartTime = findViewById(R.id.et_start_time);
        etEndTime = findViewById(R.id.et_end_time);
        etMeetingTime = findViewById(R.id.et_meeting_time);
        etMaxCapacity = findViewById(R.id.et_max_capacity);
        actvTourCategory = findViewById(R.id.act_tour_category);
        
        btnAddLocation = findViewById(R.id.btn_add_location);
        btnAddImages = findViewById(R.id.btn_add_images);
        btnSelectMeetingPoint = findViewById(R.id.btn_select_meeting_point);
        btnSave = findViewById(R.id.btn_save_tour);
        tvMeetingPointSelected = findViewById(R.id.tv_meeting_point_selected);
        tvLocationsCount = findViewById(R.id.tv_locations_count);
        
        rvLocations = findViewById(R.id.rv_locations);
        rvTourImages = findViewById(R.id.rv_tour_images);
        rvServices = findViewById(R.id.rv_services);
        tvNoServices = findViewById(R.id.tv_no_services);
        tvImagesCount = findViewById(R.id.tv_images_count);
        placeholderImages = findViewById(R.id.placeholder_images);
        
        // Configurar RecyclerView de imágenes
        setupImagesRecyclerView();
        
        // Configurar hora de encuentro
        setupMeetingTimePicker();
    }
    
    private void setupImagesRecyclerView() {
        if (rvTourImages != null) {
            rvTourImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            tourImagesAdapter = new TourImagesAdapter(selectedImageUris, position -> {
                // Eliminar imagen
                if (position >= 0 && position < selectedImageUris.size()) {
                    selectedImageUris.remove(position);
                    tourImagesAdapter.notifyDataSetChanged();
                    updateImagesUI();
                }
            });
            rvTourImages.setAdapter(tourImagesAdapter);
        }
    }
    
    private void updateImagesUI() {
        int count = selectedImageUris.size();
        
        // Actualizar contador
        if (tvImagesCount != null) {
            tvImagesCount.setText("Agrega hasta 5 imágenes para mostrar tu tour (" + count + "/5)");
        }
        
        // Actualizar texto del botón
        if (btnAddImages != null) {
            if (count >= MAX_IMAGES) {
                btnAddImages.setText("Límite alcanzado (5/5)");
                btnAddImages.setEnabled(false);
            } else {
                btnAddImages.setText("Agregar más imágenes (" + count + "/5)");
                btnAddImages.setEnabled(true);
            }
        }
        
        // Mostrar/ocultar placeholder y RecyclerView
        if (count > 0) {
            if (placeholderImages != null) {
                placeholderImages.setVisibility(View.GONE);
            }
            if (rvTourImages != null) {
                rvTourImages.setVisibility(View.VISIBLE);
            }
        } else {
            if (placeholderImages != null) {
                placeholderImages.setVisibility(View.VISIBLE);
            }
            if (rvTourImages != null) {
                rvTourImages.setVisibility(View.GONE);
            }
        }
    }
    
    private void setupClickListeners() {
        if (etTourDate != null) etTourDate.setOnClickListener(v -> showDatePicker(etTourDate));
        else Log.w(TAG, "etTourDate es null");

        if (etStartTime != null) etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        else Log.w(TAG, "etStartTime es null");

        if (btnAddLocation != null) {
            btnAddLocation.setOnClickListener(v -> openMap());
        } else {
            Log.w(TAG, "btnAddLocation es null");
        }
        
        if (btnSelectMeetingPoint != null) {
            btnSelectMeetingPoint.setOnClickListener(v -> openMeetingPointMap());
        } else {
            Log.w(TAG, "btnSelectMeetingPoint es null");
        }
        
        // Botón para agregar imágenes
        if (btnAddImages != null) {
            btnAddImages.setOnClickListener(v -> openImagePicker());
        }
        
        // Placeholder también abre selector de imágenes
        if (placeholderImages != null) {
            placeholderImages.setOnClickListener(v -> openImagePicker());
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
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
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
        // Configurar todos los checkboxes de idiomas
        setupLanguageCheckbox(R.id.cb_spanish, "Español", true);
        setupLanguageCheckbox(R.id.cb_english, "Inglés", false);
        setupLanguageCheckbox(R.id.cb_portuguese, "Portugués", false);
        setupLanguageCheckbox(R.id.cb_french, "Francés", false);
        setupLanguageCheckbox(R.id.cb_quechua, "Quechua", false);
        setupLanguageCheckbox(R.id.cb_german, "Alemán", false);
        setupLanguageCheckbox(R.id.cb_italian, "Italiano", false);
        setupLanguageCheckbox(R.id.cb_chinese, "Chino", false);
        setupLanguageCheckbox(R.id.cb_japanese, "Japonés", false);
        setupLanguageCheckbox(R.id.cb_korean, "Coreano", false);
        setupLanguageCheckbox(R.id.cb_russian, "Ruso", false);
        setupLanguageCheckbox(R.id.cb_arabic, "Árabe", false);
    }
    
    private void setupLanguageCheckbox(int checkboxId, String languageName, boolean isDefaultChecked) {
        CompoundButton checkbox = findViewById(checkboxId);
        if (checkbox != null) {
            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> 
                updateLanguageSelection(languageName, isChecked));
            // Si está marcado por defecto, agregar a la lista
            if (isDefaultChecked && checkbox.isChecked() && !selectedLanguages.contains(languageName)) {
                selectedLanguages.add(languageName);
            }
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
    
    /**
     * Convierte una lista de nombres de idiomas a sus códigos ISO correspondientes
     */
    private List<String> convertLanguageNamesToCodes(List<String> languageNames) {
        List<String> codes = new ArrayList<>();
        for (String name : languageNames) {
            String code = LANGUAGE_CODES.get(name);
            if (code != null) {
                codes.add(code);
                Log.d(TAG, "Idioma convertido: " + name + " -> " + code);
            } else {
                // Si no está en el mapa, usar el nombre tal cual (por si acaso)
                codes.add(name);
                Log.w(TAG, "Idioma no encontrado en mapa, usando tal cual: " + name);
            }
        }
        return codes;
    }
    
    /**
     * Convierte códigos ISO de idiomas a sus nombres completos (para mostrar al editar)
     */
    private List<String> convertLanguageCodesToNames(List<String> languageCodes) {
        List<String> names = new ArrayList<>();
        for (String code : languageCodes) {
            boolean found = false;
            for (java.util.Map.Entry<String, String> entry : LANGUAGE_CODES.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(code)) {
                    names.add(entry.getKey());
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Si no está en el mapa, usar el código tal cual
                names.add(code);
            }
        }
        return names;
    }
    
    private void setupCategoryDropdown() {
        if (actvTourCategory != null) {
            // Obtener array de departamentos desde strings.xml
            String[] departments = getResources().getStringArray(R.array.peru_departments);
            
            // Crear adapter
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                departments
            );
            
            // Asignar adapter al AutoCompleteTextView
            actvTourCategory.setAdapter(adapter);
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
    
    private void showTimePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
            this,
            (view, selectedHour, selectedMinute) -> {
                String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                if (editText != null) {
                    editText.setText(time);
                    // Calcular y actualizar hora de fin si hay paradas agregadas
                    updateEndTime();
                } else {
                    Log.w(TAG, "editText pasado a showTimePicker es null");
                }
            },
            hour, minute, true // true para formato 24 horas
        );
        
        timePickerDialog.show();
    }
    
    private void setupMeetingTimePicker() {
        if (etMeetingTime != null) {
            etMeetingTime.setOnClickListener(v -> showTimePicker(etMeetingTime));
        }
    }
    
    private void updateEndTime() {
        // Calcular duración total de todas las paradas
        int totalDurationMinutes = 0;
        if (!tourLocations.isEmpty()) {
            for (TourLocation loc : tourLocations) {
                totalDurationMinutes += loc.stopDuration;
            }
        }
        
        // Actualizar campo de duración en horas
        if (etTourDuration != null) {
            String durationText = formatDuration(totalDurationMinutes);
            etTourDuration.setText(durationText);
        }
        
        // Calcular y mostrar hora de fin
        if (etStartTime != null && etStartTime.getText() != null && !etStartTime.getText().toString().trim().isEmpty()) {
            String startTime = etStartTime.getText().toString().trim();
            String endTime = calculateEndTime(startTime, totalDurationMinutes);
            if (etEndTime != null) {
                etEndTime.setText(endTime);
            }
        }
    }
    
    private String calculateEndTime(String startTime, int durationMinutes) {
        try {
            String[] parts = startTime.split(":");
            int startHour = Integer.parseInt(parts[0]);
            int startMinute = Integer.parseInt(parts[1]);
            
            // Convertir todo a minutos
            int totalMinutes = (startHour * 60) + startMinute + durationMinutes;
            
            // Calcular nueva hora y minutos
            int endHour = (totalMinutes / 60) % 24; // Módulo 24 para no pasar de medianoche
            int endMinute = totalMinutes % 60;
            
            return String.format("%02d:%02d", endHour, endMinute);
        } catch (Exception e) {
            Log.e(TAG, "Error al calcular hora de fin", e);
            return "00:00";
        }
    }
    
    private String formatDuration(int totalMinutes) {
        if (totalMinutes == 0) {
            return "0";
        }
        
        // Convertir minutos a horas con decimales
        double hours = totalMinutes / 60.0;
        
        // Si es un número entero de horas, mostrar sin decimales
        if (totalMinutes % 60 == 0) {
            return String.valueOf((int) hours);
        }
        
        // Si no, mostrar con un decimal
        return String.format("%.1f", hours);
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
        
        // La duración se calcula automáticamente, no es necesario validarla
        // if (etTourDuration == null || etTourDuration.getText() == null || etTourDuration.getText().toString().trim().isEmpty()) {
        //     if (etTourDuration != null) etTourDuration.setError("Ingrese la duración");
        //     return false;
        // }
        
        if (etTourDate == null || etTourDate.getText() == null || etTourDate.getText().toString().trim().isEmpty()) {
            if (etTourDate != null) etTourDate.setError("Seleccione fecha del tour");
            return false;
        }
        
        if (etStartTime == null || etStartTime.getText() == null || etStartTime.getText().toString().trim().isEmpty()) {
            if (etStartTime != null) etStartTime.setError("Seleccione hora de inicio");
            return false;
        }
        
        if (selectedLanguages.isEmpty()) {
            Toast.makeText(this, "Seleccione al menos un idioma", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Validar que la hora de encuentro sea antes que la hora de inicio
        if (etMeetingTime != null && etMeetingTime.getText() != null && !etMeetingTime.getText().toString().trim().isEmpty() &&
            etStartTime != null && etStartTime.getText() != null && !etStartTime.getText().toString().trim().isEmpty()) {
            
            String meetingTime = etMeetingTime.getText().toString().trim();
            String startTime = etStartTime.getText().toString().trim();
            
            if (!isTimeBeforeOrEqual(meetingTime, startTime)) {
                if (etMeetingTime != null) etMeetingTime.setError("La hora de encuentro debe ser antes o igual a la hora de inicio");
                Toast.makeText(this, "La hora de encuentro debe ser antes o igual a la hora de inicio del tour", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isTimeBeforeOrEqual(String time1, String time2) {
        try {
            String[] parts1 = time1.split(":");
            String[] parts2 = time2.split(":");
            
            int hour1 = Integer.parseInt(parts1[0]);
            int minute1 = Integer.parseInt(parts1[1]);
            int hour2 = Integer.parseInt(parts2[0]);
            int minute2 = Integer.parseInt(parts2[1]);
            
            int totalMinutes1 = hour1 * 60 + minute1;
            int totalMinutes2 = hour2 * 60 + minute2;
            
            return totalMinutes1 <= totalMinutes2;
        } catch (Exception e) {
            return true; // Si hay error en el formato, no validar
        }
    }
    
    private void saveTour() {
        // Deshabilitar botón mientras guarda
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Guardando...");
        }
        
        String tourName = etTourName.getText().toString().trim();
        
        // Separar URLs existentes de Firebase vs nuevas imágenes locales
        List<Uri> newLocalImages = new ArrayList<>();
        for (Uri uri : selectedImageUris) {
            String uriString = uri.toString();
            // Si no empieza con https://, es una imagen local nueva
            if (!uriString.startsWith("https://") && !uriString.startsWith("http://")) {
                newLocalImages.add(uri);
            }
        }
        
        // Si hay imágenes nuevas locales, subirlas primero
        if (!newLocalImages.isEmpty()) {
            uploadNewImagesAndSaveTour(tourName, newLocalImages);
        } else {
            // Sin imágenes nuevas, guardar directamente con las URLs existentes
            String mainImage = uploadedImageUrls.isEmpty() ? null : uploadedImageUrls.get(0);
            saveTourToFirestore(tourName, mainImage);
        }
    }
    
    private void uploadNewImagesAndSaveTour(String tourName, List<Uri> newLocalImages) {
        // Guardar las URLs existentes
        List<String> existingUrls = new ArrayList<>(uploadedImageUrls);
        
        Toast.makeText(this, "Subiendo " + newLocalImages.size() + " imagen(es) nueva(s)...", Toast.LENGTH_SHORT).show();
        
        // Usar FirebaseStorageManager para subir solo las imágenes nuevas
        storageManager.uploadTourImages(tourName, newLocalImages, 
            new com.example.droidtour.firebase.FirebaseStorageManager.MultipleUploadCallback() {
                @Override
                public void onComplete(java.util.List<String> downloadUrls) {
                    runOnUiThread(() -> {
                        // Combinar URLs existentes con las nuevas
                        uploadedImageUrls.clear();
                        uploadedImageUrls.addAll(existingUrls);
                        uploadedImageUrls.addAll(downloadUrls);
                        
                        Log.d(TAG, "Imágenes totales: " + uploadedImageUrls.size() + " (existentes: " + existingUrls.size() + ", nuevas: " + downloadUrls.size() + ")");
                        
                        String mainImage = uploadedImageUrls.isEmpty() ? null : uploadedImageUrls.get(0);
                        saveTourToFirestore(tourName, mainImage);
                    });
                }
            });
    }
    
    private void saveTourToFirestore(String tourName, String mainImage) {
        // Obtener datos del formulario
        String description = etTourDescription.getText().toString().trim();
        double price = Double.parseDouble(etTourPrice.getText().toString().trim());
        String tourDate = etTourDate.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String category = (actvTourCategory != null && actvTourCategory.getText() != null) 
            ? actvTourCategory.getText().toString().trim() : "";
        
        // Capacidad máxima
        int maxCapacity = 0;
        if (etMaxCapacity != null && etMaxCapacity.getText() != null && !etMaxCapacity.getText().toString().trim().isEmpty()) {
            maxCapacity = Integer.parseInt(etMaxCapacity.getText().toString().trim());
        }
        
        // Calcular duración total sumando las duraciones de todas las paradas
        int totalDurationMinutes = 0;
        if (!tourLocations.isEmpty()) {
            for (TourLocation loc : tourLocations) {
                totalDurationMinutes += loc.stopDuration;
            }
        }
        
        // Calcular hora de fin
        String endTime = calculateEndTime(startTime, totalDurationMinutes);
        
        // Formatear duración en horas para compatibilidad
        String duration = formatDuration(totalDurationMinutes) + " hrs";
        
        // Crear objeto Tour
        Tour tour = new Tour();
        tour.setTourName(tourName);
        tour.setDescription(description);
        tour.setPricePerPerson(price);
        tour.setDuration(duration); // Duración en formato legible
        tour.setCategory(category); // Departamento del Perú
        tour.setMaxGroupSize(maxCapacity);
        tour.setTourDate(tourDate);
        tour.setStartTime(startTime);
        tour.setEndTime(endTime);
        tour.setTotalDuration(totalDurationMinutes);
        tour.setCompanyId(currentCompanyId);
        tour.setCompanyName(currentCompanyName);
        
        // Inicializar campos de check-in/checkout
        tour.setCheckInOutStatus("ESPERANDO_CHECKIN");
        tour.setExpectedParticipants(0);
        tour.setCheckedInCount(0);
        tour.setCheckedOutCount(0);
        tour.setScannedParticipants(new java.util.ArrayList<>());
        
        // Convertir nombres de idiomas a códigos ISO antes de guardar
        List<String> languageCodes = convertLanguageNamesToCodes(selectedLanguages);
        tour.setLanguages(languageCodes);
        
        tour.setMeetingPoint(meetingPointName);
        tour.setMeetingPointLatitude(meetingPointLat);
        tour.setMeetingPointLongitude(meetingPointLng);
        
        // Hora de encuentro (puede ser diferente a la hora de inicio)
        String meetingTime = etMeetingTime != null && etMeetingTime.getText() != null 
            ? etMeetingTime.getText().toString().trim() 
            : startTime; // Por defecto usa hora de inicio si no se especifica
        tour.setMeetingTime(meetingTime);
        
        tour.setMainImageUrl(mainImage);
        tour.setImageUrls(uploadedImageUrls.isEmpty() ? null : uploadedImageUrls);
        tour.setActive(false); // false por defecto, true solo cuando se asigna guía
        tour.setPublic(false); // false por defecto, true cuando guía acepte propuesta
        tour.setFeatured(false);
        tour.setAverageRating(0.0);
        tour.setTotalReviews(0);
        tour.setTotalBookings(0);
        
        // Servicios incluidos (nombres e IDs)
        tour.setIncludedServices(selectedServiceNames.isEmpty() ? null : new ArrayList<>(selectedServiceNames));
        tour.setIncludedServiceIds(selectedServiceIds.isEmpty() ? null : new ArrayList<>(selectedServiceIds));
        
        // Paradas del tour
        if (!tourLocations.isEmpty()) {
            List<Tour.TourStop> stops = new ArrayList<>();
            for (TourLocation loc : tourLocations) {
                stops.add(new Tour.TourStop(
                    loc.lat, 
                    loc.lng, 
                    loc.name, 
                    loc.order, 
                    loc.time, 
                    loc.description,
                    loc.stopDuration
                ));
            }
            tour.setStops(stops);
        }
        
        // Guardar en Firestore
        if (isEditMode && editingTourId != null) {
            // Modo edición: actualizar tour existente
            tour.setTourId(editingTourId);
            // Mantener valores originales que no se deben cambiar
            if (currentTour != null) {
                tour.setActive(currentTour.getActive() != null && currentTour.getActive());
                tour.setPublic(currentTour.getPublic() != null && currentTour.getPublic());
                tour.setAssignedGuideId(currentTour.getAssignedGuideId());
                tour.setAssignedGuideName(currentTour.getAssignedGuideName());
                tour.setGuidePayment(currentTour.getGuidePayment());
                tour.setTourStatus(currentTour.getTourStatus());
                tour.setTotalBookings(currentTour.getTotalBookings());
                tour.setAverageRating(currentTour.getAverageRating());
                tour.setTotalReviews(currentTour.getTotalReviews());
                
                // Mantener campos de check-in/checkout solo si ya tienen valores (no null)
                // Si son null, se mantendrán los valores inicializados arriba
                if (currentTour.getCheckInOutStatus() != null) {
                    tour.setCheckInOutStatus(currentTour.getCheckInOutStatus());
                }
                if (currentTour.getExpectedParticipants() != null && currentTour.getExpectedParticipants() > 0) {
                    tour.setExpectedParticipants(currentTour.getExpectedParticipants());
                }
                if (currentTour.getCheckedInCount() != null && currentTour.getCheckedInCount() > 0) {
                    tour.setCheckedInCount(currentTour.getCheckedInCount());
                }
                if (currentTour.getCheckedOutCount() != null && currentTour.getCheckedOutCount() > 0) {
                    tour.setCheckedOutCount(currentTour.getCheckedOutCount());
                }
                if (currentTour.getScannedParticipants() != null && !currentTour.getScannedParticipants().isEmpty()) {
                    tour.setScannedParticipants(currentTour.getScannedParticipants());
                }
            }
            
            firestoreManager.updateTour(tour, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Toast.makeText(CreateTourActivity.this, "✅ Tour actualizado exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                }
                
                @Override
                public void onFailure(Exception e) {
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Guardar Cambios");
                    }
                    Toast.makeText(CreateTourActivity.this, "❌ Error al actualizar tour: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error al actualizar tour", e);
                }
            });
        } else {
            // Modo creación: crear nuevo tour
            firestoreManager.createTour(tour, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Toast.makeText(CreateTourActivity.this, "✅ Tour creado exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                }
                
                @Override
                public void onFailure(Exception e) {
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Guardar Tour");
                    }
                    Toast.makeText(CreateTourActivity.this, "❌ Error al crear tour: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error al crear tour", e);
                }
            });
        }
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



    private void openMap() {
        Intent intent = new Intent(this, TourLocationsMapActivity.class);
        startActivityForResult(intent, REQ_LOCATIONS);
    }
    
    private void openMeetingPointMap() {
        // Abrir la misma actividad de mapa pero en modo de punto único
        Intent intent = new Intent(this, TourLocationsMapActivity.class);
        intent.putExtra("singleLocationMode", true);
        intent.putExtra("title", "Seleccionar Punto de Encuentro");
        startActivityForResult(intent, REQ_MEETING_POINT);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_LOCATIONS && resultCode == RESULT_OK && data != null) {
            ArrayList<TourLocation> locations = data.getParcelableArrayListExtra("locations");
            
            if (locations != null && !locations.isEmpty()) {
                tourLocations.clear();
                tourLocations.addAll(locations);
                
                // Mostrar resumen de paradas
                Toast.makeText(this, "✅ " + tourLocations.size() + " parada(s) agregada(s)", Toast.LENGTH_SHORT).show();
                
                // Actualizar UI - mostrar lista de paradas
                updateLocationsUI();
                
                // Actualizar hora de fin
                updateEndTime();
            }
        } else if (requestCode == REQ_MEETING_POINT && resultCode == RESULT_OK && data != null) {
            // Recibir datos del punto de encuentro
            meetingPointName = data.getStringExtra("meetingPointName");
            meetingPointLat = data.getDoubleExtra("meetingPointLat", 0.0);
            meetingPointLng = data.getDoubleExtra("meetingPointLng", 0.0);
            
            if (meetingPointName != null && !meetingPointName.isEmpty()) {
                // Actualizar UI
                if (tvMeetingPointSelected != null) {
                    tvMeetingPointSelected.setText("📍 " + meetingPointName);
                    tvMeetingPointSelected.setTextColor(getResources().getColor(R.color.primary));
                }
                if (btnSelectMeetingPoint != null) {
                    btnSelectMeetingPoint.setText("Cambiar Punto de Encuentro");
                }
                Toast.makeText(this, "✅ Punto de encuentro seleccionado", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private TourLocationsAdapter locationsAdapter;
    private androidx.recyclerview.widget.ItemTouchHelper locationsTouchHelper;
    
    private void updateLocationsUI() {
        // Actualizar contador de paradas
        if (tvLocationsCount != null) {
            int count = tourLocations.size();
            tvLocationsCount.setText(count + (count == 1 ? " parada" : " paradas"));
        }
        
        if (rvLocations != null && !tourLocations.isEmpty()) {
            rvLocations.setLayoutManager(new LinearLayoutManager(this));
            
            locationsAdapter = new TourLocationsAdapter(tourLocations, new TourLocationsAdapter.OnLocationActionListener() {
                @Override
                public void onLocationEdited(int position, TourLocation location) {
                    // La ubicación ya fue actualizada en el adapter
                    Log.d(TAG, "Parada editada: " + location.getName() + " - Duración: " + location.getStopDuration() + " min");
                    // Recalcular hora de fin
                    updateEndTime();
                }

                @Override
                public void onLocationDeleted(int position) {
                    if (position >= 0 && position < tourLocations.size()) {
                        tourLocations.remove(position);
                        
                        // Actualizar orden de las restantes
                        for (int i = 0; i < tourLocations.size(); i++) {
                            tourLocations.get(i).setOrder(i + 1);
                        }
                        
                        if (locationsAdapter != null) {
                            locationsAdapter.notifyDataSetChanged();
                        }
                        
                        // Recalcular hora de fin
                        updateEndTime();
                        
                        // Actualizar contador
                        if (tvLocationsCount != null) {
                            int count = tourLocations.size();
                            tvLocationsCount.setText(count + (count == 1 ? " parada" : " paradas"));
                        }
                        
                        Toast.makeText(CreateTourActivity.this, "Parada eliminada", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onLocationsReordered() {
                    Log.d(TAG, "Paradas reordenadas");
                    // Recalcular hora de fin
                    updateEndTime();
                }
            });
            
            rvLocations.setAdapter(locationsAdapter);
            
            // Configurar drag & drop
            TourLocationItemTouchHelper callback = new TourLocationItemTouchHelper(locationsAdapter);
            locationsTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(callback);
            locationsTouchHelper.attachToRecyclerView(rvLocations);
            locationsAdapter.setItemTouchHelper(locationsTouchHelper);
            
        } else if (rvLocations != null) {
            rvLocations.setAdapter(null);
        }
        
        // Actualizar hora de fin cuando se carguen las ubicaciones
        updateEndTime();
    }

    private void checkEditMode() {
        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);
        editingTourId = getIntent().getStringExtra("TOUR_ID");
        
        if (isEditMode && editingTourId != null) {
            // Cambiar título
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Editar Tour");
            }
            // Ocultar el header de "Crear Nuevo Tour"
            View headerCard = findViewById(R.id.card_header);
            if (headerCard != null) {
                headerCard.setVisibility(View.GONE);
            }
            // Cambiar texto del botón
            if (btnSave != null) {
                btnSave.setText("Guardar Cambios");
            }
            // Cargar datos del tour
            loadTourData();
        }
    }

    private void loadTourData() {
        firestoreManager.getTourById(editingTourId, new FirestoreManager.TourCallback() {
            @Override
            public void onSuccess(Tour tour) {
                if (tour != null) {
                    currentTour = tour;
                    populateTourData(tour);
                } else {
                    Toast.makeText(CreateTourActivity.this, "No se pudo cargar el tour", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CreateTourActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateTourData(Tour tour) {
        // Datos básicos
        if (etTourName != null && tour.getTourName() != null) {
            etTourName.setText(tour.getTourName());
        }
        if (etTourDescription != null && tour.getDescription() != null) {
            etTourDescription.setText(tour.getDescription());
        }
        if (etTourPrice != null && tour.getPricePerPerson() != null) {
            etTourPrice.setText(String.valueOf(tour.getPricePerPerson()));
        }
        
        // Capacidad máxima
        if (etMaxCapacity != null && tour.getMaxGroupSize() != null && tour.getMaxGroupSize() > 0) {
            etMaxCapacity.setText(String.valueOf(tour.getMaxGroupSize()));
        }
        
        // Fecha
        if (etTourDate != null && tour.getTourDate() != null) {
            etTourDate.setText(tour.getTourDate());
        }
        
        // Horarios
        if (etMeetingTime != null && tour.getMeetingTime() != null) {
            etMeetingTime.setText(tour.getMeetingTime());
        }
        if (etStartTime != null && tour.getStartTime() != null) {
            etStartTime.setText(tour.getStartTime());
        }
        if (etEndTime != null && tour.getEndTime() != null) {
            etEndTime.setText(tour.getEndTime());
        }
        
        // Categoría (departamento del Perú)
        if (actvTourCategory != null && tour.getCategory() != null) {
            actvTourCategory.setText(tour.getCategory(), false);
        }
        
        // Punto de encuentro
        if (tour.getMeetingPoint() != null) {
            meetingPointName = tour.getMeetingPoint();
            if (tour.getMeetingPointLatitude() != null) {
                meetingPointLat = tour.getMeetingPointLatitude();
            }
            if (tour.getMeetingPointLongitude() != null) {
                meetingPointLng = tour.getMeetingPointLongitude();
            }
            if (tvMeetingPointSelected != null) {
                tvMeetingPointSelected.setText("📍 " + meetingPointName);
                tvMeetingPointSelected.setTextColor(getResources().getColor(R.color.primary));
            }
        }
        
        // Idiomas - marcar los checkboxes correspondientes
        if (tour.getLanguages() != null && !tour.getLanguages().isEmpty()) {
            selectedLanguages.clear();
            // Convertir códigos ISO a nombres completos para mostrar en UI
            List<String> languageNames = convertLanguageCodesToNames(tour.getLanguages());
            selectedLanguages.addAll(languageNames);
            
            Log.d(TAG, "Idiomas cargados desde BD (códigos): " + tour.getLanguages());
            Log.d(TAG, "Idiomas convertidos a nombres: " + languageNames);
            
            // Marcar los checkboxes según los idiomas seleccionados
            markLanguageCheckboxes();
        }
        
        // Servicios incluidos
        if (tour.getIncludedServices() != null && !tour.getIncludedServices().isEmpty()) {
            selectedServiceNames.clear();
            selectedServiceNames.addAll(tour.getIncludedServices());
        }
        if (tour.getIncludedServiceIds() != null && !tour.getIncludedServiceIds().isEmpty()) {
            selectedServiceIds.clear();
            selectedServiceIds.addAll(tour.getIncludedServiceIds());
        }
        // Actualizar los checkboxes en el adapter con delay para asegurar que esté inicializado
        runOnUiThread(() -> {
            if (serviceAdapter != null) {
                serviceAdapter.setSelectedServices(selectedServiceIds);
            } else {
                // Si el adapter no está listo, intentar de nuevo después de un delay
                new android.os.Handler().postDelayed(() -> {
                    if (serviceAdapter != null) {
                        serviceAdapter.setSelectedServices(selectedServiceIds);
                    }
                }, 500);
            }
        });
        
        // Paradas/Ubicaciones
        if (tour.getStops() != null && !tour.getStops().isEmpty()) {
            tourLocations.clear();
            // Convertir TourStop a TourLocation
            for (Tour.TourStop stop : tour.getStops()) {
                TourLocation loc = new TourLocation(
                    stop.getLatitude(),
                    stop.getLongitude(),
                    stop.getName(),
                    stop.getOrder(),
                    stop.getTime(),
                    stop.getDescription()
                );
                if (stop.getStopDuration() != null) {
                    loc.stopDuration = stop.getStopDuration();
                }
                tourLocations.add(loc);
            }
            updateLocationsUI();
        }
        
        // Imágenes - mantener las URLs existentes
        if (tour.getImageUrls() != null && !tour.getImageUrls().isEmpty()) {
            selectedImageUris.clear();
            uploadedImageUrls.clear();
            // Las URLs ya están en Firebase, guardarlas directamente
            uploadedImageUrls.addAll(tour.getImageUrls());
            // Agregar como Uri para mostrar en el RecyclerView
            for (String url : tour.getImageUrls()) {
                selectedImageUris.add(Uri.parse(url));
            }
            updateImagesUI();
        }
    }

    private void updateLanguageChipsSelection() {
        // Buscar los checkboxes de idiomas y marcarlos
        if (selectedLanguages != null && !selectedLanguages.isEmpty()) {
            // Los chips se actualizarán cuando se cargue la UI
            Log.d(TAG, "Idiomas seleccionados: " + selectedLanguages);
        }
    }
    
    private void markLanguageCheckboxes() {
        // Marcar checkboxes según los idiomas en selectedLanguages
        markLanguageCheckbox(R.id.cb_spanish, "Español");
        markLanguageCheckbox(R.id.cb_english, "Inglés");
        markLanguageCheckbox(R.id.cb_portuguese, "Portugués");
        markLanguageCheckbox(R.id.cb_french, "Francés");
        markLanguageCheckbox(R.id.cb_quechua, "Quechua");
        markLanguageCheckbox(R.id.cb_german, "Alemán");
        markLanguageCheckbox(R.id.cb_italian, "Italiano");
        markLanguageCheckbox(R.id.cb_chinese, "Chino");
        markLanguageCheckbox(R.id.cb_japanese, "Japonés");
        markLanguageCheckbox(R.id.cb_korean, "Coreano");
        markLanguageCheckbox(R.id.cb_russian, "Ruso");
        markLanguageCheckbox(R.id.cb_arabic, "Árabe");
    }
    
    private void markLanguageCheckbox(int checkboxId, String languageName) {
        CompoundButton checkbox = findViewById(checkboxId);
        if (checkbox != null) {
            boolean shouldBeChecked = selectedLanguages.contains(languageName);
            checkbox.setChecked(shouldBeChecked);
        }
    }





}
