package com.example.droidtour.superadmin;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

import com.example.droidtour.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class SuperadminMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String CHANNEL_ID = "export_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Variables originales (MANTENER IGUAL)
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
    private int notificationCount = 3;
    private PreferencesManager prefsManager;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new PreferencesManager(this);
        
        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();
        
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
        updateKPIs();
        loadUserDataInDrawer();
    }

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
            // Abrir pantalla de "Mi cuenta" al seleccionar la opción de perfil
            Intent intentProfileMenu = new Intent(this, SuperadminMyAccount.class);
            startActivity(intentProfileMenu);
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
                    Intent intentProfileMenu = new Intent(this, SuperadminMyAccount.class);
                    startActivity(intentProfileMenu);
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
        // Actualizar nombre de usuario en el header del drawer
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null && prefsManager != null && prefsManager.sesionActiva()) {
            TextView tvUserNameHeader = headerView.findViewById(R.id.tv_user_name_header);
            if (tvUserNameHeader != null) {
                String userName = prefsManager.obtenerUsuario();
                if (userName != null && !userName.isEmpty()) {
                    tvUserNameHeader.setText(userName);
                } else {
                    tvUserNameHeader.setText("Gabrielle Ivonne");
                }
            }
        }
    }

    private void setupTabs() {
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    updateDataForPeriod(tab.getPosition());
                }
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}
                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    private void setupCharts() {
        if (lineChartRevenue != null) {
            setupLineChart();
            loadMonthlyRevenue(); // Cargar datos reales desde Firestore
        }
        if (pieChartTours != null) {
            setupPieChart();
            loadToursByCategory(); // Cargar datos reales desde Firestore
        }
        if (barChartBookings != null) {
            setupBarChart();
            loadMonthlyBookings(); // Cargar datos reales desde Firestore
        }
        if (barChartPeople != null) {
            setupBarChartPeople();
            loadMonthlyPeople(); // Cargar datos reales desde Firestore
        }
        if (lineChartAveragePrice != null) {
            setupLineChartAveragePrice();
            loadMonthlyAveragePrice(); // Cargar datos reales desde Firestore
        }
    }

    private void setupLineChart() {
        // Configuración inicial del gráfico (se actualizará con datos reales)
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "Ingresos (S/)");
        dataSet.setColor(getResources().getColor(R.color.green));
        dataSet.setCircleColor(getResources().getColor(R.color.green));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.green));
        dataSet.setFillAlpha(30);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black));

        LineData lineData = new LineData(dataSet);
        lineChartRevenue.setData(lineData);
        lineChartRevenue.getDescription().setEnabled(false);
        lineChartRevenue.setTouchEnabled(true);
        lineChartRevenue.setDragEnabled(true);
        lineChartRevenue.setScaleEnabled(true);
        lineChartRevenue.getLegend().setEnabled(false);

        XAxis xAxis = lineChartRevenue.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.gray));

        lineChartRevenue.getAxisLeft().setTextColor(getResources().getColor(R.color.gray));
        lineChartRevenue.getAxisRight().setEnabled(false);
        lineChartRevenue.invalidate();
    }

    private void loadMonthlyRevenue() {
        if (lineChartRevenue == null || db == null) {
            android.util.Log.w("SuperadminMain", "loadMonthlyRevenue: lineChartRevenue o db es null");
            return;
        }

        android.util.Log.d("SuperadminMain", "Iniciando carga de ingresos mensuales desde Firestore");

        // Consultar todas las reservas con status válido
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            // Agrupar ingresos por mes usando tourDate
                            Map<String, Double> monthlyRevenue = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                // Verificar paymentStatus
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    // Obtener tourDate (formato: "YYYY-MM-DD")
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        // Extraer año-mes (YYYY-MM) de tourDate
                                        String yearMonth = extractYearMonth(tourDate);
                                        
                                        // Obtener totalPrice
                                        Object priceObj = doc.get("totalPrice");
                                        if (priceObj != null) {
                                            double price = 0.0;
                                            if (priceObj instanceof Double) {
                                                price = (Double) priceObj;
                                            } else if (priceObj instanceof Long) {
                                                price = ((Long) priceObj).doubleValue();
                                            } else if (priceObj instanceof Number) {
                                                price = ((Number) priceObj).doubleValue();
                                            } else if (priceObj instanceof String) {
                                                try {
                                                    price = Double.parseDouble((String) priceObj);
                                                } catch (NumberFormatException e) {
                                                    android.util.Log.w("SuperadminMain", "No se pudo parsear totalPrice: " + priceObj);
                                                }
                                            }
                                            
                                            // Sumar al mes correspondiente
                                            monthlyRevenue.put(yearMonth, monthlyRevenue.getOrDefault(yearMonth, 0.0) + price);
                                        }
                                    }
                                }
                            }
                            
                            // Actualizar gráfico con datos reales
                            updateLineChartWithData(monthlyRevenue);
                            
                            android.util.Log.d("SuperadminMain", "Ingresos mensuales cargados: " + monthlyRevenue.size() + " meses");
                        } else {
                            android.util.Log.w("SuperadminMain", "QuerySnapshot de ingresos mensuales es null");
                            updateLineChartWithEmptyData();
                        }
                    } else {
                        // Si falla con whereIn, intentar sin filtro
                        android.util.Log.w("SuperadminMain", "Error con whereIn, intentando sin filtro", task.getException());
                        loadMonthlyRevenueWithoutFilter();
                    }
                });
    }

    private void loadMonthlyRevenueWithoutFilter() {
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Map<String, Double> monthlyRevenue = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String status = doc.getString("status");
                                String paymentStatus = doc.getString("paymentStatus");
                                
                                if (status != null && 
                                    (status.equals("CONFIRMADA") || 
                                     status.equals("EN_CURSO") || 
                                     status.equals("COMPLETADA"))) {
                                    
                                    if (paymentStatus == null || 
                                        paymentStatus.equals("CONFIRMADO") || 
                                        paymentStatus.equals("COBRADO")) {
                                        
                                        String tourDate = doc.getString("tourDate");
                                        if (tourDate != null && !tourDate.isEmpty()) {
                                            String yearMonth = extractYearMonth(tourDate);
                                            
                                            Object priceObj = doc.get("totalPrice");
                                            if (priceObj != null) {
                                                double price = 0.0;
                                                if (priceObj instanceof Double) {
                                                    price = (Double) priceObj;
                                                } else if (priceObj instanceof Long) {
                                                    price = ((Long) priceObj).doubleValue();
                                                } else if (priceObj instanceof Number) {
                                                    price = ((Number) priceObj).doubleValue();
                                                } else if (priceObj instanceof String) {
                                                    try {
                                                        price = Double.parseDouble((String) priceObj);
                                                    } catch (NumberFormatException e) {
                                                        // Ignorar
                                                    }
                                                }
                                                
                                                monthlyRevenue.put(yearMonth, monthlyRevenue.getOrDefault(yearMonth, 0.0) + price);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            updateLineChartWithData(monthlyRevenue);
                        } else {
                            updateLineChartWithEmptyData();
                        }
                    } else {
                        android.util.Log.e("SuperadminMain", "Error cargando ingresos mensuales", task.getException());
                        updateLineChartWithEmptyData();
                    }
                });
    }

    private String extractYearMonth(String tourDate) {
        // tourDate formato: "YYYY-MM-DD" -> extraer "YYYY-MM"
        if (tourDate != null && tourDate.length() >= 7) {
            return tourDate.substring(0, 7); // "YYYY-MM"
        }
        return "";
    }

    private void updateLineChartWithData(Map<String, Double> monthlyRevenue) {
        if (lineChartRevenue == null) return;

        // Ordenar por año-mes (ascendente)
        TreeMap<String, Double> sortedRevenue = new TreeMap<>(monthlyRevenue);
        
        // Obtener los últimos 7 meses (o todos si hay menos)
        List<String> monthKeys = new ArrayList<>(sortedRevenue.keySet());
        int startIndex = Math.max(0, monthKeys.size() - 7);
        List<String> recentMonths = monthKeys.subList(startIndex, monthKeys.size());
        
        // Crear entradas para el gráfico
        List<Entry> entries = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        
        for (int i = 0; i < recentMonths.size(); i++) {
            String yearMonth = recentMonths.get(i);
            Double revenue = sortedRevenue.get(yearMonth);
            entries.add(new Entry(i, revenue != null ? revenue.floatValue() : 0f));
            
            // Formatear etiqueta del mes (ej: "2024-10" -> "Oct 2024")
            monthLabels.add(formatMonthLabel(yearMonth));
        }
        
        // Si no hay datos, mostrar mensaje vacío
        if (entries.isEmpty()) {
            updateLineChartWithEmptyData();
            return;
        }
        
        // Actualizar el gráfico
        LineDataSet dataSet = new LineDataSet(entries, "Ingresos (S/)");
        dataSet.setColor(getResources().getColor(R.color.green));
        dataSet.setCircleColor(getResources().getColor(R.color.green));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.green));
        dataSet.setFillAlpha(30);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black));
        
        LineData lineData = new LineData(dataSet);
        lineChartRevenue.setData(lineData);
        
        // Actualizar etiquetas del eje X
        XAxis xAxis = lineChartRevenue.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        
        lineChartRevenue.invalidate();
        
        android.util.Log.d("SuperadminMain", "Gráfico actualizado con " + entries.size() + " meses de datos");
    }

    private void updateLineChartWithEmptyData() {
        if (lineChartRevenue == null) return;
        
        // Mostrar gráfico vacío con mensaje
        List<Entry> entries = new ArrayList<>();
        LineDataSet dataSet = new LineDataSet(entries, "Ingresos (S/)");
        dataSet.setColor(getResources().getColor(R.color.green));
        
        LineData lineData = new LineData(dataSet);
        lineChartRevenue.setData(lineData);
        
        XAxis xAxis = lineChartRevenue.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Sin datos")));
        
        lineChartRevenue.invalidate();
    }

    private String formatMonthLabel(String yearMonth) {
        // Formatear "YYYY-MM" a "MMM YYYY" (ej: "2024-10" -> "Oct 2024")
        try {
            String[] parts = yearMonth.split("-");
            if (parts.length == 2) {
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[0]);
                
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month - 1); // Calendar.MONTH es 0-based
                
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                return sdf.format(cal.getTime());
            }
        } catch (Exception e) {
            android.util.Log.w("SuperadminMain", "Error formateando mes: " + yearMonth, e);
        }
        return yearMonth;
    }

    private void setupLineChartAveragePrice() {
        // Configuración inicial del gráfico (se actualizará con datos reales)
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "Precio Promedio (S/)");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setCircleColor(getResources().getColor(R.color.primary));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.primary));
        dataSet.setFillAlpha(30);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black));

        LineData lineData = new LineData(dataSet);
        lineChartAveragePrice.setData(lineData);
        lineChartAveragePrice.getDescription().setEnabled(false);
        lineChartAveragePrice.setTouchEnabled(true);
        lineChartAveragePrice.setDragEnabled(true);
        lineChartAveragePrice.setScaleEnabled(true);
        lineChartAveragePrice.getLegend().setEnabled(false);

        XAxis xAxis = lineChartAveragePrice.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.gray));

        lineChartAveragePrice.getAxisLeft().setTextColor(getResources().getColor(R.color.gray));
        lineChartAveragePrice.getAxisRight().setEnabled(false);
        lineChartAveragePrice.invalidate();
    }

    private void loadMonthlyAveragePrice() {
        if (lineChartAveragePrice == null || db == null) {
            android.util.Log.w("SuperadminMain", "loadMonthlyAveragePrice: lineChartAveragePrice o db es null");
            return;
        }

        android.util.Log.d("SuperadminMain", "Iniciando carga de precio promedio mensual desde Firestore");

        // Consultar todas las reservas con status válido
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            // Agrupar ingresos y personas por mes
                            Map<String, Double> monthlyRevenue = new HashMap<>();
                            Map<String, Integer> monthlyPeople = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                // Verificar paymentStatus
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    // Obtener tourDate (formato: "YYYY-MM-DD")
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        // Extraer año-mes (YYYY-MM) de tourDate
                                        String yearMonth = extractYearMonth(tourDate);
                                        
                                        // Obtener totalPrice
                                        Object priceObj = doc.get("totalPrice");
                                        if (priceObj != null) {
                                            double price = 0.0;
                                            if (priceObj instanceof Double) {
                                                price = (Double) priceObj;
                                            } else if (priceObj instanceof Long) {
                                                price = ((Long) priceObj).doubleValue();
                                            } else if (priceObj instanceof Number) {
                                                price = ((Number) priceObj).doubleValue();
                                            } else if (priceObj instanceof String) {
                                                try {
                                                    price = Double.parseDouble((String) priceObj);
                                                } catch (NumberFormatException e) {
                                                    android.util.Log.w("SuperadminMain", "No se pudo parsear totalPrice: " + priceObj);
                                                }
                                            }
                                            
                                            // Sumar ingresos por mes
                                            monthlyRevenue.put(yearMonth, 
                                                monthlyRevenue.getOrDefault(yearMonth, 0.0) + price);
                                        }
                                        
                                        // Obtener numberOfPeople
                                        Object peopleObj = doc.get("numberOfPeople");
                                        if (peopleObj != null) {
                                            int people = 0;
                                            if (peopleObj instanceof Integer) {
                                                people = (Integer) peopleObj;
                                            } else if (peopleObj instanceof Long) {
                                                people = ((Long) peopleObj).intValue();
                                            } else if (peopleObj instanceof Number) {
                                                people = ((Number) peopleObj).intValue();
                                            }
                                            
                                            // Sumar personas por mes
                                            monthlyPeople.put(yearMonth, 
                                                monthlyPeople.getOrDefault(yearMonth, 0) + people);
                                        }
                                    }
                                }
                            }
                            
                            // Calcular precio promedio por mes (ingresos / personas)
                            Map<String, Double> monthlyAveragePrice = new HashMap<>();
                            for (String yearMonth : monthlyRevenue.keySet()) {
                                Double revenue = monthlyRevenue.get(yearMonth);
                                Integer people = monthlyPeople.get(yearMonth);
                                
                                if (revenue != null && people != null && people > 0) {
                                    double averagePrice = revenue / people;
                                    monthlyAveragePrice.put(yearMonth, averagePrice);
                                }
                            }
                            
                            // Actualizar gráfico con datos reales
                            updateLineChartAveragePriceWithData(monthlyAveragePrice);
                            
                            android.util.Log.d("SuperadminMain", "Precio promedio mensual cargado: " + monthlyAveragePrice.size() + " meses");
                        } else {
                            android.util.Log.w("SuperadminMain", "QuerySnapshot de precio promedio mensual es null");
                            updateLineChartAveragePriceWithEmptyData();
                        }
                    } else {
                        // Si falla con whereIn, intentar sin filtro
                        android.util.Log.w("SuperadminMain", "Error con whereIn, intentando sin filtro", task.getException());
                        loadMonthlyAveragePriceWithoutFilter();
                    }
                });
    }

    private void loadMonthlyAveragePriceWithoutFilter() {
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Map<String, Double> monthlyRevenue = new HashMap<>();
                            Map<String, Integer> monthlyPeople = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String status = doc.getString("status");
                                String paymentStatus = doc.getString("paymentStatus");
                                
                                if (status != null && 
                                    (status.equals("CONFIRMADA") || 
                                     status.equals("EN_CURSO") || 
                                     status.equals("COMPLETADA"))) {
                                    
                                    if (paymentStatus == null || 
                                        paymentStatus.equals("CONFIRMADO") || 
                                        paymentStatus.equals("COBRADO")) {
                                        
                                        String tourDate = doc.getString("tourDate");
                                        if (tourDate != null && !tourDate.isEmpty()) {
                                            String yearMonth = extractYearMonth(tourDate);
                                            
                                            // Obtener totalPrice
                                            Object priceObj = doc.get("totalPrice");
                                            if (priceObj != null) {
                                                double price = 0.0;
                                                if (priceObj instanceof Double) {
                                                    price = (Double) priceObj;
                                                } else if (priceObj instanceof Long) {
                                                    price = ((Long) priceObj).doubleValue();
                                                } else if (priceObj instanceof Number) {
                                                    price = ((Number) priceObj).doubleValue();
                                                } else if (priceObj instanceof String) {
                                                    try {
                                                        price = Double.parseDouble((String) priceObj);
                                                    } catch (NumberFormatException e) {
                                                        // Ignorar
                                                    }
                                                }
                                                
                                                monthlyRevenue.put(yearMonth, 
                                                    monthlyRevenue.getOrDefault(yearMonth, 0.0) + price);
                                            }
                                            
                                            // Obtener numberOfPeople
                                            Object peopleObj = doc.get("numberOfPeople");
                                            if (peopleObj != null) {
                                                int people = 0;
                                                if (peopleObj instanceof Integer) {
                                                    people = (Integer) peopleObj;
                                                } else if (peopleObj instanceof Long) {
                                                    people = ((Long) peopleObj).intValue();
                                                } else if (peopleObj instanceof Number) {
                                                    people = ((Number) peopleObj).intValue();
                                                }
                                                
                                                monthlyPeople.put(yearMonth, 
                                                    monthlyPeople.getOrDefault(yearMonth, 0) + people);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Calcular precio promedio por mes
                            Map<String, Double> monthlyAveragePrice = new HashMap<>();
                            for (String yearMonth : monthlyRevenue.keySet()) {
                                Double revenue = monthlyRevenue.get(yearMonth);
                                Integer people = monthlyPeople.get(yearMonth);
                                
                                if (revenue != null && people != null && people > 0) {
                                    double averagePrice = revenue / people;
                                    monthlyAveragePrice.put(yearMonth, averagePrice);
                                }
                            }
                            
                            updateLineChartAveragePriceWithData(monthlyAveragePrice);
                        } else {
                            updateLineChartAveragePriceWithEmptyData();
                        }
                    } else {
                        android.util.Log.e("SuperadminMain", "Error cargando precio promedio mensual", task.getException());
                        updateLineChartAveragePriceWithEmptyData();
                    }
                });
    }

    private void updateLineChartAveragePriceWithData(Map<String, Double> monthlyAveragePrice) {
        if (lineChartAveragePrice == null) return;

        // Ordenar por año-mes (ascendente)
        TreeMap<String, Double> sortedAveragePrice = new TreeMap<>(monthlyAveragePrice);
        
        // Obtener los últimos 7 meses (o todos si hay menos)
        List<String> monthKeys = new ArrayList<>(sortedAveragePrice.keySet());
        int startIndex = Math.max(0, monthKeys.size() - 7);
        List<String> recentMonths = monthKeys.subList(startIndex, monthKeys.size());
        
        // Crear entradas para el gráfico
        List<Entry> entries = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        
        for (int i = 0; i < recentMonths.size(); i++) {
            String yearMonth = recentMonths.get(i);
            Double averagePrice = sortedAveragePrice.get(yearMonth);
            entries.add(new Entry(i, averagePrice != null ? averagePrice.floatValue() : 0f));
            
            // Formatear etiqueta del mes (ej: "2024-10" -> "Oct 2024")
            monthLabels.add(formatMonthLabel(yearMonth));
        }
        
        // Si no hay datos, mostrar mensaje vacío
        if (entries.isEmpty()) {
            updateLineChartAveragePriceWithEmptyData();
            return;
        }
        
        // Actualizar el gráfico
        LineDataSet dataSet = new LineDataSet(entries, "Precio Promedio (S/)");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setCircleColor(getResources().getColor(R.color.primary));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.primary));
        dataSet.setFillAlpha(30);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black));
        
        LineData lineData = new LineData(dataSet);
        lineChartAveragePrice.setData(lineData);
        
        // Actualizar etiquetas del eje X
        XAxis xAxis = lineChartAveragePrice.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        
        lineChartAveragePrice.invalidate();
        
        android.util.Log.d("SuperadminMain", "Gráfico de precio promedio actualizado con " + entries.size() + " meses de datos");
    }

    private void updateLineChartAveragePriceWithEmptyData() {
        if (lineChartAveragePrice == null) return;
        
        // Mostrar gráfico vacío con mensaje
        List<Entry> entries = new ArrayList<>();
        LineDataSet dataSet = new LineDataSet(entries, "Precio Promedio (S/)");
        dataSet.setColor(getResources().getColor(R.color.primary));
        
        LineData lineData = new LineData(dataSet);
        lineChartAveragePrice.setData(lineData);
        
        XAxis xAxis = lineChartAveragePrice.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Sin datos")));
        
        lineChartAveragePrice.invalidate();
    }

    private void loadMonthlyAveragePriceForPeriod(int periodPosition) {
        if (lineChartAveragePrice == null || db == null) {
            return;
        }

        // Calcular rango de fechas según el período
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();
        Date startDate;
        
        switch (periodPosition) {
            case 0: // Hoy
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startDate = cal.getTime();
                break;
            case 1: // Semana (últimos 7 días)
                cal.add(Calendar.DAY_OF_YEAR, -7);
                startDate = cal.getTime();
                break;
            case 2: // Mes (último mes)
                cal.add(Calendar.MONTH, -1);
                startDate = cal.getTime();
                break;
            case 3: // Año (último año)
                cal.add(Calendar.YEAR, -1);
                startDate = cal.getTime();
                break;
            default:
                startDate = null;
        }

        android.util.Log.d("SuperadminMain", "Cargando precio promedio para período: " + periodPosition + 
                          " desde " + startDate + " hasta " + endDate);

        // Consultar reservas con filtro de fecha
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Map<String, Double> monthlyRevenue = new HashMap<>();
                            Map<String, Integer> monthlyPeople = new HashMap<>();
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        try {
                                            // Parsear tourDate y verificar si está en el rango
                                            Date tourDateObj = sdf.parse(tourDate);
                                            if (tourDateObj != null && startDate != null) {
                                                if (tourDateObj.compareTo(startDate) >= 0 && 
                                                    tourDateObj.compareTo(endDate) <= 0) {
                                                    
                                                    String yearMonth = extractYearMonth(tourDate);
                                                    
                                                    // Obtener totalPrice
                                                    Object priceObj = doc.get("totalPrice");
                                                    if (priceObj != null) {
                                                        double price = 0.0;
                                                        if (priceObj instanceof Double) {
                                                            price = (Double) priceObj;
                                                        } else if (priceObj instanceof Long) {
                                                            price = ((Long) priceObj).doubleValue();
                                                        } else if (priceObj instanceof Number) {
                                                            price = ((Number) priceObj).doubleValue();
                                                        } else if (priceObj instanceof String) {
                                                            try {
                                                                price = Double.parseDouble((String) priceObj);
                                                            } catch (NumberFormatException e) {
                                                                // Ignorar
                                                            }
                                                        }
                                                        
                                                        monthlyRevenue.put(yearMonth, 
                                                            monthlyRevenue.getOrDefault(yearMonth, 0.0) + price);
                                                    }
                                                    
                                                    // Obtener numberOfPeople
                                                    Object peopleObj = doc.get("numberOfPeople");
                                                    if (peopleObj != null) {
                                                        int people = 0;
                                                        if (peopleObj instanceof Integer) {
                                                            people = (Integer) peopleObj;
                                                        } else if (peopleObj instanceof Long) {
                                                            people = ((Long) peopleObj).intValue();
                                                        } else if (peopleObj instanceof Number) {
                                                            people = ((Number) peopleObj).intValue();
                                                        }
                                                        
                                                        monthlyPeople.put(yearMonth, 
                                                            monthlyPeople.getOrDefault(yearMonth, 0) + people);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            android.util.Log.w("SuperadminMain", "Error parseando fecha: " + tourDate, e);
                                        }
                                    }
                                }
                            }
                            
                            // Calcular precio promedio por mes
                            Map<String, Double> monthlyAveragePrice = new HashMap<>();
                            for (String yearMonth : monthlyRevenue.keySet()) {
                                Double revenue = monthlyRevenue.get(yearMonth);
                                Integer people = monthlyPeople.get(yearMonth);
                                
                                if (revenue != null && people != null && people > 0) {
                                    double averagePrice = revenue / people;
                                    monthlyAveragePrice.put(yearMonth, averagePrice);
                                }
                            }
                            
                            updateLineChartAveragePriceWithData(monthlyAveragePrice);
                        } else {
                            updateLineChartAveragePriceWithEmptyData();
                        }
                    } else {
                        android.util.Log.e("SuperadminMain", "Error cargando precio promedio por período", task.getException());
                        updateLineChartAveragePriceWithEmptyData();
                    }
                });
    }

    private void setupPieChart() {
        // Configuración inicial del gráfico (se actualizará con datos reales)
        PieDataSet dataSet = new PieDataSet(new ArrayList<>(), "");
        dataSet.setColors(new int[]{
                getResources().getColor(R.color.primary),
                getResources().getColor(R.color.green),
                getResources().getColor(R.color.orange),
                getResources().getColor(R.color.gray),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#FF5722"),
                Color.parseColor("#00BCD4")
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter());

        pieChartTours.setData(pieData);
        pieChartTours.getDescription().setEnabled(false);
        pieChartTours.setDrawHoleEnabled(true);
        pieChartTours.setHoleColor(Color.TRANSPARENT);
        pieChartTours.setHoleRadius(35f);
        pieChartTours.setTransparentCircleRadius(40f);

        Legend legend = pieChartTours.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextSize(10f);
        legend.setTextColor(getResources().getColor(R.color.black));
        pieChartTours.invalidate();
    }

    private void loadToursByCategory() {
        if (pieChartTours == null || db == null) {
            android.util.Log.w("SuperadminMain", "loadToursByCategory: pieChartTours o db es null");
            return;
        }

        android.util.Log.d("SuperadminMain", "Iniciando carga de tours por categoría desde Firestore");

        // Consultar todos los tours activos
        db.collection(FirestoreManager.COLLECTION_TOURS)
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            // Agrupar tours por categoría
                            Map<String, Integer> toursByCategory = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String category = doc.getString("category");
                                
                                // Si no tiene categoría, usar "Sin categoría"
                                if (category == null || category.isEmpty()) {
                                    category = "Sin categoría";
                                }
                                
                                // Contar tours por categoría
                                toursByCategory.put(category, 
                                    toursByCategory.getOrDefault(category, 0) + 1);
                            }
                            
                            // Actualizar gráfico con datos reales
                            updatePieChartWithData(toursByCategory);
                            
                            android.util.Log.d("SuperadminMain", "Tours por categoría cargados: " + toursByCategory.size() + " categorías");
                        } else {
                            android.util.Log.w("SuperadminMain", "QuerySnapshot de tours por categoría es null");
                            updatePieChartWithEmptyData();
                        }
                    } else {
                        // Si falla con whereEqualTo, intentar sin filtro
                        android.util.Log.w("SuperadminMain", "Error con whereEqualTo, intentando sin filtro", task.getException());
                        loadToursByCategoryWithoutFilter();
                    }
                });
    }

    private void loadToursByCategoryWithoutFilter() {
        db.collection(FirestoreManager.COLLECTION_TOURS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Map<String, Integer> toursByCategory = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                // Verificar isActive
                                Boolean isActive = doc.getBoolean("isActive");
                                if (isActive == null || isActive) {
                                    String category = doc.getString("category");
                                    
                                    if (category == null || category.isEmpty()) {
                                        category = "Sin categoría";
                                    }
                                    
                                    toursByCategory.put(category, 
                                        toursByCategory.getOrDefault(category, 0) + 1);
                                }
                            }
                            
                            updatePieChartWithData(toursByCategory);
                        } else {
                            updatePieChartWithEmptyData();
                        }
                    } else {
                        android.util.Log.e("SuperadminMain", "Error cargando tours por categoría", task.getException());
                        updatePieChartWithEmptyData();
                    }
                });
    }

    private void updatePieChartWithData(Map<String, Integer> toursByCategory) {
        if (pieChartTours == null) return;

        // Si no hay datos, mostrar gráfico vacío
        if (toursByCategory.isEmpty()) {
            updatePieChartWithEmptyData();
            return;
        }

        // Crear entradas para el gráfico
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : toursByCategory.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        // Ordenar por valor (descendente) para mejor visualización
        entries.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        // Colores disponibles
        int[] colors = new int[]{
                getResources().getColor(R.color.primary),
                getResources().getColor(R.color.green),
                getResources().getColor(R.color.orange),
                getResources().getColor(R.color.gray),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#FF5722"),
                Color.parseColor("#00BCD4"),
                Color.parseColor("#795548"),
                Color.parseColor("#607D8B"),
                Color.parseColor("#E91E63")
        };

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f); // Espacio entre slices

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter());

        pieChartTours.setData(pieData);
        pieChartTours.invalidate();

        android.util.Log.d("SuperadminMain", "Gráfico de pie actualizado con " + entries.size() + " categorías");
    }

    private void updatePieChartWithEmptyData() {
        if (pieChartTours == null) return;

        // Mostrar gráfico vacío
        List<PieEntry> entries = new ArrayList<>();
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{getResources().getColor(R.color.gray)});

        PieData pieData = new PieData(dataSet);
        pieChartTours.setData(pieData);
        pieChartTours.invalidate();
    }

    private void setupBarChart() {
        // Configuración inicial del gráfico (se actualizará con datos reales)
        BarDataSet dataSet = new BarDataSet(new ArrayList<>(), "Reservas");
        dataSet.setColor(getResources().getColor(R.color.green));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        barChartBookings.setData(barData);
        barChartBookings.getDescription().setEnabled(false);
        barChartBookings.setFitBars(true);
        barChartBookings.getLegend().setEnabled(false);

        XAxis xAxis = barChartBookings.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.gray));

        barChartBookings.getAxisLeft().setTextColor(getResources().getColor(R.color.gray));
        barChartBookings.getAxisRight().setEnabled(false);
        barChartBookings.invalidate();
    }

    private void loadMonthlyBookings() {
        if (barChartBookings == null || db == null) {
            android.util.Log.w("SuperadminMain", "loadMonthlyBookings: barChartBookings o db es null");
            return;
        }

        android.util.Log.d("SuperadminMain", "Iniciando carga de reservas mensuales desde Firestore");

        // Consultar todas las reservas con status válido
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            // Agrupar reservas por mes usando tourDate
                            Map<String, Integer> monthlyBookings = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                // Verificar paymentStatus
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    // Obtener tourDate (formato: "YYYY-MM-DD")
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        // Extraer año-mes (YYYY-MM) de tourDate
                                        String yearMonth = extractYearMonth(tourDate);
                                        
                                        // Contar reservas por mes
                                        monthlyBookings.put(yearMonth, 
                                            monthlyBookings.getOrDefault(yearMonth, 0) + 1);
                                    }
                                }
                            }
                            
                            // Actualizar gráfico con datos reales
                            updateBarChartWithData(monthlyBookings);
                            
                            android.util.Log.d("SuperadminMain", "Reservas mensuales cargadas: " + monthlyBookings.size() + " meses");
                        } else {
                            android.util.Log.w("SuperadminMain", "QuerySnapshot de reservas mensuales es null");
                            updateBarChartWithEmptyData();
                        }
                    } else {
                        // Si falla con whereIn, intentar sin filtro
                        android.util.Log.w("SuperadminMain", "Error con whereIn, intentando sin filtro", task.getException());
                        loadMonthlyBookingsWithoutFilter();
                    }
                });
    }

    private void loadMonthlyBookingsWithoutFilter() {
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Map<String, Integer> monthlyBookings = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String status = doc.getString("status");
                                String paymentStatus = doc.getString("paymentStatus");
                                
                                if (status != null && 
                                    (status.equals("CONFIRMADA") || 
                                     status.equals("EN_CURSO") || 
                                     status.equals("COMPLETADA"))) {
                                    
                                    if (paymentStatus == null || 
                                        paymentStatus.equals("CONFIRMADO") || 
                                        paymentStatus.equals("COBRADO")) {
                                        
                                        String tourDate = doc.getString("tourDate");
                                        if (tourDate != null && !tourDate.isEmpty()) {
                                            String yearMonth = extractYearMonth(tourDate);
                                            monthlyBookings.put(yearMonth, 
                                                monthlyBookings.getOrDefault(yearMonth, 0) + 1);
                                        }
                                    }
                                }
                            }
                            
                            updateBarChartWithData(monthlyBookings);
                        } else {
                            updateBarChartWithEmptyData();
                        }
                    } else {
                        android.util.Log.e("SuperadminMain", "Error cargando reservas mensuales", task.getException());
                        updateBarChartWithEmptyData();
                    }
                });
    }

    private void updateBarChartWithData(Map<String, Integer> monthlyBookings) {
        if (barChartBookings == null) return;

        // Ordenar por año-mes (ascendente)
        TreeMap<String, Integer> sortedBookings = new TreeMap<>(monthlyBookings);
        
        // Obtener los últimos 7 meses (o todos si hay menos)
        List<String> monthKeys = new ArrayList<>(sortedBookings.keySet());
        int startIndex = Math.max(0, monthKeys.size() - 7);
        List<String> recentMonths = monthKeys.subList(startIndex, monthKeys.size());
        
        // Crear entradas para el gráfico
        List<BarEntry> entries = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        
        for (int i = 0; i < recentMonths.size(); i++) {
            String yearMonth = recentMonths.get(i);
            Integer bookings = sortedBookings.get(yearMonth);
            entries.add(new BarEntry(i, bookings != null ? bookings.floatValue() : 0f));
            
            // Formatear etiqueta del mes (ej: "2024-10" -> "Oct 2024")
            monthLabels.add(formatMonthLabel(yearMonth));
        }
        
        // Si no hay datos, mostrar mensaje vacío
        if (entries.isEmpty()) {
            updateBarChartWithEmptyData();
            return;
        }
        
        // Actualizar el gráfico
        BarDataSet dataSet = new BarDataSet(entries, "Reservas");
        dataSet.setColor(getResources().getColor(R.color.green));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black));
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        barChartBookings.setData(barData);
        
        // Actualizar etiquetas del eje X
        XAxis xAxis = barChartBookings.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        
        barChartBookings.invalidate();
        
        android.util.Log.d("SuperadminMain", "Gráfico de barras actualizado con " + entries.size() + " meses de datos");
    }

    private void updateBarChartWithEmptyData() {
        if (barChartBookings == null) return;
        
        // Mostrar gráfico vacío con mensaje
        List<BarEntry> entries = new ArrayList<>();
        BarDataSet dataSet = new BarDataSet(entries, "Reservas");
        dataSet.setColor(getResources().getColor(R.color.green));
        
        BarData barData = new BarData(dataSet);
        barChartBookings.setData(barData);
        
        XAxis xAxis = barChartBookings.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Sin datos")));
        
        barChartBookings.invalidate();
    }

    private void setupBarChartPeople() {
        // Configuración inicial del gráfico (se actualizará con datos reales)
        BarDataSet dataSet = new BarDataSet(new ArrayList<>(), "Personas");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        barChartPeople.setData(barData);
        barChartPeople.getDescription().setEnabled(false);
        barChartPeople.setFitBars(true);
        barChartPeople.getLegend().setEnabled(false);

        XAxis xAxis = barChartPeople.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.gray));

        barChartPeople.getAxisLeft().setTextColor(getResources().getColor(R.color.gray));
        barChartPeople.getAxisRight().setEnabled(false);
        barChartPeople.invalidate();
    }

    private void loadMonthlyPeople() {
        if (barChartPeople == null || db == null) {
            android.util.Log.w("SuperadminMain", "loadMonthlyPeople: barChartPeople o db es null");
            return;
        }

        android.util.Log.d("SuperadminMain", "Iniciando carga de personas mensuales desde Firestore");

        // Consultar todas las reservas con status válido
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            // Agrupar personas por mes usando tourDate
                            Map<String, Integer> monthlyPeople = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                // Verificar paymentStatus
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    // Obtener tourDate (formato: "YYYY-MM-DD")
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        // Extraer año-mes (YYYY-MM) de tourDate
                                        String yearMonth = extractYearMonth(tourDate);
                                        
                                        // Obtener numberOfPeople
                                        Object peopleObj = doc.get("numberOfPeople");
                                        if (peopleObj != null) {
                                            int people = 0;
                                            if (peopleObj instanceof Integer) {
                                                people = (Integer) peopleObj;
                                            } else if (peopleObj instanceof Long) {
                                                people = ((Long) peopleObj).intValue();
                                            } else if (peopleObj instanceof Number) {
                                                people = ((Number) peopleObj).intValue();
                                            }
                                            
                                            // Sumar personas por mes
                                            monthlyPeople.put(yearMonth, 
                                                monthlyPeople.getOrDefault(yearMonth, 0) + people);
                                        }
                                    }
                                }
                            }
                            
                            // Actualizar gráfico con datos reales
                            updateBarChartPeopleWithData(monthlyPeople);
                            
                            android.util.Log.d("SuperadminMain", "Personas mensuales cargadas: " + monthlyPeople.size() + " meses");
                        } else {
                            android.util.Log.w("SuperadminMain", "QuerySnapshot de personas mensuales es null");
                            updateBarChartPeopleWithEmptyData();
                        }
                    } else {
                        // Si falla con whereIn, intentar sin filtro
                        android.util.Log.w("SuperadminMain", "Error con whereIn, intentando sin filtro", task.getException());
                        loadMonthlyPeopleWithoutFilter();
                    }
                });
    }

    private void loadMonthlyPeopleWithoutFilter() {
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Map<String, Integer> monthlyPeople = new HashMap<>();
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String status = doc.getString("status");
                                String paymentStatus = doc.getString("paymentStatus");
                                
                                if (status != null && 
                                    (status.equals("CONFIRMADA") || 
                                     status.equals("EN_CURSO") || 
                                     status.equals("COMPLETADA"))) {
                                    
                                    if (paymentStatus == null || 
                                        paymentStatus.equals("CONFIRMADO") || 
                                        paymentStatus.equals("COBRADO")) {
                                        
                                        String tourDate = doc.getString("tourDate");
                                        if (tourDate != null && !tourDate.isEmpty()) {
                                            String yearMonth = extractYearMonth(tourDate);
                                            
                                            Object peopleObj = doc.get("numberOfPeople");
                                            if (peopleObj != null) {
                                                int people = 0;
                                                if (peopleObj instanceof Integer) {
                                                    people = (Integer) peopleObj;
                                                } else if (peopleObj instanceof Long) {
                                                    people = ((Long) peopleObj).intValue();
                                                } else if (peopleObj instanceof Number) {
                                                    people = ((Number) peopleObj).intValue();
                                                }
                                                
                                                monthlyPeople.put(yearMonth, 
                                                    monthlyPeople.getOrDefault(yearMonth, 0) + people);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            updateBarChartPeopleWithData(monthlyPeople);
                        } else {
                            updateBarChartPeopleWithEmptyData();
                        }
                    } else {
                        android.util.Log.e("SuperadminMain", "Error cargando personas mensuales", task.getException());
                        updateBarChartPeopleWithEmptyData();
                    }
                });
    }

    private void updateBarChartPeopleWithData(Map<String, Integer> monthlyPeople) {
        if (barChartPeople == null) return;

        // Ordenar por año-mes (ascendente)
        TreeMap<String, Integer> sortedPeople = new TreeMap<>(monthlyPeople);
        
        // Obtener los últimos 7 meses (o todos si hay menos)
        List<String> monthKeys = new ArrayList<>(sortedPeople.keySet());
        int startIndex = Math.max(0, monthKeys.size() - 7);
        List<String> recentMonths = monthKeys.subList(startIndex, monthKeys.size());
        
        // Crear entradas para el gráfico
        List<BarEntry> entries = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        
        for (int i = 0; i < recentMonths.size(); i++) {
            String yearMonth = recentMonths.get(i);
            Integer people = sortedPeople.get(yearMonth);
            entries.add(new BarEntry(i, people != null ? people.floatValue() : 0f));
            
            // Formatear etiqueta del mes (ej: "2024-10" -> "Oct 2024")
            monthLabels.add(formatMonthLabel(yearMonth));
        }
        
        // Si no hay datos, mostrar mensaje vacío
        if (entries.isEmpty()) {
            updateBarChartPeopleWithEmptyData();
            return;
        }
        
        // Actualizar el gráfico
        BarDataSet dataSet = new BarDataSet(entries, "Personas");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black));
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        
        barChartPeople.setData(barData);
        
        // Actualizar etiquetas del eje X
        XAxis xAxis = barChartPeople.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        
        barChartPeople.invalidate();
        
        android.util.Log.d("SuperadminMain", "Gráfico de personas actualizado con " + entries.size() + " meses de datos");
    }

    private void updateBarChartPeopleWithEmptyData() {
        if (barChartPeople == null) return;
        
        // Mostrar gráfico vacío con mensaje
        List<BarEntry> entries = new ArrayList<>();
        BarDataSet dataSet = new BarDataSet(entries, "Personas");
        dataSet.setColor(getResources().getColor(R.color.primary));
        
        BarData barData = new BarData(dataSet);
        barChartPeople.setData(barData);
        
        XAxis xAxis = barChartPeople.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Sin datos")));
        
        barChartPeople.invalidate();
    }

    private void loadMonthlyPeopleForPeriod(int periodPosition) {
        if (barChartPeople == null || db == null) {
            return;
        }

        // Calcular rango de fechas según el período
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();
        Date startDate;
        
        switch (periodPosition) {
            case 0: // Hoy
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startDate = cal.getTime();
                break;
            case 1: // Semana (últimos 7 días)
                cal.add(Calendar.DAY_OF_YEAR, -7);
                startDate = cal.getTime();
                break;
            case 2: // Mes (último mes)
                cal.add(Calendar.MONTH, -1);
                startDate = cal.getTime();
                break;
            case 3: // Año (último año)
                cal.add(Calendar.YEAR, -1);
                startDate = cal.getTime();
                break;
            default:
                startDate = null;
        }

        android.util.Log.d("SuperadminMain", "Cargando personas para período: " + periodPosition + 
                          " desde " + startDate + " hasta " + endDate);

        // Consultar reservas con filtro de fecha
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Map<String, Integer> monthlyPeople = new HashMap<>();
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        try {
                                            // Parsear tourDate y verificar si está en el rango
                                            Date tourDateObj = sdf.parse(tourDate);
                                            if (tourDateObj != null && startDate != null) {
                                                if (tourDateObj.compareTo(startDate) >= 0 && 
                                                    tourDateObj.compareTo(endDate) <= 0) {
                                                    
                                                    String yearMonth = extractYearMonth(tourDate);
                                                    
                                                    // Obtener numberOfPeople
                                                    Object peopleObj = doc.get("numberOfPeople");
                                                    if (peopleObj != null) {
                                                        int people = 0;
                                                        if (peopleObj instanceof Integer) {
                                                            people = (Integer) peopleObj;
                                                        } else if (peopleObj instanceof Long) {
                                                            people = ((Long) peopleObj).intValue();
                                                        } else if (peopleObj instanceof Number) {
                                                            people = ((Number) peopleObj).intValue();
                                                        }
                                                        
                                                        // Sumar personas por mes
                                                        monthlyPeople.put(yearMonth, 
                                                            monthlyPeople.getOrDefault(yearMonth, 0) + people);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            android.util.Log.w("SuperadminMain", "Error parseando fecha: " + tourDate, e);
                                        }
                                    }
                                }
                            }
                            
                            updateBarChartPeopleWithData(monthlyPeople);
                        } else {
                            updateBarChartPeopleWithEmptyData();
                        }
                    } else {
                        android.util.Log.e("SuperadminMain", "Error cargando personas por período", task.getException());
                        updateBarChartPeopleWithEmptyData();
                    }
                });
    }

    private void updateKPIs() {
        // Cargar total de usuarios desde Firestore
        loadTotalUsers();
        
        // Cargar tours activos desde Firestore
        loadActiveTours();
        
        // Cargar total de reservas desde Firestore
        loadTotalBookings();
        
        // Cargar ingresos totales desde Firestore
        loadTotalRevenue();
    }

    private void loadTotalUsers() {
        if (tvTotalUsers == null || db == null) {
            android.util.Log.w("SuperadminMain", "loadTotalUsers: tvTotalUsers o db es null");
            return;
        }

        // Mostrar indicador de carga
        tvTotalUsers.setText("Cargando...");
        
        android.util.Log.d("SuperadminMain", "Iniciando consulta a colección 'users'");

        // Consultar todos los usuarios en la colección
        db.collection(FirestoreManager.COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    android.util.Log.d("SuperadminMain", "Consulta completada. Éxito: " + task.isSuccessful());
                    
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        android.util.Log.d("SuperadminMain", "QuerySnapshot: " + (querySnapshot != null ? "no null" : "null"));
                        
                        if (querySnapshot != null) {
                            int totalUsers = querySnapshot.size();
                            android.util.Log.d("SuperadminMain", "Total de documentos obtenidos: " + totalUsers);
                            
                            // Log adicional: listar IDs de documentos obtenidos
                            if (!querySnapshot.isEmpty()) {
                                android.util.Log.d("SuperadminMain", "Documentos encontrados:");
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                    android.util.Log.d("SuperadminMain", "  - ID: " + doc.getId() + ", Email: " + doc.getString("email"));
                                }
                            } else {
                                android.util.Log.w("SuperadminMain", "QuerySnapshot está vacío");
                            }
                            
                            // Verificar si hay más documentos (paginación)
                            if (querySnapshot.getMetadata().hasPendingWrites()) {
                                android.util.Log.w("SuperadminMain", "Hay escrituras pendientes que podrían afectar el conteo");
                            }
                            
                            // Formatear número con separador de miles
                            String formattedCount = formatNumber(totalUsers);
                            tvTotalUsers.setText(formattedCount);
                            android.util.Log.d("SuperadminMain", "Total de usuarios mostrado: " + formattedCount);
                        } else {
                            android.util.Log.w("SuperadminMain", "QuerySnapshot es null");
                            tvTotalUsers.setText("0");
                        }
                    } else {
                        // Error al cargar, mostrar valor por defecto
                        Exception exception = task.getException();
                        android.util.Log.e("SuperadminMain", "Error cargando total de usuarios", exception);
                        if (exception != null) {
                            android.util.Log.e("SuperadminMain", "Mensaje de error: " + exception.getMessage());
                            android.util.Log.e("SuperadminMain", "Causa: " + (exception.getCause() != null ? exception.getCause().getMessage() : "null"));
                        }
                        tvTotalUsers.setText("--");
                    }
                });
    }

    private void loadActiveTours() {
        if (tvActiveTours == null || db == null) {
            android.util.Log.w("SuperadminMain", "loadActiveTours: tvActiveTours o db es null");
            return;
        }

        // Mostrar indicador de carga
        tvActiveTours.setText("Cargando...");
        
        android.util.Log.d("SuperadminMain", "Iniciando consulta a colección 'tours' con filtro isActive=true");

        // Consultar tours activos (isActive == true)
        db.collection(FirestoreManager.COLLECTION_TOURS)
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    android.util.Log.d("SuperadminMain", "Consulta de tours completada. Éxito: " + task.isSuccessful());
                    
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        android.util.Log.d("SuperadminMain", "QuerySnapshot tours: " + (querySnapshot != null ? "no null" : "null"));
                        
                        if (querySnapshot != null) {
                            int activeTours = querySnapshot.size();
                            android.util.Log.d("SuperadminMain", "Total de tours activos obtenidos: " + activeTours);
                            
                            // Log adicional: listar IDs de tours activos
                            if (!querySnapshot.isEmpty()) {
                                android.util.Log.d("SuperadminMain", "Tours activos encontrados:");
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                    String tourName = doc.getString("name");
                                    if (tourName == null || tourName.isEmpty()) {
                                        tourName = doc.getId();
                                    }
                                    android.util.Log.d("SuperadminMain", "  - ID: " + doc.getId() + ", Nombre: " + tourName);
                                }
                            } else {
                                android.util.Log.w("SuperadminMain", "No se encontraron tours activos");
                            }
                            
                            // Actualizar UI con el número de tours activos
                            tvActiveTours.setText(String.valueOf(activeTours));
                            android.util.Log.d("SuperadminMain", "Total de tours activos mostrado: " + activeTours);
                        } else {
                            android.util.Log.w("SuperadminMain", "QuerySnapshot de tours es null");
                            tvActiveTours.setText("0");
                        }
                    } else {
                        // Error al cargar, mostrar valor por defecto
                        Exception exception = task.getException();
                        android.util.Log.e("SuperadminMain", "Error cargando tours activos", exception);
                        if (exception != null) {
                            android.util.Log.e("SuperadminMain", "Mensaje de error: " + exception.getMessage());
                        }
                        tvActiveTours.setText("--");
                    }
                });
    }

    private void loadTotalBookings() {
        if (tvBookings == null || db == null) {
            android.util.Log.w("SuperadminMain", "loadTotalBookings: tvBookings o db es null");
            return;
        }

        // Mostrar indicador de carga
        tvBookings.setText("Cargando...");
        
        android.util.Log.d("SuperadminMain", "Iniciando consulta a colección 'reservations' para contar reservas");

        // Consultar todas las reservas (o solo las confirmadas/completadas)
        // Filtrar por status: CONFIRMADA, EN_CURSO, COMPLETADA (excluir PENDIENTE y CANCELADA)
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", java.util.Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    android.util.Log.d("SuperadminMain", "Consulta de reservas completada. Éxito: " + task.isSuccessful());
                    
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        android.util.Log.d("SuperadminMain", "QuerySnapshot reservas: " + (querySnapshot != null ? "no null" : "null"));
                        
                        if (querySnapshot != null) {
                            int totalBookings = querySnapshot.size();
                            android.util.Log.d("SuperadminMain", "Total de reservas obtenidas: " + totalBookings);
                            
                            // Actualizar UI con el número de reservas
                            tvBookings.setText(String.valueOf(totalBookings));
                            android.util.Log.d("SuperadminMain", "Total de reservas mostrado: " + totalBookings);
                        } else {
                            android.util.Log.w("SuperadminMain", "QuerySnapshot de reservas es null");
                            tvBookings.setText("0");
                        }
                    } else {
                        // Si falla con whereIn, intentar sin filtro y contar en el cliente
                        android.util.Log.w("SuperadminMain", "Error con whereIn, intentando sin filtro", task.getException());
                        loadTotalBookingsWithoutFilter();
                    }
                });
    }

    private void loadTotalBookingsWithoutFilter() {
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            int totalBookings = 0;
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String status = doc.getString("status");
                                if (status != null && 
                                    (status.equals("CONFIRMADA") || 
                                     status.equals("EN_CURSO") || 
                                     status.equals("COMPLETADA"))) {
                                    totalBookings++;
                                }
                            }
                            android.util.Log.d("SuperadminMain", "Total de reservas (filtrado en cliente): " + totalBookings);
                            tvBookings.setText(String.valueOf(totalBookings));
                        } else {
                            tvBookings.setText("0");
                        }
                    } else {
                        Exception exception = task.getException();
                        android.util.Log.e("SuperadminMain", "Error cargando reservas", exception);
                        tvBookings.setText("--");
                    }
                });
    }

    private void loadTotalRevenue() {
        if (tvRevenue == null || db == null) {
            android.util.Log.w("SuperadminMain", "loadTotalRevenue: tvRevenue o db es null");
            return;
        }

        // Mostrar indicador de carga
        tvRevenue.setText("Cargando...");
        
        android.util.Log.d("SuperadminMain", "Iniciando consulta a colección 'reservations' para calcular ingresos");

        // Consultar todas las reservas confirmadas/completadas para calcular ingresos
        // Filtrar por status: CONFIRMADA, EN_CURSO, COMPLETADA
        // Y por paymentStatus: CONFIRMADO, COBRADO
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", java.util.Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    android.util.Log.d("SuperadminMain", "Consulta de ingresos completada. Éxito: " + task.isSuccessful());
                    
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        android.util.Log.d("SuperadminMain", "QuerySnapshot ingresos: " + (querySnapshot != null ? "no null" : "null"));
                        
                        if (querySnapshot != null) {
                            double totalRevenue = 0.0;
                            int count = 0;
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                // Obtener totalPrice (puede ser Double, Long, o String)
                                Object priceObj = doc.get("totalPrice");
                                if (priceObj != null) {
                                    double price = 0.0;
                                    if (priceObj instanceof Double) {
                                        price = (Double) priceObj;
                                    } else if (priceObj instanceof Long) {
                                        price = ((Long) priceObj).doubleValue();
                                    } else if (priceObj instanceof Number) {
                                        price = ((Number) priceObj).doubleValue();
                                    } else if (priceObj instanceof String) {
                                        try {
                                            price = Double.parseDouble((String) priceObj);
                                        } catch (NumberFormatException e) {
                                            android.util.Log.w("SuperadminMain", "No se pudo parsear totalPrice: " + priceObj);
                                        }
                                    }
                                    
                                    // Verificar también paymentStatus si está disponible
                                    String paymentStatus = doc.getString("paymentStatus");
                                    if (paymentStatus == null || 
                                        paymentStatus.equals("CONFIRMADO") || 
                                        paymentStatus.equals("COBRADO")) {
                                        totalRevenue += price;
                                        count++;
                                    }
                                }
                            }
                            
                            android.util.Log.d("SuperadminMain", "Total de ingresos calculados: S/ " + totalRevenue + " (de " + count + " reservas)");
                            
                            // Formatear como moneda peruana
                            String formattedRevenue = formatCurrency(totalRevenue);
                            tvRevenue.setText(formattedRevenue);
                            android.util.Log.d("SuperadminMain", "Ingresos mostrados: " + formattedRevenue);
                        } else {
                            android.util.Log.w("SuperadminMain", "QuerySnapshot de ingresos es null");
                            tvRevenue.setText("S/ 0");
                        }
                    } else {
                        // Si falla con whereIn, intentar sin filtro y calcular en el cliente
                        android.util.Log.w("SuperadminMain", "Error con whereIn, intentando sin filtro", task.getException());
                        loadTotalRevenueWithoutFilter();
                    }
                });
    }

    private void loadTotalRevenueWithoutFilter() {
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            double totalRevenue = 0.0;
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String status = doc.getString("status");
                                String paymentStatus = doc.getString("paymentStatus");
                                
                                // Solo contar reservas confirmadas/completadas con pago confirmado
                                if (status != null && 
                                    (status.equals("CONFIRMADA") || 
                                     status.equals("EN_CURSO") || 
                                     status.equals("COMPLETADA"))) {
                                    
                                    if (paymentStatus == null || 
                                        paymentStatus.equals("CONFIRMADO") || 
                                        paymentStatus.equals("COBRADO")) {
                                        
                                        Object priceObj = doc.get("totalPrice");
                                        if (priceObj != null) {
                                            double price = 0.0;
                                            if (priceObj instanceof Double) {
                                                price = (Double) priceObj;
                                            } else if (priceObj instanceof Long) {
                                                price = ((Long) priceObj).doubleValue();
                                            } else if (priceObj instanceof Number) {
                                                price = ((Number) priceObj).doubleValue();
                                            } else if (priceObj instanceof String) {
                                                try {
                                                    price = Double.parseDouble((String) priceObj);
                                                } catch (NumberFormatException e) {
                                                    // Ignorar si no se puede parsear
                                                }
                                            }
                                            totalRevenue += price;
                                        }
                                    }
                                }
                            }
                            
                            android.util.Log.d("SuperadminMain", "Total de ingresos (filtrado en cliente): S/ " + totalRevenue);
                            tvRevenue.setText(formatCurrency(totalRevenue));
                        } else {
                            tvRevenue.setText("S/ 0");
                        }
                    } else {
                        Exception exception = task.getException();
                        android.util.Log.e("SuperadminMain", "Error cargando ingresos", exception);
                        tvRevenue.setText("--");
                    }
                });
    }

    private String formatNumber(int number) {
        // Formatear número con separador de miles (ej: 1247 -> "1,247")
        return String.format("%,d", number);
    }

    private String formatCurrency(double amount) {
        // Formatear como moneda peruana (ej: 45250.0 -> "S/ 45,250")
        return String.format("S/ %,.0f", amount);
    }

    private void setupFAB() {
        if (fabExport != null) {
            fabExport.setOnClickListener(v -> {
                Toast.makeText(this, "Iniciando exportación...", Toast.LENGTH_SHORT).show();
                exportAnalyticsReport();
                // ✨ Incrementar badge cuando se usa FAB
                notificationCount++;
                updateNotificationBadge();
            });
        }
    }

    private void exportAnalyticsReport() {
        // En Android 10+ (API 29+), no necesitamos WRITE_EXTERNAL_STORAGE para escribir en Downloads
        // Pero verificamos si estamos en una versión anterior
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return;
            }
        }

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            
            // Exportar archivos
            String pdfPath;
            List<String> imagePaths = new ArrayList<>();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ usar MediaStore para que aparezcan en Descargas del sistema
                pdfPath = exportToPDFWithMediaStore(timestamp);
                imagePaths = exportChartsToImagesWithMediaStore(timestamp);
            } else {
                // Android 9 y anteriores usar directorio tradicional
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File droidTourDir = new File(downloadsDir, "DroidTour");
            if (!droidTourDir.exists()) {
                    boolean created = droidTourDir.mkdirs();
                    if (!created) {
                        android.util.Log.e("SuperadminMain", "No se pudo crear el directorio: " + droidTourDir.getAbsolutePath());
                        Toast.makeText(this, "❌ Error: No se pudo crear el directorio de descarga", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                pdfPath = exportToPDF(droidTourDir, timestamp);
                imagePaths = exportChartsToImages(droidTourDir, timestamp);
            }

            showExportSuccessNotification(pdfPath, imagePaths);
            String message = "✅ Exportación completada\n" + 
                           "1 PDF y " + imagePaths.size() + " imágenes guardadas\n" +
                           "Archivos en Descargas/DroidTour";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            android.util.Log.e("SuperadminMain", "Error en exportación", e);
            showExportErrorNotification();
            Toast.makeText(this, "❌ Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, intentar exportar nuevamente
                exportAnalyticsReport();
            } else {
                Toast.makeText(this, "❌ Permiso denegado. No se puede exportar sin permisos de almacenamiento.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String exportToPDF(File directory, String timestamp) throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setTextSize(24);
        paint.setColor(getResources().getColor(R.color.primary));
        paint.setFakeBoldText(true);
        canvas.drawText("Reporte Analytics - DroidTour", 50, 80, paint);

        paint.setTextSize(14);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(false);
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generado: " + currentDate, 50, 110, paint);

        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("Indicadores Clave de Rendimiento", 50, 160, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        
        // Obtener valores reales de los KPIs
        String totalUsers = tvTotalUsers != null ? tvTotalUsers.getText().toString() : "N/A";
        String activeTours = tvActiveTours != null ? tvActiveTours.getText().toString() : "N/A";
        String revenue = tvRevenue != null ? tvRevenue.getText().toString() : "N/A";
        String bookings = tvBookings != null ? tvBookings.getText().toString() : "N/A";
        
        canvas.drawText("• Total Usuarios: " + totalUsers, 70, 190, paint);
        canvas.drawText("• Tours Activos: " + activeTours, 70, 210, paint);
        canvas.drawText("• Ingresos: " + revenue, 70, 230, paint);
        canvas.drawText("• Reservas: " + bookings, 70, 250, paint);

        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("Análisis de Tendencias", 50, 300, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        
        // Análisis basado en datos reales (si están disponibles)
        String trendText = "• Datos actualizados al " + currentDate;
        canvas.drawText(trendText, 70, 330, paint);
        
        // Información sobre categorías de tours (si el gráfico tiene datos)
        if (pieChartTours != null && pieChartTours.getData() != null && 
            pieChartTours.getData().getDataSet() != null) {
            PieDataSet dataSet = (PieDataSet) pieChartTours.getData().getDataSet();
            if (dataSet.getEntryCount() > 0) {
                PieEntry largestEntry = dataSet.getEntryForIndex(0);
                if (largestEntry != null) {
                    String categoryName = largestEntry.getLabel();
                    float percentage = largestEntry.getValue();
                    canvas.drawText("• Categoría más popular: " + categoryName + " (" + 
                                  String.format(Locale.getDefault(), "%.1f", percentage) + "%)", 70, 350, paint);
                }
            }
        } else {
            canvas.drawText("• Información de categorías disponible en gráficos", 70, 350, paint);
        }
        
        canvas.drawText("• Reporte generado desde el dashboard de SuperAdmin", 70, 370, paint);
        canvas.drawText("• Todos los datos provienen de Firebase Firestore", 70, 390, paint);

        paint.setTextSize(10);
        paint.setColor(Color.GRAY);
        canvas.drawText("DroidTour SuperAdmin Dashboard - Confidencial", 50, 800, paint);

        document.finishPage(page);

        String fileName = "Reporte_Analytics_" + timestamp + ".pdf";
        File file = new File(directory, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        document.close();
        fos.close();

        return file.getAbsolutePath();
    }

    /**
     * Exportar PDF usando MediaStore para Android 10+ (aparece en Descargas del sistema)
     */
    private String exportToPDFWithMediaStore(String timestamp) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new UnsupportedOperationException("Este método solo funciona en Android 10+");
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setTextSize(24);
        paint.setColor(getResources().getColor(R.color.primary));
        paint.setFakeBoldText(true);
        canvas.drawText("Reporte Analytics - DroidTour", 50, 80, paint);

        paint.setTextSize(14);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(false);
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generado: " + currentDate, 50, 110, paint);

        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("Indicadores Clave de Rendimiento", 50, 160, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        
        // Obtener valores reales de los KPIs
        String totalUsers = tvTotalUsers != null ? tvTotalUsers.getText().toString() : "N/A";
        String activeTours = tvActiveTours != null ? tvActiveTours.getText().toString() : "N/A";
        String revenue = tvRevenue != null ? tvRevenue.getText().toString() : "N/A";
        String bookings = tvBookings != null ? tvBookings.getText().toString() : "N/A";
        
        canvas.drawText("• Total Usuarios: " + totalUsers, 70, 190, paint);
        canvas.drawText("• Tours Activos: " + activeTours, 70, 210, paint);
        canvas.drawText("• Ingresos: " + revenue, 70, 230, paint);
        canvas.drawText("• Reservas: " + bookings, 70, 250, paint);

        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("Análisis de Tendencias", 50, 300, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        
        String trendText = "• Datos actualizados al " + currentDate;
        canvas.drawText(trendText, 70, 330, paint);
        
        if (pieChartTours != null && pieChartTours.getData() != null && 
            pieChartTours.getData().getDataSet() != null) {
            PieDataSet dataSet = (PieDataSet) pieChartTours.getData().getDataSet();
            if (dataSet.getEntryCount() > 0) {
                PieEntry largestEntry = dataSet.getEntryForIndex(0);
                if (largestEntry != null) {
                    String categoryName = largestEntry.getLabel();
                    float percentage = largestEntry.getValue();
                    canvas.drawText("• Categoría más popular: " + categoryName + " (" + 
                                  String.format(Locale.getDefault(), "%.1f", percentage) + "%)", 70, 350, paint);
                }
            }
        } else {
            canvas.drawText("• Información de categorías disponible en gráficos", 70, 350, paint);
        }
        
        canvas.drawText("• Reporte generado desde el dashboard de SuperAdmin", 70, 370, paint);
        canvas.drawText("• Todos los datos provienen de Firebase Firestore", 70, 390, paint);

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

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri);
            if (fos != null) {
                document.writeTo(fos);
                fos.close();
            }
        }
        document.close();

        return uri != null ? uri.toString() : fileName;
    }

    /**
     * Exportar cada gráfico como imagen separada usando MediaStore para Android 10+
     * (aparece en Descargas del sistema)
     */
    private List<String> exportChartsToImagesWithMediaStore(String timestamp) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new UnsupportedOperationException("Este método solo funciona en Android 10+");
        }

        List<String> imagePaths = new ArrayList<>();
        int chartWidth = 1200;  // Ancho mayor para mejor calidad
        int chartHeight = 800;  // Alto mayor para mejor calidad
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        // Exportar gráfico de Ingresos Mensuales
        if (lineChartRevenue != null && lineChartRevenue.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(lineChartRevenue, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Ingresos_Mensuales_" + timestamp + ".png";
                    String path = saveChartBitmapWithMediaStore(chartBitmap, fileName, "Ingresos Mensuales", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de ingresos", e);
            }
        }

        // Exportar gráfico de Precio Promedio por Persona
        if (lineChartAveragePrice != null && lineChartAveragePrice.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(lineChartAveragePrice, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Precio_Promedio_Persona_" + timestamp + ".png";
                    String path = saveChartBitmapWithMediaStore(chartBitmap, fileName, "Precio Promedio por Persona", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de precio promedio", e);
            }
        }

        // Exportar gráfico de Tours por Categoría
        if (pieChartTours != null && pieChartTours.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(pieChartTours, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Tours_por_Categoria_" + timestamp + ".png";
                    String path = saveChartBitmapWithMediaStore(chartBitmap, fileName, "Tours por Categoría", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de categorías", e);
            }
        }

        // Exportar gráfico de Reservas por Mes
        if (barChartBookings != null && barChartBookings.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(barChartBookings, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Reservas_por_Mes_" + timestamp + ".png";
                    String path = saveChartBitmapWithMediaStore(chartBitmap, fileName, "Reservas por Mes", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de reservas", e);
            }
        }

        // Exportar gráfico de Personas por Mes
        if (barChartPeople != null && barChartPeople.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(barChartPeople, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Personas_por_Mes_" + timestamp + ".png";
                    String path = saveChartBitmapWithMediaStore(chartBitmap, fileName, "Personas por Mes", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de personas", e);
            }
        }

        return imagePaths;
    }

    /**
     * Guardar un bitmap de gráfico como imagen usando MediaStore
     */
    private String saveChartBitmapWithMediaStore(Bitmap chartBitmap, String fileName, String chartTitle, String currentDate) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }

        try {
            // Crear bitmap con título y gráfico
            int padding = 40;
            int titleHeight = 100;
            int width = chartBitmap.getWidth() + (padding * 2);
            int height = chartBitmap.getHeight() + titleHeight + (padding * 2);

            Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalBitmap);
            canvas.drawColor(Color.WHITE);

            Paint paint = new Paint();
            paint.setAntiAlias(true);

            // Dibujar título
            paint.setTextSize(36);
        paint.setColor(getResources().getColor(R.color.primary));
            paint.setFakeBoldText(true);
            float titleX = padding;
            float titleY = padding + 50;
            canvas.drawText(chartTitle, titleX, titleY, paint);

            // Dibujar fecha
        paint.setTextSize(16);
            paint.setFakeBoldText(false);
            paint.setColor(Color.GRAY);
            canvas.drawText("Generado: " + currentDate, titleX, titleY + 30, paint);

            // Dibujar el gráfico
            canvas.drawBitmap(chartBitmap, padding, titleHeight + padding, null);

            // Guardar usando MediaStore
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DroidTour");

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri);
                if (fos != null) {
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    finalBitmap.recycle();
                    return uri.toString();
                }
            }
            finalBitmap.recycle();
        } catch (Exception e) {
            android.util.Log.e("SuperadminMain", "Error guardando gráfico con MediaStore: " + chartTitle, e);
        }
        return null;
    }

    /**
     * Exportar cada gráfico como imagen separada (Android 9 y anteriores)
     */
    private List<String> exportChartsToImages(File directory, String timestamp) throws IOException {
        List<String> imagePaths = new ArrayList<>();
        int chartWidth = 1200;  // Ancho mayor para mejor calidad
        int chartHeight = 800;  // Alto mayor para mejor calidad
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        // Exportar gráfico de Ingresos Mensuales
        if (lineChartRevenue != null && lineChartRevenue.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(lineChartRevenue, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Ingresos_Mensuales_" + timestamp + ".png";
                    String path = saveChartBitmap(chartBitmap, directory, fileName, "Ingresos Mensuales", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de ingresos", e);
            }
        }

        // Exportar gráfico de Precio Promedio por Persona
        if (lineChartAveragePrice != null && lineChartAveragePrice.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(lineChartAveragePrice, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Precio_Promedio_Persona_" + timestamp + ".png";
                    String path = saveChartBitmap(chartBitmap, directory, fileName, "Precio Promedio por Persona", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de precio promedio", e);
            }
        }

        // Exportar gráfico de Tours por Categoría
        if (pieChartTours != null && pieChartTours.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(pieChartTours, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Tours_por_Categoria_" + timestamp + ".png";
                    String path = saveChartBitmap(chartBitmap, directory, fileName, "Tours por Categoría", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de categorías", e);
            }
        }

        // Exportar gráfico de Reservas por Mes
        if (barChartBookings != null && barChartBookings.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(barChartBookings, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Reservas_por_Mes_" + timestamp + ".png";
                    String path = saveChartBitmap(chartBitmap, directory, fileName, "Reservas por Mes", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de reservas", e);
            }
        }

        // Exportar gráfico de Personas por Mes
        if (barChartPeople != null && barChartPeople.getData() != null) {
            try {
                Bitmap chartBitmap = captureChartAsBitmap(barChartPeople, chartWidth, chartHeight);
                if (chartBitmap != null) {
                    String fileName = "Personas_por_Mes_" + timestamp + ".png";
                    String path = saveChartBitmap(chartBitmap, directory, fileName, "Personas por Mes", currentDate);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                    chartBitmap.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error exportando gráfico de personas", e);
            }
        }

        return imagePaths;
    }

    /**
     * Guardar un bitmap de gráfico como imagen (Android 9 y anteriores)
     */
    private String saveChartBitmap(Bitmap chartBitmap, File directory, String fileName, String chartTitle, String currentDate) {
        try {
            // Crear bitmap con título y gráfico
            int padding = 40;
            int titleHeight = 100;
            int width = chartBitmap.getWidth() + (padding * 2);
            int height = chartBitmap.getHeight() + titleHeight + (padding * 2);

            Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalBitmap);
            canvas.drawColor(Color.WHITE);

            Paint paint = new Paint();
            paint.setAntiAlias(true);

            // Dibujar título
            paint.setTextSize(36);
            paint.setColor(getResources().getColor(R.color.primary));
            paint.setFakeBoldText(true);
            float titleX = padding;
            float titleY = padding + 50;
            canvas.drawText(chartTitle, titleX, titleY, paint);

            // Dibujar fecha
            paint.setTextSize(16);
            paint.setFakeBoldText(false);
            paint.setColor(Color.GRAY);
            canvas.drawText("Generado: " + currentDate, titleX, titleY + 30, paint);

            // Dibujar el gráfico
            canvas.drawBitmap(chartBitmap, padding, titleHeight + padding, null);

            // Guardar archivo
        File file = new File(directory, fileName);
        FileOutputStream fos = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
            finalBitmap.recycle();

        return file.getAbsolutePath();
        } catch (Exception e) {
            android.util.Log.e("SuperadminMain", "Error guardando gráfico: " + chartTitle, e);
            return null;
        }
    }

    /**
     * Captura un gráfico como Bitmap usando View.draw()
     * Compatible con todas las versiones de MPAndroidChart
     * Deshabilita gestos durante la captura para evitar que el gráfico cambie de tamaño
     */
    private Bitmap captureChartAsBitmap(View chartView, int width, int height) {
        if (chartView == null) {
            android.util.Log.w("SuperadminMain", "chartView es null");
            return null;
        }

        try {
            // Deshabilitar gestos temporalmente según el tipo de gráfico
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
            
            // Usar las dimensiones actuales del view en lugar de forzar nuevas
            int viewWidth = chartView.getWidth() > 0 ? chartView.getWidth() : width;
            int viewHeight = chartView.getHeight() > 0 ? chartView.getHeight() : height;
            
            // Si el view no tiene dimensiones, medirlo
            if (viewWidth == 0 || viewHeight == 0) {
                chartView.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                );
                viewWidth = chartView.getMeasuredWidth();
                viewHeight = chartView.getMeasuredHeight();
                chartView.layout(0, 0, viewWidth, viewHeight);
            }

            // Crear bitmap y canvas
            Bitmap bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            
            // Dibujar el fondo blanco
            canvas.drawColor(Color.WHITE);
            
            // Dibujar el gráfico
            chartView.draw(canvas);

            return bitmap;
        } catch (Exception e) {
            android.util.Log.e("SuperadminMain", "Error capturando gráfico como bitmap", e);
            return null;
        } finally {
            // Restaurar estado original del gráfico (valores por defecto)
            try {
                if (chartView instanceof LineChart) {
                    LineChart chart = (LineChart) chartView;
                    chart.setTouchEnabled(true);
                    chart.setDragEnabled(true);
                    chart.setScaleEnabled(true);
                    try {
                        chart.fitScreen();
                    } catch (Exception e) {
                        // Ignorar si el método no existe
                    }
                    chart.invalidate();
                } else if (chartView instanceof BarChart) {
                    BarChart chart = (BarChart) chartView;
                    chart.setTouchEnabled(true);
                    chart.setDragEnabled(true);
                    chart.setScaleEnabled(true);
                    try {
                        chart.fitScreen();
                    } catch (Exception e) {
                        // Ignorar si el método no existe
                    }
                    chart.invalidate();
                } else if (chartView instanceof PieChart) {
                    PieChart chart = (PieChart) chartView;
                    chart.setTouchEnabled(true);
                    chart.setRotationEnabled(true);
                    chart.invalidate();
                }
            } catch (Exception e) {
                android.util.Log.e("SuperadminMain", "Error restaurando estado del gráfico", e);
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
                // Para Android 10+, pdfPath es un URI string
                pdfFileName = Uri.parse(pdfPath).getLastPathSegment();
            } else {
                pdfFileName = new File(pdfPath).getName();
            }
        } catch (Exception e) {
            // Si falla, usar el path completo
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
                // Si falla, usar el path completo
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
                .setColor(Color.RED);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
        }
    }

    private void updateDataForPeriod(int position) {
        String period = "";
        switch (position) {
            case 0: period = "Hoy"; break;
            case 1: period = "Semana"; break;
            case 2: period = "Mes"; break;
            case 3: period = "Año"; break;
        }
        
        // Recargar ingresos mensuales con el filtro de período seleccionado
        loadMonthlyRevenueForPeriod(position);
        
        // Recargar reservas mensuales con el filtro de período seleccionado
        loadMonthlyBookingsForPeriod(position);
        
        // Recargar personas mensuales con el filtro de período seleccionado
        loadMonthlyPeopleForPeriod(position);
        
        // Recargar precio promedio mensual con el filtro de período seleccionado
        loadMonthlyAveragePriceForPeriod(position);
        
        Toast.makeText(this, "Mostrando datos de: " + period, Toast.LENGTH_SHORT).show();
    }

    private void loadMonthlyRevenueForPeriod(int periodPosition) {
        if (lineChartRevenue == null || db == null) {
            return;
        }

        // Calcular rango de fechas según el período
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();
        Date startDate;
        
        switch (periodPosition) {
            case 0: // Hoy
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startDate = cal.getTime();
                break;
            case 1: // Semana (últimos 7 días)
                cal.add(Calendar.DAY_OF_YEAR, -7);
                startDate = cal.getTime();
                break;
            case 2: // Mes (último mes)
                cal.add(Calendar.MONTH, -1);
                startDate = cal.getTime();
                break;
            case 3: // Año (último año)
                cal.add(Calendar.YEAR, -1);
                startDate = cal.getTime();
                break;
            default:
                startDate = null;
        }

        android.util.Log.d("SuperadminMain", "Cargando ingresos para período: " + periodPosition + 
                          " desde " + startDate + " hasta " + endDate);

        // Consultar reservas con filtro de fecha
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Map<String, Double> monthlyRevenue = new HashMap<>();
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        try {
                                            // Parsear tourDate y verificar si está en el rango
                                            Date tourDateObj = sdf.parse(tourDate);
                                            if (tourDateObj != null && startDate != null) {
                                                if (tourDateObj.compareTo(startDate) >= 0 && 
                                                    tourDateObj.compareTo(endDate) <= 0) {
                                                    
                                                    String yearMonth = extractYearMonth(tourDate);
                                                    
                                                    Object priceObj = doc.get("totalPrice");
                                                    if (priceObj != null) {
                                                        double price = 0.0;
                                                        if (priceObj instanceof Double) {
                                                            price = (Double) priceObj;
                                                        } else if (priceObj instanceof Long) {
                                                            price = ((Long) priceObj).doubleValue();
                                                        } else if (priceObj instanceof Number) {
                                                            price = ((Number) priceObj).doubleValue();
                                                        } else if (priceObj instanceof String) {
                                                            try {
                                                                price = Double.parseDouble((String) priceObj);
                                                            } catch (NumberFormatException e) {
                                                                // Ignorar
                                                            }
                                                        }
                                                        
                                                        monthlyRevenue.put(yearMonth, 
                                                            monthlyRevenue.getOrDefault(yearMonth, 0.0) + price);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            android.util.Log.w("SuperadminMain", "Error parseando fecha: " + tourDate, e);
                                        }
                                    }
                                }
                            }
                            
                            updateLineChartWithData(monthlyRevenue);
                        } else {
                            updateLineChartWithEmptyData();
                        }
                    } else {
                        android.util.Log.e("SuperadminMain", "Error cargando ingresos por período", task.getException());
                        updateLineChartWithEmptyData();
                    }
                });
    }

    private void loadMonthlyBookingsForPeriod(int periodPosition) {
        if (barChartBookings == null || db == null) {
            return;
        }

        // Calcular rango de fechas según el período
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();
        Date startDate;
        
        switch (periodPosition) {
            case 0: // Hoy
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startDate = cal.getTime();
                break;
            case 1: // Semana (últimos 7 días)
                cal.add(Calendar.DAY_OF_YEAR, -7);
                startDate = cal.getTime();
                break;
            case 2: // Mes (último mes)
                cal.add(Calendar.MONTH, -1);
                startDate = cal.getTime();
                break;
            case 3: // Año (último año)
                cal.add(Calendar.YEAR, -1);
                startDate = cal.getTime();
                break;
            default:
                startDate = null;
        }

        android.util.Log.d("SuperadminMain", "Cargando reservas para período: " + periodPosition + 
                          " desde " + startDate + " hasta " + endDate);

        // Consultar reservas con filtro de fecha
        db.collection(FirestoreManager.COLLECTION_RESERVATIONS)
                .whereIn("status", Arrays.asList("CONFIRMADA", "EN_CURSO", "COMPLETADA"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Map<String, Integer> monthlyBookings = new HashMap<>();
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                String paymentStatus = doc.getString("paymentStatus");
                                if (paymentStatus == null || 
                                    paymentStatus.equals("CONFIRMADO") || 
                                    paymentStatus.equals("COBRADO")) {
                                    
                                    String tourDate = doc.getString("tourDate");
                                    if (tourDate != null && !tourDate.isEmpty()) {
                                        try {
                                            // Parsear tourDate y verificar si está en el rango
                                            Date tourDateObj = sdf.parse(tourDate);
                                            if (tourDateObj != null && startDate != null) {
                                                if (tourDateObj.compareTo(startDate) >= 0 && 
                                                    tourDateObj.compareTo(endDate) <= 0) {
                                                    
                                                    String yearMonth = extractYearMonth(tourDate);
                                                    
                                                    // Contar reservas por mes
                                                    monthlyBookings.put(yearMonth, 
                                                        monthlyBookings.getOrDefault(yearMonth, 0) + 1);
                                                }
                                            }
                                        } catch (Exception e) {
                                            android.util.Log.w("SuperadminMain", "Error parseando fecha: " + tourDate, e);
                                        }
                                    }
                                }
                            }
                            
                            updateBarChartWithData(monthlyBookings);
                        } else {
                            updateBarChartWithEmptyData();
                        }
                    } else {
                        android.util.Log.e("SuperadminMain", "Error cargando reservas por período", task.getException());
                        updateBarChartWithEmptyData();
                    }
                });
    }

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
            //Se limpian los datos de seión
            prefsManager.cerrarSesion();

            //Limpiar el stack de activities de Login
            Intent intent= new Intent(this,LoginActivity.class);
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
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}