package com.example.droidtour.admin;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.droidtour.R;
import com.example.droidtour.models.Tour;
import com.google.android.material.chip.Chip;
import java.util.List;

public class AdminTourAdapter extends RecyclerView.Adapter<AdminTourAdapter.ViewHolder> {

    public interface OnTourClickListener {
        void onTourClick(int position, Tour tour, View anchorView);
        void onTourEdit(int position, Tour tour);
        void onTourDelete(int position, Tour tour);
    }

    private final List<Tour> tours;
    private final OnTourClickListener listener;

    public AdminTourAdapter(List<Tour> tours, OnTourClickListener listener) {
        this.tours = tours;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_tour_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tour tour = tours.get(position);
        
        // Nombre del tour
        holder.tvTitle.setText(tour.getTourName() != null ? tour.getTourName() : "Sin nombre");
        
        // Fecha del tour
        holder.tvDates.setText(tour.getTourDate() != null ? tour.getTourDate() : "Sin fecha");
        
        // Precio
        holder.tvPrice.setText(String.format("S/ %.0f", tour.getPricePerPerson() != null ? tour.getPricePerPerson() : 0));
        
        // Categoría/Ubicación
        String category = tour.getCategory() != null ? tour.getCategory() : "Sin categoría";
        holder.tvGuideName.setText(category);
        
        // Duración
        if (holder.chipDuration != null) {
            holder.chipDuration.setText(tour.getDuration() != null ? tour.getDuration() : "N/A");
        }
        
        // Número de paradas
        if (holder.chipPlaces != null) {
            int stops = tour.getStops() != null ? tour.getStops().size() : 0;
            holder.chipPlaces.setText(stops + (stops == 1 ? " parada" : " paradas"));
        }
        
        // Estado (Activo/Sin Guía)
        boolean isActive = tour.getActive() != null && tour.getActive();
        holder.chipStatus.setText(isActive ? "Activo" : "Sin Guía");
        holder.chipStatus.setChipBackgroundColor(holder.itemView.getContext().getResources()
                .getColorStateList(isActive ? R.color.chip_green_bg : R.color.chip_orange_bg));
        holder.chipStatus.setTextColor(holder.itemView.getContext().getResources()
                .getColor(isActive ? R.color.success : R.color.warning));
        
        // Imagen principal (primera imagen del tour)
        if (tour.getMainImageUrl() != null && !tour.getMainImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(tour.getMainImageUrl())
                .centerCrop()
                .placeholder(R.color.light_gray)
                .into(holder.ivImage);
        } else if (tour.getImageUrls() != null && !tour.getImageUrls().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(tour.getImageUrls().get(0))
                .centerCrop()
                .placeholder(R.color.light_gray)
                .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.color.light_gray);
        }
        
        // Chip de reservas (para futuro uso)
        if (holder.chipBookings != null) {
            int bookings = tour.getTotalBookings() != null ? tour.getTotalBookings() : 0;
            int maxGroup = tour.getMaxGroupSize() != null ? tour.getMaxGroupSize() : 20;
            holder.chipBookings.setText(bookings + "/" + maxGroup);
            holder.chipBookings.setVisibility(View.VISIBLE);
        }

        // Click en el item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTourClick(position, tour, v);
        });
        
        // Botones de editar y eliminar
        if (holder.btnEdit != null) {
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onTourEdit(position, tour);
            });
        }
        
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onTourDelete(position, tour);
            });
        }
    }

    @Override
    public int getItemCount() {
        return tours != null ? tours.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvDates, tvPrice, tvGuideName;
        Chip chipStatus, chipDuration, chipBookings, chipPlaces;
        ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_tour_image);
            tvTitle = itemView.findViewById(R.id.tv_tour_title);
            tvDates = itemView.findViewById(R.id.tv_tour_dates);
            tvPrice = itemView.findViewById(R.id.tv_tour_price);
            tvGuideName = itemView.findViewById(R.id.tv_guide_name);
            chipStatus = itemView.findViewById(R.id.chip_status);
            chipDuration = itemView.findViewById(R.id.chip_duration);
            chipBookings = itemView.findViewById(R.id.chip_bookings);
            chipPlaces = itemView.findViewById(R.id.chip_places);
            btnEdit = itemView.findViewById(R.id.btn_edit_tour);
            btnDelete = itemView.findViewById(R.id.btn_delete_tour);
        }
    }
}
