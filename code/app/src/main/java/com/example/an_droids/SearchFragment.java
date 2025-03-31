package com.example.an_droids;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

/**
 * Fragment for searching users and sending follow requests. Displays a search input and a list of
 * users who can be followed or unfollowed based on their current follow status.
 *
 * <p>This fragment allows users to search for other users, view their profiles, and manage follow
 * requests through dynamic updates using Firebase Firestore.</p>
 */
public class SearchFragment extends Fragment {

    private EditText searchEditText; // Input field for search queries
    private ListView searchResultsListView; // List view to display the search results

    private final List<UserSearchItem> allUsersList = new ArrayList<>(); // List of all users
    private final List<UserSearchItem> filteredUsersList = new ArrayList<>(); // Filtered list based on search query
    private final Set<String> seenUserIds = new HashSet<>(); // Set to track users already processed

    private UserSearchAdapter searchAdapter; // Adapter to bind data to the list view

    private FirebaseFirestore firestore; // Firestore instance to interact with the database
    private FirebaseAuth mAuth; // Firebase authentication instance
    private FirebaseUser firebaseUser; // Current authenticated Firebase user

    private String currentUsername = ""; // Username of the current user
    private String currentUserId = ""; // User ID of the current user

    private List<String> followingList = new ArrayList<>(); // List of users the current user is following
    private List<String> followRequestsList = new ArrayList<>(); // List of follow requests made by the current user

    private ListenerRegistration usersListener; // Listener for changes to the users collection
    private ListenerRegistration currentUserLiveListener; // Listener for real-time changes to the current user data

    /**
     * Creates and returns the view for the fragment. Inflates the layout for the search fragment.
     *
     * @param inflater The LayoutInflater object to inflate the view
     * @param container The container that holds the fragment's UI
     * @param savedInstanceState Bundle object containing saved state, if any
     * @return The view for the fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    /**
     * Initializes the view components and sets up the search functionality.
     *
     * @param view The root view for the fragment
     * @param savedInstanceState Bundle object containing saved state, if any
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        searchEditText = view.findViewById(R.id.searchEditText);
        searchResultsListView = view.findViewById(R.id.searchResultsListView);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        searchAdapter = new UserSearchAdapter(filteredUsersList);
        searchResultsListView.setAdapter(searchAdapter);

        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
            fetchCurrentUserThenListen();
        } else {
            Toast.makeText(requireContext(), "You must be logged in to search", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
        }

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUserList(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Fetches the current user's data from Firestore and listens for real-time updates.
     */
    private void fetchCurrentUserThenListen() {
        firestore.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        currentUsername = snapshot.getString("username");

                        List<String> following = (List<String>) snapshot.get("following");
                        List<String> requests = (List<String>) snapshot.get("followRequests");

                        followingList = (following != null) ? following : new ArrayList<>();
                        followRequestsList = (requests != null) ? requests : new ArrayList<>();

                        startListeningToUsers();
                        listenToCurrentUserLive();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load user", Toast.LENGTH_SHORT).show());
    }

