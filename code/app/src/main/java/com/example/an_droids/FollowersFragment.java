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

public class FollowersFragment extends Fragment {
    private RecyclerView followersRecyclerView;
    private FollowAdapter followersAdapter;
    private List<String> followersList;
    private List<String> followerUsernames;
    private List<String> followerUserIds;

    // == Requests Section ==
    private RecyclerView requestsRecyclerView;
    private RequestsAdapter requestsAdapter;
    private List<String> requestsList;
    private List<String> requestUsernames;
    private List<String> requestUserIds;

    private String userId;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    public FollowersFragment() {
    }

    public static FollowersFragment newInstance(String userId) {
        FollowersFragment fragment = new FollowersFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_followers, container, false);

        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
        }

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        followersRecyclerView = view.findViewById(R.id.followersRecyclerView);
        followersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        followersList = new ArrayList<>();
        followerUsernames = new ArrayList<>();
        followerUserIds = new ArrayList<>();
        followersAdapter = new FollowAdapter(
                followerUsernames,
                followerUserIds,
                this::removeFollower
        );
        followersRecyclerView.setAdapter(followersAdapter);
        requestsRecyclerView = view.findViewById(R.id.requestsRecyclerView);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsList = new ArrayList<>();
        requestUsernames = new ArrayList<>();
        requestUserIds = new ArrayList<>();
        requestsAdapter = new RequestsAdapter(
                requestUsernames,
                requestUserIds,
                this::acceptRequest,
                this::rejectRequest
        );
        requestsRecyclerView.setAdapter(requestsAdapter);
        loadFollowers();
        loadRequests();

        return view;
    }
    private void loadFollowers() {
        if (userId == null) return;

        firestore.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        followersList = (List<String>) documentSnapshot.get("followers");
                        if (followersList == null) {
                            followersList = new ArrayList<>();
                        }
                        fetchFollowerUsernames(followersList);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowersFragment", "Error loading followers: " + e.getMessage());
                });
    }

    private void fetchFollowerUsernames(List<String> userIds) {
        followerUsernames.clear();
        followerUserIds.clear();

        // If the list is empty, we can update the adapter right away
        if (userIds.isEmpty()) {
            followersAdapter.updateLists(followerUsernames, followerUserIds);
            return;
        }

        for (String followerId : userIds) {
            firestore.collection("Users").document(followerId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                followerUsernames.add(username);
                                followerUserIds.add(followerId);
                                // Update adapter each time a new user is fetched
                                followersAdapter.updateLists(followerUsernames, followerUserIds);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowersFragment", "Error fetching follower username: " + e.getMessage());
                    });
        }
    }

    private void removeFollower(int position) {
        if (currentUser == null || followerUserIds == null
                || position < 0 || position >= followerUserIds.size()) {
            return;
        }

        String userIdToRemove = followerUserIds.get(position);

        // 1) Remove from my "followers"
        firestore.collection("Users").document(currentUser.getUid())
                .update("followers", FieldValue.arrayRemove(userIdToRemove))
                .addOnSuccessListener(aVoid -> {
                    // 2) Remove me from their "following"
                    firestore.collection("Users").document(userIdToRemove)
                            .update("following", FieldValue.arrayRemove(currentUser.getUid()))
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(getContext(), "Follower removed", Toast.LENGTH_SHORT).show();
                                loadFollowers(); // Refresh
                            })
                            .addOnFailureListener(e ->
                                    Log.e("FollowersFragment", "Error updating their following: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        Log.e("FollowersFragment", "Error removing from my followers: " + e.getMessage()));
    }

    private void loadRequests() {
        if (currentUser == null) return;
        String currentUserId = currentUser.getUid();

        firestore.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        requestsList = (List<String>) documentSnapshot.get("followRequests");
                        if (requestsList == null) {
                            requestsList = new ArrayList<>();
                        }
                        fetchRequestsUsernames(requestsList);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowersFragment", "Error loading requests: " + e.getMessage());
                });
    }

    private void fetchRequestsUsernames(List<String> userIds) {
        requestUsernames.clear();
        requestUserIds.clear();
        if (userIds.isEmpty()) {
            requestsAdapter.updateLists(requestUsernames, requestUserIds);
            return;
        }

        for (String requesterId : userIds) {
            firestore.collection("Users").document(requesterId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                requestUsernames.add(username);
                                requestUserIds.add(requesterId);
                                requestsAdapter.updateLists(requestUsernames, requestUserIds);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowersFragment", "Error fetching request username: " + e.getMessage());
                    });
        }
    }
    private void acceptRequest(int position) {
        if (currentUser == null || requestUserIds == null
                || position < 0 || position >= requestUserIds.size()) {
            return;
        }

        String requesterId = requestUserIds.get(position);
        String myUid = currentUser.getUid();

        // 1) Remove them from my "followRequests"
        firestore.collection("Users").document(myUid)
                .update("followRequests", FieldValue.arrayRemove(requesterId))
                .addOnSuccessListener(aVoid -> {
                    // 2) Add them to my "followers"
                    firestore.collection("Users").document(myUid)
                            .update("followers", FieldValue.arrayUnion(requesterId))
                            .addOnSuccessListener(aVoid2 -> {
                                // 3) Add me to their "following"
                                firestore.collection("Users").document(requesterId)
                                        .update("following", FieldValue.arrayUnion(myUid))
                                        .addOnSuccessListener(aVoid3 -> {
                                            Toast.makeText(getContext(),
                                                    "Request accepted!",
                                                    Toast.LENGTH_SHORT).show();
                                            // Reload so the request disappears immediately
                                            loadRequests();
                                            loadFollowers();
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e("FollowersFragment", "Error adding me to their following: " + e.getMessage()));
                            })
                            .addOnFailureListener(e ->
                                    Log.e("FollowersFragment", "Error adding them to my followers: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        Log.e("FollowersFragment", "Error removing from followRequests: " + e.getMessage()));
    }

    private void rejectRequest(int position) {
        if (currentUser == null || requestUserIds == null
                || position < 0 || position >= requestUserIds.size()) {
            return;
        }

        String requesterId = requestUserIds.get(position);

        // Simply remove from my "followRequests"
        firestore.collection("Users").document(currentUser.getUid())
                .update("followRequests", FieldValue.arrayRemove(requesterId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                    // Immediately reload so the request disappears from the UI
                    loadRequests();
                })
                .addOnFailureListener(e ->
                        Log.e("FollowersFragment", "Error rejecting request: " + e.getMessage()));
    }
}
