package com.example.an_droids;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;

public class MoodProvider {
    private final ArrayList<Mood> moods;
    private final CollectionReference moodCollection;

    public MoodProvider(FirebaseFirestore firestore, String userId) {
        moods = new ArrayList<>();
        moodCollection = firestore.collection("Users").document(userId).collection("Moods");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public void listenForUpdates(DataStatus status) {
        moodCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e("MoodProvider", "Snapshot listener error: " + error.getMessage());
                status.onError(error.getMessage());
                return;
            }
            moods.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot doc : snapshot) {
                    moods.add(doc.toObject(Mood.class));
                }
                status.onDataUpdated();
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
                && mood.getReason() != null && !mood.getReason().isEmpty();
    }
}
