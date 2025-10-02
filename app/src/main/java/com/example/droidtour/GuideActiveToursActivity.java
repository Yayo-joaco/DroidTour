package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import android.view.LayoutInflater;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;

public class GuideActiveToursActivity extends AppCompatActivity {

    private ChipGroup chipGroupFilter;
    private RecyclerView rvMyTours;
    private MyToursAdapter toursAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_active_tours);

        setupToolbar();
        initializeViews();
        setupChips();
        setupRecycler();
        loadToursForFilter("activos");
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Mis Tours");
    }
    
    private void initializeViews() {
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        rvMyTours = findViewById(R.id.rv_my_tours);
    }

    private void setupChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                String filterType = "";

                if (checkedId == R.id.chip_activos) {
                    filterType = "activos";
                } else if (checkedId == R.id.chip_programados) {
                    filterType = "programados";
                } else if (checkedId == R.id.chip_historial) {
                    filterType = "historial";
                } else if (checkedId == R.id.chip_completados) {
                    filterType = "completados";
                }

                loadToursForFilter(filterType);
            }
        });
    }

    private void setupRecycler() {
        rvMyTours.setLayoutManager(new LinearLayoutManager(this));
        toursAdapter = new MyToursAdapter(this::openTourMap);
        rvMyTours.setAdapter(toursAdapter);
    }

    private void loadToursForFilter(String filterType) {
        Toast.makeText(this, "Cargando tours " + filterType + "...", Toast.LENGTH_SHORT).show();
        // TODO: Cargar tours según el filtro seleccionado
        toursAdapter.notifyDataSetChanged();
    }

    private void openTourMap(String tourName, String location) {
        Intent intent = new Intent(this, TourMapActivity.class);
        intent.putExtra("TOUR_NAME", tourName);
        intent.putExtra("LOCATION", location);
        startActivity(intent);
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

// Adaptador para tours del guía
class MyToursAdapter extends RecyclerView.Adapter<MyToursAdapter.ViewHolder> {
    interface OnTourMapClick { void onClick(String tourName, String location); }

    private final OnTourMapClick onTourMapClick;

    MyToursAdapter(OnTourMapClick listener) {
        this.onTourMapClick = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guide_active_tour, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        android.view.View item = holder.itemView;
        android.widget.TextView name = item.findViewById(R.id.tv_tour_name);
        android.widget.TextView date = item.findViewById(R.id.tv_tour_date);
        android.widget.TextView time = item.findViewById(R.id.tv_tour_time);
        android.widget.TextView participants = item.findViewById(R.id.tv_participants_count);
        android.widget.TextView status = item.findViewById(R.id.tv_tour_status);
        android.widget.TextView progress = item.findViewById(R.id.tv_tour_progress);
        android.widget.TextView amount = item.findViewById(R.id.tv_payment_amount);
        android.widget.TextView currentLocation = item.findViewById(R.id.tv_current_location);
        android.view.View fabScan = item.findViewById(R.id.fab_scan_qr);
        android.widget.Button btnViewMap = item.findViewById(R.id.btn_view_map);

        String tourName = "";
        String location = "";

        switch (position % 3) {
            case 0:
                tourName = "Inka Travel Peru - City Tour Centro Histórico";
                location = "Plaza de Armas, Lima";
                name.setText(tourName);
                date.setText("Hoy, 15 Dic");
                time.setText("09:30 - 12:30");
                participants.setText("6 personas");
                status.setText("EN PROGRESO");
                progress.setText("Punto 2 de 4");
                amount.setText("S/. 200");
                currentLocation.setText("Plaza de Armas");
                break;
            case 1:
                tourName = "Lima Adventure - Barranco & Miraflores";
                location = "Malecón de Miraflores, Lima";
                name.setText(tourName);
                date.setText("Mañana, 16 Dic");
                time.setText("08:00 - 12:00");
                participants.setText("10 personas");
                status.setText("PROGRAMADO");
                progress.setText("Inicio 08:00");
                amount.setText("S/. 180");
                currentLocation.setText("Malecón de Miraflores");
                break;
            default:
                tourName = "Cusco Heritage - Valle Sagrado";
                location = "Ollantaytambo, Cusco";
                name.setText(tourName);
                date.setText("Ayer, 14 Dic");
                time.setText("14:00 - 19:00");
                participants.setText("8 personas");
                status.setText("COMPLETADO");
                progress.setText("Finalizado");
                amount.setText("S/. 250");
                currentLocation.setText("Ollantaytambo");
        }

        final String finalTourName = tourName;
        final String finalLocation = location;

        // Configurar botón "Ver Mapa"
        btnViewMap.setOnClickListener(v -> onTourMapClick.onClick(finalTourName, finalLocation));

        // Configurar botón de escanear QR
        fabScan.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(item.getContext(), QRScannerActivity.class);
            item.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View itemView) { super(itemView); }
    }
}
