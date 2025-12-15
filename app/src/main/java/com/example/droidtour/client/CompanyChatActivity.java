package com.example.droidtour.client;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ImageView;

import com.example.droidtour.utils.ChatManagerRealtime;
import com.example.droidtour.utils.PresenceManager;
import com.example.droidtour.models.Message;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.firebase.FirestoreManager;

import java.util.ArrayList;
import com.google.firebase.Timestamp;
import java.util.List;

public class CompanyChatActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private TextInputEditText etMessage;
    private ImageView ivSendMessage;
    private TextView tvClientName;
    private TextView tvClientStatus;
    private CompanyChatAdapter chatAdapter;
    private PreferencesManager prefsManager;
    private ChatManagerRealtime chatManager;
    private PresenceManager presenceManager;
    private FirestoreManager firestoreManager;
    private String conversationId;
    private String companyId;
    private String companyName;
    private String currentUserId;
    private String currentUserName;

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

        // Validar que el usuario sea CLIENT o ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("CLIENT") && !userType.equals("ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }

        setContentView(R.layout.activity_company_chat);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadCompanyInfo();

        // Inicializar managers
        chatManager = new ChatManagerRealtime();
        presenceManager = new PresenceManager();
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = prefsManager.getUserId();
        currentUserName = prefsManager.getUserName();

        // Obtener companyId del intent
        companyId = getIntent().getStringExtra("company_id");
        
        // Si no hay companyId, intentar obtenerlo del nombre de la empresa
        if (companyId == null || companyId.isEmpty()) {
            // Por ahora usar el nombre como fallback, pero idealmente debería venir del intent
            initializeConversation();
        } else {
            initializeConversation();
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chat con Empresa");
        }
    }

    private void initializeViews() {
        rvChatMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        ivSendMessage = findViewById(R.id.iv_send_message);
        tvClientName = findViewById(R.id.tv_client_name);
        tvClientStatus = findViewById(R.id.tv_client_status);
        // Mostrar estado por defecto
        if (tvClientStatus != null) tvClientStatus.setText("En línea");
    }

    private void setupRecyclerView() {
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new CompanyChatAdapter(currentUserId);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        ivSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void loadCompanyInfo() {
        // Get company info from intent
        companyName = getIntent().getStringExtra("company_name");
        if (companyName != null) {
            tvClientName.setText(companyName);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Chat - " + companyName);
        }
    }
    
    private void initializeConversation() {
        // Si no tenemos companyId, usar un valor por defecto (esto debería mejorarse)
        if (companyId == null || companyId.isEmpty()) {
            companyId = "unknown_company";
        }
        
        // Crear o obtener conversación
        chatManager.createOrGetConversation(
            currentUserId,
            companyId,
            currentUserName != null ? currentUserName : "Cliente",
            companyName != null ? companyName : "Empresa",
            new ChatManagerRealtime.ConversationCallback() {
                @Override
                public void onConversationCreated(String convId) {
                    conversationId = convId;
                    setupChat();
                }
                
                @Override
                public void onConversationFound(String convId) {
                    conversationId = convId;
                    setupChat();
                }
                
                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(CompanyChatActivity.this, "Error al inicializar chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        );
    }
    
    private void setupChat() {
        // Marcar todos los mensajes como leídos al abrir
        chatManager.markAllMessagesAsRead(conversationId, currentUserId);
        
        // Escuchar mensajes en tiempo real
        chatManager.listenForMessages(conversationId, new ChatManagerRealtime.MessagesListener() {
            @Override
            public void onNewMessage(Message message) {
                runOnUiThread(() -> {
                    chatAdapter.addMessage(message);
                    rvChatMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                    
                    // Marcar como entregado si el receptor está en línea
                    if (!message.getSenderId().equals(currentUserId)) {
                        chatManager.markMessageAsDelivered(conversationId, message.getMessageId());
                        // Marcar como leído si estamos viendo la conversación
                        chatManager.markMessageAsRead(conversationId, message.getMessageId());
                    }
                });
            }
            
            @Override
            public void onMessageUpdated(Message message) {
                runOnUiThread(() -> {
                    // Actualizar mensaje existente en el adaptador
                    chatAdapter.updateMessage(message);
                });
            }
            
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(CompanyChatActivity.this, "Error al recibir mensajes: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
        
        // Escuchar estado en línea de la empresa
        presenceManager.listenToPresence(companyId, new PresenceManager.PresenceCallback() {
            @Override
            public void onPresenceChanged(String userId, boolean isOnline, long lastSeen) {
                runOnUiThread(() -> {
                    if (isOnline) {
                        tvClientStatus.setText("En línea");
                    } else {
                        if (lastSeen > 0) {
                            long timeDiff = System.currentTimeMillis() / 1000 - lastSeen;
                            if (timeDiff < 300) { // Menos de 5 minutos
                                tvClientStatus.setText("Recientemente");
                            } else {
                                tvClientStatus.setText("Desconectado");
                            }
                        } else {
                            tvClientStatus.setText("Desconectado");
                        }
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                // Ignorar errores de presencia
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (messageText.isEmpty() || conversationId == null) return;

        // Construir Message usando el modelo existente
        Message msg = new Message();
        msg.setSenderId(currentUserId);
        msg.setSenderName(currentUserName != null ? currentUserName : "Cliente");
        msg.setReceiverId(companyId);
        msg.setReceiverName(companyName != null ? companyName : "Empresa");
        msg.setSenderType(prefsManager.getUserType());
        msg.setMessageText(messageText);
        msg.setTimestamp(Timestamp.now());
        msg.setIsRead(false);
        msg.setConversationId(conversationId);
        msg.setCompanyId(companyId);

        // Enviar mediante ChatManagerRealtime
        chatManager.sendMessage(conversationId, msg, new ChatManagerRealtime.SendCallback() {
            @Override
            public void onSuccess(String messageId) {
                runOnUiThread(() -> {
                    etMessage.setText("");
                    msg.setMessageId(messageId);
                    // El mensaje se agregará automáticamente cuando llegue por el listener
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(CompanyChatActivity.this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Establecer usuario como en línea
        if (currentUserId != null) {
            presenceManager.setOnline(currentUserId);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // El heartbeat seguirá actualizando lastSeen
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatManager != null) {
            chatManager.stopListening();
        }
        if (presenceManager != null) {
            presenceManager.setOffline(currentUserId);
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

// Adaptador para los mensajes del chat de la empresa (dinámico)
class CompanyChatAdapter extends RecyclerView.Adapter<CompanyChatAdapter.ViewHolder> {

    private final List<Message> messages = new ArrayList<>();
    private final String currentUserId;

    CompanyChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Message message = messages.get(position);

        View layoutUserMessage = holder.itemView.findViewById(R.id.layout_user_message);
        View layoutCompanyMessage = holder.itemView.findViewById(R.id.layout_company_message);
        TextView tvUserMessage = holder.itemView.findViewById(R.id.tv_user_message);
        TextView tvCompanyMessage = holder.itemView.findViewById(R.id.tv_company_message);
        TextView tvCompanyTime = holder.itemView.findViewById(R.id.tv_company_message_time);
        TextView tvUserTime = holder.itemView.findViewById(R.id.tv_user_message_time);

        boolean isCurrentUser = currentUserId != null && currentUserId.equals(message.getSenderId());

        if (isCurrentUser) {
            if (layoutUserMessage != null) layoutUserMessage.setVisibility(View.VISIBLE);
            if (layoutCompanyMessage != null) layoutCompanyMessage.setVisibility(View.GONE);
            if (tvUserMessage != null) tvUserMessage.setText(message.getMessageText());
            if (tvUserTime != null && message.getTimestamp() != null) tvUserTime.setText(android.text.format.DateFormat.format("hh:mm a", message.getTimestamp().toDate()));
        } else {
            if (layoutUserMessage != null) layoutUserMessage.setVisibility(View.GONE);
            if (layoutCompanyMessage != null) layoutCompanyMessage.setVisibility(View.VISIBLE);
            if (tvCompanyMessage != null) tvCompanyMessage.setText(message.getMessageText());
            if (tvCompanyTime != null && message.getTimestamp() != null) tvCompanyTime.setText(android.text.format.DateFormat.format("hh:mm a", message.getTimestamp().toDate()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message m) {
        messages.add(m);
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessages(List<Message> ms) {
        int start = messages.size();
        messages.addAll(ms);
        notifyItemRangeInserted(start, ms.size());
    }
    
    public void updateMessage(Message updatedMessage) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getMessageId() != null && 
                messages.get(i).getMessageId().equals(updatedMessage.getMessageId())) {
                messages.set(i, updatedMessage);
                notifyItemChanged(i);
                return;
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View v) {
            super(v);
        }
    }
}