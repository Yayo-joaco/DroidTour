package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CompanyChatActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private TextInputEditText etMessage;
    private MaterialButton btnSendMessage;
    private TextView tvCompanyName;
    private CompanyChatAdapter chatAdapter;
    
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private String currentUserId;
    private String companyId;
    private String conversationId;
    private List<Map<String, Object>> messagesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_chat);

        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = authManager.getCurrentUserId();
        
        // 🔥 TEMPORAL: Para testing sin login
        if (currentUserId == null) {
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", Toast.LENGTH_SHORT).show();
        }

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadCompanyInfo();
        loadMessages();
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
        rvChatMessages = findViewById(R.id.rv_chat_messages);
        etMessage = findViewById(R.id.et_message);
        btnSendMessage = findViewById(R.id.btn_send_message);
        tvCompanyName = findViewById(R.id.tv_company_name);
    }

    private void setupRecyclerView() {
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new CompanyChatAdapter(messagesList, currentUserId);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void loadCompanyInfo() {
        String companyName = getIntent().getStringExtra("company_name");
        companyId = getIntent().getStringExtra("company_id");
        if (companyId == null) companyId = "COMPANY_001";
        
        conversationId = currentUserId + "_" + companyId;
        
        if (companyName != null) {
            tvCompanyName.setText(companyName);
            getSupportActionBar().setTitle("Chat - " + companyName);
        }
    }

    private void loadMessages() {
        firestoreManager.getConversationMessages(conversationId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                messagesList.clear();
                messagesList.addAll((List<Map<String, Object>>) result);
                chatAdapter.notifyDataSetChanged();
                if (!messagesList.isEmpty()) {
                    rvChatMessages.scrollToPosition(messagesList.size() - 1);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CompanyChatActivity.this, "Error cargando mensajes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            firestoreManager.sendMessage(currentUserId, "Cliente", companyId, 
                tvCompanyName.getText().toString(), "CLIENT", messageText, 
                conversationId, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    etMessage.setText("");
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(CompanyChatActivity.this, "Error enviando mensaje", Toast.LENGTH_SHORT).show();
                }
            });
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
}

// Adaptador para los mensajes del chat de la empresa
class CompanyChatAdapter extends RecyclerView.Adapter<CompanyChatAdapter.ViewHolder> {
    private final List<Map<String, Object>> messages;
    private final String currentUserId;
    
    CompanyChatAdapter(List<Map<String, Object>> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }
    
    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Map<String, Object> message = messages.get(position);
        
        android.view.View layoutUserMessage = holder.itemView.findViewById(R.id.layout_user_message);
        android.view.View layoutCompanyMessage = holder.itemView.findViewById(R.id.layout_company_message);
        TextView tvUserMessage = holder.itemView.findViewById(R.id.tv_user_message);
        TextView tvCompanyMessage = holder.itemView.findViewById(R.id.tv_company_message);
        TextView tvMessageTime = holder.itemView.findViewById(R.id.tv_message_time);
        
        String senderId = (String) message.get("senderId");
        String messageText = (String) message.get("messageText");
        boolean isMyMessage = currentUserId.equals(senderId);
        
        if (isMyMessage) {
            layoutCompanyMessage.setVisibility(android.view.View.GONE);
            layoutUserMessage.setVisibility(android.view.View.VISIBLE);
            if (tvUserMessage != null) tvUserMessage.setText(messageText);
        } else {
            layoutUserMessage.setVisibility(android.view.View.GONE);
            layoutCompanyMessage.setVisibility(android.view.View.VISIBLE);
            if (tvCompanyMessage != null) tvCompanyMessage.setText(messageText);
        }
        
        Object timestampObj = message.get("timestamp");
        if (timestampObj instanceof com.google.firebase.Timestamp && tvMessageTime != null) {
            Date date = ((com.google.firebase.Timestamp) timestampObj).toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvMessageTime.setText(sdf.format(date));
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

