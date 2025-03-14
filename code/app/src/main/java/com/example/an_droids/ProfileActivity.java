package com.example.an_droids;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, dobEditText, locationEditText;
    private Button saveButton, editButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Maintain original structure while adding new functionality
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        dobEditText = findViewById(R.id.dobEditText);
        locationEditText = findViewById(R.id.locationEditText);
        saveButton = findViewById(R.id.saveButton);
        editButton = findViewById(R.id.editButton);

        // Initially enable editing mode (as before)
        setEditingEnabled(true);

        saveButton.setOnClickListener(v -> {
            saveProfileData(); // Maintain original saving functionality
            setEditingEnabled(false); // Disable fields
            saveButton.setVisibility(View.GONE); // Hide Save button
            editButton.setVisibility(View.VISIBLE); // Show Edit button
        });

        editButton.setOnClickListener(v -> {
            setEditingEnabled(true); // Enable fields for editing
            editButton.setVisibility(View.GONE); // Hide Edit button
            saveButton.setVisibility(View.VISIBLE); // Show Save button
        });
    }

    private void saveProfileData() {
        // Get user input (maintaining original structure)
        String username = usernameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String dob = dobEditText.getText().toString();
        String location = locationEditText.getText().toString();

        // TODO: Save data (e.g., Firebase or SharedPreferences)
    }

    private void setEditingEnabled(boolean enabled) {
        usernameEditText.setEnabled(enabled);
        emailEditText.setEnabled(enabled);
        dobEditText.setEnabled(enabled);
        locationEditText.setEnabled(enabled);
    }
}

