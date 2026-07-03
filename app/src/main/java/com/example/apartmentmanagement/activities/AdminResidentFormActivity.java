package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagement.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminResidentFormActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_ADD = "add";
    public static final String MODE_EDIT = "edit";
    public static final String EXTRA_DOCUMENT_ID = "document_id";

    private static final String RELATION_OWNER = "owner";
    private static final String[] GENDER_OPTIONS = {"Nam", "Nữ", "Khác"};

    private FirebaseFirestore db;
    private TextView tvTitle, tvSubtitle;
    private EditText edtResidentId, edtFullName, edtPhone, edtEmail, edtDateOfBirth, edtIdentity, edtMembersCount;
    private Spinner spnGender, spnApartment;
    private ProgressBar progressBar;
    private Button btnCancel, btnSave;

    private final List<ApartmentOption> apartments = new ArrayList<>();
    private boolean isEdit;
    private String documentId;
    private String oldResidentId;
    private String oldApartmentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_resident_form);

        db = FirebaseFirestore.getInstance();
        isEdit = MODE_EDIT.equalsIgnoreCase(getIntent().getStringExtra(EXTRA_MODE));
        documentId = value(getIntent().getStringExtra(EXTRA_DOCUMENT_ID));
        oldResidentId = value(getIntent().getStringExtra("resident_id"));
        oldApartmentId = value(getIntent().getStringExtra("apartment_id"));

        bindViews();
        setupHeader();
        setupStaticSpinners();
        fillEditingDataBeforeApartmentLoaded();
        loadApartments();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvResidentFormTitle);
        tvSubtitle = findViewById(R.id.tvResidentFormSubtitle);
        edtResidentId = findViewById(R.id.edtResidentId);
        edtFullName = findViewById(R.id.edtResidentFullName);
        edtPhone = findViewById(R.id.edtResidentPhone);
        edtEmail = findViewById(R.id.edtResidentEmail);
        edtDateOfBirth = findViewById(R.id.edtResidentDateOfBirth);
        edtIdentity = findViewById(R.id.edtResidentIdentity);
        edtMembersCount = findViewById(R.id.edtResidentMembersCount);
        spnGender = findViewById(R.id.spnResidentGender);
        spnApartment = findViewById(R.id.spnResidentApartment);
        progressBar = findViewById(R.id.progressResidentForm);
        btnCancel = findViewById(R.id.btnCancelResidentForm);
        btnSave = findViewById(R.id.btnSaveResidentForm);

        ImageButton btnBack = findViewById(R.id.btnBackResidentForm);
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveResident());
    }

    private void setupHeader() {
        tvTitle.setText(isEdit ? "Sửa chủ hộ" : "Thêm chủ hộ");
        tvSubtitle.setText(isEdit ? "Cập nhật thông tin chủ hộ và căn hộ liên kết" : "Tạo hồ sơ chủ hộ và gán vào căn hộ");
        btnSave.setText(isEdit ? "Lưu thay đổi" : "Thêm chủ hộ");
    }

    private void setupStaticSpinners() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GENDER_OPTIONS);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnGender.setAdapter(genderAdapter);
    }

    private void fillEditingDataBeforeApartmentLoaded() {
        if (!isEdit) return;

        edtResidentId.setText(oldResidentId);
        edtFullName.setText(value(getIntent().getStringExtra("full_name")));
        edtPhone.setText(value(getIntent().getStringExtra("phone_number")));
        edtEmail.setText(value(getIntent().getStringExtra("email")));
        edtDateOfBirth.setText(value(getIntent().getStringExtra("date_of_birth")));
        edtIdentity.setText(value(getIntent().getStringExtra("identity_number")));
        edtMembersCount.setText(value(getIntent().getStringExtra("members_count")));

        setSpinnerSelection(spnGender, GENDER_OPTIONS, value(getIntent().getStringExtra("gender")));
    }

    private void loadApartments() {
        setLoading(true);
        db.collection("apartments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    apartments.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        apartments.add(new ApartmentOption(
                                doc.getId(),
                                firstNonEmpty(string(doc, "id"), doc.getId()),
                                string(doc, "apartment_code"),
                                string(doc, "building"),
                                string(doc, "floor")
                        ));
                    }

                    if (apartments.isEmpty()) {
                        setLoading(false);
                        Toast.makeText(this, "Chưa có căn hộ để gán chủ hộ", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    setupApartmentSpinner();
                    if (isEdit) {
                        selectApartment(oldApartmentId, value(getIntent().getStringExtra("apartment_number")));
                        setLoading(false);
                    } else {
                        generateNextResidentId();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi tải căn hộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupApartmentSpinner() {
        List<String> labels = new ArrayList<>();
        for (ApartmentOption apartment : apartments) {
            labels.add(apartment.displayName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnApartment.setAdapter(adapter);
    }

    private void generateNextResidentId() {
        db.collection("residents")
                .get()
                .addOnSuccessListener(snapshot -> {
                    long nextId = 1;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String currentId = firstNonEmpty(string(doc, "resident_id"), string(doc, "id"), doc.getId());
                        nextId = Math.max(nextId, parseLong(currentId, 0) + 1);
                    }
                    edtResidentId.setText(String.valueOf(nextId));
                    edtMembersCount.setText("1");
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    edtResidentId.setText("1");
                    edtMembersCount.setText("1");
                    setLoading(false);
                });
    }

    private void saveResident() {
        String residentId = value(edtResidentId.getText().toString());
        String fullName = value(edtFullName.getText().toString());
        String phone = value(edtPhone.getText().toString());
        String email = value(edtEmail.getText().toString());
        String dateOfBirth = value(edtDateOfBirth.getText().toString());
        String identity = value(edtIdentity.getText().toString());
        String membersCount = value(edtMembersCount.getText().toString());
        String gender = String.valueOf(spnGender.getSelectedItem());
        String relationshipValue = RELATION_OWNER;

        if (residentId.isEmpty()) {
            toast("Vui lòng nhập Resident ID");
            return;
        }

        if (fullName.isEmpty()) {
            toast("Vui lòng nhập họ tên chủ hộ");
            return;
        }

        if (membersCount.isEmpty()) {
            membersCount = "1";
        }

        if (apartments.isEmpty()) {
            toast("Chưa có căn hộ để gán chủ hộ");
            return;
        }

        ApartmentOption selectedApartment = apartments.get(Math.max(0, spnApartment.getSelectedItemPosition()));
        if (isEdit) {
            updateResident(residentId, fullName, phone, email, dateOfBirth, identity, membersCount, gender, relationshipValue, selectedApartment);
        } else {
            createResident(residentId, fullName, phone, email, dateOfBirth, identity, membersCount, gender, relationshipValue, selectedApartment);
        }
    }

    private void createResident(String residentId, String fullName, String phone, String email, String dateOfBirth,
                                String identity, String membersCount, String gender, String relationship, ApartmentOption apartment) {
        setLoading(true);
        db.collection("residents").document(residentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        setLoading(false);
                        toast("Resident ID đã tồn tại. Vui lòng nhập ID khác.");
                        return;
                    }

                    Map<String, Object> data = buildResidentData(residentId, fullName, phone, email, dateOfBirth,
                            identity, membersCount, gender, relationship, apartment, true);

                    db.collection("residents").document(residentId)
                            .set(data)
                            .addOnSuccessListener(unused -> {
                                syncUserDocument(residentId, fullName, phone, email, true);
                                syncPrimaryHouseholdMember(residentId, fullName, dateOfBirth, identity, gender, relationship, apartment, true);
                                refreshApartmentStatus(apartment.id);
                                setLoading(false);
                                toast("Đã thêm chủ hộ");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                toast("Lỗi thêm chủ hộ: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi kiểm tra Resident ID: " + e.getMessage());
                });
    }

    private void updateResident(String residentId, String fullName, String phone, String email, String dateOfBirth,
                                String identity, String membersCount, String gender, String relationship, ApartmentOption apartment) {
        setLoading(true);

        String currentDocumentId = firstNonEmpty(documentId, oldResidentId, residentId);
        boolean changedResidentId = !residentId.equals(currentDocumentId);
        Map<String, Object> data = buildResidentData(residentId, fullName, phone, email, dateOfBirth,
                identity, membersCount, gender, relationship, apartment, false);

        if (changedResidentId) {
            db.collection("residents").document(residentId)
                    .get()
                    .addOnSuccessListener(targetDoc -> {
                        if (targetDoc.exists()) {
                            setLoading(false);
                            toast("Resident ID mới đã tồn tại. Vui lòng nhập ID khác.");
                            return;
                        }
                        moveResidentDocument(currentDocumentId, residentId, data, fullName, phone, email, dateOfBirth,
                                identity, gender, relationship, apartment);
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        toast("Lỗi kiểm tra Resident ID: " + e.getMessage());
                    });
        } else {
            db.collection("residents").document(currentDocumentId)
                    .update(data)
                    .addOnSuccessListener(unused -> afterResidentSaved(residentId, fullName, phone, email, dateOfBirth,
                            identity, gender, relationship, apartment))
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        toast("Lỗi sửa chủ hộ: " + e.getMessage());
                    });
        }
    }

    private void moveResidentDocument(String oldDocumentId, String newDocumentId, Map<String, Object> data,
                                      String fullName, String phone, String email, String dateOfBirth,
                                      String identity, String gender, String relationship, ApartmentOption apartment) {
        db.collection("residents").document(newDocumentId)
                .set(data)
                .addOnSuccessListener(unused -> db.collection("residents").document(oldDocumentId)
                        .delete()
                        .addOnSuccessListener(deleteUnused -> {
                            deleteHouseholdMembers(firstNonEmpty(oldResidentId, oldDocumentId));
                            afterResidentSaved(newDocumentId, fullName, phone, email, dateOfBirth, identity, gender, relationship, apartment);
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            toast("Đã tạo Resident ID mới nhưng lỗi xóa bản cũ: " + e.getMessage());
                        }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Lỗi cập nhật Resident ID mới: " + e.getMessage());
                });
    }

    private void afterResidentSaved(String residentId, String fullName, String phone, String email, String dateOfBirth,
                                    String identity, String gender, String relationship, ApartmentOption apartment) {
        syncUserDocument(residentId, fullName, phone, email, true);
        syncPrimaryHouseholdMember(residentId, fullName, dateOfBirth, identity, gender, relationship, apartment, true);
        refreshApartmentStatus(oldApartmentId);
        refreshApartmentStatus(apartment.id);
        setLoading(false);
        toast("Đã cập nhật chủ hộ");
        finish();
    }

    private Map<String, Object> buildResidentData(String residentId, String fullName, String phone, String email, String dateOfBirth,
                                                  String identity, String membersCount, String gender, String relationship,
                                                  ApartmentOption apartment, boolean includeCreatedAt) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", residentId);
        data.put("resident_id", residentIdValue(residentId));
        data.put("full_name", fullName);
        data.put("phone_number", phone);
        data.put("email", email);
        data.put("date_of_birth", dateOfBirth);
        data.put("identity_number", identity);
        data.put("members_count", membersCount);
        data.put("gender", gender);
        data.put("relationship_to_apartment", relationship);
        data.put("apartment_id", apartmentIdValue(apartment.id));
        data.put("apartment_number", apartment.code);
        data.put("avatar_url", "");
        data.put("updated_at", now());
        if (includeCreatedAt) {
            data.put("created_at", now());
        }
        return data;
    }

    private void syncUserDocument(String residentId, String fullName, String phone, String email, boolean active) {
        if (email == null || email.trim().isEmpty()) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", residentId);
        userData.put("resident_id", residentIdValue(residentId));
        userData.put("full_name", fullName);
        userData.put("phone_number", phone);
        userData.put("email", email.trim());
        userData.put("role", "resident");
        userData.put("is_active", active ? 1 : 0);
        userData.put("updated_at", now());

        db.collection("users")
                .whereEqualTo("email", email.trim())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        userData.put("created_at", now());
                        db.collection("users").document(residentId).set(userData);
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.update(doc.getReference(), userData);
                    }
                    batch.commit();
                });
    }

    private void syncPrimaryHouseholdMember(String residentId, String fullName, String dateOfBirth, String identity,
                                            String gender, String relationship, ApartmentOption apartment, boolean active) {
        String memberDocumentId = "owner_" + residentId;
        String relationshipLabel = displayRelationship(relationship);

        Map<String, Object> data = new HashMap<>();
        data.put("id", memberDocumentId);
        data.put("resident_id", residentId);
        data.put("apartment_id", apartmentIdValue(apartment.id));
        data.put("full_name", fullName);
        data.put("date_of_birth", dateOfBirth);
        data.put("identity_number", identity);
        data.put("gender", gender);
        data.put("relationship", relationshipLabel);
        data.put("is_active", active);
        data.put("updated_at", now());

        db.collection("household_members")
                .whereEqualTo("resident_id", residentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    DocumentSnapshot primaryDoc = null;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String docId = doc.getId();
                        String currentRelationship = string(doc, "relationship");
                        if (memberDocumentId.equals(docId) || "Chủ hộ".equalsIgnoreCase(currentRelationship)) {
                            primaryDoc = doc;
                            break;
                        }
                    }

                    if (primaryDoc == null) {
                        data.put("created_at", now());
                        db.collection("household_members").document(memberDocumentId).set(data);
                    } else {
                        primaryDoc.getReference().update(data);
                    }
                });
    }

    private void deleteHouseholdMembers(String residentId) {
        db.collection("household_members")
                .whereEqualTo("resident_id", residentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit();
                });
    }

    private void refreshApartmentStatus(String apartmentId) {
        if (apartmentId == null || apartmentId.trim().isEmpty()) return;

        Object apartmentValue = apartmentIdValue(apartmentId);
        db.collection("residents")
                .whereEqualTo("apartment_id", apartmentValue)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String status = snapshot.isEmpty() ? "empty" : "occupied";
                    updateApartmentStatusDocument(apartmentId, apartmentValue, status);
                });
    }

    private void updateApartmentStatusDocument(String apartmentId, Object apartmentValue, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("updated_at", now());

        db.collection("apartments")
                .whereEqualTo("id", apartmentValue)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            batch.update(doc.getReference(), data);
                        }
                        batch.commit();
                    } else {
                        db.collection("apartments").document(apartmentId).update(data);
                    }
                });
    }

    private void setSpinnerSelection(Spinner spinner, String[] options, String currentValue) {
        String normalizedCurrent = value(currentValue).toLowerCase(Locale.ROOT);
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(currentValue) || options[i].toLowerCase(Locale.ROOT).equals(normalizedCurrent)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void selectApartment(String apartmentId, String apartmentNumber) {
        for (int i = 0; i < apartments.size(); i++) {
            ApartmentOption apartment = apartments.get(i);
            if (apartment.matches(apartmentId, apartmentNumber)) {
                spnApartment.setSelection(i);
                return;
            }
        }
    }

    private Object apartmentIdValue(String apartmentId) {
        long number = parseLong(apartmentId, Long.MIN_VALUE);
        if (number != Long.MIN_VALUE) return number;
        return apartmentId;
    }

    private Object residentIdValue(String residentId) {
        long number = parseLong(residentId, Long.MIN_VALUE);
        if (number != Long.MIN_VALUE) return number;
        return residentId;
    }

    private String displayRelationship(String relationship) {
        if (RELATION_OWNER.equalsIgnoreCase(relationship) || "Chủ hộ".equalsIgnoreCase(relationship)) return "Chủ hộ";
        return "Chủ hộ";
    }

    private String string(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value);
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(firstNonEmpty(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String firstNonEmpty(String... values) {
        for (String item : values) {
            if (item != null && !item.trim().isEmpty()) return item.trim();
        }
        return "";
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static class ApartmentOption {
        final String docId;
        final String id;
        final String code;
        final String building;
        final String floor;

        ApartmentOption(String docId, String id, String code, String building, String floor) {
            this.docId = docId == null ? "" : docId.trim();
            this.id = id == null ? "" : id.trim();
            this.code = code == null || code.trim().isEmpty() ? "Căn hộ " + this.id : code.trim();
            this.building = building == null ? "" : building.trim();
            this.floor = floor == null ? "" : floor.trim();
        }

        String displayName() {
            StringBuilder builder = new StringBuilder(code);
            if (!building.isEmpty()) builder.append(" - ").append(building);
            if (!floor.isEmpty()) builder.append(" - Tầng ").append(floor);
            return builder.toString();
        }

        boolean matches(String apartmentId, String apartmentNumber) {
            String normalizedId = apartmentId == null ? "" : apartmentId.trim();
            String normalizedNumber = apartmentNumber == null ? "" : apartmentNumber.trim();
            return id.equalsIgnoreCase(normalizedId)
                    || docId.equalsIgnoreCase(normalizedId)
                    || code.equalsIgnoreCase(normalizedNumber);
        }
    }
}
