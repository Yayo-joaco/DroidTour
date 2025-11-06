package com.example.droidtour.client;

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

import com.example.droidtour.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientChatDetailActivity extends AppCompatActivity {
    
    private TextView tvCompanyName, tvCompanyStatus;
    private ImageView ivCompanyAvatar, ivCallCompany, ivSendMessage, ivAttach;
    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    
    private String companyName;
    private ClientChatMessagesAdapter messagesAdapter;
    private List<ClientChatMessage> messagesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_chat_detail);
        
        getIntentData();
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadChatData();
    }
    
    private void getIntentData() {
        companyName = getIntent().getStringExtra("COMPANY_NAME");
        if (companyName == null) {
            companyName = "Empresa de Tours";
        }
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
    }
    
    private void initializeViews() {
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvCompanyStatus = findViewById(R.id.tv_company_status);
        
        ivCompanyAvatar = findViewById(R.id.iv_company_avatar);
        ivCallCompany = findViewById(R.id.iv_call_company);
        ivSendMessage = findViewById(R.id.iv_send_message);
        ivAttach = findViewById(R.id.iv_attach);
        
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
    }
    
    private void setupClickListeners() {
        ivCallCompany.setOnClickListener(v -> {
            Toast.makeText(this, "Llamar a " + companyName, Toast.LENGTH_SHORT).show();
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
        messagesAdapter = new ClientChatMessagesAdapter(messagesList);
        rvMessages.setAdapter(messagesAdapter);
        
        loadSampleMessages();
    }
    
    private void loadChatData() {
        tvCompanyName.setText(companyName);
        tvCompanyStatus.setText("En línea");
    }
    
    private void loadSampleMessages() {
        // Mensajes de ejemplo basados en la imagen proporcionada
        messagesList.add(new ClientChatMessage("Hola, buenas!", true, "14:20"));
        messagesList.add(new ClientChatMessage("Me interesa comprar el top verde", false, "14:21"));
        messagesList.add(new ClientChatMessage("¡Hola, buenas tardes!", true, "14:22"));
        messagesList.add(new ClientChatMessage("Sí, claro. ¿Cómo pagará?", true, "14:23"));
        messagesList.add(new ClientChatMessage("Con tarjeta.", false, "14:24"));
        messagesList.add(new ClientChatMessage("¿Es seguro? nunca he comprado por esta app.", false, "14:25"));
        messagesList.add(new ClientChatMessage("Sí, es seguro", true, "14:26"));
        messagesList.add(new ClientChatMessage("La app tiene un sistema de protección contra robos y estafas", true, "14:27"));
        messagesList.add(new ClientChatMessage("Perfecto! ¿Cómo es el envío?", false, "14:28"));
        messagesList.add(new ClientChatMessage("Usted solo realiza la transacción y yo le enviaré un motorizado con un costo adicional de 5 soles.", true, "14:29"));
        
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
        
        ClientChatMessage newMessage = new ClientChatMessage(messageText, false, currentTime); // false = mensaje del cliente
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

// Clase para representar un mensaje de chat del cliente
class ClientChatMessage {
    public String message;
    public boolean isFromCompany; // true = empresa, false = cliente
    public String timestamp;

    public ClientChatMessage(String message, boolean isFromCompany, String timestamp) {
        this.message = message;
        this.isFromCompany = isFromCompany;
        this.timestamp = timestamp;
    }
}

// Adaptador para los mensajes del chat del cliente
class ClientChatMessagesAdapter extends RecyclerView.Adapter<ClientChatMessagesAdapter.MessageViewHolder> {
    private List<ClientChatMessage> messages;

    public ClientChatMessagesAdapter(List<ClientChatMessage> messages) {
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
        ClientChatMessage message = messages.get(position);

        if (message.isFromCompany) {
            // Mensaje de la empresa (mostrar como mensaje de empresa en el layout)
            holder.layoutIncoming.setVisibility(View.VISIBLE);
            holder.layoutOutgoing.setVisibility(View.GONE);

            holder.tvIncomingMessage.setText(message.message);
            if (holder.tvIncomingTime != null) {
                holder.tvIncomingTime.setText(message.timestamp);
            }
        } else {
            // Mensaje del cliente (mostrar como mensaje de usuario en el layout)
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
