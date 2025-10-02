package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

public class MyReservationsActivity extends AppCompatActivity {
    
    private RecyclerView rvReservations;
    private ReservationsAdapter reservationsAdapter;
    private ChipGroup chipGroupFilter;
    private TextView tvTotalReservations, tvActiveReservations, tvTotalSpent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);

        setupToolbar();
        initializeViews();
        setupSummaryData();
        setupRecyclerView();
        setupFilters();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mis Reservas");
        }
    }

    private void initializeViews() {
        rvReservations = findViewById(R.id.rv_reservations);
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        tvTotalReservations = findViewById(R.id.tv_total_reservations);
        tvActiveReservations = findViewById(R.id.tv_active_reservations);
        tvTotalSpent = findViewById(R.id.tv_total_spent);
    }

    private void setupSummaryData() {
        tvTotalReservations.setText("Total de reservas: 8");
        tvActiveReservations.setText("Activas: 2 • Completadas: 6");
        tvTotalSpent.setText("S/. 680");
    }

    private void setupRecyclerView() {
        rvReservations.setLayoutManager(new LinearLayoutManager(this));
        reservationsAdapter = new ReservationsAdapter(this::onReservationClick);
        rvReservations.setAdapter(reservationsAdapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                String filterType = "";
                
                if (checkedId == R.id.chip_all) {
                    filterType = "todas";
                } else if (checkedId == R.id.chip_active) {
                    filterType = "activas";
                } else if (checkedId == R.id.chip_completed) {
                    filterType = "completadas";
                } else if (checkedId == R.id.chip_cancelled) {
                    filterType = "canceladas";
                }
                
                Toast.makeText(this, "Filtro: " + filterType, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onReservationClick(int position) {
        // This method is no longer used since buttons handle their own navigation
        // Kept for compatibility but does nothing
    }
    
    private String getTourName(int position) {
        String[] tourNames = {
            "City Tour Lima Centro Histórico",
            "Machu Picchu Full Day",
            "Islas Ballestas y Paracas",
            "Cañón del Colca 2D/1N"
        };
        return tourNames[position % tourNames.length];
    }
    
    private String getCompanyName(int position) {
        String[] companyNames = {
            "Lima Adventure Tours",
            "Cusco Explorer",
            "Paracas Tours",
            "Arequipa Adventures"
        };
        return companyNames[position % companyNames.length];
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

// Adaptador para las reservas del cliente
class ReservationsAdapter extends RecyclerView.Adapter<ReservationsAdapter.ViewHolder> {
    interface OnReservationClick { void onClick(int position); }
    private final OnReservationClick onReservationClick;
    
    ReservationsAdapter(OnReservationClick listener) { 
        this.onReservationClick = listener; 
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TextView tourName = holder.itemView.findViewById(R.id.tv_tour_name);
        TextView companyName = holder.itemView.findViewById(R.id.tv_company_name);
        TextView tourDate = holder.itemView.findViewById(R.id.tv_tour_date);
        TextView tourTime = holder.itemView.findViewById(R.id.tv_tour_time);
        TextView participants = holder.itemView.findViewById(R.id.tv_participants);
        TextView paymentMethod = holder.itemView.findViewById(R.id.tv_payment_method);
        TextView totalAmount = holder.itemView.findViewById(R.id.tv_total_amount);
        TextView reservationStatus = holder.itemView.findViewById(R.id.tv_reservation_status);
        TextView reservationCode = holder.itemView.findViewById(R.id.tv_reservation_code);
        TextView reservationDate = holder.itemView.findViewById(R.id.tv_reservation_date);
        MaterialButton btnViewQR = holder.itemView.findViewById(R.id.btn_view_qr);
        MaterialButton btnContactCompany = holder.itemView.findViewById(R.id.btn_contact_company);
        MaterialButton btnViewDetails = holder.itemView.findViewById(R.id.btn_view_details);
        MaterialButton btnRateTour = holder.itemView.findViewById(R.id.btn_rate_tour);
        android.view.View layoutQRSection = holder.itemView.findViewById(R.id.layout_qr_section);

        String[] tourNames = {
            "City Tour Lima Centro Histórico",
            "Machu Picchu Full Day",
            "Islas Ballestas y Paracas",
            "Cañón del Colca 2D/1N"
        };
        
        String[] companyNames = {
            "Lima Adventure Tours",
            "Cusco Explorer",
            "Paracas Tours",
            "Arequipa Adventures"
        };
        
        String[] dates = {"15 Dic, 2024", "18 Dic, 2024", "20 Dic, 2024", "22 Dic, 2024"};
        String[] times = {"09:00 AM", "06:00 AM", "08:00 AM", "07:30 AM"};
        String[] amounts = {"S/. 170.00", "S/. 360.00", "S/. 130.00", "S/. 240.00"};
        String[] codes = {"#DT2024001", "#DT2024002", "#DT2024003", "#DT2024004"};

        int index = position % tourNames.length;
        
        tourName.setText(tourNames[index]);
        companyName.setText(companyNames[index]);
        tourDate.setText(dates[index]);
        tourTime.setText(times[index]);
        participants.setText("2 personas");
        paymentMethod.setText("Visa ****1234");
        totalAmount.setText(amounts[index]);
        reservationCode.setText("Código: " + codes[index]);
        reservationDate.setText(dates[index].substring(0, 6));
        
        // Set status and button visibility based on position
        boolean isCompleted = (position >= 2); // Positions 2 and 3 are completed
        boolean isConfirmed = (position < 2);   // Positions 0 and 1 are confirmed
        
        if (isCompleted) {
            reservationStatus.setText("COMPLETADA");
            // Hide QR section for completed tours
            layoutQRSection.setVisibility(android.view.View.GONE);
            // Show rating button for completed tours
            btnRateTour.setVisibility(android.view.View.VISIBLE);
        } else if (isConfirmed) {
            reservationStatus.setText("CONFIRMADA");
            // Show QR section for confirmed tours
            layoutQRSection.setVisibility(android.view.View.VISIBLE);
            // Hide rating button for confirmed tours
            btnRateTour.setVisibility(android.view.View.GONE);
        } else {
            reservationStatus.setText("PENDIENTE");
            // Hide QR section for pending tours
            layoutQRSection.setVisibility(android.view.View.GONE);
            // Hide rating button for pending tours
            btnRateTour.setVisibility(android.view.View.GONE);
        }

        // Button click listeners
        btnViewQR.setOnClickListener(v -> {
            // Only for confirmed tours - go to QR codes
            Intent intent = new Intent(v.getContext(), ClientQRCodesActivity.class);
            intent.putExtra("reservation_id", position);
            intent.putExtra("tour_name", tourNames[index]);
            intent.putExtra("company_name", companyNames[index]);
            v.getContext().startActivity(intent);
        });
        
        btnContactCompany.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CompanyChatActivity.class);
            intent.putExtra("company_name", companyNames[index]);
            intent.putExtra("tour_name", tourNames[index]);
            v.getContext().startActivity(intent);
        });
        
        btnViewDetails.setOnClickListener(v -> {
            // Always go to tour details
            Intent intent = new Intent(v.getContext(), TourDetailActivity.class);
            intent.putExtra("tour_id", position);
            intent.putExtra("tour_name", tourNames[index]);
            intent.putExtra("company_name", companyNames[index]);
            intent.putExtra("price", 85.0 + (position * 30)); // Different prices
            v.getContext().startActivity(intent);
        });
        
        btnRateTour.setOnClickListener(v -> {
            // Only for completed tours - go to rating
            Intent intent = new Intent(v.getContext(), TourRatingActivity.class);
            intent.putExtra("tour_name", tourNames[index]);
            intent.putExtra("company_name", companyNames[index]);
            v.getContext().startActivity(intent);
        });
        
        // Remove general item click listener to avoid conflicts
        holder.itemView.setOnClickListener(null);
    }

    @Override
    public int getItemCount() { return 4; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}