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

public class AddMoodFragment extends DialogFragment {

    private MoodDialogListener listener;
    private EditText reasonEditText;

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
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_mood, null);
        Spinner emotionSpinner = view.findViewById(R.id.emotionSpinner);
        reasonEditText = view.findViewById(R.id.reasonEditText);

        reasonEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("Add a Mood")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (dialog, which) -> {
                    String selectedEmotion = emotionSpinner.getSelectedItem().toString();
                    String reasonText = reasonEditText.getText().toString();
                    if (reasonText.length() > 20 || reasonText.split("\\s+", -1).length > 3) {
                        reasonEditText.setError("Reason must be max 20 characters or 3 words");
                        return;
                    }
                    listener.AddMood(new Mood(selectedEmotion, reasonText, null, null));
                })
                .create();
    }
}