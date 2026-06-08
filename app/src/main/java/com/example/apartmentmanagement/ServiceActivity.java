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

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ServiceActivity extends AppCompatActivity {

    private TextView btnBack;
    private Spinner spinnerService;
    private EditText edtTitle;
    private EditText edtDescription;
    private Button btnSubmit;

    private FirebaseFirestore db;

    private int userId;
    private int residentId;
    private int apartmentId;

    private final String[] services = {
            "Sửa chữa điện nước",
            "Đăng ký giữ xe",
            "Vệ sinh căn hộ",
            "Bảo trì thiết bị",
            "Khác"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getIntExtra("user_id", 1);
        residentId = getIntent().getIntExtra("resident_id", 1);
        apartmentId = getIntent().getIntExtra("apartment_id", 1);

        btnBack = findViewById(R.id.btnBack);
        spinnerService = findViewById(R.id.spinnerService);
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        btnSubmit = findViewById(R.id.btnSubmit);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                services
        );
        spinnerService.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitService());
    }

    private void submitService() {
        String serviceName = spinnerService.getSelectedItem().toString();
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            edtTitle.setError("Vui lòng nhập tiêu đề");
            return;
        }

        if (TextUtils.isEmpty(description)) {
            edtDescription.setError("Vui lòng nhập mô tả");
            return;
        }

        long id = System.currentTimeMillis();
        String now = getCurrentTime();

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("resident_id", residentId);
        data.put("apartment_id", apartmentId);
        data.put("category_id", 6);
        data.put("title", title);
        data.put("description", "Dịch vụ: " + serviceName + "\n" + description);
        data.put("status", "pending");
        data.put("priority", "normal");
        data.put("image_url", "");
        data.put("assigned_to", null);
        data.put("created_at", now);
        data.put("updated_at", now);
        data.put("completed_at", null);
        data.put("source", "service");
        data.put("service_name", serviceName);

        db.collection("requests")
                .document(String.valueOf(id))
                .set(data)
                .addOnSuccessListener(unused -> {
                    createStatusHistory(id);
                    Toast.makeText(this, "Đăng ký dịch vụ thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi đăng ký dịch vụ: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void createStatusHistory(long requestId) {
        long id = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("request_id", requestId);
        data.put("old_status", null);
        data.put("new_status", "pending");
        data.put("note", "Cư dân vừa đăng ký dịch vụ.");
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