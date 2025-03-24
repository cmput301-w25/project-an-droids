package com.example.an_droids;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class FollowersFragment extends Fragment {
    private RecyclerView recyclerView;
    private FollowAdapter followAdapter;
    private List<String> followersList;
    private List<String> followerUsernames; // Store usernames instead of user IDs
    private List<String> followerUserIds; // Store user IDs for unfollow functionality
    private String userId;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    public FollowersFragment(String userId) {
        this.userId = userId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_followers, container, false);

        recyclerView = view.findViewById(R.id.followersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        followersList = new ArrayList<>();
        followerUsernames = new ArrayList<>(); // Initialize the list of usernames
        followerUserIds = new ArrayList<>(); // Initialize the list of user IDs
        followAdapter = new FollowAdapter(followerUsernames, followerUserIds, this::removeFollower); // Pass the remove callback
        recyclerView.setAdapter(followAdapter);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        loadFollowers();

        return view;
    }

    private void loadFollowers() {
        firestore.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        followersList = (List<String>) documentSnapshot.get("followers");
                        if (followersList == null) {
                            followersList = new ArrayList<>(); // Ensure followersList is never null
                        }
                        fetchUsernames(followersList); // Fetch usernames for the user IDs
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowersFragment", "Error loading followers: " + e.getMessage());
                });
    }

    private void fetchUsernames(List<String> userIds) {
        followerUsernames.clear(); // Clear the list before adding new usernames
        followerUserIds.clear(); // Clear the list before adding new user IDs
        for (String userId : userIds) {
            firestore.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                followerUsernames.add(username); // Add the username to the list
                                followerUserIds.add(userId); // Add the user ID to the list
                                followAdapter.updateLists(followerUsernames, followerUserIds); // Update the adapter
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowersFragment", "Error fetching username: " + e.getMessage());
                    });
        }
    }

    private void removeFollower(int position) {
        if (currentUser == null || followerUserIds == null || position < 0 || position >= followerUserIds.size()) {
            return;
        }

        String userIdToRemove = followerUserIds.get(position);

        // Remove the user from the current user's followers list
        firestore.collection("Users").document(currentUser.getUid())
                .update("followers", FieldValue.arrayRemove(userIdToRemove))
                .addOnSuccessListener(aVoid -> {
                    // Remove the current user from the other user's following list
                    firestore.collection("Users").document(userIdToRemove)
                            .update("following", FieldValue.arrayRemove(currentUser.getUid()))
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(getContext(), "Follower removed successfully", Toast.LENGTH_SHORT).show();
                                loadFollowers(); // Refresh the list
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FollowersFragment", "Error updating following: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowersFragment", "Error updating followers: " + e.getMessage());
                });
    }
}