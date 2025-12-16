package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.droidtour.models.Tour;
import java.util.ArrayList;
import java.util.List;

public class TourStopsMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<Tour.TourStop> stops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_stops_map);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        setupToolbar();

        // Obtener paradas del intent
        stops = new ArrayList<>();
        if (getIntent().hasExtra("stops")) {
            ArrayList<Tour.TourStop> stopsFromIntent = 
                (ArrayList<Tour.TourStop>) getIntent().getSerializableExtra("stops");
            if (stopsFromIntent != null) {
                stops.addAll(stopsFromIntent);
            }
        }

        // Inicializar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mapa de Paradas");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (stops.isEmpty()) {
            // Si no hay paradas, mostrar Lima por defecto
            LatLng lima = new LatLng(-12.0464, -77.0428);
            mMap.addMarker(new MarkerOptions().position(lima).title("Lima"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lima, 12));
            return;
        }

        // Añadir marcadores para cada parada
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        List<LatLng> polylinePoints = new ArrayList<>();

        for (Tour.TourStop stop : stops) {
            LatLng position = new LatLng(stop.getLatitude(), stop.getLongitude());
            
            String title = stop.getOrder() + ". " + stop.getName();
            String snippet = stop.getTime();
            if (stop.getDescription() != null && !stop.getDescription().isEmpty()) {
                snippet += "\n" + stop.getDescription();
            }
            
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet(snippet));
            
            boundsBuilder.include(position);
            polylinePoints.add(position);
        }

        // Dibujar línea entre paradas
        if (polylinePoints.size() > 1) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(polylinePoints)
                    .color(0xFF2196F3)
                    .width(8));
        }

        // Ajustar cámara para mostrar todas las paradas
        try {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 100; // offset from edges of the map in pixels
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (Exception e) {
            android.util.Log.e("TourStopsMap", "Error setting camera bounds", e);
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
}
