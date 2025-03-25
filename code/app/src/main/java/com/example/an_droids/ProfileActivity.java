package com.example.an_droids;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, dobEditText, locationEditText;
    private Button saveButton, logoutButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize UI elements
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        dobEditText = findViewById(R.id.dobEditText);
        locationEditText = findViewById(R.id.locationEditText);
        saveButton = findViewById(R.id.saveButton);
        logoutButton = findViewById(R.id.logoutButton);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        // Disable email editing
        emailEditText.setEnabled(false);

        if (firebaseUser != null) {
            loadUserProfile();
        }

        saveButton.setOnClickListener(v -> updateUserProfile());
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // End the MainActivity so the user can't go back
        });

    }

    private void loadUserProfile() {
        firestore.collection("Users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        usernameEditText.setText(documentSnapshot.getString("username"));

                        // Ensure email is displayed correctly
                        String email = documentSnapshot.contains("email") ?
                                documentSnapshot.getString("email") : firebaseUser.getEmail();
                        emailEditText.setText(email);

                        dobEditText.setText(documentSnapshot.getString("dob"));
                        locationEditText.setText(documentSnapshot.getString("location"));
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
        String newDOB = dobEditText.getText().toString().trim();
        String newLocation = locationEditText.getText().toString().trim();

        // Retrieve the current email to prevent overwriting it as null
        firestore.collection("Users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String email = documentSnapshot.contains("email") ?
                                documentSnapshot.getString("email") : firebaseUser.getEmail();

                        Map<String, Object> updatedData = new HashMap<>();
                        updatedData.put("username", newUsername);
                        updatedData.put("dob", newDOB);
                        updatedData.put("location", newLocation);
                        updatedData.put("email", email); // Preserve email

                        firestore.collection("Users").document(firebaseUser.getUid())
                                .set(updatedData)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ProfileActivity.this, "Failed to retrieve email", Toast.LENGTH_SHORT).show());
    }
}



