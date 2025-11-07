package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class CompanyChatActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private TextInputEditText etMessage;
    private MaterialButton btnSendMessage;
    private TextView tvCompanyName;
    private CompanyChatAdapter chatAdapter;
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
        chatAdapter = new CompanyChatAdapter();
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void loadCompanyInfo() {
        // Get company info from intent
        String companyName = getIntent().getStringExtra("company_name");
        if (companyName != null) {
            tvCompanyName.setText(companyName);
            getSupportActionBar().setTitle("Chat - " + companyName);
        }
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            // TODO: Enviar mensaje al servidor
            etMessage.setText("");
            Toast.makeText(this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
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

// Adaptador para los mensajes del chat de la empresa
class CompanyChatAdapter extends RecyclerView.Adapter<CompanyChatAdapter.ViewHolder> {
    
    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Datos de ejemplo para el chat
        android.view.View layoutUserMessage = holder.itemView.findViewById(R.id.layout_user_message);
        android.view.View layoutCompanyMessage = holder.itemView.findViewById(R.id.layout_company_message);
        TextView tvUserMessage = holder.itemView.findViewById(R.id.tv_user_message);
        TextView tvCompanyMessage = holder.itemView.findViewById(R.id.tv_company_message);
        TextView tvMessageTime = holder.itemView.findViewById(R.id.tv_message_time);

        // Alternar entre mensajes del usuario y de la empresa
        if (position % 2 == 0) {
            // Mensaje de la empresa
            layoutUserMessage.setVisibility(android.view.View.GONE);
            layoutCompanyMessage.setVisibility(android.view.View.VISIBLE);
            
            switch (position) {
                case 0:
                    tvCompanyMessage.setText("¡Hola! Bienvenido a Lima Adventure Tours. ¿En qué podemos ayudarte?");
                    break;
                case 2:
                    tvCompanyMessage.setText("Claro, el City Tour incluye transporte, guía profesional y entrada a todos los sitios históricos.");
                    break;
                case 4:
                    tvCompanyMessage.setText("El tour dura aproximadamente 6 horas y visitamos la Plaza de Armas, Catedral, y el Centro Histórico.");
                    break;
                default:
                    tvCompanyMessage.setText("¿Tienes alguna otra consulta?");
            }
            tvMessageTime.setText("10:" + (30 + position) + " AM");
        } else {
            // Mensaje del usuario
            layoutCompanyMessage.setVisibility(android.view.View.GONE);
            layoutUserMessage.setVisibility(android.view.View.VISIBLE);
            
            switch (position) {
                case 1:
                    tvUserMessage.setText("Hola, tengo una consulta sobre el City Tour Lima Centro");
                    break;
                case 3:
                    tvUserMessage.setText("¿Podrías darme más detalles sobre el itinerario?");
                    break;
                default:
                    tvUserMessage.setText("Gracias por la información");
            }
        }
    }

    @Override
    public int getItemCount() { return 6; }

    static class ViewHolder extends RecyclerView.ViewHolder { 
        ViewHolder(android.view.View v) { super(v); } 
    }
}

