package com.example.droidtour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.utils.ChatManagerRealtime;
import com.example.droidtour.utils.PresenceManager;
import com.example.droidtour.models.Message;
import com.example.droidtour.firebase.FirestoreManager;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminChatDetailActivity extends AppCompatActivity {
    
    private TextView tvClientName, tvClientStatus;
    private ImageView ivClientAvatar, ivCallClient, ivSendMessage, ivAttach;
    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private View rootLayout;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private ChatManagerRealtime chatManager;
    private PresenceManager presenceManager;
    private FirestoreManager firestoreManager;
    
    private String clientId;
    private String clientName;
    private String companyId;
    private String conversationId;
    private String currentUserId;
    private String currentUserName;
    private AdminChatMessagesAdapter messagesAdapter;
    private List<AdminChatMessage> messagesList;

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
        
        setContentView(R.layout.activity_admin_chat_detail);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        getIntentData();
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupInputBehavior();
        setupRecyclerView();
        
        // Inicializar managers
        chatManager = new ChatManagerRealtime();
        presenceManager = new PresenceManager();
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = prefsManager.getUserId();
        currentUserName = prefsManager.getUserName();
        
        loadChatData();
        loadClientAvatar();
        initializeConversation();
    }
    
    private void getIntentData() {
        clientId = getIntent().getStringExtra("CLIENT_ID");
        clientName = getIntent().getStringExtra("CLIENT_NAME");
        companyId = getIntent().getStringExtra("COMPANY_ID");
        
        if (clientName == null) {
            clientName = "Cliente";
        }
        if (companyId == null) {
            // Intentar obtener companyId del usuario actual
            // Esto debería mejorarse obteniéndolo de Firestore
            companyId = "unknown_company";
        }
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
    }
    
    private void initializeViews() {
        tvClientName = findViewById(R.id.tv_client_name);
        tvClientStatus = findViewById(R.id.tv_client_status);
        
        ivClientAvatar = findViewById(R.id.iv_client_avatar);
        ivCallClient = findViewById(R.id.iv_call_client);
        ivSendMessage = findViewById(R.id.iv_send_message);
        ivAttach = findViewById(R.id.iv_attach);
        
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        rootLayout = findViewById(R.id.root_layout);
    }
    
    private void setupClickListeners() {
        if (ivCallClient != null) {
            ivCallClient.setOnClickListener(v -> {
                Toast.makeText(this, "Llamar a " + clientName, Toast.LENGTH_SHORT).show();
                // TODO: Implementar llamada telefónica
            });
        }

        if (ivSendMessage != null) {
            ivSendMessage.setOnClickListener(v -> {
                sendMessage();
            });
        }

        if (ivAttach != null) {
            ivAttach.setOnClickListener(v -> {
                Toast.makeText(this, "Adjuntar archivo", Toast.LENGTH_SHORT).show();
                // No manipulamos el TextInputLayout: el hint está dentro del EditText
                // Si quieres restaurar el texto del hint manualmente, podemos hacerlo con etMessage.setHint(...)
                // TODO: Implementar adjuntar archivos
            });
        }
    }
    
    // Solo manejamos clicks en el root para quitar foco y ocultar el teclado
    private void setupInputBehavior() {
        if (rootLayout == null) return;
        rootLayout.setOnClickListener(v -> {
            if (etMessage != null) {
                etMessage.clearFocus();
                // Ocultar teclado
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
            }
        });
    }
    
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        
        messagesList = new ArrayList<>();
        messagesAdapter = new AdminChatMessagesAdapter(messagesList);
        messagesAdapter.setClientId(clientId);
        rvMessages.setAdapter(messagesAdapter);
    }
    
    private void loadChatData() {
        tvClientName.setText(clientName);
        tvClientStatus.setText("En línea");
    }
    
    private void loadClientAvatar() {
        if (clientId == null || clientId.isEmpty()) {
            return;
        }
        
        firestoreManager.getUserById(clientId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof com.example.droidtour.models.User && ivClientAvatar != null) {
                    com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                    String photoUrl = null;
                    
                    // Intentar obtener desde personalData primero
                    if (user.getPersonalData() != null) {
                        photoUrl = user.getPersonalData().getProfileImageUrl();
                    }
                    // Fallback al método legacy
                    if ((photoUrl == null || photoUrl.isEmpty()) && user.getPhotoUrl() != null) {
                        photoUrl = user.getPhotoUrl();
                    }
                    
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(AdminChatDetailActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar_24)
                                .error(R.drawable.ic_avatar_24)
                                .circleCrop()
                                .into(ivClientAvatar);
                    } else {
                        ivClientAvatar.setImageResource(R.drawable.ic_avatar_24);
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Usar placeholder por defecto si falla
                if (ivClientAvatar != null) {
                    ivClientAvatar.setImageResource(R.drawable.ic_avatar_24);
                }
            }
        });
    }
    
    private void initializeConversation() {
        if (clientId == null || companyId == null) {
            Toast.makeText(this, "Error: falta información del cliente o empresa", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Crear o obtener conversación
        chatManager.createOrGetConversation(
            clientId,
            companyId,
            clientName,
            currentUserName != null ? currentUserName : "Administrador",
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
                        Toast.makeText(AdminChatDetailActivity.this, "Error al inicializar chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    AdminChatMessage chatMsg = convertToAdminChatMessage(message);
                    messagesList.add(chatMsg);
                    messagesAdapter.notifyItemInserted(messagesList.size() - 1);
                    rvMessages.scrollToPosition(messagesList.size() - 1);
                    
                    // Marcar como entregado y leído si es del cliente
                    if (message.getSenderId().equals(clientId)) {
                        chatManager.markMessageAsDelivered(conversationId, message.getMessageId());
                        chatManager.markMessageAsRead(conversationId, message.getMessageId());
                    }
                });
            }
            
            @Override
            public void onMessageUpdated(Message message) {
                runOnUiThread(() -> {
                    // Actualizar mensaje existente
                    for (int i = 0; i < messagesList.size(); i++) {
                        AdminChatMessage msg = messagesList.get(i);
                        if (msg.message.equals(message.getMessageText())) {
                            // Actualizar si es necesario
                            break;
                        }
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(AdminChatDetailActivity.this, "Error al recibir mensajes: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
        
        // Escuchar estado en línea del cliente
        if (clientId != null) {
            presenceManager.listenToPresence(clientId, new PresenceManager.PresenceCallback() {
                @Override
                public void onPresenceChanged(String userId, boolean isOnline, long lastSeen) {
                    runOnUiThread(() -> {
                        if (isOnline) {
                            tvClientStatus.setText("En línea");
                        } else {
                            if (lastSeen > 0) {
                                long timeDiff = System.currentTimeMillis() / 1000 - lastSeen;
                                if (timeDiff < 300) {
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
    }
    
    private AdminChatMessage convertToAdminChatMessage(Message message) {
        boolean isFromClient = message.getSenderId().equals(clientId);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeStr = "";
        if (message.getTimestamp() != null) {
            timeStr = sdf.format(message.getTimestamp().toDate());
        }
        return new AdminChatMessage(message.getMessageText(), isFromClient, timeStr);
    }
    
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        
        if (messageText.isEmpty() || conversationId == null) {
            Toast.makeText(this, "Escriba un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Construir Message
        Message msg = new Message();
        msg.setSenderId(currentUserId);
        msg.setSenderName(currentUserName != null ? currentUserName : "Administrador");
        msg.setReceiverId(clientId);
        msg.setReceiverName(clientName);
        msg.setSenderType(prefsManager.getUserType());
        msg.setMessageText(messageText);
        msg.setConversationId(conversationId);
        msg.setCompanyId(companyId);
        
        // Enviar mediante ChatManagerRealtime
        chatManager.sendMessage(conversationId, msg, new ChatManagerRealtime.SendCallback() {
            @Override
            public void onSuccess(String messageId) {
                runOnUiThread(() -> {
                    etMessage.setText("");
                    // El mensaje se agregará automáticamente cuando llegue por el listener
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(AdminChatDetailActivity.this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
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
        if (presenceManager != null && currentUserId != null) {
            presenceManager.setOffline(currentUserId);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
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

// Clase para representar un mensaje de chat del administrador
class AdminChatMessage {
    public String message;
    public boolean isFromClient; // true = cliente, false = administrador
    public String timestamp;

    public AdminChatMessage(String message, boolean isFromClient, String timestamp) {
        this.message = message;
        this.isFromClient = isFromClient;
        this.timestamp = timestamp;
    }
}

// Adaptador para los mensajes del chat del administrador
class AdminChatMessagesAdapter extends RecyclerView.Adapter<AdminChatMessagesAdapter.MessageViewHolder> {
    private List<AdminChatMessage> messages;
    private String clientId;
    private FirestoreManager firestoreManager;

    public AdminChatMessagesAdapter(List<AdminChatMessage> messages) {
        this.messages = messages;
        this.firestoreManager = FirestoreManager.getInstance();
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        AdminChatMessage message = messages.get(position);

        if (message.isFromClient) {
            // Mensaje del cliente (mostrar como mensaje de empresa en el layout)
            holder.layoutIncoming.setVisibility(View.VISIBLE);
            holder.layoutOutgoing.setVisibility(View.GONE);

            holder.tvIncomingMessage.setText(message.message);
            if (holder.tvIncomingTime != null) {
                holder.tvIncomingTime.setText(message.timestamp);
            }
            
            // Cargar avatar del cliente en el mensaje
            if (holder.ivCompanyAvatar != null && clientId != null && !clientId.isEmpty()) {
                loadClientAvatarInMessage(holder, clientId);
            }
        } else {
            // Mensaje del administrador (mostrar como mensaje de usuario en el layout)
            holder.layoutIncoming.setVisibility(View.GONE);
            holder.layoutOutgoing.setVisibility(View.VISIBLE);

            holder.tvOutgoingMessage.setText(message.message);
            // Establecer el tiempo del mensaje
            if (holder.tvOutgoingTime != null) {
                holder.tvOutgoingTime.setText(message.timestamp);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutIncoming, layoutOutgoing, layoutSystem;
        TextView tvIncomingMessage, tvIncomingTime;
        TextView tvOutgoingMessage, tvOutgoingTime;
        TextView tvSystemMessage;
        ImageView ivCompanyAvatar;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutIncoming = itemView.findViewById(R.id.layout_company_message);
            layoutOutgoing = itemView.findViewById(R.id.layout_user_message);
            // No hay layout_system_message en el layout actual
            layoutSystem = null;

            tvIncomingMessage = itemView.findViewById(R.id.tv_company_message);
            // ID correcto para el tiempo de mensaje de la empresa
            tvIncomingTime = itemView.findViewById(R.id.tv_company_message_time);
            ivCompanyAvatar = itemView.findViewById(R.id.iv_company_avatar);

            tvOutgoingMessage = itemView.findViewById(R.id.tv_user_message);
            // Campo de tiempo para mensaje de usuario (saliente)
            tvOutgoingTime = itemView.findViewById(R.id.tv_user_message_time);

            // No hay tv_system_message en el layout actual
            tvSystemMessage = null;
        }
    }
    
    private void loadClientAvatarInMessage(MessageViewHolder holder, String clientId) {
        if (firestoreManager == null || holder.ivCompanyAvatar == null) {
            return;
        }
        
        firestoreManager.getUserById(clientId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof com.example.droidtour.models.User && holder.ivCompanyAvatar != null) {
                    com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                    String photoUrl = null;
                    
                    // Intentar obtener desde personalData primero
                    if (user.getPersonalData() != null) {
                        photoUrl = user.getPersonalData().getProfileImageUrl();
                    }
                    // Fallback al método legacy
                    if ((photoUrl == null || photoUrl.isEmpty()) && user.getPhotoUrl() != null) {
                        photoUrl = user.getPhotoUrl();
                    }
                    
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(holder.ivCompanyAvatar.getContext())
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar_24)
                                .error(R.drawable.ic_avatar_24)
                                .circleCrop()
                                .into(holder.ivCompanyAvatar);
                    } else {
                        holder.ivCompanyAvatar.setImageResource(R.drawable.ic_avatar_24);
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Usar placeholder por defecto si falla
                if (holder.ivCompanyAvatar != null) {
                    holder.ivCompanyAvatar.setImageResource(R.drawable.ic_avatar_24);
                }
            }
        });
    }
}
