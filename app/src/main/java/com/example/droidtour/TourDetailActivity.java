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
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class TourDetailActivity extends AppCompatActivity {
    
    private TextView tvTourName, tvCompanyName, tvTourDescription, tvRating;
    private TextView tvDuration, tvGroupSize, tvLanguages, tvPriceBottom;
    private RecyclerView rvItinerary, rvReviews;
    private MaterialButton btnBookNow, btnSeeAllReviews, btnContact;
    private MaterialButton btnViewMap, btnDirections;

    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private com.example.droidtour.firebase.FirebaseAuthManager authManager;
    private com.example.droidtour.models.Tour currentTour;
    private String tourId, tourName, companyName, companyId, imageUrl;
    private String currentUserId;
    private double price;

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
        
        setContentView(R.layout.activity_tour_detail);

        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        authManager = com.example.droidtour.firebase.FirebaseAuthManager.getInstance(this);
        currentUserId = authManager.getCurrentUserId();
        
        getIntentData();
        setupToolbar();
        initializeViews();
        loadTourFromFirebase();
        setupRecyclerViews();
        setupClickListeners();
        checkExistingReservation();
    }

    private void getIntentData() {
        tourId = getIntent().getStringExtra("tour_id");
        tourName = getIntent().getStringExtra("tour_name");
        companyName = getIntent().getStringExtra("company_name");
        companyId = getIntent().getStringExtra("company_id");
        price = getIntent().getDoubleExtra("price", 0.0);
        imageUrl = getIntent().getStringExtra("image_url");
        
        if (tourName == null) tourName = "Tour Increíble";
        if (companyName == null) companyName = "Empresa de Tours";
    }
    
    private void loadTourFromFirebase() {
        if (tourId != null) {
            firestoreManager.getTour(tourId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    currentTour = (com.example.droidtour.models.Tour) result;
                    displayTourData();
                }
                
                @Override
                public void onFailure(Exception e) {
                    setupTourData(); // Fallback a datos locales
                }
            });
        } else {
            setupTourData();
        }
    }
    
    private void displayTourData() {
        if (currentTour == null) return;
        
        tvTourName.setText(currentTour.getTourName());
        tvCompanyName.setText("por " + currentTour.getCompanyName());
        tvPriceBottom.setText("S/. " + String.format("%.2f", currentTour.getPricePerPerson()));
        tvTourDescription.setText(currentTour.getDescription());
        tvRating.setText("⭐ " + currentTour.getAverageRating() + " (" + currentTour.getTotalReviews() + ")");
        tvDuration.setText(currentTour.getDuration());
        tvGroupSize.setText(currentTour.getMaxGroupSize() + " personas");
        tvLanguages.setText(currentTour.getLanguages() != null ? String.join(", ", currentTour.getLanguages()) : "ES, EN");
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvTourDescription = findViewById(R.id.tv_tour_description);
        tvRating = findViewById(R.id.tv_rating);
        tvDuration = findViewById(R.id.tv_duration);
        tvGroupSize = findViewById(R.id.tv_group_size);
        tvLanguages = findViewById(R.id.tv_languages);
        // El layout ahora define tv_price_bottom en la barra inferior
        tvPriceBottom = findViewById(R.id.tv_price_bottom);
        rvItinerary = findViewById(R.id.rv_itinerary);
        rvReviews = findViewById(R.id.rv_reviews);
        // Mapear botones a los nuevos IDs del layout
        btnBookNow = findViewById(R.id.btn_book_now);
        btnSeeAllReviews = findViewById(R.id.btn_see_all_reviews);
        btnContact = findViewById(R.id.btn_contact);
        btnViewMap = findViewById(R.id.btn_view_map);
        btnDirections = findViewById(R.id.btn_directions);

        android.widget.ImageView headerImage = findViewById(R.id.iv_header_image);
        if (headerImage != null) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(headerImage);
        }
    }

    private void setupTourData() {
        // Evitar NullPointer si tourId es null
        if (tvTourName != null) tvTourName.setText(tourName);
        if (tvCompanyName != null) tvCompanyName.setText("por " + companyName);
        if (tvPriceBottom != null) tvPriceBottom.setText("S/. " + String.format("%.2f", price));

        if (tourId == null) {
            // Valores por defecto cuando no hay tourId
            if (tvTourDescription != null) tvTourDescription.setText("Una experiencia increíble que te permitirá conocer los mejores destinos de la región con guías expertos y servicios de primera calidad.");
            if (tvRating != null) tvRating.setText("⭐ 4.7 (65)");
            if (tvDuration != null) tvDuration.setText("6 horas");
            if (tvGroupSize != null) tvGroupSize.setText("10 personas");
            if (tvLanguages != null) tvLanguages.setText("ES, EN");
            return;
        }

        // Set data based on tour type (fallback mock)
        switch (Math.abs(tourId.hashCode()) % 6) {
            case 0:
                if (tvTourDescription != null) tvTourDescription.setText("Descubre la historia y cultura de Lima visitando sus principales atractivos del centro histórico. Un recorrido completo por la Plaza de Armas, Catedral, Palacio de Gobierno y los balcones coloniales más emblemáticos.");
                if (tvRating != null) tvRating.setText("⭐ 4.9 (127)");
                if (tvDuration != null) tvDuration.setText("4 horas");
                if (tvGroupSize != null) tvGroupSize.setText("12 personas");
                if (tvLanguages != null) tvLanguages.setText("ES, EN");
                break;
            case 1:
                if (tvTourDescription != null) tvTourDescription.setText("Visita la maravilla del mundo con transporte y guía incluido desde Cusco. Una experiencia única que incluye tren panorámico y tiempo suficiente para explorar la ciudadela inca.");
                if (tvRating != null) tvRating.setText("⭐ 4.8 (89)");
                if (tvDuration != null) tvDuration.setText("Full Day");
                if (tvGroupSize != null) tvGroupSize.setText("15 personas");
                if (tvLanguages != null) tvLanguages.setText("ES, EN, FR");
                break;
            default:
                if (tvTourDescription != null) tvTourDescription.setText("Una experiencia increíble que te permitirá conocer los mejores destinos de la región con guías expertos y servicios de primera calidad.");
                if (tvRating != null) tvRating.setText("⭐ 4.7 (65)");
                if (tvDuration != null) tvDuration.setText("6 horas");
                if (tvGroupSize != null) tvGroupSize.setText("10 personas");
                if (tvLanguages != null) tvLanguages.setText("ES, EN");
        }
    }

    private void setupRecyclerViews() {
        // Itinerary RecyclerView
        if (rvItinerary != null) {
            rvItinerary.setLayoutManager(new LinearLayoutManager(this));
            rvItinerary.setAdapter(new ItineraryAdapter());
        }

        // Reviews RecyclerView
        if (rvReviews != null) {
            rvReviews.setLayoutManager(new LinearLayoutManager(this));
            rvReviews.setAdapter(new ReviewsAdapter());
        }
    }

    private void setupClickListeners() {

        if (btnSeeAllReviews != null) {
            btnSeeAllReviews.setOnClickListener(v -> {
                Intent intent = new Intent(this, AllReviewsActivity.class);
                intent.putExtra("tour_name", tourName);
                startActivity(intent);
            });
        }

        if (btnContact != null) {
            btnContact.setOnClickListener(v -> {
                Intent intent = new Intent(this, CompanyChatActivity.class);
                intent.putExtra("company_name", companyName);
                intent.putExtra("tour_name", tourName);
                startActivity(intent);
            });
        }

        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                Intent intent = new Intent(this, TourBookingActivity.class);
                intent.putExtra("tour_id", tourId);
                intent.putExtra("tour_name", tourName);
                intent.putExtra("company_id", companyId);
                intent.putExtra("company_name", companyName);
                intent.putExtra("price", price);
                startActivity(intent);
            });
        }

        if (btnViewMap != null) {
            btnViewMap.setOnClickListener(v -> Toast.makeText(this, "Mapa completo próximamente", Toast.LENGTH_SHORT).show());
        }

        if (btnDirections != null) {
            btnDirections.setOnClickListener(v -> Toast.makeText(this, "Direcciones próximamente", Toast.LENGTH_SHORT).show());
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
    
    /**
     * Verificar si el usuario ya tiene una reserva confirmada para este tour
     */
    private void checkExistingReservation() {
        if (currentUserId == null || tourId == null) return;
        
        firestoreManager.hasConfirmedReservation(currentUserId, tourId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Boolean hasReservation = (Boolean) result;
                if (hasReservation != null && hasReservation) {
                    // Usuario ya tiene una reserva confirmada
                    btnBookNow.setEnabled(false);
                    btnBookNow.setText("Ya tienes una reserva");
                    btnBookNow.setAlpha(0.5f);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Si falla la verificación, permitir reservar de todas formas
                android.util.Log.e("TourDetail", "Error verificando reserva: " + e.getMessage());
            }
        });
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

// Adaptador para el itinerario
class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_itinerary_point, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TextView time = holder.itemView.findViewById(R.id.tv_time);
        TextView locationName = holder.itemView.findViewById(R.id.tv_location_name);
        TextView activityDescription = holder.itemView.findViewById(R.id.tv_activity_description);
        TextView duration = holder.itemView.findViewById(R.id.tv_duration);
        android.view.View timelineLine = holder.itemView.findViewById(R.id.view_timeline_line);

        String[] times = {"09:00", "10:00", "11:30", "13:00"};
        String[] locations = {"Plaza de Armas", "Catedral de Lima", "Palacio de Gobierno", "Balcones Coloniales"};
        String[] descriptions = {
            "Visita a la plaza principal de Lima, conoce su historia y arquitectura colonial.",
            "Recorrido por el interior de la catedral, admirando su arquitectura y arte religioso.",
            "Vista exterior del palacio presidencial y cambio de guardia.",
            "Caminata por las calles históricas observando los famosos balcones limeños."
        };
        String[] durations = {"⏱️ 45 minutos", "⏱️ 30 minutos", "⏱️ 45 minutos", "⏱️ 30 minutos"};

        int index = position % times.length;
        
        time.setText(times[index]);
        locationName.setText(locations[index]);
        activityDescription.setText(descriptions[index]);
        duration.setText(durations[index]);
        
        // Hide timeline line for last item
        if (position == getItemCount() - 1) {
            timelineLine.setVisibility(android.view.View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() { return 4; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

// Adaptador para las reseñas
class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TextView userInitial = holder.itemView.findViewById(R.id.tv_user_initial);
        TextView userName = holder.itemView.findViewById(R.id.tv_user_name);
        TextView rating = holder.itemView.findViewById(R.id.tv_rating);
        TextView reviewText = holder.itemView.findViewById(R.id.tv_review_text);
        TextView reviewDate = holder.itemView.findViewById(R.id.tv_review_date);

        String[] names = {"Ana García", "Carlos Mendoza", "María López"};
        String[] initials = {"A", "C", "M"};
        String[] ratings = {"⭐⭐⭐⭐⭐", "⭐⭐⭐⭐⭐", "⭐⭐⭐⭐"};
        String[] reviews = {
            "Excelente tour, el guía muy conocedor y amable. Los lugares visitados fueron increíbles y la comida deliciosa.",
            "Una experiencia inolvidable. La organización fue perfecta y aprendimos mucho sobre la historia de Lima.",
            "Muy recomendado. El tour cumplió todas nuestras expectativas y el precio es muy justo."
        };
        String[] dates = {"Hace 2 semanas", "Hace 1 mes", "Hace 3 semanas"};

        int index = position % names.length;
        
        userInitial.setText(initials[index]);
        userName.setText(names[index]);
        rating.setText(ratings[index]);
        reviewText.setText(reviews[index]);
        reviewDate.setText(dates[index]);
    }

    @Override
    public int getItemCount() { return 3; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}
