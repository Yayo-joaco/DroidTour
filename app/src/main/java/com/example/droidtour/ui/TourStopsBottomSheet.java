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
import com.example.droidtour.models.Tour;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom Sheet para mostrar las paradas del tour con actualización en tiempo real
 */
public class TourStopsBottomSheet extends BottomSheetDialogFragment {
    
    private static final String ARG_TOUR_ID = "tour_id";
    private static final String ARG_TOUR_NAME = "tour_name";
    private static final String TAG = "TourStopsBottomSheet";
    
    private String tourId;
    private String tourName;
    private RecyclerView rvStops;
    private TextView tvTourName, tvMeetingPoint, tvTotalStops, tvCompletedStops;
    private StopsAdapter adapter;
    private List<Tour.TourStop> stops = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration tourListener;
    private OnStopUpdatedListener listener;
    
    public interface OnStopUpdatedListener {
        void onStopsUpdated(List<Tour.TourStop> stops, String meetingPoint);
    }
    
    public static TourStopsBottomSheet newInstance(String tourId, String tourName) {
        TourStopsBottomSheet fragment = new TourStopsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TOUR_ID, tourId);
        args.putString(ARG_TOUR_NAME, tourName);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setOnStopUpdatedListener(OnStopUpdatedListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tourId = getArguments().getString(ARG_TOUR_ID);
            tourName = getArguments().getString(ARG_TOUR_NAME);
        }
        db = FirebaseFirestore.getInstance();
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
        setupRealtimeListener();
    }
    
    private void setupRecyclerView() {
        rvStops.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StopsAdapter(stops);
        rvStops.setAdapter(adapter);
    }
    
    private void setupRealtimeListener() {
        // Listener en tiempo real para actualizar cuando el guía complete paradas
        tourListener = db.collection("Tours").document(tourId)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening to tour updates", error);
                    return;
                }
                
                if (snapshot != null && snapshot.exists()) {
                    Tour tour = snapshot.toObject(Tour.class);
                    if (tour != null) {
                        updateStopsData(tour);
                    }
                }
            });
    }
    
    private void updateStopsData(Tour tour) {
        Log.d(TAG, "🔄 Actualizando datos del tour: " + tour.getTourName());
        
        // Actualizar punto de encuentro
        if (tour.getMeetingPoint() != null) {
            tvMeetingPoint.setText(tour.getMeetingPoint());
            Log.d(TAG, "📍 Punto de encuentro: " + tour.getMeetingPoint());
        }
        
        // Actualizar paradas
        stops.clear();
        if (tour.getStops() != null) {
            stops.addAll(tour.getStops());
            Log.d(TAG, "📍 Total paradas recibidas: " + tour.getStops().size());
        }
        adapter.notifyDataSetChanged();
        
        // Calcular contadores con log detallado
        int total = stops.size();
        int completed = 0;
        for (Tour.TourStop stop : stops) {
            boolean isCompleted = stop.getCompleted() != null && stop.getCompleted();
            Log.d(TAG, "  - " + stop.getName() + ": completed=" + stop.getCompleted() + " -> " + (isCompleted ? "✅" : "❌"));
            if (isCompleted) {
                completed++;
            }
        }
        
        Log.d(TAG, "📊 Contadores: Total=" + total + ", Completadas=" + completed);
        tvTotalStops.setText(String.valueOf(total));
        tvCompletedStops.setText(String.valueOf(completed));
        
        // Notificar al listener para actualizar el mapa
        if (listener != null) {
            listener.onStopsUpdated(stops, tour.getMeetingPoint());
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tourListener != null) {
            tourListener.remove();
        }
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
