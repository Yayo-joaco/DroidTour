package com.example.droidtour.client;

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
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ClientProfileActivity extends AppCompatActivity {

    private static final String TAG = "ClientProfileActivity";
    private static final int EDIT_PROFILE_REQUEST = 1;

    private ImageView profileImage;
    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvFirstName, tvLastName, tvBirthDate;
    private TextView tvDocumentType, tvDocumentNumber, tvPhone;
    private TextView tvToursCount, tvRating, tvMemberSince;
    private CardView cardLanguages;
    private FloatingActionButton fabEdit;
    private View btnEditPhoto;

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

        setupToolbar();
        initializeViews();
        loadUserDataFromFirestore();

        // Ocultar sección de idiomas para cliente
        hideLanguagesSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos cuando se regresa a esta pantalla (por si editó el perfil)
        loadUserDataFromFirestore();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            // Recargar datos después de editar el perfil
            loadUserDataFromFirestore();
        }
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
        profileImage = findViewById(R.id.profile_image);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvUserRole = findViewById(R.id.tv_user_role);

        // Información personal - NUEVOS CAMPOS
        tvFirstName = findViewById(R.id.tv_first_name);
        tvLastName = findViewById(R.id.tv_last_name);
        tvBirthDate = findViewById(R.id.tv_birth_date);
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

        // Botón editar foto
        btnEditPhoto = findViewById(R.id.profile_image);

        // Placeholder inicial para la imagen
        if (profileImage != null) {
            profileImage.setImageResource(R.drawable.ic_avatar_24);
        }
    }

    /**
     * 🔥 Cargar datos del usuario desde Firestore
     */
    private void loadUserDataFromFirestore() {
        if (!prefsManager.isLoggedIn()) {
            Toast.makeText(this, "Sesión expirada. Por favor, inicia sesión nuevamente", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        String userId = prefsManager.getUserId();
        Log.d(TAG, "🔥 ==========================================");
        Log.d(TAG, "🔥 INICIANDO CARGA DE PERFIL");
        Log.d(TAG, "🔥 ==========================================");
        Log.d(TAG, "🔥 userId de PreferencesManager: " + userId);
        Log.d(TAG, "🔥 userName de PreferencesManager: " + prefsManager.getUserName());
        Log.d(TAG, "🔥 userEmail de PreferencesManager: " + prefsManager.getUserEmail());
        Log.d(TAG, "🔥 userPhone de PreferencesManager: " + prefsManager.getUserPhone());
        Log.d(TAG, "🔥 ==========================================");

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ userId es NULL o vacío!");
            Toast.makeText(this, "Error: No se encontró el ID del usuario", Toast.LENGTH_SHORT).show();
            showFallbackData();
            return;
        }

        // Cargar datos del usuario desde Firestore
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "✅ getUserById onSuccess - result: " + result);
                User user = (User) result;
                if (user != null) {
                    Log.d(TAG, "✅ Usuario encontrado: " + user.getEmail());

                    // Actualizar UI con datos reales de Firestore
                    updateUIWithUserData(user);

                    // Cargar estadísticas después de cargar los datos del usuario
                    loadStatistics(userId);
                    setupClickListeners();
                } else {
                    Log.e(TAG, "❌ Usuario es null en Firestore");
                    Toast.makeText(ClientProfileActivity.this, "No se pudo cargar la información del perfil", Toast.LENGTH_SHORT).show();
                    showFallbackData();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando usuario desde Firestore: " + e.getMessage(), e);
                Toast.makeText(ClientProfileActivity.this, "Error cargando perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showFallbackData();
            }
        });
    }

    /**
     * 🔥 Actualizar la UI con los datos del usuario
     */
    private void updateUIWithUserData(User user) {
        // Header - Nombre completo
        String fullName = user.getFullName() != null && !user.getFullName().isEmpty()
                ? user.getFullName()
                : (user.getFirstName() != null || user.getLastName() != null)
                ? ( (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "") ).trim()
                : prefsManager.getUserName();
        tvUserName.setText(fullName != null && !fullName.isEmpty() ? fullName : "Usuario");
        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : prefsManager.getUserEmail());
        tvUserRole.setText("CLIENTE");

        // NUEVOS CAMPOS: Nombres y apellidos por separado
        tvFirstName.setText(user.getFirstName() != null ? user.getFirstName() : "N/A");
        tvLastName.setText(user.getLastName() != null ? user.getLastName() : "N/A");

        // NUEVO CAMPO: Fecha de nacimiento (usa personalData dentro de User)
        String birthDateStr = null;
        if (user.getPersonalData() != null) {
            birthDateStr = user.getPersonalData().getDateOfBirth();
        }
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            try {
                String formattedDate = formatBirthDate(birthDateStr);
                tvBirthDate.setText(formattedDate);
            } catch (Exception e) {
                Log.e(TAG, "Error formateando fecha de nacimiento: " + e.getMessage());
                tvBirthDate.setText(birthDateStr);
            }
        } else {
            tvBirthDate.setText("N/A");
        }

        // Información personal existente (documento)
        String docType = null;
        String docNum = null;
        if (user.getPersonalData() != null) {
            docType = user.getPersonalData().getDocumentType();
            docNum = user.getPersonalData().getDocumentNumber();
        }
        tvDocumentType.setText(docType != null ? docType : "DNI");
        tvDocumentNumber.setText(docNum != null ? docNum : "N/A");

        // Teléfono
        String phone = null;
        if (user.getPersonalData() != null) phone = user.getPersonalData().getPhoneNumber();
        if (phone == null || phone.isEmpty()) {
            phone = prefsManager.getUserPhone();
        }
        tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "N/A");

        // 📸 CARGAR FOTO DE PERFIL DESDE FIREBASE
        String photoUrl = null;
        if (user.getPersonalData() != null) {
            photoUrl = user.getPersonalData().getProfileImageUrl();
        }

        Log.d(TAG, "📸 URL de foto de perfil: " + photoUrl);

        if (profileImage != null) {
            Glide.with(ClientProfileActivity.this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_avatar_24)
                    .error(R.drawable.ic_avatar_24)
                    .circleCrop()
                    .into(profileImage);

            Log.d(TAG, "✅ Foto de perfil cargada con Glide");
        } else {
            Log.e(TAG, "❌ profileImage es null, no se puede cargar la foto");
        }
    }

    /**
     * 🔥 Formatear fecha de nacimiento
     */
    private String formatBirthDate(Object birthDate) {
        if (birthDate instanceof com.google.firebase.Timestamp) {
            // Si es un Timestamp de Firebase
            com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) birthDate;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        } else if (birthDate instanceof String) {
            // Si es un string, devolverlo tal cual (ya está formateado en el modelo)
            return (String) birthDate;
        } else {
            return birthDate.toString();
        }
    }

    /**
     * 🔥 Mostrar datos de fallback desde PreferencesManager
     */
    private void showFallbackData() {
        tvUserName.setText(prefsManager.getUserName());
        tvUserEmail.setText(prefsManager.getUserEmail());
        tvUserRole.setText("CLIENTE");

        // NUEVOS CAMPOS con valores por defecto
        tvFirstName.setText("N/A");
        tvLastName.setText("N/A");
        tvBirthDate.setText("N/A");

        tvPhone.setText(prefsManager.getUserPhone() != null ? prefsManager.getUserPhone() : "N/A");
        tvDocumentType.setText("DNI");
        tvDocumentNumber.setText("N/A");

        // Foto por defecto
        if (profileImage != null) {
            profileImage.setImageResource(R.drawable.ic_avatar_24);
        }

        setupClickListeners();
    }

    /**
     * 🔥 Cargar estadísticas del usuario desde Firestore
     */
    private void loadStatistics(String userId) {
        // Asegurar que la etiqueta diga "Tours Reservados" para cliente
        TextView tvStatLabel1 = findViewById(R.id.tv_stat_label_1);
        if (tvStatLabel1 != null) {
            tvStatLabel1.setText("Tours\nReservados");
        }

        // Cargar cantidad de reservas desde Firestore (implementación pendiente)
        // TODO: implementar la carga real cuando FirestoreManager devuelva reservas

        // Cargar rating promedio del usuario
        double avgRating = 4.8; // Valor por defecto
        tvRating.setText(String.format("%.1f", avgRating));

        // Miembro desde (año actual por defecto)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        tvMemberSince.setText(String.valueOf(currentYear));
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
            btnEditPhoto.setOnClickListener(v -> Toast.makeText(this, "Editar foto próximamente", Toast.LENGTH_SHORT).show());
        }

        // FAB editar - NUEVO: Abrir ClientEditProfileActivity
        if (fabEdit != null) {
            fabEdit.setOnClickListener(v -> {
                Intent editIntent = new Intent(ClientProfileActivity.this, ClientEditProfileActivity.class);
                startActivityForResult(editIntent, EDIT_PROFILE_REQUEST);
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