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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.example.droidtour.database.DatabaseHelper;

public class ToursCatalogActivity extends AppCompatActivity {
    
    private RecyclerView rvTours;
    private ToursCatalogAdapter toursAdapter;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private TextView tvCompanyName, tvCompanyRating, tvToursCount;
    
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private String companyId, companyName;
    private java.util.List<com.example.droidtour.models.Tour> allTours = new java.util.ArrayList<>();
    private java.util.List<com.example.droidtour.models.Tour> filteredTours = new java.util.ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tours_catalog);

        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        
        getIntentData();
        setupToolbar();
        initializeViews();
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
        if (companyId == null) companyId = "COMP001";
    }
    
    private void loadCompanyFromFirebase() {
        firestoreManager.getCompany(companyId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.Company company = (com.example.droidtour.models.Company) result;
                tvCompanyName.setText(company.getName());
                tvCompanyRating.setText("⭐ " + company.getAverageRating() + " • " + company.getTotalReviews() + " reseñas");
                tvToursCount.setText(company.getTotalTours() + " tours");
            }
            
            @Override
            public void onFailure(Exception e) {
                setupCompanyHeader();
            }
        });
    }
    
    private void loadToursFromFirebase() {
        firestoreManager.getToursByCompany(companyId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                allTours.clear();
                allTours.addAll((java.util.List<com.example.droidtour.models.Tour>) result);
                filteredTours.clear();
                filteredTours.addAll(allTours);
                toursAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ToursCatalogActivity.this, "Error cargando tours", Toast.LENGTH_SHORT).show();
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
        // Fallback con datos de ejemplo si falla Firebase
        tvCompanyName.setText(companyName);
        tvCompanyRating.setText("⭐ 4.8 • 245 reseñas");
        tvToursCount.setText("12 tours");
    }

    private void setupRecyclerView() {
        rvTours.setLayoutManager(new LinearLayoutManager(this));
        toursAdapter = new ToursCatalogAdapter(filteredTours, this::onTourClick);
        rvTours.setAdapter(toursAdapter);
    }
    
    // Método obsoleto - ahora se usa loadToursFromFirebase()

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
        for (com.example.droidtour.models.Tour tour : allTours) {
            if (query.isEmpty() || tour.getName().toLowerCase().contains(query) || 
                (tour.getDescription() != null && tour.getDescription().toLowerCase().contains(query))) {
                filteredTours.add(tour);
            }
        }

        if ("best_price".equals(filterType)) {
            java.util.Collections.sort(filteredTours, (t1, t2) -> Double.compare(t1.getPricePerPerson(), t2.getPricePerPerson()));
        } else if ("duration".equals(filterType)) {
            java.util.Collections.sort(filteredTours, (t1, t2) -> Long.compare(parseDurationToMinutes(t1.getDuration()), parseDurationToMinutes(t2.getDuration())));
        }

        toursAdapter.notifyDataSetChanged();
        updateToursCountLabel();
    }

    private void updateToursCountLabel() {
        tvToursCount.setText(filteredTours.size() + " tours");
    }

    private long parseDurationToMinutes(String label) {
        if (label == null) return Long.MAX_VALUE;
        String text = label.trim().toLowerCase();
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
        java.util.regex.Matcher mDaysNights = java.util.regex.Pattern.compile("(\\d+)d/\\d+n").matcher(text);
        if (mDaysNights.find()) {
            long days = Long.parseLong(mDaysNights.group(1));
            return days * 24L * 60L;
        }
        java.util.regex.Matcher mDays = java.util.regex.Pattern.compile("(\\d+)\\s*d(i[aá]s?)?").matcher(text);
        if (mDays.find()) {
            long days = Long.parseLong(mDays.group(1));
            return days * 24L * 60L;
        }
        if (text.contains("full day")) return 24L * 60L;
        if (text.contains("medio dia") || text.contains("medio día") || text.contains("half day")) return 4L * 60L;
        return Long.MAX_VALUE;
    }

    private void onTourClick(com.example.droidtour.models.Tour tour) {
        Intent intent = new Intent(this, TourDetailActivity.class);
        intent.putExtra("tour_id", tour.getTourId());
        intent.putExtra("tour_name", tour.getName());
        intent.putExtra("company_id", tour.getCompanyId());
        intent.putExtra("company_name", tour.getCompanyName());
        intent.putExtra("price", tour.getPricePerPerson());
        intent.putExtra("image_url", tour.getImageUrl());
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
}

// Adaptador para el catálogo de tours
class ToursCatalogAdapter extends RecyclerView.Adapter<ToursCatalogAdapter.ViewHolder> {
    interface OnTourClick { void onClick(com.example.droidtour.models.Tour tour); }
    private final OnTourClick onTourClick;
    private final java.util.List<com.example.droidtour.models.Tour> tours;
    
    ToursCatalogAdapter(java.util.List<com.example.droidtour.models.Tour> tours, OnTourClick listener) { 
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
        com.example.droidtour.models.Tour tour = tours.get(position);
        
        android.widget.ImageView tourImage = holder.itemView.findViewById(R.id.iv_tour_image);
        TextView tourName = holder.itemView.findViewById(R.id.tv_tour_name);
        TextView tourDescription = holder.itemView.findViewById(R.id.tv_tour_description);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView duration = holder.itemView.findViewById(R.id.tv_duration);
        TextView groupSize = holder.itemView.findViewById(R.id.tv_group_size);
        TextView languages = holder.itemView.findViewById(R.id.tv_languages);
        TextView price = holder.itemView.findViewById(R.id.tv_price);
        MaterialButton btnReserve = holder.itemView.findViewById(R.id.btn_reserve);

        tourName.setText(tour.getName());
        tourDescription.setText(tour.getDescription());
        rating.setText("⭐ " + tour.getAverageRating());
        duration.setText(tour.getDuration());
        groupSize.setText("Máx " + tour.getMaxGroupSize() + " personas");
        languages.setText(tour.getLanguages() != null ? String.join(", ", tour.getLanguages()) : "ES, EN");
        price.setText("S/. " + String.format(java.util.Locale.US, "%.2f", tour.getPricePerPerson()));

        if (tourImage != null) {
            String url = tour.getImageUrl() != null ? tour.getImageUrl() : "https://www.dicasdeviagem.com/wp-content/uploads/2020/03/lima-costa-mar-2048x1364.jpg";
            Glide.with(tourImage.getContext())
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(tourImage);
        }

        btnReserve.setOnClickListener(v -> onTourClick.onClick(tour));
        holder.itemView.setOnClickListener(v -> onTourClick.onClick(tour));
    }

    @Override
    public int getItemCount() { return tours.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

