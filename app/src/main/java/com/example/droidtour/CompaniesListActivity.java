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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

public class CompaniesListActivity extends AppCompatActivity {
    
    private RecyclerView rvCompanies;
    private CompaniesAdapter companiesAdapter;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_companies_list);

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupFilters();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Empresas de Turismo");
        }
    }

    private void initializeViews() {
        rvCompanies = findViewById(R.id.rv_companies);
        etSearch = findViewById(R.id.et_search);
        chipGroupFilter = findViewById(R.id.chip_group_filter);
    }

    private void setupRecyclerView() {
        rvCompanies.setLayoutManager(new LinearLayoutManager(this));
        companiesAdapter = new CompaniesAdapter(this::onCompanyClick);
        rvCompanies.setAdapter(companiesAdapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                String filterType = "";
                
                if (checkedId == R.id.chip_all) {
                    filterType = "all";
                } else if (checkedId == R.id.chip_best_rated) {
                    filterType = "best_rated";
                } else if (checkedId == R.id.chip_most_tours) {
                    filterType = "most_tours";
                } else if (checkedId == R.id.chip_best_price) {
                    filterType = "best_price";
                }
                
                // Apply filter (in real app, this would filter the data)
                Toast.makeText(this, "Filtro aplicado: " + filterType, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCompanyClick(int position) {
        Intent intent = new Intent(this, ToursCatalogActivity.class);
        intent.putExtra("company_id", position);
        intent.putExtra("company_name", getCompanyName(position));
        startActivity(intent);
    }

    private String getCompanyName(int position) {
        switch (position % 3) {
            case 0: return "Lima Adventure Tours";
            case 1: return "Cusco Explorer";
            default: return "Arequipa Adventures";
        }
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

// Adaptador para la lista detallada de empresas
class CompaniesAdapter extends RecyclerView.Adapter<CompaniesAdapter.ViewHolder> {
    interface OnCompanyClick { void onClick(int position); }
    private final OnCompanyClick onCompanyClick;
    
    CompaniesAdapter(OnCompanyClick listener) { 
        this.onCompanyClick = listener; 
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TextView companyName = holder.itemView.findViewById(R.id.tv_company_name);
        TextView location = holder.itemView.findViewById(R.id.tv_company_location);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView reviewsCount = holder.itemView.findViewById(R.id.tv_reviews_count);
        TextView toursCount = holder.itemView.findViewById(R.id.tv_tours_count);
        TextView clientsCount = holder.itemView.findViewById(R.id.tv_clients_count);
        TextView priceFrom = holder.itemView.findViewById(R.id.tv_price_from);
        MaterialButton btnContact = holder.itemView.findViewById(R.id.btn_contact);
        MaterialButton btnViewTours = holder.itemView.findViewById(R.id.btn_view_tours);

        switch (position % 3) {
            case 0:
                companyName.setText("Lima Adventure Tours");
                location.setText("📍 Lima, Perú");
                rating.setText("⭐ 4.8");
                reviewsCount.setText("(245 reseñas)");
                toursCount.setText("12");
                clientsCount.setText("1,245");
                priceFrom.setText("S/. 45");
                break;
            case 1:
                companyName.setText("Cusco Explorer");
                location.setText("📍 Cusco, Perú");
                rating.setText("⭐ 4.9");
                reviewsCount.setText("(189 reseñas)");
                toursCount.setText("8");
                clientsCount.setText("987");
                priceFrom.setText("S/. 65");
                break;
            default:
                companyName.setText("Arequipa Adventures");
                location.setText("📍 Arequipa, Perú");
                rating.setText("⭐ 4.7");
                reviewsCount.setText("(156 reseñas)");
                toursCount.setText("6");
                clientsCount.setText("654");
                priceFrom.setText("S/. 55");
        }

        btnContact.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Contactando empresa...", Toast.LENGTH_SHORT).show();
        });

        btnViewTours.setOnClickListener(v -> onCompanyClick.onClick(position));
        holder.itemView.setOnClickListener(v -> onCompanyClick.onClick(position));
    }

    @Override
    public int getItemCount() { return 3; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

