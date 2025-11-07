package com.example.droidtour.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.database.DatabaseHelper;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;
import java.util.List;

public class ClientProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvDocumentType, tvDocumentNumber, tvPhone;
    private TextView tvToursCount, tvRating, tvMemberSince;
    private CardView cardLanguages;
    private FloatingActionButton fabEdit;
    private ImageButton btnEditPhoto;
    
    private PreferencesManager prefsManager;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myprofile);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Inicializar helpers
        prefsManager = new PreferencesManager(this);
        dbHelper = new DatabaseHelper(this);

        setupToolbar();
        initializeViews();
        loadUserData();
        loadStatistics();
        setupClickListeners();
        
        // Ocultar sección de idiomas para cliente
        hideLanguagesSection();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mi Perfil");
        }
    }

    private void initializeViews() {
        // Header
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvUserRole = findViewById(R.id.tv_user_role);
        btnEditPhoto = findViewById(R.id.btn_edit_photo_small);
        
        // Información personal
        tvDocumentType = findViewById(R.id.tv_document_type);
        tvDocumentNumber = findViewById(R.id.tv_document_number);
        tvPhone = findViewById(R.id.tv_phone);
        
        // Estadísticas
        tvToursCount = findViewById(R.id.tv_tours_count);
        tvRating = findViewById(R.id.tv_rating);
        tvMemberSince = findViewById(R.id.tv_member_since);
        
        // Sección de idiomas (para ocultar)
        cardLanguages = findViewById(R.id.card_languages);
        
        // FAB
        fabEdit = findViewById(R.id.fab_edit);
    }

    private void loadUserData() {
        if (!prefsManager.isLoggedIn()) {
            Toast.makeText(this, "Sesión expirada. Por favor, inicia sesión nuevamente", Toast.LENGTH_SHORT).show();
            // Redirigir al login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Verificar que el usuario sea un cliente
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("CLIENT")) {
            // Si no es cliente, inicializar datos del cliente
            prefsManager.saveUserData(
                "CLIENT001", 
                "Gabrielle Ivonne", 
                "cliente@email.com", 
                "912345678", 
                "CLIENT"
            );
            userType = "CLIENT";
        }

        // Cargar datos del usuario (cliente)
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();
        String userPhone = prefsManager.getUserPhone();

        // Si los datos no son de Gabrielle Ivonne, reemplazarlos con datos correctos del cliente
        if (!userName.equals("Gabrielle Ivonne") && (userName.equals("Carlos Mendoza") || 
            userName.equals("María López") || userName.equals("Ana García Rodríguez"))) {
            prefsManager.saveUserData(
                "CLIENT001", 
                "Gabrielle Ivonne", 
                "cliente@email.com", 
                "912345678", 
                "CLIENT"
            );
            userName = "Gabrielle Ivonne";
            userEmail = "cliente@email.com";
            userPhone = "912345678";
        }
        
        // Asegurar que el email sea el correcto
        if (!userEmail.equals("cliente@email.com") && userType.equals("CLIENT")) {
            prefsManager.saveUserData(
                "CLIENT001", 
                userName, 
                "cliente@email.com", 
                userPhone, 
                "CLIENT"
            );
            userEmail = "cliente@email.com";
        }

        // Actualizar header
        tvUserName.setText(userName);
        tvUserEmail.setText(userEmail);
        
        // Actualizar rol - siempre mostrar "CLIENTE" para esta actividad
        tvUserRole.setText("CLIENTE");

        // Actualizar información personal
        tvDocumentType.setText("DNI");
        tvDocumentNumber.setText("12345678"); // TODO: Agregar campo de documento a PreferencesManager
        tvPhone.setText(userPhone != null && !userPhone.isEmpty() ? userPhone : "+51 987 654 321");
    }

    private void loadStatistics() {
        // Cargar reservas de la base de datos
        List<DatabaseHelper.Reservation> reservations = dbHelper.getAllReservations();
        
        // Tours Reservados
        int toursCount = reservations.size();
        tvToursCount.setText(String.valueOf(toursCount));
        
        // Asegurar que la etiqueta diga "Tours Reservados" para cliente
        TextView tvStatLabel1 = findViewById(R.id.tv_stat_label_1);
        if (tvStatLabel1 != null) {
            tvStatLabel1.setText("Tours\nReservados");
        }
        
        // Valoración promedio (por ahora usar valor por defecto, ya que no hay reseñas en BD)
        // TODO: Calcular promedio de reseñas cuando esté implementado
        double avgRating = 4.8; // Valor por defecto
        tvRating.setText(String.format("%.1f", avgRating));
        
        // Miembro desde (año actual por defecto)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        tvMemberSince.setText(String.valueOf(currentYear));
    }

    private void hideLanguagesSection() {
        // Ocultar sección de idiomas para cliente
        if (cardLanguages != null) {
            cardLanguages.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // Botón editar foto
        if (btnEditPhoto != null) {
            btnEditPhoto.setOnClickListener(v -> {
                Toast.makeText(this, "Editar foto próximamente", Toast.LENGTH_SHORT).show();
            });
        }

        // FAB editar
        if (fabEdit != null) {
            fabEdit.setOnClickListener(v -> {
                Toast.makeText(this, "Edición de perfil próximamente", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

