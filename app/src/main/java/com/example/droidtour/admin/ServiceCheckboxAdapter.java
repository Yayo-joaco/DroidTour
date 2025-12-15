package com.example.droidtour.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.R;
import com.example.droidtour.models.Service;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;

public class ServiceCheckboxAdapter extends RecyclerView.Adapter<ServiceCheckboxAdapter.ViewHolder> {

    private final List<Service> services;
    private final OnServiceCheckedListener listener;
    private List<String> selectedServiceIds;

    public interface OnServiceCheckedListener {
        void onServiceChecked(String serviceId, String serviceName, boolean isChecked);
    }

    public ServiceCheckboxAdapter(List<Service> services, OnServiceCheckedListener listener) {
        this.services = services;
        this.listener = listener;
        this.selectedServiceIds = new java.util.ArrayList<>();
    }

    public void setSelectedServices(List<String> serviceIds) {
        this.selectedServiceIds = serviceIds != null ? new java.util.ArrayList<>(serviceIds) : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Service service = services.get(position);
        
        // Nombre del servicio
        holder.cbService.setText(service.getName());
        
        // Precio del servicio
        if (service.getPrice() > 0) {
            holder.tvServicePrice.setText("S/. " + String.format("%.2f", service.getPrice()));
            holder.tvServicePrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvServicePrice.setVisibility(View.GONE);
        }
        
        // Verificar si este servicio está seleccionado
        boolean isSelected = selectedServiceIds.contains(service.getServiceId());
        
        // Listener para el checkbox
        holder.cbService.setOnCheckedChangeListener(null); // Evitar triggers no deseados
        holder.cbService.setChecked(isSelected);
        holder.cbService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onServiceChecked(service.getServiceId(), service.getName(), isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return services != null ? services.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCheckBox cbService;
        TextView tvServicePrice;

        ViewHolder(View itemView) {
            super(itemView);
            cbService = itemView.findViewById(R.id.cb_service);
            tvServicePrice = itemView.findViewById(R.id.tv_service_price);
        }
    }
}

