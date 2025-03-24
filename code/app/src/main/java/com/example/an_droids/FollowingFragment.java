package com.example.an_droids;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class FollowingFragment extends Fragment {
    private RecyclerView recyclerView;
    private FollowAdapter followAdapter;
    private List<String> followingList;
    private List<String> followingUsernames;
    private List<String> followingUserIds; // Store user IDs for unfollow functionality
    private String userId;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    public FollowingFragment(String userId) {
        this.userId = userId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_following, container, false);

        recyclerView = view.findViewById(R.id.followingRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        followingList = new ArrayList<>();
        followingUsernames = new ArrayList<>();
        followingUserIds = new ArrayList<>(); // Initialize the list of user IDs
        followAdapter = new FollowAdapter(followingUsernames, followingUserIds, this::unfollowUser); // Pass the unfollow callback
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
                            followingList = new ArrayList<>(); // Ensure followingList is never null
                        }
                        fetchUsernames(followingList); // Fetch usernames for the user IDs
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowingFragment", "Error loading following: " + e.getMessage());
                });
    }

    private void fetchUsernames(List<String> userIds) {
        followingUsernames.clear(); // Clear the list before adding new usernames
        followingUserIds.clear(); // Clear the list before adding new user IDs
        for (String userId : userIds) {
            firestore.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                followingUsernames.add(username); // Add the username to the list
                                followingUserIds.add(userId); // Add the user ID to the list
                                followAdapter.updateLists(followingUsernames, followingUserIds); // Update the adapter
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowingFragment", "Error fetching username: " + e.getMessage());
                    });
        }
    }

    private void unfollowUser(int position) {
        if (currentUser == null || followingUserIds == null || position < 0 || position >= followingUserIds.size()) {
            return;
        }

        String userIdToUnfollow = followingUserIds.get(position);

        // Remove the user from the current user's following list
        firestore.collection("Users").document(currentUser.getUid())
                .update("following", FieldValue.arrayRemove(userIdToUnfollow))
                .addOnSuccessListener(aVoid -> {
                    // Remove the current user from the other user's followers list
                    firestore.collection("Users").document(userIdToUnfollow)
                            .update("followers", FieldValue.arrayRemove(currentUser.getUid()))
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(getContext(), "Unfollowed successfully", Toast.LENGTH_SHORT).show();
                                loadFollowing(); // Refresh the list
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FollowingFragment", "Error updating followers: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowingFragment", "Error updating following: " + e.getMessage());
                });
    }
}