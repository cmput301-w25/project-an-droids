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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewUserProfile extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String searchedUserId;
    private String searchedUsername;
    private Button followButton;
    private MoodProvider moodProvider;
    private ArrayList<Mood> moodArrayList;
    private MoodArrayAdapter moodArrayAdapter;
    private ListView moodListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_user_profile);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        moodListView = findViewById(R.id.moodListView);


        // Get the searched user's username from the intent
        searchedUsername = getIntent().getStringExtra("username");
        if (searchedUsername == null || searchedUsername.isEmpty()) {
            Toast.makeText(this, "Error: No user found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        TextView usernameTextView = findViewById(R.id.usernameTextView);
        followButton = findViewById(R.id.followButton);

        // Display the searched user's username
        usernameTextView.setText(searchedUsername);

        // Load the searched user's profile
        loadUserProfile();

        // Set up the Follow Button
        followButton.setOnClickListener(v -> followUser());
    }

    private void loadUserProfile() {
        firestore.collection("Users")
                .whereEqualTo("username", searchedUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        searchedUserId = document.getId(); // Get the searched user's ID

                        // Load public moods after fetching the user
                        loadPublicMoods(searchedUserId);

                        // Check if the current user is already following the searched user
                        checkIfFollowing();
                    } else {
                        Toast.makeText(ViewUserProfile.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewUserProfile.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }


    private void checkIfFollowing() {
        if (currentUser == null || searchedUserId == null) {
            return;
        }

        firestore.collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followingList = (List<String>) documentSnapshot.get("following");
                        if (followingList != null && followingList.contains(searchedUserId)) {
                            // The current user is already following the searched user
                            followButton.setText("Unfollow");
                        } else {
                            // The current user is not following the searched user
                            followButton.setText("Follow");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewUserProfile", "Error checking follow status: " + e.getMessage());
                });
    }

    private void followUser() {
        if (currentUser == null || searchedUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Check if the current user is already following the searched user
        firestore.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followingList = (List<String>) documentSnapshot.get("following");
                        if (followingList != null && followingList.contains(searchedUserId)) {
                            // Unfollow the user
                            unfollowUser(currentUserId, searchedUserId);
                        } else {
                            // Follow the user
                            followUser(currentUserId, searchedUserId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewUserProfile", "Error checking follow status: " + e.getMessage());
                });
    }

    private void followUser(String currentUserId, String searchedUserId) {
        // Add the searched user to the current user's following list
        firestore.collection("Users").document(currentUserId)
                .update("following", FieldValue.arrayUnion(searchedUserId))
                .addOnSuccessListener(aVoid -> {
                    // Add the current user to the searched user's followers list
                    firestore.collection("Users").document(searchedUserId)
                            .update("followers", FieldValue.arrayUnion(currentUserId))
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(ViewUserProfile.this, "Followed successfully", Toast.LENGTH_SHORT).show();
                                followButton.setText("Unfollow");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ViewUserProfile", "Error updating followers: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewUserProfile", "Error updating following: " + e.getMessage());
                });
    }

    private void unfollowUser(String currentUserId, String searchedUserId) {
        // Remove the searched user from the current user's following list
        firestore.collection("Users").document(currentUserId)
                .update("following", FieldValue.arrayRemove(searchedUserId))
                .addOnSuccessListener(aVoid -> {
                    // Remove the current user from the searched user's followers list
                    firestore.collection("Users").document(searchedUserId)
                            .update("followers", FieldValue.arrayRemove(currentUserId))
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(ViewUserProfile.this, "Unfollowed successfully", Toast.LENGTH_SHORT).show();
                                followButton.setText("Follow");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ViewUserProfile", "Error updating followers: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewUserProfile", "Error updating following: " + e.getMessage());
                });
    }

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



}