package com.example.an_droids;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private String userId;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private final String[] tabTitles = new String[]{
            "Feed", "Profile", "My Moods", "Search", "Requests"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new HomePagerAdapter(this, userId));

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }

    private static class HomePagerAdapter extends FragmentStateAdapter {

        private final String userId;

        public HomePagerAdapter(@NonNull AppCompatActivity activity, String userId) {
            super(activity);
            this.userId = userId;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new FollowedMoodsFragment();
                case 1: return ProfileFragment.newInstance(userId); // ✅ shows profile inline
                case 2: return MoodsFragment.newInstance(userId);
                case 3: return new SearchFragment();                // ✅ shows search inline
                case 4: return FollowersFragment.newInstance(userId);
                default: return new Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}