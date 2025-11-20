package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.droidtour.utils.PreferencesManager;

public class GuideApprovalPendingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_approval_pending); // Necesitarás crear este layout

        TextView tvMessage = findViewById(R.id.tvMessage);
        Button btnLogout = findViewById(R.id.btnLogout);

        tvMessage.setText("Tu registro como guía está en proceso de revisión. " +
                "Recibirás una notificación cuando tu cuenta sea activada por el administrador.");

        btnLogout.setOnClickListener(v -> {
            // Cerrar sesión
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

            PreferencesManager prefsManager = new PreferencesManager(this);
            prefsManager.clearUserData();

            // Redirigir al login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}