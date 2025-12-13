package com.example.droidtour.admin;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.R;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;

public class TourManagementActivity extends AppCompatActivity implements AdminTourAdapter.OnTourClickListener {

    private RecyclerView rvTours;
    private AdminTourAdapter adapter;
    private final List<String> dummyTours = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_tour_managment);

        setupToolbar();

        rvTours = findViewById(R.id.toursRecyclerView);
        rvTours.setLayoutManager(new LinearLayoutManager(this));

        // Datos de ejemplo
        dummyTours.add("Machu Picchu Express");
        dummyTours.add("Tour Aventura en la Montaña");
        dummyTours.add("Tour Gastronómico");

        adapter = new AdminTourAdapter(dummyTours, this);
        rvTours.setAdapter(adapter);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Gestión de Tours");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTourClick(int position, String tourName, View anchorView) {
        // Mostrar BottomSheet con detalles (usando el layout existente)
        TourDetailsBottomSheet bs = TourDetailsBottomSheet.newInstance(tourName);
        bs.show(getSupportFragmentManager(), "tour_details");
    }
}
