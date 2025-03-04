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

public class MainActivity extends AppCompatActivity implements MoodDialogListener{

    private ArrayList<Mood> dataList;
    private ListView moodList;
    private MoodArrayAdapter moodAdapter;

    @Override
    public void AddMood(Mood mood) {
        dataList.add(mood);
        moodAdapter.notifyDataSetChanged();
    }

    @Override
    public void EditMood(Mood mood) {
        moodAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataList = new ArrayList<>();

        moodList = findViewById(R.id.moodList);
        moodAdapter = new MoodArrayAdapter(this, dataList);
        moodList.setAdapter(moodAdapter);

        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddMoodFragment().show(getSupportFragmentManager(), "Add Mood");
            }
        });

        moodList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Mood mood = dataList.get(position);
                EditMoodFragment fragment = new EditMoodFragment();
                Bundle args = new Bundle();
                args.putSerializable("mood", mood);
                fragment.setArguments(args);

                fragment.show(getSupportFragmentManager(), "Edit Mood");
            }
        });

        moodList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Mood mood = moodAdapter.getItem(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Confirmation")
                        .setMessage("Are you sure you want to delete this mood?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dataList.remove(mood);
                                moodAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
        });
    }

}