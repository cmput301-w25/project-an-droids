package com.example.an_droids;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private EditText usernameEditText, emailEditText, dobEditText;
    private Button saveButton, logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser firebaseUser;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private Date selectedDOB;
    private ImageView avatarImage;
    private static final int REQUEST_IMAGE_GALLERY = 101;
    private static final int REQUEST_IMAGE_CAMERA = 102;
    private static final int MAX_IMAGE_SIZE = 65536;
    private Bitmap profileBitmap;

    public static ProfileFragment newInstance(String userId) {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false); // Reuse layout ✅
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        dobEditText = view.findViewById(R.id.dobEditText);
        saveButton = view.findViewById(R.id.saveButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        avatarImage = view.findViewById(R.id.avatarImage);
        ImageView backButton = view.findViewById(R.id.backButton);
        Button editButton = view.findViewById(R.id.editButton);


        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        emailEditText.setEnabled(false);

        if (firebaseUser != null) {
            loadUserProfile();
        }

        saveButton.setOnClickListener(v -> updateUserProfile());

        avatarImage.setOnClickListener(v -> showImagePickerDialog());

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        editButton.setOnClickListener(v -> {
            usernameEditText.setEnabled(true);
            dobEditText.setEnabled(true);
            saveButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.GONE);
        });

        dobEditText.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        new DatePickerDialog(requireContext(),
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
                        if (user.getProfileBitmap() != null) {
                            profileBitmap = user.getProfileBitmap();
                            avatarImage.setImageBitmap(profileBitmap);
                        }
                        if (selectedDOB != null) {
                            dobEditText.setText(dateFormatter.format(selectedDOB));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void updateUserProfile() {
        if (firebaseUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String newUsername = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString();

        if (newUsername.isEmpty()) {
            Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        firestore.collection("Users")
                .whereEqualTo("username", newUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean usernameTaken = false;

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (var doc : queryDocumentSnapshots.getDocuments()) {
                            if (!doc.getId().equals(firebaseUser.getUid())) {
                                usernameTaken = true;
                                break;
                            }
                        }
                    }

                    if (usernameTaken) {
                        Toast.makeText(requireContext(), "Username already taken", Toast.LENGTH_SHORT).show();
                    } else {
                        Users updatedUser = new Users(newUsername, email, selectedDOB);
                        updatedUser.setProfileBitmap(profileBitmap);

                        firestore.collection("Users").document(firebaseUser.getUid())
                                .set(updatedUser, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show();
                                    usernameEditText.setEnabled(false);
                                    dobEditText.setEnabled(false);
                                    saveButton.setVisibility(View.GONE);
                                    requireView().findViewById(R.id.editButton).setVisibility(View.VISIBLE);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error checking username uniqueness", Toast.LENGTH_SHORT).show();
                });
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Profile Picture")
                .setItems(new CharSequence[]{"Choose from Gallery", "Take a Picture", "Cancel"}, (dialog, which) -> {
                    if (which == 0) pickImageFromGallery();
                    else if (which == 1) captureImageFromCamera();
                }).show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
    }

    private void captureImageFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_IMAGE_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Bitmap selectedImage = null;
            try {
                if (requestCode == REQUEST_IMAGE_GALLERY) {
                    Uri imageUri = data.getData();
                    selectedImage = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                } else if (requestCode == REQUEST_IMAGE_CAMERA) {
                    Bundle extras = data.getExtras();
                    selectedImage = (Bitmap) extras.get("data");
                }

                if (selectedImage != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int quality = 80;
                    selectedImage.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                    byte[] imageBytes = baos.toByteArray();
                    while (imageBytes.length > MAX_IMAGE_SIZE && quality > 10) {
                        baos.reset();
                        quality -= 10;
                        selectedImage.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                        imageBytes = baos.toByteArray();
                    }

                    if (imageBytes.length > MAX_IMAGE_SIZE) {
                        Toast.makeText(requireContext(), "Image too large. Choose a smaller one.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    profileBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    avatarImage.setImageBitmap(profileBitmap);
                }

            } catch (IOException e) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

}