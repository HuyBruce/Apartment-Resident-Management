package com.example.apartmentmanagement.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.ParkingAdapter;
import com.example.apartmentmanagement.models.ParkingRegistration;
import com.example.apartmentmanagement.utils.PenaltyCalculator;
import com.example.apartmentmanagement.utils.UserHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParkingFragment extends Fragment {

    private RecyclerView recyclerView;
    private ParkingAdapter adapter;
    private List<ParkingRegistration> list = new ArrayList<>();

    private TextView tvSubtitle, tvNoParking, tvBlockReason;
    private View layoutBlocked;
    private ExtendedFloatingActionButton fabAdd;
    private View rootView;

    private FirebaseFirestore db;
    private String residentId = "";
    private boolean isBlocked = false;
    @Override
    public void onResume() {
        super.onResume();
        if (!residentId.isEmpty()) {
            checkParkingLockStatus();
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        rootView = view;

        tvSubtitle    = view.findViewById(R.id.tvParkingSubtitle);
        tvNoParking   = view.findViewById(R.id.tvNoParking);
        layoutBlocked = view.findViewById(R.id.layoutParkingBlocked);
        tvBlockReason = view.findViewById(R.id.tvParkingBlockReason);
        fabAdd        = view.findViewById(R.id.fabAddParking);
        recyclerView  = view.findViewById(R.id.recyclerParking);

        adapter = new ParkingAdapter(requireContext(), list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> checkDebtThenAddVehicle());

        UserHelper.getIds((userId, resId) -> {
            residentId = resId;
            checkParkingLockStatus();
            loadParking();
        }, msg -> Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show());
    }

    private void checkParkingLockStatus() {
        db.collection("fees")
                .whereEqualTo("resident_id", residentId)
                .whereEqualTo("status", "unpaid")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int maxOverdueDays = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String dueDate = doc.getString("due_date");
                        if (dueDate == null) continue;
                        int days = PenaltyCalculator.getOverdueDays(dueDate);
                        if (days > maxOverdueDays) maxOverdueDays = days;
                    }

                    isBlocked = PenaltyCalculator.isServiceRestricted(maxOverdueDays);

                    if (isBlocked) {
                        layoutBlocked.setVisibility(View.VISIBLE);
                        tvBlockReason.setText("Do quá hạn thanh toán phí " +
                                maxOverdueDays + " ngày. Vui lòng thanh toán để mở khóa.");
                        // Tự động set tất cả xe sang trạng thái blocked trong Firestore
                        updateAllVehiclesStatus("blocked",
                                "Công nợ quá hạn " + maxOverdueDays + " ngày");
                    } else {
                        layoutBlocked.setVisibility(View.GONE);
                        updateAllVehiclesStatus("active", "");
                    }
                });
    }

    private void updateAllVehiclesStatus(String status, String reason) {
        db.collection("parking_registrations")
                .whereEqualTo("resident_id", residentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("status", status);
                        update.put("block_reason", reason);
                        db.collection("parking_registrations")
                                .document(doc.getId())
                                .update(update);
                    }
                    loadParking();
                });
    }

    private void loadParking() {
        db.collection("parking_registrations")
                .whereEqualTo("resident_id", residentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    list.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ParkingRegistration p = doc.toObject(ParkingRegistration.class);
                        if (p.getId() == null) p.setId(doc.getId());
                        list.add(p);
                    }
                    adapter.notifyDataSetChanged();
                    tvSubtitle.setText(list.size() + " phương tiện đăng ký");
                    tvNoParking.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void checkDebtThenAddVehicle() {
        if (isBlocked) {
            Snackbar.make(rootView,
                    "⛔ Không thể đăng ký xe mới do dịch vụ giữ xe đang bị khóa vì công nợ. " +
                            "Vui lòng thanh toán phí để tiếp tục.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        showAddVehicleSheet();
    }

    private void showAddVehicleSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext(),
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View sv = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_add_parking, null);
        sheet.setContentView(sv);

        EditText etPlate = sv.findViewById(R.id.etLicensePlate);
        EditText etModel = sv.findViewById(R.id.etVehicleModel);
        MaterialButton btnSubmit = sv.findViewById(R.id.btnSubmitParking);
        MaterialButton btnCancel = sv.findViewById(R.id.btnCancelParking);

        btnCancel.setOnClickListener(v -> sheet.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String plate = etPlate.getText().toString().trim();
            String model = etModel.getText().toString().trim();

            if (plate.isEmpty()) { etPlate.setError("Nhập biển số"); return; }

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(Calendar.getInstance().getTime());

            Map<String, Object> data = new HashMap<>();
            data.put("resident_id",   residentId);
            data.put("vehicle_type",  "Xe máy");
            data.put("license_plate", plate);
            data.put("vehicle_model", model);
            data.put("status",        "active");
            data.put("block_reason",  "");
            data.put("created_at",    now);
            data.put("updated_at",    now);

            db.collection("parking_registrations").add(data)
                    .addOnSuccessListener(ref -> {
                        sheet.dismiss();
                        loadParking();
                        Snackbar.make(rootView, "✓ Đã đăng ký xe", Snackbar.LENGTH_SHORT).show();
                    });
        });

        sheet.show();
    }
}