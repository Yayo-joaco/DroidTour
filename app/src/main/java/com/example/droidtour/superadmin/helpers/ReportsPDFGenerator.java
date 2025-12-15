package com.example.droidtour.superadmin.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import com.example.droidtour.R;
import com.example.droidtour.models.Company;
import com.example.droidtour.superadmin.models.CompanyStats;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.data.BarData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generador de PDF para reportes de reservas
 */
public class ReportsPDFGenerator {
    
    private static final String TAG = "ReportsPDFGen";
    private final Context context;
    
    public ReportsPDFGenerator(Context context) {
        this.context = context;
    }
    
    /**
     * Genera PDF con estadísticas generales y por empresa
     */
    public void generateReportPDF(List<CompanyStats> companiesStats, int totalReservations,
                                  BarChart chart, int periodType, PDFGenerationCallback callback) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String periodName = getPeriodName(periodType);
            String fileName = "Reporte_Reservas_" + periodName + "_" + timestamp + ".pdf";
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                generatePDFWithMediaStore(companiesStats, totalReservations, chart, periodName, fileName, callback);
            } else {
                generatePDFWithFileSystem(companiesStats, totalReservations, chart, periodName, fileName, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    private String getPeriodName(int periodType) {
        switch (periodType) {
            case 0: return "Diario";
            case 1: return "Mensual";
            case 2: return "Anual";
            default: return "General";
        }
    }
    
    
    private void generatePDFWithMediaStore(List<CompanyStats> companiesStats, int totalReservations,
                                          BarChart chart, String periodName, String fileName,
                                          PDFGenerationCallback callback) {
        try {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            
            int yPos = 80;
            int margin = 50;
            int lineHeight = 20;
            int bottomMargin = 120; // Margen inferior considerable para evitar que el texto llegue al final
            int maxYPos = 842 - bottomMargin; // Altura máxima antes de cambiar de página (722)
            
            // Título
            paint.setTextSize(24);
            paint.setColor(context.getResources().getColor(R.color.primary));
            paint.setFakeBoldText(true);
            canvas.drawText("Reporte de Reservas - DroidTour", margin, yPos, paint);
            yPos += 30;
            
            // Fecha y período
            paint.setTextSize(14);
            paint.setColor(Color.BLACK);
            paint.setFakeBoldText(false);
            String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            canvas.drawText("Generado: " + currentDate, margin, yPos, paint);
            yPos += lineHeight;
            canvas.drawText("Período: " + periodName, margin, yPos, paint);
            yPos += 30;
            
            // Estadísticas generales
            paint.setTextSize(16);
            paint.setFakeBoldText(true);
            paint.setColor(Color.BLACK);
            canvas.drawText("Estadísticas Generales", margin, yPos, paint);
            yPos += lineHeight + 5;
            
            paint.setTextSize(12);
            paint.setFakeBoldText(false);
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            currencyFormat.setCurrency(java.util.Currency.getInstance("PEN"));
            
            // Calcular estadísticas generales desde todas las empresas
            int totalCompanies = 0;
            int totalConfirmed = 0;
            int totalInProgress = 0;
            int totalCompleted = 0;
            int totalCancelled = 0;
            int totalPeopleServed = 0;
            double totalRevenueAll = 0.0;
            int totalActiveTours = 0;
            int totalToursAll = 0;
            
            if (companiesStats != null && !companiesStats.isEmpty()) {
                totalCompanies = companiesStats.size();
                for (CompanyStats stats : companiesStats) {
                    totalConfirmed += stats.getConfirmedReservations();
                    totalInProgress += stats.getInProgressReservations();
                    totalCompleted += stats.getCompletedReservations();
                    totalCancelled += stats.getCancelledReservations();
                    totalPeopleServed += stats.getTotalPeopleServed();
                    totalRevenueAll += stats.getTotalRevenue();
                    totalActiveTours += stats.getActiveTours();
                    totalToursAll += stats.getTotalTours();
                }
            }
            
            // Mostrar estadísticas generales
            canvas.drawText("Total de Reservas: " + numberFormat.format(totalReservations), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Total de Empresas: " + totalCompanies, margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Reservas Confirmadas: " + numberFormat.format(totalConfirmed), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Reservas en Curso: " + numberFormat.format(totalInProgress), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Reservas Completadas: " + numberFormat.format(totalCompleted), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Reservas Canceladas: " + numberFormat.format(totalCancelled), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Total de Personas Atendidas: " + numberFormat.format(totalPeopleServed), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Ingresos Totales: " + currencyFormat.format(totalRevenueAll), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            if (totalReservations > 0) {
                double avgPrice = totalRevenueAll / totalReservations;
                canvas.drawText("Precio Promedio por Reserva: " + currencyFormat.format(avgPrice), margin + 20, yPos, paint);
                yPos += lineHeight;
            }
            
            canvas.drawText("Tours Activos: " + numberFormat.format(totalActiveTours), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Total de Tours: " + numberFormat.format(totalToursAll), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            yPos += 10;
            
            // Gráfico de Reportes
            paint.setTextSize(16);
            paint.setFakeBoldText(true);
            paint.setColor(Color.BLACK);
            canvas.drawText("Gráfico de Reportes", margin, yPos, paint);
            yPos += lineHeight + 10;
            
            // Capturar gráfico o mostrar ícono vacío
            Bitmap chartBitmap = null;
            boolean hasChartData = false;
            
            if (chart != null) {
                BarData chartData = chart.getData();
                if (chartData != null && chartData.getEntryCount() > 0) {
                    hasChartData = true;
                    chartBitmap = captureChartAsBitmap(chart, 500, 300);
                }
            }
            
            if (hasChartData && chartBitmap != null) {
                // Dibujar gráfico en el PDF
                int chartWidth = 495;
                int chartHeight = (int) (chartBitmap.getHeight() * (chartWidth / (float) chartBitmap.getWidth()));
                
                if (chartHeight > 250) {
                    chartHeight = 250;
                    chartWidth = (int) (chartBitmap.getWidth() * (chartHeight / (float) chartBitmap.getHeight()));
                }
                
                int chartX = margin + (495 - chartWidth) / 2;
                canvas.drawBitmap(Bitmap.createScaledBitmap(chartBitmap, chartWidth, chartHeight, true), 
                                 chartX, yPos, null);
                yPos += chartHeight + 15;
                
                chartBitmap.recycle();
            } else {
                // Mostrar texto "No hay reservas"
                paint.setTextSize(14);
                paint.setFakeBoldText(false);
                paint.setColor(Color.GRAY);
                String noDataText = "No hay reservas";
                float textWidth = paint.measureText(noDataText);
                canvas.drawText(noDataText, margin + (495 - (int)textWidth) / 2, yPos + 30, paint);
                yPos += 60;
            }
            
            yPos += 10;
            
            // Estadísticas por empresa
            if (companiesStats != null && !companiesStats.isEmpty()) {
                paint.setTextSize(16);
                paint.setFakeBoldText(true);
                paint.setColor(Color.BLACK);
                canvas.drawText("Estadísticas por Empresa", margin, yPos, paint);
                yPos += lineHeight + 5;
                
                paint.setTextSize(10);
                paint.setFakeBoldText(false);
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                
                int companyIndex = 0;
                for (CompanyStats stats : companiesStats) {
                    companyIndex++;
                    Log.d(TAG, "Procesando empresa " + companyIndex + " de " + companiesStats.size());
                    
                    if (stats == null || stats.getCompany() == null) {
                        Log.w(TAG, "CompanyStats o Company es null en índice " + companyIndex);
                        continue;
                    }
                    
                    if (yPos > maxYPos) {
                        // Nueva página si es necesario
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        yPos = 80;
                    }
                    
                    Company company = stats.getCompany();
                    Log.d(TAG, "Empresa: " + (company.getCommercialName() != null ? company.getCommercialName() : company.getBusinessName()));
                    
                    // Información de la empresa
                    paint.setTextSize(12);
                    paint.setFakeBoldText(true);
                    paint.setColor(context.getResources().getColor(R.color.primary));
                    
                    String companyName = company.getCommercialName();
                    if (companyName == null || companyName.isEmpty()) {
                        companyName = company.getBusinessName();
                    }
                    if (companyName == null || companyName.isEmpty()) {
                        companyName = "Sin nombre";
                    }
                    
                    canvas.drawText(companyName, margin + 10, yPos, paint);
                    yPos += lineHeight + 2;
                    
                    // Información detallada de la empresa
                    paint.setTextSize(9);
                    paint.setFakeBoldText(false);
                    paint.setColor(Color.BLACK);
                    
                    // Razón social
                    if (company.getBusinessName() != null && !company.getBusinessName().isEmpty()) {
                        canvas.drawText("  Razón Social: " + company.getBusinessName(), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // RUC y Tipo de negocio
                    if (company.getRuc() != null && !company.getRuc().isEmpty()) {
                        String rucInfo = "  RUC: " + company.getRuc();
                        if (company.getBusinessType() != null && !company.getBusinessType().isEmpty()) {
                            rucInfo += " (" + company.getBusinessType() + ")";
                        }
                        canvas.drawText(rucInfo, margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Email
                    if (company.getEmail() != null && !company.getEmail().isEmpty()) {
                        canvas.drawText("  Email: " + company.getEmail(), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Teléfono
                    if (company.getPhone() != null && !company.getPhone().isEmpty()) {
                        canvas.drawText("  Teléfono: " + company.getPhone(), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Dirección
                    if (company.getAddress() != null && !company.getAddress().isEmpty()) {
                        canvas.drawText("  Dirección: " + company.getAddress(), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Estado
                    String status = company.getStatus() != null ? company.getStatus() : "N/A";
                    canvas.drawText("  Estado: " + (status.equals("active") ? "Activa" : "Inactiva"), margin + 15, yPos, paint);
                    yPos += lineHeight - 2;
                    
                    // Fecha de creación
                    if (company.getCreatedAt() != null) {
                        canvas.drawText("  Fecha de Registro: " + dateFormat.format(company.getCreatedAt()), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Descripción (si cabe)
                    if (company.getDescription() != null && !company.getDescription().isEmpty() && yPos < 650) {
                        String description = company.getDescription();
                        if (description.length() > 80) {
                            description = description.substring(0, 77) + "...";
                        }
                        canvas.drawText("  Descripción: " + description, margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    yPos += 5;
                    
                    // Verificar espacio antes de agregar estadísticas de reservas (necesita ~200 píxeles)
                    if (yPos + 200 > maxYPos) {
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        yPos = 80;
                    }
                    
                    // Estadísticas de reservas
                    paint.setTextSize(10);
                    paint.setFakeBoldText(true);
                    paint.setColor(Color.BLACK);
                    canvas.drawText("  Estadísticas de Reservas:", margin + 15, yPos, paint);
                    yPos += lineHeight;
                    
                    paint.setFakeBoldText(false);
                    canvas.drawText("    • Total de Reservas: " + numberFormat.format(stats.getTotalReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Confirmadas: " + numberFormat.format(stats.getConfirmedReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • En Curso: " + numberFormat.format(stats.getInProgressReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Completadas: " + numberFormat.format(stats.getCompletedReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Canceladas: " + numberFormat.format(stats.getCancelledReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Total de Personas Atendidas: " + numberFormat.format(stats.getTotalPeopleServed()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Ingresos Totales: " + currencyFormat.format(stats.getTotalRevenue()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Precio Promedio por Reserva: " + currencyFormat.format(stats.getAveragePricePerReservation()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight;
                    
                    // Verificar espacio antes de agregar estadísticas de tours (necesita ~60 píxeles)
                    if (yPos + 60 > maxYPos) {
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        yPos = 80;
                    }
                    
                    // Estadísticas de tours
                    paint.setFakeBoldText(true);
                    canvas.drawText("  Estadísticas de Tours:", margin + 15, yPos, paint);
                    yPos += lineHeight;
                    
                    paint.setFakeBoldText(false);
                    canvas.drawText("    • Tours Activos: " + numberFormat.format(stats.getActiveTours()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Total de Tours: " + numberFormat.format(stats.getTotalTours()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight + 10;
                }
            }
            
            document.finishPage(page);
            
            // Guardar usando MediaStore
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
                    document.close();
                    if (callback != null) {
                        callback.onSuccess(uri.toString());
                    }
                    return;
                }
            }
            
            document.close();
            if (callback != null) {
                callback.onError(new IOException("No se pudo guardar el PDF"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF con MediaStore", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    private void generatePDFWithFileSystem(List<CompanyStats> companiesStats, int totalReservations,
                                         BarChart chart, String periodName, String fileName,
                                         PDFGenerationCallback callback) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File droidTourDir = new File(downloadsDir, "DroidTour");
            if (!droidTourDir.exists() && !droidTourDir.mkdirs()) {
                if (callback != null) {
                    callback.onError(new IOException("No se pudo crear el directorio de descarga"));
                }
                return;
            }
            
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            
            int yPos = 80;
            int margin = 50;
            int lineHeight = 20;
            int bottomMargin = 120; // Margen inferior considerable para evitar que el texto llegue al final
            int maxYPos = 842 - bottomMargin; // Altura máxima antes de cambiar de página (722)
            
            // Título
            paint.setTextSize(24);
            paint.setColor(context.getResources().getColor(R.color.primary));
            paint.setFakeBoldText(true);
            canvas.drawText("Reporte de Reservas - DroidTour", margin, yPos, paint);
            yPos += 30;
            
            // Fecha y período
            paint.setTextSize(14);
            paint.setColor(Color.BLACK);
            paint.setFakeBoldText(false);
            String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            canvas.drawText("Generado: " + currentDate, margin, yPos, paint);
            yPos += lineHeight;
            canvas.drawText("Período: " + periodName, margin, yPos, paint);
            yPos += 30;
            
            // Estadísticas generales
            paint.setTextSize(16);
            paint.setFakeBoldText(true);
            paint.setColor(Color.BLACK);
            canvas.drawText("Estadísticas Generales", margin, yPos, paint);
            yPos += lineHeight + 5;
            
            paint.setTextSize(12);
            paint.setFakeBoldText(false);
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            currencyFormat.setCurrency(java.util.Currency.getInstance("PEN"));
            
            // Calcular estadísticas generales desde todas las empresas
            int totalCompanies = 0;
            int totalConfirmed = 0;
            int totalInProgress = 0;
            int totalCompleted = 0;
            int totalCancelled = 0;
            int totalPeopleServed = 0;
            double totalRevenueAll = 0.0;
            int totalActiveTours = 0;
            int totalToursAll = 0;
            
            if (companiesStats != null && !companiesStats.isEmpty()) {
                totalCompanies = companiesStats.size();
                for (CompanyStats stats : companiesStats) {
                    totalConfirmed += stats.getConfirmedReservations();
                    totalInProgress += stats.getInProgressReservations();
                    totalCompleted += stats.getCompletedReservations();
                    totalCancelled += stats.getCancelledReservations();
                    totalPeopleServed += stats.getTotalPeopleServed();
                    totalRevenueAll += stats.getTotalRevenue();
                    totalActiveTours += stats.getActiveTours();
                    totalToursAll += stats.getTotalTours();
                }
            }
            
            // Mostrar estadísticas generales
            canvas.drawText("Total de Reservas: " + numberFormat.format(totalReservations), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Total de Empresas: " + totalCompanies, margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Reservas Confirmadas: " + numberFormat.format(totalConfirmed), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Reservas en Curso: " + numberFormat.format(totalInProgress), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Reservas Completadas: " + numberFormat.format(totalCompleted), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Reservas Canceladas: " + numberFormat.format(totalCancelled), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Total de Personas Atendidas: " + numberFormat.format(totalPeopleServed), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Ingresos Totales: " + currencyFormat.format(totalRevenueAll), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            if (totalReservations > 0) {
                double avgPrice = totalRevenueAll / totalReservations;
                canvas.drawText("Precio Promedio por Reserva: " + currencyFormat.format(avgPrice), margin + 20, yPos, paint);
                yPos += lineHeight;
            }
            
            canvas.drawText("Tours Activos: " + numberFormat.format(totalActiveTours), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            canvas.drawText("Total de Tours: " + numberFormat.format(totalToursAll), margin + 20, yPos, paint);
            yPos += lineHeight;
            
            yPos += 10;
            
            // Gráfico de Reportes
            paint.setTextSize(16);
            paint.setFakeBoldText(true);
            paint.setColor(Color.BLACK);
            canvas.drawText("Gráfico de Reportes", margin, yPos, paint);
            yPos += lineHeight + 10;
            
            // Capturar gráfico o mostrar texto vacío
            Bitmap chartBitmap = null;
            boolean hasChartData = false;
            
            if (chart != null) {
                BarData chartData = chart.getData();
                if (chartData != null && chartData.getEntryCount() > 0) {
                    hasChartData = true;
                    chartBitmap = captureChartAsBitmap(chart, 500, 300);
                }
            }
            
            if (hasChartData && chartBitmap != null) {
                // Dibujar gráfico en el PDF
                int chartWidth = 495;
                int chartHeight = (int) (chartBitmap.getHeight() * (chartWidth / (float) chartBitmap.getWidth()));
                
                if (chartHeight > 250) {
                    chartHeight = 250;
                    chartWidth = (int) (chartBitmap.getWidth() * (chartHeight / (float) chartBitmap.getHeight()));
                }
                
                int chartX = margin + (495 - chartWidth) / 2;
                canvas.drawBitmap(Bitmap.createScaledBitmap(chartBitmap, chartWidth, chartHeight, true), 
                                 chartX, yPos, null);
                yPos += chartHeight + 15;
                
                chartBitmap.recycle();
            } else {
                // Mostrar texto "No hay reservas"
                paint.setTextSize(14);
                paint.setFakeBoldText(false);
                paint.setColor(Color.GRAY);
                String noDataText = "No hay reservas";
                float textWidth = paint.measureText(noDataText);
                canvas.drawText(noDataText, margin + (495 - (int)textWidth) / 2, yPos + 30, paint);
                yPos += 60;
            }
            
            yPos += 10;
            
            // Estadísticas por empresa
            Log.d(TAG, "Generando PDF (FileSystem) con " + (companiesStats != null ? companiesStats.size() : 0) + " empresas");
            if (companiesStats != null && !companiesStats.isEmpty()) {
                paint.setTextSize(16);
                paint.setFakeBoldText(true);
                paint.setColor(Color.BLACK);
                canvas.drawText("Estadísticas por Empresa", margin, yPos, paint);
                yPos += lineHeight + 5;
                
                paint.setTextSize(10);
                paint.setFakeBoldText(false);
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                
                int companyIndex = 0;
                for (CompanyStats stats : companiesStats) {
                    companyIndex++;
                    Log.d(TAG, "Procesando empresa " + companyIndex + " de " + companiesStats.size() + " (FileSystem)");
                    
                    if (stats == null || stats.getCompany() == null) {
                        Log.w(TAG, "CompanyStats o Company es null en índice " + companyIndex);
                        continue;
                    }
                    if (yPos > maxYPos) {
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        yPos = 80;
                    }
                    
                    Company company = stats.getCompany();
                    
                    // Información de la empresa
                    paint.setTextSize(12);
                    paint.setFakeBoldText(true);
                    paint.setColor(context.getResources().getColor(R.color.primary));
                    
                    String companyName = company.getCommercialName();
                    if (companyName == null || companyName.isEmpty()) {
                        companyName = company.getBusinessName();
                    }
                    if (companyName == null || companyName.isEmpty()) {
                        companyName = "Sin nombre";
                    }
                    
                    canvas.drawText(companyName, margin + 10, yPos, paint);
                    yPos += lineHeight + 2;
                    
                    // Información detallada de la empresa
                    paint.setTextSize(9);
                    paint.setFakeBoldText(false);
                    paint.setColor(Color.BLACK);
                    
                    // Razón social
                    if (company.getBusinessName() != null && !company.getBusinessName().isEmpty()) {
                        canvas.drawText("  Razón Social: " + company.getBusinessName(), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // RUC y Tipo de negocio
                    if (company.getRuc() != null && !company.getRuc().isEmpty()) {
                        String rucInfo = "  RUC: " + company.getRuc();
                        if (company.getBusinessType() != null && !company.getBusinessType().isEmpty()) {
                            rucInfo += " (" + company.getBusinessType() + ")";
                        }
                        canvas.drawText(rucInfo, margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Email
                    if (company.getEmail() != null && !company.getEmail().isEmpty()) {
                        canvas.drawText("  Email: " + company.getEmail(), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Teléfono
                    if (company.getPhone() != null && !company.getPhone().isEmpty()) {
                        canvas.drawText("  Teléfono: " + company.getPhone(), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Dirección
                    if (company.getAddress() != null && !company.getAddress().isEmpty()) {
                        canvas.drawText("  Dirección: " + company.getAddress(), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Estado
                    String status = company.getStatus() != null ? company.getStatus() : "N/A";
                    canvas.drawText("  Estado: " + (status.equals("active") ? "Activa" : "Inactiva"), margin + 15, yPos, paint);
                    yPos += lineHeight - 2;
                    
                    // Fecha de creación
                    if (company.getCreatedAt() != null) {
                        canvas.drawText("  Fecha de Registro: " + dateFormat.format(company.getCreatedAt()), margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    // Descripción (si cabe)
                    if (company.getDescription() != null && !company.getDescription().isEmpty() && yPos < 650) {
                        String description = company.getDescription();
                        if (description.length() > 80) {
                            description = description.substring(0, 77) + "...";
                        }
                        canvas.drawText("  Descripción: " + description, margin + 15, yPos, paint);
                        yPos += lineHeight - 2;
                    }
                    
                    yPos += 5;
                    
                    // Verificar espacio antes de agregar estadísticas de reservas (necesita ~200 píxeles)
                    if (yPos + 200 > maxYPos) {
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        yPos = 80;
                    }
                    
                    // Estadísticas de reservas
                    paint.setTextSize(10);
                    paint.setFakeBoldText(true);
                    paint.setColor(Color.BLACK);
                    canvas.drawText("  Estadísticas de Reservas:", margin + 15, yPos, paint);
                    yPos += lineHeight;
                    
                    paint.setFakeBoldText(false);
                    canvas.drawText("    • Total de Reservas: " + numberFormat.format(stats.getTotalReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Confirmadas: " + numberFormat.format(stats.getConfirmedReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • En Curso: " + numberFormat.format(stats.getInProgressReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Completadas: " + numberFormat.format(stats.getCompletedReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Canceladas: " + numberFormat.format(stats.getCancelledReservations()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Total de Personas Atendidas: " + numberFormat.format(stats.getTotalPeopleServed()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Ingresos Totales: " + currencyFormat.format(stats.getTotalRevenue()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Precio Promedio por Reserva: " + currencyFormat.format(stats.getAveragePricePerReservation()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight;
                    
                    // Verificar espacio antes de agregar estadísticas de tours (necesita ~60 píxeles)
                    if (yPos + 60 > maxYPos) {
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        yPos = 80;
                    }
                    
                    // Estadísticas de tours
                    paint.setFakeBoldText(true);
                    canvas.drawText("  Estadísticas de Tours:", margin + 15, yPos, paint);
                    yPos += lineHeight;
                    
                    paint.setFakeBoldText(false);
                    canvas.drawText("    • Tours Activos: " + numberFormat.format(stats.getActiveTours()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight - 2;
                    canvas.drawText("    • Total de Tours: " + numberFormat.format(stats.getTotalTours()), 
                                   margin + 20, yPos, paint);
                    yPos += lineHeight + 10;
                }
            }
            
            document.finishPage(page);
            
            // Guardar archivo
            File file = new File(droidTourDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();
            
            if (callback != null) {
                callback.onSuccess(file.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF con sistema de archivos", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Captura un gráfico como Bitmap
     */
    private Bitmap captureChartAsBitmap(BarChart chart, int width, int height) {
        if (chart == null) return null;
        
        try {
            // Deshabilitar gestos temporalmente para captura limpia
            chart.setTouchEnabled(false);
            chart.setDragEnabled(false);
            chart.setScaleEnabled(false);
            
            // Usar dimensiones actuales del view
            int viewWidth = chart.getWidth() > 0 ? chart.getWidth() : width;
            int viewHeight = chart.getHeight() > 0 ? chart.getHeight() : height;
            
            if (viewWidth == 0 || viewHeight == 0) {
                chart.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                );
                viewWidth = chart.getMeasuredWidth();
                viewHeight = chart.getMeasuredHeight();
                chart.layout(0, 0, viewWidth, viewHeight);
            }
            
            // Crear bitmap
            Bitmap bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            chart.draw(canvas);
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error capturando gráfico", e);
            return null;
        } finally {
            // Restaurar gestos
            try {
                chart.setTouchEnabled(true);
                chart.setDragEnabled(true);
                chart.setScaleEnabled(true);
                chart.invalidate();
            } catch (Exception e) {
                Log.e(TAG, "Error restaurando estado del gráfico", e);
            }
        }
    }
    
    public interface PDFGenerationCallback {
        void onSuccess(String filePath);
        void onError(Exception error);
    }
}

