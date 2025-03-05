
package com.example.an_droids;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
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

    private MoodDialogListener listener;
    private EditText dateEditText;
    private EditText timeEditText;
    private Spinner emotionSpinner;
    private EditText reasonEditText;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof MoodDialogListener) {
            listener = (MoodDialogListener) context;
        } else {
            throw new RuntimeException((context + " must implement MoodDialogListener"));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_mood, null);
        emotionSpinner = view.findViewById(R.id.emotionSpinner);
        dateEditText = view.findViewById(R.id.dateEditText);
        timeEditText = view.findViewById(R.id.timeEditText);
        reasonEditText = view.findViewById(R.id.reasonEditText);

        Mood mood = (Mood) getArguments().getSerializable("mood");

        dateEditText.setText(mood.getTimestamp().toLocalDate().toString());
        timeEditText.setText(mood.getTimestamp().toLocalTime().toString());
        reasonEditText.setText(mood.getReason());

        reasonEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });


        for (int i = 0; i < emotionSpinner.getAdapter().getCount(); i++) {
            if (emotionSpinner.getAdapter().getItem(i).toString().equalsIgnoreCase(mood.getEmotion().toString())) {
                emotionSpinner.setSelection(i);
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("Edit Mood")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String reasonText = reasonEditText.getText().toString();
                    if (reasonText.length() > 20 || reasonText.split("\\s+", -1).length > 3) {
                        reasonEditText.setError("Reason must be max 20 characters or 3 words");
                        return;
                    }
                    mood.setEmotion(emotionSpinner.getSelectedItem().toString());
                    mood.setReason(reasonEditText.getText().toString());
                    listener.EditMood(mood);
                })
                .create();
    }
}

