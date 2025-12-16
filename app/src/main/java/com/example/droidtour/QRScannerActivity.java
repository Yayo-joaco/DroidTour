package com.example.droidtour;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.managers.QRScannerManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.mlkit.vision.barcode.common.Barcode;

public class QRScannerActivity extends AppCompatActivity implements QRScannerManager.QRScanCallback {

    private static final String TAG = "QRScannerActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    // Views
    private TextView tvScanStatus, tvTourName, tvParticipantCount, tvCheckInOutStatus;
    private TextView tvEmptyHistory;
    private MaterialButton btnContinueScanning, btnToggleFlash, btnFinishCheckIn;
    private MaterialCardView cardScanResult;
    private PreviewView previewView;
    private android.widget.LinearLayout layoutScannedList;
    // Managers
    private PreferencesManager prefsManager;
    private QRScannerManager qrScannerManager;
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;

    // Tour info
    private String tourId;
    private com.example.droidtour.models.Tour currentTour;
    private String scanType; // "CHECK_IN" o "CHECK_OUT"
    private int expectedParticipants = 0;
    private int scannedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar PreferencesManager y FirestoreManager
        prefsManager = new PreferencesManager(this);
        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();

        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }

        // Validar que el usuario sea un guía
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            redirectToLogin();
            finish();
            return;
        }

        setContentView(R.layout.activity_qr_scanner);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        initializeViews();
        setupToolbar();
        setupClickListeners();

        // Obtener datos del tour desde el intent
        loadTourData();

        // Solicitar permisos de cámara
        if (checkCameraPermission()) {
            initializeCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void initializeViews() {
        tvScanStatus = findViewById(R.id.tv_scan_status);
        tvTourName = findViewById(R.id.tv_tour_name);
        tvParticipantCount = findViewById(R.id.tv_participants_count);
        tvCheckInOutStatus = findViewById(R.id.tv_checkinout_status);
        tvEmptyHistory = findViewById(R.id.tv_empty_history);
        btnContinueScanning = findViewById(R.id.btn_continue_scanning);
        btnToggleFlash = findViewById(R.id.btn_toggle_flash);
        btnFinishCheckIn = findViewById(R.id.btn_finish_checkin);
        cardScanResult = findViewById(R.id.card_scan_result);
        layoutScannedList = findViewById(R.id.layout_scanned_list);

        // Reemplazar el contenedor placeholder con PreviewView
        replaceCameraPlaceholder();
    }

    private void replaceCameraPlaceholder() {
        MaterialCardView cameraContainer = findViewById(R.id.layout_camera_preview);
        if (cameraContainer != null) {
            cameraContainer.removeAllViews();

            previewView = new PreviewView(this);
            previewView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
            ));
            previewView.setBackgroundColor(Color.BLACK);
            cameraContainer.addView(previewView);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnToggleFlash.setOnClickListener(v -> toggleFlash());
        btnContinueScanning.setOnClickListener(v -> continueScanning());
        
        if (btnFinishCheckIn != null) {
            btnFinishCheckIn.setOnClickListener(v -> showFinishCheckInDialog());
        }
    }

    private void loadTourData() {
        Intent intent = getIntent();
        if (intent != null) {
            tourId = intent.getStringExtra("tour_id");
            scanType = intent.getStringExtra("SCAN_TYPE"); // "CHECK_IN" o "CHECK_OUT"
            
            if (scanType == null) scanType = "CHECK_IN";
            
            if (tourId != null && !tourId.isEmpty()) {
                loadTourFromFirestore();
            } else {
                Toast.makeText(this, "Error: ID del tour no disponible", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private void loadTourFromFirestore() {
        firestoreManager.getTourById(tourId, new com.example.droidtour.firebase.FirestoreManager.TourCallback() {
            @Override
            public void onSuccess(com.example.droidtour.models.Tour tour) {
                currentTour = tour;
                displayTourInfo();
            }
            
            @Override
            public void onFailure(String error) {
                Toast.makeText(QRScannerActivity.this, "Error al cargar tour: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void displayTourInfo() {
        if (currentTour == null) return;
        
        tvTourName.setText(currentTour.getTourName());
        
        String status = currentTour.getCheckInOutStatus();
        if (status == null) status = "ESPERANDO_CHECKIN";

        // Si el tour ya está esperando check-out o finalizado, forzar modo check-out
        if ("ESPERANDO_CHECKOUT".equals(status) || "CHECKOUT_COMPLETADO".equals(status)) {
            scanType = "CHECK_OUT";
        }

        int tourExpected = currentTour.getExpectedParticipants() != null ? currentTour.getExpectedParticipants() : 0;
        int checkedIn = currentTour.getCheckedInCount() != null ? currentTour.getCheckedInCount() : 0;

        expectedParticipants = "CHECK_OUT".equals(scanType) ? checkedIn : tourExpected;
        scannedCount = "CHECK_IN".equals(scanType) ? 
            checkedIn :
            (currentTour.getCheckedOutCount() != null ? currentTour.getCheckedOutCount() : 0);
        
        updateParticipantCount();
        updateStatusDisplay();
        updateScanHistory();
        
        // Mostrar u ocultar botón "Finalizar Check-In" según el tipo de escaneo
        if (btnFinishCheckIn != null) {
            btnFinishCheckIn.setVisibility(
                "CHECK_IN".equals(scanType) && "ESPERANDO_CHECKIN".equals(status) ? View.VISIBLE : View.GONE
            );
        }
    }

    private void updateScanHistory() {
        if (layoutScannedList == null || tvEmptyHistory == null) {
            Log.w(TAG, "updateScanHistory: vistas son null");
            return;
        }
        
        java.util.List<com.example.droidtour.models.Tour.ScannedParticipant> participants = 
            (currentTour != null && currentTour.getScannedParticipants() != null) ? 
            currentTour.getScannedParticipants() : 
            new java.util.ArrayList<>();
        
        Log.d(TAG, "Actualizando historial con " + participants.size() + " participantes");
        
        // Limpiar lista anterior
        layoutScannedList.removeAllViews();
        
        if (participants.isEmpty()) {
            // Mostrar mensaje de vacío
            tvEmptyHistory.setVisibility(View.VISIBLE);
            layoutScannedList.setVisibility(View.GONE);
        } else {
            // Ocultar mensaje y mostrar lista
            tvEmptyHistory.setVisibility(View.GONE);
            layoutScannedList.setVisibility(View.VISIBLE);
            
            // Agregar cada participante como una fila
            for (com.example.droidtour.models.Tour.ScannedParticipant participant : participants) {
                addParticipantRow(participant);
            }
        }
    }
    
    private void addParticipantRow(com.example.droidtour.models.Tour.ScannedParticipant participant) {
        // Crear LinearLayout horizontal para cada participante
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dpToPx(10));
        
        // TextView para el nombre (label)
        TextView tvLabel = new TextView(this);
        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
            dpToPx(92),
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tvLabel.setLayoutParams(labelParams);
        tvLabel.setText(participant.getUserName() != null ? participant.getUserName() : "Participante");
        tvLabel.setTextSize(13);
        tvLabel.setTextColor(0xFF6B7280);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // TextView para el estado
        TextView tvStatus = new TextView(this);
        android.widget.LinearLayout.LayoutParams statusParams = new android.widget.LinearLayout.LayoutParams(
            0,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        tvStatus.setLayoutParams(statusParams);
        
        // Determinar el estado
        String statusText = "";
        int statusColor = 0xFF202124;
        
        if (participant.getHasCheckedOut() != null && participant.getHasCheckedOut()) {
            statusText = "✓ Check-out completado";
            statusColor = 0xFF4CAF50; // verde
        } else if (participant.getHasCheckedIn() != null && participant.getHasCheckedIn()) {
            statusText = "✓ Check-in completado";
            statusColor = 0xFF2196F3; // azul
        } else {
            statusText = "⏳ Pendiente";
            statusColor = 0xFFFF9800; // naranja
        }
        
        tvStatus.setText(statusText);
        tvStatus.setTextSize(13);
        tvStatus.setTextColor(statusColor);
        
        row.addView(tvLabel);
        row.addView(tvStatus);
        
        layoutScannedList.addView(row);
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private void updateStatusDisplay() {
        if (currentTour == null) return;
        
        String status = currentTour.getCheckInOutStatus();
        if (status == null) status = "ESPERANDO_CHECKIN";
        
        // Si ya estamos en modo check-out, asegurar que el texto refleje ese estado
        if ("CHECK_OUT".equals(scanType) && "ESPERANDO_CHECKIN".equals(status)) {
            status = "ESPERANDO_CHECKOUT";
        }
        
        // Actualizar estado
        switch (status) {
            case "ESPERANDO_CHECKIN":
                if (tvScanStatus != null) {
                    tvScanStatus.setText("Escanea QR de Check-In");
                    tvScanStatus.setTextColor(getColor(R.color.orange));
                }
                break;
            case "ESPERANDO_CHECKOUT":
                if (tvScanStatus != null) {
                    tvScanStatus.setText("Escanea QR de Check-Out");
                    tvScanStatus.setTextColor(getColor(R.color.primary));
                }
                break;
            case "CHECKOUT_COMPLETADO":
                if (tvScanStatus != null) {
                    tvScanStatus.setText("Tour Completado");
                    tvScanStatus.setTextColor(getColor(R.color.green));
                }
                break;
        }
        
        // Actualizar progreso (conteo de participantes)
        if (tvCheckInOutStatus != null) {
            String progressText = scannedCount + " / " + expectedParticipants;
            if ("CHECK_IN".equals(scanType)) {
                progressText += " check-ins";
            } else {
                progressText += " check-outs";
            }
            tvCheckInOutStatus.setText(progressText);
        }
        
        // Mostrar/ocultar bot??n Finalizar Check-In
        if (btnFinishCheckIn != null) {
            btnFinishCheckIn.setVisibility(
                "CHECK_IN".equals(scanType) && "ESPERANDO_CHECKIN".equals(status) ? View.VISIBLE : View.GONE
            );
        }
    }

    private void initializeCamera() {
        if (previewView != null) {
            qrScannerManager = new QRScannerManager(this, previewView, this);
            qrScannerManager.startCamera(this);
        } else {
            Toast.makeText(this, "Error: Vista de cámara no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== QRScannerManager.QRScanCallback ====================

    @Override
    public void onQRScanned(String qrData, Barcode barcode) {
        runOnUiThread(() -> {
            Log.d(TAG, "QR escaneado: " + qrData);
            
            // Pausar escaneo mientras procesamos
            if (qrScannerManager != null) {
                qrScannerManager.pauseScanning();
            }
            
            // Validar y procesar QR
            validateAndProcessQR(qrData);
        });
    }

    @Override
    public void onScanError(Exception e) {
        runOnUiThread(() -> {
            Log.e(TAG, "Error de escaneo", e);
            Toast.makeText(this, "Error al iniciar escáner: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    // ==================== UI Updates ====================

    private void updateParticipantCount() {
        tvParticipantCount.setText(expectedParticipants + " participantes");
    }





    private void showScanResultCard(String clientName, boolean success) {
        cardScanResult.setVisibility(View.VISIBLE);

        TextView tvResultTitle = findViewById(R.id.tv_scan_result_title);
        TextView tvResultMessage = findViewById(R.id.tv_scan_result_message);
        TextView tvClientName = findViewById(R.id.tv_client_name);
        ImageView ivResultIcon = findViewById(R.id.iv_scan_result_icon);

        if (success) {
            if (tvResultTitle != null) {
                tvResultTitle.setText("¡Check-in Exitoso!");
                tvResultTitle.setTextColor(getColor(R.color.green));
            }

            if (tvResultMessage != null) {
                tvResultMessage.setText("Cliente registrado correctamente");
            }

            if (ivResultIcon != null) {
                ivResultIcon.setImageResource(R.drawable.ic_check_circle);
                ivResultIcon.setColorFilter(getColor(R.color.green));
            }
        } else {
            if (tvResultTitle != null) {
                tvResultTitle.setText("Error en Check-in");
                tvResultTitle.setTextColor(getColor(R.color.red));
            }

            if (tvResultMessage != null) {
                tvResultMessage.setText("No se pudo registrar el cliente");
            }

            if (ivResultIcon != null) {
                ivResultIcon.setImageResource(R.drawable.ic_error);
                ivResultIcon.setColorFilter(getColor(R.color.red));
            }
        }

        if (tvClientName != null) {
            tvClientName.setText(clientName);
        }
    }

    // ==================== Actions ====================

    private void showFinishCheckInDialog() {
        if (currentTour == null) return;
        
        int missing = expectedParticipants - scannedCount;
        
        String message = missing > 0 ? 
            "¿Estás seguro? Faltan " + missing + " persona(s) por escanear." :
            "¿Confirmar finalización del check-in?";
        
        new AlertDialog.Builder(this)
            .setTitle("Finalizar Check-In")
            .setMessage(message)
            .setPositiveButton("Sí, finalizar", (dialog, which) -> finishCheckInProcess())
            .setNegativeButton("Cancelar", null)
            .show();
    }
    
    private void finishCheckInProcess() {
        if (currentTour == null) return;
        
        // Actualizar expectedParticipants al scannedCount actual
        currentTour.setExpectedParticipants(scannedCount);
        expectedParticipants = scannedCount;
        
        // Cambiar estado del tour a ESPERANDO_CHECKOUT y pasar a modo check-out
        currentTour.setCheckInOutStatus("ESPERANDO_CHECKOUT");
        scanType = "CHECK_OUT";
        
        firestoreManager.updateTour(currentTour, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(QRScannerActivity.this, "Check-In finalizado. Ahora puedes hacer Check-Out.", Toast.LENGTH_LONG).show();
                // Reiniciar conteo para check-out
                scannedCount = currentTour.getCheckedOutCount() != null ? currentTour.getCheckedOutCount() : 0;
                expectedParticipants = currentTour.getCheckedInCount() != null ? currentTour.getCheckedInCount() : expectedParticipants;
                updateParticipantCount();
                updateStatusDisplay();
                if (btnFinishCheckIn != null) {
                    btnFinishCheckIn.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(QRScannerActivity.this, "Error al finalizar check-in", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void toggleFlash() {
        if (qrScannerManager != null) {
            if (qrScannerManager.hasFlash()) {
                boolean flashOn = qrScannerManager.toggleFlash();

                // Actualizar icono del botón
                btnToggleFlash.setIconResource(
                        flashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off
                );

                Toast.makeText(this,
                        flashOn ? "Flash activado" : "Flash desactivado",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Este dispositivo no tiene flash",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Cámara no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void continueScanning() {
        cardScanResult.setVisibility(View.GONE);
        if ("CHECK_OUT".equals(scanType)) {
            tvScanStatus.setText("Esperando QR de Check-Out");
            tvScanStatus.setTextColor(getColor(R.color.primary));
        } else {
            tvScanStatus.setText("Esperando QR de Check-in");
            tvScanStatus.setTextColor(getColor(R.color.orange));
        }
        
        if (qrScannerManager != null) {
            qrScannerManager.resumeScanning();
        }
        
        Toast.makeText(this, "Listo para escanear siguiente QR",
                Toast.LENGTH_SHORT).show();
    }

    private boolean areStopsCompleted() {
        if (currentTour == null || currentTour.getStops() == null || currentTour.getStops().isEmpty()) {
            return true;
        }
        for (com.example.droidtour.models.Tour.TourStop stop : currentTour.getStops()) {
            if (stop.getCompleted() == null || !stop.getCompleted()) {
                return false;
            }
        }
        return true;
    }
    
    // ==================== QR Validation ====================
    
    private void validateAndProcessQR(String qrData) {
        // Parsear QR: Formato esperado CHECKIN-{reservationId}-{tourId} o CHECKOUT-{reservationId}-{tourId}
        String[] parts = qrData.split("-");
        
        if (parts.length < 3) {
            showScanError("QR inválido", "El código QR no tiene el formato correcto");
            return;
        }
        
        String qrType = parts[0]; // "CHECKIN" o "CHECKOUT"
        String reservationId = parts[1];
        String qrTourId = parts[2];
        
        // Validar que el QR sea del tipo correcto
        if ("CHECK_IN".equals(scanType) && !"CHECKIN".equals(qrType)) {
            showScanError("QR incorrecto", "Este es un QR de check-out. Necesitas escanear un QR de check-in.");
            return;
        }
        
        if ("CHECK_OUT".equals(scanType) && !"CHECKOUT".equals(qrType)) {
            showScanError("QR incorrecto", "Este es un QR de check-in. Necesitas escanear un QR de check-out.");
            return;
        }
        
        // Validar que el QR sea del tour correcto
        if (!qrTourId.equals(tourId)) {
            showScanError("QR de otro tour", "Este QR pertenece a otro tour");
            return;
        }

        // Para check-out, asegurar que las paradas esten completas
        if ("CHECK_OUT".equals(scanType) && !areStopsCompleted()) {
            showScanError("Paradas pendientes", "Marca todas las paradas como completadas antes de hacer check-out.");
            return;
        }
        
        // Buscar la reserva en Firestore
        validateReservation(reservationId, qrData);
    }
    
    private void validateReservation(String reservationId, String qrData) {
        firestoreManager.getReservationById(reservationId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.Reservation reservation = (com.example.droidtour.models.Reservation) result;
                
                if (reservation == null) {
                    showScanError("Reserva no encontrada", "No existe una reserva con este código");
                    return;
                }
                
                // Validar que la reserva esté confirmada
                if (!"CONFIRMADA".equals(reservation.getStatus())) {
                    showScanError("Reserva no confirmada", "Esta reserva no está confirmada");
                    return;
                }
                
                // Validar que la reserva sea del tour correcto
                if (!tourId.equals(reservation.getTourId())) {
                    showScanError("Reserva de otro tour", "Esta reserva no pertenece a este tour");
                    return;
                }
                
                // Procesar según el tipo de escaneo
                if ("CHECK_IN".equals(scanType)) {
                    processCheckIn(reservation, qrData);
                } else {
                    processCheckOut(reservation, qrData);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                showScanError("Error de conexión", "No se pudo validar la reserva: " + e.getMessage());
            }
        });
    }
    
    private void processCheckIn(com.example.droidtour.models.Reservation reservation, String qrData) {
        // Verificar si ya hizo check-in
        if (reservation.getHasCheckedIn() != null && reservation.getHasCheckedIn()) {
            showScanError("Ya registrado", reservation.getUserName() + " ya hizo check-in");
            return;
        }
        
        // Registrar check-in en la reserva
        reservation.setHasCheckedIn(true);
        reservation.setCheckInTime(new java.util.Date());
        
        firestoreManager.updateReservation(reservation, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Agregar participante a la lista del tour
                addParticipantToTour(reservation, true, false);
            }
            
            @Override
            public void onFailure(Exception e) {
                showScanError("Error al registrar", "No se pudo guardar el check-in");
            }
        });
    }
    
    private void processCheckOut(com.example.droidtour.models.Reservation reservation, String qrData) {
        // Verificar si hizo check-in primero
        if (reservation.getHasCheckedIn() == null || !reservation.getHasCheckedIn()) {
            showScanError("Sin check-in", reservation.getUserName() + " no ha hecho check-in todavía");
            return;
        }
        
        // Verificar si ya hizo check-out
        if (reservation.getHasCheckedOut() != null && reservation.getHasCheckedOut()) {
            showScanError("Ya registrado", reservation.getUserName() + " ya hizo check-out");
            return;
        }
        
        // Registrar check-out en la reserva
        reservation.setHasCheckedOut(true);
        reservation.setCheckOutTime(new java.util.Date());
        
        firestoreManager.updateReservation(reservation, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Actualizar participante en la lista del tour
                addParticipantToTour(reservation, true, true);
            }
            
            @Override
            public void onFailure(Exception e) {
                showScanError("Error al registrar", "No se pudo guardar el check-out");
            }
        });
    }
    
    private void addParticipantToTour(com.example.droidtour.models.Reservation reservation, boolean checkedIn, boolean checkedOut) {
        if (currentTour == null) return;
        
        String actionType = scanType;
        
        // Inicializar lista si es null
        if (currentTour.getScannedParticipants() == null) {
            currentTour.setScannedParticipants(new java.util.ArrayList<>());
        }
        
        // Buscar si el participante ya existe en la lista
        com.example.droidtour.models.Tour.ScannedParticipant participant = null;
        for (com.example.droidtour.models.Tour.ScannedParticipant p : currentTour.getScannedParticipants()) {
            if (p.getReservationId().equals(reservation.getReservationId())) {
                participant = p;
                break;
            }
        }
        
        // Si no existe, crear nuevo
        if (participant == null) {
            participant = new com.example.droidtour.models.Tour.ScannedParticipant(
                reservation.getReservationId(),
                reservation.getUserId(),
                reservation.getUserName()
            );
            currentTour.getScannedParticipants().add(participant);
        }
        
        // Actualizar estados
        participant.setHasCheckedIn(checkedIn);
        participant.setHasCheckedOut(checkedOut);
        
        if (checkedIn && participant.getCheckInTime() == null) {
            participant.setCheckInTime(reservation.getCheckInTime());
        }
        if (checkedOut && participant.getCheckOutTime() == null) {
            participant.setCheckOutTime(reservation.getCheckOutTime());
        }
        
        // Actualizar contadores
        if ("CHECK_IN".equals(actionType)) {
            currentTour.setCheckedInCount((currentTour.getCheckedInCount() != null ? currentTour.getCheckedInCount() : 0) + 1);
            scannedCount = currentTour.getCheckedInCount();
        } else {
            currentTour.setCheckedOutCount((currentTour.getCheckedOutCount() != null ? currentTour.getCheckedOutCount() : 0) + 1);
            scannedCount = currentTour.getCheckedOutCount();
        }
        
        // Verificar si todos completaron para cambiar estado automáticamente
        checkAndUpdateTourStatus();
        
        // Guardar cambios en Firestore
        firestoreManager.updateTour(currentTour, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Actualizar historial
                updateScanHistory();
                // Mostrar éxito
                handleSuccessfulScan(reservation.getUserName(), actionType);
            }
            
            @Override
            public void onFailure(Exception e) {
                showScanError("Error al actualizar", "No se pudo actualizar el tour");
            }
        });
    }

    private void checkAndUpdateTourStatus() {
        if (currentTour == null) return;
        
        // Si todos hicieron check-in, cambiar a ESPERANDO_CHECKOUT y preparar modo check-out
        if ("CHECK_IN".equals(scanType) && scannedCount >= expectedParticipants) {
            currentTour.setCheckInOutStatus("ESPERANDO_CHECKOUT");
            scanType = "CHECK_OUT";
            expectedParticipants = currentTour.getCheckedInCount() != null ? currentTour.getCheckedInCount() : expectedParticipants;
            scannedCount = currentTour.getCheckedOutCount() != null ? currentTour.getCheckedOutCount() : 0;
            if (btnFinishCheckIn != null) {
                btnFinishCheckIn.setVisibility(View.GONE);
            }
        }
        
        int checkedInTotal = currentTour.getCheckedInCount() != null ? currentTour.getCheckedInCount() : 0;
        
        // Si todos hicieron check-out, cambiar a CHECKOUT_COMPLETADO y tourStatus a COMPLETADA
        if ("CHECK_OUT".equals(scanType) && checkedInTotal > 0 && scannedCount >= checkedInTotal) {
            currentTour.setCheckInOutStatus("CHECKOUT_COMPLETADO");
            currentTour.setTourStatus("COMPLETADA");
            // Generar notificación al admin
            generateAdminPaymentNotification();
        }
    }

    private void showScanError(String title, String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, title + ": " + message, Toast.LENGTH_LONG).show();
            showScanResultCard(title + " - " + message, false);
            
            // Reanudar escaneo después de 2 segundos
            new android.os.Handler().postDelayed(() -> {
                if (qrScannerManager != null) {
                    qrScannerManager.resumeScanning();
                }
            }, 2000);
        });
    }
    
    private void handleSuccessfulScan(String clientName, String actionType) {
        // Actualizar UI
        updateParticipantCount();
        updateScanHistory();
        updateStatusDisplay();
        
        // Mostrar resultado exitoso
        String message = "CHECK_IN".equals(actionType) ? 
            "Check-in exitoso" : 
            "Check-out exitoso";
        
        tvScanStatus.setText(message);
        tvScanStatus.setTextColor(getColor(R.color.green));
        
        showScanResultCard(clientName, true);
        
        Toast.makeText(this, message + " para " + clientName, Toast.LENGTH_LONG).show();
    }
    
    // ==================== Helpers ====================





    // ==================== Permissions ====================

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Toast.makeText(this,
                        "Se necesita permiso de cámara para escanear QR",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // ==================== Lifecycle ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (qrScannerManager != null) {
            qrScannerManager.release();
            qrScannerManager = null;
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    
    // ==================== Adapter para Historial de Escaneos ====================
    

    
    // ==================== Notificación al Admin ====================
    
    private void generateAdminPaymentNotification() {
        if (currentTour == null) return;
        
        // Obtener todas las reservas confirmadas para calcular el total
        firestoreManager.getReservationsByTour(tourId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                java.util.List<com.example.droidtour.models.Reservation> reservations = 
                    (java.util.List<com.example.droidtour.models.Reservation>) result;
                
                double totalAmount = 0.0;
                int totalParticipants = 0;
                StringBuilder participantsSummary = new StringBuilder();
                
                for (com.example.droidtour.models.Reservation reservation : reservations) {
                    if ("CONFIRMADA".equals(reservation.getStatus()) && 
                        reservation.getHasCheckedIn() != null && reservation.getHasCheckedIn() &&
                        reservation.getHasCheckedOut() != null && reservation.getHasCheckedOut()) {
                        
                        totalAmount += reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0.0;
                        totalParticipants += reservation.getNumberOfPeople() != null ? reservation.getNumberOfPeople() : 0;
                        
                        participantsSummary.append("\n\u2022 ")
                            .append(reservation.getUserName())
                            .append(": S/. ")
                            .append(String.format("%.2f", reservation.getTotalPrice()))
                            .append(" (")
                            .append(reservation.getPaymentMethod() != null ? reservation.getPaymentMethod() : "Tarjeta")
                            .append(")");
                    }
                }
                
                // Crear notificación para el admin
                String adminId = currentTour.getCompanyId(); // Asumiendo que companyId es el admin
                String title = "\ud83d\udcb3 Tour Completado - Cobro Pendiente";
                String message = "El tour '" + currentTour.getTourName() + "' ha finalizado.\n\n" +
                    "\ud83d\udc65 Participantes: " + totalParticipants + "\n" +
                    "\ud83d\udcb5 Total a cobrar: S/. " + String.format("%.2f", totalAmount) + "\n\n" +
                    "Detalles por cliente:" + participantsSummary.toString() + "\n\n" +
                    "Los cargos ser\u00e1n procesados autom\u00e1ticamente a las tarjetas registradas.";
                
                com.example.droidtour.models.Notification notification = new com.example.droidtour.models.Notification(
                    adminId,
                    "ADMIN",
                    com.example.droidtour.models.Notification.TYPE_PAYMENT_CHARGED,
                    title,
                    message,
                    tourId,
                    "tour"
                );
                notification.setIsImportant(true);
                
                // Guardar notificación en Firestore
                firestoreManager.createNotification(notification, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d(TAG, "✅ Notificación de pago enviada al admin");
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "\u274c Error al enviar notificaci\u00f3n al admin", e);
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar reservas para notificaci\u00f3n", e);
            }
        });
    }
}
