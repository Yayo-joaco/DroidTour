package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class RealTimeTrackingActivity extends AppCompatActivity {
    
    private TextView tvCurrentLocation, tvNextDestination, tvEstimatedTime, tvParticipants;
    private RecyclerView rvTourProgress;
    private MaterialButton btnCenterMap, btnContactGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_tracking);
        
        setupToolbar();
        initializeViews();
        loadTrackingData();
        setupClickListeners();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Seguimiento en Tiempo Real");
    }
    
    private void initializeViews() {
        tvCurrentLocation = findViewById(R.id.tv_current_location);
        tvNextDestination = findViewById(R.id.tv_next_destination);
        tvEstimatedTime = findViewById(R.id.tv_estimated_time);
        tvParticipants = findViewById(R.id.tv_participants);
        rvTourProgress = findViewById(R.id.rv_tour_progress);
        btnCenterMap = findViewById(R.id.btn_center_map);
        btnContactGuide = findViewById(R.id.btn_contact_guide);
    }
    
    private void loadTrackingData() {
        // Datos mock del tour en progreso
        tvCurrentLocation.setText("Plaza de Armas - Cusco");
        tvNextDestination.setText("Catedral del Cusco");
        tvEstimatedTime.setText("5 minutos");
        tvParticipants.setText("12 participantes");
        
        // Configurar RecyclerView con progreso del tour
        rvTourProgress.setLayoutManager(new LinearLayoutManager(this));
        rvTourProgress.setAdapter(new TourProgressAdapter());
    }
    
    private void setupClickListeners() {
        btnCenterMap.setOnClickListener(v -> {
            Toast.makeText(this, "Centrando mapa en ubicación actual", Toast.LENGTH_SHORT).show();
        });
        
        btnContactGuide.setOnClickListener(v -> {
            Toast.makeText(this, "Contactando con guía: Carlos Mendoza", Toast.LENGTH_SHORT).show();
        });
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

// Clase para representar el progreso del tour
class TourProgressPoint {
    public String locationName;
    public String status; // "completed", "current", "upcoming"
    public String time;
    public String description;

    public TourProgressPoint(String locationName, String status, String time, String description) {
        this.locationName = locationName;
        this.status = status;
        this.time = time;
        this.description = description;
    }
}

// Adaptador para mostrar el progreso del tour
class TourProgressAdapter extends RecyclerView.Adapter<TourProgressAdapter.ViewHolder> {
    
    private final TourProgressPoint[] progressPoints;

    TourProgressAdapter() {
        // Datos mock del progreso del tour
        this.progressPoints = new TourProgressPoint[] {
            new TourProgressPoint("Punto de Encuentro", "completed", "8:00 AM", "Recepción de participantes"),
            new TourProgressPoint("Plaza de Armas", "current", "8:30 AM", "Historia del centro histórico"),
            new TourProgressPoint("Catedral del Cusco", "upcoming", "9:00 AM", "Arquitectura colonial"),
            new TourProgressPoint("Qorikancha", "upcoming", "9:45 AM", "Templo del Sol"),
            new TourProgressPoint("San Blas", "upcoming", "10:30 AM", "Barrio de artesanos"),
            new TourProgressPoint("Mercado San Pedro", "upcoming", "11:15 AM", "Productos locales"),
            new TourProgressPoint("Punto Final", "upcoming", "12:00 PM", "Despedida y valoración")
        };
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TourProgressPoint point = progressPoints[position];
        
        holder.tvLocationName.setText(point.locationName);
        holder.tvTime.setText(point.time);
        holder.tvDescription.setText(point.description);
        
        // Cambiar colores según el estado
        int statusColor;
        switch (point.status) {
            case "completed":
                statusColor = holder.itemView.getContext().getResources().getColor(R.color.green);
                break;
            case "current":
                statusColor = holder.itemView.getContext().getResources().getColor(R.color.primary);
                break;
            default: // upcoming
                statusColor = holder.itemView.getContext().getResources().getColor(R.color.gray);
        }
        
        holder.viewStatusIndicator.setBackgroundColor(statusColor);
        holder.tvLocationName.setTextColor(statusColor);
    }

    @Override
    public int getItemCount() {
        return progressPoints.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName, tvTime, tvDescription;
        View viewStatusIndicator;

        ViewHolder(View view) {
            super(view);
            tvLocationName = view.findViewById(R.id.tv_location_name);
            tvTime = view.findViewById(R.id.tv_time);
            tvDescription = view.findViewById(R.id.tv_description);
            viewStatusIndicator = view.findViewById(R.id.view_status_indicator);
        }
    }
}
