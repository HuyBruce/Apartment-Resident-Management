package com.example.apartmentmanagement.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.adapters.ComplaintAdapter;
import com.example.apartmentmanagement.models.Complaint;
import com.example.apartmentmanagement.utils.UserHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ComplaintsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ComplaintAdapter adapter;
    private List<Complaint> complaints = new ArrayList<>();

    private TextView tvSubtitle, tvNoComplaints;
    private ExtendedFloatingActionButton fabAdd;
    private View rootView;

    private FirebaseFirestore db;
    private String residentId = "";

    // Image picker
    private ImageView ivPreview;
    private String selectedImageBase64 = "";
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_complaints, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        rootView = view;

        tvSubtitle      = view.findViewById(R.id.tvComplaintSubtitle);
        tvNoComplaints  = view.findViewById(R.id.tvNoComplaints);
        fabAdd          = view.findViewById(R.id.fabAddComplaint);
        recyclerView    = view.findViewById(R.id.recyclerComplaints);

        adapter = new ComplaintAdapter(requireContext(), complaints);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        setupImagePicker();

        fabAdd.setOnClickListener(v -> showAddComplaintSheet());

        UserHelper.getIds((userId, resId) -> {
            residentId = resId;
            loadComplaints();
        }, msg -> Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show());
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    requireContext().getContentResolver(), uri);

                            // Resize để giảm size base64
                            Bitmap resized = resizeBitmap(bitmap, 800);

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                            byte[] bytes = baos.toByteArray();

                            selectedImageBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);

                            if (ivPreview != null) {
                                ivPreview.setImageBitmap(resized);
                                ivPreview.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            Snackbar.make(rootView, "Không thể tải ảnh", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private Bitmap resizeBitmap(Bitmap original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();
        float ratio = (float) width / height;

        int newWidth, newHeight;
        if (width > height) {
            newWidth = maxSize;
            newHeight = (int) (maxSize / ratio);
        } else {
            newHeight = maxSize;
            newWidth = (int) (maxSize * ratio);
        }
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    private void loadComplaints() {
        db.collection("complaints")
                .whereEqualTo("resident_id", residentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    complaints.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Complaint c = doc.toObject(Complaint.class);
                        if (c.getId() == null) c.setId(doc.getId());
                        complaints.add(c);
                    }
                    adapter.notifyDataSetChanged();
                    tvSubtitle.setText(complaints.size() + " phản ánh");
                    tvNoComplaints.setVisibility(complaints.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rootView, "Không thể tải phản ánh", Snackbar.LENGTH_SHORT).show());
    }

    private void showAddComplaintSheet() {
        selectedImageBase64 = "";

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext(),
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View sv = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_add_complaint, null);
        sheet.setContentView(sv);

        ChipGroup chipGroup    = sv.findViewById(R.id.chipGroupCategory);
        EditText etTitle       = sv.findViewById(R.id.etComplaintTitle);
        EditText etDesc        = sv.findViewById(R.id.etComplaintDesc);
        MaterialButton btnPick = sv.findViewById(R.id.btnPickImage);
        ivPreview              = sv.findViewById(R.id.ivPreviewImage);
        MaterialButton btnCancel = sv.findViewById(R.id.btnCancelComplaint);
        MaterialButton btnSubmit = sv.findViewById(R.id.btnSubmitComplaint);

        btnPick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        btnCancel.setOnClickListener(v -> sheet.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc  = etDesc.getText().toString().trim();

            if (title.isEmpty()) { etTitle.setError("Nhập tiêu đề"); return; }
            if (desc.isEmpty())  { etDesc.setError("Nhập mô tả");   return; }

            String category = getCategoryFromChip(chipGroup);

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Đang gửi...");

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(Calendar.getInstance().getTime());

            Map<String, Object> data = new HashMap<>();
            data.put("resident_id",    residentId);
            data.put("title",          title);
            data.put("description",    desc);
            data.put("category",       category);
            data.put("image_base64",   selectedImageBase64);
            data.put("status",         "pending");
            data.put("priority",       "normal");
            data.put("created_at",     now);
            data.put("updated_at",     now);
            data.put("resolved_at",    "");
            data.put("admin_response", "");

            db.collection("complaints").add(data)
                    .addOnSuccessListener(ref -> {
                        sheet.dismiss();
                        loadComplaints();
                        Snackbar.make(rootView, "✓ Đã gửi phản ánh tới Ban quản lý",
                                Snackbar.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Gửi");
                        Snackbar.make(rootView, "Lỗi khi gửi. Thử lại.", Snackbar.LENGTH_SHORT).show();
                    });
        });

        sheet.show();
    }

    private String getCategoryFromChip(ChipGroup chipGroup) {
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == R.id.chipSuaChua) return "sửa_chữa";
        if (checkedId == R.id.chipAnNinh)  return "an_ninh";
        if (checkedId == R.id.chipKhac)    return "khác";
        return "vi_phạm_nội_quy";
    }
}