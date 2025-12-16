package com.example.droidtour.utils;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ConversationHelper: Gestiona conversaciones y sus índices
 */
public class ConversationHelper {
    private static final String TAG = "ConversationHelper";
    private static final String PATH_CONVERSATIONS = "conversations";
    private static final String PATH_INDEX = "conversation_index";
    
    private final FirebaseDatabase database;
    private DatabaseReference conversationsRef;
    private DatabaseReference indexRef;
    
    // Mapas para mantener referencias a los listeners activos
    private Map<String, ValueEventListener> activeIndexListeners = new HashMap<>();
    private Map<String, ValueEventListener> activeConversationListeners = new HashMap<>();
    
    public ConversationHelper() {
        database = FirebaseDatabase.getInstance("https://droidtour-default-rtdb.firebaseio.com/");
        conversationsRef = database.getReference(PATH_CONVERSATIONS);
        indexRef = database.getReference(PATH_INDEX);
    }
    
    public interface ConversationData {
        String getConversationId();
        String getClientId();
        String getCompanyId();
        String getClientName();
        String getCompanyName();
        String getLastMessage();
        long getLastMessageTimestamp();
        String getLastMessageSenderId();
        boolean getLastMessageHasAttachment();
        String getLastMessageAttachmentType();
        int getUnreadCountClient();
        int getUnreadCountAdmin();
    }
    
    public interface ConversationsCallback {
        void onConversationsLoaded(List<ConversationData> conversations);
        void onError(Exception e);
    }
    
    public interface ConversationCallback {
        void onConversationLoaded(ConversationData conversation);
        void onError(Exception e);
    }
    
    /**
     * Obtiene todas las conversaciones de un cliente
     */
    public void getConversationsForClient(String clientId, ConversationsCallback callback) {
        DatabaseReference clientIndexRef = indexRef.child("client_" + clientId);
        
        clientIndexRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> conversationIds = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    conversationIds.add(child.getKey());
                }
                
                loadConversations(conversationIds, callback);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading client conversations", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Obtiene todas las conversaciones de una empresa
     */
    public void getConversationsForCompany(String companyId, ConversationsCallback callback) {
        DatabaseReference companyIndexRef = indexRef.child("company_" + companyId);
        
        companyIndexRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> conversationIds = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    conversationIds.add(child.getKey());
                }
                
                loadConversations(conversationIds, callback);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading company conversations", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Obtiene una conversación por ID
     */
    public void getConversationById(String conversationId, ConversationCallback callback) {
        DatabaseReference conversationRef = conversationsRef.child(conversationId);
        
        conversationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ConversationData conversation = snapshotToConversation(snapshot);
                    if (callback != null) {
                        callback.onConversationLoaded(conversation);
                    }
                } else {
                    if (callback != null) {
                        callback.onError(new Exception("Conversación no encontrada"));
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading conversation", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Escucha cambios en las conversaciones de un cliente en tiempo real
     */
    public void listenToClientConversations(String clientId, ConversationsCallback callback) {
        String listenerKey = "client_" + clientId;
        
        // Remover listener anterior si existe
        if (activeIndexListeners.containsKey(listenerKey)) {
            DatabaseReference oldRef = indexRef.child("client_" + clientId);
            oldRef.removeEventListener(activeIndexListeners.get(listenerKey));
            activeIndexListeners.remove(listenerKey);
        }
        
        DatabaseReference clientIndexRef = indexRef.child("client_" + clientId);
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> conversationIds = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    conversationIds.add(child.getKey());
                }
                
                loadConversationsWithListeners(conversationIds, callback, "client_" + clientId);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error listening to client conversations", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        };
        
        clientIndexRef.addValueEventListener(listener);
        activeIndexListeners.put(listenerKey, listener);
    }
    
    /**
     * Escucha cambios en las conversaciones de una empresa en tiempo real
     */
    public void listenToCompanyConversations(String companyId, ConversationsCallback callback) {
        String listenerKey = "company_" + companyId;
        
        // Remover listener anterior si existe
        if (activeIndexListeners.containsKey(listenerKey)) {
            DatabaseReference oldRef = indexRef.child("company_" + companyId);
            oldRef.removeEventListener(activeIndexListeners.get(listenerKey));
            activeIndexListeners.remove(listenerKey);
        }
        
        DatabaseReference companyIndexRef = indexRef.child("company_" + companyId);
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> conversationIds = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    conversationIds.add(child.getKey());
                }
                
                loadConversationsWithListeners(conversationIds, callback, "company_" + companyId);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error listening to company conversations", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        };
        
        companyIndexRef.addValueEventListener(listener);
        activeIndexListeners.put(listenerKey, listener);
    }
    
