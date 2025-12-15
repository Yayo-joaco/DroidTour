package com.example.droidtour.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.superadmin.helpers.NotificationHelper;
import com.example.droidtour.superadmin.helpers.ReportsChartHelper;
import com.example.droidtour.superadmin.helpers.ReportsDataLoader;
import com.example.droidtour.superadmin.helpers.ReportsDataProcessor;
import com.example.droidtour.superadmin.helpers.ReportsPDFGenerator;
import com.example.droidtour.superadmin.managers.CompanyStatsRepository;
import com.example.droidtour.superadmin.managers.ReportsDataRepository;
import com.example.droidtour.utils.PreferencesManager;
import com.github.mikephil.charting.charts.BarChart;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SuperadminReportsActivity extends AppCompatActivity {
    
    private static final String TAG = "SuperadminReports";
    
    private PreferencesManager prefsManager;
    private TextView tvTotalReservations;
    private MaterialCardView cardReservationsReports;
    private BarChart barChartReservations;
    private View layoutChartEmpty;
    private ChipGroup chipGroupPeriod;
    private Chip chipDaily, chipMonthly, chipYearly;
    private MaterialButton btnGenerateReport;
    
    private ReportsDataLoader dataLoader;
    private ReportsDataRepository reportsDataRepository;
    private ReportsChartHelper chartHelper;
    private ReportsPDFGenerator pdfGenerator;
    private CompanyStatsRepository companyStatsRepository;
    private NotificationHelper notificationHelper;
    
    private int currentPeriodType = ReportsDataProcessor.PERIOD_MONTHLY; // Por defecto mensual
    private int totalReservationsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea SUPERADMIN o ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("SUPERADMIN") && !userType.equals("ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_superadmin_reports);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
        loadReportsData();
        
        // Inicializar estado vacío del gráfico
        if (layoutChartEmpty != null) {
            layoutChartEmpty.setVisibility(View.VISIBLE);
        }
        if (barChartReservations != null) {
            barChartReservations.setVisibility(View.GONE);
        }
        
        loadChartData();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void initializeViews() {
        tvTotalReservations = findViewById(R.id.tv_total_reservations);
        cardReservationsReports = findViewById(R.id.card_reservations_reports);
        barChartReservations = findViewById(R.id.bar_chart_reservations);
        layoutChartEmpty = findViewById(R.id.layout_chart_empty);
        chipGroupPeriod = findViewById(R.id.chip_group_period);
        chipDaily = findViewById(R.id.chip_daily);
        chipMonthly = findViewById(R.id.chip_monthly);
        chipYearly = findViewById(R.id.chip_yearly);
        btnGenerateReport = findViewById(R.id.btn_generate_report);
        
        // Inicializar helpers y repositories
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        dataLoader = new ReportsDataLoader(db);
        reportsDataRepository = new ReportsDataRepository(db);
        chartHelper = new ReportsChartHelper(this);
        pdfGenerator = new ReportsPDFGenerator(this);
        companyStatsRepository = new CompanyStatsRepository(db);
        notificationHelper = new NotificationHelper(this);
    }
    
    private void setupClickListeners() {
        if (cardReservationsReports != null) {
            cardReservationsReports.setOnClickListener(v -> {
                Intent intent = new Intent(this, CompaniesReportsActivity.class);
                startActivity(intent);
            });
        }
        
        // Listener para filtros de período (solo una opción puede estar activa)
        if (chipGroupPeriod != null) {
            chipGroupPeriod.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == View.NO_ID) {
                    // Si se deselecciona todo, mantener la selección anterior o usar mensual por defecto
                    return;
                }
                
                if (checkedId == R.id.chip_daily) {
                    currentPeriodType = ReportsDataProcessor.PERIOD_DAILY;
                } else if (checkedId == R.id.chip_monthly) {
                    currentPeriodType = ReportsDataProcessor.PERIOD_MONTHLY;
                } else if (checkedId == R.id.chip_yearly) {
                    currentPeriodType = ReportsDataProcessor.PERIOD_YEARLY;
                }
                
                Log.d(TAG, "Período seleccionado: " + currentPeriodType);
                loadChartData();
            });
        }
        
        // Listener para botón generar reporte
        if (btnGenerateReport != null) {
            btnGenerateReport.setOnClickListener(v -> generatePDFReport());
        }
    }
    
    private void loadReportsData() {
        dataLoader.loadTotalReservations(new ReportsDataLoader.DataLoadCallback() {
            @Override
            public void onTotalReservationsLoaded(int total) {
                updateTotalReservations(total);
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error cargando datos de reportes", error);
                Toast.makeText(SuperadminReportsActivity.this, 
                    "Error al cargar datos", Toast.LENGTH_SHORT).show();
                // Mostrar 0 en caso de error
                updateTotalReservations(0);
            }
        });
    }
    
    private void updateTotalReservations(int total) {
        totalReservationsCount = total;
        if (tvTotalReservations != null) {
            // Formatear número con separadores de miles
            String formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(total);
            tvTotalReservations.setText(formatted);
        }
    }
    
    private void loadChartData() {
        reportsDataRepository.loadReservationsForPeriod(currentPeriodType, null, 
            new ReportsDataRepository.ReportsDataCallback() {
                @Override
                public void onSuccess(QuerySnapshot reservationsSnapshot) {
                    if (reservationsSnapshot != null) {
                        // Procesar datos según período
                        Map<String, Integer> periodData = ReportsDataProcessor.processReservationsByPeriod(
                            reservationsSnapshot, currentPeriodType);
                        
                        // Actualizar gráfico y estado vacío
                        if (barChartReservations != null && chartHelper != null) {
                            if (periodData != null && !periodData.isEmpty()) {
                                chartHelper.updateBarChartWithPeriodData(barChartReservations, periodData, currentPeriodType);
                                showChartEmptyState(false);
                            } else {
                                chartHelper.updateBarChartWithPeriodData(barChartReservations, periodData, currentPeriodType);
                                showChartEmptyState(true);
                            }
                        }
                    } else {
                        showChartEmptyState(true);
                    }
                }
                
                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error cargando datos para gráfico", error);
                    Toast.makeText(SuperadminReportsActivity.this,
                        "Error al cargar datos del gráfico", Toast.LENGTH_SHORT).show();
                    showChartEmptyState(true);
                }
            });
    }
    
    private void showChartEmptyState(boolean show) {
        if (barChartReservations != null) {
            barChartReservations.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (layoutChartEmpty != null) {
            layoutChartEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    private void generatePDFReport() {
        btnGenerateReport.setEnabled(false);
        btnGenerateReport.setText("Generando...");
        
        // Cargar estadísticas de empresas para el PDF
        companyStatsRepository.loadCompaniesWithStats(new CompanyStatsRepository.CompanyStatsCallback() {
            @Override
            public void onSuccess(List<com.example.droidtour.superadmin.models.CompanyStats> companiesStats) {
                Log.d(TAG, "Empresas cargadas para PDF: " + (companiesStats != null ? companiesStats.size() : 0));
                if (companiesStats == null || companiesStats.isEmpty()) {
                    btnGenerateReport.setEnabled(true);
                    btnGenerateReport.setText("Generar Reporte");
                    Toast.makeText(SuperadminReportsActivity.this,
                        "No hay empresas para incluir en el reporte",
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Generar PDF con estadísticas
                pdfGenerator.generateReportPDF(companiesStats, totalReservationsCount, 
                    barChartReservations, currentPeriodType, 
                    new ReportsPDFGenerator.PDFGenerationCallback() {
                        @Override
                        public void onSuccess(String filePath) {
                            btnGenerateReport.setEnabled(true);
                            btnGenerateReport.setText("Generar Reporte");
                            // Mostrar notificación
                            if (notificationHelper != null) {
                                notificationHelper.showReportPDFSuccessNotification(filePath);
                            }
                            Toast.makeText(SuperadminReportsActivity.this,
                                "Reporte generado exitosamente",
                                Toast.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            btnGenerateReport.setEnabled(true);
                            btnGenerateReport.setText("Generar Reporte");
                            Log.e(TAG, "Error generando PDF", error);
                            // Mostrar notificación de error
                            if (notificationHelper != null) {
                                notificationHelper.showReportPDFErrorNotification(
                                    error.getMessage() != null ? error.getMessage() : "Error desconocido");
                            }
                            Toast.makeText(SuperadminReportsActivity.this,
                                "Error al generar reporte: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        }
                    });
            }
            
            @Override
            public void onError(Exception error) {
                btnGenerateReport.setEnabled(true);
                btnGenerateReport.setText("Generar Reporte");
                Log.e(TAG, "Error cargando estadísticas de empresas", error);
                Toast.makeText(SuperadminReportsActivity.this,
                    "Error al cargar estadísticas: " + error.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
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
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
