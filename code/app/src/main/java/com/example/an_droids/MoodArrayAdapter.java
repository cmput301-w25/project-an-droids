package com.example.an_droids;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;


public class MoodArrayAdapter extends ArrayAdapter<Mood> {
    private ArrayList<Mood> moods;
    private Context context;

    public MoodArrayAdapter(Context context, ArrayList<Mood> moods) {
        super(context, 0, moods);
        this.moods = moods;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_mood, parent, false);
        }

        Mood mood = moods.get(position);

        TextView moodTitle = view.findViewById(R.id.moodTitle);
        TextView dateView = view.findViewById(R.id.dateAdded);
        TextView timeView = view.findViewById(R.id.timeAdded);
        TextView reasonView = view.findViewById(R.id.reasonText);
        TextView socialView = view.findViewById(R.id.socialText);
        TextView privacyView = view.findViewById(R.id.privacyText);
        ImageView infoButton = view.findViewById(R.id.infoButton);

        moodTitle.setText(mood.getEmotion().name() + " " + mood.getEmotionEmoji());
        reasonView.setText(mood.getReason());

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Date timestamp = mood.getTimestamp();
        if (timestamp != null) {
            dateView.setText(dateFormatter.format(timestamp));
            timeView.setText(timeFormatter.format(timestamp));
        } else {
            dateView.setText("");
            timeView.setText("");
        }

        socialView.setText(mood.getSocialSituationEmojiLabel());

        if (mood.getPrivacy() == Mood.Privacy.PRIVATE) {
            privacyView.setText("🔒 Private");
        } else {
            privacyView.setText("🌍 Public");
        }

        view.setBackgroundColor(Color.parseColor(mood.getEmotionColorHex()));

        infoButton.setOnClickListener(v -> showDetailsDialog(mood));

        return view;
    }


    private void showDetailsDialog(Mood mood) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_mood_details, null);

        ImageView imageView = view.findViewById(R.id.detailImage);
        TextView emojiView = view.findViewById(R.id.detailEmoji);
        TextView emotionView = view.findViewById(R.id.detailEmotion);
        TextView reasonView = view.findViewById(R.id.detailReason);
        TextView dateView = view.findViewById(R.id.detailDate);
        TextView timeView = view.findViewById(R.id.detailTime);
        TextView socialView = view.findViewById(R.id.detailSocial);
        TextView privacyView = view.findViewById(R.id.detailPrivacy);
        Button followButton = view.findViewById(R.id.moodFollowButton);

        emotionView.setText(mood.getEmotion().name());
        emojiView.setText(mood.getEmotionEmoji());
        reasonView.setText(mood.getReason());

        Date ts = mood.getTimestamp();
        if (ts != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            dateView.setText("Date: " + df.format(ts));
            timeView.setText("Time: " + tf.format(ts));
        }

        socialView.setText("Social: " + mood.getSocialSituationEmojiLabel());
        privacyView.setText("Privacy: " + (mood.getPrivacy() == Mood.Privacy.PRIVATE ? "🔒 Private" : "🌍 Public"));

        if (mood.getImage() != null) {
            imageView.setImageBitmap(mood.getImage());
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }

        // Follow Button Logic
        followButton.setOnClickListener(v -> followMoodEvent(mood.getId()));

        new AlertDialog.Builder(context)
                .setView(view)
                .setTitle("Mood Details")
                .setPositiveButton("Close", null)
                .show();
    }


    private void followMoodEvent(String moodEventId) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String currentUserId = auth.getCurrentUser().getUid();
        if (currentUserId == null || moodEventId == null) {
            Toast.makeText(context, "Error: User or Mood Event missing", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userDocRef = db.collection("Users").document(currentUserId);

        // Ensure followedMoods array exists, then add moodEventId
        userDocRef.set(Collections.singletonMap("followedMoods", FieldValue.arrayUnion(moodEventId)), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Followed!", Toast.LENGTH_SHORT).show();
                    Log.d("Firestore", "Successfully followed mood: " + moodEventId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to follow: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Firestore", "Error following mood", e);
                });

        // Check if followedMoods array is updated
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Check the current followedMoods
                Object followedMoods = task.getResult().get("followedMoods");
                Log.d("Firestore", "Current followedMoods: " + followedMoods);
            } else {
                Log.e("Firestore", "Error getting document", task.getException());
            }
        });
    }


}
