package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminApartmentDetailActivity extends AppCompatActivity {

    public static final String EXTRA_APARTMENT_DOC_ID = "apartment_doc_id";
    public static final String EXTRA_APARTMENT_ID = "apartment_id";
    public static final String EXTRA_APARTMENT_CODE = "apartment_code";
    public static final String EXTRA_BUILDING = "building";
    public static final String EXTRA_FLOOR = "floor";
    public static final String EXTRA_AREA = "area";
    public static final String EXTRA_STATUS = "status";

    private TextView titleView, subtitleView, apartmentCodeView, apartmentInfoView;
    private ProgressBar progressBar;
    private AdminRecordAdapter residentsAdapter, membersAdapter, feesAdapter;
    private FirebaseFirestore db;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    private String apartmentId;
    private String apartmentCode;
    private String building;
    private String floor;
    private String area;
    private String status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_apartment_detail);

        db = FirebaseFirestore.getInstance();
        readIntentData();
        bindViews();
        setupStaticInfo();
        loadRelatedData();
    }

    private void readIntentData() {
        apartmentId = getIntent().getStringExtra(EXTRA_APARTMENT_ID);
        apartmentCode = getIntent().getStringExtra(EXTRA_APARTMENT_CODE);
        building = getIntent().getStringExtra(EXTRA_BUILDING);
        floor = getIntent().getStringExtra(EXTRA_FLOOR);
        area = getIntent().getStringExtra(EXTRA_AREA);
        status = getIntent().getStringExtra(EXTRA_STATUS);
    }

    private void bindViews() {
        ImageButton backButton = findViewById(R.id.btnBackApartmentDetail);
        titleView = findViewById(R.id.tvApartmentDetailTitle);
        subtitleView = findViewById(R.id.tvApartmentDetailSubtitle);
        apartmentCodeView = findViewById(R.id.tvApartmentCode);
        apartmentInfoView = findViewById(R.id.tvApartmentInfo);
        progressBar = findViewById(R.id.progressApartmentDetail);

        backButton.setOnClickListener(v -> finish());

        residentsAdapter = setupRecycler(R.id.recyclerApartmentResidents);
        membersAdapter = setupRecycler(R.id.recyclerHouseholdMembers);
        feesAdapter = setupRecycler(R.id.recyclerApartmentFees);
    }

    private AdminRecordAdapter setupRecycler(int recyclerId) {
        RecyclerView recyclerView = findViewById(recyclerId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AdminRecordAdapter recordAdapter = new AdminRecordAdapter((record, action) -> { });
        recyclerView.setAdapter(recordAdapter);
        return recordAdapter;
    }

    private void setupStaticInfo() {
        String code = firstNonEmpty(apartmentCode, "Căn hộ " + firstNonEmpty(apartmentId, ""));
        titleView.setText(code);
        subtitleView.setText("Tòa " + firstNonEmpty(building, "-") + " • Tầng " + firstNonEmpty(floor, "-"));
        apartmentCodeView.setText(code);
        apartmentInfoView.setText(
                "Tòa nhà: " + firstNonEmpty(building, "-")
                        + "\nTầng: " + firstNonEmpty(floor, "-")
                        + "\nDiện tích: " + firstNonEmpty(area, "-") + " m²"
                        + "\nTrạng thái: " + displayApartmentStatus(status)
        );
    }

    private void loadRelatedData() {
        if (apartmentId == null || apartmentId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu apartment_id để tải dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loadResidents();
        loadMembers();
        loadFees();
    }

    private void loadResidents() {
        db.collection("residents")
                .whereEqualTo("apartment_id", parseApartmentId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AdminRecordAdapter.AdminRecord> records = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        AdminRecordAdapter.AdminRecord record = new AdminRecordAdapter.AdminRecord();
                        record.documentId = doc.getId();
                        record.icon = initial(string(doc, "full_name"));
                        record.title = string(doc, "full_name");
                        record.subtitle = "Căn hộ " + firstNonEmpty(string(doc, "apartment_number"), apartmentCode)
                                + " • " + string(doc, "relationship_to_apartment");
                        record.body = "SĐT: " + string(doc, "phone_number")
                                + "\nEmail: " + string(doc, "email")
                                + "\nCCCD: " + string(doc, "identity_number");
                        record.status = firstNonEmpty(string(doc, "gender"), "Cư dân");
                        record.positiveStatus = true;
                        records.add(record);
                    }
                    residentsAdapter.submitList(records);
                })
                .addOnFailureListener(e -> showLoadError("cư dân", e));
    }

    private void loadMembers() {
        db.collection("household_members")
                .whereEqualTo("apartment_id", parseApartmentId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AdminRecordAdapter.AdminRecord> records = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        AdminRecordAdapter.AdminRecord record = new AdminRecordAdapter.AdminRecord();
                        record.documentId = doc.getId();
                        record.icon = initial(string(doc, "full_name"));
                        record.title = string(doc, "full_name");
                        record.subtitle = firstNonEmpty(string(doc, "relationship"), "Thành viên")
                                + " • " + firstNonEmpty(string(doc, "gender"), "-");
                        record.body = "Ngày sinh: " + firstNonEmpty(string(doc, "date_of_birth"), "-")
                                + "\nCCCD: " + firstNonEmpty(string(doc, "identity_number"), "-")
                                + "\nResident ID: " + firstNonEmpty(string(doc, "resident_id"), "-");
                        record.status = isActive(doc) ? "Đang ở" : "Ngừng ở";
                        record.positiveStatus = isActive(doc);
                        records.add(record);
                    }
                    membersAdapter.submitList(records);
                })
                .addOnFailureListener(e -> showLoadError("thành viên hộ", e));
    }

    private void loadFees() {
        db.collection("fees")
                .whereEqualTo("apartment_id", parseApartmentId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AdminRecordAdapter.AdminRecord> records = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        String feeStatus = string(doc, "status");
                        boolean paid = "paid".equalsIgnoreCase(feeStatus);
                        boolean overdue = Boolean.TRUE.equals(doc.getBoolean("is_overdue"));

                        AdminRecordAdapter.AdminRecord record = new AdminRecordAdapter.AdminRecord();
                        record.documentId = doc.getId();
                        record.icon = firstNonEmpty(apartmentCode, "đ");
                        record.title = string(doc, "title");
                        record.subtitle = firstNonEmpty(string(doc, "category"), "Khoản phí")
                                + " • Hạn đóng " + firstNonEmpty(string(doc, "due_date"), "-");
                        record.body = "Số tiền: " + money(number(doc, "amount"))
                                + "\nTiền phạt: " + money(number(doc, "penalty_amount"))
                                + "\nTổng cần thu: " + money(number(doc, "total_amount"));
                        record.status = paid ? "Đã đóng" : (overdue ? "Quá hạn" : "Chưa đóng");
                        record.positiveStatus = paid;
                        records.add(record);
                    }
                    feesAdapter.submitList(records);
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showLoadError("phí", e);
                });
    }

    private Object parseApartmentId() {
        try {
            return Long.parseLong(apartmentId);
        } catch (NumberFormatException ignored) {
            return apartmentId;
        }
    }

    private boolean isActive(DocumentSnapshot doc) {
        Boolean value = doc.getBoolean("is_active");
        return value == null || value;
    }

    private String displayApartmentStatus(String value) {
        if ("occupied".equalsIgnoreCase(value)) return "Đang có cư dân";
        if ("empty".equalsIgnoreCase(value)) return "Đang trống";
        return firstNonEmpty(value, "-");
    }

    private String string(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value);
    }

    private double number(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String money(double amount) {
        return currencyFormat.format(amount) + " đ";
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private String initial(String value) {
        return value == null || value.trim().isEmpty() ? "?" : value.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private void showLoadError(String label, Exception e) {
        Toast.makeText(this, "Lỗi tải " + label + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
