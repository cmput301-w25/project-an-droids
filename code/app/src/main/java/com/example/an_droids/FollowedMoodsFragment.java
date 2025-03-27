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

public class FollowedMoodsFragment extends Fragment {
    private ListView feedListView;
    private MoodArrayAdapter adapter;
    private ArrayList<Mood> feedList;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    public FollowedMoodsFragment() {}

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_followed_moods, container, false);

        feedListView = view.findViewById(R.id.followedMoodsListView);
        feedList = new ArrayList<>();
        adapter = new MoodArrayAdapter(requireContext(), feedList);
        feedListView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadFollowedMoods();
        return view;
    }

    private void loadFollowedMoods() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();

        firestore.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    List<String> followingList = (List<String>) doc.get("following");
                    if (followingList == null) followingList = new ArrayList<>();
                    if (followingList.isEmpty()) {
                        feedList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }
                    List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
                    ArrayList<Mood> tempList = new ArrayList<>();

                    for (String followedUserId : followingList) {
                        tasks.add(
                                firestore.collection("Users")
                                        .document(followedUserId)
                                        .collection("Moods")
                                        .orderBy("timestamp", Query.Direction.DESCENDING)
                                        .limit(3)
                                        .get()
                                        .addOnSuccessListener(qs -> {
                                            if (!qs.isEmpty()) {
                                                for (var d : qs.getDocuments()) {
                                                    Mood m = d.toObject(Mood.class);
                                                    if (m != null) tempList.add(m);
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e("FollowedMoodsFragment", "Error fetching moods: " + e))
                        );
                    }

                    Tasks.whenAllComplete(tasks).addOnSuccessListener(taskList -> {
                        Collections.sort(tempList, new Comparator<Mood>() {
                            @Override
                            public int compare(Mood a, Mood b) {
                                if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                                // Descending
                                return b.getTimestamp().compareTo(a.getTimestamp());
                            }
                        });
                        feedList.clear();
                        feedList.addAll(tempList);
                        adapter.notifyDataSetChanged();
                    });
                })
                .addOnFailureListener(e ->
                        Log.e("FollowedMoodsFragment", "Error loading following list: " + e));
    }
}
