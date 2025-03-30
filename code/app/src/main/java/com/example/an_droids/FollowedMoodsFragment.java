package com.example.an_droids;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class FollowedMoodsFragment extends Fragment {

    private ListView listView;
    private MoodArrayAdapter adapter;

    private final ArrayList<Mood> followedMoods = new ArrayList<>();
    private final ArrayList<Mood> allMoods = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private Button filterButton;

    public FollowedMoodsFragment() {}

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_followed_moods, container, false);

        listView = view.findViewById(R.id.followedMoodsListView);
        filterButton = view.findViewById(R.id.moodFilterButton); // Must be in your fragment layout

        adapter = new MoodArrayAdapter(requireContext(), followedMoods);
        listView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        loadFollowedMoods();

        filterButton.setOnClickListener(v -> showFilterOptions());

        return view;
    }

    private void loadFollowedMoods() {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> following = (List<String>) documentSnapshot.get("following");
                        if (following != null && !following.isEmpty()) {
                            fetchMoodsFromFollowedUsers(following);
                        } else {
                            Log.d("FollowedMoodsFragment", "User is not following anyone.");
                            followedMoods.clear();
                            allMoods.clear();
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("FollowedMoodsFragment", "Failed to load user doc", e));
    }

    private void fetchMoodsFromFollowedUsers(List<String> followedUserIds) {
        followedMoods.clear();
        allMoods.clear();

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (String followedUserId : followedUserIds) {
            Task<QuerySnapshot> task = db.collection("Users")
                    .document(followedUserId)
                    .collection("Moods")
                    .whereEqualTo("privacy", Mood.Privacy.PUBLIC.name())
                    .get();

            tasks.add(task);
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            for (QueryDocumentSnapshot doc : snapshot) {
                                Mood mood = doc.toObject(Mood.class);
                                if (mood != null) {
                                    allMoods.add(mood);
                                    followedMoods.add(mood);
                                }
                            }
                        }
                    }

                    // Sort and update UI
                    followedMoods.sort((a, b) -> {
                        if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    });

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("FollowedMoodsFragment", "Error fetching moods", e));
    }

    private void showFilterOptions() {
        String[] options = {"All", "Recent Week", "By Emotion", "By Reason"};
        new AlertDialog.Builder(requireContext())
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
        long oneWeekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
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

        new AlertDialog.Builder(requireContext())
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
        EditText input = new EditText(requireContext());
        input.setHint("Enter keyword");
        new AlertDialog.Builder(requireContext())
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