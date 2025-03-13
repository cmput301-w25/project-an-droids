package com.example.an_droids;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;

public class MoodProvider {
    private static MoodProvider instance;
    private final ArrayList<Mood> moods;
    private final CollectionReference moodCollection;

    private MoodProvider(FirebaseFirestore firestore) {
        moods = new ArrayList<>();
        moodCollection = firestore.collection("Moods");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public void listenForUpdates(DataStatus status) {
        moodCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
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

    public static MoodProvider getInstance(FirebaseFirestore firestore) {
        if (instance == null) {
            instance = new MoodProvider(firestore);
        }
        return instance;
    }

    public ArrayList<Mood> getMoods() {
        return moods;
    }

    public void addMood(Mood mood) {
        DocumentReference docRef = moodCollection.document();
        mood.setId(docRef.getId());
        if (validMood(mood, docRef)) {
            docRef.set(mood);
        } else {
            throw new IllegalArgumentException("Invalid Mood!");
        }
    }

    public void updateMood(Mood mood) {
        DocumentReference docRef = moodCollection.document(mood.getId());
        if (validMood(mood, docRef)) {
            docRef.set(mood);
        } else {
            throw new IllegalArgumentException("Invalid Mood!");
        }
    }

    public void deleteMood(Mood mood) {
        DocumentReference docRef = moodCollection.document(mood.getId());
        docRef.delete();
    }

    private boolean validMood(Mood mood, DocumentReference docRef) {
        return mood.getId().equals(docRef.getId()) && mood.getReason() != null && !mood.getReason().isEmpty();
    }
}
