package com.example.droidtour.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirebaseStorageManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Service;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class CreateServiceActivity extends AppCompatActivity {
    
    private static final String TAG = "CreateServiceActivity";
    
    private TextInputEditText etServiceName, etServiceDescription, etServicePrice;
    private MaterialCardView cardServiceImage1, cardServiceImage2;
    private ImageView ivServiceImage1, ivServiceImage2;
    private LinearLayout placeholderImage1, placeholderImage2;
    private MaterialButton btnCancel, btnSave;
    
    private PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    private FirebaseStorageManager storageManager;
    
    private Uri image1Uri, image2Uri;
    private int currentImageSelection = 0; // 1 o 2
    private String companyId;
    private String serviceId; // Para edición
    private Service currentService; // Para edición
    private boolean isEditMode = false;
    
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImageSelected(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefsManager = new PreferencesManager(this);
        firestoreManager = FirestoreManager.getInstance();
        storageManager = FirebaseStorageManager.getInstance();
        
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
        
        setContentView(R.layout.activity_create_service);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        // Verificar si es modo edición
        serviceId = getIntent().getStringExtra("serviceId");
        isEditMode = serviceId != null && !serviceId.isEmpty();
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
        loadCompanyId();
        
        if (isEditMode) {
            loadServiceData();
        }
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Editar Servicio" : "Crear Servicio");
        }
    }
    
    private void initializeViews() {
        etServiceName = findViewById(R.id.et_service_name);
        etServiceDescription = findViewById(R.id.et_service_description);
        etServicePrice = findViewById(R.id.et_service_price);
        
        cardServiceImage1 = findViewById(R.id.card_service_image1);
        cardServiceImage2 = findViewById(R.id.card_service_image2);
        
        ivServiceImage1 = findViewById(R.id.iv_service_image1);
        ivServiceImage2 = findViewById(R.id.iv_service_image2);
        
        placeholderImage1 = findViewById(R.id.placeholder_image1);
        placeholderImage2 = findViewById(R.id.placeholder_image2);
        
        btnCancel = findViewById(R.id.btn_cancel_service);
        btnSave = findViewById(R.id.btn_save_service);
        
        if (isEditMode) {
            btnSave.setText("Actualizar Servicio");
        }
    }
    
    private void setupClickListeners() {
        cardServiceImage1.setOnClickListener(v -> {
            currentImageSelection = 1;
            imagePickerLauncher.launch("image/*");
        });
        
        cardServiceImage2.setOnClickListener(v -> {
            currentImageSelection = 2;
            imagePickerLauncher.launch("image/*");
        });
        
        btnCancel.setOnClickListener(v -> finish());
        
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveService();
            }
        });
    }
    
    private void handleImageSelected(Uri uri) {
        if (currentImageSelection == 1) {
            image1Uri = uri;
            ivServiceImage1.setVisibility(View.VISIBLE);
            if (placeholderImage1 != null) {
                placeholderImage1.setVisibility(View.GONE);
            }
            Glide.with(this).load(uri).centerCrop().into(ivServiceImage1);
        } else if (currentImageSelection == 2) {
            image2Uri = uri;
            ivServiceImage2.setVisibility(View.VISIBLE);
            if (placeholderImage2 != null) {
                placeholderImage2.setVisibility(View.GONE);
            }
            Glide.with(this).load(uri).centerCrop().into(ivServiceImage2);
        }
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
                    }
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error obteniendo companyId", e);
                }
            });
        }
    }
    
    private void loadServiceData() {
        firestoreManager.getServiceById(serviceId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                currentService = (Service) result;
                if (currentService != null) {
                    populateServiceData();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando servicio", e);
                Toast.makeText(CreateServiceActivity.this, "Error al cargar servicio", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void populateServiceData() {
        etServiceName.setText(currentService.getName());
        etServiceDescription.setText(currentService.getDescription());
        etServicePrice.setText(String.valueOf(currentService.getPrice()));
        
        // Cargar imágenes
        List<String> imageUrls = currentService.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            // Imagen 1
            if (imageUrls.size() > 0 && imageUrls.get(0) != null && !imageUrls.get(0).isEmpty()) {
                ivServiceImage1.setVisibility(View.VISIBLE);
                if (placeholderImage1 != null) {
                    placeholderImage1.setVisibility(View.GONE);
                }
                Glide.with(this).load(imageUrls.get(0)).centerCrop().into(ivServiceImage1);
            }
            // Imagen 2
            if (imageUrls.size() > 1 && imageUrls.get(1) != null && !imageUrls.get(1).isEmpty()) {
                ivServiceImage2.setVisibility(View.VISIBLE);
                if (placeholderImage2 != null) {
                    placeholderImage2.setVisibility(View.GONE);
                }
                Glide.with(this).load(imageUrls.get(1)).centerCrop().into(ivServiceImage2);
            }
        }
    }
    
    private boolean validateInputs() {
        if (etServiceName.getText().toString().trim().isEmpty()) {
            etServiceName.setError("Ingrese el nombre del servicio");
            return false;
        }
        
        if (etServiceDescription.getText().toString().trim().isEmpty()) {
            etServiceDescription.setError("Ingrese la descripción del servicio");
            return false;
        }
        
        if (etServicePrice.getText().toString().trim().isEmpty()) {
            etServicePrice.setError("Ingrese el precio del servicio");
            return false;
        }
        
        return true;
    }
    
    private void saveService() {
        btnSave.setEnabled(false);
        btnSave.setText("Guardando...");
        
        String name = etServiceName.getText().toString().trim();
        String description = etServiceDescription.getText().toString().trim();
        double price = Double.parseDouble(etServicePrice.getText().toString().trim());
        
        // Preparar servicio
        Service service = isEditMode ? currentService : new Service(name, description, price, companyId);
        service.setName(name);
        service.setDescription(description);
        service.setPrice(price);
        if (companyId != null) {
            service.setCompanyId(companyId);
        }
        
        // Verificar si hay imágenes que subir
        int imagesToUpload = 0;
        if (image1Uri != null) imagesToUpload++;
        if (image2Uri != null) imagesToUpload++;
        
        if (imagesToUpload == 0) {
            // No hay nuevas imágenes, guardar directamente
            saveServiceToFirestore(service);
        } else {
            // Subir imágenes primero
            uploadImagesAndSave(service);
        }
    }
    
    private void uploadImagesAndSave(Service service) {
        final List<String> imageUrls = new ArrayList<>();
        if (isEditMode && currentService.getImageUrls() != null) {
            imageUrls.addAll(currentService.getImageUrls());
        }
        
        final int[] uploadedCount = {0};
        final int[] totalToUpload = {0};
        if (image1Uri != null) totalToUpload[0]++;
        if (image2Uri != null) totalToUpload[0]++;
        
        Runnable checkComplete = () -> {
            if (uploadedCount[0] == totalToUpload[0]) {
                service.setImageUrls(imageUrls);
                saveServiceToFirestore(service);
            }
        };
        
        String tempServiceId = isEditMode ? serviceId : "service_" + System.currentTimeMillis();
        
        if (image1Uri != null) {
            storageManager.uploadServiceImage(tempServiceId, image1Uri, new FirebaseStorageManager.StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    if (imageUrls.size() > 0) {
                        imageUrls.set(0, downloadUrl);
                    } else {
                        imageUrls.add(downloadUrl);
                    }
                    uploadedCount[0]++;
                    checkComplete.run();
                }
                
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText(isEditMode ? "Actualizar Servicio" : "Guardar Servicio");
                        Toast.makeText(CreateServiceActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onProgress(int progress) {}
            });
        }
        
        if (image2Uri != null) {
            storageManager.uploadServiceImage(tempServiceId, image2Uri, new FirebaseStorageManager.StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    if (imageUrls.size() > 1) {
                        imageUrls.set(1, downloadUrl);
                    } else {
                        imageUrls.add(downloadUrl);
                    }
                    uploadedCount[0]++;
                    checkComplete.run();
                }
                
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText(isEditMode ? "Actualizar Servicio" : "Guardar Servicio");
                        Toast.makeText(CreateServiceActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onProgress(int progress) {}
            });
        }
    }
    
    private void saveServiceToFirestore(Service service) {
        if (isEditMode) {
            firestoreManager.updateService(service, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreateServiceActivity.this, "Servicio actualizado", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Actualizar Servicio");
                        Toast.makeText(CreateServiceActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            firestoreManager.createService(service, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreateServiceActivity.this, "Servicio creado exitosamente", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Guardar Servicio");
                        Toast.makeText(CreateServiceActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
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
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
