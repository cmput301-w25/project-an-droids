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
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ViewUserProfile extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String searchedUserId;
    private String searchedUsername;

    private Button followButton;
    private Button viewMoodsButton;
    private TextView usernameTextView;

    private ArrayList<Mood> moodList;
    private MoodArrayAdapter moodArrayAdapter;
    private ListView moodListView;

    private MoodProvider moodProvider;

    private enum FollowState {
        FOLLOW,
        REQUESTED,
        UNFOLLOW,
        OWN_PROFILE
    }

    private FollowState currentState = FollowState.FOLLOW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_user_profile);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        usernameTextView = findViewById(R.id.usernameTextView);
        followButton = findViewById(R.id.followButton);
        viewMoodsButton = findViewById(R.id.viewMoodsButton);
        moodListView = findViewById(R.id.moodListView);

        searchedUsername = getIntent().getStringExtra("username");

        if (searchedUsername == null || searchedUsername.isEmpty()) {
            Toast.makeText(this, "Error: No user found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        usernameTextView.setText(searchedUsername);

        moodList = new ArrayList<>();
        moodArrayAdapter = new MoodArrayAdapter(this, moodList);
        moodListView.setAdapter(moodArrayAdapter);

        loadUserProfile();

        followButton.setOnClickListener(v -> onFollowButtonClicked());
        viewMoodsButton.setOnClickListener(v -> loadThreeLatestMoods());
    }

    private void loadUserProfile() {
        firestore.collection("Users")
                .whereEqualTo("username", searchedUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        searchedUserId = document.getId();

                        if (currentUser != null && searchedUserId.equals(currentUser.getUid())) {
                            currentState = FollowState.OWN_PROFILE;
                            updateFollowButtonUI();
                            return;
                        }

                        loadPublicMoods(searchedUserId);
                        checkRelationshipState();
                    } else {
                        Toast.makeText(ViewUserProfile.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ViewUserProfile.this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    private void checkRelationshipState() {
        if (searchedUserId == null) return;
        firestore.collection("Users").document(searchedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followers = (List<String>) documentSnapshot.get("followers");
                        List<String> requests = (List<String>) documentSnapshot.get("followRequests");

                        String currentUid = currentUser.getUid();

                        if (followers != null && followers.contains(currentUid)) {
                            currentState = FollowState.UNFOLLOW;
                        } else if (requests != null && requests.contains(currentUid)) {
                            currentState = FollowState.REQUESTED;
                        } else {
                            currentState = FollowState.FOLLOW;
                        }
                        updateFollowButtonUI();
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("ViewUserProfile", "Error checking relationship: " + e.getMessage())
                );
    }

    private void onFollowButtonClicked() {
        switch (currentState) {
            case FOLLOW:
                sendFollowRequest();
                break;
            case REQUESTED:
                Toast.makeText(this, "Your follow request is pending acceptance.", Toast.LENGTH_SHORT).show();
                break;
            case UNFOLLOW:
                unfollowUser();
                break;
            case OWN_PROFILE:
                Toast.makeText(this, "This is your own profile", Toast.LENGTH_SHORT).show();
                break;
        }
    }

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

    private void unfollowUser() {
        if (currentUser == null || searchedUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        firestore.collection("Users").document(searchedUserId)
                .update("followers", FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("Users").document(currentUserId)
                            .update("following", FieldValue.arrayRemove(searchedUserId))
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(ViewUserProfile.this, "Unfollowed successfully", Toast.LENGTH_SHORT).show();
                                currentState = FollowState.FOLLOW;
                                updateFollowButtonUI();
                            })
                            .addOnFailureListener(e ->
                                    Log.e("ViewUserProfile", "Error removing from following: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e ->
                        Log.e("ViewUserProfile", "Error removing from followers: " + e.getMessage())
                );
    }

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

    private void loadPublicMoods(String userId) {
        moodProvider = new MoodProvider(FirebaseFirestore.getInstance(), userId);
        moodList = moodProvider.getMoods();
        moodArrayAdapter = new MoodArrayAdapter(this, moodList);
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

        moodProvider.loadPublicMoods(searchedUserId);
    }

    private void loadThreeLatestMoods() {
        if (searchedUserId == null) return;

        firestore.collection("Users")
                .document(searchedUserId)
                .collection("Moods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    moodList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Mood mood = doc.toObject(Mood.class);
                        if (mood != null) {
                            moodList.add(mood);
                        }
                    }
                    moodArrayAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("ViewUserProfile", "Error loading moods: " + e.getMessage())
                );
    }
}