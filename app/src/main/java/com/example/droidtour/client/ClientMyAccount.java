package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

public class ClientMyAccount extends AppCompatActivity {
    private static final String TAG = "ClientMyAccount";

    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private String currentUserId;

    private ImageView profileImage;
    private TextView tvUserName;
    private TextView tvUserEmail;

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

        // Validar que el usuario sea CLIENT
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
            redirectToLogin();
            finish();
            return;
        }

        setContentView(R.layout.activity_myaccount);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar Firebase
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = prefsManager.getUserId();

        // Toolbar: permitir botón de retroceso y mostrar título de la app
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.mi_cuenta));
        }
        toolbar.setTitle(getString(R.string.mi_cuenta));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        toolbar.setNavigationOnClickListener(v -> finish());

        // Inicializar vistas
        profileImage = findViewById(R.id.profile_image);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);

        // Cargar datos del usuario (primero desde prefs, luego desde Firebase)
        loadUserDataFromPrefs();
        loadUserDataFromFirebase();

        // Encontrar las tarjetas y asignar listeners para redirecciones
        CardView cardProfile = findViewById(R.id.card_my_profile);
        CardView cardPayment = findViewById(R.id.card_payment_methods);
        CardView cardSettings = findViewById(R.id.card_settings);
        CardView cardLogout = findViewById(R.id.card_logout);

        // Ocultar sección de Preferencias para cliente turista
        if (cardSettings != null) {
            cardSettings.setVisibility(android.view.View.GONE);
        }

        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                Intent i = new Intent(ClientMyAccount.this, ClientProfileActivity.class);
                startActivity(i);
            });
        }

        if (cardPayment != null) {
            cardPayment.setOnClickListener(v -> {
                Intent i = new Intent(ClientMyAccount.this, PaymentMethodsActivity.class);
                startActivity(i);
            });
        }

        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                // Cerrar sesión
                prefsManager.logout();
                Intent i = new Intent(ClientMyAccount.this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescar datos cuando se regresa a esta pantalla
        loadUserDataFromFirebase();
    }

    /**
     * Carga datos rápidos desde SharedPreferences
     */
    private void loadUserDataFromPrefs() {
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();

        if (tvUserName != null) {
            tvUserName.setText(userName != null && !userName.isEmpty() ? userName : "Usuario");
        }

        if (tvUserEmail != null) {
            tvUserEmail.setText(userEmail != null && !userEmail.isEmpty() ? userEmail : "email@ejemplo.com");
        }

        // Placeholder para la imagen mientras carga desde Firebase
        if (profileImage != null) {
            profileImage.setImageResource(R.drawable.ic_avatar_24);
        }
    }

    /**
     * Carga datos completos desde Firestore incluyendo la foto de perfil
     */
    private void loadUserDataFromFirebase() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Log.e(TAG, "No hay userId disponible");
            return;
        }

        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (!(result instanceof User)) {
                    Log.e(TAG, "Resultado no es un objeto User");
                    return;
                }

                User user = (User) result;

                // Actualizar nombre
                String fullName = user.getFullName();
                if (fullName != null && !fullName.trim().isEmpty()) {
                    if (tvUserName != null) {
                        tvUserName.setText(fullName);
                    }
                }

                // Actualizar email
                String email = user.getEmail();
                if (email != null && !email.trim().isEmpty()) {
                    if (tvUserEmail != null) {
                        tvUserEmail.setText(email);
                    }
                }

                // Cargar foto de perfil
                String photoUrl = null;
                if (user.getPersonalData() != null) {
                    photoUrl = user.getPersonalData().getProfileImageUrl();
                }

                if (profileImage != null) {
                    Glide.with(ClientMyAccount.this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_avatar_24)
                            .error(R.drawable.ic_avatar_24)
                            .circleCrop()
                            .into(profileImage);
                }

                Log.d(TAG, "Datos del usuario cargados exitosamente desde Firebase");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando datos del usuario desde Firebase", e);

                // Mantener placeholder en caso de error
                if (profileImage != null) {
                    profileImage.setImageResource(R.drawable.ic_avatar_24);
                }
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}