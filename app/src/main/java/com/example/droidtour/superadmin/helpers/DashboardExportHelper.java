package com.example.droidtour.superadmin.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import com.example.droidtour.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Helper class para exportación completa de gráficos (PDF + imágenes)
 * Soporta Android 10+ (MediaStore) y versiones anteriores
 */
public class DashboardExportHelper {
    
    private static final String TAG = "DashboardExportHelper";
    private final Context context;
    
    public DashboardExportHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Exporta reporte completo: PDF con KPIs + imágenes de gráficos
     */
    public void exportAnalyticsReport(List<ChartInfo> charts, String[] kpiLabels, String[] kpiValues, 
                                     ExportCompleteCallback callback) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ usar MediaStore
                exportWithMediaStore(charts, kpiLabels, kpiValues, timestamp, callback);
            } else {
                // Android 9 y anteriores usar directorio tradicional
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File droidTourDir = new File(downloadsDir, "DroidTour");
                if (!droidTourDir.exists() && !droidTourDir.mkdirs()) {
                    if (callback != null) {
                        callback.onError(new IOException("No se pudo crear el directorio de descarga"));
                    }
                    return;
                }
                exportWithFileSystem(charts, kpiLabels, kpiValues, timestamp, droidTourDir, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en exportación", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Exporta usando MediaStore (Android 10+)
     */
    private void exportWithMediaStore(List<ChartInfo> charts, String[] kpiLabels, String[] kpiValues,
                                     String timestamp, ExportCompleteCallback callback) {
        try {
            // Exportar PDF
            String pdfPath = exportPDFWithMediaStore(kpiLabels, kpiValues, timestamp);
            
            // Exportar imágenes
            List<String> imagePaths = exportImagesWithMediaStore(charts, timestamp);
            
            if (callback != null) {
                callback.onSuccess(pdfPath, imagePaths);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exportando con MediaStore", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Exporta usando sistema de archivos tradicional (Android 9-)
     */
    private void exportWithFileSystem(List<ChartInfo> charts, String[] kpiLabels, String[] kpiValues,
                                      String timestamp, File directory, ExportCompleteCallback callback) {
        try {
            // Exportar PDF
            String pdfPath = exportPDFWithFileSystem(kpiLabels, kpiValues, timestamp, directory);
            
            // Exportar imágenes
            List<String> imagePaths = exportImagesWithFileSystem(charts, timestamp, directory);
            
            if (callback != null) {
                callback.onSuccess(pdfPath, imagePaths);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exportando con sistema de archivos", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Exporta PDF con KPIs usando MediaStore
     */
    private String exportPDFWithMediaStore(String[] kpiLabels, String[] kpiValues, String timestamp) throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        // Título
        paint.setTextSize(24);
        paint.setColor(context.getResources().getColor(R.color.primary));
        paint.setFakeBoldText(true);
        canvas.drawText("Reporte Analytics - DroidTour", 50, 80, paint);
        
        // Fecha
        paint.setTextSize(14);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(false);
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generado: " + currentDate, 50, 110, paint);
        
        // KPIs
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("Indicadores Clave de Rendimiento", 50, 160, paint);
        
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        int yPos = 190;
        if (kpiLabels != null && kpiValues != null) {
            for (int i = 0; i < Math.min(kpiLabels.length, kpiValues.length); i++) {
                canvas.drawText("• " + kpiLabels[i] + ": " + kpiValues[i], 70, yPos, paint);
                yPos += 20;
            }
        }
        
        // Pie de página
        paint.setTextSize(10);
        paint.setColor(Color.GRAY);
        canvas.drawText("DroidTour SuperAdmin Dashboard - Confidencial", 50, 800, paint);
        
        document.finishPage(page);
        
        // Guardar usando MediaStore
        String fileName = "Reporte_Analytics_" + timestamp + ".pdf";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DroidTour");
        
        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            FileOutputStream fos = (FileOutputStream) context.getContentResolver().openOutputStream(uri);
            if (fos != null) {
                document.writeTo(fos);
                fos.close();
            }
        }
        document.close();
        
        return uri != null ? uri.toString() : fileName;
    }
    
    /**
     * Exporta PDF con KPIs usando sistema de archivos
     */
    private String exportPDFWithFileSystem(String[] kpiLabels, String[] kpiValues, String timestamp, File directory) throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        // Título
        paint.setTextSize(24);
        paint.setColor(context.getResources().getColor(R.color.primary));
        paint.setFakeBoldText(true);
        canvas.drawText("Reporte Analytics - DroidTour", 50, 80, paint);
        
        // Fecha
        paint.setTextSize(14);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(false);
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generado: " + currentDate, 50, 110, paint);
        
        // KPIs
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("Indicadores Clave de Rendimiento", 50, 160, paint);
        
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        int yPos = 190;
        if (kpiLabels != null && kpiValues != null) {
            for (int i = 0; i < Math.min(kpiLabels.length, kpiValues.length); i++) {
                canvas.drawText("• " + kpiLabels[i] + ": " + kpiValues[i], 70, yPos, paint);
                yPos += 20;
            }
        }
        
        // Pie de página
        paint.setTextSize(10);
        paint.setColor(Color.GRAY);
        canvas.drawText("DroidTour SuperAdmin Dashboard - Confidencial", 50, 800, paint);
        
        document.finishPage(page);
        
        // Guardar archivo
        String fileName = "Reporte_Analytics_" + timestamp + ".pdf";
        File file = new File(directory, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        document.close();
        fos.close();
        
        return file.getAbsolutePath();
    }
    
    /**
     * Exporta imágenes de gráficos usando MediaStore
     */
    private List<String> exportImagesWithMediaStore(List<ChartInfo> charts, String timestamp) throws IOException {
        List<String> imagePaths = new ArrayList<>();
        int chartWidth = 1200;
        int chartHeight = 800;
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        
        for (ChartInfo chartInfo : charts) {
            if (chartInfo.chart != null && chartInfo.chart instanceof View) {
                Bitmap chartBitmap = captureChartAsBitmap((View) chartInfo.chart, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = sanitizeFileName(chartInfo.title) + "_" + timestamp + ".png";
                    String path = saveChartBitmapWithMediaStore(chartBitmap, fileName, chartInfo.title, currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            }
        }
        
        return imagePaths;
    }
    
    /**
     * Exporta imágenes de gráficos usando sistema de archivos
     */
    private List<String> exportImagesWithFileSystem(List<ChartInfo> charts, String timestamp, File directory) throws IOException {
        List<String> imagePaths = new ArrayList<>();
        int chartWidth = 1200;
        int chartHeight = 800;
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        
        for (ChartInfo chartInfo : charts) {
            if (chartInfo.chart != null && chartInfo.chart instanceof View) {
                Bitmap chartBitmap = captureChartAsBitmap((View) chartInfo.chart, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = sanitizeFileName(chartInfo.title) + "_" + timestamp + ".png";
                    String path = saveChartBitmapWithFileSystem(chartBitmap, fileName, chartInfo.title, currentDate, directory);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            }
        }
        
        return imagePaths;
    }
    
    /**
     * Guarda bitmap con MediaStore
     */
    private String saveChartBitmapWithMediaStore(Bitmap chartBitmap, String fileName, String chartTitle, String currentDate) {
        try {
            int padding = 40;
            int titleHeight = 100;
            int width = chartBitmap.getWidth() + (padding * 2);
            int height = chartBitmap.getHeight() + titleHeight + (padding * 2);
            
            Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalBitmap);
            canvas.drawColor(Color.WHITE);
            
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            
            // Título
            paint.setTextSize(36);
            paint.setColor(context.getResources().getColor(R.color.primary));
            paint.setFakeBoldText(true);
            canvas.drawText(chartTitle, padding, padding + 50, paint);
            
            // Fecha
            paint.setTextSize(16);
            paint.setFakeBoldText(false);
            paint.setColor(Color.GRAY);
            canvas.drawText("Generado: " + currentDate, padding, padding + 80, paint);
            
            // Gráfico
            canvas.drawBitmap(chartBitmap, padding, titleHeight + padding, null);
            
            // Guardar con MediaStore
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DroidTour");
            
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                FileOutputStream fos = (FileOutputStream) context.getContentResolver().openOutputStream(uri);
                if (fos != null) {
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    finalBitmap.recycle();
                    return uri.toString();
                }
            }
            finalBitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error guardando gráfico con MediaStore: " + chartTitle, e);
        }
        return null;
    }
    
    /**
     * Guarda bitmap con sistema de archivos
     */
    private String saveChartBitmapWithFileSystem(Bitmap chartBitmap, String fileName, String chartTitle, 
                                                String currentDate, File directory) {
        try {
            int padding = 40;
            int titleHeight = 100;
            int width = chartBitmap.getWidth() + (padding * 2);
            int height = chartBitmap.getHeight() + titleHeight + (padding * 2);
            
            Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalBitmap);
            canvas.drawColor(Color.WHITE);
            
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            
            // Título
            paint.setTextSize(36);
            paint.setColor(context.getResources().getColor(R.color.primary));
            paint.setFakeBoldText(true);
            canvas.drawText(chartTitle, padding, padding + 50, paint);
            
            // Fecha
            paint.setTextSize(16);
            paint.setFakeBoldText(false);
            paint.setColor(Color.GRAY);
            canvas.drawText("Generado: " + currentDate, padding, padding + 80, paint);
            
            // Gráfico
            canvas.drawBitmap(chartBitmap, padding, titleHeight + padding, null);
            
            // Guardar archivo
            File file = new File(directory, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            finalBitmap.recycle();
            
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error guardando gráfico: " + chartTitle, e);
            return null;
        }
    }
    
    /**
     * Captura un gráfico como Bitmap
     */
    private Bitmap captureChartAsBitmap(View chartView, int width, int height) {
        if (chartView == null) return null;
        
        try {
            // Deshabilitar gestos temporalmente para captura limpia
            if (chartView instanceof LineChart) {
                LineChart chart = (LineChart) chartView;
                chart.setTouchEnabled(false);
                chart.setDragEnabled(false);
                chart.setScaleEnabled(false);
            } else if (chartView instanceof BarChart) {
                BarChart chart = (BarChart) chartView;
                chart.setTouchEnabled(false);
                chart.setDragEnabled(false);
                chart.setScaleEnabled(false);
            } else if (chartView instanceof PieChart) {
                PieChart chart = (PieChart) chartView;
                chart.setTouchEnabled(false);
                chart.setRotationEnabled(false);
            }
            
            // Usar dimensiones actuales del view
            int viewWidth = chartView.getWidth() > 0 ? chartView.getWidth() : width;
            int viewHeight = chartView.getHeight() > 0 ? chartView.getHeight() : height;
            
            if (viewWidth == 0 || viewHeight == 0) {
                chartView.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                );
                viewWidth = chartView.getMeasuredWidth();
                viewHeight = chartView.getMeasuredHeight();
                chartView.layout(0, 0, viewWidth, viewHeight);
            }
            
            // Crear bitmap
            Bitmap bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            chartView.draw(canvas);
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error capturando gráfico", e);
            return null;
        } finally {
            // Restaurar gestos a valores por defecto (habilitados)
            try {
                if (chartView instanceof LineChart) {
                    LineChart chart = (LineChart) chartView;
                    chart.setTouchEnabled(true);
                    chart.setDragEnabled(true);
                    chart.setScaleEnabled(true);
                    chart.invalidate();
                } else if (chartView instanceof BarChart) {
                    BarChart chart = (BarChart) chartView;
                    chart.setTouchEnabled(true);
                    chart.setDragEnabled(true);
                    chart.setScaleEnabled(true);
                    chart.invalidate();
                } else if (chartView instanceof PieChart) {
                    PieChart chart = (PieChart) chartView;
                    chart.setTouchEnabled(true);
                    chart.setRotationEnabled(true);
                    chart.invalidate();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error restaurando estado del gráfico", e);
            }
        }
    }
    
    /**
     * Sanitiza nombre de archivo
     */
    private String sanitizeFileName(String title) {
        return title.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
    
    /**
     * Clase para información de gráfico
     */
    public static class ChartInfo {
        public View chart;
        public String title;
        
        public ChartInfo(View chart, String title) {
            this.chart = chart;
            this.title = title;
        }
    }
    
    /**
     * Callback para exportación completa
     */
    public interface ExportCompleteCallback {
        void onSuccess(String pdfPath, List<String> imagePaths);
        void onError(Exception error);
    }
}
