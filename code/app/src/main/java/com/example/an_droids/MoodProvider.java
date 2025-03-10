package com.example.an_droids;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class MoodProvider {
    private final CollectionReference moodsCollection;
    public MoodProvider() {
        moodsCollection = FirebaseFirestore.getInstance().collection("Moods");
    }
    public void getAllMoods(final OnMoodsLoadedListener callback) {
        moodsCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Mood> moodList = new ArrayList<>();
                for (DocumentSnapshot document : task.getResult()) {
                    Mood mood = document.toObject(Mood.class);
                    if (mood != null) {
                        mood.setId(document.getId());
                        moodList.add(mood);
                    }
                }
                callback.onMoodsLoaded(moodList);
            } else {
                callback.onError(task.getException());
            }
        });
    }

    public void addMood(Mood mood, final OnMoodOperationListener callback) {
        if (mood.getId() == null || mood.getId().isEmpty()) {
            String newId = moodsCollection.document().getId();
            mood.setId(newId);
        }
        moodsCollection.document(mood.getId()).set(mood)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public void updateMood(Mood mood, final OnMoodOperationListener callback) {
        if (mood.getId() == null || mood.getId().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Mood has no ID"));
            return;
        }
        moodsCollection.document(mood.getId()).set(mood)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteMood(Mood mood, final OnMoodOperationListener callback) {
        if (mood.getId() == null || mood.getId().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Mood has no ID"));
            return;
        }
        moodsCollection.document(mood.getId()).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public interface OnMoodsLoadedListener {
        void onMoodsLoaded(List<Mood> moods);
        void onError(Exception e);
    }

    public interface OnMoodOperationListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}
