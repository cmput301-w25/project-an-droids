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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EditMoodFragment extends DialogFragment {

    private  MoodDialogListener listener;
    private EditText dateEditText;
    private EditText timeEditText;
    private Spinner emotionSpinner;
    private Spinner socialSituationSpinner;
    private EditText reasonEditText;
    private ImageView selectImage;
    private Bitmap image;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private int REQUEST_IMAGE_GALLERY = 1;
    private int REQUEST_IMAGE_CAMERA = 2;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof MoodDialogListener) {
            listener = (MoodDialogListener) context;
        }
        else {
            throw new RuntimeException((context + "must implement MoodDialogListener"));
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
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        Mood mood = (Mood) getArguments().getSerializable("mood");

        LocalDateTime timestamp = mood.getTimestamp();

        String dateString = timestamp.format(dateFormatter);
        String timeString = timestamp.format(timeFormatter);

        dateEditText.setText(dateString);
        timeEditText.setText(timeString);
        reasonEditText.setText(mood.getReason());
        if (mood.getImage() != null) selectImage.setImageBitmap(mood.getImage());


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

        dateEditText.setOnClickListener(v -> showDatePickerDialog(mood));
        timeEditText.setOnClickListener(v -> showTimePickerDialog(mood));
        selectImage.setOnClickListener(view1 -> showImagePickerDialog());

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("Edit Mood")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String selectedEmotion = emotionSpinner.getSelectedItem().toString();
                    String selectedSocialSituation = socialSituationSpinner.getSelectedItem().toString();
                    String selectedDate = dateEditText.getText().toString();
                    String selectedTime = timeEditText.getText().toString();
                    String reason = reasonEditText.getText().toString();

                    LocalDate date = LocalDate.parse(selectedDate, dateFormatter);
                    LocalTime time = LocalTime.parse(selectedTime, timeFormatter);

                    LocalDateTime finalDateTime = LocalDateTime.of(date, time);
                    mood.setEmotion(selectedEmotion);
                    mood.setTimestamp(finalDateTime);
                    mood.setReason(reason);
                    mood.setImage(image);
                    mood.setSocialSituation(selectedSocialSituation);
                    listener.EditMood(mood);
                })
                .create();
    }

    private void showDatePickerDialog(Mood mood) {
        LocalDateTime timestamp = mood.getTimestamp();
        LocalDate localDate = timestamp.toLocalDate();
        int defaultYear = localDate.getYear();
        int defaultMonth = localDate.getMonthValue() - 1;
        int defaultDay = localDate.getDayOfMonth();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    LocalDate pickedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    dateEditText.setText(pickedDate.format(dateFormatter));
                },
                defaultYear,
                defaultMonth,
                defaultDay
        );
        datePickerDialog.show();
    }

    private void showTimePickerDialog(Mood mood) {
        LocalDateTime timestamp = mood.getTimestamp();
        LocalTime localTime = timestamp.toLocalTime();
        int defaultHour = localTime.getHour();
        int defaultMinute = localTime.getMinute();

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    LocalTime pickedTime = LocalTime.of(hourOfDay, minute);
                    timeEditText.setText(pickedTime.format(timeFormatter));
                },
                defaultHour,
                defaultMinute,
                true
        );
        timePickerDialog.show();
    }
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Image")
                .setItems(new CharSequence[]{"Choose from Gallery", "Take a Picture", "Go back"}, (dialog, which) -> {
                    if (which == 0) {
                        pickImageFromGallery();
                    } else if (which == 1) {
                        captureImageFromCamera();
                    } else if (which == 2) {
                        return;
                    }
                })
                .show();
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