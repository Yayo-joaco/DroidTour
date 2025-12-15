package com.example.droidtour.superadmin.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.droidtour.R;

import java.io.File;
import java.util.List;

/**
 * Helper para mostrar notificaciones de exportación y generación de reportes
 */
public class NotificationHelper {
    
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID_EXPORT = "export_channel";
    private static final String CHANNEL_ID_REPORTS = "reports_channel";
    private static final int NOTIFICATION_ID_EXPORT = 1001;
    private static final int NOTIFICATION_ID_REPORTS = 1002;
    
    private final Context context;
    
    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannels();
    }
    
    /**
     * Crea los canales de notificaciones necesarios
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                context.getSystemService(NotificationManager.class);
            
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager no disponible");
                return;
            }
            
            // Canal para exportaciones del dashboard
            NotificationChannel exportChannel = new NotificationChannel(
                CHANNEL_ID_EXPORT,
                "Exportaciones",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            exportChannel.setDescription("Notificaciones de exportación de reportes e imágenes");
            notificationManager.createNotificationChannel(exportChannel);
            
            // Canal para reportes PDF
            NotificationChannel reportsChannel = new NotificationChannel(
                CHANNEL_ID_REPORTS,
                "Reportes PDF",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            reportsChannel.setDescription("Notificaciones de generación de reportes PDF");
            notificationManager.createNotificationChannel(reportsChannel);
        }
    }
    
    /**
     * Muestra notificación de exportación exitosa del dashboard
     */
    public void showExportSuccessNotification(int imageCount, String pdfPath, List<String> imagePaths) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri pdfUri = null;
            
            // Determinar el URI según cómo se guardó el archivo
            if (pdfPath != null && pdfPath.startsWith("content://")) {
                // Android 10+ - URI de MediaStore
                pdfUri = Uri.parse(pdfPath);
            } else if (pdfPath != null) {
                // Android 9- - Archivo en sistema de archivos
                File pdfFile = new File(pdfPath);
                if (pdfFile.exists()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // Usar FileProvider para Android 7+
                        pdfUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".fileprovider",
                            pdfFile
                        );
                    } else {
                        // Android 6 y anteriores - usar URI directo
                        pdfUri = Uri.fromFile(pdfFile);
                    }
                } else {
                    Log.e(TAG, "El archivo PDF no existe: " + pdfPath);
                    // Fallback: abrir carpeta de descargas
                    pdfUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FDroidTour");
                    intent.setDataAndType(pdfUri, "resource/folder");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, 0, intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    StringBuilder contentText = new StringBuilder();
                    contentText.append("✅ Exportación completada\n\n");
                    contentText.append("📄 PDF guardado\n");
                    if (imageCount > 0) {
                        contentText.append("🖼️ ").append(imageCount).append(" imágenes guardadas\n");
                    }
                    contentText.append("📁 Ubicación: Descargas/DroidTour");
                    
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_EXPORT)
                        .setSmallIcon(R.drawable.ic_download_24)
                        .setContentTitle("✅ Exportación Completada")
                        .setContentText("Reporte guardado en Descargas/DroidTour")
                        .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(contentText.toString()))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setColor(ContextCompat.getColor(context, R.color.primary));
                    
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                        notificationManager.notify(NOTIFICATION_ID_EXPORT, builder.build());
                    }
                    return;
                }
            } else {
                // pdfPath es null, abrir carpeta de descargas
                pdfUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FDroidTour");
                intent.setDataAndType(pdfUri, "resource/folder");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                StringBuilder contentText = new StringBuilder();
                contentText.append("✅ Exportación completada\n\n");
                contentText.append("📄 PDF guardado\n");
                contentText.append("🖼️ ").append(imageCount).append(" imágenes guardadas\n");
                contentText.append("📁 Ubicación: Descargas/DroidTour");
                
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_EXPORT)
                    .setSmallIcon(R.drawable.ic_download_24)
                    .setContentTitle("✅ Exportación Completada")
                    .setContentText("Reporte guardado en Descargas/DroidTour")
                    .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText.toString()))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setColor(ContextCompat.getColor(context, R.color.primary));
                
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    notificationManager.notify(NOTIFICATION_ID_EXPORT, builder.build());
                }
                return;
            }
            
            // Configurar intent para abrir el PDF directamente
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Verificar si hay aplicaciones que puedan manejar PDFs
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            
            // Si solo hay una aplicación disponible, usarla directamente para evitar el chooser
            if (resolveInfos.size() == 1) {
                intent.setPackage(resolveInfos.get(0).activityInfo.packageName);
            }
            // Si hay múltiples aplicaciones, Android usará la predeterminada si está configurada
            // Si no hay predeterminada, mostrará el chooser (comportamiento esperado del sistema)
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Extraer nombre del archivo
            String fileName = "Reporte_Analytics.pdf";
            if (pdfPath != null) {
                if (pdfPath.startsWith("content://")) {
                    // Intentar extraer nombre del URI o usar nombre por defecto
                    try {
                        String[] segments = pdfPath.split("/");
                        if (segments.length > 0) {
                            String lastSegment = segments[segments.length - 1];
                            if (lastSegment.contains(".")) {
                                fileName = lastSegment;
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "No se pudo extraer nombre del URI", e);
                    }
                } else {
                    File file = new File(pdfPath);
                    if (file.exists()) {
                        fileName = file.getName();
                    }
                }
            }
            
            StringBuilder contentText = new StringBuilder();
            contentText.append("✅ Exportación completada\n\n");
            contentText.append("📄 PDF: ").append(fileName).append("\n");
            if (imageCount > 0) {
                contentText.append("🖼️ ").append(imageCount).append(" imágenes guardadas\n");
            }
            contentText.append("Toca para abrir el PDF");
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_EXPORT)
                .setSmallIcon(R.drawable.ic_download_24)
                .setContentTitle("✅ Exportación Completada")
                .setContentText("Reporte guardado - Toca para abrir")
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(contentText.toString()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.primary));
            
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID_EXPORT, builder.build());
            } else {
                Log.w(TAG, "Las notificaciones están deshabilitadas");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando notificación de exportación", e);
        }
    }
    
    /**
     * Muestra notificación de error en exportación
     */
    public void showExportErrorNotification(String errorMessage) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_EXPORT)
                .setSmallIcon(R.drawable.ic_download_24)
                .setContentTitle("❌ Error en Exportación")
                .setContentText(errorMessage != null ? errorMessage : "No se pudo completar la exportación")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setColor(android.graphics.Color.RED);
            
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID_EXPORT + 1, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando notificación de error", e);
        }
    }
    
    /**
     * Muestra notificación de generación exitosa de reporte PDF
     */
    public void showReportPDFSuccessNotification(String filePath) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri pdfUri = null;
            
            // Determinar el URI según cómo se guardó el archivo
            if (filePath.startsWith("content://")) {
                // Android 10+ - URI de MediaStore
                pdfUri = Uri.parse(filePath);
            } else {
                // Android 9- - Archivo en sistema de archivos
                File pdfFile = new File(filePath);
                if (pdfFile.exists()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // Usar FileProvider para Android 7+
                        pdfUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".fileprovider",
                            pdfFile
                        );
                    } else {
                        // Android 6 y anteriores - usar URI directo
                        pdfUri = Uri.fromFile(pdfFile);
                    }
                } else {
                    Log.e(TAG, "El archivo PDF no existe: " + filePath);
                    // Fallback: abrir carpeta de descargas
                    pdfUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FDroidTour");
                    intent.setDataAndType(pdfUri, "resource/folder");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, 0, intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    String fileName = new File(filePath).getName();
                    String contentText = "Reporte PDF guardado exitosamente\n📁 " + fileName;
                    
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REPORTS)
                        .setSmallIcon(R.drawable.ic_download_24)
                        .setContentTitle("✅ Reporte PDF Generado")
                        .setContentText(contentText)
                        .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(contentText + "\n\nUbicación: Descargas/DroidTour"))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setColor(ContextCompat.getColor(context, R.color.primary));
                    
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                        notificationManager.notify(NOTIFICATION_ID_REPORTS, builder.build());
                    }
                    return;
                }
            }
            
            // Configurar intent para abrir el PDF directamente
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Verificar si hay aplicaciones que puedan manejar PDFs
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            
            // Si solo hay una aplicación disponible, usarla directamente para evitar el chooser
            if (resolveInfos.size() == 1) {
                intent.setPackage(resolveInfos.get(0).activityInfo.packageName);
            }
            // Si hay múltiples aplicaciones, Android usará la predeterminada si está configurada
            // Si no hay predeterminada, mostrará el chooser (comportamiento esperado del sistema)
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Extraer nombre del archivo
            String fileName = "Reporte_Reservas.pdf";
            if (filePath.startsWith("content://")) {
                // Intentar extraer nombre del URI o usar nombre por defecto
                try {
                    String[] segments = filePath.split("/");
                    if (segments.length > 0) {
                        String lastSegment = segments[segments.length - 1];
                        if (lastSegment.contains(".")) {
                            fileName = lastSegment;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "No se pudo extraer nombre del URI", e);
                }
            } else {
                File file = new File(filePath);
                if (file.exists()) {
                    fileName = file.getName();
                }
            }
            
            String contentText = "Reporte PDF guardado exitosamente\n📁 " + fileName;
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REPORTS)
                .setSmallIcon(R.drawable.ic_download_24)
                .setContentTitle("✅ Reporte PDF Generado")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(contentText + "\n\nToca para abrir el PDF"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.primary));
            
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID_REPORTS, builder.build());
            } else {
                Log.w(TAG, "Las notificaciones están deshabilitadas");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando notificación de reporte PDF", e);
        }
    }
    
    /**
     * Muestra notificación de error en generación de reporte PDF
     */
    public void showReportPDFErrorNotification(String errorMessage) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REPORTS)
                .setSmallIcon(R.drawable.ic_download_24)
                .setContentTitle("❌ Error al Generar Reporte")
                .setContentText(errorMessage != null ? errorMessage : "No se pudo generar el reporte PDF")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setColor(android.graphics.Color.RED);
            
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID_REPORTS + 1, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando notificación de error de reporte", e);
        }
    }
}

