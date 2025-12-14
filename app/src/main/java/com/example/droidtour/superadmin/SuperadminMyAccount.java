package com.example.droidtour.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.droidtour.models.User;

public class SuperadminMyAccount extends AppCompatActivity {
    
    private static final String TAG = "SuperadminMyAccount";
    
    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private ImageView profileImage;
    private TextView tvUserName, tvUserEmail;
    
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
        
        setContentView(R.layout.activity_myaccount);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar Firestore
        firestoreManager = FirestoreManager.getInstance();

        // Inicializar vistas
        profileImage = findViewById(R.id.profile_image);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);

        // Toolbar: permitir botón de retroceso y mostrar título de la app
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Mostrar título (tomado de strings.xml) en la ActionBar
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.mi_cuenta));
        }
        // Asegurar que el título también se establezca directamente en la toolbar (para temas donde ActionBar no lo muestre)
        toolbar.setTitle(getString(R.string.mi_cuenta));
        // Asegurar color de título (si el tema no lo aplica)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Cargar y mostrar datos del usuario desde Firestore
        loadUserDataFromFirestore();

        // Encontrar las tarjetas y asignar listeners para redirecciones
        CardView cardProfile = findViewById(R.id.card_my_profile);
        CardView cardPayment = findViewById(R.id.card_payment_methods);
        CardView cardSettings = findViewById(R.id.card_settings);
        CardView cardLogout = findViewById(R.id.card_logout);

        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                Intent i = new Intent(SuperadminMyAccount.this, SuperadminProfileActivity.class);
                startActivity(i);
            });
        }

        // Los superadministradores no tienen métodos de pago, así que lo ocultamos
        if (cardPayment != null) {
            cardPayment.setVisibility(android.view.View.GONE);
        }

        // Los superadministradores no tienen configuración separada, así que ocultamos esta opción también
        if (cardSettings != null) {
            cardSettings.setVisibility(android.view.View.GONE);
        }
        
        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                // Cerrar sesión
                prefsManager.logout();
                Intent i = new Intent(SuperadminMyAccount.this, LoginActivity.class);
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
    
    private void loadUserDataFromFirestore() {
        String userId = prefsManager.getUserId();
        
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

        // Cargar datos completos desde Firestore usando FirestoreManager
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User userObj = (User) result;
                updateUIWithUserData(userObj);
            }

            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "Usuario no encontrado en Firestore con ID: " + userId + ": " + e.getMessage());
                loadFallbackData();
            }
        });
    }

    private void updateUIWithUserData(User user) {

        if (user == null) {
            loadFallbackData();
            return;
        }
        // Obtener nombre completo desde el objeto User
        String fullName = user.getFullName();
        if (fullName == null || fullName.isEmpty()) {
            String firstName = user.getFirstName();
            String lastName = user.getLastName();
            if (firstName != null && lastName != null) {
                fullName = firstName + " " + lastName;
            } else if (firstName != null) {
                fullName = firstName;
            }
        }
        
        // Obtener email desde el objeto User
        String email = user.getEmail();
        
        // Obtener foto de perfil desde el objeto User
        String profileImageUrl = null;
        if (user.getPersonalData() != null) {
            profileImageUrl = user.getPersonalData().getProfileImageUrl();
        }

        // Actualizar UI
        if (tvUserName != null) {
            tvUserName.setText(fullName != null && !fullName.isEmpty() ? fullName : "Usuario");
        }
        
        if (tvUserEmail != null) {
            tvUserEmail.setText(email != null && !email.isEmpty() ? email : "");
        }

        // Cargar imagen de perfil
        if (profileImage != null) {
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Glide.with(this)
                        .load(profileImageUrl)
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_avatar_24)
                        .error(R.drawable.ic_avatar_24)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_avatar_24);
            }
        }

        // Actualizar PreferencesManager con los datos más recientes
        String phoneNumber = null;
        if (user.getPersonalData() != null) {
            phoneNumber = user.getPersonalData().getPhoneNumber();
        }
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
        
        if (tvUserName != null) {
            tvUserName.setText(userName != null && !userName.isEmpty() ? userName : "Usuario");
        }
        
        if (tvUserEmail != null) {
            tvUserEmail.setText(userEmail != null && !userEmail.isEmpty() ? userEmail : "");
        }
        
        if (profileImage != null) {
            profileImage.setImageResource(R.drawable.ic_avatar_24);
        }
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}


