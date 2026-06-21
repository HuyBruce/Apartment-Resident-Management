package com.example.apartmentmanagement.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.apartmentmanagement.R;
import com.google.android.material.button.MaterialButton;

public class ServicesFragment extends Fragment {

    private Fragment visitorFragment   = new VisitorFragment();
    private Fragment parkingFragment   = new ParkingFragment();
    private Fragment complaintFragment = new ComplaintsFragment();
    private Fragment activeSubFragment;

    private MaterialButton btnTabVisitor, btnTabParking, btnTabComplaint;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_services, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnTabVisitor   = view.findViewById(R.id.btnTabVisitor);
        btnTabParking   = view.findViewById(R.id.btnTabParking);
        btnTabComplaint = view.findViewById(R.id.btnTabComplaint);

        getChildFragmentManager().beginTransaction()
                .add(R.id.servicesSubContainer, complaintFragment, "complaint").hide(complaintFragment)
                .add(R.id.servicesSubContainer, parkingFragment, "parking").hide(parkingFragment)
                .add(R.id.servicesSubContainer, visitorFragment, "visitor")
                .commit();

        activeSubFragment = visitorFragment;
        updateTabUI(0);

        btnTabVisitor.setOnClickListener(v -> switchSubTab(visitorFragment, 0));
        btnTabParking.setOnClickListener(v -> switchSubTab(parkingFragment, 1));
        btnTabComplaint.setOnClickListener(v -> switchSubTab(complaintFragment, 2));
    }

    private void switchSubTab(Fragment target, int tabIndex) {
        if (target == activeSubFragment) return;
        getChildFragmentManager().beginTransaction()
                .hide(activeSubFragment)
                .show(target)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        activeSubFragment = target;
        updateTabUI(tabIndex);
    }

    private void updateTabUI(int activeIndex) {
        MaterialButton[] buttons = { btnTabVisitor, btnTabParking, btnTabComplaint };
        for (int i = 0; i < buttons.length; i++) {
            if (i == activeIndex) {
                buttons[i].setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF1A2744));
                buttons[i].setTextColor(0xFFFFFFFF);
            } else {
                buttons[i].setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFF0F2F5));
                buttons[i].setTextColor(0xFF1A2744);
            }
        }
    }
}