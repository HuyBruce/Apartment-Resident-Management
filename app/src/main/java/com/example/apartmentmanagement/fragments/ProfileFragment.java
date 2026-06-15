package com.example.apartmentmanagement.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.utils.UserHelper;
import com.example.apartmentmanagement.models.Resident;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvApartmentChip;
    private EditText etName, etPhone, etEmail, etDob, etGender, etApartment, etMembers, etIdentity;
    private MaterialButton btnSave, btnLoad;
    private View rootView;

    private FirebaseFirestore db;
    private String residentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Dùng lại layout activity_profile
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        residentId = "";


        rootView = view;
        bindViews(view);
        loadProfile();

        btnLoad.setOnClickListener(v -> loadProfile());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void bindViews(View v) {
        tvName          = v.findViewById(R.id.tvName);
        tvApartmentChip = v.findViewById(R.id.tvApartmentChip);

        etName      = v.findViewById(R.id.fieldName).findViewById(R.id.etFieldValue);
        etPhone     = v.findViewById(R.id.fieldPhone).findViewById(R.id.etFieldValue);
        etEmail     = v.findViewById(R.id.fieldEmail).findViewById(R.id.etFieldValue);
        etDob       = v.findViewById(R.id.fieldDob).findViewById(R.id.etFieldValue);
        etGender    = v.findViewById(R.id.fieldGender).findViewById(R.id.etFieldValue);
        etIdentity  = v.findViewById(R.id.fieldIdentity).findViewById(R.id.etFieldValue);
        etApartment = v.findViewById(R.id.fieldApartment).findViewById(R.id.etFieldValue);
        etMembers   = v.findViewById(R.id.fieldMembers).findViewById(R.id.etFieldValue);

        setFieldLabel(v, R.id.fieldName,      com.google.android.material.R.drawable.ic_m3_chip_close, "Họ và tên");
        setFieldLabel(v, R.id.fieldPhone,     android.R.drawable.ic_menu_call,     "Số điện thoại");
        setFieldLabel(v, R.id.fieldEmail,     android.R.drawable.ic_dialog_email,  "Email");
        setFieldLabel(v, R.id.fieldDob,       android.R.drawable.ic_menu_today,    "Ngày sinh");
        setFieldLabel(v, R.id.fieldGender,    android.R.drawable.ic_menu_myplaces, "Giới tính");
        setFieldLabel(v, R.id.fieldIdentity,  android.R.drawable.ic_menu_edit,     "Số CCCD");
        setFieldLabel(v, R.id.fieldApartment, android.R.drawable.ic_menu_compass,  "Số căn hộ");
        setFieldLabel(v, R.id.fieldMembers,   android.R.drawable.ic_menu_manage,   "Số thành viên");

        btnSave = v.findViewById(R.id.btnSave);
        btnLoad = v.findViewById(R.id.btnLoad);
    }

    private void setFieldLabel(View root, int includeId, int iconRes, String label) {
        View field = root.findViewById(includeId);
        ((TextView) field.findViewById(R.id.tvFieldLabel)).setText(label);
        ((ImageView) field.findViewById(R.id.fieldIcon)).setImageResource(iconRes);
    }

    private void setStatCard(View root, int cardId, String value, String label) {
        View card = root.findViewById(cardId);
        if (card == null) return;
        ((TextView) card.findViewById(R.id.tvStatValue)).setText(value);
        ((TextView) card.findViewById(R.id.tvStatLabel)).setText(label);
    }

    private void loadProfile() {
        btnLoad.setEnabled(false);
        UserHelper.getIds((userId, resId) -> {
            residentId = resId;
            db.collection("residents").document(residentId).get()
                    .addOnSuccessListener(doc -> {
                        btnLoad.setEnabled(true);
                        if (doc.exists()) {
                            Resident r = doc.toObject(Resident.class);
                            if (r != null) populateFields(r);
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnLoad.setEnabled(true);
                        Snackbar.make(rootView, "Không thể tải hồ sơ", Snackbar.LENGTH_SHORT).show();
                    });
        }, msg -> {
            btnLoad.setEnabled(true);
            Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show();
        });
    }

    private void populateFields(Resident r) {
        tvName.setText(r.getFull_name());
        tvApartmentChip.setText("Căn hộ " + r.getApartment_number());
        etName.setText(r.getFull_name());
        etPhone.setText(r.getPhone_number());
        etEmail.setText(r.getEmail());
        etDob.setText(r.getDate_of_birth());
        etGender.setText(r.getGender());
        etIdentity.setText(r.getIdentity_number());
        etApartment.setText(r.getApartment_number());
        etMembers.setText(r.getMembers_count());

        String role = r.getRelationship_to_apartment() != null ? r.getRelationship_to_apartment() : "—";
        setStatCard(rootView, R.id.statResidence, role, "Vai trò");

        // Đếm phí chưa đóng
        db.collection("fees").whereEqualTo("resident_id", residentId)
                .whereEqualTo("status", "unpaid").get()
                .addOnSuccessListener(snap ->
                        setStatCard(rootView, R.id.statFee, String.valueOf(snap.size()), "Phí chưa đóng"));

        db.collection("requests").whereEqualTo("resident_id", residentId).get()
                .addOnSuccessListener(snap ->
                        setStatCard(rootView, R.id.statRequest, String.valueOf(snap.size()), "Yêu cầu"));
    }

    private void saveProfile() {
        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Vui lòng nhập họ tên");
            etName.requestFocus();
            return;
        }
        if (etPhone.getText().toString().trim().isEmpty()) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            etPhone.requestFocus();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");

        Map<String, Object> data = new HashMap<>();
        data.put("full_name",        etName.getText().toString().trim());
        data.put("phone_number",     etPhone.getText().toString().trim());
        data.put("email",            etEmail.getText().toString().trim());
        data.put("date_of_birth",    etDob.getText().toString().trim());
        data.put("gender",           etGender.getText().toString().trim());
        data.put("identity_number",  etIdentity.getText().toString().trim());
        data.put("apartment_number", etApartment.getText().toString().trim());
        data.put("members_count",    etMembers.getText().toString().trim());

        db.collection("residents").document(residentId).update(data)
                .addOnSuccessListener(unused -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thay đổi");
                    tvName.setText(etName.getText().toString().trim());
                    tvApartmentChip.setText("Căn hộ " + etApartment.getText().toString().trim());
                    Snackbar.make(rootView, "✓ Hồ sơ đã được cập nhật", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thay đổi");
                    Snackbar.make(rootView, "Cập nhật thất bại", Snackbar.LENGTH_SHORT).show();
                });
    }
}