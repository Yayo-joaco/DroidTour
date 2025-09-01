package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class RoleSelectionActivity extends AppCompatActivity {
    
    private MaterialCardView cardRegisterClient, cardRegisterGuide, cardRegisterAdmin;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        initializeViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        cardRegisterClient = findViewById(R.id.card_register_client);
        cardRegisterGuide = findViewById(R.id.card_register_guide);
        cardRegisterAdmin = findViewById(R.id.card_register_admin);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Seleccionar Tipo de Usuario");
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        cardRegisterClient.setOnClickListener(v -> {
            Toast.makeText(this, "Registro como Cliente", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ClientRegistrationActivity.class));
        });

        cardRegisterGuide.setOnClickListener(v -> {
            Toast.makeText(this, "Registro como Guía de Turismo", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, GuideRegistrationActivity.class));
        });

        cardRegisterAdmin.setOnClickListener(v -> {
            Toast.makeText(this, "Registro como Administrador de Empresa", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AdminRegistrationActivity.class));
        });
    }
}