    /**
     * Listens for real-time updates to the current user's follow status.
     */
    private void listenToCurrentUserLive() {
        currentUserLiveListener = firestore.collection("Users")
                .document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    List<String> following = (List<String>) snapshot.get("following");
                    List<String> requests = (List<String>) snapshot.get("followRequests");

                    followingList = (following != null) ? following : new ArrayList<>();
                    followRequestsList = (requests != null) ? requests : new ArrayList<>();

                    updateAllUserStates();
                });
    }

    /**
     * Starts listening for real-time updates to the users collection.
     */
    private void startListeningToUsers() {
        usersListener = firestore.collection("Users")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    allUsersList.clear();
                    seenUserIds.clear();

                    for (DocumentSnapshot doc : snapshots) {
                        String uid = doc.getId();
                        String username = doc.getString("username");

                        if (uid.equals(currentUserId)) continue;
                        if (username == null || username.isEmpty()) continue;
                        if (seenUserIds.contains(uid)) continue;

                        seenUserIds.add(uid);

                        firestore.collection("Users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    UserSearchItem item = new UserSearchItem(username, uid);

                                    List<String> followers = (List<String>) userDoc.get("followers");
                                    List<String> requests = (List<String>) userDoc.get("followRequests");

                                    if (followers != null && followers.contains(currentUserId)) {
                                        item.setState(UserSearchItem.FollowState.UNFOLLOW);
                                    } else if (requests != null && requests.contains(currentUserId)) {
                                        item.setState(UserSearchItem.FollowState.REQUESTED);
                                    } else {
                                        item.setState(UserSearchItem.FollowState.FOLLOW);
                                    }

                                    allUsersList.add(item);
                                    filterUserList(searchEditText.getText().toString().trim());
                                });
                    }
                });
    }

    /**
     * Updates the follow states for all users based on the current user's follow data.
     */
    private void updateAllUserStates() {
        for (UserSearchItem item : allUsersList) {
            item.setState(getFollowState(item.getUserId()));
        }
        filterUserList(searchEditText.getText().toString().trim());
    }

    /**
     * Determines the follow state of a user based on the current user's follow data.
     *
     * @param uid The user ID to check the follow state for
     * @return The follow state of the user
     */
    private UserSearchItem.FollowState getFollowState(String uid) {
        if (followingList.contains(uid)) {
            return UserSearchItem.FollowState.UNFOLLOW;
        } else if (followRequestsList.contains(uid)) {
            return UserSearchItem.FollowState.REQUESTED;
        } else {
            return UserSearchItem.FollowState.FOLLOW;
        }
    }

    /**
     * Filters the list of users based on the search query.
     *
     * @param query The search query to filter the user list
     */
    private void filterUserList(String query) {
        filteredUsersList.clear();
        if (!query.isEmpty()) {
            for (UserSearchItem item : allUsersList) {
                if (item.getUsername().toLowerCase().contains(query.toLowerCase())) {
                    filteredUsersList.add(item);
                }
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    /**
     * Sends a follow request to a user.
     *
     * @param targetUserId The ID of the user to send the follow request to
     * @param item The UserSearchItem representing the user to follow
     */
    private void sendFollowRequest(String targetUserId, UserSearchItem item) {
        firestore.collection("Users")
                .document(targetUserId)
                .update("followRequests", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    item.setState(UserSearchItem.FollowState.REQUESTED);
                    searchAdapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Follow request sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Error sending request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Unfollows a user.
     *
     * @param targetUserId The ID of the user to unfollow
     * @param item The UserSearchItem representing the user to unfollow
     */
    private void unfollowUser(String targetUserId, UserSearchItem item) {
        firestore.collection("Users").document(targetUserId)
                .update("followers", FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("Users").document(currentUserId)
                            .update("following", FieldValue.arrayRemove(targetUserId))
                            .addOnSuccessListener(aVoid2 -> {
                                item.setState(UserSearchItem.FollowState.FOLLOW);
                                searchAdapter.notifyDataSetChanged();
                                Toast.makeText(requireContext(), "Unfollowed", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error unfollowing: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Adapter class to display the user search results in the ListView.
     */
    private class UserSearchAdapter extends BaseAdapter {

        private final List<UserSearchItem> items;

        public UserSearchAdapter(List<UserSearchItem> items) {
            this.items = items;
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        /**
         * Creates the view for each item in the user search list.
         *
         * @param position The position of the item to display
         * @param convertView The recycled view to reuse
         * @param parent The parent ViewGroup
         * @return The view for the item at the given position
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_search_user, parent, false);
            }

            UserSearchItem currentItem = items.get(position);

            TextView usernameText = convertView.findViewById(R.id.usernameTextView);
            Button followButton = convertView.findViewById(R.id.followButton);

            usernameText.setText(currentItem.getUsername());

            usernameText.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ViewUserProfile.class);
                intent.putExtra("username", currentItem.getUsername());
                startActivity(intent);
            });

            followButton.setVisibility(View.VISIBLE);
            switch (currentItem.getState()) {
                case FOLLOW:
                    followButton.setText("Follow");
                    followButton.setEnabled(true);
                    break;
                case REQUESTED:
                    followButton.setText("Requested");
                    followButton.setEnabled(false);
                    break;
                case UNFOLLOW:
                    followButton.setText("Unfollow");
                    followButton.setEnabled(true);
                    break;
            }

            followButton.setOnClickListener(v -> {
                switch (currentItem.getState()) {
                    case FOLLOW:
                        sendFollowRequest(currentItem.getUserId(), currentItem);
                        break;
                    case UNFOLLOW:
                        unfollowUser(currentItem.getUserId(), currentItem);
                        break;
                    case REQUESTED:
                        Toast.makeText(requireContext(), "Request already sent", Toast.LENGTH_SHORT).show();
                        break;
                }
            });

            return convertView;
        }
    }

    /**
     * Model class for representing a user in the search results.
     */
    private static class UserSearchItem {

        public enum FollowState {
            FOLLOW,
            REQUESTED,
            UNFOLLOW
        }

        private final String username;
        private final String userId;
        private FollowState state;

        public UserSearchItem(String username, String userId) {
            this.username = username;
            this.userId = userId;
            this.state = FollowState.FOLLOW;
        }

        public String getUsername() { return username; }
        public String getUserId() { return userId; }

        public FollowState getState() { return state; }
        public void setState(FollowState state) { this.state = state; }
    }

    /**
     * Called when the fragment's view is being destroyed. This method removes the listeners
     * for real-time updates to the users and current user data to avoid memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usersListener != null) usersListener.remove();
        if (currentUserLiveListener != null) currentUserLiveListener.remove();
    }
}