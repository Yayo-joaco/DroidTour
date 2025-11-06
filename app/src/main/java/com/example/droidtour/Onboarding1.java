package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class Onboarding1 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_init_2);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        Button btnSiguiente = findViewById(R.id.button);
        TextView btnSaltar = findViewById(R.id.saltar);

        btnSaltar.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, Onboarding2.class));
            finish();
        });
    }
}
