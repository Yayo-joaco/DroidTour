package com.example.droidtour.client;

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
import com.example.droidtour.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ClientRegistrationPhotoActivity extends AppCompatActivity {

    private ImageView ivProfilePhoto;
    private TextView tvFullName, tvEmail, tvRegresar;
    private MaterialButton btnSiguiente;
    private FloatingActionButton fabEditPhoto;

    private Uri photoUri;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    assert extras != null;
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    Glide.with(this)
                        .load(imageBitmap)
                        .transform(new CircleCrop())
                        .into(ivProfilePhoto);
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
        setContentView(R.layout.activity_client_registration_photo);

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {

        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvRegresar = findViewById(R.id.tvRegresar);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        fabEditPhoto = findViewById(R.id.fabEditPhoto);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto); // Inicialización agregada
    }

    private void loadUserData() {
        // Obtener datos del Intent anterior
        Intent intent = getIntent();
        String nombres = intent.getStringExtra("nombres");
        String apellidos = intent.getStringExtra("apellidos");
        String correo = intent.getStringExtra("correo");

        if (nombres != null && apellidos != null) {
            tvFullName.setText(nombres + " " + apellidos);
        }

        if (correo != null) {
            tvEmail.setText(correo);
        }
    }

    private void setupClickListeners() {
        tvRegresar.setOnClickListener(v -> finish());

        fabEditPhoto.setOnClickListener(v -> showPhotoOptions());

        btnSiguiente.setOnClickListener(v -> {
            Intent prevIntent = getIntent();
            Intent intent = new Intent(this, ClientCreatePasswordActivity.class);
            
            // Pasar todos los datos del usuario
            intent.putExtra("nombres", prevIntent.getStringExtra("nombres"));
            intent.putExtra("apellidos", prevIntent.getStringExtra("apellidos"));
            intent.putExtra("tipoDocumento", prevIntent.getStringExtra("tipoDocumento"));
            intent.putExtra("numeroDocumento", prevIntent.getStringExtra("numeroDocumento"));
            intent.putExtra("fechaNacimiento", prevIntent.getStringExtra("fechaNacimiento"));
            intent.putExtra("correo", prevIntent.getStringExtra("correo"));
            intent.putExtra("telefono", prevIntent.getStringExtra("telefono"));
            
            // Pasar URI de la foto si existe
            if (photoUri != null) {
                intent.putExtra("photoUri", photoUri.toString());
            }
            
            startActivity(intent);
        });
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