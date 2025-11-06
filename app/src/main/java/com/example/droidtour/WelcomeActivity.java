package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.client.ClientMainActivity;
import com.example.droidtour.managers.PrefsManager;

public class WelcomeActivity extends AppCompatActivity {

    private PrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        setContentView(R.layout.welcome_init_1); // Tu layout actual del splash

        prefsManager = new PrefsManager(this);

        // Espera 3 segundos y luego decide adónde ir
        new Handler().postDelayed(this::verificarFlujo, 3000);
    }

    private void verificarFlujo() {
        Intent intent;

        if (prefsManager.sesionActiva()) {
            // Ya tiene sesión → redirigir según rol
            intent = redirigirSegunRol();
        } else if (prefsManager.esPrimeraVez()) {
            // Es la primera vez → mostrar onboarding
            intent = new Intent(this, Onboarding1.class);
        } else {
            // Ya usó la app pero no ha iniciado sesión → login
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // Cierra WelcomeActivity
    }

    private Intent redirigirSegunRol() {
        String tipoUsuario = prefsManager.obtenerTipoUsuario();
        Intent intent;

        switch (tipoUsuario) {
            case "SUPERADMIN":
                intent = new Intent(this, SuperadminMainActivity.class);
                break;
            case "ADMIN":
                intent = new Intent(this, TourAdminMainActivity.class);
                break;
            case "GUIDE":
                intent = new Intent(this, TourGuideMainActivity.class);
                break;
            case "CLIENT":
                intent = new Intent(this, ClientMainActivity.class);
                break;
            default:
                intent = new Intent(this, LoginActivity.class);
                break;
        }

        return intent;
    }
}
