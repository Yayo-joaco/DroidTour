package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.TourOffer;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class TourOffersActivity extends AppCompatActivity {

    private static final String TAG = "TourOffersActivity";
    private RecyclerView rvTourOffers;
    private Chip chipAll, chipPending, chipRejected;
    private String currentFilter = "PENDING";

    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private String currentUserId;
    private List<TourOffer> allOffers = new ArrayList<>();
    private TourOffersAdapter adapter;

    private LinearLayout emptyTourOffers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar helpers
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);

        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }

        // Validar que el usuario sea un guía
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            redirectToLogin();
            finish();
            return;
        }

        // Obtener ID del usuario actual
        currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = prefsManager.getUserId();
        }

        setContentView(R.layout.activity_tour_offers);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFilters();
        loadOffersFromFirebase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar ofertas cuando volvemos de la actividad de detalles
        loadOffersFromFirebase();
    }

    private void loadOffersFromFirebase() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Error: currentUserId es null o vacío");
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "🔄 Cargando ofertas para guía: " + currentUserId);

        firestoreManager.getOffersByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                allOffers = (List<TourOffer>) result;
                Log.d(TAG, "✅ Ofertas cargadas: " + allOffers.size());
                
                // Log detallado de cada oferta
                for (TourOffer offer : allOffers) {
                    Log.d(TAG, "   📋 Oferta: " + offer.getTourName() + " | Status: " + offer.getStatus() + " | Fecha: " + offer.getTourDate());
                }
                
                filterAndUpdateList();

                if (allOffers.isEmpty()) {
                    Toast.makeText(TourOffersActivity.this, "No hay ofertas disponibles",
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando ofertas", e);
                Toast.makeText(TourOffersActivity.this,
                    "Error al cargar ofertas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterAndUpdateList() {
        List<TourOffer> filteredOffers = new ArrayList<>();

        for (TourOffer offer : allOffers) {
            String status = offer.getStatus();
            
            // IMPORTANTE: No mostrar ofertas ACEPTADAS (ya están en "Mis Tours")
            if ("ACEPTADA".equals(status)) {
                continue;
            }
            
            if (currentFilter.equals("ALL")) {
                filteredOffers.add(offer);
            } else if (currentFilter.equals("PENDING") && "PENDIENTE".equals(status)) {
                filteredOffers.add(offer);
            } else if (currentFilter.equals("REJECTED") && "RECHAZADA".equals(status)) {
                filteredOffers.add(offer);
            }
        }

        adapter.updateData(filteredOffers);
        Log.d(TAG, "📊 Ofertas filtradas (" + currentFilter + "): " + filteredOffers.size());

        // ✅ MOSTRAR ESTADO VACÍO SI NO HAY OFERTAS
        if (filteredOffers.isEmpty()) {
            showEmptyState(true);

            // Puedes personalizar el mensaje según el filtro
            TextView emptyMessage = emptyTourOffers.findViewById(R.id.empty_message);
            if (emptyMessage != null) {
                String message;
                switch (currentFilter) {
                    case "PENDING":
                        message = "No hay ofertas pendientes";
                        break;
                    case "REJECTED":
                        message = "No hay ofertas rechazadas";
                        break;
                    case "ALL":
                    default:
                        message = "No hay ofertas para mostrar";
                        break;
                }
                emptyMessage.setText(message);
            }
        } else {
            showEmptyState(false);
        }
    }

    private void initializeViews() {
        rvTourOffers = findViewById(R.id.rv_tour_offers);
        chipAll = findViewById(R.id.chip_all);
        chipPending = findViewById(R.id.chip_pending);
        chipRejected = findViewById(R.id.chip_rejected);
        emptyTourOffers = findViewById(R.id.empty_tour_offers);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void showEmptyState(boolean showEmpty) {
        if (emptyTourOffers != null && rvTourOffers != null) {
            if (showEmpty) {
                emptyTourOffers.setVisibility(View.VISIBLE);
                rvTourOffers.setVisibility(View.GONE);
            } else {
                emptyTourOffers.setVisibility(View.GONE);
                rvTourOffers.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvTourOffers.setLayoutManager(layoutManager);
        adapter = new TourOffersAdapter(new ArrayList<>());
        rvTourOffers.setAdapter(adapter);
    }

    private void setupFilters() {
        chipPending.setChecked(true);

        chipAll.setOnClickListener(v -> {
            currentFilter = "ALL";
            filterAndUpdateList();
        });

        chipPending.setOnClickListener(v -> {
            currentFilter = "PENDING";
            filterAndUpdateList();
        });

        chipRejected.setOnClickListener(v -> {
            currentFilter = "REJECTED";
            filterAndUpdateList();
        });
    }

    // Adapter for Tour Offers
    private class TourOffersAdapter extends RecyclerView.Adapter<TourOffersAdapter.ViewHolder> {

        private List<TourOffer> offers;

        TourOffersAdapter(List<TourOffer> offers) {
            this.offers = offers != null ? offers : new ArrayList<>();
        }

        public void updateData(List<TourOffer> newOffers) {
            this.offers = newOffers != null ? newOffers : new ArrayList<>();
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_tour_offer_guide, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TourOffer offer = offers.get(position);

            holder.tvTourName.setText(offer.getTourName() != null ? offer.getTourName() : "Tour sin nombre");
            holder.tvCompanyName.setText(offer.getCompanyName() != null ? offer.getCompanyName() : "Empresa no especificada");
            holder.tvTourDate.setText(offer.getTourDate() != null ? offer.getTourDate() : "Fecha no especificada");
            holder.tvTourTime.setText(offer.getTourTime() != null ? offer.getTourTime() : "Hora no especificada");
            holder.tvTourDuration.setText(offer.getTourDuration() != null ? offer.getTourDuration() : "4 horas");
            holder.tvPaymentAmount.setText(String.format("S/. %.2f", offer.getPaymentAmount() != null ? offer.getPaymentAmount() : 0.0));
            holder.tvParticipants.setText((offer.getNumberOfParticipants() != null ? offer.getNumberOfParticipants() : 0) + " personas");
            holder.tvOfferStatus.setText(offer.getStatus() != null ? offer.getStatus() : "PENDIENTE");

            // Set status color and visibility based on status
            String status = offer.getStatus();
            if ("PENDIENTE".equals(status)) {
                holder.tvOfferStatus.setTextColor(getColor(R.color.white));
                holder.tvOfferStatus.setBackgroundResource(R.drawable.circle_orange);
                // Mostrar botones de acción para ofertas pendientes
                holder.layoutPendingActions.setVisibility(View.VISIBLE);
                holder.layoutResponseStatus.setVisibility(View.GONE);
            } else if ("RECHAZADA".equals(status)) {
                holder.tvOfferStatus.setTextColor(getColor(R.color.white));
                holder.tvOfferStatus.setBackgroundResource(R.drawable.circle_red);
                // Ocultar botones de acción y mostrar mensaje de rechazada
                holder.layoutPendingActions.setVisibility(View.GONE);
                holder.layoutResponseStatus.setVisibility(View.VISIBLE);
                holder.tvResponseMessage.setText("Oferta rechazada");
                holder.tvResponseMessage.setTextColor(getColor(R.color.red));
                holder.ivResponseIcon.setColorFilter(getColor(R.color.red));
            } else if ("ACEPTADA".equals(status)) {
                holder.tvOfferStatus.setTextColor(getColor(R.color.white));
                holder.tvOfferStatus.setBackgroundResource(R.drawable.circle_green);
                // Ocultar botones de acción y mostrar mensaje de aceptada
                holder.layoutPendingActions.setVisibility(View.GONE);
                holder.layoutResponseStatus.setVisibility(View.VISIBLE);
                holder.tvResponseMessage.setText("Oferta aceptada - Tour asignado");
                holder.tvResponseMessage.setTextColor(getColor(R.color.green));
                holder.ivResponseIcon.setColorFilter(getColor(R.color.green));
            }

            // Handle button clicks
            holder.btnAcceptOffer.setOnClickListener(v -> {
                // Verificar conflicto de fechas antes de aceptar
                firestoreManager.getToursByGuide(offer.getGuideId(), new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        List<Tour> existingTours = (List<Tour>) result;
                        
                        // Verificar conflicto de fechas
                        for (Tour tour : existingTours) {
                            if (offer.getTourDate() != null && offer.getTourDate().equals(tour.getTourDate())) {
                                String status = tour.getTourStatus();
                                if ("EN_PROGRESO".equals(status) || "CONFIRMADA".equals(status)) {
                                    Toast.makeText(TourOffersActivity.this, 
                                        "❌ Ya tienes un tour aceptado para el " + offer.getTourDate(), 
                                        Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }
                        }
                        
                        // No hay conflicto, proceder a aceptar
                        acceptOfferAndAssignTour(offer, holder);
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(TourOffersActivity.this,
                            "Error verificando disponibilidad: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                });
            });

            holder.btnRejectOffer.setOnClickListener(v -> {
                // Marcar oferta como RECHAZADA en Firebase
                firestoreManager.updateOfferStatus(offer.getOfferId(), "RECHAZADA", new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(TourOffersActivity.this,
                            "Oferta rechazada",
                            Toast.LENGTH_SHORT).show();

                        // Actualizar UI
                        holder.layoutPendingActions.setVisibility(View.GONE);
                        holder.layoutResponseStatus.setVisibility(View.VISIBLE);
                        holder.tvResponseMessage.setText("Oferta rechazada");
                        holder.tvResponseMessage.setTextColor(getColor(R.color.red));
                        holder.ivResponseIcon.setColorFilter(getColor(R.color.red));

                        // Recargar ofertas
                        loadOffersFromFirebase();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(TourOffersActivity.this,
                            "Error al rechazar oferta: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                });
            });

            holder.btnViewDetails.setOnClickListener(v -> {
                Intent intent = new Intent(TourOffersActivity.this, TourOfferDetailsActivity.class);
                intent.putExtra("offerId", offer.getOfferId());
                intent.putExtra("tourName", offer.getTourName());
                intent.putExtra("companyName", offer.getCompanyName());
                intent.putExtra("tourDate", offer.getTourDate());
                intent.putExtra("tourTime", offer.getTourTime());
                intent.putExtra("tourDuration", offer.getTourDuration());
                intent.putExtra("paymentAmount", offer.getPaymentAmount() != null ? offer.getPaymentAmount() : 0.0);
                intent.putExtra("participants", offer.getNumberOfParticipants() != null ? offer.getNumberOfParticipants() : 0);
                intent.putExtra("status", offer.getStatus());
                // Formatear fecha de creación si existe
                String createdTime = "Enviado hace poco";
                if (offer.getCreatedAt() != null) {
                    long diff = new java.util.Date().getTime() - offer.getCreatedAt().getTime();
                    long hours = diff / (1000 * 60 * 60);
                    if (hours < 1) {
                        createdTime = "Enviado hace menos de 1 hora";
                    } else if (hours == 1) {
                        createdTime = "Enviado hace 1 hora";
                    } else if (hours < 24) {
                        createdTime = "Enviado hace " + hours + " horas";
                    } else {
                        long days = hours / 24;
                        createdTime = "Enviado hace " + days + " día" + (days > 1 ? "s" : "");
                    }
                }
                intent.putExtra("createdTime", createdTime);
                intent.putExtra("languages", "es,en"); // Por defecto español e inglés
                intent.putExtra("additionalNotes", offer.getAdditionalNotes() != null ? offer.getAdditionalNotes() : "");
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return offers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTourName, tvCompanyName, tvTourDate, tvTourTime, tvTourDuration,
                    tvPaymentAmount, tvParticipants, tvOfferStatus, tvResponseMessage;
            com.google.android.material.button.MaterialButton btnAcceptOffer, btnRejectOffer, btnViewDetails;
            android.widget.LinearLayout layoutPendingActions, layoutResponseStatus;
            android.widget.ImageView ivResponseIcon;

            ViewHolder(View itemView) {
                super(itemView);
                tvTourName = itemView.findViewById(R.id.tv_tour_name);
                tvCompanyName = itemView.findViewById(R.id.tv_company_name);
                tvTourDate = itemView.findViewById(R.id.tv_tour_date);
                tvTourTime = itemView.findViewById(R.id.tv_tour_time);
                tvTourDuration = itemView.findViewById(R.id.tv_tour_duration);
                tvPaymentAmount = itemView.findViewById(R.id.tv_payment_amount);
                tvParticipants = itemView.findViewById(R.id.tv_participants);
                tvOfferStatus = itemView.findViewById(R.id.tv_offer_status);
                tvResponseMessage = itemView.findViewById(R.id.tv_response_message);
                btnAcceptOffer = itemView.findViewById(R.id.btn_accept_offer);
                btnRejectOffer = itemView.findViewById(R.id.btn_reject_offer);
                btnViewDetails = itemView.findViewById(R.id.btn_view_details);
                layoutPendingActions = itemView.findViewById(R.id.layout_pending_actions);
                layoutResponseStatus = itemView.findViewById(R.id.layout_response_status);
                ivResponseIcon = itemView.findViewById(R.id.iv_response_icon);
            }
        }
    }

    private void acceptOfferAndAssignTour(TourOffer offer, TourOffersAdapter.ViewHolder holder) {
        Log.d(TAG, "🎯 ========================================");
        Log.d(TAG, "🎯 INICIANDO ACEPTACIÓN DE OFERTA");
        Log.d(TAG, "🎯 Offer ID: " + offer.getOfferId());
        Log.d(TAG, "🎯 Tour ID: " + offer.getTourId());
        Log.d(TAG, "🎯 Guide ID: " + offer.getGuideId());
        Log.d(TAG, "🎯 Tour Name: " + offer.getTourName());
        Log.d(TAG, "🎯 ========================================");
        
        // Actualizar estado de la oferta
        firestoreManager.updateOfferStatus(offer.getOfferId(), "ACEPTADA", new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "✅ PASO 1: Oferta marcada como ACEPTADA en Firestore");
                Log.d(TAG, "   → Ahora asignando guía al tour...");
                assignGuideToTour(offer, holder);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ ERROR en PASO 1: No se pudo marcar oferta como ACEPTADA", e);
                Toast.makeText(TourOffersActivity.this,
                    "Error al aceptar oferta: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void assignGuideToTour(TourOffer offer, TourOffersAdapter.ViewHolder holder) {
        Log.d(TAG, "🔄 PASO 2: Obteniendo tours existentes del guía");
        Log.d(TAG, "   → Guide ID: " + offer.getGuideId());
        
        // Obtener tours existentes del guía para determinar status
        firestoreManager.getToursByGuide(offer.getGuideId(), new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> existingTours = (List<Tour>) result;
                Log.d(TAG, "✅ PASO 2: Tours existentes obtenidos: " + existingTours.size());
                
                String tourStatus = determineTourStatus(existingTours, offer.getTourDate());
                Log.d(TAG, "📊 Status determinado para nuevo tour: " + tourStatus);
                
                // Actualizar el Tour con el guía asignado
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("assignedGuideId", offer.getGuideId());
                updates.put("assignedGuideName", offer.getGuideName());
                updates.put("tourStatus", tourStatus);
                updates.put("guidePayment", offer.getPaymentAmount());
                updates.put("isPublic", true);
                
                Log.d(TAG, "🔄 PASO 3: Actualizando Tour en Firestore");
                Log.d(TAG, "   → Tour ID: " + offer.getTourId());
                Log.d(TAG, "   → assignedGuideId: " + offer.getGuideId());
                Log.d(TAG, "   → assignedGuideName: " + offer.getGuideName());
                Log.d(TAG, "   → tourStatus: " + tourStatus);
                Log.d(TAG, "   → isPublic: true");
                Log.d(TAG, "   → Collection: Tours (NO Reservations)");
                
                firestoreManager.updateTour(offer.getTourId(), updates, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "✅✅✅ PASO 3 COMPLETADO: Tour actualizado exitosamente ✅✅✅");
                        Log.d(TAG, "   ✅ assignedGuideId guardado en Tour: " + offer.getGuideId());
                        Log.d(TAG, "   ✅ NO se creó ninguna Reservation");
                        Log.d(TAG, "   ✅ El tour debe aparecer en getToursByGuide()");
                        
                        // Si esta es la más cercana, actualizar los demás tours EN_PROGRESO a CONFIRMADA
                        if ("EN_PROGRESO".equals(tourStatus)) {
                            Log.d(TAG, "🔄 PASO 4: Actualizando otros tours a CONFIRMADA");
                            updateOtherToursToConfirmed(offer.getGuideId(), offer.getTourId());
                        }
                        
                        Toast.makeText(TourOffersActivity.this,
                            "✅ Oferta aceptada - Tour agregado a Mis Tours",
                            Toast.LENGTH_LONG).show();

                        // Actualizar UI
                        holder.layoutPendingActions.setVisibility(View.GONE);
                        holder.layoutResponseStatus.setVisibility(View.VISIBLE);
                        holder.tvResponseMessage.setText("Oferta aceptada - Tour en Mis Tours");
                        holder.tvResponseMessage.setTextColor(getColor(R.color.green));
                        holder.ivResponseIcon.setColorFilter(getColor(R.color.green));

                        // Recargar ofertas
                        loadOffersFromFirebase();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "❌❌❌ ERROR en PASO 3: No se pudo actualizar Tour ❌❌❌", e);
                        Log.e(TAG, "   ❌ Tour ID: " + offer.getTourId());
                        Log.e(TAG, "   ❌ Error: " + e.getMessage());
                        Toast.makeText(TourOffersActivity.this,
                            "Error al actualizar tour: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ ERROR en PASO 2: No se pudieron obtener tours del guía", e);
                Toast.makeText(TourOffersActivity.this,
                    "Error verificando tours: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private String determineTourStatus(List<Tour> existingTours, String newTourDate) {
        if (existingTours == null || existingTours.isEmpty()) {
            return "EN_PROGRESO";
        }
        
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.util.Date newDate = sdf.parse(newTourDate);
            java.util.Date today = new java.util.Date();
            
            if (newDate.before(today)) {
                return "CONFIRMADA";
            }
            
            for (Tour tour : existingTours) {
                String status = tour.getTourStatus();
                if ("EN_PROGRESO".equals(status) || "CONFIRMADA".equals(status)) {
                    try {
                        java.util.Date existingDate = sdf.parse(tour.getTourDate());
                        if (existingDate.before(newDate) && existingDate.after(today)) {
                            return "CONFIRMADA";
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parseando fecha: " + tour.getTourDate());
                    }
                }
            }
            
            return "EN_PROGRESO";
            
        } catch (Exception e) {
            Log.e(TAG, "Error determinando status", e);
            return "CONFIRMADA";
        }
    }
    
    private void updateOtherToursToConfirmed(String guideId, String currentTourId) {
        firestoreManager.getToursByGuide(guideId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> tours = (List<Tour>) result;
                for (Tour tour : tours) {
                    if ("EN_PROGRESO".equals(tour.getTourStatus()) && 
                        !currentTourId.equals(tour.getTourId())) {
                        java.util.Map<String, Object> updates = new java.util.HashMap<>();
                        updates.put("tourStatus", "CONFIRMADA");
                        firestoreManager.updateTour(tour.getTourId(), updates, 
                            new FirestoreManager.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    Log.d(TAG, "Tour actualizado a CONFIRMADA");
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Error actualizando tour", e);
                                }
                            });
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error obteniendo tours", e);
            }
        });
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
