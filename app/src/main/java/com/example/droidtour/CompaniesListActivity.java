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
        
        // Configurar búsqueda por nombre
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBySearchText(s.toString());
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    private void filterBySearchText(String searchText) {
        filteredCompanies.clear();
        
        if (searchText.isEmpty()) {
            filteredCompanies.addAll(allCompanies);
        } else {
            String lowerCaseSearch = searchText.toLowerCase();
            for (Company company : allCompanies) {
                String commercialName = company.getCommercialName();
                String businessName = company.getBusinessName();
                
                if ((commercialName != null && commercialName.toLowerCase().contains(lowerCaseSearch)) ||
                    (businessName != null && businessName.toLowerCase().contains(lowerCaseSearch))) {
                    filteredCompanies.add(company);
                }
            }
        }
        
        companiesAdapter.notifyDataSetChanged();
    }

    private void applyFilter(String filterType) {
        filteredCompanies.clear();

        switch (filterType) {
            case "all":
                filteredCompanies.addAll(allCompanies);
                break;
                
            case "best_rated":
                // Ordenar por mejor valoradas (basado en promedio de ratings de tours)
                List<Company> sortedByRating = new ArrayList<>(allCompanies);
                for (Company company : sortedByRating) {
                    calculateCompanyRating(company);
                }
                Collections.sort(sortedByRating, (c1, c2) -> {
                    double rating1 = getCompanyRatingCache(c1);
                    double rating2 = getCompanyRatingCache(c2);
                    return Double.compare(rating2, rating1);
                });
                filteredCompanies.addAll(sortedByRating);
                break;
                
            case "most_tours":
                // Ordenar por m\u00e1s tours
                List<Company> sortedByTours = new ArrayList<>(allCompanies);
                for (Company company : sortedByTours) {
                    calculateCompanyToursCount(company);
                }
                Collections.sort(sortedByTours, (c1, c2) -> {
                    int tours1 = getCompanyToursCountCache(c1);
                    int tours2 = getCompanyToursCountCache(c2);
                    return Integer.compare(tours2, tours1);
                });
                filteredCompanies.addAll(sortedByTours);
                break;
                
            case "best_price":
                // Ordenar por mejor precio (menor precio promedio de tours)
                List<Company> sortedByPrice = new ArrayList<>(allCompanies);
                for (Company company : sortedByPrice) {
                    calculateCompanyAveragePrice(company);
                }
                Collections.sort(sortedByPrice, (c1, c2) -> {
                    double price1 = getCompanyPriceCache(c1);
                    double price2 = getCompanyPriceCache(c2);
                    return Double.compare(price1, price2);
                });
                filteredCompanies.addAll(sortedByPrice);
                break;
        }

        companiesAdapter.notifyDataSetChanged();
    }

    // Caché temporal para ratings, tours count y precios
    private final java.util.Map<String, Double> ratingCache = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> toursCountCache = new java.util.HashMap<>();
    private final java.util.Map<String, Double> priceCache = new java.util.HashMap<>();

    private void calculateCompanyRating(Company company) {
        firestoreManager.getToursByCompany(company.getCompanyId(), new FirestoreManager.FirestoreCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<com.example.droidtour.models.Tour> tours = (List<com.example.droidtour.models.Tour>) result;
                    double totalRating = 0.0;
                    int count = 0;
                    for (com.example.droidtour.models.Tour tour : tours) {
                        if (tour.getAverageRating() != null && tour.getAverageRating() > 0) {
                            totalRating += tour.getAverageRating();
                            count++;
                        }
                    }
                    double avgRating = count > 0 ? totalRating / count : 0.0;
                    ratingCache.put(company.getCompanyId(), avgRating);
                }
            }
            @Override
            public void onFailure(Exception e) {
                ratingCache.put(company.getCompanyId(), 0.0);
            }
        });
    }

    private void calculateCompanyToursCount(Company company) {
        firestoreManager.getToursByCompany(company.getCompanyId(), new FirestoreManager.FirestoreCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<com.example.droidtour.models.Tour> tours = (List<com.example.droidtour.models.Tour>) result;
                    toursCountCache.put(company.getCompanyId(), tours.size());
                }
            }
            @Override
            public void onFailure(Exception e) {
                toursCountCache.put(company.getCompanyId(), 0);
            }
        });
    }

    private void calculateCompanyAveragePrice(Company company) {
        firestoreManager.getToursByCompany(company.getCompanyId(), new FirestoreManager.FirestoreCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<com.example.droidtour.models.Tour> tours = (List<com.example.droidtour.models.Tour>) result;
                    double totalPrice = 0.0;
                    int count = 0;
                    for (com.example.droidtour.models.Tour tour : tours) {
                        if (tour.getPricePerPerson() != null && tour.getPricePerPerson() > 0) {
                            totalPrice += tour.getPricePerPerson();
                            count++;
                        }
                    }
                    double avgPrice = count > 0 ? totalPrice / count : Double.MAX_VALUE;
                    priceCache.put(company.getCompanyId(), avgPrice);
                }
            }
            @Override
            public void onFailure(Exception e) {
                priceCache.put(company.getCompanyId(), Double.MAX_VALUE);
            }
        });
    }

    private double getCompanyRatingCache(Company company) {
        return ratingCache.getOrDefault(company.getCompanyId(), 0.0);
    }

    private int getCompanyToursCountCache(Company company) {
        return toursCountCache.getOrDefault(company.getCompanyId(), 0);
    }

    private double getCompanyPriceCache(Company company) {
        return priceCache.getOrDefault(company.getCompanyId(), Double.MAX_VALUE);
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
                .inflate(R.layout.item_popular_company, parent, false);
        return new ViewHolder(view, parent.getContext());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Company company = companies.get(position);
        holder.bind(company, position, onCompanyClick);
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
        private final android.content.Context context;
        private final android.widget.ImageView ivCompanyLogo;
        private final TextView tvCompanyName;
        private final TextView tvLocation;
        private final TextView tvRating;
        private final TextView tvToursCount;
        private final TextView tvReviewsCount;
        private final TextView tvExperienceYears;
        private final android.widget.ImageButton btnFavorite;
        private final com.google.android.material.chip.Chip chipService1;
        private final com.google.android.material.chip.Chip chipService2;
        private final com.google.android.material.chip.Chip chipService3;
        private final MaterialButton btnViewTours;

        ViewHolder(android.view.View v, android.content.Context ctx) {
            super(v);
            this.context = ctx;
            ivCompanyLogo = v.findViewById(R.id.iv_company_logo);
            tvCompanyName = v.findViewById(R.id.tv_company_name);
            tvLocation = v.findViewById(R.id.tv_company_location);
            tvRating = v.findViewById(R.id.tv_rating);
            tvToursCount = v.findViewById(R.id.tv_tours_count);
            tvReviewsCount = v.findViewById(R.id.tv_reviews_count);
            tvExperienceYears = v.findViewById(R.id.tv_experience_years);
            btnFavorite = v.findViewById(R.id.btn_favorite);
            chipService1 = v.findViewById(R.id.chip_service_1);
            chipService2 = v.findViewById(R.id.chip_service_2);
            chipService3 = v.findViewById(R.id.chip_service_3);
            btnViewTours = v.findViewById(R.id.btn_view_tours);
        }

        void bind(Company company, int position, OnCompanyClick onCompanyClick) {
            // Nombre
            String displayName = company.getCommercialName() != null ?
                    company.getCommercialName() : company.getBusinessName();
            tvCompanyName.setText(displayName != null ? displayName : "Empresa");

            // Logo
            if (ivCompanyLogo != null) {
                String logoUrl = company.getLogoUrl();
                if (logoUrl != null && !logoUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(context)
                            .load(logoUrl)
                            .placeholder(R.drawable.ic_company)
                            .error(R.drawable.ic_company)
                            .centerInside()
                            .into(ivCompanyLogo);
                } else {
                    ivCompanyLogo.setImageResource(R.drawable.ic_company);
                }
            }

            // Ubicación
            tvLocation.setText(company.getAddress() != null ? company.getAddress() : "Ubicación no disponible");

            // Ocultar corazón
            if (btnFavorite != null) {
                btnFavorite.setVisibility(android.view.View.GONE);
            }

            // Calcular años de experiencia
            int yearsExperience = 0;
            if (company.getCreatedAt() != null) {
                long diffMillis = new java.util.Date().getTime() - company.getCreatedAt().getTime();
                yearsExperience = (int) (diffMillis / (1000L * 60 * 60 * 24 * 365));
            }
            tvExperienceYears.setText(yearsExperience + "+");

            // Rating y reseñas - Cargar desde Firebase
            tvRating.setText("0.0");
            tvReviewsCount.setText("0 reseñas");
            tvToursCount.setText("0");

            FirestoreManager firestoreManager = FirestoreManager.getInstance();
            firestoreManager.getToursByCompany(company.getCompanyId(), new FirestoreManager.FirestoreCallback() {
                @Override
                @SuppressWarnings("unchecked")
                public void onSuccess(Object result) {
                    if (result instanceof java.util.List) {
                        java.util.List<com.example.droidtour.models.Tour> tours = (java.util.List<com.example.droidtour.models.Tour>) result;
                        int tourCount = tours.size();
                        tvToursCount.setText(String.valueOf(tourCount));

                        // Calcular rating promedio y total de reseñas
                        double totalRating = 0.0;
                        int totalReviews = 0;
                        for (com.example.droidtour.models.Tour tour : tours) {
                            if (tour.getAverageRating() != null) {
                                totalRating += tour.getAverageRating();
                            }
                            if (tour.getTotalReviews() != null) {
                                totalReviews += tour.getTotalReviews();
                            }
                        }

                        if (tourCount > 0 && totalRating > 0) {
                            double avgRating = totalRating / tourCount;
                            tvRating.setText(String.format("%.1f", avgRating));
                        }
                        tvReviewsCount.setText(totalReviews + " reseñas");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    tvToursCount.setText("0");
                    tvRating.setText("0.0");
                    tvReviewsCount.setText("0 reseñas");
                }
            });

            // Cargar servicios reales de la empresa
            if (company.getServiceIds() != null && !company.getServiceIds().isEmpty()) {
                firestoreManager.getServicesByCompany(company.getCompanyId(), new FirestoreManager.FirestoreCallback() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void onSuccess(Object result) {
                        if (result instanceof java.util.List) {
                            java.util.List<com.example.droidtour.models.Service> services = (java.util.List<com.example.droidtour.models.Service>) result;
                            
                            if (services.size() > 0 && chipService1 != null) {
                                chipService1.setText(services.get(0).getName());
                                chipService1.setVisibility(android.view.View.VISIBLE);
                            } else if (chipService1 != null) {
                                chipService1.setVisibility(android.view.View.GONE);
                            }
                            
                            if (services.size() > 1 && chipService2 != null) {
                                chipService2.setText(services.get(1).getName());
                                chipService2.setVisibility(android.view.View.VISIBLE);
                            } else if (chipService2 != null) {
                                chipService2.setVisibility(android.view.View.GONE);
                            }
                            
                            if (services.size() > 2 && chipService3 != null) {
                                chipService3.setText(services.get(2).getName());
                                chipService3.setVisibility(android.view.View.VISIBLE);
                            } else if (chipService3 != null) {
                                chipService3.setVisibility(android.view.View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (chipService1 != null) chipService1.setVisibility(android.view.View.GONE);
                        if (chipService2 != null) chipService2.setVisibility(android.view.View.GONE);
                        if (chipService3 != null) chipService3.setVisibility(android.view.View.GONE);
                    }
                });
            } else {
                if (chipService1 != null) chipService1.setVisibility(android.view.View.GONE);
                if (chipService2 != null) chipService2.setVisibility(android.view.View.GONE);
                if (chipService3 != null) chipService3.setVisibility(android.view.View.GONE);
            }

            // Click listeners
            itemView.setOnClickListener(v -> onCompanyClick.onClick(position));
            if (btnViewTours != null) {
                btnViewTours.setOnClickListener(v -> onCompanyClick.onClick(position));
            }
        }
    }
}