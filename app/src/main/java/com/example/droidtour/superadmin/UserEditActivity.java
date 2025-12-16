package com.example.droidtour.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import com.example.droidtour.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserEditActivity extends AppCompatActivity {

    private static final String TAG = "UserEditActivity";
    public static final String EXTRA_USER_ID = "user_id";

    // Vistas comunes
    private TextInputEditText etFirstName, etLastName, etPhone, etEmail;
    private TextInputLayout tilFirstName, tilLastName, tilPhone, tilEmail;
    private CountryCodePicker ccpPhone;
    private androidx.cardview.widget.CardView layoutCompanyInfo;
    private androidx.cardview.widget.CardView layoutLanguages;
    private ChipGroup chipGroupLanguages;
    private RecyclerView rvAvailableLanguages;
    private TextInputEditText etSearchLanguage;
    private ExtendedFloatingActionButton fabSave;

    // Vistas para información de empresa (solo lectura)
    private TextView tvBusinessName, tvRuc, tvCommercialName, tvBusinessType;

    // Datos
    private String userId;
    private User currentUser;
    private Company userCompany;
    private FirestoreManager firestoreManager;
    private FirebaseFirestore db;

    // Idiomas
    private List<String> availableLanguageCodes = new ArrayList<>();
    private List<String> selectedLanguageCodes = new ArrayList<>();
    private LanguageAdapter languageAdapter;
    
    // Valores originales para detectar cambios
    private String originalFirstName;
    private String originalLastName;
    private String originalPhone;
    private List<String> originalSelectedLanguages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Obtener userId del intent
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestoreManager = FirestoreManager.getInstance();
        db = FirebaseFirestore.getInstance();

        setupToolbar();
        initViews();
        loadUserData();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Editar Usuario");
        }
    }

    private void initViews() {
        // Campos editables
        tilFirstName = findViewById(R.id.til_first_name);
        tilLastName = findViewById(R.id.til_last_name);
        tilPhone = findViewById(R.id.til_phone);
        tilEmail = findViewById(R.id.til_email);

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        ccpPhone = findViewById(R.id.ccp_phone);
        
        // Configurar país por defecto (Perú) antes de registrar el EditText
        ccpPhone.setDefaultCountryUsingNameCode("PE");
        ccpPhone.resetToDefaultCountry();
        
        // Registrar el EditText del teléfono con el CountryCodePicker
        ccpPhone.registerCarrierNumberEditText(etPhone);
        setupPhoneFormatter();

        // Secciones condicionales
        layoutCompanyInfo = findViewById(R.id.layout_company_info);
        layoutLanguages = findViewById(R.id.layout_languages);
        chipGroupLanguages = findViewById(R.id.chip_group_selected_languages);
        rvAvailableLanguages = findViewById(R.id.rv_available_languages);
        etSearchLanguage = findViewById(R.id.et_search_language);

        // Información de empresa (solo lectura)
        tvBusinessName = findViewById(R.id.tv_business_name);
        tvRuc = findViewById(R.id.tv_ruc);
        tvCommercialName = findViewById(R.id.tv_commercial_name);
        tvBusinessType = findViewById(R.id.tv_business_type);

        // Botón guardar (inicialmente deshabilitado)
        fabSave = findViewById(R.id.fab_save);
        fabSave.setEnabled(false);
        updateFabAppearance(false); // Establecer apariencia inicial deshabilitada
        fabSave.setOnClickListener(v -> saveChanges());
        
        // Agregar listeners para detectar cambios
        setupChangeListeners();

        // Configurar RecyclerView de idiomas
        rvAvailableLanguages.setLayoutManager(new LinearLayoutManager(this));
        rvAvailableLanguages.setNestedScrollingEnabled(false); // Deshabilitar scroll anidado para que el NestedScrollView padre maneje todo
        rvAvailableLanguages.setHasFixedSize(false); // Permitir que el RecyclerView ajuste su tamaño según el contenido
        setupLanguages();
    }

    private void setupChangeListeners() {
        // Listener para nombre
        etFirstName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkForChanges();
            }
        });

        // Listener para apellido
        etLastName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkForChanges();
            }
        });

        // Para teléfono, el formateador ya tiene un TextWatcher
        // Agregamos otro listener que se ejecute después del formateador
        // Usamos un pequeño delay para asegurar que el formateador termine primero
        etPhone.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                // Esperar un poco para que el formateador termine
                etPhone.postDelayed(() -> checkForChanges(), 150);
            }
        });
        
        // Listener para cambio de país en CountryCodePicker
        ccpPhone.setOnCountryChangeListener(() -> {
            checkForChanges();
        });
    }

    private void checkForChanges() {
        boolean hasChanges = false;

        // Comparar nombre
        String currentFirstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        if (!currentFirstName.equals(originalFirstName != null ? originalFirstName : "")) {
            hasChanges = true;
        }

        // Comparar apellido
        String currentLastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        if (!currentLastName.equals(originalLastName != null ? originalLastName : "")) {
            hasChanges = true;
        }

        // Comparar teléfono (sin espacios para comparación)
        String currentPhone = etPhone.getText() != null ? etPhone.getText().toString().trim().replaceAll("\\s+", "") : "";
        String originalPhoneClean = originalPhone != null ? originalPhone.replaceAll("\\s+", "") : "";
        if (!currentPhone.equals(originalPhoneClean)) {
            hasChanges = true;
        }

        // Comparar idiomas (solo si es GUIDE)
        if ("GUIDE".equals(currentUser != null ? currentUser.getUserType() : null)) {
            // Comparar listas de idiomas
            if (selectedLanguageCodes.size() != originalSelectedLanguages.size()) {
                hasChanges = true;
            } else {
                // Comparar contenido de las listas
                List<String> currentSorted = new ArrayList<>(selectedLanguageCodes);
                List<String> originalSorted = new ArrayList<>(originalSelectedLanguages);
                java.util.Collections.sort(currentSorted);
                java.util.Collections.sort(originalSorted);
                if (!currentSorted.equals(originalSorted)) {
                    hasChanges = true;
                }
            }
        }

        // Habilitar o deshabilitar botón según haya cambios
        fabSave.setEnabled(hasChanges);
        updateFabAppearance(hasChanges);
    }
    
    private void updateFabAppearance(boolean enabled) {
        if (enabled) {
            // Botón habilitado: color primario normal
            fabSave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            fabSave.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            // Botón deshabilitado: color más claro/blanco
            fabSave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.fab_disabled_background));
            fabSave.setTextColor(ContextCompat.getColor(this, R.color.fab_disabled_text));
        }
    }

    private void setupPhoneFormatter() {
        etPhone.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) return;

                isFormatting = true;
                String text = s.toString().replaceAll("\\s+", ""); // Remover espacios
                
                // Solo permitir dígitos
                text = text.replaceAll("[^0-9]", "");
                
                // Formatear con espacios cada 3 dígitos
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < text.length(); i++) {
                    if (i > 0 && i % 3 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(text.charAt(i));
                }
                
                // Actualizar el texto
                int cursorPosition = etPhone.getSelectionStart();
                int lengthBefore = s.length();
                s.clear();
                s.append(formatted.toString());
                
                // Ajustar la posición del cursor
                int lengthAfter = formatted.length();
                int cursorOffset = lengthAfter - lengthBefore;
                int newCursorPosition = Math.max(0, Math.min(formatted.length(), cursorPosition + cursorOffset));
                etPhone.setSelection(newCursorPosition);
                
                isFormatting = false;
            }
        });
    }

    /**
     * Formatea un número de teléfono local agregando espacios cada 3 dígitos
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < digitsOnly.length(); i++) {
            if (i > 0 && i % 3 == 0) {
                formatted.append(" ");
            }
            formatted.append(digitsOnly.charAt(i));
        }
        return formatted.toString();
    }

    private void setupLanguages() {
        // Lista de idiomas disponibles (mismo que en GuideRegistrationLanguagesActivity)
        availableLanguageCodes.add("en");
        availableLanguageCodes.add("pt");
        availableLanguageCodes.add("fr");
        availableLanguageCodes.add("es");
        availableLanguageCodes.add("qu");
        availableLanguageCodes.add("de");
        availableLanguageCodes.add("it");
        availableLanguageCodes.add("ja");
        availableLanguageCodes.add("zh");
        availableLanguageCodes.add("ru");
        availableLanguageCodes.add("ko");
        availableLanguageCodes.add("ar");
        availableLanguageCodes.add("hi");

        Map<String, String> languageNames = new HashMap<>();
        languageNames.put("en", "Inglés");
        languageNames.put("pt", "Portugués");
        languageNames.put("fr", "Francés");
        languageNames.put("es", "Español");
        languageNames.put("qu", "Quechua");
        languageNames.put("de", "Alemán");
        languageNames.put("it", "Italiano");
        languageNames.put("ja", "Japonés");
        languageNames.put("zh", "Chino");
        languageNames.put("ru", "Ruso");
        languageNames.put("ko", "Coreano");
        languageNames.put("ar", "Árabe");
        languageNames.put("hi", "Hindi");

        // Map de banderas (emojis) para cada idioma
        Map<String, String> languageFlags = new HashMap<>();
        languageFlags.put("en", "🇺🇸");
        languageFlags.put("pt", "🇧🇷");
        languageFlags.put("fr", "🇫🇷");
        languageFlags.put("es", "🇪🇸");
        languageFlags.put("qu", "🇵🇪");
        languageFlags.put("de", "🇩🇪");
        languageFlags.put("it", "🇮🇹");
        languageFlags.put("ja", "🇯🇵");
        languageFlags.put("zh", "🇨🇳");
        languageFlags.put("ru", "🇷🇺");
        languageFlags.put("ko", "🇰🇷");
        languageFlags.put("ar", "🇸🇦");
        languageFlags.put("hi", "🇮🇳");

        languageAdapter = new LanguageAdapter(availableLanguageCodes, languageNames, languageFlags, selectedLanguageCodes, this::onLanguageToggled);
        rvAvailableLanguages.setAdapter(languageAdapter);

        // Configurar búsqueda de idiomas
        etSearchLanguage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                languageAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void onLanguageToggled(String languageCode, boolean selected) {
        if (selected) {
            if (!selectedLanguageCodes.contains(languageCode)) {
                selectedLanguageCodes.add(languageCode);
                addLanguageChip(languageCode);
            }
        } else {
            selectedLanguageCodes.remove(languageCode);
            removeLanguageChip(languageCode);
        }
        // Verificar cambios después de modificar idiomas
        checkForChanges();
    }

    private void addLanguageChip(String languageCode) {
        Chip chip = new Chip(this);
        String languageName = getLanguageName(languageCode);
        chip.setText(languageName);
        chip.setTag(languageCode);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            selectedLanguageCodes.remove(languageCode);
            chipGroupLanguages.removeView(chip);
            languageAdapter.notifyItemChanged(availableLanguageCodes.indexOf(languageCode));
            checkForChanges();
        });
        chipGroupLanguages.addView(chip);
    }

    private void removeLanguageChip(String languageCode) {
        for (int i = 0; i < chipGroupLanguages.getChildCount(); i++) {
            View child = chipGroupLanguages.getChildAt(i);
            if (child instanceof Chip && languageCode.equals(child.getTag())) {
                chipGroupLanguages.removeView(child);
                break;
            }
        }
    }

    private String getLanguageName(String code) {
        Map<String, String> names = new HashMap<>();
        names.put("en", "Inglés");
        names.put("pt", "Portugués");
        names.put("fr", "Francés");
        names.put("es", "Español");
        names.put("qu", "Quechua");
        names.put("de", "Alemán");
        names.put("it", "Italiano");
        names.put("ja", "Japonés");
        names.put("zh", "Chino");
        names.put("ru", "Ruso");
        names.put("ko", "Coreano");
        names.put("ar", "Árabe");
        names.put("hi", "Hindi");
        return names.getOrDefault(code, code.toUpperCase());
    }

    private void loadUserData() {
        // Cargar usuario desde Firestore
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                currentUser = (User) result;
                if (currentUser != null) {
                    populateUserData();
                    configureFieldsByUserType();
                    
                    // Cargar datos adicionales según el tipo
                    if ("GUIDE".equals(currentUser.getUserType())) {
                        loadGuideLanguages();
                    } else if ("COMPANY_ADMIN".equals(currentUser.getUserType())) {
                        loadCompanyInfo();
                    }
                } else {
                    Toast.makeText(UserEditActivity.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
                Toast.makeText(UserEditActivity.this, "Error cargando datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void populateUserData() {
        if (currentUser == null) return;

        // Email (solo lectura)
        etEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        etEmail.setEnabled(false);

        // PersonalData
        if (currentUser.getPersonalData() != null) {
            User.PersonalData pd = currentUser.getPersonalData();
            
            String firstName = pd.getFirstName() != null ? pd.getFirstName() : "";
            String lastName = pd.getLastName() != null ? pd.getLastName() : "";
            
            // Guardar valores originales
            originalFirstName = firstName;
            originalLastName = lastName;
            
            etFirstName.setText(firstName);
            etLastName.setText(lastName);
            
            // Teléfono
            String phone = pd.getPhoneNumber();
            if (phone != null && !phone.isEmpty()) {
                if (phone.startsWith("+")) {
                    try {
                        // Extraer solo el número local (sin código de país)
                        // El formato puede ser "+51 962137991" o "+51962137991"
                        String localNumber = extractLocalPhoneNumber(phone);
                        
                        // Si se pudo extraer el número local correctamente, configurar el CountryCodePicker
                        if (localNumber != null && !localNumber.isEmpty() && !localNumber.startsWith("+")) {
                            // Construir el número completo para el CountryCodePicker
                            String countryCode = ccpPhone.getSelectedCountryCodeWithPlus();
                            String fullNumberForPicker = countryCode + localNumber;
                            ccpPhone.setFullNumber(fullNumberForPicker);
                            
                            // Formatear y mostrar solo el número local
                            String formattedPhone = formatPhoneNumber(localNumber);
                            etPhone.setText(formattedPhone);
                            // Guardar número original (sin formatear para comparación)
                            originalPhone = localNumber.replaceAll("\\s+", "");
                            Log.d(TAG, "Número cargado - Original: " + phone + ", Local extraído: " + localNumber);
                        } else {
                            // Si no se pudo extraer correctamente, intentar con setFullNumber directamente
                            ccpPhone.setFullNumber(phone);
                            // Intentar extraer de nuevo después de configurar el picker
                            localNumber = extractLocalPhoneNumber(phone);
                            if (localNumber != null && !localNumber.startsWith("+")) {
                                String formattedPhone = formatPhoneNumber(localNumber);
                                etPhone.setText(formattedPhone);
                                originalPhone = localNumber.replaceAll("\\s+", "");
                            } else {
                                // Fallback: mostrar el número completo sin formatear
                                Log.w(TAG, "No se pudo extraer número local, mostrando número completo: " + phone);
                                etPhone.setText(phone);
                                originalPhone = phone.replaceAll("\\s+", "");
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error procesando número de teléfono: " + e.getMessage(), e);
                        // Si falla, intentar extraer manualmente
                        String localNumber = extractLocalPhoneNumber(phone);
                        if (localNumber != null && !localNumber.startsWith("+")) {
                            etPhone.setText(formatPhoneNumber(localNumber));
                            originalPhone = localNumber.replaceAll("\\s+", "");
                        } else {
                            etPhone.setText(formatPhoneNumber(phone));
                            originalPhone = phone.replaceAll("\\s+", "");
                        }
                    }
                } else {
                    // Si no empieza con +, es probablemente solo el número local
                    etPhone.setText(formatPhoneNumber(phone));
                    originalPhone = phone.replaceAll("\\s+", "");
                }
            } else {
                originalPhone = "";
            }
        }
        
        // Después de cargar todos los datos, el botón debe estar deshabilitado
        // (no hay cambios al inicio porque acabamos de cargar los datos originales)
        fabSave.setEnabled(false);
        updateFabAppearance(false);
    }
    
    /**
     * Extrae el número local de teléfono (sin código de país) de un número completo
     * Maneja diferentes formatos: "+51 962137991", "+51962137991", etc.
     * 
     * @param fullPhoneNumber Número completo con código de país (ej: "+51 962137991" o "+51962137991")
     * @return Número local sin código de país (ej: "962137991") o null si no se puede extraer
     */
    private String extractLocalPhoneNumber(String fullPhoneNumber) {
        if (fullPhoneNumber == null || fullPhoneNumber.isEmpty()) {
            return null;
        }
        
        // Remover espacios al inicio y final
        String phone = fullPhoneNumber.trim();
        
        // Si no empieza con +, asumir que ya es un número local
        if (!phone.startsWith("+")) {
            // Remover todos los caracteres no numéricos y devolver
            return phone.replaceAll("[^0-9]", "");
        }
        
        // Si tiene espacio, tomar lo que está después del espacio
        if (phone.contains(" ")) {
            String localNumber = phone.substring(phone.indexOf(" ") + 1).trim();
            // Remover caracteres no numéricos y devolver
            return localNumber.replaceAll("[^0-9]", "");
        }
        
        // Si no tiene espacio, intentar extraer el código de país
        // Lista de códigos de país comunes para Perú y países vecinos
        String[] countryCodes = {"+51", "+1", "+52", "+54", "+55", "+56", "+57", "+58", "+591", "+592", "+593", "+594", "+595", "+596", "+597", "+598", "+599"};
        
        for (String code : countryCodes) {
            if (phone.startsWith(code) && phone.length() > code.length()) {
                String localNumber = phone.substring(code.length()).trim();
                // Validar que el número local tenga al menos 6 dígitos y máximo 15
                String digitsOnly = localNumber.replaceAll("[^0-9]", "");
                if (digitsOnly.length() >= 6 && digitsOnly.length() <= 15) {
                    return digitsOnly;
                }
            }
        }
        
        // Si no se encontró un código de país conocido, intentar con el código del CountryCodePicker
        try {
            String countryCode = ccpPhone.getSelectedCountryCodeWithPlus();
            if (countryCode != null && phone.startsWith(countryCode) && phone.length() > countryCode.length()) {
                String localNumber = phone.substring(countryCode.length()).trim();
                String digitsOnly = localNumber.replaceAll("[^0-9]", "");
                if (digitsOnly.length() >= 6 && digitsOnly.length() <= 15) {
                    return digitsOnly;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error obteniendo código de país del picker: " + e.getMessage());
        }
        
        // Fallback: si el número tiene más de 3 caracteres después del +, 
        // asumir que los primeros 2-3 dígitos son el código de país
        // Para Perú (+51), el número local debería tener 9 dígitos
        if (phone.length() > 4) {
            // Intentar con +51 (Perú) que es el más común
            if (phone.startsWith("+51") && phone.length() >= 12) {
                // +51 tiene 3 caracteres, el número local debería ser de 9 dígitos
                String localNumber = phone.substring(3).trim();
                String digitsOnly = localNumber.replaceAll("[^0-9]", "");
                if (digitsOnly.length() == 9) {
                    return digitsOnly;
                }
            }
            // Intentar con códigos de 2 dígitos (ej: +1, +52, etc.)
            if (phone.length() >= 10) {
                // Asumir que los primeros 2 dígitos después del + son el código de país
                String localNumber = phone.substring(3).trim(); // +XX = 3 caracteres
                String digitsOnly = localNumber.replaceAll("[^0-9]", "");
                if (digitsOnly.length() >= 6 && digitsOnly.length() <= 15) {
                    return digitsOnly;
                }
            }
        }
        
        // Si no se pudo extraer, devolver null
        Log.w(TAG, "No se pudo extraer número local de: " + fullPhoneNumber);
        return null;
    }

    private void initializeOriginalPhone(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            // Guardar el número local sin formatear para comparación
            if (phoneNumber.startsWith("+")) {
                String localNumber = extractLocalPhoneNumber(phoneNumber);
                if (localNumber != null && !localNumber.startsWith("+")) {
                    originalPhone = localNumber.replaceAll("\\s+", "");
                } else {
                    originalPhone = phoneNumber.replaceAll("\\s+", "");
                }
            } else {
                originalPhone = phoneNumber.replaceAll("\\s+", "");
            }
        } else {
            originalPhone = "";
        }
    }

    private void configureFieldsByUserType() {
        String userType = currentUser.getUserType();
        
        // Ocultar todas las secciones condicionales primero
        if (layoutCompanyInfo != null) layoutCompanyInfo.setVisibility(View.GONE);
        if (layoutLanguages != null) layoutLanguages.setVisibility(View.GONE);

        // Configurar según el tipo
        if ("GUIDE".equals(userType)) {
            // Mostrar sección de idiomas
            if (layoutLanguages != null) layoutLanguages.setVisibility(View.VISIBLE);
        } else if ("COMPANY_ADMIN".equals(userType)) {
            // Mostrar información de empresa (solo lectura)
            if (layoutCompanyInfo != null) layoutCompanyInfo.setVisibility(View.VISIBLE);
        }
        // CLIENT no tiene secciones adicionales
    }

    private void loadGuideLanguages() {
        db.collection(FirestoreManager.COLLECTION_USER_ROLES)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        List<String> languages = extractLanguagesFromDocument(doc.getData());
                        
                        if (languages != null && !languages.isEmpty()) {
                            selectedLanguageCodes.clear();
                            selectedLanguageCodes.addAll(languages);
                            
                            // Guardar idiomas originales
                            originalSelectedLanguages.clear();
                            originalSelectedLanguages.addAll(languages);
                            
                            // Actualizar chips y adapter
                            updateLanguageUI();
                        } else {
                            // Si no se encontraron en user_roles, intentar desde guides
                            loadLanguagesFromGuides();
                        }
                    } else {
                        // Si no existe el documento en user_roles, intentar desde guides
                        loadLanguagesFromGuides();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error cargando idiomas desde user_roles: " + e.getMessage());
                    // Fallback: intentar desde guides
                    loadLanguagesFromGuides();
                });
    }

    /**
     * Extrae la lista de idiomas de diferentes estructuras posibles del documento
     */
    private List<String> extractLanguagesFromDocument(Map<String, Object> data) {
        if (data == null) return null;

        List<String> languages = null;

        // 1. Intentar obtener languages directamente
        if (data.containsKey("languages")) {
            Object langObj = data.get("languages");
            if (langObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> langList = (List<String>) langObj;
                return langList;
            }
        }

        // 2. Intentar desde estructura guide.languages
        if (data.containsKey("guide")) {
            Object guideObj = data.get("guide");
            if (guideObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> guideMap = (Map<String, Object>) guideObj;
                Object langObj = guideMap.get("languages");
                if (langObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> langList = (List<String>) langObj;
                    return langList;
                }
            }
        }

        // 3. Intentar desde estructura roles.guide.languages
        if (data.containsKey("roles")) {
            Object rolesObj = data.get("roles");
            if (rolesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rolesMap = (Map<String, Object>) rolesObj;
                Object guideRole = rolesMap.get("guide");
                if (guideRole instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> guideMap = (Map<String, Object>) guideRole;
                    Object langObj = guideMap.get("languages");
                    if (langObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> langList = (List<String>) langObj;
                        return langList;
                    }
                }
            }
        }

        return languages;
    }

    /**
     * Fallback: cargar idiomas desde la colección guides
     */
    private void loadLanguagesFromGuides() {
        db.collection(FirestoreManager.COLLECTION_GUIDES)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Object langObj = doc.get("languages");
                        if (langObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> languages = (List<String>) langObj;
                            
                            if (languages != null && !languages.isEmpty()) {
                                selectedLanguageCodes.clear();
                                selectedLanguageCodes.addAll(languages);
                                
                                // Guardar idiomas originales
                                originalSelectedLanguages.clear();
                                originalSelectedLanguages.addAll(languages);
                                
                                updateLanguageUI();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error cargando idiomas desde guides: " + e.getMessage());
                });
    }

    /**
     * Actualiza la UI con los idiomas seleccionados (chips y adapter)
     */
    private void updateLanguageUI() {
        // Limpiar chips existentes
        chipGroupLanguages.removeAllViews();
        
        // Agregar chips para cada idioma seleccionado
        for (String langCode : selectedLanguageCodes) {
            addLanguageChip(langCode);
        }
        
        // Actualizar el adapter para reflejar los checkboxes seleccionados
        if (languageAdapter != null) {
            languageAdapter.notifyDataSetChanged();
        }
        
        // Verificar cambios después de actualizar UI
        checkForChanges();
    }

    private void loadCompanyInfo() {
        if (currentUser.getCompanyId() == null || currentUser.getCompanyId().isEmpty()) {
            return;
        }

        firestoreManager.getCompanyById(currentUser.getCompanyId(), new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                userCompany = (Company) result;
                if (userCompany != null) {
                    tvBusinessName.setText(userCompany.getBusinessName() != null ? userCompany.getBusinessName() : "-");
                    tvRuc.setText(userCompany.getRuc() != null ? userCompany.getRuc() : "-");
                    tvCommercialName.setText(userCompany.getCommercialName() != null ? userCompany.getCommercialName() : "-");
                    tvBusinessType.setText(userCompany.getBusinessType() != null ? userCompany.getBusinessType() : "-");
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "Error cargando información de empresa: " + e.getMessage());
            }
        });
    }

    private void saveChanges() {
        if (!validateFields()) {
            return;
        }

        fabSave.setEnabled(false);
        fabSave.setText("Guardando...");
        updateFabAppearance(false);

        // Crear usuario actualizado
        User updatedUser = createUpdatedUser();
        
        // Log para debug: verificar que el teléfono se actualizó
        if (updatedUser != null && updatedUser.getPersonalData() != null) {
            Log.d(TAG, "Guardando usuario con teléfono: " + updatedUser.getPersonalData().getPhoneNumber());
        }

        // Guardar usuario
        firestoreManager.upsertUser(updatedUser, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Si es GUIDE, actualizar idiomas
                if ("GUIDE".equals(currentUser.getUserType())) {
                    updateGuideLanguages();
                } else {
                    finishWithSuccess();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error guardando usuario", e);
                Toast.makeText(UserEditActivity.this, "Error guardando cambios: " + e.getMessage(), Toast.LENGTH_LONG).show();
                fabSave.setEnabled(true);
                fabSave.setText("Guardar Cambios");
                updateFabAppearance(true);
            }
        });
    }

    private User createUpdatedUser() {
        User updated = new User();
        updated.setUserId(currentUser.getUserId());
        updated.setEmail(currentUser.getEmail());
        updated.setUserType(currentUser.getUserType());
        updated.setStatus(currentUser.getStatus());
        updated.setCreatedAt(currentUser.getCreatedAt());
        updated.setCompanyId(currentUser.getCompanyId());

        // Actualizar PersonalData
        User.PersonalData pd = new User.PersonalData();
        if (currentUser.getPersonalData() != null) {
            // Mantener datos existentes
            pd.setFirstName(etFirstName.getText().toString().trim());
            pd.setLastName(etLastName.getText().toString().trim());
            pd.setFullName(pd.getFirstName() + " " + pd.getLastName());
            
            // Actualizar teléfono
            String fullPhone = getFullPhoneNumber();
            if (fullPhone != null && !fullPhone.isEmpty()) {
                pd.setPhoneNumber(fullPhone);
            } else if (currentUser.getPersonalData() != null) {
                // Si está vacío, mantener el teléfono original
                pd.setPhoneNumber(currentUser.getPersonalData().getPhoneNumber());
            }
            
            // Mantener otros campos
            pd.setDocumentType(currentUser.getPersonalData().getDocumentType());
            pd.setDocumentNumber(currentUser.getPersonalData().getDocumentNumber());
            pd.setDateOfBirth(currentUser.getPersonalData().getDateOfBirth());
            pd.setProfileImageUrl(currentUser.getPersonalData().getProfileImageUrl());
        } else {
            // Si no existe PersonalData, crear uno nuevo
            pd.setFirstName(etFirstName.getText().toString().trim());
            pd.setLastName(etLastName.getText().toString().trim());
            pd.setFullName(pd.getFirstName() + " " + pd.getLastName());
            
            // Actualizar teléfono
            String fullPhone = getFullPhoneNumber();
            pd.setPhoneNumber(fullPhone);
        }

        updated.setPersonalData(pd);
        return updated;
    }

    private void updateGuideLanguages() {
        Map<String, Object> extraFields = new HashMap<>();
        extraFields.put("languages", selectedLanguageCodes);

        firestoreManager.saveUserRole(userId, "GUIDE", currentUser.getStatus(), extraFields, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                finishWithSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error actualizando idiomas", e);
                Toast.makeText(UserEditActivity.this, "Usuario actualizado pero error en idiomas: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finishWithSuccess();
            }
        });
    }

    private void finishWithSuccess() {
        Toast.makeText(this, "Usuario actualizado exitosamente", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private boolean validateFields() {
        boolean isValid = true;

        String firstName = etFirstName.getText().toString().trim();
        if (TextUtils.isEmpty(firstName)) {
            tilFirstName.setError("Los nombres son obligatorios");
            isValid = false;
        } else if (firstName.length() > 40) {
            tilFirstName.setError("Los nombres no pueden exceder 40 caracteres");
            isValid = false;
        } else {
            tilFirstName.setError(null);
        }

        String lastName = etLastName.getText().toString().trim();
        if (TextUtils.isEmpty(lastName)) {
            tilLastName.setError("Los apellidos son obligatorios");
            isValid = false;
        } else if (lastName.length() > 40) {
            tilLastName.setError("Los apellidos no pueden exceder 40 caracteres");
            isValid = false;
        } else {
            tilLastName.setError(null);
        }

        String phone = etPhone.getText().toString().trim();
        if (!phone.isEmpty()) {
            // Validar que el número tenga exactamente 9 dígitos (solo el número local, sin código de país)
            String telefonoSinEspacios = phone.replaceAll("\\s+", "");
            if (telefonoSinEspacios.length() != 9) {
                tilPhone.setError("El número de teléfono debe tener 9 dígitos");
                isValid = false;
            } else if (!telefonoSinEspacios.matches("\\d{9}")) {
                tilPhone.setError("El número de teléfono solo debe contener dígitos");
                isValid = false;
            } else {
                tilPhone.setError(null);
            }
        } else {
            tilPhone.setError(null);
        }

        return isValid;
    }

    /**
     * Obtener número de teléfono completo con código de país
     */
    private String getFullPhoneNumber() {
        String phoneText = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        if (phoneText.isEmpty()) {
            Log.d(TAG, "Número de teléfono vacío");
            return null;
        }

        // Si el texto ya contiene el código de país (empieza con +), devolverlo directamente
        if (phoneText.startsWith("+")) {
            Log.d(TAG, "Número ya incluye código de país: " + phoneText);
            return phoneText;
        }

        try {
            // Intentar obtener el número completo del CountryCodePicker
            String fullNumber = ccpPhone.getFullNumberWithPlus();
            if (fullNumber != null && !fullNumber.isEmpty()) {
                Log.d(TAG, "Número obtenido del CountryCodePicker: " + fullNumber);
                return fullNumber;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error obteniendo número completo del CountryCodePicker: " + e.getMessage());
        }

        // Fallback: construir manualmente combinando código de país y número local
        try {
            String countryCode = ccpPhone.getSelectedCountryCodeWithPlus();
            String constructedNumber = countryCode + phoneText;
            Log.d(TAG, "Número construido manualmente: " + constructedNumber);
            return constructedNumber;
        } catch (Exception e) {
            Log.e(TAG, "Error construyendo número manualmente: " + e.getMessage());
            // Último fallback: devolver solo el texto si contiene números
            return phoneText;
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

    // Adapter para lista de idiomas
    private static class LanguageAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {
        private List<String> allLanguages;
        private List<String> filteredLanguages;
        private List<String> selectedLanguages;
        private Map<String, String> languageNames;
        private Map<String, String> languageFlags;
        private OnLanguageToggleListener listener;

        interface OnLanguageToggleListener {
            void onToggle(String code, boolean selected);
        }

        LanguageAdapter(List<String> allLanguages, Map<String, String> languageNames, 
                       Map<String, String> languageFlags, List<String> selectedLanguages, 
                       OnLanguageToggleListener listener) {
            this.allLanguages = allLanguages;
            this.filteredLanguages = new ArrayList<>(allLanguages);
            this.selectedLanguages = selectedLanguages;
            this.languageNames = languageNames;
            this.languageFlags = languageFlags;
            this.listener = listener;
        }

        void filter(String query) {
            filteredLanguages.clear();
            if (query == null || query.isEmpty()) {
                filteredLanguages.addAll(allLanguages);
            } else {
                String lowerQuery = query.toLowerCase();
                for (String code : allLanguages) {
                    String name = languageNames.get(code);
                    if (name != null && name.toLowerCase().contains(lowerQuery)) {
                        filteredLanguages.add(code);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_language, parent, false);
            return new LanguageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
            String languageCode = filteredLanguages.get(position);
            String languageName = languageNames.get(languageCode);
            String flagEmoji = languageFlags != null ? languageFlags.get(languageCode) : "🏳️";
            boolean isSelected = selectedLanguages.contains(languageCode);

            // Configurar bandera
            holder.tvFlagEmoji.setText(flagEmoji != null ? flagEmoji : "🏳️");
            // Configurar nombre del idioma
            holder.tvLanguageName.setText(languageName != null ? languageName : languageCode.toUpperCase());
            // Configurar checkbox
            holder.checkbox.setChecked(isSelected);
            
            holder.itemView.setOnClickListener(v -> {
                boolean newState = !isSelected;
                holder.checkbox.setChecked(newState);
                if (listener != null) {
                    listener.onToggle(languageCode, newState);
                }
            });
        }

        @Override
        public int getItemCount() {
            return filteredLanguages.size();
        }

        static class LanguageViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView tvFlagEmoji;
            TextView tvLanguageName;
            com.google.android.material.checkbox.MaterialCheckBox checkbox;

            LanguageViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFlagEmoji = itemView.findViewById(R.id.tvFlagEmoji);
                tvLanguageName = itemView.findViewById(R.id.tvLanguageName);
                checkbox = itemView.findViewById(R.id.cbSelected);
            }
        }
    }
}
