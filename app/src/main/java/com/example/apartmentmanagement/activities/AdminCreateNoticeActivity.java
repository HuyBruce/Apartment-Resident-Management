package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagement.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminCreateNoticeActivity extends AppCompatActivity {

    private static final String TYPE_FACILITY_LABEL = "Cơ sở vật chất";
    private static final String TYPE_FEE_LABEL = "Phí";

    private EditText edtTitle, edtContent;
    private Spinner spinnerType;
    private ProgressBar progressBar;
    private Button btnSubmit;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_create_notice);

        db = FirebaseFirestore.getInstance();
        bindViews();
        setupTypeSpinner();
        setupListeners();
    }

    private void bindViews() {
        edtTitle = findViewById(R.id.edtNoticeTitle);
        edtContent = findViewById(R.id.edtNoticeContent);
        spinnerType = findViewById(R.id.spinnerNoticeType);
        progressBar = findViewById(R.id.progressCreateNotice);
        btnSubmit = findViewById(R.id.btnSubmitNotice);
    }

    private void setupTypeSpinner() {
        String[] types = new String[]{TYPE_FACILITY_LABEL, TYPE_FEE_LABEL};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                types
        );
        spinnerType.setAdapter(adapter);
    }

    private void setupListeners() {
        ImageButton btnBack = findViewById(R.id.btnBackCreateNotice);
        Button btnCancel = findViewById(R.id.btnCancelNotice);

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> createNotice());
    }

    private void createNotice() {
        String title = edtTitle.getText().toString().trim();
        String content = edtContent.getText().toString().trim();
        String selectedType = spinnerType.getSelectedItem() == null
                ? TYPE_FACILITY_LABEL
                : spinnerType.getSelectedItem().toString();
        String type = toDatabaseType(selectedType);

        if (title.isEmpty()) {
            edtTitle.setError("Vui lòng nhập tiêu đề");
            edtTitle.requestFocus();
            return;
        }

        if (content.isEmpty()) {
            edtContent.setError("Vui lòng nhập nội dung");
            edtContent.requestFocus();
            return;
        }

        setLoading(true);

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("type", type);
        data.put("content", content);
        data.put("created_by", 2);
        data.put("created_at", now);
        data.put("updated_at", now);

        db.collection("notices")
                .add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Đã tạo thông báo", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi tạo thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String toDatabaseType(String label) {
        if (TYPE_FEE_LABEL.equalsIgnoreCase(label)) {
            return "fee";
        }
        // general và maintenance được gộp hiển thị thành "Cơ sở vật chất".
        // Khi tạo mới, lưu là maintenance để tương thích dữ liệu cũ.
        return "maintenance";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }
}
