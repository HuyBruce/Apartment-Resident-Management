package com.example.apartmentmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagement.MainActivity;
import com.example.apartmentmanagement.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword;
    private View rootView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Luôn bắt đầu từ màn hình đăng nhập.
        // Tránh trường hợp Firebase còn session cũ rồi tự nhảy thẳng vào Home Admin / Home cư dân.
        mAuth.signOut();

        setContentView(R.layout.activity_login);

        rootView = findViewById(android.R.id.content);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvForgotPassword.setOnClickListener(v -> {
            String email = value(etEmail.getText());
            if (email.isEmpty()) {
                tilEmail.setError("Nhập email để đặt lại mật khẩu");
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused ->
                            Snackbar.make(rootView, "✓ Đã gửi email đặt lại mật khẩu", Snackbar.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Snackbar.make(rootView, "Không tìm thấy tài khoản", Snackbar.LENGTH_SHORT).show());
        });
    }

    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = value(etEmail.getText());
        String password = value(etPassword.getText());

        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> loadUserRoleAndOpenScreen(email))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = e.getMessage() != null && e.getMessage().toLowerCase().contains("password")
                            ? "Sai mật khẩu. Vui lòng thử lại."
                            : "Không tìm thấy tài khoản với email này.";
                    Snackbar.make(rootView, msg, Snackbar.LENGTH_LONG).show();
                });
    }

    private void loadUserRoleAndOpenScreen(String email) {
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        setLoading(false);
                        mAuth.signOut();
                        Snackbar.make(rootView, "Không tìm thấy thông tin người dùng trong database users.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    DocumentSnapshot userDoc = query.getDocuments().get(0);

                    if (!isActive(userDoc)) {
                        setLoading(false);
                        mAuth.signOut();
                        Snackbar.make(rootView, "Tài khoản đã bị khóa.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    String role = value(userDoc.getString("role"));
                    if (role.isEmpty()) {
                        role = "resident";
                    }

                    String fullName = value(userDoc.getString("full_name"));
                    String userDocId = userDoc.getId();
                    int userId = toInt(firstNonEmpty(value(userDoc.get("id")), userDocId), -1);

                    if ("admin".equalsIgnoreCase(role)) {
                        openAdminHome(userId, userDocId, fullName, role);
                    } else {
                        openResidentHome(userId, userDocId, fullName, role);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    mAuth.signOut();
                    Snackbar.make(rootView, "Lỗi tải thông tin vai trò: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void openAdminHome(int userId, String userDocId, String fullName, String role) {
        Intent intent = new Intent(this, AdminHomeActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_doc_id", userDocId);
        intent.putExtra("full_name", fullName);
        intent.putExtra("role", role);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openResidentHome(int userId, String userDocId, String fullName, String role) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_doc_id", userDocId);
        intent.putExtra("full_name", fullName);
        intent.putExtra("role", role);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isActive(DocumentSnapshot doc) {
        Object raw = doc.get("is_active");
        if (raw == null) return true;

        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }

        if (raw instanceof Number) {
            return ((Number) raw).intValue() == 1;
        }

        String text = String.valueOf(raw).trim();
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "active".equalsIgnoreCase(text);
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return "";
    }

    private int toInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
