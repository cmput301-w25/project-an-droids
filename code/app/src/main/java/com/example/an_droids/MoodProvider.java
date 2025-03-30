package com.example.an_droids;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class MoodProvider {
    private final ArrayList<Mood> moods;
    private final CollectionReference moodCollection;
    private final String userId;
    private DataStatus listener;

    public MoodProvider(FirebaseFirestore firestore, String userId) {
        this.userId = userId;
        this.moods = new ArrayList<>();
        this.moodCollection = firestore.collection("Users").document(userId).collection("Moods");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public ArrayList<Mood> getMoods() {
        return moods;
    }

    public void listenForUpdates(DataStatus status) {
        this.listener = status;
        moodCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e("MoodProvider", "Snapshot error: " + error.getMessage());
                if (listener != null) listener.onError(error.getMessage());
                return;
            }

            moods.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot doc : snapshot) {
                    moods.add(doc.toObject(Mood.class));
                }
                sortMoodsByDate();
                if (listener != null) listener.onDataUpdated();
            }
        });
    }

    public void addMood(Mood mood, String userId) {
        DocumentReference docRef = moodCollection.document();
        mood.setId(docRef.getId());

        if (validMood(mood, docRef)) {
            docRef.set(mood)
                    .addOnSuccessListener(aVoid -> Log.d("MoodProvider", "Mood added"))
                    .addOnFailureListener(e -> Log.e("MoodProvider", "Error adding mood", e));
        } else {
            throw new IllegalArgumentException("Invalid Mood!");
        }
    }

    public void updateMood(Mood mood) {
        DocumentReference docRef = moodCollection.document(mood.getId());

        if (validMood(mood, docRef)) {
            docRef.set(mood)
                    .addOnSuccessListener(aVoid -> Log.d("MoodProvider", "Mood updated"))
                    .addOnFailureListener(e -> Log.e("MoodProvider", "Error updating mood", e));
        }
    }

    public void deleteMood(Mood mood) {
        moodCollection.document(mood.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("MoodProvider", "Mood deleted"))
                .addOnFailureListener(e -> Log.e("MoodProvider", "Error deleting mood", e));
    }

    private boolean validMood(Mood mood, DocumentReference docRef) {
        return mood.getId() != null && mood.getId().equals(docRef.getId())
                && mood.getPrivacy() != null && mood.getReason() != null && !mood.getReason().isEmpty();
    }

    public void loadAllMoods() {
        moodCollection.get().addOnSuccessListener(querySnapshot -> {
            moods.clear();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                moods.add(doc.toObject(Mood.class));
            }
            sortMoodsByDate();
            if (listener != null) listener.onDataUpdated();
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    public void filterByRecentWeek() {
        Date oneWeekAgo = new Date(System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000));
        moodCollection.whereGreaterThan("timestamp", oneWeekAgo)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    moods.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        moods.add(doc.toObject(Mood.class));
                    }
                    sortMoodsByDate();
                    if (listener != null) listener.onDataUpdated();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void filterByEmotion(String emotion) {
        moodCollection.whereEqualTo("emotion", emotion)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    moods.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        moods.add(doc.toObject(Mood.class));
                    }
                    sortMoodsByDate();
                    if (listener != null) listener.onDataUpdated();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void filterByReasonContains(String keyword) {
        moodCollection.get().addOnSuccessListener(snapshot -> {
            moods.clear();
            for (QueryDocumentSnapshot doc : snapshot) {
                Mood mood = doc.toObject(Mood.class);
                if (mood.getReason() != null && mood.getReason().toLowerCase().contains(keyword.toLowerCase())) {
                    moods.add(mood);
                }
            }
            sortMoodsByDate();
            if (listener != null) listener.onDataUpdated();
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    public void loadPublicMoods(String targetUserId) {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(targetUserId)
                .collection("Moods")
                .whereEqualTo("privacy", "PUBLIC")
                .get()
                .addOnSuccessListener(snapshot -> {
                    moods.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        moods.add(doc.toObject(Mood.class));
                    }
                    sortMoodsByDate();
                    if (listener != null) listener.onDataUpdated();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void loadMoodsFromFollowing(List<String> followingUserIds) {
        moods.clear();
        HashSet<String> loadedMoodIds = new HashSet<>();

        for (String followedId : followingUserIds) {
            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(followedId)
                    .collection("Moods")
                    .whereEqualTo("privacy", "PUBLIC")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Mood mood = doc.toObject(Mood.class);
                            if (mood.getId() != null && loadedMoodIds.add(mood.getId())) {
                                moods.add(mood);
                            }
                        }
                        sortMoodsByDate();
                        if (listener != null) listener.onDataUpdated();
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) listener.onError(e.getMessage());
                    });
        }
    }

    private void sortMoodsByDate() {
        moods.sort((m1, m2) -> {
            if (m1.getTimestamp() == null) return 1;
            if (m2.getTimestamp() == null) return -1;
            return m2.getTimestamp().compareTo(m1.getTimestamp());
        });
    }
}