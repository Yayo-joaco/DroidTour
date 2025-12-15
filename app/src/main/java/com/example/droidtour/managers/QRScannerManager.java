package com.example.droidtour.managers;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Clase utilitaria para manejar el escaneo de códigos QR usando CameraX y ML Kit
 */
public class QRScannerManager {

    private static final String TAG = "QRScannerManager";

    private final Context context;
    private final PreviewView previewView;
    private final QRScanCallback callback;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private ToneGenerator toneGenerator;

    private boolean isScanning = true;
    private boolean isFlashOn = false;
    private long lastScanTime = 0L;
    private static final long SCAN_COOLDOWN = 1200L; // ms entre escaneos

    /**
     * Interface para recibir callbacks de escaneo
     */
    public interface QRScanCallback {
        void onQRScanned(String qrData, Barcode barcode);
        void onScanError(Exception e);
    }

    /**
     * Constructor
     */
    public QRScannerManager(Context context, PreviewView previewView, QRScanCallback callback) {
        this.context = context;
        this.previewView = previewView;
        this.callback = callback;

        initializeScanner();
    }

    /**
     * Inicializa el escáner de códigos de barras
     */
    private void initializeScanner() {
        // Configurar opciones del escáner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_DATA_MATRIX)
                .build();

        barcodeScanner = BarcodeScanning.getClient(options);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Inicializar generador de tono
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando ToneGenerator", e);
        }
    }

    /**
     * Inicia la cámara y el escaneo
     */
    public void startCamera(LifecycleOwner lifecycleOwner) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(lifecycleOwner);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error al iniciar cámara", e);
                if (callback != null) {
                    callback.onScanError(e);
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    /**
     * Vincula los casos de uso de la cámara
     */
    private void bindCameraUseCases(LifecycleOwner lifecycleOwner) {
        // Configurar Preview
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Configurar ImageAnalysis para escanear QR
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        // Seleccionar cámara trasera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Unbind cualquier caso de uso previo
        cameraProvider.unbindAll();

        // Bind casos de uso a ciclo de vida
        camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
        );
    }

    /**
     * Analiza la imagen en busca de códigos QR
     */
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (!isScanning) {
            imageProxy.close();
            return;
        }

        // Verificar cooldown
        long now = System.currentTimeMillis();
        if (now - lastScanTime < SCAN_COOLDOWN) {
            imageProxy.close();
            return;
        }

        @androidx.camera.core.ExperimentalGetImage
        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty() && isScanning) {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && !rawValue.isEmpty()) {
                                lastScanTime = System.currentTimeMillis();
                                isScanning = false;

                                // Reproducir feedback
                                playFeedback();

                                // Notificar callback
                                if (callback != null) {
                                    callback.onQRScanned(rawValue, barcode);
                                }
                                break;
                            }
                        }
                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error escaneando código", e);
                    if (callback != null) {
                        callback.onScanError(e);
                    }
                    imageProxy.close();
                });
    }

    /**
     * Reproduce feedback sonoro y de vibración
     */
    private void playFeedback() {
        // Beep
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
            } catch (Exception e) {
                Log.e(TAG, "Error reproduciendo beep", e);
            }
        }

        // Vibración
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(60,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(60);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrando", e);
        }
    }

    /**
     * Alterna el flash de la cámara
     */
    public boolean toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            return isFlashOn;
        }
        return false;
    }

    /**
     * Obtiene el estado actual del flash
     */
    public boolean isFlashOn() {
        return isFlashOn;
    }

    /**
     * Verifica si la cámara tiene flash
     */
    public boolean hasFlash() {
        return camera != null && camera.getCameraInfo().hasFlashUnit();
    }

    /**
     * Resume el escaneo después de un escaneo exitoso
     */
    public void resumeScanning() {
        isScanning = true;
        lastScanTime = 0L;
    }

    /**
     * Pausa el escaneo
     */
    public void pauseScanning() {
        isScanning = false;
    }

    /**
     * Verifica si está escaneando
     */
    public boolean isScanning() {
        return isScanning;
    }

    /**
     * Libera recursos
     */
    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }

        if (barcodeScanner != null) {
            barcodeScanner.close();
            barcodeScanner = null;
        }

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }
}