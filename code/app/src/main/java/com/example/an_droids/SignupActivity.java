package com.example.an_droids;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Activity for the user sign-up process.
 * This activity handles user registration, input validation, profile image selection,
 * and storing user data (including profile image) into Firebase.
 */
public class SignupActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput, reenterPasswordInput, dobInput;
    private Button signupButton;
    private ImageView avatarImage;
    private TextView loginLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private Date dobDate;

    private static final int REQUEST_GALLERY = 101;
    private static final int REQUEST_CAMERA = 102;
    private static final int MAX_IMAGE_SIZE = 65536;
    private Bitmap profileBitmap;

    /**
     * Initializes the activity. Sets up input fields, buttons, and the date picker for the date of birth.
     * Also sets up listeners for handling user input.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        reenterPasswordInput = findViewById(R.id.reenterPasswordInput);
        dobInput = findViewById(R.id.dobInput);
        signupButton = findViewById(R.id.signupButton);
        avatarImage = findViewById(R.id.avatarImage);
        loginLink = findViewById(R.id.loginLink);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        dobInput.setOnClickListener(v -> showDatePicker());

        signupButton.setOnClickListener(v -> signUpUser());

        loginLink.setOnClickListener(v -> startActivity(new Intent(SignupActivity.this, LoginActivity.class)));

        avatarImage.setOnClickListener(v -> showImagePickerDialog());
    }

    /**
     * Displays a date picker dialog to let the user select their date of birth.
     */
    private void showDatePicker() {
        new DatePickerDialog(SignupActivity.this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    dobDate = calendar.getTime();
                    dobInput.setText(dateFormatter.format(dobDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /**
     * Handles user sign-up by validating inputs and creating a new user account.
     * If the sign-up is successful, the user data (including profile image) is saved in Firebase.
     */
    private void signUpUser() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String reenterPassword = reenterPasswordInput.getText().toString();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() ||
                reenterPassword.isEmpty() || dobInput.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(reenterPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            Users user = new Users(username, email, dobDate);

                            // Save image as blob if available
                            if (profileBitmap != null) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                int quality = 80;
                                profileBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                                byte[] imageBytes = baos.toByteArray();
                                while (imageBytes.length > MAX_IMAGE_SIZE && quality > 10) {
                                    baos.reset();
                                    quality -= 10;
                                    profileBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                                    imageBytes = baos.toByteArray();
                                }
                                if (imageBytes.length <= MAX_IMAGE_SIZE) {
                                    user.setProfileImageBlob(Blob.fromBytes(imageBytes));
                                }
                            }

                            firestore.collection("Users").document(firebaseUser.getUid())
                                    .set(user, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(this, "Sign Up Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Displays an alert dialog for the user to choose their profile picture from either the gallery or by taking a new picture.
     */
    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Profile Picture")
                .setItems(new CharSequence[]{"Choose from Gallery", "Take a Picture", "Cancel"}, (dialog, which) -> {
                    if (which == 0) pickImageFromGallery();
                    else if (which == 1) captureImageFromCamera();
                }).show();
    }

    /**
     * Starts an intent to pick an image from the device's gallery.
     */
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    /**
     * Starts an intent to capture an image using the device's camera.
     */
    private void captureImageFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    /**
     * Handles the result from the image picker or camera. It processes the selected or captured image and updates the profile picture.
     * @param requestCode The request code for the image picker or camera action.
     * @param resultCode The result code of the activity.
     * @param data The intent data containing the image.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Bitmap selectedBitmap = null;
            try {
                if (requestCode == REQUEST_GALLERY) {
                    Uri imageUri = data.getData();
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } else if (requestCode == REQUEST_CAMERA) {
                    Bundle extras = data.getExtras();
                    selectedBitmap = (Bitmap) extras.get("data");
                }

                if (selectedBitmap != null) {
                    profileBitmap = selectedBitmap;
                    avatarImage.setImageBitmap(profileBitmap);
                }

            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
