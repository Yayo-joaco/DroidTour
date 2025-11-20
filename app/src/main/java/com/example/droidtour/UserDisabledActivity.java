package com.example.droidtour;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class UserDisabledActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_disabled);

        TextView tvReason = findViewById(R.id.tv_disabled_reason);
        Button btnContact = findViewById(R.id.btn_contact_support);
        Button btnClose = findViewById(R.id.btn_close);

        String reason = getIntent().getStringExtra("reason");
        if (reason == null || reason.isEmpty()) reason = "Tu cuenta ha sido desactivada por el administrador.";
        tvReason.setText(reason);

        btnContact.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:support@droidtour.com"));
            email.putExtra(Intent.EXTRA_SUBJECT, "Cuenta deshabilitada - Soporte");
            email.putExtra(Intent.EXTRA_TEXT, "Hola, mi cuenta con ID: " + getIntent().getStringExtra("userId") + " ha sido desactivada. Por favor, ayúdame a revisarla.\n\nGracias.");
            startActivity(Intent.createChooser(email, "Contactar soporte"));
        });

        btnClose.setOnClickListener(v -> {
            // Volver al login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

