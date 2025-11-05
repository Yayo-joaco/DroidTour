package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.droidtour.database.DatabaseHelper;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;
import java.util.List;

public class GuideProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvDocumentType, tvDocumentNumber, tvPhone;
    private TextView tvLanguages;
    private TextView tvToursCount, tvRating, tvMemberSince, tvStatLabel1;
    private CardView cardLanguages;
    private FloatingActionButton fabEdit;
    private ImageButton btnEditPhoto;
    
    private PreferencesManager prefsManager;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myprofile);

        // Inicializar helpers
        prefsManager = new PreferencesManager(this);
        dbHelper = new DatabaseHelper(this);

        setupToolbar();
        initializeViews();
        loadUserData();
        loadStatistics();
        setupClickListeners();
        
        // Mostrar sección de idiomas para guía
        showLanguagesSection();
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
        
        // Idiomas
        tvLanguages = findViewById(R.id.tv_languages);
        
        // Estadísticas
        tvToursCount = findViewById(R.id.tv_tours_count);
        tvRating = findViewById(R.id.tv_rating);
        tvMemberSince = findViewById(R.id.tv_member_since);
        tvStatLabel1 = findViewById(R.id.tv_stat_label_1);
        
        // Sección de idiomas (para mostrar)
        cardLanguages = findViewById(R.id.card_languages);
        
        // FAB
        fabEdit = findViewById(R.id.fab_edit);
    }

    private void loadUserData() {
        if (!prefsManager.isLoggedIn()) {
            Toast.makeText(this, "No hay sesión activa", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Verificar que el usuario sea un guía
        String userType = prefsManager.getUserType();
        if (userType == null || !userType.equals("GUIDE")) {
            // Si no es guía, inicializar datos del guía
            prefsManager.saveUserData(
                "GUIDE001", 
                "Carlos Mendoza", 
                "guia@tours.com", 
                "987654321", 
                "GUIDE"
            );
            prefsManager.setGuideApproved(true);
            prefsManager.setGuideRating(4.8f);
            userType = "GUIDE";
        }

        // Cargar datos del usuario (guía)
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();
        String userPhone = prefsManager.getUserPhone();

        // Si los datos no son de Carlos Mendoza, reemplazarlos con datos correctos del guía
        if (!userName.equals("Carlos Mendoza") && (userName.equals("María López") || 
            userName.equals("Ana García Rodríguez") || userName.equals("Gabrielle Ivonne")) && userType.equals("GUIDE")) {
            prefsManager.saveUserData(
                "GUIDE001", 
                "Carlos Mendoza", 
                "guia@tours.com", 
                "987654321", 
                "GUIDE"
            );
            prefsManager.setGuideApproved(true);
            prefsManager.setGuideRating(4.8f);
            userName = "Carlos Mendoza";
            userEmail = "guia@tours.com";
            userPhone = "987654321";
        }
        
        // Asegurar que el email sea el correcto del guía
        if (!userEmail.equals("guia@tours.com") && userType.equals("GUIDE")) {
            prefsManager.saveUserData(
                "GUIDE001", 
                userName, 
                "guia@tours.com", 
                userPhone, 
                "GUIDE"
            );
            userEmail = "guia@tours.com";
        }

        // Actualizar header
        tvUserName.setText(userName);
        tvUserEmail.setText(userEmail);
        
        // Actualizar rol - siempre mostrar "GUIA DE TURISMO" en mayúsculas
        tvUserRole.setText("GUIA DE TURISMO");

        // Actualizar información personal
        tvDocumentType.setText("DNI");
        tvDocumentNumber.setText("87654321"); // Diferente al del cliente
        tvPhone.setText(userPhone != null && !userPhone.isEmpty() ? userPhone : "+51 987 654 321");
        
        // Cargar idiomas (por ahora valor por defecto)
        tvLanguages.setText("Español, Inglés, Francés"); // TODO: Agregar idiomas a PreferencesManager
    }

    private void loadStatistics() {
        // Cargar tours del guía desde la base de datos
        List<DatabaseHelper.Tour> allTours = dbHelper.getAllTours();
        
        // Tours Guiados: contar todos los tours (pueden ser completados, programados, en progreso)
        int toursCount = allTours.size();
        tvToursCount.setText(String.valueOf(toursCount));
        
        // Cambiar etiqueta para guía
        if (tvStatLabel1 != null) {
            tvStatLabel1.setText("Tours\nGuiados");
        }
        
        // Valoración: usar el rating del guía desde PreferencesManager
        float guideRating = prefsManager.getGuideRating();
        if (guideRating > 0) {
            tvRating.setText(String.format("%.1f", guideRating));
        } else {
            // Valor por defecto si no hay rating
            tvRating.setText("4.8");
        }
        
        // Miembro desde (año actual por defecto)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        tvMemberSince.setText(String.valueOf(currentYear));
    }

    private void showLanguagesSection() {
        // Mostrar sección de idiomas para guía
        if (cardLanguages != null) {
            cardLanguages.setVisibility(View.VISIBLE);
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

