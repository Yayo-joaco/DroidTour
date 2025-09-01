package com.example.droidtour;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class GuideManagementActivity extends AppCompatActivity {
    
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private GuideManagementPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_management);
        
        setupToolbar();
        initializeViews();
        setupViewPager();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    private void initializeViews() {
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
    }
    
    private void setupViewPager() {
        pagerAdapter = new GuideManagementPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Tours Activos");
                    break;
                case 1:
                    tab.setText("Buscar Guías");
                    break;
                case 2:
                    tab.setText("Propuestas");
                    break;
            }
        }).attach();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Adapter para ViewPager2
    private static class GuideManagementPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        
        public GuideManagementPagerAdapter(androidx.fragment.app.FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new ActiveToursFragment();
                case 1:
                    return new SearchGuidesFragment();
                case 2:
                    return new GuideProposalsFragment();
                default:
                    return new ActiveToursFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
