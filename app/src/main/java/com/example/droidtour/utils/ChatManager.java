package com.example.droidtour.utils;

import com.example.droidtour.models.Message;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * ChatManager: encapsula lógica de chat sobre Firestore.
 * Guarda mensajes en: conversations/{conversationId}/messages/{messageId}
 * Escucha nuevos mensajes ordenados por timestamp.
 */
public class ChatManager {

    private final FirebaseFirestore db;
    private ListenerRegistration currentListener;

    public ChatManager() {
        db = FirebaseFirestore.getInstance();
    }

    public interface SendCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface MessagesListener {
        void onNewMessages(List<Message> messages);
        void onError(Exception e);
    }

    /**
     * Envía un mensaje a una conversación (conversationId). Si conversationId es null o vacío, falla.
     */
    public void sendMessage(String conversationId, Message message, SendCallback callback) {
        if (conversationId == null || conversationId.isEmpty() || message == null) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("conversationId o message inválido"));
            return;
        }

        //CollectionReference messagesRef = db.collection(FirestoreManager.COLLECTION_CONVERSATIONS).document(conversationId).collection("messages");

        // Añadir el mensaje; Firestore asignará un id
        /*messagesRef.add(message)
                .addOnSuccessListener(docRef -> {
                    // opcional: establecer el messageId dentro del documento
                    String id = docRef.getId();
                    docRef.update("messageId", id);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });

         */
    }

    /**
     * Inicia el listener de nuevos mensajes para una conversación. Llama a listener.onNewMessages
     * con los mensajes añadidos desde la última snapshot.
     */
    public void listenForMessages(String conversationId, final MessagesListener listener) {
        if (conversationId == null || conversationId.isEmpty()) return;

        //CollectionReference messagesRef = db.collection(FirestoreManager.COLLECTION_CONVERSATIONS).document(conversationId).collection("messages");
        //Query q = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING);

        // Remover listener anterior si existe
        if (currentListener != null) {
            currentListener.remove();
            currentListener = null;
        }

        /*
        currentListener = q.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    if (listener != null) listener.onError(e);
                    return;
                }

                if (snapshots == null) return;

                List<Message> newMessages = new ArrayList<>();
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.ADDED) {
                        Message m = dc.getDocument().toObject(Message.class);
                        // Firestore rellenará campos por setters/getters si existen
                        newMessages.add(m);
                    }
                }

                if (!newMessages.isEmpty() && listener != null) {
                    listener.onNewMessages(newMessages);
                }
            }
        });

         */
    }

    /**
     * Detiene el listener activo (si existe). Llamar en onDestroy para evitar fugas.
     */
    public void stopListening() {
        if (currentListener != null) {
            currentListener.remove();
            currentListener = null;
        }
    }
}

