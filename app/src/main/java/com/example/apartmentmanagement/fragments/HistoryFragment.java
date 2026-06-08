package com.example.apartmentmanagement.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.ActivityLogAdapter;
import com.example.apartmentmanagement.utils.UserHelper;
import com.example.apartmentmanagement.models.ActivityLog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private ActivityLogAdapter adapter;
    private List<ActivityLog> allLogs = new ArrayList<>();
    private List<ActivityLog> filteredLogs = new ArrayList<>();

    private TextView tvTotalLogs;
    private View tvEmpty;
    private ChipGroup chipGroup;
    private View rootView;

    private FirebaseFirestore db;
    private String userId;
    private String currentFilter = "ALL";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        userId = "";


        rootView     = view;
        tvTotalLogs  = view.findViewById(R.id.tvTotalLogs);
        tvEmpty      = view.findViewById(R.id.tvHistoryEmpty);
        chipGroup    = view.findViewById(R.id.chipGroupHistory);
        recyclerView = view.findViewById(R.id.recyclerHistory);

        adapter = new ActivityLogAdapter(requireContext(), filteredLogs);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if      (id == R.id.chipHistoryAll)     currentFilter = "ALL";
            else if (id == R.id.chipHistoryRequest) currentFilter = "CREATE_REQUEST";
            else if (id == R.id.chipHistoryFee)     currentFilter = "PAY_FEE";
            else if (id == R.id.chipHistoryVisitor) currentFilter = "REGISTER_VISITOR";
            applyFilter();
        });

        UserHelper.getIds((uId, resId) -> {
            userId = uId; // history dùng user_id (doc ID của users collection)
            loadLogs();
        }, msg -> Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show());
    }

    private void loadLogs() {
        db.collection("activity_logs")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    allLogs.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ActivityLog log = doc.toObject(ActivityLog.class);
                        if (log.getId() == null) log.setId(doc.getId());
                        allLogs.add(log);
                    }
                    applyFilter();
                    tvTotalLogs.setText(allLogs.size() + " hoạt động");
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rootView, "Không thể tải lịch sử", Snackbar.LENGTH_SHORT).show());
    }

    private void applyFilter() {
        filteredLogs.clear();
        for (ActivityLog log : allLogs) {
            if ("ALL".equals(currentFilter)) {
                filteredLogs.add(log);
            } else if (currentFilter.equals(log.getAction())) {
                filteredLogs.add(log);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredLogs.isEmpty() ? View.VISIBLE : View.GONE);
    }
}