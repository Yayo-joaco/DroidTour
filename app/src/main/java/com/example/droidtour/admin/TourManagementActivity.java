package com.example.droidtour.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TourManagementActivity extends AppCompatActivity implements AdminTourAdapter.OnTourClickListener {

    private static final String TAG = "TourManagementActivity";
    private RecyclerView rvTours;
    private AdminTourAdapter adapter;
    private final List<Tour> allTours = new ArrayList<>();
    private final List<Tour> filteredTours = new ArrayList<>();
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;
    private LinearLayout emptyStateLayout;
    private TextInputEditText searchEditText;
    private Chip chipAll, chipActive, chipPending;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_tour_managment);

        firestoreManager = FirestoreManager.getInstance();
        prefsManager = new PreferencesManager(this);
        
        setupToolbar();
        initViews();
        setupRecyclerView();
        setupSearchAndFilters();
        setupFab();
        
        loadCompanyAndTours();
    }
    
    private void initViews() {
        rvTours = findViewById(R.id.toursRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        searchEditText = findViewById(R.id.searchEditText);
        chipAll = findViewById(R.id.chipAll);
        chipActive = findViewById(R.id.chipActive);
        chipPending = findViewById(R.id.chipPending);
    }
    
    private void setupRecyclerView() {
        rvTours.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminTourAdapter(filteredTours, this);
        rvTours.setAdapter(adapter);
    }
    
    private void setupSearchAndFilters() {
        // B\u00fasqueda
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTours(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Filtros
        chipAll.setOnClickListener(v -> {
            currentFilter = "all";
            filterTours(searchEditText.getText().toString());
        });
        
        chipActive.setOnClickListener(v -> {
            currentFilter = "active";
            filterTours(searchEditText.getText().toString());
        });
        
        chipPending.setOnClickListener(v -> {
            currentFilter = "pending";
            filterTours(searchEditText.getText().toString());
        });
    }
    
    private void setupFab() {
        ExtendedFloatingActionButton fab = findViewById(R.id.fabCreateTour);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateTourActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadCompanyAndTours() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    loadTours();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
                Toast.makeText(TourManagementActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadTours() {
        // Cargar TODOS los tours de la compañía desde Firebase (públicos y no públicos)
        firestoreManager.getAllToursByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                allTours.clear();
                allTours.addAll((List<Tour>) result);
                filterTours(searchEditText.getText().toString());
                Log.d(TAG, "Tours cargados: " + allTours.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando tours", e);
                Toast.makeText(TourManagementActivity.this, "Error al cargar tours", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void filterTours(String searchText) {
        filteredTours.clear();
        
        for (Tour tour : allTours) {
            boolean matchesSearch = true;
            boolean matchesFilter = true;
            
            // Filtro de búsqueda
            if (searchText != null && !searchText.isEmpty()) {
                String tourName = tour.getTourName() != null ? tour.getTourName().toLowerCase(Locale.ROOT) : "";
                matchesSearch = tourName.contains(searchText.toLowerCase(Locale.ROOT));
            }
            
            // Filtro de estado
            switch (currentFilter) {
                case "active":
                    matchesFilter = tour.getActive() != null && tour.getActive();
                    break;
                case "pending":
                    matchesFilter = tour.getActive() == null || !tour.getActive();
                    break;
                case "all":
                default:
                    matchesFilter = true;
                    break;
            }
            
            if (matchesSearch && matchesFilter) {
                filteredTours.add(tour);
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (filteredTours.isEmpty()) {
            rvTours.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            rvTours.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (currentCompanyId != null) {
            loadTours();
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Gestión de Tours");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTourClick(int position, Tour tour, View anchorView) {
        // Abrir pantalla de edición del tour (igual que servicios)
        Intent intent = new Intent(this, CreateTourActivity.class);
        intent.putExtra("TOUR_ID", tour.getTourId());
        intent.putExtra("EDIT_MODE", true);
        startActivity(intent);
    }
    
    @Override
    public void onTourEdit(int position, Tour tour) {
        // Verificar si está activo (tiene guía asignado)
        if (tour.getActive() != null && tour.getActive()) {
            Toast.makeText(this, "No se puede editar un tour con guía asignado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, CreateTourActivity.class);
        intent.putExtra("TOUR_ID", tour.getTourId());
        intent.putExtra("EDIT_MODE", true);
        startActivity(intent);
    }
    
    @Override
    public void onTourDelete(int position, Tour tour) {
        // Verificar si está activo
        if (tour.getActive() != null && tour.getActive()) {
            Toast.makeText(this, "No se puede eliminar un tour con guía asignado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Tour")
            .setMessage("¿Estás seguro de eliminar '" + tour.getTourName() + "'?")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                firestoreManager.deleteTour(tour.getTourId(), new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(TourManagementActivity.this, "Tour eliminado", Toast.LENGTH_SHORT).show();
                        loadTours();
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(TourManagementActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
}
