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
        DatabaseReference clientIndexRef = indexRef.child("client_" + clientId);
        
        clientIndexRef.addValueEventListener(new ValueEventListener() {
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
                Log.e(TAG, "Error listening to client conversations", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Escucha cambios en las conversaciones de una empresa en tiempo real
     */
    public void listenToCompanyConversations(String companyId, ConversationsCallback callback) {
        DatabaseReference companyIndexRef = indexRef.child("company_" + companyId);
        
        companyIndexRef.addValueEventListener(new ValueEventListener() {
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
                Log.e(TAG, "Error listening to company conversations", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    // ==================== MÉTODOS PRIVADOS ====================
    
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

