package com.example.an_droids;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MoodsFragment extends Fragment implements MoodDialogListener {

    private ListView moodListView;
    private MoodArrayAdapter moodArrayAdapter;
    private ArrayList<Mood> moodList;
    private String userId;
    private MoodProvider moodProvider;

    private Button filterButton, mapButton, addMoodButton;

    public MoodsFragment() {
        // Required empty public constructor
    }

    public static MoodsFragment newInstance(String userId) {
        MoodsFragment fragment = new MoodsFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moods, container, false);

        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
        }

        moodListView = view.findViewById(R.id.moodList);
        filterButton = view.findViewById(R.id.filterButton);
        mapButton = view.findViewById(R.id.mapButton);
        addMoodButton = view.findViewById(R.id.addMoodButton);

        moodList = new ArrayList<>();
        moodArrayAdapter = new MoodArrayAdapter(getContext(), moodList);
        moodListView.setAdapter(moodArrayAdapter);

        moodProvider = new MoodProvider(FirebaseFirestore.getInstance(), userId);
        loadMoods();

        addMoodButton.setOnClickListener(v -> openAddMoodDialog());

        moodListView.setOnItemClickListener((parent, itemView, position, id) -> {
            Mood mood = moodArrayAdapter.getItem(position);
            openEditMoodDialog(mood);
        });

        moodListView.setOnItemLongClickListener((parent, itemView, position, id) -> {
            Mood mood = moodArrayAdapter.getItem(position);
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Confirmation")
                    .setMessage("Are you sure you want to delete this mood?")
                    .setPositiveButton("Yes", (dialog, which) -> moodProvider.deleteMood(mood))
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        filterButton.setOnClickListener(v -> showFilterOptions());

        // Modified: When launching the map, clear heavy image/voice data and ensure weather is updated.
        mapButton.setOnClickListener(v -> {
            for (Mood mood : moodList) {
                mood.setVoiceNoteBlob(null);
                mood.setImage(null);
                // If weather is not available and location is set, trigger an update.
                if ((mood.getWeather() == null || mood.getWeather().isEmpty()) &&
                        (mood.getLatitude() != 0 || mood.getLongitude() != 0)) {
                    mood.updateWeather();
                }
            }
            Intent intent = new Intent(requireContext(), MapActivity.class);
            intent.putExtra("mood_list", moodList);
            startActivity(intent);
        });

        return view;
    }

    // Launch AddMoodFragment with direct listener.
    public void openAddMoodDialog() {
        AddMoodFragment addMoodFragment = new AddMoodFragment();
        addMoodFragment.setListener(this); // Pass self as listener.
        addMoodFragment.show(getChildFragmentManager(), "Add Mood");
    }

    // Launch EditMoodFragment with direct listener.
    public void openEditMoodDialog(Mood mood) {
        EditMoodFragment editMoodFragment = EditMoodFragment.newInstance(mood);
        editMoodFragment.setListener(this); // Pass self as listener.
        editMoodFragment.show(getChildFragmentManager(), "Edit Mood");
    }

    // Listener implementations.
    @Override
    public void AddMood(Mood mood) {
        if (moodProvider != null) {
            moodProvider.addMood(mood, userId);
            Log.d("MoodsFragment", "Mood added: " + mood.getReason());
        }
    }

    @Override
    public void EditMood(Mood mood) {
        if (moodProvider != null) {
            moodProvider.updateMood(mood);
            Log.d("MoodsFragment", "Mood updated: " + mood.getReason());
        }
    }

    private void loadMoods() {
        moodList.clear();
        moodList.addAll(moodProvider.getMoods());
        moodArrayAdapter.notifyDataSetChanged();

        moodProvider.listenForUpdates(new MoodProvider.DataStatus() {
            @Override
            public void onDataUpdated() {
                moodList.clear();
                moodList.addAll(moodProvider.getMoods());
                moodArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Log.e("MoodsFragment", "Error loading moods: " + error);
            }
        });
    }

    private void showFilterOptions() {
        String[] options = {"All", "Recent Week", "By Emotion", "By Reason"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Filter Moods")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            moodProvider.loadAllMoods();
                            break;
                        case 1:
                            moodProvider.filterByRecentWeek();
                            break;
                        case 2:
                            showEmotionFilterDialog();
                            break;
                        case 3:
                            showReasonFilterDialog();
                            break;
                    }
                }).show();
    }

    private void showEmotionFilterDialog() {
        Mood.EmotionalState[] values = Mood.EmotionalState.values();
        String[] emotionNames = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            emotionNames[i] = values[i].name();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Emotion")
                .setItems(emotionNames, (dialog, which) ->
                        moodProvider.filterByEmotion(emotionNames[which])
                ).show();
    }

    private void showReasonFilterDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Enter keyword");
        new AlertDialog.Builder(requireContext())
                .setTitle("Filter by Reason")
                .setView(input)
                .setPositiveButton("Filter", (dialog, which) -> {
                    String keyword = input.getText().toString().trim();
                    if (!keyword.isEmpty()) {
                        moodProvider.filterByReasonContains(keyword);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
