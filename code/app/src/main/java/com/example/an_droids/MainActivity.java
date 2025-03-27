package com.example.an_droids;

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

        // 1) Check if a user is currently signed in on this device
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // No user is signed in, so redirect to LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // 2) If someone is logged in, get the userId from FirebaseAuth
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // (Optional) If you still want to get the userId from the intent, you can do:
        // String intentUserId = getIntent().getStringExtra("userId");
        // But usually, relying on FirebaseAuth is enough.

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        addMoodButton = findViewById(R.id.addButton);
        profileButton = findViewById(R.id.profileButton);
        searchButton = findViewById(R.id.searchButton);
        FollowedMoodButton = findViewById(R.id.FollowedMoodsButton);

        addMoodButton.setOnClickListener(v -> {
            AddMoodFragment addMoodFragment = new AddMoodFragment();
            addMoodFragment.show(getSupportFragmentManager(), "Add Mood");
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        searchButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        });

        FollowedMoodButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FollowedMoodActivity.class));
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(MoodsFragment.newInstance(userId), "Moods");
        adapter.addFragment(FollowersFragment.newInstance(userId), "Followers");
        adapter.addFragment(FollowingFragment.newInstance(userId), "Following");
        viewPager.setAdapter(adapter);
    }

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

    // These methods are overshadowed by implementing `FragmentPagerAdapter` in an Activity;
    // If you don't actually need them, you can remove them.
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
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
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
