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
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.droidtour.models.User;

public class SuperadminProfileActivity extends AppCompatActivity {

    private static final String TAG = "SuperadminProfile";

    private TextView tvUserName, tvUserEmail, tvUserRole;
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
        
        // Información personal
        tvDocumentType = findViewById(R.id.tv_document_type);
        tvDocumentNumber = findViewById(R.id.tv_document_number);
        tvPhone = findViewById(R.id.tv_phone);
        
        // Secciones para ocultar
        cardLanguages = findViewById(R.id.card_languages);
        cardStatistics = findViewById(R.id.card_statistics);
        
        // FAB
        fabEdit = findViewById(R.id.fab_edit);
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
        Log.d(TAG, "Actualizando UI con datos de User");
        
        // Obtener nombre completo
        String fullName = user.getFullName();
        Log.d(TAG, "fullName desde User: " + fullName);
        if (fullName == null || fullName.isEmpty()) {
            String firstName = user.getFirstName();
            String lastName = user.getLastName();
            Log.d(TAG, "firstName: " + firstName + ", lastName: " + lastName);
            if (firstName != null && lastName != null) {
                fullName = firstName + " " + lastName;
            } else if (firstName != null) {
                fullName = firstName;
            }
        }
        Log.d(TAG, "fullName final: " + fullName);
        
        // Obtener email
        String email = user.getEmail();
        Log.d(TAG, "email: " + email);
        
        // Obtener teléfono
        String phoneNumber = user.getPersonalData().getPhoneNumber();
        Log.d(TAG, "phoneNumber: " + phoneNumber);
        
        // Obtener documento
        String documentType = user.getPersonalData().getDocumentType();
        String documentNumber = user.getPersonalData().getDocumentNumber();
        Log.d(TAG, "documentType: " + documentType + ", documentNumber: " + documentNumber);


        
        // Obtener foto de perfil
        String profileImageUrl = null;
        if (user != null && user.getPersonalData() != null) {
            profileImageUrl = user.getPersonalData().getProfileImageUrl();
        }
        Log.d(TAG, "profileImageUrl encontrada: " + profileImageUrl);

        // Actualizar UI
        if (tvUserEmail != null) {
            tvUserEmail.setText(email != null && !email.isEmpty() ? email : "");
        }
        
        if (tvUserRole != null) {
            tvUserRole.setText("SUPER ADMINISTRADOR");
        }

        if (tvDocumentType != null) {
            tvDocumentType.setText(documentType != null && !documentType.isEmpty() ? documentType : "DNI");
        }
        
        if (tvDocumentNumber != null) {
            tvDocumentNumber.setText(documentNumber != null && !documentNumber.isEmpty() ? documentNumber : "N/A");
        }
        
        if (tvPhone != null) {
            tvPhone.setText(phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "N/A");
        }

        // Cargar imagen de perfil
        if (ivProfilePhoto != null) {
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Log.d(TAG, "Cargando imagen desde URL: " + profileImageUrl);
                Glide.with(this)
                        .load(profileImageUrl)
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_avatar_24)
                        .error(R.drawable.ic_avatar_24)
                        .into(ivProfilePhoto);
            } else {
                Log.d(TAG, "No hay URL de imagen, usando placeholder");
                ivProfilePhoto.setImageResource(R.drawable.ic_avatar_24);
            }
        } else {
            Log.w(TAG, "ivProfilePhoto es null");
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

