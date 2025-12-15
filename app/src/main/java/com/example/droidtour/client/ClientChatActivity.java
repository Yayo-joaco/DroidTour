package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.example.droidtour.utils.ConversationHelper;
import com.example.droidtour.utils.PresenceManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import android.widget.ImageView;

import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ClientChatActivity extends AppCompatActivity {
    
    private static final String TAG = "ClientChatActivity";
    private RecyclerView rvCompanyChats;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private ConversationHelper conversationHelper;
    private PresenceManager presenceManager;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private View layoutNoResults;
    private ClientCompanyChatsAdapter adapter;
    private String currentClientId;

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
        
        // Validar que el usuario sea CLIENT
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_client_chat);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        // Inicializar managers
        conversationHelper = new ConversationHelper();
        presenceManager = new PresenceManager();
        currentClientId = prefsManager.getUserId();
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
        loadClientConversations();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Chat con Empresas");
    }
    
    private void initializeViews() {
        rvCompanyChats = findViewById(R.id.rv_company_chats);
        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        layoutNoResults = findViewById(R.id.layout_no_results);
    }
    
    private void setupRecyclerView() {
        rvCompanyChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClientCompanyChatsAdapter(company -> {
             // Abrir chat con empresa específica usando el chat unificado
             Intent intent = new Intent(this, CompanyChatActivity.class);
             intent.putExtra("company_name", company.name);
             intent.putExtra("company_id", company.companyId);
             startActivity(intent);
        });
        rvCompanyChats.setAdapter(adapter);

        // Search handling
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            btnClearSearch.setVisibility(View.GONE);
            adapter.filter("");
            updateNoResultsVisibility();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s == null ? "" : s.toString().trim();
                // Mostrar u ocultar botón limpiar
                btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.filter(q);
                updateNoResultsVisibility();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
    
    private void loadClientConversations() {
        if (currentClientId == null || currentClientId.isEmpty()) {
            Log.e(TAG, "currentClientId es null o vacío");
            return;
        }
        
        // Cargar conversaciones del cliente desde Realtime Database
        conversationHelper.getConversationsForClient(currentClientId, new ConversationHelper.ConversationsCallback() {
            @Override
            public void onConversationsLoaded(List<ConversationHelper.ConversationData> conversations) {
                java.util.List<CompanyChat> companyChats = new java.util.ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                
                for (ConversationHelper.ConversationData conv : conversations) {
                    String companyId = conv.getCompanyId();
                    String companyName = conv.getCompanyName() != null ? conv.getCompanyName() : "Empresa";
                    String lastMessage = conv.getLastMessage() != null ? conv.getLastMessage() : "";
                    
                    // Formatear timestamp
                    long timestamp = conv.getLastMessageTimestamp();
                    String timeStr = "";
                    if (timestamp > 0) {
                        // Verificar si es de hoy, ayer, o fecha anterior
                        long now = System.currentTimeMillis();
                        long diff = now - timestamp;
                        long daysDiff = diff / (24 * 60 * 60 * 1000);
                        
                        if (daysDiff == 0) {
                            // Hoy: mostrar hora
                            timeStr = sdf.format(new java.util.Date(timestamp));
                        } else if (daysDiff == 1) {
                            // Ayer
                            timeStr = "Ayer";
                        } else if (daysDiff < 7) {
                            // Esta semana: mostrar día
                            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                            timeStr = dayFormat.format(new java.util.Date(timestamp));
                        } else {
                            // Más de una semana: mostrar fecha
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            timeStr = dateFormat.format(new java.util.Date(timestamp));
                        }
                    }
                    
                    // Obtener contador de no leídos (desde perspectiva del cliente)
                    int unreadCount = conv.getUnreadCountClient();
                    
                    CompanyChat chat = new CompanyChat(
                        companyId,
                        companyName,
                        lastMessage,
                        timeStr,
                        unreadCount > 0,
                        unreadCount
                    );
                    companyChats.add(chat);
                }
                
                adapter.updateData(companyChats);
                updateNoResultsVisibility();
                Log.d(TAG, "Chats cargados: " + companyChats.size());
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error al cargar conversaciones", e);
                Toast.makeText(ClientChatActivity.this, "Error al cargar chats", Toast.LENGTH_SHORT).show();
                updateNoResultsVisibility();
            }
        });
    }

    private void updateNoResultsVisibility() {
        if (adapter == null) return;
        if (adapter.getItemCount() == 0) {
            layoutNoResults.setVisibility(View.VISIBLE);
        } else {
            layoutNoResults.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Establecer usuario como en línea
        if (currentClientId != null) {
            presenceManager.setOnline(currentClientId);
        }
        
        // Recargar conversaciones y escuchar cambios en tiempo real
        if (currentClientId != null) {
            conversationHelper.listenToClientConversations(currentClientId, new ConversationHelper.ConversationsCallback() {
                @Override
                public void onConversationsLoaded(List<ConversationHelper.ConversationData> conversations) {
                    java.util.List<CompanyChat> companyChats = new java.util.ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    
                    for (ConversationHelper.ConversationData conv : conversations) {
                        String companyId = conv.getCompanyId();
                        String companyName = conv.getCompanyName() != null ? conv.getCompanyName() : "Empresa";
                        String lastMessage = conv.getLastMessage() != null ? conv.getLastMessage() : "";
                        
                        long timestamp = conv.getLastMessageTimestamp();
                        String timeStr = "";
                        if (timestamp > 0) {
                            long now = System.currentTimeMillis();
                            long diff = now - timestamp;
                            long daysDiff = diff / (24 * 60 * 60 * 1000);
                            
                            if (daysDiff == 0) {
                                timeStr = sdf.format(new java.util.Date(timestamp));
                            } else if (daysDiff == 1) {
                                timeStr = "Ayer";
                            } else if (daysDiff < 7) {
                                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                                timeStr = dayFormat.format(new java.util.Date(timestamp));
                            } else {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                timeStr = dateFormat.format(new java.util.Date(timestamp));
                            }
                        }
                        
                        int unreadCount = conv.getUnreadCountClient();
                        
                        CompanyChat chat = new CompanyChat(
                            companyId,
                            companyName,
                            lastMessage,
                            timeStr,
                            unreadCount > 0,
                            unreadCount
                        );
                        companyChats.add(chat);
                    }
                    
                    adapter.updateData(companyChats);
                    updateNoResultsVisibility();
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
        if (presenceManager != null && currentClientId != null) {
            presenceManager.setOffline(currentClientId);
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

// Clase para representar una empresa con chat
class CompanyChat {
    public String companyId;
    public String name;
    public String lastMessage;
    public String timestamp;
    public boolean hasUnreadMessages;
    public int unreadCount;

    public CompanyChat(String companyId, String name, String lastMessage, String timestamp, boolean hasUnreadMessages, int unreadCount) {
        this.companyId = companyId;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.hasUnreadMessages = hasUnreadMessages;
        this.unreadCount = unreadCount;
    }
}

// Adaptador para chats de empresas desde perspectiva del cliente
class ClientCompanyChatsAdapter extends RecyclerView.Adapter<ClientCompanyChatsAdapter.ViewHolder> {
    interface OnCompanyChatClick { void onClick(CompanyChat company); }
    
    private final OnCompanyChatClick onCompanyChatClick;
    private final java.util.List<CompanyChat> allCompanyChats = new java.util.ArrayList<>();
    private final java.util.List<CompanyChat> filteredCompanyChats = new java.util.ArrayList<>();

    ClientCompanyChatsAdapter(OnCompanyChatClick listener) {
        this.onCompanyChatClick = listener;
    }
    
    public void updateData(java.util.List<CompanyChat> newData) {
        allCompanyChats.clear();
        if (newData != null) {
            allCompanyChats.addAll(newData);
        }
        filter(""); // Aplicar filtro actual (puede estar vacío)
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_client_company_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CompanyChat company = filteredCompanyChats.get(position);

        holder.tvCompanyName.setText(company.name);
        holder.tvLastMessage.setText(company.lastMessage);
        holder.tvTimestamp.setText(company.timestamp);
        
        // Mostrar/ocultar badge de mensajes no leídos
        if (company.hasUnreadMessages && company.unreadCount > 0) {
            if (holder.cvUnreadBadge != null) {
                holder.cvUnreadBadge.setVisibility(View.VISIBLE);
            }
            if (holder.tvUnreadCount != null) {
                holder.tvUnreadCount.setVisibility(View.VISIBLE);
                holder.tvUnreadCount.setText(String.valueOf(company.unreadCount));
            }
        } else {
            if (holder.cvUnreadBadge != null) {
                holder.cvUnreadBadge.setVisibility(View.GONE);
            }
            if (holder.tvUnreadCount != null) {
                holder.tvUnreadCount.setVisibility(View.GONE);
            }
        }
        
        // Cargar logo de la empresa desde Firestore
        loadCompanyLogo(holder, company.companyId);
        
        holder.itemView.setOnClickListener(v -> onCompanyChatClick.onClick(company));
    }
    
    private void loadCompanyLogo(ViewHolder holder, String companyId) {
        if (companyId == null || companyId.isEmpty() || companyId.equals("unknown_company")) {
            // Usar placeholder por defecto
            if (holder.ivCompanyAvatar != null) {
                holder.ivCompanyAvatar.setImageResource(R.drawable.ic_mountain);
            }
            return;
        }
        
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getCompanyById(companyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Company && holder.ivCompanyAvatar != null) {
                    Company company = (Company) result;
                    String logoUrl = company.getLogoUrl();
                    
                    if (logoUrl != null && !logoUrl.isEmpty()) {
                        Glide.with(holder.ivCompanyAvatar.getContext())
                                .load(logoUrl)
                                .placeholder(R.drawable.ic_mountain)
                                .error(R.drawable.ic_mountain)
                                .circleCrop()
                                .into(holder.ivCompanyAvatar);
                    } else {
                        holder.ivCompanyAvatar.setImageResource(R.drawable.ic_mountain);
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Usar placeholder por defecto si falla
                if (holder.ivCompanyAvatar != null) {
                    holder.ivCompanyAvatar.setImageResource(R.drawable.ic_mountain);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredCompanyChats.size();
    }

    // Filtrar por nombre de empresa (case-insensitive)
    public void filter(String query) {
        filteredCompanyChats.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredCompanyChats.addAll(allCompanyChats);
        } else {
            String q = query.toLowerCase();
            for (CompanyChat c : allCompanyChats) {
                if (c.name != null && c.name.toLowerCase().contains(q)) {
                    filteredCompanyChats.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCompanyName, tvLastMessage, tvTimestamp, tvUnreadCount;
        ImageView ivCompanyAvatar;
        android.view.View cvUnreadBadge;

        ViewHolder(View view) {
            super(view);
            tvCompanyName = view.findViewById(R.id.tv_company_name);
            tvLastMessage = view.findViewById(R.id.tv_last_message);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvUnreadCount = view.findViewById(R.id.tv_unread_count);
            ivCompanyAvatar = view.findViewById(R.id.iv_company_avatar);
            cvUnreadBadge = view.findViewById(R.id.cv_unread_badge);
        }
    }
}
