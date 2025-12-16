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
    private TextView tvGuideName, tvGuideRatingText, tvCompanyRatingText;
    
    // Estrellas del tour
    private TextView starTour1, starTour2, starTour3, starTour4, starTour5;
    // Estrellas del guía
    private TextView starGuide1, starGuide2, starGuide3, starGuide4, starGuide5;
    // Estrellas de la empresa
    private TextView starCompany1, starCompany2, starCompany3, starCompany4, starCompany5;
    
    private TextInputEditText etCommentTour, etCommentGuide, etCommentCompany;
    private MaterialButton btnSubmitRating;
    
    private int currentTourRating = 0;
    private int currentGuideRating = 0;
    private int currentCompanyRating = 0;
    
    private String tourName, companyName, companyId, reservationId, tourId, guideId, guideName;
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private String currentUserId;
    
    private android.view.View cardGuideRating, cardCompanyRating, cardGuideComment, cardCompanyComment;

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
        tvGuideName = findViewById(R.id.tv_guide_name);
        tvGuideRatingText = findViewById(R.id.tv_guide_rating_text);
        tvCompanyRatingText = findViewById(R.id.tv_company_rating_text);
        
        // Estrellas del tour
        starTour1 = findViewById(R.id.star_tour_1);
        starTour2 = findViewById(R.id.star_tour_2);
        starTour3 = findViewById(R.id.star_tour_3);
        starTour4 = findViewById(R.id.star_tour_4);
        starTour5 = findViewById(R.id.star_tour_5);
        
        // Estrellas del guía
        starGuide1 = findViewById(R.id.star_guide_1);
        starGuide2 = findViewById(R.id.star_guide_2);
        starGuide3 = findViewById(R.id.star_guide_3);
        starGuide4 = findViewById(R.id.star_guide_4);
        starGuide5 = findViewById(R.id.star_guide_5);
        
        // Estrellas de la empresa
        starCompany1 = findViewById(R.id.star_company_1);
        starCompany2 = findViewById(R.id.star_company_2);
        starCompany3 = findViewById(R.id.star_company_3);
        starCompany4 = findViewById(R.id.star_company_4);
        starCompany5 = findViewById(R.id.star_company_5);
        
        // Comentarios
        etCommentTour = findViewById(R.id.et_comment_tour);
        etCommentGuide = findViewById(R.id.et_comment_guide);
        etCommentCompany = findViewById(R.id.et_comment_company);
        
        btnSubmitRating = findViewById(R.id.btn_submit_rating);
        
        // Cards para mostrar/ocultar
        cardGuideRating = findViewById(R.id.card_guide_rating);
        cardCompanyRating = findViewById(R.id.card_company_rating);
        cardGuideComment = findViewById(R.id.card_guide_comment);
        cardCompanyComment = findViewById(R.id.card_company_comment);
    }

    private void setupStarRating() {
        // Estrellas del tour
        starTour1.setOnClickListener(v -> setTourRating(1));
        starTour2.setOnClickListener(v -> setTourRating(2));
        starTour3.setOnClickListener(v -> setTourRating(3));
        starTour4.setOnClickListener(v -> setTourRating(4));
        starTour5.setOnClickListener(v -> setTourRating(5));
        
        // Estrellas del guía
        starGuide1.setOnClickListener(v -> setGuideRating(1));
        starGuide2.setOnClickListener(v -> setGuideRating(2));
        starGuide3.setOnClickListener(v -> setGuideRating(3));
        starGuide4.setOnClickListener(v -> setGuideRating(4));
        starGuide5.setOnClickListener(v -> setGuideRating(5));
        
        // Estrellas de la empresa
        starCompany1.setOnClickListener(v -> setCompanyRating(1));
        starCompany2.setOnClickListener(v -> setCompanyRating(2));
        starCompany3.setOnClickListener(v -> setCompanyRating(3));
        starCompany4.setOnClickListener(v -> setCompanyRating(4));
        starCompany5.setOnClickListener(v -> setCompanyRating(5));
    }

    private void setTourRating(int rating) {
        currentTourRating = rating;
        updateStarDisplay(starTour1, starTour2, starTour3, starTour4, starTour5, currentTourRating);
        updateRatingText(tvRatingText, currentTourRating);
    }

    private void setGuideRating(int rating) {
        currentGuideRating = rating;
        updateStarDisplay(starGuide1, starGuide2, starGuide3, starGuide4, starGuide5, currentGuideRating);
        updateRatingText(tvGuideRatingText, currentGuideRating);
        
        // Mostrar/ocultar sección de comentario del guía
        if (currentGuideRating > 0) {
            cardGuideComment.setVisibility(android.view.View.VISIBLE);
        } else {
            cardGuideComment.setVisibility(android.view.View.GONE);
        }
    }

    private void setCompanyRating(int rating) {
        currentCompanyRating = rating;
        updateStarDisplay(starCompany1, starCompany2, starCompany3, starCompany4, starCompany5, currentCompanyRating);
        updateRatingText(tvCompanyRatingText, currentCompanyRating);
        
        // Mostrar/ocultar sección de comentario de la empresa
        if (currentCompanyRating > 0) {
            cardCompanyComment.setVisibility(android.view.View.VISIBLE);
        } else {
            cardCompanyComment.setVisibility(android.view.View.GONE);
        }
    }

    private void updateStarDisplay(TextView star1, TextView star2, TextView star3, TextView star4, TextView star5, int rating) {
        TextView[] stars = {star1, star2, star3, star4, star5};
        
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setAlpha(1.0f);
            } else {
                stars[i].setAlpha(0.3f);
            }
        }
    }

    private void updateRatingText(TextView textView, int rating) {
        String[] ratingTexts = {
            "Selecciona tu calificación",
            "Muy malo",
            "Malo", 
            "Regular",
            "Bueno",
            "Excelente"
        };
        
        if (rating == 0) {
            textView.setText("(Opcional)");
        } else {
            textView.setText(ratingTexts[rating]);
        }
    }

    private void setupClickListeners() {
        btnSubmitRating.setOnClickListener(v -> submitRating());
    }

    private void loadTourData() {
        tvTourName.setText(tourName);
        tvCompanyName.setText(companyName);
        
        // Cargar datos del guía y tour desde la reserva
        if (reservationId != null && !reservationId.isEmpty()) {
            firestoreManager.getReservationById(reservationId, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    com.example.droidtour.models.Reservation reservation = (com.example.droidtour.models.Reservation) result;
                    if (reservation != null) {
                        // Obtener datos del guía
                        guideId = reservation.getGuideId();
                        guideName = reservation.getGuideName();
                        
                        // Si no hay guía en la reserva, obtener del tour
                        if ((guideId == null || guideId.trim().isEmpty()) && tourId != null) {
                            loadGuideFromTour();
                        } else {
                            displayGuideInfo();
                        }
                        
                        // Completar otros datos
                        if (tourId == null && reservation.getTourId() != null) {
                            tourId = reservation.getTourId();
                        }
                        if (companyId == null && reservation.getCompanyId() != null) {
                            companyId = reservation.getCompanyId();
                        }
                        if (companyName == null && reservation.getCompanyName() != null) {
                            companyName = reservation.getCompanyName();
                            tvCompanyName.setText(companyName);
                        }
                    }
                }
                
                @Override
                public void onFailure(Exception e) {
                    // Si falla, intentar obtener del tour directamente
                    if (tourId != null) {
                        loadGuideFromTour();
                    }
                }
            });
        } else if (tourId != null) {
            loadGuideFromTour();
        }
    }
    
    private void loadGuideFromTour() {
        firestoreManager.getTourById(tourId, new FirestoreManager.TourCallback() {
            @Override
            public void onSuccess(com.example.droidtour.models.Tour tour) {
                if (tour != null) {
                    guideId = tour.getAssignedGuideId();
                    guideName = tour.getAssignedGuideName();
                    displayGuideInfo();
                    
                    if (companyId == null && tour.getCompanyId() != null) {
                        companyId = tour.getCompanyId();
                    }
                }
            }
            
            @Override
            public void onFailure(String error) {
                // Si no hay guía, ocultar sección de guía
                if (cardGuideRating != null) {
                    cardGuideRating.setVisibility(android.view.View.GONE);
                }
            }
        });
    }
    
    private void displayGuideInfo() {
        if (guideName != null && !guideName.trim().isEmpty() && tvGuideName != null) {
            tvGuideName.setText("Guía: " + guideName);
            if (cardGuideRating != null) {
                cardGuideRating.setVisibility(android.view.View.VISIBLE);
            }
        } else {
            // Si no hay guía, ocultar sección de guía
            if (cardGuideRating != null) {
                cardGuideRating.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void submitRating() {
        // Validar que al menos el rating del tour esté presente
        if (currentTourRating == 0) {
            Toast.makeText(this, "Por favor selecciona una calificación para el tour", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (reservationId == null || reservationId.trim().isEmpty()) {
            Toast.makeText(this, "Error: No se encontró la reserva", Toast.LENGTH_SHORT).show();
            return;
        }

        String commentTour = etCommentTour != null ? etCommentTour.getText().toString().trim() : "";
        String commentGuide = etCommentGuide != null ? etCommentGuide.getText().toString().trim() : "";
        String commentCompany = etCommentCompany != null ? etCommentCompany.getText().toString().trim() : "";
        
        // Deshabilitar botón para evitar múltiples envíos
        btnSubmitRating.setEnabled(false);
        btnSubmitRating.setText("Enviando...");
        
        // Crear objeto Review completo
        com.example.droidtour.models.Review review = new com.example.droidtour.models.Review();
        review.setUserId(currentUserId);
        review.setReservationId(reservationId);
        review.setTourId(tourId);
        review.setTourName(tourName);
        review.setCompanyId(companyId);
        review.setCompanyName(companyName);
        review.setGuideId(guideId);
        review.setGuideName(guideName);
        
        // Ratings
        review.setRating((float) currentTourRating);
        review.setReviewText(commentTour.isEmpty() ? null : commentTour);
        
        if (currentGuideRating > 0) {
            review.setGuideRating((float) currentGuideRating);
            review.setGuideReviewText(commentGuide.isEmpty() ? null : commentGuide);
        }
        
        if (currentCompanyRating > 0) {
            review.setCompanyRating((float) currentCompanyRating);
            review.setCompanyReviewText(commentCompany.isEmpty() ? null : commentCompany);
        }
        
        // Crear review usando el nuevo método
        firestoreManager.createReview(review, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.Review savedReview = (com.example.droidtour.models.Review) result;
                Toast.makeText(TourRatingActivity.this, "✅ Valoración enviada exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            }
            
            @Override
            public void onFailure(Exception e) {
                btnSubmitRating.setEnabled(true);
                btnSubmitRating.setText("Enviar Valoración");
                String errorMsg = e.getMessage();
                if (errorMsg == null) errorMsg = "Error desconocido";
                Toast.makeText(TourRatingActivity.this, "❌ Error: " + errorMsg, Toast.LENGTH_LONG).show();
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