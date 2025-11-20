package com.example.droidtour;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.TourOffer;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TourOfferDetailsActivity extends AppCompatActivity {

    private static final String TAG = "TourOfferDetails";
    private TextView tvOfferStatus, tvOfferTime, tvTourName, tvCompanyName, tvTourDate, 
                     tvTourTime, tvTourDuration, tvParticipants, tvPaymentAmount;
    private ChipGroup chipGroupLanguages;
    private MaterialButton btnAccept, btnReject, btnViewFullMap;
    private LinearLayout layoutActionButtons;
    private FirestoreManager firestoreManager;
    private TourOffer currentOffer;
    
    // Mapa para convertir códigos de idioma a nombres completos
    private static final Map<String, String> LANGUAGE_NAMES = new HashMap<String, String>() {{
        put("es", "Español");
        put("en", "Inglés");
        put("fr", "Francés");
        put("pt", "Portugués");
        put("de", "Alemán");
        put("it", "Italiano");
        put("ja", "Japonés");
        put("zh", "Chino");
        put("ko", "Coreano");
        put("ru", "Ruso");
        put("ar", "Árabe");
        put("qu", "Quechua");
        put("ay", "Aymara");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_offer_details);

        firestoreManager = FirestoreManager.getInstance();
        
        setupToolbar();
        initializeViews();
        loadOfferData();
        setupButtons();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeViews() {
        tvOfferStatus = findViewById(R.id.tv_offer_status);
        tvOfferTime = findViewById(R.id.tv_offer_time);
        tvTourName = findViewById(R.id.tv_tour_name);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvTourDate = findViewById(R.id.tv_tour_date);
        tvTourTime = findViewById(R.id.tv_tour_time);
        tvTourDuration = findViewById(R.id.tv_tour_duration);
        tvParticipants = findViewById(R.id.tv_participants);
        tvPaymentAmount = findViewById(R.id.tv_payment_amount);
        chipGroupLanguages = findViewById(R.id.chip_group_languages);
        btnAccept = findViewById(R.id.btn_accept);
        btnReject = findViewById(R.id.btn_reject);
        btnViewFullMap = findViewById(R.id.btn_view_full_map);
        layoutActionButtons = findViewById(R.id.layout_action_buttons);
    }

    private void loadOfferData() {
        // Obtener datos del intent
        String tourName = getIntent().getStringExtra("tourName");
        String companyName = getIntent().getStringExtra("companyName");
        String tourDate = getIntent().getStringExtra("tourDate");
        String tourTime = getIntent().getStringExtra("tourTime");
        String tourDuration = getIntent().getStringExtra("tourDuration");
        double paymentAmount = getIntent().getDoubleExtra("paymentAmount", 0.0);
        int participants = getIntent().getIntExtra("participants", 0);
        String status = getIntent().getStringExtra("status");
        String offerId = getIntent().getStringExtra("offerId");
        String createdTime = getIntent().getStringExtra("createdTime");
        String languagesString = getIntent().getStringExtra("languages");

        // Crear objeto TourOffer para uso interno
        currentOffer = new TourOffer();
        currentOffer.setOfferId(offerId);
        currentOffer.setTourName(tourName);
        currentOffer.setCompanyName(companyName);
        currentOffer.setTourDate(tourDate);
        currentOffer.setTourTime(tourTime);
        currentOffer.setTourDuration(tourDuration);
        currentOffer.setPaymentAmount(paymentAmount);
        currentOffer.setNumberOfParticipants(participants);
        currentOffer.setStatus(status);

        // Mostrar datos
        tvTourName.setText(tourName != null ? tourName : "Tour sin nombre");
        tvCompanyName.setText(companyName != null ? companyName : "Empresa no especificada");
        tvTourDate.setText(tourDate != null ? tourDate : "Fecha no especificada");
        tvTourTime.setText(tourTime != null ? tourTime : "Hora no especificada");
        tvTourDuration.setText(tourDuration != null ? tourDuration : "4 horas");
        tvParticipants.setText(participants + " persona" + (participants != 1 ? "s" : ""));
        tvPaymentAmount.setText(String.format("S/. %.2f", paymentAmount));
        tvOfferStatus.setText(status != null ? status : "PENDIENTE");
        tvOfferTime.setText(createdTime != null ? createdTime : "Recién enviada");

        // Configurar color del estado
        if ("PENDIENTE".equals(status)) {
            tvOfferStatus.setBackgroundResource(R.drawable.circle_orange);
            layoutActionButtons.setVisibility(View.VISIBLE);
        } else if ("RECHAZADA".equals(status)) {
            tvOfferStatus.setBackgroundResource(R.drawable.circle_red);
            layoutActionButtons.setVisibility(View.GONE);
        } else if ("ACEPTADA".equals(status)) {
            tvOfferStatus.setBackgroundResource(R.drawable.circle_green);
            layoutActionButtons.setVisibility(View.GONE);
        }

        // Mostrar idiomas
        if (languagesString != null && !languagesString.isEmpty()) {
            List<String> languageCodes = Arrays.asList(languagesString.split(","));
            displayLanguageChips(languageCodes);
        } else {
            findViewById(R.id.card_languages).setVisibility(View.GONE);
        }
    }

    private void displayLanguageChips(List<String> languageCodes) {
        chipGroupLanguages.removeAllViews();
        
        for (String languageCode : languageCodes) {
            Chip chip = new Chip(this);
            
            String languageName = LANGUAGE_NAMES.get(languageCode.trim().toLowerCase());
            if (languageName == null) {
                languageName = languageCode.toUpperCase();
            }
            
            chip.setText(languageName);
            chip.setChipBackgroundColorResource(R.color.primary);
            chip.setTextColor(getResources().getColor(R.color.white, null));
            chip.setClickable(false);
            chip.setChipIconResource(R.drawable.ic_language);
            chip.setChipIconTintResource(R.color.white);
            
            chipGroupLanguages.addView(chip);
        }
    }

    private void setupButtons() {
        btnAccept.setOnClickListener(v -> acceptOffer());
        btnReject.setOnClickListener(v -> rejectOffer());
        
        btnViewFullMap.setOnClickListener(v -> {
            // TODO: Abrir mapa completo del tour
            Toast.makeText(this, "Ver mapa completo del tour", Toast.LENGTH_SHORT).show();
        });
    }

    private void acceptOffer() {
        if (currentOffer == null || currentOffer.getOfferId() == null) {
            Toast.makeText(this, "Error: Datos de oferta no disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        firestoreManager.updateOfferStatus(currentOffer.getOfferId(), "ACEPTADA", 
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Toast.makeText(TourOfferDetailsActivity.this, 
                        "✅ Oferta aceptada: " + currentOffer.getTourName(), 
                        Toast.LENGTH_LONG).show();
                    
                    // Actualizar UI
                    tvOfferStatus.setText("ACEPTADA");
                    tvOfferStatus.setBackgroundResource(R.drawable.circle_green);
                    layoutActionButtons.setVisibility(View.GONE);
                    
                    // Cerrar actividad después de un breve delay
                    findViewById(android.R.id.content).postDelayed(() -> finish(), 1500);
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(TourOfferDetailsActivity.this, 
                        "Error al aceptar oferta: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void rejectOffer() {
        if (currentOffer == null || currentOffer.getOfferId() == null) {
            Toast.makeText(this, "Error: Datos de oferta no disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        firestoreManager.updateOfferStatus(currentOffer.getOfferId(), "RECHAZADA", 
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Toast.makeText(TourOfferDetailsActivity.this, 
                        "Oferta rechazada", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Actualizar UI
                    tvOfferStatus.setText("RECHAZADA");
                    tvOfferStatus.setBackgroundResource(R.drawable.circle_red);
                    layoutActionButtons.setVisibility(View.GONE);
                    
                    // Cerrar actividad después de un breve delay
                    findViewById(android.R.id.content).postDelayed(() -> finish(), 1500);
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(TourOfferDetailsActivity.this, 
                        "Error al rechazar oferta: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
}

