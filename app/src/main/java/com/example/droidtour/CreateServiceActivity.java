package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class CreateServiceActivity extends AppCompatActivity {
    
    private TextInputEditText etServiceName, etServiceDescription, etServicePrice;
    private MaterialCardView cardServiceImage1, cardServiceImage2;
    private CheckBox cbBreakfast, cbLunch, cbDinner, cbTransport;
    private MaterialButton btnCancel, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_service);
        
        setupToolbar();
        initializeViews();
        setupClickListeners();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        etServiceName = findViewById(R.id.et_service_name);
        etServiceDescription = findViewById(R.id.et_service_description);
        etServicePrice = findViewById(R.id.et_service_price);
        
        cardServiceImage1 = findViewById(R.id.card_service_image1);
        cardServiceImage2 = findViewById(R.id.card_service_image2);
        
        cbBreakfast = findViewById(R.id.cb_breakfast);
        cbLunch = findViewById(R.id.cb_lunch);
        cbDinner = findViewById(R.id.cb_dinner);
        cbTransport = findViewById(R.id.cb_transport);
        
        btnCancel = findViewById(R.id.btn_cancel_service);
        btnSave = findViewById(R.id.btn_save_service);
    }
    
    private void setupClickListeners() {
        cardServiceImage1.setOnClickListener(v -> {
            Toast.makeText(this, "Seleccionar imagen 1 del servicio", Toast.LENGTH_SHORT).show();
            // TODO: Implementar selección de imagen
        });
        
        cardServiceImage2.setOnClickListener(v -> {
            Toast.makeText(this, "Seleccionar imagen 2 del servicio", Toast.LENGTH_SHORT).show();
            // TODO: Implementar selección de imagen
        });
        
        btnCancel.setOnClickListener(v -> finish());
        
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveService();
            }
        });
    }
    

    
    private boolean validateInputs() {
        if (etServiceName.getText().toString().trim().isEmpty()) {
            etServiceName.setError("Ingrese el nombre del servicio");
            return false;
        }
        
        if (etServiceDescription.getText().toString().trim().isEmpty()) {
            etServiceDescription.setError("Ingrese la descripción del servicio");
            return false;
        }
        
        if (etServicePrice.getText().toString().trim().isEmpty()) {
            etServicePrice.setError("Ingrese el precio del servicio");
            return false;
        }
        

        
        return true;
    }
    
    private void saveService() {
        // Recopilar datos del servicio
        String name = etServiceName.getText().toString().trim();
        String description = etServiceDescription.getText().toString().trim();
        String price = etServicePrice.getText().toString().trim();
        
        // Servicios incluidos
        boolean includesBreakfast = cbBreakfast.isChecked();
        boolean includesLunch = cbLunch.isChecked();
        boolean includesDinner = cbDinner.isChecked();
        boolean includesTransport = cbTransport.isChecked();
        
        // TODO: Guardar servicio en base de datos
        Toast.makeText(this, "Servicio '" + name + "' creado exitosamente", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
