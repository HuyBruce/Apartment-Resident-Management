package com.example.apartmentmanagement.activities;

import android.widget.Toast;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminParkingActivity extends BaseAdminListActivity {

    private final Map<String, String> apartmentCodes = new HashMap<>();
    private final Map<String, String> apartmentBuildings = new HashMap<>();
    private final Map<String, String> apartmentFloors = new HashMap<>();
    private final Map<String, String> residentNames = new HashMap<>();
    private final Map<String, List<String>> overdueFeeIdsByApartment = new HashMap<>();
    private final Map<String, Double> overdueDebtByApartment = new HashMap<>();

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_admin_parking;
    }

    @Override
    protected String getCollectionName() {
        return "parking_registrations";
    }

    @Override
    protected String getTitleText() {
        return "Quản lý giữ xe";
    }

    @Override
    protected String getSubtitleText() {
        return "Đăng ký, duyệt và chặn giữ xe theo công nợ";
    }

    @Override
    protected String getFilterField() {
        // Không dùng filter mặc định của BaseAdminListActivity vì chip đang hiển thị tiếng Việt
        // còn dữ liệu trong Firestore lưu bằng approved / blocked.
        return null;
    }

    @Override
    protected String[] getFilters() {
        return new String[]{"Tất cả", "Đã duyệt", "Chặn"};
    }

    @Override
    protected String getFabText() {
        return "Làm mới";
    }

    @Override
    protected void onAfterBaseViewsBound() {
        loadLookupData();
    }

    private void loadLookupData() {
        loadApartments();
        loadResidents();
        loadOverdueFees();
    }

    private void loadApartments() {
        db.collection("apartments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    apartmentCodes.clear();
                    apartmentBuildings.clear();
                    apartmentFloors.clear();

                    for (DocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(string(doc, "id"), doc.getId());
                        apartmentCodes.put(id, firstNonEmpty(string(doc, "apartment_code"), "Căn hộ " + id));
                        apartmentBuildings.put(id, string(doc, "building"));
                        apartmentFloors.put(id, string(doc, "floor"));
                    }
                    loadData();
                });
    }

    private void loadResidents() {
        db.collection("residents")
                .get()
                .addOnSuccessListener(snapshot -> {
                    residentNames.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(string(doc, "resident_id"), string(doc, "id"), doc.getId());
                        residentNames.put(id, string(doc, "full_name"));
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

                    for (DocumentSnapshot doc : snapshot) {
                        String status = string(doc, "status").toLowerCase(Locale.ROOT);
                        if (isPaidStatus(status)) {
                            continue;
                        }

                        boolean overdue = isTruthy(doc.get("is_overdue")) || isDateBeforeToday(string(doc, "due_date"));
                        if (!overdue) {
                            continue;
                        }

                        String apartmentId = string(doc, "apartment_id");
                        String feeId = firstNonEmpty(string(doc, "id"), doc.getId());
                        double totalAmount = number(doc, "total_amount");
                        if (totalAmount <= 0) {
                            totalAmount = number(doc, "amount") + number(doc, "penalty_amount");
                        }

                        if (!overdueFeeIdsByApartment.containsKey(apartmentId)) {
                            overdueFeeIdsByApartment.put(apartmentId, new ArrayList<>());
                        }
                        overdueFeeIdsByApartment.get(apartmentId).add(feeId);
                        overdueDebtByApartment.put(apartmentId, overdueDebtByApartment.getOrDefault(apartmentId, 0.0) + totalAmount);
                    }
                    loadData();
                });
    }

    @Override
    protected boolean shouldDisplayDocument(DocumentSnapshot doc) {
        String status = normalizeStatus(string(doc, "status"));

        // Bỏ pending và rejected khỏi trang quản lý giữ xe.
        if (!"approved".equals(status) && !"blocked".equals(status)) {
            return false;
        }

        String selectedFilter = getSelectedFilter();
        if ("Đã duyệt".equalsIgnoreCase(selectedFilter)) {
            return "approved".equals(status);
        }
        if ("Chặn".equalsIgnoreCase(selectedFilter)) {
            return "blocked".equals(status);
        }
        return true;
    }

    @Override
    protected AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc) {
        String status = normalizeStatus(string(doc, "status"));
        String apartmentId = string(doc, "apartment_id");
        String residentId = string(doc, "resident_id");
        boolean hasOverdueDebt = hasOverdueDebt(apartmentId);

        if (hasOverdueDebt && "approved".equals(status)) {
            autoBlockRegistration(doc, apartmentId);
            status = "blocked";
        }

        String apartmentCode = firstNonEmpty(apartmentCodes.get(apartmentId), "Căn hộ ID " + apartmentId);
        String residentName = firstNonEmpty(residentNames.get(residentId), "Cư dân ID " + residentId);
        String vehicleType = firstNonEmpty(string(doc, "vehicle_type"), "Phương tiện");
        String licensePlate = firstNonEmpty(string(doc, "license_plate"), "Chưa có biển số");
        String vehicleModel = string(doc, "vehicle_model");
        String building = apartmentBuildings.get(apartmentId);
        String floor = apartmentFloors.get(apartmentId);
        String blockReason = string(doc, "block_reason");

        AdminRecordAdapter.AdminRecord record = new AdminRecordAdapter.AdminRecord();
        record.documentId = doc.getId();
        record.icon = ownerInitial(residentName);
        record.title = licensePlate;
        record.subtitle = vehicleType + " • " + apartmentCode + " • " + residentName;
        record.status = displayStatus(status);
        record.positiveStatus = "approved".equalsIgnoreCase(status);

        StringBuilder body = new StringBuilder();
        body.append("Căn hộ: ").append(apartmentCode);
        if (!isEmpty(building)) {
            body.append(" - ").append(building);
        }
        if (!isEmpty(floor)) {
            body.append(" - Tầng ").append(floor);
        }
        body.append("\nChủ xe: ").append(residentName);
        if (!isEmpty(vehicleModel)) {
            body.append("\nDòng xe: ").append(vehicleModel);
        }
        body.append("\nNgày đăng ký: ").append(firstNonEmpty(string(doc, "created_at"), "Chưa có"));

        if (hasOverdueDebt) {
            body.append("\nCông nợ quá hạn: ").append(money(overdueDebtByApartment.getOrDefault(apartmentId, 0.0)));
        }
        if ("blocked".equals(status)) {
            body.append("\nLý do bị chặn: ")
                    .append(firstNonEmpty(blockReason, buildBlockReason(apartmentId)));
        }

        record.body = body.toString();

        // Chỉ giữ lại nút Bỏ chặn cho trạng thái Chặn.
        if ("blocked".equals(status)) {
            record.actions.add("Bỏ chặn");
        }

        return record;
    }

    @Override
    protected void onRecordAction(AdminRecordAdapter.AdminRecord record, String action) {
        if ("Bỏ chặn".equals(action)) {
            unblockRegistration(record);
        }
    }

    private void autoBlockRegistration(DocumentSnapshot doc, String apartmentId) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "blocked");
        data.put("block_reason", buildBlockReason(apartmentId));
        data.put("blocked_fee_ids", overdueFeeIdsByApartment.getOrDefault(apartmentId, new ArrayList<>()));
        data.put("updated_at", now());
        db.collection("parking_registrations").document(doc.getId()).update(data);
    }

    private void unblockRegistration(AdminRecordAdapter.AdminRecord record) {
        db.collection("parking_registrations").document(record.documentId)
                .get()
                .addOnSuccessListener(doc -> {
                    String apartmentId = string(doc, "apartment_id");
                    if (hasOverdueDebt(apartmentId)) {
                        Toast.makeText(this, "Chưa thể bỏ chặn vì căn hộ vẫn còn nợ quá hạn", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "approved");
                    data.put("block_reason", "");
                    data.put("blocked_fee_ids", new ArrayList<String>());
                    data.put("unblocked_by", "admin");
                    data.put("unblocked_at", now());
                    data.put("updated_at", now());
                    updateField("parking_registrations", record.documentId, data);
                });
    }

    private String normalizeStatus(String status) {
        String text = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if ("đã duyệt".equals(text) || "da duyet".equals(text)) {
            return "approved";
        }
        if ("chặn".equals(text) || "chan".equals(text) || "bị chặn".equals(text) || "bi chan".equals(text)) {
            return "blocked";
        }
        return text;
    }

    private boolean hasOverdueDebt(String apartmentId) {
        List<String> feeIds = overdueFeeIdsByApartment.get(apartmentId);
        return feeIds != null && !feeIds.isEmpty();
    }

    private String buildBlockReason(String apartmentId) {
        int feeCount = overdueFeeIdsByApartment.getOrDefault(apartmentId, new ArrayList<>()).size();
        double debt = overdueDebtByApartment.getOrDefault(apartmentId, 0.0);
        if (feeCount <= 0) {
            return "Căn hộ chưa đủ điều kiện giữ xe";
        }
        return "Căn hộ còn " + feeCount + " khoản phí quá hạn, tổng nợ " + money(debt);
    }

    private boolean isPaidStatus(String status) {
        return "paid".equals(status)
                || "done".equals(status)
                || "đã đóng".equals(status)
                || "đã thanh toán".equals(status);
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

    private String displayStatus(String status) {
        if ("approved".equalsIgnoreCase(status)) return "Đã duyệt";
        if ("blocked".equalsIgnoreCase(status)) return "Chặn";
        return "Chặn";
    }

    private String ownerInitial(String ownerName) {
        if (ownerName == null) {
            return "?";
        }
        String name = ownerName.trim();
        if (name.isEmpty()) {
            return "?";
        }
        return String.valueOf(Character.toUpperCase(name.charAt(0)));
    }

    private String firstNonEmpty(String... values) {
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
