package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.droidtour.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class TourOfferDetailActivity extends AppCompatActivity {
    
    private com.example.droidtour.utils.PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea un guía
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.item_tour_offer_guide);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Detalle de Oferta");
        toolbar.setNavigationOnClickListener(v -> finish());

        String tourName = getIntent().getStringExtra("tour_name");
        String companyName = getIntent().getStringExtra("company_name");
        double payment = getIntent().getDoubleExtra("payment", 0.0);

        // Show offer details
        android.widget.Toast.makeText(this, 
            "Detalles: " + tourName + " - " + companyName, 
            android.widget.Toast.LENGTH_SHORT).show();
        
        finish();
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
