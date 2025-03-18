package com.example.an_droids;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private ListView searchResultsListView;
    private ArrayAdapter<String> searchAdapter;
    private List<String> allUsernames;   // Stores all usernames from Firestore
    private List<String> filteredUsernames; // Stores filtered results
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String currentUsername = ""; // Store the logged-in username

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchEditText = findViewById(R.id.searchEditText);
        searchResultsListView = findViewById(R.id.searchResultsListView);
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        allUsernames = new ArrayList<>();
        filteredUsernames = new ArrayList<>();
        searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredUsernames);
        searchResultsListView.setAdapter(searchAdapter);

        // Get the current user's username
        loadCurrentUser();

        // Load all usernames from Firestore
        loadAllUsernames();

        // Handle text input for search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                filterUsernames(charSequence.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Handle "Search" button on keyboard
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterUsernames(searchEditText.getText().toString().trim());
                return true;
            }
            return false;
        });

        // Set the item click listener to navigate to the profile page of clicked user
        searchResultsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < filteredUsernames.size()) {
                String clickedUsername = filteredUsernames.get(position);

                // Debugging: Show a toast to confirm the username clicked
                Toast.makeText(SearchActivity.this, "Opening profile of: " + clickedUsername, Toast.LENGTH_SHORT).show();

                // Ensure clickedUsername is not null
                if (clickedUsername != null && !clickedUsername.isEmpty()) {
                    Intent intent = new Intent(SearchActivity.this, ViewUserProfile.class);
                    intent.putExtra("username", clickedUsername);
                    startActivity(intent);
                } else {
                    Toast.makeText(SearchActivity.this, "Error: No username selected", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void loadCurrentUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            firestore.collection("Users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            currentUsername = documentSnapshot.getString("username");
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(SearchActivity.this, "Failed to load current user", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadAllUsernames() {
        firestore.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsernames.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String username = document.getString("username");
                        if (username != null) {
                            allUsernames.add(username); // Store all usernames
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(SearchActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show());
    }

    private void filterUsernames(String query) {
        filteredUsernames.clear();
        if (!query.isEmpty()) {
            for (String username : allUsernames) {
                // Exclude the current user from the results
                if (!username.equalsIgnoreCase(currentUsername) && username.toLowerCase().contains(query.toLowerCase())) {
                    filteredUsernames.add(username);
                }
            }
        }

        searchAdapter.notifyDataSetChanged();
    }
}
