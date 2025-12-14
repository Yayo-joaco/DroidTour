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
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
public class AllReviewsActivity extends AppCompatActivity {
    
    private RecyclerView rvReviews;
    private AllReviewsAdapter reviewsAdapter;
    private ChipGroup chipGroupFilter;
    private TextView tvTourName, tvTotalReviews;
    
    private String tourName;
    private List<Review> allReviews;
    private List<Review> filteredReviews;
    
    // Clase Review simple para reemplazar DatabaseHelper.Review
    static class Review {
        private String userName;
        private String userInitial;
        private double rating;
        private String reviewText;
        private String reviewDate;
        private String tourName;

        public Review(String userName, String userInitial, double rating, String reviewText, String reviewDate, String tourName) {
            this.userName = userName;
            this.userInitial = userInitial;
            this.rating = rating;
            this.reviewText = reviewText;
            this.reviewDate = reviewDate;
            this.tourName = tourName;
        }

        public String getUserName() { return userName; }
        public String getUserInitial() { return userInitial; }
        public double getRating() { return rating; }
        public String getReviewText() { return reviewText; }
        public String getReviewDate() { return reviewDate; }
        public String getTourName() { return tourName; }
    }

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
        
        setContentView(R.layout.activity_all_reviews);

        getIntentData();
        setupToolbar();
        initializeViews();
        seedReviews();
        setupRecyclerView();
        setupFilters();
    }

    private void getIntentData() {
        tourName = getIntent().getStringExtra("tour_name");
        if (tourName == null) tourName = "Tour Increíble";
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reseñas del Tour");
        }
    }

    private void initializeViews() {
        rvReviews = findViewById(R.id.rv_reviews);
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        tvTourName = findViewById(R.id.tv_tour_name);
        tvTotalReviews = findViewById(R.id.tv_total_reviews);
    }

    private void seedReviews() {
        allReviews = new ArrayList<>();
        
        // Reseñas para diferentes tours
        allReviews.add(new Review("Ana García", "A", 5.0, 
            "Excelente tour, el guía muy conocedor y amable. Los lugares visitados fueron increíbles y la comida deliciosa.", 
            "Hace 2 semanas", tourName));
        allReviews.add(new Review("Carlos Mendoza", "C", 5.0, 
            "Una experiencia inolvidable. La organización fue perfecta y aprendimos mucho sobre la historia de Lima.", 
            "Hace 1 mes", tourName));
        allReviews.add(new Review("María López", "M", 4.0, 
            "Muy recomendado. El tour cumplió todas nuestras expectativas y el precio es muy justo.", 
            "Hace 3 semanas", tourName));
        allReviews.add(new Review("Pedro Ramírez", "P", 4.5, 
            "Increíble experiencia, el guía fue muy profesional y conocía todos los detalles históricos.", 
            "Hace 1 semana", tourName));
        allReviews.add(new Review("Laura Sánchez", "L", 3.5, 
            "Buen tour en general, aunque el tiempo en algunos lugares fue un poco corto.", 
            "Hace 2 meses", tourName));
        allReviews.add(new Review("Roberto Torres", "R", 5.0, 
            "Perfecto desde el inicio hasta el final. Definitivamente lo recomiendo a todos.", 
            "Hace 5 días", tourName));
        allReviews.add(new Review("Carmen Vega", "C", 4.0, 
            "Muy buena experiencia, aprendimos mucho sobre la cultura local.", 
            "Hace 1 mes", tourName));
        allReviews.add(new Review("Diego Flores", "D", 2.5, 
            "El tour estuvo bien pero esperaba más información histórica detallada.", 
            "Hace 3 meses", tourName));
        allReviews.add(new Review("Sofia Herrera", "S", 4.5, 
            "Excelente servicio y muy buena atención. Los lugares fueron espectaculares.", 
            "Hace 2 semanas", tourName));
        allReviews.add(new Review("Miguel Castro", "M", 5.0, 
            "Una de las mejores experiencias de mi vida. El guía hizo que todo fuera muy interesante.", 
            "Hace 1 semana", tourName));

        filteredReviews = new ArrayList<>(allReviews);
        updateTotalReviewsLabel();
    }

    private void setupRecyclerView() {
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewsAdapter = new AllReviewsAdapter(filteredReviews);
        rvReviews.setAdapter(reviewsAdapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                
                if (checkedId == R.id.chip_all) {
                    applyFilter("all");
                } else if (checkedId == R.id.chip_best_rating) {
                    applyFilter("best_rating");
                } else if (checkedId == R.id.chip_worst_rating) {
                    applyFilter("worst_rating");
                }
            }
        });
    }

    private void applyFilter(String filterType) {
        filteredReviews.clear();
        filteredReviews.addAll(allReviews);
        
        if ("best_rating".equals(filterType)) {
            Collections.sort(filteredReviews, (r1, r2) -> Double.compare(r2.getRating(), r1.getRating()));
        } else if ("worst_rating".equals(filterType)) {
            Collections.sort(filteredReviews, (r1, r2) -> Double.compare(r1.getRating(), r2.getRating()));
        }
        
        reviewsAdapter.notifyDataSetChanged();
        updateTotalReviewsLabel();
    }

    private void updateTotalReviewsLabel() {
        tvTourName.setText(tourName);
        tvTotalReviews.setText(filteredReviews.size() + " reseñas encontradas");
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

// Adaptador para todas las reseñas
class AllReviewsAdapter extends RecyclerView.Adapter<AllReviewsAdapter.ViewHolder> {
    private final List<AllReviewsActivity.Review> reviews;
    
    AllReviewsAdapter(List<AllReviewsActivity.Review> reviews) { 
        this.reviews = reviews; 
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AllReviewsActivity.Review review = reviews.get(position);
        
        TextView userInitial = holder.itemView.findViewById(R.id.tv_user_initial);
        TextView userName = holder.itemView.findViewById(R.id.tv_user_name);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView reviewText = holder.itemView.findViewById(R.id.tv_review_text);
        TextView reviewDate = holder.itemView.findViewById(R.id.tv_review_date);

        userInitial.setText(review.getUserInitial());
        userName.setText(review.getUserName());
        
        // Crear estrellas basadas en la calificación (solo estrella normal)
        StringBuilder stars = new StringBuilder();
        int roundedRating = (int) Math.round(review.getRating());
        
        for (int i = 0; i < roundedRating; i++) {
            stars.append("⭐");
        }
        
        rating.setText(stars.toString());
        reviewText.setText(review.getReviewText());
        reviewDate.setText(review.getReviewDate());
    }

    @Override
    public int getItemCount() { return reviews.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}
