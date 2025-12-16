package com.example.droidtour.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.example.droidtour.firebase.FirebaseStorageManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * Helper para descargar archivos adjuntos del chat
 */
public class AttachmentDownloadHelper {
    private static final String TAG = "AttachmentDownloadHelper";
    private static final int REQUEST_CODE_WRITE_STORAGE = 3001;
    
    /**
     * Descarga y abre una imagen
     */
    public static void downloadAndOpenImage(Context context, String imageUrl, String fileName) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(context, "URL de imagen inválida", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar toast de descarga iniciada
        Toast.makeText(context, "Descargando imagen...", Toast.LENGTH_SHORT).show();
        
        // Descargar usando Firebase Storage Manager
        FirebaseStorageManager storageManager = FirebaseStorageManager.getInstance();
        String finalFileName = fileName != null ? fileName : "imagen_" + System.currentTimeMillis() + ".jpg";
        
        storageManager.downloadChatAttachment(imageUrl, finalFileName, new FirebaseStorageManager.DownloadCallback() {
            @Override
            public void onSuccess(Uri fileUri) {
                // Abrir imagen con app externa usando FileProvider
                openImage(context, fileUri);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error descargando imagen", e);
                Toast.makeText(context, "Error al descargar imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onProgress(int progress) {
                // Opcional: mostrar progreso
            }
        });
    }
    
    /**
     * Descarga y abre un PDF
     */
    public static void downloadAndOpenPdf(Context context, String pdfUrl, String fileName) {
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(context, "URL de PDF inválida", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar toast de descarga iniciada
        Toast.makeText(context, "Descargando archivo...", Toast.LENGTH_SHORT).show();
        
        // Descargar usando Firebase Storage Manager
        FirebaseStorageManager storageManager = FirebaseStorageManager.getInstance();
        String finalFileName = fileName != null ? fileName : "documento_" + System.currentTimeMillis() + ".pdf";
        
        storageManager.downloadChatAttachment(pdfUrl, finalFileName, new FirebaseStorageManager.DownloadCallback() {
            @Override
            public void onSuccess(Uri fileUri) {
                // Abrir PDF con app externa usando FileProvider
                openPdf(context, fileUri);
                Toast.makeText(context, "Archivo descargado", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error descargando PDF", e);
                Toast.makeText(context, "Error al descargar archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onProgress(int progress) {
                // Opcional: mostrar progreso
            }
        });
    }
    
    /**
     * Abre una imagen con app externa usando FileProvider
     */
    private static void openImage(Context context, Uri fileUri) {
        try {
            File file = new File(fileUri.getPath());
            
            // Verificar que el archivo existe
            if (!file.exists()) {
                Log.e(TAG, "El archivo no existe: " + file.getAbsolutePath());
                Toast.makeText(context, "Error: archivo no encontrado", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d(TAG, "Abriendo imagen: " + file.getAbsolutePath());
            
            // Usar FileProvider para todas las versiones
            Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
            );
            
            Log.d(TAG, "Content URI: " + contentUri.toString());
            
            // Crear Intent con ACTION_VIEW
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Intentar abrir con chooser (siempre muestra opciones si hay apps disponibles)
            try {
                Intent chooser = Intent.createChooser(intent, "Abrir imagen con");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                chooser.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                context.startActivity(chooser);
            } catch (Exception e) {
                Log.e(TAG, "Error con chooser, intentando directo", e);
                // Intentar sin chooser
                try {
                    context.startActivity(intent);
                } catch (Exception e2) {
                    Log.e(TAG, "Error al iniciar actividad", e2);
                    Toast.makeText(context, "No se pudo abrir la imagen. El archivo se guardó en Descargas/DroidTour", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error abriendo imagen", e);
            Toast.makeText(context, "Error al abrir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Abre un PDF con app externa usando FileProvider
     */
    private static void openPdf(Context context, Uri fileUri) {
        try {
            File file = new File(fileUri.getPath());
            
            // Verificar que el archivo existe
            if (!file.exists()) {
                Log.e(TAG, "El archivo no existe: " + file.getAbsolutePath());
                Toast.makeText(context, "Error: archivo no encontrado", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d(TAG, "Abriendo PDF: " + file.getAbsolutePath());
            
            // Usar FileProvider para todas las versiones
            Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
            );
            
            Log.d(TAG, "Content URI: " + contentUri.toString());
            
            // Crear Intent con ACTION_VIEW
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Intentar abrir con chooser (siempre muestra opciones si hay apps disponibles)
            try {
                Intent chooser = Intent.createChooser(intent, "Abrir PDF con");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                chooser.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                context.startActivity(chooser);
            } catch (Exception e) {
                Log.e(TAG, "Error con chooser, intentando directo", e);
                // Intentar sin chooser
                try {
                    context.startActivity(intent);
                } catch (Exception e2) {
                    Log.e(TAG, "Error al iniciar actividad", e2);
                    Toast.makeText(context, "No se pudo abrir el PDF. El archivo se guardó en Descargas/DroidTour", Toast.LENGTH_LONG).show();
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error con FileProvider - ruta no configurada", e);
            // Intentar con MediaStore para Android 10+
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Usar MediaStore para Android 10+
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, new File(fileUri.getPath()).getName());
                    values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf");
                    values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DroidTour");
                    
                    Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "application/pdf");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(Intent.createChooser(intent, "Abrir PDF con"));
                    }
                } else {
                    // Para Android 9 y anteriores, intentar con file:// URI
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(Intent.createChooser(intent, "Abrir PDF con"));
                }
            } catch (Exception e3) {
                Log.e(TAG, "Error en fallback", e3);
                Toast.makeText(context, "No se pudo abrir el PDF. El archivo se guardó en Descargas/DroidTour", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error abriendo PDF", e);
            Toast.makeText(context, "Error al abrir PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Verifica si tiene permisos de almacenamiento
     */
    private static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: no se necesita permiso para descargar en Downloads
            return true;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
}

