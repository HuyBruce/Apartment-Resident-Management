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
import com.example.apartmentmanagement.adapters.FeeAdapter;
import com.example.apartmentmanagement.models.Fee;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.apartmentmanagement.utils.UserHelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeeFragment extends Fragment {

    private RecyclerView recyclerView;
    private FeeAdapter adapter;
    private List<Fee> allFees = new ArrayList<>();
    private List<Fee> filteredFees = new ArrayList<>();

    private TextView tvTotalAmount, tvTotalLabel, tvEmpty;
    private ChipGroup chipGroup;
    private View rootView;

    private FirebaseFirestore db;
    private String residentId;
    private int currentFilter = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_fee, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        residentId = "";

        rootView      = view;
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        tvTotalLabel  = view.findViewById(R.id.tvTotalLabel);
        tvEmpty       = view.findViewById(R.id.tvEmpty);
        chipGroup     = view.findViewById(R.id.chipGroup);
        recyclerView  = view.findViewById(R.id.recyclerFees);

        // Ẩn toolbar khi dùng trong Fragment (đã có BottomNav)
        View appBar = view.findViewById(R.id.appBarLayout);
        if (appBar != null) appBar.setVisibility(View.GONE);

        adapter = new FeeAdapter(requireContext(), filteredFees, fee -> confirmPayment(fee));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipAll)         currentFilter = 0;
            else if (id == R.id.chipUnpaid) currentFilter = 1;
            else if (id == R.id.chipPaid)   currentFilter = 2;
            applyFilter();
        });
        UserHelper.getIds((userId, resId) -> {
            residentId = resId;
            loadFees();
        }, msg -> Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show());

    }

    private void loadFees() {
        db.collection("fees").whereEqualTo("resident_id", residentId).get()
                .addOnSuccessListener(snapshot -> {
                    allFees.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Fee fee = doc.toObject(Fee.class);
                        if (fee.getId() == null) fee.setId(doc.getId());
                        allFees.add(fee);
                    }
                    applyFilter();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rootView, "Không thể tải danh sách phí", Snackbar.LENGTH_SHORT).show());
    }

    private void applyFilter() {
        filteredFees.clear();
        for (Fee fee : allFees) {
            if (currentFilter == 0) filteredFees.add(fee);
            else if (currentFilter == 1 && "unpaid".equals(fee.getStatus())) filteredFees.add(fee);
            else if (currentFilter == 2 && "paid".equals(fee.getStatus())) filteredFees.add(fee);
        }
        adapter.notifyDataSetChanged();
        updateSummary();
        tvEmpty.setVisibility(filteredFees.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateSummary() {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        if (currentFilter == 2) {
            long total = 0;
            for (Fee f : filteredFees) total += f.getAmount();
            tvTotalAmount.setText(fmt.format(total) + " đ");
            tvTotalLabel.setText("Tổng đã đóng");
        } else {
            long total = 0;
            for (Fee f : allFees) if ("unpaid".equals(f.getStatus())) total += f.getAmount();
            tvTotalAmount.setText(fmt.format(total) + " đ");
            tvTotalLabel.setText("Tổng cần đóng");
        }
    }

    private void confirmPayment(Fee fee) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận thanh toán")
                .setMessage("Bạn xác nhận đã đóng:\n\n" +
                        fee.getCategory() + "\n" +
                        fmt.format(fee.getAmount()) + " đ\n\nHạn: " + fee.getDue_date())
                .setPositiveButton("Xác nhận", (d, w) -> markAsPaid(fee))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void markAsPaid(Fee fee) {
        db.collection("fees").document(fee.getId())
                .update("status", "paid")
                .addOnSuccessListener(unused -> {
                    fee.setStatus("paid");
                    applyFilter();
                    Snackbar.make(rootView, "✓ Đã cập nhật thanh toán", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rootView, "Cập nhật thất bại", Snackbar.LENGTH_SHORT).show());
    }
}