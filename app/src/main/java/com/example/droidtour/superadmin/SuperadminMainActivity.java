package com.example.droidtour.superadmin;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.superadmin.helpers.DashboardChartHelper;
import com.example.droidtour.superadmin.helpers.DashboardDateHelper;
import com.example.droidtour.superadmin.helpers.DashboardExportHelper;
import com.example.droidtour.superadmin.managers.DashboardDataRepository;
import com.example.droidtour.superadmin.managers.DashboardKPIManager;
import com.example.droidtour.utils.PreferencesManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity principal del dashboard de SuperAdmin
 * 
 * Versión refactorizada que elimina duplicación de código y delega responsabilidades
 * a helpers y managers especializados.
 */
public class SuperadminMainActivity extends AppCompatActivity 
        implements NavigationView.OnNavigationItemSelectedListener {
    
    private static final String TAG = "SuperadminMain";
    private static final String CHANNEL_ID = "export_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int TOTAL_QUERIES = 6; // 4 KPIs + 1 gráficos consolidados + 1 tours
    
    // ========== UI COMPONENTS ==========
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private MaterialCardView cardUserManagement, cardReports, cardLogs;
    private TabLayout tabLayout;
    private LineChart lineChartRevenue;
    private LineChart lineChartAveragePrice;
    private PieChart pieChartTours;
    private BarChart barChartBookings;
    private BarChart barChartPeople;
    private ExtendedFloatingActionButton fabExport;
    private TextView tvTotalUsers, tvActiveTours, tvRevenue, tvBookings;
    private TextView tvNotificationBadge;
    private ImageView ivAvatarAction;
    private FrameLayout notificationActionLayout, avatarActionLayout;
    private MaterialButton btnSelectDate;
    private LinearLayout dateSelectionContainer;
    private LinearLayout monthSelectionLayout;
    private LinearLayout yearSelectionLayout;
    private AutoCompleteTextView spinnerMonth;
    private AutoCompleteTextView spinnerYear;
    private AutoCompleteTextView spinnerYearOnly;
    private TextInputLayout tilMonth, tilYear, tilYearOnly;
    private SwipeRefreshLayout swipeRefresh;
    
    // ========== STATE ==========
    private int notificationCount = 3;
    private Date selectedDate = null;
    private int currentPeriodType = 0; // 0=Día, 1=Semana, 2=Mes, 3=Año
    private int selectedMonth = -1;
    private int selectedYear = -1;
    private int completedQueries = 0;
    
    // ========== MANAGERS & HELPERS ==========
    private PreferencesManager prefsManager;
    private FirebaseFirestore db;
    private DashboardChartHelper chartHelper;
    private DashboardDataRepository dataRepository;
    private DashboardKPIManager kpiManager;
    private DashboardExportHelper exportHelper;
    
    // ========== LIFECYCLE ==========
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar managers PRIMERO
        prefsManager = new PreferencesManager(this);
        db = FirebaseFirestore.getInstance();
        chartHelper = new DashboardChartHelper(this);
        dataRepository = new DashboardDataRepository(db);
        kpiManager = new DashboardKPIManager(db);
        exportHelper = new DashboardExportHelper(this);
        
        // Validar sesión
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar tipo de usuario
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("SUPERADMIN") && !userType.equals("ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_superadmin_main);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        initViews();
        setupDrawer();
        setupTabs();
        setupCharts();
        setupFAB();
        setupSwipeRefresh();
        updateKPIs();
        loadUserDataInDrawer();
    }
    
    // ========== INITIALIZATION ==========
    
    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        cardUserManagement = findViewById(R.id.card_user_management);
        cardReports = findViewById(R.id.card_reports);
        cardLogs = findViewById(R.id.card_logs);
        tabLayout = findViewById(R.id.tab_layout);
        lineChartRevenue = findViewById(R.id.line_chart_revenue);
        lineChartAveragePrice = findViewById(R.id.line_chart_average_price);
        pieChartTours = findViewById(R.id.pie_chart_tours);
        barChartBookings = findViewById(R.id.bar_chart_bookings);
        barChartPeople = findViewById(R.id.bar_chart_people);
        fabExport = findViewById(R.id.fab_export);
        tvTotalUsers = findViewById(R.id.tv_total_users);
        tvActiveTours = findViewById(R.id.tv_active_tours);
        tvRevenue = findViewById(R.id.tv_revenue);
        tvBookings = findViewById(R.id.tv_bookings);
        btnSelectDate = findViewById(R.id.btn_select_date);
        dateSelectionContainer = findViewById(R.id.date_selection_container);
        monthSelectionLayout = findViewById(R.id.month_selection_layout);
        yearSelectionLayout = findViewById(R.id.year_selection_layout);
        spinnerMonth = findViewById(R.id.spinner_month);
        spinnerYear = findViewById(R.id.spinner_year);
        spinnerYearOnly = findViewById(R.id.spinner_year_only);
        tilMonth = findViewById(R.id.til_month);
        tilYear = findViewById(R.id.til_year);
        tilYearOnly = findViewById(R.id.til_year_only);
        swipeRefresh = findViewById(R.id.swipe_refresh);
    }
    
    private void setupDrawer() {
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }
    
    private void loadUserDataInDrawer() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null && prefsManager != null && prefsManager.sesionActiva()) {
            TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
            if (tvUserNameHeader != null) {
                String userName = prefsManager.obtenerUsuario();
                tvUserNameHeader.setText(userName != null && !userName.isEmpty() ? userName : "Gabrielle Ivonne");
            }
        }
    }
    
    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::refreshDashboardData);
        }
    }
    
    // ========== TABS & DATE SELECTION ==========
    
    private void setupTabs() {
        setupMonthSpinner();
        setupYearSpinners();
        
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentPeriodType = tab.getPosition();
                    updateSelectionUI();
                    
                    if (currentPeriodType == 2 || currentPeriodType == 3) {
                        initializeSpinnerDefaults();
                    }
                    
                    if (currentPeriodType == 0 || currentPeriodType == 1) {
                        selectedDate = null;
                    }
                    
                    loadAllDashboardDataForPeriod(currentPeriodType, getSelectedDateForPeriod());
                }
                
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}
                
                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
        
        if (btnSelectDate != null) {
            btnSelectDate.setOnClickListener(v -> showDatePickerDialog());
        }
        
        updateSelectionUI();
    }
    
    private void setupMonthSpinner() {
        if (spinnerMonth == null) return;
        
        String[] months = new String[]{
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            months
        );
        spinnerMonth.setAdapter(adapter);
        
        Calendar cal = Calendar.getInstance();
        spinnerMonth.setText(months[cal.get(Calendar.MONTH)], false);
        selectedMonth = cal.get(Calendar.MONTH);
        
        spinnerMonth.setOnItemClickListener((parent, view, position, id) -> {
            selectedMonth = position;
            if (currentPeriodType == 2) {
                loadAllDashboardDataForPeriod(currentPeriodType, getSelectedDateForPeriod());
            }
        });
    }
    
    private void setupYearSpinners() {
        if (spinnerYear != null) {
            setupYearSpinner(spinnerYear, true);
        }
        if (spinnerYearOnly != null) {
            setupYearSpinner(spinnerYearOnly, false);
        }
    }
    
    private void setupYearSpinner(AutoCompleteTextView spinner, boolean isForMonth) {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int startYear = 2020;
        int endYear = currentYear + 1;
        
        List<String> years = new ArrayList<>();
        for (int year = endYear; year >= startYear; year--) {
            years.add(String.valueOf(year));
        }
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            years
        );
        spinner.setAdapter(adapter);
        
        String currentYearStr = String.valueOf(currentYear);
        spinner.setText(currentYearStr, false);
        selectedYear = currentYear;
        
        spinner.setOnItemClickListener((parent, view, position, id) -> {
            try {
                String yearStr = (String) parent.getItemAtPosition(position);
                selectedYear = Integer.parseInt(yearStr);
                if (currentPeriodType == 2 || currentPeriodType == 3) {
                    loadAllDashboardDataForPeriod(currentPeriodType, getSelectedDateForPeriod());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parseando año", e);
            }
        });
    }
    
    private void initializeSpinnerDefaults() {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH);
        
        if (spinnerMonth != null) {
            String[] months = new String[]{
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            };
            if (currentMonth >= 0 && currentMonth < months.length) {
                spinnerMonth.setText(months[currentMonth], false);
                selectedMonth = currentMonth;
            }
        }
        
        if (spinnerYear != null) {
            android.widget.ArrayAdapter<String> adapter = (android.widget.ArrayAdapter<String>) spinnerYear.getAdapter();
            if (adapter != null) {
                String currentYearStr = String.valueOf(currentYear);
                for (int i = 0; i < adapter.getCount(); i++) {
                    String yearStr = adapter.getItem(i);
                    if (yearStr != null && yearStr.equals(currentYearStr)) {
                        spinnerYear.setText(yearStr, false);
                        selectedYear = currentYear;
                        break;
                    }
                }
            }
        }
        
        if (spinnerYearOnly != null) {
            android.widget.ArrayAdapter<String> adapter = (android.widget.ArrayAdapter<String>) spinnerYearOnly.getAdapter();
            if (adapter != null) {
                String currentYearStr = String.valueOf(currentYear);
                for (int i = 0; i < adapter.getCount(); i++) {
                    String yearStr = adapter.getItem(i);
                    if (yearStr != null && yearStr.equals(currentYearStr)) {
                        spinnerYearOnly.setText(yearStr, false);
                        if (selectedYear < 0) {
                            selectedYear = currentYear;
                        }
                        break;
                    }
                }
            }
        }
    }
    
    private void updateSelectionUI() {
        if (dateSelectionContainer == null) return;
        
        if (btnSelectDate != null) btnSelectDate.setVisibility(View.GONE);
        if (monthSelectionLayout != null) monthSelectionLayout.setVisibility(View.GONE);
        if (yearSelectionLayout != null) yearSelectionLayout.setVisibility(View.GONE);
        
        switch (currentPeriodType) {
            case 0:
            case 1:
                if (btnSelectDate != null) {
                    btnSelectDate.setVisibility(View.VISIBLE);
                    updateButtonText();
                }
                break;
            case 2:
                if (monthSelectionLayout != null) {
                    monthSelectionLayout.setVisibility(View.VISIBLE);
                }
                break;
            case 3:
                if (yearSelectionLayout != null) {
                    yearSelectionLayout.setVisibility(View.VISIBLE);
                }
                break;
        }
    }
    
    private Date getSelectedDateForPeriod() {
        Calendar cal = Calendar.getInstance();
        
        switch (currentPeriodType) {
            case 0:
            case 1:
                return selectedDate;
            case 2:
                if (selectedMonth >= 0 && selectedYear > 0) {
                    cal.set(Calendar.YEAR, selectedYear);
                    cal.set(Calendar.MONTH, selectedMonth);
                    cal.set(Calendar.DAY_OF_MONTH, 15);
                    return cal.getTime();
                }
                cal.set(Calendar.DAY_OF_MONTH, 15);
                return cal.getTime();
            case 3:
                if (selectedYear > 0) {
                    cal.set(Calendar.YEAR, selectedYear);
                    cal.set(Calendar.MONTH, 0);
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    return cal.getTime();
                }
                cal.set(Calendar.MONTH, 0);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                return cal.getTime();
        }
        
        return null;
    }
    
    private void updateButtonText() {
        if (btnSelectDate == null) return;
        
        String periodText = currentPeriodType == 0 ? "Día" : "Semana";
        String selectText = currentPeriodType == 0 ? "Seleccionar día" : "Seleccionar inicio de semana";
        
        if (selectedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            btnSelectDate.setText(periodText + ": " + sdf.format(selectedDate));
        } else {
            btnSelectDate.setText(selectText);
        }
    }
    
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTime(selectedDate);
        }
        
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                selectedDate = selectedCalendar.getTime();
                updateButtonText();
                loadAllDashboardDataForPeriod(currentPeriodType, selectedDate);
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }
    
    // ========== CHARTS SETUP ==========
    
    private void setupCharts() {
        if (lineChartRevenue != null) {
            setupLineChart(lineChartRevenue, "Ingresos (S/)", R.color.green);
        }
        if (lineChartAveragePrice != null) {
            setupLineChart(lineChartAveragePrice, "Precio Promedio (S/)", R.color.primary);
        }
        if (pieChartTours != null) {
            setupPieChart();
            loadToursByCategory();
        }
        if (barChartBookings != null) {
            setupBarChart(barChartBookings);
        }
        if (barChartPeople != null) {
            setupBarChart(barChartPeople);
        }
        
        initializeSpinnerDefaults();
        loadAllDashboardDataForPeriod(0, getSelectedDateForPeriod());
    }
    
    private void setupLineChart(LineChart lineChart, String label, int colorRes) {
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), label);
        dataSet.setColor(getResources().getColor(colorRes));
        dataSet.setCircleColor(getResources().getColor(colorRes));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(colorRes));
        dataSet.setFillAlpha(30);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black));
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.getLegend().setEnabled(false);
        
        com.github.mikephil.charting.components.XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.gray));
        
        lineChart.getAxisLeft().setTextColor(getResources().getColor(R.color.gray));
        lineChart.getAxisRight().setEnabled(false);
        lineChart.invalidate();
    }
    
    private void setupPieChart() {
        PieDataSet dataSet = new PieDataSet(new ArrayList<>(), "");
        dataSet.setColors(new int[]{
                getResources().getColor(R.color.primary),
                getResources().getColor(R.color.green),
                getResources().getColor(R.color.orange),
                getResources().getColor(R.color.gray),
                android.graphics.Color.parseColor("#9C27B0"),
                android.graphics.Color.parseColor("#FF5722"),
                android.graphics.Color.parseColor("#00BCD4")
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(android.graphics.Color.WHITE);
        
        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter());
        
        pieChartTours.setData(pieData);
        pieChartTours.getDescription().setEnabled(false);
        pieChartTours.setDrawHoleEnabled(true);
        pieChartTours.setHoleColor(android.graphics.Color.TRANSPARENT);
        pieChartTours.setHoleRadius(35f);
        pieChartTours.setTransparentCircleRadius(40f);
        
        com.github.mikephil.charting.components.Legend legend = pieChartTours.getLegend();
        legend.setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.CENTER);
        legend.setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextSize(10f);
        legend.setTextColor(getResources().getColor(R.color.black));
        pieChartTours.invalidate();
    }
    
    private void setupBarChart(BarChart barChart) {
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
        barChart.getLegend().setEnabled(false);
        
        com.github.mikephil.charting.components.XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.gray));
        
        barChart.getAxisLeft().setTextColor(getResources().getColor(R.color.gray));
        barChart.getAxisRight().setEnabled(false);
        barChart.invalidate();
    }
    
    // ========== DATA LOADING ==========
    
    private void loadAllDashboardDataForPeriod(int periodType, Date selectedDate) {
        if (dataRepository == null) {
            Log.w(TAG, "dataRepository es null");
            return;
        }
        
        DashboardDateHelper.DateRange dateRange = DashboardDateHelper.calculateDateRange(periodType, selectedDate);
        if (dateRange.startDate == null || dateRange.endDate == null) {
            Log.w(TAG, "Rango de fechas inválido");
            return;
        }
        
        dataRepository.loadAllDashboardDataForPeriod(periodType, selectedDate, 
            new DashboardDataRepository.DashboardDataCallback() {
                @Override
                public void onSuccess(Map<String, Map<String, ?>> processedData) {
                    @SuppressWarnings("unchecked")
                    Map<String, Double> dailyRevenue = (Map<String, Double>) processedData.get("revenue");
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> dailyBookings = (Map<String, Integer>) processedData.get("bookings");
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> dailyPeople = (Map<String, Integer>) processedData.get("people");
                    @SuppressWarnings("unchecked")
                    Map<String, Double> dailyAveragePrice = (Map<String, Double>) processedData.get("averagePrice");
                    
                    if (periodType == 3) {
                        chartHelper.updateLineChartWithMonthlyData(lineChartRevenue, dailyRevenue, dateRange);
                        chartHelper.updateBarChartWithMonthlyData(barChartBookings, dailyBookings, dateRange);
                        chartHelper.updateBarChartPeopleWithMonthlyData(barChartPeople, dailyPeople, dateRange);
                        chartHelper.updateLineChartAveragePriceWithMonthlyData(lineChartAveragePrice, dailyAveragePrice, dateRange);
                    } else {
                        chartHelper.updateLineChartWithDailyData(lineChartRevenue, dailyRevenue, dateRange);
                        chartHelper.updateBarChartWithDailyData(barChartBookings, dailyBookings, dateRange);
                        chartHelper.updateBarChartPeopleWithDailyData(barChartPeople, dailyPeople, dateRange);
                        chartHelper.updateLineChartAveragePriceWithDailyData(lineChartAveragePrice, dailyAveragePrice, dateRange);
                    }
                    
                    kpiManager.loadTotalRevenue(tvRevenue, currentPeriodType, getSelectedDateForPeriod(), 
                        SuperadminMainActivity.this::checkAndStopRefresh);
                    kpiManager.loadTotalBookings(tvBookings, currentPeriodType, getSelectedDateForPeriod(), 
                        SuperadminMainActivity.this::checkAndStopRefresh);
                    
                    if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
                        swipeRefresh.setRefreshing(false);
                    }
                }
                
                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error cargando datos", error);
                    updateAllChartsWithEmptyData();
                    if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
                        swipeRefresh.setRefreshing(false);
                    }
                }
            });
    }
    
    private void loadToursByCategory() {
        if (pieChartTours == null || dataRepository == null) {
            Log.w(TAG, "loadToursByCategory: pieChartTours o dataRepository es null");
            return;
        }
        
        dataRepository.loadToursByCategory(new DashboardDataRepository.ToursByCategoryCallback() {
            @Override
            public void onSuccess(Map<String, Integer> toursByCategory) {
                if (chartHelper != null) {
                    chartHelper.updatePieChartWithData(pieChartTours, toursByCategory);
                }
                checkAndStopRefresh();
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error cargando tours por categoría", error);
                if (chartHelper != null) {
                    chartHelper.updatePieChartWithEmptyData(pieChartTours);
                }
                checkAndStopRefresh();
            }
        });
    }
    
    private void updateAllChartsWithEmptyData() {
        if (chartHelper != null) {
            chartHelper.updateLineChartWithEmptyData(lineChartRevenue);
            chartHelper.updateBarChartWithEmptyData(barChartBookings);
            chartHelper.updateBarChartPeopleWithEmptyData(barChartPeople);
            chartHelper.updateLineChartAveragePriceWithEmptyData(lineChartAveragePrice);
        }
    }
    
    // ========== KPIs ==========
    
    private void updateKPIs() {
        if (kpiManager == null) return;
        
        kpiManager.loadTotalUsers(tvTotalUsers, this::checkAndStopRefresh);
        kpiManager.loadActiveTours(tvActiveTours, this::checkAndStopRefresh);
        kpiManager.loadTotalBookings(tvBookings, currentPeriodType, getSelectedDateForPeriod(), 
            this::checkAndStopRefresh);
        kpiManager.loadTotalRevenue(tvRevenue, currentPeriodType, getSelectedDateForPeriod(), 
            this::checkAndStopRefresh);
    }
    
    // ========== REFRESH MANAGEMENT ==========
    
    private void refreshDashboardData() {
        Log.d(TAG, "Iniciando recarga de datos del dashboard");
        startRefresh();
        updateKPIs();
        loadAllDashboardDataForPeriod(currentPeriodType, getSelectedDateForPeriod());
        loadToursByCategory();
    }
    
    private void startRefresh() {
        completedQueries = 0;
    }
    
    private void checkAndStopRefresh() {
        if (swipeRefresh == null || !swipeRefresh.isRefreshing()) {
            return;
        }
        
        completedQueries++;
        if (completedQueries >= TOTAL_QUERIES) {
            swipeRefresh.post(() -> {
                if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
                    swipeRefresh.setRefreshing(false);
                }
            });
            completedQueries = 0;
        }
    }
    
    // ========== EXPORT ==========
    
    private void setupFAB() {
        if (fabExport != null) {
            fabExport.setOnClickListener(v -> {
                Toast.makeText(this, "Iniciando exportación...", Toast.LENGTH_SHORT).show();
                exportAnalyticsReport();
                notificationCount++;
                updateNotificationBadge();
            });
        }
    }
    
    private void exportAnalyticsReport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }
        
        try {
            // Preparar datos de KPIs
            String[] kpiLabels = {"Total Usuarios", "Tours Activos", "Ingresos", "Reservas"};
            String[] kpiValues = {
                tvTotalUsers != null ? tvTotalUsers.getText().toString() : "N/A",
                tvActiveTours != null ? tvActiveTours.getText().toString() : "N/A",
                tvRevenue != null ? tvRevenue.getText().toString() : "N/A",
                tvBookings != null ? tvBookings.getText().toString() : "N/A"
            };
            
            // Preparar gráficos
            List<DashboardExportHelper.ChartInfo> charts = new ArrayList<>();
            if (lineChartRevenue != null && lineChartRevenue.getData() != null) {
                charts.add(new DashboardExportHelper.ChartInfo(lineChartRevenue, "Ingresos Mensuales"));
            }
            if (lineChartAveragePrice != null && lineChartAveragePrice.getData() != null) {
                charts.add(new DashboardExportHelper.ChartInfo(lineChartAveragePrice, "Precio Promedio por Persona"));
            }
            if (pieChartTours != null && pieChartTours.getData() != null) {
                charts.add(new DashboardExportHelper.ChartInfo(pieChartTours, "Tours por Categoría"));
            }
            if (barChartBookings != null && barChartBookings.getData() != null) {
                charts.add(new DashboardExportHelper.ChartInfo(barChartBookings, "Reservas por Mes"));
            }
            if (barChartPeople != null && barChartPeople.getData() != null) {
                charts.add(new DashboardExportHelper.ChartInfo(barChartPeople, "Personas por Mes"));
            }
            
            // Exportar usando helper
            exportHelper.exportAnalyticsReport(charts, kpiLabels, kpiValues, 
                new DashboardExportHelper.ExportCompleteCallback() {
                    @Override
                    public void onSuccess(String pdfPath, List<String> imagePaths) {
                        showExportSuccessNotification(pdfPath, imagePaths);
                        String message = "✅ Exportación completada\n" + 
                                       "1 PDF y " + imagePaths.size() + " imágenes guardadas\n" +
                                       "Archivos en Descargas/DroidTour";
                        Toast.makeText(SuperadminMainActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Error en exportación", error);
                        showExportErrorNotification();
                        Toast.makeText(SuperadminMainActivity.this, 
                            "❌ Error al exportar: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error en exportación", e);
            showExportErrorNotification();
            Toast.makeText(this, "❌ Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportAnalyticsReport();
            } else {
                Toast.makeText(this, "❌ Permiso denegado. No se puede exportar sin permisos de almacenamiento.", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void showExportSuccessNotification(String pdfPath, List<String> imagePaths) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        String pdfFileName = pdfPath;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pdfFileName = Uri.parse(pdfPath).getLastPathSegment();
            } else {
                pdfFileName = new File(pdfPath).getName();
            }
        } catch (Exception e) {
            // Ignorar
        }
        
        StringBuilder imagesText = new StringBuilder();
        imagesText.append("✅ PDF: ").append(pdfFileName != null ? pdfFileName : "Reporte_Analytics.pdf");
        imagesText.append("\n📊 Imágenes: ").append(imagePaths.size()).append(" gráficos");
        for (int i = 0; i < Math.min(imagePaths.size(), 3); i++) {
            String imagePath = imagePaths.get(i);
            String imageName = imagePath;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    imageName = Uri.parse(imagePath).getLastPathSegment();
                } else {
                    imageName = new File(imagePath).getName();
                }
            } catch (Exception e) {
                // Ignorar
            }
            imagesText.append("\n  • ").append(imageName != null ? imageName : "Gráfico " + (i + 1));
        }
        if (imagePaths.size() > 3) {
            imagesText.append("\n  ... y ").append(imagePaths.size() - 3).append(" más");
        }
        imagesText.append("\n📁 Ubicación: Descargas/DroidTour");
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download_24)
                .setContentTitle("✅ Exportación Completada")
                .setContentText("Reporte guardado en Descargas/DroidTour")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(imagesText.toString()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(getResources().getColor(R.color.primary));
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
    
    private void showExportErrorNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download_24)
                .setContentTitle("❌ Error en Exportación")
                .setContentText("No se pudo completar la exportación")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setColor(android.graphics.Color.RED);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
        }
    }
    
    // ========== MENU ==========
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_general, menu);
        setupVisualMenuElements(menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_notifications) {
            Toast.makeText(this, "POR IMPLEMENTAR", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, SuperadminMyAccount.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void setupVisualMenuElements(Menu menu) {
        MenuItem notificationItem = menu.findItem(R.id.action_notifications);
        if (notificationItem != null) {
            notificationActionLayout = (FrameLayout) notificationItem.getActionView();
            if (notificationActionLayout != null) {
                tvNotificationBadge = notificationActionLayout.findViewById(R.id.tv_notification_badge);
                updateNotificationBadge();
                notificationActionLayout.setOnClickListener(v ->
                        Toast.makeText(this, "por implementr xd", Toast.LENGTH_SHORT).show());
            }
        }
        
        MenuItem avatarItem = menu.findItem(R.id.action_profile);
        if (avatarItem != null) {
            avatarActionLayout = (FrameLayout) avatarItem.getActionView();
            if (avatarActionLayout != null) {
                ivAvatarAction = avatarActionLayout.findViewById(R.id.iv_avatar_action);
                avatarActionLayout.setOnClickListener(v -> {
                    startActivity(new Intent(this, SuperadminMyAccount.class));
                });
            }
        }
    }
    
    private void updateNotificationBadge() {
        if (tvNotificationBadge != null) {
            if (notificationCount > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(String.valueOf(Math.min(notificationCount, 9)));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        }
    }
    
    // ========== NAVIGATION ==========
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_user_management) {
            startActivity(new Intent(this, SuperadminUsersActivity.class));
        } else if (id == R.id.nav_reports) {
            startActivity(new Intent(this, SuperadminReportsActivity.class));
        } else if (id == R.id.nav_logs) {
            startActivity(new Intent(this, SuperadminLogsActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, SuperadminProfileActivity.class));
        } else if (id == R.id.nav_logout) {
            prefsManager.cerrarSesion();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
        }
        
        drawerLayout.closeDrawers();
        return true;
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
    
    // ========== UTILITIES ==========
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
