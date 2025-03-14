package com.example.an_droids;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private ListView searchResultsListView;
    private ArrayAdapter<String> searchAdapter;
    private List<String> usernames;
    private List<String> filteredUsernames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchEditText = findViewById(R.id.searchEditText);
        searchResultsListView = findViewById(R.id.searchResultsListView);

        // Sample list of usernames (basic implementation)
        usernames = new ArrayList<>();
        usernames.add("Alice");
        usernames.add("Bob");
        usernames.add("Charlie");
        usernames.add("David");
        usernames.add("Eve");

        filteredUsernames = new ArrayList<>(usernames);
        searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredUsernames);
        searchResultsListView.setAdapter(searchAdapter);

        // Handle typing in search bar
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                filterUsernames(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Handle Enter key press
        searchEditText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                filterUsernames(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void filterUsernames(String query) {
        filteredUsernames.clear();
        for (String username : usernames) {
            if (username.toLowerCase().contains(query.toLowerCase())) {
                filteredUsernames.add(username);
            }
        }
        searchAdapter.notifyDataSetChanged();
    }
}
