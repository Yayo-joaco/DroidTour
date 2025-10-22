package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

public class TourGuideMainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView rvPendingOffers, rvUpcomingTours;
    private MaterialCardView cardActiveTour, cardQRScanner, cardLocationTracking;
    private TextView tvViewAllOffers, tvViewAllTours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_guide_main);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupRecyclerViews();
        setupClickListeners();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        rvPendingOffers = findViewById(R.id.rv_pending_offers);
        rvUpcomingTours = findViewById(R.id.rv_upcoming_tours);
        cardActiveTour = findViewById(R.id.card_active_tour);
        cardQRScanner = findViewById(R.id.card_qr_scanner);
        cardLocationTracking = findViewById(R.id.card_location_tracking);
        tvViewAllOffers = findViewById(R.id.tv_view_all_offers);
        tvViewAllTours = findViewById(R.id.tv_view_all_tours);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, findViewById(R.id.toolbar),
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_home) {
                // Already in home
            } else if (id == R.id.nav_available_tours) {
                startActivity(new Intent(this, TourOffersActivity.class));
            } else if (id == R.id.nav_my_tours) {
                startActivity(new Intent(this, GuideActiveToursActivity.class));
            } else if (id == R.id.nav_tour_location) {
                startActivity(new Intent(this, LocationTrackingActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, GuideProfileActivity.class));
            } else if (id == R.id.nav_logout) {
                // Handle logout
                finish();
            }
            
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupRecyclerViews() {
        // Pending Offers RecyclerView (Horizontal)
        LinearLayoutManager offersLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvPendingOffers.setLayoutManager(offersLayoutManager);
        rvPendingOffers.setAdapter(new PendingOffersAdapter(this::onOfferClick));

        // Upcoming Tours RecyclerView (Vertical)
        LinearLayoutManager toursLayoutManager = new LinearLayoutManager(this);
        rvUpcomingTours.setLayoutManager(toursLayoutManager);
        rvUpcomingTours.setAdapter(new UpcomingToursAdapter(this::onTourClick));
    }

    private void setupClickListeners() {
        // Active Tour Card and Continue Button
        MaterialButton btnContinueTour = findViewById(R.id.btn_continue_tour);
        if (btnContinueTour != null) {
            btnContinueTour.setOnClickListener(v -> {
                Intent intent = new Intent(this, LocationTrackingActivity.class);
                intent.putExtra("tour_id", 1);
                intent.putExtra("tour_name", "City Tour Lima - Centro Histórico");
                startActivity(intent);
            });
        }

        cardActiveTour.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationTrackingActivity.class);
            intent.putExtra("tour_id", 1);
            intent.putExtra("tour_name", "City Tour Lima - Centro Histórico");
            startActivity(intent);
        });

        // View All Offers
        tvViewAllOffers.setOnClickListener(v -> {
            Intent intent = new Intent(this, TourOffersActivity.class);
            startActivity(intent);
        });

        // View All Tours
        tvViewAllTours.setOnClickListener(v -> {
            Intent intent = new Intent(this, GuideActiveToursActivity.class);
            startActivity(intent);
        });

        // QR Scanner
        cardQRScanner.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScannerActivity.class);
            intent.putExtra("tour_name", "City Tour Lima - Centro Histórico");
            startActivity(intent);
        });

        // Location Tracking
        cardLocationTracking.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationTrackingActivity.class);
            intent.putExtra("tour_name", "City Tour Lima - Centro Histórico");
            startActivity(intent);
        });
    }

    private void onOfferClick(int position) {
        Intent intent = new Intent(this, TourOfferDetailActivity.class);
        intent.putExtra("offer_id", position);
        startActivity(intent);
    }

    private void onTourClick(int position) {
        Intent intent = new Intent(this, GuideActiveTourDetailActivity.class);
        intent.putExtra("tour_id", position);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ============================= ADAPTERS =============================

    // Adapter for Pending Offers (Horizontal)
    private class PendingOffersAdapter extends RecyclerView.Adapter<PendingOffersAdapter.ViewHolder> {
        
        private final OnOfferClickListener listener;
        private final String[] tourNames = {
                "City Tour Lima Centro",
                "Cusco Mágico",
                "Paracas y Huacachina"
        };
        private final String[] companyNames = {
                "Lima Adventure",
                "Cusco Explorer",
                "Paracas Tours"
        };
        private final String[] dates = {
                "15 Dic, 2024",
                "20 Dic, 2024",
                "22 Dic, 2024"
        };
        private final String[] times = {
                "09:00 AM",
                "08:00 AM",
                "07:00 AM"
        };
        private final double[] payments = {180.0, 250.0, 200.0};
        private final int[] participants = {8, 12, 6};

        PendingOffersAdapter(OnOfferClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_pending_offer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.tvTourName.setText(tourNames[position]);
            holder.tvCompanyName.setText(companyNames[position]);
            holder.tvTourDate.setText(dates[position]);
            holder.tvTourTime.setText(times[position]);
            holder.tvPaymentAmount.setText(String.format("S/. %.0f", payments[position]));
            holder.tvParticipants.setText(participants[position] + " personas");

            holder.btnAccept.setOnClickListener(v -> {
                // Handle accept offer
                android.widget.Toast.makeText(TourGuideMainActivity.this, 
                    "Oferta aceptada: " + tourNames[position], 
                    android.widget.Toast.LENGTH_SHORT).show();
            });

            holder.btnReject.setOnClickListener(v -> {
                // Handle reject offer
                android.widget.Toast.makeText(TourGuideMainActivity.this, 
                    "Oferta rechazada", 
                    android.widget.Toast.LENGTH_SHORT).show();
            });

            holder.itemView.setOnClickListener(v -> listener.onClick(position));
        }

        @Override
        public int getItemCount() {
            return tourNames.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTourName, tvCompanyName, tvTourDate, tvTourTime, 
                     tvPaymentAmount, tvParticipants;
            com.google.android.material.button.MaterialButton btnAccept, btnReject;

            ViewHolder(View itemView) {
                super(itemView);
                tvTourName = itemView.findViewById(R.id.tv_tour_name);
                tvCompanyName = itemView.findViewById(R.id.tv_company_name);
                tvTourDate = itemView.findViewById(R.id.tv_tour_date);
                tvTourTime = itemView.findViewById(R.id.tv_tour_time);
                tvPaymentAmount = itemView.findViewById(R.id.tv_payment_amount);
                tvParticipants = itemView.findViewById(R.id.tv_participants);
                btnAccept = itemView.findViewById(R.id.btn_accept);
                btnReject = itemView.findViewById(R.id.btn_reject);
            }
        }
    }

    // Adapter for Upcoming Tours (Vertical)
    private class UpcomingToursAdapter extends RecyclerView.Adapter<UpcomingToursAdapter.ViewHolder> {
        
        private final OnOfferClickListener listener;
        private final String[] tourNames = {
                "Cusco Mágico Full Day",
                "Paracas y Huacachina"
        };
        private final String[] companyNames = {
                "Cusco Explorer Tours",
                "Paracas Adventure"
        };
        private final String[] dates = {
                "Mañana",
                "20 Dic"
        };
        private final String[] times = {
                "08:00 AM",
                "07:00 AM"
        };
        private final double[] payments = {250.0, 200.0};
        private final int[] participants = {12, 6};

        UpcomingToursAdapter(OnOfferClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_upcoming_tour, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.tvTourName.setText(tourNames[position]);
            holder.tvCompanyName.setText(companyNames[position]);
            holder.tvTourDate.setText(dates[position]);
            holder.tvTourTime.setText(times[position]);
            holder.tvPaymentAmount.setText(String.format("S/. %.0f", payments[position]));
            holder.tvParticipants.setText(participants[position] + " personas");

            holder.itemView.setOnClickListener(v -> listener.onClick(position));
        }

        @Override
        public int getItemCount() {
            return tourNames.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTourName, tvCompanyName, tvTourDate, tvTourTime, 
                     tvPaymentAmount, tvParticipants, tvStatus;
            View viewStatusIndicator;

            ViewHolder(View itemView) {
                super(itemView);
                tvTourName = itemView.findViewById(R.id.tv_tour_name);
                tvCompanyName = itemView.findViewById(R.id.tv_company_name);
                tvTourDate = itemView.findViewById(R.id.tv_tour_date);
                tvTourTime = itemView.findViewById(R.id.tv_tour_time);
                tvPaymentAmount = itemView.findViewById(R.id.tv_payment_amount);
                tvParticipants = itemView.findViewById(R.id.tv_participants);
                tvStatus = itemView.findViewById(R.id.tv_status);
                viewStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
            }
        }
    }

    // Click listener interface
    private interface OnOfferClickListener {
        void onClick(int position);
    }
}
