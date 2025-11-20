package com.example.droidtour;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.droidtour.utils.NavigationUtils;

import java.util.HashMap;
import java.util.Map;

public class GuideRegistrationPhotoActivity extends AppCompatActivity {

    private ImageView ivProfilePhoto;
    private TextView tvFullName, tvEmail, tvRegresar;
    private MaterialButton btnSiguiente;
    private FloatingActionButton fabEditPhoto;

    private Uri photoUri;
    private boolean isGoogleUser = false;
    private String userEmail, userName, userPhoto, userType;

    // Datos del formulario
    private String nombres, apellidos, tipoDocumento, numeroDocumento, fechaNacimiento, telefono, correo;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        Glide.with(this)
                                .load(imageBitmap)
                                .transform(new CircleCrop())
                                .into(ivProfilePhoto);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    photoUri = result.getData().getData();
                    Glide.with(this)
                            .load(photoUri)
                            .transform(new CircleCrop())
                            .into(ivProfilePhoto);
                }
            });

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_registration_photo);

        initializeViews();
        loadFormData(); // Cargar datos del formulario
        handleGoogleUser(); // Manejar datos de Google
        setupClickListeners();
    }

    private void initializeViews() {
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvRegresar = findViewById(R.id.tvRegresar);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        fabEditPhoto = findViewById(R.id.fabEditPhoto);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
    }

    private void loadFormData() {
        // Obtener datos del formulario anterior
        Intent intent = getIntent();
        nombres = intent.getStringExtra("nombres");
        apellidos = intent.getStringExtra("apellidos");
        correo = intent.getStringExtra("correo");
        tipoDocumento = intent.getStringExtra("tipoDocumento");
        numeroDocumento = intent.getStringExtra("numeroDocumento");
        fechaNacimiento = intent.getStringExtra("fechaNacimiento");
        telefono = intent.getStringExtra("telefono");

        // Mostrar nombre y email
        if (nombres != null && apellidos != null) {
            tvFullName.setText(nombres + " " + apellidos);
        }

        if (correo != null) {
            tvEmail.setText(correo);
        }
    }

    private void handleGoogleUser() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("googleUser", false)) {
            isGoogleUser = true;
            userEmail = extras.getString("userEmail", "");
            userName = extras.getString("userName", "");
            userPhoto = extras.getString("userPhoto", "");
            userType = extras.getString("userType", "");

            // Cargar foto de Google si existe
            if (userPhoto != null && !userPhoto.isEmpty()) {
                Glide.with(this)
                        .load(userPhoto)
                        .transform(new CircleCrop())
                        .into(ivProfilePhoto);
            }
        }
    }

    private void setupClickListeners() {
        tvRegresar.setOnClickListener(v -> handleBackNavigation());

        fabEditPhoto.setOnClickListener(v -> showPhotoOptions());

        btnSiguiente.setOnClickListener(v -> {
            // PARA AMBOS CASOS (Google y normal) IR A LENGUAJES
            proceedToLanguages();
        });
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void handleBackNavigation() {
        if (isGoogleUser) {
            NavigationUtils.navigateBackToLogin(this, true);
        } else {
            finish();
        }
    }

    private void proceedToLanguages() {
        Intent intent = new Intent(this, GuideRegistrationLanguagesActivity.class);

        if (isGoogleUser) {
            // Pasar datos de Google
            intent.putExtra("googleUser", true);
            intent.putExtra("userType", userType);
            intent.putExtra("userEmail", userEmail);
            intent.putExtra("userName", userName);
            intent.putExtra("userPhoto", userPhoto);
        } else {
            // Pasar datos de registro normal
            intent.putExtra("googleUser", false);
            intent.putExtra("userEmail", correo);
        }

        // Pasar datos del formulario (comunes para ambos)
        intent.putExtra("nombres", nombres);
        intent.putExtra("apellidos", apellidos);
        intent.putExtra("correo", correo);
        intent.putExtra("tipoDocumento", tipoDocumento);
        intent.putExtra("numeroDocumento", numeroDocumento);
        intent.putExtra("fechaNacimiento", fechaNacimiento);
        intent.putExtra("telefono", telefono);

        // Pasar la foto seleccionada (si existe)
        if (photoUri != null) {
            intent.putExtra("photoUri", photoUri.toString());
        }

        startActivity(intent);
    }

    private void showPhotoOptions() {
        String[] options = {"Tomar foto", "Elegir de galería", "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona una opción");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Tomar foto
                    checkCameraPermission();
                    break;
                case 1: // Galería
                    openGallery();
                    break;
                case 2: // Cancelar
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private void openGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pickPhotoIntent);
    }
}