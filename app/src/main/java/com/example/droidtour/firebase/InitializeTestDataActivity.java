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
 * Solo para desarrollo - usar con a20221957@pucp.edu.pe
 */
public class InitializeTestDataActivity extends AppCompatActivity {
    
    private MaterialButton btnInitialize;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private FirebaseClientDataInitializer dataInitializer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize_test_data);
        
        btnInitialize = findViewById(R.id.btn_initialize_data);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);
        
        dataInitializer = new FirebaseClientDataInitializer();
        
        btnInitialize.setOnClickListener(v -> initializeData());
    }
    
    private void initializeData() {
        // Obtener el UID del usuario actual
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Debes iniciar sesión primero", 
                Toast.LENGTH_LONG).show();
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        String userEmail = auth.getCurrentUser().getEmail();
        
        // Si no es el email esperado, mostrar confirmación
        if (!"a20221957@pucp.edu.pe".equals(userEmail)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Confirmar Inicialización")
                .setMessage("Esta función creará datos de prueba para tu cuenta actual:\n\n" +
                    "📧 " + userEmail + "\n" +
                    "🆔 " + userId + "\n\n" +
                    "Se crearán:\n" +
                    "• Métodos de pago de prueba\n" +
                    "• Reservas de ejemplo\n" +
                    "• Notificaciones\n" +
                    "• Empresas y tours\n\n" +
                    "⚠️ Esta función está diseñada para a20221957@pucp.edu.pe\n\n" +
                    "¿Deseas continuar?")
                .setPositiveButton("Sí, Continuar", (dialog, which) -> startInitialization(userId))
                .setNegativeButton("Cancelar", null)
                .show();
        } else {
            // Si es el email correcto, proceder directamente
            startInitialization(userId);
        }
    }
    
    private void startInitialization(String userId) {
        // Mostrar progreso
        btnInitialize.setEnabled(false);
        progressBar.setVisibility(android.view.View.VISIBLE);
        tvStatus.setText("Inicializando datos de prueba para:\n" + userId);
        
        // Inicializar datos
        dataInitializer.initializeAllClientData(userId, new FirebaseClientDataInitializer.ClientDataCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(android.view.View.GONE);
                tvStatus.setText("✅ Datos inicializados correctamente\n\n" +
                    "Se han creado para tu usuario:\n" +
                    "• 2 métodos de pago\n" +
                    "• 4 reservas\n" +
                    "• 6 notificaciones\n" +
                    "• 3 reseñas\n" +
                    "• Mensajes de chat\n" +
                    "• Empresas y tours (3)\n" +
                    "• Preferencias\n\n" +
                    "✨ Puedes cerrar esta ventana y navegar a:\n" +
                    "• Métodos de Pago\n" +
                    "• Notificaciones\n" +
                    "• Mis Reservas");
                btnInitialize.setEnabled(true);
                btnInitialize.setText("Reinicializar Datos");
                
                Toast.makeText(InitializeTestDataActivity.this, 
                    "¡Datos de prueba creados exitosamente!", Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(android.view.View.GONE);
                tvStatus.setText("❌ Error al inicializar datos:\n" + e.getMessage() +
                    "\n\nPor favor, intenta de nuevo");
                btnInitialize.setEnabled(true);
                
                Toast.makeText(InitializeTestDataActivity.this, 
                    "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e("InitTestData", "Error initializing data", e);
            }
        });
    }
}

