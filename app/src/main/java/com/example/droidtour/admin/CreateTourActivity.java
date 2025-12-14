package com.example.droidtour.admin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Tour;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class CreateTourActivity extends AppCompatActivity {
    
    private static final String TAG = "CreateTourActivity";
    private static final int REQ_LOCATIONS = 101;

    private TextInputEditText etTourName, etTourDescription, etTourPrice, etTourDuration;
    private TextInputEditText etStartDate, etEndDate;
    private MaterialButton btnAddLocation, btnAddImages;
    private ExtendedFloatingActionButton btnSave;
    private RecyclerView rvLocations, rvTourImages;
    private CheckBox cbBreakfast, cbLunch, cbDinner, cbTransport;
    // ivTourPreview y progressBar son opcionales (pueden no existir en el layout)
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private FirestoreManager firestoreManager;
    
    private List<String> selectedLanguages = new ArrayList<>();
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();
    private String mainImageUrl = null;
    private String currentUserId;
    private String currentCompanyId;
    
    // Launcher para seleccionar imágenes
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
        
        // Validar que el usuario sea ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("COMPANY_ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_create_tour);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        // Inicializar Firebase
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = prefsManager.getUserId();
        
        // Obtener companyId del usuario (si es COMPANY_ADMIN)
        loadCompanyId();
        
        setupToolbar();
        initializeViews();
        setupImagePicker();
        setupClickListeners();
        setupRecyclerView();
        setupLanguageChips();
    }
    
    private void loadCompanyId() {
        // Obtener el companyId del usuario actual desde Firestore
        firestoreManager.getUserById(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    Log.d(TAG, "CompanyId cargado: " + currentCompanyId);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar companyId", e);
            }
        });
    }
    
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    
                    // Verificar si se seleccionaron múltiples imágenes
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count && selectedImageUris.size() < 5; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            selectedImageUris.add(imageUri);
                        }
                    } else if (data.getData() != null) {
                        // Una sola imagen
                        selectedImageUris.add(data.getData());
                    }
                    
                    Toast.makeText(this, selectedImageUris.size() + " imagen(es) seleccionada(s)", 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void initializeViews() {
        etTourName = findViewById(R.id.et_tour_name);
        etTourDescription = findViewById(R.id.et_tour_description);
        etTourPrice = findViewById(R.id.et_tour_price);
        etTourDuration = findViewById(R.id.et_tour_duration);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        
        btnAddLocation = findViewById(R.id.btn_add_location);
        btnAddImages = findViewById(R.id.btn_add_images);
        btnSave = findViewById(R.id.btn_save_tour);
        
        rvLocations = findViewById(R.id.rv_locations);
        rvTourImages = findViewById(R.id.rv_tour_images);

        cbBreakfast = findViewById(R.id.cb_breakfast);
        cbLunch = findViewById(R.id.cb_lunch);
        cbDinner = findViewById(R.id.cb_dinner);
        cbTransport = findViewById(R.id.cb_transport);
    }
    
    private void setupClickListeners() {
        if (etStartDate != null) etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        else Log.w(TAG, "etStartDate es null");

        if (etEndDate != null) etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        else Log.w(TAG, "etEndDate es null");

        if (btnAddLocation != null) {
            btnAddLocation.setOnClickListener(v -> openMap());
        } else {
            Log.w(TAG, "btnAddLocation es null");
        }
        
        // Botón para agregar imágenes
        if (btnAddImages != null) {
            btnAddImages.setOnClickListener(v -> openImagePicker());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (validateInputs()) {
                    saveTour();
                }
            });
        } else {
            Log.w(TAG, "btnSave es null");
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
    
    private void setupRecyclerView() {
        if (rvLocations != null) {
            rvLocations.setLayoutManager(new LinearLayoutManager(this));
        } else {
            Log.w(TAG, "rvLocations es null");
        }
        // TODO: Configurar adapter para lista de ubicaciones
    }
    
    private void setupLanguageChips() {
        // El layout actual usa MaterialCheckBox con ids cb_spanish, cb_english, cb_french, cb_portuguese
        CompoundButton cbSpanish = findViewById(R.id.cb_spanish);
        CompoundButton cbEnglish = findViewById(R.id.cb_english);
        CompoundButton cbFrench = findViewById(R.id.cb_french);
        CompoundButton cbPortuguese = findViewById(R.id.cb_portuguese);

        if (cbSpanish != null) {
            cbSpanish.setOnCheckedChangeListener((buttonView, isChecked) -> updateLanguageSelection("Español", isChecked));
            // Si está marcado por defecto, agregar a la lista
            if (cbSpanish.isChecked() && !selectedLanguages.contains("Español")) selectedLanguages.add("Español");
        }

        if (cbEnglish != null) {
            cbEnglish.setOnCheckedChangeListener((buttonView, isChecked) -> updateLanguageSelection("Inglés", isChecked));
        }

        if (cbFrench != null) {
            cbFrench.setOnCheckedChangeListener((buttonView, isChecked) -> updateLanguageSelection("Francés", isChecked));
        }

        if (cbPortuguese != null) {
            cbPortuguese.setOnCheckedChangeListener((buttonView, isChecked) -> updateLanguageSelection("Portugués", isChecked));
        }
    }
    
    private void updateLanguageSelection(String language, boolean isSelected) {
        if (isSelected) {
            if (!selectedLanguages.contains(language)) {
                selectedLanguages.add(language);
            }
        } else {
            selectedLanguages.remove(language);
        }
    }
    
    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                if (editText != null) {
                    editText.setText(date);
                } else {
                    Log.w(TAG, "editText pasado a showDatePicker es null");
                }
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }
    
    private boolean validateInputs() {
        if (etTourName == null || etTourName.getText() == null || etTourName.getText().toString().trim().isEmpty()) {
            if (etTourName != null) etTourName.setError("Ingrese el nombre del tour");
            return false;
        }
        
        if (etTourDescription == null || etTourDescription.getText() == null || etTourDescription.getText().toString().trim().isEmpty()) {
            if (etTourDescription != null) etTourDescription.setError("Ingrese la descripción");
            return false;
        }
        
        if (etTourPrice == null || etTourPrice.getText() == null || etTourPrice.getText().toString().trim().isEmpty()) {
            if (etTourPrice != null) etTourPrice.setError("Ingrese el precio");
            return false;
        }
        
        if (etTourDuration == null || etTourDuration.getText() == null || etTourDuration.getText().toString().trim().isEmpty()) {
            if (etTourDuration != null) etTourDuration.setError("Ingrese la duración");
            return false;
        }
        
        if (etStartDate == null || etStartDate.getText() == null || etStartDate.getText().toString().trim().isEmpty()) {
            if (etStartDate != null) etStartDate.setError("Seleccione fecha de inicio");
            return false;
        }
        
        if (etEndDate == null || etEndDate.getText() == null || etEndDate.getText().toString().trim().isEmpty()) {
            if (etEndDate != null) etEndDate.setError("Seleccione fecha de fin");
            return false;
        }
        
        if (selectedLanguages.isEmpty()) {
            Toast.makeText(this, "Seleccione al menos un idioma", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void saveTour() {
        // Deshabilitar botón mientras guarda
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Guardando...");
        }
        
        String tourName = etTourName.getText().toString().trim();
        
        // Si hay imágenes seleccionadas, subirlas primero
        if (!selectedImageUris.isEmpty()) {
            uploadImagesAndSaveTour(tourName);
        } else {
            // Sin imágenes, guardar directamente
            saveTourToFirestore(tourName, null);
        }
    }
    
    private void uploadImagesAndSaveTour(String tourName) {
        uploadedImageUrls.clear();
        uploadNextImage(tourName, 0);
    }
    
    private void uploadNextImage(String tourName, int index) {
        if (index >= selectedImageUris.size()) {
            // Todas las imágenes subidas, guardar el tour
            String mainImage = uploadedImageUrls.isEmpty() ? null : uploadedImageUrls.get(0);
            saveTourToFirestore(tourName, mainImage);
            return;
        }
        
        Uri imageUri = selectedImageUris.get(index);
        
        // Crear nombre de archivo: tours_images/nombre_del_tour_1.jpg
        String cleanTourName = tourName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String fileName = cleanTourName + "_" + (index + 1) + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
        
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("tours_images/" + fileName);
        
        Log.d(TAG, "Subiendo imagen " + (index + 1) + "/" + selectedImageUris.size() + ": " + fileName);
        
        imageRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                // Obtener URL de descarga
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    uploadedImageUrls.add(downloadUri.toString());
                    Log.d(TAG, "Imagen subida: " + downloadUri.toString());
                    
                    // Subir la siguiente imagen
                    uploadNextImage(tourName, index + 1);
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al subir imagen", e);
                Toast.makeText(this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                
                // Continuar con la siguiente imagen aunque falle esta
                uploadNextImage(tourName, index + 1);
            })
            .addOnProgressListener(snapshot -> {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                Log.d(TAG, "Progreso de subida: " + progress + "%");
            });
    }
    
    private void saveTourToFirestore(String tourName, String mainImage) {
        // Obtener datos del formulario
        String description = etTourDescription.getText().toString().trim();
        double price = Double.parseDouble(etTourPrice.getText().toString().trim());
        String duration = etTourDuration.getText().toString().trim();
        
        // Crear objeto Tour
        Tour tour = new Tour();
        tour.setTourName(tourName);
        tour.setDescription(description);
        tour.setPricePerPerson(price);
        tour.setDuration(duration + " horas");
        tour.setCompanyId(currentCompanyId);
        tour.setLanguages(selectedLanguages);
        tour.setMainImageUrl(mainImage);
        tour.setImageUrls(uploadedImageUrls.isEmpty() ? null : uploadedImageUrls);
        tour.setActive(true);
        tour.setFeatured(false);
        tour.setAverageRating(0.0);
        tour.setTotalReviews(0);
        tour.setTotalBookings(0);
        
        // Servicios incluidos
        List<String> includedServices = new ArrayList<>();
        if (cbBreakfast != null && cbBreakfast.isChecked()) includedServices.add("Desayuno");
        if (cbLunch != null && cbLunch.isChecked()) includedServices.add("Almuerzo");
        if (cbDinner != null && cbDinner.isChecked()) includedServices.add("Cena");
        if (cbTransport != null && cbTransport.isChecked()) includedServices.add("Transporte");
        tour.setIncludedServices(includedServices);
        
        // Guardar en Firestore
        firestoreManager.createTour(tour, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(CreateTourActivity.this, "✅ Tour creado exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            }
            
            @Override
            public void onFailure(Exception e) {
                if (btnSave != null) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Guardar Tour");
                }
                Toast.makeText(CreateTourActivity.this, "❌ Error al crear tour: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error al crear tour", e);
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
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }



    private void openMap() {
        Intent intent = new Intent(this, TourLocationsMapActivity.class);
        startActivityForResult(intent, REQ_LOCATIONS);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_LOCATIONS && resultCode == RESULT_OK) {
            ArrayList<TourLocation> locations =
                    data.getParcelableArrayListExtra("locations");

            // Actualiza UI resumen
        }
    }





}
