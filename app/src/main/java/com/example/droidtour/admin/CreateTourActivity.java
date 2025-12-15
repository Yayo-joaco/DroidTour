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

    private TextInputEditText etTourName, etTourDescription, etTourPrice, etTourDuration;
    private TextInputEditText etStartDate, etEndDate;
    private MaterialButton btnAddLocation, btnAddImages;
    private ExtendedFloatingActionButton btnSave;
    private RecyclerView rvLocations, rvTourImages, rvServices;
    private android.widget.TextView tvNoServices, tvImagesCount;
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
        setupImagePicker();
        setupClickListeners();
        setupRecyclerView();
        setupLanguageChips();
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
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        
        btnAddLocation = findViewById(R.id.btn_add_location);
        btnAddImages = findViewById(R.id.btn_add_images);
        btnSave = findViewById(R.id.btn_save_tour);
        
        rvLocations = findViewById(R.id.rv_locations);
        rvTourImages = findViewById(R.id.rv_tour_images);
        rvServices = findViewById(R.id.rv_services);
        tvNoServices = findViewById(R.id.tv_no_services);
        tvImagesCount = findViewById(R.id.tv_images_count);
        placeholderImages = findViewById(R.id.placeholder_images);
        
        // Configurar RecyclerView de imágenes
        setupImagesRecyclerView();
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
        if (etStartDate != null) etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        else Log.w(TAG, "etStartDate es null");

        if (etEndDate != null) etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        else Log.w(TAG, "etEndDate es null");

        if (btnAddLocation != null) {
            btnAddLocation.setOnClickListener(v -> openMap());
        } else {
            Log.w(TAG, "btnAddLocation es null");
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
        // Deshabilitar botón mientras guarda
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Guardando...");
        }
        
        String tourName = etTourName.getText().toString().trim();
        
        // Si hay imágenes seleccionadas, subirlas primero
        if (!selectedImageUris.isEmpty()) {
            uploadImagesAndSaveTour(tourName);
        } else {
            // Sin imágenes, guardar directamente
            saveTourToFirestore(tourName, null);
        }
    }
    
    private void uploadImagesAndSaveTour(String tourName) {
        uploadedImageUrls.clear();
        
        Toast.makeText(this, "Subiendo " + selectedImageUris.size() + " imagen(es)...", Toast.LENGTH_SHORT).show();
        
        // Usar FirebaseStorageManager para subir todas las imágenes
        storageManager.uploadTourImages(tourName, selectedImageUris, 
            new com.example.droidtour.firebase.FirebaseStorageManager.MultipleUploadCallback() {
                @Override
                public void onComplete(java.util.List<String> downloadUrls) {
                    runOnUiThread(() -> {
                        uploadedImageUrls.clear();
                        uploadedImageUrls.addAll(downloadUrls);
                        
                        Log.d(TAG, "Imágenes subidas: " + uploadedImageUrls.size());
                        
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
        String duration = etTourDuration.getText().toString().trim();
        
        // Crear objeto Tour
        Tour tour = new Tour();
        tour.setTourName(tourName);
        tour.setDescription(description);
        tour.setPricePerPerson(price);
        tour.setDuration(duration);
        tour.setCompanyId(currentCompanyId);
        tour.setCompanyName(currentCompanyName);
        tour.setLanguages(selectedLanguages);
        tour.setMainImageUrl(mainImage);
        tour.setImageUrls(uploadedImageUrls.isEmpty() ? null : uploadedImageUrls);
        tour.setActive(true);
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
                stops.add(new Tour.TourStop(loc.lat, loc.lng, loc.name, loc.order));
            }
            tour.setStops(stops);
        }
        
        // Guardar en Firestore
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
            }
        }
    }
    
    private void updateLocationsUI() {
        if (rvLocations != null && !tourLocations.isEmpty()) {
            rvLocations.setLayoutManager(new LinearLayoutManager(this));
            rvLocations.setAdapter(new LocationsAdapter(tourLocations));
        }
    }
    
    // Adapter simple para mostrar las ubicaciones seleccionadas
    private class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.ViewHolder> {
        private List<TourLocation> locations;
        
        LocationsAdapter(List<TourLocation> locations) {
            this.locations = locations;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tour_location, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TourLocation location = locations.get(position);
            if (holder.tvLocationName != null) {
                holder.tvLocationName.setText(location.name != null ? location.name : "Parada " + (position + 1));
            }
            if (holder.tvLocationOrder != null) {
                holder.tvLocationOrder.setText(String.valueOf(position + 1));
            }
        }
        
        @Override
        public int getItemCount() {
            return locations.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView tvLocationName, tvLocationOrder;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvLocationName = itemView.findViewById(R.id.tv_location_name);
                tvLocationOrder = itemView.findViewById(R.id.tv_location_order);
            }
        }
    }





}
