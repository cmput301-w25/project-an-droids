package com.example.an_droids;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FollowersMoodsFragment extends Fragment {
    private ListView followersMoodsListView;
    private MoodArrayAdapter adapter;
    private ArrayList<Mood> moodFeed;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    public FollowersMoodsFragment() {}

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_followers_mood, container, false);

        followersMoodsListView = view.findViewById(R.id.followersMoodsListView);
        moodFeed = new ArrayList<>();
        adapter = new MoodArrayAdapter(requireContext(), moodFeed);
        followersMoodsListView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadFollowersMoods();
        return view;
    }

    private void loadFollowersMoods() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();

        firestore.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    List<String> followersList = (List<String>) doc.get("followers");
                    if (followersList == null) followersList = new ArrayList<>();
                    if (followersList.isEmpty()) {
                        moodFeed.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
                    ArrayList<Mood> tempList = new ArrayList<>();

                    for (String followerId : followersList) {
                        tasks.add(
                                firestore.collection("Users")
                                        .document(followerId)
                                        .collection("Moods")
                                        .orderBy("timestamp", Query.Direction.DESCENDING)
                                        .limit(3)
                                        .get()
                                        .addOnSuccessListener(qs -> {
                                            for (var docSnap : qs.getDocuments()) {
                                                Mood m = docSnap.toObject(Mood.class);
                                                if (m != null && m.getPrivacy() == Mood.Privacy.PUBLIC) {
                                                    tempList.add(m);
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e("FollowersMoodsFragment", "Error fetching moods: " + e)
                                        )
                        );
                    }

                    Tasks.whenAllComplete(tasks).addOnSuccessListener(taskList -> {
                        tempList.sort((m1, m2) -> {
                            if (m1.getTimestamp() == null || m2.getTimestamp() == null) return 0;
                            return m2.getTimestamp().compareTo(m1.getTimestamp());
                        });
                        moodFeed.clear();
                        moodFeed.addAll(tempList);
                        adapter.notifyDataSetChanged();
                    });
                })
                .addOnFailureListener(e ->
                        Log.e("FollowersMoodsFragment", "Error loading my user doc: " + e)
                );
    }
}
