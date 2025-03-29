package com.example.an_droids;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class MoodArrayAdapter extends ArrayAdapter<Mood> {
    private final ArrayList<Mood> moods;
    private final Context context;
    private final String ownerId;

    public MoodArrayAdapter(Context context, ArrayList<Mood> moods, String ownerId) {
        super(context, 0, moods);
        this.moods = moods;
        this.context = context;
        this.ownerId = ownerId;
    }

    public MoodArrayAdapter(Context context, ArrayList<Mood> moods) {
        this(context, moods, null);
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
        privacyView.setText(mood.getPrivacy() == Mood.Privacy.PRIVATE ? "🔒 Private" : "🌍 Public");
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
        TextView locationView = view.findViewById(R.id.detailLocation);
        Button commentButton = view.findViewById(R.id.moodCommentButton);
        Button viewCommentsButton = view.findViewById(R.id.moodViewCommentsButton);

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

        if (mood.getAddress() != null && !mood.getAddress().isEmpty()) {
            locationView.setText("Location: 📍 " + mood.getAddress());
        } else {
            locationView.setText("Location: 📍 Not available");
        }

        if (mood.getImage() != null) {
            imageView.setImageBitmap(mood.getImage());
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }

        // ✅ Anyone can comment
        commentButton.setVisibility(View.VISIBLE);
        commentButton.setOnClickListener(v -> openCommentDialog(mood));

        viewCommentsButton.setVisibility(View.VISIBLE);
        viewCommentsButton.setOnClickListener(v -> openViewCommentsDialog(mood));

        new AlertDialog.Builder(context)
                .setView(view)
                .setTitle("Mood Details")
                .setPositiveButton("Close", null)
                .show();
    }

    private void openCommentDialog(Mood mood) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Comment");
        final EditText input = new EditText(context);
        input.setHint("Enter your comment");
        builder.setView(input);
        builder.setPositiveButton("Submit", (dialog, which) -> {
            String commentText = input.getText().toString().trim();
            if (!commentText.isEmpty()) {
                addComment(mood, commentText);
            } else {
                Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addComment(Mood mood, String commentText) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = documentSnapshot.getString("username");
                    if (username == null) {
                        username = currentUserId;
                    }

                    Map<String, Object> comment = new HashMap<>();
                    comment.put("moodId", mood.getId());
                    comment.put("commentText", commentText);
                    comment.put("commenterId", currentUserId);
                    comment.put("commenterName", username);
                    comment.put("timestamp", new Date());

                    FirebaseFirestore.getInstance()
                            .collection("MoodComments")
                            .add(comment)
                            .addOnSuccessListener(documentReference ->
                                    Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to comment: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }

    private void openViewCommentsDialog(Mood mood) {
        FirebaseFirestore.getInstance()
                .collection("MoodComments")
                .whereEqualTo("moodId", mood.getId())
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    StringBuilder commentsBuilder = new StringBuilder();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String commenterName = doc.getString("commenterName");
                        if (commenterName == null) {
                            commenterName = doc.getString("commenterId");
                        }
                        String commentText = doc.getString("commentText");
                        commentsBuilder.append(commenterName)
                                .append(": ")
                                .append(commentText)
                                .append("\n\n");
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Comments");
                    builder.setMessage(commentsBuilder.length() > 0 ? commentsBuilder.toString() : "No comments yet.");
                    builder.setPositiveButton("Close", null);
                    builder.show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to load comments: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}