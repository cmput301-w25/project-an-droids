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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FollowingFragment extends Fragment {
    private RecyclerView recyclerView;
    private FollowAdapter followAdapter;
    private List<String> followingList;
    private List<String> followingUsernames;
    private List<String> followingUserIds;
    private String userId;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    public FollowingFragment() {}

    public static FollowingFragment newInstance(String userId) {
        FollowingFragment fragment = new FollowingFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_following, container, false);

        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
        }

        recyclerView = view.findViewById(R.id.followingRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        followingList = new ArrayList<>();
        followingUsernames = new ArrayList<>();
        followingUserIds = new ArrayList<>();
        followAdapter = new FollowAdapter(followingUsernames, followingUserIds, this::unfollowUser);
        recyclerView.setAdapter(followAdapter);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        listenToFollowingRealtime();

        return view;
    }

    private void listenToFollowingRealtime() {
        firestore.collection("Users").document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("FollowingFragment", "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        followingList = (List<String>) snapshot.get("following");
                        if (followingList == null) followingList = new ArrayList<>();
                        fetchUsernames(followingList);
                    }
                });
    }

    private void fetchUsernames(List<String> userIds) {
        Set<String> seenUserIds = new HashSet<>();
        List<String> tempUsernames = new ArrayList<>();
        List<String> tempUserIds = new ArrayList<>();

        if (userIds.isEmpty()) {
            followingUsernames.clear();
            followingUserIds.clear();
            followAdapter.updateLists(followingUsernames, followingUserIds);
            return;
        }

        final int[] counter = {0};
        for (String id : userIds) {
            if (!seenUserIds.add(id)) {
                counter[0]++;
                continue;
            }

            firestore.collection("Users").document(id).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                tempUsernames.add(username);
                                tempUserIds.add(id);
                            }
                        }
                        counter[0]++;
                        if (counter[0] == seenUserIds.size()) {
                            followingUsernames.clear();
                            followingUsernames.addAll(tempUsernames);
                            followingUserIds.clear();
                            followingUserIds.addAll(tempUserIds);
                            followAdapter.updateLists(followingUsernames, followingUserIds);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowingFragment", "Error fetching username: " + e.getMessage());
                        counter[0]++;
                        if (counter[0] == seenUserIds.size()) {
                            followingUsernames.clear();
                            followingUsernames.addAll(tempUsernames);
                            followingUserIds.clear();
                            followingUserIds.addAll(tempUserIds);
                            followAdapter.updateLists(followingUsernames, followingUserIds);
                        }
                    });
        }
    }
    private void sendFollowRequest(String userIdToFollow) {
        if (currentUser == null) return;
        String myUid = currentUser.getUid();

        // Add me (the current user) to the other user's "followRequests" array
        firestore.collection("Users").document(userIdToFollow)
                .update("followRequests", FieldValue.arrayUnion(myUid))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Follow request sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Log.e("FollowingFragment", "Error sending follow request: " + e.getMessage()));
    }


    private void unfollowUser(int position) {
        if (currentUser == null || followingUserIds == null || position < 0 || position >= followingUserIds.size()) return;

        String userIdToUnfollow = followingUserIds.get(position);

        firestore.collection("Users").document(currentUser.getUid())
                .update("following", FieldValue.arrayRemove(userIdToUnfollow))
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("Users").document(userIdToUnfollow)
                            .update("followers", FieldValue.arrayRemove(currentUser.getUid()))
                            .addOnSuccessListener(aVoid1 ->
                                    Toast.makeText(getContext(), "Unfollowed", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Log.e("FollowingFragment", "Error updating followers: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e("FollowingFragment", "Error updating following: " + e.getMessage()));
    }
}