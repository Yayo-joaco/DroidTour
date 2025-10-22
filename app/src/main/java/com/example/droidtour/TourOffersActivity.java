package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;

public class TourOffersActivity extends AppCompatActivity {

    private RecyclerView rvTourOffers;
    private Chip chipAll, chipPending, chipRejected;
    private String currentFilter = "PENDING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_offers);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFilters();
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
        rvTourOffers.setAdapter(new TourOffersAdapter(currentFilter));
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
        // Reload adapter with filtered data
        rvTourOffers.setAdapter(new TourOffersAdapter(currentFilter));
    }

    // Adapter for Tour Offers
    private class TourOffersAdapter extends RecyclerView.Adapter<TourOffersAdapter.ViewHolder> {
        
        private final String[] allTourNames = {
                "City Tour Centro",
                "Cusco Mágico",
                "Paracas y Huacachina"
        };
        private final String[] allCompanyNames = {
                "Lima Adventure",
                "Cusco Explorer",
                "Paracas Tours"
        };
        private final String[] allDates = {
                "15 Dic, 2024",
                "20 Dic, 2024",
                "22 Dic, 2024"
        };
        private final String[] allTimes = {
                "09:00 AM",
                "08:00 AM",
                "07:00 AM"
        };
        private final String[] allDurations = {
                "4 horas",
                "8 horas",
                "2 días"
        };
        private final double[] allPayments = {180.0, 250.0, 200.0};
        private final int[] allParticipants = {8, 12, 6};
        private final String[] allStatuses = {"PENDIENTE", "PENDIENTE", "RECHAZADA"};
        
        private java.util.List<Integer> filteredIndices;
        
        TourOffersAdapter(String filter) {
            filteredIndices = new java.util.ArrayList<>();
            for (int i = 0; i < allTourNames.length; i++) {
                if (filter.equals("ALL")) {
                    filteredIndices.add(i);
                } else if (filter.equals("PENDING") && allStatuses[i].equals("PENDIENTE")) {
                    filteredIndices.add(i);
                } else if (filter.equals("REJECTED") && allStatuses[i].equals("RECHAZADA")) {
                    filteredIndices.add(i);
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
            int actualIndex = filteredIndices.get(position);
            
            holder.tvTourName.setText(allTourNames[actualIndex]);
            holder.tvCompanyName.setText(allCompanyNames[actualIndex]);
            holder.tvTourDate.setText(allDates[actualIndex]);
            holder.tvTourTime.setText(allTimes[actualIndex]);
            holder.tvTourDuration.setText(allDurations[actualIndex]);
            holder.tvPaymentAmount.setText(String.format("S/. %.2f", allPayments[actualIndex]));
            holder.tvParticipants.setText(allParticipants[actualIndex] + " personas");
            holder.tvOfferStatus.setText(allStatuses[actualIndex]);
            
            // Set status color
            if (allStatuses[actualIndex].equals("PENDIENTE")) {
                holder.tvOfferStatus.setTextColor(getColor(R.color.white));
                holder.tvOfferStatus.setBackgroundResource(R.drawable.circle_orange);
            } else if (allStatuses[actualIndex].equals("RECHAZADA")) {
                holder.tvOfferStatus.setTextColor(getColor(R.color.white));
                holder.tvOfferStatus.setBackgroundResource(R.drawable.circle_red);
            }

            // Handle button clicks
            holder.btnAcceptOffer.setOnClickListener(v -> {
                android.widget.Toast.makeText(TourOffersActivity.this, 
                    "Oferta aceptada: " + allTourNames[actualIndex], 
                    android.widget.Toast.LENGTH_SHORT).show();
                
                // Hide buttons and show accepted status
                holder.layoutPendingActions.setVisibility(View.GONE);
                holder.layoutResponseStatus.setVisibility(View.VISIBLE);
                holder.tvResponseMessage.setText("Oferta aceptada - Tour asignado");
                holder.tvResponseMessage.setTextColor(getColor(R.color.green));
                holder.ivResponseIcon.setColorFilter(getColor(R.color.green));
            });

            holder.btnRejectOffer.setOnClickListener(v -> {
                android.widget.Toast.makeText(TourOffersActivity.this, 
                    "Oferta rechazada", 
                    android.widget.Toast.LENGTH_SHORT).show();
                
                // Hide buttons and show rejected status
                holder.layoutPendingActions.setVisibility(View.GONE);
                holder.layoutResponseStatus.setVisibility(View.VISIBLE);
                holder.tvResponseMessage.setText("Oferta rechazada");
                holder.tvResponseMessage.setTextColor(getColor(R.color.red));
                holder.ivResponseIcon.setColorFilter(getColor(R.color.red));
            });

            holder.btnViewDetails.setOnClickListener(v -> {
                // Show details dialog or toast
                String details = "Tour: " + allTourNames[actualIndex] + "\n" +
                        "Empresa: " + allCompanyNames[actualIndex] + "\n" +
                        "Fecha: " + allDates[actualIndex] + "\n" +
                        "Hora: " + allTimes[actualIndex] + "\n" +
                        "Duración: " + allDurations[actualIndex] + "\n" +
                        "Pago: S/. " + String.format("%.2f", allPayments[actualIndex]) + "\n" +
                        "Participantes: " + allParticipants[actualIndex] + " personas";
                
                new android.app.AlertDialog.Builder(TourOffersActivity.this)
                    .setTitle("Detalles de la Oferta")
                    .setMessage(details)
                    .setPositiveButton("Cerrar", null)
                    .show();
            });
        }

        @Override
        public int getItemCount() {
            return filteredIndices.size();
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
}
