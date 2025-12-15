package com.example.droidtour.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * PresenceManager: Gestiona el estado en línea de los usuarios
 * Usa Firebase Realtime Database con onDisconnect() para detectar desconexiones
 */
public class PresenceManager {
    private static final String TAG = "PresenceManager";
    private static final String PATH_PRESENCE = "user_presence";
    private static final long HEARTBEAT_INTERVAL = 30000; // 30 segundos
    
    private final FirebaseDatabase database;
    private DatabaseReference presenceRef;
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private String currentUserId;
    private boolean isOnline;
    
    public PresenceManager() {
        database = FirebaseDatabase.getInstance("https://droidtour-default-rtdb.firebaseio.com/");
        presenceRef = database.getReference(PATH_PRESENCE);
        heartbeatHandler = new Handler(Looper.getMainLooper());
    }
    
    public interface PresenceCallback {
        void onPresenceChanged(String userId, boolean isOnline, long lastSeen);
        void onError(Exception e);
    }
    
    /**
     * Establece el usuario como en línea
     * Configura onDisconnect() para marcarlo como offline automáticamente
     */
    public void setOnline(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "setOnline: userId inválido");
            return;
        }
        
        currentUserId = userId;
        DatabaseReference userPresenceRef = presenceRef.child(userId);
        
        // Establecer estado en línea
        Map<String, Object> onlineStatus = new HashMap<>();
        onlineStatus.put("isOnline", true);
        onlineStatus.put("lastSeen", ServerValue.TIMESTAMP);
        onlineStatus.put("status", "online");
        
        userPresenceRef.setValue(onlineStatus);
        
        // Configurar onDisconnect para marcar como offline cuando se desconecte
        Map<String, Object> offlineStatus = new HashMap<>();
        offlineStatus.put("isOnline", false);
        offlineStatus.put("status", "offline");
        offlineStatus.put("lastSeen", ServerValue.TIMESTAMP);
        
        userPresenceRef.onDisconnect().setValue(offlineStatus);
        
        // Iniciar heartbeat
        startHeartbeat(userId);
        isOnline = true;
        
        Log.d(TAG, "Usuario " + userId + " marcado como en línea");
    }
    
    /**
     * Establece el usuario como offline
     */
    public void setOffline(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        stopHeartbeat();
        
        DatabaseReference userPresenceRef = presenceRef.child(userId);
        
        Map<String, Object> offlineStatus = new HashMap<>();
        offlineStatus.put("isOnline", false);
        offlineStatus.put("status", "offline");
        offlineStatus.put("lastSeen", ServerValue.TIMESTAMP);
        
        userPresenceRef.setValue(offlineStatus);
        userPresenceRef.onDisconnect().cancel(); // Cancelar onDisconnect
        
        isOnline = false;
        currentUserId = null;
        
        Log.d(TAG, "Usuario " + userId + " marcado como offline");
    }
    
    /**
     * Inicia el heartbeat para actualizar lastSeen periódicamente
     */
    public void startHeartbeat(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        stopHeartbeat(); // Detener heartbeat anterior si existe
        
        currentUserId = userId;
        DatabaseReference userPresenceRef = presenceRef.child(userId);
        
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isOnline && currentUserId != null && currentUserId.equals(userId)) {
                    // Actualizar solo lastSeen, mantener isOnline
                    userPresenceRef.child("lastSeen").setValue(ServerValue.TIMESTAMP);
                    
                    // Programar siguiente heartbeat
                    heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
                }
            }
        };
        
        // Iniciar heartbeat inmediatamente y luego cada intervalo
        heartbeatHandler.post(heartbeatRunnable);
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL);
        
        Log.d(TAG, "Heartbeat iniciado para usuario " + userId);
    }
    
    /**
     * Detiene el heartbeat
     */
    public void stopHeartbeat() {
        if (heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            heartbeatRunnable = null;
        }
    }
    
    /**
     * Obtiene el estado de presencia de un usuario
     */
    public void getUserPresence(String userId, PresenceCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("userId inválido"));
            }
            return;
        }
        
        DatabaseReference userPresenceRef = presenceRef.child(userId);
        
        userPresenceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
                    Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                    
                    boolean online = isOnline != null && isOnline;
                    long lastSeenTime = lastSeen != null ? lastSeen : 0;
                    
                    if (callback != null) {
                        callback.onPresenceChanged(userId, online, lastSeenTime);
                    }
                } else {
                    // Usuario no tiene presencia registrada (offline)
                    if (callback != null) {
                        callback.onPresenceChanged(userId, false, 0);
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error getting user presence", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Escucha cambios en el estado de presencia de un usuario en tiempo real
     */
    public void listenToPresence(String userId, PresenceCallback callback) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        DatabaseReference userPresenceRef = presenceRef.child(userId);
        
        userPresenceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
                    Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                    
                    boolean online = isOnline != null && isOnline;
                    long lastSeenTime = lastSeen != null ? lastSeen : 0;
                    
                    if (callback != null) {
                        callback.onPresenceChanged(userId, online, lastSeenTime);
                    }
                } else {
                    // Usuario no tiene presencia registrada (offline)
                    if (callback != null) {
                        callback.onPresenceChanged(userId, false, 0);
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error listening to presence", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Limpia recursos cuando la app se cierra
     */
    public void cleanup() {
        stopHeartbeat();
        if (currentUserId != null) {
            setOffline(currentUserId);
        }
    }
    
    /**
     * Verifica si el usuario actual está en línea
     */
    public boolean isOnline() {
        return isOnline;
    }
}

