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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MoodDialogListener {
    private Button addMoodButton;
    private ImageView profileButton, searchButton;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            startActivity(new Intent(MainActivity.this, SignupActivity.class));
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
        moodProvider.addMood(mood);
    }

    @Override
    public void EditMood(Mood mood) {
        MoodProvider moodProvider = new MoodProvider(FirebaseFirestore.getInstance(), userId);
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