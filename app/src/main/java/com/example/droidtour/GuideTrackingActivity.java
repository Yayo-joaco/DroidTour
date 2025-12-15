package com.example.droidtour;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.TourOffer;
import com.example.droidtour.models.User;
import com.example.droidtour.ui.TourStopsBottomSheet;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class GuideTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final String TAG = "GuideTrackingActivity";
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private RecyclerView rvActiveGuides;
    private TextView tvActiveCount;
    private FloatingActionButton fabFilter;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private String currentCompanyId;
    private List<TourOffer> activeOffers = new ArrayList<>();
    private ActiveGuidesAdapter adapter;
    private Tour currentSelectedTour;
    private TourStopsBottomSheet bottomSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea ADMIN o GUIDE
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("COMPANY_ADMIN") && !userType.equals("GUIDE"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_guide_tracking);
        
        firestoreManager = FirestoreManager.getInstance();
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupMap();
        loadCompanyAndGuides();
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
        mMap.getUiSettings().setMapToolbarEnabled(true);
        
        // Centrar en Lima, Perú por defecto
        LatLng lima = new LatLng(-12.0464, -77.0428);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lima, 12));
    }
    
    private void loadCompanyAndGuides() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadActiveGuides();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
            }
        });
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        rvActiveGuides = findViewById(R.id.rv_active_guides);
        tvActiveCount = findViewById(R.id.tv_active_count);
        fabFilter = findViewById(R.id.fab_filter);
    }
    
    private void setupClickListeners() {
        fabFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filtros de seguimiento", Toast.LENGTH_SHORT).show();
            // TODO: Mostrar dialog de filtros
        });
    }
    
    private void setupRecyclerView() {
        rvActiveGuides.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActiveGuidesAdapter(activeOffers);
        rvActiveGuides.setAdapter(adapter);
    }
    
    private void loadActiveGuides() {
        if (currentCompanyId == null) {
            tvActiveCount.setText("0 activos");
            return;
        }
        
        // Usar el mismo filtro que ActiveGuidesFragment: ofertas ACEPTADAS
        Log.d(TAG, "🔍 Cargando guías activos para companyId: " + currentCompanyId);
        
        firestoreManager.getOffersByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<TourOffer> allOffers = (List<TourOffer>) result;
                Log.d(TAG, "📄 Total ofertas encontradas: " + allOffers.size());
                
                activeOffers.clear();
                
                // Filtrar solo las ofertas aceptadas (mismo filtro que Gestión de Guías)
                for (TourOffer offer : allOffers) {
                    Log.d(TAG, "  - Oferta: " + offer.getOfferId() + " Status: " + offer.getStatus());
                    if ("ACEPTADA".equals(offer.getStatus())) {
                        activeOffers.add(offer);
                        Log.d(TAG, "    ✅ Agregada a activos");
                    }
                }
                
                tvActiveCount.setText(activeOffers.size() + " activos");
                adapter.updateData(activeOffers);
                
                Log.d(TAG, "Guías activos cargados: " + activeOffers.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando guías activos", e);
                tvActiveCount.setText("0 activos");
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
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    
    // Adapter para guías activos (igual que Gestión de Guías pero sin bottom sheet)
    private class ActiveGuidesAdapter extends RecyclerView.Adapter<ActiveGuidesAdapter.ViewHolder> {
        private List<TourOffer> offers;
        
        ActiveGuidesAdapter(List<TourOffer> offers) {
            this.offers = offers != null ? offers : new ArrayList<>();
        }
        
        void updateData(List<TourOffer> newData) {
            this.offers = newData != null ? newData : new ArrayList<>();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_active_guide, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TourOffer offer = offers.get(position);
            
            // Nombre del guía
            holder.tvGuideName.setText(offer.getGuideName() != null ? offer.getGuideName() : "Guía");
            
            // Nombre del tour con fecha
            String tourInfo = offer.getTourName() != null ? offer.getTourName() : "Tour";
            if (offer.getTourDate() != null) {
                tourInfo += " - " + offer.getTourDate();
            }
            holder.tvTourName.setText(tourInfo);
            
            // Cargar foto del guía
            String guideId = offer.getGuideId();
            if (guideId != null && !guideId.isEmpty()) {
                firestoreManager.getUserById(guideId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        User user = (User) result;
                        if (user != null && user.getPersonalData() != null && 
                            user.getPersonalData().getProfileImageUrl() != null) {
                            Glide.with(holder.itemView.getContext())
                                .load(user.getPersonalData().getProfileImageUrl())
                                .placeholder(R.drawable.ic_person)
                                .circleCrop()
                                .into(holder.ivGuidePhoto);
                        }
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        // Dejar imagen por defecto
                    }
                });
            }
            
            // Click en el card para actualizar mapa directamente
            holder.itemView.setOnClickListener(v -> {
                String tourId = offer.getTourId();
                if (tourId != null) {
                    loadTourAndUpdateMap(tourId, offer.getTourName());
                } else {
                    Toast.makeText(v.getContext(), "Tour sin ID", Toast.LENGTH_SHORT).show();
                }
            });
            
            // Botón de detalles para abrir bottom sheet
            holder.btnShowDetails.setVisibility(View.VISIBLE);
            holder.btnShowDetails.setOnClickListener(v -> {
                String tourId = offer.getTourId();
                if (tourId != null) {
                    showTourStopsBottomSheet(tourId, offer.getTourName());
                } else {
                    Toast.makeText(v.getContext(), "Tour sin ID", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return offers != null ? offers.size() : 0;
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvGuideName, tvTourName;
            ImageView ivGuidePhoto;
            View btnShowDetails;
            
            ViewHolder(View view) {
                super(view);
                tvGuideName = view.findViewById(R.id.tv_guide_name);
                tvTourName = view.findViewById(R.id.tv_tour_name);
                ivGuidePhoto = view.findViewById(R.id.iv_guide_photo);
                btnShowDetails = view.findViewById(R.id.btn_show_guide_details);
            }
        }
    }
    
    private void loadTourAndUpdateMap(String tourId, String tourName) {
        // Cargar el tour y actualizar el mapa directamente (sin bottom sheet)
        firestoreManager.getTour(tourId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                currentSelectedTour = (Tour) result;
                if (currentSelectedTour != null) {
                    Toast.makeText(GuideTrackingActivity.this, 
                        "Mostrando paradas de: " + tourName, 
                        Toast.LENGTH_SHORT).show();
                    updateMapWithStops(currentSelectedTour, currentSelectedTour.getStops(), 
                        currentSelectedTour.getMeetingPoint());
                } else {
                    Toast.makeText(GuideTrackingActivity.this, 
                        "Este tour no tiene paradas definidas", 
                        Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando tour para mapa", e);
                Toast.makeText(GuideTrackingActivity.this, 
                    "Error al cargar datos del tour", 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showTourStopsBottomSheet(String tourId, String tourName) {
        // Cargar el tour y mostrar bottom sheet con actualización en tiempo real
        firestoreManager.getTour(tourId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                currentSelectedTour = (Tour) result;
                if (currentSelectedTour != null) {
                    // Mostrar bottom sheet
                    bottomSheet = TourStopsBottomSheet.newInstance(tourId, tourName);
                    bottomSheet.setOnStopUpdatedListener((stops, meetingPoint) -> {
                        // Actualizar mapa en tiempo real cuando cambien las paradas
                        updateMapWithStops(currentSelectedTour, stops, meetingPoint);
                    });
                    bottomSheet.show(getSupportFragmentManager(), "tour_stops");
                } else {
                    Toast.makeText(GuideTrackingActivity.this, 
                        "Este tour no tiene paradas definidas", 
                        Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando tour para mapa", e);
                Toast.makeText(GuideTrackingActivity.this, 
                    "Error al cargar datos del tour", 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateMapWithStops(Tour tour, List<Tour.TourStop> stops, String meetingPoint) {
        if (mMap == null) return;
        
        // Limpiar marcadores previos
        mMap.clear();
        
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasMarkers = false;
        
        // Agregar punto de encuentro (AZUL)
        if (tour.getMeetingPointLatitude() != null && tour.getMeetingPointLongitude() != null) {
            LatLng meetingLatLng = new LatLng(tour.getMeetingPointLatitude(), tour.getMeetingPointLongitude());
            mMap.addMarker(new MarkerOptions()
                .position(meetingLatLng)
                .title("Punto de Encuentro")
                .snippet(meetingPoint != null ? meetingPoint : "Inicio del tour")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            boundsBuilder.include(meetingLatLng);
            hasMarkers = true;
            Log.d(TAG, "🔵 Marcador AZUL agregado: " + meetingPoint);
        }
        
        // Agregar paradas (ROJO = pendiente, VERDE = completada)
        if (stops != null && !stops.isEmpty()) {
            for (Tour.TourStop stop : stops) {
                LatLng stopLatLng = new LatLng(stop.getLatitude(), stop.getLongitude());
                boolean isCompleted = stop.getCompleted() != null && stop.getCompleted();
                
                float markerColor = isCompleted ? 
                    BitmapDescriptorFactory.HUE_GREEN :  // Verde para completadas
                    BitmapDescriptorFactory.HUE_RED;     // Rojo para pendientes
                
                String snippet = stop.getTime() != null ? stop.getTime() : "";
                if (isCompleted) {
                    snippet += " ✓ Completada";
                }
                
                mMap.addMarker(new MarkerOptions()
                    .position(stopLatLng)
                    .title(stop.getOrder() + ". " + stop.getName())
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                
                boundsBuilder.include(stopLatLng);
                hasMarkers = true;
                
                Log.d(TAG, (isCompleted ? "🟢" : "🔴") + " Marcador agregado: " + stop.getName());
            }
        }
        
        // Ajustar cámara para mostrar todos los marcadores
        if (hasMarkers) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 150; // padding en pixels
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                Log.e(TAG, "Error ajustando cámara", e);
            }
        }
    }
}
