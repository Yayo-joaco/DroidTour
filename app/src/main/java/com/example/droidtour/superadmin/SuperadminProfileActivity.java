package com.example.droidtour.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.droidtour.models.User;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class SuperadminProfileActivity extends AppCompatActivity {

    private static final String TAG = "SuperadminProfile";

    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvFirstName, tvLastName, tvBirthDate;
    private TextView tvDocumentType, tvDocumentNumber, tvPhone;
    private CardView cardLanguages, cardStatistics;
    private FloatingActionButton fabEdit;
    private ImageButton btnEditPhoto;
    private ImageView ivProfilePhoto;
    
    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea SUPERADMIN o ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("SUPERADMIN") && !userType.equals("ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_myprofile);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar Firestore
        firestoreManager = FirestoreManager.getInstance();

        setupToolbar();
        initializeViews();
        loadUserDataFromFirestore();
        setupClickListeners();
        
        // Ocultar secciones no necesarias para superadministrador
        hideLanguagesSection();
        hideStatisticsSection();
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
        ivProfilePhoto = findViewById(R.id.profile_image);
        
        // Información personal - Campos nuevos
        tvFirstName = findViewById(R.id.tv_first_name);
        tvLastName = findViewById(R.id.tv_last_name);
        tvBirthDate = findViewById(R.id.tv_birth_date);
        tvDocumentType = findViewById(R.id.tv_document_type);
        tvDocumentNumber = findViewById(R.id.tv_document_number);
        tvPhone = findViewById(R.id.tv_phone);
        
        // Secciones para ocultar
        cardLanguages = findViewById(R.id.card_languages);
        cardStatistics = findViewById(R.id.card_statistics);
        
        // FAB - OCULTAR para superadministrador
        fabEdit = findViewById(R.id.fab_edit);
        if (fabEdit != null) {
            fabEdit.setVisibility(View.GONE);
        }
    }

    private void loadUserDataFromFirestore() {
        String userId = prefsManager.getUserId();
        
        Log.d(TAG, "Intentando cargar datos para userId: " + userId);
        
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "No se encontró userId en PreferencesManager");
            loadFallbackData();
            return;
        }

        // Mostrar datos básicos desde PreferencesManager mientras se carga desde Firestore
        if (tvUserName != null) {
            String name = prefsManager.getUserName();
            tvUserName.setText(name != null && !name.isEmpty() ? name : "Usuario");
        }
        if (tvUserEmail != null) {
            String email = prefsManager.getUserEmail();
            tvUserEmail.setText(email != null && !email.isEmpty() ? email : "");
        }
        if (tvUserRole != null) {
            tvUserRole.setText("SUPER ADMINISTRADOR");
        }

        // Cargar datos completos desde Firestore usando FirestoreManager
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User userObj = (User) result;
                Log.d(TAG, "Usuario obtenido exitosamente");
                Log.d(TAG, "Email: " + userObj.getEmail());
                updateUIWithUserData(userObj);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando datos del usuario desde Firestore", e);
                Toast.makeText(SuperadminProfileActivity.this, "Error cargando datos del perfil", Toast.LENGTH_SHORT).show();
                loadFallbackData();
            }
        });
    }

    private void updateUIWithUserData(User user) {
        Log.d(TAG, "🔥 ==========================================");
        Log.d(TAG, "🔥 Actualizando UI con datos de User");
        Log.d(TAG, "🔥 ==========================================");
        
        // Obtener nombre completo
        String fullName = user.getFullName();
        Log.d(TAG, "🔥 fullName desde User: " + fullName);
        if (fullName == null || fullName.isEmpty()) {
            String firstName = user.getFirstName();
            String lastName = user.getLastName();
            Log.d(TAG, "🔥 firstName: " + firstName + ", lastName: " + lastName);
            if (firstName != null && lastName != null) {
                fullName = firstName + " " + lastName;
            } else if (firstName != null) {
                fullName = firstName;
            } else if (lastName != null) {
                fullName = lastName;
            }
        }
        Log.d(TAG, "🔥 fullName final: " + fullName);
        
        // Obtener email
        String email = user.getEmail();
        Log.d(TAG, "🔥 email: " + email);
        
        // Obtener teléfono
        String phoneNumber = null;
        String documentType = null;
        String documentNumber = null;
        if (user.getPersonalData() != null) {
            phoneNumber = user.getPersonalData().getPhoneNumber();
            documentType = user.getPersonalData().getDocumentType();
            documentNumber = user.getPersonalData().getDocumentNumber();
        }
        Log.d(TAG, "🔥 phoneNumber: " + phoneNumber);
        Log.d(TAG, "🔥 documentType: " + documentType + ", documentNumber: " + documentNumber);
        
        // Obtener nombres y apellidos por separado
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        Log.d(TAG, "🔥 firstName: " + firstName + ", lastName: " + lastName);
        
        // Obtener fecha de nacimiento
        String birthDateStr = null;
        if (user.getPersonalData() != null) {
            birthDateStr = user.getPersonalData().getDateOfBirth();
        }
        Log.d(TAG, "🔥 birthDateStr: " + birthDateStr);

        // Actualizar UI - NOMBRE COMPLETO
        if (tvUserName != null) {
            tvUserName.setText(fullName != null && !fullName.isEmpty() ? fullName : "Usuario");
            Log.d(TAG, "✅ tvUserName actualizado: " + fullName);
        } else {
            Log.w(TAG, "⚠️ tvUserName es null");
        }
        
        // Actualizar UI - EMAIL
        if (tvUserEmail != null) {
            tvUserEmail.setText(email != null && !email.isEmpty() ? email : "");
            Log.d(TAG, "✅ tvUserEmail actualizado: " + email);
        }
        
        // Actualizar UI - ROL
        if (tvUserRole != null) {
            tvUserRole.setText("SUPER ADMINISTRADOR");
        }
        
        // Actualizar UI - NOMBRES Y APELLIDOS POR SEPARADO
        if (tvFirstName != null) {
            tvFirstName.setText(firstName != null && !firstName.isEmpty() ? firstName : "N/A");
            Log.d(TAG, "✅ tvFirstName actualizado: " + firstName);
        }
        
        if (tvLastName != null) {
            tvLastName.setText(lastName != null && !lastName.isEmpty() ? lastName : "N/A");
            Log.d(TAG, "✅ tvLastName actualizado: " + lastName);
        }
        
        // Actualizar UI - FECHA DE NACIMIENTO
        if (tvBirthDate != null) {
            if (birthDateStr != null && !birthDateStr.isEmpty()) {
                try {
                    String formattedDate = formatBirthDate(birthDateStr);
                    tvBirthDate.setText(formattedDate);
                    Log.d(TAG, "✅ tvBirthDate actualizado: " + formattedDate);
                } catch (Exception e) {
                    Log.e(TAG, "Error formateando fecha de nacimiento: " + e.getMessage());
                    tvBirthDate.setText(birthDateStr);
                }
            } else {
                tvBirthDate.setText("N/A");
                Log.d(TAG, "✅ tvBirthDate actualizado: N/A");
            }
        }

        // Actualizar UI - DOCUMENTO
        if (tvDocumentType != null) {
            tvDocumentType.setText(documentType != null && !documentType.isEmpty() ? documentType : "DNI");
            Log.d(TAG, "✅ tvDocumentType actualizado: " + documentType);
        }
        
        if (tvDocumentNumber != null) {
            tvDocumentNumber.setText(documentNumber != null && !documentNumber.isEmpty() ? documentNumber : "N/A");
            Log.d(TAG, "✅ tvDocumentNumber actualizado: " + documentNumber);
        }
        
        // Actualizar UI - TELÉFONO
        if (tvPhone != null) {
            tvPhone.setText(phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "N/A");
            Log.d(TAG, "✅ tvPhone actualizado: " + phoneNumber);
        }

        // 📸 CARGAR FOTO DE PERFIL DESDE FIREBASE (mejorado como en ClientProfileActivity)
        String profileImageUrl = null;
        if (user.getPersonalData() != null) {
            profileImageUrl = user.getPersonalData().getProfileImageUrl();
            Log.d(TAG, "📸 PersonalData encontrado");
            Log.d(TAG, "📸 profileImageUrl desde PersonalData: " + profileImageUrl);
        } else {
            Log.w(TAG, "⚠️ PersonalData es null");
        }

        // También intentar obtener desde getPhotoUrl() (método legacy)
        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
            profileImageUrl = user.getPhotoUrl();
            Log.d(TAG, "📸 Intentando obtener desde getPhotoUrl(): " + profileImageUrl);
        }

        Log.d(TAG, "📸 URL final de foto de perfil: " + profileImageUrl);
        Log.d(TAG, "📸 ¿URL es válida? " + (profileImageUrl != null && !profileImageUrl.isEmpty() && profileImageUrl.startsWith("http")));

        // Cargar imagen de perfil
        if (ivProfilePhoto != null) {
            if (profileImageUrl != null && !profileImageUrl.isEmpty() && profileImageUrl.startsWith("http")) {
                Log.d(TAG, "📸 Cargando imagen con Glide desde URL: " + profileImageUrl);
                Glide.with(this)
                        .load(profileImageUrl)
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_avatar_24)
                        .error(R.drawable.ic_avatar_24)
                        .into(ivProfilePhoto);
                Log.d(TAG, "✅ Glide configurado para cargar imagen");
            } else {
                Log.w(TAG, "⚠️ URL de imagen no válida o vacía, usando placeholder");
                ivProfilePhoto.setImageResource(R.drawable.ic_avatar_24);
            }
        } else {
            Log.e(TAG, "❌ ivProfilePhoto es null, no se puede cargar la foto");
        }

        // Actualizar PreferencesManager con los datos más recientes
        String userType = user.getUserType();
        if (userType != null && !userType.isEmpty()) {
            prefsManager.saveUserData(
                    prefsManager.getUserId(),
                    fullName != null ? fullName : prefsManager.getUserName(),
                    email != null ? email : prefsManager.getUserEmail(),
                    phoneNumber != null ? phoneNumber : prefsManager.getUserPhone(),
                    userType
            );
        }
    }

    private void loadFallbackData() {
        // Cargar datos desde PreferencesManager como fallback
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();
        String userPhone = prefsManager.getUserPhone();

        if (tvUserName != null) {
            tvUserName.setText(userName != null && !userName.isEmpty() ? userName : "Usuario");
        }
        
        if (tvUserEmail != null) {
            tvUserEmail.setText(userEmail != null && !userEmail.isEmpty() ? userEmail : "");
        }
        
        if (tvUserRole != null) {
            tvUserRole.setText("SUPER ADMINISTRADOR");
        }
        
        if (tvDocumentType != null) {
            tvDocumentType.setText("DNI");
        }
        
        if (tvDocumentNumber != null) {
            tvDocumentNumber.setText("N/A");
        }
        
        if (tvPhone != null) {
            tvPhone.setText(userPhone != null && !userPhone.isEmpty() ? userPhone : "N/A");
        }
        
        if (ivProfilePhoto != null) {
            ivProfilePhoto.setImageResource(R.drawable.ic_avatar_24);
        }
        
        // Campos de información personal con valores por defecto
        if (tvFirstName != null) {
            tvFirstName.setText("N/A");
        }
        if (tvLastName != null) {
            tvLastName.setText("N/A");
        }
        if (tvBirthDate != null) {
            tvBirthDate.setText("N/A");
        }
    }
    
    /**
     * Formatear fecha de nacimiento
     */
    private String formatBirthDate(Object birthDate) {
        if (birthDate instanceof com.google.firebase.Timestamp) {
            // Si es un Timestamp de Firebase
            com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) birthDate;
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            return sdf.format(timestamp.toDate());
        } else if (birthDate instanceof String) {
            // Si es un string, devolverlo tal cual (ya está formateado en el modelo)
            return (String) birthDate;
        } else {
            return birthDate.toString();
        }
    }

    private void hideLanguagesSection() {
        // Ocultar sección de idiomas para superadministrador
        if (cardLanguages != null) {
            cardLanguages.setVisibility(View.GONE);
        }
    }

    private void hideStatisticsSection() {
        // Ocultar sección de estadísticas para superadministrador
        if (cardStatistics != null) {
            cardStatistics.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // Botón editar foto
        if (btnEditPhoto != null) {
            btnEditPhoto.setOnClickListener(v -> {
                Toast.makeText(this, "Editar foto próximamente", Toast.LENGTH_SHORT).show();
            });
        }

        // FAB editar - NO necesario para superadministrador (ya está oculto)
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

