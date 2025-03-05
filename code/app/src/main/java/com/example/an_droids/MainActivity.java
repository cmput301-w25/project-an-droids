package com.example.an_droids;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MoodDialogListener {

    private ArrayList<Mood> dataList;
    private ListView moodList;
    private MoodArrayAdapter moodAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        dataList = new ArrayList<>();

        moodList = findViewById(R.id.moodList);
        moodAdapter = new MoodArrayAdapter(this, dataList);
        moodList.setAdapter(moodAdapter);

        // Fetch existing moods from Firestore
        db.collection("moods")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Mood mood = document.toObject(Mood.class);
                            mood.setId(document.getId());
                            dataList.add(mood);
                        }
                        moodAdapter.notifyDataSetChanged();
                    } else {
                    }
                });

        // Add Button
        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            new AddMoodFragment().show(getSupportFragmentManager(), "Add Mood");
        });

        // OnItemClick for editing
        moodList.setOnItemClickListener((parent, view, position, id) -> {
            Mood mood = dataList.get(position);
            EditMoodFragment fragment = new EditMoodFragment();
            Bundle args = new Bundle();
            args.putSerializable("mood", mood);
            fragment.setArguments(args);
            fragment.show(getSupportFragmentManager(), "Edit Mood");
        });

        // OnItemLongClick for deletion
        moodList.setOnItemLongClickListener((parent, view, position, id) -> {
            Mood mood = moodAdapter.getItem(position);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete Confirmation")
                    .setMessage("Are you sure you want to delete this mood?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        db.collection("moods").document(mood.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    dataList.remove(mood);
                                    moodAdapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                });
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    @Override
    public void AddMood(Mood mood) {
        db.collection("moods")
                .add(mood)
                .addOnSuccessListener(documentReference -> {
                    mood.setId(documentReference.getId());
                    dataList.add(mood);
                    moodAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                });
    }

    @Override
    public void EditMood(Mood mood) {
        db.collection("moods")
                .document(mood.getId())
                .set(mood)
                .addOnSuccessListener(aVoid -> {
                    for (int i = 0; i < dataList.size(); i++) {
                        if (dataList.get(i).getId().equals(mood.getId())) {
                            dataList.set(i, mood);
                            break;
                        }
                    }
                    moodAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                });
    }
}
