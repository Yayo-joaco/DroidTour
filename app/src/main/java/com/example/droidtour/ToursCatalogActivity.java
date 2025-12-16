package com.example.droidtour;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import com.example.droidtour.models.Tour;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToursCatalogActivity extends AppCompatActivity {

    private RecyclerView rvTours;
    private ToursCatalogAdapter toursAdapter;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private TextView tvCompanyName, tvCompanyRating, tvToursCount;

    // Variables para el collapsing toolbar
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;

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
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();

        getIntentData();
        setupToolbar();
        setupCollapsingToolbar();
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

        // Usar el nuevo método que obtiene empresa + estadísticas
        firestoreManager.getCompanyCatalogHeaderData(companyId,
                new com.example.droidtour.firebase.FirestoreManager.CompanyCatalogCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        Company company = (Company) result.get("company");
                        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
                        int toursCount = (int) result.get("toursCount");

                        displayCompanyInfo(company, stats, toursCount);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        android.util.Log.e("ToursCatalog", "Error cargando datos del catálogo: " + e.getMessage());
                        setupCompanyHeader(); // Usar valores por defecto
                    }
                });
    }

    /**
     * Mostrar información completa de la empresa con estadísticas
     */
    private void displayCompanyInfo(Company company, Map<String, Object> stats, int toursCount) {
        // 1. Nombre: preferir commercialName, luego businessName
        String displayName = company.getCommercialName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = company.getBusinessName();
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = companyName;
        }
        tvCompanyName.setText(displayName);

        // 2. Logo
        ImageView companyLogo = findViewById(R.id.iv_company_logo);
        TextView companyInitial = findViewById(R.id.tv_company_initial);

        String logoUrl = company.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty()) {
            // Cargar logo con Glide
            Glide.with(this)
                    .load(logoUrl)
                    .placeholder(R.drawable.ic_company)
                    .error(R.drawable.ic_company)
                    .into(companyLogo);
            companyInitial.setVisibility(View.GONE);
            companyLogo.setVisibility(View.VISIBLE);
        } else {
            // Mostrar iniciales si no hay logo
            companyLogo.setVisibility(View.GONE);
            companyInitial.setVisibility(View.VISIBLE);

            if (displayName != null && !displayName.isEmpty()) {
                // Tomar las primeras dos letras del nombre
                String initials = displayName.substring(0, Math.min(2, displayName.length())).toUpperCase();
                companyInitial.setText(initials);
            }
        }

        // 3. Ubicación
        TextView companyLocation = findViewById(R.id.tv_company_location);
        String location = company.getAddress();
        if (location != null && !location.isEmpty()) {
            companyLocation.setText(location);
        } else {
            companyLocation.setText("Ubicación no disponible");
        }

        // 4. Estadísticas
        TextView tvToursCount = findViewById(R.id.tv_tours_count);
        TextView tvPriceFrom = findViewById(R.id.tv_price_from);
        TextView tvExperienceYears = findViewById(R.id.tv_experience_years);

        // Número de tours
        tvToursCount.setText(String.valueOf(toursCount));

        // Precio desde
        double minPrice = (double) stats.get("minPrice");
        if (minPrice > 0) {
            tvPriceFrom.setText(String.format("S/ %.0f", minPrice));
        } else {
            tvPriceFrom.setText("S/ --");
        }

        // Años de experiencia
        int years = (int) stats.get("experienceYears");
        tvExperienceYears.setText(String.valueOf(years) + "+");

        // 5. Rating (placeholder - agregar campo a Company si es necesario)
        TextView tvCompanyRating = findViewById(R.id.tv_company_rating);
        TextView tvReviewsCount = findViewById(R.id.tv_reviews_count);
        tvCompanyRating.setText("4.8"); // Temporal
        tvReviewsCount.setText("(245 reseñas)"); // Temporal

        // Actualizar título del collapsing toolbar
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(displayName); // O "Tours Disponibles" si prefieres
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
            getSupportActionBar().setTitle(""); // Dejar vacío, el CollapsingToolbar maneja el título
        }
    }

    /**
     * ✅ Configurar el efecto de título colapsable
     * El título permanece invisible hasta que el AppBar está completamente colapsado
     */
    private void setupCollapsingToolbar() {
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        appBarLayout = findViewById(R.id.app_bar_layout);

        // Inicialmente, el título debe estar vacío o con un valor mínimo
        collapsingToolbar.setTitle(""); // Vacío inicialmente

        // Listener para controlar la visibilidad del título según el scroll
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBar, int verticalOffset) {
                // Calcular el porcentaje de colapso (0 = expandido, 1 = colapsado)
                float percentage = Math.abs(verticalOffset) / (float) appBar.getTotalScrollRange();

                // Definir un umbral (ej: 95% colapsado)
                float collapseThreshold = 0.95f;

                if (percentage >= collapseThreshold) {
                    // Cuando está completamente colapsado, mostrar el título
                    collapsingToolbar.setTitle("Tours Disponibles");
                    // Configurar el color del título colapsado
                    collapsingToolbar.setCollapsedTitleTextColor(
                            ContextCompat.getColor(ToursCatalogActivity.this, R.color.white)
                    );
                } else {
                    // Cuando no está colapsado, ocultar el título
                    collapsingToolbar.setTitle("");
                    // Opcional: asegurar que sea transparente
                    collapsingToolbar.setCollapsedTitleTextColor(
                            android.graphics.Color.TRANSPARENT
                    );
                }

                // También puedes ajustar el título expandido si es necesario
                collapsingToolbar.setExpandedTitleColor(
                        android.graphics.Color.TRANSPARENT
                );
            }
        });

        // Configurar también el color del título expandido como transparente
        collapsingToolbar.setExpandedTitleColor(android.graphics.Color.TRANSPARENT);
    }

    private void initializeViews() {
        rvTours = findViewById(R.id.rv_tours);
        etSearch = findViewById(R.id.et_search);
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvCompanyRating = findViewById(R.id.tv_company_rating);
        tvToursCount = findViewById(R.id.tv_tours_count);
        
        // Botón Ver Perfil
        MaterialButton btnProfile = findViewById(R.id.btn_profile);
        if (btnProfile != null && companyId != null && !companyId.isEmpty()) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.example.droidtour.client.CompanyProfileActivity.class);
                intent.putExtra("company_id", companyId);
                startActivity(intent);
            });
        }
    }

    private void setupCompanyHeader() {
        // Establecer valores iniciales
        tvCompanyName.setText(companyName);
        tvCompanyRating.setText("--");
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
        Double avgRating = tour.getAverageRating();
        if (avgRating != null && avgRating > 0) {
            rating.setText(String.format(java.util.Locale.US, "%.1f", avgRating));
        } else {
            rating.setText("--");
        }

        // Duración
        String durationStr = tour.getDuration();
        duration.setText(durationStr != null && !durationStr.isEmpty() ? durationStr : "No especificado");

        // Tamaño de grupo
        Integer maxSize = tour.getMaxGroupSize();
        if (maxSize != null && maxSize > 0) {
            groupSize.setText(String.valueOf(maxSize));
        } else {
            groupSize.setText("--");
        }

        // Idiomas
        java.util.List<String> tourLanguages = tour.getLanguages();
        if (tourLanguages != null && !tourLanguages.isEmpty()) {
            // Limitar a 2 idiomas para no saturar la vista
            if (tourLanguages.size() > 2) {
                languages.setText(tourLanguages.get(0) + ", " + tourLanguages.get(1) + "...");
            } else {
                languages.setText(String.join(", ", tourLanguages));
            }
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

        // ==================== CARGAR SERVICIOS ====================
        ChipGroup chipGroupServices = holder.itemView.findViewById(R.id.chip_group_services);
        chipGroupServices.removeAllViews(); // Limpiar chips estáticos

        // Obtener servicios del tour o de la empresa
        if (tour.getIncludedServices() != null && !tour.getIncludedServices().isEmpty()) {
            // Si el tour tiene servicios incluidos específicos, mostrarlos
            displayServicesFromList(tour.getIncludedServices(), chipGroupServices);
        } else if (tour.getIncludedServiceIds() != null && !tour.getIncludedServiceIds().isEmpty()) {
            // Si tiene IDs de servicios, cargarlos de Firestore
            loadServicesByIds(tour.getIncludedServiceIds(), chipGroupServices, tour.getCompanyId());
        } else {
            // Si no tiene servicios específicos, cargar todos los servicios de la empresa
            loadServicesByCompany(tour.getCompanyId(), chipGroupServices);
        }

        // Click listeners
        btnReserve.setOnClickListener(v -> onTourClick.onClick(tour));
        holder.itemView.setOnClickListener(v -> onTourClick.onClick(tour));
    }

    /**
     * Mostrar servicios desde una lista de strings
     */
    private void displayServicesFromList(List<String> services, ChipGroup chipGroup) {
        if (chipGroup == null) return;

        chipGroup.removeAllViews();

        if (services == null || services.isEmpty()) {
            chipGroup.setVisibility(View.GONE);
            return;
        }
        chipGroup.setVisibility(View.VISIBLE);

        // Espaciado tipo "Companies"
        chipGroup.setChipSpacingHorizontal(dp(chipGroup, 8));
        chipGroup.setChipSpacingVertical(dp(chipGroup, 8));

        int maxToShow = Math.min(3, services.size()); // opcional (como companies, no satures)
        for (int i = 0; i < maxToShow; i++) {
            String name = services.get(i);
            if (name == null || name.trim().isEmpty()) continue;

            Chip chip = new Chip(chipGroup.getContext());
            chip.setText(name);

            //  No interactivo
            chip.setClickable(false);
            chip.setCheckable(false);

            //  Que no lo “aplane” Material por touch target
            chip.setEnsureMinTouchTargetSize(false);

            // Altura + padding para que NO se vea estrecho
            chip.setMinHeight(dp(chipGroup, 36));
            chip.setChipMinHeight(dp(chipGroup, 36));
            chip.setChipStartPadding(dp(chipGroup, 14));
            chip.setChipEndPadding(dp(chipGroup, 14));
            chip.setTextStartPadding(0f);
            chip.setTextEndPadding(0f);

            //  Forma pill
            chip.setChipCornerRadius(dp(chipGroup, 18));

            // Colores neutros (ajústalos a tu paleta si quieres)
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E8F1FF")));
            chip.setTextColor(Color.parseColor("#0B3B7A"));

            //  Opcional: borde suave como algunos chips de Companies
            chip.setChipStrokeWidth(dp(chipGroup, 1));
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#C7DAFF")));

            chipGroup.addView(chip);
        }

        // opcional: +N
        int remaining = services.size() - maxToShow;
        if (remaining > 0) {
            Chip more = new Chip(chipGroup.getContext());
            more.setText("+" + remaining);
            more.setClickable(false);
            more.setCheckable(false);
            more.setEnsureMinTouchTargetSize(false);
            more.setMinHeight(dp(chipGroup, 36));
            more.setChipMinHeight(dp(chipGroup, 36));
            more.setChipStartPadding(dp(chipGroup, 14));
            more.setChipEndPadding(dp(chipGroup, 14));
            more.setChipCornerRadius(dp(chipGroup, 18));
            more.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
            more.setTextColor(Color.parseColor("#334155"));
            more.setChipStrokeWidth(dp(chipGroup, 1));
            more.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
            chipGroup.addView(more);
        }
    }

    private int dp(View v, int dp) {
        float density = v.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }


    /**
     * Cargar servicios por IDs desde Firestore
     */
    private void loadServicesByIds(List<String> serviceIds, ChipGroup chipGroup, String companyId) {
        FirestoreManager firestoreManager = FirestoreManager.getInstance();

        firestoreManager.getNeoServicesByIds(serviceIds, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<com.example.droidtour.models.Service> services = (List<com.example.droidtour.models.Service>) result;

                if (services != null && !services.isEmpty()) {
                    List<String> serviceNames = new ArrayList<>();
                    for (com.example.droidtour.models.Service service : services) {
                        if (service.getName() != null) {
                            serviceNames.add(service.getName());
                        }
                    }
                    displayServicesFromList(serviceNames, chipGroup);
                } else {
                    // Si no se cargaron servicios, intentar cargar todos los de la empresa
                    loadServicesByCompany(companyId, chipGroup);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("ToursCatalog", "Error loading services by IDs", e);
                loadServicesByCompany(companyId, chipGroup);
            }
        });
    }

    /**
     * Cargar todos los servicios de una empresa
     */
    private void loadServicesByCompany(String companyId, ChipGroup chipGroup) {
        if (companyId == null || companyId.isEmpty()) return;

        FirestoreManager firestoreManager = FirestoreManager.getInstance();

        firestoreManager.getServicesByCompany(companyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<com.example.droidtour.models.Service> services = (List<com.example.droidtour.models.Service>) result;

                if (services != null && !services.isEmpty()) {
                    List<String> serviceNames = new ArrayList<>();
                    for (com.example.droidtour.models.Service service : services) {
                        if (service.getName() != null) {
                            serviceNames.add(service.getName());
                        }
                    }
                    displayServicesFromList(serviceNames, chipGroup);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("ToursCatalog", "Error loading company services", e);
            }
        });
    }

    @Override
    public int getItemCount() { return tours.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View v) { super(v); }
    }
}