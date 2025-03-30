package com.example.an_droids;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class AddMoodFragment extends DialogFragment {

    private MoodDialogListener listener;
    private Spinner emotionSpinner, socialSituationSpinner, privacySpinner;
    private EditText reasonEditText;
    private ImageView selectImage, locationButton;
    private TextView locationText;
    private Bitmap image;
    private Location currentLocation;

    private static final int REQUEST_IMAGE_GALLERY = 1;
    private static final int REQUEST_IMAGE_CAMERA = 2;
    private static final int MAX_IMAGE_SIZE = 65536;
    private VoiceNoteUtil voiceUtil = new VoiceNoteUtil();
    private byte[] voiceNoteBytes = null;

    public void setListener(MoodDialogListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_mood, null);

        emotionSpinner = view.findViewById(R.id.emotionSpinner);
        reasonEditText = view.findViewById(R.id.reasonEditText);
        selectImage = view.findViewById(R.id.uploadImage);
        socialSituationSpinner = view.findViewById(R.id.socialSituationSpinner);
        privacySpinner = view.findViewById(R.id.privacySpinner);
        locationButton = view.findViewById(R.id.locationButton);
        locationText = view.findViewById(R.id.locationText);
        Button recordButton = view.findViewById(R.id.recordVoiceButton);
        Button playButton = view.findViewById(R.id.playVoiceButton);

        reasonEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
        selectImage.setOnClickListener(v -> showImagePickerDialog());
        locationButton.setOnClickListener(v -> fetchLocation());
        recordButton.setOnClickListener(v -> {
            if (recordButton.getText().toString().contains("🎙")) {
                if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.RECORD_AUDIO}, 201);
                    Toast.makeText(getContext(), "Please grant microphone permission", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    voiceUtil.startRecording(requireContext());
                    recordButton.setText("🛑 Stop");
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Recording failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                try {
                    voiceNoteBytes = voiceUtil.stopRecording();
                    recordButton.setText("🎙 Record");
                    Toast.makeText(getContext(), "Recording saved!", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Stop failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Voice playback button
        playButton.setOnClickListener(v -> {
            if (voiceNoteBytes != null && voiceNoteBytes.length > 0) {
                Log.d("VOICE", "Playback starting, bytes = " + voiceNoteBytes.length);
                try {
                    voiceUtil.startPlayback(requireContext(), voiceNoteBytes);
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Playback failed", Toast.LENGTH_SHORT).show();
                    Log.e("VOICE", "Playback failed", e);
                }
            } else {
                Log.d("VOICE", "No voice note to play");
                Toast.makeText(getContext(), "No voice note", Toast.LENGTH_SHORT).show();
            }
        });

        return new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle("Add a Mood")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (dialog, which) -> onSubmit())
                .create();
    }

    private void onSubmit() {
        String selectedEmotion = emotionSpinner.getSelectedItem().toString();
        String reasonText = reasonEditText.getText().toString().trim();
        String selectedSocialSituation = socialSituationSpinner.getSelectedItem().toString();
        String selectedPrivacy = privacySpinner.getSelectedItem().toString();

        if (reasonText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a reason", Toast.LENGTH_SHORT).show();
            return;
        }

        // Compress the image under 65KB if it exists
        if (image != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int quality = 80;
            image.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] imageBytes = baos.toByteArray();

            while (imageBytes.length > MAX_IMAGE_SIZE && quality > 10) {
                baos.reset();
                quality -= 10;
                image.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                imageBytes = baos.toByteArray();
            }

            if (imageBytes.length > MAX_IMAGE_SIZE) {
                Toast.makeText(getContext(), "Image too large. Try smaller one.", Toast.LENGTH_LONG).show();
                return;
            }

            image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }

        // Create Mood object
        Mood newMood = new Mood(selectedEmotion, reasonText, null, image, selectedSocialSituation, Mood.Privacy.valueOf(selectedPrivacy));

        // Add owner ID
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        newMood.setOwnerId(currentUserId);

        if (currentLocation != null) {
            newMood.setLatitude(currentLocation.getLatitude());
            newMood.setLongitude(currentLocation.getLongitude());
        }

        if (locationText.getTag() != null) {
            newMood.setAddress(locationText.getTag().toString());
        }

        if (voiceNoteBytes != null) {
            newMood.setVoiceNoteBlob(Blob.fromBytes(voiceNoteBytes));
        }

        try {
            listener.AddMood(newMood);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to add mood", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void fetchLocation() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;

                        // Run geocoder off main thread
                        new Thread(() -> {
                            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                            try {
                                List<Address> addresses = geocoder.getFromLocation(
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        1
                                );

                                if (!addresses.isEmpty()) {
                                    Address addr = addresses.get(0);
                                    StringBuilder addressBuilder = new StringBuilder();

                                    if (addr.getThoroughfare() != null) addressBuilder.append(addr.getThoroughfare()).append(" ");
                                    if (addr.getFeatureName() != null) addressBuilder.append(addr.getFeatureName()).append(", ");
                                    if (addr.getLocality() != null) addressBuilder.append(addr.getLocality()).append(", ");
                                    if (addr.getAdminArea() != null) addressBuilder.append(addr.getAdminArea()).append(", ");
                                    if (addr.getPostalCode() != null) addressBuilder.append(addr.getPostalCode()).append(", ");
                                    if (addr.getCountryName() != null) addressBuilder.append(addr.getCountryName());

                                    String fullAddress = addressBuilder.toString();

                                    // Update UI on the main thread
                                    requireActivity().runOnUiThread(() -> {
                                        locationText.setText(fullAddress);
                                        locationText.setTag(fullAddress);
                                    });
                                }
                            } catch (IOException e) {
                                requireActivity().runOnUiThread(() -> locationText.setText("Geocoder failed"));
                                Log.e("LOCATION", "Geocoder failed", e);
                            }
                        }).start();

                    } else {
                        locationText.setText("Location: Unavailable");
                    }
                })
                .addOnFailureListener(e -> locationText.setText("Failed to get location"));
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Select Image")
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
            if (requestCode == REQUEST_IMAGE_GALLERY) {
                Uri imageUri = data.getData();
                try {
                    image = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                    selectImage.setImageBitmap(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_IMAGE_CAMERA) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    image = (Bitmap) extras.get("data");
                    selectImage.setImageBitmap(image);
                }
            }
        }
    }
}