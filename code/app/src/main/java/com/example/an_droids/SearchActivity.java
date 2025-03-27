package com.example.an_droids;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * This SearchActivity uses a custom adapter to show usernames + a "Follow" button in the results.
 * Clicking on the username text opens that user's profile, while clicking on "Follow"
 * actually sends a follow request.
 */
public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private ListView searchResultsListView;

    // We'll keep a full list of "UserSearchItem" for all users
    private List<UserSearchItem> allUsersList;
    // Then filter them for display
    private List<UserSearchItem> filteredUsersList;

    // Our custom adapter
    private UserSearchAdapter searchAdapter;

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;

    private String currentUsername = "";
    private String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchEditText = findViewById(R.id.searchEditText);
        searchResultsListView = findViewById(R.id.searchResultsListView);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        allUsersList = new ArrayList<>();
        filteredUsersList = new ArrayList<>();

        // Create our custom adapter
        searchAdapter = new UserSearchAdapter(filteredUsersList);
        searchResultsListView.setAdapter(searchAdapter);

        // 1) Load current user info
        loadCurrentUser();

        // 2) Load all users from Firestore
        loadAllUsers();

        // 3) As user types, filter the list
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUserList(s.toString().trim());
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        // 4) If user presses "Search" on keyboard
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterUserList(searchEditText.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    /**
     * Loads the currently logged-in user's details (ID & username).
     */
    private void loadCurrentUser() {
        if (firebaseUser == null) {
            Toast.makeText(this, "You must be logged in to search", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = firebaseUser.getUid();

        firestore.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(SearchActivity.this,
                                "Failed to load current user", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads all users from Firestore, storing username & doc ID into allUsersList.
     */
    private void loadAllUsers() {
        firestore.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsersList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid = doc.getId();
                        String username = doc.getString("username");
                        if (username != null && !username.isEmpty()) {
                            allUsersList.add(new UserSearchItem(username, uid));
                        }
                    }
                    // Once loaded, re-filter with current text
                    filterUserList(searchEditText.getText().toString().trim());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(SearchActivity.this,
                                "Failed to load users", Toast.LENGTH_SHORT).show());
    }

    /**
     * Filters out our own username and any user not matching the query.
     */
    private void filterUserList(String query) {
        filteredUsersList.clear();
        if (!query.isEmpty()) {
            for (UserSearchItem item : allUsersList) {
                String username = item.getUsername();
                if (username.equalsIgnoreCase(currentUsername)) {
                    // Exclude ourselves from search
                    continue;
                }
                // Check if it contains the query
                if (username.toLowerCase().contains(query.toLowerCase())) {
                    filteredUsersList.add(item);
                }
            }
        }
        // Notify adapter that data changed
        searchAdapter.notifyDataSetChanged();
    }

    /**
     * Actually sends a follow request to the target user by updating
     * their "followRequests" with currentUserId.
     */
    private void sendFollowRequest(String targetUserId) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "No current user ID found", Toast.LENGTH_SHORT).show();
            return;
        }
        firestore.collection("Users")
                .document(targetUserId)
                .update("followRequests", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SearchActivity.this,
                            "Follow request sent!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SearchActivity.this,
                            "Error sending request: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * An item in the search list.
     */
    private static class UserSearchItem {
        private final String username;
        private final String userId;

        public UserSearchItem(String username, String userId) {
            this.username = username;
            this.userId = userId;
        }
        public String getUsername() { return username; }
        public String getUserId() { return userId; }
    }

    /**
     * A custom adapter that shows:
     *  - A TextView for username (tap to open that user's profile)
     *  - A "Follow" button (tap to send request)
     */
    private class UserSearchAdapter extends android.widget.BaseAdapter {

        private final List<UserSearchItem> items;

        public UserSearchAdapter(List<UserSearchItem> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position; // Not used
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView,
                                         android.view.ViewGroup parent) {
            if (convertView == null) {
                // Inflate our custom row layout
                convertView = getLayoutInflater().inflate(
                        R.layout.item_search_user, parent, false);
            }

            UserSearchItem currentItem = items.get(position);

            // Get references
            android.widget.TextView usernameText = convertView.findViewById(R.id.usernameTextView);
            android.widget.Button followButton = convertView.findViewById(R.id.followButton);

            // Set data
            usernameText.setText(currentItem.getUsername());

            // 1) Tapping on the username -> open that user's profile
            usernameText.setOnClickListener(v -> {
                Intent intent = new Intent(SearchActivity.this, ViewUserProfile.class);
                intent.putExtra("username", currentItem.getUsername());
                startActivity(intent);
            });

            // 2) Tapping on "Follow" -> send follow request
            followButton.setOnClickListener(v -> {
                sendFollowRequest(currentItem.getUserId());
            });

            return convertView;
        }
    }
}
