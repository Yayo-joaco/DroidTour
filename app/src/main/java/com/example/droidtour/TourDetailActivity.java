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

public class TourDetailActivity extends AppCompatActivity {
    
    private TextView tvTourName, tvCompanyName, tvTourDescription, tvRating;
    private TextView tvDuration, tvGroupSize, tvLanguages, tvPrice;
    private RecyclerView rvItinerary, rvReviews;
    private MaterialButton btnReserveNow, btnSeeAllReviews, btnContactCompany;
    private MaterialButton btnViewFullMap, btnGetDirections;
    
    private String tourName, companyName;
    private double price;
    private int tourId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_detail);

        getIntentData();
        setupToolbar();
        initializeViews();
        setupTourData();
        setupRecyclerViews();
        setupClickListeners();
    }

    private void getIntentData() {
        tourId = getIntent().getIntExtra("tour_id", 0);
        tourName = getIntent().getStringExtra("tour_name");
        companyName = getIntent().getStringExtra("company_name");
        price = getIntent().getDoubleExtra("price", 0.0);
        
        if (tourName == null) tourName = "Tour Increíble";
        if (companyName == null) companyName = "Empresa de Tours";
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalle del Tour");
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
        tvPrice = findViewById(R.id.tv_price);
        rvItinerary = findViewById(R.id.rv_itinerary);
        rvReviews = findViewById(R.id.rv_reviews);
        btnReserveNow = findViewById(R.id.btn_reserve_now);
        btnSeeAllReviews = findViewById(R.id.btn_see_all_reviews);
        btnContactCompany = findViewById(R.id.btn_contact_company);
        btnViewFullMap = findViewById(R.id.btn_view_full_map);
        btnGetDirections = findViewById(R.id.btn_get_directions);
    }

    private void setupTourData() {
        tvTourName.setText(tourName);
        tvCompanyName.setText("por " + companyName);
        tvPrice.setText("S/. " + String.format("%.2f", price));
        
        // Set data based on tour type
        switch (tourId % 6) {
            case 0:
                tvTourDescription.setText("Descubre la historia y cultura de Lima visitando sus principales atractivos del centro histórico. Un recorrido completo por la Plaza de Armas, Catedral, Palacio de Gobierno y los balcones coloniales más emblemáticos.");
                tvRating.setText("⭐ 4.9 (127)");
                tvDuration.setText("4 horas");
                tvGroupSize.setText("12 personas");
                tvLanguages.setText("ES, EN");
                break;
            case 1:
                tvTourDescription.setText("Visita la maravilla del mundo con transporte y guía incluido desde Cusco. Una experiencia única que incluye tren panorámico y tiempo suficiente para explorar la ciudadela inca.");
                tvRating.setText("⭐ 4.8 (89)");
                tvDuration.setText("Full Day");
                tvGroupSize.setText("15 personas");
                tvLanguages.setText("ES, EN, FR");
                break;
            default:
                tvTourDescription.setText("Una experiencia increíble que te permitirá conocer los mejores destinos de la región con guías expertos y servicios de primera calidad.");
                tvRating.setText("⭐ 4.7 (65)");
                tvDuration.setText("6 horas");
                tvGroupSize.setText("10 personas");
                tvLanguages.setText("ES, EN");
        }
    }

    private void setupRecyclerViews() {
        // Itinerary RecyclerView
        rvItinerary.setLayoutManager(new LinearLayoutManager(this));
        rvItinerary.setAdapter(new ItineraryAdapter());
        
        // Reviews RecyclerView
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(new ReviewsAdapter());
    }

    private void setupClickListeners() {

        btnSeeAllReviews.setOnClickListener(v -> {
            Toast.makeText(this, "Ver todas las reseñas", Toast.LENGTH_SHORT).show();
        });

        btnContactCompany.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompanyChatActivity.class);
            intent.putExtra("company_name", companyName);
            intent.putExtra("tour_name", tourName);
            startActivity(intent);
        });

        btnReserveNow.setOnClickListener(v -> {
            Intent intent = new Intent(this, TourBookingActivity.class);
            intent.putExtra("tour_name", tourName);
            intent.putExtra("company_name", companyName);
            intent.putExtra("price", price);
            startActivity(intent);
        });

        btnViewFullMap.setOnClickListener(v -> {
            // TODO: Abrir mapa completo con Google Maps API
            Toast.makeText(this, "Mapa completo próximamente", Toast.LENGTH_SHORT).show();
        });

        btnGetDirections.setOnClickListener(v -> {
            // TODO: Abrir Google Maps con direcciones
            Toast.makeText(this, "Direcciones próximamente", Toast.LENGTH_SHORT).show();
        });
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
