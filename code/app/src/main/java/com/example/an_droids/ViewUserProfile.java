package com.example.an_droids;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ViewUserProfile extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String searchedUserId;    // Firestore doc ID of the user we’re viewing
    private String searchedUsername;  // The username we got from the Intent

    private Button followButton;

    private MoodProvider moodProvider;
    private ArrayList<Mood> moodArrayList;
    private MoodArrayAdapter moodArrayAdapter;
    private ListView moodListView;

    private TextView usernameTextView;

    // Possible “follow states”
    private enum FollowState {
        FOLLOW,        // Not following, no request -> show "Follow"
        REQUESTED,     // Request pending -> show "Requested" (disabled)
        UNFOLLOW,      // Already a follower -> show "Unfollow"
        OWN_PROFILE    // This is your own profile -> hide button
    }

    private FollowState currentState = FollowState.FOLLOW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_user_profile);

        // Init Firebase
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        moodListView = findViewById(R.id.moodListView);

        // Get the searched username from Intent
        searchedUsername = getIntent().getStringExtra("username");
        if (searchedUsername == null || searchedUsername.isEmpty()) {
            Toast.makeText(this, "Error: No user found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements
        usernameTextView = findViewById(R.id.usernameTextView);
        followButton = findViewById(R.id.followButton);

        // Display that username
        usernameTextView.setText(searchedUsername);

        // Query Firestore to find doc with this username, load details
        loadUserProfile();

        // Button click logic will be set after we know the current follow state
        followButton.setOnClickListener(v -> onFollowButtonClicked());
    }

    private void loadUserProfile() {
        // Find the Firestore doc with this username
        firestore.collection("Users")
                .whereEqualTo("username", searchedUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        searchedUserId = document.getId(); // The doc ID of the searched user

                        // Load public moods after fetching the user
                        loadPublicMoods(searchedUserId);

                        // If the user somehow tries to view their own profile
                        if (currentUser != null && searchedUserId.equals(currentUser.getUid())) {
                            currentState = FollowState.OWN_PROFILE;
                            updateFollowButtonUI();
                            return;
                        }

                        // Now check if there's a pending request or if we’re actually following
                        checkRelationshipState();

                    } else {
                        Toast.makeText(ViewUserProfile.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewUserProfile.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Determines whether the current user:
     * - is already in the searchedUser's "followers" (=> UNFOLLOW)
     * - is in the searchedUser's "followRequests" (=> REQUESTED)
     * - otherwise => FOLLOW
     */
    private void checkRelationshipState() {
        if (searchedUserId == null) return;

        firestore.collection("Users").document(searchedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 1) Check if current user is in "followers"
                        List<String> followers = (List<String>) documentSnapshot.get("followers");
                        if (followers != null && currentUser != null && followers.contains(currentUser.getUid())) {
                            currentState = FollowState.UNFOLLOW;
                            updateFollowButtonUI();
                            return;
                        }

                        // 2) Check if current user is in "followRequests"
                        List<String> requests = (List<String>) documentSnapshot.get("followRequests");
                        if (requests != null && currentUser != null && requests.contains(currentUser.getUid())) {
                            currentState = FollowState.REQUESTED;
                            updateFollowButtonUI();
                            return;
                        }

                        // 3) Otherwise, user is not following & no request -> SHOW "Follow"
                        currentState = FollowState.FOLLOW;
                        updateFollowButtonUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewUserProfile", "Error checking relationship: " + e.getMessage());
                });
    }

    /**
     * Handles the button click based on currentState.
     */
    private void onFollowButtonClicked() {
        switch (currentState) {
            case FOLLOW:
                // Send a follow request
                sendFollowRequest();
                break;
            case REQUESTED:
                // Already requested – maybe show a toast
                Toast.makeText(this, "Your follow request is pending acceptance.", Toast.LENGTH_SHORT).show();
                break;
            case UNFOLLOW:
                // If we've already been accepted, let's allow direct "unfollow" logic
                unfollowUser();
                break;
            case OWN_PROFILE:
                // Should never happen if we hide the button, but just in case:
                Toast.makeText(this, "This is your own profile", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Sends a follow request by adding currentUser’s UID to the searchedUser’s "followRequests".
     */
    private void sendFollowRequest() {
        if (currentUser == null || searchedUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = currentUser.getUid();

        firestore.collection("Users").document(searchedUserId)
                .update("followRequests", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ViewUserProfile.this, "Follow request sent", Toast.LENGTH_SHORT).show();
                    currentState = FollowState.REQUESTED;
                    updateFollowButtonUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewUserProfile", "Error sending request: " + e.getMessage());
                    Toast.makeText(ViewUserProfile.this, "Failed to send request", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Unfollow: since we’re already in the “followers” array, we remove ourselves
     * from the searchedUser's "followers" array, and also remove searchedUser from
     * our own "following" array. This is the opposite of an accepted follow relationship.
     */
    private void unfollowUser() {
        if (currentUser == null || searchedUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = currentUser.getUid();

        // Remove me from their "followers"
        firestore.collection("Users").document(searchedUserId)
                .update("followers", FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    // Remove them from my "following"
                    firestore.collection("Users").document(currentUserId)
                            .update("following", FieldValue.arrayRemove(searchedUserId))
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(ViewUserProfile.this, "Unfollowed successfully", Toast.LENGTH_SHORT).show();
                                currentState = FollowState.FOLLOW;
                                updateFollowButtonUI();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ViewUserProfile", "Error removing from following: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewUserProfile", "Error removing from followers: " + e.getMessage());
                });
    }

    /**
     * Loads the public moods of the searched user.
     */
    private void loadPublicMoods(String userId) {
        moodProvider = new MoodProvider(FirebaseFirestore.getInstance(), userId);
        moodArrayList = moodProvider.getMoods();  // Get reference to moods list
        moodArrayAdapter = new MoodArrayAdapter(this, moodArrayList);
        moodListView.setAdapter(moodArrayAdapter);

        moodProvider.listenForUpdates(new MoodProvider.DataStatus() {
            @Override
            public void onDataUpdated() {
                moodArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ViewUserProfile.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        moodProvider.loadPublicMoods(searchedUserId);  // Now just calls the method without a callback
    }

    /**
     * Updates the button’s text (and optionally enabled/disabled state) based on currentState.
     */
    private void updateFollowButtonUI() {
        switch (currentState) {
            case OWN_PROFILE:
                followButton.setVisibility(View.GONE);
                break;
            case FOLLOW:
                followButton.setVisibility(View.VISIBLE);
                followButton.setText("Follow");
                followButton.setEnabled(true);
                break;
            case REQUESTED:
                followButton.setVisibility(View.VISIBLE);
                followButton.setText("Requested");
                followButton.setEnabled(false);
                break;
            case UNFOLLOW:
                followButton.setVisibility(View.VISIBLE);
                followButton.setText("Unfollow");
                followButton.setEnabled(true);
                break;
        }
    }
}
