package com.example.droidtour.client;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.example.droidtour.R;

public class ClientNotificationsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView rvNotifications;
    private ClientNotificationsAdapter notificationsAdapter;
    private ImageView ivMarkAllRead;
    private LinearLayout emptyState, loadingState;
    private com.example.droidtour.utils.PreferencesManager prefsManager;

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
        
        // Validar que el usuario sea CLIENT
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_notifications);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        initializeViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupClickListeners();

        // Simulate loading
        showLoading();
        loadNotifications();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        rvNotifications = findViewById(R.id.rv_notifications);
        ivMarkAllRead = findViewById(R.id.iv_mark_all_read);
        emptyState = findViewById(R.id.empty_state);
        loadingState = findViewById(R.id.loading_state);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterNotifications(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationsAdapter = new ClientNotificationsAdapter((position, action) -> onNotificationAction(position, action));
        rvNotifications.setAdapter(notificationsAdapter);
    }

    private void setupClickListeners() {
        ivMarkAllRead.setOnClickListener(v -> {
            Toast.makeText(this, "Todas marcadas como leídas", Toast.LENGTH_SHORT).show();
            notificationsAdapter.markAllAsRead();
        });
    }

    private void filterNotifications(int tabPosition) {
        showLoading();
        // Simulate API call
        rvNotifications.postDelayed(() -> {
            hideLoading();
            Toast.makeText(this, "Filtro: " + getFilterName(tabPosition), Toast.LENGTH_SHORT).show();
        }, 500);
    }

    private String getFilterName(int position) {
        switch (position) {
            case 0: return "Todas";
            case 1: return "Sin leer";
            case 2: return "Importantes";
            default: return "Todas";
        }
    }

    private void loadNotifications() {
        // Simulate API call
        rvNotifications.postDelayed(() -> {
            hideLoading();
            // If no notifications, show empty state
            // emptyState.setVisibility(View.VISIBLE);
        }, 1500);
    }

    private void showLoading() {
        loadingState.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingState.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.VISIBLE);
    }

    private void onNotificationAction(int position, String action) {
        switch (action) {
            case "click":
                Toast.makeText(this, "Abrir notificación " + position, Toast.LENGTH_SHORT).show();
                notificationsAdapter.markAsRead(position);
                break;
            case "menu":
                Toast.makeText(this, "Abrir menú opción para " + position, Toast.LENGTH_SHORT).show();
                break;
            case "delete":
                Toast.makeText(this, "Notificación eliminada", Toast.LENGTH_SHORT).show();
                notificationsAdapter.removeNotification(position);
                break;
            case "mark_read":
                Toast.makeText(this, "Marcada como leída", Toast.LENGTH_SHORT).show();
                notificationsAdapter.markAsRead(position);
                break;
        }
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
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}