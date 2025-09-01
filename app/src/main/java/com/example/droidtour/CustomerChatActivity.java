package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

public class CustomerChatActivity extends AppCompatActivity {
    
    private RecyclerView rvChatList;
    private TextView tvActiveChats, tvPendingChats, tvAvgResponseTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_chat);
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
        loadChatData();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        rvChatList = findViewById(R.id.rv_chat_list);
        tvActiveChats = findViewById(R.id.tv_active_chats);
        tvPendingChats = findViewById(R.id.tv_pending_chats);
        tvAvgResponseTime = findViewById(R.id.tv_avg_response_time);
    }
    
    private void setupRecyclerView() {
        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        rvChatList.setAdapter(new ChatConversationsAdapter(conversation -> {
            // Abrir chat detallado
            Intent intent = new Intent(this, ChatDetailActivity.class);
            intent.putExtra("clientName", conversation.clientName);
            intent.putExtra("lastMessage", conversation.lastMessage);
            startActivity(intent);
        }));
    }
    
    private void loadChatData() {
        // TODO: Cargar datos reales desde base de datos
        // Por ahora mostrar datos de prueba
        tvActiveChats.setText("3");
        tvPendingChats.setText("1");
        tvAvgResponseTime.setText("2.5 min");
    }
    
    private void openChatDetail(String chatId, String clientName, String tourName) {
        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("CHAT_ID", chatId);
        intent.putExtra("CLIENT_NAME", clientName);
        intent.putExtra("TOUR_NAME", tourName);
        startActivity(intent);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

// Clase para representar una conversación de chat
class ChatConversation {
    public String clientName;
    public String lastMessage;
    public String timestamp;
    public boolean isUnread;
    public String tourName;

    public ChatConversation(String clientName, String lastMessage, String timestamp, boolean isUnread, String tourName) {
        this.clientName = clientName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.isUnread = isUnread;
        this.tourName = tourName;
    }
}

// Adaptador para la lista de conversaciones
class ChatConversationsAdapter extends RecyclerView.Adapter<ChatConversationsAdapter.ViewHolder> {
    interface OnConversationClick { void onClick(ChatConversation conversation); }
    
    private final OnConversationClick onConversationClick;
    private final ChatConversation[] conversations;

    ChatConversationsAdapter(OnConversationClick listener) {
        this.onConversationClick = listener;
        // Datos mock de conversaciones
        this.conversations = new ChatConversation[] {
            new ChatConversation("María García", "¿A qué hora es el punto de encuentro?", "10:30 AM", true, "City Tour Centro"),
            new ChatConversation("Carlos López", "Muchas gracias por el tour, fue excelente", "9:45 AM", false, "Valle Sagrado"),
            new ChatConversation("Ana Martínez", "¿Incluye almuerzo el tour?", "9:15 AM", true, "Machu Picchu Express"),
            new ChatConversation("Pedro Rojas", "¿Puedo cancelar mi reserva?", "8:30 AM", false, "Tour Gastronómico"),
            new ChatConversation("Laura Silva", "¿Qué debo llevar para el tour?", "Ayer", true, "City Tour Centro")
        };
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatConversation conversation = conversations[position];
        
        holder.tvClientName.setText(conversation.clientName);
        holder.tvLastMessage.setText(conversation.lastMessage);
        holder.tvTimestamp.setText(conversation.timestamp);
        holder.tvTourName.setText(conversation.tourName);
        
        // Mostrar indicador de no leído
        holder.viewUnreadIndicator.setVisibility(conversation.isUnread ? View.VISIBLE : View.GONE);
        
        holder.itemView.setOnClickListener(v -> onConversationClick.onClick(conversation));
    }

    @Override
    public int getItemCount() {
        return conversations.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientName, tvLastMessage, tvTimestamp, tvTourName;
        View viewUnreadIndicator;

        ViewHolder(View view) {
            super(view);
            tvClientName = view.findViewById(R.id.tv_client_name);
            tvLastMessage = view.findViewById(R.id.tv_last_message);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvTourName = view.findViewById(R.id.tv_tour_name);
            viewUnreadIndicator = view.findViewById(R.id.view_unread_indicator);
        }
    }
}
