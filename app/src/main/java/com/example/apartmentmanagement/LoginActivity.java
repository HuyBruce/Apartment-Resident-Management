package com.example.apartmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.apartmentmanagement.activities.AdminHomeActivity;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();

        initViews();

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void initViews() {
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void handleLogin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }

        loginWithFirestore(email, password);
    }

    private void loginWithFirestore(String email, String password) {
        showLoading(true);

        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        showLoading(false);
                        Toast.makeText(
                                LoginActivity.this,
                                "Sai email hoặc mật khẩu",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);

                    Long isActiveLong = userDoc.getLong("is_active");
                    int isActive = isActiveLong != null ? isActiveLong.intValue() : 1;

                    if (isActive != 1) {
                        showLoading(false);
                        Toast.makeText(
                                LoginActivity.this,
                                "Tài khoản đã bị khóa",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    Long userIdLong = userDoc.getLong("id");

                    int userId = userIdLong != null ? userIdLong.intValue() : -1;

                    String fullName = userDoc.getString("full_name");
                    String role = userDoc.getString("role");

                    if (fullName == null) {
                        fullName = "";
                    }
                    if (!"admin".equals(role)) {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "Ứng dụng này chỉ dành cho quản trị viên", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    openDashboard(userId, fullName, role);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(
                            LoginActivity.this,
                            "Lỗi đăng nhập: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void openDashboard(
            int userId,
            String fullName,
            String role
    ) {
        Intent intent = new Intent(LoginActivity.this, AdminHomeActivity.class);

        intent.putExtra("user_id", userId);
        intent.putExtra("full_name", fullName);
        intent.putExtra("role", role);

        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            btnLogin.setText("Đang đăng nhập...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setText("Đăng nhập");
        }
    }
}