package com.example.an_droids;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.firestore.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditMoodFragment extends DialogFragment {

    private MoodDialogListener listener;
    private Spinner emotionSpinner;
    private Spinner socialSituationSpinner;
    private Spinner privacySpinner;
    private EditText dateEditText;
    private EditText timeEditText;
    private EditText reasonEditText;
    private ImageView selectImage;
    private Bitmap image;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();
    private static final int REQUEST_IMAGE_GALLERY = 1;
    private static final int REQUEST_IMAGE_CAMERA = 2;
    private static final int MAX_IMAGE_SIZE = 65536;

    private VoiceNoteUtil voiceUtil = new VoiceNoteUtil();
    private byte[] voiceNoteBytes = null;

    public static EditMoodFragment newInstance(Mood mood) {
        EditMoodFragment fragment = new EditMoodFragment();
        Bundle args = new Bundle();
        args.putSerializable("mood", mood);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(MoodDialogListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_mood, null);

        emotionSpinner = view.findViewById(R.id.emotionSpinner);
        socialSituationSpinner = view.findViewById(R.id.socialSituationSpinner);
        privacySpinner = view.findViewById(R.id.privacySpinner);
        dateEditText = view.findViewById(R.id.dateEditText);
        timeEditText = view.findViewById(R.id.timeEditText);
        reasonEditText = view.findViewById(R.id.reasonEditText);
        selectImage = view.findViewById(R.id.uploadImage);
        Button recordButton = view.findViewById(R.id.recordVoiceButton);
        Button playButton = view.findViewById(R.id.playVoiceButton);

        reasonEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});

        Mood mood = (Mood) getArguments().getSerializable("mood");
        if (mood == null) {
            throw new IllegalArgumentException("No Mood passed to EditMoodFragment");
        }

        // Restore date/time
        Date timestamp = mood.getTimestamp();
        if (timestamp != null) {
            calendar.setTime(timestamp);
        }
        dateEditText.setText(dateFormatter.format(calendar.getTime()));
        timeEditText.setText(timeFormatter.format(calendar.getTime()));

        // Restore mood values
        reasonEditText.setText(mood.getReason());
        if (mood.getImage() != null) {
            image = mood.getImage();
            selectImage.setImageBitmap(image);
        }

        // Load existing voice note blob if it exists
        if (mood.getVoiceNoteBlob() != null) {
            voiceNoteBytes = mood.getVoiceNoteBlob().toBytes();
        }

        // Set spinner selections
        setSpinnerSelection(emotionSpinner, mood.getEmotion().toString());
        setSpinnerSelection(socialSituationSpinner, mood.getSocialSituation());
        setSpinnerSelection(privacySpinner, capitalize(mood.getPrivacy().name()));

        dateEditText.setOnClickListener(v -> showDatePickerDialog());
        timeEditText.setOnClickListener(v -> showTimePickerDialog());
        selectImage.setOnClickListener(v -> showImagePickerDialog());

        // Voice recording button
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
            if (voiceNoteBytes != null) {
                try {
                    voiceUtil.startPlayback(requireContext(), voiceNoteBytes);
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Playback failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "No voice note", Toast.LENGTH_SHORT).show();
            }
        });

        return new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle("Edit Mood")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String selectedEmotion = emotionSpinner.getSelectedItem().toString();
                    String selectedSocialSituation = socialSituationSpinner.getSelectedItem().toString();
                    String reason = reasonEditText.getText().toString();
                    Date newDate = calendar.getTime();

                    // Compress image
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
                            Toast.makeText(getContext(), "Image exceeds size limit. Use smaller image.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    }

                    // Update mood
                    mood.setEmotion(selectedEmotion);
                    mood.setTimestamp(newDate);
                    mood.setReason(reason);
                    mood.setImage(image);
                    mood.setSocialSituation(selectedSocialSituation);
                    Mood.Privacy selectedPrivacy = Mood.Privacy.valueOf(
                            privacySpinner.getSelectedItem().toString().toUpperCase(Locale.ROOT));
                    mood.setPrivacy(selectedPrivacy);

                    if (voiceNoteBytes != null) {
                        mood.setVoiceNoteBlob(Blob.fromBytes(voiceNoteBytes));
                    }

                    listener.EditMood(mood);
                })
                .create();
    }

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            calendar.set(Calendar.YEAR, y);
            calendar.set(Calendar.MONTH, m);
            calendar.set(Calendar.DAY_OF_MONTH, d);
            dateEditText.setText(dateFormatter.format(calendar.getTime()));
        }, year, month, day).show();
    }

    private void showTimePickerDialog() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        new TimePickerDialog(requireContext(), (view, h, m) -> {
            calendar.set(Calendar.HOUR_OF_DAY, h);
            calendar.set(Calendar.MINUTE, m);
            timeEditText.setText(timeFormatter.format(calendar.getTime()));
        }, hour, minute, true).show();
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Select Image")
                .setItems(new CharSequence[]{"Choose from Gallery", "Take a Picture", "Cancel"},
                        (dialog, which) -> {
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
                    image = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
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

    private void setSpinnerSelection(Spinner spinner, String valueToSelect) {
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            String item = spinner.getAdapter().getItem(i).toString();
            if (item.equalsIgnoreCase(valueToSelect)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private String capitalize(String text) {
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1).toLowerCase(Locale.ROOT);
    }
}