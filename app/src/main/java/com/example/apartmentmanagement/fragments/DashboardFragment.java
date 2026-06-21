package com.example.apartmentmanagement.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.ActivityLogAdapter;
import com.example.apartmentmanagement.adapters.FeeAdapter;
import com.example.apartmentmanagement.MainActivity;

import com.example.apartmentmanagement.models.ActivityLog;
import com.example.apartmentmanagement.models.Fee;
import com.example.apartmentmanagement.models.Resident;
import com.example.apartmentmanagement.utils.UserHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView tvGreeting, tvResidentName, tvApartmentNumber;
    private TextView tvUnpaidCount, tvUnpaidAmount;
    private TextView tvVisitorCount, tvVisitorPending;
    private TextView tvLogCount;
    private TextView tvSeeAllFees, tvSeeAllLogs, tvNoFees, tvNoLogs;
    private RecyclerView recyclerFees, recyclerLogs;
    private FeeAdapter feeAdapter;
    private ActivityLogAdapter logAdapter;
    private List<Fee> unpaidFees = new ArrayList<>();
    private List<ActivityLog> recentLogs = new ArrayList<>();
    private ImageButton btnRefresh;

    private FirebaseFirestore db;
    private String residentId = "";
    private View rootView;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        rootView = view;

        bindViews(view);
        setupRecyclerViews();
        setupClickListeners();

        UserHelper.getIds((userId, resId) -> {
            residentId = resId;
            loadDashboard(userId);
        }, msg -> {});
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!residentId.isEmpty()) {
            loadUnpaidFees();
        }
    }

    private void bindViews(View v) {
        tvGreeting        = v.findViewById(R.id.tvDashGreeting);
        tvResidentName    = v.findViewById(R.id.tvDashResidentName);
        tvApartmentNumber = v.findViewById(R.id.tvDashApartment);
        tvUnpaidCount     = v.findViewById(R.id.tvUnpaidCount);
        tvUnpaidAmount    = v.findViewById(R.id.tvUnpaidAmount);
        tvVisitorCount    = v.findViewById(R.id.tvVisitorCount);
        tvVisitorPending  = v.findViewById(R.id.tvVisitorPending);
        tvLogCount        = v.findViewById(R.id.tvLogCount);
        recyclerFees      = v.findViewById(R.id.recyclerDashFees);
        recyclerLogs      = v.findViewById(R.id.recyclerDashLogs);
        tvSeeAllFees      = v.findViewById(R.id.tvSeeAllFees);
        tvSeeAllLogs      = v.findViewById(R.id.tvSeeAllLogs);
        tvNoFees          = v.findViewById(R.id.tvNoFees);
        tvNoLogs          = v.findViewById(R.id.tvNoLogs);
        btnRefresh        = v.findViewById(R.id.btnRefresh);
    }

    private void setupRecyclerViews() {
        feeAdapter = new FeeAdapter(requireContext(), unpaidFees, fee -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_fee);
        });
        recyclerFees.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerFees.setAdapter(feeAdapter);
        recyclerFees.setNestedScrollingEnabled(false);

        logAdapter = new ActivityLogAdapter(requireContext(), recentLogs);
        recyclerLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLogs.setAdapter(logAdapter);
        recyclerLogs.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        tvSeeAllFees.setOnClickListener(v -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_fee);
        });

        // Xem tất cả logs → History
        tvSeeAllLogs.setOnClickListener(v -> {
            MainActivity ma = (MainActivity) requireActivity();
            ma.navigateTo(ma.getHistoryFragment());
        });

        rootView.findViewById(R.id.cardStatFee).setOnClickListener(v -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_fee);
        });

        // Card Khách → VisitorFragment
        rootView.findViewById(R.id.cardStatVisitor).setOnClickListener(v -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_visitor);
        });

        // Card Lịch sử → HistoryFragment
        rootView.findViewById(R.id.cardStatLog).setOnClickListener(v -> {
            MainActivity ma = (MainActivity) requireActivity();
            ma.navigateTo(ma.getHistoryFragment());
        });

        rootView.findViewById(R.id.quickProfile).setOnClickListener(v -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_profile);
        });

        rootView.findViewById(R.id.quickFee).setOnClickListener(v -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_fee);
        });

        rootView.findViewById(R.id.quickVisitor).setOnClickListener(v -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_visitor);
        });

        // Quick Sự kiện → EventsFragment
        rootView.findViewById(R.id.quickHistory).setOnClickListener(v -> {
            MainActivity ma = (MainActivity) requireActivity();
            ma.navigateTo(ma.getEventsFragment());
        });

        btnRefresh.setOnClickListener(v -> {
            btnRefresh.animate().rotation(360f).setDuration(500).start();
            btnRefresh.setRotation(0f);
            UserHelper.getIds((userId, resId) -> {
                residentId = resId;
                loadDashboard(userId);
            }, msg -> {});
        });
    }

    private void loadDashboard(String userId) {
        loadResidentInfo();
        loadUnpaidFees();
        loadVisitorStats();
        loadRecentLogs(userId);
    }

    private void loadResidentInfo() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = "☀️ Chào buổi sáng";
        else if (hour < 18) greeting = "🌤 Chào buổi chiều";
        else                greeting = "🌙 Chào buổi tối";
        tvGreeting.setText(greeting);

        db.collection("residents").document(residentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Resident r = doc.toObject(Resident.class);
                        if (r != null) {
                            tvResidentName.setText(r.getFull_name() != null ? r.getFull_name() : "Cư dân");
                            tvApartmentNumber.setText("🏠 Căn hộ " +
                                    (r.getApartment_number() != null ? r.getApartment_number() : "—"));
                        }
                    }
                });
    }

    private void loadUnpaidFees() {
        db.collection("fees")
                .whereEqualTo("resident_id", residentId)
                .whereEqualTo("status", "unpaid")
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    List<Fee> all = new ArrayList<>();
                    long totalAmount = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Fee fee = doc.toObject(Fee.class);
                        if (fee.getId() == null) fee.setId(doc.getId());
                        all.add(fee);
                        totalAmount += fee.getAmount();
                    }
                    tvUnpaidCount.setText(String.valueOf(all.size()));
                    NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
                    tvUnpaidAmount.setText(fmt.format(totalAmount) + "đ");

                    unpaidFees.clear();
                    int count = Math.min(2, all.size());
                    for (int i = 0; i < count; i++) unpaidFees.add(all.get(i));
                    feeAdapter.notifyDataSetChanged();

                    tvNoFees.setVisibility(unpaidFees.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerFees.setVisibility(unpaidFees.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void loadVisitorStats() {
        db.collection("visitors").get()
                .addOnSuccessListener(snapshot -> {
                    int total = 0, pending = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String rid = doc.getString("resident_id");
                        if (residentId.equals(rid)) {
                            total++;
                            if ("pending".equals(doc.getString("status"))) pending++;
                        }
                    }
                    tvVisitorCount.setText(String.valueOf(total));
                    tvVisitorPending.setText(pending > 0 ? pending + " chờ duyệt" : "Tất cả đã duyệt");
                });
    }

    private void loadRecentLogs(String userId) {
        db.collection("activity_logs")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    recentLogs.clear();
                    List<ActivityLog> all = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ActivityLog log = doc.toObject(ActivityLog.class);
                        if (log.getId() == null) log.setId(doc.getId());
                        all.add(log);
                    }
                    tvLogCount.setText(all.size() + " hoạt động");
                    int count = Math.min(3, all.size());
                    for (int i = 0; i < count; i++) recentLogs.add(all.get(i));
                    logAdapter.notifyDataSetChanged();
                    tvNoLogs.setVisibility(recentLogs.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerLogs.setVisibility(recentLogs.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }
}