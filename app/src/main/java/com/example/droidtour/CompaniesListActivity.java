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
import com.example.droidtour.database.DatabaseHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CompaniesListActivity extends AppCompatActivity {
    
    private RecyclerView rvCompanies;
    private CompaniesAdapter companiesAdapter;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private List<DatabaseHelper.Company> allCompanies;
    private List<DatabaseHelper.Company> filteredCompanies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        com.example.droidtour.utils.PreferencesManager prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
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
        
        setContentView(R.layout.activity_companies_list);

        setupToolbar();
        initializeViews();
        initializeCompanies();
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

    private void initializeCompanies() {
        allCompanies = new ArrayList<>();
        allCompanies.add(new DatabaseHelper.Company("Lima Adventure Tours", "📍 Lima, Perú", 4.8, 245, 12, 1245, 45.0, 
            "Especialistas en tours culturales y gastronómicos por Lima. Más de 10 años de experiencia mostrando lo mejor de nuestra ciudad."));
        allCompanies.add(new DatabaseHelper.Company("Cusco Explorer", "📍 Cusco, Perú", 4.9, 189, 8, 987, 65.0, 
            "Especialistas en tours culturales y gastronómicos por Cusco. Más de 8 años de experiencia mostrando lo mejor de nuestra ciudad."));
        allCompanies.add(new DatabaseHelper.Company("Arequipa Adventures", "📍 Arequipa, Perú", 4.7, 156, 6, 654, 55.0, 
            "Especialistas en tours culturales y gastronómicos por Arequipa. Más de 6 años de experiencia mostrando lo mejor de nuestra ciudad."));
        allCompanies.add(new DatabaseHelper.Company("Trujillo Tours", "📍 Trujillo, Perú", 4.6, 98, 4, 432, 35.0, 
            "Especialistas en tours arqueológicos por Trujillo. Más de 5 años de experiencia mostrando las ruinas de Chan Chan."));
        allCompanies.add(new DatabaseHelper.Company("Iquitos Jungle Tours", "📍 Iquitos, Perú", 4.5, 76, 3, 321, 85.0, 
            "Especialistas en tours de selva por Iquitos. Más de 7 años de experiencia mostrando la biodiversidad amazónica."));
        allCompanies.add(new DatabaseHelper.Company("Puno Lake Tours", "📍 Puno, Perú", 4.8, 134, 5, 567, 75.0, 
            "Especialistas en tours del Lago Titicaca. Más de 9 años de experiencia mostrando las islas flotantes."));
        
        filteredCompanies = new ArrayList<>(allCompanies);
    }

    private void setupRecyclerView() {
        rvCompanies.setLayoutManager(new LinearLayoutManager(this));
        companiesAdapter = new CompaniesAdapter(filteredCompanies, this::onCompanyClick);
        rvCompanies.setAdapter(companiesAdapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                
                if (checkedId == R.id.chip_all) {
                    applyFilter("all");
                } else if (checkedId == R.id.chip_best_rated) {
                    applyFilter("best_rated");
                } else if (checkedId == R.id.chip_most_tours) {
                    applyFilter("most_tours");
                } else if (checkedId == R.id.chip_best_price) {
                    applyFilter("best_price");
                }
            }
        });
    }

    private void applyFilter(String filterType) {
        filteredCompanies.clear();
        
        switch (filterType) {
            case "all":
                filteredCompanies.addAll(allCompanies);
                break;
            case "best_rated":
                filteredCompanies.addAll(allCompanies);
                Collections.sort(filteredCompanies, (c1, c2) -> Double.compare(c2.getRating(), c1.getRating()));
                break;
            case "most_tours":
                filteredCompanies.addAll(allCompanies);
                Collections.sort(filteredCompanies, (c1, c2) -> Integer.compare(c2.getToursCount(), c1.getToursCount()));
                break;
            case "best_price":
                filteredCompanies.addAll(allCompanies);
                Collections.sort(filteredCompanies, (c1, c2) -> Double.compare(c1.getPriceFrom(), c2.getPriceFrom()));
                break;
        }
        
        companiesAdapter.notifyDataSetChanged();
    }

    private void onCompanyClick(int position) {
        Intent intent = new Intent(this, ToursCatalogActivity.class);
        intent.putExtra("company_id", position);
        intent.putExtra("company_name", filteredCompanies.get(position).getName());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

// Adaptador para la lista detallada de empresas
class CompaniesAdapter extends RecyclerView.Adapter<CompaniesAdapter.ViewHolder> {
    interface OnCompanyClick { void onClick(int position); }
    private final OnCompanyClick onCompanyClick;
    private final List<DatabaseHelper.Company> companies;
    
    CompaniesAdapter(List<DatabaseHelper.Company> companies, OnCompanyClick listener) { 
        this.companies = companies;
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
        DatabaseHelper.Company company = companies.get(position);
        
        TextView companyName = holder.itemView.findViewById(R.id.tv_company_name);
        TextView location = holder.itemView.findViewById(R.id.tv_company_location);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView reviewsCount = holder.itemView.findViewById(R.id.tv_reviews_count);
        TextView toursCount = holder.itemView.findViewById(R.id.tv_tours_count);
        TextView clientsCount = holder.itemView.findViewById(R.id.tv_clients_count);
        TextView priceFrom = holder.itemView.findViewById(R.id.tv_price_from);
        TextView description = holder.itemView.findViewById(R.id.tv_company_description);
        MaterialButton btnContact = holder.itemView.findViewById(R.id.btn_contact);
        MaterialButton btnViewTours = holder.itemView.findViewById(R.id.btn_view_tours);

        companyName.setText(company.getName());
        location.setText(company.getLocation());
        rating.setText("⭐ " + company.getRating());
        reviewsCount.setText("(" + company.getReviewsCount() + " reseñas)");
        toursCount.setText(String.valueOf(company.getToursCount()));
        clientsCount.setText(String.valueOf(company.getClientsCount()));
        priceFrom.setText("S/. " + (int)company.getPriceFrom());
        description.setText(company.getDescription());

        btnContact.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), CompanyChatActivity.class);
            intent.putExtra("company_name", company.getName());
            v.getContext().startActivity(intent);
        });

        btnViewTours.setOnClickListener(v -> onCompanyClick.onClick(position));
        holder.itemView.setOnClickListener(v -> onCompanyClick.onClick(position));
    }

    @Override
    public int getItemCount() { return companies.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

