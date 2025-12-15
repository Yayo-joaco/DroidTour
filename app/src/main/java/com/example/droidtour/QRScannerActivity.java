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
    private TextView tvScanStatus, tvTourName, tvParticipantCount, tvScansCount;
    private MaterialButton btnManualEntry, btnContinueScanning, btnToggleFlash;
    private MaterialCardView cardScanResult;
    private PreviewView previewView;
    // Managers
    private PreferencesManager prefsManager;
    private QRScannerManager qrScannerManager;

    // Tour info
    private String tourName = "City Tour Lima Centro";
    private int participantsCount = 0;
    private int scannedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar PreferencesManager
        prefsManager = new PreferencesManager(this);

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
        tvScansCount = findViewById(R.id.tv_scans_count);
        btnManualEntry = findViewById(R.id.btn_manual_entry);
        btnContinueScanning = findViewById(R.id.btn_continue_scanning);
        btnToggleFlash = findViewById(R.id.btn_toggle_flash);
        cardScanResult = findViewById(R.id.card_scan_result);

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
        btnManualEntry.setOnClickListener(v -> showManualEntryDialog());
        btnToggleFlash.setOnClickListener(v -> toggleFlash());
        btnContinueScanning.setOnClickListener(v -> continueScanning());
    }

    private void loadTourData() {
        Intent intent = getIntent();
        if (intent != null) {
            String intentTourName = intent.getStringExtra("tour_name");
            if (intentTourName != null) {
                tourName = intentTourName;
                tvTourName.setText(tourName);
            }

            // Puedes obtener más datos como reservation_id si los necesitas
            // String reservationId = intent.getStringExtra("reservation_id");
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

            // Incrementar contadores
            scannedCount++;
            participantsCount++;

            // Actualizar UI
            updateParticipantCount();
            updateScansCount();

            // Mostrar resultado exitoso
            String clientName = extractClientNameFromQR(qrData);
            handleSuccessfulScan(qrData, clientName);
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
        tvParticipantCount.setText(participantsCount + " personas");
    }

    private void updateScansCount() {
        if (tvScansCount != null) {
            tvScansCount.setText(scannedCount + " escaneos");
        }
    }

    private void handleSuccessfulScan(String qrData, String clientName) {
        // Actualizar estado
        tvScanStatus.setText("✓ Check-in Completado");
        tvScanStatus.setTextColor(getColor(R.color.green));

        // Mostrar card de resultado
        showScanResultCard(clientName, true);

        // Mostrar toast
        Toast.makeText(this, "¡Check-in exitoso para " + clientName + "!",
                Toast.LENGTH_LONG).show();

        // Enviar datos al servidor
        sendScanToServer(qrData, clientName);
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

    private void showManualEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Entrada manual");
        builder.setMessage("Introduce el código del cliente:");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Validar", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                processManualCode(code);
            } else {
                Toast.makeText(QRScannerActivity.this,
                        "Por favor, introduce un código",
                        Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void processManualCode(String code) {
        // Incrementar contadores
        scannedCount++;
        participantsCount++;

        // Actualizar UI
        updateParticipantCount();
        updateScansCount();

        // Simular escaneo exitoso
        String clientName = "Cliente Manual #" + code;
        handleSuccessfulScan("MANUAL:" + code, clientName);
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
        tvScanStatus.setText("Esperando QR de Check-in");
        tvScanStatus.setTextColor(getColor(R.color.orange));

        if (qrScannerManager != null) {
            qrScannerManager.resumeScanning();
        }

        Toast.makeText(this, "Listo para escanear siguiente QR",
                Toast.LENGTH_SHORT).show();
    }

    // ==================== Helpers ====================

    private String extractClientNameFromQR(String qrData) {
        // Aquí puedes parsear el QR para extraer información real
        // Por ejemplo, si el QR tiene formato JSON:
        // {"clientName":"Ana García", "reservationId":"12345"}

        // Por ahora, simulamos nombres
        String[] fakeNames = {
                "Ana García Pérez",
                "Carlos López Silva",
                "María Rodríguez Torres",
                "Juan Pérez Gómez",
                "Laura Martínez Ruiz"
        };

        int index = Math.abs(qrData.hashCode() % fakeNames.length);
        return fakeNames[index];
    }

    private void sendScanToServer(String qrData, String clientName) {
        // Implementar envío a Firebase o tu backend
        Log.d(TAG, "Enviando scan al servidor:");
        Log.d(TAG, "  - QR Data: " + qrData);
        Log.d(TAG, "  - Cliente: " + clientName);
        Log.d(TAG, "  - Tour: " + tourName);
        Log.d(TAG, "  - Guía ID: " + prefsManager.getUserId());

        // Ejemplo con Firestore:
        /*
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> scanData = new HashMap<>();
        scanData.put("qrData", qrData);
        scanData.put("clientName", clientName);
        scanData.put("tourName", tourName);
        scanData.put("timestamp", FieldValue.serverTimestamp());
        scanData.put("guideId", prefsManager.getUserId());

        db.collection("scans")
            .add(scanData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Scan guardado con ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error guardando scan", e);
            });
        */
    }

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
}