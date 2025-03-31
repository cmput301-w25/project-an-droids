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
 * Fragment for managing and displaying the user's followers and follow requests.
 */
public class FollowersFragment extends Fragment {
    private RecyclerView followersRecyclerView;
    private FollowAdapter followersAdapter;
    private List<String> followersList;
    private List<String> followerUsernames;
    private List<String> followerUserIds;
    private RecyclerView requestsRecyclerView;
    private RequestsAdapter requestsAdapter;
    private List<String> requestsList;
    private List<String> requestUsernames;
    private List<String> requestUserIds;

    private String userId;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    /**
     * Default constructor.
     */
    public FollowersFragment() {}

    /**
     * Creates a new instance of FollowersFragment with a specified user ID.
     *
     * @param userId The ID of the user whose followers are displayed.
     * @return A new instance of FollowersFragment.
     */
    public static FollowersFragment newInstance(String userId) {
        FollowersFragment fragment = new FollowersFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called to create and return the fragment's view hierarchy.
     *
     * @param inflater  The LayoutInflater used to inflate views.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed.
     * @return The root view of the fragment.
     */
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

        // == Followers RecyclerView ==
        followersRecyclerView = view.findViewById(R.id.followersRecyclerView);
        followersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        followersList = new ArrayList<>();
        followerUsernames = new ArrayList<>();
        followerUserIds = new ArrayList<>();
        followersAdapter = new FollowAdapter(
                followerUsernames,
                followerUserIds,
                this::removeFollower,
                "Remove"
        );
        followersRecyclerView.setAdapter(followersAdapter);

        // == Requests RecyclerView ==
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
        listenToFollowersRealtime();

        return view;
    }

    /**
     * Listens for real-time changes on the user's "followers" field.
     */
    private void listenToFollowersRealtime() {
        firestore.collection("Users").document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("FollowersFragment", "Listen failed.", e);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        followersList = (List<String>) snapshot.get("followers");
                        if (followersList == null) {
                            followersList = new ArrayList<>();
                        }
                        fetchFollowerUsernames(followersList);
                    }
                });
    }

    /**
     * Loads the list of followers from Firestore.
     */
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
                .addOnFailureListener(e ->
                        Log.e("FollowersFragment", "Error loading followers: " + e.getMessage()));
    }

    /**
     * Fetches usernames of the provided list of user IDs.
     *
     * @param userIds List of user IDs to fetch usernames for.
     */
    private void fetchFollowerUsernames(List<String> userIds) {
        // Use a Set to ensure no duplicates
        Set<String> seenUserIds = new HashSet<>();
        List<String> tempUsernames = new ArrayList<>();
        List<String> tempUserIds = new ArrayList<>();

        if (userIds.isEmpty()) {
            followerUsernames.clear();
            followerUserIds.clear();
            followersAdapter.updateLists(followerUsernames, followerUserIds);
            return;
        }

        final int[] counter = {0};
        for (String followerId : userIds) {
            // Skip duplicate IDs
            if (!seenUserIds.add(followerId)) {
                counter[0]++;
                continue;
            }

            firestore.collection("Users").document(followerId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                tempUsernames.add(username);
                                tempUserIds.add(followerId);
                            }
                        }
                        counter[0]++;
                        if (counter[0] == seenUserIds.size()) {
                            // Only update once, after all fetches complete
                            followerUsernames.clear();
                            followerUsernames.addAll(tempUsernames);
                            followerUserIds.clear();
                            followerUserIds.addAll(tempUserIds);
                            followersAdapter.updateLists(followerUsernames, followerUserIds);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowersFragment", "Error fetching username: " + e.getMessage());
                        counter[0]++;
                        if (counter[0] == seenUserIds.size()) {
                            followerUsernames.clear();
                            followerUsernames.addAll(tempUsernames);
                            followerUserIds.clear();
                            followerUserIds.addAll(tempUserIds);
                            followersAdapter.updateLists(followerUsernames, followerUserIds);
                        }
                    });
        }
    }

    /**
     * Removes a follower from the user's follower list.
     *
     * @param position The position of the follower in the list.
     */
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

    /**
     * Loads the follow requests from Firestore.
     */
    private void loadRequests() {
        if (currentUser == null) return;

        firestore.collection("Users").document(currentUser.getUid()).get()
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

    /**
     * Fetches usernames of users who sent follow requests.
     *
     * @param userIds List of user IDs who sent requests.
     */
    private void fetchRequestsUsernames(List<String> userIds) {
        requestUsernames.clear();
        requestUserIds.clear();
        if (userIds.isEmpty()) {
            requestsAdapter.updateLists(requestUsernames, requestUserIds);
            return;
        }

        final int[] counter = {0};
        for (String requesterId : userIds) {
            firestore.collection("Users").document(requesterId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                requestUsernames.add(username);
                                requestUserIds.add(requesterId);
                            }
                        }
                        counter[0]++;
                        if (counter[0] == userIds.size()) {
                            requestsAdapter.updateLists(requestUsernames, requestUserIds);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowersFragment", "Error fetching request username: " + e.getMessage());
                        counter[0]++;
                        if (counter[0] == userIds.size()) {
                            requestsAdapter.updateLists(requestUsernames, requestUserIds);
                        }
                    });
        }
    }

    /**
     * Accepts a follow request and updates Firestore accordingly.
     *
     * @param position The position of the request in the list.
     */
    private void acceptRequest(int position) {
        if (currentUser == null || requestUserIds == null
                || position < 0 || position >= requestUserIds.size()) {
            return;
        }

        String requesterId = requestUserIds.get(position);
        String myUid = currentUser.getUid();

        firestore.collection("Users").document(myUid)
                .update("followRequests", FieldValue.arrayRemove(requesterId))
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("Users").document(myUid)
                            .update("followers", FieldValue.arrayUnion(requesterId))
                            .addOnSuccessListener(aVoid2 -> {
                                firestore.collection("Users").document(requesterId)
                                        .update("following", FieldValue.arrayUnion(myUid))
                                        .addOnSuccessListener(aVoid3 -> {
                                            Toast.makeText(getContext(), "Request accepted!", Toast.LENGTH_SHORT).show();
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

    /**
     * Rejects a follow request and removes it from Firestore.
     *
     * @param position The position of the request in the list.
     */
    private void rejectRequest(int position) {
        if (currentUser == null || requestUserIds == null
                || position < 0 || position >= requestUserIds.size()) {
            return;
        }

        String requesterId = requestUserIds.get(position);

        firestore.collection("Users").document(currentUser.getUid())
                .update("followRequests", FieldValue.arrayRemove(requesterId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                    loadRequests();
                })
                .addOnFailureListener(e ->
                        Log.e("FollowersFragment", "Error rejecting request: " + e.getMessage()));
    }
}