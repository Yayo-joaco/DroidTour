package com.example.droidtour.client;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.models.Review;
import com.example.droidtour.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;
import java.util.List;

public class ClientProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvDocumentType, tvDocumentNumber, tvPhone;
    private TextView tvToursCount, tvRating, tvMemberSince;
    private CardView cardLanguages;
    private FloatingActionButton fabEdit;
    private ImageButton btnEditPhoto;
    
    // ✅ Firebase Managers
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private String currentUserId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myprofile);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // ✅ Inicializar Firebase
        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        
        // ✅ Verificar sesión
        currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null) {
            // 🔥 TEMPORAL: Para testing sin login
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", Toast.LENGTH_SHORT).show();
        }

        setupToolbar();
        initializeViews();
        loadUserDataFromFirebase();
        setupClickListeners();
        
        // Ocultar sección de idiomas para cliente
        hideLanguagesSection();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mi Perfil");
        }
    }

    private void initializeViews() {
        // Header
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvUserRole = findViewById(R.id.tv_user_role);
        btnEditPhoto = findViewById(R.id.btn_edit_photo_small);
        
        // Información personal
        tvDocumentType = findViewById(R.id.tv_document_type);
        tvDocumentNumber = findViewById(R.id.tv_document_number);
        tvPhone = findViewById(R.id.tv_phone);
        
        // Estadísticas
        tvToursCount = findViewById(R.id.tv_tours_count);
        tvRating = findViewById(R.id.tv_rating);
        tvMemberSince = findViewById(R.id.tv_member_since);
        
        // Sección de idiomas (para ocultar)
        cardLanguages = findViewById(R.id.card_languages);
        
        // FAB
        fabEdit = findViewById(R.id.fab_edit);
    }

    /**
     * ✅ CARGAR DATOS DEL USUARIO DESDE FIREBASE
     */
    private void loadUserDataFromFirebase() {
        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                currentUser = (User) result;
                displayUserData();
                loadStatisticsFromFirebase();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClientProfileActivity.this, 
                    "Error cargando perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ✅ MOSTRAR DATOS DEL USUARIO
     */
    private void displayUserData() {
        tvUserName.setText(currentUser.getFullName());
        tvUserEmail.setText(currentUser.getEmail());
        tvUserRole.setText("CLIENTE");
        
        // Información personal
        tvDocumentType.setText(currentUser.getDocumentType());
        tvDocumentNumber.setText(currentUser.getDocumentNumber());
        tvPhone.setText(currentUser.getPhoneNumber());
    }

    /**
     * ✅ CARGAR ESTADÍSTICAS DESDE FIREBASE
     */
    private void loadStatisticsFromFirebase() {
        // Cargar reservas del usuario
        firestoreManager.getReservationsByUser(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Reservation> reservations = (List<Reservation>) result;
                
                // Tours Reservados
                tvToursCount.setText(String.valueOf(reservations.size()));
                
                // Asegurar que la etiqueta diga "Tours Reservados"
                TextView tvStatLabel1 = findViewById(R.id.tv_stat_label_1);
                if (tvStatLabel1 != null) {
                    tvStatLabel1.setText("Tours\nReservados");
                }
                
                // Cargar valoración
                loadRatingFromFirebase();
                
                // Miembro desde
                if (currentUser.getCreatedAt() != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(currentUser.getCreatedAt());
                    tvMemberSince.setText(String.valueOf(cal.get(Calendar.YEAR)));
                } else {
                    tvMemberSince.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Usar valores por defecto
                tvToursCount.setText("0");
                tvMemberSince.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
            }
        });
    }

    /**
     * ✅ CARGAR VALORACIÓN PROMEDIO
     */
    private void loadRatingFromFirebase() {
        firestoreManager.getReviewsByUser(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Review> reviews = (List<Review>) result;
                
                if (!reviews.isEmpty()) {
                    double totalRating = 0;
                    for (Review review : reviews) {
                        totalRating += review.getRating();
                    }
                    double avgRating = totalRating / reviews.size();
                    tvRating.setText(String.format("%.1f", avgRating));
                } else {
                    tvRating.setText("N/A");
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                tvRating.setText("N/A");
            }
        });
    }

    private void hideLanguagesSection() {
        // Ocultar sección de idiomas para cliente
        if (cardLanguages != null) {
            cardLanguages.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // Botón editar foto
        if (btnEditPhoto != null) {
            btnEditPhoto.setOnClickListener(v -> {
                Toast.makeText(this, "Editar foto próximamente", Toast.LENGTH_SHORT).show();
            });
        }

        // FAB editar
        if (fabEdit != null) {
            fabEdit.setOnClickListener(v -> {
                Toast.makeText(this, "Edición de perfil próximamente", Toast.LENGTH_SHORT).show();
            });
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
}

