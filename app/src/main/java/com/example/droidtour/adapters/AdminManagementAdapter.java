package com.example.droidtour.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.example.droidtour.R;
import java.util.List;

/**
 * Adapter para mostrar opciones de gestión en el dashboard de admin
 */
public class AdminManagementAdapter extends RecyclerView.Adapter<AdminManagementAdapter.ManagementViewHolder> {
    
    private Context context;
    private List<ManagementItem> managementItems;
    
    public AdminManagementAdapter(Context context, List<ManagementItem> managementItems) {
        this.context = context;
        this.managementItems = managementItems;
    }
    
    @NonNull
    @Override
    public ManagementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_management, parent, false);
        return new ManagementViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ManagementViewHolder holder, int position) {
        ManagementItem item = managementItems.get(position);
        
        holder.tvTitle.setText(item.getTitle());
        holder.tvDescription.setText(item.getDescription());
        holder.ivIcon.setImageResource(item.getIconResource());
        
        holder.cardView.setOnClickListener(v -> {
            if (item.getTargetActivity() != null) {
                Intent intent = new Intent(context, item.getTargetActivity());
                context.startActivity(intent);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return managementItems.size();
    }
    
    static class ManagementViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivIcon;
        TextView tvTitle, tvDescription;
        
        public ManagementViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_management_item);
            ivIcon = itemView.findViewById(R.id.iv_management_icon);
            tvTitle = itemView.findViewById(R.id.tv_management_title);
            tvDescription = itemView.findViewById(R.id.tv_management_description);
        }
    }
    
    /**
     * Clase para representar un item de gestión
     */
    public static class ManagementItem {
        private String title;
        private String description;
        private int iconResource;
        private Class<?> targetActivity;
        
        public ManagementItem(String title, String description, int iconResource, Class<?> targetActivity) {
            this.title = title;
            this.description = description;
            this.iconResource = iconResource;
            this.targetActivity = targetActivity;
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public int getIconResource() { return iconResource; }
        public Class<?> getTargetActivity() { return targetActivity; }
    }
}
