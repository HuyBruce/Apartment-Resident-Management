package com.example.apartmentmanagement.activities;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class AdminApartmentsActivity extends BaseAdminListActivity {

    private static final String ALL = "Tất cả";
    private static final String ALL_FLOORS = "Tất cả tầng";
    private static final String STATUS_OCCUPIED = "occupied";
    private static final String STATUS_EMPTY = "empty";

    private LinearLayout layoutApartmentFilterToggle;
    private LinearLayout layoutApartmentFilterPanel;
    private TextView tvApartmentFilterSummary;
    private TextView tvApartmentFilterArrow;
    private ChipGroup chipGroupBuildingFilter;
    private ChipGroup chipGroupFloorFilter;
    private ChipGroup chipGroupStatusFilter;

    private String selectedBuilding = ALL;
    private String selectedFloor = ALL_FLOORS;
    private String selectedStatus = ALL;
    private boolean suppressFilterEvents = false;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_admin_apartments;
    }

    @Override
    protected String getCollectionName() {
        return "apartments";
    }

    @Override
    protected String getTitleText() {
        return "Quản lý căn hộ";
    }

    @Override
    protected String getSubtitleText() {
        return "Tòa nhà, tầng, diện tích và trạng thái";
    }

    @Override
    protected void onAfterBaseViewsBound() {
        layoutApartmentFilterToggle = findViewById(R.id.layoutApartmentFilterToggle);
        layoutApartmentFilterPanel = findViewById(R.id.layoutApartmentFilterPanel);
        tvApartmentFilterSummary = findViewById(R.id.tvApartmentFilterSummary);
        tvApartmentFilterArrow = findViewById(R.id.tvApartmentFilterArrow);
        chipGroupBuildingFilter = findViewById(R.id.chipGroupBuildingFilter);
        chipGroupFloorFilter = findViewById(R.id.chipGroupFloorFilter);
        chipGroupStatusFilter = findViewById(R.id.chipGroupStatusFilter);

        if (layoutApartmentFilterToggle != null) {
            layoutApartmentFilterToggle.setOnClickListener(v -> toggleFilterPanel());
        }

        updateFilterSummary();
    }

    @Override
    protected void setupFilters() {
        // Trang căn hộ không dùng chip category cũ của BaseAdminListActivity.
        // Bộ lọc mới gồm 3 hàng: Tòa, Tầng, Trạng thái.
    }

    @Override
    protected void loadData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);

        db.collection(getCollectionName())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<QueryDocumentSnapshot> documents = new ArrayList<>();
                    Set<String> buildings = new TreeSet<>();
                    Set<String> floors = new TreeSet<>((left, right) -> compareFloor(left, right));

                    for (QueryDocumentSnapshot doc : snapshot) {
                        documents.add(doc);
                        String building = firstNonEmpty(string(doc, "building"));
                        String floor = firstNonEmpty(string(doc, "floor"));

                        if (!building.isEmpty()) buildings.add(building);
                        if (!floor.isEmpty()) floors.add(floor);
                    }

                    rebuildFilterChips(buildings, floors);

                    allRecords.clear();
                    for (QueryDocumentSnapshot doc : documents) {
                        if (matchesApartmentFilters(doc)) {
                            allRecords.add(mapRecord(doc));
                        }
                    }

                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    applySearchFilter();
                    updateFilterSummary();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải căn hộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc) {
        String apartmentId = firstNonEmpty(string(doc, "id"), doc.getId());
        String apartmentCode = string(doc, "apartment_code");
        String building = string(doc, "building");
        String floor = string(doc, "floor");
        String area = string(doc, "area");
        String status = string(doc, "status");

        AdminRecordAdapter.AdminRecord record = new AdminRecordAdapter.AdminRecord();
        record.documentId = doc.getId();
        record.icon = firstNonEmpty(apartmentCode, "A");
        record.title = firstNonEmpty(apartmentCode, "Căn hộ " + apartmentId) + " - " + firstNonEmpty(building, "Chưa rõ tòa");
        record.subtitle = "Tầng " + firstNonEmpty(floor, "-") + " • " + firstNonEmpty(area, "-") + " m²";
        record.body = "Mã căn hộ: " + apartmentId
                + "\nTòa nhà: " + firstNonEmpty(building, "-")
                + "\nNgày tạo: " + firstNonEmpty(string(doc, "created_at"), "-");
        record.status = displayStatus(status);
        record.positiveStatus = STATUS_OCCUPIED.equalsIgnoreCase(status);
        record.actions.add("Xem hộ");
        record.extras.put("apartment_doc_id", doc.getId());
        record.extras.put("apartment_id", apartmentId);
        record.extras.put("apartment_code", apartmentCode);
        record.extras.put("building", building);
        record.extras.put("floor", floor);
        record.extras.put("area", area);
        record.extras.put("status", status);
        return record;
    }

    @Override
    protected void onRecordAction(AdminRecordAdapter.AdminRecord record, String action) {
        if (!"Xem hộ".equals(action)) return;

        Intent intent = new Intent(this, AdminApartmentDetailActivity.class);
        intent.putExtra(AdminApartmentDetailActivity.EXTRA_APARTMENT_DOC_ID, record.extras.get("apartment_doc_id"));
        intent.putExtra(AdminApartmentDetailActivity.EXTRA_APARTMENT_ID, record.extras.get("apartment_id"));
        intent.putExtra(AdminApartmentDetailActivity.EXTRA_APARTMENT_CODE, record.extras.get("apartment_code"));
        intent.putExtra(AdminApartmentDetailActivity.EXTRA_BUILDING, record.extras.get("building"));
        intent.putExtra(AdminApartmentDetailActivity.EXTRA_FLOOR, record.extras.get("floor"));
        intent.putExtra(AdminApartmentDetailActivity.EXTRA_AREA, record.extras.get("area"));
        intent.putExtra(AdminApartmentDetailActivity.EXTRA_STATUS, record.extras.get("status"));
        startActivity(intent);
    }

    private void toggleFilterPanel() {
        if (layoutApartmentFilterPanel == null) return;

        boolean willShow = layoutApartmentFilterPanel.getVisibility() != View.VISIBLE;
        layoutApartmentFilterPanel.setVisibility(willShow ? View.VISIBLE : View.GONE);
        if (tvApartmentFilterArrow != null) {
            tvApartmentFilterArrow.setText(willShow ? "▲" : "▼");
        }
    }

    private void rebuildFilterChips(Set<String> buildings, Set<String> floors) {
        suppressFilterEvents = true;

        rebuildChipGroup(chipGroupBuildingFilter, buildBuildingOptions(buildings), selectedBuilding, value -> {
            selectedBuilding = value;
            loadData();
        });

        rebuildChipGroup(chipGroupFloorFilter, buildFloorOptions(floors), selectedFloor, value -> {
            selectedFloor = value;
            loadData();
        });

        rebuildChipGroup(chipGroupStatusFilter, buildStatusOptions(), selectedStatus, value -> {
            selectedStatus = value;
            loadData();
        });

        suppressFilterEvents = false;
    }

    private List<String> buildBuildingOptions(Set<String> buildings) {
        List<String> options = new ArrayList<>();
        options.add(ALL);
        options.addAll(buildings);
        if (!options.contains(selectedBuilding)) selectedBuilding = ALL;
        return options;
    }

    private List<String> buildFloorOptions(Set<String> floors) {
        List<String> options = new ArrayList<>();
        options.add(ALL_FLOORS);
        for (String floor : floors) {
            options.add(formatFloorLabel(floor));
        }
        if (!options.contains(selectedFloor)) selectedFloor = ALL_FLOORS;
        return options;
    }

    private List<String> buildStatusOptions() {
        List<String> options = new ArrayList<>();
        options.add(ALL);
        options.add("Đang ở");
        options.add("Trống");
        if (!options.contains(selectedStatus)) selectedStatus = ALL;
        return options;
    }

    private void rebuildChipGroup(ChipGroup chipGroup, List<String> options, String selectedValue, OnFilterSelected listener) {
        if (chipGroup == null) return;

        chipGroup.setOnCheckedStateChangeListener(null);
        chipGroup.removeAllViews();

        for (String option : options) {
            Chip chip = new Chip(this);
            chip.setText(option);
            chip.setCheckable(true);
            chip.setChecked(option.equals(selectedValue));
            chipGroup.addView(chip);
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressFilterEvents || checkedIds == null || checkedIds.isEmpty()) return;
            Chip checkedChip = group.findViewById(checkedIds.get(0));
            if (checkedChip == null) return;
            listener.onSelected(checkedChip.getText().toString());
        });
    }

    private boolean matchesApartmentFilters(DocumentSnapshot doc) {
        String building = firstNonEmpty(string(doc, "building"));
        String floorLabel = formatFloorLabel(firstNonEmpty(string(doc, "floor")));
        String statusLabel = displayShortStatus(firstNonEmpty(string(doc, "status")));

        boolean matchesBuilding = ALL.equals(selectedBuilding) || selectedBuilding.equalsIgnoreCase(building);
        boolean matchesFloor = ALL_FLOORS.equals(selectedFloor) || selectedFloor.equalsIgnoreCase(floorLabel);
        boolean matchesStatus = ALL.equals(selectedStatus) || selectedStatus.equalsIgnoreCase(statusLabel);

        return matchesBuilding && matchesFloor && matchesStatus;
    }

    private void updateFilterSummary() {
        if (tvApartmentFilterSummary == null) return;

        String buildingText = ALL.equals(selectedBuilding) ? "Tất cả tòa" : selectedBuilding;
        String floorText = ALL_FLOORS.equals(selectedFloor) ? "Tất cả tầng" : selectedFloor;
        String statusText = ALL.equals(selectedStatus) ? "Tất cả trạng thái" : selectedStatus;
        tvApartmentFilterSummary.setText(buildingText + " • " + floorText + " • " + statusText);
    }

    private String displayStatus(String status) {
        if (STATUS_OCCUPIED.equalsIgnoreCase(status)) return "Đang ở";
        if (STATUS_EMPTY.equalsIgnoreCase(status)) return "Đang trống";
        return firstNonEmpty(status, "-");
    }

    private String displayShortStatus(String status) {
        if (STATUS_OCCUPIED.equalsIgnoreCase(status)) return "Đang ở";
        if (STATUS_EMPTY.equalsIgnoreCase(status)) return "Trống";
        return firstNonEmpty(status, "-");
    }

    private String formatFloorLabel(String floor) {
        String value = firstNonEmpty(floor);
        if (value.isEmpty()) return "Tầng -";
        if (value.toLowerCase(Locale.ROOT).startsWith("tầng")) return value;
        return "Tầng " + value;
    }

    private int compareFloor(String left, String right) {
        try {
            int leftNumber = Integer.parseInt(left.trim());
            int rightNumber = Integer.parseInt(right.trim());
            return Integer.compare(leftNumber, rightNumber);
        } catch (Exception ignored) {
            return left.compareToIgnoreCase(right);
        }
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private interface OnFilterSelected {
        void onSelected(String value);
    }
}
