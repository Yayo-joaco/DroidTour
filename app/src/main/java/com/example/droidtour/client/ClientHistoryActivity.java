package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.example.droidtour.TourRatingActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class ClientHistoryActivity extends AppCompatActivity {
    
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private String currentUserId;
    private RecyclerView rvHistory;
    private ClientHistoryAdapter adapter;
    private java.util.List<com.example.droidtour.models.Reservation> completedReservations = new java.util.ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_history);

        authManager = com.example.droidtour.firebase.FirebaseAuthManager.getInstance(this);
        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        currentUserId = authManager.getCurrentUserId();
        
        // 🔥 TEMPORAL: Para testing sin login
        if (currentUserId == null) {
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            android.widget.Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", android.widget.Toast.LENGTH_SHORT).show();
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rvHistory = findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClientHistoryAdapter(completedReservations, reservation -> {
            Intent intent = new Intent(this, com.example.droidtour.TourRatingActivity.class);
            intent.putExtra("reservation_id", reservation.getReservationId());
            intent.putExtra("tour_id", reservation.getTourId());
            intent.putExtra("tour_name", reservation.getTourName());
            intent.putExtra("company_name", reservation.getCompanyName());
            startActivity(intent);
        });
        rvHistory.setAdapter(adapter);
        
        loadCompletedReservations();
    }
    
    private void loadCompletedReservations() {
        firestoreManager.getReservationsByUser(currentUserId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                java.util.List<com.example.droidtour.models.Reservation> allReservations = 
                    (java.util.List<com.example.droidtour.models.Reservation>) result;
                
                completedReservations.clear();
                for (com.example.droidtour.models.Reservation reservation : allReservations) {
                    if ("COMPLETADA".equals(reservation.getStatus())) {
                        completedReservations.add(reservation);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClientHistoryActivity.this, "Error cargando historial", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

class ClientHistoryAdapter extends RecyclerView.Adapter<ClientHistoryAdapter.ViewHolder> {
    interface OnHistoryClick { void onClick(com.example.droidtour.models.Reservation reservation); }
    private final java.util.List<com.example.droidtour.models.Reservation> reservations;
    private final OnHistoryClick onHistoryClick;
    
    ClientHistoryAdapter(java.util.List<com.example.droidtour.models.Reservation> reservations, OnHistoryClick listener) {
        this.reservations = reservations;
        this.onHistoryClick = listener; 
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_client_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        com.example.droidtour.models.Reservation reservation = reservations.get(position);
        
        View item = holder.itemView;
        TextView tourName = item.findViewById(R.id.tv_tour_name);
        TextView companyName = item.findViewById(R.id.tv_company_name);
        TextView completionDate = item.findViewById(R.id.tv_completion_date);
        TextView paymentAmount = item.findViewById(R.id.tv_payment_amount);
        TextView status = item.findViewById(R.id.tv_completion_status);
        RatingBar ratingBar = item.findViewById(R.id.rating_bar);
        MaterialButton btnRate = item.findViewById(R.id.btn_rate_tour);
        MaterialButton btnDetails = item.findViewById(R.id.btn_view_details);

        tourName.setText(reservation.getTourName());
        companyName.setText(reservation.getCompanyName());
        completionDate.setText(reservation.getTourDate() + " - Completado");
        paymentAmount.setText("S/. " + String.format("%.2f", reservation.getTotalPrice()));
        status.setText("✅ Completado");
        ratingBar.setRating(0); // Sin rating hasta que el usuario califique

        btnRate.setOnClickListener(v -> onHistoryClick.onClick(reservation));
        btnDetails.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Detalles del tour", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() { return reservations.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(View v) { super(v); } 
    }
}

