package com.example.apartmentmanagement;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateRequestActivity extends AppCompatActivity {

    private TextView btnBack;
    private Spinner spinnerCategory;
    private Spinner spinnerPriority;
    private EditText edtTitle;
    private EditText edtDescription;
    private Button btnSubmit;

    private FirebaseFirestore db;

    private int userId;
    private int residentId;
    private int apartmentId;

    private final ArrayList<String> categoryNames = new ArrayList<>();
    private final ArrayList<Integer> categoryIds = new ArrayList<>();

    private final String[] priorities = {"normal", "high", "urgent"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_request);

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getIntExtra("user_id", 1);
        residentId = getIntent().getIntExtra("resident_id", 1);
        apartmentId = getIntent().getIntExtra("apartment_id", 1);

        initViews();
        setupPrioritySpinner();
        loadCategories();

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitRequest());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupPrioritySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                priorities
        );
        spinnerPriority.setAdapter(adapter);
    }

    private void loadCategories() {
        db.collection("request_categories")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    categoryNames.clear();
                    categoryIds.clear();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Long idLong = doc.getLong("id");
                        String name = doc.getString("name");

                        if (idLong != null && name != null) {
                            categoryIds.add(idLong.intValue());
                            categoryNames.add(name);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            categoryNames
                    );
                    spinnerCategory.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không tải được loại yêu cầu", Toast.LENGTH_SHORT).show()
                );
    }

    private void submitRequest() {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();

        if (categoryIds.isEmpty()) {
            Toast.makeText(this, "Chưa có loại yêu cầu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(title)) {
            edtTitle.setError("Vui lòng nhập tiêu đề");
            return;
        }

        if (TextUtils.isEmpty(description)) {
            edtDescription.setError("Vui lòng nhập mô tả");
            return;
        }

        int selectedIndex = spinnerCategory.getSelectedItemPosition();
        int categoryId = categoryIds.get(selectedIndex);

        String priority = spinnerPriority.getSelectedItem().toString();

        long id = System.currentTimeMillis();
        String now = getCurrentTime();

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("resident_id", residentId);
        data.put("apartment_id", apartmentId);
        data.put("category_id", categoryId);
        data.put("title", title);
        data.put("description", description);
        data.put("status", "pending");
        data.put("priority", priority);
        data.put("image_url", "");
        data.put("assigned_to", null);
        data.put("created_at", now);
        data.put("updated_at", now);
        data.put("completed_at", null);
        data.put("source", "complaint");

        db.collection("requests")
                .document(String.valueOf(id))
                .set(data)
                .addOnSuccessListener(unused -> {
                    createStatusHistory(id, null, "pending", "Cư dân vừa gửi yêu cầu.");
                    Toast.makeText(this, "Gửi yêu cầu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void createStatusHistory(long requestId, String oldStatus, String newStatus, String note) {
        long id = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("request_id", requestId);
        data.put("old_status", oldStatus);
        data.put("new_status", newStatus);
        data.put("note", note);
        data.put("changed_by", userId);
        data.put("changed_at", getCurrentTime());

        db.collection("request_status_history")
                .document(String.valueOf(id))
                .set(data);
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}