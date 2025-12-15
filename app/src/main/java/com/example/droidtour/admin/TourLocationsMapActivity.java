package com.example.droidtour.admin;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.List;

public class TourLocationsMapActivity extends AppCompatActivity
        implements OnMapReadyCallback, TourLocationsAdapter.OnLocationDeleteListener {

    private GoogleMap googleMap;
    private final ArrayList<TourLocation> locations = new ArrayList<>();
    private TourLocationsAdapter adapter;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    
    // Modo de punto único (para punto de encuentro)
    private boolean singleLocationMode = false;
    private String customTitle = "Ubicaciones del Tour";
    private TourLocation singleSelectedLocation = null;

    private final List<Marker> markers = new ArrayList<>();
    private Polyline routePolyline;

    // Views del peek fijo (fuera del bottom sheet)
    private View peekViewContainer;
    private TextView tvPeekSummary;
    private ImageView ivExpandIndicator;

    // Views del bottom sheet modal
    private View bottomSheetModal;
    private TextView tvTotalStops;
    private TextView tvTotalDistance;
    private View layoutEmptyLocations;

    private int selectedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_locations_map);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Verificar si es modo de punto único
        Intent intent = getIntent();
        if (intent != null) {
            singleLocationMode = intent.getBooleanExtra("singleLocationMode", false);
            customTitle = intent.getStringExtra("title");
            if (customTitle == null) customTitle = "Ubicaciones del Tour";
        }

        setupToolbar();
        setupPeekView();
        setupBottomSheet();

        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(customTitle);
        }
        
        // Actualizar subtitle dependiendo del modo
        if (singleLocationMode) {
            toolbar.setSubtitle("Toca el mapa para agregar el punto");
        } else {
            toolbar.setSubtitle("Toca el mapa para agregar paradas");
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupPeekView() {
        // El peek ahora es independiente del bottom sheet
        peekViewContainer = findViewById(R.id.peek_view_container);
        tvPeekSummary = findViewById(R.id.tv_peek_summary);
        ivExpandIndicator = findViewById(R.id.iv_expand_indicator);

        // Ocultar peek view en modo de selección única (punto de encuentro)
        if (singleLocationMode) {
            if (peekViewContainer != null) {
                peekViewContainer.setVisibility(View.GONE);
            }
            return;
        }

        // Click en el peek para abrir el bottom sheet modal
        peekViewContainer.setOnClickListener(v -> openBottomSheet());

        updatePeekInfo();
    }

    private void setupBottomSheet() {
        bottomSheetModal = findViewById(R.id.bottom_sheet_modal);
        
        // Ocultar bottom sheet en modo de selección única (punto de encuentro)
        if (singleLocationMode) {
            if (bottomSheetModal != null) {
                bottomSheetModal.setVisibility(View.GONE);
            }
            return;
        }
        
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetModal);

        // Configurar como modal (oculto por defecto)
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setSkipCollapsed(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Referencias a las vistas del bottom sheet
        tvTotalStops = bottomSheetModal.findViewById(R.id.tv_total_stops);
        tvTotalDistance = bottomSheetModal.findViewById(R.id.tv_total_distance);
        layoutEmptyLocations = bottomSheetModal.findViewById(R.id.layout_empty_locations);

        // Configurar RecyclerView
        RecyclerView rv = bottomSheetModal.findViewById(R.id.rv_locations);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TourLocationsAdapter(locations);
        adapter.setOnLocationDeleteListener(this);
        adapter.setOnLocationClickListener(this::onLocationClicked);
        rv.setAdapter(adapter);

        // Botón cerrar
        View btnClose = bottomSheetModal.findViewById(R.id.btn_close_sheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> closeBottomSheet());
        }

        // Botón limpiar todo
        View btnClearAll = bottomSheetModal.findViewById(R.id.btn_clear_all);
        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> clearAllLocations());
        }

        // Botón guardar
        View btnSave = bottomSheetModal.findViewById(R.id.btn_save_route);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> returnResult());
        }

        // Configurar drag & drop
        setupItemTouchHelper(rv);

        // Listener de cambios de estado
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                updateUIForBottomSheetState(newState);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Opcional: animar el peek mientras se desliza el bottom sheet
                if (peekViewContainer != null) {
                    peekViewContainer.setAlpha(1f - (slideOffset * 0.3f));
                }
            }
        });

        updateEmptyState();
    }

    private void setupItemTouchHelper(RecyclerView rv) {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                TourLocation movedLocation = locations.remove(fromPosition);
                locations.add(toPosition, movedLocation);

                adapter.notifyItemMoved(fromPosition, toPosition);
                updateLocationsOrder();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // No usado
            }
        });
        itemTouchHelper.attachToRecyclerView(rv);
    }

    private void openBottomSheet() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetModal.setVisibility(View.VISIBLE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            // Animar la flecha del indicador
            if (ivExpandIndicator != null) {
                ivExpandIndicator.animate()
                        .rotation(180)
                        .setDuration(300)
                        .start();
            }
        }
    }

    private void closeBottomSheet() {
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            // Animar la flecha del indicador
            if (ivExpandIndicator != null) {
                ivExpandIndicator.animate()
                        .rotation(0)
                        .setDuration(300)
                        .start();
            }
        }
    }

    private void updateUIForBottomSheetState(int state) {
        switch (state) {
            case BottomSheetBehavior.STATE_HIDDEN:
                // Bottom sheet oculto - peek visible
                bottomSheetModal.setVisibility(View.GONE);
                if (peekViewContainer != null) {
                    peekViewContainer.setVisibility(View.VISIBLE);
                    peekViewContainer.setAlpha(1f);
                }
                if (ivExpandIndicator != null) {
                    ivExpandIndicator.setRotation(0);
                }
                break;

            case BottomSheetBehavior.STATE_EXPANDED:
                // Bottom sheet expandido - ocultar peek view
                bottomSheetModal.setVisibility(View.VISIBLE);
                if (peekViewContainer != null) {
                    peekViewContainer.setVisibility(View.GONE);
                }
                if (ivExpandIndicator != null) {
                    ivExpandIndicator.setRotation(180);
                }
                break;

            case BottomSheetBehavior.STATE_DRAGGING:
            case BottomSheetBehavior.STATE_SETTLING:
                // Estados transitorios
                break;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Configurar el mapa
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        // Posición inicial (Lima, Peru)
        LatLng initialPosition = new LatLng(-12.0464, -77.0428);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 12));

        if (singleLocationMode) {
            // Modo de selección única para punto de encuentro
            map.setOnMapClickListener(this::selectSingleLocation);
        } else {
            // Modo normal para múltiples ubicaciones
            map.setOnMapClickListener(this::addNewLocation);

            // Listener para clicks en marcadores
            map.setOnMarkerClickListener(marker -> {
                for (int i = 0; i < markers.size(); i++) {
                    if (markers.get(i).equals(marker)) {
                        onMarkerSelected(i);
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void addNewLocation(LatLng latLng) {
        // Crear ubicación temporal con nombre genérico primero
        final int position = locations.size();
        String tempName = "Parada " + (position + 1);
        
        TourLocation location = new TourLocation(
                latLng.latitude,
                latLng.longitude,
                tempName,
                position + 1
        );

        locations.add(location);
        adapter.notifyItemInserted(locations.size() - 1);

        // Agregar marcador temporal
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(tempName)
                .snippet("Orden: " + location.order));

        if (marker != null) {
            markers.add(marker);
        }

        drawRoute();
        updatePeekInfo();
        updateBottomSheetInfo();
        updateEmptyState();

        // Obtener nombre del lugar en segundo plano
        new Thread(() -> {
            String locationName = getAddressFromLatLng(latLng);
            
            // Actualizar en el hilo principal
            runOnUiThread(() -> {
                if (position < locations.size() && position < markers.size()) {
                    // Actualizar el nombre solo si se obtuvo uno válido
                    if (!locationName.equals("Ubicación seleccionada")) {
                        locations.get(position).name = locationName;
                        markers.get(position).setTitle(locationName);
                        adapter.notifyItemChanged(position);
                    }
                }
            });
        }).start();

        // Si es la primera parada, abrir el bottom sheet automáticamente
        if (locations.size() == 1) {
            openBottomSheet();
        }
    }

    private void onMarkerSelected(int position) {
        selectedPosition = position;

        // Abrir el bottom sheet si está cerrado
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            openBottomSheet();
        }

        // Scroll al item seleccionado
        RecyclerView rv = bottomSheetModal.findViewById(R.id.rv_locations);
        if (rv != null) {
            rv.smoothScrollToPosition(position);
        }

        // Resaltar el item
        adapter.setSelectedPosition(position);

        // Centrar el mapa en el marcador
        if (position < locations.size()) {
            TourLocation location = locations.get(position);
            LatLng latLng = new LatLng(location.lat, location.lng);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }

    private void onLocationClicked(int position) {
        selectedPosition = position;

        // Centrar el mapa en la ubicación seleccionada
        if (position < locations.size()) {
            TourLocation location = locations.get(position);
            LatLng latLng = new LatLng(location.lat, location.lng);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

            // Mostrar info del marcador
            if (position < markers.size()) {
                markers.get(position).showInfoWindow();
            }
        }

        // Resaltar en el adapter
        adapter.setSelectedPosition(position);
    }

    private void drawRoute() {
        // Eliminar polilínea anterior
        if (routePolyline != null) {
            routePolyline.remove();
        }

        // Crear nueva polilínea si hay al menos 2 ubicaciones
        if (locations.size() >= 2) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .width(8)
                    .color(0xFF2196F3)
                    .geodesic(true);

            for (TourLocation location : locations) {
                polylineOptions.add(new LatLng(location.lat, location.lng));
            }

            routePolyline = googleMap.addPolyline(polylineOptions);
        }
    }

    private void updateLocationsOrder() {
        for (int i = 0; i < locations.size(); i++) {
            locations.get(i).order = i + 1;
            // NO resetear el nombre - mantener el nombre original del lugar

            if (i < markers.size()) {
                // Actualizar el marcador con el nombre original de la ubicación
                markers.get(i).setTitle(locations.get(i).name);
                markers.get(i).setSnippet("Orden: " + (i + 1));
            }
        }
        adapter.notifyDataSetChanged();
        drawRoute();
        updatePeekInfo();
        updateBottomSheetInfo();
    }

    private void updatePeekInfo() {
        if (tvPeekSummary == null || singleLocationMode) return;

        int count = locations.size();

        if (count == 0) {
            tvPeekSummary.setText("Toca el mapa para agregar paradas");
        } else {
            double distance = calculateTotalDistance();
            if (count == 1) {
                tvPeekSummary.setText("1 parada - 0 km");
            } else {
                tvPeekSummary.setText(String.format("%d paradas - %.1f km", count, distance));
            }
        }
    }

    private void updateBottomSheetInfo() {
        int count = locations.size();

        // Actualizar contador de paradas
        if (tvTotalStops != null) {
            tvTotalStops.setText(String.valueOf(count));
        }

        // Actualizar distancia total
        if (tvTotalDistance != null) {
            if (locations.size() > 1) {
                double distance = calculateTotalDistance();
                tvTotalDistance.setText(String.format("%.1f km", distance));
            } else {
                tvTotalDistance.setText("0 km");
            }
        }
    }

    private double calculateTotalDistance() {
        double totalDistance = 0;
        for (int i = 0; i < locations.size() - 1; i++) {
            TourLocation loc1 = locations.get(i);
            TourLocation loc2 = locations.get(i + 1);
            totalDistance += distance(loc1.lat, loc1.lng, loc2.lat, loc2.lng);
        }
        return totalDistance;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Radio de la Tierra en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private void updateEmptyState() {
        if (layoutEmptyLocations != null) {
            layoutEmptyLocations.setVisibility(
                    locations.isEmpty() ? View.VISIBLE : View.GONE
            );
        }

        RecyclerView rv = bottomSheetModal.findViewById(R.id.rv_locations);
        if (rv != null) {
            rv.setVisibility(locations.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void clearAllLocations() {
        locations.clear();
        adapter.notifyDataSetChanged();

        // Limpiar marcadores
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();

        // Limpiar polilínea
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }

        selectedPosition = -1;
        updatePeekInfo();
        updateBottomSheetInfo();
        updateEmptyState();

        // Cerrar el bottom sheet si está abierto
        closeBottomSheet();
    }

    @Override
    public void onLocationDeleted(int position, TourLocation location) {
        if (position < 0 || position >= locations.size()) {
            return;
        }

        // Eliminar del array
        locations.remove(position);
        adapter.notifyItemRemoved(position);

        // Eliminar marcador
        if (position < markers.size()) {
            markers.get(position).remove();
            markers.remove(position);
        }

        selectedPosition = -1;
        updateLocationsOrder();
        updatePeekInfo();
        updateBottomSheetInfo();
        updateEmptyState();

        // Si no quedan paradas, cerrar el bottom sheet
        if (locations.isEmpty()) {
            closeBottomSheet();
        }
    }

    private void selectSingleLocation(LatLng latLng) {
        // Limpiar marcador anterior si existe
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();

        // Obtener el nombre del lugar usando Geocoder
        String locationName = getAddressFromLatLng(latLng);

        // Crear la ubicación
        singleSelectedLocation = new TourLocation(
                latLng.latitude,
                latLng.longitude,
                locationName,
                1
        );

        // Agregar nuevo marcador
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(locationName));

        if (marker != null) {
            markers.add(marker);
        }

        // Mostrar diálogo de confirmación
        new AlertDialog.Builder(this)
                .setTitle("Punto de Encuentro Seleccionado")
                .setMessage("📍 " + locationName + "\n\nLat: " + String.format("%.6f", latLng.latitude) 
                        + "\nLng: " + String.format("%.6f", latLng.longitude))
                .setPositiveButton("Confirmar", (dialog, which) -> returnSingleLocationResult())
                .setNegativeButton("Cambiar", null)
                .show();
    }

    private String getAddressFromLatLng(LatLng latLng) {
        try {
            Geocoder geocoder = new Geocoder(this);
            
            // Verificar si el servicio está disponible
            if (!Geocoder.isPresent()) {
                Log.w("TourLocationsMap", "Geocoder no disponible");
                return "Ubicación seleccionada";
            }
            
            // Agregar timeout implícito usando múltiples intentos
            List<Address> addresses = null;
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        break;
                    }
                } catch (Exception e) {
                    Log.w("TourLocationsMap", "Intento " + (attempt + 1) + " falló", e);
                    if (attempt == 0) {
                        // Pequeña pausa antes del segundo intento
                        try { Thread.sleep(500); } catch (InterruptedException ie) {}
                    }
                }
            }
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Construir nombre legible con prioridad
                StringBuilder sb = new StringBuilder();
                
                // Prioridad 1: Nombre de lugar (Feature)
                if (address.getFeatureName() != null && !address.getFeatureName().matches("\\d+")) {
                    sb.append(address.getFeatureName());
                }
                
                // Prioridad 2: Calle (Thoroughfare)
                if (address.getThoroughfare() != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(address.getThoroughfare());
                }
                
                // Prioridad 3: Localidad/Ciudad
                if (address.getLocality() != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(address.getLocality());
                }
                
                // Si aún está vacío, usar subLocalidad o AdminArea
                if (sb.length() == 0) {
                    if (address.getSubLocality() != null) {
                        sb.append(address.getSubLocality());
                    } else if (address.getAdminArea() != null) {
                        sb.append(address.getAdminArea());
                    }
                }
                
                String result = sb.toString().trim();
                if (result.endsWith(",")) {
                    result = result.substring(0, result.length() - 1).trim();
                }
                
                Log.d("TourLocationsMap", "Dirección obtenida: " + result);
                return result.isEmpty() ? "Ubicación seleccionada" : result;
            }
        } catch (Exception e) {
            Log.e("TourLocationsMap", "Error obteniendo dirección", e);
        }
        return "Ubicación seleccionada";
    }

    private void returnSingleLocationResult() {
        if (singleSelectedLocation == null) {
            Toast.makeText(this, "Por favor selecciona un punto de encuentro", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent data = new Intent();
        data.putExtra("meetingPointName", singleSelectedLocation.name);
        data.putExtra("meetingPointLat", singleSelectedLocation.lat);
        data.putExtra("meetingPointLng", singleSelectedLocation.lng);
        setResult(RESULT_OK, data);
        finish();
    }

    private void returnResult() {
        Intent data = new Intent();
        data.putParcelableArrayListExtra("locations", locations);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Si el bottom sheet está abierto, cerrarlo primero
        if (bottomSheetBehavior != null &&
                bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            closeBottomSheet();
        } else {
            super.onBackPressed();
        }
    }
}