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

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ImageView filterButton;
    private ArrayList<Mood> moodList;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        moodList = (ArrayList<Mood>) getIntent().getSerializableExtra("mood_list");

        filterButton = findViewById(R.id.filterButton);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        filterButton.setOnClickListener(v -> showTopLevelFilterDialog());
    }

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
                });

        // No marker display until filter is selected
    }

    private void showTopLevelFilterDialog() {
        String[] options = {"My Mood Events", "Following’s Mood Events", "Mood Events within 5 km"};
        new AlertDialog.Builder(this)
                .setTitle("Filter Mood Events")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showMyMoodEventsOnMap();
                            break;
                        case 1:
                            showFollowingFilterDialog();
                            break;
                        case 2:
                            // Implement nearby logic here
                            break;
                    }
                })
                .show();
    }

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

    private void showEmotionSelection() {
        Mood.EmotionalState[] values = Mood.EmotionalState.values();
        String[] emotionNames = new String[values.length];
        for (int i = 0; i < values.length; i++) emotionNames[i] = values[i].name();

        new AlertDialog.Builder(this)
                .setTitle("Select Emotion")
                .setItems(emotionNames, (dialog, which) -> loadFollowingMoods("emotion", emotionNames[which]))
                .show();
    }

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
                                .collection("Moods")
                                .get()
                                .addOnSuccessListener(moodsSnapshot -> {
                                    for (QueryDocumentSnapshot doc : moodsSnapshot) {
                                        Mood mood = doc.toObject(Mood.class);
                                        if (mood.getPrivacy() == Mood.Privacy.PUBLIC &&
                                                mood.getLatitude() != 0 && mood.getLongitude() != 0) {

                                            boolean shouldAdd = false;
                                            if ("all".equals(filterType)) {
                                                shouldAdd = true;
                                            } else if ("recent".equals(filterType)) {
                                                shouldAdd = mood.getTimestamp() != null &&
                                                        mood.getTimestamp().after(new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000));
                                            } else if ("emotion".equals(filterType)) {
                                                shouldAdd = mood.getEmotion().name().equalsIgnoreCase(filterValue);
                                            } else if ("reason".equals(filterType)) {
                                                shouldAdd = mood.getReason() != null &&
                                                        mood.getReason().toLowerCase().contains(filterValue.toLowerCase());
                                            }

                                            if (shouldAdd) {
                                                LatLng moodLocation = new LatLng(mood.getLatitude(), mood.getLongitude());
                                                mMap.addMarker(new MarkerOptions()
                                                        .position(moodLocation)
                                                        .title(mood.getEmotionEmoji() + " " + mood.getEmotion().name()));
                                            }
                                        }
                                    }
                                });
                    }
                });
    }

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
}
