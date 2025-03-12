package com.example.an_droids;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MoodDialogListener {
    private Button addMoodButton;
    private ListView moodListView;
    private MoodProvider moodProvider;
    private ArrayList<Mood> moodArrayList;
    private MoodArrayAdapter moodArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        addMoodButton = findViewById(R.id.addButton);
        moodListView = findViewById(R.id.moodList);

        moodProvider = MoodProvider.getInstance(FirebaseFirestore.getInstance());
        moodArrayList = moodProvider.getMoods();
        moodArrayAdapter = new MoodArrayAdapter(this, moodArrayList);
        moodListView.setAdapter(moodArrayAdapter);

        moodProvider.listenForUpdates(new MoodProvider.DataStatus() {
            @Override
            public void onDataUpdated() {
                moodArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Log.e("Mood Update Error", error);
            }
        });

        addMoodButton.setOnClickListener(view -> {
            AddMoodFragment addMoodFragment = new AddMoodFragment();
            addMoodFragment.show(getSupportFragmentManager(), "Add Mood");
        });

        moodListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Mood mood = moodArrayAdapter.getItem(i);
            EditMoodFragment editMoodFragment = EditMoodFragment.newInstance(mood);
            editMoodFragment.show(getSupportFragmentManager(), "Edit Mood");
        });

        moodListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            Mood mood = moodArrayAdapter.getItem(i);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete Confirmation")
                    .setMessage("Are you sure you want to delete this mood?")
                    .setPositiveButton("Yes", (dialog, which) -> moodProvider.deleteMood(mood))
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    @Override
    public void AddMood(Mood mood) {
        moodProvider.addMood(mood);
    }

    @Override
    public void EditMood(Mood mood) {
        moodProvider.updateMood(mood);
    }
}
