package com.example.apartmentmanagement.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminResidentsActivity extends BaseAdminListActivity {

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_admin_residents;
    }

    @Override
    protected String getCollectionName() {
        return "residents";
    }

    @Override
    protected String getTitleText() {
        return "Quản lý cư dân";
    }

    @Override
    protected String getSubtitleText() {
        return "Hồ sơ cư dân và căn hộ liên kết";
    }

    @Override
    protected String getFabText() {
        return "Thêm chủ hộ";
    }

    @Override
    protected void onAfterBaseViewsBound() {
        if (fab != null) {
            fab.setIconResource(R.drawable.ic_user_plus);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) {
            loadData();
        }
    }

    @Override
    protected void onFabClick() {
        Intent intent = new Intent(this, AdminResidentFormActivity.class);
        intent.putExtra(AdminResidentFormActivity.EXTRA_MODE, AdminResidentFormActivity.MODE_ADD);
        startActivity(intent);
    }

    @Override
    protected AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc) {
        String fullName = string(doc, "full_name");
        String apartmentNumber = firstNonEmpty(string(doc, "apartment_number"), "-");
        String relationship = "owner";
        String gender = firstNonEmpty(string(doc, "gender"), "-");
        String residentId = firstNonEmpty(string(doc, "resident_id"), string(doc, "id"), doc.getId());
        String apartmentId = string(doc, "apartment_id");

        AdminRecordAdapter.AdminRecord r = new AdminRecordAdapter.AdminRecord();
        r.documentId = doc.getId();
        r.icon = initial(fullName);
        r.title = firstNonEmpty(fullName, "Cư dân " + doc.getId());
        r.subtitle = "Căn hộ " + apartmentNumber + " • Chủ hộ";
        r.body = "Resident ID: " + firstNonEmpty(residentId, "-")
                + "\nSĐT: " + firstNonEmpty(string(doc, "phone_number"), "-")
                + "\nEmail: " + firstNonEmpty(string(doc, "email"), "-")
                + "\nCCCD: " + firstNonEmpty(string(doc, "identity_number"), "-")
                + "\nSố thành viên: " + firstNonEmpty(string(doc, "members_count"), "-");
        r.status = gender;
        r.positiveStatus = true;
        r.actions.add("Sửa");
        r.actions.add("Xóa");

        r.extras.put("resident_id", residentId);
        r.extras.put("apartment_id", apartmentId);
        r.extras.put("apartment_number", apartmentNumber);
        r.extras.put("full_name", fullName);
        r.extras.put("phone_number", string(doc, "phone_number"));
        r.extras.put("email", string(doc, "email"));
        r.extras.put("date_of_birth", string(doc, "date_of_birth"));
        r.extras.put("gender", gender);
        r.extras.put("identity_number", string(doc, "identity_number"));
        r.extras.put("members_count", string(doc, "members_count"));
        r.extras.put("relationship_to_apartment", relationship);
        return r;
    }

    @Override
    protected void onRecordAction(AdminRecordAdapter.AdminRecord record, String action) {
        if ("Sửa".equalsIgnoreCase(action)) {
            openEditResident(record);
            return;
        }

        if ("Xóa".equalsIgnoreCase(action)) {
            confirmDeleteResident(record);
        }
    }

    private void openEditResident(AdminRecordAdapter.AdminRecord record) {
        Intent intent = new Intent(this, AdminResidentFormActivity.class);
        intent.putExtra(AdminResidentFormActivity.EXTRA_MODE, AdminResidentFormActivity.MODE_EDIT);
        intent.putExtra(AdminResidentFormActivity.EXTRA_DOCUMENT_ID, record.documentId);
        intent.putExtra("resident_id", record.extras.get("resident_id"));
        intent.putExtra("full_name", record.extras.get("full_name"));
        intent.putExtra("phone_number", record.extras.get("phone_number"));
        intent.putExtra("email", record.extras.get("email"));
        intent.putExtra("date_of_birth", record.extras.get("date_of_birth"));
        intent.putExtra("identity_number", record.extras.get("identity_number"));
        intent.putExtra("members_count", record.extras.get("members_count"));
        intent.putExtra("gender", record.extras.get("gender"));
        intent.putExtra("apartment_id", record.extras.get("apartment_id"));
        intent.putExtra("apartment_number", record.extras.get("apartment_number"));
        startActivity(intent);
    }

    private void confirmDeleteResident(AdminRecordAdapter.AdminRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa cư dân")
                .setMessage("Bạn có chắc muốn xóa cư dân \"" + record.title + "\" không?\n\nSau khi xóa, trạng thái căn hộ liên kết sẽ được cập nhật lại.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> deleteResident(record))
                .show();
    }

    private void deleteResident(AdminRecordAdapter.AdminRecord record) {
        String residentId = firstNonEmpty(record.extras.get("resident_id"), record.documentId);
        String apartmentId = record.extras.get("apartment_id");
        String email = record.extras.get("email");

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("residents").document(record.documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    deleteHouseholdMembers(residentId);
                    disableUserDocument(residentId, email);
                    refreshApartmentStatus(apartmentId);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Đã xóa cư dân", Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi xóa cư dân: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteHouseholdMembers(String residentId) {
        db.collection("household_members")
                .whereEqualTo("resident_id", residentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit();
                });
    }

    private void disableUserDocument(String residentId, @Nullable String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("is_active", 0);
        data.put("updated_at", now());

        db.collection("users")
                .whereEqualTo("resident_id", parseLong(residentId, 0))
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.update(doc.getReference(), data);
                    }
                    if (!snapshot.isEmpty()) batch.commit();
                });

        if (email != null && !email.trim().isEmpty()) {
            db.collection("users")
                    .whereEqualTo("email", email.trim())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            batch.update(doc.getReference(), data);
                        }
                        if (!snapshot.isEmpty()) batch.commit();
                    });
        }
    }

    private void refreshApartmentStatus(String apartmentId) {
        if (apartmentId == null || apartmentId.trim().isEmpty()) return;

        Object apartmentValue = apartmentIdValue(apartmentId);
        db.collection("residents")
                .whereEqualTo("apartment_id", apartmentValue)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String status = snapshot.isEmpty() ? "empty" : "occupied";
                    updateApartmentStatusDocument(apartmentId, apartmentValue, status);
                });
    }

    private void updateApartmentStatusDocument(String apartmentId, Object apartmentValue, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("updated_at", now());

        db.collection("apartments")
                .whereEqualTo("id", apartmentValue)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            batch.update(doc.getReference(), data);
                        }
                        batch.commit();
                    } else {
                        db.collection("apartments").document(apartmentId).update(data);
                    }
                });
    }

    private Object apartmentIdValue(String apartmentId) {
        long number = parseLong(apartmentId, Long.MIN_VALUE);
        if (number != Long.MIN_VALUE) return number;
        return apartmentId;
    }

    private String displayRelationship(String relationship) {
        return "Chủ hộ";
    }

    private String initial(String value) {
        return value == null || value.trim().isEmpty() ? "?" : value.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(firstNonEmpty(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
}
