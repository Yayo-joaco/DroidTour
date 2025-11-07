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
    
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea CLIENT
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_client_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ClientHistoryAdapter(v -> {
            // Al hacer click en un tour completado, ir a valoración
            startActivity(new Intent(this, TourRatingActivity.class));
        }));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
    
    private void redirectToLogin() {
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

class ClientHistoryAdapter extends RecyclerView.Adapter<ClientHistoryAdapter.ViewHolder> {
    interface OnHistoryClick { void onClick(View v); }
    private final OnHistoryClick onHistoryClick;
    
    ClientHistoryAdapter(OnHistoryClick listener) { 
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
        View item = holder.itemView;
        TextView tourName = item.findViewById(R.id.tv_tour_name);
        TextView companyName = item.findViewById(R.id.tv_company_name);
        TextView completionDate = item.findViewById(R.id.tv_completion_date);
        TextView paymentAmount = item.findViewById(R.id.tv_payment_amount);
        TextView status = item.findViewById(R.id.tv_completion_status);
        RatingBar ratingBar = item.findViewById(R.id.rating_bar);
        MaterialButton btnRate = item.findViewById(R.id.btn_rate_tour);
        MaterialButton btnDetails = item.findViewById(R.id.btn_view_details);

        // Datos de ejemplo para tours completados
        switch (position % 4) {
            case 0:
                tourName.setText("Valle Sagrado Completo");
                companyName.setText("Tours Cusco Adventures");
                completionDate.setText("10 Dic 2024 - Completado");
                paymentAmount.setText("S/. 250");
                ratingBar.setRating(4.5f);
                break;
            case 1:
                tourName.setText("City Tour Centro Histórico");
                companyName.setText("Tours Cusco Adventures");
                completionDate.setText("8 Dic 2024 - Completado");
                paymentAmount.setText("S/. 120");
                ratingBar.setRating(5.0f);
                break;
            case 2:
                tourName.setText("Machu Picchu Express");
                companyName.setText("Lima City Travel");
                completionDate.setText("5 Dic 2024 - Completado");
                paymentAmount.setText("S/. 180");
                ratingBar.setRating(4.0f);
                break;
            default:
                tourName.setText("Tour Gastronómico Lima");
                companyName.setText("Lima City Travel");
                completionDate.setText("3 Dic 2024 - Completado");
                paymentAmount.setText("S/. 90");
                ratingBar.setRating(4.8f);
        }

        btnRate.setOnClickListener(v -> onHistoryClick.onClick(v));
        btnDetails.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Detalles del tour", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() { return 8; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(View v) { super(v); } 
    }
}

