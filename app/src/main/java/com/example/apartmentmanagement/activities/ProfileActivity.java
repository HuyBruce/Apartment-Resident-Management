package com.example.apartmentmanagement.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagement.R;
import com.example.apartmentmanagement.models.Resident;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    EditText edtName;
    EditText edtPhone;
    EditText edtEmail;
    EditText edtDob;
    EditText edtGender;

    Button btnLoad;
    Button btnSave;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        edtName = findViewById(R.id.edtName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
        edtDob = findViewById(R.id.edtDob);
        edtGender = findViewById(R.id.edtGender);

        btnLoad = findViewById(R.id.btnLoad);
        btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();

        loadProfile();

        btnLoad.setOnClickListener(v -> loadProfile());

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {

        db.collection("residents")
                .document("1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        Resident resident =
                                documentSnapshot.toObject(Resident.class);

                        if (resident != null) {

                            edtName.setText(resident.getFull_name());
                            edtPhone.setText(resident.getPhone_number());
                            edtEmail.setText(resident.getEmail());
                            edtDob.setText(resident.getDate_of_birth());
                            edtGender.setText(resident.getGender());
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Load failed",
                                Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {

        Map<String, Object> data = new HashMap<>();

        data.put("full_name",
                edtName.getText().toString());

        data.put("phone_number",
                edtPhone.getText().toString());

        data.put("email",
                edtEmail.getText().toString());

        data.put("date_of_birth",
                edtDob.getText().toString());

        data.put("gender",
                edtGender.getText().toString());

        db.collection("residents")
                .document("1")
                .update(data)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this,
                                "Profile Updated",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Update Failed",
                                Toast.LENGTH_SHORT).show());
    }
}