package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.client.ClientMainActivity;
import com.example.droidtour.managers.PrefsManager;

public class Onboarding2 extends AppCompatActivity {
    private PrefsManager prefsManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_init_3);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        prefsManager= new PrefsManager(this);

        //Verificación de sesión activa
        if(prefsManager.sesionActiva()){
            redirigirSegunRol();
            finish();
            return;
        }

        //Verificación de presentación del onboarding
        if(!prefsManager.esPrimeraVez()){
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Button btnSiguiente = findViewById(R.id.button2);
        TextView btnSaltar = findViewById(R.id.saltar);

        btnSiguiente.setOnClickListener(v -> {
            prefsManager.marcarPrimeraVezCompletada();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnSaltar.setOnClickListener(v -> {
            prefsManager.marcarPrimeraVezCompletada();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

    }

    private void redirigirSegunRol(){
        String tipoUsuario = prefsManager.obtenerTipoUsuario();
        Intent intent = null;

        switch (tipoUsuario){
            case "SUPERADMIN":
                intent = new Intent(this, SuperadminLogsActivity.class);
                break;
            case "ADMIN":
                intent = new Intent(this, TourAdminMainActivity.class);
                break;
            case "GUIDE":
                intent=new Intent(this, TourGuideMainActivity.class);
                break;
            case "CLIENT":
                intent = new Intent(this, ClientMainActivity.class);
                break;
            default:
                intent = new Intent(this, LoginActivity.class);
                break;
        }
        startActivity(intent);
    }
}
