package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.apartmentmanagement.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvStatus, tvApartmentChip;
    private EditText etName, etPhone, etEmail, etDob, etGender, etApartment, etMembers, etIdentity;
    private MaterialButton btnSave, btnLoad;
    private FirebaseFirestore db;
    private View rootView;

    private String role;
    private String profileDocId;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        role = getIntent().getStringExtra("role");
        if (role == null || role.trim().isEmpty()) role = "resident";
        isAdmin = "admin".equalsIgnoreCase(role);
        profileDocId = resolveProfileDocId();

        setupToolbar();
        bindViews();
        configureForRole();
        loadProfile();

        btnLoad.setOnClickListener(v -> loadProfile());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private String resolveProfileDocId() {
        int userId = getIntent().getIntExtra("user_id", 1);
        if (isAdmin) return String.valueOf(userId);

        int residentId = getIntent().getIntExtra("resident_id", userId);
        if (residentId > 0) return String.valueOf(residentId);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return "1";
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(isAdmin ? "Hồ sơ admin" : "Hồ sơ cư dân");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isAdmin ? "Hồ sơ admin" : "Hồ sơ cư dân");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        rootView = findViewById(android.R.id.content);
        tvName = findViewById(R.id.tvName);
        tvStatus = findViewById(R.id.tvStatus);
        tvApartmentChip = findViewById(R.id.tvApartmentChip);

        etName = fieldValue(R.id.fieldName);
        etPhone = fieldValue(R.id.fieldPhone);
        etEmail = fieldValue(R.id.fieldEmail);
        etDob = fieldValue(R.id.fieldDob);
        etGender = fieldValue(R.id.fieldGender);
        etIdentity = fieldValue(R.id.fieldIdentity);
        etApartment = fieldValue(R.id.fieldApartment);
        etMembers = fieldValue(R.id.fieldMembers);

        setFieldLabel(R.id.fieldName, R.drawable.ic_person, "Họ và tên");
        setFieldLabel(R.id.fieldPhone, R.drawable.ic_phone, "Số điện thoại");
        setFieldLabel(R.id.fieldEmail, R.drawable.ic_email, "Email");
        setFieldLabel(R.id.fieldDob, R.drawable.ic_calendar, "Ngày sinh");
        setFieldLabel(R.id.fieldGender, R.drawable.ic_gender, "Giới tính");
        setFieldLabel(R.id.fieldIdentity, R.drawable.ic_shield_check, "CCCD/CMND");
        setFieldLabel(R.id.fieldApartment, R.drawable.ic_building, "Số căn hộ");
        setFieldLabel(R.id.fieldMembers, R.drawable.ic_group, "Số thành viên");

        btnSave = findViewById(R.id.btnSave);
        btnLoad = findViewById(R.id.btnLoad);
    }

    private EditText fieldValue(int includeId) {
        return findViewById(includeId).findViewById(R.id.etFieldValue);
    }

    private void setFieldLabel(int includeId, int iconRes, String label) {
        View field = findViewById(includeId);
        ((TextView) field.findViewById(R.id.tvFieldLabel)).setText(label);
        ((ImageView) field.findViewById(R.id.fieldIcon)).setImageResource(iconRes);
    }

    private void configureForRole() {
        if (!isAdmin) {
            tvStatus.setText("Cư dân đang hoạt động");
            return;
        }

        tvStatus.setText("Quản trị viên");
        tvApartmentChip.setText("Admin");
        setVisible(R.id.fieldDob, false);
        setVisible(R.id.fieldGender, false);
        setVisible(R.id.fieldIdentity, false);
        setVisible(R.id.cardApartment, false);

        View statResidence = findViewById(R.id.statResidence);
        if (statResidence != null && statResidence.getParent() instanceof View) {
            ((View) statResidence.getParent()).setVisibility(View.GONE);
        }
    }

    private void loadProfile() {
        btnLoad.setEnabled(false);
        String collection = isAdmin ? "users" : "residents";

        db.collection(collection)
                .document(profileDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    btnLoad.setEnabled(true);
                    if (doc.exists()) {
                        populateFromMap(doc.getData());
                    } else {
                        populateFallback();
                    }
                })
                .addOnFailureListener(e -> {
                    btnLoad.setEnabled(true);
                    populateFallback();
                    showSnackbar("Không thể tải hồ sơ. Vui lòng thử lại.");
                });
    }

    private void populateFromMap(Map<String, Object> data) {
        if (data == null) {
            populateFallback();
            return;
        }

        String name = value(data.get("full_name"), getIntent().getStringExtra("full_name"));
        String phone = value(data.get("phone_number"), "");
        String email = value(data.get("email"), "");

        tvName.setText(value(name, isAdmin ? "Admin" : "Cư dân"));
        etName.setText(name);
        etPhone.setText(phone);
        etEmail.setText(email);

        if (isAdmin) {
            tvApartmentChip.setText(value(data.get("role"), "admin"));
            return;
        }

        etDob.setText(value(data.get("date_of_birth"), ""));
        etGender.setText(value(data.get("gender"), ""));
        etIdentity.setText(value(data.get("identity_number"), ""));
        etApartment.setText(value(data.get("apartment_number"), value(data.get("apartment_code"), "")));
        etMembers.setText(value(data.get("members_count"), ""));
        tvApartmentChip.setText("Căn hộ " + value(etApartment.getText(), ""));
    }

    private void populateFallback() {
        String name = getIntent().getStringExtra("full_name");
        if (name == null || name.trim().isEmpty()) name = isAdmin ? "Admin" : "Cư dân";

        tvName.setText(name);
        tvApartmentChip.setText(isAdmin ? "Admin" : "Căn hộ");
        etName.setText(name);
    }

    private void saveProfile() {
        if (!validateInputs()) return;

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");

        Map<String, Object> data = new HashMap<>();
        data.put("full_name", etName.getText().toString().trim());
        data.put("phone_number", etPhone.getText().toString().trim());
        data.put("email", etEmail.getText().toString().trim());

        if (isAdmin) {
            data.put("role", "admin");
            data.put("is_active", 1);
        } else {
            data.put("date_of_birth", etDob.getText().toString().trim());
            data.put("gender", etGender.getText().toString().trim());
            data.put("identity_number", etIdentity.getText().toString().trim());
            data.put("apartment_number", etApartment.getText().toString().trim());
            data.put("members_count", etMembers.getText().toString().trim());
        }

        String collection = isAdmin ? "users" : "residents";
        db.collection(collection)
                .document(profileDocId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thay đổi");
                    tvName.setText(etName.getText().toString().trim());
                    if (!isAdmin) {
                        tvApartmentChip.setText("Căn hộ " + etApartment.getText().toString().trim());
                    }
                    showSnackbar("Hồ sơ đã được cập nhật");
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thay đổi");
                    showSnackbar("Cập nhật thất bại. Vui lòng kiểm tra kết nối mạng.");
                });
    }

    private boolean validateInputs() {
        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Vui lòng nhập họ tên");
            etName.requestFocus();
            return false;
        }
        if (etPhone.getText().toString().trim().isEmpty()) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            etPhone.requestFocus();
            return false;
        }
        return true;
    }

    private void setVisible(int id, boolean visible) {
        View view = findViewById(id);
        if (view != null) view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private String value(Object value, String fallback) {
        if (value == null) return fallback == null ? "" : fallback;
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? (fallback == null ? "" : fallback) : text;
    }

    private void showSnackbar(String msg) {
        Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show();
    }
}
