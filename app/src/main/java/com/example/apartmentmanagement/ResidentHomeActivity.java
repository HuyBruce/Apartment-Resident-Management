package com.example.apartmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagement.activities.ProfileActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ResidentHomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvApartmentCode;
    private TextView tvResidentName;
    private TextView tvApartmentAddress;
    private TextView tvTotalUnpaid;
    private TextView tvViewAllFees;
    private TextView tvViewAllRequests;

    private TextView navHome;
    private TextView navUtilities;
    private TextView navProperty;
    private TextView navAccount;

    private LinearLayout layoutUnpaidFees;
    private LinearLayout layoutRequests;

    private LinearLayout btnInvoice;
    private LinearLayout btnRequest;
    private LinearLayout btnMember;
    private LinearLayout btnChat;

    private Button btnPay;

    private FirebaseFirestore db;

    private int userId = 1;
    private int residentId = 1;
    private int apartmentId = 1;

    private final Map<Integer, String> feeCategoryMap = new HashMap<>();
    private final Map<Integer, String> requestCategoryMap = new HashMap<>();

    private double totalUnpaidAmount = 0;
    private float density;
    private NumberFormat currencyFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        density = getResources().getDisplayMetrics().density;
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        initViews();
        getIntentData();
        setupClickEvents();
        requestNotificationPermission();
        loadHomeData();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvApartmentCode = findViewById(R.id.tvApartmentCode);
        tvResidentName = findViewById(R.id.tvResidentName);
        tvApartmentAddress = findViewById(R.id.tvApartmentAddress);
        tvTotalUnpaid = findViewById(R.id.tvTotalUnpaid);
        tvViewAllFees = findViewById(R.id.tvViewAllFees);
        tvViewAllRequests = findViewById(R.id.tvViewAllRequests);

        layoutUnpaidFees = findViewById(R.id.layoutUnpaidFees);
        layoutRequests = findViewById(R.id.layoutRequests);

        btnInvoice = findViewById(R.id.btnInvoice);
        btnRequest = findViewById(R.id.btnRequest);
        btnMember = findViewById(R.id.btnMember);
        btnChat = findViewById(R.id.btnChat);

        btnPay = findViewById(R.id.btnPay);

        navHome = findViewById(R.id.navHome);
        navUtilities = findViewById(R.id.navUtilities);
        navProperty = findViewById(R.id.navProperty);
        navAccount = findViewById(R.id.navAccount);
    }

    private void getIntentData() {
        userId = getIntent().getIntExtra("user_id", 1);
        residentId = getIntent().getIntExtra("resident_id", 1);
        apartmentId = getIntent().getIntExtra("apartment_id", 1);

        String fullName = getIntent().getStringExtra("full_name");

        if (fullName != null && !fullName.trim().isEmpty()) {
            tvWelcome.setText("Mừng bạn về nhà, " + fullName);
            tvResidentName.setText(fullName.toUpperCase());
        } else {
            tvWelcome.setText("Mừng bạn về nhà");
            tvResidentName.setText("CƯ DÂN");
        }
    }

    private void setupClickEvents() {
        btnInvoice.setOnClickListener(v -> {
            Intent intent = new Intent(this, RequestStatusActivity.class);
            putUserData(intent);
            startActivity(intent);
        });

        btnRequest.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateRequestActivity.class);
            putUserData(intent);
            startActivity(intent);
        });

        btnMember.setOnClickListener(v ->
                Toast.makeText(this, "Chức năng thành viên sẽ làm sau", Toast.LENGTH_SHORT).show()
        );

        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            putUserData(intent);
            startActivity(intent);
        });

        tvViewAllFees.setOnClickListener(v ->
                Toast.makeText(this, "Chức năng quản lý phí sẽ làm ở phần 7", Toast.LENGTH_SHORT).show()
        );

        tvViewAllRequests.setOnClickListener(v -> {
            Intent intent = new Intent(this, RequestStatusActivity.class);
            putUserData(intent);
            startActivity(intent);
        });

        navUtilities.setOnClickListener(v -> {
            Intent intent = new Intent(this, ServiceActivity.class);
            putUserData(intent);
            startActivity(intent);
        });

        navAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            putUserData(intent);
            startActivity(intent);
        });

        btnPay.setOnClickListener(v -> {
            if (totalUnpaidAmount <= 0) {
                Toast.makeText(this, "Không có hóa đơn cần thanh toán", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Chức năng thanh toán sẽ làm ở phần 8", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void putUserData(Intent intent) {
        intent.putExtra("user_id", userId);
        intent.putExtra("resident_id", residentId);
        intent.putExtra("apartment_id", apartmentId);

        String fullName = getIntent().getStringExtra("full_name");
        String role = getIntent().getStringExtra("role");

        intent.putExtra("full_name", fullName);
        intent.putExtra("role", role);
    }

    private void loadHomeData() {
        loadResidentInfo();
        loadApartmentInfo();
        loadFeeCategoriesThenFees();
        loadRequestCategoriesThenRequests();
    }

    private void loadResidentInfo() {
        db.collection("residents")
                .document(String.valueOf(residentId))
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        return;
                    }

                    String fullName = document.getString("full_name");

                    Long apartmentIdLong = document.getLong("apartment_id");
                    if (apartmentIdLong != null) {
                        apartmentId = apartmentIdLong.intValue();
                        loadApartmentInfo();
                    }

                    if (fullName != null && !fullName.trim().isEmpty()) {
                        tvWelcome.setText("Mừng bạn về nhà, " + fullName);
                        tvResidentName.setText(fullName.toUpperCase());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không tải được thông tin cư dân", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadApartmentInfo() {
        db.collection("apartments")
                .document(String.valueOf(apartmentId))
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        return;
                    }

                    String apartmentCode = document.getString("apartment_code");
                    String building = document.getString("building");

                    Long floorLong = document.getLong("floor");

                    if (apartmentCode == null) apartmentCode = "Căn hộ";
                    if (building == null) building = "Chung cư";

                    String title = apartmentCode + "@" + building;
                    tvApartmentCode.setText(title.toUpperCase());

                    if (floorLong != null) {
                        tvApartmentAddress.setText("Tầng " + floorLong + " - " + building);
                    } else {
                        tvApartmentAddress.setText(building);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không tải được căn hộ", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadFeeCategoriesThenFees() {
        feeCategoryMap.clear();

        db.collection("fee_categories")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        Long idLong = document.getLong("id");
                        String name = document.getString("name");

                        if (idLong != null && name != null) {
                            feeCategoryMap.put(idLong.intValue(), name);
                        }
                    }

                    loadUnpaidFees();
                })
                .addOnFailureListener(e -> loadUnpaidFees());
    }

    private void loadUnpaidFees() {
        layoutUnpaidFees.removeAllViews();
        totalUnpaidAmount = 0;
        tvTotalUnpaid.setText(formatCurrency(0));

        db.collection("fees")
                .whereEqualTo("apartment_id", apartmentId)
                .whereEqualTo("status", "unpaid")
                .limit(5)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    layoutUnpaidFees.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        layoutUnpaidFees.addView(createEmptyCard("Không có hóa đơn chưa thanh toán"));
                        tvTotalUnpaid.setText(formatCurrency(0));
                        return;
                    }

                    for (DocumentSnapshot document : querySnapshot) {
                        String title = document.getString("title");
                        String dueDate = document.getString("due_date");

                        Long monthLong = document.getLong("month");
                        Long yearLong = document.getLong("year");
                        Long categoryIdLong = document.getLong("category_id");

                        double amount = getDoubleValue(document, "amount");
                        totalUnpaidAmount += amount;

                        String categoryName = "Phí";
                        if (categoryIdLong != null) {
                            String found = feeCategoryMap.get(categoryIdLong.intValue());
                            if (found != null) categoryName = found;
                        }

                        if (title == null) title = categoryName;
                        if (dueDate == null) dueDate = "";

                        String period = "";
                        if (monthLong != null && yearLong != null) {
                            period = "Kỳ " + monthLong + "/" + yearLong;
                        }

                        View feeCard = createFeeCard(title, period, dueDate, amount);
                        layoutUnpaidFees.addView(feeCard);
                    }

                    tvTotalUnpaid.setText(formatCurrency(totalUnpaidAmount));
                })
                .addOnFailureListener(e -> {
                    layoutUnpaidFees.removeAllViews();
                    layoutUnpaidFees.addView(createEmptyCard("Không tải được hóa đơn"));
                    Toast.makeText(this, "Lỗi tải hóa đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRequestCategoriesThenRequests() {
        requestCategoryMap.clear();

        db.collection("request_categories")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        Long idLong = document.getLong("id");
                        String name = document.getString("name");

                        if (idLong != null && name != null) {
                            requestCategoryMap.put(idLong.intValue(), name);
                        }
                    }

                    loadRecentRequests();
                })
                .addOnFailureListener(e -> loadRecentRequests());
    }

    private void loadRecentRequests() {
        layoutRequests.removeAllViews();

        db.collection("requests")
                .whereEqualTo("resident_id", residentId)
                .limit(5)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    layoutRequests.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        layoutRequests.addView(createEmptyCard("Chưa có yêu cầu nào"));
                        return;
                    }

                    for (DocumentSnapshot document : querySnapshot) {
                        String title = document.getString("title");
                        String description = document.getString("description");
                        String status = document.getString("status");
                        String createdAt = document.getString("created_at");

                        Long categoryIdLong = document.getLong("category_id");

                        String categoryName = "Khác";
                        if (categoryIdLong != null) {
                            String found = requestCategoryMap.get(categoryIdLong.intValue());
                            if (found != null) categoryName = found;
                        }

                        if (title == null) title = "Yêu cầu";
                        if (description == null) description = "";
                        if (status == null) status = "pending";
                        if (createdAt == null) createdAt = "";

                        View requestCard = createRequestCard(
                                title,
                                description,
                                status,
                                categoryName,
                                createdAt
                        );

                        layoutRequests.addView(requestCard);
                    }
                })
                .addOnFailureListener(e -> {
                    layoutRequests.removeAllViews();
                    layoutRequests.addView(createEmptyCard("Không tải được yêu cầu"));
                    Toast.makeText(this, "Lỗi tải yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private View createFeeCard(String title, String period, String dueDate, double amount) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFEAF3FF);
        card.setPadding(18, 16, 18, 16);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dpToPx(190), dpToPx(110));
        cardParams.setMargins(0, 0, dpToPx(12), 0);
        card.setLayoutParams(cardParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF333333);
        tvTitle.setTextSize(14);
        tvTitle.setMaxLines(2);

        TextView tvPeriod = new TextView(this);
        tvPeriod.setText(period);
        tvPeriod.setTextColor(0xFF777777);
        tvPeriod.setTextSize(13);
        tvPeriod.setPadding(0, dpToPx(8), 0, 0);

        TextView tvAmount = new TextView(this);
        tvAmount.setText(formatCurrency(amount));
        tvAmount.setTextColor(0xFF26A69A);
        tvAmount.setTextSize(17);
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAmount.setPadding(0, dpToPx(8), 0, 0);

        card.addView(tvTitle);
        card.addView(tvPeriod);
        card.addView(tvAmount);

        return card;
    }

    private View createRequestCard(
            String title,
            String description,
            String status,
            String categoryName,
            String createdAt
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFDE7);
        card.setPadding(18, 16, 18, 16);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dpToPx(250), dpToPx(170));
        cardParams.setMargins(0, 0, dpToPx(12), 0);
        card.setLayoutParams(cardParams);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(convertStatus(status));
        tvStatus.setTextColor(0xFFFFFFFF);
        tvStatus.setTextSize(12);
        tvStatus.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        tvStatus.setBackgroundColor(getStatusColor(status));

        TextView tvCategory = new TextView(this);
        tvCategory.setText(categoryName);
        tvCategory.setTextColor(0xFFFFFFFF);
        tvCategory.setTextSize(12);
        tvCategory.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        tvCategory.setBackgroundColor(0xFF26A69A);

        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        categoryParams.setMargins(dpToPx(8), 0, 0, 0);
        tvCategory.setLayoutParams(categoryParams);

        topRow.addView(tvStatus);
        topRow.addView(tvCategory);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF333333);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, dpToPx(12), 0, 0);
        tvTitle.setMaxLines(2);

        TextView tvDescription = new TextView(this);
        tvDescription.setText(description);
        tvDescription.setTextColor(0xFF444444);
        tvDescription.setTextSize(13);
        tvDescription.setPadding(0, dpToPx(8), 0, 0);
        tvDescription.setMaxLines(2);

        TextView tvCreatedAt = new TextView(this);
        tvCreatedAt.setText(createdAt);
        tvCreatedAt.setTextColor(0xFF777777);
        tvCreatedAt.setTextSize(12);
        tvCreatedAt.setPadding(0, dpToPx(10), 0, 0);

        card.addView(topRow);
        card.addView(tvTitle);
        card.addView(tvDescription);
        card.addView(tvCreatedAt);

        return card;
    }

    private View createEmptyCard(String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextColor(0xFF777777);
        textView.setTextSize(14);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(240), dpToPx(90));
        params.setMargins(0, 0, dpToPx(12), 0);
        textView.setLayoutParams(params);

        return textView;
    }

    private String convertStatus(String status) {
        if ("pending".equals(status)) {
            return "Đang xử lý";
        } else if ("processing".equals(status)) {
            return "Đang xử lý";
        } else if ("done".equals(status)) {
            return "Hoàn tất";
        }
        return status;
    }

    private int getStatusColor(String status) {
        if ("pending".equals(status)) {
            return 0xFFFFC107;
        } else if ("processing".equals(status)) {
            return 0xFF2196F3;
        } else if ("done".equals(status)) {
            return 0xFF4CAF50;
        }
        return 0xFF999999;
    }

    private double getDoubleValue(DocumentSnapshot document, String fieldName) {
        Double doubleValue = document.getDouble(fieldName);
        if (doubleValue != null) {
            return doubleValue;
        }

        Long longValue = document.getLong(fieldName);
        if (longValue != null) {
            return longValue.doubleValue();
        }

        return 0;
    }

    private String formatCurrency(double amount) {
        if (currencyFormatter == null) {
            currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        }
        return currencyFormatter.format(amount);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * density);
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100
                );
            }
        }
    }
}