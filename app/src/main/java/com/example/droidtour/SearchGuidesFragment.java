package com.example.droidtour;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Guide;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.TourOffer;
import com.example.droidtour.models.User;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * Fragmento para buscar guías disponibles y enviarles propuestas
 */
public class SearchGuidesFragment extends Fragment {
    
    private static final String TAG = "SearchGuidesFragment";
    private ChipGroup chipGroupLanguages;
    private RecyclerView rvAvailableGuides;
    private View layoutEmptyGuides;
    private TextView tvGuidesCount;
    private TextView tvRatingValue;
    private com.google.android.material.slider.Slider sliderRating;
    private MaterialButton btnSort;
    private MaterialButton btnClearLanguages;
    private MaterialButton btnToggleFilters;
    private com.google.android.material.card.MaterialCardView cardFilters;
    
    private Set<String> selectedLanguages = new HashSet<>();
    private float minRating = 0.0f;
    private boolean sortByRatingDesc = true; // true = mayor a menor, false = menor a mayor
    
    // Mapeo de nombres completos a códigos ISO de idiomas
    private static final Map<String, String> LANGUAGE_CODES = new HashMap<String, String>() {{
        put("Español", "es");
        put("Inglés", "en");
        put("Portugués", "pt");
        put("Francés", "fr");
        put("Quechua", "qu");
        put("Alemán", "de");
        put("Italiano", "it");
        put("Chino", "zh");
        put("Japonés", "ja");
        put("Coreano", "ko");
        put("Ruso", "ru");
        put("Árabe", "ar");
    }};
    
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;
    private String currentCompanyName;
    private List<GuideWithUser> availableGuides = new ArrayList<>();
    private List<GuideWithUser> filteredGuides = new ArrayList<>();
    private List<Tour> companyTours = new ArrayList<>();
    private List<TourOffer> activeOffers = new ArrayList<>();
    private Set<String> guidesWithTours = new HashSet<>(); // IDs de guías que ya tienen tours asignados
    private GuidesAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_guides, container, false);
        
        firestoreManager = FirestoreManager.getInstance();
        prefsManager = new PreferencesManager(requireContext());
        
        initializeViews(view);
        setupLanguageChips();
        setupRecyclerView();
        loadCompanyData();
        
        return view;
    }
    
    private void initializeViews(View view) {
        chipGroupLanguages = view.findViewById(R.id.chip_group_languages);
        rvAvailableGuides = view.findViewById(R.id.recycler_guides);
        layoutEmptyGuides = view.findViewById(R.id.layout_empty);
        tvGuidesCount = view.findViewById(R.id.tv_guides_count);
        tvRatingValue = view.findViewById(R.id.tv_rating_value);
        sliderRating = view.findViewById(R.id.slider_rating);
        btnSort = view.findViewById(R.id.btn_sort);
        btnClearLanguages = view.findViewById(R.id.btn_clear_languages);
        btnToggleFilters = view.findViewById(R.id.btn_toggle_filters);
        cardFilters = view.findViewById(R.id.card_filters);
        
        // Configurar slider de rating
        sliderRating.addOnChangeListener((slider, value, fromUser) -> {
            minRating = value;
            tvRatingValue.setText(String.format("%.1f ⭐", value));
            filterGuides();
        });
        
        // Configurar botón de ordenar
        btnSort.setOnClickListener(v -> {
            sortByRatingDesc = !sortByRatingDesc;
            btnSort.setText(sortByRatingDesc ? "Mayor Rating" : "Menor Rating");
            btnSort.setIconResource(sortByRatingDesc ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
            filterGuides();
        });
        
        // Configurar botón limpiar idiomas
        btnClearLanguages.setOnClickListener(v -> {
            selectedLanguages.clear();
            // Desmarcar todos los chips
            for (int i = 0; i < chipGroupLanguages.getChildCount(); i++) {
                View child = chipGroupLanguages.getChildAt(i);
                if (child instanceof Chip) {
                    ((Chip) child).setChecked(false);
                }
            }
            filterGuides();
        });
        
        // Configurar botón de colapsar/expandir filtros
        btnToggleFilters.setOnClickListener(v -> {
            if (cardFilters.getVisibility() == View.VISIBLE) {
                // Colapsar filtros
                cardFilters.setVisibility(View.GONE);
                btnToggleFilters.setIconResource(R.drawable.ic_expand_more);
            } else {
                // Expandir filtros
                cardFilters.setVisibility(View.VISIBLE);
                btnToggleFilters.setIconResource(R.drawable.ic_expand_less);
            }
        });
    }
    
    private void setupLanguageChips() {
        if (chipGroupLanguages == null) return;
        
        String[] languages = {"Español", "Inglés", "Portugués", "Francés", "Quechua", 
                             "Alemán", "Italiano", "Chino", "Japonés", "Coreano", "Ruso", "Árabe"};
        
        for (String lang : languages) {
            Chip chip = new Chip(getContext());
            chip.setText(lang);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.chip_background_selector);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedLanguages.add(lang);
                } else {
                    selectedLanguages.remove(lang);
                }
                filterGuides();
            });
            chipGroupLanguages.addView(chip);
        }
    }
    
    private void setupRecyclerView() {
        rvAvailableGuides.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GuidesAdapter(filteredGuides, this::showProposalDialog);
        rvAvailableGuides.setAdapter(adapter);
        // Inicialmente mostrar el RecyclerView y ocultar el empty state
        showEmptyState(false);
    }
    
    private void loadCompanyData() {
        String userId = prefsManager.getUserId();
        firestoreManager.getUserById(userId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    currentCompanyName = user.getPersonalData() != null ? 
                        user.getPersonalData().getFullName() : "Empresa";
                    // Primero cargar todos los tours para identificar guías ocupados
                    loadAllToursToCheckGuides();
                    loadCompanyTours();
                    loadActiveOffers();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
            }
        });
    }
    
    private void loadAllToursToCheckGuides() {
        Log.d(TAG, "Cargando todos los tours para identificar guías ocupados...");
        
        // Cargar TODOS los tours de Firebase para ver qué guías están asignados
        firestoreManager.getAllTours(new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> allTours = (List<Tour>) result;
                guidesWithTours.clear();
                
                // Obtener fecha actual para comparar
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                String todayStr = sdf.format(new java.util.Date());
                
                // Extraer IDs de guías que tienen tours ACTIVOS (no completados)
                for (Tour tour : allTours) {
                    if (tour.getAssignedGuideId() != null && !tour.getAssignedGuideId().isEmpty()) {
                        // Verificar si el tour ya fue completado (fecha pasada)
                        boolean tourCompletado = isTourCompleted(tour.getTourDate(), todayStr);
                        
                        if (!tourCompletado) {
                            // Solo agregar guías con tours ACTIVOS (no completados)
                            guidesWithTours.add(tour.getAssignedGuideId());
                            Log.d(TAG, "Guía ocupado: " + tour.getAssignedGuideId() + " en tour: " + tour.getTourName() + " (Fecha: " + tour.getTourDate() + ")");
                        } else {
                            Log.d(TAG, "✅ Tour COMPLETADO: " + tour.getTourName() + " (Fecha: " + tour.getTourDate() + ") - Guía " + tour.getAssignedGuideId() + " disponible");
                        }
                    }
                }
                
                Log.d(TAG, "Total de guías con tours ACTIVOS: " + guidesWithTours.size());
                
                // Ahora que tenemos la lista de guías ocupados, cargar los guías disponibles
                loadAvailableGuides();
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando todos los tours", e);
                // Aunque falle, intentar cargar guías
                loadAvailableGuides();
            }
        });
    }
    
    /**
     * Verifica si un tour ya fue completado comparando su fecha con la fecha actual
     */
    private boolean isTourCompleted(String tourDate, String todayStr) {
        if (tourDate == null || tourDate.isEmpty()) {
            return false; // Si no tiene fecha, considerarlo como activo por precaución
        }
        
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.util.Date tourDateParsed = sdf.parse(tourDate);
            java.util.Date today = sdf.parse(todayStr);
            
            // Tour completado si la fecha es anterior a hoy
            return tourDateParsed.before(today);
        } catch (Exception e) {
            Log.e(TAG, "Error parseando fecha del tour: " + tourDate, e);
            return false; // En caso de error, considerarlo activo por precaución
        }
    }
    
    private void loadCompanyTours() {
        firestoreManager.getAllToursByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                companyTours = (List<Tour>) result;
                Log.d(TAG, "Tours de la compañía cargados: " + companyTours.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando tours de la compañía", e);
            }
        });
    }
    
    private void loadActiveOffers() {
        firestoreManager.getOffersByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<TourOffer> allOffers = (List<TourOffer>) result;
                activeOffers.clear();
                // Solo ofertas pendientes o aceptadas
                for (TourOffer offer : allOffers) {
                    if ("PENDIENTE".equals(offer.getStatus()) || "ACEPTADA".equals(offer.getStatus())) {
                        activeOffers.add(offer);
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando ofertas", e);
            }
        });
    }
    
    private void loadAvailableGuides() {
        Log.d(TAG, "Iniciando carga de guías disponibles...");
        
        // Cargar todos los guías aprobados
        firestoreManager.getAllGuides(new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Guide> allGuides = (List<Guide>) result;
                Log.d(TAG, "Total de guías en Firebase: " + allGuides.size());
                
                availableGuides.clear();
                int approvedCount = 0;
                int activeUserCount = 0;
                int availableCount = 0;
                int busyCount = 0;
                
                // Cargar info de usuario para cada guía
                for (Guide guide : allGuides) {
                    // Filtro 1: Debe estar aprobado
                    if (guide.getApproved() != null && guide.getApproved()) {
                        approvedCount++;
                        
                        // Filtro 2: No debe tener tour asignado (verificar en guidesWithTours)
                        if (guidesWithTours.contains(guide.getGuideId())) {
                            busyCount++;
                            Log.d(TAG, "❌ Guía " + guide.getGuideId() + " tiene tour asignado - EXCLUIDO");
                            continue;
                        }
                        
                        // Filtro 3: Verificar que no tenga ofertas aceptadas pendientes
                        if (hasActiveTour(guide.getGuideId())) {
                            Log.d(TAG, "❌ Guía " + guide.getGuideId() + " tiene oferta aceptada - EXCLUIDO");
                            continue;
                        }
                        
                        // Si pasa todos los filtros, cargar info del usuario
                        availableCount++;
                        loadGuideUserInfo(guide);
                    }
                }
                
                Log.d(TAG, "📊 Resumen:");
                Log.d(TAG, "  - Guías aprobados: " + approvedCount);
                Log.d(TAG, "  - Guías con tours asignados: " + busyCount);
                Log.d(TAG, "  - Guías disponibles: " + availableCount);
                
                // Si no hay guías después de procesar, mostrar empty state
                if (availableCount == 0) {
                    showEmptyState(true);
                    updateGuidesCount();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando guías", e);
                Toast.makeText(requireContext(), "Error al cargar guías: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }
    
    private boolean hasActiveTour(String guideId) {
        for (TourOffer offer : activeOffers) {
            // Excluir guías con ofertas PENDIENTES o ACEPTADAS
            if (guideId.equals(offer.getGuideId()) && 
                ("PENDIENTE".equals(offer.getStatus()) || "ACEPTADA".equals(offer.getStatus()))) {
                Log.d(TAG, "❌ Guía " + guideId + " tiene oferta " + offer.getStatus() + " - EXCLUIDO");
                return true;
            }
        }
        return false;
    }
    
    private void loadGuideUserInfo(Guide guide) {
        firestoreManager.getUserById(guide.getGuideId(), new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                User user = (User) result;
                if (user != null) {
                    // Filtro final: Verificar que el usuario esté activo
                    if (user.getStatus() == null || !"active".equals(user.getStatus())) {
                        Log.d(TAG, "❌ Usuario " + guide.getGuideId() + " está inactivo (status: " + user.getStatus() + ") - EXCLUIDO");
                        return;
                    }
                    
                    String userName = user.getPersonalData() != null ? 
                        user.getPersonalData().getFullName() : "Sin nombre";
                    Log.d(TAG, "✅ Guía disponible: " + userName + " - Rating: " + guide.getRating());
                    
                    GuideWithUser guideWithUser = new GuideWithUser(guide, user);
                    availableGuides.add(guideWithUser);
                    filterGuides();
                } else {
                    Log.w(TAG, "❌ Usuario null para guía: " + guide.getGuideId());
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando info de guía " + guide.getGuideId(), e);
            }
        });
    }
    
    private void filterGuides() {
        filteredGuides.clear();
        
        for (GuideWithUser gwu : availableGuides) {
            // Filtro por rating
            float guideRating = gwu.guide.getRating() != null ? gwu.guide.getRating() : 0.0f;
            boolean matchesRating = guideRating >= minRating;
            
            // Filtro por idioma
            boolean matchesLanguage = selectedLanguages.isEmpty() || 
                (gwu.guide.getLanguages() != null && 
                 hasCommonLanguage(gwu.guide.getLanguages(), selectedLanguages));
            
            if (matchesRating && matchesLanguage) {
                filteredGuides.add(gwu);
            }
        }
        
        // Ordenar por rating
        filteredGuides.sort((g1, g2) -> {
            float rating1 = g1.guide.getRating() != null ? g1.guide.getRating() : 0.0f;
            float rating2 = g2.guide.getRating() != null ? g2.guide.getRating() : 0.0f;
            
            if (sortByRatingDesc) {
                return Float.compare(rating2, rating1); // Mayor a menor
            } else {
                return Float.compare(rating1, rating2); // Menor a mayor
            }
        });
        
        adapter.updateData(filteredGuides);
        updateGuidesCount();
        showEmptyState(filteredGuides.isEmpty());
    }
    
    private void updateGuidesCount() {
        int count = filteredGuides.size();
        String text = count + (count == 1 ? " guía encontrado" : " guías encontrados");
        if (tvGuidesCount != null) {
            tvGuidesCount.setText(text);
        }
    }
    
    private boolean hasCommonLanguage(List<String> guideLanguages, Set<String> selectedLangs) {
        // Si el guía no tiene idiomas, no cumple el filtro
        if (guideLanguages == null || guideLanguages.isEmpty()) {
            return false;
        }
        
        // Verificar si el guía habla TODOS los idiomas seleccionados (AND, no OR)
        for (String selectedLang : selectedLangs) {
            // Convertir nombre completo a código ISO
            String selectedCode = LANGUAGE_CODES.get(selectedLang);
            if (selectedCode == null) {
                selectedCode = selectedLang; // Si no está en el mapa, usar tal cual
            }
            
            boolean foundThisLanguage = false;
            
            for (String guideLang : guideLanguages) {
                // Comparación flexible: código ISO o nombre completo
                String selectedClean = selectedCode.trim().toLowerCase();
                String guideClean = guideLang.trim().toLowerCase();
                
                // Verificar si coincide el código o el nombre
                if (guideClean.equals(selectedClean) || 
                    guideClean.contains(selectedClean) || 
                    selectedClean.contains(guideClean)) {
                    foundThisLanguage = true;
                    Log.d(TAG, "✅ Idioma encontrado: Guía habla '" + guideLang + "', buscado: '" + selectedLang + "' (código: " + selectedCode + ")");
                    break;
                }
            }
            
            // Si no encontró este idioma específico, el guía no cumple el filtro
            if (!foundThisLanguage) {
                Log.d(TAG, "❌ Guía NO habla '" + selectedLang + "'. Idiomas del guía: " + guideLanguages);
                return false;
            }
        }
        
        // Si llegó aquí, el guía habla TODOS los idiomas seleccionados
        Log.d(TAG, "✅ Guía habla TODOS los idiomas requeridos. Guía: " + guideLanguages + ", Requeridos: " + selectedLangs);
        return true;
    }
    
    private void showProposalDialog(GuideWithUser guideWithUser) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_send_proposal);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        TextView tvGuideName = dialog.findViewById(R.id.tv_guide_name);
        RecyclerView rvTours = dialog.findViewById(R.id.recycler_tours);
        TextInputEditText etPaymentAmount = dialog.findViewById(R.id.et_payment);
        TextInputEditText etNotes = dialog.findViewById(R.id.et_notes);
        MaterialButton btnSend = dialog.findViewById(R.id.btn_send);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        
        String guideName = guideWithUser.user.getPersonalData() != null ? 
            guideWithUser.user.getPersonalData().getFullName() : "Guía";
        tvGuideName.setText("Enviar propuesta a " + guideName);
        
        // Setup tours selector
        TourSelectorAdapter tourAdapter = new TourSelectorAdapter(companyTours);
        rvTours.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTours.setAdapter(tourAdapter);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSend.setOnClickListener(v -> {
            Tour selectedTour = tourAdapter.getSelectedTour();
            String paymentStr = etPaymentAmount.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();
            
            if (selectedTour == null) {
                Toast.makeText(getContext(), "Seleccione un tour", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (paymentStr.isEmpty()) {
                Toast.makeText(getContext(), "Ingrese el monto de pago", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validar que el guía hable TODOS los idiomas requeridos por el tour
            if (!guideHasRequiredLanguages(guideWithUser.guide, selectedTour)) {
                String tourLanguages = selectedTour.getLanguages() != null ? 
                    String.join(", ", selectedTour.getLanguages()) : "ninguno";
                String guideLanguages = guideWithUser.guide.getLanguages() != null ? 
                    String.join(", ", guideWithUser.guide.getLanguages()) : "ninguno";
                
                Toast.makeText(getContext(), 
                    "❌ El guía no habla todos los idiomas requeridos.\n" +
                    "Tour requiere: " + tourLanguages + "\n" +
                    "Guía habla: " + guideLanguages, 
                    Toast.LENGTH_LONG).show();
                return;
            }
            
            try {
                double paymentAmount = Double.parseDouble(paymentStr);
                sendProposal(guideWithUser, selectedTour, paymentAmount, notes);
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Monto inválido", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    /**
     * Verifica si el guía habla TODOS los idiomas requeridos por el tour
     */
    private boolean guideHasRequiredLanguages(Guide guide, Tour tour) {
        // Si el tour no requiere idiomas específicos, permitir
        if (tour.getLanguages() == null || tour.getLanguages().isEmpty()) {
            return true;
        }
        
        // Si el guía no tiene idiomas, rechazar
        if (guide.getLanguages() == null || guide.getLanguages().isEmpty()) {
            return false;
        }
        
        // Verificar que el guía hable TODOS los idiomas del tour
        for (String requiredLang : tour.getLanguages()) {
            boolean hasThisLanguage = false;
            
            for (String guideLang : guide.getLanguages()) {
                String requiredClean = requiredLang.trim().toLowerCase();
                String guideClean = guideLang.trim().toLowerCase();
                
                if (guideClean.equals(requiredClean) || 
                    guideClean.contains(requiredClean) || 
                    requiredClean.contains(guideClean)) {
                    hasThisLanguage = true;
                    break;
                }
            }
            
            // Si el guía no habla este idioma requerido, fallar
            if (!hasThisLanguage) {
                Log.d(TAG, "❌ Guía no habla el idioma requerido: " + requiredLang);
                return false;
            }
        }
        
        Log.d(TAG, "✅ Guía habla todos los idiomas requeridos por el tour");
        return true;
    }
    
    private void sendProposal(GuideWithUser guideWithUser, Tour tour, double paymentAmount, String notes) {
        String guideName = guideWithUser.user.getPersonalData() != null ? 
            guideWithUser.user.getPersonalData().getFullName() : "Guía";
        
        TourOffer offer = new TourOffer(
            guideWithUser.guide.getGuideId(),
            guideName,
            currentCompanyId,
            currentCompanyName,
            tour.getTourId(),
            tour.getTourName(),
            tour.getTourDate(),
            tour.getStartTime(),
            tour.getDuration(),
            paymentAmount,
            tour.getMaxGroupSize()
        );
        offer.setAdditionalNotes(notes);
        
        Log.d(TAG, "📤 Creando propuesta:");
        Log.d(TAG, "  - CompanyId: " + currentCompanyId);
        Log.d(TAG, "  - GuideId: " + guideWithUser.guide.getGuideId());
        Log.d(TAG, "  - TourId: " + tour.getTourId());
        Log.d(TAG, "  - AgencyId (getter): " + offer.getAgencyId());
        
        firestoreManager.createTourOffer(offer, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(getContext(), "✅ Propuesta enviada correctamente", Toast.LENGTH_SHORT).show();
                loadActiveOffers();
                loadAvailableGuides(); // Recargar lista
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "❌ Error al enviar propuesta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error enviando propuesta", e);
            }
        });
    }
    
    private void showEmptyState(boolean show) {
        if (layoutEmptyGuides == null || rvAvailableGuides == null) return;
        
        if (show) {
            rvAvailableGuides.setVisibility(View.GONE);
            layoutEmptyGuides.setVisibility(View.VISIBLE);
        } else {
            rvAvailableGuides.setVisibility(View.VISIBLE);
            layoutEmptyGuides.setVisibility(View.GONE);
        }
    }
    
    // Clase auxiliar
    static class GuideWithUser {
        Guide guide;
        User user;
        
        GuideWithUser(Guide guide, User user) {
            this.guide = guide;
            this.user = user;
        }
    }
    
    // Adapter para guías disponibles
    private static class GuidesAdapter extends RecyclerView.Adapter<GuidesAdapter.ViewHolder> {
        private List<GuideWithUser> guides;
        private OnGuideClickListener listener;
        
        interface OnGuideClickListener {
            void onGuideClick(GuideWithUser guide);
        }
        
        GuidesAdapter(List<GuideWithUser> guides, OnGuideClickListener listener) {
            this.guides = guides != null ? guides : new ArrayList<>();
            this.listener = listener;
        }
        
        void updateData(List<GuideWithUser> newData) {
            this.guides = newData != null ? newData : new ArrayList<>();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_guide_search, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GuideWithUser gwu = guides.get(position);
            
            String name = gwu.user.getPersonalData() != null ? 
                gwu.user.getPersonalData().getFullName() : "Guía";
            holder.tvGuideName.setText(name);
            
            // Rating
            float rating = gwu.guide.getRating() != null ? gwu.guide.getRating() : 0.0f;
            holder.tvRating.setText(String.format("%.1f", rating));
            
            // Idiomas
            if (gwu.guide.getLanguages() != null && !gwu.guide.getLanguages().isEmpty()) {
                holder.tvLanguages.setText(String.join(", ", gwu.guide.getLanguages()));
            } else {
                holder.tvLanguages.setText("No especificado");
            }
            
            // Foto
            String profileUrl = gwu.user.getPersonalData() != null ? 
                gwu.user.getPersonalData().getProfileImageUrl() : null;
            if (profileUrl != null && !profileUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.ivGuidePhoto);
            } else {
                holder.ivGuidePhoto.setImageResource(R.drawable.ic_person);
            }
            
            holder.btnSendProposal.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGuideClick(gwu);
                }
            });
            
            // Click listener for info button
            holder.btnShowDetails.setOnClickListener(v -> {
                String guideId = gwu.guide.getGuideId();
                if (guideId != null && !guideId.isEmpty() && holder.itemView.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                    com.example.droidtour.ui.GuideDetailsBottomSheet sheet = 
                        com.example.droidtour.ui.GuideDetailsBottomSheet.newInstance(guideId);
                    sheet.show(((androidx.fragment.app.FragmentActivity) holder.itemView.getContext())
                        .getSupportFragmentManager(), "guide_details");
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return guides != null ? guides.size() : 0;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvGuideName, tvLanguages, tvRating;
            ImageView ivGuidePhoto;
            MaterialButton btnSendProposal;
            View btnShowDetails;
            
            ViewHolder(View view) {
                super(view);
                tvGuideName = view.findViewById(R.id.tv_guide_name);
                tvLanguages = view.findViewById(R.id.tv_languages);
                tvRating = view.findViewById(R.id.tv_rating);
                ivGuidePhoto = view.findViewById(R.id.img_guide);
                btnSendProposal = view.findViewById(R.id.btn_send_proposal);
                btnShowDetails = view.findViewById(R.id.btn_show_guide_details);
            }
        }
    }
    
    // Adapter para seleccionar tour
    private static class TourSelectorAdapter extends RecyclerView.Adapter<TourSelectorAdapter.ViewHolder> {
        private List<Tour> tours;
        private int selectedPosition = -1;
        
        TourSelectorAdapter(List<Tour> tours) {
            this.tours = tours != null ? tours : new ArrayList<>();
        }
        
        Tour getSelectedTour() {
            return selectedPosition >= 0 && selectedPosition < tours.size() ? 
                tours.get(selectedPosition) : null;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tour_selector, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Tour tour = tours.get(position);
            holder.tvTourName.setText(tour.getTourName());
            holder.tvTourDate.setText(tour.getTourDate() != null ? tour.getTourDate() : "");
            holder.tvTourTime.setText(tour.getStartTime() != null ? tour.getStartTime() : "");
            holder.tvTourDuration.setText(tour.getDuration() != null ? tour.getDuration() : "");
            
            boolean isSelected = position == selectedPosition;
            holder.itemView.setSelected(isSelected);
            holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            
            // Cambiar el color del borde si está seleccionado
            if (isSelected) {
                ((com.google.android.material.card.MaterialCardView) holder.itemView)
                    .setStrokeColor(holder.itemView.getContext().getColor(R.color.primary));
                ((com.google.android.material.card.MaterialCardView) holder.itemView)
                    .setStrokeWidth(4);
            } else {
                ((com.google.android.material.card.MaterialCardView) holder.itemView)
                    .setStrokeColor(holder.itemView.getContext().getColor(R.color.divider));
                ((com.google.android.material.card.MaterialCardView) holder.itemView)
                    .setStrokeWidth(2);
            }
            
            holder.itemView.setOnClickListener(v -> {
                int oldPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
            });
        }
        
        @Override
        public int getItemCount() {
            return tours != null ? tours.size() : 0;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTourName, tvTourDate, tvTourTime, tvTourDuration;
            ImageView ivCheck;
            
            ViewHolder(View view) {
                super(view);
                tvTourName = view.findViewById(R.id.tv_tour_name);
                tvTourDate = view.findViewById(R.id.tv_tour_date);
                tvTourTime = view.findViewById(R.id.tv_tour_time);
                tvTourDuration = view.findViewById(R.id.tv_tour_duration);
                ivCheck = view.findViewById(R.id.iv_check);
            }
        }
    }
}
