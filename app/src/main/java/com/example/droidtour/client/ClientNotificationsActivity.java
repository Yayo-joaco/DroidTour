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
import com.example.droidtour.models.Notification;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.firebase.FirebaseAuthManager;

import java.util.ArrayList;
import java.util.List;

public class ClientNotificationsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView rvNotifications;
    private ClientNotificationsAdapter notificationsAdapter;
    private ImageView ivMarkAllRead;
    private LinearLayout emptyState, loadingState;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private String currentUserId;
    private List<Notification> notificationsList = new ArrayList<>();

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

        // Inicializar Firebase
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);
        currentUserId = authManager.getCurrentUserId();
        
        // Si no hay usuario autenticado, usar del PreferencesManager
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = prefsManager.getUserId();
        }

        initializeViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupClickListeners();

        // 🔥 Cargar notificaciones desde Firestore
        showLoading();
        loadNotificationsFromFirestore();
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
        notificationsAdapter = new ClientNotificationsAdapter(notificationsList, (position, action) -> onNotificationAction(position, action));
        rvNotifications.setAdapter(notificationsAdapter);
    }
    
    /**
     * 🔥 Cargar notificaciones desde Firestore
     */
    private void loadNotificationsFromFirestore() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            hideLoading();
            emptyState.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        firestoreManager.getNotificationsByUser(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                notificationsList = (List<Notification>) result;
                hideLoading();
                
                if (notificationsList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvNotifications.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);
                    notificationsAdapter.updateData(notificationsList);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                hideLoading();
                emptyState.setVisibility(View.VISIBLE);
                Toast.makeText(ClientNotificationsActivity.this, 
                    "Error al cargar notificaciones: " + e.getMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e("ClientNotifications", "Error loading notifications", e);
            }
        });
    }

    private void setupClickListeners() {
        ivMarkAllRead.setOnClickListener(v -> {
            markAllNotificationsAsRead();
        });
    }
    
    /**
     * 🔥 Marcar todas las notificaciones como leídas
     */
    private void markAllNotificationsAsRead() {
        if (currentUserId == null || currentUserId.isEmpty()) return;
        
        firestoreManager.markAllNotificationsAsRead(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(ClientNotificationsActivity.this, 
                    "Todas marcadas como leídas", Toast.LENGTH_SHORT).show();
                loadNotificationsFromFirestore(); // Recargar
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClientNotificationsActivity.this, 
                    "Error al marcar notificaciones", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterNotifications(int tabPosition) {
        showLoading();
        
        if (tabPosition == 0) {
            // Tab 0: Todas las notificaciones
            loadNotificationsFromFirestore();
        } else if (tabPosition == 1) {
            // Tab 1: Sin leer
            firestoreManager.getUnreadNotifications(currentUserId, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    notificationsList = (List<Notification>) result;
                    hideLoading();
                    updateUI();
                }
                
                @Override
                public void onFailure(Exception e) {
                    hideLoading();
                    Toast.makeText(ClientNotificationsActivity.this, 
                        "Error al filtrar notificaciones sin leer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Fallback: mostrar todas
            loadNotificationsFromFirestore();
        }
    }
    
    /**
     * Actualizar UI después de cargar notificaciones
     */
    private void updateUI() {
        notificationsAdapter.updateData(notificationsList);
        
        if (notificationsList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private String getFilterName(int position) {
        switch (position) {
            case 0: return "Todas";
            case 1: return "Sin leer";
            case 2: return "Importantes";
            default: return "Todas";
        }
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
        if (position < 0 || position >= notificationsList.size()) return;
        
        Notification notification = notificationsList.get(position);
        
        switch (action) {
            case "click":
                markNotificationAsRead(notification);
                Toast.makeText(this, "Notificación: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                // TODO: Navegar a la pantalla relacionada
                break;
            case "menu":
                Toast.makeText(this, "Abrir menú opción", Toast.LENGTH_SHORT).show();
                break;
            case "delete":
                // TODO: Implementar eliminación de notificación
                Toast.makeText(this, "Función de eliminación no implementada aún", Toast.LENGTH_SHORT).show();
                break;
            case "mark_read":
                markNotificationAsRead(notification);
                break;
        }
    }
    
    /**
     * 🔥 Marcar notificación como leída
     */
    private void markNotificationAsRead(Notification notification) {
        if (notification.isReadNotification()) return; // Ya está leída
        
        firestoreManager.markNotificationAsRead(notification.getNotificationId(), 
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                        notification.setIsRead(true);
                    notificationsAdapter.notifyDataSetChanged();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(ClientNotificationsActivity.this, 
                        "Error al marcar notificación", Toast.LENGTH_SHORT).show();
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
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}