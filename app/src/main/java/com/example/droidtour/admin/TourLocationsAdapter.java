package com.example.droidtour.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;

import java.util.List;

/**
 * Adapter para la lista de ubicaciones del tour
 * Soporta drag & drop, selección, y eliminación
 * Creado por dan xd
 */
public class TourLocationsAdapter extends RecyclerView.Adapter<TourLocationsAdapter.ViewHolder> {

    private final List<TourLocation> locations;
    private OnLocationDeleteListener deleteListener;
    private OnLocationClickListener clickListener;
    private int selectedPosition = -1;

    public interface OnLocationDeleteListener {
        void onLocationDeleted(int position, TourLocation location);
    }

    public interface OnLocationClickListener {
        void onLocationClicked(int position);
    }

    public TourLocationsAdapter(List<TourLocation> locations) {
        this.locations = locations;
    }

    public void setOnLocationDeleteListener(OnLocationDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnLocationClickListener(OnLocationClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Establece la posición seleccionada y actualiza la UI
     */
    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;

        // Notificar cambios para actualizar el resaltado visual
        if (previousPosition != -1 && previousPosition < locations.size()) {
            notifyItemChanged(previousPosition);
        }
        if (position != -1 && position < locations.size()) {
            notifyItemChanged(position);
        }
    }

    /**
     * Limpia la selección actual
     */
    public void clearSelection() {
        setSelectedPosition(-1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_location_neo, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TourLocation location = locations.get(position);

        // Establecer datos
        holder.tvOrder.setText(String.valueOf(position + 1));
        holder.tvName.setText(location.name);

        // Resaltar el item seleccionado
        boolean isSelected = position == selectedPosition;
        if (isSelected) {
            // Aplicar estilo de selección
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.selected_item_bg));
            holder.cardView.setCardElevation(8f);
            holder.tvOrder.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
            holder.tvName.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
        } else {
            // Estilo normal
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(android.R.color.white));
            holder.cardView.setCardElevation(2f);
            holder.tvOrder.setTextColor(holder.itemView.getContext().getColor(R.color.text_primary));
            holder.tvName.setTextColor(holder.itemView.getContext().getColor(R.color.text_primary));
        }

        // Click en el botón eliminar
        holder.ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    deleteListener.onLocationDeleted(adapterPosition, location);
                }
            }
        });

        // Click en el item completo
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    setSelectedPosition(adapterPosition);
                    clickListener.onLocationClicked(adapterPosition);
                }
            }
        });

        // Agregar efecto de ripple al hacer click
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvOrder;
        TextView tvName;
        ImageView ivDelete;
        ImageView ivDragHandle;

        ViewHolder(View v) {
            super(v);

            // Si tu item usa CardView como root
            if (v instanceof CardView) {
                cardView = (CardView) v;
            } else {
                // Si el CardView está dentro del layout
                cardView = v.findViewById(R.id.card_location);
            }

            tvOrder = v.findViewById(R.id.tv_order);
            tvName = v.findViewById(R.id.tv_location_name);
            ivDelete = v.findViewById(R.id.iv_delete);
            ivDragHandle = v.findViewById(R.id.iv_drag_handle);
        }
    }
}