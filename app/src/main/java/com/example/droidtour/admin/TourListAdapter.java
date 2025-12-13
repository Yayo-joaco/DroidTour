package com.example.droidtour.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.R;
import java.util.List;

public class TourListAdapter extends RecyclerView.Adapter<TourListAdapter.ViewHolder> {

    public interface OnTourClickListener {
        void onTourClick(int position, String tourName, View anchorView);
    }

    private List<String> items;
    private OnTourClickListener listener;

    public TourListAdapter(List<String> items, OnTourClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tour_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = items.get(position);
        holder.tvName.setText(name);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTourClick(position, name, v);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_tour_name);
        }
    }
}

