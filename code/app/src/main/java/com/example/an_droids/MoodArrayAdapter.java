package com.example.an_droids;
//
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MoodArrayAdapter extends android.widget.ArrayAdapter<Mood> {
    private final Context context;
    private final ArrayList<Mood> moods;

    public MoodArrayAdapter(Context context, ArrayList<Mood> moods) {
        super(context, 0, moods);
        this.context = context;
        this.moods = moods;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Mood mood = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_mood, parent, false);
        }

        TextView moodTitle = convertView.findViewById(R.id.moodTitle);
        TextView dateAdded = convertView.findViewById(R.id.dateAdded);
        TextView timeAdded = convertView.findViewById(R.id.timeAdded);
        TextView locationText = convertView.findViewById(R.id.locationText);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (mood.getEmotion() != null) {
            moodTitle.setText(mood.getEmotion().name() + " " + mood.getEmotionEmoji());
            convertView.setBackgroundColor(Color.parseColor(mood.getEmotionColorHex()));
        } else {
            moodTitle.setText("Unknown Mood");
        }

        if (mood.getTimestamp() != null) {
            dateAdded.setText(dateFormat.format(mood.getTimestamp()));
            timeAdded.setText(timeFormat.format(mood.getTimestamp()));
        } else {
            dateAdded.setText("No date");
            timeAdded.setText("No time");
        }

        // ✅ Show full address string instead of lat/lng
        if (mood.getAddress() != null && !mood.getAddress().isEmpty()) {
            locationText.setText("📍 " + mood.getAddress());
        } else {
            locationText.setText("📍 N/A");
        }

        return convertView;
    }
}
