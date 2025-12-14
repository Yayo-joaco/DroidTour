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
import com.example.droidtour.models.Message;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminChatListActivity extends AppCompatActivity {
    
    private static final String TAG = "AdminChatListActivity";
    private RecyclerView rvClientChats;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private String currentCompanyId;
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
        // Cargar mensajes de la empresa
        firestoreManager.getMessagesByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Message> messages = (List<Message>) result;
                
                // Agrupar mensajes por cliente
                Map<String, ClientChat> clientMap = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                
                for (Message msg : messages) {
                    String clientId = msg.getSenderId();
                    String clientName = msg.getSenderName() != null ? msg.getSenderName() : "Cliente";
                    
                    ClientChat existing = clientMap.get(clientId);
                    if (existing == null) {
                        ClientChat chat = new ClientChat(
                            clientId,
                            clientName, 
                            msg.getContent(), 
                            msg.getCreatedAt() != null ? sdf.format(msg.getCreatedAt()) : "",
                            !msg.getIsRead(),
                            msg.getIsRead() ? 0 : 1
                        );
                        clientMap.put(clientId, chat);
                    } else {
                        // Actualizar con mensaje más reciente
                        if (!msg.getIsRead()) {
                            existing.unreadCount++;
                            existing.hasUnreadMessages = true;
                        }
                    }
                }
                
                clientChatList.clear();
                clientChatList.addAll(clientMap.values());
                adapter.updateData(clientChatList);
                
                Log.d(TAG, "Chats cargados: " + clientChatList.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar mensajes", e);
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
        if (currentCompanyId != null) {
            loadClientChats();
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
        
        holder.itemView.setOnClickListener(v -> onClientChatClick.onClick(client));
    }

    @Override
    public int getItemCount() {
        return clientChats != null ? clientChats.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientName, tvLastMessage, tvTimestamp, tvUnreadCount;

        ViewHolder(View view) {
            super(view);
            tvClientName = view.findViewById(R.id.tv_client_name);
            tvLastMessage = view.findViewById(R.id.tv_last_message);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvUnreadCount = view.findViewById(R.id.tv_unread_count);
        }
    }
}
