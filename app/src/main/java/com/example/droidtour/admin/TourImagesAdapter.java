package com.example.droidtour.admin;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.droidtour.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class TourImagesAdapter extends RecyclerView.Adapter<TourImagesAdapter.ViewHolder> {

    private final List<Uri> imageUris;
    private final OnImageRemoveListener listener;

    public interface OnImageRemoveListener {
        void onImageRemove(int position);
    }

    public TourImagesAdapter(List<Uri> imageUris, OnImageRemoveListener listener) {
        this.imageUris = imageUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        
        // Cargar imagen
        Glide.with(holder.itemView.getContext())
                .load(imageUri)
                .centerCrop()
                .into(holder.ivTourImage);
        
        // Número de imagen
        holder.tvImageNumber.setText(String.valueOf(position + 1));
        
        // Botón eliminar
        holder.btnRemoveImage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageRemove(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris != null ? imageUris.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTourImage;
        MaterialButton btnRemoveImage;
        TextView tvImageNumber;

        ViewHolder(View itemView) {
            super(itemView);
            ivTourImage = itemView.findViewById(R.id.iv_tour_image);
            btnRemoveImage = itemView.findViewById(R.id.btn_remove_image);
            tvImageNumber = itemView.findViewById(R.id.tv_image_number);
        }
    }
}

