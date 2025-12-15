package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.droidtour.admin.CompanyInfoActivity;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

public class AdminMyAccountActivity extends AppCompatActivity {

    private static final String TAG = "AdminMyAccountActivity";
    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private ImageView ivProfileImage;
    private TextView tvUserName, tvUserEmail;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar managers
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
        
        setContentView(R.layout.activity_myaccount);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Mi Cuenta");
        }
        toolbar.setTitle("Mi Cuenta");
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Inicializar vistas
        ivProfileImage = findViewById(R.id.profile_image);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        
        // Encontrar las tarjetas
        CardView cardProfile = findViewById(R.id.card_my_profile);
        CardView cardPayment = findViewById(R.id.card_payment_methods);
        CardView cardSettings = findViewById(R.id.card_settings);
        CardView cardBusinessProfile = findViewById(R.id.card_business_profile);
        CardView cardLogout = findViewById(R.id.card_logout);

        // Ocultar métodos de pago y preferencias para admin
        if (cardPayment != null) {
            cardPayment.setVisibility(View.GONE);
        }
        if (cardSettings != null) {
            cardSettings.setVisibility(View.GONE);
        }
        
        // Mostrar perfil de empresa
        if (cardBusinessProfile != null) {
            cardBusinessProfile.setVisibility(View.VISIBLE);
            cardBusinessProfile.setOnClickListener(v -> {
                Intent i = new Intent(AdminMyAccountActivity.this, CompanyInfoActivity.class);
                startActivity(i);
            });
        }

        // Mi Perfil - Abrir pantalla de perfil del admin
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                Intent profileIntent = new Intent(AdminMyAccountActivity.this, com.example.droidtour.admin.AdminProfileActivity.class);
                startActivity(profileIntent);
            });
        }

        // Cerrar sesión
        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                prefsManager.cerrarSesion();
                Intent i = new Intent(AdminMyAccountActivity.this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
        }
        
        // Cargar datos del usuario desde Firebase
        loadUserDataFromFirebase();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void loadUserDataFromFirebase() {
        String userId = prefsManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            loadUserDataFromLocal();
            return;
        }
        
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null) {
                    // Nombre
                    String fullName = user.getFullName();
                    if (fullName != null && !fullName.isEmpty()) {
                        tvUserName.setText(fullName);
                    } else {
                        tvUserName.setText("Administrador");
                    }
                    
                    // Email
                    String email = user.getEmail();
                    if (email != null && !email.isEmpty()) {
                        tvUserEmail.setText(email);
                    }
                    
                    // Foto de perfil
                    String photoUrl = user.getPhotoUrl();
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(AdminMyAccountActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar_24)
                                .circleCrop()
                                .into(ivProfileImage);
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando datos del usuario", e);
                loadUserDataFromLocal();
            }
        });
    }
    
    private void loadUserDataFromLocal() {
        String userName = prefsManager.obtenerUsuario();
        String userEmail = prefsManager.obtenerEmail();

        if (tvUserName != null) {
            tvUserName.setText(userName != null && !userName.isEmpty() ? userName : "Administrador");
        }

        if (tvUserEmail != null) {
            tvUserEmail.setText(userEmail != null && !userEmail.isEmpty() ? userEmail : "admin@empresa.com");
        }
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

