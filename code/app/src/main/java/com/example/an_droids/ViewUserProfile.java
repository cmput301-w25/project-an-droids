package com.example.an_droids;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a user's profile, including their profile image, follower and following count,
 * and their latest moods. Allows users to follow or unfollow other users, and view the list of followers/following.
 */
public class ViewUserProfile extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String searchedUserId;
    private String searchedUsername;

    private ImageView profileImageView, backButton;
    private Button followButton, viewMoodsButton;
    private TextView usernameTextView, followersText, followingText;
    private ListView moodListView;

    private ArrayList<Mood> moodList;
    private MoodArrayAdapter moodArrayAdapter;

    /**
     * Enum representing the possible follow states of the current user.
     */
    private enum FollowState {
        FOLLOW, REQUESTED, UNFOLLOW, OWN_PROFILE
    }

    private FollowState currentState = FollowState.FOLLOW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_user_profile);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        profileImageView = findViewById(R.id.profileImageView);
        backButton = findViewById(R.id.backButton);
        usernameTextView = findViewById(R.id.usernameTextView);
        followersText = findViewById(R.id.followersText);
        followingText = findViewById(R.id.followingText);
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
        moodArrayAdapter = new MoodArrayAdapter(this, moodList, searchedUserId);
        moodListView.setAdapter(moodArrayAdapter);

        loadUserProfile();

        followButton.setOnClickListener(v -> onFollowButtonClicked());
        viewMoodsButton.setOnClickListener(v -> loadThreeLatestMoods());
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Loads the user's profile data from Firestore.
     * Retrieves user data like username, profile picture, and follow state.
     */
    private void loadUserProfile() {
        firestore.collection("Users")
                .whereEqualTo("username", searchedUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        searchedUserId = document.getId();

                        Users user = document.toObject(Users.class);
                        if (user != null) {
                            loadProfilePicture(user.getProfileBitmap());
                        }

                        if (currentUser != null && searchedUserId.equals(currentUser.getUid())) {
                            currentState = FollowState.OWN_PROFILE;
                            updateFollowButtonUI();
                            loadFollowStats(); // still show follower/following count
                        } else {
                            checkRelationshipState();
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads the user's profile picture into the ImageView.
     * If no profile picture is available, a default image is shown.
     *
     * @param bitmap The profile picture bitmap.
     */
    private void loadProfilePicture(Bitmap bitmap) {
        if (bitmap != null) {
            profileImageView.setImageBitmap(bitmap);
        } else {
            profileImageView.setImageResource(R.drawable.default_profile_account_unknown_icon_black_silhouette_free_vector); // fallback
        }
    }

    /**
     * Checks the relationship state between the current user and the searched user.
     * Determines whether the current user is following, has sent a follow request, or is not following the user.
     */
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
                        loadFollowStats();
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("ViewUserProfile", "Error checking relationship: " + e.getMessage()));
    }

    /**
     * Handles the follow button click event.
     * Sends a follow request, unfollows, or shows a message if it's the user's own profile.
     */
    private void onFollowButtonClicked() {
        switch (currentState) {
            case FOLLOW:
                sendFollowRequest();
                break;
            case REQUESTED:
                Toast.makeText(this, "Follow request already sent", Toast.LENGTH_SHORT).show();
                break;
            case UNFOLLOW:
                unfollowUser();
                break;
            case OWN_PROFILE:
                Toast.makeText(this, "This is your profile", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Sends a follow request to the searched user.
     * Updates the user's follow requests in Firestore.
     */
    private void sendFollowRequest() {
        if (currentUser == null || searchedUserId == null) return;

        String currentUserId = currentUser.getUid();
        firestore.collection("Users").document(searchedUserId)
                .update("followRequests", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Follow request sent", Toast.LENGTH_SHORT).show();
                    currentState = FollowState.REQUESTED;
                    updateFollowButtonUI();
                })
                .addOnFailureListener(e -> Log.e("ViewUserProfile", "Error sending request: " + e.getMessage()));
    }

    /**
     * Unfollows the searched user by removing the current user from the followers list
     * and the searched user from the following list.
     */
    private void unfollowUser() {
        if (currentUser == null || searchedUserId == null) return;

        String currentUserId = currentUser.getUid();
        firestore.collection("Users").document(searchedUserId)
                .update("followers", FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("Users").document(currentUserId)
                            .update("following", FieldValue.arrayRemove(searchedUserId))
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show();
                                currentState = FollowState.FOLLOW;
                                updateFollowButtonUI();
                            })
                            .addOnFailureListener(e ->
                                    Log.e("ViewUserProfile", "Error removing from following: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        Log.e("ViewUserProfile", "Error removing from followers: " + e.getMessage()));
    }

    /**
     * Updates the follow button's UI based on the current follow state.
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

    /**
     * Loads the follow statistics (followers and following count) for the searched user.
     * Displays the number of followers and following, and shows a dialog with user names when clicked.
     */
    private void loadFollowStats() {
        if (searchedUserId == null) return;

        firestore.collection("Users").document(searchedUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> followers = (List<String>) doc.get("followers");
                    List<String> following = (List<String>) doc.get("following");

                    int followerCount = (followers != null) ? followers.size() : 0;
                    int followingCount = (following != null) ? following.size() : 0;

                    followersText.setText(followerCount + " Followers");
                    followingText.setText(followingCount + " Following");

                    followersText.setOnClickListener(v -> showUserListDialog("Followers", followers));
                    followingText.setOnClickListener(v -> showUserListDialog("Following", following));
                });
    }

    /**
     * Displays a dialog showing the list of user names for the given list of user IDs.
     *
     * @param title The title of the dialog (either "Followers" or "Following").
     * @param userIds The list of user IDs to be displayed in the dialog.
     */
    private void showUserListDialog(String title, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            Toast.makeText(this, "No " + title.toLowerCase(), Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> usernames = new ArrayList<>();

        for (String id : userIds) {
            firestore.collection("Users").document(id)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String uname = doc.getString("username");
                        if (uname != null) usernames.add(uname);

                        if (usernames.size() == userIds.size()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle(title);
                            builder.setItems(usernames.toArray(new String[0]), null);
                            builder.setPositiveButton("Close", null);
                            builder.show();
                        }
                    });
        }
    }

    /**
     * Loads the latest three public moods of the searched user.
     */
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
                        if (mood != null && mood.getPrivacy() == Mood.Privacy.PUBLIC) {
                            moodList.add(mood);
                        }
                    }
                    moodArrayAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("ViewUserProfile", "Error loading moods: " + e.getMessage()));
    }
}
