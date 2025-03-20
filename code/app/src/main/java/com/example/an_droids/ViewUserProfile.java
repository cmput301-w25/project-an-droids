package com.example.an_droids;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ViewUserProfile extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private TextView usernameTextView, emailTextView, dobTextView, locationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_user_profile);

        firestore = FirebaseFirestore.getInstance();
        usernameTextView = findViewById(R.id.usernameTextView);
        //emailTextView = findViewById(R.id.emailTextView);
        //dobTextView = findViewById(R.id.dobTextView);
        locationTextView = findViewById(R.id.locationTextView);

        // Get the username passed from the SearchActivity
        String username = getIntent().getStringExtra("username");

        // Check if username is null or empty
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Error: No user found", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no username is passed
        } else {
            Toast.makeText(this, "Loading profile of: " + username, Toast.LENGTH_SHORT).show();
            loadUserProfile(username);
        }
    }

    private void loadUserProfile(String username) {
        firestore.collection("Users").whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                        // Check if document contains expected fields
                        String email = document.contains("email") ? document.getString("email") : "Not available";
                        String dob = document.contains("dob") ? document.getString("dob") : "Not available";
                        String location = document.contains("location") ? document.getString("location") : "Not available";

                        usernameTextView.setText(username);
                        //emailTextView.setText(email);
                        //dobTextView.setText(dob);
                        locationTextView.setText(location);
                    } else {
                        Toast.makeText(ViewUserProfile.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewUserProfile.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

}
