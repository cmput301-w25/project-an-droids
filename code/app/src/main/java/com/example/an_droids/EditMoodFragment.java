package com.example.an_droids;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EditMoodFragment extends DialogFragment {

    private  MoodDialogListener listener;
    private EditText dateEditText;
    private EditText timeEditText;
    private Spinner emotionSpinner;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

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
        dateEditText = view.findViewById(R.id.dateEditText);
        timeEditText = view.findViewById(R.id.timeEditText);
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        Mood mood = (Mood) getArguments().getSerializable("mood");

        LocalDateTime timestamp = mood.getTimestamp();

        String dateString = timestamp.format(dateFormatter);
        String timeString = timestamp.format(timeFormatter);

        dateEditText.setText(dateString);
        timeEditText.setText(timeString);

        for (int i = 0; i < emotionSpinner.getAdapter().getCount(); i++) {
            String item = (String) emotionSpinner.getAdapter().getItem(i);
            if (item.equalsIgnoreCase(mood.getEmotion().toString())) {
                emotionSpinner.setSelection(i);
                break;
            }
        }

        dateEditText.setOnClickListener(v -> showDatePickerDialog(mood));
        timeEditText.setOnClickListener(v -> showTimePickerDialog(mood));

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("Edit Mood")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String selectedEmotion = emotionSpinner.getSelectedItem().toString();
                    String selectedDate = dateEditText.getText().toString();
                    String selectedTime = timeEditText.getText().toString();

                    LocalDate date = LocalDate.parse(selectedDate, dateFormatter);
                    LocalTime time = LocalTime.parse(selectedTime, timeFormatter);

                    LocalDateTime finalDateTime = LocalDateTime.of(date, time);
                    mood.setEmotion(selectedEmotion);
                    mood.setTimestamp(finalDateTime);
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
}
