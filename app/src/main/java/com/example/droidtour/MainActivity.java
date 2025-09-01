package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardSuperadmin, cardTourAdmin, cardTourGuide, cardClient;
    private MaterialButton btnLogin;

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

    private void initViews() {
        cardSuperadmin = findViewById(R.id.card_superadmin);
        cardTourAdmin = findViewById(R.id.card_tour_admin);
        cardTourGuide = findViewById(R.id.card_tour_guide);
        cardClient = findViewById(R.id.card_client);
        btnLogin = findViewById(R.id.btn_login);
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
    }
}