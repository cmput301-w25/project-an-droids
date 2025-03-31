package com.example.an_droids;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * MainActivity serves as the entry point for the application after user authentication.
 * It manages navigation between different fragments and user profile interactions.
 */
public class MainActivity extends AppCompatActivity {

    /** User ID of the currently authenticated user. */
    private String userId;

    /** Bottom navigation bar for switching between different fragments. */
    private BottomNavigationView bottomNav;

    /** Profile icon used for navigating to the profile screen. */
    private ImageView profileIcon;

    /**
     * Initializes the activity, checks for authentication, and sets up UI components.
     *
     * @param savedInstanceState The saved instance state from a previous instance of this activity.
     */
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

        loadProfilePicture();

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
                FirebaseFirestore.getInstance().collection("Users").document(userId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            String username = doc.getString("username");
                            if (username != null) {
                                Intent intent = new Intent(MainActivity.this, ViewUserProfile.class);
                                intent.putExtra("username", username);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                        });

                return true;
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

    /**
     * Loads the specified fragment into the main fragment container.
     *
     * @param fragment The fragment to be displayed.
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    /**
     * Loads the profile picture of the currently authenticated user.
     * If no profile picture is available, a default image is used.
     */
    private void loadProfilePicture() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("Users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Users user = documentSnapshot.toObject(Users.class);
                    if (user != null && user.getProfileBitmap() != null) {
                        profileIcon.setImageBitmap(user.getProfileBitmap());
                    } else {
                        profileIcon.setImageResource(R.drawable.default_profile_account_unknown_icon_black_silhouette_free_vector);
                    }
                })
                .addOnFailureListener(e -> {
                    profileIcon.setImageResource(R.drawable.default_profile_account_unknown_icon_black_silhouette_free_vector);
                });
    }

}