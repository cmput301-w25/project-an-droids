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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoodArrayAdapter extends ArrayAdapter<Mood> {
    private final ArrayList<Mood> moods;
    private final Context context;
    // This ownerId is non-null when displaying a single user's moods (for following/unfollowing).
    // For followed moods (from various users), this will be null.
    private final String ownerId;

    // Constructor for cases where an ownerId is available.
    public MoodArrayAdapter(Context context, ArrayList<Mood> moods, String ownerId) {
        super(context, 0, moods);
        this.moods = moods;
        this.context = context;
        this.ownerId = ownerId;
    }

    // Overloaded constructor for cases (like FollowedMoodActivity) where no single ownerId applies.
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
        Button followButton = view.findViewById(R.id.moodFollowButton);
        Button commentButton = view.findViewById(R.id.moodCommentButton);
        Button viewCommentsButton = view.findViewById(R.id.moodViewCommentsButton);

        // Set mood details text
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

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Follow button is shown only when the current user is not the owner
        if (mood.getOwnerId() == null || mood.getOwnerId().equals(currentUserId)) {
            followButton.setVisibility(View.GONE);
        } else {
            followButton.setVisibility(View.VISIBLE);
            // Query current user's followedMoods to set initial button text.
            FirebaseFirestore.getInstance().collection("Users")
                    .document(currentUserId)
                    .get().addOnSuccessListener(documentSnapshot -> {
                        List<String> followed = (List<String>) documentSnapshot.get("followedMoods");
                        if (followed != null && followed.contains(mood.getId())) {
                            followButton.setText("Unfollow");
                        } else {
                            followButton.setText("Follow");
                        }
                    });
            followButton.setOnClickListener(v -> {
                String currentText = followButton.getText().toString();
                if (currentText.equals("Follow")) {
                    followMoodEvent(mood, followButton);
                } else {
                    unfollowMoodEvent(mood, followButton);
                }
            });
        }

        // Show comment button only if the current user is not the owner.
        if (mood.getOwnerId() != null && !mood.getOwnerId().equals(currentUserId)) {
            commentButton.setVisibility(View.VISIBLE);
            commentButton.setOnClickListener(v -> openCommentDialog(mood));
        } else {
            commentButton.setVisibility(View.GONE);
        }
        // View Comments button is always available.
        viewCommentsButton.setVisibility(View.VISIBLE);
        viewCommentsButton.setOnClickListener(v -> openViewCommentsDialog(mood));

        new AlertDialog.Builder(context)
                .setView(view)
                .setTitle("Mood Details")
                .setPositiveButton("Close", null)
                .show();
    }

    private void followMoodEvent(Mood mood, Button followButton) {
        String moodEventId = mood.getId();
        String moodOwnerId = mood.getOwnerId();
        if (moodEventId == null || moodOwnerId == null) {
            Toast.makeText(context, "Error: Mood details incomplete", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userDocRef = FirebaseFirestore.getInstance().collection("Users").document(currentUserId);

        userDocRef.update("followedMoods", FieldValue.arrayUnion(moodEventId))
                .addOnSuccessListener(aVoid -> {
                    // Create or update document in MoodLookup.
                    FirebaseFirestore.getInstance().collection("MoodLookup")
                            .document(moodEventId)
                            .set(new MoodLookup(moodEventId, moodOwnerId))
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(context, "Followed!", Toast.LENGTH_SHORT).show();
                                followButton.setText("Unfollow");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to update MoodLookup: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to follow: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void unfollowMoodEvent(Mood mood, Button followButton) {
        String moodEventId = mood.getId();
        if (moodEventId == null) {
            Toast.makeText(context, "Error: Mood details incomplete", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userDocRef = FirebaseFirestore.getInstance().collection("Users").document(currentUserId);

        userDocRef.update("followedMoods", FieldValue.arrayRemove(moodEventId))
                .addOnSuccessListener(aVoid -> {
                    // Remove from MoodLookup.
                    FirebaseFirestore.getInstance().collection("MoodLookup")
                            .document(moodEventId)
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(context, "Unfollowed!", Toast.LENGTH_SHORT).show();
                                followButton.setText("Follow");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to remove from MoodLookup: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to unfollow: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
        // First, fetch the current user's username from the "Users" collection.
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = documentSnapshot.getString("username");
                    if (username == null) {
                        username = currentUserId; // Fallback if no username is set.
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
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to add comment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to get username: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                        // Try to get the commenter's name; fall back to the ID if needed.
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
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to load comments: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}
