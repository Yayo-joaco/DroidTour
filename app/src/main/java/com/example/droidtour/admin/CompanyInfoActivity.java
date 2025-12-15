package com.example.droidtour.admin;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.droidtour.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.chip.Chip;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class CompanyInfoActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "CompanyInfoActivity";
    
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private com.example.droidtour.firebase.FirebaseStorageManager storageManager;
    private String currentUserId;
    private String currentCompanyId;
    private com.example.droidtour.models.Company currentCompany;

    private TextInputEditText etCompanyName, etCompanyEmail, etCompanyPhone, etCompanyAddress;
    private TextInputEditText etCompanyRuc, etCompanyDescription;
    private android.widget.AutoCompleteTextView actBusinessType;
    private MaterialCardView cardImage1, cardImage2, cardMapPreview;
    private MaterialButton btnSelectLocation, btnCancel;
    private ExtendedFloatingActionButton btnSave;
    private com.example.droidtour.utils.PreferencesManager prefsManager;

    // Variables para Google Maps
    private MapView mapView;
    private GoogleMap googleMap;
    private Chip chipCoordinates;
    private View mapPlaceholder;
    private LatLng selectedLocation;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    
    // Variables para imágenes
    private MaterialCardView cardLogo;
    private ImageView ivCompanyLogo, ivCompanyImage1, ivCompanyImage2;
    private LinearLayout placeholderLogo, placeholderImage1, placeholderImage2;
    private Uri logoUri, image1Uri, image2Uri;
    private int currentImageSelection = 0; // 0=logo, 1=image1, 2=image2
    
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImageSelected(uri);
                }
            }
    );

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

        setContentView(R.layout.activity_company_info);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        authManager = com.example.droidtour.firebase.FirebaseAuthManager.getInstance(this);
        storageManager = com.example.droidtour.firebase.FirebaseStorageManager.getInstance();
        //currentUserId = authManager.getCurrentUserId();

        setupToolbar();
        initializeViews();
        initializeMapView(savedInstanceState);
        setupClickListeners();
        setupSaveButton();
        loadCompanyData();
    }
    
    private void loadCompanyData() {
        String userId = prefsManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            android.util.Log.e(TAG, "No hay userId");
            return;
        }
        
        // Primero obtener el companyId del usuario
        firestoreManager.getUserById(userId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadCompanyDetails();
                } else {
                    android.util.Log.e(TAG, "Usuario sin companyId");
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e(TAG, "Error cargando usuario", e);
            }
        });
    }
    
    private void loadCompanyDetails() {
        firestoreManager.getCompanyById(currentCompanyId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                currentCompany = (com.example.droidtour.models.Company) result;
                if (currentCompany != null) {
                    populateCompanyData();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e(TAG, "Error cargando empresa", e);
                Toast.makeText(CompanyInfoActivity.this, "Error al cargar datos de empresa", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void populateCompanyData() {
        // Campos bloqueados (NO EDITABLES) - datos del superadmin
        if (etCompanyName != null && currentCompany.getCommercialName() != null) {
            etCompanyName.setText(currentCompany.getCommercialName());
            etCompanyName.setEnabled(false);
            etCompanyName.setFocusable(false);
            etCompanyName.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
        
        if (etCompanyRuc != null && currentCompany.getRuc() != null) {
            etCompanyRuc.setText(currentCompany.getRuc());
            etCompanyRuc.setEnabled(false);
            etCompanyRuc.setFocusable(false);
            etCompanyRuc.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
        
        if (actBusinessType != null && currentCompany.getBusinessType() != null) {
            actBusinessType.setText(currentCompany.getBusinessType());
            actBusinessType.setEnabled(false);
            actBusinessType.setFocusable(false);
            actBusinessType.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
        
        // Campos editables - cargar datos de la EMPRESA (no del admin)
        if (etCompanyEmail != null) {
            String companyEmail = currentCompany.getEmail();
            if (companyEmail != null && !companyEmail.isEmpty()) {
                etCompanyEmail.setText(companyEmail);
            } else {
                etCompanyEmail.setText(""); // Vacío si no hay email de empresa
            }
        }
        
        if (etCompanyPhone != null) {
            String phone = currentCompany.getPhone();
            if (phone != null && !phone.isEmpty()) {
                // Remover el código de país si está presente
                if (phone.startsWith("+51")) {
                    phone = phone.substring(3);
                }
                etCompanyPhone.setText(phone.trim());
            } else {
                etCompanyPhone.setText(""); // Vacío si no hay teléfono de empresa
            }
        }
        
        if (etCompanyAddress != null) {
            String address = currentCompany.getAddress();
            if (address != null && !address.isEmpty()) {
                etCompanyAddress.setText(address);
            } else {
                etCompanyAddress.setText("");
            }
        }
        
        if (etCompanyDescription != null) {
            String description = currentCompany.getDescription();
            if (description != null && !description.isEmpty()) {
                etCompanyDescription.setText(description);
            } else {
                etCompanyDescription.setText("");
            }
        }
        
        // Cargar logo de la empresa
        String logoUrl = currentCompany.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty() && ivCompanyLogo != null) {
            Glide.with(this)
                    .load(logoUrl)
                    .centerCrop()
                    .into(ivCompanyLogo);
            if (placeholderLogo != null) {
                placeholderLogo.setVisibility(View.GONE);
            }
        }
        
        // Cargar imágenes promocionales
        java.util.List<String> coverUrls = currentCompany.getCoverImageUrls();
        if (coverUrls != null && !coverUrls.isEmpty()) {
            // Imagen 1
            if (coverUrls.size() > 0 && coverUrls.get(0) != null && !coverUrls.get(0).isEmpty()) {
                if (ivCompanyImage1 != null) {
                    Glide.with(this)
                            .load(coverUrls.get(0))
                            .centerCrop()
                            .into(ivCompanyImage1);
                    if (placeholderImage1 != null) {
                        placeholderImage1.setVisibility(View.GONE);
                    }
                }
            }
            
            // Imagen 2
            if (coverUrls.size() > 1 && coverUrls.get(1) != null && !coverUrls.get(1).isEmpty()) {
                if (ivCompanyImage2 != null) {
                    Glide.with(this)
                            .load(coverUrls.get(1))
                            .centerCrop()
                            .into(ivCompanyImage2);
                    if (placeholderImage2 != null) {
                        placeholderImage2.setVisibility(View.GONE);
                    }
                }
            }
        }
        
        android.util.Log.d(TAG, "Datos de empresa cargados - Email: " + currentCompany.getEmail() + 
                ", Phone: " + currentCompany.getPhone() + ", Address: " + currentCompany.getAddress() +
                ", LogoUrl: " + logoUrl);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeViews() {
        etCompanyName = findViewById(R.id.et_company_name);
        etCompanyEmail = findViewById(R.id.et_company_email);
        etCompanyPhone = findViewById(R.id.et_company_phone);
        etCompanyAddress = findViewById(R.id.et_company_address);
        etCompanyRuc = findViewById(R.id.et_company_ruc);
        etCompanyDescription = findViewById(R.id.et_company_description);
        actBusinessType = findViewById(R.id.act_business_type);

        cardLogo = findViewById(R.id.card_company_logo);
        cardImage1 = findViewById(R.id.card_image1);
        cardImage2 = findViewById(R.id.card_image2);
        cardMapPreview = findViewById(R.id.card_map_preview);
        
        // ImageViews
        ivCompanyLogo = findViewById(R.id.iv_company_logo);
        ivCompanyImage1 = findViewById(R.id.iv_company_image1);
        ivCompanyImage2 = findViewById(R.id.iv_company_image2);
        
        // Placeholders
        placeholderLogo = findViewById(R.id.placeholder_logo);
        placeholderImage1 = findViewById(R.id.placeholder_image1);
        placeholderImage2 = findViewById(R.id.placeholder_image2);

        btnSelectLocation = findViewById(R.id.btn_select_location);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save_company);

        // Map views
        mapView = findViewById(R.id.map_view);
        chipCoordinates = findViewById(R.id.chip_coordinates);
        mapPlaceholder = findViewById(R.id.map_placeholder);
    }

    private void initializeMapView(Bundle savedInstanceState) {
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
    }

    private void setupClickListeners() {
        // Logo de la empresa
        if (cardLogo != null) {
            cardLogo.setOnClickListener(v -> {
                currentImageSelection = 0;
                imagePickerLauncher.launch("image/*");
            });
        }
        
        // Imagen promocional 1
        if (cardImage1 != null) {
            cardImage1.setOnClickListener(v -> {
                currentImageSelection = 1;
                imagePickerLauncher.launch("image/*");
            });
        }

        // Imagen promocional 2
        if (cardImage2 != null) {
            cardImage2.setOnClickListener(v -> {
                currentImageSelection = 2;
                imagePickerLauncher.launch("image/*");
            });
        }

        if (btnSelectLocation != null) {
            btnSelectLocation.setOnClickListener(v -> {
                openLocationSelection();
            });
        }

        if (cardMapPreview != null) {
            cardMapPreview.setOnClickListener(v -> {
                if (selectedLocation != null) {
                    openFullMapView();
                } else {
                    Toast.makeText(this, "Primero selecciona una ubicación", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }
    
    private void handleImageSelected(Uri uri) {
        switch (currentImageSelection) {
            case 0: // Logo
                logoUri = uri;
                if (ivCompanyLogo != null) {
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(ivCompanyLogo);
                    if (placeholderLogo != null) {
                        placeholderLogo.setVisibility(View.GONE);
                    }
                }
                break;
            case 1: // Imagen 1
                image1Uri = uri;
                if (ivCompanyImage1 != null) {
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(ivCompanyImage1);
                    if (placeholderImage1 != null) {
                        placeholderImage1.setVisibility(View.GONE);
                    }
                }
                break;
            case 2: // Imagen 2
                image2Uri = uri;
                if (ivCompanyImage2 != null) {
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(ivCompanyImage2);
                    if (placeholderImage2 != null) {
                        placeholderImage2.setVisibility(View.GONE);
                    }
                }
                break;
        }
    }
    
    private void setupSaveButton() {
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (validateInputs()) {
                    saveCompanyInfo();
                }
            });
        }

        // Listener para geocodificación desde la dirección
        if (etCompanyAddress != null) {
            etCompanyAddress.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && !etCompanyAddress.getText().toString().trim().isEmpty()) {
                    geocodeAddress(etCompanyAddress.getText().toString().trim());
                }
            });
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Configuración básica del mapa
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);

        // Posición inicial (Lima, Perú)
        LatLng defaultLocation = new LatLng(-12.0464, -77.0428);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));

        // Listener para clics en el mapa
        googleMap.setOnMapClickListener(latLng -> {
            setSelectedLocation(latLng);
            updateAddressFromLocation(latLng);
        });
    }

    private void openLocationSelection() {
        if (googleMap != null) {
            Toast.makeText(this, "Toca en el mapa para seleccionar ubicación", Toast.LENGTH_LONG).show();
            mapPlaceholder.setVisibility(View.GONE);
            mapView.setVisibility(View.VISIBLE);
        }
    }

    private void setSelectedLocation(LatLng location) {
        this.selectedLocation = location;
        showSelectedLocationOnMap();

        // Actualizar chip de coordenadas
        chipCoordinates.setText(String.format(Locale.getDefault(),
                "Lat: %.4f, Lng: %.4f", location.latitude, location.longitude));
        chipCoordinates.setVisibility(View.VISIBLE);

        // Ocultar placeholder y mostrar mapa
        mapPlaceholder.setVisibility(View.GONE);
        mapView.setVisibility(View.VISIBLE);
    }

    private void showSelectedLocationOnMap() {
        if (googleMap != null && selectedLocation != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(selectedLocation)
                    .title("Ubicación de la empresa"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
        }
    }

    private void geocodeAddress(String address) {
        if (address.isEmpty()) return;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address result = addresses.get(0);
                LatLng location = new LatLng(result.getLatitude(), result.getLongitude());
                setSelectedLocation(location);
                Toast.makeText(this, "Ubicación encontrada en el mapa", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No se pudo encontrar la ubicación en el mapa", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error al buscar ubicación: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAddressFromLocation(LatLng location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder fullAddress = new StringBuilder();

                // Construir dirección completa
                if (address.getThoroughfare() != null) {
                    fullAddress.append(address.getThoroughfare());
                }
                if (address.getSubAdminArea() != null) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getSubAdminArea()); // Distrito
                }
                if (address.getAdminArea() != null) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getAdminArea()); // Provincia/Departamento
                }

                if (etCompanyAddress != null && fullAddress.length() > 0) {
                    etCompanyAddress.setText(fullAddress.toString());
                }
            }
        } catch (IOException e) {
            // No mostrar error, la dirección se mantiene como estaba
        }
    }

    private void openFullMapView() {
        if (selectedLocation != null) {
            Intent intent = new Intent(this, com.example.droidtour.TourMapActivity.class);
            intent.putExtra("latitude", selectedLocation.latitude);
            intent.putExtra("longitude", selectedLocation.longitude);
            intent.putExtra("title", "Ubicación de la empresa");
            startActivity(intent);
        }
    }

    private boolean validateInputs() {
        // Nombre, RUC y Tipo de empresa ya están validados (son campos bloqueados del superadmin)
        
        if (etCompanyEmail.getText().toString().trim().isEmpty()) {
            etCompanyEmail.setError("Ingrese el correo electrónico de la empresa");
            return false;
        }

        if (etCompanyPhone.getText().toString().trim().isEmpty()) {
            etCompanyPhone.setError("Ingrese el teléfono de la empresa");
            return false;
        }

        return true;
    }

    private void saveCompanyInfo() {
        if (currentCompany == null || currentCompanyId == null) {
            Toast.makeText(this, "Error: No hay empresa para actualizar", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar progreso
        btnSave.setEnabled(false);
        btnSave.setText("Guardando...");
        
        String email = etCompanyEmail.getText().toString().trim();
        String phone = etCompanyPhone.getText().toString().trim();
        String address = etCompanyAddress.getText().toString().trim();
        String description = etCompanyDescription != null ? etCompanyDescription.getText().toString().trim() : "";

        // Preparar mapa de actualizaciones
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("email", email);
        updates.put("phone", "+51" + phone);
        updates.put("address", address);
        updates.put("description", description);
        
        // Contador para saber cuántas imágenes subir
        int imagesToUpload = 0;
        if (logoUri != null) imagesToUpload++;
        if (image1Uri != null) imagesToUpload++;
        if (image2Uri != null) imagesToUpload++;
        
        if (imagesToUpload == 0) {
            // No hay imágenes que subir, guardar directamente
            updateCompanyInFirestore(updates);
        } else {
            // Subir imágenes primero
            uploadImagesAndSave(updates);
        }
    }
    
    private void uploadImagesAndSave(java.util.Map<String, Object> updates) {
        final int[] uploadedCount = {0};
        final int[] totalToUpload = {0};
        final boolean[] hasError = {false};
        
        if (logoUri != null) totalToUpload[0]++;
        if (image1Uri != null) totalToUpload[0]++;
        if (image2Uri != null) totalToUpload[0]++;
        
        com.example.droidtour.firebase.FirebaseStorageManager.StorageCallback callback = 
            new com.example.droidtour.firebase.FirebaseStorageManager.StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    uploadedCount[0]++;
                    if (uploadedCount[0] == totalToUpload[0] && !hasError[0]) {
                        // Todas las imágenes subidas, guardar empresa
                        updateCompanyInFirestore(updates);
                    }
                }
                
                @Override
                public void onFailure(Exception e) {
                    if (!hasError[0]) {
                        hasError[0] = true;
                        runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Guardar Cambios");
                            Toast.makeText(CompanyInfoActivity.this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                
                @Override
                public void onProgress(int progress) {
                    android.util.Log.d(TAG, "Progreso de subida: " + progress + "%");
                }
            };
        
        // Subir logo
        if (logoUri != null) {
            storageManager.uploadCompanyLogo(currentCompanyId, logoUri, new com.example.droidtour.firebase.FirebaseStorageManager.StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    updates.put("logoUrl", downloadUrl);
                    callback.onSuccess(downloadUrl);
                }
                
                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
                
                @Override
                public void onProgress(int progress) {
                    callback.onProgress(progress);
                }
            });
        }
        
        // Subir imágenes promocionales
        java.util.List<String> coverUrls = new java.util.ArrayList<>();
        if (currentCompany.getCoverImageUrls() != null) {
            coverUrls.addAll(currentCompany.getCoverImageUrls());
        }
        
        if (image1Uri != null) {
            storageManager.uploadCompanyCover(currentCompanyId, image1Uri, new com.example.droidtour.firebase.FirebaseStorageManager.StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    if (coverUrls.size() > 0) {
                        coverUrls.set(0, downloadUrl);
                    } else {
                        coverUrls.add(downloadUrl);
                    }
                    updates.put("coverImageUrls", coverUrls);
                    callback.onSuccess(downloadUrl);
                }
                
                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
                
                @Override
                public void onProgress(int progress) {
                    callback.onProgress(progress);
                }
            });
        }
        
        if (image2Uri != null) {
            storageManager.uploadCompanyCover(currentCompanyId, image2Uri, new com.example.droidtour.firebase.FirebaseStorageManager.StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    if (coverUrls.size() > 1) {
                        coverUrls.set(1, downloadUrl);
                    } else {
                        coverUrls.add(downloadUrl);
                    }
                    updates.put("coverImageUrls", coverUrls);
                    callback.onSuccess(downloadUrl);
                }
                
                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
                
                @Override
                public void onProgress(int progress) {
                    callback.onProgress(progress);
                }
            });
        }
    }
    
    private void updateCompanyInFirestore(java.util.Map<String, Object> updates) {
        firestoreManager.updateCompany(currentCompanyId, updates, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Guardar Cambios");
                    Toast.makeText(CompanyInfoActivity.this, "Empresa actualizada exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Guardar Cambios");
                    Toast.makeText(CompanyInfoActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Métodos del ciclo de vida para MapView
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);

        // Guardar ubicación seleccionada
        if (selectedLocation != null) {
            outState.putDouble("selected_lat", selectedLocation.latitude);
            outState.putDouble("selected_lng", selectedLocation.longitude);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restaurar ubicación seleccionada
        if (savedInstanceState.containsKey("selected_lat") && savedInstanceState.containsKey("selected_lng")) {
            double lat = savedInstanceState.getDouble("selected_lat");
            double lng = savedInstanceState.getDouble("selected_lng");
            selectedLocation = new LatLng(lat, lng);
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
        Intent intent = new Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}