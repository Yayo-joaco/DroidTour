package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class TourRatingActivity extends AppCompatActivity {

    private TextView tvTourName, tvCompanyName, tvRatingText;
    private TextView star1, star2, star3, star4, star5;
    private TextInputEditText etComment;
    private MaterialButton btnSubmitRating;
    
    private int currentRating = 0;
    private String tourName, companyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_rating);

        getIntentData();
        setupToolbar();
        initializeViews();
        setupStarRating();
        setupClickListeners();
        loadTourData();
    }

    private void getIntentData() {
        tourName = getIntent().getStringExtra("tour_name");
        companyName = getIntent().getStringExtra("company_name");
        
        if (tourName == null) tourName = "Tour Increíble";
        if (companyName == null) companyName = "Empresa de Tours";
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Valorar Tour");
        }
    }

    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvRatingText = findViewById(R.id.tv_rating_text);
        
        star1 = findViewById(R.id.star_1);
        star2 = findViewById(R.id.star_2);
        star3 = findViewById(R.id.star_3);
        star4 = findViewById(R.id.star_4);
        star5 = findViewById(R.id.star_5);
        
        etComment = findViewById(R.id.et_comment);
        btnSubmitRating = findViewById(R.id.btn_submit_rating);
    }

    private void setupStarRating() {
        star1.setOnClickListener(v -> setRating(1));
        star2.setOnClickListener(v -> setRating(2));
        star3.setOnClickListener(v -> setRating(3));
        star4.setOnClickListener(v -> setRating(4));
        star5.setOnClickListener(v -> setRating(5));
    }

    private void setRating(int rating) {
        currentRating = rating;
        updateStarDisplay();
        updateRatingText();
    }

    private void updateStarDisplay() {
        TextView[] stars = {star1, star2, star3, star4, star5};
        
        for (int i = 0; i < stars.length; i++) {
            if (i < currentRating) {
                stars[i].setAlpha(1.0f);
            } else {
                stars[i].setAlpha(0.3f);
            }
        }
    }

    private void updateRatingText() {
        String[] ratingTexts = {
            "Selecciona tu calificación",
            "Muy malo",
            "Malo", 
            "Regular",
            "Bueno",
            "Excelente"
        };
        
        tvRatingText.setText(ratingTexts[currentRating]);
    }

    private void setupClickListeners() {
        btnSubmitRating.setOnClickListener(v -> submitRating());
    }

    private void loadTourData() {
        tvTourName.setText(tourName);
        tvCompanyName.setText(companyName);
    }

    private void submitRating() {
        if (currentRating == 0) {
            Toast.makeText(this, "Por favor selecciona una calificación", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etComment.getText().toString().trim();
        
        // TODO: Enviar calificación al servidor
        Toast.makeText(this, "Valoración enviada: " + currentRating + " estrellas", Toast.LENGTH_SHORT).show();
        
        // Return to previous activity
        finish();
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