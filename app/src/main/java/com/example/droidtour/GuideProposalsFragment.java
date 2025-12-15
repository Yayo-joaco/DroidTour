package com.example.droidtour;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.droidtour.models.Guide;
import com.example.droidtour.models.Tour;
import com.example.droidtour.models.TourOffer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GuideProposalsFragment extends Fragment {

    private static final String TAG = "GuideProposalsFragment";
    
    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private ChipGroup chipGroupStatus;
    private Chip chipAll, chipPending, chipAccepted, chipRejected;
    
    private ProposalsAdapter adapter;
    private List<ProposalWithDetails> proposals = new ArrayList<>();
    private List<ProposalWithDetails> filteredProposals = new ArrayList<>();
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String currentCompanyId;
    private String currentFilter = "ALL";

    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private com.example.droidtour.firebase.FirestoreManager firestoreManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        prefsManager = new com.example.droidtour.utils.PreferencesManager(requireContext());
        firestoreManager = com.example.droidtour.firebase.FirestoreManager.getInstance();
        
        // Obtener ID desde FirebaseAuth o PreferencesManager
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            currentUserId = prefsManager.getUserId();
        }
        
        Log.d(TAG, "📱 CurrentUserId obtenido: " + currentUserId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guide_proposals, container, false);
        
        recyclerView = view.findViewById(R.id.recycler_proposals);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        chipGroupStatus = view.findViewById(R.id.chip_group_status);
        chipAll = view.findViewById(R.id.chip_all);
        chipPending = view.findViewById(R.id.chip_pending);
        chipAccepted = view.findViewById(R.id.chip_accepted);
        chipRejected = view.findViewById(R.id.chip_rejected);
        
        setupRecyclerView();
        setupStatusChips();
        loadProposals();
        
        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProposalsAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupStatusChips() {
        chipAll.setChecked(true);
        
        chipAll.setOnClickListener(v -> {
            currentFilter = "ALL";
            filterProposals();
        });
        
        chipPending.setOnClickListener(v -> {
            currentFilter = "PENDIENTE";
            filterProposals();
        });
        
        chipAccepted.setOnClickListener(v -> {
            currentFilter = "ACEPTADA";
            filterProposals();
        });
        
        chipRejected.setOnClickListener(v -> {
            currentFilter = "RECHAZADA";
            filterProposals();
        });
    }

    private void loadProposals() {
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Primero obtener el companyId del usuario actual
        firestoreManager.getUserById(currentUserId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                if (user != null && user.getCompanyId() != null) {
                    currentCompanyId = user.getCompanyId();
                    Log.d(TAG, "✅ CompanyId obtenido: " + currentCompanyId);
                    loadProposalsFromFirestore();
                } else {
                    Log.e(TAG, "❌ Usuario sin companyId");
                    Toast.makeText(requireContext(), "Error: Usuario sin empresa asignada", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error cargando usuario", e);
                Toast.makeText(requireContext(), "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProposalsFromFirestore() {
        if (currentCompanyId == null) {
            return;
        }

        // Usar el MISMO método que SearchGuidesFragment
        Log.d(TAG, "🔍 Usando getOffersByCompany con companyId: " + currentCompanyId);
        
        firestoreManager.getOffersByCompany(currentCompanyId, new com.example.droidtour.firebase.FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<TourOffer> allOffers = (List<TourOffer>) result;
                Log.d(TAG, "✅ Ofertas encontradas: " + allOffers.size());
                
                proposals.clear();
                
                for (TourOffer offer : allOffers) {
                    Log.d(TAG, "  📋 Oferta ID: " + offer.getOfferId() + " | Status: " + offer.getStatus() + " | Guía: " + offer.getGuideName() + " | Tour: " + offer.getTourName());
                    loadProposalDetails(offer);
                }
                
                // Ordenar por fecha después de cargar todos los datos
                sortProposalsByDate();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error al cargar propuestas", e);
                Toast.makeText(requireContext(), "Error al cargar propuestas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProposalDetails(TourOffer offer) {
        ProposalWithDetails proposalDetails = new ProposalWithDetails();
        proposalDetails.offer = offer;
        
        // Cargar información del guía
        db.collection("Guides").document(offer.getGuideId())
                .get()
                .addOnSuccessListener(guideDoc -> {
                    if (guideDoc.exists()) {
                        Guide guide = guideDoc.toObject(Guide.class);
                        if (guide != null) {
                            proposalDetails.guide = guide;
                            
                            // Cargar información del usuario del guía
                            db.collection("Users").document(offer.getGuideId())
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            proposalDetails.guideName = userDoc.getString("name");
                                            // Obtener la foto de perfil desde personalData
                                            if (userDoc.contains("personalData")) {
                                                Object personalData = userDoc.get("personalData");
                                                if (personalData instanceof java.util.Map) {
                                                    java.util.Map<String, Object> personalDataMap = (java.util.Map<String, Object>) personalData;
                                                    proposalDetails.guidePhotoUrl = (String) personalDataMap.get("profileImageUrl");
                                                }
                                            }
                                            checkAndAddProposal(proposalDetails);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error cargando usuario del guía", e);
                                        checkAndAddProposal(proposalDetails);
                                    });
                        }
                    }
                });
        
        // Cargar información del tour
        db.collection("Tours").document(offer.getTourId())
                .get()
                .addOnSuccessListener(tourDoc -> {
                    if (tourDoc.exists()) {
                        Tour tour = tourDoc.toObject(Tour.class);
                        if (tour != null) {
                            proposalDetails.tour = tour;
                            proposalDetails.tourName = tour.getTourName();
                            checkAndAddProposal(proposalDetails);
                        }
                    }
                });
    }

    private void checkAndAddProposal(ProposalWithDetails proposalDetails) {
        // Solo agregar si tenemos todos los datos necesarios
        if (proposalDetails.guideName != null && proposalDetails.tourName != null) {
            // Verificar si ya existe en la lista
            boolean exists = false;
            for (int i = 0; i < proposals.size(); i++) {
                if (proposals.get(i).offer.getId().equals(proposalDetails.offer.getId())) {
                    proposals.set(i, proposalDetails);
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                proposals.add(proposalDetails);
            }
            
            filterProposals();
        }
    }

    private void sortProposalsByDate() {
        // Ordenar propuestas por fecha de creación (más reciente primero)
        proposals.sort((p1, p2) -> {
            Date date1 = p1.offer.getCreatedAt();
            Date date2 = p2.offer.getCreatedAt();
            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;
            return date2.compareTo(date1); // Orden descendente
        });
        filterProposals();
    }

    private void filterProposals() {
        filteredProposals.clear();
        
        for (ProposalWithDetails proposal : proposals) {
            if (currentFilter.equals("ALL") || proposal.offer.getStatus().equals(currentFilter)) {
                filteredProposals.add(proposal);
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredProposals.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showCancelConfirmationDialog(ProposalWithDetails proposal) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancelar Propuesta")
                .setMessage("¿Estás seguro de que deseas cancelar esta propuesta?")
                .setPositiveButton("Sí, Cancelar", (dialog, which) -> {
                    cancelProposal(proposal);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelProposal(ProposalWithDetails proposal) {
        db.collection("TourOffers").document(proposal.offer.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Propuesta cancelada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cancelar propuesta", e);
                    Toast.makeText(requireContext(), "Error al cancelar propuesta", Toast.LENGTH_SHORT).show();
                });
    }

    // Clase interna para almacenar los detalles completos de una propuesta
    private static class ProposalWithDetails {
        TourOffer offer;
        Guide guide;
        Tour tour;
        String guideName;
        String guidePhotoUrl;
        String tourName;
    }

    // Adapter para el RecyclerView
    private class ProposalsAdapter extends RecyclerView.Adapter<ProposalsAdapter.ProposalViewHolder> {

        @NonNull
        @Override
        public ProposalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_proposal, parent, false);
            return new ProposalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProposalViewHolder holder, int position) {
            ProposalWithDetails proposal = filteredProposals.get(position);
            holder.bind(proposal);
        }

        @Override
        public int getItemCount() {
            return filteredProposals.size();
        }

        class ProposalViewHolder extends RecyclerView.ViewHolder {
            private ImageView imgGuide;
            private ImageView icStatus;
            private TextView tvGuideName;
            private TextView tvTourName;
            private TextView tvPayment;
            private TextView tvDate;
            private TextView tvStatus;
            private TextView tvNotes;
            private MaterialButton btnCancel;
            private Chip chipStatus;

            public ProposalViewHolder(@NonNull View itemView) {
                super(itemView);
                imgGuide = itemView.findViewById(R.id.img_guide);
                icStatus = itemView.findViewById(R.id.ic_status);
                tvGuideName = itemView.findViewById(R.id.tv_guide_name);
                tvTourName = itemView.findViewById(R.id.tv_tour_name);
                tvPayment = itemView.findViewById(R.id.tv_payment);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvNotes = itemView.findViewById(R.id.tv_notes);
                btnCancel = itemView.findViewById(R.id.btn_cancel);
                chipStatus = itemView.findViewById(R.id.chip_status);
            }

            public void bind(ProposalWithDetails proposal) {
                // Foto del guía
                if (proposal.guidePhotoUrl != null && !proposal.guidePhotoUrl.isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(proposal.guidePhotoUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(imgGuide);
                } else {
                    imgGuide.setImageResource(R.drawable.ic_person);
                }

                // Nombre del guía
                tvGuideName.setText(proposal.guideName);

                // Nombre del tour
                tvTourName.setText(proposal.tourName);

                // Monto de pago
                DecimalFormat formatter = new DecimalFormat("#,###.00");
                tvPayment.setText("S/ " + formatter.format(proposal.offer.getPaymentAmount()));

                // Fecha
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("es", "PE"));
                String dateStr = sdf.format(proposal.offer.getCreatedAt());
                tvDate.setText(dateStr);

                // Estado - Actualizar texto y colores
                String status = proposal.offer.getStatus();
                String statusText = getStatusText(status);
                tvStatus.setText(statusText);
                
                // Configurar chip de estado y colores
                int statusColor;
                int chipBackgroundColor;
                switch (status) {
                    case "ACEPTADA":
                        statusColor = ContextCompat.getColor(itemView.getContext(), R.color.success);
                        chipBackgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.success_light);
                        chipStatus.setChipIconResource(R.drawable.ic_check);
                        break;
                    case "RECHAZADA":
                        statusColor = ContextCompat.getColor(itemView.getContext(), R.color.error);
                        chipBackgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.error_light);
                        chipStatus.setChipIconResource(R.drawable.ic_cancel);
                        break;
                    default:
                        statusColor = ContextCompat.getColor(itemView.getContext(), R.color.warning);
                        chipBackgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.warning_light);
                        chipStatus.setChipIconResource(R.drawable.ic_time);
                        break;
                }
                
                // Aplicar colores al chip del header
                chipStatus.setText(statusText);
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(chipBackgroundColor));
                chipStatus.setTextColor(statusColor);
                chipStatus.setChipIconTint(ColorStateList.valueOf(statusColor));
                
                // Aplicar colores a la sección de estado
                tvStatus.setTextColor(statusColor);
                if (icStatus != null) {
                    icStatus.setImageTintList(ColorStateList.valueOf(statusColor));
                }

                // Notas
                if (proposal.offer.getNotes() != null && !proposal.offer.getNotes().isEmpty()) {
                    tvNotes.setVisibility(View.VISIBLE);
                    tvNotes.setText(proposal.offer.getNotes());
                } else {
                    tvNotes.setVisibility(View.GONE);
                }

                // Botón cancelar (solo para propuestas pendientes)
                if (status.equals("PENDIENTE")) {
                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setOnClickListener(v -> showCancelConfirmationDialog(proposal));
                } else {
                    btnCancel.setVisibility(View.GONE);
                }
            }

            private String getStatusText(String status) {
                switch (status) {
                    case "ACEPTADA":
                        return "Aceptada";
                    case "RECHAZADA":
                        return "Rechazada";
                    case "PENDIENTE":
                    default:
                        return "Pendiente";
                }
            }
        }
    }
}
