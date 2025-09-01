package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import android.view.LayoutInflater;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

public class GuideActiveToursActivity extends AppCompatActivity {
    
    private TabLayout tabLayout;
    private RecyclerView rvMyTours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_active_tours);
        
        setupToolbar();
        initializeViews();
        setupTabs();
        setupRecycler();
        loadToursForTab(0);
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Mis Tours");
    }
    
    private void initializeViews() {
        tabLayout = findViewById(R.id.tab_layout);
        rvMyTours = findViewById(R.id.rv_my_tours);
    }
    
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Activos"));
        tabLayout.addTab(tabLayout.newTab().setText("Programados"));
        tabLayout.addTab(tabLayout.newTab().setText("Completados"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadToursForTab(tab.getPosition());
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void setupRecycler() {
        rvMyTours.setLayoutManager(new LinearLayoutManager(this));
        rvMyTours.setAdapter(new ExampleMyToursAdapter());
    }
    
    private void loadToursForTab(int tabPosition) {
        String status = "";
        switch (tabPosition) {
            case 0: status = "activos"; break;
            case 1: status = "programados"; break;
            case 2: status = "completados"; break;
        }
        Toast.makeText(this, "Cargando tours " + status + "...", Toast.LENGTH_SHORT).show();
        // TODO: Cargar tours según el estado seleccionado
        rvMyTours.getAdapter().notifyDataSetChanged();
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

// Adaptador con ejemplos que continúan el flujo de Ofertas aceptadas
class ExampleMyToursAdapter extends RecyclerView.Adapter<ExampleMyToursAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guide_active_tour, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        android.view.View item = holder.itemView;
        android.widget.TextView name = item.findViewById(R.id.tv_tour_name);
        android.widget.TextView date = item.findViewById(R.id.tv_tour_date);
        android.widget.TextView time = item.findViewById(R.id.tv_tour_time);
        android.widget.TextView participants = item.findViewById(R.id.tv_participants_count);
        android.widget.TextView status = item.findViewById(R.id.tv_tour_status);
        android.widget.TextView progress = item.findViewById(R.id.tv_tour_progress);
        android.widget.TextView amount = item.findViewById(R.id.tv_payment_amount);
        android.view.View fabScan = item.findViewById(R.id.fab_scan_qr);

        switch (position % 3) {
            case 0:
                name.setText("Inka Travel Peru - City Tour Centro Histórico");
                date.setText("Hoy, 15 Dic");
                time.setText("09:30 - 12:30");
                participants.setText("6 personas");
                status.setText("EN PROGRESO");
                progress.setText("Punto 2 de 4");
                amount.setText("S/. 200");
                break;
            case 1:
                name.setText("Lima Adventure - Barranco & Miraflores");
                date.setText("Mañana, 16 Dic");
                time.setText("08:00 - 12:00");
                participants.setText("10 personas");
                status.setText("PROGRAMADO");
                progress.setText("Inicio 08:00");
                amount.setText("S/. 180");
                break;
            default:
                name.setText("Cusco Heritage - Valle Sagrado");
                date.setText("Ayer, 14 Dic");
                time.setText("14:00 - 19:00");
                participants.setText("8 personas");
                status.setText("COMPLETADO");
                progress.setText("Finalizado");
                amount.setText("S/. 250");
        }

        fabScan.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(item.getContext(), QRScannerActivity.class);
            item.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(android.view.View itemView) { super(itemView); }
    }
}
