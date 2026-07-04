package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagement.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminEventFormActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_ADD = "add";
    public static final String MODE_EDIT = "edit";
    public static final String EXTRA_DOCUMENT_ID = "document_id";

    private static final String[] TYPE_LABELS = {"Sự kiện chung", "Quà tặng", "Dịch vụ", "Họp cư dân", "Khác"};
    private static final String[] TYPE_VALUES = {"event", "gift", "service", "meeting", "other"};

    private static final String[] TARGET_LABELS = {"Tất cả", "Trẻ em", "Người cao tuổi"};
    private static final String[] TARGET_VALUES = {"all", "children", "elderly"};

    private FirebaseFirestore db;
    private TextView tvTitle, tvSubtitle;
    private EditText edtTitle, edtDescription, edtDate, edtTime, edtLocation, edtMaxParticipants, edtDeadline;
    private Spinner spnType, spnTarget;
    private ProgressBar progressBar;
    private Button btnCancel, btnSave;

    private boolean isEdit;
    private String documentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_form);

        db = FirebaseFirestore.getInstance();
        isEdit = MODE_EDIT.equalsIgnoreCase(getIntent().getStringExtra(EXTRA_MODE));
        documentId = value(getIntent().getStringExtra(EXTRA_DOCUMENT_ID));

        bindViews();
        setupHeader();
        setupSpinners();

        if (isEdit) {
            loadEvent();
        }
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvEventFormTitle);
        tvSubtitle = findViewById(R.id.tvEventFormSubtitle);
        edtTitle = findViewById(R.id.edtEventTitle);
        edtDescription = findViewById(R.id.edtEventDescription);
        edtDate = findViewById(R.id.edtEventDate);
        edtTime = findViewById(R.id.edtEventTime);
        edtLocation = findViewById(R.id.edtEventLocation);
        edtMaxParticipants = findViewById(R.id.edtEventMaxParticipants);
        edtDeadline = findViewById(R.id.edtEventDeadline);
        spnType = findViewById(R.id.spnEventType);
        spnTarget = findViewById(R.id.spnEventTarget);
        progressBar = findViewById(R.id.progressEventForm);
        btnCancel = findViewById(R.id.btnCancelEventForm);
        btnSave = findViewById(R.id.btnSaveEventForm);

        ImageButton btnBack = findViewById(R.id.btnBackEventForm);
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void setupHeader() {
        tvTitle.setText(isEdit ? "Sửa sự kiện" : "Tạo sự kiện");
        tvSubtitle.setText(isEdit ? "Cập nhật thông tin sự kiện" : "Tạo sự kiện mới cho cư dân đăng ký tham gia");
        btnSave.setText(isEdit ? "Lưu thay đổi" : "Tạo sự kiện");
    }

    private void setupSpinners() {
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TYPE_LABELS);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnType.setAdapter(typeAdapter);

        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TARGET_LABELS);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTarget.setAdapter(targetAdapter);
    }

    private void loadEvent() {
        if (documentId.isEmpty()) {
            toast("Không tìm thấy sự kiện cần sửa");
            finish();
            return;
        }

        setLoading(true);
        db.collection("events").document(documentId)
                .get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    if (doc == null || !doc.exists()) {
                        toast("Sự kiện không tồn tại");
                        finish();
                        return;
                    }
                    fillEventData(doc);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi tải sự kiện: " + e.getMessage());
                    finish();
                });
    }

    private void fillEventData(DocumentSnapshot doc) {
        edtTitle.setText(firstNonEmpty(string(doc, "title"), string(doc, "name")));
        edtDescription.setText(string(doc, "description"));
        edtDate.setText(firstNonEmpty(string(doc, "event_date"), string(doc, "date")));
        edtTime.setText(string(doc, "event_time"));
        edtLocation.setText(string(doc, "location"));

        int maxParticipants = (int) number(doc, "max_participants");
        edtMaxParticipants.setText(maxParticipants <= 0 ? "" : String.valueOf(maxParticipants));
        edtDeadline.setText(string(doc, "registration_deadline"));

        setSpinnerSelection(spnType, TYPE_VALUES, string(doc, "type"));
        setSpinnerSelection(spnTarget, TARGET_VALUES, firstNonEmpty(string(doc, "target_group"), "all"));
    }

    private void saveEvent() {
        String title = value(edtTitle.getText().toString());
        String description = value(edtDescription.getText().toString());
        String date = value(edtDate.getText().toString());
        String time = value(edtTime.getText().toString());
        String location = value(edtLocation.getText().toString());
        int maxParticipants = parseInt(edtMaxParticipants.getText().toString());
        String deadline = value(edtDeadline.getText().toString());
        String type = TYPE_VALUES[Math.max(0, spnType.getSelectedItemPosition())];
        String target = TARGET_VALUES[Math.max(0, spnTarget.getSelectedItemPosition())];

        if (title.isEmpty()) {
            toast("Vui lòng nhập tên sự kiện");
            return;
        }
        if (date.isEmpty()) {
            toast("Vui lòng nhập ngày sự kiện");
            return;
        }
        if (time.isEmpty()) {
            toast("Vui lòng nhập giờ sự kiện");
            return;
        }
        if (location.isEmpty()) {
            toast("Vui lòng nhập địa điểm");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", description);
        data.put("event_date", date);
        data.put("event_time", time);
        data.put("location", location);
        data.put("max_participants", maxParticipants);
        data.put("registration_deadline", deadline);
        data.put("type", type);
        data.put("target_group", target);
        data.put("requires_registration", maxParticipants > 0 || !deadline.isEmpty());
        data.put("updated_at", now());

        setLoading(true);
        if (isEdit) {
            updateEvent(data);
        } else {
            createEvent(data);
        }
    }

    private void createEvent(Map<String, Object> data) {
        String newId = "ev" + System.currentTimeMillis();
        data.put("id", newId);
        data.put("status", "upcoming");
        data.put("current_participants", 0);
        data.put("cancel_reason", "");
        data.put("created_by", 2);
        data.put("created_at", now());

        db.collection("events").document(newId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    toast("Đã tạo sự kiện");
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi tạo sự kiện: " + e.getMessage());
                });
    }

    private void updateEvent(Map<String, Object> data) {
        db.collection("events").document(documentId)
                .update(data)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    toast("Đã cập nhật sự kiện");
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi cập nhật: " + e.getMessage());
                });
    }

    private void setSpinnerSelection(Spinner spinner, String[] values, String targetValue) {
        String normalized = value(targetValue).toLowerCase(Locale.ROOT);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(normalized)) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(0);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
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

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String firstNonEmpty(String... values) {
        for (String item : values) {
            if (item != null && !item.trim().isEmpty()) {
                return item.trim();
            }
        }
        return "";
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
