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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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

    public FollowingFragment() {
        // Required empty constructor
    }

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

        loadFollowing();

        return view;
    }

    private void loadFollowing() {
        firestore.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        followingList = (List<String>) documentSnapshot.get("following");
                        if (followingList == null) {
                            followingList = new ArrayList<>();
                        }
                        fetchUsernames(followingList);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowingFragment", "Error loading following: " + e.getMessage());
                });
    }

    private void fetchUsernames(List<String> userIds) {
        followingUsernames.clear();
        followingUserIds.clear();
        for (String userId : userIds) {
            firestore.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                followingUsernames.add(username);
                                followingUserIds.add(userId);
                                followAdapter.updateLists(followingUsernames, followingUserIds);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowingFragment", "Error fetching username: " + e.getMessage());
                    });
        }
    }

    private void unfollowUser(int position) {
        if (currentUser == null || followingUserIds == null || position < 0 || position >= followingUserIds.size()) return;

        String userIdToUnfollow = followingUserIds.get(position);

        firestore.collection("Users").document(currentUser.getUid())
                .update("following", FieldValue.arrayRemove(userIdToUnfollow))
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("Users").document(userIdToUnfollow)
                            .update("followers", FieldValue.arrayRemove(currentUser.getUid()))
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(getContext(), "Unfollowed", Toast.LENGTH_SHORT).show();
                                loadFollowing();
                            })
                            .addOnFailureListener(e -> Log.e("FollowingFragment", "Error updating followers: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e("FollowingFragment", "Error updating following: " + e.getMessage()));
    }
}