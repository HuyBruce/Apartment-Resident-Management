package com.example.apartmentmanagement.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserHelper {

    public interface OnIdsReady {
        void onReady(String userId, String residentId);
    }

    public interface OnError {
        void onError(String msg);
    }

    public static void getIds(OnIdsReady onReady, OnError onError) {
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

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
                    String residentId = String.valueOf(resIdObj);
                    onReady.onReady(userId, residentId);
                })
                .addOnFailureListener(e -> onError.onError("Lỗi kết nối."));
    }
}