package com.example.droidtour;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Guide;
import com.example.droidtour.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SearchGuidesFragment extends Fragment {
    
    private static final String TAG = "SearchGuidesFragment";
    private TextInputEditText etDateFrom, etDateTo;
    private MaterialButton btnSearchGuides;
    private RecyclerView rvAvailableGuides;
    private View layoutEmptyGuides;
    private List<String> selectedLanguages = new ArrayList<>();
    private FirestoreManager firestoreManager;
    private List<Guide> guidesList = new ArrayList<>();
    private GuidesAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_guides, container, false);
        
        firestoreManager = FirestoreManager.getInstance();
        
        initializeViews(view);
        setupClickListeners(view);
        setupRecyclerView();
        
        return view;
    }
    
    private void initializeViews(View view) {
        etDateFrom = view.findViewById(R.id.et_date_from);
        etDateTo = view.findViewById(R.id.et_date_to);
        btnSearchGuides = view.findViewById(R.id.btn_search_guides);
        rvAvailableGuides = view.findViewById(R.id.rv_available_guides);
        layoutEmptyGuides = view.findViewById(R.id.layout_empty_guides);
    }
    
    private void setupClickListeners(View view) {
        etDateFrom.setOnClickListener(v -> showDatePicker(etDateFrom));
        etDateTo.setOnClickListener(v -> showDatePicker(etDateTo));
        
        btnSearchGuides.setOnClickListener(v -> searchGuides());
        
        // Setup language chips
        Chip chipSpanish = view.findViewById(R.id.chip_spanish);
        Chip chipEnglish = view.findViewById(R.id.chip_english);
        Chip chipFrench = view.findViewById(R.id.chip_french);
        Chip chipGerman = view.findViewById(R.id.chip_german);
        
        chipSpanish.setOnCheckedChangeListener((buttonView, isChecked) -> 
            updateLanguageSelection("Español", isChecked));
        chipEnglish.setOnCheckedChangeListener((buttonView, isChecked) -> 
            updateLanguageSelection("Inglés", isChecked));
        chipFrench.setOnCheckedChangeListener((buttonView, isChecked) -> 
            updateLanguageSelection("Francés", isChecked));
        chipGerman.setOnCheckedChangeListener((buttonView, isChecked) -> 
            updateLanguageSelection("Alemán", isChecked));
    }
    
    private void setupRecyclerView() {
        rvAvailableGuides.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GuidesAdapter(guidesList);
        rvAvailableGuides.setAdapter(adapter);
    }
    
    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            getContext(),
            (view, selectedYear, selectedMonth, selectedDay) -> {
                String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                editText.setText(date);
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }
    
    private void updateLanguageSelection(String language, boolean isSelected) {
        if (isSelected) {
            if (!selectedLanguages.contains(language)) {
                selectedLanguages.add(language);
            }
        } else {
            selectedLanguages.remove(language);
        }
    }
    
    private void searchGuides() {
        String dateFrom = etDateFrom.getText().toString().trim();
        String dateTo = etDateTo.getText().toString().trim();
        
        if (dateFrom.isEmpty() || dateTo.isEmpty()) {
            Toast.makeText(getContext(), "Seleccione las fechas", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedLanguages.isEmpty()) {
            Toast.makeText(getContext(), "Seleccione al menos un idioma", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getContext(), "Buscando guías disponibles...", Toast.LENGTH_SHORT).show();
        
        // Buscar guías desde la colección guides en Firebase
        firestoreManager.getAllGuides(new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                List<Guide> allGuides = (List<Guide>) result;
                guidesList.clear();
                
                // Filtrar por idiomas seleccionados
                for (Guide guide : allGuides) {
                    List<String> guideLanguages = guide.getLanguages();
                    if (guideLanguages != null) {
                        for (String lang : selectedLanguages) {
                            String langLower = lang.toLowerCase();
                            for (String gl : guideLanguages) {
                                if (gl != null && (gl.equalsIgnoreCase(lang) || gl.toLowerCase().contains(langLower))) {
                                    guidesList.add(guide);
                                    break;
                                }
                            }
                        }
                    }
                }
                
                if (guidesList.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    adapter.updateData(guidesList);
                }
                
                Log.d(TAG, "Guías encontrados: " + guidesList.size());
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error buscando guías", e);
                Toast.makeText(getContext(), "Error al buscar guías", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }
    
    private void showEmptyState(boolean show) {
        if (layoutEmptyGuides == null || rvAvailableGuides == null) return;
        
        if (show) {
            rvAvailableGuides.setVisibility(View.GONE);
            layoutEmptyGuides.setVisibility(View.VISIBLE);
        } else {
            rvAvailableGuides.setVisibility(View.VISIBLE);
            layoutEmptyGuides.setVisibility(View.GONE);
        }
    }
    
    // Adapter para mostrar guías
    private static class GuidesAdapter extends RecyclerView.Adapter<GuidesAdapter.ViewHolder> {
        private List<Guide> guides;
        
        GuidesAdapter(List<Guide> guides) {
            this.guides = guides != null ? guides : new ArrayList<>();
        }
        
        void updateData(List<Guide> newData) {
            this.guides = newData != null ? newData : new ArrayList<>();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Guide guide = guides.get(position);
            holder.text1.setText(guide.getGuideId() != null ? guide.getGuideId() : "Guía");
            
            List<String> langs = guide.getLanguages();
            String langStr = langs != null ? String.join(", ", langs) : "";
            holder.text2.setText("Idiomas: " + langStr);
        }
        
        @Override
        public int getItemCount() {
            return guides != null ? guides.size() : 0;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View view) {
                super(view);
                text1 = view.findViewById(android.R.id.text1);
                text2 = view.findViewById(android.R.id.text2);
            }
        }
    }
}
