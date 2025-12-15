package com.example.droidtour;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Tour;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class LocationTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "LocationTracking";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap mMap;
    private TextView tvTourName, tvPointsCompleted;
    private MaterialButton btnShowStops;
    private android.widget.ProgressBar progressLoading;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private FirebaseFirestore db;
    private String tourId;
    private Tour currentTour;
    private ListenerRegistration tourListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea un guía
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_location_tracking);

        firestoreManager = FirestoreManager.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Obtener tour ID desde el intent
        tourId = getIntent().getStringExtra("tour_id");
        if (tourId == null || tourId.isEmpty()) {
            Toast.makeText(this, "Error: Tour ID no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        
        // Cargar mapa y datos de forma asíncrona en segundo plano
        new Thread(() -> {
            // Esperar un momento antes de inicializar el mapa
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            runOnUiThread(() -> {
                setupMap();
                
                // Cargar datos después de que el mapa esté inicializado
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    loadTourData();
                    setupRealtimeListener();
                }, 300);
            });
        }).start();
    }

    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvPointsCompleted = findViewById(R.id.tv_points_completed);
        btnShowStops = findViewById(R.id.btn_show_stops);
        progressLoading = findViewById(R.id.progress_loading);
        
        btnShowStops.setOnClickListener(v -> {
            if (tourId != null && currentTour != null) {
                Intent intent = new Intent(this, GuideStopsManagementActivity.class);
                intent.putExtra(GuideStopsManagementActivity.EXTRA_TOUR_ID, tourId);
                startActivity(intent);
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "🗺️ Mapa listo");
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        
        // Verificar y solicitar permisos de ubicación
        checkLocationPermission();
        
        // Configurar click en marcadores para marcar paradas como completadas
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() != null && marker.getTag() instanceof Integer) {
                int stopIndex = (int) marker.getTag();
                toggleStopCompleted(stopIndex);
                return true;
            }
            return false;
        });
        
        // Si ya tenemos el tour, mostrar paradas
        if (currentTour != null) {
            updateMapWithStops();
        }
    }
    
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            // Permiso ya concedido
            enableMyLocation();
        } else {
            // Solicitar permiso
            Log.d(TAG, "📍 Solicitando permisos de ubicación");
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, 
                             Manifest.permission.ACCESS_COARSE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    private void enableMyLocation() {
        if (mMap == null) return;
        
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                Log.d(TAG, "✅ Mi ubicación habilitada");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Error de permisos de ubicación", e);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Permisos de ubicación concedidos");
                enableMyLocation();
            } else {
                Log.d(TAG, "❌ Permisos de ubicación denegados");
                Toast.makeText(this, "Permisos de ubicación necesarios para mostrar tu posición", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void loadTourData() {
        Log.d(TAG, "🔄 Cargando datos del tour: " + tourId);
        
        firestoreManager.getTourById(tourId, new FirestoreManager.TourCallback() {
            @Override
            public void onSuccess(Tour tour) {
                currentTour = tour;
                if (progressLoading != null) {
                    progressLoading.setVisibility(android.view.View.GONE);
                }
                Log.d(TAG, "✅ Tour cargado: " + currentTour.getTourName());
                
                if (currentTour.getStops() != null) {
                    Log.d(TAG, "   📍 Paradas totales: " + currentTour.getStops().size());
                } else {
                    Log.d(TAG, "   ⚠️ Tour no tiene paradas");
                }
                
                displayTourInfo();
                updateMapWithStops();
            }
            
            @Override
            public void onFailure(String error) {
                if (progressLoading != null) {
                    progressLoading.setVisibility(android.view.View.GONE);
                }
                Log.e(TAG, "❌ Error cargando tour: " + error);
                Toast.makeText(LocationTrackingActivity.this, 
                    "Error al cargar tour: " + error, 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupRealtimeListener() {
        if (tourListener != null) {
            Log.d(TAG, "⚠️ Listener ya configurado, omitiendo duplicado");
            return;
        }
        
        Log.d(TAG, "🔔 Configurando listener en tiempo real para tour: " + tourId);
        
        // Listener en tiempo real para actualizar cuando se completen paradas
        tourListener = db.collection("Tours").document(tourId)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to tour updates", error);
                    return;
                }
                
                if (snapshot != null && snapshot.exists()) {
                    Tour updatedTour = snapshot.toObject(Tour.class);
                    if (updatedTour != null) {
                        currentTour = updatedTour;
                        Log.d(TAG, "🔔 Tour actualizado en tiempo real");
                        runOnUiThread(() -> {
                            displayTourInfo();
                            updateMapWithStops();
                        });
                    }
                }
            });
    }
    
    private void displayTourInfo() {
        if (currentTour == null) return;
        
        tvTourName.setText(currentTour.getTourName());
        
        // Calcular paradas completadas
        int total = 0;
        int completed = 0;
        if (currentTour.getStops() != null) {
            total = currentTour.getStops().size();
            for (Tour.TourStop stop : currentTour.getStops()) {
                if (stop.getCompleted() != null && stop.getCompleted()) {
                    completed++;
                }
            }
        }
        
        tvPointsCompleted.setText(completed + "/" + total + " paradas completadas");
    }
    
    private void updateMapWithStops() {
        if (mMap == null) {
            Log.d(TAG, "⚠️ Mapa no está listo");
            return;
        }
        
        if (currentTour == null || currentTour.getStops() == null) {
            Log.d(TAG, "⚠️ No hay datos del tour para mostrar");
            return;
        }
        
        Log.d(TAG, "🗺️ Actualizando mapa con paradas");
        mMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasMarkers = false;
        
        // Punto de encuentro (AZUL)
        if (currentTour.getMeetingPointLatitude() != null && currentTour.getMeetingPointLongitude() != null) {
            LatLng meetingLatLng = new LatLng(currentTour.getMeetingPointLatitude(), currentTour.getMeetingPointLongitude());
            mMap.addMarker(new MarkerOptions()
                .position(meetingLatLng)
                .title("📍 Punto de Encuentro")
                .snippet(currentTour.getMeetingPoint())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            boundsBuilder.include(meetingLatLng);
            hasMarkers = true;
            Log.d(TAG, "   ✅ Marcador de punto de encuentro agregado");
        }
        
        // Paradas (ROJO = pendiente, VERDE = completada)
        if (currentTour.getStops() != null && !currentTour.getStops().isEmpty()) {
            Log.d(TAG, "   📍 Agregando " + currentTour.getStops().size() + " paradas");
            for (int i = 0; i < currentTour.getStops().size(); i++) {
                Tour.TourStop stop = currentTour.getStops().get(i);
                LatLng stopLatLng = new LatLng(stop.getLatitude(), stop.getLongitude());
                boolean isCompleted = stop.getCompleted() != null && stop.getCompleted();
                
                float markerColor = isCompleted ? 
                    BitmapDescriptorFactory.HUE_GREEN : 
                    BitmapDescriptorFactory.HUE_RED;
                
                String title = stop.getOrder() + ". " + stop.getName();
                String snippet = "Toca para marcar como " + (isCompleted ? "pendiente" : "completada");
                if (stop.getTime() != null && !stop.getTime().isEmpty()) {
                    snippet = stop.getTime() + " - " + snippet;
                }
                if (isCompleted) {
                    title += " ✓";
                }
                
                com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(stopLatLng)
                    .title(title)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                
                // Guardar el índice de la parada en el tag del marcador
                if (marker != null) {
                    marker.setTag(i);
                }
                
                boundsBuilder.include(stopLatLng);
                hasMarkers = true;
                
                Log.d(TAG, "      " + (isCompleted ? "✅" : "🔴") + " Parada " + stop.getOrder() + ": " + stop.getName());
            }
        } else {
            Log.d(TAG, "   ⚠️ No hay paradas para mostrar");
        }
        
        // Ajustar cámara
        if (hasMarkers) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 150;
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                Log.d(TAG, "   ✅ Cámara ajustada a todos los marcadores");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error ajustando cámara", e);
            }
        } else {
            Log.d(TAG, "   ⚠️ No hay marcadores para ajustar cámara");
        }
    }
    
    private void toggleStopCompleted(int stopIndex) {
        if (currentTour == null || currentTour.getStops() == null || 
            stopIndex < 0 || stopIndex >= currentTour.getStops().size()) {
            return;
        }
        
        Tour.TourStop stop = currentTour.getStops().get(stopIndex);
        boolean currentStatus = stop.getCompleted() != null && stop.getCompleted();
        boolean newStatus = !currentStatus;
        
        Log.d(TAG, "🔄 Cambiando estado de parada " + stop.getOrder() + ": " + stop.getName());
        Log.d(TAG, "   Estado actual: " + (currentStatus ? "Completada" : "Pendiente"));
        Log.d(TAG, "   Nuevo estado: " + (newStatus ? "Completada" : "Pendiente"));
        
        // Actualizar localmente
        stop.setCompleted(newStatus);
        if (newStatus) {
            stop.setCompletedAt(new java.util.Date());
        } else {
            stop.setCompletedAt(null);
        }
        
        // Actualizar en Firestore
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("stops", currentTour.getStops());
        
        firestoreManager.updateTour(tourId, updates, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "✅ Parada actualizada en Firestore");
                String message = newStatus ? "✅ Parada completada" : "🔄 Parada marcada como pendiente";
                Toast.makeText(LocationTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                
                // El listener en tiempo real actualizará el mapa automáticamente
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error actualizando parada", e);
                Toast.makeText(LocationTrackingActivity.this, 
                    "Error al actualizar parada: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                
                // Revertir cambio local
                stop.setCompleted(currentStatus);
                stop.setCompletedAt(null);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tourListener != null) {
            tourListener.remove();
        }
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
