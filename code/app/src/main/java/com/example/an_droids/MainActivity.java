package com.example.an_droids;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private String userId;
    private BottomNavigationView bottomNav;
    private ImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        profileIcon = findViewById(R.id.profile_icon);

        loadFragment(new FollowedMoodsFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;

            if (id == R.id.nav_feed) {
                selectedFragment = new FollowedMoodsFragment();
            } else if (id == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (id == R.id.nav_moods) {
                selectedFragment = MoodsFragment.newInstance(userId);
            } else if (id == R.id.nav_requests) {
                selectedFragment = FollowersFragment.newInstance(userId);
            } else if (id == R.id.nav_profile) {
                selectedFragment = ProfileFragment.newInstance(userId);
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });

        profileIcon.setOnClickListener(v ->
                loadFragment(ProfileFragment.newInstance(userId)));
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }
}