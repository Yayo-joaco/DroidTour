package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.client.ClientQRCodesActivity;
import com.example.droidtour.database.DatabaseHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

public class MyReservationsActivity extends AppCompatActivity {
    
    private RecyclerView rvReservations;
    private ReservationsAdapter reservationsAdapter;
    private ChipGroup chipGroupFilter;
    private TextView tvTotalReservations, tvActiveReservations, tvTotalSpent;
    
    // Storage Local
    private DatabaseHelper dbHelper;
    private List<DatabaseHelper.Reservation> allReservations;
    private List<DatabaseHelper.Reservation> filteredReservations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar Storage Local
        dbHelper = new DatabaseHelper(this);
        
        setupToolbar();
        initializeViews();
        loadReservationsFromDatabase();
        setupSummaryData();
        setupRecyclerView();
        setupFilters();
    }
    
    private void loadReservationsFromDatabase() {
        // ✅ CARGAR RESERVAS DE LA BASE DE DATOS
        allReservations = dbHelper.getAllReservations();
        // Inicialmente, mostrar todas
        filteredReservations = new java.util.ArrayList<>(allReservations);
        
        // Si no hay reservas, mostrar mensaje
        if (allReservations.isEmpty()) {
            Toast.makeText(this, "No tienes reservas aún", Toast.LENGTH_SHORT).show();
        }
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
        // ✅ CALCULAR ESTADÍSTICAS DESDE LA BASE DE DATOS
        int totalReservations = allReservations.size();
        int activeCount = 0;
        int completedCount = 0;
        double totalSpent = 0.0;
        
        for (DatabaseHelper.Reservation res : allReservations) {
            if (res.getStatus().equals("CONFIRMADA")) {
                activeCount++;
            } else if (res.getStatus().equals("COMPLETADA")) {
                completedCount++;
            }
            totalSpent += res.getPrice();
        }
        
        tvTotalReservations.setText("Total de reservas: " + totalReservations);
        tvActiveReservations.setText("Activas: " + activeCount + " • Completadas: " + completedCount);
        tvTotalSpent.setText("S/. " + String.format("%.0f", totalSpent));
    }

    private void setupRecyclerView() {
        rvReservations.setLayoutManager(new LinearLayoutManager(this));
        // ✅ PASAR LAS RESERVAS DE LA BASE DE DATOS AL ADAPTADOR
        reservationsAdapter = new ReservationsAdapter(filteredReservations, this::onReservationClick);
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
                
                applyFilter(filterType);
            }
        });
        // Seleccionar "Todas" al iniciar
        if (chipGroupFilter.getCheckedChipId() == View.NO_ID) {
            chipGroupFilter.check(R.id.chip_all);
        } else {
            applyFilter("todas");
        }
    }

    private void applyFilter(String filterType) {
        if (filteredReservations == null) return;
        filteredReservations.clear();

        if ("todas".equals(filterType)) {
            filteredReservations.addAll(allReservations);
        } else if ("activas".equals(filterType)) {
            for (DatabaseHelper.Reservation res : allReservations) {
                String status = safeStatus(res.getStatus());
                if (status.equals("CONFIRMADA") || status.equals("PENDIENTE") || status.equals("EN_PROCESO")) {
                    filteredReservations.add(res);
                }
            }
        } else if ("completadas".equals(filterType)) {
            for (DatabaseHelper.Reservation res : allReservations) {
                if (safeStatus(res.getStatus()).equals("COMPLETADA")) {
                    filteredReservations.add(res);
                }
            }
        } else if ("canceladas".equals(filterType)) {
            for (DatabaseHelper.Reservation res : allReservations) {
                String status = safeStatus(res.getStatus());
                if (status.equals("CANCELADA") || status.equals("CANCELADO") || status.equals("RECHAZADA") || status.equals("NO_PROCESADA") || status.equals("FALLIDA")) {
                    filteredReservations.add(res);
                }
            }
        }

        reservationsAdapter.notifyDataSetChanged();
        // Actualizar resumen con los elementos filtrados si es necesario
        tvTotalReservations.setText("Total de reservas: " + filteredReservations.size());
    }

    private String safeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
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
    private final List<DatabaseHelper.Reservation> reservations;
    
    ReservationsAdapter(List<DatabaseHelper.Reservation> reservations, OnReservationClick listener) { 
        this.reservations = reservations;
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
        // ✅ OBTENER RESERVA DE LA BASE DE DATOS
        DatabaseHelper.Reservation reservation = reservations.get(position);
        
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

        // ✅ USAR DATOS DE LA RESERVA
        tourName.setText(reservation.getTourName());
        companyName.setText(reservation.getCompany());
        tourDate.setText(reservation.getDate());
        tourTime.setText(reservation.getTime());
        participants.setText(reservation.getPeople() + " personas");
        paymentMethod.setText("Visa ****1234");
        totalAmount.setText("S/. " + String.format("%.2f", reservation.getPrice()));
        reservationCode.setText("Código: " + reservation.getQrCode());
        reservationDate.setText(reservation.getDate());
        
        // Set status and button visibility based on status from database
        boolean isCompleted = reservation.getStatus().equals("COMPLETADA");
        boolean isConfirmed = reservation.getStatus().equals("CONFIRMADA");
        
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

        // Button click listeners - ✅ USAR DATOS DE LA RESERVA
        btnViewQR.setOnClickListener(v -> {
            // Only for confirmed tours - go to QR codes
            Intent intent = new Intent(v.getContext(), ClientQRCodesActivity.class);
            intent.putExtra("reservation_id", reservation.getId());
            intent.putExtra("tour_name", reservation.getTourName());
            intent.putExtra("company_name", reservation.getCompany());
            intent.putExtra("qr_code", reservation.getQrCode());
            v.getContext().startActivity(intent);
        });
        
        btnContactCompany.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CompanyChatActivity.class);
            intent.putExtra("company_name", reservation.getCompany());
            intent.putExtra("tour_name", reservation.getTourName());
            v.getContext().startActivity(intent);
        });
        
        btnViewDetails.setOnClickListener(v -> {
            // Always go to tour details
            Intent intent = new Intent(v.getContext(), TourDetailActivity.class);
            intent.putExtra("tour_id", reservation.getId());
            intent.putExtra("tour_name", reservation.getTourName());
            intent.putExtra("company_name", reservation.getCompany());
            intent.putExtra("price", reservation.getPrice() / reservation.getPeople()); // Price per person
            v.getContext().startActivity(intent);
        });
        
        btnRateTour.setOnClickListener(v -> {
            // Only for completed tours - go to rating
            Intent intent = new Intent(v.getContext(), TourRatingActivity.class);
            intent.putExtra("tour_name", reservation.getTourName());
            intent.putExtra("company_name", reservation.getCompany());
            v.getContext().startActivity(intent);
        });
        
        // Remove general item click listener to avoid conflicts
        holder.itemView.setOnClickListener(null);
    }

    @Override
    public int getItemCount() { 
        // ✅ RETORNAR EL NÚMERO REAL DE RESERVAS
        return reservations.size(); 
    }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}