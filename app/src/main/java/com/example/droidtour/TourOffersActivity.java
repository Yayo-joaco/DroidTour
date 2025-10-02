package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TourOffersActivity extends AppCompatActivity {

    private RecyclerView rvTourOffers;
    private ChipGroup chipGroupFilter;
    private TourOffersAdapter offersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_offers);

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadTourOffers();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Ofertas de Tours");
    }
    
    private void initializeViews() {
        rvTourOffers = findViewById(R.id.rv_tour_offers);
        chipGroupFilter = findViewById(R.id.chip_group_filter);
    }

    private void setupRecyclerView() {
        rvTourOffers.setLayoutManager(new LinearLayoutManager(this));
        offersAdapter = new TourOffersAdapter(this::openTourDetails);
        rvTourOffers.setAdapter(offersAdapter);
    }

    private void setupClickListeners() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                String filterType = "";

                if (checkedId == R.id.chip_all) {
                    filterType = "Todas";
                } else if (checkedId == R.id.chip_pending) {
                    filterType = "Pendiente";
                } else if (checkedId == R.id.chip_accepted) {
                    filterType = "Aceptado";
                } else if (checkedId == R.id.chip_rejected) {
                    filterType = "Rechazado";
                }

                Toast.makeText(this, "Filtro aplicado: " + filterType, Toast.LENGTH_SHORT).show();
                offersAdapter.filterByStatus(filterType);
            }
        });
    }
    
    private void loadTourOffers() {
        // TODO: Cargar ofertas de tours desde base de datos
        Toast.makeText(this, "Cargando ofertas de tours...", Toast.LENGTH_SHORT).show();

        // Aplicar filtro inicial "Pendiente" ya que el chip está marcado por defecto
        offersAdapter.filterByStatus("Pendiente");
    }

    private void openTourDetails(TourOffer offer) {
        Intent intent = new Intent(this, TourOfferDetailActivity.class);
        intent.putExtra("TOUR_NAME", offer.tourName);
        intent.putExtra("COMPANY_NAME", offer.companyName);
        intent.putExtra("TOUR_DATE", offer.date);
        intent.putExtra("TOUR_TIME", offer.time);
        intent.putExtra("TOUR_DURATION", offer.duration);
        intent.putExtra("PAYMENT_AMOUNT", offer.paymentAmount);
        intent.putExtra("PARTICIPANTS", offer.participants);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

// Clase para representar una oferta de tour
class TourOffer {
    public String tourName;
    public String companyName;
    public String date;
    public String time;
    public String duration;
    public String paymentAmount;
    public String participants;
    public String status;
    public String offerDate;

    public TourOffer(String tourName, String companyName, String date, String time, String duration,
                     String paymentAmount, String participants, String status, String offerDate) {
        this.tourName = tourName;
        this.companyName = companyName;
        this.date = date;
        this.time = time;
        this.duration = duration;
        this.paymentAmount = paymentAmount;
        this.participants = participants;
        this.status = status;
        this.offerDate = offerDate;
    }
}

// Adaptador para ofertas de tours
class TourOffersAdapter extends RecyclerView.Adapter<TourOffersAdapter.ViewHolder> {
    interface OnTourOfferClick { void onClick(TourOffer offer); }

    private final OnTourOfferClick onTourOfferClick;
    private final TourOffer[] allTourOffers;
    private java.util.List<TourOffer> filteredTourOffers;

    TourOffersAdapter(OnTourOfferClick listener) {
        this.onTourOfferClick = listener;
        // Datos mock de ofertas
        this.allTourOffers = new TourOffer[] {
            new TourOffer("City Tour Lima Centro Histórico", "Inka Travel Peru", "Hoy", "09:30 AM",
                         "3 horas", "S/. 200.00", "6 personas", "Pendiente", "Enviado hace 2 horas"),
            new TourOffer("City Tour Lima Centro Histórico", "Lima Adventure Tours", "Mañana", "08:00 AM",
                         "4 horas", "S/. 180.00", "10 personas", "Aceptado", "Enviado hace 3 horas"),
            new TourOffer("Tour Gastronómico", "Cusco Heritage", "15 Dic, 2024", "02:00 PM",
                         "5 horas", "S/. 250.00", "8 personas", "Rechazado", "Enviado ayer"),
            new TourOffer("Valle Sagrado Express", "Sacred Valley Tours", "16 Dic, 2024", "07:00 AM",
                         "8 horas", "S/. 300.00", "12 personas", "Pendiente", "Enviado hace 1 hora")
        };
        this.filteredTourOffers = java.util.Arrays.asList(allTourOffers);
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_offer_guide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TourOffer offer = filteredTourOffers.get(position);

        holder.tvCompanyName.setText(offer.companyName);
        holder.tvOfferDate.setText(offer.offerDate);
        holder.tvOfferStatus.setText(offer.status);
        holder.tvTourName.setText(offer.tourName);
        holder.tvTourDate.setText(offer.date);
        holder.tvTourTime.setText(offer.time);
        holder.tvTourDuration.setText(offer.duration);
        holder.tvPaymentAmount.setText(offer.paymentAmount);
        holder.tvParticipants.setText(offer.participants);

        // Configurar background y color del estado
        if (offer.status.equals("Aceptado")) {
            holder.tvOfferStatus.setBackgroundResource(R.drawable.status_background_green);
            holder.tvOfferStatus.setTextColor(holder.itemView.getContext().getColor(R.color.white));
        } else if (offer.status.equals("Rechazado")) {
            holder.tvOfferStatus.setBackgroundResource(R.drawable.status_background_red);
            holder.tvOfferStatus.setTextColor(holder.itemView.getContext().getColor(R.color.white));
        } else {
            holder.tvOfferStatus.setBackgroundResource(R.drawable.status_background_orange);
            holder.tvOfferStatus.setTextColor(holder.itemView.getContext().getColor(R.color.white));
        }

        // Mostrar/ocultar botones según el estado
        if (offer.status.equals("Aceptado") || offer.status.equals("Rechazado")) {
            holder.layoutPendingActions.setVisibility(android.view.View.GONE);
            holder.layoutResponseStatus.setVisibility(android.view.View.VISIBLE);

            if (offer.status.equals("Aceptado")) {
                holder.ivResponseIcon.setImageResource(android.R.drawable.ic_menu_agenda);
                holder.ivResponseIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.green));
                holder.tvResponseMessage.setText("Oferta aceptada - Tour asignado");
                holder.tvResponseMessage.setTextColor(holder.itemView.getContext().getColor(R.color.green));
            } else {
                holder.ivResponseIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                holder.ivResponseIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.red));
                holder.tvResponseMessage.setText("Oferta rechazada");
                holder.tvResponseMessage.setTextColor(holder.itemView.getContext().getColor(R.color.red));
            }
            holder.tvResponseDate.setText(offer.offerDate);
        } else {
            holder.layoutPendingActions.setVisibility(android.view.View.VISIBLE);
            holder.layoutResponseStatus.setVisibility(android.view.View.GONE);
        }

        // Configurar botón "Ver Detalles"
        holder.btnViewDetails.setOnClickListener(v -> onTourOfferClick.onClick(offer));

        // Configurar botones de aceptar/rechazar
        holder.btnAcceptOffer.setOnClickListener(v -> {
            // TODO: Implementar lógica de aceptar oferta
            android.widget.Toast.makeText(v.getContext(), "Oferta aceptada", android.widget.Toast.LENGTH_SHORT).show();
        });

        holder.btnRejectOffer.setOnClickListener(v -> {
            // TODO: Implementar lógica de rechazar oferta
            android.widget.Toast.makeText(v.getContext(), "Oferta rechazada", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return filteredTourOffers.size();
    }

    public void filterByStatus(String status) {
        if (status.equals("Todas")) {
            filteredTourOffers = java.util.Arrays.asList(allTourOffers);
        } else {
            filteredTourOffers = new java.util.ArrayList<>();
            for (TourOffer offer : allTourOffers) {
                if (offer.status.equals(status)) {
                    filteredTourOffers.add(offer);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        android.widget.TextView tvCompanyName, tvOfferDate, tvOfferStatus, tvTourName;
        android.widget.TextView tvTourDate, tvTourTime, tvTourDuration, tvPaymentAmount, tvParticipants;
        android.widget.TextView tvResponseMessage, tvResponseDate;
        android.widget.ImageView ivResponseIcon;
        android.widget.Button btnViewDetails;
        com.google.android.material.button.MaterialButton btnAcceptOffer, btnRejectOffer;
        android.widget.LinearLayout layoutPendingActions, layoutResponseStatus;

        ViewHolder(android.view.View itemView) {
            super(itemView);
            tvCompanyName = itemView.findViewById(R.id.tv_company_name);
            tvOfferDate = itemView.findViewById(R.id.tv_offer_date);
            tvOfferStatus = itemView.findViewById(R.id.tv_offer_status);
            tvTourName = itemView.findViewById(R.id.tv_tour_name);
            tvTourDate = itemView.findViewById(R.id.tv_tour_date);
            tvTourTime = itemView.findViewById(R.id.tv_tour_time);
            tvTourDuration = itemView.findViewById(R.id.tv_tour_duration);
            tvPaymentAmount = itemView.findViewById(R.id.tv_payment_amount);
            tvParticipants = itemView.findViewById(R.id.tv_participants);
            tvResponseMessage = itemView.findViewById(R.id.tv_response_message);
            tvResponseDate = itemView.findViewById(R.id.tv_response_date);
            ivResponseIcon = itemView.findViewById(R.id.iv_response_icon);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
            btnAcceptOffer = itemView.findViewById(R.id.btn_accept_offer);
            btnRejectOffer = itemView.findViewById(R.id.btn_reject_offer);
            layoutPendingActions = itemView.findViewById(R.id.layout_pending_actions);
            layoutResponseStatus = itemView.findViewById(R.id.layout_response_status);
        }
    }
}
