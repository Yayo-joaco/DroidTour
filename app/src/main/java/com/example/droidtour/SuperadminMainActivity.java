package com.example.droidtour;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private PieChart pieChartTours;
    private BarChart barChartBookings;
    private ExtendedFloatingActionButton fabExport;
    private TextView tvTotalUsers, tvActiveTours, tvRevenue, tvBookings;

    private TextView tvNotificationBadge;
    private ImageView ivAvatarAction;
    private FrameLayout notificationActionLayout, avatarActionLayout;
    private int notificationCount = 3;
    private PreferencesManager prefsManager;

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
        pieChartTours = findViewById(R.id.pie_chart_tours);
        barChartBookings = findViewById(R.id.bar_chart_bookings);
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
        if (lineChartRevenue != null) setupLineChart();
        if (pieChartTours != null) setupPieChart();
        if (barChartBookings != null) setupBarChart();
    }

    private void setupLineChart() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0f, 25000f));
        entries.add(new Entry(1f, 30000f));
        entries.add(new Entry(2f, 28000f));
        entries.add(new Entry(3f, 35000f));
        entries.add(new Entry(4f, 32000f));
        entries.add(new Entry(5f, 45000f));
        entries.add(new Entry(6f, 42000f));

        LineDataSet dataSet = new LineDataSet(entries, "Ingresos (S/)");
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
        lineChartRevenue.setData(lineData);
        lineChartRevenue.getDescription().setEnabled(false);
        lineChartRevenue.setTouchEnabled(true);
        lineChartRevenue.setDragEnabled(true);
        lineChartRevenue.setScaleEnabled(true);
        lineChartRevenue.getLegend().setEnabled(false);

        XAxis xAxis = lineChartRevenue.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul")));
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.gray));

        lineChartRevenue.getAxisLeft().setTextColor(getResources().getColor(R.color.gray));
        lineChartRevenue.getAxisRight().setEnabled(false);
        lineChartRevenue.invalidate();
    }

    private void setupPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(35f, "Cuzco"));
        entries.add(new PieEntry(25f, "Arequipa"));
        entries.add(new PieEntry(20f, "Puno"));
        entries.add(new PieEntry(12f, "Piura"));
        entries.add(new PieEntry(8f, "Iquitos"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                getResources().getColor(R.color.primary),
                getResources().getColor(R.color.green),
                getResources().getColor(R.color.orange),
                getResources().getColor(R.color.gray),
                Color.parseColor("#9C27B0")
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

    private void setupBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, 45f));
        entries.add(new BarEntry(1f, 52f));
        entries.add(new BarEntry(2f, 48f));
        entries.add(new BarEntry(3f, 65f));
        entries.add(new BarEntry(4f, 58f));
        entries.add(new BarEntry(5f, 72f));
        entries.add(new BarEntry(6f, 68f));

        BarDataSet dataSet = new BarDataSet(entries, "Reservas");
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
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul")));
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.gray));

        barChartBookings.getAxisLeft().setTextColor(getResources().getColor(R.color.gray));
        barChartBookings.getAxisRight().setEnabled(false);
        barChartBookings.invalidate();
    }

    private void updateKPIs() {
        if (tvTotalUsers != null) tvTotalUsers.setText("1,247");
        if (tvActiveTours != null) tvActiveTours.setText("23");
        if (tvRevenue != null) tvRevenue.setText("S/ 45,250");
        if (tvBookings != null) tvBookings.setText("156");
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File droidTourDir = new File(downloadsDir, "DroidTour");
            if (!droidTourDir.exists()) {
                droidTourDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String pdfPath = exportToPDF(droidTourDir, timestamp);
            String imagePath = exportChartsToImage(droidTourDir, timestamp);

            showExportSuccessNotification(pdfPath, imagePath);
            Toast.makeText(this, "✅ Exportación completada\nArchivos guardados en: Descargas/DroidTour", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            showExportErrorNotification();
            Toast.makeText(this, "❌ Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        canvas.drawText("• Total Usuarios: 1,247", 70, 190, paint);
        canvas.drawText("• Tours Activos: 23", 70, 210, paint);
        canvas.drawText("• Ingresos: S/ 45,250", 70, 230, paint);
        canvas.drawText("• Reservas: 156", 70, 250, paint);

        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("Análisis de Tendencias", 50, 300, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        canvas.drawText("• Los ingresos han mostrado un crecimiento del 15% este mes", 70, 330, paint);
        canvas.drawText("• Las reservas de tours de aventura lideran con 35%", 70, 350, paint);
        canvas.drawText("• Se observa un incremento en tours gastronómicos", 70, 370, paint);
        canvas.drawText("• La satisfacción del cliente se mantiene en 4.8/5", 70, 390, paint);

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

    private String exportChartsToImage(File directory, String timestamp) throws IOException {
        int width = 800;
        int height = 1200;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setTextSize(32);
        paint.setColor(getResources().getColor(R.color.primary));
        paint.setFakeBoldText(true);
        paint.setAntiAlias(true);

        canvas.drawText("Gráficos Analytics", 50, 60, paint);

        paint.setColor(getResources().getColor(R.color.primary));
        canvas.drawRect(50, 100, 750, 300, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(16);
        canvas.drawText("Gráfico de Ingresos Mensuales", 60, 130, paint);
        canvas.drawText("Datos exportados correctamente", 60, 160, paint);

        paint.setColor(getResources().getColor(R.color.green));
        canvas.drawRect(50, 350, 750, 550, paint);

        paint.setColor(Color.WHITE);
        canvas.drawText("Distribución de Tours por Categoría", 60, 380, paint);
        canvas.drawText("Aventura: 35% | Cultural: 25% | Gastronómico: 20%", 60, 410, paint);

        paint.setColor(getResources().getColor(R.color.orange));
        canvas.drawRect(50, 600, 750, 800, paint);

        paint.setColor(Color.WHITE);
        canvas.drawText("Reservas por Mes", 60, 630, paint);
        canvas.drawText("Tendencia al alza en los últimos 6 meses", 60, 660, paint);

        String fileName = "Charts_Analytics_" + timestamp + ".png";
        File file = new File(directory, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
        bitmap.recycle();

        return file.getAbsolutePath();
    }

    private void showExportSuccessNotification(String pdfPath, String imagePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download_24)
                .setContentTitle("✅ Exportación Completada")
                .setContentText("Reporte guardado en Descargas/DroidTour")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("✅ PDF: " + new File(pdfPath).getName() +
                                "\n📊 Gráficos: " + new File(imagePath).getName() +
                                "\n📁 Ubicación: Descargas/DroidTour"))
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
        Toast.makeText(this, "Mostrando datos de: " + period, Toast.LENGTH_SHORT).show();
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