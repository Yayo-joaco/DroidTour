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

public class ClientReservationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_reservations);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = findViewById(R.id.rv_reservations);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ClientReservationsAdapter(v -> {
            // Al hacer click en una reserva, mostrar QRs
            startActivity(new Intent(this, ClientQRActivity.class));
        }));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

class ClientReservationsAdapter extends RecyclerView.Adapter<ClientReservationsAdapter.ViewHolder> {
    interface OnReservationClick { void onClick(View v); }
    private final OnReservationClick onReservationClick;
    
    ClientReservationsAdapter(OnReservationClick listener) { 
        this.onReservationClick = listener; 
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_client_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        View item = holder.itemView;
        TextView tourName = item.findViewById(R.id.tv_tour_name);
        TextView companyName = item.findViewById(R.id.tv_company_name);
        TextView tourDate = item.findViewById(R.id.tv_tour_date);
        TextView paymentAmount = item.findViewById(R.id.tv_payment_amount);
        TextView status = item.findViewById(R.id.tv_reservation_status);
        MaterialButton btnViewQR = item.findViewById(R.id.btn_view_qr);
        MaterialButton btnCancel = item.findViewById(R.id.btn_cancel_reservation);

        // Datos de ejemplo para reservas activas
        switch (position % 3) {
            case 0:
                tourName.setText("City Tour Centro Histórico");
                companyName.setText("Tours Cusco Adventures");
                tourDate.setText("15 Dic 2024 - 09:00 AM");
                paymentAmount.setText("S/. 120");
                status.setText("CONFIRMADA");
                break;
            case 1:
                tourName.setText("Valle Sagrado Completo");
                companyName.setText("Tours Cusco Adventures");
                tourDate.setText("18 Dic 2024 - 07:30 AM");
                paymentAmount.setText("S/. 250");
                status.setText("CONFIRMADA");
                break;
            default:
                tourName.setText("Machu Picchu Express");
                companyName.setText("Lima City Travel");
                tourDate.setText("20 Dic 2024 - 06:00 AM");
                paymentAmount.setText("S/. 180");
                status.setText("PENDIENTE");
                status.setTextColor(item.getContext().getResources().getColor(R.color.orange));
        }

        btnViewQR.setOnClickListener(v -> onReservationClick.onClick(v));
        btnCancel.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Reserva cancelada", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() { return 6; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(View v) { super(v); } 
    }
}

