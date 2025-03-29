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

    private List<UserSearchItem> allUsersList;
    private List<UserSearchItem> filteredUsersList;
    private UserSearchAdapter searchAdapter;

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;

    private String currentUsername = "";
    private String currentUserId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false); // Reuse activity layout
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        searchEditText = view.findViewById(R.id.searchEditText);
        searchResultsListView = view.findViewById(R.id.searchResultsListView);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        allUsersList = new ArrayList<>();
        filteredUsersList = new ArrayList<>();

        searchAdapter = new UserSearchAdapter(filteredUsersList);
        searchResultsListView.setAdapter(searchAdapter);

        loadCurrentUser();
        loadAllUsers();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUserList(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCurrentUser() {
        if (firebaseUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to search", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
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
                        Toast.makeText(requireContext(),
                                "Failed to load current user", Toast.LENGTH_SHORT).show());
    }

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
                    filterUserList(searchEditText.getText().toString().trim());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to load users", Toast.LENGTH_SHORT).show());
    }

    private void filterUserList(String query) {
        filteredUsersList.clear();
        if (!query.isEmpty()) {
            for (UserSearchItem item : allUsersList) {
                String username = item.getUsername();
                if (username.equalsIgnoreCase(currentUsername)) continue;
                if (username.toLowerCase().contains(query.toLowerCase())) {
                    filteredUsersList.add(item);
                }
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void sendFollowRequest(String targetUserId) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "No current user ID found", Toast.LENGTH_SHORT).show();
            return;
        }
        firestore.collection("Users")
                .document(targetUserId)
                .update("followRequests", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(),
                            "Follow request sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Error sending request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

            followButton.setOnClickListener(v -> sendFollowRequest(currentItem.getUserId()));

            return convertView;
        }
    }

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
}
