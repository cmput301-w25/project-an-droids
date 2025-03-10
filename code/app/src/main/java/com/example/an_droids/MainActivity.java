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

public class MainActivity extends AppCompatActivity implements MoodDialogListener {

    private ListView moodList;
    private MoodArrayAdapter moodAdapter;
    private ArrayList<Mood> dataList;    // local data to populate the ListView
    private MoodProvider moodProvider;   // our Firebase provider for CRUD

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataList = new ArrayList<>();
        moodProvider = new MoodProvider();

        moodList = findViewById(R.id.moodList);
        moodAdapter = new MoodArrayAdapter(this, dataList);
        moodList.setAdapter(moodAdapter);

        loadMoodsFromFirebase();

        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            new AddMoodFragment().show(getSupportFragmentManager(), "Add Mood");
        });

        moodList.setOnItemClickListener((parent, view, position, id) -> {
            Mood mood = dataList.get(position);
            EditMoodFragment fragment = new EditMoodFragment();

            Bundle args = new Bundle();
            args.putSerializable("mood", mood);
            fragment.setArguments(args);

            fragment.show(getSupportFragmentManager(), "Edit Mood");
        });

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

    @Override
    public void AddMood(Mood mood) {
        moodProvider.addMood(mood, new MoodProvider.OnMoodOperationListener() {
            @Override
            public void onSuccess() {
                dataList.add(mood);
                moodAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Exception e) {
            }
        });
    }

    @Override
    public void EditMood(Mood mood) {
        moodProvider.updateMood(mood, new MoodProvider.OnMoodOperationListener() {
            @Override
            public void onSuccess() {
                moodAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Exception e) {
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
            }
        });
    }
}
