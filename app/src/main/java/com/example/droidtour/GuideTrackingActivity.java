package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class GuideTrackingActivity extends AppCompatActivity {
    
    private static final String TAG = "GuideTrackingActivity";
    private MaterialCardView cardMap;
    private RecyclerView rvActiveGuides;
    private TextView tvActiveCount;
    private FloatingActionButton fabFilter;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private String currentCompanyId;
    private List<User> activeGuides = new ArrayList<>();
    private ActiveGuidesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea ADMIN o GUIDE
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("COMPANY_ADMIN") && !userType.equals("GUIDE"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_guide_tracking);
        
        firestoreManager = FirestoreManager.getInstance();
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadCompanyAndGuides();
    }
    
    private void loadCompanyAndGuides() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadActiveGuides();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
            }
        });
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        cardMap = findViewById(R.id.card_map);
        rvActiveGuides = findViewById(R.id.rv_active_guides);
        tvActiveCount = findViewById(R.id.tv_active_count);
        fabFilter = findViewById(R.id.fab_filter);
    }
    
    private void setupClickListeners() {
        cardMap.setOnClickListener(v -> {
            Toast.makeText(this, "Abrir mapa completo", Toast.LENGTH_SHORT).show();
            // TODO: Implementar mapa completo con ubicaciones de guías
        });
        
        fabFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filtros de seguimiento", Toast.LENGTH_SHORT).show();
            // TODO: Mostrar dialog de filtros
        });
    }
    
    private void setupRecyclerView() {
        rvActiveGuides.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActiveGuidesAdapter(activeGuides);
        rvActiveGuides.setAdapter(adapter);
    }
    
    private void loadActiveGuides() {
        // Cargar guías desde Firebase
        firestoreManager.getGuideUsers(new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<User> allGuides = (List<User>) result;
                activeGuides.clear();
                
                // Filtrar guías activos (con status activo)
                for (User guide : allGuides) {
                    if ("active".equalsIgnoreCase(guide.getStatus())) {
                        activeGuides.add(guide);
                    }
                }
                
                tvActiveCount.setText(activeGuides.size() + " activos");
                adapter.updateData(activeGuides);
                
                Log.d(TAG, "Guías activos cargados: " + activeGuides.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando guías", e);
                tvActiveCount.setText("0 activos");
            }
        });
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
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    
    // Adapter para guías activos
    private static class ActiveGuidesAdapter extends RecyclerView.Adapter<ActiveGuidesAdapter.ViewHolder> {
        private List<User> guides;
        
        ActiveGuidesAdapter(List<User> guides) {
            this.guides = guides != null ? guides : new ArrayList<>();
        }
        
        void updateData(List<User> newData) {
            this.guides = newData != null ? newData : new ArrayList<>();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User guide = guides.get(position);
            holder.text1.setText(guide.getFullName() != null ? guide.getFullName() : "Guía");
            holder.text2.setText("Estado: " + (guide.getStatus() != null ? guide.getStatus() : "activo"));
        }
        
        @Override
        public int getItemCount() {
            return guides != null ? guides.size() : 0;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View view) {
                super(view);
                text1 = view.findViewById(android.R.id.text1);
                text2 = view.findViewById(android.R.id.text2);
            }
        }
    }
}
