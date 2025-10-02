package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class TourOfferDetailActivity extends AppCompatActivity {
    
    private TextView tvTourName, tvCompanyName, tvTourDate, tvTourTime, tvTourDuration;
    private TextView tvPaymentAmount, tvParticipants, tvTourDescription, tvMeetingPoint;
    private TextView tvIncluded, tvNotIncluded, tvRequirements;
    private MaterialButton btnAcceptOffer, btnRejectOffer, btnContactCompany;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_offer_detail);
        
        setupToolbar();
        initializeViews();
        loadTourDetails();
        setupClickListeners();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Ver Detalles");
    }
    
    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvTourDate = findViewById(R.id.tv_tour_date);
        tvTourTime = findViewById(R.id.tv_tour_time);
        tvTourDuration = findViewById(R.id.tv_tour_duration);
        tvPaymentAmount = findViewById(R.id.tv_payment_amount);
        tvParticipants = findViewById(R.id.tv_participants);
        tvTourDescription = findViewById(R.id.tv_tour_description);
        tvMeetingPoint = findViewById(R.id.tv_meeting_point);
        tvIncluded = findViewById(R.id.tv_included);
        tvNotIncluded = findViewById(R.id.tv_not_included);
        tvRequirements = findViewById(R.id.tv_requirements);
        
        btnAcceptOffer = findViewById(R.id.btn_accept_offer);
        btnRejectOffer = findViewById(R.id.btn_reject_offer);
        btnContactCompany = findViewById(R.id.btn_contact_company);
    }
    
    private void loadTourDetails() {
        Intent intent = getIntent();
        
        String tourName = intent.getStringExtra("TOUR_NAME");
        String companyName = intent.getStringExtra("COMPANY_NAME");
        String tourDate = intent.getStringExtra("TOUR_DATE");
        String tourTime = intent.getStringExtra("TOUR_TIME");
        String tourDuration = intent.getStringExtra("TOUR_DURATION");
        String paymentAmount = intent.getStringExtra("PAYMENT_AMOUNT");
        String participants = intent.getStringExtra("PARTICIPANTS");
        
        tvTourName.setText(tourName != null ? tourName : "City Tour Lima Centro Histórico");
        tvCompanyName.setText(companyName != null ? companyName : "Inka Travel Peru");
        tvTourDate.setText(tourDate != null ? tourDate : "Hoy");
        tvTourTime.setText(tourTime != null ? tourTime : "09:30 AM");
        tvTourDuration.setText(tourDuration != null ? tourDuration : "3 horas");
        tvPaymentAmount.setText(paymentAmount != null ? paymentAmount : "S/. 200.00");
        tvParticipants.setText(participants != null ? participants : "6 personas");
        
        // Información adicional (mock data)
        tvTourDescription.setText("Descubre el corazón histórico de Lima en este fascinante recorrido por el centro de la ciudad. Visitaremos la Plaza Mayor, la Catedral de Lima, el Palacio de Gobierno y otros sitios emblemáticos que narran la rica historia de la capital peruana.");
        
        tvMeetingPoint.setText("Plaza Mayor de Lima - Frente a la Catedral\nReferencia: Pileta central");
        
        tvIncluded.setText("• Guía turístico profesional\n• Transporte en vehículo privado\n• Entradas a monumentos\n• Agua mineral\n• Seguro de viaje");
        
        tvNotIncluded.setText("• Almuerzo\n• Propinas\n• Gastos personales\n• Souvenirs");
        
        tvRequirements.setText("• Documento de identidad\n• Ropa cómoda y zapatos para caminar\n• Protector solar\n• Cámara fotográfica\n• Conocimiento de español e inglés");
    }
    
    private void setupClickListeners() {
        btnAcceptOffer.setOnClickListener(v -> {
            Toast.makeText(this, "Oferta aceptada - Se notificará a la empresa", Toast.LENGTH_LONG).show();
            finish();
        });
        
        btnRejectOffer.setOnClickListener(v -> {
            Toast.makeText(this, "Oferta rechazada", Toast.LENGTH_SHORT).show();
            finish();
        });
        
        btnContactCompany.setOnClickListener(v -> {
            Toast.makeText(this, "Contactando con la empresa...", Toast.LENGTH_SHORT).show();
            // TODO: Abrir chat o llamada con la empresa
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
