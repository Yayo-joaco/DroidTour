package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.superadmin.SuperadminMainActivity;
import com.example.droidtour.utils.PreferencesManager;

public class Onboarding1 extends AppCompatActivity {
    private PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_init_2);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        prefsManager = new PreferencesManager(this);

        Button btnSiguiente = findViewById(R.id.button);
        TextView btnSaltar = findViewById(R.id.saltar);

        // Si el usuario ya tiene sesión activa, redirigir según rol y cerrar
        if (prefsManager.sesionActiva()) {
            String tipo = prefsManager.obtenerTipoUsuario();
            switch (tipo) {
                case "SUPERADMIN":
                    startActivity(new Intent(this, SuperadminMainActivity.class));
                    break;
                case "ADMIN":
                    startActivity(new Intent(this, TourAdminMainActivity.class));
                    break;
                case "GUIDE":
                    startActivity(new Intent(this, TourGuideMainActivity.class));
                    break;
                case "CLIENT":
                    startActivity(new Intent(this, com.example.droidtour.client.ClientMainActivity.class));
                    break;
                default:
                    startActivity(new Intent(this, LoginActivity.class));
                    break;
            }
            finish();
            return;
        }

        // Cambiado: al saltar el onboarding ahora ir a LoginActivity y marcar primera vez completada
        btnSaltar.setOnClickListener(v -> {
            prefsManager.marcarPrimeraVezCompletada();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, Onboarding2.class));
            finish();
        });
    }
}
