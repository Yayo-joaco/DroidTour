package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class GuideActiveTourDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_guide_active_tour);

        String tourName = getIntent().getStringExtra("tour_name");
        double payment = getIntent().getDoubleExtra("payment", 0.0);

        // Show tour details
        android.widget.Toast.makeText(this, 
            "Tour: " + tourName + " - Pago: S/. " + payment, 
            android.widget.Toast.LENGTH_SHORT).show();
        
        finish();
    }
}