    // ==================== MÉTODOS PRIVADOS ====================
    
    /**
     * Carga conversaciones con listeners de una sola vez (para carga inicial)
     */
    private void loadConversations(List<String> conversationIds, ConversationsCallback callback) {
        if (conversationIds.isEmpty()) {
            if (callback != null) {
                callback.onConversationsLoaded(new ArrayList<>());
            }
            return;
        }
        
        List<ConversationData> conversations = new ArrayList<>();
        final int[] completed = {0};
        final int total = conversationIds.size();
        
        for (String conversationId : conversationIds) {
            DatabaseReference conversationRef = conversationsRef.child(conversationId);
            
            conversationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        ConversationData conversation = snapshotToConversation(snapshot);
                        conversations.add(conversation);
                    }
                    
                    completed[0]++;
                    if (completed[0] == total && callback != null) {
                        // Ordenar por lastMessageTimestamp descendente
                        conversations.sort((a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
                        callback.onConversationsLoaded(conversations);
                    }
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    completed[0]++;
                    if (completed[0] == total && callback != null) {
                        callback.onConversationsLoaded(conversations);
                    }
                }
            });
        }
    }
    
    /**
     * Carga conversaciones con listeners continuos para actualizaciones en tiempo real
     */
    private void loadConversationsWithListeners(List<String> conversationIds, ConversationsCallback callback, String parentKey) {
        if (conversationIds.isEmpty()) {
            if (callback != null) {
                callback.onConversationsLoaded(new ArrayList<>());
            }
            return;
        }
        
        // Remover listeners anteriores de conversaciones que ya no existen
        List<String> toRemove = new ArrayList<>();
        for (String key : activeConversationListeners.keySet()) {
            if (key.startsWith(parentKey + "_") && !conversationIds.contains(key.substring(parentKey.length() + 1))) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            String conversationId = key.substring(parentKey.length() + 1);
            DatabaseReference oldRef = conversationsRef.child(conversationId);
            oldRef.removeEventListener(activeConversationListeners.get(key));
            activeConversationListeners.remove(key);
        }
        
        // Map para mantener las conversaciones actualizadas
        final Map<String, ConversationData> conversationMap = new HashMap<>();
        final int[] completed = {0};
        final int total = conversationIds.size();
        
        for (String conversationId : conversationIds) {
            String listenerKey = parentKey + "_" + conversationId;
            
            // Si ya existe un listener para esta conversación, no crear otro
            if (activeConversationListeners.containsKey(listenerKey)) {
                // Obtener el valor actual
                DatabaseReference conversationRef = conversationsRef.child(conversationId);
                conversationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            ConversationData conversation = snapshotToConversation(snapshot);
                            conversationMap.put(conversationId, conversation);
                        }
                        completed[0]++;
                        checkAndNotify(completed, total, conversationMap, callback);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        completed[0]++;
                        checkAndNotify(completed, total, conversationMap, callback);
                    }
                });
                continue;
            }
            
            DatabaseReference conversationRef = conversationsRef.child(conversationId);
            
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        ConversationData conversation = snapshotToConversation(snapshot);
                        conversationMap.put(conversationId, conversation);
                    } else {
                        conversationMap.remove(conversationId);
                    }
                    
                    // Notificar cambios cada vez que se actualiza una conversación
                    if (callback != null) {
                        List<ConversationData> updatedConversations = new ArrayList<>(conversationMap.values());
                        updatedConversations.sort((a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
                        callback.onConversationsLoaded(updatedConversations);
                    }
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Error listening to conversation: " + conversationId, error.toException());
                    conversationMap.remove(conversationId);
                    if (callback != null) {
                        List<ConversationData> updatedConversations = new ArrayList<>(conversationMap.values());
                        updatedConversations.sort((a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
                        callback.onConversationsLoaded(updatedConversations);
                    }
                }
            };
            
            conversationRef.addValueEventListener(listener);
            activeConversationListeners.put(listenerKey, listener);
            
            // Cargar valor inicial
            conversationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        ConversationData conversation = snapshotToConversation(snapshot);
                        conversationMap.put(conversationId, conversation);
                    }
                    completed[0]++;
                    checkAndNotify(completed, total, conversationMap, callback);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    completed[0]++;
                    checkAndNotify(completed, total, conversationMap, callback);
                }
            });
        }
    }
    
    /**
     * Verifica si todas las conversaciones se han cargado y notifica al callback
     */
    private void checkAndNotify(int[] completed, int total, Map<String, ConversationData> conversationMap, ConversationsCallback callback) {
        if (completed[0] == total && callback != null) {
            List<ConversationData> conversations = new ArrayList<>(conversationMap.values());
            conversations.sort((a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
            callback.onConversationsLoaded(conversations);
        }
    }
    
    /**
     * Detiene todos los listeners activos
     */
    public void stopAllListeners() {
        // Remover listeners del índice
        for (Map.Entry<String, ValueEventListener> entry : activeIndexListeners.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("client_")) {
                String clientId = key.substring(7);
                DatabaseReference ref = indexRef.child("client_" + clientId);
                ref.removeEventListener(entry.getValue());
            } else if (key.startsWith("company_")) {
                String companyId = key.substring(8);
                DatabaseReference ref = indexRef.child("company_" + companyId);
                ref.removeEventListener(entry.getValue());
            }
        }
        activeIndexListeners.clear();
        
        // Remover listeners de conversaciones
        for (Map.Entry<String, ValueEventListener> entry : activeConversationListeners.entrySet()) {
            String key = entry.getKey();
            // Extraer conversationId del key (formato: "client_xxx_convId" o "company_xxx_convId")
            int lastUnderscore = key.lastIndexOf('_');
            if (lastUnderscore > 0) {
                String conversationId = key.substring(lastUnderscore + 1);
                DatabaseReference ref = conversationsRef.child(conversationId);
                ref.removeEventListener(entry.getValue());
            }
        }
        activeConversationListeners.clear();
    }
    
    private ConversationData snapshotToConversation(DataSnapshot snapshot) {
        return new ConversationData() {
            @Override
            public String getConversationId() {
                return snapshot.getKey();
            }
            
            @Override
            public String getClientId() {
                return snapshot.child("clientId").getValue(String.class);
            }
            
            @Override
            public String getCompanyId() {
                return snapshot.child("companyId").getValue(String.class);
            }
            
            @Override
            public String getClientName() {
                return snapshot.child("clientName").getValue(String.class);
            }
            
            @Override
            public String getCompanyName() {
                return snapshot.child("companyName").getValue(String.class);
            }
            
            @Override
            public String getLastMessage() {
                return snapshot.child("lastMessage").getValue(String.class);
            }
            
            @Override
            public long getLastMessageTimestamp() {
                Long timestamp = snapshot.child("lastMessageTimestamp").getValue(Long.class);
                return timestamp != null ? timestamp : 0;
            }
            
            @Override
            public String getLastMessageSenderId() {
                return snapshot.child("lastMessageSenderId").getValue(String.class);
            }
            
            @Override
            public boolean getLastMessageHasAttachment() {
                Boolean hasAttachment = snapshot.child("lastMessageHasAttachment").getValue(Boolean.class);
                return hasAttachment != null && hasAttachment;
            }
            
            @Override
            public String getLastMessageAttachmentType() {
                return snapshot.child("lastMessageAttachmentType").getValue(String.class);
            }
            
            @Override
            public int getUnreadCountClient() {
                Long count = snapshot.child("unreadCountClient").getValue(Long.class);
                return count != null ? count.intValue() : 0;
            }
            
            @Override
            public int getUnreadCountAdmin() {
                Long count = snapshot.child("unreadCountAdmin").getValue(Long.class);
                return count != null ? count.intValue() : 0;
            }
        };
    }
}

