package com.example.apartmentmanagement.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class AdminFeedbackActivity extends BaseAdminListActivity {

    private LinearLayout layoutFilterToggle;
    private LinearLayout layoutFilterPanel;
    private TextView tvFilterSummary;
    private TextView tvFilterArrow;
    private ChipGroup chipGroupType;
    private ChipGroup chipGroupStatus;

    private final Map<String, String> residentNames = new HashMap<>();
    private final Map<String, String> residentApartments = new HashMap<>();
    private final Map<String, String> apartmentCodes = new HashMap<>();
    private final Map<String, String> categoryNames = new HashMap<>();

    private static final String[] TYPE_FILTERS = new String[]{"Tất cả loại", "Yêu cầu", "Phản ánh"};
    private static final String[] STATUS_FILTERS = new String[]{"Tất cả", "Đang xử lý", "Hoàn thành", "Từ chối"};

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_admin_feedback;
    }

    @Override
    protected String getCollectionName() {
        // Activity này tự load cả requests và complaints trong loadData().
        return "requests";
    }

    @Override
    protected String getTitleText() {
        return "Phản ánh / Yêu cầu";
    }

    @Override
    protected String getSubtitleText() {
        return "Tiếp nhận, phản hồi và cập nhật trạng thái";
    }

    @Override
    protected String getFabText() {
        return "Làm mới";
    }

    @Override
    protected void onAfterBaseViewsBound() {
        bindFilterViews();
        setupDropdownFilters();
        loadLookupData();
    }

    @Override
    protected void setupFilters() {
        // Không dùng chipGroupAdmin mặc định vì màn này có filter dropdown riêng.
    }

    @Override
    protected void onFabClick() {
        loadLookupData();
        loadData();
    }

    private void bindFilterViews() {
        layoutFilterToggle = findViewById(R.id.layoutFeedbackFilterToggle);
        layoutFilterPanel = findViewById(R.id.layoutFeedbackFilterPanel);
        tvFilterSummary = findViewById(R.id.tvFeedbackFilterSummary);
        tvFilterArrow = findViewById(R.id.tvFeedbackFilterArrow);
        chipGroupType = findViewById(R.id.chipGroupFeedbackType);
        chipGroupStatus = findViewById(R.id.chipGroupFeedbackStatus);
    }

    private void setupDropdownFilters() {
        setupChipGroup(chipGroupType, TYPE_FILTERS);
        setupChipGroup(chipGroupStatus, STATUS_FILTERS);
        updateFilterSummary();

        if (layoutFilterToggle != null) {
            layoutFilterToggle.setOnClickListener(v -> {
                boolean show = layoutFilterPanel.getVisibility() != View.VISIBLE;
                layoutFilterPanel.setVisibility(show ? View.VISIBLE : View.GONE);
                tvFilterArrow.setText(show ? "▲" : "▼");
            });
        }

        ChipGroup.OnCheckedStateChangeListener listener = (group, checkedIds) -> {
            updateFilterSummary();
            applyCustomFilters();
        };
        if (chipGroupType != null) chipGroupType.setOnCheckedStateChangeListener(listener);
        if (chipGroupStatus != null) chipGroupStatus.setOnCheckedStateChangeListener(listener);
    }

    private void setupChipGroup(ChipGroup group, String[] labels) {
        if (group == null) return;
        group.removeAllViews();
        for (int i = 0; i < labels.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            group.addView(chip);
        }
    }

    private void updateFilterSummary() {
        String type = selectedChipText(chipGroupType, "Tất cả loại");
        String status = selectedChipText(chipGroupStatus, "Tất cả");
        if (tvFilterSummary != null) {
            tvFilterSummary.setText(type + " • " + status);
        }
    }

    private String selectedChipText(ChipGroup group, String fallback) {
        if (group == null) return fallback;
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) return fallback;
        Chip chip = group.findViewById(id);
        return chip == null ? fallback : chip.getText().toString();
    }

    private void loadLookupData() {
        db.collection("request_categories")
                .get()
                .addOnSuccessListener(snapshot -> {
                    categoryNames.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(string(doc, "id"), doc.getId());
                        categoryNames.put(id, string(doc, "name"));
                    }
                    loadData();
                });

        db.collection("residents")
                .get()
                .addOnSuccessListener(snapshot -> {
                    residentNames.clear();
                    residentApartments.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(string(doc, "resident_id"), string(doc, "id"), doc.getId());
                        residentNames.put(id, firstNonEmpty(string(doc, "full_name"), "Cư dân ID " + id));
                        residentApartments.put(id, string(doc, "apartment_number"));
                    }
                    loadData();
                });

        db.collection("apartments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    apartmentCodes.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(string(doc, "id"), doc.getId());
                        String code = firstNonEmpty(string(doc, "apartment_code"), string(doc, "apartment_number"), id);
                        apartmentCodes.put(id, code);
                    }
                    loadData();
                });
    }

    @Override
    protected void loadData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);

        db.collection("requests")
                .get()
                .addOnSuccessListener(requestSnapshot -> {
                    List<AdminRecordAdapter.AdminRecord> merged = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : requestSnapshot) {
                        merged.add(mapFeedbackRecord(doc, "requests"));
                    }

                    db.collection("complaints")
                            .get()
                            .addOnSuccessListener(complaintSnapshot -> {
                                for (QueryDocumentSnapshot doc : complaintSnapshot) {
                                    merged.add(mapFeedbackRecord(doc, "complaints"));
                                }

                                allRecords.clear();
                                allRecords.addAll(merged);
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                applyCustomFilters();
                            })
                            .addOnFailureListener(e -> {
                                allRecords.clear();
                                allRecords.addAll(merged);
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                applyCustomFilters();
                                Toast.makeText(this, "Không tải được complaints: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tải được requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc) {
        // Không dùng trực tiếp vì activity này cần biết document thuộc collection nào.
        return mapFeedbackRecord(doc, "requests");
    }

    private AdminRecordAdapter.AdminRecord mapFeedbackRecord(DocumentSnapshot doc, String sourceCollection) {
        String sourceType = "complaints".equals(sourceCollection) ? "complaint" : "request";
        String typeLabel = determineTypeLabel(doc, sourceCollection);
        String rawStatus = normalizeStatus(string(doc, "status"));
        String displayStatus = displayStatus(rawStatus);
        String residentId = firstNonEmpty(string(doc, "resident_id"), string(doc, "residentId"));
        String apartmentId = firstNonEmpty(string(doc, "apartment_id"), string(doc, "apartmentId"));
        String residentName = firstNonEmpty(residentNames.get(residentId), "Cư dân ID " + residentId);
        String apartmentCode = firstNonEmpty(apartmentCodes.get(apartmentId), residentApartments.get(residentId), "Căn hộ ID " + apartmentId);
        String title = firstNonEmpty(string(doc, "title"), "Nội dung chưa có tiêu đề");
        String description = firstNonEmpty(string(doc, "description"), string(doc, "content"));
        String priority = displayPriority(string(doc, "priority"));
        String createdAt = firstNonEmpty(string(doc, "created_at"), string(doc, "createdAt"));
        String adminResponse = firstNonEmpty(string(doc, "admin_response"), string(doc, "response"));
        String category = displayCategory(doc, sourceCollection);

        AdminRecordAdapter.AdminRecord record = new AdminRecordAdapter.AdminRecord();
        record.documentId = doc.getId();
        record.icon = firstLetter(residentName);
        record.title = title;
        record.subtitle = residentName + " • " + apartmentCode;
        record.status = displayStatus;
        record.positiveStatus = "Hoàn thành".equals(displayStatus) || "Đã phản hồi".equals(displayStatus);
        record.extras.put("source_collection", sourceCollection);
        record.extras.put("source_type", sourceType);
        record.extras.put("raw_status", rawStatus);
        record.extras.put("type_label", typeLabel);
        record.extras.put("request_id", firstNonEmpty(string(doc, "id"), doc.getId()));

        StringBuilder body = new StringBuilder();
        body.append("Loại: ").append(typeLabel);
        if (!isEmpty(category)) body.append(" • ").append(category);
        if (!isEmpty(priority)) body.append("\nMức độ: ").append(priority);
        if (!isEmpty(createdAt)) body.append("\nNgày gửi: ").append(createdAt);
        if (!isEmpty(description)) body.append("\nNội dung: ").append(description);
        if (!isEmpty(adminResponse)) body.append("\nPhản hồi admin: ").append(adminResponse);
        record.body = body.toString();

        if ("Đang xử lý".equals(displayStatus)) {
            if ("Yêu cầu".equals(typeLabel)) {
                record.actions.add("Hoàn thành");
                record.actions.add("Từ chối");
            } else if ("Phản ánh".equals(typeLabel)) {
                record.actions.add("Phản hồi");
                record.actions.add("Từ chối");
            }
        }

        return record;
    }

    private void applyCustomFilters() {
        String selectedType = selectedChipText(chipGroupType, "Tất cả loại");
        String selectedStatus = selectedChipText(chipGroupStatus, "Tất cả");
        List<AdminRecordAdapter.AdminRecord> visible = new ArrayList<>();

        for (AdminRecordAdapter.AdminRecord record : allRecords) {
            String type = record.extras.get("type_label");
            if (!"Tất cả loại".equals(selectedType) && !selectedType.equals(type)) {
                continue;
            }
            if (!"Tất cả".equals(selectedStatus) && !selectedStatus.equals(record.status)) {
                continue;
            }
            visible.add(record);
        }

        adapter.submitList(visible);
        if (emptyLayout != null) emptyLayout.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        if (emptyText != null) emptyText.setText(visible.isEmpty() ? "Không có phản ánh/yêu cầu phù hợp" : "");
        if (subtitleView != null) subtitleView.setText(getSubtitleText() + " • " + visible.size() + " bản ghi");
    }

    @Override
    protected void onRecordAction(AdminRecordAdapter.AdminRecord record, String action) {
        if ("Phản hồi".equals(action)) {
            showResponseDialog(record);
        } else if ("Hoàn thành".equals(action)) {
            updateStatus(record, "done", "Đã đánh dấu hoàn thành");
        } else if ("Từ chối".equals(action)) {
            showRejectDialog(record);
        }
    }

    private void showResponseDialog(AdminRecordAdapter.AdminRecord record) {
        EditText input = new EditText(this);
        input.setHint("Nhập nội dung phản hồi cho cư dân");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        input.setPadding(dp(20), 0, dp(20), 0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Phản hồi")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String response = input.getText().toString().trim();
            if (response.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung phản hồi", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("admin_response", response);
            updates.put("status", "responded");
            updates.put("responded_at", now());
            updates.put("updated_at", now());
            String collection = record.extras.get("source_collection");
            String oldStatus = record.extras.get("raw_status");
            db.collection(collection).document(record.documentId).update(updates)
                    .addOnSuccessListener(unused -> {
                        createHistory(record, oldStatus, "responded", "Admin phản hồi: " + response);
                        Toast.makeText(this, "Đã phản hồi", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadData();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu phản hồi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }));
        dialog.show();
    }

    private void showRejectDialog(AdminRecordAdapter.AdminRecord record) {
        EditText input = new EditText(this);
        input.setHint("Nhập lý do từ chối");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        input.setPadding(dp(20), 0, dp(20), 0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Từ chối")
                .setMessage("Bạn có chắc muốn từ chối nội dung này không?")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Từ chối", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập lý do từ chối", Toast.LENGTH_SHORT).show();
                return;
            }
            updateStatus(record, "rejected", "Từ chối: " + reason, reason);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void updateStatus(AdminRecordAdapter.AdminRecord record, String newStatus, String note) {
        updateStatus(record, newStatus, note, "");
    }

    private void updateStatus(AdminRecordAdapter.AdminRecord record, String newStatus, String note, String reasonOrResponse) {
        String collection = record.extras.get("source_collection");
        String oldStatus = record.extras.get("raw_status");
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updated_at", now());

        if ("done".equals(newStatus)) {
            updates.put("completed_at", now());
            updates.put("resolved_at", now());
        }
        if ("rejected".equals(newStatus)) {
            updates.put("rejected_at", now());
            updates.put("reject_reason", reasonOrResponse);
            updates.put("admin_response", reasonOrResponse);
        }

        db.collection(collection).document(record.documentId).update(updates)
                .addOnSuccessListener(unused -> {
                    createHistory(record, oldStatus, newStatus, note);
                    Toast.makeText(this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createHistory(AdminRecordAdapter.AdminRecord record, String oldStatus, String newStatus, String note) {
        long id = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("request_id", record.extras.get("request_id"));
        data.put("source_collection", record.extras.get("source_collection"));
        data.put("source_document_id", record.documentId);
        data.put("old_status", oldStatus);
        data.put("new_status", newStatus);
        data.put("note", note);
        data.put("changed_by", 2);
        data.put("changed_at", now());
        db.collection("request_status_history").document(String.valueOf(id)).set(data);
    }

    private String determineTypeLabel(DocumentSnapshot doc, String sourceCollection) {
        String text = normalizeNoAccent(
                firstNonEmpty(string(doc, "category"), "") + " "
                        + firstNonEmpty(string(doc, "title"), "") + " "
                        + firstNonEmpty(string(doc, "description"), "") + " "
                        + displayCategory(doc, sourceCollection)
        );

        if ("complaints".equals(sourceCollection)) {
            // Gộp phản ánh và khiếu nại vào cùng một nhóm xử lý.
            return "Phản ánh";
        }

        // Gộp yêu cầu và sửa chữa vào cùng một nhóm xử lý.
        return "Yêu cầu";
    }

    private String displayCategory(DocumentSnapshot doc, String sourceCollection) {
        if ("complaints".equals(sourceCollection)) {
            return displayComplaintCategory(string(doc, "category"));
        }
        String categoryId = string(doc, "category_id");
        return firstNonEmpty(categoryNames.get(categoryId), "Khác");
    }

    private String displayComplaintCategory(String category) {
        if (isEmpty(category)) return "Khác";
        String normalized = normalizeNoAccent(category);
        if (normalized.contains("vi pham")) return "Vi phạm nội quy";
        if (normalized.contains("ve sinh")) return "Vệ sinh";
        if (normalized.contains("an ninh")) return "An ninh";
        if (normalized.contains("khieu nai")) return "Khiếu nại";
        return category.replace("_", " ");
    }

    private String normalizeStatus(String status) {
        String s = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (s.equals("done") || s.equals("completed") || s.equals("complete") || s.equals("resolved")) return "done";
        if (s.equals("responded") || s.equals("response") || s.equals("replied") || s.equals("answered")) return "responded";
        if (s.equals("rejected") || s.equals("reject") || s.equals("cancelled") || s.equals("canceled") || s.equals("refused")) return "rejected";
        return "processing";
    }

    private String displayStatus(String status) {
        String normalized = normalizeStatus(status);
        if ("done".equals(normalized)) return "Hoàn thành";
        if ("responded".equals(normalized)) return "Đã phản hồi";
        if ("rejected".equals(normalized)) return "Từ chối";
        return "Đang xử lý";
    }

    private String displayPriority(String priority) {
        String p = priority == null ? "" : priority.trim().toLowerCase(Locale.ROOT);
        if (p.equals("high") || p.equals("urgent")) return "Khẩn";
        if (p.equals("low")) return "Thấp";
        if (p.equals("normal") || p.equals("medium")) return "Bình thường";
        return "";
    }

    private String firstLetter(String value) {
        if (value == null || value.trim().isEmpty()) return "?";
        return value.trim().substring(0, 1).toUpperCase(new Locale("vi", "VN"));
    }

    private String normalizeNoAccent(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        return normalized.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
