package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.droidtour.client.ClientMainActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardSuperadmin, cardTourAdmin, cardTourGuide, cardClient;
    private MaterialButton btnLogin;
    
    // 🔥 TEMPORAL: Botón para inicializar datos de ejemplo en Firebase
    private MaterialButton btnInitFirebaseData;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupClickListeners();
    }


    /*
    //este es para probar las pantallas -just testing
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // No inicializar ni usar elementos que no existen en welcome_init_1.xml
    }

     */


    private void initViews() {
        cardSuperadmin = findViewById(R.id.card_superadmin);
        cardTourAdmin = findViewById(R.id.card_tour_admin);
        cardTourGuide = findViewById(R.id.card_tour_guide);
        cardClient = findViewById(R.id.card_client);
        btnLogin = findViewById(R.id.btn_login);
        
        // 🔥 TEMPORAL: Botón para inicializar datos de ejemplo
        btnInitFirebaseData = findViewById(R.id.btn_init_firebase_data);
    }

    private void setupClickListeners() {
        cardSuperadmin.setOnClickListener(v -> {
            Toast.makeText(this, "Accediendo como Superadministrador", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, SuperadminMainActivity.class));
        });

        cardTourAdmin.setOnClickListener(v -> {
            Toast.makeText(this, "Accediendo como Administrador de Empresa", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, TourAdminMainActivity.class));
        });

        cardTourGuide.setOnClickListener(v -> {
            Toast.makeText(this, "Accediendo como Guía de Turismo", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, TourGuideMainActivity.class));
        });

        cardClient.setOnClickListener(v -> {
            Toast.makeText(this, "Accediendo como Cliente", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, ClientMainActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
        
        // 🔥 TEMPORAL: Inicializar datos de ejemplo en Firebase
        btnInitFirebaseData.setOnClickListener(v -> {
            btnInitFirebaseData.setEnabled(false);
            btnInitFirebaseData.setText("Inicializando...");
            
            Toast.makeText(this, "Creando datos de ejemplo en Firebase...", Toast.LENGTH_LONG).show();
            
            com.example.droidtour.firebase.FirebaseClientDataInitializer initializer = 
                new com.example.droidtour.firebase.FirebaseClientDataInitializer();
            
            // 🔥 Usar el UID real del usuario prueba@droidtour.com que ya existe en Firebase Auth
            String realUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            
            initializer.initializeAllClientData(realUserId, new com.example.droidtour.firebase.FirebaseClientDataInitializer.ClientDataCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        btnInitFirebaseData.setEnabled(true);
                        btnInitFirebaseData.setText("✅ Datos Creados");
                        Toast.makeText(MainActivity.this, 
                            "✅ Datos de ejemplo creados exitosamente en Firebase Cloud", 
                            Toast.LENGTH_LONG).show();
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        btnInitFirebaseData.setEnabled(true);
                        btnInitFirebaseData.setText("🔥 Inicializar Datos Firebase");
                        Toast.makeText(MainActivity.this, 
                            "❌ Error: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }


}