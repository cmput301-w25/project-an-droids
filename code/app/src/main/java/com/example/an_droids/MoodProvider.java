package com.example.an_droids;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Date;
import java.util.ArrayList;

public class MoodProvider {
    private final ArrayList<Mood> moods;
    private final CollectionReference moodCollection;
    private DataStatus listener;

    public MoodProvider(FirebaseFirestore firestore, String userId) {
        moods = new ArrayList<>();
        moodCollection = firestore.collection("Users").document(userId).collection("Moods");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public void listenForUpdates(DataStatus status) {
        this.listener = status;
        moodCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e("MoodProvider", "Snapshot listener error: " + error.getMessage());
                if (listener != null) listener.onError(error.getMessage());
                return;
            }

            moods.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot doc : snapshot) {
                    moods.add(doc.toObject(Mood.class));
                }

                sortMoodsByDate();
                if (listener != null) listener.onDataUpdated(); // Notify UI
            }
        });
    }

    public ArrayList<Mood> getMoods() {
        return moods;
    }

    public void addMood(Mood mood) {
        DocumentReference docRef = moodCollection.document();
        mood.setId(docRef.getId());
        if (validMood(mood, docRef)) {
            docRef.set(mood)
                    .addOnSuccessListener(aVoid -> Log.d("MoodProvider", "Mood added successfully"))
                    .addOnFailureListener(e -> Log.e("MoodProvider", "Error adding mood", e));
        } else {
            throw new IllegalArgumentException("Invalid Mood!");
        }
    }

    public void updateMood(Mood mood) {
        DocumentReference docRef = moodCollection.document(mood.getId());
        if (validMood(mood, docRef)) {
            docRef.set(mood)
                    .addOnSuccessListener(aVoid -> Log.d("MoodProvider", "Mood updated successfully"))
                    .addOnFailureListener(e -> Log.e("MoodProvider", "Error updating mood", e));
        } else {
            throw new IllegalArgumentException("Invalid Mood!");
        }
    }

    public void deleteMood(Mood mood) {
        DocumentReference docRef = moodCollection.document(mood.getId());
        docRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("MoodProvider", "Mood deleted successfully"))
                .addOnFailureListener(e -> Log.e("MoodProvider", "Error deleting mood", e));
    }

    private boolean validMood(Mood mood, DocumentReference docRef) {
        return mood.getId() != null && mood.getId().equals(docRef.getId())
                && mood.getReason() != null && !mood.getReason().isEmpty()
                && mood.getPrivacy() != null;
    }

    public void loadAllMoods() {
        moodCollection.get().addOnSuccessListener(queryDocumentSnapshots -> {
            moods.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                moods.add(doc.toObject(Mood.class));
            }
            sortMoodsByDate();
            if (listener != null) listener.onDataUpdated(); // Notify UI
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    public void filterByRecentWeek() {
        Date oneWeekAgo = new Date(System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000));
        moodCollection.whereGreaterThan("timestamp", oneWeekAgo)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    moods.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        moods.add(doc.toObject(Mood.class));
                    }
                    sortMoodsByDate();
                    if (listener != null) listener.onDataUpdated(); // Notify UI
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void filterByEmotion(String emotion) {
        moodCollection.whereEqualTo("emotion", emotion)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    moods.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        moods.add(doc.toObject(Mood.class));
                    }
                    sortMoodsByDate();
                    if (listener != null) listener.onDataUpdated(); // Notify UI
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    private void sortMoodsByDate() {
        moods.sort((m1, m2) -> {
            if (m1.getTimestamp() == null) return 1;
            if (m2.getTimestamp() == null) return -1;
            return m2.getTimestamp().compareTo(m1.getTimestamp());
        });
    }

    public void filterByReasonContains(String keyword) {
        moodCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    moods.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Mood mood = doc.toObject(Mood.class);
                        if (mood.getReason() != null && mood.getReason().toLowerCase().contains(keyword.toLowerCase())) {
                            moods.add(mood);
                        }
                    }
                    sortMoodsByDate();
                    if (listener != null) listener.onDataUpdated(); // Notify UI
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void loadPublicMoods(String userId) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Query the "Moods" collection of the specific user and filter by privacy
        firestore.collection("Users")
                .document(userId)  // Reference to the user you're searching for
                .collection("Moods")  // Moods collection of that user
                .whereEqualTo("privacy", "PUBLIC")  // Only public moods
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    moods.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        moods.add(doc.toObject(Mood.class));
                    }
                    sortMoodsByDate();
                    if (listener != null) listener.onDataUpdated();  // Notify UI
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }




}
