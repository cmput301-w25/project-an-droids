package com.example.an_droids;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActivity extends AppCompatActivity {
    private EditText usernameInput, emailInput, passwordInput, reenterPasswordInput, dobInput, locationInput;
    private Button signupButton;
    private ImageView avatarImage;
    private TextView loginLink;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        reenterPasswordInput = findViewById(R.id.reenterPasswordInput);
        dobInput = findViewById(R.id.dobInput);
        locationInput = findViewById(R.id.locationInput);
        signupButton = findViewById(R.id.signupButton);
        avatarImage = findViewById(R.id.avatarImage);
        loginLink = findViewById(R.id.loginLink);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        signupButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            String reenterPassword = reenterPasswordInput.getText().toString();
            String dob = dobInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() ||
                    reenterPassword.isEmpty() || dob.isEmpty() || location.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(reenterPassword)) {
                Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                Users newUser = new Users(username, email, location, dob);
                                firestore.collection("Users").document(firebaseUser.getUid())
                                        .set(newUser)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(SignupActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();

                                            // Redirect to MainActivity instead of ProfileActivity
                                            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                                            intent.putExtra("userId", firebaseUser.getUid());
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(SignupActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                            }
                        } else {
                            Toast.makeText(SignupActivity.this, "Sign Up Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        loginLink.setOnClickListener(v -> startActivity(new Intent(SignupActivity.this, LoginActivity.class)));
    }
}

