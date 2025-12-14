package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.client.CompanyChatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompaniesListActivity extends AppCompatActivity {

    private RecyclerView rvCompanies;
    private CompaniesAdapter companiesAdapter;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private List<Company> allCompanies;
    private List<Company> filteredCompanies;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar PreferencesManager PRIMERO
        com.example.droidtour.utils.PreferencesManager prefsManager =
                new com.example.droidtour.utils.PreferencesManager(this);

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
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar Firebase
        firestoreManager = FirestoreManager.getInstance();

        // Inicializar listas
        allCompanies = new ArrayList<>();
        filteredCompanies = new ArrayList<>();

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupFilters();

        // Cargar empresas desde Firestore
        loadCompaniesFromFirestore();
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

    /**
     * Cargar empresas desde Firestore
     */
    private void loadCompaniesFromFirestore() {
        firestoreManager.getCompanies(new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Company> companies = (List<Company>) result;

                allCompanies.clear();
                allCompanies.addAll(companies);

                filteredCompanies.clear();
                filteredCompanies.addAll(allCompanies);

                companiesAdapter.notifyDataSetChanged();

                if (allCompanies.isEmpty()) {
                    Toast.makeText(CompaniesListActivity.this,
                            "No hay empresas registradas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CompaniesListActivity.this,
                        "Error cargando empresas: " + e.getMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e("CompaniesListActivity", "Error loading companies", e);
            }
        });
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
            case "most_tours":
            case "best_price":
                // Por ahora solo mostrar todas, ya que Company no tiene estos campos
                // TODO: Implementar cuando se agreguen campos averageRating, totalTours
                filteredCompanies.addAll(allCompanies);
                break;
        }

        companiesAdapter.notifyDataSetChanged();
    }

    private void onCompanyClick(int position) {
        Company company = filteredCompanies.get(position);
        Intent intent = new Intent(this, ToursCatalogActivity.class);
        intent.putExtra("company_id", company.getCompanyId());
        intent.putExtra("company_name", getCompanyDisplayName(company));
        startActivity(intent);
    }

    /**
     * Obtener nombre de visualización de la empresa
     */
    private String getCompanyDisplayName(Company company) {
        if (company.getCommercialName() != null && !company.getCommercialName().isEmpty()) {
            return company.getCommercialName();
        }
        if (company.getBusinessName() != null && !company.getBusinessName().isEmpty()) {
            return company.getBusinessName();
        }
        return "Empresa";
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
    private final List<Company> companies;

    CompaniesAdapter(List<Company> companies, OnCompanyClick listener) {
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
        Company company = companies.get(position);

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

        // Nombre de la empresa (preferir commercialName, si no businessName)
        String displayName = getCompanyDisplayName(company);
        companyName.setText(displayName);

        // Ubicación: usar address si existe
        String locationText = "📍 ";
        if (company.getAddress() != null && !company.getAddress().isEmpty()) {
            locationText += company.getAddress();
        } else {
            locationText += "Ubicación no especificada";
        }
        location.setText(locationText);

        // Rating y reseñas (placeholder - Company actual no tiene estos campos)
        // TODO: Agregar estos campos al modelo Company o cargarlos de una colección separada
        rating.setText("⭐ 0.0");
        reviewsCount.setText("(0 reseñas)");

        // Total de tours (placeholder)
        // TODO: Cargar desde la colección de tours
        toursCount.setText("0");

        // Clientes (placeholder)
        clientsCount.setText("0");

        // Precio (placeholder)
        priceFrom.setText("--");

        // Descripción: usar businessType o RUC como info adicional
        String desc = buildCompanyDescription(company);
        description.setText(desc);

        // Botón de contacto
        btnContact.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), CompanyChatActivity.class);
            intent.putExtra("company_name", displayName);
            intent.putExtra("company_id", company.getCompanyId());
            v.getContext().startActivity(intent);
        });

        // Botón de ver tours
        btnViewTours.setOnClickListener(v -> onCompanyClick.onClick(position));
        holder.itemView.setOnClickListener(v -> onCompanyClick.onClick(position));
    }

    /**
     * Construir descripción de la empresa con la información disponible
     */
    private String buildCompanyDescription(Company company) {
        StringBuilder desc = new StringBuilder();

        if (company.getBusinessType() != null && !company.getBusinessType().isEmpty()) {
            desc.append(company.getBusinessType());
        }

        if (company.getRuc() != null && !company.getRuc().isEmpty()) {
            if (desc.length() > 0) desc.append(" • ");
            desc.append("RUC: ").append(company.getRuc());
        }

        if (company.getEmail() != null && !company.getEmail().isEmpty()) {
            if (desc.length() > 0) desc.append(" • ");
            desc.append(company.getEmail());
        }

        if (desc.length() == 0) {
            desc.append("Empresa de turismo");
        }

        return desc.toString();
    }

    /**
     * Obtener nombre de visualización de la empresa
     */
    private String getCompanyDisplayName(Company company) {
        if (company.getCommercialName() != null && !company.getCommercialName().isEmpty()) {
            return company.getCommercialName();
        }
        if (company.getBusinessName() != null && !company.getBusinessName().isEmpty()) {
            return company.getBusinessName();
        }
        return "Empresa";
    }

    @Override
    public int getItemCount() { return companies.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View v) { super(v); }
    }
}