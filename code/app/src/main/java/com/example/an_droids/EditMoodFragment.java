package com.example.an_droids;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditMoodFragment extends DialogFragment {
    private MoodDialogListener listener;
    private Spinner emotionSpinner;
    private Spinner socialSituationSpinner;
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

    public static EditMoodFragment newInstance(Mood mood) {
        EditMoodFragment fragment = new EditMoodFragment();
        Bundle args = new Bundle();
        args.putSerializable("mood", mood);
        fragment.setArguments(args);
        return fragment;
    }

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
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_mood, null);
        emotionSpinner = view.findViewById(R.id.emotionSpinner);
        socialSituationSpinner = view.findViewById(R.id.socialSituationSpinner);
        dateEditText = view.findViewById(R.id.dateEditText);
        timeEditText = view.findViewById(R.id.timeEditText);
        reasonEditText = view.findViewById(R.id.reasonEditText);
        selectImage = view.findViewById(R.id.uploadImage);
        Mood mood = (Mood) getArguments().getSerializable("mood");
        if (mood == null) throw new IllegalArgumentException("No Mood passed to EditMoodFragment");
        Date timestamp = mood.getTimestamp();
        if (timestamp != null) {
            calendar.setTime(timestamp);
        }
        dateEditText.setText(dateFormatter.format(calendar.getTime()));
        timeEditText.setText(timeFormatter.format(calendar.getTime()));
        reasonEditText.setText(mood.getReason());
        if (mood.getImage() != null) {
            image = mood.getImage();
            selectImage.setImageBitmap(image);
        }
        for (int i = 0; i < emotionSpinner.getAdapter().getCount(); i++) {
            String item = (String) emotionSpinner.getAdapter().getItem(i);
            if (item.equalsIgnoreCase(mood.getEmotion().toString())) {
                emotionSpinner.setSelection(i);
                break;
            }
        }
        for (int i = 0; i < socialSituationSpinner.getAdapter().getCount(); i++) {
            String item = (String) socialSituationSpinner.getAdapter().getItem(i);
            if (item.equalsIgnoreCase(mood.getSocialSituation())) {
                socialSituationSpinner.setSelection(i);
                break;
            }
        }
        dateEditText.setOnClickListener(v -> showDatePickerDialog());
        timeEditText.setOnClickListener(v -> showTimePickerDialog());
        selectImage.setOnClickListener(v -> showImagePickerDialog());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder.setView(view)
                .setTitle("Edit Mood")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String selectedEmotion = emotionSpinner.getSelectedItem().toString();
                    String selectedSocialSituation = socialSituationSpinner.getSelectedItem().toString();
                    String reason = reasonEditText.getText().toString();
                    Date newDate = calendar.getTime();
                    mood.setEmotion(selectedEmotion);
                    mood.setTimestamp(newDate);
                    mood.setReason(reason);
                    mood.setImage(image);
                    mood.setSocialSituation(selectedSocialSituation);
                    listener.EditMood(mood);
                }).create();
    }

    private void showDatePickerDialog() {
        int defaultYear = calendar.get(Calendar.YEAR);
        int defaultMonth = calendar.get(Calendar.MONTH);
        int defaultDay = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    dateEditText.setText(dateFormatter.format(calendar.getTime()));
                }, defaultYear, defaultMonth, defaultDay);
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        int defaultHour = calendar.get(Calendar.HOUR_OF_DAY);
        int defaultMinute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    timeEditText.setText(timeFormatter.format(calendar.getTime()));
                }, defaultHour, defaultMinute, true);
        timePickerDialog.show();
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Select Image")
                .setItems(new CharSequence[]{"Choose from Gallery", "Take a Picture", "Cancel"},
                        (dialog, which) -> {
                            if (which == 0) pickImageFromGallery();
                            else if (which == 1) captureImageFromCamera();
                            else dialog.dismiss();
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
}
