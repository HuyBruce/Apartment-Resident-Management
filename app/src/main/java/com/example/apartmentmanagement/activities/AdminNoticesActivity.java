package com.example.apartmentmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;

public class AdminNoticesActivity extends BaseAdminListActivity {

    private static final String FILTER_ALL = "Tất cả";
    private static final String FILTER_FACILITY = "Cơ sở vật chất";
    private static final String FILTER_FEE = "Phí";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button btnCreateNotice = findViewById(R.id.btnCreateNoticeTop);
        if (btnCreateNotice != null) {
            btnCreateNotice.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminCreateNoticeActivity.class)));
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
    protected int getLayoutResId() {
        return R.layout.activity_admin_notices;
    }

    @Override
    protected String getCollectionName() {
        return "notices";
    }

    @Override
    protected String getTitleText() {
        return "Quản lý thông báo";
    }

    @Override
    protected String getSubtitleText() {
        return "Tạo và theo dõi thông báo gửi cư dân";
    }

    @Override
    protected String getFilterField() {
        // Không lọc trực tiếp theo field type vì dữ liệu cũ có general/maintenance/fee.
        // Ta gom general + maintenance thành "Cơ sở vật chất" ở shouldDisplayDocument().
        return null;
    }

    @Override
    protected String[] getFilters() {
        return new String[]{FILTER_ALL, FILTER_FACILITY, FILTER_FEE};
    }

    @Override
    protected boolean shouldDisplayDocument(DocumentSnapshot doc) {
        String selected = getSelectedFilter();
        if (FILTER_ALL.equals(selected)) {
            return true;
        }
        return selected.equals(displayType(string(doc, "type")));
    }

    @Override
    protected String getFabText() {
        return "Làm mới";
    }

    @Override
    protected void onFabClick() {
        loadData();
    }

    @Override
    protected AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc) {
        String displayType = displayType(string(doc, "type"));

        AdminRecordAdapter.AdminRecord record = new AdminRecordAdapter.AdminRecord();
        record.documentId = doc.getId();
        record.icon = "!";
        record.title = string(doc, "title");
        record.subtitle = displayType + " • " + string(doc, "created_at");
        record.body = string(doc, "content");
        record.status = displayType;
        record.positiveStatus = true;
        return record;
    }

    @Override
    protected boolean matches(AdminRecordAdapter.AdminRecord record, String keyword) {
        // Chỉ tìm theo tên thông báo và loại thông báo: "Phí" / "Cơ sở vật chất".
        String text = (safe(record.title) + " " + safe(record.status)).toLowerCase(Locale.ROOT);
        return text.contains(keyword);
    }

    private String displayType(String rawType) {
        String value = rawType == null ? "" : rawType.trim().toLowerCase(Locale.ROOT);
        if (value.equals("fee") || value.equals("phí") || value.equals("phi")) {
            return FILTER_FEE;
        }
        return FILTER_FACILITY;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
