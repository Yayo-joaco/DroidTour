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
import com.google.android.material.appbar.MaterialToolbar;

public class ClientChatActivity extends AppCompatActivity {
    
    private RecyclerView rvCompanyChats;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private View layoutNoResults;
    private ClientCompanyChatsAdapter adapter;

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
        
        setupToolbar();
        initializeViews();
        setupRecyclerView();
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

    private void updateNoResultsVisibility() {
        if (adapter == null) return;
        if (adapter.getItemCount() == 0) {
            layoutNoResults.setVisibility(View.VISIBLE);
        } else {
            layoutNoResults.setVisibility(View.GONE);
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
    public String name;
    public String lastMessage;
    public String timestamp;
    public boolean hasUnreadMessages;
    public int unreadCount;

    public CompanyChat(String name, String lastMessage, String timestamp, boolean hasUnreadMessages, int unreadCount) {
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
        // Datos mock de chats con empresas (inicializar lista completa)
        allCompanyChats.add(new CompanyChat("Tours Cusco Adventures", "Perfecto, nos vemos mañana a las 8 AM", "2:30 PM", false, 0));
        allCompanyChats.add(new CompanyChat("Lima City Travel", "Su reserva ha sido confirmada", "1:15 PM", true, 2));
        allCompanyChats.add(new CompanyChat("Peru Explorer Tours", "¿Necesita transporte desde el hotel?", "11:45 AM", true, 1));
        allCompanyChats.add(new CompanyChat("Sacred Valley Tours", "Gracias por su valoración", "Ayer", false, 0));

        // Inicialmente mostrar todos
        filteredCompanyChats.addAll(allCompanyChats);
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
        
        if (company.hasUnreadMessages) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(company.unreadCount));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> onCompanyChatClick.onClick(company));
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

        ViewHolder(View view) {
            super(view);
            tvCompanyName = view.findViewById(R.id.tv_company_name);
            tvLastMessage = view.findViewById(R.id.tv_last_message);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvUnreadCount = view.findViewById(R.id.tv_unread_count);
        }
    }
}
