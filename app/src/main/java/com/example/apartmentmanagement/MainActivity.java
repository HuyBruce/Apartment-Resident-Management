package com.example.apartmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.apartmentmanagement.activities.LoginActivity;
import com.example.apartmentmanagement.fragments.DashboardFragment;
import com.example.apartmentmanagement.fragments.EventsFragment;
import com.example.apartmentmanagement.fragments.FeeFragment;
import com.example.apartmentmanagement.fragments.HistoryFragment;
import com.example.apartmentmanagement.fragments.HouseholdFragment;
import com.example.apartmentmanagement.fragments.ProfileFragment;
import com.example.apartmentmanagement.fragments.VisitorFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    private Fragment dashboardFragment = new DashboardFragment();
    private Fragment visitorFragment   = new VisitorFragment();
    private Fragment feeFragment       = new FeeFragment();
    private Fragment householdFragment = new HouseholdFragment();
    private Fragment profileFragment   = new ProfileFragment();
    private Fragment eventsFragment    = new EventsFragment();
    private Fragment historyFragment   = new HistoryFragment();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottomNav);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, historyFragment,   "history").hide(historyFragment)
                .add(R.id.fragmentContainer, eventsFragment,    "events").hide(eventsFragment)
                .add(R.id.fragmentContainer, householdFragment, "household").hide(householdFragment)
                .add(R.id.fragmentContainer, visitorFragment,   "visitor").hide(visitorFragment)
                .add(R.id.fragmentContainer, feeFragment,       "fee").hide(feeFragment)
                .add(R.id.fragmentContainer, profileFragment,   "profile").hide(profileFragment)
                .add(R.id.fragmentContainer, dashboardFragment, "dashboard")
                .commit();

        activeFragment = dashboardFragment;

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_dashboard) switchTo(dashboardFragment);
            else if (id == R.id.nav_visitor)   switchTo(visitorFragment);
            else if (id == R.id.nav_fee)       switchTo(feeFragment);
            else if (id == R.id.nav_household) switchTo(householdFragment);
            else if (id == R.id.nav_profile)   switchTo(profileFragment);
            return true;
        });
    }

    private void switchTo(Fragment target) {
        if (target == activeFragment) return;
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        activeFragment = target;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    goToLogin();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    public void navigateTo(Fragment target) {
        switchTo(target);
        if (target == visitorFragment)    bottomNav.setSelectedItemId(R.id.nav_visitor);
        else if (target == feeFragment)   bottomNav.setSelectedItemId(R.id.nav_fee);
        else if (target == profileFragment)   bottomNav.setSelectedItemId(R.id.nav_profile);
        else if (target == householdFragment) bottomNav.setSelectedItemId(R.id.nav_household);
        else if (target == dashboardFragment) bottomNav.setSelectedItemId(R.id.nav_dashboard);
    }

    public Fragment getEventsFragment()  { return eventsFragment; }
    public Fragment getHistoryFragment() { return historyFragment; }
}