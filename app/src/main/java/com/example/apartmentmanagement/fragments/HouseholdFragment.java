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
import com.example.apartmentmanagement.adapters.HouseholdMemberAdapter;
import com.example.apartmentmanagement.models.HouseholdMember;
import com.example.apartmentmanagement.utils.UserHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

public class HouseholdFragment extends Fragment {

    private RecyclerView recyclerView;
    private HouseholdMemberAdapter adapter;
    private List<HouseholdMember> members = new ArrayList<>();

    private TextView tvStatTotal, tvStatChildren, tvStatElderly, tvNoMembers;
    private ExtendedFloatingActionButton fabAdd;
    private View rootView;

    private FirebaseFirestore db;
    private String residentId = "";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_household, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        rootView = view;

        tvStatTotal    = view.findViewById(R.id.tvStatTotal);
        tvStatChildren = view.findViewById(R.id.tvStatChildren);
        tvStatElderly  = view.findViewById(R.id.tvStatElderly);
        tvNoMembers    = view.findViewById(R.id.tvNoMembers);
        fabAdd         = view.findViewById(R.id.fabAddMember);
        recyclerView   = view.findViewById(R.id.recyclerMembers);

        adapter = new HouseholdMemberAdapter(requireContext(), members,
                m -> confirmDelete(m));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddMemberSheet());

        UserHelper.getIds((userId, resId) -> {
            residentId = resId;
            loadMembers();
        }, msg -> Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show());
    }

    private void loadMembers() {
        db.collection("household_members")
                .whereEqualTo("resident_id", residentId)
                .whereEqualTo("is_active", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    members.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        HouseholdMember m = doc.toObject(HouseholdMember.class);
                        if (m.getId() == null) m.setId(doc.getId());
                        members.add(m);
                    }
                    adapter.notifyDataSetChanged();
                    updateStats();
                    tvNoMembers.setVisibility(members.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rootView, "Không thể tải danh sách", Snackbar.LENGTH_SHORT).show());
    }

    private void updateStats() {
        long children = members.stream()
                .filter(m -> m.getAge() < 15).count();
        long elderly  = members.stream()
                .filter(m -> m.getAge() >= 60).count();

        tvStatTotal.setText(members.size() + " thành viên");
        tvStatChildren.setText(children + " trẻ em");
        tvStatElderly.setText(elderly + " người cao tuổi");

        // Cập nhật members_count trong residents
        db.collection("residents").document(residentId)
                .update("members_count", String.valueOf(members.size()));
    }

    private void showAddMemberSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext(),
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View sv = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_add_member, null);
        sheet.setContentView(sv);

        EditText etName     = sv.findViewById(R.id.etMemberName);
        EditText etDob      = sv.findViewById(R.id.etMemberDob);
        EditText etGender   = sv.findViewById(R.id.etMemberGender);
        EditText etRelation = sv.findViewById(R.id.etMemberRelation);
        EditText etIdNumber = sv.findViewById(R.id.etMemberIdNumber);
        MaterialButton btnSubmit = sv.findViewById(R.id.btnSubmitMember);
        MaterialButton btnCancel = sv.findViewById(R.id.btnCancelMember);

        btnCancel.setOnClickListener(v -> sheet.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String dob      = etDob.getText().toString().trim();
            String gender   = etGender.getText().toString().trim();
            String relation = etRelation.getText().toString().trim();
            String idNum    = etIdNumber.getText().toString().trim();

            if (name.isEmpty())     { etName.setError("Nhập họ tên"); return; }
            if (dob.isEmpty())      { etDob.setError("Nhập ngày sinh (yyyy-MM-dd)"); return; }
            if (relation.isEmpty()) { etRelation.setError("Nhập quan hệ"); return; }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Đang lưu...");

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(Calendar.getInstance().getTime());

            Map<String, Object> data = new HashMap<>();
            data.put("resident_id",     residentId);
            data.put("full_name",       name);
            data.put("date_of_birth",   dob);
            data.put("gender",          gender);
            data.put("relationship",    relation);
            data.put("identity_number", idNum);
            data.put("is_active",       true);
            data.put("created_at",      now);

            db.collection("household_members").add(data)
                    .addOnSuccessListener(ref -> {
                        sheet.dismiss();
                        loadMembers();
                        Snackbar.make(rootView, "✓ Đã thêm thành viên",
                                Snackbar.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Thêm");
                        Snackbar.make(rootView, "Lỗi khi lưu", Snackbar.LENGTH_SHORT).show();
                    });
        });
        sheet.show();
    }

    private void confirmDelete(HouseholdMember m) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa thành viên")
                .setMessage("Xóa " + m.getFull_name() + " khỏi hộ gia đình?")
                .setPositiveButton("Xóa", (d, w) -> deleteMember(m))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteMember(HouseholdMember m) {
        db.collection("household_members").document(m.getId())
                .update("is_active", false)
                .addOnSuccessListener(unused -> {
                    members.remove(m);
                    adapter.notifyDataSetChanged();
                    updateStats();
                    Snackbar.make(rootView, "Đã xóa thành viên",
                            Snackbar.LENGTH_SHORT).show();
                });
    }
}