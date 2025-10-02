package com.example.droidtour;

import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;

public class TourGuideActiveActivity extends AppCompatActivity {
    
    private TextView tvTourName, tvTourStatus, tvCurrentLocation, tvPointsProgress;
    private RecyclerView rvTourPoints;
    private MaterialButton btnRegisterLocation, btnScanCheckin, btnScanCheckout;
    private TourPointsAdapter tourPointsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_guide_active);
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadTourData();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Tour en Progreso");
    }
    
    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvTourStatus = findViewById(R.id.tv_tour_status);
        tvCurrentLocation = findViewById(R.id.tv_current_location);
        tvPointsProgress = findViewById(R.id.tv_points_progress);
        rvTourPoints = findViewById(R.id.rv_tour_points);
        btnRegisterLocation = findViewById(R.id.btn_register_location);
        btnScanCheckin = findViewById(R.id.btn_scan_checkin);
        btnScanCheckout = findViewById(R.id.btn_scan_checkout);
    }
    
    private void setupRecyclerView() {
        rvTourPoints.setLayoutManager(new LinearLayoutManager(this));
        tourPointsAdapter = new TourPointsAdapter(this::onPointAction);
        rvTourPoints.setAdapter(tourPointsAdapter);
    }
    
    private void setupClickListeners() {
        btnRegisterLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationTrackingActivity.class);
            startActivity(intent);
        });
        
        btnScanCheckin.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScannerActivity.class);
            intent.putExtra("SCAN_TYPE", "CHECK_IN");
            startActivity(intent);
        });
        
        btnScanCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScannerActivity.class);
            intent.putExtra("SCAN_TYPE", "CHECK_OUT");
            startActivity(intent);
        });
    }
    
    private void loadTourData() {
        // Cargar datos del tour desde Intent o base de datos
        tvTourName.setText("Tour Machu Picchu");
        tvTourStatus.setText("En progreso - Punto 2 de 5");
        tvCurrentLocation.setText("Ubicación actual: Sacsayhuamán");
        tvPointsProgress.setText("2/5 completados");
    }
    
    private void onPointAction(int position, String action) {
        Toast.makeText(this, "Acción: " + action + " en punto " + (position + 1), Toast.LENGTH_SHORT).show();
        // TODO: Implementar lógica de registro de llegada
        tourPointsAdapter.notifyItemChanged(position);
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

// Adaptador para puntos del tour
class TourPointsAdapter extends RecyclerView.Adapter<TourPointsAdapter.ViewHolder> {
    interface OnPointAction { void onAction(int position, String action); }
    
    private final OnPointAction onPointAction;

    TourPointsAdapter(OnPointAction listener) {
        this.onPointAction = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_point, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        android.view.View item = holder.itemView;
        TextView pointNumber = item.findViewById(R.id.tv_point_number);
        TextView pointName = item.findViewById(R.id.tv_point_name);
        TextView description = item.findViewById(R.id.tv_point_description);
        TextView arrivalTime = item.findViewById(R.id.tv_arrival_time);
        TextView duration = item.findViewById(R.id.tv_duration);
        Button btnRegister = item.findViewById(R.id.btn_register_arrival);
        TextView statusCompleted = item.findViewById(R.id.tv_status_completed);
        android.view.View statusIndicator = item.findViewById(R.id.view_status_indicator);

        pointNumber.setText(String.valueOf(position + 1));

        switch (position) {
            case 0:
                pointName.setText("Plaza de Armas");
                description.setText("Centro histórico de Lima");
                arrivalTime.setText("⏰ 10:15 AM");
                duration.setText("• ⏱️ 30 min");
                // Completado
                btnRegister.setVisibility(android.view.View.GONE);
                statusCompleted.setVisibility(android.view.View.VISIBLE);
                statusIndicator.setBackgroundResource(R.drawable.circle_green);
                break;
            case 1:
                pointName.setText("Catedral de Lima");
                description.setText("Arquitectura colonial");
                arrivalTime.setText("⏰ 10:45 AM");
                duration.setText("• ⏱️ 45 min");
                // Completado
                btnRegister.setVisibility(android.view.View.GONE);
                statusCompleted.setVisibility(android.view.View.VISIBLE);
                statusIndicator.setBackgroundResource(R.drawable.circle_green);
                break;
            case 2:
                pointName.setText("Palacio de Gobierno");
                description.setText("Sede del poder ejecutivo");
                arrivalTime.setText("⏰ 11:30 AM");
                duration.setText("• ⏱️ 20 min");
                // En progreso (actual)
                btnRegister.setVisibility(android.view.View.VISIBLE);
                statusCompleted.setVisibility(android.view.View.GONE);
                statusIndicator.setBackgroundResource(R.drawable.circle_orange);
                btnRegister.setText("Registrar");
                btnRegister.setOnClickListener(v -> onPointAction.onAction(position, "REGISTER"));
                break;
            case 3:
                pointName.setText("Balcones de Lima");
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
            case 4:
                pointName.setText("Mercado Central");
                description.setText("Gastronomía local");
                arrivalTime.setText("⏰ 12:15 PM");
                duration.setText("• ⏱️ 30 min");
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
        return 5;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View itemView) { super(itemView); }
    }
}
