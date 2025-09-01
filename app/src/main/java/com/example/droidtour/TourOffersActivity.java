package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TourOffersActivity extends AppCompatActivity {
    
    private RecyclerView rvTourOffers;
    // private FloatingActionButton fabFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_offers);
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadTourOffers();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Ofertas de Tours");
    }
    
    private void initializeViews() {
        rvTourOffers = findViewById(R.id.rv_tour_offers);
        // fabFilter = findViewById(R.id.fab_filter); // No existe en el layout
    }
    
    private void setupRecyclerView() {
        rvTourOffers.setLayoutManager(new LinearLayoutManager(this));
        rvTourOffers.setAdapter(new ExampleOffersAdapter());
    }
    
    private void setupClickListeners() {
        // fabFilter.setOnClickListener(v -> {
        //     Toast.makeText(this, "Filtros de ofertas", Toast.LENGTH_SHORT).show();
        //     // TODO: Mostrar dialog de filtros
        // });
    }
    
    private void loadTourOffers() {
        // TODO: Cargar ofertas de tours desde base de datos
        Toast.makeText(this, "Cargando ofertas de tours...", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

// Adaptador simple con datos de ejemplo para visualizar el diseño
class ExampleOffersAdapter extends RecyclerView.Adapter<ExampleOffersAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_offer_guide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Rellenar con ejemplos distintos
        android.content.Context ctx = holder.itemView.getContext();
        android.widget.TextView company = holder.itemView.findViewById(R.id.tv_company_name);
        android.widget.TextView date = holder.itemView.findViewById(R.id.tv_tour_date);
        android.widget.TextView time = holder.itemView.findViewById(R.id.tv_tour_time);
        android.widget.TextView duration = holder.itemView.findViewById(R.id.tv_tour_duration);
        android.widget.TextView amount = holder.itemView.findViewById(R.id.tv_payment_amount);
        android.widget.TextView participants = holder.itemView.findViewById(R.id.tv_participants);

        switch (position % 3) {
            case 0:
                company.setText("Inka Travel Peru");
                date.setText("Hoy");
                time.setText("09:30 AM");
                duration.setText("3 horas");
                amount.setText("S/. 200.00");
                participants.setText("6 personas");
                break;
            case 1:
                company.setText("Lima Adventure Tours");
                date.setText("Mañana");
                time.setText("08:00 AM");
                duration.setText("4 horas");
                amount.setText("S/. 180.00");
                participants.setText("10 personas");
                break;
            default:
                company.setText("Cusco Heritage");
                date.setText("15 Dic, 2024");
                time.setText("02:00 PM");
                duration.setText("5 horas");
                amount.setText("S/. 250.00");
                participants.setText("8 personas");
        }
    }

    @Override
    public int getItemCount() {
        return 8; // cantidad de ejemplos
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View itemView) {
            super(itemView);
        }
    }
}
