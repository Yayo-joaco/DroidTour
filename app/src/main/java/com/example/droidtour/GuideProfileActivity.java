package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.User;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuideProfileActivity extends AppCompatActivity {

    private static final String TAG = "GuideProfileActivity";
    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvDocumentType, tvDocumentNumber, tvPhone;
    private ChipGroup chipGroupLanguages;
    private TextView tvToursCount, tvRating, tvMemberSince, tvStatLabel1;
    private CardView cardLanguages;
    private FloatingActionButton fabEdit;
    private ImageButton btnEditPhoto;

    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private String currentUserId;

    // Mapa para convertir códigos de idioma a nombres completos
    private static final Map<String, String> LANGUAGE_NAMES = new HashMap<String, String>() {{
        put("es", "Español");
        put("en", "Inglés");
        put("fr", "Francés");
        put("pt", "Portugués");
        put("de", "Alemán");
        put("it", "Italiano");
        put("ja", "Japonés");
        put("zh", "Chino");
        put("ko", "Coreano");
        put("ru", "Ruso");
        put("ar", "Árabe");
        put("qu", "Quechua");
        put("ay", "Aymara");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar helpers
        prefsManager = new PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);

        // Validar sesión
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }

        // Validar que el usuario sea un guía
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            Toast.makeText(this, "Acceso denegado: Solo guías", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            finish();
            return;
        }

        // Obtener ID del usuario actual
        currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = prefsManager.getUserId();
        }

        setContentView(R.layout.activity_myprofile);

        setupToolbar();
        initializeViews();
        setupClickListeners();

        // Cargar datos del guía desde Firebase
        loadUserData();
        loadStatistics();

        // Mostrar sección de idiomas para guía
        showLanguagesSection();
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

        // Información personal
        tvDocumentType = findViewById(R.id.tv_document_type);
        tvDocumentNumber = findViewById(R.id.tv_document_number);
        tvPhone = findViewById(R.id.tv_phone);

        // Idiomas
        chipGroupLanguages = findViewById(R.id.chip_group_languages);

        // Estadísticas
        tvToursCount = findViewById(R.id.tv_tours_count);
        tvRating = findViewById(R.id.tv_rating);
        tvMemberSince = findViewById(R.id.tv_member_since);
        tvStatLabel1 = findViewById(R.id.tv_stat_label_1);

        // Sección de idiomas (para mostrar)
        cardLanguages = findViewById(R.id.card_languages);

        // FAB
        fabEdit = findViewById(R.id.fab_edit);
    }

    private void loadUserData() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Error: currentUserId es null o vacío");
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }

        Log.d(TAG, "🔄 Cargando datos del guía: " + currentUserId);

        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                Log.d(TAG, "✅ Datos del guía cargados");
                displayUserData(user);
                // También cargar idiomas desde user_roles
                loadLanguagesFromUserRoles();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando datos del guía", e);
                Toast.makeText(GuideProfileActivity.this,
                        "Error al cargar perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Usar datos de PreferencesManager como fallback
                displayFallbackData();
            }
        });
    }

    private void displayUserData(User user) {
        // Header - obtener nombre desde personalData
        String fullName = "Usuario";
        if (user.getPersonalData() != null) {
            fullName = user.getPersonalData().getFullName();
            if (fullName == null || fullName.isEmpty()) {
                String firstName = user.getPersonalData().getFirstName();
                String lastName = user.getPersonalData().getLastName();
                fullName = (firstName != null ? firstName : "") + " " +
                        (lastName != null ? lastName : "");
                fullName = fullName.trim();
            }
        }
        if (fullName.isEmpty()) fullName = "Usuario";

        tvUserName.setText(fullName);
        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        tvUserRole.setText("GUIA DE TURISMO");

        // Información personal desde personalData
        if (user.getPersonalData() != null) {
            User.PersonalData pd = user.getPersonalData();

            String docType = pd.getDocumentType();
            tvDocumentType.setText(docType != null ? docType : "DNI");

            String docNumber = pd.getDocumentNumber();
            tvDocumentNumber.setText(docNumber != null ? docNumber : "N/A");

            String phoneNumber = pd.getPhoneNumber();
            tvPhone.setText(phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "N/A");
        } else {
            tvDocumentType.setText("DNI");
            tvDocumentNumber.setText("N/A");
            tvPhone.setText("N/A");
        }
    }

    /**
     * Cargar idiomas desde la colección user_roles
     */
    private void loadLanguagesFromUserRoles() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FirestoreManager.COLLECTION_USER_ROLES)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        List<String> languages = null;

                        // Intentar obtener languages de diferentes estructuras posibles
                        if (doc.contains("languages")) {
                            Object langObj = doc.get("languages");
                            if (langObj instanceof List) {
                                languages = (List<String>) langObj;
                            }
                        } else if (doc.contains("guide")) {
                            Object guideObj = doc.get("guide");
                            if (guideObj instanceof Map) {
                                Map<String, Object> guideMap = (Map<String, Object>) guideObj;
                                Object langObj = guideMap.get("languages");
                                if (langObj instanceof List) {
                                    languages = (List<String>) langObj;
                                }
                            }
                        } else if (doc.contains("roles")) {
                            Object rolesObj = doc.get("roles");
                            if (rolesObj instanceof Map) {
                                Map<String, Object> rolesMap = (Map<String, Object>) rolesObj;
                                Object guideRole = rolesMap.get("guide");
                                if (guideRole instanceof Map) {
                                    Map<String, Object> guideMap = (Map<String, Object>) guideRole;
                                    Object langObj = guideMap.get("languages");
                                    if (langObj instanceof List) {
                                        languages = (List<String>) langObj;
                                    }
                                }
                            }
                        }

                        displayLanguageChips(languages);
                    } else {
                        // Si no hay documento en user_roles, intentar cargar desde guides
                        loadLanguagesFromGuides();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error cargando idiomas desde user_roles: " + e.getMessage());
                    loadLanguagesFromGuides();
                });
    }

    /**
     * Cargar idiomas desde la colección guides (fallback)
     */
    private void loadLanguagesFromGuides() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FirestoreManager.COLLECTION_GUIDES)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Object langObj = doc.get("languages");
                        if (langObj instanceof List) {
                            List<String> languages = (List<String>) langObj;
                            displayLanguageChips(languages);
                        } else {
                            displayLanguageChips(null);
                        }
                    } else {
                        displayLanguageChips(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error cargando idiomas desde guides: " + e.getMessage());
                    displayLanguageChips(null);
                });
    }

    private void displayLanguageChips(List<String> languageCodes) {
        // Limpiar chips existentes
        chipGroupLanguages.removeAllViews();

        if (languageCodes == null || languageCodes.isEmpty()) {
            // Agregar un chip indicando que no hay idiomas
            Chip chip = new Chip(this);
            chip.setText("No especificado");
            chip.setChipBackgroundColorResource(R.color.light_gray);
            chip.setTextColor(getResources().getColor(R.color.dark_gray, null));
            chip.setClickable(false);
            chipGroupLanguages.addView(chip);
            return;
        }

        // Agregar un chip por cada idioma
        for (String languageCode : languageCodes) {
            Chip chip = new Chip(this);

            // Convertir código a nombre completo
            String languageName = LANGUAGE_NAMES.get(languageCode.toLowerCase());
            if (languageName == null) {
                // Si no se encuentra el código, usar el código en mayúsculas
                languageName = languageCode.toUpperCase();
            }

            chip.setText(languageName);
            chip.setChipBackgroundColorResource(R.color.primary);
            chip.setTextColor(getResources().getColor(R.color.white, null));
            chip.setClickable(false);
            chip.setChipIconResource(R.drawable.ic_language);
            chip.setChipIconTintResource(R.color.white);

            chipGroupLanguages.addView(chip);
        }
    }

    private void displayFallbackData() {
        tvUserName.setText(prefsManager.getUserName());
        tvUserEmail.setText(prefsManager.getUserEmail());
        tvUserRole.setText("GUIA DE TURISMO");
        tvDocumentType.setText("DNI");
        tvDocumentNumber.setText("N/A");
        tvPhone.setText(prefsManager.getUserPhone() != null && !prefsManager.getUserPhone().isEmpty()
                ? prefsManager.getUserPhone() : "N/A");
        displayLanguageChips(null);
    }

    private void loadStatistics() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Error: currentUserId es null o vacío al cargar estadísticas");
            return;
        }

        // Cambiar etiqueta para guía
        if (tvStatLabel1 != null) {
            tvStatLabel1.setText("Tours\nGuiados");
        }

        // Cargar número de tours guiados
        firestoreManager.getReservationsByGuide(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<Reservation> reservations = (List<Reservation>) result;
                int toursCount = reservations.size();
                tvToursCount.setText(String.valueOf(toursCount));
                Log.d(TAG, "✅ Tours guiados: " + toursCount);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando tours guiados", e);
                tvToursCount.setText("0");
            }
        });

        // Cargar rating desde user_roles o guides
        loadRatingFromFirestore();

        // Miembro desde (obtener año de createdAt si existe)
        loadMemberSinceYear();
    }

    /**
     * Cargar rating desde user_roles o guides
     */
    private void loadRatingFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Primero intentar desde user_roles
        db.collection(FirestoreManager.COLLECTION_USER_ROLES)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Float rating = null;

                        if (doc.contains("rating")) {
                            Object ratingObj = doc.get("rating");
                            if (ratingObj instanceof Number) {
                                rating = ((Number) ratingObj).floatValue();
                            }
                        } else if (doc.contains("guide")) {
                            Object guideObj = doc.get("guide");
                            if (guideObj instanceof Map) {
                                Map<String, Object> guideMap = (Map<String, Object>) guideObj;
                                Object ratingObj = guideMap.get("rating");
                                if (ratingObj instanceof Number) {
                                    rating = ((Number) ratingObj).floatValue();
                                }
                            }
                        }

                        if (rating != null && rating > 0) {
                            tvRating.setText(String.format("%.1f", rating));
                        } else {
                            // Si no hay rating en user_roles, intentar desde guides
                            loadRatingFromGuides();
                        }
                    } else {
                        loadRatingFromGuides();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error cargando rating desde user_roles: " + e.getMessage());
                    loadRatingFromGuides();
                });
    }

    /**
     * Cargar rating desde colección guides (fallback)
     */
    private void loadRatingFromGuides() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FirestoreManager.COLLECTION_GUIDES)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Object ratingObj = doc.get("rating");
                        if (ratingObj instanceof Number) {
                            float rating = ((Number) ratingObj).floatValue();
                            tvRating.setText(String.format("%.1f", rating));
                        } else {
                            tvRating.setText("0.0");
                        }
                    } else {
                        tvRating.setText("0.0");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error cargando rating desde guides: " + e.getMessage());
                    tvRating.setText("0.0");
                });
    }

    /**
     * Cargar año de registro desde createdAt
     */
    private void loadMemberSinceYear() {
        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user.getCreatedAt() != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(user.getCreatedAt());
                    int year = cal.get(Calendar.YEAR);
                    tvMemberSince.setText(String.valueOf(year));
                } else {
                    // Usar año actual por defecto
                    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                    tvMemberSince.setText(String.valueOf(currentYear));
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Usar año actual por defecto
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                tvMemberSince.setText(String.valueOf(currentYear));
            }
        });
    }

    private void showLanguagesSection() {
        // Mostrar sección de idiomas para guía
        if (cardLanguages != null) {
            cardLanguages.setVisibility(View.VISIBLE);
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

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}