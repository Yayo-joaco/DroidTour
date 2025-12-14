package com.example.droidtour.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import java.util.ArrayList;
import java.util.List;

public class TourManagementActivity extends AppCompatActivity implements AdminTourAdapter.OnTourClickListener {

    private static final String TAG = "TourManagementActivity";
    private RecyclerView rvTours;
    private AdminTourAdapter adapter;
    private final List<String> tourNames = new ArrayList<>();
    private final List<Tour> tours = new ArrayList<>();
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_tour_managment);

        firestoreManager = FirestoreManager.getInstance();
        prefsManager = new PreferencesManager(this);
        
        setupToolbar();

        rvTours = findViewById(R.id.toursRecyclerView);
        rvTours.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminTourAdapter(tourNames, this);
        rvTours.setAdapter(adapter);
        
        loadCompanyAndTours();
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
        // Cargar tours de la compañía desde Firebase
        firestoreManager.getToursByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> allTours = (List<Tour>) result;
                tours.clear();
                tourNames.clear();
                
                for (Tour t : allTours) {
                    tours.add(t);
                    tourNames.add(t.getName() != null ? t.getName() : "Tour");
                }
                
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Tours cargados: " + tours.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando tours", e);
                Toast.makeText(TourManagementActivity.this, "Error al cargar tours", Toast.LENGTH_SHORT).show();
            }
        });
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
    public void onTourClick(int position, String tourName, View anchorView) {
        // Mostrar BottomSheet con detalles (usando el layout existente)
        TourDetailsBottomSheet bs = TourDetailsBottomSheet.newInstance(tourName);
        bs.show(getSupportFragmentManager(), "tour_details");
    }
}
