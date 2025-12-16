package com.example.droidtour;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.models.Tour;
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
                     tvTourTime, tvTourDuration, tvParticipants, tvPaymentAmount, tvAdditionalNotes;
    private ChipGroup chipGroupLanguages;
    private MaterialButton btnAccept, btnReject, btnViewFullMap;
    private LinearLayout layoutActionButtons;
    private com.google.android.material.card.MaterialCardView cardNotes;
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
        tvAdditionalNotes = findViewById(R.id.tv_additional_notes);
        chipGroupLanguages = findViewById(R.id.chip_group_languages);
        cardNotes = findViewById(R.id.card_notes);
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
        String additionalNotes = getIntent().getStringExtra("additionalNotes");

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
        
        // Mostrar notas adicionales si existen
        if (additionalNotes != null && !additionalNotes.trim().isEmpty()) {
            tvAdditionalNotes.setText(additionalNotes);
            cardNotes.setVisibility(View.VISIBLE);
        } else {
            cardNotes.setVisibility(View.GONE);
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

        // Paso 1: Verificar si ya tiene un tour en la misma fecha
        String guideId = currentOffer.getGuideId();
        String offerDate = currentOffer.getTourDate();
        
        firestoreManager.getToursByGuide(guideId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> existingTours = (List<Tour>) result;
                
                // Verificar conflicto de fechas
                for (Tour tour : existingTours) {
                    if (offerDate != null && offerDate.equals(tour.getTourDate())) {
                        String status = tour.getTourStatus();
                        if ("EN_PROGRESO".equals(status) || "CONFIRMADA".equals(status)) {
                            Toast.makeText(TourOfferDetailsActivity.this, 
                                "❌ Ya tienes un tour aceptado para el " + offerDate, 
                                Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
                
                Log.d(TAG, "✅ Validación de fechas OK - No hay conflictos");
                // No hay conflicto, proceder a aceptar
                updateOfferAndAssignTour();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(TourOfferDetailsActivity.this, 
                    "Error verificando disponibilidad: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateOfferAndAssignTour() {
        Log.d(TAG, "🔄 PASO 1: Marcando oferta como ACEPTADA");
        // Paso 1: Actualizar estado de la oferta
        firestoreManager.updateOfferStatus(currentOffer.getOfferId(), "ACEPTADA", 
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "✅ PASO 1: Oferta marcada como ACEPTADA");
                    // Paso 2: Asignar guía al tour
                    assignGuideToTour();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(TourOfferDetailsActivity.this, 
                        "Error al aceptar oferta: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void assignGuideToTour() {
        String tourId = currentOffer.getTourId();
        String guideId = currentOffer.getGuideId();
        
        Log.d(TAG, "🔄 PASO 2: Obteniendo tours del guía y datos del tour");
        
        // Obtener el tour completo para acceder a startTime y endTime
        firestoreManager.getTourById(tourId, new FirestoreManager.TourCallback() {
            @Override
            public void onSuccess(Tour newTour) {
                if (newTour == null) {
                    Toast.makeText(TourOfferDetailsActivity.this, "Error: Tour no encontrado", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Obtener tours existentes del guía para determinar status
                firestoreManager.getToursByGuide(guideId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        List<Tour> existingTours = (List<Tour>) result;
                        Log.d(TAG, "✅ PASO 2: Tours obtenidos: " + existingTours.size());
                        
                        String tourStatus = determineTourStatus(existingTours, newTour);
                        Log.d(TAG, "📊 Status calculado: " + tourStatus);
                
                // Actualizar el Tour con los datos del guía
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("assignedGuideId", guideId);
                updates.put("assignedGuideName", currentOffer.getGuideName());
                updates.put("tourStatus", tourStatus);
                updates.put("guidePayment", currentOffer.getPaymentAmount());
                updates.put("isPublic", true);
                
                Log.d(TAG, "🔄 PASO 3: Actualizando Tour en Firestore (NO Reservation)");
                Log.d(TAG, "   → Tour ID: " + tourId);
                Log.d(TAG, "   → assignedGuideId: " + guideId);
                Log.d(TAG, "   → assignedGuideName: " + currentOffer.getGuideName());
                Log.d(TAG, "   → tourStatus: " + tourStatus);
                
                firestoreManager.updateTour(tourId, updates, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "✅✅✅ PASO 3 COMPLETADO: Tour actualizado exitosamente ✅✅✅");
                        Log.d(TAG, "   ✅ assignedGuideId guardado: " + guideId);
                        Log.d(TAG, "   ✅ NO se creó Reservation");
                        Log.d(TAG, "   ✅ Verificar en Firestore -> Tours/" + tourId);
                        
                        // Si esta es la más cercana, actualizar los demás tours EN_PROGRESO a CONFIRMADA
                        if ("EN_PROGRESO".equals(tourStatus)) {
                            Log.d(TAG, "🔄 PASO 4: Actualizando otros tours");
                            updateOtherToursToConfirmed(guideId, tourId);
                        }
                        
                        Toast.makeText(TourOfferDetailsActivity.this, 
                            "✅ Oferta aceptada - Tour agregado a Mis Tours", 
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
                        android.util.Log.e("TourOfferDetails", "❌ Error actualizando tour", e);
                        Toast.makeText(TourOfferDetailsActivity.this, 
                            "Error al actualizar tour: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                });
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        android.util.Log.e("TourOfferDetails", "Error obteniendo tours del guía", e);
                        Toast.makeText(TourOfferDetailsActivity.this, 
                            "Error verificando tours: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(String error) {
                android.util.Log.e("TourOfferDetails", "Error obteniendo tour: " + error);
                Toast.makeText(TourOfferDetailsActivity.this, 
                    "Error obteniendo datos del tour: " + error, 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Determina el status de un tour usando fecha, hora de inicio y hora de fin para comparaciones precisas
     */
    private String determineTourStatus(List<Tour> existingTours, Tour newTour) {
        if (newTour == null || newTour.getTourDate() == null) {
            return "CONFIRMADA";
        }
        
        // Si no hay tours existentes, verificar si está completado antes de asignar EN_PROGRESO
        if (existingTours == null || existingTours.isEmpty()) {
            if (isTourCompleted(newTour.getTourDate(), newTour.getStartTime(), newTour.getEndTime())) {
                return "COMPLETADA";
            }
            return "EN_PROGRESO";
        }
        
        try {
            // Verificar si el nuevo tour ya está completado
            if (isTourCompleted(newTour.getTourDate(), newTour.getStartTime(), newTour.getEndTime())) {
                return "COMPLETADA";
            }
            
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            
            java.util.Date newDate = dateFormat.parse(newTour.getTourDate());
            java.util.Calendar todayCal = java.util.Calendar.getInstance();
            java.util.Date today = todayCal.getTime();
            
            // Normalizar fechas a medianoche para comparación de fechas
            java.util.Calendar newDateCal = java.util.Calendar.getInstance();
            newDateCal.setTime(newDate);
            newDateCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            newDateCal.set(java.util.Calendar.MINUTE, 0);
            newDateCal.set(java.util.Calendar.SECOND, 0);
            newDateCal.set(java.util.Calendar.MILLISECOND, 0);
            
            java.util.Calendar todayCalNormalized = java.util.Calendar.getInstance();
            todayCalNormalized.setTime(today);
            todayCalNormalized.set(java.util.Calendar.HOUR_OF_DAY, 0);
            todayCalNormalized.set(java.util.Calendar.MINUTE, 0);
            todayCalNormalized.set(java.util.Calendar.SECOND, 0);
            todayCalNormalized.set(java.util.Calendar.MILLISECOND, 0);
            
            // Si la fecha ya pasó completamente (antes de hoy), es COMPLETADA
            if (newDateCal.before(todayCalNormalized)) {
                return "COMPLETADA";
            }
            
            // Buscar el tour más cercano entre todos los tours activos
            Tour closestTour = null;
            java.util.Date closestDate = null;
            
            for (Tour tour : existingTours) {
                String status = tour.getTourStatus();
                if ("EN_PROGRESO".equals(status) || "CONFIRMADA".equals(status)) {
                    try {
                        if (tour.getTourDate() != null) {
                            java.util.Date existingDate = dateFormat.parse(tour.getTourDate());
                            
                            // Normalizar a medianoche
                            java.util.Calendar existingCal = java.util.Calendar.getInstance();
                            existingCal.setTime(existingDate);
                            existingCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                            existingCal.set(java.util.Calendar.MINUTE, 0);
                            existingCal.set(java.util.Calendar.SECOND, 0);
                            existingCal.set(java.util.Calendar.MILLISECOND, 0);
                            existingDate = existingCal.getTime();
                            
                            // Solo considerar tours futuros
                            if (existingDate.after(todayCalNormalized.getTime()) || 
                                existingDate.equals(todayCalNormalized.getTime())) {
                                
                                if (closestDate == null || existingDate.before(closestDate)) {
                                    closestDate = existingDate;
                                    closestTour = tour;
                                }
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("TourOfferDetails", "Error parseando fecha: " + tour.getTourDate());
                    }
                }
            }
            
            // Comparar el nuevo tour con el más cercano
            if (closestDate != null) {
                // Si hay un tour más cercano, este nuevo tour es CONFIRMADA
                if (closestDate.before(newDateCal.getTime())) {
                    return "CONFIRMADA";
                }
                // Si tienen la misma fecha, comparar por hora de inicio
                if (closestDate.equals(newDateCal.getTime())) {
                    String newStartTime = newTour.getStartTime();
                    String closestStartTime = closestTour != null ? closestTour.getStartTime() : null;
                    
                    if (newStartTime != null && closestStartTime != null) {
                        try {
                            java.util.Date newStartTimeObj = timeFormat.parse(newStartTime);
                            java.util.Date closestStartTimeObj = timeFormat.parse(closestStartTime);
                            
                            // Si el tour existente empieza antes, el nuevo es CONFIRMADA
                            if (closestStartTimeObj.before(newStartTimeObj)) {
                                return "CONFIRMADA";
                            }
                        } catch (Exception e) {
                            android.util.Log.e("TourOfferDetails", "Error parseando hora de inicio", e);
                        }
                    }
                }
            }
            
            // Esta es la fecha más cercana, verificar si debe ser EN_PROGRESO
            // Un tour está EN_PROGRESO si es hoy y ya pasó la hora de inicio pero no la de fin
            if (newDateCal.equals(todayCalNormalized)) {
                if (isTourInProgress(newTour.getStartTime(), newTour.getEndTime())) {
                    return "EN_PROGRESO";
                }
            }
            
            // Si es futuro, puede ser EN_PROGRESO si es el más cercano
            return "EN_PROGRESO";
            
        } catch (Exception e) {
            android.util.Log.e("TourOfferDetails", "Error determinando status", e);
            return "CONFIRMADA";
        }
    }
    
    /**
     * Verifica si un tour ya está completado (fecha pasó Y hora de fin pasó)
     */
    private boolean isTourCompleted(String tourDate, String startTime, String endTime) {
        if (tourDate == null) return false;
        
        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.util.Date tourDateObj = dateFormat.parse(tourDate);
            
            java.util.Calendar todayCal = java.util.Calendar.getInstance();
            java.util.Calendar tourCal = java.util.Calendar.getInstance();
            tourCal.setTime(tourDateObj);
            
            // Normalizar ambas fechas a medianoche
            todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            todayCal.set(java.util.Calendar.MINUTE, 0);
            todayCal.set(java.util.Calendar.SECOND, 0);
            todayCal.set(java.util.Calendar.MILLISECOND, 0);
            
            tourCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            tourCal.set(java.util.Calendar.MINUTE, 0);
            tourCal.set(java.util.Calendar.SECOND, 0);
            tourCal.set(java.util.Calendar.MILLISECOND, 0);
            
            // Si la fecha ya pasó completamente, está completado
            if (tourCal.before(todayCal)) {
                return true;
            }
            
            // Si es hoy, verificar si ya pasó la hora de fin
            if (tourCal.equals(todayCal) && endTime != null && !endTime.isEmpty()) {
                try {
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                    java.util.Date endTimeObj = timeFormat.parse(endTime);
                    
                    java.util.Calendar endCal = java.util.Calendar.getInstance();
                    endCal.setTime(tourDateObj);
                    endCal.set(java.util.Calendar.HOUR_OF_DAY, endTimeObj.getHours());
                    endCal.set(java.util.Calendar.MINUTE, endTimeObj.getMinutes());
                    endCal.set(java.util.Calendar.SECOND, 0);
                    endCal.set(java.util.Calendar.MILLISECOND, 0);
                    
                    java.util.Calendar nowCal = java.util.Calendar.getInstance();
                    
                    // Si ya pasó la hora de fin, está completado
                    return nowCal.after(endCal);
                } catch (Exception e) {
                    android.util.Log.e("TourOfferDetails", "Error parseando hora de fin: " + endTime, e);
                }
            }
            
            return false;
        } catch (Exception e) {
            android.util.Log.e("TourOfferDetails", "Error verificando si tour está completado", e);
            return false;
        }
    }
    
    /**
     * Verifica si un tour está actualmente en progreso (hoy Y hora actual >= startTime Y hora actual < endTime)
     */
    private boolean isTourInProgress(String startTime, String endTime) {
        if (startTime == null || endTime == null) return false;
        
        try {
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            java.util.Date startTimeObj = timeFormat.parse(startTime);
            java.util.Date endTimeObj = timeFormat.parse(endTime);
            
            java.util.Calendar nowCal = java.util.Calendar.getInstance();
            java.util.Calendar startCal = java.util.Calendar.getInstance();
            java.util.Calendar endCal = java.util.Calendar.getInstance();
            
            startCal.set(java.util.Calendar.HOUR_OF_DAY, startTimeObj.getHours());
            startCal.set(java.util.Calendar.MINUTE, startTimeObj.getMinutes());
            startCal.set(java.util.Calendar.SECOND, 0);
            startCal.set(java.util.Calendar.MILLISECOND, 0);
            
            endCal.set(java.util.Calendar.HOUR_OF_DAY, endTimeObj.getHours());
            endCal.set(java.util.Calendar.MINUTE, endTimeObj.getMinutes());
            endCal.set(java.util.Calendar.SECOND, 0);
            endCal.set(java.util.Calendar.MILLISECOND, 0);
            
            // Está en progreso si: hora actual >= hora inicio Y hora actual < hora fin
            return (nowCal.after(startCal) || nowCal.equals(startCal)) && nowCal.before(endCal);
            
        } catch (Exception e) {
            android.util.Log.e("TourOfferDetails", "Error verificando si tour está en progreso", e);
            return false;
        }
    }
    
    private void updateOtherToursToConfirmed(String guideId, String currentTourId) {
        firestoreManager.getToursByGuide(guideId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> tours = (List<Tour>) result;
                for (Tour tour : tours) {
                    if ("EN_PROGRESO".equals(tour.getTourStatus()) && 
                        !currentTourId.equals(tour.getTourId())) {
                        // Actualizar a CONFIRMADA
                        java.util.Map<String, Object> updates = new java.util.HashMap<>();
                        updates.put("tourStatus", "CONFIRMADA");
                        firestoreManager.updateTour(tour.getTourId(), updates, 
                            new FirestoreManager.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    android.util.Log.d("TourOfferDetails", "Tour actualizado a CONFIRMADA");
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    android.util.Log.e("TourOfferDetails", "Error actualizando tour", e);
                                }
                            });
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("TourOfferDetails", "Error obteniendo tours", e);
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

