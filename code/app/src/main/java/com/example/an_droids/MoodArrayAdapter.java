package com.example.an_droids;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MoodArrayAdapter extends ArrayAdapter<Mood> {
    private final ArrayList<Mood> moods;
    private final Context context;

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
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_mood_details, null);

        ImageView imageView = dialogView.findViewById(R.id.detailImage);
        TextView emojiView = dialogView.findViewById(R.id.detailEmoji);
        TextView emotionView = dialogView.findViewById(R.id.detailEmotion);
        TextView reasonView = dialogView.findViewById(R.id.detailReason);
        TextView dateView = dialogView.findViewById(R.id.detailDate);
        TextView timeView = dialogView.findViewById(R.id.detailTime);
        TextView socialView = dialogView.findViewById(R.id.detailSocial);
        TextView privacyView = dialogView.findViewById(R.id.detailPrivacy);

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
        privacyView.setText(
                "Privacy: " + (mood.getPrivacy() == Mood.Privacy.PRIVATE ? "🔒 Private" : "🌍 Public")
        );

        if (mood.getImage() != null) {
            imageView.setImageBitmap(mood.getImage());
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle("Mood Details")
                .setPositiveButton("Close", null)
                .show();
    }
}


