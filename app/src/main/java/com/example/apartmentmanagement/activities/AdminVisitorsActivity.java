package com.example.apartmentmanagement.activities;

import android.widget.Toast;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminVisitorsActivity extends BaseAdminListActivity {

    private final Map<String, String> apartmentCodeById = new HashMap<>();
    private final Map<String, String> apartmentIdByCode = new HashMap<>();
    private final Map<String, String> residentApartmentIdById = new HashMap<>();
    private final Map<String, List<String>> overdueFeeIdsByApartment = new HashMap<>();
    private final Map<String, Double> overdueDebtByApartment = new HashMap<>();

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
        // Không hiển thị filter Đang xử lý nữa. Khách bị hạn chế do nợ quá hạn sẽ nằm ở nhóm Chặn.
        return new String[]{"Tất cả", "Đã duyệt", "Từ chối", "Chặn"};
    }

    @Override
    protected void onAfterBaseViewsBound() {
        loadLookups();
    }

    private void loadLookups() {
        loadApartmentLookup();
        loadResidentLookup();
        loadOverdueFees();
    }

    private void loadApartmentLookup() {
        db.collection("apartments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    apartmentCodeById.clear();
                    apartmentIdByCode.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(string(doc, "id"), doc.getId());
                        String code = firstNonEmpty(string(doc, "apartment_code"), string(doc, "apartment_number"), doc.getId());

                        putApartmentAlias(id, id, code);
                        putApartmentAlias(doc.getId(), id, code);
                        putApartmentAlias(code, id, code);
                    }
                    loadData();
                });
    }

    private void putApartmentAlias(String alias, String apartmentId, String apartmentCode) {
        if (isEmpty(alias)) {
            return;
        }
        apartmentCodeById.put(alias, apartmentCode);
        apartmentIdByCode.put(alias, apartmentId);
    }

    private void loadResidentLookup() {
        db.collection("residents")
                .get()
                .addOnSuccessListener(snapshot -> {
                    residentApartmentIdById.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String residentId = firstNonEmpty(string(doc, "resident_id"), string(doc, "id"), doc.getId());
                        String apartmentId = firstNonEmpty(string(doc, "apartment_id"), string(doc, "apartment_number"), string(doc, "apartment_code"));
                        if (!isEmpty(residentId) && !isEmpty(apartmentId)) {
                            residentApartmentIdById.put(residentId, normalizeApartmentId(apartmentId));
                            residentApartmentIdById.put(doc.getId(), normalizeApartmentId(apartmentId));
                        }
                    }
                    loadData();
                });
    }

    private void loadOverdueFees() {
        db.collection("fees")
                .get()
                .addOnSuccessListener(snapshot -> {
                    overdueFeeIdsByApartment.clear();
                    overdueDebtByApartment.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String status = string(doc, "status").toLowerCase(Locale.ROOT);
                        if (isPaidStatus(status)) {
                            continue;
                        }

                        boolean overdue = isTruthy(doc.get("is_overdue")) || isDateBeforeToday(string(doc, "due_date"));
                        if (!overdue) {
                            continue;
                        }

                        String rawApartmentId = firstNonEmpty(string(doc, "apartment_id"), string(doc, "apartment_code"), string(doc, "apartment_number"));
                        String apartmentId = normalizeApartmentId(rawApartmentId);
                        if (isEmpty(apartmentId)) {
                            continue;
                        }

                        String feeId = firstNonEmpty(string(doc, "id"), doc.getId());
                        double totalAmount = number(doc, "total_amount");
                        if (totalAmount <= 0) {
                            totalAmount = number(doc, "amount") + number(doc, "penalty_amount");
                        }

                        addOverdueFee(apartmentId, feeId, totalAmount);

                        String apartmentCode = apartmentCodeById.get(apartmentId);
                        if (!isEmpty(apartmentCode)) {
                            addOverdueFee(apartmentCode, feeId, totalAmount);
                        }
                    }
                    loadData();
                });
    }

    private void addOverdueFee(String apartmentId, String feeId, double amount) {
        if (isEmpty(apartmentId)) {
            return;
        }
        if (!overdueFeeIdsByApartment.containsKey(apartmentId)) {
            overdueFeeIdsByApartment.put(apartmentId, new ArrayList<>());
        }
        if (!overdueFeeIdsByApartment.get(apartmentId).contains(feeId)) {
            overdueFeeIdsByApartment.get(apartmentId).add(feeId);
            overdueDebtByApartment.put(apartmentId, overdueDebtByApartment.getOrDefault(apartmentId, 0.0) + amount);
        }
    }

    @Override
    protected boolean shouldDisplayDocument(DocumentSnapshot doc) {
        String selected = getSelectedFilter();
        if ("Tất cả".equalsIgnoreCase(selected)) {
            return true;
        }

        String status = effectiveStatus(doc);
        String displayStatus = displayStatus(status);
        return selected.equalsIgnoreCase(displayStatus);
    }

    @Override
    protected AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc) {
        String apartmentId = getVisitorApartmentId(doc);
        String rawStatus = normalizeStatus(string(doc, "status"));

        if (shouldAutoBlock(rawStatus, apartmentId)) {
            autoBlockVisitor(doc, apartmentId);
            rawStatus = "blocked";
        }

        String apartmentCode = getApartmentCode(doc, apartmentId);
        boolean hasOverdueDebt = hasOverdueDebt(apartmentId);
        String displayStatus = displayStatus(rawStatus);

        AdminRecordAdapter.AdminRecord r = new AdminRecordAdapter.AdminRecord();
        r.documentId = doc.getId();
        r.icon = initial(string(doc, "visitor_name"));
        r.title = firstNonEmpty(string(doc, "visitor_name"), "Khách");
        r.subtitle = firstNonEmpty(string(doc, "visitor_phone"), "Chưa có SĐT") + " • " + apartmentCode;

        StringBuilder body = new StringBuilder();
        body.append("Mục đích: ").append(firstNonEmpty(string(doc, "purpose"), "Chưa có"));
        body.append("\nThời gian: ").append(firstNonEmpty(string(doc, "visit_date"), "Chưa có"));
        if (!isEmpty(string(doc, "visit_time"))) {
            body.append(" ").append(string(doc, "visit_time"));
        }
        body.append("\nCăn hộ: ").append(apartmentCode);

        if (hasOverdueDebt) {
            body.append("\nCông nợ quá hạn: ").append(money(overdueDebtByApartment.getOrDefault(apartmentId, 0.0)));
        }
        if ("blocked".equals(rawStatus)) {
            body.append("\nLý do bị chặn: ")
                    .append(firstNonEmpty(string(doc, "block_reason"), buildBlockReason(apartmentId)));
        }

        r.body = body.toString();
        r.status = displayStatus;
        r.positiveStatus = "approved".equalsIgnoreCase(rawStatus);
        r.extras.put("apartment_id", apartmentId);

        if ("blocked".equals(rawStatus)) {
            r.actions.add("Bỏ chặn");
        } else if ("pending".equals(rawStatus)) {
            r.actions.add("Duyệt");
            r.actions.add("Từ chối");
        }

        return r;
    }

    @Override
    protected void onRecordAction(AdminRecordAdapter.AdminRecord record, String action) {
        if ("Bỏ chặn".equalsIgnoreCase(action)) {
            unblockVisitor(record);
            return;
        }

        if ("Duyệt".equalsIgnoreCase(action)) {
            approveVisitor(record);
            return;
        }

        if ("Từ chối".equalsIgnoreCase(action)) {
            updateVisitorStatus(record.documentId, "rejected", null);
        }
    }

    private void approveVisitor(AdminRecordAdapter.AdminRecord record) {
        db.collection("visitors").document(record.documentId)
                .get()
                .addOnSuccessListener(doc -> {
                    String apartmentId = getVisitorApartmentId(doc);
                    if (hasOverdueDebt(apartmentId)) {
                        autoBlockVisitor(doc, apartmentId);
                        Toast.makeText(this, "Không thể duyệt vì căn hộ còn nợ quá hạn", Toast.LENGTH_LONG).show();
                        loadData();
                        return;
                    }
                    updateVisitorStatus(record.documentId, "approved", null);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi kiểm tra công nợ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void unblockVisitor(AdminRecordAdapter.AdminRecord record) {
        db.collection("visitors").document(record.documentId)
                .get()
                .addOnSuccessListener(doc -> {
                    String apartmentId = getVisitorApartmentId(doc);
                    if (hasOverdueDebt(apartmentId)) {
                        Toast.makeText(this, "Chưa thể bỏ chặn vì căn hộ vẫn còn nợ quá hạn", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "pending");
                    data.put("block_reason", "");
                    data.put("blocked_fee_ids", new ArrayList<String>());
                    data.put("unblocked_by", "admin");
                    data.put("unblocked_at", now());
                    data.put("updated_at", now());
                    updateField("visitors", record.documentId, data);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi bỏ chặn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateVisitorStatus(String documentId, String status, Map<String, Object> extraData) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("updated_at", now());
        if (extraData != null) {
            data.putAll(extraData);
        }
        updateField("visitors", documentId, data);
    }

    private void autoBlockVisitor(DocumentSnapshot doc, String apartmentId) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "blocked");
        data.put("block_reason", buildBlockReason(apartmentId));
        data.put("blocked_fee_ids", overdueFeeIdsByApartment.getOrDefault(apartmentId, new ArrayList<>()));
        data.put("blocked_at", now());
        data.put("updated_at", now());
        db.collection("visitors").document(doc.getId()).update(data);
    }

    private String effectiveStatus(DocumentSnapshot doc) {
        String status = normalizeStatus(string(doc, "status"));
        String apartmentId = getVisitorApartmentId(doc);
        if (shouldAutoBlock(status, apartmentId)) {
            return "blocked";
        }
        return status;
    }

    private boolean shouldAutoBlock(String status, String apartmentId) {
        if (!hasOverdueDebt(apartmentId)) {
            return false;
        }
        return "pending".equals(status) || "approved".equals(status) || isEmpty(status);
    }

    private boolean hasOverdueDebt(String apartmentId) {
        if (isEmpty(apartmentId)) {
            return false;
        }
        String normalized = normalizeApartmentId(apartmentId);
        List<String> feeIds = overdueFeeIdsByApartment.get(normalized);
        if (feeIds != null && !feeIds.isEmpty()) {
            return true;
        }
        String code = apartmentCodeById.get(normalized);
        feeIds = overdueFeeIdsByApartment.get(code);
        return feeIds != null && !feeIds.isEmpty();
    }

    private String buildBlockReason(String apartmentId) {
        String normalized = normalizeApartmentId(apartmentId);
        List<String> feeIds = overdueFeeIdsByApartment.get(normalized);
        if ((feeIds == null || feeIds.isEmpty()) && !isEmpty(apartmentCodeById.get(normalized))) {
            feeIds = overdueFeeIdsByApartment.get(apartmentCodeById.get(normalized));
        }

        int feeCount = feeIds == null ? 0 : feeIds.size();
        double debt = overdueDebtByApartment.getOrDefault(normalized, 0.0);
        if (debt <= 0 && !isEmpty(apartmentCodeById.get(normalized))) {
            debt = overdueDebtByApartment.getOrDefault(apartmentCodeById.get(normalized), 0.0);
        }

        if (feeCount <= 0) {
            return "Căn hộ chưa đủ điều kiện duyệt khách";
        }
        return "Căn hộ còn " + feeCount + " khoản phí quá hạn, tổng nợ " + money(debt);
    }

    private String getVisitorApartmentId(DocumentSnapshot doc) {
        String direct = firstNonEmpty(string(doc, "apartment_id"), string(doc, "apartment_code"), string(doc, "apartment_number"));
        if (!isEmpty(direct)) {
            return normalizeApartmentId(direct);
        }

        String residentId = firstNonEmpty(string(doc, "resident_id"), string(doc, "residentId"));
        String apartmentId = residentApartmentIdById.get(residentId);
        return apartmentId == null ? "" : normalizeApartmentId(apartmentId);
    }

    private String normalizeApartmentId(String apartmentIdOrCode) {
        String text = apartmentIdOrCode == null ? "" : apartmentIdOrCode.trim();
        if (text.isEmpty()) {
            return "";
        }
        String id = apartmentIdByCode.get(text);
        return isEmpty(id) ? text : id;
    }

    private String getApartmentCode(DocumentSnapshot doc, String apartmentId) {
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
        String normalized = normalizeStatus(status);
        switch (normalized) {
            case "approved":
                return "Đã duyệt";
            case "rejected":
                return "Từ chối";
            case "blocked":
                return "Chặn";
            case "pending":
            default:
                return "Chờ duyệt";
        }
    }

    private String normalizeStatus(String status) {
        String text = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if ("đã duyệt".equals(text) || "da duyet".equals(text)) {
            return "approved";
        }
        if ("từ chối".equals(text) || "tu choi".equals(text)) {
            return "rejected";
        }
        if ("chặn".equals(text) || "chan".equals(text) || "bị chặn".equals(text) || "bi chan".equals(text)) {
            return "blocked";
        }
        if ("đang xử lý".equals(text) || "dang xu ly".equals(text) || "chờ duyệt".equals(text) || "cho duyet".equals(text)) {
            return "pending";
        }
        return text.isEmpty() ? "pending" : text;
    }

    private boolean isPaidStatus(String status) {
        return "paid".equals(status)
                || "done".equals(status)
                || "completed".equals(status)
                || "đã đóng".equals(status)
                || "đã thanh toán".equals(status)
                || "da dong".equals(status)
                || "da thanh toan".equals(status);
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).toLowerCase(Locale.ROOT).trim();
        return "true".equals(text) || "1".equals(text) || "yes".equals(text);
    }

    private boolean isDateBeforeToday(String dateText) {
        if (isEmpty(dateText)) {
            return false;
        }
        try {
            Date dueDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateText);
            Date today = new Date();
            return dueDate != null && dueDate.before(today);
        } catch (ParseException ignored) {
            return false;
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

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
