package com.example.droidtour.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdminProfileActivity extends AppCompatActivity {

    private static final String TAG = "AdminProfileActivity";

    private ImageView ivProfileImage;
    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvFirstName, tvLastName;
    private TextView tvDocumentType, tvDocumentNumber, tvPhone;
    private TextView tvToursCount, tvRating, tvMemberSince;
    private CardView cardLanguages;
    private FloatingActionButton fabEdit;
    private View rowBirthDate, dividerBeforeBirthDate, dividerAfterBirthDate;
    private View ratingSection;

    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myprofile);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar helpers
        prefsManager = new PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();

        // Validar sesión
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }

        // Validar que el usuario sea ADMIN o COMPANY_ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("COMPANY_ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        loadUserDataFromFirestore();

        // Ocultar sección de idiomas para admin
        hideLanguagesSection();
        
        // Ocultar sección de valoración para administrador de empresa
        hideRatingSection();
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
        ivProfileImage = findViewById(R.id.profile_image);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvUserRole = findViewById(R.id.tv_user_role);

        // Información personal
        tvFirstName = findViewById(R.id.tv_first_name);
        tvLastName = findViewById(R.id.tv_last_name);
        tvDocumentType = findViewById(R.id.tv_document_type);
        
        // Fecha de nacimiento (ocultar para admin)
        rowBirthDate = findViewById(R.id.row_birth_date);
        dividerBeforeBirthDate = findViewById(R.id.divider_before_birth_date);
        dividerAfterBirthDate = findViewById(R.id.divider_after_birth_date);
        
        // Ocultar fecha de nacimiento
        if (rowBirthDate != null) rowBirthDate.setVisibility(View.GONE);
        if (dividerBeforeBirthDate != null) dividerBeforeBirthDate.setVisibility(View.GONE);
        if (dividerAfterBirthDate != null) dividerAfterBirthDate.setVisibility(View.GONE);
        tvDocumentNumber = findViewById(R.id.tv_document_number);
        tvPhone = findViewById(R.id.tv_phone);

        // Estadísticas
        tvToursCount = findViewById(R.id.tv_tours_count);
        tvRating = findViewById(R.id.tv_rating);
        tvMemberSince = findViewById(R.id.tv_member_since);
        ratingSection = findViewById(R.id.rating_section);

        // Sección de idiomas (para ocultar)
        cardLanguages = findViewById(R.id.card_languages);

        // FAB
        fabEdit = findViewById(R.id.fab_edit);
    }

    private void loadUserDataFromFirestore() {
        String userId = prefsManager.getUserId();
        Log.d(TAG, "Cargando datos del admin: " + userId);

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "userId es NULL o vacío!");
            showFallbackData();
            return;
        }

        // Cargar datos del usuario desde Firestore
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null) {
                    Log.d(TAG, "Usuario admin encontrado: " + user.getEmail());
                    updateUIWithUserData(user);
                    loadStatistics(user);
                    setupClickListeners();
                } else {
                    Log.e(TAG, "Usuario es null en Firestore");
                    showFallbackData();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario desde Firestore: " + e.getMessage(), e);
                showFallbackData();
            }
        });
    }

    private void updateUIWithUserData(User user) {
        // Header - Nombre completo
        String fullName = user.getFullName();
        if (fullName == null || fullName.isEmpty()) {
            if (user.getPersonalData() != null) {
                String firstName = user.getPersonalData().getFirstName();
                String lastName = user.getPersonalData().getLastName();
                fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            }
        }
        tvUserName.setText(fullName != null && !fullName.isEmpty() ? fullName : "Administrador");
        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : prefsManager.obtenerEmail());
        tvUserRole.setText("ADMINISTRADOR");

        // Información personal
        if (user.getPersonalData() != null) {
            User.PersonalData pd = user.getPersonalData();
            tvFirstName.setText(pd.getFirstName() != null ? pd.getFirstName() : "N/A");
            tvLastName.setText(pd.getLastName() != null ? pd.getLastName() : "N/A");
            
            // Documento
            tvDocumentType.setText(pd.getDocumentType() != null ? pd.getDocumentType() : "DNI");
            tvDocumentNumber.setText(pd.getDocumentNumber() != null ? pd.getDocumentNumber() : "N/A");
            
            // Teléfono
            tvPhone.setText(pd.getPhoneNumber() != null ? pd.getPhoneNumber() : "N/A");
        } else {
            tvFirstName.setText(user.getFirstName() != null ? user.getFirstName() : "N/A");
            tvLastName.setText(user.getLastName() != null ? user.getLastName() : "N/A");
            tvDocumentType.setText("DNI");
            tvDocumentNumber.setText("N/A");
            tvPhone.setText("N/A");
        }

        // Foto de perfil
        String photoUrl = user.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_avatar_24)
                    .circleCrop()
                    .into(ivProfileImage);
        }
    }

    private void showFallbackData() {
        tvUserName.setText(prefsManager.obtenerUsuario());
        tvUserEmail.setText(prefsManager.obtenerEmail());
        tvUserRole.setText("ADMINISTRADOR");

        tvFirstName.setText("N/A");
        tvLastName.setText("N/A");
        tvDocumentType.setText("DNI");
        tvDocumentNumber.setText("N/A");
        tvPhone.setText("N/A");

        setupClickListeners();
    }

    private void loadStatistics(User user) {
        // Cambiar etiqueta para admin
        TextView tvStatLabel1 = findViewById(R.id.tv_stat_label_1);
        if (tvStatLabel1 != null) {
            tvStatLabel1.setText("Tours\nCreados");
        }

        // Obtener companyId del usuario
        String companyId = user.getCompanyId();
        if (companyId != null && !companyId.isEmpty()) {
            // Cargar tours de la empresa desde Firebase
            firestoreManager.getAllToursByCompany(companyId, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    @SuppressWarnings("unchecked")
                    List<com.example.droidtour.models.Tour> tours = (List<com.example.droidtour.models.Tour>) result;
                    int toursCount = tours != null ? tours.size() : 0;
                    
                    if (tvToursCount != null) {
                        tvToursCount.setText(String.valueOf(toursCount));
                    }
                    
                    Log.d(TAG, "Tours creados cargados: " + toursCount);
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error cargando tours de la empresa", e);
                    if (tvToursCount != null) {
                        tvToursCount.setText("0");
                    }
                }
            });
        } else {
            Log.w(TAG, "Usuario no tiene companyId asignado");
            if (tvToursCount != null) {
                tvToursCount.setText("0");
            }
        }

        // Miembro desde
        if (tvMemberSince != null) {
            if (user.getCreatedAt() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(user.getCreatedAt());
                int year = cal.get(Calendar.YEAR);
                tvMemberSince.setText(String.valueOf(year));
            } else {
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                tvMemberSince.setText(String.valueOf(currentYear));
            }
        }
    }

    private void hideLanguagesSection() {
        if (cardLanguages != null) {
            cardLanguages.setVisibility(View.GONE);
        }
    }
    
    private void hideRatingSection() {
        if (ratingSection != null) {
            ratingSection.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // FAB editar - Por ahora solo muestra toast
        if (fabEdit != null) {
            fabEdit.setOnClickListener(v -> {
                Toast.makeText(this, "Edición de perfil próximamente", Toast.LENGTH_SHORT).show();
            });
        }

        // Foto de perfil
        if (ivProfileImage != null) {
            ivProfileImage.setOnClickListener(v -> {
                Toast.makeText(this, "Cambiar foto próximamente", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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

