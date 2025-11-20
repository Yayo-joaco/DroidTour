package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.client.ClientNotificationsAdapter;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Notification;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Actividad de Notificaciones para Guía de Turista
 * Similar a ClientNotificationsActivity pero adaptada para guías
 */
public class GuideNotificationsActivity extends AppCompatActivity {
    
    private static final String TAG = "GuideNotifications";
    private RecyclerView rvNotifications;
    private TabLayout tabLayout;
    private ClientNotificationsAdapter notificationsAdapter;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private String currentUserId;
    private List<Notification> allNotifications = new ArrayList<>();
    private List<Notification> filteredNotifications = new ArrayList<>();
    private String currentFilter = "Todas";
    
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
        
        // Validar que el usuario sea GUIDE
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_client_notifications); // Reutilizar el mismo layout
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        // Inicializar Firebase
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);
        currentUserId = authManager.getCurrentUserId();
        
        // Fallback a PreferencesManager
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = prefsManager.getUserId();
        }
        
        Log.d(TAG, "📱 GuideNotificationsActivity iniciada para userId: " + currentUserId);
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupTabs();
        loadNotifications();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recargar notificaciones cuando volvemos a la actividad
        loadNotifications();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notificaciones");
        }
    }
    
    private void initializeViews() {
        rvNotifications = findViewById(R.id.rv_notifications);
        tabLayout = findViewById(R.id.tab_layout);
    }
    
    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationsAdapter = new ClientNotificationsAdapter(filteredNotifications, 
            (position, action) -> {
                if (position >= 0 && position < filteredNotifications.size()) {
                    Notification notification = filteredNotifications.get(position);
                    onNotificationAction(notification, action);
                }
            });
        rvNotifications.setAdapter(notificationsAdapter);
    }
    
    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        currentFilter = "Todas";
                        break;
                    case 1:
                        currentFilter = "Sin leer";
                        break;
                }
                filterNotifications();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void loadNotifications() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "❌ Error: currentUserId es null o vacío");
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "🔄 Cargando notificaciones para userId: " + currentUserId);
        
        if (currentFilter.equals("Sin leer")) {
            loadUnreadNotifications();
        } else {
            loadAllNotifications();
        }
    }
    
    private void loadAllNotifications() {
        firestoreManager.getNotificationsByUser(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                allNotifications = (List<Notification>) result;
                Log.d(TAG, "✅ Notificaciones cargadas: " + allNotifications.size());
                filterNotifications();
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando notificaciones", e);
                Toast.makeText(GuideNotificationsActivity.this, 
                    "Error cargando notificaciones", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadUnreadNotifications() {
        firestoreManager.getUnreadNotifications(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                allNotifications = (List<Notification>) result;
                Log.d(TAG, "✅ Notificaciones sin leer: " + allNotifications.size());
                filterNotifications();
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando notificaciones sin leer", e);
                Toast.makeText(GuideNotificationsActivity.this, 
                    "Error cargando notificaciones", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void filterNotifications() {
        filteredNotifications.clear();
        
        if (currentFilter.equals("Todas")) {
            filteredNotifications.addAll(allNotifications);
        } else if (currentFilter.equals("Sin leer")) {
            for (Notification notification : allNotifications) {
                if (notification.getIsRead() != null && !notification.getIsRead()) {
                    filteredNotifications.add(notification);
                }
            }
        }
        
        notificationsAdapter.updateData(filteredNotifications);
        
        Log.d(TAG, "📊 Notificaciones filtradas (" + currentFilter + "): " + filteredNotifications.size());
    }
    
    private void onNotificationAction(Notification notification, String action) {
        switch (action) {
            case "mark_read":
                markAsRead(notification);
                break;
            case "delete":
                deleteNotification(notification);
                break;
        }
    }
    
    private void markAsRead(Notification notification) {
        if (notification.getNotificationId() == null) return;
        
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("isRead", true);
        
        firestoreManager.updateNotification(notification.getNotificationId(), updates, 
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "✅ Notificación marcada como leída");
                    loadNotifications(); // Recargar
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "❌ Error marcando como leída", e);
                    Toast.makeText(GuideNotificationsActivity.this, 
                        "Error al marcar como leída", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void deleteNotification(Notification notification) {
        if (notification.getNotificationId() == null) return;
        
        firestoreManager.deleteNotification(notification.getNotificationId(), 
            new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    Log.d(TAG, "✅ Notificación eliminada");
                    Toast.makeText(GuideNotificationsActivity.this, 
                        "Notificación eliminada", Toast.LENGTH_SHORT).show();
                    loadNotifications(); // Recargar
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "❌ Error eliminando notificación", e);
                    Toast.makeText(GuideNotificationsActivity.this, 
                        "Error al eliminar", Toast.LENGTH_SHORT).show();
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

