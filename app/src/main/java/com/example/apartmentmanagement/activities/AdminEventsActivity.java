package com.example.apartmentmanagement.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminEventsActivity extends BaseAdminListActivity {

    private final Map<String, Integer> participantCountByEvent = new HashMap<>();
    private final Map<String, List<RegistrationInfo>> registrationsByEvent = new HashMap<>();
    private final Map<String, String> residentNames = new HashMap<>();
    private final Map<String, String> apartmentCodes = new HashMap<>();
    private final Set<String> completedSyncedDocumentIds = new HashSet<>();

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_admin_events;
    }

    @Override
    protected String getCollectionName() {
        return "events";
    }

    @Override
    protected String getTitleText() {
        return "Quản lý sự kiện";
    }

    @Override
    protected String getSubtitleText() {
        return "Tạo, sửa, hủy và xem đăng ký sự kiện";
    }

    @Override
    protected String getFilterField() {
        // Không dùng filter mặc định vì chip đang hiển thị tiếng Việt,
        // còn Firestore thường lưu status bằng upcoming / completed / cancelled.
        return null;
    }

    @Override
    protected String[] getFilters() {
        return new String[]{"Tất cả", "Sắp diễn ra", "Đã hoàn thành", "Đã hủy"};
    }

    @Override
    protected String getFabText() {
        return "Tạo sự kiện";
    }

    @Override
    protected void onAfterBaseViewsBound() {
        loadLookupData();
    }

    @Override
    protected void onFabClick() {
        showEventDialog(null, null);
    }

    private void loadLookupData() {
        db.collection("residents")
                .get()
                .addOnSuccessListener(snapshot -> {
                    residentNames.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(string(doc, "resident_id"), string(doc, "id"), doc.getId());
                        residentNames.put(id, string(doc, "full_name"));
                    }
                    loadRegistrations();
                });

        db.collection("apartments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    apartmentCodes.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(string(doc, "id"), doc.getId());
                        apartmentCodes.put(id, firstNonEmpty(string(doc, "apartment_code"), "Căn hộ " + id));
                    }
                    loadRegistrations();
                });

        loadRegistrations();
    }

    private void loadRegistrations() {
        db.collection("event_registrations")
                .get()
                .addOnSuccessListener(snapshot -> {
                    participantCountByEvent.clear();
                    registrationsByEvent.clear();

                    for (DocumentSnapshot doc : snapshot) {
                        String status = string(doc, "status").toLowerCase(Locale.ROOT).trim();
                        if ("cancelled".equals(status) || "canceled".equals(status) || "rejected".equals(status)) {
                            continue;
                        }

                        String eventId = string(doc, "event_id");
                        int memberCount = (int) number(doc, "member_count");
                        if (memberCount <= 0) {
                            memberCount = 1;
                        }

                        participantCountByEvent.put(eventId, participantCountByEvent.getOrDefault(eventId, 0) + memberCount);

                        if (!registrationsByEvent.containsKey(eventId)) {
                            registrationsByEvent.put(eventId, new ArrayList<>());
                        }

                        String residentId = string(doc, "resident_id");
                        String apartmentId = string(doc, "apartment_id");
                        RegistrationInfo info = new RegistrationInfo();
                        info.memberNames = firstNonEmpty(string(doc, "member_names"), residentNames.get(residentId), "Người đăng ký");
                        info.residentName = firstNonEmpty(residentNames.get(residentId), "Resident ID " + residentId);
                        info.apartmentCode = firstNonEmpty(apartmentCodes.get(apartmentId), "Căn hộ ID " + apartmentId);
                        info.memberCount = memberCount;
                        info.status = firstNonEmpty(string(doc, "status"), "confirmed");
                        info.note = string(doc, "note");
                        info.createdAt = string(doc, "created_at");
                        registrationsByEvent.get(eventId).add(info);
                    }

                    loadData();
                });
    }

    @Override
    protected boolean shouldDisplayDocument(DocumentSnapshot doc) {
        String selectedFilter = getSelectedFilter();
        String status = getEffectiveStatus(doc);

        if ("Sắp diễn ra".equalsIgnoreCase(selectedFilter)) {
            return "upcoming".equals(status);
        }
        if ("Đã hoàn thành".equalsIgnoreCase(selectedFilter)) {
            return "completed".equals(status);
        }
        if ("Đã hủy".equalsIgnoreCase(selectedFilter)) {
            return "cancelled".equals(status);
        }
        return true;
    }

    @Override
    protected AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc) {
        String eventId = firstNonEmpty(string(doc, "id"), doc.getId());
        String title = firstNonEmpty(string(doc, "title"), string(doc, "name"), "Sự kiện chưa đặt tên");
        String status = getEffectiveStatus(doc);
        String date = string(doc, "event_date");
        String time = string(doc, "event_time");
        String location = string(doc, "location");
        String type = string(doc, "type");
        String targetGroup = string(doc, "target_group");
        String deadline = string(doc, "registration_deadline");
        String description = string(doc, "description");
        boolean requiresRegistration = isTruthy(doc.get("requires_registration"));
        int currentParticipants = Math.max((int) number(doc, "current_participants"), participantCountByEvent.getOrDefault(eventId, 0));
        int maxParticipants = (int) number(doc, "max_participants");

        AdminRecordAdapter.AdminRecord record = new AdminRecordAdapter.AdminRecord();
        record.documentId = doc.getId();
        record.icon = "S";
        record.title = title;
        record.subtitle = firstNonEmpty(date, "Chưa có ngày")
                + (isEmpty(time) ? "" : " • " + time)
                + (isEmpty(location) ? "" : " • " + location);
        record.status = displayStatus(status);
        record.positiveStatus = !"cancelled".equals(status);
        record.extras.put("event_id", eventId);

        StringBuilder body = new StringBuilder();
        body.append("Mã sự kiện: ").append(eventId);
        if (!isEmpty(type)) {
            body.append("\nLoại: ").append(type);
        }
        if (!isEmpty(targetGroup)) {
            body.append("\nĐối tượng: ").append(displayTargetGroup(targetGroup));
        }
        body.append("\nĐăng ký: ").append(requiresRegistration ? "Có" : "Không");
        if (requiresRegistration) {
            body.append("\nSố lượng tham gia: ").append(currentParticipants);
            if (maxParticipants > 0) {
                body.append("/").append(maxParticipants);
            }
            if (!isEmpty(deadline)) {
                body.append("\nHạn đăng ký: ").append(deadline);
            }
        }
        if (!isEmpty(description)) {
            body.append("\nMô tả: ").append(description);
        }
        if ("cancelled".equals(status) && !isEmpty(string(doc, "cancel_reason"))) {
            body.append("\nLý do hủy: ").append(string(doc, "cancel_reason"));
        }
        record.body = body.toString();

        if ("upcoming".equals(status)) {
            record.actions.add("Sửa");
            record.actions.add("DS đăng ký");
            record.actions.add("Hủy");
        } else if ("completed".equals(status)) {
            // Sự kiện đã hoàn thành vẫn cho admin xem danh sách đăng ký,
            // nhưng không cho sửa hoặc hủy nữa.
            record.actions.add("DS đăng ký");
        }
        // Sự kiện đã hủy: không hiển thị nút nào.

        return record;
    }


    @Override
    protected boolean matches(AdminRecordAdapter.AdminRecord record, String keyword) {
        String bodyWithoutTarget = removeTargetGroupLine(record.body);
        String text = (firstNonEmpty(record.title) + " "
                + firstNonEmpty(record.subtitle) + " "
                + firstNonEmpty(bodyWithoutTarget) + " "
                + firstNonEmpty(record.status))
                .toLowerCase(Locale.ROOT);
        return text.contains(keyword);
    }

    private String removeTargetGroupLine(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = body.split("\n");
        for (String line : lines) {
            String normalizedLine = line.trim().toLowerCase(Locale.ROOT);
            if (normalizedLine.startsWith("đối tượng:") || normalizedLine.startsWith("doi tuong:")) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(line);
        }
        return result.toString();
    }

    @Override
    protected void onRecordAction(AdminRecordAdapter.AdminRecord record, String action) {
        if ("Sửa".equals(action)) {
            openEditDialog(record.documentId);
        } else if ("Hủy".equals(action)) {
            showCancelDialog(record.documentId);
        } else if ("DS đăng ký".equals(action)) {
            showRegistrationList(record.extras.get("event_id"), record.title);
        }
    }

    private void openEditDialog(String documentId) {
        db.collection("events").document(documentId)
                .get()
                .addOnSuccessListener(doc -> showEventDialog(documentId, doc))
                .addOnFailureListener(e -> Toast.makeText(this, "Không tải được sự kiện", Toast.LENGTH_SHORT).show());
    }

    private void showEventDialog(String documentId, DocumentSnapshot doc) {
        boolean isEdit = documentId != null;

        ScrollView scrollView = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), 0, dp(20), 0);
        scrollView.addView(form);

        EditText edtTitle = input("Tên sự kiện", InputType.TYPE_CLASS_TEXT);
        EditText edtDescription = input("Mô tả", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edtDescription.setMinLines(3);
        EditText edtDate = input("Ngày sự kiện, ví dụ 2026-09-17", InputType.TYPE_CLASS_DATETIME);
        EditText edtTime = input("Giờ, ví dụ 18:00", InputType.TYPE_CLASS_DATETIME);
        EditText edtLocation = input("Địa điểm", InputType.TYPE_CLASS_TEXT);
        EditText edtMax = input("Số lượng tối đa", InputType.TYPE_CLASS_NUMBER);
        EditText edtDeadline = input("Hạn đăng ký, ví dụ 2026-09-16", InputType.TYPE_CLASS_DATETIME);
        EditText edtType = input("Loại sự kiện, ví dụ gift/service/meeting", InputType.TYPE_CLASS_TEXT);
        EditText edtTarget = input("Đối tượng, ví dụ all/children/elderly", InputType.TYPE_CLASS_TEXT);

        form.addView(edtTitle);
        form.addView(edtDescription);
        form.addView(edtDate);
        form.addView(edtTime);
        form.addView(edtLocation);
        form.addView(edtMax);
        form.addView(edtDeadline);
        form.addView(edtType);
        form.addView(edtTarget);

        if (doc != null && doc.exists()) {
            edtTitle.setText(firstNonEmpty(string(doc, "title"), string(doc, "name")));
            edtDescription.setText(string(doc, "description"));
            edtDate.setText(string(doc, "event_date"));
            edtTime.setText(string(doc, "event_time"));
            edtLocation.setText(string(doc, "location"));
            int max = (int) number(doc, "max_participants");
            edtMax.setText(max <= 0 ? "" : String.valueOf(max));
            edtDeadline.setText(string(doc, "registration_deadline"));
            edtType.setText(string(doc, "type"));
            edtTarget.setText(firstNonEmpty(string(doc, "target_group"), "all"));
        } else {
            edtTarget.setText("all");
            edtType.setText("event");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Sửa sự kiện" : "Tạo sự kiện")
                .setView(scrollView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(isEdit ? "Lưu" : "Tạo", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên sự kiện", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("description", edtDescription.getText().toString().trim());
            data.put("event_date", edtDate.getText().toString().trim());
            data.put("event_time", edtTime.getText().toString().trim());
            data.put("location", edtLocation.getText().toString().trim());
            data.put("max_participants", parseInt(edtMax.getText().toString()));
            data.put("registration_deadline", edtDeadline.getText().toString().trim());
            data.put("type", firstNonEmpty(edtType.getText().toString().trim(), "event"));
            data.put("target_group", firstNonEmpty(edtTarget.getText().toString().trim(), "all"));
            data.put("requires_registration", parseInt(edtMax.getText().toString()) > 0 || !edtDeadline.getText().toString().trim().isEmpty());
            data.put("updated_at", now());

            if (isEdit) {
                db.collection("events").document(documentId).update(data)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Đã cập nhật sự kiện", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadRegistrations();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                String newId = "ev" + System.currentTimeMillis();
                data.put("id", newId);
                data.put("status", "upcoming");
                data.put("current_participants", 0);
                data.put("cancel_reason", "");
                data.put("created_by", 2);
                data.put("created_at", now());
                db.collection("events").document(newId).set(data)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Đã tạo sự kiện", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadRegistrations();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tạo sự kiện: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }));

        dialog.show();
    }

    private void showCancelDialog(String documentId) {
        EditText reason = input("Lý do hủy sự kiện", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        reason.setMinLines(3);
        int padding = dp(20);
        reason.setPadding(padding, 0, padding, 0);

        new AlertDialog.Builder(this)
                .setTitle("Hủy sự kiện")
                .setMessage("Bạn có chắc muốn hủy sự kiện này không?")
                .setView(reason)
                .setNegativeButton("Không", null)
                .setPositiveButton("Hủy sự kiện", (dialog, which) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "cancelled");
                    data.put("cancel_reason", reason.getText().toString().trim());
                    data.put("cancelled_at", now());
                    data.put("updated_at", now());
                    updateField("events", documentId, data);
                })
                .show();
    }

    private void showRegistrationList(String eventId, String eventTitle) {
        List<RegistrationInfo> registrations = registrationsByEvent.get(eventId);
        if (registrations == null || registrations.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Danh sách đăng ký")
                    .setMessage("Sự kiện này chưa có người đăng ký.")
                    .setPositiveButton("Đóng", null)
                    .show();
            return;
        }

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < registrations.size(); i++) {
            RegistrationInfo item = registrations.get(i);
            message.append(i + 1).append(". ").append(item.memberNames)
                    .append("\nCăn hộ: ").append(item.apartmentCode)
                    .append("\nNgười đăng ký: ").append(item.residentName)
                    .append("\nSố lượng: ").append(item.memberCount)
                    .append("\nTrạng thái: ").append(item.status);
            if (!isEmpty(item.note)) {
                message.append("\nGhi chú: ").append(item.note);
            }
            if (!isEmpty(item.createdAt)) {
                message.append("\nNgày đăng ký: ").append(item.createdAt);
            }
            if (i < registrations.size() - 1) {
                message.append("\n\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Đăng ký: " + eventTitle)
                .setMessage(message.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    private EditText input(String hint, int inputType) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setInputType(inputType);
        editText.setTextSize(14);
        editText.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        editText.setLayoutParams(params);
        return editText;
    }

    private String getEffectiveStatus(DocumentSnapshot doc) {
        String status = normalizeStatus(string(doc, "status"));
        if ("cancelled".equals(status)) {
            return "cancelled";
        }

        String eventDate = firstNonEmpty(string(doc, "event_date"), string(doc, "date"));
        if (isPastEventDate(eventDate)) {
            syncCompletedStatusIfNeeded(doc, status);
            return "completed";
        }

        return status;
    }

    private boolean isPastEventDate(String eventDate) {
        Date date = parseDate(eventDate);
        if (date == null) {
            return false;
        }

        Calendar eventDay = Calendar.getInstance();
        eventDay.setTime(date);
        eventDay.set(Calendar.HOUR_OF_DAY, 23);
        eventDay.set(Calendar.MINUTE, 59);
        eventDay.set(Calendar.SECOND, 59);
        eventDay.set(Calendar.MILLISECOND, 999);

        return eventDay.getTime().before(new Date());
    }

    private Date parseDate(String value) {
        if (isEmpty(value)) {
            return null;
        }

        String text = value.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd/MM/yyyy",
                "dd-MM-yyyy"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
                format.setLenient(false);
                return format.parse(text);
            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    private void syncCompletedStatusIfNeeded(DocumentSnapshot doc, String oldStatus) {
        if ("completed".equals(oldStatus) || "cancelled".equals(oldStatus)) {
            return;
        }
        if (completedSyncedDocumentIds.contains(doc.getId())) {
            return;
        }

        completedSyncedDocumentIds.add(doc.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("status", "completed");
        data.put("completed_at", now());
        data.put("updated_at", now());

        db.collection("events").document(doc.getId()).update(data);
    }

    private String normalizeStatus(String status) {
        String text = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if ("sắp diễn ra".equals(text) || "sap dien ra".equals(text)) return "upcoming";
        if ("đã hoàn thành".equals(text) || "da hoan thanh".equals(text) || "done".equals(text)) return "completed";
        if ("đã hủy".equals(text) || "da huy".equals(text) || "canceled".equals(text)) return "cancelled";
        if (text.isEmpty()) return "upcoming";
        return text;
    }

    private String displayStatus(String status) {
        if ("completed".equals(status)) return "Đã hoàn thành";
        if ("cancelled".equals(status)) return "Đã hủy";
        return "Sắp diễn ra";
    }

    private String displayTargetGroup(String targetGroup) {
        String text = targetGroup == null ? "" : targetGroup.trim().toLowerCase(Locale.ROOT);
        if ("all".equals(text)) return "Tất cả";
        if ("children".equals(text)) return "Trẻ em";
        if ("elderly".equals(text)) return "Người cao tuổi";
        return targetGroup;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class RegistrationInfo {
        String memberNames;
        String residentName;
        String apartmentCode;
        String status;
        String note;
        String createdAt;
        int memberCount;
    }
}
