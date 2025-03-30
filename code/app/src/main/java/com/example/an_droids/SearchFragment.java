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

public class SearchFragment extends Fragment {

    private EditText searchEditText;
    private ListView searchResultsListView;

    private final List<UserSearchItem> allUsersList = new ArrayList<>();
    private final List<UserSearchItem> filteredUsersList = new ArrayList<>();
    private final Set<String> seenUserIds = new HashSet<>();

    private UserSearchAdapter searchAdapter;

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;

    private String currentUsername = "";
    private String currentUserId = "";

    private List<String> followingList = new ArrayList<>();
    private List<String> followRequestsList = new ArrayList<>();

    private ListenerRegistration usersListener;
    private ListenerRegistration currentUserLiveListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

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
            fetchCurrentUserThenListen(); // 🧠 new flow
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

    // 🧠 BLOCKING fetch + real-time after init
    private void fetchCurrentUserThenListen() {
        firestore.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        currentUsername = snapshot.getString("username");

                        List<String> following = (List<String>) snapshot.get("following");
                        List<String> requests = (List<String>) snapshot.get("followRequests");

                        followingList = (following != null) ? following : new ArrayList<>();
                        followRequestsList = (requests != null) ? requests : new ArrayList<>();

                        // ✅ Start real-time user list after we KNOW we have follow data
                        startListeningToUsers();

                        // ✅ Also set up live listener for follow updates
                        listenToCurrentUserLive();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load user", Toast.LENGTH_SHORT).show());
    }

    private void listenToCurrentUserLive() {
        currentUserLiveListener = firestore.collection("Users")
                .document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    List<String> following = (List<String>) snapshot.get("following");
                    List<String> requests = (List<String>) snapshot.get("followRequests");

                    followingList = (following != null) ? following : new ArrayList<>();
                    followRequestsList = (requests != null) ? requests : new ArrayList<>();

                    updateAllUserStates(); // Re-render follow buttons
                });
    }

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

                        // 🔍 Check this user's followers/followRequests
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

    private void updateAllUserStates() {
        for (UserSearchItem item : allUsersList) {
            item.setState(getFollowState(item.getUserId()));
        }
        filterUserList(searchEditText.getText().toString().trim());
    }

    private UserSearchItem.FollowState getFollowState(String uid) {
        if (followingList.contains(uid)) {
            return UserSearchItem.FollowState.UNFOLLOW;
        } else if (followRequestsList.contains(uid)) {
            return UserSearchItem.FollowState.REQUESTED;
        } else {
            return UserSearchItem.FollowState.FOLLOW;
        }
    }

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

    private class UserSearchAdapter extends BaseAdapter {

        private final List<UserSearchItem> items;

        public UserSearchAdapter(List<UserSearchItem> items) {
            this.items = items;
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usersListener != null) usersListener.remove();
        if (currentUserLiveListener != null) currentUserLiveListener.remove();
    }
}