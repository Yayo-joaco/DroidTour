package com.example.droidtour.utils;

import android.util.Log;
import com.example.droidtour.models.Message;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatManager para Firebase Realtime Database
 * Maneja el envío y recepción de mensajes en tiempo real
 */
public class ChatManagerRealtime {
    private static final String TAG = "ChatManagerRealtime";
    
    private static final String PATH_CONVERSATIONS = "conversations";
    private static final String PATH_MESSAGES = "messages";
    
    private final FirebaseDatabase database;
    private DatabaseReference conversationsRef;
    private ChildEventListener messagesListener;
    private String currentConversationId;
    
    public ChatManagerRealtime() {
        database = FirebaseDatabase.getInstance("https://droidtour-default-rtdb.firebaseio.com/");
        // setPersistenceEnabled() debe llamarse antes de cualquier uso de FirebaseDatabase
        // Se configura en DroidTourApplication.onCreate()
        conversationsRef = database.getReference(PATH_CONVERSATIONS);
    }
    
    public interface SendCallback {
        void onSuccess(String messageId);
        void onFailure(Exception e);
        default void onUploadProgress(int progress) {} // Método opcional para progreso de subida
    }
    
    public interface MessagesListener {
        void onNewMessage(Message message);
        void onMessageUpdated(Message message);
        void onError(Exception e);
    }
    
    /**
     * Crea o obtiene una conversación entre cliente y empresa
     */
    public void createOrGetConversation(String clientId, String companyId, 
                                        String clientName, String companyName,
                                        ConversationCallback callback) {
        // Generar ID de conversación único
        String conversationId = generateConversationId(clientId, companyId);
        
        DatabaseReference conversationRef = conversationsRef.child(conversationId);
        
        // Verificar si la conversación ya existe
        conversationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Conversación existe
                    if (callback != null) {
                        callback.onConversationFound(conversationId);
                    }
                } else {
                    // Crear nueva conversación
                    Map<String, Object> conversationData = new HashMap<>();
                    conversationData.put("clientId", clientId);
                    conversationData.put("companyId", companyId);
                    conversationData.put("clientName", clientName);
                    conversationData.put("companyName", companyName);
                    conversationData.put("lastMessage", "");
                    conversationData.put("lastMessageTimestamp", ServerValue.TIMESTAMP);
                    conversationData.put("lastMessageSenderId", "");
                    conversationData.put("unreadCountClient", 0);
                    conversationData.put("unreadCountAdmin", 0);
                    conversationData.put("createdAt", ServerValue.TIMESTAMP);
                    conversationData.put("updatedAt", ServerValue.TIMESTAMP);
                    
                    conversationRef.setValue(conversationData)
                        .addOnSuccessListener(aVoid -> {
                            // Crear índices
                            createConversationIndex(clientId, companyId, conversationId);
                            if (callback != null) {
                                callback.onConversationCreated(conversationId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error creating conversation", e);
                            if (callback != null) {
                                callback.onError(e);
                            }
                        });
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error checking conversation", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Envía un mensaje a una conversación
     */
    public void sendMessage(String conversationId, Message message, SendCallback callback) {
        if (conversationId == null || conversationId.isEmpty() || message == null) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("conversationId o message inválido"));
            }
            return;
        }
        
        DatabaseReference messagesRef = conversationsRef
            .child(conversationId)
            .child(PATH_MESSAGES)
            .push(); // Genera ID único automáticamente
        
        String messageId = messagesRef.getKey();
        message.setMessageId(messageId);
        message.setConversationId(conversationId);
        
        // Preparar datos del mensaje para Realtime Database
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messageId);
        messageData.put("senderId", message.getSenderId());
        messageData.put("senderName", message.getSenderName());
        messageData.put("senderType", message.getSenderType());
        messageData.put("receiverId", message.getReceiverId());
        messageData.put("receiverName", message.getReceiverName());
        messageData.put("messageText", message.getMessageText() != null ? message.getMessageText() : "");
        messageData.put("status", "SENT");
        messageData.put("timestamp", ServerValue.TIMESTAMP);
        messageData.put("isRead", false);
        
        // Agregar campos de archivos adjuntos si existen
        if (message.hasAttachment()) {
            messageData.put("hasAttachment", true);
            if (message.getAttachmentUrl() != null) {
                messageData.put("attachmentUrl", message.getAttachmentUrl());
            }
            if (message.getAttachmentType() != null) {
                messageData.put("attachmentType", message.getAttachmentType());
            }
            if (message.getAttachmentName() != null) {
                messageData.put("attachmentName", message.getAttachmentName());
            }
            if (message.getAttachmentSize() != null) {
                messageData.put("attachmentSize", message.getAttachmentSize());
            }
        } else {
            messageData.put("hasAttachment", false);
        }
        
