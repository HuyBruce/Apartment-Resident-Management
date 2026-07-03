package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagement.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminHouseholdMemberFormActivity extends AppCompatActivity {

    public static final String EXTRA_APARTMENT_ID = "apartment_id";
    public static final String EXTRA_APARTMENT_CODE = "apartment_code";
    public static final String EXTRA_BUILDING = "building";
    public static final String EXTRA_FLOOR = "floor";
    public static final String EXTRA_OWNER_RESIDENT_ID = "owner_resident_id";

    private static final String[] GENDER_OPTIONS = {"Nam", "Nữ", "Khác"};
    private static final String[] RELATION_OPTIONS = {"Vợ/Chồng", "Con cái", "Cha/Mẹ", "Ông/Bà", "Anh/Chị/Em", "Người thân", "Khác"};

    private FirebaseFirestore db;
    private TextView tvSubtitle;
    private EditText edtFullName, edtDateOfBirth, edtIdentity;
    private Spinner spnGender, spnRelationship;
    private ProgressBar progressBar;
    private Button btnCancel, btnSave;

    private String apartmentId;
    private String apartmentCode;
    private String building;
    private String floor;
    private String ownerResidentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_household_member_form);

        db = FirebaseFirestore.getInstance();
        readIntentData();
        bindViews();
        setupHeader();
        setupSpinners();
    }

    private void readIntentData() {
        apartmentId = value(getIntent().getStringExtra(EXTRA_APARTMENT_ID));
        apartmentCode = value(getIntent().getStringExtra(EXTRA_APARTMENT_CODE));
        building = value(getIntent().getStringExtra(EXTRA_BUILDING));
        floor = value(getIntent().getStringExtra(EXTRA_FLOOR));
        ownerResidentId = value(getIntent().getStringExtra(EXTRA_OWNER_RESIDENT_ID));
    }

    private void bindViews() {
        ImageButton btnBack = findViewById(R.id.btnBackHouseholdMemberForm);
        tvSubtitle = findViewById(R.id.tvHouseholdMemberFormSubtitle);
        edtFullName = findViewById(R.id.edtMemberFullName);
        edtDateOfBirth = findViewById(R.id.edtMemberDateOfBirth);
        edtIdentity = findViewById(R.id.edtMemberIdentity);
        spnGender = findViewById(R.id.spnMemberGender);
        spnRelationship = findViewById(R.id.spnMemberRelationship);
        progressBar = findViewById(R.id.progressHouseholdMemberForm);
        btnCancel = findViewById(R.id.btnCancelHouseholdMemberForm);
        btnSave = findViewById(R.id.btnSaveHouseholdMemberForm);

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveMember());
    }

    private void setupHeader() {
        String apartmentLabel = firstNonEmpty(apartmentCode, "Căn hộ " + apartmentId);
        StringBuilder subtitle = new StringBuilder("Thêm thành viên vào ").append(apartmentLabel);
        if (!building.isEmpty()) subtitle.append(" • ").append(building);
        if (!floor.isEmpty()) subtitle.append(" • Tầng ").append(floor);
        tvSubtitle.setText(subtitle.toString());
    }

    private void setupSpinners() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GENDER_OPTIONS);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnGender.setAdapter(genderAdapter);

        ArrayAdapter<String> relationshipAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, RELATION_OPTIONS);
        relationshipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnRelationship.setAdapter(relationshipAdapter);
    }

    private void saveMember() {
        String fullName = value(edtFullName.getText().toString());
        String dateOfBirth = value(edtDateOfBirth.getText().toString());
        String identity = value(edtIdentity.getText().toString());
        String gender = String.valueOf(spnGender.getSelectedItem());
        String relationship = String.valueOf(spnRelationship.getSelectedItem());

        if (apartmentId.isEmpty()) {
            toast("Thiếu apartment_id để thêm thành viên");
            return;
        }

        if (fullName.isEmpty()) {
            toast("Vui lòng nhập họ tên thành viên");
            return;
        }

        if (ownerResidentId.isEmpty()) {
            findOwnerThenSave(fullName, dateOfBirth, identity, gender, relationship);
        } else {
            createMember(fullName, dateOfBirth, identity, gender, relationship, ownerResidentId);
        }
    }

    private void findOwnerThenSave(String fullName, String dateOfBirth, String identity, String gender, String relationship) {
        setLoading(true);
        db.collection("residents")
                .whereEqualTo("apartment_id", apartmentIdValue(apartmentId))
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        setLoading(false);
                        toast("Căn hộ này chưa có chủ hộ. Vui lòng thêm chủ hộ trước.");
                        return;
                    }

                    DocumentSnapshot ownerDoc = snapshot.getDocuments().get(0);
                    ownerResidentId = firstNonEmpty(string(ownerDoc, "resident_id"), string(ownerDoc, "id"), ownerDoc.getId());
                    createMember(fullName, dateOfBirth, identity, gender, relationship, ownerResidentId);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi tìm chủ hộ: " + e.getMessage());
                });
    }

    private void createMember(String fullName, String dateOfBirth, String identity, String gender, String relationship, String residentId) {
        setLoading(true);
        String memberDocumentId = "member_" + System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("id", memberDocumentId);
        data.put("resident_id", residentId);
        data.put("apartment_id", apartmentIdValue(apartmentId));
        data.put("full_name", fullName);
        data.put("date_of_birth", dateOfBirth);
        data.put("identity_number", identity);
        data.put("gender", gender);
        data.put("relationship", relationship);
        data.put("is_active", true);
        data.put("created_at", now());
        data.put("updated_at", now());

        db.collection("household_members")
                .document(memberDocumentId)
                .set(data)
                .addOnSuccessListener(unused -> updateMembersCountAndFinish())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi thêm thành viên: " + e.getMessage());
                });
    }

    private void updateMembersCountAndFinish() {
        db.collection("household_members")
                .whereEqualTo("apartment_id", apartmentIdValue(apartmentId))
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = Math.max(1, snapshot.size());
                    updateResidentMembersCount(count);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Đã thêm thành viên nhưng lỗi cập nhật số thành viên: " + e.getMessage());
                    finish();
                });
    }

    private void updateResidentMembersCount(int count) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("members_count", String.valueOf(count));
        updateData.put("updated_at", now());

        db.collection("residents")
                .whereEqualTo("apartment_id", apartmentIdValue(apartmentId))
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            batch.update(doc.getReference(), updateData);
                        }
                        batch.commit();
                    }
                    setLoading(false);
                    toast("Đã thêm thành viên");
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Đã thêm thành viên nhưng lỗi cập nhật chủ hộ: " + e.getMessage());
                    finish();
                });
    }

    private Object apartmentIdValue(String value) {
        long number = parseLong(value, Long.MIN_VALUE);
        if (number != Long.MIN_VALUE) return number;
        return value;
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(firstNonEmpty(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String string(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonEmpty(String... values) {
        for (String item : values) {
            if (item != null && !item.trim().isEmpty()) return item.trim();
        }
        return "";
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
