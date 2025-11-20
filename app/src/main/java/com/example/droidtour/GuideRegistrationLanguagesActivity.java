package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuideRegistrationLanguagesActivity extends AppCompatActivity {

    private TextInputEditText etBuscarIdioma;
    private ChipGroup chipGroupSelectedLanguages;
    private RecyclerView rvLanguages;
    private MaterialButton btnSiguiente;
    private TextView tvRegresar;

    private LanguageAdapter languageAdapter;
    private List<Language> allLanguages = new ArrayList<>();
    private List<Language> filteredLanguages = new ArrayList<>();
    private List<Language> selectedLanguages = new ArrayList<>();

    // Variables para datos del usuario
    private boolean isGoogleUser = false;
    private String userEmail, userName, userPhoto, userType;
    private String nombres, apellidos, tipoDocumento, numeroDocumento, fechaNacimiento, telefono;
    private String photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_registration_languages);

        initViews();
        loadUserData();
        setupLanguages();
        setupRecyclerView();
        setupSearchFilter();
        setupBackButton();
        setupNextButton();
    }

    private void initViews() {
        etBuscarIdioma = findViewById(R.id.etBuscarIdioma);
        chipGroupSelectedLanguages = findViewById(R.id.chipGroupSelectedLanguages);
        rvLanguages = findViewById(R.id.rvLanguages);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        tvRegresar = findViewById(R.id.tvRegresar);
    }

    private void loadUserData() {
        Intent intent = getIntent();
        isGoogleUser = intent.getBooleanExtra("googleUser", false);
        userType = intent.getStringExtra("userType");
        userEmail = intent.getStringExtra("userEmail");
        userName = intent.getStringExtra("userName");
        userPhoto = intent.getStringExtra("userPhoto");

        // Datos del formulario
        nombres = intent.getStringExtra("nombres");
        apellidos = intent.getStringExtra("apellidos");
        tipoDocumento = intent.getStringExtra("tipoDocumento");
        numeroDocumento = intent.getStringExtra("numeroDocumento");
        fechaNacimiento = intent.getStringExtra("fechaNacimiento");
        telefono = intent.getStringExtra("telefono");
        photoUri = intent.getStringExtra("photoUri");
    }

    private void setupLanguages() {
        // Agregar idiomas disponibles con emojis de banderas
        allLanguages.add(new Language("en", "Inglés", "🇺🇸"));
        allLanguages.add(new Language("pt", "Portugués", "🇧🇷"));
        allLanguages.add(new Language("fr", "Francés", "🇫🇷"));
        allLanguages.add(new Language("es", "Español", "🇪🇸"));
        allLanguages.add(new Language("qu", "Quechua", "🇵🇪"));
        allLanguages.add(new Language("de", "Alemán", "🇩🇪"));
        allLanguages.add(new Language("it", "Italiano", "🇮🇹"));
        allLanguages.add(new Language("zh", "Chino", "🇨🇳"));
        allLanguages.add(new Language("ja", "Japonés", "🇯🇵"));
        allLanguages.add(new Language("ko", "Coreano", "🇰🇷"));
        allLanguages.add(new Language("ru", "Ruso", "🇷🇺"));
        allLanguages.add(new Language("ar", "Árabe", "🇸🇦"));
        allLanguages.add(new Language("nl", "Holandés", "🇳🇱"));
        allLanguages.add(new Language("sv", "Sueco", "🇸🇪"));
        allLanguages.add(new Language("no", "Noruego", "🇳🇴"));
        allLanguages.add(new Language("da", "Danés", "🇩🇰"));
        allLanguages.add(new Language("fi", "Finlandés", "🇫🇮"));
        allLanguages.add(new Language("pl", "Polaco", "🇵🇱"));
        allLanguages.add(new Language("tr", "Turco", "🇹🇷"));
        allLanguages.add(new Language("el", "Griego", "🇬🇷"));

        filteredLanguages.addAll(allLanguages);
    }

    private void setupRecyclerView() {
        languageAdapter = new LanguageAdapter(filteredLanguages, new LanguageAdapter.OnLanguageClickListener() {
            @Override
            public void onLanguageClick(Language language) {
                toggleLanguageSelection(language);
            }
        });

        rvLanguages.setLayoutManager(new LinearLayoutManager(this));
        rvLanguages.setAdapter(languageAdapter);
    }

    private void setupSearchFilter() {
        etBuscarIdioma.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLanguages(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterLanguages(String query) {
        filteredLanguages.clear();

        if (query.isEmpty()) {
            filteredLanguages.addAll(allLanguages);
        } else {
            String searchQuery = query.toLowerCase();
            for (Language language : allLanguages) {
                if (language.getName().toLowerCase().contains(searchQuery)) {
                    filteredLanguages.add(language);
                }
            }
        }

        languageAdapter.notifyDataSetChanged();
    }

    private void toggleLanguageSelection(Language language) {
        language.setSelected(!language.isSelected());

        if (language.isSelected()) {
            selectedLanguages.add(language);
            addChip(language);
        } else {
            selectedLanguages.remove(language);
            removeChip(language);
        }

        languageAdapter.notifyDataSetChanged();
        updateNextButtonState();
    }

    private void addChip(Language language) {
        Chip chip = new Chip(this);
        chip.setText(language.getFlagEmoji() + " " + language.getName());
        chip.setCloseIconVisible(true);
        chip.setTag(language.getCode());

        chip.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLanguageSelection(language);
            }
        });

        chipGroupSelectedLanguages.addView(chip);
    }

    private void removeChip(Language language) {
        for (int i = 0; i < chipGroupSelectedLanguages.getChildCount(); i++) {
            View view = chipGroupSelectedLanguages.getChildAt(i);
            if (view instanceof Chip) {
                Chip chip = (Chip) view;
                if (language.getCode().equals(chip.getTag())) {
                    chipGroupSelectedLanguages.removeView(chip);
                    break;
                }
            }
        }
    }

    private void updateNextButtonState() {
        btnSiguiente.setEnabled(!selectedLanguages.isEmpty());
    }

    private void setupBackButton() {
        tvRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupNextButton() {
        btnSiguiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGoogleUser) {
                    completeGoogleRegistration();
                } else {
                    // Para registro normal, ir a crear contraseña
                    Intent intent = new Intent(GuideRegistrationLanguagesActivity.this, GuideCreatePasswordActivity.class);

                    // Pasar datos del formulario
                    intent.putExtra("nombres", nombres);
                    intent.putExtra("apellidos", apellidos);
                    intent.putExtra("correo", userEmail);
                    intent.putExtra("tipoDocumento", tipoDocumento);
                    intent.putExtra("numeroDocumento", numeroDocumento);
                    intent.putExtra("fechaNacimiento", fechaNacimiento);
                    intent.putExtra("telefono", telefono);

                    // Pasar idiomas seleccionados
                    ArrayList<String> idiomasSeleccionados = new ArrayList<>();
                    for (Language lang : selectedLanguages) {
                        idiomasSeleccionados.add(lang.getCode());
                    }
                    intent.putStringArrayListExtra("idiomas", idiomasSeleccionados);

                    startActivity(intent);
                }
            }
        });
    }

    private void completeGoogleRegistration() {
        btnSiguiente.setEnabled(false);
        Toast.makeText(this, "Completando registro...", Toast.LENGTH_SHORT).show();

        // Obtener el usuario actual de Firebase Auth
        com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            // Preparar lista de idiomas
            List<String> languagesList = new ArrayList<>();
            for (Language lang : selectedLanguages) {
                languagesList.add(lang.getCode());
            }

            // Combinar nombre completo
            String fullName = (nombres != null && apellidos != null) ?
                    nombres + " " + apellidos : userName;

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
            if (photoUri != null && !photoUri.isEmpty()) {
                // Si el usuario cambió la foto, usar la URI local
                finalPhotoUrl = photoUri;
                additionalData.put("customPhoto", true);
            } else {
                finalPhotoUrl = userPhoto;
                additionalData.put("customPhoto", false);
            }

            // Guardar en Firestore CON idiomas
            com.example.droidtour.utils.FirebaseUtils.saveGoogleGuideToFirestore(
                    firebaseUser,
                    "GUIDE",
                    additionalData,
                    languagesList
            );

            // Guardar en PreferencesManager para la sesión actual
            saveToPreferencesManager(firebaseUser.getUid(), fullName, firebaseUser.getEmail(), telefono, "GUIDE");

            Toast.makeText(this, "¡Registro completado exitosamente!", Toast.LENGTH_SHORT).show();

            // Redirigir a la pantalla de espera de aprobación
            redirectToApprovalPending();

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

    private void redirectToApprovalPending() {
        // Crear una actividad temporal para mostrar el mensaje de aprobación pendiente
        Intent intent = new Intent(this, GuideApprovalPendingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

// Las clases Language y LanguageAdapter se mantienen igual...

// Clase Language
class Language {
    private String code;
    private String name;
    private String flagEmoji;
    private boolean selected;

    public Language(String code, String name, String flagEmoji) {
        this.code = code;
        this.name = name;
        this.flagEmoji = flagEmoji;
        this.selected = false;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getFlagEmoji() {
        return flagEmoji;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}

// Adapter para el RecyclerView
class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {

    private List<Language> languages;
    private OnLanguageClickListener listener;

    public interface OnLanguageClickListener {
        void onLanguageClick(Language language);
    }

    public LanguageAdapter(List<Language> languages, OnLanguageClickListener listener) {
        this.languages = languages;
        this.listener = listener;
    }

    @Override
    public LanguageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LanguageViewHolder holder, int position) {
        Language language = languages.get(position);

        holder.tvFlagEmoji.setText(language.getFlagEmoji());
        holder.tvLanguageName.setText(language.getName());
        holder.cbSelected.setChecked(language.isSelected());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onLanguageClick(language);
            }
        });
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    static class LanguageViewHolder extends RecyclerView.ViewHolder {
        TextView tvFlagEmoji;
        TextView tvLanguageName;
        MaterialCheckBox cbSelected;

        public LanguageViewHolder(View itemView) {
            super(itemView);
            tvFlagEmoji = itemView.findViewById(R.id.tvFlagEmoji);
            tvLanguageName = itemView.findViewById(R.id.tvLanguageName);
            cbSelected = itemView.findViewById(R.id.cbSelected);
        }
    }
}