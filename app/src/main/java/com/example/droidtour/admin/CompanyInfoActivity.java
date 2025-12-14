package com.example.droidtour.admin;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.R;
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

    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private String currentUserId;

    private TextInputEditText etCompanyName, etCompanyEmail, etCompanyPhone, etCompanyAddress;
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

        setContentView(R.layout.activity_company_info);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        authManager = com.example.droidtour.firebase.FirebaseAuthManager.getInstance(this);
        //currentUserId = authManager.getCurrentUserId();

        setupToolbar();
        initializeViews();
        initializeMapView(savedInstanceState);
        setupClickListeners();
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

        cardImage1 = findViewById(R.id.card_image1);
        cardImage2 = findViewById(R.id.card_image2);
        cardMapPreview = findViewById(R.id.card_map_preview);

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
        if (cardImage1 != null) {
            cardImage1.setOnClickListener(v -> {
                Toast.makeText(this, "Seleccionar imagen 1", Toast.LENGTH_SHORT).show();
                // TODO: Implementar selección de imagen
            });
        }

        if (cardImage2 != null) {
            cardImage2.setOnClickListener(v -> {
                Toast.makeText(this, "Seleccionar imagen 2", Toast.LENGTH_SHORT).show();
                // TODO: Implementar selección de imagen
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
        if (etCompanyName.getText().toString().trim().isEmpty()) {
            etCompanyName.setError("Ingrese el nombre de la empresa");
            return false;
        }

        if (etCompanyEmail.getText().toString().trim().isEmpty()) {
            etCompanyEmail.setError("Ingrese el correo electrónico");
            return false;
        }

        if (etCompanyPhone.getText().toString().trim().isEmpty()) {
            etCompanyPhone.setError("Ingrese el teléfono");
            return false;
        }

        if (etCompanyAddress.getText().toString().trim().isEmpty()) {
            etCompanyAddress.setError("Ingrese la dirección");
            return false;
        }

        return true;
    }

    private void saveCompanyInfo() {
        String name = etCompanyName.getText().toString().trim();
        String email = etCompanyEmail.getText().toString().trim();
        String phone = etCompanyPhone.getText().toString().trim();
        String address = etCompanyAddress.getText().toString().trim();

        // Generar ID único para la empresa
        String companyId = "COMP_" + System.currentTimeMillis();

        com.example.droidtour.models.Company company = new com.example.droidtour.models.Company();
        company.setBusinessName(name);
        company.setCommercialName(name); // Mismo nombre si no hay comercial
        company.setRuc(""); // Dejar vacío o pedir al usuario
        company.setBusinessType(""); // Dejar vacío
        company.setAdminUserId(currentUserId);
        company.setEmail(email);
        company.setPhone(phone);
        company.setAddress(address);
        company.setStatus("active");

        firestoreManager.createCompany(company, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(CompanyInfoActivity.this, "Empresa guardada exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CompanyInfoActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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