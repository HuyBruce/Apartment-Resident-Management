package com.example.apartmentmanagement;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.apartmentmanagement.fragments.DashboardFragment;
import com.example.apartmentmanagement.fragments.FeeFragment;
import com.example.apartmentmanagement.fragments.HistoryFragment;
import com.example.apartmentmanagement.fragments.ProfileFragment;
import com.example.apartmentmanagement.fragments.VisitorFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment;

            if (item.getItemId() == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else if (item.getItemId() == R.id.nav_fee) {
                fragment = new FeeFragment();
            } else if (item.getItemId() == R.id.nav_visitor) {
                fragment = new VisitorFragment();
            } else if (item.getItemId() == R.id.nav_history) {
                fragment = new HistoryFragment();
            } else {
                fragment = new DashboardFragment();
            }

            showFragment(fragment);
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
