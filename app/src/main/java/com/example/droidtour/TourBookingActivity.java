package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class TourBookingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_booking);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String tourName = getIntent().getStringExtra("tourName");
        String companyName = getIntent().getStringExtra("companyName");
        double price = getIntent().getDoubleExtra("price", 0);

        ((TextView) findViewById(R.id.tv_tour_name)).setText(tourName);
        ((TextView) findViewById(R.id.tv_company_name)).setText(companyName);
        ((TextView) findViewById(R.id.tv_total_amount)).setText(String.format("S/. %.2f", price));

        MaterialButton book = findViewById(R.id.btn_book_tour);
        book.setOnClickListener(v -> {
            Toast.makeText(this, "Reserva realizada. Se generará QR-Inicio y QR-Fin.", Toast.LENGTH_LONG).show();
            // TODO: Persistir reserva y generar QRs
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

