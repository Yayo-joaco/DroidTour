package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.utils.ConversationHelper;
import com.example.droidtour.utils.PresenceManager;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminChatListActivity extends AppCompatActivity {
    
    private static final String TAG = "AdminChatListActivity";
    private RecyclerView rvClientChats;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private ConversationHelper conversationHelper;
    private PresenceManager presenceManager;
    private String currentCompanyId;
    private String currentUserId;
    private List<ClientChat> clientChatList = new ArrayList<>();
    private AdminClientChatsAdapter adapter;

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
        
        setContentView(R.layout.activity_admin_chat_list);
        
        firestoreManager = FirestoreManager.getInstance();
        conversationHelper = new ConversationHelper();
        presenceManager = new PresenceManager();
        currentUserId = prefsManager.getUserId();
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
        loadCompanyAndChats();
    }
    
    private void loadCompanyAndChats() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadClientChats();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar usuario", e);
            }
        });
    }
    
    private void loadClientChats() {
        // Cargar conversaciones de la empresa desde Realtime Database
        conversationHelper.getConversationsForCompany(currentCompanyId, new ConversationHelper.ConversationsCallback() {
            @Override
            public void onConversationsLoaded(List<ConversationHelper.ConversationData> conversations) {
                clientChatList.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                
                for (ConversationHelper.ConversationData conv : conversations) {
                    String clientId = conv.getClientId();
                    String clientName = conv.getClientName() != null ? conv.getClientName() : "Cliente";
                    
                    // Formatear último mensaje con "Tú: " si es del usuario actual
                    String lastMessage = formatLastMessage(conv, currentUserId);
                    
                    // Formatear timestamp
                    long timestamp = conv.getLastMessageTimestamp();
                    String timeStr = "";
                    if (timestamp > 0) {
                        timeStr = sdf.format(new java.util.Date(timestamp));
                    }
                    
                    // Obtener contador de no leídos (desde perspectiva del admin)
                    int unreadCount = conv.getUnreadCountAdmin();
                    
                    ClientChat chat = new ClientChat(
                        clientId,
                        clientName,
                        lastMessage,
                        timeStr,
                        unreadCount > 0,
                        unreadCount
                    );
                    clientChatList.add(chat);
                }
                
                adapter.updateData(clientChatList);
                Log.d(TAG, "Chats cargados: " + clientChatList.size());
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error al cargar conversaciones", e);
                Toast.makeText(AdminChatListActivity.this, "Error al cargar chats", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Chat con Clientes");
    }
    
    private void initializeViews() {
        rvClientChats = findViewById(R.id.rv_client_chats);
    }
    
    private void setupRecyclerView() {
        rvClientChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminClientChatsAdapter(clientChatList, client -> {
            // Abrir chat con cliente específico
            Intent intent = new Intent(this, AdminChatDetailActivity.class);
            intent.putExtra("CLIENT_ID", client.clientId);
            intent.putExtra("CLIENT_NAME", client.name);
            intent.putExtra("COMPANY_ID", currentCompanyId);
            startActivity(intent);
        });
        rvClientChats.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Establecer usuario como en línea
        if (currentUserId != null) {
            presenceManager.setOnline(currentUserId);
        }
        
        if (currentCompanyId != null) {
            // Escuchar cambios en conversaciones en tiempo real
            conversationHelper.listenToCompanyConversations(currentCompanyId, new ConversationHelper.ConversationsCallback() {
                @Override
                public void onConversationsLoaded(List<ConversationHelper.ConversationData> conversations) {
                    clientChatList.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    
                    for (ConversationHelper.ConversationData conv : conversations) {
                        String clientId = conv.getClientId();
                        String clientName = conv.getClientName() != null ? conv.getClientName() : "Cliente";
                        
                        // Formatear último mensaje con "Tú: " si es del usuario actual
                        String lastMessage = formatLastMessage(conv, currentUserId);
                        
                        long timestamp = conv.getLastMessageTimestamp();
                        String timeStr = "";
                        if (timestamp > 0) {
                            timeStr = sdf.format(new java.util.Date(timestamp));
                        }
                        
                        int unreadCount = conv.getUnreadCountAdmin();
                        
                        ClientChat chat = new ClientChat(
                            clientId,
                            clientName,
                            lastMessage,
                            timeStr,
                            unreadCount > 0,
                            unreadCount
                        );
                        clientChatList.add(chat);
                    }
                    
                    adapter.updateData(clientChatList);
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error escuchando conversaciones", e);
                }
            });
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
        Intent intent = new Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    
    /**
     * Formatea el último mensaje agregando "Tú: " si es del usuario actual
     */
    private String formatLastMessage(ConversationHelper.ConversationData conv, String currentUserId) {
        String lastMessage = conv.getLastMessage() != null ? conv.getLastMessage() : "";
        String lastSenderId = conv.getLastMessageSenderId();
        
        // Si el último mensaje tiene adjunto, mostrar "Imagen" o "Archivo"
        if (conv.getLastMessageHasAttachment()) {
            String attachmentType = conv.getLastMessageAttachmentType();
            String attachmentText = (attachmentType != null && attachmentType.equals("IMAGE")) ? "Imagen" : "Archivo";
            
            // Si el último mensaje es del usuario actual, agregar "Tú: "
            if (lastSenderId != null && lastSenderId.equals(currentUserId)) {
                return "Tú: " + attachmentText;
            } else {
                return attachmentText;
            }
        }
        
        // Si el último mensaje es del usuario actual, agregar "Tú: "
        if (lastSenderId != null && lastSenderId.equals(currentUserId)) {
            return "Tú: " + lastMessage;
        }
        
        return lastMessage;
    }
}

// Clase para representar un cliente con chat
class ClientChat {
    public String clientId;
    public String name;
    public String lastMessage;
    public String timestamp;
    public boolean hasUnreadMessages;
    public int unreadCount;

    public ClientChat(String clientId, String name, String lastMessage, String timestamp, boolean hasUnreadMessages, int unreadCount) {
        this.clientId = clientId;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.hasUnreadMessages = hasUnreadMessages;
        this.unreadCount = unreadCount;
    }
}

// Adaptador para chats de clientes desde perspectiva del administrador
class AdminClientChatsAdapter extends RecyclerView.Adapter<AdminClientChatsAdapter.ViewHolder> {
    interface OnClientChatClick { void onClick(ClientChat client); }
    
    private final OnClientChatClick onClientChatClick;
    private List<ClientChat> clientChats;

    AdminClientChatsAdapter(List<ClientChat> chats, OnClientChatClick listener) {
        this.clientChats = chats != null ? chats : new ArrayList<>();
        this.onClientChatClick = listener;
    }
    
    public void updateData(List<ClientChat> newData) {
        this.clientChats = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_client_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClientChat client = clientChats.get(position);
        
        holder.tvClientName.setText(client.name);
        holder.tvLastMessage.setText(client.lastMessage);
        holder.tvTimestamp.setText(client.timestamp);
        
        if (client.hasUnreadMessages && holder.tvUnreadCount != null) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(client.unreadCount));
        } else if (holder.tvUnreadCount != null) {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }
        
        // Cargar avatar del cliente desde Firestore
        loadClientAvatar(holder, client.clientId);
        
        holder.itemView.setOnClickListener(v -> onClientChatClick.onClick(client));
    }
    
    private void loadClientAvatar(ViewHolder holder, String clientId) {
        if (clientId == null || clientId.isEmpty() || holder.ivClientAvatar == null) {
            // Usar placeholder por defecto
            if (holder.ivClientAvatar != null) {
                holder.ivClientAvatar.setImageResource(R.drawable.ic_avatar_24);
            }
            return;
        }
        
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getUserById(clientId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof com.example.droidtour.models.User && holder.ivClientAvatar != null) {
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
                        Glide.with(holder.ivClientAvatar.getContext())
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar_24)
                                .error(R.drawable.ic_avatar_24)
                                .circleCrop()
                                .into(holder.ivClientAvatar);
                    } else {
                        holder.ivClientAvatar.setImageResource(R.drawable.ic_avatar_24);
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Usar placeholder por defecto si falla
                if (holder.ivClientAvatar != null) {
                    holder.ivClientAvatar.setImageResource(R.drawable.ic_avatar_24);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return clientChats != null ? clientChats.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientName, tvLastMessage, tvTimestamp, tvUnreadCount;
        ImageView ivClientAvatar;

        ViewHolder(View view) {
            super(view);
            tvClientName = view.findViewById(R.id.tv_client_name);
            tvLastMessage = view.findViewById(R.id.tv_last_message);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvUnreadCount = view.findViewById(R.id.tv_unread_count);
            ivClientAvatar = view.findViewById(R.id.img_client_avatar);
        }
    }
}
