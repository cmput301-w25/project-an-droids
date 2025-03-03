package com.example.an_droids;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoodActivity extends AppCompatActivity {

    private Spinner emotionSpinner; // Dropdown to select emotions
    private EditText inputDay, inputMonth, inputYear; // Date input fields
    private Button saveButton; // Button to save selected mood
    private String selectedMood = ""; // Stores selected mood
    private int position = -1; // Stores the position of the clicked mood event

    private Map<String, String> moodEmojis; // Maps moods to emojis
    private Map<String, String> moodColors; // Maps moods to colors

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood);

        // Initialize UI elements
        emotionSpinner = findViewById(R.id.emotionSpinner);
        inputDay = findViewById(R.id.inputDay);
        inputMonth = findViewById(R.id.inputMonth);
        inputYear = findViewById(R.id.inputYear);
        saveButton = findViewById(R.id.saveButton);

        // Get the position of the clicked item from Intent
        position = getIntent().getIntExtra("position", -1);

        // Define mapping of moods to corresponding emojis
        moodEmojis = new HashMap<>();
        moodEmojis.put("Anger", "😠");
        moodEmojis.put("Confusion", "😕");
        moodEmojis.put("Disgust", "🤢");
        moodEmojis.put("Fear", "😨");
        moodEmojis.put("Happiness", "😃");
        moodEmojis.put("Sadness", "😢");
        moodEmojis.put("Shame", "😳");
        moodEmojis.put("Surprise", "😲");

        // Define mapping of moods to corresponding colors
        moodColors = new HashMap<>();
        moodColors.put("Anger", "#FF6666"); // Light Red
        moodColors.put("Confusion", "#C19A6B"); // Light Brown
        moodColors.put("Disgust", "#90EE90"); // Light Green
        moodColors.put("Fear", "#D8BFD8"); // Light Purple
        moodColors.put("Happiness", "#FFFF99"); // Light Yellow
        moodColors.put("Sadness", "#ADD8E6"); // Light Blue
        moodColors.put("Shame", "#FFB6C1"); // Light Pink
        moodColors.put("Surprise", "#FFD580"); // Light Orange

        // Create a list with "No Selection" as the default item
        List<String> emotions = new ArrayList<>();
        emotions.add("No Selection"); // Default value
        emotions.addAll(moodEmojis.keySet()); // Add actual emotions

        // Set up the spinner with emotions
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, emotions);
        emotionSpinner.setAdapter(adapter);
        emotionSpinner.setSelection(0, false); // Default to "No Selection"

        // Handle spinner item selection
        emotionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMood = position == 0 ? "" : parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMood = "";
            }
        });

        // Handle save button click event
        saveButton.setOnClickListener(view -> {
            String day = inputDay.getText().toString().trim();
            String month = inputMonth.getText().toString().trim();
            String year = inputYear.getText().toString().trim();

            if (selectedMood.isEmpty()) {
                Toast.makeText(this, "Please select a mood.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidDate(day, month, year)) {
                Toast.makeText(this, "Invalid date. Ensure DD (1-31), MM (1-12), YYYY (1900-2100)", Toast.LENGTH_LONG).show();
                return;
            }

            // Format the date and create the event description
            String date = String.format("Date: %02d-%02d-%04d",
                    Integer.parseInt(day), Integer.parseInt(month), Integer.parseInt(year));

            // Create a combined description of mood and date
            String emoji = moodEmojis.get(selectedMood);
            String eventDescription = emoji + " " + selectedMood + "\n" + date;

            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedMood", selectedMood);
            resultIntent.putExtra("emoji", emoji);
            resultIntent.putExtra("color", moodColors.get(selectedMood));
            resultIntent.putExtra("position", position);
            resultIntent.putExtra("date", date);
            resultIntent.putExtra("eventDescription", eventDescription);

            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });
    }

    // Method to validate the date input
    private boolean isValidDate(String day, String month, String year) {
        try {
            int d = Integer.parseInt(day);
            int m = Integer.parseInt(month);
            int y = Integer.parseInt(year);

            return d >= 1 && d <= 31 && m >= 1 && m <= 12 && y >= 1900 && y <= 2100;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
