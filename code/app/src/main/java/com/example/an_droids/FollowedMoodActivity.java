package com.example.an_droids;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import androidx.annotation.NonNull;
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
    private FirebaseFirestore db;
    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_followed_moods);

        listView = findViewById(R.id.listView);
        adapter = new MoodArrayAdapter(this, followedMoods);
        listView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadFollowedMoods();
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
                    followedMoods.clear(); // Clear previous moods to avoid duplicates

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
                                                followedMoods.add(mood);
                                                adapter.notifyDataSetChanged(); // Update ListView
                                            }
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FollowedMoodActivity", "Error fetching moods", e));
    }
}
