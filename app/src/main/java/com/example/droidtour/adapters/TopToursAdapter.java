package com.example.droidtour.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.R;
import java.util.List;
import java.util.Locale;

public class TopToursAdapter extends RecyclerView.Adapter<TopToursAdapter.ViewHolder> {
    
    public static class TopTour {
        private String tourId;
        private String tourName;
        private int salesCount;
        private double totalRevenue;
        
        public TopTour(String tourId, String tourName, int salesCount, double totalRevenue) {
            this.tourId = tourId;
            this.tourName = tourName;
            this.salesCount = salesCount;
            this.totalRevenue = totalRevenue;
        }
        
        public String getTourId() { return tourId; }
        public String getTourName() { return tourName; }
        public int getSalesCount() { return salesCount; }
        public double getTotalRevenue() { return totalRevenue; }
    }
    
    private List<TopTour> topTours;
    
    public TopToursAdapter(List<TopTour> topTours) {
        this.topTours = topTours != null ? topTours : new java.util.ArrayList<>();
    }
    
    public void updateData(List<TopTour> newTopTours) {
        this.topTours = newTopTours != null ? newTopTours : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_tour, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TopTour tour = topTours.get(position);
        
        // Posición (1, 2, 3, etc.)
        holder.tvPosition.setText(String.valueOf(position + 1));
        
        // Nombre del tour
        holder.tvTourName.setText(tour.getTourName() != null ? tour.getTourName() : "Sin nombre");
        
        // Cantidad de ventas
        String salesText = tour.getSalesCount() == 1 
            ? "1 reserva" 
            : tour.getSalesCount() + " reservas";
        holder.tvSalesCount.setText(salesText);
        
        // Ingresos del tour
        holder.tvTourRevenue.setText(String.format(Locale.getDefault(), "S/. %.0f", tour.getTotalRevenue()));
    }
    
    @Override
    public int getItemCount() {
        return topTours != null ? topTours.size() : 0;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPosition;
        TextView tvTourName;
        TextView tvSalesCount;
        TextView tvTourRevenue;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.tv_position);
            tvTourName = itemView.findViewById(R.id.tv_tour_name);
            tvSalesCount = itemView.findViewById(R.id.tv_sales_count);
            tvTourRevenue = itemView.findViewById(R.id.tv_tour_revenue);
        }
    }
}

