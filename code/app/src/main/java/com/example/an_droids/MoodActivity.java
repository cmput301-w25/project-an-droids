package com.example.an_droids;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoodActivity extends AppCompatActivity {

    private Spinner emotionSpinner;// Dropdown to select emotions
    private Button saveButton;// Button to save selected mood
    private String selectedMood = "";// Stores selected mood
    private int position = -1; // Stores the position of the clicked mood event
    private Map<String, String> moodEmojis; // Maps moods to emojis
    private Map<String, String> moodColors; // Maps moods to colors

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood);

        // Initialize UI elements
        emotionSpinner = findViewById(R.id.emotionSpinner);
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
        moodColors.put("Anger", "#FF6666");     // Light Red
        moodColors.put("Confusion", "#C19A6B"); // Light Brown
        moodColors.put("Disgust", "#90EE90");   // Light Green
        moodColors.put("Fear", "#D8BFD8");      // Light Purple
        moodColors.put("Happiness", "#FFFF99"); // Light Yellow
        moodColors.put("Sadness", "#ADD8E6");   // Light Blue
        moodColors.put("Shame", "#FFB6C1");     // Light Pink
        moodColors.put("Surprise", "#FFD580");  // Light Orange

        // Create a list with "No Selection" as the default item
        List<String> emotions = new ArrayList<>();
        emotions.add("No Selection"); // This will be the default value
        emotions.addAll(moodEmojis.keySet()); // Add actual emotions

        // Set up the spinner with emotions
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, emotions) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0; // Disable "No Selection"
            }
        };
        emotionSpinner.setAdapter(adapter);
        emotionSpinner.setSelection(0, false); // Default to "No Selection"

        // Handle spinner item selection
        emotionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedMood = ""; // If "No Selection" is chosen, reset selection
                } else {
                    selectedMood = parent.getItemAtPosition(position).toString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMood = "";
            }
        });

        // Handle save button click event
        saveButton.setOnClickListener(view -> {
            if (!selectedMood.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedMood", selectedMood);
                resultIntent.putExtra("emoji", moodEmojis.get(selectedMood));
                resultIntent.putExtra("color", moodColors.get(selectedMood));
                resultIntent.putExtra("position", position);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }
}
