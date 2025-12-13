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
import com.example.droidtour.utils.NavigationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.Map;

public class ClientRegistrationPhotoActivity extends AppCompatActivity {

    private ImageView ivProfilePhoto;
    private TextView tvFullName, tvEmail, tvRegresar;
    private MaterialButton btnSiguiente;
    private FloatingActionButton fabEditPhoto;

    private Uri photoUri;
    private boolean isGoogleUser = false;
    private String userEmail, userName, userPhoto, userType;

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
        setContentView(R.layout.activity_client_registration_photo);

        initializeViews();
        loadUserData();     // ← LUEGO cargar datos normales (solo si no hay Google)
        handleGoogleUser(); // ← PRIMERO manejar datos de Google
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

    private void loadUserData() {
        // Solo cargar datos normales si NO es usuario de Google
        if (!isGoogleUser) {
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
    }

    private void handleGoogleUser() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("googleUser", false)) {
            isGoogleUser = true;
            userEmail = extras.getString("userEmail", "");
            userName = extras.getString("userName", "");
            userPhoto = extras.getString("userPhoto", "");
            userType = extras.getString("userType", "");

            // DEBUG: Verificar qué datos estamos recibiendo
            android.util.Log.d("GooglePhoto", "Datos recibidos - userPhoto: " + userPhoto + ", userName: " + userName + ", userEmail: " + userEmail);

            // PRIMERO: Usar datos del formulario (nombres y apellidos)
            String nombres = getIntent().getStringExtra("nombres");
            String apellidos = getIntent().getStringExtra("apellidos");
            if (nombres != null && apellidos != null && !nombres.isEmpty() && !apellidos.isEmpty()) {
                tvFullName.setText(nombres + " " + apellidos);
            }
            // SEGUNDO: Si no hay datos del formulario, usar los de Google
            else if (!userName.isEmpty()) {
                tvFullName.setText(userName);
            }

            // PRIMERO: Usar correo del formulario
            String correoFormulario = getIntent().getStringExtra("correo");
            if (correoFormulario != null && !correoFormulario.isEmpty()) {
                tvEmail.setText(correoFormulario);
            }
            // SEGUNDO: Si no hay correo del formulario, usar el de Google
            else if (!userEmail.isEmpty()) {
                tvEmail.setText(userEmail);
            }

            // Cargar foto de Google si existe (aquí sí priorizamos Google)
            if (userPhoto != null && !userPhoto.isEmpty()) {
                android.util.Log.d("GooglePhoto", "Cargando foto de Google: " + userPhoto);
                Glide.with(this)
                        .load(userPhoto)
                        .transform(new CircleCrop())
                        .into(ivProfilePhoto);
            } else {
                android.util.Log.d("GooglePhoto", "No hay URL de foto de Google o está vacía");
            }
        } else {
            android.util.Log.d("GooglePhoto", "No es usuario de Google");
        }
    }

    private void setupClickListeners() {
        tvRegresar.setOnClickListener(v -> handleBackNavigation());

        fabEditPhoto.setOnClickListener(v -> showPhotoOptions());

        btnSiguiente.setOnClickListener(v -> {
            if (isGoogleUser) {
                // Mostrar diálogo para confirmar si usar la foto de Google o cambiarla
                String[] options = {"Usar foto actual", "Cambiar foto", "Cancelar"};
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Foto de perfil");
                builder.setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Usar la foto actual (Google o ya seleccionada) y completar registro
                            completeGoogleRegistration();
                            break;
                        case 1:
                            // Cambiar foto
                            showPhotoOptions();
                            break;
                        case 2:
                        default:
                            dialog.dismiss();
                            break;
                    }
                });
                builder.show();
            } else {
                // Para registro normal, ir a crear contraseña
                Intent intent = new Intent(this, ClientCreatePasswordActivity.class);

                // Pasar datos del formulario anterior
                Intent previousIntent = getIntent();
                intent.putExtra("nombres", previousIntent.getStringExtra("nombres"));
                intent.putExtra("apellidos", previousIntent.getStringExtra("apellidos"));
                intent.putExtra("correo", previousIntent.getStringExtra("correo"));
                intent.putExtra("tipoDocumento", previousIntent.getStringExtra("tipoDocumento"));
                intent.putExtra("numeroDocumento", previousIntent.getStringExtra("numeroDocumento"));
                intent.putExtra("fechaNacimiento", previousIntent.getStringExtra("fechaNacimiento"));
                intent.putExtra("telefono", previousIntent.getStringExtra("telefono"));

                // Pasar la foto si fue seleccionada
                if (photoUri != null) {
                    intent.putExtra("photoUri", photoUri.toString());
                }

                startActivity(intent);
            }
        });
    }

    private void completeGoogleRegistration() {
        // Mostrar progreso
        btnSiguiente.setEnabled(false);
        Toast.makeText(this, "Completando registro...", Toast.LENGTH_SHORT).show();

        // Obtener datos del formulario
        Intent previousIntent = getIntent();
        String nombres = previousIntent.getStringExtra("nombres");
        String apellidos = previousIntent.getStringExtra("apellidos");
        String tipoDocumento = previousIntent.getStringExtra("tipoDocumento");
        String numeroDocumento = previousIntent.getStringExtra("numeroDocumento");
        String fechaNacimiento = previousIntent.getStringExtra("fechaNacimiento");
        String telefono = previousIntent.getStringExtra("telefono");

        // Combinar nombre completo
        String fullName = (nombres != null && apellidos != null) ?
                nombres + " " + apellidos : userName;

        // Obtener el usuario actual de Firebase Auth
        com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            // Preparar datos adicionales del formulario
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("firstName", nombres);
            additionalData.put("lastName", apellidos);
            additionalData.put("fullName", fullName);
            additionalData.put("documentType", tipoDocumento);
            additionalData.put("documentNumber", numeroDocumento);
            additionalData.put("birthDate", fechaNacimiento);
            additionalData.put("phone", telefono);
            additionalData.put("profileCompleted", true);
            additionalData.put("profileCompletedAt", new java.util.Date());

            // Determinar qué foto usar
            String finalPhotoUrl;
            if (photoUri != null) {
                // Si el usuario cambió la foto, usar la URI local
                finalPhotoUrl = photoUri.toString();
                additionalData.put("customPhoto", true);
            } else {
                finalPhotoUrl = userPhoto;
                additionalData.put("customPhoto", false);
            }

            // Guardar en Firestore
            com.example.droidtour.utils.FirebaseUtils.saveGoogleUserToFirestore(
                    firebaseUser,
                    "CLIENT", // Siempre será CLIENT en este flujo
                    additionalData
            );

            // Guardar en PreferencesManager para la sesión actual
            saveToPreferencesManager(firebaseUser.getUid(), fullName, firebaseUser.getEmail(), telefono, "CLIENT");

            Toast.makeText(this, "¡Registro completado exitosamente!", Toast.LENGTH_SHORT).show();

            // Redirigir al dashboard del cliente
            redirectToMainActivity();

        } else {
            btnSiguiente.setEnabled(true);
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToPreferencesManager(String userId, String fullName, String email, String phone, String userType) {
        com.example.droidtour.utils.PreferencesManager prefsManager =
                new com.example.droidtour.utils.PreferencesManager(this);

        prefsManager.saveUserData(userId, fullName, email, phone, userType);
        prefsManager.guardarUltimoLogin(System.currentTimeMillis());
        prefsManager.marcarPrimeraVezCompletada();
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(this, com.example.droidtour.client.ClientMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
}