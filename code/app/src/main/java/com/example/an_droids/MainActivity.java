package com.example.an_droids;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity demonstrates how to integrate Firebase CRUD operations
 * into your mood-tracking app via a MoodProvider. This example:
 *
 * - Shows how to fetch Mood data from Firestore on startup
 * - Allows adding/editing Moods through dialog fragments
 * - Deletes Moods via a long-click context menu
 */
public class MainActivity extends AppCompatActivity implements MoodDialogListener {

    private ListView moodList;
    private MoodArrayAdapter moodAdapter;
    private ArrayList<Mood> dataList;    // local data to populate the ListView
    private MoodProvider moodProvider;   // our Firebase provider for CRUD

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize local data list and provider
        dataList = new ArrayList<>();
        moodProvider = new MoodProvider();

        // Set up ListView and Adapter
        moodList = findViewById(R.id.moodList);
        moodAdapter = new MoodArrayAdapter(this, dataList);
        moodList.setAdapter(moodAdapter);

        // Load existing moods from Firestore
        loadMoodsFromFirebase();

        // Handle "Add Mood" button
        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            // Show the AddMoodFragment dialog
            new AddMoodFragment().show(getSupportFragmentManager(), "Add Mood");
        });

        // Short-click: Edit a Mood
        moodList.setOnItemClickListener((parent, view, position, id) -> {
            Mood mood = dataList.get(position);
            EditMoodFragment fragment = new EditMoodFragment();

            Bundle args = new Bundle();
            args.putSerializable("mood", mood);
            fragment.setArguments(args);

            fragment.show(getSupportFragmentManager(), "Edit Mood");
        });

        // Long-click: Delete a Mood
        moodList.setOnItemLongClickListener((parent, view, position, id) -> {
            Mood mood = moodAdapter.getItem(position);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete Confirmation")
                    .setMessage("Are you sure you want to delete this mood?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        // Delete from Firestore
                        moodProvider.deleteMood(mood, new MoodProvider.OnMoodOperationListener() {
                            @Override
                            public void onSuccess() {
                                // Remove from local list and refresh adapter
                                dataList.remove(mood);
                                moodAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                // Handle error (e.g., show a toast)
                            }
                        });
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    /**
     * Callback when a new Mood is created in AddMoodFragment.
     */
    @Override
    public void AddMood(Mood mood) {
        // Add new Mood to Firestore
        moodProvider.addMood(mood, new MoodProvider.OnMoodOperationListener() {
            @Override
            public void onSuccess() {
                // Also add to local list so UI updates immediately
                dataList.add(mood);
                moodAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error (e.g., show a toast)
            }
        });
    }

    /**
     * Callback when an existing Mood is edited in EditMoodFragment.
     */
    @Override
    public void EditMood(Mood mood) {
        // Update the existing Mood in Firestore
        moodProvider.updateMood(mood, new MoodProvider.OnMoodOperationListener() {
            @Override
            public void onSuccess() {
                // If the reference is the same in dataList, local data is already updated;
                // just refresh the adapter to reflect changes
                moodAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error (e.g., show a toast)
            }
        });
    }
    private void loadMoodsFromFirebase() {
        moodProvider.getAllMoods(new MoodProvider.OnMoodsLoadedListener() {
            @Override
            public void onMoodsLoaded(List<Mood> moods) {
                dataList.clear();
                dataList.addAll(moods);
                moodAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                // Handle error (e.g., log or display a message)
            }
        });
    }
}
