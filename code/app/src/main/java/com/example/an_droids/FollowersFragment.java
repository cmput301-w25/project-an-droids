package com.example.an_droids;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class FollowersFragment extends Fragment {
    private RecyclerView recyclerView;
    private FollowAdapter followAdapter;
    private List<String> followersList;
    private List<String> followerUsernames;
    private List<String> followerUserIds;
    private String userId;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    public FollowersFragment() {}

    public static FollowersFragment newInstance(String userId) {
        FollowersFragment fragment = new FollowersFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_followers, container, false);

        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
        }

        recyclerView = view.findViewById(R.id.followersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        followersList = new ArrayList<>();
        followerUsernames = new ArrayList<>();
        followerUserIds = new ArrayList<>();
        followAdapter = new FollowAdapter(followerUsernames, followerUserIds, this::removeFollower);
        recyclerView.setAdapter(followAdapter);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        listenToFollowersRealtime();

        return view;
    }

    private void listenToFollowersRealtime() {
        firestore.collection("Users").document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("FollowersFragment", "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        followersList = (List<String>) snapshot.get("followers");
                        if (followersList == null) followersList = new ArrayList<>();
                        fetchUsernames(followersList);
                    }
                });
    }

    private void fetchUsernames(List<String> userIds) {
        followerUsernames.clear();
        followerUserIds.clear();
        for (String userId : userIds) {
            firestore.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                followerUsernames.add(username);
                                followerUserIds.add(userId);
                                followAdapter.updateLists(followerUsernames, followerUserIds);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FollowersFragment", "Error fetching username: " + e.getMessage()));
        }
    }

    private void removeFollower(int position) {
        if (currentUser == null || followerUserIds == null || position < 0 || position >= followerUserIds.size()) return;

        String userIdToRemove = followerUserIds.get(position);

        firestore.collection("Users").document(currentUser.getUid())
                .update("followers", FieldValue.arrayRemove(userIdToRemove))
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("Users").document(userIdToRemove)
                            .update("following", FieldValue.arrayRemove(currentUser.getUid()))
                            .addOnSuccessListener(aVoid1 ->
                                    Toast.makeText(getContext(), "Follower removed", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Log.e("FollowersFragment", "Error updating following: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e("FollowersFragment", "Error updating followers: " + e.getMessage()));
    }
}