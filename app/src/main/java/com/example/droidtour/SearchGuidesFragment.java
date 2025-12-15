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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragmento para buscar guías disponibles y enviarles propuestas
 */
public class SearchGuidesFragment extends Fragment {
    
    private static final String TAG = "SearchGuidesFragment";
    private TextInputEditText etSearchGuide;
    private ChipGroup chipGroupLanguages;
    private RecyclerView rvAvailableGuides;
    private View layoutEmptyGuides;
    private Set<String> selectedLanguages = new HashSet<>();
    private FirestoreManager firestoreManager;
    private PreferencesManager prefsManager;
    private String currentCompanyId;
    private String currentCompanyName;
    private List<GuideWithUser> availableGuides = new ArrayList<>();
    private List<GuideWithUser> filteredGuides = new ArrayList<>();
    private List<Tour> companyTours = new ArrayList<>();
    private List<TourOffer> activeOffers = new ArrayList<>();
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
        etSearchGuide = view.findViewById(R.id.et_search);
        chipGroupLanguages = view.findViewById(R.id.chip_group_languages);
        rvAvailableGuides = view.findViewById(R.id.recycler_guides);
        layoutEmptyGuides = view.findViewById(R.id.layout_empty);
        
        // Búsqueda en tiempo real
        if (etSearchGuide != null) {
            etSearchGuide.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterGuides();
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
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
                    loadCompanyTours();
                    loadActiveOffers();
                    loadAvailableGuides();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
            }
        });
    }
    
    private void loadCompanyTours() {
        firestoreManager.getAllToursByCompany(currentCompanyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                companyTours = (List<Tour>) result;
                Log.d(TAG, "Tours cargados: " + companyTours.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando tours", e);
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
        // Cargar todos los guías aprobados
        firestoreManager.getAllGuides(new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Guide> allGuides = (List<Guide>) result;
                availableGuides.clear();
                
                // Cargar info de usuario para cada guía
                for (Guide guide : allGuides) {
                    if (guide.getApproved() != null && guide.getApproved()) {
                        // Verificar si el guía ya tiene un tour activo
                        if (!hasActiveTour(guide.getGuideId())) {
                            loadGuideUserInfo(guide);
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando guías", e);
                showEmptyState(true);
            }
        });
    }
    
    private boolean hasActiveTour(String guideId) {
        for (TourOffer offer : activeOffers) {
            if (guideId.equals(offer.getGuideId()) && "ACEPTADA".equals(offer.getStatus())) {
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
                    GuideWithUser guideWithUser = new GuideWithUser(guide, user);
                    availableGuides.add(guideWithUser);
                    filterGuides();
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando info de guía", e);
            }
        });
    }
    
    private void filterGuides() {
        String searchText = etSearchGuide != null ? etSearchGuide.getText().toString().toLowerCase().trim() : "";
        filteredGuides.clear();
        
        for (GuideWithUser gwu : availableGuides) {
            boolean matchesSearch = searchText.isEmpty() || 
                (gwu.user.getPersonalData() != null && 
                 gwu.user.getPersonalData().getFullName().toLowerCase().contains(searchText));
            
            boolean matchesLanguage = selectedLanguages.isEmpty() || 
                (gwu.guide.getLanguages() != null && 
                 hasCommonLanguage(gwu.guide.getLanguages(), selectedLanguages));
            
            if (matchesSearch && matchesLanguage) {
                filteredGuides.add(gwu);
            }
        }
        
        adapter.updateData(filteredGuides);
        showEmptyState(filteredGuides.isEmpty());
    }
    
    private boolean hasCommonLanguage(List<String> guideLanguages, Set<String> selectedLangs) {
        for (String lang : selectedLangs) {
            for (String gLang : guideLanguages) {
                if (gLang.equalsIgnoreCase(lang) || gLang.contains(lang) || lang.contains(gLang)) {
                    return true;
                }
            }
        }
        return false;
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
        MaterialButton btnSend = dialog.findViewById(R.id.btn_send_proposal);
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
            
            // Idiomas
            if (gwu.guide.getLanguages() != null && !gwu.guide.getLanguages().isEmpty()) {
                holder.tvLanguages.setText(String.join(", ", gwu.guide.getLanguages()));
            } else {
                holder.tvLanguages.setText("No especificado");
            }
            
            // Foto
            String profileUrl = gwu.user.getPersonalData() != null ? 
                gwu.user.getPersonalData().getProfileImageUrl() : null;
            if (profileUrl != null) {
                Glide.with(holder.itemView.getContext())
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_person)
                    .into(holder.ivGuidePhoto);
            }
            
            holder.btnSendProposal.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGuideClick(gwu);
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return guides != null ? guides.size() : 0;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvGuideName, tvLanguages;
            ImageView ivGuidePhoto;
            MaterialButton btnSendProposal;
            
            ViewHolder(View view) {
                super(view);
                tvGuideName = view.findViewById(R.id.tv_guide_name);
                tvLanguages = view.findViewById(R.id.tv_languages);
                ivGuidePhoto = view.findViewById(R.id.iv_guide_photo);
                btnSendProposal = view.findViewById(R.id.btn_send_proposal);
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
            holder.tvTourDate.setText(tour.getTourDate() + " - " + tour.getStartTime());
            
            holder.itemView.setSelected(position == selectedPosition);
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
            TextView tvTourName, tvTourDate;
            
            ViewHolder(View view) {
                super(view);
                tvTourName = view.findViewById(R.id.tv_tour_name);
                tvTourDate = view.findViewById(R.id.tv_tour_date);
            }
        }
    }
}
