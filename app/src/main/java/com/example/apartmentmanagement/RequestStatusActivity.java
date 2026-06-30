package com.example.apartmentmanagement;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RequestStatusActivity extends AppCompatActivity {

    private TextView btnBack;
    private LinearLayout layoutRequests;
    private FirebaseFirestore db;
    private int userId;
    private int residentId;
    private String role;

    private final Map<Integer, String> categoryMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_status);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getIntExtra("user_id", 1);
        residentId = getIntent().getIntExtra("resident_id", 1);
        role = getIntent().getStringExtra("role");
        if (role == null) role = "resident";

        btnBack = findViewById(R.id.btnBack);
        layoutRequests = findViewById(R.id.layoutRequests);

        btnBack.setOnClickListener(v -> finish());
        loadCategoriesThenRequests();
    }

    private void loadCategoriesThenRequests() {
        db.collection("request_categories")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    categoryMap.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Long idLong = doc.getLong("id");
                        String name = doc.getString("name");
                        if (idLong != null && name != null) {
                            categoryMap.put(idLong.intValue(), name);
                        }
                    }
                    loadRequests();
                })
                .addOnFailureListener(e -> loadRequests());
    }

    private void loadRequests() {
        layoutRequests.removeAllViews();

        Query query;
        if ("resident".equals(role)) {
            query = db.collection("requests").whereEqualTo("resident_id", residentId);
        } else {
            query = db.collection("requests").orderBy("created_at", Query.Direction.DESCENDING);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    layoutRequests.removeAllViews();
                    if (querySnapshot.isEmpty()) {
                        layoutRequests.addView(createText("Chưa có yêu cầu nào."));
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot) {
                        layoutRequests.addView(createRequestCard(doc));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private View createRequestCard(DocumentSnapshot doc) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);

        String docId = doc.getId();
        Long requestIdLong = doc.getLong("id");
        long requestId = requestIdLong != null ? requestIdLong : 0;

        String title = valueOrDefault(doc.getString("title"), "Yêu cầu");
        String description = valueOrDefault(doc.getString("description"), "");
        String status = valueOrDefault(doc.getString("status"), "pending");
        String createdAt = valueOrDefault(doc.getString("created_at"), "");

        Long categoryIdLong = doc.getLong("category_id");
        String foundCategory = categoryIdLong != null ? categoryMap.get(categoryIdLong.intValue()) : null;
        String categoryName = foundCategory != null ? foundCategory : "Khác";

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF222222);
        tvTitle.setTextSize(17);
        tvTitle.setTypeface(null, Typeface.BOLD);

        TextView tvInfo = new TextView(this);
        if ("resident".equals(role)) {
            tvInfo.setText(
                    "Loại: " + categoryName +
                            "\nTrạng thái: " + convertStatus(status) +
                            "\nNgày gửi: " + createdAt +
                            "\nMô tả: " + description
            );
        } else {
            tvInfo.setText(
                    "Loại: " + categoryName +
                            "\nNgày gửi: " + createdAt +
                            "\nMô tả: " + description
            );
        }
        tvInfo.setTextColor(0xFF444444);
        tvInfo.setTextSize(14);
        tvInfo.setPadding(0, dp(8), 0, 0);

        card.addView(tvTitle);
        card.addView(tvInfo);

        if (!"resident".equals(role)) {
            TextView tvAdminHint = new TextView(this);
            tvAdminHint.setText("Chọn trạng thái xử lý");
            tvAdminHint.setTextColor(0xFF1A2744);
            tvAdminHint.setTextSize(13);
            tvAdminHint.setTypeface(null, Typeface.BOLD);
            tvAdminHint.setPadding(0, dp(10), 0, 0);

            Spinner spinnerStatus = new Spinner(this);
            String[] labels = new String[]{"Chờ xử lý", "Đang xử lý", "Đã xử lý"};
            String[] values = new String[]{"pending", "processing", "done"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    labels
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStatus.setAdapter(adapter);
            spinnerStatus.setSelection(statusIndex(status));

            LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
            );
            spinnerParams.setMargins(0, dp(8), 0, 0);
            spinnerStatus.setLayoutParams(spinnerParams);

            Button btnSave = createSaveButton();
            btnSave.setOnClickListener(v -> {
                String newStatus = values[spinnerStatus.getSelectedItemPosition()];
                updateStatus(docId, requestId, status, newStatus);
            });

            card.addView(tvAdminHint);
            card.addView(spinnerStatus);
            card.addView(btnSave);
        }

        return card;
    }

    private Button createSaveButton() {
        Button button = new Button(this);
        button.setText("Lưu trạng thái");
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(16), 0, dp(16), 0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFF2F5F9E);
        background.setCornerRadius(dp(14));
        button.setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(156), dp(40));
        params.setMargins(0, dp(12), 0, 0);
        params.gravity = Gravity.END;
        button.setLayoutParams(params);

        return button;
    }

    private int statusIndex(String status) {
        if ("processing".equals(status)) return 1;
        if ("done".equals(status)) return 2;
        return 0;
    }

    private void updateStatus(String docId, long requestId, String oldStatus, String newStatus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updated_at", getCurrentTime());
        if ("done".equals(newStatus)) {
            updates.put("completed_at", getCurrentTime());
        }

        db.collection("requests")
                .document(docId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    createStatusHistory(requestId, oldStatus, newStatus);
                    NotificationHelper.showNotification(
                            this,
                            "Cập nhật yêu cầu",
                            "Yêu cầu đã chuyển sang trạng thái: " + convertStatus(newStatus)
                    );
                    Toast.makeText(this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                    loadRequests();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void createStatusHistory(long requestId, String oldStatus, String newStatus) {
        long id = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("request_id", requestId);
        data.put("old_status", oldStatus);
        data.put("new_status", newStatus);
        data.put("note", "Cập nhật trạng thái yêu cầu");
        data.put("changed_by", userId);
        data.put("changed_at", getCurrentTime());

        db.collection("request_status_history")
                .document(String.valueOf(id))
                .set(data);
    }

    private String convertStatus(String status) {
        if ("pending".equals(status)) return "Đang chờ";
        if ("processing".equals(status)) return "Đang xử lý";
        if ("done".equals(status)) return "Hoàn tất";
        return status;
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String valueOrDefault(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView createText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(0xFF777777);
        textView.setTextSize(15);
        textView.setPadding(dp(12), dp(12), dp(12), dp(12));
        return textView;
    }
}
