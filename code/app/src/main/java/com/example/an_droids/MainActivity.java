package com.example.an_droids;
//
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MoodDialogListener {

    private Button addMoodButton, FollowedMoodButton;
    private ImageView profileButton, searchButton;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 1) Check if a user is currently signed in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // No user is signed in, so redirect to LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // 2) If logged in, get the userId from FirebaseAuth
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup the ViewPager and TabLayout
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        // Initialize main UI elements
        addMoodButton = findViewById(R.id.addButton);
        profileButton = findViewById(R.id.profileButton);
        searchButton = findViewById(R.id.searchButton);
        FollowedMoodButton = findViewById(R.id.FollowedMoodsButton);

        // Button for adding a new mood
        addMoodButton.setOnClickListener(v -> {
            AddMoodFragment addMoodFragment = new AddMoodFragment();
            addMoodFragment.show(getSupportFragmentManager(), "Add Mood");
        });

        // Go to profile
        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        // Go to search
        searchButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        });

        FollowedMoodButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FollowedMoodActivity.class));
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        // 1st tab: Moods
        adapter.addFragment(MoodsFragment.newInstance(userId), "Moods");
        // 2nd tab: Followers + Follow Requests
        adapter.addFragment(FollowersFragment.newInstance(userId), "Followers/Requests");
        // 3rd tab: Following
        adapter.addFragment(FollowingFragment.newInstance(userId), "Following");
        viewPager.setAdapter(adapter);
    }

    // MoodDialogListener callbacks
    @Override
    public void AddMood(Mood mood) {
        MoodProvider moodProvider = new MoodProvider(FirebaseFirestore.getInstance(), userId);
        moodProvider.addMood(mood, userId);
    }

    @Override
    public void EditMood(Mood mood) {
        MoodProvider moodProvider = new MoodProvider(FirebaseFirestore.getInstance(), userId);
        moodProvider.updateMood(mood);
    }

    // Required stubs because we're extending FragmentPagerAdapter in the same activity
    @NonNull
    @Override
    public Fragment getItem(int position) {
        // Not used directly, because we use a custom ViewPagerAdapter
        return null;
    }

    @Override
    public int getCount() {
        // Not used directly, because we use a custom ViewPagerAdapter
        return 0;
    }

    // ViewPagerAdapter as an inner class
    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitleList.get(position);
        }
    }
}
