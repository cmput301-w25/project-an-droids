package com.example.an_droids;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class FollowedMoodActivity extends AppCompatActivity {
    private ListView listView;
    private MoodArrayAdapter adapter;
    private final ArrayList<Mood> followedMoods = new ArrayList<>();
    private final ArrayList<Mood> allMoods = new ArrayList<>();
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

        adapter = new MoodArrayAdapter(this, followedMoods);
        listView.setAdapter(adapter);

        loadFollowedMoods();

        filterButton.setOnClickListener(v -> showFilterOptions());
    }

    private void loadFollowedMoods() {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> following = (List<String>) documentSnapshot.get("following");
                        if (following != null && !following.isEmpty()) {
                            fetchMoodsFromFollowedUsers(following);
                        } else {
                            Log.d("FollowedMoodActivity", "User is not following anyone.");
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("FollowedMoodActivity", "Failed to load user doc", e));
    }

    private void fetchMoodsFromFollowedUsers(List<String> followedUserIds) {
        followedMoods.clear();
        allMoods.clear();

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (String followedUserId : followedUserIds) {
            Task<QuerySnapshot> task = db.collection("Users")
                    .document(followedUserId)
                    .collection("Moods")
                    .whereEqualTo("privacy", Mood.Privacy.PUBLIC.name()) // 💡 Only PUBLIC
                    .get();

            task.addOnSuccessListener(snapshot -> {
                for (QueryDocumentSnapshot doc : snapshot) {
                    Mood mood = doc.toObject(Mood.class);
                    if (mood != null) {
                        allMoods.add(mood);
                        followedMoods.add(mood);
                    }
                }
            });

            tasks.add(task);
        }

        // Wait for all fetches to complete
        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(done -> adapter.notifyDataSetChanged())
                .addOnFailureListener(e ->
                        Log.e("FollowedMoodActivity", "Error fetching followed moods", e));
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
        followedMoods.clear();
        for (Mood mood : allMoods) {
            if (mood.getPrivacy() == Mood.Privacy.PUBLIC) {
                followedMoods.add(mood);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterByRecentWeek() {
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        ArrayList<Mood> filtered = new ArrayList<>();

        for (Mood mood : allMoods) {
            if (mood.getTimestamp() != null && mood.getTimestamp().getTime() >= oneWeekAgo) {
                filtered.add(mood);
            }
        }

        followedMoods.clear();
        followedMoods.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void showEmotionFilterDialog() {
        Mood.EmotionalState[] values = Mood.EmotionalState.values();
        String[] emotionNames = new String[values.length];
        for (int i = 0; i < values.length; i++) emotionNames[i] = values[i].name();

        new AlertDialog.Builder(this)
                .setTitle("Select Emotion")
                .setItems(emotionNames, (dialog, which) ->
                        filterByEmotion(emotionNames[which]))
                .show();
    }

    private void filterByEmotion(String emotion) {
        ArrayList<Mood> filtered = new ArrayList<>();
        for (Mood mood : allMoods) {
            if (mood.getEmotion().name().equalsIgnoreCase(emotion)) {
                filtered.add(mood);
            }
        }
        followedMoods.clear();
        followedMoods.addAll(filtered);
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
        ArrayList<Mood> filtered = new ArrayList<>();
        for (Mood mood : allMoods) {
            if (mood.getReason() != null && mood.getReason().toLowerCase().contains(keyword.toLowerCase())) {
                filtered.add(mood);
            }
        }
        followedMoods.clear();
        followedMoods.addAll(filtered);
        adapter.notifyDataSetChanged();
    }
}