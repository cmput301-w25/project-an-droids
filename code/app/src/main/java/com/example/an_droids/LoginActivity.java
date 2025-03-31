package com.example.an_droids;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * LoginActivity handles user authentication via Firebase Authentication.
 * Users can log in using their email and password, and if they are already logged in,
 * they are redirected to the main activity.
 */
public class LoginActivity extends AppCompatActivity {
    /** Input field for email and password */
    private EditText emailInput, passwordInput;

    /** Button to initiate login process. */
    private Button loginButton;

    /** Link to navigate to the signup screen. */
    private TextView signupLink;

    /** Firebase Authentication instance. */
    private FirebaseAuth mAuth;

    /**
     * Called when the activity is first created. Initializes Firebase authentication,
     * checks if a user is already logged in, and sets up UI elements and event listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down, this contains the most recent data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // 1) If user is already signed in on this device, skip the login screen
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Go straight to MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("userId", currentUser.getUid());
            startActivity(intent);
            finish();
            return; // Important to return here so we don't inflate the layout
        }

        // 2) Otherwise, show the login layout
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signupLink = findViewById(R.id.signupLink);

        // 3) Handle login logic
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("userId", firebaseUser.getUid());
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // 4) If user doesn’t have an account, send them to signup
        signupLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );
    }
}
