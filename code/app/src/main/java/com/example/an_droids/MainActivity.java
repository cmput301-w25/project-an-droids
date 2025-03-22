package com.example.an_droids;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MoodDialogListener {
    private Button addMoodButton, filterButton;
    private ImageView profileButton, searchButton;
    private ListView moodListView;
    private MoodProvider moodProvider;
    private ArrayList<Mood> moodArrayList;
    private MoodArrayAdapter moodArrayAdapter;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Get the userId passed from SignUpActivity
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            // If no user is found, redirect to sign-up.
            startActivity(new Intent(MainActivity.this, SignupActivity.class));
            finish();
            return;
        }

        addMoodButton = findViewById(R.id.addButton);
        profileButton = findViewById(R.id.profileButton);
        searchButton = findViewById(R.id.searchButton);
        moodListView = findViewById(R.id.moodList);
        filterButton = findViewById(R.id.filterButton);

        // Create a new MoodProvider for the current user
        moodProvider = new MoodProvider(FirebaseFirestore.getInstance(), userId);
        moodArrayList = moodProvider.getMoods();
        moodArrayAdapter = new MoodArrayAdapter(this, moodArrayList);
        moodListView.setAdapter(moodArrayAdapter);

        moodProvider.listenForUpdates(new MoodProvider.DataStatus() {
            @Override
            public void onDataUpdated() {
                moodArrayAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Error listening for mood updates: " + error);
            }
        });

        addMoodButton.setOnClickListener(v -> {
            AddMoodFragment addMoodFragment = new AddMoodFragment();
            addMoodFragment.show(getSupportFragmentManager(), "Add Mood");
        });

        moodListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Mood mood = moodArrayAdapter.getItem(i);
            EditMoodFragment editMoodFragment = EditMoodFragment.newInstance(mood);
            editMoodFragment.show(getSupportFragmentManager(), "Edit Mood");
        });

        moodListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            Mood mood = moodArrayAdapter.getItem(i);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete Confirmation")
                    .setMessage("Are you sure you want to delete this mood?")
                    .setPositiveButton("Yes", (dialog, which) -> moodProvider.deleteMood(mood))
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        filterButton.setOnClickListener(v -> {
            showFilterOptions();
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        searchButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        });

    }

    private void showFilterOptions() {
        String[] options = {"All", "Recent Week", "By Emotion", "By Reason"};
        new AlertDialog.Builder(this)
                .setTitle("Filter Moods")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            moodProvider.loadAllMoods();
                            break;
                        case 1:
                            moodProvider.filterByRecentWeek();
                            break;
                        case 2:
                            showEmotionFilterDialog();
                            break;
                        case 3:
                            showReasonFilterDialog();
                            break;
                    }
                })
                .show();
    }

    private void showEmotionFilterDialog() {
        Mood.EmotionalState[] values = Mood.EmotionalState.values();
        String[] emotionNames = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            emotionNames[i] = values[i].name();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Emotion")
                .setItems(emotionNames, (dialog, which) -> {
                    String selectedEmotion = emotionNames[which];
                    moodProvider.filterByEmotion(selectedEmotion);
                })
                .show();
    }

    private void showReasonFilterDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter keyword");

        new AlertDialog.Builder(this)
                .setTitle("Filter by Reason")
                .setView(input)
                .setPositiveButton("Filter", (dialog, which) -> {
                    String keyword = input.getText().toString().trim();
                    if (!keyword.isEmpty()) {
                        moodProvider.filterByReasonContains(keyword);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void AddMood(Mood mood) {
        moodProvider.addMood(mood);
    }

    @Override
    public void EditMood(Mood mood) {
        moodProvider.updateMood(mood);
    }
}
