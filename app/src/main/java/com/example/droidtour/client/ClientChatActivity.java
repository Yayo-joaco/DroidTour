package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.CompanyChatActivity;
import com.example.droidtour.R;
import com.google.android.material.appbar.MaterialToolbar;

public class ClientChatActivity extends AppCompatActivity {
    
    private RecyclerView rvCompanyChats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }
    
    private void setupRecyclerView() {
        rvCompanyChats.setLayoutManager(new LinearLayoutManager(this));
        rvCompanyChats.setAdapter(new ClientCompanyChatsAdapter(company -> {
            // Abrir chat con empresa específica usando el chat unificado
            Intent intent = new Intent(this, CompanyChatActivity.class);
            intent.putExtra("company_name", company.name);
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
    private final CompanyChat[] companyChats;

    ClientCompanyChatsAdapter(OnCompanyChatClick listener) {
        this.onCompanyChatClick = listener;
        // Datos mock de chats con empresas
        this.companyChats = new CompanyChat[] {
            new CompanyChat("Tours Cusco Adventures", "Perfecto, nos vemos mañana a las 8 AM", "2:30 PM", false, 0),
            new CompanyChat("Lima City Travel", "Su reserva ha sido confirmada", "1:15 PM", true, 2),
            new CompanyChat("Peru Explorer Tours", "¿Necesita transporte desde el hotel?", "11:45 AM", true, 1),
            new CompanyChat("Sacred Valley Tours", "Gracias por su valoración", "Ayer", false, 0)
        };
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_client_company_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CompanyChat company = companyChats[position];
        
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
        return companyChats.length;
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
