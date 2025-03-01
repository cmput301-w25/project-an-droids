package com.example.an_droids;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<MoodEvent> moodEvents; // Stores the mood blocks
    private MoodEventAdapter adapter; // Handles RecyclerView updates
    private RecyclerView moodRecyclerView;// RecyclerView to display mood events

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;


        });

        // Initialize RecyclerView
        moodRecyclerView = findViewById(R.id.moodRecyclerView);
        moodEvents = new ArrayList<>();

        // Create initial Mood Event blocks with default title & color
        for (int i = 0; i < 5; i++) {
            moodEvents.add(new MoodEvent("Mood Event Title", Color.LTGRAY));
        }

        // Set up RecyclerView adapter
        adapter = new MoodEventAdapter(this, moodEvents);
        moodRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        moodRecyclerView.setAdapter(adapter);
    }

    // Handle the result from MoodActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String selectedMood = data.getStringExtra("selectedMood");
            String emoji = data.getStringExtra("emoji");
            String color = data.getStringExtra("color");
            int position = data.getIntExtra("position", -1);

            if (position != -1) {
                // Update mood event with selected mood, emoji, and color
                MoodEvent updatedMoodEvent = moodEvents.get(position);
                updatedMoodEvent.setMoodText(emoji + " " + selectedMood);
                updatedMoodEvent.setMoodColor(Color.parseColor(color));
                adapter.notifyItemChanged(position);
            }
        }
    }



}