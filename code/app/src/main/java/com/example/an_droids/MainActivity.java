package com.example.an_droids;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private List<MoodEvent> moodEvents; // List to store mood events
    private static final int EDIT_MOOD_EVENT_REQUEST = 1;

    private MoodEventAdapter adapter; // Adapter for the RecyclerView
    private RecyclerView moodRecyclerView; // RecyclerView to display mood events

    // Mapping of moods to emojis and colors
    private Map<String, String> moodEmojis;
    private Map<String, String> moodColors;
    private String selectedMood = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        moodRecyclerView = findViewById(R.id.moodRecyclerView);
        moodEvents = new ArrayList<>();

        // Initialize the list with default mood events
        for (int i = 0; i < 5; i++) {
            moodEvents.add(new MoodEvent("Mood Event Title", android.graphics.Color.LTGRAY));
        }

        // Set up RecyclerView with the custom adapter
        adapter = new MoodEventAdapter(this, moodEvents);
        moodRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        moodRecyclerView.setAdapter(adapter);

        // Initialize Floating Action Button and set its click listener
        FloatingActionButton addMoodButton = findViewById(R.id.addMoodButton);
        addMoodButton.setOnClickListener(v -> showAddMoodDialog());

        // Initialize mappings of moods to emojis and colors
        initializeMoodMappings();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_MOOD_EVENT_REQUEST && resultCode == RESULT_OK && data != null) {
            String updatedDescription = data.getStringExtra("eventDescription");
            int position = data.getIntExtra("position", -1);

            // Get the color as a String and convert to an int using Color.parseColor()
            String colorHex = data.getStringExtra("color");
            int color = Color.parseColor(colorHex); // This is the correct way to parse the color

            if (position != -1 && updatedDescription != null) {
                // Update the MoodEvent with the new description and color
                moodEvents.set(position, new MoodEvent(updatedDescription, color));
                adapter.notifyItemChanged(position);
            }
        }
    }



    // Initialize the emoji and color mappings for different moods
    private void initializeMoodMappings() {
        moodEmojis = new HashMap<>();
        moodEmojis.put("Anger", "😠");
        moodEmojis.put("Confusion", "😕");
        moodEmojis.put("Disgust", "🤢");
        moodEmojis.put("Fear", "😨");
        moodEmojis.put("Happiness", "😃");
        moodEmojis.put("Sadness", "😢");
        moodEmojis.put("Shame", "😳");
        moodEmojis.put("Surprise", "😲");

        moodColors = new HashMap<>();
        moodColors.put("Anger", "#FF6666");
        moodColors.put("Confusion", "#C19A6B");
        moodColors.put("Disgust", "#90EE90");
        moodColors.put("Fear", "#D8BFD8");
        moodColors.put("Happiness", "#FFFF99");
        moodColors.put("Sadness", "#ADD8E6");
        moodColors.put("Shame", "#FFB6C1");
        moodColors.put("Surprise", "#FFD580");
    }

    // Method to display the floating window with the mood selection interface

    private void showAddMoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Mood");

        // Inflate the mood selection layout
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.activity_mood, null, false);
        Spinner emotionSpinner = viewInflated.findViewById(R.id.emotionSpinner);
        Button saveButton = viewInflated.findViewById(R.id.saveButton);

        // Date input fields
        EditText inputDay = viewInflated.findViewById(R.id.inputDay);
        EditText inputMonth = viewInflated.findViewById(R.id.inputMonth);
        EditText inputYear = viewInflated.findViewById(R.id.inputYear);

        // Populate the Spinner with emotions
        List<String> emotions = new ArrayList<>();
        emotions.add("No Selection");
        emotions.addAll(moodEmojis.keySet());

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, emotions);
        emotionSpinner.setAdapter(spinnerAdapter);
        emotionSpinner.setSelection(0, false);

        builder.setView(viewInflated);
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set the dialog size to make the floating window larger
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9), // 90% of screen width
                (int) (getResources().getDisplayMetrics().heightPixels * 0.9) // 90% of screen height
        );

        // Handle the save button click
        saveButton.setOnClickListener(v -> {
            String selectedMood = emotionSpinner.getSelectedItem().toString();
            String day = inputDay.getText().toString();
            String month = inputMonth.getText().toString();
            String year = inputYear.getText().toString();

            if (selectedMood.equals("No Selection")) {
                Toast.makeText(this, "Please select a mood.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidDate(day, month, year)) {
                Toast.makeText(this, "Invalid date. Please check your input.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Construct the date and display format
            String date = String.format("Date: %02d-%02d-%04d",
                    Integer.parseInt(day),
                    Integer.parseInt(month),
                    Integer.parseInt(year));

            // Create a combined message for date, emoji, and emotion
            String eventDescription = date + "\n" + moodEmojis.get(selectedMood) + " " + selectedMood;

            // Create a new MoodEvent with the combined message
            MoodEvent newMoodEvent = new MoodEvent(
                    eventDescription,
                    android.graphics.Color.parseColor(moodColors.get(selectedMood))
            );

            // Add the new event to the list and update the adapter
            moodEvents.add(newMoodEvent);
            adapter.notifyItemInserted(moodEvents.size() - 1);

            // Close the dialog after saving
            dialog.dismiss();

            // Display confirmation to the user
            Toast.makeText(this, "Mood event saved!", Toast.LENGTH_SHORT).show();

            Log.d("SaveButton", "Mood event saved: " + eventDescription);
        });
    }


    // Validate the date input
    private boolean isValidDate(String day, String month, String year) {
        try {
            int d = Integer.parseInt(day);
            int m = Integer.parseInt(month);
            int y = Integer.parseInt(year);

            if (d < 1 || d > 31) return false;
            if (m < 1 || m > 12) return false;
            if (y < 1900 || y > 2100) return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
