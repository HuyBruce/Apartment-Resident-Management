package com.example.apartmentmanagement.activities;

import android.content.Intent;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.AdminRecordAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseAdminListActivity extends AppCompatActivity {

    protected FirebaseFirestore db;
    protected AdminRecordAdapter adapter;
    protected final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    protected ProgressBar progressBar;
    protected LinearLayout emptyLayout;
    protected TextView titleView, subtitleView, emptyText;
    protected EditText searchEditText;
    protected ChipGroup chipGroup;
    protected ExtendedFloatingActionButton fab;
    protected final List<AdminRecordAdapter.AdminRecord> allRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());

        db = FirebaseFirestore.getInstance();
        bindBaseViews();
        onAfterBaseViewsBound();
        setupHeader();
        setupBackButton();
        setupFilters();
        setupSearch();
        loadData();
    }

    protected abstract int getLayoutResId();
    protected abstract String getCollectionName();
    protected abstract String getTitleText();
    protected abstract String getSubtitleText();
    protected abstract AdminRecordAdapter.AdminRecord mapRecord(DocumentSnapshot doc);

    protected String getFilterField() {
        return null;
    }

    protected String[] getFilters() {
        return new String[]{"Tất cả"};
    }

    protected String getFabText() {
        return "Làm mới";
    }

    protected void onAfterBaseViewsBound() {
    }

    protected void onFabClick() {
        loadData();
    }

    protected void onRecordAction(AdminRecordAdapter.AdminRecord record, String action) {
    }

    protected boolean shouldDisplayDocument(DocumentSnapshot doc) {
        return true;
    }

    private void bindBaseViews() {
        titleView = findViewById(R.id.tvAdminListTitle);
        subtitleView = findViewById(R.id.tvAdminListSubtitle);
        progressBar = findViewById(R.id.progressAdmin);
        emptyLayout = findViewById(R.id.layoutAdminEmpty);
        emptyText = findViewById(R.id.tvAdminEmptyText);
        searchEditText = findViewById(R.id.edtAdminSearch);
        chipGroup = findViewById(R.id.chipGroupAdmin);
        fab = findViewById(R.id.fabAdminAction);

        RecyclerView recyclerView = findViewById(R.id.recyclerAdmin);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminRecordAdapter(this::onRecordAction);
        recyclerView.setAdapter(adapter);
    }

    private void setupHeader() {
        if (titleView != null) titleView.setText(getTitleText());
        if (subtitleView != null) subtitleView.setText(getSubtitleText());
        if (fab != null) {
            fab.setText(getFabText());
            fab.setOnClickListener(v -> onFabClick());
        }
    }

    private void setupBackButton() {
        View backButton = findViewById(R.id.btnAdminBack);
        if (backButton == null) return;

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    protected void setupFilters() {
        if (chipGroup == null) return;

        chipGroup.removeAllViews();
        String[] filters = getFilters();
        for (int i = 0; i < filters.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(filters[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            chipGroup.addView(chip);
        }
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> loadData());
    }

    private void setupSearch() {
        if (searchEditText == null) return;
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    protected void loadData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);

        db.collection(getCollectionName())
                .get()
                .addOnSuccessListener(snapshot -> {
                    allRecords.clear();
                    String selectedFilter = getSelectedFilter();
                    String filterField = getFilterField();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (filterField != null && !"Tất cả".equals(selectedFilter)) {
                            String value = string(doc, filterField);
                            if (!selectedFilter.equalsIgnoreCase(value)) {
                                continue;
                            }
                        }

                        if (!shouldDisplayDocument(doc)) {
                            continue;
                        }

                        allRecords.add(mapRecord(doc));
                    }

                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    applySearchFilter();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    protected String getSelectedFilter() {
        if (chipGroup == null) return "Tất cả";
        int id = chipGroup.getCheckedChipId();
        if (id == View.NO_ID) {
            return "Tất cả";
        }
        Chip chip = chipGroup.findViewById(id);
        return chip == null ? "Tất cả" : chip.getText().toString();
    }

    protected void applySearchFilter() {
        String keyword = searchEditText == null ? "" : searchEditText.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<AdminRecordAdapter.AdminRecord> visibleRecords = new ArrayList<>();

        for (AdminRecordAdapter.AdminRecord record : allRecords) {
            if (keyword.isEmpty() || matches(record, keyword)) {
                visibleRecords.add(record);
            }
        }

        adapter.submitList(visibleRecords);
        if (emptyLayout != null) emptyLayout.setVisibility(visibleRecords.isEmpty() ? View.VISIBLE : View.GONE);
        if (emptyText != null) emptyText.setText(visibleRecords.isEmpty() ? "Không tìm thấy dữ liệu phù hợp" : "");
        if (subtitleView != null) subtitleView.setText(getSubtitleText() + " • " + visibleRecords.size() + " bản ghi");
    }

    protected boolean matches(AdminRecordAdapter.AdminRecord record, String keyword) {
        String text = (safe(record.title) + " " + safe(record.subtitle) + " " + safe(record.body) + " " + safe(record.status))
                .toLowerCase(Locale.ROOT);
        return text.contains(keyword);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    protected void updateField(String collection, String documentId, Map<String, Object> data) {
        db.collection(collection).document(documentId).update(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    protected void showNoticeDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(24, 0, 24, 0);
        EditText title = new EditText(this);
        title.setHint("Tiêu đề");
        EditText content = new EditText(this);
        content.setHint("Nội dung");
        content.setMinLines(3);
        form.addView(title);
        form.addView(content);

        new AlertDialog.Builder(this)
                .setTitle("Tạo thông báo")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi", (dialog, which) -> createNotice(title.getText().toString(), content.getText().toString()))
                .show();
    }

    private void createNotice(String title, String content) {
        if (title.trim().isEmpty() || content.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("title", title.trim());
        data.put("content", content.trim());
        data.put("type", "general");
        data.put("created_by", 2);
        data.put("created_at", now());
        data.put("updated_at", now());
        db.collection("notices").add(data)
                .addOnSuccessListener(ref -> loadData())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tạo thông báo", Toast.LENGTH_SHORT).show());
    }

    protected String string(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value);
    }

    protected double number(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    protected String money(double amount) {
        return currencyFormat.format(amount) + " đ";
    }

    protected String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }
}
