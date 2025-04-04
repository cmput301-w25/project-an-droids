package com.example.an_droids;
//
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The MapActivity class displays a Google Map with mood events.
 * Users can view their own moods, their following's moods, and moods within a 5 km radius.
 *
 * Implements {@link OnMapReadyCallback} to handle the map initialization.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    /** GoogleMap instance to display the map. */
    private GoogleMap mMap;

    /** Button to apply filters to the displayed moods. */
    private ImageView filterButton;

    /** List of mood events to be displayed on the map. */
    private ArrayList<Mood> moodList;

    /** Location provider client for fetching user location. */
    private FusedLocationProviderClient fusedLocationProviderClient;

    /** Request code for location permission. */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Initializes the activity, loads the map, and sets up event listeners.
     *
     * @param savedInstanceState Saved instance state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        moodList = (ArrayList<Mood>) getIntent().getSerializableExtra("mood_list");

        filterButton = findViewById(R.id.filterButton);
        ImageView backButton = findViewById(R.id.backButton);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        filterButton.setOnClickListener(v -> showTopLevelFilterDialog());
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(MapActivity.this, MainActivity.class));
            finish();
        });
    }

    /**
     * Callback when the Google Map is ready. Enables location tracking and loads user moods.
     *
     * @param googleMap The GoogleMap instance.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13f));
                    }
                    loadCurrentUserMoods();
                });

        // No marker display until filter is selected
    }

    /**
     * Loads and displays the user's mood events on the map.
     */
    private void loadCurrentUserMoods() {
        mMap.clear();
        // If a mood list was passed in, use it; otherwise, fetch from Firestore
        if (moodList != null && !moodList.isEmpty()) {
            for (Mood mood : moodList) {
                if (mood.getLatitude() != 0 && mood.getLongitude() != 0) {
                    LatLng moodLocation = new LatLng(mood.getLatitude(), mood.getLongitude());
                    String title = mood.getEmotion().name() + " " + mood.getEmotionEmoji();
                    mMap.addMarker(new MarkerOptions().position(moodLocation).title(title));
                }
            }
        } else {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) return;

            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(currentUser.getUid())
                    .collection("Moods")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Mood mood = doc.toObject(Mood.class);
                            if (mood.getLatitude() != 0 && mood.getLongitude() != 0) {
                                LatLng moodLocation = new LatLng(mood.getLatitude(), mood.getLongitude());
                                String title = mood.getEmotion().name() + " " + mood.getEmotionEmoji();
                                mMap.addMarker(new MarkerOptions().position(moodLocation).title(title));
                            }
                        }
                    });
        }
    }

    /**
     * Shows a dialog allowing users to filter mood events.
     */
    private void showTopLevelFilterDialog() {
        String[] options = {"My Mood Events", "Following’s Mood Events", "Mood Events within 5 km"};
        new AlertDialog.Builder(this)
                .setTitle("Filter Mood Events")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showMyMoodEventsOnMap(); break;
                        case 1: showFollowingFilterDialog(); break;
                        case 2: loadNearbyFollowingMoods(); break;
                    }
                }).show();
    }


    /**
     * Displays a dialog for users to filter mood events from their following.
     */
    private void showFollowingFilterDialog() {
        String[] filters = {"All", "Recent Week", "By Emotion", "By Reason"};
        new AlertDialog.Builder(this)
                .setTitle("Filter Moods")
                .setItems(filters, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            loadFollowingMoods("all", null);
                            break;
                        case 1:
                            loadFollowingMoods("recent", null);
                            break;
                        case 2:
                            showEmotionSelection();
                            break;
                        case 3:
                            showReasonKeywordInput();
                            break;
                    }
                })
                .show();
    }

    /**
     * Displays a dialog for users to select an emotion filter.
     */
    private void showEmotionSelection() {
        Mood.EmotionalState[] values = Mood.EmotionalState.values();
        String[] emotionNames = new String[values.length];
        for (int i = 0; i < values.length; i++) emotionNames[i] = values[i].name();

        new AlertDialog.Builder(this)
                .setTitle("Select Emotion")
                .setItems(emotionNames, (dialog, which) -> loadFollowingMoods("emotion", emotionNames[which]))
                .show();
    }

    /**
     * Displays a dialog for users to enter a reason keyword filter.
     */
    private void showReasonKeywordInput() {
        EditText input = new EditText(this);
        input.setHint("Enter keyword");

        new AlertDialog.Builder(this)
                .setTitle("Filter by Reason")
                .setView(input)
                .setPositiveButton("Filter", (dialog, which) -> {
                    String keyword = input.getText().toString().trim();
                    if (!keyword.isEmpty()) {
                        loadFollowingMoods("reason", keyword);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Loads and displays mood events from the user's following based on the selected filter.
     *
     * @param filterType   The type of filter to apply (e.g., "all", "recent", "emotion", "reason").
     * @param filterValue  The specific value for filtering (e.g., emotion name, reason keyword).
     */
    private void loadFollowingMoods(String filterType, String filterValue) {
        mMap.clear();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> following = (List<String>) snapshot.get("following");
                    if (following == null) return;

                    for (String userId : following) {
                        FirebaseFirestore.getInstance()
                                .collection("Users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener(userSnap -> {
                                    String username = userSnap.getString("username");

                                    FirebaseFirestore.getInstance()
                                            .collection("Users")
                                            .document(userId)
                                            .collection("Moods")
                                            .get()
                                            .addOnSuccessListener(moodsSnapshot -> {
                                                for (QueryDocumentSnapshot doc : moodsSnapshot) {
                                                    Mood mood = doc.toObject(Mood.class);
                                                    if (mood.getPrivacy() == Mood.Privacy.PUBLIC &&
                                                            mood.getLatitude() != 0 && mood.getLongitude() != 0) {

                                                        boolean shouldAdd = false;

                                                        switch (filterType) {
                                                            case "all":
                                                                shouldAdd = true;
                                                                break;
                                                            case "recent":
                                                                if (mood.getTimestamp() != null &&
                                                                        mood.getTimestamp().after(new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))) {
                                                                    shouldAdd = true;
                                                                }
                                                                break;
                                                            case "emotion":
                                                                if (mood.getEmotion().name().equalsIgnoreCase(filterValue)) {
                                                                    shouldAdd = true;
                                                                }
                                                                break;
                                                            case "reason":
                                                                if (mood.getReason() != null &&
                                                                        mood.getReason().toLowerCase().contains(filterValue.toLowerCase())) {
                                                                    shouldAdd = true;
                                                                }
                                                                break;
                                                        }

                                                        if (shouldAdd) {
                                                            LatLng moodLocation = new LatLng(mood.getLatitude(), mood.getLongitude());
                                                            String markerTitle = mood.getEmotionEmoji() + " " + mood.getEmotion().name();
                                                            String markerSnippet = "by " + username;

                                                            mMap.addMarker(new MarkerOptions()
                                                                    .position(moodLocation)
                                                                    .title(markerTitle)
                                                                    .snippet(markerSnippet));
                                                        }
                                                    }
                                                }
                                            });
                                });
                    }


                });
    }

    /**
     * Displays only the user's own mood events on the map.
     */
    private void showMyMoodEventsOnMap() {
        mMap.clear();
        if (moodList != null && !moodList.isEmpty()) {
            for (Mood mood : moodList) {
                if (mood.getLatitude() != 0 && mood.getLongitude() != 0) {
                    LatLng moodLocation = new LatLng(mood.getLatitude(), mood.getLongitude());
                    String title = mood.getEmotion().name() + " " + mood.getEmotionEmoji();
                    mMap.addMarker(new MarkerOptions()
                            .position(moodLocation)
                            .title(title));
                }
            }
        }
    }

    /**
     * Requests location permissions if not granted.
     *
     * @param requestCode  Request code.
     * @param permissions  Permissions requested.
     * @param grantResults Result of permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onMapReady(mMap);
        }
    }

    /**
     * Loads and displays public mood events from the user's following that are within a 5 km radius.
     * Only moods from the past 7 days are considered.
     */
    private void loadNearbyFollowingMoods() {
        mMap.clear();

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) return;

                    double currentLat = location.getLatitude();
                    double currentLng = location.getLongitude();

                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) return;

                    FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                List<String> following = (List<String>) snapshot.get("following");
                                if (following == null) return;

                                for (String userId : following) {
                                    FirebaseFirestore.getInstance()
                                            .collection("Users")
                                            .document(userId)
                                            .get()
                                            .addOnSuccessListener(userSnap -> {
                                                String username = userSnap.getString("username");

                                                FirebaseFirestore.getInstance()
                                                        .collection("Users")
                                                        .document(userId)
                                                        .collection("Moods")
                                                        .get()
                                                        .addOnSuccessListener(moodsSnapshot -> {
                                                            for (QueryDocumentSnapshot doc : moodsSnapshot) {
                                                                Mood mood = doc.toObject(Mood.class);

                                                                if (mood.getPrivacy() != Mood.Privacy.PUBLIC) continue;
                                                                if (mood.getTimestamp() == null) continue;
                                                                if (mood.getLatitude() == 0 || mood.getLongitude() == 0) continue;

                                                                // Check if within last 7 days
                                                                long oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
                                                                if (mood.getTimestamp().getTime() < oneWeekAgo) continue;

                                                                // Check distance
                                                                float[] result = new float[1];
                                                                Location.distanceBetween(currentLat, currentLng,
                                                                        mood.getLatitude(), mood.getLongitude(), result);

                                                                if (result[0] <= 5000) { // ≤ 5 km
                                                                    LatLng moodLocation = new LatLng(mood.getLatitude(), mood.getLongitude());
                                                                    String title = mood.getEmotionEmoji() + " " + mood.getEmotion().name();
                                                                    String snippet = "by " + username;

                                                                    mMap.addMarker(new MarkerOptions()
                                                                            .position(moodLocation)
                                                                            .title(title)
                                                                            .snippet(snippet));
                                                                }
                                                            }
                                                        });
                                            });
                                }
                            });
                });
    }
}