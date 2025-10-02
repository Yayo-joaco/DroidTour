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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
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
    
    private String clientName;
    private AdminChatMessagesAdapter messagesAdapter;
    private List<AdminChatMessage> messagesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat_detail);
        
        getIntentData();
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadChatData();
    }
    
    private void getIntentData() {
        clientName = getIntent().getStringExtra("CLIENT_NAME");
        if (clientName == null) {
            clientName = "Cliente";
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
    }
    
    private void setupClickListeners() {
        ivCallClient.setOnClickListener(v -> {
            Toast.makeText(this, "Llamar a " + clientName, Toast.LENGTH_SHORT).show();
            // TODO: Implementar llamada telefónica
        });
        
        ivSendMessage.setOnClickListener(v -> sendMessage());
        
        ivAttach.setOnClickListener(v -> {
            Toast.makeText(this, "Adjuntar archivo", Toast.LENGTH_SHORT).show();
            // TODO: Implementar adjuntar archivos
        });
    }
    
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        
        messagesList = new ArrayList<>();
        messagesAdapter = new AdminChatMessagesAdapter(messagesList);
        rvMessages.setAdapter(messagesAdapter);
        
        loadSampleMessages();
    }
    
    private void loadChatData() {
        tvClientName.setText(clientName);
        tvClientStatus.setText("En línea");
    }
    
    private void loadSampleMessages() {
        // Mensajes de ejemplo desde la perspectiva del administrador
        // Los mensajes del cliente aparecen a la izquierda (gris)
        // Los mensajes del administrador aparecen a la derecha (azul)
        messagesList.add(new AdminChatMessage("Hola, buenas!", true, "14:20"));
        messagesList.add(new AdminChatMessage("Me interesa comprar el top verde", true, "14:21"));
        messagesList.add(new AdminChatMessage("¡Hola, buenas tardes!", false, "14:22"));
        messagesList.add(new AdminChatMessage("Sí, claro. ¿Cómo pagará?", false, "14:23"));
        messagesList.add(new AdminChatMessage("Con tarjeta.", true, "14:24"));
        messagesList.add(new AdminChatMessage("¿Es seguro? nunca he comprado por esta app.", true, "14:25"));
        messagesList.add(new AdminChatMessage("Sí, es seguro", false, "14:26"));
        messagesList.add(new AdminChatMessage("La app tiene un sistema de protección contra robos y estafas", false, "14:27"));
        messagesList.add(new AdminChatMessage("Perfecto! ¿Cómo es el envío?", true, "14:28"));
        messagesList.add(new AdminChatMessage("Usted solo realiza la transacción y yo le enviaré un motorizado con un costo adicional de 5 soles.", false, "14:29"));
        
        messagesAdapter.notifyDataSetChanged();
        rvMessages.scrollToPosition(messagesList.size() - 1);
    }
    
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Escriba un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Agregar mensaje a la lista
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());
        
        AdminChatMessage newMessage = new AdminChatMessage(messageText, false, currentTime); // false = mensaje del administrador
        messagesList.add(newMessage);
        messagesAdapter.notifyItemInserted(messagesList.size() - 1);
        rvMessages.scrollToPosition(messagesList.size() - 1);
        
        etMessage.setText("");
        
        // TODO: Enviar mensaje a la base de datos
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

    public AdminChatMessagesAdapter(List<AdminChatMessage> messages) {
        this.messages = messages;
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
        } else {
            // Mensaje del administrador (mostrar como mensaje de usuario en el layout)
            holder.layoutIncoming.setVisibility(View.GONE);
            holder.layoutOutgoing.setVisibility(View.VISIBLE);

            holder.tvOutgoingMessage.setText(message.message);
            // No hay campo de tiempo para mensajes salientes en este layout
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

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutIncoming = itemView.findViewById(R.id.layout_company_message);
            layoutOutgoing = itemView.findViewById(R.id.layout_user_message);
            // No hay layout_system_message en el layout actual
            layoutSystem = null;

            tvIncomingMessage = itemView.findViewById(R.id.tv_company_message);
            tvIncomingTime = itemView.findViewById(R.id.tv_message_time);

            tvOutgoingMessage = itemView.findViewById(R.id.tv_user_message);
            // No hay tv_outgoing_time en el layout actual
            tvOutgoingTime = null;

            // No hay tv_system_message en el layout actual
            tvSystemMessage = null;
        }
    }
}
