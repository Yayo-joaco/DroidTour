package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_init_1); // Asegúrate de que este XML exista

        new Handler().postDelayed(() -> {
            startActivity(new Intent(this,Onboarding1.class));
            finish(); // Cierra la pantalla de bienvenida
        }, 3000); // Espera 2 segundos
    }
}

