package com.example.an_droids;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.*;

public class ProfileActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, dobEditText;
    private Button saveButton, logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser firebaseUser;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private Date selectedDOB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        dobEditText = findViewById(R.id.dobEditText);
        saveButton = findViewById(R.id.saveButton);
        logoutButton = findViewById(R.id.logoutButton);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        emailEditText.setEnabled(false);

        if (firebaseUser != null) {
            loadUserProfile();
        }

        saveButton.setOnClickListener(v -> updateUserProfile());
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });

        dobEditText.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        new DatePickerDialog(ProfileActivity.this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDOB = calendar.getTime();
                    dobEditText.setText(dateFormatter.format(selectedDOB));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void loadUserProfile() {
        firestore.collection("Users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Users user = documentSnapshot.toObject(Users.class);
                    if (user != null) {
                        usernameEditText.setText(user.getUsername());
                        emailEditText.setText(user.getEmail());
                        selectedDOB = user.getDob();
                        if (selectedDOB != null) {
                            dobEditText.setText(dateFormatter.format(selectedDOB));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void updateUserProfile() {
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String newUsername = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString();

        Users updatedUser = new Users(newUsername, email, selectedDOB);

        firestore.collection("Users").document(firebaseUser.getUid())
                .set(updatedUser, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show());
    }
}