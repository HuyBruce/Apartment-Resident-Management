package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FeeAdapter adapter;
    private List<Fee> allFees = new ArrayList<>();
    private List<Fee> filteredFees = new ArrayList<>();

    private TextView tvTotalAmount, tvTotalLabel, tvEmpty;
    private ChipGroup chipGroup;
    private View rootView;

    private FirebaseFirestore db;
    private String residentId;

    // Filter: 0=Tất cả, 1=Chưa đóng, 2=Đã đóng
    private int currentFilter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fee);

        db = FirebaseFirestore.getInstance();
        residentId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "1";

        setupToolbar();
        bindViews();
        setupRecyclerView();
        setupChipFilter();
        loadFees();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void bindViews() {
        rootView     = findViewById(android.R.id.content);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvTotalLabel  = findViewById(R.id.tvTotalLabel);
        tvEmpty       = findViewById(R.id.tvEmpty);
        chipGroup     = findViewById(R.id.chipGroup);
        recyclerView  = findViewById(R.id.recyclerFees);
    }

    private void setupRecyclerView() {
        adapter = new FeeAdapter(this, filteredFees, fee -> confirmPayment(fee));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupChipFilter() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipAll)     currentFilter = 0;
            else if (id == R.id.chipUnpaid) currentFilter = 1;
            else if (id == R.id.chipPaid)   currentFilter = 2;
            applyFilter();
        });
    }

    private void loadFees() {
        db.collection("fees")
                .whereEqualTo("resident_id", residentId)
                .get()
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
            // Hiển thị tổng đã đóng
            long totalPaid = 0;
            for (Fee f : filteredFees) totalPaid += f.getAmount();
            tvTotalAmount.setText(fmt.format(totalPaid) + " đ");
            tvTotalLabel.setText("Tổng đã đóng");
        } else {
            // Hiển thị tổng chưa đóng
            long totalUnpaid = 0;
            for (Fee f : allFees)
                if ("unpaid".equals(f.getStatus())) totalUnpaid += f.getAmount();
            tvTotalAmount.setText(fmt.format(totalUnpaid) + " đ");
            tvTotalLabel.setText("Tổng cần đóng");
        }
    }

    private void confirmPayment(Fee fee) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận thanh toán")
                .setMessage("Bạn xác nhận đã đóng:\n\n" +
                        fee.getCategory() + "\n" +
                        fmt.format(fee.getAmount()) + " đ\n\n" +
                        "Hạn: " + fee.getDue_date())
                .setPositiveButton("Xác nhận", (dialog, which) -> markAsPaid(fee))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void markAsPaid(Fee fee) {
        db.collection("fees").document(fee.getId())
                .update("status", "paid")
                .addOnSuccessListener(unused -> {
                    fee.setStatus("paid");
                    applyFilter();
                    Snackbar.make(rootView, "✓ Đã cập nhật trạng thái thanh toán", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rootView, "Cập nhật thất bại. Thử lại sau.", Snackbar.LENGTH_SHORT).show());
    }
}