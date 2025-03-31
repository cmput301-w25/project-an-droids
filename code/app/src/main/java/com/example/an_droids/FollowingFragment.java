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

/**
 * A Fragment that displays the list of users the current user is following.
 * Provides real-time updates and allows users to unfollow others.
 */
public class FollowingFragment extends Fragment {
    /** RecyclerView to display the list of followed users. */
    private RecyclerView recyclerView;

    /** Adapter for handling the list of followed users. */
    private FollowAdapter followAdapter;

    /** List of user IDs the current user is following. */
    private List<String> followingList;

    /** List of usernames corresponding to the followed users. */
    private List<String> followingUsernames;

    /** List of user IDs corresponding to the followed users. */
    private List<String> followingUserIds;

    /** User ID of the current user. */
    private String userId;

    /** Firebase Firestore instance for database operations. */
    private FirebaseFirestore firestore;

    /** Firebase Authentication instance for user authentication. */
    private FirebaseAuth mAuth;

    /** Currently authenticated Firebase user. */
    private FirebaseUser currentUser;

    /**
     * Default constructor for FollowingFragment.
     */
    public FollowingFragment() {}

    /**
     * Creates a new instance of FollowingFragment with a specified user ID.
     *
     * @param userId The ID of the user whose following list is being displayed.
     * @return A new instance of FollowingFragment.
     */
    public static FollowingFragment newInstance(String userId) {
        FollowingFragment fragment = new FollowingFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Inflates the fragment layout and initializes UI components.
     *
     * @param inflater LayoutInflater to inflate the layout.
     * @param container Parent view that the fragment's UI is attached to.
     * @param savedInstanceState Saved instance state bundle.
     * @return The root view of the fragment.
     */
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
        followAdapter = new FollowAdapter(
                followingUsernames,
                followingUserIds,
                this::unfollowUser,
                "Unfollow"
        );
        recyclerView.setAdapter(followAdapter);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        listenToFollowingRealtime();

        return view;
    }

    /**
     * Listens for real-time updates to the list of followed users.
     */
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

    /**
     * Fetches usernames for the list of followed user IDs.
     *
     * @param userIds List of user IDs to fetch usernames for.
     */
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
    /**
     * Sends a follow request to another user.
     *
     * @param userIdToFollow The ID of the user to follow.
     */
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


    /**
     * Unfollows a user at the specified position in the list.
     *
     * @param position The index of the user in the following list.
     */
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