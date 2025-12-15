package com.example.droidtour.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Service;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ServiceManagementActivity extends AppCompatActivity {

    private static final String TAG = "ServiceManagementActivity";

    private RecyclerView rvServices;
    private TextView tvEmptyState;
    private FloatingActionButton fabAddService;

    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private String companyId;

    private ServiceAdapter adapter;
    private List<Service> serviceList = new ArrayList<>();

    private final ActivityResultLauncher<Intent> createServiceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadServices();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_management);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        prefsManager = new PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();

        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }

        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("COMPANY_ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        loadCompanyId();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Gestión de Servicios");
        }
    }

    private void initializeViews() {
        rvServices = findViewById(R.id.rv_services);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        fabAddService = findViewById(R.id.fab_add_service);

        rvServices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ServiceAdapter();
        rvServices.setAdapter(adapter);

        fabAddService.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateServiceActivity.class);
            createServiceLauncher.launch(intent);
        });
    }

    private void loadCompanyId() {
        String userId = prefsManager.getUserId();
        if (userId != null) {
            firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    User user = (User) result;
                    if (user != null && user.getCompanyId() != null) {
                        companyId = user.getCompanyId();
                        loadServices();
                    } else {
                        tvEmptyState.setText("Error: No se encontró la empresa");
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvServices.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error obteniendo companyId", e);
                    tvEmptyState.setText("Error al cargar datos");
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void loadServices() {
        if (companyId == null) return;

        firestoreManager.getServicesByCompany(companyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Service> services = (List<Service>) result;
                serviceList.clear();
                if (services != null) {
                    serviceList.addAll(services);
                }
                adapter.notifyDataSetChanged();

                if (serviceList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvServices.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvServices.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando servicios", e);
                Toast.makeText(ServiceManagementActivity.this, "Error al cargar servicios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editService(Service service) {
        Intent intent = new Intent(this, CreateServiceActivity.class);
        intent.putExtra("serviceId", service.getServiceId());
        createServiceLauncher.launch(intent);
    }

    private void deleteService(Service service) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Servicio")
                .setMessage("¿Estás seguro de que deseas eliminar el servicio \"" + service.getName() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    firestoreManager.deleteService(service.getServiceId(), new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            Toast.makeText(ServiceManagementActivity.this, "Servicio eliminado", Toast.LENGTH_SHORT).show();
                            loadServices();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(ServiceManagementActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
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

    // ==================== ADAPTER ====================

    private class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_service_management, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Service service = serviceList.get(position);

            holder.tvServiceName.setText(service.getName());
            holder.tvServiceDescription.setText(service.getDescription());
            holder.tvServicePrice.setText(String.format("S/. %.2f", service.getPrice()));

            // Cargar imagen
            if (service.getImageUrls() != null && !service.getImageUrls().isEmpty()) {
                Glide.with(ServiceManagementActivity.this)
                        .load(service.getImageUrls().get(0))
                        .placeholder(R.drawable.ic_image)
                        .centerCrop()
                        .into(holder.ivServiceImage);
            } else {
                holder.ivServiceImage.setImageResource(R.drawable.ic_image);
            }

            holder.btnEdit.setOnClickListener(v -> editService(service));
            holder.btnDelete.setOnClickListener(v -> deleteService(service));
            holder.itemView.setOnClickListener(v -> editService(service));
        }

        @Override
        public int getItemCount() {
            return serviceList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivServiceImage;
            TextView tvServiceName, tvServiceDescription, tvServicePrice;
            ImageButton btnEdit, btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivServiceImage = itemView.findViewById(R.id.iv_service_image);
                tvServiceName = itemView.findViewById(R.id.tv_service_name);
                tvServiceDescription = itemView.findViewById(R.id.tv_service_description);
                tvServicePrice = itemView.findViewById(R.id.tv_service_price);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}

