package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.droidtour.client.CompanyChatActivity;
import com.example.droidtour.models.Tour;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class TourDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private TextView tvTourName, tvCompanyName, tvTourDescription, tvRating;
    private TextView tvDuration, tvGroupSize, tvLanguages, tvPriceBottom;
    private RecyclerView rvItinerary, rvReviews;
    private MaterialButton btnBookNow, btnSeeAllReviews, btnContact;
    private MaterialButton btnViewMap;
    private android.widget.LinearLayout layoutServices;
    private double basePrice = 0.0;
    private double additionalServicesPrice = 0.0;
    private GoogleMap meetingPointMap;

    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private Tour currentTour;
    private String tourId, tourName, companyName, companyId, imageUrl;
    private String currentUserId;
    private double price;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        com.example.droidtour.utils.PreferencesManager prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea CLIENT
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_tour_detail);

        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        authManager = com.example.droidtour.firebase.FirebaseAuthManager.getInstance(this);
        currentUserId = authManager.getCurrentUserId();
        
        getIntentData();
        setupToolbar();
        initializeViews();
        loadTourFromFirebase();
        setupRecyclerViews();
        setupClickListeners();
        checkExistingReservation();
        
        // Si viene desde reserva, ocultar bottom bar
        if (fromReservation) {
            android.view.View bottomBar = findViewById(R.id.bottom_bar_reservation);
            if (bottomBar != null) {
                bottomBar.setVisibility(android.view.View.GONE);
            }
        }
        
        // Inicializar mapa del punto de encuentro
        SupportMapFragment meetingMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.meeting_point_map);
        if (meetingMapFragment != null) {
            meetingMapFragment.getMapAsync(this);
        }
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        meetingPointMap = googleMap;
        
        // Deshabilitar gestos para el mapa pequeño
        meetingPointMap.getUiSettings().setScrollGesturesEnabled(false);
        meetingPointMap.getUiSettings().setZoomGesturesEnabled(false);
        meetingPointMap.getUiSettings().setRotateGesturesEnabled(false);
        meetingPointMap.getUiSettings().setTiltGesturesEnabled(false);
        
        // Actualizar mapa si ya tenemos los datos del tour
        if (currentTour != null) {
            updateMeetingPointMap();
        }
    }
    
    private void updateMeetingPointMap() {
        if (meetingPointMap != null && currentTour != null && 
            currentTour.getMeetingPointLatitude() != null && 
            currentTour.getMeetingPointLongitude() != null) {
            
            LatLng meetingPoint = new LatLng(
                currentTour.getMeetingPointLatitude(),
                currentTour.getMeetingPointLongitude()
            );
            
            meetingPointMap.clear();
            meetingPointMap.addMarker(new MarkerOptions()
                    .position(meetingPoint)
                    .title("Punto de Encuentro"));
            meetingPointMap.moveCamera(CameraUpdateFactory.newLatLngZoom(meetingPoint, 15));
        }
    }

    private boolean fromReservation = false;
    private java.util.ArrayList<String> reservedServiceIds;
    
    private void getIntentData() {
        tourId = getIntent().getStringExtra("tour_id");
        tourName = getIntent().getStringExtra("tour_name");
        companyName = getIntent().getStringExtra("company_name");
        companyId = getIntent().getStringExtra("company_id");
        price = getIntent().getDoubleExtra("price", 0.0);
        imageUrl = getIntent().getStringExtra("image_url");
        fromReservation = getIntent().getBooleanExtra("from_reservation", false);
        reservedServiceIds = getIntent().getStringArrayListExtra("reserved_service_ids");
        
        if (tourName == null) tourName = "Tour Increíble";
        if (companyName == null) companyName = "Empresa de Tours";
    }
    
    private void loadTourFromFirebase() {
        if (tourId != null) {
            firestoreManager.getTour(tourId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    currentTour = (Tour) result;
                    displayTourData();
                }
                
                @Override
                public void onFailure(Exception e) {
                    setupTourData(); // Fallback a datos locales
                }
            });
        } else {
            setupTourData();
        }
    }
    
    private void displayTourData() {
        if (currentTour == null) return;
        
        // Actualizar companyId si viene del tour
        if (currentTour.getCompanyId() != null && !currentTour.getCompanyId().isEmpty()) {
            companyId = currentTour.getCompanyId();
        }
        
        tvTourName.setText(currentTour.getTourName());
        tvCompanyName.setText("por " + currentTour.getCompanyName());
        tvPriceBottom.setText("S/. " + String.format("%.2f", currentTour.getPricePerPerson()));
        tvTourDescription.setText(currentTour.getDescription());
        
        // Quitar emoji de estrella porque ya hay ImageView con estrella
        Double avgRating = currentTour.getAverageRating();
        Integer totalReviews = currentTour.getTotalReviews();
        tvRating.setText(String.format("%.1f", avgRating != null ? avgRating : 0.0));
        
        TextView tvReviewsCount = findViewById(R.id.tv_reviews_count);
        if (tvReviewsCount != null) {
            tvReviewsCount.setText(" (" + (totalReviews != null ? totalReviews : 0) + " reseñas)");
        }
        
        tvDuration.setText(currentTour.getDuration());
        
        // Solo mostrar número sin "personas"
        tvGroupSize.setText(String.valueOf(currentTour.getMaxGroupSize()));
        
        // Convertir códigos ISO a nombres completos de idiomas
        if (currentTour.getLanguages() != null) {
            StringBuilder languagesText = new StringBuilder();
            for (int i = 0; i < currentTour.getLanguages().size(); i++) {
                String langCode = currentTour.getLanguages().get(i);
                languagesText.append(getLanguageName(langCode));
                if (i < currentTour.getLanguages().size() - 1) {
                    languagesText.append(", ");
                }
            }
            tvLanguages.setText(languagesText.toString());
        } else {
            tvLanguages.setText("Español, Inglés");
        }
        
        // Establecer precio base
        basePrice = currentTour.getPricePerPerson();
        
        // Cargar servicios del tour en segundo plano para evitar lag
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadTourServices();
            }
        }, 100);
        
        // Actualizar itinerario con paradas reales
        updateItineraryAdapter();
        
        // Actualizar punto de encuentro
        updateMeetingPoint();
        
        // Actualizar hora de encuentro
        TextView tvMeetingTime = findViewById(R.id.tv_meeting_time);
        if (tvMeetingTime != null) {
            String meetingTime = currentTour.getMeetingTime();
            if (meetingTime != null && !meetingTime.isEmpty()) {
                tvMeetingTime.setText(meetingTime);
            } else {
                tvMeetingTime.setText("No especificado");
            }
        }
        
        // Actualizar mapa del punto de encuentro
        updateMeetingPointMap();
    }
    
    private void updateMeetingPoint() {
        TextView tvMeetingPoint = findViewById(R.id.tv_meeting_point);
        if (tvMeetingPoint != null && currentTour != null) {
            String meetingPoint = currentTour.getMeetingPoint();
            if (meetingPoint != null && !meetingPoint.isEmpty()) {
                tvMeetingPoint.setText(meetingPoint);
            } else {
                tvMeetingPoint.setText("Punto de encuentro no especificado");
            }
        }
    }
    
    private void loadTourServices() {
        android.util.Log.d("TourDetail", "loadTourServices called");
        
        if (currentTour == null) {
            android.util.Log.e("TourDetail", "currentTour is null");
            return;
        }
        
        // IMPORTANTE: Usar includedServiceIds (IDs reales) en lugar de includedServices (nombres)
        android.util.Log.d("TourDetail", "IncludedServiceIds: " + (currentTour.getIncludedServiceIds() != null ? currentTour.getIncludedServiceIds().toString() : "null"));
        
        if (currentTour.getIncludedServiceIds() == null || currentTour.getIncludedServiceIds().isEmpty()) {
            if (layoutServices != null) {
                layoutServices.removeAllViews();
                TextView noServices = new TextView(this);
                noServices.setText("No hay servicios adicionales disponibles");
                noServices.setTextColor(0xFF757575);
                noServices.setTextSize(14);
                noServices.setPadding(0, 16, 0, 16);
                layoutServices.addView(noServices);
            }
            return;
        }
        
        if (layoutServices != null) {
            layoutServices.removeAllViews();
        }
        
        // Cargar cada servicio del tour usando los IDs correctos
        android.util.Log.d("TourDetail", "Loading " + currentTour.getIncludedServiceIds().size() + " services");
        for (String serviceId : currentTour.getIncludedServiceIds()) {
            android.util.Log.d("TourDetail", "Loading service: " + serviceId);
            firestoreManager.getServiceById(serviceId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    com.example.droidtour.models.Service service = (com.example.droidtour.models.Service) result;
                    if (service != null) {
                        android.util.Log.d("TourDetail", "Service loaded: " + service.getName());
                        addServiceToLayout(service);
                    } else {
                        android.util.Log.e("TourDetail", "Service is null");
                    }
                }
                
                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e("TourDetail", "Error loading service: " + e.getMessage(), e);
                }
            });
        }
    }
    
    private void addServiceToLayout(com.example.droidtour.models.Service service) {
        if (layoutServices == null) return;
        
        android.view.View serviceView = getLayoutInflater().inflate(R.layout.item_service_checkbox, layoutServices, false);
        
        com.google.android.material.checkbox.MaterialCheckBox cbService = serviceView.findViewById(R.id.cb_service);
        TextView tvServicePrice = serviceView.findViewById(R.id.tv_service_price);
        
        cbService.setText(service.getName());
        
        double servicePrice = service.getPrice();
        tvServicePrice.setText("S/. " + String.format("%.2f", servicePrice));
        
        // Si viene desde reserva, preseleccionar y bloquear servicios
        if (fromReservation && reservedServiceIds != null && reservedServiceIds.contains(service.getServiceId())) {
            cbService.setChecked(true);
            cbService.setEnabled(false); // Deshabilitar checkbox
            serviceView.setAlpha(0.7f); // Opacidad para indicar que está bloqueado
            additionalServicesPrice += service.getPrice();
            updateTotalPrice();
        } else if (!fromReservation) {
            // Solo permitir cambios si NO viene desde reserva
            cbService.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    additionalServicesPrice += service.getPrice();
                } else {
                    additionalServicesPrice -= service.getPrice();
                }
                updateTotalPrice();
            });
        }
        
        // Hacer clickeable todo el view para ver detalles
        serviceView.setOnClickListener(v -> showServiceDetailsDialog(service));
        
        layoutServices.addView(serviceView);
    }
    
    private void showServiceDetailsDialog(com.example.droidtour.models.Service service) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_service_details, null);
        
        // Inicializar vistas
        android.widget.ImageView ivServiceImage1 = dialogView.findViewById(R.id.iv_service_image1);
        android.widget.ImageView ivServiceImage2 = dialogView.findViewById(R.id.iv_service_image2);
        TextView tvServiceName = dialogView.findViewById(R.id.tv_service_name);
        TextView tvServicePrice = dialogView.findViewById(R.id.tv_service_price);
        TextView tvServiceDescription = dialogView.findViewById(R.id.tv_service_description);
        
        // Establecer datos
        tvServiceName.setText(service.getName());
        tvServicePrice.setText("S/. " + String.format("%.2f", service.getPrice()));
        
        if (service.getDescription() != null && !service.getDescription().isEmpty()) {
            tvServiceDescription.setText(service.getDescription());
        } else {
            tvServiceDescription.setText("Sin descripción disponible");
        }
        
        // Cargar imágenes con Glide optimizado
        if (service.getImageUrls() != null && !service.getImageUrls().isEmpty()) {
            // Imagen 1
            if (service.getImageUrls().size() > 0 && service.getImageUrls().get(0) != null && !service.getImageUrls().get(0).isEmpty()) {
                Glide.with(this)
                    .load(service.getImageUrls().get(0))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .thumbnail(0.1f)
                    .override(600, 600)
                    .centerCrop()
                    .into(ivServiceImage1);
            } else {
                ivServiceImage1.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            
            // Imagen 2
            if (service.getImageUrls().size() > 1 && service.getImageUrls().get(1) != null && !service.getImageUrls().get(1).isEmpty()) {
                Glide.with(this)
                    .load(service.getImageUrls().get(1))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .thumbnail(0.1f)
                    .override(600, 600)
                    .centerCrop()
                    .into(ivServiceImage2);
            } else {
                ivServiceImage2.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            ivServiceImage1.setImageResource(android.R.drawable.ic_menu_gallery);
            ivServiceImage2.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        builder.setView(dialogView)
               .setPositiveButton("Cerrar", null)
               .show();
    }
    
    private void updateTotalPrice() {
        double totalPrice = basePrice + additionalServicesPrice;
        if (tvPriceBottom != null) {
            tvPriceBottom.setText("S/. " + String.format("%.2f", totalPrice));
        }
    }
    
    private String getLanguageName(String isoCode) {
        switch (isoCode.toUpperCase()) {
            case "ES": return "Español";
            case "EN": return "Inglés";
            case "FR": return "Francés";
            case "DE": return "Alemán";
            case "IT": return "Italiano";
            case "PT": return "Portugués";
            case "ZH": return "Chino";
            case "JA": return "Japonés";
            case "KO": return "Coreano";
            case "RU": return "Ruso";
            default: return isoCode;
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvTourDescription = findViewById(R.id.tv_tour_description);
        tvRating = findViewById(R.id.tv_rating);
        tvDuration = findViewById(R.id.tv_duration);
        tvGroupSize = findViewById(R.id.tv_group_size);
        tvLanguages = findViewById(R.id.tv_languages);
        // El layout ahora define tv_price_bottom en la barra inferior
        tvPriceBottom = findViewById(R.id.tv_price_bottom);
        rvItinerary = findViewById(R.id.rv_itinerary);
        rvReviews = findViewById(R.id.rv_reviews);
        layoutServices = findViewById(R.id.layout_services);
        // Mapear botones a los nuevos IDs del layout
        btnBookNow = findViewById(R.id.btn_book_now);
        btnSeeAllReviews = findViewById(R.id.btn_see_all_reviews);
        btnContact = findViewById(R.id.btn_contact);
        btnViewMap = findViewById(R.id.btn_view_map);

        android.widget.ImageView headerImage = findViewById(R.id.iv_header_image);
        if (headerImage != null) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(headerImage);
        }
    }

    private void setupTourData() {
        // Evitar NullPointer si tourId es null
        if (tvTourName != null) tvTourName.setText(tourName);
        if (tvCompanyName != null) tvCompanyName.setText("por " + companyName);
        if (tvPriceBottom != null) tvPriceBottom.setText("S/. " + String.format("%.2f", price));

        if (tourId == null) {
            // Valores por defecto cuando no hay tourId
            if (tvTourDescription != null) tvTourDescription.setText("Una experiencia increíble que te permitirá conocer los mejores destinos de la región con guías expertos y servicios de primera calidad.");
            if (tvRating != null) tvRating.setText("⭐ 4.7 (65)");
            if (tvDuration != null) tvDuration.setText("6 horas");
            if (tvGroupSize != null) tvGroupSize.setText("10 personas");
            if (tvLanguages != null) tvLanguages.setText("ES, EN");
            return;
        }

        // Set data based on tour type (fallback mock)
        switch (Math.abs(tourId.hashCode()) % 6) {
            case 0:
                if (tvTourDescription != null) tvTourDescription.setText("Descubre la historia y cultura de Lima visitando sus principales atractivos del centro histórico. Un recorrido completo por la Plaza de Armas, Catedral, Palacio de Gobierno y los balcones coloniales más emblemáticos.");
                if (tvRating != null) tvRating.setText("⭐ 4.9 (127)");
                if (tvDuration != null) tvDuration.setText("4 horas");
                if (tvGroupSize != null) tvGroupSize.setText("12 personas");
                if (tvLanguages != null) tvLanguages.setText("ES, EN");
                break;
            case 1:
                if (tvTourDescription != null) tvTourDescription.setText("Visita la maravilla del mundo con transporte y guía incluido desde Cusco. Una experiencia única que incluye tren panorámico y tiempo suficiente para explorar la ciudadela inca.");
                if (tvRating != null) tvRating.setText("⭐ 4.8 (89)");
                if (tvDuration != null) tvDuration.setText("Full Day");
                if (tvGroupSize != null) tvGroupSize.setText("15 personas");
                if (tvLanguages != null) tvLanguages.setText("ES, EN, FR");
                break;
            default:
                if (tvTourDescription != null) tvTourDescription.setText("Una experiencia increíble que te permitirá conocer los mejores destinos de la región con guías expertos y servicios de primera calidad.");
                if (tvRating != null) tvRating.setText("⭐ 4.7 (65)");
                if (tvDuration != null) tvDuration.setText("6 horas");
                if (tvGroupSize != null) tvGroupSize.setText("10 personas");
                if (tvLanguages != null) tvLanguages.setText("ES, EN");
        }
    }

    private void setupRecyclerViews() {
        // Itinerary RecyclerView - Se actualizará cuando se cargue el tour
        if (rvItinerary != null) {
            rvItinerary.setLayoutManager(new LinearLayoutManager(this));
        }

        // Reviews RecyclerView
        if (rvReviews != null) {
            rvReviews.setLayoutManager(new LinearLayoutManager(this));
            rvReviews.setAdapter(new ReviewsAdapter());
        }
    }
    
    private void updateItineraryAdapter() {
        if (rvItinerary != null && currentTour != null) {
            java.util.List<Tour.TourStop> stops = currentTour.getStops();
            if (stops != null && !stops.isEmpty()) {
                // Ordenar paradas por orden
                java.util.Collections.sort(stops, (s1, s2) -> Integer.compare(s1.getOrder(), s2.getOrder()));
                rvItinerary.setAdapter(new ItineraryAdapter(stops));
            } else {
                rvItinerary.setAdapter(new ItineraryAdapter(new java.util.ArrayList<>()));
            }
        }
    }

    private void setupClickListeners() {

        if (btnSeeAllReviews != null) {
            btnSeeAllReviews.setOnClickListener(v -> {
                Intent intent = new Intent(this, AllReviewsActivity.class);
                intent.putExtra("tour_name", tourName);
                startActivity(intent);
            });
        }

        if (btnContact != null) {
            btnContact.setOnClickListener(v -> {
                Intent intent = new Intent(this, CompanyChatActivity.class);
                intent.putExtra("company_name", companyName);
                intent.putExtra("tour_name", tourName);
                
                // Obtener companyId: primero del tour actual, luego del intent, luego del tour cargado
                String finalCompanyId = null;
                if (currentTour != null && currentTour.getCompanyId() != null) {
                    finalCompanyId = currentTour.getCompanyId();
                } else if (companyId != null && !companyId.isEmpty()) {
                    finalCompanyId = companyId;
                }
                
                if (finalCompanyId != null && !finalCompanyId.isEmpty()) {
                    intent.putExtra("company_id", finalCompanyId);
                }
                
                startActivity(intent);
            });
        }

        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                if (currentTour == null) {
                    Toast.makeText(this, "Cargando datos del tour...", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Intent intent = new Intent(this, TourBookingActivity.class);
                intent.putExtra("tour_id", tourId);
                intent.putExtra("tour_name", tourName);
                intent.putExtra("company_id", companyId);
                intent.putExtra("company_name", companyName);
                intent.putExtra("price", currentTour.getPricePerPerson());
                intent.putExtra("service_price", additionalServicesPrice);
                
                // Pasar fecha del tour si existe
                if (currentTour.getTourDate() != null && !currentTour.getTourDate().isEmpty()) {
                    intent.putExtra("tour_date", currentTour.getTourDate());
                } else {
                    intent.putExtra("tour_date", "Por confirmar");
                }
                
                // Pasar hora del tour si existe
                if (currentTour.getMeetingTime() != null && !currentTour.getMeetingTime().isEmpty()) {
                    intent.putExtra("tour_time", currentTour.getMeetingTime());
                } else {
                    intent.putExtra("tour_time", "09:00");
                }
                
                startActivity(intent);
            });
        }

        // Botón "Ver mapa" de itinerario - muestra mapa con todas las paradas
        MaterialButton btnViewStopsMap = findViewById(R.id.btn_view_stops_map);
        if (btnViewStopsMap != null) {
            btnViewStopsMap.setOnClickListener(v -> {
                if (currentTour != null && currentTour.getStops() != null && !currentTour.getStops().isEmpty()) {
                    showStopsMapDialog();
                } else {
                    Toast.makeText(this, "No hay paradas disponibles", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Botón "Ver mapa" del punto de encuentro
        if (btnViewMap != null) {
            btnViewMap.setOnClickListener(v -> {
                if (currentTour != null && currentTour.getMeetingPointLatitude() != null && currentTour.getMeetingPointLongitude() != null) {
                    openMeetingPointMap();
                } else {
                    Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void showStopsMapDialog() {
        // Abrir actividad con mapa completo de paradas
        Intent intent = new Intent(this, TourStopsMapActivity.class);
        
        // Pasar lista de paradas (TourStop ahora es Serializable)
        if (currentTour != null && currentTour.getStops() != null) {
            intent.putExtra("stops", new java.util.ArrayList<>(currentTour.getStops()));
        }
        
        startActivity(intent);
    }
    
    private void openMeetingPointMap() {
        // Abrir actividad con mapa grande del punto de encuentro
        Intent intent = new Intent(this, MeetingPointMapActivity.class);
        
        if (currentTour != null) {
            intent.putExtra("latitude", currentTour.getMeetingPointLatitude());
            intent.putExtra("longitude", currentTour.getMeetingPointLongitude());
            intent.putExtra("locationName", currentTour.getMeetingPoint());
        }
        
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Verificar si el usuario ya tiene una reserva confirmada para este tour
     */
    private void checkExistingReservation() {
        // Si viene desde "Ver Detalles" de una reserva, no verificar
        if (fromReservation) {
            return;
        }
        
        if (currentUserId == null || tourId == null) return;
        
        firestoreManager.hasConfirmedReservation(currentUserId, tourId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Boolean hasReservation = (Boolean) result;
                if (hasReservation != null && hasReservation) {
                    // Usuario ya tiene una reserva confirmada
                    btnBookNow.setEnabled(false);
                    btnBookNow.setText("Ya tienes una reserva");
                    btnBookNow.setAlpha(0.5f);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Si falla la verificación, permitir reservar de todas formas
                android.util.Log.e("TourDetail", "Error verificando reserva: " + e.getMessage());
            }
        });
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

// Adaptador para el itinerario
class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {
    private java.util.List<Tour.TourStop> stops;

    ItineraryAdapter(java.util.List<Tour.TourStop> stops) {
        this.stops = stops != null ? stops : new java.util.ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_itinerary_point, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (stops.isEmpty()) return;
        
        Tour.TourStop stop = stops.get(position);
        
        TextView time = holder.itemView.findViewById(R.id.tv_time);
        TextView locationName = holder.itemView.findViewById(R.id.tv_location_name);
        TextView activityDescription = holder.itemView.findViewById(R.id.tv_activity_description);
        TextView duration = holder.itemView.findViewById(R.id.tv_duration);
        android.view.View timelineLine = holder.itemView.findViewById(R.id.view_timeline_line);
        
        time.setText(stop.getTime() != null ? stop.getTime() : "");
        locationName.setText(stop.getName() != null ? stop.getName() : "Parada " + (position + 1));
        activityDescription.setText(stop.getDescription() != null ? stop.getDescription() : "");
        
        if (stop.getStopDuration() != null && stop.getStopDuration() > 0) {
            duration.setText("⏱️ " + stop.getStopDuration() + " minutos");
        } else {
            duration.setVisibility(android.view.View.GONE);
        }
        
        // Hide timeline line for last item
        if (position == getItemCount() - 1) {
            timelineLine.setVisibility(android.view.View.INVISIBLE);
        } else {
            timelineLine.setVisibility(android.view.View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() { return stops.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

// Adaptador para las reseñas
class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TextView userInitial = holder.itemView.findViewById(R.id.tv_user_initial);
        TextView userName = holder.itemView.findViewById(R.id.tv_user_name);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView reviewText = holder.itemView.findViewById(R.id.tv_review_text);
        TextView reviewDate = holder.itemView.findViewById(R.id.tv_review_date);

        String[] names = {"Ana García", "Carlos Mendoza", "María López"};
        String[] initials = {"A", "C", "M"};
        String[] ratings = {"⭐⭐⭐⭐⭐", "⭐⭐⭐⭐⭐", "⭐⭐⭐⭐"};
        String[] reviews = {
            "Excelente tour, el guía muy conocedor y amable. Los lugares visitados fueron increíbles y la comida deliciosa.",
            "Una experiencia inolvidable. La organización fue perfecta y aprendimos mucho sobre la historia de Lima.",
            "Muy recomendado. El tour cumplió todas nuestras expectativas y el precio es muy justo."
        };
        String[] dates = {"Hace 2 semanas", "Hace 1 mes", "Hace 3 semanas"};

        int index = position % names.length;
        
        userInitial.setText(initials[index]);
        userName.setText(names[index]);
        rating.setText(ratings[index]);
        reviewText.setText(reviews[index]);
        reviewDate.setText(dates[index]);
    }

    @Override
    public int getItemCount() { return 3; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}
