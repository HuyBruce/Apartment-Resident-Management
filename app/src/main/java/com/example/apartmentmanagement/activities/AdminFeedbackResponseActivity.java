package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagement.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminFeedbackResponseActivity extends AppCompatActivity {

    public static final String EXTRA_COLLECTION = "extra_collection";
    public static final String EXTRA_DOCUMENT_ID = "extra_document_id";
    public static final String EXTRA_REQUEST_ID = "extra_request_id";
    public static final String EXTRA_OLD_STATUS = "extra_old_status";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_SUBTITLE = "extra_subtitle";
    public static final String EXTRA_BODY = "extra_body";
    public static final String EXTRA_STATUS = "extra_status";
    public static final String EXTRA_TYPE_LABEL = "extra_type_label";

    private FirebaseFirestore db;

    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvFeedbackTitle;
    private TextView tvFeedbackSubtitle;
    private TextView tvFeedbackType;
    private TextView tvFeedbackStatus;
    private TextView tvFeedbackBody;
    private EditText edtResponse;
    private ProgressBar progressBar;
    private Button btnCancel;
    private Button btnSave;

    private String collection;
    private String documentId;
    private String requestId;
    private String oldStatus;
    private String feedbackTitle;
    private String feedbackSubtitle;
    private String feedbackBody;
    private String feedbackStatus;
    private String typeLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_feedback_response);

        db = FirebaseFirestore.getInstance();
        readIntentData();
        bindViews();
        setupViews();
        setupActions();
    }

    private void readIntentData() {
        collection = safe(getIntent().getStringExtra(EXTRA_COLLECTION));
        documentId = safe(getIntent().getStringExtra(EXTRA_DOCUMENT_ID));
        requestId = safe(getIntent().getStringExtra(EXTRA_REQUEST_ID));
        oldStatus = safe(getIntent().getStringExtra(EXTRA_OLD_STATUS));
        feedbackTitle = safe(getIntent().getStringExtra(EXTRA_TITLE));
        feedbackSubtitle = safe(getIntent().getStringExtra(EXTRA_SUBTITLE));
        feedbackBody = safe(getIntent().getStringExtra(EXTRA_BODY));
        feedbackStatus = safe(getIntent().getStringExtra(EXTRA_STATUS));
        typeLabel = safe(getIntent().getStringExtra(EXTRA_TYPE_LABEL));
    }

    private void bindViews() {
        ImageButton btnBack = findViewById(R.id.btnBackFeedbackResponse);
        tvTitle = findViewById(R.id.tvFeedbackResponseTitle);
        tvSubtitle = findViewById(R.id.tvFeedbackResponseSubtitle);
        tvFeedbackTitle = findViewById(R.id.tvFeedbackResponseItemTitle);
        tvFeedbackSubtitle = findViewById(R.id.tvFeedbackResponseItemSubtitle);
        tvFeedbackType = findViewById(R.id.tvFeedbackResponseType);
        tvFeedbackStatus = findViewById(R.id.tvFeedbackResponseStatus);
        tvFeedbackBody = findViewById(R.id.tvFeedbackResponseBody);
        edtResponse = findViewById(R.id.edtAdminFeedbackResponse);
        progressBar = findViewById(R.id.progressFeedbackResponse);
        btnCancel = findViewById(R.id.btnCancelFeedbackResponse);
        btnSave = findViewById(R.id.btnSaveFeedbackResponse);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupViews() {
        tvTitle.setText("Phản hồi phản ánh");
        tvSubtitle.setText("Nhập nội dung phản hồi để gửi lại cho cư dân");
        tvFeedbackTitle.setText(isEmpty(feedbackTitle) ? "Nội dung phản ánh" : feedbackTitle);
        tvFeedbackSubtitle.setText(isEmpty(feedbackSubtitle) ? "Cư dân chưa xác định" : feedbackSubtitle);
        tvFeedbackType.setText(isEmpty(typeLabel) ? "Phản ánh" : typeLabel);
        tvFeedbackStatus.setText(isEmpty(feedbackStatus) ? "Đang xử lý" : feedbackStatus);
        tvFeedbackBody.setText(isEmpty(feedbackBody) ? "Không có nội dung chi tiết." : feedbackBody);
    }

    private void setupActions() {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveResponse());
    }

    private void saveResponse() {
        String response = edtResponse.getText().toString().trim();
        if (response.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung phản hồi", Toast.LENGTH_SHORT).show();
            return;
        }
        if (collection.isEmpty() || documentId.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin phản ánh cần phản hồi", Toast.LENGTH_SHORT).show();
            return;
        }

        setSaving(true);
        String now = now();
        Map<String, Object> updates = new HashMap<>();
        updates.put("admin_response", response);
        updates.put("status", "responded");
        updates.put("responded_at", now);
        updates.put("updated_at", now);

        db.collection(collection).document(documentId).update(updates)
                .addOnSuccessListener(unused -> {
                    createHistory(response);
                    Toast.makeText(this, "Đã phản hồi cư dân", Toast.LENGTH_SHORT).show();
                    setSaving(false);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setSaving(false);
                    Toast.makeText(this, "Lỗi lưu phản hồi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createHistory(String response) {
        long id = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("request_id", requestId);
        data.put("source_collection", collection);
        data.put("source_document_id", documentId);
        data.put("old_status", oldStatus);
        data.put("new_status", "responded");
        data.put("note", "Admin phản hồi: " + response);
        data.put("changed_by", 2);
        data.put("changed_at", now());
        db.collection("request_status_history").document(String.valueOf(id)).set(data);
    }

    private void setSaving(boolean saving) {
        progressBar.setVisibility(saving ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!saving);
        btnCancel.setEnabled(!saving);
        edtResponse.setEnabled(!saving);
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
