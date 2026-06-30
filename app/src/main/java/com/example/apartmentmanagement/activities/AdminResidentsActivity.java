package com.example.apartmentmanagement.activities;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

public class AdminResidentsActivity extends BaseAdminListActivity {
    protected int getLayoutResId() { return R.layout.activity_admin_residents; }
    protected String getCollectionName() { return "residents"; }
    protected String getTitleText() { return "Quản lý cư dân"; }
    protected String getSubtitleText() { return "Hồ sơ cư dân và căn hộ liên kết"; }

    protected AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc) {
        AdminRecordAdapter.AdminRecord r = new AdminRecordAdapter.AdminRecord();
        r.documentId = doc.getId();
        r.icon = initial(string(doc, "full_name"));
        r.title = string(doc, "full_name");
        r.subtitle = "Căn hộ " + string(doc, "apartment_number") + " • " + string(doc, "relationship_to_apartment");
        r.body = "SĐT: " + string(doc, "phone_number")
                + "\nEmail: " + string(doc, "email")
                + "\nCCCD: " + string(doc, "identity_number")
                + "\nSố thành viên: " + string(doc, "members_count");
        r.status = string(doc, "gender");
        r.positiveStatus = true;
        return r;
    }

    private String initial(String value) {
        return value == null || value.isEmpty() ? "?" : value.substring(0, 1).toUpperCase();
    }
}
