package com.example.droidtour.client;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Company;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CompanyProfileActivity extends AppCompatActivity {

    private static final String TAG = "CompanyProfileActivity";
    
    private ImageView ivCompanyLogo;
    private TextView tvCompanyName;
    private TextView tvBusinessName;
    private TextView tvRuc;
    private TextView tvBusinessType;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvAddress;
    private TextView tvDescription;
    private TextView tvExperienceYears;
    private ChipGroup chipGroupServices;
    
    private FirestoreManager firestoreManager;
    private String companyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_profile);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Obtener companyId del intent
        companyId = getIntent().getStringExtra("company_id");
        if (companyId == null || companyId.isEmpty()) {
            Toast.makeText(this, "Error: ID de empresa no disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestoreManager = FirestoreManager.getInstance();
        
        setupToolbar();
        initializeViews();
        loadCompanyData();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Perfil de Empresa");
        }
    }

    private void initializeViews() {
        ivCompanyLogo = findViewById(R.id.iv_company_logo);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvBusinessName = findViewById(R.id.tv_business_name);
        tvRuc = findViewById(R.id.tv_ruc);
        tvBusinessType = findViewById(R.id.tv_business_type);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvAddress = findViewById(R.id.tv_address);
        tvDescription = findViewById(R.id.tv_description);
        tvExperienceYears = findViewById(R.id.tv_experience_years);
        chipGroupServices = findViewById(R.id.chip_group_services);
    }

    private void loadCompanyData() {
        firestoreManager.getCompanyById(companyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Company company = (Company) result;
                if (company != null) {
                    displayCompanyInfo(company);
                    loadCompanyServices(company);
                } else {
                    Toast.makeText(CompanyProfileActivity.this, "No se encontró la empresa", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar datos de la empresa", e);
                Toast.makeText(CompanyProfileActivity.this, "Error al cargar datos de la empresa", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayCompanyInfo(Company company) {
        // Logo
        if (ivCompanyLogo != null) {
            String logoUrl = company.getLogoUrl();
            if (logoUrl != null && !logoUrl.isEmpty()) {
                Glide.with(this)
                        .load(logoUrl)
                        .placeholder(R.drawable.ic_company)
                        .error(R.drawable.ic_company)
                        .centerInside()
                        .into(ivCompanyLogo);
            } else {
                ivCompanyLogo.setImageResource(R.drawable.ic_company);
            }
        }

        // Nombre comercial (o razón social si no hay comercial)
        String displayName = company.getCommercialName() != null && !company.getCommercialName().isEmpty() ?
                company.getCommercialName() : company.getBusinessName();
        if (tvCompanyName != null) {
            tvCompanyName.setText(displayName != null ? displayName : "Empresa");
        }

        // Razón social
        if (tvBusinessName != null) {
            tvBusinessName.setText(company.getBusinessName() != null ? company.getBusinessName() : "No disponible");
        }

        // RUC
        if (tvRuc != null) {
            tvRuc.setText(company.getRuc() != null ? company.getRuc() : "No disponible");
        }

        // Tipo de negocio
        if (tvBusinessType != null) {
            tvBusinessType.setText(company.getBusinessType() != null ? company.getBusinessType() : "No disponible");
        }

        // Email
        if (tvEmail != null) {
            tvEmail.setText(company.getEmail() != null ? company.getEmail() : "No disponible");
        }

        // Teléfono
        if (tvPhone != null) {
            tvPhone.setText(company.getPhone() != null ? company.getPhone() : "No disponible");
        }

        // Dirección
        if (tvAddress != null) {
            tvAddress.setText(company.getAddress() != null ? company.getAddress() : "No disponible");
        }

        // Descripción
        if (tvDescription != null) {
            tvDescription.setText(company.getDescription() != null && !company.getDescription().isEmpty() ?
                    company.getDescription() : "No hay descripción disponible");
        }

        // Años de experiencia
        if (tvExperienceYears != null) {
            int yearsExperience = 0;
            if (company.getCreatedAt() != null) {
                long diffMillis = new java.util.Date().getTime() - company.getCreatedAt().getTime();
                yearsExperience = (int) (diffMillis / (1000L * 60 * 60 * 24 * 365));
            }
            tvExperienceYears.setText(String.format(Locale.getDefault(), "%d+ años de experiencia", yearsExperience));
        }
    }

    private void loadCompanyServices(Company company) {
        if (chipGroupServices == null) return;
        
        chipGroupServices.removeAllViews();
        
        if (company.getServiceIds() == null || company.getServiceIds().isEmpty()) {
            return;
        }

        firestoreManager.getServicesByCompany(company.getCompanyId(), new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof List) {
                    List<com.example.droidtour.models.Service> services = (List<com.example.droidtour.models.Service>) result;
                    
                    for (com.example.droidtour.models.Service service : services) {
                        Chip chip = new Chip(CompanyProfileActivity.this);
                        chip.setText(service.getName());
                        chip.setChipBackgroundColorResource(R.color.primary_light);
                        chip.setTextColor(ContextCompat.getColor(CompanyProfileActivity.this, R.color.primary));
                        chip.setChipStrokeWidth(0);
                        chip.setChipMinHeight((int) (28 * getResources().getDisplayMetrics().density));
                        chipGroupServices.addView(chip);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar servicios de la empresa", e);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

