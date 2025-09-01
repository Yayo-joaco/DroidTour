package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class LocationTrackingActivity extends AppCompatActivity {
    
    // private MaterialCardView cardCurrentLocation;
    private TextView tvCurrentLocation;
    private TextView tvLocationTime;
    private Button btnMarkLocation;
    private RecyclerView rvLocationHistory;
    private FloatingActionButton fabToggleTracking;
    private boolean isTrackingActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_tracking);
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        updateTrackingStatus();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Registro de Ubicación");
    }
    
    private void initializeViews() {
        // Usar los IDs que realmente existen en el layout
        // cardCurrentLocation = findViewById(R.id.card_current_location);
        tvCurrentLocation = findViewById(R.id.tv_tour_name); // Usar como placeholder
        tvLocationTime = findViewById(R.id.tv_current_time);
        btnMarkLocation = findViewById(R.id.btn_center_location); // Reutilizar botón existente
        rvLocationHistory = findViewById(R.id.rv_location_points);
        fabToggleTracking = findViewById(R.id.fab_register_location);
    }
    
    private void setupClickListeners() {
        btnMarkLocation.setOnClickListener(v -> {
            markCurrentLocation();
        });
        
        fabToggleTracking.setOnClickListener(v -> {
            toggleLocationTracking();
        });
        
        // cardCurrentLocation.setOnClickListener(v -> {
        //     refreshCurrentLocation();
        // });
    }
    
    private void setupRecyclerView() {
        rvLocationHistory.setLayoutManager(new LinearLayoutManager(this));
        rvLocationHistory.setAdapter(new ExampleLocationPointsAdapter());
    }
    
    private void markCurrentLocation() {
        // TODO: Obtener ubicación GPS actual y marcarla
        Toast.makeText(this, "Ubicación marcada correctamente", Toast.LENGTH_SHORT).show();
        
        // Simular actualización de ubicación
        tvCurrentLocation.setText("Plaza de Armas, Cusco\n-13.5164, -71.9785");
        tvLocationTime.setText("Última actualización: " + new java.util.Date().toString());
    }
    
    private void toggleLocationTracking() {
        isTrackingActive = !isTrackingActive;
        updateTrackingStatus();
        
        if (isTrackingActive) {
            Toast.makeText(this, "Seguimiento de ubicación activado", Toast.LENGTH_SHORT).show();
            // TODO: Iniciar servicio de seguimiento GPS
        } else {
            Toast.makeText(this, "Seguimiento de ubicación desactivado", Toast.LENGTH_SHORT).show();
            // TODO: Detener servicio de seguimiento GPS
        }
    }
    
    private void refreshCurrentLocation() {
        Toast.makeText(this, "Actualizando ubicación...", Toast.LENGTH_SHORT).show();
        // TODO: Forzar actualización de ubicación GPS
        markCurrentLocation();
    }
    
    private void updateTrackingStatus() {
        if (isTrackingActive) {
            fabToggleTracking.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            fabToggleTracking.setImageResource(android.R.drawable.ic_media_play);
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

// Adaptador con ejemplos de puntos del tour
class ExampleLocationPointsAdapter extends RecyclerView.Adapter<ExampleLocationPointsAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_point, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        android.view.View item = holder.itemView;
        TextView pointNumber = item.findViewById(R.id.tv_point_number);
        TextView locationName = item.findViewById(R.id.tv_location_name);
        TextView description = item.findViewById(R.id.tv_location_description);
        TextView arrivalTime = item.findViewById(R.id.tv_arrival_time);
        TextView duration = item.findViewById(R.id.tv_duration);
        android.widget.Button btnRegister = item.findViewById(R.id.btn_register_arrival);
        TextView statusCompleted = item.findViewById(R.id.tv_status_completed);
        android.view.View statusIndicator = item.findViewById(R.id.view_status_indicator);

        pointNumber.setText(String.valueOf(position + 1));

        switch (position) {
            case 0:
                locationName.setText("Plaza de Armas");
                description.setText("Centro histórico de Lima");
                arrivalTime.setText("⏰ 10:15 AM");
                duration.setText("• ⏱️ 30 min");
                // Completado
                btnRegister.setVisibility(android.view.View.GONE);
                statusCompleted.setVisibility(android.view.View.VISIBLE);
                statusIndicator.setBackgroundResource(R.drawable.circle_green);
                break;
            case 1:
                locationName.setText("Catedral de Lima");
                description.setText("Arquitectura colonial");
                arrivalTime.setText("⏰ 10:45 AM");
                duration.setText("• ⏱️ 45 min");
                // Completado
                btnRegister.setVisibility(android.view.View.GONE);
                statusCompleted.setVisibility(android.view.View.VISIBLE);
                statusIndicator.setBackgroundResource(R.drawable.circle_green);
                break;
            case 2:
                locationName.setText("Palacio de Gobierno");
                description.setText("Sede del poder ejecutivo");
                arrivalTime.setText("⏰ 11:30 AM");
                duration.setText("• ⏱️ 20 min");
                // En progreso (actual)
                btnRegister.setVisibility(android.view.View.VISIBLE);
                statusCompleted.setVisibility(android.view.View.GONE);
                statusIndicator.setBackgroundResource(R.drawable.circle_orange);
                btnRegister.setText("Registrar");
                btnRegister.setOnClickListener(v -> {
                    android.widget.Toast.makeText(item.getContext(), "Llegada registrada: " + locationName.getText(), android.widget.Toast.LENGTH_SHORT).show();
                });
                break;
            case 3:
                locationName.setText("Balcones de Lima");
                description.setText("Arquitectura virreinal");
                arrivalTime.setText("⏰ 11:50 AM");
                duration.setText("• ⏱️ 25 min");
                // Pendiente
                btnRegister.setVisibility(android.view.View.VISIBLE);
                statusCompleted.setVisibility(android.view.View.GONE);
                statusIndicator.setBackgroundResource(R.drawable.circle_light_gray);
                btnRegister.setText("Pendiente");
                btnRegister.setEnabled(false);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View itemView) { super(itemView); }
    }
}
