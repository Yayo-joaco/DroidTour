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

import com.example.droidtour.utils.ChatManager;
import com.example.droidtour.models.Message;
import com.example.droidtour.utils.PreferencesManager;

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
    private ChatManager chatManager;
    private String conversationId;
    private String companyName;
    private String currentUserId;

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

        // Preparar ChatManager
        chatManager = new ChatManager();
        currentUserId = prefsManager.getUserId();

        // conversationId: preferimos recibir company_id en el Intent, sino generar uno a partir del nombre
        conversationId = getIntent().getStringExtra("company_id");
        if (conversationId == null || conversationId.isEmpty()) {
            // Sanitizar el nombre para usarlo como id
            String tmp = companyName != null ? companyName : "unknown_company";
            conversationId = tmp.trim().toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_-]", "");
        }

        // Escuchar mensajes
        chatManager.listenForMessages(conversationId, new ChatManager.MessagesListener() {
            @Override
            public void onNewMessages(List<Message> messages) {
                runOnUiThread(() -> {
                    chatAdapter.addMessages(messages);
                    rvChatMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(CompanyChatActivity.this, "Error al recibir mensajes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
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

    private void sendMessage() {
        String messageText = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (messageText.isEmpty()) return;

        // Construir Message usando el modelo existente
        Message msg = new Message();
        msg.setMessageId(null); // Firestore asignará id
        msg.setSenderId(currentUserId);
        msg.setSenderName(prefsManager.getUserName());
        msg.setReceiverId(getIntent().getStringExtra("company_id") != null ? getIntent().getStringExtra("company_id") : conversationId);
        msg.setReceiverName(companyName != null ? companyName : "Empresa");
        msg.setSenderType(prefsManager.getUserType());
        msg.setMessageText(messageText);
        msg.setTimestamp(Timestamp.now());
        msg.setRead(false);
        msg.setConversationId(conversationId);

        // Enviar mediante ChatManager
        chatManager.sendMessage(conversationId, msg, new ChatManager.SendCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    etMessage.setText("");
                    // Opcional: añadir al adaptador inmediatamente (optimista)
                    chatAdapter.addMessage(msg);
                    rvChatMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(CompanyChatActivity.this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatManager != null) chatManager.stopListening();
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View v) {
            super(v);
        }
    }
}