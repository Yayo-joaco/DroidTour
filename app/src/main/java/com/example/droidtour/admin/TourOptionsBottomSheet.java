package com.example.droidtour.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.droidtour.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TourOptionsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TOUR_NAME = "arg_tour_name";

    public static TourOptionsBottomSheet newInstance(String tourName) {
        TourOptionsBottomSheet bs = new TourOptionsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TOUR_NAME, tourName);
        bs.setArguments(args);
        return bs;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Usar el layout de detalles principal
        View view = inflater.inflate(R.layout.bottom_sheet_tour_details, container, false);
        String tourName = getArguments() != null ? getArguments().getString(ARG_TOUR_NAME) : "Tour";

        TextView tvTitle = view.findViewById(R.id.tv_sheet_title);
        ImageButton btnClose = view.findViewById(R.id.btn_close);

        tvTitle.setText(tourName);

        btnClose.setOnClickListener(v -> dismiss());

        // Mantener opciones Edit/Delete como acciones en el bottom sheet de detalles si lo deseas
        // Se pueden agregar botones en ese layout y referenciarlos aquí.

        return view;
    }
}
