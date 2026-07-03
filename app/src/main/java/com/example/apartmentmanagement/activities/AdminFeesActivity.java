package com.example.apartmentmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class AdminFeesActivity extends AppCompatActivity {

    private static final String FILTER_ALL = "Tất cả";
    private static final String FILTER_DEBT = "Còn nợ";
    private static final String FILTER_OVERDUE = "Quá hạn";
    private static final String FILTER_PAID = "Đã đóng";

    private FirebaseFirestore db;
    private AdminRecordAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView titleView, subtitleView, emptyText;
    private TextView tvTotalDebt, tvOverdueApartments, tvDebtApartments;
    private EditText searchEditText;
    private ChipGroup chipGroup;
    private ExtendedFloatingActionButton fab;

    private final Map<String, ApartmentInfo> apartmentMap = new LinkedHashMap<>();
    private final List<AdminRecordAdapter.AdminRecord> allRecords = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_fees);

        db = FirebaseFirestore.getInstance();
        bindViews();
        setupHeader();
        setupBackButton();
        setupFilters();
        setupSearch();
        loadData();
    }

    private void bindViews() {
        titleView = findViewById(R.id.tvAdminListTitle);
        subtitleView = findViewById(R.id.tvAdminListSubtitle);
        tvTotalDebt = findViewById(R.id.tvTotalDebt);
        tvOverdueApartments = findViewById(R.id.tvOverdueApartments);
        tvDebtApartments = findViewById(R.id.tvDebtApartments);
        progressBar = findViewById(R.id.progressAdmin);
        emptyLayout = findViewById(R.id.layoutAdminEmpty);
        emptyText = findViewById(R.id.tvAdminEmptyText);
        searchEditText = findViewById(R.id.edtAdminSearch);
        chipGroup = findViewById(R.id.chipGroupAdmin);
        fab = findViewById(R.id.fabAdminAction);

        RecyclerView recyclerView = findViewById(R.id.recyclerAdmin);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminRecordAdapter(this::onRecordAction);
        recyclerView.setAdapter(adapter);
    }

    private void setupHeader() {
        titleView.setText("Quản lý phí & nợ quá hạn");
        subtitleView.setText("Tổng nợ theo căn hộ, phí quá hạn và nhắc nợ");
        fab.setText("Làm mới");
        fab.setOnClickListener(v -> loadData());
    }

    private void setupBackButton() {
        View backButton = findViewById(R.id.btnAdminBack);
        if (backButton == null) return;

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupFilters() {
        chipGroup.removeAllViews();
        String[] filters = new String[]{FILTER_ALL, FILTER_DEBT, FILTER_OVERDUE, FILTER_PAID};
        for (int i = 0; i < filters.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(filters[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            chipGroup.addView(chip);
        }
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> applySearchAndFilter());
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchAndFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);

        db.collection("apartments")
                .get()
                .addOnSuccessListener(apartmentSnapshot -> {
                    apartmentMap.clear();
                    for (DocumentSnapshot doc : apartmentSnapshot) {
                        ApartmentInfo apartment = new ApartmentInfo();
                        apartment.id = normalizeId(firstNonEmpty(string(doc, "id"), doc.getId()));
                        apartment.code = firstNonEmpty(string(doc, "apartment_code"), "Căn hộ " + apartment.id);
                        apartment.building = string(doc, "building");
                        apartment.floor = string(doc, "floor");
                        apartmentMap.put(apartment.id, apartment);
                    }
                    loadFees();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải căn hộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFees() {
        db.collection("fees")
                .get()
                .addOnSuccessListener(feeSnapshot -> {
                    Map<String, DebtGroup> groups = new LinkedHashMap<>();
                    double totalDebt = 0;
                    int debtApartments = 0;
                    int overdueApartments = 0;

                    for (QueryDocumentSnapshot doc : feeSnapshot) {
                        String apartmentId = normalizeId(string(doc, "apartment_id"));
                        if (apartmentId.isEmpty()) {
                            apartmentId = "unknown";
                        }

                        DebtGroup group = groups.get(apartmentId);
                        if (group == null) {
                            group = new DebtGroup(apartmentId, apartmentMap.get(apartmentId));
                            groups.put(apartmentId, group);
                        }
                        group.addFee(doc);
                    }

                    allRecords.clear();
                    for (DebtGroup group : groups.values()) {
                        if (group.unpaidCount > 0) {
                            totalDebt += group.totalDebt;
                            debtApartments++;
                        }
                        if (group.overdueCount > 0) {
                            overdueApartments++;
                        }
                        allRecords.add(group.toRecord());
                    }

                    tvTotalDebt.setText(money(totalDebt));
                    tvDebtApartments.setText(String.valueOf(debtApartments));
                    tvOverdueApartments.setText(String.valueOf(overdueApartments));

                    progressBar.setVisibility(View.GONE);
                    applySearchAndFilter();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải phí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applySearchAndFilter() {
        String keyword = normalizeSearch(searchEditText.getText().toString().trim());
        String filter = getSelectedFilter();
        List<AdminRecordAdapter.AdminRecord> visibleRecords = new ArrayList<>();

        for (AdminRecordAdapter.AdminRecord record : allRecords) {
            if (!matchesFilter(record, filter)) {
                continue;
            }
            if (keyword.isEmpty() || matchesSearch(record, keyword)) {
                visibleRecords.add(record);
            }
        }

        adapter.submitList(visibleRecords);
        emptyLayout.setVisibility(visibleRecords.isEmpty() ? View.VISIBLE : View.GONE);
        emptyText.setText(visibleRecords.isEmpty() ? "Không có căn hộ phù hợp" : "");
        subtitleView.setText("Tổng nợ theo căn hộ, phí quá hạn và nhắc nợ • " + visibleRecords.size() + " bản ghi");
    }

    private boolean matchesFilter(AdminRecordAdapter.AdminRecord record, String filter) {
        String status = firstNonEmpty(record.extras.get("debt_status"), "");
        if (FILTER_DEBT.equals(filter)) {
            return "debt".equals(status) || "overdue".equals(status);
        }
        if (FILTER_OVERDUE.equals(filter)) {
            return "overdue".equals(status);
        }
        if (FILTER_PAID.equals(filter)) {
            return "paid".equals(status);
        }
        return true;
    }

    private boolean matchesSearch(AdminRecordAdapter.AdminRecord record, String keyword) {
        String text = normalizeSearch(firstNonEmpty(record.title) + " "
                + firstNonEmpty(record.subtitle) + " "
                + firstNonEmpty(record.body) + " "
                + firstNonEmpty(record.status));
        return text.contains(keyword);
    }

    private String getSelectedFilter() {
        int id = chipGroup.getCheckedChipId();
        if (id == View.NO_ID) {
            return FILTER_ALL;
        }
        Chip chip = chipGroup.findViewById(id);
        return chip == null ? FILTER_ALL : chip.getText().toString();
    }

    private void onRecordAction(AdminRecordAdapter.AdminRecord record, String action) {
        if ("Gửi nhắc nợ".equals(action)) {
            sendReminder(record);
        } else if (action.startsWith("Xác nhận")) {
            confirmPaid(record);
        }
    }

    private void sendReminder(AdminRecordAdapter.AdminRecord record) {
        String feeIds = firstNonEmpty(record.extras.get("unpaid_fee_ids"));
        if (feeIds.isEmpty()) {
            Toast.makeText(this, "Căn hộ này không còn khoản nợ cần nhắc", Toast.LENGTH_SHORT).show();
            return;
        }

        String now = now();
        String apartmentId = firstNonEmpty(record.extras.get("apartment_id"));
        String residentId = firstNonEmpty(record.extras.get("resident_id"));
        String title = "Nhắc thanh toán phí căn hộ " + record.title;
        String content = "Căn hộ " + record.title + " hiện còn nợ "
                + firstNonEmpty(record.extras.get("total_debt_text"))
                + ". Vui lòng thanh toán các khoản phí đúng hạn để tránh bị hạn chế dịch vụ.";

        WriteBatch batch = db.batch();

        DocumentReference noticeRef = db.collection("notices").document();
        Map<String, Object> notice = new LinkedHashMap<>();
        notice.put("id", noticeRef.getId());
        notice.put("title", title);
        notice.put("content", content);
        notice.put("type", "fee_reminder");
        notice.put("created_by", "admin");
        notice.put("created_at", now);
        notice.put("updated_at", now);
        batch.set(noticeRef, notice);

        DocumentReference targetRef = db.collection("notice_targets").document();
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("id", targetRef.getId());
        target.put("notice_id", noticeRef.getId());
        target.put("target_type", "apartment");
        target.put("target_id", apartmentId);
        target.put("target_building", firstNonEmpty(record.extras.get("building")));
        target.put("target_floor", firstNonEmpty(record.extras.get("floor")));
        target.put("target_apartment_id", apartmentId);
        target.put("target_resident_id", residentId);
        target.put("created_at", now);
        batch.set(targetRef, target);

        for (String feeId : feeIds.split(",")) {
            String cleanFeeId = feeId.trim();
            if (cleanFeeId.isEmpty()) {
                continue;
            }
            DocumentReference reminderRef = db.collection("admin_reminders").document();
            Map<String, Object> reminder = new LinkedHashMap<>();
            reminder.put("id", reminderRef.getId());
            reminder.put("fee_id", cleanFeeId);
            reminder.put("resident_id", residentId);
            reminder.put("apartment_id", apartmentId);
            reminder.put("notice_id", noticeRef.getId());
            reminder.put("type", "overdue_fee");
            reminder.put("message", content);
            reminder.put("sent_at", now);
            reminder.put("sent_by", "admin");
            reminder.put("status", "sent");
            reminder.put("created_at", now);
            reminder.put("updated_at", now);
            batch.set(reminderRef, reminder);

            DocumentReference feeRef = db.collection("fees").document(cleanFeeId);
            batch.update(feeRef, "reminder_sent_at", now, "reminder_count", FieldValue.increment(1), "updated_at", now);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã gửi nhắc nợ", Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi gửi nhắc nợ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmPaid(AdminRecordAdapter.AdminRecord record) {
        String feeIds = firstNonEmpty(record.extras.get("unpaid_fee_ids"));
        if (feeIds.isEmpty()) {
            Toast.makeText(this, "Không có khoản phí chưa đóng", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] feeIdArray = feeIds.split(",");
        String now = now();
        WriteBatch batch = db.batch();
        for (String feeId : feeIdArray) {
            String cleanFeeId = feeId.trim();
            if (cleanFeeId.isEmpty()) {
                continue;
            }
            DocumentReference feeRef = db.collection("fees").document(cleanFeeId);
            batch.update(feeRef,
                    "status", "paid",
                    "paid_at", now,
                    "updated_at", now,
                    "payment_id", "manual_" + cleanFeeId);

            DocumentReference paymentRef = db.collection("payments").document();
            Map<String, Object> payment = new LinkedHashMap<>();
            payment.put("id", paymentRef.getId());
            payment.put("fee_id", cleanFeeId);
            payment.put("amount", parseMoneyFromRecord(record, feeIdArray.length));
            payment.put("note", "Admin xác nhận thanh toán thủ công");
            payment.put("paid_at", now);
            payment.put("payment_method", "manual");
            payment.put("payment_status", "success");
            payment.put("user_id", firstNonEmpty(record.extras.get("resident_id")));
            batch.set(paymentRef, payment);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã xác nhận thanh toán", Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xác nhận: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private double parseMoneyFromRecord(AdminRecordAdapter.AdminRecord record, int count) {
        try {
            double totalDebt = Double.parseDouble(firstNonEmpty(record.extras.get("total_debt"), "0"));
            return count <= 0 ? totalDebt : totalDebt / count;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private class DebtGroup {
        final String apartmentId;
        final ApartmentInfo apartment;
        double totalDebt = 0;
        int unpaidCount = 0;
        int overdueCount = 0;
        int paidCount = 0;
        String residentId = "";
        final List<String> unpaidFeeIds = new ArrayList<>();
        final List<String> feeLines = new ArrayList<>();

        DebtGroup(String apartmentId, ApartmentInfo apartment) {
            this.apartmentId = apartmentId;
            this.apartment = apartment == null ? new ApartmentInfo(apartmentId) : apartment;
        }

        void addFee(QueryDocumentSnapshot doc) {
            String status = string(doc, "status");
            boolean paid = "paid".equalsIgnoreCase(status);
            boolean overdue = !paid && isOverdue(doc);
            double amount = number(doc, "total_amount");
            if (amount <= 0) {
                amount = number(doc, "amount") + number(doc, "penalty_amount");
            }

            String title = firstNonEmpty(string(doc, "title"), "Khoản phí");
            String dueDate = string(doc, "due_date");
            String docId = doc.getId();
            if (residentId.isEmpty()) {
                residentId = normalizeId(string(doc, "resident_id"));
            }

            if (paid) {
                paidCount++;
                return;
            }

            unpaidCount++;
            totalDebt += amount;
            unpaidFeeIds.add(docId);
            if (overdue) {
                overdueCount++;
            }
            feeLines.add("• " + title + " - " + money(amount) + " - hạn " + firstNonEmpty(dueDate, "chưa có") + (overdue ? " - quá hạn" : ""));
        }

        AdminRecordAdapter.AdminRecord toRecord() {
            AdminRecordAdapter.AdminRecord record = new AdminRecordAdapter.AdminRecord();
            record.documentId = apartmentId;
            record.icon = apartment.code.length() <= 4 ? apartment.code : apartment.code.substring(0, 4);
            record.title = apartment.code;

            if (unpaidCount > 0) {
                record.subtitle = "Tổng nợ: " + money(totalDebt) + " • " + unpaidCount + " khoản chưa đóng";
                record.body = "Tòa nhà: " + firstNonEmpty(apartment.building, "Chưa rõ")
                        + "\nTầng: " + firstNonEmpty(apartment.floor, "Chưa rõ")
                        + "\nPhí chưa đóng:\n" + joinLines(feeLines, 4);
                record.status = overdueCount > 0 ? "Quá hạn" : "Còn nợ";
                record.positiveStatus = false;
                record.actions.add("Gửi nhắc nợ");
                record.actions.add("Xác nhận đã đóng");
                record.extras.put("debt_status", overdueCount > 0 ? "overdue" : "debt");
            } else {
                record.subtitle = "Không còn khoản nợ";
                record.body = "Tòa nhà: " + firstNonEmpty(apartment.building, "Chưa rõ")
                        + "\nTầng: " + firstNonEmpty(apartment.floor, "Chưa rõ")
                        + "\nSố khoản đã đóng: " + paidCount;
                record.status = "Đã đóng";
                record.positiveStatus = true;
                record.extras.put("debt_status", "paid");
            }

            record.extras.put("apartment_id", apartmentId);
            record.extras.put("apartment_code", apartment.code);
            record.extras.put("building", firstNonEmpty(apartment.building));
            record.extras.put("floor", firstNonEmpty(apartment.floor));
            record.extras.put("resident_id", firstNonEmpty(residentId));
            record.extras.put("unpaid_fee_ids", joinIds(unpaidFeeIds));
            record.extras.put("total_debt", String.valueOf(totalDebt));
            record.extras.put("total_debt_text", money(totalDebt));
            return record;
        }
    }

    private static class ApartmentInfo {
        String id;
        String code;
        String building;
        String floor;

        ApartmentInfo() {
        }

        ApartmentInfo(String id) {
            this.id = id;
            this.code = "Căn hộ " + id;
            this.building = "";
            this.floor = "";
        }
    }

    private boolean isOverdue(DocumentSnapshot doc) {
        Object isOverdue = doc.get("is_overdue");
        if (isOverdue instanceof Boolean && (Boolean) isOverdue) {
            return true;
        }

        String dueDate = string(doc, "due_date");
        if (dueDate.isEmpty()) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            Date due = sdf.parse(dueDate);
            Date today = sdf.parse(sdf.format(new Date()));
            return due != null && today != null && due.before(today);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String joinLines(List<String> lines, int maxLines) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(lines.size(), maxLines);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append("\n");
            }
            builder.append(lines.get(i));
        }
        if (lines.size() > maxLines) {
            builder.append("\n• ... và ").append(lines.size() - maxLines).append(" khoản khác");
        }
        return builder.toString();
    }

    private String joinIds(List<String> ids) {
        StringBuilder builder = new StringBuilder();
        for (String id : ids) {
            if (id == null || id.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(id.trim());
        }
        return builder.toString();
    }

    private String string(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private double number(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String money(double amount) {
        return currencyFormat.format(amount) + " đ";
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalizeId(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.endsWith(".0")) {
            return trimmed.substring(0, trimmed.length() - 2);
        }
        return trimmed;
    }

    private String normalizeSearch(String value) {
        String temp = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }
}
