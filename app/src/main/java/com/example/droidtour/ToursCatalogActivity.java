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
    
    private String companyName;
    private int companyId;
    private java.util.List<DatabaseHelper.Tour> allTours;
    private java.util.List<DatabaseHelper.Tour> filteredTours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tours_catalog);

        getIntentData();
        setupToolbar();
        initializeViews();
        setupCompanyHeader();
        setupRecyclerView();
        seedTours();
        bindSearch();
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
        filteredTours = new java.util.ArrayList<>();
        toursAdapter = new ToursCatalogAdapter(filteredTours, this::onTourClick);
        rvTours.setAdapter(toursAdapter);
    }

    private void seedTours() {
        allTours = new java.util.ArrayList<>();
        allTours.add(new DatabaseHelper.Tour(
            "City Tour Lima Centro Histórico",
            "Descubre la historia y cultura de Lima visitando sus principales atractivos del centro histórico.",
            85.0,
            "4 horas",
            4.9,
            "ES, EN",
            "Max 12"
        ));
        allTours.get(allTours.size()-1).setImageUrl("https://www.dicasdeviagem.com/wp-content/uploads/2020/03/lima-costa-mar-2048x1364.jpg");
        allTours.add(new DatabaseHelper.Tour(
            "Machu Picchu Full Day",
            "Visita la maravilla del mundo con transporte y guía incluido desde Cusco.",
            180.0,
            "Full Day",
            4.8,
            "ES, EN",
            "Max 12"
        ));
        allTours.get(allTours.size()-1).setImageUrl("https://res.klook.com/image/upload/c_fill,w_627,h_470/q_80/w_80,x_15,y_15,g_south_west,l_Klook_water_br_trans_yhcmh3/activities/jdnneadpdsxcsnghocbu.jpg");
        allTours.add(new DatabaseHelper.Tour(
            "Islas Ballestas y Paracas",
            "Observa lobos marinos, pingüinos y aves en su hábitat natural.",
            65.0,
            "6 horas",
            4.7,
            "ES, EN",
            "Max 12"
        ));
        allTours.get(allTours.size()-1).setImageUrl("https://image.jimcdn.com/app/cms/image/transf/none/path/s336fd9bc7dca3ebc/image/ida0ff171f4a6d885/version/1391479285/image.jpg");
        allTours.add(new DatabaseHelper.Tour(
            "Cañón del Colca 2D/1N",
            "Aventura de dos días por uno de los cañones más profundos del mundo.",
            120.0,
            "2D/1N",
            4.6,
            "ES, EN",
            "Max 12"
        ));
        allTours.get(allTours.size()-1).setImageUrl("https://thriveandwander.com/wp-content/uploads/2023/12/barranco-lima-768x514.jpg");

        filteredTours.clear();
        filteredTours.addAll(allTours);
        toursAdapter.notifyDataSetChanged();
        updateToursCountLabel();
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
        for (DatabaseHelper.Tour tour : allTours) {
            if (query.isEmpty() || tour.getName().toLowerCase().contains(query) || tour.getDescription().toLowerCase().contains(query)) {
                filteredTours.add(tour);
            }
        }

        if ("best_price".equals(filterType)) {
            java.util.Collections.sort(filteredTours, (t1, t2) -> Double.compare(t1.getPayment(), t2.getPayment()));
        } else if ("duration".equals(filterType)) {
            java.util.Collections.sort(filteredTours, (t1, t2) -> Long.compare(parseDurationToMinutes(t1.getDurationLabel()), parseDurationToMinutes(t2.getDurationLabel())));
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

    private void onTourClick(int position) {
        Intent intent = new Intent(this, TourDetailActivity.class);
        intent.putExtra("tour_id", position);
        intent.putExtra("company_name", companyName);
        DatabaseHelper.Tour tour = filteredTours.get(position);
        intent.putExtra("tour_name", tour.getName());
        intent.putExtra("price", tour.getPayment());
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
    interface OnTourClick { void onClick(int position); }
    private final OnTourClick onTourClick;
    private final java.util.List<DatabaseHelper.Tour> tours;
    
    ToursCatalogAdapter(java.util.List<DatabaseHelper.Tour> tours, OnTourClick listener) { 
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
        DatabaseHelper.Tour tour = tours.get(position);
        
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
        rating.setText("⭐ " + tour.getRating());
        duration.setText(tour.getDurationLabel());
        groupSize.setText(tour.getGroupSizeLabel());
        languages.setText(tour.getLanguages());
        price.setText("S/. " + String.format(java.util.Locale.US, "%.2f", tour.getPayment()));

        // Cargar imagen con Glide si hay URL
        if (tourImage != null) {
            String url = tour.getImageUrl();
            Glide.with(tourImage.getContext())
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(tourImage);
        }

        btnReserve.setOnClickListener(v -> onTourClick.onClick(position));
        holder.itemView.setOnClickListener(v -> onTourClick.onClick(position));
    }

    @Override
    public int getItemCount() { return tours.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

