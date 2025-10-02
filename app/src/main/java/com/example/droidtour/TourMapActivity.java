package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class TourMapActivity extends AppCompatActivity {

    private TextView tvTourName, tvLocationAddress, tvLocationCoordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_map);

        setupToolbar();
        initializeViews();
        loadLocationData();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Ubicación del Tour");
    }
    
    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvLocationAddress = findViewById(R.id.tv_location_address);
        tvLocationCoordinates = findViewById(R.id.tv_location_coordinates);
    }

    private void loadLocationData() {
        Intent intent = getIntent();

        String tourName = intent.getStringExtra("TOUR_NAME");
        String location = intent.getStringExtra("LOCATION");

        tvTourName.setText(tourName != null ? tourName : "Tour no especificado");
        tvLocationAddress.setText(location != null ? location : "Ubicación no disponible");

        // Coordenadas mock - aquí se integraría con la API de Google Maps
        tvLocationCoordinates.setText("Lat: -12.0464, Lng: -77.0428");
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
