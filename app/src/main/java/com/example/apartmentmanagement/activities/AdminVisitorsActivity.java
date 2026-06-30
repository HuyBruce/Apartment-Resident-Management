package com.example.apartmentmanagement.activities;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminVisitorsActivity extends BaseAdminListActivity {

    private final Map<String, String> apartmentCodeById = new HashMap<>();

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_admin_visitors;
    }

    @Override
    protected String getCollectionName() {
        return "visitors";
    }

    @Override
    protected String getTitleText() {
        return "Duyệt khách";
    }

    @Override
    protected String getSubtitleText() {
        return "Đăng ký khách ra vào chung cư";
    }

    @Override
    protected String getFilterField() {
        return null;
    }

    @Override
    protected String[] getFilters() {
        return new String[]{"Tất cả", "Đang xử lý", "Đã duyệt", "Từ chối"};
    }

    @Override
    protected void onAfterBaseViewsBound() {
        loadApartmentLookup();
    }

    private void loadApartmentLookup() {
        db.collection("apartments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    apartmentCodeById.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(string(doc, "id"), doc.getId());
                        String code = firstNonEmpty(string(doc, "apartment_code"), string(doc, "apartment_number"), doc.getId());
                        apartmentCodeById.put(id, code);
                        apartmentCodeById.put(doc.getId(), code);
                    }
                    loadData();
                });
    }

    @Override
    protected boolean shouldDisplayDocument(DocumentSnapshot doc) {
        String selected = getSelectedFilter();
        if ("Tất cả".equalsIgnoreCase(selected)) {
            return true;
        }
        String displayStatus = displayStatus(string(doc, "status"));
        return selected.equalsIgnoreCase(displayStatus);
    }

    @Override
    protected AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc) {
        String rawStatus = string(doc, "status");
        String displayStatus = displayStatus(rawStatus);
        String apartmentCode = getApartmentCode(doc);

        AdminRecordAdapter.AdminRecord r = new AdminRecordAdapter.AdminRecord();
        r.documentId = doc.getId();
        r.icon = initial(string(doc, "visitor_name"));
        r.title = string(doc, "visitor_name");
        r.subtitle = string(doc, "visitor_phone") + " • " + apartmentCode;
        r.body = "Mục đích: " + string(doc, "purpose")
                + "\nThời gian: " + string(doc, "visit_date") + " " + string(doc, "visit_time")
                + "\nCăn hộ: " + apartmentCode;
        r.status = displayStatus;
        r.positiveStatus = "approved".equalsIgnoreCase(rawStatus);

        if ("pending".equalsIgnoreCase(rawStatus)) {
            r.actions.add("Duyệt");
            r.actions.add("Từ chối");
        }
        return r;
    }

    @Override
    protected void onRecordAction(AdminRecordAdapter.AdminRecord record, String action) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "Duyệt".equalsIgnoreCase(action) ? "approved" : "rejected");
        data.put("updated_at", now());
        updateField("visitors", record.documentId, data);
    }

    private String getApartmentCode(DocumentSnapshot doc) {
        String apartmentId = string(doc, "apartment_id");
        String directCode = firstNonEmpty(string(doc, "apartment_code"), string(doc, "apartment_number"));
        if (!directCode.isEmpty()) {
            return directCode;
        }
        String code = apartmentCodeById.get(apartmentId);
        if (code != null && !code.trim().isEmpty()) {
            return code;
        }
        return apartmentId.isEmpty() ? "Chưa có căn hộ" : "Căn hộ " + apartmentId;
    }

    private String displayStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "approved":
                return "Đã duyệt";
            case "rejected":
                return "Từ chối";
            case "pending":
            default:
                return "Đang xử lý";
        }
    }

    private String initial(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "?";
        }
        return value.trim().substring(0, 1).toUpperCase(new Locale("vi", "VN"));
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
