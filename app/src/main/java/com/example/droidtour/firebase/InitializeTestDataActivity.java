package com.example.droidtour.firebase;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.droidtour.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Activity para inicializar datos de prueba en Firestore
 * Solo para desarrollo
 */
public class InitializeTestDataActivity extends AppCompatActivity {
    
    private MaterialButton btnInitializeClient, btnInitializeGuide;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private FirebaseClientDataInitializer clientDataInitializer;
    private FirebaseGuideDataInitializer guideDataInitializer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize_test_data);
        
        btnInitializeClient = findViewById(R.id.btn_initialize_client);
        btnInitializeGuide = findViewById(R.id.btn_initialize_guide);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);
        
        clientDataInitializer = new FirebaseClientDataInitializer();
        guideDataInitializer = new FirebaseGuideDataInitializer();
        
        btnInitializeClient.setOnClickListener(v -> initializeClientData());
        btnInitializeGuide.setOnClickListener(v -> initializeGuideData());
    }
    
    private void initializeClientData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Debes iniciar sesión primero", Toast.LENGTH_LONG).show();
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        String userEmail = auth.getCurrentUser().getEmail();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Confirmar Inicialización Cliente")
            .setMessage("📧 " + userEmail + "\n🆔 " + userId + "\n\nSe crearán datos de prueba de CLIENTE")
            .setPositiveButton("Sí, Continuar", (dialog, which) -> startClientInitialization(userId))
            .setNegativeButton("Cancelar", null)
            .show();
    }
    
    private void initializeGuideData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Debes iniciar sesión primero", Toast.LENGTH_LONG).show();
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        String userEmail = auth.getCurrentUser().getEmail();
        String userName = auth.getCurrentUser().getDisplayName() != null ? 
                         auth.getCurrentUser().getDisplayName() : "Guía";
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Confirmar Inicialización Guía")
            .setMessage("📧 " + userEmail + "\n🆔 " + userId + "\n\nSe crearán datos de prueba de GUÍA\n(ofertas, reservas, notificaciones)")
            .setPositiveButton("Sí, Continuar", (dialog, which) -> startGuideInitialization(userId, userName))
            .setNegativeButton("Cancelar", null)
            .show();
    }
    
    private void startClientInitialization(String userId) {
        btnInitializeClient.setEnabled(false);
        btnInitializeGuide.setEnabled(false);
        progressBar.setVisibility(android.view.View.VISIBLE);
        tvStatus.setText("Inicializando datos de CLIENTE para:\n" + userId);
        
        clientDataInitializer.initializeAllClientData(userId, new FirebaseClientDataInitializer.ClientDataCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(android.view.View.GONE);
                tvStatus.setText("✅ Datos de cliente inicializados correctamente");
                btnInitializeClient.setEnabled(true);
                btnInitializeGuide.setEnabled(true);
                btnInitializeClient.setText("Reinicializar Cliente");
                Toast.makeText(InitializeTestDataActivity.this, 
                    "¡Datos de cliente creados exitosamente!", Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(android.view.View.GONE);
                tvStatus.setText("❌ Error al inicializar datos de cliente:\n" + e.getMessage());
                btnInitializeClient.setEnabled(true);
                btnInitializeGuide.setEnabled(true);
                Toast.makeText(InitializeTestDataActivity.this, 
                    "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e("InitTestData", "Error initializing client data", e);
            }
        });
    }
    
    private void startGuideInitialization(String userId, String userName) {
        btnInitializeClient.setEnabled(false);
        btnInitializeGuide.setEnabled(false);
        progressBar.setVisibility(android.view.View.VISIBLE);
        tvStatus.setText("Inicializando datos de GUÍA para:\n" + userId);
        
        guideDataInitializer.initializeAllData(userId, userName, (success, message) -> {
            progressBar.setVisibility(android.view.View.GONE);
            btnInitializeClient.setEnabled(true);
            btnInitializeGuide.setEnabled(true);
            
            if (success) {
                tvStatus.setText("✅ Datos de guía inicializados correctamente");
                btnInitializeGuide.setText("Reinicializar Guía");
                Toast.makeText(InitializeTestDataActivity.this, 
                    "¡Datos de guía creados exitosamente!", Toast.LENGTH_LONG).show();
            } else {
                tvStatus.setText("❌ Error al inicializar datos de guía:\n" + message);
                Toast.makeText(InitializeTestDataActivity.this, 
                    "Error: " + message, Toast.LENGTH_LONG).show();
                android.util.Log.e("InitTestData", "Error initializing guide data: " + message);
            }
        });
    }
}

