package com.example.apartmentmanagement.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.example.apartmentmanagement.adapters.VisitorAdapter;
import com.example.apartmentmanagement.utils.UserHelper;
import com.example.apartmentmanagement.models.Visitor;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VisitorFragment extends Fragment {

    private RecyclerView recyclerView;
    private VisitorAdapter adapter;
    private List<Visitor> allVisitors = new ArrayList<>();
    private List<Visitor> filteredVisitors = new ArrayList<>();

    private TextView tvTotalVisitors;
    private View tvEmpty;

    private ChipGroup chipGroup;
    private ExtendedFloatingActionButton fabAdd;
    private View rootView;

    private FirebaseFirestore db;
    private String residentId;
    private int currentFilter = 0; // 0=all, 1=pending, 2=approved

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_visitor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        residentId = "";

        rootView        = view;
        tvTotalVisitors = view.findViewById(R.id.tvTotalVisitors);
        tvEmpty         = view.findViewById(R.id.tvVisitorEmpty);
        chipGroup       = view.findViewById(R.id.chipGroupVisitor);
        fabAdd          = view.findViewById(R.id.fabAddVisitor);
        recyclerView    = view.findViewById(R.id.recyclerVisitors);

        adapter = new VisitorAdapter(requireContext(), filteredVisitors, v -> confirmDelete(v));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if      (id == R.id.chipVisitorAll)      currentFilter = 0;
            else if (id == R.id.chipVisitorPending)  currentFilter = 1;
            else if (id == R.id.chipVisitorApproved) currentFilter = 2;
            applyFilter();
        });

        fabAdd.setOnClickListener(v -> showAddVisitorSheet());

        UserHelper.getIds((userId, resId) -> {
            residentId = resId;
            loadVisitors();
        }, msg -> Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show());
    }

    private void loadVisitors() {
        db.collection("visitors")
                .get()
                .addOnSuccessListener(snapshot -> {
                    allVisitors.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Visitor v = doc.toObject(Visitor.class);
                        if (v.getId() == null) v.setId(doc.getId());
                        // filter thủ công
                        if (residentId.equals(v.getResident_id())) {
                            allVisitors.add(v);
                        }
                    }
                    applyFilter();
                    updateHeader();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rootView, "Không thể tải danh sách khách: " + e.getMessage(),
                                Snackbar.LENGTH_LONG).show());
    }

    private void applyFilter() {
        filteredVisitors.clear();
        for (Visitor v : allVisitors) {
            if      (currentFilter == 0) filteredVisitors.add(v);
            else if (currentFilter == 1 && "pending".equals(v.getStatus()))  filteredVisitors.add(v);
            else if (currentFilter == 2 && "approved".equals(v.getStatus())) filteredVisitors.add(v);
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredVisitors.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateHeader() {
        long pending  = allVisitors.stream().filter(v -> "pending".equals(v.getStatus())).count();
        long approved = allVisitors.stream().filter(v -> "approved".equals(v.getStatus())).count();
        tvTotalVisitors.setText(allVisitors.size() + " lượt đăng ký  •  " +
                pending + " chờ duyệt  •  " + approved + " đã duyệt");
    }

    // ── Add Visitor Bottom Sheet ─────────────────────────────────────────────
    private void showAddVisitorSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext(),
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_add_visitor, null);
        sheet.setContentView(sheetView);

        EditText etName    = sheetView.findViewById(R.id.etVisitorName);
        EditText etPhone   = sheetView.findViewById(R.id.etVisitorPhone);
        EditText etPurpose = sheetView.findViewById(R.id.etVisitorPurpose);
        EditText etDate    = sheetView.findViewById(R.id.etVisitDate);
        EditText etTime    = sheetView.findViewById(R.id.etVisitTime);
        MaterialButton btnSubmit = sheetView.findViewById(R.id.btnSubmitVisitor);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btnCancelVisitor);

        // Date picker
        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                etDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m+1, d));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time picker
        etTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(requireContext(), (tp, h, min) -> {
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        btnCancel.setOnClickListener(v -> sheet.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String name    = etName.getText().toString().trim();
            String phone   = etPhone.getText().toString().trim();
            String purpose = etPurpose.getText().toString().trim();
            String date    = etDate.getText().toString().trim();
            String time    = etTime.getText().toString().trim();

            if (name.isEmpty())    { etName.setError("Nhập tên khách");  return; }
            if (phone.isEmpty())   { etPhone.setError("Nhập SĐT");       return; }
            if (date.isEmpty())    { etDate.setError("Chọn ngày");       return; }
            if (time.isEmpty())    { etTime.setError("Chọn giờ");        return; }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Đang lưu...");

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());

            Map<String, Object> data = new HashMap<>();
            data.put("resident_id",   residentId);
            data.put("visitor_name",  name);
            data.put("visitor_phone", phone);
            data.put("purpose",       purpose);
            data.put("visit_date",    date);
            data.put("visit_time",    time);
            data.put("status",        "pending");
            data.put("created_at",    now);
            data.put("updated_at",    now);

            db.collection("visitors").add(data)
                    .addOnSuccessListener(ref -> {
                        sheet.dismiss();
                        loadVisitors();
                        Snackbar.make(rootView, "✓ Đã đăng ký khách thành công", Snackbar.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Đăng ký");
                        Snackbar.make(rootView, "Lỗi khi lưu. Thử lại.", Snackbar.LENGTH_SHORT).show();
                    });
        });

        sheet.show();
    }

    private void confirmDelete(Visitor v) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa đăng ký khách")
                .setMessage("Bạn có chắc muốn xóa lượt đăng ký của\n" + v.getVisitor_name() + "?")
                .setPositiveButton("Xóa", (d, w) -> deleteVisitor(v))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteVisitor(Visitor v) {
        db.collection("visitors").document(v.getId()).delete()
                .addOnSuccessListener(unused -> {
                    allVisitors.remove(v);
                    applyFilter();
                    updateHeader();
                    Snackbar.make(rootView, "Đã xóa đăng ký", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rootView, "Xóa thất bại", Snackbar.LENGTH_SHORT).show());
    }
}