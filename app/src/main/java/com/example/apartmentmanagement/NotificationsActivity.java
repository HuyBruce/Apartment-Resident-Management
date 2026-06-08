package com.example.apartmentmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity {

    private TextView btnBack;
    private LinearLayout layoutNotifications;

    private FirebaseFirestore db;

    private int userId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getIntExtra("user_id", 1);

        btnBack = findViewById(R.id.btnBack);
        layoutNotifications = findViewById(R.id.layoutNotifications);

        btnBack.setOnClickListener(v -> finish());

        loadNotifications();
    }

    private void loadNotifications() {
        layoutNotifications.removeAllViews();

        db.collection("notices")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    layoutNotifications.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        layoutNotifications.addView(createText("Chưa có thông báo nào."));
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot) {
                        Long noticeIdLong = doc.getLong("id");
                        int noticeId = noticeIdLong != null ? noticeIdLong.intValue() : -1;

                        String title = doc.getString("title");
                        String content = doc.getString("content");
                        String type = doc.getString("type");
                        String createdAt = doc.getString("created_at");

                        if (title == null) title = "Thông báo";
                        if (content == null) content = "";
                        if (type == null) type = "general";
                        if (createdAt == null) createdAt = "";

                        View card = createNoticeCard(noticeId, title, content, type, createdAt);
                        layoutNotifications.addView(card);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private View createNoticeCard(int noticeId, String title, String content, String type, String createdAt) {
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

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(17);
        tvTitle.setTextColor(0xFF222222);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvContent = new TextView(this);
        tvContent.setText(content);
        tvContent.setTextSize(14);
        tvContent.setTextColor(0xFF444444);
        tvContent.setPadding(0, 10, 0, 0);

        TextView tvFooter = new TextView(this);
        tvFooter.setText("Loại: " + type + " | " + createdAt);
        tvFooter.setTextSize(12);
        tvFooter.setTextColor(0xFF777777);
        tvFooter.setPadding(0, 10, 0, 0);

        TextView btnMarkRead = new TextView(this);
        btnMarkRead.setText("Đánh dấu đã đọc");
        btnMarkRead.setTextColor(0xFF1976D2);
        btnMarkRead.setTextSize(14);
        btnMarkRead.setPadding(0, 14, 0, 0);

        btnMarkRead.setOnClickListener(v -> markAsRead(noticeId));

        card.addView(tvTitle);
        card.addView(tvContent);
        card.addView(tvFooter);
        card.addView(btnMarkRead);

        return card;
    }

    private void markAsRead(int noticeId) {
        if (noticeId == -1) {
            Toast.makeText(this, "Không xác định được thông báo", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("notice_reads")
                .whereEqualTo("notice_id", noticeId)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();

                        db.collection("notice_reads")
                                .document(docId)
                                .update("is_read", 1, "read_at", String.valueOf(System.currentTimeMillis()))
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, "Đã đánh dấu đã đọc", Toast.LENGTH_SHORT).show()
                                );
                    } else {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", System.currentTimeMillis());
                        data.put("notice_id", noticeId);
                        data.put("user_id", userId);
                        data.put("is_read", 1);
                        data.put("read_at", String.valueOf(System.currentTimeMillis()));

                        db.collection("notice_reads")
                                .document(noticeId + "_" + userId)
                                .set(data)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, "Đã đánh dấu đã đọc", Toast.LENGTH_SHORT).show()
                                );
                    }
                });
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