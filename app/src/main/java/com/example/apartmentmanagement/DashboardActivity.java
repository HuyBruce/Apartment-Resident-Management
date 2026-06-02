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

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvUnreadNotices;
    private TextView tvUnpaidFees;
    private TextView tvPendingRequests;
    private TextView tvProcessingRequests;
    private TextView tvDoneRequests;

    private android.widget.ImageButton btnBack;
    private LinearLayout headerNotices, headerRequests;
    private android.widget.ImageView ivToggleNotices, ivToggleRequests;

    private LinearLayout layoutLatestNotices;
    private LinearLayout layoutRecentRequests;

    private FirebaseFirestore db;

    // Dữ liệu mẫu sau khi bạn import SQLite lên Firestore
    private int userId = 1;
    private int residentId = 1;
    private int apartmentId = 1;

    private final Map<Integer, String> requestCategoryMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseFirestore.getInstance();

        initViews();

        // Nếu LoginActivity có truyền dữ liệu qua thì lấy ở đây.
        userId = getIntent().getIntExtra("user_id", 1);
        residentId = getIntent().getIntExtra("resident_id", 1);
        apartmentId = getIntent().getIntExtra("apartment_id", 1);

        loadDashboard();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUnreadNotices = findViewById(R.id.tvUnreadNotices);
        tvUnpaidFees = findViewById(R.id.tvUnpaidFees);
        tvPendingRequests = findViewById(R.id.tvPendingRequests);
        tvProcessingRequests = findViewById(R.id.tvProcessingRequests);
        tvDoneRequests = findViewById(R.id.tvDoneRequests);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        headerNotices = findViewById(R.id.headerNotices);
        ivToggleNotices = findViewById(R.id.ivToggleNotices);
        layoutLatestNotices = findViewById(R.id.layoutLatestNotices);

        headerRequests = findViewById(R.id.headerRequests);
        ivToggleRequests = findViewById(R.id.ivToggleRequests);
        layoutRecentRequests = findViewById(R.id.layoutRecentRequests);

        headerNotices.setOnClickListener(v -> toggleSection(layoutLatestNotices, ivToggleNotices));
        headerRequests.setOnClickListener(v -> toggleSection(layoutRecentRequests, ivToggleRequests));
    }

    private void toggleSection(View layout, android.widget.ImageView icon) {
        if (layout.getVisibility() == View.VISIBLE) {
            layout.setVisibility(View.GONE);
            icon.setRotation(0);
        } else {
            layout.setVisibility(View.VISIBLE);
            icon.setRotation(180);
        }
    }

    private void loadDashboard() {
        loadResidentInfo();
        loadUnreadNoticeCount();
        loadUnpaidFees();
        loadRequestCounts();
        loadRequestCategoriesThenRecentRequests();
        loadLatestNotices();
    }

    private void loadResidentInfo() {
        db.collection("residents")
                .document(String.valueOf(residentId))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("full_name");

                        Long apartmentIdLong = documentSnapshot.getLong("apartment_id");
                        if (apartmentIdLong != null) {
                            apartmentId = apartmentIdLong.intValue();
                        }

                        if (fullName == null || fullName.trim().isEmpty()) {
                            fullName = "Cư dân";
                        }

                        tvWelcome.setText("Xin chào, " + fullName);
                    } else {
                        tvWelcome.setText("Xin chào, cư dân");
                    }
                })
                .addOnFailureListener(e -> {
                    tvWelcome.setText("Xin chào, cư dân");
                    showError("Không tải được thông tin cư dân");
                });
    }

    private void loadUnreadNoticeCount() {
        db.collection("notice_reads")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("is_read", 0)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int unreadCount = queryDocumentSnapshots.size();
                    tvUnreadNotices.setText(String.valueOf(unreadCount));
                })
                .addOnFailureListener(e -> {
                    tvUnreadNotices.setText("0");
                    showError("Không tải được số thông báo chưa đọc");
                });
    }

    private void loadUnpaidFees() {
        db.collection("fees")
                .whereEqualTo("apartment_id", apartmentId)
                .whereEqualTo("status", "unpaid")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double total = 0;

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Double amountDouble = document.getDouble("amount");

                        if (amountDouble != null) {
                            total += amountDouble;
                        } else {
                            Long amountLong = document.getLong("amount");
                            if (amountLong != null) {
                                total += amountLong;
                            }
                        }
                    }

                    tvUnpaidFees.setText(formatCurrency(total));
                })
                .addOnFailureListener(e -> {
                    tvUnpaidFees.setText(formatCurrency(0));
                    showError("Không tải được phí chưa thanh toán");
                });
    }

    private void loadRequestCounts() {
        loadRequestCountByStatus("pending", tvPendingRequests);
        loadRequestCountByStatus("processing", tvProcessingRequests);
        loadRequestCountByStatus("done", tvDoneRequests);
    }

    private void loadRequestCountByStatus(String status, TextView targetTextView) {
        db.collection("requests")
                .whereEqualTo("resident_id", residentId)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    targetTextView.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    targetTextView.setText("0");
                    showError("Không tải được số yêu cầu " + status);
                });
    }

    private void loadLatestNotices() {
        layoutLatestNotices.removeAllViews();

        db.collection("notices")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutLatestNotices.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        layoutLatestNotices.addView(createSimpleTextView("Chưa có thông báo nào."));
                        return;
                    }

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String title = document.getString("title");
                        String content = document.getString("content");
                        String createdAt = document.getString("created_at");
                        String type = document.getString("type");

                        if (title == null) title = "Không có tiêu đề";
                        if (content == null) content = "";
                        if (createdAt == null) createdAt = "";
                        if (type == null) type = "general";

                        View card = createCardView(
                                title,
                                content,
                                "Loại: " + type + " | " + createdAt
                        );

                        layoutLatestNotices.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    layoutLatestNotices.removeAllViews();
                    layoutLatestNotices.addView(createSimpleTextView("Không tải được thông báo."));
                    showError("Không tải được danh sách thông báo");
                });
    }

    private void loadRequestCategoriesThenRecentRequests() {
        requestCategoryMap.clear();

        db.collection("request_categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Long idLong = document.getLong("id");
                        String name = document.getString("name");

                        if (idLong != null && name != null) {
                            requestCategoryMap.put(idLong.intValue(), name);
                        }
                    }

                    loadRecentRequests();
                })
                .addOnFailureListener(e -> {
                    loadRecentRequests();
                });
    }

    private void loadRecentRequests() {
        layoutRecentRequests.removeAllViews();

        db.collection("requests")
                .whereEqualTo("resident_id", residentId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutRecentRequests.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        layoutRecentRequests.addView(createSimpleTextView("Chưa có yêu cầu nào."));
                        return;
                    }

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String title = document.getString("title");
                        String status = document.getString("status");
                        String createdAt = document.getString("created_at");

                        Long categoryIdLong = document.getLong("category_id");
                        String categoryName = "Khác";

                        if (categoryIdLong != null) {
                            String foundCategory = requestCategoryMap.get(categoryIdLong.intValue());
                            if (foundCategory != null) {
                                categoryName = foundCategory;
                            }
                        }

                        if (title == null) title = "Không có tiêu đề";
                        if (status == null) status = "pending";
                        if (createdAt == null) createdAt = "";

                        View card = createCardView(
                                title,
                                "Loại: " + categoryName + "\nTrạng thái: " + status,
                                createdAt
                        );

                        layoutRecentRequests.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    layoutRecentRequests.removeAllViews();
                    layoutRecentRequests.addView(createSimpleTextView("Không tải được yêu cầu gần đây."));
                    showError("Không tải được danh sách yêu cầu");
                });
    }

    private TextView createCardView(String title, String content, String footer) {
        TextView textView = new TextView(this);

        String text = title + "\n\n" + content + "\n\n" + footer;

        textView.setText(text);
        textView.setTextSize(15);
        textView.setTextColor(0xFF222222);
        textView.setPadding(24, 20, 24, 20);
        textView.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, 0, 14);
        textView.setLayoutParams(params);

        return textView;
    }

    private TextView createSimpleTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(15);
        textView.setTextColor(0xFF777777);
        textView.setPadding(12, 12, 12, 12);
        return textView;
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}