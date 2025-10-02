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
        loadToursForFilter("todas");
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

                if (checkedId == R.id.chip_todas) {
                    filterType = "todas";
                } else if (checkedId == R.id.chip_en_progreso) {
                    filterType = "en_progreso";
                } else if (checkedId == R.id.chip_programados) {
                    filterType = "programados";
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
        toursAdapter.setFilter(filterType);
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

// Clase para representar un tour
class Tour {
    String name, company, date, time, participants, status, progress, amount, location, currentLocation;
    int statusOrder; // Para ordenamiento: 1=En progreso, 2=Programado, 3=Completado

    Tour(String name, String company, String date, String time, String participants,
         String status, String progress, String amount, String location, String currentLocation, int statusOrder) {
        this.name = name;
        this.company = company;
        this.date = date;
        this.time = time;
        this.participants = participants;
        this.status = status;
        this.progress = progress;
        this.amount = amount;
        this.location = location;
        this.currentLocation = currentLocation;
        this.statusOrder = statusOrder;
    }
}

// Adaptador para tours del guía
class MyToursAdapter extends RecyclerView.Adapter<MyToursAdapter.ViewHolder> {
    interface OnTourMapClick { void onClick(String tourName, String location); }

    private final OnTourMapClick onTourMapClick;
    private String currentFilter = "todas";
    private final Tour[] allTours;
    private java.util.List<Tour> filteredTours;

    MyToursAdapter(OnTourMapClick listener) {
        this.onTourMapClick = listener;

        // Datos mock de tours
        this.allTours = new Tour[] {
            new Tour("City Tour Lima Centro Histórico", "Inka Travel Peru", "Hoy, 15 Dic", "09:30 - 12:30",
                    "6 personas", "En progreso", "Punto 2 de 4", "S/. 200", "Plaza de Armas, Lima", "Plaza de Armas", 1),
            new Tour("Barranco & Miraflores", "Lima Adventure", "Mañana, 16 Dic", "08:00 - 12:00",
                    "10 personas", "Programado", "Inicio 08:00", "S/. 180", "Malecón de Miraflores, Lima", "Malecón de Miraflores", 2),
            new Tour("Valle Sagrado Express", "Cusco Heritage", "Ayer, 14 Dic", "14:00 - 19:00",
                    "8 personas", "Completado", "Finalizado", "S/. 250", "Ollantaytambo, Cusco", "Ollantaytambo", 3),
            new Tour("Tour Gastronómico", "Lima Food Tours", "Hoy, 15 Dic", "18:00 - 21:00",
                    "4 personas", "Programado", "Inicio 18:00", "S/. 120", "Miraflores, Lima", "Miraflores", 2),
            new Tour("Circuito Mágico del Agua", "Water Tours", "Ayer, 14 Dic", "19:00 - 21:00",
                    "12 personas", "Completado", "Finalizado", "S/. 80", "Parque de la Reserva, Lima", "Parque de la Reserva", 3)
        };

        updateFilteredTours();
    }

    public void setFilter(String filter) {
        this.currentFilter = filter;
        updateFilteredTours();
    }

    private void updateFilteredTours() {
        if (currentFilter.equals("todas")) {
            // Mostrar todos, ordenados por estado: En progreso, Programado, Completado
            filteredTours = java.util.Arrays.asList(allTours);
            filteredTours.sort((t1, t2) -> Integer.compare(t1.statusOrder, t2.statusOrder));
        } else {
            filteredTours = new java.util.ArrayList<>();
            for (Tour tour : allTours) {
                boolean matches = false;
                switch (currentFilter) {
                    case "en_progreso":
                        matches = tour.status.equals("En progreso");
                        break;
                    case "programados":
                        matches = tour.status.equals("Programado");
                        break;
                    case "completados":
                        matches = tour.status.equals("Completado");
                        break;
                }
                if (matches) {
                    filteredTours.add(tour);
                }
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guide_active_tour, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Tour tour = filteredTours.get(position);
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
        android.widget.Button btnRegisterLocation = item.findViewById(R.id.btn_register_location);

        // Configurar datos del tour
        name.setText(tour.company + " - " + tour.name);
        date.setText(tour.date);
        time.setText(tour.time);
        participants.setText(tour.participants);
        status.setText(tour.status.toUpperCase());
        progress.setText(tour.progress);
        amount.setText(tour.amount);
        currentLocation.setText(tour.currentLocation);

        // Configurar botones según el estado
        if (tour.status.equals("En progreso")) {
            btnRegisterLocation.setVisibility(android.view.View.VISIBLE);
            btnRegisterLocation.setText("Registrar Llegada");
        } else if (tour.status.equals("Programado")) {
            btnRegisterLocation.setVisibility(android.view.View.VISIBLE);
            btnRegisterLocation.setText("Comenzar Tour");
        } else if (tour.status.equals("Completado")) {
            btnRegisterLocation.setVisibility(android.view.View.GONE);
        }

        // Configurar botón "Ver Mapa"
        btnViewMap.setOnClickListener(v -> onTourMapClick.onClick(tour.company + " - " + tour.name, tour.location));

        // Configurar botón de registrar/comenzar
        btnRegisterLocation.setOnClickListener(v -> {
            if (tour.status.equals("En progreso")) {
                android.widget.Toast.makeText(v.getContext(), "Registrando llegada...", android.widget.Toast.LENGTH_SHORT).show();
            } else if (tour.status.equals("Programado")) {
                android.widget.Toast.makeText(v.getContext(), "Comenzando tour...", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Configurar botón de escanear QR
        fabScan.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(item.getContext(), QRScannerActivity.class);
            item.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return filteredTours.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View itemView) { super(itemView); }
    }
}
