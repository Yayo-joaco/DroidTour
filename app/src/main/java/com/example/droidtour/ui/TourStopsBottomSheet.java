package com.example.droidtour.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Tour;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom Sheet para Admin - Muestra paradas del tour (solo lectura)
 * NUEVO: Basado en GuideStopsManagementActivity - carga una sola vez sin listener
 */
public class TourStopsBottomSheet extends BottomSheetDialogFragment {
    
    private static final String ARG_GUIDE_ID = "guide_id";
    private static final String ARG_TOUR_NAME = "tour_name";
    private static final String TAG = "TourStopsBottomSheet";
    
    private String guideId;
    private String tourName;
    private RecyclerView rvStops;
    private TextView tvTourName, tvMeetingPoint, tvTotalStops, tvCompletedStops;
    private StopsAdapter adapter;
    private List<Tour.TourStop> stops = new ArrayList<>();
    private FirestoreManager firestoreManager;
    private Tour currentTour;
    
    public static TourStopsBottomSheet newInstance(String guideId, String tourName) {
        TourStopsBottomSheet fragment = new TourStopsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_GUIDE_ID, guideId);
        args.putString(ARG_TOUR_NAME, tourName);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            guideId = getArguments().getString(ARG_GUIDE_ID);
            tourName = getArguments().getString(ARG_TOUR_NAME);
        }
        firestoreManager = FirestoreManager.getInstance();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_tour_stops, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tvTourName = view.findViewById(R.id.tv_tour_name);
        tvMeetingPoint = view.findViewById(R.id.tv_meeting_point);
        tvTotalStops = view.findViewById(R.id.tv_total_stops);
        tvCompletedStops = view.findViewById(R.id.tv_completed_stops);
        rvStops = view.findViewById(R.id.rv_stops);
        
        tvTourName.setText(tourName);
        
        setupRecyclerView();
        loadTourData();
    }
    
    private void setupRecyclerView() {
        rvStops.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StopsAdapter(stops);
        rvStops.setAdapter(adapter);
    }
    
    private void loadTourData() {
        Log.d(TAG, "🔍 Buscando tour para guideId: " + guideId);
        
        // Buscar tour por guideId (como en GuideStopsManagementActivity)
        firestoreManager.getToursByGuide(guideId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Tour> tours = (List<Tour>) result;
                Log.d(TAG, "📄 Tours encontrados: " + (tours != null ? tours.size() : 0));
                
                if (tours != null && !tours.isEmpty()) {
                    currentTour = tours.get(0);
                    Log.d(TAG, "✅ Tour cargado: " + currentTour.getTourName());
                    displayTourData();
                } else {
                    Log.e(TAG, "❌ No se encontró tour para este guía");
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error cargando tour", e);
            }
        });
    }
    
    private void displayTourData() {
        if (currentTour == null) return;
        
        // Nombre del tour
        tvTourName.setText(currentTour.getTourName());
        
        // Punto de encuentro
        if (currentTour.getMeetingPoint() != null) {
            tvMeetingPoint.setText(currentTour.getMeetingPoint());
        }
        
        // Paradas
        stops.clear();
        if (currentTour.getStops() != null && !currentTour.getStops().isEmpty()) {
            stops.addAll(currentTour.getStops());
            Log.d(TAG, "📍 Total paradas: " + stops.size());
        }
        adapter.notifyDataSetChanged();
        
        // Contadores
        updateStopCounters();
    }
    
    private void updateStopCounters() {
        int total = stops.size();
        int completed = 0;
        
        for (Tour.TourStop stop : stops) {
            if (stop.getCompleted() != null && stop.getCompleted()) {
                completed++;
            }
        }
        
        Log.d(TAG, "📊 Contadores: Total=" + total + ", Completadas=" + completed);
        tvTotalStops.setText(String.valueOf(total));
        tvCompletedStops.setText(String.valueOf(completed));
    }
    
    // Adapter para las paradas
    private static class StopsAdapter extends RecyclerView.Adapter<StopsAdapter.ViewHolder> {
        private List<Tour.TourStop> stops;
        
        StopsAdapter(List<Tour.TourStop> stops) {
            this.stops = stops;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stop_tracking, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Tour.TourStop stop = stops.get(position);
            
            holder.tvStopNumber.setText(String.valueOf(stop.getOrder()));
            holder.tvStopName.setText(stop.getName());
            
            // Mostrar duración en minutos (igual que en GuideStopsManagementActivity)
            if (stop.getStopDuration() != null && stop.getStopDuration() > 0) {
                holder.tvStopTime.setText(stop.getStopDuration() + " min");
                holder.tvStopTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvStopTime.setVisibility(View.GONE);
            }
            
            // Estado: completada o pendiente
            boolean isCompleted = stop.getCompleted() != null && stop.getCompleted();
            if (isCompleted) {
                holder.tvStatus.setText("✓ Completada");
                holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark, null));
                holder.tvStopNumber.setBackgroundResource(R.drawable.circle_green);
            } else {
                holder.tvStatus.setText("Pendiente");
                holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark, null));
                holder.tvStopNumber.setBackgroundResource(R.drawable.circle_red);
            }
        }
        
        @Override
        public int getItemCount() {
            return stops.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStopNumber, tvStopName, tvStopTime, tvStatus;
            
            ViewHolder(View view) {
                super(view);
                tvStopNumber = view.findViewById(R.id.tv_stop_number);
                tvStopName = view.findViewById(R.id.tv_stop_name);
                tvStopTime = view.findViewById(R.id.tv_stop_time);
                tvStatus = view.findViewById(R.id.tv_status);
            }
        }
    }
}
