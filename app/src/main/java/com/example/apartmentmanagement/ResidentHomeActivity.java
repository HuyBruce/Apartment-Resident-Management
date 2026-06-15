package com.example.apartmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ResidentHomeActivity extends AppCompatActivity {

    private TextView tvAdminName;
    private TextView tvAdminSub;
    private TextView tvTotalResidents;
    private TextView tvTotalApartments;
    private TextView tvMonthlyRevenue;
    private TextView tvPendingRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        showAdminInfo();
        setupClickEvents();
    }

    private void initViews() {
        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminSub = findViewById(R.id.tvAdminSub);
        tvTotalResidents = findViewById(R.id.tvTotalResidents);
        tvTotalApartments = findViewById(R.id.tvTotalApartments);
        tvMonthlyRevenue = findViewById(R.id.tvMonthlyRevenue);
        tvPendingRequests = findViewById(R.id.tvPendingRequests);
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
        TextView tvSeeAllAnn = findViewById(R.id.tvSeeAllAnn);
        Button btnCreateAnnouncement = findViewById(R.id.btnCreateAnnouncement);
        LinearLayout tabHome = findViewById(R.id.tabHome);
        LinearLayout tabResidents = findViewById(R.id.tabResidents);
        LinearLayout tabFee = findViewById(R.id.tabFee);
        LinearLayout tabRequests = findViewById(R.id.tabRequests);
        LinearLayout tabSettings = findViewById(R.id.tabSettings);

        btnSearch.setOnClickListener(v -> showComingSoon("Tìm kiếm"));
        btnNotification.setOnClickListener(v -> openActivity(NotificationsActivity.class));
        tvSeeAllRepair.setOnClickListener(v -> openActivity(RequestStatusActivity.class));
        tvSeeAllGuest.setOnClickListener(v -> showComingSoon("Quản lý khách"));
        tvSeeAllAnn.setOnClickListener(v -> openActivity(NotificationsActivity.class));
        btnCreateAnnouncement.setOnClickListener(v -> showComingSoon("Tạo thông báo"));

        tabHome.setOnClickListener(v -> { });
        tabResidents.setOnClickListener(v -> showComingSoon("Quản lý cư dân"));
        tabFee.setOnClickListener(v -> showComingSoon("Quản lý phí"));
        tabRequests.setOnClickListener(v -> openActivity(RequestStatusActivity.class));
        tabSettings.setOnClickListener(v -> showComingSoon("Cài đặt"));

        setupApprovalButton(R.id.btnApproveGuest1, "Đã duyệt khách thứ nhất");
        setupApprovalButton(R.id.btnRejectGuest1, "Đã từ chối khách thứ nhất");
        setupApprovalButton(R.id.btnApproveGuest2, "Đã duyệt khách thứ hai");
        setupApprovalButton(R.id.btnRejectGuest2, "Đã từ chối khách thứ hai");
    }

    private void setupApprovalButton(int buttonId, String message) {
        ImageButton button = findViewById(buttonId);
        button.setOnClickListener(v -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
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