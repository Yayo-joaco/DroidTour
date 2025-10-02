package com.example.droidtour;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.util.HashSet;
import java.util.Set;

public class SelectLanguageRegisterActivity extends AppCompatActivity {
    private MaterialAutoCompleteTextView spinnerIdiomas;
    private ChipGroup chipGroupIdiomas;
    private Set<String> idiomasSeleccionados = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_language_register);

        spinnerIdiomas = findViewById(R.id.spinnerIdiomas);
        chipGroupIdiomas = findViewById(R.id.chipGroupIdiomas);

        // Lista de idiomas sugeridos
        String[] idiomas = new String[]{
                "Español", "Inglés", "Francés", "Portugués", "Quechua", "Aymara", "Alemán", "Italiano", "Chino", "Japonés", "Ruso", "Árabe", "Turco", "Coreano", "Hindi", "Guaraní", "Catalán", "Holandés", "Sueco", "Noruego", "Danés", "Finlandés", "Polaco", "Griego", "Hebreo", "Ucraniano", "Rumano", "Húngaro", "Checo", "Eslovaco", "Búlgaro", "Serbio", "Croata", "Esloveno", "Estonio", "Lituano", "Letón", "Islandés", "Macedonio", "Tailandés", "Vietnamita", "Malayo", "Indonesio", "Filipino", "Swahili", "Zulu", "Afrikáans", "Persa", "Pashto", "Urdu", "Bengalí", "Punjabi", "Tamil", "Telugu", "Kannada", "Maratí", "Gujarati", "Nepalí", "Mongol", "Camboyano", "Lao", "Birmano", "Georgiano", "Armenio", "Kazajo", "Uzbeco", "Tayiko", "Kirguís", "Turcomano", "Azerí", "Maltes", "Luxemburgués", "Esperanto"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, idiomas);
        spinnerIdiomas.setAdapter(adapter);

        spinnerIdiomas.setOnItemClickListener((parent, view, position, id) -> {
            String idioma = adapter.getItem(position);
            if (!idiomasSeleccionados.contains(idioma)) {
                agregarChipIdioma(idioma);
                idiomasSeleccionados.add(idioma);
            } else {
                Toast.makeText(this, "Ya seleccionaste ese idioma", Toast.LENGTH_SHORT).show();
            }
            spinnerIdiomas.setText(""); // Limpiar campo
        });
    }

    private void agregarChipIdioma(String idioma) {
        Chip chip = new Chip(this);
        chip.setText(idioma);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupIdiomas.removeView(chip);
            idiomasSeleccionados.remove(idioma);
        });
        chipGroupIdiomas.addView(chip);
    }
}

