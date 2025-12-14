package com.example.droidtour.client;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.droidtour.R;
import com.example.droidtour.models.Reservation;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class ClientQRCodesActivity extends AppCompatActivity {
    
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    private TextView tvTourName, tvTourDate, tvReservationCode;
    private TextView tvCheckinStatus, tvCheckoutStatus;
    private MaterialButton btnShareCheckin, btnSaveCheckin;
    private MaterialButton btnShareCheckout, btnSaveCheckout;
    private ImageView ivQrCheckin, ivQrCheckout;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    
    private String reservationId;
    private Reservation currentReservation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea CLIENT
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_client_qr_codes);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        
        getIntentData();
        setupToolbar();
        initializeViews();
        //loadReservationFromFirebase();
        setupClickListeners();
    }

    private void getIntentData() {
        reservationId = getIntent().getStringExtra("reservation_id");
        if (reservationId == null) {
            reservationId = "RES001"; // Fallback
        }
    }


    /*
    private void loadReservationFromFirebase() {
        firestoreManager.getReservationById(reservationId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                currentReservation = (com.example.droidtour.modelsenhanced.Reservation) result;
                displayReservationData();
                generateQRCodes();
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClientQRCodesActivity.this, "Error cargando reserva", Toast.LENGTH_SHORT).show();
                setupTourData(); // Fallback a hardcode
            }
        });
    }

     */
    
    private void displayReservationData() {
        if (currentReservation == null) return;
        
        tvTourName.setText(currentReservation.getTourName());
        tvTourDate.setText(currentReservation.getTourDate() + " • " + currentReservation.getTourTime());
        tvReservationCode.setText(currentReservation.getReservationId());
        
        // Actualizar estados de check-in/check-out
        if (currentReservation.getHasCheckedIn()) {
            tvCheckinStatus.setText("✅ Check-in realizado");
            tvCheckinStatus.setTextColor(getColor(R.color.success));
        } else {
            tvCheckinStatus.setText("⏳ Pendiente");
            tvCheckinStatus.setTextColor(getColor(R.color.warning));
        }
        
        if (currentReservation.getHasCheckedOut()) {
            tvCheckoutStatus.setText("✅ Check-out realizado");
            tvCheckoutStatus.setTextColor(getColor(R.color.success));
        } else {
            tvCheckoutStatus.setText("⏳ Pendiente");
            tvCheckoutStatus.setTextColor(getColor(R.color.warning));
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Códigos QR");
        }
    }

    private void initializeViews() {
        tvTourName = findViewById(R.id.tv_tour_name);
        tvTourDate = findViewById(R.id.tv_tour_date);
        tvReservationCode = findViewById(R.id.tv_reservation_code);
        tvCheckinStatus = findViewById(R.id.tv_checkin_status);
        tvCheckoutStatus = findViewById(R.id.tv_checkout_status);
        btnShareCheckin = findViewById(R.id.btn_share_checkin);
        btnSaveCheckin = findViewById(R.id.btn_save_checkin);
        btnShareCheckout = findViewById(R.id.btn_share_checkout);
        btnSaveCheckout = findViewById(R.id.btn_save_checkout);
        ivQrCheckin = findViewById(R.id.iv_qr_checkin);
        ivQrCheckout = findViewById(R.id.iv_qr_checkout);
    }

    private void setupTourData() {
        // Fallback con datos de ejemplo
        tvTourName.setText("City Tour Lima Centro");
        tvTourDate.setText("15 Dic, 2024 • 09:00 AM");
        tvReservationCode.setText(reservationId != null ? reservationId : "#DT2024001");
        tvCheckinStatus.setText("⏳ Pendiente");
        tvCheckoutStatus.setText("⏳ Pendiente");
    }

    private void setupClickListeners() {
        btnShareCheckin.setOnClickListener(v -> {
            shareQRCode("Check-in");
        });

        btnSaveCheckin.setOnClickListener(v -> {
            saveQRCode("Check-in");
        });

        btnShareCheckout.setOnClickListener(v -> {
            shareQRCode("Check-out");
        });

        btnSaveCheckout.setOnClickListener(v -> {
            saveQRCode("Check-out");
        });
    }

    private void shareQRCode(String type) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Código QR " + type + " para tour: " + tvTourName.getText());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Código QR " + type);
        startActivity(Intent.createChooser(shareIntent, "Compartir QR " + type));
    }

    private void saveQRCode(String type) {
        Toast.makeText(this, "QR " + type + " guardado en galería", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Generar códigos QR usando ZXing
     */
    private void generateQRCodes() {
        if (currentReservation == null) return;
        
        String qrCheckinData = currentReservation.getQrCodeCheckIn();
        String qrCheckoutData = currentReservation.getQrCodeCheckOut();
        
        if (qrCheckinData != null && !qrCheckinData.isEmpty()) {
            Bitmap qrCheckinBitmap = generateQRCodeBitmap(qrCheckinData, 500, 500);
            if (qrCheckinBitmap != null) {
                ivQrCheckin.setImageBitmap(qrCheckinBitmap);
            }
        }
        
        if (qrCheckoutData != null && !qrCheckoutData.isEmpty()) {
            Bitmap qrCheckoutBitmap = generateQRCodeBitmap(qrCheckoutData, 500, 500);
            if (qrCheckoutBitmap != null) {
                ivQrCheckout.setImageBitmap(qrCheckoutBitmap);
            }
        }
    }
    
    /**
     * Generar bitmap de código QR usando ZXing
     */
    private Bitmap generateQRCodeBitmap(String data, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generando código QR", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void redirectToLogin() {
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

