package com.example.droidtour.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Guide;
import com.example.droidtour.models.Reservation;
import com.example.droidtour.models.User;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GuideDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "GuideDetailsSheet";
    private static final String ARG_GUIDE_ID = "arg_guide_id";

    // Language code mapping
    private static final Map<String, String> LANGUAGE_NAMES = new HashMap<String, String>() {{
        put("es", "Español");
        put("en", "Inglés");
        put("fr", "Francés");
        put("de", "Alemán");
        put("it", "Italiano");
        put("pt", "Portugués");
        put("zh", "Chino");
        put("ja", "Japonés");
        put("ko", "Coreano");
        put("ru", "Ruso");
        put("ar", "Árabe");
        put("qu", "Quechua");
    }};

    public static GuideDetailsBottomSheet newInstance(@NonNull String guideId) {
        GuideDetailsBottomSheet fragment = new GuideDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_GUIDE_ID, guideId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_guide_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get views
        ImageButton btnClose = view.findViewById(R.id.btn_close);
        ImageView ivAvatar = view.findViewById(R.id.iv_guide_avatar);
        TextView tvAvatarInitial = view.findViewById(R.id.tv_avatar_initial);
        TextView tvName = view.findViewById(R.id.tv_guide_name);
        TextView tvEmail = view.findViewById(R.id.tv_guide_email);
        TextView tvPhone = view.findViewById(R.id.tv_guide_phone);
        TextView tvRegistration = view.findViewById(R.id.tv_registration_date);
        Chip chipStatus = view.findViewById(R.id.chip_guide_status);
        ChipGroup chipLanguages = view.findViewById(R.id.chip_group_languages);
        
        TextView tvBiography = view.findViewById(R.id.tv_biography);
        View cardBiography = view.findViewById(R.id.card_biography);
        
        TextView tvToursCount = view.findViewById(R.id.tv_tours_count);
        TextView tvRating = view.findViewById(R.id.tv_rating);
        TextView tvExperienceYears = view.findViewById(R.id.tv_experience_years);
        
        TextView tvSpecialties = view.findViewById(R.id.tv_specialties);
        View cardSpecialties = view.findViewById(R.id.card_specialties);

        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }

        final String guideId = args.getString(ARG_GUIDE_ID);
        if (guideId == null || guideId.isEmpty()) {
            dismiss();
            return;
        }

        // Close button
        btnClose.setOnClickListener(v -> dismiss());

        // Load guide data
        loadGuideData(guideId, ivAvatar, tvAvatarInitial, tvName, tvEmail, tvPhone, 
                     tvRegistration, chipStatus, chipLanguages, tvBiography, cardBiography,
                     tvToursCount, tvRating, tvExperienceYears, tvSpecialties, cardSpecialties);
    }

    private void loadGuideData(String guideId, ImageView ivAvatar, TextView tvAvatarInitial,
                               TextView tvName, TextView tvEmail, TextView tvPhone,
                               TextView tvRegistration, Chip chipStatus, ChipGroup chipLanguages,
                               TextView tvBiography, View cardBiography,
                               TextView tvToursCount, TextView tvRating, TextView tvExperienceYears,
                               TextView tvSpecialties, View cardSpecialties) {
        
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        
        // Load User data
        firestoreManager.getUserById(guideId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof User) {
                    User user = (User) result;
                    
                    // Name
                    String fullName = user.getFullName();
                    if (fullName != null && !fullName.isEmpty()) {
                        tvName.setText(fullName);
                        
                        // Avatar initial
                        String initial = fullName.substring(0, Math.min(2, fullName.length())).toUpperCase();
                        tvAvatarInitial.setText(initial);
                    } else {
                        tvName.setText("Sin nombre");
                        tvAvatarInitial.setText("?");
                    }
                    
                    // Email
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        tvEmail.setText(user.getEmail());
                    } else {
                        tvEmail.setText("-");
                    }
                    
                    // Phone
                    String phone = null;
                    if (user.getPersonalData() != null) {
                        phone = user.getPersonalData().getPhoneNumber();
                    }
                    tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "-");
                    
                    // Registration date
                    if (user.getCreatedAt() != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        tvRegistration.setText(sdf.format(user.getCreatedAt()));
                    } else {
                        tvRegistration.setText("-");
                    }
                    
                    // Status
                    String status = user.getStatus();
                    if (status != null) {
                        chipStatus.setText(status);
                        if (status.equalsIgnoreCase("active")) {
                            chipStatus.setChipBackgroundColorResource(R.color.success_light);
                            chipStatus.setTextColor(getResources().getColor(R.color.success, null));
                        } else {
                            chipStatus.setChipBackgroundColorResource(R.color.notification_orange);
                            chipStatus.setTextColor(getResources().getColor(R.color.white, null));
                        }
                    } else {
                        chipStatus.setText("Desconocido");
                    }
                    
                    // Profile image
                    String photoUrl = null;
                    if (user.getPersonalData() != null) {
                        photoUrl = user.getPersonalData().getProfileImageUrl();
                    }
                    
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(requireContext())
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar_24)
                                .error(R.drawable.ic_avatar_24)
                                .centerCrop()
                                .into(ivAvatar);
                        tvAvatarInitial.setVisibility(View.GONE);
                    } else {
                        tvAvatarInitial.setVisibility(View.VISIBLE);
                    }
                }
                
                // Load Guide-specific data
                loadGuideSpecificData(guideId, chipLanguages, tvBiography, cardBiography,
                                     tvRating, tvExperienceYears, tvSpecialties, cardSpecialties);
                
                // Load tours count
                loadToursCount(guideId, tvToursCount);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando datos del usuario", e);
                tvName.setText("Error al cargar");
            }
        });
    }

    private void loadGuideSpecificData(String guideId, ChipGroup chipLanguages,
                                       TextView tvBiography, View cardBiography,
                                       TextView tvRating, TextView tvExperienceYears,
                                       TextView tvSpecialties, View cardSpecialties) {
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FirestoreManager.COLLECTION_GUIDES)
                .document(guideId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Guide guide = doc.toObject(Guide.class);
                        if (guide != null) {
                            // Languages
                            if (guide.getLanguages() != null && !guide.getLanguages().isEmpty()) {
                                chipLanguages.removeAllViews();
                                for (String langCode : guide.getLanguages()) {
                                    Chip chip = new Chip(requireContext());
                                    chip.setText(getLanguageName(langCode));
                                    chip.setChipBackgroundColorResource(R.color.primary_light);
                                    chip.setTextColor(getResources().getColor(R.color.primary, null));
                                    chip.setClickable(false);
                                    chipLanguages.addView(chip);
                                }
                            }
                            
                            // Biography
                            if (guide.getBiography() != null && !guide.getBiography().isEmpty()) {
                                tvBiography.setText(guide.getBiography());
                                cardBiography.setVisibility(View.VISIBLE);
                            } else {
                                cardBiography.setVisibility(View.GONE);
                            }
                            
                            // Rating
                            if (guide.getRating() != null && guide.getRating() > 0) {
                                tvRating.setText(String.format(Locale.getDefault(), "%.1f", guide.getRating()));
                            } else {
                                tvRating.setText("0.0");
                            }
                            
                            // Experience
                            if (guide.getYearsOfExperience() != null && guide.getYearsOfExperience() > 0) {
                                tvExperienceYears.setText(String.valueOf(guide.getYearsOfExperience()));
                            } else {
                                tvExperienceYears.setText("0");
                            }
                            
                            // Specialties
                            if (guide.getSpecialties() != null && !guide.getSpecialties().isEmpty()) {
                                tvSpecialties.setText(guide.getSpecialties());
                                cardSpecialties.setVisibility(View.VISIBLE);
                            } else {
                                cardSpecialties.setVisibility(View.GONE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cargando datos del guía", e);
                });
    }

    private void loadToursCount(String guideId, TextView tvToursCount) {
        FirestoreManager firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getReservationsByGuide(guideId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<Reservation> reservations = (List<Reservation>) result;
                    tvToursCount.setText(String.valueOf(reservations.size()));
                } else {
                    tvToursCount.setText("0");
                }
            }

            @Override
            public void onFailure(Exception e) {
                tvToursCount.setText("0");
            }
        });
    }

    private String getLanguageName(String code) {
        if (code == null) return "Desconocido";
        String name = LANGUAGE_NAMES.get(code.toLowerCase());
        return name != null ? name : code;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
