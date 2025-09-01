package com.example.droidtour;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SearchGuidesFragment extends Fragment {
    
    private TextInputEditText etDateFrom, etDateTo;
    private MaterialButton btnSearchGuides;
    private RecyclerView rvAvailableGuides;
    private View layoutEmptyGuides;
    private List<String> selectedLanguages = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_guides, container, false);
        
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
        // TODO: Configurar adapter para guías disponibles
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
        
        // TODO: Realizar búsqueda de guías con los filtros
        Toast.makeText(getContext(), "Buscando guías disponibles...", Toast.LENGTH_SHORT).show();
        showEmptyState(true);
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            rvAvailableGuides.setVisibility(View.GONE);
            layoutEmptyGuides.setVisibility(View.VISIBLE);
        } else {
            rvAvailableGuides.setVisibility(View.VISIBLE);
            layoutEmptyGuides.setVisibility(View.GONE);
        }
    }
}
