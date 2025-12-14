package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.droidtour.models.Tour;
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

    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private String companyId, companyName;
    private java.util.List<Tour> allTours = new java.util.ArrayList<>();
    private java.util.List<Tour> filteredTours = new java.util.ArrayList<>();

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

        setContentView(R.layout.activity_tours_catalog);

        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();

        getIntentData();
        setupToolbar();
        initializeViews();
        setupCompanyHeader(); // Establecer header inicial
        loadCompanyFromFirebase();
        setupRecyclerView();
        loadToursFromFirebase();
        bindSearch();
        setupFilters();
    }

    private void getIntentData() {
        companyId = getIntent().getStringExtra("company_id");
        companyName = getIntent().getStringExtra("company_name");
        if (companyName == null) companyName = "Empresa de Tours";
        if (companyId == null) companyId = "";
    }

    private void loadCompanyFromFirebase() {
        if (companyId == null || companyId.isEmpty()) {
            setupCompanyHeader();
            return;
        }

        firestoreManager.getCompany(companyId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.Company company = (com.example.droidtour.models.Company) result;
                displayCompanyInfo(company);
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("ToursCatalog", "Error cargando empresa: " + e.getMessage());
                setupCompanyHeader(); // Usar valores por defecto
            }
        });
    }

    /**
     * Mostrar información de la empresa
     */
    private void displayCompanyInfo(com.example.droidtour.models.Company company) {
        // Nombre: preferir commercialName, luego businessName
        String displayName = company.getCommercialName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = company.getBusinessName();
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = companyName;
        }
        tvCompanyName.setText(displayName);

        // Rating y reseñas (placeholder - Company no tiene estos campos)
        // TODO: Implementar cuando se agreguen estos campos al modelo
        tvCompanyRating.setText("⭐ --");

        // Contador de tours se actualiza cuando se cargan los tours
    }

    private void loadToursFromFirebase() {
        if (companyId == null || companyId.isEmpty()) {
            Toast.makeText(this, "ID de empresa no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        firestoreManager.getToursByCompany(companyId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                java.util.List<Tour> tours = (java.util.List<Tour>) result;

                allTours.clear();
                allTours.addAll(tours);
                filteredTours.clear();
                filteredTours.addAll(allTours);
                toursAdapter.notifyDataSetChanged();

                // Actualizar contador de tours
                updateToursCountLabel();

                if (allTours.isEmpty()) {
                    Toast.makeText(ToursCatalogActivity.this,
                            "Esta empresa no tiene tours disponibles", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ToursCatalogActivity.this,
                        "Error cargando tours: " + e.getMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e("ToursCatalog", "Error cargando tours", e);
            }
        });
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
        // Establecer valores iniciales
        tvCompanyName.setText(companyName);
        tvCompanyRating.setText("⭐ --");
        tvToursCount.setText("-- tours");
    }

    private void setupRecyclerView() {
        rvTours.setLayoutManager(new LinearLayoutManager(this));
        toursAdapter = new ToursCatalogAdapter(filteredTours, this::onTourClick);
        rvTours.setAdapter(toursAdapter);
    }

    private void bindSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                applySearchAndCurrentFilter();
            }
        });
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_all) {
                    applyFilter("all");
                } else if (checkedId == R.id.chip_best_price) {
                    applyFilter("best_price");
                } else if (checkedId == R.id.chip_duration) {
                    applyFilter("duration");
                }
            }
        });
    }

    private void applySearchAndCurrentFilter() {
        int checkedId = chipGroupFilter.getCheckedChipId();
        if (checkedId == R.id.chip_best_price) applyFilter("best_price");
        else if (checkedId == R.id.chip_duration) applyFilter("duration");
        else applyFilter("all");
    }

    private void applyFilter(String filterType) {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        filteredTours.clear();

        // Aplicar búsqueda
        for (Tour tour : allTours) {
            if (query.isEmpty() ||
                    (tour.getName() != null && tour.getName().toLowerCase().contains(query)) ||
                    (tour.getDescription() != null && tour.getDescription().toLowerCase().contains(query))) {
                filteredTours.add(tour);
            }
        }

        // Aplicar ordenamiento
        if ("best_price".equals(filterType)) {
            java.util.Collections.sort(filteredTours, (t1, t2) -> {
                double price1 = t1.getPricePerPerson() != null ? t1.getPricePerPerson() : Double.MAX_VALUE;
                double price2 = t2.getPricePerPerson() != null ? t2.getPricePerPerson() : Double.MAX_VALUE;
                return Double.compare(price1, price2);
            });
        } else if ("duration".equals(filterType)) {
            java.util.Collections.sort(filteredTours, (t1, t2) -> {
                long duration1 = parseDurationToMinutes(t1.getDuration());
                long duration2 = parseDurationToMinutes(t2.getDuration());
                return Long.compare(duration1, duration2);
            });
        }

        toursAdapter.notifyDataSetChanged();
        updateToursCountLabel();
    }

    private void updateToursCountLabel() {
        int count = filteredTours.size();
        tvToursCount.setText(count + (count == 1 ? " tour" : " tours"));
    }

    private long parseDurationToMinutes(String label) {
        if (label == null || label.isEmpty()) return Long.MAX_VALUE;

        String text = label.trim().toLowerCase();

        // Patrones para horas
        java.util.regex.Matcher mHours = java.util.regex.Pattern.compile("(\\d+)\\s*h(ora|oras)?").matcher(text);
        if (mHours.find()) {
            long hours = Long.parseLong(mHours.group(1));
            return hours * 60L;
        }

        mHours = java.util.regex.Pattern.compile("(\\d+)\\s*hora(s)?").matcher(text);
        if (mHours.find()) {
            long hours = Long.parseLong(mHours.group(1));
            return hours * 60L;
        }

        // Patrón para días/noches (ej: "3d/2n")
        java.util.regex.Matcher mDaysNights = java.util.regex.Pattern.compile("(\\d+)d/\\d+n").matcher(text);
        if (mDaysNights.find()) {
            long days = Long.parseLong(mDaysNights.group(1));
            return days * 24L * 60L;
        }

        // Patrón para días
        java.util.regex.Matcher mDays = java.util.regex.Pattern.compile("(\\d+)\\s*d(i[aá]s?)?").matcher(text);
        if (mDays.find()) {
            long days = Long.parseLong(mDays.group(1));
            return days * 24L * 60L;
        }

        // Casos especiales
        if (text.contains("full day")) return 24L * 60L;
        if (text.contains("medio dia") || text.contains("medio día") || text.contains("half day")) return 4L * 60L;

        return Long.MAX_VALUE;
    }

    private void onTourClick(Tour tour) {
        Intent intent = new Intent(this, TourDetailActivity.class);
        intent.putExtra("tour_id", tour.getTourId());
        intent.putExtra("tour_name", tour.getName());
        intent.putExtra("company_id", tour.getCompanyId());
        intent.putExtra("company_name", tour.getCompanyName());

        if (tour.getPricePerPerson() != null) {
            intent.putExtra("price", tour.getPricePerPerson());
        }

        if (tour.getImageUrl() != null) {
            intent.putExtra("image_url", tour.getImageUrl());
        }

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

// Adaptador para el catálogo de tours
class ToursCatalogAdapter extends RecyclerView.Adapter<ToursCatalogAdapter.ViewHolder> {
    interface OnTourClick { void onClick(Tour tour); }
    private final OnTourClick onTourClick;
    private final java.util.List<Tour> tours;

    ToursCatalogAdapter(java.util.List<Tour> tours, OnTourClick listener) {
        this.tours = tours;
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
        Tour tour = tours.get(position);

        android.widget.ImageView tourImage = holder.itemView.findViewById(R.id.iv_tour_image);
        TextView tourName = holder.itemView.findViewById(R.id.tv_tour_name);
        TextView tourDescription = holder.itemView.findViewById(R.id.tv_tour_description);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView duration = holder.itemView.findViewById(R.id.tv_duration);
        TextView groupSize = holder.itemView.findViewById(R.id.tv_group_size);
        TextView languages = holder.itemView.findViewById(R.id.tv_languages);
        TextView price = holder.itemView.findViewById(R.id.tv_price);
        MaterialButton btnReserve = holder.itemView.findViewById(R.id.btn_reserve);

        // Nombre del tour
        tourName.setText(tour.getName() != null ? tour.getName() : "Tour sin nombre");

        // Descripción
        String description = tour.getDescription();
        if (description != null && !description.isEmpty()) {
            tourDescription.setText(description);
            tourDescription.setVisibility(android.view.View.VISIBLE);
        } else {
            tourDescription.setVisibility(android.view.View.GONE);
        }

        // Rating
        /*Float avgRating = tour.getAverageRating();
        if (avgRating != null && avgRating > 0) {
            rating.setText("⭐ " + String.format(java.util.Locale.US, "%.1f", avgRating));
        } else {
            rating.setText("⭐ --");
        }

         */

        // Duración
        String durationStr = tour.getDuration();
        duration.setText(durationStr != null && !durationStr.isEmpty() ? durationStr : "No especificado");

        // Tamaño de grupo
        Integer maxSize = tour.getMaxGroupSize();
        if (maxSize != null && maxSize > 0) {
            groupSize.setText("Máx " + maxSize + " personas");
        } else {
            groupSize.setText("Grupo flexible");
        }

        // Idiomas
        java.util.List<String> tourLanguages = tour.getLanguages();
        if (tourLanguages != null && !tourLanguages.isEmpty()) {
            languages.setText(String.join(", ", tourLanguages));
        } else {
            languages.setText("ES");
        }

        // Precio
        Double priceValue = tour.getPricePerPerson();
        if (priceValue != null && priceValue > 0) {
            price.setText("S/. " + String.format(java.util.Locale.US, "%.2f", priceValue));
        } else {
            price.setText("S/. --");
        }

        // Imagen del tour
        if (tourImage != null) {
            String url = tour.getImageUrl();
            if (url == null || url.isEmpty()) {
                url = "https://www.dicasdeviagem.com/wp-content/uploads/2020/03/lima-costa-mar-2048x1364.jpg";
            }

            Glide.with(tourImage.getContext())
                    .load(url)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(tourImage);
        }

        // Click listeners
        btnReserve.setOnClickListener(v -> onTourClick.onClick(tour));
        holder.itemView.setOnClickListener(v -> onTourClick.onClick(tour));
    }

    @Override
    public int getItemCount() { return tours.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View v) { super(v); }
    }
}