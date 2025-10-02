package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

public class ToursCatalogActivity extends AppCompatActivity {
    
    private RecyclerView rvTours;
    private ToursCatalogAdapter toursAdapter;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private TextView tvCompanyName, tvCompanyRating, tvToursCount;
    
    private String companyName;
    private int companyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tours_catalog);

        getIntentData();
        setupToolbar();
        initializeViews();
        setupCompanyHeader();
        setupRecyclerView();
        setupFilters();
    }

    private void getIntentData() {
        companyId = getIntent().getIntExtra("company_id", 0);
        companyName = getIntent().getStringExtra("company_name");
        if (companyName == null) companyName = "Empresa de Tours";
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tours Disponibles");
        }
    }

    private void initializeViews() {
        rvTours = findViewById(R.id.rv_tours);
        etSearch = findViewById(R.id.et_search);
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvCompanyRating = findViewById(R.id.tv_company_rating);
        tvToursCount = findViewById(R.id.tv_tours_count);
    }

    private void setupCompanyHeader() {
        tvCompanyName.setText(companyName);
        
        switch (companyId % 3) {
            case 0:
                tvCompanyRating.setText("⭐ 4.8 • 245 reseñas");
                tvToursCount.setText("12 tours");
                break;
            case 1:
                tvCompanyRating.setText("⭐ 4.9 • 189 reseñas");
                tvToursCount.setText("8 tours");
                break;
            default:
                tvCompanyRating.setText("⭐ 4.7 • 156 reseñas");
                tvToursCount.setText("6 tours");
        }
    }

    private void setupRecyclerView() {
        rvTours.setLayoutManager(new LinearLayoutManager(this));
        toursAdapter = new ToursCatalogAdapter(this::onTourClick);
        rvTours.setAdapter(toursAdapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                String filterType = "";
                
                if (checkedId == R.id.chip_all) {
                    filterType = "todos";
                } else if (checkedId == R.id.chip_available) {
                    filterType = "disponibles";
                } else if (checkedId == R.id.chip_best_price) {
                    filterType = "mejor precio";
                } else if (checkedId == R.id.chip_duration) {
                    filterType = "duración";
                }
                
                Toast.makeText(this, "Filtro: " + filterType, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onTourClick(int position) {
        Intent intent = new Intent(this, TourDetailActivity.class);
        intent.putExtra("tour_id", position);
        intent.putExtra("company_name", companyName);
        intent.putExtra("tour_name", getTourName(position));
        intent.putExtra("price", getTourPrice(position));
        startActivity(intent);
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

    private double getTourPrice(int position) {
        double[] prices = {85.0, 180.0, 65.0, 120.0};
        return prices[position % prices.length];
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

// Adaptador para el catálogo de tours
class ToursCatalogAdapter extends RecyclerView.Adapter<ToursCatalogAdapter.ViewHolder> {
    interface OnTourClick { void onClick(int position); }
    private final OnTourClick onTourClick;
    
    ToursCatalogAdapter(OnTourClick listener) { 
        this.onTourClick = listener; 
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_catalog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TextView tourName = holder.itemView.findViewById(R.id.tv_tour_name);
        TextView tourDescription = holder.itemView.findViewById(R.id.tv_tour_description);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView duration = holder.itemView.findViewById(R.id.tv_duration);
        TextView groupSize = holder.itemView.findViewById(R.id.tv_group_size);
        TextView languages = holder.itemView.findViewById(R.id.tv_languages);
        TextView price = holder.itemView.findViewById(R.id.tv_price);
        MaterialButton btnReserve = holder.itemView.findViewById(R.id.btn_reserve);

        String[] tourNames = {
            "City Tour Lima Centro Histórico",
            "Machu Picchu Full Day",
            "Islas Ballestas y Paracas",
            "Cañón del Colca 2D/1N"
        };

        String[] descriptions = {
            "Descubre la historia y cultura de Lima visitando sus principales atractivos del centro histórico.",
            "Visita la maravilla del mundo con transporte y guía incluido desde Cusco.",
            "Observa lobos marinos, pingüinos y aves en su hábitat natural.",
            "Aventura de dos días por uno de los cañones más profundos del mundo."
        };

        String[] durations = {"4 horas", "Full Day", "6 horas", "2D/1N"};
        String[] prices = {"S/. 85.00", "S/. 180.00", "S/. 65.00", "S/. 120.00"};
        String[] ratings = {"⭐ 4.9", "⭐ 4.8", "⭐ 4.7", "⭐ 4.6"};

        int index = position % tourNames.length;
        
        tourName.setText(tourNames[index]);
        tourDescription.setText(descriptions[index]);
        rating.setText(ratings[index]);
        duration.setText(durations[index]);
        groupSize.setText("Max 12");
        languages.setText("ES, EN");
        price.setText(prices[index]);

        btnReserve.setOnClickListener(v -> onTourClick.onClick(position));
        holder.itemView.setOnClickListener(v -> onTourClick.onClick(position));
    }

    @Override
    public int getItemCount() { return 4; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

