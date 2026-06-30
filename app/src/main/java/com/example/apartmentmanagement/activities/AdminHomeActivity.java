package com.example.apartmentmanagement.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagement.R;

import com.example.apartmentmanagement.activities.AdminFeesActivity;
import com.example.apartmentmanagement.activities.AdminNoticesActivity;
import com.example.apartmentmanagement.activities.AdminPaymentsActivity;
import com.example.apartmentmanagement.activities.AdminVisitorsActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminHomeActivity extends AppCompatActivity {

    private TextView tvAdminName;
    private TextView tvAdminSub;
    private TextView tvTotalResidents;
    private TextView tvTotalApartments;
    private TextView tvMonthlyRevenue;
    private TextView tvPendingRequests;
    private TextView tvPaidCount;
    private TextView tvUnpaidCount;
    private TextView tvCollectionRate;
    private ProgressBar progressFeeCollection;
    private LinearLayout containerHomeRequests;
    private LinearLayout containerHomeFees;
    private LinearLayout containerHomeVisitors;

    private FirebaseFirestore db;
    private final NumberFormat numberFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final Map<String, String> residentNames = new HashMap<>();
    private final Map<String, String> residentApartments = new HashMap<>();
    private final Map<String, String> apartments = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        db = FirebaseFirestore.getInstance();

        initViews();
        showAdminInfo();
        setupClickEvents();
        loadDashboardData();
    }

    private void initViews() {
        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminSub = findViewById(R.id.tvAdminSub);
        tvTotalResidents = findViewById(R.id.tvTotalResidents);
        tvTotalApartments = findViewById(R.id.tvTotalApartments);
        tvMonthlyRevenue = findViewById(R.id.tvMonthlyRevenue);
        tvPendingRequests = findViewById(R.id.tvPendingRequests);
        tvPaidCount = findViewById(R.id.tvPaidCount);
        tvUnpaidCount = findViewById(R.id.tvUnpaidCount);
        tvCollectionRate = findViewById(R.id.tvCollectionRate);
        progressFeeCollection = findViewById(R.id.progressFeeCollection);
        containerHomeRequests = findViewById(R.id.containerHomeRequests);
        containerHomeFees = findViewById(R.id.containerHomeFees);
        containerHomeVisitors = findViewById(R.id.containerHomeVisitors);
    }

    private void showAdminInfo() {
        String fullName = getIntent().getStringExtra("full_name");
        if (fullName != null && !fullName.trim().isEmpty()) {
            tvAdminName.setText(fullName);
        }

        String role = getIntent().getStringExtra("role");
        if (role != null && !role.trim().isEmpty()) {
            tvAdminSub.setText("Vai trò: " + role);
        }
    }

    private void setupClickEvents() {
        ImageButton btnSearch = findViewById(R.id.btnSearch);
        ImageButton btnNotification = findViewById(R.id.btnNotification);
        TextView tvSeeAllRepair = findViewById(R.id.tvSeeAllRepair);
        TextView tvSeeAllGuest = findViewById(R.id.tvSeeAllGuest);
        TextView tvFeeDetail = findViewById(R.id.tvFeeDetail);
        LinearLayout btnAdminMenu = findViewById(R.id.btnAdminMenu);
        View drawerOverlay = findViewById(R.id.drawerOverlay);
        LinearLayout drawerMenu = findViewById(R.id.drawerMenu);
        LinearLayout menuApartments = findViewById(R.id.menuApartments);
        LinearLayout menuResidents = findViewById(R.id.menuResidents);
        LinearLayout menuFees = findViewById(R.id.menuFees);
        LinearLayout menuParking = findViewById(R.id.menuParking);
        LinearLayout menuEvents = findViewById(R.id.menuEvents);
        LinearLayout menuFeedback = findViewById(R.id.menuFeedback);
        LinearLayout menuVisitors = findViewById(R.id.menuVisitors);
        LinearLayout tabHome = findViewById(R.id.tabHome);
        LinearLayout tabRequests = findViewById(R.id.tabRequests);
        LinearLayout tabSettings = findViewById(R.id.tabSettings);

        btnSearch.setOnClickListener(v -> showComingSoon("Tìm kiếm"));
        btnNotification.setOnClickListener(v -> openActivity(AdminNoticesActivity.class));
        tvSeeAllRepair.setOnClickListener(v -> openActivity(AdminFeedbackActivity.class));
        tvSeeAllGuest.setOnClickListener(v -> openActivity(AdminVisitorsActivity.class));
        tvFeeDetail.setOnClickListener(v -> openActivity(AdminPaymentsActivity.class));

        btnAdminMenu.setOnClickListener(v -> drawerOverlay.setVisibility(View.VISIBLE));
        drawerOverlay.setOnClickListener(v -> drawerOverlay.setVisibility(View.GONE));
        drawerMenu.setOnClickListener(v -> { });

        menuApartments.setOnClickListener(v -> openMenuActivity(drawerOverlay, AdminApartmentsActivity.class));
        menuResidents.setOnClickListener(v -> openMenuActivity(drawerOverlay, AdminResidentsActivity.class));
        menuFees.setOnClickListener(v -> openMenuActivity(drawerOverlay, AdminFeesActivity.class));
        menuParking.setOnClickListener(v -> openMenuActivity(drawerOverlay, AdminParkingActivity.class));
        menuEvents.setOnClickListener(v -> openMenuActivity(drawerOverlay, AdminEventsActivity.class));
        menuFeedback.setOnClickListener(v -> openMenuActivity(drawerOverlay, AdminFeedbackActivity.class));
        menuVisitors.setOnClickListener(v -> openMenuActivity(drawerOverlay, AdminVisitorsActivity.class));

        tabHome.setOnClickListener(v -> { });
        tabRequests.setOnClickListener(v -> openActivity(AdminNoticesActivity.class));
        tabSettings.setOnClickListener(v -> openActivity(ProfileActivity.class));
    }


    private void openMenuActivity(View drawerOverlay, Class<?> activityClass) {
        drawerOverlay.setVisibility(View.GONE);
        openActivity(activityClass);
    }

    @Override
    public void onBackPressed() {
        View drawerOverlay = findViewById(R.id.drawerOverlay);
        if (drawerOverlay != null && drawerOverlay.getVisibility() == View.VISIBLE) {
            drawerOverlay.setVisibility(View.GONE);
            return;
        }
        super.onBackPressed();
    }

    private void loadDashboardData() {
        loadCollectionCount("residents", tvTotalResidents);
        loadCollectionCount("apartments", tvTotalApartments);
        loadLookupData();
        loadPendingRequestCount();
        loadRevenueStats();
        loadHomeRequests();
        loadHomeVisitors();
    }

    private void loadCollectionCount(String collectionName, TextView target) {
        db.collection(collectionName)
                .get()
                .addOnSuccessListener(snapshot -> target.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(e -> target.setText("0"));
    }

    private void loadLookupData() {
        db.collection("residents")
                .get()
                .addOnSuccessListener(snapshot -> {
                    residentNames.clear();
                    residentApartments.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(safeString(doc.get("resident_id")), safeString(doc.get("id")), doc.getId());
                        residentNames.put(id, safeString(doc.get("full_name")));
                        residentApartments.put(id, safeString(doc.get("apartment_number")));
                    }
                    loadHomeRequests();
                    loadRevenueStats();
                    loadHomeVisitors();
                });

        db.collection("apartments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    apartments.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String id = firstNonEmpty(safeString(doc.get("id")), doc.getId());
                        apartments.put(id, firstNonEmpty(safeString(doc.get("apartment_code")), id));
                    }
                    loadHomeRequests();
                    loadRevenueStats();
                    loadHomeVisitors();
                });
    }

    private void loadPendingRequestCount() {
        db.collection("requests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> tvPendingRequests.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(e -> tvPendingRequests.setText("0"));
    }

    private void loadRevenueStats() {
        db.collection("fees")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Calendar now = Calendar.getInstance();
                    int currentMonth = now.get(Calendar.MONTH) + 1;
                    int currentYear = now.get(Calendar.YEAR);

                    double paidThisMonth = 0;
                    double paidAllTime = 0;
                    int paidCount = 0;
                    int totalCount = snapshot.size();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String status = safeString(doc.get("status")).toLowerCase(Locale.ROOT);
                        boolean paid = status.equals("paid")
                                || status.equals("done")
                                || status.equals("đã tt")
                                || status.equals("đã thanh toán");

                        if (!paid) {
                            continue;
                        }

                        double amount = getNumber(doc, "amount");
                        paidAllTime += amount;
                        paidCount++;

                        int month = (int) getNumber(doc, "month");
                        int year = (int) getNumber(doc, "year");
                        if (month == currentMonth && year == currentYear) {
                            paidThisMonth += amount;
                        }
                    }

                    double revenue = paidThisMonth > 0 ? paidThisMonth : paidAllTime;
                    tvMonthlyRevenue.setText(formatCompactCurrency(revenue));

                    if (progressFeeCollection != null) {
                        int progress = totalCount == 0 ? 0 : Math.round((paidCount * 100f) / totalCount);
                        progressFeeCollection.setProgress(progress);
                        tvPaidCount.setText(paidCount + "\nĐã thanh toán");
                        tvUnpaidCount.setText((totalCount - paidCount) + "\nChưa thanh toán");
                        tvCollectionRate.setText(progress + "%\nTỉ lệ thu");
                    }

                    renderFees(snapshot.getDocuments());
                })
                .addOnFailureListener(e -> {
                    tvMonthlyRevenue.setText("0đ");
                    if (progressFeeCollection != null) {
                        progressFeeCollection.setProgress(0);
                    }
                });
    }

    private void loadHomeRequests() {
        db.collection("requests")
                .get()
                .addOnSuccessListener(snapshot -> {
                    containerHomeRequests.removeAllViews();
                    int count = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String status = safeString(doc.get("status"));
                        if ("done".equalsIgnoreCase(status)) {
                            continue;
                        }
                        String residentId = safeString(doc.get("resident_id"));
                        String title = displayResident(residentId, safeString(doc.get("apartment_id")));
                        String subtitle = firstNonEmpty(safeString(doc.get("title")), safeString(doc.get("description")));
                        String badge = "high".equalsIgnoreCase(safeString(doc.get("priority"))) ? "Khẩn" : status;
                        containerHomeRequests.addView(infoRow(initials(residentNames.get(residentId)), title, subtitle, badge));
                        if (++count >= 3) break;
                    }
                    if (count == 0) containerHomeRequests.addView(emptyRow("Không có yêu cầu đang xử lý"));
                })
                .addOnFailureListener(e -> containerHomeRequests.addView(emptyRow("Không tải được yêu cầu")));
    }

    private void renderFees(Iterable<DocumentSnapshot> docs) {
        containerHomeFees.removeAllViews();
        int count = 0;
        for (DocumentSnapshot doc : docs) {
            String residentId = safeString(doc.get("resident_id"));
            String title = displayResident(residentId, safeString(doc.get("apartment_id")));
            String subtitle = numberFormat.format(getNumber(doc, "amount")) + " đ";
            String badge = isPaid(safeString(doc.get("status"))) ? "Đã TT" : "Chưa TT";
            containerHomeFees.addView(infoRow(initials(residentNames.get(residentId)), title, subtitle, badge));
            if (++count >= 3) break;
        }
        if (count == 0) containerHomeFees.addView(emptyRow("Chưa có khoản phí"));
    }

    private void loadHomeVisitors() {
        db.collection("visitors")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> {
                    containerHomeVisitors.removeAllViews();
                    int count = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String residentId = safeString(doc.get("resident_id"));
                        String subtitle = "Đăng ký bởi " + displayApartment(residentId, safeString(doc.get("apartment_id")))
                                + " · " + safeString(doc.get("visit_date")) + " " + safeString(doc.get("visit_time"));
                        String visitorName = safeString(doc.get("visitor_name"));
                        LinearLayout row = infoRow(initials(visitorName), visitorName, subtitle, "Chờ duyệt");
                        row.setOnClickListener(v -> openActivity(AdminVisitorsActivity.class));
                        containerHomeVisitors.addView(row);
                        if (++count >= 2) break;
                    }
                    if (count == 0) containerHomeVisitors.addView(emptyRow("Không có khách chờ duyệt"));
                })
                .addOnFailureListener(e -> containerHomeVisitors.addView(emptyRow("Không tải được khách")));
    }

    private double getNumber(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).replace(",", ""));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean isPaid(String status) {
        String value = status.toLowerCase(Locale.ROOT);
        return value.equals("paid") || value.equals("done") || value.equals("đã tt") || value.equals("đã thanh toán");
    }

    private String displayResident(String residentId, String apartmentId) {
        String name = firstNonEmpty(residentNames.get(residentId), "Chưa rõ cư dân");
        return name + " · " + displayApartment(residentId, apartmentId);
    }

    private String displayApartment(String residentId, String apartmentId) {
        return firstNonEmpty(residentApartments.get(residentId), apartments.get(apartmentId), apartmentId, "Căn hộ");
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private LinearLayout emptyRow(String message) {
        return infoRow("–", message, "Firebase chưa có bản ghi phù hợp", "");
    }

    private LinearLayout infoRow(String icon, String title, String subtitle, String badge) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(13), dp(13), dp(13));
        row.setBackgroundResource(R.drawable.shape_card);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowParams);

        TextView avatar = text(firstNonEmpty(icon, "?"), "#1A3C6E", 13, true);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(Color.parseColor("#EAF2FF"));
        avatar.setBackground(avatarBg);
        row.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(11), 0, dp(8), 0);
        body.addView(text(title, "#1A1A2E", 13, true));
        body.addView(text(subtitle, "#999999", 11, false));
        row.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (badge != null && !badge.trim().isEmpty()) {
            TextView badgeView = text(badge, "#1A3C6E", 11, true);
            badgeView.setGravity(Gravity.CENTER);
            badgeView.setPadding(dp(10), dp(5), dp(10), dp(5));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(Color.parseColor("#EAF2FF"));
            badgeBg.setCornerRadius(dp(18));
            badgeView.setBackground(badgeBg);
            row.addView(badgeView);
        }
        return row;
    }

    private TextView text(String value, String color, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(Color.parseColor(color));
        view.setTextSize(sp);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatCompactCurrency(double amount) {
        if (amount >= 1_000_000_000) {
            return numberFormat.format(amount / 1_000_000_000d) + " tỷ";
        }
        if (amount >= 1_000_000) {
            return numberFormat.format(amount / 1_000_000d) + "tr";
        }
        return numberFormat.format(amount) + "đ";
    }

    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.putExtras(getIntent());
        startActivity(intent);
    }

    private void showComingSoon(String feature) {
        Toast.makeText(this, feature + " đang được phát triển", Toast.LENGTH_SHORT).show();
    }
}
