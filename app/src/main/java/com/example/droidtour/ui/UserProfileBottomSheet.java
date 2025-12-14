package com.example.droidtour.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Reservation;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class UserProfileBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_USER_ID = "arg_user_id";
    private static final String ARG_FULL_NAME = "arg_full_name";
    private static final String ARG_EMAIL = "arg_email";
    private static final String ARG_PHONE = "arg_phone";
    private static final String ARG_AVATAR = "arg_avatar";
    private static final String ARG_USER_TYPE = "arg_user_type";
    private static final String ARG_CREATED_AT = "arg_created_at"; // millis
    private static final String ARG_STATUS = "arg_status";
    private OnUserProfileActionListener actionListener;

    public interface OnUserProfileActionListener {
        void onEditUser(String userId);
        void onSendMessageToUser(String userId);
    }

    public static UserProfileBottomSheet newInstance(@NonNull String userId,
                                                     @Nullable String fullName,
                                                     @Nullable String email,
                                                     @Nullable String phone,
                                                     @Nullable String avatarUrl,
                                                     @Nullable String userType,
                                                     long createdAtMillis,
                                                     @Nullable String status) {
        UserProfileBottomSheet fragment = new UserProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_FULL_NAME, fullName);
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_PHONE, phone);
        args.putString(ARG_AVATAR, avatarUrl);
        args.putString(ARG_USER_TYPE, userType);
        args.putLong(ARG_CREATED_AT, createdAtMillis);
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnUserProfileActionListener) {
            actionListener = (OnUserProfileActionListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btn_close);
        ImageView ivAvatar = view.findViewById(R.id.iv_user_avatar);
        TextView tvName = view.findViewById(R.id.tv_user_name);
        TextView tvEmail = view.findViewById(R.id.tv_user_email);
        TextView tvPhone = view.findViewById(R.id.tv_user_phone);
        TextView tvRegistration = view.findViewById(R.id.tv_registration_date);
        Chip chipRole = view.findViewById(R.id.chip_user_role);
        Chip chipStatus = view.findViewById(R.id.chip_user_status);
        ChipGroup chipLanguages = view.findViewById(R.id.chip_group_languages);

        TextView tvToursCount = view.findViewById(R.id.tv_tours_count);
        TextView tvRating = view.findViewById(R.id.tv_rating);
        TextView tvMemberYear = view.findViewById(R.id.tv_member_year);
        LinearLayout layoutRating = view.findViewById(R.id.layout_rating);
        View cardStatistics = view.findViewById(R.id.card_statistics);

        View btnEdit = view.findViewById(R.id.btn_edit_profile);
        View btnMessage = view.findViewById(R.id.btn_send_message);

        Bundle args = getArguments();
        if (args == null) return;

        final String userId = args.getString(ARG_USER_ID);
        String fullName = args.getString(ARG_FULL_NAME);
        String email = args.getString(ARG_EMAIL);
        String phone = args.getString(ARG_PHONE);
        String avatar = args.getString(ARG_AVATAR);
        String userType = args.getString(ARG_USER_TYPE);
        long createdAt = args.getLong(ARG_CREATED_AT, -1);
        String status = args.getString(ARG_STATUS);

        tvName.setText(fullName != null && !fullName.isEmpty() ? fullName : "Sin nombre");
        tvEmail.setText(email != null ? email : "-");
        tvPhone.setText(phone != null ? phone : "-");
        chipRole.setText(getUserTypeDisplayName(userType));
        chipStatus.setText(status != null ? status : "-");

        if (createdAt > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());
            tvRegistration.setText(sdf.format(new java.util.Date(createdAt)));
            tvMemberYear.setText(new java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(new java.util.Date(createdAt)));
        } else {
            tvRegistration.setText("-");
            tvMemberYear.setText("-");
        }

        // Configurar estadísticas según el tipo de usuario
        setupStatistics(userId, userType, tvToursCount, tvRating, tvMemberYear, layoutRating, cardStatistics);

        // Avatar
        if (avatar != null && !avatar.isEmpty()) {
            Glide.with(requireContext())
                    .load(avatar)
                    .placeholder(R.drawable.ic_avatar_24)
                    .error(R.drawable.ic_avatar_24)
                    .centerCrop()
                    .into(ivAvatar);
            view.findViewById(R.id.tv_avatar_initial).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.tv_avatar_initial).setVisibility(View.VISIBLE);
        }

        btnClose.setOnClickListener(v -> dismiss());

        btnEdit.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEditUser(userId);
            } else {
                Toast.makeText(getContext(), "Editar: " + userId, Toast.LENGTH_SHORT).show();
            }
        });

        btnMessage.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onSendMessageToUser(userId);
            } else {
                Toast.makeText(getContext(), "Mensaje a: " + userId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Hacer el fondo de la ventana transparente para que se aprecien las esquinas redondeadas del drawable de fondo
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        actionListener = null;
    }

    /**
     * Convierte el tipo de usuario a un nombre legible para mostrar
     */
    private String getUserTypeDisplayName(String userType) {
        if (userType == null) return "Sin tipo";

        switch (userType) {
            case "SUPERADMIN": return "Super Admin";
            case "ADMIN": return "Administrador";
            case "COMPANY_ADMIN": return "Administrador de empresa";
            case "GUIDE": return "Guía Turístico";
            case "CLIENT": return "Cliente";
            default: return userType;
        }
    }

    /**
     * Configura las estadísticas según el tipo de usuario
     */
    private void setupStatistics(String userId, String userType, TextView tvToursCount, 
                                 TextView tvRating, TextView tvMemberYear, 
                                 LinearLayout layoutRating, View cardStatistics) {
        if (userId == null || userId.isEmpty()) {
            // Ocultar estadísticas si no hay userId
            if (cardStatistics != null) {
                cardStatistics.setVisibility(View.GONE);
            }
            return;
        }

        // Solo mostrar estadísticas para GUIDE y CLIENT
        if (!"GUIDE".equals(userType) && !"CLIENT".equals(userType)) {
            if (cardStatistics != null) {
                cardStatistics.setVisibility(View.GONE);
            }
            return;
        }

        // Mostrar la tarjeta de estadísticas
        if (cardStatistics != null) {
            cardStatistics.setVisibility(View.VISIBLE);
        }

        // Para CLIENT: ocultar Rating
        // Para GUIDE: mostrar Rating
        if (layoutRating != null) {
            layoutRating.setVisibility("GUIDE".equals(userType) ? View.VISIBLE : View.GONE);
        }

        // Cargar número de tours desde la base de datos
        loadToursCount(userId, userType, tvToursCount);

        // Cargar rating solo para GUIDE
        if ("GUIDE".equals(userType)) {
            loadGuideRating(userId, tvRating);
        } else {
            // Para CLIENT, ocultar o dejar vacío
            if (tvRating != null) {
                tvRating.setText("0.0");
            }
        }
    }

    /**
     * Carga el número de tours desde la base de datos
     */
    private void loadToursCount(String userId, String userType, TextView tvToursCount) {
        if (tvToursCount == null) return;

        FirestoreManager firestoreManager = FirestoreManager.getInstance();

        if ("GUIDE".equals(userType)) {
            // Para guía: contar todas las reservas donde guideId = userId
            firestoreManager.getReservationsByGuide(userId, new FirestoreManager.FirestoreCallback() {
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
        } else if ("CLIENT".equals(userType)) {
            // Para cliente: contar todas las reservas donde userId = userId
            firestoreManager.getReservationsByUser(userId, new FirestoreManager.FirestoreCallback() {
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
        } else {
            tvToursCount.setText("0");
        }
    }

    /**
     * Carga el rating del guía desde la base de datos
     */
    private void loadGuideRating(String userId, TextView tvRating) {
        if (tvRating == null || userId == null || userId.isEmpty()) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Intentar cargar desde user_roles primero
        db.collection(FirestoreManager.COLLECTION_USER_ROLES)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Float rating = null;

                        // Extraer rating de diferentes estructuras posibles
                        if (doc.contains("rating")) {
                            Object ratingObj = doc.get("rating");
                            if (ratingObj instanceof Number) {
                                rating = ((Number) ratingObj).floatValue();
                            }
                        } else if (doc.contains("guide")) {
                            Object guideObj = doc.get("guide");
                            if (guideObj instanceof java.util.Map) {
                                java.util.Map<String, Object> guideMap = (java.util.Map<String, Object>) guideObj;
                                Object ratingObj = guideMap.get("rating");
                                if (ratingObj instanceof Number) {
                                    rating = ((Number) ratingObj).floatValue();
                                }
                            }
                        }

                        // Actualizar UI con el rating
                        if (rating != null && rating > 0) {
                            tvRating.setText(String.format("%.1f", rating));
                        } else {
                            tvRating.setText("0.0");
                        }
                    } else {
                        // Si no existe en user_roles, intentar desde guides
                        loadRatingFromGuides(userId, tvRating);
                    }
                })
                .addOnFailureListener(e -> {
                    // Si falla, intentar desde guides
                    loadRatingFromGuides(userId, tvRating);
                });
    }

    /**
     * Método fallback para cargar rating desde la colección guides
     */
    private void loadRatingFromGuides(String userId, TextView tvRating) {
        if (tvRating == null || userId == null || userId.isEmpty()) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FirestoreManager.COLLECTION_GUIDES)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Object ratingObj = doc.get("rating");
                        if (ratingObj instanceof Number) {
                            float rating = ((Number) ratingObj).floatValue();
                            tvRating.setText(String.format("%.1f", rating));
                        } else {
                            tvRating.setText("0.0");
                        }
                    } else {
                        tvRating.setText("0.0");
                    }
                })
                .addOnFailureListener(e -> {
                    tvRating.setText("0.0");
                });
    }
}
