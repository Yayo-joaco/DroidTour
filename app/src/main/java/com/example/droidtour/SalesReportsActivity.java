package com.example.droidtour;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesReportsActivity extends AppCompatActivity {
    
    private static final String TAG = "SalesReportsActivity";
    
    // Views
    private ChipGroup chipGroupPeriod;
    private Chip chipDaily, chipMonthly, chipAnnual, chipGeneral;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private SalesReportsPagerAdapter pagerAdapter;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;
    
    // KPI Views
    private TextView tvTotalRevenue, tvTotalReservations, tvAvgTicket;
    
    // Data
    private String currentCompanyId;
    
    // Date selection views
    private LinearLayout dateSelectionContainer;
    private MaterialButton btnSelectDate;
    private LinearLayout monthSelectionLayout;
    private LinearLayout yearSelectionLayout;
    private AppCompatAutoCompleteTextView spinnerMonth;
    private AppCompatAutoCompleteTextView spinnerYear;
    private AppCompatAutoCompleteTextView spinnerYearOnly;
    private TextInputLayout tilMonth, tilYear, tilYearOnly;
    
    // State
    private int currentPeriodType = 0; // 0=Diario, 1=Mensual, 2=Anual, 3=General
    private Date selectedDate = null;
    private int selectedMonth = -1;
    private int selectedYear = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("COMPANY_ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_sales_reports);
        
        setupToolbar();
        initializeViews();
        setupDateSelectors();
        setupPeriodFilters();
        setupViewPager();
        
        // Inicializar FirestoreManager
        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        
        // Cargar companyId del usuario
        loadCompanyId();
        
        // Inicializar con período por defecto (General)
        // El chip ya está marcado en el layout, solo configuramos el estado
        currentPeriodType = 3;
        updateSelectionUI();
        loadReportsForPeriod();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void initializeViews() {
        chipGroupPeriod = findViewById(R.id.chip_group_period);
        chipDaily = findViewById(R.id.chip_daily);
        chipMonthly = findViewById(R.id.chip_monthly);
        chipAnnual = findViewById(R.id.chip_annual);
        chipGeneral = findViewById(R.id.chip_general);
        
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        
        // KPI Views
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        tvTotalReservations = findViewById(R.id.tv_total_reservations);
        tvAvgTicket = findViewById(R.id.tv_avg_ticket);
        
        // Date selection views
        dateSelectionContainer = findViewById(R.id.date_selection_container);
        btnSelectDate = findViewById(R.id.btn_select_date);
        monthSelectionLayout = findViewById(R.id.month_selection_layout);
        yearSelectionLayout = findViewById(R.id.year_selection_layout);
        spinnerMonth = findViewById(R.id.spinner_month);
        spinnerYear = findViewById(R.id.spinner_year);
        spinnerYearOnly = findViewById(R.id.spinner_year_only);
        tilMonth = findViewById(R.id.til_month);
        tilYear = findViewById(R.id.til_year);
        tilYearOnly = findViewById(R.id.til_year_only);
    }
    
    private void setupPeriodFilters() {
        chipGroupPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_daily) {
                currentPeriodType = 0;
                // Si no hay fecha seleccionada, usar fecha actual por defecto (normalizada a medianoche)
                if (selectedDate == null) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    selectedDate = cal.getTime();
                }
            } else if (checkedId == R.id.chip_monthly) {
                currentPeriodType = 1;
                initializeSpinnerDefaults();
            } else if (checkedId == R.id.chip_annual) {
                currentPeriodType = 2;
                initializeSpinnerDefaults();
            } else if (checkedId == R.id.chip_general) {
                currentPeriodType = 3;
            }
            
            updateSelectionUI();
            loadReportsForPeriod();
        });
    }
    
    private void setupDateSelectors() {
        setupMonthSpinner();
        setupYearSpinners();
        
        if (btnSelectDate != null) {
            btnSelectDate.setOnClickListener(v -> showDatePickerDialog());
        }
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
            if (currentPeriodType == 1) {
                loadReportsForPeriod();
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
    
    private void setupYearSpinner(AppCompatAutoCompleteTextView spinner, boolean isForMonth) {
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
                if (currentPeriodType == 1 || currentPeriodType == 2) {
                    loadReportsForPeriod();
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
        
        // Ocultar todos los selectores primero
        if (btnSelectDate != null) btnSelectDate.setVisibility(View.GONE);
        if (monthSelectionLayout != null) monthSelectionLayout.setVisibility(View.GONE);
        if (yearSelectionLayout != null) yearSelectionLayout.setVisibility(View.GONE);
        
        switch (currentPeriodType) {
            case 0: // Diario
                dateSelectionContainer.setVisibility(View.VISIBLE);
                if (btnSelectDate != null) {
                    btnSelectDate.setVisibility(View.VISIBLE);
                    updateButtonText();
                }
                break;
            case 1: // Mensual
                dateSelectionContainer.setVisibility(View.VISIBLE);
                if (monthSelectionLayout != null) {
                    monthSelectionLayout.setVisibility(View.VISIBLE);
                }
                break;
            case 2: // Anual
                dateSelectionContainer.setVisibility(View.VISIBLE);
                if (yearSelectionLayout != null) {
                    yearSelectionLayout.setVisibility(View.VISIBLE);
                }
                break;
            case 3: // General
                dateSelectionContainer.setVisibility(View.GONE);
                break;
        }
    }
    
    private Date getSelectedDateForPeriod() {
        Calendar cal = Calendar.getInstance();
        
        switch (currentPeriodType) {
            case 0: // Diario
                // Si no hay fecha seleccionada, usar fecha actual por defecto (normalizada a medianoche)
                if (selectedDate == null) {
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal.getTime();
                }
                // Normalizar la fecha seleccionada a medianoche para comparación precisa
                cal.setTime(selectedDate);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTime();
            case 1: // Mensual
                if (selectedMonth >= 0 && selectedYear > 0) {
                    cal.set(Calendar.YEAR, selectedYear);
                    cal.set(Calendar.MONTH, selectedMonth);
                    cal.set(Calendar.DAY_OF_MONTH, 15);
                    return cal.getTime();
                }
                cal.set(Calendar.DAY_OF_MONTH, 15);
                return cal.getTime();
            case 2: // Anual
                if (selectedYear > 0) {
                    cal.set(Calendar.YEAR, selectedYear);
                    cal.set(Calendar.MONTH, 0);
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    return cal.getTime();
                }
                cal.set(Calendar.MONTH, 0);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                return cal.getTime();
            case 3: // General
                return null; // Sin filtro de fecha
        }
        
        return null;
    }
    
    private void updateButtonText() {
        if (btnSelectDate == null) return;
        
        if (selectedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            btnSelectDate.setText("Día: " + sdf.format(selectedDate));
        } else {
            btnSelectDate.setText("Seleccionar día");
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
                // Normalizar a medianoche para comparación precisa
                selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                selectedCalendar.set(Calendar.MINUTE, 0);
                selectedCalendar.set(Calendar.SECOND, 0);
                selectedCalendar.set(Calendar.MILLISECOND, 0);
                selectedDate = selectedCalendar.getTime();
                updateButtonText();
                loadReportsForPeriod();
            },
            year, month, day
        );
        
        // No permitir fechas futuras
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        
        datePickerDialog.show();
    }
    
    private void setupViewPager() {
        pagerAdapter = new SalesReportsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Por Servicio");
                    break;
                case 1:
                    tab.setText("Por Tour");
                    break;
                case 2:
                    tab.setText("Resumen");
                    break;
            }
        }).attach();
    }
    
    private void loadReportsForPeriod() {
        Date periodDate = getSelectedDateForPeriod();
        int periodType = currentPeriodType;
        
        // Notificar a los fragmentos sobre el cambio de período
        notifyFragmentsPeriodChanged(periodType, periodDate);
        
        // Actualizar KPIs en el header
        updateKPIHeader(periodType, periodDate);
    }
    
    private void notifyFragmentsPeriodChanged(int periodType, Date periodDate) {
        // Actualizar el estado del adapter para que los nuevos fragmentos reciban el período correcto
        pagerAdapter.notifyPeriodChanged(periodType, periodDate);
        
        // Notificar a los fragmentos existentes si están creados
        // Buscar fragmentos por tag del ViewPager2
        try {
            Fragment fragment0 = getSupportFragmentManager().findFragmentByTag("f" + pagerAdapter.getItemId(0));
            Fragment fragment1 = getSupportFragmentManager().findFragmentByTag("f" + pagerAdapter.getItemId(1));
            Fragment fragment2 = getSupportFragmentManager().findFragmentByTag("f" + pagerAdapter.getItemId(2));
            
            // Notificar a cada fragmento si existe
            if (fragment0 instanceof SalesByServiceFragment) {
                ((SalesByServiceFragment) fragment0).updatePeriod(periodType, periodDate);
            }
            if (fragment1 instanceof SalesByTourFragment) {
                ((SalesByTourFragment) fragment1).updatePeriod(periodType, periodDate);
            }
            if (fragment2 instanceof SalesSummaryFragment) {
                ((SalesSummaryFragment) fragment2).updatePeriod(periodType, periodDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error notificando fragmentos", e);
        }
    }
    
    private void loadCompanyId() {
        String userId = prefsManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "No se encontró userId, no se pueden cargar KPIs");
            showEmptyKPIs();
            return;
        }
        
        firestoreManager.getUserById(userId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null && !user.getCompanyId().isEmpty()) {
                    currentCompanyId = user.getCompanyId();
                    // Cargar KPIs después de obtener el companyId
                    updateKPIHeader(currentPeriodType, getSelectedDateForPeriod());
                } else {
                    Log.w(TAG, "Usuario no tiene companyId asignado");
                    showEmptyKPIs();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario para obtener companyId", e);
                showEmptyKPIs();
            }
        });
    }
    
    private void updateKPIHeader(int periodType, Date periodDate) {
        if (currentCompanyId == null || currentCompanyId.isEmpty()) {
            Log.w(TAG, "No hay companyId, no se pueden cargar KPIs");
            showEmptyKPIs();
            return;
        }
        
        // Cargar reservaciones de la empresa desde Firebase
        firestoreManager.getReservationsByCompany(currentCompanyId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                @SuppressWarnings("unchecked")
                List<com.example.droidtour.models.Reservation> reservations = (List<com.example.droidtour.models.Reservation>) result;
                
                if (reservations == null) {
                    reservations = new ArrayList<>();
                }
                
                // Filtrar reservaciones según el período seleccionado
                List<com.example.droidtour.models.Reservation> filteredReservations = filterReservationsByPeriod(reservations, periodType, periodDate);
                
                // Calcular KPIs
                double totalRevenue = 0;
                int validReservations = 0;
                
                for (com.example.droidtour.models.Reservation r : filteredReservations) {
                    if (isValidReservationForReports(r)) {
                        totalRevenue += r.getTotalPrice() != null ? r.getTotalPrice() : 0;
                        validReservations++;
                    }
                }
                
                double avgTicket = validReservations > 0 ? totalRevenue / validReservations : 0;
                
                // Actualizar UI
                if (tvTotalRevenue != null) {
                    tvTotalRevenue.setText(String.format(Locale.getDefault(), "S/. %.0f", totalRevenue));
                }
                if (tvTotalReservations != null) {
                    tvTotalReservations.setText(String.valueOf(validReservations));
                }
                if (tvAvgTicket != null) {
                    tvAvgTicket.setText(String.format(Locale.getDefault(), "S/. %.0f", avgTicket));
                }
                
                Log.d(TAG, "KPIs actualizados - Ingresos: S/. " + totalRevenue + 
                      ", Reservas: " + validReservations + 
                      ", Ticket Prom: S/. " + avgTicket);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando reservaciones para KPIs", e);
                showEmptyKPIs();
            }
        });
    }
    
    private void showEmptyKPIs() {
        if (tvTotalRevenue != null) tvTotalRevenue.setText("S/. 0");
        if (tvTotalReservations != null) tvTotalReservations.setText("0");
        if (tvAvgTicket != null) tvAvgTicket.setText("S/. 0");
    }
    
    /**
     * Verifica si la reserva es válida para reportes de ventas
     * Requiere: hasCheckedOut = true (ya se cobró) y status válido (CONFIRMADA, EN_CURSO, COMPLETADA)
     */
    private boolean isValidReservationForReports(com.example.droidtour.models.Reservation reservation) {
        if (reservation == null) return false;
        
        // Verificar que hasCheckedOut sea true (ya se realizó el pago)
        Boolean hasCheckedOut = reservation.getHasCheckedOut();
        if (hasCheckedOut == null || !hasCheckedOut) {
            return false;
        }
        
        // Verificar que el status sea válido
        String status = reservation.getStatus();
        if (status == null) return false;
        return "CONFIRMADA".equals(status) || 
               "EN_CURSO".equals(status) || 
               "COMPLETADA".equals(status);
    }
    
    /**
     * Obtiene la fecha relevante de la reserva para filtrado por período
     * Prioriza tourDate (fecha del servicio) sobre createdAt (fecha de reserva)
     */
    private Date getReservationDateForFiltering(com.example.droidtour.models.Reservation reservation) {
        // Priorizar tourDate (fecha del servicio) sobre createdAt
        if (reservation.getTourDate() != null) {
            try {
                // Formato DD/MM/YYYY
                String[] parts = reservation.getTourDate().split("/");
                if (parts.length == 3) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[0])
                    );
                    return cal.getTime();
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parseando tourDate: " + reservation.getTourDate(), e);
            }
        }
        
        // Fallback a createdAt si no hay tourDate
        return reservation.getCreatedAt();
    }
    
    /**
     * Filtra reservaciones según el período seleccionado
     */
    private List<com.example.droidtour.models.Reservation> filterReservationsByPeriod(
            List<com.example.droidtour.models.Reservation> reservations, 
            int periodType, 
            Date periodDate) {
        
        if (periodType == 3) {
            // General: sin filtro
            return reservations;
        }
        
        if (periodDate == null) {
            Log.w(TAG, "periodDate es null, retornando todas las reservaciones");
            return reservations;
        }
        
        Calendar periodCal = Calendar.getInstance();
        periodCal.setTime(periodDate);
        // Normalizar hora a medianoche para comparación precisa
        periodCal.set(Calendar.HOUR_OF_DAY, 0);
        periodCal.set(Calendar.MINUTE, 0);
        periodCal.set(Calendar.SECOND, 0);
        periodCal.set(Calendar.MILLISECOND, 0);
        
        Calendar reservationCal = Calendar.getInstance();
        
        List<com.example.droidtour.models.Reservation> filtered = new ArrayList<>();
        
        for (com.example.droidtour.models.Reservation r : reservations) {
            Date reservationDate = getReservationDateForFiltering(r);
            if (reservationDate == null) {
                continue;
            }
            
            reservationCal.setTime(reservationDate);
            // Normalizar hora a medianoche para comparación precisa
            reservationCal.set(Calendar.HOUR_OF_DAY, 0);
            reservationCal.set(Calendar.MINUTE, 0);
            reservationCal.set(Calendar.SECOND, 0);
            reservationCal.set(Calendar.MILLISECOND, 0);
            
            boolean matches = false;
            switch (periodType) {
                case 0: // Diario
                    matches = reservationCal.get(Calendar.YEAR) == periodCal.get(Calendar.YEAR) &&
                             reservationCal.get(Calendar.MONTH) == periodCal.get(Calendar.MONTH) &&
                             reservationCal.get(Calendar.DAY_OF_MONTH) == periodCal.get(Calendar.DAY_OF_MONTH);
                    break;
                case 1: // Mensual
                    matches = reservationCal.get(Calendar.YEAR) == periodCal.get(Calendar.YEAR) &&
                             reservationCal.get(Calendar.MONTH) == periodCal.get(Calendar.MONTH);
                    break;
                case 2: // Anual
                    matches = reservationCal.get(Calendar.YEAR) == periodCal.get(Calendar.YEAR);
                    break;
            }
            
            if (matches) {
                filtered.add(r);
            }
        }
        
        return filtered;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Adapter para ViewPager2
    private class SalesReportsPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        
        private int currentPeriodType = 3; // General por defecto
        private Date currentPeriodDate = null;
        
        public SalesReportsPagerAdapter(androidx.fragment.app.FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment;
            Bundle args = new Bundle();
            args.putInt("periodType", currentPeriodType);
            if (currentPeriodDate != null) {
                args.putLong("periodDate", currentPeriodDate.getTime());
            }
            
            switch (position) {
                case 0:
                    fragment = new SalesByServiceFragment();
                    break;
                case 1:
                    fragment = new SalesByTourFragment();
                    break;
                case 2:
                    fragment = new SalesSummaryFragment();
                    break;
                default:
                    fragment = new SalesByServiceFragment();
            }
            
            fragment.setArguments(args);
            return fragment;
        }
        
        @Override
        public int getItemCount() {
            return 3;
        }
        
        public void notifyPeriodChanged(int periodType, Date periodDate) {
            this.currentPeriodType = periodType;
            this.currentPeriodDate = periodDate;
            // No usar notifyDataSetChanged() porque recrea los fragmentos
            // En su lugar, notificamos directamente a los fragmentos existentes
        }
    }
    
    private void redirectToLogin() {
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
