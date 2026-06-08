package com.example.apartmentmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
            query = db.collection("requests")
                    .whereEqualTo("resident_id", residentId);
        } else {
            query = db.collection("requests")
                    .orderBy("created_at", Query.Direction.DESCENDING);
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
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        String docId = doc.getId();

        Long requestIdLong = doc.getLong("id");
        long requestId = requestIdLong != null ? requestIdLong : 0;

        String rawTitle = doc.getString("title");
        final String title = rawTitle != null ? rawTitle : "Yêu cầu";

        String rawDescription = doc.getString("description");
        final String description = rawDescription != null ? rawDescription : "";

        String rawStatus = doc.getString("status");
        final String status = rawStatus != null ? rawStatus : "pending";

        String rawCreatedAt = doc.getString("created_at");
        final String createdAt = rawCreatedAt != null ? rawCreatedAt : "";

        Long categoryIdLong = doc.getLong("category_id");
        String foundCategory = (categoryIdLong != null) ? categoryMap.get(categoryIdLong.intValue()) : null;
        final String categoryName = (foundCategory != null) ? foundCategory : "Khác";

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF222222);
        tvTitle.setTextSize(17);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(
                "Loại: " + categoryName +
                        "\nTrạng thái: " + convertStatus(status) +
                        "\nNgày gửi: " + createdAt +
                        "\nMô tả: " + description
        );
        tvInfo.setTextColor(0xFF444444);
        tvInfo.setTextSize(14);
        tvInfo.setPadding(0, 10, 0, 0);

        card.addView(tvTitle);
        card.addView(tvInfo);

        if (!"resident".equals(role)) {
            LinearLayout buttonRow = new LinearLayout(this);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);
            buttonRow.setPadding(0, 14, 0, 0);

            Button btnPending = createButton("Pending");
            Button btnProcessing = createButton("Processing");
            Button btnDone = createButton("Done");

            btnPending.setOnClickListener(v -> updateStatus(docId, requestId, status, "pending"));
            btnProcessing.setOnClickListener(v -> updateStatus(docId, requestId, status, "processing"));
            btnDone.setOnClickListener(v -> updateStatus(docId, requestId, status, "done"));

            buttonRow.addView(btnPending);
            buttonRow.addView(btnProcessing);
            buttonRow.addView(btnDone);

            card.addView(buttonRow);
        }

        return card;
    }

    private Button createButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(11);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        params.setMargins(4, 0, 4, 0);
        button.setLayoutParams(params);

        return button;
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

    private TextView createText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(0xFF777777);
        textView.setTextSize(15);
        textView.setPadding(12, 12, 12, 12);
        return textView;
    }
}