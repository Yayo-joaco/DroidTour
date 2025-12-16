package com.example.droidtour;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Actividad para que el guia gestione y confirme las paradas del tour.
 */
public class GuideStopsManagementActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_TOUR_ID = "tour_id";
    private static final String TAG = "GuideStopsManagement";

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private RecyclerView rvStops;
    private TextView tvTourName, tvTotalStops, tvCompletedStops;
    private MaterialCardView cardMeetingPoint;
    private TextView tvMeetingPointName;
    private MaterialButton btnConfirmMeetingPoint;

    private FirestoreManager firestoreManager;
    private FirebaseFirestore db;
    private String tourId;
    private Tour currentTour;
    private List<Tour.TourStop> stops = new ArrayList<>();
    private StopsAdapter adapter;
    private boolean meetingPointConfirmed = false;
    private boolean canConfirmStops = true; // Se habilita solo cuando el check-in ha terminado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_stops_management);

        firestoreManager = FirestoreManager.getInstance();
        db = FirebaseFirestore.getInstance();

        tourId = getIntent().getStringExtra(EXTRA_TOUR_ID);
        if (tourId == null || tourId.isEmpty()) {
            Toast.makeText(this, "Error: Tour ID no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupMap();
        loadTourData();
    }

    private void setupMap() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (currentTour != null) {
            updateMapWithStops();
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvTotalStops = findViewById(R.id.tv_total_stops);
        tvCompletedStops = findViewById(R.id.tv_completed_stops);
        rvStops = findViewById(R.id.rv_stops);
        cardMeetingPoint = findViewById(R.id.card_meeting_point);
        tvMeetingPointName = findViewById(R.id.tv_meeting_point_name);
        btnConfirmMeetingPoint = findViewById(R.id.btn_confirm_meeting_point);

        btnConfirmMeetingPoint.setOnClickListener(v -> confirmMeetingPoint());
    }

    private void setupRecyclerView() {
        rvStops.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StopsAdapter(stops);
        rvStops.setAdapter(adapter);
    }

    private void loadTourData() {
        Log.d(TAG, "Cargando tour con ID: " + tourId);

        firestoreManager.getTourById(tourId, new FirestoreManager.TourCallback() {
            @Override
            public void onSuccess(Tour tour) {
                currentTour = tour;
                if (currentTour != null) {
                    Log.d(TAG, "Tour cargado: " + currentTour.getTourName());
                    displayTourData();
                } else {
                    Log.e(TAG, "Tour es null");
                    Toast.makeText(GuideStopsManagementActivity.this,
                            "Error: Tour no encontrado", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error cargando tour: " + error);
                Toast.makeText(GuideStopsManagementActivity.this,
                        "Error al cargar tour: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayTourData() {
        tvTourName.setText(currentTour.getTourName());

        // Punto de encuentro
        if (currentTour.getMeetingPoint() != null) {
            tvMeetingPointName.setText(currentTour.getMeetingPoint());
        }

        if (currentTour.getMeetingPointConfirmed() != null && currentTour.getMeetingPointConfirmed()) {
            Log.d(TAG, "Punto de encuentro ya confirmado en Firestore");
            meetingPointConfirmed = true;
            btnConfirmMeetingPoint.setEnabled(false);
            btnConfirmMeetingPoint.setText("Confirmado");
        } else {
            Log.d(TAG, "Punto de encuentro pendiente de confirmacion");
            meetingPointConfirmed = false;
            btnConfirmMeetingPoint.setEnabled(true);
            btnConfirmMeetingPoint.setText("Confirmar llegada");
        }

        // Bloqueo de paradas hasta que se complete el check-in
        String checkStatus = currentTour.getCheckInOutStatus();
        canConfirmStops = checkStatus != null && !"ESPERANDO_CHECKIN".equals(checkStatus);
        if (!canConfirmStops) {
            Log.d(TAG, "Paradas bloqueadas: check-in no finalizado");
        }

        // Paradas - MOSTRAR TODAS, no solo las confirmadas
        if (currentTour.getStops() != null && !currentTour.getStops().isEmpty()) {
            Log.d(TAG, "Total de paradas en tour: " + currentTour.getStops().size());
            stops.clear();
            stops.addAll(currentTour.getStops());

            for (int i = 0; i < stops.size(); i++) {
                Tour.TourStop stop = stops.get(i);
                Log.d(TAG, "  Parada " + (i + 1) + ": " + stop.getName() + " (Order: " + stop.getOrder() + ", Completed: " + stop.getCompleted() + ")");
            }

            adapter.notifyDataSetChanged();
        } else {
            Log.w(TAG, "No hay paradas en este tour o la lista es null");
        }

        updateStopCounters();
        updateMapWithStops();
    }

    private void updateMapWithStops() {
        if (mMap == null || currentTour == null) return;

        mMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasMarkers = false;

        if (currentTour.getMeetingPointLatitude() != null && currentTour.getMeetingPointLongitude() != null) {
            LatLng meetingLatLng = new LatLng(currentTour.getMeetingPointLatitude(), currentTour.getMeetingPointLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(meetingLatLng)
                    .title("Punto de Encuentro")
                    .snippet(currentTour.getMeetingPoint())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            boundsBuilder.include(meetingLatLng);
            hasMarkers = true;
        }

        if (stops != null && !stops.isEmpty()) {
            for (Tour.TourStop stop : stops) {
                LatLng stopLatLng = new LatLng(stop.getLatitude(), stop.getLongitude());
                boolean isCompleted = stop.getCompleted() != null && stop.getCompleted();

                float markerColor = isCompleted ?
                        BitmapDescriptorFactory.HUE_GREEN :
                        BitmapDescriptorFactory.HUE_RED;

                String snippet = stop.getTime() != null ? stop.getTime() : "";
                if (isCompleted) {
                    snippet += " - Completada";
                }

                mMap.addMarker(new MarkerOptions()
                        .position(stopLatLng)
                        .title(stop.getOrder() + ". " + stop.getName())
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                boundsBuilder.include(stopLatLng);
                hasMarkers = true;
            }
        }

        if (hasMarkers) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 100;
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                Log.e(TAG, "Error ajustando camara", e);
            }
        }
    }

    private void updateStopCounters() {
        int total = stops.size();
        int completed = 0;
        for (Tour.TourStop stop : stops) {
            if (stop.getCompleted() != null && stop.getCompleted()) {
                completed++;
            }
        }

        tvTotalStops.setText(String.valueOf(total));
        tvCompletedStops.setText(String.valueOf(completed));
    }

    private void showCheckInRequiredAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Check-in pendiente")
                .setMessage("No puedes confirmar paradas hasta finalizar el check-in de los participantes (o pulsar 'Finalizar Check-In').")
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void confirmMeetingPoint() {
        if (currentTour == null) {
            Toast.makeText(this, "Error: Tour no cargado", Toast.LENGTH_SHORT).show();
            return;
        }

        meetingPointConfirmed = true;
        btnConfirmMeetingPoint.setEnabled(false);
        btnConfirmMeetingPoint.setText("Confirmado");

        currentTour.setMeetingPointConfirmed(true);
        currentTour.setMeetingPointConfirmedAt(new Date());

        firestoreManager.updateTour(currentTour, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Punto de encuentro confirmado en Firestore");
                Toast.makeText(GuideStopsManagementActivity.this, "Punto de encuentro confirmado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al confirmar punto de encuentro", e);
                Toast.makeText(GuideStopsManagementActivity.this,
                        "Error al confirmar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                currentTour.setMeetingPointConfirmed(false);
                currentTour.setMeetingPointConfirmedAt(null);
                meetingPointConfirmed = false;
                btnConfirmMeetingPoint.setEnabled(true);
                btnConfirmMeetingPoint.setText("Confirmar");
            }
        });
    }

    private void confirmStop(int position) {
        if (position < 0 || position >= stops.size()) return;
        if (currentTour == null) {
            Toast.makeText(this, "Error: Tour no cargado", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canConfirmStops) {
            showCheckInRequiredAlert();
            return;
        }

        Tour.TourStop stop = stops.get(position);

        for (int i = 0; i < position; i++) {
            Tour.TourStop previousStop = stops.get(i);
            if (previousStop.getCompleted() == null || !previousStop.getCompleted()) {
                return;
            }
        }

        stop.setCompleted(true);
        stop.setCompletedAt(new Date());

        currentTour.setStops(stops);

        firestoreManager.updateTour(currentTour, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Parada confirmada: " + stop.getName());
                Toast.makeText(GuideStopsManagementActivity.this,
                        "Parada confirmada: " + stop.getName(), Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
                updateStopCounters();
                updateMapWithStops();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al confirmar parada: " + e.getMessage(), e);
                Toast.makeText(GuideStopsManagementActivity.this,
                        "Error al confirmar parada: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                stop.setCompleted(false);
                stop.setCompletedAt(null);
                adapter.notifyDataSetChanged();
            }
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

    // Adapter para las paradas
    private class StopsAdapter extends RecyclerView.Adapter<StopsAdapter.ViewHolder> {
        private List<Tour.TourStop> stops;

        StopsAdapter(List<Tour.TourStop> stops) {
            this.stops = stops != null ? stops : new ArrayList<>();
            Log.d(TAG, "StopsAdapter creado con " + this.stops.size() + " paradas");
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tour_stop, parent, false);
            Log.d(TAG, "onCreateViewHolder llamado");
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Tour.TourStop stop = stops.get(position);
            Log.d(TAG, "onBindViewHolder - Position: " + position + ", Stop: " + stop.getName());

            holder.tvStopNumber.setText(String.valueOf(stop.getOrder()));
            holder.tvStopName.setText(stop.getName());

            if (stop.getDescription() != null && !stop.getDescription().isEmpty()) {
                holder.tvStopDescription.setText(stop.getDescription());
                holder.tvStopDescription.setVisibility(View.VISIBLE);
            } else {
                holder.tvStopDescription.setVisibility(View.GONE);
            }

            if (stop.getStopDuration() != null && stop.getStopDuration() > 0) {
                holder.tvStopTime.setText(stop.getStopDuration() + " min");
                holder.tvStopTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvStopTime.setVisibility(View.GONE);
            }

            boolean isCompleted = stop.getCompleted() != null && stop.getCompleted();

            boolean canConfirm = !isCompleted;
            if (!isCompleted && position > 0) {
                for (int i = 0; i < position; i++) {
                    Tour.TourStop previousStop = stops.get(i);
                    if (previousStop.getCompleted() == null || !previousStop.getCompleted()) {
                        canConfirm = false;
                        break;
                    }
                }
            }

            boolean canConfirmWithCheck = canConfirmStops && canConfirm;
            holder.btnConfirmStop.setEnabled(canConfirmWithCheck);

            if (isCompleted) {
                holder.btnConfirmStop.setText("Confirmada");
                holder.btnConfirmStop.setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light, null));
            } else if (!canConfirmStops) {
                holder.btnConfirmStop.setText("Esperando check-in");
                holder.btnConfirmStop.setBackgroundColor(
                        getResources().getColor(android.R.color.darker_gray, null));
            } else if (!canConfirm) {
                holder.btnConfirmStop.setText("Pendiente");
                holder.btnConfirmStop.setBackgroundColor(
                        getResources().getColor(android.R.color.darker_gray, null));
            } else {
                holder.btnConfirmStop.setText("Confirmar");
                holder.btnConfirmStop.setBackgroundColor(
                        getResources().getColor(R.color.primary, null));
            }

            holder.btnConfirmStop.setOnClickListener(v -> {
                if (!canConfirmStops) {
                    showCheckInRequiredAlert();
                    return;
                }
                confirmStop(holder.getAdapterPosition());
            });
        }

        @Override
        public int getItemCount() {
            int count = stops != null ? stops.size() : 0;
            Log.d(TAG, "getItemCount devuelve: " + count);
            return count;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStopNumber, tvStopName, tvStopDescription, tvStopTime;
            MaterialButton btnConfirmStop;

            ViewHolder(View view) {
                super(view);
                tvStopNumber = view.findViewById(R.id.tv_stop_number);
                tvStopName = view.findViewById(R.id.tv_stop_name);
                tvStopDescription = view.findViewById(R.id.tv_stop_description);
                tvStopTime = view.findViewById(R.id.tv_stop_time);
                btnConfirmStop = view.findViewById(R.id.btn_confirm_stop);
            }
        }
    }
}