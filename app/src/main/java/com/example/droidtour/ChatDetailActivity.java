package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class ChatDetailActivity extends AppCompatActivity {
    
    private TextView tvClientName, tvClientStatus, tvTourName, tvTourDate, tvBookingStatus;
    private ImageView ivClientAvatar, ivCallClient;
    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private FloatingActionButton fabSendMessage;
    
    private String chatId, clientName, tourName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);
        
        getIntentData();
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadChatData();
    }
    
    private void getIntentData() {
        chatId = getIntent().getStringExtra("CHAT_ID");
        clientName = getIntent().getStringExtra("CLIENT_NAME");
        tourName = getIntent().getStringExtra("TOUR_NAME");
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        tvClientName = findViewById(R.id.tv_client_name);
        tvClientStatus = findViewById(R.id.tv_client_status);
        tvTourName = findViewById(R.id.tv_tour_name);
        tvTourDate = findViewById(R.id.tv_tour_date);
        tvBookingStatus = findViewById(R.id.tv_booking_status);
        
        ivClientAvatar = findViewById(R.id.iv_client_avatar);
        ivCallClient = findViewById(R.id.iv_call_client);
        
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        fabSendMessage = findViewById(R.id.fab_send_message);
    }
    
    private void setupClickListeners() {
        ivCallClient.setOnClickListener(v -> {
            Toast.makeText(this, "Llamar a " + clientName, Toast.LENGTH_SHORT).show();
            // TODO: Implementar llamada telefónica
        });
        
        fabSendMessage.setOnClickListener(v -> sendMessage());
    }
    
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Para mostrar mensajes más recientes abajo
        rvMessages.setLayoutManager(layoutManager);
        
        // TODO: Configurar adapter para mensajes del chat
    }
    
    private void loadChatData() {
        // TODO: Cargar datos reales desde base de datos
        // Por ahora mostrar datos de prueba
        if (clientName != null) {
            tvClientName.setText(clientName);
        }
        if (tourName != null) {
            tvTourName.setText(tourName);
        }
        
        tvClientStatus.setText("En línea");
        tvTourDate.setText("Mañana, 15 Dic • 09:00 AM");
        tvBookingStatus.setText("CONFIRMADO");
    }
    
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Escriba un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // TODO: Enviar mensaje a la base de datos y actualizar RecyclerView
        Toast.makeText(this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
        etMessage.setText("");
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
