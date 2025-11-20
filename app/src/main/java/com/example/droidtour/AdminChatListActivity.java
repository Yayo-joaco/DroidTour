package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

public class AdminChatListActivity extends AppCompatActivity {
    
    private RecyclerView rvClientChats;
    private com.example.droidtour.utils.PreferencesManager prefsManager;

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
        if (userType == null || !userType.equals("ADMIN")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_admin_chat_list);
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
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
        rvClientChats.setAdapter(new AdminClientChatsAdapter(client -> {
            // Abrir chat con cliente específico
            Intent intent = new Intent(this, AdminChatDetailActivity.class);
            intent.putExtra("CLIENT_NAME", client.name);
            startActivity(intent);
        }));
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
    public String name;
    public String lastMessage;
    public String timestamp;
    public boolean hasUnreadMessages;
    public int unreadCount;

    public ClientChat(String name, String lastMessage, String timestamp, boolean hasUnreadMessages, int unreadCount) {
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
    private final ClientChat[] clientChats;

    AdminClientChatsAdapter(OnClientChatClick listener) {
        this.onClientChatClick = listener;
        // Datos mock de chats con clientes
        this.clientChats = new ClientChat[] {
            new ClientChat("María González", "¿A qué hora es el punto de encuentro?", "2:30 PM", true, 2),
            new ClientChat("Carlos López", "Muchas gracias por el tour, fue excelente", "1:15 PM", false, 0),
            new ClientChat("Ana Martínez", "¿Incluye almuerzo el tour?", "11:45 AM", true, 1),
            new ClientChat("Pedro Rojas", "¿Puedo cancelar mi reserva?", "Ayer", false, 0)
        };
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
        ClientChat client = clientChats[position];
        
        holder.tvClientName.setText(client.name);
        holder.tvLastMessage.setText(client.lastMessage);
        holder.tvTimestamp.setText(client.timestamp);
        
        if (client.hasUnreadMessages) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(client.unreadCount));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> onClientChatClick.onClick(client));
    }

    @Override
    public int getItemCount() {
        return clientChats.length;
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