        // Guardar mensaje
        messagesRef.setValue(messageData)
            .addOnSuccessListener(aVoid -> {
                // Actualizar información de la conversación
                updateConversationAfterMessage(conversationId, message);
                
                if (callback != null) {
                    callback.onSuccess(messageId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending message", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            });
    }
    
    /**
     * Escucha mensajes nuevos en tiempo real para una conversación
     */
    public void listenForMessages(String conversationId, MessagesListener listener) {
        if (conversationId == null || conversationId.isEmpty()) {
            return;
        }
        
        // Remover listener anterior si existe
        stopListening();
        
        currentConversationId = conversationId;
        DatabaseReference messagesRef = conversationsRef
            .child(conversationId)
            .child(PATH_MESSAGES);
        
        // Ordenar por timestamp y limitar a últimos 50 mensajes
        Query query = messagesRef.orderByChild("timestamp").limitToLast(50);
        
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Message message = snapshotToMessage(snapshot);
                if (message != null && listener != null) {
                    listener.onNewMessage(message);
                }
            }
            
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                Message message = snapshotToMessage(snapshot);
                if (message != null && listener != null) {
                    listener.onMessageUpdated(message);
                }
            }
            
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                // No manejamos eliminación por ahora
            }
            
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                // No necesario para chat
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error listening to messages", error.toException());
                if (listener != null) {
                    listener.onError(error.toException());
                }
            }
        };
        
        query.addChildEventListener(messagesListener);
    }
    
    /**
     * Carga mensajes históricos (paginación)
     */
    public void loadMessages(String conversationId, long beforeTimestamp, int limit,
                            MessagesLoadCallback callback) {
        DatabaseReference messagesRef = conversationsRef
            .child(conversationId)
            .child(PATH_MESSAGES);
        
        Query query = messagesRef
            .orderByChild("timestamp")
            .endAt(beforeTimestamp)
            .limitToLast(limit);
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Message> messages = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message message = snapshotToMessage(child);
                    if (message != null) {
                        messages.add(message);
                    }
                }
                if (callback != null) {
                    callback.onMessagesLoaded(messages);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading messages", error.toException());
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }
    
    /**
     * Marca un mensaje como entregado
     */
    public void markMessageAsDelivered(String conversationId, String messageId) {
        DatabaseReference messageRef = conversationsRef
            .child(conversationId)
            .child(PATH_MESSAGES)
            .child(messageId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "DELIVERED");
        updates.put("deliveredAt", ServerValue.TIMESTAMP);
        
        messageRef.updateChildren(updates);
    }
    
    /**
     * Marca un mensaje como leído
     */
    public void markMessageAsRead(String conversationId, String messageId) {
        DatabaseReference messageRef = conversationsRef
            .child(conversationId)
            .child(PATH_MESSAGES)
            .child(messageId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "READ");
        updates.put("isRead", true);
        updates.put("readAt", ServerValue.TIMESTAMP);
        
        messageRef.updateChildren(updates);
    }
    
    /**
     * Marca todos los mensajes de una conversación como leídos
     */
    public void markAllMessagesAsRead(String conversationId, String userId) {
        DatabaseReference messagesRef = conversationsRef
            .child(conversationId)
            .child(PATH_MESSAGES);
        
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message message = snapshotToMessage(child);
                    if (message != null && 
                        !message.getSenderId().equals(userId) && 
                        !message.isRead()) {
                        
                        String messageKey = child.getKey();
                        updates.put(messageKey + "/status", "READ");
                        updates.put(messageKey + "/isRead", true);
                        updates.put(messageKey + "/readAt", ServerValue.TIMESTAMP);
                    }
                }
                
                if (!updates.isEmpty()) {
                    messagesRef.updateChildren(updates);
                    // Actualizar contador de no leídos
                    updateUnreadCount(conversationId, userId, 0);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error marking messages as read", error.toException());
            }
        });
    }
    
    /**
     * Detiene el listener activo
     */
    public void stopListening() {
        if (messagesListener != null && currentConversationId != null) {
            DatabaseReference messagesRef = conversationsRef
                .child(currentConversationId)
                .child(PATH_MESSAGES);
            messagesRef.removeEventListener(messagesListener);
            messagesListener = null;
            currentConversationId = null;
        }
    }
    
    // ==================== MÉTODOS PRIVADOS ====================
    
    private String generateConversationId(String clientId, String companyId) {
        // Ordenar IDs para garantizar unicidad
        if (clientId.compareTo(companyId) < 0) {
            return clientId + "_" + companyId;
        } else {
            return companyId + "_" + clientId;
        }
    }
    
    private void createConversationIndex(String clientId, String companyId, String conversationId) {
        DatabaseReference indexRef = database.getReference("conversation_index");
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("client_" + clientId + "/" + conversationId, true);
        updates.put("company_" + companyId + "/" + conversationId, true);
        
        indexRef.updateChildren(updates);
    }
    
    private void updateConversationAfterMessage(String conversationId, Message message) {
        DatabaseReference conversationRef = conversationsRef.child(conversationId);
        
        Map<String, Object> updates = new HashMap<>();
        // Si el mensaje tiene adjunto, mostrar texto especial
        String lastMessageText = message.getMessageText();
        if (message.hasAttachment()) {
            if (message.getAttachmentType() != null) {
                if (message.getAttachmentType().equals("IMAGE")) {
                    lastMessageText = "Imagen";
                } else if (message.getAttachmentType().equals("PDF")) {
                    lastMessageText = "Archivo";
                }
            } else {
                lastMessageText = "Archivo";
            }
        }
        updates.put("lastMessage", lastMessageText != null ? lastMessageText : "");
        updates.put("lastMessageTimestamp", ServerValue.TIMESTAMP);
        updates.put("lastMessageSenderId", message.getSenderId());
        updates.put("lastMessageHasAttachment", message.hasAttachment());
        if (message.hasAttachment() && message.getAttachmentType() != null) {
            updates.put("lastMessageAttachmentType", message.getAttachmentType());
        }
        updates.put("updatedAt", ServerValue.TIMESTAMP);
        
        // Incrementar contador de no leídos del receptor
        conversationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String clientId = snapshot.child("clientId").getValue(String.class);
                String receiverId = message.getReceiverId();
                String unreadField = receiverId.equals(clientId) 
                    ? "unreadCountClient" 
                    : "unreadCountAdmin";
                
                Long currentValue = snapshot.child(unreadField).getValue(Long.class);
                if (currentValue != null) {
                    updates.put(unreadField, currentValue + 1);
                } else {
                    updates.put(unreadField, 1);
                }
                
                conversationRef.updateChildren(updates);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error updating unread count", error.toException());
                conversationRef.updateChildren(updates);
            }
        });
    }
    
    private void updateUnreadCount(String conversationId, String userId, int count) {
        DatabaseReference conversationRef = conversationsRef.child(conversationId);
        
        conversationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String clientId = snapshot.child("clientId").getValue(String.class);
                String unreadField = userId.equals(clientId) ? "unreadCountClient" : "unreadCountAdmin";
                
                conversationRef.child(unreadField).setValue(count);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error updating unread count", error.toException());
            }
        });
    }
    
    private Message snapshotToMessage(DataSnapshot snapshot) {
        try {
            Message message = new Message();
            message.setMessageId(snapshot.child("messageId").getValue(String.class));
            message.setSenderId(snapshot.child("senderId").getValue(String.class));
            message.setSenderName(snapshot.child("senderName").getValue(String.class));
            message.setSenderType(snapshot.child("senderType").getValue(String.class));
            message.setReceiverId(snapshot.child("receiverId").getValue(String.class));
            message.setReceiverName(snapshot.child("receiverName").getValue(String.class));
            message.setMessageText(snapshot.child("messageText").getValue(String.class));
            message.setConversationId(currentConversationId);
            
            // Timestamp
            Object timestampObj = snapshot.child("timestamp").getValue();
            if (timestampObj instanceof Long) {
                Long timestampMillis = (Long) timestampObj;
                // Convertir milisegundos a Timestamp de Firebase
                // Realtime Database almacena en milisegundos, Timestamp necesita segundos y nanosegundos
                long seconds = timestampMillis / 1000;
                int nanoseconds = (int) ((timestampMillis % 1000) * 1000000);
                message.setTimestamp(new com.google.firebase.Timestamp(seconds, nanoseconds));
            } else if (timestampObj instanceof Map) {
                // Si viene como Map (ServerValue.TIMESTAMP), usar el valor actual
                @SuppressWarnings("unchecked")
                Map<String, Object> timestampMap = (Map<String, Object>) timestampObj;
                if (timestampMap.containsKey(".sv")) {
                    // Es un ServerValue, usar timestamp actual
                    message.setTimestamp(com.google.firebase.Timestamp.now());
                }
            }
            
            // Estado
            String status = snapshot.child("status").getValue(String.class);
            Boolean isRead = snapshot.child("isRead").getValue(Boolean.class);
            message.setIsRead(isRead != null && isRead);
            
            // Archivos adjuntos
            Boolean hasAttachment = snapshot.child("hasAttachment").getValue(Boolean.class);
            if (hasAttachment != null && hasAttachment) {
                message.setHasAttachment(true);
                message.setAttachmentUrl(snapshot.child("attachmentUrl").getValue(String.class));
                message.setAttachmentType(snapshot.child("attachmentType").getValue(String.class));
                message.setAttachmentName(snapshot.child("attachmentName").getValue(String.class));
                Long attachmentSize = snapshot.child("attachmentSize").getValue(Long.class);
                if (attachmentSize != null) {
                    message.setAttachmentSize(attachmentSize);
                }
            } else {
                message.setHasAttachment(false);
            }
            
            return message;
        } catch (Exception e) {
            Log.e(TAG, "Error converting snapshot to message", e);
            return null;
        }
    }
    
    // ==================== INTERFACES ====================
    
    public interface ConversationCallback {
        void onConversationCreated(String conversationId);
        void onConversationFound(String conversationId);
        void onError(Exception e);
    }
    
    public interface MessagesLoadCallback {
        void onMessagesLoaded(List<Message> messages);
        void onError(Exception e);
    }
}

