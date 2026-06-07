package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.models.Resident;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // Header views
    private TextView tvName, tvStatus, tvApartmentChip;

    // Field EditTexts (from included layouts)
    private EditText etName, etPhone, etEmail, etDob, etGender;
    private EditText etApartment, etMembers;

    // Buttons
    private MaterialButton btnSave, btnLoad;

    // Firebase
    private FirebaseFirestore db;
    private String residentId;

    // Root view for Snackbar
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();

        if (getIntent().hasExtra("resident_id")) {
            residentId = getIntent().getStringExtra("resident_id");
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            residentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            residentId = "1";
        }

        setupToolbar();
        bindViews();
        loadProfile();

        btnLoad.setOnClickListener(v -> loadProfile());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void bindViews() {
        rootView = findViewById(android.R.id.content);
        tvName = findViewById(R.id.tvName);
        tvApartmentChip = findViewById(R.id.tvApartmentChip);

        etName   = findViewById(R.id.fieldName).findViewById(R.id.etFieldValue);
        etPhone  = findViewById(R.id.fieldPhone).findViewById(R.id.etFieldValue);
        etEmail  = findViewById(R.id.fieldEmail).findViewById(R.id.etFieldValue);
        etDob    = findViewById(R.id.fieldDob).findViewById(R.id.etFieldValue);
        etGender = findViewById(R.id.fieldGender).findViewById(R.id.etFieldValue);

        // Apartment fields
        etApartment = findViewById(R.id.fieldApartment).findViewById(R.id.etFieldValue);
        etMembers   = findViewById(R.id.fieldMembers).findViewById(R.id.etFieldValue);

        // Set labels
        setFieldLabel(R.id.fieldName,      com.google.android.material.R.drawable.ic_m3_chip_close,  "Họ và tên");
        setFieldLabel(R.id.fieldPhone,     android.R.drawable.ic_menu_call,     "Số điện thoại");
        setFieldLabel(R.id.fieldEmail,     android.R.drawable.ic_dialog_email,  "Email");
        setFieldLabel(R.id.fieldDob,       android.R.drawable.ic_menu_today,    "Ngày sinh");
        setFieldLabel(R.id.fieldGender,    android.R.drawable.ic_menu_myplaces, "Giới tính");
        setFieldLabel(R.id.fieldApartment, android.R.drawable.ic_menu_compass,  "Số căn hộ");
        setFieldLabel(R.id.fieldMembers,   android.R.drawable.ic_menu_manage,   "Số thành viên");
        btnSave = findViewById(R.id.btnSave);
        btnLoad = findViewById(R.id.btnLoad);
    }

    private void setFieldLabel(int includeId, int iconRes, String label) {
        View field = findViewById(includeId);
        ((TextView) field.findViewById(R.id.tvFieldLabel)).setText(label);
        ((ImageView) field.findViewById(R.id.fieldIcon)).setImageResource(iconRes);
    }

    private void loadProfile() {
        btnLoad.setEnabled(false);

        db.collection("residents")
                .document(residentId)
                .get()
                .addOnSuccessListener(doc -> {
                    btnLoad.setEnabled(true);
                    if (doc.exists()) {
                        Resident r = doc.toObject(Resident.class);
                        if (r != null) populateFields(r);
                    }
                })
                .addOnFailureListener(e -> {
                    btnLoad.setEnabled(true);
                    showSnackbar("Không thể tải hồ sơ. Thử lại sau.");
                });
    }

    private void populateFields(Resident r) {
        // Header
        tvName.setText(r.getFull_name());
        tvApartmentChip.setText("Căn hộ " + r.getApartment_number());

        // Fields
        etName.setText(r.getFull_name());
        etPhone.setText(r.getPhone_number());
        etEmail.setText(r.getEmail());
        etDob.setText(r.getDate_of_birth());
        etGender.setText(r.getGender());
        etApartment.setText(r.getApartment_number());
        etMembers.setText(r.getMembers_count());
    }

    private void saveProfile() {
        if (!validateInputs()) return;

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");

        Map<String, Object> data = new HashMap<>();
        data.put("full_name",        etName.getText().toString().trim());
        data.put("phone_number",     etPhone.getText().toString().trim());
        data.put("email",            etEmail.getText().toString().trim());
        data.put("date_of_birth",    etDob.getText().toString().trim());
        data.put("gender",           etGender.getText().toString().trim());
        data.put("apartment_number", etApartment.getText().toString().trim());
        data.put("members_count",    etMembers.getText().toString().trim());

        db.collection("residents")
                .document(residentId)
                .update(data)
                .addOnSuccessListener(unused -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thay đổi");
                    // Update header live
                    tvName.setText(etName.getText().toString().trim());
                    tvApartmentChip.setText("Căn hộ " + etApartment.getText().toString().trim());
                    showSnackbar("✓ Hồ sơ đã được cập nhật");
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thay đổi");
                    showSnackbar("Cập nhật thất bại. Kiểm tra kết nối mạng.");
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

    private void showSnackbar(String msg) {
        Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show();
    }
}