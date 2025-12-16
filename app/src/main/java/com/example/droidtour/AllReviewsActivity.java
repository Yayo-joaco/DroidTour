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
import com.example.droidtour.firebase.FirestoreManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
public class AllReviewsActivity extends AppCompatActivity {
    
    private RecyclerView rvReviews;
    private AllReviewsAdapter reviewsAdapter;
    private ChipGroup chipGroupFilter;
    private TextView tvTourName, tvTotalReviews;
    
    private String tourName, tourId;
    private List<com.example.droidtour.models.Review> allReviews;
    private List<com.example.droidtour.models.Review> filteredReviews;
    private FirestoreManager firestoreManager;

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
        loadReviewsFromFirebase();
        setupRecyclerView();
        setupFilters();
    }

    private void getIntentData() {
        tourName = getIntent().getStringExtra("tour_name");
        tourId = getIntent().getStringExtra("tour_id");
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
        
        firestoreManager = FirestoreManager.getInstance();
    }

    private void loadReviewsFromFirebase() {
        if (tourId == null || tourId.trim().isEmpty()) {
            Toast.makeText(this, "Error: No se encontró el ID del tour", Toast.LENGTH_SHORT).show();
            allReviews = new ArrayList<>();
            filteredReviews = new ArrayList<>();
            updateTotalReviewsLabel();
            if (reviewsAdapter != null) {
                reviewsAdapter.notifyDataSetChanged();
            }
            return;
        }
        
        firestoreManager.getReviewsByTour(tourId, new FirestoreManager.FirestoreCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSuccess(Object result) {
                allReviews = (List<com.example.droidtour.models.Review>) result;
                if (allReviews == null) {
                    allReviews = new ArrayList<>();
                }
                filteredReviews = new ArrayList<>(allReviews);
                updateTotalReviewsLabel();
                
                if (reviewsAdapter != null) {
                    reviewsAdapter.notifyDataSetChanged();
                } else {
                    setupRecyclerView();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("AllReviews", "Error loading reviews", e);
                Toast.makeText(AllReviewsActivity.this, "Error cargando reseñas", Toast.LENGTH_SHORT).show();
                allReviews = new ArrayList<>();
                filteredReviews = new ArrayList<>();
                updateTotalReviewsLabel();
                if (reviewsAdapter != null) {
                    reviewsAdapter.notifyDataSetChanged();
                }
            }
        });
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
            Collections.sort(filteredReviews, (r1, r2) -> {
                Float rating1 = r1.getRating() != null ? r1.getRating() : 0f;
                Float rating2 = r2.getRating() != null ? r2.getRating() : 0f;
                return rating2.compareTo(rating1);
            });
        } else if ("worst_rating".equals(filterType)) {
            Collections.sort(filteredReviews, (r1, r2) -> {
                Float rating1 = r1.getRating() != null ? r1.getRating() : 0f;
                Float rating2 = r2.getRating() != null ? r2.getRating() : 0f;
                return rating1.compareTo(rating2);
            });
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
    private final List<com.example.droidtour.models.Review> reviews;
    
    AllReviewsAdapter(List<com.example.droidtour.models.Review> reviews) { 
        this.reviews = reviews != null ? reviews : new ArrayList<>(); 
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        com.example.droidtour.models.Review review = reviews.get(position);
        
        TextView userInitial = holder.itemView.findViewById(R.id.tv_user_initial);
        TextView userName = holder.itemView.findViewById(R.id.tv_user_name);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView reviewText = holder.itemView.findViewById(R.id.tv_review_text);
        TextView reviewDate = holder.itemView.findViewById(R.id.tv_review_date);

        // Mostrar inicial del usuario
        String initial = review.getUserInitial();
        if (initial == null || initial.trim().isEmpty()) {
            String name = review.getUserName();
            if (name != null && !name.trim().isEmpty()) {
                initial = name.trim().substring(0, 1).toUpperCase();
            } else {
                initial = "U";
            }
        }
        userInitial.setText(initial);
        
        // Mostrar nombre del usuario
        userName.setText(review.getUserName() != null ? review.getUserName() : "Usuario");
        
        // Crear estrellas basadas en la calificación del tour
        StringBuilder stars = new StringBuilder();
        Float tourRating = review.getRating();
        if (tourRating == null) tourRating = 0f;
        int roundedRating = Math.round(tourRating);
        
        for (int i = 0; i < roundedRating; i++) {
            stars.append("⭐");
        }
        
        rating.setText(stars.toString());
        
        // Mostrar texto del comentario del tour
        String text = review.getReviewText();
        if (text == null || text.trim().isEmpty()) {
            text = "Sin comentario";
        }
        reviewText.setText(text);
        
        // Mostrar fecha de creación
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        if (review.getCreatedAt() != null) {
            reviewDate.setText(dateFormat.format(review.getCreatedAt()));
        } else {
            reviewDate.setText("Fecha no disponible");
        }
    }

    @Override
    public int getItemCount() { 
        return reviews != null ? reviews.size() : 0; 
    }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}
