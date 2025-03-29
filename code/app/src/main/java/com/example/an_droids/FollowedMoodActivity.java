package com.example.an_droids;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FollowedMoodActivity extends AppCompatActivity {
    private ListView listView;
    private MoodArrayAdapter adapter;
    private ArrayList<Mood> followedMoods = new ArrayList<>();
    private ArrayList<Mood> allMoods = new ArrayList<>();  // Store the original followed moods
    private FirebaseFirestore db;
    private String userId;
    private Button filterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_followed_moods);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.listView);
        filterButton = findViewById(R.id.moodFilterButton);

        // Use the adapter constructor that does NOT require an ownerId.
        adapter = new MoodArrayAdapter(this, followedMoods);
        listView.setAdapter(adapter);

        loadFollowedMoods();

        filterButton.setOnClickListener(v -> showFilterOptions());
    }

    private void loadFollowedMoods() {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("followedMoods")) {
                        List<String> followedMoodsIds = (List<String>) documentSnapshot.get("followedMoods");

                        if (followedMoodsIds != null && !followedMoodsIds.isEmpty()) {
                            fetchMoods(followedMoodsIds);
                        } else {
                            Log.d("FollowedMoodActivity", "No followed moods found.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FollowedMoodActivity", "Error loading followed moods", e));
    }

    private void fetchMoods(List<String> moodIds) {
        db.collection("MoodLookup")
                .whereIn("moodId", moodIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    followedMoods.clear();
                    allMoods.clear();  // Clear the allMoods list

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String moodId = doc.getString("moodId");
                        String followedUserId = doc.getString("userId");

                        if (followedUserId != null) {
                            db.collection("Users").document(followedUserId)
                                    .collection("Moods").document(moodId)
                                    .get()
                                    .addOnSuccessListener(moodDoc -> {
                                        if (moodDoc.exists()) {
                                            Mood mood = moodDoc.toObject(Mood.class);
                                            if (mood != null) {
                                                // Optionally, if you add a transient ownerId field to Mood,
                                                // you can do: mood.setOwnerId(followedUserId);
                                                followedMoods.add(mood);
                                                allMoods.add(mood); // Add to the allMoods list
                                                adapter.notifyDataSetChanged();
                                            }
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FollowedMoodActivity", "Error fetching moods", e));
    }

    private void showFilterOptions() {
        String[] options = {"All", "Recent Week", "By Emotion", "By Reason"};
        new AlertDialog.Builder(this)
                .setTitle("Filter Moods")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            resetAndLoadAllMoods();
                            break;
                        case 1:
                            filterByRecentWeek();
                            break;
                        case 2:
                            showEmotionFilterDialog();
                            break;
                        case 3:
                            showReasonFilterDialog();
                            break;
                    }
                }).show();
    }

    private void resetAndLoadAllMoods() {
        // Reset to the original list (no filtering applied)
        followedMoods.clear();
        followedMoods.addAll(allMoods);
        adapter.notifyDataSetChanged();
    }

    private void filterByRecentWeek() {
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        ArrayList<Mood> filteredMoods = new ArrayList<>();

        for (Mood mood : allMoods) {  // Use the allMoods list
            if (mood.getTimestamp().getTime() >= oneWeekAgo) {
                filteredMoods.add(mood);
            }
        }

        followedMoods.clear();
        followedMoods.addAll(filteredMoods);
        adapter.notifyDataSetChanged();
    }

    private void showEmotionFilterDialog() {
        Mood.EmotionalState[] values = Mood.EmotionalState.values();
        String[] emotionNames = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            emotionNames[i] = values[i].name();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Emotion")
                .setItems(emotionNames, (dialog, which) -> filterByEmotion(emotionNames[which]))
                .show();
    }

    private void filterByEmotion(String emotion) {
        ArrayList<Mood> filteredMoods = new ArrayList<>();

        for (Mood mood : allMoods) {  // Use the allMoods list
            if (mood.getEmotion().name().equalsIgnoreCase(emotion)) {
                filteredMoods.add(mood);
            }
        }

        followedMoods.clear();
        followedMoods.addAll(filteredMoods);
        adapter.notifyDataSetChanged();
    }

    private void showReasonFilterDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter keyword");

        new AlertDialog.Builder(this)
                .setTitle("Filter by Reason")
                .setView(input)
                .setPositiveButton("Filter", (dialog, which) -> {
                    String keyword = input.getText().toString().trim();
                    if (!keyword.isEmpty()) filterByReasonContains(keyword);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterByReasonContains(String keyword) {
        ArrayList<Mood> filteredMoods = new ArrayList<>();

        for (Mood mood : allMoods) {  // Use the allMoods list
            if (mood.getReason() != null && mood.getReason().toLowerCase().contains(keyword.toLowerCase())) {
                filteredMoods.add(mood);
            }
        }

        followedMoods.clear();
        followedMoods.addAll(filteredMoods);
        adapter.notifyDataSetChanged();
    }
}
