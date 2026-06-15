package com.example.apartmentmanagement.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserHelper {

    public interface OnIdsReady {
        void onReady(String userId, String residentId);
    }

    public interface OnError {
        void onError(String msg);
    }

    public static void getIds(OnIdsReady onReady, OnError onError) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            onError.onError("Bạn chưa đăng nhập.");
            return;
        }

        String email = currentUser.getEmail();
        if (email == null || email.trim().isEmpty()) {
            onError.onError("Tài khoản không có email.");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        onError.onError("Không tìm thấy tài khoản.");
                        return;
                    }
                    String userId     = query.getDocuments().get(0).getId();
                    Object resIdObj   = query.getDocuments().get(0).get("resident_id");
                    if (resIdObj == null) {
                        onError.onError("Tài khoản chưa liên kết hồ sơ cư dân.");
                        return;
                    }
                    String residentId = String.valueOf(resIdObj);
                    onReady.onReady(userId, residentId);
                })
                .addOnFailureListener(e -> onError.onError("Lỗi kết nối."));
    }
}
