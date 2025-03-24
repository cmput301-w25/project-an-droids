package com.example.an_droids;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MoodDialogListener {

    private Button addMoodButton, filterButton;
    private ImageView profileButton, searchButton;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private ListView moodListView;

    private MoodProvider moodProvider;
    private ArrayList<Mood> moodArrayList;
    private MoodArrayAdapter moodArrayAdapter;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
            return;
        }

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        addMoodButton = findViewById(R.id.addButton);
        profileButton = findViewById(R.id.profileButton);
        searchButton = findViewById(R.id.searchButton);
        moodListView = findViewById(R.id.moodList);
        filterButton = findViewById(R.id.filterButton);

        moodProvider = new MoodProvider(FirebaseFirestore.getInstance(), userId);
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
                Log.e("MainActivity", "Error listening for mood updates: " + error);
            }
        });

        addMoodButton.setOnClickListener(v ->
                new AddMoodFragment().show(getSupportFragmentManager(), "Add Mood")
        );

        moodListView.setOnItemClickListener((parent, view, position, id) -> {
            Mood mood = moodArrayAdapter.getItem(position);
            EditMoodFragment.newInstance(mood)
                    .show(getSupportFragmentManager(), "Edit Mood");
        });

        moodListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Mood mood = moodArrayAdapter.getItem(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Confirmation")
                    .setMessage("Are you sure you want to delete this mood?")
                    .setPositiveButton("Yes", (dialog, which) -> moodProvider.deleteMood(mood))
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        filterButton.setOnClickListener(v -> showFilterOptions());

        profileButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
        searchButton.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );

        findViewById(R.id.mapButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("mood_list", moodArrayList);
            startActivity(intent);
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(
                getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        );
        adapter.addFragment(new MoodsFragment(userId), "Moods");
        adapter.addFragment(new FollowersFragment(userId), "Followers");
        adapter.addFragment(new FollowingFragment(userId), "Following");
        viewPager.setAdapter(adapter);
    }

    private void showFilterOptions() {
        String[] options = {"All", "Recent Week", "By Emotion", "By Reason"};
        new AlertDialog.Builder(this)
                .setTitle("Filter Moods")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: moodProvider.loadAllMoods(); break;
                        case 1: moodProvider.filterByRecentWeek(); break;
                        case 2: showEmotionFilterDialog(); break;
                        case 3: showReasonFilterDialog(); break;
                    }
                }).show();
    }

    private void showEmotionFilterDialog() {
        Mood.EmotionalState[] values = Mood.EmotionalState.values();
        String[] emotionNames = new String[values.length];
        for (int i = 0; i < values.length; i++) emotionNames[i] = values[i].name();

        new AlertDialog.Builder(this)
                .setTitle("Select Emotion")
                .setItems(emotionNames, (dialog, which) ->
                        moodProvider.filterByEmotion(emotionNames[which])
                ).show();
    }

    private void showReasonFilterDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter keyword");
        new AlertDialog.Builder(this)
                .setTitle("Filter by Reason")
                .setView(input)
                .setPositiveButton("Filter", (dialog, which) -> {
                    String keyword = input.getText().toString().trim();
                    if (!keyword.isEmpty()) moodProvider.filterByReasonContains(keyword);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void AddMood(Mood mood) {
        moodProvider.addMood(mood);
    }

    @Override
    public void EditMood(Mood mood) {
        moodProvider.updateMood(mood);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return null;
    }

    @Override
    public int getCount() {
        return 0;
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments = new ArrayList<>();
        private final List<String> titles = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }
}
