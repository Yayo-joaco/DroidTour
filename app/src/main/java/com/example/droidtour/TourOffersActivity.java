package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.database.DatabaseHelper;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import java.util.List;

public class TourOffersActivity extends AppCompatActivity {

    private RecyclerView rvTourOffers;
    private Chip chipAll, chipPending, chipRejected;
    private String currentFilter = "PENDING";
    
    // Storage Local
    private DatabaseHelper dbHelper;
    private NotificationHelper notificationHelper;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private List<DatabaseHelper.Offer> allOffers;

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
        
        // Validar que el usuario sea un guía
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_tour_offers);

        // Inicializar Storage Local
        dbHelper = new DatabaseHelper(this);
        notificationHelper = new NotificationHelper(this);

        initializeViews();
        setupToolbar();
        loadOffersFromDatabase();
        setupRecyclerView();
        setupFilters();
    }
    
    private void loadOffersFromDatabase() {
        // ✅ CARGAR OFERTAS DE LA BASE DE DATOS
        allOffers = dbHelper.getAllOffers();
        
        if (allOffers.isEmpty()) {
            android.widget.Toast.makeText(this, "No hay ofertas disponibles", 
                android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        rvTourOffers = findViewById(R.id.rv_tour_offers);
        chipAll = findViewById(R.id.chip_all);
        chipPending = findViewById(R.id.chip_pending);
        chipRejected = findViewById(R.id.chip_rejected);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvTourOffers.setLayoutManager(layoutManager);
        // ✅ PASAR OFERTAS DE LA BD AL ADAPTADOR
        rvTourOffers.setAdapter(new TourOffersAdapter(allOffers, currentFilter));
    }

    private void setupFilters() {
        chipPending.setChecked(true);
        
        chipAll.setOnClickListener(v -> {
            currentFilter = "ALL";
            refreshList();
        });
        
        chipPending.setOnClickListener(v -> {
            currentFilter = "PENDING";
            refreshList();
        });
        
        chipRejected.setOnClickListener(v -> {
            currentFilter = "REJECTED";
            refreshList();
        });
    }

    private void refreshList() {
        // ✅ RECARGAR DATOS DE LA BD Y ACTUALIZAR ADAPTADOR
        allOffers = dbHelper.getAllOffers();
        rvTourOffers.setAdapter(new TourOffersAdapter(allOffers, currentFilter));
    }

    // Adapter for Tour Offers
    private class TourOffersAdapter extends RecyclerView.Adapter<TourOffersAdapter.ViewHolder> {
        
        private final List<DatabaseHelper.Offer> offers;
        private final List<DatabaseHelper.Offer> filteredOffers;
        
        TourOffersAdapter(List<DatabaseHelper.Offer> allOffers, String filter) {
            this.offers = allOffers;
            this.filteredOffers = new java.util.ArrayList<>();
            
            // ✅ FILTRAR OFERTAS SEGÚN EL FILTRO SELECCIONADO
            for (DatabaseHelper.Offer offer : allOffers) {
                if (filter.equals("ALL")) {
                    filteredOffers.add(offer);
                } else if (filter.equals("PENDING") && offer.getStatus().equals("PENDIENTE")) {
                    filteredOffers.add(offer);
                } else if (filter.equals("REJECTED") && offer.getStatus().equals("RECHAZADA")) {
                    filteredOffers.add(offer);
                }
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_tour_offer_guide, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // ✅ OBTENER OFERTA DE LA BASE DE DATOS
            DatabaseHelper.Offer offer = filteredOffers.get(position);
            
            holder.tvTourName.setText(offer.getTourName());
            holder.tvCompanyName.setText(offer.getCompany());
            holder.tvTourDate.setText(offer.getDate());
            holder.tvTourTime.setText(offer.getTime());
            holder.tvTourDuration.setText("4 horas"); // Duración por defecto
            holder.tvPaymentAmount.setText(String.format("S/. %.2f", offer.getPayment()));
            holder.tvParticipants.setText(offer.getParticipants() + " personas");
            holder.tvOfferStatus.setText(offer.getStatus());
            
            // Set status color
            if (offer.getStatus().equals("PENDIENTE")) {
                holder.tvOfferStatus.setTextColor(getColor(R.color.white));
                holder.tvOfferStatus.setBackgroundResource(R.drawable.circle_orange);
            } else if (offer.getStatus().equals("RECHAZADA")) {
                holder.tvOfferStatus.setTextColor(getColor(R.color.white));
                holder.tvOfferStatus.setBackgroundResource(R.drawable.circle_red);
            }

            // Handle button clicks
            holder.btnAcceptOffer.setOnClickListener(v -> {
                // ✅ GUARDAR ACEPTACIÓN EN BASE DE DATOS
                
                // 1. Marcar oferta como ACEPTADA
                dbHelper.updateOfferStatus(offer.getId(), "ACEPTADA");
                
                // 2. Agregar tour a "Mis Tours"
                dbHelper.addTour(
                    offer.getTourName(),
                    offer.getCompany(),
                    offer.getDate(),
                    offer.getTime(),
                    "PROGRAMADO",                    // estado inicial
                    offer.getPayment(),
                    offer.getParticipants()
                );
                
                // 3. Enviar notificación
                notificationHelper.sendTourReminderNotification(
                    offer.getTourName(), 
                    offer.getTime()
                );
                
                // 4. Mostrar mensaje
                android.widget.Toast.makeText(TourOffersActivity.this, 
                    "✅ Oferta aceptada: " + offer.getTourName(), 
                    android.widget.Toast.LENGTH_LONG).show();
                
                // 5. Actualizar UI
                holder.layoutPendingActions.setVisibility(View.GONE);
                holder.layoutResponseStatus.setVisibility(View.VISIBLE);
                holder.tvResponseMessage.setText("Oferta aceptada - Tour asignado");
                holder.tvResponseMessage.setTextColor(getColor(R.color.green));
                holder.ivResponseIcon.setColorFilter(getColor(R.color.green));
                
                // 6. Recargar lista para actualizar dashboard
                refreshList();
            });

            holder.btnRejectOffer.setOnClickListener(v -> {
                // ✅ GUARDAR RECHAZO EN BASE DE DATOS
                
                // 1. Marcar oferta como RECHAZADA
                dbHelper.updateOfferStatus(offer.getId(), "RECHAZADA");
                
                // 2. Mostrar mensaje
                android.widget.Toast.makeText(TourOffersActivity.this, 
                    "Oferta rechazada", 
                    android.widget.Toast.LENGTH_SHORT).show();
                
                // 3. Actualizar UI
                holder.layoutPendingActions.setVisibility(View.GONE);
                holder.layoutResponseStatus.setVisibility(View.VISIBLE);
                holder.tvResponseMessage.setText("Oferta rechazada");
                holder.tvResponseMessage.setTextColor(getColor(R.color.red));
                holder.ivResponseIcon.setColorFilter(getColor(R.color.red));
                
                // 4. Recargar lista
                refreshList();
            });

            holder.btnViewDetails.setOnClickListener(v -> {
                // ✅ MOSTRAR DETALLES DE LA OFERTA DESDE LA BD
                String details = "Tour: " + offer.getTourName() + "\n" +
                        "Empresa: " + offer.getCompany() + "\n" +
                        "Fecha: " + offer.getDate() + "\n" +
                        "Hora: " + offer.getTime() + "\n" +
                        "Duración: 4 horas\n" +
                        "Pago: S/. " + String.format("%.2f", offer.getPayment()) + "\n" +
                        "Participantes: " + offer.getParticipants() + " personas\n" +
                        "Estado: " + offer.getStatus();
                
                new android.app.AlertDialog.Builder(TourOffersActivity.this)
                    .setTitle("Detalles de la Oferta")
                    .setMessage(details)
                    .setPositiveButton("Cerrar", null)
                    .show();
            });
        }

        @Override
        public int getItemCount() {
            // ✅ RETORNAR EL NÚMERO REAL DE OFERTAS FILTRADAS
            return filteredOffers.size();
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
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
