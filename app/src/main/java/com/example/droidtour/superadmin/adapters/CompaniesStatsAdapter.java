package com.example.droidtour.superadmin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.R;
import com.example.droidtour.superadmin.models.CompanyStats;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para mostrar empresas con estadísticas de reservas
 */
public class CompaniesStatsAdapter extends RecyclerView.Adapter<CompaniesStatsAdapter.ViewHolder> {
    
    private final List<CompanyStats> companiesStats;
    
    public CompaniesStatsAdapter(List<CompanyStats> companiesStats) {
        this.companiesStats = companiesStats;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company_stats, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CompanyStats stats = companiesStats.get(position);
        
        // Nombre de la empresa
        String displayName = stats.getCompany().getCommercialName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = stats.getCompany().getBusinessName();
        }
        holder.tvCompanyName.setText(displayName != null ? displayName : "Sin nombre");
        
        // Ubicación
        String location = stats.getCompany().getAddress();
        holder.tvLocation.setText(location != null && !location.isEmpty() ? location : "Ubicación no especificada");
        
        // Estadísticas
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        holder.tvTotalReservations.setText(numberFormat.format(stats.getTotalReservations()));
        holder.tvConfirmedReservations.setText(numberFormat.format(stats.getConfirmedReservations()));
        holder.tvCompletedReservations.setText(numberFormat.format(stats.getCompletedReservations()));
        
        // Revenue
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(java.util.Currency.getInstance("PEN"));
        holder.tvTotalRevenue.setText(currencyFormat.format(stats.getTotalRevenue()));
    }
    
    @Override
    public int getItemCount() {
        return companiesStats != null ? companiesStats.size() : 0;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCompanyName;
        TextView tvLocation;
        TextView tvTotalReservations;
        TextView tvConfirmedReservations;
        TextView tvCompletedReservations;
        TextView tvTotalRevenue;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCompanyName = itemView.findViewById(R.id.tv_company_name);
            tvLocation = itemView.findViewById(R.id.tv_company_location);
            tvTotalReservations = itemView.findViewById(R.id.tv_total_reservations);
            tvConfirmedReservations = itemView.findViewById(R.id.tv_confirmed_reservations);
            tvCompletedReservations = itemView.findViewById(R.id.tv_completed_reservations);
            tvTotalRevenue = itemView.findViewById(R.id.tv_total_revenue);
        }
    }
}

