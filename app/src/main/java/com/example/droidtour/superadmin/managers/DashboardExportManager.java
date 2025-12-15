package com.example.droidtour.superadmin.managers;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.droidtour.R;
import com.example.droidtour.superadmin.helpers.DashboardExportHelper;
import com.example.droidtour.superadmin.helpers.NotificationHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager para coordinar la exportación de reportes del dashboard
 * Maneja preparación de datos, permisos, notificaciones y coordinación con DashboardExportHelper
 */
public class DashboardExportManager {
    
    private static final String TAG = "DashboardExportManager";
    private static final String CHANNEL_ID = "export_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private final Activity activity;
    private final DashboardExportHelper exportHelper;
    private final NotificationHelper notificationHelper;
    private final int permissionRequestCode;
    
    // Referencias a UI components
    private TextView tvTotalUsers, tvActiveTours, tvRevenue, tvBookings;
    private LineChart lineChartRevenue, lineChartAveragePrice;
    private PieChart pieChartTours;
    private BarChart barChartBookings, barChartPeople;
    
    public DashboardExportManager(Activity activity, DashboardExportHelper exportHelper, 
                                  int permissionRequestCode) {
        this.activity = activity;
        this.exportHelper = exportHelper;
        this.notificationHelper = new NotificationHelper(activity);
        this.permissionRequestCode = permissionRequestCode;
        createNotificationChannel();
    }
    
    /**
     * Crea el canal de notificaciones para exportaciones
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Exportaciones",
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Notificaciones de exportación de reportes e imágenes");
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Establece referencias a los TextViews de KPIs
     */
    public void setKPIViews(TextView tvTotalUsers, TextView tvActiveTours, 
                           TextView tvRevenue, TextView tvBookings) {
        this.tvTotalUsers = tvTotalUsers;
        this.tvActiveTours = tvActiveTours;
        this.tvRevenue = tvRevenue;
        this.tvBookings = tvBookings;
    }
    
    /**
     * Establece referencias a los gráficos
     */
    public void setCharts(LineChart lineChartRevenue, LineChart lineChartAveragePrice,
                         PieChart pieChartTours, BarChart barChartBookings, BarChart barChartPeople) {
        this.lineChartRevenue = lineChartRevenue;
        this.lineChartAveragePrice = lineChartAveragePrice;
        this.pieChartTours = pieChartTours;
        this.barChartBookings = barChartBookings;
        this.barChartPeople = barChartPeople;
    }
    
    /**
     * Inicia el proceso de exportación
     * Verifica permisos y prepara datos antes de exportar
     */
    public void requestExport() {
        // Verificar permisos para Android 9 y anteriores
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        permissionRequestCode);
                return;
            }
        }
        
        // Si tiene permisos, proceder con exportación
        performExport();
    }
    
    /**
     * Realiza la exportación después de verificar permisos
     */
    public void performExport() {
        try {
            // Preparar datos de KPIs
            String[] kpiLabels = {"Total Usuarios", "Tours Activos", "Ingresos", "Reservas"};
            String[] kpiValues = prepareKPIValues();
            
            // Preparar gráficos
            List<DashboardExportHelper.ChartInfo> charts = prepareCharts();
            
            // Exportar usando helper
            exportHelper.exportAnalyticsReport(charts, kpiLabels, kpiValues, 
                new DashboardExportHelper.ExportCompleteCallback() {
                    @Override
                    public void onSuccess(String pdfPath, List<String> imagePaths) {
                        // Usar NotificationHelper para mostrar notificación (solo PDF, sin imágenes)
                        if (notificationHelper != null) {
                            notificationHelper.showExportSuccessNotification(0, pdfPath, new ArrayList<>());
                        }
                        showSuccessToast();
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Error en exportación", error);
                        if (notificationHelper != null) {
                            notificationHelper.showExportErrorNotification(error.getMessage());
                        }
                        showErrorToast(error.getMessage());
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error en exportación", e);
            if (notificationHelper != null) {
                notificationHelper.showExportErrorNotification(e.getMessage());
            }
            showErrorToast(e.getMessage());
        }
    }
    
    /**
     * Prepara valores de KPIs desde los TextViews
     */
    private String[] prepareKPIValues() {
        return new String[]{
            tvTotalUsers != null ? tvTotalUsers.getText().toString() : "N/A",
            tvActiveTours != null ? tvActiveTours.getText().toString() : "N/A",
            tvRevenue != null ? tvRevenue.getText().toString() : "N/A",
            tvBookings != null ? tvBookings.getText().toString() : "N/A"
        };
    }
    
    /**
     * Prepara lista de gráficos para exportar
     */
    private List<DashboardExportHelper.ChartInfo> prepareCharts() {
        List<DashboardExportHelper.ChartInfo> charts = new ArrayList<>();
        
        if (lineChartRevenue != null && lineChartRevenue.getData() != null) {
            charts.add(new DashboardExportHelper.ChartInfo(lineChartRevenue, "Ingresos"));
        }
        if (lineChartAveragePrice != null && lineChartAveragePrice.getData() != null) {
            charts.add(new DashboardExportHelper.ChartInfo(lineChartAveragePrice, "Precio Promedio por Persona"));
        }
        if (pieChartTours != null && pieChartTours.getData() != null) {
            charts.add(new DashboardExportHelper.ChartInfo(pieChartTours, "Tours por Categoría"));
        }
        if (barChartBookings != null && barChartBookings.getData() != null) {
            charts.add(new DashboardExportHelper.ChartInfo(barChartBookings, "Cantidad de reservas"));
        }
        if (barChartPeople != null && barChartPeople.getData() != null) {
            charts.add(new DashboardExportHelper.ChartInfo(barChartPeople, "Cantidad de personas"));
        }
        
        return charts;
    }
    
    /**
     * Muestra Toast de éxito
     */
    private void showSuccessToast() {
        String message = "✅ Exportación completada\n" + 
                       "PDF guardado en Descargas/DroidTour";
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Muestra Toast de error
     */
    private void showErrorToast(String errorMessage) {
        String message = "❌ Error al exportar: " + (errorMessage != null ? errorMessage : "Error desconocido");
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Extrae nombre de archivo desde path (soporta MediaStore y sistema tradicional)
     */
    private String extractFileName(String path) {
        if (path == null) return null;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri uri = Uri.parse(path);
                return uri.getLastPathSegment();
            } else {
                return new File(path).getName();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error extrayendo nombre de archivo", e);
            return null;
        }
    }
    
    /**
     * Maneja el resultado de permisos (llamado desde Activity)
     */
    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == permissionRequestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performExport();
            } else {
                Toast.makeText(activity, 
                    "❌ Permiso denegado. No se puede exportar sin permisos de almacenamiento.", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}


