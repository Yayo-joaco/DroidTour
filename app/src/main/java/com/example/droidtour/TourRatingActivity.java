package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class TourRatingActivity extends AppCompatActivity {

    private TextView tvTourName, tvCompanyName, tvRatingText;
    private TextView star1, star2, star3, star4, star5;
    private TextInputEditText etComment;
    private MaterialButton btnSubmitRating;
    
    private int currentRating = 0;
    private String tourName, companyName, reservationId, tourId;
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private String currentUserId;

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
        
        setContentView(R.layout.activity_tour_rating);

        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = authManager.getCurrentUserId();
        
        // 🔥 TEMPORAL: Para testing sin login
        if (currentUserId == null) {
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", Toast.LENGTH_SHORT).show();
        }

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
        reservationId = getIntent().getStringExtra("reservation_id");
        tourId = getIntent().getStringExtra("tour_id");
        
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
        
        // Deshabilitar botón para evitar múltiples envíos
        btnSubmitRating.setEnabled(false);
        btnSubmitRating.setText("Enviando...");
        
        if (tourId != null) {
            firestoreManager.createReview(tourId, currentUserId, currentRating, comment, 
                new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    // Marcar la reserva como reviewed ANTES de cerrar
                    if (reservationId != null && !reservationId.isEmpty()) {
                        markReservationAsReviewed();
                    } else {
                        // Si no hay reservationId, cerrar inmediatamente
                        Toast.makeText(TourRatingActivity.this, "✅ Valoración enviada: " + currentRating + " estrellas", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                
                @Override
                public void onFailure(Exception e) {
                    btnSubmitRating.setEnabled(true);
                    btnSubmitRating.setText("Enviar Valoración");
                    Toast.makeText(TourRatingActivity.this, "❌ Error enviando valoración", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "✅ Valoración enviada: " + currentRating + " estrellas", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    /**
     * Marcar la reserva como ya valorada
     */
    private void markReservationAsReviewed() {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("hasReview", true);
        
        firestoreManager.updateReservation(reservationId, updates, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                android.util.Log.d("TourRating", "✅ Reserva marcada como valorada");
                // Ahora sí cerrar la actividad
                Toast.makeText(TourRatingActivity.this, "✅ Valoración enviada: " + currentRating + " estrellas", Toast.LENGTH_SHORT).show();
                finish();
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourRating", "❌ Error marcando reserva como valorada", e);
                // Cerrar de todas formas porque la review ya se guardó
                Toast.makeText(TourRatingActivity.this, "✅ Valoración enviada", Toast.LENGTH_SHORT).show();
                finish();
            }
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
    
    private void redirectToLogin() {
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}