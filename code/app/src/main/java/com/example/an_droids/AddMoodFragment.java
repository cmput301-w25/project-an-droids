package com.example.an_droids;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddMoodFragment extends DialogFragment {

    private MoodDialogListener listener;
    private Spinner emotionSpinner;
    private Spinner socialSituationSpinner;
    private Spinner privacySpinner;
    private EditText reasonEditText;
    private ImageView selectImage;
    private Bitmap image;
    private final int REQUEST_IMAGE_GALLERY = 1;
    private final int REQUEST_IMAGE_CAMERA = 2;
    private static final int MAX_IMAGE_SIZE = 65536; // 65,536 bytes

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MoodDialogListener) {
            listener = (MoodDialogListener) context;
        } else {
            throw new RuntimeException(context + " must implement MoodDialogListener");
        }
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

        reasonEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        selectImage.setOnClickListener(v -> showImagePickerDialog());

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view)
                .setTitle("Add a Mood")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (dialog, which) -> {
                    String selectedEmotion = emotionSpinner.getSelectedItem().toString();
                    String reasonText = reasonEditText.getText().toString().trim();
                    String selectedSocialSituation = socialSituationSpinner.getSelectedItem().toString();
                    String selectedPrivacy = privacySpinner.getSelectedItem().toString();

                    if (reasonText.length() > 20 || reasonText.split("\\s+").length > 3) {
                        reasonEditText.setError("Reason must be max 20 characters or 3 words");
                        return;
                    }
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
                            reasonEditText.setError("Image exceeds maximum size of 65,536 bytes. Please choose a smaller image.");
                            return;
                        }
                        image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    }
                    Mood newMood = new Mood(selectedEmotion, reasonText, null, null, image, selectedSocialSituation, Mood.Privacy.valueOf(selectedPrivacy));
                    listener.AddMood(newMood);
                });
        return builder.create();
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Image")
                .setItems(new CharSequence[]{"Choose from Gallery", "Take a Picture", "Cancel"}, (dialog, which) -> {
                    if (which == 0) {
                        pickImageFromGallery();
                    } else if (which == 1) {
                        captureImageFromCamera();
                    } else {
                        dialog.dismiss();
                    }
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
