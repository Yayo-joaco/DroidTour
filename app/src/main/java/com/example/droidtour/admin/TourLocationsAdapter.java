package com.example.droidtour.admin;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TourLocationsAdapter extends RecyclerView.Adapter<TourLocationsAdapter.ViewHolder> 
        implements ItemTouchHelperAdapter {

    private final List<TourLocation> locations;
    private OnLocationActionListener listener;
    private ItemTouchHelper itemTouchHelper;
    
    // Interfaces para compatibilidad con TourLocationsMapActivity
    private OnLocationDeleteListener deleteListener;
    private OnLocationClickListener clickListener;

    public interface OnLocationActionListener {
        void onLocationEdited(int position, TourLocation location);
        void onLocationDeleted(int position);
        void onLocationsReordered();
    }
    
    // Interfaces para compatibilidad con el mapa
    public interface OnLocationDeleteListener {
        void onLocationDeleted(int position, TourLocation location);
    }
    
    public interface OnLocationClickListener {
        void onLocationClick(int position);
    }

    // Constructor para CreateTourActivity (con listener completo)
    public TourLocationsAdapter(List<TourLocation> locations, OnLocationActionListener listener) {
        this.locations = locations;
        this.listener = listener;
    }
    
    // Constructor para TourLocationsMapActivity (sin listener)
    public TourLocationsAdapter(List<TourLocation> locations) {
        this.locations = locations;
        this.listener = null;
    }

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }
    
    // Setters para compatibilidad con el mapa
    public void setOnLocationDeleteListener(OnLocationDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void setOnLocationClickListener(OnLocationClickListener listener) {
        this.clickListener = listener;
    }
    
    // Variables y métodos para selección (compatibilidad con mapa)
    private int selectedPosition = -1;
    
    public void setSelectedPosition(int position) {
        int previousSelected = selectedPosition;
        selectedPosition = position;
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }
    
    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_location, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TourLocation location = locations.get(position);
        Context context = holder.itemView.getContext();

        // Número de orden
        if (holder.tvLocationOrder != null) {
            holder.tvLocationOrder.setText(String.valueOf(position + 1));
        }

        // Nombre de la ubicación
        if (holder.tvLocationName != null) {
            holder.tvLocationName.setText(location.getName() != null && !location.getName().isEmpty() 
                    ? location.getName() 
                    : "Ubicación " + (position + 1));
        }

        // Duración
        if (holder.tvLocationTime != null) {
            if (location.stopDuration > 0) {
                holder.tvLocationTime.setText(location.stopDuration + " minutos");
                holder.tvLocationTime.setTextColor(context.getResources().getColor(R.color.primary));
            } else {
                holder.tvLocationTime.setText("Toca para agregar duración");
                holder.tvLocationTime.setTextColor(context.getResources().getColor(R.color.gray));
            }
        }

        // Descripción
        if (holder.tvLocationDescription != null) {
            if (location.getDescription() != null && !location.getDescription().isEmpty()) {
                holder.tvLocationDescription.setText(location.getDescription());
                holder.tvLocationDescription.setVisibility(View.VISIBLE);
            } else {
                holder.tvLocationDescription.setText("Sin descripción");
                holder.tvLocationDescription.setVisibility(View.VISIBLE);
            }
        }

        // Handle para arrastrar
        if (holder.ivDragHandle != null) {
            holder.ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (itemTouchHelper != null) {
                        itemTouchHelper.startDrag(holder);
                    }
                }
                return false;
            });
        }

        // Botón editar
        if (holder.btnEditLocation != null) {
            holder.btnEditLocation.setOnClickListener(v -> {
                showEditDialog(context, position, location);
            });
        }

        // Botón eliminar
        if (holder.btnDeleteLocation != null) {
            holder.btnDeleteLocation.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Eliminar parada")
                        .setMessage("¿Estás seguro de eliminar \"" + location.getName() + "\"?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            if (listener != null) {
                                listener.onLocationDeleted(position);
                            }
                        // Compatibilidad con TourLocationsMapActivity
                        if (deleteListener != null) {
                            deleteListener.onLocationDeleted(position, location);
                        }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }
        
        // Click en el item (para compatibilidad con el mapa)
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onLocationClick(position);
            }
        });
    }

    private void showEditDialog(Context context, int position, TourLocation location) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_stop, null);

        TextView tvStopTitle = dialogView.findViewById(R.id.tv_stop_title);
        TextView tvLocationName = dialogView.findViewById(R.id.tv_location_name);
        TextInputEditText etStopDuration = dialogView.findViewById(R.id.et_stop_duration);
        TextInputEditText etStopDescription = dialogView.findViewById(R.id.et_stop_description);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // Configurar título
        tvStopTitle.setText("Editar Parada " + (position + 1));
        tvLocationName.setText(location.getName() != null ? location.getName() : "Sin nombre");

        // Cargar valores actuales
        if (location.stopDuration > 0) {
            etStopDuration.setText(String.valueOf(location.stopDuration));
        }
        if (location.getDescription() != null && !location.getDescription().isEmpty()) {
            etStopDescription.setText(location.getDescription());
        }

        // Crear diálogo
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Cancelar
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Guardar
        btnSave.setOnClickListener(v -> {
            String durationStr = etStopDuration.getText() != null ? etStopDuration.getText().toString().trim() : "";
            String description = etStopDescription.getText() != null ? etStopDescription.getText().toString().trim() : "";

            // Validar y guardar duración
            int duration = 0;
            if (!durationStr.isEmpty()) {
                try {
                    duration = Integer.parseInt(durationStr);
                    if (duration < 0) duration = 0;
                } catch (NumberFormatException e) {
                    duration = 0;
                }
            }

            location.stopDuration = duration;
            location.setDescription(description);

            if (listener != null) {
                listener.onLocationEdited(position, location);
            }

            notifyItemChanged(position);
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return locations != null ? locations.size() : 0;
    }

    // ItemTouchHelperAdapter implementation
    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(locations, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(locations, i, i - 1);
            }
        }
        
        // Actualizar el campo order de cada location
        for (int i = 0; i < locations.size(); i++) {
            locations.get(i).setOrder(i + 1);
        }
        
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemMoveFinished() {
        // Notificar que los items han cambiado para actualizar los números
        notifyDataSetChanged();
        
        if (listener != null) {
            listener.onLocationsReordered();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDragHandle;
        TextView tvLocationOrder;
        TextView tvLocationName;
        TextView tvLocationTime;
        TextView tvLocationDescription;
        MaterialButton btnEditLocation;
        MaterialButton btnDeleteLocation;

        ViewHolder(View itemView) {
            super(itemView);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            tvLocationOrder = itemView.findViewById(R.id.tv_location_order);
            tvLocationName = itemView.findViewById(R.id.tv_location_name);
            tvLocationTime = itemView.findViewById(R.id.tv_location_time);
            tvLocationDescription = itemView.findViewById(R.id.tv_location_description);
            btnEditLocation = itemView.findViewById(R.id.btn_edit_location);
            btnDeleteLocation = itemView.findViewById(R.id.btn_delete_location);
        }
    }
}

// Interface para ItemTouchHelper
interface ItemTouchHelperAdapter {
    void onItemMove(int fromPosition, int toPosition);
    void onItemMoveFinished();
}
