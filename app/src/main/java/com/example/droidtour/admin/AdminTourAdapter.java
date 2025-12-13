package com.example.droidtour.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.R;
import java.util.List;

public class AdminTourAdapter extends RecyclerView.Adapter<AdminTourAdapter.ViewHolder> {

    public interface OnTourClickListener {
        void onTourClick(int position, String tourName, View anchorView);
    }

    private final List<String> items;
    private final OnTourClickListener listener;

    public AdminTourAdapter(List<String> items, OnTourClickListener listener) {
        this.items = items;
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
        String name = items.get(position);
        holder.tvTitle.setText(name);
        // Datos de ejemplo
        holder.tvDates.setText("15 - 18 Dic 2024");
        holder.tvPrice.setText("S/ 850");
        holder.tvGuideName.setText("Carlos M.");
        // Imagen: mantener placeholder
        holder.ivImage.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.light_gray));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTourClick(position, name, v);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvDates, tvPrice, tvGuideName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_tour_image);
            tvTitle = itemView.findViewById(R.id.tv_tour_title);
            tvDates = itemView.findViewById(R.id.tv_tour_dates);
            tvPrice = itemView.findViewById(R.id.tv_tour_price);
            tvGuideName = itemView.findViewById(R.id.tv_guide_name);
        }
    }
}
